package com.shimpyo.app;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.shimpyo.app.core.BudgetEngine;
import com.shimpyo.app.core.Enums.Intensity;
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

/**
 * 리포트(S10) 후보 화면. UiPrefs.reportStyle로 고른다 (UI 갤러리 › 앱 UI 설정 › 리포트 내용).
 *
 * 방향: "가벼움·보통·강함 / 결과 분류"처럼 시스템 용어 대신, 사용자가 직접 한 판단을 보여 준다.
 * - 결제할 때 고른 이유 (가벼움 이유 칩)
 * - 다음 날 돌아본 마음 (S09 회고: 만족해요 / 후회돼요)
 * - 대화하다 멈춘 결제 (방지 실적)
 * - 강도(가벼움 · 보통 · 강함)는 "쉼표가 얼마나 깊게 물었는지"로 바꿔 부른다 (③ 대화 깊이)
 *
 * 집계는 화면 전용이라 core/Stats를 건드리지 않고 여기서 기록을 직접 센다.
 * 후보가 정해지면 필요한 집계만 Stats(core)로 옮기는 걸 차민상과 상의한다.
 */
final class ReportViews {
    private ReportViews() {}

    static final int TOP_LIST = 3; // 만족 · 후회 목록에 보여 줄 개수

    // ================================================================== 데이터

    /** 이번 급여일 주기 기록에서 뽑은 "내 판단" 묶음. */
    static final class Data {
        int talks;              // 쉼표가 말을 건 결제
        int stopped;            // 그중 멈춘 결제
        long saved;             // 멈춰서 아낀 돈
        Record bestStop;        // 가장 큰 금액을 멈춘 결제
        final List<Record> satisfied = new ArrayList<>(); // 다음 날 "만족해요"
        final List<Record> regretted = new ArrayList<>(); // 다음 날 "후회돼요"
        final List<Record> unreviewed = new ArrayList<>(); // 대화 뒤 결제했지만 아직 안 돌아본 결제
        long satisfiedSum, regrettedSum, unreviewedSum;
        /** 고른 이유 → [고른 횟수, 후회, 만족]. 많이 고른 순서. */
        final Map<String, int[]> reasons = new LinkedHashMap<>();
        int lateCount;
        long lateSum;
        /** 대화 뒤 결제한 건 (이유 선택 · 숙고 후), 최근 순. */
        final List<Record> paidAfterTalk = new ArrayList<>();
        /** 쉼표가 말을 건 결제 전체, 최근 순. */
        final List<Record> talked = new ArrayList<>();
        /** 멈춘 결제, 금액 큰 순. */
        final List<Record> stoppedList = new ArrayList<>();
        /** 깊이(강도)별 대화, 시간 순. */
        final Map<Intensity, List<Record>> byDepth = new EnumMap<>(Intensity.class);

        /** 깊이 하나의 [대화 수, 멈춘 수]. */
        int[] depth(Intensity i) {
            int[] v = new int[2];
            List<Record> l = byDepth.get(i);
            if (l == null) return v;
            v[0] = l.size();
            for (Record r : l) if (r.outcome == Outcome.PREVENTED) v[1]++;
            return v;
        }

