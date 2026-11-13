package com.shimpyo.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 온보딩(S01~S03) · 홈(S04) · 회고(S09) · 리포트(S10) · 설정/테스트 도구. 화면은 코드로 그린다. */
public class MainActivity extends Activity {

    private Store store;
    private final ZoneId zone = ZoneId.systemDefault();
    private LinearLayout content;
    private LinearLayout nav;
    private int tab = 0;
    private int onboardStep = 0;
    private boolean bankConsent = true;
    private int bankSelected = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = Store.get(this);
        LinearLayout root = Ui.v(this);
        root.setBackgroundColor(Ui.BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = Ui.v(this);
        Ui.pad(content, this, 20, 20, 20, 28);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        nav = Ui.h(this);
        nav.setBackgroundColor(Ui.WHITE);
        root.addView(nav, Ui.fillW());
        setContentView(root);
        if (!store.settings().onboarded) onboardStep = 1;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    void refresh() {
        content.removeAllViews();
        if (onboardStep > 0) {
            nav.setVisibility(View.GONE);
            if (onboardStep == 1) s01();
            else if (onboardStep == 2) s02();
            else s03();
            return;
        }
        nav.setVisibility(View.VISIBLE);
        buildNav();
        if (tab == 0) home();
        else if (tab == 1) records();
        else if (tab == 2) report();
        else settingsTab();
    }

    private void buildNav() {
        nav.removeAllViews();
        String[] labels = {"홈", "기록", "리포트", "설정"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView t = Ui.text(this, labels[i], 14, i == tab ? Ui.BLUE : Ui.MUTED, i == tab);
            t.setGravity(Gravity.CENTER);
            Ui.pad(t, this, 0, 14, 0, 14);
            t.setOnClickListener(v -> {
                tab = idx;
                refresh();
            });
            nav.addView(t, Ui.weight1());
        }
    }

    // ------------------------------------------------------------------ 공통

    private void add(View v, int gapDp) {
        content.addView(v, Ui.fillW());
        if (gapDp > 0) Ui.gapTop(v, this, gapDp);
    }

    private TextView h1(String s) {
        return Ui.text(this, s, 22, Ui.DARK, true);
    }

    private TextView body(String s) {
        return Ui.text(this, s, 14, Ui.MUTED, false);
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

    private String time(long ts) {
        return new SimpleDateFormat("M/d HH:mm", Locale.KOREA).format(new Date(ts));
    }

    private TextView outcomeTag(Record r) {
        switch (r.outcome) {
            case PREVENTED:
                return Ui.tag(this, r.outcome.label, Ui.GREEN_BG, Ui.GREEN);
            case REASON_PAY:
                return Ui.tag(this, r.outcome.label, Ui.BLUE_LIGHT, Ui.BLUE);
            case DELIBERATE_PAY:
                return Ui.tag(this, r.outcome.label, Ui.AMBER_BG, Ui.AMBER);
            case SYNC:
                return Ui.tag(this, r.food > 0 ? "식비 예산" : "여윳돈", Ui.GRAY_BG, Ui.MUTED);
            default:
                return Ui.tag(this, r.outcome.label, Ui.GRAY_BG, Ui.MUTED);
        }
    }

    private View recordRow(Record r, boolean deletable) {
        LinearLayout row = Ui.h(this);
        LinearLayout left = Ui.v(this);
        left.addView(Ui.text(this, r.appLabel() + " · " + r.title, 14, Ui.DARK, false));
        String sub = time(r.ts);
        if (r.intensity != null) sub += " · " + r.intensity.label;
        if (r.lateNight) sub += " · 야식";
        if (r.chip != null) sub += " · \"" + r.chip + "\"";
        left.addView(Ui.text(this, sub, 12, Ui.MUTED, false));
        row.addView(left, Ui.weight1());
        LinearLayout right = Ui.v(this);
        right.setGravity(Gravity.END);
        right.addView(Ui.text(this, Money.won(r.amount), 14, Ui.DARK, true));
        right.addView(outcomeTag(r));
        row.addView(right);
        if (deletable) {
            row.setOnLongClickListener(v -> {
                store.deleteRecord(r.id);
                toast("기록을 지웠어요");
                refresh();
                return true;
            });
        }
        Ui.pad(row, this, 0, 8, 0, 8);
        return row;
    }

    // ------------------------------------------------------------------ S01~S03 온보딩

    private View progress(int step) {
        LinearLayout r = Ui.h(this);
        for (int i = 1; i <= 3; i++) {
            View bar = new View(this);
            bar.setBackground(Ui.round(this, i <= step ? Ui.BLUE : Ui.LINE, 2));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(Ui.dp(this, 28), Ui.dp(this, 4));
            p.rightMargin = Ui.dp(this, 6);
            r.addView(bar, p);
        }
        r.addView(Ui.text(this, step + "/3", 12, Ui.MUTED, false));
        return r;
    }

    private void s01() {
        add(progress(1), 0);
        add(Ui.text(this, "쉼표", 26, Ui.BLUE, true), 18);
        add(h1("결제 직전에\n쉼표 하나 찍어드릴게요"), 8);
        add(body("이번 달 돈 흐름에 무리가 될 때만 결제 화면 위에서 질문을 건네요. 결정은 언제나 내가 해요.\n대상 앱: 배달의민족 · 쿠팡 · 무신사"), 10);
        LinearLayout c = Ui.card(this);
        boolean on = serviceEnabled();
        c.addView(permRow("필수", "접근성 서비스", "결제 화면의 상품명과 금액을 읽고, 결제 화면 위에 질문을 띄워요", on));
        View r2 = permRow("선택", "알림", "다음 날 어제 결제를 돌아보자고 물어봐요 (앱 홈에서도 볼 수 있어요)", true);
        c.addView(r2);
        Ui.gapTop(r2, this, 12);
        add(c, 16);
        TextView note = Ui.text(this, "비밀번호·카드번호·메시지는 판단에 쓰지 않아요. 상품명과 금액만 써요.", 12, Ui.BLUE, true);
        note.setBackground(Ui.round(this, Ui.BLUE_LIGHT, 10));
        Ui.pad(note, this, 12, 12, 12, 12);
        add(note, 12);
        add(Ui.button(this, on ? "접근성 서비스 켜짐 ✓" : "접근성 설정 열기", !on, v -> openA11ySettings()), 20);
        add(Ui.button(this, "다음", on, v -> {
            onboardStep = 2;
            refresh();
        }), 10);
        if (!on) add(body("에뮬레이터: 설정 → 접근성 → 설치된 앱(또는 다운로드한 앱) → 쉼표 → 사용. 켜지 않고 넘어가도 나중에 설정 탭에서 켤 수 있어요."), 10);
    }

    private View permRow(String level, String title, String desc, boolean ok) {
        LinearLayout r = Ui.h(this);
        r.addView(level.equals("필수") ? Ui.tag(this, level, Ui.BLUE_LIGHT, Ui.BLUE) : Ui.tag(this, level, Ui.GRAY_BG, Ui.MUTED));
        LinearLayout t = Ui.v(this);
        Ui.pad(t, this, 10, 0, 0, 0);
        t.addView(Ui.text(this, title + (ok ? "  ✓" : ""), 14, Ui.DARK, true));
        t.addView(Ui.text(this, desc, 12, Ui.MUTED, false));
        r.addView(t, Ui.weight1());
        return r;
    }

    private EditText incomeIn, paydayIn;

    private void s02() {
        Settings s = store.settings();
        add(progress(2), 0);
        add(h1("입출금 계좌를\n연결해 주세요"), 18);
        add(body("수입과 고정지출을 찾아 한 달 여윳돈을 계산해요. (프로토타입: Mock 데이터)"), 8);
        List<TextView> banks = new ArrayList<>();
        String[] names = {"은행 A", "은행 B", "은행 C", "은행 D"};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            banks.add(Ui.chip(this, names[i], i == bankSelected, v -> {
                bankSelected = idx;
                refresh();
            }));
        }
        add(Ui.chipGrid(this, banks, 2), 14);
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, "정기 수입을 찾았어요", 12, Ui.MUTED, false));
        c.addView(Ui.text(this, "매달 " + s.payday + "일 · 약 " + Money.won(s.income), 18, Ui.DARK, true));
        c.addView(Ui.text(this, "최근 3개월 같은 날짜에 비슷한 금액이 들어왔어요. 다르면 고쳐 주세요.", 12, Ui.MUTED, false));
        LinearLayout row = Ui.h(this);
        incomeIn = Ui.input(this, "월 수입", String.valueOf(s.income), true);
        paydayIn = Ui.input(this, "급여일", String.valueOf(s.payday), true);
        row.addView(incomeIn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        LinearLayout.LayoutParams pp = Ui.weight1();
        pp.leftMargin = Ui.dp(this, 8);
        row.addView(paydayIn, pp);
        c.addView(row);
        Ui.gapTop(row, this, 10);
        add(c, 14);
        add(Ui.chip(this, (bankConsent ? "✓ " : "") + "입출금 내역 조회에 동의해요 (필수)", bankConsent, v -> {
            bankConsent = !bankConsent;
            refresh();
        }), 14);
        add(Ui.text(this, "정기 수입이 없으면 하루 한도를 계산할 수 없어 지금은 서비스 대상이 아니에요.", 11, Ui.MUTED, false), 8);
        add(Ui.button(this, "다음", bankConsent, v -> {
            if (!bankConsent) return;
            Settings st = store.settings();
            st.income = Ui.num(incomeIn, st.income);
            st.payday = (int) Math.max(1, Math.min(28, Ui.num(paydayIn, st.payday)));
            store.saveSettings(st);
            onboardStep = 3;
            refresh();
        }), 20);
    }

