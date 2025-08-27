// File: com.example.keyboardai.data.NoteRepository.java

package com.example.keyboardai.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.keyboardai.Models.Note;
import com.example.keyboardai.data.NotesContract.NoteEntry;

import java.util.ArrayList;
import java.util.List;

public class NoteRepository {

    private final NotesDbHelper dbHelper;

    public NoteRepository(Context context) {
        dbHelper = new NotesDbHelper(context);
    }

    /**
     * Inserts a new note into the database.
     * The method is updated to handle 'order' and 'isPinned' fields.
     *
     * @param note The note to be inserted.
     * @return The ID of the newly inserted row, or -1 if an error occurred.
     */
    public long insertNote(Note note) {
        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(NoteEntry.COLUMN_TITLE, note.getTitle());
        values.put(NoteEntry.COLUMN_CONTENT, note.getContent());
        values.put(NoteEntry.COLUMN_DATE, note.getDate());
        values.put(NoteEntry.COLUMN_DRAWING_DATA, note.getDrawingData());
        values.put(NoteEntry.COLUMN_COLOR, note.getColor());
        values.put(NoteEntry.COLUMN_ORDER, note.getOrder());
        // NEW: Add the note's pinned status, converting boolean to integer (0 or 1)
        values.put(NoteEntry.COLUMN_IS_PINNED, note.isPinned() ? 1 : 0);

        return db.insert(NoteEntry.TABLE_NAME, null, values);
    }

    /**
     * Retrieves all notes from the database.
     * The results are now sorted by 'isPinned' (pinned notes first),
     * and then by 'order' to maintain a consistent display order.
     *
     * @return A list of all notes.
     */
    public List<Note> getAllNotes() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                NoteEntry._ID,
                NoteEntry.COLUMN_TITLE,
                NoteEntry.COLUMN_CONTENT,
                NoteEntry.COLUMN_DATE,
                NoteEntry.COLUMN_DRAWING_DATA,
                NoteEntry.COLUMN_COLOR,
                NoteEntry.COLUMN_ORDER,
                NoteEntry.COLUMN_IS_PINNED
        };

        // The sorting is crucial here: pinned notes (1) come before unpinned notes (0), then by order.
        String sortOrder = NoteEntry.COLUMN_IS_PINNED + " DESC, " + NoteEntry.COLUMN_ORDER + " ASC";
        Cursor cursor = db.query(
                NoteEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        List<Note> notes = new ArrayList<>();
        while (cursor.moveToNext()) {
            long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(NoteEntry._ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_CONTENT));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_DATE));
            byte[] drawingData = cursor.getBlob(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_DRAWING_DATA));
            int color = cursor.getInt(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_COLOR));
            int order = cursor.getInt(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_ORDER));
            // Get the pinned status (0 for false, 1 for true)
            boolean isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_IS_PINNED)) == 1;

            Note note = new Note(itemId, title, content, date, drawingData, color, order, isPinned);
            notes.add(note);
        }
        cursor.close();
        return notes;
    }

    /**
     * Unpins a note and updates its order in the database.
     * The unpinned note is placed at the end of the list.
     *
     * @param note The note to unpin.
     */
    public void unpinNote(Note note) {
        // Find the maximum 'order' value in the database to place this note last
        int maxOrder = getMaxNoteOrder();

        // Update the note's local state
        note.setPinned(false);
        note.setOrder(maxOrder + 1);

        // Save the updated note to the database
        updateNote(note);
    }

    /**
     * Updates an existing note in the database.
     * This method is now used to save changes for both unpinning and other edits.
     *
     * @param note The note to be updated.
     * @return The number of rows affected.
     */
    public int updateNote(Note note) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(NoteEntry.COLUMN_TITLE, note.getTitle());
        values.put(NoteEntry.COLUMN_CONTENT, note.getContent());
        values.put(NoteEntry.COLUMN_DATE, note.getDate());
        values.put(NoteEntry.COLUMN_DRAWING_DATA, note.getDrawingData());
        values.put(NoteEntry.COLUMN_COLOR, note.getColor());
        values.put(NoteEntry.COLUMN_ORDER, note.getOrder());
        values.put(NoteEntry.COLUMN_IS_PINNED, note.isPinned() ? 1 : 0);

        String selection = NoteEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(note.getId()) };

        return db.update(
                NoteEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
    }

    /**
     * Deletes a note from the database.
     *
     * @param noteId The ID of the note to be deleted.
     * @return The number of rows affected.
     */
    public int deleteNote(long noteId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String selection = NoteEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(noteId) };

        return db.delete(NoteEntry.TABLE_NAME, selection, selectionArgs);
    }

    /**
     * Updates only the pinned status of a note.
     * This is an alternative to the general `updateNote` method.
     *
     * @param noteId The ID of the note to update.
     * @param isPinned The new pinned status.
     * @return The number of rows affected.
     */
    public int updateNotePinStatus(long noteId, boolean isPinned) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NoteEntry.COLUMN_IS_PINNED, isPinned ? 1 : 0);

        String selection = NoteEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(noteId) };

        return db.update(NoteEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    /**
     * Retrieves the maximum 'order' value from the notes table.
     * This is a private helper method used by `unpinNote`.
     *
     * @return The maximum integer value in the 'order' column.
     */
    private int getMaxNoteOrder() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int maxOrder = 0;
        Cursor cursor = db.rawQuery("SELECT MAX(" + NoteEntry.COLUMN_ORDER + ") FROM " + NoteEntry.TABLE_NAME, null);
        if (cursor.moveToFirst()) {
            maxOrder = cursor.getInt(0);
        }
        cursor.close();
        return maxOrder;
    }
    // New method to update the order of a note
    public void updateNoteOrder(long noteId, int order) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NotesContract.NoteEntry.COLUMN_ORDER, order);

        String selection = NotesContract.NoteEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(noteId)};

        db.update(NotesContract.NoteEntry.TABLE_NAME, values, selection, selectionArgs);
        db.close();
    }
}
