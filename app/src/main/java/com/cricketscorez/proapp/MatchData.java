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
    public String tournamentId = "";

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

    // ✅ NEW: Retired Hurt Player record for resuming innings
    public static class RetiredHurtRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        public String playerName = "";
        public String batsmanName = "";
        public int runs = 0;
        public int balls = 0;
        public int fours = 0;
        public int sixes = 0;
        public boolean wasStriker = true;

        public RetiredHurtRecord() {}

        public RetiredHurtRecord(String name, int r, int b, int f, int s, boolean wasStriker) {
            this.playerName = (name != null) ? name : "";
            this.batsmanName = this.playerName;
            this.runs = r; this.balls = b; this.fours = f; this.sixes = s;
            this.wasStriker = wasStriker;
        }

        public RetiredHurtRecord cloneRecord() {
            return new RetiredHurtRecord(playerName, runs, balls, fours, sixes, wasStriker);
        }
    }

    // ✅ NEW: Complete Partnership record across all wickets
    public static class PartnershipRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        public int wicketNumber; // 1 for 1st wicket, etc.
        public String batsman1Name = "";
        public int batsman1Runs = 0;
        public int batsman1Balls = 0;
        public String batsman2Name = "";
        public int batsman2Runs = 0;
        public int batsman2Balls = 0;
        public int totalRuns = 0;
        public int totalBalls = 0;
        public boolean isUnbroken = false;

        public PartnershipRecord() {}

        public PartnershipRecord(int wkt, String b1, int r1, int bl1, String b2, int r2, int bl2, int totR, int totB, boolean unbroken) {
            this.wicketNumber = wkt;
            this.batsman1Name = (b1 != null) ? b1 : "";
            this.batsman1Runs = r1; this.batsman1Balls = bl1;
            this.batsman2Name = (b2 != null) ? b2 : "";
            this.batsman2Runs = r2; this.batsman2Balls = bl2;
            this.totalRuns = totR; this.totalBalls = totB;
            this.isUnbroken = unbroken;
        }

        public PartnershipRecord cloneRecord() {
            return new PartnershipRecord(wicketNumber, batsman1Name, batsman1Runs, batsman1Balls,
                    batsman2Name, batsman2Runs, batsman2Balls, totalRuns, totalBalls, isUnbroken);
        }
    }

    public ArrayList<RetiredHurtRecord> retiredHurtList = new ArrayList<>();
    public ArrayList<PartnershipRecord> partnerships = new ArrayList<>();
    public ArrayList<PartnershipRecord> partnershipsInn1 = new ArrayList<>();

    // 🌧️ DLS (Duckworth-Lewis-Stern) parameters
    public boolean isDlsApplied = false;
    public int dlsTargetRuns = 0;
    public String dlsTargetText = "";
    public double dlsTeam1Resource = 100.0;
    public double dlsTeam2Resource = 100.0;
    public int dlsRevisedOvers = 0;

    // ⚡ Super Over support
    public boolean isSuperOver = false;
    public String superOverParentMatchId = "";

    // ⚠️ Consecutive over restriction check (ICC Law 17.5)
    public String lastOverBowlerName = "";

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

    // ═════════════════════════════════════════════════════════════════════
    // 🏏 TEST MATCH MODE FIELDS
    // ═════════════════════════════════════════════════════════════════════
    public boolean isTestMatch = false;
    public int testDays = 5;
    public int oversPerDay = 90;
    public int currentDay = 1;
    public int currentSession = 1; // 1 = Morning, 2 = Afternoon, 3 = Evening
    public int followOnMargin = 200;
    public int testFollowOnMargin = 200;
    public int secondInningsScore = 0;
    public int ballsBowledToday = 0;
    public boolean isFollowOnEnforced = false;
    public int currentInnings = 1; // 1, 2, 3, or 4

    // Declaration markers per innings
    public boolean inn1Declared = false;
    public boolean inn2Declared = false;
    public boolean inn3Declared = false;
    public boolean inn4Declared = false;

    // Innings 2 archived data
    public String teamInn2 = "";
    public String scoreInn2 = "", oversInn2 = "", extrasInn2 = "";
    public int inn2Runs = 0, inn2Wickets = 0;
    public ArrayList<String[]> batsmanHistoryInn2 = new ArrayList<>();
    public ArrayList<String[]> bowlerHistoryInn2 = new ArrayList<>();
    public ArrayList<String> fallOfWicketsInn2 = new ArrayList<>();

    // Innings 3 archived data
    public String teamInn3 = "";
    public String scoreInn3 = "", oversInn3 = "", extrasInn3 = "";
    public int inn3Runs = 0, inn3Wickets = 0;
    public ArrayList<String[]> batsmanHistoryInn3 = new ArrayList<>();
    public ArrayList<String[]> bowlerHistoryInn3 = new ArrayList<>();
    public ArrayList<String> fallOfWicketsInn3 = new ArrayList<>();

    // Innings 4 archived data
    public String teamInn4 = "";
    public String scoreInn4 = "", oversInn4 = "", extrasInn4 = "";
    public int inn4Runs = 0, inn4Wickets = 0;
    public ArrayList<String[]> batsmanHistoryInn4 = new ArrayList<>();
    public ArrayList<String[]> bowlerHistoryInn4 = new ArrayList<>();
    public ArrayList<String> fallOfWicketsInn4 = new ArrayList<>();

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

        // Retired Hurt, Partnerships, DLS, and Super Over state
        ArrayList<RetiredHurtRecord> retiredHurtList;
        ArrayList<PartnershipRecord> partnerships;
        ArrayList<PartnershipRecord> partnershipsInn1;
        boolean isDlsApplied;
        int dlsTargetRuns;
        String dlsTargetText;
        double dlsTeam1Resource;
        double dlsTeam2Resource;
        int dlsRevisedOvers;
        boolean isSuperOver;
        String superOverParentMatchId;
        String lastOverBowlerName;

        // Test Match Snapshot state
        boolean isTestMatch;
        int testDays;
        int oversPerDay;
        int currentDay;
        int currentSession;
        int followOnMargin;
        boolean isFollowOnEnforced;
        int currentInnings;
        boolean inn1Declared, inn2Declared, inn3Declared, inn4Declared;
        String teamInn2, scoreInn2, oversInn2, extrasInn2;
        int inn2Runs, inn2Wickets;
        ArrayList<String[]> batsmanHistoryInn2;
        ArrayList<String[]> bowlerHistoryInn2;
        ArrayList<String> fallOfWicketsInn2;
        String teamInn3, scoreInn3, oversInn3, extrasInn3;
        int inn3Runs, inn3Wickets;
        ArrayList<String[]> batsmanHistoryInn3;
        ArrayList<String[]> bowlerHistoryInn3;
        ArrayList<String> fallOfWicketsInn3;
        String teamInn4, scoreInn4, oversInn4, extrasInn4;
        int inn4Runs, inn4Wickets;
        ArrayList<String[]> batsmanHistoryInn4;
        ArrayList<String[]> bowlerHistoryInn4;
        ArrayList<String> fallOfWicketsInn4;
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

        s.retiredHurtList = new ArrayList<>();
        for (RetiredHurtRecord r : retiredHurtList) {
            s.retiredHurtList.add(r.cloneRecord());
        }

        s.partnerships = new ArrayList<>();
        for (PartnershipRecord p : partnerships) {
            s.partnerships.add(p.cloneRecord());
        }

        s.partnershipsInn1 = new ArrayList<>();
        for (PartnershipRecord p : partnershipsInn1) {
            s.partnershipsInn1.add(p.cloneRecord());
        }

        s.isDlsApplied = isDlsApplied;
        s.dlsTargetRuns = dlsTargetRuns;
        s.dlsTargetText = dlsTargetText;
        s.dlsTeam1Resource = dlsTeam1Resource;
        s.dlsTeam2Resource = dlsTeam2Resource;
        s.dlsRevisedOvers = dlsRevisedOvers;
        s.isSuperOver = isSuperOver;
        s.superOverParentMatchId = superOverParentMatchId;
        s.lastOverBowlerName = lastOverBowlerName;

        s.isTestMatch = isTestMatch;
        s.testDays = testDays;
        s.oversPerDay = oversPerDay;
        s.currentDay = currentDay;
        s.currentSession = currentSession;
        s.followOnMargin = followOnMargin;
        s.isFollowOnEnforced = isFollowOnEnforced;
        s.currentInnings = currentInnings;
        s.inn1Declared = inn1Declared;
        s.inn2Declared = inn2Declared;
        s.inn3Declared = inn3Declared;
        s.inn4Declared = inn4Declared;

        s.teamInn2 = teamInn2; s.scoreInn2 = scoreInn2; s.oversInn2 = oversInn2; s.extrasInn2 = extrasInn2;
        s.inn2Runs = inn2Runs; s.inn2Wickets = inn2Wickets;
        s.batsmanHistoryInn2 = new ArrayList<>();
        if (batsmanHistoryInn2 != null) { for (String[] b : batsmanHistoryInn2) s.batsmanHistoryInn2.add(b.clone()); }
        s.bowlerHistoryInn2 = new ArrayList<>();
        if (bowlerHistoryInn2 != null) { for (String[] b : bowlerHistoryInn2) s.bowlerHistoryInn2.add(b.clone()); }
        s.fallOfWicketsInn2 = new ArrayList<>();
        if (fallOfWicketsInn2 != null) s.fallOfWicketsInn2.addAll(fallOfWicketsInn2);

        s.teamInn3 = teamInn3; s.scoreInn3 = scoreInn3; s.oversInn3 = oversInn3; s.extrasInn3 = extrasInn3;
        s.inn3Runs = inn3Runs; s.inn3Wickets = inn3Wickets;
        s.batsmanHistoryInn3 = new ArrayList<>();
        if (batsmanHistoryInn3 != null) { for (String[] b : batsmanHistoryInn3) s.batsmanHistoryInn3.add(b.clone()); }
        s.bowlerHistoryInn3 = new ArrayList<>();
        if (bowlerHistoryInn3 != null) { for (String[] b : bowlerHistoryInn3) s.bowlerHistoryInn3.add(b.clone()); }
        s.fallOfWicketsInn3 = new ArrayList<>();
        if (fallOfWicketsInn3 != null) s.fallOfWicketsInn3.addAll(fallOfWicketsInn3);

        s.teamInn4 = teamInn4; s.scoreInn4 = scoreInn4; s.oversInn4 = oversInn4; s.extrasInn4 = extrasInn4;
        s.inn4Runs = inn4Runs; s.inn4Wickets = inn4Wickets;
        s.batsmanHistoryInn4 = new ArrayList<>();
        if (batsmanHistoryInn4 != null) { for (String[] b : batsmanHistoryInn4) s.batsmanHistoryInn4.add(b.clone()); }
        s.bowlerHistoryInn4 = new ArrayList<>();
        if (bowlerHistoryInn4 != null) { for (String[] b : bowlerHistoryInn4) s.bowlerHistoryInn4.add(b.clone()); }
        s.fallOfWicketsInn4 = new ArrayList<>();
        if (fallOfWicketsInn4 != null) s.fallOfWicketsInn4.addAll(fallOfWicketsInn4);

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

        retiredHurtList.clear();
        if (s.retiredHurtList != null) {
            for (RetiredHurtRecord r : s.retiredHurtList) {
                retiredHurtList.add(r.cloneRecord());
            }
        }

        partnerships.clear();
        if (s.partnerships != null) {
            for (PartnershipRecord p : s.partnerships) {
                partnerships.add(p.cloneRecord());
            }
        }

        partnershipsInn1.clear();
        if (s.partnershipsInn1 != null) {
            for (PartnershipRecord p : s.partnershipsInn1) {
                partnershipsInn1.add(p.cloneRecord());
            }
        }

        isDlsApplied = s.isDlsApplied;
        dlsTargetRuns = s.dlsTargetRuns;
        dlsTargetText = s.dlsTargetText;
        dlsTeam1Resource = s.dlsTeam1Resource;
        dlsTeam2Resource = s.dlsTeam2Resource;
        dlsRevisedOvers = s.dlsRevisedOvers;
        isSuperOver = s.isSuperOver;
        superOverParentMatchId = s.superOverParentMatchId;
        lastOverBowlerName = s.lastOverBowlerName;

        isTestMatch = s.isTestMatch;
        testDays = s.testDays;
        oversPerDay = s.oversPerDay;
        currentDay = s.currentDay;
        currentSession = s.currentSession;
        followOnMargin = s.followOnMargin;
        isFollowOnEnforced = s.isFollowOnEnforced;
        currentInnings = s.currentInnings;
        inn1Declared = s.inn1Declared;
        inn2Declared = s.inn2Declared;
        inn3Declared = s.inn3Declared;
        inn4Declared = s.inn4Declared;

        teamInn2 = s.teamInn2; scoreInn2 = s.scoreInn2; oversInn2 = s.oversInn2; extrasInn2 = s.extrasInn2;
        inn2Runs = s.inn2Runs; inn2Wickets = s.inn2Wickets;
        batsmanHistoryInn2.clear();
        if (s.batsmanHistoryInn2 != null) { for (String[] b : s.batsmanHistoryInn2) batsmanHistoryInn2.add(b.clone()); }
        bowlerHistoryInn2.clear();
        if (s.bowlerHistoryInn2 != null) { for (String[] b : s.bowlerHistoryInn2) bowlerHistoryInn2.add(b.clone()); }
        fallOfWicketsInn2.clear();
        if (s.fallOfWicketsInn2 != null) fallOfWicketsInn2.addAll(s.fallOfWicketsInn2);

        teamInn3 = s.teamInn3; scoreInn3 = s.scoreInn3; oversInn3 = s.oversInn3; extrasInn3 = s.extrasInn3;
        inn3Runs = s.inn3Runs; inn3Wickets = s.inn3Wickets;
        batsmanHistoryInn3.clear();
        if (s.batsmanHistoryInn3 != null) { for (String[] b : s.batsmanHistoryInn3) batsmanHistoryInn3.add(b.clone()); }
        bowlerHistoryInn3.clear();
        if (s.bowlerHistoryInn3 != null) { for (String[] b : s.bowlerHistoryInn3) bowlerHistoryInn3.add(b.clone()); }
        fallOfWicketsInn3.clear();
        if (s.fallOfWicketsInn3 != null) fallOfWicketsInn3.addAll(s.fallOfWicketsInn3);

        teamInn4 = s.teamInn4; scoreInn4 = s.scoreInn4; oversInn4 = s.oversInn4; extrasInn4 = s.extrasInn4;
        inn4Runs = s.inn4Runs; inn4Wickets = s.inn4Wickets;
        batsmanHistoryInn4.clear();
        if (s.batsmanHistoryInn4 != null) { for (String[] b : s.batsmanHistoryInn4) batsmanHistoryInn4.add(b.clone()); }
        bowlerHistoryInn4.clear();
        if (s.bowlerHistoryInn4 != null) { for (String[] b : s.bowlerHistoryInn4) bowlerHistoryInn4.add(b.clone()); }
        fallOfWicketsInn4.clear();
        if (s.fallOfWicketsInn4 != null) fallOfWicketsInn4.addAll(s.fallOfWicketsInn4);

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

    public int getMaxWickets() {
        return isSuperOver ? 2 : 10;
    }

    public boolean isAllOut() {
        return totalWickets >= getMaxWickets();
    }

    public void recordPartnership(String outBatsman, boolean wasStrikerOut) {
        partnerships.add(new PartnershipRecord(
                totalWickets + 1,
                strikerName, strikerRuns, strikerBalls,
                nonStrikerName, nonStrikerRuns, nonStrikerBalls,
                partnershipRuns, partnershipBalls,
                false
        ));
    }

    public void recordUnbrokenPartnership() {
        if (strikerName != null && nonStrikerName != null && (partnershipRuns > 0 || partnershipBalls > 0 || strikerBalls > 0 || nonStrikerBalls > 0)) {
            partnerships.add(new PartnershipRecord(
                    totalWickets + 1,
                    strikerName, strikerRuns, strikerBalls,
                    nonStrikerName, nonStrikerRuns, nonStrikerBalls,
                    partnershipRuns, partnershipBalls,
                    true
            ));
        }
    }

    public ArrayList<PartnershipRecord> getAllPartnerships(boolean forFirstInnings) {
        ArrayList<PartnershipRecord> list = new ArrayList<>();
        if (forFirstInnings) {
            list.addAll(partnershipsInn1);
        } else {
            list.addAll(partnerships);
            // If match or innings is ongoing, add current unbroken partnership dynamically
            if (strikerName != null && nonStrikerName != null && (partnershipRuns > 0 || partnershipBalls > 0)) {
                list.add(new PartnershipRecord(
                        totalWickets + 1,
                        strikerName, strikerRuns, strikerBalls,
                        nonStrikerName, nonStrikerRuns, nonStrikerBalls,
                        partnershipRuns, partnershipBalls,
                        true
                ));
            }
        }
        return list;
    }

    public void resumeRetiredHurtBatsman(RetiredHurtRecord rec, boolean asStriker) {
        ensureUndoStack();
        undoStack.add(captureSnapshot());

        // Remove from retiredHurtList
        retiredHurtList.remove(rec);

        // Remove previous (Ret Hurt) from batsmanHistory so it doesn't double count
        for (int i = batsmanHistory.size() - 1; i >= 0; i--) {
            String[] row = batsmanHistory.get(i);
            if (row != null && row.length > 0 && row[0] != null && row[0].contains(rec.playerName) && row[0].contains("Ret Hurt")) {
                batsmanHistory.remove(i);
                break;
            }
        }

        if (asStriker) {
            strikerName = rec.playerName;
            strikerRuns = rec.runs;
            strikerBalls = rec.balls;
            striker4s = rec.fours;
            striker6s = rec.sixes;
        } else {
            nonStrikerName = rec.playerName;
            nonStrikerRuns = rec.runs;
            nonStrikerBalls = rec.balls;
            nonStriker4s = rec.fours;
            nonStriker6s = rec.sixes;
        }
    }

    public void switchBowler(String newName) {
        if (currentBowlerName != null && !currentBowlerName.equals("Bowler")) {
            lastOverBowlerName = currentBowlerName;
            saveCurrentBowlerStatsToRegistry();
        }
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
        recordPartnership(strikerName, true);
        String status = isHurt ? "(Ret Hurt)" : "(Ret Out)";
        String sr = getBatsmanSR(strikerRuns, strikerBalls);
        batsmanHistory.add(new String[]{strikerName + " " + status, String.valueOf(strikerRuns), String.valueOf(strikerBalls), String.valueOf(striker4s), String.valueOf(striker6s), sr});
        if (isHurt) {
            retiredHurtList.add(new RetiredHurtRecord(strikerName, strikerRuns, strikerBalls, striker4s, striker6s, true));
        } else {
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
        recordPartnership(nonStrikerName, false);
        String status = isHurt ? "(Ret Hurt)" : "(Ret Out)";
        String sr = getBatsmanSR(nonStrikerRuns, nonStrikerBalls);
        batsmanHistory.add(new String[]{nonStrikerName + " " + status, String.valueOf(nonStrikerRuns), String.valueOf(nonStrikerBalls), String.valueOf(nonStriker4s), String.valueOf(nonStriker6s), sr});
        if (isHurt) {
            retiredHurtList.add(new RetiredHurtRecord(nonStrikerName, nonStrikerRuns, nonStrikerBalls, nonStriker4s, nonStriker6s, false));
        } else {
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

        recordUnbrokenPartnership();
        this.partnershipsInn1.addAll(this.partnerships);
        this.partnerships.clear();
        this.retiredHurtList.clear();
        this.lastOverBowlerName = "";

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

    public boolean isInningsFinished() {
        if (isTestMatch) {
            return totalWickets >= 10;
        }
        try { return ballsBowled >= Integer.parseInt(totalOvers) * 6; } catch (Exception e) { return false; }
    }

    public void advanceTestInnings(String newStriker, String newNonStriker, String newBowler) {
        archiveCurrentInnings(false);
        currentInnings++;
        if (currentInnings == 2) {
            isSecondInnings = true;
        } else if (currentInnings == 4) {
            targetRuns = getTestFourthInningsTarget();
        }

        // Reset crease
        this.strikerName = newStriker;
        this.nonStrikerName = newNonStriker;
        this.currentBowlerName = newBowler;
        this.strikerRuns = 0; this.strikerBalls = 0; this.striker4s = 0; this.striker6s = 0;
        this.nonStrikerRuns = 0; this.nonStrikerBalls = 0; this.nonStriker4s = 0; this.nonStriker6s = 0;
        this.currentBowlerMaidens = 0; this.bowlerRuns = 0; this.bowlerWickets = 0; this.bowlerBallsBowled = 0;
        this.partnershipRuns = 0; this.partnershipBalls = 0;

        // Reset active innings totals
        this.totalRuns = pendingBowlingTeamPenalty;
        this.pendingBowlingTeamPenalty = 0;
        this.totalWickets = 0;
        this.ballsBowled = 0;
        this.currentBalls = 0;
        this.currentOvers = 0;
        this.extraWide = 0;
        this.extraNoBall = 0;
        this.extraByes = 0;
        this.extraLegByes = 0;
        this.extraPenalty = 0;

        this.bowlerRegistry.clear();
        this.bowlerDisplayNames.clear();
        this.currentOverBalls.clear();
        this.batsmanHistory.clear();
        this.bowlerHistory.clear();
        this.fallOfWickets.clear();
        this.commentaryList.clear();
        this.ballHistory.clear();
        this.partnerships.clear();
        this.retiredHurtList.clear();

        this.isOverFinished = false;
        this.isMaidenOver = true;

        ensureUndoStack();
        this.undoStack.clear();
        this.manualSwapMarkers.clear();

        if (newBowler != null && !newBowler.isEmpty() && !newBowler.equalsIgnoreCase("Bowler")) {
            switchBowler(newBowler);
        }
    }

    public void archiveCurrentInnings(boolean declared) {
        if (strikerName != null && !strikerName.isEmpty()) {
            boolean found = false;
            for (String[] b : batsmanHistory) {
                if (b[0].replace("*", "").trim().equalsIgnoreCase(strikerName.trim())) { found = true; break; }
            }
            if (!found) {
                batsmanHistory.add(new String[]{strikerName + "*", String.valueOf(strikerRuns), String.valueOf(strikerBalls), String.valueOf(striker4s), String.valueOf(striker6s), getStrikerSR()});
            }
        }
        if (nonStrikerName != null && !nonStrikerName.isEmpty()) {
            boolean found = false;
            for (String[] b : batsmanHistory) {
                if (b[0].replace("*", "").trim().equalsIgnoreCase(nonStrikerName.trim())) { found = true; break; }
            }
            if (!found) {
                batsmanHistory.add(new String[]{nonStrikerName + "*", String.valueOf(nonStrikerRuns), String.valueOf(nonStrikerBalls), String.valueOf(nonStriker4s), String.valueOf(nonStriker6s), getNonStrikerSR()});
            }
        }
        if (currentBowlerName != null && !currentBowlerName.equals("Bowler")) {
            saveCurrentBowlerStatsToRegistry();
        }

        int inNum = currentInnings;
        if (inNum == 1) {
            inn1Declared = declared;
            this.firstInningsScore = this.totalRuns;
            this.scoreInn1 = getScoreString() + (declared ? " d" : "");
            this.oversInn1 = currentOvers + "." + currentBalls + " ov";
            this.extrasInn1 = getExtrasString();
            this.batsmanHistoryInn1 = new ArrayList<>(this.batsmanHistory);
            this.bowlerHistoryInn1 = getAllBowlingStats();
            this.fallOfWicketsInn1 = new ArrayList<>(this.fallOfWickets);
            this.commentaryListInn1 = new ArrayList<>(this.commentaryList);
            this.ballHistoryInn1 = new ArrayList<>(this.ballHistory);
            this.runRateInn1 = getCumulativeRunsPerOver();
            this.wicketsInn1 = getCumulativeWicketsPerOver();
            this.inn1Sixes = getSixesCount();
            this.inn1Fours = getFoursCount();
            this.inn1Dots = getDotsCount();
            this.inn1ExtrasTotal = getTotalExtras();
            this.partnershipsInn1 = new ArrayList<>(this.partnerships);
        } else if (inNum == 2) {
            inn2Declared = declared;
            this.teamInn2 = teamBattingSecond;
            this.inn2Runs = this.totalRuns;
            this.inn2Wickets = this.totalWickets;
            this.scoreInn2 = getScoreString() + (declared ? " d" : "");
            this.oversInn2 = currentOvers + "." + currentBalls + " ov";
            this.extrasInn2 = getExtrasString();
            this.batsmanHistoryInn2 = new ArrayList<>(this.batsmanHistory);
            this.bowlerHistoryInn2 = getAllBowlingStats();
            this.fallOfWicketsInn2 = new ArrayList<>(this.fallOfWickets);
        } else if (inNum == 3) {
            inn3Declared = declared;
            this.teamInn3 = getBattingTeamName();
            this.inn3Runs = this.totalRuns;
            this.inn3Wickets = this.totalWickets;
            this.scoreInn3 = getScoreString() + (declared ? " d" : "");
            this.oversInn3 = currentOvers + "." + currentBalls + " ov";
            this.extrasInn3 = getExtrasString();
            this.batsmanHistoryInn3 = new ArrayList<>(this.batsmanHistory);
            this.bowlerHistoryInn3 = getAllBowlingStats();
            this.fallOfWicketsInn3 = new ArrayList<>(this.fallOfWickets);
        } else if (inNum == 4) {
            inn4Declared = declared;
            this.teamInn4 = getBattingTeamName();
            this.inn4Runs = this.totalRuns;
            this.inn4Wickets = this.totalWickets;
            this.scoreInn4 = getScoreString() + (declared ? " d" : "");
            this.oversInn4 = currentOvers + "." + currentBalls + " ov";
            this.extrasInn4 = getExtrasString();
            this.batsmanHistoryInn4 = new ArrayList<>(this.batsmanHistory);
            this.bowlerHistoryInn4 = getAllBowlingStats();
            this.fallOfWicketsInn4 = new ArrayList<>(this.fallOfWickets);
        }
    }

    public String getTestMatchLeadTrailStatus() {
        if (!isTestMatch) return "";
        if (currentInnings == 1) {
            return "1st Innings in progress";
        } else if (currentInnings == 2) {
            int diff = totalRuns - firstInningsScore;
            if (diff < 0) {
                return teamBattingSecond + " trail by " + Math.abs(diff) + " runs";
            } else if (diff > 0) {
                return teamBattingSecond + " lead by " + diff + " runs";
            } else {
                return "Scores Level (" + totalRuns + " runs)";
            }
        } else if (currentInnings == 3) {
            if (isFollowOnEnforced) {
                int diff = (inn2Runs + totalRuns) - firstInningsScore;
                if (diff < 0) {
                    return teamBattingSecond + " (f/o) trail by " + Math.abs(diff) + " runs";
                } else if (diff > 0) {
                    return teamBattingSecond + " lead by " + diff + " runs";
                } else {
                    return "Scores Level (Trail erased)";
                }
            } else {
                int lead = (firstInningsScore + totalRuns) - inn2Runs;
                return teamBattingFirst + " lead by " + lead + " runs";
            }
        } else if (currentInnings == 4) {
            int runsNeeded = targetRuns - totalRuns;
            if (runsNeeded <= 0) {
                return "Target Reached!";
            }
            return "Target: " + targetRuns + " • Need " + runsNeeded + " runs to win";
        }
        return "";
    }

    public void advanceOverAndSessionIfNeeded() {
        ballsBowledToday += 6;
        int oversToday = ballsBowledToday / 6;
        int sessionOvers = oversPerDay > 0 ? (oversPerDay / 3) : 30;
        if (sessionOvers <= 0) sessionOvers = 30;

        if (oversToday >= currentSession * sessionOvers) {
            if (currentSession < 3) {
                currentSession++;
            } else if (oversToday >= oversPerDay) {
                currentDay++;
                currentSession = 1;
                ballsBowledToday = 0;
            }
        }
    }

    public void startTestThirdInnings() {
        archiveCurrentInnings(false);
        currentInnings = 3;
        inn2Runs = secondInningsScore > 0 ? secondInningsScore : inn2Runs;

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
        this.partnerships.clear();
        this.retiredHurtList.clear();
        this.isOverFinished = false; 
        this.isMaidenOver = true;

        ensureUndoStack();
        this.undoStack.clear();
        this.manualSwapMarkers.clear();
    }

    public void startTestFourthInnings() {
        archiveCurrentInnings(false);
        currentInnings = 4;
        targetRuns = getTestFourthInningsTarget();

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
        this.partnerships.clear();
        this.retiredHurtList.clear();
        this.isOverFinished = false; 
        this.isMaidenOver = true;

        ensureUndoStack();
        this.undoStack.clear();
        this.manualSwapMarkers.clear();
    }

    public boolean canEnforceFollowOn() {
        if (!isTestMatch || currentInnings != 2) return false;
        int trail = firstInningsScore - totalRuns;
        int margin = testFollowOnMargin > 0 ? testFollowOnMargin : followOnMargin;
        return trail >= margin;
    }

    public int getTestFourthInningsTarget() {
        if (!isTestMatch) return 0;
        if (isFollowOnEnforced) {
            int t = (inn2Runs + inn3Runs) - firstInningsScore + 1;
            return Math.max(1, t);
        } else {
            int t = (firstInningsScore + inn3Runs) - inn2Runs + 1;
            return Math.max(1, t);
        }
    }

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
    public String getOversString() {
        if (isTestMatch) {
            return currentOvers + "." + currentBalls + " ov";
        }
        return currentOvers + "." + currentBalls + " / " + totalOvers;
    }
    public String getScoreString() { return totalRuns + "/" + totalWickets; }
    public String getBatsmanSR(int runs, int balls) { if (balls == 0) return "0.00"; return String.format("%.2f", (double) runs * 100 / balls); }
    public String getStrikerSR() { return getBatsmanSR(strikerRuns, strikerBalls); }
    public String getNonStrikerSR() { return getBatsmanSR(nonStrikerRuns, nonStrikerBalls); }
    public String getBowlerFigures() { return (bowlerBallsBowled / 6) + "." + (bowlerBallsBowled % 6); }
    public String getBowlerER() { if (bowlerBallsBowled == 0) return "0.00"; return String.format("%.2f", (double) bowlerRuns / (bowlerBallsBowled / 6.0)); }
    public String getCRR() { return String.format("%.2f", getCurrentRunRate()); }
    public String getPartnershipString() { return "Partnership: " + partnershipRuns + " (" + partnershipBalls + ")"; }

    public String getBattingTeamName() {
        if (!isTestMatch) {
            return isSecondInnings ? teamBattingSecond : teamBattingFirst;
        }
        switch (currentInnings) {
            case 2:
                return teamBattingSecond;
            case 3:
                return isFollowOnEnforced ? teamBattingSecond : teamBattingFirst;
            case 4:
                return isFollowOnEnforced ? teamBattingFirst : teamBattingSecond;
            case 1:
            default:
                return teamBattingFirst;
        }
    }

    public String getBowlingTeamName() {
        if (!isTestMatch) {
            return isSecondInnings ? teamBattingFirst : teamBattingSecond;
        }
        switch (currentInnings) {
            case 2:
                return teamBattingFirst;
            case 3:
                return isFollowOnEnforced ? teamBattingFirst : teamBattingSecond;
            case 4:
                return isFollowOnEnforced ? teamBattingSecond : teamBattingFirst;
            case 1:
            default:
                return teamBattingSecond;
        }
    }
}
