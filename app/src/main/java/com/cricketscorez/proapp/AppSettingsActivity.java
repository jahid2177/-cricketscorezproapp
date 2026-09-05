package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * AppSettingsActivity — অ্যাপের সাধারণ সেটিংস স্ক্রিন।
 * HomeActivity.java-এর btnSettings থেকে এখানে navigate করা হয়।
 */
public class AppSettingsActivity extends Activity {

    // Views
    Button  btnBack, btnSaveSettings, btnClearHistory;
    EditText etDefaultOvers;
    Switch  swSoundEffects, swKeepScreenOn, swShowRunRate;
    TextView tvVersion;

    // SharedPreferences
    SharedPreferences appPrefs;
    private static final String PREF_NAME          = "AppSettings";
    private static final String KEY_DEFAULT_OVERS   = "DEFAULT_OVERS";
    private static final String KEY_SOUND           = "SOUND_EFFECTS";
    private static final String KEY_KEEP_SCREEN     = "KEEP_SCREEN_ON";
    private static final String KEY_SHOW_RUN_RATE   = "SHOW_RUN_RATE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_settings);

        ThemeManager.applyStatusBar(this);

        appPrefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // --- View Binding ---
        btnBack          = findViewById(R.id.btnBack);
        btnSaveSettings  = findViewById(R.id.btnSaveSettings);
        btnClearHistory  = findViewById(R.id.btnClearHistory);
        etDefaultOvers   = findViewById(R.id.etDefaultOvers);
        swSoundEffects   = findViewById(R.id.swSoundEffects);
        swKeepScreenOn   = findViewById(R.id.swKeepScreenOn);
        swShowRunRate    = findViewById(R.id.swShowRunRate);
        tvVersion        = findViewById(R.id.tvVersion);

        // --- সেভ করা সেটিংস লোড করা ---
        loadSettings();

        // --- App Version দেখানো ---
        try {
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            tvVersion.setText("Version " + versionName);
        } catch (Exception e) {
            tvVersion.setText("Version 1.0");
        }

        // --- Back Button ---
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        // --- Save Button ---
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String oversStr = etDefaultOvers.getText().toString().trim();
                if (oversStr.isEmpty()) {
                    Toast.makeText(AppSettingsActivity.this,
                            "Please enter default overs", Toast.LENGTH_SHORT).show();
                    return;
                }
                int overs = Integer.parseInt(oversStr);
                if (overs < 1 || overs > 50) {
                    Toast.makeText(AppSettingsActivity.this,
                            "Overs must be between 1 and 50", Toast.LENGTH_SHORT).show();
                    return;
                }

                SharedPreferences.Editor editor = appPrefs.edit();
                editor.putInt(KEY_DEFAULT_OVERS,  overs);
                editor.putBoolean(KEY_SOUND,       swSoundEffects.isChecked());
                editor.putBoolean(KEY_KEEP_SCREEN, swKeepScreenOn.isChecked());
                editor.putBoolean(KEY_SHOW_RUN_RATE, swShowRunRate.isChecked());
                editor.apply();

                Toast.makeText(AppSettingsActivity.this,
                        "✅ Settings saved!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // --- Clear History Button ---
        btnClearHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPremiumClearHistoryDialog();
            }
        });
    }

    private void showPremiumClearHistoryDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(32), dp(24), dp(24));
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dp(20));
        container.setBackground(bgShape);

        // Warning icon
        TextView iconView = new TextView(this);
        iconView.setText("⚠️");
        iconView.setTextSize(40);
        iconView.setGravity(Gravity.CENTER);
        container.addView(iconView);

        // Title
        TextView title = new TextView(this);
        title.setText("Clear Match History?");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, dp(16), 0, dp(8));
        title.setGravity(Gravity.CENTER);
        container.addView(title);

        // Warning label
        TextView warningLabel = new TextView(this);
        warningLabel.setText("This action cannot be undone!");
        warningLabel.setTextSize(13);
        warningLabel.setTypeface(null, Typeface.BOLD);
        warningLabel.setTextColor(Color.parseColor("#EF4444"));
        warningLabel.setGravity(Gravity.CENTER);
        warningLabel.setPadding(0, 0, 0, dp(6));
        container.addView(warningLabel);

        // Message
        TextView message = new TextView(this);
        message.setText("সব Match History মুছে ফেলা হবে।\nআপনি কি নিশ্চিত?");
        message.setTextSize(14);
        message.setTextColor(Color.parseColor("#64748B"));
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 0, 0, dp(28));
        container.addView(message);

        // Buttons row
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Cancel button
        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#475569"));
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setAllCaps(false);
        btnCancel.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(48), 1.0f);
        cancelParams.setMargins(0, 0, dp(8), 0);
        btnCancel.setLayoutParams(cancelParams);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Clear button
        Button btnClear = new Button(this);
        btnClear.setText("Yes, Clear");
        btnClear.setTextColor(Color.WHITE);
        btnClear.setAllCaps(false);
        btnClear.setTypeface(null, Typeface.BOLD);
        GradientDrawable clearBg = new GradientDrawable();
        clearBg.setColor(Color.parseColor("#EF4444"));
        clearBg.setCornerRadius(dp(12));
        btnClear.setBackground(clearBg);
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, dp(48), 1.0f);
        btnClear.setLayoutParams(clearParams);
        btnClear.setOnClickListener(v -> {
            dialog.dismiss();
            // ✅ FIX: সঠিক key "MatchHistoryDB" দিয়ে clear করা হচ্ছে
            DataManager.clearAllMatches(AppSettingsActivity.this);
            // পুরনো key-ও clear করা (backward compat)
            getSharedPreferences("MatchHistory", MODE_PRIVATE).edit().clear().apply();
            try {
                DatabaseHelper db = new DatabaseHelper(AppSettingsActivity.this);
                db.clearAllMatchHistory();
            } catch (Exception ignored) {}
            Toast.makeText(AppSettingsActivity.this,
                    "✅ Match history cleared!", Toast.LENGTH_SHORT).show();
        });

        btnLayout.addView(btnCancel);
        btnLayout.addView(btnClear);
        container.addView(btnLayout);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.85),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    /** সেভ করা সেটিংস UI-তে লোড করা */
    private void loadSettings() {
        etDefaultOvers.setText(String.valueOf(appPrefs.getInt(KEY_DEFAULT_OVERS, 20)));
        swSoundEffects.setChecked(appPrefs.getBoolean(KEY_SOUND, true));
        swKeepScreenOn.setChecked(appPrefs.getBoolean(KEY_KEEP_SCREEN, false));
        swShowRunRate.setChecked(appPrefs.getBoolean(KEY_SHOW_RUN_RATE, true));
    }
}
