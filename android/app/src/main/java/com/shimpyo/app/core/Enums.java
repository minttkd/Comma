package com.shimpyo.app.core;

/** 쉼표 전역에서 쓰는 열거형 모음. */
public final class Enums {
    private Enums() {}

    /** 대상 앱. 패키지명은 Google Play 기준. */
    public enum TargetApp {
        BAEMIN("com.sampleapp", "배달의민족"),
        COUPANG("com.coupang.mobile", "쿠팡"),
        MUSINSA("com.musinsa.store", "무신사");

        public final String pkg;
        public final String label;

        TargetApp(String pkg, String label) {
            this.pkg = pkg;
            this.label = label;
        }

        public static TargetApp fromPackage(CharSequence pkg) {
            if (pkg == null) return null;
            String p = pkg.toString();
            for (TargetApp a : values()) if (a.pkg.equals(p)) return a;
            return null;
        }

        public static TargetApp fromName(String name) {
            if (name == null) return null;
            for (TargetApp a : values()) if (a.name().equals(name)) return a;
            return null;
        }
    }

    /** AI 품목 판정 범주. */
    public enum Judgment {
        NEED("필요"), AMBIGUOUS("애매"), IMPULSE("충동");
        public final String label;
        Judgment(String label) { this.label = label; }

        public static Judgment fromLabel(String s) {
            if (s == null) return null;
            String t = s.trim();
            for (Judgment j : values()) if (j.label.equals(t) || j.name().equalsIgnoreCase(t)) return j;
            return null;
        }
    }

    /** 개입 트리거. 재정 트리거 2개 + 행동 패턴 트리거 2개. */
    public enum Trigger {
        FIN_RISK("재정위험 전환점", true),
        HIGH_AMOUNT("고액 단건", true),
        BINGE("몰아치기", false),
        LATE_NIGHT("야식 반복", false);

        public final String label;
        public final boolean financial;
        Trigger(String label, boolean financial) { this.label = label; this.financial = financial; }
    }

    /** 개입 강도. */
    public enum Intensity {
        LIGHT("가벼움", 1), MEDIUM("보통", 2), STRONG("강함", 3);
        public final String label;
        /** 가벼움은 고정 질문 1개(이유 칩), 보통 2개, 강함 3개. */
        public final int questions;
        Intensity(String label, int questions) { this.label = label; this.questions = questions; }

        public Intensity up() { return this == LIGHT ? MEDIUM : STRONG; }
        public Intensity down() { return this == STRONG ? MEDIUM : LIGHT; }
    }

    /** 결과 기록 분류 (REQ-9). */
    public enum Outcome {
        PREVENTED("방지 실적", false),
        REASON_PAY("이유 선택 결제", true),
        DELIBERATE_PAY("숙고 후 결제", true),
        EXIT("자기결정 이탈", false),
        PASS("비개입", true),
        SYNC("계좌 내역", true);

        public final String label;
        /** 실제 지출로 예산에 반영하는지. */
        public final boolean spend;
        Outcome(String label, boolean spend) { this.label = label; this.spend = spend; }
    }

    /** 질문 답(칩)이 담은 결정. */
    public enum Decision { NONE, STOP, BUY }
}
