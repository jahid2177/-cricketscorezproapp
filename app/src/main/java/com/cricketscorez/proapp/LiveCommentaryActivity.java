package com.cricketscorez.proapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LiveCommentaryActivity extends Activity {

    LinearLayout containerCommentary;
    TextView tvMatchHeader, tvTossInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_commentary);

        containerCommentary = (LinearLayout) findViewById(R.id.containerCommentary);
        tvMatchHeader = (TextView) findViewById(R.id.tvMatchHeader);
        tvTossInfo = (TextView) findViewById(R.id.tvTossInfo); 
        ImageView btnBack = (ImageView) findViewById(R.id.btnBackCommentary);

        btnBack.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					finish();
				}
			});

        MatchData matchData = (MatchData) getIntent().getSerializableExtra("MATCH_DATA");

        if (matchData != null) {
            // ১. হেডার সেটআপ (টিম নাম এবং টস)
            tvMatchHeader.setText(matchData.teamBattingFirst + " vs " + matchData.teamBattingSecond);
            if (matchData.tossMessage != null) {
                tvTossInfo.setText(matchData.tossMessage);
            }

            containerCommentary.removeAllViews();

            // ২. দুই ইনিংসের ডাটা আলাদাভাবে প্রসেস করার লজিক
            if (matchData.isSecondInnings) {
                // ২য় ইনিংস চললে বর্তমান দলের ডাটা সবার উপরে দেখাবে
                addInningsSeparator("2nd Innings: " + matchData.teamBattingSecond);
                processBallData(matchData.ballHistory);

                // ১ম ইনিংসের ডাটা সেপারেটর দিয়ে নিচে দেখানো হবে
                addInningsSeparator("1st Innings: " + matchData.teamBattingFirst);
                processBallData(matchData.ballHistoryInn1); 
            } else {
                // শুধুমাত্র ১ম ইনিংস চললে
                addInningsSeparator("1st Innings: " + matchData.teamBattingFirst);
                processBallData(matchData.ballHistory);
            }
        }
    }

    // ৩. বল বাই বল ডাটাকে ওভারে ভাগ করে দেখানোর মেথড
    private void processBallData(ArrayList<BallEvent> allBalls) {
        if (allBalls == null || allBalls.isEmpty()) {
            addEmptyView();
            return;
        }

        List<List<BallEvent>> oversList = new ArrayList<List<BallEvent>>();
        List<BallEvent> currentOver = new ArrayList<BallEvent>();

        int legalBalls = 0;
        for (BallEvent ball : allBalls) {
            currentOver.add(ball);
            if (ball.isLegalBall) legalBalls++;
            if (legalBalls == 6) {
                oversList.add(new ArrayList<BallEvent>(currentOver));
                currentOver.clear();
                legalBalls = 0;
            }
        }
        if (!currentOver.isEmpty()) oversList.add(currentOver);

        // কমেন্টারিতে লেটেস্ট ওভার সবার উপরে দেখানোর জন্য রিভার্স করা
        Collections.reverse(oversList);
        int overIndex = oversList.size(); 

        for (List<BallEvent> over : oversList) {
            String bowler = (over.size() > 0 && over.get(0).bowlerName != null) ? over.get(0).bowlerName : "Bowler";
            int runsInOver = 0;
            int wktsInOver = 0;
            for(BallEvent b : over) {
                runsInOver += b.runs;
                if(b.isWicket) wktsInOver++;
            }
            addOverHeader(overIndex, bowler, runsInOver, wktsInOver);

            // ওভারের শেষ বলটি উপরে দেখানোর জন্য রিভার্স করা
            List<BallEvent> reversedBalls = new ArrayList<BallEvent>(over);
            Collections.reverse(reversedBalls);
            for (BallEvent ball : reversedBalls) { addBallView(ball); }
            overIndex--;
        }
    }

    private void addInningsSeparator(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(Color.parseColor("#004D40")); // ডার্ক গ্রিন থিম
        tv.setPadding(20, 15, 20, 15);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextSize(15);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 10, 0, 10);
        tv.setLayoutParams(params);

        containerCommentary.addView(tv);
    }

    private void addEmptyView() {
        TextView tv = new TextView(this);
        tv.setText("No ball data available for this innings.");
        tv.setPadding(0, 30, 0, 30);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.GRAY);
        containerCommentary.addView(tv);
    }

    private void addOverHeader(int overNum, String bowler, int runs, int wkts) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#424242"));
        header.setPadding(25, 15, 25, 15);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 15, 0, 0);
        header.setLayoutParams(params);

        TextView tvBowler = new TextView(this);
        tvBowler.setText("End of Over " + overNum + " (" + bowler + ")");
        tvBowler.setTextColor(Color.WHITE);
        tvBowler.setTextSize(13);
        tvBowler.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvStats = new TextView(this);
        tvStats.setText(runs + " runs, " + wkts + " wkts");
        tvStats.setTextColor(Color.parseColor("#FFD700")); // গোল্ডেন কালার
        tvStats.setTextSize(13);
        tvStats.setGravity(Gravity.END);
        tvStats.setTypeface(null, android.graphics.Typeface.BOLD);

        header.addView(tvBowler); 
        header.addView(tvStats);
        containerCommentary.addView(header);
    }

    private void addBallView(BallEvent ball) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(20, 25, 20, 25);
        row.setBackgroundColor(Color.WHITE);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView runCircle = new TextView(this);
        runCircle.setLayoutParams(new LinearLayout.LayoutParams(90, 90));
        runCircle.setGravity(Gravity.CENTER);
        runCircle.setTextColor(Color.WHITE);
        runCircle.setTextSize(14);
        runCircle.setTypeface(null, android.graphics.Typeface.BOLD);

        String runText = String.valueOf(ball.runs);
        int bgColor = Color.GRAY;

        // ৪. আউটপুট অনুযায়ী কালার সেটআপ
        if (ball.isWicket) { 
            runText = "W"; 
            bgColor = Color.RED; 
        } else if (ball.runs == 4) {
            bgColor = Color.parseColor("#FF9800"); // অরেঞ্জ
        } else if (ball.runs == 6) {
            bgColor = Color.parseColor("#4CAF50"); // গ্রিন
        } else if (ball.isExtra) {
            bgColor = Color.parseColor("#795548"); // ব্রাউন
            if ("WD".equals(ball.extraType)) {
                int bRuns = ball.runs - 1;
                runText = bRuns > 0 ? "Wd+" + bRuns : "Wd";
            } else if ("NB".equals(ball.extraType)) {
                int bRuns = ball.runs - 1;
                runText = bRuns > 0 ? "Nb+" + bRuns : "Nb";
            } else if ("BYE".equals(ball.extraType)) {
                runText = ball.runs > 1 ? ball.runs + "B" : "1B";
            } else if ("LB".equals(ball.extraType)) {
                runText = ball.runs > 1 ? ball.runs + "LB" : "1LB";
            } else {
                runText = "Ex";
            }
        } else {
            bgColor = Color.parseColor("#2196F3"); // ব্লু
        }

        runCircle.setText(runText);
        runCircle.setBackground(createCircleDrawable(bgColor));

        TextView tvComment = new TextView(this);
        tvComment.setPadding(35, 0, 0, 0);
        tvComment.setTextColor(Color.BLACK);
        tvComment.setTextSize(15);

        String bName = (ball.bowlerName != null) ? ball.bowlerName : "Bowler";
        String batName = (ball.batsmanName != null) ? ball.batsmanName : "Batsman";
        String outcome;
        if (ball.isWicket) {
            outcome = ", OUT!";
            if (ball.isExtra) outcome += " (" + ball.extraType + ")";
        } else if (ball.isExtra) {
            if ("WD".equals(ball.extraType)) {
                int bRuns = ball.runs - 1;
                outcome = bRuns > 0 ? ", Wide + " + bRuns + " run" + (bRuns > 1 ? "s" : "") + "." : ", Wide ball (1 run).";
            } else if ("NB".equals(ball.extraType)) {
                int bRuns = ball.runs - 1;
                if (bRuns == 4) outcome = ", No ball + FOUR off the bat!";
                else if (bRuns == 6) outcome = ", No ball + SIX off the bat!";
                else if (bRuns > 0) outcome = ", No ball + " + bRuns + " run" + (bRuns > 1 ? "s" : "") + " off the bat.";
                else outcome = ", No ball (1 run).";
            } else if ("BYE".equals(ball.extraType)) {
                outcome = ", " + ball.runs + " Bye run" + (ball.runs > 1 ? "s" : "") + ".";
            } else if ("LB".equals(ball.extraType)) {
                outcome = ", " + ball.runs + " Leg-bye run" + (ball.runs > 1 ? "s" : "") + ".";
            } else {
                outcome = ", " + ball.runs + " extras.";
            }
        } else if (ball.runs == 4) {
            outcome = ", FOUR!";
        } else if (ball.runs == 6) {
            outcome = ", SIX!";
        } else if (ball.runs == 0) {
            outcome = ", dot ball.";
        } else {
            outcome = ", " + ball.runs + " runs.";
        }

        tvComment.setText(bName + " to " + batName + outcome);

        row.addView(runCircle); 
        row.addView(tvComment);

        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
        line.setBackgroundColor(Color.parseColor("#EEEEEE"));

        containerCommentary.addView(row); 
        containerCommentary.addView(line);
    }

    private GradientDrawable createCircleDrawable(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);
        return shape;
    }
}

