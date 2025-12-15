package com.noteaiapp.keyboardai.geminichat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.noteaiapp.keyboardai.BuildConfig;
import com.noteaiapp.keyboardai.Models.ChatMessage;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.Notepad;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.adapter.ChatAdapter;
import com.noteaiapp.keyboardai.adapter.SuggestionAdapter;
import com.noteaiapp.keyboardai.camera.CameraActivity;
import com.noteaiapp.keyboardai.imagenote.ImageNoteActivity;
import com.noteaiapp.keyboardai.interfaces.FirebaseNoteFetchCallback;
import com.noteaiapp.keyboardai.interfaces.GeminiAPIKey;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import android.os.Handler;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.noteaiapp.keyboardai.interfaces.GeminiFormattingCallback;
import com.noteaiapp.keyboardai.interfaces.GeminiMindMapCallback;
import com.noteaiapp.keyboardai.mindmap.MindMapActivity;

import okhttp3.RequestBody;

public class GeminiChatActivity extends AppCompatActivity implements ChatAdapter.SelectionListener {

    private static final String TAG = "com.noteaiapp.keyboardai";
    // IMPORTANT: Make sure you have your API Key here, or load it securely
    private boolean isListening = false;
    private SpeechRecognizer speechRecognizer;
    private boolean isExistingNote = false;
    private boolean isChatModified = false;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            // This will be called when the countdown finishes
            // We'll add a check to ensure we only save if there are changes
            if (isExistingNote) { // Only auto-save notes that have been saved at least once
                Log.d(TAG, "Auto-saving note...");
                Toast.makeText(GeminiChatActivity.this, R.string.auto_saving, Toast.LENGTH_SHORT).show();
                saveNote();
            }
        }
    };
    // In GeminiChatActivity.java

    private void triggerAutoSave() {
        // 1. Remove any pending auto-save callbacks. This resets the timer.
        autoSaveHandler.removeCallbacks(autoSaveRunnable);

        // 2. Schedule the auto-save to run after the delay.
        // This will only execute if another change doesn't happen within AUTO_SAVE_DELAY_MS.
        autoSaveHandler.postDelayed(autoSaveRunnable, AUTO_SAVE_DELAY_MS);
    }

    private static final long AUTO_SAVE_DELAY_MS = 10000; // 3 seconds
    // 1. Add these member variables at the top of the class
    private static final int MAX_PDF_SIZE_MB = 5; // Set a 5MB limit
    private ActivityResultLauncher<Intent> pdfPickerLauncher;
    private String attachedPdfText = ""; // To hold the extracted text
    EditText noteTitleEditText;
    private List<ChatMessage> filteredChatMessages; // <-- ADD THIS
    private ActionMode actionMode; // To hold the contextual action bar

    private ImageView voiceicon;
    private Intent recognizerIntent;
    //private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="+API_KEY;

    private static final String API_URL = GeminiAPIKey.API_URL_GEMINI+GeminiAPIKey.API_KEY;

    private RecyclerView chatRecyclerView;

    private EditText inputEditText;
    private static final int PERMISSION_REQUEST_CODE = 1;

    private ImageButton sendButton;
    private ProgressBar progressBar,listeningProgress;

    private ChatAdapter chatAdapter;
    SearchView search_view;
    private RecyclerView suggestionRecyclerView;
    private SuggestionAdapter suggestionAdapter;
    private List<String> suggestionList = new ArrayList<>();
    private List<ChatMessage> chatMessages;
    public static final String EXTRA_FOLDER_NAME = "FOLDER_NAME";
    public MaterialToolbar toolbar;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String currentNoteUuid = "";
    private ImageButton cameraIcon;
    private static final String DATE_EXTRA_KEY = "date_specific_notes";
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private boolean dateReceived = false;
    private String receivedDateFromActivities = "";
    private String ocrCameraText = "";
    private String folderName = "";
    StorageReference storageRef;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Set connection timeout
            .readTimeout(60, TimeUnit.SECONDS)    // Set read timeout
            .writeTimeout(60, TimeUnit.SECONDS)   // Set write timeout
            .build();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_gemini_chat);
        progressBar = findViewById(R.id.chat_progress_bar);
        init();
        listener();
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, GeminiChatActivity.this, (ChatAdapter.SelectionListener) GeminiChatActivity.this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        this.currentNoteUuid = (getIntent().getStringExtra("note_font_size") == null)? "": getIntent().getStringExtra("note_font_size");

        if (currentNoteUuid.length() > 0) {
            isExistingNote = true;
            loadChatHistoryFromFirebase();
        } else {
            addMessage("Hello! How can I help you today?", false);
        }
        if(getIntent().getStringExtra(DATE_EXTRA_KEY) != null && !getIntent().getStringExtra(DATE_EXTRA_KEY).isEmpty()){
            dateReceived = true;
            this.receivedDateFromActivities = getIntent().getStringExtra(DATE_EXTRA_KEY);
        }
        else{
            receivedDateFromActivities = (getIntent().getStringExtra("note_date") == null)? getCurrentDate(): getIntent().getStringExtra("note_date");
        }
        this.folderName = (getIntent().getStringExtra(EXTRA_FOLDER_NAME) == null)? "":(getIntent().getStringExtra(EXTRA_FOLDER_NAME));

        // Initialize UI components

        inputEditText = findViewById(R.id.chat_input_edit_text);
        listeningProgress = findViewById(R.id.listeningProgress);
        sendButton = findViewById(R.id.send_button);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);


        // voice setup
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString());

        // In onCreate()

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override    public void onReadyForSpeech(Bundle params) {
                // Show a visual indicator that the app is listening
                if (listeningProgress != null) {
                    listeningProgress.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                // Hide the listening indicator when the user stops talking
                if (listeningProgress != null) {
                    listeningProgress.setVisibility(View.GONE);
                }
                // You can optionally automatically stop listening here if you prefer
                // isListening = false;
                // voiceicon.setBackground(...); // Reset icon background
            }

            @Override
            public void onError(int error) {
                // Handle errors, e.g., no speech input
                Log.e(TAG, "Speech Recognizer Error: " + error);
                if (listeningProgress != null) {
                    listeningProgress.setVisibility(View.GONE);
                }
                isListening = false;
                voiceicon.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.round_voice_bg));
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    // Get the best match and set it as the text in your input EditText
                    String spokenText = matches.get(0);
                    inputEditText.setText(spokenText);
                    // Move the cursor to the end of the text
                    inputEditText.setSelection(spokenText.length());
                }
                isListening = false; // Stop listening after getting a final result
                voiceicon.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.round_voice_bg));
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null && !partial.isEmpty()) {
                    // Show the user what's being heard in real-time
                    String partialText = partial.get(0);
                    inputEditText.setText(partialText);
                    inputEditText.setSelection(partialText.length());
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });

        voiceicon = findViewById(R.id.voiceicon);
        // In onCreate()

        voiceicon.setOnClickListener(new View.OnClickListener() {
            @Override    public void onClick(View v) {
                // --- START: THIS IS THE FIX ---

                // 1. Check if permission is already granted.
                if (ContextCompat.checkSelfPermission(GeminiChatActivity.this, android.Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {

                    // Permission is granted, so we can proceed directly.
                    Log.d(TAG, "Permission is already granted. Toggling listener.");
                    toggleListening();

                } else {
                    // Permission has not been granted yet, so request it.
                    Log.d(TAG, "Permission not granted. Requesting permission...");
                    ActivityCompat.requestPermissions(
                            GeminiChatActivity.this,
                            new String[]{android.Manifest.permission.RECORD_AUDIO},
                            PERMISSION_REQUEST_CODE
                    );
                }
                // --- END: THIS IS THE FIX ---
            }
        });


        // Set up the RecyclerView


        // Set up the send button listener
        sendButton.setOnClickListener(v -> sendMessage());
    }
    private void loadChatHistoryFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);

        if (currentNoteUuid == null || currentUser == null) {
            Toast.makeText(this, R.string.note_id_missing, Toast.LENGTH_SHORT).show();
            noteTitleEditText.setText("NotesAI Notebook...");
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Fetch the specific note document from Firestore
        db.collection("users").document(currentUser.getUid())
                .collection("notes").document(currentNoteUuid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        Note existingNote = documentSnapshot.toObject(Note.class);
                        if (existingNote != null && existingNote.getContent() != null) {
                            // Parse the note's HTML content back into ChatMessage objects
                            parseAndDisplayChatHistory(existingNote.getContent());
                            noteTitleEditText.setText(existingNote.getTitle().toString());
                            generateDynamicSuggestions(); // Generate suggestions based on the loaded chat
                        }
                    } else {
                        Toast.makeText(this, R.string.could_not_find_chat, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading chat note from Firebase", e);
                    Toast.makeText(this, R.string.failed_load, Toast.LENGTH_SHORT).show();
                });
    }
    private void parseAndDisplayChatHistory(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) return;

        chatMessages.clear();
        Log.d(TAG, "HTML Content: " + htmlContent);

        // This is a simplified parser. It splits the content by the "<h3>" tags.
        // A more robust solution might use an HTML parsing library like Jsoup.
        String[] parts = htmlContent.split("<h3>");
        for (String part : parts) {
            Log.d(TAG, "Part: " + part);
            if (part.trim().isEmpty()) continue;
            boolean isUser = part.startsWith("You:");
            String message;

            if (isUser) {
                if(part.contains("https://firebasestorage.googleapis.com")){
                    message = part.replace("You:</h3>", "").replaceAll("<[^>]*>", "").trim();
                    Log.d("com.noteaiapp.keyboardai","firbase image:" + message);
                    ChatMessage message1 = new ChatMessage();
                    message1.setType("image");
                    message1.setMessage(message);
                    message1.setUser(true);
                    chatMessages.add(message1);
                    continue;
                }
                else if(part.contains("pdf:")){
                    message = part.replace("You:</h3>", "").replaceAll("<[^>]*>", "").trim();
                    ChatMessage message1 = new ChatMessage();
                    message1.setType("pdf");
                    message1.setMessage(message);
                    message1.setUser(true);
                    chatMessages.add(message1);
                    continue;
                }
                else{
                    message = part.replace("You:</h3>", "").replaceAll("<[^>]*>", "").trim();
                }
                // For
                //user messages, we strip out all HTML tags to get the plain text.
            } else if (part.startsWith("NotesAI:")) {
                // For Gemini messages, we keep the inner HTML for styled rendering.
                message = part.replace("NotesAI:</h3>", "");
            } else {
                continue; // Skip parts that don't match, like the initial <h1>
            }
            chatMessages.add(new ChatMessage(message, isUser));
        }
        chatAdapter.notifyDataSetChanged();
        if (!chatMessages.isEmpty()) {
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        }
    }
    // Add this new method to GeminiChatActivity.java

    private void formatAiResponseWithGemini(String rawText, GeminiFormattingCallback callback) {// 1. Create a highly specific prompt for the formatting task.
        String formattingPrompt = "You are an expert HTML formatter. Analyze the following text and wrap it in the appropriate HTML tags for an Android app.\n" +
                "Follow these rules precisely:\n" +
                "- Use <h3> for main headings.\n" +
                "- Use <p> for paragraphs.\n" +
                "- Use <ul> and <li> for bullet points.\n" +
                "- Use <pre><code> for code blocks, preserving all indentation and line breaks.\n" +
                "- Use <b> for bold and <i> for italic.\n" +
                "- Your entire output must ONLY be the formatted HTML. Do not add any extra commentary.\n\n" +
                "Text to format:\n\n\"" + rawText + "\"";

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 2. Construct the JSON payload for this simple, single-turn request.
                JSONObject jsonBody = new JSONObject();
                JSONArray contentsArray = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text", formattingPrompt);
                parts.put(textPart);
                content.put("parts", parts);
                contentsArray.put(content);
                jsonBody.put("contents", contentsArray);

                RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));

                // Use the UNARY endpoint for this simple task. It's faster.
                Request request = new Request.Builder()
                        .url(GeminiAPIKey.API_URL_GEMINI + GeminiAPIKey.API_KEY)
                        .post(body)
                        .build();

                // 3. Execute the synchronous API call.
                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String formattedHtml = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim();

                    // 4. Return the result via the callback on the UI thread.
                    runOnUiThread(() -> callback.onFormattingComplete(formattedHtml));
                } else {
                    throw new IOException("Formatter API call failed with code: " + response.code());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error during Gemini formatting step: " + e.getMessage(), e);
                runOnUiThread(callback::onFormattingFailed);
            }
        });
    }
    private void sendMessage() {
        String prompt = inputEditText.getText().toString().trim();
        if (prompt.isEmpty()) {
            return;
        }
        isChatModified = true; // <-- ADD THIS
        // 1. Add user's message to the list and update UI
        addMessage(prompt, true);
        triggerAutoSave();
        inputEditText.setText(""); // Clear the input field
        getGeminiResponse(prompt);
    }

    private void addMessage(String message, boolean isUser) {
        // Add the message to the list
        chatMessages.add(new ChatMessage(message, isUser));
        // Notify the adapter that a new item has been inserted
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        // Scroll to the bottom to show the latest message
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
    private void addImageMessage(String message, boolean isUser, Bitmap imageUri) {
        ChatMessage chatMessage = new ChatMessage(message, isUser);
        chatMessage.setImage(imageUri);
        chatMessage.setIsImageFlag(true);
        chatMessage.setMessage(message);
        chatMessage.setType("image");
        chatMessage.setUser(isUser);
        // Notify the adapter that a new item has been inserted
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        // Scroll to the bottom to show the latest message
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
    private void addPdfMessage(String message, boolean isUser, Bitmap imageUri) {
        ChatMessage chatMessage = new ChatMessage("pdf:"+message, true);
        chatMessage.setImage(imageUri);
        chatMessage.setIsImageFlag(true);
        chatMessage.setMessage("pdf:"+message);
        chatMessage.setUser(true);
        chatMessage.setType("pdf");
        chatMessages.add(chatMessage);
        // Notify the adapter that a new item has been inserted
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        // Scroll to the bottom to show the latest message
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
    private void getGeminiResponse(String prompt1) {
        progressBar.setVisibility(View.VISIBLE);

        // Optimized prompt for better HTML formatting
        String prompt = "Respond to this prompt using clean HTML formatting (important 100 words maximum reply):\n\n" +
                "Rules:\n" +
                "- Use <br> please after main headings and list items (very important)\n" +
                "- Use <h3> for main headings\n" +
                "- Use <p> for paragraphs (never use plain text)\n" +
                "- Use <ul><li> for bullet points\n" +
                "- Use <ol><li> for numbered lists\n" +
                "- Use <b> for bold, <i> for italic\n" +
                "- Use <br> only between major sections\n" +
                "- Use <code> for inline code\n" +
                "- Use <pre><code> for code blocks\n" +
                "- Keep response around 200 words\n" +
                "- NO markdown symbols (*, **, ***)\n" +
                "- Start directly with content, no preamble\n\n" +
                "User: " + prompt1 +"Give me the internet links from where you are getting the data";

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JSONArray contentsArray = new JSONArray();
                int start = Math.max(chatMessages.size() - 10, 0);

                for (int i = start; i < chatMessages.size(); i++) {
                    ChatMessage msg = chatMessages.get(i);
                    JSONObject contentObj = new JSONObject();
                    JSONArray parts = new JSONArray();
                    JSONObject part = new JSONObject();
                    part.put("text", msg.getMessage());
                    parts.put(part);
                    contentObj.put("parts", parts);
                    contentObj.put("role", msg.isUser() ? "user" : "model");
                    contentsArray.put(contentObj);
                }

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("contents", contentsArray);

                RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .build();

                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);

                    String geminiResponse = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim();

                    // Clean up markdown code blocks
                    geminiResponse = geminiResponse
                            .replaceAll("```html\\s*", "")
                            .replaceAll("```\\s*", "")
                            .trim();

                    // Convert remaining markdown to HTML (fallback)
                    geminiResponse = geminiResponse
                            .replaceAll("### (.*?)\\n", "<h3>$1</h3>")
                            .replaceAll("## (.*?)\\n", "<h2>$1</h2>")
                            .replaceAll("# (.*?)\\n", "<h1>$1</h1>")
                            .replaceAll("\\*\\*\\*(.*?)\\*\\*\\*", "<b><i>$1</i></b>")
                            .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")
                            .replaceAll("(?<!\\*)\\*(?!\\*)(.*?)\\*(?!\\*)", "<i>$1</i>")
                            .replaceAll("\\n\\n", "<br><br>")
                            .replaceAll("^- (.*?)$", "<li>$1</li>")
                            .replaceAll("^\\d+\\. (.*?)$", "<li>$1</li>");

                    // Wrap orphaned <li> tags in <ul>
                    geminiResponse = geminiResponse.replaceAll("(<li>.*?</li>)+", "<ul>$0</ul>");

                    String finalGeminiResponse = geminiResponse;

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        addMessage(finalGeminiResponse, false);
                        isChatModified = true;
                        generateDynamicSuggestions();
                    });
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(GeminiChatActivity.this,
                                R.string.error_message, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini API: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    triggerAutoSave();
                    Toast.makeText(GeminiChatActivity.this,
                            R.string.error_occured, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // In GeminiChatActivity.java

    private void generateDynamicSuggestions() {
        // 1. Create a prompt specifically for generating suggestions.
        // We will send the last few messages as context.
        String historyForSuggestions = "";
        int start = Math.max(0, chatMessages.size() - 4); // Use last 4 messages for context
        for (int i = start; i < chatMessages.size(); i++) {
            ChatMessage msg = chatMessages.get(i);
            String role = msg.isUser() ? "User" : "Model";
            historyForSuggestions += role + ": " + msg.getMessage() + "\n";
        }

        String suggestionPrompt = "Based on the last part of this conversation, generate exactly 3 short, relevant follow-up questions or prompts. " +
                "RULES:\n" +
                "1. Your entire response must ONLY be the 3 prompts.\n" +
                "2. Each prompt must be on a new line.\n" +
                "3. Do NOT number the prompts or use bullet points.\n\n" +
                "CONVERSATION:\n" + historyForSuggestions;

        // 2. This must run on a background thread.
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 3. Construct the JSON payload for this separate, quick request.
                JSONObject jsonBody = new JSONObject();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text", suggestionPrompt);
                parts.put(textPart);
                content.put("parts", parts);
                jsonBody.put("contents", new JSONArray().put(content));

                RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));

                // Use the UNARY endpoint. It's faster for this simple task.
                String unaryApiUrl = GeminiAPIKey.API_URL_GEMINI + GeminiAPIKey.API_KEY;

                Request request = new Request.Builder()
                        .url(unaryApiUrl)
                        .post(body)
                        .build();

                // 4. Execute the API call synchronously.
                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String suggestionsText = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    // 5. Split the response into a list and update the UI.
                    String[] newSuggestions = suggestionsText.trim().split("\n");
                    runOnUiThread(() -> {
                        suggestionList.clear();
                        suggestionList.addAll(Arrays.asList(newSuggestions));
                        suggestionAdapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error generating dynamic suggestions: " + e.getMessage(), e);
                // If it fails, we can just clear the suggestions or do nothing.
                runOnUiThread(() -> {
                    suggestionList.clear();
                    suggestionAdapter.notifyDataSetChanged();
                });
            }
        });
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_attach){
            attachfile();
            return true;
        }else if (id == R.id.action_save) {
            saveNote();
            return true;
        }
        else if (id == R.id.action_share) {
            shareAINote();
            return true;
        } else if (id == R.id.mindmap) {
            StringBuilder totalContent = new StringBuilder();
            for (ChatMessage message : chatMessages) {
                Log.d(TAG, "saving Message: " + message.getMessage());
                if (message.getMessage() == null || message.getMessage().trim().isEmpty()) continue;
                totalContent.append(message.getMessage());
            }
            generateMindMapWithContent(totalContent.toString());
            return true;
        }
        return true;
    }
    private void generateMindMapWithContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            Toast.makeText(this, R.string.no_content_for_mindmap, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Generating smart mind map...", Toast.LENGTH_SHORT).show();

        // Call Gemini to generate structured mind map
        generateMindMapStructureWithGemini(content, new GeminiMindMapCallback() {
            @Override
            public void onStructureGenerated(String structuredText) {
                progressBar.setVisibility(View.GONE);

                Intent intent = new Intent(GeminiChatActivity.this, MindMapActivity.class);
                intent.putExtra("notecontent", structuredText);
                startActivity(intent);
            }

            @Override
            public void onGenerationFailed(Exception e) {
                progressBar.setVisibility(View.GONE);

                Log.e(TAG, "Gemini mind map generation failed.", e);
                Toast.makeText(GeminiChatActivity.this,
                        R.string.analysis_failed,
                        Toast.LENGTH_LONG).show();

                // Fallback: Launch with the original text
                Intent intent = new Intent(GeminiChatActivity.this, MindMapActivity.class);
                intent.putExtra("notecontent", content);
                startActivity(intent);
            }
        });
    }
    private void generateMindMapStructureWithGemini(String noteContent, GeminiMindMapCallback callback) {
        if (noteContent == null || noteContent.trim().isEmpty()) {
            callback.onGenerationFailed(new Exception("Note is empty."));
            return;
        }

        // Check content length and truncate if necessary (Gemini has token limits)
        String processedContent = noteContent;
        boolean isTruncated = false;

        // Rough estimate: 1 token ≈ 4 characters, limit to ~30k characters for safety
        int maxChars = 30000;
        if (processedContent.length() > maxChars) {
            processedContent = processedContent.substring(0, maxChars);
            isTruncated = true;
        }

        // Enhanced prompt for potentially complex/long content from PDFs
        String prompt = "You are a helpful assistant that specializes in creating structured mind maps from text content.\n" +
                "Analyze the following content and generate a comprehensive hierarchical mind map structure.\n\n" +
                "RULES:\n" +
                "1. Identify the main themes/topics. These will be your primary nodes.\n" +
                "2. Group related information under appropriate parent nodes.\n" +
                "3. Create a logical hierarchy with clear parent-child relationships.\n" +
                "4. If the content is from multiple sources (note + PDFs), organize them coherently.\n" +
                "5. Your output MUST be a simple indented list. Use two spaces for each level of indentation.\n" +
                "6. Do NOT use any bullet points, dashes, asterisks, numbers (like 1. or 1.1), or any other characters before the text.\n" +
                "7. Keep node text concise but meaningful (under 50 characters per node when possible).\n" +
                "8. Maximum depth of 4 levels to keep the mind map readable.\n\n" +
                "EXAMPLE OUTPUT FORMAT:\n" +
                "Main Topic\n" +
                "  Sub-Topic 1\n" +
                "    Detail A\n" +
                "    Detail B\n" +
                "  Sub-Topic 2\n" +
                "    Detail C\n\n" +
                (isTruncated ? "NOTE: The content was truncated due to length. Focus on the main themes and key points.\n\n" : "") +
                "Content to analyze:\n\n\"" + processedContent + "\"";

        // Use a background thread for the network call
        Executors.newSingleThreadExecutor().execute(() -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            try {
                // Create the JSON payload
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
                        .url(API_URL + GeminiAPIKey.API_KEY)
                        .post(body)
                        .build();

                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String structuredText = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim();

                    // Use the callback to return the result to the UI thread
                    runOnUiThread(() -> callback.onStructureGenerated(structuredText));
                } else {
                    throw new IOException("API call failed with code: " + response.code());
                }
            } catch (Exception e) {
                runOnUiThread(() -> callback.onGenerationFailed(e));
            }
        });
    }
    public void shareAINote(){
        if (chatMessages == null || chatMessages.isEmpty()) {
            Toast.makeText(this, "There is nothing in the chat to share.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();

        // Perform file I/O and PDF generation on a background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 2. Generate a single HTML string from the chat history.
                StringBuilder htmlBuilder = new StringBuilder();
                htmlBuilder.append("<html><head><style>body{font-family:sans-serif;} h3{margin-bottom:0;} p{margin-top:0;} pre{background-color:#f0f0f0; padding:10px; border-radius:5px;}</style></head><body>");
                htmlBuilder.append("<h1>Notes AI Chat Transcript</h1>");

                for (ChatMessage message : chatMessages) {
                    if (message.getMessage() == null || message.getMessage().trim().isEmpty()) continue;

                   if (message.isUser()) {
                        htmlBuilder.append("<h3>You:</h3>");
                        // Sanitize user input to prevent it from being interpreted as HTML
                        String sanitizedMessage = message.getMessage().replace("<", "&lt;").replace(">", "&gt;");
                        htmlBuilder.append("<p>").append(sanitizedMessage).append("</p>");
                    } else {
                        htmlBuilder.append("<h3>NotesAI:</h3>");
                        // AI message already contains HTML, so just append it
                        htmlBuilder.append(message.getMessage());
                    }
                    htmlBuilder.append("<br>");
                }
                htmlBuilder.append("</body></html>");
                String finalHtml = htmlBuilder.toString();

                // 3. Create a temporary PDF file in the app's cache directory.
                //    This matches the <cache-path> in your file_paths.xml
                File pdfFile = new File(getCacheDir(), "chat_transcript_" + System.currentTimeMillis() + ".pdf");

                // 4. Use iText to convert the HTML string to a PDF file.
                PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
                PdfDocument pdf = new PdfDocument(writer);
                HtmlConverter.convertToPdf(finalHtml, pdf, null);
                pdf.close();

                // 5. Get a content URI for the file using the FileProvider.
                //    The authority must match what's in your AndroidManifest.xml
                Uri fileUri = FileProvider.getUriForFile(
                        GeminiChatActivity.this,
                        BuildConfig.APPLICATION_ID + ".fileprovider", // This generates "com.noteaiapp.keyboardai.provider"
                        pdfFile
                );

                // 6. Create the Share Intent.
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/pdf");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "AI Chat Transcript");
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Grant permission for other apps to read the file

                // Switch back to the UI thread to launch the share sheet
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    startActivity(Intent.createChooser(shareIntent, "Share PDF Via"));
                });

            } catch (Exception e) {
                Log.e(TAG, "Error generating or sharing PDF", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(GeminiChatActivity.this, "Failed to generate PDF.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // In GeminiChatActivity.java
    private void getNoteFromFirebase(String noteCloudId, FirebaseNoteFetchCallback callback) {
        if (currentUser == null) {
            callback.onFetchFailed(new Exception("User not logged in."));
            return;
        }
        if (noteCloudId == null || noteCloudId.isEmpty()) {
            callback.onNoteFetched(null);
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .collection("notes").document(noteCloudId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Convert the Firestore document into a Note object
                        Note note = documentSnapshot.toObject(Note.class);
                        // Return the note via the callback
                        callback.onNoteFetched(note);
                    } else {
                        // The note doesn't exist in Firebase, which is an error state
                        callback.onNoteFetched(null); // Pass null to indicate not found
                    }
                })
                .addOnFailureListener(e -> {
                    // An error occurred (e.g., no internet)
                    Log.w(TAG, "Error fetching single note from Firebase", e);
                    callback.onFetchFailed(e);
                });
    }
    public void saveNote() {
        // <= 1 to ignore an initial AI greeting if nothing else was said
        if (chatMessages == null || chatMessages.size() <= 1) {
            if (isExistingNote && currentNoteUuid != null && currentUser != null) {
                // If it's an existing note, delete it from Firebase.
                Log.d(TAG, "Chat is empty. Deleting note with ID: " + currentNoteUuid);
                progressBar.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Empty chat, deleting note...", Toast.LENGTH_SHORT).show();

                db.collection("users").document(currentUser.getUid())
                        .collection("notes").document(currentNoteUuid)
                        .delete()
                        .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(GeminiChatActivity.this, "Note deleted.", Toast.LENGTH_SHORT).show();

                            // Inform NotesListActivity to refresh its list from Firebase
                            Intent intent = new Intent("com.noteaiapp.ACTION_NOTE_UPDATED");
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                            supportFinishAfterTransition(); // Close the activity
                        }))
                        .addOnFailureListener(e -> runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Error deleting note from Firebase.", e);
                            Toast.makeText(GeminiChatActivity.this, R.string.error_deleting_chat, Toast.LENGTH_SHORT).show();
                            supportFinishAfterTransition(); // Still close the activity
                        }));
            } else {

                supportFinishAfterTransition();
            }

            return;
        }
        final String title = noteTitleEditText.getText().toString().trim();
        Log.d(TAG, "Saving ai chat with title: ");
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Saving chat...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            // --- Convert chat history to a single HTML string ---
            StringBuilder chatHtmlBuilder = new StringBuilder();
            for (ChatMessage message : chatMessages) {
                Log.d(TAG, "saving Message: " + message.getMessage());
                if (message.getMessage() == null || message.getMessage().trim().isEmpty()) continue;

                if (message.isUser()) {
                    chatHtmlBuilder.append("<h3>You:</h3>");
                    // Wrap user's plain text in a paragraph tag for consistent formatting
                    chatHtmlBuilder.append("<p>").append(message.getMessage()).append("</p>");
                } else {
                    chatHtmlBuilder.append("<h3>NotesAI:</h3>");
                    // The AI message already contains rich HTML, so append it directly
                    chatHtmlBuilder.append(message.getMessage());
                }
            }
            String finalNoteContent = chatHtmlBuilder.toString();
            Log.d(TAG, "Final HTML: " + finalNoteContent);
            // ---
            getNoteFromFirebase(currentNoteUuid, new FirebaseNoteFetchCallback() {
                @Override
                public void onNoteFetched(Note note){
                    if (isExistingNote && currentNoteUuid != null && currentNoteUuid.length() > 0) {
                        note.setUserFirebaseId(currentNoteUuid);
                        if(folderName.length() != 0){
                            note.setFontFamily(folderName);
                        }
                    } else {
                        note = new Note();
                        String newNoteId = UUID.randomUUID().toString();
                        note.setUserFirebaseId(newNoteId);
                        // Update activity state so subsequent saves are updates
                        currentNoteUuid = newNoteId;
                        note.setFontFamily(folderName);
                        isExistingNote = true;
                    }
                    if(title != null && title.length() > 0){
                        note.setTitle(title);
                    }
                    else{
                        note.setTitle("NotesAI Notebook");
                    }
                    note.setFontColor("ainote");

                    // --- Set/Update note properties ---
                    note.setContent(finalNoteContent);
                    note.setDate(receivedDateFromActivities); // Update the last modified date
                    note.setImagePath("");
                    // --- Save DIRECTLY to Firebase ---
                    if (currentUser != null) {

                        db.collection("users").document(currentUser.getUid())
                                .collection("notes").document(note.getUserFirebaseId())
                                .set(note)
                                .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(GeminiChatActivity.this, R.string.chat_saved, Toast.LENGTH_LONG).show();
                                    // Inform NotesListActivity to refresh its list from Firebase
                                    isChatModified = false;
                                    Intent intent = new Intent("com.noteaiapp.ACTION_NOTE_UPDATED");
                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                                    //supportFinishAfterTransition();
                                }))
                                .addOnFailureListener(e -> runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Log.e(TAG, "Error saving chat note to Firebase.", e);
                                    Toast.makeText(GeminiChatActivity.this, "Error saving chat. Please try again.", Toast.LENGTH_SHORT).show();
                                    //supportFinishAfterTransition();
                                }));
                    }
                }
                @Override
                public void onFetchFailed(Exception e) {
                }
            });
        });
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void attachfile(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf"); // Only show PDF files
        try {
            pdfPickerLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.file_manager_not_found, Toast.LENGTH_SHORT).show();
        }
    }



    // You can override onPostResume if you need to, but it's not required for this functionality.
    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    public void onBackPressed() {

        if (isChatModified) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.unsaved_changes_text)
                    .setMessage(R.string.do_you_want_to_save)
                    .setPositiveButton(R.string.save_menu, (dialog, which) -> {
                        // User clicked "Save"
                        saveNote();
                        // Note: We let the saveNote method's success listener handle finishing the activity.
                    })
                    .setNegativeButton(R.string.discard_text, (dialog, which) -> {
                        // User clicked "Discard"
                        // Close the activity without saving.
                        super.onBackPressed();
                    })
                    .setNeutralButton(R.string.cancel_list, (dialog, which) -> {
                        // User clicked "Cancel"
                        // Just dismiss the dialog and do nothing.
                        dialog.dismiss();
                    })
                    .show();
        } else {
            // 3. If there are no unsaved changes, just perform the default back action.
                super.onBackPressed();
        }

        }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // --- Case 1: User GRANTED the permission. ---
                Log.d(TAG, "Permission granted by user in dialog.");
                // Inform the user and let them tap again, or start listening immediately.
                Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_LONG).show();
                // Or, to start immediately: toggleListening();

            } else {
                // --- Case 2: User DENIED the permission. ---
                Log.d(TAG, "Permission denied by user in dialog.");

                // Now, check if they checked "Don't ask again".
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
                    // This is the true "permanently denied" case. Show the settings dialog.
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.permission_denied)
                            .setMessage(R.string.permission_text)
                            .setPositiveButton(R.string.settings_text, (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton(R.string.cancel_text, (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    // User just denied it for this session. Show a simple toast.
                    Toast.makeText(this, R.string.microphone_access, Toast.LENGTH_SHORT).show();
                }
            }
        }
        else  if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //openCamera();
            } else {
                Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }
    // Add this helper method to your GeminiChatActivity.java

    private void toggleListening() {MediaPlayer mp = MediaPlayer.create(GeminiChatActivity.this, R.raw.googleassistant);
        if (mp != null) {
            mp.start();
            mp.setOnCompletionListener(mediaPlayer -> mediaPlayer.release());
        }

        if (!isListening) {
            isListening = true;
            voiceicon.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.rounded_purple_background));
            speechRecognizer.startListening(recognizerIntent);
            Toast.makeText(getApplicationContext(), R.string.listening_text, Toast.LENGTH_SHORT).show();
        } else {
            isListening = false;
            voiceicon.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.round_voice_bg));
            speechRecognizer.stopListening();
            if (listeningProgress != null) {
                listeningProgress.setVisibility(View.GONE);
            }
        }
    }

    private void loadInitialSuggestions() {
        suggestionList.clear();
        suggestionList.add("Summarize this document");
        suggestionList.add("Generate ideas for a project.");
        suggestionList.add("Which is the largest word in English?");
        suggestionList.add("who is the president of India?");
        suggestionList.add("best places of tacos");
        suggestionAdapter.notifyDataSetChanged();
    }

    // You can create another method to dynamically change suggestions based on context
    private void loadFollowUpSuggestions() {
        suggestionList.clear();
        suggestionList.add("Elaborate on the first point");    suggestionList.add("Give me an example");
        suggestionList.add("What are the counter-arguments?");
        suggestionAdapter.notifyDataSetChanged();
    }
