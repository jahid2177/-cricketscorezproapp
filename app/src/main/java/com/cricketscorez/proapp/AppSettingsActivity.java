package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AppSettingsActivity — অ্যাপের সাধারণ সেটিংস স্ক্রিন।
 * HomeActivity.java-এর btnSettings থেকে এখানে navigate করা হয়।
 */
public class AppSettingsActivity extends Activity {

    // Views
    Button  btnBack, btnSaveSettings, btnClearHistory, btnExportJson, btnImportJson, btnShareBackup, btnGoogleDriveBackup;
    EditText etDefaultOvers;
    Switch  swSoundEffects, swKeepScreenOn, swShowRunRate;
    TextView tvVersion;

    private static final int REQUEST_CODE_EXPORT = 2001;
    private static final int REQUEST_CODE_IMPORT = 2002;

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
        btnExportJson    = findViewById(R.id.btnExportJson);
        btnImportJson    = findViewById(R.id.btnImportJson);
        btnShareBackup   = findViewById(R.id.btnShareBackup);
        btnGoogleDriveBackup = findViewById(R.id.btnGoogleDriveBackup);
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

        // --- Backup & Restore Listeners ---
        if (btnExportJson != null) {
            btnExportJson.setOnClickListener(v -> startExportFlow());
        }
        if (btnImportJson != null) {
            btnImportJson.setOnClickListener(v -> startImportFlow());
        }
        if (btnShareBackup != null) {
            btnShareBackup.setOnClickListener(v -> shareBackupDirectly());
        }
        if (btnGoogleDriveBackup != null) {
            btnGoogleDriveBackup.setOnClickListener(v -> showGoogleDriveBackupDialog());
        }

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

    // ═══════════════════════════════════════════════════════════════════════
    //  JSON DATA BACKUP & RESTORE
    // ═══════════════════════════════════════════════════════════════════════

