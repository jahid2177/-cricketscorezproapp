package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.util.Log;
import com.cricketscorez.proapp.api.ApiClient;
import com.cricketscorez.proapp.api.ApiInterface;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class FixturesActivity extends Activity {

    ImageView btnBack;

    // ✅ FIX: btnAutoGenerate — XML এ LinearLayout, তাই এখানেও LinearLayout
    Button btnView, btnSave, btnAddGroup;
    LinearLayout btnAutoGenerate, btnManualFixtures;

    LinearLayout containerGroups, containerGroupMatches, containerQFMatches,
                 containerSFMatches, containerFinalMatch;

    List<String[]> groupList   = new ArrayList<>();
    List<int[]>    groupMatches = new ArrayList<>();
    List<int[]>    qfMatches    = new ArrayList<>();
    List<int[]>    sfMatches    = new ArrayList<>();
    List<int[]>    finalMatch   = new ArrayList<>();

    List<String>   allTeams = new ArrayList<>();
    SharedPreferences prefs;
    String tournamentName = "Tournament";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fixtures);

        btnBack              = findViewById(R.id.btnBack);
        btnView              = findViewById(R.id.btnView);
        btnSave              = findViewById(R.id.btnSave);
        btnAutoGenerate      = findViewById(R.id.btnAutoGenerate);
        btnManualFixtures    = findViewById(R.id.btnManualFixtures);
        btnAddGroup          = findViewById(R.id.btnAddGroup);
        containerGroups      = findViewById(R.id.containerGroups);
        containerGroupMatches= findViewById(R.id.containerGroupMatches);
        containerQFMatches   = findViewById(R.id.containerQFMatches);
        containerSFMatches   = findViewById(R.id.containerSFMatches);
        containerFinalMatch  = findViewById(R.id.containerFinalMatch);

        prefs = getSharedPreferences("TournamentData", MODE_PRIVATE);
        tournamentName = prefs.getString("TOURNAMENT_NAME", "Tournament");

        btnBack.setOnClickListener(v -> finish());
        btnView.setOnClickListener(v -> startActivity(new Intent(this, ViewFixturesActivity.class)));
        btnSave.setOnClickListener(v -> saveFixtures());
        btnAddGroup.setOnClickListener(v -> showAddGroupDialog());
        btnAutoGenerate.setOnClickListener(v -> showAIGenerateDialog());
        btnManualFixtures.setOnClickListener(v -> showManualFixturesDialog());

        loadExistingData();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🤖 AI AUTO-GENERATE DIALOG
    // ═══════════════════════════════════════════════════════════════════════
    private void showAIGenerateDialog() {
        List<String> teams = collectAllTeams();
        if (teams.size() < 2) {
            Toast.makeText(this, "Please add at least 2 teams in groups first!", Toast.LENGTH_LONG).show();
            return;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, 0);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.WHITE);
        rootBg.setCornerRadius(dp(20));
        root.setBackground(rootBg);

        // ── Header ──────────────────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(20), dp(24), dp(20), dp(20));
        GradientDrawable hBg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#388E3C")});
        hBg.setCornerRadii(new float[]{dp(20), dp(20), dp(20), dp(20), 0, 0, 0, 0});
        header.setBackground(hBg);

        TextView tvAI = new TextView(this);
        tvAI.setText("🤖"); tvAI.setTextSize(34); tvAI.setGravity(Gravity.CENTER);
        header.addView(tvAI);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("AI Fixture Generator");
        tvTitle.setTextSize(18); tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.WHITE); tvTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.setMargins(0, dp(8), 0, 0);
        tvTitle.setLayoutParams(titleLp);
        header.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(teams.size() + " teams detected • Smart balancing enabled");
        tvSub.setTextSize(11); tvSub.setTextColor(Color.parseColor("#A5D6A7")); tvSub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0, dp(4), 0, 0);
        tvSub.setLayoutParams(subLp);
        header.addView(tvSub);
        root.addView(header, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ── Body ─────────────────────────────────────────────────────────
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(16), dp(20), dp(8));

        // AI Recommendation box
        String recommended = getAIRecommendedFormat(teams.size());
        LinearLayout recBox = new LinearLayout(this);
        recBox.setOrientation(LinearLayout.HORIZONTAL);
        recBox.setGravity(Gravity.CENTER_VERTICAL);
        recBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable recBg = new GradientDrawable();
        recBg.setColor(Color.parseColor("#E8F5E9"));
        recBg.setStroke(dp(1), Color.parseColor("#A5D6A7"));
        recBg.setCornerRadius(dp(10));
        recBox.setBackground(recBg);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, 0, 0, dp(16));
        recBox.setLayoutParams(rlp);

        TextView tvRecIcon = new TextView(this);
        tvRecIcon.setText("✨"); tvRecIcon.setTextSize(16);
        recBox.addView(tvRecIcon);

        TextView tvRec = new TextView(this);
        tvRec.setText("  AI recommends: " + recommended);
        tvRec.setTextSize(13); tvRec.setTypeface(null, Typeface.BOLD);
        tvRec.setTextColor(Color.parseColor("#1B5E20"));
        recBox.addView(tvRec);
        body.addView(recBox);

        // Format options
        addSectionLabel(body, "Tournament Format");
        String[] formats = {
            "Round Robin (All play all)",
            "Knockout (Direct elimination)",
            "Group Stage + Knockouts",
            "Super League (2-phase RR)"
        };
        final int[] selectedFormat = {getDefaultFormatIndex(teams.size())};
        LinearLayout[] formatBtns = new LinearLayout[formats.length];
        for (int i = 0; i < formats.length; i++) {
            final int idx = i;
            LinearLayout btn = makeFormatOption(formats[i], getFormatDesc(i, teams.size()), i == selectedFormat[0]);
            formatBtns[i] = btn;
            btn.setOnClickListener(v -> {
                selectedFormat[0] = idx;
                for (int j = 0; j < formatBtns.length; j++) setFormatSelected(formatBtns[j], j == idx);
            });
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            blp.setMargins(0, 0, 0, dp(8));
            btn.setLayoutParams(blp);
            body.addView(btn);
        }

        // Match Legs
        addSectionLabel(body, "Match Legs");
        String[] legs = {"Single Leg", "Home & Away (Double)"};
        final int[] selectedLeg = {0};
        LinearLayout[] legBtns = new LinearLayout[legs.length];
        LinearLayout legRow = new LinearLayout(this);
        legRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams legRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        legRowLp.setMargins(0, 0, 0, dp(16));
        legRow.setLayoutParams(legRowLp);
        for (int i = 0; i < legs.length; i++) {
            final int idx = i;
            LinearLayout lb = makeCompactOption(legs[i], i == 0);
            legBtns[i] = lb;
            lb.setOnClickListener(v -> {
                selectedLeg[0] = idx;
                for (int j = 0; j < legBtns.length; j++) setCompactSelected(legBtns[j], j == idx);
            });
            LinearLayout.LayoutParams llp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) llp2.setMargins(dp(8), 0, 0, 0);
            lb.setLayoutParams(llp2);
            legRow.addView(lb);
        }
        body.addView(legRow);

        // ── NEW: Seeding Priority ─────────────────────────────────────────
        addSectionLabel(body, "Seeding Priority");
        String[] seedOpts = {"Random Draw", "Ranking Based", "Manual Seeding"};
        String[] seedDescs = {"Fully random match-ups", "Higher ranked teams seeded top", "Define seeds manually"};
        final int[] selectedSeed = {0};
        LinearLayout[] seedBtns = new LinearLayout[seedOpts.length];
        for (int i = 0; i < seedOpts.length; i++) {
            final int idx = i;
            LinearLayout sb = makeCompactOptionWithDesc(seedOpts[i], seedDescs[i], i == 0);
            seedBtns[i] = sb;
            sb.setOnClickListener(v -> {
                selectedSeed[0] = idx;
                for (int j = 0; j < seedBtns.length; j++) setCompactDescSelected(seedBtns[j], j == idx);
            });
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.setMargins(0, 0, 0, dp(6));
            sb.setLayoutParams(slp);
            body.addView(sb);
        }
        LinearLayout.LayoutParams seedBottomLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        seedBottomLp.setMargins(0, 0, 0, dp(10));
        seedBtns[seedOpts.length - 1].setLayoutParams(seedBottomLp);

        // ── NEW: Venue Type ───────────────────────────────────────────────
        addSectionLabel(body, "Venue Type");
        String[] venueOpts = {"Single Venue", "Home & Away Grounds", "Neutral Venue"};
        final int[] selectedVenue = {0};
        LinearLayout[] venueBtns = new LinearLayout[venueOpts.length];
        LinearLayout venueRow = new LinearLayout(this);
        venueRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams venueRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        venueRowLp.setMargins(0, 0, 0, dp(16));
        venueRow.setLayoutParams(venueRowLp);
        for (int i = 0; i < venueOpts.length; i++) {
            final int idx = i;
            LinearLayout vb = makeCompactOption(venueOpts[i], i == 0);
            venueBtns[i] = vb;
            vb.setOnClickListener(v -> {
                selectedVenue[0] = idx;
                for (int j = 0; j < venueBtns.length; j++) setCompactSelected(venueBtns[j], j == idx);
            });
            LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) vlp.setMargins(dp(6), 0, 0, 0);
            vb.setLayoutParams(vlp);
            venueRow.addView(vb);
        }
        body.addView(venueRow);

        // ── NEW: Toss Preference ──────────────────────────────────────────
        addSectionLabel(body, "Toss Preference");
        String[] tossOpts = {"Auto / Random", "Bat First Preference", "Bowl First Preference"};
        final int[] selectedToss = {0};
        LinearLayout[] tossBtns = new LinearLayout[tossOpts.length];
        LinearLayout tossRow = new LinearLayout(this);
        tossRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tossRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tossRowLp.setMargins(0, 0, 0, dp(16));
        tossRow.setLayoutParams(tossRowLp);
        for (int i = 0; i < tossOpts.length; i++) {
            final int idx = i;
            LinearLayout tb = makeCompactOption(tossOpts[i], i == 0);
            tossBtns[i] = tb;
            tb.setOnClickListener(v -> {
                selectedToss[0] = idx;
                for (int j = 0; j < tossBtns.length; j++) setCompactSelected(tossBtns[j], j == idx);
            });
            LinearLayout.LayoutParams tlpInner = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) tlpInner.setMargins(dp(6), 0, 0, 0);
            tb.setLayoutParams(tlpInner);
            tossRow.addView(tb);
        }
        body.addView(tossRow);

        // ── NEW: Match Day Interval ───────────────────────────────────────
        addSectionLabel(body, "Rest Days Between Matches");
        final int[] selectedInterval = {1};
        String[] intervalOpts = {"Same Day", "1 Day Rest", "2 Days Rest", "3+ Days Rest"};
        LinearLayout[] intervalBtns = new LinearLayout[intervalOpts.length];
        LinearLayout intervalRow = new LinearLayout(this);
        intervalRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams intervalRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        intervalRowLp.setMargins(0, 0, 0, dp(16));
        intervalRow.setLayoutParams(intervalRowLp);
        for (int i = 0; i < intervalOpts.length; i++) {
            final int idx = i;
            LinearLayout ib = makeCompactOption(intervalOpts[i], i == 1);
            intervalBtns[i] = ib;
            ib.setOnClickListener(v -> {
                selectedInterval[0] = idx;
                for (int j = 0; j < intervalBtns.length; j++) setCompactSelected(intervalBtns[j], j == idx);
            });
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) ilp.setMargins(dp(4), 0, 0, 0);
            ib.setLayoutParams(ilp);
            intervalRow.addView(ib);
        }
        body.addView(intervalRow);

        // ── NEW: Extra Options (Checkboxes) ───────────────────────────────
        addSectionLabel(body, "Extra Options");
        LinearLayout extrasBox = new LinearLayout(this);
        extrasBox.setOrientation(LinearLayout.VERTICAL);
        extrasBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable extrasBg = new GradientDrawable();
        extrasBg.setColor(Color.parseColor("#F8FAFC"));
        extrasBg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        extrasBg.setCornerRadius(dp(10));
        extrasBox.setBackground(extrasBg);
        LinearLayout.LayoutParams extrasLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        extrasLp.setMargins(0, 0, 0, dp(8));
        extrasBox.setLayoutParams(extrasLp);

        CheckBox cbBalanced = makeStyledCheckbox("⚖️  Balanced group distribution", true);
        CheckBox cbAvoidRepeat = makeStyledCheckbox("🔄  Avoid back-to-back matches for same team", true);
        CheckBox cbWeekend = makeStyledCheckbox("📅  Prefer weekend matches", false);
        CheckBox cbDayNight = makeStyledCheckbox("🌙  Day/Night match split", false);
        extrasBox.addView(cbBalanced);
        extrasBox.addView(cbAvoidRepeat);
        extrasBox.addView(cbWeekend);
        extrasBox.addView(cbDayNight);
        body.addView(extrasBox);

        root.addView(body);

        // ── Stats row ────────────────────────────────────────────────────
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setGravity(Gravity.CENTER);
        statsRow.setPadding(dp(20), dp(8), dp(20), dp(8));
        GradientDrawable statsBg = new GradientDrawable();
        statsBg.setColor(Color.parseColor("#F8FAFC"));
        statsRow.setBackground(statsBg);
        statsRow.addView(makeStatChip("Teams",        String.valueOf(teams.size()),             "#1B5E20"));
        statsRow.addView(makeStatChip("Groups",       String.valueOf(Math.max(1, teams.size()/4)), "#1565C0"));
        statsRow.addView(makeStatChip("Est. Matches", estimateMatches(teams.size()),            "#E65100"));
        root.addView(statsRow, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ── Action buttons ───────────────────────────────────────────────
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams brLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brLp.setMargins(dp(16), dp(12), dp(16), dp(16));
        btnRow.setLayoutParams(brLp);

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel"); btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setAllCaps(false); btnCancel.setTypeface(null, Typeface.BOLD);
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setColor(Color.parseColor("#F1F5F9")); cancelBg.setCornerRadius(dp(12));
        btnCancel.setBackground(cancelBg);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(0, dp(50), 1f));
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        Button btnGenerate = new Button(this);
        btnGenerate.setText("🤖  Generate Now"); btnGenerate.setTextColor(Color.WHITE);
        btnGenerate.setAllCaps(false); btnGenerate.setTypeface(null, Typeface.BOLD);
        GradientDrawable genBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#388E3C")});
        genBg.setCornerRadius(dp(12)); btnGenerate.setBackground(genBg);
        LinearLayout.LayoutParams genLp = new LinearLayout.LayoutParams(0, dp(50), 1.5f);
        genLp.setMargins(dp(8), 0, 0, 0);
        btnGenerate.setLayoutParams(genLp);
        btnGenerate.setOnClickListener(v -> {
            dialog.dismiss();
            runAIGenerate(teams, selectedFormat[0], selectedLeg[0]);
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnGenerate);
        root.addView(btnRow);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        dialog.setContentView(sv);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            int w = (int)(getResources().getDisplayMetrics().widthPixels * 0.92f);
            dialog.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 🤖 AI GENERATE ENGINE
    // ═══════════════════════════════════════════════════════════════════════
    private void runAIGenerate(List<String> teams, int format, int legs) {
        try {
            SharedPreferences prefs = getSharedPreferences("TournamentData", MODE_PRIVATE);
            JSONObject mainObj = new JSONObject(prefs.getString("ALL_DATA", "{}"));

            allTeams.clear();
            allTeams.add("Select Team");
            allTeams.addAll(teams);

            JSONArray newGroupMatches = new JSONArray();
            JSONArray newQFMatches    = new JSONArray();
            JSONArray newSFMatches    = new JSONArray();
            JSONArray newFinalMatch   = new JSONArray();

            int teamCount = teams.size();

            if (format == 0) {
                // Round Robin
                newGroupMatches = generateRoundRobin(teams, legs);

            } else if (format == 1) {
                // Knockout
                Collections.shuffle(teams);
                if (teamCount >= 4) {
                    for (int i = 0; i < Math.min(teamCount, 8) - 1; i += 2) {
                        JSONObject m = new JSONObject();
                        m.put("team1_idx", allTeams.indexOf(teams.get(i)));
                        m.put("team2_idx", allTeams.indexOf(teams.get(i + 1)));
                        m.put("date", "TBD");
                        newQFMatches.put(m);
                    }
                    for (int i = 0; i < 2; i++) {
                        JSONObject m = new JSONObject();
                        m.put("team1_idx", 0); m.put("team2_idx", 0); m.put("date", "TBD");
                        newSFMatches.put(m);
                    }
                    JSONObject fin = new JSONObject();
                    fin.put("team1_idx", 0); fin.put("team2_idx", 0); fin.put("date", "TBD");
                    newFinalMatch.put(fin);
                }

            } else if (format == 2) {
                // Group Stage + Knockouts
                newGroupMatches = generateSmartGroupFixtures(teams, legs);
                int numGroups  = Math.max(1, teamCount / 4);
                int qualifiers = numGroups * 2;
                if (qualifiers >= 4) {
                    for (int i = 0; i < qualifiers / 2; i++) {
                        JSONObject m = new JSONObject();
                        m.put("team1_idx", 0); m.put("team2_idx", 0); m.put("date", "TBD");
                        newQFMatches.put(m);
                    }
                    for (int i = 0; i < Math.max(1, qualifiers / 4); i++) {
                        JSONObject m = new JSONObject();
                        m.put("team1_idx", 0); m.put("team2_idx", 0); m.put("date", "TBD");
                        newSFMatches.put(m);
                    }
                    JSONObject fin = new JSONObject();
                    fin.put("team1_idx", 0); fin.put("team2_idx", 0); fin.put("date", "TBD");
                    newFinalMatch.put(fin);
                }

            } else {
                // Super League
                newGroupMatches = generateRoundRobin(teams, 1);
                for (int i = 0; i < 2; i++) {
                    JSONObject m = new JSONObject();
                    m.put("team1_idx", 0); m.put("team2_idx", 0); m.put("date", "TBD");
                    newSFMatches.put(m);
                }
                JSONObject fin = new JSONObject();
                fin.put("team1_idx", 0); fin.put("team2_idx", 0); fin.put("date", "TBD");
                newFinalMatch.put(fin);
            }

            mainObj.put("MatchesGroup", newGroupMatches);
            mainObj.put("MatchesQF",    newQFMatches);
            mainObj.put("MatchesSF",    newSFMatches);
            mainObj.put("MatchesFinal", newFinalMatch);

            if (!mainObj.has("DynamicGroups")) {
                JSONArray grp = new JSONArray();
                JSONObject g  = new JSONObject();
                JSONArray tArr = new JSONArray();
                for (String t : teams) tArr.put(t);
                g.put("name", "Group A"); g.put("teams", tArr);
                grp.put(g);
                mainObj.put("DynamicGroups", grp);
            }

            prefs.edit().putString("ALL_DATA", mainObj.toString()).apply();

            int total = newGroupMatches.length() + newQFMatches.length()
                      + newSFMatches.length()    + newFinalMatch.length();
            showAISuccessDialog(total, teams.size(), format);
            loadExistingData();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── Round Robin generator ─────────────────────────────────────────────
    private JSONArray generateRoundRobin(List<String> teams, int legs) throws Exception {
        JSONArray matches = new JSONArray();
        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                JSONObject m = new JSONObject();
                m.put("team1_idx", allTeams.indexOf(teams.get(i)));
                m.put("team2_idx", allTeams.indexOf(teams.get(j)));
                m.put("date", "TBD");
                matches.put(m);
                if (legs == 1) { // Home & Away
                    JSONObject m2 = new JSONObject();
                    m2.put("team1_idx", allTeams.indexOf(teams.get(j)));
                    m2.put("team2_idx", allTeams.indexOf(teams.get(i)));
                    m2.put("date", "TBD");
                    matches.put(m2);
                }
            }
        }
        return matches;
    }

    // ── Smart Group Fixtures ──────────────────────────────────────────────
    private JSONArray generateSmartGroupFixtures(List<String> teams, int legs) throws Exception {
        int numGroups = Math.max(1, teams.size() / 4);
        List<List<String>> groups = new ArrayList<>();
        for (int g = 0; g < numGroups; g++) groups.add(new ArrayList<>());

        try {
            SharedPreferences p = getSharedPreferences("TournamentData", MODE_PRIVATE);
            JSONObject mainObj  = new JSONObject(p.getString("ALL_DATA", "{}"));
            JSONArray dynGroups = new JSONArray();
            for (int g = 0; g < numGroups; g++) {
                JSONObject grpObj = new JSONObject();
                grpObj.put("name", "Group " + (char)('A' + g));
                dynGroups.put(grpObj);
            }
            for (int i = 0; i < teams.size(); i++) {
                int grpIdx = (i / numGroups % 2 == 0) ? (i % numGroups)
                                                       : (numGroups - 1 - i % numGroups);
                groups.get(grpIdx).add(teams.get(i));
            }
            for (int g = 0; g < numGroups; g++) {
                JSONArray tArr = new JSONArray();
                for (String t : groups.get(g)) tArr.put(t);
                dynGroups.getJSONObject(g).put("teams", tArr);
            }
            mainObj.put("DynamicGroups", dynGroups);
            p.edit().putString("ALL_DATA", mainObj.toString()).apply();
        } catch (Exception ignored) {}

        JSONArray matches = new JSONArray();
        for (List<String> grp : groups) {
            for (int i = 0; i < grp.size(); i++) {
                for (int j = i + 1; j < grp.size(); j++) {
                    JSONObject m = new JSONObject();
                    m.put("team1_idx", allTeams.indexOf(grp.get(i)));
                    m.put("team2_idx", allTeams.indexOf(grp.get(j)));
                    m.put("date", "TBD");
                    matches.put(m);
                    if (legs == 1) {
                        JSONObject m2 = new JSONObject();
                        m2.put("team1_idx", allTeams.indexOf(grp.get(j)));
                        m2.put("team2_idx", allTeams.indexOf(grp.get(i)));
                        m2.put("date", "TBD");
                        matches.put(m2);
                    }
                }
            }
        }
        return matches;
    }

    // ── AI helpers ───────────────────────────────────────────────────────
    private String getAIRecommendedFormat(int n) {
        if (n <= 4)  return "Knockout";
        if (n <= 12) return "Group Stage + Knockouts";
        return "Super League";
    }

    private int getDefaultFormatIndex(int n) {
        if (n <= 4)  return 1;
        if (n <= 12) return 2;
        return 3;
    }

    private String getFormatDesc(int idx, int n) {
        switch (idx) {
            case 0: return "Every team plays each other · " + (n * (n - 1) / 2) + " total matches";
            case 1: return "Win or go home · Fast tournament · " + (n - 1) + " matches";
            case 2: return "Groups → QF → SF → Final · Best balance";
            case 3: return "2-phase league + playoffs · High engagement";
            default: return "";
        }
    }

    private String estimateMatches(int n) {
        int groups = Math.max(1, n / 4);
        int groupM = 0;
        for (int i = 0; i < groups; i++) {
            int sz = n / groups + (i < n % groups ? 1 : 0);
            groupM += sz * (sz - 1) / 2;
        }
        return String.valueOf(groupM + groups + 3);
    }

    private void showAISuccessDialog(int total, int teams, int format) {
        String[] fmtNames = {"Round Robin", "Knockout", "Group + Knockouts", "Super League"};
        Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(28), dp(24), dp(24));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(20));
        root.setBackground(bg);

        TextView icon = new TextView(this);
        icon.setText("✅"); icon.setTextSize(44); icon.setGravity(Gravity.CENTER);
        root.addView(icon);

        TextView title = new TextView(this);
        title.setText("Fixtures Generated!");
        title.setTextSize(20); title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1B5E20")); title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.setMargins(0, dp(8), 0, dp(4));
        title.setLayoutParams(tlp);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText(teams + " teams · " + total + " matches · "
            + (format < fmtNames.length ? fmtNames[format] : "Custom"));
        sub.setTextSize(13); sub.setTextColor(Color.parseColor("#64748B")); sub.setGravity(Gravity.CENTER);
        root.addView(sub);

        Button ok = new Button(this);
        ok.setText("View Fixtures"); ok.setTextColor(Color.WHITE);
        ok.setAllCaps(false); ok.setTypeface(null, Typeface.BOLD);
        GradientDrawable okBg = new GradientDrawable();
        okBg.setColor(Color.parseColor("#1B5E20")); okBg.setCornerRadius(dp(12));
        ok.setBackground(okBg);
        LinearLayout.LayoutParams oklp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        oklp.setMargins(0, dp(20), 0, 0);
        ok.setLayoutParams(oklp);
        ok.setOnClickListener(v -> {
            d.dismiss();
            startActivity(new Intent(this, ViewFixturesActivity.class));
        });
        root.addView(ok);

        d.setContentView(root);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.82f),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        d.show();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ADD GROUP DIALOG
    // ═══════════════════════════════════════════════════════════════════════
    private void showAddGroupDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(18));
        root.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("➕  Create New Group");
        title.setTextSize(17); title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, 0, 0, dp(16));
        root.addView(title);

        EditText etName  = makeEditText("Group Name (e.g., Group A)");
        EditText etTeams = makeEditText("Team names (comma separated)");
        etTeams.setMinLines(3); etTeams.setMaxLines(5);
        root.addView(etName);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.setMargins(0, dp(10), 0, 0);
        etTeams.setLayoutParams(tlp);
        root.addView(etTeams);

        Button btnOk = new Button(this);
        btnOk.setText("Create Group"); btnOk.setTextColor(Color.WHITE);
        btnOk.setAllCaps(false); btnOk.setTypeface(null, Typeface.BOLD);
        GradientDrawable okBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#388E3C")});
        okBg.setCornerRadius(dp(12)); btnOk.setBackground(okBg);
        LinearLayout.LayoutParams oklp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        oklp.setMargins(0, dp(16), 0, 0);
        btnOk.setLayoutParams(oklp);
        btnOk.setOnClickListener(v -> {
            String name     = etName.getText().toString().trim();
            String teamsStr = etTeams.getText().toString().trim();
            if (name.isEmpty() || teamsStr.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            addGroupToData(name, teamsStr);
            dialog.dismiss();
            loadExistingData();
        });
        root.addView(btnOk);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.88f),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void addGroupToData(String groupName, String teamsStr) {
        try {
            SharedPreferences p = getSharedPreferences("TournamentData", MODE_PRIVATE);
            JSONObject mainObj  = new JSONObject(p.getString("ALL_DATA", "{}"));
            JSONArray dynGroups = mainObj.optJSONArray("DynamicGroups");
            if (dynGroups == null) dynGroups = new JSONArray();

            JSONObject grp = new JSONObject();
            grp.put("name", groupName);
            JSONArray tArr = new JSONArray();
            for (String t : teamsStr.split(",")) {
                String tn = t.trim();
                if (!tn.isEmpty()) tArr.put(tn);
            }
            grp.put("teams", tArr);
            dynGroups.put(grp);
            mainObj.put("DynamicGroups", dynGroups);
            p.edit().putString("ALL_DATA", mainObj.toString()).apply();
            Toast.makeText(this, "✅ " + groupName + " created!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 📝 MANUAL FIXTURES DIALOG
    // ═══════════════════════════════════════════════════════════════════════
    private void showManualFixturesDialog() {
        List<String> teams = collectAllTeams();
        if (teams.size() < 2) {
            Toast.makeText(this, "Please add at least 2 teams in groups first!", Toast.LENGTH_LONG).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.WHITE); rootBg.setCornerRadius(dp(20));
        root.setBackground(rootBg);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(20), dp(22), dp(20), dp(18));
        GradientDrawable hBg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#388E3C")});
        hBg.setCornerRadii(new float[]{dp(20),dp(20),dp(20),dp(20),0,0,0,0});
        header.setBackground(hBg);

        TextView tvIcon = new TextView(this); tvIcon.setText("📝");
        tvIcon.setTextSize(32); tvIcon.setGravity(Gravity.CENTER);
        header.addView(tvIcon);
        TextView tvTitle = new TextView(this); tvTitle.setText("Manual Fixtures");
        tvTitle.setTextSize(18); tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.WHITE); tvTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.setMargins(0, dp(6), 0, 0); tvTitle.setLayoutParams(tlp);
        header.addView(tvTitle);
        TextView tvSub = new TextView(this); tvSub.setText(teams.size() + " teams available");
        tvSub.setTextSize(11); tvSub.setTextColor(Color.parseColor("#A5D6A7")); tvSub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0, dp(3), 0, 0); tvSub.setLayoutParams(slp);
        header.addView(tvSub);
        root.addView(header);

        // Body — scrollable
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(14), dp(16), dp(8));

        // Build Spinner adapter (team names) — index 0 = "Select Team" placeholder
        final List<String> teamOptions = new ArrayList<>();
        teamOptions.add("— Select Team —");
        teamOptions.addAll(teams);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teamOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Load existing fixture data
        JSONArray existingGroup = new JSONArray();
        JSONArray existingQF    = new JSONArray();
        JSONArray existingSF    = new JSONArray();
        JSONArray existingFinal = new JSONArray();
        try {
            JSONObject mainObj = new JSONObject(prefs.getString("ALL_DATA", "{}"));
            existingGroup = mainObj.optJSONArray("MatchesGroup") != null ? mainObj.optJSONArray("MatchesGroup") : new JSONArray();
            existingQF    = mainObj.optJSONArray("MatchesQF")    != null ? mainObj.optJSONArray("MatchesQF")    : new JSONArray();
            existingSF    = mainObj.optJSONArray("MatchesSF")    != null ? mainObj.optJSONArray("MatchesSF")    : new JSONArray();
            existingFinal = mainObj.optJSONArray("MatchesFinal") != null ? mainObj.optJSONArray("MatchesFinal") : new JSONArray();
        } catch (Exception ignored) {}

        // Holders for spinners per stage
        final List<Spinner[]> groupSpinners = new ArrayList<>();
        final List<Spinner[]> qfSpinners    = new ArrayList<>();
        final List<Spinner[]> sfSpinners    = new ArrayList<>();
        final List<Spinner[]> finalSpinners = new ArrayList<>();

        // ── GROUP STAGE section ──────────────────────────────────────────
        addManualSectionHeader(body, "⚽  GROUP STAGE", "#1B5E20");
        int grpCount = Math.max(existingGroup.length(), 4);
        final int[] grpMatchCount = {grpCount};
        final LinearLayout grpContainer = new LinearLayout(this);
        grpContainer.setOrientation(LinearLayout.VERTICAL);
        body.addView(grpContainer);

        // populate existing group matches
        try {
            for (int i = 0; i < existingGroup.length(); i++) {
                JSONObject m = existingGroup.getJSONObject(i);
                int t1 = m.optInt("team1_idx", 0);
                int t2 = m.optInt("team2_idx", 0);
                Spinner[] row = addManualMatchRow(grpContainer, adapter, "Group Match " + (i + 1), t1, t2);
                groupSpinners.add(row);
            }
        } catch (Exception ignored) {}

        // "Add Match" button for group
        Button btnAddGroup2 = makeAddMatchBtn("＋  Add Group Match");
        btnAddGroup2.setOnClickListener(v -> {
            Spinner[] row = addManualMatchRow(grpContainer, adapter, "Group Match " + (groupSpinners.size() + 1), 0, 0);
            groupSpinners.add(row);
        });
        LinearLayout.LayoutParams addGrpLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        addGrpLp.setMargins(0, dp(4), 0, dp(14)); btnAddGroup2.setLayoutParams(addGrpLp);
        body.addView(btnAddGroup2);

        // ── QUARTER FINALS section ───────────────────────────────────────
        addManualSectionHeader(body, "⚡  QUARTER FINALS", "#1565C0");
        final LinearLayout qfContainer = new LinearLayout(this);
        qfContainer.setOrientation(LinearLayout.VERTICAL);
        body.addView(qfContainer);
        try {
            for (int i = 0; i < existingQF.length(); i++) {
                JSONObject m = existingQF.getJSONObject(i);
                Spinner[] row = addManualMatchRow(qfContainer, adapter, "QF " + (i + 1), m.optInt("team1_idx", 0), m.optInt("team2_idx", 0));
                qfSpinners.add(row);
            }
        } catch (Exception ignored) {}
        Button btnAddQF = makeAddMatchBtn("＋  Add QF Match");
        btnAddQF.setOnClickListener(v -> {
            Spinner[] row = addManualMatchRow(qfContainer, adapter, "QF " + (qfSpinners.size() + 1), 0, 0);
            qfSpinners.add(row);
        });
        LinearLayout.LayoutParams addQFLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        addQFLp.setMargins(0, dp(4), 0, dp(14)); btnAddQF.setLayoutParams(addQFLp);
        body.addView(btnAddQF);

        // ── SEMI FINALS section ──────────────────────────────────────────
        addManualSectionHeader(body, "🔥  SEMI FINALS", "#E65100");
        final LinearLayout sfContainer = new LinearLayout(this);
        sfContainer.setOrientation(LinearLayout.VERTICAL);
        body.addView(sfContainer);
        try {
            for (int i = 0; i < existingSF.length(); i++) {
                JSONObject m = existingSF.getJSONObject(i);
                Spinner[] row = addManualMatchRow(sfContainer, adapter, "SF " + (i + 1), m.optInt("team1_idx", 0), m.optInt("team2_idx", 0));
                sfSpinners.add(row);
            }
        } catch (Exception ignored) {}
        Button btnAddSF = makeAddMatchBtn("＋  Add SF Match");
        btnAddSF.setOnClickListener(v -> {
            Spinner[] row = addManualMatchRow(sfContainer, adapter, "SF " + (sfSpinners.size() + 1), 0, 0);
            sfSpinners.add(row);
        });
        LinearLayout.LayoutParams addSFLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        addSFLp.setMargins(0, dp(4), 0, dp(14)); btnAddSF.setLayoutParams(addSFLp);
        body.addView(btnAddSF);

        // ── FINAL section ────────────────────────────────────────────────
        addManualSectionHeader(body, "🏆  FINAL", "#F9A825");
        final LinearLayout finalContainer = new LinearLayout(this);
        finalContainer.setOrientation(LinearLayout.VERTICAL);
        body.addView(finalContainer);
        try {
            for (int i = 0; i < existingFinal.length(); i++) {
                JSONObject m = existingFinal.getJSONObject(i);
                Spinner[] row = addManualMatchRow(finalContainer, adapter, "Final", m.optInt("team1_idx", 0), m.optInt("team2_idx", 0));
                finalSpinners.add(row);
            }
        } catch (Exception ignored) {}
        if (existingFinal.length() == 0) {
            Spinner[] row = addManualMatchRow(finalContainer, adapter, "Final", 0, 0);
            finalSpinners.add(row);
        }

        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        sv.setLayoutParams(svLp);
        root.addView(sv);

        // Save / Cancel
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(dp(16), dp(12), dp(16), dp(16));

        Button btnCancel = new Button(this); btnCancel.setText("Cancel"); btnCancel.setAllCaps(false);
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setColor(Color.parseColor("#F1F5F9")); cancelBg.setCornerRadius(dp(10));
        btnCancel.setBackground(cancelBg);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        cancelLp.setMargins(0, 0, dp(8), 0); btnCancel.setLayoutParams(cancelLp);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        Button btnSave2 = new Button(this); btnSave2.setText("💾  Save Fixtures"); btnSave2.setAllCaps(false);
        btnSave2.setTextColor(Color.WHITE); btnSave2.setTypeface(null, Typeface.BOLD);
        GradientDrawable saveBg2 = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#388E3C")});
        saveBg2.setCornerRadius(dp(10)); btnSave2.setBackground(saveBg2);
        btnSave2.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1.5f));
        btnSave2.setOnClickListener(v -> {
            try {
                JSONObject mainObj = new JSONObject(prefs.getString("ALL_DATA", "{}"));
                mainObj.put("MatchesGroup", buildMatchArray(groupSpinners, teamOptions));
                mainObj.put("MatchesQF",    buildMatchArray(qfSpinners,    teamOptions));
                mainObj.put("MatchesSF",    buildMatchArray(sfSpinners,    teamOptions));
                mainObj.put("MatchesFinal", buildMatchArray(finalSpinners, teamOptions));
                prefs.edit().putString("ALL_DATA", mainObj.toString()).apply();
                int total = groupSpinners.size() + qfSpinners.size() + sfSpinners.size() + finalSpinners.size();
                Toast.makeText(this, "✅ " + total + " fixtures saved!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadExistingData();
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnRow.addView(btnCancel); btnRow.addView(btnSave2);
        root.addView(btnRow);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.95f),
                (int)(getResources().getDisplayMetrics().heightPixels * 0.88f));
        }
        dialog.show();
    }

    private void addManualSectionHeader(LinearLayout parent, String label, String colorHex) {
        TextView tv = new TextView(this); tv.setText(label);
        tv.setTextSize(11); tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(8)); tv.setLayoutParams(lp);
        parent.addView(tv);
    }

    private Spinner[] addManualMatchRow(LinearLayout container, ArrayAdapter<String> adapter, String label, int t1Idx, int t2Idx) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        cardBg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        cardBg.setCornerRadius(dp(10)); card.setBackground(cardBg);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(8)); card.setLayoutParams(cardLp);

        // Match label
        TextView tvLabel = new TextView(this); tvLabel.setText(label);
        tvLabel.setTextSize(11); tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setTextColor(Color.parseColor("#94A3B8"));
        LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lblLp.setMargins(0, 0, 0, dp(6)); tvLabel.setLayoutParams(lblLp);
        card.addView(tvLabel);

        // Team vs Team row
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Spinner sp1 = new Spinner(this);
        sp1.setAdapter(adapter);
        // Adjust selection — allTeams has "Select Team" at 0, but our teamOptions also does
        // t1Idx in saved data is based on allTeams list (which starts with "Select Team")
        // so offset is same — just clamp
        int sel1 = (t1Idx >= 0 && t1Idx < adapter.getCount()) ? t1Idx : 0;
        int sel2 = (t2Idx >= 0 && t2Idx < adapter.getCount()) ? t2Idx : 0;
        sp1.setSelection(sel1);
        sp1.setLayoutParams(new LinearLayout.LayoutParams(0, dp(42), 1f));

        TextView tvVs = new TextView(this); tvVs.setText("vs");
        tvVs.setTextSize(12); tvVs.setTypeface(null, Typeface.BOLD);
        tvVs.setTextColor(Color.parseColor("#94A3B8")); tvVs.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams vsLp = new LinearLayout.LayoutParams(dp(32), ViewGroup.LayoutParams.WRAP_CONTENT);
        tvVs.setLayoutParams(vsLp);

        Spinner sp2 = new Spinner(this);
        sp2.setAdapter(adapter);
        sp2.setSelection(sel2);
        sp2.setLayoutParams(new LinearLayout.LayoutParams(0, dp(42), 1f));

        // Delete button
        Button btnDel = new Button(this);
        btnDel.setText("✕");
        btnDel.setTextColor(Color.parseColor("#FFFFFF"));
        btnDel.setTextSize(13);
        GradientDrawable delBg = new GradientDrawable();
        delBg.setColor(Color.parseColor("#EF5350"));
        delBg.setCornerRadius(dp(8));
        btnDel.setBackground(delBg);
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(dp(38), dp(42));
        delLp.setMargins(dp(6), 0, 0, 0);
        btnDel.setLayoutParams(delLp);
        btnDel.setPadding(0, 0, 0, 0);

        final LinearLayout cardRef = card;
        btnDel.setOnClickListener(v -> container.removeView(cardRef));

        row.addView(sp1); row.addView(tvVs); row.addView(sp2); row.addView(btnDel);
        card.addView(row);
        container.addView(card);
        return new Spinner[]{sp1, sp2};
    }

    private Button makeAddMatchBtn(String label) {
        Button btn = new Button(this); btn.setText(label); btn.setAllCaps(false);
        btn.setTextColor(Color.parseColor("#1B5E20")); btn.setTextSize(12);
        btn.setTypeface(null, Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F0FDF4"));
        bg.setStroke(dp(1), Color.parseColor("#86EFAC")); bg.setCornerRadius(dp(10));
        btn.setBackground(bg);
        return btn;
    }

    private JSONArray buildMatchArray(List<Spinner[]> spinners, List<String> teamOptions) throws Exception {
        JSONArray arr = new JSONArray();
        for (Spinner[] pair : spinners) {
            int t1 = pair[0].getSelectedItemPosition();
            int t2 = pair[1].getSelectedItemPosition();
            if (t1 == 0 && t2 == 0) continue; // both placeholder — skip
            JSONObject m = new JSONObject();
            // Convert from teamOptions index to allTeams index
            // teamOptions[0]="— Select Team —", teamOptions[1..n]=teams
            // allTeams[0]="Select Team", allTeams[1..n]=teams → same offset
            m.put("team1_idx", t1);
            m.put("team2_idx", t2);
            m.put("date", "TBD");
            arr.put(m);
        }
        return arr;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOAD & DISPLAY
    // ═══════════════════════════════════════════════════════════════════════
    private void loadExistingData() {
        containerGroups.removeAllViews();
        containerGroupMatches.removeAllViews();
        containerQFMatches.removeAllViews();
        containerSFMatches.removeAllViews();
        containerFinalMatch.removeAllViews();
        allTeams.clear();
        allTeams.add("Select Team");

        try {
            String data = prefs.getString("ALL_DATA", "");
            if (data.isEmpty()) { addEmptyGroupsHint(); return; }
            JSONObject mainObj = new JSONObject(data);

            JSONArray dynGroups = mainObj.optJSONArray("DynamicGroups");
            if (dynGroups != null) {
                for (int g = 0; g < dynGroups.length(); g++) {
                    JSONObject grp = dynGroups.getJSONObject(g);
                    String gName   = grp.optString("name", "Group " + (char)('A' + g));
                    JSONArray tArr = grp.getJSONArray("teams");
                    StringBuilder sb = new StringBuilder();
                    for (int t = 0; t < tArr.length(); t++) {
                        allTeams.add(tArr.getString(t));
                        sb.append(tArr.getString(t));
                        if (t < tArr.length() - 1) sb.append(", ");
                    }
                    addGroupCard(gName, sb.toString(), tArr.length(), g);
                }
            }

            if (dynGroups == null || dynGroups.length() == 0) addEmptyGroupsHint();

            displayMatchSummary("Group Stage",  mainObj.optJSONArray("MatchesGroup"), containerGroupMatches);
            displayMatchSummary("Quarter Final",mainObj.optJSONArray("MatchesQF"),    containerQFMatches);
            displayMatchSummary("Semi Final",   mainObj.optJSONArray("MatchesSF"),    containerSFMatches);
            displayMatchSummary("Final",        mainObj.optJSONArray("MatchesFinal"), containerFinalMatch);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addGroupCard(String name, String teams, int count, int groupIdx) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setStroke(dp(1), Color.parseColor("#BBF7D0"));
        bg.setCornerRadius(dp(14));
        card.setBackground(bg);
        card.setElevation(dp(2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        // ── Header row ──────────────────────────────────────────────────
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topLp.setMargins(0, 0, 0, dp(10));
        top.setLayoutParams(topLp);

        // Group icon + name
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        nameRow.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(Color.parseColor("#E8F5E9")); iconBg.setCornerRadius(dp(8));
        TextView tvIcon = new TextView(this); tvIcon.setText("📌");
        tvIcon.setTextSize(14); tvIcon.setPadding(dp(6), dp(4), dp(6), dp(4));
        tvIcon.setBackground(iconBg);
        nameRow.addView(tvIcon);

        TextView tvName = new TextView(this);
        tvName.setText("  " + name);
        tvName.setTextSize(14); tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#1B5E20"));
        nameRow.addView(tvName);
        top.addView(nameRow);

        // Teams count badge
        TextView tvCount = new TextView(this);
        tvCount.setText(count + " teams");
        tvCount.setTextSize(10); tvCount.setTextColor(Color.WHITE);
        GradientDrawable chip = new GradientDrawable();
        chip.setColor(Color.parseColor("#2E7D32")); chip.setCornerRadius(dp(20));
        tvCount.setBackground(chip); tvCount.setPadding(dp(8), dp(3), dp(8), dp(3));
        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipLp.setMargins(0, 0, dp(8), 0);
        tvCount.setLayoutParams(chipLp);
        top.addView(tvCount);

        // Delete Group button
        TextView btnDelGroup = new TextView(this);
        btnDelGroup.setText("🗑");
        btnDelGroup.setTextSize(15);
        btnDelGroup.setPadding(dp(6), dp(3), dp(6), dp(3));
        btnDelGroup.setClickable(true); btnDelGroup.setFocusable(true);
        GradientDrawable delBg = new GradientDrawable();
        delBg.setColor(Color.parseColor("#FFF5F5")); delBg.setCornerRadius(dp(8));
        delBg.setStroke(dp(1), Color.parseColor("#FECACA"));
        btnDelGroup.setBackground(delBg);
        btnDelGroup.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Delete Group")
                .setMessage("Delete \"" + name + "\" and all its teams?")
                .setPositiveButton("Delete", (d, w) -> deleteGroup(groupIdx))
                .setNegativeButton("Cancel", null)
                .show();
        });
        top.addView(btnDelGroup);
        card.addView(top);

        // ── Thin divider ─────────────────────────────────────────────────
        View div = new View(this);
        div.setBackgroundColor(Color.parseColor("#F0FDF4"));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divLp.setMargins(0, 0, 0, dp(8));
        div.setLayoutParams(divLp);
        card.addView(div);

        // ── Team list with Edit & Delete ──────────────────────────────────
        String[] teamArray = teams.split(", ");
        for (int t = 0; t < teamArray.length; t++) {
            final String teamName = teamArray[t].trim();
            if (teamName.isEmpty()) continue;
            final int teamIdx = t;

            LinearLayout teamRow = new LinearLayout(this);
            teamRow.setOrientation(LinearLayout.HORIZONTAL);
            teamRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            trLp.setMargins(0, 0, 0, dp(6));
            teamRow.setLayoutParams(trLp);
            GradientDrawable teamRowBg = new GradientDrawable();
            teamRowBg.setColor(Color.parseColor("#F8FFF8"));
            teamRowBg.setStroke(dp(1), Color.parseColor("#DCFCE7"));
            teamRowBg.setCornerRadius(dp(8));
            teamRow.setBackground(teamRowBg);
            teamRow.setPadding(dp(10), dp(7), dp(8), dp(7));

            // Team number badge
            TextView tvNum = new TextView(this);
            tvNum.setText(String.valueOf(t + 1));
            tvNum.setTextSize(10); tvNum.setTypeface(null, Typeface.BOLD);
            tvNum.setTextColor(Color.parseColor("#2E7D32"));
            GradientDrawable numBg = new GradientDrawable();
            numBg.setColor(Color.parseColor("#DCFCE7")); numBg.setCornerRadius(dp(20));
            tvNum.setBackground(numBg); tvNum.setGravity(Gravity.CENTER);
            tvNum.setMinWidth(dp(22)); tvNum.setPadding(dp(4), dp(2), dp(4), dp(2));
            LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            numLp.setMargins(0, 0, dp(8), 0);
            tvNum.setLayoutParams(numLp);
            teamRow.addView(tvNum);

            // Team name
            TextView tvTeam = new TextView(this);
            tvTeam.setText(teamName);
            tvTeam.setTextSize(13); tvTeam.setTypeface(null, Typeface.BOLD);
            tvTeam.setTextColor(Color.parseColor("#1E293B"));
            tvTeam.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            teamRow.addView(tvTeam);

            // Edit button ✏️
            TextView btnEdit = new TextView(this);
            btnEdit.setText("✏️");
            btnEdit.setTextSize(14);
            btnEdit.setPadding(dp(7), dp(4), dp(7), dp(4));
            btnEdit.setClickable(true); btnEdit.setFocusable(true);
            GradientDrawable editBg = new GradientDrawable();
            editBg.setColor(Color.parseColor("#EFF6FF")); editBg.setCornerRadius(dp(7));
            editBg.setStroke(dp(1), Color.parseColor("#BFDBFE"));
            btnEdit.setBackground(editBg);
            LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            editLp.setMargins(0, 0, dp(6), 0);
            btnEdit.setLayoutParams(editLp);
            btnEdit.setOnClickListener(v -> showEditTeamDialog(groupIdx, teamIdx, teamName));
            teamRow.addView(btnEdit);

            // Delete button 🗑️
            TextView btnDel = new TextView(this);
            btnDel.setText("🗑");
            btnDel.setTextSize(14);
            btnDel.setPadding(dp(7), dp(4), dp(7), dp(4));
            btnDel.setClickable(true); btnDel.setFocusable(true);
            GradientDrawable delTeamBg = new GradientDrawable();
            delTeamBg.setColor(Color.parseColor("#FFF5F5")); delTeamBg.setCornerRadius(dp(7));
            delTeamBg.setStroke(dp(1), Color.parseColor("#FECACA"));
            btnDel.setBackground(delTeamBg);
            btnDel.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("Remove Team")
                    .setMessage("Remove \"" + teamName + "\" from " + name + "?")
                    .setPositiveButton("Remove", (d, w) -> removeTeamFromGroup(groupIdx, teamIdx))
                    .setNegativeButton("Cancel", null)
                    .show();
            });
            teamRow.addView(btnDel);
            card.addView(teamRow);
        }

        // ── Add Team Button (inside group card) ───────────────────────────
        LinearLayout addTeamBtn = new LinearLayout(this);
        addTeamBtn.setOrientation(LinearLayout.HORIZONTAL);
        addTeamBtn.setGravity(Gravity.CENTER);
        addTeamBtn.setClickable(true); addTeamBtn.setFocusable(true);
        GradientDrawable addTeamBg = new GradientDrawable();
        addTeamBg.setColor(Color.parseColor("#F0FDF4"));
        addTeamBg.setStroke(dp(1), Color.parseColor("#86EFAC"));
        addTeamBg.setCornerRadius(dp(8));
        addTeamBtn.setBackground(addTeamBg);
        addTeamBtn.setPadding(dp(10), dp(9), dp(10), dp(9));
        LinearLayout.LayoutParams addTeamLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addTeamLp.setMargins(0, dp(6), 0, 0);
        addTeamBtn.setLayoutParams(addTeamLp);

        TextView tvAddIcon = new TextView(this); tvAddIcon.setText("＋");
        tvAddIcon.setTextSize(14); tvAddIcon.setTypeface(null, Typeface.BOLD);
        tvAddIcon.setTextColor(Color.parseColor("#15803D"));
        LinearLayout.LayoutParams plusLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plusLp.setMargins(0, 0, dp(6), 0); tvAddIcon.setLayoutParams(plusLp);
        addTeamBtn.addView(tvAddIcon);

        TextView tvAddLabel = new TextView(this); tvAddLabel.setText("Add Team to " + name);
        tvAddLabel.setTextSize(12); tvAddLabel.setTypeface(null, Typeface.BOLD);
        tvAddLabel.setTextColor(Color.parseColor("#15803D"));
        addTeamBtn.addView(tvAddLabel);

        addTeamBtn.setOnClickListener(v -> showAddTeamToGroupDialog(groupIdx, name));
        card.addView(addTeamBtn);

        containerGroups.addView(card);
    }

    // ── Add single team to existing group dialog ──────────────────────────
    private void showAddTeamToGroupDialog(int groupIdx, String groupName) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.WHITE); rootBg.setCornerRadius(dp(18));
        root.setBackground(rootBg);

        // Header
        LinearLayout hdrRow = new LinearLayout(this);
        hdrRow.setOrientation(LinearLayout.HORIZONTAL); hdrRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hdrLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hdrLp.setMargins(0, 0, 0, dp(4)); hdrRow.setLayoutParams(hdrLp);

        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(Color.parseColor("#E8F5E9")); iconBg.setCornerRadius(dp(10));
        TextView tvIco = new TextView(this); tvIco.setText("➕");
        tvIco.setTextSize(20); tvIco.setPadding(dp(8), dp(6), dp(8), dp(6));
        tvIco.setBackground(iconBg);
        LinearLayout.LayoutParams icoLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        icoLp.setMargins(0, 0, dp(12), 0); tvIco.setLayoutParams(icoLp);
        hdrRow.addView(tvIco);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView tvTitle = new TextView(this); tvTitle.setText("Add Team");
        tvTitle.setTextSize(17); tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#1B5E20"));
        TextView tvGrpLabel = new TextView(this); tvGrpLabel.setText("to " + groupName);
        tvGrpLabel.setTextSize(11); tvGrpLabel.setTextColor(Color.parseColor("#64748B"));
        titleCol.addView(tvTitle); titleCol.addView(tvGrpLabel);
        hdrRow.addView(titleCol);
        root.addView(hdrRow);

        // Divider
        View divider = new View(this); divider.setBackgroundColor(Color.parseColor("#F0FDF4"));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divLp.setMargins(0, dp(14), 0, dp(14)); divider.setLayoutParams(divLp);
        root.addView(divider);

        // Team name input
        TextView tvHint = new TextView(this); tvHint.setText("Team Name");
        tvHint.setTextSize(11); tvHint.setTypeface(null, Typeface.BOLD);
        tvHint.setTextColor(Color.parseColor("#94A3B8"));
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.setMargins(0, 0, 0, dp(6)); tvHint.setLayoutParams(hintLp);
        root.addView(tvHint);

        EditText etTeamName = makeEditText("e.g. Mumbai Indians");
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etLp.setMargins(0, 0, 0, dp(20)); etTeamName.setLayoutParams(etLp);
        root.addView(etTeamName);

        // Buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel"); btnCancel.setAllCaps(false);
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setColor(Color.parseColor("#F1F5F9")); cancelBg.setCornerRadius(dp(10));
        btnCancel.setBackground(cancelBg);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        cancelLp.setMargins(0, 0, dp(8), 0); btnCancel.setLayoutParams(cancelLp);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        Button btnAdd = new Button(this);
        btnAdd.setText("✅  Add Team"); btnAdd.setAllCaps(false);
        btnAdd.setTextColor(Color.WHITE); btnAdd.setTypeface(null, Typeface.BOLD);
        GradientDrawable addBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#388E3C")});
        addBg.setCornerRadius(dp(10)); btnAdd.setBackground(addBg);
        btnAdd.setLayoutParams(new LinearLayout.LayoutParams(0, dp(46), 1.5f));
        btnAdd.setOnClickListener(v -> {
            String newTeam = etTeamName.getText().toString().trim();
            if (newTeam.isEmpty()) {
                Toast.makeText(this, "Please enter a team name", Toast.LENGTH_SHORT).show();
                return;
            }
            addTeamToGroup(groupIdx, newTeam);
            dialog.dismiss();
        });

        btnRow.addView(btnCancel); btnRow.addView(btnAdd);
        root.addView(btnRow);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.88f),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
        etTeamName.requestFocus();
    }

    // ── Save new team into existing group ─────────────────────────────────
    private void addTeamToGroup(int groupIdx, String teamName) {
        try {
            JSONObject mainObj = new JSONObject(prefs.getString("ALL_DATA", "{}"));
            JSONArray dynGroups = mainObj.optJSONArray("DynamicGroups");
            if (dynGroups == null || groupIdx >= dynGroups.length()) return;
            JSONArray tArr = dynGroups.getJSONObject(groupIdx).getJSONArray("teams");
            tArr.put(teamName);
            dynGroups.getJSONObject(groupIdx).put("teams", tArr);
            mainObj.put("DynamicGroups", dynGroups);
            prefs.edit().putString("ALL_DATA", mainObj.toString()).apply();
            Toast.makeText(this, "✅ \"" + teamName + "\" added!", Toast.LENGTH_SHORT).show();
            loadExistingData();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Edit team name dialog ─────────────────────────────────────────────
    private void showEditTeamDialog(int groupIdx, int teamIdx, String currentName) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.WHITE); rootBg.setCornerRadius(dp(18));
        root.setBackground(rootBg);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("✏️  Edit Team Name");
        tvTitle.setTextSize(17); tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#1B5E20"));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.setMargins(0, 0, 0, dp(16));
        tvTitle.setLayoutParams(titleLp);
        root.addView(tvTitle);

        EditText et = makeEditText("Team Name");
        et.setText(currentName);
        et.setSelection(currentName.length());
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etLp.setMargins(0, 0, 0, dp(20));
        et.setLayoutParams(etLp);
        root.addView(et);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel"); btnCancel.setAllCaps(false);
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setColor(Color.parseColor("#F1F5F9")); cancelBg.setCornerRadius(dp(10));
        btnCancel.setBackground(cancelBg);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        cancelLp.setMargins(0, 0, dp(8), 0);
        btnCancel.setLayoutParams(cancelLp);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        Button btnSaveEdit = new Button(this);
        btnSaveEdit.setText("Save"); btnSaveEdit.setAllCaps(false);
        btnSaveEdit.setTextColor(Color.WHITE); btnSaveEdit.setTypeface(null, Typeface.BOLD);
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setColor(Color.parseColor("#1B5E20")); saveBg.setCornerRadius(dp(10));
        btnSaveEdit.setBackground(saveBg);
        btnSaveEdit.setLayoutParams(new LinearLayout.LayoutParams(0, dp(46), 1.5f));
        btnSaveEdit.setOnClickListener(v -> {
            String newName = et.getText().toString().trim();
            if (newName.isEmpty()) { Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show(); return; }
            updateTeamName(groupIdx, teamIdx, newName);
            dialog.dismiss();
        });

        btnRow.addView(btnCancel); btnRow.addView(btnSaveEdit);
        root.addView(btnRow);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.88f),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // ── Update team name in SharedPreferences ─────────────────────────────
    private void updateTeamName(int groupIdx, int teamIdx, String newName) {
        try {
            JSONObject mainObj = new JSONObject(prefs.getString("ALL_DATA", "{}"));
            JSONArray dynGroups = mainObj.optJSONArray("DynamicGroups");
            if (dynGroups == null || groupIdx >= dynGroups.length()) return;
            JSONArray tArr = dynGroups.getJSONObject(groupIdx).getJSONArray("teams");
            if (teamIdx >= tArr.length()) return;
            tArr.put(teamIdx, newName);
            dynGroups.getJSONObject(groupIdx).put("teams", tArr);
            mainObj.put("DynamicGroups", dynGroups);
            prefs.edit().putString("ALL_DATA", mainObj.toString()).apply();
            Toast.makeText(this, "✅ Team renamed to \"" + newName + "\"", Toast.LENGTH_SHORT).show();
            loadExistingData();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Remove team from group ────────────────────────────────────────────
    private void removeTeamFromGroup(int groupIdx, int teamIdx) {
        try {
            JSONObject mainObj = new JSONObject(prefs.getString("ALL_DATA", "{}"));
            JSONArray dynGroups = mainObj.optJSONArray("DynamicGroups");
            if (dynGroups == null || groupIdx >= dynGroups.length()) return;
            JSONArray oldArr = dynGroups.getJSONObject(groupIdx).getJSONArray("teams");
            JSONArray newArr = new JSONArray();
            for (int i = 0; i < oldArr.length(); i++) {
                if (i != teamIdx) newArr.put(oldArr.getString(i));
            }
            dynGroups.getJSONObject(groupIdx).put("teams", newArr);
            mainObj.put("DynamicGroups", dynGroups);
            prefs.edit().putString("ALL_DATA", mainObj.toString()).apply();
            Toast.makeText(this, "✅ Team removed", Toast.LENGTH_SHORT).show();
            loadExistingData();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Delete entire group ───────────────────────────────────────────────
    private void deleteGroup(int groupIdx) {
        try {
            JSONObject mainObj = new JSONObject(prefs.getString("ALL_DATA", "{}"));
            JSONArray dynGroups = mainObj.optJSONArray("DynamicGroups");
            if (dynGroups == null || groupIdx >= dynGroups.length()) return;
            JSONArray newGroups = new JSONArray();
            for (int i = 0; i < dynGroups.length(); i++) {
                if (i != groupIdx) newGroups.put(dynGroups.getJSONObject(i));
            }
            mainObj.put("DynamicGroups", newGroups);
            prefs.edit().putString("ALL_DATA", mainObj.toString()).apply();
            Toast.makeText(this, "✅ Group deleted", Toast.LENGTH_SHORT).show();
            loadExistingData();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void displayMatchSummary(String stage, JSONArray matches, LinearLayout container) {
        if (matches == null || matches.length() == 0) return;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL); card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F0FDF4"));
        bg.setStroke(dp(1), Color.parseColor("#BBF7D0")); bg.setCornerRadius(dp(10));
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        card.setLayoutParams(lp);

        TextView tvInfo = new TextView(this);
        tvInfo.setText("✅ " + matches.length() + " " + stage + " matches scheduled");
        tvInfo.setTextSize(13); tvInfo.setTextColor(Color.parseColor("#1B5E20"));
        tvInfo.setTypeface(null, Typeface.BOLD);
        card.addView(tvInfo);
        container.addView(card);
    }

    private void addEmptyGroupsHint() {
        TextView tv = new TextView(this);
        tv.setText("📋  No groups yet.\nUse 'Create New Group' or '🤖 AI Generate' to start.");
        tv.setTextColor(Color.parseColor("#94A3B8")); tv.setTextSize(13);
        tv.setGravity(Gravity.CENTER); tv.setPadding(0, dp(16), 0, dp(16));
        containerGroups.addView(tv);
    }

    

    private List<String> collectAllTeams() {
        List<String> teams = new ArrayList<>();
        try {
            JSONObject mainObj  = new JSONObject(prefs.getString("ALL_DATA", "{}"));
            JSONArray dynGroups = mainObj.optJSONArray("DynamicGroups");
            if (dynGroups != null) {
                for (int g = 0; g < dynGroups.length(); g++) {
                    JSONArray tArr = dynGroups.getJSONObject(g).getJSONArray("teams");
                    for (int t = 0; t < tArr.length(); t++) teams.add(tArr.getString(t));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return teams;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UI HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    private LinearLayout makeFormatOption(String label, String desc, boolean selected) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setClickable(true); row.setFocusable(true);
        updateFormatBg(row, selected);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvLabel = new TextView(this); tvLabel.setText(label);
        tvLabel.setTextSize(13); tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setTextColor(selected ? Color.WHITE : Color.parseColor("#1E293B"));
        tvLabel.setTag("label");

        TextView tvDesc = new TextView(this); tvDesc.setText(desc);
        tvDesc.setTextSize(11);
        tvDesc.setTextColor(selected ? Color.parseColor("#A5D6A7") : Color.parseColor("#64748B"));
        tvDesc.setTag("desc");

        textCol.addView(tvLabel); textCol.addView(tvDesc); row.addView(textCol);

        TextView check = new TextView(this); check.setText(selected ? "●" : "○");
        check.setTextSize(16);
        check.setTextColor(selected ? Color.WHITE : Color.parseColor("#CBD5E1"));
        check.setTag("check");
        row.addView(check);
        return row;
    }

    private void setFormatSelected(LinearLayout row, boolean selected) {
        updateFormatBg(row, selected);
        ((TextView) row.findViewWithTag("label")).setTextColor(
            selected ? Color.WHITE : Color.parseColor("#1E293B"));
        ((TextView) row.findViewWithTag("desc")).setTextColor(
            selected ? Color.parseColor("#A5D6A7") : Color.parseColor("#64748B"));
        ((TextView) row.findViewWithTag("check")).setText(selected ? "●" : "○");
        ((TextView) row.findViewWithTag("check")).setTextColor(
            selected ? Color.WHITE : Color.parseColor("#CBD5E1"));
    }

    private void updateFormatBg(LinearLayout row, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        if (selected) {
            bg.setColor(Color.parseColor("#1B5E20"));
        } else {
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        }
        bg.setCornerRadius(dp(10));
        row.setBackground(bg);
    }

    private LinearLayout makeCompactOption(String label, boolean selected) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER); row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setClickable(true); row.setFocusable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(selected ? Color.parseColor("#E8F5E9") : Color.parseColor("#F8FAFC"));
        bg.setStroke(dp(1), selected ? Color.parseColor("#2E7D32") : Color.parseColor("#E2E8F0"));
        bg.setCornerRadius(dp(10)); row.setBackground(bg);
        TextView tv = new TextView(this); tv.setText(label); tv.setTag("lbl");
        tv.setTextSize(12); tv.setTypeface(null, Typeface.BOLD); tv.setGravity(Gravity.CENTER);
        tv.setTextColor(selected ? Color.parseColor("#1B5E20") : Color.parseColor("#64748B"));
        row.addView(tv);
        return row;
    }

    private void setCompactSelected(LinearLayout row, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(selected ? Color.parseColor("#E8F5E9") : Color.parseColor("#F8FAFC"));
        bg.setStroke(dp(1), selected ? Color.parseColor("#2E7D32") : Color.parseColor("#E2E8F0"));
        bg.setCornerRadius(dp(10)); row.setBackground(bg);
        ((TextView) row.findViewWithTag("lbl")).setTextColor(
            selected ? Color.parseColor("#1B5E20") : Color.parseColor("#64748B"));
    }

    private LinearLayout makeStatChip(String label, String value, String color) {
        LinearLayout chip = new LinearLayout(this); chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER); chip.setPadding(dp(16), dp(10), dp(16), dp(10));
        chip.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView tvVal = new TextView(this); tvVal.setText(value);
        tvVal.setTextSize(20); tvVal.setTypeface(null, Typeface.BOLD);
        tvVal.setTextColor(Color.parseColor(color)); tvVal.setGravity(Gravity.CENTER);
        TextView tvLabel = new TextView(this); tvLabel.setText(label);
        tvLabel.setTextSize(10); tvLabel.setTextColor(Color.parseColor("#94A3B8"));
        tvLabel.setGravity(Gravity.CENTER);
        chip.addView(tvVal); chip.addView(tvLabel);
        return chip;
    }

    private void addSectionLabel(LinearLayout parent, String label) {
        TextView tv = new TextView(this); tv.setText(label);
        tv.setTextSize(12); tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }

    private EditText makeEditText(String hint) {
        EditText et = new EditText(this); et.setHint(hint); et.setTextSize(14);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F8FAFC"));
        bg.setStroke(dp(1), Color.parseColor("#E2E8F0")); bg.setCornerRadius(dp(10));
        et.setBackground(bg);
        return et;
    }

    private LinearLayout makeCompactOptionWithDesc(String label, String desc, boolean selected) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setClickable(true); row.setFocusable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(selected ? Color.parseColor("#E8F5E9") : Color.parseColor("#F8FAFC"));
        bg.setStroke(dp(1), selected ? Color.parseColor("#2E7D32") : Color.parseColor("#E2E8F0"));
        bg.setCornerRadius(dp(10)); row.setBackground(bg);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvLbl = new TextView(this); tvLbl.setText(label); tvLbl.setTag("lbl");
        tvLbl.setTextSize(13); tvLbl.setTypeface(null, Typeface.BOLD);
        tvLbl.setTextColor(selected ? Color.parseColor("#1B5E20") : Color.parseColor("#1E293B"));

        TextView tvDesc = new TextView(this); tvDesc.setText(desc); tvDesc.setTag("desc");
        tvDesc.setTextSize(10);
        tvDesc.setTextColor(selected ? Color.parseColor("#4CAF50") : Color.parseColor("#94A3B8"));

        textCol.addView(tvLbl); textCol.addView(tvDesc); row.addView(textCol);

        TextView check = new TextView(this); check.setText(selected ? "●" : "○"); check.setTag("check");
        check.setTextSize(14);
        check.setTextColor(selected ? Color.parseColor("#2E7D32") : Color.parseColor("#CBD5E1"));
        row.addView(check);
        return row;
    }

    private void setCompactDescSelected(LinearLayout row, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(selected ? Color.parseColor("#E8F5E9") : Color.parseColor("#F8FAFC"));
        bg.setStroke(dp(1), selected ? Color.parseColor("#2E7D32") : Color.parseColor("#E2E8F0"));
        bg.setCornerRadius(dp(10)); row.setBackground(bg);
        View lbl = row.findViewWithTag("lbl");
        View desc = row.findViewWithTag("desc");
        View check = row.findViewWithTag("check");
        if (lbl instanceof TextView)
            ((TextView) lbl).setTextColor(selected ? Color.parseColor("#1B5E20") : Color.parseColor("#1E293B"));
        if (desc instanceof TextView)
            ((TextView) desc).setTextColor(selected ? Color.parseColor("#4CAF50") : Color.parseColor("#94A3B8"));
        if (check instanceof TextView) {
            ((TextView) check).setText(selected ? "●" : "○");
            ((TextView) check).setTextColor(selected ? Color.parseColor("#2E7D32") : Color.parseColor("#CBD5E1"));
        }
    }

    private CheckBox makeStyledCheckbox(String label, boolean checked) {
        CheckBox cb = new CheckBox(this);
        cb.setText(label); cb.setChecked(checked);
        cb.setTextSize(13); cb.setTextColor(Color.parseColor("#374151"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        cb.setLayoutParams(lp);
        return cb;
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }
    // ১. ফিক্সচার সেভ করার মেইন মেথড
    private void saveFixtures() {
        Toast.makeText(this, "⏳ Syncing fixtures to server...", Toast.LENGTH_SHORT).show();

        List<String> allTeamsList = collectAllTeams(); // সব টিমের নাম কালেক্ট করা

        // Firebase-এ fixture যোগ করার জন্য বর্তমান tournamentId (upsertTournament() থেকে সেভ করা)
        String tournamentId = prefs.getString("SUPABASE_TOURNAMENT_ID", null);

        // Group Matches সেভ করা
        if (groupMatches != null) {
            for (int i = 0; i < groupMatches.size(); i++) {
                int[] m = groupMatches.get(i);
                String team1 = (m[0] >= 0 && m[0] < allTeamsList.size()) ? allTeamsList.get(m[0]) : "TBD";
                String team2 = (m[1] >= 0 && m[1] < allTeamsList.size()) ? allTeamsList.get(m[1]) : "TBD";
                saveMatchToMySql(team1, team2, "TBD", "Group Stage");
                pushFixtureToFirebase(tournamentId, team1, team2, "TBD");
            }
        }

        // Quarter Final Matches সেভ করা
        if (qfMatches != null) {
            for (int i = 0; i < qfMatches.size(); i++) {
                int[] m = qfMatches.get(i);
                String team1 = (m[0] >= 0 && m[0] < allTeamsList.size()) ? allTeamsList.get(m[0]) : "TBD";
                String team2 = (m[1] >= 0 && m[1] < allTeamsList.size()) ? allTeamsList.get(m[1]) : "TBD";
                saveMatchToMySql(team1, team2, "TBD", "Quarter Final");
                pushFixtureToFirebase(tournamentId, team1, team2, "TBD");
            }
        }

        // Semi Final Matches সেভ করা
        if (sfMatches != null) {
            for (int i = 0; i < sfMatches.size(); i++) {
                int[] m = sfMatches.get(i);
                String team1 = (m[0] >= 0 && m[0] < allTeamsList.size()) ? allTeamsList.get(m[0]) : "TBD";
                String team2 = (m[1] >= 0 && m[1] < allTeamsList.size()) ? allTeamsList.get(m[1]) : "TBD";
                saveMatchToMySql(team1, team2, "TBD", "Semi Final");
                pushFixtureToFirebase(tournamentId, team1, team2, "TBD");
            }
        }

        // Final Match সেভ করা
        if (finalMatch != null) {
            for (int i = 0; i < finalMatch.size(); i++) {
                int[] m = finalMatch.get(i);
                String team1 = (m[0] >= 0 && m[0] < allTeamsList.size()) ? allTeamsList.get(m[0]) : "TBD";
                String team2 = (m[1] >= 0 && m[1] < allTeamsList.size()) ? allTeamsList.get(m[1]) : "TBD";
                saveMatchToMySql(team1, team2, "TBD", "Final");
                pushFixtureToFirebase(tournamentId, team1, team2, "TBD");
            }
        }

        Toast.makeText(this, "✅ Fixtures synced to server successfully!", Toast.LENGTH_SHORT).show();
    }

    // ➕ Firebase-এ fixture push করা — viewer অ্যাপ/ওয়েবসাইট এখান থেকে schedule দেখতে পারবে
    private void pushFixtureToFirebase(String tournamentId, String teamA, String teamB, String date) {
        if (teamA.equals("TBD") || teamB.equals("TBD")) return;
        if (tournamentId == null || tournamentId.isEmpty()) {
            Log.w("FirebaseSync", "pushFixtureToFirebase() skipped — no tournamentId found. " +
                    "আগে Tournament Settings স্ক্রিন থেকে টুর্নামেন্ট তৈরি করে নাও।");
            return;
        }
        FirebaseSync.addFixture(tournamentId, teamA, teamB, date, "");
    }

    // ২. নিজস্ব MySQL সার্ভারে ম্যাচ সেভ করার মেথড
    private void saveMatchToMySql(String team1, String team2, String date, String venue) {
        if(team1.equals("TBD") || team2.equals("TBD")) return; 

        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        apiInterface.addMatch(team1, team2, date, venue).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if(response.isSuccessful()){
                    Log.d("MYSQL_MATCH", team1 + " vs " + team2 + " synced successfully!");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MYSQL_ERROR", "Failed to sync fixture: " + t.getMessage());
            }
        });
    }

    // ৩. নিজস্ব MySQL সার্ভার থেকে ম্যাচ ডিলিট করার মেথড
    private void deleteMatchFromMySql(String matchId) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        apiInterface.deleteMatch(matchId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if(response.isSuccessful()){
                    Log.d("MYSQL_MATCH", "Fixture deleted from server successfully!");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MYSQL_ERROR", "Failed to delete fixture: " + t.getMessage());
            }
        });
    }

}
