package com.shimpyo.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * 숨숨이: 쉼표 잎을 단 하얀 찹쌀떡. 서비스 캐릭터.
 * 좌표는 캐릭터 시안(SVG, 가로 220 · 세로 240 판)을 그대로 옮겼다.
 * compact=true면 몸통만 꽉 차게 잘라 작은 얼굴(채팅 · 머리줄)로 쓴다.
 */
final class Sumsumi extends View {

    enum Mood { NORMAL, BREATHE, CURIOUS, PROUD, OKAY, WORRIED }

    private static final int INK = Color.parseColor("#292D2B");
    private static final int LEAF = Color.parseColor("#37624C");
    private static final int SHINE = Color.parseColor("#EAF1EC");
    private static final int CHEEK = Color.parseColor("#F2C2B0");
    private static final int SPARK = Color.parseColor("#1F7A72");

    private static final String BODY = "M30 150 C30 96 62 70 100 70 C138 70 170 96 170 150 C170 188 140 204 100 204 C60 204 30 188 30 150 Z";
    private static final String STEM = "M104 70 C104 62 106 56 110 50";
    private static final String COMMA = "M100 33 C134 33 162 61 162 95 C162 146 132 183 86 204 C80 207 75 200 80 196 C104 178 117 160 121 147 C114 152 107 157 100 157 C66 157 38 129 38 95 C38 61 66 33 100 33 Z";
    private static final String STAR = "M0 -10 L2.6 -2.6 L10 0 L2.6 2.6 L0 10 L-2.6 2.6 L-10 0 L-2.6 -2.6 Z";

    /** 전체 판 (발 그림자 · 반짝이까지) */
    private static final RectF FULL = new RectF(0, 30, 220, 226);
    /** 작은 얼굴용: 잎 끝부터 발까지 */
    private static final RectF COMPACT = new RectF(24, 34, 176, 214);

    /** 손에 든 소품 (리포트 입구: 가장 많이 쓴 카테고리). */
    enum Prop { NONE, DRUMSTICK, SUNGLASSES, BOX, CARROT, FORK }

    /** 소품이 몸 오른쪽으로 삐져나오니 판을 넓힌다. */
    private static final RectF PROP_COMPACT = new RectF(22, 30, 216, 220);
    private static final int MEAT = Color.parseColor("#C98A4B");
    private static final int CARDBOARD = Color.parseColor("#D9B48A");
    private static final int TAPE = Color.parseColor("#EFD9BC");
    private static final int CARROT_ORANGE = Color.parseColor("#F08A3C");
    private static final int SPROUT = Color.parseColor("#5E9E6E");

    private Prop prop = Prop.NONE;
    private Mood mood;
    private final boolean compact;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    Sumsumi(Context c, Mood mood, boolean compact) {
        super(c);
        this.mood = mood;
        this.compact = compact;
        fill.setStyle(Paint.Style.FILL);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        setContentDescription("숨숨이");
    }

    Sumsumi prop(Prop p) {
        prop = p;
        invalidate();
        return this;
    }

    void setMood(Mood m) {
        mood = m;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        RectF box = compact ? (prop != Prop.NONE ? PROP_COMPACT : COMPACT) : FULL;
        float w = getWidth() - getPaddingLeft() - getPaddingRight();
        float h = getHeight() - getPaddingTop() - getPaddingBottom();
        if (w <= 0 || h <= 0) return;
        float s = Math.min(w / box.width(), h / box.height());
        canvas.save();
        canvas.translate(getPaddingLeft() + (w - box.width() * s) / 2f, getPaddingTop() + (h - box.height() * s) / 2f);
        canvas.scale(s, s);
        canvas.translate(-box.left, -box.top);

        // 작게 그리면 선이 너무 가늘어지니 화면에서 최소 1.3dp는 남긴다.
        float minStroke = 1.3f * getResources().getDisplayMetrics().density / s;
        float outline = Math.max(5f, minStroke);
        float feature = Math.max(3.5f, minStroke);

        if (!compact) {
            fill.setColor(INK);
            fill.setAlpha(20);
            ellipse(canvas, fill, 100, 218, 60, 6);
            fill.setAlpha(255);
        }
        // 줄기 · 쉼표 잎
        line.setColor(LEAF);
        line.setStrokeWidth(Math.max(4f, minStroke));
        canvas.drawPath(path(STEM), line);
        Path leaf = path(COMMA);
        Matrix m = new Matrix();
        m.setScale(0.26f, 0.26f);
        m.postTranslate(88, 10);
        leaf.transform(m);
        fill.setColor(LEAF);
        canvas.drawPath(leaf, fill);
        // 발
        fill.setColor(INK);
        ellipse(canvas, fill, 78, 204, 14, 8);
        ellipse(canvas, fill, 122, 204, 14, 8);
        // 몸통
        Path body = path(BODY);
        fill.setColor(Color.WHITE);
        canvas.drawPath(body, fill);
        line.setColor(INK);
        line.setStrokeWidth(outline);
        canvas.drawPath(body, line);
        // 윤기
        fill.setColor(SHINE);
        canvas.save();
        canvas.rotate(-25, 68, 102);
        ellipse(canvas, fill, 68, 102, 12, 6);
        canvas.restore();
        // 볼
        fill.setColor(CHEEK);
        ellipse(canvas, fill, 64, 158, 9, 5);
        ellipse(canvas, fill, 136, 158, 9, 5);

        face(canvas, feature);
        drawProp(canvas, outline, feature);
        if (!compact && mood == Mood.PROUD) {
            fill.setColor(SPARK);
            star(canvas, 178, 80, 1f);
            star(canvas, 196, 110, 0.6f);
            star(canvas, 28, 90, 0.5f);
        }
        canvas.restore();
    }

