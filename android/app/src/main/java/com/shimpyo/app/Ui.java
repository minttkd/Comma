package com.shimpyo.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * 쉼표 디자인 시스템: 토큰(색·글자·간격·모서리·크기)과 공통 UI 부품.
 * 값은 Figma "쉼표 Hi-Fi 프로토타입"(쉼표 와이어프레임 › Components)의 변수와 맞추고, 주석에 Figma 이름을 적는다.
 *
 * 화면 코드에서는 숫자 대신 이 파일의 이름만 쓴다.
 * <pre>
 *   색          Ui.TEXT, Ui.MUTED, Ui.BRAND …
 *   상태 색 묶음 Ui.Tone.SUCCESS · WARNING · DANGER …
 *   글자 크기    Ui.Font.TITLE · BODY · CAPTION …
 *   간격(사이)   Ui.Space.BLOCK · ROW · ITEM …
 *   여백(안쪽)   Ui.Pad.CARD · BUTTON_H · CHIP_V …
 *   모서리      Ui.Radius.CARD · BOX · CHIP …
 *   고정 크기    Ui.Size.NAV_HEIGHT · ICON_BUTTON …
 * </pre>
 * 전체 목록과 쓰는 곳은 docs/UI_컴포넌트.md 에 정리했다.
 */
final class Ui {
    private Ui() {}

    // ================================================================== 색 (Figma Color 변수)
    // brand/blue는 이름과 달리 짙은 먹색이다. 서비스 색(쉼표 초록)은 GREEN · BRAND_LIGHT.

    static final int BRAND = Color.parseColor("#292D2B");      // brand/blue · brand/navy · Primary 버튼
    static final int BRAND_LIGHT = Color.parseColor("#EAF1EC"); // brand/blue-light · 서비스 색 연한 바탕
    static final int TEXT = Color.parseColor("#292D2B");       // text/primary
    static final int MUTED = Color.parseColor("#737A75");      // text/muted
    static final int SUBTLE = Color.parseColor("#879A8A");     // text/subtle
    static final int LINE = Color.parseColor("#DFE3DF");       // line · chip/stroke
    static final int BG = Color.parseColor("#F3F5F2");         // bg
    static final int WHITE = Color.WHITE;                      // surface (카드 · 시트 바탕)
    static final int RED = Color.parseColor("#A14D46");        // status/red
    static final int RED_BG = Color.parseColor("#F7EAE8");     // status/red-bg
    static final int AMBER = Color.parseColor("#8C6C23");      // status/amber
    static final int AMBER_BG = Color.parseColor("#F7F0DE");   // status/amber-bg
    static final int GREEN = Color.parseColor("#37624C");      // status/green · 서비스 색(쉼표 초록)
    static final int GREEN_BG = Color.parseColor("#E8EFE9");   // status/green-bg
    static final int DIM = Color.argb(140, 0x17, 0x20, 0x1A);  // dim #17201A · 55%

    static final int ON_BRAND = WHITE;                         // 먹색(BRAND) 바탕 위 글자
    static final int ON_BRAND_SUB = LINE;                      // 먹색 바탕 위 보조 글자
    // 강조 카드(heroCard · 리포트 "이번 달 내 기준")는 서비스 색(쉼표 초록) 바탕
    static final int HERO_BG = GREEN;
    static final int ON_HERO = WHITE;                          // 강조 카드 위 글자
    static final int ON_HERO_SUB = Color.parseColor("#CFE0D5"); // 강조 카드 위 보조 글자 (초록 바탕 대비 5:1)
    static final int ON_HERO_BOX = Color.argb(36, 255, 255, 255); // 강조 카드 안 상자 (흰색 14%)
    static final int GUARD_FILL = Color.argb(40, 0x37, 0x62, 0x4C); // status/green 16% · 결제 버튼 가드 표시

    /** 상태 색 묶음 (바탕, 글자). 태그·알림에 쓴다. */
    enum Tone {
        SUCCESS(GREEN_BG, GREEN),       // Tag/Green · 멈춤, 여유, 만족
        WARNING(AMBER_BG, AMBER),       // Tag/Amber · 주의, 숙고 후 결제
        DANGER(RED_BG, RED),            // Tag/Red · 초과, 재무 위험, 후회
        BRAND(BRAND_LIGHT, Ui.BRAND),   // Tag/Blue · 강도, 회고
        ACCENT(BRAND_LIGHT, GREEN),     // 연초록 바탕 + 초록 글자 · 필수, 이유 선택 결제
        NEUTRAL(BG, MUTED),             // 회색 · 선택, 기타
        PLAIN(WHITE, MUTED),            // 흰 바탕 + 회색 글자 · 색 바탕 위 태그
        INVERSE(Ui.BRAND, WHITE);       // 먹색 바탕 · 강한 개입

        final int bg, fg;

        Tone(int bg, int fg) {
            this.bg = bg;
            this.fg = fg;
        }
    }

    // ================================================================== 글자 (Figma Text Style)

    /** 글자 크기 (sp). 줄 높이는 lineHeight()에 있다. */
    static final class Font {
        private Font() {}

        static final int DISPLAY = 30;  // Display 30/38 · 리포트 큰 숫자
        static final int AMOUNT = 28;   // 28/40 · 홈 "오늘 쓸 수 있는 돈"
        static final int TITLE = 22;    // Title 22/30 · 화면 제목
        static final int STAT = 18;     // 18/26 · 홈 요약 타일 값
        static final int HEADING = 17;  // Heading 17/25 · 시트·개입 제목
        static final int BUTTON = 16;   // Button 16/22
        static final int BODY = 15;     // Body 15/22
        static final int LABEL = 14;    // 14/20 · 목록 제목
        static final int SMALL = 13;    // 13/19 · 짧은 안내
        static final int CAPTION = 12;  // Caption 12/17
        static final int MICRO = 11;    // 11/16 · 아주 작은 설명
    }

    static final int REGULAR = 400, MEDIUM = 500, BOLD = 700;

    // ================================================================== 간격 · 모서리 · 크기 (dp)

    /**
     * 요소와 요소 사이 간격 (바깥 여백, margin).
     * 부품 안쪽 여백은 여기 말고 Pad에 있다. 그래서 Space 값을 바꿔도 카드·버튼 크기는 그대로다.
     */
    static final class Space {
        private Space() {}

