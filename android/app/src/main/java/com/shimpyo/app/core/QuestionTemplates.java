package com.shimpyo.app.core;

import com.shimpyo.app.core.Enums.Decision;
import com.shimpyo.app.core.Enums.Intensity;
import com.shimpyo.app.core.Enums.Judgment;
import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.Enums.Trigger;
import com.shimpyo.app.core.Question.Chip;
import com.shimpyo.app.core.TriggerEngine.Evaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * AI가 없거나 늦을 때 쓰는 기본 질문 (REQ-8-3, 8-5).
 * 숫자는 계산값만 쓰고, 판단·훈계 표현은 쓰지 않는다.
 */
public final class QuestionTemplates {
    private QuestionTemplates() {}

    /** 과거 기록에서 뽑은 질문 재료. */
    public static class History {
        /** 같은 앱에서 '후회돼요'라고 답한 최근 결제 제목 (없으면 null). */
        public String regretTitle;
        /** 이번 주기 같은 앱 결제 횟수 (이번 결제 제외). */
        public int sameAppCount;
        /** '습관처럼 시켰어요'를 고른 적이 있는지. */
        public boolean habitChosen;
    }

    // ------------------------------------------------------------ 가벼움

    public static String lightHeadline(Evaluation e, PaymentAttempt a) {
        if (e.has(Trigger.LATE_NIGHT) && e.onlyPattern())
            return "이번 주 " + ordinal(e.lateCountNow) + " 야식이에요 · " + e.lateCountNow + "번 합계 " + Money.won(e.lateSumNow);
        if (e.has(Trigger.FIN_RISK) && a.judgment == Judgment.NEED && a.app != TargetApp.BAEMIN)
            return "필요한 소비지만 결제하면 오늘 한도를 넘어요.";
        if (e.has(Trigger.BINGE))
            return "1시간 안에 " + ordinal(e.bingeCountNow) + " 결제예요.";
        if (e.has(Trigger.HIGH_AMOUNT))
            return Money.won(a.amount) + ", 큰 금액 결제예요.";
        if (e.has(Trigger.FIN_RISK))
            return "결제하면 오늘 한도를 " + Money.won(e.overLimit) + " 넘어요.";
        return "잠깐 이유만 골라 주세요.";
    }

    public static List<String> lightChips(Evaluation e, PaymentAttempt a) {
        List<String> c = new ArrayList<>();
        if (e.has(Trigger.LATE_NIGHT) && e.onlyPattern()) {
            c.add("진짜 배고파요");
            c.add("습관처럼 시켰어요");
            c.add("공부하느라요");
            c.add("약속이 있어요");
        } else if (a.app == TargetApp.BAEMIN) {
            c.add("끼니 대용이에요");
            c.add("약속이 있어요");
            c.add("습관처럼 시켰어요");
            c.add("그냥 먹고 싶어요");
        } else if (a.isFood && a.judgment == Judgment.NEED) {
            c.add("한 달치 장보기예요");
            c.add("다 떨어졌어요");
            c.add("약속이 있어요");
            c.add("그냥 사고 싶어요");
        } else {
            c.add("다 떨어졌어요");
            c.add("선물이에요");
            c.add("약속이 있어요");
            c.add("그냥 사고 싶어요");
        }
        return c;
    }

    // ------------------------------------------------------------ 보통·강함

    /** 방금 멈춘 뒤 다시 결제를 시도했을 때 첫 화면에 붙이는 한 줄. 훈계 없이 사실과 질문만 쓴다. */
    public static String retryLine(TargetApp app) {
        return app == TargetApp.BAEMIN
                ? "아까는 멈추셨어요. 다시 주문하려는 이유가 있나요?"
                : "아까는 멈추셨어요. 다시 결제하려는 이유가 있나요?";
    }

