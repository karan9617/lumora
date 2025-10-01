package com.noteaiapp.keyboardai.calendar;


import com.prolificinteractive.materialcalendarview.CalendarDay;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Utility class to bridge the gap between MaterialCalendarView's CalendarDay
 * and standard Java Date objects used by SimpleDateFormat.
 */
public class DateConverter {

    /**
     * Converts a CalendarDay object into a standard java.util.Date object.
     * * IMPORTANT: CalendarDay uses 1-based months (Jan=1, Dec=12),
     * but the legacy java.util.Calendar object uses 0-based months (Jan=0, Dec=11).
     * We must subtract 1 from the CalendarDay's month value during conversion.
     *
     * @param day The CalendarDay to convert.
     * @return A java.util.Date representation of the day.
     */
    public static Date toDate(CalendarDay day) {
        // Use GregorianCalendar as a concrete implementation of Calendar
        Calendar calendar = new GregorianCalendar();

        // Set the year
        calendar.set(Calendar.YEAR, day.getYear());

        // Set the month, adjusting for 0-based index: CalendarDay.getMonth() - 1
        calendar.set(Calendar.MONTH, day.getMonth());

        // Set the day of the month
        calendar.set(Calendar.DAY_OF_MONTH, day.getDay());

        // Return the final Date object
        return calendar.getTime();
    }

    /**
     * Converts a CalendarDay directly to a formatted string using SimpleDateFormat.
     *
     * @param day The CalendarDay to format.
     * @param formatString The pattern string for SimpleDateFormat (e.g., "d MMM, yyyy").
     * @return The formatted date string.
     */
    public static String formatCalendarDay(CalendarDay day, String formatString) {
        Date date = toDate(day);
        SimpleDateFormat formatter = new SimpleDateFormat(formatString, Locale.getDefault());
        return formatter.format(date);
    }
}
