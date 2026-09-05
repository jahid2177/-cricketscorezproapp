package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;

public class MatchShareManager {

    /**
     * Shows a modal dialog offering choice between Text Snippet and Image Card.
     */
    public static void showShareDialog(final Activity activity, final MatchData matchData) {
        if (activity == null || activity.isFinishing()) return;

        if (matchData == null) {
            Toast.makeText(activity, "No completed match data available to share.", Toast.LENGTH_SHORT).show();
            return;
        }

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        boolean isDark = ThemeManager.isDarkVariant(activity);
        int dialogBg = isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FFFFFF");
        int textColor = isDark ? Color.parseColor("#F8FAFC") : Color.parseColor("#0F172A");
        int subTextColor = isDark ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");
        int borderColor = isDark ? Color.parseColor("#1E293B") : Color.parseColor("#E2E8F0");

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(activity, 24), dp(activity, 24), dp(activity, 24), dp(activity, 24));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(dialogBg);
        bg.setCornerRadius(dp(activity, 24));
        bg.setStroke(dp(activity, 1), borderColor);
        root.setBackground(bg);

        // Icon Header
        TextView tvIcon = new TextView(activity);
        tvIcon.setText("📤");
        tvIcon.setTextSize(32);
        tvIcon.setGravity(Gravity.CENTER);
        root.addView(tvIcon);

        // Title
        TextView tvTitle = new TextView(activity);
        tvTitle.setText("Share Match Result");
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(textColor);
        tvTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.setMargins(0, dp(activity, 8), 0, dp(activity, 4));
        tvTitle.setLayoutParams(tLp);
        root.addView(tvTitle);

        // Subtitle
        TextView tvSub = new TextView(activity);
        String t1 = matchData.team1Name != null ? matchData.team1Name : "Team 1";
        String t2 = matchData.team2Name != null ? matchData.team2Name : "Team 2";
        tvSub.setText(t1 + " vs " + t2);
        tvSub.setTextSize(14);
        tvSub.setTextColor(subTextColor);
        tvSub.setGravity(Gravity.CENTER);
        root.addView(tvSub);

