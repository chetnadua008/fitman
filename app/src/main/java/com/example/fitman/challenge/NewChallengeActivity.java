package com.example.fitman.challenge;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fitman.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class NewChallengeActivity extends AppCompatActivity {

    private EditText etEmail, etTarget;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_challenge);
        etEmail = findViewById(R.id.etEmail);
        etTarget = findViewById(R.id.etTarget);
        progress = findViewById(R.id.progress);
        findViewById(R.id.btnSend).setOnClickListener(v -> submit());
    }

    private void submit() {
        String email = etEmail.getText().toString().trim().toLowerCase();
        String targetStr = etTarget.getText().toString().trim();
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) { finish(); return; }
        if (email.isEmpty()) { etEmail.setError("Enter an email"); return; }
        if (email.equalsIgnoreCase(me.getEmail())) { etEmail.setError("That's your own email"); return; }

        long target;
        try {
            target = Long.parseLong(targetStr);
            if (target <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            etTarget.setError("Enter a valid step target"); return;
        }

        setLoading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("userDirectory").document(email).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        setLoading(false);
                        Toast.makeText(this, "No FitMan user with that email yet. Ask them to open the app once.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    createChallenge(db, me, doc, target);
                })
                .addOnFailureListener(e -> { setLoading(false); Toast.makeText(this, "Lookup failed", Toast.LENGTH_SHORT).show(); });
    }

    private void createChallenge(FirebaseFirestore db, FirebaseUser me, DocumentSnapshot friendDoc, long target) {
        String friendUid = friendDoc.getString("uid");
        String friendName = friendDoc.getString("name");
        String friendEmail = friendDoc.getString("email");

        Map<String, Object> challenge = new HashMap<>();
        challenge.put("participants", Arrays.asList(me.getUid(), friendUid));
        challenge.put("fromUid", me.getUid());
        challenge.put("fromName", me.getDisplayName());
        challenge.put("fromEmail", me.getEmail());
        challenge.put("toUid", friendUid);
        challenge.put("toName", friendName);
        challenge.put("toEmail", friendEmail);
        challenge.put("targetSteps", target);
        challenge.put("status", "pending");
        challenge.put("createdAt", FieldValue.serverTimestamp());

        db.collection("challenges").add(challenge)
                .addOnSuccessListener(ref -> {
                    Map<String, Object> n = new HashMap<>();
                    n.put("message", (me.getDisplayName() == null ? "Someone" : me.getDisplayName())
                            + " challenged you to " + target + " steps/day!");
                    n.put("challengeId", ref.getId());
                    n.put("read", false);
                    n.put("delivered", false);
                    n.put("createdAt", FieldValue.serverTimestamp());
                    db.collection("users").document(friendUid).collection("notifications").add(n);

                    setLoading(false);
                    Toast.makeText(this, "Challenge sent!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> { setLoading(false); Toast.makeText(this, "Could not send challenge", Toast.LENGTH_SHORT).show(); });
    }

    private void setLoading(boolean on) { progress.setVisibility(on ? View.VISIBLE : View.GONE); }
}