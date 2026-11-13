package com.shimpyo.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.shimpyo.app.core.BudgetEngine;
import com.shimpyo.app.core.Enums.Intensity;
import com.shimpyo.app.core.Enums.Outcome;
import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.Money;
import com.shimpyo.app.core.Record;
import com.shimpyo.app.core.Settings;
import com.shimpyo.app.core.Stats;
import com.shimpyo.app.core.TriggerEngine;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 온보딩(S01~S03) · 홈(S04) · 다음 날 회고(S09) · 리포트(S10) · 설정/테스트 도구.
 * 화면은 코드로 그리고, 모양은 Figma "쉼표 Hi-Fi 프로토타입"을 따른다.
 */
public class MainActivity extends Activity {

    private Store store;
    private final ZoneId zone = ZoneId.systemDefault();
    private LinearLayout content;
    private ScrollView scroll;
    private LinearLayout footer;
    private LinearLayout nav;
    private FrameLayout layer;
    private View reviewSheet;
    private int tab = 0;
    private int onboardStep = 0;
    private boolean bankConsent = true;
    private boolean cycleConsent = true;
    private boolean showAllDemos = false;
    private boolean reviewDismissed = false;

    /** 상태바·내비게이션바를 앱 바탕색에 맞춘다 (Figma StatusBar: 밝은 바탕 + 어두운 글자). */
    static void styleWindow(Activity a) {
        Window w = a.getWindow();
        w.setStatusBarColor(Ui.BG);
        int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) {
            w.setNavigationBarColor(Ui.WHITE);
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        w.getDecorView().setSystemUiVisibility(flags);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        styleWindow(this);
        setContentView(buildLayout());
        if (!store.settings().onboarded) onboardStep = 1;
    }

