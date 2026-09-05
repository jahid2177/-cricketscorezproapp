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
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OversDetailActivity extends Activity {

    LinearLayout containerOvers;

    // ── Design Tokens ────────────────────────────────────────────────────────
    private static final String C_BG         = "#F0F4F8";
    private static final String C_CARD       = "#FFFFFF";
    private static final String C_CARD_ALT   = "#F8FBFF";
    private static final String C_DIVIDER    = "#E8EDF2";
    private static final String C_OV_LABEL   = "#1B5E20";
    private static final String C_RUNS_CHIP  = "#E8F5E9";
    private static final String C_RUNS_TEXT  = "#2E7D32";
    private static final String C_PLAYER_TXT = "#37474F";
    private static final String C_GRAY_TXT   = "#90A4AE";

    // Ball palette
    private static final String B_SIX    = "#1B5E20";
    private static final String B_FOUR   = "#E65100";
    private static final String B_WICKET = "#B71C1C";
    private static final String B_WIDE   = "#546E7A";
    private static final String B_NOBALL = "#6A1B9A";
    private static final String B_BYE    = "#00695C";
    private static final String B_LEGBYE = "#00838F";
    private static final String B_DOT    = "#ECEFF1";
    private static final String B_ONE    = "#E3F2FD";
    private static final String B_TWO    = "#E8EAF6";
    private static final String B_THREE  = "#FFF8E1";
    private static final String B_FIVE   = "#FCE4EC";

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overs_detail);

        containerOvers = findViewById(R.id.containerOvers);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        MatchData matchData = (MatchData) getIntent().getSerializableExtra("MATCH_DATA");
        if (matchData != null) {
            displayOvers(matchData);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void displayOvers(MatchData data) {
        ArrayList<BallEvent> allBalls = data.ballHistory;
        if (allBalls == null || allBalls.isEmpty()) return;

        // Group into overs
        List<List<BallEvent>> oversList = new ArrayList<>();
        List<BallEvent> currentOver    = new ArrayList<>();
        int legalBalls = 0;

        for (BallEvent ball : allBalls) {
            currentOver.add(ball);
            if (ball.isLegalBall) legalBalls++;
            if (legalBalls == 6) {
                oversList.add(new ArrayList<>(currentOver));
                currentOver.clear();
                legalBalls = 0;
            }
        }
        if (!currentOver.isEmpty()) oversList.add(currentOver);

        Collections.reverse(oversList);

        int overNumber = oversList.size();
        boolean alternate = false;
        for (List<BallEvent> over : oversList) {
            addOverCard(over, overNumber, alternate);
            overNumber--;
            alternate = !alternate;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void addOverCard(List<BallEvent> balls, int overNum, boolean alternate) {

        // ── Outer wrapper ─────────────────────────────────────────────────────
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        boolean isFirstCard = (containerOvers.getChildCount() == 0);
        wLp.setMargins(dp(12), isFirstCard ? dp(14) : dp(6), dp(12), dp(6));
        wrapper.setLayoutParams(wLp);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor(alternate ? C_CARD_ALT : C_CARD));
        cardBg.setCornerRadius(dp(16));
        cardBg.setStroke(dp(1), Color.parseColor(C_DIVIDER));
        wrapper.setBackground(cardBg);
        wrapper.setPadding(0, 0, 0, 0);
        wrapper.setElevation(dp(2));

        // ── Green accent top strip ────────────────────────────────────────────
        View topStrip = new View(this);
        LinearLayout.LayoutParams stripLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(4));
        topStrip.setLayoutParams(stripLp);
        GradientDrawable stripBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#1B5E20"), Color.parseColor("#4CAF50")});
        stripBg.setCornerRadii(new float[]{dp(16), dp(16), dp(16), dp(16), 0, 0, 0, 0});
        topStrip.setBackground(stripBg);
        wrapper.addView(topStrip);

        // ── Body ──────────────────────────────────────────────────────────────
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setPadding(dp(14), dp(14), dp(14), dp(16));

        // ── LEFT: over label + runs chip ─────────────────────────────────────
        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        leftCol.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.WRAP_CONTENT);
        leftLp.setMargins(0, dp(2), dp(10), 0);
        leftCol.setLayoutParams(leftLp);

        // Over badge circle
        TextView tvOv = new TextView(this);
        tvOv.setText("Ov\n" + overNum);
        tvOv.setGravity(Gravity.CENTER);
        tvOv.setTextSize(13);
        tvOv.setTypeface(null, Typeface.BOLD);
        tvOv.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams ovLp = new LinearLayout.LayoutParams(dp(52), dp(52));
        tvOv.setLayoutParams(ovLp);
        GradientDrawable ovBg = new GradientDrawable();
        ovBg.setShape(GradientDrawable.OVAL);
        ovBg.setColor(Color.parseColor(C_OV_LABEL));
        tvOv.setBackground(ovBg);
        leftCol.addView(tvOv);

        // Runs chip
        int runsInOver = 0;
        int wktInOver  = 0;
        for (BallEvent b : balls) {
            runsInOver += b.runs;
            if (b.isWicket) wktInOver++;
        }
        TextView tvRuns = new TextView(this);
        tvRuns.setText(runsInOver + "R" + (wktInOver > 0 ? " · " + wktInOver + "W" : ""));
        tvRuns.setGravity(Gravity.CENTER);
        tvRuns.setTextSize(10.5f);
        tvRuns.setTypeface(null, Typeface.BOLD);
        tvRuns.setTextColor(Color.parseColor(wktInOver > 0 ? "#B71C1C" : C_RUNS_TEXT));
        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        chipLp.setMargins(0, dp(6), 0, 0);
        chipLp.gravity = Gravity.CENTER_HORIZONTAL;
        tvRuns.setLayoutParams(chipLp);
        tvRuns.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setCornerRadius(dp(20));
        chipBg.setColor(Color.parseColor(wktInOver > 0 ? "#FFEBEE" : C_RUNS_CHIP));
        tvRuns.setBackground(chipBg);
        leftCol.addView(tvRuns);

        body.addView(leftCol);

        // ── Vertical divider ─────────────────────────────────────────────────
        View vDiv = new View(this);
        LinearLayout.LayoutParams vdLp = new LinearLayout.LayoutParams(dp(1), LinearLayout.LayoutParams.MATCH_PARENT);
        vdLp.setMargins(0, dp(2), dp(12), dp(2));
        vDiv.setLayoutParams(vdLp);
        vDiv.setBackgroundColor(Color.parseColor(C_DIVIDER));
        body.addView(vDiv);

        // ── RIGHT: bowler → batsman + balls row ──────────────────────────────
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // Bowler → Batsman header
        String bowlerName  = "";
        String batsmanName = "";
        for (BallEvent b : balls) {
            if (b.bowlerName  != null && !b.bowlerName.isEmpty())  bowlerName  = b.bowlerName;
            if (b.batsmanName != null && !b.batsmanName.isEmpty()) batsmanName = b.batsmanName;
            if (!bowlerName.isEmpty() && !batsmanName.isEmpty()) break;
        }

        if (!bowlerName.isEmpty() || !batsmanName.isEmpty()) {
            LinearLayout playerRow = new LinearLayout(this);
            playerRow.setOrientation(LinearLayout.HORIZONTAL);
            playerRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams prLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            prLp.setMargins(0, 0, 0, dp(8));
            playerRow.setLayoutParams(prLp);

            if (!bowlerName.isEmpty()) {
                playerRow.addView(makeTag(bowlerName, "#E8F5E9", "#1B5E20"));
            }
            if (!bowlerName.isEmpty() && !batsmanName.isEmpty()) {
                TextView tvArrow = new TextView(this);
                tvArrow.setText(" → ");
                tvArrow.setTextSize(12);
                tvArrow.setTextColor(Color.parseColor(C_GRAY_TXT));
                playerRow.addView(tvArrow);
            }
            if (!batsmanName.isEmpty()) {
                playerRow.addView(makeTag(batsmanName, "#E3F2FD", "#1565C0"));
            }
            rightCol.addView(playerRow);
        }

        // ── Balls row — ✅ FIX: HorizontalScrollView দিয়ে wrap করা হয়েছে ──────
        // No Ball / Wide এর কারণে over লম্বা হলে বামদিকে scroll করে দেখা যাবে
        LinearLayout ballsRow = new LinearLayout(this);
        ballsRow.setOrientation(LinearLayout.HORIZONTAL);
        ballsRow.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

        int ballCount = 0;
        for (BallEvent ball : balls) {
            ballsRow.addView(createBallView(ball, ballCount));
            ballCount++;
        }

        // HorizontalScrollView এ ballsRow রাখা হচ্ছে
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false); // scrollbar লুকানো — clean look
        LinearLayout.LayoutParams hsvLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hsv.setLayoutParams(hsvLp);
        hsv.addView(ballsRow);
        rightCol.addView(hsv);

        // ── Commentary text row ───────────────────────────────────────────────
        LinearLayout commentRow = new LinearLayout(this);
        commentRow.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams crLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        crLp.setMargins(0, dp(6), 0, 0);
        commentRow.setLayoutParams(crLp);

        for (BallEvent ball : balls) {
            String comment = getBallComment(ball);
            if (!comment.isEmpty()) {
                TextView tvComment = new TextView(this);
                tvComment.setText(comment);
                tvComment.setTextSize(10);
                tvComment.setTextColor(Color.parseColor(
                        ball.isWicket ? "#B71C1C" : C_GRAY_TXT));
                tvComment.setTypeface(null, ball.isWicket ? Typeface.BOLD : Typeface.ITALIC);
                LinearLayout.LayoutParams commentLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                commentLp.setMargins(0, dp(2), 0, 0);
                tvComment.setLayoutParams(commentLp);
                commentRow.addView(tvComment);
            }
        }

        if (commentRow.getChildCount() > 0) rightCol.addView(commentRow);

        body.addView(rightCol);
        wrapper.addView(body);

        LinearLayout.LayoutParams wrapperLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        wrapperLp.setMargins(dp(12), dp(6), dp(12), dp(4));
        wrapper.setLayoutParams(wrapperLp);

        containerOvers.addView(wrapper);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private TextView makeTag(String text, String bgColor, String textColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(10.5f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor(textColor));
        tv.setPadding(dp(7), dp(2), dp(7), dp(2));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(Color.parseColor(bgColor));
        tv.setBackground(bg);
        return tv;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private View createBallView(BallEvent ball, int index) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        wLp.setMargins(0, 0, dp(8), 0);
        wrapper.setLayoutParams(wLp);

        TextView tv = new TextView(this);
        int size = dp(38);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(11);
        tv.setTypeface(null, Typeface.BOLD);

        String text;
        int    fillColor;
        int    textColor;
        boolean hasBorder  = false;
        int    borderColor = Color.parseColor("#CFD8DC");

        if (ball.isWicket && ball.isExtra) {
            text      = "W+";
            fillColor = Color.parseColor(B_WICKET);
            textColor = Color.WHITE;
        } else if (ball.isWicket) {
            text      = "W";
            fillColor = Color.parseColor(B_WICKET);
            textColor = Color.WHITE;
        } else if (ball.isExtra) {
            String et = ball.extraType != null ? ball.extraType : "";
            int bRuns = ball.runs - (et.equals("WD") || et.equals("NB") ? 1 : 0);
            switch (et) {
                case "WD":
                    text = bRuns > 0 ? "wd+" + bRuns : "wd";
                    fillColor = Color.parseColor(B_WIDE);
                    break;
                case "NB":
                    text = bRuns > 0 ? "nb+" + bRuns : "nb";
                    fillColor = Color.parseColor(B_NOBALL);
                    break;
                case "BYE":
                    text = ball.runs > 1 ? ball.runs + "b" : "1b";
                    fillColor = Color.parseColor(B_BYE);
                    break;
                case "LB":
                    text = ball.runs > 1 ? ball.runs + "lb" : "1lb";
                    fillColor = Color.parseColor(B_LEGBYE);
                    break;
                default:
                    text = "e";
                    fillColor = Color.GRAY;
            }
            textColor = Color.WHITE;
        } else {
            switch (ball.runs) {
                case 0:
                    text = "•";
                    fillColor = Color.parseColor(B_DOT);
                    textColor = Color.parseColor("#78909C");
                    hasBorder = true;
                    break;
                case 1:
                    text = "1";
                    fillColor = Color.parseColor(B_ONE);
                    textColor = Color.parseColor("#1565C0");
                    hasBorder = true;
                    borderColor = Color.parseColor("#BBDEFB");
                    break;
                case 2:
                    text = "2";
                    fillColor = Color.parseColor(B_TWO);
                    textColor = Color.parseColor("#283593");
                    hasBorder = true;
                    borderColor = Color.parseColor("#C5CAE9");
                    break;
                case 3:
                    text = "3";
                    fillColor = Color.parseColor(B_THREE);
                    textColor = Color.parseColor("#E65100");
                    hasBorder = true;
                    borderColor = Color.parseColor("#FFE0B2");
                    break;
                case 4:
                    text = "4";
                    fillColor = Color.parseColor(B_FOUR);
                    textColor = Color.WHITE;
                    break;
                case 5:
                    text = "5";
                    fillColor = Color.parseColor(B_FIVE);
                    textColor = Color.parseColor("#880E4F");
                    hasBorder = true;
                    borderColor = Color.parseColor("#F8BBD0");
                    break;
                case 6:
                    text = "6";
                    fillColor = Color.parseColor(B_SIX);
                    textColor = Color.WHITE;
                    break;
                default:
                    text = String.valueOf(ball.runs);
                    fillColor = Color.GRAY;
                    textColor = Color.WHITE;
            }
        }

        tv.setText(text);
        tv.setTextColor(textColor);

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(fillColor);
        if (hasBorder) circle.setStroke(dp(1), borderColor);
        tv.setBackground(circle);

        wrapper.addView(tv);
        return wrapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private String getBallComment(BallEvent ball) {
        if (ball.isWicket) {
            String who = (ball.batsmanName != null && !ball.batsmanName.isEmpty())
                    ? ball.batsmanName + " out!" : "Wicket!";
            return "🎯 " + who;
        }
        if (ball.runs == 6) return "🔥 Six!";
        if (ball.runs == 4) return "🏏 Four!";
        return "";
    }

    // ─────────────────────────────────────────────────────────────────────────
    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }
}
