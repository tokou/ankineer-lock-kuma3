package com.ankineer.app;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * About: gradient hero with key numbers, "how it works" in 3 steps, features grouped by theme
 * (each card opens on tap), then the Ankineer ecosystem (each card opens its video).
 */
final class AboutPage implements MainActivity.Page {
    private final View root;
    private int shown;

    static final int BLUE = 0xFF5B8CFF, PURPLE = 0xFFB16CFF, GREEN = 0xFF3DDC97, ORANGE = 0xFFFF9F43, AMBER = 0xFFFFB547,
            RED = 0xFFFF6B6B, GREY = 0xFF8A93A3;

    AboutPage(MainActivity a) {
        LinearLayout col = Ui.column(a);
        col.setLayoutTransition(new LayoutTransition());

        col.addView(hero(a));
        steps(a, col);

        group(a, col, L.t("L'essentiel", "The essentials"));
        feature(a, col, "🔒", BLUE, L.t("Le principe", "The idea"),
                L.t("Révise d'abord, profite ensuite.", "Review first, enjoy after."),
                L.t("Tant que ton objectif Anki du jour n'est pas atteint, les applications qui distraient (réseaux sociaux, vidéos, jeux…) sont bloquées. "
                                + "AnkiDroid, le téléphone, les SMS et les apps que tu autorises restent toujours accessibles. "
                                + "Dès que l'objectif est atteint, tout se débloque jusqu'au changement de jour d'AnkiDroid (4h par défaut, lu dans tes réglages AnkiDroid).",
                        "Until your daily Anki goal is reached, distracting apps (social media, videos, games…) are blocked. "
                                + "AnkiDroid, phone, SMS and the apps you allow always stay available. "
                                + "Once the goal is reached, everything unlocks until AnkiDroid's next day change (4 am by default, read from your AnkiDroid settings)."));
        feature(a, col, "🎯", GREEN, L.t("L'objectif du jour", "The daily goal"),
                L.t("Cartes, minutes, ou les deux : à toi de choisir.", "Cards, minutes or both: your choice."),
                L.t("Choisis ce qui compte, sur l'Accueil ou dans Paramètres : 🃏 cartes seulement (au moins " + Prefs.MIN_DAILY_CARDS + "), "
                                + "⏱ minutes seulement (au moins " + Prefs.MIN_DAILY_MINUTES + "), ou 🎯 les deux seuils ensemble. "
                                + "Ce choix se change une seule fois par jour, et pendant le verrou seulement vers « Les deux ».\n\n"
                                + "Les chiffres sont exactement ceux qu'affiche AnkiDroid (« Étudié X cartes en Y minutes aujourd'hui ») : "
                                + "ils viennent de l'historique des révisions d'AnkiDroid, donc tes révisions sur PC comptent dès qu'AnkiDroid est synchronisé. "
                                + "Tu peux augmenter les seuils à tout moment ; tu ne peux les baisser que lorsque le téléphone est libre.",
                        "Choose what counts, on Home or in Settings: 🃏 cards only (at least " + Prefs.MIN_DAILY_CARDS + "), "
                                + "⏱ minutes only (at least " + Prefs.MIN_DAILY_MINUTES + "), or 🎯 both thresholds together. "
                                + "This choice can be changed once a day, and while locked only to “Both”.\n\n"
                                + "The numbers are exactly those AnkiDroid shows (“Studied X cards in Y minutes today”): "
                                + "they come from AnkiDroid's review history, so your PC reviews count as soon as AnkiDroid is synced. "
                                + "You can raise the thresholds anytime; you can only lower them while your phone is free."));
        feature(a, col, "📚", BLUE, L.t("Les cartes dues", "Due cards"),
                L.t("Plus rien à réviser ? Tu es libre.", "Nothing left to review? You're free."),
                L.t("Deux options dans Paramètres :\n"
                                + "• « Débloquer dès que toutes les cartes dues sont faites » (activé par défaut, désactivable) : s'il ne reste rien à réviser, les seuils ne s'appliquent pas.\n"
                                + "• « Exiger aussi 0 carte due » : il faut en plus avoir terminé toutes les cartes dues du jour.",
                        "Two options in Settings:\n"
                                + "• “Unlock as soon as all due cards are done” (on by default, can be turned off): if there's nothing left to review, the thresholds don't apply.\n"
                                + "• “Also require 0 due cards”: you must also finish all of today's due cards."));
        feature(a, col, "⏱", ORANGE, L.t("Les sessions", "Sessions"),
                L.t("Un paquet, un objectif, mode concentration.", "One deck, one target, focus mode."),
                L.t("Une session est un objectif ponctuel que tu te fixes. Choisis d'abord le paquet (ou tous les paquets), puis l'objectif : "
                                + "un nombre de cartes (ex. 40), des minutes (ex. 25) ou « Tout finir » (toutes les cartes dues du paquet). "
                                + "Seules les révisions de ce paquet comptent, et si le paquet est terminé avant l'objectif, la session s'arrête aussi. "
                                + "Pendant la session, tout est bloqué — même si l'objectif du jour est déjà atteint. "
                                + "Le compte part de zéro au lancement et la session s'arrête toute seule à la fin.",
                        "A session is a one-off target you set yourself. First choose the deck (or all decks), then the target: "
                                + "a number of cards (e.g. 40), minutes (e.g. 25) or “Finish all” (every due card in the deck). "
                                + "Only reviews in that deck count, and if the deck is finished before the target, the session ends too. "
                                + "During the session everything is locked — even if the daily goal is already reached. "
                                + "The count starts at zero and the session ends on its own."));
        feature(a, col, "🃏", GREEN, L.t("Les jokers", "Jokers"),
                L.t("Ta série récompensée : 30 min de liberté.", "Your streak rewarded: 30 free minutes."),
                L.t("Chaque semaine de série (7 jours d'affilée avec ton objectif atteint) te donne un joker. Un joker libère le téléphone "
                                + Jokers.MINUTES + " minutes, même pendant le verrou ou une session — depuis l'Accueil ou l'écran de blocage (deux touches pour confirmer). "
                                + "Jusqu'à " + Jokers.MAX + " jokers en réserve. Une notification te prévient quand tu en gagnes un.",
                        "Every week of streak (7 days in a row with your goal reached) earns you a joker. A joker frees your phone for "
                                + Jokers.MINUTES + " minutes, even while locked or in a session — from Home or the block screen (two taps to confirm). "
                                + "Up to " + Jokers.MAX + " jokers in stock. A notification tells you when you earn one."));

        group(a, col, L.t("Pendant le verrou", "While locked"));
        feature(a, col, "▬", PURPLE, L.t("La barre de progression", "The progress bar"),
                L.t("Ta progression, toujours sous les yeux.", "Your progress, always in sight."),
                L.t("Une petite barre reste en haut de l'écran pendant le verrou : barre bleue = cartes, barre violette = minutes "
                                + "(seulement celles que ton objectif compte), pourcentage à droite. "
                                + "Pendant une session, une barre orange montre ce qu'il reste ; pendant une urgence, une barre rouge se vide. "
                                + "Touche-la pour ouvrir Ankineer Lock, glisse-la pour la déplacer.",
                        "A small bar stays at the top of the screen while locked: blue bar = cards, purple bar = minutes "
                                + "(only those your goal counts), percentage on the right. "
                                + "During a session an orange bar shows what's left; during an emergency a red bar empties. "
                                + "Tap it to open Ankineer Lock, drag it to move it."));
        feature(a, col, "🚫", RED, L.t("L'écran de blocage", "The block screen"),
                L.t("Une app bloquée ? Retour à l'essentiel.", "A blocked app? Back to what matters."),
                L.t("Quand tu ouvres une app bloquée, Ankineer Lock te ramène à l'accueil et affiche ta progression, avec un bouton pour ouvrir AnkiDroid "
                                + "et les urgences disponibles.",
                        "When you open a blocked app, Ankineer Lock sends you back to the home screen and shows your progress, with a button to open AnkiDroid "
                                + "and the emergencies available."));
        feature(a, col, "🚨", RED, L.t("Les urgences", "Emergencies"),
                L.t("Une porte de sortie, une fois par jour.", "A way out, once a day."),
                L.t("Deux moyens de déblocage, réglables dans la section Urgence :\n"
                                + "• Par code : libère tout de suite (15 min par défaut), une seule fois par jour, 5 essais maximum. "
                                + "Idéal si quelqu'un de confiance choisit le code.\n"
                                + "• Extrême urgence : 5 minutes tout de suite, une seule fois par jour.\n"
                                + "Avant le code et l'extrême urgence, un grand avertissement rappelle que c'est une autorisation exceptionnelle "
                                + "et qu'il faut revenir à la révision rapidement. Pendant l'urgence, la barre devient un compte à rebours rouge. "
                                + "Une urgence libère tes apps, mais ne permet jamais d'arrêter le verrou ni de baisser l'objectif.",
                        "Two ways to unlock, set in the Emergency section:\n"
                                + "• With a code: frees right away (15 min by default), once a day only, 5 attempts max. "
                                + "Best if someone you trust picks the code.\n"
                                + "• Extreme emergency: 5 minutes right away, once a day only.\n"
                                + "Before the code and the extreme emergency, a big warning reminds you this is an exceptional permission "
                                + "and that you must get back to your reviews quickly. During the emergency, the bar becomes a red countdown. "
                                + "An emergency frees your apps but never lets you stop the lock or lower the goal."));
        feature(a, col, "🔔", AMBER, L.t("Les rappels", "Reminders"),
                L.t("Un coup de pouce chaque heure.", "A nudge every hour."),
                L.t("Chaque heure, deux notifications distinctes : « Encore N cartes dues » s'il reste des cartes à réviser, "
                                + "et « Objectif minimum pas encore atteint » avec ce qu'il te manque, chacune avec une citation de motivation différente. "
                                + "Rien pendant les heures calmes (23h → 8h par défaut). "
                                + "Si Android coupe le service d'accessibilité, une alerte arrive au plus toutes les heures. "
                                + "Le dimanche soir, un résumé compare ta semaine (cartes, minutes, jours réussis, série) à la précédente. "
                                + "Tout se règle dans Paramètres → Rappels.",
                        "Every hour, two separate notifications: “Still N due cards” if cards are left to review, "
                                + "and “Minimum goal not reached yet” with what's missing, each with a different motivational quote. "
                                + "Nothing during quiet hours (11 pm → 8 am by default). "
                                + "If Android turns the accessibility service off, you get an alert at most every hour. "
                                + "On Sunday evening, a summary compares your week (cards, minutes, days reached, streak) with the previous one. "
                                + "Everything is in Settings → Reminders."));

        group(a, col, L.t("Ton suivi", "Your progress"));
        feature(a, col, "📊", GREEN, L.t("Les statistiques", "Statistics"),
                L.t("Séries, rétention, tendances.", "Streaks, retention, trends."),
                L.t("Sur 7 jours, 30 jours ou 1 an : série et meilleure série, taux de réussite, moyennes, temps par carte, record, tendance, "
                                + "graphiques jour par jour, ton jour de la semaine le plus productif, carte d'activité sur 1 an, sessions et urgences.",
                        "Over 7 days, 30 days or 1 year: streak and best streak, success rate, averages, time per card, record, trend, "
                                + "day-by-day charts, your most productive weekday, a 1-year activity map, sessions and emergencies."));
        feature(a, col, "📅", GREEN, L.t("L'agenda et les séries", "Calendar and streaks"),
                L.t("365 jours d'historique, même avant l'installation.", "365 days of history, even before install."),
                L.t("L'agenda montre les 365 derniers jours d'après l'historique d'AnkiDroid, même avant l'installation : vert = objectif atteint "
                                + "(selon ton objectif : cartes, minutes ou les deux), rouge = non atteint, point orange = session terminée. Les séries sont calculées sur tout cet historique. "
                                + "La rétention du jour (réponses autres que « À revoir » sur les cartes en révision) est comparée à ta rétention globale.",
                        "The calendar shows the last 365 days from AnkiDroid's history, even before install: green = goal reached "
                                + "(according to your goal: cards, minutes or both), red = not reached, orange dot = session completed. Streaks are computed over this whole history. "
                                + "Today's retention (answers other than “Again” on review cards) is compared with your overall retention."));
        feature(a, col, "🧩", BLUE, L.t("Les widgets d'écran d'accueil", "Home screen widgets"),
                L.t("4 widgets × 4 styles.", "4 widgets × 4 styles."),
                L.t("Quatre widgets : anneaux (2×2), mini anneau (1×1), barres cartes · minutes (4×1) et semaine + série (4×2). "
                                + "Quatre styles au choix dans Paramètres : sombre, clair, verre, dégradé. Ils changent de couleur selon l'état : "
                                + "verrouillé, session, urgence, libre.",
                        "Four widgets: rings (2×2), mini ring (1×1), cards · minutes bars (4×1) and week + streak (4×2). "
                                + "Four styles in Settings: dark, light, glass, gradient. Their colour follows the state: locked, session, emergency, free."));

        group(a, col, L.t("Sécurité et confiance", "Safety and trust"));
        feature(a, col, "🧱", AMBER, L.t("Protection contre la triche", "Anti-cheat protection"),
                L.t("Pas de raccourci pendant le verrou.", "No shortcuts while locked."),
                L.t("Pendant le verrou, les réglages qui assouplissent l'objectif sont figés (seuils, mode d'objectif, cartes dues), et l'option « Bloquer les Paramètres pendant le verrou » "
                                + "empêche de couper le service. Pour sortir plus tôt : l'extrême urgence (5 min), le code ou un joker. Ce n'est pas une prison (un redémarrage en mode sans échec reste possible) : "
                                + "c'est une barrière forte qui t'aide à tenir ton engagement.",
                        "While locked, settings that would relax the goal are frozen (thresholds, goal mode, due cards), and “Block Settings while locked” "
                                + "prevents turning off the service. To get out early: the extreme emergency (5 min), the code or a joker. It isn't a prison (a safe-mode reboot is still possible): "
                                + "it's a strong barrier that helps you keep your commitment."));
        feature(a, col, "🛡", PURPLE, L.t("Avant de désinstaller", "Before uninstalling"),
                L.t("3 minutes pour y réfléchir.", "3 minutes to think it over."),
                L.t("Tu peux toujours désinstaller Ankineer Lock. Mais quand tu essaies, une grande fenêtre « Attention, réfléchis avant de désinstaller » "
                                + "apparaît, avec un message de motivation, ta série en cours et un minuteur de 3 minutes. "
                                + "« Je garde Ankineer Lock » la ferme à tout moment ; une fois les 3 minutes passées, « Désinstaller quand même » ouvre la fenêtre de désinstallation d'Android. "
                                + "Rien d'autre n'est bloqué (Play Store, infos de l'appli…).",
                        "You can always uninstall Ankineer Lock. But when you try, a big “Warning, think before uninstalling” window "
                                + "appears, with a motivational message, your current streak and a 3-minute timer. "
                                + "“Keep Ankineer Lock” closes it at any time; once the 3 minutes are over, “Uninstall anyway” opens Android's uninstall window. "
                                + "Nothing else is blocked (Play Store, app info…)."));
        feature(a, col, "🚀", GREEN, L.t("Lancement automatique", "Automatic start"),
                L.t("Configure une fois, c'est parti.", "Set it up once, off you go."),
                L.t("Activé par défaut. Dès que l'installation est terminée (AnkiDroid connecté, service actif, batterie non optimisée, applications autorisées choisies), "
                                + "le verrou se lance tout seul. Si tu l'arrêtes à la main quand le téléphone est libre, il se relance le lendemain.",
                        "On by default. As soon as setup is complete (AnkiDroid connected, service running, battery not optimised, allowed apps chosen), "
                                + "the lock starts on its own. If you stop it by hand while your phone is free, it starts again the next day."));
        feature(a, col, "⬆", BLUE, L.t("Les mises à jour", "Updates"),
                L.t("Sans désinstaller, sans rien perdre.", "No uninstall, nothing lost."),
                L.t("Section Mise à jour : choisis le nouvel APK, Ankineer Lock vérifie que c'est bien lui (même signature, version plus récente) "
                                + "et lance la mise à jour par Android. Pas de désinstallation : réglages, statistiques et service sont gardés.",
                        "Update section: pick the new APK, Ankineer Lock checks it's really itself (same signature, newer version) "
                                + "and starts the update through Android. No uninstall: settings, stats and the service are kept."));
        feature(a, col, "🔐", GREY, L.t("Vie privée", "Privacy"),
                L.t("Zéro Internet, zéro donnée envoyée.", "Zero Internet, zero data sent."),
                L.t("Tout reste sur ton téléphone. Ankineer Lock n'a pas accès à Internet, ne lit pas le contenu de tes écrans et n'envoie aucune donnée. "
                                + "Il lit seulement, dans AnkiDroid, ton nombre de cartes, ton temps d'étude et tes cartes dues.",
                        "Everything stays on your phone. Ankineer Lock has no Internet access, doesn't read your screen content and sends no data. "
                                + "It only reads your card count, study time and due cards from AnkiDroid."));

        feature(a, col, "🎬", AMBER, L.t("Les tutoriels", "Tutorials"),
                L.t("Toutes les vidéos de la chaîne The Ankineer.", "Every video of The Ankineer channel."),
                L.t("Menu ☰ → Tutoriels : toutes les vidéos de la chaîne YouTube The Ankineer, avec recherche et catégories "
                                + "(apprendre Anki, add-ons, Ankineer Lock, la chaîne). La liste est intégrée à l'app (pas d'accès Internet) "
                                + "et s'enrichit à chaque mise à jour : les nouvelles vidéos sont marquées NOUVEAU et une notification te les annonce.",
                        "☰ menu → Tutorials: every video of The Ankineer YouTube channel, with search and categories "
                                + "(learn Anki, add-ons, Ankineer Lock, the channel). The list is built into the app (no Internet access) "
                                + "and grows with each update: new videos are marked NEW and a notification tells you about them."));

        // ---- Ankineer ecosystem: each card opens its video
        group(a, col, L.t("Écosystème Ankineer", "Ankineer ecosystem"));
        TextView eco = Ui.text(a, L.t("Les autres outils Ankineer pour Anki sur ordinateur. Touche une carte pour voir sa vidéo.",
                "The other Ankineer tools for Anki on your computer. Tap a card to watch its video."), 13, Ui.MUTED, false);
        col.addView(eco);
        LinearLayout grid = new LinearLayout(a);
        String[][] tools = {
                {"⚡", "FocusForce", L.t("Verrou d'étude sur Anki PC", "Study lock for Anki desktop"), FOCUSFORCE},
                {"✏️", "PopEdit", L.t("Barre d'édition pendant la révision", "Editing toolbar while reviewing"), POPEDIT},
                {"✨", "Ankineer AI", L.t("Cartes créées par l'IA", "Cards made by AI"), ANKINEER_AI}};
        int[] tints = {AMBER, GREEN, PURPLE};
        for (int i = 0; i < tools.length; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) lp.leftMargin = Ui.dp(a, 8);
            grid.addView(tile(a, tools[i][0], tints[i], tools[i][1], tools[i][2], tools[i][3]), lp);
        }
        col.addView(grid, Ui.topMargin(a, 10));

