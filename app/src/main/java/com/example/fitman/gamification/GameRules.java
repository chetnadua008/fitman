package com.example.fitman.gamification;

public final class GameRules {
    public static final int STEPS_PER_POINT = 10000;   // change to 10 while testing
    public static final int SMALL = 0, MEDIUM = 1, LARGE = 2;

    public static int sizeFor(long effectivePoints) {
        if (effectivePoints >= 15) return LARGE;
        if (effectivePoints >= 10) return MEDIUM;
        return SMALL;
    }

    // Called once for every completed week
    public static long applyWeeklyDecay(long effectivePoints, long weeklyPoints) {
        if (weeklyPoints > 0) return effectivePoints;
        switch (sizeFor(effectivePoints)) {
            case LARGE:  return 14;   // drops to top of medium
            case MEDIUM: return 9;    // drops to top of small
            default:     return effectivePoints;
        }
    }
}