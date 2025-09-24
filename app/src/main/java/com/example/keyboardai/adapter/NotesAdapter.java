package com.example.keyboardai.adapter;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.keyboardai.Models.Label;
import com.example.keyboardai.Models.Note;
import com.example.keyboardai.NotesListActivity;
import com.example.keyboardai.R;
import com.example.keyboardai.data.FileUtils;
import com.example.keyboardai.data.NoteRepository;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> implements ItemTouchHelperAdapter {

    private final Context context;
    private final List<Note> notes;
    private final OnNoteClickListener listener;
    private final OnNoteLongClickListener longClickListener;
    private final NoteRepository noteRepository;
    private final ItemTouchHelper itemTouchHelper;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnNoteClickListener {
        void onNoteClick(Note note, View sharedView);
    }

    public interface OnNoteLongClickListener {
        void onNoteLongClick(Note note, View sharedView);
    }

    // This constructor expects an ItemTouchHelper instance as the last argument
    public NotesAdapter(Context context,
                        List<Note> notes,
                        OnNoteClickListener listener,
                        OnNoteLongClickListener longClickListener,
                        ItemTouchHelper itemTouchHelper) {
        this.context = context;
        this.notes = notes;
        this.listener = listener;
        this.longClickListener = longClickListener;
        this.noteRepository = new NoteRepository(context);
        this.itemTouchHelper = itemTouchHelper;
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
        try {
            // Define the input and output date formats
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());

            // Parse the existing date string into a Date object
            Date date = inputFormat.parse(note.getDate());

            // Format the Date object into the desired output string
            String formattedDate = outputFormat.format(date);
            holder.noteDate.setText(formattedDate);
        } catch (ParseException e) {
            e.printStackTrace();
            holder.noteDate.setText(note.getDate()); // Fallback to original date if parsing fails
        }
        // Conditionally show/hide drawing and text content views
        byte[] drawingData = FileUtils.loadFileFromPath(note.getImagePath());

        if (drawingData != null && drawingData.length > 0) {
            try {
                Bitmap drawingBitmap = BitmapFactory.decodeByteArray(drawingData, 0, drawingData.length);
                if (drawingBitmap != null) {
                    holder.noteDrawing.setImageBitmap(drawingBitmap);
                    holder.noteDrawing.setVisibility(View.VISIBLE);
                    holder.noteContent.setVisibility(View.GONE);
                } else {
                    holder.noteDrawing.setVisibility(View.GONE);
                    holder.noteContent.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                holder.noteDrawing.setVisibility(View.GONE);
                holder.noteContent.setVisibility(View.GONE);
            }
        } else {
            holder.noteContent.setText(note.getContent());
            holder.noteContent.setVisibility(View.VISIBLE);
            holder.noteDrawing.setVisibility(View.GONE);
        }

        // Set the note's background color
        holder.noteCard.setCardBackgroundColor(note.getColor());

        // Set transition name for shared element transition
        ViewCompat.setTransitionName(holder.noteCard, "note_card_transition_" + note.getId());

        // Manage the visibility of the pin icon based on the note's pinned status
        if (note.isPinned()) {
            holder.notePinImageView.setVisibility(View.VISIBLE);
        } else {
            holder.notePinImageView.setVisibility(View.GONE);
        }

        // Add a border for the selected note
        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.TRANSPARENT);
        border.setCornerRadius(16);
        border.setStroke(4, Color.parseColor("#ADD8E6"));

        if (position == selectedPosition) {
            holder.noteCard.setForeground(border);
        } else {
            holder.noteCard.setForeground(null);
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                Note clickedNote = notes.get(currentPosition);
                int oldSelectedPosition = selectedPosition;
                selectedPosition = RecyclerView.NO_POSITION;
                if (oldSelectedPosition != RecyclerView.NO_POSITION && oldSelectedPosition < notes.size()) {
                    notifyItemChanged(oldSelectedPosition);
                }
                listener.onNoteClick(clickedNote, holder.noteCard);
            }
        });
        /*holder.itemView.setOnLongClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                Note clickedNote = notes.get(currentPosition);
                selectItem(currentPosition);

                longClickListener.onNoteLongClick(clickedNote, holder.noteCard);
                return true;
            }
            return false;
        });*/
    }
    public void selectItem(int position) {
        if (selectedPosition == position) {
            // If the same item is long-clicked again, clear the selection
            clearSelection();
        } else {
            // Un-select the previously selected item
            int oldSelectedPosition = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(oldSelectedPosition);
            // Select the new item
            notifyItemChanged(selectedPosition);
        }
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    /**
     * Required by the ItemTouchHelperAdapter interface.
     * This method is called when an item is moved.
     * It now returns a boolean to satisfy the interface.
     */
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

        if (selectedPosition != RecyclerView.NO_POSITION) {
            int oldSelectedPosition = selectedPosition;
            selectedPosition = RecyclerView.NO_POSITION;
            if (oldSelectedPosition < notes.size()) {
                notifyItemChanged(oldSelectedPosition);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return; // Return true to indicate the move was handled
    }

    /**
     * Required by the ItemTouchHelperAdapter interface.
     * This method is called when an item is dismissed (e.g., swiped away).
     */


    @Override
    public void onItemsMoved() {
        new Thread(() -> {
            for (int i = 0; i < notes.size(); i++) {
                Note note = notes.get(i);
                note.setOrder(i);
                noteRepository.updateNoteOrder(note.getId(), i);
            }
        }).start();
    }

    public void onPinUnpinNote(Note note, boolean isPinned) {
        new Thread(() -> {
            noteRepository.updateNotePinStatus(note.getId(), isPinned);
            List<Note> updatedNotes = noteRepository.getAllNotes();
            ((NotesListActivity) context).runOnUiThread(() -> {
                notes.clear();
                notes.addAll(updatedNotes);

                notifyDataSetChanged();

                if (isPinned) {
                    Toast.makeText(context, "Note pinned to top", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Note unpinned", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    public void clearSelection() {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            int oldSelectedPosition = selectedPosition;
            selectedPosition = RecyclerView.NO_POSITION;
            if (oldSelectedPosition < notes.size()) {
                notifyItemChanged(oldSelectedPosition);
            }
        }
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle,noteContent,noteDate;
        ImageView noteDrawing;
        ImageView notePinImageView;
        CardView noteCard;
        LinearLayout labelsContainer;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.noteTitleTextView);
            noteContent = itemView.findViewById(R.id.noteContentTextView);
            noteDate = itemView.findViewById(R.id.noteDateTextView);
            noteDrawing = itemView.findViewById(R.id.noteDrawingImageView);
            notePinImageView = itemView.findViewById(R.id.pinImageView);
            noteCard = itemView.findViewById(R.id.note_card_container);
            labelsContainer = itemView.findViewById(R.id.labels_container);
        }
    }
}
