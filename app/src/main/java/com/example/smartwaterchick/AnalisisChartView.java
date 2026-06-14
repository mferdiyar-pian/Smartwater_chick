package com.example.smartwaterchick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

public class AnalisisChartView extends View {

    private Paint linePaint, fillPaint, gridPaint, dotPaint, dotStrokePaint;
    private Paint valueTextPaint, xLabelPaint;
    private Paint tooltipBgPaint, tooltipTextPaint, selectedDotPaint;

    private float[] data;
    private float[] pointsX;
    private float[] pointsY;

    private int selectedIndex = -1;

    private int lineColor = Color.parseColor("#1565C0");
    private int fillColorTop = Color.parseColor("#4D90CAF9");
    private int fillColorBottom = Color.parseColor("#1090CAF9");
    private int gridColor = Color.parseColor("#E3ECF7");
    private int valueColor = Color.parseColor("#1565C0");
    private int axisColor = Color.parseColor("#9AAABF");

    public AnalisisChartView(Context context) {
        super(context);
        init();
    }

    public AnalisisChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClickable(true);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3.0f);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setColor(lineColor);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(gridColor);
        gridPaint.setStrokeWidth(0.8f);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.WHITE);

        dotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotStrokePaint.setStyle(Paint.Style.STROKE);
        dotStrokePaint.setStrokeWidth(2.0f);
        dotStrokePaint.setColor(lineColor);

        selectedDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedDotPaint.setStyle(Paint.Style.FILL);
        selectedDotPaint.setColor(lineColor);

        // Y-axis label: lebih compact
        valueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valueTextPaint.setColor(valueColor);
        valueTextPaint.setTextSize(26f);
        valueTextPaint.setFakeBoldText(true);
        valueTextPaint.setTextAlign(Paint.Align.LEFT);

        xLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xLabelPaint.setColor(axisColor);
        xLabelPaint.setTextSize(13f);
        xLabelPaint.setTextAlign(Paint.Align.CENTER);

        tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipBgPaint.setColor(Color.parseColor("#EAF3FF"));
        tooltipBgPaint.setStyle(Paint.Style.FILL);

        tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipTextPaint.setColor(Color.parseColor("#0D47A1"));
        tooltipTextPaint.setTextSize(18f);
        tooltipTextPaint.setFakeBoldText(true);
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    private java.util.List<String> labels;
    private String periodType = "monthly";
    private float adjustedMin = 6.0f;
    private float adjustedMax = 8.0f;

    public void setData(float[] data, java.util.List<String> labels, String periodType, int color) {
        this.data = data;
        this.labels = labels;
        this.periodType = periodType;
        this.lineColor = color;
        this.selectedIndex = -1;

        this.fillColorTop = Color.argb(60, Color.red(color), Color.green(color), Color.blue(color));
        this.fillColorBottom = Color.argb(8, Color.red(color), Color.green(color), Color.blue(color));

        linePaint.setColor(color);
        dotStrokePaint.setColor(color);
        selectedDotPaint.setColor(color);
        valueTextPaint.setColor(color);

        invalidate();
    }

    @Deprecated
    public void setData(float[] data, int color) {
        setData(data, null, "monthly", color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data == null || data.length == 0) return;

        int w = getWidth();
        int h = getHeight();

        // Padding lebih proporsional: kiri cukup untuk label Y, bawah cukup untuk label X
        float leftPadding = 110f;
        float rightPadding = 20f;
        float topPadding = 28f;
        float bottomPadding = 90f;

        float chartLeft = leftPadding;
        float chartTop = topPadding;
        float chartRight = w - rightPadding;
        float chartBottom = h - bottomPadding;

        float chartW = chartRight - chartLeft;
        float chartH = chartBottom - chartTop;

        if (chartW <= 0 || chartH <= 0) return;

        float minVal = 6.5f;
        float maxVal = 7.5f;
        for (float val : data) {
            if (val < minVal) minVal = val;
            if (val > maxVal) maxVal = val;
        }
        adjustedMin = (float) Math.floor(minVal - 0.2f);
        adjustedMax = (float) Math.ceil(maxVal + 0.2f);
        float range = adjustedMax - adjustedMin;
        if (range == 0f) range = 1f;

        float stepX = (data.length > 1) ? (chartW / (data.length - 1)) : 0f;

        // Grid lines: 4 garis horizontal lebih tipis
        for (int i = 0; i <= 4; i++) {
            float y = chartTop + (chartH / 4f) * i;
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);
        }

        pointsX = new float[data.length];
        pointsY = new float[data.length];

        for (int i = 0; i < data.length; i++) {
            float value = data[i];
            if (value < adjustedMin) value = adjustedMin;
            if (value > adjustedMax) value = adjustedMax;

            pointsX[i] = chartLeft + i * stepX;
            pointsY[i] = chartBottom - ((value - adjustedMin) / range) * chartH;
        }

        // Line path dengan sedikit curve (cubic bezier sederhana)
        Path linePath = buildSmoothPath(pointsX, pointsY);

        Path fillPath = new Path(linePath);
        fillPath.lineTo(pointsX[data.length - 1], chartBottom);
        fillPath.lineTo(pointsX[0], chartBottom);
        fillPath.close();

        fillPaint.setShader(new LinearGradient(
                0, chartTop, 0, chartBottom,
                fillColorTop, fillColorBottom,
                Shader.TileMode.CLAMP
        ));

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        // Dot titik data: lebih kecil dan bersih
        for (int i = 0; i < data.length; i++) {
            canvas.drawCircle(pointsX[i], pointsY[i], 4.5f, dotPaint);
            canvas.drawCircle(pointsX[i], pointsY[i], 4.5f, dotStrokePaint);
        }

        if (selectedIndex >= 0 && selectedIndex < data.length) {
            float x = pointsX[selectedIndex];
            float y = pointsY[selectedIndex];

            canvas.drawCircle(x, y, 7.0f, selectedDotPaint);
            canvas.drawCircle(x, y, 9.0f, dotStrokePaint);

            drawTooltip(canvas, x, y, selectedIndex, data[selectedIndex]);
        }

        drawYAxisLabels(canvas, chartTop, chartBottom);
        drawXAxisLabels(canvas, pointsX, chartBottom);
    }

    /**
     * Membuat path dengan tension ringan agar line lebih smooth
     */
    private Path buildSmoothPath(float[] xs, float[] ys) {
        Path path = new Path();
        if (xs.length == 0) return path;

        path.moveTo(xs[0], ys[0]);

        if (xs.length == 1) return path;

        for (int i = 1; i < xs.length; i++) {
            float cpX1 = xs[i - 1] + (xs[i] - xs[i - 1]) * 0.4f;
            float cpY1 = ys[i - 1];
            float cpX2 = xs[i] - (xs[i] - xs[i - 1]) * 0.4f;
            float cpY2 = ys[i];
            path.cubicTo(cpX1, cpY1, cpX2, cpY2, xs[i], ys[i]);
        }

        return path;
    }

    private void drawYAxisLabels(Canvas canvas, float chartTop, float chartBottom) {
        // Label Y-axis: tampilkan 3 nilai (max, mid, min) agar lebih informatif
        float labelX = 8f;

        float midValue = adjustedMin + (adjustedMax - adjustedMin) / 2f;
        float midY = chartTop + (chartBottom - chartTop) / 2f;

        Paint.FontMetrics fm = valueTextPaint.getFontMetrics();

        canvas.drawText(String.format(Locale.getDefault(), "%.1f", adjustedMax), labelX, chartTop - fm.ascent / 2f, valueTextPaint);
        canvas.drawText(String.format(Locale.getDefault(), "%.1f", midValue), labelX, midY - fm.ascent / 2f, valueTextPaint);
        canvas.drawText(String.format(Locale.getDefault(), "%.1f", adjustedMin), labelX, chartBottom - fm.descent, valueTextPaint);
    }

    private void drawTooltip(Canvas canvas, float x, float y, int index, float value) {
        String labelStr = (labels != null && index < labels.size()) ? labels.get(index) : String.valueOf(index + 1);
        String text;
        if ("daily".equals(periodType)) {
            text = "Pukul " + labelStr + ": " + formatValue(value) + " pH";
        } else {
            text = "Tgl " + labelStr + ": " + formatValue(value) + " pH";
        }

        float paddingX = 18f;
        float textWidth = tooltipTextPaint.measureText(text);
        float boxWidth = textWidth + (paddingX * 2);
        float boxHeight = 42f;

        float left = x - (boxWidth / 2f);
        float top = y - 62f;
        float right = x + (boxWidth / 2f);
        float bottom = top + boxHeight;

        // Pastikan tooltip tidak keluar layar
        if (left < 10) {
            left = 10;
            right = left + boxWidth;
        }
        if (right > getWidth() - 10) {
            right = getWidth() - 10;
            left = right - boxWidth;
        }
        if (top < 10) {
            top = y + 18f;
            bottom = top + boxHeight;
        }

        // Shadow tooltip tipis
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.parseColor("#18000000"));
        shadowPaint.setStyle(Paint.Style.FILL);
        RectF shadowRect = new RectF(left + 2, top + 2, right + 2, bottom + 2);
        canvas.drawRoundRect(shadowRect, 14f, 14f, shadowPaint);

        RectF rect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(rect, 14f, 14f, tooltipBgPaint);

        // Border tooltip
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.0f);
        borderPaint.setColor(Color.parseColor("#B5D4F4"));
        canvas.drawRoundRect(rect, 14f, 14f, borderPaint);

        float textX = rect.centerX();
        float textY = rect.centerY() + 6f;
        canvas.drawText(text, textX, textY, tooltipTextPaint);
    }

    private void drawXAxisLabels(Canvas canvas, float[] pointsX, float chartBottom) {
        int count = data.length;
        float labelY = chartBottom + 20f;

        int interval;
        if ("monthly".equals(periodType)) {
            interval = count > 15 ? 5 : 2;
        } else if ("daily".equals(periodType)) {
            interval = count > 12 ? 2 : 1;
        } else {
            interval = 1;
        }

        for (int i = 0; i < count; i++) {
            boolean isFirst = i == 0;
            boolean isLast = i == count - 1;
            boolean shouldDraw = isFirst || isLast || ((i + 1) % interval == 0);

            if (shouldDraw) {
                String labelText = (labels != null && i < labels.size()) ? labels.get(i) : String.valueOf(i + 1);
                canvas.drawText(labelText, pointsX[i], labelY, xLabelPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (data == null || pointsX == null || pointsY == null) return false;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float touchX = event.getX();
            float touchY = event.getY();

            int nearestIndex = -1;
            float nearestDistance = Float.MAX_VALUE;

            for (int i = 0; i < pointsX.length; i++) {
                float dx = touchX - pointsX[i];
                float dy = touchY - pointsY[i];
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestIndex = i;
                }
            }

            if (nearestDistance <= 44f) {
                selectedIndex = nearestIndex;
                invalidate();
                performClick();
                return true;
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private String formatValue(float value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }
}