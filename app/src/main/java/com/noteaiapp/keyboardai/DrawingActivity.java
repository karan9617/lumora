package com.noteaiapp.keyboardai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.data.NoteRepository;
import com.noteaiapp.keyboardai.ui.DrawingView;
import com.noteaiapp.keyboardai.widget.NotesWidgetProvider;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DrawingActivity extends AppCompatActivity {
    private RelativeLayout saveDiscardDialog;
    private static final int PICK_IMAGE_REQUEST = 1;
    private DrawingView drawingView;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drawing);
        init();
        listeners();

        // Check if we are editing an existing note
        Intent intent = getIntent();
        String receivedDate = getIntent().getStringExtra(DATE_EXTRA_KEY);
        if(receivedDate != null && !receivedDate.isEmpty()){
            dateReceived = true;
            this.receivedDateFromActivities = receivedDate;
        }
        else{
            receivedDateFromActivities = getCurrentDate();
        }

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
        // Get the drawing data as a byte array
        byte[] drawingData = drawingView.getDrawingData();
        NotesWidgetProvider.refreshWidget(getApplicationContext());

        if (drawingData != null && drawingData.length > 0) {
            // Convert the byte array to a Bitmap
            Bitmap drawingBitmap = BitmapFactory.decodeByteArray(drawingData, 0, drawingData.length);
            if (drawingBitmap != null) {
                // Generate a unique file name using a timestamp
                String filename = "drawing_" + System.currentTimeMillis() + ".png";


                String imagePath = noteRepository.saveImageToInternalStorage(drawingBitmap, filename);

                if (imagePath != null) {
                    if (currentNoteId != -1) {
                        // We are updating an existing note
                        Note existingNote = noteRepository.getNoteById(currentNoteId);
                        if (existingNote != null) {
                            existingNote.setImagePath(imagePath);
                            noteRepository.updateNote(existingNote);
                            Toast.makeText(this, R.string.drawing_saved, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // We are saving a new note
                        Note drawingNote = new Note();
                        drawingNote.setTitle("Sketch");
                        drawingNote.setColor(Color.WHITE); // Default color
                        drawingNote.setDate(getCurrentDate());
                        drawingNote.setDate(receivedDateFromActivities);
                        drawingNote.setContent("");
                        drawingNote.setPinned(false);
                        drawingNote.setImagePath(imagePath);
                        long newRowId = noteRepository.addNote(drawingNote);
                        if (newRowId != -1) {
                            Toast.makeText(this, R.string.drawing_saved, Toast.LENGTH_SHORT).show();
                            Log.d("DrawingActivity", "Saved note with ID: " + newRowId);
                        } else {
                            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                    isDirty = false; // Reset the dirty flag after saving
                    finish(); // Close the activity after saving
                } else {
                }
            } else {
            }
        } else {
        }
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
                drawingView.setBackgroundImage(bitmap);
                Toast.makeText(this, R.string.image_load_text, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.image_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }
}