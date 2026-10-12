package com.example.fitman.period;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Firestore access for period/flow logging. Stores one doc per day, same shape as the steps "days" collection. */
public class CycleSync {

    private static CollectionReference cycleDays() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;
        return FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).collection("cycleDays");
    }

    /** Saves flow for a single day. Logging NONE removes the entry so queries only ever see real period days. */
    public static Task<Void> logFlow(LocalDate date, int flow) {
        CollectionReference col = cycleDays();
        if (col == null) return Tasks.forException(new IllegalStateException("Not signed in"));

        DocumentReference ref = col.document(date.toString());
        if (flow == FlowLevel.NONE) {
            return ref.delete();
        }
        Map<String, Object> d = new HashMap<>();
        d.put("date", date.toString());
        d.put("flow", flow);
        d.put("updatedAt", FieldValue.serverTimestamp());
        return ref.set(d, SetOptions.merge());
    }

    /** Flow value (0 if unlogged) for every day in [start, end], inclusive. Used to paint one visible month. */
    public static Task<Map<String, Integer>> loadRange(LocalDate start, LocalDate end) {
        CollectionReference col = cycleDays();
        if (col == null) return Tasks.forException(new IllegalStateException("Not signed in"));

        return col.whereGreaterThanOrEqualTo("date", start.toString())
                .whereLessThanOrEqualTo("date", end.toString())
                .get()
                .continueWith(task -> {
                    QuerySnapshot qs = task.getResult();
                    Map<String, Integer> map = new HashMap<>();
                    for (DocumentSnapshot d : qs) {
                        Long f = d.getLong("flow");
                        map.put(d.getId(), f == null ? FlowLevel.NONE : f.intValue());
                    }
                    return map;
                });
    }

    /** Every logged period day over a wide look-back window, for CycleRules to find the current cycle day. */
    public static Task<List<LocalDate>> loadRecentPeriodDates() {
        CollectionReference col = cycleDays();
        if (col == null) return Tasks.forException(new IllegalStateException("Not signed in"));

        String cutoff = LocalDate.now().minusMonths(12).toString();
        return col.whereGreaterThanOrEqualTo("date", cutoff)
                .get()
                .continueWith(task -> {
                    QuerySnapshot qs = task.getResult();
                    List<LocalDate> dates = new ArrayList<>();
                    for (DocumentSnapshot d : qs) {
                        Long f = d.getLong("flow");
                        if (f != null && f > 0) dates.add(LocalDate.parse(d.getId()));
                    }
                    return dates;
                });
    }
}
