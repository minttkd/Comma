package com.shimpyo.app;

import android.app.Activity;
import android.widget.FrameLayout;

import com.shimpyo.app.core.BudgetEngine;
import com.shimpyo.app.core.Enums.Decision;
import com.shimpyo.app.core.Enums.Intensity;
import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.Enums.Trigger;
import com.shimpyo.app.core.QuestionTemplates;
import com.shimpyo.app.core.Session;
import com.shimpyo.app.core.TriggerEngine;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.time.ZoneId;

/**
 * UI 갤러리 (개발용): Ui.java의 토큰과 컴포넌트를 이름표와 함께 한 화면에 늘어놓는다.
 * 설정 탭 › 테스트 도구 › "UI 갤러리 열기"로 연다.
 *
 * 토큰 목록(색·Font·Space·Pad·Radius·Size)은 Ui.java에서 자동으로 읽으므로, 토큰을 추가하면 여기에도 바로 나온다.
 * 컴포넌트를 새로 만들면 components() 아래에 예시를 하나 추가해 두면 좋다.
 */
public class UiGalleryActivity extends Activity {

    private LinearLayout content;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        MainActivity.styleWindow(this);
        LinearLayout root = Ui.v(this);
        root.setBackgroundColor(Ui.BG);

        LinearLayout bar = Ui.h(this);
        Ui.pad(bar, this, Ui.Pad.SCREEN_H, Ui.Pad.SCREEN_TOP);
        bar.addView(Ui.backButton(this, v -> finish()), Ui.square(this, Ui.Size.ICON_BUTTON));
        Ui.inline(bar, Ui.title(this, "UI 갤러리"), Ui.Space.BLOCK);
        root.addView(bar, Ui.fillW());

