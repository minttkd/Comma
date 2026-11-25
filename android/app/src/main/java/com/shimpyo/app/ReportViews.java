package com.shimpyo.app;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.shimpyo.app.core.BudgetEngine;
import com.shimpyo.app.core.Enums.Judgment;
import com.shimpyo.app.core.Enums.Outcome;
import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.Money;
import com.shimpyo.app.core.Record;
import com.shimpyo.app.core.Settings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 리포트(S10) "가치 회고": 숫자 대신 "내가 무엇에 돈을 쓰고, 다음 날 어떻게 느꼈는지"로 돌아보게 한다.
 *
 * "가벼움·보통·강함 / 결과 분류" 같은 시스템 용어는 쓰지 않고, 사용자가 직접 한 판단만 보여 준다.
 * - 결제할 때 고른 이유 (가벼움 이유 칩) → 가치(사람 · 기분 · 대화 끝에)로 묶는다
 * - 다음 날 돌아본 마음 (S09 회고: 만족해요 / 후회돼요)
 * - 대화에서 직접 적은 말
 * 필요해서 한 결제(생필품 · 끼니, AI가 필요로 판정한 결제)는 이미 판단 기준이 정해진 것이라 회고에서 뺀다.
 *
 * 집계는 화면 전용이라 core/Stats를 건드리지 않고 여기서 기록을 직접 센다.
 */
final class ReportViews {
    private ReportViews() {}

    static final int TOP_LIST = 3; // "그때 내가 한 말" · "멈춘 결제"에 보여 줄 개수

    // ================================================================== 데이터

    /** 이번 급여일 주기 기록에서 뽑은 "내 판단" 묶음. */
    static final class Data {
        /** 대화 뒤 결제한 건 (이유 선택 · 숙고 후), 최근 순. */
        final List<Record> paidAfterTalk = new ArrayList<>();
        /** 쉼표가 말을 건 결제 전체, 최근 순. */
        final List<Record> talked = new ArrayList<>();
        /** 멈춘 결제, 금액 큰 순. */
        final List<Record> stoppedList = new ArrayList<>();
        /** 배민 야식 결제 횟수. */
        int lateCount;

        static Data of(Settings s, List<Record> records, long now, ZoneId zone) {
            Data d = new Data();
            LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
            LocalDate[] cycle = BudgetEngine.cycle(s, today);
            for (Record r : records) {
                LocalDate day = Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate();
                if (day.isBefore(cycle[0]) || !day.isBefore(cycle[1])) continue;
                if (r.isIntervention()) {
                    d.talked.add(r);
                    if (r.outcome == Outcome.PREVENTED) d.stoppedList.add(r);
                }
                if (r.outcome == Outcome.REASON_PAY || r.outcome == Outcome.DELIBERATE_PAY) d.paidAfterTalk.add(r);
                if (r.lateNight && r.app == TargetApp.BAEMIN && r.outcome.spend) d.lateCount++;
            }
            Collections.sort(d.paidAfterTalk, (a, b) -> Long.compare(b.ts, a.ts));
            Collections.sort(d.talked, (a, b) -> Long.compare(b.ts, a.ts));
            Collections.sort(d.stoppedList, (a, b) -> Long.compare(b.amount, a.amount));
            return d;
        }
    }

    // ================================================================== 가치로 묶기

    /**
     * 결제 이유를 가치로 묶는다 (초안 · 기획 확인 필요).
     * 가벼움에서 고른 이유 칩 → 필요 / 사람 / 기분. 이유 없이 대화 끝에 [그래도 살게요]면 TALKED.
     * NEED(필요해서)는 회고에서 뺀다 (AI가 이미 필요한 결제로 판단한 것이라 가치판단이 아니다).
     */
    enum Value {
        NEED("필요해서", "떨어졌거나 끼니라서", ""),
        PEOPLE("사람 때문에", "약속 · 선물", "사람 때문에 쓰는 돈"),
        MOOD("기분으로", "그냥 · 습관처럼", "기분으로 쓰는 돈"),
        TALKED("대화 끝에", "숨숨이와 이야기하고도 [그래도 살게요]", "대화하고도 사는 것"),
        OTHER("다른 이유로", "", "");

