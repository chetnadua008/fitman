package com.example.fitman.challenge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitman.R;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChallengeListAdapter extends RecyclerView.Adapter<ChallengeListAdapter.VH> {

    public interface Listener {
        void onOpen(QueryDocumentSnapshot challenge);
        void onAccept(QueryDocumentSnapshot challenge);
        void onDecline(QueryDocumentSnapshot challenge);
        void onDelete(QueryDocumentSnapshot challenge);
    }

    private final List<QueryDocumentSnapshot> items = new ArrayList<>();
    private final String myUid;
    private final Listener listener;

    public ChallengeListAdapter(String myUid, Listener listener) {
        this.myUid = myUid; this.listener = listener;
    }

    public void submit(List<QueryDocumentSnapshot> newItems) {
        items.clear(); items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_challenge, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        QueryDocumentSnapshot c = items.get(position);
        String status = c.getString("status");
        String fromUid = c.getString("fromUid");
        boolean incoming = "pending".equals(status) && myUid.equals(c.getString("toUid"));
        boolean outgoingPending = "pending".equals(status) && myUid.equals(fromUid);

        String otherName = myUid.equals(fromUid) ? c.getString("toName") : c.getString("fromName");
        if (otherName == null) otherName = myUid.equals(fromUid) ? c.getString("toEmail") : c.getString("fromEmail");
        Long target = c.getLong("targetSteps");

        h.title.setText("You vs " + otherName);
        h.subtitle.setText(target + " steps/day  •  " +
                (incoming ? "Wants to challenge you" : outgoingPending ? "Waiting for them to accept" : "Active"));
        h.actions.setVisibility(incoming ? View.VISIBLE : View.GONE);
        h.btnDelete.setVisibility(View.VISIBLE);
        h.btnDelete.setOnClickListener(v -> listener.onDelete(c));
        h.itemView.setOnClickListener(v -> { if ("active".equals(status)) listener.onOpen(c); });
        h.btnAccept.setOnClickListener(v -> listener.onAccept(c));
        h.btnDecline.setOnClickListener(v -> listener.onDecline(c));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(c));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        LinearLayout actions;
        Button btnAccept, btnDecline;
        ImageButton btnDelete;
        VH(View v) {
            super(v);
            title = v.findViewById(R.id.tvTitle);
            subtitle = v.findViewById(R.id.tvSubtitle);
            actions = v.findViewById(R.id.rowActions);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnDecline = v.findViewById(R.id.btnDecline);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}