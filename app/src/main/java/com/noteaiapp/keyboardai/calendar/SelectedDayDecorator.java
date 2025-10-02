package com.noteaiapp.keyboardai.calendar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.noteaiapp.keyboardai.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

public class SelectedDayDecorator implements DayViewDecorator {

    private CalendarDay selectedDate;
    private final Drawable highlightDrawable;

    public SelectedDayDecorator(Context context) {
        // Use your own drawable (e.g., a colored circle)
        highlightDrawable = ContextCompat.getDrawable(context, R.drawable.selected_day_circle);
    }

    public void setDate(CalendarDay date) {
        this.selectedDate = date;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return selectedDate != null && day.equals(selectedDate);
    }

    @Override
    public void decorate(DayViewFacade view) {
        if (highlightDrawable != null) {
            view.setBackgroundDrawable(highlightDrawable);
            view.addSpan(new ForegroundColorSpan(Color.WHITE)); // Optional: set text color
        }
    }
}
