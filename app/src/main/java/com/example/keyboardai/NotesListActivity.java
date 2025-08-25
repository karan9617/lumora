package com.example.keyboardai;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.keyboardai.Models.Note;
import com.example.keyboardai.adapter.ItemMoveCallback;
import com.example.keyboardai.adapter.NotesAdapter;
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
    private RecyclerView notesRecyclerView;
    private NotesAdapter notesAdapter;
    private List<Note> notesList;
    private NoteRepository noteRepository;
    FloatingActionButton fabAddNote;
    private DrawerLayout drawerLayout;
    private SearchView searchView;
    private List<Note> allNotes; // To hold the full, unfiltered list of notes

    // Contextual Action Bar variables
    private ActionMode actionMode;
    private Note selectedNote;

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
            Intent intent = new Intent(NotesListActivity.this, Notepad.class);
            startActivity(intent);
        });

        // Set up the RecyclerView
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        notesRecyclerView.setLayoutManager(layoutManager);

        notesList = new ArrayList<>();
        allNotes = new ArrayList<>();

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

        // FIX: The adapter itself now implements ItemTouchHelperAdapter, so we pass it directly
        ItemTouchHelper.Callback callback = new ItemMoveCallback(notesAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(notesRecyclerView);
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

    private void loadNotesFromDatabase() {
        new Thread(() -> {
            List<Note> allNotesFromDb = noteRepository.getAllNotes();

            runOnUiThread(() -> {
                allNotes.clear();
                allNotes.addAll(allNotesFromDb);
                notesList.clear();
                notesList.addAll(allNotes);
                notesAdapter.notifyDataSetChanged();
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
                // You would need to add a "pinned" column to your database and update it here
                Toast.makeText(NotesListActivity.this, "Note Pinned", Toast.LENGTH_SHORT).show();
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
        }
    };
}
