package com.noteaiapp.keyboardai.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.NotesListActivity;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.auth.LoginActivity;
import com.noteaiapp.keyboardai.data.NoteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AppWidgetConfigureActivity extends Activity {

    private static final String PREFS_NAME = "com.noteaiapp.keyboardai.widget.NotesWidgetProvider";
    private static final String PREF_PREFIX_KEY = "note_id_";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String TAG = "com.keyboardai.adapter";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private NoteRepository noteRepository;
    private RecyclerView recyclerView;
    private TextView emptyTextView;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.widget_config_layout);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not signed in. We cannot show them a list of notes.
            // Inform the user and redirect them to the Login screen.
            Toast.makeText(this, "Please log in to add a widget.", Toast.LENGTH_LONG).show();

            Intent loginIntent = new Intent(this, LoginActivity.class);
            // Add flags to make it a new task, so it doesn't feel like a deep part of the widget flow.
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
            return; // IMPORTANT: Stop executing the rest of onCreate.
        }
        noteRepository = new NoteRepository(this);
        recyclerView = findViewById(R.id.notes_recycler_view);
        emptyTextView = findViewById(R.id.empty_text_view);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadNotes();
    }

    private void syncNotesFromFirebase(NotesListActivity.FirestoreSyncCallback callback) {
        if (currentUser == null) {
            Log.w(TAG, "Cannot sync notes from cloud, user is not logged in.");
            return; // Don't proceed if there's no user
        }
        String userId = currentUser.getUid();
        Log.d(TAG, "Starting sync from Firestore for user: " + userId);
        List<Note> notesListFromFirestore = new ArrayList<>();
        // This is the query to get all notes for the current user
        db.collection("users").document(userId).collection("notes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("com.noteaiapp.keyboardai", "Successfully fetched " + task.getResult().size() + " notes from Firestore.");

                        // Perform the heavy database operations on a background thread
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Convert each document from Firestore into a Note object
                            Note cloudNote = document.toObject(Note.class);
                            notesListFromFirestore.add(cloudNote);
                        }
                        callback.onSyncComplete(notesListFromFirestore);

                    } else {
                        Log.w("com.noteaiapp.keyboardai", "Error getting documents from Firestore: ", task.getException());
                        callback.onSyncFailed(task.getException());
                    }
                    // Optional: Hide your loading indicator here
                    // swipeRefreshLayout.setRefreshing(false);
                });
    }
    private void loadNotes() {

        syncNotesFromFirebase(new NotesListActivity.FirestoreSyncCallback() {
            @Override
            public void onSyncComplete(List<Note> syncedNotes) {
                Log.d(TAG, "Sync complete. Processing " + syncedNotes.size() + " notes.");
                Executors.newSingleThreadExecutor().execute(() -> {
                    List<Note> filteredNotesForUi = new ArrayList<>();
                    for (Note note : syncedNotes) {
                        if (note.getFontFamily() == null || note.getFontFamily().isEmpty()) {
                            filteredNotesForUi.add(note);
                        }
                    }
                    // Update the UI on the main thread with the final, filtered list.
                    runOnUiThread(() -> {
                        if (filteredNotesForUi.isEmpty()) {
                            emptyTextView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            NoteConfigAdapter adapter = new NoteConfigAdapter(filteredNotesForUi, note -> onNoteSelected(note.getId()));
                            recyclerView.setAdapter(adapter);
                            emptyTextView.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    });
                });
            }

            @Override
            public void onSyncFailed(Exception e) {
                Log.e(TAG, "Sync failed: " + e.getMessage());
            }
        });
    }

    private void onNoteSelected(long noteId) {
        Context context = AppWidgetConfigureActivity.this;
        saveNoteId(context, mAppWidgetId, noteId);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        NotesWidgetProvider.updateAppWidget(context, appWidgetManager, mAppWidgetId);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    static void saveNoteId(Context context, int appWidgetId, long noteId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putLong(PREF_PREFIX_KEY + appWidgetId, noteId);
        prefs.apply();
    }

    public static long loadNoteId(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getLong(PREF_PREFIX_KEY + appWidgetId, -1L);
    }
}
