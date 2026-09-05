package com.cricketscorez.proapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import java.util.ArrayList;

public class ScoringBreakdownActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoring_breakdown);

        // --- ডাটা রিসিভ ---
        ArrayList<Integer> runs1 = getIntent().getIntegerArrayListExtra("INN1_DATA");
        ArrayList<Integer> wkts1 = getIntent().getIntegerArrayListExtra("INN1_WICKET_DATA");
        ArrayList<Integer> runs2 = getIntent().getIntegerArrayListExtra("INN2_DATA");
        ArrayList<Integer> wkts2 = getIntent().getIntegerArrayListExtra("INN2_WICKET_DATA");

        int t1Sixes = getIntent().getIntExtra("T1_SIXES", 0);
        int t1Fours = getIntent().getIntExtra("T1_FOURS", 0);
        int t1Dots = getIntent().getIntExtra("T1_DOTS", 0);
        int t1Extras = getIntent().getIntExtra("T1_EXTRAS", 0);

        int t2Sixes = getIntent().getIntExtra("T2_SIXES", 0);
        int t2Fours = getIntent().getIntExtra("T2_FOURS", 0);
        int t2Dots = getIntent().getIntExtra("T2_DOTS", 0);
        int t2Extras = getIntent().getIntExtra("T2_EXTRAS", 0);

        // --- হেল্পার অবজেক্ট ---
        MatchData helper = new MatchData(null, null, "20");

        // --- ভিউ বাইন্ডিং এবং ডাটা সেট ---
        ((TextView)findViewById(R.id.tvTeam1Name)).setText(getIntent().getStringExtra("TEAM_1"));
        ((TextView)findViewById(R.id.tvTeam2Name)).setText(getIntent().getStringExtra("TEAM_2"));

        // Phase Scores (Powerplay 1-6, Middle 7-15, Final 16-20)
        setTextSafely(R.id.tvT1PowerPlay, helper.getPhaseScore(runs1, wkts1, 1, 6));
        setTextSafely(R.id.tvT2PowerPlay, helper.getPhaseScore(runs2, wkts2, 1, 6));

        setTextSafely(R.id.tvT1Middle, helper.getPhaseScore(runs1, wkts1, 7, 15));
        setTextSafely(R.id.tvT2Middle, helper.getPhaseScore(runs2, wkts2, 7, 15));

        setTextSafely(R.id.tvT1Final, helper.getPhaseScore(runs1, wkts1, 16, 20));
        setTextSafely(R.id.tvT2Final, helper.getPhaseScore(runs2, wkts2, 16, 20));

        // Stats
        setTextSafely(R.id.tvT1Sixes, String.valueOf(t1Sixes));
        setTextSafely(R.id.tvT2Sixes, String.valueOf(t2Sixes));

        setTextSafely(R.id.tvT1Fours, String.valueOf(t1Fours));
        setTextSafely(R.id.tvT2Fours, String.valueOf(t2Fours));

        // Boundary Runs (4*4 + 6*6)
        setTextSafely(R.id.tvT1BoundRuns, String.valueOf(helper.getBoundaryRuns(t1Fours, t1Sixes)));
        setTextSafely(R.id.tvT2BoundRuns, String.valueOf(helper.getBoundaryRuns(t2Fours, t2Sixes)));

        // Dot ball % (মোট বলের সাপেক্ষে ডট বল)
        int balls1 = (runs1 != null && runs1.size() > 1) ? (runs1.size() - 1) * 6 : 0;
        int balls2 = (runs2 != null && runs2.size() > 1) ? (runs2.size() - 1) * 6 : 0;
        setTextSafely(R.id.tvT1Dots, helper.getDotBallPercent(t1Dots, balls1));
        setTextSafely(R.id.tvT2Dots, helper.getDotBallPercent(t2Dots, balls2));

        setTextSafely(R.id.tvT1Extras, String.valueOf(t1Extras));
        setTextSafely(R.id.tvT2Extras, String.valueOf(t2Extras));
    }

    private void setTextSafely(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text);
    }
}

