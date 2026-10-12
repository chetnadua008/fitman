package com.example.fitman.sync;

import android.content.Context;

import com.example.fitman.challenge.NotificationHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChallengeSync {
    private ChallengeSync() {}

    /** Call after a successful step sync. Pushes today's steps into every active challenge. */
    public static Task<Void> pushProgress(long todaySteps) {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return Tasks.forResult(null);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String today = LocalDate.now().toString();

        return db.collection("challenges")
                .whereArrayContains("participants", myUid)
                .whereEqualTo("status", "active")
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return Tasks.forResult(null);
                    List<Task<Void>> updates = new ArrayList<>();
                    for (QueryDocumentSnapshot ch : task.getResult()) {
                        updates.add(updateOneChallenge(db, ch, myUid, today, todaySteps));
                    }
                    return Tasks.whenAll(updates);
                });
    }

    @SuppressWarnings("unchecked")
    private static Task<Void> updateOneChallenge(FirebaseFirestore db, QueryDocumentSnapshot ch,
                                                 String myUid, String today, long todaySteps) {
        Long targetObj = ch.getLong("targetSteps");
        long goal = targetObj == null ? 0 : targetObj;
        String fromUid = ch.getString("fromUid");
        String otherUid = myUid.equals(fromUid) ? ch.getString("toUid") : fromUid;
        String myName = myUid.equals(fromUid) ? ch.getString("fromName") : ch.getString("toName");

        DocumentReference dayRef = ch.getReference().collection("days").document(today);

        return db.runTransaction(tx -> {
            DocumentSnapshot daySnap = tx.get(dayRef);
            Map<String, Object> steps = daySnap.exists() && daySnap.get("steps") instanceof Map
                    ? new HashMap<>((Map<String, Object>) daySnap.get("steps")) : new HashMap<>();
            Map<String, Object> done = daySnap.exists() && daySnap.get("done") instanceof Map
                    ? new HashMap<>((Map<String, Object>) daySnap.get("done")) : new HashMap<>();

            boolean alreadyDone = Boolean.TRUE.equals(done.get(myUid));
            steps.put(myUid, todaySteps);
            boolean justCompleted = !alreadyDone && goal > 0 && todaySteps >= goal;
            if (justCompleted) done.put(myUid, true);

            Map<String, Object> data = new HashMap<>();
            data.put("steps", steps);
            data.put("done", done);
            data.put("date", today);
            tx.set(dayRef, data, SetOptions.merge());
            return justCompleted;
        }).continueWithTask(t -> {
            if (t.isSuccessful() && Boolean.TRUE.equals(t.getResult())) {
                String name = myName == null ? "Your friend" : myName;
                return notify(db, otherUid, name + " completed today's challenge! \uD83C\uDF89", ch.getId());
            }
            return Tasks.forResult(null);
        });
    }

    private static Task<Void> notify(FirebaseFirestore db, String toUid, String message, String challengeId) {
        Map<String, Object> n = new HashMap<>();
        n.put("message", message);
        n.put("challengeId", challengeId);
        n.put("read", false);
        n.put("delivered", false);
        n.put("createdAt", FieldValue.serverTimestamp());
        return db.collection("users").document(toUid).collection("notifications")
                .add(n).continueWith(t -> null);
    }

    /** Shows any notifications this device hasn't displayed yet, then marks them delivered. */
    public static void deliverPending(Context context) {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) return;
        FirebaseFirestore.getInstance()
                .collection("users").document(myUid).collection("notifications")
                .whereEqualTo("delivered", false)
                .get()
                .addOnSuccessListener(qs -> {
                    for (QueryDocumentSnapshot d : qs) {
                        String msg = d.getString("message");
                        NotificationHelper.show(context, msg == null ? "Challenge update" : msg);
                        d.getReference().update("delivered", true);
                    }
                });
    }
}