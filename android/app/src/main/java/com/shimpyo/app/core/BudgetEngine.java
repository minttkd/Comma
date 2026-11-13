package com.shimpyo.app.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 예산 엔진 (REQ-4).
 * 여윳돈 = 수입 − 고정지출 − 식비 예산
 * B = 여윳돈 / 주기, Q = 남은 여윳돈(오늘 시작 기준) / 남은 일수, 적용 한도 = min(Q, k·B)
 */
public final class BudgetEngine {
    private BudgetEngine() {}

    public static final class State {
        public long monthly;          // 월 여윳돈
        public LocalDate cycleStart;
        public LocalDate nextPayday;
        public int cycleDays;
        public int dayIndex;          // 주기 몇 일째 (1부터)
        public int daysLeft;          // 오늘 포함 급여일까지 남은 일수
        public double b;              // 기본 일일액
        public long spentBeforeToday; // 이번 주기, 어제까지 여윳돈 지출
        public long remainingAtDayStart;
        public double q;              // 당일 가용액
        public long limit;            // 적용 한도
        public long todaySpent;       // 오늘 누적 (여윳돈)
        public long available;        // 오늘 쓸 수 있는 돈 = 한도 − 오늘 누적
        public long remainingNow;     // 지금 남은 여윳돈
        public long foodBudget;
        public long foodSpent;
        public long foodRemaining;

        /** 홈 신호등: 0 초록, 1 노랑, 2 빨강 */
        public int signal() {
            if (remainingNow <= 0 || todaySpent > limit) return 2;
            if (limit > 0 && todaySpent >= limit * 0.7) return 1;
            return 0;
        }
    }

    public static LocalDate[] cycle(Settings s, LocalDate today) {
        if (s.cycleStartOverride >= 0) {
            LocalDate start = LocalDate.ofEpochDay(s.cycleStartOverride);
            LocalDate next = start.plusDays(30);
            // 오버라이드 주기가 지나면 30일 단위로 굴린다.
            while (!today.isBefore(next)) {
                start = next;
                next = start.plusDays(30);
            }
            return new LocalDate[]{start, next};
        }
        int p = Math.max(1, Math.min(28, s.payday));
        LocalDate start = today.getDayOfMonth() >= p ? today.withDayOfMonth(p) : today.minusMonths(1).withDayOfMonth(p);
        return new LocalDate[]{start, start.plusMonths(1)};
    }

    public static State compute(Settings s, List<Record> records, long now, ZoneId zone) {
        State st = new State();
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        LocalDate[] c = cycle(s, today);
        st.cycleStart = c[0];
        st.nextPayday = c[1];
        st.cycleDays = (int) ChronoUnit.DAYS.between(c[0], c[1]);
        st.dayIndex = (int) ChronoUnit.DAYS.between(c[0], today) + 1;
        st.daysLeft = Math.max(1, (int) ChronoUnit.DAYS.between(today, c[1]));
        st.monthly = s.monthlyDiscretionary();
        st.b = st.cycleDays > 0 ? (double) st.monthly / st.cycleDays : st.monthly;
        st.foodBudget = s.foodBudget();

        for (Record r : records) {
            if (!r.outcome.spend) continue;
            LocalDate d = Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate();
            if (d.isBefore(c[0]) || !d.isBefore(c[1])) continue;
            st.foodSpent += r.food;
            if (d.isBefore(today)) st.spentBeforeToday += r.discretionary;
            else if (d.equals(today)) st.todaySpent += r.discretionary;
        }
        st.remainingAtDayStart = st.monthly - st.spentBeforeToday;
        st.q = (double) st.remainingAtDayStart / st.daysLeft;
        st.limit = Math.max(0, Math.round(Math.min(st.q, s.k * st.b)));
        st.available = st.limit - st.todaySpent;
        st.remainingNow = st.remainingAtDayStart - st.todaySpent;
        st.foodRemaining = st.foodBudget - st.foodSpent;
        return st;
    }
}
