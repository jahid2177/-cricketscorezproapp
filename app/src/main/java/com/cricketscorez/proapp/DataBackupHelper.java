package com.cricketscorez.proapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.cricketscorez.proapp.room.AppRoomDatabase;
import com.cricketscorez.proapp.room.FavoriteMatchEntity;
import com.cricketscorez.proapp.room.LiveMatchProgressEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * DataBackupHelper
 * Handles complete JSON Export and Import of all application data:
 * - Teams, Players, Roles, and Statistics (Batting, Bowling, Fielding)
 * - Match History (Full MatchData object graph with complete ball-by-ball events & summaries)
 * - Tournament Settings, Groups, Fixtures, Results, and Player Rankings
 * - Application Settings & Theme preferences
 * - SQLite database entries & Room Database Favorites
 */
public class DataBackupHelper {

    private static final String TAG = "DataBackupHelper";
    public static final String BACKUP_FILE_PREFIX = "CricketScorez_Backup_";
    public static final String MIME_TYPE_JSON = "application/json";

    // ─────────────────────────────────────────────────────────────────────────
    //  EXPORT LOGIC
    // ─────────────────────────────────────────────────────────────────────────

    public static JSONObject exportAllDataToJson(Context context) throws Exception {
        JSONObject root = new JSONObject();

        // 1. Metadata
        long now = System.currentTimeMillis();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(now));
        root.put("appName", "CricketScorez Pro");
        root.put("appPackage", context.getPackageName());
        root.put("schemaVersion", 1);
        root.put("exportTimestamp", now);
        root.put("exportDate", dateStr);

        JSONObject dataObj = new JSONObject();

        // 2. Teams and Players
        JSONArray teamsArray = new JSONArray();
        ArrayList<String> teams = DataManager.getAllTeams(context);

