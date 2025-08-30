package com.example.keyboardai.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.keyboardai.R;

import java.util.List;

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {

    private final List<Integer> colors;
    private final OnColorSelectedListener listener;
    private int selectedColor;

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    public ColorAdapter(List<Integer> colors, OnColorSelectedListener listener) {
        this.colors = colors;
        this.listener = listener;
        this.selectedColor = colors.get(0); // Set default color
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_color, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        int color = colors.get(position);
        holder.colorCircle.setColorFilter(color);

        // Highlight the selected color
        if (color == selectedColor) {
            holder.selectionRing.setVisibility(View.VISIBLE);
        } else {
            holder.selectionRing.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldSelectedColor = selectedColor;
            selectedColor = color;
            listener.onColorSelected(selectedColor);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    public static class ColorViewHolder extends RecyclerView.ViewHolder {
        ImageView colorCircle;
        ImageView selectionRing;

        public ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            colorCircle = itemView.findViewById(R.id.color_circle);
            selectionRing = itemView.findViewById(R.id.selection_ring);
        }
    }
}
