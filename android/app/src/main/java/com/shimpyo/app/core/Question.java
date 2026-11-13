package com.shimpyo.app.core;

import com.shimpyo.app.core.Enums.Decision;

import java.util.ArrayList;
import java.util.List;

/** 개입 대화의 질문 하나와 답 칩. */
public class Question {
    public static class Chip {
        public final String label;
        public final Decision decision;

        public Chip(String label, Decision decision) {
            this.label = label;
            this.decision = decision == null ? Decision.NONE : decision;
        }

        public Chip(String label) {
            this(label, Decision.NONE);
        }
    }

    public String text = "";
    public List<Chip> chips = new ArrayList<>();
    /** 직접 입력이 필수인 질문 (강함의 과거 연결 질문). */
    public boolean requireInput;
    /** 직접 입력을 허용 (보통·강함 전체). */
    public boolean allowInput = true;
    /** 질문 유형: 필요vs욕구, 시점, 대안, 미래의 나, 과거 연결, 패턴 */
    public String type = "";
    /** "AI" 또는 "기본 질문" */
    public String source = "기본 질문";

    public Question() {}

    public Question(String type, String text, Chip... chips) {
        this.type = type;
        this.text = text;
        for (Chip c : chips) this.chips.add(c);
    }
}
