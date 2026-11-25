package com.shimpyo.app;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.shimpyo.app.core.BudgetEngine;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 리포트(S10): 이번 급여일 주기에
 * 1) 어느 카테고리에 많이 썼는지 (원형표 + 목록)
 * 2) 다음 날 "후회돼요"를 누른 결제와 그 카테고리
 * 3) 앞으로 줄이고 싶은 결제 (사용자가 고르고, 고른 것은 저장)
 */
final class ReportViews {
    private ReportViews() {}

    /**
     * 지출 카테고리. 색은 카테고리에 붙는다 (순위가 바뀌어도 같은 색).
     * 색 묶음은 색각 이상 대비 · 서로 구분 검사를 통과한 값 (노랑은 대비가 낮아 목록에 이름 · 금액을 함께 쓴다).
     */
    enum Category {
        DELIVERY("배달 음식", "#1F8A5E"),
        FASHION("옷 · 패션", "#3D8BD9"),
        LIVING("생활용품", "#E3A12F"),
        GROCERY("식료품", "#D9577A"),
        DINING("외식", "#8C7BD6"),
        OTHER("기타", "#B5BCB7");

        final String label;
        final int color;

        Category(String label, String hex) {
            this.label = label;
            this.color = Color.parseColor(hex);
        }

        static Category of(Record r) {
            if (r.app == TargetApp.BAEMIN) return DELIVERY;
            if (r.app == TargetApp.MUSINSA) return FASHION;
            if (r.app == TargetApp.COUPANG) return r.isFood || r.food > 0 ? GROCERY : LIVING;
            if (r.title != null && r.title.contains("외식")) return DINING;
            return OTHER;
        }

        static Category byLabel(String l) {
            for (Category c : values()) if (c.label.equals(l)) return c;
            return null;
        }
    }

    static final String LATE_NIGHT = "야식";

    // ================================================================== 데이터

    static final class Data {
        final Map<Category, Long> sums = new EnumMap<>(Category.class);
        long total;
        /** 다음 날 후회돼요를 누른 결제, 최근 순. */
        final List<Record> regretted = new ArrayList<>();
        final Map<Category, Integer> regretCount = new EnumMap<>(Category.class);
        final Map<Category, Long> regretSum = new EnumMap<>(Category.class);
        long lateSum;
        int lateCount;

        /** 이 데이터의 급여 주기 [시작, 끝) 와 이름("10월" = 시작한 달). */
        LocalDate[] cycle;
        String monthLabel;

        /** 이번 주기(진행 중). */
        static Data of(Settings s, List<Record> records, long now, ZoneId zone) {
            return of(s, records, cycleAt(s, Instant.ofEpochMilli(now).atZone(zone).toLocalDate(), 0), zone);
        }

        static Data of(Settings s, List<Record> records, LocalDate[] cycle, ZoneId zone) {
            Data d = new Data();
            d.cycle = cycle;
            d.monthLabel = cycle[0].getMonthValue() + "월";
            for (Record r : records) {
                LocalDate day = Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate();
                if (day.isBefore(cycle[0]) || !day.isBefore(cycle[1])) continue;
                if (!r.outcome.spend) continue;
                Category c = Category.of(r);
                d.sums.put(c, d.sums.getOrDefault(c, 0L) + r.amount);
                d.total += r.amount;
                if (r.lateNight && r.app == TargetApp.BAEMIN) { d.lateCount++; d.lateSum += r.amount; }
                if (r.review != null && r.review.contains("후회")) {
                    d.regretted.add(r);
                    d.regretCount.put(c, d.regretCount.getOrDefault(c, 0) + 1);
                    d.regretSum.put(c, d.regretSum.getOrDefault(c, 0L) + r.amount);
                }
            }
            Collections.sort(d.regretted, (a, b) -> Long.compare(b.ts, a.ts));
            return d;
        }

