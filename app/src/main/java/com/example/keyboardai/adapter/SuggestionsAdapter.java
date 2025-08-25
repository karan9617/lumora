package com.example.keyboardai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.keyboardai.R;
import java.util.ArrayList;
import java.util.List;

public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.SuggestionViewHolder> {
    private List<String> suggestions = new ArrayList<>();
    private OnSuggestionClickListener listener;

    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);
    }

    public SuggestionsAdapter(OnSuggestionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_suggestion, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        String suggestion = suggestions.get(position);
        holder.suggestionText.setText(suggestion);
        holder.itemView.setOnClickListener(v -> listener.onSuggestionClick(suggestion));
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public void setSuggestions(List<String> newSuggestions) {
        this.suggestions.clear();
        if (newSuggestions != null) {
            this.suggestions.addAll(newSuggestions);
        }
        notifyDataSetChanged();
    }

    public static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        TextView suggestionText;

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            suggestionText = itemView.findViewById(R.id.suggestionTextView);
        }
    }
}
