package com.example.fitman.challenge;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitman.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.time.LocalDate;
import java.util.Map;

public class ChallengeDetailActivity extends AppCompatActivity {

    private ListenerRegistration registration;
    private String myUid, otherUid, myName = "You", otherName = "Friend";
    private long target = 1;

    private TextView tvTitle, tvTarget, tvMe, tvFriend, tvFriendLabel, tvStatus;
    private ProgressBar barMe, barFriend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge_detail);

        myUid = FirebaseAuth.getInstance().getUid();
        String challengeId = getIntent().getStringExtra("challengeId");
        if (myUid == null || challengeId == null) { finish(); return; }

        tvTitle = findViewById(R.id.tvTitle);
        tvTarget = findViewById(R.id.tvTarget);
        tvMe = findViewById(R.id.tvMe);
        tvFriend = findViewById(R.id.tvFriend);
        tvFriendLabel = findViewById(R.id.tvFriendLabel);
        tvStatus = findViewById(R.id.tvStatus);
        barMe = findViewById(R.id.barMe);
        barFriend = findViewById(R.id.barFriend);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("challenges").document(challengeId).get().addOnSuccessListener(doc -> {
            String fromUid = doc.getString("fromUid");
            Long t = doc.getLong("targetSteps");
            target = t == null ? 1 : t;
            if (myUid.equals(fromUid)) {
                otherUid = doc.getString("toUid");
                otherName = safe(doc.getString("toName"), doc.getString("toEmail"));
                myName = safe(doc.getString("fromName"), "You");
            } else {
                otherUid = fromUid;
                otherName = safe(doc.getString("fromName"), doc.getString("fromEmail"));
                myName = safe(doc.getString("toName"), "You");
            }

            tvTitle.setText(myName + " vs " + otherName);
            tvTarget.setText("Goal: " + target + " steps today");
            tvFriendLabel.setText(otherName);
            barMe.setMax((int) target);
            barFriend.setMax((int) target);

            String today = LocalDate.now().toString();
            registration = doc.getReference().collection("days").document(today)
                    .addSnapshotListener((snap, err) -> {
                        if (snap == null || !snap.exists()) return;
                        updateBars(snap);
                    });
        });
    }

    private String safe(String primary, String fallback) {
        return primary != null ? primary : (fallback != null ? fallback : "Friend");
    }

    @SuppressWarnings("unchecked")
    private void updateBars(DocumentSnapshot snap) {
        Map<String, Object> steps = (Map<String, Object>) snap.get("steps");
        long mySteps = steps != null && steps.get(myUid) != null ? ((Number) steps.get(myUid)).longValue() : 0;
        long otherSteps = steps != null && steps.get(otherUid) != null ? ((Number) steps.get(otherUid)).longValue() : 0;

        barMe.setProgress((int) Math.min(mySteps, target));
        barFriend.setProgress((int) Math.min(otherSteps, target));
        tvMe.setText(mySteps + " / " + target);
        tvFriend.setText(otherSteps + " / " + target);

        if (mySteps >= target && otherSteps >= target) tvStatus.setText("You both hit today's goal! \uD83C\uDF89");
        else if (mySteps >= target) tvStatus.setText("You're done for today. Waiting on " + otherName + ".");
        else if (otherSteps >= target) tvStatus.setText(otherName + " finished. Your turn!");
        else tvStatus.setText("Keep walking — neither of you has hit today's goal yet.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}