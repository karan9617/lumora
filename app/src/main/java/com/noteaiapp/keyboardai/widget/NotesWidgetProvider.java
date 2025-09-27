package com.noteaiapp.keyboardai.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import com.noteaiapp.keyboardai.DrawingActivity;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.Notepad;
import com.noteaiapp.keyboardai.NotesListActivity;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.data.NoteRepository;

import java.io.File;

/**
 * Implementation of App Widget functionality.
 */
public class NotesWidgetProvider extends AppWidgetProvider {

    /**
     * Updates a single widget instance.
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notes_widget_layout);

        // Default click → open NotesListActivity
        Intent appIntent = new Intent(context, NotesListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.layout_widget_id, pendingIntent);

        // Load saved noteId
        long noteId = AppWidgetConfigureActivity.loadNoteId(context, appWidgetId);

        if (noteId != -1L) {
            // Show a single note
            new UpdateNoteViewTask(context, appWidgetManager, views, appWidgetId).execute(noteId);
        } else {
            // Show the list of notes
            views.setViewVisibility(R.id.widget_notes_list, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.widget_empty_view, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.widget_single_note_content_layout, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_single_drawing_layout, android.view.View.GONE);

            // Connect list view to RemoteViewsService
            Intent serviceIntent = new Intent(context, NotesWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
            views.setRemoteAdapter(R.id.widget_notes_list, serviceIntent);

            // Set empty view
            views.setEmptyView(R.id.widget_notes_list, R.id.widget_empty_view);

            // Add click template so each list item works
            Intent clickIntent = new Intent(context, Notepad.class);
            PendingIntent clickPendingIntent = PendingIntent.getActivity(
                    context, 0, clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setPendingIntentTemplate(R.id.widget_notes_list, clickPendingIntent);

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
     * AsyncTask to fetch single note and update view.
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

                if (selectedNote.getImagePath() != null &&
                        selectedNote.getImagePath().length() > 0 &&
                        (context.getString(R.string.sketch_text).trim().toLowerCase())
                                .equalsIgnoreCase(selectedNote.getTitle().toLowerCase().trim().split(";")[0])) {
                    // Open DrawingActivity
                    openIntent = new Intent(context, DrawingActivity.class);
                    openIntent.putExtra("note_id", noteId);
                } else {
                    // Open Notepad
                    openIntent = new Intent(context, Notepad.class);
                    openIntent.putExtra("note_id", noteId);
                }

                // PendingIntent for single note
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, (int) noteId, openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                views.setOnClickPendingIntent(R.id.layout_widget_id, pendingIntent);

                // Show drawing if available
                if (selectedNote.getImagePath() != null && !selectedNote.getImagePath().isEmpty()) {
                    File imageFile = new File(selectedNote.getImagePath());
                    if (imageFile.exists()) {
                        String titleFromNotes = selectedNote.getTitle().split(";")[0].trim().toLowerCase();
                        String titleFromStrings = context.getString(R.string.sketch_text).trim().toLowerCase();
                        if (titleFromNotes.equalsIgnoreCase(titleFromStrings)) {
                            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                            views.setTextViewText(R.id.widget_note_title, selectedNote.getTitle().split(";")[0]);
                            views.setImageViewBitmap(R.id.image_drawing, bitmap);
                            views.setViewVisibility(R.id.widget_single_note_content_layout, android.view.View.GONE);
                            views.setViewVisibility(R.id.widget_single_drawing_layout, android.view.View.VISIBLE);
                            views.setViewVisibility(R.id.widget_notes_list, android.view.View.GONE);
                            views.setViewVisibility(R.id.widget_empty_view, android.view.View.GONE);
                        } else {
                            updateViewsForTextNote(selectedNote);
                        }
                    } else {
                        updateViewsForTextNote(selectedNote);
                    }
                } else {
                    updateViewsForTextNote(selectedNote);
                }
            } else {
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
            views.setViewVisibility(R.id.widget_single_note_content_layout, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.widget_single_drawing_layout, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_notes_list, android.view.View.GONE);
            views.setViewVisibility(R.id.widget_empty_view, android.view.View.GONE);
        }
    }

    /**
     * Call this whenever notes are added/edited/deleted to refresh widget list.
     */
    public static void refreshWidget(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, NotesWidgetProvider.class);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);

        // Notify the RemoteViewsService that the data set has changed
        for (int widgetId : widgetIds) {
            appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_notes_list);
            // Optionally update the widget to reflect any single note changes
            updateAppWidget(context, appWidgetManager, widgetId);
        }
    }
}
