package com.cricketscorez.proapp;

import android.app.Activity;
import android.os.Bundle;
import java.util.ArrayList;

public class RunRateGraphActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_rate_graph);

        // আইডি অবশ্যই XML-এর সাথে মিলতে হবে
        RunRateGraphView rrView = findViewById(R.id.customRunRateGraph);

        // ডাটা রিসিভ করা
        ArrayList<Integer> inn1Runs = getIntent().getIntegerArrayListExtra("INN1_DATA");
        ArrayList<Integer> inn1Wkts = getIntent().getIntegerArrayListExtra("INN1_WICKET_DATA");
        ArrayList<Integer> inn2Runs = getIntent().getIntegerArrayListExtra("INN2_DATA");
        ArrayList<Integer> inn2Wkts = getIntent().getIntegerArrayListExtra("INN2_WICKET_DATA");

        String t1 = getIntent().getStringExtra("TEAM_1");
        String t2 = getIntent().getStringExtra("TEAM_2");
        String oversStr = getIntent().getStringExtra("TOTAL_OVERS");

        int totalOvers = 20;
        try { if(oversStr != null) totalOvers = Integer.parseInt(oversStr); } catch (Exception e) {}

        // কাস্টম ভিউতে ডাটা পাস করা
        if (rrView != null) {
            rrView.setData(inn1Runs, inn1Wkts, inn2Runs, inn2Wkts, t1, t2, totalOvers);
        }
    }
}

