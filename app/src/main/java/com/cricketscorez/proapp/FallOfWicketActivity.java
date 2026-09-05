package com.cricketscorez.proapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FallOfWicketActivity extends Activity {

    // Fall of Wicket keys
    public static final String WICKET_TYPE = "WICKET_TYPE";
    public static final String OUT_BATSMAN = "OUT_BATSMAN";
    public static final String HELPER_NAME = "HELPER_NAME";
    public static final String NEW_BATSMAN_NAME = "NEW_BATSMAN_NAME";
    public static final String COMPLETED_RUNS = "COMPLETED_RUNS";
    public static final String NEXT_STRIKER_IS_NEW = "NEXT_STRIKER_IS_NEW";

    private LinearLayout layoutExtraNotice;
    private TextView tvExtraNoticeIcon;
    private TextView tvExtraNoticeText;

    private Spinner spWicketType;
    private Spinner spWhoGotOut;
    private Spinner spNextStriker;
    private EditText etWhoHelped;
    private EditText etNewBatsman;
    private EditText etCompletedRuns;

    private Button btnRun0, btnRun1, btnRun2, btnRun3;
    private Button btnDone;

    private String strikerName = "Striker";
    private String nonStrikerName = "Non-Striker";
    private int completedRuns = 0;
    private boolean isNoBall = false;
    private boolean isWide = false;
    private boolean isBye = false;
    private boolean isLegBye = false;

    private List<String> availableWicketTypes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fall_of_wicket);

        // View initialization
        layoutExtraNotice = findViewById(R.id.layoutExtraNotice);
        tvExtraNoticeIcon = findViewById(R.id.tvExtraNoticeIcon);
        tvExtraNoticeText = findViewById(R.id.tvExtraNoticeText);

        spWicketType      = findViewById(R.id.spWicketType);
        spWhoGotOut       = findViewById(R.id.spWhoGotOut);
        spNextStriker     = findViewById(R.id.spNextStriker);
        etWhoHelped       = findViewById(R.id.etWhoHelped);
        etNewBatsman      = findViewById(R.id.etNewBatsman);
        etCompletedRuns   = findViewById(R.id.etCompletedRuns);

        btnRun0           = findViewById(R.id.btnRun0);
        btnRun1           = findViewById(R.id.btnRun1);
        btnRun2           = findViewById(R.id.btnRun2);
        btnRun3           = findViewById(R.id.btnRun3);
        btnDone           = findViewById(R.id.btnDoneWicket);

        // Retrieve data from MainActivity
        Intent intent = getIntent();
        if (intent != null) {
            String s = intent.getStringExtra("STRIKER_NAME");
            if (s != null && !s.isEmpty()) strikerName = s;
            String ns = intent.getStringExtra("NON_STRIKER_NAME");
            if (ns != null && !ns.isEmpty()) nonStrikerName = ns;

            completedRuns = intent.getIntExtra("RUNS_ENTERED", 0);
            isNoBall      = intent.getBooleanExtra("IS_NO_BALL", false);
            isWide        = intent.getBooleanExtra("IS_WIDE", false);
            isBye         = intent.getBooleanExtra("IS_BYE", false);
            isLegBye      = intent.getBooleanExtra("IS_LEG_BYE", false);
        }

        setupExtraNoticeAndWicketTypes();
        setupWhoGotOutSpinner();
        setupCompletedRunsUI();
        setupNextStrikerSpinner();

        // Listen for Wicket Type selection changes
        spWicketType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = availableWicketTypes.get(position);
                handleWicketTypeSelected(selectedType);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Listen for Who Got Out changes
        spWhoGotOut.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateNextStrikerOptions();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Done button logic
        btnDone.setOnClickListener(v -> {
            String outBatsmanOption = spWhoGotOut.getSelectedItem() != null ? spWhoGotOut.getSelectedItem().toString() : "";
            String outBatsmanName = outBatsmanOption.contains(" (") ? outBatsmanOption.substring(0, outBatsmanOption.indexOf(" (")).trim() : outBatsmanOption.trim();
            if (outBatsmanName.isEmpty()) outBatsmanName = strikerName;

            String newBatsman = etNewBatsman.getText().toString().trim();
            if (newBatsman.isEmpty()) {
                Toast.makeText(FallOfWicketActivity.this, "Please enter the incoming batsman's name.", Toast.LENGTH_SHORT).show();
                etNewBatsman.requestFocus();
                return;
            }

            int finalRuns = getEnteredRuns();
            String selectedType = spWicketType.getSelectedItem() != null ? spWicketType.getSelectedItem().toString() : "out";

            // Safety Validation: Extra vs Dismissal rules
            if (isNoBall && (selectedType.equalsIgnoreCase("Bowled") || selectedType.equalsIgnoreCase("Caught")
                    || selectedType.equalsIgnoreCase("LBW") || selectedType.equalsIgnoreCase("Stumped")
                    || selectedType.equalsIgnoreCase("Hit wicket"))) {
                Toast.makeText(FallOfWicketActivity.this, "Invalid dismissal! On a No-Ball, only Run Out is possible.", Toast.LENGTH_LONG).show();
                return;
            }
            if (isWide && (selectedType.equalsIgnoreCase("Bowled") || selectedType.equalsIgnoreCase("Caught")
                    || selectedType.equalsIgnoreCase("LBW"))) {
                Toast.makeText(FallOfWicketActivity.this, "Invalid dismissal! On a Wide ball, batsman cannot be Bowled/Caught/LBW.", Toast.LENGTH_LONG).show();
                return;
            }

            boolean nextStrikerIsNew = true;
            if (spNextStriker.getSelectedItemPosition() == 1) {
                nextStrikerIsNew = false;
            }

            // Return data to MainActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra(WICKET_TYPE, selectedType);
            resultIntent.putExtra(OUT_BATSMAN, outBatsmanName);
            resultIntent.putExtra(HELPER_NAME, etWhoHelped.getText().toString().trim());
            resultIntent.putExtra(NEW_BATSMAN_NAME, newBatsman);
            resultIntent.putExtra(COMPLETED_RUNS, finalRuns);
            resultIntent.putExtra(NEXT_STRIKER_IS_NEW, nextStrikerIsNew);

            setResult(RESULT_OK, resultIntent);
            finish();
        });

        // Back button
        ImageView btnBack = findViewById(R.id.btnBackWicket);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                setResult(RESULT_CANCELED);
                finish();
            });
        }
    }

    private void setupExtraNoticeAndWicketTypes() {
        availableWicketTypes.clear();

        if (isNoBall) {
            layoutExtraNotice.setVisibility(View.VISIBLE);
            tvExtraNoticeIcon.setText("⚠️");
            tvExtraNoticeText.setText("No-Ball Delivery: By cricket laws, only Run Out / Obstructing Field is allowed.");
            availableWicketTypes.add("Run out striker");
            availableWicketTypes.add("Run out non-striker");
            availableWicketTypes.add("Obstructing the field");
            availableWicketTypes.add("Handled the ball");
            availableWicketTypes.add("Hit the ball twice");
        } else if (isWide) {
            layoutExtraNotice.setVisibility(View.VISIBLE);
            tvExtraNoticeIcon.setText("⚠️");
            tvExtraNoticeText.setText("Wide Delivery: By cricket laws, only Stumped, Run Out, or Hit Wicket is allowed.");
            availableWicketTypes.add("Stumped");
            availableWicketTypes.add("Run out striker");
            availableWicketTypes.add("Run out non-striker");
            availableWicketTypes.add("Hit wicket");
            availableWicketTypes.add("Obstructing the field");
            availableWicketTypes.add("Handled the ball");
        } else if (isBye || isLegBye) {
            layoutExtraNotice.setVisibility(View.VISIBLE);
            tvExtraNoticeIcon.setText("ℹ️");
            tvExtraNoticeText.setText((isBye ? "Bye" : "Leg-Bye") + " Delivery: Ball bypassed bat without direct hit.");
            availableWicketTypes.add("Run out striker");
            availableWicketTypes.add("Run out non-striker");
            availableWicketTypes.add("Hit wicket");
            availableWicketTypes.add("Obstructing the field");
            availableWicketTypes.add("Handled the ball");
        } else {
            layoutExtraNotice.setVisibility(View.GONE);
            availableWicketTypes.add("Caught");
            availableWicketTypes.add("Bowled");
            availableWicketTypes.add("LBW");
            availableWicketTypes.add("Run out striker");
            availableWicketTypes.add("Run out non-striker");
            availableWicketTypes.add("Stumped");
            availableWicketTypes.add("Hit wicket");
            availableWicketTypes.add("Handled the ball");
            availableWicketTypes.add("Obstructing the field");
            availableWicketTypes.add("Timed out");
        }

        ArrayAdapter<String> adapterWicketType = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, availableWicketTypes);
        adapterWicketType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWicketType.setAdapter(adapterWicketType);
    }

    private void setupWhoGotOutSpinner() {
        String[] batsmanOptions = new String[]{strikerName + " (Striker)", nonStrikerName + " (Non-Striker)"};
        ArrayAdapter<String> adapterWhoOut = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, batsmanOptions);
        adapterWhoOut.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWhoGotOut.setAdapter(adapterWhoOut);
    }

    private void setupCompletedRunsUI() {
        etCompletedRuns.setText(String.valueOf(completedRuns));
        highlightRunButton(completedRuns);

        btnRun0.setOnClickListener(v -> setRuns(0));
        btnRun1.setOnClickListener(v -> setRuns(1));
        btnRun2.setOnClickListener(v -> setRuns(2));
        btnRun3.setOnClickListener(v -> setRuns(3));

        etCompletedRuns.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                int r = getEnteredRuns();
                highlightRunButton(r);
                updateNextStrikerOptions();
            }
        });
    }

    private void setRuns(int r) {
        completedRuns = r;
        etCompletedRuns.setText(String.valueOf(r));
        highlightRunButton(r);
        updateNextStrikerOptions();
    }

    private int getEnteredRuns() {
        try {
            String txt = etCompletedRuns.getText().toString().trim();
            if (txt.isEmpty()) return 0;
            int val = Integer.parseInt(txt);
            return Math.max(0, val);
        } catch (Exception e) {
            return 0;
        }
    }

    private void highlightRunButton(int runs) {
        Button[] btns = new Button[]{btnRun0, btnRun1, btnRun2, btnRun3};
        for (int i = 0; i < btns.length; i++) {
            if (i == runs) {
                btns[i].setBackgroundResource(R.drawable.bg_green_rect);
                btns[i].setTextColor(Color.WHITE);
            } else {
                btns[i].setBackgroundResource(R.drawable.bg_card);
                btns[i].setTextColor(Color.parseColor("#334155"));
            }
        }
    }

    private void handleWicketTypeSelected(String type) {
        boolean isRunOut = type.toLowerCase().contains("run out");
        if (type.equalsIgnoreCase("Run out non-striker")) {
            spWhoGotOut.setSelection(1); // Non-Striker
        } else if (type.equalsIgnoreCase("Run out striker") || type.equalsIgnoreCase("Caught")
                || type.equalsIgnoreCase("Bowled") || type.equalsIgnoreCase("LBW")
                || type.equalsIgnoreCase("Stumped") || type.equalsIgnoreCase("Hit wicket")) {
            spWhoGotOut.setSelection(0); // Striker
        }

        // If not a run out and user didn't enter runs, reset completed runs to 0
        if (!isRunOut && completedRuns == 0) {
            setRuns(0);
        }

        updateNextStrikerOptions();
    }

    private void setupNextStrikerSpinner() {
        updateNextStrikerOptions();
    }

    private void updateNextStrikerOptions() {
        boolean isStrikerOut = spWhoGotOut.getSelectedItemPosition() == 0;
        String survivingName = isStrikerOut ? nonStrikerName : strikerName;
        int runs = getEnteredRuns();

        List<String> options = new ArrayList<>();
        options.add("Incoming Batsman (New)");
        options.add("Surviving Batsman (" + survivingName + ")");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spNextStriker.setAdapter(adapter);

        // Smart default selection based on runs crossed and who was dismissed:
        // - If Striker out: 0 or even completed runs -> New batsman takes strike (pos 0)
        //                   1 or odd completed runs -> Batsmen crossed, surviving non-striker takes strike (pos 1)
        // - If Non-Striker out: 0 or even completed runs -> Surviving striker stays on strike (pos 1)
        //                       1 or odd completed runs -> Batsmen crossed, incoming batsman at striker end (pos 0)
        if (isStrikerOut) {
            if (runs % 2 == 1) {
                spNextStriker.setSelection(1); // Surviving batsman on strike
            } else {
                spNextStriker.setSelection(0); // New batsman on strike
            }
        } else {
            if (runs % 2 == 1) {
                spNextStriker.setSelection(0); // New batsman on strike
            } else {
                spNextStriker.setSelection(1); // Surviving striker stays on strike
            }
        }
    }
}
