package com.cricketscorez.proapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ImageStorageHelper
 * Handles persistent offline storage, scaling, compression,
 * circular masking, and asynchronous loading of Player and Tournament pictures.
 */
public class ImageStorageHelper {

    private static final String PREF_DB = "CricketScorezDB";
    private static final String PREF_TOURNAMENT = "TournamentData";
    private static final String KEY_TOURNAMENT_LOGO = "TOURNAMENT_LOGO_PATH";

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ─────────────────────────────────────────────────────────────────────────
    //  PLAYER PHOTO MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    public static String savePlayerPhoto(Context context, String playerName, Uri uri) {
        if (context == null || playerName == null || uri == null) return null;
        try {
            String safeName = sanitizeFilename(playerName);
            String savedPath = saveImageInternal(context, uri, "player_photos", "player_" + safeName, 512);
            if (savedPath != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREF_DB, Context.MODE_PRIVATE);
                prefs.edit().putString("PHOTO_" + playerName, savedPath).apply();
            }
            return savedPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getPlayerPhotoPath(Context context, String playerName) {
        if (context == null || playerName == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_DB, Context.MODE_PRIVATE);
        String path = prefs.getString("PHOTO_" + playerName, null);
        if (path != null && new File(path).exists()) {
            return path;
        }
        return null;
    }

