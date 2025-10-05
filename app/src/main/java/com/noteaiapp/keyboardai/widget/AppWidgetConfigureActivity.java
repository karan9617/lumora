package com.noteaiapp.keyboardai.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.data.NoteRepository;
import java.util.List;
import java.util.concurrent.Executors;

public class AppWidgetConfigureActivity extends Activity {

    private static final String PREFS_NAME = "com.noteaiapp.keyboardai.widget.NotesWidgetProvider";
    private static final String PREF_PREFIX_KEY = "note_id_";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private NoteRepository noteRepository;
    private RecyclerView recyclerView;
    private TextView emptyTextView;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.widget_config_layout);

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


    private void loadNotes() {

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Note> pinnedNotes = noteRepository.getAllPinnedNotes();
            List<Note> notes = noteRepository.getAllNotes();
            notes.addAll(pinnedNotes);
            runOnUiThread(() -> {
                if (notes.isEmpty()) {
                    emptyTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    NoteConfigAdapter adapter = new NoteConfigAdapter(notes, note -> onNoteSelected(note.getId()));
                    recyclerView.setAdapter(adapter);
                    emptyTextView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
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
