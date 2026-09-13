package com.cricketscorez.proapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * ThemeManager — Centralized theme engine for CricketScorez Pro.
 * Provides live runtime switching between Minimal Dark, Clean Light, Midnight Emerald, and Royal Navy.
 */
public class ThemeManager {

    public static final String PREF_NAME = "AppTheme";
    public static final String KEY_THEME = "THEME";

    public static final String THEME_DARK = "dark";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_EMERALD = "emerald";
    public static final String THEME_NAVY = "navy";
    public static final String THEME_SYSTEM = "system";

    public static String getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, THEME_LIGHT);
    }

    public static void setTheme(Context context, String themeKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, themeKey).apply();
    }

    public static String getResolvedTheme(Context context) {
        return THEME_LIGHT;
    }

    public static boolean isDarkVariant(Context context) {
        String resolved = getResolvedTheme(context);
        return !THEME_LIGHT.equals(resolved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DYNAMIC COLORS (MATCHING USER IMAGE)
    // ─────────────────────────────────────────────────────────────────────────
    public static int getCanvasColor(Context context) {
        return Color.parseColor("#FFFFFF");
    }

    public static int getPrimaryTextColor(Context context) {
        return Color.parseColor("#111827");
    }

    public static int getSecondaryTextColor(Context context) {
        return Color.parseColor("#6B7280");
    }

    public static int getMutedTextColor(Context context) {
        return Color.parseColor("#9CA3AF");
    }

    public static int getAccentColor(Context context) {
        return Color.parseColor("#059669");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DYNAMIC DRAWABLES
    // ─────────────────────────────────────────────────────────────────────────
    public static Drawable getCanvasBackground(Context context) {
        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.parseColor("#FFFFFF"), Color.parseColor("#FFFFFF")});
        return gd;
    }

    public static int getTitleDisplayColor(Context context) {
        return Color.parseColor("#111827");
    }

    public static Drawable getPillMenuBackground(Context context) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 32));
        shape.setColor(Color.parseColor("#FFFFFF"));
        shape.setStroke(dp(context, 1), Color.parseColor("#E2E8F0"));
        return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#14059669")), shape, null);
    }

    public static Drawable getHeroCardBackground(Context context) {
        GradientDrawable gd = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.parseColor("#FFFFFF"), Color.parseColor("#FFFFFF")});
        gd.setCornerRadius(dp(context, 20));
        gd.setStroke(dp(context, 1), Color.parseColor("#E2E8F0"));
        return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#14059669")), gd, null);
    }

    public static Drawable getGridCardBackground(Context context) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 18));
        shape.setColor(Color.parseColor("#FFFFFF"));
        shape.setStroke(dp(context, 1), Color.parseColor("#E2E8F0"));
        return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#14059669")), shape, null);
    }

    public static Drawable getHeaderButtonBackground(Context context) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(Color.parseColor("#FFFFFF"));
        shape.setStroke(dp(context, 1), Color.parseColor("#E2E8F0"));
        return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#14059669")), shape, null);
    }

    public static Drawable getCardBackground(Context context, boolean withRipple) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 18));
        shape.setColor(Color.parseColor("#FFFFFF"));
        shape.setStroke(dp(context, 1), Color.parseColor("#E2E8F0"));

        if (withRipple) {
            return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#14059669")), shape, null);
        }
        return shape;
    }

    public static Drawable getGlassChipBackground(Context context) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 16));
        shape.setColor(Color.parseColor("#ECFDF5"));
        shape.setStroke(dp(context, 1), Color.parseColor("#A7F3D0"));
        return shape;
    }

    public static Drawable getPillPresetBackground(Context context, boolean isSelected) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 12));
        int strokeW = dp(context, 1);

        if (isSelected) {
            shape.setColor(Color.parseColor("#ECFDF5"));
            shape.setStroke(dp(context, 2), Color.parseColor("#059669"));
        } else {
            shape.setColor(Color.parseColor("#FFFFFF"));
            shape.setStroke(strokeW, Color.parseColor("#E2E8F0"));
        }

        return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#14059669")), shape, null);
    }

    public static Drawable getIconBadgeBackground(Context context) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(Color.parseColor("#ECFDF5"));
        shape.setStroke(dp(context, 1), Color.parseColor("#A7F3D0"));
        return shape;
    }

    public static Drawable getCtaButtonBackground(Context context) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 14));
        shape.setColor(Color.parseColor("#059669"));
        return new RippleDrawable(ColorStateList.valueOf(Color.parseColor("#40FFFFFF")), shape, null);
    }

    public static void applyStatusBar(Activity activity) {
        if (activity == null || activity.getWindow() == null) return;
        Window window = activity.getWindow();

        // Skip dedicated fullscreen graph screens
        String activityName = activity.getClass().getSimpleName();
        if (activityName.contains("Graph") || activityName.contains("Chart")) {
            return;
        }

        int canvasColor = getCanvasColor(activity);
        boolean isLight = THEME_LIGHT.equals(getResolvedTheme(activity));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(canvasColor);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();
            int flags = decorView.getSystemUiVisibility();
            // Remove flags that force layout to draw behind status bar
            flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (isLight) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(flags);
        }

        // Ensure root content container respects system window insets
        View content = activity.findViewById(android.R.id.content);
        if (content != null) {
            content.setFitsSystemWindows(true);
        }
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
