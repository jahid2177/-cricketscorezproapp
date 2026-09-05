package com.cricketscorez.proapp;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SupabaseSync — Admin App থেকে Supabase এ data পাঠানোর জন্য
 *
 * ব্যবহার:
 *   SupabaseSync.upsertTeam(teamName, players);
 *   SupabaseSync.upsertTournament(name, overs, winPts, tiePts);
 *   SupabaseSync.upsertPointTable(tournamentId, rows);
 *   SupabaseSync.saveMatchHistory(matchData, resultMessage);
 */
public class SupabaseSync {

    // ─── আপনার Supabase তথ্য এখানে বসান ─────────────────────────────────
    private static final String BASE_URL     = "https://dwlwbqhfzcqabxlqxerq.supabase.co"; // ← আপনার Project URL
    private static final String SERVICE_KEY  = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImR3bHdicWhmemNxYWJ4bHF4ZXJxIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NzIxNjkyMywiZXhwIjoyMDkyNzkyOTIzfQ.7cIe_p24Da1QubQuKF9G51vt6yVw56LKSbeAHzzpsG0"; // ← service_role key

    private static final String REST         = BASE_URL + "/rest/v1";
    private static final int    TIMEOUT      = 15000;

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SyncCallback { void onDone(boolean success, String message); }

    // ═══════════════════════════════════════════════════════════════════
    //  TEAMS — save/update team + players
    // ═══════════════════════════════════════════════════════════════════

    public static void upsertTeam(String teamName, java.util.ArrayList<String> players) {
        upsertTeam(teamName, players, null);
    }

    public static void upsertTeam(String teamName, java.util.ArrayList<String> players, SyncCallback cb) {
        executor.execute(() -> {
            try {
                // team_id হিসেবে নাম lowercase+underscore ব্যবহার করা হচ্ছে
                String teamId = teamName.trim().toLowerCase().replaceAll("\\s+", "_");

                JSONObject body = new JSONObject();
                body.put("team_id",         teamId);
                body.put("team_name",       teamName);
                body.put("team_short_name", teamName.length() >= 3
                        ? teamName.substring(0, 3).toUpperCase() : teamName.toUpperCase());
                body.put("total_matches",   0);
                body.put("wins",            0);
                body.put("losses",          0);
                body.put("draws",           0);

                // Players list → description field এ JSON হিসেবে রাখা হচ্ছে
                JSONArray pArr = new JSONArray();
                if (players != null) for (String p : players) pArr.put(p);
                body.put("description", pArr.toString());

                boolean ok = upsert("teams", "team_id", body);

                // Players table এও আলাদা আলাদা row রাখা হচ্ছে
                if (players != null) {
                    for (String playerName : players) {
                        String playerId = teamId + "_" + playerName.trim()
                                .toLowerCase().replaceAll("\\s+", "_");
                        JSONObject pBody = new JSONObject();
                        pBody.put("player_id",   playerId);
                        pBody.put("player_name", playerName);
                        pBody.put("team_id",     teamId);
                        pBody.put("team_name",   teamName);
                        upsert("players", "player_id", pBody);
                    }
                }

                if (cb != null) mainHandler.post(() -> cb.onDone(ok, ok ? "Synced" : "Failed"));

            } catch (Exception e) {
                if (cb != null) mainHandler.post(() -> cb.onDone(false, e.getMessage()));
            }
        });
    }

