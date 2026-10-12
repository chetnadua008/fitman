package com.example.fitman.cheatmeal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.fitman.cheatmeal.CheatLevel;

public class CheatDayCellView extends View {

    private int day;               // 0 = empty cell before day 1
    private int level;             // CheatLevel constant
    private boolean today, future;

    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;
    private int baseTextColor = Color.BLACK;

    public CheatDayCellView(Context c) { this(c, null); }

    public CheatDayCellView(Context c, AttributeSet a) {
        super(c, a);
        dp = c.getResources().getDisplayMetrics().density;

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 14, c.getResources().getDisplayMetrics()));

        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
        if (tv.resourceId != 0) baseTextColor = ContextCompat.getColor(c, tv.resourceId);
    }

    public void bind(int day, int level, boolean today, boolean future) {
        this.day = day; this.level = level;
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
        float maxCircle = Math.min(cx, cy) - 3 * dp;

        if (level > CheatLevel.NONE) {
            float fraction = level / (float) CheatLevel.HIGH;   // HIGH = full size
            float r = Math.max(4 * dp, maxCircle * fraction);
            circlePaint.setColor(CheatLevel.color(level));
            circlePaint.setAlpha(220);
            canvas.drawCircle(cx, cy, r, circlePaint);
        }

        textPaint.setColor(today ? 0xFF1976D2 : baseTextColor);   // today = blue + bold
        textPaint.setAlpha(future ? 90 : 255);
        textPaint.setFakeBoldText(today);
        float ty = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(String.valueOf(day), cx, ty, textPaint);
    }
}