        ScrollView scroll = new ScrollView(this);
        content = Ui.v(this);
        Ui.pad(content, this, Ui.Pad.SCREEN_H, 0, Ui.Pad.SCREEN_H, Ui.Pad.SCREEN_BOTTOM);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        UiPrefs.load(this);
        content.addView(Ui.bodySmall(this, "Ui.java의 토큰과 부품 모음이에요. 회색 글자가 코드에서 쓰는 이름이에요. 값을 고치고 다시 실행하면 여기서 바로 확인할 수 있어요."));
        uiSettings();
        chatPreview();
        tokens();
        components();
    }

    // ================================================================== 앱 UI 설정 (UiPrefs)

    /** 번호 칩으로 UI 방식을 고른다. 고르면 바로 저장되고, 홈으로 돌아가면 반영된다. */
    private void uiSettings() {
        section("앱 UI 설정");
        Ui.stack(content, Ui.meta(this, "①이 기본 디자인이고 나머지는 비교용 후보예요. 고르면 바로 저장돼요. 홈으로 돌아가면 반영돼요. 기록 색은 설정 › 테스트 도구의 \"결과별 예시 기록\"을 넣고 보면 잘 보여요."), Ui.Space.TIGHT);
        LinearLayout c = Ui.card(this);

        c.addView(Ui.caption(this, "홈 · 오늘 쓸 수 있는 돈 카드"));
        List<TextView> today = new ArrayList<>();
        for (UiPrefs.TodayCard t : UiPrefs.TodayCard.values()) {
            today.add(Ui.chip(this, t.label, UiPrefs.todayCard == t, v -> apply(() -> UiPrefs.todayCard = t)));
        }
        Ui.stack(c, Ui.chipGrid(this, today, 3), Ui.Space.ITEM);

        Ui.stack(c, Ui.caption(this, "홈 · 남은 여윳돈 · 식비 예산 진행 막대"), Ui.Space.SECTION);
        Ui.stack(c, onOff(UiPrefs.tileBars, on -> UiPrefs.tileBars = on), Ui.Space.ITEM);

        Ui.stack(c, Ui.caption(this, "홈 · 기록 · 카드 회색 테두리"), Ui.Space.SECTION);
        Ui.stack(c, onOff(UiPrefs.cardOutline, on -> UiPrefs.cardOutline = on), Ui.Space.ITEM);

        Ui.stack(c, Ui.caption(this, "홈 최근 기록 · 기록 탭 · 결과 색 표시"), Ui.Space.SECTION);
        List<TextView> rec = new ArrayList<>();
        for (UiPrefs.RecordColor r : UiPrefs.RecordColor.values()) {
            rec.add(Ui.chip(this, r.label, UiPrefs.recordColor == r, v -> apply(() -> UiPrefs.recordColor = r)));
        }
        Ui.stack(c, Ui.chipGrid(this, rec, 2), Ui.Space.ITEM);

        Ui.stack(c, Ui.caption(this, "개입 채팅 (결제 화면 위 대화) · 아래 미리보기"), Ui.Space.SECTION);
        List<TextView> chat = new ArrayList<>();
        for (UiPrefs.ChatStyle t : UiPrefs.ChatStyle.values()) {
            chat.add(Ui.chip(this, t.label, UiPrefs.chatStyle == t, v -> apply(() -> UiPrefs.chatStyle = t)));
        }
        Ui.stack(c, Ui.chipGrid(this, chat, 3), Ui.Space.ITEM);

        Ui.stack(c, Ui.caption(this, "리포트 내용 · [리포트에서 보기]"), Ui.Space.SECTION);
        List<TextView> rep = new ArrayList<>();
        for (UiPrefs.ReportStyle t : UiPrefs.ReportStyle.values()) {
            rep.add(Ui.chip(this, t.label, UiPrefs.reportStyle == t, v -> apply(() -> UiPrefs.reportStyle = t)));
        }
        Ui.stack(c, Ui.chipGrid(this, rep, 2), Ui.Space.ITEM);
        Ui.stack(c, Ui.micro(this, "리포트는 설정 › 테스트 도구 › \"리포트 예시 기록 12건 추가\"를 넣고 보면 잘 보여요.", Ui.MUTED), Ui.Space.TIGHT);

        Ui.stack(c, Ui.buttonPair(this,
                Ui.button(this, "기본값으로", false, v -> {
                    UiPrefs.reset(this);
                    recreate();
                }),
                Ui.button(this, "홈에서 보기", true, v -> {
                    setResult(RESULT_OK); // MainActivity가 설정 대신 홈을 연다
                    finish();
                })), Ui.Space.SECTION);
        Ui.stack(c, Ui.button(this, "리포트에서 보기", false, v -> {
            setResult(RESULT_REPORT);
            finish();
        }), Ui.Space.ITEM);
        Ui.stack(content, c, Ui.Space.ITEM);
    }

    /** [리포트에서 보기] → MainActivity가 리포트를 연다. */
    static final int RESULT_REPORT = RESULT_FIRST_USER;

    // ================================================================== 개입 화면 미리보기

    /** 지금 고른 개입 채팅 방식으로 보통(대화 중) · 가벼움(이유 칩) 화면을 그린다. 기록은 남지 않는다. */
    private void chatPreview() {
        section("개입 화면 미리보기 (" + UiPrefs.chatStyle.label + ")");
        Ui.stack(content, Ui.meta(this, "실제 결제 화면 위에 뜨는 그대로예요. 칩을 누르면 다음 질문으로 넘어가요 (기록은 남지 않아요)."), Ui.Space.TIGHT);
        sample("보통 · 배민 치킨 23,000원, 이번 주 세 번째 야식 (두 번째 질문)", previewOverlay(Intensity.MEDIUM));
        sample("가벼움 · 배민 야식 (이유 칩)", previewOverlay(Intensity.LIGHT));
    }

    private View previewOverlay(Intensity in) {
        long now = System.currentTimeMillis();
        Store store = Store.get(this);
        com.shimpyo.app.core.PaymentAttempt a = new com.shimpyo.app.core.PaymentAttempt();
        a.app = TargetApp.BAEMIN;
        a.amount = 23_000;
        a.title = "후라이드 치킨 세트";
        a.ts = now;
        TriggerEngine.Evaluation e = new TriggerEngine.Evaluation();
        e.budget = BudgetEngine.compute(store.settings(), store.records(), now, ZoneId.systemDefault());
        e.intensity = in;
        e.base = in;
        e.discretionary = a.amount;
        e.remainingAfter = e.budget.remainingNow - a.amount;
        e.dailyAfter = e.budget.daysLeft > 0 ? Math.max(0, e.remainingAfter) / e.budget.daysLeft : 0;
        // 야식 반복으로 걸린 경우: 예산 상태와 상관없이 같은 질문이 나와 비교하기 좋다
        e.triggers.add(Trigger.LATE_NIGHT);
        e.lateHour = true;
        e.lateCountNow = in == Intensity.LIGHT ? 2 : 3;
        Session s = new Session("gallery-" + in, now);
        s.attempt = a;
        s.eval = e;
        s.state = Session.State.ACTIVE;
        if (in != Intensity.LIGHT) {
            s.questions.add(QuestionTemplates.build(e, a, 0, null));
            s.answer("그냥 당겨요", Decision.NONE);
            s.questions.add(QuestionTemplates.build(e, a, 1, null));
        }
        OverlayController[] ref = new OverlayController[1];
        ref[0] = new OverlayController(this, new OverlayController.Callbacks() {
            public void onStop(Session x) { toast("미리보기: 그만둘게요"); }
            public void onReason(Session x, String chip) { toast("미리보기: 이유 '" + chip + "'"); }
            public void onBuy(Session x) { toast("미리보기: 그래도 살게요"); }
            public void onClose(Session x) { toast("미리보기: 닫기"); }
            public void requestNext(Session x) {
                x.questions.add(QuestionTemplates.build(x.eval, x.attempt, x.index, null));
                x.loading = false;
                ref[0].render();
            }
        });
        FrameLayout root = ref[0].preview(s);
        root.setBackground(Ui.round(this, Ui.DIM, Ui.Radius.SHEET)); // 결제 화면을 덮는 어두운 바탕
        Ui.pad(root, this, 0, Ui.Pad.SHEET_TOP, 0, 0);
        return root;
    }

    private interface BoolSetter {
        void set(boolean on);
    }

    /** [켜기] [끄기] 칩 한 줄. */
    private View onOff(boolean current, BoolSetter setter) {
        List<TextView> chips = new ArrayList<>();
        chips.add(Ui.chip(this, "켜기", current, v -> apply(() -> setter.set(true))));
        chips.add(Ui.chip(this, "끄기", !current, v -> apply(() -> setter.set(false))));
        return Ui.chipGrid(this, chips, 2);
    }

    /** 값을 바꾸고 저장한 뒤 갤러리를 다시 그린다 (아래 예시들도 새 설정으로). */
    private void apply(Runnable change) {
        change.run();
        UiPrefs.save(this);
        recreate();
    }

    // ================================================================== 공통

    private void section(String title) {
        Ui.stack(content, Ui.sectionTitle(this, title), Ui.Space.LARGE);
    }

    /** 이름표(코드 이름) + 예시 한 벌. */
    private void sample(String name, View view) {
        LinearLayout box = Ui.v(this);
        box.addView(Ui.text(this, name, Ui.Font.CAPTION, Ui.SUBTLE, Ui.MEDIUM));
        Ui.stack(box, view, Ui.Space.TIGHT);
        Ui.stack(content, box, Ui.Space.STACK);
    }

    /** 흰 카드 안에 이름표 + 예시를 놓는다 (흰 바탕 위에서 보는 부품용). */
    private void sampleOnCard(String name, View view) {
        LinearLayout c = Ui.card(this);
        c.addView(view, Ui.fillW());
        sample(name, c);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    /** cls 안의 static final 숫자 필드를 선언 순서대로 (이름, 값) 목록으로. */
    private static List<Object[]> numberFields(Class<?> cls, List<String> skip) {
        List<Object[]> out = new ArrayList<>();
        for (Field f : cls.getDeclaredFields()) {
            int m = f.getModifiers();
            if (!Modifier.isStatic(m) || !Modifier.isFinal(m) || skip.contains(f.getName())) continue;
            Class<?> t = f.getType();
            if (t != int.class && t != float.class) continue;
            try {
                f.setAccessible(true);
                out.add(new Object[]{f.getName(), ((Number) f.get(null)).floatValue()});
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    /** 값 작은 순으로 정렬 (desc=true면 큰 순). */
    private static List<Object[]> byValue(List<Object[]> l, boolean desc) {
        l.sort((a, b) -> desc ? Float.compare((Float) b[1], (Float) a[1]) : Float.compare((Float) a[1], (Float) b[1]));
        return l;
    }

    /** 이름 순으로 정렬 (같은 접두어끼리 모인다: BOX_H, BOX_V …). */
    private static List<Object[]> byName(List<Object[]> l) {
        l.sort((a, b) -> ((String) a[0]).compareTo((String) b[0]));
        return l;
    }

    private static String num(float v) {
        return v == Math.round(v) ? String.valueOf(Math.round(v)) : String.valueOf(v);
    }

    // ================================================================== 토큰

    private void tokens() {
        section("색 · Ui.색이름");
        LinearLayout colors = Ui.card(this);
        boolean first = true;
        for (Object[] f : byName(numberFields(Ui.class, Arrays.asList("REGULAR", "MEDIUM", "BOLD")))) {
            int color = colorValue((String) f[0]);
            LinearLayout r = Ui.h(this);
            View sw = new View(this);
            sw.setBackground(Ui.roundStroke(this, color, Ui.Radius.CHECK, Ui.LINE));
            r.addView(sw, Ui.square(this, Ui.Size.SWATCH));
            Ui.inlineFill(r, Ui.text(this, "Ui." + f[0], Ui.Font.BODY, Ui.TEXT, Ui.MEDIUM), Ui.Space.BLOCK);
            r.addView(Ui.meta(this, String.format(Locale.ROOT, "#%08X", color)));
            Ui.stack(colors, r, first ? 0 : Ui.Space.ITEM);
            first = false;
        }
        Ui.stack(content, colors, Ui.Space.ITEM);

        section("상태 색 묶음 · Ui.Tone");
        FlowLayout tones = Ui.tagFlow(this);
        for (Ui.Tone t : Ui.Tone.values()) tones.addView(Ui.tag(this, "Tone." + t.name(), t));
        Ui.stack(content, tones, Ui.Space.ITEM);

        section("글자 크기 · Ui.Font");
        LinearLayout fonts = Ui.card(this);
        first = true;
        for (Object[] f : byValue(numberFields(Ui.Font.class, new ArrayList<>()), true)) {
            float sp = (Float) f[1];
            TextView t = Ui.text(this, "Font." + f[0] + " · " + num(sp) + "/" + Ui.lineHeight(sp), sp, Ui.TEXT, Ui.BOLD);
            Ui.stack(fonts, t, first ? 0 : Ui.Space.ITEM);
            first = false;
        }
        Ui.stack(content, fonts, Ui.Space.ITEM);
        LinearLayout weights = Ui.card(this);
        weights.addView(Ui.text(this, "Ui.REGULAR 400", Ui.Font.BODY, Ui.TEXT, Ui.REGULAR));
        weights.addView(Ui.text(this, "Ui.MEDIUM 500", Ui.Font.BODY, Ui.TEXT, Ui.MEDIUM));
        weights.addView(Ui.text(this, "Ui.BOLD 700", Ui.Font.BODY, Ui.TEXT, Ui.BOLD));
        Ui.stack(content, weights, Ui.Space.ITEM);

        section("간격 (사이) · Ui.Space  — 막대는 실제의 4배");
        Ui.stack(content, sizeBars(byValue(numberFields(Ui.Space.class, new ArrayList<>()), false)), Ui.Space.ITEM);

        section("여백 (안쪽) · Ui.Pad  — 막대는 실제의 4배");
        Ui.stack(content, sizeBars(byName(numberFields(Ui.Pad.class, new ArrayList<>()))), Ui.Space.ITEM);

        section("모서리 · Ui.Radius");
        FlowLayout radii = Ui.tagFlow(this);
        for (Object[] f : byValue(numberFields(Ui.Radius.class, new ArrayList<>()), false)) {
            LinearLayout cell = Ui.v(this);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            View box = new View(this);
            box.setBackground(Ui.round(this, Ui.BRAND_LIGHT, (Float) f[1]));
            cell.addView(box, Ui.square(this, Ui.Size.RADIUS_SAMPLE));
            Ui.stack(cell, Ui.meta(this, f[0] + "\n" + num((Float) f[1])), Ui.Space.LINE);
            radii.addView(cell);
        }
        Ui.stack(content, radii, Ui.Space.ITEM);

        section("고정 크기 · Ui.Size");
        LinearLayout sizes = Ui.card(this);
        first = true;
        for (Object[] f : byName(numberFields(Ui.Size.class, new ArrayList<>()))) {
            Ui.stack(sizes, Ui.kv(this, "Size." + f[0], num((Float) f[1]) + "dp", false), first ? 0 : Ui.Space.LINE);
            first = false;
        }
        Ui.stack(content, sizes, Ui.Space.ITEM);
    }

    /** Ui의 색 상수 값을 이름으로 읽는다 (int 그대로). */
    private static int colorValue(String name) {
        try {
            Field f = Ui.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(null);
        } catch (Exception e) {
            return 0;
        }
    }

    /** 간격 막대는 비교하기 쉽게 실제 값의 4배 길이로 그린다. */
    private static final int BAR_SCALE = 4;

    /** 토큰 값만큼 긴 막대(×BAR_SCALE) + 이름 · 값. */
    private View sizeBars(List<Object[]> fields) {
        LinearLayout c = Ui.card(this);
        boolean first = true;
        for (Object[] f : fields) {
            LinearLayout r = Ui.h(this);
            TextView label = Ui.text(this, f[0] + " " + num((Float) f[1]), Ui.Font.CAPTION, Ui.TEXT, Ui.MEDIUM);
            r.addView(label, new LinearLayout.LayoutParams(Ui.dp(this, Ui.Size.GALLERY_LABEL), ViewGroup.LayoutParams.WRAP_CONTENT));
            View bar = new View(this);
            bar.setBackground(Ui.round(this, Ui.BRAND, Ui.Radius.BAR / 2f));
            Ui.inline(r, bar, Ui.Space.ITEM);
            bar.getLayoutParams().width = Ui.dp(this, (Float) f[1] * BAR_SCALE);
            bar.getLayoutParams().height = Ui.dp(this, Ui.Size.BAR);
            Ui.stack(c, r, first ? 0 : Ui.Space.TIGHT);
            first = false;
        }
        return c;
    }

    // ================================================================== 컴포넌트

    private void components() {
        View.OnClickListener tap = v -> toast("눌렀어요");

        section("글자");
        LinearLayout texts = Ui.card(this);
        texts.addView(Ui.title(this, "Ui.title · 화면 제목"));
        Ui.stack(texts, Ui.heading(this, "Ui.heading · 시트 제목"), Ui.Space.ITEM);
        Ui.stack(texts, Ui.sectionTitle(this, "Ui.sectionTitle · 섹션 머리말"), Ui.Space.ITEM);
        Ui.stack(texts, Ui.body(this, "Ui.body · 본문이에요."), Ui.Space.ITEM);
        Ui.stack(texts, Ui.bodySmall(this, "Ui.bodySmall · 짧은 안내예요."), Ui.Space.ITEM);
        Ui.stack(texts, Ui.caption(this, "Ui.caption · 카드 머리말"), Ui.Space.ITEM);
        Ui.stack(texts, Ui.meta(this, "Ui.meta · 오늘 12:30 · 보통"), Ui.Space.ITEM);
        Ui.stack(texts, Ui.micro(this, "Ui.micro · 아주 작은 설명", Ui.MUTED), Ui.Space.ITEM);
        Ui.stack(content, texts, Ui.Space.ITEM);

        section("버튼");
        sample("Ui.button(primary = true)", Ui.button(this, "그만둘게요", true, tap));
        sample("Ui.button(primary = false)", Ui.button(this, "다음", false, tap));
        TextView disabled = Ui.button(this, "다음", true, tap);
        Ui.setEnabled(disabled, false);
        sample("Ui.setEnabled(button, false)", disabled);
        sample("Ui.buttonPair", Ui.buttonPair(this, Ui.button(this, "만족해요", false, tap), Ui.button(this, "후회돼요", false, tap)));
        sample("Ui.linkButton", Ui.centered(this, Ui.linkButton(this, "그래도 살게요", tap)));
        sample("Ui.textLink", Ui.textLink(this, "접근성 설정 열기 ›", Ui.BRAND, tap));
        LinearLayout icons = Ui.h(this);
        icons.addView(Ui.backButton(this, tap), Ui.square(this, Ui.Size.ICON_BUTTON));
        Ui.inline(icons, Ui.closeButton(this, tap), Ui.Space.BLOCK);
        Ui.inline(icons, Ui.settingsButton(this, tap), Ui.Space.BLOCK, Ui.Size.ICON_BUTTON + 2 * Ui.Pad.ICON);
        sample("Ui.backButton · Ui.closeButton · Ui.settingsButton", icons);
        sample("Ui.screenHeader (기록 · 리포트 · 설정 윗줄)", Ui.screenHeader(this, "기록", tap));
        sample("Ui.sectionHeader (홈 최근 기록)", Ui.sectionHeader(this, "최근 기록", "더보기 ›", tap));

        section("칩 · 태그");
        List<TextView> chips = new ArrayList<>();
        chips.add(Ui.chip(this, "배고파서", false, tap));
        chips.add(Ui.chip(this, "선택된 칩", true, tap));
        chips.add(Ui.chip(this, "그냥 사고 싶어요", false, tap));
        sample("Ui.chip(selected) · Ui.chipFlow", Ui.chipFlow(this, chips));
        List<TextView> grid = new ArrayList<>();
        grid.add(Ui.chip(this, "켜짐", true, tap));
        grid.add(Ui.chip(this, "꺼짐", false, tap));
        grid.add(Ui.chip(this, "자동", false, tap));
        sample("Ui.chipGrid(perRow = 2)", Ui.chipGrid(this, grid, 2));
        FlowLayout smallTags = Ui.tagFlow(this);
        for (Ui.Tone t : Ui.Tone.values()) smallTags.addView(Ui.smallTag(this, t.name(), t));
        sample("Ui.smallTag(label, Tone)", smallTags);

        section("컨테이너");
        LinearLayout card = Ui.card(this);
        card.addView(Ui.caption(this, "머리말"));
        Ui.stack(card, Ui.body(this, "흰 카드예요."), Ui.Space.LINE);
        sample("Ui.card", card);
        LinearLayout outline = Ui.outlineCard(this, Ui.Pad.CARD, Ui.Pad.CARD);
        outline.addView(Ui.body(this, "테두리 카드예요."));
        sample("Ui.outlineCard(Pad.CARD, Pad.CARD)", outline);
        LinearLayout tint = Ui.tintCard(this, Ui.BRAND_LIGHT);
        tint.addView(Ui.body(this, "색 바탕 카드예요."));
        sample("Ui.tintCard(BRAND_LIGHT)", tint);
        LinearLayout box = Ui.tintBox(this, Ui.BG, Ui.Pad.ANSWER_BOX, Ui.Pad.ANSWER_BOX);
        box.addView(Ui.body(this, "색 바탕 상자예요."));
        sampleOnCard("Ui.tintBox(BG, …)", box);
        LinearLayout sheet = Ui.bottomSheet(this, Ui.Pad.SHEET_TOP);
        sheet.addView(Ui.heading(this, "바텀시트예요"));
        Ui.stack(sheet, Ui.meta(this, "위 모서리만 둥글어요."), Ui.Space.ITEM);
        LinearLayout dim = Ui.v(this);
        dim.setBackgroundColor(Ui.DIM);
        Ui.pad(dim, this, 0, Ui.Pad.SHEET_TOP, 0, 0);
        dim.addView(sheet, Ui.fillW());
        sample("Ui.bottomSheet(Pad.SHEET_TOP) · Ui.DIM", dim);

        section("정보 표시");
        LinearLayout kvs = Ui.card(this);
        kvs.addView(Ui.kv(this, "월세·관리비", "350,000원", false));
        Ui.stack(kvs, Ui.kv(this, "합계", "500,000원", true), Ui.Space.ROW);
        sample("Ui.kv(total = false / true)", kvs);
        LinearLayout list = Ui.outlineCard(this, Ui.Pad.CARD, Ui.Pad.CARD);
        list.addView(Ui.listRow(this, Ui.GREEN, "쿠팡 · 휴지·세제", "어제 21:05", "26,000원", Ui.smallTag(this, "멈춤", Ui.Tone.SUCCESS)));
        Ui.stack(list, Ui.listRow(this, Ui.BRAND, "배민 · 치킨 세트", "오늘 19:20 · 보통 · \"배고파서\"", "23,000원", Ui.smallTag(this, "이유 선택 결제", Ui.Tone.ACCENT)), Ui.Space.BLOCK);
        Ui.stack(list, Ui.listRow(this, Ui.AMBER, "무신사 · 울 코트", "어제 23:10 · 강함", "129,000원", Ui.smallTag(this, "숙고 후 결제", Ui.Tone.WARNING)), Ui.Space.BLOCK);
        Ui.stack(list, Ui.listRow(this, Ui.LINE, "외식 · 학교 앞 식당", "오늘 12:30", "8,000원", Ui.smallTag(this, "여윳돈", Ui.Tone.NEUTRAL)), Ui.Space.BLOCK);
        sample("Ui.listRow(dotColor, …)  — 점 없는 버전은 dotColor 없이", list);
        sample("Ui.statTile(outlined = true, ratio, barColor, barTrack) · Ui.tilePair", Ui.tilePair(this,
                Ui.statTile(this, "남은 여윳돈", "280,000원", "급여일까지 8일", true, 0.7f, Ui.GREEN, Ui.BG),
                Ui.statTile(this, "식비 예산", "0원", "10만 원 중 남음", true, 0f, Ui.RED, Ui.RED_BG), Ui.Space.BLOCK));
        sample("Ui.statTile(outlined = true)  — 막대 없는 버전", Ui.tilePair(this,
                Ui.statTile(this, "남은 여윳돈", "280,000원", "급여일까지 8일", true),
                Ui.statTile(this, "식비 예산", "64,000원", "10만 원 중 남음", true), Ui.Space.BLOCK));
        sample("Ui.statTile(outlined = false)", Ui.tilePair(this,
                Ui.statTile(this, "야식", "3번", "39,000원 · 지난달 −1번", false),
                Ui.statTile(this, "회고 후회", "1/3", "후회 응답 33%", false), Ui.Space.ROW));
        sample("Ui.heroCard(large = false)", Ui.heroCard(this, "월 여윳돈", "400,000원", "수입 100만 − 고정 50만 − 식비 10만", false));
        sample("Ui.heroCard(large = true)", Ui.heroCard(this, "마음 돌림 비율", "67%", "쉼표가 말을 건 3번 중 2번 그만뒀어요", true));
        sample("Ui.alertBox", Ui.alertBox(this, "결제 감지가 꺼져 있어요", "설정 › 접근성에서 '쉼표'를 켜 주세요.", "접근성 설정 열기 ›", tap));
        sample("Ui.highlightBox", Ui.highlightBox(this, "이번 달 쉼표", "2번 멈춰서 52,000원 아꼈어요"));
        sample("Ui.highlightBox(..., click) · 누르면 이동 (홈 → 리포트)", Ui.highlightBox(this, "이번 달 쉼표", "2번 멈춰서 52,000원 아꼈어요", tap));
        sample("Ui.tintNote(…)", Ui.tintNote(this, "비밀번호·카드번호·메시지는 읽지 않아요.", Ui.Font.CAPTION, Ui.GREEN, Ui.Radius.BOX, Ui.Pad.PRIVACY_NOTE, Ui.Pad.PRIVACY_NOTE));
        sampleOnCard("Ui.factBox", Ui.factBox(this, "23,000원을 결제하면 오늘 31,000원 · 오늘 한도 14,000원"));
        LinearLayout bars = Ui.card(this);
        bars.addView(Ui.bar(this, 0.4f, Ui.BRAND, Ui.BG), Ui.fillW());
        Ui.stack(bars, Ui.bar(this, 1f, Ui.RED, Ui.BG), Ui.Space.ITEM);
        sample("Ui.bar(ratio, fill, track)", bars);
        LinearLayout div = Ui.card(this);
        div.addView(Ui.body(this, "위"));
        View d = Ui.divider(this);
        div.addView(d);
        Ui.gapTop(d, this, Ui.Space.ITEM);
        Ui.stack(div, Ui.body(this, "아래"), Ui.Space.ITEM);
        sample("Ui.divider", div);

        section("대화 (개입 오버레이)");
        LinearLayout conv = Ui.card(this);
        conv.addView(Ui.bubbleAi(this, "지난번 치킨은 만족스러웠나요?", true), Ui.fillW());
        Ui.stack(conv, Ui.bubbleUser(this, "그냥 그랬어요"), Ui.Space.STACK);
        Ui.stack(conv, Ui.bubbleAi(this, "그럼 오늘은 내일로 미뤄 볼까요?", false), Ui.Space.STACK);
        sample("Ui.bubbleAi(faded) · Ui.bubbleUser", conv);

        section("내비게이션 · 온보딩");
        sample("Ui.bottomNav (지금 앱에서는 안 씀)", Ui.bottomNav(this, new String[]{"홈", "기록", "리포트", "설정"}, 0, i -> toast("탭 " + i)));
        LinearLayout steps = Ui.v(this);
        steps.addView(Ui.steps(this, 1, 3, true));
        Ui.stack(steps, Ui.steps(this, 2, 3, true), Ui.Space.ITEM);
        Ui.stack(steps, Ui.steps(this, 3, 3, true), Ui.Space.ITEM);
        sample("Ui.steps(current, total, doneFilled)", steps);
        LinearLayout checks = Ui.card(this);
        checks.addView(Ui.checkRow(this, "체크된 줄", true, tap), Ui.fillW());
        Ui.stack(checks, Ui.checkRow(this, "체크 안 된 줄", false, tap), Ui.Space.ROW);
        sample("Ui.checkRow", checks);

        section("입력");
        sampleOnCard("Ui.input", Ui.input(this, "힌트 글자", null, false));
        LinearLayout plain = Ui.h(this);
        plain.setBackground(Ui.round(this, Ui.BG, Ui.Radius.INPUT));
        plain.addView(Ui.plainInput(this, "직접 입력 (선택)", null), Ui.weight1());
        sampleOnCard("Ui.plainInput (바탕은 감싸는 쪽에서)", plain);
        sampleOnCard("Ui.labeledField", Ui.labeledField(this, "월 수입", Ui.input(this, "", "1000000", true)));
    }
}
