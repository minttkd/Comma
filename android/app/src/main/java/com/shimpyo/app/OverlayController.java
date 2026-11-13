package com.shimpyo.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.shimpyo.app.core.Enums.Decision;
import com.shimpyo.app.core.Enums.Intensity;
import com.shimpyo.app.core.Enums.Judgment;
import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.Enums.Trigger;
import com.shimpyo.app.core.Money;
import com.shimpyo.app.core.Question;
import com.shimpyo.app.core.QuestionTemplates;
import com.shimpyo.app.core.Session;

import java.util.ArrayList;
import java.util.List;

/**
 * 결제 화면 위 개입 오버레이 (S05 이유 칩 / S06 질문 대화 / S07 결정) 와 S08 토스트.
 * 접근성 서비스 전용 창(TYPE_ACCESSIBILITY_OVERLAY)이라 '다른 앱 위에 표시' 권한 없이 뜬다.
 * 화면 전체를 덮어 아래 결제 버튼이 눌리지 않게 한다.
 */
final class OverlayController {

    interface Callbacks {
        void onStop(Session s);

        void onReason(Session s, String chip);

        void onBuy(Session s);

        void onClose(Session s);

        /** 다음 질문을 준비해 달라 (AI 또는 기본 질문). 준비되면 render()를 다시 불러야 한다. */
        void requestNext(Session s);
    }

    private final Context ctx;
    private final WindowManager wm;
    private final Callbacks cb;
    private final Handler main = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private LinearLayout sheet;
    private Session session;
    private View toastView;
    private String draft = "";
    private View guardView;
    private WindowManager.LayoutParams guardLp;
    private Runnable guardTap;

    OverlayController(Context ctx, Callbacks cb) {
        this.ctx = ctx;
        this.cb = cb;
        this.wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
    }

    boolean isShowing() {
        return root != null;
    }

    Session current() {
        return session;
    }

    void show(Session s) {
        session = s;
        if (root == null) {
            buildRoot();
            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.BOTTOM;
            p.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
            try {
                wm.addView(root, p);
            } catch (RuntimeException e) {
                Store.get(ctx).log("오버레이 표시 실패: " + e.getMessage());
                root = null;
                return;
            }
        }
        render();
    }

