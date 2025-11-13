package com.noteaiapp.keyboardai.calendar;


import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.noteaiapp.keyboardai.DrawingActivity;
import com.noteaiapp.keyboardai.FolderNotesActivity;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.Notepad;
import com.noteaiapp.keyboardai.NotesListActivity;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.adapter.NotesAdapter;
import com.noteaiapp.keyboardai.adapter.NotesAdapterPinned;
import com.noteaiapp.keyboardai.auth.LoginActivity;
import com.noteaiapp.keyboardai.data.FileUtils;
import com.noteaiapp.keyboardai.data.NoteRepository;
import com.noteaiapp.keyboardai.imagenote.ImageNoteActivity;
import com.noteaiapp.keyboardai.listitems.ListItemsActivity;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class CalendarActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private FirebaseUser currentUser;
    private Uri cameraImageUri;
    public static final String ACTION_NOTE_SAVED = "com.noteaiapp.ACTION_NOTE_UPDATED";
    private ActivityResultLauncher<Intent> noteActivityLauncher;
    private BroadcastReceiver noteSavedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_NOTE_SAVED)) {
                // This is the moment to update the adapter!
                refreshNotesAndCalendar();
            }
        }
    };
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
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Renamed for clarity and added updateData method
    private static class NotesCalendarAdapter extends RecyclerView.Adapter<NotesCalendarAdapter.ViewHolder> {
        Context context;
        List<Note> notes;
        OnNoteClickListener listener;
        private final Set<Integer> selectedPositions = new HashSet<>();
        public interface OnNoteClickListener {
            void onNoteClick(Note note, View sharedView);
        }
        NotesCalendarAdapter(Context context, List<Note> notes, OnNoteClickListener listener){
            this.context = context;
            this.notes = notes;
            this.listener = listener;
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
        public String loadNote(String savedHtml) {
            if (savedHtml == null || savedHtml.isEmpty()) {
                return "";
            }
            // HtmlCompat.fromHtml converts HTML tags (<b>, <i>, etc.) back into Spannable text.
            return String.valueOf(HtmlCompat.fromHtml(savedHtml, HtmlCompat.FROM_HTML_MODE_COMPACT));
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
            if (note.getImagePath() != null && note.getImagePath().length() > 0) {
                if(note.getContent() == null || (note.getContent() != null && note.getContent().length() == 0)){
                    holder.labeltext1.setVisibility(View.GONE);
                    holder.labeltext2.setVisibility(View.GONE);
                }
                else{
                    holder.labeltext1.setVisibility(View.VISIBLE);
                    holder.labeltext2.setVisibility(View.VISIBLE);
                }
                Glide.with(context)
                        .load(note.getImagePath()) // Tell Glide to load the image from this file path
                        .into(holder.noteDrawing);
                // holder.noteDrawing.setImageBitmap(drawingBitmap);
                holder.noteDrawing.setVisibility(View.VISIBLE);
                try {
                    holder.noteDrawing.setVisibility(View.VISIBLE);
                    holder.noteContent.setVisibility(View.GONE);
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
                    holder.noteContent.setText(loadNote(noteContent));
                }
                holder.noteContent.setVisibility(View.VISIBLE);
                holder.noteDrawing.setVisibility(View.GONE);


            }
            holder.itemView.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                Log.d("com.noteaiapp.keyboardai","position:"+currentPosition);
                if (currentPosition != RecyclerView.NO_POSITION) {
                    // If we are in multi-selection mode, a click should toggle the selection.
                    if (selectedPositions.size() > 0) {
                        toggleSelection(currentPosition);
                    } else {
                        // Otherwise, a normal click should open the note.
                        Note clickedNote = notes.get(currentPosition);
                        listener.onNoteClick(clickedNote, holder.noteCard);
                    }
                }
            });
        }
        public void toggleSelection(int position) {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(position);
            } else {
                selectedPositions.add(position);
            }
            notifyItemChanged(position);
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

    public static final String LIST_NOTE_PREFIX = "[LIST_NOTE_START]";
    FloatingActionButton fabAddNote;
    LinearLayout optionsLayout;
    private boolean isOptionsVisible = false;
    LinearLayout option_text_layout, option_drawings_layout,option_list_layout,option_image_layout;
    private static final String DATE_EXTRA_KEY = "date_specific_notes";
    SelectedDayDecorator selectedDayDecorator;
    private TextView tvNoNotesMessage;

    Animation slideUpAnimation;
    Animation slideDownAnimation;
    OutOfMonthDecorator outOfMonthDecorator;
    private String TAG = "com.noteaiapp.keyboardai";
    View transparent_overlay;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assuming R.layout.activity_calendar is available
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calendar);
        fabAddNote = findViewById(R.id.fabAddNote);
        optionsLayout = findViewById(R.id.options_layout);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (currentUser == null) {
            // No user is signed in, we cannot proceed.
            // Redirect to the login screen to be safe.
            Toast.makeText(this, "Please log in to view the calendar.", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish(); // Close this activity
            return;   // IMPORTANT: Stop the rest of onCreate from running
        }
        noteActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Returned from note activity with result code: " + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "Note was saved. Refreshing data...");
                        // Add a small delay to ensure Firebase has processed the write
                        new android.os.Handler().postDelayed(() -> {
                            loadNotesFromDatabase();
                        }, 500); // 500ms delay
                    } else {
                        // Even if RESULT_OK wasn't set, try refreshing anyway
                        Log.d(TAG, "Result was not OK, but refreshing data anyway...");
                        loadNotesFromDatabase();
                    }
                }
        );
        calendarView = findViewById(R.id.calendarView);
        transparent_overlay = findViewById(R.id.transparent_overlay);
        tvNoNotesMessage = findViewById(R.id.tvNoNotesMessage);
        tvNoNotesMessage.setText("No notes currently for this date, press the add button to add notes :)");
        tvNoNotesMessage.setGravity(Gravity.CENTER_HORIZONTAL);
        tvNoNotesMessage.setVisibility(View.GONE); // Start hidden
        option_text_layout = findViewById(R.id.option_text_layout);
        option_image_layout = findViewById(R.id.option_image_layout);
        option_drawings_layout = findViewById(R.id.option_drawings_layout);
        option_list_layout = findViewById(R.id.option_list_layout);
        selectedDayDecorator = new SelectedDayDecorator(this);
        transparent_overlay.setOnClickListener(v -> {
            if (isOptionsVisible) {
                hideOptions();
            }
        });
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            // The image was selected from the gallery. Now, save a copy and launch ImageNoteActivity.
                            String imagePath = saveImageToAppStorage(selectedImageUri);
                            if (imagePath != null) {
                                launchImageNoteActivity(imagePath);
                            } else {
                                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                isSuccess -> {
                    if (isSuccess && cameraImageUri != null) {
                        // The photo was taken successfully. The URI is in cameraImageUri.
                        // Now, save a copy and launch ImageNoteActivity.
                        String imagePath = saveImageToAppStorage(cameraImageUri);
                        if (imagePath != null) {
                            launchImageNoteActivity(imagePath);
                        } else {
                            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
        slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        // 1. Setup Toolbar
        toolbar = findViewById(R.id.calendar_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // Enable the back button
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getApplicationContext().getString(R.string.calendar_notes));
        }
        // Handle back button click
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize lists and repository
        allNotes = new ArrayList<>();
        displayedNotes = new ArrayList<>();
        notesRepository = new NoteRepository(this);

        // Load all notes once (unfiltered source)
        // 1. Load all notes for the current user from firebase
        //    and add them to the allNotes list below
        loadNotesFromDatabase();
        ///allNotes.addAll(notesRepository.getAllNotesForUser(currentUser.getUid()));
        //allNotes.addAll(notesRepository.getAllPinnedNotes());
/*


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
*/

        selectedDateLabel = findViewById(R.id.tv_selected_date_label);
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes);
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            // FIX: Do NOT subtract 1 — MaterialCalendarView months are already 0-indexed internally
            Calendar cal = Calendar.getInstance();
            cal.set(date.getYear(), date.getMonth(), date.getDay()); //

            long selectedDateMillis = cal.getTimeInMillis();

            updateSelectedDateLabel(selectedDateMillis);
            filterAndDisplayNotes(selectedDateMillis);
            selectedDayDecorator.setDate(date);
            calendarView.invalidateDecorators();
        });

        calendarView.addDecorator(new AllDatesDecorator(this));

        // 3. Setup RecyclerView
        // Initialize adapter with the empty displayedNotes list
        notesAdapter = new NotesCalendarAdapter(getApplicationContext(), displayedNotes, new NotesCalendarAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note, View sharedView) {
                Intent intent;
                String noteContent = note.getContent();
                boolean isListNote = noteContent != null && noteContent.startsWith(LIST_NOTE_PREFIX);
                if(isListNote){
                    intent = new Intent(CalendarActivity.this, ListItemsActivity.class);
                }
                else if (note.getContent() != null && !note.getContent().isEmpty()) {
                    intent = new Intent(CalendarActivity.this, Notepad.class);
                } else if (note.getImagePath() != null && note.getImagePath().length() > 0) {
                    intent = new Intent(CalendarActivity.this, DrawingActivity.class);
                } else {
                    intent = new Intent(CalendarActivity.this, Notepad.class);
                }
                intent.putExtra("note_id", note.getId());
                intent.putExtra("note_title", note.getTitle());
                intent.putExtra("note_content", note.getContent());
                intent.putExtra("note_date", note.getDate());
                intent.putExtra("note_color", note.getColor());
                intent.putExtra("note_image_path",note.getImagePath());
                intent.putExtra("note_font_size",note.getUserFirebaseId());

                String transitionName = ViewCompat.getTransitionName(sharedView);
                if (transitionName != null) {
                    intent.putExtra("TRANSITION_NAME", transitionName);
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                            CalendarActivity.this,
                            sharedView,
                            transitionName
                    );
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            }
        });
        recyclerViewNotes.setAdapter(notesAdapter);

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerViewNotes.setLayoutManager(layoutManager);

        // 4. Handle Date Selection
        // Initialize the label and filter the notes with today's date
        long todayMillis = System.currentTimeMillis();
        updateSelectedDateLabel(todayMillis);
        filterAndDisplayNotes(todayMillis); // <--- Initial filter applied here
        listener();
    }
    // In CalendarActivity.java
