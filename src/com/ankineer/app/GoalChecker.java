package com.ankineer.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

import org.json.JSONArray;

/**
 * Progress read from AnkiDroid (official API):
 *  - daily goal: cards and/or minutes (goal mode, thresholds ≥ 25 / 10), + "0 due cards" if enabled;
 *  - session: one-off goal in cards, minutes or "finish the deck", on all decks or one deck, counted from its start
 *    (a deck session also ends as soon as that deck has no due card left).
 * check() reads AnkiDroid (call it off the main thread); snapshot() only re-reads memory.
 */
final class GoalChecker {
    static final String ANKI_PKG = "io.github.pedrubik2000.kuma3";
    static final String AUTHORITY = "io.github.pedrubik2000.kuma3.flashcards";
    static final String PERMISSION = "io.github.pedrubik2000.kuma3.permission.READ_WRITE_DATABASE";

    static final class Result {
        boolean apiOk;
        String error;
        int cards, minutes, due = -1;
        int goalCards, goalMinutes;
        /** Which thresholds count (goal mode). */
        boolean needCards = true, needMinutes = true;
        boolean dueRequired, dueSufficient;
        /** Unlocked because all due cards are done (option). */
        boolean byDue;
        float fCards, fMinutes;
        boolean dailyMet;

        boolean session;
        String sessionType;
        int sessionDone, sessionTarget;
        float fSession;
        boolean sessionMet;
        /** Session deck name ("" = all decks) and due cards left in it (-1 = unknown). */
        String sessionDeck = "";
        int sessionDue = -1;
        /** The session ended because its deck has no due card left. */
        boolean sessionDeckDone;

        /** Least advanced of the criteria that count today (goal mode). */
        float dailyFraction() {
            return needCards && needMinutes ? Math.min(fCards, fMinutes) : needCards ? fCards : fMinutes;
        }

        /** Fraction shown (session if running, otherwise the least advanced criterion of the day). */
        float fraction;
        String text = "";
        /** Centre of the widget, e.g. "64%". */
        String shortText = "";
    }

    private GoalChecker() {}

