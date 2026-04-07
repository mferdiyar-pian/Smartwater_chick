package com.example.smartwaterchick;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class AnalisisChartView extends View {

    private Paint linePaint, fillPaint, gridPaint;
    private float[] data;

    private int lineColor = Color.parseColor("#1B5BCE");
    private int fillColorTop = Color.parseColor("#331B5BCE");
    private int fillColorBottom = Color.parseColor("#001B5BCE");

    public AnalisisChartView(Context context) {
        super(context);
        init();
    }

    public AnalisisChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStrokeWidth(3f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#F0F2F8"));
        gridPaint.setStrokeWidth(1f);
    }

    public void setData(float[] data, int color) {
        this.data = data;
        this.lineColor = color;

        this.fillColorTop = Color.argb(60, Color.red(color), Color.green(color), Color.blue(color));
        this.fillColorBottom = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color));

        linePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data == null || data.length < 2) return;

        int w = getWidth();
        int h = getHeight();
        int padding = 40;

        float chartW = w - padding * 2;
        float chartH = h - padding * 2;

        float min = data[0], max = data[0];
        for (float v : data) {
            if (v < min) min = v;
            if (v > max) max = v;
        }

        float range = max - min;
        if (range == 0) range = 1;

        float stepX = chartW / (data.length - 1);

        Path linePath = new Path();
        Path fillPath = new Path();

        float startX = padding;
        float startY = padding + chartH - ((data[0] - min) / range) * chartH;

        linePath.moveTo(startX, startY);
        fillPath.moveTo(startX, h - padding);
        fillPath.lineTo(startX, startY);

        for (int i = 1; i < data.length; i++) {
            float x = padding + i * stepX;
            float y = padding + chartH - ((data[i] - min) / range) * chartH;

            linePath.lineTo(x, y);
            fillPath.lineTo(x, y);
        }

        fillPath.lineTo(padding + (data.length - 1) * stepX, h - padding);
        fillPath.close();

        fillPaint.setShader(new LinearGradient(
                0, padding, 0, h,
                fillColorTop, fillColorBottom,
                Shader.TileMode.CLAMP
        ));

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }
}