package com.noteaiapp.keyboardai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.data.NoteRepository;
import com.noteaiapp.keyboardai.ui.DrawingView;
import com.noteaiapp.keyboardai.widget.NotesWidgetProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;


public class DrawingActivity extends AppCompatActivity {
    private RelativeLayout saveDiscardDialog;
    public static final String EXTRA_FOLDER_NAME = "FOLDER_NAME";

    private static final int PICK_IMAGE_REQUEST = 1;
    private DrawingView drawingView;
    private FirebaseUser currentUser;
    private String TAG = "com.noteaiapp.keyboardai";

    // --- START: ADD THESE LINES ---
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private ImageButton blackBtn, redBtn, blueBtn, smallPen, eraser, largePen, sprayPaintBtn, rectangleBtn,
            color_blue, color_green, color_yellow, color_orange, color_purple, color_teal, color_pink, color_maroon, color_color1,
            uploadImageBtn,color_grey,new_text_btn;
    private TextView dialog_discard_btn, dialog_cancel_btn, dialog_save_btn;
    ImageButton button_save;
    private SeekBar strokeWidthSeekBar;
    private NoteRepository noteRepository;
    private long currentNoteId = -1;
    private boolean isDirty = false, isSpray = false, isRectangle = false,isErasing = false,isTextMode = false; // Flag to track unsaved changes
    private static final String DATE_EXTRA_KEY = "date_specific_notes";
    private boolean dateReceived = false;
    private String receivedDateFromActivities = "";
    private String folderName = "";
    private String currentNoteUuid = "";

    // This static class will temporarily hold the drawing data to bypass the Intent size limit
    public static class DrawingDataManager {
        private static byte[] drawingData;

        public static void setDrawingData(byte[] data) {
            drawingData = data;
        }

        public static byte[] getDrawingData() {
            return drawingData;
        }