        final String name, desc;
        /** "다음 달엔 어떤 결제를 줄이고 싶어요?"의 답 칩. 비어 있으면 칩으로 내지 않는다. */
        final String nextChip;

        Value(String name, String desc, String nextChip) {
            this.name = name;
            this.desc = desc;
            this.nextChip = nextChip;
        }

        static Value of(Record r) {
            if (r.judgment == Judgment.NEED) return NEED;
            if (r.chip == null || r.chip.isEmpty()) return r.outcome == Outcome.DELIBERATE_PAY ? TALKED : OTHER;
            switch (r.chip) {
                case "다 떨어졌어요": case "끼니 대용이에요": case "한 달치 장보기예요":
                case "진짜 배고파요": case "공부하느라요": case "배고파서":
                    return NEED;
                case "약속이 있어요": case "선물이에요":
                    return PEOPLE;
                case "그냥 먹고 싶어요": case "그냥 사고 싶어요": case "습관처럼 시켰어요":
                    return MOOD;
                default:
                    return OTHER;
            }
        }
    }

    /** 가치 하나의 묶음: 결제들 · 금액 · 다음 날 만족/후회/아직 · 고른 이유들. */
    static final class ValueGroup {
        final List<Record> records = new ArrayList<>();
        long sum, satSum, regSum, unrevSum;
        int sat, reg, unrev;
        final List<String> reasons = new ArrayList<>();

        boolean good() {
            return sat > reg;
        }

        boolean bad() {
            return reg > sat;
        }
    }

    /** 대화 뒤 결제한 건(이유 선택 · 숙고 후)을 가치별로, 필요해서는 빼고. 많은 순서. */
    static Map<Value, ValueGroup> valueGroups(Data d) {
        Map<Value, ValueGroup> m = new EnumMap<>(Value.class);
        for (Record r : d.paidAfterTalk) {
            Value v = Value.of(r);
            if (v == Value.NEED) continue;
            ValueGroup g = m.get(v);
            if (g == null) m.put(v, g = new ValueGroup());
            g.records.add(r);
            g.sum += r.amount;
            if (r.review == null) { g.unrev++; g.unrevSum += r.amount; }
            else if (r.review.contains("후회")) { g.reg++; g.regSum += r.amount; }
            else { g.sat++; g.satSum += r.amount; }
            if (r.chip != null && !r.chip.isEmpty() && !g.reasons.contains(r.chip)) g.reasons.add(r.chip);
        }
        List<Map.Entry<Value, ValueGroup>> l = new ArrayList<>(m.entrySet());
        Collections.sort(l, (a, b) -> b.getValue().records.size() - a.getValue().records.size());
        Map<Value, ValueGroup> out = new LinkedHashMap<>();
        for (Map.Entry<Value, ValueGroup> e : l) out.put(e.getKey(), e.getValue());
        return out;
    }

    private static List<Value> pick(Map<Value, ValueGroup> groups, boolean good) {
        List<Value> l = new ArrayList<>();
        for (Map.Entry<Value, ValueGroup> e : groups.entrySet()) {
            if (good ? e.getValue().good() : e.getValue().bad()) l.add(e.getKey());
        }
        return l;
    }

    private static String joinNames(List<Value> l) {
        StringBuilder b = new StringBuilder();
        for (Value v : l) b.append(b.length() == 0 ? "" : "·").append(v.name);
        return b.toString();
    }

    /** "사람 때문에 쓴 돈엔 만족했고, / 기분으로 쓴 돈은 후회했어요". 답이 없으면 null. */
    static String valueInsight(Map<Value, ValueGroup> groups) {
        List<Value> good = pick(groups, true), bad = pick(groups, false);
        if (!good.isEmpty() && !bad.isEmpty()) return joinNames(good) + " 쓴 돈엔 만족했고,\n" + joinNames(bad) + " 쓴 돈은 후회했어요";
        if (!good.isEmpty()) return joinNames(good) + " 쓴 돈엔\n대부분 만족했어요";
        if (!bad.isEmpty()) return joinNames(bad) + " 쓴 돈은\n후회가 남았어요";
        return null;
    }

    // ================================================================== 공통 글자

