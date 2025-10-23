package com.noteaiapp.keyboardai.operationactivity;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.noteaiapp.keyboardai.DrawingActivity;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.Notepad;
import com.noteaiapp.keyboardai.NotesListActivity;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.adapter.NotesAdapter;
import com.noteaiapp.keyboardai.calendar.CalendarActivity;
import com.noteaiapp.keyboardai.data.NoteRepository;
import com.noteaiapp.keyboardai.imagenote.ImageNoteActivity;
import com.noteaiapp.keyboardai.listitems.ListItemsActivity;
import com.noteaiapp.keyboardai.operationactivity.trashfiles.NotesRepositoryTrash;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ArchivesActivity extends AppCompatActivity {
    private RecyclerView notesRecyclerView;/// notesRecyclerViewPinned;
    private NotesAdapter notesAdapter;
    private List<Note> notesList;
    List<Note> allNotesFromDb;// allPinnedNotesFromDb;
    Toolbar toolbar;
    private NoteRepository noteRepository;
    TextView initialtext;
    private DrawerLayout drawerLayout;
    ItemTouchHelper itemTouchHelper;//itemTouchHelperPinned;

    private SearchView searchView;

    static public List<Note> allNotes;// pinnedNotes;
    private ActionMode actionMode;
    private Note selectedNote;
    //TextView pinnedNotesHeader;
    public static final String LIST_NOTE_PREFIX = "[LIST_NOTE_START]";
    NotesRepositoryTrash notesRepositoryTrash;
    ImageButton shuffle;
    NavigationView navigationView;
    private enum DriveAction { BACKUP, RESTORE }

    private BroadcastReceiver noteUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Call the refresh logic here
            loadNotesFromDatabase();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_archives);

        getWindow().setAllowEnterTransitionOverlap(false);
        getWindow().setAllowReturnTransitionOverlap(false);
        // Add this inside your onCreate method in NotesListActivity.java

        allNotesFromDb = new ArrayList<>();
        //allPinnedNotesFromDb = new ArrayList<>();
        noteRepository = new NoteRepository(this);
        notesRepositoryTrash = new NotesRepositoryTrash(this);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initialtext = findViewById(R.id.initialtext);
        shuffle = findViewById(R.id.shuffle);
        //pinnedNotesHeader = findViewById(R.id.pinnedNotesHeader);
        searchView = findViewById(R.id.search_view);
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        //notesRecyclerViewPinned = findViewById(R.id.notesRecyclerViewPinned);
        //notesRecyclerViewPinned.setLayoutManager(new LinearLayoutManager(this));
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadAndApplyBackgroundColor();
        final Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        final Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);


        toggle.syncState();
        //loadFoldersToDrawer();
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
/*
            if (id == R.id.new_folder) {
                // 1. Handle the "New folder" action
                showNewFolderDialog();
            }
            else if (item.getGroupId() == R.id.folders_group) {
                // Logic to filter your notes list by this folder's name/ID
                String folderName = item.getTitle().toString();

                Intent intent = new Intent(this, FolderNotesActivity.class);
                intent.putExtra(EXTRA_FOLDER_NAME, folderName);
                startActivity(intent);

                Toast.makeText(this, "Loading notes from folder: " + folderName, Toast.LENGTH_SHORT).show();
                // TODO: Implement actual data filtering logic here
            }else */
            if (id == R.id.nav_instructions) {
                startActivity(new Intent(this, InstructionsActivity.class));
            } else if (id == R.id.nav_trash) {
                startActivity(new Intent(this, TrashActivity.class));
            } else if (id == R.id.nav_policies) {
                startActivity(new Intent(this, PoliciesActivity.class));
            } else if (id == R.id.nav_feedback) {
                startActivity(new Intent(this, Feedback.class));
            }
            else if(id == R.id.calendar_option){
                Intent intent = new Intent(ArchivesActivity.this, CalendarActivity.class);
                startActivity(intent);
            }
            else if (id == R.id.nav_archive) {
                return true;
            }
            drawerLayout.closeDrawers();
            return true;
        });
        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortDialog();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // This is still here for when the user hits enter on the keyboard
                filterNotes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Call filterNotes here to enable real-time searching
                filterNotes(newText);
                return true;
            }
        });

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        notesRecyclerView.setLayoutManager(layoutManager);

        //StaggeredGridLayoutManager layoutManagerPinned = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        // notesRecyclerViewPinned.setLayoutManager(layoutManagerPinned);

        notesList = new ArrayList<>();
        allNotes = new ArrayList<>();
        // pinnedNotes = new ArrayList<>();

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                return makeMovementFlags(dragFlags, 0);
            }
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                notesAdapter.onItemMove(fromPosition, toPosition);
                return true;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            }
            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
                notesAdapter.onItemsMoved();
            }
            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    notesAdapter.onItemsMoved();

                } else if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    if (actionMode == null) {
                        actionMode = startSupportActionMode(actionModeCallback);
                    }
                    int position = viewHolder.getAdapterPosition();

                    if (position != RecyclerView.NO_POSITION) {
                        selectedNote = notesList.get(position);
                        //selectedNote.setSelected(true);
                    }
                }


            }

            @Override
            public boolean isLongPressDragEnabled() {
                // Enable long press drag
                return false;
            }

        };

        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(notesRecyclerView);

        notesAdapter = new NotesAdapter(this, notesList, new NotesAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note, View sharedView) {
                if (actionMode != null) {
                    actionMode.finish();
                    return;
                }
                Intent intent;
                String noteContent = note.getContent();
                boolean isListNote = noteContent != null && noteContent.startsWith(LIST_NOTE_PREFIX);
                if(isListNote){
                    intent = new Intent(ArchivesActivity.this, ListItemsActivity.class);
                }
                else if(note.getFontColor() != null && !note.getFontColor().isEmpty() && note.getFontColor().equalsIgnoreCase("imagenote")){
                    intent = new Intent(ArchivesActivity.this, ImageNoteActivity.class);
                }
                else if (note.getContent() != null && !note.getContent().isEmpty()) {
                    intent = new Intent(ArchivesActivity.this, Notepad.class);
                } else if (note.getImagePath() != null && note.getImagePath().length() > 0) {
                    intent = new Intent(ArchivesActivity.this, DrawingActivity.class);
                } else {
                    intent = new Intent(ArchivesActivity.this, Notepad.class);
                }
                intent.putExtra("note_id", note.getId());
                intent.putExtra("note_title", note.getTitle());
                intent.putExtra("note_content", note.getContent());
                intent.putExtra("note_date", note.getDate());
                intent.putExtra("note_color", note.getColor());
                intent.putExtra("note_image_path",note.getImagePath());

                String transitionName = ViewCompat.getTransitionName(sharedView);
                if (transitionName != null) {
                    intent.putExtra("TRANSITION_NAME", transitionName);
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                            ArchivesActivity.this,
                            sharedView,
                            transitionName
                    );
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }

            }
        }, new NotesAdapter.OnNoteLongClickListener() {
            @Override
            public void onNoteLongClick(View view, Note note, View sharedView) {
                if (actionMode == null) {
                    selectedNote = note;
                    actionMode = startSupportActionMode(actionModeCallback);
                }
            }
        }, itemTouchHelper,notesRecyclerView);
        notesRecyclerView.setAdapter(notesAdapter);
        updatePinnedSectionVisibility();

    }
    // --- Background Color Persistence Variables ---
    private static final String PREFS_NAME = "NotesPrefs";
    private static final String BACKGROUND_COLOR_KEY = "BackgroundColor";
    private static final int DEFAULT_BACKGROUND_COLOR = Color.BLACK;
    private void saveAndApplyBackgroundColor(int color) {
        // 1. Save color
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(BACKGROUND_COLOR_KEY, color)
                .apply();

        // 2. Apply color
        if (drawerLayout != null) {
            drawerLayout.setBackgroundColor(color);
        }
    }
    /**
     * Loads the saved background color from SharedPreferences and applies it to the drawerLayout.
     */
    private void loadAndApplyBackgroundColor() {
        int color = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(BACKGROUND_COLOR_KEY, DEFAULT_BACKGROUND_COLOR);
        if (drawerLayout != null) {
            drawerLayout.setBackgroundColor(color);
        }
    }
    private void showSortDialog() {
        final String[] options = {getApplicationContext().getString(R.string.sort_by_date), getApplicationContext().getString(R.string.sort_by_alpha)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.sort_notes_by);
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Sort by Date
                    sortNotesByDate();
                    break;
                case 1: // Sort Alphabetically
                    sortNotesAlphabetically();
                    break;
            }
        });
        builder.show();
    }
    /**
     * Sorts the notes in both lists (pinned and unpinned) by date in descending order.
     */
    private void sortNotesByDate() {
        // Sort the unpinned notes
        Collections.sort(notesList, (note1, note2) -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            try {
                Date date1 = dateFormat.parse(note1.getDate());
                Date date2 = dateFormat.parse(note2.getDate());
                // Sort in descending order (newest first)
                return date2.compareTo(date1);
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        });
        notesAdapter.notifyDataSetChanged();
/*
        // Sort the pinned notes
        Collections.sort(pinnedNotes, (note1, note2) -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            try {
                Date date1 = dateFormat.parse(note1.getDate());
                Date date2 = dateFormat.parse(note2.getDate());
                // Sort in descending order (newest first)
                return date2.compareTo(date1);
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        });
        notesAdapterPinned.notifyDataSetChanged();*/
        Toast.makeText(this, R.string.notes_sorted_date, Toast.LENGTH_SHORT).show();
    }

    /**
     * Sorts the notes in both lists (pinned and unpinned) alphabetically by title.
     */
    private void sortNotesAlphabetically() {
        // Sort the unpinned notes
        Collections.sort(notesList, (note1, note2) -> {
            String title1 = note1.getTitle() != null ? note1.getTitle() : "";
            String title2 = note2.getTitle() != null ? note2.getTitle() : "";
            return title1.compareToIgnoreCase(title2);
        });
        notesAdapter.notifyDataSetChanged();
        /*
        // Sort the pinned notes
        Collections.sort(pinnedNotes, (note1, note2) -> {
            String title1 = note1.getTitle() != null ? note1.getTitle() : "";
            String title2 = note2.getTitle() != null ? note2.getTitle() : "";
            return title1.compareToIgnoreCase(title2);
        });
        notesAdapterPinned.notifyDataSetChanged();*/
        Toast.makeText(this, R.string.notes_sorted_alphabetically, Toast.LENGTH_SHORT).show();
    }

    private boolean isTouchInsideView(View view, MotionEvent event) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        return !(event.getRawX() < x || event.getRawX() > x + view.getWidth() ||
                event.getRawY() < y || event.getRawY() > y + view.getHeight());
    }
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(noteUpdateReceiver,
                new IntentFilter("com.noteaiapp.ACTION_NOTE_UPDATED"));
        loadNotesFromDatabase();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (actionMode != null) {
            actionMode.finish();
        }
    }
    @Override
    public void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(noteUpdateReceiver);
    }

    private void loadNotesFromDatabase() {
        new Thread(() -> {
            allNotesFromDb.clear();
            List<Note> allNotesFromDb1 = noteRepository.getAllNotes();
            //allPinnedNotesFromDb = noteRepository.getAllPinnedNotes();
            for(Note note: allNotesFromDb1){
                if(note.getFontFamily() != null && note.getFontFamily().length() > 0 && note.getFontFamily().equalsIgnoreCase("archived")){
                    allNotesFromDb.add(note);
                }
            }
            List<Note> allPinnedArchivedNotes = noteRepository.getAllPinnedNotes();
            for(Note currentNote: allPinnedArchivedNotes){
                if(currentNote.getFontFamily() != null && currentNote.getFontFamily().length() > 0 && currentNote.getFontFamily().equalsIgnoreCase("archived")){
                    allNotesFromDb.add(currentNote);
                }
            }
            //allNotesFromDb.addAll(noteRepository.getAllPinnedNotes());
            runOnUiThread(() -> {
                allNotes.clear();
                allNotes.addAll(allNotesFromDb);

                // pinnedNotes.clear();
                // pinnedNotes.addAll(allPinnedNotesFromDb);

                notesList.clear();
                notesList.addAll(allNotes);

                notesAdapter.notifyDataSetChanged();
                // notesAdapterPinned.notifyDataSetChanged();
                updatePinnedSectionVisibility();
            });
        }).start();

    }

    private void filterNotes(String query) {
        List<Note> masterUnpinned = (allNotesFromDb != null) ? allNotesFromDb : new ArrayList<>();
        //  List<Note> masterPinned   = (allPinnedNotesFromDb != null) ? allPinnedNotesFromDb : new ArrayList<>();

        // Create new lists to hold the filtered results.
        List<Note> filteredNotesList = new ArrayList<>();

        if (query == null || query.isEmpty()) {
            // If the query is empty, add all notes back from the master lists.
            filteredNotesList.addAll(masterUnpinned);
        } else {
            String lowercaseQuery = query.toLowerCase();

            // Filter un-pinned notes from the master list.
            for (Note note : allNotesFromDb) {
                boolean titleMatches = note.getTitle() != null && note.getTitle().toLowerCase().contains(lowercaseQuery);
                boolean contentMatches = note.getContent() != null && note.getContent().toLowerCase().contains(lowercaseQuery);

                if (titleMatches || contentMatches) {
                    filteredNotesList.add(note);
                }
            }

            /* Filter pinned notes from the master list.
            for (Note note : allPinnedNotesFromDb) {
                boolean titleMatches = note.getTitle() != null && note.getTitle().toLowerCase().contains(lowercaseQuery);
                boolean contentMatches = note.getContent() != null && note.getContent().toLowerCase().contains(lowercaseQuery);

                if (titleMatches || contentMatches) {
                    filteredPinnedNotesList.add(note);
                }
            }*/
        }

        // Update the adapters with the filtered lists.
        notesList.clear();
        notesList.addAll(filteredNotesList);
        notesAdapter.notifyDataSetChanged();

        // pinnedNotes.clear();
        // pinnedNotes.addAll(filteredPinnedNotesList);
        // notesAdapterPinned.notifyDataSetChanged();

        updatePinnedSectionVisibility();
    }

    public void updatePinnedSectionVisibility(){
       /* if(pinnedNotes.isEmpty()){
            pinnedNotesHeader.setVisibility(View.GONE);
        }
        else{
            pinnedNotesHeader.setVisibility(View.VISIBLE);
        }*/
        if(allNotes.size() == 0){
            initialtext.setVisibility(View.VISIBLE);
        }
        else{
            initialtext.setVisibility(View.GONE);
        }

    }
    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_contextual_archive_action_bar, menu);
            searchView.setVisibility(View.GONE);
            toolbar.setVisibility(View.GONE);
            drawerLayout.setBackgroundColor(Color.argb(43,135,73,251));
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (selectedNote == null) {
                mode.finish();
                return true;
            }

            int id = item.getItemId();

            if (id == R.id.action_share) {

                // 1. Check if it's an image/drawing note
                if (selectedNote.getImagePath() != null && !selectedNote.getImagePath().isEmpty()) {
                    try {
                        File imageFile = new File(selectedNote.getImagePath());

                        if (!imageFile.exists()) {
                            mode.finish();
                            return true;
                        }

                        // ---- Convert transparent image to white background ----
                        Bitmap originalBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                        if (originalBitmap == null) {
                            mode.finish();
                            return true;
                        }

                        Bitmap newBitmap = Bitmap.createBitmap(
                                originalBitmap.getWidth(),
                                originalBitmap.getHeight(),
                                Bitmap.Config.ARGB_8888
                        );

                        Canvas canvas = new Canvas(newBitmap);
                        canvas.drawColor(Color.WHITE); // white background
                        canvas.drawBitmap(originalBitmap, 0, 0, null);

                        File rootDir = getApplicationContext().getFilesDir();
                        // Save the processed bitmap into cache directory
                        File cachePath = new File(rootDir, "drawing_notes");
                        if (!cachePath.exists()) cachePath.mkdirs();
                        File newImageFile = new File(cachePath, "shared_image.png");

                        FileOutputStream fos = new FileOutputStream(newImageFile);
                        newBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.close();


                        //File directory = context.getDir("images", Context.MODE_PRIVATE);

                        // Get content URI using FileProvider
                        Uri contentUri = FileProvider.getUriForFile(
                                ArchivesActivity.this,
                                getApplicationContext().getPackageName() + ".fileprovider",
                                newImageFile
                        );

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("image/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

                        // Add optional text
                        String title = selectedNote.getTitle() != null ? selectedNote.getTitle().split(";")[0] : "";
                        String content = (selectedNote.getContent() != null)? selectedNote.getContent():"";
                        String cleanContent = "";
                        if(content.length() > 0) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                cleanContent = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString();
                            } else {
                                cleanContent = Html.fromHtml(content).toString();
                            }
                        }
                        String shareText = "Title: " + title + "\n\n" +
                                "Description: " + (cleanContent);
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        startActivity(Intent.createChooser(shareIntent, "Share image note via"));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    // 2. This is a text note - share as text
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");

                    String title = selectedNote.getTitle() != null ? selectedNote.getTitle().split(";")[0] : "";
                    String content = selectedNote.getContent() != null ? selectedNote.getContent() : "";
                    String cleanContent;

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        cleanContent = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString();
                    } else {
                        cleanContent = Html.fromHtml(content).toString();
                    }

// 3. Construct the final share text using the clean content
                    String shareText = title + "\n\n" + cleanContent;

                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

                    startActivity(Intent.createChooser(shareIntent, "Share text note via"));
                }

                mode.finish();
                return true;
            }  /*else if (id == R.id.action_pin) {
                final List<Note> selectedNotesToPin = notesAdapter.getSelectedNotes();
                //final List<Note> selectedPinnedNotesToUnpin = notesAdapterPinned.getSelectedNotes();

                // Determine if we are pinning or unpinning.
                boolean isPinning = !selectedNotesToPin.isEmpty();

                Executors.newSingleThreadExecutor().execute(() -> {
                    if (isPinning) {
                        noteRepository.updateNotePinStatusBulk(selectedNotesToPin, true);
                    } else {
                        noteRepository.updateNotePinStatusBulk(selectedPinnedNotesToUnpin, false);
                    }

                    runOnUiThread(() -> {
                        loadNotesFromDatabase();
                        Toast.makeText(NotesListActivity.this, isPinning ? "Notes pinned" : "Notes unpinned", Toast.LENGTH_SHORT).show();
                        mode.finish();
                    });
                });
                return true;
            }*/
            else if (id == R.id.action_delete_note) {
                final List<Note> selectedNotes = notesAdapter.getSelectedNotes();
                // final List<Note> selectedPinnedNotes = notesAdapterPinned.getSelectedNotes();

                if (selectedNotes.isEmpty()) {
                    mode.finish();
                    return true;
                }

                Executors.newSingleThreadExecutor().execute(() -> {
                    // Delete notes from the main list
                    for (Note note : selectedNotes) {
                        notesRepositoryTrash.addNote(note);
                        noteRepository.deleteNote(note.getId());
                    }
                    /* Delete notes from the pinned list
                    for (Note note : selectedPinnedNotes) {
                        notesRepositoryTrash.addNote(note);
                        noteRepository.deleteNote(note.getId());
                    }*/

                    runOnUiThread(() -> {
                        // Reload data to reflect changes
                        loadNotesFromDatabase();
                        Toast.makeText(ArchivesActivity.this, "Notes Deleted", Toast.LENGTH_SHORT).show();
                        mode.finish();
                    });
                });
                return true;
            } else if(id == R.id.action_archive){
                final List<Note> selectedNotes = notesAdapter.getSelectedNotes();
                // final List<Note> selectedPinnedNotes = notesAdapterPinned.getSelectedNotes();
                if (selectedNotes.isEmpty()) {
                    mode.finish();
                    return true;
                }
                Executors.newSingleThreadExecutor().execute(() -> {
                    // Delete notes from the main list
                    for (Note note : selectedNotes) {
                        note.setFontFamily("");
                        noteRepository.updateNote(note);
                    }
                    runOnUiThread(() -> {
                        // Reload data to reflect changes
                        loadNotesFromDatabase();
                        Toast.makeText(ArchivesActivity.this, "Notes unarchived", Toast.LENGTH_SHORT).show();
                        mode.finish();
                    });
                });
                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            selectedNote = null;
            searchView.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.VISIBLE);
            drawerLayout.setBackgroundColor(Color.argb(71,0,0,0));
            notesAdapter.clearSelections();
        }
    };

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

    /**
     * Starts the ImageNoteActivity with the path of the saved image.
     * @param imagePath The path to the image in our app's storage.
     */
    private void launchImageNoteActivity(String imagePath) {
        Intent intent = new Intent(ArchivesActivity.this, ImageNoteActivity.class);
        // We pass the image path so the activity knows which image to load.
        // The note doesn't exist yet, so we don't pass a note_id.
        intent.putExtra("image_path", imagePath);
        startActivity(intent);
    }
}