        static Data of(Settings s, List<Record> records, long now, ZoneId zone) {
            Data d = new Data();
            LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
            LocalDate[] cycle = BudgetEngine.cycle(s, today);
            Map<String, int[]> reasons = new LinkedHashMap<>();
            for (Record r : records) {
                LocalDate day = Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate();
                if (day.isBefore(cycle[0]) || !day.isBefore(cycle[1])) continue;
                if (r.isIntervention()) {
                    d.talks++;
                    List<Record> l = d.byDepth.get(r.intensity);
                    if (l == null) d.byDepth.put(r.intensity, l = new ArrayList<>());
                    l.add(r);
                    d.talked.add(r);
                    if (r.outcome == Outcome.PREVENTED) {
                        d.stopped++;
                        d.saved += r.amount;
                        if (d.bestStop == null || r.amount > d.bestStop.amount) d.bestStop = r;
                        d.stoppedList.add(r);
                    }
                }
                boolean paidAfterTalk = r.outcome == Outcome.REASON_PAY || r.outcome == Outcome.DELIBERATE_PAY;
                if (paidAfterTalk) d.paidAfterTalk.add(r);
                if (r.review != null && r.review.contains("후회")) {
                    d.regretted.add(r);
                    d.regrettedSum += r.amount;
                } else if (r.review != null) {
                    d.satisfied.add(r);
                    d.satisfiedSum += r.amount;
                } else if (paidAfterTalk) {
                    d.unreviewed.add(r);
                    d.unreviewedSum += r.amount;
                }
                if (r.chip != null && !r.chip.isEmpty()) {
                    int[] v = reasons.get(r.chip);
                    if (v == null) reasons.put(r.chip, v = new int[3]);
                    v[0]++;
                    if (r.review != null && r.review.contains("후회")) v[1]++;
                    else if (r.review != null) v[2]++;
                }
                if (r.lateNight && r.app == TargetApp.BAEMIN && r.outcome.spend) {
                    d.lateCount++;
                    d.lateSum += r.amount;
                }
            }
            List<Map.Entry<String, int[]>> sorted = new ArrayList<>(reasons.entrySet());
            Collections.sort(sorted, (a, b) -> b.getValue()[0] - a.getValue()[0]);
            for (Map.Entry<String, int[]> e : sorted) d.reasons.put(e.getKey(), e.getValue());
            for (List<Record> l : d.byDepth.values()) Collections.sort(l, (a, b) -> Long.compare(a.ts, b.ts));
            Collections.sort(d.paidAfterTalk, (a, b) -> Long.compare(b.ts, a.ts));
            Collections.sort(d.talked, (a, b) -> Long.compare(b.ts, a.ts));
            Collections.sort(d.stoppedList, (a, b) -> Long.compare(b.amount, a.amount));
            Collections.sort(d.satisfied, (a, b) -> Long.compare(b.amount, a.amount));
            Collections.sort(d.regretted, (a, b) -> Long.compare(b.amount, a.amount));
            return d;
        }
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

    /** 결제할 때의 판단: 고른 이유 또는 [그래도 살게요]. */
    private static String why(Record r) {
        if (r.chip != null && !r.chip.isEmpty()) return "고른 이유 '" + r.chip + "'";
        if (r.outcome == Outcome.DELIBERATE_PAY) return "대화 뒤 [그래도 " + (r.app == TargetApp.BAEMIN ? "시킬게요" : "살게요") + "]";
        return "";
    }

    private static String sub(Record r, ZoneId zone) {
        String w = why(r);
        return date(r, zone) + (w.isEmpty() ? "" : " · " + w);
    }

    /** 이유 하나의 뒷말: "3번 · 후회 2" */
    private static String reasonCount(int[] v) {
        String s = v[0] + "번";
        if (v[1] > 0) s += " · 후회 " + v[1];
        if (v[2] > 0) s += " · 만족 " + v[2];
        return s;
    }

    private static void reviewList(Context c, LinearLayout out, String caption, List<Record> list, int dot, String tag, Ui.Tone tone, ZoneId zone) {
        if (list.isEmpty()) return;
        LinearLayout card = Ui.card(c);
        card.addView(Ui.caption(c, caption + " " + list.size() + "건"));
        for (int i = 0; i < list.size() && i < TOP_LIST; i++) {
            Record r = list.get(i);
            Ui.stack(card, Ui.listRow(c, dot, app(r) + r.title, sub(r, zone), Money.won(r.amount), Ui.smallTag(c, tag, tone)), Ui.Space.BLOCK);
        }
        Ui.stack(out, card, Ui.Space.BLOCK);
    }

