package com.noteaiapp.keyboardai.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.NotesListActivity;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.data.FileUtils;
import com.noteaiapp.keyboardai.data.NoteRepository;
import com.google.android.material.card.MaterialCardView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotesAdapterPinned extends RecyclerView.Adapter<NotesAdapterPinned.NoteViewHolder> implements ItemTouchHelperAdapter {

    private final Context context;
    private final List<Note> notes;
    private final OnNoteClickListener listener;
    private final OnNoteLongClickListener longClickListener;
    private final NoteRepository noteRepository;
    private int selectedPosition = RecyclerView.NO_POSITION;
    RecyclerView notesRecyclerViewPinned;
    private final ItemTouchHelper itemTouchHelper;
    private final Set<Integer> selectedPositions = new HashSet<>();
    private Animation shakeAnimation;
    public interface OnNoteClickListener {
        void onNoteClick(Note note, View sharedView);
    }

    public interface OnNoteLongClickListener {
        void onNoteLongClick(View v,Note note, View sharedView);
    }

    public NotesAdapterPinned(Context context,
                              List<Note> notes,
                              OnNoteClickListener listener,
                              OnNoteLongClickListener longClickListener,
                              ItemTouchHelper itemTouchHelper, RecyclerView notesRecyclerViewPinned) {
        this.context = context;
        this.notes = notes;
        this.listener = listener;
        this.longClickListener = longClickListener;
        this.noteRepository = new NoteRepository(context);
        this.itemTouchHelper = itemTouchHelper;
        this.notesRecyclerViewPinned = notesRecyclerViewPinned;
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
        String[] titleArr = note.getTitle().split(";");
        if(titleArr.length >= 3){
            holder.labeltext1.setText(titleArr[1]);
            holder.labeltext2.setText(titleArr[2]);
        }
        else if(titleArr.length >=2){
            holder.labeltext1.setText(titleArr[1]);
            holder.labeltext2.setText(R.string.general_text);
        }
        else{
            holder.labeltext1.setText(R.string.general_text);
            holder.labeltext2.setText(R.string.general_text);
        }
        holder.noteTitle.setText(titleArr[0]);
        //holder.noteContent.setText(note.getContent());
        try {
            // Define the input and output date formats
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(note.getDate());
            String formattedDate = outputFormat.format(date);
            holder.noteDate.setText(formattedDate);
        } catch (ParseException e) {
            e.printStackTrace();
            holder.noteDate.setText(note.getDate()); // Fallback to original date if parsing fails
        }

        // Check if the note has a drawing and set its visibility
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
            String noteContent = note.getContent();
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
                holder.noteContent.setText(previewBuilder.toString());

            } else {
                // Standard note, use content as is
                holder.noteContent.setText(loadNote(noteContent));
            }
            holder.noteContent.setVisibility(View.VISIBLE);
            holder.noteDrawing.setVisibility(View.GONE);
        }

        // Set the note's background color
        ViewCompat.setTransitionName(holder.noteCard, "note_card_transition_" + note.getId());
        holder.notePinImageView.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);
        final int baseColor = note.getColor();

