package com.shimpyo.app;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.Consumer;

/**
 * S02 계좌 연결: 은행 목록 · 은행 표시(색 원 + 짧은 글자) · 은행 고르기 격자.
 *
 * 실제 오픈뱅킹 연동 전이라, 은행을 고르면 앱 안의 "은행 앱 연결 화면"(MainActivity.showBankLink)으로
 * 넘어가 계좌를 고르고 동의하는 흐름만 보여 준다. 실제 은행 로고 대신 은행 색 원 + 짧은 글자를 쓴다.
 */
final class Banks {
    private Banks() {}

    static final class Bank {
        final String name;   // 화면 이름 (격자 · 연결된 계좌)
        final String mark;   // 원 안의 짧은 글자
        final int bg, fg;    // 원 바탕 · 글자 색

        Bank(String name, String mark, String bg, String fg) {
            this.name = name;
            this.mark = mark;
            this.bg = Color.parseColor(bg);
            this.fg = Color.parseColor(fg);
        }
    }

    static final Bank[] ALL = {
            new Bank("NH농협", "NH", "#00A851", "#FFFFFF"),
            new Bank("KB국민", "KB", "#FFBC00", "#4A3F35"),
            new Bank("카카오뱅크", "카", "#FFE300", "#1E1E1E"),
            new Bank("신한", "신한", "#0046FF", "#FFFFFF"),
            new Bank("우리", "우리", "#0067AC", "#FFFFFF"),
            new Bank("IBK기업", "IBK", "#1A4FA0", "#FFFFFF"),
            new Bank("하나", "하나", "#008C8C", "#FFFFFF"),
            new Bank("새마을금고", "MG", "#0072BC", "#FFFFFF"),
            new Bank("iM뱅크", "iM", "#00A3E0", "#FFFFFF"),
            new Bank("부산", "BNK", "#E60012", "#FFFFFF"),
            new Bank("케이뱅크", "K", "#2B2BD9", "#FFFFFF"),
            new Bank("우체국", "우체", "#E2231A", "#FFFFFF"),
            new Bank("수협", "Sh", "#0072C6", "#FFFFFF"),
            new Bank("광주", "광주", "#0055A5", "#FFFFFF"),
            new Bank("전북", "전북", "#00539B", "#FFFFFF"),
            new Bank("토스뱅크", "토스", "#0064FF", "#FFFFFF"),
            new Bank("SC제일", "SC", "#0473EA", "#FFFFFF"),
            new Bank("경남", "BNK", "#C8102E", "#FFFFFF"),
    };

    /** "KB국민" → "KB국민은행", "카카오뱅크" · "새마을금고" · "우체국"은 그대로. */
    static String fullName(Bank b) {
        String n = b.name;
        return n.endsWith("뱅크") || n.endsWith("금고") || n.equals("우체국") ? n : n + "은행";
    }

    static Bank byName(String name) {
        for (Bank b : ALL) if (b.name.equals(name)) return b;
        return null;
    }

    /** 은행 색 원 + 짧은 글자. */
    static TextView badge(Context c, Bank b, int sizeDp) {
        float sp = b.mark.length() >= 3 ? sizeDp * 0.26f : b.mark.length() == 2 ? sizeDp * 0.32f : sizeDp * 0.42f;
        TextView t = Ui.text(c, b.mark, sp, b.fg, Ui.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        t.setBackground(Ui.circle(b.bg));
        return t;
    }

    /** 은행 고르기 격자 (3열). 누르면 그 칸만 테두리로 표시하고 pick에 알린다 (화면을 다시 그리지 않아 스크롤 위치 유지). */
    static LinearLayout grid(Context c, Bank selected, Consumer<Bank> pick) {
        LinearLayout g = Ui.v(c);
        java.util.List<LinearLayout> tiles = new java.util.ArrayList<>();
        LinearLayout row = null;
        for (int i = 0; i < ALL.length; i++) {
            if (i % 3 == 0) {
                row = Ui.h(c);
                if (i == 0) g.addView(row, Ui.fillW());
                else Ui.stack(g, row, Ui.Space.ITEM);
            }
            Bank b = ALL[i];
            LinearLayout tile = Ui.v(c);
            tile.setGravity(Gravity.CENTER_HORIZONTAL);
            tile.setBackground(tileBg(c, b == selected));
            Ui.pad(tile, c, 4, 14);
            tile.addView(badge(c, b, 36), Ui.square(c, 36));
            TextView name = Ui.text(c, b.name, Ui.Font.SMALL, Ui.TEXT, Ui.MEDIUM);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            Ui.stack(tile, name, Ui.Space.TIGHT);
            tile.setClickable(true);
            tile.setContentDescription(b.name);
            tile.setOnClickListener(v -> {
                for (int k = 0; k < tiles.size(); k++) tiles.get(k).setBackground(tileBg(c, ALL[k] == b));
                pick.accept(b);
            });
            tiles.add(tile);
            if (i % 3 == 0) row.addView(tile, Ui.weight1());
            else Ui.inlineFill(row, tile, Ui.Space.ITEM);
        }
        return g;
    }

    /** 고른 칸: 먹색 2dp 테두리 · 나머지: 흰 칸. 테두리 두께만큼 높이가 변하지 않게 둘 다 테두리를 둔다. */
    private static android.graphics.drawable.GradientDrawable tileBg(Context c, boolean on) {
        return Ui.roundStroke(c, Ui.WHITE, Ui.Radius.CARD, on ? Ui.TEXT : Ui.WHITE, 2);
    }

    /** 연결된 계좌 카드 안 한 줄: 은행 원 + 이름 + 계좌. */
    static LinearLayout accountRow(Context c, Bank b, String title, String sub) {
        LinearLayout r = Ui.h(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.addView(badge(c, b, 36), Ui.square(c, 36));
        LinearLayout t = Ui.v(c);
        t.addView(Ui.text(c, title, Ui.Font.LABEL, Ui.TEXT, Ui.BOLD));
        Ui.stack(t, Ui.meta(c, sub), Ui.Space.TEXT);
        Ui.inlineFill(r, t, Ui.Space.ROW);
        return r;
    }
}
