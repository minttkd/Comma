package com.shimpyo.app;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.shimpyo.app.core.Enums.Outcome;
import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.KeywordJudge;
import com.shimpyo.app.core.Money;
import com.shimpyo.app.core.PaymentAttempt;
import com.shimpyo.app.core.Question;
import com.shimpyo.app.core.QuestionTemplates;
import com.shimpyo.app.core.Record;
import com.shimpyo.app.core.ScreenParser;
import com.shimpyo.app.core.Session;
import com.shimpyo.app.core.Settings;
import com.shimpyo.app.core.Stats;
import com.shimpyo.app.core.TriggerEngine;
import com.shimpyo.app.core.TriggerEngine.Evaluation;

import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 배민·쿠팡·무신사의 결제 준비 화면을 감지해 개입하는 접근성 서비스.
 * 흐름: 결제 준비 화면 감지(ScreenParser) → 결제 버튼 위에 투명 가드를 올림 → 사용자가 결제 버튼을 누르면
 * 그 순간의 최종 금액(쿠폰 적용 후)을 다시 읽어 품목 판정(AI/키워드) → 트리거·강도(TriggerEngine) → 오버레이.
 * 화면에 들어오거나 쿠폰으로 금액이 바뀔 때는 개입하지 않고, 결제 버튼을 누를 때만 개입한다.
 */
public class GuardService extends AccessibilityService implements OverlayController.Callbacks {

    static volatile GuardService instance;

