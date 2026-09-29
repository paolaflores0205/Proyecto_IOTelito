package com.example.proyecto_iotelito.ui.superadmin.reportes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;

public class WeeklyBookingsChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] values = {18, 26, 34, 42, 38, 51, 45};
    private String[] labels = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    public WeeklyBookingsChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
    }

    public void setData(String[] labels, int[] values) {
        if (labels == null || values == null || labels.length != values.length || values.length == 0) return;
        this.labels = labels;
        this.values = values;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = getPaddingLeft() + dp(30);
        float right = getWidth() - getPaddingRight() - dp(8);
        float top = getPaddingTop() + dp(20);
        float bottom = getHeight() - getPaddingBottom() - dp(28);
        int max = 1;
        for (int value : values) max = Math.max(max, value);
        int axisMax = ((max + 9) / 10) * 10;

        paint.setStrokeWidth(dp(1));
        paint.setTextSize(sp(10));
        paint.setColor(ContextCompat.getColor(getContext(), R.color.io_text_muted));
        paint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 3; i++) {
            float y = bottom - ((bottom - top) * i / 3f);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.io_divider));
            canvas.drawLine(left, y, right, y, paint);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.io_text_muted));
            canvas.drawText(String.valueOf(axisMax * i / 3), left - dp(6), y + dp(3), paint);
        }

        float slot = (right - left) / values.length;
        float barWidth = Math.min(dp(28), slot * 0.55f);
        for (int i = 0; i < values.length; i++) {
            float center = left + slot * i + slot / 2f;
            float barTop = bottom - (bottom - top) * values[i] / axisMax;
            paint.setColor(ContextCompat.getColor(getContext(), R.color.io_teal));
            canvas.drawRoundRect(new RectF(center - barWidth / 2f, barTop,
                    center + barWidth / 2f, bottom), dp(6), dp(6), paint);

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(sp(10));
            paint.setColor(ContextCompat.getColor(getContext(), R.color.io_navy));
            canvas.drawText(String.valueOf(values[i]), center, barTop - dp(6), paint);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.io_text_secondary));
            canvas.drawText(labels[i], center, bottom + dp(18), paint);
        }
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    private float sp(float value) { return value * getResources().getDisplayMetrics().scaledDensity; }
}
