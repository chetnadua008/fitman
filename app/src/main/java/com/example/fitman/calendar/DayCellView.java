package com.example.fitman.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.fitman.gamification.GameRules;

public class DayCellView extends View {

    private int day;               // 0 = empty cell before day 1
    private long steps;
    private boolean pointEarned, today, future;

    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;
    private int baseTextColor = Color.BLACK;

    public DayCellView(Context c) { this(c, null); }

    public DayCellView(Context c, AttributeSet a) {
        super(c, a);
        dp = c.getResources().getDisplayMetrics().density;

        circlePaint.setColor(0xFF26A69A);          // teal
        circlePaint.setAlpha(170);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2.5f * dp);
        ringPaint.setColor(0xFFFFB300);            // gold

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 14, c.getResources().getDisplayMetrics()));

        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
        if (tv.resourceId != 0) baseTextColor = ContextCompat.getColor(c, tv.resourceId);
    }

    public void bind(int day, long steps, boolean pointEarned, boolean today, boolean future) {
        this.day = day; this.steps = steps; this.pointEarned = pointEarned;
        this.today = today; this.future = future;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int size = MeasureSpec.getSize(w);
        setMeasuredDimension(size, size);          // square cells
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (day == 0) return;

        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float half = Math.min(cx, cy);
        float ringR = half - ringPaint.getStrokeWidth() / 2f - dp;
        float maxCircle = ringR - 3 * dp;          // leave a gap inside the ring

        if (steps > 0) {
            float fraction = Math.min(1f, steps / (float) GameRules.STEPS_PER_POINT);
            float r = Math.max(3 * dp, maxCircle * fraction);   // tiny dot minimum so it stays visible
            canvas.drawCircle(cx, cy, r, circlePaint);
        }
        if (pointEarned) {
            canvas.drawCircle(cx, cy, ringR, ringPaint);
        }

        textPaint.setColor(today ? 0xFF1976D2 : baseTextColor);   // today = blue + bold
        textPaint.setAlpha(future ? 90 : 255);
        textPaint.setFakeBoldText(today);
        float ty = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(String.valueOf(day), cx, ty, textPaint);
    }
}