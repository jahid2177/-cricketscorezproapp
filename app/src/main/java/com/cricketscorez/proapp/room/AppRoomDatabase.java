package com.cricketscorez.proapp.room;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {FavoriteMatchEntity.class, LiveMatchProgressEntity.class}, version = 2, exportSchema = false)
public abstract class AppRoomDatabase extends RoomDatabase {

    private static volatile AppRoomDatabase INSTANCE;

    public abstract FavoriteMatchDao favoriteMatchDao();
    public abstract LiveMatchProgressDao liveMatchProgressDao();

    public static AppRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppRoomDatabase.class,
                            "cricket_scorez_room.db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