    private static String app(Record r) {
        if (r.app == null) return "";
        return (r.app == TargetApp.BAEMIN ? "배민" : r.app.label) + " ";
    }

    private static String date(Record r, ZoneId zone) {
        LocalDate d = Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate();
        return d.getMonthValue() + "/" + d.getDayOfMonth();
    }

    /** "Q1. 질문 / A. 답"에서 답만. */
    static String answerOf(String qa) {
        int i = qa.lastIndexOf(" / A. ");
        if (i >= 0) return qa.substring(i + 6).trim();
        i = qa.lastIndexOf("A:");
        return i >= 0 ? qa.substring(i + 2).trim() : qa.trim();
    }

    /** 그 대화에서 가장 "내 말"에 가까운 답: 가장 긴 답 (칩 답 "그래도 살래요"보다 직접 적은 말이 길다). 없으면 "". */
    static String ownWords(Record r) {
        String best = "";
        for (String line : r.qa) {
            String a = answerOf(line);
            if (a.length() > best.length()) best = a;
        }
        return best;
    }

    /** 그 결제가 어떻게 끝났는지: 멈췄어요 / 샀고, 다음 날 만족 · 후회 / 샀어요. */
    private static String ending(Record r) {
        if (r.outcome == Outcome.PREVENTED) return "멈췄어요";
        if (r.review == null) return "샀어요";
        return r.review.contains("후회") ? "샀고, 다음 날 후회" : "샀고, 다음 날 만족";
    }

    /** 하나만 고르는 서비스 색 칩 줄 (아직 저장하지 않는다). 고르면 onPick(고른 글자). */
    private static View choice(Context c, Consumer<String> onPick, String... labels) {
        List<TextView> chips = new ArrayList<>();
        for (String l : labels) chips.add(Ui.accentChip(c, l, false, null));
        for (int i = 0; i < chips.size(); i++) {
            TextView t = chips.get(i);
            String label = labels[i];
            t.setOnClickListener(v -> {
                for (TextView o : chips) Ui.setAccentChipSelected(c, o, o == t);
                if (onPick != null) onPick.accept(label);
            });
        }
        return Ui.chipFlow(c, chips);
    }

    // ================================================================== 화면

    /**
     * 가치 회고.
     * 1) 이번 달 내 기준 (서비스 색 강조 카드)  2) 무엇 때문에 썼고, 다음 날 어땠나요 (가치별 판정)
     * 3) 그때 내가 한 말  4) 멈춘 결제, 지금은 어때요?  5) 다음 달엔
     * 4·5는 화면만 (답 저장은 기록 구조 · 후회율 계산과 엮여 차민상과 합의 후 연결).
     */
    static void reflect(Context c, LinearLayout out, Data d, ZoneId zone) {
        Map<Value, ValueGroup> groups = valueGroups(d);
        Ui.stack(out, standardCard(c, groups), Ui.Space.BLOCK);
        if (!groups.isEmpty()) Ui.stack(out, valuesCard(c, groups), Ui.Space.BLOCK);

        View said = saidCard(c, d, zone);
        if (said != null) Ui.stack(out, said, Ui.Space.BLOCK);
        View stopped = stoppedCard(c, d);
        if (stopped != null) Ui.stack(out, stopped, Ui.Space.BLOCK);

        Ui.stack(out, nextMonthCard(c, d, groups), Ui.Space.BLOCK);
    }

    /** 1) 이번 달 내 기준: 한 문장 + 만족한 가치 · 후회한 가치를 금액과 함께. */
    private static View standardCard(Context c, Map<Value, ValueGroup> groups) {
        String insight = valueInsight(groups);
        if (insight == null) {
            return Ui.heroSentence(c, "이번 달 내 기준", "다음 날 돌아본 답이 모이면\n내 기준이 보여요",
                    "숨숨이와 대화한 뒤 결제하면, 다음 날 만족했는지 물어봐요");
        }
        List<View> rows = new ArrayList<>();
        int answered = 0;
        for (Map.Entry<Value, ValueGroup> e : groups.entrySet()) {
            ValueGroup g = e.getValue();
            answered += g.sat + g.reg;
            if (g.good()) rows.add(Ui.heroRow(c, "만족", Ui.Tone.SUCCESS, e.getKey().name, Money.won(g.satSum)));
        }
        for (Map.Entry<Value, ValueGroup> e : groups.entrySet()) {
            if (e.getValue().bad()) rows.add(Ui.heroRow(c, "후회", Ui.Tone.DANGER, e.getKey().name, Money.won(e.getValue().regSum)));
        }
        return Ui.heroSentence(c, "이번 달 내 기준", insight,
                "대화 뒤 결제하고 다음 날 답한 " + answered + "건으로 봤어요 · 필요해서 산 건 빼고",
                rows.toArray(new View[0]));
    }

