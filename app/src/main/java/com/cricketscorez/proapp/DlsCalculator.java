package com.cricketscorez.proapp;

import java.io.Serializable;
import java.util.Locale;

/**
 * ICC Duckworth-Lewis-Stern (DLS) Standard Edition Engine.
 * Calculates resource percentages based on overs remaining and wickets lost,
 * and computes revised targets and par scores for rain-interrupted limited overs matches.
 */
public class DlsCalculator implements Serializable {
    private static final long serialVersionUID = 1L;

    // Standard DLS Standard Edition parameters:
    // Z0[w] = maximum resource percentage available with w wickets lost (for u -> infinity)
    // b[w] = exponential decay constant per over with w wickets lost
    private static final double[] Z0 = {
            100.0,  // 0 wickets lost
            93.4,   // 1 wicket lost
            85.1,   // 2 wickets lost
            74.9,   // 3 wickets lost
            62.7,   // 4 wickets lost
            49.0,   // 5 wickets lost
            34.9,   // 6 wickets lost
            22.0,   // 7 wickets lost
            11.9,   // 8 wickets lost
            4.7     // 9 wickets lost
    };

    private static final double[] B = {
            0.035,  // 0 wickets lost
            0.034,  // 1 wicket lost
            0.032,  // 2 wickets lost
            0.030,  // 3 wickets lost
            0.028,  // 4 wickets lost
            0.026,  // 5 wickets lost
            0.024,  // 6 wickets lost
            0.022,  // 7 wickets lost
            0.020,  // 8 wickets lost
            0.018   // 9 wickets lost
    };

    // Reference factor: R(50, 0) unscaled
    private static final double R_50_0_RAW = Z0[0] * (1.0 - Math.exp(-B[0] * 50.0));

    /**
     * Calculates the percentage of resources available given the number of overs remaining
     * and the number of wickets lost.
     *
     * @param oversRemaining Number of overs remaining (can include fractional overs, e.g. 14.3 -> 14.5 overs)
     * @param wicketsLost Number of wickets already fallen (0 to 10)
     * @return Resource percentage between 0.0% and 100.0%
     */
    public static double getResourcePercentage(double oversRemaining, int wicketsLost) {
        if (oversRemaining <= 0.0 || wicketsLost >= 10) {
            return 0.0;
        }
        if (wicketsLost < 0) wicketsLost = 0;
        if (wicketsLost > 9) return 0.0;

        double raw = Z0[wicketsLost] * (1.0 - Math.exp(-B[wicketsLost] * oversRemaining));
        // Normalize so that 50 overs with 0 wickets lost equals 100.0%
        double normalized = (raw / R_50_0_RAW) * 100.0;
        if (normalized > 100.0) normalized = 100.0;
        if (normalized < 0.0) normalized = 0.0;
        return normalized;
    }

    /**
     * Helper to convert overs and balls to decimal overs (e.g., 14 overs and 3 balls -> 14.5 overs).
     */
    public static double toDecimalOvers(int overs, int balls) {
        return overs + (balls / 6.0);
    }

    /**
     * Result of a DLS target calculation.
     */
    public static class DlsResult implements Serializable {
        private static final long serialVersionUID = 1L;
        public int team1Score;
        public int team1Wickets;
        public double team1TotalOvers;
        public double team2TotalOvers;
        public double team1ResourceUsed; // R1
        public double team2ResourceAvailable; // R2
        public int revisedTarget;
        public int parScoreAtStart;
        public String explanation;

        public DlsResult() {}
    }