    /** 화면 뼈대(스크롤 내용 · 하단 버튼 영역 · 탭)를 만든다. */
    View buildLayout() {
        store = Store.get(this);
        layer = new FrameLayout(this);
        LinearLayout root = Ui.v(this);
        root.setBackgroundColor(Ui.BG);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        content = Ui.v(this);
        Ui.pad(content, this, 20, 12, 20, 24);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        footer = Ui.v(this);
        Ui.pad(footer, this, 20, 8, 20, 24);
        root.addView(footer, Ui.fillW());
        nav = Ui.h(this);
        root.addView(nav, Ui.fillW());
        layer.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return layer;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onBackPressed() {
        if (reviewSheet != null) {
            reviewDismissed = true;
            hideReview();
            return;
        }
        if (onboardStep > 1) {
            onboardStep--;
            refresh();
            return;
        }
        if (onboardStep == 0 && tab != 0) {
            tab = 0;
            refresh();
            return;
        }
        super.onBackPressed();
    }

    void refresh() {
        content.removeAllViews();
        footer.removeAllViews();
        hideReview();
        scroll.scrollTo(0, 0);
        if (onboardStep > 0) {
            nav.setVisibility(View.GONE);
            footer.setVisibility(View.VISIBLE);
            if (onboardStep == 1) s01();
            else if (onboardStep == 2) s02();
            else s03();
            return;
        }
        footer.setVisibility(View.GONE);
        nav.setVisibility(View.VISIBLE);
        buildNav();
        if (tab == 0) home();
        else if (tab == 1) records();
        else if (tab == 2) report();
        else settingsTab();
    }

    /** BottomNav: 높이 64, 위 1dp 선, 탭마다 6dp 점 + 12 Medium 글자. 선택된 탭만 점이 보인다. */
    private void buildNav() {
        nav.removeAllViews();
        nav.setOrientation(LinearLayout.VERTICAL);
        nav.setBackgroundColor(Ui.WHITE);
        nav.addView(Ui.divider(this));
        LinearLayout row = Ui.h(this);
        Ui.pad(row, this, 28, 0, 28, 0);
        String[] labels = {"홈", "기록", "리포트", "설정"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            boolean on = i == tab;
            LinearLayout item = Ui.v(this);
            item.setGravity(Gravity.CENTER);
            View dot = new View(this);
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            g.setColor(Ui.BRAND);
            dot.setBackground(g);
            dot.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
            item.addView(dot, new LinearLayout.LayoutParams(Ui.dp(this, 6), Ui.dp(this, 6)));
            TextView t = Ui.text(this, labels[i], 12, on ? Ui.BRAND : Ui.SUBTLE, Ui.MEDIUM);
            item.addView(t, Ui.wrap());
            Ui.gapTop(t, this, 4);
            item.setOnClickListener(v -> {
                tab = idx;
                refresh();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, Ui.dp(this, 63), 1f);
            row.addView(item, p);
        }
        nav.addView(row, Ui.fillW());
    }

    // ------------------------------------------------------------------ 공통

    private void add(View v, int gapDp) {
        content.addView(v, Ui.fillW());
        if (gapDp > 0) Ui.gapTop(v, this, gapDp);
    }

    private TextView h1(String s) {
        return Ui.text(this, s, 22, Ui.TEXT, Ui.BOLD);
    }

    private TextView body(String s) {
        return Ui.text(this, s, 15, Ui.MUTED, Ui.REGULAR);
    }

    boolean serviceEnabled() {
        if (GuardService.instance != null) return true;
        String enabled = Secure.getString(getContentResolver(), Secure.ENABLED_ACCESSIBILITY_SERVICES);
        ComponentName me = new ComponentName(this, GuardService.class);
        return enabled != null && (enabled.contains(me.flattenToString()) || enabled.contains(me.flattenToShortString()));
    }

    void openA11ySettings() {
        startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    /** "오늘 12:30" · "어제 19:20" · "11/10 23:10" */
    private String time(long ts) {
        LocalDate d = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate();
        LocalDate today = LocalDate.now(zone);
        String hm = new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(ts));
        if (d.equals(today)) return "오늘 " + hm;
        if (d.equals(today.minusDays(1))) return "어제 " + hm;
        return new SimpleDateFormat("M/d HH:mm", Locale.KOREA).format(new Date(ts));
    }

    private static String appShort(TargetApp app) {
        if (app == null) return "계좌";
        return app == TargetApp.BAEMIN ? "배민" : app.label;
    }

    private static String recordTitle(Record r) {
        return r.app == null ? r.title : appShort(r.app) + " · " + r.title;
    }

    private TextView outcomeTag(Record r) {
        switch (r.outcome) {
            case PREVENTED:
                return Ui.smallTag(this, r.outcome.label, Ui.GREEN_BG, Ui.GREEN);
            case REASON_PAY:
                return Ui.smallTag(this, r.outcome.label, Ui.BRAND_LIGHT, Ui.GREEN);
            case DELIBERATE_PAY:
                return Ui.smallTag(this, r.outcome.label, Ui.AMBER_BG, Ui.AMBER);
            case SYNC:
                return r.food > 0 ? Ui.smallTag(this, "식비 예산", Ui.GREEN_BG, Ui.GREEN) : Ui.smallTag(this, "여윳돈", Ui.BG, Ui.MUTED);
            default:
                if (r.food > 0 && r.discretionary == 0) return Ui.smallTag(this, "식비 예산", Ui.GREEN_BG, Ui.GREEN);
                return Ui.smallTag(this, r.outcome.label, Ui.BG, Ui.MUTED);
        }
    }

    /** S04 최근 기록 한 줄: 왼쪽 제목(14 Medium)·시각(12), 오른쪽 금액(14 Bold)·태그. */
    private View recordRow(Record r, boolean deletable) {
        LinearLayout row = Ui.h(this);
        LinearLayout left = Ui.v(this);
        left.addView(Ui.text(this, recordTitle(r), 14, Ui.TEXT, Ui.MEDIUM));
        String sub = time(r.ts);
        if (r.intensity != null) sub += " · " + r.intensity.label;
        if (r.lateNight) sub += " · 야식";
        if (r.chip != null) sub += " · \"" + r.chip + "\"";
        TextView st = Ui.text(this, sub, 12, Ui.MUTED, Ui.REGULAR);
        left.addView(st);
        Ui.gapTop(st, this, 2);
        row.addView(left, Ui.weight1());
        LinearLayout right = Ui.v(this);
        right.setGravity(Gravity.END);
        right.addView(Ui.text(this, Money.won(r.amount), 14, Ui.TEXT, Ui.BOLD), Ui.wrap());
        TextView tag = outcomeTag(r);
        right.addView(tag, Ui.wrap());
        Ui.gapTop(tag, this, 4);
        LinearLayout.LayoutParams rp = Ui.wrap();
        rp.leftMargin = Ui.dp(this, 12);
        row.addView(right, rp);
        if (deletable) {
            row.setOnLongClickListener(v -> {
                store.deleteRecord(r.id);
                toast("기록을 지웠어요");
                refresh();
                return true;
            });
        }
        return row;
    }

    /** 온보딩 윗줄: ← 뒤로 + 단계 점. */
    private View topRow(int step) {
        LinearLayout r = Ui.h(this);
        if (step > 1) {
            TextView back = Ui.text(this, "←", 17, Ui.TEXT, Ui.BOLD);
            back.setGravity(Gravity.CENTER);
            back.setContentDescription("뒤로");
            back.setOnClickListener(v -> {
                onboardStep = step - 1;
                refresh();
            });
            r.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24)));
            LinearLayout.LayoutParams p = Ui.wrap();
            p.leftMargin = Ui.dp(this, 12);
            r.addView(Ui.steps(this, step, 3, true), p);
        } else {
            Ui.pad(r, this, 0, 14, 0, 10);
            r.addView(Ui.steps(this, step, 3, true));
        }
        return r;
    }

    private void footerButton(TextView b) {
        footer.addView(b, Ui.fillW());
    }

    // ------------------------------------------------------------------ S01~S03 온보딩

    private void s01() {
        boolean on = serviceEnabled();
        add(topRow(1), 0);
        add(Ui.text(this, "결제 직전에\n쉼표 하나 찍어드릴게요", 22, Ui.TEXT, Ui.BOLD), 8);
        add(Ui.text(this, "이번 달 돈 흐름에 무리가 될 때만 결제 화면 위에서 질문을 건네요. 결정은 언제나 내가 해요.", 13, Ui.MUTED, Ui.REGULAR), 14);
        LinearLayout c = Ui.card(this);
        c.setBackground(Ui.round(this, Ui.WHITE, 14));
        c.addView(permRow("필수", "접근성 서비스", "결제 화면의 상품명과 금액을 읽어요", on), Ui.fillW());
        View r2 = permRow("필수", "결제 화면 위 질문 표시", "결제 화면 위에 질문을 띄워요 (접근성 서비스로 함께 허용돼요)", on);
        c.addView(r2, Ui.fillW());
        Ui.gapTop(r2, this, 14);
        View r3 = permRow("선택", "알림", "다음 날 어제 결제를 돌아보자고 물어봐요", false);
        c.addView(r3, Ui.fillW());
        Ui.gapTop(r3, this, 14);
        add(c, 14);
        TextView note = Ui.text(this, "비밀번호·카드번호·메시지는 읽지 않아요. 상품명과 금액만 판단에 써요.", 12, Ui.GREEN, Ui.MEDIUM);
        note.setBackground(Ui.round(this, Ui.BRAND_LIGHT, 14));
        Ui.pad(note, this, 12, 12, 12, 12);
        add(note, 14);

        if (on) {
            footerButton(Ui.button(this, "다음", true, v -> {
                onboardStep = 2;
                refresh();
            }));
        } else {
            footerButton(Ui.button(this, "허용하러 가기", true, v -> openA11ySettings()));
            TextView later = Ui.linkButton(this, "나중에 켤게요 (설정 탭에서 켤 수 있어요)", v -> {
                onboardStep = 2;
                refresh();
            });
            footer.addView(later, Ui.fillW());
            Ui.gapTop(later, this, 4);
        }
    }

    private View permRow(String level, String title, String desc, boolean ok) {
        LinearLayout r = Ui.h(this);
        r.setGravity(Gravity.TOP);
        r.addView(level.equals("필수") ? Ui.smallTag(this, level, Ui.BRAND_LIGHT, Ui.GREEN) : Ui.smallTag(this, level, Ui.BG, Ui.MUTED), Ui.wrap());
        LinearLayout t = Ui.v(this);
        t.addView(Ui.text(this, title + (ok ? "  ✓" : ""), 14, Ui.TEXT, Ui.BOLD));
        TextView d = Ui.text(this, desc, 12, Ui.MUTED, Ui.REGULAR);
        t.addView(d);
        Ui.gapTop(d, this, 2);
        LinearLayout.LayoutParams tp = Ui.weight1();
        tp.leftMargin = Ui.dp(this, 10);
        r.addView(t, tp);
        return r;
    }

    private void s02() {
        Settings s = store.settings();
        add(topRow(2), 0);
        add(Ui.text(this, "정기 수입을\n확인했어요", 22, Ui.TEXT, Ui.BOLD), 16);
        add(body("최근 6개월 입출금 내역으로 매달 들어오는 돈을 찾았어요. (데모: Mock 계좌)"), 16);

        LinearLayout acc = Ui.card(this);
        acc.addView(Ui.caption(this, "연결된 계좌"));
        TextView an = Ui.text(this, "쉼표은행 입출금 ****1234 (데모)", 15, Ui.TEXT, Ui.MEDIUM);
        acc.addView(an);
        Ui.gapTop(an, this, 6);
        add(acc, 16);

        LinearLayout inc = Ui.card(this);
        inc.setBackground(Ui.round(this, Ui.BRAND_LIGHT, 16));
        inc.addView(Ui.text(this, "정기 수입", 12, Ui.BRAND, Ui.MEDIUM));
        TextView iv = Ui.text(this, "매달 " + s.payday + "일 · 약 " + Money.won(s.income), 17, Ui.TEXT, Ui.BOLD);
        inc.addView(iv);
        Ui.gapTop(iv, this, 8);
        TextView idt = Ui.text(this, s.income == 1_000_000 ? "용돈 600,000원 + 과외비 400,000원 (6개월 연속)" : "최근 6개월 연속 같은 날 들어왔어요", 12, Ui.MUTED, Ui.REGULAR);
        inc.addView(idt);
        Ui.gapTop(idt, this, 8);
        add(inc, 16);

        LinearLayout cc = Ui.card(this);
        cc.addView(Ui.checkRow(this, "계좌 입출금 내역 조회 동의 (필수)", bankConsent, v -> {
            bankConsent = !bankConsent;
            refresh();
        }), Ui.fillW());
        View c2 = Ui.checkRow(this, "급여일 기준으로 예산 주기 설정", cycleConsent, v -> {
            cycleConsent = !cycleConsent;
            refresh();
        });
        cc.addView(c2, Ui.fillW());
        Ui.gapTop(c2, this, 10);
        add(cc, 16);

        TextView next = Ui.button(this, "다음", true, v -> {
            if (!bankConsent) return;
            onboardStep = 3;
            refresh();
        });
        Ui.setEnabled(next, bankConsent);
        footerButton(next);
    }

    private static String man(long v) {
        return v % 10_000 == 0 ? (v / 10_000) + "만" : Money.num(v);
    }

    private View amountRow(String k, long v, boolean total) {
        LinearLayout r = Ui.h(this);
        r.addView(Ui.text(this, k, 15, total ? Ui.TEXT : Ui.MUTED, total ? Ui.MEDIUM : Ui.REGULAR), Ui.weight1());
        r.addView(Ui.text(this, Money.won(v), total ? 17 : 15, Ui.TEXT, total ? Ui.BOLD : Ui.MEDIUM));
        return r;
    }

    private void s03() {
        Settings s = store.settings();
        add(topRow(3), 0);
        add(Ui.text(this, "이미 정해진 돈부터\n떼어 둘게요", 22, Ui.TEXT, Ui.BOLD), 12);

        LinearLayout fc = Ui.card(this);
        fc.addView(Ui.caption(this, "고정지출 5항목 (계좌 내역으로 추정)"));
        String[] labels = {"월세·관리비", "공과금", "통신비", "교통비", "구독"};
        long[] vals = {s.rent, s.utilities, s.phone, s.transport, s.subscriptions};
        for (int i = 0; i < 5; i++) {
            View r = amountRow(labels[i], vals[i], false);
            fc.addView(r, Ui.fillW());
            Ui.gapTop(r, this, 10);
        }
        View ft = amountRow("합계", s.fixedTotal(), true);
        fc.addView(ft, Ui.fillW());
        Ui.gapTop(ft, this, 10);
        add(fc, 12);

        LinearLayout food = Ui.card(this);
        food.addView(Ui.caption(this, "식비 예산 (해 먹는 재료)"));
        View m1 = amountRow("마트 장보기", s.martBudget, false);
        food.addView(m1, Ui.fillW());
        Ui.gapTop(m1, this, 10);
        View m2 = amountRow("쿠팡 식료품 (첫 달 임시값)", s.coupangFoodBudget, false);
        food.addView(m2, Ui.fillW());
        Ui.gapTop(m2, this, 10);
        View m3 = amountRow("합계", s.foodBudget(), true);
        food.addView(m3, Ui.fillW());
        Ui.gapTop(m3, this, 10);
        add(food, 12);

        long b = Math.round(s.monthlyDiscretionary() / 30.0 / 100.0) * 100;
        LinearLayout sum = Ui.card(this);
        sum.setBackground(Ui.round(this, Ui.BRAND, 16));
        sum.addView(Ui.text(this, "월 여윳돈", 12, Ui.LINE, Ui.MEDIUM));
        TextView mv = Ui.text(this, Money.won(s.monthlyDiscretionary()), 22, Ui.WHITE, Ui.BOLD);
        sum.addView(mv);
        Ui.gapTop(mv, this, 4);
        TextView md = Ui.text(this, "수입 " + man(s.income) + " − 고정 " + man(s.fixedTotal()) + " − 식비 " + man(s.foodBudget())
                + " · 하루 기준 약 " + Money.won(b), 12, Ui.LINE, Ui.REGULAR);
        sum.addView(md);
        Ui.gapTop(md, this, 4);
        add(sum, 12);

        footerButton(Ui.button(this, "쉼표 시작하기", true, v -> {
            Settings st = store.settings();
            st.onboarded = true;
            store.saveSettings(st);
            onboardStep = 0;
            tab = 0;
            refresh();
        }));
    }

    // ------------------------------------------------------------------ S04 홈 (+ S09 회고 시트)

    private void home() {
        Settings s = store.settings();
        long now = System.currentTimeMillis();
        List<Record> recs = store.records();
        BudgetEngine.State b = BudgetEngine.compute(s, recs, now, zone);
        Stats st = Stats.of(s, recs, now, zone);

        LinearLayout top = Ui.h(this);
        top.addView(Ui.text(this, "쉼표", 22, Ui.TEXT, Ui.BOLD), Ui.weight1());
        long mins = s.lastSyncTs == 0 ? -1 : (now - s.lastSyncTs) / 60_000;
        String sync = mins < 0 ? "동기화 전" : mins < 1 ? "마지막 동기화 방금 전" : "마지막 동기화 " + (mins < 60 ? mins + "분 전" : (mins / 60) + "시간 전");
        top.addView(Ui.text(this, sync, 12, Ui.MUTED, Ui.REGULAR));
        add(top, 0);

        if (!serviceEnabled()) {
            LinearLayout warn = Ui.v(this);
            warn.setBackground(Ui.round(this, Ui.RED_BG, 14));
            Ui.pad(warn, this, 16, 14, 16, 14);
            warn.addView(Ui.text(this, "결제 감지가 꺼져 있어요", 14, Ui.RED, Ui.BOLD));
            TextView d = Ui.text(this, "설정 › 접근성에서 '쉼표'를 켜야 배민·쿠팡·무신사 결제 화면에서 질문이 떠요.", 12, Ui.RED, Ui.REGULAR);
            warn.addView(d);
            Ui.gapTop(d, this, 2);
            TextView open = Ui.text(this, "접근성 설정 열기 ›", 13, Ui.RED, Ui.BOLD);
            Ui.pad(open, this, 0, 8, 0, 0);
            open.setOnClickListener(v -> openA11ySettings());
            warn.addView(open);
            add(warn, 12);
        }

        // 오늘 쓸 수 있는 돈
        LinearLayout today = Ui.outlineCard(this, 18, 18);
        LinearLayout tr = Ui.h(this);
        tr.addView(Ui.text(this, "오늘 쓸 수 있는 돈", 13, Ui.MUTED, Ui.REGULAR), Ui.weight1());
        int sig = b.signal();
        tr.addView(sig == 0 ? Ui.smallTag(this, "여유", Ui.GREEN_BG, Ui.GREEN)
                : sig == 1 ? Ui.smallTag(this, "주의", Ui.AMBER_BG, Ui.AMBER) : Ui.smallTag(this, "초과", Ui.RED_BG, Ui.RED));
        today.addView(tr, Ui.fillW());
        TextView big = Ui.text(this, b.available >= 0 ? Money.won(b.available) + " 남음" : Money.won(-b.available) + " 넘었어요", 28, Ui.TEXT, Ui.BOLD);
        today.addView(big);
        Ui.gapTop(big, this, 8);
        float ratio = b.limit == 0 ? 1f : Math.min(1f, (float) b.todaySpent / b.limit);
        View bar = Ui.bar(this, ratio, b.todaySpent > b.limit ? Ui.RED : Ui.BRAND, Ui.BG);
        today.addView(bar, Ui.fillW());
        Ui.gapTop(bar, this, 8);
        TextView cap = Ui.text(this, "오늘 한도 " + Money.won(b.limit) + " 중 " + Money.won(b.todaySpent) + " 사용 · 주기 " + b.dayIndex + "일째", 12, Ui.MUTED, Ui.REGULAR);
        today.addView(cap);
        Ui.gapTop(cap, this, 8);
        add(today, 12);

        LinearLayout minis = Ui.h(this);
        minis.setGravity(Gravity.TOP);
        minis.addView(mini("남은 여윳돈", Money.won(b.remainingNow), "급여일까지 " + b.daysLeft + "일"), Ui.weight1());
        LinearLayout.LayoutParams mp = Ui.weight1();
        mp.leftMargin = Ui.dp(this, 12);
        minis.addView(mini("식비 예산", Money.won(b.foodRemaining), man(b.foodBudget) + " 원 중 남음"), mp);
        add(minis, 12);

        LinearLayout saved = Ui.v(this);
        saved.setBackground(Ui.round(this, Ui.BRAND_LIGHT, 14));
        Ui.pad(saved, this, 16, 14, 16, 14);
        saved.addView(Ui.text(this, "이번 달 쉼표", 12, Ui.GREEN, Ui.REGULAR));
        TextView sv = Ui.text(this, st.prevented == 0 ? "결제 직전에 멈춘 기록이 여기 쌓여요"
                : st.prevented + "번 멈춰서 " + Money.won(st.preventedSum) + " 아꼈어요", 16, Ui.GREEN, Ui.BOLD);
        saved.addView(sv);
        Ui.gapTop(sv, this, 4);
        add(saved, 12);

        add(Ui.text(this, "최근 기록", 15, Ui.TEXT, Ui.BOLD), 12);
        LinearLayout list = Ui.outlineCard(this, 16, 16);
        List<Record> sorted = new ArrayList<>(recs);
        Collections.sort(sorted, (x, y) -> Long.compare(y.ts, x.ts));
        int n = 0;
        for (Record r : sorted) {
            if (n >= 5) break;
            View row = recordRow(r, false);
            list.addView(row, Ui.fillW());
            if (n > 0) Ui.gapTop(row, this, 12);
            n++;
        }
        if (sorted.isEmpty()) list.addView(Ui.text(this, "아직 기록이 없어요. 아래 데모 결제로 체험해 보세요.", 13, Ui.MUTED, Ui.REGULAR));
        add(list, 12);

        add(demoCard(), 12);

        maybeShowReview(recs, now);
    }

    private View mini(String label, String value, String sub) {
        LinearLayout c = Ui.outlineCard(this, 16, 14);
        c.addView(Ui.text(this, label, 12, Ui.MUTED, Ui.REGULAR));
        TextView v = Ui.text(this, value, 18, Ui.TEXT, Ui.BOLD);
        c.addView(v);
        Ui.gapTop(v, this, 2);
        TextView sb = Ui.text(this, sub, 11, Ui.MUTED, Ui.REGULAR);
        c.addView(sb);
        Ui.gapTop(sb, this, 2);
        return c;
    }

    /** D1 "데모 결제로 체험하기" 카드. 처음엔 3개, 더 보기로 7개. */
    private View demoCard() {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.caption(this, "데모 결제로 체험하기"));
        int count = showAllDemos ? DemoCheckoutActivity.PRESETS.length : 3;
        for (int i = 0; i < count; i++) {
            final int idx = i;
            DemoCheckoutActivity.Preset p = DemoCheckoutActivity.PRESETS[i];
            LinearLayout r = Ui.h(this);
            Ui.pad(r, this, 0, 10, 0, 10);
            r.addView(Ui.text(this, p.label, 15, Ui.TEXT, Ui.MEDIUM), Ui.weight1());
            r.addView(Ui.text(this, Money.won(p.amount) + " › " + p.expect, 12, Ui.BRAND, Ui.MEDIUM));
            r.setOnClickListener(v -> startActivity(new Intent(this, DemoCheckoutActivity.class).putExtra(DemoCheckoutActivity.EXTRA_PRESET, idx)));
            c.addView(r, Ui.fillW());
            if (i == 0) Ui.gapTop(r, this, 2);
        }
        TextView more = Ui.linkButton(this, showAllDemos ? "접기" : "다른 시나리오 " + (DemoCheckoutActivity.PRESETS.length - 3) + "개 더 보기", v -> {
            showAllDemos = !showAllDemos;
            refresh();
        });
        more.setGravity(Gravity.START);
        Ui.pad(more, this, 0, 6, 0, 0);
        c.addView(more, Ui.fillW());
        return c;
    }

    /** S09: 이전 날 이유 선택 결제·숙고 후 결제 중 아직 답하지 않은 건을 홈 위 시트로 묻는다. 숙고 후 결제를 먼저. */
    private void maybeShowReview(List<Record> recs, long now) {
        if (reviewDismissed) return;
        long startToday = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli();
        List<Record> due = new ArrayList<>();
        for (Record r : recs) {
            if (r.review != null || r.ts >= startToday || now - r.ts > 7 * 86_400_000L) continue;
            if (r.outcome == Outcome.REASON_PAY || r.outcome == Outcome.DELIBERATE_PAY) due.add(r);
        }
        if (due.isEmpty()) return;
        Collections.sort(due, (x, y) -> {
            int px = x.outcome == Outcome.DELIBERATE_PAY || "그냥 사고 싶어요".equals(x.chip) ? 0 : 1;
            int py = y.outcome == Outcome.DELIBERATE_PAY || "그냥 사고 싶어요".equals(y.chip) ? 0 : 1;
            return px != py ? Integer.compare(px, py) : Long.compare(y.ts, x.ts);
        });
        showReview(due.get(0), due.size());
    }

    private void showReview(Record r, int total) {
        FrameLayout box = new FrameLayout(this);
        View dim = new View(this);
        dim.setBackgroundColor(Ui.DIM);
        dim.setOnClickListener(v -> {
            reviewDismissed = true;
            hideReview();
        });
        box.addView(dim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout sheet = Ui.v(this);
        sheet.setClickable(true);
        sheet.setBackground(Ui.corners(this, Ui.WHITE, 24, 24, 0, 0));
        Ui.pad(sheet, this, 20, 22, 20, 24);
        LinearLayout tags = Ui.h(this);
        tags.addView(Ui.tag(this, "어제 결제 돌아보기", Ui.BRAND_LIGHT, Ui.BRAND), Ui.wrap());
        if (total > 1) {
            TextView cnt = Ui.text(this, total + "건 중 1번째", 12, Ui.SUBTLE, Ui.MEDIUM);
            LinearLayout.LayoutParams cp = Ui.wrap();
            cp.leftMargin = Ui.dp(this, 8);
            tags.addView(cnt, cp);
        }
        sheet.addView(tags, Ui.fillW());
        LocalDate d = Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate();
        String when = d.equals(LocalDate.now(zone).minusDays(1)) ? "어제" : d.getMonthValue() + "/" + d.getDayOfMonth();
        TextView title = Ui.text(this, when + " " + appShort(r.app) + " " + r.title + " " + Money.won(r.amount) + ",\n만족하시나요?", 17, Ui.TEXT, Ui.BOLD);
        sheet.addView(title, Ui.fillW());
        Ui.gapTop(title, this, 12);
        String why = r.outcome == Outcome.DELIBERATE_PAY
                ? "[" + (r.app == TargetApp.BAEMIN ? "그래도 시킬게요" : "그래도 살게요") + "]로 결제한 건이에요. 답은 다음 대화의 질문 재료가 돼요."
                : "'" + r.chip + "' 이유를 고르고 결제한 건이에요. 답은 다음 대화의 질문 재료가 돼요.";
        TextView desc = Ui.text(this, why, 12, Ui.MUTED, Ui.REGULAR);
        sheet.addView(desc, Ui.fillW());
        Ui.gapTop(desc, this, 12);
        LinearLayout btns = Ui.h(this);
        btns.addView(Ui.button(this, "만족해요", false, v -> answerReview(r, "만족해요")), Ui.weight1());
        LinearLayout.LayoutParams bp = Ui.weight1();
        bp.leftMargin = Ui.dp(this, 8);
        btns.addView(Ui.button(this, "후회돼요", false, v -> answerReview(r, "후회돼요")), bp);
        sheet.addView(btns, Ui.fillW());
        Ui.gapTop(btns, this, 12);

        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        box.addView(sheet, sp);
        layer.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        reviewSheet = box;
    }

    private void answerReview(Record r, String a) {
        r.review = a;
        store.updateRecord(r);
        toast(a.equals("후회돼요") ? "다음 대화의 '과거 연결' 질문에 쓸게요" : "기록했어요");
        refresh();
    }

    private void hideReview() {
        if (reviewSheet != null) {
            layer.removeView(reviewSheet);
            reviewSheet = null;
        }
    }

    // ------------------------------------------------------------------ 기록 탭

    private void records() {
        add(h1("기록"), 0);
        add(Ui.text(this, "길게 누르면 지울 수 있어요 (결제하지 않았는데 기록된 건 등).", 13, Ui.MUTED, Ui.REGULAR), 6);
        List<Record> sorted = store.records();
        Collections.sort(sorted, (x, y) -> Long.compare(y.ts, x.ts));
        LinearLayout list = Ui.outlineCard(this, 16, 16);
        boolean first = true;
        for (Record r : sorted) {
            if (!first) {
                View d = Ui.divider(this);
                list.addView(d);
                Ui.gapTop(d, this, 12);
            }
            View row = recordRow(r, true);
            list.addView(row, Ui.fillW());
            if (!first) Ui.gapTop(row, this, 12);
            for (String line : r.qa) {
                TextView q = Ui.text(this, line, 12, Ui.MUTED, Ui.REGULAR);
                Ui.pad(q, this, 10, 0, 0, 0);
                list.addView(q, Ui.fillW());
                Ui.gapTop(q, this, 4);
            }
            first = false;
        }
        if (sorted.isEmpty()) list.addView(Ui.text(this, "아직 기록이 없어요.", 13, Ui.MUTED, Ui.REGULAR));
        add(list, 12);
    }

    // ------------------------------------------------------------------ S10 리포트

    private static int pct(int a, int b) {
        return b == 0 ? 0 : Math.round(a * 100f / b);
    }

    private void report() {
        Settings s = store.settings();
        long now = System.currentTimeMillis();
        List<Record> recs = store.records();
        Stats st = Stats.of(s, recs, now, zone);
        BudgetEngine.State b = BudgetEngine.compute(s, recs, now, zone);
        add(h1(LocalDate.now(zone).getMonthValue() + "월 리포트"), 0);
        add(Ui.text(this, b.cycleStart + " ~ " + b.nextPayday.minusDays(1) + " (급여일 기준)", 12, Ui.SUBTLE, Ui.REGULAR), 4);

        LinearLayout hero = Ui.v(this);
        hero.setBackground(Ui.round(this, Ui.BRAND, 16));
        Ui.pad(hero, this, 20, 20, 20, 20);
        hero.addView(Ui.text(this, "마음 돌림 비율", 12, Ui.LINE, Ui.MEDIUM));
        int rate = st.mindChangeRate();
        TextView rv = Ui.text(this, rate < 0 ? "–" : rate + "%", 30, Ui.WHITE, Ui.BOLD);
        hero.addView(rv);
        Ui.gapTop(rv, this, 4);
        TextView rd = Ui.text(this, st.interventions == 0 ? "아직 쉼표가 말을 건 결제가 없어요"
                : "쉼표가 말을 건 " + st.interventions + "번 중 " + st.prevented + "번 그만뒀어요 · " + Money.won(st.preventedSum) + " 아낌", 12, Ui.LINE, Ui.REGULAR);
        hero.addView(rd);
        Ui.gapTop(rd, this, 4);
        add(hero, 12);

        LinearLayout ic = Ui.card(this);
        ic.addView(Ui.caption(this, "강도별 멈춘 비율"));
        for (Intensity i : Intensity.values()) {
            int[] v = st.byIntensity.get(i);
            LinearLayout r = Ui.h(this);
            r.addView(Ui.text(this, i.label, 15, Ui.TEXT, Ui.MEDIUM), new LinearLayout.LayoutParams(Ui.dp(this, 48), ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams bp = Ui.weight1();
            bp.leftMargin = Ui.dp(this, 10);
            r.addView(Ui.bar(this, v[0] == 0 ? 0f : (float) v[1] / v[0], Ui.SUBTLE, Ui.LINE), bp);
            TextView t = Ui.text(this, v[1] + "/" + v[0] + " · " + pct(v[1], v[0]) + "%", 12, Ui.MUTED, Ui.MEDIUM);
            LinearLayout.LayoutParams tp = Ui.wrap();
            tp.leftMargin = Ui.dp(this, 10);
            r.addView(t, tp);
            ic.addView(r, Ui.fillW());
            Ui.gapTop(r, this, 10);
        }
        add(ic, 12);

        LinearLayout minis = Ui.h(this);
        minis.setGravity(Gravity.TOP);
        int diff = st.lateCount - st.prevLateCount;
        minis.addView(reportMini("야식", st.lateCount + "번", Money.won(st.lateSum) + " · 지난달 " + (diff > 0 ? "+" : diff < 0 ? "−" : "±") + Math.abs(diff) + "번"), Ui.weight1());
        LinearLayout.LayoutParams mp = Ui.weight1();
        mp.leftMargin = Ui.dp(this, 10);
        minis.addView(reportMini("회고 후회", st.regret + "/" + st.reviewed, "후회 응답 " + pct(st.regret, st.reviewed) + "%"), mp);
        add(minis, 12);

        int total = 0;
        Outcome[] kinds = {Outcome.PREVENTED, Outcome.REASON_PAY, Outcome.DELIBERATE_PAY, Outcome.EXIT};
        for (Outcome o : kinds) total += st.byOutcome.get(o);
        LinearLayout rc = Ui.card(this);
        rc.addView(Ui.caption(this, "결과 분류 (" + total + "건)"));
        for (Outcome o : kinds) {
            int c = st.byOutcome.get(o);
            long sum = st.byOutcomeSum.get(o);
            View kv = Ui.kv(this, o.label, c + "건" + (o == Outcome.EXIT || c == 0 ? "" : " · " + Money.won(sum)), false);
            rc.addView(kv, Ui.fillW());
            Ui.gapTop(kv, this, 8);
        }
        add(rc, 12);

        TextView food = Ui.text(this, "식비 예산 " + Money.won(b.foodSpent) + " / " + Money.won(b.foodBudget) + " 사용 · 다음 달부터 쿠팡 식료품 실측값으로 보정해요", 12, Ui.BRAND, Ui.MEDIUM);
        food.setBackground(Ui.round(this, Ui.BRAND_LIGHT, 12));
        Ui.pad(food, this, 14, 10, 14, 10);
        add(food, 12);
    }

    private View reportMini(String label, String value, String sub) {
        LinearLayout c = Ui.v(this);
        c.setBackground(Ui.round(this, Ui.WHITE, 16));
        Ui.pad(c, this, 14, 14, 14, 14);
        c.addView(Ui.text(this, label, 12, Ui.MUTED, Ui.REGULAR));
        TextView v = Ui.text(this, value, 17, Ui.TEXT, Ui.BOLD);
        c.addView(v);
        Ui.gapTop(v, this, 2);
        TextView sb = Ui.text(this, sub, 12, Ui.SUBTLE, Ui.REGULAR);
        c.addView(sb);
        Ui.gapTop(sb, this, 2);
        return c;
    }

    // ------------------------------------------------------------------ 설정 · 테스트 도구

    private View labeled(String label, EditText in) {
        LinearLayout r = Ui.h(this);
        r.addView(Ui.text(this, label, 15, Ui.MUTED, Ui.REGULAR), Ui.weight1());
        r.addView(in, new LinearLayout.LayoutParams(Ui.dp(this, 130), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams p = Ui.fillW();
        p.topMargin = Ui.dp(this, 8);
        r.setLayoutParams(p);
        return r;
    }

    private void settingsTab() {
        Settings s = store.settings();
        add(h1("설정"), 0);

        // 상태
        LinearLayout sc = Ui.card(this);
        boolean on = serviceEnabled();
        sc.addView(Ui.kv(this, "결제 감지(접근성 서비스)", on ? "켜짐" : "꺼짐", true));
        View kv2 = Ui.kv(this, "현재 세션", GuardService.instance == null ? "서비스 미연결" : GuardService.instance.sessionState(), false);
        sc.addView(kv2, Ui.fillW());
        Ui.gapTop(kv2, this, 8);
        View kv3 = Ui.kv(this, "앱 버전", "0.5 · Figma Hi-Fi 디자인", false);
        sc.addView(kv3, Ui.fillW());
        Ui.gapTop(kv3, this, 8);
        TextView open = Ui.text(this, "접근성 설정 열기 ›", 13, Ui.BLUE, true);
        Ui.pad(open, this, 0, 8, 0, 0);
        open.setOnClickListener(v -> openA11ySettings());
        sc.addView(open);
        add(sc, 12);

        // AI
        add(section("AI (Claude API)"), 18);
        LinearLayout ac = Ui.card(this);
        EditText key = Ui.input(this, "sk-ant-... (비우면 기본 질문·키워드 판정)", s.apiKey, false);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText model = Ui.input(this, "모델", s.model, false);
        EditText timeout = Ui.input(this, "대기 시간(ms)", String.valueOf(s.aiTimeoutMs), true);
        ac.addView(Ui.text(this, "API 키", 12, Ui.MUTED, false));
        ac.addView(key);
        ac.addView(labeled("모델", model));
        ac.addView(labeled("응답 대기 시간(ms)", timeout));
        ac.addView(Ui.text(this, "키가 있으면 쿠팡·무신사 품목 판정과 질문을 AI가 만들어요. 대기 시간 안에 응답이 없으면 키워드 판정·기본 질문으로 바로 대체해요 (PRD 기본 2,000ms).", 11, Ui.MUTED, false));
        if (!ClaudeClient.lastError.isEmpty()) ac.addView(Ui.text(this, "마지막 AI 오류: " + ClaudeClient.lastError, 11, Ui.RED, false));
        add(ac, 8);

        // 예산
        add(section("예산"), 18);
        LinearLayout bc = Ui.card(this);
        EditText income = Ui.input(this, "", String.valueOf(s.income), true);
        EditText payday = Ui.input(this, "", String.valueOf(s.payday), true);
        EditText rent = Ui.input(this, "", String.valueOf(s.rent), true);
        EditText util = Ui.input(this, "", String.valueOf(s.utilities), true);
        EditText phone = Ui.input(this, "", String.valueOf(s.phone), true);
        EditText trans = Ui.input(this, "", String.valueOf(s.transport), true);
        EditText subs = Ui.input(this, "", String.valueOf(s.subscriptions), true);
        EditText mart = Ui.input(this, "", String.valueOf(s.martBudget), true);
        EditText cpg = Ui.input(this, "", String.valueOf(s.coupangFoodBudget), true);
        bc.addView(labeled("월 수입", income));
        bc.addView(labeled("급여일 (1~28)", payday));
        bc.addView(labeled("월세·관리비", rent));
        bc.addView(labeled("공과금", util));
        bc.addView(labeled("통신비", phone));
        bc.addView(labeled("교통비", trans));
        bc.addView(labeled("구독료", subs));
        bc.addView(labeled("식비: 마트", mart));
        bc.addView(labeled("식비: 쿠팡 식료품", cpg));
        add(bc, 8);

        // 야식
        add(section("야식 반복 트리거"), 18);
        LinearLayout lc = Ui.card(this);
        EditText ls = Ui.input(this, "", String.valueOf(s.lateStartHour), true);
        EditText le = Ui.input(this, "", String.valueOf(s.lateEndHour), true);
        EditText lw = Ui.input(this, "", String.valueOf(s.lateWindowDays), true);
        EditText ln = Ui.input(this, "", String.valueOf(s.lateCount), true);
        final boolean[] lateOn = {s.lateEnabled};
        TextView toggle = Ui.chip(this, lateOn[0] ? "켜짐" : "꺼짐", lateOn[0], null);
        toggle.setOnClickListener(v -> {
            lateOn[0] = !lateOn[0];
            toggle.setText(lateOn[0] ? "켜짐" : "꺼짐");
        });
        LinearLayout tr = Ui.h(this);
        tr.addView(Ui.text(this, "야식 반복 개입", 13, Ui.MUTED, false), Ui.weight1());
        tr.addView(toggle);
        lc.addView(tr);
        lc.addView(labeled("시작 시각 (시)", ls));
        lc.addView(labeled("끝 시각 (시)", le));
        lc.addView(labeled("최근 며칠", lw));
        lc.addView(labeled("몇 번째부터 개입", ln));
        add(lc, 8);

        add(Ui.button(this, "설정 저장", true, v -> {
            Settings st = store.settings();
            st.apiKey = key.getText().toString().trim();
            st.model = model.getText().toString().trim().isEmpty() ? new Settings().model : model.getText().toString().trim();
            st.aiTimeoutMs = (int) Ui.num(timeout, st.aiTimeoutMs);
            st.income = Ui.num(income, st.income);
            st.payday = (int) Math.max(1, Math.min(28, Ui.num(payday, st.payday)));
            st.rent = Ui.num(rent, st.rent);
            st.utilities = Ui.num(util, st.utilities);
            st.phone = Ui.num(phone, st.phone);
            st.transport = Ui.num(trans, st.transport);
            st.subscriptions = Ui.num(subs, st.subscriptions);
            st.martBudget = Ui.num(mart, st.martBudget);
            st.coupangFoodBudget = Ui.num(cpg, st.coupangFoodBudget);
            st.lateEnabled = lateOn[0];
            st.lateStartHour = (int) Ui.num(ls, st.lateStartHour) % 24;
            st.lateEndHour = (int) Ui.num(le, st.lateEndHour) % 24;
            st.lateWindowDays = (int) Math.max(1, Ui.num(lw, st.lateWindowDays));
            st.lateCount = (int) Math.max(1, Ui.num(ln, st.lateCount));
            store.saveSettings(st);
            toast("저장했어요");
            refresh();
        }), 14);

        testTools(s);
    }

    private TextView section(String s) {
        return Ui.text(this, s, 15, Ui.TEXT, Ui.BOLD);
    }

    private void testTools(Settings s) {
        add(section("테스트 도구 (시연용)"), 24);
        LinearLayout tc = Ui.card(this);
        tc.addView(body("시나리오 기준: 수입 100만 · 고정 50만 · 식비 10만 → 여윳돈 40만, 10일째 적용 한도 14,000원."));
        tc.addView(testButton("① 시나리오 10일째로 맞추기 (이전 지출 120,000원)", () -> {
            Settings st = store.settings();
            st.cycleStartOverride = LocalDate.now(zone).minusDays(10).toEpochDay();
            store.saveSettings(st);
            store.deleteWhere(Outcome.SYNC);
            addSync("이번 주기 이전 지출(Mock)", 120_000, LocalDate.now(zone).minusDays(1), 12, 0, false);
            toast("10일째 · 남은 여윳돈 280,000원으로 맞췄어요");
        }));
        tc.addView(testButton("② 점심 외식 8,000원 추가 (계좌 동기화 Mock)", () -> {
            addSync("외식 · 학교 앞 식당", 8_000, LocalDate.now(zone), 12, 30, false);
            toast("오늘 누적에 8,000원 더했어요");
        }));
        tc.addView(testButton("③ 지난 며칠 야식 3건 추가 (다음 배민 야식이 4번째)", () -> {
            long now = System.currentTimeMillis();
            int[] days = {5, 3, 1};
            long[] amts = {13_000, 14_000, 12_000};
            for (int i = 0; i < 3; i++) {
                Record r = new Record();
                r.app = TargetApp.BAEMIN;
                r.ts = LocalDateTime.of(LocalDate.now(zone).minusDays(days[i]), LocalTime.of(23, 10)).atZone(zone).toInstant().toEpochMilli();
                if (r.ts > now) r.ts = now - 60_000;
                r.title = "야식 (Mock)";
                r.amount = r.discretionary = amts[i];
                r.outcome = Outcome.PASS;
                r.lateNight = true;
                store.addRecord(r);
            }
            Settings st = store.settings();
            st.lateResetTs = 0;
            store.saveSettings(st);
            toast("야식 3건을 넣었어요");
        }));
        String hourLabel = s.simulatedHour < 0 ? "실제 시각 사용 중" : "지금을 " + s.simulatedHour + "시로 간주 중";
        tc.addView(testButton("④ 시각 강제: " + hourLabel + " (눌러서 23시 ↔ 실제)", () -> {
            Settings st = store.settings();
            st.simulatedHour = st.simulatedHour < 0 ? 23 : -1;
            store.saveSettings(st);
        }));
        tc.addView(testButton("결제 버튼 가드 표시: " + (s.showGuard ? "켜짐 (파란 테두리)" : "꺼짐") + " — 눌러서 전환", () -> {
            Settings st = store.settings();
            st.showGuard = !st.showGuard;
            store.saveSettings(st);
        }));
        tc.addView(testButton("야식 카운트 초기화", () -> {
            Settings st = store.settings();
            st.lateResetTs = System.currentTimeMillis();
            store.saveSettings(st);
        }));
        tc.addView(testButton("데모 결제 화면 열기", () -> startActivity(new Intent(this, DemoCheckoutActivity.class))));
        tc.addView(testButton("기록 전체 삭제", () -> {
            store.clearRecords();
            toast("기록을 모두 지웠어요");
        }));
        tc.addView(testButton("온보딩 다시 보기", () -> onboardStep = 1));
        add(tc, 8);

        add(section("감지 로그"), 18);
        LinearLayout lg = Ui.card(this);
        List<String> logs = store.logs();
        Collections.reverse(logs);
        int n = 0;
        for (String l : logs) {
            if (n++ >= 40) break;
            TextView t = Ui.text(this, l, 11, Ui.DARK, false);
            t.setTextIsSelectable(true);
            lg.addView(t);
        }
        if (logs.isEmpty()) lg.addView(body("아직 로그가 없어요."));
        add(lg, 8);

        add(section("마지막으로 읽은 화면 텍스트"), 18);
        add(body("대상 앱 결제 화면이 감지되지 않으면 여기 텍스트를 확인해 주세요. 결제 버튼·결제 섹션 라벨이 읽히는지, 금액이 맞는지 볼 수 있어요."), 4);
        for (TargetApp app : TargetApp.values()) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, app.label + " (" + app.pkg + ")", 13, Ui.DARK, true));
            String t = store.lastScreen(app);
            TextView tv = Ui.text(this, t.isEmpty() ? "아직 읽은 화면이 없어요." : (t.length() > 2500 ? t.substring(0, 2500) + "\n…" : t), 11, Ui.MUTED, false);
            tv.setTextIsSelectable(true);
            c.addView(tv);
            add(c, 8);
        }
    }

    private View testButton(String label, Runnable r) {
        TextView t = Ui.text(this, label, 13, Ui.BLUE, true);
        Ui.pad(t, this, 0, 10, 0, 10);
        t.setOnClickListener(v -> {
            r.run();
            refresh();
        });
        return t;
    }

    private void addSync(String title, long amount, LocalDate day, int hour, int min, boolean food) {
        Record r = new Record();
        r.ts = LocalDateTime.of(day, LocalTime.of(hour, min)).atZone(zone).toInstant().toEpochMilli();
        long now = System.currentTimeMillis();
        if (r.ts > now) r.ts = now - 60_000;
        r.title = title;
        r.amount = amount;
        if (food) r.food = amount;
        else r.discretionary = amount;
        r.outcome = Outcome.SYNC;
        store.addRecord(r);
    }
}
