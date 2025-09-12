package com.example.keyboardai.operationactivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.keyboardai.Models.Note;
import com.example.keyboardai.R;
import com.example.keyboardai.adapter.TrashAdapter;
import com.example.keyboardai.NotesListActivity;
import com.example.keyboardai.operationactivity.trashfiles.NotesRepositoryTrash;

import java.util.ArrayList;
import java.util.List;
import com.example.keyboardai.data.NoteRepository;

public class TrashActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TrashAdapter adapter;
    NotesRepositoryTrash notesRepositoryTrash;
    NoteRepository notesRepository;
    private Button buttonRestoreAll;
    private Button buttonEmptyTrash;
    List<Note> allTrashNotesFromDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);
        notesRepositoryTrash = new NotesRepositoryTrash(this);
        notesRepository = new NoteRepository(this);
        allTrashNotesFromDb = notesRepositoryTrash.getAllNotes();
        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerViewTrash);
        buttonRestoreAll = findViewById(R.id.buttonRestoreAll);
        buttonEmptyTrash = findViewById(R.id.buttonEmptyTrash);

        // Set up RecyclerView with the trash list
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TrashAdapter((ArrayList<Note>) allTrashNotesFromDb, this::restoreNote, this::permanentlyDeleteNote);
        recyclerView.setAdapter(adapter);

        // Handle button clicks
        buttonRestoreAll.setOnClickListener(v -> restoreAllNotes());
        buttonEmptyTrash.setOnClickListener(v -> emptyTrash());
    }

    /**
     * Moves a note from the trash list back to the active notes list.
     *
     * @param position The position of the note to be restored.
     */
    private void restoreNote(int position) {
        if (position >= 0 && position < allTrashNotesFromDb.size()) {
            Note selectedNote = allTrashNotesFromDb.remove(position);
            new Thread(() -> {
                notesRepositoryTrash.deleteNote(selectedNote.getId());
                if (position != -1) {
                    runOnUiThread(() -> {
                        NotesListActivity.allNotes.add(selectedNote); // Keep the main list in sync
                        adapter.notifyItemRemoved(position);
                        notesRepository.addNote(selectedNote);
                        Toast.makeText(TrashActivity.this, "Note Deleted", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
        Toast.makeText(getApplicationContext(),"Note restored..",Toast.LENGTH_SHORT).show();
    }

    /**
     * Permanently deletes a note from the trash list.
     *
     * @param position The position of the note to be permanently deleted.
     */
    private void permanentlyDeleteNote(int position) {
        if (position >= 0 && position < allTrashNotesFromDb.size()) {
            Note selectedNote = allTrashNotesFromDb.remove(position);
            notesRepositoryTrash.deleteNote(selectedNote.getId());
            adapter.notifyItemRemoved(position);
        }
        Toast.makeText(getApplicationContext(),"Note deleted permanently",Toast.LENGTH_SHORT).show();
    }

    /**
     * Restores all notes from the trash.
     */
    private void restoreAllNotes() {
        new Thread(() -> {
            for(Note currentNote: allTrashNotesFromDb){
                notesRepository.addNote(currentNote);
                notesRepositoryTrash.deleteNote(currentNote.getId());
            }
            allTrashNotesFromDb.clear();
            NotesListActivity.allNotes.addAll(allTrashNotesFromDb);
            adapter.notifyDataSetChanged();
        }).start();
        Toast.makeText(getApplicationContext(),"All notes restored..",Toast.LENGTH_SHORT).show();
    }

    /**
     * Empties the trash permanently.
     */
    private void emptyTrash() {
        new Thread(() -> {
            for(Note currentNote: allTrashNotesFromDb){
                notesRepositoryTrash.deleteNote(currentNote.getId());
            }
            allTrashNotesFromDb.clear();
        }).start();
        adapter.notifyDataSetChanged();
        Toast.makeText(getApplicationContext(),"Trash emptied..",Toast.LENGTH_SHORT).show();
    }


}
