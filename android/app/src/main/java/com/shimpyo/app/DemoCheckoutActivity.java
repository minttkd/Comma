package com.shimpyo.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.Money;

import java.util.Arrays;
import java.util.List;

/**
 * 데모 결제 화면. 실제 앱 없이도 개입 흐름(판정 → 트리거 → 오버레이 → 기록)을 시연·테스트할 수 있다.
 * 실제 앱 화면 읽기(접근성 트리)는 건너뛰고, 같은 엔진·같은 오버레이를 쓴다.
 * 화면은 Figma "Backdrops (실제 앱 주문 화면 재현, 로고 없음)"를 따른다.
 */
public class DemoCheckoutActivity extends Activity {

    static final String EXTRA_PRESET = "preset";

    static final class Preset {
        final TargetApp app;
        final String label;      // 홈 데모 목록에 보이는 이름
        final String expect;     // 예상 강도 (홈 데모 목록 오른쪽)
        final long amount;
        final List<String> items;
        final long[] prices;
        final long fee;          // 배달팁 · 배송비
        final String hint;

        Preset(TargetApp app, String label, String expect, long amount, long fee, String hint, String[] items, long[] prices) {
            this.app = app;
            this.label = label;
            this.expect = expect;
            this.amount = amount;
            this.fee = fee;
            this.hint = hint;
            this.items = Arrays.asList(items);
            this.prices = prices;
        }
    }

    static final Preset[] PRESETS = {
            new Preset(TargetApp.BAEMIN, "배민 · 치킨 세트", "보통", 23_000, 2_000, "설정 ①② 후 → 재정위험 · 보통",
                    new String[]{"후라이드 반 + 양념 반 세트"}, new long[]{21_000}),
            new Preset(TargetApp.COUPANG, "쿠팡 · 휴지·세제", "가벼움", 26_000, 0, "설정 ①② 후 → 필요 판정 · 한 단계 낮춰 가벼움",
                    new String[]{"3겹 롤화장지 30롤", "중성세제 2.5L"}, new long[]{15_900, 10_100}),
            new Preset(TargetApp.MUSINSA, "무신사 · 울 코트", "강함", 129_000, 0, "설정 ① 후 → 재정위험+고액 · 강함 (질문 3개)",
                    new String[]{"울 블렌드 싱글 코트 (차콜, M)"}, new long[]{129_000}),
            new Preset(TargetApp.BAEMIN, "배민 · 떡볶이", "가벼움", 12_000, 2_000, "설정 ③④ 후 → 야식 반복 · 가벼움 (②도 하면 보통)",
                    new String[]{"로제 떡볶이 세트"}, new long[]{10_000}),
            new Preset(TargetApp.BAEMIN, "배민 · 회·초밥", "강함", 45_000, 3_000, "설정 ①③④ 후 → 재정위험+고액+야식 · 강함",
                    new String[]{"모둠 회·초밥 세트"}, new long[]{42_000}),
            new Preset(TargetApp.COUPANG, "쿠팡 · 계란·우유·두부", "개입 없음", 18_000, 0, "식료품 + 필요 → 식비 예산 차감, 개입 없음",
                    new String[]{"계란 30구", "우유 900ml", "두부 300g"}, new long[]{9_900, 4_980, 3_120}),
            new Preset(TargetApp.MUSINSA, "무신사 · 머리끈", "1시간에 3번째부터", 4_500, 0, "1시간 안에 3번 결제하면 → 몰아치기 · 가벼움",
                    new String[]{"머리끈 세트"}, new long[]{4_500}),
    };

