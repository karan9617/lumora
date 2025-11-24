package com.noteaiapp.keyboardai.operationactivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.noteaiapp.keyboardai.R;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.adapter.TrashAdapter;
import com.noteaiapp.keyboardai.NotesListActivity;
import com.noteaiapp.keyboardai.operationactivity.trashfiles.NotesRepositoryTrash;

import java.io.File;
import java.util.ArrayList;

import java.util.List;
import java.util.UUID;

import com.noteaiapp.keyboardai.data.NoteRepository;

import okhttp3.internal.concurrent.Task;

public class TrashActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TrashAdapter adapter;
    private static final String TAG = "com.noteaiapp.keyboardai";
    private FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    NotesRepositoryTrash notesRepositoryTrash;
    NoteRepository notesRepository;
    private Button buttonRestoreAll;
    private Button buttonEmptyTrash;
    List<Note> allTrashNotesFromDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trash);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        notesRepositoryTrash = new NotesRepositoryTrash(this);
        notesRepository = new NoteRepository(this);
        allTrashNotesFromDb = new ArrayList<>(); // Initialize the list to be used throughout the activity
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
    // In TrashActivity.java
    private void restoreNote(int position) {
        if (position >= 0 && position < allTrashNotesFromDb.size()) {

            // --- THIS IS THE FIX ---
            // 1. Get the note object from the list, but DO NOT remove it yet.
            final Note noteToRestore = allTrashNotesFromDb.get(position);
            // ------------------------

            new Thread(() -> {
                // All operations now use the 'noteToRestore' object, which is guaranteed to be correct.

                // 2. Perform all database and cloud operations.
                //    First, delete from the local trash database.
                notesRepositoryTrash.deleteNote(noteToRestore.getId());

                // 3. Handle the image path.
                String imageUrl = noteToRestore.getImagePath();
                if (imageUrl != null && !imageUrl.isEmpty() && imageUrl.startsWith("http")) {
                    try {
                        File localImageFile = Glide.with(getApplicationContext())
                                .asFile()
                                .load(imageUrl)
                                .submit()
                                .get();
                        noteToRestore.setImagePath(localImageFile.getAbsolutePath());
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to re-download image on restore.", e);
                        noteToRestore.setImagePath("");
                    }
                }

                // 4. Add the complete note back to the main local database.
                //    (We assume the content was correctly loaded by getNoteById in the previous version,
                //     but let's make it explicit here for safety).
                Note fullNote = notesRepositoryTrash.getNoteById(noteToRestore.getId());
                if (fullNote == null) {
                    // If the full note isn't found, use the partial one. It might have no content.
                    fullNote = noteToRestore;
                }
                notesRepository.addNote(fullNote);

                // 5. Prepare and sync the note to Firestore.
                String noteCloudId = fullNote.getUserFirebaseId();
                if (noteCloudId == null || noteCloudId.isEmpty()) {
                    noteCloudId = UUID.randomUUID().toString();
                    fullNote.setUserFirebaseId(noteCloudId);
                    notesRepository.updateNote(fullNote);
                }

                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                db.collection("users").document(userId).collection("notes").document(noteCloudId)
                        .set(fullNote);
                        //.addOnSuccessListener(aVoid -> Log.d(TAG, "Note " + fullNote.getTitle() + " successfully restored to Firestore."))
                        //.addOnFailureListener(e -> Log.w(TAG, "Error restoring note " + noteCloudId + " to Firestore.", e));

                // --- THIS IS THE SECOND PART OF THE FIX ---
                // 6. NOW, update the UI on the main thread after all background work is done.
                runOnUiThread(() -> {
                    // Remove the note from the list that the adapter is using.
                    allTrashNotesFromDb.remove(position);
                    // Notify the adapter that the item at this specific position was removed.
                    adapter.notifyItemRemoved(position);
                    // (Optional but good practice) Notify for range change to update subsequent item positions.
                    adapter.notifyItemRangeChanged(position, allTrashNotesFromDb.size());

                    Toast.makeText(TrashActivity.this, R.string.notes_restored, Toast.LENGTH_SHORT).show();
                });
                // ------------------------------------------

            }).start();
        }
    }


    /**
     * Permanently deletes a note from the trash list.
     *
     * @param position The position of the note to be permanently deleted.
     */
    private void permanentlyDeleteNote(int position) {
        // 1. First, perform a safety check on the position.
        if (position >= 0 && position < allTrashNotesFromDb.size()) {

            // 2. Remove the note from the adapter's list and notify the UI AT THE SAME TIME.
            //    This is the safest way to update the RecyclerView.
            final Note noteToDelete = allTrashNotesFromDb.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, allTrashNotesFromDb.size()); // Helps prevent inconsistency

            // 3. Now, perform the slow database and cloud operations on a background thread.
            new Thread(() -> {
                // Delete from the local trash database
                notesRepositoryTrash.deleteNote(noteToDelete.getId());

                // Also delete the note from Firebase Storage if it has an image
                if (noteToDelete.getImagePath() != null && !noteToDelete.getImagePath().isEmpty()) {
                    // You would add your Firebase Storage deletion logic here
                }
                Log.d(TAG, "Note with ID " + noteToDelete.getId() + " permanently deleted from local DB.");

            }).start();

            // 4. Show immediate feedback to the user on the UI thread.
            Toast.makeText(getApplicationContext(), R.string.note_deleted, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Restores all notes from the trash.
     */
    // DELETE your old restoreAllNotes() method.
// REPLACE it with this new version.

    private void restoreAllNotes() {if (allTrashNotesFromDb == null || allTrashNotesFromDb.isEmpty()) {
        Toast.makeText(getApplicationContext(), R.string.restore_note_trash, Toast.LENGTH_SHORT).show();
        return;
    }

        // Create a copy of the list to iterate over, as we will be modifying the original list.
        final List<Note> notesToRestore = new ArrayList<>(allTrashNotesFromDb);
        final int notesCount = notesToRestore.size();

        // Show immediate feedback
        Toast.makeText(getApplicationContext(), "Restoring " + notesCount + " notes...", Toast.LENGTH_SHORT).show();

        // Perform all slow operations on a background thread.
        new Thread(() -> {

            for (Note partialNote : notesToRestore) {
                // 1. For each note, fetch the FULL object from the trash DB to get its content.
                Note fullNoteToRestore = notesRepositoryTrash.getNoteById(partialNote.getId());

                if (fullNoteToRestore == null) {
                    // If for some reason the full note can't be found, skip it.
                    Log.w(TAG, "Could not find full note for ID: " + partialNote.getId() + ". Skipping restore.");
                    continue;
                }

                // 2. Perform local DB operations: delete from trash, add to main.
                notesRepositoryTrash.deleteNote(fullNoteToRestore.getId());
                notesRepository.addNote(fullNoteToRestore);

                // 3. Handle image re-download for cloud-based images, same as in restoreNote().
                String imageUrl = fullNoteToRestore.getImagePath();
                if (imageUrl != null && !imageUrl.isEmpty() && imageUrl.startsWith("http")) {
                    try {
                        File localImageFile = Glide.with(getApplicationContext())
                                .asFile()
                                .load(imageUrl)
                                .submit()
                                .get();
                        // Update the path to the new local file before saving to Firebase.
                        fullNoteToRestore.setImagePath(localImageFile.getAbsolutePath());
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to re-download image on restore for note ID: " + fullNoteToRestore.getId(), e);
                        fullNoteToRestore.setImagePath(""); // Clear path on failure
                    }
                }

                // 4. THIS IS THE FIX: Sync the restored note back to Firebase.
                String noteCloudId = "";
                if (noteCloudId == null || noteCloudId.isEmpty()) {
                    // If it's an old note without a cloud ID, generate a new one.
                    noteCloudId = UUID.randomUUID().toString();
                    fullNoteToRestore.setUserFirebaseId(noteCloudId);
                    // Also update the main local DB with this new UUID
                    notesRepository.updateNote(fullNoteToRestore);
                } else {
                    noteCloudId = fullNoteToRestore.getUserFirebaseId();
                }

                if (currentUser != null) {
                    // Set the note in the main 'notes' collection in Firestore.
                    db.collection("users").document(currentUser.getUid()).collection("notes")
                            .document(noteCloudId)
                            .set(fullNoteToRestore);

                }
            } // End of for loop

            // 5. AFTER all background work is done, update the UI on the main thread.
            runOnUiThread(() -> {
                // Clear the adapter's list and notify it that the data set is now empty.
                allTrashNotesFromDb.clear();
                adapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(), notesCount + " notes restored.", Toast.LENGTH_SHORT).show();
            });

        }).start();
    }


    /**
     * Empties the trash permanently.
     */
    private void emptyTrash() {
        new Thread(() -> {
            for(Note currentNote: allTrashNotesFromDb){
                notesRepositoryTrash.deleteNoteByCloudId(currentNote.getUserFirebaseId());
            }
            allTrashNotesFromDb.clear();
        }).start();
        adapter.notifyDataSetChanged();
        Toast.makeText(getApplicationContext(),R.string.trash_emptied,Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load the fresh data from the database
        List<Note> freshData = notesRepositoryTrash.getAllNotes();
        // Clear the existing list and add all the new data
        allTrashNotesFromDb.clear();
        allTrashNotesFromDb.addAll(freshData);
        // Notify the adapter that the underlying data has changed
        adapter.notifyDataSetChanged();
    }
}
