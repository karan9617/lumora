package com.example.keyboardai;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class OnboardingFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_IMAGE = "image";

    public static OnboardingFragment newInstance(String title, String description, int imageResId) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putInt(ARG_IMAGE, imageResId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding, container, false);

        TextView titleTextView = view.findViewById(R.id.onboardingTitle);
        TextView descriptionTextView = view.findViewById(R.id.onboardingDescription);
        ImageView imageView = view.findViewById(R.id.onboardingImage);

        if (getArguments() != null) {
            titleTextView.setText(getArguments().getString(ARG_TITLE));
            descriptionTextView.setText(getArguments().getString(ARG_DESCRIPTION));
            imageView.setImageResource(getArguments().getInt(ARG_IMAGE));
        }

        return view;
    }
}

