package com.shimpyo.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 앱 안에서 바꿔 볼 수 있는 UI 방식 (UI 갤러리 › "앱 UI 설정").
 *
 * 디자인을 비교해 보려고 둔 스위치다. 값은 프론트 전용 저장소("ui_prefs")에 두어 Store(설정·기록)와 섞이지 않는다.
 * 각 enum의 ①이 팀이 정한 기본 디자인이고, 나머지는 비교용 후보다. "기본값으로" 버튼으로 언제든 ①로 돌아간다.
 * 새 방식을 추가하려면: 여기 enum에 값을 넣고 → 그리는 코드에서 분기 → UiGalleryActivity.uiSettings()에 칩을 추가한다.
 */
final class UiPrefs {
    private UiPrefs() {}

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

    // ------------------------------------------------------------------ 현재 값 (기본값 = 지금 디자인)

    static TodayCard todayCard = TodayCard.SIGNAL;
    static boolean tileBars = true;     // 홈 남은 여윳돈 · 식비 예산 진행 막대
    static boolean cardOutline = true;  // 테두리 카드(홈 타일 · 기록 목록)의 회색 테두리
    static RecordColor recordColor = RecordColor.TINT;

    // ------------------------------------------------------------------ 저장 · 불러오기

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("ui_prefs", Context.MODE_PRIVATE);
    }

    static void load(Context c) {
        SharedPreferences p = prefs(c);
        todayCard = valueOf(TodayCard.class, p.getString("todayCard", null), TodayCard.SIGNAL);
        tileBars = p.getBoolean("tileBars", true);
        cardOutline = p.getBoolean("cardOutline", true);
        recordColor = valueOf(RecordColor.class, p.getString("recordColor", null), RecordColor.TINT);
    }

    static void save(Context c) {
        prefs(c).edit()
                .putString("todayCard", todayCard.name())
                .putBoolean("tileBars", tileBars)
                .putBoolean("cardOutline", cardOutline)
                .putString("recordColor", recordColor.name())
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
