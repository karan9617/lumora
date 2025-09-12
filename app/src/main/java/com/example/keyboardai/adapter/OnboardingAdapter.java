package com.example.keyboardai.adapter;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.keyboardai.OnboardingFragment;
import com.example.keyboardai.R;

public class OnboardingAdapter extends FragmentStateAdapter {

    // You can customize the page content here
    private String[] titles = {"Welcome!", "Create Notes", "Voice enabled AI", "Sketch the photos"};
    private String[] descriptions = {
            "Welcome to Notes.AI! This app helps you capture and organize your thoughts with ease with Integrated AI tools.",
            "Quickly create text or drawing notes. Just tap the plus button to get started.",
            "Voice enabled note taking with AI brain storming feature enabled.",
            "Draw on the rich canvas to enhance the note taking experience with images editing options."
    };
    private int[] images = {
            R.mipmap.logotransparent, // You'll need to create these drawable resources
            R.drawable.ic_large_pen,
            R.drawable.ic_eraser,
            R.drawable.ic_check_circle_24
    };

    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return OnboardingFragment.newInstance(titles[position], descriptions[position], images[position]);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }
}