package com.example.fitman.weather;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class WalkAdvisor {

    public static final double MAX_GOOD_TEMP = 33.0;   // °C
    public static final int MIN_GOOD_CLOUD = 45;        // %
    public static final double MAX_GOOD_PRECIP = 1.0;   // mm per 15-min slot

    public static class Advice {
        public boolean goodRightNow;
        public WeatherSlot nextGoodSlot;   // null if none found ahead
        public String message;
    }

    private static boolean isGood(double temp, int cloud, double precip) {
        return temp <= MAX_GOOD_TEMP && cloud >= MIN_GOOD_CLOUD && precip <= MAX_GOOD_PRECIP;
    }

    public static Advice evaluate(WeatherData data) {
        Advice advice = new Advice();
        advice.goodRightNow = isGood(data.currentTemp, data.currentCloud, data.currentPrecip);

        if (advice.goodRightNow) {
            advice.message = String.format("Good time for a walk now — %.0f°C and cloudy (%d%% cover).",
                    data.currentTemp, data.currentCloud);
            return advice;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime now = LocalDateTime.parse(data.currentTime, fmt);

        for (WeatherSlot s : data.slots) {
            LocalDateTime t = LocalDateTime.parse(s.time, fmt);
            if (!t.isAfter(now)) continue;
            if (t.isAfter(now.plusHours(4))) break;   // don't look too far ahead

            if (isGood(s.temp, s.cloudCover, s.precipitation)) {
                advice.nextGoodSlot = s;
                advice.message = String.format("Next good window around %s — %.0f°C, cloudy.",
                        t.toLocalTime().toString(), s.temp);
                return advice;
            }
        }

        advice.message = "Too hot right now, no cloudy break expected soon. Try early morning or evening.";
        return advice;
    }
}