        // Also check SQLite teams to ensure no team is left out
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            ArrayList<String> sqlTeams = dbHelper.getAllTeams();
            if (sqlTeams != null) {
                for (String t : sqlTeams) {
                    if (t != null && !teams.contains(t)) {
                        teams.add(t);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking SQLite teams: " + e.getMessage());
        }

        SharedPreferences scorezPrefs = context.getSharedPreferences("CricketScorezDB", Context.MODE_PRIVATE);

        Set<String> allPlayerNames = new HashSet<>();

        for (String teamName : teams) {
            if (teamName == null || teamName.trim().isEmpty()) continue;
            JSONObject teamObj = new JSONObject();
            teamObj.put("name", teamName);

            ArrayList<String> players = DataManager.getPlayers(context, teamName);
            JSONArray playersArr = new JSONArray();
            JSONArray playerDetailsArr = new JSONArray();

            for (String p : players) {
                if (p == null || p.trim().isEmpty()) continue;
                playersArr.put(p);
                allPlayerNames.add(p);

                JSONObject pObj = new JSONObject();
                pObj.put("name", p);
                pObj.put("role", DataManager.getPlayerRole(context, p));
                pObj.put("stats", DataManager.getPlayerStats(context, p));
                playerDetailsArr.put(pObj);
            }

            teamObj.put("players", playersArr);
            teamObj.put("playerDetails", playerDetailsArr);

            // Team Logo reference if available
            String logoPath = ImageStorageHelper.getTeamLogoPath(context, teamName);
            if (logoPath != null) {
                teamObj.put("logoPath", logoPath);
            }

            teamsArray.put(teamObj);
        }
        dataObj.put("teams", teamsArray);

        // Collect all player roles and stats from SharedPreferences
        JSONObject allRolesObj = new JSONObject();
        JSONObject allStatsObj = new JSONObject();
        Map<String, ?> scorezEntries = scorezPrefs.getAll();
        for (Map.Entry<String, ?> entry : scorezEntries.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("ROLE_")) {
                allRolesObj.put(key.substring(5), String.valueOf(entry.getValue()));
            } else if (key.startsWith("STATS_")) {
                try {
                    allStatsObj.put(key.substring(6), new JSONObject(String.valueOf(entry.getValue())));
                } catch (Exception ignored) {
                    allStatsObj.put(key.substring(6), String.valueOf(entry.getValue()));
                }
            }
        }
        dataObj.put("allPlayerRoles", allRolesObj);
        dataObj.put("allPlayerStats", allStatsObj);

        // 3. Match History
        JSONArray matchesArray = new JSONArray();
        SharedPreferences historyPrefs = context.getSharedPreferences("MatchHistoryDB", Context.MODE_PRIVATE);
        Map<String, ?> historyEntries = historyPrefs.getAll();

        for (Map.Entry<String, ?> entry : historyEntries.entrySet()) {
            String matchId = entry.getKey();
            Object rawVal = entry.getValue();
            if (!(rawVal instanceof String)) continue;

            String base64Payload = (String) rawVal;
            JSONObject matchObj = new JSONObject();
            matchObj.put("matchId", matchId);
            matchObj.put("rawPayload", base64Payload);

            // Try to extract human-readable summary fields from deserialized MatchData
            try {
                byte[] b = Base64.decode(base64Payload, Base64.DEFAULT);
                ByteArrayInputStream bi = new ByteArrayInputStream(b);
                ObjectInputStream si = new ObjectInputStream(bi);
                MatchData match = (MatchData) si.readObject();
                if (match != null) {
                    matchObj.put("team1Name", match.team1Name);
                    matchObj.put("team2Name", match.team2Name);
                    matchObj.put("matchDate", match.matchDate);
                    matchObj.put("matchStatus", match.matchStatus);
                    matchObj.put("matchResult", match.matchResult);
                    matchObj.put("totalOvers", match.totalOvers);
                    matchObj.put("totalRuns", match.totalRuns);
                    matchObj.put("totalWickets", match.totalWickets);
                    matchObj.put("currentOvers", match.currentOvers);
                    matchObj.put("currentBalls", match.currentBalls);
                    matchObj.put("scoreInn1", match.scoreInn1);
                    matchObj.put("oversInn1", match.oversInn1);
                    matchObj.put("isSecondInnings", match.isSecondInnings);
                    matchObj.put("tossMessage", match.tossMessage);
                    matchObj.put("isTournamentMatch", match.isTournamentMatch);
                    matchObj.put("tournamentName", match.tournamentName);
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not decode match summary for: " + matchId + " - " + e.getMessage());
            }

            matchesArray.put(matchObj);
        }
        dataObj.put("matchHistory", matchesArray);

        // 4. Tournament Data
        JSONObject tournamentRoot = new JSONObject();
        tournamentRoot.put("tournamentData", exportSharedPrefs(context, "TournamentData"));
        tournamentRoot.put("tournamentResult", exportSharedPrefs(context, "TournamentResult"));
        tournamentRoot.put("tournamentFixtures", exportSharedPrefs(context, "TournamentFixtures"));
        tournamentRoot.put("tournamentRankings", exportSharedPrefs(context, "TournamentRankings"));
        tournamentRoot.put("tournamentSettings", exportSharedPrefs(context, "TournamentSettings"));
        dataObj.put("tournament", tournamentRoot);

        // 5. App Settings & Theme
        dataObj.put("appSettings", exportSharedPrefs(context, "AppSettings"));
        dataObj.put("appTheme", exportSharedPrefs(context, "AppThemePrefs"));

        // 6. SQLite Database (Teams and Players table)
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            JSONObject sqliteObj = new JSONObject();
            ArrayList<String[]> sqlPlayers = dbHelper.getAllPlayers();
            JSONArray sqlPlayersArr = new JSONArray();
            if (sqlPlayers != null) {
                for (String[] p : sqlPlayers) {
                    JSONObject pObj = new JSONObject();
                    pObj.put("id", p[0]);
                    pObj.put("name", p[1]);
                    pObj.put("role", p[2]);
                    pObj.put("teamName", p[3]);
                    sqlPlayersArr.put(pObj);
                }
            }
            sqliteObj.put("players", sqlPlayersArr);
            dataObj.put("sqliteData", sqliteObj);
        } catch (Exception e) {
            Log.w(TAG, "Error exporting SQLite: " + e.getMessage());
        }

        // 7. Room Database (Favorite matches)
        try {
            AppRoomDatabase roomDb = AppRoomDatabase.getDatabase(context);
            List<FavoriteMatchEntity> favs = roomDb.favoriteMatchDao().getAllFavorites();
            JSONArray favArr = new JSONArray();
            if (favs != null) {
                for (FavoriteMatchEntity f : favs) {
                    JSONObject fObj = new JSONObject();
                    fObj.put("matchId", f.matchId);
                    fObj.put("team1Name", f.team1Name);
                    fObj.put("team2Name", f.team2Name);
                    fObj.put("matchDate", f.matchDate);
                    fObj.put("overs", f.overs);
                    fObj.put("matchStatus", f.matchStatus);
                    fObj.put("scoreInn1", f.scoreInn1);
                    fObj.put("oversInn1", f.oversInn1);
                    fObj.put("scoreInn2", f.scoreInn2);
                    fObj.put("oversInn2", f.oversInn2);
                    fObj.put("isSecondInnings", f.isSecondInnings);
                    fObj.put("isLive", f.isLive);
                    fObj.put("savedAt", f.savedAt);
                    favArr.put(fObj);
                }
            }
            dataObj.put("favoriteMatches", favArr);
        } catch (Exception e) {
            Log.w(TAG, "Error exporting Room DB: " + e.getMessage());
        }

        root.put("data", dataObj);
        return root;
    }

