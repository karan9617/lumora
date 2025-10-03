package com.noteaiapp.keyboardai;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.camera.CameraActivity;
import com.noteaiapp.keyboardai.data.FileUtils;
import com.noteaiapp.keyboardai.data.NoteRepository;
import com.noteaiapp.keyboardai.data.WordTokenizer;
import com.noteaiapp.keyboardai.processor.WordProcessor;
import com.noteaiapp.keyboardai.ui.DrawingView;
import com.noteaiapp.keyboardai.widget.NotesWidgetProvider;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.json.JSONArray;
import org.json.JSONObject;

public class Notepad extends AppCompatActivity {

    private static final String TAG = "NotepadActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;
    // camera
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private Uri photoUri;
    private EditText editText;
    private Button cameraButton;
    private String currentPhotoPath;

    ImageButton cameraScanButton;

    //speach
    private SpeechRecognizer speechRecognizer;
    FrameLayout imageframelayout;
    private StringBuilder resultBuilder = new StringBuilder(),titleBuilder = new StringBuilder();;
    private boolean isListening = false;
    private ProgressBar listeningProgress;
    private EditText resultText;
    private EditText titleText;
    CardView imageCard;
    private TextView hintTextView;
    private Intent recognizerIntent;
    private NoteRepository noteRepository;
    private DrawingView drawingView;
    private Button toggleModeDrawSave;
    ImageButton clearDrawingButton,clearImageButton,black_pen,red_pen;
    private long noteId = -1;
    private String noteDate;
    private byte[] drawingData;
    private String imagePath;
    private boolean isDrawingMode = false;
    private RelativeLayout mainContentLayout;
    private int selectedColor = Color.WHITE;
    private boolean isNoteModified = false,isDirty = false;
    MaterialToolbar toolbar;
    private Handler handler;
    private Runnable suggestionRunnable;
    private final long DELAY = 500;
    private ImageView imagesketch,voiceicon;
    private String currentHint = "";
    // NEW: Variable to hold the note's pinned status
    private boolean isPinned = false;
    // NEW: Variable to hold the note's order
    private int noteOrder;

    // API Key for Gemini API, will be provided at runtime
    private static final String API_KEY = "AIzaSyCes8zNYgUuYAfpKGLGYmG5r0oQW5cx_2o";
   // private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;
   private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=" + API_KEY;
    private static final String DATE_EXTRA_KEY = "date_specific_notes";
    private boolean dateReceived = false;
    private String receivedDateFromActivities = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        EdgeToEdge.enable(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        postponeEnterTransition();
        init();
        registerListeners();
        noteRepository = new NoteRepository(this);
        drawingView = findViewById(R.id.drawingView);

        noteId = getIntent().getLongExtra("note_id", -1);
        Note currentNode = noteRepository.getNoteById(noteId);
        String receivedDate = getIntent().getStringExtra(DATE_EXTRA_KEY);
        if(receivedDate != null && !receivedDate.isEmpty()){
            dateReceived = true;
            this.receivedDateFromActivities = receivedDate;
        }
        else{
            receivedDateFromActivities = getCurrentDate();
        }
        /*
        String noteTitle = getIntent().getStringExtra("note_title");
        String noteContent = getIntent().getStringExtra("note_content");
        noteDate = getIntent().getStringExtra("note_date");
        selectedColor = getIntent().getIntExtra("note_color", Color.WHITE);
        imagePath = getIntent().getStringExtra("note_image_path"); // Retrieve the image path
        drawingData = FileUtils.loadFileFromPath(getIntent().getStringExtra("note_image_path"));
        */
        String noteTitle = (currentNode == null )? "":(currentNode.getTitle().split(";")[0]);
        String noteContent = (currentNode == null )? "":currentNode.getContent();
        noteDate = (currentNode == null )? "":currentNode.getDate();
        selectedColor = (currentNode == null )? Color.WHITE:currentNode.getColor();
        imagePath = (currentNode == null )? "":currentNode.getImagePath();
        drawingData = (currentNode == null )? null :FileUtils.loadFileFromPath(currentNode.getImagePath());
        DrawingActivity.DrawingDataManager.clearDrawingData();
        /*
        // NEW: Retrieve the pinned status from the intent
        isPinned = getIntent().getBooleanExtra("note_is_pinned", false);
        // NEW: Retrieve the note's order from the intent
        noteOrder = getIntent().getIntExtra("note_order", -1);*/
        isPinned = (currentNode == null )? false: currentNode.isPinned();
        noteOrder = (currentNode == null )? -1:currentNode.getOrder();
        // setting the imagesketch from the database
        if(drawingData != null && drawingData.length > 0){
            Bitmap savedBitmap = noteRepository.loadImageFromInternalStorage(imagePath);
            // Check if the bitmap was successfully created
            if (savedBitmap != null) {
                // Assign the bitmap to your ImageView and make it visible
                imagesketch.setImageBitmap(savedBitmap);
                imageframelayout.setVisibility(View.VISIBLE);
                imagesketch.setVisibility(View.VISIBLE);
                imageCard.setVisibility(View.VISIBLE);
                clearImageButton.setVisibility(View.VISIBLE);
            }
        }

        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
        if (noteId != -1) {
            titleText.setText(noteTitle);
            resultText.setText(noteContent);
            resultBuilder.append(noteContent);
            titleBuilder.append(noteTitle);
        } else {
            noteDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
            titleText.setHint("Untitled");
        }
        // setting background
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

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);

