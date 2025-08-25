package com.example.keyboardai;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.ComponentActivity;

import com.example.keyboardai.data.NoteRepository;

import java.util.ArrayList;

public class MainActivity extends ComponentActivity {

    Button mainActivityButton;
    NoteRepository noteRepository;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivityButton = findViewById(R.id.mainActivityButton);

        listener();



// In onCreate()
        noteRepository = new NoteRepository(this);
    }
    public void listener(){
        mainActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this,NotesListActivity.class);
                startActivity(i);
            }
        });
    }
}