        static final int TEXT = 2;      // 제목 ↔ 바로 아래 설명
        static final int LINE = 4;      // 가까운 줄 사이
        static final int TIGHT = 6;     // 단계 점·태그 사이, 내 답 줄 사이
        static final int ITEM = 8;      // 나란히 놓인 요소 사이, 칩 간격, 설정 입력 줄 사이
        static final int ROW = 10;      // 카드 안 줄 사이
        static final int BLOCK = 12;    // 카드 ↔ 카드 (홈·리포트·기록·S03)
        static final int STACK = 14;    // 개입 시트·S01의 세로 간격
        static final int WIDE = 16;     // S02의 블록 사이
        static final int SECTION = 18;  // 섹션 제목 위 (설정)
        static final int LARGE = 24;    // 큰 섹션 위 (설정 테스트 도구)
    }

    /**
     * 안쪽 여백 (padding). 부품·자리마다 따로 이름을 두어, 하나를 고쳐도 다른 곳은 바뀌지 않는다.
     * _H는 좌우, _V는 위아래.
     */
    static final class Pad {
        private Pad() {}

        // 화면 틀
        static final int SCREEN_H = 20;         // 화면 좌우
        static final int SCREEN_TOP = 12;       // 스크롤 내용 위
        static final int SCREEN_BOTTOM = 24;    // 스크롤 내용·하단 버튼 영역 아래
        static final int FOOTER_TOP = 8;        // 온보딩 하단 버튼 영역 위
        static final int STEPS_TOP = 14;        // S01 단계 점 위 (뒤로 버튼 없을 때)
        static final int STEPS_BOTTOM = 10;     // S01 단계 점 아래

        // 시트
        static final int SHEET_H = 20;          // Sheet 좌우
        static final int SHEET_TOP = 18;        // 개입 시트 위 (Figma A2~C3)
        static final int REVIEW_SHEET_TOP = 22; // 회고 시트 위 (Figma S09)
        static final int SHEET_BOTTOM = 24;     // Sheet 아래

        // 카드·상자
        static final int CARD = 16;             // Card
        static final int TODAY_CARD = 18;       // 홈 "오늘 쓸 수 있는 돈"
        static final int TILE_H = 16;           // 홈 요약 타일 좌우
        static final int TILE_V = 14;           // 홈 요약 타일 위아래
        static final int REPORT_TILE = 14;      // 리포트 요약 타일
        static final int HERO = 16;             // 먹색 강조 카드 (S03)
        static final int HERO_LARGE = 20;       // 먹색 강조 카드 (S10)
        static final int BOX_H = 16;            // 경고·성과 상자 좌우
        static final int BOX_V = 14;            // 경고·성과 상자 위아래
        static final int ANSWER_BOX = 14;       // 개입 결정 화면 "내 답"
        static final int TOAST_H = 18;          // 토스트 좌우
        static final int TOAST_V = 14;          // 토스트 위아래
        static final int FACT_H = 12;           // 사실 한 줄 상자 좌우
        static final int FACT_V = 8;            // 사실 한 줄 상자 위아래
        static final int PRIVACY_NOTE = 12;     // S01 개인정보 안내
        static final int RETRY_NOTE_H = 12;     // 개입 재시도 안내 좌우
        static final int RETRY_NOTE_V = 10;     // 개입 재시도 안내 위아래
        static final int FOOD_NOTE_H = 14;      // 리포트 식비 안내 좌우
        static final int FOOD_NOTE_V = 10;      // 리포트 식비 안내 위아래

        // 버튼·칩·태그
        static final int BUTTON_H = 20;         // Button 좌우
        static final int BUTTON_V = 16;         // Button 위아래 (Secondary는 테두리만큼 1 작게)
        static final int LINK = 8;              // Button/Text
        static final int ICON = 6;              // ✕ 닫기
        static final int CHIP_H = 16;
        static final int CHIP_V = 10;
        static final int TAG_H = 10;
        static final int TAG_V = 4;
        static final int SMALL_TAG_H = 8;
        static final int SMALL_TAG_V = 3;

        // 대화·입력
        static final int BUBBLE_H = 14;         // 말풍선 좌우
        static final int BUBBLE_AI_V = 12;      // 쉼표 말풍선 위아래
        static final int BUBBLE_USER_V = 10;    // 내 말풍선 위아래
        static final int INPUT_H = 14;          // Input 좌우
        static final int INPUT_V = 12;          // Input 위아래
        static final int SEND_LEFT = 10;        // [보내기] 왼쪽

        // 목록
        static final int LIST_ROW_V = 10;       // 데모 시나리오·테스트 도구 한 줄 위아래
        static final int QA_INDENT = 10;        // 기록 탭 질문·답 들여쓰기
        static final int TINT_ROW_H = 12;       // 기록 줄 "줄 바탕" 방식 좌우
        static final int TINT_ROW_V = 10;       // 기록 줄 "줄 바탕" 방식 위아래

        // 데모 결제 화면 (다른 앱 화면 재현)
        static final int DEMO_SIDE = 16;        // AppBar·섹션·PayBar 좌우
        static final int DEMO_APPBAR_V = 12;
        static final int DEMO_SECTION_V = 14;
        static final int DEMO_PAYBAR_TOP = 10;
        static final int DEMO_PAYBAR_BOTTOM = 18;
        static final int DEMO_PAY_V = 15;       // 결제 버튼 위아래
        static final int DEMO_CONTENT_TOP = 8;
        static final int DEMO_CONTENT_BOTTOM = 16;
    }

    /** 모서리 반지름. */
    static final class Radius {
        private Radius() {}

        static final int BAR = 4;       // 진행 막대, 단계 점, 말풍선 꼬리
        static final int CHECK = 6;     // 체크 상자, 작은 태그
        static final int TAG = 8;       // Tag
        static final int NOTE = 10;     // 사실 한 줄 상자, 짧은 안내
        static final int INPUT = 12;    // Input
        static final int BOX = 14;      // Button, 테두리 카드, 알림, 토스트
        static final int CARD = 16;     // Card, 말풍선
        static final int CHIP = 20;     // Chip
        static final int SHEET = 24;    // Sheet 위 모서리
    }

    /** 고정 크기. */
    static final class Size {
        private Size() {}

