package com.shimpyo.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * 코드로 화면을 그리는 작은 도우미.
 * 색·글자·모서리 값은 Figma "쉼표 Hi-Fi 프로토타입"(쉼표 와이어프레임 › Components)의 변수와 맞춘다.
 */
final class Ui {
    private Ui() {}

    // ---- Figma 변수 (brand/blue는 이름과 달리 짙은 먹색)
    static final int BRAND = Color.parseColor("#292D2B");      // brand/blue · brand/navy
    static final int BRAND_LIGHT = Color.parseColor("#EAF1EC"); // brand/blue-light
    static final int TEXT = Color.parseColor("#292D2B");       // text/primary
    static final int MUTED = Color.parseColor("#737A75");      // text/muted
    static final int SUBTLE = Color.parseColor("#879A8A");     // text/subtle
    static final int LINE = Color.parseColor("#DFE3DF");       // line · chip/stroke
    static final int BG = Color.parseColor("#F3F5F2");         // bg
    static final int WHITE = Color.WHITE;                      // surface
    static final int RED = Color.parseColor("#A14D46");        // status/red
    static final int RED_BG = Color.parseColor("#F7EAE8");
    static final int AMBER = Color.parseColor("#8C6C23");      // status/amber
    static final int AMBER_BG = Color.parseColor("#F7F0DE");
    static final int GREEN = Color.parseColor("#37624C");      // status/green
    static final int GREEN_BG = Color.parseColor("#E8EFE9");
    static final int DIM = Color.argb(140, 0x17, 0x20, 0x1A);   // dim #17201A · 55%

    // 예전 이름 (다른 화면 코드와의 호환)
    static final int BLUE = BRAND;
    static final int BLUE_LIGHT = BRAND_LIGHT;
    static final int DARK = TEXT;
    static final int GRAY_BG = BG;