    private void face(Canvas c, float sw) {
        fill.setColor(INK);
        line.setColor(INK);
        line.setStrokeWidth(sw);
        if (prop == Prop.SUNGLASSES) {
            // 선글라스: 두 알 + 다리, 웃는 입
            RectF l = new RectF(64, 130, 95, 150), r = new RectF(105, 130, 136, 150);
            c.drawRoundRect(l, 8, 8, fill);
            c.drawRoundRect(r, 8, 8, fill);
            c.drawPath(path("M95 136 L105 136"), line);
            line.setColor(Color.WHITE);
            line.setStrokeWidth(sw * 0.7f);
            c.drawPath(path("M70 136 L78 136"), line);
            c.drawPath(path("M111 136 L119 136"), line);
            line.setColor(INK);
            line.setStrokeWidth(sw);
            c.drawPath(path("M92 158 Q100 167 108 158"), line);
            return;
        }
        switch (mood) {
            case BREATHE:
                c.drawPath(path("M73 140 Q80 146 87 140"), line);
                c.drawPath(path("M113 140 Q120 146 127 140"), line);
                c.drawCircle(100, 160, 4, fill);
                break;
            case CURIOUS:
                ellipse(c, fill, 80, 140, 4.5f, 6.5f);
                ellipse(c, fill, 120, 139, 5.5f, 7.5f);
                c.drawPath(path("M112 126 L126 123"), line);
                c.drawPath(path("M95 160 L105 159"), line);
                break;
            case PROUD:
                c.drawPath(path("M73 143 Q80 133 87 143"), line);
                c.drawPath(path("M113 143 Q120 133 127 143"), line);
                c.drawPath(path("M91 157 Q100 173 109 157 Z"), fill);
                break;
            case WORRIED:
                ellipse(c, fill, 80, 142, 4f, 5.5f);
                ellipse(c, fill, 120, 142, 4f, 5.5f);
                line.setStrokeWidth(Math.max(3f, sw * 0.86f));
                c.drawPath(path("M87 128 L74 132"), line);
                c.drawPath(path("M113 128 L126 132"), line);
                line.setStrokeWidth(sw);
                c.drawPath(path("M91 162 Q95.5 157 100 162 Q104.5 167 109 162"), line);
                break;
            case OKAY:
                c.drawPath(path("M74 142 Q80 135 86 142"), line);
                c.drawPath(path("M114 142 Q120 135 126 142"), line);
                c.drawPath(path("M92 158 Q100 167 108 158"), line);
                break;
            case NORMAL:
            default:
                ellipse(c, fill, 80, 140, 4.5f, 6.5f);
                ellipse(c, fill, 120, 140, 4.5f, 6.5f);
                c.drawPath(path("M93 158 Q100 165 107 158"), line);
                break;
        }
    }

