package com.ankineer.app;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Home: today's state, big "Start" button, custom sessions. */
final class HomePage implements MainActivity.Page {
    private final MainActivity a;
    private final View root;
    private TextView stateTv, whyTv, cardsTv, minutesTv, dueTv, sessionTv;
    private Ui.Bar dueBar, sessionBar;
    private Charts.Ring cardsRing, minutesRing;
    private Button launch, start, typeCards, typeMinutes, typeDeck, deckBtn;
    private TextView deckNote;
    private long deckId;
    private String deckName;
    private EditText sessionEt;
    private LinearLayout sessionLive, sessionForm, cardsCol, minutesCol;
    private Button[] modeBtn;
    private TextView jokerTv, jokerSub;
    private Ui.Bar jokerBar;
    private Button jokerBtn;
    private String type;

    private LinearLayout crashBox;

    HomePage(MainActivity act) {
        a = act;
        type = Prefs.sessionType(a);
        deckId = Prefs.lastSessionDeckId(a);
        deckName = Prefs.lastSessionDeckName(a);
        LinearLayout col = Ui.column(a);

        // ---- last recorded crash (copyable to send to the developer)
        crashBox = Ui.card(a);
        crashBox.addView(Ui.text(a, "⚠ " + L.t("Un plantage a été enregistré", "A crash was recorded"), 15, Ui.AMBER, true));
        crashBox.addView(Ui.text(a, L.t("Copie le rapport et envoie-le au développeur pour qu'il corrige le problème.",
                "Copy the report and send it to the developer so the problem can be fixed."), 13, Ui.MUTED, false), Ui.topMargin(a, 4));
        LinearLayout cr = new LinearLayout(a);
        Button copyCrash = a.chip(L.t("Copier le rapport", "Copy report"));
        Button clearCrash = a.chip(L.t("Effacer", "Dismiss"));
        LinearLayout.LayoutParams c1 = new LinearLayout.LayoutParams(0, Ui.dp(a, 40), 1.4f);
        LinearLayout.LayoutParams c2 = new LinearLayout.LayoutParams(0, Ui.dp(a, 40), 1f);
        c2.leftMargin = Ui.dp(a, 8);
        cr.addView(copyCrash, c1);
        cr.addView(clearCrash, c2);
        crashBox.addView(cr, Ui.topMargin(a, 10));
        copyCrash.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String rep = CrashLog.last(a);
                if (rep != null) CrashLog.copy(a, rep);
            }
        });
        clearCrash.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                CrashLog.clear(a);
                crashBox.setVisibility(View.GONE);
            }
        });
        crashBox.setVisibility(View.GONE);
        col.addView(crashBox);

        // ---- today's state
        LinearLayout st = Ui.card(a);
        stateTv = Ui.text(a, "", 22, Ui.TEXT, true);
        st.addView(stateTv);
        whyTv = Ui.text(a, "", 13, Ui.MUTED, false);
        st.addView(whyTv, Ui.topMargin(a, 4));
        // goal mode: cards, minutes or both (once a day)
        st.addView(Ui.text(a, L.t("Mon objectif compte :", "My goal counts:"), 13, Ui.MUTED, true), Ui.topMargin(a, 14));
        modeBtn = a.goalModeRow(st);
        // cards and minutes: two rings (daily goal thresholds)
        LinearLayout rings = new LinearLayout(a);
        rings.setGravity(Gravity.CENTER);
        LinearLayout rc = Ui.column(a), rm = Ui.column(a);
        cardsCol = rc;
        minutesCol = rm;
        rc.setGravity(Gravity.CENTER_HORIZONTAL);
        rm.setGravity(Gravity.CENTER_HORIZONTAL);
        cardsRing = new Charts.Ring(a);
        minutesRing = new Charts.Ring(a);
        rc.addView(cardsRing, new LinearLayout.LayoutParams(Ui.dp(a, 132), Ui.dp(a, 132)));
        rm.addView(minutesRing, new LinearLayout.LayoutParams(Ui.dp(a, 132), Ui.dp(a, 132)));
        cardsTv = Ui.text(a, "", 13, Ui.MUTED, true);
        cardsTv.setGravity(Gravity.CENTER);
        minutesTv = Ui.text(a, "", 13, Ui.MUTED, true);
        minutesTv.setGravity(Gravity.CENTER);
        rc.addView(cardsTv, Ui.topMargin(a, 6));
        rm.addView(minutesTv, Ui.topMargin(a, 6));
        rings.addView(rc, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        rings.addView(rm, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        st.addView(rings, Ui.topMargin(a, 16));
        // due cards: progress bar (done since the start of the day)
        dueTv = Ui.text(a, "", 14, Ui.TEXT, true);
        st.addView(dueTv, Ui.topMargin(a, 16));
        dueBar = a.bar(st, Ui.AMBER);
        TextView refresh = Ui.text(a, L.t("↻  Actualiser depuis AnkiDroid", "↻  Refresh from AnkiDroid"), 13, Ui.ACCENT, true);
        refresh.setPadding(0, Ui.dp(a, 12), 0, Ui.dp(a, 2));
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                whyTv.setText(L.t("Lecture d'AnkiDroid…", "Reading AnkiDroid…"));
                a.runCheck();
            }
        });
        st.addView(refresh);
        col.addView(st);

        // ---- grand bouton Lancer
        launch = new Button(a);
        launch.setAllCaps(false);
        launch.setTextSize(19);
        launch.setStateListAnimator(null);
        launch.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(a, 68));
        llp.topMargin = Ui.dp(a, 4);
        launch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onLaunch(); }
        });
        col.addView(launch, llp);
        a.note(col, L.t("Verrouille le téléphone jusqu'à ton objectif du jour (réglable dans Paramètres).",
                "Locks your phone until you reach your daily goal (set it in Settings)."));

        // ---- jokers (streak rewards)
        a.section(col, L.t("Jokers", "Jokers"));
        LinearLayout jk = Ui.card(a);
        jokerTv = Ui.text(a, "", 18, Ui.TEXT, true);
        jk.addView(jokerTv);
        jokerBar = a.bar(jk, ProgressWidget.C_DONE);
        jokerSub = a.note(jk, "");
        jokerBtn = Ui.button(a, "", true);
        jk.addView(jokerBtn);
        a.note(jk, L.t("Chaque semaine de série (7 jours d'affilée avec ton objectif) te donne un joker : " + Jokers.MINUTES
                        + " minutes de liberté, même quand le téléphone est verrouillé. Maximum " + Jokers.MAX + " en réserve.",
                "Every week of streak (7 days in a row with your goal) earns you a joker: " + Jokers.MINUTES
                        + " free minutes, even while your phone is locked. Up to " + Jokers.MAX + " in stock."));
        col.addView(jk);
        jokerBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { askJoker(); }
        });

        // ---- sessions
        a.section(col, L.t("Sessions personnalisées", "Custom sessions"));
        LinearLayout cs = Ui.card(a);
        sessionLive = Ui.column(a);
        sessionTv = Ui.text(a, "", 18, Ui.TEXT, true);
        sessionLive.addView(sessionTv);
        sessionBar = a.bar(sessionLive, ProgressWidget.C_SESSION);
        a.note(sessionLive, L.t("Tout est bloqué jusqu'à la fin de la session (urgence possible depuis l'écran de blocage).",
                "Everything is locked until the session ends (emergency unlock available on the block screen)."));
        cs.addView(sessionLive);

        sessionForm = Ui.column(a);
        sessionForm.addView(Ui.text(a, L.t("Fixe-toi un objectif précis : le téléphone reste bloqué jusqu'à ce que tu l'atteignes.",
                "Set a precise target: your phone stays locked until you reach it."), 13, Ui.MUTED, false));
        // 1) deck
        sessionForm.addView(Ui.text(a, L.t("1. Paquet", "1. Deck"), 14, Ui.TEXT, true), Ui.topMargin(a, 12));
        deckBtn = Ui.button(a, "", false);
        deckBtn.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        deckBtn.setPadding(Ui.dp(a, 14), 0, Ui.dp(a, 14), 0);
        sessionForm.addView(deckBtn);
        deckBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickDeck(); }
        });
        // 2) target
        sessionForm.addView(Ui.text(a, L.t("2. Objectif", "2. Target"), 14, Ui.TEXT, true), Ui.topMargin(a, 14));
        LinearLayout seg = new LinearLayout(a);
        typeCards = a.chip(L.t("Cartes", "Cards"));
        typeMinutes = a.chip("Minutes");
        typeDeck = a.chip(L.t("Tout finir", "Finish all"));
        LinearLayout.LayoutParams s1 = new LinearLayout.LayoutParams(0, Ui.dp(a, 42), 1f);
        LinearLayout.LayoutParams s2 = new LinearLayout.LayoutParams(0, Ui.dp(a, 42), 1f);
        LinearLayout.LayoutParams s3 = new LinearLayout.LayoutParams(0, Ui.dp(a, 42), 1.1f);
        s2.leftMargin = Ui.dp(a, 8);
        s3.leftMargin = Ui.dp(a, 8);
        seg.addView(typeCards, s1);
        seg.addView(typeMinutes, s2);
        seg.addView(typeDeck, s3);
        sessionForm.addView(seg, Ui.topMargin(a, 8));
        sessionEt = a.numberField("");
        sessionForm.addView(sessionEt, Ui.topMargin(a, 8));
        deckNote = a.note(sessionForm, "");
        start = Ui.button(a, L.t("Démarrer la session 🔒", "Start session 🔒"), true);
        sessionForm.addView(start);
        cs.addView(sessionForm);
        col.addView(cs);

        typeCards.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setType(Prefs.SESSION_CARDS); }
        });
        typeMinutes.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setType(Prefs.SESSION_MINUTES); }
        });
        typeDeck.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setType(Prefs.SESSION_DECK); }
        });
        start.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Prefs.SESSION_DECK.equals(type)) { begin(type, 0); return; }
                int n;
                try { n = Integer.parseInt(sessionEt.getText().toString().trim()); }
                catch (NumberFormatException e) {
                    Toast.makeText(a, L.t("Indique un nombre.", "Enter a number."), Toast.LENGTH_SHORT).show();
                    return;
                }
                begin(type, n);
            }
        });
        setType(type);

        TextView brand = Ui.text(a, "Developed by Ankineer", 11, Ui.MUTED, false);
        brand.setGravity(Gravity.CENTER);
        col.addView(brand, Ui.topMargin(a, 22));
        root = a.scroll(col);
    }

    private void setType(String t) {
        type = t;
        boolean cards = Prefs.SESSION_CARDS.equals(t), whole = Prefs.SESSION_DECK.equals(t);
        a.styleChip(typeCards, cards);
        a.styleChip(typeMinutes, !cards && !whole);
        a.styleChip(typeDeck, whole);
        sessionEt.setVisibility(whole ? View.GONE : View.VISIBLE);
        if (!whole) {
            sessionEt.setHint(cards ? L.t("Nombre de cartes (ex. 40)", "Number of cards (e.g. 40)")
                    : L.t("Nombre de minutes (ex. 25)", "Number of minutes (e.g. 25)"));
            sessionEt.setText(String.valueOf(Prefs.lastSessionValue(a, t)));
        }
        showDeck();
    }

    private String deckLabel() {
        return deckId == 0 || deckName == null || deckName.length() == 0 ? L.t("Tous les paquets", "All decks") : deckName.replace("::", " › ");
    }

    private void showDeck() {
        deckBtn.setText("📚  " + deckLabel() + "   ▾");
        boolean whole = Prefs.SESSION_DECK.equals(type);
        deckNote.setText(whole
                ? L.t("La session se termine quand toutes les cartes dues de ce paquet sont faites.",
                "The session ends when every due card of this deck is done.")
                : L.t("Seules les révisions de ce paquet comptent. Si le paquet est terminé avant l'objectif, la session s'arrête aussi.",
                "Only reviews in this deck count. If the deck is finished before the target, the session ends too."));
    }

    /** Reads AnkiDroid's decks (off the main thread) and lets the user pick one. */
    private void pickDeck() {
        deckBtn.setText(L.t("Lecture des paquets…", "Reading decks…"));
        final Context ctx = a.getApplicationContext();
        Ui.safeThread(a, new Runnable() {
            @Override public void run() {
                java.util.List<GoalChecker.Deck> list = null;
                int total = -1;
                String err = null;
                try {
                    list = GoalChecker.decks(ctx);
                    total = GoalChecker.totalDue(ctx);
                } catch (Exception e) {
                    err = e.getMessage();
                }
                final java.util.List<GoalChecker.Deck> decks = list;
                final int all = total;
                final String error = err;
                a.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        showDeck();
                        if (decks == null) {
                            Toast.makeText(a, L.t("Impossible de lire les paquets AnkiDroid", "Can't read AnkiDroid decks")
                                    + (error != null ? " : " + error : ""), Toast.LENGTH_LONG).show();
                            return;
                        }
                        deckDialog(decks, all);
                    }
                });
            }
        }).start();
    }

    /** Deck chooser: only decks with due cards, with a search field. */
    private void deckDialog(java.util.List<GoalChecker.Deck> decks, final int all) {
        final java.util.List<GoalChecker.Deck> due = new java.util.ArrayList<GoalChecker.Deck>();
        for (GoalChecker.Deck d : decks) if (d.due > 0) due.add(d);
        LinearLayout box = Ui.column(a);
        int p = Ui.dp(a, 20);
        box.setPadding(p, Ui.dp(a, 8), p, 0);
        final EditText search = new EditText(a);
        search.setSingleLine(true);
        search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.MUTED);
        search.setTextSize(15);
        search.setHint("🔍  " + L.t("Rechercher un paquet…", "Search a deck…"));
        search.setBackground(Ui.round(a, Ui.BG, 10, Ui.STROKE));
        int pe = Ui.dp(a, 12);
        search.setPadding(pe, pe, pe, pe);
        box.addView(search);
        final TextView count = Ui.text(a, "", 12, Ui.MUTED, false);
        box.addView(count, Ui.topMargin(a, 8));
        final LinearLayout list = Ui.column(a);
        android.widget.ScrollView sc = new android.widget.ScrollView(a);
        sc.addView(list);
        box.addView(sc, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(a, 340)));
        final android.app.AlertDialog dlg = new android.app.AlertDialog.Builder(a, Ui.DARK ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle(L.t("Choisis un paquet", "Choose a deck"))
                .setView(box)
                .setNegativeButton(L.t("Annuler", "Cancel"), null)
                .create();
        final Runnable[] render = new Runnable[1];
        render[0] = new Runnable() {
            @Override public void run() {
                String q = search.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
                list.removeAllViews();
                if (q.length() == 0 && all > 0) list.addView(deckRow(dlg, 0, "", "🌐  " + L.t("Tous les paquets", "All decks"), "", all));
                int n = 0;
                for (GoalChecker.Deck d : due) {
                    if (q.length() > 0 && !d.name.toLowerCase(java.util.Locale.ROOT).replace("::", " ").contains(q)) continue;
                    int cut = d.name.lastIndexOf("::");
                    list.addView(deckRow(dlg, d.id, d.name, "📚  " + (cut < 0 ? d.name : d.name.substring(cut + 2)),
                            cut < 0 ? "" : d.name.substring(0, cut).replace("::", " › "), d.due));
                    n++;
                }
                count.setText(n + (n > 1 ? L.t(" paquets avec des cartes dues", " decks with due cards") : L.t(" paquet avec des cartes dues", " deck with due cards")));
                if (n == 0) {
                    TextView none = Ui.text(a, q.length() > 0 ? L.t("Aucun paquet ne correspond.", "No matching deck.")
                            : L.t("Aucune carte due aujourd'hui ✓", "No due card today ✓"), 14, Ui.MUTED, false);
                    none.setPadding(0, Ui.dp(a, 16), 0, 0);
                    list.addView(none);
                }
            }
        };
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) { render[0].run(); }
        });
        render[0].run();
        dlg.show();
    }

    /** One deck line: name, parent path, due-card badge; tap = choose it. */
    private View deckRow(final android.app.AlertDialog dlg, final long id, final String full, String title, String path, int dueCount) {
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int v = Ui.dp(a, 10);
        row.setPadding(Ui.dp(a, 4), v, Ui.dp(a, 4), v);
        LinearLayout tx = Ui.column(a);
        tx.addView(Ui.text(a, title, 15, Ui.TEXT, id == deckId));
        if (path.length() > 0) tx.addView(Ui.text(a, path, 11.5f, Ui.MUTED, false));
        row.addView(tx, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView badge = Ui.text(a, String.valueOf(dueCount), 12, 0xFFFFFFFF, true);
        badge.setBackground(Ui.round(a, Ui.AMBER, 10, 0));
        badge.setPadding(Ui.dp(a, 8), Ui.dp(a, 2), Ui.dp(a, 8), Ui.dp(a, 2));
        row.addView(badge);
        row.setClickable(true);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View x) {
                deckId = id;
                deckName = full;
                Prefs.setLastSessionDeck(a, deckId, deckName);
                showDeck();
                dlg.dismiss();
            }
        });
        return row;
    }

    @Override public View view() { return root; }

    // ------------------------------------------------------------------ actions

    private void onLaunch() {
        if (Prefs.armed(a)) {
            if (!Prefs.isEditable(a)) {
                Toast.makeText(a, L.t("Impossible d'arrêter pendant le verrou.", "Can't stop while locked."), Toast.LENGTH_LONG).show();
                return;
            }
            Prefs.setArmed(a, false);
            Toast.makeText(a, L.t("Verrou arrêté.", "Lock stopped."), Toast.LENGTH_SHORT).show();
            refresh();
            return;
        }
        launch.setText(L.t("Vérification…", "Checking…"));
        final Context ctx = a.getApplicationContext();
        Ui.safeThread(a, new Runnable() {
            @Override public void run() {
                a.lastRes = GoalChecker.check(ctx);
                a.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        String pb = a.blockingProblem();
                        if (pb != null) {
                            Toast.makeText(a, L.t("À corriger : ", "To fix: ") + pb, Toast.LENGTH_LONG).show();
                        } else {
                            Prefs.setArmed(a, true);
                            Toast.makeText(a, Prefs.unlockReason(a) == Prefs.REASON_GOAL
                                    ? L.t("Verrou lancé ✓ Objectif déjà atteint aujourd'hui : il bloquera dès demain 4h.",
                                    "Lock started ✓ Goal already reached today: it will lock from tomorrow 4 am.")
                                    : L.t("Verrou lancé 🔒", "Lock started 🔒"), Toast.LENGTH_LONG).show();
                        }
                        refresh();
                    }
                });
            }
        }).start();
    }

    /** Asks for confirmation: a session locks the phone until it ends (an accidental tap must not be enough). */
    private void begin(final String t, final int target) {
        final boolean minutes = Prefs.SESSION_MINUTES.equals(t), whole = Prefs.SESSION_DECK.equals(t);
        if (!whole && (target < 1 || target > (minutes ? 600 : 2000))) {
            Toast.makeText(a, minutes ? L.t("Entre 1 et 600 minutes.", "Between 1 and 600 minutes.")
                    : L.t("Entre 1 et 2000 cartes.", "Between 1 and 2000 cards."), Toast.LENGTH_SHORT).show();
            return;
        }
        String deck = deckLabel();
        String what = whole ? L.t("toutes les cartes dues", "every due card") : target + (minutes ? " minutes" : L.t(" cartes", " cards"));
        new android.app.AlertDialog.Builder(a, Ui.DARK ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle(whole ? L.t("Finir « ", "Finish “") + deck + L.t(" » ?", "”?")
                        : L.t("Démarrer une session de ", "Start a session of ") + what + " ?")
                .setMessage("📚 " + deck + "\n\n" + L.t("Le téléphone sera verrouillé jusqu'à ce que tu aies fait ", "Your phone will be locked until you've done ") + what
                        + L.t(" dans ce paquet", " in this deck")
                        + (whole ? "" : L.t(" (ou que le paquet soit terminé)", " (or the deck is finished)"))
                        + L.t(". Seules les urgences et les jokers permettront de sortir avant.", ". Only emergencies and jokers will let you out early."))
                .setPositiveButton(L.t("Démarrer \uD83D\uDD12", "Start \uD83D\uDD12"), new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) { startSession(t, target); }
                })
                .setNegativeButton(L.t("Annuler", "Cancel"), null)
                .show();
    }

    private void startSession(final String t, final int target) {
        final boolean minutes = Prefs.SESSION_MINUTES.equals(t), whole = Prefs.SESSION_DECK.equals(t);
        final long deck = deckId;
        final String dName = deckId == 0 ? "" : deckName;
        start.setText(L.t("Vérification…", "Checking…"));
        final Context ctx = a.getApplicationContext();
        Ui.safeThread(a, new Runnable() {
            @Override public void run() {
                a.lastRes = GoalChecker.check(ctx);             // up-to-date starting point
                int d = -1;
                try { d = GoalChecker.deckDue(ctx, deck); } catch (Exception ignored) {}
                final int due = d;
                a.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        String pb = a.blockingProblem();
                        if (pb == null && whole && due < 0) pb = L.t("impossible de lire les cartes dues de ce paquet", "can't read this deck's due cards");
                        if (pb != null) {
                            Toast.makeText(a, L.t("À corriger : ", "To fix: ") + pb, Toast.LENGTH_LONG).show();
                        } else if (whole && due == 0) {
                            Toast.makeText(a, L.t("Rien à réviser dans ce paquet aujourd'hui ✓", "Nothing to review in this deck today ✓"), Toast.LENGTH_LONG).show();
                        } else {
                            Prefs.startSession(a, t, whole ? due : target, Math.max(0, Prefs.studiedCardsToday(a)), Math.max(0, Prefs.studiedMinutesToday(a)),
                                    deck, dName, due);
                            Toast.makeText(a, L.t("Session lancée : ", "Session started: ") + (whole ? due + L.t(" cartes dues", " due cards") : target
                                    + (minutes ? " minutes" : L.t(" cartes", " cards"))) + (dName.length() > 0 ? " · " + dName : "") + " 🔒", Toast.LENGTH_LONG).show();
                        }
                        start.setText(L.t("Démarrer la session 🔒", "Start session 🔒"));
                        refresh();
                        a.startTickerIfNeeded();
                    }
                });
            }
        }).start();
    }

    private void askJoker() {
        if (!Jokers.usable(a)) return;
        new android.app.AlertDialog.Builder(a, Ui.DARK ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle(L.t("Utiliser un joker ?", "Use a joker?"))
                .setMessage(L.t("Ton téléphone sera libre pendant ", "Your phone will be free for ") + Jokers.MINUTES
                        + L.t(" minutes. Il te restera ", " minutes. You'll have ") + (Jokers.count(a) - 1) + L.t(" joker(s).", " joker(s) left."))
                .setPositiveButton(L.t("Utiliser 🃏", "Use 🃏"), new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) {
                        if (Jokers.use(a)) Toast.makeText(a, "🃏 " + L.t("Libre jusqu'à ", "Free until ") + Prefs.hhmm(Prefs.emergencyUntil(a)),
                                Toast.LENGTH_LONG).show();
                        refresh();
                    }
                })
                .setNegativeButton(L.t("Annuler", "Cancel"), null)
                .show();
    }

    private void showJokers() {
        int n = Jokers.count(a), left = Jokers.daysToNext(a);
        jokerTv.setText("🃏  " + n + (n > 1 ? L.t(" jokers", " jokers") : L.t(" joker", " joker")));
        jokerBar.set((Jokers.STREAK_DAYS - left) / (float) Jokers.STREAK_DAYS, ProgressWidget.C_DONE);
        jokerSub.setText(n >= Jokers.MAX ? L.t("Réserve pleine (" + Jokers.MAX + ")", "Stock full (" + Jokers.MAX + ")")
                : (Jokers.STREAK_DAYS - left) + "/" + Jokers.STREAK_DAYS + L.t(" jours de série · prochain joker dans ", " streak days · next joker in ")
                + left + (left > 1 ? L.t(" jours", " days") : L.t(" jour", " day")));
        boolean usable = Jokers.usable(a);
        jokerBtn.setText(usable ? L.t("Utiliser un joker — ", "Use a joker — ") + Jokers.MINUTES + L.t(" min libres", " free min")
                : n == 0 ? L.t("Pas encore de joker", "No joker yet")
                : L.t("Utilisable quand le téléphone est verrouillé", "Usable while your phone is locked"));
        Ui.enable(jokerBtn, usable);
    }

    // ------------------------------------------------------------------ affichage

    private void progress(GoalChecker.Result r) {
        cardsCol.setVisibility(r.needCards ? View.VISIBLE : View.GONE);
        minutesCol.setVisibility(r.needMinutes ? View.VISIBLE : View.GONE);
        boolean cOk = r.cards >= r.goalCards, mOk = r.minutes >= r.goalMinutes;
        cardsRing.set(r.fCards, cOk ? Ui.GREEN : ProgressWidget.C_CARDS, r.cards + "/" + r.goalCards, L.t("cartes", "cards"));
        minutesRing.set(r.fMinutes, mOk ? Ui.GREEN : ProgressWidget.C_MINUTES, r.minutes + "/" + r.goalMinutes, "min");
        cardsTv.setText(cOk ? L.t("Cartes ✓", "Cards ✓") : L.t("encore ", "") + (r.goalCards - r.cards) + L.t(" cartes", " cards to go"));
        cardsTv.setTextColor(cOk ? Ui.GREEN : Ui.MUTED);
        minutesTv.setText(mOk ? "Minutes ✓" : L.t("encore ", "") + (r.goalMinutes - r.minutes) + L.t(" min", " min to go"));
        minutesTv.setTextColor(mOk ? Ui.GREEN : Ui.MUTED);
        // due cards: share done since the start of the day
        if (r.due < 0) {
            dueTv.setText(L.t("Cartes dues  ?", "Due cards  ?"));
            dueBar.set(0f, Ui.AMBER);
        } else {
            int base = Math.max(r.due, Prefs.dueBaseline(a, r.due));
            int done = Math.max(0, base - r.due);
            float fd = base <= 0 ? 1f : done / (float) base;
            dueTv.setText(r.due == 0 ? L.t("Cartes dues  : toutes faites ✓", "Due cards: all done ✓")
                    : L.t("Cartes dues  ", "Due cards  ") + r.due + L.t(" restantes", " left") + (base > 0 ? "  ·  " + done + " / " + base + L.t(" faites", " done") : ""));
            dueBar.set(fd, r.due == 0 ? Ui.GREEN : Ui.AMBER);
        }

        boolean session = r.session && Prefs.sessionActive(a);
        sessionLive.setVisibility(session ? View.VISIBLE : View.GONE);
        sessionForm.setVisibility(session ? View.GONE : View.VISIBLE);
        if (session) {
            boolean min = Prefs.SESSION_MINUTES.equals(r.sessionType), whole = Prefs.SESSION_DECK.equals(r.sessionType);
            sessionTv.setText("⏱  " + Math.min(r.sessionDone, r.sessionTarget) + " / " + r.sessionTarget
                    + (min ? " minutes" : whole ? L.t(" cartes dues", " due cards") : L.t(" cartes", " cards"))
                    + (r.sessionDeck.length() > 0 ? "\n📚 " + r.sessionDeck.replace("::", " › ")
                    + (r.sessionDue >= 0 && !whole ? L.t("  ·  ", "  ·  ") + r.sessionDue + L.t(" dues restantes", " due left") : "") : ""));
            sessionBar.set(r.fSession, ProgressWidget.C_SESSION);
        }
    }

    @Override
    public void tick() {
        long left = Prefs.testUntil(a) - Clock.now();
        if (left > 0) stateTv.setText(L.t("Test en cours (", "Test running (") + (left / 1000 + 1) + " s)");
        progress(GoalChecker.snapshot(a));
    }

    @Override
    public void refresh() {
        crashBox.setVisibility(CrashLog.last(a) != null ? View.VISIBLE : View.GONE);
        boolean service = Apps.isServiceEnabled(a);
        int reason = Prefs.unlockReason(a);
        GoalChecker.Result r = GoalChecker.snapshot(a);
        String why;
        int col;
        String state;
        if (Prefs.testActive(a)) {
            state = L.t("Test en cours", "Test running"); col = Ui.ACCENT;
            why = L.t("Les apps non autorisées sont bloquées pendant 1 minute.", "Non-allowed apps are blocked for 1 minute.");
        } else if (!service || !LockService.running) {
            state = L.t("Service inactif", "Service off"); col = Ui.AMBER;
            why = L.t("Rien n'est bloqué : termine l'Installation (menu ☰).", "Nothing is blocked: finish Setup (☰ menu).");
        } else if (Prefs.emergencyActive(a)) {
            state = L.t("Urgence", "Emergency"); col = Ui.AMBER;
            why = L.t("Libre jusqu'à ", "Free until ") + Prefs.hhmm(Prefs.emergencyUntil(a)) + ".";
        } else if (Prefs.sessionActive(a)) {
            state = L.t("Session en cours 🔒", "Session running 🔒"); col = ProgressWidget.C_SESSION;
            why = L.t("Tout est bloqué jusqu'à la fin de la session.", "Everything is locked until the session ends.");
        } else if (!Prefs.armed(a)) {
            state = L.t("Verrou non lancé", "Lock not started"); col = Ui.AMBER;
            why = L.t("Appuie sur « Lancer » pour bloquer jusqu'à ton objectif du jour.", "Tap “Start” to lock until your daily goal.");
        } else if (reason == Prefs.REASON_GOAL) {
            state = L.t("Libre jusqu'à demain 4h ✓", "Free until tomorrow 4 am ✓"); col = Ui.GREEN;
            why = r.byDue ? L.t("Toutes les cartes dues sont faites.", "All due cards are done.")
                    : L.t("Objectif du jour atteint.", "Daily goal reached.");
        } else {
            state = L.t("Verrouillé 🔒", "Locked 🔒"); col = Ui.RED;
            why = L.t("Les apps non autorisées sont bloquées jusqu'à ton objectif du jour.", "Non-allowed apps are blocked until your daily goal.");
        }
        GoalChecker.Result lr = a.lastRes;
        if (lr != null && lr.error != null) why += "\n⚠ " + lr.error;
        stateTv.setText(state);
        stateTv.setTextColor(col);
        whyTv.setText(why);
        a.styleGoalMode(modeBtn);
        showJokers();
        progress(r);

        boolean armed = Prefs.armed(a);
        if (!armed) {
            launch.setText(L.t("🔒  LANCER LE VERROU", "🔒  START THE LOCK"));
            launch.setTextColor(0xFFFFFFFF);
            launch.setBackground(Ui.round(a, Ui.ACCENT, 18, 0));
        } else if (Prefs.isEditable(a)) {
            launch.setText(L.t("Arrêter le verrou", "Stop the lock"));
            launch.setTextColor(Ui.TEXT);
            launch.setBackground(Ui.round(a, 0x00000000, 18, Ui.STROKE));
        } else {
            launch.setText(L.t("🔒  VERROU ACTIF", "🔒  LOCK ACTIVE"));
            launch.setTextColor(0xFFFFFFFF);
            launch.setBackground(Ui.round(a, Ui.RED, 18, 0));
        }
    }
}