    public static void deletePlayerPhoto(Context context, String playerName) {
        if (context == null || playerName == null) return;
        try {
            String path = getPlayerPhotoPath(context, playerName);
            if (path != null) {
                File f = new File(path);
                if (f.exists()) f.delete();
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_DB, Context.MODE_PRIVATE);
            prefs.edit().remove("PHOTO_" + playerName).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadPlayerPhotoInto(final Context context, final String playerName,
                                          final ImageView imageView, final View fallbackView) {
        if (imageView == null) return;

        final String path = getPlayerPhotoPath(context, playerName);
        if (path == null) {
            imageView.setVisibility(View.GONE);
            if (fallbackView != null) fallbackView.setVisibility(View.VISIBLE);
            return;
        }

        // Tag view to avoid mismatched recycling in lists
        imageView.setTag(path);

        executor.execute(() -> {
            try {
                Bitmap bitmap = decodeSampledBitmapFromFile(path, 120, 120);
                if (bitmap != null) {
                    final Bitmap circular = getCircularBitmap(bitmap);
                    mainHandler.post(() -> {
                        if (path.equals(imageView.getTag())) {
                            imageView.setImageBitmap(circular);
                            imageView.setVisibility(View.VISIBLE);
                            if (fallbackView != null) fallbackView.setVisibility(View.GONE);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        imageView.setVisibility(View.GONE);
                        if (fallbackView != null) fallbackView.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    imageView.setVisibility(View.GONE);
                    if (fallbackView != null) fallbackView.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TEAM LOGO / PICTURE MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    public static String saveTeamLogo(Context context, String teamName, Uri uri) {
        if (context == null || teamName == null || uri == null) return null;
        try {
            String safeName = sanitizeFilename(teamName);
            String savedPath = saveImageInternal(context, uri, "team_logos", "team_" + safeName, 512);
            if (savedPath != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREF_DB, Context.MODE_PRIVATE);
                prefs.edit().putString("TEAM_LOGO_" + teamName, savedPath).apply();
            }
            return savedPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getTeamLogoPath(Context context, String teamName) {
        if (context == null || teamName == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_DB, Context.MODE_PRIVATE);
        String path = prefs.getString("TEAM_LOGO_" + teamName, null);
        if (path != null && new File(path).exists()) {
            return path;
        }
        return null;
    }

    public static void deleteTeamLogo(Context context, String teamName) {
        if (context == null || teamName == null) return;
        try {
            String path = getTeamLogoPath(context, teamName);
            if (path != null) {
                File f = new File(path);
                if (f.exists()) f.delete();
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_DB, Context.MODE_PRIVATE);
            prefs.edit().remove("TEAM_LOGO_" + teamName).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void renameTeamLogo(Context context, String oldTeamName, String newTeamName) {
        if (context == null || oldTeamName == null || newTeamName == null) return;
        String path = getTeamLogoPath(context, oldTeamName);
        if (path != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_DB, Context.MODE_PRIVATE);
            prefs.edit().putString("TEAM_LOGO_" + newTeamName, path).remove("TEAM_LOGO_" + oldTeamName).apply();
        }
    }

    public static void loadTeamLogoInto(final Context context, final String teamName,
                                       final ImageView imageView, final View fallbackView) {
        if (imageView == null) return;

        final String path = getTeamLogoPath(context, teamName);
        if (path == null) {
            imageView.setVisibility(View.GONE);
            if (fallbackView != null) fallbackView.setVisibility(View.VISIBLE);
            return;
        }

        imageView.setTag(path);

        executor.execute(() -> {
            try {
                Bitmap bitmap = decodeSampledBitmapFromFile(path, 160, 160);
                if (bitmap != null) {
                    final Bitmap circular = getCircularBitmap(bitmap);
                    mainHandler.post(() -> {
                        if (path.equals(imageView.getTag())) {
                            imageView.setImageBitmap(circular);
                            imageView.setVisibility(View.VISIBLE);
                            if (fallbackView != null) fallbackView.setVisibility(View.GONE);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        imageView.setVisibility(View.GONE);
                        if (fallbackView != null) fallbackView.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    imageView.setVisibility(View.GONE);
                    if (fallbackView != null) fallbackView.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TOURNAMENT LOGO / PICTURE MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    public static String saveTournamentLogo(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        try {
            String savedPath = saveImageInternal(context, uri, "tournament", "tournament_logo", 1024);
            if (savedPath != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREF_TOURNAMENT, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_TOURNAMENT_LOGO, savedPath).apply();
            }
            return savedPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getTournamentLogoPath(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_TOURNAMENT, Context.MODE_PRIVATE);
        String path = prefs.getString(KEY_TOURNAMENT_LOGO, null);
        if (path != null && new File(path).exists()) {
            return path;
        }
        // Fallback check legacy URI
        String legacyUri = prefs.getString("TOURNAMENT_LOGO_URI", null);
        if (legacyUri != null && legacyUri.startsWith("/")) {
            if (new File(legacyUri).exists()) return legacyUri;
        }
        return null;
    }

    public static void deleteTournamentLogo(Context context) {
        if (context == null) return;
        try {
            String path = getTournamentLogoPath(context);
            if (path != null) {
                File f = new File(path);
                if (f.exists()) f.delete();
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_TOURNAMENT, Context.MODE_PRIVATE);
            prefs.edit().remove(KEY_TOURNAMENT_LOGO).remove("TOURNAMENT_LOGO_URI").apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadTournamentLogoInto(final Context context, final ImageView imageView, final int fallbackResId) {
        if (imageView == null) return;

        final String path = getTournamentLogoPath(context);
        if (path == null) {
            if (fallbackResId != 0) {
                imageView.setImageResource(fallbackResId);
            }
            return;
        }

        imageView.setTag(path);

        executor.execute(() -> {
            try {
                Bitmap bitmap = decodeSampledBitmapFromFile(path, 400, 400);
                if (bitmap != null) {
                    final Bitmap rounded = getRoundedBitmap(bitmap, 24);
                    mainHandler.post(() -> {
                        if (path.equals(imageView.getTag())) {
                            imageView.setImageBitmap(rounded);
                        }
                    });
                } else if (fallbackResId != 0) {
                    mainHandler.post(() -> imageView.setImageResource(fallbackResId));
                }
            } catch (Exception e) {
                if (fallbackResId != 0) {
                    mainHandler.post(() -> imageView.setImageResource(fallbackResId));
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  IMAGE PROCESSING & SCALING UTILS
    // ─────────────────────────────────────────────────────────────────────────

    private static String saveImageInternal(Context context, Uri sourceUri, String subDir, String fileName, int maxDimension) {
        try {
            File dir = new File(context.getFilesDir(), subDir);
            if (!dir.exists()) dir.mkdirs();

            File destFile = new File(dir, fileName + "_" + System.currentTimeMillis() + ".jpg");

            // 1. Decode bounds first
            InputStream is = context.getContentResolver().openInputStream(sourceUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            if (is != null) is.close();

            int sampleSize = 1;
            int width = options.outWidth;
            int height = options.outHeight;
            while (width / 2 >= maxDimension || height / 2 >= maxDimension) {
                width /= 2;
                height /= 2;
                sampleSize *= 2;
            }

            // 2. Decode sampled bitmap
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            is = context.getContentResolver().openInputStream(sourceUri);
            Bitmap original = BitmapFactory.decodeStream(is, null, options);
            if (is != null) is.close();

            if (original == null) return null;

            // 3. Fix rotation if available
            Bitmap rotated = handleRotation(context, sourceUri, original);

            // 4. Save to local storage
            FileOutputStream fos = new FileOutputStream(destFile);
            rotated.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            return destFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        int sampleSize = 1;
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            final int halfHeight = options.outHeight / 2;
            final int halfWidth = options.outWidth / 2;
            while ((halfHeight / sampleSize) >= reqHeight && (halfWidth / sampleSize) >= reqWidth) {
                sampleSize *= 2;
            }
        }
        options.inSampleSize = sampleSize;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect srcRect = new Rect(
                (bitmap.getWidth() - size) / 2,
                (bitmap.getHeight() - size) / 2,
                (bitmap.getWidth() + size) / 2,
                (bitmap.getHeight() + size) / 2
        );
        final Rect dstRect = new Rect(0, 0, size, size);
        final RectF rectF = new RectF(dstRect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xFF424242);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint);

        return output;
    }

    public static Bitmap getRoundedBitmap(Bitmap bitmap, int cornerRadiusPx) {
        if (bitmap == null) return null;
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xFF424242);
        canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private static Bitmap handleRotation(Context context, Uri uri, Bitmap bitmap) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) return bitmap;
            ExifInterface ei = new ExifInterface(input);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            input.close();

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateBitmap(bitmap, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateBitmap(bitmap, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateBitmap(bitmap, 270);
                default:
                    return bitmap;
            }
        } catch (Exception ignored) {
            return bitmap;
        }
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
}
