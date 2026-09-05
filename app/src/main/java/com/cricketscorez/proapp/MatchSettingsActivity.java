package com.cricketscorez.proapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MatchSettingsActivity extends Activity {

    // ✅ FIX: btnBack সরানো হয়েছে, ivBack (ImageView) যোগ করা হয়েছে
    ImageView ivBack;
    Button    btnStartMatch;
    EditText  etTeam1, etTeam2, etOvers;
    RadioGroup rgTossDecision, rgTossWinner;
    RadioButton rbTeam1, rbTeam2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_settings);

        // ✅ FIX: header-এর ivBack ImageView bind করা হয়েছে
        ivBack        = (ImageView)   findViewById(R.id.ivBack);
        btnStartMatch = (Button)      findViewById(R.id.btnStartMatch);
        etTeam1       = (EditText)    findViewById(R.id.etTeam1);
        etTeam2       = (EditText)    findViewById(R.id.etTeam2);
        etOvers       = (EditText)    findViewById(R.id.etOvers);
        rgTossDecision = (RadioGroup) findViewById(R.id.rgTossDecision);
        rgTossWinner   = (RadioGroup) findViewById(R.id.rgTossWinner);
        rbTeam1        = (RadioButton) findViewById(R.id.rbTeam1);
        rbTeam2        = (RadioButton) findViewById(R.id.rbTeam2);

        // AppSettingsActivity-তে সেভ করা Default Overs লোড
        SharedPreferences appPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int defaultOvers = appPrefs.getInt("DEFAULT_OVERS", 20);
        etOvers.setText(String.valueOf(defaultOvers));

        // Team 1 নাম পরিবর্তনে RadioButton আপডেট
        etTeam1.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                rbTeam1.setText(s.toString().trim().isEmpty() ? "Team 1" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Team 2 নাম পরিবর্তনে RadioButton আপডেট
        etTeam2.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                rbTeam2.setText(s.toString().trim().isEmpty() ? "Team 2" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // ✅ FIX: ivBack & btnBack click listeners
        if (ivBack != null) {
            ivBack.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { finish(); }
            });
        }
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { finish(); }
            });
        }

        // 💾 Room Database Check: Detect active auto-saved match to prevent data loss
        com.cricketscorez.proapp.room.LiveMatchProgressRepository.getLatestActiveMatch(this, new com.cricketscorez.proapp.room.LiveMatchProgressRepository.OnMatchLoadedCallback() {
            @Override
            public void onLoaded(final MatchData activeMatch) {
                if (activeMatch != null && (activeMatch.matchStatus == null || activeMatch.matchStatus.isEmpty() || activeMatch.matchStatus.equals("Incomplete") || activeMatch.matchStatus.equals("Live"))) {
                    showResumeMatchDialog(activeMatch);
                }
            }

            @Override
            public void onNotFound() {}
        });

        btnStartMatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String t1    = etTeam1.getText().toString().trim();
                String t2    = etTeam2.getText().toString().trim();
                String overs = etOvers.getText().toString().trim();

                if (t1.isEmpty() || t2.isEmpty() || overs.isEmpty()) {
                    Toast.makeText(MatchSettingsActivity.this,
                            "Please enter Teams and Overs", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (t1.equalsIgnoreCase(t2)) {
                    Toast.makeText(MatchSettingsActivity.this,
                            "Team names must be different", Toast.LENGTH_SHORT).show();
                    return;
                }

                int selectedWinnerId = rgTossWinner.getCheckedRadioButtonId();
                if (selectedWinnerId == -1) {
                    Toast.makeText(MatchSettingsActivity.this,
                            "Please select who won the toss", Toast.LENGTH_SHORT).show();
                    return;
                }

                int selectedDecisionId = rgTossDecision.getCheckedRadioButtonId();
                if (selectedDecisionId == -1) {
                    Toast.makeText(MatchSettingsActivity.this,
                            "Please select Bat or Bowl", Toast.LENGTH_SHORT).show();
                    return;
                }

                String tossWinner = (selectedWinnerId == R.id.rbTeam2) ? t2 : t1;

                RadioButton rbDecision = (RadioButton) findViewById(selectedDecisionId);
                String tossDecision    = rbDecision.getText().toString();

                String tossInfo = tossWinner + " won the toss and elected to "
                        + tossDecision.toLowerCase() + " first";

                Intent intent = new Intent(MatchSettingsActivity.this, PlayerSelectionActivity.class);
                intent.putExtra("TEAM_1",        t1);
                intent.putExtra("TEAM_2",        t2);
                intent.putExtra("TOTAL_OVERS",   overs);
                intent.putExtra("TOSS_WINNER",   tossWinner);
                intent.putExtra("TOSS_DECISION", tossDecision);
                intent.putExtra("TOSS_INFO",     tossInfo);
                startActivity(intent);
            }
        });
    }

    private void showResumeMatchDialog(final MatchData activeMatch) {
        if (isFinishing()) return;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("⚡ Resume Active Match");
        String t1 = activeMatch.team1Name != null ? activeMatch.team1Name : "Team 1";
        String t2 = activeMatch.team2Name != null ? activeMatch.team2Name : "Team 2";
        builder.setMessage("An unfinished match was found in Room auto-save:\n\n" + t1 + " vs " + t2
                + "\nScore: " + activeMatch.getScoreString() + " (" + activeMatch.getOversString() + " ov)"
                + "\n\nWould you like to resume scoring this match or start fresh?");
        builder.setPositiveButton("Resume Scoring", (dialog, which) -> {
            Intent intent = new Intent(MatchSettingsActivity.this, MainActivity.class);
            intent.putExtra("RESUME_MATCH_DATA", activeMatch);
            startActivity(intent);
            finish();
        });
        builder.setNegativeButton("Start Fresh", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.setNeutralButton("Clear Saved", (dialog, which) -> {
            com.cricketscorez.proapp.room.LiveMatchProgressRepository.clearMatchProgress(MatchSettingsActivity.this, activeMatch.matchId);
            Toast.makeText(MatchSettingsActivity.this, "Auto-saved match cleared.", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }
}
