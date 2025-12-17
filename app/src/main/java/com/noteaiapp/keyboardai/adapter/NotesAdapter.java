package com.noteaiapp.keyboardai.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Spannable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.noteaiapp.keyboardai.Models.ChatMessage;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;

public class NotesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemTouchHelperAdapter {

    private final Context context;
    private final List<Note> notes;
    private final OnNoteClickListener listener;
    private final OnNoteLongClickListener longClickListener;
    private final NoteRepository noteRepository;
    private Animation shakeAnimation;

    private final ItemTouchHelper itemTouchHelper;

    // Use a Set to store multiple selected positions
    private final Set<Integer> selectedPositions = new HashSet<>();
    RecyclerView notesRecyclerView;
    // At the top of your NotesAdapter class
    private static final int VIEW_TYPE_STANDARD_NOTE = 1;
    private static final int VIEW_TYPE_AI_NOTE = 2;

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
                        ItemTouchHelper itemTouchHelper, RecyclerView notesRecyclerView) {
        this.context = context;
        this.notes = notes;
        this.listener = listener;
        this.longClickListener = longClickListener;
        this.noteRepository = new NoteRepository(context);
        this.itemTouchHelper = itemTouchHelper;
        this.notesRecyclerView = notesRecyclerView;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_AI_NOTE) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_ainote, parent, false);
            return new AiNoteViewHolder(view); // Return the new AiNoteViewHolder
        } else { // VIEW_TYPE_STANDARD_NOTE
            view = LayoutInflater.from(context).inflate(R.layout.list_item_note, parent, false);
            return new NoteViewHolder(view); // Return the standard NoteViewHolder
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder1, int position) {
        Note note = notes.get(position);
        if (holder1.getItemViewType() == VIEW_TYPE_AI_NOTE) {
            // This is a saved AI Chat Note.
            // --- START: THIS IS THE CODE YOU NEED ---

            // 1. Cast the generic holder to your specific AiNoteViewHolder.
            AiNoteViewHolder aiHolder = (AiNoteViewHolder) holder1;

            // Set the title from the note object
            String[] titleArr = note.getTitle().split(";");
            aiHolder.noteTitle.setText(titleArr[0].trim().substring(0, Math.min(titleArr[0].length(), 10)) + "...");

            // Set the date (using your existing parsing logic for consistency)
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(note.getDate());
                String formattedDate = outputFormat.format(date);
                aiHolder.noteDate.setText(formattedDate);
            } catch (ParseException e) {
                e.printStackTrace();
                aiHolder.noteDate.setText(note.getDate());
            }

            // --- DYNAMICALLY CREATING THE MINI-CHAT PREVIEW ---

            // 2. Clear any old views from the recycled container to prevent duplicates.
            aiHolder.miniChatContainer.removeAllViews();

            // 3. Get the saved HTML content.
            String htmlContent = note.getContent();
            if (htmlContent == null || htmlContent.isEmpty()) {
                // If there's no content, hide the container.
                aiHolder.miniChatContainer.setVisibility(View.GONE);
                return;
            } else {
                aiHolder.miniChatContainer.setVisibility(View.VISIBLE);
            }

            // 4. Parse the HTML and create a mini-bubble for each chat turn.
            String[] parts = htmlContent.split("<h3>");
            int messageCount = 0;
            final int MAX_PREVIEW_MESSAGES = 4; // Set a limit for the preview

            for (String part : parts) {
                if (messageCount >= MAX_PREVIEW_MESSAGES) break;
                if (part.trim().isEmpty() || !part.contains("</h3>")) continue; // Skip empty or invalid parts

                boolean isUser = part.startsWith("You:");
                String message;

                // Inflate the correct bubble layout (user or gemini)
                TextView bubbleView = (TextView) LayoutInflater.from(context).inflate(
                        isUser ? R.layout.mini_bubble_user : R.layout.mini_bubble_gemini,
                        aiHolder.miniChatContainer,
                        false // Don't attach to root yet
                );

                // Extract plain text for the preview, stripping all HTML tags.
                if (isUser) {

                    message = part.replace("You:</h3>", "").replaceAll("<[^>]*>", "").trim();
                    if(message.contains("https://firebasestorage.googleapis.com")){
                        message = "Image";
                    }

                } else if (part.startsWith("Gemini:")) {
                    // For Gemini, we also strip the HTML for this short preview
                    message = part.replace("Gemini:</h3>", "").replaceAll("<[^>]*>", "").trim();
                } else {
                    continue; // Skip parts that don't match, like the initial <h1>
                }

                // Set a short preview of the message text on the bubble
                bubbleView.setText(message.substring(0, Math.min(message.length(), 50)) + "...");

                // 5. Add the newly created bubble view to our container.
                aiHolder.miniChatContainer.addView(bubbleView);
                messageCount++;
            }

            // --- SETTING UP CLICK LISTENERS ---

            // 6. Set click and long-click listeners on the main item view.
            // Use a lambda to ensure the correct note object is captured.
            aiHolder.itemView.setOnClickListener(v -> {
                if (selectedPositions.size() > 0) {
                    toggleSelection(aiHolder.getAdapterPosition());
                } else {
                    listener.onNoteClick(note, aiHolder.itemView);
                }
            });

            aiHolder.itemView.setOnLongClickListener(v -> {
                toggleSelection(aiHolder.getAdapterPosition());
                longClickListener.onNoteLongClick(v, note, aiHolder.itemView);
                return true;
            });

            // --- SELECTION STATE ---
            if (selectedPositions.contains(position)) {
                // Apply selection highlight
                ((MaterialCardView) aiHolder.itemView).setStrokeWidth(12);
                ((MaterialCardView) aiHolder.itemView).setStrokeColor(Color.rgb(41, 128, 185));
            } else {
                // Remove selection highlight
                ((MaterialCardView) aiHolder.itemView).setStrokeWidth(0);
            }

        }
        else{
            NoteViewHolder holder = (NoteViewHolder) holder1; // Cast to NoteViewHolder

            holder.noteContent.setText("");
            holder.noteDrawing.setImageDrawable(null);
            holder.noteDrawing.setVisibility(View.GONE);
            holder.noteContent.setVisibility(View.GONE);
            holder.noteTitle.setVisibility(View.VISIBLE);
            holder.noteDate.setVisibility(View.VISIBLE);
            holder.labeltext1.setVisibility(View.VISIBLE);
            holder.labeltext2.setVisibility(View.VISIBLE);
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
            holder.noteTitle.setVisibility(View.VISIBLE);

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
            String imagePath = note.getImagePath();
            //byte[] drawingData = FileUtils.loadFileFromPath(note.getImagePath());
            if (imagePath != null && imagePath.length() > 0) {
                if(note.getContent() == null || (note.getContent() != null && note.getContent().length() == 0)){
                    holder.labeltext1.setVisibility(View.GONE);
                    holder.labeltext2.setVisibility(View.GONE);
                    holder.noteContent.setVisibility(View.GONE);
                    if(titleArr[0].equalsIgnoreCase("sketch")){
                        holder.noteTitle.setVisibility(View.GONE);
                        holder.noteDate.setVisibility(View.GONE);
                    }
                    else{
                        holder.noteTitle.setVisibility(View.VISIBLE);
                    }
                }
                else{
                    holder.labeltext1.setVisibility(View.VISIBLE);
                    holder.labeltext2.setVisibility(View.VISIBLE);
                    holder.noteContent.setVisibility(View.VISIBLE);
                }
                try {
                    //Bitmap drawingBitmap = BitmapFactory.decodeByteArray(drawingData, 0, drawingData.length);
                    //if (drawingBitmap != null) {
                    Glide.with(context)
                            .load(imagePath) // Tell Glide to load the image from this file path
                            .into(holder.noteDrawing);
                    // holder.noteDrawing.setImageBitmap(drawingBitmap);
                    holder.noteDrawing.setVisibility(View.VISIBLE);

                    /// } else {
                    // holder.noteDrawing.setVisibility(View.GONE);
                    // holder.noteContent.setVisibility(View.GONE);
                    //}
                } catch (Exception e) {
                    e.printStackTrace();
                    holder.noteDrawing.setVisibility(View.GONE);
                    holder.noteContent.setVisibility(View.GONE);
                }
            } else {
                String noteContent = note.getContent();
                String type = note.getFontColor();

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

            ViewCompat.setTransitionName(holder.noteCard, "note_card_transition_" + note.getId());
            holder.notePinImageView.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);
            final int baseColor = note.getColor();
            // Check if the current note's position is in the set of selected positions.
            if (selectedPositions.contains(position)) {
                //holder.noteCard.setCardBackgroundColor(Color.argb(0,123,166,239));
                float[] hsv = new float[3];
                Color.colorToHSV(baseColor, hsv);
                // Reduce the Value (brightness) component, e.g., by 20% (0.8)
                hsv[2] *= 0.8f;
                int darkerColor = Color.HSVToColor(hsv);
                holder.noteCard.setCardBackgroundColor(darkerColor);
                //holder.noteCard.setCardBackgroundColor(note.getColor());
                holder.noteCard.setStrokeWidth(12); // Adjust the thickness as needed (in pixels)
                holder.noteCard.setStrokeColor(Color.rgb(41, 128, 185)); // A nice dark blue color
            } else {
                holder.noteCard.setCardBackgroundColor(note.getColor());
                holder.noteCard.setStrokeWidth(0);
                holder.noteCard.clearAnimation();
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
                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    float[] hsv = new float[3];
                    Color.colorToHSV(baseColor, hsv);
                    // Reduce the Value (brightness) component, e.g., by 20% (0.8)
                    hsv[2] *= 0.8f;
                    startShaking();
                    int darkerColor = Color.HSVToColor(hsv);
                    holder.noteCard.setCardBackgroundColor(darkerColor);
                    // On long click, we toggle the selection and enter multi-selection mode.
                    toggleSelection(currentPosition);
                    longClickListener.onNoteLongClick(v, clickedNote, holder.noteCard);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        String type = notes.get(position).getFontColor(); // You're using fontColor as a type field
        if ("ainote".equalsIgnoreCase(type)) {
            return VIEW_TYPE_AI_NOTE;
        } else {
            return VIEW_TYPE_STANDARD_NOTE;
        }
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
        RecyclerView recyclerView = this.notesRecyclerView; // Assuming your RecyclerView ID is 'recyclerView'
        for (int i = 0; i < getItemCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
            if (holder instanceof NoteViewHolder) {
                ((NoteViewHolder) holder).noteCard.startAnimation(getShakeAnimation());
            }
        }
    }

    /**
     * Stops the shaking animation on all visible items.
     */
    public void stopShaking() {
        // Iterate over all currently visible ViewHolders in the RecyclerView
        RecyclerView recyclerView = this.notesRecyclerView;
        for (int i = 0; i < getItemCount(); i++) {
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
            if (holder instanceof NoteViewHolder) {
                ((NoteViewHolder) holder).noteCard.clearAnimation();
            }
        }
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

    /**
     * Clears all current selections.
     */
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
        TextView noteTitle, noteContent, noteDate,labeltext1,labeltext2;
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
    public static class AiNoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle, noteDate;
        LinearLayout miniChatContainer;

        public AiNoteViewHolder(@NonNull View itemView) {
            super(itemView);
            // These views MUST exist in list_item_ainote.xml
            noteTitle = itemView.findViewById(R.id.note_title);
            noteDate = itemView.findViewById(R.id.note_date);
            miniChatContainer = itemView.findViewById(R.id.mini_chat_container);
        }
    }
}
