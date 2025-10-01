package com.noteaiapp.keyboardai.calendar;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.card.MaterialCardView;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.NotesListActivity;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.adapter.NotesAdapterPinned;
import com.noteaiapp.keyboardai.data.FileUtils;
import com.noteaiapp.keyboardai.data.NoteRepository;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView selectedDateLabel;
    NoteRepository notesRepository;
    private RecyclerView recyclerViewNotes;
    List<Note> allNotes;
    // New list to hold the notes filtered by the selected date for display
    private List<Note> displayedNotes;
    // Adapter instance
    private NotesCalendarAdapter notesAdapter;
    private Toolbar toolbar;
    // Formatter to compare dates (ignoring time component: "yyyy-MM-dd")
    private final SimpleDateFormat DATE_KEY_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Renamed for clarity and added updateData method
    private static class NotesCalendarAdapter extends RecyclerView.Adapter<NotesCalendarAdapter.ViewHolder> {
        Context context;
        List<Note> notes;

        NotesCalendarAdapter(Context context, List<Note> notes){
            this.context = context;
            this.notes = notes;
        }

        /**
         * Updates the RecyclerView data set with a new list of filtered notes.
         * @param newNotes The list of notes for the currently selected date.
         */
        public void updateData(List<Note> newNotes) {
            this.notes = newNotes;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.calendar_notes_items, parent, false);
            return new NotesCalendarAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Note note = notes.get(position); // Use 'notes'
            String[] titleArr = note.getTitle().split(";");

            if(titleArr.length >= 3){
                holder.labeltext1.setText(titleArr[1]);
                holder.labeltext2.setText(titleArr[2]);
            }
            else if(titleArr.length >=2){
                holder.labeltext1.setText(titleArr[1]);
                holder.labeltext2.setText(R.string.general_text);
            }
            else{
                holder.labeltext1.setText(R.string.general_text);
                holder.labeltext2.setText(R.string.general_text);
            }
            holder.noteTitle.setText(titleArr[0]);
            //holder.noteContent.setText(note.getContent());
            try {
                // Define the input and output date formats
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(note.getDate());
                String formattedDate = outputFormat.format(date);
                holder.noteDate.setText(formattedDate);
            } catch (ParseException e) {
                e.printStackTrace();
                holder.noteDate.setText(note.getDate()); // Fallback to original date if parsing fails
            }

            // Check if the note has a drawing and set its visibility
            byte[] drawingData = FileUtils.loadFileFromPath(note.getImagePath());
            if (drawingData != null && drawingData.length > 0) {
                if(note.getContent() == null || (note.getContent() != null && note.getContent().length() == 0)){
                    holder.labeltext1.setVisibility(View.GONE);
                    holder.labeltext2.setVisibility(View.GONE);
                }
                else{
                    holder.labeltext1.setVisibility(View.VISIBLE);
                    holder.labeltext2.setVisibility(View.VISIBLE);
                }
                try {
                    Bitmap drawingBitmap = BitmapFactory.decodeByteArray(drawingData, 0, drawingData.length);
                    if (drawingBitmap != null) {
                        holder.noteDrawing.setImageBitmap(drawingBitmap);
                        holder.noteDrawing.setVisibility(View.VISIBLE);
                        holder.noteContent.setVisibility(View.GONE);
                    } else {
                        holder.noteDrawing.setVisibility(View.GONE);
                        holder.noteContent.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    holder.noteDrawing.setVisibility(View.GONE);
                    holder.noteContent.setVisibility(View.GONE);
                }
            } else {
                String noteContent = note.getContent();
                if (noteContent != null && noteContent.startsWith(NotesListActivity.LIST_NOTE_PREFIX)) {
                    // 1. Remove the LIST_NOTE_PREFIX and any preceding whitespace
                    String listContent = noteContent.substring(NotesListActivity.LIST_NOTE_PREFIX.length()).trim();

                    // 2. Split the list content into individual lines
                    String[] items = listContent.split("\n");
                    StringBuilder previewBuilder = new StringBuilder();
                    int itemCount = 0;
                    final int MAX_PREVIEW_ITEMS = 5; // Set the maximum number of items to show

                    for (String item : items) {
                        if (itemCount >= MAX_PREVIEW_ITEMS) {
                            break; // Stop after collecting MAX_PREVIEW_ITEMS
                        }

                        String cleanedItem = item.trim();
                        if (cleanedItem.isEmpty()) {
                            continue; // Skip empty lines
                        }

                        // 3. Remove the "[x] " or "[ ] " prefix (which is 4 characters long)
                        if (cleanedItem.length() >= 4 && (cleanedItem.startsWith("[x] ") || cleanedItem.startsWith("[ ] "))) {
                            cleanedItem = cleanedItem.substring(4).trim();
                        }

                        if (cleanedItem.isEmpty()) {
                            continue; // Skip items that become empty after cleaning
                        }

                        // 4. Append the cleaned item to the preview string, separating by a newline character
                        if (previewBuilder.length() > 0) {
                            // *** CHANGED: Use newline (\n) instead of ", " ***
                            previewBuilder.append("\n");
                        }
                        previewBuilder.append(cleanedItem);
                        itemCount++;
                    }

                    // Set the cleaned content preview
                    holder.noteContent.setText(previewBuilder.toString());

                } else {
                    // Standard note, use content as is
                    holder.noteContent.setText(noteContent);
                }
                holder.noteContent.setVisibility(View.VISIBLE);
                holder.noteDrawing.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return notes.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView noteTitle,labeltext1,labeltext2;
            TextView noteContent;
            TextView noteDate;
            ImageView noteDrawing;
            ImageView notePinImageView;
            CardView noteCard;
            LinearLayout labelsContainer;
            public ViewHolder(@NonNull android.view.View itemView) {
                super(itemView);
                noteTitle = itemView.findViewById(R.id.noteTitleTextView);
                noteContent = itemView.findViewById(R.id.noteContentTextView);
                labeltext1 = itemView.findViewById(R.id.labeltext1);
                labeltext2 = itemView.findViewById(R.id.labeltext2);
                noteDate = itemView.findViewById(R.id.noteDateTextView);
                noteDrawing = itemView.findViewById(R.id.noteDrawingImageView);
                notePinImageView = itemView.findViewById(R.id.pinImageView);
                noteCard = itemView.findViewById(R.id.note_card_container);
                labelsContainer = itemView.findViewById(R.id.labels_container);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assuming R.layout.activity_calendar is available
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calendar);

        // 1. Setup Toolbar
        toolbar = findViewById(R.id.calendar_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // Enable the back button
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Note Calendar");
        }
        // Handle back button click
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize lists and repository
        allNotes = new ArrayList<>();
        displayedNotes = new ArrayList<>();
        notesRepository = new NoteRepository(this);

        // Load all notes once (unfiltered source)
        allNotes.addAll(notesRepository.getAllNotes());
        allNotes.addAll(notesRepository.getAllPinnedNotes());

        // 2. Find Views
        calendarView = findViewById(R.id.calendarView);

        Set<CalendarDay> noteDates = new HashSet<>();

        for (Note note : allNotes) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(note.getDate());
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                noteDates.add(CalendarDay.from(cal));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

// Add the decorator with flag markers

        HashMap<CalendarDay, Integer> noteCountMap = new HashMap<>();

        for (Note note : allNotes) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(note.getDate());
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                CalendarDay day = CalendarDay.from(cal);

                int currentCount = noteCountMap.getOrDefault(day, 0);
                noteCountMap.put(day, currentCount + 1);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        for (Map.Entry<CalendarDay, Integer> entry : noteCountMap.entrySet()) {
            calendarView.addDecorator(new MultiNoteDayDecorator(this, entry.getKey(), entry.getValue()));
        }


        calendarView.addDecorator(new NoteDayDecorator(this, noteDates));
        selectedDateLabel = findViewById(R.id.tv_selected_date_label);
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes);
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            // FIX: Do NOT subtract 1 — MaterialCalendarView months are already 0-indexed internally
            Calendar cal = Calendar.getInstance();
            cal.set(date.getYear(), date.getMonth(), date.getDay()); // ✅ Fix applied here

            long selectedDateMillis = cal.getTimeInMillis();

            updateSelectedDateLabel(selectedDateMillis);
            filterAndDisplayNotes(selectedDateMillis);
        });

        // 3. Setup RecyclerView
        // Initialize adapter with the empty displayedNotes list
        notesAdapter = new NotesCalendarAdapter(getApplicationContext(), displayedNotes);
        recyclerViewNotes.setAdapter(notesAdapter);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerViewNotes.setLayoutManager(layoutManager);

        // 4. Handle Date Selection
        // Initialize the label and filter the notes with today's date
        long todayMillis = System.currentTimeMillis();
        updateSelectedDateLabel(todayMillis);
        filterAndDisplayNotes(todayMillis); // <--- Initial filter applied here

    }

    /**
     * Updates the label above the notes list to show the currently selected date.
     * @param timeInMillis The timestamp of the selected date.
     */
    private void updateSelectedDateLabel(long timeInMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        String dateString = sdf.format(timeInMillis);
        selectedDateLabel.setText("Notes for " + dateString);
        selectedDateLabel.setTextColor(Color.WHITE);
    }

    /**
     * Filters the master list of notes (allNotes) to only include those created on the selected date
     * and updates the RecyclerView adapter with the results.
     * @param targetTimeInMillis The timestamp of the selected date (time part is ignored).
     */
    private void filterAndDisplayNotes(long targetTimeInMillis) {
        // Clear previous results
        displayedNotes.clear();

        // Format the selected timestamp into a date key (e.g., "2024-03-10")
        String targetDateKey = DATE_KEY_FORMAT.format(new Date(targetTimeInMillis));

        // Iterate through all notes and find matches
        for (Note note : allNotes) {
            if (isSameDay(note.getDate(), targetDateKey)) {
                displayedNotes.add(note);
            }
        }

        // Update the adapter with the new filtered list
        notesAdapter.updateData(displayedNotes);

        // Optionally show a message if no notes are found
        if (displayedNotes.isEmpty()) {
            // Note: Use a better method for showing "No Notes" than just a Toast in a real app
            Toast.makeText(this, "No notes found for " + targetDateKey, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Checks if a note's full date string (e.g., "2024-03-10 15:30:00") matches the target date key (e.g., "2024-03-10").
     * @param noteDateString The date string from the Note object (full format).
     * @param targetDateKey The date string representing the target day (date-only format).
     * @return true if the dates match, false otherwise.
     */
    private boolean isSameDay(String noteDateString, String targetDateKey) {
        // The Note's date string is "yyyy-MM-dd HH:mm:ss".
        // We only need the first 10 characters ("yyyy-MM-dd") for comparison.
        if (noteDateString != null && noteDateString.length() >= 10) {
            return noteDateString.substring(0, 10).equals(targetDateKey);
        }
        return false;
    }
}