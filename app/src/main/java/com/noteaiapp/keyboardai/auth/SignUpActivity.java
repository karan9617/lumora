package com.noteaiapp.keyboardai.auth;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.noteaiapp.keyboardai.NotesListActivity;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.data.NoteRepository;

import java.util.List;
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
        boolean hasMigrated = prefs.getBoolean("has_migrated_local_notes", false);
        FirebaseUser user = mAuth.getCurrentUser();

        if (!hasMigrated && user != null) {
            Log.d(TAG, "New user account created. Checking for and claiming local notes...");
            Executors.newSingleThreadExecutor().execute(() -> {
                NoteRepository repository = new NoteRepository(getApplicationContext());
                List<com.noteaiapp.keyboardai.Models.Note> localNotes = repository.getAllNotes();

                if (localNotes.isEmpty()) {
                    prefs.edit().putBoolean("has_migrated_local_notes", true).apply();
                    return;
                }

                for (com.noteaiapp.keyboardai.Models.Note note : localNotes) {
                    note.setUserFirebaseId(user.getUid());
                    repository.updateNote(note);
                    // TODO: In the next phase, upload this note to Firestore
                }
                prefs.edit().putBoolean("has_migrated_local_notes", true).apply();
                Log.d(TAG, "Migration complete. " + localNotes.size() + " notes were claimed.");
            });
        }
    }
}

