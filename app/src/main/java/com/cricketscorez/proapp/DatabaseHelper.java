package com.cricketscorez.proapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "CricketPro.db";
    private static final int DATABASE_VERSION = 2; // ভার্সন পরিবর্তন করা হলো কারণ নতুন টেবিল যোগ হচ্ছে

    // Team Table
    private static final String TABLE_TEAMS = "teams";
    private static final String COL_TEAM_NAME = "name";

    // Player Table
    private static final String TABLE_PLAYERS = "players";
    private static final String COL_PLAYER_ID = "id";
    private static final String COL_PLAYER_NAME = "name";
    private static final String COL_PLAYER_ROLE = "role";
    private static final String COL_TEAM_REF = "team_name"; // কোন টিমের প্লেয়ার

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTeamTable = "CREATE TABLE " + TABLE_TEAMS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_TEAM_NAME + " TEXT)";

        // প্লেয়ার টেবিল তৈরি
        String createPlayerTable = "CREATE TABLE " + TABLE_PLAYERS + " (" +
            COL_PLAYER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_PLAYER_NAME + " TEXT, " +
            COL_PLAYER_ROLE + " TEXT, " +
            COL_TEAM_REF + " TEXT)";

        db.execSQL(createTeamTable);
        db.execSQL(createPlayerTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEAMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYERS);
        onCreate(db);
    }

    // --- TEAM METHODS ---
    public boolean addTeam(String teamName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TEAM_NAME, teamName);
        return db.insert(TABLE_TEAMS, null, cv) != -1;
    }

    public ArrayList<String> getAllTeams() {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TEAMS, null);
        if (cursor.moveToFirst()) {
            do { list.add(cursor.getString(1)); } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public boolean updateTeam(String oldName, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TEAM_NAME, newName);
        return db.update(TABLE_TEAMS, cv, COL_TEAM_NAME + "=?", new String[]{oldName}) != -1;
    }

    public boolean deleteTeam(String teamName) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_TEAMS, COL_TEAM_NAME + "=?", new String[]{teamName}) != -1;
    }

    // --- PLAYER METHODS (NEW) ---

    // প্লেয়ার অ্যাড করা
    public boolean addPlayer(String name, String role, String teamName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PLAYER_NAME, name);
        cv.put(COL_PLAYER_ROLE, role);
        cv.put(COL_TEAM_REF, teamName);
        return db.insert(TABLE_PLAYERS, null, cv) != -1;
    }

    // নির্দিষ্ট টিমের প্লেয়ার লোড করা
    public Cursor getPlayersByTeam(String teamName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_PLAYERS + " WHERE " + COL_TEAM_REF + "=?", new String[]{teamName});
    }

    // প্লেয়ার আপডেট
    public boolean updatePlayer(int id, String name, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PLAYER_NAME, name);
        cv.put(COL_PLAYER_ROLE, role);
        return db.update(TABLE_PLAYERS, cv, COL_PLAYER_ID + "=?", new String[]{String.valueOf(id)}) != -1;
    }

    // প্লেয়ার ডিলিট
    public boolean deletePlayer(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_PLAYERS, COL_PLAYER_ID + "=?", new String[]{String.valueOf(id)}) != -1;
    }

    // --- MATCH HISTORY METHODS ---

    // ম্যাচ হিস্ট্রি ক্লিয়ার করার মেথড (এই মেথডটি মিসিং ছিল)
    public void clearAllMatchHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // যদি আপনার ডাটাবেসে হিস্ট্রি টেবিলের নাম আলাদা হয়, তবে "match_history" পরিবর্তন করে সেটি দিন
            db.execSQL("DELETE FROM match_history"); 
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
    }
}
