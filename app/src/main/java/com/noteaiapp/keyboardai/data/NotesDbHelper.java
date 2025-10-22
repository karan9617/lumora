package com.noteaiapp.keyboardai.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NotesDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notes.db";
    // IMPORTANT: Version is incremented to trigger the onUpgrade method for all users.
    private static final int DATABASE_VERSION = 6; // IMPORTANT: Version is incremented to trigger the onUpgrade method for all users.

    public static final String TABLE_NOTES = "notes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_COLOR = "color";
    public static final String COLUMN_IMAGE_PATH = "image_path";
    public static final String COLUMN_ORDER = "note_order";
    public static final String COLUMN_PINNED = "pinned";
    public static final String COLUMN_FONT_FAMILY = "font_family";
    public static final String COLUMN_FONT_SIZE = "font_size";
    public static final String COLUMN_FONT_COLOR = "font_color";

    // Labels Tables
    public static final String TABLE_LABELS = "labels";
    public static final String COLUMN_LABEL_ID = "id";
    public static final String COLUMN_LABEL_NAME = "name";
    public static final String COLUMN_LABEL_COLOR = "color";

    public static final String TABLE_NOTE_LABELS = "note_labels";
    public static final String COLUMN_NOTE_ID_FK = "note_id";
    public static final String COLUMN_LABEL_ID_FK = "label_id";

    // This statement is for BRAND NEW installations of the app.
    private static final String CREATE_TABLE_NOTES = "CREATE TABLE "
            + TABLE_NOTES + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TITLE + " TEXT,"
            + COLUMN_CONTENT + " TEXT,"
            + COLUMN_DATE + " TEXT,"
            + COLUMN_COLOR + " INTEGER,"
            + COLUMN_ORDER + " INTEGER DEFAULT 0,"
            + COLUMN_PINNED + " INTEGER DEFAULT 0,"
            + COLUMN_FONT_FAMILY + " TEXT DEFAULT 'sans-serif',"
            + COLUMN_FONT_SIZE + " REAL DEFAULT 16,"
            + COLUMN_FONT_COLOR + " TEXT DEFAULT '#000000',"
            + COLUMN_IMAGE_PATH + " TEXT"
            + ")";

    private static final String CREATE_TABLE_LABELS = "CREATE TABLE "
            + TABLE_LABELS + "("
            + COLUMN_LABEL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_LABEL_NAME + " TEXT UNIQUE,"
            + COLUMN_LABEL_COLOR + " INTEGER" + ")";

    private static final String CREATE_TABLE_NOTE_LABELS = "CREATE TABLE "
            + TABLE_NOTE_LABELS + "("
            + COLUMN_NOTE_ID_FK + " INTEGER,"
            + COLUMN_LABEL_ID_FK + " INTEGER,"
            + "FOREIGN KEY(" + COLUMN_NOTE_ID_FK + ") REFERENCES " + TABLE_NOTES + "(" + COLUMN_ID + "),"
            + "FOREIGN KEY(" + COLUMN_LABEL_ID_FK + ") REFERENCES " + TABLE_LABELS + "(" + COLUMN_LABEL_ID + "),"
            + "PRIMARY KEY(" + COLUMN_NOTE_ID_FK + ", " + COLUMN_LABEL_ID_FK + ")" + ")";

    public NotesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create all tables for a fresh installation
        db.execSQL(CREATE_TABLE_NOTES);
        db.execSQL(CREATE_TABLE_LABELS);
        db.execSQL(CREATE_TABLE_NOTE_LABELS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        android.util.Log.d("NotesDbHelper", "onUpgrade called: oldVersion=" + oldVersion + ", newVersion=" + newVersion);

        // This is a "fall-through" migration. It will apply all necessary changes
        // based on the user's old version.

        try {
            // Migration for users on a version before 3 (they don't have fonts or labels)
            if (oldVersion < 3) {
                android.util.Log.d("NotesDbHelper", "Upgrading to v3: adding fonts and labels");
                addColumnIfNotExists(db, TABLE_NOTES, COLUMN_FONT_FAMILY, "TEXT DEFAULT 'sans-serif'");
                addColumnIfNotExists(db, TABLE_NOTES, COLUMN_FONT_SIZE, "REAL DEFAULT 16");
                addColumnIfNotExists(db, TABLE_NOTES, COLUMN_FONT_COLOR, "TEXT DEFAULT '#000000'");

                // Create tables only if they don't exist
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LABELS + "("
                        + COLUMN_LABEL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + COLUMN_LABEL_NAME + " TEXT UNIQUE,"
                        + COLUMN_LABEL_COLOR + " INTEGER" + ")");

                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NOTE_LABELS + "("
                        + COLUMN_NOTE_ID_FK + " INTEGER,"
                        + COLUMN_LABEL_ID_FK + " INTEGER,"
                        + "FOREIGN KEY(" + COLUMN_NOTE_ID_FK + ") REFERENCES " + TABLE_NOTES + "(" + COLUMN_ID + "),"
                        + "FOREIGN KEY(" + COLUMN_LABEL_ID_FK + ") REFERENCES " + TABLE_LABELS + "(" + COLUMN_LABEL_ID + "),"
                        + "PRIMARY KEY(" + COLUMN_NOTE_ID_FK + ", " + COLUMN_LABEL_ID_FK + ")" + ")");
            }

            // Migration for users on a version before 4 (they don't have image_path)
            if (oldVersion < 4) {
                android.util.Log.d("NotesDbHelper", "Upgrading to v4: adding image_path");
                addColumnIfNotExists(db, TABLE_NOTES, COLUMN_IMAGE_PATH, "TEXT");
            }

            android.util.Log.d("NotesDbHelper", "onUpgrade completed successfully");

        } catch (Exception e) {
            android.util.Log.e("NotesDbHelper", "ERROR during upgrade: " + e.getMessage(), e);
            throw e; // Re-throw to see the full crash
        }
    }

    /**
     * Safely adds a column to a table only if it doesn't already exist.
     * Prevents SQLiteException when column already exists.
     */
    private void addColumnIfNotExists(SQLiteDatabase db, String tableName, String columnName, String columnDefinition) {
        if (!columnExists(db, tableName, columnName)) {
            try {
                db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
            } catch (Exception e) {
                // Log the error but don't crash - the column might have been added by another thread
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks if a column exists in a table using PRAGMA table_info.
     *
     * @param db Database instance
     * @param tableName Name of the table to check
     * @param columnName Name of the column to look for
     * @return true if column exists, false otherwise
     */
    private boolean columnExists(SQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameIndex);
                    if (columnName.equalsIgnoreCase(name)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }
}