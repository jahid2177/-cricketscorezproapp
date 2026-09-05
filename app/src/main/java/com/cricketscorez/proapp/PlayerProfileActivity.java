package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import org.json.JSONException;
import org.json.JSONObject;

// ── Retrofit Imports ──────────────────────────────────────────────
import com.cricketscorez.proapp.api.ApiClient;
import com.cricketscorez.proapp.api.ApiInterface;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerProfileActivity extends Activity {

    private String playerName;
    private String teamName; // সার্ভারে আপডেট করার জন্য টিমের নাম প্রয়োজন

    private TextView tvName, tvRole, tvProfileInitial;
    private ImageView ivProfileImage;
    private FrameLayout layoutPhotoContainer;
    private GridLayout gridStats;
    private Button tabBat, tabBowl, tabField;

    private JSONObject stats;

    private static final int REQUEST_PLAYER_PHOTO = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_profile);

        playerName = getIntent().getStringExtra("PLAYER_NAME");
        teamName = getIntent().getStringExtra("TEAM_NAME"); 
        if (teamName == null) teamName = "Unknown Team";

        tvName = (TextView) findViewById(R.id.tvProfileName);
        tvRole = (TextView) findViewById(R.id.tvProfileRole);
        tvProfileInitial = (TextView) findViewById(R.id.tvProfileInitial);
        ivProfileImage = (ImageView) findViewById(R.id.ivPlayerProfileImage);
        layoutPhotoContainer = (FrameLayout) findViewById(R.id.layoutPhotoContainer);
        gridStats = (GridLayout) findViewById(R.id.gridStats);

        tabBat = (Button) findViewById(R.id.tabBatting);
        tabBowl = (Button) findViewById(R.id.tabBowling);
        tabField = (Button) findViewById(R.id.tabFielding);

        ImageView btnBack = (ImageView) findViewById(R.id.btnBackProfile);

        tvName.setText(playerName);
        tvRole.setText(DataManager.getPlayerRole(this, playerName));

        if (playerName != null && !playerName.trim().isEmpty()) {
            tvProfileInitial.setText(playerName.trim().substring(0, 1).toUpperCase());
        }

        // Load Player Photo
        loadPlayerPhoto();

        boolean isViewerMode = getIntent().getBooleanExtra("IS_VIEWER_MODE", false);

        View cameraBadge = findViewById(R.id.btnCameraBadge);
        if (isViewerMode) {
            if (cameraBadge != null) cameraBadge.setVisibility(View.GONE);
            if (layoutPhotoContainer != null) {
                layoutPhotoContainer.setClickable(false);
                layoutPhotoContainer.setFocusable(false);
            }
        } else {
            if (cameraBadge != null) cameraBadge.setVisibility(View.VISIBLE);
            // Photo Upload / Change Click
            if (layoutPhotoContainer != null) {
                layoutPhotoContainer.setOnClickListener(v -> showPhotoOptionsDialog());
            }
        }

        DataManager.ensurePlayerStats(this, playerName);
        stats = DataManager.getPlayerStats(this, playerName);

        btnBack.setOnClickListener(v -> finish());

        if (!isViewerMode) {
            // ★ অ্যাডমিন ট্রিক: প্লেয়ারের নামের ওপর চেপে ধরে রাখলে এডিট ফর্ম আসবে
            tvName.setOnLongClickListener(v -> {
                showEditStatsDialog();
                return true;
            });
            Toast.makeText(this, "💡 Tap on avatar to upload photo, Long press on name to edit stats", Toast.LENGTH_LONG).show();
        }

        tabBat.setOnClickListener(v -> {
            loadBattingStats();
            highlightTab(tabBat);
        });

        tabBowl.setOnClickListener(v -> {
            loadBowlingStats();
            highlightTab(tabBowl);
        });

        tabField.setOnClickListener(v -> {
            loadFieldingStats();
            highlightTab(tabField);
        });

        loadBattingStats();
        highlightTab(tabBat);
    }

    @Override
    protected void onResume() {
        super.onResume();
        stats = DataManager.getPlayerStats(this, playerName);
        loadBattingStats();
        loadPlayerPhoto();
    }

    private void loadPlayerPhoto() {
        ImageStorageHelper.loadPlayerPhotoInto(this, playerName, ivProfileImage, tvProfileInitial);
    }

    private void showPhotoOptionsDialog() {
        String existingPhoto = ImageStorageHelper.getPlayerPhotoPath(this, playerName);
        String[] options;
        if (existingPhoto != null) {
            options = new String[]{"🖼️ Choose from Gallery", "🗑️ Remove Photo", "Cancel"};
        } else {
            options = new String[]{"🖼️ Choose from Gallery", "Cancel"};
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("👤 Player Photo - " + playerName);
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                openGalleryPicker();
            } else if (which == 1 && existingPhoto != null) {
                ImageStorageHelper.deletePlayerPhoto(this, playerName);
                loadPlayerPhoto();
                Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void openGalleryPicker() {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
            startActivityForResult(android.content.Intent.createChooser(intent, "Select Player Photo"), REQUEST_PLAYER_PHOTO);
        } catch (Exception e) {
            try {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_PLAYER_PHOTO);
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open gallery", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PLAYER_PHOTO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri selectedUri = data.getData();
            String savedPath = ImageStorageHelper.savePlayerPhoto(this, playerName, selectedUri);
            if (savedPath != null) {
                loadPlayerPhoto();
                Toast.makeText(this, "✅ Player photo saved successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save image. Please try another.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Edit Stats Dialog (ডায়নামিক ফর্ম)
    // ─────────────────────────────────────────────────────────────────────────
    private void showEditStatsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✏️ Edit Player Stats");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        // Batting Inputs
        addSectionTitle(layout, "🏏 Batting");
        EditText etMatches = addField(layout, "Matches", stats.optString("matches", "0"));
        EditText etInnings = addField(layout, "Innings", stats.optString("innings", "0"));
        EditText etRuns    = addField(layout, "Runs", stats.optString("runs", "0"));
        EditText etBalls   = addField(layout, "Balls Faced", stats.optString("balls", "0"));
        EditText etBest    = addField(layout, "Highest Score", stats.optString("best_score", "0"));
        EditText et50s     = addField(layout, "50s", stats.optString("50s", "0"));
        EditText et100s    = addField(layout, "100s", stats.optString("100s", "0"));

        // Bowling Inputs
        addSectionTitle(layout, "⚾ Bowling");
        EditText etOvers      = addField(layout, "Overs", stats.optString("overs", "0"));
        EditText etWickets    = addField(layout, "Wickets", stats.optString("wickets", "0"));
        EditText etBowlRuns   = addField(layout, "Runs Conceded", stats.optString("bowl_runs", "0"));
        EditText etMaidens    = addField(layout, "Maidens", stats.optString("maidens", "0"));

        // Fielding Inputs
        addSectionTitle(layout, "🧤 Fielding");
        EditText etCatches   = addField(layout, "Catches", stats.optString("catches", "0"));
        EditText etStumpings = addField(layout, "Stumpings", stats.optString("stumpings", "0"));
        EditText etRunOuts   = addField(layout, "Run Outs", stats.optString("runouts", "0"));

        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                // লোকাল JSON Object আপডেট করা
                stats.put("matches", Integer.parseInt(etMatches.getText().toString().trim()));
                stats.put("innings", Integer.parseInt(etInnings.getText().toString().trim()));
                stats.put("runs", Integer.parseInt(etRuns.getText().toString().trim()));
                stats.put("balls", Integer.parseInt(etBalls.getText().toString().trim()));
                stats.put("best_score", Integer.parseInt(etBest.getText().toString().trim()));
                stats.put("50s", Integer.parseInt(et50s.getText().toString().trim()));
                stats.put("100s", Integer.parseInt(et100s.getText().toString().trim()));
                
                stats.put("overs", Double.parseDouble(etOvers.getText().toString().trim()));
                stats.put("wickets", Integer.parseInt(etWickets.getText().toString().trim()));
                stats.put("bowl_runs", Integer.parseInt(etBowlRuns.getText().toString().trim()));
                stats.put("maidens", Integer.parseInt(etMaidens.getText().toString().trim()));

                stats.put("catches", Integer.parseInt(etCatches.getText().toString().trim()));
                stats.put("stumpings", Integer.parseInt(etStumpings.getText().toString().trim()));
                stats.put("runouts", Integer.parseInt(etRunOuts.getText().toString().trim()));

                // লোকাল ডাটাবেসে সেভ (DataManager-এর মাধ্যমে)
                // (যদি আপনার DataManager-এ savePlayerStats নামে মেথড না থাকে, তবে সেটি তৈরি করে নেবেন)
                DataManager.savePlayerStats(this, playerName, stats);

                // ★ সার্ভারে (MySQL) আপডেট করার জন্য API কল
                updateStatsToMySql(stats);

                // UI রিফ্রেশ
                loadBattingStats();
                highlightTab(tabBat);
                Toast.makeText(this, "✅ Stats Updated Successfully!", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(this, "Error parsing numbers", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ফর্ম তৈরি করার সহায়ক মেথড
    private void addSectionTitle(LinearLayout layout, String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#004D40"));
        tv.setPadding(0, 30, 0, 10);
        layout.addView(tv);
    }

    private EditText addField(LinearLayout layout, String label, String value) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setPadding(0, 10, 0, 0);
        layout.addView(tv);

        EditText et = new EditText(this);
        et.setText(value);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(et);
        return et;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MySQL সার্ভারে Player Stats আপডেট করার মেথড (Retrofit)
    // ─────────────────────────────────────────────────────────────────────────
    private void updateStatsToMySql(JSONObject updatedStats) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);

        int matches = updatedStats.optInt("matches", 0);
        int innings = updatedStats.optInt("innings", 0);
        int runs = updatedStats.optInt("runs", 0);
        int ballsFaced = updatedStats.optInt("balls", 0);
        int highestScore = updatedStats.optInt("best_score", 0);
        int fifties = updatedStats.optInt("50s", 0);
        int hundreds = updatedStats.optInt("100s", 0);
        
        int wickets = updatedStats.optInt("wickets", 0);
        double overs = updatedStats.optDouble("overs", 0);
        int ballsBowled = (int) (overs * 6); // ওভারকে বলে কনভার্ট করা হলো
        int runsConceded = updatedStats.optInt("bowl_runs", 0);
        int maidens = updatedStats.optInt("maidens", 0);
        
        int catches = updatedStats.optInt("catches", 0);
        int stumpings = updatedStats.optInt("stumpings", 0);
        int runOuts = updatedStats.optInt("runouts", 0);

        // API Call
        apiInterface.updatePlayerStats(
            teamName, playerName, matches, innings, runs, ballsFaced, highestScore,
            fifties, hundreds, 0, 0, wickets, ballsBowled, runsConceded, maidens, 
            catches, stumpings, runOuts
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if(response.isSuccessful()){
                    Log.d("MYSQL_STATS", "Player stats successfully synced to your server!");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MYSQL_ERROR", "Failed to sync stats: " + t.getMessage());
            }
        });
    }

    /* ================= TAB UI ================= */

    private void highlightTab(Button active) {
        resetTab(tabBat);
        resetTab(tabBowl);
        resetTab(tabField);

        active.setBackgroundColor(Color.parseColor("#E0F2F1"));
        active.setTextColor(Color.parseColor("#004D40"));
    }

    private void resetTab(Button btn) {
        btn.setBackgroundColor(Color.WHITE);
        btn.setTextColor(Color.parseColor("#00796B"));
    }

    /* ================= STAT CARD ================= */

    private void addStatCard(String title, String value) {

        LinearLayout card = new LinearLayout(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(16, 16, 16, 16);
        card.setLayoutParams(params);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(30, 40, 30, 40);
        card.setBackgroundColor(Color.WHITE);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(12);
        tvTitle.setTextColor(Color.GRAY);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(20);
        tvValue.setTypeface(Typeface.DEFAULT_BOLD);
        tvValue.setTextColor(Color.parseColor("#004D40"));

        card.addView(tvTitle);
        card.addView(tvValue);

        gridStats.addView(card);
    }

    /* ================= BATTING ================= */

    private void loadBattingStats() {
        gridStats.removeAllViews();

        int matches = stats.optInt("matches", 0);
        int innings = stats.optInt("innings", 0);
        int runs = stats.optInt("runs", 0);
        int balls = stats.optInt("balls", 0);
        int notOut = stats.optInt("not_outs", 0);
        int outs = innings - notOut;

        double avg = outs > 0 ? (double) runs / outs : runs;
        double sr = balls > 0 ? ((double) runs / balls) * 100 : 0;

        addStatCard("Matches", String.valueOf(matches));
        addStatCard("Innings", String.valueOf(innings));
        addStatCard("Runs", String.valueOf(runs));
        addStatCard("Not Out", String.valueOf(notOut));
        addStatCard("Best", String.valueOf(stats.optInt("best_score", 0)));
        addStatCard("Average", String.format("%.2f", avg));
        addStatCard("Strike Rate", String.format("%.2f", sr));
        addStatCard("4s", String.valueOf(stats.optInt("fours", 0)));
        addStatCard("6s", String.valueOf(stats.optInt("sixes", 0)));
        addStatCard("30s", String.valueOf(stats.optInt("30s", 0)));
        addStatCard("50s", String.valueOf(stats.optInt("50s", 0)));
        addStatCard("100s", String.valueOf(stats.optInt("100s", 0)));
        addStatCard("Ducks", String.valueOf(stats.optInt("ducks", 0)));
    }

    /* ================= BOWLING ================= */

    private void loadBowlingStats() {
        gridStats.removeAllViews();

        int wickets = stats.optInt("wickets", 0);
        int runs = stats.optInt("bowl_runs", 0);
        double overs = stats.optDouble("overs", 0);

        double econ = overs > 0 ? runs / overs : 0;
        double avg = wickets > 0 ? (double) runs / wickets : 0;

        addStatCard("Matches", String.valueOf(stats.optInt("matches", 0)));
        addStatCard("Innings", String.valueOf(stats.optInt("bowl_innings", 0)));
        addStatCard("Overs", String.format("%.1f", overs));
        addStatCard("Maidens", String.valueOf(stats.optInt("maidens", 0)));
        addStatCard("Wickets", String.valueOf(wickets));
        addStatCard("Runs", String.valueOf(runs));
        addStatCard("Average", String.format("%.2f", avg));
        addStatCard("Economy", String.format("%.2f", econ));
        addStatCard("4 Wkts", String.valueOf(stats.optInt("4w", 0)));
        addStatCard("5 Wkts", String.valueOf(stats.optInt("5w", 0)));
    }

    /* ================= FIELDING ================= */

    private void loadFieldingStats() {
        gridStats.removeAllViews();

        addStatCard("Matches", String.valueOf(stats.optInt("matches", 0)));
        addStatCard("Catches", String.valueOf(stats.optInt("catches", 0)));
        addStatCard("Run Outs", String.valueOf(stats.optInt("runouts", 0)));
        addStatCard("Stumpings", String.valueOf(stats.optInt("stumpings", 0)));
    }
}
