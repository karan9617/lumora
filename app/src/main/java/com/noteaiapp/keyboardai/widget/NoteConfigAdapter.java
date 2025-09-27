package com.noteaiapp.keyboardai.widget;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.data.FileUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteConfigAdapter extends RecyclerView.Adapter<NoteConfigAdapter.NoteViewHolder> {

    private List<Note> notes;
    private OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public NoteConfigAdapter(List<Note> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.widget_list_item, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        String t = note.getTitle().split(";")[0];
        holder.titleTextView.setText(t);
        holder.contentTextView.setText(note.getContent());
        holder.itemView.setOnClickListener(v -> listener.onNoteClick(note));

        holder.titleTextView.setText(t);
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(note.getDate());
            String formattedDate = outputFormat.format(date);
            holder.noteDate.setText(formattedDate);
        } catch (ParseException e) {
            e.printStackTrace();
            holder.noteDate.setText(note.getDate());
        }

        byte[] drawingData = FileUtils.loadFileFromPath(note.getImagePath());
        if (drawingData != null && drawingData.length > 0) {
            if(note.getContent() == null || (note.getContent() != null && note.getContent().length() == 0)){
                holder.labeltext1.setVisibility(View.GONE);
                holder.labeltext2.setVisibility(View.GONE);
            }
            else{
                holder.labeltext1.setVisibility(View.VISIBLE);
                holder.labeltext2.setVisibility(View.VISIBLE);
            }
            try {
                Bitmap drawingBitmap = BitmapFactory.decodeByteArray(drawingData, 0, drawingData.length);
                if (drawingBitmap != null) {
                    holder.noteDrawingImageView.setImageBitmap(drawingBitmap);
                    holder.noteDrawingImageView.setVisibility(View.VISIBLE);
                    holder.contentTextView.setVisibility(View.GONE);
                } else {
                    holder.noteDrawingImageView.setVisibility(View.GONE);
                    holder.contentTextView.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                holder.noteDrawingImageView.setVisibility(View.GONE);
                holder.contentTextView.setVisibility(View.GONE);
            }
        } else {
            holder.contentTextView.setText(note.getContent());
            holder.contentTextView.setVisibility(View.VISIBLE);
            holder.noteDrawingImageView.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView contentTextView,noteDate,labeltext1,labeltext2;

        ImageView noteDrawingImageView;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.widget_note_title);
            contentTextView = itemView.findViewById(R.id.widget_note_content);
            noteDrawingImageView = itemView.findViewById(R.id.noteDrawingImageView);
            labeltext1 = itemView.findViewById(R.id.labeltext1);
            labeltext2 = itemView.findViewById(R.id.labeltext2);
            noteDate = itemView.findViewById(R.id.noteDateTextView);
        }
    }
}
