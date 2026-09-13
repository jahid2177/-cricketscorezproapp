package com.cricketscorez.proapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;

public class ChooseBowlerActivity extends Activity {

    EditText etName;
    Button btnDone;
    TextView tvTitle, tvSuggestionLabel;
    ListView listSuggestions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        layout.setGravity(Gravity.CENTER_HORIZONTAL); 
        layout.setBackgroundColor(Color.parseColor("#F5F5F5"));

        tvTitle = new TextView(this);
        tvTitle.setTextSize(22);
        tvTitle.setTextColor(Color.parseColor("#00796B"));
        tvTitle.setPadding(0, 20, 0, 40);
        tvTitle.setGravity(Gravity.CENTER);
        layout.addView(tvTitle);

        etName = new EditText(this);
        etName.setTextColor(Color.BLACK);
        etName.setHintTextColor(Color.GRAY);
        etName.setBackgroundColor(Color.WHITE);
        etName.setPadding(20, 20, 20, 20);
        layout.addView(etName);

        btnDone = new Button(this);
        btnDone.setBackgroundColor(Color.parseColor("#4CAF50"));
        btnDone.setTextColor(Color.WHITE);
        btnDone.setTextSize(16);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 30, 0, 0);
        btnDone.setLayoutParams(btnParams);
        layout.addView(btnDone);

        tvSuggestionLabel = new TextView(this);
        tvSuggestionLabel.setText("\nChoose from Previous Bowlers:");
        tvSuggestionLabel.setTextSize(14);
        tvSuggestionLabel.setTextColor(Color.DKGRAY);
        tvSuggestionLabel.setPadding(0, 40, 0, 10);
        layout.addView(tvSuggestionLabel);

        listSuggestions = new ListView(this);
        layout.addView(listSuggestions);

        setContentView(layout);

        boolean isBatsman = getIntent().getBooleanExtra("IS_BATSMAN_OUT", false);
        final ArrayList<String> suggestions = getIntent().getStringArrayListExtra("SUGGESTIONS");
        final String lastOverBowler = getIntent().getStringExtra("LAST_OVER_BOWLER");

        if (isBatsman) {
            tvTitle.setText("Select New Batsman");
            etName.setHint("Enter Batsman Name");
            btnDone.setText("Add Batsman");
            tvSuggestionLabel.setVisibility(View.GONE);
            listSuggestions.setVisibility(View.GONE);
        } else {
            tvTitle.setText("Select New Bowler");
            etName.setHint("Enter Bowler Name");
            btnDone.setText("Start Over");

            if (suggestions != null && !suggestions.isEmpty()) {
                tvSuggestionLabel.setVisibility(View.VISIBLE);
                listSuggestions.setVisibility(View.VISIBLE);

                // Highlight/tag the bowler who bowled the last over
                ArrayList<String> displayList = new ArrayList<>();
                for (String b : suggestions) {
                    String baseName = b.contains("(") ? b.substring(0, b.indexOf("(")).trim() : b.trim();
                    if (lastOverBowler != null && baseName.equalsIgnoreCase(lastOverBowler.trim())) {
                        displayList.add(b + "  ⚠️ (Bowled Last Over)");
                    } else {
                        displayList.add(b);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
                listSuggestions.setAdapter(adapter);

                listSuggestions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String selected = displayList.get(position);
                        if (selected.contains("(")) {
                            selected = selected.substring(0, selected.indexOf("(")).trim();
                        }
                        etName.setText(selected);
                        etName.setSelection(etName.getText().length());
                    }
                });
            } else {
                tvSuggestionLabel.setVisibility(View.GONE);
                listSuggestions.setVisibility(View.GONE);
            }
        }

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = etName.getText().toString().trim();
                if (name.isEmpty()) {
                    etName.setError("Please enter a name");
                    return;
                }

                if (!isBatsman && lastOverBowler != null && !lastOverBowler.isEmpty()
                        && !lastOverBowler.equalsIgnoreCase("Bowler")
                        && name.equalsIgnoreCase(lastOverBowler.trim())) {
                    // ⚠️ Consecutive over warning dialog
                    new android.app.AlertDialog.Builder(ChooseBowlerActivity.this)
                            .setTitle("⚠️ Consecutive Over Restriction")
                            .setMessage("'" + name + "' bowled the previous over.\n\nAccording to ICC Law 17.5, a bowler cannot bowl two consecutive overs from either end.\n\nDo you want to override and allow this bowler anyway?")
                            .setPositiveButton("Allow (Override)", (d, w) -> {
                                confirmAndFinish(name);
                            })
                            .setNegativeButton("Choose Another", (d, w) -> {
                                etName.requestFocus();
                                etName.selectAll();
                            })
                            .show();
                } else {
                    confirmAndFinish(name);
                }
            }
        });
    }

    private void confirmAndFinish(String name) {
        Intent intent = new Intent();
        intent.putExtra("NEW_BOWLER_NAME", name);
        intent.putExtra("NEW_BATSMAN_NAME", name);
        setResult(RESULT_OK, intent);
        finish();
    }
}

