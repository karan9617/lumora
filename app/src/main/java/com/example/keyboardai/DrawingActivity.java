package com.example.keyboardai;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.keyboardai.R;
import com.example.keyboardai.ui.DrawingView;

public class DrawingActivity extends AppCompatActivity {

    private DrawingView drawingView;
    SeekBar strokeWidthSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);

        drawingView = findViewById(R.id.drawing_view);
        strokeWidthSeekBar = findViewById(R.id.stroke_width_seek_bar);
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
        // Setup the color buttons
        ImageButton blackBtn = findViewById(R.id.color_black);
        ImageButton redBtn = findViewById(R.id.color_red);
        ImageButton blueBtn = findViewById(R.id.color_blue);

        // Setup the pen size buttons
        ImageButton smallPen = findViewById(R.id.pen_small);
        ImageButton mediumPen = findViewById(R.id.pen_medium);
        ImageButton largePen = findViewById(R.id.pen_large);

        // Set up click listeners for the color buttons
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
