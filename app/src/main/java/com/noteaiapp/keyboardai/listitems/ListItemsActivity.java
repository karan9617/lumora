package com.noteaiapp.keyboardai.listitems;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint; // Import Paint to use STRIKE_THRU_TEXT_FLAG
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.data.NoteRepository;
import com.noteaiapp.keyboardai.NotesListActivity; // Import to access the LIST_NOTE_PREFIX
import com.noteaiapp.keyboardai.operationactivity.trashfiles.NotesRepositoryTrash;
import com.noteaiapp.keyboardai.widget.NotesWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

public class ListItemsActivity extends AppCompatActivity {

    private EditText noteTitleEditText;
    private RecyclerView recyclerViewList;
    public static final String EXTRA_FOLDER_NAME = "FOLDER_NAME";
    private String folderName = "";
    private FirebaseUser currentUser;

    private Toolbar toolbar;
    private NoteRepository noteRepository;
    NotesRepositoryTrash notesRepositoryTrash;
    ConstraintLayout listItemLayout;
    private int selectedColor = Color.parseColor("#232323");
    private List<ListItem> listItems = new ArrayList<>();
    private boolean isNoteModified = false;
    private ListAdapter listAdapter;

    private long noteId = -1;
    private int noteColor = Color.parseColor("#FFFFFF"); // Default white
    private static final String DATE_EXTRA_KEY = "date_specific_notes";
    private boolean dateReceived = false;
    private String receivedDateFromActivities = "";
    private String TAG = "com.noteaiapp.keyboardai";

    // --- START: ADD THESE LINES ---
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String currentNoteUuid;
    private static class ListItem {
        String content;
        boolean isChecked;

        public ListItem(String content, boolean isChecked) {
            this.content = content;
            this.isChecked = isChecked;
        }

        // Used internally for display/debugging, but the serializeListItems method is used for storage.
        @NonNull
        @Override
        public String toString() {
            return (isChecked ? "[x] " : "[ ] ") + content;
        }
    }