    private static final long SESSION_TTL_MS = 20 * 60_000L;
    private static final long EXIT_AFTER_MS = 2 * 60_000L;
    private static final long SYNC_CACHE_MS = 60 * 60_000L;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Map<TargetApp, Boolean> scanScheduled = new EnumMap<>(TargetApp.class);
    private Store store;
    private OverlayController overlay;
    private Session session;
    private final ZoneId zone = ZoneId.systemDefault();
    private TargetApp visibleApp;
    /** 결제 화면이 처음 보인 시각 (0 = 결제 화면 아님). 화면 전환 직후의 클릭을 결제 버튼 클릭으로 오인하지 않게 한다. */
    private long checkoutSince;
    private boolean noButtonLogged;
    private final Runnable guardWatch = new Runnable() {
        @Override
        public void run() {
            if (overlay == null || !overlay.guardShown() || visibleApp == null) return;
            scan(visibleApp);
            if (overlay.guardShown()) main.postDelayed(this, 800);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        store = Store.get(this);
        overlay = new OverlayController(this, this);
        store.log("감지 서비스 연결됨 (v0.3 · 결제 버튼을 누를 때만 개입)");
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (overlay != null) {
            overlay.hide();
            overlay.hideGuard();
        }
        instance = null;
        super.onDestroy();
    }

    // ------------------------------------------------------------------ 이벤트

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null) return;
        TargetApp app = TargetApp.fromPackage(e.getPackageName());
        if (app == null) return;
        int type = e.getEventType();
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) maybeSync(app);
        if (type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            onClick(app, eventText(e));
            return;
        }
        scheduleScan(app, type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ? 150 : 400);
    }

    private static String eventText(AccessibilityEvent e) {
        StringBuilder sb = new StringBuilder();
        List<CharSequence> t = e.getText();
        if (t != null) for (CharSequence c : t) sb.append(c).append(' ');
        if (e.getContentDescription() != null) sb.append(e.getContentDescription());
        AccessibilityNodeInfo src = e.getSource();
        if (src != null) {
            // 버튼 글자가 클릭된 노드의 자식에 있는 경우가 많다 (예: 클릭 가능한 레이아웃 안의 "결제하기" 텍스트).
            Deque<AccessibilityNodeInfo> st = new ArrayDeque<>();
            st.push(src);
            int n = 0;
            while (!st.isEmpty() && n < 30) {
                AccessibilityNodeInfo x = st.pop();
                n++;
                if (x.getText() != null) sb.append(' ').append(x.getText());
                if (x.getContentDescription() != null) sb.append(' ').append(x.getContentDescription());
                for (int i = x.getChildCount() - 1; i >= 0; i--) {
                    AccessibilityNodeInfo c = x.getChild(i);
                    if (c != null) st.push(c);
                }
            }
        }
        return sb.toString();
    }

    private void scheduleScan(TargetApp app, long delay) {
        if (Boolean.TRUE.equals(scanScheduled.get(app))) return;
        scanScheduled.put(app, true);
        main.postDelayed(() -> {
            scanScheduled.put(app, false);
            scan(app);
        }, delay);
    }

    private void scan(TargetApp app) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null || !app.pkg.equals(root.getPackageName().toString())) {
            // 다른 앱(간편결제 앱 등)이 앞으로 왔거나 화면을 읽을 수 없다 → 가드를 반드시 내린다.
            overlay.hideGuard();
            return;
        }
        List<String> texts = collectTexts(root);
        ScreenParser.Result r = ScreenParser.parse(app, texts);
        store.setLastScreen(app, "[" + r.reason + ", 금액=" + r.amount + "]\n" + r.joined);
        if (r.complete) onComplete(app);
        if (r.checkout && r.amount > 0) {
            if (checkoutSince == 0 || visibleApp != app) {
                checkoutSince = System.currentTimeMillis();
                noButtonLogged = false;
            }
            visibleApp = app;
            updateGuard(app, root, r);
        } else {
            checkoutSince = 0;
            overlay.hideGuard();
            onNonCheckout(app);
        }
    }

    /** 결제 버튼의 클릭 가능한 노드를 찾는다. 여러 개면 화면 아래쪽 것. */
    static AccessibilityNodeInfo findPayNode(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo best = null;
        int bestTop = Integer.MIN_VALUE;
        Deque<AccessibilityNodeInfo> stack = new ArrayDeque<>();
        stack.push(root);
        int visited = 0;
        Rect r = new Rect();
        while (!stack.isEmpty() && visited < 1500) {
            AccessibilityNodeInfo n = stack.pop();
            visited++;
            String t = n.getText() == null ? null : n.getText().toString();
            String d = n.getContentDescription() == null ? null : n.getContentDescription().toString();
            if (ScreenParser.isPayButtonText(t) || ScreenParser.isPayButtonText(d)) {
                AccessibilityNodeInfo target = n;
                AccessibilityNodeInfo p = n;
                for (int i = 0; i < 4 && p != null; i++) {
                    if (p.isClickable()) {
                        target = p;
                        break;
                    }
                    p = p.getParent();
                }
                target.getBoundsInScreen(r);
                if (r.top > bestTop && r.width() > 0 && r.height() > 0) {
                    bestTop = r.top;
                    best = target;
                }
            }
            for (int i = n.getChildCount() - 1; i >= 0; i--) {
                AccessibilityNodeInfo c = n.getChild(i);
                if (c != null) stack.push(c);
            }
        }
        return best;
    }

    /**
     * 결제 버튼 위에 터치 가로채기 가드를 올린다. 이미 개입 없이 지나가도록 허용된 금액이거나,
     * 개입 오버레이가 떠 있거나, 버튼 위치를 못 찾으면 올리지 않는다.
     */
    private void updateGuard(TargetApp app, AccessibilityNodeInfo root, ScreenParser.Result r) {
        if (overlay.isShowing()) {
            overlay.hideGuard();
            return;
        }
        Session s = session;
        long now = System.currentTimeMillis();
        if (s != null && s.attempt != null && s.attempt.app == app && now - s.createdAt < SESSION_TTL_MS) {
            boolean sameAmount = s.attempt.amount == r.amount;
            if (sameAmount && (s.state == Session.State.ALLOWED || s.state == Session.State.PASSED)) {
                overlay.hideGuard();
                return;
            }
            if (s.state == Session.State.EVALUATING) return; // 판단 중에는 가드 유지
        }
        AccessibilityNodeInfo btn = findPayNode(root);
        Rect rect = new Rect();
        if (btn != null) btn.getBoundsInScreen(rect);
        if (btn == null || rect.width() < 24 || rect.height() < 24) {
            overlay.hideGuard();
            if (!noButtonLogged) {
                noButtonLogged = true;
                store.log(app.label + " 결제 버튼 위치를 못 찾음 → 버튼 클릭 감지로 대체 (결제가 먼저 시작될 수 있음)");
            }
            return;
        }
        boolean wasShown = overlay.guardShown();
        overlay.showGuard(rect, this::onGuardTap);
        if (!wasShown && overlay.guardShown()) {
            store.log(app.label + " 결제 화면 감지(" + Money.won(r.amount) + ") → 결제 버튼을 누를 때 개입");
            main.removeCallbacks(guardWatch);
            main.postDelayed(guardWatch, 800);
        }
    }

    /** 사용자가 (가드에 가려진) 결제 버튼을 눌렀다. 이 순간의 최종 금액으로 개입 여부를 판단한다. */
    private void onGuardTap() {
        TargetApp app = visibleApp;
        if (app == null) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null || !app.pkg.equals(root.getPackageName().toString())) {
            overlay.hideGuard();
            return;
        }
        ScreenParser.Result r = ScreenParser.parse(app, collectTexts(root));
        if (!r.checkout || r.amount <= 0) {
            overlay.hideGuard();
            return;
        }
        store.log(app.label + " 결제 버튼 터치 → 최종 결제금액 " + Money.won(r.amount));
        beginAttempt(app, r.amount, r.items, r.joined, true);
    }

    /** 개입이 필요 없을 때: 가로챈 터치를 그대로 결제 버튼에 전달한다. */
    private void forwardPayClick() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo btn = root == null ? null : findPayNode(root);
        boolean ok = btn != null && btn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (ok) {
            store.log("개입 없음 → 결제 버튼 터치를 그대로 전달");
        } else {
            store.log("터치 전달 실패 → 결제 버튼을 한 번 더 누르도록 안내");
            overlay.toast("결제 버튼을 한 번 더 눌러 주세요", "쉼표 확인은 끝났어요");
        }
    }

    static List<String> collectTexts(AccessibilityNodeInfo root) {
        List<String> out = new ArrayList<>();
        Deque<AccessibilityNodeInfo> stack = new ArrayDeque<>();
        stack.push(root);
        int visited = 0;
        while (!stack.isEmpty() && visited < 1500) {
            AccessibilityNodeInfo n = stack.pop();
            visited++;
            CharSequence t = n.getText();
            CharSequence d = n.getContentDescription();
            if (t != null && t.length() > 0) out.add(t.toString());
            if (d != null && d.length() > 0 && (t == null || !d.toString().equals(t.toString()))) out.add(d.toString());
            for (int i = n.getChildCount() - 1; i >= 0; i--) {
                AccessibilityNodeInfo c = n.getChild(i);
                if (c != null) stack.push(c);
            }
        }
        return out;
    }

    private void maybeSync(TargetApp app) {
        Settings s = store.settings();
        long now = System.currentTimeMillis();
        if (now - s.lastSyncTs > SYNC_CACHE_MS) {
            s.lastSyncTs = now;
            store.saveSettings(s);
            store.log(app.label + " 실행 → 계좌 동기화(Mock), 캐시 1시간");
        }
    }

    // ------------------------------------------------------------------ 결제 화면

    /**
     * 결제 버튼을 누른 시점의 최종 금액으로 개입 시도를 시작한다.
     * forwardTap=true면 쉼표가 가로챈 터치이므로, 개입이 필요 없을 때 터치를 대신 전달한다.
     */
    void beginAttempt(TargetApp app, long amount, List<String> items, String screenText, boolean forwardTap) {
        long now = System.currentTimeMillis();
        String key = app.name() + ":" + amount;
        if (session != null && session.key.equals(key) && now - session.createdAt < SESSION_TTL_MS) {
            switch (session.state) {
                case PAUSED:
                    store.log("X 후 결제 버튼 다시 터치 → 하던 질문부터 다시 표시");
                    reshow();
                    return;
                case ACTIVE:
                case EVALUATING:
                    return;
                case ALLOWED:
                case PASSED:
                    if (forwardTap) {
                        overlay.hideGuard();
                        forwardPayClick();
                    }
                    return;
                default:
                    break; // CLOSED: 새 시도로 본다
            }
        }
        if (session != null && session.state == Session.State.PAUSED) finalizeExit(session);
        if (session != null && session.state == Session.State.ACTIVE) overlay.hide();

        Session s = new Session(key, now);
        s.forwardTap = forwardTap;
        s.retryAfterStop = Session.isRetryAfterStop(session, app, now);
        if (s.retryAfterStop) store.log("방금 멈춘 뒤 다시 시도 → 첫 화면에 '아까는 멈추셨어요' 문구");
        session = s;
        PaymentAttempt a = new PaymentAttempt();
        a.app = app;
        a.amount = amount;
        a.items = new ArrayList<>(items);
        a.screenText = screenText;
        a.ts = now;
        s.attempt = a;
        store.log(app.label + " 개입 판단 시작: " + Money.won(amount) + (items.isEmpty() ? "" : " · " + items));
        worker.execute(() -> evaluate(s));
    }

    private void evaluate(Session s) {
        PaymentAttempt a = s.attempt;
        Settings st = store.settings();
        boolean ai = false;
        if (a.app == TargetApp.BAEMIN) {
            KeywordJudge.judge(a, "");
        } else {
            ai = ClaudeClient.judge(st, a);
            if (!ai) KeywordJudge.judge(a, a.items.isEmpty() ? a.screenText : String.join(" ", a.items));
        }
        if (a.title == null || a.title.isEmpty()) a.title = a.items.isEmpty() ? a.app.label + " 주문" : a.items.get(0)
                + (a.items.size() > 1 ? " 외 " + (a.items.size() - 1) + "개" : "");
        if (!ai && st.hasApiKey() && a.app != TargetApp.BAEMIN) store.log("AI 판정 실패 → 키워드 판정 (" + ClaudeClient.lastError + ")");

        long now = System.currentTimeMillis();
        List<Record> recs = store.records();
        Evaluation e = TriggerEngine.evaluate(a, st, recs, now, zone, TriggerEngine.interventionsToday(recs, now, zone));
        s.eval = e;
        String judged = a.app == TargetApp.BAEMIN ? "배민(판정 없음)" : a.judgment.label + (a.isFood ? "·식료품" : "") + " [" + a.judgedBy + "]";
        if (!e.intervene()) {
            s.state = Session.State.PASSED;
            store.log("개입 없음 · " + judged + (e.notes.isEmpty() ? "" : " · " + String.join(", ", e.notes))
                    + (e.lateHour ? " · 야식 " + e.lateCountNow + "번째" : ""));
            if (s.forwardTap) main.post(() -> {
                if (session != s) return;
                overlay.hideGuard();
                forwardPayClick();
            });
            return;
        }
        store.log("개입 · " + judged + " · " + e.triggers + " → " + e.intensity.label
                + (e.notes.isEmpty() ? "" : " (" + String.join(", ", e.notes) + ")"));
        if (e.intensity.questions > 1) s.questions.add(buildQuestion(st, s, 0));
        main.post(() -> {
            if (session != s) return;
            s.state = Session.State.ACTIVE;
            overlay.show(s);
        });
    }

    private Question buildQuestion(Settings st, Session s, int index) {
        QuestionTemplates.History h = history(s);
        Question q = ClaudeClient.nextQuestion(st, s, index, h);
        if (q == null) {
            if (st.hasApiKey()) store.log("AI 질문 실패/지연 → 기본 질문 (" + ClaudeClient.lastError + ")");
            q = QuestionTemplates.build(s.eval, s.attempt, index, h);
        }
        return q;
    }

    private QuestionTemplates.History history(Session s) {
        QuestionTemplates.History h = new QuestionTemplates.History();
        List<Record> recs = store.records();
        long cycleFrom = System.currentTimeMillis() - 31L * 86_400_000L;
        for (int i = recs.size() - 1; i >= 0; i--) {
            Record r = recs.get(i);
            if (r.app != s.attempt.app) continue;
            if (h.regretTitle == null && r.review != null && r.review.contains("후회")) h.regretTitle = r.title;
            if (r.outcome.spend && r.ts > cycleFrom) h.sameAppCount++;
            if ("습관처럼 시켰어요".equals(r.chip)) h.habitChosen = true;
        }
        return h;
    }

    void onNonCheckout(TargetApp app) {
        Session s = session;
        if (s == null || s.attempt == null || s.attempt.app != app) return;
        if (s.state == Session.State.PAUSED && !s.leftScreen) {
            s.leftScreen = true;
            s.pausedAt = System.currentTimeMillis();
            main.postDelayed(() -> {
                if (session == s && s.state == Session.State.PAUSED && s.leftScreen) finalizeExit(s);
            }, EXIT_AFTER_MS);
        }
    }

    /**
     * 앱에서 결제 버튼 클릭 이벤트가 왔다. 가드가 있으면 가로챈 터치는 이 이벤트를 만들지 않으므로,
     * 여기 오는 건 (a) 개입 없이 전달된 터치 (b) 허용 후 사용자의 두 번째 터치 (c) 가드를 못 올린 화면의 터치다.
     */
    void onClick(TargetApp app, String text) {
        if (!containsPayText(text)) return;
        long now = System.currentTimeMillis();
        Session s = session;
        if (s != null && s.attempt != null && s.attempt.app == app && now - s.createdAt < SESSION_TTL_MS) {
            if (s.state == Session.State.PAUSED) {
                store.log("X 후 결제 버튼 클릭 → 하던 질문부터 다시 표시");
                reshow();
                return;
            }
            if (s.state == Session.State.PASSED) {
                if (!s.recorded) recordPass(s);
                return;
            }
            if (s.state == Session.State.ACTIVE || s.state == Session.State.EVALUATING || s.state == Session.State.ALLOWED) return;
        }
        if (overlay.guardShown()) return;
        // 결제 화면이 막 열린 직후의 클릭(장바구니의 주문 버튼 등)은 결제 버튼 클릭이 아니다.
        if (checkoutSince == 0 || now - checkoutSince < 800) return;
        // 가드를 못 올린 화면(버튼 위치를 못 찾음): 클릭 시점의 최종 금액으로 개입한다. 결제가 이미 시작됐을 수 있다.
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null || !app.pkg.equals(root.getPackageName().toString())) return;
        ScreenParser.Result r = ScreenParser.parse(app, collectTexts(root));
        if (r.checkout && r.amount > 0) {
            store.log(app.label + " 결제 버튼 클릭 감지(가드 없음) → 개입 · 결제가 이미 시작됐을 수 있어요");
            beginAttempt(app, r.amount, r.items, r.joined, false);
        }
    }

    /** 클릭 이벤트 글자 안에 결제 버튼 문구가 있는지. 여러 노드 글자를 합친 문자열이라 단어 단위로 본다. */
    static boolean containsPayText(String text) {
        if (text == null) return false;
        if (ScreenParser.isPayButtonText(text)) return true;
        for (String part : text.split("\\s{2,}|\\n")) if (ScreenParser.isPayButtonText(part)) return true;
        return text.length() <= 60 && text.contains("결제하기");
    }

    void onComplete(TargetApp app) {
        Session s = session;
        if (s == null || s.attempt == null || s.attempt.app != app) return;
        if (s.state == Session.State.PASSED && !s.recorded) recordPass(s);
    }

    private void reshow() {
        Session s = session;
        if (s == null) return;
        s.leftScreen = false;
        s.state = Session.State.ACTIVE;
        overlay.hideGuard();
        overlay.show(s);
    }

    private void recordPass(Session s) {
        if (s.eval == null) return;
        s.recorded = true;
        Record r = toRecord(s, Outcome.PASS);
        store.addRecord(r);
        store.log("비개입 결제 기록: " + r.title + " " + Money.won(r.amount));
    }

    private void finalizeExit(Session s) {
        if (s.state != Session.State.PAUSED) return;
        s.state = Session.State.CLOSED;
        store.addRecord(toRecord(s, Outcome.EXIT));
        resetLateIfNeeded(s);
        store.log("결제 화면을 떠남 → 자기결정 이탈");
    }

    private Record toRecord(Session s, Outcome outcome) {
        Evaluation e = s.eval;
        PaymentAttempt a = s.attempt;
        Record r = new Record();
        r.ts = System.currentTimeMillis();
        r.app = a.app;
        r.title = a.shortTitle();
        r.amount = a.amount;
        r.judgment = a.app == TargetApp.BAEMIN ? null : a.judgment;
        r.isFood = a.isFood;
        r.triggers = e.triggers.clone();
        r.intensity = e.intensity;
        r.outcome = outcome;
        r.lateNight = e.lateHour;
        r.interventionNo = e.intervene() ? e.interventionNo : 0;
        if (outcome.spend) {
            r.discretionary = e.discretionary;
            r.food = e.food;
        }
        r.qa.addAll(s.transcript());
        return r;
    }

    private void resetLateIfNeeded(Session s) {
        if (s.eval == null || !s.eval.lateHour || !s.eval.intervene()) return;
        Settings st = store.settings();
        st.lateResetTs = System.currentTimeMillis();
        store.saveSettings(st);
        store.log("야식 주문에 개입 → 야식 카운트 초기화");
    }

    // ------------------------------------------------------------------ 오버레이 콜백

    @Override
    public void onStop(Session s) {
        if (s.state == Session.State.CLOSED) return;
        s.state = Session.State.CLOSED;
        s.stopped = true;
        s.stoppedAt = System.currentTimeMillis();
        Record r = toRecord(s, Outcome.PREVENTED);
        r.stoppedAt = s.eval.intensity.questions == 1 ? 1 : Math.min(s.index + 1, s.total());
        store.addRecord(r);
        resetLateIfNeeded(s);
        overlay.hide();
        overlay.hideGuard();
        performGlobalAction(GLOBAL_ACTION_BACK);
        Stats st = Stats.of(store.settings(), store.records(), System.currentTimeMillis(), zone);
        String l1 = Money.won(s.attempt.amount) + ", 쉼표 찍었어요";
        String l2 = "이번 달 " + st.prevented + "번 · " + Money.won(st.preventedSum) + " 아꼈어요";
        main.postDelayed(() -> overlay.toast(l1, l2), 400);
        store.log("그만둘게요 → 방지 실적 (" + r.stoppedAt + "번째 질문에서)");
    }

    @Override
    public void onReason(Session s, String chip) {
        if (s.state == Session.State.CLOSED) return;
        s.state = Session.State.ALLOWED;
        Record r = toRecord(s, Outcome.REASON_PAY);
        r.chip = chip;
        s.recorded = true;
        store.addRecord(r);
        resetLateIfNeeded(s);
        overlay.hide();
        afterAllowed(s);
        store.log("이유 칩 '" + chip + "' → 이유 선택 결제 (결제 진행 허용)");
    }

    @Override
    public void onBuy(Session s) {
        if (s.state == Session.State.CLOSED) return;
        s.state = Session.State.ALLOWED;
        s.recorded = true;
        store.addRecord(toRecord(s, Outcome.DELIBERATE_PAY));
        resetLateIfNeeded(s);
        overlay.hide();
        afterAllowed(s);
        store.log("그래도 살게요 → 숙고 후 결제 (결제 진행 허용)");
    }

    @Override
    public void onClose(Session s) {
        if (s.state != Session.State.ACTIVE) return;
        s.state = Session.State.PAUSED;
        s.pausedAt = System.currentTimeMillis();
        overlay.hide();
        if (visibleApp != null) scheduleScan(visibleApp, 300); // 결제 버튼 가드를 다시 올린다
        store.log("X → 오버레이만 닫힘 (결제 버튼을 누르면 다시 표시)");
    }

    /** 질문이 끝나 결제를 허용했다. 가드를 내리고 사용자가 직접 결제 버튼을 다시 누르게 한다. */
    private void afterAllowed(Session s) {
        overlay.hideGuard();
        if (s.forwardTap) main.postDelayed(() -> overlay.toast("결제 버튼을 한 번 더 눌러 주세요", "쉼표 질문은 끝났어요"), 300);
    }

    @Override
    public void requestNext(Session s) {
        Settings st = store.settings();
        int index = s.index;
        worker.execute(() -> {
            Question q = buildQuestion(st, s, index);
            main.post(() -> {
                while (s.questions.size() <= index) s.questions.add(null);
                s.questions.set(index, q);
                s.loading = false;
                if (overlay.current() == s && overlay.isShowing()) overlay.render();
            });
        });
    }

    // ------------------------------------------------------------------ 데모 결제 화면용

    /** 데모: 새 결제 화면에 들어왔다. 실제 앱처럼 진입만으로는 개입하지 않는다. */
    void demoCheckout(TargetApp app) {
        if (session != null && session.state == Session.State.PAUSED) finalizeExit(session);
        if (session != null && session.state == Session.State.ACTIVE) overlay.hide();
        session = null;
    }

    /** 데모 화면의 결제 버튼. true면 결제 진행 가능(허용·비개입). false면 판단·개입 중. */
    boolean demoPayClick(TargetApp app, long amount, List<String> items, String screenText) {
        long now = System.currentTimeMillis();
        String key = app.name() + ":" + amount;
        Session s = session;
        if (s != null && s.key.equals(key) && now - s.createdAt < SESSION_TTL_MS) {
            if (s.state == Session.State.PASSED) {
                if (!s.recorded) recordPass(s);
                return true;
            }
            if (s.state == Session.State.ALLOWED) return true;
        }
        beginAttempt(app, amount, items, screenText, false);
        return false;
    }

    void demoLeave(TargetApp app) {
        onNonCheckout(app);
    }

    String sessionState() {
        Session s = session;
        return s == null ? "없음" : s.state.name();
    }
}
