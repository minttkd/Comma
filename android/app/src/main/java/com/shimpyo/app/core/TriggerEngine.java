package com.shimpyo.app.core;

import com.shimpyo.app.core.Enums.Intensity;
import com.shimpyo.app.core.Enums.Judgment;
import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.Enums.Trigger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * 트리거 평가(REQ-7)와 강도 결정(REQ-8-2).
 * 재정 트리거(재정위험 전환점·고액 단건)가 강도를 정하고,
 * 행동 패턴 트리거(몰아치기·야식 반복)는 개입을 여는 조건이자 질문 재료로만 쓴다.
 */
public final class TriggerEngine {
    private TriggerEngine() {}

    public static final class Evaluation {
        public EnumSet<Trigger> triggers = EnumSet.noneOf(Trigger.class);
        public Intensity base;
        public Intensity intensity;
        public List<String> notes = new ArrayList<>();
        /** 여윳돈에서 나가는 부분 (오늘 누적에 더해짐). */
        public long discretionary;
        /** 식비 예산에서 나가는 부분. */
        public long food;
        /** 결제 후 오늘 누적이 한도를 넘는 금액 (0 이상). */
        public long overLimit;
        /** 결제 후 남은 여윳돈. */
        public long remainingAfter;
        /** 결제 후 급여일까지 하루 얼마로 지내야 하는지. */
        public long dailyAfter;
        /** 이번 결제가 최근 n일 중 몇 번째 야식인지 (야식 시간대 배민일 때만). */
        public int lateCountNow;
        public long lateSumNow;
        public boolean lateHour;
        /** 이번 결제가 1시간 안 몇 번째 애매·충동 결제인지. */
        public int bingeCountNow;
        public boolean zeroOrBelow;
        public int interventionNo;
        public BudgetEngine.State budget;

        public boolean intervene() {
            return !triggers.isEmpty();
        }

        public boolean has(Trigger t) {
            return triggers.contains(t);
        }

        public boolean onlyPattern() {
            for (Trigger t : triggers) if (t.financial) return false;
            return !triggers.isEmpty();
        }
    }

    public static boolean isLateHour(Settings s, long now, ZoneId zone) {
        int h = s.simulatedHour >= 0 ? s.simulatedHour : Instant.ofEpochMilli(now).atZone(zone).getHour();
        if (s.lateStartHour == s.lateEndHour) return false;
        if (s.lateStartHour > s.lateEndHour) return h >= s.lateStartHour || h < s.lateEndHour;
        return h >= s.lateStartHour && h < s.lateEndHour;
    }

    /** 초기화 이후 최근 n일 안의 결제한 야식 기록. */
    public static List<Record> recentLateNight(Settings s, List<Record> records, long now) {
        long from = Math.max(now - s.lateWindowDays * 86_400_000L, s.lateResetTs);
        List<Record> out = new ArrayList<>();
        for (Record r : records) {
            if (r.lateNight && r.app == TargetApp.BAEMIN && r.outcome.spend && r.ts > from && r.ts <= now) out.add(r);
        }
        return out;
    }

    public static int countBinge(Settings s, List<Record> records, long now) {
        long from = now - s.bingeWindowMin * 60_000L;
        int n = 0;
        for (Record r : records) {
            if (r.app == null || !r.outcome.spend || r.ts <= from || r.ts > now) continue;
            boolean discretionaryKind = r.app == TargetApp.BAEMIN || (r.judgment != null && r.judgment != Judgment.NEED);
            if (discretionaryKind) n++;
        }
        return n;
    }

    public static int interventionsToday(List<Record> records, long now, ZoneId zone) {
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        int n = 0;
        for (Record r : records) {
            if (!r.isIntervention()) continue;
            if (Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate().equals(today)) n++;
        }
        return n;
    }