    static boolean isAnkiInstalled(Context c) {
        try {
            return c.getPackageManager().resolveContentProvider(AUTHORITY, 0) != null;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean hasPermission(Context c) {
        return c.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    // ------------------------------------------------------------------ lectures AnkiDroid

    /** An AnkiDroid deck and its due cards today (learning + reviews + new, subdecks included). */
    static final class Deck {
        long id;
        String name;
        int due;
    }

    /** Every deck (subdecks included, "Parent::Child" names), sorted by name. */
    static java.util.List<Deck> decks(Context c) throws Exception {
        Uri uri = Uri.parse("content://" + AUTHORITY + "/decks");
        Cursor cur = c.getContentResolver().query(uri, null, null, null, null);
        if (cur == null) throw new Exception("AnkiDroid n'a pas répondu");
        java.util.List<Deck> out = new java.util.ArrayList<Deck>();
        try {
            int nameIdx = cur.getColumnIndex("deck_name"), idIdx = cur.getColumnIndex("deck_id"), countIdx = cur.getColumnIndex("deck_count");
            if (idIdx < 0 || nameIdx < 0) throw new Exception("liste des decks indisponible");
            while (cur.moveToNext()) {
                Deck d = new Deck();
                d.id = cur.getLong(idIdx);
                d.name = cur.getString(nameIdx);
                if (d.id == 0 || d.name == null || d.name.trim().length() == 0) continue;
                String counts = countIdx >= 0 ? cur.getString(countIdx) : null;
                if (counts != null) {
                    JSONArray arr = new JSONArray(counts);
                    for (int i = 0; i < arr.length(); i++) d.due += arr.optInt(i, 0);
                }
                out.add(d);
            }
        } finally {
            cur.close();
        }
        java.util.Collections.sort(out, new java.util.Comparator<Deck>() {
            @Override public int compare(Deck x, Deck y) { return x.name.compareToIgnoreCase(y.name); }
        });
        return out;
    }

    /** Due cards of a deck (its subdecks included); 0 = all decks. */
    static int deckDue(Context c, long deckId) throws Exception {
        if (deckId == 0) return totalDue(c);
        for (Deck d : decks(c)) if (d.id == deckId) return d.due;
        throw new Exception("deck introuvable");
    }

    /** SQL filter on revlog for a deck and its subdecks (filtered decks included); "" = all decks. */
    private static String deckFilter(Context c, long deckId) throws Exception {
        if (deckId == 0) return "";
        String name = null;
        java.util.List<Deck> all = decks(c);
        for (Deck d : all) if (d.id == deckId) name = d.name;
        if (name == null) throw new Exception("deck introuvable");
        StringBuilder ids = new StringBuilder();
        for (Deck d : all) {
            if (d.name.equals(name) || d.name.startsWith(name + "::")) ids.append(ids.length() > 0 ? "," : "").append(d.id);
        }
        return " AND cid IN (SELECT id FROM cards WHERE did IN (" + ids + ") OR odid IN (" + ids + "))";
    }

    /** Total due cards today (learning + reviews + new) across root decks. */
    static int totalDue(Context c) throws Exception {
        Uri uri = Uri.parse("content://" + AUTHORITY + "/decks");
        Cursor cur = c.getContentResolver().query(uri, null, null, null, null);
        if (cur == null) throw new Exception("AnkiDroid n'a pas répondu");
        try {
            int nameIdx = cur.getColumnIndex("deck_name");
            int idIdx = cur.getColumnIndex("deck_id");
            int countIdx = cur.getColumnIndex("deck_count");
            if (countIdx < 0) throw new Exception("compteurs de decks indisponibles");
            int total = 0;
            while (cur.moveToNext()) {
                String name = nameIdx >= 0 ? cur.getString(nameIdx) : "";
                if (name == null || name.trim().length() == 0) continue;     // root node = total already counted
                if (idIdx >= 0 && cur.getLong(idIdx) == 0) continue;
                if (name.contains("::")) continue;                            // subdeck: counted in the parent
                String counts = cur.getString(countIdx);
                if (counts == null) continue;
                JSONArray arr = new JSONArray(counts);
                for (int i = 0; i < arr.length(); i++) total += arr.optInt(i, 0);
            }
            return total;
        } finally {
            cur.close();
        }
    }

    private static int countSearch(Context c, String path, String search) throws Exception {
        Uri uri = Uri.parse("content://" + AUTHORITY + "/" + path);
        Cursor cur = c.getContentResolver().query(uri, new String[]{"_id"}, search, null, null);
        if (cur == null) return 0;            // AnkiDroid returns null when nothing matches
        try {
            return cur.getCount();
        } finally {
            cur.close();
        }
    }

    /** Start of the Anki day (4 am, like AnkiDroid) in milliseconds. */
    static long dayStartMs() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(Clock.now());
        if (cal.get(java.util.Calendar.HOUR_OF_DAY) < Prefs.DAY_CUTOFF_HOUR) cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, Prefs.DAY_CUTOFF_HOUR);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Value computed from AnkiDroid's review history (revlog) — exactly what
     * AnkiDroid shows ("Studied X cards in Y minutes today"), PC included after sync.
     * The API only reads the notes table: the value comes back as a number of rows.
     */
    private static int revlogValue(Context c, String valueSql) throws Exception {
        Uri uri = Uri.parse("content://" + AUTHORITY + "/notes_v2");
        String sel = "id IN (SELECT id FROM notes ORDER BY id LIMIT (" + valueSql + "))";
        Cursor cur;
        try {
            cur = c.getContentResolver().query(uri, new String[]{"_id"}, sel, null, null);
        } catch (IllegalArgumentException e) {
            cur = c.getContentResolver().query(uri, null, sel, null, null);
        }
        if (cur == null) return 0;
        try {
            return cur.getCount();
        } finally {
            cur.close();
        }
    }

    /** {cards, minutes} studied between two instants (ms), from AnkiDroid's history. */
    static int[] studiedBetween(Context c, long fromMs, long toMs) throws Exception { return studiedBetween(c, fromMs, toMs, 0); }

    /** Same, counting only one deck and its subdecks (0 = all decks). */
    static int[] studiedBetween(Context c, long fromMs, long toMs, long deckId) throws Exception {
        String where = "FROM revlog WHERE id > " + fromMs + " AND id <= " + toMs + " AND ease > 0" + deckFilter(c, deckId);
        return new int[]{revlogValue(c, "SELECT count() " + where),
                revlogValue(c, "SELECT coalesce(sum(time), 0) / 60000 " + where)};
    }

    /**
     * Review retention (cards in review, like Anki's "true retention": any answer other than "Again")
     * between two instants (0 / Long.MAX_VALUE = no limit). Returns {‰ passed (-1 if no review), number of reviews}.
     */
    static int[] retentionBetween(Context c, long fromMs, long toMs) throws Exception {
        String where = "FROM revlog WHERE type = 1 AND ease > 0"
                + (fromMs > 0 ? " AND id > " + fromMs : "") + (toMs < Long.MAX_VALUE / 8 ? " AND id <= " + toMs : "");
        int pm = revlogValue(c, "SELECT coalesce(CAST(round(1000.0 * sum(ease > 1) / count()) AS INTEGER), -1) + 1 " + where) - 1;
        if (pm < 0) return new int[]{-1, 0};
        // the value comes back as a number of rows of the notes table: capped if the collection has few notes
        int pc = revlogValue(c, "SELECT coalesce(CAST(round(100.0 * sum(ease > 1) / count()) AS INTEGER), -1) + 1 " + where) - 1;
        if (pc >= 0 && Math.abs(pm - pc * 10) > 10) pm = pc * 10;
        int n = fromMs > 0 ? revlogValue(c, "SELECT count() " + where) : -1;
        return new int[]{pm, n};
    }

    /** Today's retention (at most one read per minute) and overall retention (at most every 6 h). */
    static void readRetention(Context c, boolean forceGlobal) {
        try {
            long now = System.currentTimeMillis();
            if (now - Prefs.sp(c).getLong("retAt", 0) > 60000L || now < Prefs.sp(c).getLong("retAt", 0)) {
                int[] t = retentionBetween(c, dayStartMs(), Long.MAX_VALUE);
                Prefs.sp(c).edit().putInt("retToday", t[0]).putInt("retTodayN", t[1]).putString("retDay", Prefs.today()).putLong("retAt", now).apply();
            }
            long g = Prefs.sp(c).getLong("retGlobalAt", 0);
            if (forceGlobal || now - g > 6 * 3600000L || now < g) {
                int[] all = retentionBetween(c, 0, Long.MAX_VALUE);
                Prefs.sp(c).edit().putInt("retGlobal", all[0]).putLong("retGlobalAt", now).apply();
            }
        } catch (Exception ignored) {}
    }

    /** Today's retention in ‰ (-1 if no review today). */
    static int retentionToday(Context c) {
        return Prefs.today().equals(Prefs.sp(c).getString("retDay", "")) ? Prefs.sp(c).getInt("retToday", -1) : -1;
    }
    static int retentionTodayCount(Context c) {
        return Prefs.today().equals(Prefs.sp(c).getString("retDay", "")) ? Prefs.sp(c).getInt("retTodayN", 0) : 0;
    }
    /** Overall retention (whole history) in ‰, -1 if unknown. */
    static int retentionGlobal(Context c) { return Prefs.sp(c).getInt("retGlobal", -1); }

    /** AnkiDroid's "next day starts at" hour (re-read at most once an hour). */
    static void readRollover(Context c) {
        if (System.currentTimeMillis() - Prefs.cutoffReadAt(c) < 3600000L) return;
        String[] q = {
                "SELECT coalesce((SELECT CAST(val AS INTEGER) FROM config WHERE KEY = 'rollover'), -1) + 1",   // recent schema
                "SELECT coalesce((SELECT json_extract(conf, '$.rollover') FROM col), -1) + 1"};               // old schema
        for (String sql : q) {
            try {
                int v = revlogValue(c, sql) - 1;
                if (v >= 0 && v <= 23) { Prefs.saveCutoff(c, v); return; }
            } catch (Exception ignored) {}
        }
    }

    /** Session progress read from history since its start time (exact, even past midnight/4 am). */
    static void readSession(Context c) {
        if (!Prefs.sessionActive(c)) return;
        try {
            long start = Prefs.sessionStartedAt(c), deck = Prefs.sessionDeckId(c);
            int[] v = studiedBetween(c, start, Long.MAX_VALUE / 4, deck);
            int due = -1;
            try { due = deckDue(c, deck); } catch (Exception ignored) {}
            Prefs.saveSessionProgress(c, start, v[0], v[1], due);
        } catch (Exception ignored) {}
    }

    /** Reads today's cards + minutes and stores them. Fallback without history: "rated:1" + Ankineer timer. */
    static void readStudied(Context c) throws Exception {
        try {
            String where = "FROM revlog WHERE id > " + dayStartMs() + " AND ease > 0";
            int cards = revlogValue(c, "SELECT count() " + where);
            int minutes = revlogValue(c, "SELECT coalesce(sum(time), 0) / 60000 " + where);
            Prefs.saveStudied(c, cards, minutes);
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            int cards;
            try {
                cards = countSearch(c, "cards", "rated:1");
            } catch (Exception e2) {
                cards = countSearch(c, "notes", "rated:1");
            }
            Prefs.saveStudied(c, cards, (int) (Prefs.studyMsToday(c) / 60000L));
        }
    }

    // ------------------------------------------------------------------ calcul

    /** Reads AnkiDroid, then computes (off the main thread). */
    static Result check(Context c) {
        String error = null;
        if (!isAnkiInstalled(c)) {
            error = L.t("AnkiDroid introuvable (ou API AnkiDroid désactivée)", "AnkiDroid not found (or AnkiDroid API disabled)");
        } else if (!hasPermission(c)) {
            error = L.t("Accès à AnkiDroid non autorisé", "AnkiDroid access not granted");
        } else {
            try {
                readRollover(c);
                readStudied(c);
                readSession(c);
                readRetention(c, false);
                if (Prefs.dueRequired(c) || Prefs.dueSufficient(c)) {
                    int due = totalDue(c);
                    Prefs.saveDue(c, due);
                    Prefs.dueBaseline(c, due);
                } else {
                    // Home "due cards" bar: read even without the options (an error here blocks nothing)
                    try {
                        int due = totalDue(c);
                        Prefs.saveDue(c, due);
                        Prefs.dueBaseline(c, due);
                    } catch (Exception ignored) {}
                }
            } catch (SecurityException e) {
                error = L.t("AnkiDroid a refusé l'accès", "AnkiDroid refused access");
            } catch (Exception e) {
                error = L.t("Lecture AnkiDroid impossible : ", "Cannot read AnkiDroid: ") + e.getMessage();
            }
        }
        Result r = snapshot(c, error);
        Prefs.saveStatus(c, r.text);
        if (r.apiOk) {
            Stats.recordToday(c, r.cards, r.minutes, r.dailyMet);
            int earned = Jokers.update(c, r.dailyMet);
            if (earned > 0) Reminders.jokerEarned(c, earned);
        }
        WidgetUpdater.updateAll(c, false);          // home-screen widgets (nothing if unchanged)
        return r;
    }

    /** Instant computation from the last values read (no disk access): widget, screens. */
    static Result snapshot(Context c) { return snapshot(c, null); }

    private static Result snapshot(Context c, String error) {
        L.load(c);
        Result r = new Result();
        r.error = error;
        int rc = Prefs.studiedCardsToday(c), rm = Prefs.studiedMinutesToday(c);
        r.apiOk = error == null && rc >= 0;
        r.cards = Math.max(0, rc);
        // minutes: those from AnkiDroid's history (fallback: Ankineer timer if never read today)
        r.minutes = rm >= 0 ? rm : (int) (Prefs.studyMsToday(c) / 60000L);
        r.goalCards = Prefs.dailyCards(c);
        r.goalMinutes = Prefs.dailyMinutes(c);
        r.dueRequired = Prefs.dueRequired(c);
        r.dueSufficient = Prefs.dueSufficient(c);
        r.due = Prefs.dueToday(c);
        r.fCards = Math.min(1f, r.cards / (float) r.goalCards);
        r.fMinutes = Math.min(1f, r.minutes / (float) r.goalMinutes);
        r.needCards = Prefs.needsCards(c);
        r.needMinutes = Prefs.needsMinutes(c);
        boolean dueOk = !r.dueRequired || r.due == 0;
        boolean thresholds = (!r.needCards || r.cards >= r.goalCards) && (!r.needMinutes || r.minutes >= r.goalMinutes) && dueOk;
        r.byDue = r.dueSufficient && r.due == 0 && !thresholds;
        r.dailyMet = r.apiOk && (thresholds || (r.dueSufficient && r.due == 0));
        if (r.dailyMet) Prefs.markGoalDone(c);

        float daily = r.dailyFraction();
        if (r.byDue && r.apiOk) daily = 1f;
        if (r.dueRequired) {
            int base = Prefs.dueBaseline(c, Math.max(0, r.due));
            float fDue = r.due < 0 ? 0f : base <= 0 ? 1f : 1f - r.due / (float) base;
            daily = Math.min(daily, fDue);
        }

        StringBuilder t = new StringBuilder();
        if (Prefs.sessionActive(c)) {
            r.session = true;
            r.sessionType = Prefs.sessionType(c);
            r.sessionTarget = Math.max(1, Prefs.sessionTarget(c));
            r.sessionDeck = Prefs.sessionDeckName(c);
            boolean sameDay = Prefs.today().equals(Prefs.sessionDay(c));
            boolean minutes = Prefs.SESSION_MINUTES.equals(r.sessionType), wholeDeck = Prefs.SESSION_DECK.equals(r.sessionType);
            int[] prog = Prefs.sessionProgress(c);
            int baseDue = Prefs.sessionDeckBase(c);
            r.sessionDue = prog != null ? prog[2] : -1;
            if (wholeDeck) {
                // target = due cards when it started; done = how many of them are gone
                r.sessionTarget = Math.max(1, baseDue);
                int left = r.sessionDue >= 0 ? r.sessionDue : Math.max(0, baseDue);
                r.sessionDone = Math.max(0, Math.min(r.sessionTarget, r.sessionTarget - left));
            } else if (prog != null) {
                r.sessionDone = minutes ? prog[1] : prog[0];          // history since the start of the session
            } else {
                int base = sameDay ? (minutes ? Prefs.sessionBaseMinutes(c) : Prefs.sessionBaseCards(c)) : 0;
                r.sessionDone = Math.max(0, (minutes ? r.minutes : r.cards) - base);
            }
            // the deck is finished (it had due cards at the start): the session ends too
            r.sessionDeckDone = r.apiOk && baseDue > 0 && r.sessionDue == 0;
            r.fSession = r.sessionDeckDone ? 1f : Math.min(1f, r.sessionDone / (float) r.sessionTarget);
            r.sessionMet = r.apiOk && (r.sessionDone >= r.sessionTarget || r.sessionDeckDone);
            if (r.sessionMet) {
                synchronized (GoalChecker.class) {          // a finished session is counted only once
                    if (Prefs.sessionActive(c)) {
                        Prefs.endSession(c, true);
                        Stats.addSession(c);
                    }
                }
            }
            t.append("Session : ").append(Math.min(r.sessionDone, r.sessionTarget)).append(" / ").append(r.sessionTarget)
                    .append(minutes ? " min" : wholeDeck ? L.t(" cartes dues", " due cards") : L.t(" cartes", " cards"))
                    .append(r.sessionDeck.length() > 0 ? "  ·  📚 " + r.sessionDeck : "").append('\n');
            r.fraction = r.fSession;
        } else {
            r.fraction = daily;
        }
        if (r.needCards) t.append(L.t("Cartes ", "Cards ")).append(r.cards).append(" / ").append(r.goalCards);
        if (r.needCards && r.needMinutes) t.append("  ·  ");
        if (r.needMinutes) t.append("Minutes ").append(r.minutes).append(" / ").append(r.goalMinutes);
        if (r.dueRequired || r.dueSufficient) t.append(L.t("  ·  Dues ", "  ·  Due ")).append(r.due < 0 ? "?" : String.valueOf(r.due));
        if (r.byDue && r.apiOk) t.append(L.t("\nToutes les cartes dues sont faites ✓", "\nAll due cards are done ✓"));
        if (error != null) t.append('\n').append(error);
        r.text = t.toString();
        r.shortText = Math.round(r.fraction * 100) + "%";
        return r;
    }
}