    // ─── Team মুছলে Supabase থেকেও মুছা হবে ──────────────────────────
    public static void deleteTeam(String teamName) {
        executor.execute(() -> {
            try {
                String teamId = teamName.trim().toLowerCase().replaceAll("\\s+", "_");
                // players মুছা
                httpDelete(REST + "/players?team_id=eq." + enc(teamId));
                // team মুছা
                httpDelete(REST + "/teams?team_id=eq." + enc(teamId));
            } catch (Exception e) { /* silent fail */ }
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    //  TOURNAMENT — save/update tournament settings
    // ═══════════════════════════════════════════════════════════════════

    public static void upsertTournament(String name, String overs, String winPts, String tiePts) {
        upsertTournament(name, overs, winPts, tiePts, null);
    }

    public static void upsertTournament(String name, String overs, String winPts,
                                        String tiePts, SyncCallback cb) {
        executor.execute(() -> {
            try {
                String tournamentId = name.trim().toLowerCase().replaceAll("\\s+", "_")
                        + "_" + System.currentTimeMillis() / 86400000L; // প্রতিদিন আলাদা ID

                JSONObject body = new JSONObject();
                body.put("tournament_id",   tournamentId);
                body.put("tournament_name", name);
                body.put("tournament_type", overs + " Overs");
                body.put("status",          "active");
                body.put("organizer",       "Admin");
                // overs, winPts, tiePts → description এ রাখা হচ্ছে
                JSONObject meta = new JSONObject();
                meta.put("total_overs", overs);
                meta.put("win_points",  winPts);
                meta.put("tie_points",  tiePts);
                body.put("description", meta.toString());

                boolean ok = upsert("tournaments", "tournament_id", body);

                // tournament_id SharedPreferences এ save করা হচ্ছে
                // যাতে পরে point table ও history তে ব্যবহার করা যায়
                // (MainActivity/PointTableActivity থেকে পড়বে)
                // এখানে শুধু sync করা হচ্ছে

                if (cb != null) mainHandler.post(() ->
                        cb.onDone(ok, ok ? tournamentId : "Failed"));

            } catch (Exception e) {
                if (cb != null) mainHandler.post(() -> cb.onDone(false, e.getMessage()));
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    //  POINT TABLE — tournament এর point table sync করা
    // ═══════════════════════════════════════════════════════════════════

    /**
     * @param tournamentId  Supabase এ tournament_id
     * @param rows          প্রতিটি row: {"team_name","played","won","lost","tied","points","nrr"}
     */
    public static void upsertPointTable(String tournamentId,
                                        java.util.List<org.json.JSONObject> rows) {
        executor.execute(() -> {
            try {
                for (JSONObject row : rows) {
                    String teamName = row.optString("team_name", "");
                    String teamId   = teamName.trim().toLowerCase().replaceAll("\\s+", "_");

                    JSONObject body = new JSONObject();
                    body.put("tournament_id",   tournamentId);
                    body.put("team_id",         teamId);
                    body.put("team_name",       teamName);
                    body.put("team_short_name", teamName.length() >= 3
                            ? teamName.substring(0, 3).toUpperCase() : teamName.toUpperCase());
                    body.put("played",          row.optInt("played", 0));
                    body.put("won",             row.optInt("won", 0));
                    body.put("lost",            row.optInt("lost", 0));
                    body.put("tied",            row.optInt("tied", 0));
                    body.put("no_result",       row.optInt("no_result", 0));
                    body.put("points",          row.optInt("points", 0));
                    body.put("net_run_rate",    row.optDouble("nrr", 0.0));
                    body.put("form",            row.optString("form", ""));

                    upsert("point_table", "tournament_id,team_id", body);
                }
            } catch (Exception e) { /* silent fail */ }
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MATCH HISTORY — match শেষ হলে history save করা
    // ═══════════════════════════════════════════════════════════════════

    public static void saveMatchHistory(MatchData matchData, String resultMessage) {
        saveMatchHistory(matchData, resultMessage, null);
    }

    public static void saveMatchHistory(MatchData matchData, String resultMessage,
                                        SyncCallback cb) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("match_id",        matchData.matchId);
                body.put("tournament_name", matchData.tournamentName != null
                        ? matchData.tournamentName : "");
                body.put("team_a",          matchData.teamBattingFirst  != null
                        ? matchData.teamBattingFirst : matchData.team1Name);
                body.put("team_b",          matchData.teamBattingSecond != null
                        ? matchData.teamBattingSecond : matchData.team2Name);

                // 1st innings score
                body.put("team_a_score", matchData.scoreInn1 != null
                        ? matchData.scoreInn1 : matchData.getScoreString());
                body.put("team_a_overs", matchData.oversInn1 != null
                        ? matchData.oversInn1 : "");

                // 2nd innings score
                body.put("team_b_score", matchData.getScoreString());
                body.put("team_b_overs", matchData.getOversString());

                body.put("match_type",   matchData.totalOvers + " Overs");
                body.put("match_date",   matchData.matchDate   != null ? matchData.matchDate : "");
                body.put("toss_winner",  matchData.tossMessage != null ? matchData.tossMessage : "");
                body.put("result",       resultMessage);
                body.put("status",       "completed");

                boolean ok = upsert("match_history", "match_id", body);

                // Players এর stats update করা হচ্ছে
                syncPlayerStats(matchData);

                if (cb != null) mainHandler.post(() -> cb.onDone(ok, ok ? "Saved" : "Failed"));

            } catch (Exception e) {
                if (cb != null) mainHandler.post(() -> cb.onDone(false, e.getMessage()));
            }
        });
    }

    // ─── Player stats sync ────────────────────────────────────────────
    private static void syncPlayerStats(MatchData matchData) {
        // matchData থেকে batting/bowling stats নিয়ে Supabase players table update
        // batsmen
        try {
            if (matchData.strikerName != null) updatePlayerStats(matchData, matchData.strikerName);
            if (matchData.nonStrikerName != null) updatePlayerStats(matchData, matchData.nonStrikerName);
        } catch (Exception ignored) {}
    }

    private static void updatePlayerStats(MatchData matchData, String playerName) {
        try {
            String teamId   = (matchData.teamBattingFirst != null
                    ? matchData.teamBattingFirst : "unknown")
                    .toLowerCase().replaceAll("\\s+", "_");
            String playerId = teamId + "_" + playerName.trim()
                    .toLowerCase().replaceAll("\\s+", "_");

            JSONObject body = new JSONObject();
            body.put("player_id",      playerId);
            body.put("player_name",    playerName);
            body.put("team_id",        teamId);
            body.put("matches_played", 1); // upsert হলে বাড়বে না, তাই আলাদা logic দরকার হলে পরে যোগ করুন

            upsert("players", "player_id", body);
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Internal HTTP Helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Supabase UPSERT — record থাকলে update, না থাকলে insert
     * @param table      Supabase table name
     * @param onConflict conflict column(s), e.g. "team_id" or "tournament_id,team_id"
     * @param body       JSON body
     */
    private static boolean upsert(String table, String onConflict, JSONObject body) {
        try {
            String url = REST + "/" + table + "?on_conflict=" + onConflict;
            int code = httpPost(url, body.toString(), "POST");
            return code >= 200 && code < 300;
        } catch (Exception e) {
            return false;
        }
    }

    private static int httpPost(String urlStr, String json, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("apikey",         SERVICE_KEY);
        conn.setRequestProperty("Authorization",  "Bearer " + SERVICE_KEY);
        conn.setRequestProperty("Content-Type",   "application/json");
        conn.setRequestProperty("Prefer",         "resolution=merge-duplicates");
        conn.setDoOutput(true);
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);

        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes("UTF-8"));
        os.close();

        int code = conn.getResponseCode();
        conn.disconnect();
        return code;
    }

    private static void httpDelete(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("apikey",        SERVICE_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + SERVICE_KEY);
        conn.setConnectTimeout(TIMEOUT);
        conn.setReadTimeout(TIMEOUT);
        conn.getResponseCode();
        conn.disconnect();
    }

    private static String enc(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }
}