    /**
     * @param interventionsToday 오늘 이미 열린 개입 수 (X로 닫았다 다시 연 것은 세지 않음)
     */
    public static Evaluation evaluate(PaymentAttempt a, Settings s, List<Record> records,
                                      long now, ZoneId zone, int interventionsToday) {
        Evaluation e = new Evaluation();
        BudgetEngine.State b = BudgetEngine.compute(s, records, now, zone);
        e.budget = b;

        boolean baemin = a.app == TargetApp.BAEMIN;
        Judgment j = baemin ? Judgment.AMBIGUOUS : (a.judgment == null ? Judgment.AMBIGUOUS : a.judgment);
        boolean need = !baemin && j == Judgment.NEED;
        boolean foodNeed = need && a.isFood;

        // 1) 금액을 식비 예산 / 여윳돈으로 나눈다 (REQ-2)
        if (foodNeed) {
            e.food = Math.min(a.amount, Math.max(0, b.foodRemaining));
            e.discretionary = a.amount - e.food;
            if (e.discretionary > 0) e.notes.add("식비 잔액 초과분 " + Money.won(e.discretionary) + "만 오늘 누적에 반영");
            else e.notes.add("식비 예산에서 차감 (오늘 누적 변화 없음)");
        } else {
            e.discretionary = a.amount;
        }

        e.remainingAfter = b.remainingNow - e.discretionary;
        e.overLimit = Math.max(0, b.todaySpent + e.discretionary - b.limit);
        e.dailyAfter = Math.max(0, Money.round100((double) e.remainingAfter / Math.max(1, b.daysLeft)));

        // 2) 트리거 계산
        boolean risk = e.discretionary > 0 && (b.todaySpent + e.discretionary > b.limit || e.remainingAfter <= 0);
        e.zeroOrBelow = e.discretionary > 0 && e.remainingAfter <= 0;
        boolean high = a.amount >= s.highAbs || a.amount >= s.highMultipleOfB * b.b;

        e.bingeCountNow = countBinge(s, records, now) + 1;
        boolean binge = e.bingeCountNow >= s.bingeCount;

        e.lateHour = baemin && isLateHour(s, now, zone);
        boolean late = false;
        if (e.lateHour) {
            List<Record> recent = recentLateNight(s, records, now);
            e.lateCountNow = recent.size() + 1;
            e.lateSumNow = a.amount;
            for (Record r : recent) e.lateSumNow += r.amount;
            late = s.lateEnabled && e.lateCountNow >= s.lateCount;
        }

        // 3) 판정 결과별 적용 범위
        if (need) {
            // 필요 판정(식료품 포함): 재정위험·고액 두 트리거만 평가
            if (risk) e.triggers.add(Trigger.FIN_RISK);
            if (high) e.triggers.add(Trigger.HIGH_AMOUNT);
            if (e.triggers.isEmpty()) e.notes.add(foodNeed ? "식료품 + 필요 → 개입 없음" : "필요 판정 · 고액·재정위험 아님 → 개입 없음");
        } else {
            if (risk) e.triggers.add(Trigger.FIN_RISK);
            if (high) e.triggers.add(Trigger.HIGH_AMOUNT);
            if (binge) e.triggers.add(Trigger.BINGE);
            if (late) e.triggers.add(Trigger.LATE_NIGHT);
        }
        if (e.triggers.isEmpty()) return e;

        // 4) 강도 결정
        Intensity base;
        if (e.zeroOrBelow) base = Intensity.STRONG;
        else if (e.has(Trigger.FIN_RISK) && e.has(Trigger.HIGH_AMOUNT)) base = Intensity.STRONG;
        else if (e.has(Trigger.FIN_RISK)) base = Intensity.MEDIUM;
        else base = Intensity.LIGHT; // 고액만 · 행동 패턴 트리거만
        e.base = base;
        Intensity it = base;
        if (need) {
            it = it.down();
            e.notes.add("필요 판정 → 한 단계 낮춤");
        }
        e.interventionNo = interventionsToday + 1;
        if (e.interventionNo >= 3) {
            Intensity up = it.up();
            if (up != it) e.notes.add("오늘 " + e.interventionNo + "번째 개입 → 한 단계 올림");
            it = up;
        }
        e.intensity = it;
        return e;
    }
}
