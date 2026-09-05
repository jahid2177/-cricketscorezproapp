package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TournamentResultActivity extends Activity {

    ImageView btnBack;
    Button btnSaveResults, btnPdfResults;
    LinearLayout resultsContainer;
    List<String> allTeams;
    SharedPreferences tournamentPrefs, resultPrefs;
    String tournamentName = "Cricket Tournament";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_result);

        btnBack        = findViewById(R.id.btnBack);
        btnSaveResults = findViewById(R.id.btnSaveResults);
        btnPdfResults  = findViewById(R.id.btnPdfResults);
        resultsContainer = findViewById(R.id.resultsContainer);

        tournamentPrefs = getSharedPreferences("TournamentData",   MODE_PRIVATE);
        resultPrefs     = getSharedPreferences("TournamentResult", MODE_PRIVATE);

        tournamentName = tournamentPrefs.getString("TOURNAMENT_NAME", "Cricket Tournament");
        if (tournamentName.isEmpty()) tournamentName = "Cricket Tournament";

        btnBack.setOnClickListener(v -> finish());
        btnSaveResults.setOnClickListener(v -> saveResultsAndAutomateKnockouts());
        btnPdfResults.setOnClickListener(v -> generateResultsPDF());

        loadAllMatchesForResults();
    }

    // ─────────────────────────────────────────────────────────────────────
    private void loadAllMatchesForResults() {
        resultsContainer.removeAllViews();
        addTournamentHeader();
        try {
            String data = tournamentPrefs.getString("ALL_DATA", "");
            if (data.isEmpty()) { addEmptyState(); return; }

            JSONObject mainObj = new JSONObject(data);
            allTeams = getAllTeams(mainObj);
            JSONObject savedResults = new JSONObject(resultPrefs.getString("RESULT_DATA", "{}"));

            renderSection("⚽  GROUP STAGE", mainObj.optJSONArray("MatchesGroup"), "GroupMatch_", savedResults);
            renderSection("⚡  QUARTER FINALS",  mainObj.optJSONArray("MatchesQF"),    "QFMatch_",    savedResults);
            renderSection("🔥  SEMI FINALS",     mainObj.optJSONArray("MatchesSF"),    "SFMatch_",    savedResults);
            renderSection("🏆  FINAL",           mainObj.optJSONArray("MatchesFinal"), "FinalMatch_", savedResults);

            // Champion card
            if (savedResults.has("FinalMatch_0")) {
                JSONObject fRes = savedResults.getJSONObject("FinalMatch_0");
                String txt = fRes.optString("txt", "");
                String champion = "";
                if (txt.contains(" won ")) champion = txt.substring(0, txt.indexOf(" won ")).trim();
                if (!champion.isEmpty()) addChampionCard(champion);
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    // 🔥 Premium programmatic UI creation (No XML item needed)
    private void renderSection(String title, JSONArray matches, String idPrefix, JSONObject savedResults) throws Exception {
        if (matches == null || matches.length() == 0) return;

        // Section header
        LinearLayout sectionHeader = new LinearLayout(this);
        sectionHeader.setOrientation(LinearLayout.HORIZONTAL);
        sectionHeader.setGravity(Gravity.CENTER_VERTICAL);
        sectionHeader.setPadding(dp(16), dp(16), dp(16), dp(8));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(13);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#64748B")); // Slate Gray
        sectionHeader.addView(tvTitle);
        resultsContainer.addView(sectionHeader);

        // Match cards
        for (int i = 0; i < matches.length(); i++) {
            JSONObject match = matches.getJSONObject(i);
            String t1 = allTeams.get(match.getInt("team1_idx"));
            String t2 = allTeams.get(match.getInt("team2_idx"));
            final String matchId = idPrefix + i;

            // Main Card Container
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setTag(matchId); // Important for saving
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(dp(16), 0, dp(16), dp(16));
            card.setLayoutParams(cardLp);
            card.setPadding(dp(16), dp(16), dp(16), dp(16));
            
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(Color.WHITE);
            cardBg.setCornerRadius(dp(16));
            card.setBackground(cardBg);
            card.setElevation(dp(3));

            // Top Bar: Match Number & Status
            LinearLayout topBar = new LinearLayout(this);
            topBar.setOrientation(LinearLayout.HORIZONTAL);
            topBar.setGravity(Gravity.CENTER_VERTICAL);
            topBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView tvMatchNo = new TextView(this);
            tvMatchNo.setText("MATCH " + (i + 1));
            tvMatchNo.setTextSize(10);
            tvMatchNo.setTypeface(null, Typeface.BOLD);
            tvMatchNo.setTextColor(Color.WHITE);
            tvMatchNo.setPadding(dp(8), dp(4), dp(8), dp(4));
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(Color.parseColor("#3B82F6")); // Blue badge
            badgeBg.setCornerRadius(dp(8));
            tvMatchNo.setBackground(badgeBg);
            
            TextView tvStatus = new TextView(this);
            tvStatus.setText("✅ Completed");
            tvStatus.setTextSize(11);
            tvStatus.setTypeface(null, Typeface.BOLD);
            tvStatus.setTextColor(Color.parseColor("#16A34A")); // Green
            tvStatus.setGravity(Gravity.END);
            tvStatus.setVisibility(View.GONE);
            
            LinearLayout.LayoutParams spaceLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            topBar.addView(tvMatchNo);
            TextView space = new TextView(this); space.setLayoutParams(spaceLp);
            topBar.addView(space);
            topBar.addView(tvStatus);
            card.addView(topBar);

            // Teams Row
            LinearLayout teamsRow = new LinearLayout(this);
            teamsRow.setOrientation(LinearLayout.HORIZONTAL);
            teamsRow.setPadding(0, dp(12), 0, dp(12));
            teamsRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvT1 = new TextView(this);
            tvT1.setText(t1); tvT1.setTextSize(16); tvT1.setTypeface(null, Typeface.BOLD); tvT1.setTextColor(Color.parseColor("#0F172A")); tvT1.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            
            TextView tvVS = new TextView(this);
            tvVS.setText(" VS "); tvVS.setTextSize(12); tvVS.setTypeface(null, Typeface.BOLD); tvVS.setTextColor(Color.parseColor("#EF4444"));
            
            TextView tvT2 = new TextView(this);
            tvT2.setText(t2); tvT2.setTextSize(16); tvT2.setTypeface(null, Typeface.BOLD); tvT2.setTextColor(Color.parseColor("#0F172A")); tvT2.setGravity(Gravity.END); tvT2.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            
            teamsRow.addView(tvT1); teamsRow.addView(tvVS); teamsRow.addView(tvT2);
            card.addView(teamsRow);

            // Inputs Row (Scores)
            LinearLayout scoresRow = new LinearLayout(this);
            scoresRow.setOrientation(LinearLayout.HORIZONTAL);
            scoresRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            EditText etScore1 = createModernInput("Score (e.g. 150/5)"); etScore1.setTag("etS1");
            EditText etScore2 = createModernInput("Score (e.g. 145/8)"); etScore2.setTag("etS2");
            
            LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            inputLp.setMargins(0, 0, dp(8), 0);
            etScore1.setLayoutParams(inputLp);
            LinearLayout.LayoutParams inputLp2 = new LinearLayout.LayoutParams(0, dp(48), 1f);
            etScore2.setLayoutParams(inputLp2);

            scoresRow.addView(etScore1); scoresRow.addView(etScore2);
            card.addView(scoresRow);

            // Result Summary Input
            EditText etSummary = createModernInput("Result (e.g. Team A won by 5 runs)");
            etSummary.setTag("etRes");
            LinearLayout.LayoutParams sumLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
            sumLp.setMargins(0, dp(8), 0, dp(12));
            etSummary.setLayoutParams(sumLp);
            card.addView(etSummary);

            // Scoreboard Button
            Button btnScoreboard = new Button(this);
            btnScoreboard.setText("View Digital Scorecard");
            btnScoreboard.setTextColor(Color.parseColor("#10B981"));
            btnScoreboard.setTextSize(12);
            btnScoreboard.setAllCaps(false);
            btnScoreboard.setTypeface(null, Typeface.BOLD);
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(Color.parseColor("#F0FDF4"));
            btnBg.setCornerRadius(dp(12));
            btnBg.setStroke(dp(1), Color.parseColor("#10B981"));
            btnScoreboard.setBackground(btnBg);
            btnScoreboard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
            
            btnScoreboard.setOnClickListener(v -> {
                Intent intent = new Intent(TournamentResultActivity.this, ScorecardActivity.class);
                intent.putExtra("MATCH_ID", matchId);
                startActivity(intent);
            });
            card.addView(btnScoreboard);

            // Pre-fill data if saved
            if (savedResults.has(matchId)) {
                JSONObject res = savedResults.getJSONObject(matchId);
                etScore1.setText(res.optString("s1", ""));
                etScore2.setText(res.optString("s2", ""));
                etSummary.setText(res.optString("txt", ""));
                tvStatus.setVisibility(View.VISIBLE); // Show completed badge
            }

            resultsContainer.addView(card);
        }
    }

    private EditText createModernInput(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextSize(13);
        et.setPadding(dp(12), 0, dp(12), 0);
        et.setTextColor(Color.parseColor("#1E293B"));
        et.setHintTextColor(Color.parseColor("#94A3B8"));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F8FAFC"));
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        et.setBackground(bg);
        return et;
    }

    private void addTournamentHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#0F172A"), Color.parseColor("#1E293B")});
        bg.setCornerRadius(dp(16));
        header.setBackground(bg);
        header.setPadding(dp(20), dp(24), dp(20), dp(24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(16), dp(16), dp(8));
        header.setLayoutParams(lp);

        String logoPath = ImageStorageHelper.getTournamentLogoPath(this);
        if (logoPath != null) {
            ImageView ivLogo = new ImageView(this);
            LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(dp(56), dp(56));
            imgLp.setMargins(0, 0, 0, dp(8));
            ivLogo.setLayoutParams(imgLp);
            ImageStorageHelper.loadTournamentLogoInto(this, ivLogo, 0);
            header.addView(ivLogo);
        } else {
            TextView tvIcon = new TextView(this);
            tvIcon.setText("🏆");
            tvIcon.setTextSize(32);
            tvIcon.setGravity(Gravity.CENTER);
            header.addView(tvIcon);
        }

        TextView tvName = new TextView(this);
        tvName.setText(tournamentName);
        tvName.setTextSize(20);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.WHITE);
        tvName.setGravity(Gravity.CENTER);
        header.addView(tvName);

        resultsContainer.addView(header);
    }

    private void addChampionCard(String champion) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor("#F59E0B"), Color.parseColor("#D97706")});
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);
        card.setPadding(dp(20), dp(24), dp(20), dp(24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(16), dp(16), dp(32));
        card.setLayoutParams(lp);

        TextView icon = new TextView(this); icon.setText("👑"); icon.setTextSize(40); icon.setGravity(Gravity.CENTER);
        card.addView(icon);

        TextView tvLabel = new TextView(this);
        tvLabel.setText("CHAMPION");
        tvLabel.setTextSize(12); tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setTextColor(Color.parseColor("#FEF3C7")); tvLabel.setGravity(Gravity.CENTER);
        tvLabel.setLetterSpacing(0.1f);
        card.addView(tvLabel);

        TextView tvName = new TextView(this);
        tvName.setText(champion);
        tvName.setTextSize(26); tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.WHITE); tvName.setGravity(Gravity.CENTER);
        card.addView(tvName);
        resultsContainer.addView(card);
    }

    private void addEmptyState() {
        TextView tv = new TextView(this);
        tv.setText("📭  No tournament data found.\nSet up fixtures first.");
        tv.setTextColor(Color.parseColor("#94A3B8")); tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER); tv.setPadding(0, dp(100), 0, dp(60));
        resultsContainer.addView(tv);
    }

    // ─────────────────────────────────────────────────────────────────────
    private void saveResultsAndAutomateKnockouts() {
        try {
            JSONObject resultObj = new JSONObject();
            for (int i = 0; i < resultsContainer.getChildCount(); i++) {
                View child = resultsContainer.getChildAt(i);
                if (child.getTag() == null) continue;
                String matchId = child.getTag().toString();
                
                // Get elements using Tag instead of R.id
                EditText etS1  = child.findViewWithTag("etS1");
                EditText etS2  = child.findViewWithTag("etS2");
                EditText etRes = child.findViewWithTag("etRes");
                
                if (etS1 == null) continue;
                
                if (!etS1.getText().toString().isEmpty() || !etS2.getText().toString().isEmpty()) {
                    JSONObject d = new JSONObject();
                    d.put("s1",  etS1.getText().toString());
                    d.put("s2",  etS2.getText().toString());
                    d.put("txt", etRes.getText().toString());
                    resultObj.put(matchId, d);
                }
            }
            resultPrefs.edit().putString("RESULT_DATA", resultObj.toString()).apply();
            automateKnockouts(resultObj);
            Toast.makeText(this, "✅ Results Saved & Fixtures Updated!", Toast.LENGTH_SHORT).show();
            loadAllMatchesForResults();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // (Knockout Automation & PDF Generation code remains unchanged)
    private void automateKnockouts(JSONObject resultData) throws Exception {
        String data = tournamentPrefs.getString("ALL_DATA", "");
        if (data.isEmpty()) return;
        JSONObject mainObj = new JSONObject(data);

        if (mainObj.has("DynamicGroups")) {
            JSONArray groups = mainObj.getJSONArray("DynamicGroups");
            List<String> topTeams = new ArrayList<>();
            for (int g = 0; g < groups.length(); g++) {
                JSONArray teamsArr = groups.getJSONObject(g).getJSONArray("teams");
                List<TeamStat> groupStats = new ArrayList<>();
                for (int t = 0; t < teamsArr.length(); t++) groupStats.add(new TeamStat(teamsArr.getString(t)));
                JSONArray groupMatches = mainObj.optJSONArray("MatchesGroup");
                if (groupMatches != null) {
                    for (int i = 0; i < groupMatches.length(); i++) {
                        String matchId = "GroupMatch_" + i;
                        if (!resultData.has(matchId)) continue;
                        int idx1 = groupMatches.getJSONObject(i).getInt("team1_idx");
                        int idx2 = groupMatches.getJSONObject(i).getInt("team2_idx");
                        int s1 = getRuns(resultData.getJSONObject(matchId).optString("s1", "0"));
                        int s2 = getRuns(resultData.getJSONObject(matchId).optString("s2", "0"));
                        for (TeamStat ts : groupStats) {
                            if (ts.name.equals(allTeams.get(idx1))) { if (s1>s2) ts.pts+=2; else if (s1==s2) ts.pts+=1; }
                            if (ts.name.equals(allTeams.get(idx2))) { if (s2>s1) ts.pts+=2; else if (s1==s2) ts.pts+=1; }
                        }
                    }
                }
                Collections.sort(groupStats);
                if (groupStats.size() > 0) topTeams.add(groupStats.get(0).name);
                if (groupStats.size() > 1) topTeams.add(groupStats.get(1).name);
            }
            JSONArray existingQF = mainObj.optJSONArray("MatchesQF");
            if (topTeams.size() >= 4 && (existingQF == null || existingQF.length() == 0)) {
                JSONArray qfArr = new JSONArray();
                for (int i = 0; i < topTeams.size() - 1; i += 2) {
                    JSONObject m = new JSONObject();
                    m.put("team1_idx", allTeams.indexOf(topTeams.get(i)));
                    m.put("team2_idx", allTeams.indexOf(topTeams.get(i + 1)));
                    m.put("date", "TBD"); qfArr.put(m);
                }
                mainObj.put("MatchesQF", qfArr);
            }
        }

        JSONArray qfArr = mainObj.optJSONArray("MatchesQF");
        if (qfArr != null && qfArr.length() > 0) {
            List<String> sfTeams = getWinners(qfArr, "QFMatch_", resultData);
            if (sfTeams.size() >= 2) {
                JSONArray sfArray = new JSONArray();
                for (int i = 0; i < sfTeams.size() - 1; i += 2) {
                    JSONObject m = new JSONObject();
                    m.put("team1_idx", allTeams.indexOf(sfTeams.get(i)));
                    m.put("team2_idx", allTeams.indexOf(sfTeams.get(i + 1)));
                    m.put("date", "TBD"); sfArray.put(m);
                }
                mainObj.put("MatchesSF", sfArray);
            }
        }

        JSONArray sfArr = mainObj.optJSONArray("MatchesSF");
        if (sfArr != null && sfArr.length() > 0) {
            List<String> finalTeams = getWinners(sfArr, "SFMatch_", resultData);
            if (finalTeams.size() >= 2) {
                JSONArray finalArray = new JSONArray();
                JSONObject m = new JSONObject();
                m.put("team1_idx", allTeams.indexOf(finalTeams.get(0)));
                m.put("team2_idx", allTeams.indexOf(finalTeams.get(1)));
                m.put("date", "TBD"); finalArray.put(m);
                mainObj.put("MatchesFinal", finalArray);
            }
        }
        tournamentPrefs.edit().putString("ALL_DATA", mainObj.toString()).apply();
    }

    private List<String> getWinners(JSONArray matches, String prefix, JSONObject results) throws Exception {
        List<String> winners = new ArrayList<>();
        for (int i = 0; i < matches.length(); i++) {
            String matchId = prefix + i;
            if (!results.has(matchId)) continue;
            String txt = results.getJSONObject(matchId).optString("txt", "");
            int s1 = getRuns(results.getJSONObject(matchId).optString("s1", "0"));
            int s2 = getRuns(results.getJSONObject(matchId).optString("s2", "0"));
            int t1Idx = matches.getJSONObject(i).getInt("team1_idx");
            int t2Idx = matches.getJSONObject(i).getInt("team2_idx");
            if (txt.contains(" won ")) {
                String winner = txt.substring(0, txt.indexOf(" won ")).trim();
                winners.add(winner);
            } else {
                winners.add(s1 >= s2 ? allTeams.get(t1Idx) : allTeams.get(t2Idx));
            }
        }
        return winners;
    }

    private int getRuns(String scoreStr) {
        try { return Integer.parseInt(scoreStr.split("/")[0].replaceAll("[^0-9]", "")); }
        catch (Exception e) { return 0; }
    }

    private List<String> getAllTeams(JSONObject mainObj) throws Exception {
        List<String> teams = new ArrayList<>();
        teams.add("Select Team");
        if (mainObj.has("DynamicGroups")) {
            JSONArray groups = mainObj.getJSONArray("DynamicGroups");
            for (int i = 0; i < groups.length(); i++) {
                JSONArray tArr = groups.getJSONObject(i).getJSONArray("teams");
                for (int j = 0; j < tArr.length(); j++) teams.add(tArr.getString(j));
            }
        }
        return teams;
    }

    private void generateResultsPDF() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint(); paint.setAntiAlias(true);

        paint.setColor(Color.parseColor("#1B5E20"));
        canvas.drawRect(0, 0, 595, 80, paint);
        paint.setColor(Color.WHITE); paint.setTextSize(22);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(tournamentName, 297, 42, paint);
        paint.setTextSize(11); paint.setColor(Color.parseColor("#A5D6A7"));
        canvas.drawText("MATCH SCOREBOARD", 297, 64, paint);

        int y = 100;
        try {
            String data = tournamentPrefs.getString("ALL_DATA", "");
            if (!data.isEmpty()) {
                JSONObject mainObj = new JSONObject(data);
                List<String> teams = getAllTeams(mainObj);
                JSONObject savedResults = new JSONObject(resultPrefs.getString("RESULT_DATA", "{}"));
                y = drawResultSection(canvas, paint, "GROUP STAGE",   mainObj.optJSONArray("MatchesGroup"), "GroupMatch_", teams, savedResults, y);
                y = drawResultSection(canvas, paint, "QUARTER FINALS",mainObj.optJSONArray("MatchesQF"),   "QFMatch_",   teams, savedResults, y);
                y = drawResultSection(canvas, paint, "SEMI FINALS",   mainObj.optJSONArray("MatchesSF"),   "SFMatch_",   teams, savedResults, y);
                y = drawResultSection(canvas, paint, "🏆 FINAL",      mainObj.optJSONArray("MatchesFinal"),"FinalMatch_",teams, savedResults, y);
            }
        } catch (Exception e) { e.printStackTrace(); }

        paint.setColor(Color.parseColor("#90A4AE")); paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Generated by Cricket ScoreZ Pro", 297, 820, paint);
        document.finishPage(page);

        String safeName = tournamentName.replaceAll("\\s+", "_");
        String fileName = safeName + "_Results_" + System.currentTimeMillis() + ".pdf";
        PdfManager.saveAndOpenPdf(this, document, fileName, "Tournament Results Downloaded \u2705");
    }

    private int drawResultSection(Canvas c, Paint p, String title, JSONArray matches, String prefix, List<String> teams, JSONObject results, int startY) throws Exception {
        p.setColor(Color.parseColor("#2E7D32")); c.drawRect(20, startY, 575, startY + 28, p);
        p.setColor(Color.WHITE); p.setTextSize(12); p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextAlign(Paint.Align.LEFT); c.drawText("  " + title, 25, startY + 19, p); startY += 36;
        if (matches == null || matches.length() == 0) return startY + 10;
        boolean alt = false;
        for (int i = 0; i < matches.length(); i++) {
            JSONObject match = matches.getJSONObject(i);
            String t1 = teams.get(match.getInt("team1_idx")), t2 = teams.get(match.getInt("team2_idx"));
            String matchId = prefix + i;
            p.setColor(alt ? Color.parseColor("#F1F8E9") : Color.WHITE);
            c.drawRect(20, startY - 14, 575, startY + 14, p); alt = !alt;
            p.setColor(Color.parseColor("#1A237E")); p.setTextSize(12); p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); p.setTextAlign(Paint.Align.LEFT);
            c.drawText(t1, 30, startY, p);
            p.setColor(Color.parseColor("#C62828")); p.setTextSize(10); p.setTextAlign(Paint.Align.CENTER); c.drawText("VS", 297, startY, p);
            p.setColor(Color.parseColor("#1A237E")); p.setTextSize(12); p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); p.setTextAlign(Paint.Align.RIGHT); c.drawText(t2, 565, startY, p);
            if (results.has(matchId)) {
                JSONObject res = results.getJSONObject(matchId);
                p.setColor(Color.parseColor("#2E7D32")); p.setTextSize(11); p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)); p.setTextAlign(Paint.Align.LEFT); c.drawText(res.optString("s1","-"), 30, startY + 14, p);
                p.setTextAlign(Paint.Align.RIGHT); c.drawText(res.optString("s2","-"), 565, startY + 14, p);
                String txt = res.optString("txt","");
                if (!txt.isEmpty()) { p.setColor(Color.parseColor("#546E7A")); p.setTextSize(9); p.setTextAlign(Paint.Align.CENTER); c.drawText(txt, 297, startY + 14, p); }
                startY += 14;
            }
            p.setColor(Color.parseColor("#E0E0E0")); p.setStrokeWidth(1); c.drawLine(20, startY + 16, 575, startY + 16, p); startY += 30;
        }
        return startY + 10;
    }

    private int dp(int val) { return Math.round(val * getResources().getDisplayMetrics().density); }

    class TeamStat implements Comparable<TeamStat> {
        String name; int pts = 0;
        TeamStat(String n) { name = n; }
        @Override public int compareTo(TeamStat o) { return o.pts - this.pts; }
    }
}