    /** 결정 직전 사실 한 줄 (AI가 쓰지 않는 계산값). */
    public static String factLine(Evaluation e, PaymentAttempt a) {
        String amt = Money.won(a.amount);
        if (e.intensity == Intensity.STRONG) {
            if (e.remainingAfter > 0) return amt + " · 결제하면 급여일까지 하루 약 " + Money.won(e.dailyAfter);
            return amt + " · 결제하면 이번 달 여윳돈 " + Money.won(e.remainingAfter);
        }
        String s;
        if (e.has(Trigger.FIN_RISK)) s = amt + " · 오늘 한도 " + Money.won(e.overLimit) + " 초과";
        else s = amt + " · 오늘 남은 돈 " + Money.won(Math.max(0, e.budget.available));
        if (e.has(Trigger.LATE_NIGHT)) s += " · 이번 주 야식 " + e.lateCountNow + "번째";
        return s;
    }

    static String verb(PaymentAttempt a) {
        return a.app == TargetApp.BAEMIN ? "시킬까요" : "살까요";
    }

    static String ordinal(int n) {
        String[] k = {"", "첫 번째", "두 번째", "세 번째", "네 번째", "다섯 번째", "여섯 번째", "일곱 번째", "여덟 번째", "아홉 번째", "열 번째"};
        return n >= 0 && n < k.length ? k[n] : n + "번째";
    }

    /** index번째(0부터) 질문의 유형. */
    public static String typeFor(Evaluation e, int index) {
        int total = e.intensity.questions;
        if (total == 2) {
            if (index == 0) return "필요vs욕구";
            return e.has(Trigger.LATE_NIGHT) ? "패턴" : "시점";
        }
        if (index == 0) return "필요vs욕구";
        if (index == 1) return "과거 연결";
        return "미래의 나";
    }