// In GeminiChatActivity.java

    private void filterChatMessages(String query) {
        // Clear the previous search results
        filteredChatMessages.clear();

        if (query.isEmpty()) {
            // If the query is empty, show the full chat history
            chatAdapter.setMessages(chatMessages);
        } else {
            // Loop through the original, complete list of messages
            for (ChatMessage message : chatMessages) {
                // Check if the message content contains the query (case-insensitive)
                if (message.getMessage() != null && message.getMessage().toLowerCase().contains(query.toLowerCase())) {
                    // If it matches, add it to the filtered list
                    filteredChatMessages.add(message);
                }
            }
            // Update the adapter to show only the filtered results
            chatAdapter.setMessages(filteredChatMessages);
        }
        chatAdapter.notifyDataSetChanged();
    }

    public void init(){
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
        search_view = findViewById(R.id.search_view);
        filteredChatMessages = new ArrayList<>();
        search_view.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // This is called when the user presses the search button on the keyboard
                filterChatMessages(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // This is called every time the user types a character
                // We can filter in real-time
                filterChatMessages(newText);
                return true;
            }
        });
        search_view.setOnCloseListener(() -> {
            // Restore the adapter with the original, complete list of messages
            chatAdapter.setMessages(chatMessages);
            return false;
        });
        cameraIcon = findViewById(R.id.cameraIcon);
        suggestionRecyclerView = findViewById(R.id.suggestion_recycler_view);
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        noteTitleEditText = findViewById(R.id.noteTitleEditText);
        // 3. Set up the suggestion adapter and click listener
        suggestionAdapter = new SuggestionAdapter(suggestionList, suggestion -> {
            inputEditText.setText(suggestion);sendMessage();
        });

        // 4. Set the LayoutManager for the horizontal list
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        suggestionRecyclerView.setLayoutManager(layoutManager);
        suggestionRecyclerView.setAdapter(suggestionAdapter);    // 5. Populate the initial list of suggestions
        loadInitialSuggestions();
            pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri pdfUri = result.getData().getData();
                        if (pdfUri != null) {
                            // Process the selected PDF
                            processSelectedPdf(pdfUri);
                        }
                    }
                }
        );
    }
    // In GeminiChatActivity.java

    private void processSelectedPdf(Uri pdfUri) {
        // Show a progress bar while the PDF is being processed
        progressBar.setVisibility(View.VISIBLE);

        Executors.newSingleThreadExecutor().execute(() -> {
            // Perform file operations on a background thread    Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. Check file size to avoid memory errors and high API costs
                Cursor cursor = getContentResolver().query(pdfUri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    long size = cursor.getLong(sizeIndex);
                    cursor.close();
                    if (size > MAX_PDF_SIZE_MB * 1024 * 1024) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, R.string.pdf_large+" (Max " + MAX_PDF_SIZE_MB + "MB).", Toast.LENGTH_LONG).show();
                        });
                        return; // Stop processing
                    }
                }

                // 2. Extract text using PDFBox
                InputStream inputStream = getContentResolver().openInputStream(pdfUri);
                PdfReader reader = new PdfReader(inputStream);
                PdfDocument pdfDocument = new PdfDocument(reader);

                StringBuilder extractedTextBuilder = new StringBuilder();
                int numPages = pdfDocument.getNumberOfPages();
                final int MAX_PDF_PAGES = 4; // Your new page limit
                // 2. Check if the page count exceeds the limit.
                if (numPages > MAX_PDF_PAGES) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "PDF has too many pages (Max " + MAX_PDF_PAGES + ").", Toast.LENGTH_LONG).show();
                    });
                    // Clean up and stop processing
                    pdfDocument.close();
                    inputStream.close();
                    return; // Exit the background thread
                }

                for (int i = 1; i <= numPages; i++) {
                    PdfPage page = pdfDocument.getPage(i);
                    // Use PdfTextExtractor to get the text content from the page
                    String pageText = PdfTextExtractor.getTextFromPage(page);
                    extractedTextBuilder.append(pageText);
                    extractedTextBuilder.append("\n"); // Add a newline between pages
                }
                // Close the document and the input stream
                pdfDocument.close();
                inputStream.close();            // --- END OF FIX ---

                this.attachedPdfText = extractedTextBuilder.toString();

                runOnUiThread(() -> {
                    addPdfMessage(this.attachedPdfText, true, null);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.pdf_attached, Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error processing PDF with iText", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.failed_note_text, Toast.LENGTH_SHORT).show();
                });

            }
        });
    }

