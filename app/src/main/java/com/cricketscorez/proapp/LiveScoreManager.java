package com.cricketscorez.proapp;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ========================================================
 *  LiveScoreManager.java
 *  Cricket Scorez Pro — Firebase Realtime Live Score Push
 * ========================================================
 *
 *  কাজ কী করে:
 *  ━━━━━━━━━━━━
 *  প্রতিটি বলের পরে এই class-এর pushLiveScore() কল করা হয়।
 *  এটি Firebase Realtime Database-এ real-time score push করে।
 *  Viewer অ্যাপ/ওয়েবসাইট সেই data live দেখতে পাবে।
 *
 *  Firebase-এ Data Structure:
 *  ━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  live_matches/
 *    └── {matchId}/
 *          ├── match_info/       → দুই টিমের নাম, toss, overs
 *          ├── live_score/       → score, wickets, overs, CRR, RRR
 *          ├── batsmen/          → striker ও non-striker এর stats
 *          ├── bowler/           → current bowler-এর figures
 *          ├── this_over/        → চলতি over-এর ball-by-ball
 *          ├── last_ball/        → সর্বশেষ ball event
 *          ├── innings/          → 1st/2nd innings indicator
 *          └── status/           → Live / Innings Break / Completed
 */
public class LiveScoreManager {

    private static final String TAG = "LiveScoreManager";

    // Firebase-এ root path: live_matches/{matchId}/
    private static final String ROOT_PATH = "live_matches";

    // Singleton instance
    private static LiveScoreManager instance;

    private DatabaseReference matchRef;
    private String currentMatchId;
    private boolean isInitialized = false;

    // ──────────────────────────────────────────────
    // Singleton — getInstance()
    // ──────────────────────────────────────────────
    public static LiveScoreManager getInstance() {
        if (instance == null) {
            instance = new LiveScoreManager();
        }
        return instance;
    }

    private LiveScoreManager() {}

    // ──────────────────────────────────────────────
    // Step 1: Match শুরু হলে একবার init করুন
    // MainActivity.onCreate() থেকে কল হবে
    // ──────────────────────────────────────────────
    public void initMatch(MatchData matchData) {
        try {
            this.currentMatchId = matchData.matchId;

            // Firebase Realtime DB reference তৈরি
            matchRef = FirebaseDatabase.getInstance()
                    .getReference(ROOT_PATH)
                    .child(currentMatchId);

            isInitialized = true;

            // Match-এর static info একবার push করুন
            pushMatchInfo(matchData);

            Log.d(TAG, "LiveScoreManager initialized. Match ID: " + currentMatchId);

        } catch (Exception e) {
            Log.e(TAG, "initMatch() failed: " + e.getMessage());
            isInitialized = false;
        }
    }

