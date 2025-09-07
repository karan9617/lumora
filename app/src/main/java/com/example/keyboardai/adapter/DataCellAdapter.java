package com.example.keyboardai.adapter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.keyboardai.R;
import java.util.List;

public class DataCellAdapter extends RecyclerView.Adapter<DataCellAdapter.CellViewHolder> {

    private final List<List<String>> data;
    private final int numColumns;
    private final int cellWidth;
    private final int cellHeight;

    public DataCellAdapter(Context context, List<List<String>> data, int numColumns, int cellWidth, int cellHeight) {
        this.data = data;
        this.numColumns = numColumns;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    @NonNull
    @Override
    public CellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.data_cell, parent, false);
        return new CellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CellViewHolder holder, int position) {
        int row = position / numColumns;
        int col = position % numColumns;
        String cellData = data.get(row).get(col);

        holder.cellEditText.setText(cellData);
        // Set the dimensions dynamically here to ensure synchronization.
        holder.cellEditText.getLayoutParams().width = cellWidth;
        holder.cellEditText.getLayoutParams().height = cellHeight;
    }

    @Override
    public int getItemCount() {
        return data.size() * numColumns;
    }

    static class CellViewHolder extends RecyclerView.ViewHolder {
        final EditText cellEditText;

        CellViewHolder(@NonNull View itemView) {
            super(itemView);
            cellEditText = itemView.findViewById(R.id.cell_edit_text);
            cellEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
                @Override
                public void afterTextChanged(Editable s) {
                    // This is where you would normally update the data source with the new cell value.
                    // The logic has been omitted here as it depends on your specific data storage.
                }
            });
        }
    }
}
