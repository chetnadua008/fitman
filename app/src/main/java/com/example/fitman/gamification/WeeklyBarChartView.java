package com.example.fitman.gamification;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

import java.time.LocalDate;

public class WeeklyBarChartView extends View {

    private final long[] steps = new long[7];        // index 0 = Monday
    private final boolean[] earned = new boolean[7];
    private int todayIndex = -1;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;

    private static final String[] LABELS = {"M", "T", "W", "T", "F", "S", "S"};

    public WeeklyBarChartView(Context c) { this(c, null); }

    public WeeklyBarChartView(Context c, AttributeSet a) {
        super(c, a);
        dp = c.getResources().getDisplayMetrics().density;

        barPaint.setColor(0xFF26A69A);              // teal, matches calendar circles
        todayBarPaint.setColor(0xFF1976D2);         // blue, matches "today" on calendar
        trackPaint.setColor(0x1F000000);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2.5f * dp);
        ringPaint.setColor(0xFFFFB300);             // gold, matches calendar point ring

        goalLinePaint.setColor(0x552E7D32);
        goalLinePaint.setStrokeWidth(1 * dp);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(sp(c, 12));

        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(sp(c, 10));

        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
        int textColor = tv.resourceId != 0 ? ContextCompat.getColor(c, tv.resourceId) : Color.BLACK;
        labelPaint.setColor(textColor);
        valuePaint.setColor(textColor);
    }

    private float sp(Context c, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, c.getResources().getDisplayMetrics());
    }

    /** weekStart must be the Monday of the week being shown. */
    public void setWeek(LocalDate weekStart, java.util.Map<String, Long> stepsByDate,
                        java.util.Map<String, Boolean> earnedByDate) {
        LocalDate today = LocalDate.now();
        todayIndex = -1;
        for (int i = 0; i < 7; i++) {
            String key = weekStart.plusDays(i).toString();
            Long s = stepsByDate.get(key);
            steps[i] = s == null ? 0 : s;
            Boolean e = earnedByDate.get(key);
            earned[i] = Boolean.TRUE.equals(e);
            if (weekStart.plusDays(i).equals(today)) todayIndex = i;
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int width = MeasureSpec.getSize(w);
        int height = (int) (160 * dp);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float labelSpace = 34 * dp;
        float valueSpace = 16 * dp;
        float chartTop = 8 * dp;
        float chartBottom = h - labelSpace;
        float chartHeight = chartBottom - chartTop - valueSpace;

        long maxSteps = GameRules.STEPS_PER_POINT;
        for (long s : steps) maxSteps = Math.max(maxSteps, s);   // extend axis if someone beats the goal

        float goalY = chartBottom - (GameRules.STEPS_PER_POINT / (float) maxSteps) * chartHeight;
        canvas.drawLine(0, goalY, w, goalY, goalLinePaint);

        float slot = w / 7f;
        float barWidth = Math.min(28 * dp, slot * 0.5f);

        for (int i = 0; i < 7; i++) {
            float cx = slot * i + slot / 2f;
            float fraction = steps[i] / (float) maxSteps;
            float barTop = chartBottom - chartHeight * fraction;

            // track (full height, faint) so empty days are still visible
            RectF track = new RectF(cx - barWidth / 2, chartTop + valueSpace, cx + barWidth / 2, chartBottom);
            canvas.drawRoundRect(track, barWidth / 2, barWidth / 2, trackPaint);

            if (steps[i] > 0) {
                RectF bar = new RectF(cx - barWidth / 2, barTop, cx + barWidth / 2, chartBottom);
                canvas.drawRoundRect(bar, barWidth / 2, barWidth / 2, i == todayIndex ? todayBarPaint : barPaint);
            }

            if (earned[i]) {
                float ringR = barWidth / 2 + 3 * dp;
                canvas.drawCircle(cx, Math.max(barTop, chartTop + valueSpace + ringR), ringR, ringPaint);
            }

            if (steps[i] > 0) {
                String label = steps[i] >= 1000
                        ? String.format("%.1fk", steps[i] / 1000f)
                        : String.valueOf(steps[i]);
                canvas.drawText(label, cx, Math.max(barTop, chartTop + valueSpace) - 4 * dp, valuePaint);
            }

            labelPaint.setFakeBoldText(i == todayIndex);
            labelPaint.setAlpha(i == todayIndex ? 255 : 160);
            canvas.drawText(LABELS[i], cx, h - 10 * dp, labelPaint);
        }
    }
}