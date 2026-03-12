package com.example.smartwaterchick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class AnalisisChartView extends View {

    private Paint linePaint, fillPaint, gridPaint;
    private float[] data;
    private int lineColor = Color.parseColor("#1B5BCE");
    private int fillColorTop = Color.parseColor("#331B5BCE");
    private int fillColorBottom = Color.parseColor("#001B5BCE");

    // Default data: suhu tren
    private float[] defaultSuhuData = {27f, 27.5f, 28f, 27.8f, 29f, 29.5f, 28.5f, 29.2f, 30f, 29.8f, 30.5f, 31f};
    private float[] defaultAmoniaData = {10f, 11f, 12f, 11.5f, 13f, 14f, 15f, 16f, 18f, 19f, 20f, 22f};

    public AnalisisChartView(Context context) {
        super(context);
        init();
        data = defaultSuhuData;
    }

    public AnalisisChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        data = defaultSuhuData;
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(lineColor);
        linePaint.setStrokeWidth(2.5f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#F0F2F8"));
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);
    }

    public void setData(float[] data, int color) {
        this.data = data;
        this.lineColor = color;
        this.fillColorTop = Color.argb(50, Color.red(color), Color.green(color), Color.blue(color));
        this.fillColorBottom = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color));
        linePaint.setColor(color);
        invalidate();
    }

    public void setAmoniaMode() {
        data = defaultAmoniaData;
        lineColor = Color.parseColor("#F39C12");
        fillColorTop = Color.parseColor("#33F39C12");
        fillColorBottom = Color.parseColor("#00F39C12");
        linePaint.setColor(lineColor);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data == null || data.length < 2) return;

        int w = getWidth();
        int h = getHeight();
        int padLeft = 8, padRight = 8, padTop = 10, padBottom = 10;

        float chartW = w - padLeft - padRight;
        float chartH = h - padTop - padBottom;

        // Find min/max
        float min = data[0], max = data[0];
        for (float v : data) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        float range = max - min;
        if (range == 0) range = 1;

        // Draw grid lines
        for (int i = 0; i <= 3; i++) {
            float y = padTop + (chartH / 3f) * i;
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint);
        }

        float stepX = chartW / (data.length - 1);

        float[] xPts = new float[data.length];
        float[] yPts = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            xPts[i] = padLeft + i * stepX;
            yPts[i] = padTop + chartH - ((data[i] - min) / range) * chartH;
        }

        // Build smooth path
        Path linePath = new Path();
        Path fillPath = new Path();

        linePath.moveTo(xPts[0], yPts[0]);
        fillPath.moveTo(xPts[0], h - padBottom);
        fillPath.lineTo(xPts[0], yPts[0]);

        for (int i = 0; i < data.length - 1; i++) {
            float cx1 = xPts[i] + stepX / 3f;
            float cy1 = yPts[i];
            float cx2 = xPts[i + 1] - stepX / 3f;
            float cy2 = yPts[i + 1];
            linePath.cubicTo(cx1, cy1, cx2, cy2, xPts[i + 1], yPts[i + 1]);
            fillPath.cubicTo(cx1, cy1, cx2, cy2, xPts[i + 1], yPts[i + 1]);
        }

        fillPath.lineTo(xPts[data.length - 1], h - padBottom);
        fillPath.close();

        // Draw fill
        fillPaint.setShader(new LinearGradient(
                0, padTop, 0, h,
                fillColorTop, fillColorBottom,
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fillPath, fillPaint);

        // Draw line
        canvas.drawPath(linePath, linePaint);

        // Dot at last point
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(lineColor);
        dot.setStyle(Paint.Style.FILL);
        canvas.drawCircle(xPts[data.length - 1], yPts[data.length - 1], 5f, dot);
        dot.setColor(Color.WHITE);
        canvas.drawCircle(xPts[data.length - 1], yPts[data.length - 1], 2.5f, dot);
    }
}