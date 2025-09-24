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
        void onNoteLongClick(View v,Note note, View sharedView);
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

        // Show drawing or text
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

        // Set background color
        //holder.noteCard.setCardBackgroundColor(note.getColor());

        // Transition name
        ViewCompat.setTransitionName(holder.noteCard, "note_card_transition_" + note.getId());

        // Pin icon
        holder.notePinImageView.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);
        /*
        // Highlight border if selected
        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.RED);
        border.setCornerRadius(16);
        border.setStroke(6, Color.parseColor("#3399FF")); // blue highlight border
*/
        if (position == selectedPosition || note.isSelected()) {
            holder.noteCard.setCardBackgroundColor(Color.BLUE);

            //holder.noteCard.setForeground(Color.RED);
        } else {
            holder.noteCard.setCardBackgroundColor(note.getColor());
            holder.noteCard.setForeground(null);

        }

        // Normal click
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

        // Long click = highlight + trigger listener
        holder.itemView.setOnLongClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                Note clickedNote = notes.get(currentPosition);
                selectItem(currentPosition);
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
        if (selectedPosition != RecyclerView.NO_POSITION) {
            int oldSelectedPosition = selectedPosition;
            selectedPosition = RecyclerView.NO_POSITION;
            if (oldSelectedPosition < notes.size()) {
                notifyItemChanged(oldSelectedPosition);
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

    public void selectItem(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;

        if (oldPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPosition);
        }
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition);
        }
    }

    public void clearSelection() {
        int oldPosition = selectedPosition;
        selectedPosition = RecyclerView.NO_POSITION;
        if (oldPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPosition);
        }
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
