package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RankingActivity extends Activity {

    ImageView btnBack;
    LinearLayout containerTopBatsmen, containerTopBowlers;
    SharedPreferences rankingPrefs;
    String tournamentName = "Tournament";

    // Tab state
    boolean showingBatsmen = true;
    LinearLayout tabBat, tabBowl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        btnBack            = findViewById(R.id.btnBack);
        containerTopBatsmen = findViewById(R.id.containerTopBatsmen);
        containerTopBowlers = findViewById(R.id.containerTopBowlers);
        tabBat             = findViewById(R.id.tabBat);
        tabBowl            = findViewById(R.id.tabBowl);

        rankingPrefs = getSharedPreferences("TournamentRankings", MODE_PRIVATE);
        SharedPreferences settingsPrefs = getSharedPreferences("TournamentData", MODE_PRIVATE);
        tournamentName = settingsPrefs.getString("TOURNAMENT_NAME", "Tournament");
        if (tournamentName.isEmpty()) tournamentName = "Tournament";

        btnBack.setOnClickListener(v -> finish());

        tabBat.setOnClickListener(v -> {
            showingBatsmen = true;
            updateTabUI();
            loadRankings();
        });
        tabBowl.setOnClickListener(v -> {
            showingBatsmen = false;
            updateTabUI();
            loadRankings();
        });

        updateTabUI();
        loadRankings();
    }

    private void updateTabUI() {
        GradientDrawable activeBg = new GradientDrawable();
        activeBg.setColor(Color.parseColor("#1B5E20"));
        activeBg.setCornerRadius(dp(24));

        GradientDrawable inactiveBg = new GradientDrawable();
        inactiveBg.setColor(Color.parseColor("#E8F5E9"));
        inactiveBg.setCornerRadius(dp(24));

        if (showingBatsmen) {
            tabBat.setBackground(activeBg);
            ((TextView) tabBat.findViewWithTag("label")).setTextColor(Color.WHITE);
            tabBowl.setBackground(inactiveBg);
            ((TextView) tabBowl.findViewWithTag("label")).setTextColor(Color.parseColor("#1B5E20"));
            containerTopBatsmen.setVisibility(View.VISIBLE);
            containerTopBowlers.setVisibility(View.GONE);
        } else {
            tabBowl.setBackground(activeBg);
            ((TextView) tabBowl.findViewWithTag("label")).setTextColor(Color.WHITE);
            tabBat.setBackground(inactiveBg);
            ((TextView) tabBat.findViewWithTag("label")).setTextColor(Color.parseColor("#1B5E20"));
            containerTopBatsmen.setVisibility(View.GONE);
            containerTopBowlers.setVisibility(View.VISIBLE);
        }
    }

    private void loadRankings() {
        containerTopBatsmen.removeAllViews();
        containerTopBowlers.removeAllViews();

        try {
            String batData  = rankingPrefs.getString("BATSMAN_STATS", "{}");
            String bowlData = rankingPrefs.getString("BOWLER_STATS",  "{}");
            JSONObject batObj  = new JSONObject(batData);
            JSONObject bowlObj = new JSONObject(bowlData);

            // ── Batsmen ──────────────────────────────────────────
            List<BatStat> batList = new ArrayList<>();
            Iterator<String> batKeys = batObj.keys();
            while (batKeys.hasNext()) {
                String name = batKeys.next();
                JSONObject p = batObj.optJSONObject(name);
                if (p != null) {
                    batList.add(new BatStat(name,
                        p.optInt("runs", 0), p.optInt("balls", 0),
                        p.optInt("fours", 0), p.optInt("sixes", 0), p.optInt("inns", 1)));
                } else {
                    // legacy: stored as plain int
                    batList.add(new BatStat(name, batObj.optInt(name, 0), 0, 0, 0, 1));
                }
            }
            Collections.sort(batList, (a, b) -> b.runs - a.runs);

            // ── Bowlers ──────────────────────────────────────────
            List<BowlStat> bowlList = new ArrayList<>();
            Iterator<String> bowlKeys = bowlObj.keys();
            while (bowlKeys.hasNext()) {
                String name = bowlKeys.next();
                JSONObject b = bowlObj.optJSONObject(name);
                if (b != null) {
                    bowlList.add(new BowlStat(name,
                        b.optInt("wickets", b.optInt("w", 0)),
                        b.optInt("runs",    b.optInt("r", 0)),
                        b.optInt("inns", 1)));
                } else {
                    bowlList.add(new BowlStat(name, bowlObj.optInt(name, 0), 0, 1));
                }
            }
            Collections.sort(bowlList, (a, b2) -> b2.wickets != a.wickets
                    ? b2.wickets - a.wickets : a.runs - b2.runs);

            displayBatsmen(batList);
            displayBowlers(bowlList);

        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Display: Batsmen ─────────────────────────────────────────────────
    private void displayBatsmen(List<BatStat> list) {
        if (list.isEmpty()) { addEmptyState(containerTopBatsmen, "No batting data yet"); return; }

        // Header row
        addTableHeader(containerTopBatsmen, new String[]{"#", "Player", "Inn", "Runs", "Balls", "4s", "6s", "SR"});

        for (int i = 0; i < Math.min(list.size(), 15); i++) {
            BatStat p = list.get(i);
            double sr = p.balls > 0 ? (p.runs * 100.0 / p.balls) : 0.0;

            String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : String.valueOf(i + 1);
            int rowBg = i % 2 == 0 ? Color.WHITE : Color.parseColor("#F8FFF8");
            boolean isTop = i == 0;

            LinearLayout row = makeRow(rowBg, isTop);

            addCell(row, medal,                    40,  isTop ? Color.parseColor("#B8860B") : Color.parseColor("#94A3B8"), true,  Gravity.CENTER);
            addCell(row, p.name,                    0,  isTop ? Color.parseColor("#1B5E20") : Color.parseColor("#1E293B"), true,  Gravity.START, 1f);
            addCell(row, String.valueOf(p.inns),   38,  Color.parseColor("#64748B"),                                       false, Gravity.CENTER);
            addCell(row, String.valueOf(p.runs),   48,  isTop ? Color.parseColor("#C62828") : Color.parseColor("#1B5E20"), true,  Gravity.CENTER);
            addCell(row, String.valueOf(p.balls),  48,  Color.parseColor("#64748B"),                                       false, Gravity.CENTER);
            addCell(row, String.valueOf(p.fours),  38,  Color.parseColor("#1565C0"),                                       false, Gravity.CENTER);
            addCell(row, String.valueOf(p.sixes),  38,  Color.parseColor("#6A1B9A"),                                       false, Gravity.CENTER);
            addCell(row, String.format("%.1f", sr), 52, Color.parseColor("#E65100"),                                       false, Gravity.CENTER);

            containerTopBatsmen.addView(row);
            addDivider(containerTopBatsmen);
        }
    }

    // ── Display: Bowlers ─────────────────────────────────────────────────
    private void displayBowlers(List<BowlStat> list) {
        if (list.isEmpty()) { addEmptyState(containerTopBowlers, "No bowling data yet"); return; }

        addTableHeader(containerTopBowlers, new String[]{"#", "Player", "Inn", "Wkts", "Runs", "Avg", "Eco"});

        for (int i = 0; i < Math.min(list.size(), 15); i++) {
            BowlStat b = list.get(i);
            double avg = b.wickets > 0 ? (b.runs * 1.0 / b.wickets) : b.runs;
            double eco = b.inns    > 0 ? (b.runs * 1.0 / b.inns)    : 0.0;

            String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : String.valueOf(i + 1);
            int rowBg = i % 2 == 0 ? Color.WHITE : Color.parseColor("#F0F8FF");
            boolean isTop = i == 0;

            LinearLayout row = makeRow(rowBg, isTop);

            addCell(row, medal,                       40,  isTop ? Color.parseColor("#B8860B") : Color.parseColor("#94A3B8"), true,  Gravity.CENTER);
            addCell(row, b.name,                       0,  isTop ? Color.parseColor("#1565C0") : Color.parseColor("#1E293B"), true,  Gravity.START, 1f);
            addCell(row, String.valueOf(b.inns),      38,  Color.parseColor("#64748B"),                                       false, Gravity.CENTER);
            addCell(row, String.valueOf(b.wickets),   48,  isTop ? Color.parseColor("#C62828") : Color.parseColor("#1565C0"), true,  Gravity.CENTER);
            addCell(row, String.valueOf(b.runs),      48,  Color.parseColor("#64748B"),                                       false, Gravity.CENTER);
            addCell(row, String.format("%.1f", avg),  52,  Color.parseColor("#2E7D32"),                                       false, Gravity.CENTER);
            addCell(row, String.format("%.2f", eco),  52,  Color.parseColor("#E65100"),                                       false, Gravity.CENTER);

            containerTopBowlers.addView(row);
            addDivider(containerTopBowlers);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private void addTableHeader(LinearLayout container, String[] cols) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColor(Color.parseColor("#1B5E20"));
        hBg.setCornerRadii(new float[]{dp(12),dp(12),dp(12),dp(12),0,0,0,0});
        header.setBackground(hBg);

        int[] widths = {40, 0, 38, 48, 48, 38, 38, 52};
        float[] weights = {0, 1f, 0, 0, 0, 0, 0, 0};
        for (int i = 0; i < cols.length; i++) {
            TextView tv = new TextView(this);
            tv.setText(cols[i]);
            tv.setTextColor(Color.parseColor("#A5D6A7"));
            tv.setTextSize(11);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setGravity(i == 1 ? Gravity.START : Gravity.CENTER);
            if (i == 1) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tv.setLayoutParams(lp);
            } else {
                int w = i < widths.length ? widths[i] : 48;
                tv.setLayoutParams(new LinearLayout.LayoutParams(dp(w), LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            header.addView(tv);
        }
        container.addView(header);
    }

    private LinearLayout makeRow(int bgColor, boolean isTop) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(isTop ? 14 : 10), dp(12), dp(isTop ? 14 : 10));
        row.setBackgroundColor(bgColor);
        return row;
    }

    private void addCell(LinearLayout row, String text, int widthDp, int color, boolean bold, int gravity) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(13);
        tv.setGravity(gravity);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        if (widthDp == 0) {
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        } else {
            tv.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp), LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        row.addView(tv);
    }

    private void addCell(LinearLayout row, String text, int widthDp, int color, boolean bold, int gravity, float weight) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(13);
        tv.setGravity(gravity);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        row.addView(tv);
    }

    private void addDivider(LinearLayout container) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        v.setBackgroundColor(Color.parseColor("#E8F5E9"));
        container.addView(v);
    }

    private void addEmptyState(LinearLayout container, String msg) {
        TextView tv = new TextView(this);
        tv.setText("📭  " + msg);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(32), 0, dp(32));
        container.addView(tv);
    }

    private int dp(int val) { return Math.round(val * getResources().getDisplayMetrics().density); }

    // ── Data models ──────────────────────────────────────────────────────
    static class BatStat {
        String name; int runs, balls, fours, sixes, inns;
        BatStat(String n, int r, int b, int f4, int s6, int i) {
            name=n; runs=r; balls=b; fours=f4; sixes=s6; inns=i;
        }
    }
    static class BowlStat {
        String name; int wickets, runs, inns;
        BowlStat(String n, int w, int r, int i) { name=n; wickets=w; runs=r; inns=i; }
    }
}
