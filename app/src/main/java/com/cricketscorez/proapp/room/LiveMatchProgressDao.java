package com.cricketscorez.proapp.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface LiveMatchProgressDao {

    @Query("SELECT * FROM live_match_progress ORDER BY savedAt DESC LIMIT 1")
    LiveMatchProgressEntity getLatestActiveMatch();

    @Query("SELECT * FROM live_match_progress WHERE matchId = :matchId LIMIT 1")
    LiveMatchProgressEntity getMatchProgressById(String matchId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveMatchProgress(LiveMatchProgressEntity progress);

    @Query("DELETE FROM live_match_progress WHERE matchId = :matchId")
    void deleteMatchProgress(String matchId);

    @Query("DELETE FROM live_match_progress")
    void clearAllProgress();
}
