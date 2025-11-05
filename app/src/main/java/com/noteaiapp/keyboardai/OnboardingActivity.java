package com.noteaiapp.keyboardai;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.noteaiapp.keyboardai.adapter.OnboardingAdapter;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        EdgeToEdge.enable(this);
        viewPager = findViewById(R.id.viewPager);
        nextButton = findViewById(R.id.nextButton);

        // Set up the ViewPager2 with our custom adapter
        OnboardingAdapter adapter = new OnboardingAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Check if we are on the last page
                if (position == adapter.getItemCount() - 1) {
                    nextButton.setText("Start");
                } else {
                    nextButton.setText("Next");
                }
            }
        });
        // Handle the "Next" button click
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If we are on the last page, save that onboarding is complete and go to the main app
                if (viewPager.getCurrentItem() == adapter.getItemCount() - 1) {
                    markOnboardingComplete();
                    startMainActivity();
                } else {
                    // Otherwise, go to the next page
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                }
            }
        });
    }

    private void markOnboardingComplete() {
        SharedPreferences sharedPreferences = getSharedPreferences("onboarding_status", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("hasSeenOnboarding", true);
        editor.apply();
    }

    private void startMainActivity() {
        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}