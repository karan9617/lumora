package com.noteaiapp.keyboardai.data;

import static java.nio.channels.SocketChannel.open;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.noteaiapp.keyboardai.Models.Label;
import com.noteaiapp.keyboardai.Models.Note;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteRepository {

    private final NotesDbHelper dbHelper;
    private final Context context;

    public NoteRepository(Context context) {
        this.context = context;
        dbHelper = new NotesDbHelper(context);
    }

    // --- Note-related methods (existing) ---
    public long addNote(Note note) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NotesDbHelper.COLUMN_TITLE, note.getTitle());
        values.put(NotesDbHelper.COLUMN_CONTENT, note.getContent());
        values.put(NotesDbHelper.COLUMN_DATE, note.getDate());
        values.put(NotesDbHelper.COLUMN_COLOR, note.getColor());
        values.put(NotesDbHelper.COLUMN_IMAGE_PATH, note.getImagePath());
        values.put(NotesDbHelper.COLUMN_ORDER, note.getOrder());
        values.put(NotesDbHelper.COLUMN_PINNED, note.isPinned() ? 1 : 0);
        values.put(NotesDbHelper.COLUMN_FONT_FAMILY, (note.getFontFamily()== null)?"":note.getFontFamily());
        values.put(NotesDbHelper.COLUMN_FONT_SIZE, note.getUserFirebaseId());
        values.put(NotesDbHelper.COLUMN_FONT_COLOR, (note.getFontColor()==null)?"":note.getFontColor());

        long newRowId = db.insert(NotesDbHelper.TABLE_NOTES, null, values);
        db.close();
        return newRowId;
    }
    public long addImageNote(Note note) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NotesDbHelper.COLUMN_TITLE, note.getTitle());
        values.put(NotesDbHelper.COLUMN_CONTENT, note.getContent());
        values.put(NotesDbHelper.COLUMN_DATE, note.getDate());
        values.put(NotesDbHelper.COLUMN_COLOR, note.getColor());
        values.put(NotesDbHelper.COLUMN_IMAGE_PATH, note.getImagePath());
        values.put(NotesDbHelper.COLUMN_ORDER, note.getOrder());
        values.put(NotesDbHelper.COLUMN_PINNED, note.isPinned() ? 1 : 0);
        values.put(NotesDbHelper.COLUMN_FONT_COLOR, note.getFontColor());
        values.put(NotesDbHelper.COLUMN_FONT_FAMILY, (note.getFontFamily() == null)?"":note.getFontFamily());
        values.put(NotesDbHelper.COLUMN_FONT_SIZE, note.getUserFirebaseId());
        //values.put(NotesDbHelper.COLUMN_FONT_COLOR, note.getFontColor());

        long newRowId = db.insert(NotesDbHelper.TABLE_NOTES, null, values);
        db.close();
        return newRowId;
    }
    // Add this method to your NoteRepository.java file

    /**
     * Deletes all notes that belong to a specific folder.
     * It queries the database for notes where the COLUMN_FONT_FAMILY
     * matches the provided folderName.
     *
     * @param folderName The name of the folder whose notes should be deleted.
     * @return The number of notes deleted.
     */
    public int deleteNotesByFolder(String folderName) {
        // Get a writable instance of the database.
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletedRows = 0;

        // Define the WHERE clause to find notes matching the folder name.
        // We are using the font_family column for this, as specified.
        String selection = NotesDbHelper.COLUMN_FONT_FAMILY + " = ?";
        String[] selectionArgs = { folderName };

        try {
            // Execute the delete operation on the notes table.
            deletedRows = db.delete(
                    NotesDbHelper.TABLE_NOTES, // The table to delete from
                    selection,                 // The "WHERE" clause (e.g., "font_family = ?")
                    selectionArgs              // The value for the placeholder '?' (the folderName)
            );
            Log.d("NoteRepository", "Deleted " + deletedRows + " notes from folder: " + folderName);
        } catch (Exception e) {
            Log.e("NoteRepository", "Error deleting notes by folder: " + e.getMessage());
        } finally {
            // It's good practice to close the database connection when you're done.
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        // Return the count of deleted notes.
        return deletedRows;
    }
// Add this method to your NoteRepository.java file

    /**
     * Finds all notes belonging to a specific folder and unassigns them
     * by setting their folder (font_family column) to an empty string.
     * This effectively moves them out of the folder without deleting them.
     *
     * @param folderName The name of the folder to unassign from notes.
     * @return The number of notes that were updated.
     */
    public int unassignFolderFromNotes(String folderName) {
        // Get a writable instance of the database.
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int updatedRows = 0;

        // 1. Create a ContentValues object to hold the new value.
        //    We want to set the font_family column to an empty string.
        ContentValues values = new ContentValues();
        values.put(NotesDbHelper.COLUMN_FONT_FAMILY, "");

        // 2. Define the WHERE clause to find all notes in the specified folder.
        String selection = NotesDbHelper.COLUMN_FONT_FAMILY + " = ?";
        String[] selectionArgs = { folderName };

        try {
            // 3. Execute the update operation on the notes table.
            updatedRows = db.update(
                    NotesDbHelper.TABLE_NOTES, // The table to update
                    values,                    // The new values to set (font_family = "")
                    selection,                 // The "WHERE" clause (e.g., "font_family = ?")
                    selectionArgs              // The value for the placeholder '?' (the folderName)
            );
            Log.d("NoteRepository", "Unassigned " + updatedRows + " notes from folder: " + folderName);
        } catch (Exception e) {
            Log.e("NoteRepository", "Error unassigning notes from folder: " + e.getMessage());
        } finally {
            // 4. Ensure the database connection is closed to prevent leaks.
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        // Return the count of updated notes.
        return updatedRows;
    }

    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();


        String query = "SELECT * FROM " + NotesDbHelper.TABLE_NOTES +
                " WHERE " + NotesDbHelper.COLUMN_PINNED + " = 0 " +
                " ORDER BY " + NotesDbHelper.COLUMN_ORDER + " ASC;";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ID)));
                note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_TITLE)));
                note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_CONTENT)));
                note.setDate(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_DATE)));
                note.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_COLOR)));
                note.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_IMAGE_PATH)));
                note.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ORDER)));
                note.setPinned(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_PINNED)) > 0);
                note.setFontColor(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_COLOR)));
                note.setFontFamily(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_FAMILY)));
                note.setUserFirebaseId(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_SIZE)));
             //   note.setFontColor(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_COLOR)));

                notes.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
       // db.close();
        return notes;
    }

    public int removeLabel(String labelName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletedRows = 0;
        try {
            deletedRows = db.delete(NotesDbHelper.TABLE_LABELS,
                    NotesDbHelper.COLUMN_LABEL_NAME + " = ?",
                    new String[]{labelName});

        } catch (Exception e) {
            Log.e("NoteRepository", "Error deleting label: " + e.getMessage());
        }
        // IMPORTANT: We do NOT call db.close() here to prevent closing the connection
        // pool while other threads might be accessing the database.
        //db.close();
        return deletedRows;
    }
    public List<Note> getAllPinnedNotes() {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Changed the query to filter for notes where the 'pinned' column is 1.
        String query = "SELECT * FROM " + NotesDbHelper.TABLE_NOTES +
                " WHERE " + NotesDbHelper.COLUMN_PINNED + " = 1 " +
                " ORDER BY " + NotesDbHelper.COLUMN_ORDER + " ASC;";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ID)));
                note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_TITLE)));
                note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_CONTENT)));
                note.setDate(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_DATE)));
                note.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_COLOR)));
                note.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_IMAGE_PATH)));
                note.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ORDER)));
                note.setPinned(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_PINNED)) > 0);
                // The font properties are commented out, assuming they are not in the database yet.
                //note.setFontFamily(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_FAMILY)));
                // note.setFontSize(cursor.getFloat(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_SIZE)));
                // note.setFontColor(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_COLOR)));

                notes.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
       // db.close();
        return notes;
    }