    /** 몸 오른쪽 아래에 든 소품. 먹색 테두리로 몸과 같은 그림체. */
    private void drawProp(Canvas c, float outline, float sw) {
        if (prop == Prop.NONE || prop == Prop.SUNGLASSES) return;
        // 작은 카드에서도 보이게 소품만 1.4배 (몸 오른쪽 아래를 기준으로)
        c.save();
        c.scale(1.4f, 1.4f, 165, 180);
        sw /= 1.4f;
        line.setColor(INK);
        line.setStrokeWidth(sw);
        switch (prop) {
            case DRUMSTICK: {
                // 뼈: 먹색 굵은 선 위에 흰 선, 끝에 동그라미 두 개
                line.setStrokeWidth(sw + 9);
                c.drawPath(path("M166 170 L186 146"), line);
                fill.setColor(INK);
                c.drawCircle(184, 140, 7 + sw / 2, fill);
                c.drawCircle(192, 148, 7 + sw / 2, fill);
                fill.setColor(Color.WHITE);
                c.drawCircle(184, 140, 7, fill);
                c.drawCircle(192, 148, 7, fill);
                line.setColor(Color.WHITE);
                line.setStrokeWidth(9);
                c.drawPath(path("M166 170 L188 144"), line);
                // 살코기
                c.save();
                c.rotate(-40, 158, 182);
                fill.setColor(MEAT);
                ellipse(c, fill, 158, 182, 20, 15);
                line.setColor(INK);
                line.setStrokeWidth(sw);
                oval.set(138, 167, 178, 197);
                c.drawOval(oval, line);
                c.restore();
                break;
            }
            case BOX: {
                RectF b = new RectF(138, 158, 190, 206);
                fill.setColor(CARDBOARD);
                c.drawRoundRect(b, 4, 4, fill);
                fill.setColor(TAPE);
                c.drawRect(159, 158, 169, 206, fill);
                line.setStrokeWidth(sw);
                c.drawRoundRect(b, 4, 4, line);
                c.drawPath(path("M138 172 L190 172"), line);
                break;
            }
            case CARROT: {
                line.setColor(SPROUT);
                line.setStrokeWidth(sw + 1);
                c.drawPath(path("M180 150 L186 128"), line);
                c.drawPath(path("M184 154 L200 138"), line);
                c.drawPath(path("M178 150 L170 132"), line);
                Path carrot = path("M170 146 Q196 150 192 168 L150 208 Q142 206 144 198 Z");
                fill.setColor(CARROT_ORANGE);
                c.drawPath(carrot, fill);
                line.setColor(INK);
                line.setStrokeWidth(sw);
                c.drawPath(carrot, line);
                c.drawPath(path("M172 170 L178 175"), line);
                c.drawPath(path("M162 184 L168 189"), line);
                break;
            }
            case FORK: {
                line.setStrokeWidth(sw + 1.5f);
                c.drawPath(path("M180 148 L180 206"), line);
                line.setStrokeWidth(sw);
                c.drawPath(path("M170 124 L170 140 Q170 150 180 150 Q190 150 190 140 L190 124"), line);
                c.drawPath(path("M180 124 L180 146"), line);
                break;
            }
            default:
                break;
        }
        c.restore();
    }

    private void star(Canvas c, float x, float y, float scale) {
        Path p = path(STAR);
        Matrix m = new Matrix();
        m.setScale(scale, scale);
        m.postTranslate(x, y);
        p.transform(m);
        c.drawPath(p, fill);
    }

    private void ellipse(Canvas c, Paint p, float cx, float cy, float rx, float ry) {
        oval.set(cx - rx, cy - ry, cx + rx, cy + ry);
        c.drawOval(oval, p);
    }

    /** 절대 좌표 M · L · Q · C · Z만 쓰는 시안 경로를 Path로. */
    static Path path(String d) {
        Path p = new Path();
        String[] t = d.trim().split("[\\s,]+");
        int i = 0;
        char cmd = 'M';
        while (i < t.length) {
            String tok = t[i];
            if (Character.isLetter(tok.charAt(0))) {
                cmd = tok.charAt(0);
                tok = tok.substring(1);
                if (cmd == 'Z') {
                    p.close();
                    i++;
                    continue;
                }
                if (tok.isEmpty()) {
                    i++;
                    tok = t[i];
                }
            }
            float[] n = new float[6];
            int need = cmd == 'C' ? 6 : cmd == 'Q' ? 4 : 2;
            n[0] = Float.parseFloat(tok);
            for (int k = 1; k < need; k++) n[k] = Float.parseFloat(t[i + k]);
            i += need;
            switch (cmd) {
                case 'M': p.moveTo(n[0], n[1]); cmd = 'L'; break;
                case 'L': p.lineTo(n[0], n[1]); break;
                case 'Q': p.quadTo(n[0], n[1], n[2], n[3]); break;
                case 'C': p.cubicTo(n[0], n[1], n[2], n[3], n[4], n[5]); break;
                default: break;
            }
        }
        return p;
    }
}