// This is your NEW and CORRECT updateUiWithNotes method

    private void updateUiWithNotes() {
        // This method now assumes that 'allNotes' already contains the fresh, correct data.

        // 1. Determine which day is currently selected or default to today
        CalendarDay selectedDay = calendarView.getSelectedDate();
        long targetTimeMillis;
        if (selectedDay == null) {
            selectedDay = CalendarDay.today();
            targetTimeMillis = System.currentTimeMillis();
        } else {
            Calendar cal = Calendar.getInstance();
            cal.set(selectedDay.getYear(), selectedDay.getMonth(), selectedDay.getDay());
            targetTimeMillis = cal.getTimeInMillis();
        }

        // --- START: THIS IS THE FIX ---
        // 2. Update Calendar Decorators using the fresh 'allNotes' list
        Set<CalendarDay> noteDates = new HashSet<>();
        HashMap<CalendarDay, Integer> noteCountMap = new HashMap<>();

        for (Note note : allNotes) { // 'allNotes' now has the fresh data
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(note.getDate());
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                CalendarDay day = CalendarDay.from(cal);

                noteDates.add(day);
                int currentCount = noteCountMap.getOrDefault(day, 0);
                noteCountMap.put(day, currentCount + 1);
            } catch (Exception e) { // Catch all exceptions to be safe
                e.printStackTrace();
            }
        }

        // Clear all old decorators before adding the new ones
        calendarView.removeDecorators();

        // Re-add decorators based on the FRESH data
        for (Map.Entry<CalendarDay, Integer> entry : noteCountMap.entrySet()) {
            calendarView.addDecorator(new MultiNoteDayDecorator(this, entry.getKey(), entry.getValue()));
        }
        calendarView.addDecorator(new NoteDayDecorator(this, noteDates));
        calendarView.addDecorator(new AllDatesDecorator(this)); // Add your other static decorators back
        calendarView.addDecorator(selectedDayDecorator); // Ensure this is also re-added
        // --- END: THIS IS THE FIX ---

        // 3. Update the RecyclerView for the selected date
        updateSelectedDateLabel(targetTimeMillis);
        filterAndDisplayNotes(targetTimeMillis);
    }

    private void syncNotesFromFirebase(NotesListActivity.FirestoreSyncCallback callback) {
        if (currentUser == null) {
            Log.w(TAG, "Cannot sync notes from cloud, user is not logged in.");
            return; // Don't proceed if there's no user
        }
        String userId = currentUser.getUid();
        Log.d(TAG, "Starting sync from Firestore for user: " + userId);
        List<Note> notesListFromFirestore = new ArrayList<>();
        // This is the query to get all notes for the current user
        db.collection("users").document(userId).collection("notes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("com.noteaiapp.keyboardai", "Successfully fetched " + task.getResult().size() + " notes from Firestore.");
                        // Perform the heavy database operations on a background thread
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Convert each document from Firestore into a Note object
                            Note cloudNote = document.toObject(Note.class);
                            notesListFromFirestore.add(cloudNote);
                        }
                        callback.onSyncComplete(notesListFromFirestore);

                    } else {
                        Log.w("com.noteaiapp.keyboardai", "Error getting documents from Firestore: ", task.getException());
                        callback.onSyncFailed(task.getException());
                    }
                });
    }
    private void loadNotesFromDatabase() {
        syncNotesFromFirebase(new NotesListActivity.FirestoreSyncCallback() {
            @Override
            public void onSyncComplete(List<Note> syncedNotes) {
                Log.d(TAG, "Sync complete. Processing " + syncedNotes.size() + " notes.");
                Executors.newSingleThreadExecutor().execute(() -> {
                    List<Note> filteredNotesForUi = new ArrayList<>();
                    for (Note note : syncedNotes) {
                        filteredNotesForUi.add(note);
                    }
                    runOnUiThread(() -> {
                        allNotes.clear();
                        allNotes.addAll(filteredNotesForUi);
                        updateUiWithNotes();
                        Log.d(TAG, "UI has been refreshed with synced notes.");
                    });
                });
            }
            @Override
            public void onSyncFailed(Exception e) {
                // Handle the failure case
                runOnUiThread(() -> {
                    Toast.makeText(CalendarActivity.this, "Failed to sync notes.", Toast.LENGTH_SHORT).show();
                    //loadNotesFromLocalDatabase();
                });
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        refreshNotesAndCalendar();
        IntentFilter filter = new IntentFilter(ACTION_NOTE_SAVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(noteSavedReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the receiver when the activity is not visible to prevent leaks
        LocalBroadcastManager.getInstance(this).unregisterReceiver(noteSavedReceiver);
    }
    private void refreshNotesAndCalendar() {
        // 1. Reload all notes
        allNotes.clear();
        loadNotesFromDatabase();
        // 2. Determine which day is currently selected or default to today
        CalendarDay selectedDay = calendarView.getSelectedDate();
        long targetTimeMillis;

        if (selectedDay == null) {
            // If no day is selected (e.g., initial load), use today's date
            selectedDay = CalendarDay.today();
            targetTimeMillis = System.currentTimeMillis();
        } else {
            // Convert the selected CalendarDay back to milliseconds for filtering
            Calendar cal = Calendar.getInstance();
            cal.set(selectedDay.getYear(), selectedDay.getMonth(), selectedDay.getDay());
            targetTimeMillis = cal.getTimeInMillis();
        }

        // 3. Update Calendar Decorators (Dots/Counts)
        Set<CalendarDay> noteDates = new HashSet<>();
        HashMap<CalendarDay, Integer> noteCountMap = new HashMap<>();

        for (Note note : allNotes) {
            try {
                // Parse date string to Date object
                Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(note.getDate());
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                CalendarDay day = CalendarDay.from(cal);

                noteDates.add(day);

                int currentCount = noteCountMap.getOrDefault(day, 0);
                noteCountMap.put(day, currentCount + 1);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Clear all existing decorators before adding the new set
        calendarView.removeDecorators();

        // Add decorators based on the fresh data
        for (Map.Entry<CalendarDay, Integer> entry : noteCountMap.entrySet()) {
            calendarView.addDecorator(new MultiNoteDayDecorator(this, entry.getKey(), entry.getValue()));
        }
        calendarView.addDecorator(new AllDatesDecorator(this));
        calendarView.addDecorator(new NoteDayDecorator(this, noteDates));
        CalendarDay initialMonth = calendarView.getCurrentDate();
        outOfMonthDecorator = new OutOfMonthDecorator(initialMonth);
        calendarView.addDecorator(selectedDayDecorator);
        calendarView.addDecorator(outOfMonthDecorator);

        calendarView.setOnMonthChangedListener((widget, date) -> {
            // Remove the old decorator
            calendarView.removeDecorator(outOfMonthDecorator);

            // Create a new decorator for the new visible month
            outOfMonthDecorator = new OutOfMonthDecorator(date);
            calendarView.addDecorator(outOfMonthDecorator);
        });

        // 4. Update the UI for the selected date
        updateSelectedDateLabel(targetTimeMillis);
        filterAndDisplayNotes(targetTimeMillis);
    }

    public void listener(){
        option_list_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CalendarActivity.this, ListItemsActivity.class);
                CalendarDay selectedDay = calendarView.getSelectedDate();
                if (selectedDay == null) {
                    selectedDay = CalendarDay.today();
                }
                String dateString = DateConverter.formatCalendarDay(selectedDay, "yyyy-MM-dd HH:mm:ss");
                intent.putExtra(DATE_EXTRA_KEY, dateString);
                startActivity(intent);
                hideOptions();
            }
        });
        option_drawings_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CalendarActivity.this, DrawingActivity.class);
                CalendarDay selectedDay = calendarView.getSelectedDate();
                if (selectedDay == null) {
                    selectedDay = CalendarDay.today();
                }
                String dateString = DateConverter.formatCalendarDay(selectedDay, "yyyy-MM-dd HH:mm:ss");
                intent.putExtra(DATE_EXTRA_KEY, dateString);
                noteActivityLauncher.launch(intent); // <-- THE FIX
                hideOptions();
            }
        });
        option_image_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent intent = new Intent(NotesListActivity.this, ImageNoteActivity.class);
                //startActivity(intent);
                hideOptions();

                final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
                AlertDialog.Builder builder = new AlertDialog.Builder(CalendarActivity.this);
                builder.setTitle("Add an Image Note");

                builder.setItems(options, (dialog, item) -> {
                    if (options[item].equals("Take Photo")) {
                        // Create a file to store the camera image
                        File imageFile = null;
                        try {
                            imageFile = createImageFile();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        if (imageFile != null) {
                            // Get a content URI for the file using FileProvider
                            cameraImageUri = FileProvider.getUriForFile(
                                    CalendarActivity.this,
                                    "com.noteaiapp.keyboardai.fileprovider", // Make sure this matches your manifest
                                    imageFile
                            );
                            // Launch the camera
                            cameraLauncher.launch(cameraImageUri);
                        } else {
                            Toast.makeText(CalendarActivity.this, "Could not create image file", Toast.LENGTH_SHORT).show();
                        }

                    } else if (options[item].equals("Choose from Gallery")) {
                        // Launch the gallery
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(intent);

                    } else if (options[item].equals("Cancel")) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
        option_text_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CalendarActivity.this, Notepad.class);
                CalendarDay selectedDay = calendarView.getSelectedDate();
                if (selectedDay == null) {
                    selectedDay = CalendarDay.today();
                }
                String dateString = DateConverter.formatCalendarDay(selectedDay, "yyyy-MM-dd HH:mm:ss");
                intent.putExtra(DATE_EXTRA_KEY, dateString);
                startActivity(intent);
                hideOptions();
            }
        });

        fabAddNote.setOnClickListener(v -> {
            if (isOptionsVisible) {
                optionsLayout.setVisibility(View.VISIBLE);
                optionsLayout.startAnimation(slideUpAnimation);
                hideOptions();
            } else {
                optionsLayout.startAnimation(slideDownAnimation);
                slideDownAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        optionsLayout.setVisibility(View.GONE);
                        optionsLayout.clearAnimation();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                showOptions();
            }
        });
    }
    private void hideOptions() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fab_options_slide_down);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                optionsLayout.setVisibility(View.GONE);
                transparent_overlay.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        optionsLayout.startAnimation(animation);
        isOptionsVisible = false;
    }

    private void showOptions() {
        optionsLayout.setVisibility(View.VISIBLE);
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(250);
        optionsLayout.startAnimation(fadeIn);
        transparent_overlay.setVisibility(View.VISIBLE);
        isOptionsVisible = true;
    }
    private void launchImageNoteActivity(String imagePath) {
        Intent intent = new Intent(CalendarActivity.this, ImageNoteActivity.class);
        intent.putExtra("image_path", imagePath);
        CalendarDay selectedDay = calendarView.getSelectedDate();
        if (selectedDay == null) {
            // If no day is selected for some reason, default to today.
            selectedDay = CalendarDay.today();
        }
        String dateString = DateConverter.formatCalendarDay(selectedDay, "yyyy-MM-dd HH:mm:ss");
        intent.putExtra(DATE_EXTRA_KEY, dateString);
        noteActivityLauncher.launch(intent);
    }
    /**
     * Copies an image from a source URI (camera or gallery) to our app's private, permanent storage.
     * @param sourceUri The URI of the image to copy.
     * @return The absolute path of the newly saved image, or null on failure.
     */
    private String saveImageToAppStorage(Uri sourceUri) {
        try {
            // Create a destination file in the app's private files directory
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File destinationFile = new File(getFilesDir(), "note_image_" + timeStamp + ".jpg");

            // Open an input stream from the source URI
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            // Open an output stream to the destination file
            FileOutputStream outputStream = new FileOutputStream(destinationFile);

            // Copy the bytes
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }

            // Close the streams
            outputStream.close();
            inputStream.close();

            // Return the absolute path of our new file
            return destinationFile.getAbsolutePath();

        } catch (IOException e) {
            return null;
        }
    }
    private File createImageFile() throws IOException {
        // 1. Create a unique file name with a timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // 2. Get the directory for storing the image.
        //    Use getExternalFilesDir() for persistent media, or getExternalCacheDir() for temporary files.
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // 3. Ensure the directory exists.
        //    This is the crucial step that prevents the "No such file or directory" error.
        if (storageDir != null && !storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.d("NotesListActivity", "Failed to create directory");
                return null; // Return null if directory creation fails
            }
        }

        // 4. Create the temporary image file in the directory
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        // mCurrentPhotoPath = image.getAbsolutePath(); // If you need to save the path
        return image;
    }


    private void hideOptions(final Animation animation) {
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                optionsLayout.setVisibility(View.GONE);
                optionsLayout.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        optionsLayout.startAnimation(animation);
        isOptionsVisible = false;
    }
    /**
     * Updates the label above the notes list to show the currently selected date.
     * @param timeInMillis The timestamp of the selected date.
     */
    private void updateSelectedDateLabel(long timeInMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        String dateString = sdf.format(timeInMillis);
        selectedDateLabel.setText(getApplicationContext().getString(R.string.note_for_text)+" " + dateString);
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
            Toast.makeText(this, getApplicationContext().getString(R.string.no_notes_found)+" " + targetDateKey, Toast.LENGTH_SHORT).show();

            tvNoNotesMessage.setVisibility(View.VISIBLE);
            recyclerViewNotes.setVisibility(View.GONE);
        }else {
            tvNoNotesMessage.setVisibility(View.GONE);
            recyclerViewNotes.setVisibility(View.VISIBLE);
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