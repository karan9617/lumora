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
public class AllDatesDecorator implements DayViewDecorator {

    private final Drawable drawable;

    public AllDatesDecorator(Context context) {
        drawable = ContextCompat.getDrawable(context, R.drawable.all_dates_circle);
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        Log.d("AllDatesDecorator", "Decorating: " + day); // Add this
        return true;
    }

    @Override
    public void decorate(DayViewFacade view) {
        Log.d("AllDatesDecorator", "Applying background");
        view.setBackgroundDrawable(drawable);

        view.addSpan(new ForegroundColorSpan(Color.WHITE)); // Optional: text color
    }
}
