package com.noteaiapp.keyboardai.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.noteaiapp.keyboardai.R;

public class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder> {

    public static final int TYPE_ROW = 0;
    public static final int TYPE_COLUMN = 1;
    private int count;
    private final int type;
    private final int cellWidth;
    private final int cellHeight;

    public HeaderAdapter(int count, int type, int cellWidth, int cellHeight) {
        this.count = count;
        this.type = type;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    @NonNull
    @Override
    public HeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_cell, parent, false);
        return new HeaderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HeaderViewHolder holder, int position) {
        String headerText;
        if (type == TYPE_ROW) {
            headerText = String.valueOf(position + 1);
            // Set height for row headers to match the data cells.
            holder.headerTextView.getLayoutParams().height = cellHeight;
        } else { // TYPE_COLUMN
            headerText = getColumnHeader(position);
            // Set width for column headers to match the data cells.
            holder.headerTextView.getLayoutParams().width = cellWidth;
        }
        holder.headerTextView.setText(headerText);
    }

    @Override
    public int getItemCount() {
        return count;
    }

    public void updateRowCount(int newCount) {
        this.count = newCount;
        notifyDataSetChanged();
    }

    /**
     * Converts a zero-based index to an Excel-style column header (A, B, C, ..., AA, AB, ...).
     * @param index The zero-based column index.
     * @return The corresponding Excel column header.
     */
    private String getColumnHeader(int index) {
        StringBuilder sb = new StringBuilder();
        while (index >= 0) {
            sb.insert(0, (char) ('A' + (index % 26)));
            index = (index / 26) - 1;
        }
        return sb.toString();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView headerTextView;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTextView = itemView.findViewById(R.id.header_text_view);
        }
    }
}