    /** 2) 가치마다 한 칸: 판정 태그 · 만족/후회 막대 · 횟수와 금액 · 고른 이유. 칸 바탕은 판정 색. */
    private static View valuesCard(Context c, Map<Value, ValueGroup> groups) {
        LinearLayout card = Ui.card(c);
        card.addView(Ui.caption(c, "무엇 때문에 썼고, 다음 날 어땠나요"));
        LinearLayout legend = Ui.h(c);
        legend.addView(Ui.legendRow(c, Ui.GREEN, "만족", ""), Ui.weight1());
        legend.addView(Ui.legendRow(c, Ui.RED, "후회", ""), Ui.weight1());
        legend.addView(Ui.legendRow(c, Ui.LINE, "아직 안 돌아봄", ""), Ui.weight1());
        Ui.stack(card, legend, Ui.Space.ITEM);

        for (Map.Entry<Value, ValueGroup> e : groups.entrySet()) {
            Value v = e.getKey();
            ValueGroup g = e.getValue();
            int bg;
            String verdict;
            Ui.Tone tone;
            if (g.good()) { bg = Ui.GREEN_BG; verdict = "만족했어요"; tone = Ui.Tone.SUCCESS; }
            else if (g.bad()) { bg = Ui.RED_BG; verdict = "후회했어요"; tone = Ui.Tone.DANGER; }
            else if (g.sat > 0) { bg = Ui.BG; verdict = "반반이에요"; tone = Ui.Tone.WARNING; }
            else { bg = Ui.BG; verdict = "아직 몰라요"; tone = Ui.Tone.PLAIN; }

            LinearLayout b = Ui.tintBox(c, bg, Ui.Pad.BOX_H, Ui.Pad.BOX_V);
            LinearLayout head = Ui.h(c);
            head.addView(Ui.text(c, v.name, Ui.Font.HEADING, Ui.TEXT, Ui.BOLD), Ui.weight1());
            head.addView(Ui.smallTag(c, verdict, tone));
            b.addView(head, Ui.fillW());
            if (!v.desc.isEmpty()) Ui.stack(b, Ui.meta(c, v.desc), Ui.Space.LINE);
            Ui.stack(b, Ui.splitBar(c, new long[]{g.satSum, g.regSum, g.unrevSum}, new int[]{Ui.GREEN, Ui.RED, Ui.LINE}), Ui.Space.ITEM);
            String counts = g.records.size() + "번 · " + Money.won(g.sum)
                    + (g.sat > 0 ? " · 만족 " + g.sat : "") + (g.reg > 0 ? " · 후회 " + g.reg : "") + (g.unrev > 0 ? " · 아직 " + g.unrev : "");
            Ui.stack(b, Ui.text(c, counts, Ui.Font.CAPTION, Ui.TEXT, Ui.MEDIUM), Ui.Space.ITEM);
            if (!g.reasons.isEmpty()) {
                FlowLayout tags = Ui.tagFlow(c);
                for (String r : g.reasons) tags.addView(Ui.smallTag(c, "'" + r + "'", Ui.Tone.PLAIN));
                Ui.stack(b, tags, Ui.Space.ITEM);
            }
            Ui.stack(card, b, Ui.Space.ITEM);
        }
        return card;
    }

