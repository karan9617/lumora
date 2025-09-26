package com.example.keyboardai.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NotesDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 2; // Bump this to trigger onUpgrade()

    public static final String TABLE_NOTES = "notes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_COLOR = "color";
    public static final String COLUMN_IMAGE_PATH = "image_path"; // New column to store the file path
    public static final String COLUMN_ORDER = "note_order";
    public static final String COLUMN_PINNED = "pinned";
    public static final String COLUMN_FONT_FAMILY = "font_family";
    public static final String COLUMN_FONT_SIZE = "font_size";
    public static final String COLUMN_FONT_COLOR = "font_color";

    // Labels
    public static final String TABLE_LABELS = "labels";
    public static final String COLUMN_LABEL_ID = "id";
    public static final String COLUMN_LABEL_NAME = "name";
    public static final String COLUMN_LABEL_COLOR = "color";

    public static final String TABLE_NOTE_LABELS = "note_labels";
    public static final String COLUMN_NOTE_ID_FK = "note_id";
    public static final String COLUMN_LABEL_ID_FK = "label_id";

    private static final String CREATE_TABLE_NOTES = "CREATE TABLE "
            + TABLE_NOTES + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_TITLE + " TEXT,"
            + COLUMN_CONTENT + " TEXT,"
            + COLUMN_DATE + " TEXT,"
            + COLUMN_COLOR + " INTEGER,"
            + COLUMN_IMAGE_PATH + " TEXT," // Use TEXT for file path
            + COLUMN_ORDER + " INTEGER DEFAULT 0,"
            + COLUMN_PINNED + " INTEGER DEFAULT 0,"
            + COLUMN_FONT_FAMILY + " TEXT DEFAULT 'sans-serif',"
            + COLUMN_FONT_SIZE + " REAL DEFAULT 16,"
            + COLUMN_FONT_COLOR + " TEXT DEFAULT '#000000'" + ")";

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
        db.execSQL(CREATE_TABLE_NOTES);
        db.execSQL(CREATE_TABLE_LABELS);
        db.execSQL(CREATE_TABLE_NOTE_LABELS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Only handle future upgrades
        if (oldVersion < 3) {
            // If user is upgrading from v1 → v3, add fonts + labels
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_FONT_FAMILY + " TEXT DEFAULT 'sans-serif';");
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_FONT_SIZE + " REAL DEFAULT 16;");
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_FONT_COLOR + " TEXT DEFAULT '#000000';");
            db.execSQL(CREATE_TABLE_LABELS);
            db.execSQL(CREATE_TABLE_NOTE_LABELS);
        }
        if (oldVersion < 4) {
            // Add the new image_path column and drop the old drawing_data column
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COLUMN_IMAGE_PATH + " TEXT;");
            db.execSQL("CREATE TABLE temp_notes AS SELECT " +
                    COLUMN_ID + ", " + COLUMN_TITLE + ", " + COLUMN_CONTENT + ", " +
                    COLUMN_DATE + ", " + COLUMN_COLOR + ", " + COLUMN_IMAGE_PATH + ", " +
                    COLUMN_ORDER + ", " + COLUMN_PINNED + ", " + COLUMN_FONT_FAMILY + ", " +
                    COLUMN_FONT_SIZE + ", " + COLUMN_FONT_COLOR +
                    " FROM " + TABLE_NOTES + ";");
            db.execSQL("DROP TABLE " + TABLE_NOTES + ";");
            db.execSQL("ALTER TABLE temp_notes RENAME TO " + TABLE_NOTES + ";");
        }
    }

}
