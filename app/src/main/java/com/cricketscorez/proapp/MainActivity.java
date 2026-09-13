package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.view.Window;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import com.cricketscorez.proapp.api.ApiClient;
import com.cricketscorez.proapp.api.ApiInterface;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Map;

// 🔥 Live Score Firebase Manager
import com.cricketscorez.proapp.LiveScoreManager;

public class MainActivity extends Activity {

    // Views
    TextView tvMatchTitle, tvTotalScore, tvOvers, tvCRR, tvBattingTeam, tvRRR, tvMatchEquation;
    TextView tvStrikerName, tvNonStrikerName, tvBowlerName;
    TextView tvExtrasDetail, tvPartnership;
    LinearLayout layoutThisOver;

    // Stats Views
    TextView tvStrikerRuns, tvStrikerBalls, tvStriker4s, tvStriker6s, tvStrikerSR;
    TextView tvNonStrikerRuns, tvNonStrikerBalls, tvNonStriker4s, tvNonStriker6s, tvNonStrikerSR;
    TextView tvBowlerOvers, tvBowlerMaidens, tvBowlerRuns, tvBowlerWickets, tvBowlerER;

    // Buttons
    ImageView btnBack, btnMatchTools;
    Button btnUndo, btnSwap, btnPenalty, btnRetire, btnInjured;

    // UPDATED BUTTONS
    Button btnAnalysis, btnCompare, btnLiveCommentary, btnLiveScoreboard; 

    // Checkboxes
    CheckBox cbWide, cbNoBall, cbByes, cbLegByes, cbWicket;

    private Button[] runButtons = new Button[7];
    MatchData matchData;

    // Request Codes
    private static final int REQUEST_CODE_NEW_BOWLER = 1;
    private static final int REQUEST_CODE_NEW_BATSMAN = 2;
    private static final int REQUEST_CODE_WICKET_FALL = 3;
    private static final int REQUEST_CODE_BATSMAN_RETIRE = 4;
    private static final int REQUEST_CODE_BOWLER_INJURED = 5;
    private static final int REQUEST_CODE_SECOND_INNINGS = 10;
    private static final int REQUEST_CODE_TEST_NEXT_INNINGS = 11;

    // Data
    private String strikerName;
    private String nonStrikerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ThemeManager.applyStatusBar(this);

        // --- 1. Initialize Views ---
        btnBack = findViewById(R.id.btnBack);
        btnMatchTools = findViewById(R.id.btnMatchTools);
        btnUndo = findViewById(R.id.btnUndo);
        btnSwap = findViewById(R.id.btnSwap);
        btnPenalty = findViewById(R.id.btnPenalty);
        btnRetire = findViewById(R.id.btnRetire);
        btnInjured = findViewById(R.id.btnInjured);
        btnAnalysis = findViewById(R.id.btnAnalysis);
        btnCompare = findViewById(R.id.btnCompare);
        btnLiveCommentary = findViewById(R.id.btnLiveCommentary);
        btnLiveScoreboard = findViewById(R.id.btnLiveScoreboard); 

        layoutThisOver = findViewById(R.id.layoutThisOver);

        tvMatchTitle = findViewById(R.id.tvMatchTitle);
        tvBattingTeam = findViewById(R.id.tvBattingTeam);
        tvRRR = findViewById(R.id.tvRRR);
        tvMatchEquation = findViewById(R.id.tvMatchEquation);
        tvTotalScore = findViewById(R.id.tvTotalScore);
        tvOvers = findViewById(R.id.tvOvers);
        tvCRR = findViewById(R.id.tvCRR);
        tvStrikerName = findViewById(R.id.tvStrikerName);
        tvNonStrikerName = findViewById(R.id.tvNonStrikerName);
        tvBowlerName = findViewById(R.id.tvBowlerName);
        tvExtrasDetail = findViewById(R.id.tvExtrasDetail);
        tvPartnership = findViewById(R.id.tvPartnership);

        tvStrikerRuns = findViewById(R.id.tvStrikerRuns); tvStrikerBalls = findViewById(R.id.tvStrikerBalls); tvStriker4s = findViewById(R.id.tvStriker4s); tvStriker6s = findViewById(R.id.tvStriker6s); tvStrikerSR = findViewById(R.id.tvStrikerSR);
        tvNonStrikerRuns = findViewById(R.id.tvNonStrikerRuns); tvNonStrikerBalls = findViewById(R.id.tvNonStrikerBalls); tvNonStriker4s = findViewById(R.id.tvNonStriker4s); tvNonStriker6s = findViewById(R.id.tvNonStriker6s); tvNonStrikerSR = findViewById(R.id.tvNonStrikerSR);
        tvBowlerOvers = findViewById(R.id.tvBowlerOvers); tvBowlerMaidens = findViewById(R.id.tvBowlerMaidens); tvBowlerRuns = findViewById(R.id.tvBowlerRuns); tvBowlerWickets = findViewById(R.id.tvBowlerWickets); tvBowlerER = findViewById(R.id.tvBowlerER);

        cbWide = findViewById(R.id.cbWide); cbNoBall = findViewById(R.id.cbNoBall); cbByes = findViewById(R.id.cbByes); cbLegByes = findViewById(R.id.cbLegByes); cbWicket = findViewById(R.id.cbWicket);

