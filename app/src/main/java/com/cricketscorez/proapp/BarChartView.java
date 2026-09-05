package com.cricketscorez.proapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;

public class BarChartView extends View {

    private Paint pGrid, pBar1, pBar2, pText, pWkt1, pWkt2, pLegend;
    private ArrayList<Integer> inn1Runs = new ArrayList<>(), inn2Runs = new ArrayList<>();
    private ArrayList<Integer> inn1Wkts = new ArrayList<>(), inn2Wkts = new ArrayList<>();
    private String name1 = "Team 1", name2 = "Team 2";
    private int maxOvers = 20;
    private int maxRunsPerOver = 20; // Y-Axis স্কেল (ডিফল্ট ২০)

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        pGrid = new Paint(); pGrid.setColor(Color.parseColor("#EEEEEE")); pGrid.setStrokeWidth(2);
        pText = new Paint(); pText.setColor(Color.parseColor("#666666")); pText.setTextSize(25); pText.setAntiAlias(true);
        pLegend = new Paint(); pLegend.setTextSize(30); pLegend.setAntiAlias(true); pLegend.setFakeBoldText(true);

        // টিম ১ স্টাইল (গোলাপী/লাল - ছবির মতো)
        pBar1 = new Paint(); pBar1.setColor(Color.parseColor("#E91E63")); pBar1.setStyle(Paint.Style.FILL);
        pWkt1 = new Paint(); pWkt1.setColor(Color.parseColor("#E91E63")); pWkt1.setStyle(Paint.Style.STROKE); pWkt1.setStrokeWidth(3);

        // টিম ২ স্টাইল (নীল - ছবির মতো)
        pBar2 = new Paint(); pBar2.setColor(Color.parseColor("#03A9F4")); pBar2.setStyle(Paint.Style.FILL);
        pWkt2 = new Paint(); pWkt2.setColor(Color.parseColor("#03A9F4")); pWkt2.setStyle(Paint.Style.STROKE); pWkt2.setStrokeWidth(3);
    }

    public void setData(ArrayList<Integer> r1, ArrayList<Integer> w1, ArrayList<Integer> r2, ArrayList<Integer> w2, String n1, String n2, int overs) {
        this.inn1Runs = r1 != null ? r1 : new ArrayList<>();
        this.inn1Wkts = w1 != null ? w1 : new ArrayList<>();
        this.inn2Runs = r2 != null ? r2 : new ArrayList<>();
        this.inn2Wkts = w2 != null ? w2 : new ArrayList<>();
        this.name1 = n1; this.name2 = n2;
        this.maxOvers = overs > 0 ? overs : 20;

        // অটো-স্কেলিং লজিক
        int highest = 15;
        highest = Math.max(highest, getHighestPerOver(inn1Runs));
        highest = Math.max(highest, getHighestPerOver(inn2Runs));
        this.maxRunsPerOver = highest + 5;
        invalidate();
    }

    private int getHighestPerOver(ArrayList<Integer> runs) {
        int max = 0;
        for (int i = 1; i < runs.size(); i++) {
            int overRun = runs.get(i) - runs.get(i - 1);
            if (overRun > max) max = overRun;
        }
        return max;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float leftPad = 80, rightPad = 30, topPad = 120, bottomPad = 80;
        float gW = w - leftPad - rightPad;
        float gH = h - topPad - bottomPad;

        // ১. লিজেন্ড (Legend at Top)
        drawLegend(canvas, leftPad);

        // ২. গ্রিড এবং Y-Axis লেবেল (0, 5, 10, 15, 20)
        for (int i = 0; i <= 4; i++) {
            int val = (maxRunsPerOver / 4) * i;
            float y = (h - bottomPad) - (val * (gH / maxRunsPerOver));
            canvas.drawText(String.valueOf(val), 20, y + 10, pText);
            canvas.drawLine(leftPad, y, w - rightPad, y, pGrid);
        }

        // ৩. X-Axis লেবেল (ওভার সংখ্যা)
        float stepX = gW / maxOvers;
        for (int i = 1; i <= maxOvers; i += (maxOvers > 10 ? 2 : 1)) {
            float x = leftPad + (i * stepX) - (stepX / 2);
            canvas.drawText(String.valueOf(i), x - 10, h - 30, pText);
        }
        canvas.drawText("OVERS", w / 2 - 40, h - 5, pText);

        // ৪. বার ড্রয়িং লজিক
        drawBars(canvas, inn1Runs, inn1Wkts, pBar1, pWkt1, 0, leftPad, bottomPad, stepX, gH);
        drawBars(canvas, inn2Runs, inn2Wkts, pBar2, pWkt2, 1, leftPad, bottomPad, stepX, gH);
    }

    private void drawBars(Canvas canvas, ArrayList<Integer> runs, ArrayList<Integer> wkts, Paint bP, Paint wP, int teamIdx, float lP, float bP_pad, float stepX, float gH) {
        if (runs.size() < 2) return;
        float h = getHeight();
        float barWidth = stepX * 0.4f;

        for (int i = 1; i < runs.size(); i++) {
            int overRuns = runs.get(i) - runs.get(i - 1);
            float xBase = lP + (i * stepX) - stepX;
            float xPos = (teamIdx == 0) ? xBase + (stepX * 0.1f) : xBase + (stepX * 0.5f);

            float barHeight = (overRuns * (gH / maxRunsPerOver));
            float top = (h - bP_pad) - barHeight;

            // বার আঁকা
            canvas.drawRect(xPos, top, xPos + barWidth, h - bP_pad, bP);

            // উইকেট মার্কার (ছবির মতো বারের মাথার ওপর গোল বৃত্ত)
            if (wkts != null && i < wkts.size() && wkts.get(i) > wkts.get(i - 1)) {
                canvas.drawCircle(xPos + (barWidth / 2), top - 15, 10, wP);
            }
        }
    }

    private void drawLegend(Canvas canvas, float leftPad) {
        pLegend.setColor(Color.parseColor("#E91E63"));
        canvas.drawCircle(leftPad + 50, 60, 12, pLegend);
        canvas.drawText(name1, leftPad + 80, 72, pLegend);

        pLegend.setColor(Color.parseColor("#03A9F4"));
        canvas.drawCircle(leftPad + 350, 60, 12, pLegend);
        canvas.drawText(name2, leftPad + 380, 72, pLegend);
    }
}

