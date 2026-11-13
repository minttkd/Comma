package com.shimpyo.app.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Money {
    private Money() {}

    private static final Pattern NUM = Pattern.compile("(\\d{1,3}(?:,\\d{3})+|\\d+)");

    /** 23000 → "23,000원" */
    public static String won(long v) {
        return String.format(Locale.KOREA, "%,d원", v);
    }

    /** 23000 → "23,000" */
    public static String num(long v) {
        return String.format(Locale.KOREA, "%,d", v);
    }

    /** "하루 약 7,500원" 표기용: 100원 단위로 내림 (실제 쓸 수 있는 돈보다 크게 말하지 않도록). */
    public static long round100(double v) {
        return (long) Math.floor(v / 100.0) * 100L;
    }

    /** "23,000원" 같은 문자열에서 첫 숫자를 꺼낸다. 없으면 -1. */
    public static long parse(String s) {
        if (s == null) return -1;
        Matcher m = NUM.matcher(s);
        if (!m.find()) return -1;
        try {
            return Long.parseLong(m.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