/*
    public List<Note> getAllFolderNotesPinned(String currentFolder) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Changed the query to filter for notes where the 'pinned' column is 1.
        String query = "SELECT * FROM " + NotesDbHelper.TABLE_NOTES +
                " WHERE " + NotesDbHelper.COLUMN_PINNED + " = 1 AND " + NotesDbHelper.COLUMN_FOLDER_NAME +" = '"+currentFolder +"'"+
                " ORDER BY " + NotesDbHelper.COLUMN_ORDER + " ASC;";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ID)));
                note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_TITLE)));
                note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_CONTENT)));
                note.setDate(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_DATE)));
                note.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_COLOR)));
                note.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_IMAGE_PATH)));
                note.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ORDER)));
                note.setPinned(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_PINNED)) > 0);
                // The font properties are commented out, assuming they are not in the database yet.
                // note.setFontFamily(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_FAMILY)));
                // note.setFontSize(cursor.getFloat(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_SIZE)));
                // note.setFontColor(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_COLOR)));

                notes.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return notes;
    }*/
    public Note getNoteById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(NotesDbHelper.TABLE_NOTES,
                null,
                NotesDbHelper.COLUMN_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null);

        Note note = null;
        if (cursor.moveToFirst()) {
            note = new Note();
            note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ID)));
            note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_TITLE)));
            note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_CONTENT)));
            note.setDate(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_DATE)));
            note.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_COLOR)));
            note.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_IMAGE_PATH)));
            note.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ORDER)));
            note.setPinned(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_PINNED)) > 0);
            note.setFontFamily(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_FAMILY)));
          //  note.setFontSize(cursor.getFloat(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_SIZE)));
            note.setFontColor(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_COLOR)));
        }
        cursor.close();
        db.close();
        return note;
    }
    // write a function below to get note from COLUMN_FONT_SIZE column
    // Add this method to your NoteRepository.java file


