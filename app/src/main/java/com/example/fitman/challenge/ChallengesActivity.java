package com.example.fitman.challenge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitman.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChallengesActivity extends AppCompatActivity {

    private ChallengeListAdapter adapter;
    private TextView tvEmpty;
    private View progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenges);

        String myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) { finish(); return; }

        tvEmpty = findViewById(R.id.tvEmpty);
        progress = findViewById(R.id.progress);
        RecyclerView rv = findViewById(R.id.rvChallenges);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChallengeListAdapter(myUid, new ChallengeListAdapter.Listener() {
            @Override public void onOpen(QueryDocumentSnapshot c) {
                Intent i = new Intent(ChallengesActivity.this, ChallengeDetailActivity.class);
                i.putExtra("challengeId", c.getId());
                startActivity(i);
            }
            @Override public void onAccept(QueryDocumentSnapshot c) {
                c.getReference().update("status", "active").addOnSuccessListener(v -> load());
            }
            @Override public void onDecline(QueryDocumentSnapshot c) {
                c.getReference().update("status", "declined").addOnSuccessListener(v -> load());
            }
            @Override
            public void onDelete(QueryDocumentSnapshot c) {
                new androidx.appcompat.app.AlertDialog.Builder(ChallengesActivity.this)
                        .setTitle("Delete challenge?")
                        .setMessage("This will permanently delete this challenge and its progress.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete", (dialog, which) -> deleteChallenge(c))
                        .show();
            }
        });
        rv.setAdapter(adapter);

        findViewById(R.id.btnNew).setOnClickListener(v -> startActivity(new Intent(this, NewChallengeActivity.class)));
    }

    @Override
    protected void onResume() { super.onResume(); load(); }

    //load all challenges from database
    private void load() {
        String myUid = FirebaseAuth.getInstance().getUid();
        progress.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("challenges")
                .whereArrayContains("participants", myUid)
                .get()
                .addOnSuccessListener(qs -> {
                    List<QueryDocumentSnapshot> list = new ArrayList<>();
                    for (QueryDocumentSnapshot d : qs) {
                        if (!"declined".equals(d.getString("status"))) list.add(d);
                    }

                    //save challenges to adapter items list
                    adapter.submit(list);
                    tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    progress.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
            Log.e("Challenges", "load failed", e);
            progress.setVisibility(View.GONE);
            Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
    private void deleteChallenge(QueryDocumentSnapshot challenge) {

        progress.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        challenge.getReference()
                .collection("days")
                .get()
                .addOnSuccessListener(days -> {

                    com.google.firebase.firestore.WriteBatch batch =
                            db.batch();

                    for (QueryDocumentSnapshot day : days) {
                        batch.delete(day.getReference());
                    }

                    batch.delete(challenge.getReference());

                    batch.commit()
                            .addOnSuccessListener(v -> {
                                progress.setVisibility(View.GONE);

                                Toast.makeText(
                                        this,
                                        "Challenge deleted",
                                        Toast.LENGTH_SHORT
                                ).show();

                                load();
                            })
                            .addOnFailureListener(e -> {
                                progress.setVisibility(View.GONE);

                                Toast.makeText(
                                        this,
                                        "Could not delete challenge",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);

                    Toast.makeText(
                            this,
                            "Could not delete challenge",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }
}