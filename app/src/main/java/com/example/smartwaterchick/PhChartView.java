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

public class PhChartView extends View {

    private Paint linePaint, fillPaint, gridPaint, dotPaint;
    // Sample pH data points
    private float[] phData = {6.8f, 7.0f, 6.5f, 7.5f, 7.8f, 7.2f, 6.9f, 7.3f, 7.6f, 7.2f};
    private float minPh = 5.0f, maxPh = 9.0f;

    public PhChartView(Context context) {
        super(context);
        init();
    }

    public PhChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#1B5BCE"));
        linePaint.setStrokeWidth(3f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#E8EAF0"));
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.parseColor("#1B5BCE"));
        dotPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        int padLeft = 10, padRight = 10, padTop = 16, padBottom = 24;

        float chartW = w - padLeft - padRight;
        float chartH = h - padTop - padBottom;

        // Draw horizontal grid lines (3 lines)
        for (int i = 0; i <= 3; i++) {
            float y = padTop + (chartH / 3f) * i;
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint);
        }

        if (phData == null || phData.length < 2) return;

        float stepX = chartW / (phData.length - 1);

        // Build smooth curve path using cubic bezier
        Path linePath = new Path();
        Path fillPath = new Path();

        float[] xPoints = new float[phData.length];
        float[] yPoints = new float[phData.length];

        for (int i = 0; i < phData.length; i++) {
            xPoints[i] = padLeft + i * stepX;
            yPoints[i] = padTop + chartH - ((phData[i] - minPh) / (maxPh - minPh)) * chartH;
        }

        linePath.moveTo(xPoints[0], yPoints[0]);
        fillPath.moveTo(xPoints[0], h - padBottom);
        fillPath.lineTo(xPoints[0], yPoints[0]);

        for (int i = 0; i < phData.length - 1; i++) {
            float cx1 = xPoints[i] + stepX / 3f;
            float cy1 = yPoints[i];
            float cx2 = xPoints[i + 1] - stepX / 3f;
            float cy2 = yPoints[i + 1];
            linePath.cubicTo(cx1, cy1, cx2, cy2, xPoints[i + 1], yPoints[i + 1]);
            fillPath.cubicTo(cx1, cy1, cx2, cy2, xPoints[i + 1], yPoints[i + 1]);
        }

        fillPath.lineTo(xPoints[phData.length - 1], h - padBottom);
        fillPath.close();

        // Fill gradient
        fillPaint.setShader(new LinearGradient(
                0, padTop, 0, h,
                Color.parseColor("#331B5BCE"),
                Color.parseColor("#001B5BCE"),
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fillPath, fillPaint);

        // Draw line
        canvas.drawPath(linePath, linePaint);

        // Draw dot at last point
        int last = phData.length - 1;
        canvas.drawCircle(xPoints[last], yPoints[last], 6f, dotPaint);
        Paint dotInner = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotInner.setColor(Color.WHITE);
        dotInner.setStyle(Paint.Style.FILL);
        canvas.drawCircle(xPoints[last], yPoints[last], 3f, dotInner);
    }

    public void setPhData(float[] data) {
        this.phData = data;
        invalidate();
    }
}