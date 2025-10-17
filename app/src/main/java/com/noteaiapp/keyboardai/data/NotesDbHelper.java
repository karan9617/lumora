package com.noteaiapp.keyboardai.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NotesDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notes.db";
    // IMPORTANT: Version is incremented to trigger the onUpgrade method for all users.
    private static final int DATABASE_VERSION = 5;

    public static final String TABLE_NOTES = "notes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_COLOR = "color";
    public static final String COLUMN_IMAGE_PATH = "image_path";
    public static final String COLUMN_ORDER = "note_order";
    public static final String COLUMN_PINNED = "pinned";
    public static final String COLUMN_FOLDER_NAME = "folder_name"; // The problematic column
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
            // Columns added in later versions are here for new installs
            + COLUMN_FOLDER_NAME + " TEXT,"
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
        // This is a "fall-through" migration. It will apply all necessary changes
        // based on the user's old version.

        // Migration for users on a version before 2 (they don't have folder_name)
        if (oldVersion < 2) {
             db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_FOLDER_NAME + " TEXT;");
        }

        // Migration for users on a version before 3 (they don't have fonts or labels)
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_FONT_FAMILY + " TEXT DEFAULT 'sans-serif';");
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_FONT_SIZE + " REAL DEFAULT 16;");
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_FONT_COLOR + " TEXT DEFAULT '#000000';");
            db.execSQL(CREATE_TABLE_LABELS);
            db.execSQL(CREATE_TABLE_NOTE_LABELS);
        }
        
        // Migration for users on a version before 4 (they don't have image_path)
        if (oldVersion < 4) {
             db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_IMAGE_PATH + " TEXT;");
        }
        
        // Add migrations for future versions here, e.g., if (oldVersion < 6) { ... }
    }
}