    private Preset current;
    private String status = "";
    private LinearLayout content;
    private TextView statusView;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        MainActivity.styleWindow(this);
        int idx = getIntent().getIntExtra(EXTRA_PRESET, 0);
        current = PRESETS[Math.max(0, Math.min(PRESETS.length - 1, idx))];
        setContentView(buildView());
        GuardService g = GuardService.instance;
        if (g != null) g.demoCheckout(current.app);
        else status = "접근성 서비스가 꺼져 있어 판단할 수 없어요.";
        updateStatus();
    }

    // 앱별 재현 색상 (CLAUDE.md UI 규칙의 예외: 실제 앱 결제 화면을 흉내 낸다)
    private static final int BAEMIN_MINT = Color.parseColor("#2AC1BC");
    private static final int COUPANG_BLUE = Color.parseColor("#346AFF");
    private static final int COUPANG_RED = Color.parseColor("#E52528");
    private static final int MUSINSA_BLACK = Color.BLACK;
    private static final int PAY_BUTTON_RADIUS = 10;

    private int payColor() {
        if (current.app == TargetApp.BAEMIN) return BAEMIN_MINT;
        if (current.app == TargetApp.COUPANG) return COUPANG_BLUE;
        return MUSINSA_BLACK;
    }

    /** Figma Backdrop: AppBar · 섹션들 · PayBar. */
    View buildView() {
        LinearLayout root = Ui.v(this);
        root.setBackgroundColor(Ui.BG);

        // AppBar: ← 주문하기
        LinearLayout bar = Ui.h(this);
        bar.setBackgroundColor(Ui.WHITE);
        Ui.pad(bar, this, Ui.Pad.DEMO_SIDE, Ui.Pad.DEMO_APPBAR_V);
        bar.addView(Ui.backButton(this, v -> finish()));
        String title = current.app == TargetApp.BAEMIN ? "주문하기" : current.app == TargetApp.COUPANG ? "주문/결제" : "주문서";
        Ui.inline(bar, Ui.heading(this, title), Ui.Space.BLOCK);
        root.addView(bar, Ui.fillW());

        ScrollView scroll = new ScrollView(this);
        content = Ui.v(this);
        Ui.pad(content, this, 0, Ui.Pad.DEMO_CONTENT_TOP, 0, Ui.Pad.DEMO_CONTENT_BOTTOM);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        if (current.app == TargetApp.BAEMIN) baemin();
        else if (current.app == TargetApp.COUPANG) coupang();
        else musinsa();

        statusView = Ui.caption(this, "");
        Ui.pad(statusView, this, Ui.Pad.DEMO_SIDE, Ui.Space.STACK, Ui.Pad.DEMO_SIDE, 0);
        content.addView(statusView, Ui.fillW());
        TextView hint = Ui.text(this, "데모 · " + current.hint, Ui.Font.CAPTION, Ui.SUBTLE, Ui.REGULAR);
        Ui.pad(hint, this, Ui.Pad.DEMO_SIDE, Ui.Space.TIGHT, Ui.Pad.DEMO_SIDE, 0);
        content.addView(hint, Ui.fillW());
        TextView retry = Ui.linkButton(this, "같은 상품으로 다시 시도 (새 결제 화면)", v -> {
            status = "";
            GuardService g = GuardService.instance;
            if (g != null) g.demoCheckout(current.app);
            updateStatus();
        });
        content.addView(Ui.centered(this, retry), Ui.fillW());

        // PayBar
        LinearLayout payBar = Ui.v(this);
        payBar.setBackgroundColor(Ui.WHITE);
        Ui.pad(payBar, this, Ui.Pad.DEMO_SIDE, Ui.Pad.DEMO_PAYBAR_TOP, Ui.Pad.DEMO_SIDE, Ui.Pad.DEMO_PAYBAR_BOTTOM);
        String payLabel = current.app == TargetApp.COUPANG ? "결제하기" : Money.won(current.amount) + " 결제하기";
        TextView pay = Ui.text(this, payLabel, Ui.Font.BUTTON, Ui.WHITE, Ui.BOLD);
        pay.setGravity(Gravity.CENTER);
        pay.setBackground(Ui.round(this, payColor(), PAY_BUTTON_RADIUS));
        Ui.pad(pay, this, Ui.Pad.DEMO_SIDE, Ui.Pad.DEMO_PAY_V);
        pay.setOnClickListener(v -> payClick());
        payBar.addView(pay, Ui.fillW());
        root.addView(payBar, Ui.fillW());
        updateStatus();
        return root;
    }

    // ------------------------------------------------------------------ 섹션 (흰 바탕, padding 14/16, gap 8)

    private LinearLayout section(String title, String right, int rightColor) {
        LinearLayout s = Ui.v(this);
        s.setBackgroundColor(Ui.WHITE);
        Ui.pad(s, this, Ui.Pad.DEMO_SIDE, Ui.Pad.DEMO_SECTION_V);
        LinearLayout h = Ui.h(this);
        h.setGravity(Gravity.TOP);
        h.addView(Ui.text(this, title, Ui.Font.BODY, Ui.TEXT, Ui.MEDIUM), Ui.weight1());
        if (right != null) h.addView(Ui.text(this, right, Ui.Font.CAPTION, rightColor, Ui.MEDIUM));
        s.addView(h, Ui.fillW());
        Ui.stack(content, s, content.getChildCount() == 0 ? 0 : Ui.Space.ITEM);
        return s;
    }

    private void line(LinearLayout s, String text) {
        Ui.stack(s, Ui.meta(this, text), Ui.Space.ITEM);
    }

    private void row(LinearLayout s, String k, String v) {
        LinearLayout r = Ui.h(this);
        r.addView(Ui.meta(this, k), Ui.weight1());
        r.addView(Ui.text(this, v, Ui.Font.CAPTION, Ui.TEXT, Ui.REGULAR));
        Ui.stack(s, r, Ui.Space.ITEM);
    }

    private void total(LinearLayout s, String k, long v, int color) {
        LinearLayout r = Ui.h(this);
        r.addView(Ui.text(this, k, Ui.Font.BODY, Ui.TEXT, Ui.MEDIUM), Ui.weight1());
        r.addView(Ui.text(this, Money.won(v), Ui.Font.HEADING, color, Ui.BOLD));
        Ui.stack(s, r, Ui.Space.ITEM);
    }

    private long itemsSum() {
        long t = 0;
        for (long p : current.prices) t += p;
        return t;
    }

    private void baemin() {
        LinearLayout d = section("배달", "한집배달 · 25~40분", Ui.MUTED);
        line(d, "서울 동작구 상도로 369, 기숙사 B동");
        line(d, "요청사항: 문 앞에 두고 벨 눌러주세요");
        LinearLayout m = section("주문 메뉴", null, 0);
        for (int i = 0; i < current.items.size(); i++) row(m, current.items.get(i) + " · 1개", Money.won(current.prices[i]));
        if (current.items.get(0).contains("치킨") || current.items.get(0).contains("양념")) row(m, "콜라 1.25L (기본 제공)", "0원");
        LinearLayout p = section("결제수단", "간편결제 ›", Ui.MUTED);
        line(p, "등록된 카드로 바로 결제돼요");
        LinearLayout a = section("결제금액", null, 0);
        row(a, "주문금액", Money.won(itemsSum()));
        row(a, "배달팁", Money.won(current.fee));
        total(a, "총 결제금액", current.amount, Ui.TEXT);
    }

    private void coupang() {
        LinearLayout r = section("받는사람 정보", "변경 ›", Ui.MUTED);
        line(r, "이수민 · 010-****-1234");
        line(r, "서울 동작구 상도로 369, 기숙사 B동");
        LinearLayout s = section("배송 1건", "내일(목) 도착 보장", COUPANG_BLUE);
        for (int i = 0; i < current.items.size(); i++) row(s, current.items.get(i) + " · 1개", Money.won(current.prices[i]));
        LinearLayout p = section("결제정보", null, 0);
        row(p, "총상품가격", Money.won(itemsSum()));
        row(p, "배송비", Money.won(current.fee));
        total(p, "총결제금액", current.amount, COUPANG_RED);
        section("결제수단", "신용/체크카드 ›", Ui.MUTED);
    }

    private void musinsa() {
        LinearLayout d = section("배송 정보", "변경 ›", Ui.MUTED);
        line(d, "이수민 · 010-****-1234");
        line(d, "서울 동작구 상도로 369, 기숙사 B동");
        LinearLayout s = section("주문 상품 " + current.items.size() + "개", null, 0);
        for (int i = 0; i < current.items.size(); i++) row(s, current.items.get(i), Money.won(current.prices[i]));
        section("결제 수단", "간편결제 ›", Ui.MUTED);
        LinearLayout a = section("결제 금액", null, 0);
        row(a, "상품 금액", Money.won(itemsSum()));
        row(a, "배송비", current.fee == 0 ? "무료" : Money.won(current.fee));
        total(a, "최종 결제 금액", current.amount, Ui.TEXT);
    }

    private void updateStatus() {
        if (statusView == null) return;
        statusView.setText(status);
        statusView.setVisibility(status.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ------------------------------------------------------------------ 결제 버튼

    private void payClick() {
        GuardService g = GuardService.instance;
        if (g == null) {
            Toast.makeText(this, "접근성 서비스를 켜 주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        String screen = current.app.label + "\n" + String.join("\n", current.items) + "\n총 결제금액\n" + Money.won(current.amount) + "\n결제하기";
        if (g.demoPayClick(current.app, current.amount, current.items, screen)) {
            status = "결제 완료 (데모) · " + Money.won(current.amount) + " 기록됨";
            updateStatus();
            return;
        }
        status = "최종 금액으로 확인하고 있어요…";
        updateStatus();
        pollAfterTap(0);
    }

    /** 판단이 끝나면 결과를 보여 준다. 개입이 필요 없으면 가로챈 터치를 대신 전달하는 것과 같게 바로 결제 완료로 본다. */
    private void pollAfterTap(int n) {
        handler.postDelayed(() -> {
            GuardService g = GuardService.instance;
            if (g == null || current == null || isFinishing()) return;
            String st = g.sessionState();
            if ("EVALUATING".equals(st) && n < 12) {
                pollAfterTap(n + 1);
                return;
            }
            if ("PASSED".equals(st)) {
                payClick();
                return;
            }
            if ("ALLOWED".equals(st)) status = "질문이 끝났어요. 결제하기를 한 번 더 눌러 주세요.";
            else if ("ACTIVE".equals(st)) status = "숨숨이 질문 중…";
            else if ("PAUSED".equals(st)) status = "질문을 닫았어요. 결제하기를 누르면 하던 질문부터 다시 떠요.";
            else status = "";
            updateStatus();
        }, 400);
    }

    @Override
    protected void onPause() {
        super.onPause();
        GuardService g = GuardService.instance;
        if (g != null && current != null && isFinishing()) g.demoLeave(current.app);
    }
}
