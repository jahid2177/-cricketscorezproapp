package com.cricketscorez.proapp.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FavoriteMatchDao {

    @Query("SELECT * FROM favorite_matches ORDER BY savedAt DESC")
    List<FavoriteMatchEntity> getAllFavorites();

    @Query("SELECT * FROM favorite_matches WHERE matchId = :matchId LIMIT 1")
    FavoriteMatchEntity getFavoriteById(String matchId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavorite(FavoriteMatchEntity match);

    @Query("DELETE FROM favorite_matches WHERE matchId = :matchId")
    void deleteFavoriteById(String matchId);

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_matches WHERE matchId = :matchId)")
    boolean isFavorite(String matchId);

    @Query("DELETE FROM favorite_matches")
    void clearAllFavorites();
}
