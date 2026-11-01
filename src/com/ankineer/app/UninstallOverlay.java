package com.ankineer.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Big "think before uninstalling" window, shown by the service when the user tries to uninstall Ankineer Lock.
 * A 3-minute countdown with a motivational message; only then can the uninstall go on (for {@link #PASS_MS}).
 * "Keep Ankineer Lock" closes it at any time. Nothing else is blocked (Settings, Play Store…).
 */
final class UninstallOverlay {
    static final long WAIT_MS = 3 * 60000L;
    /** After the 3 minutes: Android's uninstall window is let through for this long. */
    static final long PASS_MS = 2 * 60000L;

    private final AccessibilityService svc;
    private final WindowManager wm;
    private final Handler main = new Handler(Looper.getMainLooper());
    private View root;
    private TextView timerTv;
    private Ui.Bar bar;
    private Button go;
    private long shownAt;

    UninstallOverlay(AccessibilityService s) {
        svc = s;
        wm = (WindowManager) s.getSystemService(Context.WINDOW_SERVICE);
    }

    boolean isShowing() { return root != null; }

    /** The 3 minutes are over and the user confirmed: Android's uninstall window is allowed right now. */
    static boolean passActive(Context c) {
        long until = Prefs.sp(c).getLong("unPassUntil", 0), now = System.currentTimeMillis();
        return now < until && until - now <= PASS_MS;
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (root == null) return;
            render();
            main.postDelayed(this, 1000);
        }
    };

    void show() {
        if (root != null) return;
        try {
            L.load(svc);
            Ui.applyTheme(svc);
            shownAt = System.currentTimeMillis();
            root = build();
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.OPAQUE);
            if (wm != null) wm.addView(root, lp);
            render();
            main.removeCallbacks(tick);
            main.postDelayed(tick, 1000);
        } catch (Throwable t) {
            root = null;
            CrashLog.save(svc, t);
        }
    }

    void hide() {
        main.removeCallbacks(tick);
        if (root != null && wm != null) {
            try { wm.removeView(root); } catch (Exception ignored) {}
        }
        root = null;
    }

    private View build() {
        Context c = svc;
        ScrollView sv = new ScrollView(c);
        sv.setBackgroundColor(Ui.BG);
        sv.setFillViewport(true);
        LinearLayout col = Ui.column(c);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = Ui.dp(c, 24);
        col.setPadding(p, Ui.dp(c, 44), p, p);
        sv.addView(col, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.MATCH_PARENT));

        TextView icon = Ui.text(c, "⚠️", 54, Ui.TEXT, false);
        icon.setGravity(Gravity.CENTER);
        col.addView(icon);
        TextView title = Ui.text(c, L.t("ATTENTION", "WARNING"), 34, Ui.AMBER, true);
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(0.06f);
        col.addView(title, Ui.topMargin(c, 4));
        TextView t1 = Ui.text(c, L.t("Réfléchis avant de désinstaller Ankineer Lock.", "Think before uninstalling Ankineer Lock."), 22, Ui.TEXT, true);
        t1.setGravity(Gravity.CENTER);
        col.addView(t1, Ui.topMargin(c, 10));

        LinearLayout card = Ui.card(c);
        card.setBackground(Ui.round(c, Ui.CARD, 18, Ui.AMBER));
        card.setLayoutParams(Ui.topMargin(c, 18));
        int streak = Stats.streak(c);
        String why = L.t("Tu as installé Ankineer Lock pour une raison : réviser chaque jour, sans te laisser distraire. "
                        + "Une envie de désinstaller arrive souvent au moment où c'est difficile — c'est justement là que l'habitude se construit.",
                "You installed Ankineer Lock for a reason: to review every day without getting distracted. "
                        + "The urge to uninstall often comes right when it's hard — that's exactly when the habit is built.");
        if (streak > 1) why += "\n\n🔥 " + L.t("Ta série actuelle : ", "Your current streak: ") + streak + L.t(" jours. Ne la casse pas maintenant !", " days. Don't break it now!");
        if (Jokers.count(c) > 0) why += "\n🃏 " + L.t("Tu as ", "You have ") + Jokers.count(c) + L.t(" joker(s) : tu peux souffler 30 min sans tout arrêter.", " joker(s): you can take a 30-min break without quitting.");
        TextView body = Ui.text(c, why, 15, Ui.TEXT, false);
        body.setLineSpacing(Ui.dp(c, 3), 1f);
        card.addView(body);
        TextView quote = Ui.text(c, "💬 " + Quotes.ofTheHour((int) (System.currentTimeMillis() / 60000L % Quotes.count())), 14, Ui.MUTED, false);
        card.addView(quote, Ui.topMargin(c, 12));
        col.addView(card);

        timerTv = Ui.text(c, "", 40, Ui.TEXT, true);
        timerTv.setGravity(Gravity.CENTER);
        col.addView(timerTv, Ui.topMargin(c, 18));
        TextView tl = Ui.text(c, L.t("Prends ces 3 minutes pour y réfléchir.", "Take these 3 minutes to think it over."), 13, Ui.MUTED, false);
        tl.setGravity(Gravity.CENTER);
        col.addView(tl);
        bar = new Ui.Bar(c);
        bar.setTrack(Ui.STROKE);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(c, 6));
        blp.topMargin = Ui.dp(c, 12);
        col.addView(bar, blp);

        Button keep = Ui.button(c, L.t("💪  Je garde Ankineer Lock", "💪  Keep Ankineer Lock"), true);
        keep.setBackground(Ui.round(c, Ui.GREEN, 12, 0));
        keep.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                hide();
                svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
            }
        });
        col.addView(keep, Ui.topMargin(c, 18));
        go = Ui.button(c, "", false);
        go.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { uninstall(); }
        });
        col.addView(go);

        TextView brand = Ui.text(c, "Developed by Ankineer", 11, Ui.MUTED, false);
        brand.setGravity(Gravity.CENTER);
        col.addView(brand, Ui.topMargin(c, 18));
        return sv;
    }

    private long leftMs() { return Math.max(0, shownAt + WAIT_MS - System.currentTimeMillis()); }

    private void render() {
        long left = leftMs(), s = (left + 999) / 1000;
        boolean ready = left <= 0;
        timerTv.setText(ready ? "✓" : String.format(java.util.Locale.US, "%d:%02d", s / 60, s % 60));
        timerTv.setTextColor(ready ? Ui.MUTED : Ui.AMBER);
        bar.set(1f - left / (float) WAIT_MS, ready ? Ui.MUTED : Ui.AMBER);
        go.setText(ready ? L.t("Désinstaller quand même", "Uninstall anyway") : L.t("Désinstaller (attends la fin du minuteur)", "Uninstall (wait for the timer)"));
        Ui.enable(go, ready);
    }

    /** After the 3 minutes: opens Android's own uninstall window, let through for a short while. */
    private void uninstall() {
        if (leftMs() > 0) return;
        Prefs.sp(svc).edit().putLong("unPassUntil", System.currentTimeMillis() + PASS_MS).apply();
        hide();
        try {
            svc.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + svc.getPackageName()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {}
    }
}
