package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

// ── Retrofit Imports ──────────────────────────────────────────────
import com.cricketscorez.proapp.api.ApiClient;
import com.cricketscorez.proapp.api.ApiInterface;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TournamentSettingsActivity extends Activity {

    // ভেরিয়েবল লিস্ট
    ImageView btnBack, ivTournamentLogo; 
    Button btnSaveSettings, btnUploadLogo, btnRemoveLogo, btnResetData;
    EditText etOvers, etWinPts, etTiePts, etTournamentName;
    SharedPreferences prefs;

    private static final int REQUEST_LOGO_GALLERY  = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_settings);

        // View Binding
        btnBack          = findViewById(R.id.btnBack);
        btnSaveSettings  = findViewById(R.id.btnSaveSettings);
        etOvers          = findViewById(R.id.etOvers);
        etWinPts         = findViewById(R.id.etWinPts);
        etTiePts         = findViewById(R.id.etTiePts);
        etTournamentName = findViewById(R.id.etTournamentName);
        ivTournamentLogo = findViewById(R.id.ivTournamentLogo);
        btnUploadLogo    = findViewById(R.id.btnUploadLogo);
        btnRemoveLogo    = findViewById(R.id.btnRemoveLogo);
        btnResetData     = findViewById(R.id.btnResetData);

        prefs = getSharedPreferences("TournamentData", MODE_PRIVATE);

        // আগের ডাটা লোড করা
        loadSettings();

        // বাটনের কাজগুলো
        btnBack.setOnClickListener(v -> finish());
        btnSaveSettings.setOnClickListener(v -> saveSettings());
        btnUploadLogo.setOnClickListener(v -> launchGalleryIntent());
        if (btnRemoveLogo != null) {
            btnRemoveLogo.setOnClickListener(v -> removeTournamentLogo());
        }
        btnResetData.setOnClickListener(v -> showResetWarningDialog());
    }

    private void loadSettings() {
        etTournamentName.setText(prefs.getString("TOURNAMENT_NAME", ""));
        etOvers.setText(prefs.getString("TOTAL_OVERS", "20"));
        etWinPts.setText(prefs.getString("WIN_POINTS", "2"));
        etTiePts.setText(prefs.getString("TIE_POINTS", "1"));

        loadLogoPreview();
    }

    private void loadLogoPreview() {
        String logoPath = ImageStorageHelper.getTournamentLogoPath(this);
        if (logoPath != null) {
            ImageStorageHelper.loadTournamentLogoInto(this, ivTournamentLogo, android.R.drawable.ic_menu_gallery);
            if (btnRemoveLogo != null) btnRemoveLogo.setVisibility(View.VISIBLE);
        } else {
            ivTournamentLogo.setImageResource(android.R.drawable.ic_menu_gallery);
            if (btnRemoveLogo != null) btnRemoveLogo.setVisibility(View.GONE);
        }
    }

    private void removeTournamentLogo() {
        ImageStorageHelper.deleteTournamentLogo(this);
        loadLogoPreview();
        Toast.makeText(this, "Tournament logo removed", Toast.LENGTH_SHORT).show();
    }

    private void saveSettings() {
        String name = etTournamentName.getText().toString().trim();
        String overs = etOvers.getText().toString().trim();
        String win = etWinPts.getText().toString().trim();
        String tie = etTiePts.getText().toString().trim();

        if (name.isEmpty() || overs.isEmpty() || win.isEmpty() || tie.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.edit()
                .putString("TOURNAMENT_NAME", name)
                .putString("TOTAL_OVERS", overs)
                .putString("WIN_POINTS", win)
                .putString("TIE_POINTS", tie)
                .apply();

        // Supabase এ tournament sync করা হচ্ছে
        FirebaseSync.upsertTournament(name, overs, win, tie, (success, tournamentId) -> {
            if (success) {
                prefs.edit().putString("SUPABASE_TOURNAMENT_ID", tournamentId).apply();
            }
        });

        // ★ নিজস্ব MySQL সার্ভারে টুর্নামেন্ট সেভ করার কল
        saveTournamentToMySql(name, overs, win, tie);

        Toast.makeText(this, "✅ Settings Saved Successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ── নিজস্ব MySQL সার্ভারে টুর্নামেন্ট সেভ করার মেথড ────────────────
    private void saveTournamentToMySql(String name, String overs, String winPts, String tiePts) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        apiInterface.saveTournament(name, overs, winPts, tiePts).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("MYSQL_SYNC", "Tournament saved to MySQL successfully!");
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MYSQL_ERROR", "Failed to save Tournament: " + t.getMessage());
            }
        });
    }

    // ── নিজস্ব MySQL সার্ভার থেকে টুর্নামেন্ট ডিলিট করার মেথড ──────────────
    private void deleteTournamentFromMySql(String name) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        apiInterface.deleteTournament(name).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("MYSQL_SYNC", "Tournament deleted from MySQL successfully!");
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MYSQL_ERROR", "Failed to delete Tournament: " + t.getMessage());
            }
        });
    }

    // 🔥 Premium Danger Dialog for Reset
    private void showResetWarningDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(32), dp(24), dp(24));
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(20));
        container.setBackground(bg);

        TextView icon = new TextView(this);
        icon.setText("⚠️");
        icon.setTextSize(40);
        container.addView(icon);

        TextView title = new TextView(this);
        title.setText("Reset Tournament?");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#0F172A"));
        title.setPadding(0, dp(16), 0, dp(8));
        container.addView(title);

        TextView msg = new TextView(this);
        msg.setText("This will permanently delete all fixtures, results, rankings, and tournament data. This action cannot be undone.");
        msg.setGravity(Gravity.CENTER);
        msg.setTextColor(Color.parseColor("#64748B"));
        msg.setPadding(0, 0, 0, dp(32));
        container.addView(msg);

        LinearLayout buttons = new LinearLayout(this);

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setTextColor(Color.parseColor("#475569"));
        btnCancel.setAllCaps(false);
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1f));
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        Button btnConfirm = new Button(this);
        btnConfirm.setText("Reset All");
        btnConfirm.setTextColor(Color.WHITE);
        btnConfirm.setAllCaps(false);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#EF4444")); // Danger Red
        btnBg.setCornerRadius(dp(12));
        btnConfirm.setBackground(btnBg);
        btnConfirm.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1f));

        btnConfirm.setOnClickListener(v -> {
            
            // ★ ডিলিট করার আগে বর্তমান নামটা সেভ করে নিচ্ছি সার্ভারে পাঠানোর জন্য
            String currentTournamentName = prefs.getString("TOURNAMENT_NAME", "");
            if (!currentTournamentName.isEmpty()) {
                deleteTournamentFromMySql(currentTournamentName);
            }

            // 1. Clear Tournament Settings & Data
            ImageStorageHelper.deleteTournamentLogo(this);
            prefs.edit().clear().apply();

            // 2. Clear Tournament Results
            SharedPreferences resultPrefs = getSharedPreferences("TournamentResult", MODE_PRIVATE);
            resultPrefs.edit().clear().apply();

            // 3. Clear Group Data / Fixtures
            SharedPreferences fixturePrefs = getSharedPreferences("TournamentFixtures", MODE_PRIVATE);
            fixturePrefs.edit().clear().apply();

            // 4. 🔥 BUG FIX: Clear Player Rankings
            SharedPreferences rankingPrefs = getSharedPreferences("TournamentRankings", MODE_PRIVATE);
            rankingPrefs.edit().clear().apply();

            Toast.makeText(this, "Tournament data has been completely reset", Toast.LENGTH_SHORT).show();
            
            // Clear input fields
            etTournamentName.setText("");
            etOvers.setText("20");
            etWinPts.setText("2");
            etTiePts.setText("1");
            ivTournamentLogo.setImageResource(android.R.drawable.ic_menu_gallery);
            
            dialog.dismiss();
        });

        buttons.addView(btnCancel);
        buttons.addView(btnConfirm);
        container.addView(buttons);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private int dp(int val) { return Math.round(val * getResources().getDisplayMetrics().density); }

    private void launchGalleryIntent() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Tournament Logo"), REQUEST_LOGO_GALLERY);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_LOGO_GALLERY);
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open gallery", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOGO_GALLERY && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            String savedPath = ImageStorageHelper.saveTournamentLogo(this, selectedImageUri);
            if (savedPath != null) {
                loadLogoPreview();
                Toast.makeText(this, "✅ Tournament logo saved successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save logo. Please try another image.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
