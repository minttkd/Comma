package com.shimpyo.app.core;

import com.shimpyo.app.core.Enums.Decision;
import com.shimpyo.app.core.TriggerEngine.Evaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * 결제 화면 하나에 대한 개입 세션. X로 닫았다 다시 열어도 같은 질문부터 이어간다 (REQ-8-6).
 */
public class Session {
    public enum State {
        /** 판정·평가 중 */
        EVALUATING,
        /** 트리거 미해당 — 개입 없이 진행 */
        PASSED,
        /** 오버레이 표시 중 */
        ACTIVE,
        /** X로 닫힘 — 결제 버튼을 다시 누르면 재표시 */
        PAUSED,
        /** 이유 칩 선택 또는 [그래도 살게요] — 결제 진행 허용 */
        ALLOWED,
        /** [그만둘게요] 또는 이탈로 종료 */
        CLOSED
    }

    public final String key;
    public final long createdAt;
    public PaymentAttempt attempt;
    public Evaluation eval;
    public State state = State.EVALUATING;
    public final List<Question> questions = new ArrayList<>();
    public final List<String> answers = new ArrayList<>();
    /** 현재 질문 인덱스 (0부터). 질문 수와 같으면 결정 단계. */
    public int index = 0;
    /** 다음 질문을 AI가 생성 중. */
    public boolean loading;
    /** 결제 화면을 벗어났다가 돌아왔는지 감지용. */
    public boolean leftScreen;
    public long pausedAt;
    /** 비개입 결제 기록을 이미 남겼는지. */
    public boolean recorded;
    /** 쉼표가 결제 버튼 터치를 가로챈 세션인지. true면 개입이 필요 없을 때 터치를 그대로 전달한다. */
    public boolean forwardTap;
    /** [그만둘게요]·'다음에 살게요' 같은 중단 결정으로 끝난 세션인지. */
    public boolean stopped;
    public long stoppedAt;
    /** 같은 앱에서 방금 멈춘 뒤 다시 결제를 시도한 세션인지. true면 첫 화면에 맥락 문구를 붙인다. */
    public boolean retryAfterStop;

    /** 멈춘 뒤 재시도로 보는 시간 (분). */
    public static final long RETRY_WINDOW_MS = 30 * 60_000L;

    public Session(String key, long now) {
        this.key = key;
        this.createdAt = now;
    }

    /** prev가 같은 앱에서 RETRY_WINDOW_MS 안에 '멈춤'으로 끝났으면 새 시도는 재시도다. */
    public static boolean isRetryAfterStop(Session prev, Enums.TargetApp app, long now) {
        return prev != null && prev.stopped && prev.attempt != null && prev.attempt.app == app
                && now - prev.stoppedAt >= 0 && now - prev.stoppedAt < RETRY_WINDOW_MS;
    }

    public int total() {
        return eval == null || eval.intensity == null ? 0 : eval.intensity.questions;
    }

    public boolean inDecisionStep() {
        return index >= total() && total() > 1;
    }

    public Question current() {
        return index < questions.size() ? questions.get(index) : null;
    }

    /** 답을 기록하고 다음 단계로. 반환값: 답이 담은 결정 (마지막 질문에서만 의미 있음). */
    public Decision answer(String text, Decision chipDecision) {
        Question q = current();
        answers.add(text);
        index++;
        boolean wasLast = index >= total();
        if (!wasLast) return Decision.NONE;
        if (chipDecision != null && chipDecision != Decision.NONE) return chipDecision;
        if (q != null && q.requireInput) return Decision.NONE;
        return QuestionTemplates.decisionFromText(text);
    }

    public List<String> transcript() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < answers.size() && i < questions.size(); i++) {
            out.add("Q" + (i + 1) + ". " + questions.get(i).text + " / A. " + answers.get(i));
        }
        return out;
    }
}
