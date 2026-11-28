package com.shimpyo.app.core;

import com.shimpyo.app.core.Enums.Decision;
import com.shimpyo.app.core.Enums.Intensity;
import com.shimpyo.app.core.Enums.Judgment;
import com.shimpyo.app.core.Enums.Outcome;
import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.Enums.Trigger;
import com.shimpyo.app.core.TriggerEngine.Evaluation;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * PRD 시나리오(이수민, 10일째: 여윳돈 40만, B≈13,300, 적용 한도 14,000)로 엔진을 검증한다.
 */
public class EngineTest {
    static final ZoneId Z = ZoneId.of("Asia/Seoul");
    static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    static long at(int hour, int min) {
        return LocalDateTime.of(TODAY, java.time.LocalTime.of(hour, min)).atZone(Z).toInstant().toEpochMilli();
    }

    static long daysAgo(int d, int hour) {
        return LocalDateTime.of(TODAY.minusDays(d), java.time.LocalTime.of(hour, 0)).atZone(Z).toInstant().toEpochMilli();
    }

    /** 주기 10일째로 맞추고, 어제까지 여윳돈 20,000원을 쓴 상태 (여윳돈 26만 = 100만 − 고정 64만 − 식비 10만). */
    static Settings settings() {
        Settings s = new Settings();
        s.cycleStartOverride = TODAY.minusDays(10).toEpochDay();
        return s;
    }

    /** 야식 규칙만 보려는 테스트용: 고정지출을 줄여 여윳돈 40만 (오늘 한도에 여유가 있어 재정 위험이 섞이지 않게). */
    static Settings roomy() {
        Settings s = settings();
        s.rent = 350_000;
        s.transport = 40_000;
        return s;
    }

    static List<Record> base(boolean lunch) {
        List<Record> list = new ArrayList<>();
        Record past = new Record();
        past.ts = daysAgo(1, 12);
        past.amount = past.discretionary = 20_000;
        past.outcome = Outcome.SYNC;
        past.title = "이번 주기 지출";
        list.add(past);
        if (lunch) {
            Record l = new Record();
            l.ts = at(12, 30);
            l.amount = l.discretionary = 8_000;
            l.outcome = Outcome.SYNC;
            l.title = "외식 · 학교 앞 식당";
            list.add(l);
        }
        return list;
    }

    static PaymentAttempt attempt(TargetApp app, long amount, String title, Judgment j, boolean food) {
        PaymentAttempt a = new PaymentAttempt();
        a.app = app;
        a.amount = amount;
        a.title = title;
        a.judgment = j;
        a.isFood = food;
        return a;
    }

    static Record lateRecord(int daysAgo, long amount) {
        Record r = new Record();
        r.app = TargetApp.BAEMIN;
        r.ts = daysAgo(daysAgo, 23);
        r.amount = r.discretionary = amount;
        r.outcome = Outcome.PASS;
        r.lateNight = true;
        r.title = "야식";
        return r;
    }

    @Test
    public void budgetBaseline() {
        BudgetEngine.State b = BudgetEngine.compute(settings(), base(true), at(19, 0), Z);
        assertEquals(260_000, b.monthly);
        assertEquals(30, b.cycleDays);
        assertEquals(20, b.daysLeft);
        assertEquals(12_000, b.limit);
        assertEquals(8_000, b.todaySpent);
        assertEquals(4_000, b.available);
        assertEquals(232_000, b.remainingNow);
        assertEquals(100_000, b.foodRemaining);
    }

