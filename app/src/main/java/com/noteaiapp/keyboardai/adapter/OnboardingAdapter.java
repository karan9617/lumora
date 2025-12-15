package com.noteaiapp.keyboardai.adapter;

// Remove this incorrect import:
// import static android.provider.Settings.Secure.getString;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.noteaiapp.keyboardai.OnboardingFragment;
import com.noteaiapp.keyboardai.R;

public class OnboardingAdapter extends FragmentStateAdapter {

    private final FragmentActivity activityContext; // Store the context
    private String[] titles; // Declare, but don't initialize here

    // You can customize the page content here
    private String[] descriptions;
    private int[] images = {
            R.drawable.logobgremoved, // You'll need to create these drawable resources
            R.drawable.list_icon,
            R.drawable.ic_voice_icon,
            R.drawable.ic_color_lens,
            R.drawable.outline_bolt_24_white
    };

    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        // 1. Store the FragmentActivity/Context
        this.activityContext = fragmentActivity;

        // 2. Initialize the titles array using the stored Context/Activity
        initializeTitles();
    }

    // New method to load strings using the context
    private void initializeTitles() {
        this.titles = new String[]{
                activityContext.getString(R.string.welcome_text),
                activityContext.getString(R.string.create_note_initial_text),
                activityContext.getString(R.string.voice_enabled_ai_text),
                activityContext.getString(R.string.sketch_photo_text),
                activityContext.getString(R.string.ai_chat_text)
        };
        this.descriptions = new String[]{
                activityContext.getString(R.string.welcome_note_text_ai),
                activityContext.getString(R.string.quick_notes_welcome_page_text),
                activityContext.getString(R.string.voice_enabled_initial_text),
                activityContext.getString(R.string.draw_sketch_welcome_page),
                activityContext.getString(R.string.ai_chat_description)
        };
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