        /** 많이 쓴 순서 (0원 카테고리는 뺀다). 기타는 금액과 상관없이 맨 뒤. */
        List<Category> ranked() {
            List<Category> l = new ArrayList<>(sums.keySet());
            Collections.sort(l, (a, b) -> {
                if ((a == Category.OTHER) != (b == Category.OTHER)) return a == Category.OTHER ? 1 : -1;
                return Long.compare(sums.get(b), sums.get(a));
            });
            return l;
        }

        int percent(Category c) {
            return total == 0 ? 0 : (int) Math.round(100.0 * sums.getOrDefault(c, 0L) / total);
        }
    }

    /**
     * offset번째 급여 주기: 0 = 이번(진행 중), -1 = 지난달, -2 = 지지난달 …
     * 급여일 주기는 한 달씩, 테스트용 시작일 지정 주기는 30일씩 거슬러 간다 (BudgetEngine.cycle과 같은 규칙).
     */
    static LocalDate[] cycleAt(Settings s, LocalDate today, int offset) {
        LocalDate[] c = BudgetEngine.cycle(s, today);
        for (int i = 0; i < -offset; i++) {
            LocalDate start = s.cycleStartOverride >= 0 ? c[0].minusDays(30) : c[0].minusMonths(1);
            c = new LocalDate[]{start, c[0]};
        }
        return c;
    }

    /** 리포트를 처음 열 때 보여 줄 주기: 지난달에 쓴 기록이 있으면 지난달, 없으면(첫 달) 이번 달. */
    static int defaultOffset(Settings s, List<Record> records, LocalDate today, ZoneId zone) {
        Data prev = Data.of(s, records, cycleAt(s, today, -1), zone);
        return prev.total > 0 || !prev.regretted.isEmpty() ? -1 : 0;
    }

    /** 이 주기보다 앞선 기록이 있으면 더 거슬러 볼 수 있다. */
    static boolean hasEarlier(List<Record> records, LocalDate[] cycle, ZoneId zone) {
        for (Record r : records) {
            if (Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate().isBefore(cycle[0])) return true;
        }
        return false;
    }

    // ================================================================== 화면

    static void build(Context c, LinearLayout out, Store store, Data d, ZoneId zone) {
        Ui.stack(out, categoryCard(c, d), Ui.Space.BLOCK);
        Ui.stack(out, regretCard(c, d, zone), Ui.Space.BLOCK);
        Ui.stack(out, reduceCard(c, store, d), Ui.Space.BLOCK);
    }

    /** 1) 카테고리별 지출: 원형표 + "전체" + 카테고리 줄 (점 · 이름 · 비율 · 금액). */
    private static View categoryCard(Context c, Data d) {
        LinearLayout card = Ui.card(c);
        card.addView(Ui.caption(c, "어디에 많이 썼나요"));
        List<Category> ranked = d.ranked();
        if (ranked.isEmpty()) {
            Ui.stack(card, Ui.emptyState(c, "이 달 결제가 쌓이면 카테고리별로 보여 드려요."), Ui.Space.ITEM);
            return card;
        }
        // 원형표 조각은 카테고리 순서(고정)로 그리고, 목록은 많이 쓴 순서로 둔다.
        Category[] all = Category.values();
        long[] vals = new long[all.length];
        int[] cols = new int[all.length];
        for (int i = 0; i < all.length; i++) {
            vals[i] = d.sums.getOrDefault(all[i], 0L);
            cols[i] = all[i].color;
        }
        // 가운데: 가장 많이 쓴 카테고리. 기타(분류 안 된 계좌 지출)가 가장 크면 카테고리 대신 그달 전체 지출.
        Category largest = ranked.get(0);
        for (Category cat : ranked) if (d.sums.get(cat) > d.sums.get(largest)) largest = cat;
        DonutChart chart = largest == Category.OTHER
                ? new DonutChart(c, vals, cols, d.monthLabel + " 지출", null, Money.won(d.total))
                : new DonutChart(c, vals, cols, "가장 많이 쓴 곳", largest.label, d.percent(largest) + "%");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(c, 200), Ui.dp(c, 200));
        lp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        lp.topMargin = Ui.dp(c, Ui.Space.WIDE);
        card.addView(chart, lp);

