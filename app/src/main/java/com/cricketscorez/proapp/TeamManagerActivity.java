package com.cricketscorez.proapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
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
import com.cricketscorez.proapp.models.Team;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeamManagerActivity extends Activity {

    private ListView          listViewTeams;
    private ArrayList<String> teamList;
    private ArrayList<String> filteredList;
    private TeamAdapter       adapter;
    private LinearLayout      layoutEmpty;

    // Team Logo picking state for dialog
    private static final int REQUEST_TEAM_LOGO_GALLERY = 205;
    private Uri pendingTeamLogoUri = null;
    private boolean isPendingLogoRemoved = false;
    private ImageView currentDialogLogoIv = null;
    private TextView currentDialogInitialTv = null;
    private View currentDialogRemoveBtn = null;
    private TextView currentDialogHintTv = null;

    // Avatar colors — one per team based on name hashCode
    private static final int[] AVATAR_COLORS = {
        0xFF1B5E20, 0xFF0D47A1, 0xFF880E4F,
        0xFF4A148C, 0xFFE65100, 0xFF006064,
        0xFF37474F, 0xFF558B2F
    };

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_manager);

        // ── Bind views ────────────────────────────────────────────────────
        listViewTeams = findViewById(R.id.listViewTeams);
        layoutEmpty   = findViewById(R.id.layoutEmpty);

        // Back
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // + New Team
        View btnAddTeam = findViewById(R.id.btnAddTeam);
        if (btnAddTeam != null) btnAddTeam.setOnClickListener(v -> showTeamDialog(null));

        // Search
        EditText etSearch = findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    filterTeams(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // List item click → TeamDetail
        listViewTeams.setOnItemClickListener((parent, view, position, id) -> {
            String teamName = filteredList.get(position);
            Intent intent = new Intent(TeamManagerActivity.this, TeamDetailActivity.class);
            intent.putExtra("TEAM_NAME", teamName);
            startActivity(intent);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        loadTeams();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void loadTeams() {
        // ১. প্রথমে লোকাল DataManager থেকে ডেটা লোড করবে
        teamList     = DataManager.getAllTeams(this);
        filteredList = new ArrayList<>(teamList);
        if (adapter == null) {
            adapter = new TeamAdapter(this, filteredList);
            listViewTeams.setAdapter(adapter);
        } else {
            adapter.updateList(filteredList);
        }
        updateEmptyState();

        // ২. এরপর ব্যাকগ্রাউন্ডে MySQL সার্ভার থেকে ডেটা আনবে
        fetchTeamsFromServer();
    }

    // ── MySQL সার্ভার থেকে ডেটা আনার মেথড (Retrofit API) ─────────────────
    private void fetchTeamsFromServer() {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);

        apiInterface.getTeams().enqueue(new Callback<List<Team>>() {
            @Override
            public void onResponse(Call<List<Team>> call, Response<List<Team>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Team> serverTeams = response.body();
                    
                    teamList.clear(); // লোকাল লিস্ট ক্লিয়ার করে সার্ভারের ডেটা বসানো হচ্ছে
                    for (Team team : serverTeams) {
                        teamList.add(team.getName()); 
                    }
                    
                    filteredList = new ArrayList<>(teamList);
                    if (adapter == null) {
                        adapter = new TeamAdapter(TeamManagerActivity.this, filteredList);
                        listViewTeams.setAdapter(adapter);
                    } else {
                        adapter.updateList(filteredList);
                    }
                    updateEmptyState();
                    Log.d("API_SUCCESS", "Teams Loaded from Server! Total: " + serverTeams.size());
                }
            }

            @Override
            public void onFailure(Call<List<Team>> call, Throwable t) {
                Log.e("API_ERROR", "Error fetching from MySQL: " + t.getMessage());
                // সার্ভারে কানেক্ট না হলে লোকাল ডেটাই দেখানো থাকবে
            }
        });
    }

    // ── নিজস্ব MySQL সার্ভারে ডেটা সেভ করার মেথড ───────────────────────────
    private void saveTeamToMySql(String teamName) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        
        // শর্ট নেম হিসেবে টিমের নামের প্রথম ৩টি অক্ষর নেওয়া হচ্ছে
        String shortName = teamName.length() >= 3 ? teamName.substring(0, 3).toUpperCase() : teamName.toUpperCase();

        apiInterface.addTeam(teamName, shortName).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("MYSQL_SAVE", "Team successfully saved to your server!");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MYSQL_ERROR", "Failed to save to MySQL: " + t.getMessage());
            }
        });
    }

    // ── নিজস্ব MySQL সার্ভারে টিম আপডেট করার মেথড ───────────────────────────
    private void updateTeamInMySql(String oldName, String newName) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        String shortName = newName.length() >= 3 ? newName.substring(0, 3).toUpperCase() : newName.toUpperCase();

        apiInterface.updateTeam(oldName, newName, shortName).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("MYSQL_UPDATE", "Team successfully updated on server!");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MYSQL_ERROR", "Failed to update on MySQL: " + t.getMessage());
            }
        });
    }

    // ── নিজস্ব MySQL সার্ভার থেকে টিম ডিলিট করার মেথড ────────────────────────
    private void deleteTeamFromMySql(String teamName) {
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);

        apiInterface.deleteTeam(teamName).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("MYSQL_DELETE", "Team successfully deleted from server!");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("MYSQL_ERROR", "Failed to delete from MySQL: " + t.getMessage());
            }
        });
    }

    private void filterTeams(String query) {
        filteredList = new ArrayList<>();
        for (String t : teamList) {
            if (t.toLowerCase().contains(query.toLowerCase())) filteredList.add(t);
        }
        adapter.updateList(filteredList);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (layoutEmpty == null) return;
        boolean empty = filteredList == null || filteredList.isEmpty();
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        listViewTeams.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Create / Edit Team dialog  (uses dialog_team.xml)
    // ─────────────────────────────────────────────────────────────────────────
    private void showTeamDialog(final String oldName) {
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_team, null);

        final TextView tvTitle        = dialogView.findViewById(R.id.tvDialogTeamTitle);
        final EditText etName         = dialogView.findViewById(R.id.etDialogTeamName);
        final Button   btnSave        = dialogView.findViewById(R.id.btnDialogSave);
        final Button   btnCancel      = dialogView.findViewById(R.id.btnDialogCancel);
        final View     layoutPhoto    = dialogView.findViewById(R.id.layoutDialogTeamPhoto);
        final TextView tvInitial      = dialogView.findViewById(R.id.tvDialogTeamInitial);
        final ImageView ivLogo        = dialogView.findViewById(R.id.ivDialogTeamLogo);
        final TextView tvPhotoHint    = dialogView.findViewById(R.id.tvDialogPhotoHint);
        final TextView btnRemovePhoto = dialogView.findViewById(R.id.btnDialogRemovePhoto);

        // Reset dialog photo state
        pendingTeamLogoUri = null;
        isPendingLogoRemoved = false;
        currentDialogLogoIv = ivLogo;
        currentDialogInitialTv = tvInitial;
        currentDialogRemoveBtn = btnRemovePhoto;
        currentDialogHintTv = tvPhotoHint;

        final boolean isEdit = (oldName != null);
        tvTitle.setText(isEdit ? "✏️  Edit Team" : "🏆  Create New Team");
        if (isEdit) {
            etName.setText(oldName);
            etName.setSelection(oldName.length());
            if (!oldName.isEmpty()) {
                tvInitial.setText(oldName.substring(0, 1).toUpperCase());
            }
            // Load existing logo if available
            String existingPath = ImageStorageHelper.getTeamLogoPath(this, oldName);
            if (existingPath != null) {
                ImageStorageHelper.loadTeamLogoInto(this, oldName, ivLogo, tvInitial);
                btnRemovePhoto.setVisibility(View.VISIBLE);
                tvPhotoHint.setText("Tap to change logo");
            }
        }

        // Live initial update while typing
        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (ivLogo.getVisibility() != View.VISIBLE) {
                    String str = s.toString().trim();
                    tvInitial.setText(str.isEmpty() ? "T" : str.substring(0, 1).toUpperCase());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Photo Click — choose from gallery
        layoutPhoto.setOnClickListener(v -> openGalleryPickerForTeam());

        // Remove Photo Click
        btnRemovePhoto.setOnClickListener(v -> {
            pendingTeamLogoUri = null;
            isPendingLogoRemoved = true;
            ivLogo.setVisibility(View.GONE);
            tvInitial.setVisibility(View.VISIBLE);
            String str = etName.getText().toString().trim();
            tvInitial.setText(str.isEmpty() ? "T" : str.substring(0, 1).toUpperCase());
            btnRemovePhoto.setVisibility(View.GONE);
            tvPhotoHint.setText("Tap to choose team logo");
            Toast.makeText(this, "Logo removed", Toast.LENGTH_SHORT).show();
        });

        final AlertDialog[] holder = { null };
        holder[0] = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (holder[0].getWindow() != null) {
            holder[0].getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (newName.isEmpty()) {
                etName.setError("Team name is required");
                etName.requestFocus();
                return;
            }
            if (isEdit) {
                ArrayList<String> players = DataManager.getPlayers(this, oldName);
                DataManager.renameTeam(this, oldName, newName, players);

                // Handle Photo Save / Delete / Rename
                if (isPendingLogoRemoved) {
                    ImageStorageHelper.deleteTeamLogo(this, newName);
                } else if (pendingTeamLogoUri != null) {
                    ImageStorageHelper.saveTeamLogo(this, newName, pendingTeamLogoUri);
                }

                FirebaseSync.deleteTeam(oldName);
                FirebaseSync.upsertTeam(newName, players);
                
                // ★ নিজস্ব MySQL সার্ভারে আপডেট করার জন্য কল
                updateTeamInMySql(oldName, newName);
                
                Toast.makeText(this, "✅ Team updated successfully!", Toast.LENGTH_SHORT).show();
            } else {
                if (DataManager.getAllTeams(this).contains(newName)) {
                    etName.setError("A team with this name already exists");
                    etName.requestFocus();
                    return;
                }
                
                // ১. লোকাল এবং সুপাবেস সেভ
                DataManager.saveTeam(this, newName, new ArrayList<>());

                // Handle Photo Save
                if (pendingTeamLogoUri != null) {
                    ImageStorageHelper.saveTeamLogo(this, newName, pendingTeamLogoUri);
                }

                FirebaseSync.upsertTeam(newName, new ArrayList<>());
                
                // ২. ★ নিজস্ব MySQL সার্ভারে সেভ করার জন্য কল
                saveTeamToMySql(newName);

                Toast.makeText(this, "🏆 \"" + newName + "\" created!", Toast.LENGTH_SHORT).show();
            }
            loadTeams();
            holder[0].dismiss();
        });

        btnCancel.setOnClickListener(v -> holder[0].dismiss());
        holder[0].show();
    }

    private void openGalleryPickerForTeam() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_TEAM_LOGO_GALLERY);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_TEAM_LOGO_GALLERY);
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open gallery", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TEAM_LOGO_GALLERY && resultCode == RESULT_OK && data != null && data.getData() != null) {
            pendingTeamLogoUri = data.getData();
            isPendingLogoRemoved = false;
            if (currentDialogLogoIv != null && currentDialogInitialTv != null) {
                currentDialogLogoIv.setImageURI(pendingTeamLogoUri);
                currentDialogLogoIv.setVisibility(View.VISIBLE);
                currentDialogInitialTv.setVisibility(View.GONE);
                if (currentDialogRemoveBtn != null) currentDialogRemoveBtn.setVisibility(View.VISIBLE);
                if (currentDialogHintTv != null) currentDialogHintTv.setText("Tap to change logo");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Inner Adapter
    // ─────────────────────────────────────────────────────────────────────────
    public class TeamAdapter extends BaseAdapter {
        private Context           ctx;
        private ArrayList<String> list;

        public TeamAdapter(Context ctx, ArrayList<String> list) {
            this.ctx  = ctx;
            this.list = list;
        }

        public void updateList(ArrayList<String> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @Override public int    getCount()              { return list.size(); }
        @Override public Object getItem(int pos)        { return list.get(pos); }
        @Override public long   getItemId(int pos)      { return pos; }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final String teamName = list.get(position);

            View row = LayoutInflater.from(ctx).inflate(R.layout.row_team, parent, false);

            // Avatar — color by hashCode, initial letter or actual team logo
            TextView tvInitial  = row.findViewById(R.id.tvTeamInitial);
            ImageView ivLogo    = row.findViewById(R.id.ivTeamLogo);

            int colorIdx = Math.abs(teamName.hashCode()) % AVATAR_COLORS.length;
            GradientDrawable avatarBg = new GradientDrawable();
            avatarBg.setShape(GradientDrawable.OVAL);
            avatarBg.setColor(AVATAR_COLORS[colorIdx]);
            tvInitial.setBackground(avatarBg);
            tvInitial.setText(teamName.isEmpty()
                    ? "T" : String.valueOf(teamName.charAt(0)).toUpperCase());

            ImageStorageHelper.loadTeamLogoInto(ctx, teamName, ivLogo, tvInitial);

            // Name
            TextView tvName = row.findViewById(R.id.tvTeamName);
            tvName.setText(teamName);

            // Player count
            TextView tvCount = row.findViewById(R.id.tvPlayerCount);
            ArrayList<String> players = DataManager.getPlayers(ctx, teamName);
            int pc = (players != null) ? players.size() : 0;
            tvCount.setText("👤 " + pc + " player" + (pc != 1 ? "s" : ""));

            // Edit
            View btnEdit = row.findViewById(R.id.btnEdit);
            btnEdit.setOnClickListener(v -> showTeamDialog(teamName));

            // Delete
            View btnDelete = row.findViewById(R.id.btnDelete);
            btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(ctx)
                    .setTitle("🗑️  Delete Team")
                    .setMessage("Delete \"" + teamName + "\"?\nAll player data will be removed.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        DataManager.deleteTeam(ctx, teamName);
                        FirebaseSync.deleteTeam(teamName);
                        
                        // ★ নিজস্ব MySQL সার্ভার থেকে ডিলিট করার কল
                        deleteTeamFromMySql(teamName);
                        
                        Toast.makeText(ctx, "\"" + teamName + "\" deleted", Toast.LENGTH_SHORT).show();
                        loadTeams();
                    })
                    .setNegativeButton("Cancel", null)
                    .show()
            );

            return row;
        }
    }
}
