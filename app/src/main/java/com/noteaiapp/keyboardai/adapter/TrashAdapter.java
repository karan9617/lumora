package com.noteaiapp.keyboardai.adapter;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.NotesListActivity;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.data.FileUtils;

import java.util.ArrayList;

/**
 * RecyclerView Adapter for displaying a list of notes in the trash.
 */
public class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.TrashNoteViewHolder> {

    public interface OnNoteRestoreListener {
        void onNoteRestore(int position);
    }

    public interface OnNoteDeleteListener {
        void onNoteDelete(int position);
    }

    private final ArrayList<Note> trashList;
    private final OnNoteRestoreListener restoreListener;
    private final OnNoteDeleteListener deleteListener;

    public TrashAdapter(ArrayList<Note> trashList, OnNoteRestoreListener restoreListener, OnNoteDeleteListener deleteListener) {
        this.trashList = trashList;
        this.restoreListener = restoreListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public TrashNoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.trash_item, parent, false);
        return new TrashNoteViewHolder(view);
    }
    public String loadNote(String savedHtml) {
        if (savedHtml == null || savedHtml.isEmpty()) {
            return "";
        }
        // HtmlCompat.fromHtml converts HTML tags (<b>, <i>, etc.) back into Spannable text.
        return String.valueOf(HtmlCompat.fromHtml(savedHtml, HtmlCompat.FROM_HTML_MODE_COMPACT));
    }
    @Override
    public void onBindViewHolder(@NonNull TrashNoteViewHolder holder, int position) {
        Note note = trashList.get(position);
        holder.titleTextView.setText(note.getTitle().split(";")[0]);


        String noteContent = loadNote(note.getContent());

        holder.contentTextView.setText(note.getContent());
        if (noteContent != null && noteContent.startsWith(NotesListActivity.LIST_NOTE_PREFIX)) {
            // 1. Remove the LIST_NOTE_PREFIX and any preceding whitespace
            String listContent = noteContent.substring(NotesListActivity.LIST_NOTE_PREFIX.length()).trim();

            // 2. Split the list content into individual lines
            String[] items = listContent.split("\n");
            StringBuilder previewBuilder = new StringBuilder();
            int itemCount = 0;
            final int MAX_PREVIEW_ITEMS = 5; // Set the maximum number of items to show

            for (String item : items) {
                if (itemCount >= MAX_PREVIEW_ITEMS) {
                    break; // Stop after collecting MAX_PREVIEW_ITEMS
                }

                String cleanedItem = item.trim();
                if (cleanedItem.isEmpty()) {
                    continue; // Skip empty lines
                }

                // 3. Remove the "[x] " or "[ ] " prefix (which is 4 characters long)
                if (cleanedItem.length() >= 4 && (cleanedItem.startsWith("[x] ") || cleanedItem.startsWith("[ ] "))) {
                    cleanedItem = cleanedItem.substring(4).trim();
                }

                if (cleanedItem.isEmpty()) {
                    continue; // Skip items that become empty after cleaning
                }

                // 4. Append the cleaned item to the preview string, separating by a newline character
                if (previewBuilder.length() > 0) {
                    // *** CHANGED: Use newline (\n) instead of ", " ***
                    previewBuilder.append("\n");
                }
                previewBuilder.append(cleanedItem);
                itemCount++;
            }

            // Set the cleaned content preview
            holder.contentTextView.setText(previewBuilder.toString());

        } else {
            // Standard note, use content as is
            holder.contentTextView.setText(noteContent);
        }


        holder.restoreButton.setOnClickListener(v -> restoreListener.onNoteRestore(position));
        holder.deleteButton.setOnClickListener(v -> deleteListener.onNoteDelete(position));
        byte[] drawingData = FileUtils.loadFileFromPath(note.getImagePath());
        if (drawingData != null && drawingData.length > 0) {
            try{
                Bitmap drawingBitmap = BitmapFactory.decodeByteArray(drawingData, 0, drawingData.length);
                if (drawingBitmap != null) {
                    holder.imagesketch.setVisibility(View.VISIBLE);
                    holder.imagesketch.setImageBitmap(drawingBitmap);
                } else {
                    holder.imagesketch.setVisibility(View.GONE);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getItemCount() {
        return trashList.size();
    }

    public static class TrashNoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView contentTextView;
        Button restoreButton;
        Button deleteButton;
        ImageView imagesketch;

        public TrashNoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.textViewNoteTitle);
            imagesketch = itemView.findViewById(R.id.imagesketch);
            contentTextView = itemView.findViewById(R.id.textViewNoteContent);
            restoreButton = itemView.findViewById(R.id.buttonRestore);
            deleteButton = itemView.findViewById(R.id.buttonDelete);
        }
    }
}
