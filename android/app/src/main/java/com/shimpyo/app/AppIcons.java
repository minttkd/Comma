package com.shimpyo.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;

import com.shimpyo.app.core.Enums.TargetApp;

import java.util.EnumMap;
import java.util.Map;

/**
 * 결제 기록 줄 왼쪽 앱 아이콘.
 * 휴대폰에 깔린 배민 · 쿠팡 · 무신사 앱의 아이콘을 PackageManager로 그대로 가져온다 (로고를 따로 그려 넣지 않는다).
 * 앱이 없으면(에뮬레이터 · 데모) 앱 이름 첫 글자 배지로 대신한다.
 */
final class AppIcons {
    private AppIcons() {}

    private static final Map<TargetApp, Drawable.ConstantState> CACHE = new EnumMap<>(TargetApp.class);
    private static final Map<TargetApp, Boolean> MISSING = new EnumMap<>(TargetApp.class);

    /** 앱 아이콘 뷰. app이 null이면 계좌 동기화 기록(₩ 배지). square(c, size)로 붙인다. */
    static View icon(Context c, TargetApp app, int sizeDp) {
        Drawable d = app == null ? null : installed(c, app);
        View v;
        if (d != null) {
            ImageView iv = new ImageView(c);
            iv.setImageDrawable(d);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            v = iv;
        } else {
            v = badge(c, app, sizeDp);
        }
        final float r = Ui.dp(c, sizeDp * 0.24f);
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline o) {
                o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
            }
        });
        v.setClipToOutline(true);
        v.setContentDescription(app == null ? "계좌" : app.label);
        return v;
    }

    private static Drawable installed(Context c, TargetApp app) {
        Drawable.ConstantState cs = CACHE.get(app);
        if (cs != null) return cs.newDrawable(c.getResources()).mutate();
        if (Boolean.TRUE.equals(MISSING.get(app))) return null;
        try {
            Drawable d = c.getPackageManager().getApplicationIcon(app.pkg);
            if (d.getConstantState() != null) CACHE.put(app, d.getConstantState());
            return d;
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            MISSING.put(app, true);
            return null;
        }
    }

    /** 앱이 없을 때: 앱 색 바탕 + 첫 글자. */
    private static TextView badge(Context c, TargetApp app, int sizeDp) {
        String letter;
        int bg;
        int fg = Color.WHITE;
        if (app == null) {
            letter = "₩";
            bg = Ui.BRAND_LIGHT;
            fg = Ui.GREEN;
        } else {
            switch (app) {
                case BAEMIN: letter = "배"; bg = Color.parseColor("#2AC1BC"); break;
                case COUPANG: letter = "쿠"; bg = Color.parseColor("#346AFF"); break;
                case MUSINSA: letter = "무"; bg = Color.parseColor("#111111"); break;
                default: letter = app.label.substring(0, 1); bg = Ui.GREEN; break;
            }
        }
        TextView t = Ui.text(c, letter, sizeDp * 0.42f, fg, Ui.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        t.setBackgroundColor(bg);
        return t;
    }

    /** 이 앱들이 깔려 있는지 미리 알 수 없으니, 다시 깔았을 때 새로 찾도록 비운다. */
    static void clear() {
        CACHE.clear();
        MISSING.clear();
    }
}
