package com.cricketscorez.proapp.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "live_match_progress")
public class LiveMatchProgressEntity implements Serializable {

    @PrimaryKey
    @NonNull
    public String matchId;

    public String team1Name;
    public String team2Name;
    public String teamBattingFirst;
    public String teamBattingSecond;
    public String totalOvers;
    public int totalRuns;
    public int totalWickets;
    public int totalBallsBowled;
    public String currentScore;
    public String currentOvers;
    public String strikerName;
    public String nonStrikerName;
    public String bowlerName;
    public boolean isSecondInnings;
    public String matchStatus;
    public String serializedMatchData; // Base64 serialized match data for 100% full recovery on crash
    public long savedAt;

    public LiveMatchProgressEntity() {
        this.matchId = "active_live_match";
        this.savedAt = System.currentTimeMillis();
    }
}
