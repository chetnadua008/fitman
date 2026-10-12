package com.example.fitman.period;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

//so the logic is - DayCellView is written once and used to render multiple cells by changing its data / variables like day flow level, periodStart, today, future correct?
public class CycleDayCellView extends View {

    private int day;               // 0 = empty cell before day 1
    private int flow;              // FlowLevel constant
    private boolean periodStart, today, future;

    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;
    private int baseTextColor = Color.BLACK;

    public CycleDayCellView(Context c) { this(c, null); }

    public CycleDayCellView(Context c, AttributeSet a) {
        super(c, a);
        dp = c.getResources().getDisplayMetrics().density;

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2.5f * dp);
        ringPaint.setColor(0xFF880E4F);            // deep magenta ring = "period start" marker

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 14, c.getResources().getDisplayMetrics()));

        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
        if (tv.resourceId != 0) baseTextColor = ContextCompat.getColor(c, tv.resourceId);
    }

    public void bind(int day, int flow, boolean periodStart, boolean today, boolean future) {
        this.day = day; this.flow = flow; this.periodStart = periodStart;
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

        if (flow > FlowLevel.NONE) {
            float fraction = Math.min(1f, flow / 4f);            // HEAVY = full size
            float r = Math.max(3 * dp, maxCircle * fraction);
            circlePaint.setColor(FlowLevel.color(flow));
            circlePaint.setAlpha(200);
            canvas.drawCircle(cx, cy, r, circlePaint);
        }
        if (periodStart) {
            canvas.drawCircle(cx, cy, ringR, ringPaint);
        }

        textPaint.setColor(today ? 0xFF1976D2 : baseTextColor);   // today = blue + bold
        textPaint.setAlpha(future ? 90 : 255);
        textPaint.setFakeBoldText(today);
        float ty = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(String.valueOf(day), cx, ty, textPaint);
    }
}
