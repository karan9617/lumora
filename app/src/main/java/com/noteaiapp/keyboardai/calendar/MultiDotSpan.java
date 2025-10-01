package com.noteaiapp.keyboardai.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

public class MultiDotSpan implements LineBackgroundSpan {

    private final int noteCount;
    private final Context context;
    private final int radius = 5; // dp

    public MultiDotSpan(int noteCount, Context context) {
        this.noteCount = Math.min(noteCount, 5); // Max 5 dots to keep it clean
        this.context = context;
    }

    @Override
    public void drawBackground(
            Canvas canvas, Paint paint,
            int left, int right, int top, int baseline, int bottom,
            CharSequence charSequence,
            int start, int end, int lineNum) {

        int totalWidth = (radius * 2 * noteCount) + (noteCount - 1) * 4; // spacing between dots
        int centerX = (left + right) / 2;
        int dotY = bottom + 10; // Position below the date

        for (int i = 0; i < noteCount; i++) {
            int dotX = centerX - totalWidth / 2 + i * (radius * 2 + 4);
            paint.setColor(getColorForIndex(i));
            canvas.drawCircle(dotX, dotY, radius, paint);
        }
    }

    private int getColorForIndex(int index) {
        // You can customize color for each dot
        int[] colors = {
                Color.parseColor("#F44336"), // red
                Color.parseColor("#FF9800"), // orange
                Color.parseColor("#4CAF50"), // green
                Color.parseColor("#2196F3"), // blue
                Color.parseColor("#9C27B0")  // purple
        };
        return colors[index % colors.length];
    }
}