        public static void clearDrawingData() {
            drawingData = null;
        }
    }
    public void loadNoteData(){
        Intent intent = getIntent();
        if (intent.hasExtra("note_id")) {
            currentNoteId = intent.getLongExtra("note_id", -1);
            if (currentNoteId != -1) {
                Note existingNote = noteRepository.getNoteById(currentNoteId);
                if (existingNote != null && existingNote.getImagePath() != null) {
                    final Bitmap loadedBitmap = noteRepository.loadImageFromInternalStorage(existingNote.getImagePath());
                    if (loadedBitmap != null) {
                        drawingView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                if (drawingView.getWidth() > 0 && drawingView.getHeight() > 0) {
                                    drawingView.setBackgroundImage(loadedBitmap);
                                    drawingView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            }
                        });
                    }
                }
            }
        }
    }
    public void loadDataFromFirebase(String cloudId){
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
                            populateUiWithNoteData(cloudNote);
                        }
                    } else {
                        Log.w(TAG, "List note not found in Firestore, falling back to local.");
                        loadNoteData();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch from Firestore. Falling back to local.", e);
                    loadNoteData();
                });
    }
    public void populateUiWithNoteData(Note existingNote) {
        if (existingNote == null) {
            // Handle new note case if needed, though onCreate handles this now.
            return;
        }

        // Set the member variables from the loaded note
        this.currentNoteId = existingNote.getId();
        this.receivedDateFromActivities = existingNote.getDate();
        this.folderName = existingNote.getFontFamily();

        // --- THIS IS THE FIX ---
        String imagePath = existingNote.getImagePath();

        if (imagePath != null && !imagePath.isEmpty()) {
            Log.d(TAG, "Loading image into DrawingView from path: " + imagePath);

            // Use Glide to load the image. It handles both local paths and cloud URLs.
            Glide.with(this)
                    .asBitmap() // Important: We need a Bitmap for the drawing view
                    .load(imagePath)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            // This callback runs when Glide has finished downloading/loading the Bitmap.
                            // Now, set it as the background for the DrawingView.
                            drawingView.setBackgroundImage(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Handle case where the view is cleared
                        }
                    });
        }
        // -----------------------
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drawing);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        this.currentNoteUuid = (getIntent().getStringExtra("note_font_size") == null)? "": getIntent().getStringExtra("note_font_size");

        init();
        listeners();
        folderName = (getIntent().getStringExtra(EXTRA_FOLDER_NAME) != null && !getIntent().getStringExtra(EXTRA_FOLDER_NAME).isEmpty())? getIntent().getStringExtra(EXTRA_FOLDER_NAME):"";
        if(getIntent().getStringExtra(DATE_EXTRA_KEY) != null && !getIntent().getStringExtra(DATE_EXTRA_KEY).isEmpty()){
            dateReceived = true;
            this.receivedDateFromActivities = getIntent().getStringExtra(DATE_EXTRA_KEY);
        }
        else{
            receivedDateFromActivities = getCurrentDate();
        }
        loadDataFromFirebase(currentNoteUuid);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void init() {
        blackBtn = findViewById(R.id.color_black);
        redBtn = findViewById(R.id.color_red);
        blueBtn = findViewById(R.id.color_blue);
        smallPen = findViewById(R.id.pen_small);
        eraser = findViewById(R.id.eraser);
        largePen = findViewById(R.id.pen_large);
        drawingView = findViewById(R.id.drawing_view);
        button_save = findViewById(R.id.button_save);
        saveDiscardDialog = findViewById(R.id.save_discard_dialog);
        sprayPaintBtn = findViewById(R.id.sprayPaintBtn);
        strokeWidthSeekBar = findViewById(R.id.stroke_width_seek_bar);
        noteRepository = new NoteRepository(this);
        dialog_discard_btn = findViewById(R.id.dialog_discard_btn);
        dialog_cancel_btn = findViewById(R.id.dialog_cancel_btn);
        dialog_save_btn = findViewById(R.id.dialog_save_btn);
        rectangleBtn = findViewById(R.id.rectangleBtn); // Initialize the new rectangle button
        uploadImageBtn = findViewById(R.id.uploadImageBtn);
        color_blue = findViewById(R.id.color_blue);
        color_green = findViewById(R.id.color_green);
        color_teal = findViewById(R.id.color_teal);
        color_orange = findViewById(R.id.color_orange);
        new_text_btn = findViewById(R.id.new_text_btn);
        color_maroon = findViewById(R.id.color_maroon);
        color_color1 = findViewById(R.id.color_color1);
        color_grey = findViewById(R.id.color_grey);
        color_yellow = findViewById(R.id.color_yellow);
        color_pink = findViewById(R.id.color_pink);
        color_purple = findViewById(R.id.color_purple);
        // Add a listener to the drawing view to detect changes
        drawingView.setOnDrawListener(new DrawingView.OnDrawListener() {
            @Override
            public void onDrawFinished() {
                isDirty = true;
            }
        });

    }

    private void saveOrUpdateDrawing() {
        byte[] drawingData = drawingView.getDrawingData();
        NotesWidgetProvider.refreshWidget(getApplicationContext());

        if (drawingData != null && drawingData.length > 0) {
            new Thread(() -> {
                try {
                    // Decode inside the thread
                    Bitmap originalBitmap = BitmapFactory.decodeByteArray(drawingData, 0, drawingData.length);
                    if (originalBitmap == null) return;

                    // Downscale large bitmaps to avoid crashes
                    int maxSize = 2048; // limit width/height to prevent OOM
                    Bitmap drawingBitmap = scaleBitmap(originalBitmap, maxSize);

                    String filename = "drawing_" + System.currentTimeMillis() + ".png";
                    String imagePath = noteRepository.saveImageToInternalStorage(drawingBitmap, filename);
                    Note noteToSync;
                    if (imagePath != null) {

                        if (currentNoteUuid != null && currentNoteUuid.length() != 0) {
                            noteToSync = noteRepository.getNoteByCloudId(currentNoteUuid);
                            noteToSync.setImagePath(imagePath);
                            noteToSync.setDate(receivedDateFromActivities);
                            noteToSync.setUserFirebaseId(currentNoteUuid); // used for unique note id
                            noteToSync.setFontFamily(this.folderName); // used for folder name
                            noteRepository.updateNote(noteToSync);

                        } else {
                            String noteCloudId = UUID.randomUUID().toString();
                            noteToSync = new Note();
                            noteToSync.setTitle("Sketch");
                            noteToSync.setColor(Color.WHITE);
                            noteToSync.setDate(receivedDateFromActivities);
                            noteToSync.setContent("");
                            noteToSync.setUserFirebaseId(noteCloudId);
                            noteToSync.setPinned(false);
                            noteToSync.setImagePath(imagePath);
                            if (this.folderName.length() != 0)
                                noteToSync.setFontFamily(this.folderName);
                            long newId = noteRepository.addNote(noteToSync);
                            noteToSync.setId(newId);
                            Log.d("NoteApp", "Saved drawing successfully");
                        }
                        uploadAndSyncNoteToFirebase(noteToSync, drawingData, filename);
                    }
                } catch (Exception e) {
                    Log.e("NoteApp", "Error saving drawing", e);
                } finally {
                    runOnUiThread(() -> {
                        isDirty = false;
                        Intent intent = new Intent("com.noteaiapp.ACTION_NOTE_UPDATED");
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                        finish();
                    });
                }
            }).start();
        }
    }
    private void uploadAndSyncNoteToFirebase(Note noteWithLocalPath, byte[] imageData, String filename) {
        String userId = currentUser.getUid();
        StorageReference imageRef = storage.getReference().child("images/" + userId + "/" + filename);

        imageRef.putBytes(imageData)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    Log.d(TAG, "Image uploaded, getting download URL...");
                    return imageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String downloadUrl = task.getResult().toString();
                        Log.d(TAG, "Got download URL: " + downloadUrl);
                        // Update the note object with the correct cloud URL
                        noteWithLocalPath.setImagePath(downloadUrl);
                    } else {
                        Log.w(TAG, "Image upload or URL fetch failed for note " + noteWithLocalPath.getId(), task.getException());
                        noteWithLocalPath.setImagePath("");
                    }

                    // Get the note's unique ID (the UUID) from the object
                    String noteCloudId = noteWithLocalPath.getUserFirebaseId();

                    if (noteCloudId == null || noteCloudId.isEmpty()) {
                        Log.e(TAG, "Cannot save to Firestore, note's unique ID (cloudId) is missing!");
                        return; // Stop here to prevent a crash
                    }

                    // NOW, save the final note object to Firestore using the full, correct path
                    db.collection("users").document(userId).collection("notes").document(noteCloudId)
                            .set(noteWithLocalPath)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Final note " + noteCloudId + " saved to Firestore."))
                            .addOnFailureListener(e -> Log.w(TAG, "Error saving final note " + noteCloudId + " to Firestore.", e));
                });
    }

    private Bitmap scaleBitmap(Bitmap src, int maxSize) {
        int width = src.getWidth();
        int height = src.getHeight();
        if (width <= maxSize && height <= maxSize) return src;

        float ratio = (float) width / (float) height;
        int newWidth, newHeight;
        if (ratio > 1) {
            newWidth = maxSize;
            newHeight = (int) (maxSize / ratio);
        } else {
            newHeight = maxSize;
            newWidth = (int) (maxSize * ratio);
        }

        return Bitmap.createScaledBitmap(src, newWidth, newHeight, true);
    }

    @Override
    public void onBackPressed() {
        if (isDirty) {
            // Show the custom dialog if there are unsaved changes
            saveDiscardDialog.setVisibility(View.VISIBLE);

        } else {
            super.onBackPressed();
        }
    }

    /**
     * Helper method to get the current date as a formatted string.
     */
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void resetToolButtons() {
        sprayPaintBtn.getBackground().clearColorFilter();
        rectangleBtn.getBackground().clearColorFilter();
        new_text_btn.getBackground().clearColorFilter(); // NEW
    }

    public void listeners() {
        uploadImageBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        rectangleBtn.setOnClickListener(v -> {
            isRectangle = !isRectangle;
            resetToolButtons(); // Reset all buttons first
            if (isRectangle) {
                rectangleBtn.getBackground().setColorFilter(Color.parseColor("#CCCCCC"), PorterDuff.Mode.SRC_ATOP);
                drawingView.setRectangleMode(true);
            } else {
                drawingView.setRectangleMode(false);
            }
        });

        dialog_discard_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        dialog_cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDiscardDialog.setVisibility(View.INVISIBLE);
            }
        });

        dialog_save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOrUpdateDrawing();
                finish();
            }
        });

        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOrUpdateDrawing();
                finish();
            }
        });

        sprayPaintBtn.setOnClickListener(v -> {
            isSpray = !isSpray;
            if (isSpray) {
                drawingView.setStrokeWidth(20f); // Default spray paint size
                drawingView.setErasing(false);
                sprayPaintBtn.getBackground().setColorFilter(Color.parseColor("#CCCCCC"), PorterDuff.Mode.SRC_ATOP);
                drawingView.setSprayPaint(true); // Turn on spray paint mode
            } else {
                sprayPaintBtn.getBackground().clearColorFilter();
                drawingView.setStrokeWidth(20f); // Default spray paint size
                drawingView.setErasing(false);
                drawingView.setSprayPaint(false); // Turn on spray paint mode
            }
        });

        strokeWidthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // The minimum stroke width should be 1, so we add 1 to the progress.
                drawingView.setStrokeWidth(progress + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optional: Code to execute when the user starts touching the slider
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Optional: Code to execute when the user stops touching the slider
            }
        });

        blackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.BLACK);
            }
        });

        redBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.RED);
            }
        });

        blueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.BLUE);
            }
        });
        new_text_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isTextMode = true;
                resetToolButtons(); // Reset other tool highlights
                // Highlight the text button
                new_text_btn.getBackground().setColorFilter(Color.parseColor("#CCCCCC"), PorterDuff.Mode.SRC_ATOP);

                // Show the input dialog
                showTextInputDialog();
            }
        });
        // Set up click listeners for the pen size buttons
        smallPen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setStrokeWidth(10f); // 10dp
            }
        });

        eraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(true);
            }
        });

        largePen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setStrokeWidth(30f); // 30dp
            }
        });

        setColorListener();
    }

    public void setColorListener() {
        color_blue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.parseColor("#FF0099CC"));
            }
        });
        color_orange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.parseColor("#FF9800"));
            }
        });
        color_yellow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.parseColor("#FFEB3B"));
            }
        });
        color_color1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.parseColor("#1AEA97"));
            }
        });
        color_green.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.parseColor("#5BDC2D"));

            }
        });
        color_teal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.parseColor("#00BCD4"));
            }
        });
        color_pink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.parseColor("#DC2D87"));
            }
        });
        color_maroon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.parseColor("#8A3535"));
            }
        });
        color_purple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.parseColor("#673AB7"));
            }
        });
        color_grey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setErasing(false);
                drawingView.setColor(Color.parseColor("#AEB8AB"));
            }
        });
    }
    /**
     * Shows a dialog to get text input from the user.
     */
    private void showTextInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Text");

        // Set up the input
        final EditText input = new EditText(this);
        input.setHint("Type your text here");
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = input.getText().toString();
                if (!text.isEmpty()) {
                    // Pass the text to the DrawingView to be drawn
                    drawingView.addText(text, Color.BLACK,10);
                    isDirty = true; // Mark as dirty since a change was made
                }
                // Reset the state after adding text
                isTextMode = false;
                new_text_btn.getBackground().clearColorFilter();
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                // Reset the state if cancelled
                isTextMode = false;
                new_text_btn.getBackground().clearColorFilter();
            }
        });

        builder.show();
    }

    // Update resetToolButtons() to include the new button

    /**
     * Handles the result of the Intent to pick an image from the gallery.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if the request code and result are correct
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            // Get the URI of the selected image
            Uri imageUri = data.getData();
            try {
                // Get the bitmap from the URI
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                // Set the bitmap as the background in the DrawingView
                if (bitmap != null) {
                    // It's safe to use the bitmap now.
                    drawingView.setBackgroundImage(bitmap);
                    Toast.makeText(this, R.string.image_load_text, Toast.LENGTH_SHORT).show();

                } else {
                    // The bitmap is null. Handle the error gracefully.
                    Toast.makeText(this, "Failed to load image. Please try another one.", Toast.LENGTH_LONG).show();
                }

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.image_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }
}