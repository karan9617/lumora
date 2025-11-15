package com.noteaiapp.keyboardai.widget;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.data.NoteRepository;

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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser.getUid();
        notes = repo.getAllNotesForUser(userId);  // <-- You need to implement this in NoteRepository
    }
    @Override
    public void onDataSetChanged() {
        // This is called when the data is refreshed.
        // Load your data here, potentially on a background thread.
        //notes = noteRepository.getAllPinnedNotes(); // Or get pinned notes
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // A user is logged in. Fetch only their pinned notes.
            String userId = currentUser.getUid();
            // You will need to create this new method in your NoteRepository.
            notes = noteRepository.getAllNotesForUser(userId);
        } else {
            // No user is logged in. The widget should be empty.
            // Clear the existing list to ensure no old data is shown.
            if (notes != null) {
                notes.clear();
            }
        }
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
        String imagePath = note.getImagePath();
        if (imagePath != null && !imagePath.isEmpty()) {
            views.setViewVisibility(R.id.noteDrawingImageView, View.VISIBLE);
            try {
                // Use Glide to synchronously download the image from the URL into a Bitmap.
                // This is safe because getViewAt() runs on a background thread.
                Bitmap imageBitmap = Glide.with(context)
                        .asBitmap()
                        .load(imagePath)
                        .submit() // Use submit() for synchronous loading
                        .get();   // .get() blocks the thread until the download is complete

                // Set the downloaded bitmap to the ImageView
                views.setImageViewBitmap(R.id.noteDrawingImageView, imageBitmap);
                views.setViewVisibility(R.id.noteDrawingImageView, View.VISIBLE);

            } catch (Exception e) {
                // If the download fails for any reason (e.g., offline), hide the ImageView.
                views.setViewVisibility(R.id.noteDrawingImageView, View.GONE);
            }
        } else {
            // If there is no image path, make sure the ImageView is hidden.
            views.setViewVisibility(R.id.noteDrawingImageView, View.GONE);
        }
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