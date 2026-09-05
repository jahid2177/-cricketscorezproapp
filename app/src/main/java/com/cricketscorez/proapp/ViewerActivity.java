package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class ViewerActivity extends Activity {

    private static final String TAG = "ViewerActivity";

    // Navigation state: 0 = Live, 1 = Tournaments, 2 = Teams, 3 = History
    private int currentNavTab = 0;
    private int currentLiveSubtab = 0;        // 0 = LIVE, 1 = SCOREBOARD, 2 = OVER BY OVER, 3 = FOW
    private int currentTournamentSubtab = 0;  // 0 = Fixtures, 1 = Points, 2 = Result, 3 = Awards

    // UI Root Containers
    private FrameLayout viewerContentContainer;
    private LinearLayout navTabLive, navTabTournaments, navTabTeams, navTabHistory;
    private View indicatorLive, indicatorTournaments, indicatorTeams, indicatorHistory;
    private ImageView ivNavLive, ivNavTournaments, ivNavTeams, ivNavHistory;
    private TextView tvNavLive, tvNavTournaments, tvNavTeams, tvNavHistory;

    // Firebase Realtime Database Listener
    private DatabaseReference liveMatchesRef;
    private ValueEventListener liveMatchesListener;
    private final List<LiveMatchSnapshot> activeLiveMatches = new ArrayList<>();
    private LiveMatchSnapshot selectedLiveMatch = null;

    // ✅ FIX: Tournaments/Teams/History ট্যাবগুলো এখন Firebase থেকে ডেটা পড়ে
    // (আগে এগুলো লোকাল SharedPreferences/DataManager থেকে পড়ত, তাই অন্য
    // ডিভাইসে Viewer খুললে খালি দেখাত)
    private DatabaseReference teamsRef;
    private DatabaseReference playersRef;
    private DatabaseReference tournamentsRef;
    private DatabaseReference matchHistoryRef;

    // সবচেয়ে সাম্প্রতিক (active) tournament-এর ক্যাশ করা ডেটা — Fixtures/Points/
    // Result/Awards সাবট্যাব বদলানোর সময় বারবার নেটওয়ার্ক কল না করার জন্য
    private DataSnapshot cachedTournamentSnapshot = null;
    private String cachedTournamentName = "";
    private boolean isTournamentLoading = false;

    // Local match data passed from Scorer (if applicable)
    private MatchData activeLocalMatchData = null;

    // Active BottomSheet reference for live synchronization
    private BottomSheetDialog currentBottomSheetDialog = null;
    private View currentBottomSheetView = null;

    // Data Class for Firebase Live Match Snapshot
    public static class LiveMatchSnapshot {
        public String matchId = "";
        public String team1 = "Team 1";
        public String team2 = "Team 2";
        public String totalOvers = "20";
        public String tossInfo = "";
        public String battingFirst = "";
        public String bowlingFirst = "";
        public String matchDate = "";

        // Live score
        public int score = 0;
        public int wickets = 0;
        public String overs = "0.0";
        public String crr = "0.00";
        public String rrr = "-";
        public String matchEquation = "";
        public String battingTeam = "";
        public String bowlingTeam = "";
        public String extras = "0";
        public String partnership = "0 runs (0 balls)";
        public int target = 0;
        public int innings = 1;
        public String status = "Live";
        public String matchResult = "";

        // Batters
        public String strikerName = "";
        public int strikerRuns = 0;
        public int strikerBalls = 0;
        public int strikerFours = 0;
        public int strikerSixes = 0;
        public String strikerSR = "0.0";

        public String nonStrikerName = "";
        public int nonStrikerRuns = 0;
        public int nonStrikerBalls = 0;
        public int nonStrikerFours = 0;
        public int nonStrikerSixes = 0;
        public String nonStrikerSR = "0.0";

        // Bowler
        public String bowlerName = "";
        public String bowlerFigures = "0.0-0-0-0";
        public int bowlerRuns = 0;
        public int bowlerWickets = 0;
        public int bowlerMaidens = 0;
        public String bowlerEconomy = "0.00";

        // In this over balls
        public final List<String> thisOverBalls = new ArrayList<>();

        // ✅ FIX: Over-by-Over ও Fall of Wickets ট্যাব Firebase live match-এ
        // ঠিকভাবে দেখানোর জন্য এই দুইটা লিস্ট যোগ করা হলো
        public final List<String> fallOfWickets = new ArrayList<>();
        public final List<String> completedOvers = new ArrayList<>();

        // ✅ FIX: ২য় ইনিংসে যে টিম আগে ব্যাট করেছিল তার স্কোর দেখানোর জন্য
        public String inn1Score = "";
        public String inn1Overs = "";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        viewerContentContainer = findViewById(R.id.viewerContentContainer);

        navTabLive = findViewById(R.id.navTabLive);
        navTabTournaments = findViewById(R.id.navTabTournaments);
        navTabTeams = findViewById(R.id.navTabTeams);
        navTabHistory = findViewById(R.id.navTabHistory);

        indicatorLive = findViewById(R.id.indicatorLive);
        indicatorTournaments = findViewById(R.id.indicatorTournaments);
        indicatorTeams = findViewById(R.id.indicatorTeams);
        indicatorHistory = findViewById(R.id.indicatorHistory);

        ivNavLive = findViewById(R.id.ivNavLive);
        ivNavTournaments = findViewById(R.id.ivNavTournaments);
        ivNavTeams = findViewById(R.id.ivNavTeams);
        ivNavHistory = findViewById(R.id.ivNavHistory);

        tvNavLive = findViewById(R.id.tvNavLive);
        tvNavTournaments = findViewById(R.id.tvNavTournaments);
        tvNavTeams = findViewById(R.id.tvNavTeams);
        tvNavHistory = findViewById(R.id.tvNavHistory);

        navTabLive.setOnClickListener(v -> showNavTab(0));
        navTabTournaments.setOnClickListener(v -> showNavTab(1));
        navTabTeams.setOnClickListener(v -> showNavTab(2));
        navTabHistory.setOnClickListener(v -> showNavTab(3));

        // Initialize Firebase Database reference
        try {
            liveMatchesRef = FirebaseDatabase.getInstance().getReference("live_matches");
            teamsRef = FirebaseDatabase.getInstance().getReference("teams");
            playersRef = FirebaseDatabase.getInstance().getReference("players");
            tournamentsRef = FirebaseDatabase.getInstance().getReference("tournaments");
            matchHistoryRef = FirebaseDatabase.getInstance().getReference("matchHistory");
        } catch (Exception e) {
            Log.e(TAG, "Firebase init error: " + e.getMessage());
        }

        // Check for local match data passed via intent
        if (getIntent().hasExtra("MATCH_DATA")) {
            activeLocalMatchData = (MatchData) getIntent().getSerializableExtra("MATCH_DATA");
        } else {
            refreshActiveLocalMatch();
        }

        showNavTab(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshActiveLocalMatch();
        if (currentNavTab == 0) {
            renderActiveLiveScreen();
        }
    }

    private void refreshActiveLocalMatch() {
        // If we already have a valid in-progress local match, retain it
        if (activeLocalMatchData != null && !isMatchCompleted(activeLocalMatchData)) {
            return;
        }

        // Check local DataManager for the latest in-progress match
        ArrayList<MatchData> allMatches = DataManager.getAllMatches(this);
        if (allMatches != null && !allMatches.isEmpty()) {
            for (MatchData md : allMatches) {
                if (md != null && !isMatchCompleted(md)) {
                    activeLocalMatchData = md;
                    return;
                }
            }
        }

        // Check LiveMatchProgressRepository in Room database
        try {
            com.cricketscorez.proapp.room.LiveMatchProgressRepository.getLatestActiveMatch(this, new com.cricketscorez.proapp.room.LiveMatchProgressRepository.OnMatchLoadedCallback() {
                @Override
                public void onLoaded(MatchData matchData) {
                    if (matchData != null && !isMatchCompleted(matchData)) {
                        activeLocalMatchData = matchData;
                        runOnUiThread(() -> {
                            if (currentNavTab == 0 && selectedLiveMatch == null) {
                                renderActiveLiveScreen();
                            }
                        });
                    }
                }

                @Override
                public void onNotFound() {
                    // No active room match found
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error checking Room for active match: " + e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startFirebaseLiveListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopFirebaseLiveListener();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 🔥 FIREBASE REALTIME DATABASE LIVE SYNC LISTENER
    // ═════════════════════════════════════════════════════════════════════════
    private void startFirebaseLiveListener() {
        if (liveMatchesRef == null) return;

        stopFirebaseLiveListener();

        liveMatchesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                activeLiveMatches.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot matchSnap : snapshot.getChildren()) {
                        try {
                            LiveMatchSnapshot match = parseFirebaseMatchSnapshot(matchSnap);
                            if (match != null) {
                                activeLiveMatches.add(match);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing match snap: " + e.getMessage());
                        }
                    }
                }

                // If currently selected match still exists in list, keep it selected; else pick first or null
                if (selectedLiveMatch != null) {
                    boolean found = false;
                    for (LiveMatchSnapshot m : activeLiveMatches) {
                        if (m.matchId.equals(selectedLiveMatch.matchId)) {
                            selectedLiveMatch = m;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        selectedLiveMatch = activeLiveMatches.isEmpty() ? null : activeLiveMatches.get(0);
                    }
                } else {
                    selectedLiveMatch = activeLiveMatches.isEmpty() ? null : activeLiveMatches.get(0);
                }

                // Automatically update UI in real time on the main thread
                runOnUiThread(() -> {
                    if (currentNavTab == 0) {
                        renderActiveLiveScreen();
                    }
                    updateActiveBottomSheetIfOpen();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Firebase Realtime live_matches cancelled: " + error.getMessage());
            }
        };

        liveMatchesRef.addValueEventListener(liveMatchesListener);
    }

    private void stopFirebaseLiveListener() {
        if (liveMatchesRef != null && liveMatchesListener != null) {
            liveMatchesRef.removeEventListener(liveMatchesListener);
            liveMatchesListener = null;
        }
    }

    private String getSnapString(DataSnapshot snap, String key, String defaultVal) {
        if (snap == null || !snap.hasChild(key)) return defaultVal;
        Object val = snap.child(key).getValue();
        return val != null ? String.valueOf(val) : defaultVal;
    }

    private int getSnapInt(DataSnapshot snap, String key, int defaultVal) {
        if (snap == null || !snap.hasChild(key)) return defaultVal;
        Object val = snap.child(key).getValue();
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    private LiveMatchSnapshot parseFirebaseMatchSnapshot(DataSnapshot matchSnap) {
        LiveMatchSnapshot m = new LiveMatchSnapshot();
        m.matchId = matchSnap.getKey() != null ? matchSnap.getKey() : "";

        DataSnapshot info = matchSnap.child("match_info");
        m.team1 = getSnapString(info, "team1", "Team 1");
        m.team2 = getSnapString(info, "team2", "Team 2");
        m.totalOvers = getSnapString(info, "total_overs", "20");
        m.tossInfo = getSnapString(info, "toss_info", "");
        m.battingFirst = getSnapString(info, "batting_first", "");
        m.bowlingFirst = getSnapString(info, "bowling_first", "");
        m.matchDate = getSnapString(info, "match_date", "");

        DataSnapshot score = matchSnap.child("live_score");
        m.score = getSnapInt(score, "score", 0);
        m.wickets = getSnapInt(score, "wickets", 0);
        m.overs = getSnapString(score, "overs", "0.0");
        m.crr = getSnapString(score, "crr", "0.00");
        m.rrr = getSnapString(score, "rrr", "-");
        m.matchEquation = getSnapString(score, "match_equation", "");
        m.battingTeam = getSnapString(score, "batting_team", m.team1);
        m.bowlingTeam = getSnapString(score, "bowling_team", m.team2);
        m.extras = getSnapString(score, "extras", "0");
        m.partnership = getSnapString(score, "partnership", "0 runs (0 balls)");
        m.target = getSnapInt(score, "target", 0);

        DataSnapshot bat = matchSnap.child("batsmen");
        DataSnapshot striker = bat.child("striker");
        m.strikerName = getSnapString(striker, "name", "Striker");
        m.strikerRuns = getSnapInt(striker, "runs", 0);
        m.strikerBalls = getSnapInt(striker, "balls", 0);
        m.strikerFours = getSnapInt(striker, "fours", 0);
        m.strikerSixes = getSnapInt(striker, "sixes", 0);
        m.strikerSR = getSnapString(striker, "sr", "0.0");

        DataSnapshot nonStriker = bat.child("non_striker");
        m.nonStrikerName = getSnapString(nonStriker, "name", "Non-Striker");
        m.nonStrikerRuns = getSnapInt(nonStriker, "runs", 0);
        m.nonStrikerBalls = getSnapInt(nonStriker, "balls", 0);
        m.nonStrikerFours = getSnapInt(nonStriker, "fours", 0);
        m.nonStrikerSixes = getSnapInt(nonStriker, "sixes", 0);
        m.nonStrikerSR = getSnapString(nonStriker, "sr", "0.0");

        DataSnapshot bowl = matchSnap.child("bowler");
        m.bowlerName = getSnapString(bowl, "name", "Bowler");
        m.bowlerFigures = getSnapString(bowl, "figures", "0.0-0-0-0");
        m.bowlerRuns = getSnapInt(bowl, "runs", 0);
        m.bowlerWickets = getSnapInt(bowl, "wickets", 0);
        m.bowlerMaidens = getSnapInt(bowl, "maidens", 0);
        m.bowlerEconomy = getSnapString(bowl, "economy", "0.00");

        DataSnapshot thisOver = matchSnap.child("this_over");
        if (thisOver.exists()) {
            for (DataSnapshot ball : thisOver.getChildren()) {
                if (ball.getKey() != null && ball.getKey().startsWith("ball_")) {
                    String runs = getSnapString(ball, "runs", "0");
                    boolean isWicket = Boolean.TRUE.equals(ball.child("is_wicket").getValue(Boolean.class));
                    boolean isExtra = Boolean.TRUE.equals(ball.child("is_extra").getValue(Boolean.class));
                    String extraType = getSnapString(ball, "extra_type", "");

                    if (isWicket) {
                        m.thisOverBalls.add("W");
                    } else if (isExtra) {
                        m.thisOverBalls.add(extraType.isEmpty() ? "Ex" : extraType);
                    } else {
                        m.thisOverBalls.add(runs);
                    }
                }
            }
        }

        m.innings = getSnapInt(matchSnap, "innings", 1);
        m.status = getSnapString(matchSnap, "status", "Live");
        m.matchResult = getSnapString(matchSnap.child("match_result"), "result_text", "");

        // ✅ FIX: Fall of Wickets — LiveScoreManager প্রতি বলের পরে
        // "fall_of_wickets" নোডে পুশ করে
        DataSnapshot fow = matchSnap.child("fall_of_wickets");
        if (fow.exists()) {
            for (DataSnapshot f : fow.getChildren()) {
                Object v = f.getValue();
                if (v != null) m.fallOfWickets.add(String.valueOf(v));
            }
        }

        // ✅ FIX: Over-by-Over — সম্পন্ন হওয়া ওভারগুলো "completed_overs" নোডে থাকে
        DataSnapshot completedOvers = matchSnap.child("completed_overs");
        if (completedOvers.exists()) {
            for (DataSnapshot o : completedOvers.getChildren()) {
                Object v = o.getValue();
                if (v != null) m.completedOvers.add(String.valueOf(v));
            }
        }

        // ✅ FIX: ২য় ইনিংসে প্রথম ইনিংসের স্কোর/ওভার "innings_break" নোড থেকে
        DataSnapshot inningsBreak = matchSnap.child("innings_break");
        m.inn1Score = getSnapString(inningsBreak, "first_innings_score", "");
        m.inn1Overs = getSnapString(inningsBreak, "first_innings_overs", "");

        return m;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // NAVIGATION TABS SWITCHER
    // ═════════════════════════════════════════════════════════════════════════
    private void showNavTab(int tabIndex) {
        currentNavTab = tabIndex;
        viewerContentContainer.removeAllViews();

        int green = Color.parseColor("#16A34A");
        int slate = Color.parseColor("#64748B");

        if (indicatorLive != null) indicatorLive.setVisibility(tabIndex == 0 ? View.VISIBLE : View.INVISIBLE);
        ivNavLive.setColorFilter(tabIndex == 0 ? green : slate);
        tvNavLive.setTextColor(tabIndex == 0 ? green : slate);
        tvNavLive.setTypeface(null, tabIndex == 0 ? Typeface.BOLD : Typeface.NORMAL);

        if (indicatorTournaments != null) indicatorTournaments.setVisibility(tabIndex == 1 ? View.VISIBLE : View.INVISIBLE);
        ivNavTournaments.setColorFilter(tabIndex == 1 ? green : slate);
        tvNavTournaments.setTextColor(tabIndex == 1 ? green : slate);
        tvNavTournaments.setTypeface(null, tabIndex == 1 ? Typeface.BOLD : Typeface.NORMAL);

        if (indicatorTeams != null) indicatorTeams.setVisibility(tabIndex == 2 ? View.VISIBLE : View.INVISIBLE);
        ivNavTeams.setColorFilter(tabIndex == 2 ? green : slate);
        tvNavTeams.setTextColor(tabIndex == 2 ? green : slate);
        tvNavTeams.setTypeface(null, tabIndex == 2 ? Typeface.BOLD : Typeface.NORMAL);

        if (indicatorHistory != null) indicatorHistory.setVisibility(tabIndex == 3 ? View.VISIBLE : View.INVISIBLE);
        ivNavHistory.setColorFilter(tabIndex == 3 ? green : slate);
        tvNavHistory.setTextColor(tabIndex == 3 ? green : slate);
        tvNavHistory.setTypeface(null, tabIndex == 3 ? Typeface.BOLD : Typeface.NORMAL);

        LayoutInflater inflater = LayoutInflater.from(this);

        switch (tabIndex) {
            case 0:
                renderLiveTab(inflater);
                break;
            case 1:
                renderTournamentsTab(inflater);
                break;
            case 2:
                renderTeamsTab(inflater);
                break;
            case 3:
                renderHistoryTab(inflater);
                break;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1. LIVE TAB (Screen 1 & 2 - Clean Card-Based Dashboard)
    // ═════════════════════════════════════════════════════════════════════════
    private void renderLiveTab(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_viewer_live, viewerContentContainer, false);
        viewerContentContainer.addView(view);

        // Refresh action
        LinearLayout btnLiveRefresh = view.findViewById(R.id.btnLiveRefresh);
        if (btnLiveRefresh != null) {
            btnLiveRefresh.setOnClickListener(v -> {
                Toast.makeText(this, "Checking for real-time live matches...", Toast.LENGTH_SHORT).show();
                startFirebaseLiveListener();
            });
        }

        // Pulse animation for LIVE SYNC badge
        View dotLivePulse = view.findViewById(R.id.dotLivePulse);
        if (dotLivePulse != null) {
            AlphaAnimation pulse = new AlphaAnimation(0.3f, 1.0f);
            pulse.setDuration(800);
            pulse.setRepeatMode(Animation.REVERSE);
            pulse.setRepeatCount(Animation.INFINITE);
            dotLivePulse.startAnimation(pulse);
        }

        // Empty state shortcuts
        Button btnEmptyCheckHistory = view.findViewById(R.id.btnEmptyCheckHistory);
        Button btnEmptyCheckTournaments = view.findViewById(R.id.btnEmptyCheckTournaments);
        if (btnEmptyCheckHistory != null) btnEmptyCheckHistory.setOnClickListener(v -> showNavTab(3));
        if (btnEmptyCheckTournaments != null) btnEmptyCheckTournaments.setOnClickListener(v -> showNavTab(1));

        // Subtabs
        LinearLayout subtabLive = view.findViewById(R.id.subtabLive);
        LinearLayout subtabScoreboard = view.findViewById(R.id.subtabScoreboard);
        LinearLayout subtabOverByOver = view.findViewById(R.id.subtabOverByOver);
        LinearLayout subtabFOW = view.findViewById(R.id.subtabFOW);

        if (subtabLive != null) subtabLive.setOnClickListener(v -> switchLiveSubtab(0, view));
        if (subtabScoreboard != null) subtabScoreboard.setOnClickListener(v -> switchLiveSubtab(1, view));
        if (subtabOverByOver != null) subtabOverByOver.setOnClickListener(v -> switchLiveSubtab(2, view));
        if (subtabFOW != null) subtabFOW.setOnClickListener(v -> switchLiveSubtab(3, view));

        // Clickable Hero Score Card to show detailed Bottom Sheet Modal
        FrameLayout cardHeroLiveScore = view.findViewById(R.id.cardHeroLiveScore);
        if (cardHeroLiveScore != null) {
            cardHeroLiveScore.setOnClickListener(v -> {
                if (selectedLiveMatch != null || activeLocalMatchData != null) {
                    showLiveMatchDetailBottomSheet();
                }
            });
        }

        renderActiveLiveScreen();
    }

    private void renderActiveLiveScreen() {
        if (viewerContentContainer == null || currentNavTab != 0) return;

        View root = viewerContentContainer.getChildAt(0);
        if (root == null) return;

        LinearLayout cardNoLiveMatch = root.findViewById(R.id.cardNoLiveMatch);
        LinearLayout cardLiveMatchActiveContainer = root.findViewById(R.id.cardLiveMatchActiveContainer);
        LinearLayout layoutMultiMatchBar = root.findViewById(R.id.layoutMultiMatchBar);
        LinearLayout containerMultiMatchChips = root.findViewById(R.id.containerMultiMatchChips);

        boolean hasFirebaseLive = (selectedLiveMatch != null);
        boolean hasLocalMatch = (activeLocalMatchData != null);

        if (!hasFirebaseLive && !hasLocalMatch) {
            // NO LIVE MATCH IN PROGRESS -> SHOW CLEAN HIGH-CONTRAST EMPTY STATE (ZERO DEMO DATA)
            if (cardNoLiveMatch != null) cardNoLiveMatch.setVisibility(View.VISIBLE);
            if (cardLiveMatchActiveContainer != null) cardLiveMatchActiveContainer.setVisibility(View.GONE);
            return;
        }

        if (cardNoLiveMatch != null) cardNoLiveMatch.setVisibility(View.GONE);
        if (cardLiveMatchActiveContainer != null) cardLiveMatchActiveContainer.setVisibility(View.VISIBLE);

        // Multiple match switcher chips
        if (layoutMultiMatchBar != null && containerMultiMatchChips != null) {
            if (activeLiveMatches.size() > 1) {
                layoutMultiMatchBar.setVisibility(View.VISIBLE);
                containerMultiMatchChips.removeAllViews();

                for (LiveMatchSnapshot m : activeLiveMatches) {
                    TextView chip = new TextView(this);
                    boolean isSelected = (selectedLiveMatch != null && m.matchId.equals(selectedLiveMatch.matchId));
                    chip.setText(m.team1 + " vs " + m.team2 + " (" + m.score + "/" + m.wickets + ")");
                    chip.setTextSize(12);
                    chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
                    chip.setTextColor(isSelected ? Color.WHITE : Color.parseColor("#16A34A"));
                    chip.setBackgroundResource(isSelected ? R.drawable.bg_pill_green : R.drawable.bg_role_chip);
                    chip.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 0, dpToPx(8), 0);
                    chip.setLayoutParams(lp);
                    chip.setOnClickListener(v -> {
                        selectedLiveMatch = m;
                        renderActiveLiveScreen();
                    });
                    containerMultiMatchChips.addView(chip);
                }
            } else {
                layoutMultiMatchBar.setVisibility(View.GONE);
            }
        }

        // Populate Hero Card
        TextView tvHeroTeam1 = root.findViewById(R.id.tvHeroTeam1);
        TextView tvHeroTeam2 = root.findViewById(R.id.tvHeroTeam2);
        TextView tvHeroInningsPill = root.findViewById(R.id.tvHeroInningsPill);
        TextView tvHeroBigScore = root.findViewById(R.id.tvHeroBigScore);
        TextView tvHeroOvers = root.findViewById(R.id.tvHeroOvers);
        TextView tvHeroCRR = root.findViewById(R.id.tvHeroCRR);
        TextView tvHeroProjected = root.findViewById(R.id.tvHeroProjected);

        if (hasFirebaseLive) {
            LiveMatchSnapshot m = selectedLiveMatch;
            if (tvHeroTeam1 != null) tvHeroTeam1.setText(m.team1);
            if (tvHeroTeam2 != null) tvHeroTeam2.setText(m.team2);
            if (tvHeroInningsPill != null) {
                tvHeroInningsPill.setText(m.status.equalsIgnoreCase("Live") ? "● LIVE INNINGS " + m.innings : m.status.toUpperCase(Locale.ENGLISH));
            }
            if (tvHeroBigScore != null) tvHeroBigScore.setText(m.score + "/" + m.wickets);
            // ✅ FIX: m.overs তে আগে থেকেই "current / total" ফরম্যাট থাকে
            // (LiveScoreManager.getOversString() থেকে) — তাই আর totalOvers জোড়া লাগাতে হবে না
            if (tvHeroOvers != null) tvHeroOvers.setText("(" + m.overs + " Ov)");
            if (tvHeroCRR != null) tvHeroCRR.setText("CRR: " + m.crr);
            if (tvHeroProjected != null) {
                // ✅ FIX: ম্যাচ Completed হয়ে গেলে আর stale Target/RRR equation না দেখিয়ে
                // চূড়ান্ত ফলাফল (match_result) দেখানো হচ্ছে
                if (m.status.equalsIgnoreCase("Completed")) {
                    tvHeroProjected.setText(!m.matchResult.isEmpty() ? m.matchResult : "Match Completed");
                } else if (m.target > 0) {
                    tvHeroProjected.setText("Target: " + m.target + " • " + m.matchEquation);
                } else {
                    tvHeroProjected.setText("Batting: " + (m.battingTeam.isEmpty() ? m.team1 : m.battingTeam));
                }
            }
        } else if (hasLocalMatch) {
            MatchData m = activeLocalMatchData;
            if (tvHeroTeam1 != null) tvHeroTeam1.setText(m.team1Name != null ? m.team1Name : "Team 1");
            if (tvHeroTeam2 != null) tvHeroTeam2.setText(m.team2Name != null ? m.team2Name : "Team 2");
            if (tvHeroInningsPill != null) tvHeroInningsPill.setText(m.isSecondInnings ? "INNINGS 2" : "INNINGS 1");
            if (tvHeroBigScore != null) tvHeroBigScore.setText(m.totalRuns + "/" + m.totalWickets);
            if (tvHeroOvers != null) tvHeroOvers.setText("(" + m.currentOvers + "." + m.currentBalls + " / " + m.totalOvers + " Ov)");
            if (tvHeroCRR != null) tvHeroCRR.setText("CRR: " + calculateCRR(m.totalRuns, m.currentOvers, m.currentBalls));
            if (tvHeroProjected != null) {
                if (m.isSecondInnings && m.targetRuns > 0) {
                    tvHeroProjected.setText("Target: " + m.targetRuns);
                } else {
                    tvHeroProjected.setText("Tap card for match stats →");
                }
            }
        }

        // Render current subtab
        switchLiveSubtab(currentLiveSubtab, root);
    }

    private void switchLiveSubtab(int subtabIndex, View root) {
        currentLiveSubtab = subtabIndex;
        int green = Color.parseColor("#16A34A");
        int slate = Color.parseColor("#64748B");

        TextView tvLive = root.findViewById(R.id.tvSubtabLive);
        TextView tvScoreboard = root.findViewById(R.id.tvSubtabScoreboard);
        TextView tvOverByOver = root.findViewById(R.id.tvSubtabOverByOver);
        TextView tvFOW = root.findViewById(R.id.tvSubtabFOW);

        View lineLive = root.findViewById(R.id.lineSubtabLive);
        View lineScoreboard = root.findViewById(R.id.lineSubtabScoreboard);
        View lineOverByOver = root.findViewById(R.id.lineSubtabOverByOver);
        View lineFOW = root.findViewById(R.id.lineSubtabFOW);

        if (tvLive != null) {
            tvLive.setTextColor(subtabIndex == 0 ? green : slate);
            tvLive.setTypeface(null, subtabIndex == 0 ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (lineLive != null) lineLive.setVisibility(subtabIndex == 0 ? View.VISIBLE : View.INVISIBLE);

        if (tvScoreboard != null) {
            tvScoreboard.setTextColor(subtabIndex == 1 ? green : slate);
            tvScoreboard.setTypeface(null, subtabIndex == 1 ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (lineScoreboard != null) lineScoreboard.setVisibility(subtabIndex == 1 ? View.VISIBLE : View.INVISIBLE);

        if (tvOverByOver != null) {
            tvOverByOver.setTextColor(subtabIndex == 2 ? green : slate);
            tvOverByOver.setTypeface(null, subtabIndex == 2 ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (lineOverByOver != null) lineOverByOver.setVisibility(subtabIndex == 2 ? View.VISIBLE : View.INVISIBLE);

        if (tvFOW != null) {
            tvFOW.setTextColor(subtabIndex == 3 ? green : slate);
            tvFOW.setTypeface(null, subtabIndex == 3 ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (lineFOW != null) lineFOW.setVisibility(subtabIndex == 3 ? View.VISIBLE : View.INVISIBLE);

        FrameLayout container = root.findViewById(R.id.subtabContentContainer);
        if (container == null) return;
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);

        switch (subtabIndex) {
            case 0:
                renderSubtabLiveContent(inflater, container);
                break;
            case 1:
                renderSubtabScoreboardContent(inflater, container);
                break;
            case 2:
                renderSubtabOverByOverContent(inflater, container);
                break;
            case 3:
                renderSubtabFOWContent(inflater, container);
                break;
        }
    }

    private void renderSubtabLiveContent(LayoutInflater inflater, ViewGroup container) {
        View subView = inflater.inflate(R.layout.view_subtab_live_content, container, false);
        container.addView(subView);

        TextView tvBat1Name = subView.findViewById(R.id.tvBat1Name);
        TextView tvBat1Runs = subView.findViewById(R.id.tvBat1Runs);
        TextView tvBat1Balls = subView.findViewById(R.id.tvBat1Balls);
        TextView tvBat1Fours = subView.findViewById(R.id.tvBat1Fours);
        TextView tvBat1Sixes = subView.findViewById(R.id.tvBat1Sixes);
        TextView tvBat1SR = subView.findViewById(R.id.tvBat1SR);

        TextView tvBat2Name = subView.findViewById(R.id.tvBat2Name);
        TextView tvBat2Runs = subView.findViewById(R.id.tvBat2Runs);
        TextView tvBat2Balls = subView.findViewById(R.id.tvBat2Balls);
        TextView tvBat2Fours = subView.findViewById(R.id.tvBat2Fours);
        TextView tvBat2Sixes = subView.findViewById(R.id.tvBat2Sixes);
        TextView tvBat2SR = subView.findViewById(R.id.tvBat2SR);

        TextView tvBowlerName = subView.findViewById(R.id.tvBowlerName);
        TextView tvBowlerOvers = subView.findViewById(R.id.tvBowlerOvers);
        TextView tvBowlerMaidens = subView.findViewById(R.id.tvBowlerMaidens);
        TextView tvBowlerRuns = subView.findViewById(R.id.tvBowlerRuns);
        TextView tvBowlerWickets = subView.findViewById(R.id.tvBowlerWickets);
        TextView tvBowlerEcon = subView.findViewById(R.id.tvBowlerEcon);

        LinearLayout layoutBalls = subView.findViewById(R.id.layoutInThisOverBalls);

        if (selectedLiveMatch != null) {
            LiveMatchSnapshot m = selectedLiveMatch;
            if (tvBat1Name != null) tvBat1Name.setText(m.strikerName.isEmpty() ? "Striker" : m.strikerName);
            if (tvBat1Runs != null) tvBat1Runs.setText(String.valueOf(m.strikerRuns));
            if (tvBat1Balls != null) tvBat1Balls.setText(String.valueOf(m.strikerBalls));
            if (tvBat1Fours != null) tvBat1Fours.setText(String.valueOf(m.strikerFours));
            if (tvBat1Sixes != null) tvBat1Sixes.setText(String.valueOf(m.strikerSixes));
            if (tvBat1SR != null) tvBat1SR.setText(m.strikerSR);

            if (tvBat2Name != null) tvBat2Name.setText(m.nonStrikerName.isEmpty() ? "Non-Striker" : m.nonStrikerName);
            if (tvBat2Runs != null) tvBat2Runs.setText(String.valueOf(m.nonStrikerRuns));
            if (tvBat2Balls != null) tvBat2Balls.setText(String.valueOf(m.nonStrikerBalls));
            if (tvBat2Fours != null) tvBat2Fours.setText(String.valueOf(m.nonStrikerFours));
            if (tvBat2Sixes != null) tvBat2Sixes.setText(String.valueOf(m.nonStrikerSixes));
            if (tvBat2SR != null) tvBat2SR.setText(m.nonStrikerSR);

            if (tvBowlerName != null) tvBowlerName.setText(m.bowlerName.isEmpty() ? "Bowler" : m.bowlerName);
            if (tvBowlerOvers != null) tvBowlerOvers.setText(m.bowlerFigures.contains("-") ? m.bowlerFigures.split("-")[0] : "0.0");
            if (tvBowlerMaidens != null) tvBowlerMaidens.setText(String.valueOf(m.bowlerMaidens));
            if (tvBowlerRuns != null) tvBowlerRuns.setText(String.valueOf(m.bowlerRuns));
            if (tvBowlerWickets != null) tvBowlerWickets.setText(String.valueOf(m.bowlerWickets));
            if (tvBowlerEcon != null) tvBowlerEcon.setText(m.bowlerEconomy);

            if (layoutBalls != null) {
                layoutBalls.removeAllViews();
                if (m.thisOverBalls.isEmpty()) {
                    TextView tv = new TextView(this);
                    tv.setText("This over: 0 balls bowled");
                    tv.setTextSize(12);
                    tv.setTextColor(Color.parseColor("#64748B"));
                    layoutBalls.addView(tv);
                } else {
                    for (String b : m.thisOverBalls) {
                        layoutBalls.addView(createBallChipView(b));
                    }
                }
            }
        } else if (activeLocalMatchData != null) {
            MatchData m = activeLocalMatchData;
            if (tvBat1Name != null) tvBat1Name.setText(m.strikerName != null ? m.strikerName : "Striker");
            if (tvBat1Runs != null) tvBat1Runs.setText(String.valueOf(m.strikerRuns));
            if (tvBat1Balls != null) tvBat1Balls.setText(String.valueOf(m.strikerBalls));
            if (tvBat1Fours != null) tvBat1Fours.setText(String.valueOf(m.striker4s));
            if (tvBat1Sixes != null) tvBat1Sixes.setText(String.valueOf(m.striker6s));
            if (tvBat1SR != null) tvBat1SR.setText(calculateSR(m.strikerRuns, m.strikerBalls));

            if (tvBat2Name != null) tvBat2Name.setText(m.nonStrikerName != null ? m.nonStrikerName : "Non-Striker");
            if (tvBat2Runs != null) tvBat2Runs.setText(String.valueOf(m.nonStrikerRuns));
            if (tvBat2Balls != null) tvBat2Balls.setText(String.valueOf(m.nonStrikerBalls));
            if (tvBat2Fours != null) tvBat2Fours.setText(String.valueOf(m.nonStriker4s));
            if (tvBat2Sixes != null) tvBat2Sixes.setText(String.valueOf(m.nonStriker6s));
            if (tvBat2SR != null) tvBat2SR.setText(calculateSR(m.nonStrikerRuns, m.nonStrikerBalls));

            if (tvBowlerName != null) tvBowlerName.setText(m.currentBowlerName != null ? m.currentBowlerName : "Bowler");
            if (tvBowlerOvers != null) tvBowlerOvers.setText((m.bowlerBallsBowled / 6) + "." + (m.bowlerBallsBowled % 6));
            if (tvBowlerMaidens != null) tvBowlerMaidens.setText(String.valueOf(m.currentBowlerMaidens));
            if (tvBowlerRuns != null) tvBowlerRuns.setText(String.valueOf(m.bowlerRuns));
            if (tvBowlerWickets != null) tvBowlerWickets.setText(String.valueOf(m.bowlerWickets));
            if (tvBowlerEcon != null) tvBowlerEcon.setText(calculateEcon(m.bowlerRuns, m.bowlerBallsBowled / 6, m.bowlerBallsBowled % 6));

            if (layoutBalls != null) {
                layoutBalls.removeAllViews();
                if (m.currentOverBalls != null && !m.currentOverBalls.isEmpty()) {
                    for (BallEvent be : m.currentOverBalls) {
                        layoutBalls.addView(createBallChipView(be.isWicket ? "W" : (be.isExtra ? be.extraType : String.valueOf(be.runs))));
                    }
                } else {
                    TextView tv = new TextView(this);
                    tv.setText("This over: 0 balls bowled");
                    tv.setTextSize(12);
                    tv.setTextColor(Color.parseColor("#64748B"));
                    layoutBalls.addView(tv);
                }
            }
        }
    }

    private View createBallChipView(String val) {
        FrameLayout chip = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28));
        lp.setMargins(0, 0, dpToPx(8), 0);
        chip.setLayoutParams(lp);

        int bgRes = R.drawable.bg_ball_chip_gray;
        int textColor = Color.parseColor("#1E293B");

        if ("4".equals(val)) {
            bgRes = R.drawable.bg_ball_chip_blue;
            textColor = Color.WHITE;
        } else if ("6".equals(val)) {
            bgRes = R.drawable.bg_ball_chip_purple;
            textColor = Color.WHITE;
        } else if ("W".equalsIgnoreCase(val)) {
            bgRes = R.drawable.bg_ball_chip_red;
            textColor = Color.WHITE;
        } else if ("Wd".equalsIgnoreCase(val) || "Nb".equalsIgnoreCase(val) || "WD".equalsIgnoreCase(val) || "NB".equalsIgnoreCase(val)) {
            bgRes = R.drawable.bg_role_chip;
            textColor = Color.parseColor("#D97706");
        }

        chip.setBackgroundResource(bgRes);

        TextView tv = new TextView(this);
        tv.setText(val);
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(textColor);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flp.gravity = Gravity.CENTER;
        tv.setLayoutParams(flp);

        chip.addView(tv);
        return chip;
    }

    private void renderSubtabScoreboardContent(LayoutInflater inflater, ViewGroup container) {
        View subView = inflater.inflate(R.layout.view_subtab_scoreboard_content, container, false);
        container.addView(subView);

        TextView tvSbTeam1Name = subView.findViewById(R.id.tvSbTeam1Name);
        TextView tvSbTeam1Score = subView.findViewById(R.id.tvSbTeam1Score);
        TextView tvSbTeam2Name = subView.findViewById(R.id.tvSbTeam2Name);
        TextView tvSbTeam2Score = subView.findViewById(R.id.tvSbTeam2Score);
        TextView tvSbTossInfo = subView.findViewById(R.id.tvSbTossInfo);
        TextView tvSbInningsHeader = subView.findViewById(R.id.tvSbInningsHeader);
        TextView tvSbOverBowler = subView.findViewById(R.id.tvSbOverBowler);
        LinearLayout layoutSbOverBalls = subView.findViewById(R.id.layoutSbOverBalls);

        if (selectedLiveMatch != null) {
            LiveMatchSnapshot m = selectedLiveMatch;

            // ✅ FIX: আগে Team 1-এর ঘরে সবসময় লাইভ স্কোর বসত, এমনকি ২য় ইনিংসে
            // Team 2 ব্যাট করলেও — এখন কোন টিম আসলে ব্যাট করছে সেটা দেখে
            // সঠিক ঘরে স্কোর বসানো হচ্ছে, আর ব্যাট করা টিমের নামের পাশে 🏏 দেখানো হচ্ছে
            boolean team1Batting = m.battingTeam.isEmpty() || m.battingTeam.equalsIgnoreCase(m.team1);

            if (tvSbTeam1Name != null) tvSbTeam1Name.setText((team1Batting ? "🏏 " : "") + m.team1);
            if (tvSbTeam2Name != null) tvSbTeam2Name.setText((!team1Batting ? "🏏 " : "") + m.team2);

            if (team1Batting) {
                if (tvSbTeam1Score != null) tvSbTeam1Score.setText(m.score + "/" + m.wickets + " overs " + m.overs);
                if (tvSbTeam2Score != null) tvSbTeam2Score.setText(m.innings >= 2 ? "Target " + m.target : "Yet to bat");
            } else {
                if (tvSbTeam1Score != null) {
                    tvSbTeam1Score.setText(!m.inn1Score.isEmpty()
                            ? m.inn1Score + " overs " + m.inn1Overs
                            : "Batted first");
                }
                if (tvSbTeam2Score != null) tvSbTeam2Score.setText(m.score + "/" + m.wickets + " overs " + m.overs);
            }

            if (tvSbTossInfo != null) tvSbTossInfo.setText(m.tossInfo.isEmpty() ? m.team1 + " opted to bat." : m.tossInfo);
            if (tvSbInningsHeader != null) tvSbInningsHeader.setText((m.battingTeam.isEmpty() ? m.team1 : m.battingTeam).toUpperCase(Locale.ENGLISH) + " INNINGS");
            if (tvSbOverBowler != null) tvSbOverBowler.setText("Bowler: " + (m.bowlerName.isEmpty() ? "Current Bowler" : m.bowlerName));

            if (layoutSbOverBalls != null) {
                layoutSbOverBalls.removeAllViews();
                if (m.thisOverBalls.isEmpty()) {
                    TextView tv = new TextView(this);
                    tv.setText("No balls in current over yet.");
                    tv.setTextSize(12);
                    tv.setTextColor(Color.parseColor("#64748B"));
                    layoutSbOverBalls.addView(tv);
                } else {
                    for (String b : m.thisOverBalls) {
                        layoutSbOverBalls.addView(createBallChipView(b));
                    }
                }
            }
        } else if (activeLocalMatchData != null) {
            MatchData m = activeLocalMatchData;
            if (tvSbTeam1Name != null) tvSbTeam1Name.setText(m.team1Name != null ? m.team1Name : "Team 1");
            if (tvSbTeam1Score != null) tvSbTeam1Score.setText(m.totalRuns + "/" + m.totalWickets + " overs " + m.currentOvers + "." + m.currentBalls);
            if (tvSbTeam2Name != null) tvSbTeam2Name.setText(m.team2Name != null ? m.team2Name : "Team 2");
            if (tvSbTeam2Score != null) tvSbTeam2Score.setText(m.isSecondInnings ? "Target " + m.targetRuns : "Yet to bat");
            if (tvSbTossInfo != null) tvSbTossInfo.setText(m.tossMessage != null ? m.tossMessage : "Toss information not available");
            if (tvSbInningsHeader != null) tvSbInningsHeader.setText(m.getBattingTeamName().toUpperCase(Locale.ENGLISH) + " INNINGS");
            if (tvSbOverBowler != null) tvSbOverBowler.setText("Bowler: " + (m.currentBowlerName != null ? m.currentBowlerName : "Bowler"));

            if (layoutSbOverBalls != null) {
                layoutSbOverBalls.removeAllViews();
                if (m.currentOverBalls != null && !m.currentOverBalls.isEmpty()) {
                    for (BallEvent be : m.currentOverBalls) {
                        layoutSbOverBalls.addView(createBallChipView(be.isWicket ? "W" : (be.isExtra ? be.extraType : String.valueOf(be.runs))));
                    }
                } else {
                    TextView tv = new TextView(this);
                    tv.setText("No balls in current over yet.");
                    tv.setTextSize(12);
                    tv.setTextColor(Color.parseColor("#64748B"));
                    layoutSbOverBalls.addView(tv);
                }
            }
        }
    }

    private void renderSubtabOverByOverContent(LayoutInflater inflater, ViewGroup container) {
        View subView = inflater.inflate(R.layout.view_subtab_overbyover_content, container, false);
        container.addView(subView);

        LinearLayout list = subView.findViewById(R.id.containerOverByOverList);
        list.removeAllViews();

        // ✅ FIX: আগে শুধু চলতি ওভারের বল দেখানো হতো, সম্পন্ন হওয়া ওভারগুলো
        // (completedOvers) কখনো দেখানো হতো না — তাই Firebase live match-এ সবসময়
        // ফাঁকা/0 দেখাত। এখন completedOvers + চলতি ওভার দুটোই দেখানো হচ্ছে।
        if (selectedLiveMatch != null &&
                (!selectedLiveMatch.completedOvers.isEmpty() || !selectedLiveMatch.thisOverBalls.isEmpty())) {
            List<String> overs = selectedLiveMatch.completedOvers;
            for (int i = 0; i < overs.size(); i++) {
                list.addView(createOverCard("Over " + (i + 1), overs.get(i), "Completed Over"));
            }
            if (!selectedLiveMatch.thisOverBalls.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String b : selectedLiveMatch.thisOverBalls) {
                    sb.append(b).append("  ");
                }
                list.addView(createOverCard("Current Over (" + selectedLiveMatch.overs + ")", sb.toString(), "Bowler: " + selectedLiveMatch.bowlerName));
            }
        } else if (activeLocalMatchData != null && activeLocalMatchData.ballHistory != null && !activeLocalMatchData.ballHistory.isEmpty()) {
            // Group ballHistory into completed overs
            int overCount = 0;
            StringBuilder overBalls = new StringBuilder();
            for (BallEvent be : activeLocalMatchData.ballHistory) {
                String label = be.isWicket ? "W" : (be.isExtra ? be.extraType : String.valueOf(be.runs));
                overBalls.append(label).append("  ");
                if (be.isLegalBall) {
                    overCount++;
                    if (overCount % 6 == 0) {
                        int num = overCount / 6;
                        list.addView(createOverCard("Over " + num, overBalls.toString(), "Completed Over"));
                        overBalls = new StringBuilder();
                    }
                }
            }
            if (overBalls.length() > 0) {
                list.addView(createOverCard("Current Over (" + activeLocalMatchData.currentOvers + "." + activeLocalMatchData.currentBalls + ")", overBalls.toString(), "In Progress"));
            }
        } else {
            TextView tv = new TextView(this);
            tv.setText("No completed overs in live record yet.");
            tv.setTextSize(13);
            tv.setTextColor(Color.parseColor("#64748B"));
            tv.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));
            list.addView(tv);
        }
    }

    private View createOverCard(String overTitle, String ballsText, String footerText) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_viewer_card);
        card.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(8));
        card.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(overTitle);
        tvTitle.setTextSize(13);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        card.addView(tvTitle);

        TextView tvBalls = new TextView(this);
        tvBalls.setText(ballsText);
        tvBalls.setTextSize(14);
        tvBalls.setTypeface(null, Typeface.BOLD);
        tvBalls.setTextColor(Color.parseColor("#16A34A"));
        tvBalls.setPadding(0, dpToPx(4), 0, dpToPx(4));
        card.addView(tvBalls);

        TextView tvFooter = new TextView(this);
        tvFooter.setText(footerText);
        tvFooter.setTextSize(11);
        tvFooter.setTextColor(Color.parseColor("#64748B"));
        card.addView(tvFooter);

        return card;
    }

    private void renderSubtabFOWContent(LayoutInflater inflater, ViewGroup container) {
        View subView = inflater.inflate(R.layout.view_subtab_fow_content, container, false);
        container.addView(subView);

        LinearLayout list = subView.findViewById(R.id.containerFowList);
        list.removeAllViews();

        // ✅ FIX: আগে এই ট্যাব শুধু activeLocalMatchData দেখত, Firebase live
        // match (remote viewer)-এর fallOfWickets কখনো চেক করত না — তাই সবসময়
        // "0" / খালি দেখাত। এখন Firebase live match-এর ডাটাও দেখানো হচ্ছে।
        if (selectedLiveMatch != null && !selectedLiveMatch.fallOfWickets.isEmpty()) {
            for (String fow : selectedLiveMatch.fallOfWickets) {
                TextView tv = new TextView(this);
                tv.setText("• " + fow);
                tv.setTextSize(13);
                tv.setTextColor(Color.parseColor("#334155"));
                tv.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
                list.addView(tv);
            }
        } else if (activeLocalMatchData != null && activeLocalMatchData.fallOfWickets != null && !activeLocalMatchData.fallOfWickets.isEmpty()) {
            for (String fow : activeLocalMatchData.fallOfWickets) {
                TextView tv = new TextView(this);
                tv.setText("• " + fow);
                tv.setTextSize(13);
                tv.setTextColor(Color.parseColor("#334155"));
                tv.setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6));
                list.addView(tv);
            }
        } else {
            TextView tv = new TextView(this);
            tv.setText("No wickets fallen yet in this innings.");
            tv.setTextSize(13);
            tv.setTextColor(Color.parseColor("#64748B"));
            tv.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12));
            list.addView(tv);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 🔥 BOTTOM-SHEET MODAL: LIVE MATCH DETAILED STATS
    // ═════════════════════════════════════════════════════════════════════════
    private void showLiveMatchDetailBottomSheet() {
        if (isFinishing()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.layout_bottom_sheet_match_detail, null);
        dialog.setContentView(sheetView);

        currentBottomSheetDialog = dialog;
        currentBottomSheetView = sheetView;

        dialog.setOnDismissListener(d -> {
            currentBottomSheetDialog = null;
            currentBottomSheetView = null;
        });

        populateBottomSheetData(sheetView);

        Button btnSheetFullScorecard = sheetView.findViewById(R.id.btnSheetFullScorecard);
        if (btnSheetFullScorecard != null) {
            btnSheetFullScorecard.setOnClickListener(v -> {
                dialog.dismiss();
                openFullScorecard();
            });
        }

        dialog.show();
    }

    private void updateActiveBottomSheetIfOpen() {
        if (currentBottomSheetDialog != null && currentBottomSheetDialog.isShowing() && currentBottomSheetView != null) {
            populateBottomSheetData(currentBottomSheetView);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 🔥 FIX: "View Detailed Scorecard" ক্লিক করলে আগে শুধু activeLocalMatchData
    // (এই ডিভাইসেই স্কোরিং হচ্ছে এমন ম্যাচ) থাকলেই MATCH_DATA পাঠানো হতো —
    // Firebase দিয়ে sync হওয়া রিমোট লাইভ ম্যাচের ক্ষেত্রে কিছুই পাঠানো হতো না,
    // ফলে Full Scorecard পুরো খালি/placeholder দেখাত। এখন সেক্ষেত্রে Firebase
    // থেকে এক-বারের জন্য পুরো ডাটা (full_scorecard) পড়ে MatchData বানিয়ে
    // ScorecardActivity-তে পাঠানো হচ্ছে।
    // ═════════════════════════════════════════════════════════════════════════
    private void openFullScorecard() {
        if (activeLocalMatchData != null) {
            Intent intent = new Intent(ViewerActivity.this, ScorecardActivity.class);
            intent.putExtra("MATCH_DATA", activeLocalMatchData);
            startActivity(intent);
            return;
        }

        if (selectedLiveMatch == null || liveMatchesRef == null) {
            Toast.makeText(this, "No live match data available yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String matchId = selectedLiveMatch.matchId;
        Toast.makeText(this, "Loading full scorecard…", Toast.LENGTH_SHORT).show();

        liveMatchesRef.child(matchId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        Toast.makeText(this, "Scorecard not available for this match.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        MatchData md = buildMatchDataFromFirebaseSnapshot(snapshot, matchId);
                        Intent intent = new Intent(ViewerActivity.this, ScorecardActivity.class);
                        intent.putExtra("MATCH_DATA", md);
                        intent.putExtra("TEAM_1", md.team1Name);
                        intent.putExtra("TEAM_2", md.team2Name);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "openFullScorecard() parse error: " + e.getMessage());
                        Toast.makeText(this, "Could not load full scorecard.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "openFullScorecard() fetch error: " + e.getMessage());
                    Toast.makeText(this, "Could not load full scorecard.", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Firebase DataSnapshot → MatchData (ScorecardActivity-এর জন্য) ──────
    private MatchData buildMatchDataFromFirebaseSnapshot(DataSnapshot matchSnap, String matchId) {
        DataSnapshot info = matchSnap.child("match_info");
        String team1 = getSnapString(info, "team1", "Team 1");
        String team2 = getSnapString(info, "team2", "Team 2");
        String totalOvers = getSnapString(info, "total_overs", "20");

        MatchData md = new MatchData(team1, team2, totalOvers);
        md.matchId = matchId;
        md.matchDate = getSnapString(info, "match_date", md.matchDate);
        md.tossMessage = getSnapString(info, "toss_info", md.tossMessage);
        md.teamBattingFirst = getSnapString(info, "batting_first", md.teamBattingFirst);
        md.teamBattingSecond = getSnapString(info, "bowling_first", md.teamBattingSecond);

        // ✅ FIX: MatchData-এর ডিফল্ট কনস্ট্রাক্টর strikerName/nonStrikerName-কে
        // "Striker"/"Non-Striker" (খালি স্ট্রিং না!) দিয়ে সেট করে রাখে। এই অবজেক্টটা
        // যেহেতু লাইভ স্কোরিং-এর জন্য না, শুধু Firebase থেকে পড়া একটা "স্ন্যাপশট" —
        // তাই এগুলো ফাঁকা করে দিচ্ছি, নাহলে getAllBattingStats() আসল ডাটার সাথে
        // এই ভুয়া প্লেসহোল্ডার নামও (0 রান সহ) জুড়ে দিত।
        md.strikerName = "";
        md.nonStrikerName = "";

        DataSnapshot full = matchSnap.child("full_scorecard");
        boolean hasFullScorecard = full.exists();
        if (hasFullScorecard) {
            md.totalRuns = getSnapInt(full, "total_runs", 0);
            md.totalWickets = getSnapInt(full, "total_wickets", 0);
            md.currentOvers = getSnapInt(full, "current_overs", 0);
            md.currentBalls = getSnapInt(full, "current_balls", 0);
            md.ballsBowled = getSnapInt(full, "balls_bowled", 0);
            md.totalOvers = getSnapString(full, "total_overs", md.totalOvers);
            md.isSecondInnings = Boolean.TRUE.equals(full.child("is_second_innings").getValue(Boolean.class));
            md.targetRuns = getSnapInt(full, "target_runs", 0);
            md.matchStatus = getSnapString(full, "match_status", md.matchStatus);
            md.matchResult = getSnapString(full, "match_result", md.matchResult);
            md.teamBattingFirst = getSnapString(full, "team_batting_first", md.teamBattingFirst);
            md.teamBattingSecond = getSnapString(full, "team_batting_second", md.teamBattingSecond);
            md.tossMessage = getSnapString(full, "toss_message", md.tossMessage);

            md.extraWide = getSnapInt(full, "extra_wide", 0);
            md.extraNoBall = getSnapInt(full, "extra_no_ball", 0);
            md.extraByes = getSnapInt(full, "extra_byes", 0);
            md.extraLegByes = getSnapInt(full, "extra_leg_byes", 0);
            md.extraPenalty = getSnapInt(full, "extra_penalty", 0);

            md.batsmanHistory = parseStringArrayList(full.child("batsman_history"));
            md.bowlerHistory = parseStringArrayList(full.child("bowler_history"));
            md.fallOfWickets = parseStringList(full.child("fall_of_wickets"));

            // ✅ FIX: ScorecardActivity বোলিং টেবিলের জন্য matchData.getAllBowlingStats()
            // কল করে — কিন্তু সেই মেথড আসল ফিগার আনে bowlerRegistry/bowlerDisplayNames
            // ম্যাপ থেকে, bowlerHistory লিস্ট থেকে সরাসরি না। Firebase থেকে শুধু
            // bowlerHistory বসালে registry খালি থেকে যেত, তাই বোলিং ফিগার সবসময়
            // ফাঁকা দেখাত। এখন bowlerHistory থেকে registry-ও বানানো হচ্ছে।
            populateBowlerRegistry(md, md.bowlerHistory);

            if (md.isSecondInnings) {
                md.scoreInn1 = getSnapString(full, "score_inn1", "");
                md.oversInn1 = getSnapString(full, "overs_inn1", "");
                md.extrasInn1 = getSnapString(full, "extras_inn1", "");
                md.batsmanHistoryInn1 = parseStringArrayList(full.child("batsman_history_inn1"));
                md.bowlerHistoryInn1 = parseStringArrayList(full.child("bowler_history_inn1"));
                md.fallOfWicketsInn1 = parseStringList(full.child("fall_of_wickets_inn1"));
            }
        }

        // ✅ FIX: full_scorecard নোড না থাকা মানে এই ম্যাচটা আপডেট করা কোড আসার
        // আগেই স্কোর করা হয়েছিল — তখন কোনো ডাটা বানিয়ে (fake placeholder) না
        // দেখিয়ে স্পষ্ট করে জানানো হচ্ছে, যাতে ভুল বোঝাবুঝি না হয়।
        if (!hasFullScorecard) {
            Toast.makeText(this,
                    "এই ম্যাচের বিস্তারিত স্কোরকার্ড পাওয়া যায়নি (আপডেটের আগে স্কোর করা হয়েছিল)",
                    Toast.LENGTH_LONG).show();
        }

        // status node ("Live"/"Completed") থেকে matchStatus নিশ্চিত করা, যাতে
        // ScorecardActivity-র "showFinalResult()" ঠিকভাবে ট্রিগার হয়
        String liveStatus = getSnapString(matchSnap, "status", "");
        if ("Completed".equalsIgnoreCase(liveStatus)) {
            String resultText = getSnapString(matchSnap.child("match_result"), "result_text", "");
            md.matchStatus = !resultText.isEmpty() ? resultText : "Completed";
            md.matchResult = resultText;
        }

        return md;
    }

    // ── bowlerHistory-এর প্রতিটা row ("নাম", "ওভার.বল", "মেইডেন", "রান",
    // "উইকেট", "ইকোনমি") থেকে matchData.bowlerRegistry/bowlerDisplayNames
    // পুনর্গঠন করা, যাতে getAllBowlingStats() সঠিক ফিগার দেখাতে পারে ──
    private void populateBowlerRegistry(MatchData md, ArrayList<String[]> bowlerRows) {
        for (String[] row : bowlerRows) {
            if (row.length < 5) continue;
            String name = row[0];
            String key = name.trim().toLowerCase(java.util.Locale.ROOT);
            int balls = 0;
            try {
                String[] oversParts = row[1].split("\\.");
                int overs = Integer.parseInt(oversParts[0].trim());
                int extraBalls = oversParts.length > 1 ? Integer.parseInt(oversParts[1].trim()) : 0;
                balls = overs * 6 + extraBalls;
            } catch (Exception ignored) {
            }
            int maidens = safeParseInt(row[2]);
            int runs = safeParseInt(row[3]);
            int wickets = safeParseInt(row[4]);
            md.bowlerRegistry.put(key, new int[]{balls, maidens, runs, wickets});
            md.bowlerDisplayNames.put(key, name);
        }
    }

    private int safeParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private ArrayList<String[]> parseStringArrayList(DataSnapshot node) {
        ArrayList<String[]> result = new ArrayList<>();
        if (node == null || !node.exists()) return result;
        for (DataSnapshot row : node.getChildren()) {
            ArrayList<String> cols = new ArrayList<>();
            for (DataSnapshot col : row.getChildren()) {
                Object v = col.getValue();
                cols.add(v != null ? String.valueOf(v) : "");
            }
            result.add(cols.toArray(new String[0]));
        }
        return result;
    }

    private ArrayList<String> parseStringList(DataSnapshot node) {
        ArrayList<String> result = new ArrayList<>();
        if (node == null || !node.exists()) return result;
        for (DataSnapshot child : node.getChildren()) {
            Object v = child.getValue();
            if (v != null) result.add(String.valueOf(v));
        }
        return result;
    }

    private void populateBottomSheetData(View sheetView) {
        TextView tvSheetMatchTitle = sheetView.findViewById(R.id.tvSheetMatchTitle);
        TextView tvSheetVenueInfo = sheetView.findViewById(R.id.tvSheetVenueInfo);
        TextView tvSheetLiveBadge = sheetView.findViewById(R.id.tvSheetLiveBadge);
        TextView tvSheetBattingTeam = sheetView.findViewById(R.id.tvSheetBattingTeam);
        TextView tvSheetInningsTag = sheetView.findViewById(R.id.tvSheetInningsTag);
        TextView tvSheetScore = sheetView.findViewById(R.id.tvSheetScore);
        TextView tvSheetOvers = sheetView.findViewById(R.id.tvSheetOvers);
        TextView tvSheetCRR = sheetView.findViewById(R.id.tvSheetCRR);
        TextView tvSheetRRR = sheetView.findViewById(R.id.tvSheetRRR);
        TextView tvSheetTarget = sheetView.findViewById(R.id.tvSheetTarget);

        TextView tvSheetStriker = sheetView.findViewById(R.id.tvSheetStriker);
        TextView tvSheetStrikerStats = sheetView.findViewById(R.id.tvSheetStrikerStats);
        TextView tvSheetNonStriker = sheetView.findViewById(R.id.tvSheetNonStriker);
        TextView tvSheetNonStrikerStats = sheetView.findViewById(R.id.tvSheetNonStrikerStats);

        TextView tvSheetBowler = sheetView.findViewById(R.id.tvSheetBowler);
        TextView tvSheetBowlerStats = sheetView.findViewById(R.id.tvSheetBowlerStats);

        TextView tvSheetPartnership = sheetView.findViewById(R.id.tvSheetPartnership);
        TextView tvSheetExtras = sheetView.findViewById(R.id.tvSheetExtras);

        if (selectedLiveMatch != null) {
            LiveMatchSnapshot m = selectedLiveMatch;
            if (tvSheetMatchTitle != null) tvSheetMatchTitle.setText(m.team1 + " vs " + m.team2);
            if (tvSheetVenueInfo != null) tvSheetVenueInfo.setText((m.tossInfo.isEmpty() ? "Live Tournament Match" : m.tossInfo) + " • " + m.totalOvers + " Overs");
            if (tvSheetLiveBadge != null) tvSheetLiveBadge.setText("● " + m.status.toUpperCase(Locale.ENGLISH));
            if (tvSheetBattingTeam != null) tvSheetBattingTeam.setText("Batting: " + (m.battingTeam.isEmpty() ? m.team1 : m.battingTeam));
            if (tvSheetInningsTag != null) tvSheetInningsTag.setText("Innings " + m.innings);
            if (tvSheetScore != null) tvSheetScore.setText(m.score + "/" + m.wickets);
            if (tvSheetOvers != null) tvSheetOvers.setText(" (" + m.overs + " Ov)");
            if (tvSheetCRR != null) tvSheetCRR.setText("CRR: " + m.crr);
            if (tvSheetRRR != null) tvSheetRRR.setText(m.rrr.isEmpty() || "-".equals(m.rrr) ? "RRR: -" : "RRR: " + m.rrr);
            if (tvSheetTarget != null) tvSheetTarget.setText(m.target > 0 ? "Target: " + m.target : "Target: -");

            if (tvSheetStriker != null) tvSheetStriker.setText("🏏 " + (m.strikerName.isEmpty() ? "Striker" : m.strikerName));
            if (tvSheetStrikerStats != null) tvSheetStrikerStats.setText(m.strikerRuns + " (" + m.strikerBalls + "b, " + m.strikerFours + "x4, " + m.strikerSixes + "x6) SR " + m.strikerSR);

            if (tvSheetNonStriker != null) tvSheetNonStriker.setText((m.nonStrikerName.isEmpty() ? "Non-Striker" : m.nonStrikerName));
            if (tvSheetNonStrikerStats != null) tvSheetNonStrikerStats.setText(m.nonStrikerRuns + " (" + m.nonStrikerBalls + "b, " + m.nonStrikerFours + "x4, " + m.nonStrikerSixes + "x6) SR " + m.nonStrikerSR);

            if (tvSheetBowler != null) tvSheetBowler.setText("🎯 " + (m.bowlerName.isEmpty() ? "Bowler" : m.bowlerName));
            if (tvSheetBowlerStats != null) tvSheetBowlerStats.setText(m.bowlerFigures + " (Econ " + m.bowlerEconomy + ")");

            if (tvSheetPartnership != null) tvSheetPartnership.setText(m.partnership);
            if (tvSheetExtras != null) tvSheetExtras.setText(m.extras + " Extras");
        } else if (activeLocalMatchData != null) {
            MatchData m = activeLocalMatchData;
            if (tvSheetMatchTitle != null) tvSheetMatchTitle.setText((m.team1Name != null ? m.team1Name : "Team 1") + " vs " + (m.team2Name != null ? m.team2Name : "Team 2"));
            if (tvSheetVenueInfo != null) tvSheetVenueInfo.setText("Local Match • " + m.totalOvers + " Overs");
            if (tvSheetLiveBadge != null) tvSheetLiveBadge.setText("● LIVE");
            if (tvSheetBattingTeam != null) tvSheetBattingTeam.setText("Batting: " + m.getBattingTeamName());
            if (tvSheetInningsTag != null) tvSheetInningsTag.setText(m.isSecondInnings ? "Innings 2" : "Innings 1");
            if (tvSheetScore != null) tvSheetScore.setText(m.totalRuns + "/" + m.totalWickets);
            if (tvSheetOvers != null) tvSheetOvers.setText(" (" + m.currentOvers + "." + m.currentBalls + " Ov)");
            if (tvSheetCRR != null) tvSheetCRR.setText("CRR: " + calculateCRR(m.totalRuns, m.currentOvers, m.currentBalls));
            if (tvSheetRRR != null) tvSheetRRR.setText(m.isSecondInnings ? "RRR: -" : "Target: -");
            if (tvSheetTarget != null) tvSheetTarget.setText(m.isSecondInnings ? "Target: " + m.targetRuns : "Target: -");

            if (tvSheetStriker != null) tvSheetStriker.setText("🏏 " + (m.strikerName != null ? m.strikerName : "Striker"));
            if (tvSheetStrikerStats != null) tvSheetStrikerStats.setText(m.strikerRuns + " (" + m.strikerBalls + "b, " + m.striker4s + "x4, " + m.striker6s + "x6) SR " + calculateSR(m.strikerRuns, m.strikerBalls));

            if (tvSheetNonStriker != null) tvSheetNonStriker.setText(m.nonStrikerName != null ? m.nonStrikerName : "Non-Striker");
            if (tvSheetNonStrikerStats != null) tvSheetNonStrikerStats.setText(m.nonStrikerRuns + " (" + m.nonStrikerBalls + "b, " + m.nonStriker4s + "x4, " + m.nonStriker6s + "x6) SR " + calculateSR(m.nonStrikerRuns, m.nonStrikerBalls));

            if (tvSheetBowler != null) tvSheetBowler.setText("🎯 " + (m.currentBowlerName != null ? m.currentBowlerName : "Bowler"));
            if (tvSheetBowlerStats != null) tvSheetBowlerStats.setText((m.bowlerBallsBowled / 6) + "." + (m.bowlerBallsBowled % 6) + "-" + m.currentBowlerMaidens + "-" + m.bowlerRuns + "-" + m.bowlerWickets + " (Econ " + calculateEcon(m.bowlerRuns, m.bowlerBallsBowled / 6, m.bowlerBallsBowled % 6) + ")");

            if (tvSheetPartnership != null) tvSheetPartnership.setText(m.partnershipRuns + " runs (" + m.partnershipBalls + " balls)");
            if (tvSheetExtras != null) tvSheetExtras.setText(m.getTotalExtras() + " (wd " + m.extraWide + ", nb " + m.extraNoBall + ", b " + m.extraByes + ", lb " + m.extraLegByes + ")");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 2. TOURNAMENTS TAB (Screen 3 - Real Data & Clean Empty State)
    // ═════════════════════════════════════════════════════════════════════════
    private void renderTournamentsTab(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_viewer_tournaments, viewerContentContainer, false);
        viewerContentContainer.addView(view);

        ImageView btnTournamentsBack = view.findViewById(R.id.btnTournamentsBack);
        if (btnTournamentsBack != null) btnTournamentsBack.setOnClickListener(v -> showNavTab(0));

        LinearLayout subtabFixtures = view.findViewById(R.id.subtabFixtures);
        LinearLayout subtabPoints = view.findViewById(R.id.subtabPoints);
        LinearLayout subtabResult = view.findViewById(R.id.subtabResult);
        LinearLayout subtabAwards = view.findViewById(R.id.subtabAwards);

        FrameLayout tournamentContentContainer = view.findViewById(R.id.tournamentContentContainer);

        Runnable updateTournamentSubtabs = () -> {
            int green = Color.parseColor("#16A34A");
            int slate = Color.parseColor("#64748B");

            TextView tvF = view.findViewById(R.id.tvSubtabFixtures);
            TextView tvP = view.findViewById(R.id.tvSubtabPoints);
            TextView tvR = view.findViewById(R.id.tvSubtabResult);
            TextView tvA = view.findViewById(R.id.tvSubtabAwards);

            View lineF = view.findViewById(R.id.lineSubtabFixtures);
            View lineP = view.findViewById(R.id.lineSubtabPoints);
            View lineR = view.findViewById(R.id.lineSubtabResult);
            View lineA = view.findViewById(R.id.lineSubtabAwards);

            if (tvF != null) {
                tvF.setTextColor(currentTournamentSubtab == 0 ? green : slate);
                tvF.setTypeface(null, currentTournamentSubtab == 0 ? Typeface.BOLD : Typeface.NORMAL);
            }
            if (lineF != null) lineF.setVisibility(currentTournamentSubtab == 0 ? View.VISIBLE : View.INVISIBLE);

            if (tvP != null) {
                tvP.setTextColor(currentTournamentSubtab == 1 ? green : slate);
                tvP.setTypeface(null, currentTournamentSubtab == 1 ? Typeface.BOLD : Typeface.NORMAL);
            }
            if (lineP != null) lineP.setVisibility(currentTournamentSubtab == 1 ? View.VISIBLE : View.INVISIBLE);

            if (tvR != null) {
                tvR.setTextColor(currentTournamentSubtab == 2 ? green : slate);
                tvR.setTypeface(null, currentTournamentSubtab == 2 ? Typeface.BOLD : Typeface.NORMAL);
            }
            if (lineR != null) lineR.setVisibility(currentTournamentSubtab == 2 ? View.VISIBLE : View.INVISIBLE);

            if (tvA != null) {
                tvA.setTextColor(currentTournamentSubtab == 3 ? green : slate);
                tvA.setTypeface(null, currentTournamentSubtab == 3 ? Typeface.BOLD : Typeface.NORMAL);
            }
            if (lineA != null) lineA.setVisibility(currentTournamentSubtab == 3 ? View.VISIBLE : View.INVISIBLE);

            if (tournamentContentContainer != null) {
                tournamentContentContainer.removeAllViews();

                // ✅ FIX: Fixtures/Points/Result সাবট্যাব এখন Firebase-এ ক্যাশ করা
                // সাম্প্রতিক (active) tournament স্ন্যাপশট থেকে রেন্ডার হয়। Awards
                // সাবট্যাব সরাসরি "players" নোড থেকে রিয়েলটাইম নিয়ে আসে (নিচে দেখুন)।
                if (currentTournamentSubtab == 3) {
                    renderAwardsSubtabContent(inflater, tournamentContentContainer);
                    return;
                }

                if (cachedTournamentSnapshot == null) {
                    tournamentContentContainer.addView(createEmptyStateView(
                            isTournamentLoading ? "Loading Tournament…" : "No Active Tournament",
                            isTournamentLoading ? "Fetching tournament data from Firebase."
                                    : "No tournament has been created yet. Once a tournament is set up on the scorer's device, it will appear here."));
                    return;
                }

                switch (currentTournamentSubtab) {
                    case 0:
                        renderFixturesSubtabContent(inflater, tournamentContentContainer, cachedTournamentSnapshot);
                        break;
                    case 1:
                        renderPointsSubtabContent(inflater, tournamentContentContainer, cachedTournamentSnapshot);
                        break;
                    case 2:
                        renderResultSubtabContent(inflater, tournamentContentContainer, cachedTournamentSnapshot);
                        break;
                }
            }
        };

        if (subtabFixtures != null) subtabFixtures.setOnClickListener(v -> { currentTournamentSubtab = 0; updateTournamentSubtabs.run(); });
        if (subtabPoints != null) subtabPoints.setOnClickListener(v -> { currentTournamentSubtab = 1; updateTournamentSubtabs.run(); });
        if (subtabResult != null) subtabResult.setOnClickListener(v -> { currentTournamentSubtab = 2; updateTournamentSubtabs.run(); });
        if (subtabAwards != null) subtabAwards.setOnClickListener(v -> { currentTournamentSubtab = 3; updateTournamentSubtabs.run(); });

        updateTournamentSubtabs.run();
        loadActiveTournament(updateTournamentSubtabs);
    }

    // ✅ FIX: Firebase "tournaments" নোড থেকে সবচেয়ে সাম্প্রতিক (created_at
    // অনুযায়ী) tournament খুঁজে বের করে cachedTournamentSnapshot-এ রাখা হয়,
    // যাতে Fixtures/Points/Result সাবট্যাব বদলানোর সময় বারবার নেটওয়ার্ক কল
    // করতে না হয়।
    private void loadActiveTournament(Runnable onLoaded) {
        if (tournamentsRef == null) {
            isTournamentLoading = false;
            onLoaded.run();
            return;
        }
        isTournamentLoading = true;
        tournamentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isTournamentLoading = false;
                DataSnapshot latest = null;
                long latestCreated = -1;
                for (DataSnapshot t : snapshot.getChildren()) {
                    long created = getSnapLong(t.child("info"), "created_at", 0L);
                    if (latest == null || created >= latestCreated) {
                        latest = t;
                        latestCreated = created;
                    }
                }
                cachedTournamentSnapshot = latest;
                cachedTournamentName = latest != null ? getSnapString(latest.child("info"), "name", "") : "";
                if (currentNavTab == 1) onLoaded.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isTournamentLoading = false;
                cachedTournamentSnapshot = null;
                if (currentNavTab == 1) onLoaded.run();
            }
        });
    }

    private double getSnapDouble(DataSnapshot snap, String key, double defaultVal) {
        if (snap == null || !snap.hasChild(key)) return defaultVal;
        Object val = snap.child(key).getValue();
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) {
            try { return Double.parseDouble((String) val); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    // ✅ FIX: আগে এই মেথড লোকাল SharedPreferences("TournamentData")/"ALL_DATA"
    // থেকে গ্রুপ/QF/SF/Final স্ট্রাকচার পড়ত (শুধু স্কোরারের নিজের ডিভাইসে কাজ
    // করত)। এখন Firebase-এর tournaments/{id}/fixtures নোড থেকে সরাসরি পড়া
    // হচ্ছে, যা FirebaseSync.addFixture() দিয়ে সেভ হয় এবং সব ডিভাইসে দেখা যাবে।
    private void renderFixturesSubtabContent(LayoutInflater inflater, ViewGroup container, DataSnapshot tournamentSnap) {
        View subView = inflater.inflate(R.layout.view_subtab_fixtures_content, container, false);
        container.addView(subView);

        LinearLayout fixturesContainer = subView.findViewById(R.id.containerDynamicFixtures);
        if (fixturesContainer == null) return;
        fixturesContainer.removeAllViews();

        DataSnapshot fixturesSnap = tournamentSnap.child("fixtures");
        if (!fixturesSnap.exists() || !fixturesSnap.hasChildren()) {
            fixturesContainer.addView(createEmptyStateView("No Tournament Fixtures", "No active tournament fixtures are scheduled yet. Once a tournament is created, schedules will appear here."));
            return;
        }

        List<DataSnapshot> liveList = new ArrayList<>();
        List<DataSnapshot> scheduledList = new ArrayList<>();
        List<DataSnapshot> completedList = new ArrayList<>();
        for (DataSnapshot f : fixturesSnap.getChildren()) {
            String status = getSnapString(f, "status", "scheduled");
            if (status.equalsIgnoreCase("live")) liveList.add(f);
            else if (status.equalsIgnoreCase("completed")) completedList.add(f);
            else scheduledList.add(f);
        }

        boolean hasAny = false;
        if (!liveList.isEmpty()) {
            hasAny = true;
            addTournamentSectionHeader(fixturesContainer, "🔴 LIVE");
            addFirebaseFixturesCard(fixturesContainer, liveList);
        }
        if (!scheduledList.isEmpty()) {
            hasAny = true;
            addTournamentSectionHeader(fixturesContainer, "📅 SCHEDULED");
            addFirebaseFixturesCard(fixturesContainer, scheduledList);
        }
        if (!completedList.isEmpty()) {
            hasAny = true;
            addTournamentSectionHeader(fixturesContainer, "✅ COMPLETED");
            addFirebaseFixturesCard(fixturesContainer, completedList);
        }

        if (!hasAny) {
            fixturesContainer.addView(createEmptyStateView("No Fixtures Scheduled", "Tournament groups are configured, but no matches are generated yet."));
        }
    }

    private void addFirebaseFixturesCard(LinearLayout container, List<DataSnapshot> matches) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_viewer_card);
        card.setPadding(0, dpToPx(6), 0, dpToPx(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(14));
        card.setLayoutParams(lp);

        for (int i = 0; i < matches.size(); i++) {
            DataSnapshot m = matches.get(i);
            String t1 = getSnapString(m, "team_a", "Team 1");
            String t2 = getSnapString(m, "team_b", "Team 2");
            String date = getSnapString(m, "date", "");
            String venue = getSnapString(m, "venue", "");

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));

            LinearLayout teamRow = new LinearLayout(this);
            teamRow.setOrientation(LinearLayout.HORIZONTAL);
            teamRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvT1 = new TextView(this);
            tvT1.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tvT1.setText(t1);
            tvT1.setTextSize(14);
            tvT1.setTypeface(null, Typeface.BOLD);
            tvT1.setTextColor(Color.parseColor("#0F172A"));
            teamRow.addView(tvT1);

            TextView tvVs = new TextView(this);
            tvVs.setText("VS");
            tvVs.setGravity(Gravity.CENTER);
            tvVs.setTextSize(11);
            tvVs.setTypeface(null, Typeface.BOLD);
            tvVs.setTextColor(Color.parseColor("#16A34A"));
            tvVs.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            teamRow.addView(tvVs);

            TextView tvT2 = new TextView(this);
            tvT2.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tvT2.setGravity(Gravity.END);
            tvT2.setText(t2);
            tvT2.setTextSize(14);
            tvT2.setTypeface(null, Typeface.BOLD);
            tvT2.setTextColor(Color.parseColor("#0F172A"));
            teamRow.addView(tvT2);

            row.addView(teamRow);

            String subLine = (date.isEmpty() ? "" : date) + (venue.isEmpty() ? "" : (date.isEmpty() ? venue : " • " + venue));
            if (!subLine.isEmpty()) {
                TextView tvSub = new TextView(this);
                tvSub.setText(subLine);
                tvSub.setTextSize(11);
                tvSub.setTextColor(Color.parseColor("#94A3B8"));
                tvSub.setPadding(0, dpToPx(4), 0, 0);
                row.addView(tvSub);
            }

            card.addView(row);

            if (i < matches.size() - 1) {
                View div = new View(this);
                div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                div.setBackgroundColor(Color.parseColor("#F1F5F9"));
                card.addView(div);
            }
        }

        container.addView(card);
    }

    private void addTournamentSectionHeader(LinearLayout container, String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#64748B"));
        tv.setPadding(dpToPx(4), dpToPx(10), dpToPx(4), dpToPx(6));
        container.addView(tv);
    }

    private void addFixturesCard(LinearLayout container, JSONArray matches, List<String> teams) throws Exception {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_viewer_card);
        card.setPadding(0, dpToPx(6), 0, dpToPx(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(14));
        card.setLayoutParams(lp);

        for (int i = 0; i < matches.length(); i++) {
            JSONObject m = matches.getJSONObject(i);
            int idx1 = m.optInt("team1_idx", -1);
            int idx2 = m.optInt("team2_idx", -1);
            String t1 = (idx1 >= 0 && idx1 < teams.size()) ? teams.get(idx1) : "Team 1";
            String t2 = (idx2 >= 0 && idx2 < teams.size()) ? teams.get(idx2) : "Team 2";

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));

            // Team 1
            TextView tvT1 = new TextView(this);
            tvT1.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tvT1.setText(t1);
            tvT1.setTextSize(14);
            tvT1.setTypeface(null, Typeface.BOLD);
            tvT1.setTextColor(Color.parseColor("#0F172A"));
            row.addView(tvT1);

            // VS Badge
            TextView tvVs = new TextView(this);
            tvVs.setText("VS");
            tvVs.setGravity(Gravity.CENTER);
            tvVs.setTextSize(11);
            tvVs.setTypeface(null, Typeface.BOLD);
            tvVs.setTextColor(Color.parseColor("#16A34A"));
            tvVs.setBackgroundResource(R.drawable.bg_vs_badge_viewer);
            tvVs.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            row.addView(tvVs);

            // Team 2
            TextView tvT2 = new TextView(this);
            tvT2.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tvT2.setGravity(Gravity.END);
            tvT2.setText(t2);
            tvT2.setTextSize(14);
            tvT2.setTypeface(null, Typeface.BOLD);
            tvT2.setTextColor(Color.parseColor("#0F172A"));
            row.addView(tvT2);

            card.addView(row);

            if (i < matches.length() - 1) {
                View div = new View(this);
                div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                div.setBackgroundColor(Color.parseColor("#F1F5F9"));
                card.addView(div);
            }
        }

        container.addView(card);
    }

    // ✅ FIX: আগে লোকাল "DynamicGroups" প্রেফারেন্স থেকে শুধু গ্রুপে থাকা টিমের
    // নাম দেখানো হতো (played/won/lost/points সবসময় 0)। এখন Firebase-এর
    // tournaments/{id}/point_table নোড থেকে প্রকৃত পয়েন্ট টেবিল ডেটা
    // (FirebaseSync.upsertPointTable() দিয়ে সেভ করা) পয়েন্ট অনুযায়ী সাজিয়ে
    // দেখানো হচ্ছে।
    private void renderPointsSubtabContent(LayoutInflater inflater, ViewGroup container, DataSnapshot tournamentSnap) {
        View subView = inflater.inflate(R.layout.view_subtab_points_content, container, false);
        container.addView(subView);

        LinearLayout pointsContainer = subView.findViewById(R.id.containerPointsRows);
        if (pointsContainer == null) return;
        pointsContainer.removeAllViews();

        DataSnapshot ptSnap = tournamentSnap.child("point_table");
        if (!ptSnap.exists() || !ptSnap.hasChildren()) {
            pointsContainer.addView(createEmptyStateView("No Standings Data", "Points table will be generated as tournament matches are played."));
            return;
        }

        List<DataSnapshot> rows = new ArrayList<>();
        for (DataSnapshot r : ptSnap.getChildren()) rows.add(r);
        Collections.sort(rows, (a, b) -> {
            int pa = getSnapInt(a, "points", 0);
            int pb = getSnapInt(b, "points", 0);
            if (pb != pa) return Integer.compare(pb, pa);
            double nrra = getSnapDouble(a, "net_run_rate", 0.0);
            double nrrb = getSnapDouble(b, "net_run_rate", 0.0);
            return Double.compare(nrrb, nrra);
        });

        for (DataSnapshot r : rows) {
            String teamName = getSnapString(r, "team_name", r.getKey());
            int played = getSnapInt(r, "played", 0);
            int won = getSnapInt(r, "won", 0);
            int lost = getSnapInt(r, "lost", 0);
            int points = getSnapInt(r, "points", 0);
            double nrr = getSnapDouble(r, "net_run_rate", 0.0);
            String nrrStr = (nrr >= 0 ? "+" : "") + String.format(Locale.ENGLISH, "%.3f", nrr);
            pointsContainer.addView(createPointRow(teamName, played, won, lost, points, nrrStr));
        }
    }

    private View createPointRow(String team, int p, int w, int l, int pts, String nrr) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(8), dpToPx(10), dpToPx(8), dpToPx(10));

        TextView tvTeam = new TextView(this);
        tvTeam.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 3));
        tvTeam.setText(team);
        tvTeam.setTextSize(13);
        tvTeam.setTypeface(null, Typeface.BOLD);
        tvTeam.setTextColor(Color.parseColor("#0F172A"));

        TextView tvP = createCellTextView(String.valueOf(p), 1, Gravity.CENTER, Color.parseColor("#475569"), false);
        TextView tvW = createCellTextView(String.valueOf(w), 1, Gravity.CENTER, Color.parseColor("#475569"), false);
        TextView tvL = createCellTextView(String.valueOf(l), 1, Gravity.CENTER, Color.parseColor("#475569"), false);
        TextView tvPts = createCellTextView(String.valueOf(pts), 1.2f, Gravity.CENTER, Color.parseColor("#16A34A"), true);
        TextView tvNrr = createCellTextView(nrr, 1.5f, Gravity.END, Color.parseColor("#475569"), false);

        row.addView(tvTeam);
        row.addView(tvP);
        row.addView(tvW);
        row.addView(tvL);
        row.addView(tvPts);
        row.addView(tvNrr);

        return row;
    }

    private TextView createCellTextView(String text, float weight, int gravity, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        tv.setText(text);
        tv.setGravity(gravity);
        tv.setTextSize(13);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    // ✅ FIX: আগে লোকাল SharedPreferences("TournamentResult")/"RESULT_DATA"
    // থেকে ফলাফল পড়া হতো। এখন Firebase-এর tournaments/{id}/results নোড
    // থেকে পড়া হচ্ছে, যা FirebaseSync.saveTournamentResult() (match শেষে
    // saveMatchHistory() থেকে অটো-কল হয়) দিয়ে সেভ হয়।
    private void renderResultSubtabContent(LayoutInflater inflater, ViewGroup container, DataSnapshot tournamentSnap) {
        View subView = inflater.inflate(R.layout.view_subtab_results_content, container, false);
        container.addView(subView);

        LinearLayout list = subView.findViewById(R.id.containerTournamentResults);
        if (list == null) return;
        list.removeAllViews();

        DataSnapshot resultsSnap = tournamentSnap.child("results");
        if (!resultsSnap.exists() || !resultsSnap.hasChildren()) {
            list.addView(createEmptyStateView("No Match Results", "No completed tournament match results have been recorded yet."));
            return;
        }

        List<DataSnapshot> results = new ArrayList<>();
        for (DataSnapshot r : resultsSnap.getChildren()) results.add(r);
        Collections.sort(results, (a, b) -> {
            long ta = getSnapLong(a, "timestamp", 0L);
            long tb = getSnapLong(b, "timestamp", 0L);
            return Long.compare(tb, ta);
        });

        for (DataSnapshot r : results) {
            String t1 = getSnapString(r, "team_a", "Team 1");
            String s1 = getSnapString(r, "team_a_score", "-");
            String t2 = getSnapString(r, "team_b", "Team 2");
            String s2 = getSnapString(r, "team_b_score", "-");
            String txt = getSnapString(r, "result_text", "Match Result");
            list.addView(createResultCard("Match Result", t1, s1, t2, s2, txt));
        }
    }

    private View createResultCard(String stage, String t1, String s1, String t2, String s2, String winner) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_viewer_card);
        card.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(lp);

        TextView tvStage = new TextView(this);
        tvStage.setText(stage);
        tvStage.setTextSize(11);
        tvStage.setTextColor(Color.parseColor("#64748B"));
        card.addView(tvStage);

        LinearLayout scoreRow1 = new LinearLayout(this);
        scoreRow1.setOrientation(LinearLayout.HORIZONTAL);
        scoreRow1.setGravity(Gravity.CENTER_VERTICAL);
        scoreRow1.setPadding(0, dpToPx(6), 0, dpToPx(2));

        TextView tvT1 = new TextView(this);
        tvT1.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tvT1.setText(t1);
        tvT1.setTextSize(14);
        tvT1.setTypeface(null, Typeface.BOLD);
        tvT1.setTextColor(Color.parseColor("#0F172A"));

        TextView tvS1 = new TextView(this);
        tvS1.setText(s1);
        tvS1.setTextSize(13);
        tvS1.setTextColor(Color.parseColor("#334155"));

        scoreRow1.addView(tvT1);
        scoreRow1.addView(tvS1);
        card.addView(scoreRow1);

        LinearLayout scoreRow2 = new LinearLayout(this);
        scoreRow2.setOrientation(LinearLayout.HORIZONTAL);
        scoreRow2.setGravity(Gravity.CENTER_VERTICAL);
        scoreRow2.setPadding(0, dpToPx(2), 0, dpToPx(8));

        TextView tvT2 = new TextView(this);
        tvT2.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tvT2.setText(t2);
        tvT2.setTextSize(14);
        tvT2.setTypeface(null, Typeface.BOLD);
        tvT2.setTextColor(Color.parseColor("#0F172A"));

        TextView tvS2 = new TextView(this);
        tvS2.setText(s2);
        tvS2.setTextSize(13);
        tvS2.setTextColor(Color.parseColor("#334155"));

        scoreRow2.addView(tvT2);
        scoreRow2.addView(tvS2);
        card.addView(scoreRow2);

        TextView tvResult = new TextView(this);
        tvResult.setText("🏆 " + winner);
        tvResult.setTextSize(13);
        tvResult.setTypeface(null, Typeface.BOLD);
        tvResult.setTextColor(Color.parseColor("#16A34A"));
        card.addView(tvResult);

        return card;
    }

    // ✅ FIX: আগে লোকাল SharedPreferences("TournamentRankings") থেকে
    // ব্যাটসম্যান/বোলার স্ট্যাটস পড়া হতো — অন্য ডিভাইসে ফাঁকা থাকত। এখন
    // Firebase-এর "players" নোড (FirebaseSync.syncPlayerStats() দিয়ে প্রতি
    // ম্যাচ শেষে runs/wickets ইনক্রিমেন্ট হয়) থেকে সরাসরি লিডিং রান
    // স্কোরার/উইকেট টেকার বের করা হচ্ছে।
    private void renderAwardsSubtabContent(LayoutInflater inflater, ViewGroup container) {
        View subView = inflater.inflate(R.layout.view_subtab_awards_content, container, false);
        container.addView(subView);

        LinearLayout list = subView.findViewById(R.id.containerTournamentAwards);
        if (list == null) return;
        list.removeAllViews();
        list.addView(createEmptyStateView("Loading Awards…", "Fetching player stats from Firebase."));

        if (playersRef == null) {
            list.removeAllViews();
            list.addView(createEmptyStateView("No Awards Calculated Yet", "Tournament player rankings and awards will be evaluated as match statistics are saved."));
            return;
        }

        playersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (currentNavTab != 1 || currentTournamentSubtab != 3) return;
                list.removeAllViews();

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    list.addView(createEmptyStateView("No Awards Calculated Yet", "Tournament player rankings and awards will be evaluated as match statistics are saved."));
                    return;
                }

                String topBatName = "";
                int maxRuns = -1;
                String topBowlName = "";
                int maxWickets = -1;

                for (DataSnapshot p : snapshot.getChildren()) {
                    String name = getSnapString(p, "player_name", p.getKey());
                    DataSnapshot stats = p.child("stats");
                    int runs = getSnapInt(stats, "runs", 0);
                    int wickets = getSnapInt(stats, "wickets", 0);
                    if (runs > maxRuns) { maxRuns = runs; topBatName = name; }
                    if (wickets > maxWickets) { maxWickets = wickets; topBowlName = name; }
                }

                if (maxRuns <= 0 && maxWickets <= 0) {
                    list.addView(createEmptyStateView("No Awards Calculated Yet", "Tournament player rankings and awards will be evaluated as match statistics are saved."));
                    return;
                }

                if (maxRuns > 0) list.addView(createAwardCard("🏏 Leading Run Scorer", topBatName, maxRuns + " Total Runs"));
                if (maxWickets > 0) list.addView(createAwardCard("🎯 Leading Wicket Taker", topBowlName, maxWickets + " Total Wickets"));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (currentNavTab != 1 || currentTournamentSubtab != 3) return;
                list.removeAllViews();
                list.addView(createEmptyStateView("Awards Unavailable", "Unable to load tournament awards."));
            }
        });
    }

    private View createAwardCard(String title, String recipient, String stats) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_viewer_card);
        card.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(13);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        card.addView(tvTitle);

        TextView tvRecipient = new TextView(this);
        tvRecipient.setText(recipient);
        tvRecipient.setTextSize(14);
        tvRecipient.setTypeface(null, Typeface.BOLD);
        tvRecipient.setTextColor(Color.parseColor("#16A34A"));
        tvRecipient.setPadding(0, dpToPx(4), 0, dpToPx(2));
        card.addView(tvRecipient);

        TextView tvStats = new TextView(this);
        tvStats.setText(stats);
        tvStats.setTextSize(12);
        tvStats.setTextColor(Color.parseColor("#64748B"));
        card.addView(tvStats);

        return card;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 3. TEAMS TAB (Real Data & Clean Empty State)
    // ═════════════════════════════════════════════════════════════════════════
    private void renderTeamsTab(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_viewer_teams, viewerContentContainer, false);
        viewerContentContainer.addView(view);

        ImageView btnTeamsBack = view.findViewById(R.id.btnTeamsBack);
        if (btnTeamsBack != null) btnTeamsBack.setOnClickListener(v -> showNavTab(0));

        LinearLayout containerTeamsList = view.findViewById(R.id.containerTeamsList);
        if (containerTeamsList == null) return;
        containerTeamsList.removeAllViews();
        containerTeamsList.addView(createEmptyStateView("Loading Teams…", "Fetching teams from Firebase."));

        // ✅ FIX: আগে DataManager.getAllTeams()/getPlayers() দিয়ে এই ডিভাইসের
        // লোকাল স্টোরেজ থেকে টিম পড়া হতো — অন্য ডিভাইসে (দর্শকের ফোনে) Viewer
        // খুললে কিছুই দেখাত না। এখন সরাসরি Firebase "teams" নোড থেকে পড়া হচ্ছে,
        // যেখানে স্কোরারের ডিভাইস FirebaseSync.upsertTeam() দিয়ে টিম/প্লেয়ার সেভ করে।
        if (teamsRef == null) {
            containerTeamsList.removeAllViews();
            containerTeamsList.addView(createEmptyStateView("No Registered Teams", "Teams created in Team Manager will automatically appear here with their complete squads."));
            return;
        }

        teamsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (currentNavTab != 2) return; // ইতিমধ্যে অন্য ট্যাবে চলে গেলে UI আপডেট করার দরকার নেই
                containerTeamsList.removeAllViews();

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    containerTeamsList.addView(createEmptyStateView("No Registered Teams", "Teams created in Team Manager will automatically appear here with their complete squads."));
                    return;
                }

                for (DataSnapshot teamSnap : snapshot.getChildren()) {
                    String teamName = getSnapString(teamSnap, "team_name", teamSnap.getKey());
                    ArrayList<String> players = new ArrayList<>();
                    DataSnapshot playersSnap = teamSnap.child("players");
                    if (playersSnap.exists()) {
                        for (DataSnapshot pSnap : playersSnap.getChildren()) {
                            Object pName = pSnap.getValue();
                            if (pName != null) players.add(String.valueOf(pName));
                        }
                    }
                    containerTeamsList.addView(createTeamCardView(teamName, players.size(), players));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (currentNavTab != 2) return;
                containerTeamsList.removeAllViews();
                containerTeamsList.addView(createEmptyStateView("Unable to Load Teams", "Could not fetch teams from Firebase: " + error.getMessage()));
            }
        });
    }

    private View createTeamCardView(String teamName, int playerCount, ArrayList<String> players) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.bg_viewer_card);
        card.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(lp);

        // Team Logo or Initial circle
        FrameLayout logoContainer = new FrameLayout(this);
        logoContainer.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(42), dpToPx(42)));

        TextView tvInitial = new TextView(this);
        tvInitial.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tvInitial.setGravity(Gravity.CENTER);
        tvInitial.setText(teamName.isEmpty() ? "T" : String.valueOf(teamName.charAt(0)).toUpperCase(Locale.ENGLISH));
        tvInitial.setTextSize(16);
        tvInitial.setTypeface(null, Typeface.BOLD);
        tvInitial.setTextColor(Color.WHITE);
        tvInitial.setBackgroundResource(R.drawable.bg_avatar_circle);
        logoContainer.addView(tvInitial);

        ImageView ivLogo = new ImageView(this);
        ivLogo.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivLogo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivLogo.setVisibility(View.GONE);
        logoContainer.addView(ivLogo);

        ImageStorageHelper.loadTeamLogoInto(this, teamName, ivLogo, tvInitial);

        card.addView(logoContainer);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        textLp.setMargins(dpToPx(12), 0, dpToPx(8), 0);
        textCol.setLayoutParams(textLp);

        TextView tvName = new TextView(this);
        tvName.setText(teamName);
        tvName.setTextSize(15);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#0F172A"));
        textCol.addView(tvName);

        TextView tvCount = new TextView(this);
        tvCount.setText(playerCount + " Squad Members");
        tvCount.setTextSize(12);
        tvCount.setTextColor(Color.parseColor("#64748B"));
        textCol.addView(tvCount);

        card.addView(textCol);

        TextView tvViewSquad = new TextView(this);
        tvViewSquad.setText("View Squad →");
        tvViewSquad.setTextSize(12);
        tvViewSquad.setTypeface(null, Typeface.BOLD);
        tvViewSquad.setTextColor(Color.parseColor("#16A34A"));
        card.addView(tvViewSquad);

        card.setOnClickListener(v -> showTeamSquadDialog(teamName, players));

        return card;
    }

    private void showTeamSquadDialog(String teamName, ArrayList<String> players) {
        if (players == null || players.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(teamName + " — Squad")
                    .setMessage("No players added to this team yet.")
                    .setPositiveButton("Close", null)
                    .show();
            return;
        }

        BottomSheetDialog squadDialog = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(18), dpToPx(16), dpToPx(18), dpToPx(24));
        root.setBackgroundColor(Color.WHITE);

        // Header Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("👥 " + teamName + " — Squad (" + players.size() + ")");
        tvTitle.setTextSize(17);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        root.addView(tvTitle);

        // Hint subtitle
        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("Tap any player to view career statistics & status");
        tvSubtitle.setTextSize(12);
        tvSubtitle.setTextColor(Color.parseColor("#64748B"));
        tvSubtitle.setPadding(0, dpToPx(2), 0, dpToPx(12));
        root.addView(tvSubtitle);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout playerListLayout = new LinearLayout(this);
        playerListLayout.setOrientation(LinearLayout.VERTICAL);

        for (String pName : players) {
            LinearLayout pRow = new LinearLayout(this);
            pRow.setOrientation(LinearLayout.HORIZONTAL);
            pRow.setGravity(Gravity.CENTER_VERTICAL);
            pRow.setBackgroundResource(R.drawable.bg_viewer_card);
            pRow.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pLp.setMargins(0, 0, 0, dpToPx(8));
            pRow.setLayoutParams(pLp);
            pRow.setClickable(true);
            pRow.setFocusable(true);

            // Avatar container (Photo or Initial)
            FrameLayout photoContainer = new FrameLayout(this);
            photoContainer.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(38), dpToPx(38)));

            TextView tvInitial = new TextView(this);
            tvInitial.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tvInitial.setGravity(Gravity.CENTER);
            tvInitial.setText(pName.isEmpty() ? "P" : String.valueOf(pName.charAt(0)).toUpperCase(Locale.ENGLISH));
            tvInitial.setTextSize(14);
            tvInitial.setTypeface(null, Typeface.BOLD);
            tvInitial.setTextColor(Color.WHITE);
            tvInitial.setBackgroundResource(R.drawable.bg_avatar_circle);
            photoContainer.addView(tvInitial);

            ImageView ivPhoto = new ImageView(this);
            ivPhoto.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ivPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ivPhoto.setVisibility(View.GONE);
            photoContainer.addView(ivPhoto);

            ImageStorageHelper.loadPlayerPhotoInto(this, pName, ivPhoto, tvInitial);

            pRow.addView(photoContainer);

            // Name + Role column
            LinearLayout nameCol = new LinearLayout(this);
            nameCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            nameLp.setMargins(dpToPx(10), 0, dpToPx(8), 0);
            nameCol.setLayoutParams(nameLp);

            TextView tvName = new TextView(this);
            tvName.setText(pName);
            tvName.setTextSize(14);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setTextColor(Color.parseColor("#0F172A"));
            nameCol.addView(tvName);

            String role = DataManager.getPlayerRole(this, pName);
            TextView tvRole = new TextView(this);
            tvRole.setText((role != null && !role.isEmpty()) ? role : "Player");
            tvRole.setTextSize(11);
            tvRole.setTextColor(Color.parseColor("#16A34A"));
            nameCol.addView(tvRole);

            pRow.addView(nameCol);

            TextView tvArrow = new TextView(this);
            tvArrow.setText("Stats →");
            tvArrow.setTextSize(12);
            tvArrow.setTypeface(null, Typeface.BOLD);
            tvArrow.setTextColor(Color.parseColor("#2563EB"));
            pRow.addView(tvArrow);

            pRow.setOnClickListener(v -> {
                squadDialog.dismiss();
                Intent intent = new Intent(ViewerActivity.this, PlayerProfileActivity.class);
                intent.putExtra("PLAYER_NAME", pName);
                intent.putExtra("TEAM_NAME", teamName);
                intent.putExtra("IS_VIEWER_MODE", true);
                startActivity(intent);
            });

            playerListLayout.addView(pRow);
        }

        scroll.addView(playerListLayout);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        squadDialog.setContentView(root);
        squadDialog.show();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 4. HISTORY TAB (Screen 4 - Real Data & Clean Empty State)
    // ═════════════════════════════════════════════════════════════════════════
    private void renderHistoryTab(LayoutInflater inflater) {
        View view = inflater.inflate(R.layout.view_viewer_history, viewerContentContainer, false);
        viewerContentContainer.addView(view);

        ImageView btnHistoryBack = view.findViewById(R.id.btnHistoryBack);
        if (btnHistoryBack != null) btnHistoryBack.setOnClickListener(v -> showNavTab(0));

        ImageView btnHistoryRefresh = view.findViewById(R.id.btnHistoryRefresh);
        if (btnHistoryRefresh != null) {
            btnHistoryRefresh.setOnClickListener(v -> {
                Toast.makeText(this, "Match History Refreshed", Toast.LENGTH_SHORT).show();
                showNavTab(3);
            });
        }

        LinearLayout containerMatchHistory = view.findViewById(R.id.containerMatchHistory);
        if (containerMatchHistory == null) return;
        containerMatchHistory.removeAllViews();
        containerMatchHistory.addView(createEmptyStateView("Loading History…", "Fetching match history from Firebase."));

        // ✅ FIX: আগে DataManager.getAllMatches() দিয়ে এই ডিভাইসের লোকাল
        // ম্যাচ হিস্ট্রি পড়া হতো — অন্য ডিভাইসে (দর্শকের ফোনে) কিছুই দেখাত না।
        // এখন Firebase "matchHistory" নোড থেকে পড়া হচ্ছে, যেখানে স্কোরার ম্যাচ
        // শেষে FirebaseSync.saveMatchHistory() দিয়ে সেভ করে।
        if (matchHistoryRef == null) {
            containerMatchHistory.removeAllViews();
            containerMatchHistory.addView(createEmptyStateView("No Match History", "Saved and completed matches from scorers will be permanently cataloged here."));
            return;
        }

        matchHistoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (currentNavTab != 3) return;
                containerMatchHistory.removeAllViews();

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    containerMatchHistory.addView(createEmptyStateView("No Match History", "Saved and completed matches from scorers will be permanently cataloged here."));
                    return;
                }

                // সাম্প্রতিক ম্যাচ আগে দেখানোর জন্য saved_at অনুযায়ী sort করা হচ্ছে
                List<DataSnapshot> matches = new ArrayList<>();
                for (DataSnapshot m : snapshot.getChildren()) matches.add(m);
                Collections.sort(matches, (a, b) -> {
                    long ta = getSnapLong(a, "saved_at", 0L);
                    long tb = getSnapLong(b, "saved_at", 0L);
                    return Long.compare(tb, ta);
                });

                String lastDateHeader = null;
                for (DataSnapshot m : matches) {
                    String dateHeader = getSnapString(m, "match_date", "");
                    if (dateHeader.isEmpty()) dateHeader = "Recent Match";
                    if (!dateHeader.equals(lastDateHeader)) {
                        addDateHeader(containerMatchHistory, dateHeader);
                        lastDateHeader = dateHeader;
                    }
                    containerMatchHistory.addView(createFirebaseHistoryCard(m));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (currentNavTab != 3) return;
                containerMatchHistory.removeAllViews();
                containerMatchHistory.addView(createEmptyStateView("Unable to Load History", "Could not fetch match history from Firebase: " + error.getMessage()));
            }
        });
    }

    private long getSnapLong(DataSnapshot snap, String key, long defaultVal) {
        if (snap == null || !snap.hasChild(key)) return defaultVal;
        Object val = snap.child(key).getValue();
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) {
            try { return Long.parseLong((String) val); } catch (Exception ignored) {}
        }
        return defaultVal;
    }

    // ✅ FIX: matchHistory Firebase নোডের raw ফিল্ড (team_a, team_a_score,
    // team_b, team_b_score, result, tournament_name) থেকে সরাসরি হিস্ট্রি
    // কার্ড বানানো হচ্ছে — স্থানীয় MatchData অবজেক্ট প্রয়োজন হচ্ছে না
    private View createFirebaseHistoryCard(DataSnapshot m) {
        String teamA = getSnapString(m, "team_a", "Team 1");
        String teamB = getSnapString(m, "team_b", "Team 2");
        String teamAScore = getSnapString(m, "team_a_score", "-");
        String teamAOvers = getSnapString(m, "team_a_overs", "");
        String teamBScore = getSnapString(m, "team_b_score", "-");
        String teamBOvers = getSnapString(m, "team_b_overs", "");
        String result = getSnapString(m, "result", "");
        String tournamentName = getSnapString(m, "tournament_name", "");
        String status = getSnapString(m, "status", "completed");

        String s1 = teamAOvers.isEmpty() ? teamAScore : teamAScore + " (" + teamAOvers + " Ov)";
        String s2 = teamBOvers.isEmpty() ? teamBScore : teamBScore + " (" + teamBOvers + " Ov)";
        boolean isCompleted = status.equalsIgnoreCase("completed");
        String resultText = !result.isEmpty() ? "🏆 " + result : (isCompleted ? "🏆 Match Completed" : "⏳ Match In Progress");
        String mom = tournamentName.isEmpty() ? "N/A" : tournamentName;

        return buildFirebaseHistoryCardView(teamA, s1, teamB, s2, resultText, mom, isCompleted);
    }

    private View buildFirebaseHistoryCardView(String t1, String s1, String t2, String s2, String result, String tournamentLabel, boolean isCompleted) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_viewer_card);
        card.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(lp);

        // Header status badge row
        LinearLayout topHeaderRow = new LinearLayout(this);
        topHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        topHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
        topHeaderRow.setPadding(0, 0, 0, dpToPx(8));

        TextView tvStatusBadge = new TextView(this);
        if (isCompleted) {
            tvStatusBadge.setText("✔ COMPLETED");
            tvStatusBadge.setTextColor(Color.parseColor("#15803D"));
        } else {
            tvStatusBadge.setText("● IN PROGRESS");
            tvStatusBadge.setTextColor(Color.parseColor("#B45309"));
        }
        tvStatusBadge.setBackgroundResource(R.drawable.bg_role_chip);
        tvStatusBadge.setTextSize(10);
        tvStatusBadge.setTypeface(null, Typeface.BOLD);
        tvStatusBadge.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
        topHeaderRow.addView(tvStatusBadge);

        View space = new View(this);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, 1, 1);
        space.setLayoutParams(spLp);
        topHeaderRow.addView(space);

        if (!tournamentLabel.isEmpty() && !tournamentLabel.equals("N/A")) {
            TextView tvTournament = new TextView(this);
            tvTournament.setText(tournamentLabel);
            tvTournament.setTextSize(11);
            tvTournament.setTextColor(Color.parseColor("#94A3B8"));
            topHeaderRow.addView(tvTournament);
        }
        card.addView(topHeaderRow);

        // Row 1: Team 1
        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        r1.setGravity(Gravity.CENTER_VERTICAL);
        r1.setPadding(0, 0, 0, dpToPx(6));

        TextView tvT1 = new TextView(this);
        LinearLayout.LayoutParams lpT1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tvT1.setLayoutParams(lpT1);
        tvT1.setText(t1);
        tvT1.setTextSize(15);
        tvT1.setTypeface(null, Typeface.BOLD);
        tvT1.setTextColor(Color.parseColor("#0F172A"));
        r1.addView(tvT1);

        TextView tvS1 = new TextView(this);
        tvS1.setText(s1);
        tvS1.setTextSize(13);
        tvS1.setTextColor(Color.parseColor("#334155"));
        r1.addView(tvS1);
        card.addView(r1);

        // Row 2: Team 2
        LinearLayout r2 = new LinearLayout(this);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        r2.setGravity(Gravity.CENTER_VERTICAL);
        r2.setPadding(0, 0, 0, dpToPx(8));

        TextView tvT2 = new TextView(this);
        LinearLayout.LayoutParams lpT2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tvT2.setLayoutParams(lpT2);
        tvT2.setText(t2);
        tvT2.setTextSize(15);
        tvT2.setTypeface(null, Typeface.BOLD);
        tvT2.setTextColor(Color.parseColor("#0F172A"));
        r2.addView(tvT2);

        TextView tvS2 = new TextView(this);
        tvS2.setText(s2);
        tvS2.setTextSize(13);
        tvS2.setTextColor(Color.parseColor("#334155"));
        r2.addView(tvS2);
        card.addView(r2);

        // Result Text
        TextView tvResult = new TextView(this);
        tvResult.setText(result);
        tvResult.setTextSize(13);
        tvResult.setTypeface(null, Typeface.BOLD);
        tvResult.setTextColor(isCompleted ? Color.parseColor("#16A34A") : Color.parseColor("#D97706"));
        card.addView(tvResult);

        return card;
    }

    private void addDateHeader(LinearLayout container, String dateText) {
        TextView tvDate = new TextView(this);
        tvDate.setText(dateText);
        tvDate.setTextSize(13);
        tvDate.setTypeface(null, Typeface.BOLD);
        tvDate.setTextColor(Color.parseColor("#64748B"));
        tvDate.setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(6));
        container.addView(tvDate);
    }

    private boolean isMatchCompleted(MatchData md) {
        if (md == null || md.matchStatus == null || md.matchStatus.trim().isEmpty()) {
            return false;
        }
        String s = md.matchStatus.trim().toLowerCase(Locale.ENGLISH);
        if (s.equals("incomplete") || s.equals("in progress") || s.equals("live") || s.startsWith("incomplete")) {
            return false;
        }
        return s.equals("completed") || s.contains("won") || s.contains("tied") || s.contains("win");
    }

    private View createMatchHistoryCard(MatchData md) {
        String t1 = md.team1Name != null ? md.team1Name : "Team 1";
        String s1 = md.totalRuns + "/" + md.totalWickets + " (" + md.currentOvers + "." + md.currentBalls + " Ov)";
        String t2 = md.team2Name != null ? md.team2Name : "Team 2";

        boolean isComp = isMatchCompleted(md);
        String s2;
        if (md.isSecondInnings) {
            s2 = md.totalRuns + "/" + md.totalWickets + " (" + md.currentOvers + "." + md.currentBalls + " Ov)";
            s1 = (md.scoreInn1 != null && !md.scoreInn1.isEmpty()) ? md.scoreInn1 + " (" + md.oversInn1 + " Ov)" : s1;
        } else {
            s2 = isComp ? "Did not bat" : "Yet to bat";
        }

        String result;
        if (isComp) {
            if (md.matchResult != null && !md.matchResult.trim().isEmpty()) {
                result = "🏆 " + md.matchResult;
            } else if (md.matchStatus != null && !md.matchStatus.equalsIgnoreCase("Incomplete") && !md.matchStatus.equalsIgnoreCase("Live")) {
                result = "🏆 " + md.matchStatus;
            } else {
                result = "🏆 Match Completed";
            }
        } else {
            result = "⏳ Match Incomplete — In Progress (Over " + md.currentOvers + "." + md.currentBalls + ")";
        }

        String mom = (md.strikerName != null && !md.strikerName.isEmpty()) ? md.strikerName : "N/A";

        return buildHistoryCardView(t1, s1, t2, s2, result, mom, md, isComp);
    }

    private View buildHistoryCardView(String t1, String s1, String t2, String s2, String result, String mom, MatchData md, boolean isCompleted) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_viewer_card);
        card.setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(lp);

        // Header status badge row
        LinearLayout topHeaderRow = new LinearLayout(this);
        topHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        topHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
        topHeaderRow.setPadding(0, 0, 0, dpToPx(8));

        TextView tvStatusBadge = new TextView(this);
        if (isCompleted) {
            tvStatusBadge.setText("✔ COMPLETED");
            tvStatusBadge.setTextColor(Color.parseColor("#15803D"));
            tvStatusBadge.setBackgroundResource(R.drawable.bg_role_chip);
        } else {
            tvStatusBadge.setText("● IN PROGRESS");
            tvStatusBadge.setTextColor(Color.parseColor("#B45309"));
            tvStatusBadge.setBackgroundResource(R.drawable.bg_role_chip);
        }
        tvStatusBadge.setTextSize(10);
        tvStatusBadge.setTypeface(null, Typeface.BOLD);
        tvStatusBadge.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
        topHeaderRow.addView(tvStatusBadge);

        View space = new View(this);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, 1, 1);
        space.setLayoutParams(spLp);
        topHeaderRow.addView(space);

        String matchDate = (md.matchDate != null && !md.matchDate.isEmpty()) ? md.matchDate : "";
        if (!matchDate.isEmpty()) {
            TextView tvDate = new TextView(this);
            tvDate.setText(matchDate);
            tvDate.setTextSize(11);
            tvDate.setTextColor(Color.parseColor("#94A3B8"));
            topHeaderRow.addView(tvDate);
        }
        card.addView(topHeaderRow);

        // Row 1: Team 1
        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        r1.setGravity(Gravity.CENTER_VERTICAL);
        r1.setPadding(0, 0, 0, dpToPx(6));

        ImageView iv1 = new ImageView(this);
        iv1.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)));
        iv1.setImageResource(R.drawable.ic_cricket_ball_globe);
        iv1.setColorFilter(Color.parseColor("#1E293B"));
        r1.addView(iv1);

        TextView tvT1 = new TextView(this);
        LinearLayout.LayoutParams lpT1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lpT1.setMargins(dpToPx(8), 0, 0, 0);
        tvT1.setLayoutParams(lpT1);
        tvT1.setText(t1);
        tvT1.setTextSize(15);
        tvT1.setTypeface(null, Typeface.BOLD);
        tvT1.setTextColor(Color.parseColor("#0F172A"));
        r1.addView(tvT1);

        TextView tvS1 = new TextView(this);
        tvS1.setText(s1);
        tvS1.setTextSize(13);
        tvS1.setTextColor(Color.parseColor("#334155"));
        r1.addView(tvS1);
        card.addView(r1);

        // Row 2: Team 2
        LinearLayout r2 = new LinearLayout(this);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        r2.setGravity(Gravity.CENTER_VERTICAL);
        r2.setPadding(0, 0, 0, dpToPx(8));

        ImageView iv2 = new ImageView(this);
        iv2.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)));
        iv2.setImageResource(R.drawable.ic_cricket_ball_globe);
        iv2.setColorFilter(Color.parseColor("#1E293B"));
        r2.addView(iv2);

        TextView tvT2 = new TextView(this);
        LinearLayout.LayoutParams lpT2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lpT2.setMargins(dpToPx(8), 0, 0, 0);
        tvT2.setLayoutParams(lpT2);
        tvT2.setText(t2);
        tvT2.setTextSize(15);
        tvT2.setTypeface(null, Typeface.BOLD);
        tvT2.setTextColor(Color.parseColor("#0F172A"));
        r2.addView(tvT2);

        TextView tvS2 = new TextView(this);
        tvS2.setText(s2);
        tvS2.setTextSize(13);
        tvS2.setTextColor(Color.parseColor("#334155"));
        r2.addView(tvS2);
        card.addView(r2);

        // Result Text
        TextView tvResult = new TextView(this);
        tvResult.setText(result);
        tvResult.setTextSize(13);
        tvResult.setTypeface(null, Typeface.BOLD);
        tvResult.setTextColor(isCompleted ? Color.parseColor("#16A34A") : Color.parseColor("#D97706"));
        tvResult.setPadding(0, 0, 0, dpToPx(10));
        card.addView(tvResult);

        // Bottom Row
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER_VERTICAL);

        if (isCompleted) {
            TextView tvMom = new TextView(this);
            tvMom.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tvMom.setText("★ MOM: " + mom);
            tvMom.setTextSize(12);
            tvMom.setTypeface(null, Typeface.BOLD);
            tvMom.setTextColor(Color.parseColor("#D97706"));
            bottomRow.addView(tvMom);
        } else {
            TextView tvOngoing = new TextView(this);
            tvOngoing.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tvOngoing.setText("⚡ Live Match In Progress");
            tvOngoing.setTextSize(11);
            tvOngoing.setTextColor(Color.parseColor("#64748B"));
            bottomRow.addView(tvOngoing);
        }

        // View Scoreboard Button
        LinearLayout btnScoreboard = new LinearLayout(this);
        btnScoreboard.setOrientation(LinearLayout.HORIZONTAL);
        btnScoreboard.setGravity(Gravity.CENTER);
        btnScoreboard.setBackgroundResource(R.drawable.bg_btn_view_scoreboard);
        btnScoreboard.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        btnScoreboard.setClickable(true);
        btnScoreboard.setFocusable(true);

        TextView tvBtn = new TextView(this);
        tvBtn.setText("📊 View Scoreboard");
        tvBtn.setTextSize(11);
        tvBtn.setTypeface(null, Typeface.BOLD);
        tvBtn.setTextColor(Color.parseColor("#16A34A"));
        btnScoreboard.addView(tvBtn);

        btnScoreboard.setOnClickListener(v -> {
            Intent intent = new Intent(ViewerActivity.this, ScorecardActivity.class);
            intent.putExtra("MATCH_DATA", md);
            intent.putExtra("TEAM_1", t1);
            intent.putExtra("TEAM_2", t2);
            startActivity(intent);
        });

        bottomRow.addView(btnScoreboard);
        card.addView(bottomRow);

        return card;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPER & MATH METHODS
    // ═════════════════════════════════════════════════════════════════════════
    private View createEmptyStateView(String title, String message) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_viewer_card);
        card.setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(24));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpToPx(8), 0, dpToPx(12));
        card.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setGravity(Gravity.CENTER);
        card.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(message);
        tvMsg.setTextSize(12);
        tvMsg.setTextColor(Color.parseColor("#64748B"));
        tvMsg.setGravity(Gravity.CENTER);
        tvMsg.setPadding(0, dpToPx(4), 0, 0);
        card.addView(tvMsg);

        return card;
    }

    private List<String> parseAllTeams(JSONObject mainObj) {
        List<String> list = new ArrayList<>();
        try {
            JSONArray groups = mainObj.optJSONArray("DynamicGroups");
            if (groups != null) {
                for (int i = 0; i < groups.length(); i++) {
                    JSONArray arr = groups.getJSONObject(i).optJSONArray("teams");
                    if (arr != null) {
                        for (int j = 0; j < arr.length(); j++) {
                            String t = arr.getString(j);
                            if (!list.contains(t)) list.add(t);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    private String calculateCRR(int runs, int overs, int balls) {
        int totalLegalBalls = overs * 6 + balls;
        if (totalLegalBalls == 0) return "0.00";
        double crr = ((double) runs / totalLegalBalls) * 6.0;
        return String.format(Locale.ENGLISH, "%.2f", crr);
    }

    private String calculateSR(int runs, int balls) {
        if (balls == 0) return "0.00";
        double sr = ((double) runs / balls) * 100.0;
        return String.format(Locale.ENGLISH, "%.2f", sr);
    }

    private String calculateEcon(int runs, int overs, int balls) {
        int totalLegalBalls = overs * 6 + balls;
        if (totalLegalBalls == 0) return "0.00";
        double econ = ((double) runs / totalLegalBalls) * 6.0;
        return String.format(Locale.ENGLISH, "%.2f", econ);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (currentNavTab != 0) {
            showNavTab(0);
        } else {
            ExitDialogHelper.show(this);
        }
    }
}