    public static String exportAllDataToString(Context context) throws Exception {
        JSONObject json = exportAllDataToJson(context);
        return json.toString(2); // Indented 2 spaces for beautiful human readability
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INSPECT / VALIDATE BACKUP
    // ─────────────────────────────────────────────────────────────────────────

    public static class BackupInspection {
        public boolean isValid = false;
        public String appName = "";
        public String exportDate = "";
        public int schemaVersion = 1;
        public int teamCount = 0;
        public int matchCount = 0;
        public int playerCount = 0;
        public boolean hasTournament = false;
        public boolean hasSettings = false;
        public String errorMessage = null;

        public String getSummaryText() {
            StringBuilder sb = new StringBuilder();
            sb.append("📅 Backup Date: ").append(exportDate.isEmpty() ? "Unknown" : exportDate).append("\n\n");
            sb.append("🏏 Teams: ").append(teamCount).append("\n");
            sb.append("👥 Players: ").append(playerCount).append("\n");
            sb.append("📜 Matches: ").append(matchCount).append("\n");
            sb.append("🏆 Tournament Data: ").append(hasTournament ? "Included" : "None").append("\n");
            sb.append("App Settings: ").append(hasSettings ? "Included" : "None");
            return sb.toString();
        }
    }

    public static BackupInspection inspectBackup(String jsonString) {
        BackupInspection inspection = new BackupInspection();
        if (jsonString == null || jsonString.trim().isEmpty()) {
            inspection.errorMessage = "Backup file is empty.";
            return inspection;
        }

        try {
            JSONObject root = new JSONObject(jsonString);
            inspection.appName = root.optString("appName", "CricketScorez");
            inspection.exportDate = root.optString("exportDate", "");
            inspection.schemaVersion = root.optInt("schemaVersion", 1);

            JSONObject data = root.optJSONObject("data");
            if (data == null) {
                // Check if the root itself has teams/matchHistory directly (flexible compatibility)
                data = root;
            }

            JSONArray teamsArr = data.optJSONArray("teams");
            if (teamsArr != null) {
                inspection.teamCount = teamsArr.length();
                Set<String> uniquePlayers = new HashSet<>();
                for (int i = 0; i < teamsArr.length(); i++) {
                    JSONObject team = teamsArr.optJSONObject(i);
                    if (team != null) {
                        JSONArray pl = team.optJSONArray("players");
                        if (pl != null) {
                            for (int j = 0; j < pl.length(); j++) {
                                uniquePlayers.add(pl.optString(j));
                            }
                        }
                    }
                }
                inspection.playerCount = uniquePlayers.size();
            }

            JSONObject allRoles = data.optJSONObject("allPlayerRoles");
            if (allRoles != null && allRoles.length() > inspection.playerCount) {
                inspection.playerCount = allRoles.length();
            }

            JSONArray matchesArr = data.optJSONArray("matchHistory");
            if (matchesArr != null) {
                inspection.matchCount = matchesArr.length();
            }

            JSONObject tourObj = data.optJSONObject("tournament");
            if (tourObj != null && tourObj.length() > 0) {
                inspection.hasTournament = true;
            }

            JSONObject settingsObj = data.optJSONObject("appSettings");
            if (settingsObj != null && settingsObj.length() > 0) {
                inspection.hasSettings = true;
            }

            inspection.isValid = true;
        } catch (Exception e) {
            inspection.isValid = false;
            inspection.errorMessage = "Invalid JSON format: " + e.getMessage();
        }

        return inspection;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  IMPORT / RESTORE LOGIC
    // ─────────────────────────────────────────────────────────────────────────

    public static class ImportResult {
        public boolean success = false;
        public int teamsImported = 0;
        public int matchesImported = 0;
        public int playersImported = 0;
        public boolean tournamentRestored = false;
        public boolean settingsRestored = false;
        public String errorMessage = null;

        public String getResultMessage() {
            if (!success) {
                return "Restore Failed: " + (errorMessage != null ? errorMessage : "Unknown error");
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Data successfully restored!\n\n");
            sb.append("• ").append(teamsImported).append(" Teams restored\n");
            sb.append("• ").append(playersImported).append(" Players & Statistics restored\n");
            sb.append("• ").append(matchesImported).append(" Matches restored\n");
            if (tournamentRestored) sb.append("• Tournament & Fixtures data restored\n");
            if (settingsRestored) sb.append("• App Settings & Preferences restored\n");
            return sb.toString();
        }
    }

    public static ImportResult importDataFromJson(Context context, String jsonString, boolean overwrite) {
        ImportResult result = new ImportResult();
        if (jsonString == null || jsonString.trim().isEmpty()) {
            result.errorMessage = "File is empty";
            return result;
        }

        try {
            JSONObject root = new JSONObject(jsonString);
            JSONObject data = root.optJSONObject("data");
            if (data == null) {
                data = root;
            }

            DatabaseHelper dbHelper = new DatabaseHelper(context);

            // 1. If overwrite is requested, clear existing match history and teams
            if (overwrite) {
                DataManager.clearAllMatches(context);
                try {
                    dbHelper.clearAllTeamsAndPlayers();
                    dbHelper.clearAllMatchHistory();
                } catch (Exception ignored) {}

                // Clear CricketScorezDB
                context.getSharedPreferences("CricketScorezDB", Context.MODE_PRIVATE)
                        .edit().clear().apply();

                // Clear Room favorites
                try {
                    AppRoomDatabase.getDatabase(context).favoriteMatchDao().clearAllFavorites();
                } catch (Exception ignored) {}
            }

            // 2. Restore Teams and Players
            JSONArray teamsArr = data.optJSONArray("teams");
            Set<String> processedPlayers = new HashSet<>();

            if (teamsArr != null) {
                for (int i = 0; i < teamsArr.length(); i++) {
                    JSONObject teamObj = teamsArr.optJSONObject(i);
                    if (teamObj == null) continue;

                    String teamName = teamObj.optString("name", "").trim();
                    if (teamName.isEmpty()) continue;

                    JSONArray playersArr = teamObj.optJSONArray("players");
                    ArrayList<String> playerList = new ArrayList<>();

                    if (playersArr != null) {
                        for (int j = 0; j < playersArr.length(); j++) {
                            String pName = playersArr.optString(j, "").trim();
                            if (!pName.isEmpty()) {
                                playerList.add(pName);
                                processedPlayers.add(pName);
                            }
                        }
                    }

                    // Save team to DataManager
                    DataManager.saveTeam(context, teamName, playerList);
                    result.teamsImported++;

                    // Also save to SQLite DatabaseHelper
                    try {
                        if (!dbHelper.hasTeam(teamName)) {
                            dbHelper.addTeam(teamName);
                        }
                    } catch (Exception ignored) {}

                    // Restore player details (role & stats) if provided per team
                    JSONArray detailsArr = teamObj.optJSONArray("playerDetails");
                    if (detailsArr != null) {
                        for (int k = 0; k < detailsArr.length(); k++) {
                            JSONObject pObj = detailsArr.optJSONObject(k);
                            if (pObj == null) continue;
                            String pName = pObj.optString("name", "").trim();
                            if (pName.isEmpty()) continue;

                            String role = pObj.optString("role", "Player");
                            DataManager.savePlayerRole(context, pName, role);

                            JSONObject stats = pObj.optJSONObject("stats");
                            if (stats != null) {
                                DataManager.savePlayerStats(context, pName, stats);
                            }

                            try {
                                dbHelper.addPlayer(pName, role, teamName);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            // Restore allPlayerRoles (in case some players weren't mapped to a team or had custom roles)
            JSONObject allRoles = data.optJSONObject("allPlayerRoles");
            if (allRoles != null) {
                java.util.Iterator<String> keys = allRoles.keys();
                while (keys.hasNext()) {
                    String pName = keys.next();
                    String role = allRoles.optString(pName, "Player");
                    DataManager.savePlayerRole(context, pName, role);
                    processedPlayers.add(pName);
                }
            }

            // Restore allPlayerStats
            JSONObject allStats = data.optJSONObject("allPlayerStats");
            if (allStats != null) {
                java.util.Iterator<String> keys = allStats.keys();
                while (keys.hasNext()) {
                    String pName = keys.next();
                    JSONObject stats = allStats.optJSONObject(pName);
                    if (stats != null) {
                        DataManager.savePlayerStats(context, pName, stats);
                    }
                    processedPlayers.add(pName);
                }
            }

            result.playersImported = processedPlayers.size();

            // 3. Restore Match History
            JSONArray matchesArr = data.optJSONArray("matchHistory");
            if (matchesArr != null) {
                SharedPreferences historyPrefs = context.getSharedPreferences("MatchHistoryDB", Context.MODE_PRIVATE);
                SharedPreferences.Editor historyEdit = historyPrefs.edit();

                for (int i = 0; i < matchesArr.length(); i++) {
                    JSONObject mObj = matchesArr.optJSONObject(i);
                    if (mObj == null) continue;

                    String matchId = mObj.optString("matchId", "").trim();
                    String rawPayload = mObj.optString("rawPayload", "");

                    if (!matchId.isEmpty() && !rawPayload.isEmpty()) {
                        // Direct high-fidelity binary payload restoration
                        historyEdit.putString(matchId, rawPayload);
                        result.matchesImported++;
                    } else {
                        // Fallback: construct MatchData from readable JSON fields
                        String t1 = mObj.optString("team1Name", "Team A");
                        String t2 = mObj.optString("team2Name", "Team B");
                        String overs = mObj.optString("totalOvers", "20");
                        MatchData md = new MatchData(t1, t2, overs);
                        if (!matchId.isEmpty()) md.matchId = matchId;
                        md.matchDate = mObj.optString("matchDate", md.matchDate);
                        md.matchStatus = mObj.optString("matchStatus", "Completed");
                        md.matchResult = mObj.optString("matchResult", "");
                        md.totalRuns = mObj.optInt("totalRuns", 0);
                        md.totalWickets = mObj.optInt("totalWickets", 0);
                        md.currentOvers = mObj.optInt("currentOvers", 0);
                        md.currentBalls = mObj.optInt("currentBalls", 0);
                        md.scoreInn1 = mObj.optString("scoreInn1", "");
                        md.oversInn1 = mObj.optString("oversInn1", "");
                        md.isSecondInnings = mObj.optBoolean("isSecondInnings", false);
                        md.tossMessage = mObj.optString("tossMessage", md.tossMessage);
                        md.isTournamentMatch = mObj.optBoolean("isTournamentMatch", false);
                        md.tournamentName = mObj.optString("tournamentName", "");

                        DataManager.saveMatchToHistory(context, md);
                        result.matchesImported++;
                    }
                }
                historyEdit.apply();
            }

            // 4. Restore Tournament Data
            JSONObject tourObj = data.optJSONObject("tournament");
            if (tourObj != null) {
                restoreSharedPrefs(context, "TournamentData", tourObj.optJSONObject("tournamentData"), overwrite);
                restoreSharedPrefs(context, "TournamentResult", tourObj.optJSONObject("tournamentResult"), overwrite);
                restoreSharedPrefs(context, "TournamentFixtures", tourObj.optJSONObject("tournamentFixtures"), overwrite);
                restoreSharedPrefs(context, "TournamentRankings", tourObj.optJSONObject("tournamentRankings"), overwrite);
                restoreSharedPrefs(context, "TournamentSettings", tourObj.optJSONObject("tournamentSettings"), overwrite);
                result.tournamentRestored = true;
            }

            // 5. Restore App Settings & Theme
            JSONObject settingsObj = data.optJSONObject("appSettings");
            if (settingsObj != null) {
                restoreSharedPrefs(context, "AppSettings", settingsObj, overwrite);
                result.settingsRestored = true;
            }

            JSONObject themeObj = data.optJSONObject("appTheme");
            if (themeObj != null) {
                restoreSharedPrefs(context, "AppThemePrefs", themeObj, overwrite);
            }

            // 6. Restore Room Database (Favorites)
            JSONArray favArr = data.optJSONArray("favoriteMatches");
            if (favArr != null && favArr.length() > 0) {
                try {
                    AppRoomDatabase roomDb = AppRoomDatabase.getDatabase(context);
                    for (int i = 0; i < favArr.length(); i++) {
                        JSONObject fObj = favArr.optJSONObject(i);
                        if (fObj == null) continue;
                        FavoriteMatchEntity f = new FavoriteMatchEntity();
                        f.matchId = fObj.optString("matchId", String.valueOf(System.currentTimeMillis() + i));
                        f.team1Name = fObj.optString("team1Name", "Team 1");
                        f.team2Name = fObj.optString("team2Name", "Team 2");
                        f.matchDate = fObj.optString("matchDate", "");
                        f.overs = fObj.optString("overs", "20");
                        f.matchStatus = fObj.optString("matchStatus", "");
                        f.scoreInn1 = fObj.optString("scoreInn1", "");
                        f.oversInn1 = fObj.optString("oversInn1", "");
                        f.scoreInn2 = fObj.optString("scoreInn2", "");
                        f.oversInn2 = fObj.optString("oversInn2", "");
                        f.isSecondInnings = fObj.optBoolean("isSecondInnings", false);
                        f.isLive = fObj.optBoolean("isLive", false);
                        f.savedAt = fObj.optLong("savedAt", System.currentTimeMillis());
                        roomDb.favoriteMatchDao().insertFavorite(f);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error restoring Room Favorites: " + e.getMessage());
                }
            }

            result.success = true;
        } catch (Exception e) {
            Log.e(TAG, "Import error", e);
            result.success = false;
            result.errorMessage = e.getMessage();
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FILE I/O & SHARING HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    public static boolean writeJsonToStream(OutputStream os, String jsonString) {
        if (os == null || jsonString == null) return false;
        try (OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            writer.write(jsonString);
            writer.flush();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to write JSON stream", e);
            return false;
        }
    }

    public static String readJsonFromStream(InputStream is) {
        if (is == null) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read JSON stream", e);
            return null;
        }
    }

    public static File createShareableBackupFile(Context context, String jsonString) {
        try {
            File backupDir = new File(context.getCacheDir(), "backups");
            if (!backupDir.exists()) backupDir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File file = new File(backupDir, BACKUP_FILE_PREFIX + timestamp + ".json");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                writeJsonToStream(fos, jsonString);
            }
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create shareable file", e);
            return null;
        }
    }

    public static Intent getShareIntent(Context context, File backupFile) {
        if (context == null || backupFile == null || !backupFile.exists()) return null;
        Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                backupFile
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(MIME_TYPE_JSON);
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "CricketScorez Pro Data Backup");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is the CricketScorez Pro JSON data backup file.");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(shareIntent, "Share JSON Backup");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INTERNAL UTILS
    // ─────────────────────────────────────────────────────────────────────────

    private static JSONObject exportSharedPrefs(Context context, String prefName) {
        JSONObject obj = new JSONObject();
        try {
            SharedPreferences prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            Map<String, ?> all = prefs.getAll();
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                if (val instanceof Set) {
                    JSONArray arr = new JSONArray();
                    for (Object item : (Set<?>) val) {
                        arr.put(item);
                    }
                    obj.put(key, arr);
                } else {
                    obj.put(key, val);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error exporting prefs " + prefName + ": " + e.getMessage());
        }
        return obj;
    }

    private static void restoreSharedPrefs(Context context, String prefName, JSONObject obj, boolean clearFirst) {
        if (obj == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            if (clearFirst) {
                editor.clear();
            }

            java.util.Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object val = obj.get(key);

                if (val instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) val);
                } else if (val instanceof Integer) {
                    editor.putInt(key, (Integer) val);
                } else if (val instanceof Long) {
                    editor.putLong(key, (Long) val);
                } else if (val instanceof Double || val instanceof Float) {
                    editor.putFloat(key, ((Number) val).floatValue());
                } else if (val instanceof JSONArray) {
                    JSONArray arr = (JSONArray) val;
                    Set<String> set = new HashSet<>();
                    for (int i = 0; i < arr.length(); i++) {
                        set.add(arr.optString(i));
                    }
                    editor.putStringSet(key, set);
                } else {
                    editor.putString(key, String.valueOf(val));
                }
            }
            editor.apply();
        } catch (Exception e) {
            Log.w(TAG, "Error restoring prefs " + prefName + ": " + e.getMessage());
        }
    }
}
