package com.ankineer.app;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Set;

/** Tutorials: every video of the Ankineer channel (built-in list), search + categories; tap = open in YouTube. */
final class TutorialsPage implements MainActivity.Page {
    private final MainActivity a;
    private final View root;
    private final LinearLayout list;
    private final EditText search;
    private final TextView countTv;
    private final Button[] catBtn = new Button[Tutorials.CATEGORIES.length + 1];
    private String cat = null;

    TutorialsPage(MainActivity act) {
        a = act;
        LinearLayout col = Ui.column(a);

        // ---- channel header
        LinearLayout h = Ui.column(a);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFFFF4E45, 0xFFB16CFF});
        g.setCornerRadius(Ui.dp(a, 22));
        h.setBackground(g);
        int p = Ui.dp(a, 20);
        h.setPadding(p, p, p, p);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hl.bottomMargin = Ui.dp(a, 14);
        h.setLayoutParams(hl);
        h.addView(Ui.text(a, "▶  The Ankineer", 22, 0xFFFFFFFF, true));
        h.addView(Ui.text(a, Tutorials.VIDEOS.length + L.t(" vidéos pour maîtriser Anki : méthode, add-ons, astuces.",
                " videos to master Anki: method, add-ons, tips."), 14, 0xEEFFFFFF, false), Ui.topMargin(a, 6));
        TextView sub = Ui.text(a, "🔔  " + L.t("S'abonner à la chaîne", "Subscribe to the channel"), 14, 0xFFFF4E45, true);
        sub.setGravity(Gravity.CENTER);
        sub.setBackground(Ui.round(a, 0xFFFFFFFF, 12, 0));
        sub.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 10));
        sub.setClickable(true);
        sub.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { open(Tutorials.CHANNEL_URL + "?sub_confirmation=1"); }
        });
        h.addView(sub, Ui.topMargin(a, 14));
        col.addView(h);

        // ---- search + categories
        search = new EditText(a);
        search.setSingleLine(true);
        search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.MUTED);
        search.setTextSize(15);
        search.setHint("🔍  " + L.t("Rechercher une vidéo…", "Search a video…"));
        search.setBackground(Ui.round(a, Ui.CARD, 12, Ui.STROKE));
        int pe = Ui.dp(a, 12);
        search.setPadding(pe, pe, pe, pe);
        col.addView(search);
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(a);
        for (int i = 0; i < catBtn.length; i++) {
            final String c = i == 0 ? null : Tutorials.CATEGORIES[i - 1];
            catBtn[i] = a.chip(Tutorials.categoryName(c));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(a, 38));
            if (i > 0) lp.leftMargin = Ui.dp(a, 8);
            chips.addView(catBtn[i], lp);
            catBtn[i].setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { cat = c; render(); }
            });
        }
        hs.addView(chips);
        col.addView(hs, Ui.topMargin(a, 10));
        countTv = Ui.text(a, "", 12, Ui.MUTED, false);
        col.addView(countTv, Ui.topMargin(a, 10));
        list = Ui.column(a);
        col.addView(list, Ui.topMargin(a, 6));
        a.note(col, L.t("La liste s'enrichit à chaque mise à jour d'Ankineer Lock (l'app n'a pas accès à Internet) : "
                        + "les nouvelles vidéos sont marquées NOUVEAU et une notification te les annonce.",
                "The list grows with each Ankineer Lock update (the app has no Internet access): "
                        + "new videos are marked NEW and a notification tells you about them."));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { render(); }
        });
        root = a.scroll(col);
        render();
    }

    private void render() {
        for (int i = 0; i < catBtn.length; i++) a.styleChip(catBtn[i], i == 0 ? cat == null : Tutorials.CATEGORIES[i - 1].equals(cat));
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        Set<String> fresh = Tutorials.fresh(a);
        list.removeAllViews();
        int n = 0;
        // NEW videos first, then the rest (newest first)
        for (int pass = 0; pass < 2; pass++) {
            for (String[] v : Tutorials.VIDEOS) {
                boolean isNew = fresh.contains(v[0]);
                if ((pass == 0) != isNew) continue;
                if (cat != null && !cat.equals(v[3])) continue;
                if (q.length() > 0 && !v[1].toLowerCase(Locale.ROOT).contains(q)) continue;
                list.addView(card(v, isNew));
                n++;
            }
        }
        countTv.setText(n + (n > 1 ? L.t(" vidéos", " videos") : L.t(" vidéo", " video")));
        if (n == 0) list.addView(Ui.text(a, L.t("Aucune vidéo ne correspond.", "No matching video."), 14, Ui.MUTED, false), Ui.topMargin(a, 12));
    }

    /** Video card: coloured play tile with the duration, title, category, NEW badge. */
    private View card(final String[] v, boolean isNew) {
        int tint = Tutorials.categoryColor(v[3]);
        LinearLayout c = new LinearLayout(a);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setBackground(Ui.round(a, Ui.CARD, 16, isNew ? Ui.AMBER : Ui.STROKE));
        int p = Ui.dp(a, 12);
        c.setPadding(p, p, p, p);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cl.bottomMargin = Ui.dp(a, 10);
        c.setLayoutParams(cl);

        LinearLayout tile = Ui.column(a);
        tile.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{tint, (tint & 0x00FFFFFF) | 0x99000000});
        g.setCornerRadius(Ui.dp(a, 12));
        tile.setBackground(g);
        TextView play = Ui.text(a, "▶", 22, 0xFFFFFFFF, true);
        play.setGravity(Gravity.CENTER);
        tile.addView(play);
        TextView dur = Ui.text(a, v[2], 11, 0xFFFFFFFF, true);
        dur.setGravity(Gravity.CENTER);
        tile.addView(dur);
        c.addView(tile, new LinearLayout.LayoutParams(Ui.dp(a, 84), Ui.dp(a, 60)));

        LinearLayout tx = Ui.column(a);
        if (isNew) {
            TextView nb = Ui.text(a, L.t("NOUVEAU", "NEW"), 10, 0xFFFFFFFF, true);
            nb.setBackground(Ui.round(a, Ui.AMBER, 6, 0));
            nb.setPadding(Ui.dp(a, 6), Ui.dp(a, 1), Ui.dp(a, 6), Ui.dp(a, 1));
            tx.addView(nb, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        TextView t = Ui.text(a, v[1], 14, Ui.TEXT, true);
        t.setMaxLines(3);
        tx.addView(t);
        tx.addView(Ui.text(a, Tutorials.categoryName(v[3]), 11.5f, tint, false), Ui.topMargin(a, 2));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tl.leftMargin = Ui.dp(a, 12);
        c.addView(tx, tl);

        c.setClickable(true);
        c.setContentDescription(v[1]);
        c.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View x) {
                Tutorials.opened(a, v[0]);
                open("https://www.youtube.com/watch?v=" + v[0]);
                render();
            }
        });
        return c;
    }

    /** Opens in YouTube (or the browser). The app itself still has no Internet access. */
    private void open(String url) {
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(a, L.t("Aucune app pour ouvrir la vidéo", "No app to open the video"), Toast.LENGTH_LONG).show();
        }
    }

    @Override public View view() { return root; }
    @Override public void refresh() { render(); }
    @Override public void tick() {}
}
