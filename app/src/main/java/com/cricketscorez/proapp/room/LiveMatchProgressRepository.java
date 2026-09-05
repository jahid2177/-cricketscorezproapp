package com.cricketscorez.proapp.room;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import com.cricketscorez.proapp.MatchData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LiveMatchProgressRepository {

    private static final String TAG = "LiveMatchAutoSave";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnMatchLoadedCallback {
        void onLoaded(MatchData matchData);
        void onNotFound();
    }

    public static void autoSave(final Context context, final MatchData matchData) {
        if (context == null || matchData == null) return;

        executor.execute(() -> {
            try {
                // Serialize MatchData object
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ObjectOutputStream so = new ObjectOutputStream(bo);
                so.writeObject(matchData);
                so.flush();
                String serialized = Base64.encodeToString(bo.toByteArray(), Base64.DEFAULT);

                LiveMatchProgressEntity entity = new LiveMatchProgressEntity();
                entity.matchId = (matchData.matchId != null && !matchData.matchId.isEmpty())
                        ? matchData.matchId : "active_live_match";
                entity.team1Name = matchData.team1Name;
                entity.team2Name = matchData.team2Name;
                entity.teamBattingFirst = matchData.teamBattingFirst;
                entity.teamBattingSecond = matchData.teamBattingSecond;
                entity.totalOvers = matchData.totalOvers;
                entity.totalRuns = matchData.totalRuns;
                entity.totalWickets = matchData.totalWickets;
                entity.totalBallsBowled = matchData.ballsBowled;
                entity.currentScore = matchData.getScoreString();
                entity.currentOvers = matchData.getOversString();
                entity.strikerName = matchData.strikerName;
                entity.nonStrikerName = matchData.nonStrikerName;
                entity.bowlerName = matchData.currentBowlerName;
                entity.isSecondInnings = matchData.isSecondInnings;
                entity.matchStatus = matchData.matchStatus;
                entity.serializedMatchData = serialized;
                entity.savedAt = System.currentTimeMillis();

                AppRoomDatabase db = AppRoomDatabase.getDatabase(context);
                db.liveMatchProgressDao().saveMatchProgress(entity);
                Log.d(TAG, "Ball-by-ball progress auto-saved to Room: " + entity.currentScore + " (" + entity.currentOvers + " ov)");
            } catch (Exception e) {
                Log.e(TAG, "Failed to auto-save to Room", e);
            }
        });
    }

    public static void getLatestActiveMatch(final Context context, final OnMatchLoadedCallback callback) {
        if (context == null) {
            if (callback != null) callback.onNotFound();
            return;
        }

        executor.execute(() -> {
            try {
                AppRoomDatabase db = AppRoomDatabase.getDatabase(context);
                LiveMatchProgressEntity entity = db.liveMatchProgressDao().getLatestActiveMatch();
                if (entity != null && entity.serializedMatchData != null && !entity.serializedMatchData.isEmpty()) {
                    MatchData matchData = deserialize(entity.serializedMatchData);
                    if (matchData != null) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onLoaded(matchData);
                        });
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load active match from Room", e);
            }

            mainHandler.post(() -> {
                if (callback != null) callback.onNotFound();
            });
        });
    }

    public static void clearMatchProgress(final Context context, final String matchId) {
        if (context == null) return;
        executor.execute(() -> {
            try {
                AppRoomDatabase db = AppRoomDatabase.getDatabase(context);
                if (matchId != null && !matchId.isEmpty()) {
                    db.liveMatchProgressDao().deleteMatchProgress(matchId);
                }
                db.liveMatchProgressDao().clearAllProgress();
                Log.d(TAG, "Live match progress cleared from Room.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear match progress", e);
            }
        });
    }

    public static MatchData deserialize(String base64Str) {
        try {
            byte[] bytes = Base64.decode(base64Str, Base64.DEFAULT);
            ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
            ObjectInputStream si = new ObjectInputStream(bi);
            return (MatchData) si.readObject();
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing MatchData", e);
            return null;
        }
    }
}
