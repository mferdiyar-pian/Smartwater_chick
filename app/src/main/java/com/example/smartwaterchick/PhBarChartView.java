package com.example.smartwaterchick;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class PhBarChartView extends View {

    private Paint barPaint, barActivePaint, gridPaint;
    // 24 data points (hourly), current hour index = 13
    private float[] data = {
            7.1f,7.0f,7.2f,7.1f,7.3f,7.2f,7.0f,7.1f,
            7.2f,7.3f,7.2f,7.1f,7.0f,7.2f,7.3f,7.2f,
            7.1f,7.2f,7.3f,7.1f,7.2f,7.0f,7.1f,7.2f
    };
    private int activeIndex = 13; // highlighted bar
    private float minVal = 6.5f, maxVal = 8.0f;

    public PhBarChartView(Context context) { super(context); init(); }
    public PhBarChartView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#D0DCF5"));
        barPaint.setStyle(Paint.Style.FILL);

        barActivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barActivePaint.setColor(Color.parseColor("#1B5BCE"));
        barActivePaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#F0F2F8"));
        gridPaint.setStrokeWidth(1f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float padL = 8f, padR = 8f, padT = 8f, padB = 8f;
        float chartW = w - padL - padR;
        float chartH = h - padT - padB;
        float range = maxVal - minVal;

        int n = data.length;
        float slotW = chartW / n;
        float barW = slotW * 0.55f;

        for (int i = 0; i < n; i++) {
            float ratio = (data[i] - minVal) / range;
            float barH = chartH * ratio;
            float cx = padL + i * slotW + slotW / 2f;
            float left = cx - barW / 2f;
            float right = cx + barW / 2f;
            float top = padT + chartH - barH;
            float bottom = padT + chartH;

            Paint p = (i == activeIndex) ? barActivePaint : barPaint;
            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, 6f, 6f, p);
        }
    }
}