package com.example.smartwaterchick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class BarChartView extends View {

    private Paint barPaint, textPaint, gridPaint, axisPaint;

    // Data volume per hari (Sen-Min) dalam Liter
    private float[] data = {80f, 60f, 100f, 75f, 120f, 150f, 180f};
    private String[] labels = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
    private float maxValue = 200f;
    private float[] yGridValues = {0f, 40f, 80f, 120f, 160f, 200f};

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#1B5BCE"));
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#8A8FA8"));
        textPaint.setTextSize(26f);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#EEF1F8"));
        gridPaint.setStrokeWidth(1.5f);
        gridPaint.setStyle(Paint.Style.STROKE);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.parseColor("#E0E4EF"));
        axisPaint.setStrokeWidth(1.5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        float padLeft  = 90f;   // untuk label Y
        float padRight = 12f;
        float padTop   = 10f;
        float padBottom= 44f;   // untuk label X

        float chartW = w - padLeft - padRight;
        float chartH = h - padTop - padBottom;

        // --- Grid lines + Y labels ---
        for (float val : yGridValues) {
            float ratio = val / maxValue;
            float y = padTop + chartH - (ratio * chartH);

            // grid line
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint);

            // Y label (kanan rata kiri chart)
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf((int) val), padLeft - 8f, y + 9f, textPaint);
        }

        // --- Bars ---
        int n = data.length;
        float slotW = chartW / n;
        float barW  = slotW * 0.48f;
        float radius = 10f;

        for (int i = 0; i < n; i++) {
            float centerX  = padLeft + i * slotW + slotW / 2f;
            float barH     = (data[i] / maxValue) * chartH;
            float left     = centerX - barW / 2f;
            float right    = centerX + barW / 2f;
            float top      = padTop + chartH - barH;
            float bottom   = padTop + chartH;

            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, radius, radius, barPaint);

            // X label
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(labels[i], centerX, h - 10f, textPaint);
        }
    }

    public void setData(float[] data, String[] labels, float maxValue) {
        this.data = data;
        this.labels = labels;
        this.maxValue = maxValue;
        invalidate();
    }
}