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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.noteaiapp.keyboardai.Models.ChatMessage;
import com.noteaiapp.keyboardai.R;

import java.util.List;

// --- Make the Adapter generic for RecyclerView.ViewHolder ---
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // --- Define view type constants ---
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_GEMINI = 2;
    private static final int VIEW_TYPE_PDF = 3;

    private final List<ChatMessage> chatMessages;
    Context context;

    public ChatAdapter(List<ChatMessage> chatMessages, Context context) {
        this.chatMessages = chatMessages;
        this.context = context;
    }

    // --- getItemViewType is now correct ---
    @Override
    public int getItemViewType(int position) {
        String type = chatMessages.get(position).getType();
        if ("pdf".equalsIgnoreCase(type)) {
            return VIEW_TYPE_PDF;
        } else if (chatMessages.get(position).isUser()) {
            return VIEW_TYPE_USER;
        } else {
            return VIEW_TYPE_GEMINI;
        }
    }

    // --- onCreateViewHolder now returns the correct ViewHolder for each type ---
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_PDF) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_pdf, parent, false);
            return new PdfViewHolder(view); // Return a PdfViewHolder
        } else if (viewType == VIEW_TYPE_USER) { // Handles both USER and GEMINI text types
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
                // --- END: ADD CLICK LISTENERS ---

                break;
        }
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

        public GeminiViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.chat_message_text);
            copyButton = itemView.findViewById(R.id.copyMessageButton);
            shareButton = itemView.findViewById(R.id.shareButton);
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
    // --- END: DEFINE SEPARATE VIEWHOLDERS ---
}