    /** 어두운 바탕(Dim) + 아래 시트(Sheet). 결제 화면 위를 덮는다. */
    FrameLayout buildRoot() {
        root = new FrameLayout(ctx) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    if (session != null) cb.onClose(session);
                    return true;
                }
                return super.dispatchKeyEvent(event);
            }
        };
        root.setBackgroundColor(Ui.DIM);
        root.setClickable(true); // 아래 결제 화면 터치 차단
        sheet = Ui.v(ctx);
        sheet.setBackground(Ui.corners(ctx, Ui.WHITE, 24, 24, 0, 0));
        Ui.pad(sheet, ctx, 20, 18, 20, 24);
        sheet.setClickable(true);
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        root.addView(sheet, sp);
        return root;
    }

    /** 미리보기·테스트용: 창에 붙이지 않고 세션을 그린다. */
    FrameLayout preview(Session s) {
        session = s;
        buildRoot();
        render();
        return root;
    }

    void hide() {
        if (root != null) {
            try {
                wm.removeView(root);
            } catch (RuntimeException ignored) {
            }
        }
        root = null;
        sheet = null;
        draft = "";
    }

    // ------------------------------------------------------------------ 결제 버튼 가드

    boolean guardShown() {
        return guardView != null;
    }

    /**
     * 앱의 결제 버튼 위에 투명한 터치 가로채기 창을 올린다. 버튼이 눌리는 대신 onTap이 불린다.
     * 버튼 영역 밖의 터치는 그대로 아래 앱으로 간다.
     */
    void showGuard(android.graphics.Rect r, Runnable onTap) {
        guardTap = onTap;
        boolean visible = Store.get(ctx).settings().showGuard;
        if (guardView == null) {
            guardView = new View(ctx);
            guardView.setClickable(true);
            if (visible) {
                android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
                g.setColor(Color.argb(40, 0x37, 0x62, 0x4C));
                g.setStroke(Ui.dp(ctx, 3), Ui.GREEN);
                g.setCornerRadius(Ui.dp(ctx, 10));
                guardView.setBackground(g);
            }
            guardView.setOnClickListener(v -> {
                if (guardTap != null) guardTap.run();
            });
            guardLp = new WindowManager.LayoutParams(r.width(), r.height(),
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            guardLp.gravity = Gravity.TOP | Gravity.START;
            guardLp.x = r.left;
            guardLp.y = r.top;
            try {
                wm.addView(guardView, guardLp);
            } catch (RuntimeException e) {
                Store.get(ctx).log("결제 버튼 가드 표시 실패: " + e.getMessage());
                guardView = null;
            }
            return;
        }
        if (guardLp.x != r.left || guardLp.y != r.top || guardLp.width != r.width() || guardLp.height != r.height()) {
            guardLp.x = r.left;
            guardLp.y = r.top;
            guardLp.width = r.width();
            guardLp.height = r.height();
            try {
                wm.updateViewLayout(guardView, guardLp);
            } catch (RuntimeException ignored) {
            }
        }
    }

    void hideGuard() {
        if (guardView != null) {
            try {
                wm.removeView(guardView);
            } catch (RuntimeException ignored) {
            }
            guardView = null;
            guardLp = null;
        }
    }

    /** S08 처리 완료 토스트 (Figma Toast: 먹색 바탕, radius 14, 15 Medium / 12 Regular). */
    void toast(String line1, String line2) {
        removeToast();
        LinearLayout box = Ui.v(ctx);
        box.setBackground(Ui.round(ctx, Ui.BRAND, 14));
        box.setElevation(Ui.dp(ctx, 6));
        Ui.pad(box, ctx, 18, 14, 18, 14);
        box.addView(Ui.text(ctx, line1, 15, Ui.WHITE, Ui.MEDIUM));
        if (line2 != null && !line2.isEmpty()) {
            TextView t2 = Ui.text(ctx, line2, 12, Ui.LINE, Ui.REGULAR);
            box.addView(t2);
            Ui.gapTop(t2, ctx, 4);
        }
        FrameLayout wrap = new FrameLayout(ctx);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.leftMargin = Ui.dp(ctx, 20);
        bp.rightMargin = Ui.dp(ctx, 20);
        bp.bottomMargin = Ui.dp(ctx, 10);
        wrap.addView(box, bp);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.BOTTOM;
        p.y = Ui.dp(ctx, 100);
        try {
            wm.addView(wrap, p);
            toastView = wrap;
            main.postDelayed(this::removeToast, 3000);
        } catch (RuntimeException e) {
            Store.get(ctx).log("토스트 표시 실패: " + e.getMessage());
        }
    }

    private void removeToast() {
        if (toastView != null) {
            try {
                wm.removeView(toastView);
            } catch (RuntimeException ignored) {
            }
            toastView = null;
        }
    }

    // ------------------------------------------------------------------ 그리기 (Figma A2~A4 · B2 · C2~C3)

    private void add(View v, int gapDp) {
        sheet.addView(v, Ui.fillW());
        if (gapDp > 0) Ui.gapTop(v, ctx, gapDp);
    }

    void render() {
        if (sheet == null || session == null) return;
        Session s = session;
        sheet.removeAllViews();
        add(headerRow(s), 0);
        // 방금 멈춘 뒤 다시 결제를 누른 경우: 첫 화면에만 맥락 한 줄을 붙인다.
        if (s.retryAfterStop && s.answers.isEmpty()) {
            TextView note = Ui.text(ctx, QuestionTemplates.retryLine(s.attempt.app), 13, Ui.BRAND, Ui.MEDIUM);
            note.setBackground(Ui.round(ctx, Ui.BRAND_LIGHT, 10));
            Ui.pad(note, ctx, 12, 10, 12, 10);
            add(note, 14);
        }
        if (s.eval.intensity == Intensity.LIGHT) renderLight(s);
        else renderDialogue(s);
    }

    private static String intensityLabel(Intensity i) {
        return i.label + (i == Intensity.LIGHT ? " ●○○" : i == Intensity.MEDIUM ? " ●●○" : " ●●●");
    }

    /** Header: 트리거 태그 · (필요 판정) · 강도 태그 · 진행 표시 · X */
    private View headerRow(Session s) {
        LinearLayout row = Ui.h(ctx);
        FlowLayout tags = new FlowLayout(ctx, Ui.dp(ctx, 6), Ui.dp(ctx, 6));
        for (Trigger t : s.eval.triggers) {
            tags.addView(t.financial ? Ui.tag(ctx, t.label, Ui.RED_BG, Ui.RED) : Ui.tag(ctx, t.label, Ui.AMBER_BG, Ui.AMBER));
        }
        if (s.attempt.judgment == Judgment.NEED && s.attempt.app != TargetApp.BAEMIN) {
            tags.addView(Ui.tag(ctx, "필요", Ui.GREEN_BG, Ui.GREEN));
        }
        Intensity in = s.eval.intensity;
        tags.addView(in == Intensity.STRONG ? Ui.tag(ctx, intensityLabel(in), Ui.BRAND, Ui.WHITE) : Ui.tag(ctx, intensityLabel(in), Ui.BRAND_LIGHT, Ui.BRAND));
        row.addView(tags, Ui.weight1());
        if (in != Intensity.LIGHT) {
            int total = s.total();
            String step = s.inDecisionStep() ? "결정" : Math.min(s.index + 1, total) + " / " + total;
            TextView st = Ui.text(ctx, step, 12, Ui.SUBTLE, Ui.MEDIUM);
            LinearLayout.LayoutParams sp = Ui.wrap();
            sp.leftMargin = Ui.dp(ctx, 6);
            row.addView(st, sp);
        }
        TextView x = Ui.text(ctx, "✕", 17, Ui.SUBTLE, Ui.BOLD);
        Ui.pad(x, ctx, 6, 6, 6, 6);
        x.setContentDescription("닫기");
        x.setOnClickListener(v -> cb.onClose(s));
        LinearLayout.LayoutParams xp = Ui.wrap();
        xp.leftMargin = Ui.dp(ctx, 6);
        row.addView(x, xp);
        return row;
    }

    /** 제목 아래 사실 한 줄: 계산값만 쓴다 (AI 아님). */
    private String factBefore(Session s, boolean withAmount) {
        if (s.eval.has(Trigger.FIN_RISK) && s.eval.budget != null) {
            long after = s.eval.budget.todaySpent + s.eval.discretionary;
            String head = withAmount ? Money.won(s.attempt.amount) + "을 결제하면" : "결제하면";
            return head + " 오늘 " + Money.won(after) + " · 오늘 한도 " + Money.won(s.eval.budget.limit);
        }
        return QuestionTemplates.factLine(s.eval, s.attempt);
    }

    /** B2 가벼움: 고정 질문 1개 + 이유 칩. */
    private void renderLight(Session s) {
        boolean baemin = s.attempt.app == TargetApp.BAEMIN;
        String head = QuestionTemplates.lightHeadline(s.eval, s.attempt).replace("결제하면 ", "").trim();
        if (head.endsWith(".")) head = head.substring(0, head.length() - 1);
        add(Ui.text(ctx, head, 17, Ui.TEXT, Ui.BOLD), 14);
        add(Ui.factBox(ctx, factBefore(s, false)), 14);
        add(Ui.text(ctx, baemin ? "지금 시키려는 이유가 뭐예요?" : "지금 사려는 이유가 뭐예요?", 15, Ui.MUTED, Ui.REGULAR), 14);
        List<TextView> chips = new ArrayList<>();
        for (String c : QuestionTemplates.lightChips(s.eval, s.attempt)) {
            chips.add(Ui.chip(ctx, c, false, v -> cb.onReason(s, c)));
        }
        add(Ui.chipFlow(ctx, chips), 14);
        add(Ui.button(ctx, "그만둘게요", true, v -> cb.onStop(s)), 14);
    }

    /** A2·A3·C2 질문 대화, A4·C3 결정 화면. */
    private void renderDialogue(Session s) {
        if (s.inDecisionStep()) {
            renderDecision(s);
            return;
        }
        add(Ui.text(ctx, "잠깐, 쉼표 하나 찍을까요?", 17, Ui.TEXT, Ui.BOLD), 14);
        if (s.answers.isEmpty()) add(Ui.factBox(ctx, factBefore(s, true)), 14);

        // 대화 영역: 내용만큼 늘어나고, 길어지면 화면의 45%까지만 차지하고 스크롤
        int maxH = (int) (ctx.getResources().getDisplayMetrics().heightPixels * 0.45);
        Ui.MaxHeightScrollView scroll = new Ui.MaxHeightScrollView(ctx, maxH);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout conv = Ui.v(ctx);
        scroll.addView(conv);
        int n = Math.min(s.answers.size(), s.questions.size());
        for (int i = 0; i < n; i++) {
            View q = Ui.bubbleAi(ctx, s.questions.get(i).text, true);
            conv.addView(q, Ui.fillW());
            if (i > 0) Ui.gapTop(q, ctx, 14);
            View a = Ui.bubbleUser(ctx, s.answers.get(i));
            conv.addView(a, Ui.fillW());
            Ui.gapTop(a, ctx, 14);
        }
        Question q = s.current();
        if (q == null || s.loading) {
            TextView think = Ui.text(ctx, "쉼표가 다음 질문을 고르는 중…", 15, Ui.SUBTLE, Ui.MEDIUM);
            conv.addView(think, Ui.fillW());
            if (n > 0) Ui.gapTop(think, ctx, 14);
        } else {
            View b = Ui.bubbleAi(ctx, q.text, false);
            conv.addView(b, Ui.fillW());
            if (n > 0) Ui.gapTop(b, ctx, 14);
        }
        add(scroll, 14);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));

        if (q != null && !s.loading) {
            if (q.requireInput) {
                renderRequiredInput(s);
                return;
            }
            if (!q.chips.isEmpty()) {
                List<TextView> chips = new ArrayList<>();
                for (Question.Chip c : q.chips) chips.add(Ui.chip(ctx, c.label, false, v -> answer(s, c.label, c.decision)));
                add(Ui.chipFlow(ctx, chips), 14);
            }
            add(optionalInput(s), 14);
        }
        add(Ui.button(ctx, "그만둘게요", true, v -> cb.onStop(s)), 14);
    }

    /** Input (선택): 회색 바탕, 입력하면 오른쪽에 [보내기]가 나타난다. 키보드의 보내기로도 전송. */
    private View optionalInput(Session s) {
        LinearLayout box = Ui.h(ctx);
        box.setBackground(Ui.round(ctx, Ui.BG, 12));
        EditText in = bareInput("직접 입력 (선택)");
        box.addView(in, Ui.weight1());
        TextView send = Ui.text(ctx, "보내기", 15, Ui.BRAND, Ui.BOLD);
        Ui.pad(send, ctx, 10, 12, 14, 12);
        send.setVisibility(draft.trim().isEmpty() ? View.GONE : View.VISIBLE);
        box.addView(send);
        Runnable submit = () -> {
            String t = in.getText().toString().trim();
            if (t.length() < 2) {
                in.setError("두 글자 이상 적어 주세요");
                return;
            }
            draft = "";
            answer(s, t, Decision.NONE);
        };
        send.setOnClickListener(v -> submit.run());
        in.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence a, int b, int c, int d) {}
            public void onTextChanged(CharSequence a, int b, int c, int d) {}
            public void afterTextChanged(android.text.Editable e) {
                draft = e.toString();
                send.setVisibility(draft.trim().isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
        in.setOnEditorActionListener((v, actionId, ev) -> {
            submit.run();
            return true;
        });
        return box;
    }

    /** C2 Input (필수): 먹색 1.5dp 테두리 + 안내 문구, [다음] 보조 버튼. */
    private void renderRequiredInput(Session s) {
        LinearLayout box = Ui.v(ctx);
        box.setBackground(Ui.roundStroke(ctx, Ui.WHITE, 12, Ui.BRAND, 1.5f));
        EditText in = bareInput("직접 입력해 주세요");
        in.setSingleLine(false);
        in.setMaxLines(3);
        box.addView(in, Ui.fillW());
        TextView help = Ui.text(ctx, "이 질문은 직접 입력해야 다음으로 넘어가요", 12, Ui.BRAND, Ui.REGULAR);
        Ui.pad(help, ctx, 14, 0, 14, 12);
        box.addView(help, Ui.fillW());
        in.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence a, int b, int c, int d) {}
            public void onTextChanged(CharSequence a, int b, int c, int d) {}
            public void afterTextChanged(android.text.Editable e) {
                draft = e.toString();
            }
        });
        add(box, 14);
        add(Ui.button(ctx, "다음", false, v -> {
            String t = in.getText().toString().trim();
            if (t.length() < 2) {
                in.setError("두 글자 이상 적어 주세요");
                return;
            }
            draft = "";
            answer(s, t, Decision.NONE);
        }), 14);
        add(Ui.button(ctx, "그만둘게요", true, v -> cb.onStop(s)), 14);
    }

    private EditText bareInput(String hint) {
        EditText in = new EditText(ctx);
        in.setHint(hint);
        in.setText(draft);
        in.setSelection(draft.length());
        in.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
        in.setTextColor(Ui.TEXT);
        in.setHintTextColor(Ui.SUBTLE);
        in.setLetterSpacing(-0.01f);
        in.setBackground(null);
        in.setSingleLine(true);
        in.setInputType(InputType.TYPE_CLASS_TEXT);
        in.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
        Ui.pad(in, ctx, 14, 12, 14, 12);
        return in;
    }

    /** 질문 유형을 '내 답' 요약의 짧은 이름으로. */
    private static String shortName(Question q, int index) {
        if (q == null || q.type == null) return "질문 " + (index + 1);
        switch (q.type) {
            case "필요vs욕구": return "이유";
            case "시점": return "미루기";
            case "패턴": return "야식 패턴";
            case "과거 연결": return "지난번과 다른 점";
            case "미래의 나": return "급여일까지 지내기";
            default: return q.type;
        }
    }

    /** A4·C3 결정 화면: 마지막 답에 결정이 없을 때만. */
    private void renderDecision(Session s) {
        add(Ui.text(ctx, "어떻게 할까요?", 22, Ui.TEXT, Ui.BOLD), 14);
        add(Ui.factBox(ctx, QuestionTemplates.factLine(s.eval, s.attempt)), 14);
        LinearLayout mine = Ui.v(ctx);
        mine.setBackground(Ui.round(ctx, Ui.BG, 14));
        Ui.pad(mine, ctx, 14, 14, 14, 14);
        mine.addView(Ui.caption(ctx, "내 답"));
        int n = Math.min(s.answers.size(), s.questions.size());
        for (int i = 0; i < n; i++) {
            TextView line = Ui.text(ctx, shortName(s.questions.get(i), i) + " → " + s.answers.get(i), 15, Ui.TEXT, Ui.REGULAR);
            mine.addView(line, Ui.fillW());
            Ui.gapTop(line, ctx, 6);
        }
        add(mine, 14);
        add(Ui.button(ctx, "그만둘게요", true, v -> cb.onStop(s)), 14);
        TextView buy = Ui.linkButton(ctx, s.attempt.app == TargetApp.BAEMIN ? "그래도 시킬게요" : "그래도 살게요", v -> cb.onBuy(s));
        LinearLayout buyRow = Ui.h(ctx);
        buyRow.setGravity(Gravity.CENTER_HORIZONTAL);
        buyRow.addView(buy, Ui.wrap());
        add(buyRow, 14);
    }

    private void answer(Session s, String text, Decision chipDecision) {
        Decision d = s.answer(text, chipDecision);
        if (s.index < s.total()) {
            s.loading = true;
            render();
            cb.requestNext(s);
            return;
        }
        if (d == Decision.STOP) cb.onStop(s);
        else if (d == Decision.BUY) cb.onBuy(s);
        else render();
    }
}