// In GeminiChatActivity.java

//1. Make sure you have Firebase Storage initialized in onCreate()
// private FirebaseStorage storage;
// private StorageReference storageRef;
// storage = FirebaseStorage.getInstance();
// storageRef = storage.getReference();

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // --- START: THIS IS THE FIX ---

                // 1. Check if the result is valid
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                    // 2. Directly get the OCR text that CameraActivity already detected for you.
                    String detectedText = result.getData().getStringExtra("ocr_text");

                    // 3. Check if the OCR text is valid.
                    if (detectedText != null && !detectedText.trim().isEmpty()) {
                        // 4. OCR was successful! Call your existing method to add the text to the chat.
                        Log.d(TAG, "Received OCR text from CameraActivity: " + detectedText);
                        //addImgTextMessage(detectedText);
                        String imagePath = result.getData().getStringExtra("image_path");
                        if (imagePath != null && !imagePath.isEmpty()) {
                            addCapturedImageToChat(imagePath,detectedText);
                        }
                        Log.d(TAG, "Image path from gemini CameraActivity: " + imagePath);
                    } else {
                        // This toast now correctly means that CameraActivity didn't find any text.
                        Toast.makeText(this, R.string.no_text_found_image, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // This is the toast you were seeing. It means the result was not RESULT_OK.
                    Log.w(TAG, "CameraActivity returned a non-OK result or null data.");
                }
                // --- END: THIS IS THE FIX ---
            });