    private void s03() {
        Settings s = store.settings();
        add(progress(3), 0);
        add(h1("한 달 여윳돈을 확인해 주세요"), 18);
        LinearLayout fc = Ui.card(this);
        fc.addView(Ui.text(this, "고정지출 (계좌 내역에서 자동으로 찾았어요)", 13, Ui.DARK, true));
        String[] labels = {"월세·관리비", "공과금", "통신비", "교통비", "구독료"};
        long[] vals = {s.rent, s.utilities, s.phone, s.transport, s.subscriptions};
        EditText[] fixed = new EditText[5];
        for (int i = 0; i < 5; i++) {
            fixed[i] = Ui.input(this, labels[i], String.valueOf(vals[i]), true);
            fc.addView(labeled(labels[i], fixed[i]));
        }
        add(fc, 14);
        LinearLayout food = Ui.card(this);
        food.addView(Ui.text(this, "식비 예산 (해 먹는 재료)", 13, Ui.DARK, true));
        EditText mart = Ui.input(this, "마트", String.valueOf(s.martBudget), true);
        EditText cpg = Ui.input(this, "쿠팡 식료품", String.valueOf(s.coupangFoodBudget), true);
        food.addView(labeled("마트 장보기 · 최근 3개월 평균", mart));
        food.addView(labeled("쿠팡 식료품 · 첫 달 임시값", cpg));
        food.addView(Ui.text(this, "쿠팡 결제의 50%로 잡고, 다음 달부터 실제 식료품 판정 기록으로 보정해요. 외식·배달은 여윳돈에서 나가요.", 11, Ui.MUTED, false));
        add(food, 10);
        TextView sum = Ui.text(this, summary(s), 14, Ui.WHITE, true);
        sum.setBackground(Ui.round(this, Ui.DARK, 14));
        Ui.pad(sum, this, 14, 14, 14, 14);
        add(sum, 10);
        add(Ui.button(this, "다시 계산", false, v -> {
            Settings st = read(fixed, mart, cpg);
            store.saveSettings(st);
            refresh();
        }), 12);
        add(Ui.button(this, "쉼표 시작하기", true, v -> {
            Settings st = read(fixed, mart, cpg);
            st.onboarded = true;
            store.saveSettings(st);
            onboardStep = 0;
            tab = 0;
            refresh();
        }), 10);
    }

