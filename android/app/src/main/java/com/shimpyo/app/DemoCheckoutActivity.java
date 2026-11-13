package com.shimpyo.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.Money;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 데모 결제 화면. 실제 앱 없이도 개입 흐름(판정 → 트리거 → 오버레이 → 기록)을 시연·테스트할 수 있다.
 * 실제 앱 화면 읽기(접근성 트리)는 건너뛰고, 같은 엔진·같은 오버레이를 쓴다.
 */
public class DemoCheckoutActivity extends Activity {

    static final class Preset {
        final TargetApp app;
        final String label;
        final long amount;
        final List<String> items;
        final String hint;

        Preset(TargetApp app, String label, long amount, String hint, String... items) {
            this.app = app;
            this.label = label;
            this.amount = amount;
            this.hint = hint;
            this.items = Arrays.asList(items);
        }
    }

    static final Preset[] PRESETS = {
            new Preset(TargetApp.BAEMIN, "배민 치킨 23,000", 23_000, "설정 ①② 후 → 재정위험 · 보통", "후라이드 치킨 세트"),
            new Preset(TargetApp.BAEMIN, "배민 떡볶이 12,000", 12_000, "설정 ③④ 후 → 야식 반복 · 가벼움 (②도 하면 보통)", "떡볶이 세트"),
            new Preset(TargetApp.BAEMIN, "배민 회·초밥 45,000", 45_000, "설정 ①③④ 후 → 재정위험+고액+야식 · 강함", "모둠 회·초밥 세트"),
            new Preset(TargetApp.COUPANG, "쿠팡 휴지·세제 26,000", 26_000, "설정 ①② 후 → 필요 판정 · 한 단계 낮춰 가벼움", "3겹 데코 화장지 30롤", "액체 세제 3L"),
            new Preset(TargetApp.COUPANG, "쿠팡 계란·우유 18,000", 18_000, "식료품 + 필요 → 식비 예산 차감, 개입 없음", "계란 30구", "우유 900ml", "두부 300g"),
            new Preset(TargetApp.MUSINSA, "무신사 코트 129,000", 129_000, "설정 ① 후 → 재정위험+고액 · 강함 (질문 3개)", "오버핏 울 코트 (차콜)"),
            new Preset(TargetApp.MUSINSA, "무신사 머리끈 4,500", 4_500, "1시간 안에 3번 결제하면 → 몰아치기 · 가벼움", "머리끈 세트"),
    };

    private LinearLayout content;
    private Preset current;
    private String status = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        content = Ui.v(this);
        Ui.pad(content, this, 20, 20, 20, 28);
        scroll.addView(content);
        setContentView(scroll);
        render();
    }

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    private void add(View v, int gap) {
        content.addView(v, Ui.fillW());
        if (gap > 0) Ui.gapTop(v, this, gap);
    }

    private int appColor(TargetApp a) {
        if (a == TargetApp.BAEMIN) return Color.parseColor("#2AC1BC");
        if (a == TargetApp.COUPANG) return Color.parseColor("#346AFF");
        return Color.BLACK;
    }

    private void render() {
        content.removeAllViews();
        add(Ui.text(this, "데모 결제 화면", 22, Ui.DARK, true), 0);
        add(Ui.text(this, "실제 앱 대신 이 화면에서 결제 시도를 흉내 내요. 판정·트리거·질문·기록은 실제와 같은 엔진을 써요."
                + (GuardService.instance == null ? "\n※ 접근성 서비스가 꺼져 있으면 오버레이가 뜨지 않아요." : ""), 13, Ui.MUTED, false), 6);

        List<TextView> chips = new ArrayList<>();
        for (Preset p : PRESETS) chips.add(Ui.chip(this, p.label, p == current, v -> select(p)));
        add(Ui.chipGrid(this, chips, 2), 14);

        if (current == null) {
            add(Ui.text(this, "위에서 결제할 상품을 고르면, 결제 준비 화면에 들어온 것으로 보고 쉼표가 판단해요.", 13, Ui.MUTED, false), 16);
            return;
        }
        add(Ui.text(this, current.hint, 12, Ui.BLUE, true), 12);

        LinearLayout sheet = Ui.card(this);
        String title = current.app == TargetApp.BAEMIN ? "주문하기" : current.app == TargetApp.COUPANG ? "주문/결제" : "주문서";
        sheet.addView(Ui.text(this, current.app.label + " · " + title, 17, Ui.DARK, true));
        for (String it : current.items) {
            TextView t = Ui.text(this, it, 14, Ui.DARK, false);
            sheet.addView(t);
            Ui.gapTop(t, this, 8);
        }
        View d = Ui.divider(this);
        sheet.addView(d);
        Ui.gapTop(d, this, 12);
        View kv = Ui.kv(this, "총 결제금액", Money.won(current.amount), true);
        sheet.addView(kv);
        Ui.gapTop(kv, this, 10);
        add(sheet, 12);

        TextView pay = Ui.text(this, Money.won(current.amount) + " 결제하기", 16, Color.WHITE, true);
        pay.setGravity(Gravity.CENTER);
        pay.setBackground(Ui.round(this, appColor(current.app), 10));
        Ui.pad(pay, this, 16, 16, 16, 16);
        pay.setOnClickListener(v -> payClick());
        add(pay, 14);

        if (!status.isEmpty()) add(Ui.text(this, status, 13, Ui.DARK, true), 12);
        add(Ui.linkButton(this, "같은 상품으로 다시 시도 (새 결제 화면)", v -> select(current)), 8);
    }

    private void select(Preset p) {
        current = p;
        status = "";
        GuardService g = GuardService.instance;
        if (g == null) {
            status = "접근성 서비스가 꺼져 있어 판단할 수 없어요.";
        } else {
            g.demoCheckout(p.app);
            status = "결제 화면에 들어왔어요. 결제하기를 누르면 그때 쉼표가 최종 금액으로 판단해요.";
        }
        render();
    }

    private void payClick() {
        GuardService g = GuardService.instance;
        if (g == null || current == null) {
            Toast.makeText(this, "접근성 서비스를 켜 주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        String screen = current.app.label + "\n" + String.join("\n", current.items) + "\n총 결제금액\n" + Money.won(current.amount) + "\n결제하기";
        if (g.demoPayClick(current.app, current.amount, current.items, screen)) {
            status = "결제 완료 (데모) · " + Money.won(current.amount) + " 기록됨";
            render();
            return;
        }
        status = "결제하기를 눌렀어요 · 쉼표가 확인 중…";
        render();
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
            else if ("ACTIVE".equals(st)) status = "쉼표 질문 중…";
            else status = "세션 상태: " + st;
            render();
        }, 400);
    }

    @Override
    protected void onPause() {
        super.onPause();
        GuardService g = GuardService.instance;
        if (g != null && current != null && isFinishing()) g.demoLeave(current.app);
    }
}
