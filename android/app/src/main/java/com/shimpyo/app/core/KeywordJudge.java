package com.shimpyo.app.core;

import com.shimpyo.app.core.Enums.Judgment;
import com.shimpyo.app.core.Enums.TargetApp;

/**
 * AI 판정이 없거나 2초 안에 오지 않을 때 쓰는 키워드 판정.
 * 판정이 애매하면 '애매'로 둔다 (REQ-6: 응답 실패 시 애매로 간주).
 */
public final class KeywordJudge {
    private KeywordJudge() {}

    static final String[] FOOD = {
            "쌀", "현미", "계란", "달걀", "우유", "두부", "고기", "돼지", "소고기", "닭가슴살", "닭고기", "삼겹",
            "채소", "야채", "양파", "감자", "고구마", "당근", "대파", "마늘", "버섯", "배추", "양배추", "상추",
            "김치", "생수", "과일", "사과", "바나나", "귤", "토마토", "두유", "요거트", "치즈", "버터", "식빵",
            "밀가루", "식용유", "간장", "된장", "고추장", "참치", "햄", "소시지", "냉동밥", "즉석밥", "햇반", "만두",
            "어묵", "새우", "연어", "생선", "오트밀", "시리얼", "견과", "파스타", "국수", "라면"
    };
    /** 식료품이지만 기호품 성격이 강한 것 → 식비 예산이 아닌 여윳돈 + 애매/충동. */
    static final String[] SNACK = {
            "과자", "젤리", "초콜릿", "초코", "사탕", "아이스크림", "탄산", "콜라", "사이다", "에너지드링크",
            "맥주", "소주", "와인", "케이크", "쿠키", "디저트", "스낵", "팝콘"
    };
    static final String[] NEED = {
            "휴지", "화장지", "물티슈", "키친타올", "세제", "섬유유연제", "샴푸", "린스", "바디워시", "비누", "치약",
            "칫솔", "생리대", "면도기", "쓰레기봉투", "종량제", "수세미", "고무장갑", "건전지", "배터리", "충전기",
            "케이블", "마스크", "밴드", "소독", "영양제", "렌즈", "세정제", "방향제", "행주", "랩", "지퍼백"
    };
    static final String[] IMPULSE = {
            "코트", "자켓", "재킷", "패딩", "니트", "가디건", "후드", "맨투맨", "셔츠", "티셔츠", "청바지", "데님",
            "슬랙스", "스커트", "원피스", "스니커즈", "운동화", "부츠", "로퍼", "가방", "백팩", "지갑", "모자", "캡",
            "비니", "목도리", "머플러", "키링", "굿즈", "피규어", "향수", "액세서리", "반지", "목걸이", "귀걸이",
            "시계", "선글라스", "한정", "콜라보", "에디션"
    };

    public static boolean containsAny(String text, String[] words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }

    /** 생활필수품 단어가 들어 있는지. 판정 값이 없는 기록(예시 기록 등)을 리포트 카테고리로 나눌 때 쓴다. */
    public static boolean looksNeed(String text) {
        return text != null && containsAny(text, NEED);
    }

    /** text에는 상품명(또는 화면 텍스트 전체)을 넣는다. */
    public static void judge(PaymentAttempt a, String text) {
        a.judgedBy = "키워드";
        if (a.app == TargetApp.BAEMIN) {
            a.judgment = Judgment.AMBIGUOUS;
            a.isFood = false;
            a.judgedBy = "배민 기본";
            return;
        }
        String t = text == null ? "" : text;
        if (containsAny(t, SNACK)) {
            a.judgment = Judgment.AMBIGUOUS;
            a.isFood = false;
            return;
        }
        if (containsAny(t, FOOD)) {
            a.judgment = Judgment.NEED;
            a.isFood = true;
            return;
        }
        if (containsAny(t, NEED)) {
            a.judgment = Judgment.NEED;
            a.isFood = false;
            return;
        }
        if (containsAny(t, IMPULSE)) {
            a.judgment = Judgment.IMPULSE;
            a.isFood = false;
            return;
        }
        a.judgment = Judgment.AMBIGUOUS;
        a.isFood = false;
    }
}