    private Settings read(EditText[] fixed, EditText mart, EditText cpg) {
        Settings st = store.settings();
        st.rent = Ui.num(fixed[0], st.rent);
        st.utilities = Ui.num(fixed[1], st.utilities);
        st.phone = Ui.num(fixed[2], st.phone);
        st.transport = Ui.num(fixed[3], st.transport);
        st.subscriptions = Ui.num(fixed[4], st.subscriptions);
        st.martBudget = Ui.num(mart, st.martBudget);
        st.coupangFoodBudget = Ui.num(cpg, st.coupangFoodBudget);
        return st;
    }

    private String summary(Settings s) {
        long b = Math.round(s.monthlyDiscretionary() / 30.0);
        return "수입 " + Money.num(s.income) + " − 고정 " + Money.num(s.fixedTotal()) + " − 식비 " + Money.num(s.foodBudget())
                + "\n월 여윳돈  " + Money.won(s.monthlyDiscretionary())
                + "\n하루 기준액 약 " + Money.won(b) + " · 남은 돈과 날짜에 따라 매일 다시 계산해요";
    }

    private View labeled(String label, EditText in) {
        LinearLayout r = Ui.h(this);
        r.addView(Ui.text(this, label, 13, Ui.MUTED, false), Ui.weight1());
        r.addView(in, new LinearLayout.LayoutParams(Ui.dp(this, 120), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams p = Ui.fillW();
        p.topMargin = Ui.dp(this, 6);
        r.setLayoutParams(p);
        return r;
    }

    // ------------------------------------------------------------------ S04 홈 (+ S09 회고)

    private void home() {
        Settings s = store.settings();
        long now = System.currentTimeMillis();
        List<Record> recs = store.records();
        BudgetEngine.State b = BudgetEngine.compute(s, recs, now, zone);
        Stats st = Stats.of(s, recs, now, zone);

        LinearLayout top = Ui.h(this);
        top.addView(Ui.text(this, "쉼표", 24, Ui.BLUE, true), Ui.weight1());
        long mins = s.lastSyncTs == 0 ? -1 : (now - s.lastSyncTs) / 60_000;
        top.addView(Ui.text(this, mins < 0 ? "동기화 전" : "마지막 동기화 " + (mins < 60 ? mins + "분 전" : (mins / 60) + "시간 전"), 12, Ui.MUTED, false));
        add(top, 0);

        if (!serviceEnabled()) {
            LinearLayout warn = Ui.card(this);
            warn.setBackground(Ui.round(this, Ui.RED_BG, 14));
            warn.addView(Ui.text(this, "결제 감지가 꺼져 있어요", 14, Ui.RED, true));
            warn.addView(Ui.text(this, "설정 → 접근성에서 '쉼표'를 켜야 배민·쿠팡·무신사 결제 화면에서 질문이 떠요.", 12, Ui.RED, false));
            TextView open = Ui.text(this, "접근성 설정 열기 ›", 13, Ui.RED, true);
            Ui.pad(open, this, 0, 8, 0, 0);
            open.setOnClickListener(v -> openA11ySettings());
            warn.addView(open);
            add(warn, 12);
        }

        reviewCards(recs, now);

        LinearLayout today = Ui.card(this);
        LinearLayout tr = Ui.h(this);
        tr.addView(Ui.text(this, "오늘 쓸 수 있는 돈", 13, Ui.MUTED, false), Ui.weight1());
        int sig = b.signal();
        tr.addView(sig == 0 ? Ui.tag(this, "여유", Ui.GREEN_BG, Ui.GREEN) : sig == 1 ? Ui.tag(this, "주의", Ui.AMBER_BG, Ui.AMBER) : Ui.tag(this, "초과", Ui.RED_BG, Ui.RED));
        today.addView(tr);
        TextView big = Ui.text(this, b.available >= 0 ? Money.won(b.available) + " 남음" : Money.won(-b.available) + " 넘었어요", 28, Ui.DARK, true);
        today.addView(big);
        Ui.gapTop(big, this, 6);
        today.addView(bar(b.limit == 0 ? 1f : Math.min(1f, (float) b.todaySpent / b.limit)));
        today.addView(Ui.text(this, "오늘 한도 " + Money.won(b.limit) + " 중 " + Money.won(b.todaySpent) + " 사용 · 주기 " + b.dayIndex + "일째", 12, Ui.MUTED, false));
        add(today, 14);

        LinearLayout minis = Ui.h(this);
        minis.addView(mini("남은 여윳돈", Money.won(b.remainingNow), "급여일까지 " + b.daysLeft + "일"), Ui.weight1());
        View m2 = mini("식비 예산", Money.won(b.foodRemaining), Money.num(b.foodBudget) + "원 중 남음");
        LinearLayout.LayoutParams mp = Ui.weight1();
        mp.leftMargin = Ui.dp(this, 10);
        minis.addView(m2, mp);
        add(minis, 10);

        LinearLayout saved = Ui.card(this);
        saved.setBackground(Ui.round(this, Ui.BLUE_LIGHT, 14));
        saved.addView(Ui.text(this, "이번 달 쉼표", 12, Ui.BLUE, false));
        saved.addView(Ui.text(this, st.prevented + "번 멈춰서 " + Money.won(st.preventedSum) + " 아꼈어요", 16, Ui.BLUE, true));
        add(saved, 10);

        if (s.lateEnabled) {
            int lateNow = TriggerEngine.recentLateNight(s, recs, now).size();
            LinearLayout late = Ui.card(this);
            late.addView(Ui.kv(this, "최근 " + s.lateWindowDays + "일 야식 (" + s.lateStartHour + "시~" + s.lateEndHour + "시 배민)",
                    lateNow + "번 / " + s.lateCount + "번째부터 질문", false));
            add(late, 10);
        }

        add(Ui.text(this, "최근 기록", 16, Ui.DARK, true), 18);
        LinearLayout list = Ui.card(this);
        List<Record> sorted = new ArrayList<>(recs);
        Collections.sort(sorted, (x, y) -> Long.compare(y.ts, x.ts));
        int n = 0;
        for (Record r : sorted) {
            if (n++ >= 6) break;
            list.addView(recordRow(r, false));
        }
        if (sorted.isEmpty()) list.addView(body("아직 기록이 없어요. 대상 앱에서 결제 화면에 들어가거나, 아래 데모 결제 화면으로 테스트해 보세요."));
        add(list, 8);

        add(Ui.button(this, "데모 결제 화면으로 테스트", false, v -> startActivity(new Intent(this, DemoCheckoutActivity.class))), 16);
    }

    private View bar(float ratio) {
        LinearLayout track = Ui.h(this);
        track.setBackground(Ui.round(this, Ui.LINE, 4));
        View fill = new View(this);
        fill.setBackground(Ui.round(this, ratio >= 1f ? Ui.RED : Ui.BLUE, 4));
        track.addView(fill, new LinearLayout.LayoutParams(0, Ui.dp(this, 8), Math.max(0.001f, ratio)));
        View rest = new View(this);
        track.addView(rest, new LinearLayout.LayoutParams(0, Ui.dp(this, 8), Math.max(0.001f, 1f - ratio)));
        LinearLayout.LayoutParams p = Ui.fillW();
        p.topMargin = Ui.dp(this, 10);
        p.bottomMargin = Ui.dp(this, 6);
        track.setLayoutParams(p);
        return track;
    }

    private View mini(String label, String value, String sub) {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, label, 12, Ui.MUTED, false));
        c.addView(Ui.text(this, value, 18, Ui.DARK, true));
        c.addView(Ui.text(this, sub, 11, Ui.MUTED, false));
        return c;
    }

