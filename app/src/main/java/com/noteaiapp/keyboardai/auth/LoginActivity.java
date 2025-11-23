package com.noteaiapp.keyboardai.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
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

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "com.noteaiapp.keyboardai";
    private TextView forgotPasswordTextView; // <-- ADD THIS LINE
    // UI Elements
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private SignInButton googleSignInButton;
    private TextView signUpTextView;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI Elements
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        googleSignInButton = findViewById(R.id.google_sign_in_button);
        signUpTextView = findViewById(R.id.sign_up_text_view);
        progressBar = findViewById(R.id.login_progress_bar);
        forgotPasswordTextView = findViewById(R.id.forgot_password_text_view); // <-- ADD THIS LINE

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        configureGoogleSignIn();

        // Set up click listeners
        setupClickListeners();
    }

    private void configureGoogleSignIn() {
        // Configure Google Sign In to request the user's ID, email address, and basic profile.
        // ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Get the web client ID from strings.xml
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize the launcher for the Google Sign-In result
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            // Google Sign In was successful, authenticate with Firebase
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            // Google Sign In failed, update UI appropriately
                            Log.w(TAG, "Google sign in failed", e);
                            Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                    } else {
                        // The user cancelled the sign-in flow
                        showLoading(false);
                    }
                });
    }

    private void setupClickListeners() {
        forgotPasswordTextView.setOnClickListener(v -> showForgotPasswordDialog());
        loginButton.setOnClickListener(v -> loginUserWithEmail());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        signUpTextView.setOnClickListener(v -> {
            // TODO: Create and launch SignUpActivity
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
    }
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        // Set up the input
        final EditText input = new EditText(this);
        input.setHint("Enter your registered email");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Send", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_LONG).show();
                return;
            }
            sendPasswordResetEmail(email);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    // Add this new method to LoginActivity.java
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
    private void sendPasswordResetEmail(String email) {
        showLoading(true);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent.");
                        Toast.makeText(LoginActivity.this, "Password reset email sent.. Please check your spam folder as well.", Toast.LENGTH_LONG).show();
                    } else {
                        Log.w(TAG, "sendPasswordResetEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Failed to send reset email. Please check the address.", Toast.LENGTH_LONG).show();
                    }
                    showLoading(false);
                });
    }

    private void loginUserWithEmail() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            return;
        }
        if (password.isEmpty() || password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            // --- SUCCESS: User is verified, proceed to the main app ---
                            Log.d(TAG, "signInWithEmail:success - User is verified.");
                            //navigateToMainApp();
                            checkForLocalNotesMigration();
                        }else {
                            // --- FAILURE: User is not verified ---
                            Log.w(TAG,"signInWithEmail:failure - User's email is not verified.");
                            Toast.makeText(LoginActivity.this, "Please verify your email address to log in. Check your inbox.", Toast.LENGTH_LONG).show();

                            // Sign the user out immediately because they are not allowed in yet.
                            mAuth.signOut();
                            showLoading(false);

                            // Create and show a dialog to inform the user and offer to resend the email.
                            new AlertDialog.Builder(this)
                                    .setTitle("Email Not Verified")
                                    .setMessage("Please verify your email address to continue. Would you like us to resend the verification link?")
                                    .setPositiveButton("Resend", (dialog, which) -> {
                                        // Call the new method to resend the email
                                        resendVerificationEmail(user);
                                    })
                                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                                    .show();
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        String errorMessage = "Authentication failed.";
                        if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                            errorMessage = "No account found with this email.";
                        } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Incorrect password. Please try again.";
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();

                        Log.w(TAG, "signInWithEmail:failure", task.getException());

                        showLoading(false);
                    }
                });
    }
    // Add this new method to LoginActivity.java
    private void resendVerificationEmail(FirebaseUser user) {
        if (user == null) return;

        showLoading(true);
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Verification email resent successfully.");
                        Toast.makeText(LoginActivity.this, "A new verification email has been sent. Please check your inbox or spam folder.", Toast.LENGTH_LONG).show();
                    } else {
                        Log.w(TAG, "sendEmailVerification:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Failed to send verification email. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                    showLoading(false);
                });
    }

    private void signInWithGoogle() {
        showLoading(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithCredential:success");
                        //navigateToMainApp();
                        checkForLocalNotesMigration();
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(this, "Firebase Authentication failed.", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
    }
    // Add this new method to LoginActivity.java
    // In LoginActivity.java
    private void checkForLocalNotesMigration() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean hasMigrated = prefs.getBoolean("has_migrated_local_notes102", false);
        FirebaseUser user = mAuth.getCurrentUser();

        if (!hasMigrated && user != null) {
            // --- MIGRATION IS NEEDED ---
            Log.d(TAG, "First login on this device detected. Starting full data migration to Firebase...");
            showLoading(true); // Show loading spinner

            Executors.newSingleThreadExecutor().execute(() -> {
                // Get instances of Firebase services
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                String userId = user.getUid();

                NoteRepository repository = new NoteRepository(getApplicationContext());
                List<Note> localNotes = repository.getAllNotes();

                if (localNotes.isEmpty()) {
                    // If there's nothing to migrate, set the flag and navigate.
                    prefs.edit().putBoolean("has_migrated_local_notes102", true).apply();
                    Log.d(TAG, "No local notes found to migrate.");
                    runOnUiThread(this::navigateToMainApp); // Use method reference for cleanliness
                    return;
                }
                Set<String> uniqueFolders = new HashSet<>();
                final int totalNotes = localNotes.size();
                final int[] notesProcessed = {0};
                // Loop through each local note to upload it
                for (Note note : localNotes) {
                    String noteCloudId = UUID.randomUUID().toString();
                    note.setUserFirebaseId(noteCloudId); // Stamp with user ID
                    if (note.getFontFamily() != null && !note.getFontFamily().isEmpty()) {
                        uniqueFolders.add(note.getFontFamily());
                    }
                    if (note.getImagePath() != null && !note.getImagePath().isEmpty()) {
                        File localImageFile = new File(note.getImagePath());
                        if (localImageFile.exists()) {
                            Uri localImageUri = Uri.fromFile(localImageFile);
                            String cloudFileName = localImageFile.getName();
                            StorageReference imageRef = storageRef.child("images/" + userId + "/" + cloudFileName);

                            // --- START: THIS IS THE CRITICAL FIX ---
                            // Upload the file, and only in the success listener do we upload the note data.
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
                                    })
                                    /*
                                    .addOnSuccessListener(taskSnapshot -> {
                                        // 1. Image upload is successful. Now, update the note's path.
                                        note.setImagePath(imageRef.getPath()); // e.g., "images/userId/image.jpg"
                                        Log.d(TAG, "Image uploaded for note " + note.getId() + " to " + imageRef.getPath());

                                        // 2. With the correct cloud path set, NOW upload the note to Firestore.
                                        uploadNoteToFirestore(db, note, totalNotes, notesProcessed, prefs, uniqueFolders, userId);
                                    })*/
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Image upload failed for note " + note.getId(), e);
                                        // If image upload fails, still upload the note but with a null image path.
                                        note.setImagePath(null);
                                        uploadNoteToFirestore(db, note, totalNotes, notesProcessed, prefs, uniqueFolders, userId);
                                    });
                            // --- END: CRITICAL FIX ---
                        } else {
                            // Local image file doesn't exist, so upload note without it.
                            Log.w(TAG, "Local image file not found for note " + note.getId() + " at path: " + note.getImagePath());
                            note.setImagePath(null);
                            uploadNoteToFirestore(db, note, totalNotes, notesProcessed, prefs, uniqueFolders, userId);
                        }
                    } else {
                        // No image for this note, so upload its data directly to Firestore.
                        uploadNoteToFirestore(db, note, totalNotes, notesProcessed, prefs, uniqueFolders, userId);
                    }
                }
            });
        } else {
            // --- MIGRATION IS NOT NEEDED ---
            // The flag is already true. Navigate to the main app immediately.
            Log.d(TAG, "Migration not needed. Navigating to main app.");
            showLoading(false);

            navigateToMainApp();
        }
    }

    private void uploadNoteToFirestore(FirebaseFirestore db, Note note, int totalNotes, int[] notesProcessed, SharedPreferences prefs, Set<String> uniqueFolders, String userId) {
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
        if (notesProcessed[0] == totalNotes) {
            prefs.edit().putBoolean("has_migrated_local_notes102", true).apply();
            // All notes have been processed, now upload the folder list
            Log.d(TAG, "All notes processed. Uploading folder list...");
            if (!uniqueFolders.isEmpty()) {
                Map<String, Object> folderData = new
                        HashMap<>();
                folderData.put("array", new ArrayList<>(uniqueFolders));
                db.collection("folder").document(userId).set(folderData)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Folder list uploaded successfully."))
                        .addOnFailureListener(e -> Log.w(TAG, "Error uploading folder list.", e));
            }

            // CRITICAL: Set the migration flag so this never runs again
            prefs.edit().putBoolean("has_migrated_local_notes102", true).apply();
            Log.d(TAG, "Full data migration complete.");
            runOnUiThread(() -> showLoading(false)); // Hide loading indicator
        }
    }

    private void navigateToMainApp() {
        //checkForLocalNotesMigration();
        Intent intent = new Intent(LoginActivity.this, NotesListActivity.class);
        // Clear the activity stack so the user can't go back to the login screen
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
            googleSignInButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            loginButton.setEnabled(true);
            googleSignInButton.setEnabled(true);
        }
    }
}
