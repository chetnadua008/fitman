package com.example.fitman.sync;

import com.example.fitman.gamification.GameRules;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

public class StepSync {

    public static class SyncResult {
        public long steps, totalPoints, effectivePoints, weeklyPoints;
    }

    private static long num(DocumentSnapshot s, String field) {
        Long v = s.getLong(field);
        return v == null ? 0 : v;
    }

    /** counter = value of TYPE_STEP_COUNTER (steps since phone boot) */
    public static Task<SyncResult> sync(final long counter) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference userRef = db.collection("users").document(user.getUid());
        final LocalDate today = LocalDate.now();
        final LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        final DocumentReference dayRef = userRef.collection("days").document(today.toString());

        return db.runTransaction(tx -> {
            DocumentSnapshot userSnap = tx.get(userRef);   // all reads first
            DocumentSnapshot daySnap = tx.get(dayRef);

            long total = 0, effective = 0, weekly = 0;
            long lastCounter = counter;          // new user: start counting from now
            LocalDate weekStart = thisMonday;

            if (userSnap.exists()) {
                total = num(userSnap, "totalPoints");
                effective = num(userSnap, "effectivePoints");
                weekly = num(userSnap, "weeklyPoints");
                lastCounter = num(userSnap, "lastCounter");
                weekStart = LocalDate.parse(userSnap.getString("weekStart"));
            }

            // 1. Close every finished week (shrink if it had zero points)
            while (weekStart.isBefore(thisMonday)) {
                effective = GameRules.applyWeeklyDecay(effective, weekly);
                weekly = 0;
                weekStart = weekStart.plusWeeks(1);
            }

            // 2. New steps since last sync
            long delta = counter - lastCounter;
            if (delta < 0) delta = counter;      // phone rebooted, counter restarted

            long steps = daySnap.exists() ? num(daySnap, "steps") : 0;
            boolean earned = daySnap.exists() && Boolean.TRUE.equals(daySnap.getBoolean("pointEarned"));
            steps += delta;
            //add delta counter to existing steps

            // 3. Award point once per day
            if (!earned && steps >= GameRules.STEPS_PER_POINT) {
                earned = true;
                total++; effective++; weekly++;
            }

            // 4. Save
            Map<String, Object> u = new HashMap<>();
            u.put("totalPoints", total);
            u.put("effectivePoints", effective);
            u.put("weeklyPoints", weekly);
            u.put("weekStart", weekStart.toString());
            u.put("lastCounter", counter);
            u.put("email", user.getEmail());
            tx.set(userRef, u, SetOptions.merge());

            Map<String, Object> d = new HashMap<>();
            d.put("steps", steps);
            d.put("pointEarned", earned);
            d.put("date", today.toString());
            d.put("updatedAt", FieldValue.serverTimestamp());
            tx.set(dayRef, d, SetOptions.merge());

            SyncResult r = new SyncResult();
            r.steps = steps; r.totalPoints = total;
            r.effectivePoints = effective; r.weeklyPoints = weekly;
            return r;
        });
    }
}