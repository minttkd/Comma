package com.shimpyo.app.core;

/** 사용자 설정과 예산 입력값. 기본값은 PRD 시나리오(이수민) 기준. */
public class Settings {
    public boolean onboarded = false;

    // ---- 소득 · 고정지출 · 식비 예산 (REQ-1~3)
    public long income = 1_000_000;
    /** 급여일 (1~28). */
    public int payday = 25;
    /** 테스트용: 이번 주기 시작일을 직접 지정 (epochDay, -1이면 급여일로 계산). 지정 시 주기는 30일. */
    public long cycleStartOverride = -1;
    public long rent = 350_000;       // 월세·관리비
    public long utilities = 50_000;   // 공과금
    public long phone = 45_000;       // 통신비
    public long transport = 40_000;   // 교통비
    public long subscriptions = 15_000; // 구독료
    public long martBudget = 40_000;
    public long coupangFoodBudget = 60_000;

    // ---- 트리거 수치 (PRD 결정 필요 수치)
    public double k = 2.0;
    public long highAbs = 100_000;
    public double highMultipleOfB = 3.0;
    public int bingeWindowMin = 60;
    public int bingeCount = 3;

    // ---- 야식 반복
    public boolean lateEnabled = true;
    public int lateStartHour = 22;
    public int lateEndHour = 4;
    public int lateWindowDays = 7;
    public int lateCount = 4;
    /** 야식 카운트 초기화 시각 (개입이 일어난 시각). */
    public long lateResetTs = 0;

    // ---- AI
    public String apiKey = "";
    public String model = "claude-haiku-5-5";
    public int aiTimeoutMs = 2000;

    // ---- 테스트
    /** 0~23이면 현재 시각의 '시'를 이 값으로 간주 (야식 테스트용). -1이면 실제 시각. */
    public int simulatedHour = -1;
    /** 테스트용: 결제 버튼 가드 영역을 파란 테두리로 보여 준다. */
    public boolean showGuard = true;
    public long lastSyncTs = 0;

    public long fixedTotal() {
        return rent + utilities + phone + transport + subscriptions;
    }

    public long foodBudget() {
        return martBudget + coupangFoodBudget;
    }

    /** 월 여윳돈 = 수입 − 고정지출 − 식비 예산 */
    public long monthlyDiscretionary() {
        return income - fixedTotal() - foodBudget();
    }

    public boolean hasApiKey() {
        return apiKey != null && apiKey.trim().length() > 10;
    }
}
