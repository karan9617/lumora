// File: com.example.keyboardai.data.NotesDbHelper.java

package com.example.keyboardai.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.keyboardai.data.NotesContract.NoteEntry;

public class NotesDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "Notes.db";

    // SQL statement to create the notes table with all necessary columns.
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + NoteEntry.TABLE_NAME + " (" +
                    NoteEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    NoteEntry.COLUMN_TITLE + " TEXT," +
                    NoteEntry.COLUMN_CONTENT + " TEXT," +
                    NoteEntry.COLUMN_DATE + " TEXT," +
                    NoteEntry.COLUMN_DRAWING_DATA + " BLOB," +
                    NoteEntry.COLUMN_COLOR + " INTEGER NOT NULL DEFAULT 0," +
                    NoteEntry.COLUMN_IS_PINNED + " INTEGER NOT NULL DEFAULT 0," +
                    NoteEntry.COLUMN_ORDER + " INTEGER NOT NULL DEFAULT 0)";

    // SQL statement to delete the notes table.
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + NoteEntry.TABLE_NAME;

    public NotesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // This is called when the database is first created.
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This method is called to upgrade the database schema.
        // It uses a non-destructive approach by adding new columns.
        if (oldVersion < 2) {
            // Upgrade from version 1 to 2, adds the color column.
            db.execSQL("ALTER TABLE " + NoteEntry.TABLE_NAME +
                    " ADD COLUMN " + NoteEntry.COLUMN_COLOR + " INTEGER NOT NULL DEFAULT 0;");
        }
        if (oldVersion < 3) {
            // Upgrade from version 2 to 3, adds the pinning and order columns.
            db.execSQL("ALTER TABLE " + NoteEntry.TABLE_NAME +
                    " ADD COLUMN " + NoteEntry.COLUMN_IS_PINNED + " INTEGER NOT NULL DEFAULT 0;");
            db.execSQL("ALTER TABLE " + NoteEntry.TABLE_NAME +
                    " ADD COLUMN " + NoteEntry.COLUMN_ORDER + " INTEGER NOT NULL DEFAULT 0;");
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // In a simple app like this, we can just call onUpgrade, as it handles incremental changes.
        onUpgrade(db, oldVersion, newVersion);
    }
}
