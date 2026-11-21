package com.shimpyo.app;

import android.content.Context;
import android.widget.LinearLayout;

import com.shimpyo.app.core.BudgetEngine;
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

        boolean empty() {
            return talks == 0 && satisfied.isEmpty() && regretted.isEmpty() && reasons.isEmpty();
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
                    if (r.outcome == Outcome.PREVENTED) {
                        d.stopped++;
                        d.saved += r.amount;
                        if (d.bestStop == null || r.amount > d.bestStop.amount) d.bestStop = r;
                    }
                }
                boolean paidAfterTalk = r.outcome == Outcome.REASON_PAY || r.outcome == Outcome.DELIBERATE_PAY;
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

        LinearLayout why = Ui.card(c);
        why.addView(Ui.caption(c, "결제할 때 내가 고른 이유"));
        if (d.reasons.isEmpty()) {
            Ui.stack(why, Ui.bodySmall(c, "쉼표가 \"지금 사려는 이유가 뭐예요?\"라고 물을 때 고른 답이 여기에 모여요."), Ui.Space.ITEM);
        }
        for (Map.Entry<String, int[]> e : d.reasons.entrySet()) {
            Ui.stack(why, Ui.kv(c, "'" + e.getKey() + "'", reasonCount(e.getValue()), false), Ui.Space.ITEM);
        }
        Ui.stack(out, why, Ui.Space.BLOCK);

        Ui.stack(out, Ui.highlightBox(c, "쉼표와 이야기하다 멈춘 결제", d.stopped == 0 ? "멈춘 결제가 생기면 여기 쌓여요"
                : d.stopped + "번 멈춰서 " + Money.won(d.saved) + " 아꼈어요"), Ui.Space.BLOCK);
    }

    // ================================================================== ③ 한 달 이야기

    /** ③ 한 달을 문장 몇 개로: 가장 잘한 결정 · 다시 생각해 볼 결제 · 잘 산 결제 · 자주 고른 이유. */
    static void story(Context c, LinearLayout out, Data d, ZoneId zone) {
        Ui.stack(out, Ui.heroCard(c, "이번 달 쉼표", d.stopped + "번 멈췄어요",
                d.talks == 0 ? "아직 쉼표가 말을 건 결제가 없어요"
                        : "쉼표가 말을 건 " + d.talks + "번 중 · " + Money.won(d.saved) + " 아낌", true), Ui.Space.BLOCK);
        if (d.empty()) {
            Ui.stack(out, Ui.sentenceCard(c, "한 달 이야기", "쉼표와 대화한 결제가 생기면 여기에 이야기가 채워져요",
                    "결제할 때 고른 이유와 다음 날 돌아본 마음으로 만들어요."), Ui.Space.BLOCK);
            return;
        }
        if (d.bestStop != null) {
            Record r = d.bestStop;
            Ui.stack(out, Ui.sentenceCard(c, "가장 잘한 결정", date(r, zone) + " " + app(r) + r.title + " 결제를 멈췄어요",
                    Money.won(r.amount) + " 아꼈어요" + (r.stoppedAt > 0 ? " · " + r.stoppedAt + "번째 질문에서 마음을 돌렸어요" : "")), Ui.Space.BLOCK);
        }
        if (!d.regretted.isEmpty()) {
            Record r = d.regretted.get(0);
            String w = why(r);
            Ui.stack(out, Ui.sentenceCard(c, "다시 생각해 볼 결제", app(r) + r.title + ", 다음 날 후회된다고 했어요",
                    Money.won(r.amount) + (w.isEmpty() ? "" : " · 그때 " + w)), Ui.Space.BLOCK);
        }
        if (!d.satisfied.isEmpty()) {
            Record r = d.satisfied.get(0);
            String w = why(r);
            Ui.stack(out, Ui.sentenceCard(c, "잘 샀다고 한 결제", app(r) + r.title + ", 다음 날 만족한다고 했어요",
                    Money.won(r.amount) + (w.isEmpty() ? "" : " · 그때 " + w)), Ui.Space.BLOCK);
        }
        if (!d.reasons.isEmpty()) {
            Map.Entry<String, int[]> top = d.reasons.entrySet().iterator().next();
            int[] v = top.getValue();
            String after = v[1] > 0 ? "그중 " + v[1] + "번은 다음 날 후회했어요"
                    : v[2] == v[0] ? "모두 다음 날 만족했어요" : "아직 돌아보지 않은 결제가 있어요";
            Ui.stack(out, Ui.sentenceCard(c, "가장 자주 고른 이유", "'" + top.getKey() + "' 이유로 " + v[0] + "번 결제했어요", after), Ui.Space.BLOCK);
        }
        if (d.lateCount > 0) {
            Ui.stack(out, Ui.sentenceCard(c, "야식", "야식을 " + d.lateCount + "번 시켰어요", Money.won(d.lateSum)), Ui.Space.BLOCK);
        }
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