    /** 결제할 때 내가 고른 이유: 이유별 횟수 · 후회 · 만족. */
    private static LinearLayout reasonsCard(Context c, Data d) {
        LinearLayout why = Ui.card(c);
        why.addView(Ui.caption(c, "결제할 때 내가 고른 이유"));
        if (d.reasons.isEmpty()) {
            Ui.stack(why, Ui.bodySmall(c, "쉼표가 \"지금 사려는 이유가 뭐예요?\"라고 물을 때 고른 답이 여기에 모여요."), Ui.Space.ITEM);
        }
        for (Map.Entry<String, int[]> e : d.reasons.entrySet()) {
            Ui.stack(why, Ui.kv(c, "'" + e.getKey() + "'", reasonCount(e.getValue()), false), Ui.Space.ITEM);
        }
        return why;
    }

    // ================================================================== ② 가치 회고

    /**
     * 결제 이유를 가치로 묶는다 (초안 · 기획 확인 필요).
     * 가벼움에서 고른 이유 칩 → 필요 / 사람 / 기분. 이유 없이 대화 끝에 [그래도 살게요]면 TALKED.
     */
    enum Value {
        NEED("필요해서", "떨어졌거나 끼니라서"),
        PEOPLE("사람 때문에", "약속 · 선물"),
        MOOD("기분으로", "그냥 · 습관처럼"),
        TALKED("대화 끝에", "쉼표와 이야기하고도 [그래도 살게요]"),
        OTHER("다른 이유로", "");

        final String name, desc;

