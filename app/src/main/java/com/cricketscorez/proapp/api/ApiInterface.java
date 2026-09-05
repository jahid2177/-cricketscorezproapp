package com.cricketscorez.proapp.api;

// আপনার মডেল ক্লাসগুলোর ইমপোর্ট
import com.cricketscorez.proapp.models.Team;
import com.cricketscorez.proapp.models.Tournament;
import com.cricketscorez.proapp.models.Player;
import com.cricketscorez.proapp.models.PlayerStat;
import com.cricketscorez.proapp.models.Match;
import com.cricketscorez.proapp.models.MatchHistory;
import com.cricketscorez.proapp.models.PointTable;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiInterface {

    @GET("get_tournaments.php")
    Call<List<Tournament>> getTournaments();

    @GET("get_teams.php")
    Call<List<Team>> getTeams();

    @GET("get_players.php")
    Call<List<Player>> getPlayers(@Query("team_id") int teamId);

    @GET("get_player_stats.php")
    Call<List<PlayerStat>> getPlayerStats(@Query("player_id") int playerId);

    @GET("get_matches.php")
    Call<List<Match>> getMatches(@Query("status") String status);

    @GET("get_history.php")
    Call<List<MatchHistory>> getHistory();

    @GET("get_points_table.php")
    Call<List<PointTable>> getPointsTable(@Query("tournament_id") int tournamentId);

    // ─── Team Management API ──────────────────────────────────────────

    // ★ নতুন টিম সার্ভারে সেভ করার জন্য API কল
    @FormUrlEncoded
    @POST("add_team.php")
    Call<Void> addTeam(
        @Field("name") String teamName,
        @Field("short_name") String shortName
    );

    // ★ টিম আপডেট করার জন্য API কল
    @FormUrlEncoded
    @POST("update_team.php")
    Call<Void> updateTeam(
        @Field("old_name") String oldName,
        @Field("new_name") String newName,
        @Field("short_name") String shortName
    );

    // ★ টিম ডিলিট করার জন্য API কল
    @FormUrlEncoded
    @POST("delete_team.php")
    Call<Void> deleteTeam(
        @Field("name") String teamName
    );

    // ─── Player Management API ────────────────────────────────────────

    // ★ নতুন প্লেয়ার সার্ভারে সেভ করার জন্য
    @FormUrlEncoded
    @POST("add_player.php")
    Call<Void> addPlayer(
        @Field("team_name") String teamName,
        @Field("player_name") String playerName,
        @Field("role") String role
    );

    // ★ প্লেয়ারের নাম আপডেট করার জন্য
    @FormUrlEncoded
    @POST("update_player.php")
    Call<Void> updatePlayer(
        @Field("team_name") String teamName,
        @Field("old_name") String oldName,
        @Field("new_name") String newName
    );

    // ★ প্লেয়ার ডিলিট করার জন্য
    @FormUrlEncoded
    @POST("delete_player.php")
    Call<Void> deletePlayer(
        @Field("team_name") String teamName,
        @Field("player_name") String playerName
    );

    // ─── Player Stats Management API ──────────────────────────────────

    // ★ প্লেয়ারের স্ট্যাটাস (Batting, Bowling, Fielding) আপডেট করার জন্য
    @FormUrlEncoded
    @POST("update_player_stats.php")
    Call<Void> updatePlayerStats(
        @Field("team_name") String teamName,
        @Field("player_name") String playerName,
        @Field("matches") int matches,
        @Field("innings") int innings,
        @Field("runs") int runs,
        @Field("balls_faced") int ballsFaced,
        @Field("highest_score") int highestScore,
        @Field("fifties") int fifties,
        @Field("hundreds") int hundreds,
        @Field("fours") int fours,
        @Field("sixes") int sixes,
        @Field("wickets") int wickets,
        @Field("balls_bowled") int ballsBowled,
        @Field("runs_conceded") int runsConceded,
        @Field("maidens") int maidens,
        @Field("catches") int catches,
        @Field("stumpings") int stumpings,
        @Field("run_outs") int runOuts
    );

    // ─── Tournament Management API ──────────────────────────────────────

    // ★ টুর্নামেন্ট সেভ/আপডেট করার জন্য
    @FormUrlEncoded
    @POST("add_tournament.php")
    Call<Void> saveTournament(
        @Field("name") String name,
        @Field("overs") String overs,
        @Field("win_pts") String winPts,
        @Field("tie_pts") String tiePts
    );

    // ★ টুর্নামেন্ট ডিলিট/রিসেট করার জন্য
    @FormUrlEncoded
    @POST("delete_tournament.php")
    Call<Void> deleteTournament(
        @Field("name") String name
    );
    // ─── Match / Fixture Management API ─────────────────────────────────

    // ★ নতুন ম্যাচ সেভ করার জন্য
    @FormUrlEncoded
    @POST("add_match.php")
    Call<Void> addMatch(
        @Field("team1_name") String team1Name,
        @Field("team2_name") String team2Name,
        @Field("match_date") String matchDate,
        @Field("venue") String venue
    );

    // ★ কোনো ম্যাচ ডিলিট করার জন্য
    @FormUrlEncoded
    @POST("delete_match.php")
    Call<Void> deleteMatch(
        @Field("match_id") String matchId
    );
    // ★ লাইভ স্কোর আপডেট করার জন্য
    @FormUrlEncoded
    @POST("update_score.php")
    Call<Void> updateLiveScore(
        @Field("match_id") String matchId,
        @Field("runs") int runs,
        @Field("wickets") int wickets,
        @Field("overs") String overs,
        @Field("target") int target,
        @Field("current_innings") int currentInnings,
        @Field("status") String status
    );
}
