package com.shimpyo.app.core;

import com.shimpyo.app.core.Enums.Intensity;
import com.shimpyo.app.core.Enums.Outcome;
import com.shimpyo.app.core.Enums.TargetApp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 홈·월말 리포트(REQ-11) 집계. 기준 기간은 급여일 주기. */
public class Stats {
    public int interventions;
    public int prevented;
    public long preventedSum;
    public Map<Outcome, Integer> byOutcome = new EnumMap<>(Outcome.class);
    public Map<Outcome, Long> byOutcomeSum = new EnumMap<>(Outcome.class);
    public Map<Intensity, int[]> byIntensity = new EnumMap<>(Intensity.class); // [개입 수, 방지 수]
    public int lateCount;
    public long lateSum;
    public int prevLateCount;
    public int reviewed;
    public int regret;

    /** 마음 돌림 비율 (0~100). 개입이 없으면 -1. */
    public int mindChangeRate() {
        return interventions == 0 ? -1 : Math.round(prevented * 100f / interventions);
    }

    public static Stats of(Settings s, List<Record> records, long now, ZoneId zone) {
        Stats st = new Stats();
        LocalDate today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        LocalDate[] c = BudgetEngine.cycle(s, today);
        LocalDate prevStart = s.cycleStartOverride >= 0 ? c[0].minusDays(30) : c[0].minusMonths(1);
        for (Intensity i : Intensity.values()) st.byIntensity.put(i, new int[2]);
        for (Outcome o : Outcome.values()) {
            st.byOutcome.put(o, 0);
            st.byOutcomeSum.put(o, 0L);
        }
        for (Record r : records) {
            LocalDate d = Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate();
            boolean inCycle = !d.isBefore(c[0]) && d.isBefore(c[1]);
            boolean inPrev = !d.isBefore(prevStart) && d.isBefore(c[0]);
            if (inPrev && r.lateNight && r.app == TargetApp.BAEMIN && r.outcome.spend) st.prevLateCount++;
            if (!inCycle) continue;
            st.byOutcome.put(r.outcome, st.byOutcome.get(r.outcome) + 1);
            st.byOutcomeSum.put(r.outcome, st.byOutcomeSum.get(r.outcome) + r.amount);
            if (r.isIntervention()) {
                st.interventions++;
                int[] v = st.byIntensity.get(r.intensity);
                v[0]++;
                if (r.outcome == Outcome.PREVENTED) {
                    v[1]++;
                    st.prevented++;
                    st.preventedSum += r.amount;
                }
            }
            if (r.lateNight && r.app == TargetApp.BAEMIN && r.outcome.spend) {
                st.lateCount++;
                st.lateSum += r.amount;
            }
            if (r.review != null) {
                st.reviewed++;
                if (r.review.contains("후회")) st.regret++;
            }
        }
        return st;
    }
}
