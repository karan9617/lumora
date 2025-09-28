package com.noteaiapp.keyboardai.operationactivity;


import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.noteaiapp.keyboardai.R;


public class PoliciesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_policies);

        // Reference the TextView if needed to set policies dynamically
        TextView policiesTextView = findViewById(R.id.textViewPolicies);
        policiesTextView.setText(
                Html.fromHtml(
                         getApplicationContext().getString(R.string.notes_ai_policies_line1)+
                                 getApplicationContext().getString(R.string.notes_ai_policies_line2)+
                                 getApplicationContext().getString(R.string.notes_ai_policies_line3) +"<br><br>" +

                                "<b>"+getApplicationContext().getString(R.string.information_heading)+"</b><br>" +
                                getApplicationContext().getString(R.string.notes_ai_1) +
                                getApplicationContext().getString(R.string.notes_ai_2) +
                                getApplicationContext().getString(R.string.notes_ai_3) +
                                getApplicationContext().getString(R.string.notes_ai_4) +
                                getApplicationContext().getString(R.string.notes_ai_5)+"<br><br>" +

                                "<b>"+getApplicationContext().getString(R.string.notes_ai_6)+"</b><br>" +
                                getApplicationContext().getString(R.string.notes_ai_7) +
                                getApplicationContext().getString(R.string.notes_ai_8) +
                                getApplicationContext().getString(R.string.notes_ai_9) +
                                getApplicationContext().getString(R.string.notes_ai_10)
                )
        );
    }
}