    /** S09 사후 회고: 어제(이전) 이유 선택 결제·숙고 후 결제 중 응답 안 한 건. 숙고 후 결제 우선. */
    private void reviewCards(List<Record> recs, long now) {
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
        add(Ui.text(this, "어제 결제 돌아보기", 18, Ui.DARK, true), 14);
        int n = 0;
        for (Record r : due) {
            if (n++ >= 3) break;
            LinearLayout c = Ui.card(this);
            LinearLayout top = Ui.h(this);
            LinearLayout t = Ui.v(this);
            t.addView(Ui.text(this, r.appLabel(), 12, Ui.MUTED, false));
            t.addView(Ui.text(this, r.title + " · " + Money.won(r.amount), 15, Ui.DARK, true));
            top.addView(t, Ui.weight1());
            top.addView(outcomeTag(r));
            c.addView(top);
            String q = r.chip != null ? "\"" + r.chip + "\"를 골랐어요. 지금 보니 어때요?" : "고민 끝에 결제했어요. 지금도 잘 샀다고 느껴요?";
            TextView qt = Ui.text(this, q, 13, Ui.DARK, false);
            c.addView(qt);
            Ui.gapTop(qt, this, 6);
            List<TextView> chips = new ArrayList<>();
            for (String a : new String[]{"잘 샀어요", "그저 그래요", "후회돼요"}) {
                chips.add(Ui.chip(this, a, false, v -> {
                    r.review = a;
                    store.updateRecord(r);
                    toast(a.equals("후회돼요") ? "다음 대화의 '과거 연결' 질문에 쓸게요" : "기록했어요");
                    refresh();
                }));
            }
            View g = Ui.chipGrid(this, chips, 3);
            c.addView(g);
            Ui.gapTop(g, this, 8);
            add(c, 8);
        }
    }