// Check if the current note's position is in the set of selected positions.
        if (selectedPositions.contains(position)) {
            float[] hsv = new float[3];
            Color.colorToHSV(baseColor, hsv);
            // Reduce the Value (brightness) component, e.g., by 20% (0.8)
            hsv[2] *= 0.8f;
            int darkerColor = Color.HSVToColor(hsv);
            holder.noteCard.setCardBackgroundColor(darkerColor);
            holder.noteCard.setStrokeWidth(12); // Adjust the thickness as needed (in pixels)
            startShaking();
            holder.noteCard.setStrokeColor(Color.rgb(41, 128, 185)); // A nice dark blue color
        } else {
            holder.noteCard.setCardBackgroundColor(note.getColor());
            holder.noteCard.setStrokeWidth(0);
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

        // The following code for the long-press animation and labels has been restored.
        holder.itemView.setOnLongClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));

            float[] hsv = new float[3];
            Color.colorToHSV(baseColor, hsv);
            // Reduce the Value (brightness) component, e.g., by 20% (0.8)
            hsv[2] *= 0.8f;
            int darkerColor = Color.HSVToColor(hsv);
            startShaking();
            holder.noteCard.setCardBackgroundColor(darkerColor);

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
    public String loadNote(String savedHtml) {
        if (savedHtml == null || savedHtml.isEmpty()) {
            return "";
        }
        // HtmlCompat.fromHtml converts HTML tags (<b>, <i>, etc.) back into Spannable text.
        return String.valueOf(HtmlCompat.fromHtml(savedHtml, HtmlCompat.FROM_HTML_MODE_COMPACT));
    }
    private Animation getShakeAnimation() {
        if (shakeAnimation == null) {
            shakeAnimation = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.shake);
            // Set the number of times to repeat the shake.
            // We'll just let it run until we explicitly stop it in this noteaiapp.
        }
        return shakeAnimation;
    }

    /**
     * Starts the shaking animation on all visible items.
     */
    public void startShaking() {
        // Iterate over all currently visible ViewHolders in the RecyclerView
        RecyclerView recyclerView = this.notesRecyclerViewPinned; // Assuming your RecyclerView ID is 'recyclerView'
        for (int i = 0; i < getItemCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
            if (holder instanceof NotesAdapter.NoteViewHolder) {
                ((NotesAdapter.NoteViewHolder) holder).noteCard.startAnimation(getShakeAnimation());
            }
        }
    }

    /**
     * Stops the shaking animation on all visible items.
     */
    public void stopShaking() {
        // Iterate over all currently visible ViewHolders in the RecyclerView
        RecyclerView recyclerView = this.notesRecyclerViewPinned;
        for (int i = 0; i < getItemCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
            if (holder instanceof NotesAdapter.NoteViewHolder) {
                ((NotesAdapter.NoteViewHolder) holder).noteCard.clearAnimation();
            }
        }
    }
    @Override
    public int getItemCount() {
        return notes.size();
    }
    public List<Note> getSelectedNotes() {
        List<Note> selectedNotes = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position >= 0 && position < notes.size()) {
                selectedNotes.add(notes.get(position));
            }
        }
        return selectedNotes;
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
            List<Note> updatedNotes = noteRepository.getAllPinnedNotes();

            ((NotesListActivity) context).runOnUiThread(() -> {
                notes.clear();
                notes.addAll(updatedNotes);
                notifyDataSetChanged();
            });
        }).start();
    }
    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);

            // Check if this was the last item. If so, stop the shaking globally.
            if (selectedPositions.size() == 0) {
                stopShaking();
                // Note: If you have an ActionMode/Contextual Toolbar, you would also close it here
                // or rely on the Activity/Fragment to close it when getSelectedItemCount() is 0.
            }

        } else {
            // Check if this is the first item being selected. If so, start the shaking globally.
            if (selectedPositions.size() == 0) {
                startShaking();
            }
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
    }
    public void clearSelections() {
        if (!selectedPositions.isEmpty()) {
            stopShaking();
            Set<Integer> oldSelections = new HashSet<>(selectedPositions);
            selectedPositions.clear();
            for (Integer position : oldSelections) {
                notifyItemChanged(position);
            }
        }
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle,labeltext1,labeltext2;
        TextView noteContent;
        TextView noteDate;
        ImageView noteDrawing;
        ImageView notePinImageView;
        MaterialCardView noteCard;
        LinearLayout labelsContainer;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.noteTitleTextView);
            noteContent = itemView.findViewById(R.id.noteContentTextView);
            labeltext1 = itemView.findViewById(R.id.labeltext1);
            labeltext2 = itemView.findViewById(R.id.labeltext2);
            noteDate = itemView.findViewById(R.id.noteDateTextView);
            noteDrawing = itemView.findViewById(R.id.noteDrawingImageView);
            notePinImageView = itemView.findViewById(R.id.pinImageView);
            noteCard = itemView.findViewById(R.id.note_card_container);
            labelsContainer = itemView.findViewById(R.id.labels_container);
        }
    }
}
