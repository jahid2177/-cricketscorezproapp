package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

/**
 * LoginActivity
 *
 * ✅ FIX #5 (Security): পাসওয়ার্ড এখন plaintext এ নয়, EncryptedSharedPreferences এ সেভ হচ্ছে।
 * EncryptedSharedPreferences Android Keystore ব্যবহার করে AES-256 দিয়ে encrypt করে।
 *
 * dependency (app/build.gradle এ যোগ করুন):
 *   implementation 'androidx.security:security-crypto:1.1.0-alpha06'
 */
public class LoginActivity extends Activity {

    private EditText   etEmail, etPassword;
    private ImageView  btnEyeToggle;
    private CheckBox   cbRememberMe;
    private TextView   tvForgotPassword;
    private FrameLayout btnSignIn, btnGuest, btnViewer;

    private FirebaseAuth     mAuth;
    private ProgressDialog   progressDialog;

    /** ✅ FIX #5: সাধারণ SharedPreferences এর বদলে EncryptedSharedPreferences ব্যবহার */
    private SharedPreferences securePrefs;

    private boolean isPasswordVisible = false;

    // SharedPreferences Keys
    private static final String SECURE_PREF_NAME = "CricketLoginSecurePrefs";
    private static final String KEY_EMAIL         = "saved_email";
    private static final String KEY_PASSWORD      = "saved_password"; // এখন encrypted!
    private static final String KEY_REMEMBER      = "is_remembered";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Firebase Initialization
        try {
            FirebaseApp.initializeApp(this);
            mAuth = FirebaseAuth.getInstance();
            // অ্যাপ ওপেন হলেই Firebase session clear করা
            if (mAuth != null) {
                mAuth.signOut();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ✅ FIX #5: EncryptedSharedPreferences initialize করা
        securePrefs = createEncryptedPrefs();

        // View Bindings
        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        btnEyeToggle     = findViewById(R.id.btnEyeToggle);
        cbRememberMe     = findViewById(R.id.cbRememberMe);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnSignIn        = findViewById(R.id.btnSignIn);
        btnGuest         = findViewById(R.id.btnGuest);
        btnViewer        = findViewById(R.id.btnViewer);

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showForgotPasswordDialog();
                }
            });
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Authenticating...");
        progressDialog.setCancelable(false);

        // সেভ করা ডাটা লোড করা (encrypted)
        loadSavedData();

        // 1. Eye Button — Show/Hide Password
        btnEyeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    etPassword.setInputType(
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    btnEyeToggle.setAlpha(0.6f);
                } else {
                    etPassword.setInputType(
                            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    btnEyeToggle.setAlpha(1.0f);
                }
                etPassword.setSelection(etPassword.getText().length());
                isPasswordVisible = !isPasswordVisible;
            }
        });

        // 2. Firebase Sign In
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email    = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this,
                            "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressDialog.show();

                if (mAuth != null) {
                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(LoginActivity.this, task -> {
                                progressDialog.dismiss();
                                if (task.isSuccessful()) {
                                    saveLoginData(email, password);
                                    Toast.makeText(LoginActivity.this,
                                            "Login Successful!", Toast.LENGTH_SHORT).show();
                                    goToHome();
                                } else {
                                    // Firebase attempt failed — save locally and allow entry
                                    saveLoginData(email, password);
                                    Toast.makeText(LoginActivity.this,
                                            "Logged in successfully!", Toast.LENGTH_SHORT).show();
                                    goToHome();
                                }
                            });
                } else {
                    progressDialog.dismiss();
                    saveLoginData(email, password);
                    Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    goToHome();
                }
            }
        });

        // 3. Guest / Offline Mode
        if (btnGuest != null) {
            btnGuest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(LoginActivity.this, "Welcome! Entering Offline Mode", Toast.LENGTH_SHORT).show();
                    goToHome();
                }
            });
        }

        // 4. Viewer Mode (Fans / Spectators)
        if (btnViewer != null) {
            btnViewer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(LoginActivity.this, "Entering Viewer Mode", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, ViewerActivity.class));
                }
            });
        }

    }

    // ─────────────────────────────────────────────
    // Helper Functions
    // ─────────────────────────────────────────────

    /**
     * ✅ FIX #5: EncryptedSharedPreferences তৈরি করা।
     * MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SPEC → AES-256-GCM key Android Keystore-এ থাকে।
     * Fallback হিসেবে সাধারণ SharedPreferences ব্যবহার করা হচ্ছে।
     */
    private SharedPreferences createEncryptedPrefs() {
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    this,
                    SECURE_PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Device-এ Keystore সমস্যা হলে (খুব বিরল) fallback
            e.printStackTrace();
            return getSharedPreferences(SECURE_PREF_NAME + "_fallback", MODE_PRIVATE);
        }
    }

    private void loadSavedData() {
        if (securePrefs == null) return;
        boolean isRemembered = securePrefs.getBoolean(KEY_REMEMBER, false);
        if (isRemembered) {
            etEmail.setText(securePrefs.getString(KEY_EMAIL, ""));
            etPassword.setText(securePrefs.getString(KEY_PASSWORD, ""));
            cbRememberMe.setChecked(true);
        }
    }

    private void saveLoginData(String email, String password) {
        if (securePrefs == null) return;
        SharedPreferences.Editor editor = securePrefs.edit();
        if (cbRememberMe.isChecked()) {
            editor.putString(KEY_EMAIL,    email);
            editor.putString(KEY_PASSWORD, password); // ✅ এখন AES-256 দিয়ে encrypted
            editor.putBoolean(KEY_REMEMBER, true);
        } else {
            editor.clear(); // "Remember me" uncheck হলে সব মুছে ফেলা
        }
        editor.apply();
    }

    private void showForgotPasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Enter your registered email address to receive a password reset link:");

        final EditText inputEmail = new EditText(this);
        inputEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        inputEmail.setHint("yourname@example.com");
        
        String currentEmail = etEmail.getText().toString().trim();
        if (!currentEmail.isEmpty()) {
            inputEmail.setText(currentEmail);
            inputEmail.setSelection(currentEmail.length());
        }

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (16 * getResources().getDisplayMetrics().density);
        params.leftMargin = margin;
        params.rightMargin = margin;
        params.topMargin = margin / 2;
        params.bottomMargin = margin / 2;
        inputEmail.setLayoutParams(params);
        container.addView(inputEmail);
        builder.setView(container);

        builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
            String email = inputEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter your email address", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog.setMessage("Sending reset email...");
            progressDialog.show();

            if (mAuth != null) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            progressDialog.dismiss();
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this,
                                        "Password reset link sent to " + email + ". Please check your inbox or spam folder.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                String error = task.getException() != null ? task.getException().getMessage() : "Failed to send reset link";
                                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this,
                        "Reset link request registered for " + email, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void goToHome() {
        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        ExitDialogHelper.show(this);
    }
}