        LinearLayout totalRow = Ui.h(c);
        totalRow.addView(Ui.text(c, "전체", Ui.Font.SMALL, Ui.MUTED, Ui.REGULAR), Ui.weight1());
        totalRow.addView(Ui.text(c, Money.won(d.total), Ui.Font.SMALL, Ui.MUTED, Ui.REGULAR));
        Ui.stack(card, totalRow, Ui.Space.WIDE);
        Ui.stack(card, Ui.divider(c), Ui.Space.ITEM);
        for (Category cat : ranked) {
            Ui.stack(card, categoryRow(c, cat.color, cat.label, d.percent(cat) + "%", Money.won(d.sums.get(cat))), Ui.Space.ROW);
        }
        return card;
    }

    /** 점 · 이름 · 회색 비율 · 오른쪽 굵은 금액. 글자는 글자 색, 점만 카테고리 색. */
    private static LinearLayout categoryRow(Context c, int color, String name, String pct, String amount) {
        LinearLayout r = Ui.h(c);
        r.setGravity(android.view.Gravity.CENTER_VERTICAL);
        View dot = new View(c);
        dot.setBackground(Ui.circle(color));
        r.addView(dot, Ui.square(c, Ui.Size.LIST_DOT + 2));
        TextView n = Ui.text(c, name, Ui.Font.BODY, Ui.TEXT, Ui.MEDIUM);
        LinearLayout.LayoutParams nl = Ui.wrap();
        nl.leftMargin = Ui.dp(c, Ui.Space.ROW);
        r.addView(n, nl);
        TextView p = Ui.text(c, pct, Ui.Font.BODY, Ui.SUBTLE, Ui.REGULAR);
        LinearLayout.LayoutParams pl = Ui.weight1();
        pl.leftMargin = Ui.dp(c, Ui.Space.TIGHT);
        r.addView(p, pl);
        r.addView(Ui.text(c, amount, Ui.Font.BODY, Ui.TEXT, Ui.BOLD));
        return r;
    }

    /** 2) 후회한 결제: 카테고리 요약 태그 + 결제 줄. */
    private static View regretCard(Context c, Data d, ZoneId zone) {
        LinearLayout card = Ui.card(c);
        card.addView(Ui.caption(c, "다음 날 후회한 결제"));
        if (d.regretted.isEmpty()) {
            Ui.stack(card, Ui.bodySmall(c, "다음 날 '후회돼요'를 누른 결제가 여기 남아요."), Ui.Space.ITEM);
            return card;
        }
        List<Category> cats = new ArrayList<>(d.regretCount.keySet());
        Collections.sort(cats, (a, b) -> Long.compare(d.regretSum.get(b), d.regretSum.get(a)));
        Category worst = cats.get(0);
        Ui.stack(card, Ui.text(c, worst.label + "에서 후회가 가장 많았어요", Ui.Font.HEADING, Ui.TEXT, Ui.BOLD), Ui.Space.ITEM);
        FlowLayout tags = Ui.tagFlow(c);
        for (Category cat : cats) {
            tags.addView(Ui.smallTag(c, cat.label + " " + d.regretCount.get(cat) + "건 · " + Money.won(d.regretSum.get(cat)), Ui.Tone.DANGER));
        }
        Ui.stack(card, tags, Ui.Space.ITEM);
        for (Record r : d.regretted) {
            LocalDate day = Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate();
            String sub = day.getMonthValue() + "/" + day.getDayOfMonth()
                    + (r.chip != null && !r.chip.isEmpty() ? " · \"" + r.chip + "\"" : "");
            LinearLayout row = Ui.listRow(c, AppIcons.icon(c, r.app, Ui.Size.APP_ICON), r.title, sub, Money.won(r.amount), null);
            Ui.stack(card, row, Ui.Space.STACK);
        }
        return card;
    }

    /** 3) 앞으로 줄이고 싶은 결제: 여러 개 고를 수 있고, 고르면 바로 저장. 고른 것은 보고 있는 달에 쓴 돈과 함께 보여 준다. */
    private static View reduceCard(Context c, Store store, Data d) {
        LinearLayout box = Ui.tintBox(c, Ui.BRAND_LIGHT, Ui.Pad.CARD, Ui.Pad.CARD);
        box.addView(Ui.text(c, "앞으로", Ui.Font.CAPTION, Ui.GREEN, Ui.BOLD));
        Ui.stack(box, Ui.text(c, "어떤 결제를 줄이고 싶어요?", Ui.Font.TITLE, Ui.TEXT, Ui.BOLD), Ui.Space.ITEM);
        if (!d.regretted.isEmpty()) {
            Ui.stack(box, Ui.text(c, "후회가 남은 것부터 · 여러 개 골라도 돼요", Ui.Font.SMALL, Ui.GREEN, Ui.MEDIUM), Ui.Space.LINE);
        }

        // 후보: 후회한 카테고리 → 많이 쓴 카테고리 → 야식, 이미 고른 것도 빠지지 않게
        Set<String> options = new LinkedHashSet<>();
        List<Category> regretCats = new ArrayList<>(d.regretCount.keySet());
        Collections.sort(regretCats, (a, b) -> Long.compare(d.regretSum.get(b), d.regretSum.get(a)));
        for (Category cat : regretCats) options.add(cat.label);
        for (Category cat : d.ranked()) if (cat != Category.OTHER) options.add(cat.label);
        options.add(LATE_NIGHT);
        Set<String> chosen = new LinkedHashSet<>(store.settings().reduceTargetList());
        options.addAll(chosen);

        LinearLayout summary = Ui.v(c);
        List<TextView> chips = new ArrayList<>();
        for (String o : options) {
            TextView t = Ui.accentChip(c, o, chosen.contains(o), null);
            t.setOnClickListener(v -> {
                if (!chosen.remove(o)) chosen.add(o);
                Ui.setAccentChipSelected(c, t, chosen.contains(o));
                Settings st = store.settings();
                st.setReduceTargets(new ArrayList<>(chosen));
                store.saveSettings(st);
                fillSummary(c, summary, chosen, d);
            });
            chips.add(t);
        }
        Ui.stack(box, Ui.chipFlow(c, chips), Ui.Space.BLOCK);
        fillSummary(c, summary, chosen, d);
        Ui.stack(box, summary, Ui.Space.BLOCK);
        return box;
    }

    /** 고른 결제마다 "그달 N원" 한 줄 (다음 달과 비교할 기준). */
    private static void fillSummary(Context c, LinearLayout summary, Set<String> chosen, Data d) {
        summary.removeAllViews();
        if (chosen.isEmpty()) {
            summary.addView(Ui.text(c, "고르면 " + d.monthLabel + "에 쓴 돈을 기준으로 남겨 둘게요.", Ui.Font.SMALL, Ui.MUTED, Ui.REGULAR));
            return;
        }
        LinearLayout list = Ui.v(c);
        list.setBackground(Ui.round(c, Ui.WHITE, Ui.Radius.INPUT));
        Ui.pad(list, c, Ui.Pad.BOX_H, Ui.Pad.BOX_V);
        list.addView(Ui.text(c, "줄이기로 한 결제 · " + d.monthLabel + "에 쓴 돈", Ui.Font.CAPTION, Ui.MUTED, Ui.REGULAR));
        for (String o : chosen) {
            long sum;
            if (LATE_NIGHT.equals(o)) sum = d.lateSum;
            else {
                Category cat = Category.byLabel(o);
                sum = cat == null ? 0 : d.sums.getOrDefault(cat, 0L);
            }
            String right = LATE_NIGHT.equals(o) ? d.lateCount + "번 · " + Money.won(sum) : Money.won(sum);
            Ui.stack(list, Ui.kv(c, o, right, false), Ui.Space.ITEM);
        }
        summary.addView(list, Ui.fillW());
    }
}
