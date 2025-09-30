package com.noteaiapp.keyboardai.calendar;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.google.android.material.card.MaterialCardView;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.NotesListActivity;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.adapter.NotesAdapterPinned;
import com.noteaiapp.keyboardai.data.FileUtils;
import com.noteaiapp.keyboardai.data.NoteRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView selectedDateLabel;
    NoteRepository notesRepository;
    private RecyclerView recyclerViewNotes;
    List<Note> allNotes;
    private Toolbar toolbar;

    // A simple adapter for placeholder content (to be replaced with Note objects later)
    // NOTE: You would normally define this in a separate file, but we keep it here for now.
    private static class PlaceholderAdapter extends RecyclerView.Adapter<PlaceholderAdapter.ViewHolder> {
        // ... Placeholder Adapter implementation goes here ...
        // For simplicity, we'll skip the full adapter implementation for now,
        // as the focus is on the Calendar UI.
        Context context;
        List<Note> allnotes;
        PlaceholderAdapter(Context context, List<Note> allnotes){
            this.context = context;
            this.allnotes = allnotes;
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.calendar_notes_items, parent, false);
            return new PlaceholderAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Note note = allnotes.get(position);
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
            // Placeholder returns 1 item to show the "No notes" message
            return 1;
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
        allNotes = new ArrayList<>();
        notesRepository = new NoteRepository(this);
        allNotes = notesRepository.getAllNotes();
        allNotes.addAll(notesRepository.getAllPinnedNotes());
        // 2. Find Views
        calendarView = findViewById(R.id.calendarView);
        selectedDateLabel = findViewById(R.id.tv_selected_date_label);
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes);

        // 3. Setup RecyclerView
        recyclerViewNotes.setLayoutManager(new LinearLayoutManager(this));
        // You will replace PlaceholderAdapter with an adapter that loads your Note objects
        recyclerViewNotes.setAdapter(new PlaceholderAdapter(getApplicationContext(), allNotes));

        // 4. Handle Date Selection
        // Initialize the label with today's date
        updateSelectedDateLabel(System.currentTimeMillis());

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Calendar month is 0-indexed (0=Jan, 11=Dec)
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);

            // Update the label when a new date is selected
            updateSelectedDateLabel(calendar.getTimeInMillis());

            // TODO: In the future, this is where you will call a method to load
            // notes from your NoteRepository for the selected 'calendar.getTimeInMillis()' date.
            Toast.makeText(CalendarActivity.this,
                    "Selected date: " + (month + 1) + "/" + dayOfMonth + "/" + year,
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSelectedDateLabel(long timeInMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        String dateString = sdf.format(timeInMillis);
        selectedDateLabel.setText("Notes for " + dateString);
    }
}