        // Option 1: Share as Image Card
        Button btnShareImage = new Button(activity);
        btnShareImage.setText("🖼️  Share as Match Card (Image)");
        btnShareImage.setTextColor(Color.WHITE);
        btnShareImage.setTextSize(15);
        btnShareImage.setTypeface(null, Typeface.BOLD);
        btnShareImage.setAllCaps(false);
        GradientDrawable imgBtnBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#2563EB"), Color.parseColor("#1D4ED8")}
        );
        imgBtnBg.setCornerRadius(dp(activity, 14));
        btnShareImage.setBackground(imgBtnBg);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 52));
        imgLp.setMargins(0, dp(activity, 20), 0, dp(activity, 12));
        btnShareImage.setLayoutParams(imgLp);
        btnShareImage.setOnClickListener(v -> {
            dialog.dismiss();
            shareMatchResultAsImage(activity, matchData);
        });
        root.addView(btnShareImage);

        // Option 2: Share as Text Snippet
        Button btnShareText = new Button(activity);
        btnShareText.setText("📝  Share as Text Snippet");
        btnShareText.setTextColor(isDark ? Color.parseColor("#E2E8F0") : Color.parseColor("#1E293B"));
        btnShareText.setTextSize(15);
        btnShareText.setTypeface(null, Typeface.BOLD);
        btnShareText.setAllCaps(false);
        GradientDrawable txtBtnBg = new GradientDrawable();
        txtBtnBg.setColor(isDark ? Color.parseColor("#1E293B") : Color.parseColor("#F1F5F9"));
        txtBtnBg.setCornerRadius(dp(activity, 14));
        btnShareText.setBackground(txtBtnBg);
        LinearLayout.LayoutParams txtLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 52));
        txtLp.setMargins(0, 0, 0, dp(activity, 8));
        btnShareText.setLayoutParams(txtLp);
        btnShareText.setOnClickListener(v -> {
            dialog.dismiss();
            shareMatchResultAsText(activity, matchData);
        });
        root.addView(btnShareText);

        dialog.setContentView(root);
        int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.88f);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    /**
     * Shares match result as a clean, rich text snippet via Intent.ACTION_SEND.
     */
    public static void shareMatchResultAsText(Activity activity, MatchData matchData) {
        if (activity == null || matchData == null) return;

        String textSnippet = generateMatchResultText(matchData);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Cricket Match Result: " + matchData.team1Name + " vs " + matchData.team2Name);
        intent.putExtra(Intent.EXTRA_TEXT, textSnippet);

        activity.startActivity(Intent.createChooser(intent, "Share Match Result via"));
    }

    /**
     * Builds the formatted text snippet.
     */
    public static String generateMatchResultText(MatchData matchData) {
        StringBuilder sb = new StringBuilder();
        String t1 = matchData.teamBattingFirst != null ? matchData.teamBattingFirst : matchData.team1Name;
        String t2 = matchData.teamBattingSecond != null ? matchData.teamBattingSecond : matchData.team2Name;

        sb.append("🏏 *CRICKET MATCH RESULT* 🏏\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🏆 *").append(t1).append(" vs ").append(t2).append("*\n");
        if (matchData.matchDate != null && !matchData.matchDate.isEmpty()) {
            sb.append("📅 ").append(matchData.matchDate).append("\n");
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n");

        // Innings 1
        String s1 = (matchData.scoreInn1 != null && !matchData.scoreInn1.isEmpty())
                ? matchData.scoreInn1 : (matchData.isSecondInnings ? "0/0" : matchData.getScoreString());
        String ov1 = (matchData.oversInn1 != null && !matchData.oversInn1.isEmpty())
                ? matchData.oversInn1 : (matchData.isSecondInnings ? "0" : matchData.getOversString());
        sb.append("🔹 *").append(t1).append("*: ").append(s1).append(" (").append(ov1).append("/").append(matchData.totalOvers).append(" ov)\n");

        // Innings 2
        if (matchData.isSecondInnings) {
            String s2 = matchData.getScoreString();
            String ov2 = matchData.getOversString();
            sb.append("🔹 *").append(t2).append("*: ").append(s2).append(" (").append(ov2).append("/").append(matchData.totalOvers).append(" ov)\n");
        }
        sb.append("\n");

        // Result / Status
        String status = (matchData.matchStatus != null && !matchData.matchStatus.isEmpty())
                ? matchData.matchStatus : "Match in Progress";
        sb.append("🎉 *Result:* ").append(status).append("\n\n");

        // Top Performers
        TopPerformers top = findTopPerformers(matchData);
        if (top != null) {
            sb.append("⭐ *Key Highlights:*\n");
            if (top.topBatsman != null && !top.topBatsman.isEmpty()) {
                sb.append("🏏 Best Batter: ").append(top.topBatsman).append(" - ").append(top.topBatsmanScore).append("\n");
            }
            if (top.bestBowler != null && !top.bestBowler.isEmpty()) {
                sb.append("🎯 Best Bowler: ").append(top.bestBowler).append(" - ").append(top.bestBowlerFigure).append("\n");
            }
            sb.append("\n");
        }

        sb.append("⚡ _Scored with Cricket Scorez Pro_");
        return sb.toString();
    }

    /**
     * Renders a high-resolution visual Match Result Graphic Card and shares via Intent.
     */
    public static void shareMatchResultAsImage(final Activity activity, final MatchData matchData) {
        if (activity == null || matchData == null) return;

        Toast.makeText(activity, "Generating match card image...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Bitmap bitmap = renderMatchCardBitmap(activity, matchData);
                if (bitmap == null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(activity, "Failed to render match card.", Toast.LENGTH_SHORT).show());
                    return;
                }

                File cachePath = new File(activity.getCacheDir(), "shared_cards");
                if (!cachePath.exists()) cachePath.mkdirs();

                File imageFile = new File(cachePath, "match_result_" + System.currentTimeMillis() + ".png");
                FileOutputStream stream = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                stream.flush();
                stream.close();

                Uri contentUri = FileProvider.getUriForFile(
                        activity,
                        activity.getPackageName() + ".provider",
                        imageFile
                );

                new Handler(Looper.getMainLooper()).post(() -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/png");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, generateMatchResultText(matchData));
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    activity.startActivity(Intent.createChooser(shareIntent, "Share Match Card via"));
                });

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(activity, "Error sharing image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Generates a 1080x1350 high quality Match Summary Graphic Canvas.
     */
    private static Bitmap renderMatchCardBitmap(Context context, MatchData matchData) {
        int width = 1080;
        int height = 1350;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 1. Background Gradient
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LinearGradient gradient = new LinearGradient(
                0, 0, 0, height,
                new int[]{Color.parseColor("#0B1329"), Color.parseColor("#152238"), Color.parseColor("#0A0E1A")},
                null, Shader.TileMode.CLAMP
        );
        bgPaint.setShader(gradient);
        canvas.drawRect(0, 0, width, height, bgPaint);

        // Top Accent glow arc
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setColor(Color.parseColor("#1E3A8A"));
        glowPaint.setAlpha(80);
        canvas.drawCircle(width / 2f, -100, 500, glowPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // 2. Header: App Logo & Badge
        textPaint.setTextSize(34);
        textPaint.setColor(Color.parseColor("#60A5FA"));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("⚡ CRICKET SCOREZ PRO", width / 2f, 90, textPaint);

        textPaint.setTextSize(24);
        textPaint.setColor(Color.parseColor("#94A3B8"));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        String matchDate = (matchData.matchDate != null && !matchData.matchDate.isEmpty())
                ? matchData.matchDate : "Official Match Summary";
        canvas.drawText(matchDate + " • " + matchData.totalOvers + " Overs Match", width / 2f, 135, textPaint);

        // 3. Match Card Container
        RectF cardRect = new RectF(50, 180, width - 50, 680);
        Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setColor(Color.parseColor("#1E293B"));
        cardPaint.setAlpha(220);
        canvas.drawRoundRect(cardRect, 36, 36, cardPaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);
        borderPaint.setColor(Color.parseColor("#334155"));
        canvas.drawRoundRect(cardRect, 36, 36, borderPaint);

        // Team 1 Section
        String t1 = matchData.teamBattingFirst != null ? matchData.teamBattingFirst : matchData.team1Name;
        String s1 = (matchData.scoreInn1 != null && !matchData.scoreInn1.isEmpty())
                ? matchData.scoreInn1 : (matchData.isSecondInnings ? "0/0" : matchData.getScoreString());
        String ov1 = (matchData.oversInn1 != null && !matchData.oversInn1.isEmpty())
                ? matchData.oversInn1 : (matchData.isSecondInnings ? "0" : matchData.getOversString());

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(44);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(Color.WHITE);
        canvas.drawText(t1, 90, 270, textPaint);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(62);
        textPaint.setColor(Color.parseColor("#38BDF8"));
        canvas.drawText(s1, width - 90, 275, textPaint);

        textPaint.setTextSize(26);
        textPaint.setColor(Color.parseColor("#94A3B8"));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(ov1 + "/" + matchData.totalOvers + " ov", width - 90, 315, textPaint);

        // Divider
        Paint divPaint = new Paint();
        divPaint.setColor(Color.parseColor("#334155"));
        divPaint.setStrokeWidth(2);
        canvas.drawLine(90, 370, width - 90, 370, divPaint);

        // Team 2 Section
        String t2 = matchData.teamBattingSecond != null ? matchData.teamBattingSecond : matchData.team2Name;
        String s2 = matchData.isSecondInnings ? matchData.getScoreString() : "--";
        String ov2 = matchData.isSecondInnings ? matchData.getOversString() : "--";

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(44);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(Color.WHITE);
        canvas.drawText(t2, 90, 480, textPaint);

        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(62);
        textPaint.setColor(Color.parseColor("#38BDF8"));
        canvas.drawText(s2, width - 90, 485, textPaint);

        textPaint.setTextSize(26);
        textPaint.setColor(Color.parseColor("#94A3B8"));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText(ov2 + "/" + matchData.totalOvers + " ov", width - 90, 525, textPaint);

        // 4. Winner Ribbon
        RectF resultRect = new RectF(80, 570, width - 80, 650);
        Paint resBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        resBgPaint.setColor(Color.parseColor("#10B981"));
        canvas.drawRoundRect(resultRect, 20, 20, resBgPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(32);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String status = (matchData.matchStatus != null && !matchData.matchStatus.isEmpty())
                ? matchData.matchStatus : "Match in Progress";
        canvas.drawText("🏆 " + status, width / 2f, 622, textPaint);

        // 5. Highlights Section Cards
        TopPerformers top = findTopPerformers(matchData);

        // Batter Highlight Card
        RectF batCard = new RectF(50, 720, width / 2f - 15, 960);
        Paint hlCardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hlCardPaint.setColor(Color.parseColor("#1E293B"));
        canvas.drawRoundRect(batCard, 28, 28, hlCardPaint);
        canvas.drawRoundRect(batCard, 28, 28, borderPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(26);
        textPaint.setColor(Color.parseColor("#F59E0B"));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("🏏 TOP BATTER", 80, 775, textPaint);

        textPaint.setTextSize(34);
        textPaint.setColor(Color.WHITE);
        String batName = (top != null && top.topBatsman != null) ? top.topBatsman : "Batsman";
        if (batName.length() > 16) batName = batName.substring(0, 14) + "..";
        canvas.drawText(batName, 80, 835, textPaint);

        textPaint.setTextSize(28);
        textPaint.setColor(Color.parseColor("#38BDF8"));
        String batScore = (top != null && top.topBatsmanScore != null) ? top.topBatsmanScore : "0 (0)";
        canvas.drawText(batScore, 80, 895, textPaint);

        // Bowler Highlight Card
        RectF bowlCard = new RectF(width / 2f + 15, 720, width - 50, 960);
        canvas.drawRoundRect(bowlCard, 28, 28, hlCardPaint);
        canvas.drawRoundRect(bowlCard, 28, 28, borderPaint);

        textPaint.setTextSize(26);
        textPaint.setColor(Color.parseColor("#10B981"));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("🎯 TOP BOWLER", width / 2f + 45, 775, textPaint);

        textPaint.setTextSize(34);
        textPaint.setColor(Color.WHITE);
        String bowlName = (top != null && top.bestBowler != null) ? top.bestBowler : "Bowler";
        if (bowlName.length() > 16) bowlName = bowlName.substring(0, 14) + "..";
        canvas.drawText(bowlName, width / 2f + 45, 835, textPaint);

        textPaint.setTextSize(28);
        textPaint.setColor(Color.parseColor("#38BDF8"));
        String bowlFig = (top != null && top.bestBowlerFigure != null) ? top.bestBowlerFigure : "0/0 (0 ov)";
        canvas.drawText(bowlFig, width / 2f + 45, 895, textPaint);

        // 6. Match Info Footer Banner
        RectF footerCard = new RectF(50, 1000, width - 50, 1220);
        canvas.drawRoundRect(footerCard, 28, 28, hlCardPaint);
        canvas.drawRoundRect(footerCard, 28, 28, borderPaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(28);
        textPaint.setColor(Color.parseColor("#E2E8F0"));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Match Played on Cricket Scorez Pro", width / 2f, 1070, textPaint);

        textPaint.setTextSize(22);
        textPaint.setColor(Color.parseColor("#94A3B8"));
        canvas.drawText("Live Ball-by-Ball Scoring • Tournament Tracking • PDF Scorecards", width / 2f, 1120, textPaint);

        // Watermark Footer
        textPaint.setTextSize(24);
        textPaint.setColor(Color.parseColor("#64748B"));
        canvas.drawText("Generated by Cricket Scorez Pro App", width / 2f, 1290, textPaint);

        return bitmap;
    }

    private static class TopPerformers {
        String topBatsman;
        String topBatsmanScore;
        String bestBowler;
        String bestBowlerFigure;
    }

    private static TopPerformers findTopPerformers(MatchData matchData) {
        if (matchData == null) return null;
        TopPerformers top = new TopPerformers();

        int maxRuns = -1;
        String topBat = null;
        String topBatScore = null;

        // Check Innings 1 & 2 Batsmen
        ArrayList<String[]> allBatsmen = new ArrayList<>();
        if (matchData.batsmanHistory != null) allBatsmen.addAll(matchData.batsmanHistory);
        if (matchData.batsmanHistoryInn1 != null) allBatsmen.addAll(matchData.batsmanHistoryInn1);

        for (String[] bat : allBatsmen) {
            try {
                if (bat.length >= 5) {
                    String name = bat[0].replace("*", "").trim();
                    int r = Integer.parseInt(bat[1]);
                    int b = Integer.parseInt(bat[2]);
                    int f = Integer.parseInt(bat[3]);
                    int s = Integer.parseInt(bat[4]);
                    if (r > maxRuns) {
                        maxRuns = r;
                        topBat = name;
                        topBatScore = r + " (" + b + "b, " + f + "x4, " + s + "x6)";
                    }
                }
            } catch (Exception ignored) {}
        }

        // Check current active strikers
        if (matchData.strikerRuns > maxRuns && matchData.strikerName != null && !matchData.strikerName.isEmpty()) {
            maxRuns = matchData.strikerRuns;
            topBat = matchData.strikerName;
            topBatScore = matchData.strikerRuns + " (" + matchData.strikerBalls + "b, " + matchData.striker4s + "x4, " + matchData.striker6s + "x6)";
        }
        if (matchData.nonStrikerRuns > maxRuns && matchData.nonStrikerName != null && !matchData.nonStrikerName.isEmpty()) {
            topBat = matchData.nonStrikerName;
            topBatScore = matchData.nonStrikerRuns + " (" + matchData.nonStrikerBalls + "b, " + matchData.nonStriker4s + "x4, " + matchData.nonStriker6s + "x6)";
        }

        top.topBatsman = topBat;
        top.topBatsmanScore = topBatScore;

        // Bowlers
        int maxWkts = -1;
        int minRuns = 999;
        String bestBowl = null;
        String bestBowlFig = null;

        ArrayList<String[]> allBowlers = new ArrayList<>();
        if (matchData.bowlerHistory != null) allBowlers.addAll(matchData.getAllBowlingStats());
        if (matchData.bowlerHistoryInn1 != null) allBowlers.addAll(matchData.bowlerHistoryInn1);

        for (String[] bowl : allBowlers) {
            try {
                if (bowl.length >= 5) {
                    String name = bowl[0].replace("*", "").trim();
                    String overs = bowl[1];
                    int r = Integer.parseInt(bowl[3]);
                    int w = Integer.parseInt(bowl[4]);
                    if (w > maxWkts || (w == maxWkts && r < minRuns)) {
                        maxWkts = w;
                        minRuns = r;
                        bestBowl = name;
                        bestBowlFig = w + "/" + r + " (" + overs + " ov)";
                    }
                }
            } catch (Exception ignored) {}
        }

        // Also check current active bowler if not already in list
        if (matchData.bowlerWickets > maxWkts || (matchData.bowlerWickets == maxWkts && matchData.bowlerRuns < minRuns && matchData.currentBowlerName != null)) {
            if (matchData.currentBowlerName != null && !matchData.currentBowlerName.isEmpty()) {
                bestBowl = matchData.currentBowlerName;
                bestBowlFig = matchData.bowlerWickets + "/" + matchData.bowlerRuns + " (" + matchData.getBowlerFigures() + " ov)";
            }
        }

        top.bestBowler = bestBowl;
        top.bestBowlerFigure = bestBowlFig;

        return top;
    }

    private static int dp(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
