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
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.noteaiapp.keyboardai.Models.ChatMessage;
import com.noteaiapp.keyboardai.Notepad;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.adapter.ChatAdapter;
import com.noteaiapp.keyboardai.interfaces.GeminiAPIKey;


import java.io.InputStream;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import com.itextpdf.kernel.pdf.PdfDocument;
import okhttp3.RequestBody;

public class GeminiChatActivity extends AppCompatActivity {

    private static final String TAG = "GeminiChatActivity";
    // IMPORTANT: Make sure you have your API Key here, or load it securely
    private static final String API_KEY = GeminiAPIKey.API_KEY;
    private boolean isListening = false;
    private SpeechRecognizer speechRecognizer;
    // 1. Add these member variables at the top of the class
    private static final int MAX_PDF_SIZE_MB = 5; // Set a 5MB limit
    private ActivityResultLauncher<Intent> pdfPickerLauncher;
    private String attachedPdfText = ""; // To hold the extracted text

    private ImageView voiceicon;
    private Intent recognizerIntent;
    //private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="+API_KEY;

    private static final String API_URL = GeminiAPIKey.API_URL_GEMINI+API_KEY;

    private RecyclerView chatRecyclerView;
    private EditText inputEditText;
    private static final int PERMISSION_REQUEST_CODE = 1;

    private ImageButton sendButton;
    private ProgressBar progressBar,listeningProgress;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    public MaterialToolbar toolbar;
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
        init();
        // Initialize UI components
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        inputEditText = findViewById(R.id.chat_input_edit_text);
        listeningProgress = findViewById(R.id.listeningProgress);
        sendButton = findViewById(R.id.send_button);
        progressBar = findViewById(R.id.chat_progress_bar);
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
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        // Set up the send button listener
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String prompt = inputEditText.getText().toString().trim();
        if (prompt.isEmpty()) {
            return;
        }

        // 1. Add user's message to the list and update UI
        addMessage(prompt, true);
        inputEditText.setText(""); // Clear the input field

        // 2. Get the response from Gemini
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
        ChatMessage chatMessage = new ChatMessage(message, isUser);
        chatMessage.setImage(imageUri);
        chatMessage.setIsImageFlag(true);
        chatMessage.setMessage(message);
        chatMessage.setUser(isUser);
        chatMessage.setType("pdf");
        chatMessages.add(chatMessage);
        // Notify the adapter that a new item has been inserted
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        // Scroll to the bottom to show the latest message
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
    private void getGeminiResponse(String prompt1) {
        progressBar.setVisibility(View.VISIBLE);
        String prompt = "Please respond to the following user prompt. " +
                "Format your response for display in an Android app using simple HTML tags. " +
                "Follow these rules precisely:\n" +
                "1. Use <b>Your Heading</b> for any headings.\n" +
                "2. For numbered lists, use <ol><li>Item 1</li><li>Item 2</li></ol>.\n" +
                "3. For bullet points, use <ul><li>Item 1</li><li>Item 2</li></ul>.\n" +
                "4. For code snippets, wrap the code inside <pre><code>...code...</code></pre> tags.\n" +
                "5. Use <br> for line breaks between paragraphs.\n" +
                "6. Keep the response concise, around 200 words.\n\n" +
                "User prompt: \"" + prompt1 + "\"";
        Executors.newSingleThreadExecutor().execute(() -> {
            //OkHttpClient client = new OkHttpClient();
            try {
                // Construct the JSON payload for the Gemini API

                JSONArray contentsArray = new JSONArray();
                int start = Math.max(chatMessages.size() - 10, 0); // last 10 messages


                JSONObject systemMsg = new JSONObject();
                JSONObject systemAuthor = new JSONObject();
                systemAuthor.put("role", "system");
                systemMsg.put("author", systemAuthor);

                JSONArray systemContent = new JSONArray();
                JSONObject systemPart = new JSONObject();
                systemPart.put("type", "text");
                systemPart.put("text",
                        "You are a helpful assistant. Format all responses strictly in HTML for Android display:" +
                                "<br>1. Use <b>Heading</b> for titles." +
                                "<br>2. Use <ol><li>Item</li></ol> for numbered lists." +
                                "<br>3. Use <ul><li>Item</li></ul> for bullet points." +
                                "<br>4. Use <pre><code>code</code></pre> for code blocks." +
                                "<br>5. Use <br> for line breaks." +
                                "<br>Do NOT use Markdown like *** or ** or ##.");
                systemContent.put(systemPart);
                systemMsg.put("content", systemContent);
                contentsArray.put(systemMsg);


                for (int i = start; i < chatMessages.size(); i++) {
                    ChatMessage msg = chatMessages.get(i);
                    JSONObject contentObj = new JSONObject();
                    JSONArray parts = new JSONArray();
                    JSONObject part = new JSONObject();
                    part.put("text", msg.getMessage());
                    parts.put(part);
                    contentObj.put("parts", parts);
                    contentObj.put("role", msg.isUser() ? "user" : "assistant");
                    contentsArray.put(contentObj);
                }


                JSONObject jsonBody = new JSONObject();
                JSONObject contents = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt);
                parts.put(textPart);
                contents.put("parts", parts);
                jsonBody.put("contents", contentsArray);

                RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .build();

                // Synchronous API call
                okhttp3.Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    // Extract the text from the response
                    String geminiResponse = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim().replace("```","").replace("html","");
                    geminiResponse = geminiResponse
                            .replaceAll("\\*\\*\\*(.*?)\\*\\*\\*", "<b>$1</b>")   // ***heading*** -> <b>heading</b>
                            .replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>")         // **bold** -> <b>bold</b>
                            .replaceAll("\\*(.*?)\\*", "<i>$1</i>");
                    String finalGeminiResponse = geminiResponse;
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        // Add Gemini's response to the chat
                        addMessage(finalGeminiResponse, false);
                    });
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(GeminiChatActivity.this, "Error: Failed to get response.", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini API: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(GeminiChatActivity.this, "An error occurred.", Toast.LENGTH_SHORT).show();
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
        if (id == R.id.action_translate) {
            showLanguageSelectionDialog();
            return true;
        } else if (id == R.id.mindmap) {
            return true;
        }
        return true;
    }
    public void attachfile(){
        Toast.makeText(getApplicationContext(),"pdf clicked",Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf"); // Only show PDF files

        // Launch the file picker
        try {
            pdfPickerLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No file manager found to pick a PDF.", Toast.LENGTH_SHORT).show();
        }
    }
    public void saveNote(){

    }
    public void showLanguageSelectionDialog(){

    }
    // You can override onPostResume if you need to, but it's not required for this functionality.
    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // --- Case 1: User GRANTED the permission. ---
                Log.d(TAG, "Permission granted by user in dialog.");
                // Inform the user and let them tap again, or start listening immediately.
                Toast.makeText(this, "Permission granted. Tap the icon again to listen.", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, "Microphone access is required for voice input.", Toast.LENGTH_SHORT).show();
                }
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
    public void init(){
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
                            Toast.makeText(this, "PDF is too large (Max " + MAX_PDF_SIZE_MB + "MB).", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, "PDF attached successfully! Type your prompt.", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error processing PDF with iText", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to read PDF.", Toast.LENGTH_SHORT).show();
                });

            }
        });
    }


}
