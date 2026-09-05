package com.cricketscorez.proapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;

public class RunRateGraphView extends View {

    private Paint pGrid, pLine1, pLine2, pText, pWkt1, pWkt2, pLegend, pHollow;
    private ArrayList<Integer> runs1 = new ArrayList<>(), runs2 = new ArrayList<>();
    private ArrayList<Integer> wkts1 = new ArrayList<>(), wkts2 = new ArrayList<>();
    private String name1 = "Giants", name2 = "KnightRiders";
    private int maxOvers = 20; 
    private final float MAX_RR = 40f; // 🔥 স্কেল ১৬ থেকে ৪০ করা হয়েছে

    public RunRateGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        pGrid = new Paint(); pGrid.setColor(Color.parseColor("#E0E0E0")); pGrid.setStrokeWidth(2);
        pText = new Paint(); pText.setColor(Color.parseColor("#757575")); pText.setTextSize(30); pText.setAntiAlias(true);
        pLegend = new Paint(); pLegend.setTextSize(35); pLegend.setAntiAlias(true);

        pHollow = new Paint(); pHollow.setColor(Color.WHITE); pHollow.setStyle(Paint.Style.FILL); pHollow.setAntiAlias(true);

        // টিম ১ (সায়ান/নীল)
        pLine1 = new Paint(); pLine1.setColor(Color.parseColor("#03A9F4"));
        pLine1.setStrokeWidth(5); pLine1.setStyle(Paint.Style.STROKE); pLine1.setAntiAlias(true);
        pWkt1 = new Paint(); pWkt1.setColor(Color.parseColor("#03A9F4")); pWkt1.setStyle(Paint.Style.STROKE); pWkt1.setStrokeWidth(4);

        // টিম ২ (লাল)
        pLine2 = new Paint(); pLine2.setColor(Color.parseColor("#D32F2F"));
        pLine2.setStrokeWidth(5); pLine2.setStyle(Paint.Style.STROKE); pLine2.setAntiAlias(true);
        pWkt2 = new Paint(); pWkt2.setColor(Color.parseColor("#D32F2F")); pWkt2.setStyle(Paint.Style.STROKE); pWkt2.setStrokeWidth(4);
    }

    public void setData(ArrayList<Integer> r1, ArrayList<Integer> w1, ArrayList<Integer> r2, ArrayList<Integer> w2, String n1, String n2, int overs) {
        this.runs1 = r1 != null ? r1 : new ArrayList<>();
        this.wkts1 = w1 != null ? w1 : new ArrayList<>();
        this.runs2 = r2 != null ? r2 : new ArrayList<>();
        this.wkts2 = w2 != null ? w2 : new ArrayList<>();
        this.name1 = n1 != null ? n1 : "Team 1"; 
        this.name2 = n2 != null ? n2 : "Team 2";

        // 🔥 ওভার সংখ্যা ডাইনামিক করা হয়েছে (সর্বোচ্চ ৫০)
        this.maxOvers = (overs > 0) ? Math.min(overs, 50) : 20;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float leftPad = 120, rightPad = 50, topPad = 150, bottomPad = 120;
        float gW = w - leftPad - rightPad;
        float gH = h - topPad - bottomPad;

        // ১. লিজেন্ড ড্রয়িং
        pLegend.setColor(Color.parseColor("#03A9F4"));
        canvas.drawCircle(w * 0.45f, 80, 15, pLegend);
        pLegend.setColor(Color.parseColor("#757575"));
        canvas.drawText(name1, w * 0.45f + 30, 92, pLegend);

        pLegend.setColor(Color.parseColor("#D32F2F"));
        canvas.drawCircle(w * 0.70f, 80, 15, pLegend);
        pLegend.setColor(Color.parseColor("#757575"));
        canvas.drawText(name2, w * 0.70f + 30, 92, pLegend);

        // ২. Y-Axis গ্রিড এবং লেবেল (০ থেকে ৪০ পর্যন্ত ১০ করে ব্যবধান)
        canvas.drawText("RUN RATE", 30, h / 2, pText);
        for (int i = 0; i <= 4; i++) {
            int val = i * 10; // ০, ১০, ২০, ৩০, ৪০
            float y = (h - bottomPad) - (val * (gH / MAX_RR));
            canvas.drawText(String.valueOf(val), 60, y + 10, pText);
            canvas.drawLine(leftPad, y, w - rightPad, y, pGrid);
        }

        // ৩. X-Axis লেবেল (০ থেকে ৫০ পর্যন্ত)
        // 🔥 ২০ ওভারের বেশি হলে ৫ ওভার অন্তর অন্তর লেবেল দেখাবে যাতে হিজিবিজি না হয়
        int step = (maxOvers > 20) ? 5 : 2;
        for (int i = 0; i <= maxOvers; i += step) {
            float x = leftPad + (i * (gW / maxOvers));
            canvas.drawText(String.valueOf(i), x - 10, h - 70, pText);
        }
        canvas.drawText("OVERS", w / 2, h - 20, pText);

        // ৪. টিম লাইন এবং হলু উইকেট মার্কার ড্রয়িং
        drawRRLine(canvas, runs1, wkts1, pLine1, pWkt1, leftPad, bottomPad, gW, gH);
        drawRRLine(canvas, runs2, wkts2, pLine2, pWkt2, leftPad, bottomPad, gW, gH);
    }

    private void drawRRLine(Canvas canvas, ArrayList<Integer> runs, ArrayList<Integer> wkts, Paint lineP, Paint wktP, float lP, float bP, float gW, float gH) {
        if (runs.size() < 2) return;
        Path path = new Path();
        float h = getHeight();

        for (int i = 1; i < runs.size(); i++) {
            float rr = (float) runs.get(i) / i;
            float x = lP + (i * (gW / maxOvers));

            // রান রেট ৪০ এর বেশি হলে গ্রাফের ভেতরে রাখার জন্য লিমিট করা হয়েছে
            float rrLimited = Math.min(rr, MAX_RR);
            float y = (h - bP) - (rrLimited * (gH / MAX_RR));

            if (i == 1) path.moveTo(x, y); else path.lineTo(x, y);

            // উইকেট মার্কার (Hollow Circles)
            if (wkts != null && i < wkts.size() && wkts.get(i) > wkts.get(i - 1)) {
                canvas.drawCircle(x, y, 10, pHollow); 
                canvas.drawCircle(x, y, 10, wktP);   
            }
        }
        canvas.drawPath(path, lineP);
    }
}