    static final int REGULAR = 400, MEDIUM = 500, BOLD = 700;

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
        return roundStroke(c, color, radiusDp, strokeColor, 1);
    }

    static GradientDrawable roundStroke(Context c, int color, float radiusDp, int strokeColor, float strokeDp) {
        GradientDrawable g = round(c, color, radiusDp);
        g.setStroke(Math.max(1, dp(c, strokeDp)), strokeColor);
        return g;
    }

    /** 모서리마다 반지름이 다른 둥근 사각형 (왼쪽 위, 오른쪽 위, 오른쪽 아래, 왼쪽 아래). */
    static GradientDrawable corners(Context c, int color, float tl, float tr, float br, float bl) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        float a = dp(c, tl), b = dp(c, tr), d = dp(c, br), e = dp(c, bl);
        g.setCornerRadii(new float[]{a, a, b, b, d, d, e, e});
        return g;
    }

    // ------------------------------------------------------------------ 글자

    /** Figma 글자 스타일의 줄 높이 (Caption 12/17 · Body 15/22 · Button 16/22 · Heading 17/25 · Title 22/30 · Display 30/38). */
    static int lineHeight(float sp) {
        if (sp == 11) return 16;
        if (sp == 12) return 17;
        if (sp == 13) return 19;
        if (sp == 14) return 20;
        if (sp == 15) return 22;
        if (sp == 16) return 22;
        if (sp == 17) return 25;
        if (sp == 18) return 26;
        if (sp == 22) return 30;
        if (sp == 28) return 40;
        if (sp == 30) return 38;
        return Math.round(sp * 1.45f);
    }

    static TextView text(Context c, String s, float sp, int color, int weight) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setLetterSpacing(-0.01f);
        setWeight(t, weight);
        int lh = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, lineHeight(sp), c.getResources().getDisplayMetrics()));
        if (Build.VERSION.SDK_INT >= 28) {
            t.setLineHeight(lh);
        } else {
            t.setLineSpacing(Math.max(0, lh - t.getPaint().getFontMetricsInt(null)), 1f);
        }
        return t;
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        return text(c, s, sp, color, bold ? BOLD : REGULAR);
    }

    static void setWeight(TextView t, int weight) {
        if (Build.VERSION.SDK_INT >= 28) {
            t.setTypeface(Typeface.create(Typeface.DEFAULT, weight, false));
        } else if (weight >= 600) {
            t.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (weight >= 500) {
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        } else {
            t.setTypeface(Typeface.DEFAULT);
        }
    }

    // ------------------------------------------------------------------ 배치

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

    static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

    static View spacer(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    // ------------------------------------------------------------------ 컴포넌트

    /** 흰 카드 (radius 16, padding 16). */
    static LinearLayout card(Context c) {
        LinearLayout l = v(c);
        l.setBackground(round(c, WHITE, 16));
        pad(l, c, 16, 16, 16, 16);
        return l;
    }

    /** 테두리 있는 흰 카드 (S04 홈: radius 14, 1dp line). */
    static LinearLayout outlineCard(Context c, int padH, int padV) {
        LinearLayout l = v(c);
        l.setBackground(roundStroke(c, WHITE, 14, LINE));
        pad(l, c, padH, padV, padH, padV);
        return l;
    }

    /** Button/Primary · Button/Secondary (radius 14, 16 Bold). */
    static TextView button(Context c, String label, boolean primary, View.OnClickListener click) {
        TextView b = text(c, label, 16, primary ? WHITE : TEXT, BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(primary ? round(c, BRAND, 14) : roundStroke(c, WHITE, 14, LINE));
        if (primary) pad(b, c, 20, 16, 20, 16);
        else pad(b, c, 20, 15, 20, 15);
        b.setOnClickListener(click);
        b.setClickable(true);
        return b;
    }

    /** 비활성 Primary 버튼. */
    static void setEnabled(TextView b, boolean on) {
        b.setEnabled(on);
        b.setAlpha(on ? 1f : 0.35f);
    }

    /** Button/Text (12 Medium muted, padding 8). */
    static TextView linkButton(Context c, String label, View.OnClickListener click) {
        TextView b = text(c, label, 12, MUTED, MEDIUM);
        b.setGravity(Gravity.CENTER);
        pad(b, c, 8, 8, 8, 8);
        b.setOnClickListener(click);
        return b;
    }

    /** Chip · Chip/Selected (radius 20, padding 10/16, 15 Medium). */
    static TextView chip(Context c, String label, boolean selected, View.OnClickListener click) {
        TextView t = text(c, label, 15, TEXT, MEDIUM);
        t.setGravity(Gravity.CENTER);
        t.setBackground(selected ? roundStroke(c, BRAND_LIGHT, 20, BRAND, 1.5f) : roundStroke(c, WHITE, 20, LINE));
        pad(t, c, 16, 10, 16, 10);
        if (click != null) t.setOnClickListener(click);
        return t;
    }

    /** Tag/Blue · Red · Amber · Green (radius 8, padding 4/10, 12 Medium). */
    static TextView tag(Context c, String label, int bg, int fg) {
        TextView t = text(c, label, 12, fg, MEDIUM);
        t.setBackground(round(c, bg, 8));
        pad(t, c, 10, 4, 10, 4);
        return t;
    }

    /** 홈·온보딩 화면의 작은 태그 (radius 6, padding 3/8, 12 Bold). */
    static TextView smallTag(Context c, String label, int bg, int fg) {
        TextView t = text(c, label, 12, fg, BOLD);
        t.setBackground(round(c, bg, 6));
        pad(t, c, 8, 3, 8, 3);
        return t;
    }

    /** 칩을 줄바꿈하며 왼쪽부터 채운다 (Figma Chips: wrap, gap 8). */
    static FlowLayout chipFlow(Context c, List<TextView> chips) {
        FlowLayout f = new FlowLayout(c, dp(c, 8), dp(c, 8));
        for (TextView t : chips) f.addView(t, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return f;
    }

    /** 칩을 한 줄에 perRow개씩 같은 폭으로 배치 (설정 화면 등). */
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

    /** Bubble/AI: "쉼표" 라벨 + 질문 (bg, 모서리 4/16/16/16). */
    static LinearLayout bubbleAi(Context c, String question, boolean faded) {
        LinearLayout b = v(c);
        b.setBackground(corners(c, BG, 4, 16, 16, 16));
        pad(b, c, 14, 12, 14, 12);
        b.addView(text(c, "쉼표", 12, BRAND, MEDIUM));
        TextView q = text(c, question, 15, TEXT, MEDIUM);
        b.addView(q);
        gapTop(q, c, 4);
        if (faded) b.setAlpha(0.6f);
        LinearLayout row = h(c);
        row.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 276f);
        row.addView(b, bp);
        row.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 44f));
        return row;
    }

    /** Bubble/User: 오른쪽 정렬 먹색 말풍선 (모서리 16/4/16/16). */
    static LinearLayout bubbleUser(Context c, String answer) {
        LinearLayout row = h(c);
        row.setGravity(Gravity.END);
        TextView b = text(c, answer, 15, WHITE, MEDIUM);
        b.setBackground(corners(c, BRAND, 16, 4, 16, 16));
        pad(b, c, 14, 10, 14, 10);
        row.addView(b, wrap());
        return row;
    }

    /** 사실 한 줄 상자: 연초록 바탕 + 왼쪽 3dp 초록 선 (radius 10, padding 8/12, 12 Medium). */
    static TextView factBox(Context c, String s) {
        TextView t = text(c, s, 12, TEXT, MEDIUM);
        Drawable line = round(c, GREEN, 10);
        Drawable fill = round(c, GREEN_BG, 10);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{line, fill});
        ld.setLayerInset(1, dp(c, 3), 0, 0, 0);
        t.setBackground(ld);
        pad(t, c, 12, 8, 12, 8);
        return t;
    }

    /** 온보딩 단계 표시: 현재 단계는 24×8 막대, 나머지는 8×8 점 (gap 6). */
    static LinearLayout steps(Context c, int current, int total, boolean doneFilled) {
        LinearLayout r = h(c);
        for (int i = 1; i <= total; i++) {
            View d = new View(c);
            boolean on = i == current || (doneFilled && i < current);
            d.setBackground(round(c, on ? BRAND : LINE, 4));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(c, i == current ? 24 : 8), dp(c, 8));
            if (i > 1) p.leftMargin = dp(c, 6);
            r.addView(d, p);
        }
        return r;
    }

    /** 20dp 체크 상자 + 문구 (Figma S02). */
    static LinearLayout checkRow(Context c, String label, boolean checked, View.OnClickListener click) {
        LinearLayout r = h(c);
        View box = new View(c);
        box.setBackground(checked ? round(c, BRAND, 6) : roundStroke(c, WHITE, 6, LINE, 1.5f));
        r.addView(box, new LinearLayout.LayoutParams(dp(c, 20), dp(c, 20)));
        TextView t = text(c, label, 15, TEXT, REGULAR);
        LinearLayout.LayoutParams tp = weight1();
        tp.leftMargin = dp(c, 10);
        r.addView(t, tp);
        r.setOnClickListener(click);
        return r;
    }

    static EditText input(Context c, String hint, String value, boolean number) {
        EditText e = new EditText(c);
        e.setHint(hint);
        if (value != null) e.setText(value);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        e.setTextColor(TEXT);
        e.setHintTextColor(SUBTLE);
        e.setSingleLine(true);
        e.setLetterSpacing(-0.01f);
        if (number) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setBackground(roundStroke(c, WHITE, 12, LINE));
        pad(e, c, 14, 12, 14, 12);
        return e;
    }

    /** 왼쪽 라벨 · 오른쪽 값 한 줄 (Figma Row: 15 Regular muted / 15 Medium primary). */
    static LinearLayout kv(Context c, String k, String v, boolean bold) {
        LinearLayout r = h(c);
        TextView kt = text(c, k, 15, bold ? TEXT : MUTED, bold ? MEDIUM : REGULAR);
        r.addView(kt, weight1());
        r.addView(text(c, v, bold ? 17 : 15, TEXT, bold ? BOLD : MEDIUM));
        return r;
    }

    /** 카드 안 작은 머리말 (12 Medium muted). */
    static TextView caption(Context c, String s) {
        return text(c, s, 12, MUTED, MEDIUM);
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
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 1))));
        return v;
    }

    /** 진행 막대 (높이 8, radius 4). */
    static LinearLayout bar(Context c, float ratio, int fillColor, int trackColor) {
        LinearLayout track = h(c);
        track.setBackground(round(c, trackColor, 4));
        float r = Math.max(0f, Math.min(1f, ratio));
        View fill = new View(c);
        fill.setBackground(round(c, fillColor, 4));
        track.addView(fill, new LinearLayout.LayoutParams(0, dp(c, 8), Math.max(0.0001f, r)));
        track.addView(new View(c), new LinearLayout.LayoutParams(0, dp(c, 8), Math.max(0.0001f, 1f - r)));
        if (r <= 0f) fill.setVisibility(View.INVISIBLE);
        return track;
    }

    /** 내용 높이만큼 늘어나되 maxHeight를 넘으면 스크롤되는 영역. */
    static final class MaxHeightScrollView extends ScrollView {
        private final int maxHeight;

        MaxHeightScrollView(Context c, int maxHeightPx) {
            super(c);
            this.maxHeight = maxHeightPx;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
        }
    }
}
