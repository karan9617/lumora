package com.example.keyboardai;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.keyboardai.Models.Label;
import com.example.keyboardai.adapter.ColorAdapter;
import com.example.keyboardai.data.NoteRepository;

import java.util.Arrays;
import java.util.List;

public class LabelDialogFragment extends DialogFragment {

    private static final String ARG_NOTE_ID = "note_id";

    private EditText labelNameInput;
    private Button createButton;
    private Button cancelButton;
    private RecyclerView colorPickerRecyclerView;

    private int selectedColor = Color.parseColor("#4285F4"); // Default color
    private long noteId;
    private NoteRepository noteRepository;

    public static LabelDialogFragment newInstance(long noteId) {
        LabelDialogFragment fragment = new LabelDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_NOTE_ID, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            noteId = getArguments().getLong(ARG_NOTE_ID);
        }
        noteRepository = new NoteRepository(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_label, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        labelNameInput = view.findViewById(R.id.label_name_input);
        createButton = view.findViewById(R.id.create_button);
        cancelButton = view.findViewById(R.id.cancel_button);
        colorPickerRecyclerView = view.findViewById(R.id.color_picker_recycler_view);

        setupColorPicker();

        createButton.setOnClickListener(v -> createLabel());
        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void setupColorPicker() {
        List<Integer> colors = Arrays.asList(
                Color.parseColor("#4285F4"), // Blue
                Color.parseColor("#34A853"), // Green
                Color.parseColor("#FBBC05"), // Yellow
                Color.parseColor("#EA4335"), // Red
                Color.parseColor("#9E9E9E"), // Grey
                Color.parseColor("#FFC107"), // Amber
                Color.parseColor("#E91E63"), // Pink
                Color.parseColor("#9C27B0")  // Purple
        );
        ColorAdapter adapter = new ColorAdapter(colors, color -> selectedColor = color);
        colorPickerRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        colorPickerRecyclerView.setAdapter(adapter);
    }

    private void createLabel() {
        String labelName = labelNameInput.getText().toString().trim();
        if (labelName.isEmpty()) {
            Toast.makeText(getContext(), "Label name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add the new label and link it to the note in a background thread
        new Thread(() -> {
            long labelId = noteRepository.addLabel(new Label(labelName, selectedColor));
            if (labelId != -1) {
                noteRepository.addLabelToNote(noteId, labelId);
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Label created and added to note!", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            } else {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Label already exists or an error occurred", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
