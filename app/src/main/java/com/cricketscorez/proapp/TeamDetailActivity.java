package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

// ── Retrofit Imports ──────────────────────────────────────────────
import com.cricketscorez.proapp.api.ApiClient;
import com.cricketscorez.proapp.api.ApiInterface;
import com.cricketscorez.proapp.models.Player;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeamDetailActivity extends Activity {

    private TextView           tvTeamName;
    private TextView           tvPlayerCount;
    private LinearLayout       layoutEmpty;
    private ListView           listPlayers;
    private ArrayList<String>  playerList;
    private PlayerAdapter      adapter;
    private String             teamName;
    private int                teamId = 0; // সার্ভার থেকে team_id খোঁজার জন্য

    private static final int   REQUEST_TEAM_PHOTO = 106;
    private ImageView          ivTeamLogo;
    private TextView           tvTeamInitial;
    private TextView           btnChangeLogo;
    private TextView           btnRemoveLogo;

    // Avatar colors — consistent with TeamManagerActivity
    private static final int[] AVATAR_COLORS = {
        0xFF1B5E20, 0xFF0D47A1, 0xFF880E4F,
        0xFF4A148C, 0xFFE65100, 0xFF006064,
        0xFF37474F, 0xFF558B2F
    };

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        teamName = getIntent().getStringExtra("TEAM_NAME");
        if (teamName == null) teamName = "Team";

        // ── Root layout (built programmatically to match TeamManagerActivity) ──
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F0F4F8"));

        // ── Header ────────────────────────────────────────────────────────
        android.widget.RelativeLayout header = new android.widget.RelativeLayout(this);
        android.widget.RelativeLayout.LayoutParams hLp = new android.widget.RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(60));
        header.setLayoutParams(hLp);
        GradientDrawable headerBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{Color.parseColor("#0D3B0E"), Color.parseColor("#2E7D32")});
        header.setBackground(headerBg);
        header.setElevation(dp(6));

        // Back button
        TextView btnBack = new TextView(this);
        android.widget.RelativeLayout.LayoutParams backLp = new android.widget.RelativeLayout.LayoutParams(dp(36), dp(36));
        backLp.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        backLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
        backLp.setMargins(dp(10), 0, 0, 0);
        btnBack.setLayoutParams(backLp);
        btnBack.setText("←");
        btnBack.setTextColor(Color.WHITE);
        btnBack.setTextSize(22);
        btnBack.setGravity(Gravity.CENTER);
        btnBack.setOnClickListener(v -> finish());
        header.addView(btnBack);

        // Center title block
        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setGravity(Gravity.CENTER_HORIZONTAL);
        android.widget.RelativeLayout.LayoutParams titleLp =
            new android.widget.RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
        titleBlock.setLayoutParams(titleLp);

        tvTeamName = new TextView(this);
        tvTeamName.setText(teamName);
        tvTeamName.setTextSize(17);
        tvTeamName.setTypeface(null, Typeface.BOLD);
        tvTeamName.setTextColor(Color.WHITE);
        tvTeamName.setGravity(Gravity.CENTER);
        tvTeamName.setLetterSpacing(0.01f);
        titleBlock.addView(tvTeamName);

        tvPlayerCount = new TextView(this);
        tvPlayerCount.setTextSize(11);
        tvPlayerCount.setTextColor(Color.parseColor("#A5D6A7"));
        tvPlayerCount.setGravity(Gravity.CENTER);
        titleBlock.addView(tvPlayerCount);
        header.addView(titleBlock);

        // + Add Player button
        LinearLayout fabBtn = new LinearLayout(this);
        fabBtn.setGravity(Gravity.CENTER);
        fabBtn.setPadding(dp(12), 0, dp(12), 0);
        android.widget.RelativeLayout.LayoutParams fabLp =
            new android.widget.RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
        fabLp.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        fabLp.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
        fabLp.setMargins(0, 0, dp(12), 0);
        fabBtn.setLayoutParams(fabLp);
        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setColor(Color.WHITE);
        fabBg.setCornerRadius(dp(16));
        fabBg.setStroke(dp(1), Color.parseColor("#43A047"));
        fabBtn.setBackground(fabBg);
        fabBtn.setOnClickListener(v -> showPlayerDialog(null));
        TextView fabText = new TextView(this);
        fabText.setText("+ Add");
        fabText.setTextColor(Color.parseColor("#1B5E20"));
        fabText.setTextSize(12);
        fabText.setTypeface(null, Typeface.BOLD);
        fabBtn.addView(fabText);
        header.addView(fabBtn);

        root.addView(header);

        // ── Team Profile / Logo Card ──────────────────────────────────────────
        LinearLayout teamCard = new LinearLayout(this);
        teamCard.setOrientation(LinearLayout.HORIZONTAL);
        teamCard.setGravity(Gravity.CENTER_VERTICAL);
        teamCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        teamCard.setBackgroundColor(Color.WHITE);
        teamCard.setElevation(dp(2));
        LinearLayout.LayoutParams tcLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tcLp.setMargins(0, 0, 0, dp(4));
        teamCard.setLayoutParams(tcLp);

        // Logo Container
        FrameLayout logoContainer = new FrameLayout(this);
        logoContainer.setLayoutParams(new LinearLayout.LayoutParams(dp(54), dp(54)));

        tvTeamInitial = new TextView(this);
        tvTeamInitial.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tvTeamInitial.setGravity(Gravity.CENTER);
        tvTeamInitial.setTextSize(20);
        tvTeamInitial.setTypeface(null, Typeface.BOLD);
        tvTeamInitial.setTextColor(Color.WHITE);
        int colorIdx = Math.abs(teamName.hashCode()) % AVATAR_COLORS.length;
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(AVATAR_COLORS[colorIdx]);
        tvTeamInitial.setBackground(avatarBg);
        tvTeamInitial.setText(teamName.isEmpty() ? "T" : String.valueOf(teamName.charAt(0)).toUpperCase());
        logoContainer.addView(tvTeamInitial);

        ivTeamLogo = new ImageView(this);
        ivTeamLogo.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivTeamLogo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivTeamLogo.setVisibility(View.GONE);
        logoContainer.addView(ivTeamLogo);

        logoContainer.setOnClickListener(v -> showTeamPhotoOptionsDialog());
        teamCard.addView(logoContainer);

        // Text & Actions column
        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        infoLp.setMargins(dp(14), 0, dp(8), 0);
        infoCol.setLayoutParams(infoLp);

        TextView tvCardTitle = new TextView(this);
        tvCardTitle.setText(teamName);
        tvCardTitle.setTextSize(16);
        tvCardTitle.setTypeface(null, Typeface.BOLD);
        tvCardTitle.setTextColor(Color.parseColor("#0F172A"));
        infoCol.addView(tvCardTitle);

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, dp(4), 0, 0);

        btnChangeLogo = new TextView(this);
        btnChangeLogo.setText("📷 Upload Team Photo");
        btnChangeLogo.setTextSize(12);
        btnChangeLogo.setTypeface(null, Typeface.BOLD);
        btnChangeLogo.setTextColor(Color.parseColor("#16A34A"));
        btnChangeLogo.setPadding(0, dp(2), dp(12), dp(2));
        btnChangeLogo.setOnClickListener(v -> showTeamPhotoOptionsDialog());
        actionRow.addView(btnChangeLogo);

        btnRemoveLogo = new TextView(this);
        btnRemoveLogo.setText("🗑️ Remove");
        btnRemoveLogo.setTextSize(12);
        btnRemoveLogo.setTextColor(Color.parseColor("#DC2626"));
        btnRemoveLogo.setPadding(0, dp(2), 0, dp(2));
        btnRemoveLogo.setVisibility(View.GONE);
        btnRemoveLogo.setOnClickListener(v -> {
            ImageStorageHelper.deleteTeamLogo(this, teamName);
            loadTeamLogoPreview();
            Toast.makeText(this, "Team photo removed", Toast.LENGTH_SHORT).show();
        });
        actionRow.addView(btnRemoveLogo);

        infoCol.addView(actionRow);
        teamCard.addView(infoCol);

        root.addView(teamCard);

        // ── Empty state ───────────────────────────────────────────────────
        layoutEmpty = new LinearLayout(this);
        layoutEmpty.setOrientation(LinearLayout.VERTICAL);
        layoutEmpty.setGravity(Gravity.CENTER);
        layoutEmpty.setPadding(dp(32), dp(48), dp(32), dp(48));
        layoutEmpty.setVisibility(View.GONE);

        TextView emIcon = new TextView(this);
        emIcon.setText("👤");
        emIcon.setTextSize(40);
        emIcon.setGravity(Gravity.CENTER);
        layoutEmpty.addView(emIcon);

        TextView emTitle = new TextView(this);
        emTitle.setText("No players yet");
        emTitle.setTextSize(15);
        emTitle.setTypeface(null, Typeface.BOLD);
        emTitle.setTextColor(Color.parseColor("#374151"));
        emTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams emTitleLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        emTitleLp.setMargins(0, dp(10), 0, dp(4));
        emTitle.setLayoutParams(emTitleLp);
        layoutEmpty.addView(emTitle);

        TextView emHint = new TextView(this);
        emHint.setText("Tap \"+ Add\" to add players to this team");
        emHint.setTextSize(12);
        emHint.setTextColor(Color.parseColor("#94A3B8"));
        emHint.setGravity(Gravity.CENTER);
        layoutEmpty.addView(emHint);

        root.addView(layoutEmpty, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ── Player list ───────────────────────────────────────────────────
        listPlayers = new ListView(this);
        listPlayers.setDivider(null);
        listPlayers.setDividerHeight(0);
        listPlayers.setClipToPadding(false);
        listPlayers.setPadding(0, dp(8), 0, dp(16));
        listPlayers.setOnItemClickListener((parent, view, position, id) -> {
            Intent i = new Intent(TeamDetailActivity.this, PlayerProfileActivity.class);
            i.putExtra("PLAYER_NAME", playerList.get(position));
            i.putExtra("TEAM_NAME",   teamName);
            startActivity(i);
        });
        root.addView(listPlayers, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        loadPlayers();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void loadPlayers() {
        // ১. লোকাল ডাটাবেস থেকে লোড করা
        playerList = DataManager.getPlayers(this, teamName);
        if (playerList == null) playerList = new ArrayList<>();

        updateUI();

        // ২. MySQL সার্ভার থেকে ডেটা লোড করার মেথড কল
        fetchPlayersFromServer();
    }

    private void updateUI() {
        int pc = playerList.size();
        tvPlayerCount.setText(pc + " player" + (pc != 1 ? "s" : ""));
        tvTeamName.setText(teamName);

        loadTeamLogoPreview();

        boolean empty = (pc == 0);
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        listPlayers.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (adapter == null) {
            adapter = new PlayerAdapter(this, playerList);
            listPlayers.setAdapter(adapter);
        } else {
            adapter.updateList(playerList);
        }
    }

    private void loadTeamLogoPreview() {
        if (ivTeamLogo == null || tvTeamInitial == null) return;
        String path = ImageStorageHelper.getTeamLogoPath(this, teamName);
        if (path != null) {
            ImageStorageHelper.loadTeamLogoInto(this, teamName, ivTeamLogo, tvTeamInitial);
            if (btnChangeLogo != null) btnChangeLogo.setText("📷 Change Photo");
            if (btnRemoveLogo != null) btnRemoveLogo.setVisibility(View.VISIBLE);
        } else {
            ivTeamLogo.setVisibility(View.GONE);
            tvTeamInitial.setVisibility(View.VISIBLE);
            if (btnChangeLogo != null) btnChangeLogo.setText("📷 Upload Team Photo");
            if (btnRemoveLogo != null) btnRemoveLogo.setVisibility(View.GONE);
        }
    }

    private void showTeamPhotoOptionsDialog() {
        String path = ImageStorageHelper.getTeamLogoPath(this, teamName);
        if (path != null) {
            new AlertDialog.Builder(this)
                .setTitle("Team Photo")
                .setItems(new String[]{"🖼️  Choose New Photo from Gallery", "🗑️  Remove Photo", "Cancel"}, (dialog, which) -> {
                    if (which == 0) {
                        openTeamGalleryPicker();
                    } else if (which == 1) {
                        ImageStorageHelper.deleteTeamLogo(this, teamName);
                        loadTeamLogoPreview();
                        Toast.makeText(this, "Team photo removed", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
        } else {
            openTeamGalleryPicker();
        }
    }

    private void openTeamGalleryPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_TEAM_PHOTO);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_TEAM_PHOTO);
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open gallery", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TEAM_PHOTO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String savedPath = ImageStorageHelper.saveTeamLogo(this, teamName, uri);
            if (savedPath != null) {
                loadTeamLogoPreview();
                Toast.makeText(this, "✅ Team photo updated successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ── MySQL সার্ভার থেকে প্লেয়ার আনার মেথড ───────────────────────────
    private void fetchPlayersFromServer() {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        
        // যেহেতু get_players.php তে team_id লাগে, তাই এটি আপাতত 0 পাঠানো হচ্ছে। 
        // ভবিষ্যতে team_id ঠিকমতো সেভ হলে এখানে পাস করা হবে।
        apiInterface.getPlayers(0).enqueue(new Callback<List<Player>>() {
            @Override
            public void onResponse(Call<List<Player>> call, Response<List<Player>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Player> serverPlayers = response.body();
                    // যদি সার্ভারে ওই টিমের প্লেয়ার পাওয়া যায়, তবে লিস্ট আপডেট করা যেতে পারে
                    // আপাতত এটি লোকাল ডাটাবেস এর সাথেই কাজ করছে।
                }
            }

            @Override
            public void onFailure(Call<List<Player>> call, Throwable t) {
                Log.e("API_ERROR", "Error fetching players: " + t.getMessage());
            }
        });
    }

    // ── নিজস্ব MySQL সার্ভারে প্লেয়ার ডেটা সেভ করার মেথড ─────────────────
    private void addPlayerToMySql(String playerName, String role) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        apiInterface.addPlayer(teamName, playerName, role).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("MYSQL_PLAYER", "Player saved to MySQL");
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MYSQL_ERROR", "Failed to save player: " + t.getMessage());
            }
        });
    }

    // ── নিজস্ব MySQL সার্ভারে প্লেয়ার আপডেট করার মেথড ───────────────────
    private void updatePlayerInMySql(String oldName, String newName) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        apiInterface.updatePlayer(teamName, oldName, newName).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("MYSQL_PLAYER", "Player updated on MySQL");
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MYSQL_ERROR", "Failed to update player: " + t.getMessage());
            }
        });
    }

    // ── নিজস্ব MySQL সার্ভার থেকে প্লেয়ার ডিলিট করার মেথড ─────────────────
    private void deletePlayerFromMySql(String playerName) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        apiInterface.deletePlayer(teamName, playerName).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("MYSQL_PLAYER", "Player deleted from MySQL");
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MYSQL_ERROR", "Failed to delete player: " + t.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Add / Edit Player dialog  (uses dialog_add_player.xml)
    // ─────────────────────────────────────────────────────────────────────────
    private void showPlayerDialog(final String oldName) {
        final View dlgView = LayoutInflater.from(this).inflate(R.layout.dialog_add_player, null);

        final TextView tvTitle   = dlgView.findViewById(R.id.tvDialogTitle);
        final EditText etName    = dlgView.findViewById(R.id.etPlayerName);
        final Button   btnSave   = dlgView.findViewById(R.id.btnDialogSave);
        final Button   btnCancel = dlgView.findViewById(R.id.btnDialogCancel);

        final CheckBox cbBat  = dlgView.findViewById(R.id.cbBatsman);
        final CheckBox cbBowl = dlgView.findViewById(R.id.cbBowler);
        final CheckBox cbAll  = dlgView.findViewById(R.id.cbAllRounder);
        final CheckBox cbWK   = dlgView.findViewById(R.id.cbWicketKeeper);
        final CheckBox cbCap  = dlgView.findViewById(R.id.cbCaptain);

        tvTitle.setText(oldName == null ? "➕  Add Player" : "✏️  Edit Player");
        btnSave.setText(oldName == null ? "Add" : "Save");

        if (oldName != null) {
            etName.setText(oldName);
            etName.setSelection(oldName.length());
            String role = DataManager.getPlayerRole(this, oldName);
            if (role != null) {
                if (role.contains("Batsman"))     cbBat.setChecked(true);
                if (role.contains("Bowler"))      cbBowl.setChecked(true);
                if (role.contains("All-Rounder")) cbAll.setChecked(true);
                if (role.contains("WK"))          cbWK.setChecked(true);
                if (role.contains("Captain"))     cbCap.setChecked(true);
            }
        }

        final AlertDialog[] holder = { null };
        holder[0] = new AlertDialog.Builder(this)
                .setView(dlgView)
                .create();
        if (holder[0].getWindow() != null) {
            holder[0].getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Player name is required");
                etName.requestFocus();
                return;
            }
            
            // Build role string
            StringBuilder roles = new StringBuilder();
            if (cbBat.isChecked())  roles.append("Batsman, ");
            if (cbBowl.isChecked()) roles.append("Bowler, ");
            if (cbAll.isChecked())  roles.append("All-Rounder, ");
            if (cbWK.isChecked())   roles.append("WK, ");
            if (cbCap.isChecked())  roles.append("Captain, ");
            String finalRole = roles.toString();

            if (oldName != null && !oldName.equals(name)) {
                playerList.remove(oldName);
                // ★ সার্ভারে প্লেয়ারের নাম আপডেট করা
                updatePlayerInMySql(oldName, name);
            } else if (oldName == null) {
                // ★ নতুন প্লেয়ার সার্ভারে সেভ করা
                addPlayerToMySql(name, finalRole);
            }
            
            if (!playerList.contains(name)) playerList.add(name);
            DataManager.saveTeam(this, teamName, playerList);
            DataManager.savePlayerRole(this, name, finalRole);
            DataManager.ensurePlayerStats(this, name);

            // ☁️ SUPABASE Sync
            FirebaseSync.upsertTeam(teamName, playerList);

            Toast.makeText(this,
                oldName == null ? "✅ Player added!" : "✅ Player updated!",
                Toast.LENGTH_SHORT).show();
            loadPlayers();
            holder[0].dismiss();
        });

        btnCancel.setOnClickListener(v -> holder[0].dismiss());
        holder[0].show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Inner Adapter
    // ─────────────────────────────────────────────────────────────────────────
    public class PlayerAdapter extends BaseAdapter {
        private Context           ctx;
        private ArrayList<String> list;

        public PlayerAdapter(Context ctx, ArrayList<String> list) {
            this.ctx  = ctx;
            this.list = list;
        }

        public void updateList(ArrayList<String> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @Override public int    getCount()         { return list.size(); }
        @Override public Object getItem(int pos)   { return list.get(pos); }
        @Override public long   getItemId(int pos) { return pos; }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final String pName = list.get(position);

            View row = LayoutInflater.from(ctx).inflate(R.layout.row_player, parent, false);

            // Avatar — color by hashCode, initial letter or player photo
            TextView tvInitial = row.findViewById(R.id.tvPlayerInitial);
            ImageView ivPhoto  = row.findViewById(R.id.ivPlayerPhoto);

            int colorIdx = Math.abs(pName.hashCode()) % AVATAR_COLORS.length;
            GradientDrawable avatarBg = new GradientDrawable();
            avatarBg.setShape(GradientDrawable.OVAL);
            avatarBg.setColor(AVATAR_COLORS[colorIdx]);
            tvInitial.setBackground(avatarBg);
            tvInitial.setText(pName.isEmpty()
                    ? "P" : String.valueOf(pName.charAt(0)).toUpperCase());

            ImageStorageHelper.loadPlayerPhotoInto(ctx, pName, ivPhoto, tvInitial);

            // Name
            TextView tvName = row.findViewById(R.id.tvPlayerName);
            tvName.setText(pName);

            // Role badge — primary role only for clean display
            TextView tvRole = row.findViewById(R.id.tvPlayerRole);
            String role = DataManager.getPlayerRole(ctx, pName);
            if (role != null && !role.isEmpty()) {
                // Show first role only as badge (cleaner)
                String primary = role.split(",")[0].trim();
                tvRole.setText(primary);
                // Color-code badge by role
                if (primary.contains("Batsman")) {
                    tvRole.setTextColor(Color.parseColor("#15803D"));
                    setRoleBadgeBg(tvRole, "#DCFCE7", "#86EFAC");
                } else if (primary.contains("Bowler")) {
                    tvRole.setTextColor(Color.parseColor("#1565C0"));
                    setRoleBadgeBg(tvRole, "#DBEAFE", "#93C5FD");
                } else if (primary.contains("All-Rounder")) {
                    tvRole.setTextColor(Color.parseColor("#7C3AED"));
                    setRoleBadgeBg(tvRole, "#EDE9FE", "#C4B5FD");
                } else if (primary.contains("Captain")) {
                    tvRole.setTextColor(Color.parseColor("#B45309"));
                    setRoleBadgeBg(tvRole, "#FEF3C7", "#FCD34D");
                } else if (primary.contains("WK")) {
                    tvRole.setTextColor(Color.parseColor("#0E7490"));
                    setRoleBadgeBg(tvRole, "#CFFAFE", "#67E8F9");
                } else {
                    tvRole.setTextColor(Color.parseColor("#64748B"));
                    setRoleBadgeBg(tvRole, "#F1F5F9", "#CBD5E1");
                }
                tvRole.setVisibility(View.VISIBLE);
            } else {
                tvRole.setVisibility(View.GONE);
            }

            // Open Player Profile on tapping row, name, or initial
            View.OnClickListener openProfileListener = v -> {
                Intent i = new Intent(ctx, PlayerProfileActivity.class);
                i.putExtra("PLAYER_NAME", pName);
                i.putExtra("TEAM_NAME",   teamName);
                ctx.startActivity(i);
            };
            row.setOnClickListener(openProfileListener);
            tvName.setOnClickListener(openProfileListener);
            tvInitial.setOnClickListener(openProfileListener);

            // Edit
            View btnEdit = row.findViewById(R.id.btnEdit);
            btnEdit.setOnClickListener(v -> showPlayerDialog(pName));

            // Delete
            View btnDelete = row.findViewById(R.id.btnDelete);
            btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(ctx)
                    .setTitle("🗑️  Remove Player")
                    .setMessage("Remove \"" + pName + "\" from this team?")
                    .setPositiveButton("Remove", (dialog, which) -> {
                        playerList.remove(pName);
                        DataManager.saveTeam(ctx, teamName, playerList);
                        DataManager.deletePlayerData(ctx, pName);
                        FirebaseSync.upsertTeam(teamName, playerList);
                        
                        // ★ সার্ভার থেকে প্লেয়ার ডিলিট করা
                        deletePlayerFromMySql(pName);
                        
                        loadPlayers();
                    })
                    .setNegativeButton("Cancel", null)
                    .show()
            );

            return row;
        }

        private void setRoleBadgeBg(TextView tv, String bgHex, String borderHex) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.parseColor(bgHex));
            bg.setCornerRadius(dp(20));
            bg.setStroke(dp(1), Color.parseColor(borderHex));
            tv.setBackground(bg);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlayers();
    }
}
