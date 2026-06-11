package com.example.smartwaterchick;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class TankView extends View {

    private Paint tankBodyPaint, tankCapPaint, waterPaint, waterGradPaint,
            textPaint, borderPaint, shinePaint;
    private float fillPercent = 0.65f; // 65%

    public TankView(Context context) { super(context); init(); }
    public TankView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        tankBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tankBodyPaint.setColor(Color.parseColor("#D8E4F0"));
        tankBodyPaint.setStyle(Paint.Style.FILL);

        tankCapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tankCapPaint.setColor(Color.parseColor("#B0C4DE"));
        tankCapPaint.setStyle(Paint.Style.FILL);

        waterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        waterPaint.setColor(Color.parseColor("#1B5BCE"));
        waterPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(52f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#A0B8D0"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);

        shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shinePaint.setColor(Color.parseColor("#40FFFFFF"));
        shinePaint.setStyle(Paint.Style.FILL);
    }

    public void setFillPercent(float percent) {
        this.fillPercent = percent;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();

        float capH   = h * 0.12f;
        float bodyL  = w * 0.12f;
        float bodyR  = w * 0.88f;
        float bodyT  = capH;
        float bodyB  = h * 0.94f;
        float radius = (bodyR - bodyL) * 0.18f;

        // ── Tank body ──
        RectF body = new RectF(bodyL, bodyT, bodyR, bodyB);
        canvas.drawRoundRect(body, radius, radius, tankBodyPaint);
        canvas.drawRoundRect(body, radius, radius, borderPaint);

        // ── Water fill (clipped to body) ──
        canvas.save();
        Path clipPath = new Path();
        clipPath.addRoundRect(body, radius, radius, Path.Direction.CW);
        canvas.clipPath(clipPath);

        float waterTop = bodyT + (bodyB - bodyT) * (1f - fillPercent);

        // Gradient water
        Paint gradPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient grad = new LinearGradient(
                bodyL, waterTop, bodyR, bodyB,
                Color.parseColor("#3B82F6"),
                Color.parseColor("#1B5BCE"),
                Shader.TileMode.CLAMP);
        gradPaint.setShader(grad);
        gradPaint.setStyle(Paint.Style.FILL);

        RectF waterRect = new RectF(bodyL, waterTop, bodyR, bodyB);
        canvas.drawRect(waterRect, gradPaint);

        // Wave effect on water surface
        Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint.setColor(Color.parseColor("#60FFFFFF"));
        wavePaint.setStyle(Paint.Style.FILL);
        Path wave = new Path();
        float waveAmp = 6f;
        float wW = bodyR - bodyL;
        wave.moveTo(bodyL, waterTop);
        for (float x = 0; x <= wW; x += 4f) {
            float y = (float)(waterTop + waveAmp * Math.sin((x / wW) * 2 * Math.PI));
            wave.lineTo(bodyL + x, y);
        }
        wave.lineTo(bodyR, waterTop);
        wave.close();
        canvas.drawPath(wave, wavePaint);

        canvas.restore();

        // ── Tank cap (top lid) ──
        RectF cap = new RectF(bodyL + (bodyR - bodyL) * 0.2f, 0, bodyR - (bodyR - bodyL) * 0.2f, capH + 4f);
        canvas.drawRoundRect(cap, 10f, 10f, tankCapPaint);
        canvas.drawRoundRect(cap, 10f, 10f, borderPaint);

        // ── Shine highlight on body ──
        RectF shine = new RectF(bodyL + 8f, bodyT + 10f, bodyL + (bodyR - bodyL) * 0.22f, bodyB - 10f);
        canvas.drawRoundRect(shine, 8f, 8f, shinePaint);

        // ── Percentage text ──
        float centerY = bodyT + (bodyB - bodyT) / 2f;
        if (waterTop < centerY) {
            textPaint.setColor(Color.WHITE);
        } else {
            textPaint.setColor(Color.parseColor("#1B5BCE"));
        }
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textOffset = (fm.descent + fm.ascent) / 2f;
        canvas.drawText((int)(fillPercent * 100) + "%", w / 2f, centerY - textOffset, textPaint);
    }
}