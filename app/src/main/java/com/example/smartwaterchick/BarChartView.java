package com.example.smartwaterchick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

public class BarChartView extends View {

    private Paint barPaint, textPaint, gridPaint, axisPaint, tooltipBgPaint, tooltipTextPaint;

    private float[] data = {
        85f, 92f, 78f, 105f, 120f, 95f, 140f, 88f, 112f, 135f,
        76f, 98f, 125f, 82f, 110f, 145f, 90f, 118f, 132f, 160f
    };
    private String[] labels = {
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
        "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"
    };
    private float maxValue = 200f;
    private float[] yGridValues = {0f, 40f, 80f, 120f, 160f, 200f};

    private RectF[] barRects;
    private int selectedBarIndex = -1;
    private Context context;

    public BarChartView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#1B5BCE"));
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#8A8FA8"));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#EEF1F8"));
        gridPaint.setStrokeWidth(1.5f);
        gridPaint.setStyle(Paint.Style.STROKE);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.parseColor("#E0E4EF"));
        axisPaint.setStrokeWidth(1.5f);

        tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipBgPaint.setColor(Color.parseColor("#1A1A2E"));
        tooltipBgPaint.setStyle(Paint.Style.FILL);

        tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipTextPaint.setColor(Color.WHITE);
        tooltipTextPaint.setTextSize(32f);
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);

        barRects = new RectF[data.length];
        for (int i = 0; i < data.length; i++) {
            barRects[i] = new RectF();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        float padLeft = 80f;
        float padRight = 16f;
        float padTop = 20f;
        float padBottom = 70f;

        float chartW = w - padLeft - padRight;
        float chartH = h - padTop - padBottom;

        textPaint.setTextSize(22f);
        for (float val : yGridValues) {
            float ratio = val / maxValue;
            float y = padTop + chartH - (ratio * chartH);
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf((int) val), padLeft - 10f, y + 8f, textPaint);
        }

        int n = data.length;
        float slotW = chartW / n;
        float barW = slotW * 0.55f;
        float radius = 8f;

        int labelStep = 1;
        if (n > 15) {
            labelStep = 3;
        } else if (n > 7) {
            labelStep = 2;
        }

        for (int i = 0; i < n; i++) {
            float centerX = padLeft + i * slotW + slotW / 2f;
            float barH = (data[i] / maxValue) * chartH;
            float left = centerX - barW / 2f;
            float right = centerX + barW / 2f;
            float top = padTop + chartH - barH;
            float bottom = padTop + chartH;

            barRects[i].set(left, top, right, bottom);

            if (i == selectedBarIndex) {
                barPaint.setColor(Color.parseColor("#FF6B35"));
            } else {
                barPaint.setColor(Color.parseColor("#1B5BCE"));
            }

            canvas.drawRoundRect(barRects[i], radius, radius, barPaint);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(20f);
            if (i % labelStep == 0 || i == n - 1) {
                canvas.drawText(labels[i], centerX, h - 20f, textPaint);
            }
        }

        if (selectedBarIndex >= 0 && selectedBarIndex < n) {
            drawTooltip(canvas, selectedBarIndex, barRects[selectedBarIndex]);
        }
    }

    private void drawTooltip(Canvas canvas, int index, RectF barRect) {
        String tooltipText = String.format("%.0f L", data[index]);
        float tooltipWidth = 120f;
        float tooltipHeight = 60f;
        float tooltipRadius = 12f;

        float tooltipLeft = barRect.centerX() - tooltipWidth / 2f;
        float tooltipTop = barRect.top - tooltipHeight - 20f;

        if (tooltipTop < 10f) tooltipTop = barRect.bottom + 20f;
        if (tooltipLeft < 10f) tooltipLeft = 10f;
        if (tooltipLeft + tooltipWidth > getWidth() - 10f) {
            tooltipLeft = getWidth() - tooltipWidth - 10f;
        }

        float tooltipRight = tooltipLeft + tooltipWidth;
        float tooltipBottom = tooltipTop + tooltipHeight;

        RectF tooltipRect = new RectF(tooltipLeft, tooltipTop, tooltipRight, tooltipBottom);
        canvas.drawRoundRect(tooltipRect, tooltipRadius, tooltipRadius, tooltipBgPaint);

        float textX = tooltipRect.centerX();
        float textY = tooltipRect.centerY() + 12f;
        canvas.drawText(tooltipText, textX, textY, tooltipTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            for (int i = 0; i < barRects.length; i++) {
                if (barRects[i].contains(x, y)) {
                    selectedBarIndex = i;
                    invalidate();
                    if (context != null) {
                        Toast.makeText(context,
                            String.format("Data %s: %.0f Liter", labels[i], data[i]),
                            Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            }

            selectedBarIndex = -1;
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    public void setData(float[] data, String[] labels, float maxValue) {
        this.data = data;
        this.labels = labels;
        this.maxValue = maxValue;
        this.barRects = new RectF[data.length];
        for (int i = 0; i < data.length; i++) {
            barRects[i] = new RectF();
        }
        selectedBarIndex = -1;
        invalidate();
    }

    public void loadDataFromDatabase(ArrayList<Float> dataList, ArrayList<String> labelList, String tipe) {
        if (dataList == null || dataList.isEmpty() || labelList == null || labelList.isEmpty()) {
            setDefaultData(tipe);
            return;
        }

        float[] newData = new float[dataList.size()];
        String[] newLabels = new String[labelList.size()];

        for (int i = 0; i < dataList.size(); i++) {
            newData[i] = dataList.get(i);
            newLabels[i] = labelList.get(i);
        }

        if (tipe.equals("weekly")) {
            float[] weeklyGrid = {0f, 200f, 400f, 600f, 800f};
            this.yGridValues = weeklyGrid;
            setData(newData, newLabels, 800f);
        } else {
            float[] dailyGrid = {0f, 40f, 80f, 120f, 160f, 200f};
            this.yGridValues = dailyGrid;
            setData(newData, newLabels, 200f);
        }
    }

    private void setDefaultData(String tipe) {
        if (tipe.equals("daily")) {
            float[] dailyData = {85f, 92f, 78f, 105f, 120f, 95f, 140f};
            String[] dailyLabels = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
            float[] dailyGrid = {0f, 40f, 80f, 120f, 160f, 200f};
            this.yGridValues = dailyGrid;
            setData(dailyData, dailyLabels, 200f);
        } else if (tipe.equals("weekly")) {
            float[] weeklyData = {580f, 620f, 590f, 650f};
            String[] weeklyLabels = {"Minggu 1", "Minggu 2", "Minggu 3", "Minggu 4"};
            float[] weeklyGrid = {0f, 200f, 400f, 600f, 800f};
            this.yGridValues = weeklyGrid;
            setData(weeklyData, weeklyLabels, 800f);
        } else {
            float[] monthlyData = {
                85f, 92f, 78f, 105f, 120f, 95f, 140f, 88f, 112f, 135f,
                76f, 98f, 125f, 82f, 110f, 145f, 90f, 118f, 132f, 160f
            };
            String[] monthlyLabels = {
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
                "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"
            };
            float[] monthlyGrid = {0f, 40f, 80f, 120f, 160f, 200f};
            this.yGridValues = monthlyGrid;
            setData(monthlyData, monthlyLabels, 200f);
        }
    }
}
