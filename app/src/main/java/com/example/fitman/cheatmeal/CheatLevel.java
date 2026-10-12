package com.example.fitman.cheatmeal;

/** Cheat meal intensity for a single day. Stored in Firestore as a plain int. */
public final class CheatLevel {
    public static final int NONE = 0;
    public static final int LIGHT = 1;    // e.g. tea, a biscuit
    public static final int MEDIUM = 2;   // e.g. Maggi, fried food
    public static final int HIGH = 3;     // e.g. chocolate, sweets, full party

    public static final String[] LABELS = {"None", "Light", "Medium", "High"};

    public static String label(int level) {
        if (level < 0 || level >= LABELS.length) return LABELS[NONE];
        return LABELS[level];
    }

    /**
     * Purple/magenta shades increase with cheat-meal intensity.
     * NONE draws nothing.
     */
    public static int color(int level) {
        switch (level) {
            case HIGH:
                return 0xFFC2185B;    // Deep magenta
            case MEDIUM:
                return 0xFF9C27B0;    // Purple
            case LIGHT:
                return 0xFFE1BEE7;    // Light lavender
            default:
                return 0x00000000;    // Transparent
        }
    }
}
