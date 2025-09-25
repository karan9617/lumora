package com.example.keyboardai.operationactivity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.keyboardai.R;
import com.example.keyboardai.data.EmailValidator;

public class Feedback extends AppCompatActivity {

    private EditText nameInput, emailInput, feedbackInput;
    private Button submitButton;
    private static final String RECIPIENT_EMAIL = "notes.app.company@gmail.com";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

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
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                // For now just show a success message
                Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_LONG).show();
                if(name != null && email != null && feedback != null){
                    if(name.length() > 0){
                        if(EmailValidator.isValidEmail(email)){
                            if(feedback.length() > 10){
                                Toast.makeText(getApplicationContext(),"Feedback shared",Toast.LENGTH_SHORT).show();
                            }
                            else{
                                message("Please elaborate your feedback, it will helps us to assist you better.");
                            }
                        }
                        else{
                            message("Please enter a valid email.");
                        }
                    }
                    else{
                        message("Please enter a name");
                    }
                }
                else{
                    message("Please enter all the fields.");
                }

                // Later: You could send this data to a server, Firebase, or email
                nameInput.setText("");
                emailInput.setText("");
                feedbackInput.setText("");
            }
        });
    }
    private void message(String s){
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
    }
    // New method to create and launch the email Intent
    private void sendFeedbackEmail(String name, String senderEmail, String feedback) {

        // 1. Construct the email subject
        String subject = "NotesAI Feedback from " + name;

        // 2. Construct the email body
        String body = "Name: " + name + "\n" +
                "Contact Email: " + senderEmail + "\n\n" +
                "Feedback/Message:\n" + feedback;

        // 3. Create the Intent
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        // Use mailto: URI scheme to ensure only email apps handle it
        intent.setData(Uri.parse("mailto:"));

        // Add the recipients, subject, and body
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{RECIPIENT_EMAIL});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        // 4. Launch the email client
        try {
            startActivity(Intent.createChooser(intent, "Send feedback using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            // Handle case where no email app is installed
            Toast.makeText(this, "No email client installed.", Toast.LENGTH_SHORT).show();
        }
    }
}