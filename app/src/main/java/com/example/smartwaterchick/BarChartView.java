package com.example.smartwaterchick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class BarChartView extends View {

    private Paint barPaint, barSelectedPaint, textPaint, gridPaint, axisPaint;
    private Paint tooltipBgPaint, tooltipTextPaint, tooltipBorderPaint, tooltipShadowPaint;

    private float[] data = {
        85f, 92f, 78f, 105f, 120f, 95f, 140f, 88f, 112f, 135f,
        76f, 98f, 125f, 82f, 110f, 145f, 90f, 118f, 132f, 160f
    };
    private String[] labels = {
        "1","2","3","4","5","6","7","8","9","10",
        "11","12","13","14","15","16","17","18","19","20"
    };
    private float maxValue = 200f;
    private float[] yGridValues = {0f, 40f, 80f, 120f, 160f, 200f};
    private String currentTipe = "monthly";

    private RectF[] barRects;
    private int selectedBarIndex = -1;

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClickable(true);

        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#1B5BCE"));
        barPaint.setStyle(Paint.Style.FILL);

        barSelectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barSelectedPaint.setColor(Color.parseColor("#FF6B35"));
        barSelectedPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#8A8FA8"));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#EEF1F8"));
        gridPaint.setStrokeWidth(1.5f);
        gridPaint.setStyle(Paint.Style.STROKE);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.parseColor("#E0E4EF"));
        axisPaint.setStrokeWidth(1.5f);

        // Tooltip — style sama seperti AnalisisChartView
        tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipBgPaint.setColor(Color.parseColor("#EAF3FF"));
        tooltipBgPaint.setStyle(Paint.Style.FILL);

        tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipTextPaint.setColor(Color.parseColor("#0D47A1"));
        tooltipTextPaint.setTextSize(28f);
        tooltipTextPaint.setFakeBoldText(true);
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);

        tooltipBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipBorderPaint.setStyle(Paint.Style.STROKE);
        tooltipBorderPaint.setStrokeWidth(1.0f);
        tooltipBorderPaint.setColor(Color.parseColor("#B5D4F4"));

        tooltipShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipShadowPaint.setColor(Color.parseColor("#18000000"));
        tooltipShadowPaint.setStyle(Paint.Style.FILL);

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

        float padLeft  = 80f;
        float padRight = 16f;
        float padTop   = 20f;
        float padBottom = 50f;

        float chartW = w - padLeft - padRight;
        float chartH = h - padTop - padBottom;

        // Grid lines Y
        textPaint.setTextSize(22f);
        for (float val : yGridValues) {
            float ratio = val / maxValue;
            float y = padTop + chartH - (ratio * chartH);
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf((int) val), padLeft - 10f, y + 8f, textPaint);
        }

        int n = data.length;
        // Batasi lebar slot maks seperti 7-slot agar 1 bar tidak melebar penuh
        int minSlots = Math.max(n, 7);
        float slotW = chartW / minSlots;
        float barW  = slotW * 0.55f;
        float radius = 8f;

        // Offset untuk center batang jika jumlah batang < minSlots
        float totalBarsWidth = n * slotW;
        float startOffset = padLeft + (chartW - totalBarsWidth) / 2f;

        // Label step — jangan terlalu penuh
        int labelStep = 1;
        if (n > 20) labelStep = 5;
        else if (n > 14) labelStep = 3;
        else if (n > 7)  labelStep = 2;

        for (int i = 0; i < n; i++) {
            float centerX = startOffset + i * slotW + slotW / 2f;
            float barH    = (data[i] / maxValue) * chartH;
            float left    = centerX - barW / 2f;
            float right   = centerX + barW / 2f;
            float top     = padTop + chartH - barH;
            float bottom  = padTop + chartH;

            barRects[i].set(left, top, right, bottom);

            canvas.drawRoundRect(barRects[i], radius, radius,
                    i == selectedBarIndex ? barSelectedPaint : barPaint);

            // Label X di bawah
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(20f);
            boolean isFirst = (i == 0);
            boolean isLast  = (i == n - 1);
            if (isFirst || isLast || i % labelStep == 0) {
                canvas.drawText(labels[i], centerX, h - 8f, textPaint);
            }
        }

        // Tooltip di atas batang yang dipilih
        if (selectedBarIndex >= 0 && selectedBarIndex < n) {
            drawTooltip(canvas, selectedBarIndex);
        }
    }

    private void drawTooltip(Canvas canvas, int index) {
        String labelStr = labels[index];
        String text;
        if (currentTipe.equals("daily")) {
            text = "Hari ini: " + String.format("%.0f L", data[index]);
        } else if (currentTipe.equals("weekly")) {
            text = "Hari " + labelStr + ": " + String.format("%.0f L", data[index]);
        } else {
            text = "Tgl " + labelStr + ": " + String.format("%.0f L", data[index]);
        }

        float paddingX  = 18f;
        float textWidth = tooltipTextPaint.measureText(text);
        float boxW      = textWidth + paddingX * 2;
        float boxH      = 42f;

        RectF bar = barRects[index];
        float cx  = bar.centerX();
        float left   = cx - boxW / 2f;
        float top    = bar.top - boxH - 12f;
        float right  = left + boxW;
        float bottom = top + boxH;

        // Clamp agar tidak keluar layar
        if (left < 10f)              { left = 10f;               right = left + boxW; }
        if (right > getWidth() - 10f){ right = getWidth() - 10f; left  = right - boxW; }
        if (top < 10f)               { top = bar.bottom + 12f;   bottom = top + boxH; }

        // Shadow
        RectF shadow = new RectF(left + 2, top + 2, right + 2, bottom + 2);
        canvas.drawRoundRect(shadow, 14f, 14f, tooltipShadowPaint);

        // Background
        RectF rect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(rect, 14f, 14f, tooltipBgPaint);

        // Border
        canvas.drawRoundRect(rect, 14f, 14f, tooltipBorderPaint);

        // Text
        canvas.drawText(text, rect.centerX(), rect.centerY() + 10f, tooltipTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            for (int i = 0; i < barRects.length; i++) {
                // Perluas area sentuh sedikit ke atas agar lebih mudah diklik
                RectF hitArea = new RectF(barRects[i]);
                hitArea.top -= 20f;
                if (hitArea.contains(x, y)) {
                    selectedBarIndex = (selectedBarIndex == i) ? -1 : i;
                    invalidate();
                    performClick();
                    return true;
                }
            }
            selectedBarIndex = -1;
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void setData(float[] data, String[] labels, float maxValue) {
        this.data      = data;
        this.labels    = labels;
        this.maxValue  = maxValue;
        this.barRects  = new RectF[data.length];
        for (int i = 0; i < data.length; i++) barRects[i] = new RectF();
        selectedBarIndex = -1;
        invalidate();
    }

    public void loadDataFromDatabase(ArrayList<Float> dataList, ArrayList<String> labelList, String tipe) {
        this.currentTipe = tipe;
        if (dataList == null || dataList.isEmpty() || labelList == null || labelList.isEmpty()) {
            setDefaultData(tipe);
            return;
        }

        float[] newData   = new float[dataList.size()];
        String[] newLabels = new String[labelList.size()];
        for (int i = 0; i < dataList.size(); i++) {
            newData[i]   = dataList.get(i);
            newLabels[i] = labelList.get(i);
        }

        if (tipe.equals("weekly")) {
            yGridValues = new float[]{0f, 40f, 80f, 120f, 160f, 200f};
            setData(newData, newLabels, 200f);
        } else if (tipe.equals("monthly")) {
            yGridValues = new float[]{0f, 40f, 80f, 120f, 160f, 200f};
            setData(newData, newLabels, 200f);
        } else { // daily
            yGridValues = new float[]{0f, 40f, 80f, 120f, 160f, 200f};
            setData(newData, newLabels, 200f);
        }
    }

    private void setDefaultData(String tipe) {
        if (tipe.equals("daily")) {
            // 1 Hari = hanya hari ini (1 batang)
            float[] d  = {120f};
            String[] l = {"Hari ini"};
            yGridValues = new float[]{0f, 40f, 80f, 120f, 160f, 200f};
            setData(d, l, 200f);
        } else if (tipe.equals("weekly")) {
            // 1 Minggu = 7 hari terakhir
            float[] d  = {85f, 92f, 78f, 105f, 120f, 95f, 140f};
            String[] l = {"1","2","3","4","5","6","7"};
            yGridValues = new float[]{0f, 40f, 80f, 120f, 160f, 200f};
            setData(d, l, 200f);
        } else {
            // 1 Bulan = 30 hari
            float[] d  = {
                85f, 92f, 78f, 105f, 120f, 95f, 140f, 88f, 112f, 135f,
                76f, 98f, 125f, 82f, 110f, 145f, 90f, 118f, 132f, 160f,
                88f, 97f, 115f, 80f, 130f, 100f, 142f, 95f, 108f, 125f
            };
            String[] l = {
                "1","2","3","4","5","6","7","8","9","10",
                "11","12","13","14","15","16","17","18","19","20",
                "21","22","23","24","25","26","27","28","29","30"
            };
            yGridValues = new float[]{0f, 40f, 80f, 120f, 160f, 200f};
            setData(d, l, 200f);
        }
    }
}
