package com.shimpyo.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** 자식 뷰를 왼쪽부터 채우고 넘치면 다음 줄로 넘기는 배치 (Figma flex-wrap). */
final class FlowLayout extends ViewGroup {
    private final int hGap;
    private final int vGap;

    FlowLayout(Context c, int hGapPx, int vGapPx) {
        super(c);
        this.hGap = hGapPx;
        this.vGap = vGapPx;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxW = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        boolean unbounded = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED;
        int x = 0, y = 0, rowH = 0, usedW = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View ch = getChildAt(i);
            if (ch.getVisibility() == GONE) continue;
            ch.measure(MeasureSpec.makeMeasureSpec(unbounded ? 0 : maxW, unbounded ? MeasureSpec.UNSPECIFIED : MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int w = ch.getMeasuredWidth(), h = ch.getMeasuredHeight();
            if (!unbounded && x > 0 && x + w > maxW) {
                x = 0;
                y += rowH + vGap;
                rowH = 0;
            }
            x += w + hGap;
            usedW = Math.max(usedW, x - hGap);
            rowH = Math.max(rowH, h);
        }
        int totalH = y + rowH + getPaddingTop() + getPaddingBottom();
        int width = unbounded ? usedW + getPaddingLeft() + getPaddingRight() : MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, resolveSize(totalH, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int maxW = r - l - getPaddingLeft() - getPaddingRight();
        int x = 0, y = 0, rowH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View ch = getChildAt(i);
            if (ch.getVisibility() == GONE) continue;
            int w = ch.getMeasuredWidth(), h = ch.getMeasuredHeight();
            if (x > 0 && x + w > maxW) {
                x = 0;
                y += rowH + vGap;
                rowH = 0;
            }
            ch.layout(getPaddingLeft() + x, getPaddingTop() + y, getPaddingLeft() + x + w, getPaddingTop() + y + h);
            x += w + hGap;
            rowH = Math.max(rowH, h);
        }
    }
}
