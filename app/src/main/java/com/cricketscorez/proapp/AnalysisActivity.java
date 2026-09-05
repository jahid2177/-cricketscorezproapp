package com.cricketscorez.proapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;

public class AnalysisActivity extends Activity {

    private TextView tvHeader;
    private ListView listAnalysis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        tvHeader      = findViewById(R.id.tvTeam1Name);
        listAnalysis  = findViewById(R.id.listAnalysis);

        ImageView btnBack = findViewById(R.id.btnBackAnalysis);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        String t1 = getIntent().getStringExtra("TEAM_1_NAME");
        String t2 = getIntent().getStringExtra("TEAM_2_NAME");
        ArrayList<Integer> inn1 = getIntent().getIntegerArrayListExtra("INN1_DATA");
        ArrayList<Integer> inn2 = getIntent().getIntegerArrayListExtra("INN2_DATA");

        // Header
        if (t1 != null && t2 != null) {
            tvHeader.setText(t1 + " vs " + t2);
        }

        // Legend
        TextView tvLeg1 = findViewById(R.id.tvLegendTeam1);
        TextView tvLeg2 = findViewById(R.id.tvLegendTeam2);
        TextView tvCol1 = findViewById(R.id.tvColTeam1);
        TextView tvCol2 = findViewById(R.id.tvColTeam2);
        if (t1 != null) { tvLeg1.setText(t1); tvCol1.setText(t1.toUpperCase()); }
        if (t2 != null) { tvLeg2.setText(t2); tvCol2.setText(t2.toUpperCase()); }

        // Build over-by-over data rows
        final ArrayList<int[]> rows = new ArrayList<>();
        int maxOvers = Math.max(inn1 != null ? inn1.size() : 0,
                                inn2 != null ? inn2.size() : 0);

        for (int i = 1; i < maxOvers; i++) {
            int s1 = (inn1 != null && i < inn1.size()) ? inn1.get(i) : -1;
            int s2 = (inn2 != null && i < inn2.size()) ? inn2.get(i) : -1;
            rows.add(new int[]{i, s1, s2});
        }

        if (listAnalysis != null) {
            listAnalysis.setAdapter(new OverRowAdapter(this, rows));
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Custom Adapter — renders each over as a styled row
    // ─────────────────────────────────────────────────────────────────────
    private static class OverRowAdapter extends ArrayAdapter<int[]> {

        private final ArrayList<int[]> data;

        OverRowAdapter(Context ctx, ArrayList<int[]> data) {
            super(ctx, 0, data);
            this.data = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;

            if (convertView instanceof LinearLayout) {
                row = (LinearLayout) convertView;
                row.removeAllViews();
            } else {
                row = new LinearLayout(getContext());
            }

            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(28, 14, 28, 14);

            // Zebra stripe
            row.setBackgroundColor(position % 2 == 0
                    ? Color.parseColor("#FFFFFF")
                    : Color.parseColor("#F9FBF9"));

            int[] d  = data.get(position);
            int over = d[0];
            int s1   = d[1]; // -1 means no data
            int s2   = d[2];

            int diff = (s1 >= 0 && s2 >= 0) ? (s1 - s2) : Integer.MIN_VALUE;

            // ── Over number pill ─────────────────────────────
            TextView tvOver = new TextView(getContext());
            LinearLayout.LayoutParams overLp =
                    new LinearLayout.LayoutParams(0, -2, 1f);
            tvOver.setLayoutParams(overLp);
            tvOver.setText("Over " + over);
            tvOver.setTextSize(13f);
            tvOver.setTypeface(null, Typeface.BOLD);
            tvOver.setTextColor(Color.parseColor("#37474F"));
            tvOver.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(tvOver);

            // ── Team 1 score ─────────────────────────────────
            TextView tvS1 = new TextView(getContext());
            LinearLayout.LayoutParams s1Lp =
                    new LinearLayout.LayoutParams(0, -2, 1.5f);
            tvS1.setLayoutParams(s1Lp);
            tvS1.setText(s1 >= 0 ? String.valueOf(s1) : "—");
            tvS1.setTextSize(14f);
            tvS1.setTypeface(null, Typeface.BOLD);
            tvS1.setTextColor(Color.parseColor("#1B5E20"));
            tvS1.setGravity(Gravity.CENTER);
            row.addView(tvS1);

            // ── Team 2 score ─────────────────────────────────
            TextView tvS2 = new TextView(getContext());
            LinearLayout.LayoutParams s2Lp =
                    new LinearLayout.LayoutParams(0, -2, 1.5f);
            tvS2.setLayoutParams(s2Lp);
            tvS2.setText(s2 >= 0 ? String.valueOf(s2) : "—");
            tvS2.setTextSize(14f);
            tvS2.setTypeface(null, Typeface.BOLD);
            tvS2.setTextColor(Color.parseColor("#E65100"));
            tvS2.setGravity(Gravity.CENTER);
            row.addView(tvS2);

            // ── Diff badge ───────────────────────────────────
            TextView tvDiff = new TextView(getContext());
            LinearLayout.LayoutParams diffLp =
                    new LinearLayout.LayoutParams(0, -2, 1f);
            tvDiff.setLayoutParams(diffLp);

            if (diff == Integer.MIN_VALUE) {
                tvDiff.setText("—");
                tvDiff.setTextColor(Color.parseColor("#9E9E9E"));
            } else if (diff > 0) {
                tvDiff.setText("+" + diff);
                tvDiff.setTextColor(Color.parseColor("#1B5E20"));
            } else if (diff < 0) {
                tvDiff.setText(String.valueOf(diff));
                tvDiff.setTextColor(Color.parseColor("#C62828"));
            } else {
                tvDiff.setText("=");
                tvDiff.setTextColor(Color.parseColor("#546E7A"));
            }
            tvDiff.setTextSize(13f);
            tvDiff.setTypeface(null, Typeface.BOLD);
            tvDiff.setGravity(Gravity.CENTER);
            row.addView(tvDiff);

            return row;
        }
    }
}