        // NEW: Call the method to set the correct pin icon when the activity is created.
        setPinIcon(isPinned);
        clearDrawingButton = findViewById(R.id.clearDrawingButton);
        clearDrawingButton.setOnClickListener(v -> {
            //drawingView.setColor(Color.TRANSPARENT);
            drawingView.setErasing(true);
            isNoteModified = true;
        });
        resultText.setVisibility(View.VISIBLE);
        drawingView.setVisibility(View.GONE);

        handler = new Handler(Looper.getMainLooper());

        // Inline suggestion logic
        resultText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isNoteModified = true;

                // Clear current hint if user types anything other than accepting it
                if (!currentHint.isEmpty()) {
                    // Check if typed text still matches the prefix of the hint
                    String typedText = s.toString();
                    String expected = typedText + currentHint;
                    if (!expected.startsWith(typedText)) {
                        currentHint = "";
                        hintTextView.setText(typedText);
                    }
                }

                // Debounce suggestion generation
                if (suggestionRunnable != null) {
                    handler.removeCallbacks(suggestionRunnable);
                }

                if (s.length() > 0 && s.charAt(s.length() - 1) == ' ') {
                    suggestionRunnable = () -> generateSuggestions(s.toString());
                    handler.postDelayed(suggestionRunnable, 300); // reduced debounce for speed
                } else if (s.length() == 0) {
                    hintTextView.setText("");
                    currentHint = "";
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
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
        cameraScanButton.setOnClickListener(v -> openCamera());
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
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString());

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
                    if (titleText.hasFocus()) {
                        titleBuilder.append(spokenText).append(" ");
                        titleText.setText(titleBuilder.toString());
                    } else {
                        WordProcessor wordProcessor = new WordProcessor();
                        wordProcessor.processWord(spokenText,resultBuilder);

                        if (spokenText.equalsIgnoreCase("done") || spokenText.equalsIgnoreCase("stop")) {
                            isListening = false;
                            hintTextView.setVisibility(View.INVISIBLE);
                            speechRecognizer.stopListening();
                            listeningProgress.setVisibility(ProgressBar.GONE);
                            return;
                        }
                        else if(spokenText.contains("save")) {
                            saveNote();
                            isListening = false;
                            hintTextView.setVisibility(View.INVISIBLE);
                            speechRecognizer.stopListening();
                            listeningProgress.setVisibility(ProgressBar.GONE);
                            return;
                        }
                    }



