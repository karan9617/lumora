package com.example.keyboardai;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.keyboardai.Models.Note;
import com.example.keyboardai.adapter.NotesAdapter;
import com.example.keyboardai.adapter.NotesAdapterPinned;
import com.example.keyboardai.data.NoteRepository;
import com.example.keyboardai.operationactivity.Feedback;
import com.example.keyboardai.operationactivity.InstructionsActivity;
import com.example.keyboardai.operationactivity.PoliciesActivity;
import com.example.keyboardai.operationactivity.TrashActivity;
import com.example.keyboardai.operationactivity.trashfiles.NotesRepositoryTrash;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class NotesListActivity extends AppCompatActivity {
    private RecyclerView notesRecyclerView, notesRecyclerViewPinned;
    private NotesAdapter notesAdapter;
    private NotesAdapterPinned notesAdapterPinned;
    View transparentOverlay;
    private List<Note> notesList;
    List<Note> allNotesFromDb, allPinnedNotesFromDb;
    public static List<Note> trashList = new ArrayList<>();
    Toolbar toolbar;
    private NoteRepository noteRepository;
    TextView initialtext;
    FloatingActionButton fabAddNote;
    LinearLayout option_text_layout, option_drawings_layout;
    private DrawerLayout drawerLayout;
    ItemTouchHelper itemTouchHelper,itemTouchHelperPinned;
    private SearchView searchView;
    static public List<Note> allNotes, pinnedNotes;
    private ActionMode actionMode;
    private Note selectedNote;
    TextView pinnedNotesHeader;

    private LinearLayout optionsLayout;
    NotesRepositoryTrash notesRepositoryTrash;
    private boolean isOptionsVisible = false;
    ImageButton shuffle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list_main);

        getWindow().setAllowEnterTransitionOverlap(false);
        getWindow().setAllowReturnTransitionOverlap(false);
        transparentOverlay = findViewById(R.id.transparent_overlay);
        transparentOverlay.setOnClickListener(v -> {
            if (isOptionsVisible) {
                hideOptions();
            }
        });
        allNotesFromDb = new ArrayList<>();
        allPinnedNotesFromDb = new ArrayList<>();
        noteRepository = new NoteRepository(this);
        notesRepositoryTrash = new NotesRepositoryTrash(this);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initialtext = findViewById(R.id.initialtext);
        option_drawings_layout = findViewById(R.id.option_drawings_layout);
        option_text_layout = findViewById(R.id.option_text_layout);
        shuffle = findViewById(R.id.shuffle);
        pinnedNotesHeader = findViewById(R.id.pinnedNotesHeader);
        searchView = findViewById(R.id.search_view);
        fabAddNote = findViewById(R.id.fabAddNote);
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerViewPinned = findViewById(R.id.notesRecyclerViewPinned);

        final Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        final Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down);

        optionsLayout = findViewById(R.id.options_layout);
        TextView optionText = findViewById(R.id.option_text);
        TextView optionDrawing = findViewById(R.id.option_drawings);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_instructions) {
                startActivity(new Intent(this, InstructionsActivity.class));
            } else if (id == R.id.nav_trash) {
                startActivity(new Intent(this, TrashActivity.class));
            } else if (id == R.id.nav_policies) {
                startActivity(new Intent(this, PoliciesActivity.class));
            } else if (id == R.id.nav_feedback) {
                startActivity(new Intent(this, Feedback.class));
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

        optionText.setOnClickListener(v -> {
            Intent intent = new Intent(NotesListActivity.this, Notepad.class);
            startActivity(intent);
            hideOptions();
        });

        optionDrawing.setOnClickListener(v -> {
            Intent intent = new Intent(NotesListActivity.this, DrawingActivity.class);
            startActivity(intent);
            hideOptions();
        });
        option_drawings_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesListActivity.this, DrawingActivity.class);
                startActivity(intent);
                hideOptions();
            }
        });
        option_text_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesListActivity.this, Notepad.class);
                startActivity(intent);
                hideOptions();
            }
        });

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        notesRecyclerView.setLayoutManager(layoutManager);

        StaggeredGridLayoutManager layoutManagerPinned = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        notesRecyclerViewPinned.setLayoutManager(layoutManagerPinned);

        notesList = new ArrayList<>();
        allNotes = new ArrayList<>();
        pinnedNotes = new ArrayList<>();

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
                if (note.getContent() != null && !note.getContent().isEmpty()) {
                    intent = new Intent(NotesListActivity.this, Notepad.class);
                } else if (note.getImagePath() != null && note.getImagePath().length() > 0) {
                    intent = new Intent(NotesListActivity.this, DrawingActivity.class);
                } else {
                    intent = new Intent(NotesListActivity.this, Notepad.class);
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
                            NotesListActivity.this,
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

        ItemTouchHelper.Callback callbackPinned = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                return makeMovementFlags(dragFlags, 0);
            }
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                notesAdapterPinned.onItemMove(fromPosition, toPosition);
                return true;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            }
            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
                notesAdapterPinned.onItemsMoved();
            }
            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    notesAdapterPinned.onItemsMoved();
                } else if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    if (actionMode == null) {
                        actionMode = startSupportActionMode(actionModeCallback);
                    }
                    int position = viewHolder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        selectedNote = pinnedNotes.get(position);
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

        itemTouchHelperPinned = new ItemTouchHelper(callbackPinned);
        itemTouchHelperPinned.attachToRecyclerView(notesRecyclerViewPinned);

        notesAdapterPinned = new NotesAdapterPinned(this, pinnedNotes, new NotesAdapterPinned.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note, View sharedView) {
                if (actionMode != null) {
                    actionMode.finish();
                    return;
                }
                Intent intent;
                if (note.getContent() != null && !note.getContent().isEmpty()) {
                    intent = new Intent(NotesListActivity.this, Notepad.class);
                } else if (note.getImagePath() != null && note.getImagePath().length() > 0) {
                    intent = new Intent(NotesListActivity.this, DrawingActivity.class);
                } else {
                    intent = new Intent(NotesListActivity.this, Notepad.class);
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
                            NotesListActivity.this,
                            sharedView,
                            transitionName
                    );
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            }
        }, new NotesAdapterPinned.OnNoteLongClickListener() {
            @Override
            public void onNoteLongClick(View view, Note note, View sharedView) {
                if (actionMode == null) {
                    selectedNote = note;
                    actionMode = startSupportActionMode(actionModeCallback);
                }
            }
        }, itemTouchHelperPinned,notesRecyclerViewPinned);
        notesRecyclerViewPinned.setAdapter(notesAdapterPinned);
        updatePinnedSectionVisibility();

        drawerLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isOptionsVisible && event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!isTouchInsideView(optionsLayout, event)) {
                        hideOptions(slideDownAnimation);
                        return true;
                    }
                }
                return false;
            }
        });

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
        notesAdapterPinned.notifyDataSetChanged();
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

        // Sort the pinned notes
        Collections.sort(pinnedNotes, (note1, note2) -> {
            String title1 = note1.getTitle() != null ? note1.getTitle() : "";
            String title2 = note2.getTitle() != null ? note2.getTitle() : "";
            return title1.compareToIgnoreCase(title2);
        });
        notesAdapterPinned.notifyDataSetChanged();
        Toast.makeText(this, R.string.notes_sorted_alphabetically, Toast.LENGTH_SHORT).show();
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
        loadNotesFromDatabase();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();  // closes all activities in the task
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (actionMode != null) {
            actionMode.finish();
        }
    }
    private void showOptions() {
        optionsLayout.setVisibility(View.VISIBLE);
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(250);
        optionsLayout.startAnimation(fadeIn);
        transparentOverlay.setVisibility(View.VISIBLE);
        isOptionsVisible = true;
    }
    private void hideOptions() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fab_options_slide_down);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                optionsLayout.setVisibility(View.GONE);
                transparentOverlay.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        optionsLayout.startAnimation(animation);
        isOptionsVisible = false;
    }
    private void loadNotesFromDatabase() {
        new Thread(() -> {
            allNotesFromDb = noteRepository.getAllNotes();
            allPinnedNotesFromDb = noteRepository.getAllPinnedNotes();
            runOnUiThread(() -> {
                allNotes.clear();
                allNotes.addAll(allNotesFromDb);

                pinnedNotes.clear();
                pinnedNotes.addAll(allPinnedNotesFromDb);

                notesList.clear();
                notesList.addAll(allNotes);

                notesAdapter.notifyDataSetChanged();
                notesAdapterPinned.notifyDataSetChanged();
                updatePinnedSectionVisibility();
            });
        }).start();

    }

    private void filterNotes(String query) {
        List<Note> masterUnpinned = (allNotesFromDb != null) ? allNotesFromDb : new ArrayList<>();
        List<Note> masterPinned   = (allPinnedNotesFromDb != null) ? allPinnedNotesFromDb : new ArrayList<>();

        // Create new lists to hold the filtered results.
        List<Note> filteredNotesList = new ArrayList<>();
        List<Note> filteredPinnedNotesList = new ArrayList<>();

        if (query == null || query.isEmpty()) {
            // If the query is empty, add all notes back from the master lists.
            filteredNotesList.addAll(masterUnpinned);
            filteredPinnedNotesList.addAll(masterPinned);
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

            // Filter pinned notes from the master list.
            for (Note note : allPinnedNotesFromDb) {
                boolean titleMatches = note.getTitle() != null && note.getTitle().toLowerCase().contains(lowercaseQuery);
                boolean contentMatches = note.getContent() != null && note.getContent().toLowerCase().contains(lowercaseQuery);

                if (titleMatches || contentMatches) {
                    filteredPinnedNotesList.add(note);
                }
            }
        }

        // Update the adapters with the filtered lists.
        notesList.clear();
        notesList.addAll(filteredNotesList);
        notesAdapter.notifyDataSetChanged();

        pinnedNotes.clear();
        pinnedNotes.addAll(filteredPinnedNotesList);
        notesAdapterPinned.notifyDataSetChanged();

        updatePinnedSectionVisibility();
    }

    public void updatePinnedSectionVisibility(){
        if(pinnedNotes.isEmpty()){
            pinnedNotesHeader.setVisibility(View.GONE);
        }
        else{
            pinnedNotesHeader.setVisibility(View.VISIBLE);
        }
        if(allNotes.size() == 0 && pinnedNotes.size() == 0){
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
            inflater.inflate(R.menu.menu_contextual_action_bar, menu);
            searchView.setVisibility(View.GONE);

            toolbar.setVisibility(View.GONE);
            drawerLayout.setBackgroundColor(Color.argb(43,135,73,251));
            fabAddNote.setVisibility(View.GONE);
            optionsLayout.setVisibility(View.GONE);

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
                                NotesListActivity.this,
                                getApplicationContext().getPackageName() + ".fileprovider",
                                newImageFile
                        );

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("image/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

                        // Add optional text
                        String title = selectedNote.getTitle() != null ? selectedNote.getTitle().split(";")[0] : "";
                        String shareText = "Title: " + title + "\n\n" +
                                "Description: " + (selectedNote.getContent() != null ? selectedNote.getContent() : "");
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
                    String shareText = title + "\n\n" + content;

                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

                    startActivity(Intent.createChooser(shareIntent, "Share text note via"));
                }

                mode.finish();
                return true;
            }  else if (id == R.id.action_pin) {
                final List<Note> selectedNotesToPin = notesAdapter.getSelectedNotes();
                final List<Note> selectedPinnedNotesToUnpin = notesAdapterPinned.getSelectedNotes();

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
            }
            else if (id == R.id.action_delete_note) {
                final List<Note> selectedNotes = notesAdapter.getSelectedNotes();
                final List<Note> selectedPinnedNotes = notesAdapterPinned.getSelectedNotes();

                if (selectedNotes.isEmpty() && selectedPinnedNotes.isEmpty()) {
                    mode.finish();
                    return true;
                }

                Executors.newSingleThreadExecutor().execute(() -> {
                    // Delete notes from the main list
                    for (Note note : selectedNotes) {
                        notesRepositoryTrash.addNote(note);
                        noteRepository.deleteNote(note.getId());
                    }
                    // Delete notes from the pinned list
                    for (Note note : selectedPinnedNotes) {
                        notesRepositoryTrash.addNote(note);
                        noteRepository.deleteNote(note.getId());
                    }

                    runOnUiThread(() -> {
                        // Reload data to reflect changes
                        loadNotesFromDatabase();
                        Toast.makeText(NotesListActivity.this, "Notes Deleted", Toast.LENGTH_SHORT).show();
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
            fabAddNote.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.VISIBLE);
            drawerLayout.setBackgroundColor(Color.argb(71,0,0,0));

            notesAdapter.clearSelections();
            notesAdapterPinned.clearSelections();
        }
    };
}
