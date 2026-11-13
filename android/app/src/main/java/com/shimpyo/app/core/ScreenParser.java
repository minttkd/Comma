package com.shimpyo.app.core;

import com.shimpyo.app.core.Enums.TargetApp;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 접근성 트리에서 모은 텍스트 목록으로 '결제 준비 화면'인지 판단하고 금액·상품명을 뽑는다.
 * 세 앱의 화면 구조가 바뀔 수 있으므로 앱별 하드코딩 대신 공통 신호(결제 버튼 + 결제 섹션 라벨)를 쓴다.
 */
public final class ScreenParser {
    private ScreenParser() {}

    public static final class Result {
        public boolean checkout;
        public boolean complete;
        public long amount = -1;
        public String payButton;
        public List<String> items = new ArrayList<>();
        public String joined = "";
        public String reason = "";
    }

    static final Pattern PAY_WITH_AMOUNT = Pattern.compile("([\\d,]{3,})\\s*원\\s*(?:배달\\s*)?(?:결제하기|결제|주문하기|구매하기)");
    static final Pattern WON = Pattern.compile("([\\d]{1,3}(?:,\\d{3})+|\\d{3,})\\s*원");
    static final Pattern QTY = Pattern.compile("(수량\\s*\\d+|\\d+\\s*개$|\\d+개\\s*[·/]|/\\s*\\d+\\s*개)");

    /** 결제 준비 화면 신호 1: 결제 버튼. 상품 상세의 '구매하기'는 결제 준비 화면이 아니므로 넣지 않는다. */
    static final String[] PAY_BUTTONS = {"결제하기", "원 결제"};
    /** 결제 준비 화면 신호 2: 결제 섹션 라벨 */
    static final String[] CHECKOUT_LABELS = {"결제수단", "결제 수단", "결제정보", "결제 정보", "결제금액", "결제 금액",
            "총결제금액", "총 결제금액", "최종 결제", "주문서", "주문/결제", "결제방법", "결제 방법"};
    static final String[] TOTAL_LABELS = {"총결제금액", "총 결제금액", "총 결제 금액", "최종 결제금액", "최종 결제 금액",
            "결제예정금액", "결제 예정 금액", "총 주문금액", "총 주문 금액", "결제금액", "결제 금액"};
    static final String[] COMPLETE = {"주문완료", "주문 완료", "결제완료", "결제 완료", "주문이 완료", "주문이 접수",
            "주문 접수", "결제가 완료", "주문해주셔서"};
    /** 상품명 후보에서 빼는 단어 (섹션·버튼·안내 문구) */
    static final String[] NOT_ITEM = {"결제", "배송", "배달", "쿠폰", "적립", "주소", "요청", "동의", "약관", "카드", "페이",
            "할인", "포인트", "캐시", "변경", "선택", "수량", "옵션", "주문", "합계", "총", "금액", "받는", "도착", "보장",
            "전화", "연락처", "휴대폰", "이름", "기본", "닫기", "뒤로", "확인", "취소", "안내", "혜택", "정보", "필수",
            "무료", "멤버십", "와우", "클럽", "가게", "사장님", "라이더", "포장", "더보기", "펼치기", "접기", "상세",
            "%", "원", "LTE", "Wi-Fi"};

    public static Result parse(TargetApp app, List<String> texts) {
        Result r = new Result();
        StringBuilder sb = new StringBuilder();
        for (String t : texts) {
            if (t == null) continue;
            String s = t.trim();
            if (s.isEmpty()) continue;
            sb.append(s).append('\n');
        }
        r.joined = sb.toString();
        String all = r.joined;

        for (String c : COMPLETE) {
            if (all.contains(c)) {
                r.complete = true;
                break;
            }
        }

        boolean hasPay = false;
        for (String t : texts) {
            if (isPayButtonText(t)) {
                hasPay = true;
                if (r.payButton == null) r.payButton = t.trim();
            }
        }
        boolean hasLabel = false;
        for (String l : CHECKOUT_LABELS) {
            if (all.contains(l)) {
                hasLabel = true;
                break;
            }
        }
        r.checkout = hasPay && hasLabel && !r.complete;
        r.reason = "결제버튼=" + hasPay + ", 결제섹션=" + hasLabel + ", 완료화면=" + r.complete;

        r.amount = findAmount(texts);
        r.items = findItems(texts);
        return r;
    }

    /** 결제 버튼 문구인지. 상품 상세의 '구매하기'는 제외한다. */
    public static boolean isPayButtonText(String t) {
        if (t == null) return false;
        String s = t.trim();
        if (s.isEmpty() || s.length() > 30) return false;
        for (String p : PAY_BUTTONS) if (s.contains(p)) return true;
        return false;
    }

    static long findAmount(List<String> texts) {
        // 1순위: "23,000원 결제하기" 버튼
        for (String t : texts) {
            if (t == null) continue;
            Matcher m = PAY_WITH_AMOUNT.matcher(t);
            if (m.find()) {
                long v = Money.parse(m.group(1));
                if (v > 0) return v;
            }
        }
        // 2순위: "총 결제금액" 라벨 다음 1~3개 텍스트 안의 금액
        for (int i = 0; i < texts.size(); i++) {
            String t = texts.get(i);
            if (t == null) continue;
            String compact = t.replace(" ", "");
            boolean label = false;
            for (String l : TOTAL_LABELS) {
                if (compact.startsWith(l.replace(" ", ""))) {
                    label = true;
                    break;
                }
            }
            if (!label) continue;
            Matcher same = WON.matcher(t);
            if (same.find()) return Money.parse(same.group(1));
            for (int j = i + 1; j < Math.min(texts.size(), i + 4); j++) {
                String n = texts.get(j);
                if (n == null) continue;
                Matcher m = WON.matcher(n);
                if (m.find()) return Money.parse(m.group(1));
            }
        }
        // 3순위: 화면에서 가장 큰 금액
        long max = -1;
        for (String t : texts) {
            if (t == null) continue;
            Matcher m = WON.matcher(t);
            while (m.find()) {
                long v = Money.parse(m.group(1));
                if (v > max && v < 10_000_000) max = v;
            }
        }
        return max;
    }

    static boolean itemLike(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.length() < 2 || t.length() > 60) return false;
        if (t.matches(".*\\d{1,2}:\\d{2}.*")) return false;
        if (t.matches("^[\\d,\\s원:·/()-]+$")) return false;
        for (String w : NOT_ITEM) if (t.contains(w)) return false;
        return true;
    }

    static List<String> findItems(List<String> texts) {
        List<String> out = new ArrayList<>();
        // 수량 표기 바로 앞의 텍스트를 상품명으로 본다.
        for (int i = 1; i < texts.size(); i++) {
            String t = texts.get(i);
            if (t == null || !QTY.matcher(t.trim()).find()) continue;
            for (int k = i - 1; k >= Math.max(0, i - 3); k--) {
                String prev = texts.get(k);
                if (itemLike(prev)) {
                    String p = prev.trim();
                    if (!out.contains(p)) out.add(p);
                    break;
                }
            }
            if (out.size() >= 5) break;
        }
        return out;
    }
}