        static final int NAV_HEIGHT = 63;    // BottomNav 64 − 위 1dp 선
        static final int NAV_INSET = 28;     // BottomNav 좌우 여백
        static final int NAV_DOT = 6;        // 선택된 탭 점
        static final int ICON_BUTTON = 24;   // ← · ✕ 누르는 영역
        static final int CHECKBOX = 20;
        static final int STEP_DOT = 8;       // 온보딩 단계 점
        static final int STEP_ACTIVE = 24;   // 현재 단계 막대 폭
        static final int BAR = 8;            // 진행 막대 높이
        static final int BAR_THIN = 6;       // 요약 타일 안 진행 막대 높이
        static final int LIST_DOT = 8;       // 기록 줄 왼쪽 결과 색 점
        static final int LIST_BAR = 4;       // 기록 줄 왼쪽 결과 색 막대 폭
        static final int AVATAR = 28;        // 개입 채팅 쉼표 얼굴 (채팅 ②·③)
        static final int FIELD_WIDTH = 130;  // 설정 입력칸 폭
        static final int ROW_LABEL = 48;     // 리포트 강도 이름 칸
        static final float STROKE = 1f;
        static final float STROKE_STRONG = 1.5f; // 선택된 칩, 필수 입력
        static final int GUARD_STROKE = 3;   // 결제 버튼 가드 테두리
        static final int ELEVATION = 6;      // 토스트 그림자
        static final int TOAST_OFFSET = 100; // 토스트 바닥에서 띄우는 높이

        // UI 갤러리 전용
        static final int SWATCH = 32;        // 색 견본
        static final int RADIUS_SAMPLE = 48; // 모서리 견본
        static final int GALLERY_LABEL = 150; // 간격 막대 이름 칸
    }

    /** Figma 글자 스타일의 줄 높이 (Caption 12/17 · Body 15/22 · Button 16/22 · Heading 17/25 · Title 22/30 · Display 30/38). */
    static int lineHeight(float sp) {
        if (sp == Font.MICRO) return 16;
        if (sp == Font.CAPTION) return 17;
        if (sp == Font.SMALL) return 19;
        if (sp == Font.LABEL) return 20;
        if (sp == Font.BODY) return 22;
        if (sp == Font.BUTTON) return 22;
        if (sp == Font.HEADING) return 25;
        if (sp == Font.STAT) return 26;
        if (sp == Font.TITLE) return 30;
        if (sp == Font.AMOUNT) return 40;
        if (sp == Font.DISPLAY) return 38;
        return Math.round(sp * 1.45f);
    }

    // ================================================================== 바탕 그리기

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
        return roundStroke(c, color, radiusDp, strokeColor, Size.STROKE);
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

