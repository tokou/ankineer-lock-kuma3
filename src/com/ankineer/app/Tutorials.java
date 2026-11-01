package com.ankineer.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Videos of the Ankineer YouTube channel, built into the app (no Internet access: the list grows with each update).
 * After an update that adds videos: they are marked NEW and a notification announces them (once).
 */
final class Tutorials {
    static final String CHANNEL_URL = "https://www.youtube.com/@TheAnkineer";
    static final String LEARN = "learn", ADDONS = "addons", APP = "app", CHANNEL = "channel";
    static final String[] CATEGORIES = {LEARN, ADDONS, APP, CHANNEL};

    /** {video id, title, duration, category}, newest first. */
    static final String[][] VIDEOS = {
            {"q9RroIzoDI4", "You're Pressing the Wrong Button in Anki (Again vs Good) - Anki Mastery #4", "8:24", LEARN},
            {"IxMFWCbNbHo", "7 Rules for Cards That Stick - Anki Mastery #4", "10:39", LEARN},
            {"jlygB5JQHF4", "Meet the Anki Note Types: Cloze, Type-in & Image Occlusion | Anki Mastery #03", "11:27", LEARN},
            {"cuXYVAJLYM4", "Style Any Anki Flashcard Without CSS | Ankineer CSS Studio Walkthrough", "9:38", ADDONS},
            {"iQH_uRslWIM", "What Anki Doesn’t Tell You Before You Press “Study”", "7:35", LEARN},
            {"tlLsAzFRewg", "This Anki Add-on X-Rays Every Card You Study (Card Pulse )", "7:10", ADDONS},
            {"U3qvnCI9-k8", "Tomatune: Focus Music + Pomodoro Timer INSIDE Anki (Free Add-on)", "8:05", ADDONS},
            {"QL8mQYMFlqE", "Turn Anki Into a Game: HabitHero V2 (Free Add-on, 20 Heroes, XP & Streaks)", "7:19", ADDONS},
            {"Gu4YQbMimAM", "My Phone Stays Locked Until My Anki Reviews Are Done (Free App)", "5:04", APP},
            {"jIV3V1BUm4E", "FINALLY Here! Download & Launch Ankineer AI – Free AI Add-on for Anki", "4:28", ADDONS},
            {"icgGOmZm7ak", "The Anki Course I Wish I Had - Anki Mastery #0", "3:40", LEARN},
            {"ifCrhGugJzQ", "Ankineer Channel Intro: What This Channel Is About ?", "4:01", CHANNEL},
            {"Kd-NGVGfhS8", "This Anki Add-on Locks You In Until Your Cards Are Done (FocusForce V2)", "7:49", ADDONS},
            {"fATrUeXQbwg", "Your Anki Cards Deserve Better Formatting (1-Click Add-on)", "5:02", ADDONS},
            {"czgoUDjp2Ec", "From Resits to Residency: How Anki Changed the Way I Study", "4:07", CHANNEL},
            {"ELSonoU8Q8k", "Stop Writing Anki Cards by Hand — This Free AI Add-on Does It", "9:56", ADDONS},
            {"C-wixdbatQM", "Anki, But Smarter! 🔥 Organize Notes & Connect Knowledge with ANKINEER NEXUS", "12:19", ADDONS},
            {"2dgaEsiSvs8", "PopEdit for Anki: Format Cards While Reviewing | Original Version", "2:39", ADDONS},
            {"hKcmfLYwnKc", "Stop Making Basic Cards - Anki Mastery #2", "5:20", LEARN},
            {"SYZV0uMfrdM", "Beat the Forgetting Curve - Anki Mastery #1", "3:58", LEARN},
            {"hxQQQ8mqti8", "Card Auditor for Anki: Instant Card Stats & Verdicts | Original Version", "2:09", ADDONS},
            {"4iwVfChvjdY", "I Turned Anki Into a Game 🎮 | HabitHero (Original Version)", "1:59", ADDONS},
            {"VbT7athYYsw", "Anki HUD: Track Your Deck Progress & Reviews | Original Version", "2:18", ADDONS},
            {"2X10wYn5ygE", "FocusForce for Anki: Lock In & Finish Your Reviews (Original Version)", "2:15", ADDONS},
            {"sb67xTst3MU", "Don't Just Study, Know You're Learning: The FLUX Dashboard for Anki", "1:34", ADDONS},
            {"p0yJQGXOVOk", "Turn Anki into the Ultimate Productivity Machine 🚀 (Music + Pomodoro)", "1:58", ADDONS},
    };

    private Tutorials() {}

    static String categoryName(String cat) {
        if (LEARN.equals(cat)) return L.t("🎓 Apprendre Anki", "🎓 Learn Anki");
        if (ADDONS.equals(cat)) return L.t("🧩 Add-ons", "🧩 Add-ons");
        if (APP.equals(cat)) return "🔒 Ankineer Lock";
        if (CHANNEL.equals(cat)) return L.t("💬 La chaîne", "💬 The channel");
        return L.t("Tout", "All");
    }

    static int categoryColor(String cat) {
        if (LEARN.equals(cat)) return AboutPage.GREEN;
        if (ADDONS.equals(cat)) return AboutPage.PURPLE;
        if (APP.equals(cat)) return AboutPage.BLUE;
        return AboutPage.AMBER;
    }

    private static SharedPreferences sp(Context c) { return Prefs.sp(c); }

    /** Videos marked NEW (added by the last updates, not opened yet). */
    static Set<String> fresh(Context c) { return new HashSet<String>(sp(c).getStringSet("tutNew", new HashSet<String>())); }

    static void opened(Context c, String id) {
        Set<String> s = fresh(c);
        if (s.remove(id)) sp(c).edit().putStringSet("tutNew", s).apply();
    }

    /**
     * Compares the built-in list with the one already known (after an update). First run: everything known, nothing NEW.
     * Returns the newest video just added, or null.
     */
    static synchronized String[] checkNew(Context c) {
        SharedPreferences p = sp(c);
        Set<String> all = new HashSet<String>();
        for (String[] v : VIDEOS) all.add(v[0]);
        if (!p.contains("tutKnown")) {
            p.edit().putStringSet("tutKnown", all).apply();
            return null;
        }
        Set<String> known = new HashSet<String>(p.getStringSet("tutKnown", new HashSet<String>()));
        Set<String> fresh = fresh(c);
        String[] newest = null;
        int added = 0;
        for (String[] v : VIDEOS) {
            if (known.contains(v[0])) continue;
            fresh.add(v[0]);
            added++;
            if (newest == null) newest = v;
        }
        if (added == 0) return null;
        p.edit().putStringSet("tutKnown", all).putStringSet("tutNew", fresh).putInt("tutAdded", added).apply();
        return newest;
    }

    /** After an update: notification for the videos just added. */
    static void notifyNew(Context c) {
        String[] v = checkNew(c);
        if (v != null) Reminders.tutorialsAdded(c, sp(c).getInt("tutAdded", 1), v[1]);
    }
}
