package com.shimpyo.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * 리포트 카테고리 원형표 (도넛). 조각 사이 2dp 흰 틈, 가운데에 가장 큰 카테고리 이름과 비율.
 * 가운데 글자는 조각 색이 아니라 글자 색(먹색 · 회색)으로 쓴다.
 */
final class DonutChart extends View {

    private final long[] values;
    private final int[] colors;
    private final String centerTop, centerMain, centerSub;
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gap = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();

    DonutChart(Context c, long[] values, int[] colors, String centerTop, String centerMain, String centerSub) {
        super(c);
        this.values = values;
        this.colors = colors;
        this.centerTop = centerTop;
        this.centerMain = centerMain;
        this.centerSub = centerSub;
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.BUTT);
        gap.setStyle(Paint.Style.STROKE);
        gap.setColor(Ui.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
        StringBuilder d = new StringBuilder("카테고리별 지출 원형표. ");
        if (centerMain != null) d.append(centerMain).append(' ').append(centerSub);
        setContentDescription(d);
    }

    @Override
    protected void onDraw(Canvas c) {
        float density = getResources().getDisplayMetrics().density;
        float size = Math.min(getWidth(), getHeight());
        float stroke = size * 0.17f;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = size / 2f - stroke / 2f;
        box.set(cx - r, cy - r, cx + r, cy + r);
        arc.setStrokeWidth(stroke);

        long total = 0;
        for (long v : values) total += Math.max(0, v);
        if (total == 0) {
            arc.setColor(Ui.BG);
            c.drawArc(box, 0, 360, false, arc);
        } else {
            float start = -90f;
            int parts = 0;
            for (long v : values) if (v > 0) parts++;
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0) continue;
                float sweep = 360f * values[i] / total;
                arc.setColor(colors[i]);
                c.drawArc(box, start, sweep, false, arc);
                start += sweep;
            }
            // 조각 사이 흰 틈 (2dp)
            if (parts > 1) {
                gap.setStrokeWidth(2 * density);
                float a = -90f;
                for (long v : values) {
                    if (v <= 0) continue;
                    double rad = Math.toRadians(a);
                    float in = r - stroke / 2f - density, out = r + stroke / 2f + density;
                    c.drawLine(cx + (float) Math.cos(rad) * in, cy + (float) Math.sin(rad) * in,
                            cx + (float) Math.cos(rad) * out, cy + (float) Math.sin(rad) * out, gap);
                    a += 360f * v / total;
                }
            }
        }

        // 가운데 글자: 위 회색 작은 글 · 가운데 굵은 글 · 아래 큰 숫자
        float sp = getResources().getDisplayMetrics().scaledDensity;
        float y = cy - 14 * sp;
        if (centerTop != null) {
            text.setTypeface(Typeface.DEFAULT);
            text.setTextSize(12 * sp);
            text.setColor(Ui.MUTED);
            c.drawText(centerTop, cx, y, text);
        }
        if (centerMain != null) {
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextSize(15 * sp);
            text.setColor(Ui.TEXT);
            c.drawText(centerMain, cx, y + 20 * sp, text);
        }
        if (centerSub != null) {
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextSize(24 * sp);
            text.setColor(Ui.TEXT);
            c.drawText(centerSub, cx, y + 48 * sp, text);
        }
    }
}
