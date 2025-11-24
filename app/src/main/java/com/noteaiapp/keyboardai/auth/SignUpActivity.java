package com.noteaiapp.keyboardai.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.NotesListActivity;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.data.NoteRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    // UI Elements
    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private Button createAccountButton;
    private TextView loginTextView;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize UI
        emailEditText = findViewById(R.id.email_edit_text_signup);
        passwordEditText = findViewById(R.id.password_edit_text_signup);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text_signup);
        createAccountButton = findViewById(R.id.create_account_button);
        loginTextView = findViewById(R.id.login_text_view);
        progressBar = findViewById(R.id.signup_progress_bar);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Set click listeners
        createAccountButton.setOnClickListener(v -> createNewUser());
        loginTextView.setOnClickListener(v -> {
            // If user wants to go back to login, just finish this activity
            finish();
        });
    }

    private void createNewUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // --- Input Validation ---
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty() || password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            confirmPasswordEditText.requestFocus();
            return;
        }

        // Show loading progress
        showLoading(true);

        // --- Firebase User Creation ---
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign up success
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        if (verificationTask.isSuccessful()) {
                                            Log.d(TAG, "Verification email sent.");
                                            Toast.makeText(SignUpActivity.this,
                                                    "Account created! Please check your email to verify your account.",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                        // Since this is a new user, we still run the migration check.
                        // It will find no local notes and simply set the "migrated" flag.
                        checkForLocalNotesMigration();

                        // Navigate to the main app
                        navigateToMainApp();
                    } else {
                        // If sign up fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignUpActivity.this, "Sign up failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        showLoading(false);
                    }
                });
    }

    private void navigateToMainApp() {
        Intent intent = new Intent(SignUpActivity.this, NotesListActivity.class);
        // Clear the activity stack so the user can't go back to the auth flow
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            createAccountButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            createAccountButton.setEnabled(true);
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            // User is already logged in, no need to be here. Go to main app.
            navigateToMainApp();
        }
    }
    // --- IMPORTANT: Include the migration logic here as well ---
    // This ensures that if a user with an old app creates a new account,
    // their existing local notes are still claimed by that new account.
    private void checkForLocalNotesMigration() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean hasMigrated = prefs.getBoolean("has_migrated_local_notes103", false);
        FirebaseUser user = mAuth.getCurrentUser();

        if (!hasMigrated && user != null) {
            showLoading(true);
            Log.d(TAG, "New user account created. Checking for and claiming local notes...");
            Executors.newSingleThreadExecutor().execute(() -> {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                String userId = user.getUid();

                NoteRepository repository = new NoteRepository(getApplicationContext());
                List<Note> localNotes = repository.getAllNotes();

                if (localNotes.isEmpty()) {
                    prefs.edit().putBoolean("has_migrated_local_notes103", true).apply();
                    Log.d(TAG, "No local notes found to migrate.");
                    return;
                }
                Set<String> uniqueFolders = new HashSet<>();
                final int totalNotes = localNotes.size();
                final int[] notesProcessed = {0};

                for (Note note : localNotes) {
                    String noteCloudId = UUID.randomUUID().toString();
                    note.setUserFirebaseId(noteCloudId);

                    // Collect folder names
                    if (note.getFontFamily() != null && !note.getFontFamily().isEmpty()) {
                        uniqueFolders.add(note.getFontFamily());
                    }
                    if (note.getImagePath() != null && !note.getImagePath().isEmpty()) {
                        File localImageFile = new File(note.getImagePath());
                        if (localImageFile.exists()) {
                            Uri localImageUri = Uri.fromFile(localImageFile);
                            String cloudFileName = localImageFile.getName();
                            // Create a user-specific path in Cloud Storage
                            StorageReference imageRef = storageRef.child("images/" + userId + "/" + cloudFileName);

                            // Upload the file to Cloud Storage
                            imageRef.putFile(localImageUri)
                                    .continueWithTask(task -> {
                                        if (!task.isSuccessful()) {
                                            // If upload fails, pass the exception down the chain
                                            throw task.getException();
                                        }
                                        // 2. If upload succeeds, get the public download URL
                                        Log.d(TAG, "Image uploaded, getting download URL...");
                                        return imageRef.getDownloadUrl();
                                    })
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            String downloadUrl = task.getResult().toString();
                                            Log.d(TAG, "Got download URL: " + downloadUrl);
                                            // Update the note object with the correct CLOUD URL
                                            note.setImagePath(downloadUrl);
                                            uploadNoteToFirestore(db, note, totalNotes, notesProcessed, prefs, uniqueFolders, userId);
                                        } else {
                                            // --- FAILURE ---
                                            Log.w(TAG, "Image upload or URL fetch failed for note", task.getException());
                                            // Fallback: save the note with an empty image path
                                            note.setImagePath("");
                                        }
                                    })/*
                                    .addOnSuccessListener(taskSnapshot -> {
                                        // After image upload, update the note's imagePath to the cloud path
                                        note.setImagePath(imageRef.getPath()); // e.g., "images/userId/image.jpg"
                                        // Now, upload the note metadata to Firestore
                                        uploadNoteToFirestore(db, note, totalNotes, notesProcessed, prefs, uniqueFolders, userId);
                                    })*/
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Image upload failed for note " + note.getId(), e);
                                        // Still upload the note, but with a null image path
                                        note.setImagePath(null);
                                        uploadNoteToFirestore(db, note, totalNotes, notesProcessed, prefs, uniqueFolders, userId);
                                    });
                        } else {
                            // Local image file not found, upload note without it
                            note.setImagePath(null);
                            uploadNoteToFirestore(db, note, totalNotes, notesProcessed, prefs, uniqueFolders, userId);
                        }
                    } else {
                        // No image, just upload the note metadata to Firestore
                        uploadNoteToFirestore(db, note, totalNotes, notesProcessed, prefs, uniqueFolders, userId);
                    }
                    repository.updateNote(note);
                    // TODO: In the next phase, upload this note to Firestore
                }
                Log.d(TAG, "Migration complete. " + localNotes.size() + " notes were claimed.");
            });
        }
    }
    private void uploadNoteToFirestore(FirebaseFirestore db, com.noteaiapp.keyboardai.Models.Note note, int totalNotes, int[] notesProcessed, SharedPreferences prefs, Set<String> uniqueFolders, String userId) {
        // Using the local ID as the document ID in Firestore for easy mapping
        db.collection("users").document(userId).collection("notes").document(String.valueOf(note.getUserFirebaseId()))
                .set(note)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Note " + note.getId() + " uploaded to Firestore.");
                    checkIfMigrationIsComplete(db, totalNotes, notesProcessed, prefs, uniqueFolders, userId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error uploading note " + note.getId(), e);
                    checkIfMigrationIsComplete(db, totalNotes, notesProcessed, prefs, uniqueFolders, userId);
                });
    }
    private void checkIfMigrationIsComplete(FirebaseFirestore db, int totalNotes, int[] notesProcessed, SharedPreferences prefs, Set<String> uniqueFolders, String userId) {
        notesProcessed[0]++;
        if (notesProcessed[0] >= totalNotes) {
            prefs.edit().putBoolean("has_migrated_local_notes103", true).apply();

            // All notes have been processed, now upload the folder list
            Log.d(TAG, "All notes processed. Uploading folder list...");
            if (!uniqueFolders.isEmpty()) {
                Map<String, Object> folderData = new HashMap<>();
                folderData.put("array", new ArrayList<>(uniqueFolders));
                db.collection("folder").document(userId).set(folderData)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Folder list uploaded successfully."))
                        .addOnFailureListener(e -> Log.w(TAG, "Error uploading folder list.", e));
            }

            // CRITICAL: Set the migration flag so this never runs again
            prefs.edit().putBoolean("has_migrated_local_notes103", true).apply();
            Log.d(TAG, "Full data migration complete.");

            // Now that migration is fully complete, navigate to the main app
            runOnUiThread(() -> navigateToMainApp());
        }
    }
}

