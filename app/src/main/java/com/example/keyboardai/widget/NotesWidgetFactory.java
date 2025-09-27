package com.example.keyboardai.widget;


import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.keyboardai.Models.Note;
import com.example.keyboardai.Notepad;
import com.example.keyboardai.R;
import com.example.keyboardai.data.NoteRepository;

import java.util.List;

public class NotesWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context context;
    private List<Note> notes;
    private NoteRepository noteRepository;

    public NotesWidgetFactory(Context context, Intent intent) {
        this.context = context;
        this.noteRepository = new NoteRepository(context);
    }

    @Override
    public void onCreate() {
        // In a real app, you would load data here synchronously.
        // It's called on the main thread, so be careful with long operations.
        loadNotes();
    }
    private void loadNotes() {
        NoteRepository repo = new NoteRepository(context);
        notes = repo.getAllNotes();  // <-- You need to implement this in NoteRepository
    }
    @Override
    public void onDataSetChanged() {
        // This is called when the data is refreshed.
        // Load your data here, potentially on a background thread.
        notes = noteRepository.getAllPinnedNotes(); // Or get pinned notes
    }

    @Override
    public void onDestroy() {
        notes.clear();
    }

    @Override
    public int getCount() {
        return notes != null ? notes.size() : 0;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position < 0 || position >= getCount()) {
            return null;
        }

        Note note = notes.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_list_item);

        views.setTextViewText(R.id.widget_note_title, note.getTitle().split(";")[0]);
        views.setTextViewText(R.id.widget_note_content, note.getContent());

        // Fill in the intent for the individual list item
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra("note_id", note.getId());
        fillInIntent.putExtra("note_title", note.getTitle().split(";")[0]);
        fillInIntent.putExtra("note_content", note.getContent());
        fillInIntent.putExtra("note_date", note.getDate());
        views.setOnClickFillInIntent(R.id.widget_list_item_root, fillInIntent); // You'll need to set an ID for your root layout in widget_list_item.xml

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}