// Add this new helper method to GeminiChatActivity.java

    private void addCapturedImageToChat(String imagePath,String content) {
        if (imagePath == null) return;
        try {
            ChatMessage imageMessage = new ChatMessage();
            imageMessage.setType("image"); // Set the type to "image"
            imageMessage.setUser(true);    // The user sent this image
            imageMessage.setMessage(imagePath + ";"+content);

            ChatMessage imageMessage2 = new ChatMessage();
            imageMessage2.setType("text"); // Set the type to "image"
            imageMessage2.setUser(true);    // The user sent this image
            imageMessage2.setMessage("What would you like to do with the image?");

            chatMessages.add(imageMessage);
            chatMessages.add(imageMessage2);
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

            // You can optionally trigger an auto-save here as well
            // triggerAutoSave();

        } catch (Exception e) {
            Log.e(TAG, "Failed to load bitmap from URI for chat display", e);
            Toast.makeText(this, R.string.failed_display_capture, Toast.LENGTH_SHORT).show();
        }
    }

    // You'll need a modified addImgMessage method
    private void addImgMessage(ChatMessage message) {
        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
        triggerAutoSave();
    }


    public void listener(){
        cameraIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(GeminiChatActivity.this, CameraActivity.class);
                cameraLauncher.launch(i);
            }
        });
    }
    @Override
    public void onSelectionModeChanged(boolean isEnabled) {
        if (isEnabled && actionMode == null) {
            // Start the action mode
            actionMode = startSupportActionMode(actionModeCallback);
        } else if (!isEnabled && actionMode != null) {
            // Finish the action mode
            actionMode.finish();
        }
    }

    @Override
    public void onSelectionCountChanged(int count) {
        if (actionMode != null) {
            // Update the title of the action bar
            actionMode.setTitle(count + " selected");
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate the menu for the contextual action bar
            getMenuInflater().inflate(R.menu.chat_selection_menu, menu);
            return true; // Return true to show the action mode
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false because nothing is prepared
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete) {
                chatAdapter.deleteSelectedMessages();
                Toast.makeText(GeminiChatActivity.this, R.string.message_deleted, Toast.LENGTH_SHORT).show();
                isChatModified = true;
                // Finish the action mode. This will automatically call onDestroyActionMode.
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // This is called when the action mode is finished (e.g., back button or action completed)
            chatAdapter.exitSelectionMode();
            actionMode = null;
        }
    };

}
