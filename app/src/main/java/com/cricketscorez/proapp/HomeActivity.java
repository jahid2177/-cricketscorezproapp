package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.cricketscorez.proapp.fcm.FcmScoreNotifier;
import java.util.ArrayList;

public class HomeActivity extends Activity {

    // Views
    private SwipeRefreshLayout swipeRefreshHome;
    private ScrollView rootScrollView;
    private TextView tvAppTitle, tvGreeting, tvThemeIcon;
    private View btnTheme, btnAbout, btnShare;

    // Main Menu Pill Buttons
    private View btnStartNewMatch, btnTournament, btnTeams, btnHistory, btnMatchSetting;
    private TextView tvTitleStartNewMatch, tvTitleTournament, tvTitleTeams, tvTitleHistory, tvTitleMatchSetting;

    // Recent Match card (Minimal / Discrete)
    private View cardRecentMatch, btnRecentShare;
    private TextView tvRecentTeams, tvRecentScore, tvRecentResult;

    private MatchData latestMatchData = null;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Android 13+ notification permission
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{"android.permission.POST_NOTIFICATIONS"}, 101);
            }
        }

        bindViews();
        setupListeners();
        setupSwipeRefresh();
        applyThemeToUI();
    }

    private void bindViews() {
        swipeRefreshHome       = findViewById(R.id.swipeRefreshHome);
        rootScrollView         = findViewById(R.id.rootScrollView);
        tvAppTitle             = findViewById(R.id.tvAppTitle);
        tvGreeting             = findViewById(R.id.tvGreeting);
        btnTheme               = findViewById(R.id.btnTheme);
        tvThemeIcon            = findViewById(R.id.tvThemeIcon);
        btnAbout               = findViewById(R.id.btnAbout);
        btnShare               = findViewById(R.id.btnShare);

        btnStartNewMatch       = findViewById(R.id.btnStartNewMatch);
        tvTitleStartNewMatch   = findViewById(R.id.tvTitleStartNewMatch);

        btnTournament          = findViewById(R.id.btnTournament);
        tvTitleTournament      = findViewById(R.id.tvTitleTournament);

        btnTeams               = findViewById(R.id.btnTeams);
        tvTitleTeams           = findViewById(R.id.tvTitleTeams);

        btnHistory             = findViewById(R.id.btnHistory);
        tvTitleHistory         = findViewById(R.id.tvTitleHistory);

        btnMatchSetting        = findViewById(R.id.btnMatchSetting);
        tvTitleMatchSetting    = findViewById(R.id.tvTitleMatchSetting);

        cardRecentMatch        = findViewById(R.id.cardRecentMatch);
        btnRecentShare         = findViewById(R.id.btnRecentShare);
        tvRecentTeams          = findViewById(R.id.tvRecentTeams);
        tvRecentScore          = findViewById(R.id.tvRecentScore);
        tvRecentResult         = findViewById(R.id.tvRecentResult);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshHome == null) return;
        swipeRefreshHome.setColorSchemeColors(
                Color.parseColor("#2563EB"),
                Color.parseColor("#10B981"),
                Color.parseColor("#F59E0B")
        );
        swipeRefreshHome.setOnRefreshListener(() -> {
            // Refresh scores and recent match data
            loadRecentMatch();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (swipeRefreshHome != null) {
                    swipeRefreshHome.setRefreshing(false);
                }
                Toast.makeText(this, "⚡ Match scores and live updates refreshed", Toast.LENGTH_SHORT).show();
            }, 800);
        });
    }

    private void setupListeners() {
        // 1. Start New Match
        if (btnStartNewMatch != null) {
            btnStartNewMatch.setOnClickListener(v ->
                startActivity(new Intent(this, MatchSettingsActivity.class)));
        }

        // 2. Tournament
        if (btnTournament != null) {
            btnTournament.setOnClickListener(v ->
                startActivity(new Intent(this, TournamentMenuActivity.class)));
        }

        // 3. Teams
        if (btnTeams != null) {
            btnTeams.setOnClickListener(v ->
                startActivity(new Intent(this, TeamManagerActivity.class)));
        }

        // 4. Match History
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, MatchHistoryActivity.class)));
        }

        // 5. Match Setting (Opens App & Match Settings)
        if (btnMatchSetting != null) {
            btnMatchSetting.setOnClickListener(v ->
                startActivity(new Intent(this, AppSettingsActivity.class)));
        }

        // Top Utility Buttons
        if (btnTheme != null) {
            btnTheme.setOnClickListener(v -> showThemeDialog());
        }

        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> showAboutDialog());
        }

        // Top Share Button
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> handleShareAction());
        }

        // Quick Share in Recent Match Card
        if (btnRecentShare != null) {
            btnRecentShare.setOnClickListener(v -> handleShareAction());
        }

        // Recent Match Tap
        if (cardRecentMatch != null) {
            cardRecentMatch.setOnClickListener(v -> {
                if (latestMatchData != null) {
                    Intent intent = new Intent(this, ScorecardActivity.class);
                    intent.putExtra("MATCH_DATA", latestMatchData);
                    intent.putExtra("IS_SUMMARY", true);
                    startActivity(intent);
                } else {
                    startActivity(new Intent(this, MatchSettingsActivity.class));
                }
            });
        }
    }

    private void handleShareAction() {
        if (latestMatchData != null) {
            MatchShareManager.showShareDialog(this, latestMatchData);
            return;
        }

        // Try getting latest from match history or active live match
        ArrayList<MatchData> matches = DataManager.getAllMatches(this);
        if (matches != null && !matches.isEmpty()) {
            latestMatchData = matches.get(0);
            MatchShareManager.showShareDialog(this, latestMatchData);
            return;
        }

        // Check if there is an active auto-saved match in Room
        com.cricketscorez.proapp.room.LiveMatchProgressRepository.getLatestActiveMatch(this, new com.cricketscorez.proapp.room.LiveMatchProgressRepository.OnMatchLoadedCallback() {
            @Override
            public void onLoaded(MatchData matchData) {
                latestMatchData = matchData;
                MatchShareManager.showShareDialog(HomeActivity.this, matchData);
            }

            @Override
            public void onNotFound() {
                Toast.makeText(HomeActivity.this, "No match results available to share. Play or start a match first!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DYNAMIC LIVE THEME APPLICATION
    // ─────────────────────────────────────────────────────────────────────────
    public void applyThemeToUI() {
        ThemeManager.applyStatusBar(this);

        int primaryText   = ThemeManager.getPrimaryTextColor(this);
        int secondaryText = ThemeManager.getSecondaryTextColor(this);
        int accent        = ThemeManager.getAccentColor(this);
        int titleColor    = ThemeManager.getTitleDisplayColor(this);
        boolean isDark    = ThemeManager.isDarkVariant(this);

        // Root Background
        if (rootScrollView != null) {
            rootScrollView.setBackground(ThemeManager.getCanvasBackground(this));
        }

        // Title styling
        if (tvAppTitle != null) {
            tvAppTitle.setTextColor(titleColor);
        }
        if (tvGreeting != null) {
            tvGreeting.setTextColor(secondaryText);
        }

        // Theme Icon
        if (tvThemeIcon != null) {
            tvThemeIcon.setText(isDark ? "🌙" : "☀️");
        }

        // Utility Buttons
        if (btnTheme != null) btnTheme.setBackground(ThemeManager.getPillMenuBackground(this));
        if (btnAbout != null) btnAbout.setBackground(ThemeManager.getPillMenuBackground(this));
        if (btnShare != null) btnShare.setBackground(ThemeManager.getPillMenuBackground(this));
        if (btnRecentShare != null) btnRecentShare.setBackground(ThemeManager.getPillMenuBackground(this));

        // Menu Pill Buttons Background & Text
        if (btnStartNewMatch != null)   btnStartNewMatch.setBackground(ThemeManager.getPillMenuBackground(this));
        if (btnTournament != null)      btnTournament.setBackground(ThemeManager.getPillMenuBackground(this));
        if (btnTeams != null)           btnTeams.setBackground(ThemeManager.getPillMenuBackground(this));
        if (btnHistory != null)         btnHistory.setBackground(ThemeManager.getPillMenuBackground(this));
        if (btnMatchSetting != null)    btnMatchSetting.setBackground(ThemeManager.getPillMenuBackground(this));

        if (tvTitleStartNewMatch != null)   tvTitleStartNewMatch.setTextColor(primaryText);
        if (tvTitleTournament != null)      tvTitleTournament.setTextColor(primaryText);
        if (tvTitleTeams != null)           tvTitleTeams.setTextColor(primaryText);
        if (tvTitleHistory != null)         tvTitleHistory.setTextColor(primaryText);
        if (tvTitleMatchSetting != null)    tvTitleMatchSetting.setTextColor(primaryText);

        // Recent Match
        if (cardRecentMatch != null)  cardRecentMatch.setBackground(ThemeManager.getPillMenuBackground(this));
        if (tvRecentTeams != null)    tvRecentTeams.setTextColor(primaryText);
        if (tvRecentScore != null)    tvRecentScore.setTextColor(secondaryText);
        if (tvRecentResult != null)   tvRecentResult.setTextColor(accent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        applyThemeToUI();
        loadRecentMatch();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void loadRecentMatch() {
        if (cardRecentMatch == null) return;
        try {
            ArrayList<MatchData> matches = DataManager.getAllMatches(this);
            if (matches != null && !matches.isEmpty()) {
                latestMatchData = matches.get(0);
                cardRecentMatch.setVisibility(View.VISIBLE);

                if (tvRecentTeams != null) {
                    String t1 = latestMatchData.team1Name != null ? latestMatchData.team1Name : "Team 1";
                    String t2 = latestMatchData.team2Name != null ? latestMatchData.team2Name : "Team 2";
                    tvRecentTeams.setText(t1 + " vs " + t2);
                }

                if (tvRecentScore != null) {
                    String s1 = (latestMatchData.scoreInn1 != null ? latestMatchData.scoreInn1 : "0/0")
                            + " (" + (latestMatchData.oversInn1 != null ? latestMatchData.oversInn1 : "0") + " ov)";
                    String s2 = latestMatchData.isSecondInnings
                            ? " • " + latestMatchData.getScoreString() + " (" + latestMatchData.getOversString() + " ov)"
                            : "";
                    tvRecentScore.setText(s1 + s2);
                }

                if (tvRecentResult != null) {
                    if (latestMatchData.matchStatus != null && !latestMatchData.matchStatus.isEmpty()) {
                        tvRecentResult.setText(latestMatchData.matchStatus);
                    } else {
                        tvRecentResult.setText("View ›");
                    }
                }
            } else {
                latestMatchData = null;
                cardRecentMatch.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DYNAMIC THEME SELECTOR DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    private void showThemeDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.65f);
        }

        boolean isDark = ThemeManager.isDarkVariant(this);
        int dialogBgColor = isDark ? Color.parseColor("#111620") : Color.parseColor("#FFFFFF");
        int dialogBorderColor = isDark ? Color.parseColor("#222C3D") : Color.parseColor("#E2E8F0");
        int headerBgColor = isDark ? Color.parseColor("#17202E") : Color.parseColor("#F8FAFC");
        int textColorPrimary = isDark ? Color.parseColor("#F8FAFC") : Color.parseColor("#0F172A");
        int textColorSecondary = isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");

        // Root
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(dialogBgColor);
        rootBg.setCornerRadius(dp(24));
        rootBg.setStroke(dp(1), dialogBorderColor);
        root.setBackground(rootBg);
        root.setClipToOutline(true);

        // ── Header bar ───────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(20), dp(20), dp(20), dp(16));
        GradientDrawable hdrBg = new GradientDrawable();
        hdrBg.setColor(headerBgColor);
        hdrBg.setCornerRadii(new float[]{dp(24), dp(24), dp(24), dp(24), 0, 0, 0, 0});
        header.setBackground(hdrBg);

        TextView tvHdrIcon = new TextView(this);
        tvHdrIcon.setText("🎨");
        tvHdrIcon.setTextSize(26);
        tvHdrIcon.setGravity(Gravity.CENTER);
        header.addView(tvHdrIcon);

        TextView tvHdrTitle = new TextView(this);
        tvHdrTitle.setText("App Theme");
        tvHdrTitle.setTextSize(18);
        tvHdrTitle.setTypeface(null, Typeface.BOLD);
        tvHdrTitle.setTextColor(textColorPrimary);
        tvHdrTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams htLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        htLp.setMargins(0, dp(4), 0, dp(2));
        tvHdrTitle.setLayoutParams(htLp);
        header.addView(tvHdrTitle);

        TextView tvHdrSub = new TextView(this);
        tvHdrSub.setText("Select your visual preference");
        tvHdrSub.setTextSize(12);
        tvHdrSub.setTextColor(textColorSecondary);
        tvHdrSub.setGravity(Gravity.CENTER);
        header.addView(tvHdrSub);

        root.addView(header);

        // ── Theme options list ─────────────────────────────────────────
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(16), dp(16), dp(12));

        String currentKey = ThemeManager.getSavedTheme(this);

        String[][] themeList = {
            {ThemeManager.THEME_LIGHT,   "☀️", "Clean Light (Default)", "Crisp white & pastel sky"},
            {ThemeManager.THEME_DARK,    "🌙", "Executive Dark",        "Slate & matte charcoal"},
            {ThemeManager.THEME_EMERALD, "🌲", "Midnight Emerald",      "Deep stadium forest green"},
            {ThemeManager.THEME_NAVY,    "🌊", "Royal Navy",            "Executive deep ocean blue"},
            {ThemeManager.THEME_SYSTEM,  "📱", "System Default",        "Clean Light / OS Mode"}
        };

        for (String[] th : themeList) {
            String key = th[0], icon = th[1], title = th[2], sub = th[3];
            boolean isSelected = currentKey.equals(key);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.setClickable(true);
            row.setFocusable(true);

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setCornerRadius(dp(14));
            if (isSelected) {
                rowBg.setColor(isDark ? Color.parseColor("#1B2738") : Color.parseColor("#EFF6FF"));
                rowBg.setStroke(dp(2), Color.parseColor("#3B82F6"));
            } else {
                rowBg.setColor(isDark ? Color.parseColor("#161D2A") : Color.parseColor("#F8FAFC"));
                rowBg.setStroke(dp(1), isDark ? Color.parseColor("#222C3D") : Color.parseColor("#E2E8F0"));
            }
            row.setBackground(rowBg);

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, dp(8));
            row.setLayoutParams(rowLp);

            // Icon bubble
            LinearLayout iconBubble = new LinearLayout(this);
            iconBubble.setGravity(Gravity.CENTER);
            GradientDrawable ibBg = new GradientDrawable();
            ibBg.setShape(GradientDrawable.OVAL);
            ibBg.setColor(isSelected ? (isDark ? Color.parseColor("#2563EB") : Color.parseColor("#DBEAFE"))
                                     : (isDark ? Color.parseColor("#20293A") : Color.parseColor("#E2E8F0")));
            iconBubble.setBackground(ibBg);
            LinearLayout.LayoutParams ibLp = new LinearLayout.LayoutParams(dp(40), dp(40));
            ibLp.setMargins(0, 0, dp(12), 0);
            iconBubble.setLayoutParams(ibLp);
            TextView tvIco = new TextView(this);
            tvIco.setText(icon);
            tvIco.setTextSize(18);
            tvIco.setGravity(Gravity.CENTER);
            iconBubble.addView(tvIco);
            row.addView(iconBubble);

            // Text column
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView tvTitle = new TextView(this);
            tvTitle.setText(title);
            tvTitle.setTextSize(14);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextColor(isSelected ? (isDark ? Color.parseColor("#93C5FD") : Color.parseColor("#1D4ED8")) : textColorPrimary);
            TextView tvSub = new TextView(this);
            tvSub.setText(sub);
            tvSub.setTextSize(11);
            tvSub.setTextColor(textColorSecondary);
            textCol.addView(tvTitle);
            textCol.addView(tvSub);
            row.addView(textCol);

            // Selected checkmark
            if (isSelected) {
                TextView tvCheck = new TextView(this);
                tvCheck.setText("✓");
                tvCheck.setTextSize(16);
                tvCheck.setTypeface(null, Typeface.BOLD);
                tvCheck.setTextColor(isDark ? Color.parseColor("#60A5FA") : Color.parseColor("#2563EB"));
                row.addView(tvCheck);
            }

            row.setOnClickListener(v -> {
                ThemeManager.setTheme(this, key);
                applyThemeToUI();
                dialog.dismiss();
                Toast.makeText(this, icon + " " + title + " applied.", Toast.LENGTH_SHORT).show();
            });

            body.addView(row);
        }

        // Cancel button
        LinearLayout btnCancel = new LinearLayout(this);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setPadding(0, dp(12), 0, dp(12));
        btnCancel.setClickable(true);
        btnCancel.setFocusable(true);
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setColor(isDark ? Color.parseColor("#161D2A") : Color.parseColor("#F1F5F9"));
        cancelBg.setCornerRadius(dp(12));
        cancelBg.setStroke(dp(1), isDark ? Color.parseColor("#222C3D") : Color.parseColor("#CBD5E1"));
        btnCancel.setBackground(cancelBg);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cancelLp.setMargins(0, dp(4), 0, dp(8));
        btnCancel.setLayoutParams(cancelLp);
        TextView tvCancel = new TextView(this);
        tvCancel.setText("Cancel");
        tvCancel.setTextSize(13);
        tvCancel.setTextColor(textColorSecondary);
        tvCancel.setTypeface(null, Typeface.BOLD);
        btnCancel.addView(tvCancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        body.addView(btnCancel);

        root.addView(body);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            int w = (int)(getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ABOUT DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    private void showAboutDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.65f);
        }

        boolean isDark = ThemeManager.isDarkVariant(this);
        int dialogBgColor = isDark ? Color.parseColor("#111620") : Color.parseColor("#FFFFFF");
        int dialogBorderColor = isDark ? Color.parseColor("#222C3D") : Color.parseColor("#E2E8F0");
        int textColorPrimary = isDark ? Color.parseColor("#F8FAFC") : Color.parseColor("#0F172A");
        int textColorSecondary = isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");

        // Root card
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(dialogBgColor);
        rootBg.setCornerRadius(dp(26));
        rootBg.setStroke(dp(1), dialogBorderColor);
        root.setBackground(rootBg);
        root.setClipToOutline(true);

        // Hero banner
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(24), dp(28), dp(24), dp(20));
        GradientDrawable heroBg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            isDark ? new int[]{Color.parseColor("#162235"), Color.parseColor("#101724")}
                   : new int[]{Color.parseColor("#EFF6FF"), Color.parseColor("#DBEAFE")});
        heroBg.setCornerRadii(new float[]{dp(26), dp(26), dp(26), dp(26), 0, 0, 0, 0});
        hero.setBackground(heroBg);

        // Logo ring
        LinearLayout logoRing = new LinearLayout(this);
        logoRing.setGravity(Gravity.CENTER);
        GradientDrawable ringBg = new GradientDrawable();
        ringBg.setShape(GradientDrawable.OVAL);
        ringBg.setColor(isDark ? Color.parseColor("#1E293B") : Color.parseColor("#FFFFFF"));
        ringBg.setStroke(dp(2), isDark ? Color.parseColor("#38BDF8") : Color.parseColor("#3B82F6"));
        logoRing.setBackground(ringBg);
        LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(dp(76), dp(76));
        ringLp.gravity = Gravity.CENTER_HORIZONTAL;
        logoRing.setLayoutParams(ringLp);
        TextView tvBall = new TextView(this);
        tvBall.setText("🏏");
        tvBall.setTextSize(32);
        tvBall.setGravity(Gravity.CENTER);
        logoRing.addView(tvBall);
        hero.addView(logoRing);

        // App name
        TextView tvAppName = new TextView(this);
        tvAppName.setText("CricketScorez Pro");
        tvAppName.setTextSize(20);
        tvAppName.setTypeface(null, Typeface.BOLD);
        tvAppName.setTextColor(textColorPrimary);
        tvAppName.setGravity(Gravity.CENTER);
        tvAppName.setLetterSpacing(0.02f);
        LinearLayout.LayoutParams anLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        anLp.setMargins(0, dp(12), 0, dp(4));
        tvAppName.setLayoutParams(anLp);
        hero.addView(tvAppName);

        // PRO badge
        TextView tvBadge = new TextView(this);
        tvBadge.setText("  ★ PRO EDITION  ");
        tvBadge.setTextSize(10);
        tvBadge.setTypeface(null, Typeface.BOLD);
        tvBadge.setTextColor(Color.parseColor("#10B981"));
        tvBadge.setLetterSpacing(0.12f);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(isDark ? Color.parseColor("#0F291E") : Color.parseColor("#DCFCE7"));
        badgeBg.setStroke(dp(1), Color.parseColor("#10B981"));
        badgeBg.setCornerRadius(dp(20));
        tvBadge.setBackground(badgeBg);
        tvBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams bdgLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bdgLp.setMargins(0, 0, 0, dp(8));
        tvBadge.setLayoutParams(bdgLp);
        hero.addView(tvBadge);

        // Creator info: Created by Md Jahidul Islam, CBD
        TextView tvCreator = new TextView(this);
        tvCreator.setText("Created by Md Jahidul Islam, CBD");
        tvCreator.setTextSize(13);
        tvCreator.setTypeface(null, Typeface.BOLD);
        tvCreator.setTextColor(textColorPrimary);
        tvCreator.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams crLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        crLp.setMargins(0, dp(2), 0, dp(6));
        tvCreator.setLayoutParams(crLp);
        hero.addView(tvCreator);

        root.addView(hero);

        // Close button
        LinearLayout btnClose = new LinearLayout(this);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setPadding(0, dp(14), 0, dp(14));
        btnClose.setClickable(true);
        btnClose.setFocusable(true);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setColor(isDark ? Color.parseColor("#1D4ED8") : Color.parseColor("#2563EB"));
        closeBg.setCornerRadius(dp(14));
        btnClose.setBackground(closeBg);
        LinearLayout.LayoutParams clLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clLp.setMargins(dp(20), dp(16), dp(20), dp(20));
        btnClose.setLayoutParams(clLp);
        TextView tvClose = new TextView(this);
        tvClose.setText("Done");
        tvClose.setTextSize(14);
        tvClose.setTextColor(Color.WHITE);
        tvClose.setTypeface(null, Typeface.BOLD);
        btnClose.addView(tvClose);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnClose);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            int w = (int)(getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXIT CONFIRMATION DIALOG (Are you sure you want to exit?)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void onBackPressed() {
        ExitDialogHelper.show(this);
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
