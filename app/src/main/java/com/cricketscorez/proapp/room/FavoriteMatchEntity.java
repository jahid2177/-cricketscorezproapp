package com.cricketscorez.proapp.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "favorite_matches")
public class FavoriteMatchEntity implements Serializable {

    @PrimaryKey
    @NonNull
    public String matchId;

    public String team1Name;
    public String team2Name;
    public String matchDate;
    public String overs;
    public String matchStatus;

    public String scoreInn1;
    public String oversInn1;
    public String scoreInn2;
    public String oversInn2;
    public boolean isSecondInnings;
    public boolean isLive;

    public long savedAt;

    public FavoriteMatchEntity() {
        this.matchId = "";
        this.savedAt = System.currentTimeMillis();
    }

    @androidx.room.Ignore
    public FavoriteMatchEntity(@NonNull String matchId, String team1Name, String team2Name,
                               String matchDate, String overs, String matchStatus,
                               String scoreInn1, String oversInn1, String scoreInn2,
                               String oversInn2, boolean isSecondInnings, boolean isLive) {
        this.matchId = matchId;
        this.team1Name = team1Name;
        this.team2Name = team2Name;
        this.matchDate = matchDate;
        this.overs = overs;
        this.matchStatus = matchStatus;
        this.scoreInn1 = scoreInn1;
        this.oversInn1 = oversInn1;
        this.scoreInn2 = scoreInn2;
        this.oversInn2 = oversInn2;
        this.isSecondInnings = isSecondInnings;
        this.isLive = isLive;
        this.savedAt = System.currentTimeMillis();
    }
}
