package com.cricketscorez.proapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

	public class CompareActivity extends Activity {

    private String team1, team2, totalOvers;

    private ArrayList<Integer> inn1Data, inn2Data;
    private ArrayList<Integer> inn1Wickets, inn2Wickets;

    // Scoring Breakdown stats
    private int t1Sixes, t1Fours, t1Dots, t1Extras;
    private int t2Sixes, t2Fours, t2Dots, t2Extras;

    @Override
		protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);

        TextView tvHeader = findViewById(R.id.tvCompareHeader);
        ImageView btnBack = findViewById(R.id.btnBackCompare);

        LinearLayout btnRunRate  = findViewById(R.id.btnRunRateGraph);
        LinearLayout btnBarChart = findViewById(R.id.btnBarChart);
        LinearLayout btnWormGraph = findViewById(R.id.btnWormGraph);
        LinearLayout btnBreakdown = findViewById(R.id.btnScoringBreakdown);

        Intent intent = getIntent();

        // 🔥 TEAM INFO রিসিভ
        team1 = intent.getStringExtra("TEAM_1");
        team2 = intent.getStringExtra("TEAM_2");
        totalOvers = intent.getStringExtra("TOTAL_OVERS");

        // 🔥 RUN & WICKET DATA রিসিভ
        inn1Data = intent.getIntegerArrayListExtra("INN1_DATA");
        inn2Data = intent.getIntegerArrayListExtra("INN2_DATA");
        inn1Wickets = intent.getIntegerArrayListExtra("INN1_WICKET_DATA");
        inn2Wickets = intent.getIntegerArrayListExtra("INN2_WICKET_DATA");

        // 🔥 SCORING BREAKDOWN STATS রিসিভ
        t1Sixes  = intent.getIntExtra("T1_SIXES", 0);
        t1Fours  = intent.getIntExtra("T1_FOURS", 0);
        t1Dots   = intent.getIntExtra("T1_DOTS", 0);
        t1Extras = intent.getIntExtra("T1_EXTRAS", 0);

        t2Sixes  = intent.getIntExtra("T2_SIXES", 0);
        t2Fours  = intent.getIntExtra("T2_FOURS", 0);
        t2Dots   = intent.getIntExtra("T2_DOTS", 0);
        t2Extras = intent.getIntExtra("T2_EXTRAS", 0);

			if (team1 != null && team2 != null) {
		tvHeader.setText(team1 + " vs " + team2);
        }

        // 🔙 BACK বাটন লজিক
				btnBack.setOnClickListener(new View.OnClickListener() {
				@Override
					public void onClick(View v) {
                finish();
            }
        });

        // 📈 ১. Run Rate Line Graph (Smooth Bezier with Wicket markers)
				btnRunRate.setOnClickListener(new View.OnClickListener() {
				@Override
					public void onClick(View v) {
                if (validateData()) {
			openActivity(RunRateGraphActivity.class);
		}
		}
        });

				// 📊 ২. Runs Per Over Bar Chart (Grouped bars with per-over calculation)
					btnBarChart.setOnClickListener(new View.OnClickListener() {
				@Override
            public void onClick(View v) {
		if (validateData()) {
		openActivity(BarChartActivity.class);
		}
				}
				});

				// 🪱 ৩. Dynamic Worm Graph (Cumulative scoring comparison)
			btnWormGraph.setOnClickListener(new View.OnClickListener() {
		@Override
		public void onClick(View v) {
		if (validateData()) {
				openActivity(WormGraphActivity.class);
                }
					}
				});

	// 📋 ৪. Scoring Breakdown (Phase-wise Analysis & Boundary Stats)
	btnBreakdown.setOnClickListener(new View.OnClickListener() {
	@Override
	public void onClick(View v) {
		openActivity(ScoringBreakdownActivity.class);
		}
        });
		}

		// 🔥 নতুন ফিচার: ডাটা ভ্যালিডেশন (ম্যাচ শুরু না হলে গ্রাফ ওপেন হবে না)
		private boolean validateData() {
        if (inn1Data == null || inn1Data.size() < 2) {
		Toast.makeText(this, "গ্রাফ দেখানোর জন্য পর্যাপ্ত ডাটা নেই!", Toast.LENGTH_SHORT).show();
		return false;
        }
        return true;
		}

		// 🔥 COMMON METHOD – ডাটা ফরোয়ার্ডিং লজিক
		private void openActivity(Class<?> activityClass) {
        Intent i = new Intent(CompareActivity.this, activityClass);

        // কমন ম্যাচ ইনফো
        i.putExtra("TEAM_1", team1);
        i.putExtra("TEAM_2", team2);
        i.putExtra("TOTAL_OVERS", totalOvers);

        // রান এবং উইকেট ডাটা (একই Key ব্যবহার করা হয়েছে যা গ্রাফ ফাইলে রিসিভ করা হবে)
        i.putIntegerArrayListExtra("INN1_DATA", inn1Data);
        i.putIntegerArrayListExtra("INN2_DATA", inn2Data);
	i.putIntegerArrayListExtra("INN1_WICKET_DATA", inn1Wickets);
i.putIntegerArrayListExtra("INN2_WICKET_DATA", inn2Wickets);

        // স্কোরিং ব্রেকডাউন স্ট্যাটাস
        i.putExtra("T1_SIXES", t1Sixes);
        i.putExtra("T1_FOURS", t1Fours);
        i.putExtra("T1_DOTS", t1Dots);
        i.putExtra("T1_EXTRAS", t1Extras);

        i.putExtra("T2_SIXES", t2Sixes);
        i.putExtra("T2_FOURS", t2Fours);
        i.putExtra("T2_DOTS", t2Dots);
        i.putExtra("T2_EXTRAS", t2Extras);

        startActivity(i);
    }
}

