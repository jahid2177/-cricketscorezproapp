package com.cricketscorez.proapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;

public class WormGraphView extends View {

    private Paint pGrid, pLine1, pLine2, pText, pWkt1, pWkt2, pLegend;
    private ArrayList<Integer> runs1 = new ArrayList<>(), runs2 = new ArrayList<>();
    private ArrayList<Integer> wkts1 = new ArrayList<>(), wkts2 = new ArrayList<>();
    private String name1 = "Team 1", name2 = "Team 2";
    private int maxOvers = 20;
    private final int MAX_RUNS = 150; // ছবির মতো ১৫০ স্কেল

    public WormGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // গ্রিড এবং টেক্সট স্টাইল
        pGrid = new Paint(); pGrid.setColor(Color.parseColor("#333333")); pGrid.setStrokeWidth(2);
        pText = new Paint(); pText.setColor(Color.parseColor("#999999")); pText.setTextSize(30); pText.setAntiAlias(true);
        pLegend = new Paint(); pLegend.setTextSize(35); pLegend.setAntiAlias(true); pLegend.setFakeBoldText(true);

        // টিম ১ (সবুজ - গোল মার্কার)
        pLine1 = new Paint(); pLine1.setColor(Color.parseColor("#00C853"));
        pLine1.setStrokeWidth(5); pLine1.setStyle(Paint.Style.STROKE); pLine1.setAntiAlias(true);
        pWkt1 = new Paint(); pWkt1.setColor(Color.parseColor("#00C853")); pWkt1.setStyle(Paint.Style.FILL);

        // টিম ২ (নীল - চারকোনা মার্কার)
        pLine2 = new Paint(); pLine2.setColor(Color.parseColor("#448AFF"));
        pLine2.setStrokeWidth(5); pLine2.setStyle(Paint.Style.STROKE); pLine2.setAntiAlias(true);
        pWkt2 = new Paint(); pWkt2.setColor(Color.parseColor("#448AFF")); pWkt2.setStyle(Paint.Style.FILL);
    }

    public void setData(ArrayList<Integer> r1, ArrayList<Integer> w1, ArrayList<Integer> r2, ArrayList<Integer> w2, String n1, String n2, int overs) {
        this.runs1 = r1 != null ? r1 : new ArrayList<>();
        this.wkts1 = w1 != null ? w1 : new ArrayList<>();
        this.runs2 = r2 != null ? r2 : new ArrayList<>();
        this.wkts2 = w2 != null ? w2 : new ArrayList<>();
        this.name1 = n1; this.name2 = n2;
        this.maxOvers = overs > 0 ? overs : 20;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float leftPad = 100, rightPad = 50, topPad = 150, bottomPad = 100;
        float gW = w - leftPad - rightPad;
        float gH = h - topPad - bottomPad;

        // ১. লিজেন্ড ড্রয়িং ( Legend at Top )
        pLegend.setColor(Color.parseColor("#00C853"));
        canvas.drawCircle(leftPad, 60, 12, pLegend);
        canvas.drawText(name1, leftPad + 30, 75, pLegend);

        pLegend.setColor(Color.parseColor("#448AFF"));
        canvas.drawRect(leftPad + 250, 48, leftPad + 274, 72, pLegend);
        canvas.drawText(name2, leftPad + 285, 75, pLegend);

        // ২. Y-Axis গ্রিড এবং লেবেল (0, 50, 100, 150)
        for (int i = 0; i <= 3; i++) {
            int runVal = i * 50;
            float y = (h - bottomPad) - (runVal * (gH / MAX_RUNS));
            canvas.drawText(String.valueOf(runVal), 20, y + 10, pText);
            canvas.drawLine(leftPad, y, w - rightPad, y, pGrid);
        }

        // ৩. X-Axis লেবেল (0, 5, 10, 15, 20)
        for (int i = 0; i <= maxOvers; i += 5) {
            float x = leftPad + (i * (gW / maxOvers));
            canvas.drawText(String.valueOf(i), x - 10, h - 50, pText);
        }
        canvas.drawText("overs", leftPad, h - 10, pText);

        // ৪. টিম ১ এর লাইন এবং গোল উইকেট মার্কার
        drawTeam(canvas, runs1, wkts1, pLine1, pWkt1, true, leftPad, bottomPad, gW, gH);

        // ৫. টিম ২ এর লাইন এবং চারকোনা উইকেট মার্কার
        drawTeam(canvas, runs2, wkts2, pLine2, pWkt2, false, leftPad, bottomPad, gW, gH);
    }

    private void drawTeam(Canvas canvas, ArrayList<Integer> runs, ArrayList<Integer> wkts, Paint lineP, Paint wktP, boolean isCircle, float lP, float bP, float gW, float gH) {
        if (runs.isEmpty()) return;
        Path path = new Path();
        float h = getHeight();

        for (int i = 0; i < runs.size(); i++) {
            float x = lP + (i * (gW / maxOvers));
            float y = (h - bP) - (runs.get(i) * (gH / MAX_RUNS));

            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);

            // উইকেট মার্কার লজিক (ছবির মতো নির্দিষ্ট পয়েন্টে)
            if (wkts != null && i > 0 && i < wkts.size() && wkts.get(i) > wkts.get(i-1)) {
                if (isCircle) {
                    canvas.drawCircle(x, y, 10, wktP);
                } else {
                    canvas.drawRect(x-10, y-10, x+10, y+10, wktP);
                }
            }
        }
        canvas.drawPath(path, lineP);
    }
}

