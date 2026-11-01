package com.ankineer.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Calendar;

/**
 * Hourly reminders (two separate notifications):
 *  1. "You still have N due cards" (if any are left);
 *  2. "Minimum goal not reached yet" (cards / minutes);
 * plus the Sunday weekly summary (this week vs the previous one) and the "joker earned" notification.
 * Adjustable quiet hours (no reminder at night). Scheduled with AlarmManager, rescheduled after a reboot.
 */
final class Reminders {
    static final String CHANNEL = "ankineer_reminders";
    static final int ID_DUE = 41, ID_GOAL = 42, ID_SERVICE = 43, ID_WEEK = 44, ID_JOKER = 45, ID_TUTO = 46;
    /** Weekly summary: Sunday, from this hour on (first hourly run outside quiet hours). */
    static final int WEEKLY_HOUR = 18;
    static final String PERM = "android.permission.POST_NOTIFICATIONS";
    static final String ACTION = "com.ankineer.app.REMINDER";
    /** "Service off" alert: at every hourly reminder (margin for alarm drift), instead of every 3 h. */
    static final long SERVICE_ALERT_MS = 50 * 60000L;

    private Reminders() {}

    private static SharedPreferences sp(Context c) { return Prefs.sp(c); }

    // ------------------------------------------------------------------ settings
    static boolean enabled(Context c) { return sp(c).getBoolean("remOn", true); }
    static void setEnabled(Context c, boolean on) { sp(c).edit().putBoolean("remOn", on).apply(); schedule(c); }
    static boolean dueOn(Context c) { return sp(c).getBoolean("remDue", true); }
    static void setDueOn(Context c, boolean on) { sp(c).edit().putBoolean("remDue", on).apply(); }
    static boolean goalOn(Context c) { return sp(c).getBoolean("remGoal", true); }
    static boolean quoteOn(Context c) { return sp(c).getBoolean("remQuote", true); }
    static boolean weeklyOn(Context c) { return sp(c).getBoolean("remWeek", true); }
    static void setWeeklyOn(Context c, boolean on) { sp(c).edit().putBoolean("remWeek", on).apply(); }
    static void setQuoteOn(Context c, boolean on) { sp(c).edit().putBoolean("remQuote", on).apply(); }

    /** Adds the motivational quote of the hour (if the option is on). */
    private static String withQuote(Context c, String text, int offset) {
        return quoteOn(c) ? text + "\n\n💬 " + Quotes.ofTheHour(offset) : text;
    }
    static void setGoalOn(Context c, boolean on) { sp(c).edit().putBoolean("remGoal", on).apply(); }
    /** Quiet hours: from quietFrom (included) to quietTo (excluded), e.g. 23 → 8. */
    static int quietFrom(Context c) { return sp(c).getInt("remQf", 23); }
    static int quietTo(Context c) { return sp(c).getInt("remQt", 8); }
    static void setQuiet(Context c, int from, int to) {
        sp(c).edit().putInt("remQf", Math.max(0, Math.min(23, from))).putInt("remQt", Math.max(0, Math.min(23, to))).apply();
    }

    static boolean isQuiet(Context c, int hour) {
        int f = quietFrom(c), t = quietTo(c);
        if (f == t) return false;
        return f < t ? (hour >= f && hour < t) : (hour >= f || hour < t);
    }

    static boolean permitted(Context c) {
        return Build.VERSION.SDK_INT < 33 || c.checkSelfPermission(PERM) == PackageManager.PERMISSION_GRANTED;
    }

