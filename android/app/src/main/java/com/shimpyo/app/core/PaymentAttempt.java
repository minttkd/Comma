package com.shimpyo.app.core;

import com.shimpyo.app.core.Enums.Judgment;
import com.shimpyo.app.core.Enums.TargetApp;

import java.util.ArrayList;
import java.util.List;

/** 결제 준비 화면에서 읽어낸 결제 시도 한 건 + 품목 판정 결과. */
public class PaymentAttempt {
    public TargetApp app;
    public long amount;
    public String title = "";
    public List<String> items = new ArrayList<>();
    /** 배민은 판정 없이 애매·충동으로 취급 → AMBIGUOUS로 둔다. */
    public Judgment judgment = Judgment.AMBIGUOUS;
    public boolean isFood;
    /** "AI", "키워드", "배민 기본" */
    public String judgedBy = "";
    /** 화면에서 읽은 전체 텍스트(디버그·AI 판정용). */
    public String screenText = "";
    public long ts;

    public String shortTitle() {
        if (title != null && !title.isEmpty()) return title;
        if (!items.isEmpty()) return items.get(0);
        return app == null ? "결제" : app.label + " 주문";
    }
}
