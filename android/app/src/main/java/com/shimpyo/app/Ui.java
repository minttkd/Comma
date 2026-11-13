package com.shimpyo.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** 코드로 화면을 그리는 작은 도우미. 색은 Figma 와이어프레임과 맞춘다. */
final class Ui {
    private Ui() {}

    static final int BLUE = Color.parseColor("#2E70ED");
    static final int BLUE_LIGHT = Color.parseColor("#E8F0FF");
    static final int DARK = Color.parseColor("#1C1C21");
    static final int MUTED = Color.parseColor("#70737D");
    static final int LINE = Color.parseColor("#EBEBF0");
    static final int BG = Color.parseColor("#F5F5F7");
    static final int WHITE = Color.WHITE;
    static final int RED = Color.parseColor("#C4302B");
    static final int RED_BG = Color.parseColor("#FCEBEB");
    static final int AMBER = Color.parseColor("#B26B00");
    static final int AMBER_BG = Color.parseColor("#FFF4E0");
    static final int GREEN = Color.parseColor("#1F8A4C");
    static final int GREEN_BG = Color.parseColor("#E5F5EB");
    static final int GRAY_BG = Color.parseColor("#EBEBF0");

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static GradientDrawable round(Context c, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    static GradientDrawable roundStroke(Context c, int color, float radiusDp, int strokeColor) {
        GradientDrawable g = round(c, color, radiusDp);
        g.setStroke(dp(c, 1), strokeColor);
        return g;
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    static LinearLayout v(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout h(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    static LinearLayout.LayoutParams fillW() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    static LinearLayout.LayoutParams weight1() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    static void pad(View v, Context c, int l, int t, int r, int b) {
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    static void gapTop(View v, Context c, int dpTop) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        LinearLayout.LayoutParams lp = p instanceof LinearLayout.LayoutParams ? (LinearLayout.LayoutParams) p : fillW();
        lp.topMargin = dp(c, dpTop);
        v.setLayoutParams(lp);
    }

    /** 흰 카드. */
    static LinearLayout card(Context c) {
        LinearLayout l = v(c);
        l.setBackground(round(c, WHITE, 14));
        pad(l, c, 16, 14, 16, 14);
        return l;
    }

    static TextView button(Context c, String label, boolean primary, View.OnClickListener click) {
        TextView b = text(c, label, 16, primary ? WHITE : DARK, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(primary ? round(c, BLUE, 12) : roundStroke(c, WHITE, 12, Color.parseColor("#D9DBE0")));
        pad(b, c, 16, 14, 16, 14);
        b.setOnClickListener(click);
        b.setClickable(true);
        return b;
    }

    static TextView linkButton(Context c, String label, View.OnClickListener click) {
        TextView b = text(c, label, 14, MUTED, false);
        b.setGravity(Gravity.CENTER);
        pad(b, c, 12, 10, 12, 10);
        b.setOnClickListener(click);
        return b;
    }

    static TextView chip(Context c, String label, boolean selected, View.OnClickListener click) {
        TextView t = text(c, label, 14, selected ? BLUE : DARK, selected);
        t.setGravity(Gravity.CENTER);
        t.setBackground(roundStroke(c, selected ? BLUE_LIGHT : WHITE, 20, selected ? BLUE : Color.parseColor("#D9DBE0")));
        pad(t, c, 14, 9, 14, 9);
        if (click != null) t.setOnClickListener(click);
        return t;
    }

    static TextView tag(Context c, String label, int bg, int fg) {
        TextView t = text(c, label, 11, fg, true);
        t.setBackground(round(c, bg, 6));
        pad(t, c, 8, 3, 8, 3);
        return t;
    }

    /** 칩을 한 줄에 perRow개씩 배치. */
    static LinearLayout chipGrid(Context c, List<TextView> chips, int perRow) {
        LinearLayout box = v(c);
        LinearLayout row = null;
        for (int i = 0; i < chips.size(); i++) {
            if (i % perRow == 0) {
                row = h(c);
                box.addView(row, fillW());
                if (i > 0) gapTop(row, c, 8);
            }
            LinearLayout.LayoutParams p = weight1();
            if (i % perRow != 0) p.leftMargin = dp(c, 8);
            row.addView(chips.get(i), p);
        }
        // 마지막 줄 빈 칸 채우기
        if (row != null && chips.size() % perRow != 0) {
            for (int k = chips.size() % perRow; k < perRow; k++) {
                View spacer = new View(c);
                LinearLayout.LayoutParams p = weight1();
                p.leftMargin = dp(c, 8);
                row.addView(spacer, p);
            }
        }
        return box;
    }

    static EditText input(Context c, String hint, String value, boolean number) {
        EditText e = new EditText(c);
        e.setHint(hint);
        if (value != null) e.setText(value);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        e.setTextColor(DARK);
        e.setSingleLine(true);
        if (number) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setBackground(roundStroke(c, WHITE, 10, Color.parseColor("#D9DBE0")));
        pad(e, c, 12, 10, 12, 10);
        return e;
    }

    /** 왼쪽 라벨 · 오른쪽 값 한 줄. */
    static LinearLayout kv(Context c, String k, String v, boolean bold) {
        LinearLayout r = h(c);
        TextView kt = text(c, k, 14, bold ? DARK : MUTED, bold);
        r.addView(kt, weight1());
        r.addView(text(c, v, bold ? 16 : 14, DARK, true));
        return r;
    }

    static long num(EditText e, long def) {
        try {
            String s = e.getText().toString().replace(",", "").trim();
            return s.isEmpty() ? def : Long.parseLong(s);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 1)));
        return v;
    }
}
