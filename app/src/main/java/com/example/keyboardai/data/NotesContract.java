package com.example.keyboardai.data;

import android.provider.BaseColumns;

public final class NotesContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private NotesContract() {}

    /* Inner class that defines the table contents */
    public static class NoteEntry implements BaseColumns {
        public static final String TABLE_NAME = "notes";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_CONTENT = "content";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_DRAWING_DATA = "drawing_data"; // New column
        public static final String COLUMN_COLOR = "color"; // Add this new column
        public static final String COLUMN_ORDER = "note_order";
    }
}