// In ChatAdapter.java

package com.noteaiapp.keyboardai.adapter;

import static android.view.View.GONE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.noteaiapp.keyboardai.Models.ChatMessage;
import com.noteaiapp.keyboardai.R;
import com.noteaiapp.keyboardai.geminichat.GeminiChatActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// --- Make the Adapter generic for RecyclerView.ViewHolder ---
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // --- Define view type constants ---
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_GEMINI = 2;
    private static final int VIEW_TYPE_PDF = 3;
    private static final int VIEW_TYPE_IMAGE = 4;
    private static final int VIEW_TYPE_IMAGE_TEXT = 5;
    private static final Logger log = LoggerFactory.getLogger(ChatAdapter.class);
    private List<ChatMessage> chatMessages;
    Context context;
    private final List<Integer> selectedPositions = new ArrayList<>();
    private boolean isSelectionMode = false;
    private final SelectionListener selectionListener;
    public interface SelectionListener {
        void onSelectionModeChanged(boolean isEnabled);
        void onSelectionCountChanged(int count);
    }

    public ChatAdapter(List<ChatMessage> chatMessages, Context context, SelectionListener listener) {
        this.chatMessages = chatMessages;
        this.context = context;
        this.selectionListener = listener;
    }

    // --- getItemViewType is now correct ---
    @Override
    public int getItemViewType(int position) {
        String type = chatMessages.get(position).getType();
        if ("pdf".equalsIgnoreCase(type)) {
            return VIEW_TYPE_PDF;
        }
        else if("imagetext".equalsIgnoreCase(type)){
            return VIEW_TYPE_IMAGE_TEXT;
        }else if("image".equalsIgnoreCase(type)){
            return VIEW_TYPE_IMAGE;
        }
        else if (chatMessages.get(position).isUser()) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_GEMINI;
        }
    }
    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(Integer.valueOf(position));
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);

        // If the last item is deselected, exit selection mode
        if (selectedPositions.isEmpty()) {
            exitSelectionMode();
        } else if (!isSelectionMode) {
            // If the first item is selected, enter selection mode
            isSelectionMode = true;
            selectionListener.onSelectionModeChanged(true);
        }
        if (isSelectionMode) {
            selectionListener.onSelectionCountChanged(selectedPositions.size());
        }
    }
    public void exitSelectionMode() {
        isSelectionMode = false;
        List<Integer> positionsToUpdate = new ArrayList<>(selectedPositions);
        selectedPositions.clear();
        for (int position : positionsToUpdate) {
            notifyItemChanged(position); // Redraw only previously selected items
        }
        selectionListener.onSelectionModeChanged(false);
    }
    public void setMessages(List<ChatMessage> newMessages) {
        this.chatMessages = newMessages;
        // Don't call notifyDataSetChanged() here; let the activity control it.
    }
    public void deleteSelectedMessages() {
        if (selectedPositions.isEmpty()) {
            return;
        }
        Collections.sort(selectedPositions, Collections.reverseOrder());

        for (int position : selectedPositions) {
            if (position < chatMessages.size()) {
                chatMessages.remove(position);
                notifyItemRemoved(position);
            }
        }
        // --- END: THIS IS THE FIX ---

        // Clear the selection list after deletion
        selectedPositions.clear();
        isSelectionMode = false; // We are exiting selection mode
    }
    public List<ChatMessage> getSelectedMessages() {
        List<ChatMessage> selectedMessages = new ArrayList<>();
        for (int position : selectedPositions) {
            selectedMessages.add(chatMessages.get(position));
        }
        return selectedMessages;
    }
    // --- onCreateViewHolder now returns the correct ViewHolder for each type ---
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_PDF) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_pdf, parent, false);
            return new PdfViewHolder(view); // Return a PdfViewHolder
        }
        else if(viewType == VIEW_TYPE_IMAGE_TEXT){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_image_text, parent, false);
            return new ImageTextHolder(view);
        }
        else if (viewType == VIEW_TYPE_IMAGE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_image, parent, false);
            return new ImageViewHolder(view); // Return a PdfViewHolder
        }
        else if (viewType == VIEW_TYPE_USER) { // Handles both USER and GEMINI text types
            // We assume item_chat_user and item_chat_gemini's root TextView ID is the same
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new TextViewHolder(view); // Return a TextViewHolder
        }
        else{
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_gemini, parent, false);
            return new GeminiViewHolder(view); // Return a TextViewHolder
        }
    }

    // --- onBindViewHolder now casts to the correct ViewHolder ---
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);

        switch (holder.getItemViewType()) {

            case VIEW_TYPE_IMAGE_TEXT:
                ImageTextHolder imageTextHolder = (ImageTextHolder) holder;
                String messageTextFromImage = chatMessage.getMessage();
                imageTextHolder.imagetext_message_text_view.setText("Image Attached: what would you like to know?");
                // The icon is already set in the XML, so we don't need to change it.
                break;

            case VIEW_TYPE_IMAGE:
                ImageViewHolder imageViewHolder = (ImageViewHolder) holder;
                String urlFromFirebase = chatMessage.getMessage().trim().toString();
                String[] urls = urlFromFirebase.split(";");
                // Set a preview of the attached content
                if (urlFromFirebase != null && !urlFromFirebase.isEmpty()) {
                    Glide.with(context) // Use the context from the adapter
                            .load(urls[0]) // The URL of the image
                            .into(imageViewHolder.image_icon); // The target ImageView
                } else {
                    // If the URL is null or empty, you can set a default or error image.
                    imageViewHolder.image_icon.setImageResource(R.drawable.ic_menu_favorites);
                }
                // The icon is already set in the XML, so we don't need to change it.
                break;

            case VIEW_TYPE_PDF:
                PdfViewHolder pdfViewHolder = (PdfViewHolder) holder;
                String message = chatMessage.getMessage();
                // Set a preview of the attached content
                pdfViewHolder.messageText.setText("PDF Attached: " + message.substring(0, Math.min(message.length(), 40)) + "...");
                // The icon is already set in the XML, so we don't need to change it.
                break;

            case VIEW_TYPE_USER:
                TextViewHolder userViewHolder = (TextViewHolder) holder;
                userViewHolder.messageText.setText(chatMessage.getMessage());
                userViewHolder.messageText.setBackgroundResource(R.drawable.user_chat_bubble);
                userViewHolder.messageText.setTextColor(Color.WHITE);
                break;

            case VIEW_TYPE_GEMINI:
                GeminiViewHolder geminiViewHolder = (GeminiViewHolder) holder;
                String messageText = chatMessage.getMessage();

                // --- FIX: Use HtmlCompat to render styled text ---
                Spannable styledText = (Spannable) HtmlCompat.fromHtml(messageText, HtmlCompat.FROM_HTML_MODE_LEGACY);
                styleCodeBlocks(styledText);
                geminiViewHolder.messageText.setText(styledText);

                // --- START: ADD CLICK LISTENERS ---
                geminiViewHolder.copyButton.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    // Use the styled text for copying, which will paste as plain text
                    ClipData clip = ClipData.newPlainText("Copied Text", geminiViewHolder.messageText.getText());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                });

                geminiViewHolder.shareButton.setOnClickListener(v -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, geminiViewHolder.messageText.getText().toString());
                    context.startActivity(Intent.createChooser(shareIntent, "Share via"));
                });
                geminiViewHolder.editButton.setOnClickListener(v -> {
                    // Get the current message object
                    ChatMessage messageToEdit = chatMessages.get(holder.getAdapterPosition());

                    // Show the edit dialog
                    showEditDialog(messageToEdit, holder.getAdapterPosition());
                });
                // --- END: ADD CLICK LISTENERS ---

                break;
        }
        if (selectedPositions.contains(position)) {
            // If it is selected, set a light blue highlight color on its background.
            holder.itemView.setBackgroundColor(Color.parseColor("#B0E0E6")); // Light blue/cyan highlight
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(holder.getAdapterPosition());
            }
            // Add any regular click logic here if needed
        });

        holder.itemView.setOnLongClickListener(v -> {
            toggleSelection(holder.getAdapterPosition());
            return true; // Consume the long click event
        });
    }
    // In ChatAdapter.java


    // In ChatAdapter.java

    private void showEditDialog(ChatMessage message, int position) {
        // 1. Inflate the custom layout.
        // The parent is null because it's going in an AlertDialog.
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_message, null);
        final com.google.android.material.textfield.TextInputEditText input = dialogView.findViewById(R.id.edit_text_input);

        // 2. Pre-fill the EditText with the current message content.
        // It's better to use the raw message for editing, as HtmlCompat can alter it.
        input.setText(message.getMessage());

        // 3. Create the AlertDialog using the Material Design builder.
        // This provides better default styling.
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Message");
        builder.setView(dialogView); // Set the custom view

        // 4. Set up the dialog buttons.
        builder.setPositiveButton("Save", (dialog, which) -> {
            String editedText = input.getText().toString().trim();
            if (!editedText.isEmpty()) {
                // Update the message object in our list
                message.setMessage(editedText);
                // Notify the adapter that this specific item has changed
                notifyItemChanged(position);
                Toast.makeText(context, "Message updated", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // 5. Create and show the dialog.
        AlertDialog dialog = builder.create();
        dialog.show();

        // Optional: Request focus and show keyboard when the dialog appears.
        input.requestFocus();
        android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(lp);
    }



    // --- The rest of your methods ---
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    private void styleCodeBlocks(Spannable spannable) {
        // Your existing implementation is correct
        Object[] spans = spannable.getSpans(0, spannable.length(), Object.class);
        for (Object span : spans) {
            if (span.getClass().getName().equals("android.text.style.QuoteSpan")) {
                int start = spannable.getSpanStart(span);
                int end = spannable.getSpanEnd(span);
                spannable.removeSpan(span);
                spannable.setSpan(new BackgroundColorSpan(Color.parseColor("#F0F0F0")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    // --- START: DEFINE SEPARATE VIEWHOLDERS ---

    // 1. A ViewHolder for simple text messages (User and Gemini)
    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            // Assumes your item_chat_user.xml's TextView has this ID.
            // If the root element IS the TextView, this could be just `(TextView) itemView`.
            messageText = itemView.findViewById(R.id.chat_message_text);
        }
    }

    static class GeminiViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView copyButton;
        ImageView shareButton;
        ImageView editButton;

        public GeminiViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.chat_message_text);
            copyButton = itemView.findViewById(R.id.copyMessageButton);
            shareButton = itemView.findViewById(R.id.shareButton);
            editButton = itemView.findViewById(R.id.editprompt);
        }
    }

    // 2. A ViewHolder specifically for PDF messages
    static class PdfViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView promptimage; // Renamed from iconView for consistency

        public PdfViewHolder(@NonNull View itemView) {
            super(itemView);
            // These IDs must match your chat_item_pdf.xml
            messageText = itemView.findViewById(R.id.pdf_message_text_view);
            promptimage = itemView.findViewById(R.id.pdf_icon);
        }
    }
    static class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView image_icon; // Renamed from iconView for consistency

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            // These IDs must match your chat_item_pdf.xml
            image_icon = itemView.findViewById(R.id.image_icon);
        }
    }
    static class ImageTextHolder extends RecyclerView.ViewHolder {
        TextView imagetext_message_text_view;

        public ImageTextHolder(@NonNull View itemView) {
            super(itemView);
            // These IDs must match your chat_item_pdf.xml
            imagetext_message_text_view = itemView.findViewById(R.id.imagetext_message_text_view);
        }
    }
    // --- END: DEFINE SEPARATE VIEWHOLDERS ---
}

