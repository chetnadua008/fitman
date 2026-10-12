package com.example.fitman.cheatmeal;

import com.example.fitman.cheatmeal.CheatLevel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Firestore access for cheat meals. One doc per day at users/{uid}/cheatDays/{yyyy-MM-dd}. */
public class CheatSync {

    public static class Entry {
        public final int level;      // CheatLevel constant
        public final String meal;    // what was eaten (may be empty)
        public Entry(int level, String meal) {
            this.level = level;
            this.meal = meal == null ? "" : meal;
        }
    }

    private static CollectionReference cheatDays() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;
        return FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).collection("cheatDays");
    }

    /** Saves a cheat meal. Level NONE deletes the entry so only real cheat days are stored. */
    public static Task<Void> logCheat(LocalDate date, int level, String meal) {
        CollectionReference col = cheatDays();
        if (col == null) return Tasks.forException(new IllegalStateException("Not signed in"));

        DocumentReference ref = col.document(date.toString());
        if (level == CheatLevel.NONE) {
            return ref.delete();
        }
        Map<String, Object> d = new HashMap<>();
        d.put("date", date.toString());
        d.put("level", level);
        d.put("meal", meal == null ? "" : meal);
        d.put("updatedAt", FieldValue.serverTimestamp());
        return ref.set(d, SetOptions.merge());
    }

    /** All logged cheat meals in [start, end], keyed by date string. Paints one visible month. */
    public static Task<Map<String, Entry>> loadRange(LocalDate start, LocalDate end) {
        CollectionReference col = cheatDays();
        if (col == null) return Tasks.forException(new IllegalStateException("Not signed in"));

        return col.whereGreaterThanOrEqualTo("date", start.toString())
                .whereLessThanOrEqualTo("date", end.toString())
                .get()
                .continueWith(task -> {
                    QuerySnapshot qs = task.getResult();
                    Map<String, Entry> map = new HashMap<>();
                    for (DocumentSnapshot d : qs) {
                        Long l = d.getLong("level");
                        map.put(d.getId(), new Entry(l == null ? CheatLevel.NONE : l.intValue(),
                                d.getString("meal")));
                    }
                    return map;
                });
    }

    /** Most recent cheat day on or before today within the last 90 days, or null if none. */
    public static Task<LocalDate> loadLastCheatDate() {
        CollectionReference col = cheatDays();
        if (col == null) return Tasks.forException(new IllegalStateException("Not signed in"));

        final LocalDate today = LocalDate.now();
        return col.whereGreaterThanOrEqualTo("date", today.minusDays(90).toString())
                .get()
                .continueWith(task -> {
                    QuerySnapshot qs = task.getResult();
                    LocalDate last = null;
                    for (DocumentSnapshot d : qs) {
                        Long l = d.getLong("level");
                        if (l == null || l <= 0) continue;
                        LocalDate date = LocalDate.parse(d.getId());
                        if (date.isAfter(today)) continue;
                        if (last == null || date.isAfter(last)) last = date;
                    }
                    return last;
                });
    }
}