    /** 3) 그때 내가 한 말: 대화에서 직접 적은 말 (칩 한마디는 빼고, 필요해서 한 결제도 빼고). 없으면 null. */
    private static View saidCard(Context c, Data d, ZoneId zone) {
        List<Record> said = new ArrayList<>();
        for (Record r : d.talked) if (Value.of(r) != Value.NEED && ownWords(r).length() >= 8) said.add(r);
        if (said.isEmpty()) return null;
        LinearLayout card = Ui.card(c);
        card.addView(Ui.caption(c, "그때 내가 한 말"));
        for (int i = 0; i < said.size() && i < TOP_LIST; i++) {
            Record r = said.get(i);
            Ui.stack(card, Ui.quote(c, ownWords(r),
                    date(r, zone) + " " + app(r) + r.title + " · " + ending(r)), i == 0 ? Ui.Space.ITEM : Ui.Space.ROW);
        }
        return card;
    }

    /** 4) 멈춘 결제, 지금은 어때요? (미리보기). 멈춘 결제가 없으면 null. */
    private static View stoppedCard(Context c, Data d) {
        if (d.stoppedList.isEmpty()) return null;
        LinearLayout card = Ui.card(c);
        card.addView(Ui.caption(c, "멈춘 결제, 지금은 어때요?"));
        for (int i = 0; i < d.stoppedList.size() && i < TOP_LIST; i++) {
            Record r = d.stoppedList.get(i);
            Ui.stack(card, Ui.kv(c, app(r) + r.title, Money.won(r.amount), false), i == 0 ? Ui.Space.ITEM : Ui.Space.STACK);
            Ui.stack(card, choice(c, null, "아직 사고 싶어요", "안 사길 잘했어요"), Ui.Space.ITEM);
        }
        Ui.stack(card, Ui.micro(c, "미리보기예요. 아직 답이 저장되지 않아요.", Ui.SUBTLE), Ui.Space.BLOCK);
        return card;
    }

    /** 5) 다음 달엔: 서비스 색 상자. 이번 달 후회가 남은 가치를 앞에 둔다. 고르면 쉼표의 한마디. */
    private static View nextMonthCard(Context c, Data d, Map<Value, ValueGroup> groups) {
        List<String> options = new ArrayList<>();
        List<Value> bad = pick(groups, false);
        for (Value v : bad) if (!v.nextChip.isEmpty()) options.add(v.nextChip);
        if (!options.contains(Value.MOOD.nextChip)) options.add(Value.MOOD.nextChip);
        options.add(d.lateCount > 0 ? "야식 (이번 달 " + d.lateCount + "번)" : "야식");
        options.add("큰 금액 쇼핑");
        options.add("지금이 좋아요");

        LinearLayout box = Ui.tintBox(c, Ui.BRAND_LIGHT, Ui.Pad.CARD, Ui.Pad.CARD);
        box.addView(Ui.text(c, "다음 달엔", Ui.Font.CAPTION, Ui.GREEN, Ui.BOLD));
        Ui.stack(box, Ui.text(c, "어떤 결제를 줄이고 싶어요?", Ui.Font.TITLE, Ui.TEXT, Ui.BOLD), Ui.Space.ITEM);
        if (!bad.isEmpty()) {
            Ui.stack(box, Ui.text(c, "이번 달 후회가 남은 것부터 보여 드려요", Ui.Font.SMALL, Ui.GREEN, Ui.MEDIUM), Ui.Space.LINE);
        }
        TextView reply = Ui.text(c, "", Ui.Font.BODY, Ui.GREEN, Ui.BOLD);
        reply.setVisibility(View.GONE);
        Ui.stack(box, choice(c, label -> {
            reply.setText(label.equals("지금이 좋아요") ? "좋아요. 지금처럼 쓰고, 다음 달에 다시 돌아봐요."
                    : "좋아요. 다음 달엔 이런 결제 앞에서 숨숨이가 한 번 더 물어볼게요.");
            reply.setVisibility(View.VISIBLE);
        }, options.toArray(new String[0])), Ui.Space.BLOCK);
        Ui.stack(box, Ui.wordBreak(reply), Ui.Space.BLOCK);
        Ui.stack(box, Ui.micro(c, "미리보기예요. 고른 답은 다음 달 대화의 질문 재료로 쓸 예정이에요 (아직 저장 안 됨).", Ui.MUTED), Ui.Space.BLOCK);
        return box;
    }
}
