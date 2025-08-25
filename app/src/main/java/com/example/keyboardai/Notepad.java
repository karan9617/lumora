package com.example.keyboardai;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    private TextView hintTextView; // The TextView for the inline hint
    private Intent recognizerIntent;
    private NoteRepository noteRepository;
    private DrawingView drawingView;
    private Button toggleModeButton;
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
    private String currentHint = "";

    // API Key for Gemini API, will be provided at runtime
    private static final String API_KEY = "AIzaSyCes8zNYgUuYAfpKGLGYmG5r0oQW5cx_2o";
    // FIX: Updated the API URL to use the working model name
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

        noteRepository = new NoteRepository(this);
        drawingView = findViewById(R.id.drawingView);
        toggleModeButton = findViewById(R.id.toggleModeButton);

        noteId = getIntent().getLongExtra("note_id", -1);
        String noteTitle = getIntent().getStringExtra("note_title");
        String noteContent = getIntent().getStringExtra("note_content");
        noteDate = getIntent().getStringExtra("note_date");
        selectedColor = getIntent().getIntExtra("note_color", Color.WHITE);
        drawingData = getIntent().getByteArrayExtra("drawing_data");

        if (noteId != -1) {
            titleText.setText(noteTitle);
            resultText.setText(noteContent);
            resultBuilder.append(noteContent);
        } else {
            noteDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
            titleText.setHint("Untitled");
        }

        mainContentLayout.setBackgroundColor(selectedColor);
        titleText.setBackgroundColor(selectedColor);
        resultText.setBackgroundColor(selectedColor);
        hintTextView.setBackgroundColor(selectedColor);
        // FIX: Set the background of the EditTexts and TextView to null to prevent them from having a white background
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
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isNoteModified = true;
                // Clear the hint if the text is empty
                if (s.length() == 0) {
                    hintTextView.setText("");
                    currentHint = "";
                }

                if (suggestionRunnable != null) {
                    handler.removeCallbacks(suggestionRunnable);
                }

                // Only generate a new hint if the text is not empty and the last character is a space
                if (s.length() > 0 && s.charAt(s.length() - 1) == ' ') {
                    suggestionRunnable = () -> generateSuggestions(s.toString());
                    handler.postDelayed(suggestionRunnable, DELAY);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });

        // NEW: Add a key listener to handle "accepting" the hint with a space or enter
        resultText.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (!currentHint.isEmpty()) {
                        resultText.append(currentHint);
                        hintTextView.setText("");
                        currentHint = "";
                        return true; // Consume the event so space/enter isn't added
                    }
                }
            }
            return false;
        });

        // FIX: Removed the non-working OnTouchListener on hintTextView
        // NEW: Add an OnClickListener to the resultText to handle taps on the hint
        resultText.setOnClickListener(v -> {
            if (!currentHint.isEmpty() && resultText.getSelectionEnd() == resultText.getText().length()) {
                resultText.append(currentHint);
                hintTextView.setText("");
                currentHint = "";
            }
        });

        titleText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isNoteModified = true;
            }
            @Override
            public void afterTextChanged(Editable s) {}
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
                    isNoteModified = true; // Mark as modified from voice input
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

    private void toggleMode() {
        if (isDrawingMode) {
            isDrawingMode = false;
            resultText.setVisibility(View.VISIBLE);
            drawingView.setVisibility(View.GONE);
            toggleModeButton.setText("Draw");
        } else {
            isDrawingMode = true;
            resultText.setVisibility(View.GONE);
            drawingView.setVisibility(View.VISIBLE);
            toggleModeButton.setText("Text");
        }
    }

    // UPDATED: Override onBackPressed to check for unsaved changes
    @Override
    public void onBackPressed() {
        Log.d(TAG, "on back press");
        if (isNoteModified) {
            new AlertDialog.Builder(this)
                    .setTitle("Save Note?")
                    .setMessage("You have unsaved changes. Do you want to save this note?")
                    .setPositiveButton("Save", (dialog, which) -> {
                        saveNote();
                    })
                    .setNegativeButton("Discard", (dialog, which) -> {
                        supportFinishAfterTransition();
                    })
                    .setNeutralButton("Cancel", (dialog, which) -> {
                        // Do nothing, stay on the same screen
                    })
                    .show();
        } else {
            // No changes, just exit
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
        }
        return super.onOptionsItemSelected(item);
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
        builder.setItems(colorNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedColor = colors[which];
                mainContentLayout.setBackgroundColor(selectedColor);
                titleText.setBackgroundColor(selectedColor);
                resultText.setBackgroundColor(selectedColor);
                hintTextView.setBackgroundColor(selectedColor);
                isNoteModified = true;
            }
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

        new Thread(() -> {
            if (noteId != -1) {
                Note existingNote = new Note(noteId, title, content, noteDate, drawingDataToSave, finalColorToSave);
                noteRepository.updateNote(existingNote);
                runOnUiThread(() -> Toast.makeText(this, "Note updated!", Toast.LENGTH_SHORT).show());
            } else {
                Note newNote = new Note(title, content, noteDate, drawingDataToSave, finalColorToSave);
                noteRepository.insertNote(newNote);
                runOnUiThread(() -> Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show());
            }
        }).start();

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
                    Log.d(TAG, " Response: " + response.body());

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

                                // Find the common prefix to avoid re-typing
                                String[] words = text.split(" ");
                                String lastWord = words[words.length - 1];

                                if (generatedText.toLowerCase().startsWith(lastWord.toLowerCase())) {
                                    generatedText = generatedText.substring(lastWord.length());
                                }

                                final String finalGeneratedText = generatedText;

                                // Update UI on the main thread
                                runOnUiThread(() -> {
                                    currentHint = finalGeneratedText.trim();
                                    hintTextView.setText(text + currentHint);
                                });
                                return; // Exit loop on success
                            }
                        }
                    } else {
                        Log.e(TAG, "API call failed with code: " + response.code() + ". Message: " + response.message());
                        if (response.body() != null) {
                            Log.e(TAG, "Failed response body: " + response.body().string());
                        }
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
            // Hide suggestions if all retries fail
            runOnUiThread(() -> {
                hintTextView.setText("");
                currentHint = "";
            });
        });
    }
}
