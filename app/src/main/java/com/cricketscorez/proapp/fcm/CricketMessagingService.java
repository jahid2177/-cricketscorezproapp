package com.cricketscorez.proapp.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.cricketscorez.proapp.HomeActivity;
import com.cricketscorez.proapp.R;
import java.util.Map;

public class CricketMessagingService {

    private static final String TAG = "CricketNotification";
    public static final String CHANNEL_ID_LIVE_SCORES = "cricket_live_score_channel";
    public static final String CHANNEL_NAME_LIVE_SCORES = "Live Cricket Match Scores & Updates";

    public static void sendScoreNotification(Context context, String title, String messageBody, Map<String, String> data) {
        if (context == null) return;
        Intent intent = new Intent(context, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID_LIVE_SCORES,
                    CHANNEL_NAME_LIVE_SCORES,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Real-time notifications for live cricket matches, boundaries, wickets and win probability.");
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CHANNEL_ID_LIVE_SCORES)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify((int) (System.currentTimeMillis() % 100000), notificationBuilder.build());
        }
    }
}
