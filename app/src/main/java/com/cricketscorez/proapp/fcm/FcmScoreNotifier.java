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
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.cricketscorez.proapp.HomeActivity;
import com.cricketscorez.proapp.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class FcmScoreNotifier {

    private static final String TAG = "FcmScoreNotifier";

    /**
     * Checks if Firebase is initialized with valid production credentials.
     * Prevents FIS_AUTH_ERROR exceptions when placeholder/dummy keys are present.
     */
    public static boolean isFirebaseConfigured(Context context) {
        try {
            FirebaseApp app = FirebaseApp.getInstance();
            if (app == null) return false;
            FirebaseOptions options = app.getOptions();
            if (options == null) return false;
            String apiKey = options.getApiKey();
            if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("DummyKey") || apiKey.startsWith("AIzaSyDummy")) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void subscribeToLiveUpdates(Context context) {
        // Topic sync is optional/dormant to avoid FCM TOO_MANY_REGISTRATIONS on emulators/test devices.
        // Real-time live score updates are powered directly by Firebase Realtime Database.
    }

    public static void subscribeToMatch(Context context, String matchId) {
        // Topic sync is optional/dormant to avoid FCM TOO_MANY_REGISTRATIONS on emulators/test devices.
    }

    public static void subscribeToMatch(String matchId) {
        subscribeToMatch(null, matchId);
    }

    public static void sendLocalScoreAlert(Context context, String matchTitle, String scoreUpdate, String status) {
        if (context == null) return;
        try {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CricketMessagingService.CHANNEL_ID_LIVE_SCORES,
                        CricketMessagingService.CHANNEL_NAME_LIVE_SCORES,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.enableLights(true);
                channel.setLightColor(Color.GREEN);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            }

            Intent intent = new Intent(context, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    (int) System.currentTimeMillis(),
                    intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
            );

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context, CricketMessagingService.CHANNEL_ID_LIVE_SCORES)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setContentTitle("🏏 " + matchTitle)
                            .setContentText(scoreUpdate + " • " + status)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(scoreUpdate + "\n" + status))
                            .setAutoCancel(true)
                            .setSound(defaultSoundUri)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntent);

            if (notificationManager != null) {
                notificationManager.notify((int) (System.currentTimeMillis() % 100000), builder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
