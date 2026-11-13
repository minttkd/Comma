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
            root.setBackgroundColor(Color.parseColor("#99000000"));
            root.setClickable(true); // 아래 결제 화면 터치 차단
            sheet = Ui.v(ctx);
            sheet.setBackground(Ui.round(ctx, Ui.WHITE, 22));
            Ui.pad(sheet, ctx, 22, 20, 22, 24);
            sheet.setClickable(true);
            FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            root.addView(sheet, sp);

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
                g.setColor(Color.argb(40, 46, 112, 237));
                g.setStroke(Ui.dp(ctx, 3), Color.rgb(46, 112, 237));
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

    /** S08 처리 완료 토스트 */
    void toast(String line1, String line2) {
        removeToast();
        LinearLayout box = Ui.v(ctx);
        box.setBackground(Ui.round(ctx, Ui.DARK, 14));
        Ui.pad(box, ctx, 18, 14, 18, 14);
        box.addView(Ui.text(ctx, line1, 15, Ui.WHITE, true));
        if (line2 != null && !line2.isEmpty()) {
            TextView t2 = Ui.text(ctx, line2, 12, Color.parseColor("#C9CBD1"), false);
            box.addView(t2);
            Ui.gapTop(t2, ctx, 4);
        }
        FrameLayout wrap = new FrameLayout(ctx);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.leftMargin = Ui.dp(ctx, 20);
        bp.rightMargin = Ui.dp(ctx, 20);
        wrap.addView(box, bp);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.BOTTOM;
        p.y = Ui.dp(ctx, 110);
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

    // ------------------------------------------------------------------ 그리기

    void render() {
        if (sheet == null || session == null) return;
        Session s = session;
        sheet.removeAllViews();
        sheet.addView(headerRow(s), Ui.fillW());
        // 방금 멈춘 뒤 다시 결제를 누른 경우: 첫 화면에만 맥락 한 줄을 붙인다.
        if (s.retryAfterStop && s.answers.isEmpty()) {
            TextView note = Ui.text(ctx, QuestionTemplates.retryLine(s.attempt.app), 13, Ui.BLUE, true);
            note.setBackground(Ui.round(ctx, Ui.BLUE_LIGHT, 10));
            Ui.pad(note, ctx, 12, 10, 12, 10);
            sheet.addView(note, Ui.fillW());
            Ui.gapTop(note, ctx, 10);
        }
        if (s.eval.intensity == Intensity.LIGHT) renderLight(s);
        else renderDialogue(s);
    }

    private View headerRow(Session s) {
        LinearLayout row = Ui.h(ctx);
        LinearLayout tags = Ui.h(ctx);
        for (Trigger t : s.eval.triggers) {
            TextView tag = t.financial ? Ui.tag(ctx, t.label, Ui.RED_BG, Ui.RED) : Ui.tag(ctx, t.label, Ui.AMBER_BG, Ui.AMBER);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.rightMargin = Ui.dp(ctx, 6);
            tags.addView(tag, p);
        }
        if (s.attempt.judgment == Judgment.NEED && s.attempt.app != TargetApp.BAEMIN) {
            tags.addView(Ui.tag(ctx, "필요 판정 · 한 단계 낮춤", Ui.GRAY_BG, Ui.MUTED));
        }
        row.addView(tags, Ui.weight1());
        TextView x = Ui.text(ctx, "✕", s.eval.intensity == Intensity.LIGHT ? 22 : 18, Ui.MUTED, false);
        Ui.pad(x, ctx, 10, 2, 2, 2);
        x.setContentDescription("닫기");
        x.setOnClickListener(v -> cb.onClose(s));
        row.addView(x);
        return row;
    }

    private void renderLight(Session s) {
        TextView title = Ui.text(ctx, "잠깐, 이번엔 어떤 이유예요?", 20, Ui.DARK, true);
        sheet.addView(title, Ui.fillW());
        Ui.gapTop(title, ctx, 12);
        TextView sub = Ui.text(ctx, QuestionTemplates.lightHeadline(s.eval, s.attempt) + " 이유를 고르면 바로 결제돼요.", 14, Ui.MUTED, false);
        sheet.addView(sub, Ui.fillW());
        Ui.gapTop(sub, ctx, 8);
        List<TextView> chips = new ArrayList<>();
        for (String c : QuestionTemplates.lightChips(s.eval, s.attempt)) {
            chips.add(Ui.chip(ctx, c, false, v -> cb.onReason(s, c)));
        }
        View grid = Ui.chipGrid(ctx, chips, 2);
        sheet.addView(grid, Ui.fillW());
        Ui.gapTop(grid, ctx, 16);
        TextView stop = Ui.button(ctx, "그만둘게요", true, v -> cb.onStop(s));
        sheet.addView(stop, Ui.fillW());
        Ui.gapTop(stop, ctx, 18);
        TextView foot = Ui.text(ctx, "고른 이유는 기록만 하고, 다음 날 한 번 물어볼게요.", 12, Ui.MUTED, false);
        foot.setGravity(Gravity.CENTER);
        sheet.addView(foot, Ui.fillW());
        Ui.gapTop(foot, ctx, 10);
    }

    private void renderDialogue(Session s) {
        int total = s.total();
        LinearLayout titleRow = Ui.h(ctx);
        titleRow.addView(Ui.text(ctx, "잠깐, 쉼표 하나 찍을까요?", 19, Ui.DARK, true), Ui.weight1());
        String step = s.inDecisionStep() ? "결정" : "질문 " + Math.min(s.index + 1, total) + "/" + total;
        titleRow.addView(Ui.text(ctx, step, 12, Ui.BLUE, true));
        sheet.addView(titleRow, Ui.fillW());
        Ui.gapTop(titleRow, ctx, 12);

        // 대화 영역 (스크롤)
        ScrollView scroll = new ScrollView(ctx);
        LinearLayout conv = Ui.v(ctx);
        Ui.pad(conv, ctx, 0, 4, 0, 4);
        scroll.addView(conv);
        for (int i = 0; i < s.answers.size() && i < s.questions.size(); i++) {
            conv.addView(aiBubble(s.questions.get(i)));
            conv.addView(userBubble(s.answers.get(i), s.questions.get(i).requireInput));
        }
        Question q = s.current();
        if (!s.inDecisionStep()) {
            if (q == null || s.loading) {
                TextView think = Ui.text(ctx, "질문을 고르는 중…", 13, Ui.MUTED, false);
                conv.addView(think);
                Ui.gapTop(think, ctx, 10);
            } else {
                conv.addView(aiBubble(q));
            }
        }
        int maxH = (int) (ctx.getResources().getDisplayMetrics().heightPixels * 0.40);
        sheet.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxH));
        Ui.gapTop(scroll, ctx, 8);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));

        if (s.inDecisionStep()) {
            TextView fact = Ui.text(ctx, QuestionTemplates.factLine(s.eval, s.attempt), 14, Ui.DARK, true);
            fact.setBackground(Ui.round(ctx, Ui.BG, 10));
            Ui.pad(fact, ctx, 12, 12, 12, 12);
            sheet.addView(fact, Ui.fillW());
            Ui.gapTop(fact, ctx, 10);
            TextView stop = Ui.button(ctx, "그만둘게요", true, v -> cb.onStop(s));
            sheet.addView(stop, Ui.fillW());
            Ui.gapTop(stop, ctx, 12);
            TextView buy = Ui.linkButton(ctx, s.attempt.app == TargetApp.BAEMIN ? "그래도 시킬게요" : "그래도 살게요", v -> cb.onBuy(s));
            sheet.addView(buy, Ui.fillW());
            return;
        }

        if (q != null && !s.loading) {
            if (!q.requireInput && !q.chips.isEmpty()) {
                List<TextView> chips = new ArrayList<>();
                for (Question.Chip c : q.chips) chips.add(Ui.chip(ctx, c.label, false, v -> answer(s, c.label, c.decision)));
                View grid = Ui.chipGrid(ctx, chips, q.chips.size() >= 3 ? 3 : 2);
                sheet.addView(grid, Ui.fillW());
                Ui.gapTop(grid, ctx, 10);
            }
            LinearLayout inRow = Ui.h(ctx);
            EditText in = Ui.input(ctx, q.requireInput ? "직접 입력해 주세요 (필수)" : "직접 쓰기 (선택)", draft, false);
            in.setInputType(InputType.TYPE_CLASS_TEXT);
            inRow.addView(in, Ui.weight1());
            TextView send = Ui.text(ctx, "보내기", 14, Ui.BLUE, true);
            Ui.pad(send, ctx, 14, 10, 4, 10);
            send.setOnClickListener(v -> {
                String t = in.getText().toString().trim();
                if (t.length() < 2) {
                    in.setError("두 글자 이상 적어 주세요");
                    return;
                }
                draft = "";
                answer(s, t, Decision.NONE);
            });
            inRow.addView(send);
            sheet.addView(inRow, Ui.fillW());
            Ui.gapTop(inRow, ctx, 10);
        }
        TextView stop = Ui.button(ctx, "그만둘게요", true, v -> cb.onStop(s));
        sheet.addView(stop, Ui.fillW());
        Ui.gapTop(stop, ctx, 14);
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

    private View aiBubble(Question q) {
        LinearLayout wrap = Ui.h(ctx);
        TextView b = Ui.text(ctx, q.text, 14, Ui.DARK, false);
        b.setBackground(Ui.round(ctx, Ui.BG, 14));
        Ui.pad(b, ctx, 14, 11, 14, 11);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.86f);
        wrap.addView(b, p);
        View sp = new View(ctx);
        wrap.addView(sp, new LinearLayout.LayoutParams(0, 1, 0.14f));
        LinearLayout.LayoutParams wp = Ui.fillW();
        wp.topMargin = Ui.dp(ctx, 10);
        wrap.setLayoutParams(wp);
        return wrap;
    }

    private View userBubble(String text, boolean typed) {
        LinearLayout col = Ui.v(ctx);
        col.setGravity(Gravity.END);
        if (typed) {
            TextView label = Ui.text(ctx, "직접 입력", 11, Ui.MUTED, false);
            col.addView(label);
        }
        TextView b = Ui.text(ctx, text, 14, Ui.BLUE, true);
        b.setBackground(Ui.round(ctx, Ui.BLUE_LIGHT, 14));
        Ui.pad(b, ctx, 14, 10, 14, 10);
        col.addView(b);
        LinearLayout.LayoutParams wp = Ui.fillW();
        wp.topMargin = Ui.dp(ctx, 8);
        col.setLayoutParams(wp);
        return col;
    }
}
