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
     * @param note The note to be inserted.
     * @return The ID of the newly inserted row, or -1 if an error occurred.
     */
    public long insertNote(Note note) {
        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(NoteEntry.COLUMN_TITLE, note.getTitle());
        values.put(NoteEntry.COLUMN_CONTENT, note.getContent());
        values.put(NoteEntry.COLUMN_DATE, note.getDate());
        values.put(NoteEntry.COLUMN_DRAWING_DATA, note.getDrawingData());
        values.put(NoteEntry.COLUMN_COLOR, note.getColor());
        // NEW: Add the note's order
        values.put(NoteEntry.COLUMN_ORDER, note.getOrder());

        // Insert the new row, returning the primary key value of the new row
        return db.insert(NoteEntry.TABLE_NAME, null, values);
    }

    /**
     * Retrieves all notes from the database.
     * @return A list of all notes.
     */
    public List<Note> getAllNotes() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        String[] projection = {
                NoteEntry._ID,
                NoteEntry.COLUMN_TITLE,
                NoteEntry.COLUMN_CONTENT,
                NoteEntry.COLUMN_DATE,
                NoteEntry.COLUMN_DRAWING_DATA,
                NoteEntry.COLUMN_COLOR,
                NoteEntry.COLUMN_ORDER // NEW: Retrieve the order
        };

        // Query the table
        // CRITICAL FIX: Order the results by the new COLUMN_ORDER
        String sortOrder = NoteEntry.COLUMN_ORDER + " ASC";
        Cursor cursor = db.query(
                NoteEntry.TABLE_NAME,
                projection,
                null, // The columns for the WHERE clause
                null, // The values for the WHERE clause
                null, // don't group the rows
                null, // don't filter by row groups
                sortOrder // The sort order
        );

        List<Note> notes = new ArrayList<>();
        while (cursor.moveToNext()) {
            long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(NoteEntry._ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_CONTENT));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_DATE));
            byte[] drawingData = cursor.getBlob(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_DRAWING_DATA));
            int color = cursor.getInt(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_COLOR));
            // NEW: Get the order from the database
            int order = cursor.getInt(cursor.getColumnIndexOrThrow(NoteEntry.COLUMN_ORDER));

            Note note = new Note(itemId, title, content, date, drawingData, color);
            note.setOrder(order); // Set the order on the Note object
            notes.add(note);
        }
        cursor.close();
        return notes;
    }

    /**
     * Updates an existing note in the database.
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
        // NEW: Update the order as well
        values.put(NoteEntry.COLUMN_ORDER, note.getOrder());

        // Define 'where' part of query.
        String selection = NoteEntry._ID + " = ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { String.valueOf(note.getId()) };

        // Issue SQL statement.
        return db.update(
                NoteEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
    }

    /**
     * Deletes a note from the database.
     * @param noteId The ID of the note to be deleted.
     * @return The number of rows affected.
     */
    public int deleteNote(long noteId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Define 'where' part of query.
        String selection = NoteEntry._ID + " = ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { String.valueOf(noteId) };

        return db.delete(NoteEntry.TABLE_NAME, selection, selectionArgs);
    }

    /**
     * Deletes all notes from the database.
     * @return The number of rows affected.
     */
    public int deleteAllNotes() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(NoteEntry.TABLE_NAME, null, null);
    }

    // NEW: Update the order of all notes in the database
    public void updateNoteOrder(List<Note> notes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            for (int i = 0; i < notes.size(); i++) {
                Note note = notes.get(i);
                ContentValues values = new ContentValues();
                values.put(NoteEntry.COLUMN_ORDER, i);

                String selection = NoteEntry._ID + " = ?";
                String[] selectionArgs = { String.valueOf(note.getId()) };

                db.update(NoteEntry.TABLE_NAME, values, selection, selectionArgs);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
