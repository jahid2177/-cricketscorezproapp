package com.cricketscorez.proapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Custom Canvas View for rendering professional cricket partnership charts.
 * Displays stacked horizontal bars for each wicket stand, contrasting batter contributions.
 */
public class PartnershipGraphView extends View {

    private final Paint paintBar1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBar2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSubText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintCardBg = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<MatchData.PartnershipRecord> records = new ArrayList<>();
    private String teamName = "";

    public PartnershipGraphView(Context context) {
        super(context);
        init();
    }

    public PartnershipGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PartnershipGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintBar1.setColor(Color.parseColor("#1B5E20")); // Green
        paintBar2.setColor(Color.parseColor("#1565C0")); // Blue

        paintCardBg.setColor(Color.parseColor("#FFFFFF"));
        paintCardBg.setShadowLayer(4, 0, 2, Color.parseColor("#1A000000"));

        paintText.setColor(Color.parseColor("#1E293B"));
        paintText.setTextSize(spToPx(13));
        paintText.setFakeBoldText(true);

        paintSubText.setColor(Color.parseColor("#64748B"));
        paintSubText.setTextSize(spToPx(11));

        paintBg.setColor(Color.parseColor("#F1F5F9"));
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    public void setPartnershipData(List<MatchData.PartnershipRecord> data, String team) {
        records.clear();
        if (data != null) {
            records.addAll(data);
        }
        this.teamName = (team != null) ? team : "";
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int itemCount = Math.max(1, records.size());
        int itemHeight = (int) dpToPx(110);
        int totalHeight = (itemCount * itemHeight) + (int) dpToPx(60);

        int minHeight = (int) dpToPx(200);
        int desiredHeight = Math.max(minHeight, totalHeight);

        setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        if (width <= 0) return;

        if (records.isEmpty()) {
            paintText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No partnership data recorded yet", width / 2f, dpToPx(100), paintText);
            return;
        }

        // Find max partnership runs for proportional scaling
        int maxRuns = 10;
        for (MatchData.PartnershipRecord r : records) {
            if (r.totalRuns > maxRuns) maxRuns = r.totalRuns;
        }

        float padding = dpToPx(14);
        float currentY = padding;
        float cardWidth = width - (padding * 2);
        float barHeight = dpToPx(18);
        float cornerRadius = dpToPx(10);

        for (int i = 0; i < records.size(); i++) {
            MatchData.PartnershipRecord r = records.get(i);
            float cardHeight = dpToPx(98);
            RectF cardRect = new RectF(padding, currentY, padding + cardWidth, currentY + cardHeight);

            // Card background
            canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, paintCardBg);

            // Header: Wicket info + Total Runs
            String wicketTitle = r.isUnbroken
                    ? "Stand #" + (i + 1) + " (Unbroken)"
                    : getOrdinal(r.wicketNumber) + " Wicket";
            paintText.setTextAlign(Paint.Align.LEFT);
            paintText.setColor(Color.parseColor("#0F172A"));
            paintText.setTextSize(spToPx(13));
            canvas.drawText(wicketTitle, padding + dpToPx(12), currentY + dpToPx(22), paintText);

            String totalStr = r.totalRuns + " runs (" + r.totalBalls + " balls)";
            paintText.setTextAlign(Paint.Align.RIGHT);
            paintText.setColor(Color.parseColor("#1B5E20"));
            canvas.drawText(totalStr, padding + cardWidth - dpToPx(12), currentY + dpToPx(22), paintText);

            // Stacked Bar
            float barY = currentY + dpToPx(32);
            float barStartX = padding + dpToPx(12);
            float maxBarWidth = cardWidth - dpToPx(24);

            // Background bar track
            RectF trackRect = new RectF(barStartX, barY, barStartX + maxBarWidth, barY + barHeight);
            canvas.drawRoundRect(trackRect, barHeight / 2f, barHeight / 2f, paintBg);

            // Calculate ratio
            int tot = Math.max(1, r.totalRuns);
            float barTotalWidth = Math.max(dpToPx(24), (r.totalRuns / (float) maxRuns) * maxBarWidth);
            float w1 = (r.batsman1Runs / (float) tot) * barTotalWidth;
            float w2 = barTotalWidth - w1;

            // Batter 1 portion
            if (w1 > 0) {
                RectF r1 = new RectF(barStartX, barY, barStartX + w1, barY + barHeight);
                canvas.drawRoundRect(r1, barHeight / 2f, barHeight / 2f, paintBar1);
            }
            // Batter 2 portion
            if (w2 > 0) {
                RectF r2 = new RectF(barStartX + w1, barY, barStartX + barTotalWidth, barY + barHeight);
                canvas.drawRoundRect(r2, barHeight / 2f, barHeight / 2f, paintBar2);
            }

            // Sub labels: Batter 1 stats (left) and Batter 2 stats (right)
            float textY = currentY + dpToPx(70);
            paintSubText.setTextAlign(Paint.Align.LEFT);
            paintSubText.setColor(Color.parseColor("#1B5E20"));
            String b1Text = "🟢 " + r.batsman1Name + ": " + r.batsman1Runs + " (" + r.batsman1Balls + "b)";
            canvas.drawText(b1Text, padding + dpToPx(12), textY, paintSubText);

            paintSubText.setTextAlign(Paint.Align.RIGHT);
            paintSubText.setColor(Color.parseColor("#1565C0"));
            String b2Text = "🔵 " + r.batsman2Name + ": " + r.batsman2Runs + " (" + r.batsman2Balls + "b)";
            canvas.drawText(b2Text, padding + cardWidth - dpToPx(12), textY, paintSubText);

            // Run Rate
            float rrY = currentY + dpToPx(88);
            double rr = r.totalBalls > 0 ? (r.totalRuns * 6.0) / r.totalBalls : 0.0;
            paintSubText.setTextAlign(Paint.Align.LEFT);
            paintSubText.setColor(Color.parseColor("#64748B"));
            canvas.drawText(String.format(Locale.getDefault(), "Run Rate: %.2f rpo", rr), padding + dpToPx(12), rrY, paintSubText);

            currentY += cardHeight + dpToPx(12);
        }
    }

    private String getOrdinal(int n) {
        if (n <= 0) return "1st";
        int mod100 = n % 100;
        int mod10 = n % 10;
        if (mod100 >= 11 && mod100 <= 13) return n + "th";
        switch (mod10) {
            case 1: return n + "st";
            case 2: return n + "nd";
            case 3: return n + "rd";
            default: return n + "th";
        }
    }
}
