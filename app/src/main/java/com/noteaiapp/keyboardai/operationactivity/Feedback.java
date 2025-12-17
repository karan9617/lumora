package com.noteaiapp.keyboardai.operationactivity;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.data.EmailValidator;

import java.util.HashMap;
import java.util.Map;

public class Feedback extends AppCompatActivity {

    private EditText nameInput, emailInput, feedbackInput;
    private Button submitButton;
    private static final String RECIPIENT_EMAIL = "notes.app.company@gmail.com";
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String TAG = "com.noteaiapp.keyboardai";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feedback);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize views
        nameInput = findViewById(R.id.editTextName);
        emailInput = findViewById(R.id.editTextEmail);
        feedbackInput = findViewById(R.id.editTextFeedback);
        submitButton = findViewById(R.id.buttonSubmit);

        // Handle button click
        submitButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String feedback = feedbackInput.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || feedback.isEmpty()) {
                Toast.makeText(this, R.string.fill_notes_messages, Toast.LENGTH_SHORT).show();
            } else {
                // For now just show a success message
                Toast.makeText(this, R.string.thank_you_feedback, Toast.LENGTH_LONG).show();
                if(name != null && email != null && feedback != null){
                    if(name.length() > 0){
                        if(EmailValidator.isValidEmail(email)){
                            if(feedback.length() > 10){
                                Toast.makeText(getApplicationContext(),R.string.feedback_successful,Toast.LENGTH_SHORT).show();
                                sendFeedbackEmail(name, email, feedback);
                            }
                            else{
                                Toast.makeText(getApplicationContext(),R.string.elaborate_feedback_text,Toast.LENGTH_SHORT).show();
                            }
                        }
                        else{
                            Toast.makeText(getApplicationContext(),R.string.invalid_email_text,Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{
                        Toast.makeText(getApplicationContext(),R.string.enter_name_feedback,Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(),R.string.enter_fields_text,Toast.LENGTH_SHORT).show();
                }

                // Later: You could send this data to a server, Firebase, or email
                nameInput.setText("");
                emailInput.setText("");
                feedbackInput.setText("");
            }
        });
    }

    // New method to create and launch the email Intent
    private void sendFeedbackEmail(String name, String senderEmail, String feedback) {

       // firebase code to store the feedback
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to send feedback.", Toast.LENGTH_SHORT).show();
            // Optional: Send them to the login screen
            // startActivity(new Intent(Feedback.this, LoginActivity.class));
            return;
        }

        // 1. Create a data model for the feedback. A HashMap is perfect for this.
        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("name", name);
        feedbackData.put("email", senderEmail);
        feedbackData.put("message", feedback);
        feedbackData.put("timestamp", FieldValue.serverTimestamp()); // Adds a server-side timestamp

        // 2. Get the current user's ID.
        String userId = currentUser.getUid();

        // 3. Add the feedback as a new document to the user's feedback subcollection.
        // The path will be: feedback/{userId}/user_feedback/{auto-generated-id}
        db.collection("feedback")
                .document(userId)
                .collection("user_feedback")
                .add(feedbackData) // .add() automatically generates a unique ID for the document
                .addOnSuccessListener(documentReference -> {
                    // This runs on the UI thread, so it's safe to show a Toast.
                    Log.d(TAG, "Feedback successfully saved to Firestore with ID: " + documentReference.getId());
                    Toast.makeText(Feedback.this, "Feedback sent successfully!", Toast.LENGTH_SHORT).show();

                    // Clear the fields and finish the activity on success
                    nameInput.setText("");
                    emailInput.setText("");
                    feedbackInput.setText("");
                    finish(); // Close the feedback activity
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving feedback", e);
                    Toast.makeText(Feedback.this, "Error sending feedback. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }
}