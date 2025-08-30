package com.example.keyboardai.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.keyboardai.Models.Label;
import com.example.keyboardai.Models.Note;

import java.util.ArrayList;
import java.util.List;

public class NoteRepository {

    private final NotesDbHelper dbHelper;

    public NoteRepository(Context context) {
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
        values.put(NotesDbHelper.COLUMN_DRAWING_DATA, note.getDrawingData());
        values.put(NotesDbHelper.COLUMN_ORDER, note.getOrder());
        values.put(NotesDbHelper.COLUMN_PINNED, note.isPinned() ? 1 : 0);
        //values.put(NotesDbHelper.COLUMN_FONT_FAMILY, note.getFontFamily());
        //values.put(NotesDbHelper.COLUMN_FONT_SIZE, note.getFontSize());
        //values.put(NotesDbHelper.COLUMN_FONT_COLOR, note.getFontColor());

        long newRowId = db.insert(NotesDbHelper.TABLE_NOTES, null, values);
        db.close();
        return newRowId;
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
                note.setDrawingData(cursor.getBlob(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_DRAWING_DATA)));
                note.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ORDER)));
                note.setPinned(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_PINNED)) > 0);
             //   note.setFontFamily(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_FAMILY)));
             //   note.setFontSize(cursor.getFloat(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_SIZE)));
             //   note.setFontColor(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_COLOR)));

                notes.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return notes;
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
                note.setDrawingData(cursor.getBlob(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_DRAWING_DATA)));
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
    }
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
            note.setDrawingData(cursor.getBlob(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_DRAWING_DATA)));
            note.setOrder(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_ORDER)));
            note.setPinned(cursor.getInt(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_PINNED)) > 0);
          //  note.setFontFamily(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_FAMILY)));
          //  note.setFontSize(cursor.getFloat(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_SIZE)));
          //  note.setFontColor(cursor.getString(cursor.getColumnIndexOrThrow(NotesDbHelper.COLUMN_FONT_COLOR)));
        }
        cursor.close();
        db.close();
        return note;
    }

    public int updateNote(Note note) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NotesDbHelper.COLUMN_TITLE, note.getTitle());
        values.put(NotesDbHelper.COLUMN_CONTENT, note.getContent());
        values.put(NotesDbHelper.COLUMN_COLOR, note.getColor());
        values.put(NotesDbHelper.COLUMN_DRAWING_DATA, note.getDrawingData());
      //  values.put(NotesDbHelper.COLUMN_FONT_FAMILY, note.getFontFamily());
      //  values.put(NotesDbHelper.COLUMN_FONT_SIZE, note.getFontSize());
      //  values.put(NotesDbHelper.COLUMN_FONT_COLOR, note.getFontColor());

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
}
