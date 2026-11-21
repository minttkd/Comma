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
 * 온보딩(S01~S03) · 홈(S04) · 다음 날 회고(S09) · 기록 · 리포트(S10) · 설정/테스트 도구.
 * 화면은 코드로 그리고, 모양은 Figma "쉼표 Hi-Fi 프로토타입"을 따른다.
 *
 * 화면 하나는 xxxScreen() 메서드 하나이고, 화면 안의 카드는 각자 이름 붙은 메서드로 나눴다.
 * 데이터(Store · BudgetEngine · Stats)는 화면 메서드 맨 위에서 한 번만 읽고 카드 메서드에 값으로 넘긴다.
 */
public class MainActivity extends Activity {

    // 하단 탭 순서
    // 화면: 홈이 기본이고, 나머지는 홈에서 들어가는 하위 화면 (← 뒤로 · 기기 뒤로가기로 홈)
    private static final int PAGE_HOME = 0, PAGE_RECORDS = 1, PAGE_REPORT = 2, PAGE_SETTINGS = 3;
    private static final int REQ_GALLERY = 1; // UI 갤러리 [홈에서 보기] → 홈으로

    // 온보딩 단계 (0이면 온보딩이 끝난 상태)
    private static final int ONBOARD_DONE = 0, ONBOARD_PERMISSION = 1, ONBOARD_INCOME = 2, ONBOARD_BUDGET = 3;

    private static final int HOME_RECENT_COUNT = 5;   // 홈 최근 기록 개수
    private static final int DEMO_PREVIEW_COUNT = 3;  // 홈 데모 카드에 처음 보이는 개수
    private static final int LOG_COUNT = 40;          // 설정 감지 로그 개수
    private static final int SCREEN_TEXT_MAX = 2500;  // 설정 화면 텍스트 최대 글자

    private Store store;
    private final ZoneId zone = ZoneId.systemDefault();
    private LinearLayout content;
    private ScrollView scroll;
    private LinearLayout footer;
    private FrameLayout layer;
    private View reviewSheet;
    private int page = PAGE_HOME;
    private int onboardStep = ONBOARD_DONE;
    private boolean bankConsent = true;
    private boolean cycleConsent = true;
    private boolean showAllDemos = false;
    private boolean reviewDismissed = false;