    public static Question build(Evaluation e, PaymentAttempt a, int index, History h) {
        String type = typeFor(e, index);
        int total = e.intensity.questions;
        boolean last = index == total - 1;
        boolean baemin = a.app == TargetApp.BAEMIN;
        String item = a.shortTitle();

        Question q;
        switch (type) {
            case "필요vs욕구": {
                String prefix = "";
                if (total == 2 && e.has(Trigger.FIN_RISK)) prefix = "이 결제면 오늘 한도를 " + Money.won(e.overLimit) + " 넘어요. ";
                if (baemin) {
                    if (e.intensity == Intensity.STRONG && e.has(Trigger.HIGH_AMOUNT)) {
                        q = new Question(type, Money.won(a.amount) + "짜리 주문이에요. 오늘 특별한 날이에요?",
                                new Chip("특별한 날이에요"), new Chip("그냥 먹고 싶어요"), new Chip("잘 모르겠어요"));
                    } else {
                        q = new Question(type, prefix + "지금 배가 고픈 거예요, 그냥 당기는 거예요?",
                                new Chip("배가 고파요"), new Chip("그냥 당겨요"), new Chip("잘 모르겠어요"));
                    }
                } else if (a.judgment == Judgment.NEED) {
                    q = new Question(type, prefix + "지금 쓰던 건 어떤 상태예요?",
                            new Chip("다 떨어졌어요"), new Chip("아직 조금 있어요"), new Chip("잘 모르겠어요"));
                } else if (a.app == TargetApp.MUSINSA) {
                    q = new Question(type, prefix + "가진 게 없어서예요, 마음에 들어서예요?",
                            new Chip("없어서요"), new Chip("마음에 들어서요"), new Chip("둘 다예요"));
                } else {
                    q = new Question(type, prefix + "지금 이게 필요한 거예요, 갖고 싶은 거예요?",
                            new Chip("필요해요"), new Chip("갖고 싶어요"), new Chip("둘 다예요"));
                }
                break;
            }
            case "시점": {
                String head = e.has(Trigger.FIN_RISK) ? "" : "오늘 안 사면 어떤 일이 생겨요? ";
                q = new Question(type, head + (baemin ? "주말까지 미뤄도 괜찮을까요?" : "다음에 사도 괜찮을까요?"),
                        new Chip(baemin ? "주말도 괜찮아요" : "다음에 살게요", Decision.STOP),
                        new Chip(baemin ? "오늘 먹을래요" : "지금 필요해요", Decision.BUY),
                        new Chip("잘 모르겠어요"));
                break;
            }
            case "패턴": {
                q = new Question(type, "이번 주 " + ordinal(e.lateCountNow) + " 야식이에요. 주말로 미뤄볼까요?",
                        new Chip("주말로 미룰게요", Decision.STOP),
                        new Chip("오늘 먹을래요", Decision.BUY),
                        new Chip("잘 모르겠어요"));
                break;
            }
            case "과거 연결": {
                String text;
                if (h != null && h.regretTitle != null)
                    text = "지난번 " + a.app.label + "에서 " + h.regretTitle + "을(를) 산 뒤 '후회돼요'라고 답했어요. 이번엔 뭐가 다를까요?";
                else if (e.has(Trigger.LATE_NIGHT) && h != null && h.habitChosen)
                    text = "지난번 야식 때 '습관처럼 시켰어요'를 골랐어요. 오늘은 뭐가 달라요?";
                else if (h != null && h.sameAppCount >= 2)
                    text = "이번 달 " + a.app.label + "에서 벌써 " + h.sameAppCount + "번 결제했어요. 이번엔 뭐가 다를까요?";
                else if (e.zeroOrBelow)
                    text = "급여일까지 " + e.budget.daysLeft + "일 남았어요. 남은 기간을 어떻게 보낼 계획이에요?";
                else
                    text = "일주일 뒤의 나는 이 결제를 어떻게 생각할 것 같아요?";
                q = new Question(type, text);
                q.requireInput = true;
                break;
            }
            default: { // 미래의 나
                String text = e.remainingAfter > 0
                        ? "결제하면 급여일까지 하루 약 " + Money.won(e.dailyAfter) + "으로 지내야 해요. 그래도 지금 " + verb(a) + "?"
                        : "결제하면 이번 달 여윳돈이 " + Money.won(e.remainingAfter) + "이 돼요. 그래도 지금 " + verb(a) + "?";
                q = new Question("미래의 나", text,
                        new Chip("다음으로 미룰게요", Decision.STOP),
                        new Chip(baemin ? "그래도 시킬래요" : "그래도 살래요", Decision.BUY),
                        new Chip("잘 모르겠어요"));
            }
        }
        if (last) ensureDecisionChips(q, baemin);
        return q;
    }

    /** 마지막 질문에는 결정을 담은 칩(미루기·사기)이 있어야 한다. */
    public static void ensureDecisionChips(Question q, boolean baemin) {
        if (q.requireInput) return;
        boolean hasStop = false, hasBuy = false;
        for (Chip c : q.chips) {
            if (c.decision == Decision.STOP) hasStop = true;
            if (c.decision == Decision.BUY) hasBuy = true;
        }
        if (!hasStop) q.chips.add(new Chip("이번엔 미룰게요", Decision.STOP));
        if (!hasBuy) q.chips.add(new Chip(baemin ? "그래도 시킬래요" : "그래도 살래요", Decision.BUY));
    }

    /** 직접 입력한 답에서 결정을 추정한다 (마지막 질문용). */
    public static Decision decisionFromText(String text) {
        if (text == null) return Decision.NONE;
        String t = text.replace(" ", "");
        String[] stop = {"미룰", "미루", "안살", "안시킬", "그만", "참을", "다음에", "주말에", "안먹", "취소"};
        String[] buy = {"살래", "살게", "시킬래", "시킬게", "먹을래", "먹을게", "지금살", "그래도살", "결제할"};
        for (String s : stop) if (t.contains(s)) return Decision.STOP;
        for (String s : buy) if (t.contains(s)) return Decision.BUY;
        return Decision.NONE;
    }
}
