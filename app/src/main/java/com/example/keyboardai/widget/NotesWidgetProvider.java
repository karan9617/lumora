package com.example.keyboardai.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.example.keyboardai.DrawingActivity;
import com.example.keyboardai.Models.Note;
import com.example.keyboardai.Notepad;
import com.example.keyboardai.NotesListActivity;
import com.example.keyboardai.R;
import com.example.keyboardai.data.NoteRepository;

import java.io.File;

/**
 * Implementation of App Widget functionality.
 * This class handles updating the widget view and directs user interactions.
 */
public class NotesWidgetProvider extends AppWidgetProvider {

    /**
     * Updates a single widget instance.
     * @param context The application context.
     * @param appWidgetManager The AppWidgetManager instance.
     * @param appWidgetId The ID of the widget instance to update.
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notes_widget_layout);

        // Create a pending intent to launch the main app activity (NotesListActivity).
        Intent appIntent = new Intent(context, NotesListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.layout_widget_id, pendingIntent);

        // Get the note ID saved from the configuration activity.
        long noteId = AppWidgetConfigureActivity.loadNoteId(context, appWidgetId);

        // Check if a specific note has been selected for this widget.
        if (noteId != -1L) {
            // A specific note has been chosen, so we'll show its content.
            new UpdateNoteViewTask(context, appWidgetManager, views, appWidgetId).execute(noteId);
        } else {
            // No specific note is configured, so we show the full list.
            views.setViewVisibility(R.id.widget_notes_list, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.widget_empty_view, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.widget_single_note_content_layout, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_single_drawing_layout, android.view.View.GONE);

            // Set up the RemoteViewsService for the note list.
            Intent serviceIntent = new Intent(context, NotesWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
            views.setRemoteAdapter(R.id.widget_notes_list, serviceIntent);

            // Set the empty view for the note list in case there are no notes.
            views.setEmptyView(R.id.widget_notes_list, R.id.widget_empty_view);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    /**
     * An AsyncTask to fetch the note details on a background thread
     * and update the widget's view.
     */
    private static class UpdateNoteViewTask extends AsyncTask<Long, Void, Note> {
        private final Context context;
        private final AppWidgetManager appWidgetManager;
        private final RemoteViews views;
        private final int appWidgetId;

        UpdateNoteViewTask(Context context, AppWidgetManager appWidgetManager, RemoteViews views, int appWidgetId) {
            this.context = context;
            this.appWidgetManager = appWidgetManager;
            this.views = views;
            this.appWidgetId = appWidgetId;
        }

        @Override
        protected Note doInBackground(Long... noteIds) {
            if (noteIds.length > 0) {
                NoteRepository noteRepository = new NoteRepository(context);
                return noteRepository.getNoteById(noteIds[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Note selectedNote) {
            if (selectedNote != null) {
                Intent openIntent;
                long noteId = selectedNote.getId();
                views.setInt(R.id.layout_widget_id, "setBackgroundColor", selectedNote.getColor());


                if (selectedNote.getImagePath() != null && selectedNote.getImagePath().length() > 0 && (context.getString(R.string.sketch_text).trim().toLowerCase()).equalsIgnoreCase(selectedNote.getTitle().toLowerCase().trim().split(";")[0])) {
                    // Note has a drawing, so we open the DrawingActivity.
                    openIntent = new Intent(context, DrawingActivity.class);
                    openIntent.putExtra("note_id", noteId);
                } else {
                    // Note is either text-based or the image is a generic sketch,
                    // so we open the Notepad activity.
                    openIntent = new Intent(context, Notepad.class);
                    openIntent.putExtra("note_id", noteId);
                }

                // Create a pending intent and set it on the widget layout.
                PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) noteId, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                views.setOnClickPendingIntent(R.id.layout_widget_id, pendingIntent);

                // If the note has a drawing image path, prioritize showing the drawing.
                if (selectedNote.getImagePath() != null && !selectedNote.getImagePath().isEmpty()) {
                    File imageFile = new File(selectedNote.getImagePath());
                    if (imageFile.exists()) {
                        String titleFromNotes = selectedNote.getTitle().split(";")[0].trim().toLowerCase();
                        String titleFromStrings = context.getString(R.string.sketch_text).trim().toLowerCase();
                        if(titleFromNotes.equalsIgnoreCase(titleFromStrings)) {
                            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                            views.setTextViewText(R.id.widget_note_title, selectedNote.getTitle().split(";")[0]);
                            views.setImageViewBitmap(R.id.image_drawing, bitmap);
                            // Set visibility for the drawing layout
                            views.setViewVisibility(R.id.widget_single_note_content_layout, android.view.View.GONE);
                            views.setViewVisibility(R.id.widget_single_drawing_layout, android.view.View.VISIBLE);
                            views.setViewVisibility(R.id.widget_notes_list, android.view.View.GONE);
                            views.setViewVisibility(R.id.widget_empty_view, android.view.View.GONE);
                        }
                        else{
                            updateViewsForTextNote(selectedNote);
                        }
                    } else {
                        // Image file not found, fall back to showing the regular text content
                        updateViewsForTextNote(selectedNote);
                    }
                } else {
                    // No image path, so we'll display the regular note content.
                    updateViewsForTextNote(selectedNote);
                }
            } else {
                // Handle case where note was deleted or not found.
                views.setTextViewText(R.id.widget_note_title, context.getString(R.string.no_text_found));
                views.setTextViewText(R.id.widget_note_content, context.getString(R.string.widget_text_deleted));
                views.setImageViewBitmap(R.id.image_drawing, null);
            }
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        private void updateViewsForTextNote(Note note) {
            views.setTextViewText(R.id.widget_note_title, note.getTitle().split(";")[0]);
            views.setTextViewText(R.id.widget_note_content, note.getContent());
            views.setInt(R.id.layout_widget_id, "setBackgroundColor", note.getColor());

            // Set visibility for the text content layout
            views.setViewVisibility(R.id.widget_single_note_content_layout, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.widget_single_drawing_layout, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_notes_list, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_empty_view, android.view.View.GONE);
        }
    }
}
