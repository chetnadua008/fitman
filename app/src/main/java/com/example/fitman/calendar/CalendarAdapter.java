package com.example.fitman.calendar;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.VH> {

    public static class DayData {
        public final long steps;
        public final boolean pointEarned;
        public DayData(long steps, boolean pointEarned) {
            this.steps = steps; this.pointEarned = pointEarned;
        }
    }

    public interface OnDayClick { void onDayClick(LocalDate date, DayData data); }

    private LocalDate month = LocalDate.now().withDayOfMonth(1);
    private Map<String, DayData> data = new HashMap<>();
    private final OnDayClick listener;

    public CalendarAdapter(OnDayClick listener) { this.listener = listener; }

    public void setData(LocalDate firstOfMonth, Map<String, DayData> newData) {
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
        DayCellView v = new DayCellView(parent.getContext());
        v.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DayCellView v = (DayCellView) holder.itemView;
        int day = position - blanks() + 1;

        if (day < 1) {                       // empty cell
            v.bind(0, 0, false, false, false);
            v.setOnClickListener(null);
            v.setClickable(false);
            return;
        }

        final LocalDate date = month.withDayOfMonth(day);
        final DayData d = data.get(date.toString());
        LocalDate today = LocalDate.now();

        v.bind(day, d == null ? 0 : d.steps, d != null && d.pointEarned,
                date.equals(today), date.isAfter(today));
        v.setOnClickListener(x ->
                listener.onDayClick(date, d == null ? new DayData(0, false) : d));
    }

    static class VH extends RecyclerView.ViewHolder {
        VH(DayCellView v) { super(v); }
    }
}