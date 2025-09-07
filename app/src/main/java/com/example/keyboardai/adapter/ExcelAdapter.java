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

public class ExcelAdapter extends RecyclerView.Adapter<ExcelAdapter.ViewHolder> {

    private final Context context;
    private final List<List<String>> data;
    private final int numColumns;

    public ExcelAdapter(Context context, List<List<String>> data, int numColumns) {
        this.context = context;
        this.data = data;
        this.numColumns = numColumns;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cell_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Calculate the row and column index from the overall position
        int rowIndex = position / (numColumns + 1);
        int colIndex = position % (numColumns + 1);

        if (colIndex == 0) {
            // This is the row number cell
            holder.editText.setText(String.valueOf(rowIndex + 1));
            holder.editText.setEnabled(false); // Make it non-editable
            holder.editText.setBackgroundResource(R.drawable.cell_background_header); // Optional: add a header background
        } else {
            // This is a data cell
            holder.editText.setEnabled(true);
            // Get the text from the data source and set it to the EditText
            holder.editText.setText(data.get(rowIndex).get(colIndex - 1));
            holder.editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Do nothing
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Update the data source as the user types
                    data.get(rowIndex).set(colIndex - 1, s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Do nothing
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return data.size() * (numColumns + 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public EditText editText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            editText = itemView.findViewById(R.id.cell_edit_text);
        }
    }
}
