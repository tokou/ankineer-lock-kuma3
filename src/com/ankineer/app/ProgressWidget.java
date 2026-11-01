package com.ankineer.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Small floating bar at the top of the screen:
 *  - daily goal: blue bar = cards, purple bar = minutes (only those the goal mode counts), % on the right;
 *  - session: one orange bar + what's left on the right;
 * tap = open Ankineer · drag = move (position remembered).
 */
final class ProgressWidget {
    static final int C_CARDS = 0xFF5B8CFF, C_MINUTES = 0xFFB16CFF, C_SESSION = 0xFFFFB547, C_DONE = 0xFF3DDC97, C_EMERGENCY = 0xFFFF5A5A;

    private final AccessibilityService svc;
    private final WindowManager wm;
    private Pill pill;
    private WindowManager.LayoutParams lp;

    ProgressWidget(AccessibilityService s) {
        svc = s;
        wm = (WindowManager) s.getSystemService(Context.WINDOW_SERVICE);
    }

    boolean isShowing() { return pill != null; }

    /** A negative fraction hides that bar (criterion not counted by the goal mode). */
    void showDaily(float fCards, float fMinutes, String center) {
        if (pill == null && !attach()) return;
        if (fCards < 0f) pill.set(fMinutes, C_MINUTES, -1f, 0, center, null);
        else pill.set(fCards, C_CARDS, fMinutes, C_MINUTES, center, null);
    }

    void showSession(float f, int remaining, boolean minutes) {
        if (pill == null && !attach()) return;
        pill.set(f, C_SESSION, -1f, 0, String.valueOf(remaining), minutes ? "min" : L.t("cartes", "cards"));
    }

    /** Emergency running: red bar emptying + time left (reminder to get back to reviewing). */
    void showEmergency(float fractionLeft, String left) {
        if (pill == null && !attach()) return;
        pill.set(fractionLeft, C_EMERGENCY, -1f, 0, left, null);
    }

    void showDone() {
        if (pill == null && !attach()) return;
        pill.set(1f, C_DONE, -1f, 0, "✓", null);
    }

    void hide() {
        if (pill != null) {
            try { wm.removeView(pill); } catch (Exception ignored) {}
            pill = null;
        }
    }

    private boolean attach() {
        final Context c = svc;
        pill = new Pill(c);
        lp = new WindowManager.LayoutParams(Ui.dp(c, 176), Ui.dp(c, 32),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.x = Prefs.widgetX(c);
        int y = Prefs.widgetY(c);
        lp.y = y >= 0 ? y : statusBarHeight(c) + Ui.dp(c, 6);

        pill.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy;
            int ox, oy;
            boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = e.getRawX(); sy = e.getRawY(); ox = lp.x; oy = lp.y; moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - sx, dy = e.getRawY() - sy;
                        if (!moved && Math.abs(dx) + Math.abs(dy) > Ui.dp(c, 8)) moved = true;
                        if (moved) {
                            lp.x = ox + Math.round(dx);
                            lp.y = Math.max(0, oy + Math.round(dy));
                            try { wm.updateViewLayout(pill, lp); } catch (Exception ignored) {}
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moved) Prefs.setWidgetPos(c, lp.x, lp.y);
                        else openAnkineer();
                        return true;
                    default:
                        return false;
                }
            }
        });

        try {
            wm.addView(pill, lp);
            return true;
        } catch (Exception e) {
            pill = null;
            return false;
        }
    }

    private void openAnkineer() {
        Intent i = new Intent(svc, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try { svc.startActivity(i); } catch (Exception ignored) {}
    }

    private static int statusBarHeight(Context c) {
        int id = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? c.getResources().getDimensionPixelSize(id) : Ui.dp(c, 24);
    }

    /** Hand-drawn pill with one or two horizontal progress bars: no animation, redrawn only when something changes. */
    static final class Pill extends View {
        private final Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG), edge = new Paint(Paint.ANTI_ALIAS_FLAG),
                track = new Paint(Paint.ANTI_ALIAS_FLAG), bar = new Paint(Paint.ANTI_ALIAS_FLAG),
                txt = new Paint(Paint.ANTI_ALIAS_FLAG), sub = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private float f1, f2 = -1f;
        private int c1, c2;
        private String center = "", unit;
        private final float d;

        Pill(Context c) {
            super(c);
            d = c.getResources().getDisplayMetrics().density;
            disc.setColor(0xF011151C);
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(d);
            edge.setColor(0x40FFFFFF);
            track.setColor(0x2EFFFFFF);
            txt.setColor(0xFFFFFFFF);
            txt.setTextAlign(Paint.Align.RIGHT);
            txt.setTypeface(Typeface.DEFAULT_BOLD);
            txt.setTextSize(12.5f * d);
            sub.setColor(0xB3FFFFFF);
            sub.setTextAlign(Paint.Align.RIGHT);
            sub.setTextSize(9.5f * d);
        }

        void set(float a, int ca, float b, int cb, String centerText, String unitText) {
            if (a == f1 && b == f2 && ca == c1 && cb == c2 && centerText.equals(center)
                    && (unitText == null ? unit == null : unitText.equals(unit))) return;
            f1 = a; c1 = ca; f2 = b; c2 = cb; center = centerText; unit = unitText;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight(), half = h / 2f;
            r.set(d / 2f, d / 2f, w - d / 2f, h - d / 2f);
            cv.drawRoundRect(r, half, half, disc);
            cv.drawRoundRect(r, half, half, edge);

            // label on the right: value (+ unit)
            float pad = 12f * d, right = w - pad, base = half - (txt.descent() + txt.ascent()) / 2f;
            float unitW = 0f;
            if (unit != null) {
                cv.drawText(unit, right, base, sub);
                unitW = sub.measureText(unit) + 3f * d;
            }
            cv.drawText(center, right - unitW, base, txt);
            float labelW = Math.max(txt.measureText("100%"), txt.measureText(center) + unitW);

            // bars on the left
            float x0 = pad, x1 = w - pad - labelW - 10f * d;
            if (x1 <= x0) return;
            if (f2 >= 0f) {
                float th = 5f * d, gap = 4f * d, top = half - th - gap / 2f;
                drawBar(cv, x0, x1, top, th, f1, c1);
                drawBar(cv, x0, x1, top + th + gap, th, f2, c2);
            } else {
                float th = 8f * d;
                drawBar(cv, x0, x1, half - th / 2f, th, f1, c1);
            }
        }

        private void drawBar(Canvas cv, float x0, float x1, float top, float th, float f, int color) {
            float rad = th / 2f;
            r.set(x0, top, x1, top + th);
            cv.drawRoundRect(r, rad, rad, track);
            if (f <= 0f) return;
            bar.setColor(color);
            r.set(x0, top, x0 + Math.max(th, (x1 - x0) * Math.min(1f, f)), top + th);
            cv.drawRoundRect(r, rad, rad, bar);
        }
    }
}
