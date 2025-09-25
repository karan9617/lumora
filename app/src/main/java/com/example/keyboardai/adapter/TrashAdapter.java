package com.example.keyboardai.adapter;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.keyboardai.Models.Note;
import com.example.keyboardai.R;
import com.example.keyboardai.data.FileUtils;

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

    @Override
    public void onBindViewHolder(@NonNull TrashNoteViewHolder holder, int position) {
        Note note = trashList.get(position);
        holder.titleTextView.setText(note.getTitle().split(";")[0]);
        holder.contentTextView.setText(note.getContent());
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
