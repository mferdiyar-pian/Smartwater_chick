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

public class BarChartView extends View {

    private Paint barPaint, textPaint, gridPaint, axisPaint, tooltipPaint, tooltipTextPaint, tooltipBgPaint;

    // 20 Data dummy untuk volume air dalam Liter
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

    // Untuk interaksi klik
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
        textPaint.setTextSize(26f);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#EEF1F8"));
        gridPaint.setStrokeWidth(1.5f);
        gridPaint.setStyle(Paint.Style.STROKE);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.parseColor("#E0E4EF"));
        axisPaint.setStrokeWidth(1.5f);

        // Tooltip paints
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

        float padLeft  = 80f;   // untuk label Y
        float padRight = 16f;
        float padTop   = 20f;
        float padBottom= 70f;   // untuk label X (ditingkatkan untuk 20 label)

        float chartW = w - padLeft - padRight;
        float chartH = h - padTop - padBottom;

        // --- Grid lines + Y labels ---
        textPaint.setTextSize(22f);
        for (float val : yGridValues) {
            float ratio = val / maxValue;
            float y = padTop + chartH - (ratio * chartH);

            // grid line
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint);

            // Y label (kanan rata kiri chart)
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf((int) val), padLeft - 10f, y + 8f, textPaint);
        }

        // --- Bars ---
        int n = data.length;
        float slotW = chartW / n;
        float barW  = slotW * 0.55f;  // Sedikit lebih sempit agar ada jarak
        float radius = 8f;

        // Tentukan step untuk label X berdasarkan jumlah data
        int labelStep = 1;
        if (n > 15) {
            labelStep = 3; // Tampilkan setiap 3 label untuk 20 data
        } else if (n > 7) {
            labelStep = 2; // Tampilkan setiap 2 label untuk data 8-15
        }

        for (int i = 0; i < n; i++) {
            float centerX  = padLeft + i * slotW + slotW / 2f;
            float barH     = (data[i] / maxValue) * chartH;
            float left     = centerX - barW / 2f;
            float right    = centerX + barW / 2f;
            float top      = padTop + chartH - barH;
            float bottom   = padTop + chartH;

            // Simpan rect untuk deteksi klik
            barRects[i].set(left, top, right, bottom);

            // Warna berbeda untuk bar yang dipilih
            if (i == selectedBarIndex) {
                barPaint.setColor(Color.parseColor("#FF6B35")); // Orange untuk selected
            } else {
                barPaint.setColor(Color.parseColor("#1B5BCE")); // Blue default
            }

            canvas.drawRoundRect(barRects[i], radius, radius, barPaint);

            // X label - tampilkan sesuai step agar tidak terlalu padat
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(20f); // Ukuran font lebih kecil
            if (i % labelStep == 0 || i == n - 1) {
                canvas.drawText(labels[i], centerX, h - 20f, textPaint);
            }
        }

        // --- Tooltip untuk bar yang dipilih ---
        if (selectedBarIndex >= 0 && selectedBarIndex < n) {
            drawTooltip(canvas, selectedBarIndex, barRects[selectedBarIndex]);
        }
    }

    private void drawTooltip(Canvas canvas, int index, RectF barRect) {
        String tooltipText = String.format("%.0f L", data[index]);
        float tooltipWidth = 120f;
        float tooltipHeight = 60f;
        float tooltipRadius = 12f;

        // Posisi tooltip di atas bar
        float tooltipLeft = barRect.centerX() - tooltipWidth / 2f;
        float tooltipTop = barRect.top - tooltipHeight - 20f;

        // Pastikan tooltip tidak keluar dari canvas
        if (tooltipTop < 10f) tooltipTop = barRect.bottom + 20f;
        if (tooltipLeft < 10f) tooltipLeft = 10f;
        if (tooltipLeft + tooltipWidth > getWidth() - 10f) {
            tooltipLeft = getWidth() - tooltipWidth - 10f;
        }

        float tooltipRight = tooltipLeft + tooltipWidth;
        float tooltipBottom = tooltipTop + tooltipHeight;

        // Gambar background tooltip
        RectF tooltipRect = new RectF(tooltipLeft, tooltipTop, tooltipRight, tooltipBottom);
        canvas.drawRoundRect(tooltipRect, tooltipRadius, tooltipRadius, tooltipBgPaint);

        // Gambar teks tooltip
        float textX = tooltipRect.centerX();
        float textY = tooltipRect.centerY() + 12f;
        canvas.drawText(tooltipText, textX, textY, tooltipTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            // Cek apakah klik pada salah satu bar
            for (int i = 0; i < barRects.length; i++) {
                if (barRects[i].contains(x, y)) {
                    selectedBarIndex = i;
                    invalidate();
                    // Tampilkan toast dengan detail
                    if (context != null) {
                        Toast.makeText(context, 
                            String.format("Data %s: %.0f Liter", labels[i], data[i]), 
                            Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            }

            // Klik di luar bar = hilangkan tooltip
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

    // Data untuk filter
    public void setDailyData() {
        // 7 data untuk hari (Sen-Min)
        float[] dailyData = {85f, 92f, 78f, 105f, 120f, 95f, 140f};
        String[] dailyLabels = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
        float[] dailyGrid = {0f, 40f, 80f, 120f, 160f, 200f};
        this.yGridValues = dailyGrid;
        setData(dailyData, dailyLabels, 200f);
    }

    public void setWeeklyData() {
        // 4 data untuk minggu
        float[] weeklyData = {580f, 620f, 590f, 650f};
        String[] weeklyLabels = {"Minggu 1", "Minggu 2", "Minggu 3", "Minggu 4"};
        float[] weeklyGrid = {0f, 200f, 400f, 600f, 800f};
        this.yGridValues = weeklyGrid;
        setData(weeklyData, weeklyLabels, 800f);
    }

    public void setMonthlyData() {
        // 20 data untuk bulan (tetap)
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