    @Test
    public void baeminChickenIsMedium() {
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.BAEMIN, 23_000, "후라이드 치킨 세트", null, false),
                settings(), base(true), at(19, 0), Z, 0);
        assertTrue(e.has(Trigger.FIN_RISK));
        assertFalse(e.has(Trigger.HIGH_AMOUNT));
        assertEquals(Intensity.MEDIUM, e.intensity);
        assertEquals(19_000, e.overLimit);
    }

    @Test
    public void coupangTissueNeedIsLight() {
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.COUPANG, 24_000, "휴지·세제", Judgment.NEED, false),
                settings(), base(true), at(19, 0), Z, 0);
        assertTrue(e.has(Trigger.FIN_RISK));
        assertEquals(Intensity.MEDIUM, e.base);
        assertEquals(Intensity.LIGHT, e.intensity);
    }

    @Test
    public void foodWithinBudgetNoIntervention() {
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.COUPANG, 18_000, "계란·우유·두부", Judgment.NEED, true),
                settings(), base(true), at(19, 0), Z, 0);
        assertFalse(e.intervene());
        assertEquals(18_000, e.food);
        assertEquals(0, e.discretionary);
    }

    @Test
    public void foodOverBudgetOnlyExcessCounts() {
        Settings s = settings();
        s.martBudget = 0;
        s.coupangFoodBudget = 10_000;
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.COUPANG, 18_000, "계란", Judgment.NEED, true),
                s, base(true), at(19, 0), Z, 0);
        assertEquals(10_000, e.food);
        assertEquals(8_000, e.discretionary);
    }

    @Test
    public void coatIsStrongWithDaily5500() {
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.MUSINSA, 129_000, "오버핏 울 코트", Judgment.IMPULSE, false),
                settings(), base(false), at(21, 0), Z, 0);
        assertTrue(e.has(Trigger.FIN_RISK));
        assertTrue(e.has(Trigger.HIGH_AMOUNT));
        assertEquals(Intensity.STRONG, e.intensity);
        assertEquals(5_500, e.dailyAfter);
        assertEquals("129,000원 · 결제하면 급여일까지 하루 약 5,500원",
                QuestionTemplates.factLine(e, attempt(TargetApp.MUSINSA, 129_000, "코트", Judgment.IMPULSE, false)));
    }

    @Test
    public void bingeThirdIsLight() {
        List<Record> recs = base(false);
        for (int i = 0; i < 2; i++) {
            Record r = new Record();
            r.app = TargetApp.MUSINSA;
            r.ts = at(22, 10 + i * 10);
            r.amount = r.discretionary = 3_400;
            r.judgment = Judgment.AMBIGUOUS;
            r.outcome = Outcome.PASS;
            recs.add(r);
        }
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.MUSINSA, 4_500, "머리끈 세트", Judgment.AMBIGUOUS, false),
                settings(), recs, at(22, 35), Z, 0);
        assertEquals(1, e.triggers.size());
        assertTrue(e.has(Trigger.BINGE));
        assertEquals(Intensity.LIGHT, e.intensity);
    }

    @Test
    public void lateNightFourthOnlyIsLight() {
        List<Record> recs = base(false);
        recs.add(lateRecord(5, 13_000));
        recs.add(lateRecord(3, 14_000));
        recs.add(lateRecord(1, 12_000));
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.BAEMIN, 12_000, "떡볶이", null, false),
                roomy(), recs, at(23, 0), Z, 0);
        assertEquals(1, e.triggers.size());
        assertTrue(e.has(Trigger.LATE_NIGHT));
        assertEquals(4, e.lateCountNow);
        assertEquals(51_000, e.lateSumNow);
        assertEquals(Intensity.LIGHT, e.intensity);
        assertEquals("이번 주 네 번째 야식이에요 · 4번 합계 51,000원",
                QuestionTemplates.lightHeadline(e, attempt(TargetApp.BAEMIN, 12_000, "떡볶이", null, false)));
    }

    @Test
    public void lateNightThirdNoIntervention() {
        List<Record> recs = base(false);
        recs.add(lateRecord(3, 14_000));
        recs.add(lateRecord(1, 12_000));
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.BAEMIN, 12_000, "떡볶이", null, false),
                roomy(), recs, at(23, 0), Z, 0);
        assertFalse(e.intervene());
        assertEquals(3, e.lateCountNow);
    }

    @Test
    public void lateNightResetAfterIntervention() {
        Settings s = roomy();
        List<Record> recs = base(false);
        recs.add(lateRecord(5, 13_000));
        recs.add(lateRecord(3, 14_000));
        recs.add(lateRecord(2, 12_000));
        s.lateResetTs = daysAgo(2, 23) + 60_000; // 2일 전 개입 뒤 초기화
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.BAEMIN, 12_000, "떡볶이", null, false),
                s, recs, at(23, 0), Z, 0);
        assertFalse(e.intervene());
        assertEquals(1, e.lateCountNow);
    }

    @Test
    public void lateNightWithRiskIsMediumWithPatternQuestion() {
        List<Record> recs = base(true);
        recs.add(lateRecord(5, 13_000));
        recs.add(lateRecord(3, 14_000));
        recs.add(lateRecord(1, 12_000));
        PaymentAttempt a = attempt(TargetApp.BAEMIN, 23_000, "후라이드 치킨 세트", null, false);
        Evaluation e = TriggerEngine.evaluate(a, settings(), recs, at(23, 0), Z, 0);
        assertTrue(e.has(Trigger.FIN_RISK));
        assertTrue(e.has(Trigger.LATE_NIGHT));
        assertEquals(Intensity.MEDIUM, e.intensity);
        Question q1 = QuestionTemplates.build(e, a, 0, new QuestionTemplates.History());
        Question q2 = QuestionTemplates.build(e, a, 1, new QuestionTemplates.History());
        assertTrue(q1.text.startsWith("이 결제면 오늘 한도를 " + Money.won(e.overLimit) + " 넘어요."));
        assertEquals("패턴", q2.type);
        assertTrue(q2.text.contains("네 번째 야식"));
    }

    @Test
    public void lateNightOutsideHoursIgnored() {
        List<Record> recs = base(false);
        recs.add(lateRecord(5, 13_000));
        recs.add(lateRecord(3, 14_000));
        recs.add(lateRecord(1, 12_000));
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.BAEMIN, 12_000, "떡볶이", null, false),
                roomy(), recs, at(19, 0), Z, 0);
        assertFalse(e.intervene());
    }

    @Test
    public void simulatedHourMakesLate() {
        Settings s = settings();
        s.simulatedHour = 23;
        assertTrue(TriggerEngine.isLateHour(s, at(15, 0), Z));
        s.simulatedHour = 3;
        assertTrue(TriggerEngine.isLateHour(s, at(15, 0), Z));
        s.simulatedHour = 4;
        assertFalse(TriggerEngine.isLateHour(s, at(15, 0), Z));
    }

    @Test
    public void zeroRemainingIsStrong() {
        List<Record> recs = base(false);
        recs.get(0).amount = recs.get(0).discretionary = 395_000;
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.BAEMIN, 15_000, "떡볶이", null, false),
                settings(), recs, at(19, 0), Z, 0);
        assertTrue(e.zeroOrBelow);
        assertEquals(Intensity.STRONG, e.intensity);
    }

    @Test
    public void thirdInterventionEscalates() {
        List<Record> recs = base(false);
        for (int i = 0; i < 2; i++) {
            Record r = new Record();
            r.app = TargetApp.MUSINSA;
            r.ts = at(22, 10 + i * 10);
            r.amount = r.discretionary = 3_400;
            r.judgment = Judgment.AMBIGUOUS;
            r.outcome = Outcome.PASS;
            recs.add(r);
        }
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.MUSINSA, 4_500, "머리끈", Judgment.AMBIGUOUS, false),
                settings(), recs, at(22, 35), Z, 2);
        assertEquals(Intensity.LIGHT, e.base);
        assertEquals(Intensity.MEDIUM, e.intensity);
        assertEquals(3, e.interventionNo);
    }

    @Test
    public void needWithinLimitPasses() {
        Evaluation e = TriggerEngine.evaluate(attempt(TargetApp.COUPANG, 5_000, "칫솔", Judgment.NEED, false),
                settings(), base(false), at(19, 0), Z, 0);
        assertFalse(e.intervene());
    }

    @Test
    public void lastChipDecision() {
        PaymentAttempt a = attempt(TargetApp.BAEMIN, 23_000, "치킨", null, false);
        Evaluation e = TriggerEngine.evaluate(a, settings(), base(true), at(19, 0), Z, 0);
        Session s = new Session("k", at(19, 0));
        s.attempt = a;
        s.eval = e;
        s.questions.add(QuestionTemplates.build(e, a, 0, null));
        assertEquals(Decision.NONE, s.answer("그냥 당겨요", Decision.NONE));
        s.questions.add(QuestionTemplates.build(e, a, 1, null));
        assertEquals(Decision.STOP, s.answer("주말도 괜찮아요", Decision.STOP));
        assertTrue(s.inDecisionStep());
    }

    @Test
    public void strongPastLinkRequiresInput() {
        PaymentAttempt a = attempt(TargetApp.MUSINSA, 129_000, "코트", Judgment.IMPULSE, false);
        Evaluation e = TriggerEngine.evaluate(a, settings(), base(false), at(21, 0), Z, 0);
        QuestionTemplates.History h = new QuestionTemplates.History();
        h.regretTitle = "숏 패딩";
        Question q = QuestionTemplates.build(e, a, 1, h);
        assertTrue(q.requireInput);
        assertTrue(q.text.contains("숏 패딩"));
        Question q3 = QuestionTemplates.build(e, a, 2, h);
        assertTrue(q3.text.contains("5,500원"));
    }

    @Test
    public void decisionFromFreeText() {
        assertEquals(Decision.STOP, QuestionTemplates.decisionFromText("다음 주에 다시 볼게요, 지금은 미룰래요"));
        assertEquals(Decision.BUY, QuestionTemplates.decisionFromText("그래도 살래요"));
        assertEquals(Decision.NONE, QuestionTemplates.decisionFromText("음 글쎄요"));
    }

    // ------------------------------------------------------------ 화면 파싱

    @Test
    public void parseBaeminCheckout() {
        List<String> t = Arrays.asList("주문하기", "배달 방식", "한집배달", "OO구 OO로 12", "결제수단", "배민페이",
                "결제금액", "주문금액", "23,000원", "배달팁", "0원", "23,000원 결제하기");
        ScreenParser.Result r = ScreenParser.parse(TargetApp.BAEMIN, t);
        assertTrue(r.checkout);
        assertEquals(23_000, r.amount);
    }

    @Test
    public void parseBaeminCartIsNotCheckout() {
        List<String> t = Arrays.asList("장바구니", "바삭치킨", "후라이드 치킨 세트", "23,000원", "+ 더 담으러 가기",
                "한집배달", "23,000원 배달 주문하기");
        ScreenParser.Result r = ScreenParser.parse(TargetApp.BAEMIN, t);
        assertFalse(r.checkout);
    }

    @Test
    public void parseCoupangCheckout() {
        List<String> t = Arrays.asList("주문/결제", "받는사람 정보", "이수민", "내일(목) 도착 보장", "3겹 데코 화장지 30롤",
                "수량 1개 · 15,900원", "액체 세제 3L", "수량 1개 · 10,100원", "결제정보", "총상품가격", "26,000원",
                "배송비", "0원", "총결제금액", "26,000원", "결제수단", "쿠페이 카드", "결제하기");
        ScreenParser.Result r = ScreenParser.parse(TargetApp.COUPANG, t);
        assertTrue(r.checkout);
        assertEquals(26_000, r.amount);
        assertEquals(Arrays.asList("3겹 데코 화장지 30롤", "액체 세제 3L"), r.items);
        PaymentAttempt a = attempt(TargetApp.COUPANG, r.amount, "", null, false);
        KeywordJudge.judge(a, String.join(" ", r.items));
        assertEquals(Judgment.NEED, a.judgment);
        assertFalse(a.isFood);
    }

    @Test
    public void parseMusinsaCheckout() {
        List<String> t = Arrays.asList("주문서", "배송 정보", "브랜드명", "오버핏 울 코트 (차콜)", "M / 1개", "129,000원",
                "결제 수단", "무신사페이", "총 결제 금액", "129,000원", "129,000원 결제하기");
        ScreenParser.Result r = ScreenParser.parse(TargetApp.MUSINSA, t);
        assertTrue(r.checkout);
        assertEquals(129_000, r.amount);
        PaymentAttempt a = attempt(TargetApp.MUSINSA, r.amount, "", null, false);
        KeywordJudge.judge(a, r.joined);
        assertEquals(Judgment.IMPULSE, a.judgment);
    }

    @Test
    public void musinsaProductPageIsNotCheckout() {
        List<String> t = Arrays.asList("오버핏 울 코트 (차콜)", "129,000원", "무신사페이 결제 시 5% 할인", "리뷰 132", "구매하기");
        assertFalse(ScreenParser.parse(TargetApp.MUSINSA, t).checkout);
    }

    @Test
    public void parseComplete() {
        ScreenParser.Result r = ScreenParser.parse(TargetApp.BAEMIN, Arrays.asList("주문이 완료되었어요", "주문내역 보기"));
        assertTrue(r.complete);
        assertFalse(r.checkout);
    }

    @Test
    public void statsMindChangeRate() {
        Settings s = settings();
        List<Record> recs = base(false);
        for (int i = 0; i < 12; i++) {
            Record r = new Record();
            r.app = TargetApp.BAEMIN;
            r.ts = at(10 + (i % 8), i);
            r.amount = 10_000;
            r.intensity = Intensity.MEDIUM;
            r.outcome = i < 5 ? Outcome.PREVENTED : Outcome.DELIBERATE_PAY;
            r.discretionary = r.outcome.spend ? 10_000 : 0;
            recs.add(r);
        }
        Stats st = Stats.of(s, recs, at(23, 0), Z);
        assertEquals(12, st.interventions);
        assertEquals(5, st.prevented);
        assertEquals(42, st.mindChangeRate());
    }

    @Test
    public void couponAppliedAmountIsReadFromPayButton() {
        // 쿠폰 적용 전(25,000원)과 후(23,000원)가 한 화면에 같이 보여도 결제 버튼의 최종 금액을 쓴다.
        List<String> before = Arrays.asList("주문하기", "주문금액", "25,000원", "할인쿠폰", "쿠폰 선택", "결제수단", "25,000원 결제하기");
        List<String> after = Arrays.asList("주문하기", "주문금액", "25,000원", "할인쿠폰", "2,000원 할인", "결제수단", "결제금액", "25,000원", "23,000원", "23,000원 결제하기");
        assertEquals(25_000, ScreenParser.parse(TargetApp.BAEMIN, before).amount);
        assertEquals(23_000, ScreenParser.parse(TargetApp.BAEMIN, after).amount);
    }

    @Test
    public void payButtonTextDetection() {
        assertTrue(ScreenParser.isPayButtonText("23,000원 결제하기"));
        assertTrue(ScreenParser.isPayButtonText("결제하기"));
        assertFalse(ScreenParser.isPayButtonText("구매하기"));
        assertFalse(ScreenParser.isPayButtonText("총 결제금액 23,000원"));
        assertFalse(ScreenParser.isPayButtonText(null));
    }

    private static Session stoppedSession(TargetApp app, long stoppedAt) {
        Session s = new Session(app.name() + ":23000", stoppedAt - 1000);
        s.attempt = new PaymentAttempt();
        s.attempt.app = app;
        s.stopped = true;
        s.stoppedAt = stoppedAt;
        return s;
    }

    @Test
    public void retryAfterStopWithinWindowOnSameApp() {
        long t = at(19, 0);
        Session prev = stoppedSession(TargetApp.BAEMIN, t);
        assertTrue(Session.isRetryAfterStop(prev, TargetApp.BAEMIN, t + 60_000L));
        assertTrue(Session.isRetryAfterStop(prev, TargetApp.BAEMIN, t + Session.RETRY_WINDOW_MS - 1));
    }

    @Test
    public void notRetryWhenWindowPassedOtherAppOrNotStopped() {
        long t = at(19, 0);
        Session prev = stoppedSession(TargetApp.BAEMIN, t);
        assertFalse(Session.isRetryAfterStop(prev, TargetApp.BAEMIN, t + Session.RETRY_WINDOW_MS));
        assertFalse(Session.isRetryAfterStop(prev, TargetApp.COUPANG, t + 60_000L));
        assertFalse(Session.isRetryAfterStop(null, TargetApp.BAEMIN, t));
        prev.stopped = false; // 자기결정 이탈·허용 후 결제 등 '멈춤'이 아닌 종료
        assertFalse(Session.isRetryAfterStop(prev, TargetApp.BAEMIN, t + 60_000L));
    }

    @Test
    public void retryLineMentionsPreviousStopWithoutLecturing() {
        String b = QuestionTemplates.retryLine(TargetApp.BAEMIN);
        String c = QuestionTemplates.retryLine(TargetApp.COUPANG);
        assertTrue(b.startsWith("아까는 멈추셨어요") && b.contains("주문"));
        assertTrue(c.startsWith("아까는 멈추셨어요") && c.contains("결제"));
        assertFalse(b.contains("안 돼") || c.contains("안 돼"));
    }
}
