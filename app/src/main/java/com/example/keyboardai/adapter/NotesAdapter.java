package com.example.keyboardai.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.keyboardai.Models.Note;
import com.example.keyboardai.R;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> implements ItemTouchHelperAdapter {

    private final Context context;
    private final List<Note> notes;
    private final OnNoteClickListener listener;
    private final OnNoteLongClickListener longClickListener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnNoteClickListener {
        void onNoteClick(Note note, View sharedView);
    }

    public interface OnNoteLongClickListener {
        void onNoteLongClick(Note note, View sharedView);
    }

    public NotesAdapter(Context context, List<Note> notes, OnNoteClickListener listener, OnNoteLongClickListener longClickListener) {
        this.context = context;
        this.notes = notes;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);

        holder.noteTitle.setText(note.getTitle());
        holder.noteContent.setText(note.getContent());
        holder.noteDate.setText(note.getDate());

        // Check for drawing data and display it
        byte[] drawingData = note.getDrawingData();
        if (drawingData != null && drawingData.length > 0) {
            Bitmap drawingBitmap = BitmapFactory.decodeByteArray(drawingData, 0, drawingData.length);
            holder.noteDrawing.setImageBitmap(drawingBitmap);
            holder.noteDrawing.setVisibility(View.VISIBLE);
        } else {
            holder.noteDrawing.setVisibility(View.GONE);
        }

        // Apply the note's color to the CardView background
        holder.noteCard.setCardBackgroundColor(note.getColor());

        // **CRITICAL FIX**: Set the transition name on the entire CardView
        ViewCompat.setTransitionName(holder.noteCard, "note_card_transition_" + note.getId());

        // Create a border for the long-pressed state
        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.TRANSPARENT);
        border.setCornerRadius(16);
        border.setStroke(4, Color.parseColor("#ADD8E6")); // Light blue border

        if (position == selectedPosition) {
            holder.noteCard.setForeground(border);
        } else {
            holder.noteCard.setForeground(null);
        }

        // Handle both click and long-click events
        holder.itemView.setOnClickListener(v -> {
            // FIX: Use getAdapterPosition() to get the current, valid position
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                Note clickedNote = notes.get(currentPosition);

                // Deselect on click if a note is currently selected
                if (selectedPosition != RecyclerView.NO_POSITION) {
                    int oldSelectedPosition = selectedPosition;
                    selectedPosition = RecyclerView.NO_POSITION;
                    notifyItemChanged(oldSelectedPosition);
                }
                listener.onNoteClick(clickedNote, holder.noteCard);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            // FIX: Use getAdapterPosition() to get the current, valid position
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                Note longPressedNote = notes.get(currentPosition);

                if (currentPosition != selectedPosition) {
                    int oldSelectedPosition = selectedPosition;
                    selectedPosition = currentPosition;
                    notifyItemChanged(oldSelectedPosition);
                    notifyItemChanged(selectedPosition);
                }
                longClickListener.onNoteLongClick(longPressedNote, holder.noteCard);
                return true; // Consume the long-press event
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(notes, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(notes, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle;
        TextView noteContent;
        TextView noteDate;
        ImageView noteDrawing;
        CardView noteCard; // Reference to the CardView

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.noteTitleTextView);
            noteContent = itemView.findViewById(R.id.noteContentTextView);
            noteDate = itemView.findViewById(R.id.noteDateTextView);
            noteDrawing = itemView.findViewById(R.id.noteDrawingImageView);
            noteCard = itemView.findViewById(R.id.note_card_container); // Link the CardView
        }
    }
}
