package com.shimpyo.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 앱 안에서 바꿔 볼 수 있는 UI 방식 (UI 갤러리 › "앱 UI 설정").
 *
 * 디자인을 비교해 보려고 둔 스위치다. 값은 프론트 전용 저장소("ui_prefs")에 두어 Store(설정·기록)와 섞이지 않는다.
 * 각 enum의 ①이 기본이고, 나머지는 비교용 후보다. "기본값으로" 버튼으로 언제든 ①로 돌아간다.
 * 개입 채팅 · 리포트는 아직 팀이 고르는 중이라 ①이 "지금 화면"이다. 정해지면 그 값을 ①로 옮긴다.
 * 새 방식을 추가하려면: 여기 enum에 값을 넣고 → 그리는 코드에서 분기 → UiGalleryActivity.uiSettings()에 칩을 추가한다.
 */
final class UiPrefs {
    private UiPrefs() {}

    /** 화면 모드 (다크 모드). Ui.applyTheme()가 읽는다. */
    enum Theme {
        SYSTEM("① 자동"),   // 휴대폰 설정이 다크 모드면 어둡게
        LIGHT("② 밝게"),
        DARK("③ 어둡게");

        final String label;

        Theme(String label) {
            this.label = label;
        }
    }

    /** 홈 "오늘 쓸 수 있는 돈" 카드. */
    enum TodayCard {
        SIGNAL("① 상태 색"),  // 기본
        HERO("② 먹색"),
        OUTLINE("③ 흰색");

        final String label;

        TodayCard(String label) {
            this.label = label;
        }
    }

    /** 기록 줄(홈 최근 기록 · 기록 탭)의 결과 색 표시. */
    enum RecordColor {
        TINT("① 줄 바탕"),    // 기본
        DOT("② 점"),
        BAR("③ 왼쪽 막대"),
        NONE("④ 없음");

        final String label;

        RecordColor(String label) {
            this.label = label;
        }
    }

    /** 개입 채팅(결제 화면 위 대화) 모양. */
    enum ChatStyle {
        NOW("① 지금"),     // 트리거·강도 태그 + 회색 말풍선
        SOFT("② 부드럽게"), // 쉼표 얼굴 + 연초록 말풍선, 태그 대신 앱·금액, 진행은 점
        BRAND("③ 쉼표 색"); // ②에 더해 시트 바탕을 연초록으로

        final String label;

        ChatStyle(String label) {
            this.label = label;
        }
    }

    /** 리포트(S10) 내용. ②~④는 사용자가 직접 한 판단(고른 이유 · 다음 날 만족/후회)을 중심으로. ReportViews 참고. */
    enum ReportStyle {
        NOW("① 지금"),        // 마음 돌림 비율 · 강도별 · 결과 분류
        VALUES("② 만족·후회"), // 다음 날 돌아본 마음 + 고른 이유
        STORY("③ 한 달 이야기"), // 문장 카드 몇 개
        SIMPLE("④ 숫자 세 개"); // 아낀 돈 · 만족한 소비 · 후회한 소비

        final String label;

        ReportStyle(String label) {
            this.label = label;
        }
    }

    // ------------------------------------------------------------------ 현재 값 (기본값 = 지금 디자인)

    static Theme theme = Theme.SYSTEM;
    static TodayCard todayCard = TodayCard.SIGNAL;
    static boolean tileBars = true;     // 홈 남은 여윳돈 · 식비 예산 진행 막대
    static boolean cardOutline = true;  // 테두리 카드(홈 타일 · 기록 목록)의 회색 테두리
    static RecordColor recordColor = RecordColor.TINT;
    static ChatStyle chatStyle = ChatStyle.NOW;
    static ReportStyle reportStyle = ReportStyle.NOW;

    // ------------------------------------------------------------------ 저장 · 불러오기

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("ui_prefs", Context.MODE_PRIVATE);
    }

    static void load(Context c) {
        SharedPreferences p = prefs(c);
        theme = valueOf(Theme.class, p.getString("theme", null), Theme.SYSTEM);
        todayCard = valueOf(TodayCard.class, p.getString("todayCard", null), TodayCard.SIGNAL);
        tileBars = p.getBoolean("tileBars", true);
        cardOutline = p.getBoolean("cardOutline", true);
        recordColor = valueOf(RecordColor.class, p.getString("recordColor", null), RecordColor.TINT);
        chatStyle = valueOf(ChatStyle.class, p.getString("chatStyle", null), ChatStyle.NOW);
        reportStyle = valueOf(ReportStyle.class, p.getString("reportStyle", null), ReportStyle.NOW);
    }

    static void save(Context c) {
        prefs(c).edit()
                .putString("theme", theme.name())
                .putString("todayCard", todayCard.name())
                .putBoolean("tileBars", tileBars)
                .putBoolean("cardOutline", cardOutline)
                .putString("recordColor", recordColor.name())
                .putString("chatStyle", chatStyle.name())
                .putString("reportStyle", reportStyle.name())
                .apply();
    }

    static void reset(Context c) {
        prefs(c).edit().clear().apply();
        load(c);
    }

    private static <E extends Enum<E>> E valueOf(Class<E> type, String name, E def) {
        if (name == null) return def;
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