    private void fetchNoteFromFirebase(String cloudId) {

        if (currentUser == null || cloudId == null || cloudId.length() == 0) {
            Log.w(TAG, "User not logged in, falling back to local database.");
            loadNoteData(getIntent());
            return;
        }

        String userId = currentUser.getUid();
        // Show a ProgressBar if you have one
        Log.d(TAG, "List item user uid:"+userId);
        db.collection("users").document(userId).collection("notes").document(cloudId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Successfully fetched list note from Firestore.");
                        Note cloudNote = documentSnapshot.toObject(Note.class);
                        Log.d(TAG, "cloudNote :"+cloudNote.getTitle()+"|cloudNote content:"+cloudNote.getContent());

                        if (cloudNote != null) {
                            populateUiWithNoteData(cloudNote);
                        }
                    } else {
                        Log.w(TAG, "List note not found in Firestore, falling back to local.");
                        loadNoteData(getIntent());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch from Firestore. Falling back to local.", e);
                    loadNoteData(getIntent());
                });
    }
    public void populateUiWithNoteData(Note currentNode){
        if (currentNode == null) {
            // This is a new note, add one empty item to start
            if (listItems.isEmpty()) {
                addNewListItem("");
            }
            return;
        }

        // --- This is an existing note, so populate the UI ---

        // 1. Get the raw data from the note object
        String title = currentNode.getTitle();
        String content = currentNode.getContent(); // This is the serialized list
        noteColor = currentNode.getColor();
        selectedColor = noteColor;
        Log.w(TAG, "title final:" +title + "|content:"+content);

        // 2. Set the title
        if (title != null) {
            noteTitleEditText.setText(title.split(";")[0]); // Safely get the title part
        }

        // --- 3. THIS IS THE FIX FOR THE BLANK SCREEN ---
        // Clear the existing list before adding new items
        listItems.clear();

        if (content != null && !content.isEmpty()) {
            String listContent = content;
            // Remove the prefix if it exists
            if (listContent.startsWith(NotesListActivity.LIST_NOTE_PREFIX)) {
                listContent = listContent.substring(NotesListActivity.LIST_NOTE_PREFIX.length()).trim();
            }

            String[] items = listContent.split("\n");
            for (String item : items) {
                if (item.trim().isEmpty()) continue; // Skip empty lines

                boolean isChecked = item.startsWith("[x] ");
                // The prefix is 4 characters long ("[x] " or "[ ] ")
                String itemContent = item.length() >= 4 ? item.substring(4).trim() : item.trim();

                listItems.add(new ListItem(itemContent, isChecked));
            }
        }

        // 4. Notify the adapter *once*, after the list has been fully populated.
        listAdapter.notifyDataSetChanged();
        //
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list_items);

        toolbar = findViewById(R.id.list_toolbar);
        setSupportActionBar(toolbar);
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        this.currentNoteUuid = (getIntent().getStringExtra("note_font_size") == null)? "": getIntent().getStringExtra("note_font_size");

        String receivedFolder = getIntent().getStringExtra(EXTRA_FOLDER_NAME);
        this.folderName = (receivedFolder != null && !receivedFolder.isEmpty())? receivedFolder:"";

        // Set up the toolbar to act as the action bar and add a back/close icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Assuming you have an ic_close drawable
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        getSupportActionBar().setTitle("");
        notesRepositoryTrash = new NotesRepositoryTrash(this);
        noteTitleEditText = findViewById(R.id.noteTitleEditText);
        recyclerViewList = findViewById(R.id.recyclerViewList);
        ImageButton addItemButton = findViewById(R.id.addItemButton);
        listItemLayout = findViewById(R.id.listItemLayout);
        noteRepository = new NoteRepository(this);
        recyclerViewList.setLayoutManager(new LinearLayoutManager(this));
        listAdapter = new ListAdapter(this, listItems);
        String receivedDate = getIntent().getStringExtra(DATE_EXTRA_KEY);
        if(receivedDate != null && !receivedDate.isEmpty()){
            dateReceived = true;
            this.receivedDateFromActivities = receivedDate;
        }
        else{
            receivedDateFromActivities = getCurrentDate();
        }

        fetchNoteFromFirebase(currentNoteUuid);
        // Setup RecyclerView

        recyclerViewList.setAdapter(listAdapter);

        // Get the date from the intent

        // Add New Item Button Listener
        addItemButton.setOnClickListener(v -> addNewListItem(""));

        // Adjust input mode to prevent the layout from shrinking when the keyboard appears
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private void addNewListItem(String content) {
        int newPosition = listItems.size();
        listItems.add(new ListItem(content, false));
        listAdapter.notifyItemInserted(newPosition);
        // Scroll to the new item so it's visible
        recyclerViewList.scrollToPosition(newPosition);

        // Post the action to the RecyclerView's message queue to ensure the new view has been created and bound
        recyclerViewList.post(() -> {
            RecyclerView.ViewHolder holder = recyclerViewList.findViewHolderForAdapterPosition(newPosition);
            if (holder instanceof ListAdapter.ViewHolder) {
                ListAdapter.ViewHolder listHolder = (ListAdapter.ViewHolder) holder;

                // Request focus on the item's content field
                listHolder.itemContent.requestFocus();

                // Manually show the keyboard for the focused view
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(listHolder.itemContent, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    /**
     * Loads note data if the activity was opened to edit an existing note.
     * It deserializes the content string back into individual ListItem objects.
     */
    private void loadNoteData(Intent intent) {
        noteId = intent.getLongExtra("note_id", -1);
        Note currentNode = noteRepository.getNoteById(noteId);
        String title = (currentNode == null)?"":currentNode.getTitle();
        String content = (currentNode == null ) ?"":currentNode.getContent();// This is the serialized list
        noteColor = (currentNode == null )? Color.WHITE:currentNode.getColor();
        selectedColor = (currentNode == null )? Color.WHITE:currentNode.getColor();
        if (noteId != -1) {
            if (title != null) noteTitleEditText.setText(title);

            // Deserialize content back into list items
            if (content != null && !content.isEmpty()) {

                // 1. Remove the LIST_NOTE_PREFIX before processing the list content
                String listContent = content;
                if (listContent.startsWith(NotesListActivity.LIST_NOTE_PREFIX)) {
                    listContent = listContent.substring(NotesListActivity.LIST_NOTE_PREFIX.length()).trim();
                }

                String[] items = listContent.split("\n");
                for (String item : items) {
                    if (item.trim().isEmpty()) continue; // Skip empty lines

                    // Check for "[x] " (checked) or "[ ] " (unchecked) prefix
                    boolean isChecked = item.startsWith("[x] ");
                    // The prefix is 4 characters long ("[*]" + space)
                    String itemContent = item.length() >= 4 ? item.substring(4).trim() : item.trim();

                    listItems.add(new ListItem(itemContent, isChecked));
                }
            }
            listAdapter.notifyDataSetChanged();
        } else {
            // New note, add one empty item to start if the list is empty
            if (listItems.isEmpty()) {
                addNewListItem("");
            }
        }
    }
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
    /**
     * Serializes the list of items into a single string to be saved in the Note content field.
     * Format: "[ ] Item text 1\n[x] Item text 2\n[ ] Item text 3"
     */
    private String serializeListItems() {
        StringBuilder sb = new StringBuilder();
        // Filter out empty rows when saving
        for (ListItem item : listItems) {
            if (!item.content.trim().isEmpty()) {
                // Save state: "[x] " for checked, "[ ] " for unchecked
                sb.append(item.isChecked ? "[x] " : "[ ] ")
                        .append(item.content.trim())
                        .append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Utility to synchronize the text from the currently focused EditText back to the data model.
     * This handles the edge case where the user hits save/back while an item is being edited.
     */
    private void synchronizeRecyclerViewData() {
        // Force any currently focused view to lose focus, which triggers the item's
        // OnFocusChangeListener to save the content back to the ListItem model.
        View focusedView = getCurrentFocus();
        if (focusedView != null) {
            focusedView.clearFocus();
        }

        // Even better: manually iterate through visible items to capture content,
        // especially for the row currently being edited.
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerViewList.getLayoutManager();
        if (layoutManager == null) return;

        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();

        for (int i = firstVisible; i <= lastVisible; i++) {
            RecyclerView.ViewHolder holder = recyclerViewList.findViewHolderForAdapterPosition(i);
            if (holder instanceof ListAdapter.ViewHolder) {
                ListAdapter.ViewHolder listHolder = (ListAdapter.ViewHolder) holder;
                ListItem item = listItems.get(i);

                // Directly update the model from the view's content
                item.content = listHolder.itemContent.getText().toString();
            }
        }
    }

    private void saveNote() {
        // *** NEW STEP: Synchronize data from EditText views before serializing ***
        synchronizeRecyclerViewData();
        NotesWidgetProvider.refreshWidget(getApplicationContext());
        String title = noteTitleEditText.getText().toString().trim();
        String listContent = serializeListItems();

        if (listContent.isEmpty()) {
            if (title.isEmpty()) {
                Toast.makeText(this, "Empty list discarded.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        // If title is empty, use the first non-empty list item as the title
        if (title.isEmpty()) {
            if (!listItems.isEmpty()) {
                for (ListItem item : listItems) {
                    if (!item.content.trim().isEmpty()) {
                        title = item.content.trim();
                        break;
                    }
                }
            }
            if (title.isEmpty()) {
                title = "New Checklist"; // Fallback title
            }
        }

        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // *** CRITICAL CHANGE: Prepend the LIST_NOTE_PREFIX to the content before saving ***
        String finalContent = NotesListActivity.LIST_NOTE_PREFIX + "\n" + listContent;
        noteColor = selectedColor;
        String finalTitle = title;

        Executors.newSingleThreadExecutor().execute(() -> {

            // Note: The 'imagePath' is null as this is a list note
            Note notetosync = new Note(finalTitle, finalContent, receivedDateFromActivities, noteColor, false, "");
            if (this.folderName.length() != 0) {
                notetosync.setFontFamily(this.folderName);
            }
            if (currentNoteUuid.length() == 0) {
                String noteCloudId = UUID.randomUUID().toString();
                notetosync.setUserFirebaseId(noteCloudId);
                if (this.folderName.length() != 0) {
                    notetosync.setFontFamily(this.folderName);
                }
                long id = noteRepository.addNote(notetosync);
                notetosync.setId(id);
                runOnUiThread(() -> {
                    Toast.makeText(this, "List note saved!", Toast.LENGTH_SHORT).show();
                });
            } else {
                // Existing Note
                notetosync.setUserFirebaseId(currentNoteUuid);
                if (this.folderName.length() != 0) {
                    notetosync.setFontFamily(this.folderName);
                }
                noteRepository.updateNote(notetosync);
                runOnUiThread(() -> {
                    Toast.makeText(this, "List note updated!", Toast.LENGTH_SHORT).show();
                });

            }
            uploadAndSyncNoteToFirebase(notetosync);
            finish();
        });
    }
    private void uploadAndSyncNoteToFirebase(Note noteWithLocalPath) {
        noteWithLocalPath.setImagePath("");
        db.collection("users").document(this.currentUser.getUid()).collection("notes").document(String.valueOf(noteWithLocalPath.getUserFirebaseId()))
                    .set(noteWithLocalPath)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Note " + noteWithLocalPath.getId() + " metadata saved to Firestore."))
                    .addOnFailureListener(e -> Log.w(TAG, "Error saving note " + noteWithLocalPath.getId() + " metadata to Firestore.", e));
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the standard action menu (Save, Delete, etc.)
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_notepad_actions, menu);
        // Hide the color palette if you want to enforce a standard look for list notes
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home ) {
            // Toolbar back button or explicit close button
            onBackPressed();
            return true;
        } else if (id == R.id.action_save) {
            saveNote();
            return true;
        } else if (id == R.id.action_delete) {
            // Implement deletion logic (moving note to trash)
            if (noteId != -1) {
                showDeleteConfirmationDialog();
            } else {
                Toast.makeText(this, "Note discarded.", Toast.LENGTH_SHORT).show();
                finish();
            }
            return true;
        }
        else if(id== R.id.action_color){
            showColorPickerDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void showColorPickerDialog() {
        final int[] colors = {
                Color.WHITE,
                Color.parseColor("#FFC0CB"),
                Color.parseColor("#FFFF66"),
                Color.parseColor("#C0C0C0"),
                Color.parseColor("#ADD8E6")
        };

        final String[] colorNames = {getApplicationContext().getString(R.string.white_text),
                getApplicationContext().getString(R.string.pink_text),
                getApplicationContext().getString(R.string.yellow_text),
                getApplicationContext().getString(R.string.silver_text),
                getApplicationContext().getString(R.string.blue_text)};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.background_color_choose);
        builder.setItems(colorNames, (dialog, which) -> {
            selectedColor = colors[which];
            Toast.makeText(getApplicationContext(),"note background color saved",Toast.LENGTH_SHORT).show();
            //listItemLayout.setBackgroundColor(selectedColor);
            isNoteModified = true;
        });
        builder.show();
    }
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_text)
                .setMessage(R.string.dialog_text)
                .setPositiveButton(R.string.delete_text2, (dialog, which) -> {
                    // User confirmed deletion
                    if (noteId != -1) {
                        // Assuming you have access to a background thread mechanism if needed,
                        // but sticking to the synchronous call for simplicity in this context.
                        Executors.newSingleThreadExecutor().execute(() -> {
                            // Delete notes from the main list
                            Note deletedNote = noteRepository.getNoteById(noteId);
                            noteRepository.deleteNote(noteId);
                            notesRepositoryTrash.addNote(deletedNote);
                        });
                        Toast.makeText(this, getApplicationContext().getString(R.string.list_trash_text), Toast.LENGTH_SHORT).show();
                    } else {
                        // This case is handled in onOptionsItemSelected, but is here for robustness
                        Toast.makeText(this, getApplicationContext().getString(R.string.notes_discarded), Toast.LENGTH_SHORT).show();
                    }
                    finish();
                })
                .setNegativeButton(getApplicationContext().getString(R.string.cancel_list), (dialog, which) -> {
                    // User cancelled, dismiss the dialog
                    dialog.dismiss();
                })
                .show();
    }
    @Override
    public void onBackPressed() {
        // Auto-save on back press
        saveNote();
        super.onBackPressed();
    }

    // --- RecyclerView Adapter for List Items ---
    private static class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
        private final Context context;
        private final List<ListItem> items;

        public ListAdapter(Context context, List<ListItem> items) {
            this.context = context;
            this.items = items;
        }

        // Helper function to apply or remove the strikethrough flag
        private void applyStrikethrough(CheckBox checkbox, boolean isChecked) {
            if (isChecked) {
                checkbox.setPaintFlags(checkbox.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                // Optional: Reduce text color opacity to visually "dim" completed items
                // This assumes your CheckBox text color is black or similar.
                checkbox.setTextColor(Color.parseColor("#676565")); // Dark grey/faded black
            } else {
                checkbox.setPaintFlags(checkbox.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                // Restore original text color
                checkbox.setTextColor(Color.parseColor("#ffffff")); // Full black/default
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.list_item_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ListItem item = items.get(position);

            // Set initial values
            // We temporarily remove the listener to prevent recursive calls when setting text/checked state
            // on an existing view that is being recycled.
            holder.itemContent.setOnCheckedChangeListener(null);

            holder.itemContent.setText(item.content);
            holder.itemContent.setChecked(item.isChecked);

            // *** APPLY STRIKETHROUGH ON BIND ***
            applyStrikethrough(holder.itemContent, item.isChecked);


            // Re-apply the listener after the state has been set
            holder.itemContent.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    items.get(currentPosition).isChecked = isChecked;
                    // *** APPLY STRIKETHROUGH ON CHANGE ***
                    applyStrikethrough(holder.itemContent, isChecked);
                }
            });

            // Update model when content is changed
            // We use a focus listener to capture changes after the user is done typing a row.
            holder.itemContent.setOnFocusChangeListener((v, hasFocus) -> {
                int currentPosition = holder.getAdapterPosition();
                if (!hasFocus && currentPosition != RecyclerView.NO_POSITION) {
                    items.get(currentPosition).content = holder.itemContent.getText().toString();
                }
            });

            // Handle delete button
            holder.deleteButton.setOnClickListener(v -> {
                // Use getAdapterPosition() to ensure we get the correct, current index of the item.
                int adapterPosition = holder.getAdapterPosition();

                if (adapterPosition != RecyclerView.NO_POSITION) {
                    items.remove(adapterPosition);
                    // FIX: Only call notifyItemRemoved. This is sufficient and correct for single deletions,
                    // preventing the visual corruption caused by combining it with notifyItemRangeChanged.
                    notifyItemRemoved(adapterPosition);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            final CheckBox itemContent;
            final ImageButton deleteButton;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                itemContent = itemView.findViewById(R.id.itemContent);
                deleteButton = itemView.findViewById(R.id.deleteItemButton);
            }
        }
    }
}