                    //resultBuilder.append(spokenText).append(" ");
                    //resultText.append(spokenText);
                    isNoteModified = true;
                    if (isListening) {
                        speechRecognizer.startListening(recognizerIntent);
                    }
                }
            }
            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (titleText.hasFocus()) {
                    titleText.setText(titleBuilder.toString() + partial.get(0));
                } else {
                    resultText.setText(resultBuilder.toString() + partial.get(0));
                }
                /*
                String joinedText = "";

                if (partial != null) {

                    joinedText = String.join(" ", partial);
                }

                if (partial != null && partial.size() > 0) {
                    resultText.setText(resultBuilder.toString() + joinedText);
                }*/
            }
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }
    private void openCamera() {
        Intent i = new Intent(Notepad.this, CameraActivity.class);
        cameraLauncher.launch(i);
    }
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String ocrText = result.getData().getStringExtra("ocr_text");
                    if (ocrText != null && !ocrText.isEmpty()) {
                        resultText.setText(ocrText);
                    } else {
                        Toast.makeText(this, "No text detected", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void performOcr(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    // Task completed successfully
                    String recognizedText = result.getText();
                    if (!recognizedText.trim().isEmpty()) {
                        // Append the recognized text to the current note content
                        String currentText = resultText.getText().toString();
                        if (!currentText.isEmpty() && !currentText.endsWith("\n")) {
                            currentText += "\n\n"; // Add spacing if there's existing text
                        }
                        resultText.setText(currentText + recognizedText);
                        resultText.setSelection(resultText.getText().length()); // Move cursor to end
                        isNoteModified = true;
                        Toast.makeText(this, "Text scanned and added!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "No text found in the image.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Task failed with an exception
                    Log.e(TAG, "ML Kit Text Recognition failed: " + e.getMessage());
                    Toast.makeText(this, "Text scanning failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void registerListeners(){
        clearImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingData = null;
                imageframelayout.setVisibility(View.GONE);
                saveNote();
            }
        });
        black_pen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.BLACK);
            }
        });
        voiceicon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the permission is already granted.
                if (ContextCompat.checkSelfPermission(Notepad.this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                    // --- Case 1: Permission is denied, but not permanently. ---
                    if (ActivityCompat.shouldShowRequestPermissionRationale(Notepad.this, android.Manifest.permission.RECORD_AUDIO)) {
                        // Show an explanation to the user via a dialog.
                        new AlertDialog.Builder(Notepad.this)
                                .setTitle(R.string.microphone_permission)
                                .setMessage(R.string.microphone_message)
                                .setPositiveButton(R.string.grant_text, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Request the permission again.
                                        ActivityCompat.requestPermissions(Notepad.this, new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
                                    }
                                })
                                .setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create()
                                .show();
                    } else {
                        // --- Case 2: Permission is permanently denied ("Don't ask again" was checked). ---
                        new AlertDialog.Builder(Notepad.this)
                                .setTitle(R.string.permission_denied)
                                .setMessage(R.string.permission_text)
                                .setPositiveButton(R.string.settings_text, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Direct the user to the app's settings page.
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create()
                                .show();
                    }

                } else {
                    // --- Case 3: Permission is already granted. ---
                    MediaPlayer mp = MediaPlayer.create(Notepad.this, R.raw.googleassistant);
                    if (mp != null) {
                        mp.start();
                        mp.setOnCompletionListener(mediaPlayer -> {
                            mediaPlayer.release();
                        });
                    }
                    if (!isListening) {
                        isListening = true;
                        hintTextView.setVisibility(View.VISIBLE);
                        speechRecognizer.startListening(recognizerIntent);
                        Toast.makeText(getApplicationContext(), R.string.listening_text, Toast.LENGTH_SHORT).show();
                    } else {
                        isListening = false;
                        hintTextView.setVisibility(View.INVISIBLE);
                        speechRecognizer.stopListening();
                        listeningProgress.setVisibility(ProgressBar.GONE);
                    }
                }
            }
        });
        toggleModeDrawSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        // NEW: OnClickListener for imagesketch
        imagesketch.setOnClickListener(v -> {
            // Check if there is an image loaded to the ImageView
            if (imagesketch.getDrawable() != null) {
                drawOnDrawingView();
                toggleMode();
            }
        });
        red_pen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setColor(Color.RED);
            }
        });
    }
    private void drawOnDrawingView(){
        if (imagePath != null && imagePath.length() > 0) {
            // Use a ViewTreeObserver to wait until the view is laid out
            // and its dimensions are available before loading the bitmap.
            drawingView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // Ensure the view has valid dimensions before loading the data
                    if (drawingView.getWidth() > 0 && drawingView.getHeight() > 0) {
                        drawingView.setDrawingData(drawingData);
                        // Remove the listener to avoid repeated calls
                        drawingView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
        }
    }
    private void toggleMode() {
        isDrawingMode = !isDrawingMode;
        if (isDrawingMode) {
            titleText.setVisibility(View.GONE);
            isDirty = true;
            red_pen.setVisibility(View.VISIBLE);
            black_pen.setVisibility(View.VISIBLE);
            voiceicon.setVisibility(View.GONE);
            clearDrawingButton.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.GONE);
            resultText.setVisibility(View.GONE);
            imagesketch.setVisibility(View.GONE); // Hide the ImageView when drawing
            imageCard.setVisibility(View.GONE);
            imageframelayout.setVisibility(View.GONE);
            clearImageButton.setVisibility(View.GONE);
            drawingView.setVisibility(View.VISIBLE);
            toggleModeDrawSave.setVisibility(View.VISIBLE);

        } else {
            titleText.setVisibility(View.VISIBLE);
            isDirty = false;
            voiceicon.setVisibility(View.VISIBLE);
            red_pen.setVisibility(View.GONE);
            black_pen.setVisibility(View.GONE);
            toolbar.setVisibility(View.VISIBLE);
            clearDrawingButton.setVisibility(View.GONE);
            resultText.setVisibility(View.VISIBLE);
            if (imagesketch.getDrawable() != null) { // Only show imagesketch if it has an image
                imagesketch.setVisibility(View.VISIBLE);
                imageCard.setVisibility(View.VISIBLE);
                clearImageButton.setVisibility(View.VISIBLE);
                imageframelayout.setVisibility(View.VISIBLE);
            }
            drawingView.setVisibility(View.GONE);
            toggleModeDrawSave.setVisibility(View.GONE);
        }
    }

    // UPDATED: Override onBackPressed to check for unsaved changes
    @Override
    public void onBackPressed() {
        if(isDirty){
            isDirty = !isDirty;
            if(drawingData != null && drawingData.length > 0) {
                saveNote();
            }
            toggleMode();
        }
        else {
            if (isNoteModified) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.save_note_text)
                        .setMessage(R.string.save_note_message)
                        .setPositiveButton(R.string.save_menu, (dialog, which) -> saveNote())
                        .setNegativeButton(R.string.discard_text, (dialog, which) -> supportFinishAfterTransition())
                        .setNeutralButton(R.string.cancel_text, (dialog, which) -> {
                        })
                        .show();
            } else {
                supportFinishAfterTransition();
            }
        }
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_draw){
            drawOnDrawingView();
            toggleMode();
            return true;
        }
        else if (id == R.id.action_save) {
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

        final String[] colorNames = {getApplicationContext().getString(R.string.white_text),
                getApplicationContext().getString(R.string.pink_text),
                getApplicationContext().getString(R.string.yellow_text),
                getApplicationContext().getString(R.string.silver_text),
                getApplicationContext().getString(R.string.blue_text)};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.background_color_choose);
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

    public void saveNote() {
        NotesWidgetProvider.refreshWidget(getApplicationContext());

        String content = resultText.getText().toString().trim();
        WordTokenizer tokenizer = new WordTokenizer(content);
        List<String> labels = tokenizer.getTokenizedWords();
        if(labels == null) {
            Log.d(TAG, "LABELS is actually NULL!");
        } else if(labels.size() == 0) {
            Log.d(TAG, "LABELS is empty (size=0) with content: '" + content + "'");
        } else if(labels.size() == 1) {
            Log.d(TAG, "LABELS has 1 item: " + labels.get(0) + " with content: '" + content + "'");
        } else {
            Log.d(TAG, "LABELS: " + labels.get(0) + " : " + labels.get(1) + " (total=" + labels.size() + ")");
        }
        String title = titleText.getText().toString().trim() + ";"+ labels.get(0) + ";" + labels.get(1);
        byte[] drawingDataToSave = isDrawingMode ? drawingView.getDrawingData() : this.drawingData;
        byte[] drawingData = (drawingView.getDrawingData() == null || drawingView.getDrawingData().length == 0) ? this.drawingData: drawingView.getDrawingData() ;
        if (title.isEmpty() && content.isEmpty() && (imagePath == null || imagePath.isEmpty())) {
            Toast.makeText(this, "Note is empty, not saved.", Toast.LENGTH_SHORT).show();
            isNoteModified = false;
            supportFinishAfterTransition();
            return;
        }
        String newimagePath = "";
        if(drawingData != null && drawingData.length > 0){
            Bitmap drawingBitmap = BitmapFactory.decodeByteArray(drawingData, 0, drawingData.length);
            if(drawingBitmap != null){
                String filename = "drawing_" + System.currentTimeMillis() + ".png";

                newimagePath = noteRepository.saveImageToInternalStorage(drawingBitmap, filename);

            }
        }
        int colorToSave = Color.WHITE;
        Drawable background = mainContentLayout.getBackground();
        if (background instanceof ColorDrawable) {
            colorToSave = ((ColorDrawable) background).getColor();
        }

        final int finalColorToSave = colorToSave;
        final String imagepathfinal = newimagePath;
        Executors.newSingleThreadExecutor().execute(() -> {
            if (noteId != -1) {
                // Update existing note with the new imagePath
                Note existingNote = new Note(noteId, title, content, receivedDateFromActivities, finalColorToSave, noteOrder, isPinned, imagepathfinal);
                noteRepository.updateNote(existingNote);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Note updated!", Toast.LENGTH_SHORT).show();
                    isNoteModified = false;
                    supportFinishAfterTransition();
                });
            } else {
                // Create a new note with the new imagePath
                Note newNote = new Note(title, content, receivedDateFromActivities, finalColorToSave, 0, isPinned, imagepathfinal);
                noteRepository.addNote(newNote);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
                    isNoteModified = false;
                    supportFinishAfterTransition();
                });
            }
        });
        this.drawingData = drawingDataToSave;

        if (drawingData != null && drawingData.length > 0) {
            if(drawingDataToSave != null && drawingDataToSave.length > 0){
                Bitmap savedBitmap = BitmapFactory.decodeByteArray(drawingDataToSave, 0, drawingDataToSave.length);
                noteRepository.saveBytesToFile(drawingDataToSave,imagePath);
                if (savedBitmap != null) {
                    imagesketch.setImageBitmap(savedBitmap);
                    imagesketch.setVisibility(View.VISIBLE);
                    imageCard.setVisibility(View.VISIBLE);

                    imageframelayout.setVisibility(View.VISIBLE);
                    clearImageButton.setVisibility(View.VISIBLE);
                }
            }

        } else {
            imagesketch.setImageDrawable(null);
            imagesketch.setVisibility(View.GONE);
            imageCard.setVisibility(View.GONE);
            imageframelayout.setVisibility(View.GONE);
            clearImageButton.setVisibility(View.GONE);
        }

        isNoteModified = false;
        supportFinishAfterTransition();
    }
    /*

    public void saveNote() {
        String title = titleText.getText().toString().trim();
        String content = resultText.getText().toString().trim();

        // This variable will hold the final image path for the database.
        // It starts with the existing path.
        String finalImagePath = this.imagePath;

        // Check if the user was in drawing mode and if a drawing exists.
        if (isDrawingMode) {
            byte[] drawingDataFromView = drawingView.getDrawingData();
            if (drawingDataFromView != null && drawingDataFromView.length > 0) {
                // A new drawing was created, save it and update the path.
                Bitmap drawingBitmap = BitmapFactory.decodeByteArray(drawingDataFromView, 0, drawingDataFromView.length);
                if (drawingBitmap != null) {
                    String filename = "drawing_" + System.currentTimeMillis() + ".png";
                    finalImagePath = noteRepository.saveImageToInternalStorage(drawingBitmap, filename);
                }
            } else {
                // The user was in drawing mode but cleared the drawing.
                // We should delete the old image file if it exists.
                if (this.imagePath != null && !this.imagePath.isEmpty()) {
                    FileUtils.deleteFile(this.imagePath);
                }
                finalImagePath = ""; // Set the path to empty
            }
        }

        if (title.isEmpty() && content.isEmpty() && (finalImagePath == null || finalImagePath.isEmpty())) {
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
            Note noteToSave;
            if (noteId != -1) {
                // Update existing note with the new imagePath
                noteToSave = new Note(noteId, title, content, getCurrentDate(), finalColorToSave, noteOrder, isPinned, finalImagePath);
                noteRepository.updateNote(noteToSave);
            } else {
                // Create a new note with the new imagePath
                noteToSave = new Note(title, content, getCurrentDate(), finalColorToSave, 0, isPinned, finalImagePath);
                noteRepository.addNote(noteToSave);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Note updated!", Toast.LENGTH_SHORT).show();
                isNoteModified = false;
                supportFinishAfterTransition();
            });
        });

        // Update the in-memory drawing data after the save operation
        if (finalImagePath != null && !finalImagePath.isEmpty()) {
            this.drawingData = FileUtils.loadFileFromPath(finalImagePath);
        } else {
            this.drawingData = null;
        }

        if (this.drawingData != null && this.drawingData.length > 0) {
            Bitmap savedBitmap = BitmapFactory.decodeByteArray(this.drawingData, 0, this.drawingData.length);
            if (savedBitmap != null) {
                imagesketch.setImageBitmap(savedBitmap);
                imagesketch.setVisibility(View.VISIBLE);
            }
        } else {
            imagesketch.setImageDrawable(null);
            imagesketch.setVisibility(View.GONE);
        }

        isNoteModified = false;
        supportFinishAfterTransition();
    }
     */
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
    /**
     * Replaces the deprecated AsyncTask with a modern Thread-based approach.
     * Generates text suggestions in the background using the Gemini API.
     */
    private void generateSuggestions(String text) {
        if (text == null || text.trim().isEmpty()) {
            runOnUiThread(() -> {
                hintTextView.setText("");
                currentHint = "";
            });
            return;
        }

        // Cancel previous scheduled suggestion
        if (suggestionRunnable != null) {
            handler.removeCallbacks(suggestionRunnable);
        }

        suggestionRunnable = () -> {
            // Take only last 3 words for context
            String[] words = text.trim().split("\\s+");
            int start = Math.max(0, words.length - 3);
            String promptFragment = String.join(" ", Arrays.copyOfRange(words, start, words.length));

            String prompt = "Complete the following with a short suggestion. Do not include a period.\n\n" + promptFragment;

            OkHttpClient client = new OkHttpClient();

            try {
                JSONObject jsonBody = new JSONObject();
                JSONObject contents = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt);
                parts.put(textPart);
                contents.put("parts", parts);
                jsonBody.put("contents", new JSONArray().put(contents));

                RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .build();

                // Asynchronous call
                client.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        Log.e(TAG, "Gemini API call failed: " + e.getMessage());
                        runOnUiThread(() -> {
                            hintTextView.setText("");
                            currentHint = "";
                        });
                    }

                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                        if (!response.isSuccessful() || response.body() == null) {
                            runOnUiThread(() -> {
                                hintTextView.setText("");
                                currentHint = "";
                            });
                            return;
                        }

                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray candidates = jsonResponse.getJSONArray("candidates");

                            if (candidates.length() > 0) {
                                JSONObject firstCandidate = candidates.getJSONObject(0);
                                JSONObject content = firstCandidate.getJSONObject("content");
                                JSONArray partsArray = content.getJSONArray("parts");

                                if (partsArray.length() > 0) {
                                    String generatedText = partsArray.getJSONObject(0).getString("text").trim();

                                    // Remove the repeated last word from suggestion
                                    String lastWord = words[words.length - 1];
                                    if (generatedText.toLowerCase().startsWith(lastWord.toLowerCase())) {
                                        generatedText = generatedText.substring(lastWord.length()).trim();
                                    }

                                    final String finalGeneratedText = generatedText;
                                    runOnUiThread(() -> {
                                        currentHint = finalGeneratedText;
                                        hintTextView.setText(text + currentHint);
                                    });
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing Gemini response: " + e.getMessage(), e);
                            runOnUiThread(() -> {
                                hintTextView.setText("");
                                currentHint = "";
                            });
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error preparing Gemini request: " + e.getMessage(), e);
            }
        };

        // Post with short debounce (300ms)
        handler.postDelayed(suggestionRunnable, 300);
    }
    public void init(){
        setContentView(R.layout.notepad_layout);
        voiceicon = findViewById(R.id.voiceicon);
        listeningProgress = findViewById(R.id.listeningProgress);
        imageframelayout = findViewById(R.id.imageframelayout);
        cameraScanButton = findViewById(R.id.cameraScanButton);
        clearImageButton =  findViewById(R.id.clearImageButton);
        imageCard = findViewById(R.id.imageCard);
        resultText = findViewById(R.id.resultText);
        titleText = findViewById(R.id.noteTitleEditText);
        mainContentLayout = findViewById(R.id.main_content_layout);
        hintTextView = findViewById(R.id.hintTextView);
        toggleModeDrawSave =  findViewById(R.id.toggleModeDrawSave);
        red_pen = findViewById(R.id.red_pen);
        toggleModeDrawSave.setVisibility(View.GONE);
        imagesketch = findViewById(R.id.imagesketch);
        black_pen = findViewById(R.id.black_pen);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
