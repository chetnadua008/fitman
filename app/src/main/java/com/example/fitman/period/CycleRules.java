package com.example.fitman.period;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Turns a set of logged period days into "what cycle day is it today".
 * Pure logic (no Firebase/Android), so it's easy to unit test.
 */
public final class CycleRules {

    public static class CycleInfo {
        public final int cycleDay;            // 1-based; -1 if nothing has ever been logged
        public final boolean onPeriod;        // true if today falls inside the most recent bleed streak
        public final int periodDay;           // 1-based day of the current bleed streak, 0 if not bleeding
        public final LocalDate lastPeriodStart;
        public final Integer avgCycleLengthDays; // null if not enough history to estimate

        CycleInfo(int cycleDay, boolean onPeriod, int periodDay,
                  LocalDate lastPeriodStart, Integer avgCycleLengthDays) {
            this.cycleDay = cycleDay;
            this.onPeriod = onPeriod;
            this.periodDay = periodDay;
            this.lastPeriodStart = lastPeriodStart;
            this.avgCycleLengthDays = avgCycleLengthDays;
        }
    }

    /**
     * @param periodDates every date (any order, duplicates OK) that has flow > NONE logged.
     * @param today       usually LocalDate.now(), passed in so this stays testable.
     */
    public static CycleInfo compute(List<LocalDate> periodDates, LocalDate today) {
        if (periodDates == null || periodDates.isEmpty()) {
            return new CycleInfo(-1, false, 0, null, null);
        }

        List<LocalDate> sorted = new ArrayList<>(new TreeSet<>(periodDates)); // ascending, de-duped

        // Collapse consecutive-day runs into periods: each run's first day is a "period start".
        List<LocalDate> starts = new ArrayList<>();
        List<LocalDate> ends = new ArrayList<>();
        LocalDate runStart = sorted.get(0);
        LocalDate prev = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            LocalDate d = sorted.get(i);
            if (!d.equals(prev.plusDays(1))) {
                starts.add(runStart);
                ends.add(prev);
                runStart = d;
            }
            prev = d;
        }
        starts.add(runStart);
        ends.add(prev);

        // The active cycle is anchored to the most recent period that has already started.
        LocalDate lastStart = null, lastEnd = null;
        for (int i = 0; i < starts.size(); i++) {
            if (!starts.get(i).isAfter(today)) {
                lastStart = starts.get(i);
                lastEnd = ends.get(i);
            }
        }
        if (lastStart == null) {
            return new CycleInfo(-1, false, 0, null, null);
        }

        int cycleDay = (int) ChronoUnit.DAYS.between(lastStart, today) + 1;
        boolean onPeriod = !today.isBefore(lastStart) && !today.isAfter(lastEnd);
        int periodDay = onPeriod ? cycleDay : 0;

        Integer avg = null;
        long totalGap = 0;
        int count = 0;
        for (int i = 1; i < starts.size(); i++) {
            long gap = ChronoUnit.DAYS.between(starts.get(i - 1), starts.get(i));
            if (gap >= 15 && gap <= 60) { // sanity window, ignores logging gaps/typos
                totalGap += gap;
                count++;
            }
        }
        if (count > 0) avg = (int) Math.round(totalGap / (double) count);

        return new CycleInfo(cycleDay, onPeriod, periodDay, lastStart, avg);
    }
}
