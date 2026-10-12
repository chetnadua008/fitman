package com.example.fitman.cheatmeal;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitman.cheatmeal.CheatDayCellView;
import com.example.fitman.cheatmeal.CheatLevel;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class CheatAdapter extends RecyclerView.Adapter<CheatAdapter.VH> {

    public interface OnDayClick { void onDayClick(LocalDate date, CheatSync.Entry entry); }

    private LocalDate month = LocalDate.now().withDayOfMonth(1);
    private Map<String, CheatSync.Entry> data = new HashMap<>();
    private final OnDayClick listener;

    public CheatAdapter(OnDayClick listener) { this.listener = listener; }

    public void setData(LocalDate firstOfMonth, Map<String, CheatSync.Entry> newData) {
        month = firstOfMonth;
        data = newData;
        notifyDataSetChanged();
    }

    private int blanks() { return month.getDayOfWeek().getValue() - 1; }   // Monday = 1

    @Override
    public int getItemCount() { return blanks() + month.lengthOfMonth(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CheatDayCellView v = new CheatDayCellView(parent.getContext());
        v.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CheatDayCellView v = (CheatDayCellView) holder.itemView;
        int day = position - blanks() + 1;

        if (day < 1) {                       // empty cell before the 1st
            v.bind(0, CheatLevel.NONE, false, false);
            v.setOnClickListener(null);
            v.setClickable(false);
            return;
        }

        final LocalDate date = month.withDayOfMonth(day);
        final CheatSync.Entry e = data.get(date.toString());
        LocalDate today = LocalDate.now();

        v.bind(day, e == null ? CheatLevel.NONE : e.level, date.equals(today), date.isAfter(today));
        v.setOnClickListener(x ->
                listener.onDayClick(date, e == null ? new CheatSync.Entry(CheatLevel.NONE, "") : e));
    }

    static class VH extends RecyclerView.ViewHolder {
        VH(CheatDayCellView v) { super(v); }
    }
}
