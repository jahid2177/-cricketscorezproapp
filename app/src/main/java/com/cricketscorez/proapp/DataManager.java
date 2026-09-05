package com.cricketscorez.proapp;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import android.util.Base64;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

public class DataManager {

    private static final String PREF_NAME = "CricketScorezDB";
    private static final String PREF_HISTORY = "MatchHistoryDB";

    // ================= TEAM MANAGEMENT =================

    public static void saveTeam(Context context, String teamName, ArrayList<String> players) {
        if (teamName == null || teamName.trim().isEmpty()) return;
        SharedPreferences prefs = prefs(context);
        Set<String> teams = new HashSet<>(prefs.getStringSet("TEAM_LIST", new HashSet<String>()));
        teams.add(teamName);
        prefs.edit().putStringSet("TEAM_LIST", teams).putString("PLAYERS_" + teamName, toJson(players)).apply();
    }

    public static ArrayList<String> getAllTeams(Context context) {
        return new ArrayList<>(prefs(context).getStringSet("TEAM_LIST", new HashSet<String>()));
    }

    public static ArrayList<String> getPlayers(Context context, String teamName) {
        return fromJson(prefs(context).getString("PLAYERS_" + teamName, "[]"));
    }

    public static void deleteTeam(Context context, String teamName) {
        SharedPreferences prefs = prefs(context);
        Set<String> teams = new HashSet<>(prefs.getStringSet("TEAM_LIST", new HashSet<String>()));
        teams.remove(teamName);
        prefs.edit().putStringSet("TEAM_LIST", teams).remove("PLAYERS_" + teamName).apply();
        ImageStorageHelper.deleteTeamLogo(context, teamName);
    }

    public static void renameTeam(Context context, String oldName, String newName, ArrayList<String> players) {
        deleteTeam(context, oldName);
        saveTeam(context, newName, players);
        ImageStorageHelper.renameTeamLogo(context, oldName, newName);
    }

    // ================= PLAYER ROLES =================

    public static void savePlayerRole(Context context, String player, String role) {
        prefs(context).edit().putString("ROLE_" + player, role).apply();
    }

    public static String getPlayerRole(Context context, String player) {
        return prefs(context).getString("ROLE_" + player, "Player");
    }

    public static void deletePlayerData(Context context, String player) {
        prefs(context).edit().remove("ROLE_" + player).remove("STATS_" + player).apply();
        ImageStorageHelper.deletePlayerPhoto(context, player);
    }

    // ================= MATCH HISTORY (NEW FEATURES) =================

    public static void saveMatchToHistory(Context context, MatchData matchData) {
        if (matchData == null) return;

        matchData.initHistoryData(); // Ensure ID and Date exist

        SharedPreferences prefs = context.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(matchData);
            so.flush();
            String serializedObject = Base64.encodeToString(bo.toByteArray(), Base64.DEFAULT);

            editor.putString(matchData.matchId, serializedObject);
            editor.apply();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static ArrayList<MatchData> getAllMatches(Context context) {
        ArrayList<MatchData> matches = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            try {
                String serializedObject = (String) entry.getValue();
                byte[] b = Base64.decode(serializedObject, Base64.DEFAULT);
                ByteArrayInputStream bi = new ByteArrayInputStream(b);
                ObjectInputStream si = new ObjectInputStream(bi);
                MatchData match = (MatchData) si.readObject();
                matches.add(match);
            } catch (Exception e) { e.printStackTrace(); }
        }

        // Sort by Date (Newest First) using Match ID (timestamp)
        Collections.sort(matches, new Comparator<MatchData>() {
				@Override
				public int compare(MatchData o1, MatchData o2) {
					return o2.matchId.compareTo(o1.matchId);
				}
			});

        return matches;
    }

    public static void deleteMatch(Context context, String matchId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE);
        prefs.edit().remove(matchId).apply();
    }

    public static void clearAllMatches(Context context) {
        context.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE).edit().clear().apply();
    }

    // ================= STATS LOGIC =================

    public static void ensurePlayerStats(Context context, String player) {
        SharedPreferences prefs = prefs(context);
        String key = "STATS_" + player;
        if (!prefs.contains(key)) {
            try {
                JSONObject base = new JSONObject();
                base.put("matches", 0); base.put("innings", 0); base.put("runs", 0);
                base.put("balls", 0); base.put("not_outs", 0); base.put("best_score", 0);
                base.put("fours", 0); base.put("sixes", 0); base.put("hundreds", 0);
                base.put("fifties", 0); base.put("thirties", 0); base.put("ducks", 0);

                base.put("bowl_innings", 0); base.put("overs", 0.0); base.put("maidens", 0);
                base.put("wickets", 0); base.put("bowl_runs", 0); base.put("four_w", 0); base.put("five_w", 0);

                base.put("catches", 0); base.put("runouts", 0); base.put("stumpings", 0);

                prefs.edit().putString(key, base.toString()).apply();
            } catch (Exception e) {}
        }
    }

    public static JSONObject getPlayerStats(Context context, String player) {
        ensurePlayerStats(context, player);
        try {
            return new JSONObject(prefs(context).getString("STATS_" + player, "{}"));
        } catch (Exception e) { return new JSONObject(); }
    }

    public static void updateBatting(Context c, String p, int runs, int balls, int fours, int sixes, boolean isOut) {
        try {
            JSONObject s = getPlayerStats(c, p);
            s.put("matches", s.optInt("matches", 0) + 1);
            s.put("innings", s.optInt("innings", 0) + 1);
            s.put("runs", s.optInt("runs", 0) + runs);
            s.put("balls", s.optInt("balls", 0) + balls);
            s.put("fours", s.optInt("fours", 0) + fours);
            s.put("sixes", s.optInt("sixes", 0) + sixes);

            if (!isOut) s.put("not_outs", s.optInt("not_outs", 0) + 1);

            if (runs > s.optInt("best_score", 0)) s.put("best_score", runs);

            if (runs >= 100) s.put("hundreds", s.optInt("hundreds", 0) + 1);
            else if (runs >= 50) s.put("fifties", s.optInt("fifties", 0) + 1);
            else if (runs >= 30) s.put("thirties", s.optInt("thirties", 0) + 1);

            if (runs == 0 && isOut) s.put("ducks", s.optInt("ducks", 0) + 1);

            savePlayerStats(c, p, s);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void updateBowling(Context c, String p, double overs, int maidens, int runs, int wickets) {
        try {
            JSONObject s = getPlayerStats(c, p);
            s.put("bowl_innings", s.optInt("bowl_innings", 0) + 1);
            s.put("overs", s.optDouble("overs", 0.0) + overs);
            s.put("maidens", s.optInt("maidens", 0) + maidens);
            s.put("bowl_runs", s.optInt("bowl_runs", 0) + runs);
            s.put("wickets", s.optInt("wickets", 0) + wickets);

            if (wickets >= 5) s.put("five_w", s.optInt("five_w", 0) + 1);
            else if (wickets >= 4) s.put("four_w", s.optInt("four_w", 0) + 1);

            savePlayerStats(c, p, s);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ★ নতুন মেথড: এটি PlayerProfileActivity থেকে কল হবে
    public static void savePlayerStats(Context c, String p, JSONObject s) {
        prefs(c).edit().putString("STATS_" + p, s.toString()).apply();
    }

    // --- UTILS ---
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static String toJson(ArrayList<String> list) {
        JSONArray arr = new JSONArray();
        if (list != null) for (String s : list) arr.put(s);
        return arr.toString();
    }

    private static ArrayList<String> fromJson(String json) {
        ArrayList<String> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
        } catch (Exception e) {}
        return list;
    }
}