// Add this new method to NoteRepository.java
public Note getNoteByCloudId(String cloudId) {
    // 1. Get a readable instance of the database.
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor = null;
    Note note = null;
    // Ensure the cloudId is not null or empty to prevent errors.
    if (cloudId == null || cloudId.isEmpty()) {
        return null;
    }
    try {
        // 2. Define the selection criteria to find the note.
        // We are querying where the FONT_SIZE column matches the provided cloudId.
        String selection = NotesDbHelper.COLUMN_FONT_SIZE + " = ?";
        String[] selectionArgs = { cloudId };

        // 3. Execute the query.
        cursor = db.query(
                NotesDbHelper.TABLE_NOTES,
                null, // Select all columns
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            note = new Note();
            note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ID)));
            note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_TITLE)));
            note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_CONTENT)));
            note.setDate(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_DATE)));
            note.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_COLOR)));
            note.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_IMAGE_PATH)));
            note.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ORDER)));
            note.setPinned(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_PINNED)) > 0);
            note.setFontFamily(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_FAMILY)));
            // Set the note's unique cloud ID from the FONT_SIZE column
            note.setFontColor(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_COLOR)));
            note.setUserFirebaseId(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_SIZE)));
        }
    } catch (Exception e) {
        Log.e("NoteRepository", "Error getting note by cloudId", e);
    } finally {
        // 5. Ensure the cursor and database are closed to prevent memory leaks.
        if (cursor != null) {
            cursor.close();
        }
        // It's often better practice to manage db.close() at a higher level,
        // but closing it here ensures it's closed for this specific operation.
         db.close();
    }

    // 6. Return the found note, or null if no note was found.
    return note;
}
    /**
     * Retrieves all notes from the local database that belong to a specific user.
     *
     * @param userId The Firebase UID of the user whose notes are to be fetched.
     * @return A list of notes for that user.
     */
    // Add this new method to NoteRepository.java

    /**
     * Retrieves all notes from the local database that are relevant to a specific user.
     * This includes notes that match the given userId AND any notes that have a
     * null or empty userId (unclaimed notes from a previous app version).
     *
     * @param userId The Firebase UID of the user whose notes are to be fetched.
     * @return A list of all relevant notes for that user.
     */
    public List<Note> getAllNotesForUser(String userId) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        // Define the WHERE clause. This is the core logic:
        // It selects rows WHERE the user ID column (stored in FONT_SIZE) either:
        // 1. Exactly matches the provided userId.
        // 2. Is NULL (for very old notes before the column was used).
        // 3. Is an empty string (for notes saved with an empty user ID).
        String selection = NotesDbHelper.COLUMN_FONT_SIZE + " = ? OR " +
                NotesDbHelper.COLUMN_FONT_SIZE + " IS NULL OR " +
                NotesDbHelper.COLUMN_FONT_SIZE + " = ''";

        // The argument for the '?' placeholder in the selection.
        String[] selectionArgs = { userId };

        try {
            // Query the database with the selection criteria.
            cursor = database.query(
                    NotesDbHelper.TABLE_NOTES,
                    null, // Passing null here selects all columns.
                    selection,
                    selectionArgs,
                    null,
                    null,
                    NotesDbHelper.COLUMN_PINNED + " DESC, " + NotesDbHelper.COLUMN_ORDER + " ASC" // Order by pinned status then custom order
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // --- Manually creating the Note object from the cursor ---
                    // This replaces the non-existent cursorToNote() helper.
                    Note note = new Note();
                    note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ID)));
                    note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_TITLE)));
                    note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_CONTENT)));
                    note.setDate(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_DATE)));
                    note.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_COLOR)));
                    note.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_IMAGE_PATH)));
                    note.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ORDER)));
                    note.setPinned(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_PINNED)) > 0);

                    // For the folder name
                    note.setFontFamily(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_FAMILY)));

                    // For the user ID (stored in the FONT_SIZE column)
                    note.setUserFirebaseId(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_SIZE)));

                    notes.add(note);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("NoteRepository", "Error getting notes for user", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            database.close(); // Close the database connection
        }

        return notes;
    }


    public int updateNote(Note note) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NotesDbHelper.COLUMN_TITLE, note.getTitle());
        values.put(NotesDbHelper.COLUMN_CONTENT, note.getContent());
        values.put(NotesDbHelper.COLUMN_COLOR, note.getColor());
        values.put(NotesDbHelper.COLUMN_IMAGE_PATH, note.getImagePath());
        values.put(NotesDbHelper.COLUMN_FONT_FAMILY, (note.getFontFamily() == null)?"":note.getFontFamily());
        values.put(NotesDbHelper.COLUMN_FONT_SIZE, note.getUserFirebaseId());
      //  values.put(NotesDbHelper.COLUMN_FONT_COLOR, note.getFontColor());

        int updatedRows = db.update(NotesDbHelper.TABLE_NOTES,
                values,
                NotesDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(note.getId())});
        db.close();
        return updatedRows;
    }
    public int updateNoteByCloudId(Note note) {
        // Ensure we have a cloudId to update with, otherwise the operation is meaningless.
        if (note == null || note.getUserFirebaseId() == null || note.getUserFirebaseId().isEmpty()) {
            Log.e("NoteRepository", "Cannot update note by cloudId, the ID is null or empty.");
            return 0;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Populate the values with all the note's data, just like in updateNote().
        values.put(NotesDbHelper.COLUMN_TITLE, note.getTitle());
        values.put(NotesDbHelper.COLUMN_CONTENT, note.getContent());
        values.put(NotesDbHelper.COLUMN_COLOR, note.getColor());
        values.put(NotesDbHelper.COLUMN_IMAGE_PATH, note.getImagePath());
        values.put(NotesDbHelper.COLUMN_FONT_FAMILY, (note.getFontFamily() == null) ? "" : note.getFontFamily());
        values.put(NotesDbHelper.COLUMN_FONT_SIZE, note.getUserFirebaseId()); // The cloudId itself
        values.put(NotesDbHelper.COLUMN_PINNED, note.isPinned() ? 1 : 0); // Include other fields as well
        values.put(NotesDbHelper.COLUMN_ORDER, note.getOrder());

        // --- THIS IS THE KEY DIFFERENCE ---
        // The 'WHERE' clause of the update statement now targets the COLUMN_FONT_SIZE
        // to find the correct note to update.
        int updatedRows = db.update(NotesDbHelper.TABLE_NOTES,
                values,
                NotesDbHelper.COLUMN_FONT_SIZE + " = ?",
                new String[]{note.getUserFirebaseId()});

        db.close();
        return updatedRows;
    }
    public int updateImageNote(Note note) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NotesDbHelper.COLUMN_TITLE, note.getTitle());
        values.put(NotesDbHelper.COLUMN_CONTENT, note.getContent());
        values.put(NotesDbHelper.COLUMN_COLOR, note.getColor());
        values.put(NotesDbHelper.COLUMN_IMAGE_PATH, note.getImagePath());
        values.put(NotesDbHelper.COLUMN_FONT_FAMILY, (note.getFontFamily() == null)?"":note.getFontFamily());
        //  values.put(NotesDbHelper.COLUMN_FONT_SIZE, note.getFontSize());
        values.put(NotesDbHelper.COLUMN_FONT_COLOR, note.getFontColor());

        int updatedRows = db.update(NotesDbHelper.TABLE_NOTES,
                values,
                NotesDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(note.getId())});
        db.close();
        return updatedRows;
    }
    public void updateNoteOrder(long noteId, int newOrder) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NotesDbHelper.COLUMN_ORDER, newOrder);
        db.update(NotesDbHelper.TABLE_NOTES,
                values,
                NotesDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(noteId)});
        db.close();
    }

    public void updateNotePinStatus(long noteId, boolean isPinned) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NotesDbHelper.COLUMN_PINNED, isPinned ? 1 : 0);
        db.update(NotesDbHelper.TABLE_NOTES,
                values,
                NotesDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(noteId)});
        db.close();
    }

    public int deleteNote(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletedRows = db.delete(NotesDbHelper.TABLE_NOTES,
                NotesDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return deletedRows;
    }

    public int deleteNoteByCloudId(String cloudId) {
        // 1. Ensure the cloudId is valid before attempting to delete.
        if (cloudId == null || cloudId.isEmpty()) {
            Log.e("NoteRepository", "Cannot delete note, the provided cloudId is null or empty.");
            return 0;
        }

        // 2. Get a writable instance of the database.
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletedRows = 0;

        try {
            String selection = NotesDbHelper.COLUMN_FONT_SIZE + " = ?";
            String[] selectionArgs = { cloudId };
            deletedRows = db.delete(NotesDbHelper.TABLE_NOTES,
                    selection,
                    selectionArgs);
            if (deletedRows > 0) {
                Log.d("NoteRepository", "Successfully deleted note with cloudId: " + cloudId);
            } else {
                Log.w("NoteRepository", "No note found with cloudId to delete: " + cloudId);
            }
        } catch (Exception e) {
            Log.e("NoteRepository", "Error deleting note by cloudId", e);
        } finally {
            db.close();
        }

        // 6. Return the number of rows affected.
        return deletedRows;
    }
    public void updateNotePinStatusBulk(List<Note> notes, boolean isPinned) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Note note : notes) {
                ContentValues values = new ContentValues();
                values.put(NotesDbHelper.COLUMN_PINNED, isPinned ? 1 : 0);
                db.update(NotesDbHelper.TABLE_NOTES, values, NotesDbHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(note.getId())});
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("NoteRepository", "Error updating pin status for multiple notes", e);
        } finally {
            db.endTransaction();
            db.close();
        }
    }
    // --- New Label-related methods ---
    public long addLabel(Label label) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NotesDbHelper.COLUMN_LABEL_NAME, label.getName());
        values.put(NotesDbHelper.COLUMN_LABEL_COLOR, label.getColor());
        long newRowId = db.insertWithOnConflict(NotesDbHelper.TABLE_LABELS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return newRowId;
    }

    public void addLabelToNote(long noteId, long labelId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NotesDbHelper.COLUMN_NOTE_ID_FK, noteId);
        values.put(NotesDbHelper.COLUMN_LABEL_ID_FK, labelId);
        db.insert(NotesDbHelper.TABLE_NOTE_LABELS, null, values);
        db.close();
    }

    public void removeLabelFromNote(long noteId, long labelId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(NotesDbHelper.TABLE_NOTE_LABELS,
                NotesDbHelper.COLUMN_NOTE_ID_FK + " = ? AND " + NotesDbHelper.COLUMN_LABEL_ID_FK + " = ?",
                new String[]{String.valueOf(noteId), String.valueOf(labelId)});
        db.close();
    }

    public List<Label> getLabelsForNote(long noteId) {
        List<Label> labels = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT T1.* FROM " + NotesDbHelper.TABLE_LABELS + " T1 INNER JOIN " +
                NotesDbHelper.TABLE_NOTE_LABELS + " T2 ON T1." + NotesDbHelper.COLUMN_LABEL_ID +
                " = T2." + NotesDbHelper.COLUMN_LABEL_ID_FK + " WHERE T2." +
                NotesDbHelper.COLUMN_NOTE_ID_FK + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(noteId)});

        if (cursor.moveToFirst()) {
            do {
                Label label = new Label();
                label.setId(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_LABEL_ID)));
                label.setName(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_LABEL_NAME)));
                label.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_LABEL_COLOR)));
                labels.add(label);
            } while (cursor.moveToNext());
        }
        cursor.close();
        //db.close();
        return labels;
    }

    public List<Label> getAllLabels() {
        List<Label> labels = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT * FROM " + NotesDbHelper.TABLE_LABELS;
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Label label = new Label();
                label.setId(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_LABEL_ID)));
                label.setName(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_LABEL_NAME)));
                label.setColor(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_LABEL_COLOR)));
                labels.add(label);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return labels;
    }

    /**
     * Saves a bitmap to the app's internal storage and returns the file path.
     * @param bitmap The bitmap to save.
     * @param filename The desired filename (e.g., "my_image.png").
     * @return The absolute path to the saved image file, or null if saving fails.
     */

    public String saveImageToInternalStorage(Bitmap bitmap, String filename) {
        try {
            File rootDir = context.getFilesDir();
            String DRAWING_IMAGES_DIR = "drawing_notes";
            //File directory = context.getDir("images", Context.MODE_PRIVATE);
            File directory = new File(rootDir, DRAWING_IMAGES_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File imageFile = new File(directory, filename);
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            // IMPORTANT: Recycle the temporary bitmaps
            bitmap.recycle();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e("NoteRepository", "Error saving image", e);
            bitmap.recycle();
            return null;
        }
    }
    public static boolean saveBytesToFile(byte[] data, String filePath) {
        if (data == null || data.length == 0 || filePath == null || filePath.isEmpty()) {
            return false;
        }

        File file = new File(filePath);

        // Ensure the parent directories exist
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Loads a bitmap from a file path in the app's internal storage.
     * @param imagePath The absolute path to the image file.
     * @return The loaded Bitmap, or null if loading fails.
     */
    public Bitmap loadImageFromInternalStorage(String imagePath) {
        if (imagePath == null) {
            return null;
        }
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        }
        return null;
    }
    public void dbclose(){
        if(dbHelper.getReadableDatabase().isOpen())
            dbHelper.close();
    }
}
