package com.example.keyboardai;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.example.keyboardai.Models.Note;
import com.example.keyboardai.data.NoteRepository;
import com.example.keyboardai.ui.DrawingView;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Notepad extends AppCompatActivity {

    private static final String TAG = "NotepadActivity";

    private SpeechRecognizer speechRecognizer;
    private StringBuilder resultBuilder = new StringBuilder();
    private boolean isListening = false;
    private ProgressBar listeningProgress;
    private EditText resultText;
    private EditText titleText;
    private TextView hintTextView;
    private Intent recognizerIntent;
    private NoteRepository noteRepository;
    private DrawingView drawingView;
    private Button toggleModeButton,toggleModeDrawSave;
    private long noteId = -1;
    private String noteDate;
    private byte[] drawingData;
    private boolean isDrawingMode = false;
    private RelativeLayout mainContentLayout;
    private int selectedColor = Color.WHITE;
    private boolean isNoteModified = false;
    private Handler handler;
    private Runnable suggestionRunnable;
    private final long DELAY = 500;
    private ImageView imagesketch;
    private String currentHint = "";
    // NEW: Variable to hold the note's pinned status
    private boolean isPinned = false;
    // NEW: Variable to hold the note's order
    private int noteOrder;

    // API Key for Gemini API, will be provided at runtime
    private static final String API_KEY = "AIzaSyCes8zNYgUuYAfpKGLGYmG5r0oQW5cx_2o";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postponeEnterTransition();
        setContentView(R.layout.notepad_layout);

        // UI references
        listeningProgress = findViewById(R.id.listeningProgress);
        resultText = findViewById(R.id.resultText);
        titleText = findViewById(R.id.noteTitleEditText);
        mainContentLayout = findViewById(R.id.main_content_layout);
        hintTextView = findViewById(R.id.hintTextView);
        toggleModeDrawSave =  findViewById(R.id.toggleModeDrawSave);
        toggleModeDrawSave.setVisibility(View.INVISIBLE);
        imagesketch = findViewById(R.id.imagesketch);
        registerListeners();
        noteRepository = new NoteRepository(this);
        drawingView = findViewById(R.id.drawingView);
        toggleModeButton = findViewById(R.id.toggleModeButton);

        noteId = getIntent().getLongExtra("note_id", -1);
        String noteTitle = getIntent().getStringExtra("note_title");
        String noteContent = getIntent().getStringExtra("note_content");
        noteDate = getIntent().getStringExtra("note_date");
        selectedColor = getIntent().getIntExtra("note_color", Color.WHITE);
        drawingData = getIntent().getByteArrayExtra("drawing_data");
        // NEW: Retrieve the pinned status from the intent
        isPinned = getIntent().getBooleanExtra("note_is_pinned", false);
        // NEW: Retrieve the note's order from the intent
        noteOrder = getIntent().getIntExtra("note_order", -1);
        if(drawingData != null && drawingData.length > 0){
            Bitmap savedBitmap = BitmapFactory.decodeByteArray(drawingData, 0, drawingData.length);
            Toast.makeText(getApplicationContext(),"rendering image",Toast.LENGTH_SHORT).show();
            // Check if the bitmap was successfully created
            if (savedBitmap != null) {
                // Assign the bitmap to your ImageView and make it visible
                imagesketch.setImageBitmap(savedBitmap);
                imagesketch.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(),"image assigned",Toast.LENGTH_SHORT).show();
            }
        }


        if (noteId != -1) {
            titleText.setText(noteTitle);
            resultText.setText(noteContent);
            resultBuilder.append(noteContent);
        } else {
            noteDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
            titleText.setHint("Untitled");
        }

        mainContentLayout.setBackgroundColor(selectedColor);
        titleText.setBackground(null);
        resultText.setBackground(null);
        hintTextView.setBackground(null);

        String transitionName = getIntent().getStringExtra("TRANSITION_NAME");
        if (transitionName != null) {
            ViewCompat.setTransitionName(findViewById(R.id.main_content_layout), transitionName);
        }
        startPostponedEnterTransition();

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);

        // NEW: Call the method to set the correct pin icon when the activity is created.
        setPinIcon(isPinned);

        toggleModeButton.setOnClickListener(v -> toggleMode());
        Button clearDrawingButton = findViewById(R.id.clearDrawingButton);
        clearDrawingButton.setOnClickListener(v -> {
            drawingView.clearDrawing();
            isNoteModified = true;
        });
        resultText.setVisibility(View.VISIBLE);
        drawingView.setVisibility(View.GONE);
        toggleModeButton.setText("Draw");

        handler = new Handler(Looper.getMainLooper());

        // Inline suggestion logic
        resultText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isNoteModified = true;
                if (s.length() == 0) {
                    hintTextView.setText("");
                    currentHint = "";
                }

                if (suggestionRunnable != null) {
                    handler.removeCallbacks(suggestionRunnable);
                }

                if (s.length() > 0 && s.charAt(s.length() - 1) == ' ') {
                    suggestionRunnable = () -> generateSuggestions(s.toString());
                    handler.postDelayed(suggestionRunnable, DELAY);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // NEW: Add a key listener to handle "accepting" the hint with a space or enter
        resultText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (!currentHint.isEmpty()) {
                        resultText.append(currentHint);
                        hintTextView.setText("");
                        currentHint = "";
                        return true;
                    }
                }
            }
            return false;
        });

        resultText.setOnClickListener(v -> {
            if (!currentHint.isEmpty() && resultText.getSelectionEnd() == resultText.getText().length()) {
                resultText.append(currentHint);
                hintTextView.setText("");
                currentHint = "";
            }
        });

        titleText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { isNoteModified = true; }
            @Override public void afterTextChanged(Editable s) {}
        });

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { listeningProgress.setVisibility(ProgressBar.VISIBLE); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                if (isListening) {
                    speechRecognizer.startListening(recognizerIntent);
                }
            }
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && matches.size() > 0) {
                    String spokenText = matches.get(0);
                    if (spokenText.equalsIgnoreCase("done")) {
                        isListening = false;
                        speechRecognizer.stopListening();
                        listeningProgress.setVisibility(ProgressBar.GONE);
                        return;
                    }
                    resultBuilder.append(spokenText).append(" ");
                    resultText.setText(resultBuilder.toString());
                    isNoteModified = true;
                    if (isListening) {
                        speechRecognizer.startListening(recognizerIntent);
                    }
                }
            }
            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null && partial.size() > 0) {
                    resultText.setText(resultBuilder.toString() + partial.get(0));
                }
            }
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }
    public void registerListeners(){
        toggleModeDrawSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        // NEW: OnClickListener for imagesketch
        imagesketch.setOnClickListener(v -> {
            // Check if there is an image loaded to the ImageView
            if (imagesketch.getDrawable() != null) {
                // Get the bitmap from the ImageView
                BitmapDrawable drawable = (BitmapDrawable) imagesketch.getDrawable();
                Bitmap existingBitmap = drawable.getBitmap();

                // Load the bitmap onto the drawing view and switch to drawing mode
                if (existingBitmap != null) {
                    drawingView.setDrawingBitmap(existingBitmap);
                    toggleMode(); // Call the toggleMode method to switch views
                }
            }
        });
    }
    private void toggleMode() {
        isDrawingMode = !isDrawingMode;
        if (isDrawingMode) {
            resultText.setVisibility(View.GONE);
            imagesketch.setVisibility(View.GONE); // Hide the ImageView when drawing
            drawingView.setVisibility(View.VISIBLE);
            toggleModeDrawSave.setVisibility(View.VISIBLE);
            toggleModeButton.setText("Text");
        } else {
            resultText.setVisibility(View.VISIBLE);
            if (imagesketch.getDrawable() != null) { // Only show imagesketch if it has an image
                imagesketch.setVisibility(View.VISIBLE);
            }
            drawingView.setVisibility(View.GONE);
            toggleModeDrawSave.setVisibility(View.GONE);
            toggleModeButton.setText("Draw");
        }
    }

    // UPDATED: Override onBackPressed to check for unsaved changes
    @Override
    public void onBackPressed() {
        if (isNoteModified) {
            new AlertDialog.Builder(this)
                    .setTitle("Save Note?")
                    .setMessage("You have unsaved changes. Do you want to save this note?")
                    .setPositiveButton("Save", (dialog, which) -> saveNote())
                    .setNegativeButton("Discard", (dialog, which) -> supportFinishAfterTransition())
                    .setNeutralButton("Cancel", (dialog, which) -> {})
                    .show();
        } else {
            supportFinishAfterTransition();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_voice) {
            if (!isListening) {
                isListening = true;
                resultBuilder.setLength(0);
                speechRecognizer.startListening(recognizerIntent);
                Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show();
            } else {
                isListening = false;
                speechRecognizer.stopListening();
                listeningProgress.setVisibility(ProgressBar.GONE);
                Toast.makeText(this, "Stopped listening", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.action_save) {
            saveNote();
            return true;
        } else if (id == R.id.action_color) {
            showColorPickerDialog();
            return true;
        } else if (id == R.id.action_pin_unpin) {
            // NEW: Handle the pin/unpin action
            togglePinStatus();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // NEW: Method to handle toggling the pin status
    private void togglePinStatus() {
        isPinned = !isPinned;
        setPinIcon(isPinned);
        if (noteId != -1) {
            Executors.newSingleThreadExecutor().execute(() -> {
                noteRepository.updateNotePinStatus(noteId, isPinned);
                runOnUiThread(() -> {
                    Toast.makeText(this, isPinned ? "Note pinned!" : "Note unpinned!", Toast.LENGTH_SHORT).show();
                });
            });
        }
    }

    // NEW: Method to set the correct icon on the toolbar
    private void setPinIcon(boolean isPinned) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        MenuItem pinItem = toolbar.getMenu().findItem(R.id.action_pin_unpin);
        if (pinItem != null) {
            if (isPinned) {
                pinItem.setIcon(R.drawable.ic_pin_off);
            } else {
                pinItem.setIcon(R.drawable.ic_pin);
            }
        }
    }

    private void showColorPickerDialog() {
        final int[] colors = {
                Color.WHITE,
                Color.parseColor("#FFC0CB"),
                Color.parseColor("#FFFF66"),
                Color.parseColor("#C0C0C0"),
                Color.parseColor("#ADD8E6")
        };

        final String[] colorNames = {"White", "Pink", "Yellow", "Silver", "Light Blue"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Background Color");
        builder.setItems(colorNames, (dialog, which) -> {
            selectedColor = colors[which];
            mainContentLayout.setBackgroundColor(selectedColor);
            titleText.setBackgroundColor(selectedColor);
            resultText.setBackgroundColor(selectedColor);
            hintTextView.setBackgroundColor(selectedColor);
            isNoteModified = true;
        });
        builder.show();
    }

    private void saveNote() {
        String title = titleText.getText().toString().trim();
        String content = resultText.getText().toString().trim();
        byte[] drawingDataToSave = drawingView.getDrawingData();

        if (title.isEmpty() && content.isEmpty() && drawingDataToSave == null) {
            Toast.makeText(this, "Note is empty, not saved.", Toast.LENGTH_SHORT).show();
            isNoteModified = false;
            supportFinishAfterTransition();
            return;
        }

        int colorToSave = Color.WHITE;
        Drawable background = mainContentLayout.getBackground();
        if (background instanceof ColorDrawable) {
            colorToSave = ((ColorDrawable) background).getColor();
        }

        final int finalColorToSave = colorToSave;

        Executors.newSingleThreadExecutor().execute(() -> {
            if (noteId != -1) {
                // NEW: Pass the pinned status and order to the Note object
                Note existingNote = new Note(noteId, title, content, noteDate, drawingDataToSave, finalColorToSave, noteOrder, isPinned);
                noteRepository.updateNote(existingNote);
                runOnUiThread(() -> Toast.makeText(this, "Note updated!", Toast.LENGTH_SHORT).show());
            } else {
                // NEW: Pass the pinned status to the new Note object. The order will be set by the repository.
                Note newNote = new Note(title, content, noteDate, drawingDataToSave, finalColorToSave, 0, isPinned);
                noteRepository.addNote(newNote);
                runOnUiThread(() -> Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show());
            }
        });

// Check if the drawing data is not null or empty
        if (drawingDataToSave != null && drawingDataToSave.length > 0) {
            // Convert the byte array back into a Bitmap
            Bitmap savedBitmap = BitmapFactory.decodeByteArray(drawingDataToSave, 0, drawingDataToSave.length);
            Toast.makeText(getApplicationContext(),"saving image",Toast.LENGTH_SHORT).show();
            // Check if the bitmap was successfully created
            if (savedBitmap != null) {
                // Assign the bitmap to your ImageView and make it visible
                imagesketch.setImageBitmap(savedBitmap);
                imagesketch.setVisibility(View.VISIBLE);
                Toast.makeText(getApplicationContext(),"image assigned",Toast.LENGTH_SHORT).show();
            }
        } else {
            // If there is no drawing data, hide the ImageView
            imagesketch.setVisibility(View.GONE);
        }

        isNoteModified = false;
        supportFinishAfterTransition();
    }

    /**
     * Replaces the deprecated AsyncTask with a modern Thread-based approach.
     * Generates text suggestions in the background using the Gemini API.
     */
    private void generateSuggestions(String text) {
        if (text == null || text.isEmpty()) {
            Log.d(TAG, "Input text is empty, hiding suggestions.");
            runOnUiThread(() -> {
                hintTextView.setText("");
                currentHint = "";
            });
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            final int RETRY_COUNT = 3;
            final long RETRY_DELAY_MS = 1000;
            String prompt = "Complete the following sentence with one short suggestion. Do not include a period at the end of the suggestion.\n\n" + text;

            for (int i = 0; i < RETRY_COUNT; i++) {
                try {
                    OkHttpClient client = new OkHttpClient();
                    JSONObject jsonBody = new JSONObject();
                    JSONObject contents = new JSONObject();
                    JSONArray parts = new JSONArray();
                    JSONObject textPart = new JSONObject();
                    textPart.put("text", prompt);
                    parts.put(textPart);
                    contents.put("parts", parts);
                    jsonBody.put("contents", new JSONArray().put(contents));

                    RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
                    Log.d(TAG, "Making API call. Prompt: " + prompt);

                    Request request = new Request.Builder()
                            .url(API_URL)
                            .post(body)
                            .build();
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        Log.d(TAG, "API call successful. Response body length: " + responseBody.length());
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        if (candidates.length() > 0) {
                            JSONObject firstCandidate = candidates.getJSONObject(0);
                            JSONObject content = firstCandidate.getJSONObject("content");
                            JSONArray partsArray = content.getJSONArray("parts");
                            if (partsArray.length() > 0) {
                                JSONObject firstPart = partsArray.getJSONObject(0);
                                String generatedText = firstPart.getString("text").trim();
                                String[] words = text.split(" ");
                                String lastWord = words[words.length - 1];
                                if (generatedText.toLowerCase().startsWith(lastWord.toLowerCase())) {
                                    generatedText = generatedText.substring(lastWord.length());
                                }
                                final String finalGeneratedText = generatedText;
                                runOnUiThread(() -> {
                                    currentHint = finalGeneratedText.trim();
                                    hintTextView.setText(text + currentHint);
                                });
                                return; // Exit loop on success
                            }
                        }
                    } else {
                        Log.e(TAG, "API call failed with code: " + response.code() + ". Message: " + response.message());
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error during API call (attempt " + (i + 1) + "): " + e.getMessage(), e);
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS * (long) Math.pow(2, i));
                    Log.d(TAG, "Retrying API call after delay.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            runOnUiThread(() -> {
                hintTextView.setText("");
                currentHint = "";
            });
        });
    }
}
