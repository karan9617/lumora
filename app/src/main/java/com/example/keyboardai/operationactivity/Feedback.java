package com.example.keyboardai.operationactivity;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.keyboardai.R;

public class Feedback extends AppCompatActivity {

    private EditText nameInput, emailInput, feedbackInput;
    private Button submitButton;

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

                // Later: You could send this data to a server, Firebase, or email
                nameInput.setText("");
                emailInput.setText("");
                feedbackInput.setText("");
            }
        });
    }
}