package com.example.keyboardai.operationactivity;


import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.keyboardai.R;

public class PoliciesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policies);

        // Reference the TextView if needed to set policies dynamically
        TextView policiesTextView = findViewById(R.id.textViewPolicies);
        policiesTextView.setText(
                "App Policies:\n\n" +
                        "1. Respect other users.\n" +
                        "2. Do not share personal information.\n" +
                        "3. Feedback and bug reports are welcome.\n" +
                        "4. Follow the terms and conditions."
        );
    }
}