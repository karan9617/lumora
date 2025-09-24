package com.example.keyboardai.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> implements ItemTouchHelperAdapter {

    private final Context context;
    private final List<Note> notes;
    private final OnNoteClickListener listener;
    private final OnNoteLongClickListener longClickListener;
    private final NoteRepository noteRepository;
    private final ItemTouchHelper itemTouchHelper;

    // Use a Set to store multiple selected positions
    private final Set<Integer> selectedPositions = new HashSet<>();

    public interface OnNoteClickListener {
        void onNoteClick(Note note, View sharedView);
    }

    public interface OnNoteLongClickListener {
        void onNoteLongClick(View v, Note note, View sharedView);
    }

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

        ViewCompat.setTransitionName(holder.noteCard, "note_card_transition_" + note.getId());
        holder.notePinImageView.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);

        // Check if the current note's position is in the set of selected positions.
        if (selectedPositions.contains(position)) {
            holder.noteCard.setCardBackgroundColor(Color.BLUE);
        } else {
            holder.noteCard.setCardBackgroundColor(note.getColor());
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                // If we are in multi-selection mode, a click should toggle the selection.
                if (selectedPositions.size() > 0) {
                    toggleSelection(currentPosition);
                } else {
                    // Otherwise, a normal click should open the note.
                    Note clickedNote = notes.get(currentPosition);
                    listener.onNoteClick(clickedNote, holder.noteCard);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                Note clickedNote = notes.get(currentPosition);
                // On long click, we toggle the selection and enter multi-selection mode.
                toggleSelection(currentPosition);
                longClickListener.onNoteLongClick(v, clickedNote, holder.noteCard);
                return true;
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
                Toast.makeText(context, isPinned ? "Note pinned to top" : "Note unpinned", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    public void onPinUnpinNote(List<Note> notes, boolean isPinned) {
        noteRepository.updateNotePinStatusBulk(notes, isPinned);
    }
    /**
     * Toggles the selection state of an item.
     * @param position The position of the item to toggle.
     */
    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
    }

    /**
     * Clears all current selections.
     */
    public void clearSelections() {
        if (!selectedPositions.isEmpty()) {
            Set<Integer> oldSelections = new HashSet<>(selectedPositions);
            selectedPositions.clear();
            for (Integer position : oldSelections) {
                notifyItemChanged(position);
            }
        }
    }

    /**
     * @return The number of notes currently selected.
     */
    public int getSelectedItemCount() {
        return selectedPositions.size();
    }

    /**
     * @return A list of the currently selected Note objects.
     */
    public List<Note> getSelectedNotes() {
        List<Note> selectedNotes = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position >= 0 && position < notes.size()) {
                selectedNotes.add(notes.get(position));
            }
        }
        return selectedNotes;
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle, noteContent, noteDate;
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
