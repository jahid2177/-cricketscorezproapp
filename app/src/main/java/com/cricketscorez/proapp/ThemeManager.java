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
        String saved = getSavedTheme(context);
        if (THEME_SYSTEM.equals(saved)) {
            int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return (nightMode == Configuration.UI_MODE_NIGHT_YES) ? THEME_DARK : THEME_LIGHT;
        }
        return saved;
    }

    public static boolean isDarkVariant(Context context) {
        String resolved = getResolvedTheme(context);
        return !THEME_LIGHT.equals(resolved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DYNAMIC COLORS
    // ─────────────────────────────────────────────────────────────────────────
    public static int getCanvasColor(Context context) {
        String theme = getResolvedTheme(context);
        switch (theme) {
            case THEME_LIGHT:   return Color.parseColor("#F1F5F9");
            case THEME_EMERALD: return Color.parseColor("#06140B");
            case THEME_NAVY:    return Color.parseColor("#0A1227");
            case THEME_DARK:
            default:            return Color.parseColor("#0B0E14");
        }
    }

    public static int getPrimaryTextColor(Context context) {
        String theme = getResolvedTheme(context);
        switch (theme) {
            case THEME_LIGHT:   return Color.parseColor("#0F172A");
            case THEME_EMERALD: return Color.parseColor("#F0FDF4");
            case THEME_NAVY:    return Color.parseColor("#F8FAFC");
            case THEME_DARK:
            default:            return Color.parseColor("#F8FAFC");
        }
    }

    public static int getSecondaryTextColor(Context context) {
        String theme = getResolvedTheme(context);
        switch (theme) {
            case THEME_LIGHT:   return Color.parseColor("#475569");
            case THEME_EMERALD: return Color.parseColor("#86EFAC");
            case THEME_NAVY:    return Color.parseColor("#93C5FD");
            case THEME_DARK:
            default:            return Color.parseColor("#94A3B8");
        }
    }

    public static int getMutedTextColor(Context context) {
        String theme = getResolvedTheme(context);
        switch (theme) {
            case THEME_LIGHT:   return Color.parseColor("#64748B");
            case THEME_EMERALD: return Color.parseColor("#4ADE80");
            case THEME_NAVY:    return Color.parseColor("#60A5FA");
            case THEME_DARK:
            default:            return Color.parseColor("#64748B");
        }
    }

    public static int getAccentColor(Context context) {
        String theme = getResolvedTheme(context);
        switch (theme) {
            case THEME_LIGHT:   return Color.parseColor("#059669");
            case THEME_EMERALD: return Color.parseColor("#34D399");
            case THEME_NAVY:    return Color.parseColor("#38BDF8");
            case THEME_DARK:
            default:            return Color.parseColor("#10B981");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DYNAMIC DRAWABLES
    // ─────────────────────────────────────────────────────────────────────────
    public static Drawable getCanvasBackground(Context context) {
        String theme = getResolvedTheme(context);
        GradientDrawable gd;
        switch (theme) {
            case THEME_LIGHT:
                gd = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.parseColor("#EEF6FF"), Color.parseColor("#E7F1FE"), Color.parseColor("#DDEBFC")});
                break;
            case THEME_EMERALD:
                gd = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.parseColor("#07180D"), Color.parseColor("#051109"), Color.parseColor("#030A05")});
                break;
            case THEME_NAVY:
                gd = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.parseColor("#0D1630"), Color.parseColor("#091024"), Color.parseColor("#060A18")});
                break;
            case THEME_DARK:
            default:
                gd = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.parseColor("#0B0E14"), Color.parseColor("#0D1117"), Color.parseColor("#080A0E")});
                break;
        }
        return gd;
    }

    public static int getTitleDisplayColor(Context context) {
        String theme = getResolvedTheme(context);
        switch (theme) {
            case THEME_LIGHT:   return Color.parseColor("#1D4ED8");
            case THEME_EMERALD: return Color.parseColor("#34D399");
            case THEME_NAVY:    return Color.parseColor("#60A5FA");
            case THEME_DARK:
            default:            return Color.parseColor("#38BDF8");
        }
    }

    public static Drawable getPillMenuBackground(Context context) {
        String theme = getResolvedTheme(context);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 32));
        int strokeW = dp(context, 1);

        switch (theme) {
            case THEME_LIGHT:
                shape.setColor(Color.parseColor("#FFFFFF"));
                shape.setStroke(strokeW, Color.parseColor("#E2E8F0"));
                break;
            case THEME_EMERALD:
                shape.setColor(Color.parseColor("#0D2315"));
                shape.setStroke(strokeW, Color.parseColor("#1B4D2C"));
                break;
            case THEME_NAVY:
                shape.setColor(Color.parseColor("#101D40"));
                shape.setStroke(strokeW, Color.parseColor("#223977"));
                break;
            case THEME_DARK:
            default:
                shape.setColor(Color.parseColor("#141923"));
                shape.setStroke(strokeW, Color.parseColor("#252E3E"));
                break;
        }

        int rippleColor = THEME_LIGHT.equals(theme) ? Color.parseColor("#18000000") : Color.parseColor("#25FFFFFF");
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), shape, null);
    }

    public static Drawable getHeroCardBackground(Context context) {
        String theme = getResolvedTheme(context);
        GradientDrawable gd;
        int corner = dp(context, 20);
        int strokeW = dp(context, 1);

        switch (theme) {
            case THEME_LIGHT:
                gd = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.parseColor("#FFFFFF"), Color.parseColor("#F8FAFC"), Color.parseColor("#F1F5F9")});
                gd.setCornerRadius(corner);
                gd.setStroke(strokeW, Color.parseColor("#CBD5E1"));
                break;
            case THEME_EMERALD:
                gd = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.parseColor("#0E2A18"), Color.parseColor("#0A1E11"), Color.parseColor("#07160D")});
                gd.setCornerRadius(corner);
                gd.setStroke(strokeW, Color.parseColor("#1B4D2C"));
                break;
            case THEME_NAVY:
                gd = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.parseColor("#14234C"), Color.parseColor("#0F1A3A"), Color.parseColor("#0A1229")});
                gd.setCornerRadius(corner);
                gd.setStroke(strokeW, Color.parseColor("#223977"));
                break;
            case THEME_DARK:
            default:
                gd = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.parseColor("#131B24"), Color.parseColor("#101720"), Color.parseColor("#0D131A")});
                gd.setCornerRadius(corner);
                gd.setStroke(strokeW, Color.parseColor("#283547"));
                break;
        }
        return gd;
    }

    public static Drawable getCardBackground(Context context, boolean withRipple) {
        String theme = getResolvedTheme(context);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 18));
        int strokeW = dp(context, 1);

        switch (theme) {
            case THEME_LIGHT:
                shape.setColor(Color.parseColor("#FFFFFF"));
                shape.setStroke(strokeW, Color.parseColor("#CBD5E1"));
                break;
            case THEME_EMERALD:
                shape.setColor(Color.parseColor("#0D2315"));
                shape.setStroke(strokeW, Color.parseColor("#194429"));
                break;
            case THEME_NAVY:
                shape.setColor(Color.parseColor("#101D40"));
                shape.setStroke(strokeW, Color.parseColor("#1C326E"));
                break;
            case THEME_DARK:
            default:
                shape.setColor(Color.parseColor("#141922"));
                shape.setStroke(strokeW, Color.parseColor("#232B38"));
                break;
        }

        if (withRipple) {
            int rippleColor = THEME_LIGHT.equals(theme) ? Color.parseColor("#20000000") : Color.parseColor("#26FFFFFF");
            return new RippleDrawable(ColorStateList.valueOf(rippleColor), shape, null);
        }
        return shape;
    }

    public static Drawable getGlassChipBackground(Context context) {
        String theme = getResolvedTheme(context);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 16));
        int strokeW = dp(context, 1);

        switch (theme) {
            case THEME_LIGHT:
                shape.setColor(Color.parseColor("#FFFFFF"));
                shape.setStroke(strokeW, Color.parseColor("#E2E8F0"));
                break;
            case THEME_EMERALD:
                shape.setColor(Color.parseColor("#0A1C11"));
                shape.setStroke(strokeW, Color.parseColor("#163A23"));
                break;
            case THEME_NAVY:
                shape.setColor(Color.parseColor("#0E1936"));
                shape.setStroke(strokeW, Color.parseColor("#192B5C"));
                break;
            case THEME_DARK:
            default:
                shape.setColor(Color.parseColor("#131821"));
                shape.setStroke(strokeW, Color.parseColor("#212936"));
                break;
        }
        return shape;
    }

    public static Drawable getPillPresetBackground(Context context, boolean isSelected) {
        String theme = getResolvedTheme(context);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 12));
        int strokeW = dp(context, 1);

        if (isSelected) {
            shape.setColor(THEME_LIGHT.equals(theme) ? Color.parseColor("#DCFCE7") : Color.parseColor("#1A3D24"));
            shape.setStroke(dp(context, 2), getAccentColor(context));
        } else {
            switch (theme) {
                case THEME_LIGHT:
                    shape.setColor(Color.parseColor("#F1F5F9"));
                    shape.setStroke(strokeW, Color.parseColor("#CBD5E1"));
                    break;
                case THEME_EMERALD:
                    shape.setColor(Color.parseColor("#112918"));
                    shape.setStroke(strokeW, Color.parseColor("#1D4D2B"));
                    break;
                case THEME_NAVY:
                    shape.setColor(Color.parseColor("#13224B"));
                    shape.setStroke(strokeW, Color.parseColor("#203878"));
                    break;
                case THEME_DARK:
                default:
                    shape.setColor(Color.parseColor("#1A222E"));
                    shape.setStroke(strokeW, Color.parseColor("#2D3849"));
                    break;
            }
        }

        int rippleColor = THEME_LIGHT.equals(theme) ? Color.parseColor("#20000000") : Color.parseColor("#26FFFFFF");
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), shape, null);
    }

    public static Drawable getIconBadgeBackground(Context context) {
        String theme = getResolvedTheme(context);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        int strokeW = dp(context, 1);

        switch (theme) {
            case THEME_LIGHT:
                shape.setColor(Color.parseColor("#F1F5F9"));
                shape.setStroke(strokeW, Color.parseColor("#CBD5E1"));
                break;
            case THEME_EMERALD:
                shape.setColor(Color.parseColor("#102A19"));
                shape.setStroke(strokeW, Color.parseColor("#1E5030"));
                break;
            case THEME_NAVY:
                shape.setColor(Color.parseColor("#13234F"));
                shape.setStroke(strokeW, Color.parseColor("#223C82"));
                break;
            case THEME_DARK:
            default:
                shape.setColor(Color.parseColor("#1A222E"));
                shape.setStroke(strokeW, Color.parseColor("#2D3849"));
                break;
        }
        return shape;
    }

    public static Drawable getCtaButtonBackground(Context context) {
        String theme = getResolvedTheme(context);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 14));
        shape.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);

        switch (theme) {
            case THEME_NAVY:
                shape.setColors(new int[]{Color.parseColor("#0284C7"), Color.parseColor("#0EA5E9")});
                shape.setStroke(dp(context, 1), Color.parseColor("#38BDF8"));
                break;
            case THEME_EMERALD:
                shape.setColors(new int[]{Color.parseColor("#047857"), Color.parseColor("#10B981")});
                shape.setStroke(dp(context, 1), Color.parseColor("#34D399"));
                break;
            case THEME_LIGHT:
            case THEME_DARK:
            default:
                shape.setColors(new int[]{Color.parseColor("#059669"), Color.parseColor("#10B981")});
                shape.setStroke(dp(context, 1), Color.parseColor("#34D399"));
                break;
        }

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