    // ------------------------------------------------------------------ 기록 탭

    private void records() {
        add(h1("기록"), 0);
        add(body("길게 누르면 지울 수 있어요 (결제하지 않았는데 기록된 건 등)."), 6);
        List<Record> sorted = store.records();
        Collections.sort(sorted, (x, y) -> Long.compare(y.ts, x.ts));
        LinearLayout list = Ui.card(this);
        for (Record r : sorted) {
            list.addView(recordRow(r, true));
            if (!r.qa.isEmpty()) {
                for (String line : r.qa) {
                    TextView q = Ui.text(this, line, 11, Ui.MUTED, false);
                    Ui.pad(q, this, 8, 0, 0, 4);
                    list.addView(q);
                }
            }
            list.addView(Ui.divider(this));
        }
        if (sorted.isEmpty()) list.addView(body("아직 기록이 없어요."));
        add(list, 12);
    }

    // ------------------------------------------------------------------ S10 리포트

    private void report() {
        Settings s = store.settings();
        long now = System.currentTimeMillis();
        List<Record> recs = store.records();
        Stats st = Stats.of(s, recs, now, zone);
        BudgetEngine.State b = BudgetEngine.compute(s, recs, now, zone);
        add(h1("이번 달 리포트"), 0);
        add(body(b.cycleStart + " ~ " + b.nextPayday.minusDays(1) + " (급여일 기준)"), 4);

        LinearLayout hero = Ui.card(this);
        hero.setBackground(Ui.round(this, Ui.BLUE, 14));
        hero.addView(Ui.text(this, "마음 돌림 비율", 12, Ui.BLUE_LIGHT, false));
        int rate = st.mindChangeRate();
        hero.addView(Ui.text(this, rate < 0 ? "–" : rate + "%", 34, Ui.WHITE, true));
        hero.addView(Ui.text(this, "쉼표가 " + st.interventions + "번 말을 걸었고, 그중 " + st.prevented + "번은 스스로 멈췄어요.", 13, Ui.WHITE, false));
        add(hero, 14);

        LinearLayout minis = Ui.h(this);
        minis.addView(mini("아낀 돈", Money.won(st.preventedSum), "방지 실적 " + st.prevented + "건"), Ui.weight1());
        LinearLayout.LayoutParams mp = Ui.weight1();
        mp.leftMargin = Ui.dp(this, 10);
        minis.addView(mini("회고 후회", st.regret + " / " + st.reviewed + "건", "결제 후 돌아본 결과"), mp);
        add(minis, 10);

        LinearLayout ic = Ui.card(this);
        ic.addView(Ui.text(this, "강도별로 멈춘 비율", 14, Ui.DARK, true));
        for (Intensity i : Intensity.values()) {
            int[] v = st.byIntensity.get(i);
            View kv = Ui.kv(this, i.label, v[0] + "번 중 " + v[1] + "번", false);
            ic.addView(kv);
            Ui.gapTop(kv, this, 8);
            ic.addView(bar(v[0] == 0 ? 0f : (float) v[1] / v[0]));
        }
        add(ic, 10);

        LinearLayout rc = Ui.card(this);
        rc.addView(Ui.text(this, "개입 " + st.interventions + "번의 결과", 14, Ui.DARK, true));
        for (Outcome o : new Outcome[]{Outcome.PREVENTED, Outcome.REASON_PAY, Outcome.DELIBERATE_PAY, Outcome.EXIT, Outcome.PASS}) {
            View kv = Ui.kv(this, o.label, st.byOutcome.get(o) + "건", false);
            rc.addView(kv);
            Ui.gapTop(kv, this, 6);
        }
        add(rc, 10);

        LinearLayout lc = Ui.card(this);
        lc.addView(Ui.text(this, "야식", 14, Ui.DARK, true));
        View l1 = Ui.kv(this, "이번 달 야식 (배민 " + s.lateStartHour + "~" + s.lateEndHour + "시)", st.lateCount + "번 · " + Money.won(st.lateSum), false);
        lc.addView(l1);
        Ui.gapTop(l1, this, 6);
        lc.addView(Ui.kv(this, "지난달", st.prevLateCount + "번", false));
        add(lc, 10);

        LinearLayout fc = Ui.card(this);
        fc.addView(Ui.text(this, "식비 예산", 14, Ui.DARK, true));
        View f1 = Ui.kv(this, "사용", Money.won(b.foodSpent) + " / " + Money.won(b.foodBudget), false);
        fc.addView(f1);
        Ui.gapTop(f1, this, 6);
        fc.addView(Ui.text(this, "다음 달부터 쿠팡 식료품 실측값으로 식비 예산을 보정해요 (이번 프로토타입은 표시만).", 11, Ui.MUTED, false));
        add(fc, 10);
    }

    // ------------------------------------------------------------------ 설정 · 테스트 도구

    private void settingsTab() {
        Settings s = store.settings();
        add(h1("설정"), 0);

        // 상태
        LinearLayout sc = Ui.card(this);
        boolean on = serviceEnabled();
        sc.addView(Ui.kv(this, "결제 감지(접근성 서비스)", on ? "켜짐" : "꺼짐", true));
        sc.addView(Ui.kv(this, "현재 세션", GuardService.instance == null ? "서비스 미연결" : GuardService.instance.sessionState(), false));
        sc.addView(Ui.kv(this, "앱 버전", "0.3 · 멈춘 뒤 재시도 문구", false));
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
        return Ui.text(this, s, 16, Ui.DARK, true);
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