    // ------------------------------------------------------------------ programmation

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, ReminderAlarmReceiver.class).setAction(ACTION);
        return PendingIntent.getBroadcast(c, 7, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Schedules (or cancels) the next reminder, at the next full hour (+5 s). Single alarm, rescheduled at every
     * run: unlike a "repeating" alarm, Android doesn't batch it with others (no 1 h 30 drift).
     */
    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = alarmIntent(c);
        am.cancel(pi);
        if (!enabled(c)) return;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 5);
        cal.set(Calendar.MILLISECOND, 0);
        long at = cal.getTimeInMillis();
        try {
            if (exactAllowed(am)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    /** Android 12+: exact alarms only if allowed (USE_EXACT_ALARM grants it at install on 13+). */
    private static boolean exactAllowed(AlarmManager am) {
        if (Build.VERSION.SDK_INT < 31) return true;
        try {
            return (Boolean) am.getClass().getMethod("canScheduleExactAlarms").invoke(am);
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ envoi

    /** Called every hour (off the main thread). {@code force} = "test" button (ignores quiet hours). */
    static void run(Context c, boolean force) {
        Clock.init(c);
        L.load(c);
        if (!force && !enabled(c)) return;
        if (!force && isQuiet(c, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))) return;
        if (!permitted(c)) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        // lock service stopped (crash) or switched off by Android while the lock is started:
        // warn at every hourly run (at most once an hour)
        if (Apps.isServiceEnabled(c) && !LockService.running) {
            // process just restarted by the alarm: Android reconnects the service within a few seconds
            for (int i = 0; i < 10 && !LockService.running; i++) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        boolean enabledSvc = Apps.isServiceEnabled(c);
        boolean stopped = enabledSvc && !LockService.running;
        boolean switchedOff = !enabledSvc && sp(c).getBoolean("svcSeen", false) && Prefs.armed(c);
        if (stopped || switchedOff) {
            long last = sp(c).getLong("remSvcAt", 0), t = System.currentTimeMillis();
            if (force || t - last > SERVICE_ALERT_MS || t < last) {
                sp(c).edit().putLong("remSvcAt", t).apply();
                notify(c, nm, ID_SERVICE, "⚠ " + (switchedOff
                                ? L.t("Le service d'accessibilité est coupé", "The accessibility service is off")
                                : L.t("Le verrou Ankineer est arrêté", "The Ankineer lock is stopped")),
                        switchedOff
                                ? L.t("Android a désactivé « Ankineer – Verrou Anki » : le verrou ne bloque plus rien. Ouvre Ankineer Lock → Installation, étape 3, et réactive-le.",
                                "Android turned “Ankineer – Anki Lock” off: the lock no longer blocks anything. Open Ankineer Lock → Setup, step 3, and turn it back on.")
                                : L.t("Android a arrêté le service. Ouvre Ankineer Lock → Installation, étape 3 : désactive puis réactive « Ankineer – Verrou Anki ».",
                                "Android stopped the service. Open Ankineer Lock → Setup, step 3: turn “Ankineer – Anki Lock” off and on again."), false, true);
            }
        } else {
            nm.cancel(ID_SERVICE);
        }
        GoalChecker.Result r = GoalChecker.check(c);
        if (!r.apiOk) {
            // AnkiDroid unreadable: warn (at most every 3 h) instead of staying silent
            long now = System.currentTimeMillis(), last = sp(c).getLong("remErrAt", 0);
            if (force || now - last > 3 * 3600000L || now < last) {
                sp(c).edit().putLong("remErrAt", now).apply();
                nm.cancel(ID_DUE);
                notify(c, nm, ID_GOAL, "⚠ " + L.t("Ankineer Lock ne lit plus AnkiDroid", "Ankineer Lock can't read AnkiDroid"),
                        (r.error != null ? r.error + "\n" : "") + L.t("Ouvre Ankineer Lock → Installation, étape 1, pour réparer.",
                                "Open Ankineer Lock → Setup, step 1, to fix it."), false, true);
            }
            return;
        }
        sp(c).edit().remove("remErrAt").apply();
        int due = -1;
        if (dueOn(c) || force) {
            try { due = GoalChecker.totalDue(c); } catch (Exception ignored) {}
        }
        // 1) due cards
        if ((dueOn(c) || force) && due > 0) {
            notify(c, nm, ID_DUE, "📚 " + L.t("Encore ", "Still ") + due + (due > 1 ? L.t(" cartes dues", " due cards") : L.t(" carte due", " due card")),
                    withQuote(c, L.t("Ouvre AnkiDroid pour les terminer.", "Open AnkiDroid to finish them."), 0), true);
        } else {
            nm.cancel(ID_DUE);
        }
        // 2) objectif minimum
        if ((goalOn(c) || force) && !r.dailyMet && !Prefs.goalDoneToday(c)) {
            int lc = r.needCards ? Math.max(0, r.goalCards - r.cards) : 0, lm = r.needMinutes ? Math.max(0, r.goalMinutes - r.minutes) : 0;
            String left = (lc > 0 ? lc + L.t(" cartes", " cards") : "") + (lc > 0 && lm > 0 ? L.t(" et ", " and ") : "") + (lm > 0 ? lm + " min" : "");
            notify(c, nm, ID_GOAL, "🎯 " + L.t("Objectif minimum pas encore atteint", "Minimum goal not reached yet"),
                    withQuote(c, (r.needCards ? L.t("Cartes ", "Cards ") + r.cards + "/" + r.goalCards : "") + (r.needCards && r.needMinutes ? "  ·  " : "")
                            + (r.needMinutes ? "Minutes " + r.minutes + "/" + r.goalMinutes : "")
                            + (left.length() > 0 ? L.t("  —  encore ", "  —  ") + left + L.t("", " to go") : ""), 1), true);
        } else {
            nm.cancel(ID_GOAL);
        }
        if (force && due <= 0 && (r.dailyMet || Prefs.goalDoneToday(c))) {
            notify(c, nm, ID_GOAL, "✓ " + L.t("Tout est fait pour aujourd'hui", "All done for today"),
                    L.t("Aucun rappel nécessaire : bravo !", "No reminder needed: well done!"), false);
        }
        weekly(c, nm, force);
        WidgetUpdater.updateAll(c, true);
    }

    /** Sunday evening: this week (Monday → today) compared with the previous one. Once a week; {@code force} = test. */
    private static void weekly(Context c, NotificationManager nm, boolean force) {
        Calendar cal = Calendar.getInstance();
        boolean due = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && cal.get(Calendar.HOUR_OF_DAY) >= WEEKLY_HOUR;
        String today = Prefs.today();
        if (!force && (!weeklyOn(c) || !due || today.equals(sp(c).getString("remWeekDay", "")))) return;
        if (!force) sp(c).edit().putString("remWeekDay", today).apply();
        int[] cur = week(c, today), prev = week(c, Stats.addDays(today, -7));
        String body = L.t("Cartes ", "Cards ") + cur[0] + trend(cur[0], prev[0])
                + "\nMinutes " + cur[1] + trend(cur[1], prev[1])
                + "\n" + L.t("Objectif atteint ", "Goal reached ") + cur[2] + "/7" + L.t(" jours (semaine passée ", " days (last week ") + prev[2] + "/7)";
        int streak = Stats.streak(c);
        if (streak > 1) body += "\n🔥 " + L.t("Série : ", "Streak: ") + streak + L.t(" jours", " days");
        body += "\n🃏 " + L.t("Jokers : ", "Jokers: ") + Jokers.count(c) + L.t(" · prochain dans ", " · next in ") + Jokers.daysToNext(c) + L.t(" j", " d");
        notify(c, nm, ID_WEEK, "📊 " + L.t("Ta semaine : ", "Your week: ") + cur[0] + L.t(" cartes · ", " cards · ") + cur[1] + " min",
                withQuote(c, body, 2), false);
    }

    /** {cards, minutes, days with the goal reached} over the 7 days ending on {@code lastDay}. */
    private static int[] week(Context c, String lastDay) {
        int[] t = new int[3];
        for (int i = 0; i < 7; i++) {
            Stats.Day d = Stats.get(c, Stats.addDays(lastDay, -i));
            t[0] += d.cards;
            t[1] += d.minutes;
            if (d.met) t[2]++;
        }
        return t;
    }

    /** " (▲ +12 % vs 375)" */
    private static String trend(int cur, int prev) {
        if (prev <= 0) return cur > 0 ? L.t("  (▲ nouvelle semaine)", "  (▲ new week)") : "";
        int pct = Math.round(100f * (cur - prev) / prev);
        return "  (" + (pct >= 0 ? "▲ +" : "▼ ") + pct + " %" + L.t(" vs ", " vs ") + prev + ")";
    }

    /** New tutorial videos built into this update. */
    static void tutorialsAdded(Context c, int n, String newestTitle) {
        if (!permitted(c)) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        L.load(c);
        notify(c, nm, ID_TUTO, "🎬 " + (n > 1 ? n + L.t(" nouveaux tutoriels Ankineer", " new Ankineer tutorials") : L.t("Nouveau tutoriel Ankineer", "New Ankineer tutorial")),
                "▶ " + newestTitle + "\n" + L.t("Retrouve-les dans Ankineer Lock → ☰ → Tutoriels.", "Find them in Ankineer Lock → ☰ → Tutorials."),
                false, MainActivity.TUTORIALS);
    }

    /** Streak reward earned (one joker per full week of streak). */
    static void jokerEarned(Context c, int n) {
        if (!permitted(c)) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        L.load(c);
        notify(c, nm, ID_JOKER, "🃏 " + (n > 1 ? n + L.t(" nouveaux jokers !", " new jokers!") : L.t("Nouveau joker gagné !", "New joker earned!")),
                L.t("7 jours d'affilée avec ton objectif : bravo ! Un joker = ", "7 days in a row with your goal: well done! One joker = ")
                        + Jokers.MINUTES + L.t(" min de liberté, même pendant le verrou. Tu en as ", " free minutes, even while locked. You have ")
                        + Jokers.count(c) + ".", false);
    }

    private static void notify(Context c, NotificationManager nm, int id, String title, String text, boolean openAnki) {
        notify(c, nm, id, title, text, openAnki, false);
    }

    private static void notify(Context c, NotificationManager nm, int id, String title, String text, boolean openAnki, boolean openSetup) {
        notify(c, nm, id, title, text, openAnki, openSetup ? MainActivity.INSTALL : -1);
    }

    private static void notify(Context c, NotificationManager nm, int id, String title, String text, boolean openAnki, int page) {
        Notification.Builder b = builder(c, nm);
        Intent open = openAnki ? c.getPackageManager().getLaunchIntentForPackage(GoalChecker.ANKI_PKG) : null;
        if (open == null) open = new Intent(c, MainActivity.class);
        if (page >= 0) open.putExtra("page", page);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent app = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pa = PendingIntent.getActivity(c, 100 + id, app, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        b.setSmallIcon(R.drawable.ic_stat_lock)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setColor(Ui.ACCENT)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setShowWhen(true)
                .addAction(0, "Ankineer Lock", pa);
        if (openAnki) b.addAction(0, L.t("Ouvrir AnkiDroid", "Open AnkiDroid"), pi);
        try { nm.notify(id, b.build()); } catch (Exception ignored) {}
    }

    /** Builder compatible with Android 6 → 15 (notification channel created by reflection on Android 8+). */
    @SuppressWarnings("deprecation")
    private static Notification.Builder builder(Context c, NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                Class<?> ch = Class.forName("android.app.NotificationChannel");
                Object channel = ch.getConstructor(String.class, CharSequence.class, int.class)
                        .newInstance(CHANNEL, L.t("Rappels de révision", "Review reminders"), 3 /* IMPORTANCE_DEFAULT */);
                nm.getClass().getMethod("createNotificationChannel", ch).invoke(nm, channel);
                return Notification.Builder.class.getConstructor(Context.class, String.class).newInstance(c, CHANNEL);
            } catch (Exception ignored) {}
        }
        return new Notification.Builder(c).setPriority(Notification.PRIORITY_DEFAULT);
    }
}
