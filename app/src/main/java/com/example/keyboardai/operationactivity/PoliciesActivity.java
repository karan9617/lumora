package com.example.keyboardai.operationactivity;


import android.os.Bundle;
import android.text.Html;
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
                Html.fromHtml(
                        "The NotesAI policies describe the privacy practices of the Notes AI android application. " +
                                "NotesAI is a software which enables users to save data and personal information in the form of personalized notes. " +
                                "A comprehensive guidelines of the way the user information is handled is described below:<br><br>" +

                                "<b>Information collection and use.</b><br>" +
                                "While using NotesAI users can store data as well as information to keep intact on one application. " +
                                "In the form of notes the data can be accessed later for future references. " +
                                "The personal information shared by the user will be stored in the user's phone and will not be accessible by the NotesAI application or any other third party. " +
                                "Only the user is qualified to use and view his or her information as long as the user doesn't manually share it. " +
                                "The information is safe in the user's phone.<br><br>" +

                                "<b>Our access to your content.</b><br>" +
                                "While using NotesAI, the user can store data in the form of voice notes as text. " +
                                "For providing the functionality, NotesAI uses a microphone permission from the user's phone. " +
                                "The data collected from the microphone is stored in the user's phone. " +
                                "The data is secured in the user's phone and can be retrieved for future use."
                )
        );
    }
}