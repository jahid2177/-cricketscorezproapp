package com.cricketscorez.proapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class DataSyncService extends Service {

    private static final String CHANNEL_ID = "DataSyncChannel";
    private static final int NOTIFICATION_ID = 123;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Foreground Service চালু করার জন্য নোটিফিকেশন দরকার
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
				.setContentTitle("Cricket Scorez Pro")
				.setContentText("Syncing live match data...")
				.setSmallIcon(android.R.drawable.ic_popup_sync)
				.build();
        }

        // সার্ভিস স্টার্ট করা
        startForeground(NOTIFICATION_ID, notification);

        // 🔥 এখানে আপনার ব্যাকগ্রাউন্ড ডাটা সিঙ্কের কোড বসাতে পারেন
        // উদাহরণ: performNetworkSync();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
				CHANNEL_ID,
				"Data Sync Channel",
				NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}

