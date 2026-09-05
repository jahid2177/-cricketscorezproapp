package com.cricketscorez.proapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.cricketscorez.proapp.room.FavoriteMatchEntity;
import com.cricketscorez.proapp.room.FavoriteMatchRepository;
import java.util.ArrayList;
import java.util.List;

public class FavoriteMatchesActivity extends Activity {

    private LinearLayout rootFavLayout, layoutEmptyState;
    private ListView listFavoriteMatches;
    private TextView tvFavCountBadge, tvEmptyTitle, tvEmptySub;
    private ImageView btnBackFav;

    private FavoriteMatchRepository repository;
    private final List<FavoriteMatchEntity> favoriteList = new ArrayList<>();
    private FavoriteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_matches);

        repository = new FavoriteMatchRepository(this);

        rootFavLayout      = findViewById(R.id.rootFavLayout);
        layoutEmptyState   = findViewById(R.id.layoutEmptyState);
        listFavoriteMatches = findViewById(R.id.listFavoriteMatches);
        tvFavCountBadge    = findViewById(R.id.tvFavCountBadge);
        tvEmptyTitle       = findViewById(R.id.tvEmptyTitle);
        tvEmptySub         = findViewById(R.id.tvEmptySub);
        btnBackFav         = findViewById(R.id.btnBackFav);

        if (btnBackFav != null) {
            btnBackFav.setOnClickListener(v -> finish());
        }

        adapter = new FavoriteAdapter(this, favoriteList);
        if (listFavoriteMatches != null) {
            listFavoriteMatches.setAdapter(adapter);
            listFavoriteMatches.setOnItemClickListener((parent, view, position, id) -> {
                if (position >= 0 && position < favoriteList.size()) {
                    FavoriteMatchEntity fav = favoriteList.get(position);
                    openMatchDetails(fav);
                }
            });
        }

        applyTheme();
        loadFavorites();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void applyTheme() {
        if (rootFavLayout != null) {
            rootFavLayout.setBackground(ThemeManager.getCanvasBackground(this));
        }
        int primaryText = ThemeManager.getPrimaryTextColor(this);
        int secondaryText = ThemeManager.getSecondaryTextColor(this);
        if (tvEmptyTitle != null) tvEmptyTitle.setTextColor(primaryText);
        if (tvEmptySub != null) tvEmptySub.setTextColor(secondaryText);
    }

    private void loadFavorites() {
        repository.getAllFavorites(matches -> {
            favoriteList.clear();
            if (matches != null) {
                favoriteList.addAll(matches);
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            if (tvFavCountBadge != null) {
                tvFavCountBadge.setText(favoriteList.size() + " Tracked");
            }

            if (favoriteList.isEmpty()) {
                if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
                if (listFavoriteMatches != null) listFavoriteMatches.setVisibility(View.GONE);
            } else {
                if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
                if (listFavoriteMatches != null) listFavoriteMatches.setVisibility(View.VISIBLE);
            }
        });
    }

    private void openMatchDetails(FavoriteMatchEntity fav) {
        try {
            ArrayList<MatchData> all = DataManager.getAllMatches(this);
            MatchData matched = null;
            if (all != null) {
                for (MatchData m : all) {
                    if (fav.matchId.equals(m.matchId) ||
                       (fav.team1Name.equals(m.team1Name) && fav.team2Name.equals(m.team2Name))) {
                        matched = m;
                        break;
                    }
                }
            }

            if (matched != null) {
                Intent intent = new Intent(this, ScorecardActivity.class);
                intent.putExtra("MATCH_DATA", matched);
                intent.putExtra("IS_SUMMARY", true);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Match info: " + fav.team1Name + " vs " + fav.team2Name, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Adapter
    // ─────────────────────────────────────────────────────────────────────────
    private class FavoriteAdapter extends BaseAdapter {
        private final Context context;
        private final List<FavoriteMatchEntity> items;

        FavoriteAdapter(Context context, List<FavoriteMatchEntity> items) {
            this.context = context;
            this.items = items;
        }

        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_favorite_match, parent, false);
            }

            FavoriteMatchEntity item = items.get(position);

            View cardRoot        = convertView.findViewById(R.id.cardFavRoot);
            TextView tvDate      = convertView.findViewById(R.id.tvFavDate);
            TextView tvBadge     = convertView.findViewById(R.id.tvFavStatusBadge);
            TextView btnRemove   = convertView.findViewById(R.id.btnRemoveFav);
            TextView tvTeam1     = convertView.findViewById(R.id.tvFavTeam1);
            TextView tvScore1    = convertView.findViewById(R.id.tvFavScore1);
            View layoutInn2      = convertView.findViewById(R.id.layoutFavInn2);
            TextView tvTeam2     = convertView.findViewById(R.id.tvFavTeam2);
            TextView tvScore2    = convertView.findViewById(R.id.tvFavScore2);
            TextView tvResult    = convertView.findViewById(R.id.tvFavResult);

            if (cardRoot != null) {
                cardRoot.setBackground(ThemeManager.getPillMenuBackground(context));
            }

            int primaryColor = ThemeManager.getPrimaryTextColor(context);
            if (tvTeam1 != null) {
                tvTeam1.setText(item.team1Name != null ? item.team1Name : "Team 1");
                tvTeam1.setTextColor(primaryColor);
            }
            if (tvTeam2 != null) {
                tvTeam2.setText(item.team2Name != null ? item.team2Name : "Team 2");
                tvTeam2.setTextColor(primaryColor);
            }

            if (tvDate != null) {
                tvDate.setText(item.matchDate != null && !item.matchDate.isEmpty()
                        ? "📅 " + item.matchDate : "📅 Match");
            }

            if (tvScore1 != null) {
                String s1 = (item.scoreInn1 != null && !item.scoreInn1.isEmpty()) ? item.scoreInn1 : "-";
                String o1 = (item.oversInn1 != null && !item.oversInn1.isEmpty()) ? "(" + item.oversInn1 + " ov)" : "";
                tvScore1.setText(s1 + " " + o1);
            }

            if (item.isSecondInnings || (item.scoreInn2 != null && !item.scoreInn2.isEmpty())) {
                if (layoutInn2 != null) layoutInn2.setVisibility(View.VISIBLE);
                if (tvScore2 != null) {
                    String s2 = item.scoreInn2 != null ? item.scoreInn2 : "-";
                    String o2 = item.oversInn2 != null ? "(" + item.oversInn2 + " ov)" : "";
                    tvScore2.setText(s2 + " " + o2);
                }
            } else {
                if (layoutInn2 != null) layoutInn2.setVisibility(View.GONE);
            }

            if (tvResult != null) {
                if (item.matchStatus != null && !item.matchStatus.isEmpty()) {
                    tvResult.setText(item.matchStatus);
                    tvResult.setVisibility(View.VISIBLE);
                } else {
                    tvResult.setVisibility(View.GONE);
                }
            }

            if (tvBadge != null) {
                if (item.isLive) {
                    tvBadge.setText("LIVE");
                    tvBadge.setBackgroundColor(Color.parseColor("#EF4444"));
                    tvBadge.setVisibility(View.VISIBLE);
                } else {
                    tvBadge.setText("SAVED");
                    tvBadge.setBackgroundColor(Color.parseColor("#3B82F6"));
                    tvBadge.setVisibility(View.VISIBLE);
                }
                GradientDrawable badgeBg = new GradientDrawable();
                badgeBg.setColor(item.isLive ? Color.parseColor("#EF4444") : Color.parseColor("#2563EB"));
                badgeBg.setCornerRadius(16);
                tvBadge.setBackground(badgeBg);
            }

            if (btnRemove != null) {
                btnRemove.setOnClickListener(v -> {
                    repository.removeFavorite(item.matchId, result -> {
                        Toast.makeText(context, "Removed from Favorites", Toast.LENGTH_SHORT).show();
                        loadFavorites();
                    });
                });
            }

            return convertView;
        }
    }
}
