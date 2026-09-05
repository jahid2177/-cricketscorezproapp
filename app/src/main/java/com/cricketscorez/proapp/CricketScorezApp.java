package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import com.google.firebase.FirebaseApp;

public class CricketScorezApp extends Application {

    private static final String TAG = "CricketScorezApp";

    @Override
    public void onCreate() {
        super.onCreate();

        initFirebaseSafely();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                ThemeManager.applyStatusBar(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(Activity activity) {
                ThemeManager.applyStatusBar(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });
    }

    private void initFirebaseSafely() {
        try {
            FirebaseApp.initializeApp(this);
        } catch (Exception e) {
            Log.w(TAG, "Firebase initialization skipped or deferred: " + e.getMessage());
        }
    }
}