    // ──────────────────────────────────────────────
    // Step 2: প্রতিটি ball event-এর পরে কল হবে
    // recordScoringEvent() এর শেষে ও recordWicketEvent()-এর পরে
    // ──────────────────────────────────────────────
    public void pushLiveScore(MatchData matchData, String strikerName, String nonStrikerName) {
        if (!isInitialized || matchRef == null) {
            Log.w(TAG, "pushLiveScore() skipped — not initialized");
            return;
        }

        try {
            // ── Live Score ──────────────────────────────────
            Map<String, Object> liveScore = new HashMap<>();
            liveScore.put("score", matchData.totalRuns);
            liveScore.put("wickets", matchData.totalWickets);
            liveScore.put("overs", matchData.getOversString());
            liveScore.put("crr", matchData.getCRR());
            liveScore.put("batting_team", matchData.getBattingTeamName());
            liveScore.put("bowling_team", matchData.isSecondInnings
                    ? matchData.teamBattingFirst : matchData.teamBattingSecond);
            liveScore.put("extras", matchData.getExtrasString());
            liveScore.put("partnership", matchData.getPartnershipString());
            liveScore.put("target", matchData.isSecondInnings ? matchData.targetRuns : 0);
            liveScore.put("timestamp", ServerValue.TIMESTAMP);

            // 2nd innings হলে RRR ও equation যোগ করুন
            if (matchData.isSecondInnings) {
                int runsNeeded = matchData.targetRuns - matchData.totalRuns;
                int totalBalls = Integer.parseInt(matchData.totalOvers) * 6;
                int ballsLeft  = totalBalls - matchData.ballsBowled;
                double rrr     = (ballsLeft > 0) ? (runsNeeded * 6.0) / ballsLeft : 0.0;
                liveScore.put("rrr", String.format("%.2f", Math.max(rrr, 0.0)));
                liveScore.put("match_equation", runsNeeded + " runs needed in " + ballsLeft + " balls");
            }

            matchRef.child("live_score").setValue(liveScore);

            // ── Batsmen Stats ───────────────────────────────
            Map<String, Object> batsmen = new HashMap<>();

            Map<String, Object> striker = new HashMap<>();
            striker.put("name", strikerName != null ? strikerName : "");
            striker.put("runs", matchData.strikerRuns);
            striker.put("balls", matchData.strikerBalls);
            striker.put("fours", matchData.striker4s);
            striker.put("sixes", matchData.striker6s);
            striker.put("sr", matchData.getStrikerSR());
            striker.put("on_strike", true);
            batsmen.put("striker", striker);

            Map<String, Object> nonStriker = new HashMap<>();
            nonStriker.put("name", nonStrikerName != null ? nonStrikerName : "");
            nonStriker.put("runs", matchData.nonStrikerRuns);
            nonStriker.put("balls", matchData.nonStrikerBalls);
            nonStriker.put("fours", matchData.nonStriker4s);
            nonStriker.put("sixes", matchData.nonStriker6s);
            nonStriker.put("sr", matchData.getNonStrikerSR());
            nonStriker.put("on_strike", false);
            batsmen.put("non_striker", nonStriker);

            matchRef.child("batsmen").setValue(batsmen);

            // ── Current Bowler ──────────────────────────────
            Map<String, Object> bowler = new HashMap<>();
            bowler.put("name", matchData.currentBowlerName != null ? matchData.currentBowlerName : "");
            bowler.put("figures", matchData.getBowlerFigures());
            bowler.put("runs", matchData.bowlerRuns);
            bowler.put("wickets", matchData.bowlerWickets);
            bowler.put("maidens", matchData.currentBowlerMaidens);
            bowler.put("economy", matchData.getBowlerER());
            matchRef.child("bowler").setValue(bowler);

            // ── This Over (ball-by-ball) ────────────────────
            Map<String, Object> thisOver = new HashMap<>();
            int ballIndex = 0;
            for (BallEvent event : matchData.currentOverBalls) {
                Map<String, Object> ball = new HashMap<>();
                ball.put("runs", event.runs);
                ball.put("is_wicket", event.isWicket);
                ball.put("is_extra", event.isExtra);
                ball.put("extra_type", event.extraType != null ? event.extraType : "");
                ball.put("is_legal", event.isLegalBall);
                thisOver.put("ball_" + ballIndex, ball);
                ballIndex++;
            }
            thisOver.put("ball_count", ballIndex);
            matchRef.child("this_over").setValue(thisOver);

            // ── Last Ball Event (notification-style) ────────
            if (!matchData.currentOverBalls.isEmpty()) {
                BallEvent lastBall = matchData.currentOverBalls.get(matchData.currentOverBalls.size() - 1);
                Map<String, Object> lastBallMap = new HashMap<>();
                String display;
                if (lastBall.isWicket)       display = "W";
                else if ("WD".equals(lastBall.extraType)) display = "Wd+" + (lastBall.runs - 1);
                else if ("NB".equals(lastBall.extraType)) display = "Nb+" + (lastBall.runs - 1);
                else if ("BYE".equals(lastBall.extraType))  display = "Bye+" + lastBall.runs;
                else if ("LB".equals(lastBall.extraType))   display = "Lb+" + lastBall.runs;
                else display = String.valueOf(lastBall.runs);
                lastBallMap.put("display", display);
                lastBallMap.put("is_wicket", lastBall.isWicket);
                lastBallMap.put("runs", lastBall.runs);
                lastBallMap.put("timestamp", ServerValue.TIMESTAMP);
                matchRef.child("last_ball").setValue(lastBallMap);
            }

            // ── Fall of Wickets (Viewer অ্যাপের FOW ট্যাবের জন্য) ─────
            matchRef.child("fall_of_wickets").setValue(matchData.fallOfWickets);

            // ── Completed Overs (Viewer অ্যাপের Over-by-Over ট্যাবের জন্য) ─
            matchRef.child("completed_overs").setValue(buildCompletedOversList(matchData));

            // ── Full Scorecard Snapshot (Viewer-এ "View Detailed Scorecard"
            //    খুললে সম্পূর্ণ ব্যাটিং/বোলিং হিস্ট্রি দেখানোর জন্য) ───────
            matchRef.child("full_scorecard").setValue(buildFullScorecardMap(matchData));

            // ── Innings Status ──────────────────────────────
            matchRef.child("innings").setValue(matchData.isSecondInnings ? 2 : 1);
            matchRef.child("status").setValue("Live");

        } catch (Exception e) {
            Log.e(TAG, "pushLiveScore() error: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // matchData.ballHistory থেকে সম্পন্ন হওয়া ওভারগুলো
    // (৬টি বৈধ বলের গ্রুপ) স্ট্রিং আকারে বানানো —
    // চলতি (অসম্পূর্ণ) ওভারটি এই লিস্টে থাকবে না।
    // ──────────────────────────────────────────────
    private List<String> buildCompletedOversList(MatchData matchData) {
        List<String> overs = new ArrayList<>();
        int legalCount = 0;
        StringBuilder sb = new StringBuilder();
        for (BallEvent event : matchData.ballHistory) {
            String label = event.isWicket ? "W" : (event.isExtra ? event.extraType : String.valueOf(event.runs));
            sb.append(label).append("  ");
            if (event.isLegalBall) {
                legalCount++;
                if (legalCount % 6 == 0) {
                    overs.add(sb.toString().trim());
                    sb = new StringBuilder();
                }
            }
        }
        return overs;
    }

    // ──────────────────────────────────────────────
    // ScorecardActivity-তে দেখানোর জন্য প্রয়োজনীয় সব ডাটা
    // (batting/bowling history, extras breakdown, ২য় ইনিংস
    // হলে ১ম ইনিংসের তথ্যও) একটা Map হিসেবে বানানো
    // ──────────────────────────────────────────────
    private Map<String, Object> buildFullScorecardMap(MatchData matchData) {
        Map<String, Object> body = new HashMap<>();
        body.put("total_runs", matchData.totalRuns);
        body.put("total_wickets", matchData.totalWickets);
        body.put("current_overs", matchData.currentOvers);
        body.put("current_balls", matchData.currentBalls);
        body.put("balls_bowled", matchData.ballsBowled);
        body.put("total_overs", matchData.totalOvers);
        body.put("is_second_innings", matchData.isSecondInnings);
        body.put("target_runs", matchData.targetRuns);
        body.put("match_status", matchData.matchStatus);
        body.put("match_result", matchData.matchResult);
        body.put("team1_name", matchData.team1Name);
        body.put("team2_name", matchData.team2Name);
        body.put("team_batting_first", matchData.teamBattingFirst);
        body.put("team_batting_second", matchData.teamBattingSecond);
        body.put("toss_message", matchData.tossMessage);
        body.put("extra_wide", matchData.extraWide);
        body.put("extra_no_ball", matchData.extraNoBall);
        body.put("extra_byes", matchData.extraByes);
        body.put("extra_leg_byes", matchData.extraLegByes);
        body.put("extra_penalty", matchData.extraPenalty);
        body.put("batsman_history", matchData.getAllBattingStats());
        body.put("bowler_history", matchData.getAllBowlingStats());
        body.put("fall_of_wickets", matchData.fallOfWickets);

        if (matchData.isSecondInnings) {
            body.put("score_inn1", matchData.scoreInn1);
            body.put("overs_inn1", matchData.oversInn1);
            body.put("extras_inn1", matchData.extrasInn1);
            body.put("batsman_history_inn1", matchData.batsmanHistoryInn1);
            body.put("bowler_history_inn1", matchData.bowlerHistoryInn1);
            body.put("fall_of_wickets_inn1", matchData.fallOfWicketsInn1);
        }
        return body;
    }

    // ──────────────────────────────────────────────
    // Match-এর static info: একবারই push হয়
    // ──────────────────────────────────────────────
    private void pushMatchInfo(MatchData matchData) {
        if (matchRef == null) return;
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("team1", matchData.team1Name);
            info.put("team2", matchData.team2Name);
            info.put("total_overs", matchData.totalOvers);
            info.put("toss_info", matchData.tossMessage);
            info.put("batting_first", matchData.teamBattingFirst);
            info.put("bowling_first", matchData.teamBattingSecond);
            info.put("match_id", matchData.matchId);
            info.put("match_date", matchData.matchDate);
            info.put("created_at", ServerValue.TIMESTAMP);
            matchRef.child("match_info").setValue(info);
        } catch (Exception e) {
            Log.e(TAG, "pushMatchInfo() error: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 2nd Innings শুরু হলে কল করুন
    // MainActivity-এ REQUEST_CODE_SECOND_INNINGS handler-এ
    // ──────────────────────────────────────────────
    public void pushInningsBreak(MatchData matchData) {
        if (!isInitialized || matchRef == null) return;
        try {
            Map<String, Object> breakInfo = new HashMap<>();
            breakInfo.put("first_innings_score", matchData.scoreInn1);
            breakInfo.put("first_innings_overs", matchData.oversInn1);
            breakInfo.put("target", matchData.targetRuns);
            breakInfo.put("timestamp", ServerValue.TIMESTAMP);
            matchRef.child("innings_break").setValue(breakInfo);
            matchRef.child("status").setValue("Innings Break");
            matchRef.child("innings").setValue(2);
        } catch (Exception e) {
            Log.e(TAG, "pushInningsBreak() error: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // Match শেষ হলে কল করুন
    // showMatchResultDialog() এ
    // ──────────────────────────────────────────────
    public void pushMatchResult(MatchData matchData, String resultMessage) {
        if (!isInitialized || matchRef == null) return;
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("result_text", resultMessage);
            result.put("team1_score", matchData.scoreInn1 != null ? matchData.scoreInn1 : "");
            result.put("team2_score", matchData.getScoreString());
            result.put("timestamp", ServerValue.TIMESTAMP);
            matchRef.child("match_result").setValue(result);

            // ✅ FIX: ম্যাচ Completed মার্ক করার ঠিক আগে "this_over"-এর একটা final,
            // নির্ভুল স্ন্যাপশট আবার পুশ করা হচ্ছে — যাতে ম্যাচ শেষ হওয়ার সময় যদি কোনো
            // মাঝপথের/স্টেল স্টেট (যেমন undo বা correction-এর সময়কার) আগে থেকেই
            // Firebase-এ পুশ হয়ে থাকে, সেটা এই চূড়ান্ত সঠিক ডাটা দিয়ে ওভাররাইট হয়ে যায়।
            // matchData.currentOverBalls সবসময় সত্যিকারের ballHistory থেকে
            // recalculateCurrentOverBalls() দিয়ে হিসাব করা — তাই এটাই সবচেয়ে নির্ভরযোগ্য উৎস।
            Map<String, Object> finalOver = new HashMap<>();
            int idx = 0;
            for (BallEvent event : matchData.currentOverBalls) {
                Map<String, Object> ball = new HashMap<>();
                ball.put("runs", event.runs);
                ball.put("is_wicket", event.isWicket);
                ball.put("is_extra", event.isExtra);
                ball.put("extra_type", event.extraType != null ? event.extraType : "");
                ball.put("is_legal", event.isLegalBall);
                finalOver.put("ball_" + idx, ball);
                idx++;
            }
            finalOver.put("ball_count", idx);
            matchRef.child("this_over").setValue(finalOver);

            // ✅ ম্যাচ শেষ হওয়ার সময় FOW / Over-by-Over / Full Scorecard-ও
            // শেষবারের মতো সঠিক ডাটা দিয়ে আপডেট করা হচ্ছে
            matchRef.child("fall_of_wickets").setValue(matchData.fallOfWickets);
            matchRef.child("completed_overs").setValue(buildCompletedOversList(matchData));
            matchRef.child("full_scorecard").setValue(buildFullScorecardMap(matchData));

            matchRef.child("status").setValue("Completed");
        } catch (Exception e) {
            Log.e(TAG, "pushMatchResult() error: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // Undo হলে কল করুন (same as pushLiveScore)
    // ──────────────────────────────────────────────
    public void pushUndoUpdate(MatchData matchData, String strikerName, String nonStrikerName) {
        // Undo-এর পরেও same live score push করলেই হবে
        pushLiveScore(matchData, strikerName, nonStrikerName);
    }

    // ──────────────────────────────────────────────
    // Match শেষে বা exit-এ cleanup
    // ──────────────────────────────────────────────
    public void cleanup() {
        isInitialized = false;
        matchRef = null;
        instance = null;
    }

    public boolean isReady() {
        return isInitialized && matchRef != null;
    }
}
