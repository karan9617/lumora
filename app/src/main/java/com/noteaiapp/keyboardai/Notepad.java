package com.noteaiapp.keyboardai;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.AlignmentSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
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
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.camera.CameraActivity;
import com.noteaiapp.keyboardai.data.FileUtils;
import com.noteaiapp.keyboardai.data.NoteRepository;
import com.noteaiapp.keyboardai.data.WordTokenizer;
import com.noteaiapp.keyboardai.processor.WordProcessor;
import com.noteaiapp.keyboardai.ui.DrawingView;
import com.noteaiapp.keyboardai.ui.LinkPreviewHelper;
import com.noteaiapp.keyboardai.widget.NotesWidgetProvider;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.json.JSONArray;
import org.json.JSONObject;

public class Notepad extends AppCompatActivity {

    private static final String TAG = "com.noteaiapp.keyboardai";
    ProgressBar correctionProgressBar;
    private String currentUserUuid = "";
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseStorage storage = FirebaseStorage.getInstance();

    public static final String EXTRA_FOLDER_NAME = "FOLDER_NAME";
    private static final int PERMISSION_REQUEST_CODE = 1;
    // camera
    private static final int CAMERA_PERMISSION_CODE = 100;

    private static final int REQUEST_CAMERA_PERMISSION = 100;
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
    ImageButton clearDrawingButton,clearImageButton,black_pen,red_pen,boldButton,italicsButton,linkCreationButton,pdfUploadButton,leftAlignButton,centerAlignButton,rightAlignButton;
    private String noteDate;
    private byte[] drawingData;
    private String imagePath;
    private boolean isDrawingMode = false;
    private RelativeLayout mainContentLayout;
    // Search state management (now only used to find indices for highlighting)
    private List<Integer> searchIndices = new ArrayList<>();
    LinearLayout linear_layout_main;
    private final int HIGHLIGHT_COLOR = Color.YELLOW;
    private String folderName = "";
    private int selectedColor = Color.WHITE;
    private boolean isNoteModified = false,isDirty = false;
    MaterialToolbar toolbar;
    private Handler handler;
    private Runnable suggestionRunnable;
    private final long DELAY = 500;
    SearchView search_view;
    private ImageView imagesketch,voiceicon,linkImage;
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
    private ActivityResultLauncher<Intent> pdfFileLauncher;
    public void loadNoteData(){
        String noteTitle = getIntent().getStringExtra("note_title");
        String noteContent = getIntent().getStringExtra("note_content");
        noteDate = getIntent().getStringExtra("note_date");
        selectedColor = getIntent().getIntExtra("note_color", Color.WHITE);
        imagePath = getIntent().getStringExtra("note_image_path"); // Retrieve the image path
        isPinned = getIntent().getBooleanExtra("note_is_pinned", false);
        // NEW: Retrieve the note's order from the intent
        noteOrder = getIntent().getIntExtra("note_order", -1);
        if (this.currentUserUuid != null && this.currentUserUuid.length() != 0) {
            titleText.setText(noteTitle);
            loadNote(noteContent);
            //resultText.setText(noteContent);
            resultBuilder.append(noteContent);
            titleBuilder.append(noteTitle);
        } else {
            noteDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
            titleText.setHint("Untitled");
        }
    }
    private void fetchNoteFromFirebase(String cloudId) {

        if (currentUser == null || cloudId == null || cloudId.length() == 0) {
            Log.w(TAG, "User not logged in, falling back to local database.");
            loadNoteData();
            return;
        }

        String userId = currentUser.getUid();
        // Show a ProgressBar if you have one
        Log.d(TAG, "List item user uid:"+userId);
        db.collection("users").document(userId).collection("notes").document(cloudId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Successfully fetched list note from Firestore.");
                        Note cloudNote = documentSnapshot.toObject(Note.class);
                        Log.d(TAG, "cloudNote :"+cloudNote.getTitle()+"|cloudNote content:"+cloudNote.getContent());

                        if (cloudNote != null) {
                            populateUiWithNoteData(cloudNote, cloudId);
                        }
                    } else {
                        Log.w(TAG, "List note not found in Firestore, falling back to local.");
                        //loadNoteData();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch from Firestore. Falling back to local.", e);
                    //loadNoteData();
                });
    }
    public void populateUiWithNoteData(Note currentNode, String cloudId){
        String noteTitle = currentNode.getTitle().split(";")[0];
        String noteContent = currentNode.getContent();
        noteDate = this.receivedDateFromActivities;
        selectedColor = (currentNode == null )? Color.WHITE:currentNode.getColor();
        imagePath = (currentNode == null )? "":currentNode.getImagePath();
        drawingData = (currentNode == null )? null :FileUtils.loadFileFromPath(currentNode.getImagePath());
        isPinned = (currentNode == null )? false: currentNode.isPinned();
        noteOrder = (currentNode == null )? -1:currentNode.getOrder();
        if (cloudId != null && cloudId.length() != 0) {
            titleText.setText(noteTitle);
            loadNote(noteContent);
            //resultText.setText(noteContent);
            resultBuilder.append(noteContent);
            titleBuilder.append(noteTitle);
        } else {
            noteDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
            titleText.setHint("Untitled");
        }
        if (imagePath != null && !imagePath.isEmpty()) {
            Log.d(TAG, "Populating UI with image from path: " + imagePath);
            imageframelayout.setVisibility(View.VISIBLE);
            imagesketch.setVisibility(View.VISIBLE);
            imageCard.setVisibility(View.VISIBLE);
            clearImageButton.setVisibility(View.VISIBLE);

            // Use Glide to load the image. It handles both local paths and cloud URLs.
            Glide.with(this).load(imagePath).into(imagesketch);
        } else {
            Log.d(TAG, "Note has no image path. Hiding image views.");
            imageframelayout.setVisibility(View.GONE);
            imagesketch.setVisibility(View.GONE);
            imageCard.setVisibility(View.GONE);
            clearImageButton.setVisibility(View.GONE);
        }
        if(drawingData != null && drawingData.length > 0){
            Glide.with(this)
                    .asBitmap() // Important: We need a Bitmap for the drawing view
                    .load(imagePath)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            // This callback runs when Glide has finished downloading/loading the Bitmap.
                            // Now, set it as the background for the DrawingView.
                            imagesketch.setImageBitmap(resource);
                            imageframelayout.setVisibility(View.VISIBLE);
                            imagesketch.setVisibility(View.VISIBLE);
                            imageCard.setVisibility(View.VISIBLE);
                            clearImageButton.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Handle case where the view is cleared
                        }
                    });
            imagesketch.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        EdgeToEdge.enable(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //firebase storage
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();
        mAuth =FirebaseAuth.getInstance();
        currentUserUuid = (getIntent().getStringExtra("note_font_size") == null)? "": getIntent().getStringExtra("note_font_size");

        postponeEnterTransition();
        init();
        registerListeners();
        RelativeLayout sideSheet = findViewById(R.id.side_sheet);
        LinearLayout sideSheetHandle = findViewById(R.id.side_sheet_handle);

// Initially hide the side sheet (off-screen to the right)
        sideSheet.setVisibility(View.VISIBLE);
        sideSheet.setTranslationX(130); // Only the handle is visible (80dp is the sheet width)
        final boolean[] isOpen = {false};

// Add touch listener for dragging
        sideSheetHandle.setOnTouchListener(new View.OnTouchListener() {
            private float startX;
            private float startTranslationX;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startTranslationX = sideSheet.getTranslationX();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - startX;
                        float newTranslationX = startTranslationX + deltaX;

                        // Constrain movement between -100 (fully open, extends left) and 180 (closed)
                        if (newTranslationX >= -100 && newTranslationX <= 180) {
                            sideSheet.setTranslationX(newTranslationX);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        // Snap to open or closed based on position
                        float currentTranslation = sideSheet.getTranslationX();
                        if (currentTranslation < 40) {
                            // Snap to open (negative value makes it extend more to the left)
                            sideSheet.animate()
                                    .translationX(-100)
                                    .setDuration(200)
                                    .start();
                            isOpen[0] = true;
                        } else {
                            // Snap to closed
                            sideSheet.animate()
                                    .translationX(180)
                                    .setDuration(200)
                                    .start();
                            isOpen[0] = false;
                        }
                        return true;
                }
                return false;
            }
        });
