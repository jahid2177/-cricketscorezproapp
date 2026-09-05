package com.cricketscorez.proapp.room;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.cricketscorez.proapp.MatchData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteMatchRepository {

    private final FavoriteMatchDao dao;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface Callback<T> {
        void onComplete(T result);
    }

    public FavoriteMatchRepository(Context context) {
        AppRoomDatabase db = AppRoomDatabase.getDatabase(context);
        this.dao = db.favoriteMatchDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getAllFavorites(Callback<List<FavoriteMatchEntity>> callback) {
        executor.execute(() -> {
            List<FavoriteMatchEntity> list = dao.getAllFavorites();
            if (callback != null) {
                mainHandler.post(() -> callback.onComplete(list));
            }
        });
    }

    public void isFavorite(String matchId, Callback<Boolean> callback) {
        if (matchId == null || matchId.isEmpty()) {
            if (callback != null) callback.onComplete(false);
            return;
        }
        executor.execute(() -> {
            boolean fav = dao.isFavorite(matchId);
            if (callback != null) {
                mainHandler.post(() -> callback.onComplete(fav));
            }
        });
    }

    public void toggleFavorite(MatchData matchData, Callback<Boolean> callback) {
        if (matchData == null) {
            if (callback != null) callback.onComplete(false);
            return;
        }
        String id = matchData.matchId != null && !matchData.matchId.isEmpty()
                ? matchData.matchId
                : (matchData.team1Name + "_" + matchData.team2Name + "_" + (matchData.matchDate != null ? matchData.matchDate : ""));

        executor.execute(() -> {
            boolean currentFav = dao.isFavorite(id);
            if (currentFav) {
                dao.deleteFavoriteById(id);
                if (callback != null) {
                    mainHandler.post(() -> callback.onComplete(false));
                }
            } else {
                String score1 = matchData.scoreInn1;
                String overs1 = matchData.oversInn1;
                String score2 = "";
                String overs2 = "";

                if (matchData.isSecondInnings) {
                    score2 = matchData.totalRuns + "/" + matchData.totalWickets;
                    overs2 = matchData.currentOvers + "." + matchData.currentBalls;
                } else {
                    if (score1 == null || score1.isEmpty()) {
                        score1 = matchData.totalRuns + "/" + matchData.totalWickets;
                    }
                    if (overs1 == null || overs1.isEmpty()) {
                        overs1 = matchData.currentOvers + "." + matchData.currentBalls;
                    }
                }

                boolean isLive = matchData.matchStatus == null
                        || matchData.matchStatus.equalsIgnoreCase("Incomplete")
                        || matchData.matchStatus.equalsIgnoreCase("In Progress")
                        || matchData.matchStatus.equalsIgnoreCase("Live");

                FavoriteMatchEntity entity = new FavoriteMatchEntity(
                        id,
                        matchData.team1Name != null ? matchData.team1Name : "Team 1",
                        matchData.team2Name != null ? matchData.team2Name : "Team 2",
                        matchData.matchDate != null ? matchData.matchDate : "",
                        matchData.totalOvers != null ? matchData.totalOvers : "20",
                        matchData.matchStatus != null ? matchData.matchStatus : "Live",
                        score1 != null ? score1 : "0/0",
                        overs1 != null ? overs1 : "0.0",
                        score2,
                        overs2,
                        matchData.isSecondInnings,
                        isLive
                );
                dao.insertFavorite(entity);
                if (callback != null) {
                    mainHandler.post(() -> callback.onComplete(true));
                }
            }
        });
    }

    public void removeFavorite(String matchId, Callback<Void> callback) {
        if (matchId == null || matchId.isEmpty()) return;
        executor.execute(() -> {
            dao.deleteFavoriteById(matchId);
            if (callback != null) {
                mainHandler.post(() -> callback.onComplete(null));
            }
        });
    }
}
