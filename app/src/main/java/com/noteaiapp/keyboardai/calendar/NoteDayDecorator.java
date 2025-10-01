package com.noteaiapp.keyboardai.calendar;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.noteaiapp.keyboardai.R;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.HashSet;
import java.util.Set;

public class NoteDayDecorator implements DayViewDecorator {

    private final Drawable markerDrawable;
    private final Set<CalendarDay> datesWithNotes;

    public NoteDayDecorator(Context context, Set<CalendarDay> datesWithNotes) {
        this.markerDrawable = ContextCompat.getDrawable(context, R.drawable.button_background_transparent); // Your custom flag or arrow drawable
        this.datesWithNotes = datesWithNotes;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return datesWithNotes.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.setSelectionDrawable(markerDrawable);
    }
}