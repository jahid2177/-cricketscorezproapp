package com.cricketscorez.proapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MatchData implements Serializable {
    private static final long serialVersionUID = 1L;

    // Tournament Automation Fields
    public boolean isTournamentMatch = false;
    public String tournamentMatchId = "";
    public String tournamentName = "";

    // Toss & Team Info
    public String tossMessage = "Toss info not available";
    public String teamBattingFirst = "";
    public String teamBattingSecond = "";

    // Commentary & Ball History Backups
    public ArrayList<String> commentaryList = new ArrayList<>(); 
    public ArrayList<String> commentaryListInn1 = new ArrayList<>(); 
    public ArrayList<BallEvent> ballHistory = new ArrayList<>(); 
    public ArrayList<BallEvent> ballHistoryInn1 = new ArrayList<>(); 

    // 1st Innings Data for Comparison
    public ArrayList<Integer> runRateInn1 = new ArrayList<>();
    public ArrayList<Integer> wicketsInn1 = new ArrayList<>();
    public int inn1Sixes = 0;
    public int inn1Fours = 0;
    public int inn1Dots = 0;
    public int inn1ExtrasTotal = 0;

    // Existing Variables
    public String matchId;
    public String matchDate;
    public String matchStatus = "Incomplete";
    public String matchResult = "";
    public String team1Name, team2Name, totalOvers;
    public int totalRuns = 0, totalWickets = 0, currentBalls = 0, currentOvers = 0, ballsBowled = 0; 
    public int extraWide = 0, extraNoBall = 0, extraByes = 0, extraLegByes = 0, extraPenalty = 0;
    // ✅ NEW: বোলিং টিমকে দেওয়া পেনাল্টি রান যদি তারা তখনো ব্যাট করেনি (এখনো ফিল্ডিং করছে),
    // তাহলে সেই রান এখানে জমা থাকে এবং তাদের ইনিংস শুরু হলে তাদের স্কোরে যোগ হয়।
    public int pendingBowlingTeamPenalty = 0;
    public String strikerName = "Striker", nonStrikerName = "Non-Striker";
    public int strikerRuns = 0, strikerBalls = 0, striker4s = 0, striker6s = 0;
    public int nonStrikerRuns = 0, nonStrikerBalls = 0, nonStriker4s = 0, nonStriker6s = 0;
    public String currentBowlerName = "Bowler";
    public int bowlerRuns = 0, bowlerWickets = 0, currentBowlerMaidens = 0, bowlerBallsBowled = 0; 
    public HashMap<String, int[]> bowlerRegistry = new HashMap<>();
    public HashMap<String, String> bowlerDisplayNames = new HashMap<>();
    public int partnershipRuns = 0, partnershipBalls = 0;
    public ArrayList<BallEvent> currentOverBalls = new ArrayList<>();
    public boolean isOverFinished = false, isMaidenOver = true;

    public ArrayList<String[]> batsmanHistory = new ArrayList<>(); 
    public ArrayList<String[]> bowlerHistory = new ArrayList<>();  
    public ArrayList<String> fallOfWickets = new ArrayList<>();    
    public boolean isSecondInnings = false;
    public int targetRuns = 0, firstInningsScore = 0;
    public ArrayList<String[]> batsmanHistoryInn1 = new ArrayList<>();
    public ArrayList<String[]> bowlerHistoryInn1 = new ArrayList<>();
    public ArrayList<String> fallOfWicketsInn1 = new ArrayList<>();
    public String extrasInn1 = "", scoreInn1 = "", oversInn1 = "";

    public MatchData(String t1, String t2, String overs) {
        this.team1Name = (t1 != null) ? t1 : "Team A";
        this.team2Name = (t2 != null) ? t2 : "Team B";
        this.totalOvers = (overs != null) ? overs : "20";

        this.teamBattingFirst = this.team1Name;
        this.teamBattingSecond = this.team2Name;
        this.tossMessage = this.team1Name + " elected to bat first"; 

        initHistoryData();
    }

    public void initHistoryData() {
        if (this.matchId == null) {
            this.matchId = String.valueOf(System.currentTimeMillis());
            this.matchDate = new java.text.SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale.getDefault()).format(new java.util.Date());
        }
    }

    public void addEvent(BallEvent event) {
        ensureUndoStack();
        undoStack.add(captureSnapshot());
        ballHistory.add(event);
        updateScore(event, 1); 
        recalculateCurrentOverBalls();
        saveCurrentBowlerStatsToRegistry();
    }

    // =====================================================================
    // ✅ UNDO সিস্টেম — সম্পূর্ণ Snapshot-ভিত্তিক Unlimited Undo
    // =====================================================================
    private static class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        int totalRuns, totalWickets, currentBalls, currentOvers, ballsBowled;
        int extraWide, extraNoBall, extraByes, extraLegByes, extraPenalty, pendingBowlingTeamPenalty;
        String strikerName, nonStrikerName;
        int strikerRuns, strikerBalls, striker4s, striker6s;
        int nonStrikerRuns, nonStrikerBalls, nonStriker4s, nonStriker6s;
        String currentBowlerName;
        int bowlerRuns, bowlerWickets, currentBowlerMaidens, bowlerBallsBowled;
        HashMap<String, int[]> bowlerRegistry;
        HashMap<String, String> bowlerDisplayNames;
        int partnershipRuns, partnershipBalls;
        ArrayList<String[]> batsmanHistory;
        ArrayList<String[]> bowlerHistory;
        ArrayList<String> fallOfWickets;
        ArrayList<String> commentaryList;
        ArrayList<BallEvent> ballHistory;
        ArrayList<Integer> manualSwapMarkers;

        // Innings & Match State
        boolean isSecondInnings;
        int targetRuns, firstInningsScore;
        String matchStatus, matchResult;
        String extrasInn1, scoreInn1, oversInn1;
        int inn1Sixes, inn1Fours, inn1Dots, inn1ExtrasTotal;
        ArrayList<String[]> batsmanHistoryInn1;
        ArrayList<String[]> bowlerHistoryInn1;
        ArrayList<String> fallOfWicketsInn1;
        ArrayList<String> commentaryListInn1;
        ArrayList<BallEvent> ballHistoryInn1;
        ArrayList<Integer> runRateInn1;
        ArrayList<Integer> wicketsInn1;
    }

    public ArrayList<Snapshot> undoStack = new ArrayList<>();

    private void ensureUndoStack() {
        if (undoStack == null) undoStack = new ArrayList<>();
    }

    public int getUndoCount() {
        ensureUndoStack();
        return undoStack.size();
    }

    private Snapshot captureSnapshot() {
        Snapshot s = new Snapshot();
        s.totalRuns = totalRuns; s.totalWickets = totalWickets; s.currentBalls = currentBalls;
        s.currentOvers = currentOvers; s.ballsBowled = ballsBowled;
        s.extraWide = extraWide; s.extraNoBall = extraNoBall; s.extraByes = extraByes;
        s.extraLegByes = extraLegByes; s.extraPenalty = extraPenalty;
        s.pendingBowlingTeamPenalty = pendingBowlingTeamPenalty;
        s.strikerName = strikerName; s.nonStrikerName = nonStrikerName;
        s.strikerRuns = strikerRuns; s.strikerBalls = strikerBalls; s.striker4s = striker4s; s.striker6s = striker6s;
        s.nonStrikerRuns = nonStrikerRuns; s.nonStrikerBalls = nonStrikerBalls; s.nonStriker4s = nonStriker4s; s.nonStriker6s = nonStriker6s;
        s.currentBowlerName = currentBowlerName;
        s.bowlerRuns = bowlerRuns; s.bowlerWickets = bowlerWickets;
        s.currentBowlerMaidens = currentBowlerMaidens; s.bowlerBallsBowled = bowlerBallsBowled;

        s.bowlerRegistry = new HashMap<>();
        for (Map.Entry<String, int[]> e : bowlerRegistry.entrySet()) {
            s.bowlerRegistry.put(e.getKey(), e.getValue().clone());
        }
        s.bowlerDisplayNames = new HashMap<>(bowlerDisplayNames);

        s.partnershipRuns = partnershipRuns; s.partnershipBalls = partnershipBalls;

        s.batsmanHistory = new ArrayList<>();
        for (String[] b : batsmanHistory) {
            s.batsmanHistory.add(b.clone());
        }

        s.bowlerHistory = new ArrayList<>();
        for (String[] b : bowlerHistory) {
            s.bowlerHistory.add(b.clone());
        }

        s.fallOfWickets = new ArrayList<>(fallOfWickets);
        s.commentaryList = new ArrayList<>(commentaryList);
        s.ballHistory = new ArrayList<>(ballHistory);
        s.manualSwapMarkers = new ArrayList<>(manualSwapMarkers);

        s.isSecondInnings = isSecondInnings;
        s.targetRuns = targetRuns;
        s.firstInningsScore = firstInningsScore;
        s.matchStatus = matchStatus;
        s.matchResult = matchResult;
        s.extrasInn1 = extrasInn1;
        s.scoreInn1 = scoreInn1;
        s.oversInn1 = oversInn1;
        s.inn1Sixes = inn1Sixes;
        s.inn1Fours = inn1Fours;
        s.inn1Dots = inn1Dots;
        s.inn1ExtrasTotal = inn1ExtrasTotal;

        s.batsmanHistoryInn1 = new ArrayList<>();
        for (String[] b : batsmanHistoryInn1) {
            s.batsmanHistoryInn1.add(b.clone());
        }

        s.bowlerHistoryInn1 = new ArrayList<>();
        for (String[] b : bowlerHistoryInn1) {
            s.bowlerHistoryInn1.add(b.clone());
        }

        s.fallOfWicketsInn1 = new ArrayList<>(fallOfWicketsInn1);
        s.commentaryListInn1 = new ArrayList<>(commentaryListInn1);
        s.ballHistoryInn1 = new ArrayList<>(ballHistoryInn1);
        s.runRateInn1 = new ArrayList<>(runRateInn1);
        s.wicketsInn1 = new ArrayList<>(wicketsInn1);

        return s;
    }

    private void restoreSnapshot(Snapshot s) {
        totalRuns = s.totalRuns; totalWickets = s.totalWickets; currentBalls = s.currentBalls;
        currentOvers = s.currentOvers; ballsBowled = s.ballsBowled;
        extraWide = s.extraWide; extraNoBall = s.extraNoBall; extraByes = s.extraByes;
        extraLegByes = s.extraLegByes; extraPenalty = s.extraPenalty;
        pendingBowlingTeamPenalty = s.pendingBowlingTeamPenalty;
        strikerName = s.strikerName; nonStrikerName = s.nonStrikerName;
        strikerRuns = s.strikerRuns; strikerBalls = s.strikerBalls; striker4s = s.striker4s; striker6s = s.striker6s;
        nonStrikerRuns = s.nonStrikerRuns; nonStrikerBalls = s.nonStrikerBalls; nonStriker4s = s.nonStriker4s; nonStriker6s = s.nonStriker6s;
        currentBowlerName = s.currentBowlerName;
        bowlerRuns = s.bowlerRuns; bowlerWickets = s.bowlerWickets;
        currentBowlerMaidens = s.currentBowlerMaidens; bowlerBallsBowled = s.bowlerBallsBowled;

        bowlerRegistry.clear();
        for (Map.Entry<String, int[]> e : s.bowlerRegistry.entrySet()) {
            bowlerRegistry.put(e.getKey(), e.getValue().clone());
        }
        bowlerDisplayNames.clear();
        bowlerDisplayNames.putAll(s.bowlerDisplayNames);

        partnershipRuns = s.partnershipRuns; partnershipBalls = s.partnershipBalls;

        batsmanHistory.clear();
        for (String[] b : s.batsmanHistory) {
            batsmanHistory.add(b.clone());
        }

        bowlerHistory.clear();
        for (String[] b : s.bowlerHistory) {
            bowlerHistory.add(b.clone());
        }

        fallOfWickets.clear(); fallOfWickets.addAll(s.fallOfWickets);
        commentaryList.clear(); commentaryList.addAll(s.commentaryList);
        ballHistory.clear(); ballHistory.addAll(s.ballHistory);
        manualSwapMarkers.clear(); manualSwapMarkers.addAll(s.manualSwapMarkers);

        isSecondInnings = s.isSecondInnings;
        targetRuns = s.targetRuns;
        firstInningsScore = s.firstInningsScore;
        matchStatus = s.matchStatus;
        matchResult = s.matchResult;
        extrasInn1 = s.extrasInn1;
        scoreInn1 = s.scoreInn1;
        oversInn1 = s.oversInn1;
        inn1Sixes = s.inn1Sixes;
        inn1Fours = s.inn1Fours;
        inn1Dots = s.inn1Dots;
        inn1ExtrasTotal = s.inn1ExtrasTotal;

        batsmanHistoryInn1.clear();
        for (String[] b : s.batsmanHistoryInn1) {
            batsmanHistoryInn1.add(b.clone());
        }

        bowlerHistoryInn1.clear();
        for (String[] b : s.bowlerHistoryInn1) {
            bowlerHistoryInn1.add(b.clone());
        }

        fallOfWicketsInn1.clear(); fallOfWicketsInn1.addAll(s.fallOfWicketsInn1);
        commentaryListInn1.clear(); commentaryListInn1.addAll(s.commentaryListInn1);
        ballHistoryInn1.clear(); ballHistoryInn1.addAll(s.ballHistoryInn1);
        runRateInn1.clear(); runRateInn1.addAll(s.runRateInn1);
        wicketsInn1.clear(); wicketsInn1.addAll(s.wicketsInn1);

        recalculateCurrentOverBalls();
    }

    public boolean canUndo() {
        ensureUndoStack();
        return !undoStack.isEmpty();
    }

    public void undoLastEvent() {
        ensureUndoStack();
        if (undoStack.isEmpty()) return;
        Snapshot last = undoStack.remove(undoStack.size() - 1);
        restoreSnapshot(last);
    }

    public void recalculateCurrentOverBalls() {
        currentOverBalls.clear();
        int legalBallsInOver = 0;
        for (int i = 0; i < ballHistory.size(); i++) {
            BallEvent ball = ballHistory.get(i);
            if (legalBallsInOver == 6) {
                currentOverBalls.clear();
                legalBallsInOver = 0;
            }
            currentOverBalls.add(ball);
            if (ball.isLegalBall) legalBallsInOver++;
        }
        isOverFinished = (legalBallsInOver == 6);
        // ✅ FIX: ICC নিয়মে Maiden Over মানে ওই ওভারে কোনো ধরনের রানই না হওয়া —
        // ব্যাটের রান, Wide/No-ball penalty, এমনকি Bye/Leg-bye সবকিছু ধরে।
        // আগে Bye/Leg-bye-এর রানকে বাদ দেওয়া হতো, ফলে কেউ শুধু bye/leg-bye
        // নিলেও ওভারটা ভুলভাবে maiden হিসেবে গণ্য হয়ে যেত।
        isMaidenOver = true;
        for (BallEvent b : currentOverBalls) {
            if (b.runs > 0) {
                isMaidenOver = false;
                break;
            }
        }
    }

    private void updateScore(BallEvent event, int multiplier) {
        totalRuns += event.runs * multiplier;
        if (event.isWicket) {
            totalWickets += multiplier;
            // ✅ FIX: আগে সব ধরনের আউটে (run out সহ) bowlerWickets বাড়তো,
            // যা ক্রিকেট নিয়মে ভুল — Run Out বোলারের wicket হিসেবে গণ্য হয় না।
            if (event.creditBowlerForWicket) { bowlerWickets += multiplier; }
        }
        if (event.isExtra) {
            if (event.extraType.equals("WD")) extraWide += event.runs * multiplier;
            // ✅ FIX: No-ball-এ পেনাল্টি শুধু ১ রান "nb" extra হিসেবে যায়;
            // বাকি (batter-এর হিট করা) রান ব্যাটসম্যানের একাউন্টে যাওয়া উচিত,
            // আগে পুরো finalRuns (penalty+batter) ভুলভাবে extraNoBall-এ যেত।
            if (event.extraType.equals("NB")) extraNoBall += 1 * multiplier;
            if (event.extraType.equals("BYE")) extraByes += event.runs * multiplier;
            if (event.extraType.equals("LB")) extraLegByes += event.runs * multiplier;
        }
        partnershipRuns += event.runs * multiplier;

        // ✅ FIX: No-ball legal ball না হওয়ায় (isLegalBall=false) আগে batter-এর
        // স্কোর/বাউন্ডারি ও bowler-এর রান কখনো যুক্ত হতো না — no-ball-এ batter
        // boundary মারলেও scorecard-এ তা দেখাতো না। তাই batter/bowler রান
        // আপডেট isLegalBall ব্লকের বাইরে নিয়ে এসে, no-ball-কে আলাদাভাবে হ্যান্ডেল
        // করা হলো — over/ball কাউন্ট শুধু legal ball-এই বাড়বে, কিন্তু রান-হিসাব
        // legal ball বা no-ball দুটোতেই হবে।
        boolean isNoBall = event.isExtra && event.extraType.equals("NB");

        boolean isByeOrLegBye = event.isExtra
                && (event.extraType.equals("BYE") || event.extraType.equals("LB"));

        if (!event.isExtra) {
            // সাধারণ রান (legal ball, কোনো extra না)
            strikerBalls += multiplier; strikerRuns += event.runs * multiplier;
            if (event.runs == 4) striker4s += multiplier;
            if (event.runs == 6) striker6s += multiplier;
            bowlerRuns += event.runs * multiplier;
        } else if (isNoBall) {
            // No-ball: ১ রান পেনাল্টি বাদ দিয়ে বাকিটা batter-এর রান/বাউন্ডারি
            int batterRuns = event.runs - 1;
            if (batterRuns < 0) batterRuns = 0;
            strikerRuns += batterRuns * multiplier;
            if (batterRuns == 4) striker4s += multiplier;
            if (batterRuns == 6) striker6s += multiplier;
            // no-ball legal ball না, তাই strikerBalls বাড়বে না (ক্রিকেট নিয়ম)
            bowlerRuns += event.runs * multiplier; // bowler পুরো (penalty+batter) রানের দায়ী
        } else if (isByeOrLegBye) {
            // ✅ FIX: Bye/Leg-bye একটা legal (fair) delivery — তাই ICC নিয়মে এটা
            // ব্যাটসম্যানের "balls faced" এ যোগ হওয়া উচিত, যদিও রানটা তার
            // ব্যক্তিগত স্কোরে যায় না (Extras-এ যায়)। আগে এখানে strikerBalls
            // একদমই বাড়তো না, ফলে Strike Rate ভুলভাবে বেশি দেখাতো।
            strikerBalls += multiplier;
        }
        // Wide/Bye/LegBye-এ batter-এর কোনো রান/বল যুক্ত হয় না (ক্রিকেট নিয়ম)।
        // Wide-এ পুরো রান bowler-এর নামে যায়, কিন্তু Bye/LegBye bowler-এর
        // বিরুদ্ধে রান হিসেবে গণ্য হয় না (ইকোনমি রেটে প্রভাব ফেলে না)।
        if (event.isExtra && event.extraType.equals("WD")) {
            bowlerRuns += event.runs * multiplier;
        }

        if (event.isLegalBall) {
            currentBalls += multiplier; ballsBowled += multiplier; partnershipBalls += multiplier; bowlerBallsBowled += multiplier; 
            if (currentBalls == 6) { 
                currentOvers += 1; currentBalls = 0; 
                if (isMaidenOver && multiplier == 1) {
                    currentBowlerMaidens += 1;
                    event.completedMaidenOver = true; // undo-র জন্য স্ন্যাপশট
                }
            } else if (currentBalls < 0) { 
                currentOvers -= 1; currentBalls = 5; 
                // ✅ FIX: আগে undo-তে maiden-over count কখনো কমতো না, কারণ
                // এই ব্রাঞ্চে কোনো decrement-লজিকই ছিল না। এখন event-এ
                // সেভ করা completedMaidenOver স্ন্যাপশট দিয়ে সঠিকভাবে কমানো হচ্ছে।
                if (multiplier == -1 && event.completedMaidenOver && currentBowlerMaidens > 0) {
                    currentBowlerMaidens -= 1;
                }
            }
        }

        // ✅ DEFENSIVE FIX: কোনো অজানা/অপ্রত্যাশিত undo-sequence-এও যেন
        // ব্যাটিং/পার্টনারশিপ স্ট্যাটস স্ক্রিনে negative দেখা না যায় — এটা
        // মূল swap-desync বাগটার (উপরে recordManualSwap দ্রষ্টব্য) আসল
        // ফিক্স না, কিন্তু একটা শেষ safety-net হিসেবে রাখা হলো।
        if (strikerRuns < 0) strikerRuns = 0;
        if (strikerBalls < 0) strikerBalls = 0;
        if (striker4s < 0) striker4s = 0;
        if (striker6s < 0) striker6s = 0;
        if (nonStrikerRuns < 0) nonStrikerRuns = 0;
        if (nonStrikerBalls < 0) nonStrikerBalls = 0;
        if (nonStriker4s < 0) nonStriker4s = 0;
        if (nonStriker6s < 0) nonStriker6s = 0;
        if (partnershipRuns < 0) partnershipRuns = 0;
        if (partnershipBalls < 0) partnershipBalls = 0;
        if (totalRuns < 0) totalRuns = 0;
        if (totalWickets < 0) totalWickets = 0;
    }

    public void switchBowler(String newName) {
        if (currentBowlerName != null && !currentBowlerName.equals("Bowler")) { saveCurrentBowlerStatsToRegistry(); }
        String key = newName.trim().toLowerCase(Locale.ROOT);
        this.currentBowlerName = newName; 
        if (bowlerRegistry.containsKey(key)) {
            int[] stats = bowlerRegistry.get(key);
            this.bowlerBallsBowled = stats[0]; 
            this.currentBowlerMaidens = stats.length > 1 ? stats[1] : 0;
            this.bowlerRuns = stats.length > 2 ? stats[2] : 0;
            this.bowlerWickets = stats.length > 3 ? stats[3] : 0;
        } else {
            this.bowlerBallsBowled = 0; this.currentBowlerMaidens = 0; this.bowlerRuns = 0; this.bowlerWickets = 0;
            bowlerDisplayNames.put(key, newName);
            saveCurrentBowlerStatsToRegistry();
        }
    }

    public void saveCurrentBowlerStatsToRegistry() {
        String key = currentBowlerName.trim().toLowerCase(Locale.ROOT);
        int[] stats = {bowlerBallsBowled, currentBowlerMaidens, bowlerRuns, bowlerWickets};
        bowlerRegistry.put(key, stats);
        bowlerDisplayNames.put(key, currentBowlerName);
    }

    public ArrayList<String> getBowlerSuggestions() {
        ArrayList<String> suggestions = new ArrayList<>();
        for (String key : bowlerDisplayNames.keySet()) {
            String name = bowlerDisplayNames.get(key);
            if (!name.equalsIgnoreCase(currentBowlerName)) {
                int[] s = bowlerRegistry.get(key);
                String figures = (s[0]/6) + "." + (s[0]%6) + "-" + s[1] + "-" + s[2] + "-" + s[3];
                suggestions.add(name + " (" + figures + ")");
            }
        }
        return suggestions;
    }

    public void retireStriker(boolean isHurt) {
        ensureUndoStack();
        undoStack.add(captureSnapshot());
        String status = isHurt ? "(Ret Hurt)" : "(Ret Out)";
        String sr = getBatsmanSR(strikerRuns, strikerBalls);
        batsmanHistory.add(new String[]{strikerName + " " + status, String.valueOf(strikerRuns), String.valueOf(strikerBalls), String.valueOf(striker4s), String.valueOf(striker6s), sr});
        if (!isHurt) {
            fallOfWickets.add(getScoreString() + " (" + strikerName + ", " + getOversString() + ")");
            totalWickets++;
        }
        // নাম ও stats reset — নতুন ব্যাটসম্যানের নাম MainActivity থেকে set হবে
        strikerRuns = 0; strikerBalls = 0; striker4s = 0; striker6s = 0;
        partnershipRuns = 0; partnershipBalls = 0;
    }

    // ✅ NEW: Non-striker retire করার জন্য
    public void retireNonStriker(boolean isHurt) {
        ensureUndoStack();
        undoStack.add(captureSnapshot());
        String status = isHurt ? "(Ret Hurt)" : "(Ret Out)";
        String sr = getBatsmanSR(nonStrikerRuns, nonStrikerBalls);
        batsmanHistory.add(new String[]{nonStrikerName + " " + status, String.valueOf(nonStrikerRuns), String.valueOf(nonStrikerBalls), String.valueOf(nonStriker4s), String.valueOf(nonStriker6s), sr});
        if (!isHurt) {
            fallOfWickets.add(getScoreString() + " (" + nonStrikerName + ", " + getOversString() + ")");
            totalWickets++;
        }
        nonStrikerRuns = 0; nonStrikerBalls = 0; nonStriker4s = 0; nonStriker6s = 0;
        partnershipRuns = 0; partnershipBalls = 0;
    }

    // ✅ NEW: Non-striker stats reset (নতুন ব্যাটসম্যান আসার পরে)
    public void resetNonStrikerStats() {
        nonStrikerRuns = 0; nonStrikerBalls = 0; nonStriker4s = 0; nonStriker6s = 0;
        partnershipRuns = 0; partnershipBalls = 0;
    }

    // ✅ FIX: আগে এই মেথড শুধু বর্তমান (ইনজুরড) বোলারের figures সেভ করে
    // 0 করে দিতো, কিন্তু নতুন বোলারকে সেট করতো না — এবং এর পরে যদি কেউ
    // switchBowler() কল করতো, সেটা আবার currentBowlerName (তখনো পুরোনো
    // বোলারের নাম) দিয়ে registry-তে শূন্য মান সেভ করে পুরোনো বোলারের আসল
    // ফিগার নষ্ট করে দিতো। এখন পুরো কাজ — পুরোনো বোলারের figures সেভ +
    // নতুন বোলারকে সেট করা — একটাই মেথডে অ্যাটমিকভাবে হচ্ছে, over মাঝপথে
    // থামলেও strike/over সংখ্যা স্পর্শ করা হচ্ছে না (এটা over-শেষ নয়)।
    public void replaceBowlerMidOver(String newBowlerName) {
        ensureUndoStack();
        undoStack.add(captureSnapshot());
        // পুরোনো (ইনজুরড) বোলারের এই ইনিংসের চূড়ান্ত figures সেভ করা হচ্ছে —
        // সে আর এই ইনিংসে বল করবে না।
        saveBowlerToHistory();
        saveCurrentBowlerStatsToRegistry();

        String key = newBowlerName.trim().toLowerCase(Locale.ROOT);
        this.currentBowlerName = newBowlerName;
        if (bowlerRegistry.containsKey(key)) {
            int[] stats = bowlerRegistry.get(key);
            this.bowlerBallsBowled   = stats[0];
            this.currentBowlerMaidens = stats.length > 1 ? stats[1] : 0;
            this.bowlerRuns          = stats.length > 2 ? stats[2] : 0;
            this.bowlerWickets       = stats.length > 3 ? stats[3] : 0;
        } else {
            this.bowlerBallsBowled = 0; this.currentBowlerMaidens = 0; this.bowlerRuns = 0; this.bowlerWickets = 0;
            bowlerDisplayNames.put(key, newBowlerName);
            saveCurrentBowlerStatsToRegistry();
        }
    }

    public void saveBatsman(String name, int r, int b, int f, int s) {
        if (name == null) name = "Unknown";
        String sr = getBatsmanSR(r, b);
        batsmanHistory.add(new String[]{name, String.valueOf(r), String.valueOf(b), String.valueOf(f), String.valueOf(s), sr});
    }

    public void saveBowler() { saveBowlerToHistory(); saveCurrentBowlerStatsToRegistry(); }

    private void saveBowlerToHistory() {
        String er = getBowlerER();
        bowlerHistory.add(new String[]{currentBowlerName, getBowlerFigures(), String.valueOf(currentBowlerMaidens), String.valueOf(bowlerRuns), String.valueOf(bowlerWickets), er});
    }

    public void startSecondInnings() {
        this.commentaryListInn1 = new ArrayList<>(this.commentaryList);
        this.ballHistoryInn1 = new ArrayList<>(this.ballHistory); 

        this.runRateInn1 = new ArrayList<>(getCumulativeRunsPerOver());
        this.wicketsInn1 = new ArrayList<>(getCumulativeWicketsPerOver());
        this.inn1Sixes = getSixesCount();
        this.inn1Fours = getFoursCount();
        this.inn1Dots = getDotsCount();
        this.inn1ExtrasTotal = getTotalExtras();

        saveBatsman(strikerName + "*", strikerRuns, strikerBalls, striker4s, striker6s);
        saveBatsman(nonStrikerName + "*", nonStrikerRuns, nonStrikerBalls, nonStriker4s, nonStriker6s);
        saveBowler();

        this.batsmanHistoryInn1.addAll(this.batsmanHistory);
        this.bowlerHistoryInn1.addAll(getAllBowlingStats());
        this.fallOfWicketsInn1.addAll(this.fallOfWickets);

        this.extrasInn1 = getExtrasString();
        this.scoreInn1 = getScoreString();
        this.oversInn1 = getOversString();
        this.isSecondInnings = true;
        this.firstInningsScore = this.totalRuns;
        this.targetRuns = this.totalRuns + 1;

        // ✅ FIX: ২য় ইনিংস শুরুর সময় আগে থেকে জমে থাকা "bowling team penalty"
        // (যা ১ম ইনিংসে তখনকার বোলিং টিমকে দেওয়া হয়েছিল, কিন্তু তারা তখনো
        // ব্যাট করেনি) — এখন তাদের ইনিংসের শুরুতেই স্কোরে যোগ হয়ে যাচ্ছে।
        this.totalRuns = pendingBowlingTeamPenalty;
        this.extraPenalty = pendingBowlingTeamPenalty;
        this.pendingBowlingTeamPenalty = 0;
        this.totalWickets = 0; this.currentBalls = 0; this.currentOvers = 0; this.ballsBowled = 0;
        this.extraWide = 0; this.extraNoBall = 0; this.extraByes = 0; this.extraLegByes = 0;
        this.strikerRuns = 0; this.strikerBalls = 0; this.striker4s = 0; this.striker6s = 0;
        this.nonStrikerRuns = 0; this.nonStrikerBalls = 0; this.nonStriker4s = 0; this.nonStriker6s = 0;
        this.bowlerRegistry.clear(); this.bowlerDisplayNames.clear();
        this.currentBowlerName = "Bowler"; this.bowlerRuns = 0; this.bowlerWickets = 0; this.currentBowlerMaidens = 0; this.bowlerBallsBowled = 0;
        this.partnershipRuns = 0; this.partnershipBalls = 0;
        this.currentOverBalls.clear(); 
        this.ballHistory.clear();
        this.commentaryList.clear();
        this.batsmanHistory.clear(); 
        this.bowlerHistory.clear(); 
        this.fallOfWickets.clear();
        this.isOverFinished = false; 
        this.isMaidenOver = true;

        // ✅ ১ম ইনিংসের undo history নতুন ইনিংসে বহন করা ঠিক নয় (তাহলে
        // Undo চাপলে ২য় ইনিংসের বল ১ম ইনিংসের স্টেটে ফিরে যেতে পারত)।
        ensureUndoStack();
        this.undoStack.clear();
        this.manualSwapMarkers.clear();
    }

    public boolean isInningsFinished() { try { return ballsBowled >= Integer.parseInt(totalOvers) * 6; } catch (Exception e) { return false; } }

    public ArrayList<String[]> getAllBattingStats() {
        ArrayList<String[]> fullList = new ArrayList<>(this.batsmanHistory);
        if (strikerName != null && !strikerName.isEmpty()) {
            String sr = getBatsmanSR(strikerRuns, strikerBalls);
            fullList.add(new String[]{strikerName + "*", String.valueOf(strikerRuns), String.valueOf(strikerBalls), String.valueOf(striker4s), String.valueOf(striker6s), sr});
        }
        if (nonStrikerName != null && !nonStrikerName.isEmpty()) {
            String sr = getBatsmanSR(nonStrikerRuns, nonStrikerBalls);
            fullList.add(new String[]{nonStrikerName + "*", String.valueOf(nonStrikerRuns), String.valueOf(nonStrikerBalls), String.valueOf(nonStriker4s), String.valueOf(nonStriker6s), sr});
        }
        return fullList;
    }

    public ArrayList<String[]> getAllBowlingStats() {
        ArrayList<String[]> list = new ArrayList<>();
        ArrayList<String> addedKeys = new ArrayList<>();
        for (String[] entry : bowlerHistory) {
            String key = entry[0].trim().toLowerCase(Locale.ROOT);
            if (!addedKeys.contains(key)) addedKeys.add(key);
        }
        if (currentBowlerName != null && !currentBowlerName.equals("Bowler")) {
            String key = currentBowlerName.trim().toLowerCase(Locale.ROOT);
            if (!addedKeys.contains(key)) { addedKeys.add(key); saveCurrentBowlerStatsToRegistry(); }
        }
        for (String key : addedKeys) {
            if (bowlerRegistry.containsKey(key)) {
                int[] s = bowlerRegistry.get(key);
                String name = bowlerDisplayNames.get(key);
                int balls = s[0];
                String overs = (balls / 6) + "." + (balls % 6);
                String er = (balls == 0) ? "0.00" : String.format(Locale.US, "%.2f", (double) s[2] * 6.0 / (balls == 0 ? 1 : balls));
                list.add(new String[]{name, overs, String.valueOf(s[1]), String.valueOf(s[2]), String.valueOf(s[3]), er});
            }
        }
        return list;
    }

    public ArrayList<Integer> getCumulativeRunsPerOver() { 
        ArrayList<Integer> list = new ArrayList<>(); 
        list.add(0); 
        int total = 0, balls = 0; 
        for(BallEvent e : ballHistory) { 
            total += e.runs; 
            if(e.isLegalBall) { 
                balls++; 
                if(balls%6==0) list.add(total); 
            } 
        } 
        if(balls%6!=0 && balls > 0) list.add(total); 
        return list; 
    }

    public ArrayList<Integer> getCumulativeWicketsPerOver() {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(0);
        int w = 0, balls = 0;
        for (BallEvent e : ballHistory) {
            if (e.isWicket) w++;
            if (e.isLegalBall) {
                balls++;
                if (balls % 6 == 0) list.add(w);
            }
        }
        if (balls % 6 != 0 && balls > 0) list.add(w);
        return list;
    }

    public int getSixesCount() { int count = 0; for (BallEvent e : ballHistory) if (e.runs == 6 && !e.isExtra) count++; return count; }
    public int getFoursCount() { int count = 0; for (BallEvent e : ballHistory) if (e.runs == 4 && !e.isExtra) count++; return count; }
    public int getDotsCount() { int count = 0; for (BallEvent e : ballHistory) if (e.runs == 0 && !e.isWicket && !e.isExtra) count++; return count; }
    public int getTotalExtras() { return extraWide + extraNoBall + extraByes + extraLegByes + extraPenalty; }

    public String getPhaseScore(ArrayList<Integer> runsList, ArrayList<Integer> wicketsList, int startOver, int endOver) {
        if (runsList == null || runsList.size() <= 1 || wicketsList == null || wicketsList.size() <= 1) return "0/0";
        int startIdx = Math.min(startOver - 1, runsList.size() - 1);
        int endIdx = Math.min(endOver, runsList.size() - 1);
        if (startIdx < 0) startIdx = 0;
        if (endIdx < startIdx) return "0/0";
        int phaseRuns = runsList.get(endIdx) - runsList.get(startIdx);
        int phaseWickets = wicketsList.get(endIdx) - wicketsList.get(startIdx);
        return phaseRuns + "/" + phaseWickets;
    }

    public int getBoundaryRuns(int fours, int sixes) { return (fours * 4) + (sixes * 6); }
    public String getDotBallPercent(int dots, int totalBalls) { if (totalBalls <= 0) return "0%"; return Math.round((dots * 100.0f) / totalBalls) + "%"; }
    public double getCurrentRunRate() { return ballsBowled == 0 ? 0.0 : (totalRuns * 6.0) / ballsBowled; }
    public void addPenaltyRuns(int runs) {
        if (runs > 0) {
            ensureUndoStack();
            undoStack.add(captureSnapshot());
            totalRuns += runs;
            extraPenalty += runs;
        }
    }

    // ✅ FIX: আগে "Bowling Team"-কে পেনাল্টি দেওয়ার বাটন শুধু Toast দেখাতো,
    // কোনো ডেটা আপডেট হতো না। এখন: যদি বোলিং টিম আগেই ব্যাট করে ফেলে থাকে
    // (মানে এটা ২য় ইনিংস চলছে), তাদের ১ম ইনিংসের স্কোর/টার্গেটে সরাসরি যোগ
    // হবে। আর যদি তারা এখনো ব্যাট না করে থাকে, রানটা জমা থাকবে এবং তাদের
    // ইনিংস শুরু হওয়ার সাথে সাথেই স্কোরে যোগ হয়ে যাবে (startSecondInnings() দেখুন)।
    public void addPenaltyRunsToBowlingTeam(int runs) {
        if (runs <= 0) return;
        ensureUndoStack();
        undoStack.add(captureSnapshot());
        if (isSecondInnings) {
            firstInningsScore += runs;
            targetRuns += runs;
            if (scoreInn1 != null && scoreInn1.contains("/")) {
                String[] parts = scoreInn1.split("/");
                try {
                    int r = Integer.parseInt(parts[0].trim()) + runs;
                    scoreInn1 = r + "/" + parts[1].trim();
                } catch (Exception ignored) { }
            }
        } else {
            pendingBowlingTeamPenalty += runs;
        }
    }

    public void swapStrikerStats() { 
        int tempR = strikerRuns; int tempB = strikerBalls; int temp4 = striker4s; int temp6 = striker6s; String tempN = strikerName; 
        strikerRuns = nonStrikerRuns; strikerBalls = nonStrikerBalls; striker4s = nonStriker4s; striker6s = nonStriker6s; strikerName = nonStrikerName; 
        nonStrikerRuns = tempR; nonStrikerBalls = tempB; nonStriker4s = temp4; nonStriker6s = temp6; nonStrikerName = tempN; 
    }

    // ✅ UNDO FIX: manual/rule-based strike swap (SWAP বাটন, বা ওভার শেষে নতুন
    // bowler সিলেক্ট হওয়ার পর ব্যাটসম্যান বদল) কোনো বল না হওয়া সত্ত্বেও
    // strikerRuns/nonStrikerRuns swap করে দেয়, কিন্তু ballHistory-তে কিছু
    // যোগ হয় না। ফলে পরে Undo চাপলে সেটা ধরেই নেয় বর্তমান striker-ই শেষ
    // বলটা খেলেছিল — যা এই swap-এর পরে আর সত্যি থাকে না, এবং ভুল
    // ব্যাটসম্যানের রান/বল থেকে বিয়োগ হয়ে negative স্ট্যাটস তৈরি হয়।
    //
    // ফিক্স: এই swap-টা কোন মুহূর্তে (ballHistory-র কোন size-এ) হয়েছিল তা
    // একটা আলাদা marker list-এ রাখা হচ্ছে। Undo চাপলে আগে চেক করা হবে —
    // সর্বশেষ action যদি এই swap-ই হয়ে থাকে (তার পরে নতুন কোনো বল না
    // হয়ে থাকে), তাহলে শুধু swap-টাই ফিরিয়ে দেওয়া হবে, ballHistory-তে
    // হাত দেওয়া হবে না।
    public java.util.ArrayList<Integer> manualSwapMarkers = new java.util.ArrayList<>();

    public void recordManualSwap() {
        // ✅ এখন recordManualSwap()-ও নতুন snapshot-ভিত্তিক undo stack-এ
        // যুক্ত হচ্ছে, তাই Swap বাটন চাপার পরেও Undo চাপলে ঠিকভাবে
        // আগের (pre-swap) অবস্থায় ফিরে যাওয়া যাবে।
        ensureUndoStack();
        undoStack.add(captureSnapshot());
        swapStrikerStats();
        manualSwapMarkers.add(ballHistory.size());
    }

    public void resetStrikerStats() { strikerRuns = 0; strikerBalls = 0; striker4s = 0; striker6s = 0; partnershipRuns = 0; partnershipBalls = 0; }
    public String getExtrasString() { return String.format("Extras: %d (wd %d, nb %d, b %d, lb %d, p %d)", getTotalExtras(), extraWide, extraNoBall, extraByes, extraLegByes, extraPenalty); }
    public String getOversString() { return currentOvers + "." + currentBalls + " / " + totalOvers; }
    public String getScoreString() { return totalRuns + "/" + totalWickets; }
    public String getBatsmanSR(int runs, int balls) { if (balls == 0) return "0.00"; return String.format("%.2f", (double) runs * 100 / balls); }
    public String getStrikerSR() { return getBatsmanSR(strikerRuns, strikerBalls); }
    public String getNonStrikerSR() { return getBatsmanSR(nonStrikerRuns, nonStrikerBalls); }
    public String getBowlerFigures() { return (bowlerBallsBowled / 6) + "." + (bowlerBallsBowled % 6); }
    public String getBowlerER() { if (bowlerBallsBowled == 0) return "0.00"; return String.format("%.2f", (double) bowlerRuns / (bowlerBallsBowled / 6.0)); }
    public String getCRR() { return String.format("%.2f", getCurrentRunRate()); }
    public String getPartnershipString() { return "Partnership: " + partnershipRuns + " (" + partnershipBalls + ")"; }
    public String getBattingTeamName() { return isSecondInnings ? teamBattingSecond : teamBattingFirst; }
}