    /**
     * 상태바·내비게이션바·창 바탕을 앱 바탕색에 맞춘다 (Figma StatusBar: 밝은 바탕 + 어두운 글자).
     * 어두운 화면(Ui.dark)이면 시스템 아이콘도 밝게. Ui.applyTheme() 다음에 부른다.
     */
    static void styleWindow(Activity a) {
        Window w = a.getWindow();
        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Ui.BG)); // 화면이 그려지기 전 흰 번쩍임 방지
        w.setStatusBarColor(Ui.BG);
        int flags = 0;
        if (!Ui.dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) {
            w.setNavigationBarColor(Ui.WHITE);
            if (!Ui.dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        w.getDecorView().setSystemUiVisibility(flags);
    }

    /** 이 화면을 만들 때 쓴 색 한 벌 (어두운 화면이었는지). 돌아왔을 때 바뀌었으면 다시 만든다. */
    private boolean builtDark;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        UiPrefs.load(this);
        Ui.applyTheme(this); // 다크 모드: 화면을 만들기 전에 색 한 벌을 고른다
        builtDark = Ui.dark;
        if (b != null) page = b.getInt("page", PAGE_HOME); // 화면 모드를 바꿔 다시 만들어도 보던 화면 그대로
        styleWindow(this);
        setContentView(buildLayout());
        BackendSlots.load(this);
        getWindow().setNavigationBarColor(Ui.BG); // 하단 탭이 없어 시스템 내비게이션 바도 바탕색으로
        if (!store.settings().onboarded) onboardStep = ONBOARD_PERMISSION;
    }

    /** 화면 뼈대: 스크롤 내용(content) · 하단 버튼 영역(footer, 온보딩). */
    View buildLayout() {
        store = Store.get(this);
        layer = new FrameLayout(this);
        LinearLayout root = Ui.v(this);
        root.setBackgroundColor(Ui.BG);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        content = Ui.v(this);
        Ui.pad(content, this, Ui.Pad.SCREEN_H, Ui.Pad.SCREEN_TOP, Ui.Pad.SCREEN_H, Ui.Pad.SCREEN_BOTTOM);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        footer = Ui.v(this);
        Ui.pad(footer, this, Ui.Pad.SCREEN_H, Ui.Pad.FOOTER_TOP, Ui.Pad.SCREEN_H, Ui.Pad.SCREEN_BOTTOM);
        root.addView(footer, Ui.fillW());
        layer.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return layer;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 이어서 onResume이 그 화면을 그린다
        if (requestCode == REQ_GALLERY && resultCode == RESULT_OK) page = PAGE_HOME;
        if (requestCode == REQ_GALLERY && resultCode == UiGalleryActivity.RESULT_REPORT) page = PAGE_REPORT;
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("page", page);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 데모 결제 화면(항상 밝게)이나 UI 갤러리(화면 모드 변경)에서 돌아오면 색을 다시 고른다
        Ui.applyTheme(this);
        if (Ui.dark != builtDark) {
            recreate();
            return;
        }
        refresh();
    }

    @Override
    public void onBackPressed() {
        if (reviewSheet != null) {
            reviewDismissed = true;
            hideReview();
            return;
        }
        if (onboardStep > ONBOARD_PERMISSION) {
            onboardStep--;
            refresh();
            return;
        }
        if (onboardStep == ONBOARD_DONE && page != PAGE_HOME) {
            open(PAGE_HOME);
            return;
        }
        super.onBackPressed();
    }

    /** 화면 하나로 이동 (홈 · 기록 · 리포트 · 설정). */
    private void open(int p) {
        page = p;
        refresh();
    }

    /** 지금 상태(온보딩 단계·화면)에 맞는 화면을 처음부터 다시 그린다. */
    void refresh() {
        content.removeAllViews();
        footer.removeAllViews();
        hideReview();
        scroll.scrollTo(0, 0);
        if (onboardStep != ONBOARD_DONE) {
            footer.setVisibility(View.VISIBLE);
            if (onboardStep == ONBOARD_PERMISSION) onboardingPermissionScreen();
            else if (onboardStep == ONBOARD_INCOME) onboardingIncomeScreen();
            else onboardingBudgetScreen();
            return;
        }
        footer.setVisibility(View.GONE);
        if (page == PAGE_HOME) homeScreen();
        else if (page == PAGE_RECORDS) recordsScreen();
        else if (page == PAGE_REPORT) reportScreen();
        else settingsScreen();
    }

    // ================================================================== 공통

    /** 화면 내용에 블록 하나를 붙인다 (위 간격 gapDp). */
    private void add(View v, int gapDp) {
        content.addView(v, Ui.fillW());
        if (gapDp > 0) Ui.gapTop(v, this, gapDp);
    }

    private void footerButton(TextView b) {
        footer.addView(b, Ui.fillW());
    }

    private void goOnboarding(int step) {
        onboardStep = step;
        refresh();
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

    /** build.gradle.kts의 versionName (예: "0.5"). */
    private String appVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    // ------------------------------------------------------------------ 화면 문구 만들기 (값 → 글자)

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

    /** 기록 한 줄 아래 설명: 시각 · 강도 · 야식 · 고른 이유. */
    private String recordSub(Record r) {
        String sub = time(r.ts);
        if (r.intensity != null) sub += " · " + r.intensity.label;
        if (r.lateNight) sub += " · 야식";
        if (r.chip != null) sub += " · \"" + r.chip + "\"";
        return sub;
    }

    /** 1,000,000 → "100만" */
    private static String man(long v) {
        return v % 10_000 == 0 ? (v / 10_000) + "만" : Money.num(v);
    }

    private static int pct(int a, int b) {
        return b == 0 ? 0 : Math.round(a * 100f / b);
    }

    // ------------------------------------------------------------------ 공통 부품 (이 화면들에서만)

    /** 식비 예산에서 나간 기록인지 (계좌 동기화 식비, 또는 여윳돈 없이 식비만 쓴 결제). */
    private static boolean isFoodOnly(Record r) {
        return r.outcome == Outcome.SYNC ? r.food > 0 : r.food > 0 && r.discretionary == 0;
    }

    /** 기록 결과 이름 (태그 글자). */
    private static String outcomeLabel(Record r) {
        if (r.outcome == Outcome.PREVENTED || r.outcome == Outcome.REASON_PAY || r.outcome == Outcome.DELIBERATE_PAY) return r.outcome.label;
        if (isFoodOnly(r)) return "식비 예산";
        return r.outcome == Outcome.SYNC ? "여윳돈" : r.outcome.label;
    }

    /** 기록 결과 색. 태그와 왼쪽 점이 같은 규칙을 쓴다. */
    private static Ui.Tone outcomeTone(Record r) {
        switch (r.outcome) {
            case PREVENTED:
                return Ui.Tone.SUCCESS;      // 멈춤: 초록
            case REASON_PAY:
                return Ui.Tone.ACCENT;       // 이유 선택 결제: 연초록 태그, 먹색 점
            case DELIBERATE_PAY:
                return Ui.Tone.WARNING;      // 숙고 후 결제: 노랑
            default:
                return isFoodOnly(r) ? Ui.Tone.SUCCESS : Ui.Tone.NEUTRAL; // 식비 예산: 초록, 나머지: 회색
        }
    }

    /** 기록 줄 왼쪽 점 색. 태그 글자색을 따르되, 멈춤(초록)과 구분되게 이유 선택 결제는 먹색. */
    private static int outcomeDotColor(Ui.Tone tone) {
        if (tone == Ui.Tone.ACCENT) return Ui.BRAND;
        if (tone == Ui.Tone.NEUTRAL) return Ui.LINE;
        return tone.fg;
    }

    private TextView outcomeTag(Record r) {
        return Ui.smallTag(this, outcomeLabel(r), outcomeTone(r));
    }

    /**
     * 기록 한 줄의 모양. 결과 색 표시는 UiPrefs.recordColor (UI 갤러리 › 앱 UI 설정, 기본 TINT).
     * TINT 줄 전체 연한 바탕(기본) · 후보: DOT 왼쪽 점 · BAR 왼쪽 세로 막대 · NONE 태그만.
     */
    private LinearLayout recordRowView(Record r) {
        Ui.Tone tone = outcomeTone(r);
        String title = recordTitle(r), sub = recordSub(r), amount = Money.won(r.amount);
        switch (UiPrefs.recordColor) {
            case BAR:
                return Ui.listRowBar(this, outcomeDotColor(tone), title, sub, amount, outcomeTag(r));
            case TINT: {
                LinearLayout row = Ui.listRow(this, title, sub, amount, outcomeTag(r));
                Ui.tintRow(this, row, tone.bg);
                return row;
            }
            case NONE:
                return Ui.listRow(this, title, sub, amount, outcomeTag(r));
            default:
                return Ui.listRow(this, outcomeDotColor(tone), title, sub, amount, outcomeTag(r));
        }
    }

    /** 기록 한 줄 (홈 최근 기록 · 기록 탭): 결과 색 · 제목 · 금액 · 태그. deletable이면 길게 눌러 지운다. */
    private View recordRow(Record r, boolean deletable) {
        LinearLayout row = recordRowView(r);
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

    // ================================================================== 온보딩 (S01~S03)

    /** 온보딩 윗줄: ← 뒤로 + 단계 점. 첫 단계는 뒤로 없이 점만. */
    private View onboardingTopBar(int step) {
        LinearLayout r = Ui.h(this);
        if (step > ONBOARD_PERMISSION) {
            r.addView(Ui.backButton(this, v -> goOnboarding(step - 1)), Ui.square(this, Ui.Size.ICON_BUTTON));
            Ui.inline(r, Ui.steps(this, step, ONBOARD_BUDGET, true), Ui.Space.BLOCK);
        } else {
            Ui.pad(r, this, 0, Ui.Pad.STEPS_TOP, 0, Ui.Pad.STEPS_BOTTOM);
            r.addView(Ui.steps(this, step, ONBOARD_BUDGET, true));
        }
        return r;
    }

    /** S01 권한 안내. */
    private void onboardingPermissionScreen() {
        boolean on = serviceEnabled();
        add(onboardingTopBar(ONBOARD_PERMISSION), 0);
        add(Ui.title(this, "결제 직전에\n쉼표 하나 찍어드릴게요"), Ui.Space.ITEM);
        add(Ui.bodySmall(this, "이번 달 돈 흐름에 무리가 될 때만 결제 화면 위에서 질문을 건네요. 결정은 언제나 내가 해요."), Ui.Space.STACK);
        add(permissionCard(on), Ui.Space.STACK);
        add(Ui.tintNote(this, "비밀번호·카드번호·메시지는 읽지 않아요. 상품명과 금액만 판단에 써요.",
                Ui.Font.CAPTION, Ui.GREEN, Ui.Radius.BOX, Ui.Pad.PRIVACY_NOTE, Ui.Pad.PRIVACY_NOTE), Ui.Space.STACK);

        if (on) {
            footerButton(Ui.button(this, "다음", true, v -> goOnboarding(ONBOARD_INCOME)));
        } else {
            footerButton(Ui.button(this, "허용하러 가기", true, v -> openA11ySettings()));
            Ui.stack(footer, Ui.linkButton(this, "나중에 켤게요 (설정 탭에서 켤 수 있어요)", v -> goOnboarding(ONBOARD_INCOME)), Ui.Space.LINE);
        }
    }

    private View permissionCard(boolean on) {
        LinearLayout c = Ui.card(this);
        c.setBackground(Ui.round(this, Ui.WHITE, Ui.Radius.BOX));
        c.addView(permissionRow(true, "접근성 서비스", "결제 화면의 상품명과 금액을 읽어요", on), Ui.fillW());
        Ui.stack(c, permissionRow(true, "결제 화면 위 질문 표시", "결제 화면 위에 질문을 띄워요 (접근성 서비스로 함께 허용돼요)", on), Ui.Space.STACK);
        Ui.stack(c, permissionRow(false, "알림", "다음 날 어제 결제를 돌아보자고 물어봐요", BackendSlots.get().notificationAllowed), Ui.Space.STACK);
        return c;
    }

    /** 권한 한 줄: [필수/선택] 태그 · 이름(허용되면 ✓) · 설명. */
    private View permissionRow(boolean required, String title, String desc, boolean granted) {
        LinearLayout r = Ui.h(this);
        r.setGravity(Gravity.TOP);
        r.addView(required ? Ui.smallTag(this, "필수", Ui.Tone.ACCENT) : Ui.smallTag(this, "선택", Ui.Tone.NEUTRAL), Ui.wrap());
        LinearLayout t = Ui.v(this);
        t.addView(Ui.text(this, title + (granted ? "  ✓" : ""), Ui.Font.LABEL, Ui.TEXT, Ui.BOLD));
        Ui.stack(t, Ui.meta(this, desc), Ui.Space.TEXT);
        Ui.inlineFill(r, t, Ui.Space.ROW);
        return r;
    }

    /** S02 정기 수입 확인. */
    private void onboardingIncomeScreen() {
        Settings s = store.settings();
        add(onboardingTopBar(ONBOARD_INCOME), 0);
        add(Ui.title(this, "정기 수입을\n확인했어요"), Ui.Space.WIDE);
        BackendSlots data = BackendSlots.get();
        boolean demo = data.account == null || data.account.demo;
        add(Ui.body(this, "최근 " + data.incomeStreakMonths + "개월 입출금 내역으로 매달 들어오는 돈을 찾았어요." + (demo ? " (데모: Mock 계좌)" : "")), Ui.Space.WIDE);
        add(linkedAccountCard(data.account == null ? "연결된 계좌가 없어요" : data.account.displayName()), Ui.Space.WIDE);
        add(incomeCard(s, data), Ui.Space.WIDE);
        add(consentCard(), Ui.Space.WIDE);

        TextView next = Ui.button(this, "다음", true, v -> {
            if (!bankConsent) return;
            goOnboarding(ONBOARD_BUDGET);
        });
        Ui.setEnabled(next, bankConsent);
        footerButton(next);
    }

    private View linkedAccountCard(String account) {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.caption(this, "연결된 계좌"));
        Ui.stack(c, Ui.text(this, account, Ui.Font.BODY, Ui.TEXT, Ui.MEDIUM), Ui.Space.TIGHT);
        return c;
    }

    /** 정기 수입 카드. 내역(BackendSlots)의 합계가 월 수입과 같을 때만 "용돈 … + 과외비 …"를 보여 준다. */
    private View incomeCard(Settings s, BackendSlots data) {
        LinearLayout c = Ui.tintCard(this, Ui.BRAND_LIGHT);
        c.addView(Ui.text(this, "정기 수입", Ui.Font.CAPTION, Ui.BRAND, Ui.MEDIUM));
        Ui.stack(c, Ui.heading(this, "매달 " + s.payday + "일 · 약 " + Money.won(s.income)), Ui.Space.ITEM);
        String detail;
        if (!data.incomeSources.isEmpty() && data.incomeTotal() == s.income) {
            StringBuilder b = new StringBuilder();
            for (BackendSlots.IncomeSource src : data.incomeSources) {
                if (b.length() > 0) b.append(" + ");
                b.append(src.label).append(' ').append(Money.won(src.amount));
            }
            detail = b + " (" + data.incomeStreakMonths + "개월 연속)";
        } else {
            detail = "최근 " + data.incomeStreakMonths + "개월 연속 같은 날 들어왔어요";
        }
        Ui.stack(c, Ui.meta(this, detail), Ui.Space.ITEM);
        return c;
    }

    private View consentCard() {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.checkRow(this, "계좌 입출금 내역 조회 동의 (필수)", bankConsent, v -> {
            bankConsent = !bankConsent;
            refresh();
        }), Ui.fillW());
        Ui.stack(c, Ui.checkRow(this, "급여일 기준으로 예산 주기 설정", cycleConsent, v -> {
            cycleConsent = !cycleConsent;
            refresh();
        }), Ui.Space.ROW);
        return c;
    }

    /** S03 고정지출·식비 떼어 두기 → 월 여윳돈. */
    private void onboardingBudgetScreen() {
        Settings s = store.settings();
        add(onboardingTopBar(ONBOARD_BUDGET), 0);
        add(Ui.title(this, "이미 정해진 돈부터\n떼어 둘게요"), Ui.Space.BLOCK);
        add(amountListCard("고정지출 5항목 (계좌 내역으로 추정)",
                new String[]{"월세·관리비", "공과금", "통신비", "교통비", "구독"},
                new long[]{s.rent, s.utilities, s.phone, s.transport, s.subscriptions},
                s.fixedTotal()), Ui.Space.BLOCK);
        add(amountListCard("식비 예산 (해 먹는 재료)",
                new String[]{"마트 장보기", "쿠팡 식료품 (첫 달 임시값)"},
                new long[]{s.martBudget, s.coupangFoodBudget},
                s.foodBudget()), Ui.Space.BLOCK);

        long daily = Math.round(s.monthlyDiscretionary() / 30.0 / 100.0) * 100;
        add(Ui.heroCard(this, "월 여윳돈", Money.won(s.monthlyDiscretionary()),
                "수입 " + man(s.income) + " − 고정 " + man(s.fixedTotal()) + " − 식비 " + man(s.foodBudget()) + " · 하루 기준 약 " + Money.won(daily),
                false), Ui.Space.BLOCK);

        footerButton(Ui.button(this, "쉼표 시작하기", true, v -> {
            Settings st = store.settings();
            st.onboarded = true;
            store.saveSettings(st);
            onboardStep = ONBOARD_DONE;
            open(PAGE_HOME);
        }));
    }

    /** 머리말 + 항목별 금액 + 합계 카드. */
    private View amountListCard(String caption, String[] labels, long[] values, long total) {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.caption(this, caption));
        for (int i = 0; i < labels.length; i++) Ui.stack(c, Ui.kv(this, labels[i], Money.won(values[i]), false), Ui.Space.ROW);
        Ui.stack(c, Ui.kv(this, "합계", Money.won(total), true), Ui.Space.ROW);
        return c;
    }

    // ================================================================== 홈 (S04 + S09 회고 시트)

    private void homeScreen() {
        Settings s = store.settings();
        long now = System.currentTimeMillis();
        List<Record> recs = store.records();
        BudgetEngine.State b = BudgetEngine.compute(s, recs, now, zone);
        Stats st = Stats.of(s, recs, now, zone);

        add(homeHeader(s.lastSyncTs, now), 0);
        if (!serviceEnabled()) {
            add(Ui.alertBox(this, "결제 감지가 꺼져 있어요",
                    "설정 › 접근성에서 '쉼표'를 켜야 배민·쿠팡·무신사 결제 화면에서 질문이 떠요.",
                    "접근성 설정 열기 ›", v -> openA11ySettings()), Ui.Space.BLOCK);
        }
        add(todayBudgetCard(b), Ui.Space.BLOCK);
        add(Ui.tilePair(this,
                Ui.statTile(this, "남은 여윳돈", Money.won(b.remainingNow), "급여일까지 " + b.daysLeft + "일", true,
                        tileBar(b.remainingNow, b.monthly), remainColor(b.remainingNow), remainTrack(b.remainingNow)),
                Ui.statTile(this, "식비 예산", Money.won(b.foodRemaining), man(b.foodBudget) + " 원 중 남음", true,
                        tileBar(b.foodRemaining, b.foodBudget), remainColor(b.foodRemaining), remainTrack(b.foodRemaining)),
                Ui.Space.BLOCK), Ui.Space.BLOCK);
        add(Ui.highlightBox(this, "이번 달 쉼표", st.prevented == 0 ? "결제 직전에 멈춘 기록이 여기 쌓여요"
                : st.prevented + "번 멈춰서 " + Money.won(st.preventedSum) + " 아꼈어요", v -> open(PAGE_REPORT)), Ui.Space.BLOCK);
        add(Ui.sectionHeader(this, "최근 기록", "더보기 ›", v -> open(PAGE_RECORDS)), Ui.Space.BLOCK);
        add(recentRecordsCard(recs), Ui.Space.BLOCK);
        add(demoCard(), Ui.Space.BLOCK);

        maybeShowReview(recs, now);
    }

    /** 홈 윗줄: "쉼표" + 마지막 동기화 시각 + 톱니바퀴(설정). */
    private View homeHeader(long lastSyncTs, long now) {
        LinearLayout top = Ui.h(this);
        top.addView(Ui.title(this, "쉼표"), Ui.weight1());
        long mins = lastSyncTs == 0 ? -1 : (now - lastSyncTs) / 60_000;
        String sync = mins < 0 ? "동기화 전" : mins < 1 ? "마지막 동기화 방금 전" : "마지막 동기화 " + (mins < 60 ? mins + "분 전" : (mins / 60) + "시간 전");
        top.addView(Ui.meta(this, sync));
        Ui.inline(top, Ui.settingsButton(this, v -> open(PAGE_SETTINGS)), Ui.Space.LINE, Ui.Size.ICON_BUTTON + 2 * Ui.Pad.ICON);
        return top;
    }

    /**
     * "오늘 쓸 수 있는 돈" 카드: 신호 태그 · 큰 금액 · 진행 막대 · 한도 설명.
     *
     * 색 방식은 UiPrefs.todayCard (UI 갤러리 › 앱 UI 설정에서 바꿈, 기본 SIGNAL).
     * - SIGNAL (기본): 신호에 따라 연초록 / 연노랑 / 연빨강 바탕.
     *   초과일 때 태그 바탕이 카드 바탕과 같은 색이라 태그 테두리가 잘 안 보인다.
     * - 후보 HERO: 먹색 바탕 + 흰 글자 (S03 월 여윳돈 · S10 마음 돌림 비율과 같은 강조 카드)
     * - 후보 OUTLINE: v0.5, 흰 바탕 + 테두리
     */
    private View todayBudgetCard(BudgetEngine.State b) {
        boolean over = b.todaySpent > b.limit;

        // ---- 색 정하기
        LinearLayout c;
        int label, amountColor, meta, barFill, barTrack; // 라벨 · 큰 금액 · 한도 설명 · 진행 막대 · 막대 바탕
        if (UiPrefs.todayCard == UiPrefs.TodayCard.OUTLINE) {
            c = Ui.outlineCard(this, Ui.Pad.TODAY_CARD, Ui.Pad.TODAY_CARD);
            label = Ui.MUTED;
            amountColor = Ui.TEXT;
            meta = Ui.MUTED;
            barFill = over ? Ui.RED : Ui.BRAND;
            barTrack = Ui.BG;
        } else if (UiPrefs.todayCard == UiPrefs.TodayCard.SIGNAL) {
            c = Ui.tintBox(this, signalTone(b.signal()).bg, Ui.Pad.TODAY_CARD, Ui.Pad.TODAY_CARD);
            label = Ui.MUTED;
            amountColor = Ui.TEXT;
            meta = Ui.MUTED;
            barFill = over ? Ui.RED : Ui.BRAND;
            barTrack = Ui.WHITE;
        } else { // HERO
            c = Ui.tintBox(this, Ui.HERO_BG, Ui.Pad.TODAY_CARD, Ui.Pad.TODAY_CARD);
            label = Ui.ON_HERO_SUB;
            amountColor = Ui.ON_HERO;
            meta = Ui.ON_HERO_SUB;
            barFill = over ? Ui.RED_BG : Ui.ON_HERO;
            barTrack = Ui.ON_HERO_TRACK;
        }

        LinearLayout head = Ui.h(this);
        head.addView(Ui.text(this, "오늘 쓸 수 있는 돈", Ui.Font.SMALL, label, Ui.REGULAR), Ui.weight1());
        head.addView(signalTag(b.signal()));
        c.addView(head, Ui.fillW());
        String amount = b.available >= 0 ? Money.won(b.available) + " 남음" : Money.won(-b.available) + " 넘었어요";
        Ui.stack(c, Ui.text(this, amount, Ui.Font.AMOUNT, amountColor, Ui.BOLD), Ui.Space.ITEM);
        float ratio = b.limit == 0 ? 1f : Math.min(1f, (float) b.todaySpent / b.limit);
        Ui.stack(c, Ui.bar(this, ratio, barFill, barTrack), Ui.Space.ITEM);
        Ui.stack(c, Ui.text(this, "오늘 한도 " + Money.won(b.limit) + " 중 " + Money.won(b.todaySpent) + " 사용 · 주기 " + b.dayIndex + "일째",
                Ui.Font.CAPTION, meta, Ui.REGULAR), Ui.Space.ITEM);
        return c;
    }

    /** 남은 비율 (0~1). 전체가 0이면 0. */
    private static float remainRatio(long remaining, long total) {
        return total <= 0 ? 0f : Math.max(0f, Math.min(1f, (float) remaining / total));
    }

    /** 요약 타일 막대 비율. UiPrefs.tileBars가 꺼져 있으면 -1 (막대 없음). */
    private static float tileBar(long remaining, long total) {
        return UiPrefs.tileBars ? remainRatio(remaining, total) : -1f;
    }

    /** 요약 타일 막대 색: 남아 있으면 초록, 다 쓰면 빨강. */
    private static int remainColor(long remaining) {
        return remaining > 0 ? Ui.GREEN : Ui.RED;
    }

    /** 요약 타일 막대 바탕: 평소 회색, 다 쓰면 연빨강 (빈 막대여도 다 쓴 게 보이게). */
    private static int remainTrack(long remaining) {
        return remaining > 0 ? Ui.BG : Ui.RED_BG;
    }

    /** 0 여유 · 1 주의 · 2 초과 */
    private static Ui.Tone signalTone(int signal) {
        return signal == 0 ? Ui.Tone.SUCCESS : signal == 1 ? Ui.Tone.WARNING : Ui.Tone.DANGER;
    }

    /** 0 여유 · 1 주의 · 2 초과 */
    private TextView signalTag(int signal) {
        return Ui.smallTag(this, signal == 0 ? "여유" : signal == 1 ? "주의" : "초과", signalTone(signal));
    }

    private View recentRecordsCard(List<Record> recs) {
        LinearLayout list = Ui.outlineCard(this, Ui.Pad.CARD, Ui.Pad.CARD);
        List<Record> sorted = new ArrayList<>(recs);
        Collections.sort(sorted, (x, y) -> Long.compare(y.ts, x.ts));
        for (int i = 0; i < sorted.size() && i < HOME_RECENT_COUNT; i++) {
            Ui.stack(list, recordRow(sorted.get(i), false), i == 0 ? 0 : Ui.Space.BLOCK);
        }
        if (sorted.isEmpty()) list.addView(Ui.bodySmall(this, "아직 기록이 없어요. 아래 데모 결제로 체험해 보세요."));
        return list;
    }

    /** D1 "데모 결제로 체험하기" 카드. 처음엔 3개, 더 보기로 전부. */
    private View demoCard() {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.caption(this, "데모 결제로 체험하기"));
        int all = DemoCheckoutActivity.PRESETS.length;
        int count = showAllDemos ? all : DEMO_PREVIEW_COUNT;
        for (int i = 0; i < count; i++) {
            final int idx = i;
            DemoCheckoutActivity.Preset p = DemoCheckoutActivity.PRESETS[i];
            LinearLayout r = Ui.h(this);
            Ui.pad(r, this, 0, Ui.Pad.LIST_ROW_V);
            r.addView(Ui.text(this, p.label, Ui.Font.BODY, Ui.TEXT, Ui.MEDIUM), Ui.weight1());
            r.addView(Ui.text(this, Money.won(p.amount) + " › " + p.expect, Ui.Font.CAPTION, Ui.BRAND, Ui.MEDIUM));
            r.setOnClickListener(v -> startActivity(new Intent(this, DemoCheckoutActivity.class).putExtra(DemoCheckoutActivity.EXTRA_PRESET, idx)));
            Ui.stack(c, r, i == 0 ? Ui.Space.TEXT : 0);
        }
        TextView more = Ui.linkButton(this, showAllDemos ? "접기" : "다른 시나리오 " + (all - DEMO_PREVIEW_COUNT) + "개 더 보기", v -> {
            showAllDemos = !showAllDemos;
            refresh();
        });
        more.setGravity(Gravity.START);
        Ui.pad(more, this, 0, Ui.Space.TIGHT, 0, 0);
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

    /** 회고 시트: 어두운 바탕 + 아래 시트. 바탕을 누르면 오늘은 다시 묻지 않는다. */
    private void showReview(Record r, int total) {
        FrameLayout box = new FrameLayout(this);
        View dim = new View(this);
        dim.setBackgroundColor(Ui.DIM);
        dim.setOnClickListener(v -> {
            reviewDismissed = true;
            hideReview();
        });
        box.addView(dim, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        box.addView(reviewSheetContent(r, total),
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        layer.addView(box, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        reviewSheet = box;
    }

    private View reviewSheetContent(Record r, int total) {
        LinearLayout sheet = Ui.bottomSheet(this, Ui.Pad.REVIEW_SHEET_TOP);
        LinearLayout tags = Ui.h(this);
        tags.addView(Ui.tag(this, "어제 결제 돌아보기", Ui.Tone.BRAND), Ui.wrap());
        if (total > 1) Ui.inline(tags, Ui.text(this, total + "건 중 1번째", Ui.Font.CAPTION, Ui.SUBTLE, Ui.MEDIUM), Ui.Space.ITEM);
        sheet.addView(tags, Ui.fillW());

        LocalDate d = Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate();
        String when = d.equals(LocalDate.now(zone).minusDays(1)) ? "어제" : d.getMonthValue() + "/" + d.getDayOfMonth();
        Ui.stack(sheet, Ui.heading(this, when + " " + appShort(r.app) + " " + r.title + " " + Money.won(r.amount) + ",\n만족하시나요?"), Ui.Space.BLOCK);
        String why = r.outcome == Outcome.DELIBERATE_PAY
                ? "[" + (r.app == TargetApp.BAEMIN ? "그래도 시킬게요" : "그래도 살게요") + "]로 결제한 건이에요. 답은 다음 대화의 질문 재료가 돼요."
                : "'" + r.chip + "' 이유를 고르고 결제한 건이에요. 답은 다음 대화의 질문 재료가 돼요.";
        Ui.stack(sheet, Ui.meta(this, why), Ui.Space.BLOCK);
        Ui.stack(sheet, Ui.buttonPair(this,
                Ui.button(this, "만족해요", false, v -> answerReview(r, "만족해요")),
                Ui.button(this, "후회돼요", false, v -> answerReview(r, "후회돼요"))), Ui.Space.BLOCK);
        return sheet;
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

    // ================================================================== 기록 (홈 › 최근 기록 더보기)

    private void recordsScreen() {
        add(Ui.screenHeader(this, "기록", v -> open(PAGE_HOME)), 0);
        add(Ui.bodySmall(this, "길게 누르면 지울 수 있어요 (결제하지 않았는데 기록된 건 등)."), Ui.Space.TIGHT);
        List<Record> sorted = store.records();
        Collections.sort(sorted, (x, y) -> Long.compare(y.ts, x.ts));
        LinearLayout list = Ui.outlineCard(this, Ui.Pad.CARD, Ui.Pad.CARD);
        boolean first = true;
        for (Record r : sorted) {
            if (!first) {
                View d = Ui.divider(this); // 높이 1dp LayoutParams를 그대로 쓴다
                list.addView(d);
                Ui.gapTop(d, this, Ui.Space.BLOCK);
            }
            Ui.stack(list, recordRow(r, true), first ? 0 : Ui.Space.BLOCK);
            for (String line : r.qa) {
                TextView q = Ui.meta(this, line);
                Ui.pad(q, this, Ui.Pad.QA_INDENT, 0, 0, 0);
                Ui.stack(list, q, Ui.Space.LINE);
            }
            first = false;
        }
        if (sorted.isEmpty()) list.addView(Ui.bodySmall(this, "아직 기록이 없어요."));
        add(list, Ui.Space.BLOCK);
    }

    // ================================================================== 리포트 (S10, 홈 › 이번 달 쉼표)

    private void reportScreen() {
        Settings s = store.settings();
        long now = System.currentTimeMillis();
        List<Record> recs = store.records();
        Stats st = Stats.of(s, recs, now, zone);
        BudgetEngine.State b = BudgetEngine.compute(s, recs, now, zone);

        add(Ui.screenHeader(this, LocalDate.now(zone).getMonthValue() + "월 리포트", v -> open(PAGE_HOME)), 0);
        add(Ui.text(this, b.cycleStart + " ~ " + b.nextPayday.minusDays(1) + " (급여일 기준)", Ui.Font.CAPTION, Ui.SUBTLE, Ui.REGULAR), Ui.Space.LINE);

        // 내용 후보: UiPrefs.reportStyle (UI 갤러리 › 앱 UI 설정 › 리포트 내용). ②~④는 ReportViews.
        if (UiPrefs.reportStyle != UiPrefs.ReportStyle.NOW) {
            ReportViews.Data d = ReportViews.Data.of(s, recs, now, zone);
            if (UiPrefs.reportStyle == UiPrefs.ReportStyle.VALUES) ReportViews.values(this, content, d, zone);
            else if (UiPrefs.reportStyle == UiPrefs.ReportStyle.STORY) ReportViews.story(this, content, d, zone);
            else ReportViews.simple(this, content, d);
            add(foodNote(b), Ui.Space.BLOCK);
            return;
        }

        int rate = st.mindChangeRate();
        add(Ui.heroCard(this, "마음 돌림 비율", rate < 0 ? "–" : rate + "%",
                st.interventions == 0 ? "아직 쉼표가 말을 건 결제가 없어요"
                        : "쉼표가 말을 건 " + st.interventions + "번 중 " + st.prevented + "번 그만뒀어요 · " + Money.won(st.preventedSum) + " 아낌",
                true), Ui.Space.BLOCK);
        add(intensityCard(st), Ui.Space.BLOCK);

        int diff = st.lateCount - st.prevLateCount;
        add(Ui.tilePair(this,
                Ui.statTile(this, "야식", st.lateCount + "번",
                        Money.won(st.lateSum) + " · 지난달 " + (diff > 0 ? "+" : diff < 0 ? "−" : "±") + Math.abs(diff) + "번", false),
                Ui.statTile(this, "회고 후회", st.regret + "/" + st.reviewed, "후회 응답 " + pct(st.regret, st.reviewed) + "%", false),
                Ui.Space.ROW), Ui.Space.BLOCK);
        add(outcomeCard(st), Ui.Space.BLOCK);
        add(foodNote(b), Ui.Space.BLOCK);
    }

    /** 리포트 맨 아래 식비 예산 한 줄. */
    private View foodNote(BudgetEngine.State b) {
        return Ui.tintNote(this, "식비 예산 " + Money.won(b.foodSpent) + " / " + Money.won(b.foodBudget) + " 사용 · 다음 달부터 쿠팡 식료품 실측값으로 보정해요",
                Ui.Font.CAPTION, Ui.BRAND, Ui.Radius.INPUT, Ui.Pad.FOOD_NOTE_H, Ui.Pad.FOOD_NOTE_V);
    }

    /** 강도별 멈춘 비율: 강도 이름 · 막대 · "멈춤/전체 · %". */
    private View intensityCard(Stats st) {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.caption(this, "강도별 멈춘 비율"));
        for (Intensity i : Intensity.values()) {
            int[] v = st.byIntensity.get(i);
            LinearLayout r = Ui.h(this);
            r.addView(Ui.text(this, i.label, Ui.Font.BODY, Ui.TEXT, Ui.MEDIUM),
                    new LinearLayout.LayoutParams(Ui.dp(this, Ui.Size.ROW_LABEL), ViewGroup.LayoutParams.WRAP_CONTENT));
            Ui.inlineFill(r, Ui.bar(this, v[0] == 0 ? 0f : (float) v[1] / v[0], Ui.SUBTLE, Ui.LINE), Ui.Space.ROW);
            Ui.inline(r, Ui.caption(this, v[1] + "/" + v[0] + " · " + pct(v[1], v[0]) + "%"), Ui.Space.ROW);
            Ui.stack(c, r, Ui.Space.ROW);
        }
        return c;
    }

    /** 결과 분류: 결과별 건수 · 금액. */
    private View outcomeCard(Stats st) {
        Outcome[] kinds = {Outcome.PREVENTED, Outcome.REASON_PAY, Outcome.DELIBERATE_PAY, Outcome.EXIT};
        int total = 0;
        for (Outcome o : kinds) total += st.byOutcome.get(o);
        LinearLayout c = Ui.card(this);
        c.addView(Ui.caption(this, "결과 분류 (" + total + "건)"));
        for (Outcome o : kinds) {
            int n = st.byOutcome.get(o);
            long sum = st.byOutcomeSum.get(o);
            Ui.stack(c, Ui.kv(this, o.label, n + "건" + (o == Outcome.EXIT || n == 0 ? "" : " · " + Money.won(sum)), false), Ui.Space.ITEM);
        }
        return c;
    }

    // ================================================================== 설정 · 테스트 도구 (홈 › 톱니바퀴)

    /** 화면 모드: 기기 따라 · 밝게 · 어둡게 (UiPrefs.theme). 고르면 바로 다시 그린다. */
    private View themeCard() {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.caption(this, "화면 모드 (자동: 휴대폰 다크 모드 설정을 따라요)"));
        List<TextView> chips = new ArrayList<>();
        for (UiPrefs.Theme t : UiPrefs.Theme.values()) {
            chips.add(Ui.chip(this, t.label, UiPrefs.theme == t, v -> {
                UiPrefs.theme = t;
                UiPrefs.save(this);
                recreate();
            }));
        }
        Ui.stack(c, Ui.chipGrid(this, chips, 3), Ui.Space.ITEM);
        return c;
    }

    private void settingsScreen() {
        Settings s = store.settings();
        add(Ui.screenHeader(this, "설정", v -> open(PAGE_HOME)), 0);
        add(statusCard(), Ui.Space.BLOCK);
        add(themeCard(), Ui.Space.BLOCK);

        // AI
        add(Ui.sectionTitle(this, "AI (Claude API)"), Ui.Space.SECTION);
        LinearLayout ac = Ui.card(this);
        EditText key = Ui.input(this, "sk-ant-... (비우면 기본 질문·키워드 판정)", s.apiKey, false);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText model = Ui.input(this, "모델", s.model, false);
        EditText timeout = Ui.input(this, "대기 시간(ms)", String.valueOf(s.aiTimeoutMs), true);
        ac.addView(Ui.meta(this, "API 키"));
        ac.addView(key);
        Ui.stack(ac, Ui.labeledField(this, "모델", model), Ui.Space.ITEM);
        Ui.stack(ac, Ui.labeledField(this, "응답 대기 시간(ms)", timeout), Ui.Space.ITEM);
        ac.addView(Ui.micro(this, "키가 있으면 쿠팡·무신사 품목 판정과 질문을 AI가 만들어요. 대기 시간 안에 응답이 없으면 키워드 판정·기본 질문으로 바로 대체해요 (PRD 기본 2,000ms).", Ui.MUTED));
        if (!ClaudeClient.lastError.isEmpty()) ac.addView(Ui.micro(this, "마지막 AI 오류: " + ClaudeClient.lastError, Ui.RED));
        add(ac, Ui.Space.ITEM);

        // 예산
        add(Ui.sectionTitle(this, "예산"), Ui.Space.SECTION);
        LinearLayout bc = Ui.card(this);
        EditText income = numberField(bc, "월 수입", s.income);
        EditText payday = numberField(bc, "급여일 (1~28)", s.payday);
        EditText rent = numberField(bc, "월세·관리비", s.rent);
        EditText util = numberField(bc, "공과금", s.utilities);
        EditText phone = numberField(bc, "통신비", s.phone);
        EditText trans = numberField(bc, "교통비", s.transport);
        EditText subs = numberField(bc, "구독료", s.subscriptions);
        EditText mart = numberField(bc, "식비: 마트", s.martBudget);
        EditText cpg = numberField(bc, "식비: 쿠팡 식료품", s.coupangFoodBudget);
        add(bc, Ui.Space.ITEM);

        // 야식
        add(Ui.sectionTitle(this, "야식 반복 트리거"), Ui.Space.SECTION);
        LinearLayout lc = Ui.card(this);
        final boolean[] lateOn = {s.lateEnabled};
        TextView toggle = Ui.chip(this, lateOn[0] ? "켜짐" : "꺼짐", lateOn[0], null);
        toggle.setOnClickListener(v -> {
            lateOn[0] = !lateOn[0];
            toggle.setText(lateOn[0] ? "켜짐" : "꺼짐");
        });
        LinearLayout tr = Ui.h(this);
        tr.addView(Ui.bodySmall(this, "야식 반복 개입"), Ui.weight1());
        tr.addView(toggle);
        lc.addView(tr);
        EditText ls = numberField(lc, "시작 시각 (시)", s.lateStartHour);
        EditText le = numberField(lc, "끝 시각 (시)", s.lateEndHour);
        EditText lw = numberField(lc, "최근 며칠", s.lateWindowDays);
        EditText ln = numberField(lc, "몇 번째부터 개입", s.lateCount);
        add(lc, Ui.Space.ITEM);

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
        }), Ui.Space.STACK);

        testTools(s);
    }

    /** 설정 상태 카드: 결제 감지 · 현재 세션 · 앱 버전 · 접근성 설정 링크. */
    private View statusCard() {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.kv(this, "결제 감지(접근성 서비스)", serviceEnabled() ? "켜짐" : "꺼짐", true));
        Ui.stack(c, Ui.kv(this, "현재 세션", GuardService.instance == null ? "서비스 미연결" : GuardService.instance.sessionState(), false), Ui.Space.ITEM);
        Ui.stack(c, Ui.kv(this, "앱 버전", appVersion() + " · Figma Hi-Fi 디자인", false), Ui.Space.ITEM);
        TextView open = Ui.textLink(this, "접근성 설정 열기 ›", Ui.BRAND, v -> openA11ySettings());
        Ui.pad(open, this, 0, Ui.Space.ITEM, 0, 0);
        c.addView(open);
        return c;
    }

    /** 카드에 "라벨 · 숫자 입력칸" 한 줄을 붙이고 입력칸을 돌려준다. */
    private EditText numberField(LinearLayout card, String label, long value) {
        EditText in = Ui.input(this, "", String.valueOf(value), true);
        Ui.stack(card, Ui.labeledField(this, label, in), Ui.Space.ITEM);
        return in;
    }

    private void testTools(Settings s) {
        add(Ui.sectionTitle(this, "테스트 도구 (시연용)"), Ui.Space.LARGE);
        LinearLayout tc = Ui.card(this);
        tc.addView(Ui.body(this, "시나리오 기준: 수입 100만 · 고정 50만 · 식비 10만 → 여윳돈 40만, 10일째 적용 한도 14,000원."));
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
        tc.addView(testButton("결과별 예시 기록 4건 추가 (멈춤·이유 선택·숙고 후·식비, 기록 색 확인용)", () -> {
            addSampleOutcomes();
            toast("예시 기록 4건을 넣었어요");
        }));
        tc.addView(testButton("리포트 예시 기록 12건 추가 (고른 이유·다음 날 만족/후회 포함, 리포트 후보 확인용)", () -> {
            addSampleMonth();
            toast("이번 주기에 예시 기록 12건을 넣었어요");
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
        tc.addView(testButton("UI 갤러리 열기 (토큰·컴포넌트 모음)", () -> startActivityForResult(new Intent(this, UiGalleryActivity.class), REQ_GALLERY)));
        tc.addView(testButton("기록 전체 삭제", () -> {
            store.clearRecords();
            toast("기록을 모두 지웠어요");
        }));
        tc.addView(testButton("온보딩 다시 보기", () -> onboardStep = ONBOARD_PERMISSION));
        add(tc, Ui.Space.ITEM);

        add(Ui.sectionTitle(this, "감지 로그"), Ui.Space.SECTION);
        add(logCard(), Ui.Space.ITEM);

        add(Ui.sectionTitle(this, "마지막으로 읽은 화면 텍스트"), Ui.Space.SECTION);
        add(Ui.body(this, "대상 앱 결제 화면이 감지되지 않으면 여기 텍스트를 확인해 주세요. 결제 버튼·결제 섹션 라벨이 읽히는지, 금액이 맞는지 볼 수 있어요."), Ui.Space.LINE);
        for (TargetApp app : TargetApp.values()) add(screenTextCard(app), Ui.Space.ITEM);
    }

    private View logCard() {
        LinearLayout c = Ui.card(this);
        List<String> logs = store.logs();
        Collections.reverse(logs);
        for (int i = 0; i < logs.size() && i < LOG_COUNT; i++) {
            TextView t = Ui.micro(this, logs.get(i), Ui.TEXT);
            t.setTextIsSelectable(true);
            c.addView(t);
        }
        if (logs.isEmpty()) c.addView(Ui.body(this, "아직 로그가 없어요."));
        return c;
    }

    private View screenTextCard(TargetApp app) {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, app.label + " (" + app.pkg + ")", Ui.Font.SMALL, Ui.TEXT, Ui.BOLD));
        String t = store.lastScreen(app);
        TextView tv = Ui.micro(this, t.isEmpty() ? "아직 읽은 화면이 없어요." : (t.length() > SCREEN_TEXT_MAX ? t.substring(0, SCREEN_TEXT_MAX) + "\n…" : t), Ui.MUTED);
        tv.setTextIsSelectable(true);
        c.addView(tv);
        return c;
    }

    /** 테스트 도구 한 줄 버튼: 누르면 동작 후 화면을 다시 그린다. */
    private View testButton(String label, Runnable r) {
        TextView t = Ui.textLink(this, label, Ui.BRAND, v -> {
            r.run();
            refresh();
        });
        Ui.pad(t, this, 0, Ui.Pad.LIST_ROW_V);
        return t;
    }

    /**
     * 기록 색 확인용 예시: 결과마다 1건씩 오늘 기록으로 넣는다 (Mock).
     * 실제 개입 기록처럼 결제한 건만 여윳돈·식비에 반영한다. 오늘 시각이라 다음 날 회고 시트는 뜨지 않는다.
     */
    private void addSampleOutcomes() {
        long now = System.currentTimeMillis();
        Object[][] samples = {
                // 앱, 제목, 금액, 결과, 강도, 고른 이유, 식비
                {TargetApp.MUSINSA, "울 코트 (Mock)", 129_000L, Outcome.PREVENTED, Intensity.STRONG, null, false},
                {TargetApp.BAEMIN, "치킨 세트 (Mock)", 23_000L, Outcome.REASON_PAY, Intensity.MEDIUM, "배고파서", false},
                {TargetApp.MUSINSA, "셔츠 (Mock)", 39_000L, Outcome.DELIBERATE_PAY, Intensity.MEDIUM, null, false},
                {TargetApp.COUPANG, "식료품 (Mock)", 18_000L, Outcome.PASS, null, null, true},
        };
        for (int i = 0; i < samples.length; i++) {
            Object[] s = samples[i];
            Record r = new Record();
            r.ts = now - (samples.length - i) * 60_000L;
            r.app = (TargetApp) s[0];
            r.title = (String) s[1];
            r.amount = (Long) s[2];
            r.outcome = (Outcome) s[3];
            r.intensity = (Intensity) s[4];
            r.chip = (String) s[5];
            boolean food = (Boolean) s[6];
            if (r.outcome.spend) {
                if (food) r.food = r.amount;
                else r.discretionary = r.amount;
            }
            store.addRecord(r);
        }
    }

    /** 리포트 후보 확인용: 이번 급여일 주기에 고루 퍼진 12건 (고른 이유 · 다음 날 회고 · 멈춘 질문 포함). */
    private void addSampleMonth() {
        long now = System.currentTimeMillis();
        BudgetEngine.State b = BudgetEngine.compute(store.settings(), store.records(), now, zone);
        long start = b.cycleStart.atStartOfDay(zone).toInstant().toEpochMilli();
        Object[][] samples = {
                // 앱, 제목, 금액, 결과, 강도, 고른 이유, 다음 날 회고, 멈춘 질문, 야식, 식비
                {TargetApp.MUSINSA, "울 코트 (예시)", 129_000L, Outcome.PREVENTED, Intensity.STRONG, null, null, 3, false, false},
                {TargetApp.BAEMIN, "치킨 세트 (예시)", 23_000L, Outcome.REASON_PAY, Intensity.LIGHT, "그냥 먹고 싶어요", "후회돼요", 0, true, false},
                {TargetApp.BAEMIN, "떡볶이 (예시)", 16_000L, Outcome.REASON_PAY, Intensity.LIGHT, "끼니 대용이에요", "만족해요", 0, false, false},
                {TargetApp.COUPANG, "무선 이어폰 (예시)", 89_000L, Outcome.PREVENTED, Intensity.MEDIUM, null, null, 1, false, false},
                {TargetApp.COUPANG, "휴지·세제 (예시)", 26_000L, Outcome.REASON_PAY, Intensity.LIGHT, "다 떨어졌어요", "만족해요", 0, false, false},
                {TargetApp.MUSINSA, "셔츠 (예시)", 39_000L, Outcome.DELIBERATE_PAY, Intensity.MEDIUM, null, "후회돼요", 0, false, false},
                {TargetApp.BAEMIN, "피자 (예시)", 28_000L, Outcome.REASON_PAY, Intensity.LIGHT, "그냥 먹고 싶어요", "후회돼요", 0, true, false},
                {TargetApp.MUSINSA, "머리끈 세트 (예시)", 4_500L, Outcome.REASON_PAY, Intensity.LIGHT, "선물이에요", "만족해요", 0, false, false},
                {TargetApp.BAEMIN, "회·초밥 세트 (예시)", 45_000L, Outcome.PREVENTED, Intensity.STRONG, null, null, 2, true, false},
                {TargetApp.COUPANG, "운동화 (예시)", 69_000L, Outcome.DELIBERATE_PAY, Intensity.STRONG, null, "만족해요", 0, false, false},
                {TargetApp.BAEMIN, "버거 세트 (예시)", 12_000L, Outcome.REASON_PAY, Intensity.LIGHT, "약속이 있어요", null, 0, false, false},
                {TargetApp.COUPANG, "식료품 (예시)", 18_000L, Outcome.PASS, null, null, null, 0, false, true},
        };
        for (int i = 0; i < samples.length; i++) {
            Object[] s = samples[i];
            Record r = new Record();
            r.ts = start + (now - start) * (i + 1) / (samples.length + 1);
            r.app = (TargetApp) s[0];
            r.title = (String) s[1];
            r.amount = (Long) s[2];
            r.outcome = (Outcome) s[3];
            r.intensity = (Intensity) s[4];
            r.chip = (String) s[5];
            r.review = (String) s[6];
            r.stoppedAt = (Integer) s[7];
            r.lateNight = (Boolean) s[8];
            boolean food = (Boolean) s[9];
            if (r.outcome.spend) {
                if (food) r.food = r.amount;
                else r.discretionary = r.amount;
            }
            store.addRecord(r);
        }
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
