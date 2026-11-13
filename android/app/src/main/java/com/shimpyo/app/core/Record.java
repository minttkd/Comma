package com.shimpyo.app.core;

import com.shimpyo.app.core.Enums.Intensity;
import com.shimpyo.app.core.Enums.Judgment;
import com.shimpyo.app.core.Enums.Outcome;
import com.shimpyo.app.core.Enums.TargetApp;
import com.shimpyo.app.core.Enums.Trigger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** 결제 시도·계좌 내역 한 건. 결과 기록(REQ-9)과 예산 계산의 단위. */
public class Record {
    public String id = UUID.randomUUID().toString();
    public long ts;
    /** 대상 앱. 계좌 내역(외식·마트 등)이면 null. */
    public TargetApp app;
    /** 표시용 이름(상품명 요약 또는 가맹점명). */
    public String title = "";
    public long amount;
    /** 여윳돈에서 나간 금액 (오늘 누적에 반영). */
    public long discretionary;
    /** 식비 예산에서 나간 금액. */
    public long food;
    public Judgment judgment;
    public boolean isFood;
    public EnumSet<Trigger> triggers = EnumSet.noneOf(Trigger.class);
    public Intensity intensity;
    public Outcome outcome = Outcome.PASS;
    /** 가벼움에서 고른 이유 칩. */
    public String chip;
    /** 방지 실적일 때 몇 번째 질문에서 마음을 돌렸는지 (1부터, 질문 전이면 0). */
    public int stoppedAt;
    /** 질문과 답 ("Q: ... / A: ..."). */
    public List<String> qa = new ArrayList<>();
    /** 야식 시간대의 배민 결제 여부. */
    public boolean lateNight;
    /** 다음 날 회고 응답: 잘 샀어요 / 그저 그래요 / 후회돼요. */
    public String review;
    /** 그날 몇 번째 개입이었는지 (개입이 아니면 0). */
    public int interventionNo;

    public boolean isIntervention() {
        return intensity != null;
    }

    public String appLabel() {
        return app == null ? "계좌" : app.label;
    }
}
