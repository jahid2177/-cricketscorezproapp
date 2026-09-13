package com.cricketscorez.proapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PartnershipGraphActivity extends Activity {

    private MatchData matchData;
    private String team1 = "Team 1";
    private String team2 = "Team 2";

    private boolean showFirstInnings = true;

    private PartnershipGraphView graphView;
    private Button btnTabInn1, btnTabInn2;
    private TextView tvMatchSubtitle, tvHighestStand, tvAverageStand, tvTotalStands;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partnership_graph);

        graphView = findViewById(R.id.partnershipGraphView);
        btnTabInn1 = findViewById(R.id.btnTabInn1);
        btnTabInn2 = findViewById(R.id.btnTabInn2);
        tvMatchSubtitle = findViewById(R.id.tvMatchSubtitle);
        tvHighestStand = findViewById(R.id.tvHighestStand);
        tvAverageStand = findViewById(R.id.tvAverageStand);
        tvTotalStands = findViewById(R.id.tvTotalStands);
        ImageButton btnBack = findViewById(R.id.btnBackPartnership);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Receive data
        matchData = (MatchData) getIntent().getSerializableExtra("MATCH_DATA");
        String t1 = getIntent().getStringExtra("TEAM_1");
        String t2 = getIntent().getStringExtra("TEAM_2");
        if (t1 != null && !t1.isEmpty()) team1 = t1;
        if (t2 != null && !t2.isEmpty()) team2 = t2;

        if (matchData != null) {
            if (matchData.teamBattingFirst != null && !matchData.teamBattingFirst.isEmpty()) {
                team1 = matchData.teamBattingFirst;
            }
            if (matchData.teamBattingSecond != null && !matchData.teamBattingSecond.isEmpty()) {
                team2 = matchData.teamBattingSecond;
            }
            // If match is currently in second innings, default tab can be 2nd innings
            if (matchData.isSecondInnings) {
                showFirstInnings = false;
            }
        }

        tvMatchSubtitle.setText(team1 + " vs " + team2);
        btnTabInn1.setText("1st Innings (" + team1 + ")");
        btnTabInn2.setText("2nd Innings (" + team2 + ")");

        btnTabInn1.setOnClickListener(v -> {
            showFirstInnings = true;
            updateView();
        });

        btnTabInn2.setOnClickListener(v -> {
            showFirstInnings = false;
            updateView();
        });

        updateView();
    }

    private void updateView() {
        // Tab UI styling
        if (showFirstInnings) {
            btnTabInn1.setBackgroundResource(R.drawable.bg_btn_save);
            btnTabInn1.setTextColor(Color.WHITE);
            btnTabInn2.setBackgroundResource(R.drawable.bg_card_modern);
            btnTabInn2.setTextColor(Color.parseColor("#1E293B"));
        } else {
            btnTabInn2.setBackgroundResource(R.drawable.bg_btn_save);
            btnTabInn2.setTextColor(Color.WHITE);
            btnTabInn1.setBackgroundResource(R.drawable.bg_card_modern);
            btnTabInn1.setTextColor(Color.parseColor("#1E293B"));
        }

        List<MatchData.PartnershipRecord> list = null;
        String currentTeam = showFirstInnings ? team1 : team2;

        if (matchData != null) {
            list = matchData.getAllPartnerships(showFirstInnings);
        }

        if (list == null) list = new ArrayList<>();

        // Calculate summary stats
        int maxRuns = 0;
        int sumRuns = 0;
        for (MatchData.PartnershipRecord r : list) {
            if (r.totalRuns > maxRuns) maxRuns = r.totalRuns;
            sumRuns += r.totalRuns;
        }
        double avg = list.isEmpty() ? 0.0 : (sumRuns / (double) list.size());

        tvHighestStand.setText(maxRuns + " runs");
        tvAverageStand.setText(String.format(Locale.getDefault(), "%.1f runs", avg));
        tvTotalStands.setText(String.valueOf(list.size()));

        graphView.setPartnershipData(list, currentTeam);
    }
}
