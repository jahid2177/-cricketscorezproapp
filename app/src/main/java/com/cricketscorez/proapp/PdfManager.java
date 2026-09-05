package com.cricketscorez.proapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class PdfManager {

    private static final int PAGE_WIDTH = 595;   // A4
    private static final int PAGE_HEIGHT = 842;

    public static void generateScorecard(Context context, MatchData matchData) {
        if (matchData == null) {
            Toast.makeText(context, "No match data available to export", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        drawInningsPage(document, paint, titlePaint, matchData, 1);

        if (matchData.isSecondInnings) {
            drawInningsPage(document, paint, titlePaint, matchData, 2);
        }

        String fileName = "Scorecard_" + System.currentTimeMillis() + ".pdf";
        saveAndOpenPdf(context, document, fileName);
    }

    public static Uri saveAndOpenPdf(Context context, PdfDocument document, String fileName) {
        return saveAndOpenPdf(context, document, fileName, "Document Downloaded \u2705");
    }

    public static Uri saveAndOpenPdf(Context context, PdfDocument document, String fileName, String docTitle) {
        Uri savedUri = null;
        File fallbackFile = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                savedUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (savedUri != null) {
                    try (OutputStream outputStream = resolver.openOutputStream(savedUri)) {
                        if (outputStream != null) {
                            document.writeTo(outputStream);
                        }
                    }
                }
            }

            if (savedUri == null) {
                // Fallback for older devices or when MediaStore is unavailable
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (downloadsDir != null && !downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                fallbackFile = new File(downloadsDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(fallbackFile)) {
                    document.writeTo(fos);
                } catch (Exception e) {
                    // Internal cache fallback
                    fallbackFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
                    try (FileOutputStream fos = new FileOutputStream(fallbackFile)) {
                        document.writeTo(fos);
                    }
                }

                if (fallbackFile != null && fallbackFile.exists()) {
                    savedUri = FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".provider",
                            fallbackFile);
                }
            }

            document.close();

            Toast.makeText(context, "PDF saved to Downloads folder!", Toast.LENGTH_LONG).show();
            showDownloadNotification(context, fileName, docTitle, savedUri, fallbackFile);
            promptOpenPdf(context, savedUri);

            return savedUri;

        } catch (Exception e) {
            try { document.close(); } catch (Exception ignored) {}
            Toast.makeText(context, "PDF Save Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public static void promptOpenPdf(Context context, Uri uri) {
        if (uri == null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, "Open PDF"));
        } catch (Exception e) {
            // If no PDF reader app found
            Toast.makeText(context, "Saved to Downloads. Open with any PDF reader.", Toast.LENGTH_SHORT).show();
        }
    }

    // 🔔 Notification on PDF Download
    public static void showDownloadNotification(Context context, String fileName, Uri pdfUri, File fallbackFile) {
        showDownloadNotification(context, fileName, "Scorecard Downloaded \u2705", pdfUri, fallbackFile);
    }

    public static void showDownloadNotification(Context context, String fileName, String title, Uri pdfUri, File fallbackFile) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            String channelId = "pdf_download_channel";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        channelId, "PDF Downloads", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Notifications for downloaded documents and scorecards");
                channel.enableVibration(true);
                channel.setShowBadge(true);
                nm.createNotificationChannel(channel);
            }

            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            if (pdfUri != null) {
                openIntent.setDataAndType(pdfUri, "application/pdf");
            } else if (fallbackFile != null) {
                Uri fileUri = FileProvider.getUriForFile(
                        context, context.getPackageName() + ".provider", fallbackFile);
                openIntent.setDataAndType(fileUri, "application/pdf");
            }
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, (int) System.currentTimeMillis(),
                    Intent.createChooser(openIntent, "Open PDF"),
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            android.app.Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    ? new android.app.Notification.Builder(context, channelId)
                    : new android.app.Notification.Builder(context);

            builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentTitle(title != null ? title : "Document Downloaded \u2705")
                    .setContentText(fileName + " — Tap to view")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            nm.notify((int) System.currentTimeMillis(), builder.build());
        } catch (Exception ignored) {}
    }

    // ================= DRAW PDF =====================
    private static void drawInningsPage(PdfDocument document, Paint paint, Paint titlePaint,
                                        MatchData matchData, int inningsNum) {

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, inningsNum).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = 50;

        titlePaint.setTextSize(22);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(matchData.team1Name + " v/s " + matchData.team2Name,
                PAGE_WIDTH / 2f, y, titlePaint);
        y += 30;

        paint.setTextSize(14);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(matchData.matchStatus != null ? matchData.matchStatus : "Scorecard", 20, y, paint);
        y += 20;

        String teamName = (inningsNum == 1) ? matchData.teamBattingFirst : matchData.teamBattingSecond;
        String score = (inningsNum == 1) ? matchData.scoreInn1 : matchData.getScoreString();
        String overs = (inningsNum == 1) ? matchData.oversInn1 : matchData.getOversString();

        ArrayList<String[]> batsmen =
                (inningsNum == 1) ? matchData.batsmanHistoryInn1 : matchData.getAllBattingStats();
        ArrayList<String[]> bowlers =
                (inningsNum == 1) ? matchData.bowlerHistoryInn1 : matchData.getAllBowlingStats();

        paint.setColor(Color.parseColor("#1B5E20"));
        canvas.drawRect(10, y, PAGE_WIDTH - 10, y + 25, paint);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(teamName != null ? teamName : "Team", 20, y + 17, paint);
        canvas.drawText(score + " (" + overs + ")", PAGE_WIDTH - 150, y + 17, paint);
        y += 40;

        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        canvas.drawText("Batsman", 20, y, paint);
        canvas.drawText("R", 300, y, paint);
        canvas.drawText("B", 360, y, paint);
        canvas.drawText("4s", 420, y, paint);
        canvas.drawText("6s", 480, y, paint);
        canvas.drawText("SR", 540, y, paint);
        y += 20;

        if (batsmen != null) {
            for (String[] bat : batsmen) {
                if (bat == null || bat.length < 6) continue;
                String[] parts = (bat[0] != null ? bat[0] : "").split("\n");
                canvas.drawText(parts[0], 20, y, paint);
                if (parts.length > 1) {
                    paint.setTextSize(9);
                    canvas.drawText(parts[1], 20, y + 12, paint);
                    paint.setTextSize(12);
                }
                canvas.drawText(bat[1] != null ? bat[1] : "-", 300, y, paint);
                canvas.drawText(bat[2] != null ? bat[2] : "-", 360, y, paint);
                canvas.drawText(bat[3] != null ? bat[3] : "-", 420, y, paint);
                canvas.drawText(bat[4] != null ? bat[4] : "-", 480, y, paint);
                canvas.drawText(bat[5] != null ? bat[5] : "-", 540, y, paint);
                y += 30;
            }
        }

        y += 10;
        canvas.drawText("Extras: " +
                        ((inningsNum == 1) ? matchData.extrasInn1 : matchData.getExtrasString()),
                20, y, paint);
        y += 30;

        paint.setColor(Color.parseColor("#81C784"));
        canvas.drawRect(10, y, PAGE_WIDTH - 10, y + 25, paint);
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Bowler", 20, y + 17, paint);
        canvas.drawText("O", 300, y + 17, paint);
        canvas.drawText("M", 360, y + 17, paint);
        canvas.drawText("R", 420, y + 17, paint);
        canvas.drawText("W", 480, y + 17, paint);
        canvas.drawText("ER", 540, y + 17, paint);
        y += 40;

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        if (bowlers != null) {
            for (String[] bowl : bowlers) {
                if (bowl == null || bowl.length < 6) continue;
                canvas.drawText(bowl[0] != null ? bowl[0] : "", 20, y, paint);
                canvas.drawText(bowl[1] != null ? bowl[1] : "-", 300, y, paint);
                canvas.drawText(bowl[2] != null ? bowl[2] : "-", 360, y, paint);
                canvas.drawText(bowl[3] != null ? bowl[3] : "-", 420, y, paint);
                canvas.drawText(bowl[4] != null ? bowl[4] : "-", 480, y, paint);
                canvas.drawText(bowl[5] != null ? bowl[5] : "-", 540, y, paint);
                y += 25;
            }
        }

        document.finishPage(page);
    }
}