    private void showGoogleDriveBackupDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(28), dp(24), dp(24));
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dp(20));
        container.setBackground(bgShape);

        TextView iconView = new TextView(this);
        iconView.setText("☁️");
        iconView.setTextSize(40);
        iconView.setGravity(Gravity.CENTER);
        container.addView(iconView);

        TextView title = new TextView(this);
        title.setText("Google Drive Backup");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, dp(10), 0, dp(6));
        title.setGravity(Gravity.CENTER);
        container.addView(title);

        TextView msg = new TextView(this);
        msg.setText("Securely sync your entire match history, tournaments, teams, and player stats directly with your Google Drive.\n\nWhen the system file picker opens, choose 'Drive' or 'Google Drive' from the drawer menu.");
        msg.setTextSize(13);
        msg.setTextColor(Color.parseColor("#475569"));
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, 0, 0, dp(20));
        container.addView(msg);

        // Upload to Drive Button
        Button btnUpload = new Button(this);
        btnUpload.setText("📤 Save Backup to Google Drive");
        btnUpload.setTextColor(Color.WHITE);
        btnUpload.setTextSize(13);
        btnUpload.setAllCaps(false);
        btnUpload.setTypeface(null, Typeface.BOLD);
        GradientDrawable uploadBg = new GradientDrawable();
        uploadBg.setColor(Color.parseColor("#1D4ED8"));
        uploadBg.setCornerRadius(dp(12));
        btnUpload.setBackground(uploadBg);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        p1.setMargins(0, 0, 0, dp(10));
        btnUpload.setLayoutParams(p1);
        btnUpload.setOnClickListener(v -> {
            dialog.dismiss();
            startExportFlow();
        });
        container.addView(btnUpload);

        // Restore from Drive Button
        Button btnRestore = new Button(this);
        btnRestore.setText("📥 Restore from Google Drive");
        btnRestore.setTextColor(Color.parseColor("#1D4ED8"));
        btnRestore.setTextSize(13);
        btnRestore.setAllCaps(false);
        btnRestore.setTypeface(null, Typeface.BOLD);
        GradientDrawable restoreBg = new GradientDrawable();
        restoreBg.setColor(Color.parseColor("#EFF6FF"));
        restoreBg.setCornerRadius(dp(12));
        btnRestore.setBackground(restoreBg);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        p2.setMargins(0, 0, 0, dp(10));
        btnRestore.setLayoutParams(p2);
        btnRestore.setOnClickListener(v -> {
            dialog.dismiss();
            startImportFlow();
        });
        container.addView(btnRestore);

        // Cancel
        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setAllCaps(false);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        container.addView(btnCancel);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.88), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void startExportFlow() {
        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = DataBackupHelper.BACKUP_FILE_PREFIX + timestamp + ".json";
            intent.putExtra(Intent.EXTRA_TITLE, fileName);
            startActivityForResult(intent, REQUEST_CODE_EXPORT);
        } catch (Exception e) {
            // Fallback for devices without document picker: offer direct file share
            shareBackupDirectly();
        }
    }

    private void startImportFlow() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimeTypes = {"application/json", "text/plain", "application/octet-stream", "*/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityForResult(intent, REQUEST_CODE_IMPORT);
        } catch (Exception e) {
            Toast.makeText(this, "File picker could not be opened: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareBackupDirectly() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Generating JSON backup...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                String jsonStr = DataBackupHelper.exportAllDataToString(AppSettingsActivity.this);
                File file = DataBackupHelper.createShareableBackupFile(AppSettingsActivity.this, jsonStr);
                runOnUiThread(() -> {
                    pd.dismiss();
                    if (file != null && file.exists()) {
                        Intent shareIntent = DataBackupHelper.getShareIntent(AppSettingsActivity.this, file);
                        startActivity(shareIntent);
                    } else {
                        Toast.makeText(AppSettingsActivity.this, "Failed to create backup file", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(AppSettingsActivity.this, "Export error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        if (requestCode == REQUEST_CODE_EXPORT) {
            handleExportToUri(uri);
        } else if (requestCode == REQUEST_CODE_IMPORT) {
            handleImportFromUri(uri);
        }
    }

    private void handleExportToUri(Uri destinationUri) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Exporting data to JSON...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                String jsonStr = DataBackupHelper.exportAllDataToString(AppSettingsActivity.this);
                try (OutputStream os = getContentResolver().openOutputStream(destinationUri)) {
                    boolean success = DataBackupHelper.writeJsonToStream(os, jsonStr);
                    runOnUiThread(() -> {
                        pd.dismiss();
                        if (success) {
                            showBackupResultDialog("Export Complete! 📤",
                                    "All teams, players, match history, tournaments, and settings have been exported successfully to your selected file.",
                                    true);
                        } else {
                            showBackupResultDialog("Export Failed ⚠️",
                                    "Could not write data to the selected location.",
                                    false);
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    showBackupResultDialog("Export Error ⚠️",
                            "An unexpected error occurred: " + e.getMessage(),
                            false);
                });
            }
        }).start();
    }

    private void handleImportFromUri(Uri sourceUri) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Reading backup file...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                String jsonStr;
                try (InputStream is = getContentResolver().openInputStream(sourceUri)) {
                    jsonStr = DataBackupHelper.readJsonFromStream(is);
                }

                if (jsonStr == null || jsonStr.trim().isEmpty()) {
                    runOnUiThread(() -> {
                        pd.dismiss();
                        showBackupResultDialog("Invalid Backup ⚠️", "The selected file is empty or unreadable.", false);
                    });
                    return;
                }

                DataBackupHelper.BackupInspection inspection = DataBackupHelper.inspectBackup(jsonStr);

                runOnUiThread(() -> {
                    pd.dismiss();
                    if (!inspection.isValid) {
                        showBackupResultDialog("Invalid Backup ⚠️",
                                "The selected file does not contain valid CricketScorez backup data.\n" +
                                        (inspection.errorMessage != null ? inspection.errorMessage : ""),
                                false);
                    } else {
                        showImportConfirmDialog(jsonStr, inspection);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    showBackupResultDialog("Import Error ⚠️", "Error reading backup file: " + e.getMessage(), false);
                });
            }
        }).start();
    }

    private void showImportConfirmDialog(String jsonStr, DataBackupHelper.BackupInspection inspection) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(28), dp(24), dp(20));

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dp(20));
        container.setBackground(bgShape);

        // Header Icon
        TextView iconView = new TextView(this);
        iconView.setText("📦");
        iconView.setTextSize(36);
        iconView.setGravity(Gravity.CENTER);
        container.addView(iconView);

        // Title
        TextView title = new TextView(this);
        title.setText("Restore Data from Backup?");
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, dp(12), 0, dp(6));
        title.setGravity(Gravity.CENTER);
        container.addView(title);

        // Inspection Details Box
        LinearLayout detailsBox = new LinearLayout(this);
        detailsBox.setOrientation(LinearLayout.VERTICAL);
        detailsBox.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setColor(Color.parseColor("#F8FAFC"));
        boxBg.setCornerRadius(dp(12));
        boxBg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        detailsBox.setBackground(boxBg);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxLp.setMargins(0, dp(8), 0, dp(16));
        detailsBox.setLayoutParams(boxLp);

        TextView tvSummary = new TextView(this);
        tvSummary.setText(inspection.getSummaryText());
        tvSummary.setTextSize(13);
        tvSummary.setTextColor(Color.parseColor("#334155"));
        tvSummary.setLineSpacing(dp(3), 1f);
        detailsBox.addView(tvSummary);
        container.addView(detailsBox);

        // Prompt
        TextView prompt = new TextView(this);
        prompt.setText("Choose how you would like to restore:");
        prompt.setTextSize(13);
        prompt.setTextColor(Color.parseColor("#64748B"));
        prompt.setGravity(Gravity.CENTER);
        prompt.setPadding(0, 0, 0, dp(16));
        container.addView(prompt);

        // Button 1: Merge & Add
        Button btnMerge = new Button(this);
        btnMerge.setText("Merge / Add to Existing Data");
        btnMerge.setTextColor(Color.WHITE);
        btnMerge.setTextSize(13);
        btnMerge.setAllCaps(false);
        btnMerge.setTypeface(null, Typeface.BOLD);
        GradientDrawable mergeBg = new GradientDrawable();
        mergeBg.setColor(Color.parseColor("#2563EB"));
        mergeBg.setCornerRadius(dp(12));
        btnMerge.setBackground(mergeBg);
        LinearLayout.LayoutParams mergeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        mergeParams.setMargins(0, 0, 0, dp(8));
        btnMerge.setLayoutParams(mergeParams);
        btnMerge.setOnClickListener(v -> {
            dialog.dismiss();
            executeImport(jsonStr, false);
        });
        container.addView(btnMerge);

        // Button 2: Replace All
        Button btnReplace = new Button(this);
        btnReplace.setText("Replace All Current Data");
        btnReplace.setTextColor(Color.WHITE);
        btnReplace.setTextSize(13);
        btnReplace.setAllCaps(false);
        btnReplace.setTypeface(null, Typeface.BOLD);
        GradientDrawable replaceBg = new GradientDrawable();
        replaceBg.setColor(Color.parseColor("#DC2626"));
        replaceBg.setCornerRadius(dp(12));
        btnReplace.setBackground(replaceBg);
        LinearLayout.LayoutParams replaceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        replaceParams.setMargins(0, 0, 0, dp(8));
        btnReplace.setLayoutParams(replaceParams);
        btnReplace.setOnClickListener(v -> {
            dialog.dismiss();
            executeImport(jsonStr, true);
        });
        container.addView(btnReplace);

        // Cancel button
        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setAllCaps(false);
        btnCancel.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
        btnCancel.setLayoutParams(cancelParams);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        container.addView(btnCancel);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.90),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void executeImport(String jsonStr, boolean overwrite) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Restoring data from backup...");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            DataBackupHelper.ImportResult result = DataBackupHelper.importDataFromJson(AppSettingsActivity.this, jsonStr, overwrite);
            runOnUiThread(() -> {
                pd.dismiss();
                loadSettings();
                showBackupResultDialog(
                        result.success ? "Restore Complete! ✅" : "Restore Failed ⚠️",
                        result.getResultMessage(),
                        result.success
                );
            });
        }).start();
    }

    private void showBackupResultDialog(String titleStr, String messageStr, boolean isSuccess) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(28), dp(24), dp(24));
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setColor(Color.WHITE);
        bgShape.setCornerRadius(dp(20));
        container.setBackground(bgShape);

        TextView iconView = new TextView(this);
        iconView.setText(isSuccess ? "🎉" : "⚠️");
        iconView.setTextSize(36);
        iconView.setGravity(Gravity.CENTER);
        container.addView(iconView);

        TextView title = new TextView(this);
        title.setText(titleStr);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, dp(12), 0, dp(8));
        title.setGravity(Gravity.CENTER);
        container.addView(title);

        TextView msg = new TextView(this);
        msg.setText(messageStr);
        msg.setTextSize(14);
        msg.setTextColor(Color.parseColor("#475569"));
        msg.setGravity(Gravity.CENTER);
        msg.setPadding(0, 0, 0, dp(20));
        container.addView(msg);

        Button btnOk = new Button(this);
        btnOk.setText("OK");
        btnOk.setTextColor(Color.WHITE);
        btnOk.setTextSize(14);
        btnOk.setAllCaps(false);
        btnOk.setTypeface(null, Typeface.BOLD);
        GradientDrawable okBg = new GradientDrawable();
        okBg.setColor(Color.parseColor(isSuccess ? "#16A34A" : "#EF4444"));
        okBg.setCornerRadius(dp(12));
        btnOk.setBackground(okBg);
        LinearLayout.LayoutParams okParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        btnOk.setLayoutParams(okParams);
        btnOk.setOnClickListener(v -> dialog.dismiss());
        container.addView(btnOk);

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
}

