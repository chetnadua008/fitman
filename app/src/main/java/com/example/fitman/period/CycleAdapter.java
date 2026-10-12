package com.example.fitman.period;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class CycleAdapter extends RecyclerView.Adapter<CycleAdapter.VH> {

    public static class DayData {
        public final int flow;             // FlowLevel constant
        public final boolean periodStart;  // first day of a bleed streak
        public DayData(int flow, boolean periodStart) {
            this.flow = flow; this.periodStart = periodStart;
        }
    }

    public interface OnDayClick { void onDayClick(LocalDate date, DayData data); }

    private LocalDate month = LocalDate.now().withDayOfMonth(1);
    private Map<String, DayData> data = new HashMap<>();
    private final OnDayClick listener;

    public CycleAdapter(OnDayClick listener) { this.listener = listener; }

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
        CycleDayCellView v = new CycleDayCellView(parent.getContext());
        v.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CycleDayCellView v = (CycleDayCellView) holder.itemView;
        int day = position - blanks() + 1;

        if (day < 1) {                       // empty cell
            v.bind(0, FlowLevel.NONE, false, false, false);
            v.setOnClickListener(null);
            v.setClickable(false);
            return;
        }

        final LocalDate date = month.withDayOfMonth(day);
        final DayData d = data.get(date.toString());
        LocalDate today = LocalDate.now();

        v.bind(day, d == null ? FlowLevel.NONE : d.flow, d != null && d.periodStart,
                date.equals(today), date.isAfter(today));
        v.setOnClickListener(x ->
                listener.onDayClick(date, d == null ? new DayData(FlowLevel.NONE, false) : d));
    }

    static class VH extends RecyclerView.ViewHolder {
        VH(CycleDayCellView v) { super(v); }
    }
}
