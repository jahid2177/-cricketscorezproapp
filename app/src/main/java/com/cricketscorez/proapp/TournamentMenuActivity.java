package com.cricketscorez.proapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class TournamentMenuActivity extends Activity {

    // ভেরিয়েবল ঘোষণা
    ImageView btnBack, ivMenuTournamentLogo; 
    TextView tvMenuTournamentName, tvMenuTournamentDetails;
    LinearLayout btnFixtures, btnPointTable, btnResult, btnRanking, btnSettings, cardTournamentProfile;
    android.widget.FrameLayout layoutTournamentLogoContainer;

    private static final int REQUEST_LOGO_DIRECT = 301;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_menu);

        // ভিউ আইডি খুঁজে বের করা
        btnBack = findViewById(R.id.btnBack);
        ivMenuTournamentLogo = findViewById(R.id.ivMenuTournamentLogo);
        tvMenuTournamentName = findViewById(R.id.tvMenuTournamentName);
        tvMenuTournamentDetails = findViewById(R.id.tvMenuTournamentDetails);
        cardTournamentProfile = findViewById(R.id.cardTournamentProfile);
        layoutTournamentLogoContainer = findViewById(R.id.layoutTournamentLogoContainer);

        btnFixtures = findViewById(R.id.btnFixtures);
        btnPointTable = findViewById(R.id.btnPointTable);
        btnResult = findViewById(R.id.btnResult);
        btnRanking = findViewById(R.id.btnRanking);
        btnSettings = findViewById(R.id.btnSettings);

        loadTournamentHeader();

        if (cardTournamentProfile != null) {
            cardTournamentProfile.setOnClickListener(v -> startActivity(new Intent(TournamentMenuActivity.this, TournamentSettingsActivity.class)));
        }

        if (layoutTournamentLogoContainer != null) {
            layoutTournamentLogoContainer.setOnClickListener(v -> pickTournamentLogo());
        }

        // ১. ব্যাক বাটনের কাজ
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // ২. Fixtures পেজে যাওয়া
        btnFixtures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TournamentMenuActivity.this, FixturesActivity.class));
            }
        });

        // ৩. Point Table পেজে যাওয়া
        btnPointTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TournamentMenuActivity.this, PointTableActivity.class));
            }
        });

        // ৪. Tournament Result পেজে যাওয়া
        btnResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TournamentMenuActivity.this, TournamentResultActivity.class));
            }
        });

        // ৫. Ranking পেজে যাওয়া
        btnRanking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TournamentMenuActivity.this, RankingActivity.class));
            }
        });

        // ৬. Settings পেজে যাওয়া
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TournamentMenuActivity.this, TournamentSettingsActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTournamentHeader();
    }

    private void loadTournamentHeader() {
        android.content.SharedPreferences prefs = getSharedPreferences("TournamentData", MODE_PRIVATE);
        String name = prefs.getString("TOURNAMENT_NAME", "🏆 Tournament Zone");
        String overs = prefs.getString("TOTAL_OVERS", "20");
        String winPts = prefs.getString("WIN_POINTS", "2");

        if (name == null || name.trim().isEmpty()) {
            name = "Tournament Zone";
        }

        if (tvMenuTournamentName != null) {
            tvMenuTournamentName.setText(name);
        }

        if (tvMenuTournamentDetails != null) {
            tvMenuTournamentDetails.setText("Format: " + overs + " Overs | Win: " + winPts + " pts");
        }

        if (ivMenuTournamentLogo != null) {
            ImageStorageHelper.loadTournamentLogoInto(this, ivMenuTournamentLogo, android.R.drawable.ic_menu_gallery);
        }
    }

    private void pickTournamentLogo() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Tournament Logo"), REQUEST_LOGO_DIRECT);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_LOGO_DIRECT);
            } catch (Exception ex) {
                android.widget.Toast.makeText(this, "Could not open gallery", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOGO_DIRECT && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri selectedUri = data.getData();
            String savedPath = ImageStorageHelper.saveTournamentLogo(this, selectedUri);
            if (savedPath != null) {
                loadTournamentHeader();
                android.widget.Toast.makeText(this, "✅ Tournament logo updated!", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(this, "Failed to save logo", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }
}
