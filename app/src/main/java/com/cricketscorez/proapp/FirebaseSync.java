package com.cricketscorez.proapp;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ========================================================
 *  FirebaseSync.java
 *  Cricket Scorez Pro — Firebase Realtime Database এ
 *  Teams, Tournaments, Fixtures, Point Table, Results ও
 *  Match History সেভ করার জন্য (Viewer অ্যাপ/ওয়েবসাইট এই
 *  ডাটা রিয়েলটাইমে পড়তে পারবে)।
 *
 *  SupabaseSync-এর জায়গায় ব্যবহার করার জন্য বানানো — একই
 *  মেথড নাম ও প্যারামিটার রাখা হয়েছে, তাই আগের কল করা
 *  জায়গাগুলোতে শুধু "SupabaseSync." কে "FirebaseSync." দিয়ে
 *  বদলে দিলেই কাজ চলবে।
 *
 *  ব্যবহার:
 *    FirebaseSync.upsertTeam(teamName, players);
 *    FirebaseSync.upsertTournament(name, overs, winPts, tiePts, cb);
 *    FirebaseSync.upsertPointTable(tournamentId, rows);
 *    FirebaseSync.addFixture(tournamentId, teamA, teamB, date, venue);
 *    FirebaseSync.saveTournamentResult(tournamentId, matchId, teamA, teamB, resultText);
 *    FirebaseSync.saveMatchHistory(matchData, resultMessage);
 *
 *  Firebase-এ Data Structure:
 *  ━━━━━━━━━━━━━━━━━━━━━━━━━━
 *  teams/{teamId}/
 *    ├── team_name, team_short_name
 *    ├── players/{playerId}: player_name
 *    └── wins, losses, draws, matches_played
 *
 *  players/{playerId}/
 *    ├── player_name, team_id, team_name
 *    └── stats/: matches, runs, wickets, fours, sixes, catches
 *
 *  tournaments/{tournamentId}/
 *    ├── info/: name, type, total_overs, win_points, tie_points, status
 *    ├── point_table/{teamId}: played, won, lost, tied, no_result, points, nrr, form
 *    ├── fixtures/{fixtureId}: team_a, team_b, date, venue, status, match_id
 *    └── results/{matchId}: team_a, team_b, team_a_score, team_b_score, result_text
 *
 *  matchHistory/{matchId}/
 *    └── পুরো scorecard + result + status:"completed"
 */
public class FirebaseSync {

    private static final String TAG = "FirebaseSync";

    private static final String TEAMS_PATH        = "teams";
    private static final String PLAYERS_PATH       = "players";
    private static final String TOURNAMENTS_PATH   = "tournaments";
    private static final String MATCH_HISTORY_PATH = "matchHistory";

    public interface SyncCallback { void onDone(boolean success, String message); }

    private static DatabaseReference root() {
        return FirebaseDatabase.getInstance().getReference();
    }

