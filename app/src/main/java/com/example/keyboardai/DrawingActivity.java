package com.example.keyboardai;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.keyboardai.Models.Note;
import com.example.keyboardai.R;
import com.example.keyboardai.data.NoteRepository;
import com.example.keyboardai.ui.DrawingView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DrawingActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private ImageButton blackBtn, redBtn, blueBtn, smallPen, mediumPen, largePen;
    private TextView button_save;
    private SeekBar strokeWidthSeekBar;
    private NoteRepository noteRepository;
    private long currentNoteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);
        init();
        listeners();

        // Check if we are editing an existing note
        Intent intent = getIntent();
        if (intent.hasExtra("note_id")) {
            currentNoteId = intent.getLongExtra("note_id", -1);
            final byte[] drawingData = intent.getByteArrayExtra("drawing_data");
            if (drawingData != null && drawingData.length > 0) {
                // Use a ViewTreeObserver to wait until the view is laid out
                // and its dimensions are available before loading the bitmap.
                drawingView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Ensure the view has valid dimensions before loading the data
                        if (drawingView.getWidth() > 0 && drawingView.getHeight() > 0) {
                            drawingView.setDrawingData(drawingData);
                            Toast.makeText(getApplicationContext(), "Drawing loaded successfully!", Toast.LENGTH_SHORT).show();

                            // Remove the listener to avoid repeated calls
                            drawingView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
            }
        }
    }

    public void init(){
        blackBtn = findViewById(R.id.color_black);
        redBtn = findViewById(R.id.color_red);
        blueBtn = findViewById(R.id.color_blue);
        smallPen = findViewById(R.id.pen_small);
        mediumPen = findViewById(R.id.pen_medium);
        largePen = findViewById(R.id.pen_large);
        drawingView = findViewById(R.id.drawing_view);
        button_save = findViewById(R.id.button_save);
        strokeWidthSeekBar = findViewById(R.id.stroke_width_seek_bar);
        noteRepository = new NoteRepository(this);
    }

    private void saveOrUpdateDrawing() {
        byte[] drawingData = drawingView.getDrawingData();

        if (drawingData != null && drawingData.length > 0) {
            if (currentNoteId != -1) {
                // We are updating an existing note
                Note existingNote = new Note();
                existingNote.setId(currentNoteId);
                existingNote.setDrawingData(drawingData);
                noteRepository.updateNote(existingNote);
                Toast.makeText(this, "Drawing updated successfully!", Toast.LENGTH_SHORT).show();
            } else {
                // We are saving a new note
                Note drawingNote = new Note();
                drawingNote.setTitle("My Drawing");
                drawingNote.setDrawingData(drawingData);
                drawingNote.setDate(getCurrentDate());
                drawingNote.setPinned(false);
                drawingNote.setColor(0);
                long newRowId = noteRepository.addNote(drawingNote);
                if (newRowId != -1) {
                    Toast.makeText(this, "Drawing saved successfully!", Toast.LENGTH_SHORT).show();
                    Log.d("DrawingActivity", "Saved note with ID: " + newRowId);
                } else {
                    Toast.makeText(this, "Failed to save drawing.", Toast.LENGTH_SHORT).show();
                }
            }
            finish(); // Close the activity after saving
        } else {
            Toast.makeText(this, "No drawing to save.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Helper method to get the current date as a formatted string.
     */
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void listeners(){
        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOrUpdateDrawing();
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
                drawingView.setColor(Color.BLACK);
            }
        });

        redBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setColor(Color.RED);
            }
        });

        blueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setColor(Color.BLUE);
            }
        });

        // Set up click listeners for the pen size buttons
        smallPen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setStrokeWidth(10f); // 10dp
            }
        });

        mediumPen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setStrokeWidth(20f); // 20dp
            }
        });

        largePen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingView.setStrokeWidth(30f); // 30dp
            }
        });
    }
}
