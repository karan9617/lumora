package com.noteaiapp.keyboardai;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.ComponentActivity;

import com.noteaiapp.keyboardai.data.NoteRepository;
import com.noteaiapp.keyboardai.operationactivity.trashfiles.NotesRepositoryTrash;

public class MainActivity extends ComponentActivity {

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String PREF_FIRST_RUN = "isFirstRun";
    NoteRepository noteRepository;
    NotesRepositoryTrash notesRepositoryTrash;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        noteRepository = new NoteRepository(this);
        notesRepositoryTrash = new NotesRepositoryTrash(this);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstRun = settings.getBoolean(PREF_FIRST_RUN, true);

        if (isFirstRun) {
            // It's the first run, show the onboarding screen
            Intent onboardingIntent = new Intent(MainActivity.this, OnboardingActivity.class);
            startActivity(onboardingIntent);
            // After showing onboarding, set the flag to false
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(PREF_FIRST_RUN, false);
            editor.apply();
        } else {
            // Not the first run, proceed to the main app
            Intent mainAppIntent = new Intent(MainActivity.this, NotesListActivity.class);
            startActivity(mainAppIntent);
        }
    }

}