    /**
     * Computes the revised target for Team 2 when rain reduces the match length.
     *
     * @param team1Score Total runs scored by Team 1
     * @param team1OversBatted Total overs actually bowled to Team 1
     * @param team1WicketsLost Total wickets lost by Team 1
     * @param scheduledMatchOvers Original scheduled match overs (e.g. 20 for T20, 50 for ODI)
     * @param team2RevisedOvers Overs available for Team 2's innings
     * @return DlsResult containing revised target and resource details
     */
    public static DlsResult calculateRevisedTarget(
            int team1Score,
            double team1OversBatted,
            int team1WicketsLost,
            double scheduledMatchOvers,
            double team2RevisedOvers) {

        DlsResult result = new DlsResult();
        result.team1Score = team1Score;
        result.team1Wickets = team1WicketsLost;
        result.team1TotalOvers = team1OversBatted;
        result.team2TotalOvers = team2RevisedOvers;

        // Resource available to Team 1 at start of their innings:
        double r1Start = getResourcePercentage(scheduledMatchOvers, 0);

        // If Team 1's innings was terminated early (before their scheduled overs were bowled):
        double r1RemainingAtTermination = 0.0;
        if (team1OversBatted < scheduledMatchOvers && team1WicketsLost < 10) {
            double oversLeft = scheduledMatchOvers - team1OversBatted;
            r1RemainingAtTermination = getResourcePercentage(oversLeft, team1WicketsLost);
        }

        // R1: Total resource used by Team 1
        double r1 = r1Start - r1RemainingAtTermination;
        if (r1 <= 0.0) r1 = 1.0;

        // R2: Total resource available to Team 2
        double r2 = getResourcePercentage(team2RevisedOvers, 0);

        result.team1ResourceUsed = r1;
        result.team2ResourceAvailable = r2;

        // G50: Average 50-over ODI score is ~245; scale proportionally to match length:
        double gFactor = Math.max(120.0, (scheduledMatchOvers / 50.0) * 245.0);

        int target;
        if (r2 < r1) {
            // Team 2 has less resources than Team 1
            target = (int) Math.floor(team1Score * (r2 / r1)) + 1;
            result.explanation = String.format(Locale.getDefault(),
                    "R2 (%.1f%%) < R1 (%.1f%%): Target = floor(S1 * R2/R1) + 1 = %d",
                    r2, r1, target);
        } else {
            // Team 2 has equal or more resources than Team 1
            int extraRuns = (int) Math.floor(((r2 - r1) / 100.0) * gFactor);
            target = team1Score + extraRuns + 1;
            result.explanation = String.format(Locale.getDefault(),
                    "R2 (%.1f%%) >= R1 (%.1f%%): Target = S1 + floor((R2-R1)*G) + 1 = %d",
                    r2, r1, target);
        }

        // Target cannot be less than 1
        if (target < 1) target = 1;
        result.revisedTarget = target;
        result.parScoreAtStart = Math.max(0, target - 1);

        return result;
    }

    /**
     * Calculates the live DLS Par Score for Team 2 during their chase.
     *
     * @param team1Score Innings 1 total score
     * @param r1 Total resource used by Team 1
     * @param team2TotalOvers Total scheduled overs for Team 2 (e.g. 15.0)
     * @param currentOversBatted Team 2 current overs bowled (e.g. 8.2)
     * @param currentWicketsLost Team 2 current wickets fallen (0 to 10)
     * @return Par Score (the exact score where match is tied if interrupted now)
     */
    public static int calculateLiveParScore(
            int team1Score,
            double r1,
            double team2TotalOvers,
            double currentOversBatted,
            int currentWicketsLost) {

        if (r1 <= 0.0) r1 = 100.0;
        if (currentWicketsLost >= 10) return 9999; // All out

        double r2Total = getResourcePercentage(team2TotalOvers, 0);
        double oversRemaining = Math.max(0.0, team2TotalOvers - currentOversBatted);
        double r2Remaining = getResourcePercentage(oversRemaining, currentWicketsLost);

        // Resource used by Team 2 so far:
        double r2Used = Math.max(0.0, r2Total - r2Remaining);

        // Par score is proportional to resources used:
        int parScore = (int) Math.floor(team1Score * (r2Used / r1));
        return Math.max(0, parScore);
    }

    public static int calculateRevisedTarget(int scheduledOvers, int team1Score, int team1Wickets, int team2Overs) {
        DlsResult res = calculateRevisedTarget(team1Score, scheduledOvers, team1Wickets, scheduledOvers, team2Overs);
        return res.revisedTarget;
    }

    public static double calculateResource(double overs, int wickets) {
        return getResourcePercentage(overs, wickets);
    }
}
