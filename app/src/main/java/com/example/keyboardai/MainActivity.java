package com.example.keyboardai;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String PREF_FIRST_RUN = "isFirstRun";
    NoteRepository noteRepository;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivityButton = findViewById(R.id.mainActivityButton);

        listener();
        noteRepository = new NoteRepository(this);
        /*
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstRun = settings.getBoolean(PREF_FIRST_RUN, true);

        if (isFirstRun) {
            // It's the first run, show the onboarding screen
            Intent onboardingIntent = new Intent(MainActivity.this, MainActivity.class);
            startActivity(onboardingIntent);
            // After showing onboarding, set the flag to false
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(PREF_FIRST_RUN, false);
            editor.apply();
        } else {
            // Not the first run, proceed to the main app
            Intent mainAppIntent = new Intent(MainActivity.this, NotesListActivity.class);
            startActivity(mainAppIntent);
        }*/

        // Finish MainActivity to prevent the user from returning here
        //finish();
// In onCreate()

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