        TextView brand = Ui.text(a, "Developed by Ankineer", 14, Ui.TEXT, true);
        brand.setGravity(Gravity.CENTER);
        col.addView(brand, Ui.topMargin(a, 26));
        TextView sub = Ui.text(a, L.t("Gratuit et open source (GPL-3.0)", "Free and open source (GPL-3.0)") + "\ngithub.com/BONJ37/ankineer-lock", 12, Ui.MUTED, false);
        sub.setGravity(Gravity.CENTER);
        col.addView(sub, Ui.topMargin(a, 2));

        root = a.scroll(col);
    }

    /** YouTube videos of the ecosystem tools. */
    static final String FOCUSFORCE = "Kd-NGVGfhS8", POPEDIT = "fATrUeXQbwg", ANKINEER_AI = "ELSonoU8Q8k";

    /** Gradient banner: name, tagline, version and three key numbers. */
    private View hero(MainActivity a) {
        LinearLayout h = Ui.column(a);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{BLUE, PURPLE});
        g.setCornerRadius(Ui.dp(a, 22));
        h.setBackground(g);
        int p = Ui.dp(a, 20);
        h.setPadding(p, p, p, p);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hl.bottomMargin = Ui.dp(a, 14);
        h.setLayoutParams(hl);

        LinearLayout top = new LinearLayout(a);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = Ui.text(a, "🔒", 26, 0xFFFFFFFF, false);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.round(a, 0x33FFFFFF, 16, 0));
        int s = Ui.dp(a, 52);
        top.addView(icon, new LinearLayout.LayoutParams(s, s));
        LinearLayout names = Ui.column(a);
        names.addView(Ui.text(a, "Ankineer Lock", 24, 0xFFFFFFFF, true));
        TextView ver = Ui.text(a, "Version " + version(a), 11, 0xFFFFFFFF, true);
        ver.setBackground(Ui.round(a, 0x33FFFFFF, 10, 0));
        ver.setPadding(Ui.dp(a, 8), Ui.dp(a, 2), Ui.dp(a, 8), Ui.dp(a, 2));
        LinearLayout.LayoutParams vl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vl.topMargin = Ui.dp(a, 4);
        names.addView(ver, vl);
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nl.leftMargin = Ui.dp(a, 14);
        top.addView(names, nl);
        h.addView(top);

        h.addView(Ui.text(a, L.t("Ton téléphone se libère quand ton travail Anki est fait.",
                "Your phone unlocks once your Anki work is done."), 15, 0xEEFFFFFF, false), Ui.topMargin(a, 14));

        LinearLayout stats = new LinearLayout(a);
        String[][] kv = {
                {Prefs.MIN_DAILY_CARDS + "+", L.t("cartes / jour", "cards / day")},
                {Prefs.MIN_DAILY_MINUTES + "+", L.t("minutes / jour", "minutes / day")},
                {"0", L.t("accès Internet", "Internet access")}};
        for (int i = 0; i < kv.length; i++) {
            LinearLayout c = Ui.column(a);
            c.setGravity(Gravity.CENTER_HORIZONTAL);
            c.setBackground(Ui.round(a, 0x26FFFFFF, 14, 0));
            c.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 10));
            TextView v = Ui.text(a, kv[i][0], 20, 0xFFFFFFFF, true);
            v.setGravity(Gravity.CENTER);
            c.addView(v);
            TextView l = Ui.text(a, kv[i][1], 11, 0xDDFFFFFF, false);
            l.setGravity(Gravity.CENTER);
            c.addView(l);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = Ui.dp(a, 8);
            stats.addView(c, lp);
        }
        h.addView(stats, Ui.topMargin(a, 16));
        appear(h);
        return h;
    }

    /** "How it works": three numbered steps linked by a line. */
    private void steps(MainActivity a, LinearLayout col) {
        group(a, col, L.t("Comment ça marche", "How it works"));
        LinearLayout card = Ui.card(a);
        String[][] st = {
                {"1", "📖", L.t("Révise dans AnkiDroid", "Review in AnkiDroid"), L.t("Téléphone ou PC synchronisé", "Phone or synced PC")},
                {"2", "🎯", L.t("Atteins ton objectif", "Reach your goal"), L.t("Cartes, minutes ou les deux", "Cards, minutes or both")},
                {"3", "🔓", L.t("Ton téléphone se libère", "Your phone unlocks"), L.t("Jusqu'à demain 4h", "Until tomorrow 4 am")}};
        int[] tint = {BLUE, PURPLE, GREEN};
        for (int i = 0; i < st.length; i++) {
            LinearLayout row = new LinearLayout(a);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView num = Ui.text(a, st[i][0], 15, 0xFFFFFFFF, true);
            num.setGravity(Gravity.CENTER);
            GradientDrawable dot = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{tint[i], (tint[i] & 0x00FFFFFF) | 0xAA000000});
            dot.setShape(GradientDrawable.OVAL);
            num.setBackground(dot);
            int s = Ui.dp(a, 34);
            row.addView(num, new LinearLayout.LayoutParams(s, s));
            LinearLayout txt = Ui.column(a);
            txt.addView(Ui.text(a, st[i][1] + "  " + st[i][2], 15, Ui.TEXT, true));
            txt.addView(Ui.text(a, st[i][3], 12.5f, Ui.MUTED, false));
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tl.leftMargin = Ui.dp(a, 14);
            row.addView(txt, tl);
            card.addView(row);
            if (i < st.length - 1) {
                View line = new View(a);
                line.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{tint[i], tint[i + 1]}));
                LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(Ui.dp(a, 3), Ui.dp(a, 18));
                ll.leftMargin = Ui.dp(a, 15.5f);
                ll.topMargin = Ui.dp(a, 4);
                ll.bottomMargin = Ui.dp(a, 4);
                card.addView(line, ll);
            }
        }
        appear(card);
        col.addView(card);
    }

    /** Group title with a small accent line. */
    private static void group(MainActivity a, LinearLayout col, String title) {
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        View accent = new View(a);
        accent.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{BLUE, PURPLE}));
        row.addView(accent, new LinearLayout.LayoutParams(Ui.dp(a, 18), Ui.dp(a, 3)));
        TextView t = Ui.text(a, title.toUpperCase(java.util.Locale.ROOT), 12, Ui.MUTED, true);
        t.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tl.leftMargin = Ui.dp(a, 8);
        row.addView(t, tl);
        LinearLayout.LayoutParams rl = Ui.topMargin(a, 14);
        rl.bottomMargin = Ui.dp(a, 10);
        rl.leftMargin = Ui.dp(a, 4);
        col.addView(row, rl);
    }

    /** Feature card: tinted icon, title and tagline; tap to open the full explanation. */
    private void feature(MainActivity a, LinearLayout col, String icon, int tint, String title, String tagline, String body) {
        LinearLayout c = Ui.card(a);
        c.setBackground(Ui.round(a, Ui.CARD, 16, (tint & 0x00FFFFFF) | 0x55000000));
        c.setLayoutTransition(new LayoutTransition());
        LinearLayout head = new LinearLayout(a);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(badge(a, icon, tint, 42, 20));
        LinearLayout tx = Ui.column(a);
        tx.addView(Ui.text(a, title, 16, Ui.TEXT, true));
        tx.addView(Ui.text(a, tagline, 12.5f, tint, false));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tl.leftMargin = Ui.dp(a, 12);
        head.addView(tx, tl);
        final TextView chevron = Ui.text(a, "▾", 18, Ui.MUTED, true);
        head.addView(chevron);
        c.addView(head);
        final TextView b = Ui.text(a, body, 14, Ui.MUTED, false);
        b.setLineSpacing(Ui.dp(a, 3), 1f);
        b.setVisibility(View.GONE);
        c.addView(b, Ui.topMargin(a, 12));
        c.setClickable(true);
        c.setContentDescription(title + " — " + tagline);
        c.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean open = b.getVisibility() != View.VISIBLE;
                b.setVisibility(open ? View.VISIBLE : View.GONE);
                chevron.animate().rotation(open ? 180f : 0f).setDuration(200).start();
            }
        });
        appear(c);
        col.addView(c);
    }

    /** Fade + slide-in, one card after the other. */
    private void appear(View v) {
        v.setAlpha(0f);
        v.setTranslationY(24f * v.getResources().getDisplayMetrics().density);
        v.animate().alpha(1f).translationY(0f).setStartDelay(Math.min(600, 45L * shown++)).setDuration(320).start();
    }

    /** Icon in a rounded square tinted with its colour (soft gradient). */
    private static TextView badge(MainActivity a, String icon, int tint, int sizeDp, float textSp) {
        TextView v = Ui.text(a, icon, textSp, tint, true);
        v.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{(tint & 0x00FFFFFF) | 0x40000000, (tint & 0x00FFFFFF) | 0x14000000});
        g.setCornerRadius(Ui.dp(a, 12));
        v.setBackground(g);
        int s = Ui.dp(a, sizeDp);
        v.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return v;
    }

    /** Ecosystem card: icon, name, role and link to the video. */
    private static View tile(final MainActivity a, String icon, int tint, String name, String role, final String video) {
        LinearLayout c = Ui.column(a);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setBackground(Ui.round(a, Ui.CARD, 16, (tint & 0x00FFFFFF) | 0x55000000));
        int p = Ui.dp(a, 12);
        c.setPadding(p, Ui.dp(a, 14), p, Ui.dp(a, 14));
        c.addView(badge(a, icon, tint, 44, 22));
        TextView n = Ui.text(a, name, 14, Ui.TEXT, true);
        n.setGravity(Gravity.CENTER);
        c.addView(n, Ui.topMargin(a, 10));
        TextView r = Ui.text(a, role, 12, Ui.MUTED, false);
        r.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rl = Ui.topMargin(a, 4);
        rl.weight = 1f;
        c.addView(r, rl);
        TextView play = Ui.text(a, L.t("▶ Vidéo", "▶ Video"), 13, 0xFFFFFFFF, true);
        play.setGravity(Gravity.CENTER);
        play.setBackground(Ui.round(a, tint, 10, 0));
        play.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
        c.addView(play, Ui.topMargin(a, 8));
        c.setClickable(true);
        c.setContentDescription(name + " — " + L.t("voir la vidéo", "watch the video"));
        c.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openVideo(a, video); }
        });
        return c;
    }

    /** Opens the video in YouTube (or the browser). The app itself still has no Internet access. */
    static void openVideo(MainActivity a, String id) {
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + id))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(a, L.t("Aucune app pour ouvrir la vidéo : youtu.be/", "No app to open the video: youtu.be/") + id, Toast.LENGTH_LONG).show();
        }
    }

    private static String version(MainActivity a) {
        try {
            return a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "2.0";
        }
    }

    @Override public View view() { return root; }
    @Override public void refresh() {}
    @Override public void tick() {}
}
