package com.cricketscorez.proapp;

import java.io.Serializable;

public class BallEvent implements Serializable {
    public int runs;
    public boolean isWicket;
    public boolean isExtra;
    public String extraType; // WD, NB, BYE, LB
    public boolean needsStrikeChange;
    public boolean isLegalBall;
    public boolean isStrikerOut; // কে আউট হয়েছে তা জানার জন্য

    // ✅ FIX: এই বলটা যদি ওভারের ৬ষ্ঠ (শেষ) বল হয় এবং সেই ওভারটা maiden হয়,
    // তাহলে true থাকবে — undo করার সময় currentBowlerMaidens সঠিকভাবে
    // কমানোর জন্য এই তথ্য দরকার।
    public boolean completedMaidenOver = false;

    // ✅ FIX: Run Out হলে এটা বোলারের wicket হিসেবে গণ্য হয় না (ক্রিকেট নিয়ম)।
    // Caught/Bowled/Stumped/LBW/Hit Wicket ইত্যাদিতে true থাকবে, Run Out-এ false।
    public boolean creditBowlerForWicket = true;

    // Commentary এর জন্য নাম
    public String bowlerName;
    public String batsmanName;

    public BallEvent(int runs, boolean isWicket, boolean isExtra, String extraType, boolean needsStrikeChange, boolean isStrikerOut, String bowlerName, String batsmanName) {
        this.runs = runs;
        this.isWicket = isWicket;
        this.isExtra = isExtra;
        this.extraType = extraType != null ? extraType : "";
        this.needsStrikeChange = needsStrikeChange;
        this.isStrikerOut = isStrikerOut;
        this.bowlerName = bowlerName;
        this.batsmanName = batsmanName;

        // Legal ball logic: Wide or No Ball is NOT a legal ball (doesn't count in over)
        // Byes/Leg Byes ARE legal balls (count in over)
        if ("WD".equals(this.extraType) || "NB".equals(this.extraType)) {
            this.isLegalBall = false;
        } else {
            this.isLegalBall = true;
        }
    }
}
