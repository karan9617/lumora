package com.noteaiapp.keyboardai.calendar;

import android.content.Context;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

public class MultiNoteDayDecorator implements DayViewDecorator {

    private final CalendarDay date;
    private final int noteCount;
    private final Context context;

    public MultiNoteDayDecorator(Context context, CalendarDay date, int noteCount) {
        this.context = context;
        this.date = date;
        this.noteCount = noteCount;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.equals(date);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new MultiDotSpan(noteCount, context));
    }
}