// Also add click listener for quick toggle
        sideSheetHandle.setOnClickListener(v -> {
            if (isOpen[0]) {
                // Close
                sideSheet.animate()
                        .translationX(80)
                        .setDuration(300)
                        .start();
                isOpen[0] = false;
            } else {
                // Open
                sideSheet.animate()
                        .translationX(0)
                        .setDuration(300)
                        .start();
                isOpen[0] = true;
            }
        });
        noteRepository = new NoteRepository(this);
        drawingView = findViewById(R.id.drawingView);
        italicsButton = findViewById(R.id.italicsButton);
        boldButton = findViewById(R.id.boldButton);
        if (boldButton != null) {
            // Set the listener to apply the BOLD style to selected text
            boldButton.setOnClickListener(v -> applyStyleToSelection(Typeface.BOLD));
        }
        if(italicsButton != null){
            italicsButton.setOnClickListener((v -> applyStyleToSelection(Typeface.ITALIC)));
        }
        this.folderName = (getIntent().getStringExtra(EXTRA_FOLDER_NAME) == null)? "":(getIntent().getStringExtra(EXTRA_FOLDER_NAME));
        this.receivedDateFromActivities = (getIntent().getStringExtra(DATE_EXTRA_KEY) == null)? getCurrentDate():(getIntent().getStringExtra(DATE_EXTRA_KEY));

        //Note currentNode = noteRepository.getNoteById(noteId);
        fetchNoteFromFirebase(currentUserUuid);

        DrawingActivity.DrawingDataManager.clearDrawingData();

        // setting the imagesketch from the database

        // setting background
        mainContentLayout.setBackgroundColor(selectedColor);
        titleText.setBackground(null);
        resultText.setBackground(null);
        hintTextView.setBackground(null);
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
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

                    loadNote(resultBuilder.toString() + partial.get(0));
                    //resultText.setText(resultBuilder.toString() + partial.get(0));

                }
            }
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void applyLinkToSelection() {
        final Editable editable = resultText.getText();
        if (editable == null) return;
        final int start = resultText.getSelectionStart();
        final int end = resultText.getSelectionEnd();
        if (start == end) {
            Toast.makeText(this, "Please select text to create a link.", Toast.LENGTH_SHORT).show();
            return;
        }
        final int spanStart = Math.min(start, end);
        final int spanEnd = Math.max(start, end);

        // Create an EditText for the URL input
        final EditText urlInput = new EditText(this);
        urlInput.setHint("https://www.example.com");
        urlInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);

        // Check for existing link to allow editing/removal
        URLSpan[] existingSpans = editable.getSpans(spanStart, spanEnd, URLSpan.class);

        if (existingSpans.length > 0) {
            // If there's an existing span, use its URL as initial text
            urlInput.setText(existingSpans[0].getURL());
        }
        new AlertDialog.Builder(this)
                .setTitle("Enter Link URL")
                .setView(urlInput)
                .setPositiveButton(existingSpans.length > 0 ? "Update" : "Add", (dialog, which) -> {
                    String url = urlInput.getText().toString().trim();

                    // 1. Remove all existing URL spans in the selection first
                    for (URLSpan span : existingSpans) {
                        editable.removeSpan(span);
                    }

                    if (!url.isEmpty()) {
                        // Ensure the URL has a scheme (like http:// or https://)
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://" + url;
                        }

                        // 2. Apply the new URL span
                        URLSpan urlSpan = new URLSpan(url);
                        editable.setSpan(urlSpan, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        isNoteModified = true;
                        Toast.makeText(this, "Link applied!", Toast.LENGTH_SHORT).show();
                    } else {
                        // If input is cleared, it acts as a removal
                        isNoteModified = true;
                        Toast.makeText(this, "Link removed.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .setNeutralButton(existingSpans.length > 0 ? "Remove Link" : null, (dialog, which) -> {
                    for (URLSpan span : existingSpans) {
                        editable.removeSpan(span);
                    }
                    isNoteModified = true;
                    Toast.makeText(this, "Link removed.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
    private int getParagraphBoundary(Editable editable, int index, boolean findStart) {
        if (editable == null) return index;

        // Ensure index is within bounds
        if (index < 0) return 0;
        if (index > editable.length()) return editable.length();

        if (findStart) {
            // Find the preceding newline or start of text
            while (index > 0) {
                if (editable.charAt(index - 1) == '\n') {
                    break;
                }
                index--;
            }
            return index;
        } else {
            // Find the following newline or end of text
            while (index < editable.length()) {
                if (editable.charAt(index) == '\n') {
                    // Include the newline in the span for the paragraph boundary requirement
                    index++;
                    break;
                }
                index++;
            }
            return index;
        }
    }

    private void insertPdfLink(Uri uri, String fileName) {
        // The display text will be the file name, possibly prefixed with a PDF icon/label
        String linkText = "[PDF] " + fileName;

        // 1. Append a new line and the link text to the editable
        Editable editable = resultText.getText();
        int start = editable.length();

        // Ensure the link is on a new line
        if (start > 0 && editable.charAt(start - 1) != '\n') {
            editable.append("\n");
            start++;
        }

        editable.append(linkText);
        int end = editable.length();

        // 2. Create and apply the URLSpan using the file's Content URI as the URL
        // The LinkMovementMethod will handle the click and use ACTION_VIEW with this URI.
        URLSpan fileLinkSpan = new URLSpan(uri.toString());
        editable.setSpan(fileLinkSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 3. Optional: Add a style span to make it bold or colored for better visibility
        StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
        editable.setSpan(boldSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        isNoteModified = true;
        Toast.makeText(this, "PDF link inserted: " + fileName, Toast.LENGTH_LONG).show();
    }

    public String saveNoteContent() {
        // HtmlCompat.toHtml converts Spannable text (StyleSpans) into standard HTML tags (<b>, <i>).
        return HtmlCompat.toHtml(resultText.getText(), HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
    }
    public void loadNote(String savedHtml) {
        if (savedHtml == null || savedHtml.isEmpty()) {
            resultText.setText("");
            return;
        }

        // 1. Load the HTML content using the best possible flag
        resultText.setText(HtmlCompat.fromHtml(
                savedHtml,
                HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH
        ));

        // 2. --- Manual Alignment Re-Application Fix ---

        // Determine the intended alignment from the saved HTML string
        Layout.Alignment targetAlignment = Layout.Alignment.ALIGN_NORMAL; // Default is left/start

        // Check for right alignment
        if (savedHtml.contains("align=\"right\"")) {
            targetAlignment = Layout.Alignment.ALIGN_OPPOSITE;
        }
        // Check for center alignment
        else if (savedHtml.contains("align=\"center\"")) {
            targetAlignment = Layout.Alignment.ALIGN_CENTER;
        }

        // Apply the determined alignment to the entire text
        Spannable spannable = (Spannable) resultText.getText();

        // Check if an AlignmentSpan already exists and matches the target.
        // If the AlignmentSpan is ALIGN_NORMAL, it's often missing.
        AlignmentSpan[] currentSpans = spannable.getSpans(0, spannable.length(), AlignmentSpan.class);

        boolean alignmentFound = false;
        if (currentSpans.length > 0) {
            for (AlignmentSpan span : currentSpans) {
                if (span.getAlignment() == targetAlignment) {
                    alignmentFound = true;
                    break;
                }
            }
        }

        // If the correct alignment wasn't found or applied by HtmlCompat, apply it manually.
        if (!alignmentFound && targetAlignment != Layout.Alignment.ALIGN_NORMAL) {
            // Remove any existing alignment spans first to prevent conflicts (though they shouldn't exist if alignmentFound is false)
            for (AlignmentSpan span : currentSpans) {
                spannable.removeSpan(span);
            }

            // Apply the new alignment span to the entire text
            spannable.setSpan(
                    new AlignmentSpan.Standard(targetAlignment),
                    0, // Start of the text
                    spannable.length(), // End of the text
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    private void applyStyleToSelection(int style) {
        Editable editable = resultText.getText();
        if (editable == null) return;

        int start = resultText.getSelectionStart();
        int end = resultText.getSelectionEnd();

        if (start == end) {
            Toast.makeText(this, "Please select text to apply formatting.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }

        StyleSpan[] existingSpans = editable.getSpans(start, end, StyleSpan.class);
        boolean isStylePresent = false;

        // Check if the style is already present in the selection range and remove it (toggle)
        for (StyleSpan span : existingSpans) {
            if (span.getStyle() == style) {
                editable.removeSpan(span);
                isStylePresent = true;
                break;
            }
        }

        // If the style was not present, apply the new style span
        if (!isStylePresent) {
            StyleSpan styleSpan = new StyleSpan(style);
            // SPAN_EXCLUSIVE_EXCLUSIVE is generally safe for formatting text
            editable.setSpan(styleSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        isNoteModified = true;
    }
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
            if (result == null || result.isEmpty()) {
                result = "Untitled PDF.pdf";
            }
        }
        return result;
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


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void openPdfFilePicker() {
        // Use Intent.ACTION_OPEN_DOCUMENT to allow persistent access to the URI
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Request persistence read permission
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pdfFileLauncher.launch(intent);
    }
    private void applyAlignmentToSelection(Layout.Alignment alignment) {
        Editable editable = resultText.getText();
        if (editable == null) {
            return;
        }

        int start = resultText.getSelectionStart();
        int end = resultText.getSelectionEnd();

        // 1. Find the true paragraph boundaries based on the selection
        int paragraphStart = getParagraphBoundary(editable, start, true);
        int paragraphEnd = getParagraphBoundary(editable, end, false);

        // Safety check: if start and end are the same (no selection, just cursor)
        if (paragraphStart == paragraphEnd) {
            // If the text is empty, ensure start is 0
            if (editable.length() == 0) {
                paragraphStart = 0;
                paragraphEnd = 0;
            }
            // If text is not empty, use the cursor position to find the current paragraph
            else {
                int cursor = resultText.getSelectionStart();
                paragraphStart = getParagraphBoundary(editable, cursor, true);
                paragraphEnd = getParagraphBoundary(editable, cursor, false);
            }
        }

        // 2. Remove any existing AlignmentSpans in the corrected range
        AlignmentSpan[] spans = editable.getSpans(paragraphStart, paragraphEnd, AlignmentSpan.class);
        for (AlignmentSpan span : spans) {
            editable.removeSpan(span);
        }

        // 3. Apply the new AlignmentSpan
        // Only apply the span if the range is valid (i.e., not an empty document)
        if (paragraphStart != paragraphEnd || editable.length() > 0) {
            editable.setSpan(new AlignmentSpan.Standard(alignment),
                    paragraphStart,
                    paragraphEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Optional: Ensure the UI state of the buttons reflects the change
        updateAlignmentButtonState(alignment);
    }
    // You'll likely need this helper to update your button visual state (color/tint)
    private void updateAlignmentButtonState(Layout.Alignment currentAlignment) {
        // Define default/active colors (use your resource colors)
        int inactiveColor = Color.parseColor("#707070"); // Example inactive color
        int activeColor = Color.parseColor("#007AFF"); // Example active color (blue)

        // Null checks for buttons, ensure they are initialized in init()
        if (leftAlignButton == null || rightAlignButton == null) return;

        // Reset all buttons to inactive
        leftAlignButton.setColorFilter(inactiveColor);
        rightAlignButton.setColorFilter(inactiveColor);
        // centerAlignButton.setColorFilter(inactiveColor); // Uncomment if you add a center button

        // Set the active button
        if (currentAlignment == Layout.Alignment.ALIGN_NORMAL) {
            leftAlignButton.setColorFilter(activeColor);
        } else if (currentAlignment == Layout.Alignment.ALIGN_OPPOSITE) {
            rightAlignButton.setColorFilter(activeColor);
        }
        // else if (currentAlignment == Layout.Alignment.ALIGN_CENTER) {
        //     centerAlignButton.setColorFilter(activeColor); // Uncomment if you add a center button
        // }
    }
    private void clearHighlights() {
        Spannable spannable = resultText.getText();
        BackgroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan span : spans) {
            spannable.removeSpan(span);
        }
        searchIndices.clear();
    }

    /**
     * Finds all occurrences of the query in resultText and highlights them.
     * This version uses the `searchIndices` to store locations but only for highlighting purposes.
     * @param query The text to search for.
     */
    private void performSearch(String query) {
        clearHighlights();

        if (query == null || query.isEmpty()) {
            return;
        }

        String fullText = resultText.getText().toString().toLowerCase(Locale.getDefault());
        String lowerQuery = query.toLowerCase(Locale.getDefault());
        int index = fullText.indexOf(lowerQuery);

        while (index >= 0) {
            searchIndices.add(index);
            index = fullText.indexOf(lowerQuery, index + lowerQuery.length());
        }

        if (!searchIndices.isEmpty()) {
            highlightResults(query.length());
        }
    }

    /**
     * Applies the yellow highlight span to all found search results.
     */
    private void highlightResults(int queryLength) {
        Spannable spannable = resultText.getText();

        // Re-apply the highlight for all matching indices
        for (int index : searchIndices) {
            spannable.setSpan(
                    new BackgroundColorSpan(HIGHLIGHT_COLOR),
                    index,
                    index + queryLength,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        // When results are found, scroll the view to the first match
        if (!searchIndices.isEmpty()) {
            int firstMatchIndex = searchIndices.get(0);
            resultText.post(() -> {
                Layout layout = resultText.getLayout();
                if (layout != null) {
                    int line = layout.getLineForOffset(firstMatchIndex);
                    // Scroll to the line where the first match appears
                    resultText.scrollTo(0, layout.getLineTop(line));
                }
            });
        }
    }
    public void registerListeners(){
        pdfFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            // Grant persistence read permission access to this URI
                            final int takeFlags = result.getData().getFlags()
                                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            try {
                                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                            } catch (SecurityException e) {
                                Log.e(TAG, "Failed to take persistable URI permission. Content access may be temporary.", e);
                            }

                            String fileName = getFileName(uri);
                            insertPdfLink(uri, fileName);
                        }
                    }
                }
        );
        if (search_view != null) {
            search_view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    performSearch(query);
                    search_view.clearFocus(); // Hide keyboard on submit
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Search as the user types
                    performSearch(newText);
                    return true;
                }
            });

            // Clear highlights when the user closes the search view
            search_view.setOnCloseListener(() -> {
                clearHighlights();
                return false; // Return false to allow the default close action to happen
            });
        }
        if (leftAlignButton != null) {
            // ALIGN_NORMAL is typically Left alignment
            leftAlignButton.setOnClickListener(v -> applyAlignmentToSelection(Layout.Alignment.ALIGN_NORMAL));
        }
        if (rightAlignButton != null) {
            rightAlignButton.setOnClickListener(v -> applyAlignmentToSelection(Layout.Alignment.ALIGN_OPPOSITE));
        }
        if (centerAlignButton != null) {
            // ALIGN_OPPOSITE is typically Right alignment
            centerAlignButton.setOnClickListener(v -> applyAlignmentToSelection(Layout.Alignment.ALIGN_CENTER));
        }
        if (pdfUploadButton != null) {
            pdfUploadButton.setOnClickListener(v -> openPdfFilePicker());
        }
        if (linkCreationButton != null) {
            // Set the listener to apply the LINK to selected text
            linkCreationButton.setOnClickListener(v -> applyLinkToSelection());
        }
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
                                .setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
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
                        voiceicon.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.rounded_purple_background));
                        speechRecognizer.startListening(recognizerIntent);
                        Toast.makeText(getApplicationContext(), R.string.listening_text, Toast.LENGTH_SHORT).show();
                    } else {
                        isListening = false;
                        hintTextView.setVisibility(View.INVISIBLE);
                        voiceicon.setBackground(ContextCompat.getDrawable(getApplicationContext(),R.drawable.round_voice_bg));
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
    private void drawOnDrawingView() {
        //1. Get the drawable from the ImageView that Glide loaded the image into.
        Drawable drawable = imagesketch.getDrawable();

        // 2. Check if the drawable exists and is a BitmapDrawable.
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

            if (bitmap != null) {
                Log.d("Notepad", "Bitmap retrieved from imagesketch. Preparing to draw.");
                // 3. Convert the Bitmap into a byte array.
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                // 4. Update the activity's main drawingData variable.
                this.drawingData = byteArray;

                // 5. Use a ViewTreeObserver to safely set the data on the drawingView
                //    only after it has been measured and laid out on the screen.
                drawingView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Ensure the view has valid dimensions before setting the data
                        if (drawingView.getWidth() > 0 && drawingView.getHeight() > 0) {
                            Log.d("Notepad", "DrawingView is ready. Setting drawing data.");
                            drawingView.setDrawingData(drawingData);
                            // Remove the listener to avoid this from being called repeatedly.
                            drawingView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
            } else {
                Log.w("Notepad", "Could not get Bitmap from imagesketch's drawable.");
            }
        } else {
            Log.w("Notepad", "imagesketch does not contain a valid BitmapDrawable. Cannot edit.");
        }
    }

    private void toggleMode() {
        isDrawingMode = !isDrawingMode;
        if (isDrawingMode) {
            titleText.setVisibility(View.GONE);
            boldButton.setVisibility(View.GONE);
            italicsButton.setVisibility(View.GONE);
            leftAlignButton.setVisibility(View.GONE);
            rightAlignButton.setVisibility(View.GONE);
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
            boldButton.setVisibility(View.VISIBLE);
            leftAlignButton.setVisibility(View.VISIBLE);
            rightAlignButton.setVisibility(View.VISIBLE);
            italicsButton.setVisibility(View.VISIBLE);
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
        }
        else if (id == R.id.action_correct_text) { // NEW: Handle the correct text action
            correctTextWithGemini();
            return true;
        }
        else if (id == R.id.action_summarize_note) {
            summarizeNoteWithGemini();
            return true;
        }
        else if (id == R.id.action_translate) {
            showLanguageSelectionDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void showLanguageSelectionDialog() {
        final String[] languages = {getApplicationContext().getString(R.string.spanish_text),
                getApplicationContext().getString(R.string.french_text),
                getApplicationContext().getString(R.string.german_text),
                getApplicationContext().getString(R.string.japanese_text),
                getApplicationContext().getString(R.string.hindi_text),
                getApplicationContext().getString(R.string.russian_text)
               };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.translate_to)
                .setItems(languages, (dialog, which) -> {
                    String selectedLanguage = languages[which];
                    translateNoteWithGemini(selectedLanguage);
                });
        builder.create().show();
    }
    /**
     * Takes the current note text, translates it to the target language using the Gemini API,
     * and replaces the content of the EditText with the result.
     * @param targetLanguage The language to translate the text into (e.g., "Spanish").
     */
    private void translateNoteWithGemini(String targetLanguage) {
        String originalText = resultText.getText().toString();
        if (originalText.trim().isEmpty()) {
            Toast.makeText(this, "Note is empty, nothing to translate.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reuse the same progress bar you use for spell-check
        if (correctionProgressBar != null) {
            correctionProgressBar.setVisibility(View.VISIBLE);
        }

        // A very specific prompt for translation
        String prompt = "Translate the following text into " + targetLanguage + ". " +
                "Only return the translated text itself, without any extra phrases or explanations.\n\n" +
                "Text to translate:\n\"" + originalText + "\"";

        Executors.newSingleThreadExecutor().execute(() -> {
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
                        .url(API_URL) // Your existing API_URL
                        .post(body)
                        .build();

                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String translatedText = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim();

                    // --- THIS IS THE MODIFIED PART ---
                    runOnUiThread(() -> {
                        if (correctionProgressBar != null) correctionProgressBar.setVisibility(View.GONE);

                        // 1. Set the EditText to the new translated text
                        resultText.setText(translatedText);

                        // 2. Mark the note as modified so the user is prompted to save
                        isNoteModified = true;

                        // 3. Inform the user
                        Toast.makeText(Notepad.this, "Note translated to " + targetLanguage, Toast.LENGTH_SHORT).show();
                    });
                    // --- END OF MODIFIED PART ---

                } else {
                    runOnUiThread(() -> {
                        if (correctionProgressBar != null) correctionProgressBar.setVisibility(View.GONE);
                        Toast.makeText(Notepad.this, "Error: Translation failed.", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during Gemini translation: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    if (correctionProgressBar != null) correctionProgressBar.setVisibility(View.GONE);
                    Toast.makeText(Notepad.this, "An error occurred.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Takes the current text from the notepad, sends it to the Gemini API for summarization,
     * and appends the result to the end of the note.
     */
    private void summarizeNoteWithGemini() {
        String originalText = resultText.getText().toString();
        if (originalText.trim().isEmpty()) {
            Toast.makeText(this, "Note is empty, nothing to summarize.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reuse the same progress bar
        correctionProgressBar.setVisibility(View.VISIBLE);

        // A clear prompt for summarization
        String prompt = "Summarize the following text into a few concise bullet points. " +
                "Do not include any introductory phrases or headings.\n\n" +
                "Original text:\n\"" + originalText + "\"";

        Executors.newSingleThreadExecutor().execute(() -> {
            OkHttpClient client = new OkHttpClient();

            try {
                // Create the JSON payload for the Gemini API
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
                        .url(API_URL) // You already have this defined
                        .post(body)
                        .build();

                // Synchronous API call
                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray partsArray = content.getJSONArray("parts");
                    String summary = partsArray.getJSONObject(0).getString("text").trim();

                    // *** This is the key part: Append the summary to the existing text ***
                    String textToAppend = "\n\n---\n\n**Summarized Note:**\n" + summary;
                    Spannable summaryHtml = (Spannable) HtmlCompat.fromHtml(textToAppend, HtmlCompat.FROM_HTML_MODE_LEGACY);

                    // Update the UI on the main thread
                    runOnUiThread(() -> {
                        resultText.append(summaryHtml);
                        isNoteModified = true; // Mark the note as modified
                        correctionProgressBar.setVisibility(View.GONE);
                        Toast.makeText(Notepad.this, "Summary appended!", Toast.LENGTH_SHORT).show();
                    });

                } else {
                    // Handle API errors on the main thread
                    runOnUiThread(() -> {
                        correctionProgressBar.setVisibility(View.GONE);
                        Toast.makeText(Notepad.this, "Error: Could not get summary.", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during Gemini summary: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    correctionProgressBar.setVisibility(View.GONE);
                    Toast.makeText(Notepad.this, "An error occurred.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Takes the current text from the notepad, sends it to the Gemini API for correction,
     * and updates the EditText with the result.
     */
    private void correctTextWithGemini() {
        String originalText = resultText.getText().toString();
        if (originalText.trim().isEmpty()) {
            Toast.makeText(this, "Note is empty, nothing to correct.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show a loading indicator
        correctionProgressBar.setVisibility(View.VISIBLE);

        // The prompt is very important. We tell the AI exactly what to do.
        String prompt = "Correct the spelling and grammar of the following text. " +
                "Only return the corrected text, without any introductory phrases.\n\n" +
                "Original text:\n\"" + originalText + "\"\n\nCorrected text:";

        Executors.newSingleThreadExecutor().execute(() -> {
            OkHttpClient client = new OkHttpClient();

            try {
                // Create the JSON payload for the Gemini API
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
                        .url(API_URL) // You already have this defined
                        .post(body)
                        .build();

                // Synchronous API call
                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray partsArray = content.getJSONArray("parts");
                    String correctedText = partsArray.getJSONObject(0).getString("text").trim();

                    // Update the UI on the main thread
                    runOnUiThread(() -> {
                        resultText.setText(correctedText);
                        isNoteModified = true; // Mark the note as modified
                        correctionProgressBar.setVisibility(View.GONE);
                        Toast.makeText(Notepad.this, "Spellings corrected!", Toast.LENGTH_SHORT).show();
                    });

                } else {
                    // Handle API errors on the main thread
                    runOnUiThread(() -> {
                        correctionProgressBar.setVisibility(View.GONE);
                        Toast.makeText(Notepad.this, "Error: Could not correct text.", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during Gemini text correction: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    correctionProgressBar.setVisibility(View.GONE);
                    Toast.makeText(Notepad.this, "An error occurred.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // NEW: Method to handle toggling the pin status


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
    // DELETE your old 'storetofirebasestorage' and 'savetofirestore' methods
// ADD this new, combined method to Notepad.java

    private void uploadAndSyncNoteToFirebase(Note noteWithLocalPath, byte[] imageData, String filename) {
        if (imageData == null || imageData.length == 0) {
            Log.d(TAG, "Note " + noteWithLocalPath.getId() + " has no image. Saving metadata to Firestore.");
            // Set the image path to empty for Firestore and save directly.
            noteWithLocalPath.setImagePath("");
            db.collection("users").document(currentUser.getUid()).collection("notes").document(String.valueOf(noteWithLocalPath.getUserFirebaseId()))
                    .set(noteWithLocalPath)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Note " + noteWithLocalPath.getId() + " metadata saved to Firestore."))
                    .addOnFailureListener(e -> Log.w(TAG, "Error saving note " + noteWithLocalPath.getId() + " metadata to Firestore.", e));
            return;
        }
        String userId = currentUser.getUid();
        StorageReference imageRef = storage.getReference().child("images/" + userId + "/" + filename);
        // Start the upload and chain the tasks
        imageRef.putBytes(imageData)
                // 1. First, upload the file
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        // If upload fails, pass the exception down the chain
                        throw task.getException();
                    }
                    // 2. If upload succeeds, get the public download URL
                    Log.d(TAG, "Image uploaded, getting download URL...");
                    return imageRef.getDownloadUrl();
                })
                // 3. This listener receives the result of getDownloadUrl()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // --- SUCCESS ---
                        String downloadUrl = task.getResult().toString();
                        Log.d(TAG, "Got download URL: " + downloadUrl);
                        // Update the note object with the correct cloud URL
                        noteWithLocalPath.setImagePath(downloadUrl);
                    } else {
                        // --- FAILURE ---
                        Log.w(TAG, "Image upload or URL fetch failed for note " + noteWithLocalPath.getId(), task.getException());
                        // Fallback: save the note with an empty image path
                        noteWithLocalPath.setImagePath("");
                    }
                    // 4. NOW, save the final note object (with either the cloud URL or an empty path) to Firestore
                    db.collection("users").document(currentUser.getUid()).collection("notes").document(String.valueOf(noteWithLocalPath.getUserFirebaseId()))
                            .set(noteWithLocalPath)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Final note " + noteWithLocalPath.getId() + " saved to Firestore."))
                            .addOnFailureListener(e -> Log.w(TAG, "Error saving final note " + noteWithLocalPath.getId() + " to Firestore.", e));
                });
    }
    public void labelcode(List<String> labels,String content){
        if(labels == null) {
            Log.d(TAG, "LABELS is actually NULL!");
        } else if(labels.size() == 0) {
            labels.add("quick-note");labels.add("brief");
            Log.d(TAG, "LABELS is empty (size=0) with content: '" + content + "'");
        } else if(labels.size() == 1) {
            labels.add("note");
            Log.d(TAG, "LABELS has 1 item: " + labels.get(0) + " with content: '" + content + "'");
        } else {
            Log.d(TAG, "LABELS: " + labels.get(0) + " : " + labels.get(1) + " (total=" + labels.size() + ")");
        }
    }
    public int getFinalBackgroundColorForNote(){
        int colorToSave = Color.WHITE;
        Drawable background = mainContentLayout.getBackground();
        if (background instanceof ColorDrawable) {
            colorToSave = ((ColorDrawable) background).getColor();
        }
        return colorToSave;
    }
    public void saveNote() {
        clearHighlights();
        NotesWidgetProvider.refreshWidget(getApplicationContext());
        String content = saveNoteContent();
        WordTokenizer tokenizer = new WordTokenizer(content);
        List<String> labels = tokenizer.getTokenizedWords();
        labelcode(labels,content);

        String title = titleText.getText().toString().trim() + ";"+ labels.get(0) + ";" + labels.get(1);
        byte[] drawingData = (drawingView.getDrawingData() == null || drawingView.getDrawingData().length == 0) ? this.drawingData: drawingView.getDrawingData() ;

        if (title.isEmpty() && content.isEmpty() && (imagePath == null || imagePath.isEmpty())) {
            Toast.makeText(this, "Note is empty, not saved.", Toast.LENGTH_SHORT).show();
            isNoteModified = false;
            supportFinishAfterTransition();
            return;
        }
        String newimagePath = "";
        String filename = "drawing_" + System.currentTimeMillis() + ".png";
        if(drawingData != null && drawingData.length > 0){
            Bitmap drawingBitmap = BitmapFactory.decodeByteArray(drawingData, 0, drawingData.length);
            if(drawingBitmap != null){
                newimagePath = noteRepository.saveImageToInternalStorage(drawingBitmap, filename);
            }
        }

        final int finalColorToSave = getFinalBackgroundColorForNote();
        final String imagepathfinal = newimagePath;

        Executors.newSingleThreadExecutor().execute(() -> {
            Note notetoSave;
            if (currentUserUuid != null && currentUserUuid.length() != 0) {
                // Update existing note with the new imagePath
                notetoSave = noteRepository.getNoteByCloudId(currentUserUuid);
                notetoSave.setTitle(title);
                notetoSave.setContent(content);
                notetoSave.setDate(receivedDateFromActivities);
                notetoSave.setOrder(noteOrder);
                notetoSave.setColor(finalColorToSave);

                notetoSave.setPinned(false);
                notetoSave.setImagePath(imagepathfinal);
                //notetoSave = new Note(title, content, receivedDateFromActivities, finalColorToSave, noteOrder, isPinned, imagepathfinal);
                if(this.folderName.length() != 0){
                    notetoSave.setFontFamily(this.folderName);
                }
                notetoSave.setUserFirebaseId(currentUserUuid);
                noteRepository.updateNote(notetoSave);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.note_updated, Toast.LENGTH_SHORT).show();
                    isNoteModified = false;
                    supportFinishAfterTransition();
                });
            } else {
                // Create a new note with the new imagePath
                notetoSave = new Note(title, content, receivedDateFromActivities, finalColorToSave, 0, isPinned, imagepathfinal);
                if(this.folderName.length() != 0){
                    notetoSave.setFontFamily(this.folderName);
                }
                String noteCloudId = UUID.randomUUID().toString(); // generate uuid
                notetoSave.setUserFirebaseId(noteCloudId); // set the unique note id
                notetoSave.setFontFamily(this.folderName);
                notetoSave.setDate(receivedDateFromActivities);
                long newNoteId = noteRepository.addNote(notetoSave); // save to sqlite
                notetoSave.setId(newNoteId);
                Log.d(TAG, "note family:"+notetoSave.getFontFamily()+"|folder name |"+this.folderName+"| note setUserFirebaseId:"+notetoSave.getUserFirebaseId());

                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.note_saved_text, Toast.LENGTH_SHORT).show();
                    isNoteModified = false;
                    supportFinishAfterTransition();
                });
            }
            uploadAndSyncNoteToFirebase(notetoSave,drawingData,filename);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setUIChangesBasedOnImage();
                }
            });
        });

    }
    public void setUIChangesBasedOnImage(){
        if (drawingData != null && drawingData.length > 0) {
            Bitmap savedBitmap = BitmapFactory.decodeByteArray(drawingData, 0, drawingData.length);
            noteRepository.saveBytesToFile(drawingData,imagePath);
            if (savedBitmap != null) {
                imagesketch.setImageBitmap(savedBitmap);
                imagesketch.setVisibility(View.VISIBLE);
                imageCard.setVisibility(View.VISIBLE);

                imageframelayout.setVisibility(View.VISIBLE);
                clearImageButton.setVisibility(View.VISIBLE);
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
    private String getHtmlContent() {
        if (resultText.getText() != null) {
            // TO_HTML_PARAGRAPH_LINES_CONSECUTIVE is essential for preserving
            // paragraph-level spans like alignment.
            return HtmlCompat.toHtml(resultText.getText(), HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        }
        return "";
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
        linkImage = findViewById(R.id.linkImage);
        correctionProgressBar = findViewById(R.id.correction_progress_bar);
        leftAlignButton = findViewById(R.id.leftAlignButton);
        search_view = findViewById(R.id.search_view);
        rightAlignButton =findViewById(R.id.rightAlignButton);
        centerAlignButton = findViewById(R.id.centerAlignButton);
        linkCreationButton = findViewById(R.id.linkCreationButton);
        pdfUploadButton = findViewById(R.id.pdfUploadButton);
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
        linear_layout_main = findViewById(R.id.linear_layout_main);
        imagesketch = findViewById(R.id.imagesketch);
        black_pen = findViewById(R.id.black_pen);
        //linkPreviewHelper = new LinkPreviewHelper(this, resultText, linear_layout_main,linkImage);
        //linkPreviewHelper.setupLinkPreviewWatcher();
        resultText.setMovementMethod(LinkMovementMethod.getInstance());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            // Stop listening before destroying to ensure resources are released cleanly
            try {
                speechRecognizer.stopListening();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping SpeechRecognizer before destroy: " + e.getMessage());
                // Proceed with destroy even if stopListening fails
            }

            // Wrap the destroy call in try-catch to prevent the app crash
            try {
                speechRecognizer.destroy();
            } catch (Exception e) {
                // Log the exception but prevent the crash. The internal service unbinding
                // is the likely culprit for the IllegalArgumentException.
                Log.e(TAG, "Error destroying SpeechRecognizer: " + e.getMessage(), e);
            }
        }
    }
}
