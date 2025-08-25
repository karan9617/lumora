package com.example.keyboardai.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.keyboardai.data.NotesContract.NoteEntry;

public class NotesDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "Notes.db";

    // SQL statement to create the notes table
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + NoteEntry.TABLE_NAME + " (" +
                    NoteEntry._ID + " INTEGER PRIMARY KEY," +
                    NoteEntry.COLUMN_TITLE + " TEXT," +
                    NoteEntry.COLUMN_CONTENT + " TEXT," +
                    NoteEntry.COLUMN_DATE + " TEXT," +
                    NoteEntry.COLUMN_DRAWING_DATA + " BLOB," +
                    NoteEntry.COLUMN_COLOR + " INTEGER NOT NULL DEFAULT 0,"+
                    NotesContract.NoteEntry.COLUMN_ORDER + " INTEGER)"; // Correct: Adds the new color column

    public NotesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This is a non-destructive upgrade policy. It will add the new column without deleting old data.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + NoteEntry.TABLE_NAME +
                    " ADD COLUMN " + NoteEntry.COLUMN_COLOR + " INTEGER NOT NULL DEFAULT 0;");
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For a downgrade, you can either handle it explicitly or perform a simple upgrade
        // In a real-world app, a downgrade might involve dropping columns
        // For this case, we can simply call onUpgrade as it won't crash
        onUpgrade(db, oldVersion, newVersion);
    }
}