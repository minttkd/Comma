package com.shimpyo.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.shimpyo.app.core.BudgetEngine;
import com.shimpyo.app.core.Enums.Judgment;
import com.shimpyo.app.core.Enums.Outcome;
import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.KeywordJudge;
import com.shimpyo.app.core.Money;
import com.shimpyo.app.core.Record;
import com.shimpyo.app.core.Settings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 리포트(S10). 맨 위는 "이번 달 무엇에 가장 많이 썼는지"(카테고리별 지출), 그다음 "다음 달엔",
 * "쉼표로 아낀 돈"(주별 그래프), 맨 아래는 접어 둔 "돌아본 기록"(가치 회고)이다. 순서는 render() 참고.
 *
 * 가치 회고: 숫자 대신 "내가 무엇에 돈을 쓰고, 다음 날 어떻게 느꼈는지"로 돌아보게 한다.
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

    /** 이번 급여일 주기 기록에서 뽑은 묶음. */
    static final class Data {
        Settings settings;
        LocalDate cycleStart, cycleEnd, today;
        /** 이번 주기 식비 실측: 마트(계좌 내역) · 쿠팡 식료품. */
        long martSpent, coupangFoodSpent;
        /** 실제로 돈이 나간 기록 전체 (카테고리별 지출), 최근 순. */
        final List<Record> spent = new ArrayList<>();
        /** 그중 다음 날 '후회돼요'라고 답한 결제, 최근 순. */
        final List<Record> regretted = new ArrayList<>();
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
            d.settings = s;
            d.cycleStart = cycle[0];
            d.cycleEnd = cycle[1];
            d.today = today;
            for (Record r : records) {
                LocalDate day = Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate();
                if (day.isBefore(cycle[0]) || !day.isBefore(cycle[1])) continue;
                if (r.outcome.spend) {
                    d.spent.add(r);
                    if (r.review != null && r.review.contains("후회")) d.regretted.add(r);
                    if (r.app == null) d.martSpent += r.food;
                    else if (r.app == TargetApp.COUPANG) d.coupangFoodSpent += r.food;
                }
                if (r.isIntervention()) {
                    d.talked.add(r);
                    if (r.outcome == Outcome.PREVENTED) d.stoppedList.add(r);
                }
                if (r.outcome == Outcome.REASON_PAY || r.outcome == Outcome.DELIBERATE_PAY) d.paidAfterTalk.add(r);
                if (r.lateNight && r.app == TargetApp.BAEMIN && r.outcome.spend) d.lateCount++;
            }
            Collections.sort(d.spent, (a, b) -> Long.compare(b.ts, a.ts));
            Collections.sort(d.regretted, (a, b) -> Long.compare(b.ts, a.ts));
            Collections.sort(d.paidAfterTalk, (a, b) -> Long.compare(b.ts, a.ts));
            Collections.sort(d.talked, (a, b) -> Long.compare(b.ts, a.ts));
            Collections.sort(d.stoppedList, (a, b) -> Long.compare(b.amount, a.amount));
            return d;
        }
    }

    // ================================================================== 카테고리

    /**
     * 무엇에 돈을 썼는지. 대상 앱은 앱과 품목 판정으로 (판정이 없으면 상품명 키워드로),
     * 계좌 내역은 가맹점 이름으로 나눈다.
     */
    enum Category {
        // 색은 카테고리마다 고정 (순위가 바뀌어도 같은 색). dataviz 기본 범주 팔레트 8색 순서 그대로, 기타는 회색.
        // 어두운 화면은 같은 색상의 어두운 바탕용 단계 (palette.md Dark 열).
        DELIVERY("배달 음식", 0xFF2A78D6, 0xFF3987E5),
        EATING_OUT("외식", 0xFFEB6834, 0xFFD95926),
        CAFE("카페", 0xFF1BAF7A, 0xFF199E70),
        CONVENIENCE("편의점", 0xFFEDA100, 0xFFC98500),
        GROCERY("식료품", 0xFFE87BA4, 0xFFD55181),
        DAILY("생활용품", 0xFF008300, 0xFF0CA30C),
        FASHION("옷·잡화", 0xFF6250D6, 0xFF9085E9),
        SHOPPING("그 밖의 쇼핑", 0xFFE34948, 0xFFE66767),
        ETC("기타", 0xFFA9B3AB, 0xFF5E6862);

        final String name;
        private final int light, darkColor;

        Category(String name, int light, int darkColor) {
            this.name = name;
            this.light = light;
            this.darkColor = darkColor;
        }

        /** 지금 화면 모드(Ui.dark)에 맞는 카테고리 색. */
        int color() {
            return Ui.dark ? darkColor : light;
        }

        static final String[] CAFE_WORDS = {"카페", "커피", "스타벅스", "이디야", "메가커피", "투썸", "빽다방", "컴포즈"};
        static final String[] CONVENIENCE_WORDS = {"편의점", "GS25", "CU ", "CU·", "세븐일레븐", "이마트24", "미니스톱"};
        static final String[] EATING_OUT_WORDS = {"외식", "식당", "분식", "김밥", "국밥", "한식", "중식", "일식", "고깃집", "버거", "치킨", "피자", "학식"};

        static Category of(Record r) {
            if (r.app == TargetApp.BAEMIN) return DELIVERY;
            if (r.food > 0 || r.isFood) return GROCERY;
            if (r.app == null) return ofMerchant(r.title);
            if (r.app == TargetApp.MUSINSA) return FASHION;
            boolean need = r.judgment == Judgment.NEED || (r.judgment == null && KeywordJudge.looksNeed(r.title));
            return need ? DAILY : SHOPPING;
        }

        /** 계좌 내역의 가맹점 이름으로. 모르는 가맹점은 기타. */
        static Category ofMerchant(String title) {
            String t = title == null ? "" : title;
            if (KeywordJudge.containsAny(t, CAFE_WORDS)) return CAFE;
            if (KeywordJudge.containsAny(t, CONVENIENCE_WORDS)) return CONVENIENCE;
            if (KeywordJudge.containsAny(t, EATING_OUT_WORDS)) return EATING_OUT;
            return ETC;
        }
    }

    /** 카테고리 하나의 합계. */
    static final class CategorySum {
        final Category cat;
        long sum;
        int count;

        CategorySum(Category cat) {
            this.cat = cat;
        }
    }

    /** 기록들을 카테고리별로, 금액 큰 순 (기타는 맨 뒤). */
    static List<CategorySum> categories(List<Record> records) {
        Map<Category, CategorySum> m = new EnumMap<>(Category.class);
        for (Record r : records) {
            Category k = Category.of(r);
            CategorySum g = m.get(k);
            if (g == null) m.put(k, g = new CategorySum(k));
            g.sum += r.amount;
            g.count++;
        }
        List<CategorySum> l = new ArrayList<>(m.values());
        Collections.sort(l, (a, b) -> {
            boolean ae = a.cat == Category.ETC, be = b.cat == Category.ETC;
            if (ae != be) return ae ? 1 : -1;
            return Long.compare(b.sum, a.sum);
        });
        return l;
    }

    // ================================================================== 원그래프

    /** 원그래프 조각은 큰 순 5개까지. 나머지는 "그 밖" 한 조각으로 묶는다 (조각이 많으면 읽기 어렵다). */
    static final int PIE_MAX = 5;

    /** 원그래프 조각 하나. cat이 null이면 여러 카테고리를 묶은 "그 밖". */
    static final class Slice {
        final Category cat;
        final String name;
        final int color;
        final long sum;

        Slice(Category cat, String name, int color, long sum) {
            this.cat = cat;
            this.name = name;
            this.color = color;
            this.sum = sum;
        }
    }

    static List<Slice> slices(List<CategorySum> cats) {
        List<Slice> l = new ArrayList<>();
        int keep = cats.size() <= PIE_MAX + 1 ? cats.size() : PIE_MAX;
        long rest = 0;
        for (int i = 0; i < cats.size(); i++) {
            CategorySum g = cats.get(i);
            if (i < keep) l.add(new Slice(g.cat, g.cat.name, g.cat.color(), g.sum));
            else rest += g.sum;
        }
        if (rest > 0) l.add(new Slice(null, "그 밖", Category.ETC.color(), rest));
        return l;
    }

    /**
     * 도넛 모양 원그래프. 조각을 누르면 그 조각이 커진 채로 남고(나머지는 흐려짐) 가운데에 이름 · 금액 · 비율이 나온다.
     * 같은 조각이나 가운데를 다시 누르면 풀린다. 마우스가 있으면 올리기만 해도 보인다(hover, 손을 떼면 누른 상태로 돌아감).
     * 다른 화면으로 넘어가지는 않는다 (그래프 아래 [자세히 보기] 버튼이 따로 있다).
     */
    static final class PieChart extends View {
        private final List<Slice> slices;
        private final long total;
        private final String title;
        /** 누른 조각 (-1이면 없음). */
        private int selected = -1;
        /** 마우스를 올린 조각 (-1이면 없음). 있으면 selected보다 먼저 보인다. */
        private int hovered = -1;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gap = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hole = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF oval = new RectF();

        /** hitTest 결과: 그래프 밖 / 가운데 구멍. 0 이상이면 조각 번호. */
        private static final int OUTSIDE = -1, CENTER = -2;

        PieChart(Context c, List<Slice> slices, String title) {
            super(c);
            this.slices = slices;
            this.title = title;
            long t = 0;
            for (Slice s : slices) t += s.sum;
            this.total = t;
            gap.setStyle(Paint.Style.STROKE);
            gap.setColor(Ui.WHITE);
            gap.setStrokeWidth(dp(2));
            hole.setColor(Ui.WHITE);
            small.setTextAlign(Paint.Align.CENTER);
            small.setTextSize(sp(Ui.Font.MICRO));
            small.setColor(Ui.MUTED);
            big.setTextAlign(Paint.Align.CENTER);
            big.setTextSize(sp(Ui.Font.SMALL));
            big.setColor(Ui.TEXT);
            big.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            StringBuilder desc = new StringBuilder(title + " " + Money.won(total) + ": ");
            for (Slice s : slices) desc.append(s.name).append(' ').append(pct(s.sum)).append("%, ");
            setContentDescription(desc + "조각을 누르면 크게 보여요");
        }

        private float sp(float v) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
        }

        private float dp(float v) {
            return v * getResources().getDisplayMetrics().density;
        }

        private int pct(long v) {
            return total == 0 ? 0 : Math.round(v * 100f / total);
        }

        @Override
        protected void onMeasure(int w, int h) {
            int width = MeasureSpec.getSize(w);
            setMeasuredDimension(width, Math.min(width, (int) dp(180)));
        }

        private float radius() {
            return Math.min(getWidth(), getHeight()) / 2f - dp(10); // 고른 조각이 커질 자리를 남긴다
        }

        private int hitTest(float x, float y) {
            float dx = x - getWidth() / 2f, dy = y - getHeight() / 2f;
            double dist = Math.sqrt(dx * dx + dy * dy);
            float r = radius();
            if (dist > r + dp(10)) return OUTSIDE;
            if (dist < r * 0.58f || total == 0) return CENTER;
            double deg = (Math.toDegrees(Math.atan2(dy, dx)) + 90 + 360) % 360; // 12시 방향이 0도
            double start = 0;
            for (int i = 0; i < slices.size(); i++) {
                double sweep = 360.0 * slices.get(i).sum / total;
                if (deg < start + sweep) return i;
                start += sweep;
            }
            return slices.size() - 1;
        }

        /** 지금 크게 보일 조각: 마우스를 올린 조각, 없으면 누른 조각. */
        private int shown() {
            return hovered >= 0 ? hovered : selected;
        }

        @Override
        public boolean onHoverEvent(android.view.MotionEvent e) {
            int h = e.getActionMasked() == android.view.MotionEvent.ACTION_HOVER_EXIT ? -1 : hitTest(e.getX(), e.getY());
            h = Math.max(-1, h);
            if (h != hovered) {
                hovered = h;
                invalidate();
            }
            return true;
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent e) {
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    return true; // 손을 뗄 때 고른다 (그 사이 스크롤하면 CANCEL로 무시)
                case android.view.MotionEvent.ACTION_UP: {
                    int hit = hitTest(e.getX(), e.getY());
                    if (hit == OUTSIDE) return true;
                    selected = hit >= 0 && hit != selected ? hit : -1; // 같은 조각 · 가운데를 다시 누르면 풀린다
                    performClick();
                    invalidate();
                    return true;
                }
            }
            return super.onTouchEvent(e);
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        @Override
        protected void onDraw(Canvas cv) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = radius();
            if (total == 0) {
                fill.setColor(Ui.LINE);
                fill.setAlpha(255);
                cv.drawCircle(cx, cy, r, fill);
            } else {
                float start = -90;
                for (int i = 0; i < slices.size(); i++) {
                    Slice s = slices.get(i);
                    float sweep = 360f * s.sum / total;
                    int on = shown();
                    float rr = i == on ? r + dp(8) : r;
                    oval.set(cx - rr, cy - rr, cx + rr, cy + rr);
                    fill.setColor(s.color);
                    fill.setAlpha(on >= 0 && i != on ? 90 : 255);
                    cv.drawArc(oval, start, sweep, true, fill);
                    if (slices.size() > 1) cv.drawArc(oval, start, sweep, true, gap); // 조각 사이 흰 틈
                    start += sweep;
                }
            }
            cv.drawCircle(cx, cy, r * 0.58f, hole);

            // 가운데 글자: 고른 조각 또는 제목 · 합계
            String top, mid, bottom;
            if (shown() >= 0) {
                Slice s = slices.get(shown());
                top = s.name;
                mid = Money.won(s.sum);
                bottom = pct(s.sum) + "%";
            } else {
                top = title;
                mid = total == 0 ? "없어요" : Money.won(total);
                bottom = ""; // 사용법은 카드 머리말에 있다 (가운데 구멍이 좁아 한 줄 더 넣으면 넘친다)
            }
            float line = sp(16);
            cv.drawText(top, cx, cy - line + sp(4), small);
            cv.drawText(mid, cx, cy + sp(5), big);
            cv.drawText(bottom, cx, cy + line + sp(4), small);
        }
    }

    // ================================================================== 아낀 돈 (주별)

    /** 급여일 기준 주차 수 (보통 4~5). */
    static int weekCount(Data d) {
        long days = ChronoUnit.DAYS.between(d.cycleStart, d.cycleEnd);
        return (int) Math.max(1, (days + 6) / 7);
    }

    /** 주차별 멈춘 결제 금액 (0번 = 1주차). */
    static long[] savedByWeek(Data d, ZoneId zone) {
        long[] w = new long[weekCount(d)];
        for (Record r : d.stoppedList) {
            LocalDate day = Instant.ofEpochMilli(r.ts).atZone(zone).toLocalDate();
            int i = (int) (ChronoUnit.DAYS.between(d.cycleStart, day) / 7);
            if (i >= 0 && i < w.length) w[i] += r.amount;
        }
        return w;
    }

    /** "12.9만" · "4,500" 처럼 짧게 (막대 위 값). */
    static String shortWon(long v) {
        if (v < 10_000) return Money.num(v);
        long tenth = Math.round(v / 1_000.0);
        return (tenth % 10 == 0 ? String.valueOf(tenth / 10) : (tenth / 10) + "." + (tenth % 10)) + "만";
    }

    /**
     * 주별 막대 그래프 (한 가지 색). 막대 위에 값, 아래에 주차.
     * 아직 오지 않은 주는 주차 글자만 흐리게. 값이 0인 주는 막대 없이 바닥선만.
     */
    static final class WeekBars extends View {
        private final long[] values;
        private final int current; // 이번 주 (0부터)
        private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint valueText = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint labelText = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();

        WeekBars(Context c, long[] values, int current) {
            super(c);
            this.values = values;
            this.current = current;
            bar.setColor(Ui.GREEN);
            base.setColor(Ui.LINE);
            valueText.setColor(Ui.TEXT);
            valueText.setTextAlign(Paint.Align.CENTER);
            valueText.setTextSize(sp(Ui.Font.CAPTION));
            valueText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            labelText.setTextAlign(Paint.Align.CENTER);
            labelText.setTextSize(sp(Ui.Font.CAPTION));
            StringBuilder desc = new StringBuilder("주별 아낀 돈: ");
            for (int i = 0; i < values.length; i++) desc.append(i + 1).append("주차 ").append(Money.won(values[i])).append(i < values.length - 1 ? ", " : "");
            setContentDescription(desc);
        }

        private float sp(float v) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
        }

        private float dp(float v) {
            return v * getResources().getDisplayMetrics().density;
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(MeasureSpec.getSize(w), (int) dp(140));
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            float labelH = dp(22), valueH = dp(20);
            float baseY = h - labelH;
            float top = valueH;
            long max = 1;
            for (long v : values) max = Math.max(max, v);
            float slot = w / values.length;
            float bw = Math.min(dp(32), slot * 0.5f);
            float r = dp(4);
            cv.drawRect(0, baseY, w, baseY + dp(1), base);
            for (int i = 0; i < values.length; i++) {
                float cx = slot * i + slot / 2f;
                boolean future = i > current;
                labelText.setColor(i == current ? Ui.TEXT : future ? Ui.SUBTLE : Ui.MUTED);
                labelText.setTypeface(Typeface.create(Typeface.DEFAULT, i == current ? Typeface.BOLD : Typeface.NORMAL));
                cv.drawText(i == current ? "이번 주" : (i + 1) + "주차", cx, h - dp(5), labelText);
                if (values[i] <= 0) continue;
                float bh = Math.max(dp(4), (baseY - top) * values[i] / (float) max);
                rect.set(cx - bw / 2f, baseY - bh, cx + bw / 2f, baseY);
                path.reset();
                path.addRoundRect(rect, new float[]{r, r, r, r, 0, 0, 0, 0}, Path.Direction.CW);
                cv.drawPath(path, bar);
                cv.drawText(shortWon(values[i]), cx, baseY - bh - dp(6), valueText);
            }
        }
    }

    // ================================================================== 미리 떼어 둔 돈 (평균과 비교)

    /**
     * 미리 떼어 둔 항목 하나: 평균(= 떼어 둔 금액, 최근 3개월 평균으로 정함)과 이번 달 실제.
     * 식비처럼 한 달 내내 나가는 항목은 "오늘까지 평균 속도"와 비교한다 (paced).
     */
    static final class SetAside {
        final String name;
        final long average;
        /** 이번 달 실제 (아직 청구 전이면 -1). */
        final long actual;
        /** 비교 기준: paced면 평균 × 지난 날 비율, 아니면 평균 그대로. */
        final long expected;
        final boolean paced;

        SetAside(String name, long average, long actual, long expected, boolean paced) {
            this.name = name;
            this.average = average;
            this.actual = actual;
            this.expected = expected;
            this.paced = paced;
        }

        boolean known() {
            return actual >= 0;
        }

        long diff() {
            return actual - expected;
        }
    }

    static List<SetAside> setAsides(Data d, BackendSlots back) {
        Settings s = d.settings;
        List<SetAside> l = new ArrayList<>();
        String[] names = {"월세·관리비", "공과금", "통신비", "교통비", "구독"};
        long[] avg = {s.rent, s.utilities, s.phone, s.transport, s.subscriptions};
        for (int i = 0; i < names.length; i++) {
            long act = back.fixedThisMonth != null && i < back.fixedThisMonth.length ? back.fixedThisMonth[i] : -1;
            l.add(new SetAside(names[i], avg[i], act, avg[i], false));
        }
        long days = Math.max(1, ChronoUnit.DAYS.between(d.cycleStart, d.cycleEnd));
        long passed = Math.min(days, ChronoUnit.DAYS.between(d.cycleStart, d.today) + 1);
        l.add(new SetAside("마트 장보기", s.martBudget, d.martSpent, Money.round100(s.martBudget * passed / (double) days), true));
        l.add(new SetAside("쿠팡 식료품", s.coupangFoodBudget, d.coupangFoodSpent, Money.round100(s.coupangFoodBudget * passed / (double) days), true));
        return l;
    }

    /**
     * 평균과의 차이 막대 (가운데 0선에서 양쪽으로). 더 나가면 오른쪽 빨강, 덜 나가면 왼쪽 초록.
     * 색만으로 구분하지 않도록 옆 글자에 +/− 부호를 같이 쓴다.
     */
    static final class DiffBar extends View {
        private final float ratio; // -1 ~ 1
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mid = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        DiffBar(Context c, float ratio) {
            super(c);
            this.ratio = Math.max(-1f, Math.min(1f, ratio));
            fill.setColor(ratio > 0 ? Ui.RED : Ui.GREEN);
            track.setColor(Ui.FILL);
            mid.setColor(Ui.MUTED);
        }

        private float dp(float v) {
            return v * getResources().getDisplayMetrics().density;
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(MeasureSpec.getSize(w), (int) dp(14));
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            float bh = dp(Ui.Size.BAR), top = (h - bh) / 2f, r = dp(4), cx = w / 2f;
            rect.set(0, top, w, top + bh);
            cv.drawRoundRect(rect, r, r, track);
            if (Math.abs(ratio) > 0.001f) {
                float len = Math.max(dp(4), Math.abs(ratio) * (w / 2f));
                if (ratio > 0) rect.set(cx, top, cx + len, top + bh);
                else rect.set(cx - len, top, cx, top + bh);
                cv.drawRoundRect(rect, r, r, fill);
            }
            cv.drawRect(cx - dp(1), 0, cx + dp(1), h, mid);
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
        TALKED("대화 끝에", "쉼표와 이야기하고도 [그래도 살게요]", "대화하고도 사는 것"),
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

    /** 리포트에서 다른 화면으로 넘어가기 (MainActivity가 채운다). */
    interface Nav {
        /** 결제(regret=false) 또는 후회(regret=true) 자세히. focus는 원그래프에서 고른 카테고리 (없으면 null). */
        void openSpend(boolean regret, Category focus);

        /** 돌아본 기록 자세히. */
        void openHistory();
    }

    /**
     * 리포트 순서.
     * 1) 결제 vs 후회 (강조 카드: 비율 · 금액) + 결제 · 후회 원그래프 (누르면 결제 / 후회 자세히 화면)
     * 2) 다음 달엔 (줄이고 싶은 결제 고르기)
     * 3) 쉼표로 아낀 돈 (주별 막대 그래프)
     * 4) 돌아본 기록: 한 줄 요약 + 오른쪽 위 [더보기 ›] → 돌아본 기록 화면
     * 5) 미리 떼어 둔 돈(고정지출 5항목 · 식비 예산)이 평균과 얼마나 다른지 (가운데가 평균인 막대)
     * "다음 달엔" · "멈춘 결제" 답은 화면만 (답 저장은 기록 구조 · 후회율 계산과 엮여 차민상과 합의 후 연결).
     */
    static void render(Context c, LinearLayout out, Data d, ZoneId zone, Nav nav) {
        Map<Value, ValueGroup> groups = valueGroups(d);
        Ui.stack(out, regretHero(c, d), Ui.Space.BLOCK);
        if (!d.spent.isEmpty()) Ui.stack(out, piesCard(c, d, nav), Ui.Space.BLOCK);
        Ui.stack(out, nextMonthCard(c, d, groups), Ui.Space.BLOCK);
        Ui.stack(out, savedCard(c, d, zone), Ui.Space.BLOCK);
        Ui.stack(out, historyCard(c, d, groups, nav), Ui.Space.BLOCK);
        Ui.stack(out, setAsideCard(c, d), Ui.Space.LARGE);
    }

    private static long sum(List<Record> l) {
        long t = 0;
        for (Record r : l) t += r.amount;
        return t;
    }

    /** 1) 결제 vs 후회: 이번 달 결제 중 다음 날 '후회돼요'라고 답한 금액과 비율. */
    private static View regretHero(Context c, Data d) {
        long total = sum(d.spent), reg = sum(d.regretted);
        if (total == 0) {
            return Ui.heroSentence(c, "이번 달 결제", "아직 이번 달\n결제 기록이 없어요",
                    "결제하면 무엇에 썼고 무엇을 후회했는지 모아서 보여 드려요");
        }
        int pct = Math.round(reg * 100f / total);
        String sentence = reg == 0 ? Money.won(total) + " 결제했고\n후회한 결제는 없어요"
                : "결제 " + Money.won(total) + " 중\n" + Money.won(reg) + "은 후회했어요";
        LinearLayout meter = Ui.v(c);
        // 후회 몫: 밝은 화면은 연빨강, 어두운 화면은 연빨강 바탕이 짙어서 안 보이므로 빨강(어두운 화면에선 밝은 분홍)
        meter.addView(Ui.bar(c, reg / (float) total, Ui.dark ? Ui.RED : Ui.RED_BG, Color.argb(60, 255, 255, 255)), Ui.fillW());
        return Ui.heroSentence(c, "이번 달 결제 vs 후회", sentence,
                "다음 날 '후회돼요'라고 답한 결제 기준이에요. 아직 돌아보지 않은 결제는 '그 밖의 결제'에 들어가요.",
                meter,
                Ui.heroRow(c, "후회", Ui.Tone.DANGER, "후회한 결제 " + d.regretted.size() + "건", Money.won(reg) + " · " + pct + "%"),
                Ui.heroRow(c, "결제", Ui.Tone.PLAIN, "그 밖의 결제 " + (d.spent.size() - d.regretted.size()) + "건",
                        Money.won(total - reg) + " · " + (100 - pct) + "%"));
    }

    /** 1) 결제 · 후회 원그래프 두 개 + 색 범례 + [결제 자세히 보기] [후회 자세히 보기]. 그래프 자체는 누르면 조각만 커진다. */
    private static View piesCard(Context c, Data d, Nav nav) {
        List<CategorySum> paidCats = categories(d.spent), regCats = categories(d.regretted);
        LinearLayout card = Ui.card(c);
        card.addView(Ui.cardHeader(c, "무엇에 썼나요", "조각을 누르면 그 카테고리가 크게 보여요. 다시 누르면 돌아가요.", null, null), Ui.fillW());

        LinearLayout row = Ui.h(c);
        row.setGravity(Gravity.TOP);
        row.addView(pieColumn(c, "결제", slices(paidCats)), Ui.weight1());
        Ui.inlineFill(row, pieColumn(c, "후회", slices(regCats)), Ui.Space.ITEM);
        Ui.stack(card, row, Ui.Space.BLOCK);

        // 범례: 결제 그래프에 나온 카테고리 (색은 카테고리마다 고정이라 두 그래프에 같이 쓴다)
        FlowLayout legend = Ui.tagFlow(c);
        for (Slice s : slices(paidCats)) legend.addView(legendChip(c, s.color, s.name));
        Ui.stack(card, legend, Ui.Space.BLOCK);

        Ui.stack(card, Ui.buttonPair(c,
                Ui.button(c, "결제 자세히 보기", false, v -> nav.openSpend(false, null)),
                Ui.button(c, "후회 자세히 보기", false, v -> nav.openSpend(true, null))), Ui.Space.BLOCK);
        return card;
    }

    private static View pieColumn(Context c, String title, List<Slice> slices) {
        LinearLayout col = Ui.v(c);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView t = Ui.text(c, title, Ui.Font.LABEL, Ui.TEXT, Ui.BOLD);
        t.setGravity(Gravity.CENTER);
        col.addView(t, Ui.fillW());
        Ui.stack(col, new PieChart(c, slices, title), Ui.Space.ITEM);
        return col;
    }

    /** 범례 한 칸: 색 점 + 이름. */
    private static View legendChip(Context c, int color, String name) {
        LinearLayout l = Ui.h(c);
        l.setGravity(Gravity.CENTER_VERTICAL);
        View dot = new View(c);
        dot.setBackground(Ui.circle(color));
        l.addView(dot, Ui.square(c, Ui.Size.LIST_DOT));
        Ui.inline(l, Ui.text(c, name, Ui.Font.CAPTION, Ui.MUTED, Ui.REGULAR), Ui.Space.TIGHT);
        Ui.pad(l, c, 0, 0, Ui.Space.ITEM, 0);
        return l;
    }

    /** 카테고리별 지출 막대: 이름 · 금액 · 막대(카테고리 색, 가장 큰 카테고리 기준) · 횟수와 비율. focus는 바탕을 깔아 앞세운다. */
    private static View categoryCard(Context c, String caption, List<CategorySum> cats, Category focus, Consumer<Category> onPick) {
        long total = 0, max = 1;
        for (CategorySum g : cats) {
            total += g.sum;
            max = Math.max(max, g.sum);
        }
        LinearLayout card = Ui.card(c);
        card.addView(Ui.cardHeader(c, caption, "합계 " + Money.won(total) + " · 막대를 누르면 그 카테고리 결제만 봐요", null, null), Ui.fillW());
        for (CategorySum g : cats) {
            LinearLayout row = Ui.v(c);
            LinearLayout line = Ui.h(c);
            line.setGravity(Gravity.CENTER_VERTICAL);
            View dot = new View(c);
            dot.setBackground(Ui.circle(g.cat.color()));
            line.addView(dot, Ui.square(c, Ui.Size.LIST_DOT));
            Ui.inlineFill(line, Ui.text(c, g.cat.name, Ui.Font.LABEL, Ui.TEXT, g.cat == focus ? Ui.BOLD : Ui.MEDIUM), Ui.Space.ITEM);
            Ui.inline(line, Ui.text(c, Money.won(g.sum), Ui.Font.LABEL, Ui.TEXT, Ui.BOLD), Ui.Space.ITEM);
            row.addView(line, Ui.fillW());
            Ui.stack(row, Ui.bar(c, g.sum / (float) max, g.cat.color(), Ui.FILL), Ui.Space.TIGHT);
            int pct = total == 0 ? 0 : Math.round(g.sum * 100f / total);
            Ui.stack(row, Ui.meta(c, g.count + "번 · " + pct + "%"), Ui.Space.LINE);
            Ui.pad(row, c, Ui.Pad.TINT_ROW_H, Ui.Pad.TINT_ROW_V);
            if (g.cat == focus) row.setBackground(Ui.round(c, Ui.BRAND_LIGHT, Ui.Radius.NOTE));
            row.setOnClickListener(v -> onPick.accept(g.cat == focus ? null : g.cat));
            Ui.stack(card, row, Ui.Space.ITEM);
        }
        return card;
    }

    /** 3) 쉼표로 아낀 돈: 합계 · 멈춘 건수 · 주별 막대 그래프. */
    private static View savedCard(Context c, Data d, ZoneId zone) {
        long sum = 0;
        for (Record r : d.stoppedList) sum += r.amount;
        LinearLayout card = Ui.card(c);
        card.addView(Ui.cardHeader(c, "쉼표로 아낀 돈", "결제 직전에 멈춘 금액이에요", null, null), Ui.fillW());
        Ui.stack(card, Ui.text(c, Money.won(sum), Ui.Font.TITLE, Ui.TEXT, Ui.BOLD), Ui.Space.ITEM);
        String sub = d.talked.isEmpty() ? "아직 쉼표가 말을 건 결제가 없어요"
                : "쉼표가 말을 건 " + d.talked.size() + "번 중 " + d.stoppedList.size() + "번 멈췄어요";
        Ui.stack(card, Ui.meta(c, sub), Ui.Space.TEXT);
        int current = (int) Math.max(0, ChronoUnit.DAYS.between(d.cycleStart, d.today) / 7);
        Ui.stack(card, new WeekBars(c, savedByWeek(d, zone), current), Ui.Space.BLOCK);
        Ui.stack(card, Ui.micro(c, "급여일부터 한 주씩, 멈춘 결제 금액을 더했어요", Ui.SUBTLE), Ui.Space.ITEM);
        return card;
    }

    /** 돌아본 기록 한 줄 요약 (리포트 카드 · 돌아본 기록 화면 맨 위에 같이 쓴다). */
    private static void historySummary(Context c, LinearLayout card, Data d, Map<Value, ValueGroup> groups) {
        int answered = 0;
        for (ValueGroup g : groups.values()) answered += g.sat + g.reg;
        String insight = valueInsight(groups);
        Ui.stack(card, Ui.wordBreak(Ui.text(c, insight != null ? insight.replace("\n", " ") : "다음 날 돌아본 답이 모이면 내 기준이 보여요",
                Ui.Font.BODY, Ui.TEXT, Ui.MEDIUM)), Ui.Space.ITEM);
        Ui.stack(card, Ui.meta(c, "대화한 결제 " + d.talked.size() + "건 · 다음 날 돌아본 답 " + answered + "건"), Ui.Space.LINE);
    }

    /** 4) 돌아본 기록: 제목 줄 오른쪽 위에 [더보기 ›] (→ 돌아본 기록 화면) + 한 줄 요약. */
    private static View historyCard(Context c, Data d, Map<Value, ValueGroup> groups, Nav nav) {
        LinearLayout card = Ui.card(c);
        card.addView(Ui.cardHeader(c, "돌아본 기록", "결제 다음 날 만족했는지, 후회했는지 답한 걸 모았어요",
                "더보기 ›", v -> nav.openHistory()), Ui.fillW());
        historySummary(c, card, d, groups);
        return card;
    }

    // ================================================================== 자세히 화면 (리포트에서 넘어가는 하위 화면)

    /** 돌아본 기록 화면: 요약 + 가치별 만족·후회 · 그때 내가 한 말 · 멈춘 결제. */
    static void historyPage(Context c, LinearLayout out, Data d, ZoneId zone) {
        Map<Value, ValueGroup> groups = valueGroups(d);
        LinearLayout summary = Ui.card(c);
        summary.addView(Ui.cardHeader(c, "이번 달 내 기준", null, null, null), Ui.fillW());
        historySummary(c, summary, d, groups);
        Ui.stack(out, summary, Ui.Space.BLOCK);
        if (!groups.isEmpty()) Ui.stack(out, valuesCard(c, groups), Ui.Space.BLOCK);
        View said = saidCard(c, d, zone);
        if (said != null) Ui.stack(out, said, Ui.Space.BLOCK);
        View stopped = stoppedCard(c, d);
        if (stopped != null) Ui.stack(out, stopped, Ui.Space.BLOCK);
    }

    /**
     * 결제 · 후회 자세히 화면: 합계 → 카테고리별 지출 막대 → 결제 목록.
     * focus(원그래프에서 고른 카테고리)가 있으면 막대에서 앞세우고 목록도 그 카테고리만. 막대를 누르면 focus를 바꾼다.
     */
    static void spendPage(Context c, LinearLayout out, Data d, boolean regret, Category focus,
                          Function<Record, View> rowView, Consumer<Category> onFocus) {
        List<Record> list = regret ? d.regretted : d.spent;
        long total = sum(list), all = sum(d.spent);

        LinearLayout top = Ui.card(c);
        top.addView(Ui.caption(c, regret ? "다음 날 '후회돼요'라고 답한 결제" : "이번 달 결제"));
        Ui.stack(top, Ui.text(c, Money.won(total), Ui.Font.TITLE, Ui.TEXT, Ui.BOLD), Ui.Space.LINE);
        String sub = regret
                ? list.size() + "건 · 이번 달 결제 " + Money.won(all) + "의 " + (all == 0 ? 0 : Math.round(total * 100f / all)) + "%"
                : list.size() + "건 · 계좌 내역과 쉼표가 본 결제를 합쳤어요";
        Ui.stack(top, Ui.meta(c, sub), Ui.Space.TEXT);
        Ui.stack(out, top, Ui.Space.BLOCK);

        if (list.isEmpty()) {
            Ui.stack(out, Ui.tintNote(c, regret ? "아직 후회한 결제가 없어요. 결제 다음 날 돌아본 답이 여기 모여요." : "아직 이번 달 결제가 없어요.",
                    Ui.Font.CAPTION, Ui.BRAND, Ui.Radius.INPUT, Ui.Pad.FOOD_NOTE_H, Ui.Pad.FOOD_NOTE_V), Ui.Space.BLOCK);
            return;
        }
        Ui.stack(out, categoryCard(c, "카테고리별 지출", categories(list), focus, onFocus), Ui.Space.BLOCK);

        LinearLayout records = Ui.card(c);
        int n = 0;
        for (Record r : list) if (focus == null || Category.of(r) == focus) n++;
        records.addView(Ui.cardHeader(c, (focus == null ? "결제 목록" : focus.name + " 결제") + " " + n + "건", "최근 순이에요",
                focus == null ? null : "전체 보기", v -> onFocus.accept(null)), Ui.fillW());
        for (Record r : list) {
            if (focus != null && Category.of(r) != focus) continue;
            Ui.stack(records, rowView.apply(r), Ui.Space.ITEM);
        }
        Ui.stack(out, records, Ui.Space.BLOCK);
    }

    /** "+12,000원" · "−3,000원" (부호로도 방향을 알 수 있게). */
    private static String signedWon(long v) {
        return (v > 0 ? "+" : v < 0 ? "−" : "") + Money.won(Math.abs(v));
    }

    /** 5) 미리 떼어 둔 돈, 평균과 비교: 항목마다 평균과의 차이 막대 (가운데가 평균). */
    private static View setAsideCard(Context c, Data d) {
        BackendSlots back = BackendSlots.get();
        List<SetAside> items = setAsides(d, back);
        long total = 0, maxAbs = 1;
        for (SetAside a : items) {
            if (!a.known()) continue;
            total += a.diff();
            maxAbs = Math.max(maxAbs, Math.abs(a.diff()));
        }

        LinearLayout card = Ui.card(c);
        card.addView(Ui.cardHeader(c, "미리 떼어 둔 돈, 평균과 비교", "고정지출과 식비 예산은 최근 3개월 평균만큼 떼어 뒀어요", null, null), Ui.fillW());
        String head = Math.abs(total) < 1_000 ? "평균과 비슷하게 나갔어요"
                : "평균보다 " + Money.won(Math.abs(total)) + (total > 0 ? " 더 나갔어요" : " 덜 나갔어요");
        Ui.stack(card, Ui.wordBreak(Ui.text(c, head, Ui.Font.TITLE, Math.abs(total) < 1_000 ? Ui.TEXT : total > 0 ? Ui.RED : Ui.GREEN, Ui.BOLD)), Ui.Space.ITEM);

        LinearLayout legend = Ui.h(c);
        legend.addView(Ui.legendRow(c, Ui.RED, "평균보다 더", ""), Ui.weight1());
        legend.addView(Ui.legendRow(c, Ui.GREEN, "평균보다 덜", ""), Ui.weight1());
        Ui.stack(card, legend, Ui.Space.BLOCK);

        for (SetAside a : items) {
            LinearLayout row = Ui.v(c);
            LinearLayout line = Ui.h(c);
            line.addView(Ui.text(c, a.name, Ui.Font.LABEL, Ui.TEXT, Ui.MEDIUM), Ui.weight1());
            String right = !a.known() ? "아직 청구 전" : a.diff() == 0 ? "평균대로" : signedWon(a.diff());
            line.addView(Ui.text(c, right, Ui.Font.LABEL, a.known() ? Ui.TEXT : Ui.SUBTLE, Ui.BOLD));
            row.addView(line, Ui.fillW());
            if (a.known()) Ui.stack(row, new DiffBar(c, a.diff() / (float) maxAbs), Ui.Space.TIGHT);
            String sub = a.paced
                    ? "오늘까지 평균 속도 " + Money.won(a.expected) + " · 실제 " + Money.won(a.actual) + " (한 달 " + Money.won(a.average) + ")"
                    : "평균 " + Money.won(a.average) + (a.known() ? " · 이번 달 " + Money.won(a.actual) : "");
            Ui.stack(row, Ui.meta(c, sub), Ui.Space.LINE);
            Ui.stack(card, row, Ui.Space.BLOCK);
        }

        Ui.stack(card, Ui.micro(c, "식비는 한 달 내내 나가서 오늘까지 평균 속도와 비교해요. 다음 달부터 쿠팡 식료품 실측값으로 보정해요."
                + (back.fixedDemo ? "\n고정지출의 이번 달 금액은 데모 값이에요 (계좌 연동 전)." : ""), Ui.SUBTLE), Ui.Space.BLOCK);
        return card;
    }

    /** 가치마다 한 칸: 판정 태그 · 만족/후회 막대 · 횟수와 금액 · 고른 이유. 칸 바탕은 판정 색. */
    private static View valuesCard(Context c, Map<Value, ValueGroup> groups) {
        LinearLayout card = Ui.card(c);
        card.addView(Ui.cardHeader(c, "무엇 때문에 썼고, 다음 날 어땠나요", "결제할 때 고른 이유를 묶고, 다음 날 답과 이었어요", null, null), Ui.fillW());
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
            else if (g.sat > 0) { bg = Ui.FILL; verdict = "반반이에요"; tone = Ui.Tone.WARNING; }
            else { bg = Ui.FILL; verdict = "아직 몰라요"; tone = Ui.Tone.PLAIN; }

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
        card.addView(Ui.cardHeader(c, "그때 내가 한 말", "쉼표와 대화하며 직접 적은 말이에요", null, null), Ui.fillW());
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
        card.addView(Ui.cardHeader(c, "멈춘 결제, 지금은 어때요?", "멈춘 뒤에도 생각이 그대로인지 골라 보세요", null, null), Ui.fillW());
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
                    : "좋아요. 다음 달엔 이런 결제 앞에서 쉼표가 한 번 더 물어볼게요.");
            reply.setVisibility(View.VISIBLE);
        }, options.toArray(new String[0])), Ui.Space.BLOCK);
        Ui.stack(box, Ui.wordBreak(reply), Ui.Space.BLOCK);
        Ui.stack(box, Ui.micro(c, "미리보기예요. 고른 답은 다음 달 대화의 질문 재료로 쓸 예정이에요 (아직 저장 안 됨).", Ui.MUTED), Ui.Space.BLOCK);
        return box;
    }
}
