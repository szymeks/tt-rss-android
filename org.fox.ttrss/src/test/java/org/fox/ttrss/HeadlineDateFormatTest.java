package org.fox.ttrss;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class HeadlineDateFormatTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone WARSAW = TimeZone.getTimeZone("Europe/Warsaw");

    // 2026-10-05 15:30:00 UTC
    private static final long NOW_MILLIS = millis(UTC, 2026, Calendar.OCTOBER, 5, 15, 30);

    private static long millis(TimeZone tz, int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance(tz, Locale.US);
        cal.clear();
        cal.set(year, month, day, hour, minute, 0);
        return cal.getTimeInMillis();
    }

    private static long seconds(TimeZone tz, int year, int month, int day, int hour, int minute) {
        return millis(tz, year, month, day, hour, minute) / 1000L;
    }

    private static String format(long updated, TimeZone tz) {
        return HeadlinesFragment.formatHeadlineDate(updated, NOW_MILLIS, Locale.US, tz);
    }

    @Test
    public void todayShowsTimeOnly() {
        assertEquals("08:05", format(seconds(UTC, 2026, Calendar.OCTOBER, 5, 8, 5), UTC));
    }

    @Test
    public void startOfTodayShowsTimeOnly() {
        assertEquals("00:00", format(seconds(UTC, 2026, Calendar.OCTOBER, 5, 0, 0), UTC));
    }

    @Test
    public void yesterdayShowsDateAndTime() {
        assertEquals("Oct 04, 23:59", format(seconds(UTC, 2026, Calendar.OCTOBER, 4, 23, 59), UTC));
    }

    @Test
    public void sameDayOfMonthInEarlierMonthShowsDateAndTime() {
        assertEquals("Sep 05, 10:00", format(seconds(UTC, 2026, Calendar.SEPTEMBER, 5, 10, 0), UTC));
    }

    @Test
    public void sameDayInPreviousYearShowsYearDateAndTime() {
        assertEquals("Oct 05 2025, 15:30", format(seconds(UTC, 2025, Calendar.OCTOBER, 5, 15, 30), UTC));
    }

    @Test
    public void withinHalfYearShowsDateAndTimeWithoutYear() {
        // 181 days before now
        long updated = NOW_MILLIS / 1000L - 181L * 24 * 60 * 60;
        assertEquals("Apr 07, 15:30", format(updated, UTC));
    }

    @Test
    public void exactlyHalfYearAgoShowsYearDateAndTime() {
        long updated = NOW_MILLIS / 1000L - 182L * 24 * 60 * 60;
        assertEquals("Apr 06 2026, 15:30", format(updated, UTC));
    }

    @Test
    public void olderThanHalfYearShowsYearDateAndTime() {
        assertEquals("Jan 15 2024, 09:07", format(seconds(UTC, 2024, Calendar.JANUARY, 15, 9, 7), UTC));
    }

    @Test
    public void usesTwentyFourHourClock() {
        assertEquals("Oct 01, 21:45", format(seconds(UTC, 2026, Calendar.OCTOBER, 1, 21, 45), UTC));
    }

    @Test
    public void todayIsDeterminedInGivenTimeZone() {
        // 2026-10-04 23:30 UTC is already 2026-10-05 01:30 in Warsaw (CEST, UTC+2)
        long updated = seconds(UTC, 2026, Calendar.OCTOBER, 4, 23, 30);

        assertEquals("01:30", format(updated, WARSAW));
        assertEquals("Oct 04, 23:30", format(updated, UTC));
    }

    @Test
    public void usesGivenLocaleForMonthNames() {
        String formatted = HeadlinesFragment.formatHeadlineDate(
                seconds(UTC, 2026, Calendar.OCTOBER, 3, 12, 0), NOW_MILLIS, Locale.GERMANY, UTC);

        assertEquals("Okt. 03, 12:00", formatted);
    }
}