        // ✅ Extra Checkboxes Mutual Exclusion: এক ধরনের Extra সিলেক্ট করলে অন্যগুলো অটো আনচেক হবে
        cbWide.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) { cbNoBall.setChecked(false); cbByes.setChecked(false); cbLegByes.setChecked(false); }
        });
        cbNoBall.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) { cbWide.setChecked(false); cbByes.setChecked(false); cbLegByes.setChecked(false); }
        });
        cbByes.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) { cbWide.setChecked(false); cbNoBall.setChecked(false); cbLegByes.setChecked(false); }
        });
        cbLegByes.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) { cbWide.setChecked(false); cbNoBall.setChecked(false); cbByes.setChecked(false); }
        });

        // ✅ FIX #1: AppSettings থেকে Keep Screen On ও Show Run Rate apply করা হচ্ছে।
        // AppSettingsActivity-তে ব্যবহারকারী যা সেভ করেছে, এখানে সেটি কার্যকর হবে।
        SharedPreferences appPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        if (appPrefs.getBoolean("KEEP_SCREEN_ON", false)) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        boolean showRunRate = appPrefs.getBoolean("SHOW_RUN_RATE", true);
        if (tvCRR != null) tvCRR.setVisibility(showRunRate ? View.VISIBLE : View.GONE);

        // --- MATCH SETUP ---
        if (getIntent().hasExtra("RESUME_MATCH_DATA")) {
            matchData = (MatchData) getIntent().getSerializableExtra("RESUME_MATCH_DATA");
            strikerName = matchData.strikerName;
            nonStrikerName = matchData.nonStrikerName;
            Toast.makeText(this, "Match Resumed!", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = getIntent();
            String team1 = intent.getStringExtra("TEAM_1");
            String team2 = intent.getStringExtra("TEAM_2");
            String totalOvers = intent.getStringExtra("TOTAL_OVERS");
            strikerName = intent.getStringExtra("STRIKER");
            nonStrikerName = intent.getStringExtra("NON_STRIKER");
            String bowler = intent.getStringExtra("BOWLER");

            matchData = new MatchData(team1, team2, totalOvers);
            matchData.strikerName = strikerName;
            matchData.nonStrikerName = nonStrikerName;
            matchData.switchBowler(intent.getStringExtra("BOWLER"));
            // 🔥 FIX: TOSS_INFO বা TOSS_WINNER+TOSS_DECISION দুটো থেকেই tossMessage সেট করা হচ্ছে
            String tossInfo = intent.getStringExtra("TOSS_INFO");
            if (tossInfo == null || tossInfo.isEmpty()) {
                String tossWinner   = intent.getStringExtra("TOSS_WINNER");
                String tossDecision = intent.getStringExtra("TOSS_DECISION");
                if (tossWinner != null && tossDecision != null) {
                    tossInfo = tossWinner + " won the toss and elected to " + tossDecision.toLowerCase() + " first";
                }
            }
            if (tossInfo != null && !tossInfo.isEmpty()) {
                matchData.tossMessage = tossInfo;
            }

            // ✅ FIX: PlayerSelectionActivity থেকে IS_TOURNAMENT intent পড়া হচ্ছে।
            // যদি সেখান থেকে সিদ্ধান্ত আসে (হ্যাঁ/না), তাহলে আর popup দেখাবে না।
            // যদি IS_TOURNAMENT extra না থাকে (পুরোনো flow), তাহলে নিজে চেক করবে।
            if (intent.hasExtra("IS_TOURNAMENT")) {
                boolean isTour = intent.getBooleanExtra("IS_TOURNAMENT", false);
                String tourMatchId = intent.getStringExtra("TOURNAMENT_MATCH_ID");
                if (isTour && tourMatchId != null && !tourMatchId.isEmpty()) {
                    matchData.isTournamentMatch = true;
                    matchData.tournamentMatchId = tourMatchId;
                    Toast.makeText(this,
                        "🏆 Tournament Match! Point Table & Rankings will auto-update.",
                        Toast.LENGTH_LONG).show();
                } else {
                    matchData.isTournamentMatch = false;
                }
            } else {
                // Intent-এ IS_TOURNAMENT নেই — নিজে fixture চেক করে popup দেখাবে
                checkForTournamentFixture();
            }

            if (intent.getBooleanExtra("IS_TEST_MATCH", false)) {
                matchData.isTestMatch = true;
                matchData.testDays = intent.getIntExtra("TEST_DAYS", 5);
                matchData.oversPerDay = intent.getIntExtra("TEST_OVERS_PER_DAY", 90);
                matchData.testFollowOnMargin = intent.getIntExtra("TEST_FOLLOW_ON_MARGIN", 200);
            }
        }
        tvMatchTitle.setText(matchData.team1Name + " v/s " + matchData.team2Name);
        tvStrikerName.setText(matchData.strikerName + " *");
        tvNonStrikerName.setText(matchData.nonStrikerName);
        tvBowlerName.setText(matchData.currentBowlerName);

        // 🔥 LIVE SCORE: Match শুরু হলে Firebase LiveScoreManager init করুন
        LiveScoreManager.getInstance().initMatch(matchData);

        updateScoreboardDisplay();
        setRunButtonListeners();

        // --- 3. Listeners ---
        btnUndo.setOnClickListener(v -> {
            // ✅ FIX: আগে ballHistory.isEmpty() চেক করা হতো, যা শুধুমাত্র
            // ম্যানুয়াল সোয়াপ (কোনো নতুন বল ছাড়া) undo করার প্রয়োজনটা
            // মিস করত। এখন নতুন snapshot-ভিত্তিক undo stack (canUndo())
            // দিয়ে চেক করা হচ্ছে, যা বোলিং/ব্যাটিং/এক্সট্রা/পার্টনারশিপ/
            // সোয়াপ — সব ধরনের undo-যোগ্য action ঠিকভাবে ধরে।
            if (!matchData.canUndo()) {
                Toast.makeText(MainActivity.this, "Nothing to undo.", Toast.LENGTH_SHORT).show();
                return;
            }
            showPremiumUndoDialog();
        });
        btnSwap.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { matchData.recordManualSwap(); swapBatsmenNames(); updateScoreboardDisplay(); } });
        btnPenalty.setOnClickListener(v -> showPremiumPenaltyDialog());
			
        btnBack.setOnClickListener(new View.OnClickListener() {
				@Override 
				public void onClick(View v) { 
					onBackPressed(); 
				} 
			});

        if (btnMatchTools != null) {
            btnMatchTools.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMatchToolsMenu();
                }
            });
        }

        btnLiveCommentary.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					Intent i = new Intent(MainActivity.this, LiveCommentaryActivity.class);
					i.putExtra("MATCH_DATA", matchData);
					startActivity(i);
				}
			});

        btnLiveScoreboard.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					Intent i = new Intent(MainActivity.this, ScorecardActivity.class);
					i.putExtra("MATCH_DATA", matchData);
					i.putExtra("TEAM_1", matchData.teamBattingFirst); 
					i.putExtra("TEAM_2", matchData.teamBattingSecond);
					startActivity(i);
				}
			});

        btnAnalysis.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					Intent i = new Intent(MainActivity.this, AnalysisActivity.class);
					i.putExtra("TEAM_1_NAME", matchData.teamBattingFirst);
					i.putExtra("TEAM_2_NAME", matchData.teamBattingSecond);
					i.putExtra("TOTAL_OVERS", matchData.totalOvers);

					if (!matchData.isSecondInnings) {
						i.putIntegerArrayListExtra("INN1_DATA", matchData.getCumulativeRunsPerOver());
					} else {
						i.putIntegerArrayListExtra("INN1_DATA", matchData.runRateInn1);
						i.putIntegerArrayListExtra("INN2_DATA", matchData.getCumulativeRunsPerOver());
					}
					startActivity(i);
				}
			});

		btnCompare.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (matchData.ballHistory.isEmpty() && !matchData.isSecondInnings) {
						Toast.makeText(MainActivity.this, "ম্যাচে পর্যাপ্ত ডাটা নেই!", Toast.LENGTH_SHORT).show();
						return;
					}

					Intent intent = new Intent(MainActivity.this, CompareActivity.class);
					intent.putExtra("TEAM_1", matchData.teamBattingFirst);
					intent.putExtra("TEAM_2", matchData.teamBattingSecond);
					intent.putExtra("TOTAL_OVERS", matchData.totalOvers); 

					if (!matchData.isSecondInnings) {
						intent.putIntegerArrayListExtra("INN1_DATA", matchData.getCumulativeRunsPerOver());
						intent.putIntegerArrayListExtra("INN1_WICKET_DATA", matchData.getCumulativeWicketsPerOver());
					} else {
						intent.putIntegerArrayListExtra("INN1_DATA", matchData.runRateInn1);
						intent.putIntegerArrayListExtra("INN1_WICKET_DATA", matchData.wicketsInn1);
						intent.putIntegerArrayListExtra("INN2_DATA", matchData.getCumulativeRunsPerOver());
						intent.putIntegerArrayListExtra("INN2_WICKET_DATA", matchData.getCumulativeWicketsPerOver());
					}

					intent.putExtra("T1_SIXES", matchData.isSecondInnings ? matchData.inn1Sixes : matchData.getSixesCount());
					intent.putExtra("T1_FOURS", matchData.isSecondInnings ? matchData.inn1Fours : matchData.getFoursCount());
					intent.putExtra("T1_DOTS", matchData.isSecondInnings ? matchData.inn1Dots : matchData.getDotsCount());
					intent.putExtra("T1_EXTRAS", matchData.isSecondInnings ? matchData.inn1ExtrasTotal : matchData.getTotalExtras());

					if (matchData.isSecondInnings) {
						intent.putExtra("T2_SIXES", matchData.getSixesCount());
						intent.putExtra("T2_FOURS", matchData.getFoursCount());
						intent.putExtra("T2_DOTS", matchData.getDotsCount());
						intent.putExtra("T2_EXTRAS", matchData.getTotalExtras());
					}

					intent.putExtra("MATCH_DATA", matchData);
					startActivity(intent);
				}
			});

        btnRetire.setOnClickListener(v -> showPremiumRetireDialog());

        btnInjured.setOnClickListener(v -> showPremiumInjuredDialog());
    }

    // =================================================================================
    // 🔥 NEW TOURNAMENT AUTOMATION METHODS 🔥
    // =================================================================================

    // ১. ম্যাচ শুরু করার সময় ফিক্সচার চেক করে পপআপ দেখানো
    // ১. ম্যাচ শুরুতে সকল ফিক্সচার স্টেজ চেক করে পপআপ দেখানো
    private void checkForTournamentFixture() {
        SharedPreferences tourPrefs = getSharedPreferences("TournamentData", MODE_PRIVATE);
        String data = tourPrefs.getString("ALL_DATA", "");
        if (data.isEmpty()) return;

        try {
            JSONObject mainObj = new JSONObject(data);
            List<String> tourTeams = getTeamsFromTourData(mainObj);

            // সব ফিক্সচার স্টেজ চেক করা: Group, QF, SF, Final
            String[][] stages = {
                {"MatchesGroup", "GroupMatch_"},
                {"MatchesQF",    "QFMatch_"},
                {"MatchesSF",    "SFMatch_"},
                {"MatchesFinal", "FinalMatch_"}
            };

            boolean found = false;
            String foundMatchId = "";

            outer:
            for (String[] stage : stages) {
                JSONArray stageMatches = mainObj.optJSONArray(stage[0]);
                if (stageMatches == null) continue;
                for (int i = 0; i < stageMatches.length(); i++) {
                    JSONObject m = stageMatches.getJSONObject(i);
                    int idx1 = m.getInt("team1_idx");
                    int idx2 = m.getInt("team2_idx");
                    // idx 0 = "Select Team" placeholder — unset fixture, skip
                    if (idx1 == 0 || idx2 == 0) continue;
                    String t1 = tourTeams.get(idx1);
                    String t2 = tourTeams.get(idx2);
                    if ((matchData.team1Name.equalsIgnoreCase(t1) && matchData.team2Name.equalsIgnoreCase(t2)) ||
                        (matchData.team1Name.equalsIgnoreCase(t2) && matchData.team2Name.equalsIgnoreCase(t1))) {
                        found = true;
                        foundMatchId = stage[1] + i;
                        break outer;
                    }
                }
            }

            if (found) {
                final String finalId = foundMatchId;
                showTournamentMatchPopup(finalId);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // 🔥 PREMIUM TOURNAMENT MATCH POPUP
    private void showTournamentMatchPopup(final String matchId) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        // ── Root container ──────────────────────────────────────────────
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.parseColor("#FFFFFF"));
        rootBg.setCornerRadius(dp(24));
        root.setBackground(rootBg);
        root.setClipToOutline(true);

        // ── Top gradient header ─────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(android.view.Gravity.CENTER);
        header.setPadding(dp(24), dp(28), dp(24), dp(22));
        GradientDrawable headerBg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#2E7D32")}
        );
        headerBg.setCornerRadii(new float[]{dp(24),dp(24),dp(24),dp(24),0,0,0,0});
        header.setBackground(headerBg);

        // Trophy icon with glow circle
        LinearLayout iconCircle = new LinearLayout(this);
        iconCircle.setGravity(android.view.Gravity.CENTER);
        int circleSize = dp(72);
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(circleSize, circleSize);
        iconCircle.setLayoutParams(icLp);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(Color.parseColor("#33FFFFFF"));
        iconCircle.setBackground(circleBg);

        TextView tvTrophy = new TextView(this);
        tvTrophy.setText("🏆");
        tvTrophy.setTextSize(32);
        tvTrophy.setGravity(android.view.Gravity.CENTER);
        iconCircle.addView(tvTrophy);
        header.addView(iconCircle);

        // "TOURNAMENT MATCH" label
        TextView tvBadge = new TextView(this);
        tvBadge.setText("TOURNAMENT MATCH");
        tvBadge.setTextSize(11);
        tvBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        tvBadge.setTextColor(Color.parseColor("#A5D6A7"));
        tvBadge.setLetterSpacing(0.15f);
        tvBadge.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        badgeLp.setMargins(0, dp(12), 0, 0);
        tvBadge.setLayoutParams(badgeLp);
        header.addView(tvBadge);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Match Found in Fixtures!");
        tvTitle.setTextSize(19);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleLp.setMargins(0, dp(6), 0, 0);
        tvTitle.setLayoutParams(titleLp);
        header.addView(tvTitle);

        root.addView(header, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // ── Teams chip row ──────────────────────────────────────────────
        LinearLayout teamsRow = new LinearLayout(this);
        teamsRow.setOrientation(LinearLayout.HORIZONTAL);
        teamsRow.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        trLp.setMargins(dp(20), dp(20), dp(20), 0);
        teamsRow.setLayoutParams(trLp);

        teamsRow.addView(makeTeamChip(matchData.team1Name));

        TextView tvVs = new TextView(this);
        tvVs.setText("VS");
        tvVs.setTextSize(12);
        tvVs.setTypeface(null, android.graphics.Typeface.BOLD);
        tvVs.setTextColor(Color.parseColor("#78909C"));
        tvVs.setPadding(dp(12), 0, dp(12), 0);
        tvVs.setGravity(android.view.Gravity.CENTER);
        teamsRow.addView(tvVs);

        teamsRow.addView(makeTeamChip(matchData.team2Name));
        root.addView(teamsRow);

        // ── Info message ────────────────────────────────────────────────
        LinearLayout infoBox = new LinearLayout(this);
        infoBox.setOrientation(LinearLayout.HORIZONTAL);
        infoBox.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        infoLp.setMargins(dp(20), dp(16), dp(20), 0);
        infoBox.setLayoutParams(infoLp);
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#F0FDF4"));
        infoBg.setStroke(dp(1), Color.parseColor("#BBF7D0"));
        infoBg.setCornerRadius(dp(12));
        infoBox.setBackground(infoBg);
        infoBox.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView tvInfoIcon = new TextView(this);
        tvInfoIcon.setText("ℹ️");
        tvInfoIcon.setTextSize(16);
        infoBox.addView(tvInfoIcon);

        TextView tvInfo = new TextView(this);
        tvInfo.setText("  Select Yes to auto-update Point Table, Results & Player Rankings after the match.");
        tvInfo.setTextSize(12.5f);
        tvInfo.setTextColor(Color.parseColor("#374151"));
        tvInfo.setLineSpacing(dp(3), 1f);
        infoBox.addView(tvInfo);
        root.addView(infoBox);

        // ── Divider ─────────────────────────────────────────────────────
        View divider = new View(this);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divLp.setMargins(dp(20), dp(20), dp(20), 0);
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        root.addView(divider);

        // ── Buttons row ─────────────────────────────────────────────────
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams brLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        brLp.setMargins(dp(16), dp(14), dp(16), dp(18));
        btnRow.setLayoutParams(brLp);

        // NO button
        Button btnNo = new Button(this);
        btnNo.setText("No, Skip");
        btnNo.setTextColor(Color.parseColor("#64748B"));
        btnNo.setTextSize(14);
        btnNo.setTypeface(null, android.graphics.Typeface.BOLD);
        btnNo.setAllCaps(false);
        GradientDrawable noBg = new GradientDrawable();
        noBg.setColor(Color.parseColor("#F1F5F9"));
        noBg.setCornerRadius(dp(14));
        btnNo.setBackground(noBg);
        LinearLayout.LayoutParams noLp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        noLp.setMargins(0, 0, dp(8), 0);
        btnNo.setLayoutParams(noLp);
        btnNo.setOnClickListener(v -> {
            matchData.isTournamentMatch = false;
            dialog.dismiss();
            Toast.makeText(this, "Normal Match Started", Toast.LENGTH_SHORT).show();
        });

        // YES button
        Button btnYes = new Button(this);
        btnYes.setText("✓  Yes, It Is!");
        btnYes.setTextColor(Color.WHITE);
        btnYes.setTextSize(14);
        btnYes.setTypeface(null, android.graphics.Typeface.BOLD);
        btnYes.setAllCaps(false);
        GradientDrawable yesBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#388E3C")}
        );
        yesBg.setCornerRadius(dp(14));
        btnYes.setBackground(yesBg);
        LinearLayout.LayoutParams yesLp = new LinearLayout.LayoutParams(0, dp(52), 1.4f);
        yesLp.setMargins(dp(8), 0, 0, 0);
        btnYes.setLayoutParams(yesLp);
        btnYes.setOnClickListener(v -> {
            matchData.isTournamentMatch = true;
            matchData.tournamentMatchId = matchId;
            dialog.dismiss();
            Toast.makeText(this, "🏆 Tournament Match! Results will auto-update.", Toast.LENGTH_LONG).show();
        });

        btnRow.addView(btnNo);
        btnRow.addView(btnYes);
        root.addView(btnRow);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // Helper: Team name chip card
    private LinearLayout makeTeamChip(String teamName) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        chip.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F8FAFC"));
        bg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        bg.setCornerRadius(dp(12));
        chip.setBackground(bg);

        TextView tvIcon = new TextView(this);
        tvIcon.setText("🏏");
        tvIcon.setTextSize(20);
        tvIcon.setGravity(android.view.Gravity.CENTER);
        chip.addView(tvIcon);

        TextView tvName = new TextView(this);
        tvName.setText(teamName);
        tvName.setTextSize(13);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#1E293B"));
        tvName.setGravity(android.view.Gravity.CENTER);
        tvName.setMaxLines(1);
        tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nameLp.setMargins(0, dp(4), 0, 0);
        tvName.setLayoutParams(nameLp);
        chip.addView(tvName);

        return chip;
    }

    // ২. ফিক্সচার ডাটা থেকে টিমের লিস্ট বের করার হেল্পার মেথড
    private List<String> getTeamsFromTourData(JSONObject obj) throws Exception {
        List<String> list = new ArrayList<>();
        list.add("Select Team");
        if (obj.has("DynamicGroups")) {
            JSONArray grps = obj.getJSONArray("DynamicGroups");
            for (int i = 0; i < grps.length(); i++) {
                JSONArray tArr = grps.getJSONObject(i).getJSONArray("teams");
                for (int j = 0; j < tArr.length(); j++) list.add(tArr.getString(j));
            }
        }
        return list;
    }

    // ৩. ম্যাচ শেষে Point Table, Player Ranking ও Tournament Result auto-update
    private void updateTournamentAutomation() {
        if (!matchData.isTournamentMatch) return;

        try {
            // ════════════════════════════════════════════════════
            // STEP 1: Tournament Result সেভ করা
            // ════════════════════════════════════════════════════
            SharedPreferences resPrefs = getSharedPreferences("TournamentResult", MODE_PRIVATE);
            JSONObject resObj = new JSONObject(resPrefs.getString("RESULT_DATA", "{}"));

            String score1, score2;
            if (matchData.isSecondInnings) {
                score1 = (matchData.scoreInn1 != null && !matchData.scoreInn1.isEmpty())
                        ? matchData.scoreInn1
                        : matchData.teamBattingFirst + " score";
                score2 = matchData.totalRuns + "/" + matchData.totalWickets;
            } else {
                score1 = matchData.totalRuns + "/" + matchData.totalWickets;
                score2 = "0/0";
            }

            // ✅ FIX: NRR (Net Run Rate) সঠিকভাবে হিসাব করার জন্য প্রতিটি দল
            // আসলে কত ওভার খেলেছে তা এখানে সেভ করা হচ্ছে। নিয়ম হলো:
            //  - দল অল-আউট হলে বা পূর্ণ কোটার ওভার শেষ করলে → পূর্ণ allotted ওভার ধরতে হবে
            //  - কিন্তু চেজিং দল অল-আউট না হয়েই টার্গেট তাড়া করে জিতে গেলে →
            //    তারা আসলে যত ওভার খেলেছে (partial), শুধু সেটাই ধরতে হবে —
            //    পূর্ণ কোটা ধরলে NRR ভুল হিসাব হয়।
            double totalOversAllotted;
            try { totalOversAllotted = Double.parseDouble(matchData.totalOvers); }
            catch (Exception e) { totalOversAllotted = 20.0; }

            double overs1 = totalOversAllotted; // ১ম ইনিংস সবসময় অল-আউট বা পূর্ণ কোটা
            double overs2;
            if (matchData.isSecondInnings) {
                boolean team2AllOutOrFullQuota = (matchData.totalWickets >= 10) || matchData.isInningsFinished();
                overs2 = team2AllOutOrFullQuota ? totalOversAllotted : (matchData.ballsBowled / 6.0);
            } else {
                overs2 = 0.0;
            }

            JSONObject mRes = new JSONObject();
            mRes.put("s1", score1);
            mRes.put("s2", score2);
            mRes.put("overs1", overs1);
            mRes.put("overs2", overs2);
            mRes.put("txt", matchData.matchStatus);
            mRes.put("team1", matchData.team1Name);
            mRes.put("team2", matchData.team2Name);
            resObj.put(matchData.tournamentMatchId, mRes);
            resPrefs.edit().putString("RESULT_DATA", resObj.toString()).apply();

            // ════════════════════════════════════════════════════
            // STEP 2: Point Table auto-update (Group Stage only)
            // ════════════════════════════════════════════════════
            if (matchData.tournamentMatchId.startsWith("GroupMatch_")) {
                SharedPreferences tourPrefs = getSharedPreferences("TournamentData", MODE_PRIVATE);
                JSONObject pointTable = new JSONObject(tourPrefs.getString("POINT_TABLE", "{}"));

                String winnerTeam = determineWinner();
                int winPts = 2;
                try { winPts = Integer.parseInt(tourPrefs.getString("WIN_POINTS", "2")); } catch (Exception ignored) {}

                if (winnerTeam != null && !winnerTeam.isEmpty()) {
                    JSONObject winnerData = pointTable.optJSONObject(winnerTeam);
                    if (winnerData == null) winnerData = new JSONObject();
                    winnerData.put("pts",    winnerData.optInt("pts", 0) + winPts);
                    winnerData.put("played", winnerData.optInt("played", 0) + 1);
                    winnerData.put("won",    winnerData.optInt("won", 0) + 1);
                    pointTable.put(winnerTeam, winnerData);

                    String loserTeam = matchData.team1Name.equals(winnerTeam)
                            ? matchData.team2Name : matchData.team1Name;
                    JSONObject loserData = pointTable.optJSONObject(loserTeam);
                    if (loserData == null) loserData = new JSONObject();
                    loserData.put("pts",    loserData.optInt("pts", 0));
                    loserData.put("played", loserData.optInt("played", 0) + 1);
                    loserData.put("won",    loserData.optInt("won", 0));
                    pointTable.put(loserTeam, loserData);
                } else {
                    int tiePts = 1;
                    for (String team : new String[]{matchData.team1Name, matchData.team2Name}) {
                        JSONObject td = pointTable.optJSONObject(team);
                        if (td == null) td = new JSONObject();
                        td.put("pts",    td.optInt("pts", 0) + tiePts);
                        td.put("played", td.optInt("played", 0) + 1);
                        td.put("won",    td.optInt("won", 0));
                        pointTable.put(team, td);
                    }
                }
                tourPrefs.edit().putString("POINT_TABLE", pointTable.toString()).apply();
            }

            // ════════════════════════════════════════════════════
            // STEP 3: Player Rankings update (Deduplicated & Accurate)
            // ════════════════════════════════════════════════════
            SharedPreferences rankPrefs = getSharedPreferences("TournamentRankings", MODE_PRIVATE);
            JSONObject batStats  = new JSONObject(rankPrefs.getString("BATSMAN_STATS", "{}"));
            JSONObject bowlStats = new JSONObject(rankPrefs.getString("BOWLER_STATS",  "{}"));

            // --- Batsmen: Innings 1 ---
            if (matchData.batsmanHistoryInn1 != null) {
                for (String[] p : matchData.batsmanHistoryInn1) {
                    if (p.length >= 5 && p[0] != null) {
                        String name = cleanName(p[0]);
                        if (!name.isEmpty() && !name.equalsIgnoreCase("Striker") && !name.equalsIgnoreCase("Non-Striker")) {
                            updateBatRanking(batStats, name, safeInt(p[1]), safeInt(p[2]), safeInt(p[3]), safeInt(p[4]));
                        }
                    }
                }
            }
            // --- Batsmen: Current Innings (consolidated list of all batters) ---
            ArrayList<String[]> currentBatters = matchData.getAllBattingStats();
            if (currentBatters != null) {
                for (String[] p : currentBatters) {
                    if (p.length >= 5 && p[0] != null) {
                        String name = cleanName(p[0]);
                        if (!name.isEmpty() && !name.equalsIgnoreCase("Striker") && !name.equalsIgnoreCase("Non-Striker")) {
                            updateBatRanking(batStats, name, safeInt(p[1]), safeInt(p[2]), safeInt(p[3]), safeInt(p[4]));
                        }
                    }
                }
            }

            // --- Bowlers: Innings 1 (deduplicated by name so multiple spells are aggregated) ---
            java.util.Map<String, int[]> inn1BowlersMap = new java.util.HashMap<>();
            if (matchData.bowlerHistoryInn1 != null) {
                for (String[] b : matchData.bowlerHistoryInn1) {
                    if (b.length >= 5 && b[0] != null) {
                        String name = cleanName(b[0]);
                        if (!name.isEmpty() && !name.equalsIgnoreCase("Bowler")) {
                            int r = safeInt(b[3]);
                            int w = safeInt(b[4]);
                            inn1BowlersMap.put(name, new int[]{r, w});
                        }
                    }
                }
            }
            for (java.util.Map.Entry<String, int[]> entry : inn1BowlersMap.entrySet()) {
                updateBowlRanking(bowlStats, entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
            }

            // --- Bowlers: Current Innings (getAllBowlingStats is already consolidated from bowlerRegistry) ---
            ArrayList<String[]> currentBowlers = matchData.getAllBowlingStats();
            java.util.Map<String, int[]> currentBowlersMap = new java.util.HashMap<>();
            if (currentBowlers != null) {
                for (String[] b : currentBowlers) {
                    if (b.length >= 5 && b[0] != null) {
                        String name = cleanName(b[0]);
                        if (!name.isEmpty() && !name.equalsIgnoreCase("Bowler")) {
                            int r = safeInt(b[3]);
                            int w = safeInt(b[4]);
                            currentBowlersMap.put(name, new int[]{r, w});
                        }
                    }
                }
            }
            for (java.util.Map.Entry<String, int[]> entry : currentBowlersMap.entrySet()) {
                updateBowlRanking(bowlStats, entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
            }

            rankPrefs.edit()
                .putString("BATSMAN_STATS", batStats.toString())
                .putString("BOWLER_STATS",  bowlStats.toString())
                .apply();

            Toast.makeText(this, "🏆 Tournament Updated!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) { e.printStackTrace(); }
    }

    private String determineWinner() {
        String status = matchData.matchStatus;
        if (status == null || status.toLowerCase().contains("tied")) return null;
        if (status.contains(" won ")) return status.substring(0, status.indexOf(" won ")).trim();
        if (matchData.isSecondInnings) {
            int s1 = parseRuns(matchData.scoreInn1);
            int s2 = matchData.totalRuns;
            if (s2 > s1) return matchData.teamBattingSecond;
            if (s1 > s2) return matchData.teamBattingFirst;
        }
        return null;
    }

    private String cleanName(String name) {
        return name.replace("*", "").replace("(Ret Hurt)", "").replace("(Ret Out)", "").trim();
    }
    private int safeInt(String val) {
        try { return Integer.parseInt(val.trim()); } catch (Exception e) { return 0; }
    }
    private int parseRuns(String scoreStr) {
        try { return Integer.parseInt(scoreStr.split("/")[0].replaceAll("[^0-9]", "")); }
        catch (Exception e) { return 0; }
    }
    private void updateBatRanking(JSONObject stats, String name, int runs, int balls, int fours, int sixes) throws Exception {
        if (name.isEmpty()) return;
        JSONObject p = stats.optJSONObject(name);
        if (p == null) p = new JSONObject();
        p.put("runs",  p.optInt("runs", 0)  + runs);
        p.put("balls", p.optInt("balls", 0) + balls);
        p.put("fours", p.optInt("fours", 0) + fours);
        p.put("sixes", p.optInt("sixes", 0) + sixes);
        p.put("inns",  p.optInt("inns", 0)  + 1);
        stats.put(name, p);
    }
    private void updateBowlRanking(JSONObject stats, String name, int runs, int wickets) throws Exception {
        if (name.isEmpty()) return;
        JSONObject b = stats.optJSONObject(name);
        if (b == null) b = new JSONObject();
        b.put("runs",    b.optInt("runs", 0)    + runs);
        b.put("wickets", b.optInt("wickets", 0) + wickets);
        b.put("inns",    b.optInt("inns", 0)    + 1);
        stats.put(name, b);
    }

            @Override
    public void onBackPressed() {
        showPremiumExitDialog();
    }

    private void showPremiumExitDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Main Container (Rounded White Background)
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(32), dp(24), dp(24));
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        
        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dp(20)); // Premium rounded corners
        container.setBackground(bgShape);

        // Icon (Exit door or Pause symbol)
        TextView iconView = new TextView(this);
        iconView.setText("🚪"); 
        iconView.setTextSize(38);
        iconView.setGravity(Gravity.CENTER);
        container.addView(iconView);

        // Dialog Title
        TextView title = new TextView(this);
        title.setText("Exit Match?");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B")); // Dark Slate color
        title.setPadding(0, dp(16), 0, dp(8));
        title.setGravity(Gravity.CENTER);
        container.addView(title);

        // Dialog Message
        TextView message = new TextView(this);
        message.setText("Are you sure you want to exit? The match will be saved as Incomplete.");
        message.setTextSize(14);
        message.setTextColor(Color.parseColor("#64748B")); // Slate Grey color
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 0, 0, dp(28));
        container.addView(message);

        // Buttons Layout
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btnLayout.setGravity(Gravity.CENTER);

        // Cancel Button (Flat look)
        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#475569"));
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setAllCaps(false);
        btnCancel.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                0, dp(48), 1.0f);
        cancelParams.setMargins(0, 0, dp(8), 0);
        btnCancel.setLayoutParams(cancelParams);
        
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss(); // ডায়ালগ কেটে যাবে
            }
        });

        // Exit Button (Premium Orange/Red color)
        Button btnExit = new Button(this);
        btnExit.setText("Exit");
        btnExit.setTextColor(Color.WHITE);
        btnExit.setAllCaps(false);
        btnExit.setTypeface(null, Typeface.BOLD);
        GradientDrawable exitBg = new GradientDrawable();
        exitBg.setColor(Color.parseColor("#F97316")); // Modern Orange for Exit/Incomplete status
        exitBg.setCornerRadius(dp(12));
        btnExit.setBackground(exitBg);
        
        LinearLayout.LayoutParams exitParams = new LinearLayout.LayoutParams(
                0, dp(48), 1.0f);
        exitParams.setMargins(dp(8), 0, 0, 0);
        btnExit.setLayoutParams(exitParams);
        
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                saveAndExit(); // আপনার আগের সেভ করার লজিক
            }
        });

        btnLayout.addView(btnCancel);
        btnLayout.addView(btnExit);
        container.addView(btnLayout);

        dialog.setContentView(container);
        
        // Background Transparent for showing curved corners perfectly
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        
        dialog.show();
    }


    @Override
    protected void onPause() {
        super.onPause();
        // 🔥 BUG FIX: matchStatus এ "won"/"Tied" থাকলে Incomplete লেখা হবে না
        String status = matchData.matchStatus;
        boolean isMatchOver = status != null && (
                status.equals("Completed") ||
                status.contains("won") ||
                status.contains("Tied") ||
                status.contains("tied")
        );
        if (!isMatchOver) {
            matchData.matchStatus = "Incomplete";
            DataManager.saveMatchToHistory(this, matchData);
            com.cricketscorez.proapp.room.LiveMatchProgressRepository.autoSave(this, matchData);
        }
    }

    private void saveAndExit() {
        matchData.matchStatus = "Incomplete";
        DataManager.saveMatchToHistory(this, matchData);
        // 🔥 LIVE SCORE: Exit-এ Firebase-এ status "Abandoned" করুন এবং cleanup করুন
        LiveScoreManager.getInstance().pushMatchResult(matchData, "Match Abandoned");
        LiveScoreManager.getInstance().cleanup();
        Toast.makeText(this, "Match Saved to History", Toast.LENGTH_SHORT).show();
        finish();
    }

    // --- ACTIVITY RESULT HANDLER ---
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK && data != null) {

			// ---------- SECOND INNINGS ----------
			if (requestCode == REQUEST_CODE_SECOND_INNINGS) {
				String s = data.getStringExtra("STRIKER");
				String ns = data.getStringExtra("NON_STRIKER");
				String b = data.getStringExtra("BOWLER");

				matchData.startSecondInnings();
				matchData.switchBowler(b);

				strikerName = s;
				nonStrikerName = ns;
				matchData.strikerName = s;
				matchData.nonStrikerName = ns;

				tvStrikerName.setText(s + " *");
				tvNonStrikerName.setText(ns);
				tvBowlerName.setText(b);

				updateScoreboardDisplay();
				// 🔥 LIVE SCORE: 2nd Innings শুরুর আগে Innings Break push করুন
				LiveScoreManager.getInstance().pushInningsBreak(matchData);
				Toast.makeText(this, "2nd Innings Started!", Toast.LENGTH_SHORT).show();
			}

			// ---------- TEST MATCH NEXT INNINGS ----------
			else if (requestCode == REQUEST_CODE_TEST_NEXT_INNINGS) {
				String s = data.getStringExtra("STRIKER");
				String ns = data.getStringExtra("NON_STRIKER");
				String b = data.getStringExtra("BOWLER");
				int innNum = data.getIntExtra("TEST_INNINGS_NUM", matchData.currentInnings + 1);

				if (innNum == 2) {
					matchData.startSecondInnings();
				} else if (innNum == 3) {
					matchData.startTestThirdInnings();
				} else if (innNum == 4) {
					matchData.startTestFourthInnings();
				}
				matchData.switchBowler(b);

				strikerName = s;
				nonStrikerName = ns;
				matchData.strikerName = s;
				matchData.nonStrikerName = ns;

				tvStrikerName.setText(s + " *");
				tvNonStrikerName.setText(ns);
				tvBowlerName.setText(b);

				updateScoreboardDisplay();
				LiveScoreManager.getInstance().pushInningsBreak(matchData);
				Toast.makeText(this, "Innings " + innNum + " Started!", Toast.LENGTH_SHORT).show();
			}

			// ---------- NEW BOWLER ----------
			else if (requestCode == REQUEST_CODE_NEW_BOWLER || requestCode == REQUEST_CODE_BOWLER_INJURED) {
				String newBowler = data.getStringExtra("NEW_BOWLER_NAME");
				if (newBowler != null) {
					if (requestCode == REQUEST_CODE_NEW_BOWLER) {
						matchData.saveBowler();
						matchData.isOverFinished = false;
						setScoringButtonsEnabled(true);
						swapBatsmenNames();
						matchData.recordManualSwap();
					}
					matchData.switchBowler(newBowler);
					tvBowlerName.setText(newBowler);
				}
			}

			// ---------- NEW / RETIRED BATSMAN ----------
			else if (requestCode == REQUEST_CODE_BATSMAN_RETIRE || requestCode == REQUEST_CODE_NEW_BATSMAN) {
				// ✅ FIX: key ঠিক করা হয়েছে — "NEW_BOWLER_NAME" ভুল ছিল
				String newBatsman = data.getStringExtra("NEW_BATSMAN_NAME");
				if (newBatsman == null) newBatsman = data.getStringExtra("NEW_BOWLER_NAME"); // backward compat
				if (newBatsman != null) {
					tvStrikerName.setText(newBatsman + " *");
					strikerName = newBatsman;
					matchData.strikerName = newBatsman;
				}
			}

			// ---------- 🔥 UPDATED WICKET HANDLING (WITH DISMISSAL DETAILS & RUN-OUT RUNS) ----------
			else if (requestCode == REQUEST_CODE_WICKET_FALL) {

				String outBat = data.getStringExtra(FallOfWicketActivity.OUT_BATSMAN);
				String newBat = data.getStringExtra(FallOfWicketActivity.NEW_BATSMAN_NAME);
				String detail = data.getStringExtra(FallOfWicketActivity.WICKET_TYPE);
				int completedRuns = data.getIntExtra(FallOfWicketActivity.COMPLETED_RUNS, 0);
				boolean nextStrikerIsNew = data.getBooleanExtra(FallOfWicketActivity.NEXT_STRIKER_IS_NEW, true);
				if (detail == null) detail = "out";

				boolean wasStrikerOut = outBat != null && outBat.equals(strikerName);

				recordWicketEvent(data, outBat, wasStrikerOut, detail, completedRuns);

				if (wasStrikerOut) {
					matchData.saveBatsman(
                        strikerName + "\n" + detail,
                        matchData.strikerRuns,
                        matchData.strikerBalls,
                        matchData.striker4s,
                        matchData.striker6s
					);

					if (nextStrikerIsNew) {
						strikerName = newBat;
						matchData.strikerName = newBat;
						matchData.resetStrikerStats();
						tvStrikerName.setText(newBat + " *");
						tvNonStrikerName.setText(nonStrikerName);
					} else {
						// Surviving batsman (non-striker) takes strike
						strikerName = nonStrikerName;
						matchData.strikerName = nonStrikerName;
						matchData.strikerRuns = matchData.nonStrikerRuns;
						matchData.strikerBalls = matchData.nonStrikerBalls;
						matchData.striker4s = matchData.nonStriker4s;
						matchData.striker6s = matchData.nonStriker6s;

						nonStrikerName = newBat;
						matchData.nonStrikerName = newBat;
						matchData.resetNonStrikerStats();
						tvStrikerName.setText(strikerName + " *");
						tvNonStrikerName.setText(newBat);
					}

				} else {
					matchData.saveBatsman(
                        nonStrikerName + "\n" + detail,
                        matchData.nonStrikerRuns,
                        matchData.nonStrikerBalls,
                        matchData.nonStriker4s,
                        matchData.nonStriker6s
					);

					if (!nextStrikerIsNew) {
						// Surviving batsman (striker) stays on strike
						nonStrikerName = newBat;
						matchData.nonStrikerName = newBat;
						matchData.resetNonStrikerStats();
						tvStrikerName.setText(strikerName + " *");
						tvNonStrikerName.setText(newBat);
					} else {
						// Batsmen crossed, so incoming batsman takes strike
						nonStrikerName = strikerName;
						matchData.nonStrikerName = strikerName;
						matchData.nonStrikerRuns = matchData.strikerRuns;
						matchData.nonStrikerBalls = matchData.strikerBalls;
						matchData.nonStriker4s = matchData.striker4s;
						matchData.nonStriker6s = matchData.striker6s;

						strikerName = newBat;
						matchData.strikerName = newBat;
						matchData.resetStrikerStats();
						tvStrikerName.setText(newBat + " *");
						tvNonStrikerName.setText(nonStrikerName);
					}
				}

				matchData.fallOfWickets.add(matchData.getScoreString() + " (" + outBat + ", " + matchData.getOversString() + ")");

				cbWide.setChecked(false);
				cbNoBall.setChecked(false);
				cbByes.setChecked(false);
				cbLegByes.setChecked(false);
				cbWicket.setChecked(false);
				setScoringButtonsEnabled(true);

				checkMatchStatusOrInningsEnd();
			}

			updateScoreboardDisplay();
			// 🔥 LIVE SCORE: Wicket সহ যেকোনো onActivityResult-এর পরে Firebase update
			LiveScoreManager.getInstance().pushLiveScore(matchData, strikerName, nonStrikerName);
		}
		else if (resultCode == RESULT_CANCELED) {
			if (requestCode == REQUEST_CODE_NEW_BATSMAN)
				matchData.totalWickets -= 1;

			setScoringButtonsEnabled(true);
		}
	}

    private void recordScoringEvent(final int runs) {
        boolean isWkt = cbWicket.isChecked();
        if (isWkt) {
            boolean isWd = cbWide.isChecked();
            boolean isNb = cbNoBall.isChecked();
            boolean isBye = cbByes.isChecked();
            boolean isLb = cbLegByes.isChecked();
            if ((isWd || isNb) && (isBye || isLb)) {
                Toast.makeText(this, "Select only one type of Extra", Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(MainActivity.this, FallOfWicketActivity.class);
            intent.putExtra("STRIKER_NAME", strikerName);
            intent.putExtra("NON_STRIKER_NAME", nonStrikerName);
            intent.putExtra("RUNS_ENTERED", runs);
            intent.putExtra("IS_NO_BALL", isNb);
            intent.putExtra("IS_WIDE", isWd);
            intent.putExtra("IS_BYE", isBye);
            intent.putExtra("IS_LEG_BYE", isLb);
            startActivityForResult(intent, REQUEST_CODE_WICKET_FALL);
            setScoringButtonsEnabled(false);
            return;
        }

        boolean isWd = cbWide.isChecked(); boolean isNb = cbNoBall.isChecked(); boolean isBye = cbByes.isChecked(); boolean isLb = cbLegByes.isChecked();
        if ((isWd || isNb) && (isBye || isLb)) { Toast.makeText(this, "Select only one type of Extra", Toast.LENGTH_LONG).show(); return; }

        boolean isExtra = isWd || isNb || isBye || isLb; String extraType = ""; int finalRuns = runs; boolean needsStrikeChange = (runs % 2 != 0); 
        if (isWd) {
            extraType = "WD";
            finalRuns = 1 + runs;
            // 🔥 BUG FIX 4: Wide strike change depends on BATTER's runs only (not +1 penalty)
            // Wide 0 → batter ran 0 → no strike change
            // Wide 2 → batter ran 2 → no strike change (even)
            // Wide 1 → batter ran 1 → strike changes (odd) — rare but possible
            needsStrikeChange = (runs % 2 != 0);
        } else if (isNb) {
            extraType = "NB";
            finalRuns = 1 + runs;
            // 🔥 BUG FIX 4: NoBall strike change depends on BATTER's runs (not +1 penalty)
            needsStrikeChange = (runs % 2 != 0);
        } else if (isBye) {
            extraType = "BYE";
            finalRuns = runs;
            needsStrikeChange = (finalRuns % 2 != 0);
        } else if (isLb) {
            extraType = "LB";
            finalRuns = runs;
            needsStrikeChange = (finalRuns % 2 != 0);
        }

        BallEvent event = new BallEvent(finalRuns, isWkt, isExtra, extraType, needsStrikeChange, false, matchData.currentBowlerName, strikerName);
        matchData.addEvent(event);
        updateScoreboardDisplay();

        // ✅ FIX: ICC Law 18 — বলের ধরন নির্বিশেষে (legal, wide, no-ball সব ক্ষেত্রে)
        // ব্যাটসম্যান বিজোড় সংখ্যক রান নিলে strike বদলাবে।
        if (event.needsStrikeChange) { matchData.swapStrikerStats(); swapBatsmenNames(); updateScoreboardDisplay(); }
        cbWide.setChecked(false); cbNoBall.setChecked(false); cbByes.setChecked(false); cbLegByes.setChecked(false); cbWicket.setChecked(false);

        // 🔥 LIVE SCORE: প্রতিটি বলের পরে Firebase-এ push করুন
        LiveScoreManager.getInstance().pushLiveScore(matchData, strikerName, nonStrikerName);

        checkMatchStatusOrInningsEnd();
    }

    private void checkMatchStatusOrInningsEnd() {
        boolean isAllOut = matchData.totalWickets >= 10;
        boolean isOversDone = matchData.isInningsFinished();

        if (matchData.isTestMatch) {
            int current = matchData.currentInnings;
            if (current == 4) {
                if (matchData.targetRuns > 0 && matchData.totalRuns >= matchData.targetRuns) {
                    int wicketsLeft = 10 - matchData.totalWickets;
                    String wicketWord = (wicketsLeft == 1) ? "wicket" : "wickets";
                    showMatchResultDialog(matchData.getBattingTeamName() + " won by " + wicketsLeft + " " + wicketWord + "!");
                    return;
                }
                if (isAllOut) {
                    int margin = (matchData.targetRuns - 1) - matchData.totalRuns;
                    if (margin > 0) {
                        String runWord = (margin == 1) ? "run" : "runs";
                        showMatchResultDialog(matchData.getBowlingTeamName() + " won by " + margin + " " + runWord + "!");
                    } else if (matchData.totalRuns == matchData.targetRuns - 1) {
                        showMatchResultDialog("Match Tied!");
                    }
                    return;
                }
            }

            if (isAllOut) {
                handleTestInningsFinished();
                return;
            }

            if (matchData.isOverFinished) {
                matchData.advanceOverAndSessionIfNeeded();
                if (matchData.currentDay > matchData.testDays) {
                    showMatchResultDialog("Match Drawn (Match Duration Expired)");
                    return;
                }
                setScoringButtonsEnabled(false);
                launchNewBowlerActivity();
            }
            return;
        }

        // Limited overs matches
        if (matchData.isSecondInnings) {
            if (matchData.totalRuns >= matchData.targetRuns) {
                int wicketsLeft = 10 - matchData.totalWickets;
                String wicketWord = (wicketsLeft == 1) ? "wicket" : "wickets";
                showMatchResultDialog(matchData.teamBattingSecond + " won by " + wicketsLeft + " " + wicketWord + "!");
            } else if (isOversDone || isAllOut) {
                if (matchData.totalRuns < matchData.targetRuns - 1) {
                    int runsMargin = (matchData.targetRuns - 1) - matchData.totalRuns;
                    String runWord = (runsMargin == 1) ? "run" : "runs";
                    showMatchResultDialog(matchData.teamBattingFirst + " won by " + runsMargin + " " + runWord + "!");
                } else {
                    showMatchResultDialog("Match Tied!");
                }
            } else if (matchData.isOverFinished) {
                setScoringButtonsEnabled(false);
                launchNewBowlerActivity();
            }
        } else {
            if (isAllOut || isOversDone) {
                showInningsBreakDialog();
            } else if (matchData.isOverFinished) {
                setScoringButtonsEnabled(false);
                launchNewBowlerActivity();
            }
        }
    }

    private void handleTestInningsFinished() {
        int current = matchData.currentInnings;
        if (current == 1) {
            showTestInningsBreakDialog(1);
        } else if (current == 2) {
            if (matchData.canEnforceFollowOn()) {
                showFollowOnChoiceDialog();
            } else {
                showTestInningsBreakDialog(2);
            }
        } else if (current == 3) {
            if (matchData.isFollowOnEnforced && matchData.getTestFourthInningsTarget() <= 0) {
                int lead = matchData.firstInningsScore - (matchData.inn2Runs + matchData.totalRuns);
                if (lead > 0) {
                    showMatchResultDialog(matchData.teamBattingFirst + " won by an innings and " + lead + " runs!");
                } else {
                    showMatchResultDialog("Match Tied!");
                }
            } else {
                showTestInningsBreakDialog(3);
            }
        } else if (current == 4) {
            if (matchData.totalRuns >= matchData.targetRuns) {
                int wicketsLeft = 10 - matchData.totalWickets;
                String wicketWord = (wicketsLeft == 1) ? "wicket" : "wickets";
                showMatchResultDialog(matchData.getBattingTeamName() + " won by " + wicketsLeft + " " + wicketWord + "!");
            } else {
                int margin = (matchData.targetRuns - 1) - matchData.totalRuns;
                if (margin > 0) {
                    String runWord = (margin == 1) ? "run" : "runs";
                    showMatchResultDialog(matchData.getBowlingTeamName() + " won by " + margin + " " + runWord + "!");
                } else {
                    showMatchResultDialog("Match Tied!");
                }
            }
        }
    }

    private void showTestInningsBreakDialog(final int completedInn) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(16));
        root.setBackground(bg);

        String nextBatting;
        String nextBowling;
        int nextInn = completedInn + 1;

        if (completedInn == 1) {
            nextBatting = matchData.teamBattingSecond;
            nextBowling = matchData.teamBattingFirst;
        } else if (completedInn == 2) {
            nextBatting = matchData.teamBattingFirst;
            nextBowling = matchData.teamBattingSecond;
        } else {
            nextBatting = matchData.isFollowOnEnforced ? matchData.teamBattingFirst : matchData.teamBattingSecond;
            nextBowling = matchData.isFollowOnEnforced ? matchData.teamBattingSecond : matchData.teamBattingFirst;
        }

        TextView title = new TextView(this);
        title.setText("End of " + completedInn + (completedInn == 1 ? "st" : completedInn == 2 ? "nd" : "rd") + " Innings");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#0F172A"));
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText(matchData.getBattingTeamName() + " scored " + matchData.getScoreString() + " in " + matchData.getOversString() + " overs.\n\n" + matchData.getTestMatchLeadTrailStatus());
        desc.setTextSize(13);
        desc.setTextColor(Color.parseColor("#334155"));
        desc.setPadding(0, dp(8), 0, dp(16));
        root.addView(desc);

        Button btnNext = new Button(this);
        btnNext.setText("Start " + nextInn + (nextInn == 2 ? "nd" : nextInn == 3 ? "rd" : "th") + " Innings (" + nextBatting + ")");
        btnNext.setBackgroundColor(Color.parseColor("#16A34A"));
        btnNext.setTextColor(Color.WHITE);
        btnNext.setAllCaps(false);
        btnNext.setOnClickListener(v -> {
            dialog.dismiss();
            launchTestNextInningsPlayerSelection(nextInn, nextBatting, nextBowling);
        });
        root.addView(btnNext);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void showFollowOnChoiceDialog() {
        int lead = matchData.firstInningsScore - matchData.secondInningsScore;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(16));
        root.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("🏏 Follow-On Available!");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#15803D"));
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText(matchData.teamBattingFirst + " has a 1st innings lead of " + lead + " runs (Minimum required: " + matchData.testFollowOnMargin + ").\n\nDo you want to ENFORCE the follow-on, making " + matchData.teamBattingSecond + " bat again immediately?");
        desc.setTextSize(13);
        desc.setTextColor(Color.parseColor("#334155"));
        desc.setPadding(0, dp(8), 0, dp(16));
        root.addView(desc);

        Button btnEnforce = new Button(this);
        btnEnforce.setText("Yes, Enforce Follow-On (" + matchData.teamBattingSecond + " bats)");
        btnEnforce.setBackgroundColor(Color.parseColor("#16A34A"));
        btnEnforce.setTextColor(Color.WHITE);
        btnEnforce.setAllCaps(false);
        btnEnforce.setOnClickListener(v -> {
            dialog.dismiss();
            matchData.isFollowOnEnforced = true;
            launchTestNextInningsPlayerSelection(3, matchData.teamBattingSecond, matchData.teamBattingFirst);
        });
        root.addView(btnEnforce);

        Button btnBatAgain = new Button(this);
        btnBatAgain.setText("No, Bat Again (" + matchData.teamBattingFirst + " bats)");
        btnBatAgain.setBackgroundColor(Color.parseColor("#2563EB"));
        btnBatAgain.setTextColor(Color.WHITE);
        btnBatAgain.setAllCaps(false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(8);
        btnBatAgain.setLayoutParams(p);
        btnBatAgain.setOnClickListener(v -> {
            dialog.dismiss();
            matchData.isFollowOnEnforced = false;
            launchTestNextInningsPlayerSelection(3, matchData.teamBattingFirst, matchData.teamBattingSecond);
        });
        root.addView(btnBatAgain);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void launchTestNextInningsPlayerSelection(int inn, String battingTeam, String bowlingTeam) {
        Intent intent = new Intent(MainActivity.this, PlayerSelectionActivity.class);
        intent.putExtra("IS_SECOND_INNINGS", true);
        intent.putExtra("IS_TEST_MATCH", true);
        intent.putExtra("TEST_INNINGS_NUM", inn);
        intent.putExtra("TEAM_1", battingTeam);
        intent.putExtra("TEAM_2", bowlingTeam);
        startActivityForResult(intent, REQUEST_CODE_TEST_NEXT_INNINGS);
    }

    private void showTestSessionControlsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(16));
        root.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("⏳ Test Match Day & Session");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#0F172A"));
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("Current: Day " + matchData.currentDay + " of " + matchData.testDays + " • Session " + matchData.currentSession + "\n" +
                "Overs Today: " + (matchData.ballsBowledToday / 6) + "." + (matchData.ballsBowledToday % 6) + " / " + matchData.oversPerDay + " ov\n" +
                matchData.getTestMatchLeadTrailStatus());
        info.setTextSize(13);
        info.setTextColor(Color.parseColor("#334155"));
        info.setPadding(0, dp(8), 0, dp(16));
        root.addView(info);

        Button btnNextSession = new Button(this);
        btnNextSession.setText("⏭️ Next Session (" + (matchData.currentSession < 3 ? "Session " + (matchData.currentSession + 1) : "Day " + (matchData.currentDay + 1) + " Session 1") + ")");
        btnNextSession.setBackgroundColor(Color.parseColor("#2563EB"));
        btnNextSession.setTextColor(Color.WHITE);
        btnNextSession.setAllCaps(false);
        btnNextSession.setOnClickListener(v -> {
            dialog.dismiss();
            matchData.currentSession++;
            if (matchData.currentSession > 3) {
                matchData.currentSession = 1;
                matchData.currentDay++;
                matchData.ballsBowledToday = 0;
            }
            updateScoreboardDisplay();
            Toast.makeText(this, "Advanced to Day " + matchData.currentDay + ", Session " + matchData.currentSession, Toast.LENGTH_SHORT).show();
        });
        root.addView(btnNextSession);

        Button btnStumps = new Button(this);
        btnStumps.setText("Day Stumps (End of Day " + matchData.currentDay + ")");
        btnStumps.setBackgroundColor(Color.parseColor("#475569"));
        btnStumps.setTextColor(Color.WHITE);
        btnStumps.setAllCaps(false);
        LinearLayout.LayoutParams pStumps = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pStumps.topMargin = dp(8);
        btnStumps.setLayoutParams(pStumps);
        btnStumps.setOnClickListener(v -> {
            dialog.dismiss();
            matchData.currentSession = 1;
            matchData.currentDay++;
            matchData.ballsBowledToday = 0;
            updateScoreboardDisplay();
            Toast.makeText(this, "Stumps called. Advanced to Day " + matchData.currentDay, Toast.LENGTH_SHORT).show();
        });
        root.addView(btnStumps);

        Button btnDeclareDraw = new Button(this);
        btnDeclareDraw.setText("🤝 Agree on Draw");
        btnDeclareDraw.setBackgroundColor(Color.parseColor("#DC2626"));
        btnDeclareDraw.setTextColor(Color.WHITE);
        btnDeclareDraw.setAllCaps(false);
        LinearLayout.LayoutParams pDraw = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pDraw.topMargin = dp(8);
        btnDeclareDraw.setLayoutParams(pDraw);
        btnDeclareDraw.setOnClickListener(v -> {
            dialog.dismiss();
            showMatchResultDialog("Match Drawn");
        });
        root.addView(btnDeclareDraw);

        Button btnClose = new Button(this);
        btnClose.setText("Close");
        btnClose.setTextColor(Color.parseColor("#64748B"));
        btnClose.setBackgroundColor(Color.TRANSPARENT);
        btnClose.setAllCaps(false);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnClose);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void launchNewBowlerActivity() {
        Intent intent = new Intent(MainActivity.this, ChooseBowlerActivity.class);
        intent.putStringArrayListExtra("SUGGESTIONS", matchData.getBowlerSuggestions());
        // ✅ FIX: ICC Law 17.5 — একই বোলার পরপর দুই ওভার করতে পারে না।
        intent.putExtra("LAST_OVER_BOWLER", matchData.currentBowlerName);
        startActivityForResult(intent, REQUEST_CODE_NEW_BOWLER);
    }

    // ✅ FIX: completedRuns parameter যুক্ত করা হলো — Run Out বা অন্যান্য উইকেটে
    // ব্যাটসম্যানরা যে রান দৌড়ে সম্পন্ন করেছে তা স্কোরে যোগ হবে।
    // ✅ FIX: dismissalType parameter — Run Out হলে বোলারের wicket count বাড়বে না।
    private void recordWicketEvent(Intent data, String outBat, boolean isStrikerOut, String dismissalType, int completedRuns) {
        boolean isWd = cbWide.isChecked();
        boolean isNb = cbNoBall.isChecked();
        boolean isBye = cbByes.isChecked();
        boolean isLb = cbLegByes.isChecked();
        boolean isExtra = isWd || isNb || isBye || isLb;
        String extraType = isWd ? "WD" : (isNb ? "NB" : (isBye ? "BYE" : (isLb ? "LB" : "")));
        int totalRunsForBall = completedRuns + ((isWd || isNb) ? 1 : 0);

        BallEvent event = new BallEvent(totalRunsForBall, true, isExtra, extraType,
                false, isStrikerOut, matchData.currentBowlerName, outBat);

        // ✅ FIX: Run Out-এ বোলারের wicket count বাড়বে না।
        if (dismissalType != null && dismissalType.toLowerCase(java.util.Locale.ROOT).contains("run out")) {
            event.creditBowlerForWicket = false;
        }

        matchData.recordPartnership(outBat, isStrikerOut);
        matchData.addEvent(event);
    }

    private void showInningsBreakDialog() {
        String bowlingTeam = matchData.teamBattingSecond;
        String battingTeam = matchData.teamBattingFirst;
        int score         = matchData.totalRuns;
        int wickets       = matchData.totalWickets;
        int target        = score + 1;
        String overs      = matchData.totalOvers;
        String scored     = matchData.getOversString();
        // ✅ FIX #5: totalOvers string parse করতে গিয়ে crash হতে পারে — try-catch দিয়ে সুরক্ষিত করা হয়েছে।
        int totalOversInt = 20;
        try { totalOversInt = Integer.parseInt(overs); } catch (NumberFormatException ignored) {}
        double rrr = totalOversInt > 0 ? (double) target / totalOversInt : 0.0;

        // ── Custom Dialog ────────────────────────────────────────────────
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        // Root card
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(0), dp(24), dp(24));

        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.WHITE);
        rootBg.setCornerRadius(dp(20));
        root.setBackground(rootBg);

        // ── Green header strip ──────────────────────────────────────────
        LinearLayout headerStrip = new LinearLayout(this);
        headerStrip.setOrientation(LinearLayout.VERTICAL);
        headerStrip.setGravity(Gravity.CENTER);
        headerStrip.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColor(Color.parseColor("#1B5E20"));
        int[] radii = {dp(20), dp(20), dp(20), dp(20), 0, 0, 0, 0};
        hBg.setCornerRadii(new float[]{dp(20),dp(20),dp(20),dp(20),0,0,0,0});
        headerStrip.setBackground(hBg);

        TextView tvIcon = new TextView(this);
        tvIcon.setText("🏏");
        tvIcon.setTextSize(36);
        tvIcon.setGravity(Gravity.CENTER);
        headerStrip.addView(tvIcon);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("1st Innings Complete!");
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.setMargins(0, dp(6), 0, 0);
        tvTitle.setLayoutParams(titleLp);
        headerStrip.addView(tvTitle);

        root.addView(headerStrip, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ── Score summary box ───────────────────────────────────────────
        LinearLayout scoreBox = new LinearLayout(this);
        scoreBox.setOrientation(LinearLayout.VERTICAL);
        scoreBox.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbLp.setMargins(0, dp(18), 0, 0);
        scoreBox.setLayoutParams(sbLp);
        GradientDrawable sbBg = new GradientDrawable();
        sbBg.setColor(Color.parseColor("#F1F8E9"));
        sbBg.setCornerRadius(dp(12));
        scoreBox.setBackground(sbBg);
        scoreBox.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView tvTeam = new TextView(this);
        tvTeam.setText(battingTeam);
        tvTeam.setTextSize(13);
        tvTeam.setTypeface(null, Typeface.BOLD);
        tvTeam.setTextColor(Color.parseColor("#2E7D32"));
        tvTeam.setGravity(Gravity.CENTER);
        scoreBox.addView(tvTeam);

        TextView tvScore = new TextView(this);
        tvScore.setText(score + "/" + wickets);
        tvScore.setTextSize(36);
        tvScore.setTypeface(null, Typeface.BOLD);
        tvScore.setTextColor(Color.parseColor("#1B5E20"));
        tvScore.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scLp.setMargins(0, dp(2), 0, 0);
        tvScore.setLayoutParams(scLp);
        scoreBox.addView(tvScore);

        TextView tvOvers = new TextView(this);
        tvOvers.setText("(" + scored + " overs)");
        tvOvers.setTextSize(12);
        tvOvers.setTextColor(Color.parseColor("#558B2F"));
        tvOvers.setGravity(Gravity.CENTER);
        scoreBox.addView(tvOvers);

        root.addView(scoreBox);

        // ── Target row ──────────────────────────────────────────────────
        LinearLayout targetRow = new LinearLayout(this);
        targetRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trLp.setMargins(0, dp(14), 0, 0);
        targetRow.setLayoutParams(trLp);

        // Target block
        LinearLayout tBlock = makeInfoBlock("🎯 Target", String.valueOf(target), "#E65100", "#FFF3E0");
        targetRow.addView(tBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        View sep = new View(this);
        sep.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 0));
        targetRow.addView(sep);

        // RRR block
        LinearLayout rrrBlock = makeInfoBlock("⚡ Req. Rate", String.format("%.2f", rrr), "#1565C0", "#E3F2FD");
        targetRow.addView(rrrBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(targetRow);

        // ── Chasing team label ──────────────────────────────────────────
        TextView tvChase = new TextView(this);
        tvChase.setText(bowlingTeam + " need " + target + " runs in " + overs + " overs");
        tvChase.setTextSize(13);
        tvChase.setTextColor(Color.parseColor("#546E7A"));
        tvChase.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.setMargins(0, dp(14), 0, 0);
        tvChase.setLayoutParams(cLp);
        root.addView(tvChase);

        // ── CTA Button ──────────────────────────────────────────────────
        TextView btnStart = new TextView(this);
        btnStart.setText("Start 2nd Innings  ▶");
        btnStart.setTextSize(15);
        btnStart.setTypeface(null, Typeface.BOLD);
        btnStart.setTextColor(Color.WHITE);
        btnStart.setGravity(Gravity.CENTER);
        btnStart.setPadding(dp(20), dp(14), dp(20), dp(14));
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#1B5E20"));
        btnBg.setCornerRadius(dp(12));
        btnStart.setBackground(btnBg);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, dp(20), 0, 0);
        btnStart.setLayoutParams(btnLp);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dialog.dismiss();
                Intent i = new Intent(MainActivity.this, PlayerSelectionActivity.class);
                i.putExtra("IS_SECOND_INNINGS", true);
                startActivityForResult(i, REQUEST_CODE_SECOND_INNINGS);
            }
        });
        root.addView(btnStart);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.88f), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // Helper: small info block card
    private LinearLayout makeInfoBlock(String label, String value, String textColor, String bgColor) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setGravity(Gravity.CENTER);
        block.setPadding(dp(10), dp(12), dp(10), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(bgColor));
        bg.setCornerRadius(dp(12));
        block.setBackground(bg);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(11);
        tvLabel.setTextColor(Color.parseColor(textColor));
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setGravity(Gravity.CENTER);
        block.addView(tvLabel);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(22);
        tvValue.setTypeface(null, Typeface.BOLD);
        tvValue.setTextColor(Color.parseColor(textColor));
        tvValue.setGravity(Gravity.CENTER);
        block.addView(tvValue);

        return block;
    }

    // 🔥 UPDATED: Match Result Dialog triggering Automation
    // 🔥 UPDATED: Match Result Dialog — Modern Custom UI
    private void showMatchResultDialog(String message) {
        // 🔥 BUG FIX: matchStatus এ directly message লিখলে onPause() "Completed" check fail করত।
        // তাই matchStatus = message রাখা হচ্ছে (won/Tied contain check onPause এ আছে)।
        matchData.matchStatus = message;
        DataManager.saveMatchToHistory(this, matchData);
        saveMatchDataToProfiles();
        updateTournamentAutomation();
        // 🔥 LIVE SCORE: Match শেষ হলে Firebase-এ result push করুন
        LiveScoreManager.getInstance().pushMatchResult(matchData, message);

        // ☁️ SUPABASE: Match history server এ save করা হচ্ছে
        // tournament_id নেওয়া হচ্ছে SharedPreferences থেকে
        android.content.SharedPreferences tourPrefs =
                getSharedPreferences("TournamentData", MODE_PRIVATE);
        String supabaseTournamentId = tourPrefs.getString("SUPABASE_TOURNAMENT_ID", "");
        if (!supabaseTournamentId.isEmpty()) {
            matchData.tournamentId = supabaseTournamentId;
            matchData.tournamentName = tourPrefs.getString("TOURNAMENT_NAME", "");
        } else {
            String tName = tourPrefs.getString("TOURNAMENT_NAME", "");
            if (!tName.isEmpty()) {
                matchData.tournamentName = tName;
                matchData.tournamentId = FirebaseSync.slug(tName);
            }
        }
        FirebaseSync.saveMatchHistory(matchData, message);

        final Dialog resultDialog = new Dialog(this);
        resultDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        resultDialog.setCancelable(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(0), dp(24), dp(24));
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.WHITE);
        rootBg.setCornerRadius(dp(20));
        root.setBackground(rootBg);

        boolean isTied = message.toLowerCase().contains("tied");
        int headerColor = isTied ? Color.parseColor("#E65100") : Color.parseColor("#1B5E20");

        // Header strip
        LinearLayout headerStrip = new LinearLayout(this);
        headerStrip.setOrientation(LinearLayout.VERTICAL);
        headerStrip.setGravity(Gravity.CENTER);
        headerStrip.setPadding(dp(20), dp(22), dp(20), dp(18));
        GradientDrawable hBg = new GradientDrawable();
        hBg.setColor(headerColor);
        hBg.setCornerRadii(new float[]{dp(20),dp(20),dp(20),dp(20),0,0,0,0});
        headerStrip.setBackground(hBg);

        TextView tvTrophy = new TextView(this);
        tvTrophy.setText(isTied ? "\uD83E\uDD1D" : "\uD83C\uDFC6");
        tvTrophy.setTextSize(44);
        tvTrophy.setGravity(Gravity.CENTER);
        headerStrip.addView(tvTrophy);

        TextView tvHeading = new TextView(this);
        tvHeading.setText(isTied ? "Match Tied!" : "Match Complete!");
        tvHeading.setTextSize(20);
        tvHeading.setTypeface(null, Typeface.BOLD);
        tvHeading.setTextColor(Color.WHITE);
        tvHeading.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.setMargins(0, dp(8), 0, 0);
        tvHeading.setLayoutParams(hLp);
        headerStrip.addView(tvHeading);
        root.addView(headerStrip, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Result banner
        LinearLayout resultBox = new LinearLayout(this);
        resultBox.setOrientation(LinearLayout.VERTICAL);
        resultBox.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rbLp.setMargins(0, dp(20), 0, 0);
        resultBox.setLayoutParams(rbLp);
        GradientDrawable rbBg = new GradientDrawable();
        rbBg.setColor(isTied ? Color.parseColor("#FFF3E0") : Color.parseColor("#F1F8E9"));
        rbBg.setCornerRadius(dp(12));
        resultBox.setBackground(rbBg);
        resultBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView tvResult = new TextView(this);
        tvResult.setText(message);
        tvResult.setTextSize(17);
        tvResult.setTypeface(null, Typeface.BOLD);
        tvResult.setTextColor(isTied ? Color.parseColor("#E65100") : Color.parseColor("#1B5E20"));
        tvResult.setGravity(Gravity.CENTER);
        resultBox.addView(tvResult);
        root.addView(resultBox);

        // Score summary row
        LinearLayout scoresRow = new LinearLayout(this);
        scoresRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srLp.setMargins(0, dp(14), 0, 0);
        scoresRow.setLayoutParams(srLp);
        String inn1Score = (matchData.scoreInn1 != null && !matchData.scoreInn1.isEmpty())
                ? matchData.scoreInn1 + "\n(" + matchData.oversInn1 + ")" : matchData.getScoreString();
        String inn2Score = matchData.getScoreString() + "\n(" + matchData.getOversString() + ")";
        LinearLayout b1 = makeInfoBlock(matchData.teamBattingFirst, inn1Score, "#1B5E20", "#F1F8E9");
        scoresRow.addView(b1, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        View gap = new View(this);
        gap.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 0));
        scoresRow.addView(gap);
        LinearLayout b2 = makeInfoBlock(matchData.teamBattingSecond, inn2Score, "#1565C0", "#E3F2FD");
        scoresRow.addView(b2, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(scoresRow);

        // Saved label
        TextView tvSaved = new TextView(this);
        tvSaved.setText("\u2705  Match saved to history");
        tvSaved.setTextSize(12);
        tvSaved.setTextColor(Color.parseColor("#78909C"));
        tvSaved.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        svLp.setMargins(0, dp(14), 0, 0);
        tvSaved.setLayoutParams(svLp);
        root.addView(tvSaved);

        // Action Buttons Row (Share Result + Done)
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.setMargins(0, dp(20), 0, 0);
        actionsRow.setLayoutParams(actLp);

        // Share Button
        Button btnShareResult = new Button(this);
        btnShareResult.setText("📤  Share");
        btnShareResult.setTextSize(14);
        btnShareResult.setTypeface(null, Typeface.BOLD);
        btnShareResult.setTextColor(Color.parseColor("#1E293B"));
        btnShareResult.setAllCaps(false);
        GradientDrawable shareBg = new GradientDrawable();
        shareBg.setColor(Color.parseColor("#F1F5F9"));
        shareBg.setCornerRadius(dp(12));
        shareBg.setStroke(dp(1), Color.parseColor("#CBD5E1"));
        btnShareResult.setBackground(shareBg);
        LinearLayout.LayoutParams shareLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        shareLp.setMargins(0, 0, dp(8), 0);
        btnShareResult.setLayoutParams(shareLp);
        btnShareResult.setOnClickListener(v -> MatchShareManager.showShareDialog(MainActivity.this, matchData));
        actionsRow.addView(btnShareResult);

        // OK button
        Button btnOk = new Button(this);
        btnOk.setText("Done  ✔");
        btnOk.setTextSize(14);
        btnOk.setTypeface(null, Typeface.BOLD);
        btnOk.setTextColor(Color.WHITE);
        btnOk.setAllCaps(false);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(headerColor);
        btnBg.setCornerRadius(dp(12));
        btnOk.setBackground(btnBg);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, dp(48), 1.2f);
        btnOk.setLayoutParams(btnLp);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // Clear live match progress from Room on match completion
                com.cricketscorez.proapp.room.LiveMatchProgressRepository.clearMatchProgress(MainActivity.this, matchData.matchId);
                // 🔥 LIVE SCORE: Match শেষে Done চাপলে LiveScoreManager cleanup করুন
                LiveScoreManager.getInstance().cleanup();
                resultDialog.dismiss();
                finish();
            }
        });
        actionsRow.addView(btnOk);
        root.addView(actionsRow);

        resultDialog.setContentView(root);
        if (resultDialog.getWindow() != null) {
            resultDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            resultDialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.88f), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        resultDialog.show();
    }
    private void saveMatchDataToProfiles() {
        if(matchData.strikerName != null) 
            matchData.saveBatsman(matchData.strikerName, matchData.strikerRuns, matchData.strikerBalls, matchData.striker4s, matchData.striker6s);
        if(matchData.nonStrikerName != null) 
            matchData.saveBatsman(matchData.nonStrikerName, matchData.nonStrikerRuns, matchData.nonStrikerBalls, matchData.nonStriker4s, matchData.nonStriker6s);

        matchData.saveBowler();

        ArrayList<String[]> allBatsmen = new ArrayList<>();
        if (matchData.batsmanHistory != null) allBatsmen.addAll(matchData.batsmanHistory);
        if (matchData.batsmanHistoryInn1 != null) allBatsmen.addAll(matchData.batsmanHistoryInn1);

        for (String[] bat : allBatsmen) {
            try {
                String name = bat[0].replace("*", "").trim(); 
                if (name.contains("(")) name = name.substring(0, name.indexOf("(")).trim();

                int runs = Integer.parseInt(bat[1]);
                int balls = Integer.parseInt(bat[2]);
                int fours = Integer.parseInt(bat[3]);
                int sixes = Integer.parseInt(bat[4]);
                boolean isOut = !bat[0].contains("*") && !bat[0].contains("Not Out");

                DataManager.updateBatting(this, name, runs, balls, fours, sixes, isOut);
            } catch (Exception e) {}
        }

        ArrayList<String[]> allBowlers = new ArrayList<>();
        if(matchData.bowlerHistory != null) allBowlers.addAll(matchData.getAllBowlingStats());
        if(matchData.bowlerHistoryInn1 != null) allBowlers.addAll(matchData.bowlerHistoryInn1);

        for (String[] bowl : allBowlers) {
            try {
                String name = bowl[0].replace("*", "").trim();
                double overs = 0.0;
                try { overs = Double.parseDouble(bowl[1]); } catch(Exception e){}
                int maidens = Integer.parseInt(bowl[2]);
                int runs = Integer.parseInt(bowl[3]);
                int wickets = Integer.parseInt(bowl[4]);

                DataManager.updateBowling(this, name, overs, maidens, runs, wickets);
            } catch (Exception e) {}
        }
        Toast.makeText(this, "Profiles Updated!", Toast.LENGTH_SHORT).show();
    }

    private int dp(int val) { return Math.round(val * getResources().getDisplayMetrics().density); }

    private void showPremiumUndoDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(24));
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dpToPx(20));
        container.setBackground(bgShape);

        TextView iconView = new TextView(this);
        iconView.setText("↩️");
        iconView.setTextSize(36);
        iconView.setGravity(Gravity.CENTER);
        container.addView(iconView);

        TextView title = new TextView(this);
        title.setText("Confirm Undo");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, dpToPx(16), 0, dpToPx(8));
        title.setGravity(Gravity.CENTER);
        container.addView(title);

        TextView message = new TextView(this);
        int count = matchData.getUndoCount();
        message.setText("Are you sure you want to undo the last action? (" + count + " " + (count == 1 ? "step" : "steps") + " available in history)\nThis will revert all scores, balls, wickets, and stats.");
        message.setTextSize(13);
        message.setTextColor(Color.parseColor("#64748B"));
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 0, 0, dpToPx(24));
        container.addView(message);

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#475569"));
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setAllCaps(false);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dpToPx(48), 1.0f);
        cancelParams.setMargins(0, 0, dpToPx(8), 0);
        btnCancel.setLayoutParams(cancelParams);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        Button btnUndoAction = new Button(this);
        btnUndoAction.setText("Undo");
        btnUndoAction.setTextColor(Color.WHITE);
        btnUndoAction.setAllCaps(false);
        GradientDrawable undoBg = new GradientDrawable();
        undoBg.setColor(Color.parseColor("#EF4444"));
        undoBg.setCornerRadius(dpToPx(12));
        btnUndoAction.setBackground(undoBg);
        LinearLayout.LayoutParams undoParams = new LinearLayout.LayoutParams(0, dpToPx(48), 1.0f);
        btnUndoAction.setLayoutParams(undoParams);

        btnUndoAction.setOnClickListener(v -> {
            dialog.dismiss();
            matchData.undoLastEvent();
            strikerName = matchData.strikerName;
            nonStrikerName = matchData.nonStrikerName;
            tvStrikerName.setText(strikerName + " *");
            tvNonStrikerName.setText(nonStrikerName);
            tvBowlerName.setText(matchData.currentBowlerName);
            setScoringButtonsEnabled(true);
            updateScoreboardDisplay();
            updateThisOverDisplay();
            LiveScoreManager.getInstance().pushUndoUpdate(matchData, strikerName, nonStrikerName);
            int remaining = matchData.getUndoCount();
            Toast.makeText(MainActivity.this, "Undo successful! (" + remaining + " remaining)", Toast.LENGTH_SHORT).show();
        });

        btnLayout.addView(btnCancel);
        btnLayout.addView(btnUndoAction);
        container.addView(btnLayout);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.85), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void showPremiumRetireDialog() {
    final Dialog dialog = new Dialog(this);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    
    LinearLayout container = new LinearLayout(this);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setPadding(40, 40, 40, 40);
    
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(Color.WHITE);
    bg.setCornerRadius(40);
    container.setBackground(bg);

    TextView title = new TextView(this);
    title.setText("Retire Batsman");
    title.setTextSize(20);
    title.setTypeface(null, Typeface.BOLD);
    title.setPadding(0, 0, 0, 20);
    container.addView(title);

    if (matchData.retiredHurtList != null && !matchData.retiredHurtList.isEmpty()) {
        Button btnResumeHurt = new Button(this);
        btnResumeHurt.setText("🩹 Resume Retired Hurt Player (" + matchData.retiredHurtList.size() + ")");
        btnResumeHurt.setBackgroundColor(Color.parseColor("#10B981"));
        btnResumeHurt.setTextColor(Color.WHITE);
        btnResumeHurt.setAllCaps(false);
        btnResumeHurt.setTextSize(13);
        btnResumeHurt.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 110);
        lp.setMargins(0, 0, 0, 20);
        btnResumeHurt.setLayoutParams(lp);
        btnResumeHurt.setOnClickListener(v -> {
            dialog.dismiss();
            showResumeRetiredHurtDialog();
        });
        container.addView(btnResumeHurt);
    }

    // ✅ FIX: কে retire হবে তা user select করতে পারবে
    TextView tvWho = new TextView(this);
    tvWho.setText("Who is retiring?");
    tvWho.setTextSize(13);
    tvWho.setTextColor(Color.GRAY);
    tvWho.setPadding(0, 0, 0, 8);
    container.addView(tvWho);

    // Striker / Non-Striker toggle
    final boolean[] retireStriker = {true}; // default: striker retire হবে
    LinearLayout toggleRow = new LinearLayout(this);
    toggleRow.setOrientation(LinearLayout.HORIZONTAL);
    toggleRow.setPadding(0, 0, 0, 20);

    Button btnPickStriker = new Button(this);
    btnPickStriker.setText(strikerName + " *");
    btnPickStriker.setAllCaps(false);
    btnPickStriker.setTextSize(12);
    LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(0, 90, 1f);
    toggleParams.setMargins(0, 0, 8, 0);
    btnPickStriker.setLayoutParams(toggleParams);
    btnPickStriker.setBackgroundColor(Color.parseColor("#10B981")); // selected by default
    btnPickStriker.setTextColor(Color.WHITE);

    Button btnPickNonStriker = new Button(this);
    btnPickNonStriker.setText(nonStrikerName);
    btnPickNonStriker.setAllCaps(false);
    btnPickNonStriker.setTextSize(12);
    btnPickNonStriker.setLayoutParams(new LinearLayout.LayoutParams(0, 90, 1f));
    btnPickNonStriker.setBackgroundColor(Color.LTGRAY);
    btnPickNonStriker.setTextColor(Color.BLACK);

    btnPickStriker.setOnClickListener(v -> {
        retireStriker[0] = true;
        btnPickStriker.setBackgroundColor(Color.parseColor("#10B981"));
        btnPickStriker.setTextColor(Color.WHITE);
        btnPickNonStriker.setBackgroundColor(Color.LTGRAY);
        btnPickNonStriker.setTextColor(Color.BLACK);
    });
    btnPickNonStriker.setOnClickListener(v -> {
        retireStriker[0] = false;
        btnPickNonStriker.setBackgroundColor(Color.parseColor("#10B981"));
        btnPickNonStriker.setTextColor(Color.WHITE);
        btnPickStriker.setBackgroundColor(Color.LTGRAY);
        btnPickStriker.setTextColor(Color.BLACK);
    });
    toggleRow.addView(btnPickStriker);
    toggleRow.addView(btnPickNonStriker);
    container.addView(toggleRow);

    final EditText etNewPlayer = new EditText(this);
    etNewPlayer.setHint("Enter New Batsman Name");
    etNewPlayer.setPadding(20, 30, 20, 30);
    GradientDrawable etBg = new GradientDrawable();
    etBg.setStroke(2, Color.LTGRAY);
    etBg.setCornerRadius(15);
    etNewPlayer.setBackground(etBg);
    container.addView(etNewPlayer);

    LinearLayout btnRow = new LinearLayout(this);
    btnRow.setPadding(0, 30, 0, 0);

    Button btnHurt = new Button(this);
    btnHurt.setText("Retired Hurt");
    btnHurt.setBackgroundColor(Color.parseColor("#F59E0B"));
    btnHurt.setTextColor(Color.WHITE);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 110, 1f);
    p.setMargins(0, 0, 10, 0);
    btnHurt.setLayoutParams(p);
    btnHurt.setOnClickListener(v -> {
        String name = etNewPlayer.getText().toString().trim();
        if (name.isEmpty()) { Toast.makeText(this, "Enter new batsman name", Toast.LENGTH_SHORT).show(); return; }
        dialog.dismiss();

        // ✅ FIX: আসল retire logic — isHurt=true (wicket count বাড়ে না)
        if (retireStriker[0]) {
            matchData.retireStriker(true);
            strikerName = name;
            matchData.strikerName = name;
            matchData.resetStrikerStats();
            tvStrikerName.setText(name + " *");
        } else {
            // Non-striker retire
            matchData.retireNonStriker(true);
            nonStrikerName = name;
            matchData.nonStrikerName = name;
            matchData.resetNonStrikerStats();
            tvNonStrikerName.setText(name);
        }
        updateScoreboardDisplay();
        Toast.makeText(this, name + " is in — Retired Hurt recorded", Toast.LENGTH_SHORT).show();
    });

    Button btnOut = new Button(this);
    btnOut.setText("Retired Out");
    btnOut.setBackgroundColor(Color.parseColor("#EF4444"));
    btnOut.setTextColor(Color.WHITE);
    btnOut.setLayoutParams(new LinearLayout.LayoutParams(0, 110, 1f));
    btnOut.setOnClickListener(v -> {
        String name = etNewPlayer.getText().toString().trim();
        if (name.isEmpty()) { Toast.makeText(this, "Enter new batsman name", Toast.LENGTH_SHORT).show(); return; }
        dialog.dismiss();

        // ✅ FIX: আসল retire logic — isHurt=false (wicket count বাড়ে)
        if (retireStriker[0]) {
            matchData.retireStriker(false);
            strikerName = name;
            matchData.strikerName = name;
            matchData.resetStrikerStats();
            tvStrikerName.setText(name + " *");
        } else {
            // Non-striker retire
            matchData.retireNonStriker(false);
            nonStrikerName = name;
            matchData.nonStrikerName = name;
            matchData.resetNonStrikerStats();
            tvNonStrikerName.setText(name);
        }
        updateScoreboardDisplay();
        Toast.makeText(this, name + " is in — Retired Out recorded", Toast.LENGTH_SHORT).show();
    });

    btnRow.addView(btnHurt);
    btnRow.addView(btnOut);
    container.addView(btnRow);

    // Cancel button
    Button btnCancel = new Button(this);
    btnCancel.setText("Cancel");
    btnCancel.setAllCaps(false);
    btnCancel.setBackgroundColor(Color.TRANSPARENT);
    btnCancel.setTextColor(Color.GRAY);
    LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    cancelParams.setMargins(0, 10, 0, 0);
    btnCancel.setLayoutParams(cancelParams);
    btnCancel.setOnClickListener(v -> dialog.dismiss());
    container.addView(btnCancel);

    dialog.setContentView(container);
    if (dialog.getWindow() != null) {
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
    }
    dialog.show();
}

    private void showPremiumPenaltyDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(24), dpToPx(28), dpToPx(24), dpToPx(24));
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dpToPx(20));
        container.setBackground(bgShape);

        TextView title = new TextView(this);
        title.setText("⚠️ Penalty Runs");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, 0, 0, dpToPx(8));
        container.addView(title);

        TextView message = new TextView(this);
        message.setText("Award 5 penalty runs to:");
        message.setTextSize(14);
        message.setTextColor(Color.parseColor("#64748B"));
        message.setPadding(0, 0, 0, dpToPx(24));
        container.addView(message);

        Button btnBatting = new Button(this);
        btnBatting.setText("+5 to Batting Team");
        btnBatting.setTextColor(Color.WHITE);
        btnBatting.setAllCaps(false);
        GradientDrawable batBg = new GradientDrawable();
        batBg.setColor(Color.parseColor("#10B981"));
        batBg.setCornerRadius(dpToPx(12));
        btnBatting.setBackground(batBg);
        LinearLayout.LayoutParams batParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48));
        batParams.setMargins(0, 0, 0, dpToPx(12));
        btnBatting.setLayoutParams(batParams);
        btnBatting.setOnClickListener(v -> {
            dialog.dismiss();
            matchData.addPenaltyRuns(5);
            updateScoreboardDisplay();
            Toast.makeText(MainActivity.this, "5 Penalty runs awarded to Batting Team", Toast.LENGTH_SHORT).show();
        });
        container.addView(btnBatting);

        Button btnBowling = new Button(this);
        btnBowling.setText("+5 to Bowling Team");
        btnBowling.setTextColor(Color.WHITE);
        btnBowling.setAllCaps(false);
        GradientDrawable bowlBg = new GradientDrawable();
        bowlBg.setColor(Color.parseColor("#3B82F6"));
        bowlBg.setCornerRadius(dpToPx(12));
        btnBowling.setBackground(bowlBg);
        LinearLayout.LayoutParams bowlParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(48));
        bowlParams.setMargins(0, 0, 0, dpToPx(16));
        btnBowling.setLayoutParams(bowlParams);
        btnBowling.setOnClickListener(v -> {
            dialog.dismiss();
            // ✅ FIX: আগে এই বাটন শুধু Toast দেখাতো, কোনো রান যোগ হতো না।
            // বোলিং টিম যদি আগেই ব্যাট করে থাকে (২য় ইনিংস চলছে) তাদের
            // ১ম ইনিংসের স্কোর/টার্গেটে সরাসরি যোগ হবে। এখনো ব্যাট না করলে
            // রানটা জমা থাকবে এবং তাদের ইনিংস শুরু হলে স্কোরে যোগ হবে।
            matchData.addPenaltyRunsToBowlingTeam(5);
            updateScoreboardDisplay();
            String msg = matchData.isSecondInnings
                    ? "5 Penalty runs added to " + matchData.teamBattingFirst + "'s total"
                    : "5 Penalty runs will be added to " + matchData.teamBattingSecond + "'s total when their innings begins";
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
        });
        container.addView(btnBowling);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setTextSize(16);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setPadding(0, dpToPx(8), 0, 0);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        container.addView(btnCancel);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.85), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void showPremiumInjuredDialog() {
    final Dialog dialog = new Dialog(this);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    
    LinearLayout container = new LinearLayout(this);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setPadding(40, 40, 40, 40);
    
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(Color.WHITE);
    bg.setCornerRadius(40);
    container.setBackground(bg);

    TextView title = new TextView(this);
    title.setText("Change Bowler (Injured)");
    title.setTextSize(18);
    title.setTypeface(null, Typeface.BOLD);
    title.setPadding(0, 0, 0, 20);
    container.addView(title);

    final EditText etNewBowler = new EditText(this);
    etNewBowler.setHint("Enter New Bowler Name");
    etNewBowler.setPadding(20, 30, 20, 30);
    GradientDrawable etBg = new GradientDrawable();
    etBg.setStroke(2, Color.LTGRAY);
    etBg.setCornerRadius(15);
    etNewBowler.setBackground(etBg);
    container.addView(etNewBowler);

    Button btnConfirm = new Button(this);
    btnConfirm.setText("Replace Bowler");
    btnConfirm.setTextColor(Color.WHITE);
    GradientDrawable bBg = new GradientDrawable();
    bBg.setColor(Color.parseColor("#6366F1"));
    bBg.setCornerRadius(15);
    btnConfirm.setBackground(bBg);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 110);
    lp.setMargins(0, 30, 0, 0);
    btnConfirm.setLayoutParams(lp);
    btnConfirm.setOnClickListener(v -> {
        String name = etNewBowler.getText().toString().trim();
        if(name.isEmpty()) { Toast.makeText(this, "Enter name", Toast.LENGTH_SHORT).show(); return; }
        if (name.equalsIgnoreCase(matchData.currentBowlerName)) {
            Toast.makeText(this, "Injured bowler-এর নামই আবার দেওয়া যাবে না", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ FIX: আগে এখানে কোনো লজিকই ছিল না ("TODO" ছিল) — বোলার
        // ইনজুরড হলে বাস্তবে কিছুই বদলাতো না। এখন over মাঝপথে থামিয়ে
        // (strike/over সংখ্যা অপরিবর্তিত রেখে) নতুন বোলার সেট করা হচ্ছে।
        matchData.replaceBowlerMidOver(name);
        tvBowlerName.setText(name);
        updateScoreboardDisplay();
        updateThisOverDisplay();
        Toast.makeText(this, name + " is now bowling (replaced injured bowler)", Toast.LENGTH_SHORT).show();

        dialog.dismiss();
    });
    container.addView(btnConfirm);

    dialog.setContentView(container);
    if (dialog.getWindow() != null) {
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
    }
    dialog.show();
}


    private void swapBatsmenNames() {
        String temp = strikerName; strikerName = nonStrikerName; nonStrikerName = temp;
        tvStrikerName.setText(strikerName + " *"); tvNonStrikerName.setText(nonStrikerName);
    }

    private void setScoringButtonsEnabled(boolean enabled) {
        for (Button button : runButtons) if (button != null) button.setEnabled(enabled);
        cbWide.setEnabled(enabled); cbNoBall.setEnabled(enabled); cbByes.setEnabled(enabled); cbLegByes.setEnabled(enabled); cbWicket.setEnabled(enabled); 
        btnUndo.setEnabled(enabled); btnSwap.setEnabled(enabled); btnPenalty.setEnabled(enabled); btnRetire.setEnabled(enabled); btnInjured.setEnabled(enabled);
    }

    private void updateScoreboardDisplay() {
        if (matchData.isTestMatch) {
            String suffix = matchData.currentInnings == 1 ? "1st" : matchData.currentInnings == 2 ? "2nd" : matchData.currentInnings == 3 ? "3rd" : "4th";
            tvBattingTeam.setText("Day " + matchData.currentDay + " • Sess " + matchData.currentSession + " (" + suffix + " Inn)");
            tvRRR.setVisibility(View.GONE);
            tvMatchEquation.setText(matchData.getTestMatchLeadTrailStatus());
            tvMatchEquation.setVisibility(View.VISIBLE);
        } else if (matchData.isSecondInnings) {
            tvBattingTeam.setText("2nd Innings");
            int runsNeeded = matchData.targetRuns - matchData.totalRuns;
            int totalBalls = Integer.parseInt(matchData.totalOvers) * 6;
            int ballsRemaining = totalBalls - matchData.ballsBowled;
            double rrr = (ballsRemaining > 0) ? (runsNeeded * 6.0) / ballsRemaining : 0.0;
            if(rrr < 0) rrr = 0.0;
            tvRRR.setText("RRR: " + String.format("%.2f", rrr)); tvRRR.setVisibility(View.VISIBLE);
            tvMatchEquation.setText(runsNeeded <= 0 ? "Match Won!" : ballsRemaining <= 0 ? "Match Lost/Tied" : "Need " + runsNeeded + " runs in " + ballsRemaining + " balls");
            tvMatchEquation.setVisibility(View.VISIBLE);
        } else {
            tvBattingTeam.setText("1st Innings"); tvRRR.setVisibility(View.GONE); tvMatchEquation.setVisibility(View.GONE);
        }

        tvTotalScore.setText(matchData.getBattingTeamName() + "  " + matchData.getScoreString());
        tvOvers.setText("(" + matchData.getOversString() + ")");
        tvExtrasDetail.setText(matchData.getExtrasString());
        tvPartnership.setText(matchData.getPartnershipString());
        // ✅ FIX #1: Show Run Rate setting অনুযায়ী CRR দেখানো বা লুকানো হচ্ছে।
        SharedPreferences appPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean showRunRate = appPrefs.getBoolean("SHOW_RUN_RATE", true);
        tvCRR.setText("CRR: " + matchData.getCRR());
        tvCRR.setVisibility(showRunRate ? View.VISIBLE : View.GONE);
        tvStrikerRuns.setText(String.valueOf(matchData.strikerRuns)); tvStrikerBalls.setText(String.valueOf(matchData.strikerBalls)); tvStriker4s.setText(String.valueOf(matchData.striker4s)); tvStriker6s.setText(String.valueOf(matchData.striker6s)); tvStrikerSR.setText(matchData.getStrikerSR());
        tvNonStrikerRuns.setText(String.valueOf(matchData.nonStrikerRuns)); tvNonStrikerBalls.setText(String.valueOf(matchData.nonStrikerBalls)); tvNonStriker4s.setText(String.valueOf(matchData.nonStriker4s)); tvNonStriker6s.setText(String.valueOf(matchData.nonStriker6s)); tvNonStrikerSR.setText(matchData.getNonStrikerSR());
        tvBowlerOvers.setText(matchData.getBowlerFigures()); tvBowlerMaidens.setText(String.valueOf(matchData.currentBowlerMaidens)); tvBowlerRuns.setText(String.valueOf(matchData.bowlerRuns)); tvBowlerWickets.setText(String.valueOf(matchData.bowlerWickets)); tvBowlerER.setText(matchData.getBowlerER());
        updateThisOverDisplay();

        // 💾 Room Database Auto-Save: Save ball-by-ball progress asynchronously on every score update
        com.cricketscorez.proapp.room.LiveMatchProgressRepository.autoSave(this, matchData);
    }

    private void updateThisOverDisplay() {
        layoutThisOver.removeAllViews();
        int legalBallCount = 0;
        for (BallEvent event : matchData.currentOverBalls) {
            // 6টি legal ball এর পর আর ball count হবে না
            if (legalBallCount >= 6) break;
            TextView ballView = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(35), dpToPx(35));
            params.setMargins(0, 0, dpToPx(6), 0);
            ballView.setLayoutParams(params);
            ballView.setGravity(Gravity.CENTER);
            ballView.setTextSize(12);
            String text = String.valueOf(event.runs);
            int bgRes = R.drawable.bg_circle_green;
            int textColor = Color.parseColor("#00796B");
            // ✅ FIX: আগে if/else if চেইনের কারণে wicket + extra (wide/no-ball)
            // একসাথে হলে শুধু "W" দেখাতো, wd/nb তথ্য হারিয়ে যেত।
            // এখন isWicket ও isExtra দুটোই আলাদাভাবে চেক করে combine করা হচ্ছে,
            // যাতে "W+wd" বা "W+nb" এর মতো সঠিক তথ্য দেখানো যায়।
            if (event.isWicket && event.isExtra) {
                String extraTxt = "";
                if (event.extraType.equals("WD")) extraTxt = "wd";
                else if (event.extraType.equals("NB")) extraTxt = "nb";
                else if (event.extraType.equals("BYE")) extraTxt = "b";
                else if (event.extraType.equals("LB")) extraTxt = "lb";
                text = "W+" + extraTxt;
                textColor = Color.RED;
                ballView.setTextSize(9);
            } else if (event.isWicket) {
                text = "W"; textColor = Color.RED;
            } else if (event.isExtra) {
                if (event.extraType.equals("WD")) {
                    int bRuns = event.runs - 1;
                    text = bRuns > 0 ? "wd+" + bRuns : "wd";
                } else if (event.extraType.equals("NB")) {
                    int bRuns = event.runs - 1;
                    text = bRuns > 0 ? "nb+" + bRuns : "nb";
                } else if (event.extraType.equals("BYE")) {
                    text = event.runs > 1 ? event.runs + "b" : "1b";
                } else if (event.extraType.equals("LB")) {
                    text = event.runs > 1 ? event.runs + "lb" : "1lb";
                }
            } else if (event.runs == 0) {
                text = "•"; textColor = Color.GRAY;
            } else if (event.runs == 4 || event.runs == 6) {
                bgRes = R.drawable.bg_circle_green_solid; textColor = Color.WHITE;
            }
            ballView.setBackgroundResource(bgRes);
            ballView.setTextColor(textColor);
            ballView.setText(text);
            layoutThisOver.addView(ballView);
            if (event.isLegalBall) legalBallCount++;
        }
    }

        private int dpToPx(int dp) { return Math.round((float) dp * getResources().getDisplayMetrics().density); }
    private void setRunButtonListeners() { 
        runButtons[0]=findViewById(R.id.btnRun0); runButtons[1]=findViewById(R.id.btnRun1); runButtons[2]=findViewById(R.id.btnRun2); 
        runButtons[3]=findViewById(R.id.btnRun3); runButtons[4]=findViewById(R.id.btnRun4); runButtons[5]=findViewById(R.id.btnRun5); runButtons[6]=findViewById(R.id.btnRun6); 
        for(int i=0; i<runButtons.length; i++) { 
            final int r=i; 
            if(runButtons[i]!=null) 
                runButtons[i].setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { recordScoringEvent(r); } }); 
        } 
    }
    // ── নিজস্ব MySQL সার্ভারে লাইভ স্কোর আপডেট করার মেথড ───────────────────────
    private void syncScoreToServer() {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);

        // আপনার কোড অনুযায়ী বর্তমান ডাটা থেকে ভ্যালুগুলো নেওয়া হচ্ছে
        // Match ID হিসেবে বর্তমানে একটি ডামি "1" ব্যবহার করা হচ্ছে। 
        // আপনি ফিক্সচার থেকে প্রাপ্ত আইডি এখানে ডায়নামিকভাবে পাঠাতে পারেন।
        String matchId = "1"; 
        
        int runs = matchData.totalRuns;
        int wickets = matchData.totalWickets;
        String overs = tvOvers.getText().toString().replace("Overs: ", "").trim();
        int target = matchData.targetRuns;
        int innings = matchData.isSecondInnings ? 2 : 1;
        String status = "Live";

        apiInterface.updateLiveScore(matchId, runs, wickets, overs, target, innings, status)
            .enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d("LIVE_SYNC", "Score updated on server!");
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e("LIVE_ERROR", "Sync failed: " + t.getMessage());
                }
            });
    }

    // =================================================================================
    // ⚡ MATCH TOOLS & ADVANCED CONTROLS (Features 2, 3, 4, 6, 7, 8 & DLS Method)
    // =================================================================================

    private void showMatchToolsMenu() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(20));

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dpToPx(20));
        container.setBackground(bgShape);

        TextView title = new TextView(this);
        title.setText("⚡ Match Tools & Controls");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#0F172A"));
        title.setPadding(0, 0, 0, dpToPx(16));
        title.setGravity(Gravity.CENTER);
        container.addView(title);

        // 1. Edit Names Mid-Match (Feature 2)
        container.addView(createToolMenuItem("✏️ Edit Match & Player Names", "Modify team names or active batsman/bowler names", v -> {
            dialog.dismiss();
            showEditNamesDialog();
        }));

        if (matchData.isTestMatch) {
            container.addView(createToolMenuItem("⏳ Test Match Day & Session Controls", "Day " + matchData.currentDay + " of " + matchData.testDays + " • Session " + matchData.currentSession + " • Stumps & Draw options", v -> {
                dialog.dismiss();
                showTestSessionControlsDialog();
            }));

            if (matchData.currentInnings == 2 && matchData.canEnforceFollowOn()) {
                container.addView(createToolMenuItem("🏏 Follow-On Decision", "Enforce or decline follow-on (Lead: " + (matchData.firstInningsScore - matchData.secondInningsScore) + " runs)", v -> {
                    dialog.dismiss();
                    showFollowOnChoiceDialog();
                }));
            }
        }

        // 2. D/L Method (Duckworth-Lewis-Stern)
        if (!matchData.isTestMatch) {
            container.addView(createToolMenuItem("🌧️ D/L Method (DLS Calculator)", "Rain interruption, par score & revised target setup", v -> {
                dialog.dismiss();
                showDlsCalculatorDialog();
            }));
        }

        // 3. Declare / End Current Innings (Feature 7)
        container.addView(createToolMenuItem("🚪 Declare / End Innings", "Voluntarily close current batting innings", v -> {
            dialog.dismiss();
            showDeclareInningsDialog();
        }));

        // 4. Super Over Mode (Feature 8)
        container.addView(createToolMenuItem("⚡ Super Over Mode", "1-over tie-breaker shoot-out (2 wickets max)", v -> {
            dialog.dismiss();
            showSuperOverDialog();
        }));

        // 5. Resume Retired Hurt Player (Feature 3)
        int hurtCount = (matchData.retiredHurtList != null) ? matchData.retiredHurtList.size() : 0;
        container.addView(createToolMenuItem("🩹 Resume Retired Hurt Batsman (" + hurtCount + ")", "Bring retired hurt player back to the crease", v -> {
            dialog.dismiss();
            showResumeRetiredHurtDialog();
        }));

        // 6. Partnership Graph (Feature 6)
        container.addView(createToolMenuItem("🤝 Partnership Analysis Graph", "View all batting stands and contribution charts", v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, PartnershipGraphActivity.class);
            intent.putExtra("TEAM_1", matchData.teamBattingFirst);
            intent.putExtra("TEAM_2", matchData.teamBattingSecond);
            intent.putExtra("MATCH_DATA", matchData);
            startActivity(intent);
        }));

        // Close button
        Button btnClose = new Button(this);
        btnClose.setText("Close");
        btnClose.setTextColor(Color.parseColor("#64748B"));
        btnClose.setBackgroundColor(Color.TRANSPARENT);
        btnClose.setAllCaps(false);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        container.addView(btnClose);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private View createToolMenuItem(String title, String subtitle, View.OnClickListener onClickListener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));
        row.setClickable(true);
        row.setFocusable(true);

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(Color.parseColor("#F8FAFC"));
        rowBg.setStroke(dpToPx(1), Color.parseColor("#E2E8F0"));
        rowBg.setCornerRadius(dpToPx(12));
        row.setBackground(rowBg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(8));
        row.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#1E293B"));
        row.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextSize(11);
        tvSub.setTextColor(Color.parseColor("#64748B"));
        row.addView(tvSub);

        row.setOnClickListener(onClickListener);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FEATURE 2: EDIT NAMES MID-MATCH (Team & Player Names)
    // ─────────────────────────────────────────────────────────────────────────
    private void showEditNamesDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(20));

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dpToPx(20));
        container.setBackground(bgShape);

        TextView title = new TextView(this);
        title.setText("✏️ Edit Match & Player Names");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, 0, 0, dpToPx(14));
        container.addView(title);

        // Team 1
        TextView tvT1 = new TextView(this);
        tvT1.setText("Team 1 Name:");
        tvT1.setTextSize(12);
        tvT1.setTextColor(Color.GRAY);
        container.addView(tvT1);

        final EditText etTeam1 = new EditText(this);
        etTeam1.setText(matchData.team1Name != null ? matchData.team1Name : "");
        applyInputStyle(etTeam1);
        container.addView(etTeam1);

        // Team 2
        TextView tvT2 = new TextView(this);
        tvT2.setText("Team 2 Name:");
        tvT2.setTextSize(12);
        tvT2.setTextColor(Color.GRAY);
        container.addView(tvT2);

        final EditText etTeam2 = new EditText(this);
        etTeam2.setText(matchData.team2Name != null ? matchData.team2Name : "");
        applyInputStyle(etTeam2);
        container.addView(etTeam2);

        // Striker
        TextView tvS = new TextView(this);
        tvS.setText("Striker Batsman:");
        tvS.setTextSize(12);
        tvS.setTextColor(Color.GRAY);
        container.addView(tvS);

        final EditText etStriker = new EditText(this);
        etStriker.setText(strikerName != null ? strikerName : "");
        applyInputStyle(etStriker);
        container.addView(etStriker);

        // Non-Striker
        TextView tvNS = new TextView(this);
        tvNS.setText("Non-Striker Batsman:");
        tvNS.setTextSize(12);
        tvNS.setTextColor(Color.GRAY);
        container.addView(tvNS);

        final EditText etNonStriker = new EditText(this);
        etNonStriker.setText(nonStrikerName != null ? nonStrikerName : "");
        applyInputStyle(etNonStriker);
        container.addView(etNonStriker);

        // Bowler
        TextView tvB = new TextView(this);
        tvB.setText("Current Bowler:");
        tvB.setTextSize(12);
        tvB.setTextColor(Color.GRAY);
        container.addView(tvB);

        final EditText etBowler = new EditText(this);
        etBowler.setText(matchData.currentBowlerName != null ? matchData.currentBowlerName : "");
        applyInputStyle(etBowler);
        container.addView(etBowler);

        // Buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dpToPx(16), 0, 0);

        Button btnSave = new Button(this);
        btnSave.setText("Save Names");
        btnSave.setTextColor(Color.WHITE);
        btnSave.setBackgroundColor(Color.parseColor("#16A34A"));
        btnSave.setAllCaps(false);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, dpToPx(44), 1f);
        p1.setMargins(0, 0, dpToPx(8), 0);
        btnSave.setLayoutParams(p1);
        btnSave.setOnClickListener(v -> {
            String newT1 = etTeam1.getText().toString().trim();
            String newT2 = etTeam2.getText().toString().trim();
            String newS  = etStriker.getText().toString().trim();
            String newNS = etNonStriker.getText().toString().trim();
            String newB  = etBowler.getText().toString().trim();

            if (newT1.isEmpty() || newT2.isEmpty()) {
                Toast.makeText(this, "Team names cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newS.isEmpty() || newNS.isEmpty()) {
                Toast.makeText(this, "Batsmen names cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            matchData.recordManualSwap(); // Save snapshot for undo integrity

            matchData.team1Name = newT1;
            matchData.team2Name = newT2;
            if (!matchData.isSecondInnings) {
                matchData.teamBattingFirst = newT1;
                matchData.teamBattingSecond = newT2;
            } else {
                matchData.teamBattingFirst = newT2;
                matchData.teamBattingSecond = newT1;
            }

            strikerName = newS;
            matchData.strikerName = newS;
            nonStrikerName = newNS;
            matchData.nonStrikerName = newNS;

            if (!newB.isEmpty()) {
                matchData.currentBowlerName = newB;
                tvBowlerName.setText(newB);
            }

            tvMatchTitle.setText(newT1 + " vs " + newT2);
            tvStrikerName.setText(newS + " *");
            tvNonStrikerName.setText(newNS);
            updateScoreboardDisplay();

            dialog.dismiss();
            Toast.makeText(this, "Match and player names updated!", Toast.LENGTH_SHORT).show();
        });
        btnRow.addView(btnSave);

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setBackgroundColor(Color.LTGRAY);
        btnCancel.setAllCaps(false);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(0, dpToPx(44), 1f));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnCancel);

        container.addView(btnRow);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void applyInputStyle(EditText et) {
        et.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        et.setTextSize(13);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F8FAFC"));
        bg.setStroke(dpToPx(1), Color.parseColor("#CBD5E1"));
        bg.setCornerRadius(dpToPx(8));
        et.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(40));
        p.setMargins(0, dpToPx(4), 0, dpToPx(10));
        et.setLayoutParams(p);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FEATURE 3: RESUME RETIRED HURT BATSMAN
    // ─────────────────────────────────────────────────────────────────────────
    private void showResumeRetiredHurtDialog() {
        if (matchData.retiredHurtList == null || matchData.retiredHurtList.isEmpty()) {
            Toast.makeText(this, "No batsman is currently Retired Hurt.", Toast.LENGTH_SHORT).show();
            return;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(20));

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dpToPx(20));
        container.setBackground(bgShape);

        TextView title = new TextView(this);
        title.setText("🩹 Resume Retired Hurt Batsman");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, 0, 0, dpToPx(12));
        container.addView(title);

        TextView prompt = new TextView(this);
        prompt.setText("Select a player to return to the crease:");
        prompt.setTextSize(13);
        prompt.setTextColor(Color.parseColor("#64748B"));
        prompt.setPadding(0, 0, 0, dpToPx(12));
        container.addView(prompt);

        for (final MatchData.RetiredHurtRecord record : matchData.retiredHurtList) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(Color.parseColor("#F0FDF4"));
            rowBg.setStroke(dpToPx(1), Color.parseColor("#BBF7D0"));
            rowBg.setCornerRadius(dpToPx(10));
            row.setBackground(rowBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dpToPx(8));
            row.setLayoutParams(lp);

            TextView tvName = new TextView(this);
            tvName.setText(record.batsmanName);
            tvName.setTextSize(15);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(Color.parseColor("#166534"));
            row.addView(tvName);

            TextView tvStats = new TextView(this);
            tvStats.setText(record.runs + " runs (" + record.balls + "b, " + record.fours + "x4, " + record.sixes + "x6)");
            tvStats.setTextSize(12);
            tvStats.setTextColor(Color.parseColor("#15803D"));
            row.addView(tvStats);

            row.setOnClickListener(v -> {
                dialog.dismiss();
                promptWhichBatsmanToReplace(record);
            });
            container.addView(row);
        }

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setAllCaps(false);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        container.addView(btnCancel);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void promptWhichBatsmanToReplace(final MatchData.RetiredHurtRecord record) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(20));

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dpToPx(20));
        container.setBackground(bgShape);

        TextView title = new TextView(this);
        title.setText("Resume " + record.batsmanName);
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, 0, 0, dpToPx(10));
        container.addView(title);

        TextView msg = new TextView(this);
        msg.setText("Which position should " + record.batsmanName + " resume at?");
        msg.setTextSize(13);
        msg.setTextColor(Color.parseColor("#64748B"));
        msg.setPadding(0, 0, 0, dpToPx(16));
        container.addView(msg);

        // Replace Striker
        Button btnReplaceStriker = new Button(this);
        btnReplaceStriker.setText("As Striker (Replace " + strikerName + ")");
        btnReplaceStriker.setTextColor(Color.WHITE);
        btnReplaceStriker.setBackgroundColor(Color.parseColor("#16A34A"));
        btnReplaceStriker.setAllCaps(false);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
        p1.setMargins(0, 0, 0, dpToPx(8));
        btnReplaceStriker.setLayoutParams(p1);
        btnReplaceStriker.setOnClickListener(v -> {
            d.dismiss();
            matchData.resumeRetiredHurtBatsman(record, true);
            strikerName = matchData.strikerName;
            tvStrikerName.setText(strikerName + " *");
            updateScoreboardDisplay();
            Toast.makeText(this, record.batsmanName + " resumed at striker!", Toast.LENGTH_SHORT).show();
        });
        container.addView(btnReplaceStriker);

        // Replace Non-Striker
        Button btnReplaceNonStriker = new Button(this);
        btnReplaceNonStriker.setText("As Non-Striker (Replace " + nonStrikerName + ")");
        btnReplaceNonStriker.setTextColor(Color.WHITE);
        btnReplaceNonStriker.setBackgroundColor(Color.parseColor("#0284C7"));
        btnReplaceNonStriker.setAllCaps(false);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
        p2.setMargins(0, 0, 0, dpToPx(8));
        btnReplaceNonStriker.setLayoutParams(p2);
        btnReplaceNonStriker.setOnClickListener(v -> {
            d.dismiss();
            matchData.resumeRetiredHurtBatsman(record, false);
            nonStrikerName = matchData.nonStrikerName;
            tvNonStrikerName.setText(nonStrikerName);
            updateScoreboardDisplay();
            Toast.makeText(this, record.batsmanName + " resumed at non-striker!", Toast.LENGTH_SHORT).show();
        });
        container.addView(btnReplaceNonStriker);

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setAllCaps(false);
        btnCancel.setOnClickListener(v -> d.dismiss());
        container.addView(btnCancel);

        d.setContentView(container);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.88), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        d.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DLS METHOD (Duckworth–Lewis–Stern)
    // ─────────────────────────────────────────────────────────────────────────
    private void showDlsCalculatorDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(20));

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dpToPx(20));
        container.setBackground(bgShape);

        TextView title = new TextView(this);
        title.setText("🌧️ D/L Method (DLS Calculator)");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, 0, 0, dpToPx(10));
        container.addView(title);

        TextView sub = new TextView(this);
        sub.setText("ICC Standard Duckworth-Lewis-Stern Revised Target Engine");
        sub.setTextSize(12);
        sub.setTextColor(Color.parseColor("#64748B"));
        sub.setPadding(0, 0, 0, dpToPx(12));
        container.addView(sub);

        // Default values
        int defaultSchedOvers = 20;
        try { defaultSchedOvers = Integer.parseInt(matchData.totalOvers); } catch (Exception ignored) {}
        int defaultT1Runs = matchData.isSecondInnings ? (matchData.targetRuns - 1) : matchData.totalRuns;
        int defaultT1Wkts = matchData.isSecondInnings ? 10 : matchData.totalWickets;
        int defaultT2Overs = defaultSchedOvers > 5 ? (defaultSchedOvers - 5) : defaultSchedOvers;

        // Scheduled Overs
        TextView tvSO = new TextView(this);
        tvSO.setText("Original Scheduled Overs (e.g. 20, 50):");
        tvSO.setTextSize(12);
        tvSO.setTextColor(Color.GRAY);
        container.addView(tvSO);
        final EditText etSchedOvers = new EditText(this);
        etSchedOvers.setInputType(InputType.TYPE_CLASS_NUMBER);
        etSchedOvers.setText(String.valueOf(defaultSchedOvers));
        applyInputStyle(etSchedOvers);
        container.addView(etSchedOvers);

        // Team 1 Runs
        TextView tvT1R = new TextView(this);
        tvT1R.setText("Team 1 Total Runs Scored:");
        tvT1R.setTextSize(12);
        tvT1R.setTextColor(Color.GRAY);
        container.addView(tvT1R);
        final EditText etT1Runs = new EditText(this);
        etT1Runs.setInputType(InputType.TYPE_CLASS_NUMBER);
        etT1Runs.setText(String.valueOf(defaultT1Runs));
        applyInputStyle(etT1Runs);
        container.addView(etT1Runs);

        // Team 2 Revised Overs
        TextView tvT2O = new TextView(this);
        tvT2O.setText("Team 2 Revised Overs Available (after rain interruption):");
        tvT2O.setTextSize(12);
        tvT2O.setTextColor(Color.GRAY);
        container.addView(tvT2O);
        final EditText etT2Overs = new EditText(this);
        etT2Overs.setInputType(InputType.TYPE_CLASS_NUMBER);
        etT2Overs.setText(String.valueOf(defaultT2Overs));
        applyInputStyle(etT2Overs);
        container.addView(etT2Overs);

        // Calculation Output Card
        final LinearLayout resultCard = new LinearLayout(this);
        resultCard.setOrientation(LinearLayout.VERTICAL);
        resultCard.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        GradientDrawable resBg = new GradientDrawable();
        resBg.setColor(Color.parseColor("#EFF6FF"));
        resBg.setStroke(dpToPx(1), Color.parseColor("#BFDBFE"));
        resBg.setCornerRadius(dpToPx(12));
        resultCard.setBackground(resBg);
        LinearLayout.LayoutParams lpRes = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpRes.setMargins(0, 0, 0, dpToPx(14));
        resultCard.setLayoutParams(lpRes);

        final TextView tvResultText = new TextView(this);
        tvResultText.setText("Tap 'Calculate Revised Target' to compute.");
        tvResultText.setTextSize(13);
        tvResultText.setTextColor(Color.parseColor("#1D4ED8"));
        resultCard.addView(tvResultText);
        container.addView(resultCard);

        final int[] computedTarget = {0};
        final int[] computedOvers = {0};

        // Calculate Button
        Button btnCalc = new Button(this);
        btnCalc.setText("Calculate Revised Target");
        btnCalc.setTextColor(Color.WHITE);
        btnCalc.setBackgroundColor(Color.parseColor("#2563EB"));
        btnCalc.setAllCaps(false);
        btnCalc.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams pCalc = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
        pCalc.setMargins(0, 0, 0, dpToPx(8));
        btnCalc.setLayoutParams(pCalc);
        btnCalc.setOnClickListener(v -> {
            try {
                int sO = Integer.parseInt(etSchedOvers.getText().toString().trim());
                int t1R = Integer.parseInt(etT1Runs.getText().toString().trim());
                int t2O = Integer.parseInt(etT2Overs.getText().toString().trim());

                if (t2O > sO) {
                    Toast.makeText(this, "Team 2 overs cannot exceed scheduled overs", Toast.LENGTH_SHORT).show();
                    return;
                }

                int target = DlsCalculator.calculateRevisedTarget(sO, t1R, defaultT1Wkts, t2O);
                double r1 = DlsCalculator.calculateResource(sO, 0);
                double r2 = DlsCalculator.calculateResource(t2O, 0);
                double rrr = t2O > 0 ? (double) target / t2O : 0.0;

                computedTarget[0] = target;
                computedOvers[0] = t2O;

                String out = "🎯 Revised Target: " + target + " runs in " + t2O + " overs\n" +
                             "📊 Required Run Rate: " + String.format(java.util.Locale.US, "%.2f", rrr) + "\n" +
                             "📈 Team 1 Resource: " + String.format(java.util.Locale.US, "%.1f", r1) + "% | Team 2 Resource: " + String.format(java.util.Locale.US, "%.1f", r2) + "%";
                tvResultText.setText(out);
            } catch (Exception e) {
                Toast.makeText(this, "Please enter valid numeric inputs", Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(btnCalc);

        // Apply Target Button
        Button btnApply = new Button(this);
        btnApply.setText("Apply DLS Target to Match");
        btnApply.setTextColor(Color.WHITE);
        btnApply.setBackgroundColor(Color.parseColor("#16A34A"));
        btnApply.setAllCaps(false);
        btnApply.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams pApply = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
        pApply.setMargins(0, 0, 0, dpToPx(8));
        btnApply.setLayoutParams(pApply);
        btnApply.setOnClickListener(v -> {
            if (computedTarget[0] <= 0) {
                Toast.makeText(this, "Please calculate the target first", Toast.LENGTH_SHORT).show();
                return;
            }

            matchData.recordManualSwap(); // undo snapshot
            matchData.isDlsApplied = true;
            matchData.dlsTargetRuns = computedTarget[0];
            matchData.dlsRevisedOvers = computedOvers[0];
            matchData.targetRuns = computedTarget[0];
            matchData.totalOvers = String.valueOf(computedOvers[0]);

            updateScoreboardDisplay();
            dialog.dismiss();
            Toast.makeText(this, "DLS target applied: " + computedTarget[0] + " runs in " + computedOvers[0] + " overs", Toast.LENGTH_LONG).show();
        });
        container.addView(btnApply);

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setAllCaps(false);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        container.addView(btnCancel);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FEATURE 7: DECLARE / END INNINGS
    // ─────────────────────────────────────────────────────────────────────────
    private void showDeclareInningsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(20));

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dpToPx(20));
        container.setBackground(bgShape);

        TextView title = new TextView(this);
        title.setText("🚪 Declare / End Innings");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, 0, 0, dpToPx(10));
        container.addView(title);

        String curTeam = matchData.getBattingTeamName();
        TextView msg = new TextView(this);
        msg.setText("Current Innings: " + curTeam + " (" + (matchData.isTestMatch ? "Innings " + matchData.currentInnings : (matchData.isSecondInnings ? "2nd Inn" : "1st Inn")) + ")\nScore: " + matchData.totalRuns + "/" + matchData.totalWickets + " in " + matchData.getOversString() + " ov.\n\nAre you sure you want to declare and close this innings now?");
        msg.setTextSize(13);
        msg.setTextColor(Color.parseColor("#475569"));
        msg.setPadding(0, 0, 0, dpToPx(16));
        container.addView(msg);

        Button btnDeclare = new Button(this);
        btnDeclare.setText(matchData.isTestMatch ? "Declare Innings " + matchData.currentInnings : (matchData.isSecondInnings ? "Declare & End Match" : "Declare & Switch Innings"));
        btnDeclare.setTextColor(Color.WHITE);
        btnDeclare.setBackgroundColor(Color.parseColor("#DC2626"));
        btnDeclare.setAllCaps(false);
        btnDeclare.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
        p1.setMargins(0, 0, 0, dpToPx(8));
        btnDeclare.setLayoutParams(p1);
        btnDeclare.setOnClickListener(v -> {
            dialog.dismiss();
            if (matchData.isTestMatch) {
                matchData.recordManualSwap();
                Toast.makeText(this, "Innings " + matchData.currentInnings + " declared closed!", Toast.LENGTH_SHORT).show();
                handleTestInningsFinished();
            } else if (!matchData.isSecondInnings) {
                // End 1st innings
                matchData.recordManualSwap();
                showInningsBreakDialog();
                Toast.makeText(this, "1st Innings declared closed!", Toast.LENGTH_SHORT).show();
            } else {
                // End 2nd innings / match
                matchData.recordManualSwap();
                if (matchData.totalRuns >= matchData.targetRuns) {
                    int wicketsLeft = 10 - matchData.totalWickets;
                    showMatchResultDialog(matchData.teamBattingSecond + " won by " + wicketsLeft + " wickets!");
                } else if (matchData.totalRuns < matchData.targetRuns - 1) {
                    int runsMargin = (matchData.targetRuns - 1) - matchData.totalRuns;
                    showMatchResultDialog(matchData.teamBattingFirst + " won by " + runsMargin + " runs (by declaration)!");
                } else {
                    showMatchResultDialog("Match Tied!");
                }
                Toast.makeText(this, "Match completed by declaration!", Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(btnDeclare);

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setAllCaps(false);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        container.addView(btnCancel);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FEATURE 8: SUPER OVER MODE
    // ─────────────────────────────────────────────────────────────────────────
    private void showSuperOverDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(20));

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dpToPx(20));
        container.setBackground(bgShape);

        TextView title = new TextView(this);
        title.setText("⚡ Super Over Mode");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, 0, 0, dpToPx(10));
        container.addView(title);

        TextView desc = new TextView(this);
        desc.setText("ICC Standard Super Over Rules:\n• Exactly 1 Over (6 legal deliveries)\n• Maximum 2 wickets (innings ends at 2 wickets)\n• All-out at 2 wickets");
        desc.setTextSize(12);
        desc.setTextColor(Color.parseColor("#64748B"));
        desc.setPadding(0, 0, 0, dpToPx(14));
        container.addView(desc);

        // Striker
        TextView tvS = new TextView(this);
        tvS.setText("Super Over Striker:");
        tvS.setTextSize(12);
        tvS.setTextColor(Color.GRAY);
        container.addView(tvS);
        final EditText etS = new EditText(this);
        etS.setHint("Batsman 1");
        etS.setText(strikerName != null ? strikerName : "");
        applyInputStyle(etS);
        container.addView(etS);

        // Non-Striker
        TextView tvNS = new TextView(this);
        tvNS.setText("Super Over Non-Striker:");
        tvNS.setTextSize(12);
        tvNS.setTextColor(Color.GRAY);
        container.addView(tvNS);
        final EditText etNS = new EditText(this);
        etNS.setHint("Batsman 2");
        etNS.setText(nonStrikerName != null ? nonStrikerName : "");
        applyInputStyle(etNS);
        container.addView(etNS);

        // Bowler
        TextView tvB = new TextView(this);
        tvB.setText("Super Over Bowler:");
        tvB.setTextSize(12);
        tvB.setTextColor(Color.GRAY);
        container.addView(tvB);
        final EditText etB = new EditText(this);
        etB.setHint("Bowler Name");
        etB.setText(matchData.currentBowlerName != null ? matchData.currentBowlerName : "");
        applyInputStyle(etB);
        container.addView(etB);

        Button btnStartSO = new Button(this);
        btnStartSO.setText("Start Super Over");
        btnStartSO.setTextColor(Color.WHITE);
        btnStartSO.setBackgroundColor(Color.parseColor("#D97706"));
        btnStartSO.setAllCaps(false);
        btnStartSO.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
        p1.setMargins(0, 0, 0, dpToPx(8));
        btnStartSO.setLayoutParams(p1);
        btnStartSO.setOnClickListener(v -> {
            String s = etS.getText().toString().trim();
            String ns = etNS.getText().toString().trim();
            String b = etB.getText().toString().trim();

            if (s.isEmpty() || ns.isEmpty() || b.isEmpty()) {
                Toast.makeText(this, "Please enter all players for Super Over", Toast.LENGTH_SHORT).show();
                return;
            }

            matchData.recordManualSwap();
            matchData.isSuperOver = true;
            matchData.totalOvers = "1";
            matchData.totalRuns = 0;
            matchData.totalWickets = 0;
            matchData.currentBalls = 0;
            matchData.currentOvers = 0;
            matchData.ballsBowled = 0;
            matchData.currentOverBalls.clear();
            matchData.ballHistory.clear();

            strikerName = s;
            matchData.strikerName = s;
            matchData.resetStrikerStats();

            nonStrikerName = ns;
            matchData.nonStrikerName = ns;
            matchData.resetNonStrikerStats();

            matchData.currentBowlerName = b;
            matchData.bowlerBallsBowled = 0;
            matchData.bowlerRuns = 0;
            matchData.bowlerWickets = 0;
            matchData.currentBowlerMaidens = 0;

            tvStrikerName.setText(s + " *");
            tvNonStrikerName.setText(ns);
            tvBowlerName.setText(b);

            updateScoreboardDisplay();
            dialog.dismiss();
            Toast.makeText(this, "⚡ Super Over Started! 1 over, max 2 wickets.", Toast.LENGTH_LONG).show();
        });
        container.addView(btnStartSO);

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setAllCaps(false);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        container.addView(btnCancel);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.90), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }
}

