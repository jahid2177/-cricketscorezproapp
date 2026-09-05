package com.cricketscorez.proapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.cricketscorez.proapp.room.FavoriteMatchRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class ScorecardActivity extends Activity {

    // Views
    TextView tvTossInfo, tvMatchResult, tvMatchEquation, tvRequiredRate;
    LinearLayout layoutMatchEquation;
    TextView tvInn1Title, tvInn2Title, tvInn1Score, tvInn2Score;
    TextView tvExtras1, tvFOW1, tvExtras2, tvFOW2;
    LinearLayout layoutBatting1, layoutBowling1, layoutBatting2, layoutBowling2,
                 layoutFOW1, layoutFOW2;

    // Win Probability Views
    TextView tvTeam1WinProb, tvTeam2WinProb, tvWinProbDetails;
    ProgressBar progressBarWinProb;

    // Buttons
    TextView  btnOversDetail;
    LinearLayout btnPdfDownload;

    // Data
    MatchData matchData;
    String team1, team2;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scorecard);

        // View binding
        tvTossInfo          = findViewById(R.id.tvTossInfo);
        tvMatchResult       = findViewById(R.id.tvMatchResult);
        tvMatchEquation     = findViewById(R.id.tvMatchEquation);
        tvRequiredRate      = findViewById(R.id.tvRequiredRate);
        layoutMatchEquation = findViewById(R.id.layoutMatchEquation);
        tvInn1Title         = findViewById(R.id.tvInn1Title);
        tvInn2Title         = findViewById(R.id.tvInn2Title);
        tvInn1Score         = findViewById(R.id.tvInn1Score);
        tvInn2Score         = findViewById(R.id.tvInn2Score);
        tvExtras1           = findViewById(R.id.tvExtras1);
        tvFOW1              = findViewById(R.id.tvFOW1);
        tvExtras2           = findViewById(R.id.tvExtras2);
        tvFOW2              = findViewById(R.id.tvFOW2);
        layoutBatting1      = findViewById(R.id.layoutBatting1);
        layoutBowling1      = findViewById(R.id.layoutBowling1);
        layoutFOW1          = findViewById(R.id.layoutFOW1);
        layoutBatting2      = findViewById(R.id.layoutBatting2);
        layoutBowling2      = findViewById(R.id.layoutBowling2);
        layoutFOW2          = findViewById(R.id.layoutFOW2);
        btnOversDetail      = findViewById(R.id.btnOversDetail);
        btnPdfDownload      = findViewById(R.id.btnPdfDownload);

        tvTeam1WinProb      = findViewById(R.id.tvTeam1WinProb);
        tvTeam2WinProb      = findViewById(R.id.tvTeam2WinProb);
        tvWinProbDetails    = findViewById(R.id.tvWinProbDetails);
        progressBarWinProb  = findViewById(R.id.progressBarWinProb);

        ImageView btnBack = findViewById(R.id.btnBackScorecard);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { finish(); }
            });
        }

        // Data
        matchData = (MatchData) getIntent().getSerializableExtra("MATCH_DATA");
        team1     = getIntent().getStringExtra("TEAM_1");
        team2     = getIntent().getStringExtra("TEAM_2");

        if (matchData != null) {
            if (team1 == null) team1 = matchData.team1Name;
            if (team2 == null) team2 = matchData.team2Name;

            tvMatchResult.setText(team1 + " vs " + team2);
            tvTossInfo.setText(matchData.tossMessage != null
                    ? matchData.tossMessage : "Full Match Scorecard");

            displayScorecardData();
            setupButtons();
            calculateAndDisplayWinProbability();

            // 🔥 BUG FIX: matchStatus এ "won"/"Tied"/"Completed" যেকোনোটা হলে result দেখাও
            String ms = matchData.matchStatus;
            if (ms != null && (ms.equals("Completed") || ms.contains("won") || ms.contains("Tied") || ms.contains("tied"))) {
                showFinalResult();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WIN PROBABILITY DYNAMIC CALCULATION & ANIMATION
    // ─────────────────────────────────────────────────────────────────────────
    private void calculateAndDisplayWinProbability() {
        if (matchData == null || progressBarWinProb == null) return;

        String t1Name = matchData.teamBattingFirst != null ? matchData.teamBattingFirst : (team1 != null ? team1 : "Team 1");
        String t2Name = matchData.teamBattingSecond != null ? matchData.teamBattingSecond : (team2 != null ? team2 : "Team 2");

        int team1WinPct = 50;
        int team2WinPct = 50;
        String explanation = "Equal chances based on current match conditions.";

        String status = matchData.matchStatus != null ? matchData.matchStatus : "";

        if (status.contains("won by") || status.equalsIgnoreCase("Completed")) {
            if (status.contains(t1Name)) {
                team1WinPct = 100;
                team2WinPct = 0;
                explanation = t1Name + " has won the match.";
            } else if (status.contains(t2Name)) {
                team1WinPct = 0;
                team2WinPct = 100;
                explanation = t2Name + " has won the match.";
            }
        } else if (matchData.isSecondInnings) {
            int runsInn1 = 0;
            try {
                if (matchData.scoreInn1 != null) {
                    runsInn1 = Integer.parseInt(matchData.scoreInn1.split("/")[0].trim());
                }
            } catch (Exception ignored) {}

            int target = runsInn1 + 1;
            int currentRuns2 = matchData.totalRuns;
            int wickets2 = matchData.totalWickets;
            int totalOvers = 20;
            try {
                if (matchData.totalOvers != null) totalOvers = Integer.parseInt(matchData.totalOvers);
            } catch (Exception ignored) {}

            int ballsCompleted = matchData.currentOvers * 6 + matchData.currentBalls;
            int totalBalls = totalOvers * 6;
            int ballsLeft = Math.max(0, totalBalls - ballsCompleted);
            int runsNeeded = target - currentRuns2;
            int maxWickets = 10;
            int wicketsLeft = Math.max(0, maxWickets - wickets2);

            if (runsNeeded <= 0) {
                team2WinPct = 100;
                team1WinPct = 0;
                explanation = t2Name + " has reached the target!";
            } else if (wicketsLeft <= 0 || ballsLeft <= 0) {
                team1WinPct = 100;
                team2WinPct = 0;
                explanation = t1Name + " successfully defended the total.";
            } else {
                double rrr = (runsNeeded * 6.0) / ballsLeft;
                double crr = ballsCompleted > 0 ? (currentRuns2 * 6.0) / ballsCompleted : 6.0;

                // Win predictor heuristic formula
                double scoreFactor = 50.0 + (crr - rrr) * 8.5 + (wicketsLeft - (maxWickets / 2.0)) * 5.0;
                team2WinPct = (int) Math.round(Math.max(5, Math.min(95, scoreFactor)));
                team1WinPct = 100 - team2WinPct;

                explanation = t2Name + " needs " + runsNeeded + " runs in " + ballsLeft + " balls (RRR: "
                        + String.format("%.2f", rrr) + ", " + wicketsLeft + " wkts in hand)";
            }
        } else {
            // 1st Innings
            int currentRuns = matchData.totalRuns;
            int ballsCompleted = matchData.currentOvers * 6 + matchData.currentBalls;
            int totalOvers = 20;
            try {
                if (matchData.totalOvers != null) totalOvers = Integer.parseInt(matchData.totalOvers);
            } catch (Exception ignored) {}
            int totalBalls = totalOvers * 6;
            double crr = ballsCompleted > 0 ? (currentRuns * 6.0) / ballsCompleted : 6.0;
            int wickets = matchData.totalWickets;

            double expectedFinalScore = crr * totalOvers;
            double parScore = 8.0 * totalOvers; // standard par benchmark

            double t1Advantage = 50.0 + ((expectedFinalScore - parScore) / 2.0) - (wickets * 3.5);
            team1WinPct = (int) Math.round(Math.max(15, Math.min(85, t1Advantage)));
            team2WinPct = 100 - team1WinPct;

            explanation = t1Name + " projecting " + (int)Math.round(expectedFinalScore) + " runs at current run rate ("
                    + String.format("%.2f", crr) + " rpo)";
        }

        if (tvTeam1WinProb != null) {
            tvTeam1WinProb.setText(t1Name + ": " + team1WinPct + "%");
        }
        if (tvTeam2WinProb != null) {
            tvTeam2WinProb.setText(t2Name + ": " + team2WinPct + "%");
        }
        if (tvWinProbDetails != null) {
            tvWinProbDetails.setText(explanation);
        }

        // Animate the Progress Bar smoothly
        progressBarWinProb.setProgress(0);
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBarWinProb, "progress", 0, team1WinPct);
        animator.setDuration(1100);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void displayScorecardData() {
        // ✅ FIX: কোন টিম এখনো ব্যাট করছে সেটা ইনিংস হেডারে 🏏 দিয়ে বোঝানো —
        // ম্যাচ শেষ হয়ে গেলে (matchStatus-এ won/tied/completed থাকলে) কাউকে
        // "ব্যাটিং করছে" দেখানো হবে না।
        String statusLower = matchData.matchStatus != null ? matchData.matchStatus.toLowerCase(java.util.Locale.ROOT) : "";
        boolean matchOngoing = !(statusLower.contains("won") || statusLower.contains("tied")
                || statusLower.contains("completed") || statusLower.contains("abandon"));
        String battingBadge = "🏏 ";

        if (matchData.isSecondInnings) {
            tvInn1Title.setText(matchData.teamBattingFirst);
            tvInn1Score.setText(matchData.scoreInn1 + " (" + matchData.oversInn1 + ")");
            tvExtras1.setText(matchData.extrasInn1);
            fillTable(layoutBatting1, matchData.batsmanHistoryInn1, true);
            fillTable(layoutBowling1, matchData.bowlerHistoryInn1, false);
            fillFOW(layoutFOW1, matchData.fallOfWicketsInn1);

            tvInn2Title.setText((matchOngoing ? battingBadge : "") + matchData.teamBattingSecond);
            tvInn2Score.setText(matchData.getScoreString()
                    + " (" + matchData.getOversString() + ")");
            tvExtras2.setText(matchData.getExtrasString());
            fillTable(layoutBatting2, matchData.getAllBattingStats(), true);
            fillTable(layoutBowling2, matchData.getAllBowlingStats(), false);
            fillFOW(layoutFOW2, matchData.fallOfWickets);

            updateMatchEquation();
        } else {
            tvInn1Title.setText((matchOngoing ? battingBadge : "") + matchData.teamBattingFirst);
            tvInn1Score.setText(matchData.getScoreString()
                    + " (" + matchData.getOversString() + ")");
            tvExtras1.setText(matchData.getExtrasString());
            fillTable(layoutBatting1, matchData.getAllBattingStats(), true);
            fillTable(layoutBowling1, matchData.getAllBowlingStats(), false);
            fillFOW(layoutFOW1, matchData.fallOfWickets);

            tvInn2Title.setText(matchData.teamBattingSecond + " Innings");
            tvInn2Score.setText("Yet to bat");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  fillTable — batting row এ dismissal details ছবির মত দেখাবে
    // ─────────────────────────────────────────────────────────────────────────
    private void fillTable(LinearLayout parent, ArrayList<String[]> data, boolean isBatting) {
        if (parent == null || data == null) return;
        parent.removeAllViews();

        for (String[] rowData : data) {
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(10, 12, 10, 6);

            if (isBatting) {
                // ── Batting: name + dismissal column ─────────────────────
                LinearLayout nameCol = new LinearLayout(this);
                nameCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams nameColLp =
                        new LinearLayout.LayoutParams(0, -2, 4f);
                nameCol.setLayoutParams(nameColLp);

                // Split name and dismissal (stored as "Name\nDismissal")
                String[] parts = (rowData[0] != null ? rowData[0] : "").split("\n", 2);
                String playerName  = parts[0].trim();
                String dismissal   = parts.length > 1 ? parts[1].trim() : "";

                // Player name — bold, black
                TextView tvName = new TextView(this);
                tvName.setText(playerName);
                tvName.setTextSize(13);
                tvName.setTypeface(null, Typeface.BOLD);
                tvName.setTextColor(Color.parseColor("#0D1F0E"));
                nameCol.addView(tvName);

                // Dismissal detail — italic, grey (like the image: "Catch Out b Piyash")
                if (!dismissal.isEmpty() && !dismissal.equalsIgnoreCase("not out")
                        && !dismissal.equalsIgnoreCase("batting")) {
                    TextView tvDismissal = new TextView(this);
                    tvDismissal.setText(dismissal);
                    tvDismissal.setTextSize(11);
                    tvDismissal.setTypeface(null, Typeface.ITALIC);
                    tvDismissal.setTextColor(Color.parseColor("#78909C"));
                    tvDismissal.setPadding(0, 2, 0, 0);
                    nameCol.addView(tvDismissal);
                } else if (dismissal.equalsIgnoreCase("not out")) {
                    // "not out" — green italic
                    TextView tvNotOut = new TextView(this);
                    tvNotOut.setText("not out");
                    tvNotOut.setTextSize(11);
                    tvNotOut.setTypeface(null, Typeface.ITALIC);
                    tvNotOut.setTextColor(Color.parseColor("#388E3C"));
                    tvNotOut.setPadding(0, 2, 0, 0);
                    nameCol.addView(tvNotOut);
                }

                row.addView(nameCol);

                // Stats columns
                row.addView(makeStatTV(rowData[1], 1f, Color.parseColor("#212121")));
                row.addView(makeStatTV(rowData[2], 1f, Color.parseColor("#546E7A")));
                row.addView(makeStatTV(rowData[3], 1f, Color.parseColor("#546E7A")));
                row.addView(makeStatTV(rowData[4], 1f, Color.parseColor("#546E7A")));
                row.addView(makeStatTV(rowData[5], 1.5f, Color.parseColor("#546E7A")));

            } else {
                // ── Bowling row (name left-aligned) ──────────────────────────────────
                TextView tvBowlerName = new TextView(this);
                LinearLayout.LayoutParams bowlerNameLp =
                        new LinearLayout.LayoutParams(0, -2, 4f);
                tvBowlerName.setLayoutParams(bowlerNameLp);
                tvBowlerName.setText(rowData[0] != null ? rowData[0] : "");
                tvBowlerName.setTextColor(Color.parseColor("#0D1F0E"));
                tvBowlerName.setTextSize(13);
                tvBowlerName.setTypeface(null, Typeface.BOLD);
                tvBowlerName.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                tvBowlerName.setPadding(4, 0, 0, 0);
                row.addView(tvBowlerName);
                row.addView(makeStatTV(rowData[1], 1f, Color.parseColor("#212121")));
                row.addView(makeStatTV(rowData[2], 1f, Color.parseColor("#546E7A")));
                row.addView(makeStatTV(rowData[3], 1f, Color.parseColor("#546E7A")));
                row.addView(makeStatTV(rowData[4], 1f, Color.parseColor("#C62828")));
                row.addView(makeStatTV(rowData[5], 1.5f, Color.parseColor("#00796B")));
            }

            container.addView(row);

            // Divider
            View line = new View(this);
            line.setLayoutParams(new LinearLayout.LayoutParams(-1, 1));
            line.setBackgroundColor(Color.parseColor("#E0E0E0"));
            container.addView(line);

            parent.addView(container);
        }
    }

    private TextView makeStatTV(String text, float weight, int color) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, -2, weight));
        tv.setText(text != null ? text : "");
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(12);
        return tv;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void fillFOW(LinearLayout parent, ArrayList<String> fowList) {
        if (parent == null || fowList == null) return;
        parent.removeAllViews();
        for (String fow : fowList) {
            TextView tv = new TextView(this);
            tv.setText(fow);
            tv.setTextColor(Color.DKGRAY);
            tv.setTextSize(11);
            tv.setPadding(10, 5, 10, 5);
            parent.addView(tv);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void updateMatchEquation() {
        if (layoutMatchEquation == null || !matchData.isSecondInnings) {
            if (layoutMatchEquation != null) layoutMatchEquation.setVisibility(View.GONE);
            return;
        }

        int runsNeeded     = matchData.targetRuns - matchData.totalRuns;
        int totalBalls     = Integer.parseInt(matchData.totalOvers) * 6;
        int ballsRemaining = totalBalls - matchData.ballsBowled;

        if (runsNeeded > 0 && ballsRemaining > 0) {
            // Required Run Rate
            double rrr = (ballsRemaining > 0) ? (runsNeeded * 6.0 / ballsRemaining) : 0;

            layoutMatchEquation.setVisibility(View.VISIBLE);
            tvMatchEquation.setText(runsNeeded + " runs needed in " + ballsRemaining + " balls");
            tvRequiredRate.setText(String.format("Required Run Rate: %.2f  |  Target: %d", rrr, matchData.targetRuns));
        } else if (runsNeeded <= 0) {
            // 2nd innings team জিতে গেছে
            layoutMatchEquation.setVisibility(View.VISIBLE);
            tvMatchEquation.setText(matchData.teamBattingSecond + " won! 🏆");
            tvRequiredRate.setText("Target: " + matchData.targetRuns + " achieved");
        } else {
            layoutMatchEquation.setVisibility(View.GONE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void showFinalResult() {
        // 🔥 BUG FIX: matchStatus এই সরাসরি result message থাকে (e.g. "cvv won by 6 wickets!")
        String resultText = matchData.matchStatus;
        TextView tvResult = new TextView(this);
        tvResult.setText("Result: " + resultText);
        tvResult.setGravity(Gravity.CENTER);
        tvResult.setTextSize(16);
        tvResult.setTextColor(Color.RED);
        tvResult.setTypeface(null, Typeface.BOLD);
        tvResult.setPadding(0, 30, 0, 50);
        LinearLayout parentLayout = (LinearLayout) layoutFOW2.getParent();
        if (parentLayout != null) parentLayout.addView(tvResult);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void setupButtons() {
        if (btnOversDetail != null) {
            btnOversDetail.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Intent intent = new Intent(ScorecardActivity.this,
                            OversDetailActivity.class);
                    intent.putExtra("MATCH_DATA", matchData);
                    startActivity(intent);
                }
            });
        }

        if (btnPdfDownload != null) {
            btnPdfDownload.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                            return;
                        }
                    }
                    generateProfessionalPdf();
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PDF Generation
    // ─────────────────────────────────────────────────────────────────────────
    private void generateProfessionalPdf() {
        SharedPreferences prefs = getSharedPreferences("TournamentSettings", MODE_PRIVATE);
        String tournamentName = prefs.getString("TOURNAMENT_NAME", "Cricket Tournament");

        PdfDocument document = new PdfDocument();
        drawInningsPage(document, 1, tournamentName);
        if (matchData.isSecondInnings) {
            drawInningsPage(document, 2, tournamentName);
        }

        String safeTName = tournamentName.replaceAll("\\s+", "_");
        String fileName  = safeTName + "_Scorecard_" + System.currentTimeMillis() + ".pdf";
        PdfManager.saveAndOpenPdf(this, document, fileName);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Draw one innings page — styled like the scorecard screenshot
    // ─────────────────────────────────────────────────────────────────────────
    private void drawInningsPage(PdfDocument document, int inningsNum, String tournamentName) {

        final int PAGE_W = 595;
        final int PAGE_H = 842;
        final int LEFT   = 20;
        final int RIGHT  = PAGE_W - 20;

        // Column X positions (match screenshot layout)
        final int COL_NAME   = LEFT + 10;
        final int COL_R      = 320;
        final int COL_B      = 370;
        final int COL_4S     = 420;
        final int COL_6S     = 470;
        final int COL_SR     = 530;
        final int COL_O      = 320;
        final int COL_M      = 370;
        final int COL_RUNS   = 420;
        final int COL_W      = 470;
        final int COL_ER     = 530;

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, inningsNum).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint p      = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint pBold  = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint pItal  = new Paint(Paint.ANTI_ALIAS_FLAG);

        pBold.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        pItal.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));

        int y = 0;

        // ── 1. Header gradient bar ────────────────────────────────────────
        p.setColor(Color.parseColor("#1565C0")); // blue like screenshot
        canvas.drawRect(0, y, PAGE_W, y + 60, p);

        pBold.setColor(Color.WHITE);
        pBold.setTextSize(20);
        pBold.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(tournamentName, PAGE_W / 2f, y + 28, pBold);

        p.setColor(Color.parseColor("#BBDEFB"));
        p.setTextSize(12);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(matchData.team1Name + " vs " + matchData.team2Name,
                PAGE_W / 2f, y + 48, p);

        y += 70;

        // ── 2. Toss info bar ─────────────────────────────────────────────
        p.setColor(Color.parseColor("#E3F2FD"));
        canvas.drawRect(LEFT, y, RIGHT, y + 26, p);

        p.setColor(Color.parseColor("#1565C0"));
        p.setTextSize(11);
        p.setTextAlign(Paint.Align.CENTER);
        String toss = (matchData.tossMessage != null && !matchData.tossMessage.isEmpty())
                ? matchData.tossMessage : "Toss info not available";
        canvas.drawText("🏏 " + toss, PAGE_W / 2f, y + 17, p);

        y += 36;

        // ── 3. Innings data ───────────────────────────────────────────────
        String teamName, score, overs, extrasStr;
        ArrayList<String[]> batsmen, bowlers;
        ArrayList<String> fowList;

        if (inningsNum == 1) {
            teamName  = matchData.teamBattingFirst;
            score     = matchData.isSecondInnings ? matchData.scoreInn1 : matchData.getScoreString();
            overs     = matchData.isSecondInnings ? matchData.oversInn1 : matchData.getOversString();
            extrasStr = matchData.isSecondInnings ? matchData.extrasInn1 : matchData.getExtrasString();
            batsmen   = matchData.isSecondInnings ? matchData.batsmanHistoryInn1 : matchData.getAllBattingStats();
            bowlers   = matchData.isSecondInnings ? matchData.bowlerHistoryInn1  : matchData.getAllBowlingStats();
            fowList   = matchData.isSecondInnings ? matchData.fallOfWicketsInn1  : matchData.fallOfWickets;
        } else {
            teamName  = matchData.teamBattingSecond;
            score     = matchData.getScoreString();
            overs     = matchData.getOversString();
            extrasStr = matchData.getExtrasString();
            batsmen   = matchData.getAllBattingStats();
            bowlers   = matchData.getAllBowlingStats();
            fowList   = matchData.fallOfWickets;
        }

        // ── 4. Team score header ──────────────────────────────────────────
        p.setTextAlign(Paint.Align.LEFT);
        p.setColor(Color.parseColor("#F5F5F5"));
        canvas.drawRect(LEFT, y, RIGHT, y + 30, p);

        pBold.setColor(Color.parseColor("#0D1F0E"));
        pBold.setTextSize(13);
        pBold.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(teamName + "  -  " + score + " (" + overs + ")", COL_NAME, y + 21, pBold);

        y += 38;

        // ── 5. Batting section label ──────────────────────────────────────
        p.setColor(Color.parseColor("#E8F5E9"));
        canvas.drawRect(LEFT, y, RIGHT, y + 22, p);

        pBold.setColor(Color.parseColor("#2E7D32"));
        pBold.setTextSize(11);
        canvas.drawText("Batting", COL_NAME, y + 15, pBold);

        y += 28;

        // ── 6. Batting table header ───────────────────────────────────────
        p.setColor(Color.parseColor("#ECEFF1"));
        canvas.drawRect(LEFT, y, RIGHT, y + 20, p);

        pBold.setColor(Color.parseColor("#546E7A"));
        pBold.setTextSize(10);
        pBold.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Batsman", COL_NAME, y + 14, pBold);
        drawColHeader(canvas, pBold, "R",  COL_R,  y + 14);
        drawColHeader(canvas, pBold, "B",  COL_B,  y + 14);
        drawColHeader(canvas, pBold, "4s", COL_4S, y + 14);
        drawColHeader(canvas, pBold, "6s", COL_6S, y + 14);
        drawColHeader(canvas, pBold, "SR", COL_SR, y + 14);

        y += 22;
        drawDivider(canvas, p, LEFT, RIGHT, y);
        y += 2;

        // ── 7. Batting rows ───────────────────────────────────────────────
        p.setTextAlign(Paint.Align.LEFT);
        for (String[] bat : batsmen) {
            String raw = bat[0] != null ? bat[0] : "";
            String[] parts  = raw.split("\n", 2);
            String batName  = parts[0].trim();
            String dismissal = parts.length > 1 ? parts[1].trim() : "";

            boolean hasDetail = !dismissal.isEmpty()
                    && !dismissal.equalsIgnoreCase("not out")
                    && !dismissal.equalsIgnoreCase("batting");
            boolean isNotOut = dismissal.equalsIgnoreCase("not out");

            int rowH = (hasDetail || isNotOut) ? 34 : 22;

            // Zebra stripe
            p.setColor(Color.parseColor("#FAFAFA"));
            canvas.drawRect(LEFT, y, RIGHT, y + rowH, p);

            // Player name — bold
            pBold.setColor(Color.parseColor("#0D1F0E"));
            pBold.setTextSize(11);
            pBold.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(batName, COL_NAME, y + 14, pBold);

            // Dismissal detail — italic grey (like image: "Catch Out b Piyash")
            if (hasDetail) {
                pItal.setColor(Color.parseColor("#78909C"));
                pItal.setTextSize(9);
                pItal.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(dismissal, COL_NAME, y + 26, pItal);
            } else if (isNotOut) {
                pItal.setColor(Color.parseColor("#388E3C"));
                pItal.setTextSize(9);
                pItal.setTextAlign(Paint.Align.LEFT);
                canvas.drawText("not out", COL_NAME, y + 26, pItal);
            }

            // Stats — centered above mid of each column
            p.setColor(Color.parseColor("#212121"));
            p.setTextSize(11);
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(safe(bat[1]), COL_R,  y + 14, p);
            canvas.drawText(safe(bat[2]), COL_B,  y + 14, p);
            canvas.drawText(safe(bat[3]), COL_4S, y + 14, p);
            canvas.drawText(safe(bat[4]), COL_6S, y + 14, p);
            canvas.drawText(safe(bat[5]), COL_SR, y + 14, p);

            y += rowH;
            drawDivider(canvas, p, LEFT, RIGHT, y);
            y += 2;
        }

        // ── 8. Extras ─────────────────────────────────────────────────────
        y += 6;
        pBold.setTextSize(10);
        pBold.setColor(Color.parseColor("#37474F"));
        pBold.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Extras  " + safe(extrasStr), COL_NAME, y, pBold);
        y += 18;

        // ── 9. Bowling section label ──────────────────────────────────────
        p.setColor(Color.parseColor("#E8F5E9"));
        canvas.drawRect(LEFT, y, RIGHT, y + 22, p);

        pBold.setColor(Color.parseColor("#2E7D32"));
        pBold.setTextSize(11);
        canvas.drawText("Bowling", COL_NAME, y + 15, pBold);

        y += 28;

        // ── 10. Bowling table header ──────────────────────────────────────
        p.setColor(Color.parseColor("#ECEFF1"));
        canvas.drawRect(LEFT, y, RIGHT, y + 20, p);

        pBold.setColor(Color.parseColor("#546E7A"));
        pBold.setTextSize(10);
        canvas.drawText("Bowler", COL_NAME, y + 14, pBold);
        drawColHeader(canvas, pBold, "O",  COL_O,    y + 14);
        drawColHeader(canvas, pBold, "M",  COL_M,    y + 14);
        drawColHeader(canvas, pBold, "R",  COL_RUNS, y + 14);
        drawColHeader(canvas, pBold, "W",  COL_W,    y + 14);
        drawColHeader(canvas, pBold, "ER", COL_ER,   y + 14);

        y += 22;
        drawDivider(canvas, p, LEFT, RIGHT, y);
        y += 2;

        // ── 11. Bowling rows ──────────────────────────────────────────────
        for (String[] bowl : bowlers) {
            p.setColor(Color.parseColor("#FAFAFA"));
            canvas.drawRect(LEFT, y, RIGHT, y + 22, p);

            pBold.setColor(Color.parseColor("#0D1F0E"));
            pBold.setTextSize(11);
            pBold.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(safe(bowl[0]), COL_NAME, y + 15, pBold);

            p.setColor(Color.parseColor("#212121"));
            p.setTextSize(11);
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(safe(bowl[1]), COL_O,    y + 15, p);
            canvas.drawText(safe(bowl[2]), COL_M,    y + 15, p);
            canvas.drawText(safe(bowl[3]), COL_RUNS, y + 15, p);

            // Wickets in red
            p.setColor(Color.parseColor("#C62828"));
            canvas.drawText(safe(bowl[4]), COL_W, y + 15, p);

            // Economy in teal
            p.setColor(Color.parseColor("#00796B"));
            canvas.drawText(safe(bowl[5]), COL_ER, y + 15, p);

            y += 22;
            drawDivider(canvas, p, LEFT, RIGHT, y);
            y += 2;
        }

        // ── 12. Fall of Wickets ───────────────────────────────────────────
        if (fowList != null && !fowList.isEmpty()) {
            y += 10;
            p.setColor(Color.parseColor("#F5F5F5"));
            canvas.drawRect(LEFT, y, RIGHT, y + 20, p);

            pBold.setColor(Color.parseColor("#37474F"));
            pBold.setTextSize(10);
            pBold.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Fall of Wickets", COL_NAME, y + 14, pBold);
            y += 26;

            StringBuilder fowLine = new StringBuilder();
            for (int i = 0; i < fowList.size(); i++) {
                if (i > 0) fowLine.append("  |  ");
                fowLine.append(fowList.get(i));
            }

            // Word-wrap FOW text
            p.setColor(Color.parseColor("#C62828"));
            p.setTextSize(9);
            p.setTextAlign(Paint.Align.LEFT);
            String[] fowWords = fowLine.toString().split("  \\|  ");
            StringBuilder line = new StringBuilder();
            for (String w : fowWords) {
                if (p.measureText(line + "  |  " + w) > (RIGHT - COL_NAME - 10)) {
                    canvas.drawText(line.toString(), COL_NAME, y, p);
                    y += 14;
                    line = new StringBuilder(w);
                } else {
                    if (line.length() > 0) line.append("  |  ");
                    line.append(w);
                }
            }
            if (line.length() > 0) {
                canvas.drawText(line.toString(), COL_NAME, y, p);
                y += 14;
            }
        }

        // ── 13. Match equation / result (2nd innings only) ────────────────
        if (inningsNum == 2) {
            y += 16;
            if (matchData.isSecondInnings) {
                int runs  = matchData.targetRuns - matchData.totalRuns;
                int balls = (Integer.parseInt(matchData.totalOvers) * 6)
                        - matchData.ballsBowled;
                if (runs > 0 && balls > 0) {
                    p.setColor(Color.parseColor("#1565C0"));
                    p.setTextSize(11);
                    p.setTextAlign(Paint.Align.LEFT);
                    canvas.drawText("Equation: Need " + runs + " runs in " + balls + " balls",
                            COL_NAME, y, p);
                    y += 20;
                }
            }
            // 🔥 BUG FIX: matchStatus এই result message থাকে
            String ms2 = matchData.matchStatus;
            if (ms2 != null && (ms2.equals("Completed") || ms2.contains("won") || ms2.contains("Tied") || ms2.contains("tied"))) {
                pBold.setColor(Color.parseColor("#B71C1C"));
                pBold.setTextSize(14);
                pBold.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Result: " + ms2, PAGE_W / 2f, y + 10, pBold);
            }
        }

        document.finishPage(page);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private void drawColHeader(Canvas canvas, Paint p, String text, int x, int y) {
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, x, y, p);
        p.setTextAlign(Paint.Align.LEFT);
    }

    private void drawDivider(Canvas canvas, Paint p, int left, int right, int y) {
        p.setColor(Color.parseColor("#E0E0E0"));
        p.setStrokeWidth(0.5f);
        canvas.drawLine(left, y, right, y, p);
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void showPdfNotification(String fileName, File pdfFile) {
        try {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            String channelId = "pdf_channel";
            if (Build.VERSION.SDK_INT >= 26) {
                android.app.NotificationChannel ch = new android.app.NotificationChannel(
                        channelId, "PDF Downloads",
                        android.app.NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(ch);
            }

            // 🔥 BUG FIX: notification click এ PDF file open করার Intent
            android.net.Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    pdfFile);
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(pdfUri, "application/pdf");
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, openIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                    (Build.VERSION.SDK_INT >= 23 ? android.app.PendingIntent.FLAG_IMMUTABLE : 0));

            android.app.Notification.Builder builder = (Build.VERSION.SDK_INT >= 26)
                    ? new android.app.Notification.Builder(this, channelId)
                    : new android.app.Notification.Builder(this);
            builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                   .setContentTitle("Scorecard Downloaded ✅")
                   .setContentText(fileName + " — tap to open")
                   .setContentIntent(pendingIntent)
                   .setAutoCancel(true);
            nm.notify((int) System.currentTimeMillis(), builder.build());
        } catch (Exception ignored) {}
    }
}