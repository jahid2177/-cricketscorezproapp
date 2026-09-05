package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class MatchHistoryActivity extends Activity {

    ListView listHistory;
    ArrayList<MatchData> matchList;
    HistoryAdapter adapter;
    TextView tvEmpty;
    TextView tvSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F0F4F8"));

        // ─── Header ───────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#1B5E20"));
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView btnBack = new TextView(this);
        btnBack.setText("\u2190");
        btnBack.setTextColor(Color.WHITE);
        btnBack.setTextSize(22);
        btnBack.setPadding(0, 0, dp(12), 0);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        header.addView(btnBack);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Match History");
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvTitle.setLayoutParams(titleLp);
        header.addView(tvTitle);

        // Clear All button in header
        TextView btnClearAll = new TextView(this);
        btnClearAll.setText("🗑 Clear All");
        btnClearAll.setTextColor(Color.parseColor("#FFCDD2"));
        btnClearAll.setTextSize(13);
        btnClearAll.setTypeface(null, Typeface.BOLD);
        btnClearAll.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable clearBtnBg = new GradientDrawable();
        clearBtnBg.setColor(Color.parseColor("#B71C1C"));
        clearBtnBg.setCornerRadius(dp(10));
        btnClearAll.setBackground(clearBtnBg);
        btnClearAll.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showPremiumClearAllDialog();
            }
        });
        header.addView(btnClearAll);

        root.addView(header, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ─── Summary strip ────────────────────────────────────
        LinearLayout strip = new LinearLayout(this);
        strip.setBackgroundColor(Color.parseColor("#2E7D32"));
        strip.setPadding(dp(16), dp(8), dp(16), dp(10));
        tvSummary = new TextView(this);
        tvSummary.setTextColor(Color.parseColor("#A5D6A7"));
        tvSummary.setTextSize(12);
        strip.addView(tvSummary);
        root.addView(strip, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ─── Empty state ──────────────────────────────────────
        tvEmpty = new TextView(this);
        tvEmpty.setText("No matches recorded yet.\nStart a new match to see history here.");
        tvEmpty.setTextColor(Color.parseColor("#90A4AE"));
        tvEmpty.setTextSize(15);
        tvEmpty.setGravity(Gravity.CENTER);
        tvEmpty.setPadding(dp(32), dp(80), dp(32), dp(80));
        tvEmpty.setVisibility(View.GONE);
        root.addView(tvEmpty, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ─── List ─────────────────────────────────────────────
        listHistory = new ListView(this);
        listHistory.setDivider(null);
        listHistory.setDividerHeight(0);
        listHistory.setBackgroundColor(Color.TRANSPARENT);
        listHistory.setPadding(dp(12), dp(10), dp(12), dp(12));
        listHistory.setClipToPadding(false);
        root.addView(listHistory, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        loadHistory();
    }

    private void loadHistory() {
        matchList = DataManager.getAllMatches(this);
        if (matchList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            listHistory.setVisibility(View.GONE);
            tvSummary.setText("0 matches");
        } else {
            tvEmpty.setVisibility(View.GONE);
            listHistory.setVisibility(View.VISIBLE);
            int completed = 0, inProgress = 0;
            for (MatchData m : matchList) { if (isCompleted(m)) completed++; else inProgress++; }
            tvSummary.setText(matchList.size() + " matches  \u2022  " + completed + " completed  \u2022  " + inProgress + " in progress");
            adapter = new HistoryAdapter(this, matchList);
            listHistory.setAdapter(adapter);
        }
    }

    private boolean isCompleted(MatchData m) {
        if (m == null || m.matchStatus == null || m.matchStatus.trim().isEmpty()) return false;
        String s = m.matchStatus.trim().toLowerCase();
        if (s.equals("incomplete") || s.equals("in progress") || s.equals("live") || s.startsWith("incomplete")) {
            return false;
        }
        return s.equalsIgnoreCase("Completed")
                || s.contains("won")
                || s.contains("tied")
                || s.contains("win");
    }

    private void showPremiumClearAllDialog() {
        if (matchList == null || matchList.isEmpty()) {
            Toast.makeText(this, "No match history to clear.", Toast.LENGTH_SHORT).show();
            return;
        }

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
        title.setText("Clear All History?");
        title.setTextSize(20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1E293B"));
        title.setPadding(0, dp(16), 0, dp(8));
        title.setGravity(Gravity.CENTER);
        container.addView(title);

        // Match count info
        TextView countInfo = new TextView(this);
        countInfo.setText(matchList.size() + " match" + (matchList.size() > 1 ? "es" : "") + " will be permanently deleted.");
        countInfo.setTextSize(13);
        countInfo.setTypeface(null, Typeface.BOLD);
        countInfo.setTextColor(Color.parseColor("#EF4444"));
        countInfo.setGravity(Gravity.CENTER);
        countInfo.setPadding(0, 0, 0, dp(6));
        container.addView(countInfo);

        // Message
        TextView message = new TextView(this);
        message.setText("This will remove your entire match history. This action cannot be undone.");
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

        // Clear All button
        Button btnClear = new Button(this);
        btnClear.setText("Clear All");
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
            DataManager.clearAllMatches(this);
            matchList.clear();
            if (adapter != null) adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(View.VISIBLE);
            listHistory.setVisibility(View.GONE);
            tvSummary.setText("0 matches");
            Toast.makeText(this, "Match history cleared!", Toast.LENGTH_SHORT).show();
        });

        btnLayout.addView(btnCancel);
        btnLayout.addView(btnClear);
        container.addView(btnLayout);

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.85),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private int dp(int val) { return Math.round(val * getResources().getDisplayMetrics().density); }

    private GradientDrawable pill(int color, int cornerDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(cornerDp));
        return d;
    }

    // ─── Adapter ──────────────────────────────────────────────────────────────
    public class HistoryAdapter extends BaseAdapter {
        Context ctx;
        ArrayList<MatchData> list;

        public HistoryAdapter(Context ctx, ArrayList<MatchData> list) { this.ctx = ctx; this.list = list; }
        @Override public int getCount() { return list.size(); }
        @Override public Object getItem(int pos) { return list.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final MatchData match = list.get(position);
            final boolean completed = isCompleted(match);

            // ── Card ──────────────────────────────────────────
            LinearLayout card = new LinearLayout(ctx);
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, 0, 0, dp(10));
            card.setLayoutParams(cardLp);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(Color.WHITE);
            cardBg.setCornerRadius(dp(14));
            card.setBackground(cardBg);
            card.setElevation(dp(2));

            // ── Row 1: Date + Badge ───────────────────────────
            LinearLayout row1 = new LinearLayout(ctx);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvDate = new TextView(ctx);
            tvDate.setText(match.matchDate != null ? "\uD83D\uDCC5  " + match.matchDate : "\uD83D\uDCC5  Unknown date");
            tvDate.setTextSize(11);
            tvDate.setTextColor(Color.parseColor("#78909C"));
            LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tvDate.setLayoutParams(dateLp);
            row1.addView(tvDate);

            // Tournament badge
            if (match.isTournamentMatch) {
                TextView tourBadge = new TextView(ctx);
                tourBadge.setText("\uD83C\uDFC6 Tournament");
                tourBadge.setTextSize(10);
                tourBadge.setTypeface(null, Typeface.BOLD);
                tourBadge.setTextColor(Color.WHITE);
                tourBadge.setBackground(pill(Color.parseColor("#1565C0"), 10));
                tourBadge.setPadding(dp(7), dp(3), dp(7), dp(3));
                LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tbLp.setMargins(0, 0, dp(6), 0);
                tourBadge.setLayoutParams(tbLp);
                row1.addView(tourBadge);
            }

            // Status badge
            TextView tvBadge = new TextView(ctx);
            if (completed) {
                tvBadge.setText("\u2714 Completed");
                tvBadge.setTextColor(Color.WHITE);
                tvBadge.setBackground(pill(Color.parseColor("#2E7D32"), 10));
            } else {
                tvBadge.setText("\u25CF In Progress");
                tvBadge.setTextColor(Color.WHITE);
                tvBadge.setBackground(pill(Color.parseColor("#BF360C"), 10));
            }
            tvBadge.setTextSize(10);
            tvBadge.setTypeface(null, Typeface.BOLD);
            tvBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
            row1.addView(tvBadge);
            card.addView(row1);

            // ── Thin divider ──────────────────────────────────
            card.addView(makeDivider(dp(6), dp(8)));

            // ── Row 2: Teams & Scores ─────────────────────────
            LinearLayout teamsRow = new LinearLayout(ctx);
            teamsRow.setOrientation(LinearLayout.HORIZONTAL);
            teamsRow.setGravity(Gravity.CENTER_VERTICAL);

            // Team 1 block
            LinearLayout t1 = new LinearLayout(ctx);
            t1.setOrientation(LinearLayout.VERTICAL);
            t1.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout t1nr = new LinearLayout(ctx);
            t1nr.setOrientation(LinearLayout.HORIZONTAL);
            t1nr.setGravity(Gravity.CENTER_VERTICAL);
            TextView t1ic = makeCircleIcon(match.team1Name, Color.parseColor("#1B5E20"));
            LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(dp(28), dp(28));
            icLp.setMargins(0, 0, dp(6), 0);
            t1ic.setLayoutParams(icLp);
            t1nr.addView(t1ic);
            TextView tvT1n = new TextView(ctx);
            tvT1n.setText(match.team1Name != null ? match.team1Name : "Team 1");
            tvT1n.setTextSize(13);
            tvT1n.setTypeface(null, Typeface.BOLD);
            tvT1n.setTextColor(Color.parseColor("#1A237E"));
            tvT1n.setMaxLines(1);
            t1nr.addView(tvT1n);
            t1.addView(t1nr);

            String t1Score = match.isSecondInnings
                    ? match.scoreInn1 + "  (" + match.oversInn1 + ")"
                    : match.totalRuns + "/" + match.totalWickets + "  (" + match.currentOvers + "." + match.currentBalls + ")";
            TextView tvT1s = new TextView(ctx);
            tvT1s.setText(t1Score);
            tvT1s.setTextSize(17);
            tvT1s.setTypeface(null, Typeface.BOLD);
            tvT1s.setTextColor(Color.parseColor("#1B5E20"));
            LinearLayout.LayoutParams s1Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            s1Lp.setMargins(dp(34), dp(2), 0, 0);
            tvT1s.setLayoutParams(s1Lp);
            t1.addView(tvT1s);
            teamsRow.addView(t1);

            TextView tvVs = new TextView(ctx);
            tvVs.setText("vs");
            tvVs.setTextSize(11);
            tvVs.setTypeface(null, Typeface.BOLD);
            tvVs.setTextColor(Color.parseColor("#90A4AE"));
            LinearLayout.LayoutParams vsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            vsLp.setMargins(dp(8), 0, dp(8), 0);
            tvVs.setLayoutParams(vsLp);
            teamsRow.addView(tvVs);

            // Team 2 block
            LinearLayout t2 = new LinearLayout(ctx);
            t2.setOrientation(LinearLayout.VERTICAL);
            t2.setGravity(Gravity.END);
            t2.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout t2nr = new LinearLayout(ctx);
            t2nr.setOrientation(LinearLayout.HORIZONTAL);
            t2nr.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            TextView tvT2n = new TextView(ctx);
            tvT2n.setText(match.team2Name != null ? match.team2Name : "Team 2");
            tvT2n.setTextSize(13);
            tvT2n.setTypeface(null, Typeface.BOLD);
            tvT2n.setTextColor(Color.parseColor("#1A237E"));
            tvT2n.setMaxLines(1);
            LinearLayout.LayoutParams t2nLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            t2nLp.setMargins(0, 0, dp(6), 0);
            tvT2n.setLayoutParams(t2nLp);
            t2nr.addView(tvT2n);
            TextView t2ic = makeCircleIcon(match.team2Name, Color.parseColor("#1565C0"));
            t2ic.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(28)));
            t2nr.addView(t2ic);
            t2.addView(t2nr);

            String t2Score = match.isSecondInnings
                    ? match.totalRuns + "/" + match.totalWickets + "  (" + match.currentOvers + "." + match.currentBalls + ")"
                    : "Yet to bat";
            TextView tvT2s = new TextView(ctx);
            tvT2s.setText(t2Score);
            tvT2s.setTextSize(17);
            tvT2s.setTypeface(null, Typeface.BOLD);
            tvT2s.setTextColor(Color.parseColor("#1565C0"));
            tvT2s.setGravity(Gravity.END);
            LinearLayout.LayoutParams s2Lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            s2Lp.setMargins(0, dp(2), dp(34), 0);
            tvT2s.setLayoutParams(s2Lp);
            t2.addView(tvT2s);
            teamsRow.addView(t2);

            card.addView(teamsRow);

            // ── Result banner ────────────────────────────────
            if (completed && match.matchStatus != null && !match.matchStatus.isEmpty()) {
                TextView tvResult = new TextView(ctx);
                tvResult.setText("\uD83C\uDFC6  " + match.matchStatus);
                tvResult.setTextSize(12);
                tvResult.setTypeface(null, Typeface.BOLD);
                tvResult.setTextColor(Color.parseColor("#1B5E20"));
                tvResult.setGravity(Gravity.CENTER);
                GradientDrawable rb = new GradientDrawable();
                rb.setColor(Color.parseColor("#E8F5E9"));
                rb.setCornerRadius(dp(8));
                tvResult.setBackground(rb);
                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rLp.setMargins(0, dp(8), 0, dp(2));
                tvResult.setLayoutParams(rLp);
                tvResult.setPadding(dp(10), dp(7), dp(10), dp(7));
                card.addView(tvResult);
            } else if (!completed) {
                TextView tvResult = new TextView(ctx);
                tvResult.setText("⏳  Match Incomplete — In Progress (Over " + match.currentOvers + "." + match.currentBalls + ")");
                tvResult.setTextSize(12);
                tvResult.setTypeface(null, Typeface.BOLD);
                tvResult.setTextColor(Color.parseColor("#BF360C"));
                tvResult.setGravity(Gravity.CENTER);
                GradientDrawable rb = new GradientDrawable();
                rb.setColor(Color.parseColor("#FBE9E7"));
                rb.setCornerRadius(dp(8));
                tvResult.setBackground(rb);
                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rLp.setMargins(0, dp(8), 0, dp(2));
                tvResult.setLayoutParams(rLp);
                tvResult.setPadding(dp(10), dp(7), dp(10), dp(7));
                card.addView(tvResult);
            }

            // ── Thin divider ──────────────────────────────────
            card.addView(makeDivider(dp(8), dp(6)));

            // ── Action buttons row ────────────────────────────
            LinearLayout actRow = new LinearLayout(ctx);
            actRow.setOrientation(LinearLayout.HORIZONTAL);
            actRow.setGravity(Gravity.CENTER_VERTICAL);

            // Scorecard button
            Button btnSc = makeActionBtn("\uD83D\uDCCB  Scorecard",
                    Color.parseColor("#1B5E20"), Color.parseColor("#E8F5E9"), Color.parseColor("#A5D6A7"));
            LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            scLp.setMargins(0, 0, dp(8), 0);
            btnSc.setLayoutParams(scLp);
            btnSc.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Intent i = new Intent(ctx, ScorecardActivity.class);
                    i.putExtra("MATCH_DATA", match);
                    ctx.startActivity(i);
                }
            });
            actRow.addView(btnSc);

            // Resume button — ONLY for incomplete matches
            if (!completed) {
                Button btnRes = makeActionBtn("\u25B6  Resume",
                        Color.WHITE, Color.parseColor("#BF360C"), Color.parseColor("#BF360C"));
                btnRes.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                btnRes.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Intent i = new Intent(ctx, MainActivity.class);
                        i.putExtra("RESUME_MATCH_DATA", match);
                        ctx.startActivity(i);
                        ((Activity) ctx).finish();
                    }
                });
                actRow.addView(btnRes);
            }

            // Spacer
            View spacer = new View(ctx);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
            actRow.addView(spacer);

            // Delete button (Premium Dialog Trigger)
            Button btnDel = makeActionBtn("\uD83D\uDDD1",
                    Color.parseColor("#B71C1C"), Color.parseColor("#FFEBEE"), Color.parseColor("#FFCDD2"));
            btnDel.setLayoutParams(new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));
            btnDel.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showPremiumDeleteDialog(ctx, match, position);
                }
            });
            actRow.addView(btnDel);

            card.addView(actRow);
            return card;
        }
        
        // ─── Premium Delete Dialog Method ─────────────────────────────────────────
        private void showPremiumDeleteDialog(final Context ctx, final MatchData match, final int position) {
            final Dialog dialog = new Dialog(ctx);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            
            // Main Container (Rounded White Background)
            LinearLayout container = new LinearLayout(ctx);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dp(24), dp(32), dp(24), dp(24));
            container.setGravity(Gravity.CENTER_HORIZONTAL);
            
            GradientDrawable bgShape = new GradientDrawable();
            bgShape.setColor(Color.WHITE);
            bgShape.setCornerRadius(dp(20)); // Premium rounded corners
            container.setBackground(bgShape);

            // Warning Icon
            TextView iconView = new TextView(ctx);
            iconView.setText("🗑️");
            iconView.setTextSize(36);
            iconView.setGravity(Gravity.CENTER);
            container.addView(iconView);

            // Dialog Title
            TextView title = new TextView(ctx);
            title.setText("Delete Match?");
            title.setTextSize(20);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.parseColor("#1E293B")); // Dark Slate color
            title.setPadding(0, dp(16), 0, dp(8));
            title.setGravity(Gravity.CENTER);
            container.addView(title);

            // Dialog Message
            TextView message = new TextView(ctx);
            message.setText("Are you sure you want to delete this match from history? This action cannot be undone.");
            message.setTextSize(14);
            message.setTextColor(Color.parseColor("#64748B")); // Slate Grey color
            message.setGravity(Gravity.CENTER);
            message.setPadding(0, 0, 0, dp(28));
            container.addView(message);

            // Buttons Layout
            LinearLayout btnLayout = new LinearLayout(ctx);
            btnLayout.setOrientation(LinearLayout.HORIZONTAL);
            btnLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            btnLayout.setGravity(Gravity.CENTER);

            // Cancel Button (Flat look)
            Button btnCancel = new Button(ctx);
            btnCancel.setText("Cancel");
            btnCancel.setTextColor(Color.parseColor("#475569"));
            btnCancel.setBackgroundColor(Color.TRANSPARENT);
            btnCancel.setAllCaps(false);
            btnCancel.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                    0, dp(48), 1.0f);
            cancelParams.setMargins(0, 0, dp(8), 0);
            btnCancel.setLayoutParams(cancelParams);
            
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            // Delete Button (Premium Red color)
            Button btnDelete = new Button(ctx);
            btnDelete.setText("Delete");
            btnDelete.setTextColor(Color.WHITE);
            btnDelete.setAllCaps(false);
            btnDelete.setTypeface(null, Typeface.BOLD);
            GradientDrawable deleteBg = new GradientDrawable();
            deleteBg.setColor(Color.parseColor("#EF4444")); // Modern Red
            deleteBg.setCornerRadius(dp(12));
            btnDelete.setBackground(deleteBg);
            
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                    0, dp(48), 1.0f);
            deleteParams.setMargins(dp(8), 0, 0, 0);
            btnDelete.setLayoutParams(deleteParams);
            
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Database থেকে ডিলিট
                    DataManager.deleteMatch(ctx, match.matchId);
                    
                    // List থেকে রিমুভ করে Adapter আপডেট
                    list.remove(position);
                    notifyDataSetChanged();
                    
                    // Summary Text Update লজিক
                    if (list.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        listHistory.setVisibility(View.GONE);
                        tvSummary.setText("0 matches");
                    } else {
                        int completed = 0, inProgress = 0;
                        for (MatchData m : list) { 
                            if (isCompleted(m)) completed++; 
                            else inProgress++; 
                        }
                        tvSummary.setText(list.size() + " matches  \u2022  " + completed + " completed  \u2022  " + inProgress + " in progress");
                    }
                    
                    Toast.makeText(ctx, "Match deleted", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });

            btnLayout.addView(btnCancel);
            btnLayout.addView(btnDelete);
            container.addView(btnLayout);

            dialog.setContentView(container);
            
            // Background Transparent for showing curved corners
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                int width = (int)(ctx.getResources().getDisplayMetrics().widthPixels * 0.85);
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            
            dialog.show();
        }

        private TextView makeCircleIcon(String teamName, int color) {
            TextView ic = new TextView(ctx);
            String initial = (teamName != null && teamName.length() > 0) ? teamName.substring(0, 1).toUpperCase() : "?";
            ic.setText(initial);
            ic.setTextSize(12);
            ic.setTextColor(Color.WHITE);
            ic.setTypeface(null, Typeface.BOLD);
            ic.setGravity(Gravity.CENTER);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(color);
            ic.setBackground(bg);
            return ic;
        }

        private Button makeActionBtn(String label, int textColor, int bgColor, int borderColor) {
            Button btn = new Button(ctx);
            btn.setText(label);
            btn.setTextSize(11);
            btn.setTextColor(textColor);
            btn.setTypeface(null, Typeface.BOLD);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(bgColor);
            bg.setCornerRadius(dp(8));
            bg.setStroke(dp(1), borderColor);
            btn.setBackground(bg);
            btn.setPadding(dp(12), dp(6), dp(12), dp(6));
            return btn;
        }

        private View makeDivider(int topMargin, int bottomMargin) {
            View d = new View(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
            lp.setMargins(0, topMargin, 0, bottomMargin);
            d.setLayoutParams(lp);
            d.setBackgroundColor(Color.parseColor("#ECEFF1"));
            return d;
        }
    }
}
