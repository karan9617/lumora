package com.noteaiapp.keyboardai.calendar;

import android.graphics.Color;
import android.text.style.ForegroundColorSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.Calendar;

public class OutOfMonthDecorator implements DayViewDecorator {

    private final int currentMonth;
    private final int currentYear;

    public OutOfMonthDecorator(CalendarDay today) {
        this.currentMonth = today.getMonth();
        this.currentYear = today.getYear();
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.getMonth() != currentMonth || day.getYear() != currentYear;
    }

    @Override
    public void decorate(DayViewFacade view) {
        // Apply light gray color to text of out-of-month days
        view.addSpan(new ForegroundColorSpan(Color.parseColor("#6A6A6A")));
    }
}