    static GradientDrawable circle(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    // ================================================================== 글자

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

    /** 화면 제목 · Title 22 Bold. */
    static TextView title(Context c, String s) {
        return text(c, s, Font.TITLE, TEXT, BOLD);
    }

    /** 시트·개입 제목 · Heading 17 Bold. */
    static TextView heading(Context c, String s) {
        return text(c, s, Font.HEADING, TEXT, BOLD);
    }

    /** 섹션 머리말 · 15 Bold. */
    static TextView sectionTitle(Context c, String s) {
        return text(c, s, Font.BODY, TEXT, BOLD);
    }

    /** 본문 · Body 15 Regular muted. */
    static TextView body(Context c, String s) {
        return text(c, s, Font.BODY, MUTED, REGULAR);
    }

    /** 짧은 안내 · 13 Regular muted. */
    static TextView bodySmall(Context c, String s) {
        return text(c, s, Font.SMALL, MUTED, REGULAR);
    }

    /** 카드 머리말 · Caption 12 Medium muted. */
    static TextView caption(Context c, String s) {
        return text(c, s, Font.CAPTION, MUTED, MEDIUM);
    }

    /** 시각·부가 정보 · Caption 12 Regular muted. */
    static TextView meta(Context c, String s) {
        return text(c, s, Font.CAPTION, MUTED, REGULAR);
    }

    /** 아주 작은 설명 · 11 Regular. */
    static TextView micro(Context c, String s, int color) {
        return text(c, s, Font.MICRO, color, REGULAR);
    }

    // ================================================================== 배치

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

    /** 정사각형 크기의 LayoutParams. */
    static LinearLayout.LayoutParams square(Context c, int sizeDp) {
        return new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp));
    }

    static void pad(View v, Context c, int l, int t, int r, int b) {
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    /** 좌우 h, 위아래 v 여백. */
    static void pad(View v, Context c, int h, int vert) {
        pad(v, c, h, vert, h, vert);
    }

    static void pad(View v, Context c, int all) {
        pad(v, c, all, all, all, all);
    }

    static void gapTop(View v, Context c, int dpTop) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        LinearLayout.LayoutParams lp = p instanceof LinearLayout.LayoutParams ? (LinearLayout.LayoutParams) p : fillW();
        lp.topMargin = dp(c, dpTop);
        v.setLayoutParams(lp);
    }

    /** 세로 묶음: parent 아래에 child를 가로 꽉 차게 붙이고 위 간격을 gapDp만큼 둔다. */
    static <T extends View> T stack(ViewGroup parent, T child, int gapDp) {
        LinearLayout.LayoutParams p = fillW();
        p.topMargin = dp(parent.getContext(), gapDp);
        parent.addView(child, p);
        return child;
    }

    /** 가로 묶음: row 오른쪽에 child를 내용 크기로 붙이고 왼쪽 간격을 gapDp만큼 둔다. */
    static <T extends View> T inline(LinearLayout row, T child, int gapDp) {
        LinearLayout.LayoutParams p = wrap();
        p.leftMargin = dp(row.getContext(), gapDp);
        row.addView(child, p);
        return child;
    }

    /** 가로 묶음: 정사각형(sizeDp) child를 gapDp 띄워 붙인다 (아이콘 버튼). */
    static <T extends View> T inline(LinearLayout row, T child, int gapDp, int sizeDp) {
        LinearLayout.LayoutParams p = square(row.getContext(), sizeDp);
        p.leftMargin = dp(row.getContext(), gapDp);
        row.addView(child, p);
        return child;
    }

    /** 가로 묶음: 남은 폭을 나눠 갖는 child를 붙인다. */
    static <T extends View> T inlineFill(LinearLayout row, T child, int gapDp) {
        LinearLayout.LayoutParams p = weight1();
        p.leftMargin = dp(row.getContext(), gapDp);
        row.addView(child, p);
        return child;
    }

    static View spacer(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    /** 가운데 정렬로 한 줄에 놓는다 (링크 버튼 등). */
    static LinearLayout centered(Context c, View child) {
        LinearLayout r = h(c);
        r.setGravity(Gravity.CENTER_HORIZONTAL);
        r.addView(child, wrap());
        return r;
    }

    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, Size.STROKE))));
        return v;
    }

    // ================================================================== 컨테이너

    /** Card: 흰 바탕 (radius 16, padding 16). */
    static LinearLayout card(Context c) {
        LinearLayout l = v(c);
        l.setBackground(round(c, WHITE, Radius.CARD));
        pad(l, c, Pad.CARD);
        return l;
    }

    /** 색 바탕 카드 (radius 16, padding 16). */
    static LinearLayout tintCard(Context c, int color) {
        LinearLayout l = card(c);
        l.setBackground(round(c, color, Radius.CARD));
        return l;
    }

    /** 테두리 있는 흰 카드 (S04 홈: radius 14, 1dp line). */
    static LinearLayout outlineCard(Context c, int padH, int padV) {
        LinearLayout l = v(c);
        // UI 갤러리 › 앱 UI 설정 › "카드 테두리"를 끄면 테두리 없이 흰 바탕만
        l.setBackground(roundStroke(c, WHITE, Radius.BOX, LINE));
        pad(l, c, padH, padV);
        return l;
    }

    /** 색 바탕 상자 (radius 14). */
    static LinearLayout tintBox(Context c, int color, int padH, int padV) {
        LinearLayout l = v(c);
        l.setBackground(round(c, color, Radius.BOX));
        pad(l, c, padH, padV);
        return l;
    }

    /** Sheet: 아래에서 올라오는 흰 시트 (위 모서리 24). padTop만 화면마다 다르다. */
    static LinearLayout bottomSheet(Context c, int padTop) {
        LinearLayout s = v(c);
        s.setBackground(corners(c, WHITE, Radius.SHEET, Radius.SHEET, 0, 0));
        pad(s, c, Pad.SHEET_H, padTop, Pad.SHEET_H, Pad.SHEET_BOTTOM);
        s.setClickable(true);
        return s;
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

    // ================================================================== 버튼

    /** Button/Primary · Button/Secondary (radius 14, 16 Bold). */
    static TextView button(Context c, String label, boolean primary, View.OnClickListener click) {
        TextView b = text(c, label, Font.BUTTON, primary ? ON_BRAND : TEXT, BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(primary ? round(c, BRAND, Radius.BOX) : roundStroke(c, WHITE, Radius.BOX, LINE));
        // Secondary는 1dp 테두리만큼 위아래를 줄여 Primary와 높이를 맞춘다.
        pad(b, c, Pad.BUTTON_H, primary ? Pad.BUTTON_V : Pad.BUTTON_V - 1);
        b.setOnClickListener(click);
        b.setClickable(true);
        return b;
    }

    /** 같은 폭 버튼 두 개를 나란히 (gap 8). */
    static LinearLayout buttonPair(Context c, TextView left, TextView right) {
        LinearLayout r = h(c);
        r.addView(left, weight1());
        inlineFill(r, right, Space.ITEM);
        return r;
    }

    /** 비활성 Primary 버튼. */
    static void setEnabled(TextView b, boolean on) {
        b.setEnabled(on);
        b.setAlpha(on ? 1f : 0.35f);
    }

    /** Button/Text (12 Medium muted, padding 8). */
    static TextView linkButton(Context c, String label, View.OnClickListener click) {
        TextView b = text(c, label, Font.CAPTION, MUTED, MEDIUM);
        b.setGravity(Gravity.CENTER);
        pad(b, c, Pad.LINK);
        b.setOnClickListener(click);
        return b;
    }

    /** 글자 링크 (13 Bold): "접근성 설정 열기 ›" 같은 이동 버튼. */
    static TextView textLink(Context c, String label, int color, View.OnClickListener click) {
        TextView t = text(c, label, Font.SMALL, color, BOLD);
        t.setOnClickListener(click);
        return t;
    }

    /** ← 뒤로 (17 Bold, 누르는 영역 24×24). */
    static TextView backButton(Context c, View.OnClickListener click) {
        TextView b = text(c, "←", Font.HEADING, TEXT, BOLD);
        b.setGravity(Gravity.CENTER);
        b.setContentDescription("뒤로");
        b.setOnClickListener(click);
        return b;
    }

    /** 톱니바퀴 설정 버튼 (홈 오른쪽 위). 아이콘 24dp + 누르는 여백 6 → square(c, Size.ICON_BUTTON + 2 * Pad.ICON)로 붙인다. */
    static ImageView settingsButton(Context c, View.OnClickListener click) {
        ImageView b = new ImageView(c);
        b.setImageDrawable(new GearDrawable(MUTED));
        pad(b, c, Pad.ICON);
        b.setContentDescription("설정");
        b.setOnClickListener(click);
        return b;
    }

    /** 톱니바퀴 모양: 몸통 원 + 이 8개 − 가운데 구멍. 이미지 파일 없이 코드로 그린다. */
    static final class GearDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        GearDrawable(int color) {
            paint.setColor(color);
        }

        @Override
        public void draw(Canvas canvas) {
            RectF b = new RectF(getBounds());
            float cx = b.centerX(), cy = b.centerY();
            float r = Math.min(b.width(), b.height()) / 2f * 0.9f; // 이 끝
            Path gear = new Path();
            gear.addCircle(cx, cy, r * 0.7f, Path.Direction.CW); // 몸통
            float tooth = r * 0.36f;
            for (int i = 0; i < 4; i++) { // 막대 4개를 45°씩 돌리면 이 8개
                Path bar = new Path();
                bar.addRoundRect(new RectF(cx - tooth / 2, cy - r, cx + tooth / 2, cy + r), tooth * 0.25f, tooth * 0.25f, Path.Direction.CW);
                Matrix m = new Matrix();
                m.setRotate(i * 45f, cx, cy);
                bar.transform(m);
                gear.op(bar, Path.Op.UNION);
            }
            Path hole = new Path();
            hole.addCircle(cx, cy, r * 0.3f, Path.Direction.CW);
            gear.op(hole, Path.Op.DIFFERENCE);
            canvas.drawPath(gear, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            paint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /** 하위 화면 윗줄: ← 뒤로 + 화면 제목 (기록 · 리포트 · 설정). */
    static LinearLayout screenHeader(Context c, String title, View.OnClickListener back) {
        LinearLayout r = h(c);
        r.addView(backButton(c, back), square(c, Size.ICON_BUTTON));
        inline(r, title(c, title), Space.ITEM);
        return r;
    }

    /** 섹션 머리말 + 오른쪽 글자 링크 ("최근 기록 · 더보기 ›"). */
    static LinearLayout sectionHeader(Context c, String title, String link, View.OnClickListener click) {
        LinearLayout r = h(c);
        r.addView(sectionTitle(c, title), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(textLink(c, link, MUTED, click));
        return r;
    }

    /** ✕ 닫기 (17 Bold subtle, padding 6). */
    static TextView closeButton(Context c, View.OnClickListener click) {
        TextView x = text(c, "✕", Font.HEADING, SUBTLE, BOLD);
        pad(x, c, Pad.ICON);
        x.setContentDescription("닫기");
        x.setOnClickListener(click);
        return x;
    }

    // ================================================================== 칩 · 태그

    /** Chip · Chip/Selected (radius 20, padding 10/16, 15 Medium). */
    static TextView chip(Context c, String label, boolean selected, View.OnClickListener click) {
        TextView t = text(c, label, Font.BODY, TEXT, MEDIUM);
        t.setGravity(Gravity.CENTER);
        setChipSelected(c, t, selected);
        pad(t, c, Pad.CHIP_H, Pad.CHIP_V);
        if (click != null) t.setOnClickListener(click);
        return t;
    }

    /** 칩 선택 모양만 바꾼다 (화면을 다시 그리지 않고 고를 때). */
    static void setChipSelected(Context c, TextView t, boolean selected) {
        t.setBackground(selected ? roundStroke(c, BRAND_LIGHT, Radius.CHIP, BRAND, Size.STROKE_STRONG) : roundStroke(c, WHITE, Radius.CHIP, LINE));
    }

    /** 서비스 색 칩 (리포트 회고 답): 고르면 초록 바탕 + 흰 글자. */
    static TextView accentChip(Context c, String label, boolean selected, View.OnClickListener click) {
        TextView t = chip(c, label, selected, click);
        setAccentChipSelected(c, t, selected);
        return t;
    }

    static void setAccentChipSelected(Context c, TextView t, boolean selected) {
        t.setBackground(selected ? round(c, GREEN, Radius.CHIP) : roundStroke(c, WHITE, Radius.CHIP, LINE));
        t.setTextColor(selected ? WHITE : TEXT);
    }

    /** Tag/Blue · Red · Amber · Green (radius 8, padding 4/10, 12 Medium). */
    static TextView tag(Context c, String label, Tone tone) {
        TextView t = text(c, label, Font.CAPTION, tone.fg, MEDIUM);
        t.setBackground(round(c, tone.bg, Radius.TAG));
        pad(t, c, Pad.TAG_H, Pad.TAG_V);
        return t;
    }

    /** 홈·온보딩 화면의 작은 태그 (radius 6, padding 3/8, 12 Bold). */
    static TextView smallTag(Context c, String label, Tone tone) {
        TextView t = text(c, label, Font.CAPTION, tone.fg, BOLD);
        t.setBackground(round(c, tone.bg, Radius.CHECK));
        pad(t, c, Pad.SMALL_TAG_H, Pad.SMALL_TAG_V);
        return t;
    }

    /** 칩을 줄바꿈하며 왼쪽부터 채운다 (Figma Chips: wrap, gap 8). */
    static FlowLayout chipFlow(Context c, List<TextView> chips) {
        FlowLayout f = new FlowLayout(c, dp(c, Space.ITEM), dp(c, Space.ITEM));
        for (TextView t : chips) f.addView(t, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return f;
    }

    /** 태그를 줄바꿈하며 놓는다 (gap 6). */
    static FlowLayout tagFlow(Context c) {
        return new FlowLayout(c, dp(c, Space.TIGHT), dp(c, Space.TIGHT));
    }

    /** 칩을 한 줄에 perRow개씩 같은 폭으로 배치 (설정 화면 등). */
    static LinearLayout chipGrid(Context c, List<TextView> chips, int perRow) {
        LinearLayout box = v(c);
        LinearLayout row = null;
        for (int i = 0; i < chips.size(); i++) {
            if (i % perRow == 0) {
                row = h(c);
                box.addView(row, fillW());
                if (i > 0) gapTop(row, c, Space.ITEM);
            }
            if (i % perRow == 0) row.addView(chips.get(i), weight1());
            else inlineFill(row, chips.get(i), Space.ITEM);
        }
        if (row != null && chips.size() % perRow != 0) {
            for (int k = chips.size() % perRow; k < perRow; k++) inlineFill(row, new View(c), Space.ITEM);
        }
        return box;
    }

    // ================================================================== 정보 표시

    /** 왼쪽 라벨 · 오른쪽 값 한 줄 (Figma Row: 15 Regular muted / 15 Medium primary, 합계는 15 Medium / 17 Bold). */
    static LinearLayout kv(Context c, String k, String v, boolean total) {
        LinearLayout r = h(c);
        r.addView(text(c, k, Font.BODY, total ? TEXT : MUTED, total ? MEDIUM : REGULAR), weight1());
        r.addView(text(c, v, total ? Font.HEADING : Font.BODY, TEXT, total ? BOLD : MEDIUM));
        return r;
    }

    /** 목록 한 줄 (S04 최근 기록): 왼쪽 제목 14 Medium · 부가 12, 오른쪽 금액 14 Bold · 태그. */
    static LinearLayout listRow(Context c, String title, String sub, String amount, View tag) {
        return listRow(c, 0, title, sub, amount, tag);
    }

    /** 목록 한 줄 + 왼쪽 색 점 (dotColor가 0이면 점 없음). 점은 제목 줄 가운데에 맞춘다. */
    static LinearLayout listRow(Context c, int dotColor, String title, String sub, String amount, View tag) {
        LinearLayout row = h(c);
        if (dotColor != 0) {
            row.setGravity(Gravity.TOP);
            View dot = new View(c);
            dot.setBackground(circle(dotColor));
            LinearLayout.LayoutParams dotLp = square(c, Size.LIST_DOT);
            dotLp.topMargin = dp(c, (lineHeight(Font.LABEL) - Size.LIST_DOT) / 2f);
            row.addView(dot, dotLp);
        }
        LinearLayout left = v(c);
        left.addView(text(c, title, Font.LABEL, TEXT, MEDIUM));
        stack(left, meta(c, sub), Space.TEXT);
        if (dotColor != 0) inlineFill(row, left, Space.ROW);
        else row.addView(left, weight1());
        LinearLayout right = v(c);
        right.setGravity(Gravity.END);
        right.addView(text(c, amount, Font.LABEL, TEXT, BOLD), wrap());
        if (tag != null) {
            right.addView(tag, wrap());
            gapTop(tag, c, Space.LINE);
        }
        inline(row, right, Space.BLOCK);
        return row;
    }

    /** 줄 전체에 연한 색 바탕을 깐다 (radius 10). 기록 줄 (결과 색). */
    static void tintRow(Context c, View row, int bg) {
        row.setBackground(round(c, bg, Radius.NOTE));
        pad(row, c, Pad.TINT_ROW_H, Pad.TINT_ROW_V);
    }

    /**
     * 요약 타일: 라벨 · 큰 값 · 한 줄 설명.
     * outlined=true는 홈(S04: 테두리 카드, 값 18, 설명 11 muted),
     * false는 리포트(S10: 흰 카드 16, 값 17, 설명 12 subtle).
     */
    static LinearLayout statTile(Context c, String label, String value, String sub, boolean outlined) {
        return statTile(c, label, value, sub, outlined, -1f, 0, 0);
    }

    /**
     * 요약 타일 + 값 아래 얇은 진행 막대 (ratio 0~1, 음수면 막대 없음).
     * 홈 S04: "남은 여윳돈", "식비 예산"이 얼마나 남았는지 보여 준다.
     * barTrack은 막대 바탕색 (다 썼을 때 연빨강으로 바꿔 빈 막대도 눈에 띄게).
     */
    static LinearLayout statTile(Context c, String label, String value, String sub, boolean outlined, float ratio, int barColor, int barTrack) {
        LinearLayout t;
        if (outlined) {
            t = outlineCard(c, Pad.TILE_H, Pad.TILE_V);
        } else {
            t = v(c);
            t.setBackground(round(c, WHITE, Radius.CARD));
            pad(t, c, Pad.REPORT_TILE);
        }
        t.addView(meta(c, label));
        stack(t, text(c, value, outlined ? Font.STAT : Font.HEADING, TEXT, BOLD), Space.TEXT);
        boolean withBar = ratio >= 0f;
        if (withBar) stack(t, bar(c, ratio, barColor, barTrack, Size.BAR_THIN), Space.TIGHT);
        stack(t, outlined ? micro(c, sub, MUTED) : text(c, sub, Font.CAPTION, SUBTLE, REGULAR), withBar ? Space.TIGHT : Space.TEXT);
        return t;
    }

    /** 타일 두 개를 같은 폭으로 나란히 (위쪽 맞춤). */
    static LinearLayout tilePair(Context c, View left, View right, int gapDp) {
        LinearLayout r = h(c);
        r.setGravity(Gravity.TOP);
        r.addView(left, weight1());
        inlineFill(r, right, gapDp);
        return r;
    }

    /**
     * 먹색 강조 카드: 라벨 12 · 큰 값 · 설명 12.
     * large=false는 온보딩 S03 "월 여윳돈"(값 22, padding 16), true는 리포트 S10 "마음 돌림 비율"(값 30, padding 20).
     */
    static LinearLayout heroCard(Context c, String label, String value, String sub, boolean large) {
        LinearLayout h = v(c);
        h.setBackground(round(c, HERO_BG, Radius.CARD));
        pad(h, c, large ? Pad.HERO_LARGE : Pad.HERO);
        h.addView(text(c, label, Font.CAPTION, ON_HERO_SUB, MEDIUM));
        stack(h, text(c, value, large ? Font.DISPLAY : Font.TITLE, ON_HERO, BOLD), Space.LINE);
        stack(h, text(c, sub, Font.CAPTION, ON_HERO_SUB, REGULAR), Space.LINE);
        return h;
    }

    /** 한국어 줄바꿈을 글자 단위 대신 어절 단위로 (Android 13+). 긴 문장 한 개를 크게 보여 줄 때. */
    static TextView wordBreak(TextView t) {
        if (Build.VERSION.SDK_INT >= 33) {
            t.setLineBreakWordStyle(android.graphics.text.LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE);
        }
        return t;
    }

    /**
     * 강조 카드 + 한 문장 (리포트 "이번 달 내 기준"): 서비스 색(쉼표 초록) 바탕,
     * 라벨 12 · 문장 22 Bold · 아래 줄들(heroRow, 반투명 흰 상자 안) · 부가 12.
     */
    static LinearLayout heroSentence(Context c, String label, String sentence, String sub, View... rows) {
        LinearLayout h = v(c);
        h.setBackground(round(c, HERO_BG, Radius.CARD));
        pad(h, c, Pad.HERO_LARGE);
        h.addView(text(c, label, Font.CAPTION, ON_HERO_SUB, MEDIUM));
        stack(h, wordBreak(text(c, sentence, Font.TITLE, ON_HERO, BOLD)), Space.ITEM);
        if (rows.length > 0) {
            LinearLayout box = v(c);
            box.setBackground(round(c, ON_HERO_BOX, Radius.INPUT));
            pad(box, c, Pad.BOX_H, Pad.BOX_V);
            for (int i = 0; i < rows.length; i++) {
                if (i == 0) box.addView(rows[i], fillW());
                else stack(box, rows[i], Space.ITEM);
            }
            stack(h, box, Space.BLOCK);
        }
        if (sub != null && !sub.isEmpty()) stack(h, wordBreak(text(c, sub, Font.CAPTION, ON_HERO_SUB, REGULAR)), Space.ITEM);
        return h;
    }

    /** 강조 카드 안 한 줄: 태그(만족 · 후회) · 이름 · 금액. */
    static LinearLayout heroRow(Context c, String tag, Tone tone, String name, String value) {
        LinearLayout r = h(c);
        r.addView(smallTag(c, tag, tone));
        inlineFill(r, text(c, name, Font.BODY, ON_HERO, MEDIUM), Space.ITEM);
        inline(r, text(c, value, Font.BODY, ON_HERO, BOLD), Space.ITEM);
        return r;
    }

    /** 내가 한 말 인용: 왼쪽 초록 선 + "말" + 부가 설명 (리포트 가치 회고). */
    static LinearLayout quote(Context c, String said, String sub) {
        LinearLayout row = h(c);
        row.setGravity(Gravity.TOP);
        View bar = new View(c);
        bar.setBackground(round(c, GREEN, Radius.BAR));
        row.addView(bar, new LinearLayout.LayoutParams(dp(c, Size.LIST_BAR), ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout texts = v(c);
        texts.addView(text(c, "“" + said + "”", Font.BODY, TEXT, MEDIUM));
        stack(texts, meta(c, sub), Space.LINE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.leftMargin = dp(c, Space.BLOCK);
        row.addView(texts, p);
        return row;
    }

    /** 경고 상자 (S04 결제 감지 꺼짐): 빨간 바탕, 제목 14 Bold · 설명 12 · 이동 링크. */
    static LinearLayout alertBox(Context c, String title, String desc, String action, View.OnClickListener click) {
        LinearLayout w = tintBox(c, RED_BG, Pad.BOX_H, Pad.BOX_V);
        w.addView(text(c, title, Font.LABEL, RED, BOLD));
        stack(w, text(c, desc, Font.CAPTION, RED, REGULAR), Space.TEXT);
        TextView open = textLink(c, action, RED, click);
        pad(open, c, 0, Space.ITEM, 0, 0);
        w.addView(open);
        return w;
    }

    /** 성과 상자 (S04 이번 달 쉼표): 연초록 바탕, 라벨 12 · 값 16 Bold. */
    static LinearLayout highlightBox(Context c, String label, String value) {
        LinearLayout b = tintBox(c, BRAND_LIGHT, Pad.BOX_H, Pad.BOX_V);
        b.addView(text(c, label, Font.CAPTION, GREEN, REGULAR));
        stack(b, text(c, value, Font.BUTTON, GREEN, BOLD), Space.LINE);
        return b;
    }

    /** 누르는 성과 상자 (S04 이번 달 쉼표 → 리포트): 왼쪽 라벨·값, 오른쪽 → 화살표. */
    static LinearLayout highlightBox(Context c, String label, String value, View.OnClickListener click) {
        LinearLayout b = tintBox(c, BRAND_LIGHT, Pad.BOX_H, Pad.BOX_V);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts = v(c);
        texts.addView(text(c, label, Font.CAPTION, GREEN, REGULAR));
        stack(texts, text(c, value, Font.BUTTON, GREEN, BOLD), Space.LINE);
        b.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        inline(b, text(c, "→", Font.HEADING, GREEN, BOLD), Space.ITEM);
        b.setOnClickListener(click);
        return b;
    }

    /** 연초록 안내 한 줄 (radius·글자는 자리마다 Figma 값이 달라 인자로 받는다). */
    static TextView tintNote(Context c, String s, float sp, int fg, int radius, int padH, int padV) {
        TextView t = text(c, s, sp, fg, MEDIUM);
        t.setBackground(round(c, BRAND_LIGHT, radius));
        pad(t, c, padH, padV);
        return t;
    }

    /** 사실 한 줄 상자: 연초록 바탕 + 왼쪽 3dp 초록 선 (radius 10, padding 8/12, 12 Medium). */
    static TextView factBox(Context c, String s) {
        TextView t = text(c, s, Font.CAPTION, TEXT, MEDIUM);
        Drawable line = round(c, GREEN, Radius.NOTE);
        Drawable fill = round(c, GREEN_BG, Radius.NOTE);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{line, fill});
        ld.setLayerInset(1, dp(c, 3), 0, 0, 0);
        t.setBackground(ld);
        pad(t, c, Pad.FACT_H, Pad.FACT_V);
        return t;
    }

    /** 진행 막대 (높이 8, radius 4). */
    static LinearLayout bar(Context c, float ratio, int fillColor, int trackColor) {
        return bar(c, ratio, fillColor, trackColor, Size.BAR);
    }

    /** 진행 막대 (높이 heightDp). */
    static LinearLayout bar(Context c, float ratio, int fillColor, int trackColor, int heightDp) {
        LinearLayout track = h(c);
        track.setBackground(round(c, trackColor, Radius.BAR));
        float r = Math.max(0f, Math.min(1f, ratio));
        View fill = new View(c);
        fill.setBackground(round(c, fillColor, Radius.BAR));
        track.addView(fill, new LinearLayout.LayoutParams(0, dp(c, heightDp), Math.max(0.0001f, r)));
        track.addView(new View(c), new LinearLayout.LayoutParams(0, dp(c, heightDp), Math.max(0.0001f, 1f - r)));
        if (r <= 0f) fill.setVisibility(View.INVISIBLE);
        return track;
    }

    /** 나눠진 막대: values 비율대로 colors를 이어 붙인다 (리포트 만족·후회). 합이 0이면 빈 막대. */
    static LinearLayout splitBar(Context c, long[] values, int[] colors) {
        LinearLayout track = h(c);
        track.setBackground(round(c, BG, Radius.BAR));
        track.setClipToOutline(true); // 양 끝만 둥글게
        long total = 0;
        for (long v : values) total += Math.max(0, v);
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0) continue;
            View part = new View(c);
            part.setBackgroundColor(colors[i]);
            track.addView(part, new LinearLayout.LayoutParams(0, dp(c, Size.BAR), values[i]));
        }
        if (total == 0) track.addView(new View(c), new LinearLayout.LayoutParams(0, dp(c, Size.BAR), 1f));
        return track;
    }

    /** 범례 한 줄: 색 점 · 이름 · 값 (리포트 만족·후회). */
    static LinearLayout legendRow(Context c, int color, String label, String value) {
        LinearLayout r = h(c);
        View dot = new View(c);
        dot.setBackground(circle(color));
        r.addView(dot, square(c, Size.LIST_DOT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = dp(c, Space.ITEM);
        r.addView(text(c, label, Font.BODY, MUTED, REGULAR), lp);
        r.addView(text(c, value, Font.BODY, TEXT, MEDIUM));
        return r;
    }

    // ================================================================== 대화 (개입 오버레이)

    /** Bubble/AI: "쉼표" 라벨 + 질문 (bg, 모서리 4/16/16/16). */
    static LinearLayout bubbleAi(Context c, String question, boolean faded) {
        LinearLayout b = v(c);
        b.setBackground(corners(c, BG, Radius.BAR, Radius.CARD, Radius.CARD, Radius.CARD));
        pad(b, c, Pad.BUBBLE_H, Pad.BUBBLE_AI_V);
        b.addView(text(c, "쉼표", Font.CAPTION, BRAND, MEDIUM));
        stack(b, text(c, question, Font.BODY, TEXT, MEDIUM), Space.LINE);
        if (faded) b.setAlpha(0.6f);
        LinearLayout row = h(c);
        row.setGravity(Gravity.TOP);
        // Figma: 말풍선 276 : 오른쪽 빈칸 44
        row.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 276f));
        row.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 44f));
        return row;
    }

    /** 쉼표 얼굴: 초록 동그라미 안 "쉼" (개입 채팅 ②·③). square(c, Size.AVATAR)로 붙인다. */
    static TextView avatar(Context c) {
        TextView t = text(c, "쉼", Font.CAPTION, ON_BRAND, BOLD);
        t.setGravity(Gravity.CENTER);
        t.setBackground(circle(GREEN));
        t.setIncludeFontPadding(false);
        return t;
    }

    /** 쉼표 얼굴 + 말풍선 (개입 채팅 ②·③): 얼굴이 "쉼표" 이름표를 대신한다. bg는 말풍선 바탕. */
    static LinearLayout bubbleAi(Context c, String question, boolean faded, int bg) {
        LinearLayout row = h(c);
        row.setGravity(Gravity.TOP);
        row.addView(avatar(c), square(c, Size.AVATAR));
        TextView b = text(c, question, Font.BODY, TEXT, MEDIUM);
        b.setBackground(corners(c, bg, Radius.BAR, Radius.CARD, Radius.CARD, Radius.CARD));
        pad(b, c, Pad.BUBBLE_H, Pad.BUBBLE_AI_V);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 276f);
        p.leftMargin = dp(c, Space.ITEM);
        row.addView(b, p);
        row.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 44f - Size.AVATAR));
        if (faded) row.setAlpha(0.6f);
        return row;
    }

    /** 내 답 말풍선 (색 지정). */
    static LinearLayout bubbleUser(Context c, String answer, int bg, int fg) {
        LinearLayout row = bubbleUser(c, answer);
        TextView b = (TextView) row.getChildAt(0);
        b.setTextColor(fg);
        b.setBackground(corners(c, bg, Radius.CARD, Radius.BAR, Radius.CARD, Radius.CARD));
        return row;
    }

    /** Bubble/User: 오른쪽 정렬 먹색 말풍선 (모서리 16/4/16/16). */
    static LinearLayout bubbleUser(Context c, String answer) {
        LinearLayout row = h(c);
        row.setGravity(Gravity.END);
        TextView b = text(c, answer, Font.BODY, ON_BRAND, MEDIUM);
        b.setBackground(corners(c, BRAND, Radius.CARD, Radius.BAR, Radius.CARD, Radius.CARD));
        pad(b, c, Pad.BUBBLE_H, Pad.BUBBLE_USER_V);
        row.addView(b, wrap());
        return row;
    }

    // ================================================================== 내비게이션 · 온보딩

    /** BottomNav: 위 1dp 선, 탭마다 6dp 점 + 12 Medium 글자. 선택된 탭만 점이 보인다.
     *  지금 앱에서는 안 쓴다 (하단 탭을 없애고 홈에서 더보기 · 이번 달 쉼표 · 톱니바퀴로 이동). 다시 쓸 때를 위해 남겨 둠. */
    static LinearLayout bottomNav(Context c, String[] labels, int selected, IntConsumer onSelect) {
        LinearLayout nav = v(c);
        nav.setBackgroundColor(WHITE);
        nav.addView(divider(c));
        LinearLayout row = h(c);
        pad(row, c, Size.NAV_INSET, 0);
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            boolean on = i == selected;
            LinearLayout item = v(c);
            item.setGravity(Gravity.CENTER);
            View dot = new View(c);
            dot.setBackground(circle(BRAND));
            dot.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
            item.addView(dot, square(c, Size.NAV_DOT));
            TextView t = text(c, labels[i], Font.CAPTION, on ? BRAND : SUBTLE, MEDIUM);
            item.addView(t, wrap());
            gapTop(t, c, Space.LINE);
            item.setOnClickListener(v -> onSelect.accept(idx));
            row.addView(item, new LinearLayout.LayoutParams(0, dp(c, Size.NAV_HEIGHT), 1f));
        }
        nav.addView(row, fillW());
        return nav;
    }

    /** 온보딩 단계 표시: 현재 단계는 24×8 막대, 나머지는 8×8 점 (gap 6). */
    static LinearLayout steps(Context c, int current, int total, boolean doneFilled) {
        LinearLayout r = h(c);
        for (int i = 1; i <= total; i++) {
            View d = new View(c);
            boolean on = i == current || (doneFilled && i < current);
            d.setBackground(round(c, on ? BRAND : LINE, Radius.BAR));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(c, i == current ? Size.STEP_ACTIVE : Size.STEP_DOT), dp(c, Size.STEP_DOT));
            if (i > 1) p.leftMargin = dp(c, Space.TIGHT);
            r.addView(d, p);
        }
        return r;
    }

    /** 20dp 체크 상자 + 문구 (Figma S02). */
    static LinearLayout checkRow(Context c, String label, boolean checked, View.OnClickListener click) {
        LinearLayout r = h(c);
        View box = new View(c);
        box.setBackground(checked ? round(c, BRAND, Radius.CHECK) : roundStroke(c, WHITE, Radius.CHECK, LINE, Size.STROKE_STRONG));
        r.addView(box, square(c, Size.CHECKBOX));
        inlineFill(r, text(c, label, Font.BODY, TEXT, REGULAR), Space.ROW);
        r.setOnClickListener(click);
        return r;
    }

    // ================================================================== 입력

    /** 테두리·바탕 없는 입력칸 (글자 15, padding 12/14). 다른 상자 안에 넣어 쓴다. */
    static EditText plainInput(Context c, String hint, String value) {
        EditText e = new EditText(c);
        e.setHint(hint);
        if (value != null) e.setText(value);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, Font.BODY);
        e.setTextColor(TEXT);
        e.setHintTextColor(SUBTLE);
        e.setSingleLine(true);
        e.setLetterSpacing(-0.01f);
        e.setBackground(null);
        pad(e, c, Pad.INPUT_H, Pad.INPUT_V);
        return e;
    }

    /** Input: 흰 바탕 + 1dp 테두리 (radius 12). */
    static EditText input(Context c, String hint, String value, boolean number) {
        EditText e = plainInput(c, hint, value);
        if (number) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setBackground(roundStroke(c, WHITE, Radius.INPUT, LINE));
        return e;
    }

    /** 왼쪽 라벨(15 muted) + 오른쪽 고정 폭 입력칸 (설정 화면). */
    static LinearLayout labeledField(Context c, String label, EditText in) {
        LinearLayout r = h(c);
        r.addView(text(c, label, Font.BODY, MUTED, REGULAR), weight1());
        r.addView(in, new LinearLayout.LayoutParams(dp(c, Size.FIELD_WIDTH), ViewGroup.LayoutParams.WRAP_CONTENT));
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
}
