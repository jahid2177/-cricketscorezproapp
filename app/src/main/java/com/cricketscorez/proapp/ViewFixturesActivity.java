package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ViewFixturesActivity extends Activity {

    LinearLayout mainContainer;
    Button btnBack, btnPdf;
    List<String> allTeams;
    JSONObject savedData;
    String tournamentName = "Cricket Tournament";

    // Match counter for numbering
    int matchCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_fixtures);

        mainContainer = findViewById(R.id.mainContainer);
        btnBack       = findViewById(R.id.btnBack);
        btnPdf        = findViewById(R.id.btnPdf);

        SharedPreferences settingsPrefs = getSharedPreferences("TournamentSettings", MODE_PRIVATE);
        tournamentName = settingsPrefs.getString("TOURNAMENT_NAME", "Cricket Tournament");
        if (tournamentName == null || tournamentName.isEmpty()) tournamentName = "Cricket Tournament";

        btnBack.setOnClickListener(v -> finish());

        btnPdf.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                } else {
                    createPdf();
                }
            } else {
                createPdf();
            }
        });

        loadAndDisplayData();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOAD & DISPLAY
    // ═══════════════════════════════════════════════════════════════════════
    private void loadAndDisplayData() {
        SharedPreferences prefs = getSharedPreferences("TournamentData", MODE_PRIVATE);
        String data = prefs.getString("ALL_DATA", "");

        if (data.isEmpty()) {
            showEmptyState();
            return;
        }

        try {
            savedData = new JSONObject(data);
            allTeams  = new ArrayList<>();
            allTeams.add("Select Team"); // index 0 = placeholder

            // Load teams from dynamic groups
            loadTeamsFromDynamicGroups(savedData.optJSONArray("DynamicGroups"));
            // Fallback: old static group format
            loadTeamsIntoList(savedData.optJSONArray("GroupA"));
            loadTeamsIntoList(savedData.optJSONArray("GroupB"));
            loadTeamsIntoList(savedData.optJSONArray("GroupC"));
            loadTeamsIntoList(savedData.optJSONArray("GroupD"));

            matchCounter = 0;

            // Header
            addTournamentHeader();

            // Sections
            addSectionHeader("⚽  Group Stage", "#1B5E20");
            displayMatches(savedData.optJSONArray("MatchesGroup"));

            addSectionHeader("⚡  Quarter Finals", "#1565C0");
            displayMatches(savedData.optJSONArray("MatchesQF"));

            addSectionHeader("🔥  Semi Finals", "#E65100");
            displayMatches(savedData.optJSONArray("MatchesSF"));

            addSectionHeader("🏆  Final Match", "#B71C1C");
            displayMatches(savedData.optJSONArray("MatchesFinal"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading fixtures: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EMPTY STATE
    // ═══════════════════════════════════════════════════════════════════════
    private void showEmptyState() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(32), dp(60), dp(32), dp(60));

        TextView icon = new TextView(this);
        icon.setText("📋");
        icon.setTextSize(48);
        icon.setGravity(Gravity.CENTER);
        box.addView(icon);

        TextView msg = new TextView(this);
        msg.setText("No Fixtures Saved Yet");
        msg.setTextSize(18);
        msg.setTypeface(null, Typeface.BOLD);
        msg.setTextColor(Color.parseColor("#1B5E20"));
        msg.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(12), 0, dp(6));
        msg.setLayoutParams(lp);
        box.addView(msg);

        TextView sub = new TextView(this);
        sub.setText("Go back and use AI Generate\nto create your fixtures");
        sub.setTextSize(13);
        sub.setTextColor(Color.parseColor("#94A3B8"));
        sub.setGravity(Gravity.CENTER);
        box.addView(sub);

        mainContainer.addView(box);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TOURNAMENT HEADER
    // ═══════════════════════════════════════════════════════════════════════
    private void addTournamentHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);

        GradientDrawable hBg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#2E7D32")});
        hBg.setCornerRadius(dp(16));
        header.setBackground(hBg);
        header.setPadding(dp(20), dp(20), dp(20), dp(20));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(20));
        header.setLayoutParams(lp);

        TextView tvIcon = new TextView(this);
        tvIcon.setText("🏆");
        tvIcon.setTextSize(32);
        tvIcon.setGravity(Gravity.CENTER);
        header.addView(tvIcon);

        TextView tvName = new TextView(this);
        tvName.setText(tournamentName);
        tvName.setTextSize(20);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.WHITE);
        tvName.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.setMargins(0, dp(6), 0, dp(4));
        tvName.setLayoutParams(nlp);
        header.addView(tvName);

        TextView tvSub = new TextView(this);
        tvSub.setText("MATCH FIXTURES");
        tvSub.setTextSize(11);
        tvSub.setTextColor(Color.parseColor("#A5D6A7"));
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setTypeface(null, Typeface.BOLD);
        tvSub.setLetterSpacing(0.15f);
        header.addView(tvSub);

        mainContainer.addView(header);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SECTION HEADER
    // ═══════════════════════════════════════════════════════════════════════
    private void addSectionHeader(String title, String colorHex) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(16), 0, dp(10));
        row.setLayoutParams(lp);

        // Colored left bar
        View bar = new View(this);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(Color.parseColor(colorHex));
        barBg.setCornerRadius(dp(2));
        bar.setBackground(barBg);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(4), dp(22));
        barLp.setMargins(0, 0, dp(10), 0);
        bar.setLayoutParams(barLp);
        row.addView(bar);

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(13);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.05f);
        row.addView(tv);

        // Horizontal rule
        View line = new View(this);
        line.setBackgroundColor(Color.parseColor("#E2E8F0"));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(0, dp(1), 1f);
        lineLp.setMargins(dp(10), 0, 0, 0);
        lineLp.gravity = Gravity.CENTER_VERTICAL;
        line.setLayoutParams(lineLp);
        row.addView(line);

        mainContainer.addView(row);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DISPLAY MATCHES — inflates item_match_view layout (CRASH FIX)
    // ═══════════════════════════════════════════════════════════════════════
    private void displayMatches(JSONArray matches) {
        if (matches == null || matches.length() == 0) {
            addNoMatchesPlaceholder();
            return;
        }

        int count = 0;
        for (int i = 0; i < matches.length(); i++) {
            try {
                JSONObject match = matches.getJSONObject(i);
                int idx1   = match.optInt("team1_idx", 0);
                int idx2   = match.optInt("team2_idx", 0);
                String date = match.optString("date", "TBC");

                String t1 = (idx1 > 0 && idx1 < allTeams.size()) ? allTeams.get(idx1) : "TBD";
                String t2 = (idx2 > 0 && idx2 < allTeams.size()) ? allTeams.get(idx2) : "TBD";

                // Skip placeholder "Select Team" entries
                if ("Select Team".equals(t1) && "Select Team".equals(t2)) continue;
                if (idx1 == 0 && idx2 == 0) continue;

                matchCounter++;

                // Inflate the match card
                View view = LayoutInflater.from(this).inflate(R.layout.item_match_view, mainContainer, false);

                TextView tvDate   = view.findViewById(R.id.tvMatchDate);
                TextView tvTeam1  = view.findViewById(R.id.tvTeam1);
                TextView tvTeam2  = view.findViewById(R.id.tvTeam2);
                TextView tvNum    = view.findViewById(R.id.tvMatchNumber);

                if (tvDate  != null) tvDate.setText(date.isEmpty() ? "TBC" : date);
                if (tvTeam1 != null) tvTeam1.setText(t1);
                if (tvTeam2 != null) tvTeam2.setText(t2);
                if (tvNum   != null) tvNum.setText("Match #" + matchCounter);

                mainContainer.addView(view);
                count++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (count == 0) addNoMatchesPlaceholder();
    }

    private void addNoMatchesPlaceholder() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F8FAFC"));
        bg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        bg.setCornerRadius(dp(10));
        box.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        box.setLayoutParams(lp);

        TextView tv = new TextView(this);
        tv.setText("— No matches scheduled yet —");
        tv.setTextSize(12);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTypeface(null, Typeface.ITALIC);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(tv);
        mainContainer.addView(box);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TEAM LOADING HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    private void loadTeamsFromDynamicGroups(JSONArray groups) throws Exception {
        if (groups == null) return;
        for (int g = 0; g < groups.length(); g++) {
            JSONArray tArr = groups.getJSONObject(g).optJSONArray("teams");
            if (tArr == null) continue;
            for (int j = 0; j < tArr.length(); j++) allTeams.add(tArr.getString(j));
        }
    }

    private void loadTeamsIntoList(JSONArray arr) throws Exception {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) allTeams.add(arr.getString(i));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PDF GENERATION
    // ═══════════════════════════════════════════════════════════════════════
    private void createPdf() {
        if (savedData == null) {
            Toast.makeText(this, "No data to export!", Toast.LENGTH_SHORT).show();
            return;
        }
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Header
        paint.setColor(Color.parseColor("#1B5E20"));
        canvas.drawRect(0, 0, 595, 80, paint);

        paint.setColor(Color.parseColor("#A5D6A7"));
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("🏆  FIXTURES", 297, 25, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(tournamentName, 297, 58, paint);

        int y = 104;
        try {
            y = drawSectionToPdf(canvas, paint, "GROUP STAGE",   savedData.optJSONArray("MatchesGroup"), y);
            y = drawSectionToPdf(canvas, paint, "QUARTER FINALS",savedData.optJSONArray("MatchesQF"),    y);
            y = drawSectionToPdf(canvas, paint, "SEMI FINALS",   savedData.optJSONArray("MatchesSF"),    y);
            y = drawSectionToPdf(canvas, paint, "FINAL MATCH",   savedData.optJSONArray("MatchesFinal"), y);
        } catch (Exception e) {
            e.printStackTrace();
        }

        paint.setColor(Color.parseColor("#90A4AE"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Generated by Cricket ScoreZ Pro", 297, 820, paint);

        document.finishPage(page);

        String safeName = tournamentName.replaceAll("[^a-zA-Z0-9_]", "_");
        String fileName = safeName + "_Fixtures_" + System.currentTimeMillis() + ".pdf";
        PdfManager.saveAndOpenPdf(this, document, fileName, "Fixtures Downloaded \u2705");
    }

    private int drawSectionToPdf(Canvas canvas, Paint paint, String title, JSONArray matches, int startY) throws Exception {
        paint.setColor(Color.parseColor("#2E7D32"));
        canvas.drawRect(20, startY, 575, startY + 26, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("  " + title, 25, startY + 18, paint);
        startY += 34;

        if (matches == null || matches.length() == 0) {
            paint.setColor(Color.GRAY);
            paint.setTextSize(11);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
            canvas.drawText("  No matches scheduled.", 30, startY + 14, paint);
            return startY + 30;
        }

        boolean alt = false;
        int num = 0;
        for (int i = 0; i < matches.length(); i++) {
            JSONObject match = matches.getJSONObject(i);
            int idx1 = match.optInt("team1_idx", 0);
            int idx2 = match.optInt("team2_idx", 0);
            if (idx1 == 0 && idx2 == 0) continue;
            String date = match.optString("date", "TBC");
            String t1 = (idx1 > 0 && idx1 < allTeams.size()) ? allTeams.get(idx1) : "TBD";
            String t2 = (idx2 > 0 && idx2 < allTeams.size()) ? allTeams.get(idx2) : "TBD";
            num++;

            paint.setColor(alt ? Color.parseColor("#F1F8E9") : Color.parseColor("#FAFAFA"));
            canvas.drawRect(20, startY - 14, 575, startY + 14, paint);
            alt = !alt;

            paint.setColor(Color.parseColor("#4CAF50"));
            canvas.drawCircle(36, startY - 1, 10, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(9);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(String.valueOf(num), 36, startY + 3, paint);

            paint.setColor(Color.parseColor("#78909C"));
            paint.setTextSize(10);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(date.isEmpty() ? "TBC" : date, 55, startY, paint);

            paint.setColor(Color.parseColor("#1A237E"));
            paint.setTextSize(12);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(t1, 140, startY, paint);

            paint.setColor(Color.parseColor("#C62828"));
            paint.setTextSize(10);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("VS", 360, startY, paint);

            paint.setColor(Color.parseColor("#1A237E"));
            paint.setTextSize(12);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(t2, 385, startY, paint);

            paint.setColor(Color.parseColor("#E8F5E9"));
            paint.setStrokeWidth(1);
            canvas.drawLine(20, startY + 14, 575, startY + 14, paint);
            startY += 30;
        }
        return startY + 10;
    }

    private void showDownloadNotification(String fileName) {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            String channelId = "pdf_channel";
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(channelId, "PDF Downloads", NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(ch);
            }
            Notification.Builder builder = (Build.VERSION.SDK_INT >= 26)
                ? new Notification.Builder(this, channelId)
                : new Notification.Builder(this);
            builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                   .setContentTitle("Fixtures Downloaded ✅")
                   .setContentText(fileName + " saved to Downloads")
                   .setAutoCancel(true);
            nm.notify((int) System.currentTimeMillis(), builder.build());
        } catch (Exception ignored) {}
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }
}