        Value(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        static Value of(Record r) {
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
    }

    /** 대화 뒤 결제한 건(이유 선택 · 숙고 후)을 가치별로. 많은 순서. */
    static Map<Value, ValueGroup> valueGroups(Data d) {
        Map<Value, ValueGroup> m = new EnumMap<>(Value.class);
        for (Record r : d.paidAfterTalk) {
            Value v = Value.of(r);
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

    private static String joinNames(List<Value> l) {
        StringBuilder b = new StringBuilder();
        for (Value v : l) b.append(b.length() == 0 ? "" : "·").append(v.name);
        return b.toString();
    }

    /** "필요해서·사람 때문에 쓴 돈엔 만족했고, 기분으로 쓴 돈은 후회했어요". 답이 없으면 null. */
    static String valueInsight(Map<Value, ValueGroup> groups) {
        List<Value> good = new ArrayList<>(), bad = new ArrayList<>();
        for (Map.Entry<Value, ValueGroup> e : groups.entrySet()) {
            ValueGroup g = e.getValue();
            if (g.sat > g.reg) good.add(e.getKey());
            else if (g.reg > g.sat) bad.add(e.getKey());
        }
        if (!good.isEmpty() && !bad.isEmpty()) return joinNames(good) + " 쓴 돈엔 만족했고, " + joinNames(bad) + " 쓴 돈은 후회했어요";
        if (!good.isEmpty()) return joinNames(good) + " 쓴 돈엔 대부분 만족했어요";
        if (!bad.isEmpty()) return joinNames(bad) + " 쓴 돈은 후회가 남았어요";
        return null;
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

    /** 미리보기 칩 한 줄: 하나만 고를 수 있고, 아직 저장하지 않는다. */
    private static View previewChoice(Context c, String... labels) {
        List<TextView> chips = new ArrayList<>();
        for (String l : labels) chips.add(Ui.chip(c, l, false, null));
        for (TextView t : chips) {
            t.setOnClickListener(v -> {
                for (TextView o : chips) Ui.setChipSelected(c, o, o == t);
            });
        }
        return Ui.chipFlow(c, chips);
    }

    /**
     * ② 가치 회고: 숫자 대신 "내가 무엇에 돈을 쓰고, 다음 날 어떻게 느꼈는지"로 돌아보게 한다.
     * 1) 이번 달 내 기준 한 문장  2) 이유를 가치로 묶어 만족/후회  3) 그때 내가 한 말
     * 4) 멈춘 결제, 지금은 어때요? · 5) 다음 달엔 — 4·5는 화면만 (답 저장은 기록 구조 · 후회율 계산과 엮여 차민상과 합의 후 연결)
     */
    static void reflect(Context c, LinearLayout out, Data d, ZoneId zone) {
        Map<Value, ValueGroup> groups = valueGroups(d);
        String insight = valueInsight(groups);
        int answered = 0;
        for (ValueGroup g : groups.values()) answered += g.sat + g.reg;
        Ui.stack(out, Ui.heroSentence(c, "이번 달 내 기준",
                insight != null ? insight : "다음 날 돌아본 답이 모이면 내 기준이 보여요",
                insight != null ? "대화 뒤 결제하고 다음 날 '만족해요 / 후회돼요'로 답한 " + answered + "건으로 봤어요"
                        : "쉼표와 대화한 뒤 결제하면, 다음 날 만족했는지 물어봐요"), Ui.Space.BLOCK);

        // 2) 이유를 가치로
        if (!groups.isEmpty()) {
            LinearLayout card = Ui.card(c);
            card.addView(Ui.caption(c, "무엇 때문에 썼고, 다음 날 어땠나요"));
            boolean first = true;
            for (Map.Entry<Value, ValueGroup> e : groups.entrySet()) {
                Value v = e.getKey();
                ValueGroup g = e.getValue();
                if (!first) {
                    View line = Ui.divider(c);
                    card.addView(line);
                    Ui.gapTop(line, c, Ui.Space.STACK);
                }
                LinearLayout b = Ui.v(c);
                LinearLayout head = Ui.h(c);
                head.addView(Ui.text(c, v.name, Ui.Font.BODY, Ui.TEXT, Ui.BOLD), Ui.weight1());
                head.addView(Ui.text(c, g.records.size() + "번 · " + Money.won(g.sum), Ui.Font.SMALL, Ui.TEXT, Ui.MEDIUM));
                b.addView(head, Ui.fillW());
                if (!v.desc.isEmpty()) Ui.stack(b, Ui.meta(c, v.desc), Ui.Space.LINE);
                Ui.stack(b, Ui.splitBar(c, new long[]{g.satSum, g.regSum, g.unrevSum}, new int[]{Ui.GREEN, Ui.RED, Ui.LINE}), Ui.Space.ITEM);
                String mood = (g.sat > 0 ? "만족 " + g.sat : "") + (g.reg > 0 ? (g.sat > 0 ? " · " : "") + "후회 " + g.reg : "")
                        + (g.unrev > 0 ? (g.sat + g.reg > 0 ? " · " : "") + "아직 안 돌아봄 " + g.unrev : "");
                Ui.stack(b, Ui.text(c, mood, Ui.Font.CAPTION, g.reg > g.sat ? Ui.RED : g.sat > g.reg ? Ui.GREEN : Ui.MUTED, Ui.MEDIUM), Ui.Space.TIGHT);
                if (!g.reasons.isEmpty()) {
                    FlowLayout tags = Ui.tagFlow(c);
                    for (String r : g.reasons) tags.addView(Ui.smallTag(c, "'" + r + "'", Ui.Tone.NEUTRAL));
                    Ui.stack(b, tags, Ui.Space.ITEM);
                }
                Ui.stack(card, b, first ? Ui.Space.ITEM : Ui.Space.STACK);
                first = false;
            }
            Ui.stack(card, Ui.legendRow(c, Ui.GREEN, "다음 날 만족", ""), Ui.Space.SECTION);
            Ui.stack(card, Ui.legendRow(c, Ui.RED, "다음 날 후회", ""), Ui.Space.TIGHT);
            Ui.stack(out, card, Ui.Space.BLOCK);
        }

        // 3) 그때 내가 한 말
        List<Record> said = new ArrayList<>();
        for (Record r : d.talked) if (ownWords(r).length() >= 8) said.add(r); // 칩 한마디가 아니라 직접 적은 말만
        if (!said.isEmpty()) {
            LinearLayout card = Ui.card(c);
            card.addView(Ui.caption(c, "그때 내가 한 말"));
            for (int i = 0; i < said.size() && i < TOP_LIST; i++) {
                Record r = said.get(i);
                Ui.stack(card, Ui.quote(c, ownWords(r),
                        date(r, zone) + " " + app(r) + r.title + " · " + ending(r)), i == 0 ? Ui.Space.ITEM : Ui.Space.ROW);
            }
            Ui.stack(out, card, Ui.Space.BLOCK);
        }

        // 4) 멈춘 결제, 지금은 어때요? (미리보기)
        if (!d.stoppedList.isEmpty()) {
            LinearLayout card = Ui.card(c);
            card.addView(Ui.caption(c, "멈춘 결제, 지금은 어때요?"));
            for (int i = 0; i < d.stoppedList.size() && i < TOP_LIST; i++) {
                Record r = d.stoppedList.get(i);
                Ui.stack(card, Ui.kv(c, app(r) + r.title, Money.won(r.amount), false), i == 0 ? Ui.Space.ITEM : Ui.Space.STACK);
                Ui.stack(card, previewChoice(c, "아직 사고 싶어요", "안 사길 잘했어요"), Ui.Space.ITEM);
            }
            Ui.stack(card, Ui.micro(c, "미리보기예요. 아직 답이 저장되지 않아요.", Ui.SUBTLE), Ui.Space.BLOCK);
            Ui.stack(out, card, Ui.Space.BLOCK);
        }

        // 5) 다음 달엔 (미리보기)
        LinearLayout next = Ui.card(c);
        next.addView(Ui.caption(c, "다음 달엔"));
        Ui.stack(next, Ui.heading(c, "어떤 결제를 줄이고 싶어요?"), Ui.Space.ITEM);
        Ui.stack(next, previewChoice(c, "기분으로 쓰는 돈", "야식", "큰 금액 쇼핑", "지금이 좋아요"), Ui.Space.BLOCK);
        Ui.stack(next, Ui.micro(c, "미리보기예요. 고른 답은 다음 달 대화의 질문 재료로 쓸 예정이에요 (아직 저장 안 됨).", Ui.SUBTLE), Ui.Space.BLOCK);
        Ui.stack(out, next, Ui.Space.BLOCK);
    }

    // ================================================================== ② 만족 · 후회

    /** ② 다음 날 돌아본 마음(만족·후회)을 중심에, 그 결제를 할 때 고른 이유를 아래에. */
    static void values(Context c, LinearLayout out, Data d, ZoneId zone) {
        LinearLayout mood = Ui.card(c);
        mood.addView(Ui.caption(c, "결제하고 다음 날 돌아본 내 마음"));
        if (d.satisfied.isEmpty() && d.regretted.isEmpty()) {
            Ui.stack(mood, Ui.bodySmall(c, "쉼표와 대화한 뒤 결제한 건은 다음 날 '만족해요 / 후회돼요'로 물어봐요. 답이 모이면 여기에 보여요."), Ui.Space.ITEM);
        } else {
            Ui.stack(mood, Ui.heading(c, "만족 " + d.satisfied.size() + "건 · 후회 " + d.regretted.size() + "건"), Ui.Space.ITEM);
            Ui.stack(mood, Ui.splitBar(c, new long[]{d.satisfiedSum, d.regrettedSum, d.unreviewedSum},
                    new int[]{Ui.GREEN, Ui.RED, Ui.LINE}), Ui.Space.BLOCK);
            Ui.stack(mood, Ui.legendRow(c, Ui.GREEN, "만족해요", Money.won(d.satisfiedSum)), Ui.Space.BLOCK);
            Ui.stack(mood, Ui.legendRow(c, Ui.RED, "후회돼요", Money.won(d.regrettedSum)), Ui.Space.ITEM);
            if (!d.unreviewed.isEmpty()) {
                Ui.stack(mood, Ui.legendRow(c, Ui.LINE, "아직 안 돌아봤어요", Money.won(d.unreviewedSum)), Ui.Space.ITEM);
            }
        }
        Ui.stack(out, mood, Ui.Space.BLOCK);

        reviewList(c, out, "후회한 결제", d.regretted, Ui.RED, "후회돼요", Ui.Tone.DANGER, zone);
        reviewList(c, out, "만족한 결제", d.satisfied, Ui.GREEN, "만족해요", Ui.Tone.SUCCESS, zone);

        Ui.stack(out, reasonsCard(c, d), Ui.Space.BLOCK);

        Ui.stack(out, Ui.highlightBox(c, "쉼표와 이야기하다 멈춘 결제", d.stopped == 0 ? "멈춘 결제가 생기면 여기 쌓여요"
                : d.stopped + "번 멈춰서 " + Money.won(d.saved) + " 아꼈어요"), Ui.Space.BLOCK);
    }

    // ================================================================== ③ 대화 깊이

    /**
     * 강도(가벼움 · 보통 · 강함)를 사용자 말로: 쉼표가 얼마나 깊게 물었는지.
     * 시스템 용어 대신 "그때 무슨 일이 있었는지"로 부른다.
     */
    static String depthName(Intensity i) {
        switch (i) {
            case LIGHT: return "살짝 물어봤을 때";
            case MEDIUM: return "이야기를 나눴을 때";
            default: return "깊게 이야기했을 때";
        }
    }

    /** 그 깊이에서 사용자가 한 일. */
    static String depthHow(Intensity i) {
        switch (i) {
            case LIGHT: return "이유 하나만 골랐어요";
            case MEDIUM: return "질문 두 개에 답했어요";
            default: return "질문 세 개, 한 번은 직접 적었어요";
        }
    }

    private static int rate(int[] v) {
        return v[0] == 0 ? -1 : Math.round(v[1] * 100f / v[0]);
    }

    /** 깊이별 멈춘 비율을 한 문장으로. 비교할 깊이가 두 개 미만이면 null. */
    static String depthInsight(Data d) {
        List<Intensity> has = new ArrayList<>();
        for (Intensity i : Intensity.values()) if (d.depth(i)[0] > 0) has.add(i);
        if (has.size() < 2) return null;
        Intensity first = has.get(0), last = has.get(has.size() - 1);
        int a = rate(d.depth(first)), b = rate(d.depth(last));
        boolean up = true, down = true;
        for (int k = 1; k < has.size(); k++) {
            int prev = rate(d.depth(has.get(k - 1))), cur = rate(d.depth(has.get(k)));
            if (cur < prev) up = false;
            if (cur > prev) down = false;
        }
        if (up && b > a) return "깊게 이야기할수록 더 자주 멈췄어요";
        if (down && a > b) return "살짝 물어봤을 때 오히려 더 잘 멈췄어요";
        Intensity best = has.get(0);
        for (Intensity i : has) if (rate(d.depth(i)) > rate(d.depth(best))) best = i;
        return "'" + depthName(best) + "' 가장 자주 멈췄어요";
    }

    /** 깊이 하나: ●●○ 이름 · "6번 중 2번 멈춤" / 대화마다 점 하나 (초록 = 멈춤, 회색 = 결제) / 한 일 · 아낀 돈. */
    private static View depthBlock(Context c, Data d, Intensity i) {
        LinearLayout b = Ui.v(c);
        int[] v = d.depth(i);
        LinearLayout head = Ui.h(c);
        head.addView(Ui.levelDots(c, i.ordinal() + 1, Intensity.values().length));
        Ui.inlineFill(head, Ui.text(c, depthName(i), Ui.Font.BODY, Ui.TEXT, Ui.BOLD), Ui.Space.ITEM);
        Ui.inline(head, Ui.text(c, v[0] == 0 ? "아직 없어요" : v[0] + "번 중 " + v[1] + "번 멈춤", Ui.Font.SMALL, v[0] == 0 ? Ui.SUBTLE : Ui.TEXT, Ui.MEDIUM), Ui.Space.ITEM);
        b.addView(head, Ui.fillW());
        List<Record> talks = d.byDepth.get(i);
        if (talks != null && !talks.isEmpty()) {
            int[] colors = new int[talks.size()];
            for (int k = 0; k < colors.length; k++) colors[k] = talks.get(k).outcome == Outcome.PREVENTED ? Ui.GREEN : Ui.LINE;
            Ui.stack(b, Ui.dotFlow(c, colors), Ui.Space.ITEM);
        }
        long saved = 0;
        if (talks != null) for (Record r : talks) if (r.outcome == Outcome.PREVENTED) saved += r.amount;
        Ui.stack(b, Ui.meta(c, depthHow(i) + (saved > 0 ? " · " + Money.won(saved) + " 아낌" : "")), Ui.Space.TIGHT);
        return b;
    }

    /** ③ 대화 깊이: 강도 세 단계를 "쉼표가 얼마나 깊게 물었는지"로 보여 준다. 대화 하나가 점 하나. */
    static void depth(Context c, LinearLayout out, Data d) {
        Ui.stack(out, Ui.heroCard(c, "이번 달 쉼표", d.stopped + "번 멈췄어요",
                d.talks == 0 ? "아직 쉼표가 말을 건 결제가 없어요"
                        : "쉼표가 말을 건 " + d.talks + "번 중 · " + Money.won(d.saved) + " 아낌", true), Ui.Space.BLOCK);
        String insight = depthInsight(d);
        if (insight != null) {
            Ui.stack(out, Ui.tintNote(c, insight, Ui.Font.BODY, Ui.GREEN, Ui.Radius.INPUT, Ui.Pad.FOOD_NOTE_H, Ui.Pad.FOOD_NOTE_V), Ui.Space.BLOCK);
        }
        LinearLayout card = Ui.card(c);
        card.addView(Ui.caption(c, "쉼표가 얼마나 깊게 물었나요"));
        Intensity[] all = Intensity.values();
        for (int k = 0; k < all.length; k++) {
            if (k > 0) {
                View line = Ui.divider(c);
                card.addView(line);
                Ui.gapTop(line, c, Ui.Space.STACK);
            }
            Ui.stack(card, depthBlock(c, d, all[k]), Ui.Space.STACK);
        }
        LinearLayout legend = Ui.h(c);
        legend.addView(Ui.legendRow(c, Ui.GREEN, "멈췄어요", ""), Ui.weight1());
        legend.addView(Ui.legendRow(c, Ui.LINE, "결제했어요", ""), Ui.weight1());
        Ui.stack(card, legend, Ui.Space.SECTION);
        Ui.stack(out, card, Ui.Space.BLOCK);
        Ui.stack(out, reasonsCard(c, d), Ui.Space.BLOCK);
    }

    // ================================================================== ④ 숫자 세 개

    /** ④ 숫자 세 개만: 멈춰서 아낀 돈 · 만족한 소비 · 후회한 소비 (+ 후회가 남은 이유). */
    static void simple(Context c, LinearLayout out, Data d) {
        Ui.stack(out, Ui.heroCard(c, "멈춰서 아낀 돈", Money.won(d.saved),
                d.stopped == 0 ? "쉼표와 이야기하다 멈춘 결제가 여기 쌓여요" : d.stopped + "번 멈췄어요", true), Ui.Space.BLOCK);
        Ui.stack(out, Ui.tilePair(c,
                Ui.statTile(c, "만족한 소비", Money.won(d.satisfiedSum), "다음 날 만족 " + d.satisfied.size() + "건", false),
                Ui.statTile(c, "후회한 소비", Money.won(d.regrettedSum), "다음 날 후회 " + d.regretted.size() + "건", false),
                Ui.Space.ROW), Ui.Space.BLOCK);
        List<String> regretReasons = new ArrayList<>();
        for (Map.Entry<String, int[]> e : d.reasons.entrySet()) if (e.getValue()[1] > 0) regretReasons.add(e.getKey());
        if (!regretReasons.isEmpty()) {
            LinearLayout card = Ui.card(c);
            card.addView(Ui.caption(c, "후회가 남은 이유"));
            for (String k : regretReasons) {
                Ui.stack(card, Ui.kv(c, "'" + k + "'", "후회 " + d.reasons.get(k)[1] + " / " + d.reasons.get(k)[0] + "번", false), Ui.Space.ITEM);
            }
            Ui.stack(out, card, Ui.Space.BLOCK);
        }
    }
}
