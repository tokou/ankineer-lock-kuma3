package com.ankineer.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Streak rewards: every full week of streak (7 days in a row with the goal reached) earns a joker.
 * A joker frees the phone for 30 minutes, even while locked (daily goal or session). At most {@link #MAX} in stock.
 * Weeks already completed before the feature existed don't count: counting starts at the current streak.
 */
final class Jokers {
    static final String TYPE = "jkr";
    static final int MINUTES = 30, STREAK_DAYS = 7, MAX = 10;
    /** Streak re-read at most every 10 min (it walks the history). */
    private static final long CHECK_MS = 10 * 60000L;

    private Jokers() {}

    private static SharedPreferences sp(Context c) { return Prefs.sp(c); }

    static int count(Context c) { return Math.max(0, Math.min(MAX, sp(c).getInt("jkCount", 0))); }

    /** Days of streak still needed for the next joker. */
    static int daysToNext(Context c) {
        int s = sp(c).getInt("jkStreak", 0);
        return STREAK_DAYS - s % STREAK_DAYS;
    }

    /**
     * Re-reads the streak (throttled, and right away when today's goal has just been reached)
     * and awards the jokers earned. Returns the number just earned.
     */
    static synchronized int update(Context c, boolean metToday) {
        SharedPreferences p = sp(c);
        long now = System.currentTimeMillis(), at = p.getLong("jkAt", 0);
        String day = Prefs.today();
        boolean justMet = metToday && !day.equals(p.getString("jkMetDay", ""));
        if (!justMet && now - at < CHECK_MS && now >= at && day.equals(p.getString("jkDay", ""))) return 0;
        int streak = Stats.streak(c), weeks = streak / STREAK_DAYS;
        SharedPreferences.Editor e = p.edit().putLong("jkAt", now).putString("jkDay", day).putInt("jkStreak", streak);
        if (metToday) e.putString("jkMetDay", day);
        int gained = 0;
        if (!p.contains("jkWeeks") || weeks < p.getInt("jkWeeks", 0)) {
            e.putInt("jkWeeks", weeks);                        // first run, or streak broken: count from here
        } else if (weeks > p.getInt("jkWeeks", 0)) {
            gained = Math.min(MAX - count(c), weeks - p.getInt("jkWeeks", 0));
            e.putInt("jkWeeks", weeks).putInt("jkCount", count(c) + Math.max(0, gained));
        }
        e.apply();
        return Math.max(0, gained);
    }

    /** Worth using: the phone is locked right now (no emergency running). */
    static boolean usable(Context c) { return count(c) > 0 && Prefs.lockActive(c) && !Prefs.testActive(c); }

    /** Uses a joker: 30 free minutes right away. */
    static synchronized boolean use(Context c) {
        if (!usable(c)) return false;
        sp(c).edit().putInt("jkCount", count(c) - 1).apply();
        Emergency.grant(c, TYPE, MINUTES);
        return true;
    }
}
