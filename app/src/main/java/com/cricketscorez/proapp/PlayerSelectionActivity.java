package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PlayerSelectionActivity extends Activity {

    ImageView btnBack;
    Button btnStartInnings;
    EditText etStriker, etNonStriker, etBowler;

    String team1, team2, overs, tossWinner, tossDecision;
    boolean isSecondInnings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_selection);

        Intent intent = getIntent();

        // সব extras সবসময় পড়া হচ্ছে
        isSecondInnings = intent.getBooleanExtra("IS_SECOND_INNINGS", false);
        team1        = intent.getStringExtra("TEAM_1");
        team2        = intent.getStringExtra("TEAM_2");
        overs        = intent.getStringExtra("TOTAL_OVERS");
        tossWinner   = intent.getStringExtra("TOSS_WINNER");
        tossDecision = intent.getStringExtra("TOSS_DECISION");

        // 1st innings হলে team/overs null-check
        if (!isSecondInnings) {
            if (team1 == null || team2 == null || overs == null) {
                Toast.makeText(this,
                        "Match data missing. Please restart the match setup.",
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        btnBack          = findViewById(R.id.btnBack);
        btnStartInnings  = findViewById(R.id.btnStartInnings);
        etStriker        = findViewById(R.id.etStriker);
        etNonStriker     = findViewById(R.id.etNonStriker);
        etBowler         = findViewById(R.id.etBowler);

        btnStartInnings.setText(isSecondInnings ? "Start 2nd Innings" : "Start Match");

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSecondInnings) setResult(RESULT_CANCELED);
                finish();
            }
        });

        btnStartInnings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String s  = etStriker.getText().toString().trim();
                final String ns = etNonStriker.getText().toString().trim();
                final String b  = etBowler.getText().toString().trim();

                if (s.isEmpty() || ns.isEmpty() || b.isEmpty()) {
                    Toast.makeText(PlayerSelectionActivity.this,
                            "Enter all player names", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isSecondInnings) {
                    // 2nd Innings: শুধু player names ফেরত পাঠানো
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("STRIKER",     s);
                    resultIntent.putExtra("NON_STRIKER", ns);
                    resultIntent.putExtra("BOWLER",      b);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    // 1st innings এ আসলেই checkTournamentAndStart call করা
                    checkTournamentAndStart(s, ns, b);
                }
            }
        });
    }

    // 🔥 Premium Tournament Dialog Update
    private void checkTournamentAndStart(final String striker,
                                         final String nonStriker,
                                         final String bowler) {
        if (team1 == null || team2 == null || overs == null) {
            Toast.makeText(this, "Match data missing. Please restart the match setup.", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences tourPrefs = getSharedPreferences("TournamentData", MODE_PRIVATE);
        String data = tourPrefs.getString("ALL_DATA", "");

        boolean foundMatch = false;
        String matchId = "";

        if (!data.isEmpty()) {
            try {
                JSONObject mainObj = new JSONObject(data);
                List<String> tourTeams = getTournamentTeamsList(mainObj);

                // ✅ FIX: সব স্টেজ চেক করা — Group, QF, SF, Final
                String[][] stages = {
                    {"MatchesGroup", "GroupMatch_"},
                    {"MatchesQF",    "QFMatch_"},
                    {"MatchesSF",    "SFMatch_"},
                    {"MatchesFinal", "FinalMatch_"}
                };

                outer:
                for (String[] stage : stages) {
                    JSONArray stageMatches = mainObj.optJSONArray(stage[0]);
                    if (stageMatches == null) continue;
                    for (int i = 0; i < stageMatches.length(); i++) {
                        JSONObject m = stageMatches.getJSONObject(i);
                        int idx1 = m.getInt("team1_idx");
                        int idx2 = m.getInt("team2_idx");
                        // idx 0 = "Select Team" placeholder — skip unset fixtures
                        if (idx1 == 0 || idx2 == 0) continue;
                        String t1 = tourTeams.get(idx1);
                        String t2 = tourTeams.get(idx2);

                        if ((team1.equalsIgnoreCase(t1) && team2.equalsIgnoreCase(t2)) ||
                            (team1.equalsIgnoreCase(t2) && team2.equalsIgnoreCase(t1))) {
                            foundMatch = true;
                            matchId = stage[1] + i;
                            break outer;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (foundMatch) {
            final String finalMatchId = matchId;
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(40, 50, 40, 40);
            container.setGravity(Gravity.CENTER_HORIZONTAL);
            
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(40);
            container.setBackground(bg);

            TextView icon = new TextView(this);
            icon.setText("🏆");
            icon.setTextSize(40);
            icon.setGravity(Gravity.CENTER);
            container.addView(icon);

            TextView title = new TextView(this);
            title.setText("Tournament Match");
            title.setTextSize(18);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.parseColor("#1E293B"));
            title.setPadding(0, 20, 0, 10);
            title.setGravity(Gravity.CENTER);
            container.addView(title);

            TextView msg = new TextView(this);
            msg.setText("This match matches a tournament fixture. Do you want to play it as a tournament match?");
            msg.setGravity(Gravity.CENTER);
            msg.setTextColor(Color.parseColor("#64748B"));
            msg.setPadding(0, 0, 0, 40);
            container.addView(msg);

            LinearLayout bRow = new LinearLayout(this);
            bRow.setOrientation(LinearLayout.HORIZONTAL);
            
            Button btnNo = new Button(this);
            btnNo.setText("Normal Match");
            btnNo.setAllCaps(false);
            btnNo.setTextColor(Color.parseColor("#64748B"));
            btnNo.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout.LayoutParams lpNo = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lpNo.setMargins(0, 0, 10, 0);
            btnNo.setLayoutParams(lpNo);
            btnNo.setOnClickListener(v -> {
                dialog.dismiss();
                launchMainActivity(striker, nonStriker, bowler, false, "");
            });

            Button btnYes = new Button(this);
            btnYes.setText("Yes, Play");
            btnYes.setAllCaps(false);
            btnYes.setTextColor(Color.WHITE);
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(Color.parseColor("#10B981"));
            btnBg.setCornerRadius(20);
            btnYes.setBackground(btnBg);
            btnYes.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            btnYes.setOnClickListener(v -> {
                dialog.dismiss();
                launchMainActivity(striker, nonStriker, bowler, true, finalMatchId);
            });

            bRow.addView(btnNo);
            bRow.addView(btnYes);
            container.addView(bRow);

            dialog.setContentView(container);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.85);
                dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dialog.show();
        } else {
            launchMainActivity(striker, nonStriker, bowler, false, "");
        }
    }

    private void launchMainActivity(String s, String ns, String b,
                                    boolean isTour, String tourId) {
        // ✅ FIX: আগের সেভ করা পুরো রোস্টার (TeamManagerActivity থেকে) মুছে না ফেলে
        // merge করা হচ্ছে — শুধু নতুন নাম (যদি রোস্টারে না থাকে) যোগ হবে
        ArrayList<String> p1 = mergePlayerIntoRoster(team1, s, ns);
        ArrayList<String> p2 = mergePlayerIntoRoster(team2, b);
        DataManager.saveTeam(getApplicationContext(), team1, p1);
        DataManager.saveTeam(getApplicationContext(), team2, p2);

        // ✅ Firebase-এ teams/{teamId}/players সিঙ্ক করা — viewer অ্যাপ/ওয়েবসাইট
        // থেকে টিমের পূর্ণাঙ্গ প্লেয়ার লিস্ট দেখা যাবে
        FirebaseSync.upsertTeam(team1, p1);
        FirebaseSync.upsertTeam(team2, p2);

        Intent i = new Intent(PlayerSelectionActivity.this, MainActivity.class);
        i.putExtra("TEAM_1",              team1);
        i.putExtra("TEAM_2",              team2);
        i.putExtra("TOTAL_OVERS",         overs);
        i.putExtra("TOSS_WINNER",         tossWinner);
        i.putExtra("TOSS_DECISION",       tossDecision);
        i.putExtra("STRIKER",             s);
        i.putExtra("NON_STRIKER",         ns);
        i.putExtra("BOWLER",              b);
        i.putExtra("IS_TOURNAMENT",       isTour);
        i.putExtra("TOURNAMENT_MATCH_ID", tourId);
        startActivity(i);
        finish();
    }

    // ✅ NEW: বিদ্যমান রোস্টারের সাথে নতুন নাম merge করে (duplicate ছাড়া)
    private ArrayList<String> mergePlayerIntoRoster(String teamName, String... newNames) {
        ArrayList<String> roster = DataManager.getPlayers(getApplicationContext(), teamName);
        if (roster == null) roster = new ArrayList<>();
        for (String name : newNames) {
            if (name == null || name.trim().isEmpty()) continue;
            boolean exists = false;
            for (String existing : roster) {
                if (existing.trim().equalsIgnoreCase(name.trim())) { exists = true; break; }
            }
            if (!exists) roster.add(name.trim());
        }
        return roster;
    }

    private List<String> getTournamentTeamsList(JSONObject mainObj) throws Exception {
        List<String> teams = new ArrayList<>();
        teams.add("Select Team");
        if (mainObj.has("DynamicGroups")) {
            JSONArray groups = mainObj.getJSONArray("DynamicGroups");
            for (int i = 0; i < groups.length(); i++) {
                JSONArray tArr = groups.getJSONObject(i).getJSONArray("teams");
                for (int j = 0; j < tArr.length(); j++) teams.add(tArr.getString(j));
            }
        }
        return teams;
    }
}