    // ────────────────────────────────────────────────────────────
    //  ID তৈরির helper — নাম থেকে lowercase+underscore key বানায়
    //  (Firebase key-তে ".", "#", "$", "/", "[", "]" ব্যবহার করা যায় না)
    // ────────────────────────────────────────────────────────────
    private static String slug(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase()
                .replaceAll("[.#$\\[\\]/]", "")
                .replaceAll("\\s+", "_");
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TEAMS — save/update team + players
    // ═══════════════════════════════════════════════════════════════════

    public static void upsertTeam(String teamName, ArrayList<String> players) {
        upsertTeam(teamName, players, null);
    }

    public static void upsertTeam(String teamName, ArrayList<String> players, SyncCallback cb) {
        try {
            String teamId = slug(teamName);

            Map<String, Object> body = new HashMap<>();
            body.put("team_name", teamName);
            body.put("team_short_name", teamName.length() >= 3
                    ? teamName.substring(0, 3).toUpperCase() : teamName.toUpperCase());
            body.put("updated_at", ServerValue.TIMESTAMP);

            // Players list — team নোডের ভিতরে একটা ম্যাপ হিসেবে রাখা হচ্ছে
            Map<String, Object> playerMap = new HashMap<>();
            if (players != null) {
                for (String playerName : players) {
                    String playerId = slug(playerName);
                    playerMap.put(playerId, playerName);
                }
            }
            body.put("players", playerMap);

            root().child(TEAMS_PATH).child(teamId).updateChildren(body)
                    .addOnCompleteListener(task -> {
                        boolean ok = task.isSuccessful();
                        if (cb != null) cb.onDone(ok, ok ? "Synced" : String.valueOf(task.getException()));
                    });

            // players/{playerId} নোডেও আলাদা করে entry রাখা হচ্ছে
            if (players != null) {
                for (String playerName : players) {
                    String playerId = slug(playerName);
                    Map<String, Object> pBody = new HashMap<>();
                    pBody.put("player_name", playerName);
                    pBody.put("team_id", teamId);
                    pBody.put("team_name", teamName);
                    root().child(PLAYERS_PATH).child(playerId).updateChildren(pBody);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "upsertTeam() error: " + e.getMessage());
            if (cb != null) cb.onDone(false, e.getMessage());
        }
    }

    // ─── Team মুছলে Firebase থেকেও মুছা হবে ──────────────────────────
    public static void deleteTeam(String teamName) {
        try {
            String teamId = slug(teamName);
            root().child(TEAMS_PATH).child(teamId).removeValue();
            // team-এর players গুলোও মুছে দেওয়া হচ্ছে (team_id মিলিয়ে)
            root().child(PLAYERS_PATH).orderByChild("team_id").equalTo(teamId)
                    .get().addOnSuccessListener(snapshot -> {
                        for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().removeValue();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "deleteTeam() error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TOURNAMENT — save/update tournament settings
    // ═══════════════════════════════════════════════════════════════════

    public static void upsertTournament(String name, String overs, String winPts, String tiePts) {
        upsertTournament(name, overs, winPts, tiePts, null);
    }

    public static void upsertTournament(String name, String overs, String winPts,
                                         String tiePts, SyncCallback cb) {
        try {
            String tournamentId = slug(name) + "_" + (System.currentTimeMillis() / 86400000L);

            Map<String, Object> info = new HashMap<>();
            info.put("name", name);
            info.put("type", overs + " Overs");
            info.put("total_overs", overs);
            info.put("win_points", winPts);
            info.put("tie_points", tiePts);
            info.put("status", "active");
            info.put("created_at", ServerValue.TIMESTAMP);

            root().child(TOURNAMENTS_PATH).child(tournamentId).child("info")
                    .setValue(info)
                    .addOnCompleteListener(task -> {
                        boolean ok = task.isSuccessful();
                        if (cb != null) cb.onDone(ok, ok ? tournamentId : String.valueOf(task.getException()));
                    });

        } catch (Exception e) {
            Log.e(TAG, "upsertTournament() error: " + e.getMessage());
            if (cb != null) cb.onDone(false, e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FIXTURES / SCHEDULE — আসন্ন ম্যাচের সময়সূচি
    // ═══════════════════════════════════════════════════════════════════

    /**
     * নতুন fixture যোগ করে (auto-generated fixture id দিয়ে)
     */
    public static void addFixture(String tournamentId, String teamA, String teamB,
                                   String date, String venue) {
        addFixture(tournamentId, teamA, teamB, date, venue, null);
    }

    public static void addFixture(String tournamentId, String teamA, String teamB,
                                   String date, String venue, SyncCallback cb) {
        try {
            DatabaseReference fixturesRef = root().child(TOURNAMENTS_PATH)
                    .child(tournamentId).child("fixtures");
            String fixtureId = fixturesRef.push().getKey();
            if (fixtureId == null) fixtureId = String.valueOf(System.currentTimeMillis());

            Map<String, Object> body = new HashMap<>();
            body.put("team_a", teamA);
            body.put("team_b", teamB);
            body.put("date", date);
            body.put("venue", venue != null ? venue : "");
            body.put("status", "scheduled"); // scheduled | live | completed

            String finalFixtureId = fixtureId;
            fixturesRef.child(fixtureId).setValue(body)
                    .addOnCompleteListener(task -> {
                        boolean ok = task.isSuccessful();
                        if (cb != null) cb.onDone(ok, ok ? finalFixtureId : String.valueOf(task.getException()));
                    });
        } catch (Exception e) {
            Log.e(TAG, "addFixture() error: " + e.getMessage());
            if (cb != null) cb.onDone(false, e.getMessage());
        }
    }

    /**
     * fixture-এর status আপডেট করে (যেমন ম্যাচ শুরু হলে "live", শেষ হলে "completed")
     * matchId দিয়ে লাইভ স্কোরের সাথে fixture-কে লিংক করা হয় (viewer app তখন
     * live_matches/{matchId} থেকে সরাসরি লাইভ স্কোর টেনে দেখাতে পারবে)
     */
    public static void updateFixtureStatus(String tournamentId, String fixtureId,
                                            String status, String matchId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("status", status);
            if (matchId != null) body.put("match_id", matchId);
            root().child(TOURNAMENTS_PATH).child(tournamentId)
                    .child("fixtures").child(fixtureId)
                    .updateChildren(body);
        } catch (Exception e) {
            Log.e(TAG, "updateFixtureStatus() error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POINT TABLE — tournament এর point table sync করা
    // ═══════════════════════════════════════════════════════════════════

    /**
     * @param tournamentId  Firebase-এ tournament key
     * @param rows          প্রতিটি row: {"team_name","played","won","lost","tied","points","nrr"}
     */
    public static void upsertPointTable(String tournamentId, List<JSONObject> rows) {
        try {
            DatabaseReference ptRef = root().child(TOURNAMENTS_PATH)
                    .child(tournamentId).child("point_table");

            for (JSONObject row : rows) {
                String teamName = row.optString("team_name", "");
                String teamId = slug(teamName);

                Map<String, Object> body = new HashMap<>();
                body.put("team_name", teamName);
                body.put("team_short_name", teamName.length() >= 3
                        ? teamName.substring(0, 3).toUpperCase() : teamName.toUpperCase());
                body.put("played", row.optInt("played", 0));
                body.put("won", row.optInt("won", 0));
                body.put("lost", row.optInt("lost", 0));
                body.put("tied", row.optInt("tied", 0));
                body.put("no_result", row.optInt("no_result", 0));
                body.put("points", row.optInt("points", 0));
                body.put("net_run_rate", row.optDouble("nrr", 0.0));
                body.put("form", row.optString("form", ""));

                ptRef.child(teamId).updateChildren(body);
            }
        } catch (Exception e) {
            Log.e(TAG, "upsertPointTable() error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TOURNAMENT RESULT — একটা নির্দিষ্ট ম্যাচের ফলাফল tournament-এর
    //  results তালিকায় যোগ করা (Point Table-এর পাশাপাশি)
    // ═══════════════════════════════════════════════════════════════════

    public static void saveTournamentResult(String tournamentId, String matchId,
                                             String teamA, String teamB,
                                             String teamAScore, String teamBScore,
                                             String resultText) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("team_a", teamA);
            body.put("team_b", teamB);
            body.put("team_a_score", teamAScore);
            body.put("team_b_score", teamBScore);
            body.put("result_text", resultText);
            body.put("timestamp", ServerValue.TIMESTAMP);

            root().child(TOURNAMENTS_PATH).child(tournamentId)
                    .child("results").child(matchId)
                    .setValue(body);
        } catch (Exception e) {
            Log.e(TAG, "saveTournamentResult() error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MATCH HISTORY — match শেষ হলে পুরো scorecard সহ history save করা
    // ═══════════════════════════════════════════════════════════════════

    public static void saveMatchHistory(MatchData matchData, String resultMessage) {
        saveMatchHistory(matchData, resultMessage, null);
    }

    public static void saveMatchHistory(MatchData matchData, String resultMessage,
                                         SyncCallback cb) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("match_id", matchData.matchId);
            body.put("tournament_name", matchData.tournamentName != null ? matchData.tournamentName : "");
            body.put("team_a", matchData.teamBattingFirst != null
                    ? matchData.teamBattingFirst : matchData.team1Name);
            body.put("team_b", matchData.teamBattingSecond != null
                    ? matchData.teamBattingSecond : matchData.team2Name);

            // 1st innings
            body.put("team_a_score", matchData.scoreInn1 != null ? matchData.scoreInn1 : matchData.getScoreString());
            body.put("team_a_overs", matchData.oversInn1 != null ? matchData.oversInn1 : "");

            // 2nd innings
            body.put("team_b_score", matchData.getScoreString());
            body.put("team_b_overs", matchData.getOversString());

            body.put("match_type", matchData.totalOvers + " Overs");
            body.put("match_date", matchData.matchDate != null ? matchData.matchDate : "");
            body.put("toss_info", matchData.tossMessage != null ? matchData.tossMessage : "");
            body.put("result", resultMessage);
            body.put("status", "completed");
            body.put("saved_at", ServerValue.TIMESTAMP);

            // পুরো scorecard-ও রাখা হচ্ছে (viewer-এ full scorecard দেখানোর জন্য)
            Map<String, Object> scorecard = new HashMap<>();
            scorecard.put("batsman_history", matchData.batsmanHistory);
            scorecard.put("bowler_history", matchData.bowlerHistory);
            scorecard.put("fall_of_wickets", matchData.fallOfWickets);
            scorecard.put("batsman_history_inn1", matchData.batsmanHistoryInn1);
            scorecard.put("bowler_history_inn1", matchData.bowlerHistoryInn1);
            scorecard.put("fall_of_wickets_inn1", matchData.fallOfWicketsInn1);
            body.put("scorecard", scorecard);

            root().child(MATCH_HISTORY_PATH).child(matchData.matchId)
                    .setValue(body)
                    .addOnCompleteListener(task -> {
                        boolean ok = task.isSuccessful();
                        if (cb != null) cb.onDone(ok, ok ? "Saved" : String.valueOf(task.getException()));
                    });

            // যদি এই ম্যাচ কোনো tournament-এর অংশ হয়, tournament results-এও যোগ করো
            if (matchData.isTournamentMatch && matchData.tournamentMatchId != null
                    && !matchData.tournamentMatchId.isEmpty()) {
                saveTournamentResult(
                        matchData.tournamentMatchId,
                        matchData.matchId,
                        matchData.teamBattingFirst,
                        matchData.teamBattingSecond,
                        matchData.scoreInn1,
                        matchData.getScoreString(),
                        resultMessage
                );
            }

            // Players stats আপডেট
            syncPlayerStats(matchData);

        } catch (Exception e) {
            Log.e(TAG, "saveMatchHistory() error: " + e.getMessage());
            if (cb != null) cb.onDone(false, e.getMessage());
        }
    }

    // ─── Player career stats sync (matches, runs, wickets — কাউন্টার হিসেবে বাড়বে) ──
    private static void syncPlayerStats(MatchData matchData) {
        try {
            incrementBattingStats(matchData.strikerName, matchData.strikerRuns,
                    matchData.strikerBalls, matchData.striker4s, matchData.striker6s);
            incrementBattingStats(matchData.nonStrikerName, matchData.nonStrikerRuns,
                    matchData.nonStrikerBalls, matchData.nonStriker4s, matchData.nonStriker6s);
            incrementBowlingStats(matchData.currentBowlerName, matchData.bowlerRuns,
                    matchData.bowlerWickets, matchData.currentBowlerMaidens);
        } catch (Exception e) {
            Log.e(TAG, "syncPlayerStats() error: " + e.getMessage());
        }
    }

    private static void incrementBattingStats(String playerName, int runs, int balls,
                                               int fours, int sixes) {
        if (playerName == null || playerName.trim().isEmpty()) return;
        String playerId = slug(playerName);
        DatabaseReference statsRef = root().child(PLAYERS_PATH).child(playerId).child("stats");

        Map<String, Object> body = new HashMap<>();
        body.put("matches", ServerValue.increment(1));
        body.put("runs", ServerValue.increment(runs));
        body.put("balls_faced", ServerValue.increment(balls));
        body.put("fours", ServerValue.increment(fours));
        body.put("sixes", ServerValue.increment(sixes));
        statsRef.updateChildren(body);
    }

    private static void incrementBowlingStats(String playerName, int runsConceded,
                                               int wickets, int maidens) {
        if (playerName == null || playerName.trim().isEmpty()) return;
        String playerId = slug(playerName);
        DatabaseReference statsRef = root().child(PLAYERS_PATH).child(playerId).child("stats");

        Map<String, Object> body = new HashMap<>();
        body.put("runs_conceded", ServerValue.increment(runsConceded));
        body.put("wickets", ServerValue.increment(wickets));
        body.put("maidens", ServerValue.increment(maidens));
        statsRef.updateChildren(body);
    }
}
