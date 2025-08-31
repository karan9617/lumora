package com.example.keyboardai;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.keyboardai.Models.Note;
import com.example.keyboardai.adapter.ItemMoveCallback;
import com.example.keyboardai.adapter.NotesAdapter;
import com.example.keyboardai.adapter.NotesAdapterPinned;
import com.example.keyboardai.data.NoteRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class NotesListActivity extends AppCompatActivity {
    private RecyclerView notesRecyclerView, notesRecyclerViewPinned;
  //  RelativeLayout mainLayout;
    private NotesAdapter notesAdapter;
    private NotesAdapterPinned notesAdapterPinned;
    private List<Note> notesList;
    private NoteRepository noteRepository;
    FloatingActionButton fabAddNote;
    private DrawerLayout drawerLayout;
    private SearchView searchView;
    private List<Note> allNotes, pinnedNotes; // To hold the full, unfiltered list of notes

    // Contextual Action Bar variables
    private ActionMode actionMode;
    private Note selectedNote;

    private LinearLayout optionsLayout;
    private boolean isOptionsVisible = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list_main);

        getWindow().setAllowEnterTransitionOverlap(false);
        getWindow().setAllowReturnTransitionOverlap(false);

        // Initialize the repository
        noteRepository = new NoteRepository(this);

        // Initialize the views
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        searchView = findViewById(R.id.search_view);
        fabAddNote = findViewById(R.id.fabAddNote);
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerViewPinned = findViewById(R.id.notesRecyclerViewPinned);

        final Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        final Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down);


        optionsLayout = findViewById(R.id.options_layout);
        TextView optionImage = findViewById(R.id.option_images);
        TextView optionText = findViewById(R.id.option_text);
        TextView optionDrawing = findViewById(R.id.option_drawings);

        // Setup the drawer toggle button
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Setup the search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                filterNotes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterNotes(newText);
                return true;
            }
        });

        // Set up the FAB
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
                        // Do nothing here
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // When the animation is over, hide the layout to free up space
                        optionsLayout.setVisibility(View.GONE);
                        // Clear the animation to prevent it from causing issues later
                        optionsLayout.clearAnimation();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        // Do nothing here
                    }
                });
                showOptions();
            }
            //Intent intent = new Intent(NotesListActivity.this, Notepad.class);
           // startActivity(intent);
        });

        optionImage.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Image Note", Toast.LENGTH_SHORT).show();
            // TODO: Start the activity for adding an image note here
            hideOptions();
        });

        optionText.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Text Note", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(NotesListActivity.this, Notepad.class);
            startActivity(intent);
            hideOptions();
        });

        optionDrawing.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Drawing Note", Toast.LENGTH_SHORT).show();
            // TODO: Start the activity for adding a drawing note here
            hideOptions();
        });
        // Set up the RecyclerView
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        notesRecyclerView.setLayoutManager(layoutManager);

        // Set up the RecyclerView
        StaggeredGridLayoutManager layoutManagerPinned = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        notesRecyclerViewPinned.setLayoutManager(layoutManagerPinned);

        notesList = new ArrayList<>();
        allNotes = new ArrayList<>();
        pinnedNotes = new ArrayList<>();
        // NEW: Instantiate the NotesAdapter correctly and pass the listeners
        notesAdapter = new NotesAdapter(this, notesList, new NotesAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note, View sharedView) {
                if (actionMode != null) {
                    // Deselect the note if CAB is active and a note is clicked
                    actionMode.finish();
                    return;
                }
                Intent intent = new Intent(NotesListActivity.this, Notepad.class);
                intent.putExtra("note_id", note.getId());
                intent.putExtra("note_title", note.getTitle());
                intent.putExtra("note_content", note.getContent());
                intent.putExtra("note_date", note.getDate());
                intent.putExtra("note_color", note.getColor());
                intent.putExtra("drawing_data", note.getDrawingData());

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
            public void onNoteLongClick(Note note, View sharedView) {
                if (actionMode == null) {
                    // Start the contextual action mode
                    actionMode = startSupportActionMode(actionModeCallback);
                }
                selectedNote = note;
            }
        });

        notesRecyclerView.setAdapter(notesAdapter);


        notesAdapterPinned = new NotesAdapterPinned(this, pinnedNotes, new NotesAdapterPinned.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note, View sharedView) {
                if (actionMode != null) {
                    // Deselect the note if CAB is active and a note is clicked
                    actionMode.finish();
                    return;
                }
                Intent intent = new Intent(NotesListActivity.this, Notepad.class);
                intent.putExtra("note_id", note.getId());
                intent.putExtra("note_title", note.getTitle());
                intent.putExtra("note_content", note.getContent());
                intent.putExtra("note_date", note.getDate());
                intent.putExtra("note_color", note.getColor());
                intent.putExtra("drawing_data", note.getDrawingData());

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
            public void onNoteLongClick(Note note, View sharedView) {
                if (actionMode == null) {
                    // Start the contextual action mode
                    actionMode = startSupportActionMode(actionModeCallback);
                }
                selectedNote = note;
            }
        });
        notesRecyclerViewPinned.setAdapter(notesAdapterPinned);
        // FIX: The adapter itself now implements ItemTouchHelperAdapter, so we pass it directly
        ItemTouchHelper.Callback callback = new ItemMoveCallback(notesAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(notesRecyclerView);

        ItemTouchHelper.Callback callbackPinned = new ItemMoveCallback(notesAdapterPinned);
        ItemTouchHelper touchHelperPinned = new ItemTouchHelper(callbackPinned);
        touchHelperPinned.attachToRecyclerView(notesRecyclerViewPinned);

        drawerLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Check if a touch event occurred outside the options menu
                if (isOptionsVisible && event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Check if the touch is outside the bounds of the options layout
                    if (!isTouchInsideView(optionsLayout, event)) {
                        hideOptions(slideDownAnimation);
                        // Return true to consume the touch event
                        return true;
                    }
                }
                // Return false to let the touch event pass through to other views if needed
                return false;
            }
        });
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

    // Override onDestroy to destroy CAB if it's active
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
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        optionsLayout.startAnimation(animation);
        isOptionsVisible = false;
    }
    private void loadNotesFromDatabase() {
        new Thread(() -> {
            List<Note> allNotesFromDb = noteRepository.getAllNotes();
            List<Note> allPinnedNotesFromDb = noteRepository.getAllPinnedNotes();
            runOnUiThread(() -> {
                allNotes.clear();
                allNotes.addAll(allNotesFromDb);

                pinnedNotes.clear();
                pinnedNotes.addAll(allPinnedNotesFromDb);

                notesList.clear();
                notesList.addAll(allNotes);

                notesAdapter.notifyDataSetChanged();
                notesAdapterPinned.notifyDataSetChanged();
            });
        }).start();
    }

    private void filterNotes(String query) {
        if (query == null || query.isEmpty()) {
            notesList.clear();
            notesList.addAll(allNotes);
        } else {
            List<Note> filteredList = new ArrayList<>();
            String lowercaseQuery = query.toLowerCase();
            for (Note note : allNotes) {
                if (note.getTitle().toLowerCase().contains(lowercaseQuery) ||
                        note.getContent().toLowerCase().contains(lowercaseQuery)) {
                    filteredList.add(note);
                }
            }
            notesList.clear();
            notesList.addAll(filteredList);
        }
        notesAdapter.notifyDataSetChanged();
    }

    // Contextual Action Bar Callback
    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_contextual_action_bar, menu);
            mode.setTitle("Note Options");
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // This is called every time the menu is invalidated.
            // You can use this to show/hide menu items based on the selected note.
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
                // Handle share action
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, selectedNote.getContent());
                startActivity(Intent.createChooser(shareIntent, "Share note via"));
                mode.finish();
                return true;
            } else if (id == R.id.action_pin) {
                // Handle pin/unpin action
                boolean isPinned = selectedNote.isPinned();
                notesAdapter.onPinUnpinNote(selectedNote, !isPinned);
                mode.finish();
                return true;
            } else if (id == R.id.action_color) {
                // Handle color change action
                Toast.makeText(NotesListActivity.this, "Change Color", Toast.LENGTH_SHORT).show();
                mode.finish();
                return true;
            } else if (id == R.id.action_delete) {
                // Handle delete action
                new Thread(() -> {
                    noteRepository.deleteNote(selectedNote.getId());
                    final int position = notesList.indexOf(selectedNote);
                    if (position != -1) {
                        runOnUiThread(() -> {
                            notesList.remove(position);
                            allNotes.remove(selectedNote); // Keep the main list in sync
                            notesAdapter.notifyItemRemoved(position);
                            Toast.makeText(NotesListActivity.this, "Note Deleted", Toast.LENGTH_SHORT).show();
                            mode.finish();
                        });
                    }
                }).start();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            selectedNote = null;
            // The adapter will handle deselecting the note
            notesAdapter.notifyDataSetChanged();

            notesAdapterPinned.notifyDataSetChanged();
        }
    };
}
