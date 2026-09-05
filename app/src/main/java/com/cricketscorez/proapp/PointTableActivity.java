package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PointTableActivity extends Activity {

    ImageView btnBack;
    Button btnPdf, btnEditManual;
    LinearLayout mainContainer;
    SharedPreferences tourPrefs, resultPrefs;
    String tournamentName = "Tournament";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_table);

        btnBack        = findViewById(R.id.btnBack);
        btnPdf         = findViewById(R.id.btnPdf);
        btnEditManual  = findViewById(R.id.btnEditManual);
        mainContainer  = findViewById(R.id.mainContainer);

        tourPrefs   = getSharedPreferences("TournamentData",   MODE_PRIVATE);
        resultPrefs = getSharedPreferences("TournamentResult", MODE_PRIVATE);
        tournamentName = tourPrefs.getString("TOURNAMENT_NAME", "Tournament");
        if (tournamentName.isEmpty()) tournamentName = "Tournament";

        btnBack.setOnClickListener(v -> finish());
        btnPdf.setOnClickListener(v -> generatePDF());
        btnEditManual.setOnClickListener(v -> showManualEditDialog());

        loadPointTable();
    }

    @Override
    protected void onResume() { super.onResume(); loadPointTable(); }

    // ─────────────────────────────────────────────────────────────────────
    private void loadPointTable() {
        mainContainer.removeAllViews();
        addHeader();

        try {
            String allData = tourPrefs.getString("ALL_DATA", "");
            if (allData.isEmpty()) { addEmptyState(); return; }

            JSONObject mainObj  = new JSONObject(allData);
            JSONObject results  = new JSONObject(resultPrefs.getString("RESULT_DATA", "{}"));
            JSONObject ptSaved  = new JSONObject(tourPrefs.getString("POINT_TABLE", "{}"));
            JSONObject manualPT = new JSONObject(tourPrefs.getString("MANUAL_PT", "{}"));

            JSONArray dynGroups = mainObj.optJSONArray("DynamicGroups");
            if (dynGroups == null || dynGroups.length() == 0) { addEmptyState(); return; }

            int winPts = 2, tiePts = 1;
            try { winPts = Integer.parseInt(tourPrefs.getString("WIN_POINTS", "2")); } catch (Exception ignored) {}
            try { tiePts = Integer.parseInt(tourPrefs.getString("TIE_POINTS", "1")); } catch (Exception ignored) {}

            JSONArray groupMatches = mainObj.optJSONArray("MatchesGroup");
            List<String> allTeams = getAllTeams(mainObj);

            boolean anyGroup = false;
            for (int g = 0; g < dynGroups.length(); g++) {
                JSONObject grpObj = dynGroups.getJSONObject(g);
                String gName = grpObj.optString("name", "Group " + (char)('A' + g));
                JSONArray tArr = grpObj.getJSONArray("teams");

                List<TeamRow> rows = new ArrayList<>();
                for (int t = 0; t < tArr.length(); t++) {
                    rows.add(new TeamRow(tArr.getString(t)));
                }

                // Calculate from match results
                if (groupMatches != null) {
                    for (int i = 0; i < groupMatches.length(); i++) {
                        String mid = "GroupMatch_" + i;
                        if (!results.has(mid)) continue;
                        int t1Idx = groupMatches.getJSONObject(i).getInt("team1_idx");
                        int t2Idx = groupMatches.getJSONObject(i).getInt("team2_idx");
                        if (t1Idx >= allTeams.size() || t2Idx >= allTeams.size()) continue;
                        String t1 = allTeams.get(t1Idx), t2 = allTeams.get(t2Idx);
                        JSONObject res = results.getJSONObject(mid);
                        int s1 = getRuns(res.optString("s1","0"));
                        int s2 = getRuns(res.optString("s2","0"));

                        TeamRow r1 = findRow(rows, t1), r2 = findRow(rows, t2);
                        if (r1 == null || r2 == null) continue;
                        r1.played++; r2.played++;
                        if (s1 > s2) { r1.won++; r2.lost++; r1.pts += winPts; }
                        else if (s2 > s1) { r2.won++; r1.lost++; r2.pts += winPts; }
                        else { r1.tied++; r2.tied++; r1.pts += tiePts; r2.pts += tiePts; }

                        double overs = getOversValue(tourPrefs.getString("TOTAL_OVERS", "20"));
                        if (overs > 0) {
                            r1.runsFor += s1; r1.runsAgainst += s2;
                            r2.runsFor += s2; r2.runsAgainst += s1;
                            r1.oversFor += overs; r1.oversAgainst += overs;
                            r2.oversFor += overs; r2.oversAgainst += overs;
                        }
                    }
                }

                // Merge auto-saved
                for (TeamRow r : rows) {
                    if (ptSaved.has(r.name) && r.played == 0) {
                        JSONObject saved = ptSaved.getJSONObject(r.name);
                        r.pts = saved.optInt("pts", 0);
                        r.played = saved.optInt("played", 0);
                        r.won = saved.optInt("won", 0);
                        r.lost = r.played - r.won;
                    }
                }

                // Merge manual overrides
                for (TeamRow r : rows) {
                    String key = gName + "__" + r.name;
                    if (manualPT.has(key)) {
                        JSONObject m = manualPT.getJSONObject(key);
                        r.played  = m.optInt("played",  r.played);
                        r.won     = m.optInt("won",     r.won);
                        r.lost    = m.optInt("lost",    r.lost);
                        r.tied    = m.optInt("tied",    r.tied);
                        r.nr      = m.optInt("nr",      r.nr);
                        r.pts     = m.optInt("pts",     r.pts);
                        r.nrrManual = m.optDouble("nrr", Double.MAX_VALUE);
                    }
                }

                Collections.sort(rows);
                renderGroupTable(gName, rows);
                anyGroup = true;
            }

            if (!anyGroup) addEmptyState();

        } catch (Exception e) {
            e.printStackTrace();
            addEmptyState();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // MANUAL EDIT DIALOG
    // ─────────────────────────────────────────────────────────────────────
    private void showManualEditDialog() {
        try {
            String allData = tourPrefs.getString("ALL_DATA", "");
            if (allData.isEmpty()) { Toast.makeText(this, "No groups found", Toast.LENGTH_SHORT).show(); return; }
            JSONObject mainObj = new JSONObject(allData);
            JSONArray dynGroups = mainObj.optJSONArray("DynamicGroups");
            if (dynGroups == null || dynGroups.length() == 0) {
                Toast.makeText(this, "No groups found. Add groups first.", Toast.LENGTH_SHORT).show(); return;
            }
            showGroupPickerForManual(dynGroups);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showGroupPickerForManual(JSONArray dynGroups) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.WHITE); rootBg.setCornerRadius(dp(18));
        root.setBackground(rootBg);

        // Header
        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.VERTICAL); hdr.setGravity(Gravity.CENTER);
        GradientDrawable hdrBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#388E3C")});
        hdrBg.setCornerRadii(new float[]{dp(18),dp(18),dp(18),dp(18),0,0,0,0});
        hdr.setBackground(hdrBg); hdr.setPadding(dp(20), dp(18), dp(20), dp(18));
        TextView tvH = new TextView(this); tvH.setText("✏️  Manual Point Entry");
        tvH.setTextSize(17); tvH.setTypeface(null, Typeface.BOLD);
        tvH.setTextColor(Color.WHITE); tvH.setGravity(Gravity.CENTER);
        TextView tvHSub = new TextView(this); tvHSub.setText("Select a group to edit");
        tvHSub.setTextSize(11); tvHSub.setTextColor(Color.parseColor("#A5D6A7"));
        tvHSub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0, dp(4), 0, 0); tvHSub.setLayoutParams(subLp);
        hdr.addView(tvH); hdr.addView(tvHSub);
        root.addView(hdr);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(14), dp(16), dp(16));

        try {
            for (int g = 0; g < dynGroups.length(); g++) {
                final int gIdx = g;
                JSONObject grp = dynGroups.getJSONObject(g);
                final String gName = grp.optString("name", "Group " + (char)('A' + g));
                JSONArray tArr = grp.getJSONArray("teams");

                LinearLayout grpBtn = new LinearLayout(this);
                grpBtn.setOrientation(LinearLayout.HORIZONTAL);
                grpBtn.setGravity(Gravity.CENTER_VERTICAL);
                grpBtn.setClickable(true); grpBtn.setFocusable(true);
                GradientDrawable grpBtnBg = new GradientDrawable();
                grpBtnBg.setColor(Color.parseColor("#F0FDF4"));
                grpBtnBg.setStroke(dp(1), Color.parseColor("#BBF7D0"));
                grpBtnBg.setCornerRadius(dp(12)); grpBtn.setBackground(grpBtnBg);
                grpBtn.setPadding(dp(14), dp(14), dp(14), dp(14));
                LinearLayout.LayoutParams grpBtnLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                grpBtnLp.setMargins(0, 0, 0, dp(10)); grpBtn.setLayoutParams(grpBtnLp);

                GradientDrawable grpIcoBg = new GradientDrawable();
                grpIcoBg.setColor(Color.parseColor("#1B5E20")); grpIcoBg.setCornerRadius(dp(8));
                TextView tvGIco = new TextView(this); tvGIco.setText("📋");
                tvGIco.setTextSize(16); tvGIco.setPadding(dp(6), dp(4), dp(6), dp(4));
                tvGIco.setBackground(grpIcoBg);
                LinearLayout.LayoutParams icoLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                icoLp.setMargins(0, 0, dp(12), 0); tvGIco.setLayoutParams(icoLp);
                grpBtn.addView(tvGIco);

                LinearLayout grpTextCol = new LinearLayout(this);
                grpTextCol.setOrientation(LinearLayout.VERTICAL);
                grpTextCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                TextView tvGN = new TextView(this); tvGN.setText(gName);
                tvGN.setTextSize(14); tvGN.setTypeface(null, Typeface.BOLD);
                tvGN.setTextColor(Color.parseColor("#1B5E20"));
                TextView tvGSub = new TextView(this); tvGSub.setText(tArr.length() + " teams");
                tvGSub.setTextSize(11); tvGSub.setTextColor(Color.parseColor("#64748B"));
                grpTextCol.addView(tvGN); grpTextCol.addView(tvGSub);
                grpBtn.addView(grpTextCol);

                TextView tvArrow = new TextView(this); tvArrow.setText("›");
                tvArrow.setTextSize(22); tvArrow.setTypeface(null, Typeface.BOLD);
                tvArrow.setTextColor(Color.parseColor("#4CAF50"));
                grpBtn.addView(tvArrow);

                grpBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    showManualEntryForGroup(gName, grp);
                });
                body.addView(grpBtn);
            }
        } catch (Exception e) { e.printStackTrace(); }

        Button btnClose = new Button(this);
        btnClose.setText("Cancel"); btnClose.setAllCaps(false);
        btnClose.setTextColor(Color.parseColor("#64748B"));
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setColor(Color.parseColor("#F1F5F9")); closeBg.setCornerRadius(dp(10));
        btnClose.setBackground(closeBg);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        closeLp.setMargins(dp(16), 0, dp(16), dp(16)); btnClose.setLayoutParams(closeLp);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        root.addView(body);
        root.addView(btnClose);

        ScrollView sv = new ScrollView(this); sv.addView(root);
        dialog.setContentView(sv);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.90f),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void showManualEntryForGroup(String groupName, JSONObject grpObj) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.parseColor("#F8FAFC")); rootBg.setCornerRadius(dp(18));
        root.setBackground(rootBg);

        // Header
        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.VERTICAL); hdr.setGravity(Gravity.CENTER);
        GradientDrawable hdrBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#388E3C")});
        hdrBg.setCornerRadii(new float[]{dp(18),dp(18),dp(18),dp(18),0,0,0,0});
        hdr.setBackground(hdrBg); hdr.setPadding(dp(20), dp(16), dp(20), dp(16));
        TextView tvH = new TextView(this); tvH.setText("✏️  " + groupName + " — Manual Entry");
        tvH.setTextSize(15); tvH.setTypeface(null, Typeface.BOLD);
        tvH.setTextColor(Color.WHITE); tvH.setGravity(Gravity.CENTER);
        TextView tvHSub = new TextView(this); tvHSub.setText("M · W · L · T · N/R · PTS · NRR");
        tvHSub.setTextSize(10); tvHSub.setTextColor(Color.parseColor("#A5D6A7")); tvHSub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.setMargins(0, dp(4), 0, 0); tvHSub.setLayoutParams(sLp);
        hdr.addView(tvH); hdr.addView(tvHSub);
        root.addView(hdr);

        // Column labels row
        LinearLayout colHdr = new LinearLayout(this);
        colHdr.setOrientation(LinearLayout.HORIZONTAL);
        colHdr.setGravity(Gravity.CENTER_VERTICAL);
        colHdr.setBackgroundColor(Color.parseColor("#E8F5E9"));
        colHdr.setPadding(dp(12), dp(8), dp(12), dp(8));
        addColLabel(colHdr, "TEAM", 0, 1.8f);
        addColLabel(colHdr, "M",   dp(34), 0);
        addColLabel(colHdr, "W",   dp(34), 0);
        addColLabel(colHdr, "L",   dp(34), 0);
        addColLabel(colHdr, "T",   dp(30), 0);
        addColLabel(colHdr, "N/R", dp(36), 0);
        addColLabel(colHdr, "PTS", dp(36), 0);
        addColLabel(colHdr, "NRR", dp(50), 0);
        root.addView(colHdr);

        // Team entry rows
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(10), dp(8), dp(10), dp(8));

        List<String> teamNames = new ArrayList<>();
        List<EditText[]> fieldsList = new ArrayList<>();

        JSONObject manualPT;
        try {
            manualPT = new JSONObject(tourPrefs.getString("MANUAL_PT", "{}"));
        } catch (Exception e) { manualPT = new JSONObject(); }
        final JSONObject manualPTFinal = manualPT;

        try {
            JSONArray tArr = grpObj.getJSONArray("teams");
            for (int t = 0; t < tArr.length(); t++) {
                String tName = tArr.getString(t);
                teamNames.add(tName);
                String key = groupName + "__" + tName;

                int defM = 0, defW = 0, defL = 0, defT = 0, defNR = 0, defPts = 0;
                double defNRR = 0.0;
                if (manualPTFinal.has(key)) {
                    JSONObject m = manualPTFinal.getJSONObject(key);
                    defM = m.optInt("played", 0); defW = m.optInt("won", 0);
                    defL = m.optInt("lost", 0); defT = m.optInt("tied", 0);
                    defNR = m.optInt("nr", 0); defPts = m.optInt("pts", 0);
                    defNRR = m.optDouble("nrr", 0.0);
                }

                LinearLayout teamEntryRow = new LinearLayout(this);
                teamEntryRow.setOrientation(LinearLayout.HORIZONTAL);
                teamEntryRow.setGravity(Gravity.CENTER_VERTICAL);
                GradientDrawable rBg = new GradientDrawable();
                rBg.setColor(t % 2 == 0 ? Color.WHITE : Color.parseColor("#FAFFFE"));
                rBg.setStroke(dp(1), Color.parseColor("#E2F0E2")); rBg.setCornerRadius(dp(8));
                teamEntryRow.setBackground(rBg);
                teamEntryRow.setPadding(dp(8), dp(6), dp(8), dp(6));
                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rLp.setMargins(0, dp(3), 0, dp(3)); teamEntryRow.setLayoutParams(rLp);

                // Team name label
                TextView tvTeam = new TextView(this); tvTeam.setText(tName);
                tvTeam.setTextSize(11); tvTeam.setTypeface(null, Typeface.BOLD);
                tvTeam.setTextColor(Color.parseColor("#1B5E20"));
                tvTeam.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.8f));
                tvTeam.setMaxLines(2); tvTeam.setEllipsize(android.text.TextUtils.TruncateAt.END);
                teamEntryRow.addView(tvTeam);

                // 7 input fields: M, W, L, T, N/R, PTS, NRR
                EditText[] fields = new EditText[7];
                int[] defVals = {defM, defW, defL, defT, defNR, defPts};
                int[] widths = {dp(34), dp(34), dp(34), dp(30), dp(36), dp(36)};
                for (int f = 0; f < 6; f++) {
                    fields[f] = makeSmallInput(String.valueOf(defVals[f]), false);
                    fields[f].setLayoutParams(new LinearLayout.LayoutParams(widths[f], dp(34)));
                    teamEntryRow.addView(fields[f]);
                }
                // NRR — decimal
                fields[6] = makeSmallInput(defNRR == 0.0 ? "0.000" : String.format("%+.3f", defNRR), true);
                fields[6].setLayoutParams(new LinearLayout.LayoutParams(dp(50), dp(34)));
                teamEntryRow.addView(fields[6]);

                fieldsList.add(fields);
                body.addView(teamEntryRow);
            }
        } catch (Exception e) { e.printStackTrace(); }

        ScrollView bodyScroll = new ScrollView(this);
        bodyScroll.addView(body);
        LinearLayout.LayoutParams bsLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bsLp.height = Math.min(dp(320), teamNames.size() * dp(52));
        bodyScroll.setLayoutParams(bsLp);
        root.addView(bodyScroll);

        // Footer note
        TextView tvNote = new TextView(this);
        tvNote.setText("💡 M=Matches  W=Won  L=Lost  T=Tied  N/R=No Result  PTS=Points  NRR=Net Run Rate");
        tvNote.setTextSize(9); tvNote.setTextColor(Color.parseColor("#94A3B8"));
        tvNote.setPadding(dp(14), dp(8), dp(14), dp(4));
        root.addView(tvNote);

        // Action buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams brLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brLp.setMargins(dp(12), dp(6), dp(12), dp(14)); btnRow.setLayoutParams(brLp);

        Button btnCancel = new Button(this); btnCancel.setText("Cancel"); btnCancel.setAllCaps(false);
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        GradientDrawable cancelBg = new GradientDrawable(); cancelBg.setColor(Color.parseColor("#F1F5F9")); cancelBg.setCornerRadius(dp(10));
        btnCancel.setBackground(cancelBg);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        cancelLp.setMargins(0, 0, dp(8), 0); btnCancel.setLayoutParams(cancelLp);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        final JSONObject manualPTSave = manualPT;
        Button btnSave = new Button(this); btnSave.setText("💾  Save Table"); btnSave.setAllCaps(false);
        btnSave.setTextColor(Color.WHITE); btnSave.setTypeface(null, Typeface.BOLD);
        GradientDrawable saveBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#388E3C")});
        saveBg.setCornerRadius(dp(10)); btnSave.setBackground(saveBg);
        btnSave.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1.5f));
        btnSave.setOnClickListener(v -> {
            try {
                JSONObject newManual = new JSONObject(tourPrefs.getString("MANUAL_PT", "{}"));
                java.util.List<JSONObject> ptRows = new java.util.ArrayList<>();
                for (int i = 0; i < teamNames.size(); i++) {
                    String tName = teamNames.get(i);
                    String key = groupName + "__" + tName;
                    EditText[] fs = fieldsList.get(i);
                    JSONObject entry = new JSONObject();
                    entry.put("played", safeInt(fs[0]));
                    entry.put("won",    safeInt(fs[1]));
                    entry.put("lost",   safeInt(fs[2]));
                    entry.put("tied",   safeInt(fs[3]));
                    entry.put("nr",     safeInt(fs[4]));
                    entry.put("pts",    safeInt(fs[5]));
                    entry.put("nrr",    safeDouble(fs[6]));
                    newManual.put(key, entry);

                    // ☁️ SUPABASE: Point table row তৈরি হচ্ছে
                    JSONObject ptRow = new JSONObject();
                    ptRow.put("team_name", tName);
                    ptRow.put("played",    safeInt(fs[0]));
                    ptRow.put("won",       safeInt(fs[1]));
                    ptRow.put("lost",      safeInt(fs[2]));
                    ptRow.put("tied",      safeInt(fs[3]));
                    ptRow.put("no_result", safeInt(fs[4]));
                    ptRow.put("points",    safeInt(fs[5]));
                    ptRow.put("nrr",       safeDouble(fs[6]));
                    ptRows.add(ptRow);
                }
                tourPrefs.edit().putString("MANUAL_PT", newManual.toString()).apply();

                // ☁️ SUPABASE: Supabase এ point table sync করা হচ্ছে
                String supaTournId = tourPrefs.getString("SUPABASE_TOURNAMENT_ID", "");
                if (!supaTournId.isEmpty()) {
                    FirebaseSync.upsertPointTable(supaTournId, ptRows);
                }

                Toast.makeText(this, "✅ Point table saved!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadPointTable();
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        btnRow.addView(btnCancel); btnRow.addView(btnSave);
        root.addView(btnRow);

        ScrollView sv = new ScrollView(this); sv.addView(root);
        dialog.setContentView(sv);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            int w = (int)(getResources().getDisplayMetrics().widthPixels * 0.96f);
            dialog.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private EditText makeSmallInput(String defVal, boolean isDecimal) {
        EditText et = new EditText(this);
        et.setText(defVal); et.setTextSize(11); et.setGravity(Gravity.CENTER);
        et.setInputType(isDecimal
            ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED
            : InputType.TYPE_CLASS_NUMBER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE); bg.setStroke(dp(1), Color.parseColor("#DCFCE7")); bg.setCornerRadius(dp(6));
        et.setBackground(bg); et.setPadding(dp(2), dp(2), dp(2), dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        lp.setMargins(dp(2), 0, dp(2), 0); et.setLayoutParams(lp);
        return et;
    }

    private void addColLabel(LinearLayout parent, String text, int widthDp, float weight) {
        TextView tv = new TextView(this); tv.setText(text);
        tv.setTextSize(9); tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#1B5E20")); tv.setGravity(Gravity.CENTER);
        if (weight > 0) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
            lp.setMargins(dp(2), 0, dp(2), 0); tv.setLayoutParams(lp);
        } else {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthDp, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(2), 0, dp(2), 0); tv.setLayoutParams(lp);
        }
        parent.addView(tv);
    }

    private int safeInt(EditText et) {
        try { return Integer.parseInt(et.getText().toString().trim()); } catch (Exception e) { return 0; }
    }
    private double safeDouble(EditText et) {
        try { return Double.parseDouble(et.getText().toString().trim()); } catch (Exception e) { return 0.0; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // RENDER TABLE — ছবির মতো premium design
    // ─────────────────────────────────────────────────────────────────────
    private void renderGroupTable(String groupName, List<TeamRow> rows) {

        // ── Outer wrapper card ────────────────────────────────────────────
        LinearLayout outerCard = new LinearLayout(this);
        outerCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable outerBg = new GradientDrawable();
        outerBg.setColor(Color.WHITE);
        outerBg.setCornerRadius(dp(12));
        outerBg.setStroke(dp(1), Color.parseColor("#DDE3EA"));
        outerCard.setBackground(outerBg);
        outerCard.setElevation(dp(4));
        LinearLayout.LayoutParams outerLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        outerLp.setMargins(0, dp(12), 0, dp(4));
        outerCard.setLayoutParams(outerLp);

        // ── Compact green header bar — group name, no emoji ───────────────
        LinearLayout headerBar = new LinearLayout(this);
        headerBar.setOrientation(LinearLayout.HORIZONTAL);
        headerBar.setGravity(Gravity.CENTER_VERTICAL);
        headerBar.setPadding(dp(16), dp(10), dp(16), dp(10));
        GradientDrawable headerBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor("#0D3B0E"), Color.parseColor("#2E7D32")});
        headerBg.setCornerRadii(new float[]{dp(12), dp(12), dp(12), dp(12), 0, 0, 0, 0});
        headerBar.setBackground(headerBg);

        // Group badge dot
        View dot = new View(this);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(Color.parseColor("#4ADE80"));
        dot.setBackground(dotBg);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(7), dp(7));
        dotLp.gravity = Gravity.CENTER_VERTICAL;
        dotLp.setMargins(0, 0, dp(8), 0);
        dot.setLayoutParams(dotLp);
        headerBar.addView(dot);

        TextView tvGroupName = new TextView(this);
        tvGroupName.setText(groupName);
        tvGroupName.setTextSize(14);
        tvGroupName.setTypeface(null, Typeface.BOLD);
        tvGroupName.setTextColor(Color.WHITE);
        tvGroupName.setLetterSpacing(0.04f);
        tvGroupName.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        headerBar.addView(tvGroupName);

        // Team count badge
        TextView tvCount = new TextView(this);
        tvCount.setText(rows.size() + " teams");
        tvCount.setTextSize(10);
        tvCount.setTextColor(Color.parseColor("#A5D6A7"));
        tvCount.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        headerBar.addView(tvCount);

        outerCard.addView(headerBar);

        // ── Column header row — aligned with data rows ───────────────────
        LinearLayout colHeader = new LinearLayout(this);
        colHeader.setOrientation(LinearLayout.HORIZONTAL);
        colHeader.setGravity(Gravity.CENTER_VERTICAL);
        colHeader.setPadding(dp(12), dp(9), dp(12), dp(9));
        colHeader.setBackgroundColor(Color.parseColor("#F0F9F0"));

        // Columns use EXACT same weights/widths as data rows below
        addPTH(colHeader, "TEAM",  0,   2.8f, Gravity.START);
        addPTH(colHeader, "M",     0,  0.7f,  Gravity.CENTER);
        addPTH(colHeader, "W",     0,  0.7f,  Gravity.CENTER);
        addPTH(colHeader, "L",     0,  0.7f,  Gravity.CENTER);
        addPTH(colHeader, "T",     0,  0.7f,  Gravity.CENTER);
        addPTH(colHeader, "N/R",   0,  0.8f,  Gravity.CENTER);
        addPTH(colHeader, "PTS",   0,  0.9f,  Gravity.CENTER);
        addPTH(colHeader, "NRR",   0,  1.4f,  Gravity.CENTER);
        outerCard.addView(colHeader);

        // ── 1dp separator ─────────────────────────────────────────────────
        View sep = new View(this);
        sep.setBackgroundColor(Color.parseColor("#C8E6C9"));
        sep.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        outerCard.addView(sep);

        // ── Data rows ─────────────────────────────────────────────────────
        for (int i = 0; i < rows.size(); i++) {
            TeamRow r = rows.get(i);
            double nrr = (r.nrrManual != Double.MAX_VALUE) ? r.nrrManual
                : (r.oversFor > 0 && r.oversAgainst > 0)
                    ? (r.runsFor / r.oversFor) - (r.runsAgainst / r.oversAgainst) : 0.0;
            String nrrStr = String.format("%+.3f", nrr);

            boolean isTop = (i == 0);
            boolean isSecond = (i == 1);

            // Alternating row background for scannability
            int rowBgColor = (i % 2 == 0) ? Color.WHITE : Color.parseColor("#FAFFFE");

            LinearLayout rowCard = new LinearLayout(this);
            rowCard.setOrientation(LinearLayout.HORIZONTAL);
            rowCard.setGravity(Gravity.CENTER_VERTICAL);
            rowCard.setPadding(dp(12), dp(10), dp(12), dp(10));
            rowCard.setBackgroundColor(rowBgColor);
            rowCard.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Rank indicator strip on left edge for top 2
            if (isTop || isSecond) {
                View strip = new View(this);
                strip.setBackgroundColor(isTop ? Color.parseColor("#16A34A") : Color.parseColor("#60A5FA"));
                LinearLayout.LayoutParams stripLp = new LinearLayout.LayoutParams(dp(3), ViewGroup.LayoutParams.MATCH_PARENT);
                stripLp.setMargins(-dp(12), -dp(10), dp(8), -dp(10));
                strip.setLayoutParams(stripLp);
                rowCard.addView(strip);
            }

            // Team name — weight 2.8 (more space for name)
            TextView tvTeam = new TextView(this);
            tvTeam.setText(r.name);
            tvTeam.setTextSize(13);
            tvTeam.setTypeface(null, Typeface.BOLD);
            tvTeam.setTextColor(isTop ? Color.parseColor("#15803D") : Color.parseColor("#1E293B"));
            tvTeam.setMaxLines(1);
            tvTeam.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tvTeam.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.8f));
            rowCard.addView(tvTeam);

            // M — neutral
            addPTStatCell(rowCard, String.valueOf(r.played), 0.7f, Color.parseColor("#64748B"), false, "#F1F5F9", "#CBD5E1");
            // W — green
            addPTStatCell(rowCard, String.valueOf(r.won),    0.7f, Color.parseColor("#15803D"), true,  "#DCFCE7", "#86EFAC");
            // L — red
            addPTStatCell(rowCard, String.valueOf(r.lost),   0.7f, Color.parseColor("#DC2626"), false, "#FEF2F2", "#FCA5A5");
            // T — amber
            addPTStatCell(rowCard, String.valueOf(r.tied),   0.7f, Color.parseColor("#D97706"), false, "#FFFBEB", "#FCD34D");
            // N/R — slate
            addPTStatCell(rowCard, String.valueOf(r.nr),     0.8f, Color.parseColor("#6B7280"), false, "#F9FAFB", "#E5E7EB");

            // PTS — highlighted premium cell
            TextView tvPts = new TextView(this);
            tvPts.setText(String.valueOf(r.pts));
            tvPts.setTextSize(13);
            tvPts.setGravity(Gravity.CENTER);
            tvPts.setTypeface(null, Typeface.BOLD);
            tvPts.setTextColor(Color.WHITE);
            GradientDrawable ptsBg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#22C55E"), Color.parseColor("#16A34A")});
            ptsBg.setCornerRadius(dp(7));
            tvPts.setBackground(ptsBg);
            tvPts.setIncludeFontPadding(false);
            tvPts.setPadding(0, dp(2), 0, dp(2));
            LinearLayout.LayoutParams ptsLp = new LinearLayout.LayoutParams(0, dp(30), 0.9f);
            ptsLp.setMargins(dp(3), 0, 0, 0);
            ptsLp.gravity = Gravity.CENTER_VERTICAL;
            tvPts.setLayoutParams(ptsLp);
            rowCard.addView(tvPts);

            // NRR — colored by sign, proper centering
            int nrrColor = nrr > 0 ? Color.parseColor("#1565C0")
                : nrr < 0 ? Color.parseColor("#B91C1C")
                : Color.parseColor("#6B7280");
            String nrrBg = nrr > 0 ? "#EFF6FF" : nrr < 0 ? "#FFF5F5" : "#F9FAFB";
            String nrrBorder = nrr > 0 ? "#BFDBFE" : nrr < 0 ? "#FECACA" : "#E5E7EB";
            addPTStatCell(rowCard, nrrStr, 1.4f, nrrColor, false, nrrBg, nrrBorder);

            outerCard.addView(rowCard);

            // Row divider (not after last row)
            if (i < rows.size() - 1) {
                View rowDiv = new View(this);
                rowDiv.setBackgroundColor(Color.parseColor("#F0F4F0"));
                rowDiv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
                outerCard.addView(rowDiv);
            }
        }

        // ── Bottom padding inside card ─────────────────────────────────
        View bottomSpacer = new View(this);
        bottomSpacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)));
        outerCard.addView(bottomSpacer);

        mainContainer.addView(outerCard);
    }

    // Point Table Header cell — weight-based for alignment
    private void addPTH(LinearLayout parent, String text, int widthPx, float weight, int gravity) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(10);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#2E7D32"));
        tv.setGravity(gravity);
        tv.setLetterSpacing(0.06f);
        if (weight > 0) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
            if (gravity == Gravity.CENTER) lp.setMargins(dp(3), 0, 0, 0);
            tv.setLayoutParams(lp);
        } else {
            tv.setLayoutParams(new LinearLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        parent.addView(tv);
    }

    // Premium stat cell — weight-based, colored background, perfect center
    private void addPTStatCell(LinearLayout parent, String text, float weight,
                                int textColor, boolean bold, String bgHex, String borderHex) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(textColor);
        tv.setIncludeFontPadding(false);
        tv.setPadding(0, dp(2), 0, dp(2));
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(bgHex));
        bg.setCornerRadius(dp(6));
        bg.setStroke(dp(1), Color.parseColor(borderHex));
        tv.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(30), weight);
        lp.setMargins(dp(3), 0, 0, 0);
        lp.gravity = Gravity.CENTER_VERTICAL;
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }

    // Legacy addPTCell kept for any other usage
    private void addPTCell(LinearLayout parent, String text, int widthPx, int color, boolean bold) {
        addPTStatCell(parent, text, 0, color, bold, "#F9FAFB", "#E5E7EB");
    }

        private void addTH(LinearLayout parent, String text, int widthDp, float weight, int gravity) {
        TextView tv = new TextView(this); tv.setText(text);
        tv.setTextSize(10); tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#6B7280")); tv.setGravity(gravity);
        tv.setLetterSpacing(0.04f);
        if (weight > 0) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
            lp.setMargins(dp(22), 0, 0, 0); tv.setLayoutParams(lp);
        } else {
            tv.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp), ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        parent.addView(tv);
    }

    private void addTD(LinearLayout parent, String text, int widthDp, int color, boolean bold) {
        TextView tv = new TextView(this); tv.setText(text);
        tv.setTextSize(12); tv.setGravity(Gravity.CENTER);
        tv.setTextColor(color); if (bold) tv.setTypeface(null, Typeface.BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp), ViewGroup.LayoutParams.WRAP_CONTENT));
        parent.addView(tv);
    }

    private void addFooterLegend(LinearLayout parent, String colorHex, String label) {
        View dot = new View(this);
        GradientDrawable dotBg = new GradientDrawable(); dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(Color.parseColor(colorHex)); dot.setBackground(dotBg);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(8), dp(8));
        dotLp.setMargins(0, 0, dp(4), 0); dotLp.gravity = Gravity.CENTER_VERTICAL;
        dot.setLayoutParams(dotLp); parent.addView(dot);
        TextView tv = new TextView(this); tv.setText(label);
        tv.setTextSize(10); tv.setTextColor(Color.parseColor("#6B7280"));
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLp.setMargins(0, 0, dp(16), 0); tv.setLayoutParams(tvLp); parent.addView(tv);
    }

    // ─────────────────────────────────────────────────────────────────────
    private void addHeader() {
        // Tournament title card — premium look
        LinearLayout headerCard = new LinearLayout(this);
        headerCard.setOrientation(LinearLayout.VERTICAL);
        headerCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColor(Color.WHITE);
        hBg.setCornerRadius(dp(12));
        hBg.setStroke(dp(1), Color.parseColor("#DDE3EA"));
        headerCard.setBackground(hBg);
        headerCard.setElevation(dp(2));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(tournamentName);
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setLetterSpacing(0.01f);
        headerCard.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Points Standings");
        tvSub.setTextSize(11);
        tvSub.setTextColor(Color.parseColor("#64748B"));
        tvSub.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0, dp(2), 0, 0);
        tvSub.setLayoutParams(subLp);
        headerCard.addView(tvSub);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(4));
        headerCard.setLayoutParams(cardLp);
        mainContainer.addView(headerCard);
    }

    private void addEmptyState() {
        LinearLayout emptyCard = new LinearLayout(this);
        emptyCard.setOrientation(LinearLayout.VERTICAL);
        emptyCard.setGravity(Gravity.CENTER);
        emptyCard.setPadding(dp(24), dp(48), dp(24), dp(48));
        GradientDrawable emBg = new GradientDrawable();
        emBg.setColor(Color.WHITE);
        emBg.setCornerRadius(dp(14));
        emBg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        emptyCard.setBackground(emBg);
        LinearLayout.LayoutParams emLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        emLp.setMargins(0, dp(16), 0, 0);
        emptyCard.setLayoutParams(emLp);

        TextView tvIcon = new TextView(this);
        tvIcon.setText("📭");
        tvIcon.setTextSize(36);
        tvIcon.setGravity(Gravity.CENTER);
        emptyCard.addView(tvIcon);

        TextView tvMsg = new TextView(this);
        tvMsg.setText("No match results yet");
        tvMsg.setTextSize(15);
        tvMsg.setTypeface(null, Typeface.BOLD);
        tvMsg.setTextColor(Color.parseColor("#374151"));
        tvMsg.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        msgLp.setMargins(0, dp(10), 0, dp(6));
        tvMsg.setLayoutParams(msgLp);
        emptyCard.addView(tvMsg);

        TextView tvHint = new TextView(this);
        tvHint.setText("Play matches or use Manual Entry\nto populate the table.");
        tvHint.setTextSize(12);
        tvHint.setTextColor(Color.parseColor("#94A3B8"));
        tvHint.setGravity(Gravity.CENTER);
        tvHint.setLineSpacing(dp(2), 1f);
        emptyCard.addView(tvHint);

        mainContainer.addView(emptyCard);
    }

    // ─────────────────────────────────────────────────────────────────────
    private void generatePDF() {
        PdfDocument document = new PdfDocument();

        // ── collect data same as loadPointTable ───────────────────────────
        List<String[]> pdfRows = new ArrayList<>(); // groupName marker or data row
        List<Boolean> isGroupHeader = new ArrayList<>();

        try {
            String allData = tourPrefs.getString("ALL_DATA", "");
            JSONObject mainObj  = allData.isEmpty() ? new JSONObject() : new JSONObject(allData);
            JSONObject results  = new JSONObject(resultPrefs.getString("RESULT_DATA", "{}"));
            JSONObject ptSaved  = new JSONObject(tourPrefs.getString("POINT_TABLE", "{}"));
            JSONObject manualPT = new JSONObject(tourPrefs.getString("MANUAL_PT", "{}"));
            JSONArray dynGroups = mainObj.optJSONArray("DynamicGroups");

            int winPts = 2, tiePts = 1;
            try { winPts = Integer.parseInt(tourPrefs.getString("WIN_POINTS", "2")); } catch (Exception ignored) {}
            try { tiePts = Integer.parseInt(tourPrefs.getString("TIE_POINTS", "1")); } catch (Exception ignored) {}
            JSONArray groupMatches = mainObj.optJSONArray("MatchesGroup");
            List<String> allTeams = getAllTeams(mainObj);

            if (dynGroups != null) {
                for (int g = 0; g < dynGroups.length(); g++) {
                    JSONObject grpObj = dynGroups.getJSONObject(g);
                    String gName = grpObj.optString("name", "Group " + (char)('A' + g));
                    JSONArray tArr = grpObj.getJSONArray("teams");

                    List<TeamRow> rows = new ArrayList<>();
                    for (int t = 0; t < tArr.length(); t++) rows.add(new TeamRow(tArr.getString(t)));

                    if (groupMatches != null) {
                        for (int i = 0; i < groupMatches.length(); i++) {
                            String mid = "GroupMatch_" + i;
                            if (!results.has(mid)) continue;
                            int t1Idx = groupMatches.getJSONObject(i).getInt("team1_idx");
                            int t2Idx = groupMatches.getJSONObject(i).getInt("team2_idx");
                            if (t1Idx >= allTeams.size() || t2Idx >= allTeams.size()) continue;
                            String t1 = allTeams.get(t1Idx), t2 = allTeams.get(t2Idx);
                            JSONObject res = results.getJSONObject(mid);
                            int s1 = getRuns(res.optString("s1","0"));
                            int s2 = getRuns(res.optString("s2","0"));
                            TeamRow r1 = findRow(rows, t1), r2 = findRow(rows, t2);
                            if (r1 == null || r2 == null) continue;
                            r1.played++; r2.played++;
                            if (s1 > s2) { r1.won++; r2.lost++; r1.pts += winPts; }
                            else if (s2 > s1) { r2.won++; r1.lost++; r2.pts += winPts; }
                            else { r1.tied++; r2.tied++; r1.pts += tiePts; r2.pts += tiePts; }
                            double overs = getOversValue(tourPrefs.getString("TOTAL_OVERS", "20"));
                            if (overs > 0) {
                                r1.runsFor += s1; r1.runsAgainst += s2;
                                r2.runsFor += s2; r2.runsAgainst += s1;
                                r1.oversFor += overs; r1.oversAgainst += overs;
                                r2.oversFor += overs; r2.oversAgainst += overs;
                            }
                        }
                    }
                    for (TeamRow r : rows) {
                        if (ptSaved.has(r.name) && r.played == 0) {
                            JSONObject saved = ptSaved.getJSONObject(r.name);
                            r.pts = saved.optInt("pts", 0); r.played = saved.optInt("played", 0);
                            r.won = saved.optInt("won", 0); r.lost = r.played - r.won;
                        }
                    }
                    for (TeamRow r : rows) {
                        String key = gName + "__" + r.name;
                        if (manualPT.has(key)) {
                            JSONObject m = manualPT.getJSONObject(key);
                            r.played = m.optInt("played", r.played); r.won = m.optInt("won", r.won);
                            r.lost = m.optInt("lost", r.lost); r.tied = m.optInt("tied", r.tied);
                            r.nr = m.optInt("nr", r.nr); r.pts = m.optInt("pts", r.pts);
                            r.nrrManual = m.optDouble("nrr", Double.MAX_VALUE);
                        }
                    }
                    Collections.sort(rows);

                    pdfRows.add(new String[]{gName});
                    isGroupHeader.add(true);
                    for (TeamRow r : rows) {
                        double nrr = (r.nrrManual != Double.MAX_VALUE) ? r.nrrManual
                            : (r.oversFor > 0 && r.oversAgainst > 0)
                                ? (r.runsFor / r.oversFor) - (r.runsAgainst / r.oversAgainst) : 0.0;
                        pdfRows.add(new String[]{
                            r.name, String.valueOf(r.played), String.valueOf(r.won),
                            String.valueOf(r.lost), String.valueOf(r.tied),
                            String.valueOf(r.nr), String.valueOf(r.pts),
                            String.format("%+.3f", nrr)
                        });
                        isGroupHeader.add(false);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        // ── PDF drawing ───────────────────────────────────────────────────
        final int PAGE_W = 595, PAGE_H = 842;
        final int MARGIN = 32;
        final int TABLE_W = PAGE_W - MARGIN * 2;

        // Column widths (sum = TABLE_W = 531)
        final int[] COL_W = {160, 42, 42, 42, 42, 42, 44, 72};
        final String[] COL_HEADERS = {"TEAM", "M", "W", "L", "T", "N/R", "PTS", "NRR"};

        Paint p = new Paint(); p.setAntiAlias(true);

        int pageNum = 1;
        PdfDocument.Page page = null;
        Canvas c = null;
        float y = 0;

        // Helper to start a new page
        // We'll use a simple approach: start first page manually, then check space
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create();
        page = document.startPage(pageInfo);
        c = page.getCanvas();

        // ── Page header ───────────────────────────────────────────────────
        p.setColor(Color.parseColor("#1B5E20"));
        c.drawRect(0, 0, PAGE_W, 70, p);
        p.setColor(Color.WHITE); p.setTextSize(20);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(tournamentName, PAGE_W / 2f, 38, p);
        p.setTextSize(10); p.setColor(Color.parseColor("#A5D6A7"));
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        c.drawText("POINT TABLE", PAGE_W / 2f, 58, p);

        y = 90;

        // ── Draw column header row ────────────────────────────────────────
        final float ROW_H = 22f;

        // Draw each group + its rows
        for (int ri = 0; ri < pdfRows.size(); ri++) {
            boolean isHeader = isGroupHeader.get(ri);
            String[] row = pdfRows.get(ri);

            // Check if we need a new page (leave 40px for footer)
            if (y + ROW_H + 40 > PAGE_H - 20) {
                // footer
                p.setColor(Color.parseColor("#90A4AE")); p.setTextSize(8);
                p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
                p.setTextAlign(Paint.Align.CENTER);
                c.drawText("Generated by Cricket ScoreZ Pro", PAGE_W / 2f, PAGE_H - 10, p);
                document.finishPage(page);
                pageNum++;
                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create();
                page = document.startPage(pageInfo);
                c = page.getCanvas();
                y = 20;
            }

            if (isHeader) {
                // Group name bar
                y += 8;
                p.setColor(Color.parseColor("#0D3B0E"));
                c.drawRect(MARGIN, y, MARGIN + TABLE_W, y + ROW_H + 4, p);
                p.setColor(Color.WHITE); p.setTextSize(11);
                p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                p.setTextAlign(Paint.Align.LEFT);
                c.drawText("  " + row[0], MARGIN + 6, y + ROW_H - 2, p);
                y += ROW_H + 4;

                // Column headers
                p.setColor(Color.parseColor("#E8F5E9"));
                c.drawRect(MARGIN, y, MARGIN + TABLE_W, y + ROW_H, p);
                float cx = MARGIN;
                for (int col = 0; col < COL_HEADERS.length; col++) {
                    p.setColor(Color.parseColor("#1B5E20")); p.setTextSize(8);
                    p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    p.setTextAlign(col == 0 ? Paint.Align.LEFT : Paint.Align.CENTER);
                    float textX = col == 0 ? cx + 4 : cx + COL_W[col] / 2f;
                    c.drawText(COL_HEADERS[col], textX, y + ROW_H - 6, p);
                    cx += COL_W[col];
                }
                y += ROW_H;
            } else {
                // Data row
                int rowIdx = 0;
                // Count how many data rows before this in current group
                for (int k = ri - 1; k >= 0 && !isGroupHeader.get(k); k--) rowIdx++;

                int bgColor = (rowIdx % 2 == 0) ? Color.WHITE : Color.parseColor("#F0FDF4");
                p.setColor(bgColor);
                c.drawRect(MARGIN, y, MARGIN + TABLE_W, y + ROW_H, p);

                // Border line
                p.setColor(Color.parseColor("#E2F0E2")); p.setStrokeWidth(0.5f);
                p.setStyle(Paint.Style.STROKE);
                c.drawRect(MARGIN, y, MARGIN + TABLE_W, y + ROW_H, p);
                p.setStyle(Paint.Style.FILL);

                float cx = MARGIN;
                for (int col = 0; col < row.length && col < COL_W.length; col++) {
                    String val = row[col];
                    if (col == 0) {
                        // Team name
                        p.setColor(Color.parseColor("#1E293B")); p.setTextSize(9);
                        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        p.setTextAlign(Paint.Align.LEFT);
                        c.drawText(val.length() > 20 ? val.substring(0, 19) + "…" : val, cx + 4, y + ROW_H - 6, p);
                    } else if (col == 6) {
                        // PTS — green highlight
                        p.setColor(Color.parseColor("#22C55E"));
                        float bx = cx + 3, bw = COL_W[col] - 6;
                        c.drawRoundRect(bx, y + 3, bx + bw, y + ROW_H - 3, 4, 4, p);
                        p.setColor(Color.WHITE); p.setTextSize(9);
                        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        p.setTextAlign(Paint.Align.CENTER);
                        c.drawText(val, cx + COL_W[col] / 2f, y + ROW_H - 6, p);
                    } else if (col == 7) {
                        // NRR — colored
                        double nrrVal = 0;
                        try { nrrVal = Double.parseDouble(val); } catch (Exception ignored) {}
                        p.setColor(nrrVal > 0 ? Color.parseColor("#1565C0")
                            : nrrVal < 0 ? Color.parseColor("#B91C1C") : Color.parseColor("#6B7280"));
                        p.setTextSize(8); p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                        p.setTextAlign(Paint.Align.CENTER);
                        c.drawText(val, cx + COL_W[col] / 2f, y + ROW_H - 6, p);
                    } else {
                        p.setColor(Color.parseColor("#374151")); p.setTextSize(9);
                        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                        p.setTextAlign(Paint.Align.CENTER);
                        c.drawText(val, cx + COL_W[col] / 2f, y + ROW_H - 6, p);
                    }
                    cx += COL_W[col];
                }
                y += ROW_H;
            }
        }

        // Footer on last page
        p.setColor(Color.parseColor("#90A4AE")); p.setTextSize(8);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText("Generated by Cricket ScoreZ Pro", PAGE_W / 2f, PAGE_H - 10, p);
        document.finishPage(page);

        // ── Save & notify ─────────────────────────────────────────────────
        String safeName = tournamentName.replaceAll("\\s+", "_");
        String fileName = safeName + "_PointTable_" + System.currentTimeMillis() + ".pdf";
        PdfManager.saveAndOpenPdf(this, document, fileName, "Point Table Downloaded \u2705");
    }

    // ─────────────────────────────────────────────────────────────────────
    private int getRuns(String s) {
        try { return Integer.parseInt(s.split("/")[0].replaceAll("[^0-9]", "")); } catch (Exception e) { return 0; }
    }
    private double getOversValue(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 20.0; }
    }
    private List<String> getAllTeams(JSONObject mainObj) throws Exception {
        List<String> t = new ArrayList<>(); t.add("Select Team");
        if (mainObj.has("DynamicGroups")) {
            JSONArray g = mainObj.getJSONArray("DynamicGroups");
            for (int i = 0; i < g.length(); i++) {
                JSONArray ta = g.getJSONObject(i).getJSONArray("teams");
                for (int j = 0; j < ta.length(); j++) t.add(ta.getString(j));
            }
        }
        return t;
    }
    private TeamRow findRow(List<TeamRow> rows, String name) {
        for (TeamRow r : rows) if (r.name.equals(name)) return r;
        return null;
    }
    private int dp(int val) { return Math.round(val * getResources().getDisplayMetrics().density); }

    static class TeamRow implements Comparable<TeamRow> {
        String name; int played = 0, won = 0, lost = 0, tied = 0, nr = 0, pts = 0;
        double runsFor = 0, runsAgainst = 0, oversFor = 0, oversAgainst = 0;
        double nrrManual = Double.MAX_VALUE; // MAX_VALUE = not manually set
        TeamRow(String n) { name = n; }
        double getNRR() {
            if (nrrManual != Double.MAX_VALUE) return nrrManual;
            return (oversFor > 0 && oversAgainst > 0) ? (runsFor / oversFor) - (runsAgainst / oversAgainst) : 0;
        }
        @Override public int compareTo(TeamRow o) {
            if (o.pts != pts) return o.pts - pts;
            return Double.compare(o.getNRR(), getNRR());
        }
    }
}
