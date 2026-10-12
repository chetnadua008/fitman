package com.example.fitman.period;

/** Period flow intensity for a single day. Stored in Firestore as a plain int. */
public final class FlowLevel {
    public static final int NONE = 0;
    public static final int SPOTTING = 1;
    public static final int LIGHT = 2;
    public static final int MEDIUM = 3;
    public static final int HEAVY = 4;

    public static final String[] LABELS = {"None", "Spotting", "Light", "Medium", "Heavy"};

    public static String label(int level) {
        if (level < 0 || level >= LABELS.length) return LABELS[NONE];
        return LABELS[level];
    }

    /** Deeper / more saturated red as flow increases. NONE draws nothing. */
    public static int color(int level) {
        switch (level) {
            case HEAVY:    return 0xFFB71C1C;
            case MEDIUM:   return 0xFFE53935;
            case LIGHT:    return 0xFFEF5350;
            case SPOTTING: return 0xFFF8BBD0;
            default:       return 0x00000000;
        }
    }
}
