package com.cricketscorez.proapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

public class PlayerAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<PlayerModel> playerList;
    private PlayerActionListener listener;

    public interface PlayerActionListener {
        void onEdit(PlayerModel player);
        void onDelete(int id);
        void onProfileClick(PlayerModel player);
    }

    public PlayerAdapter(Context context, ArrayList<PlayerModel> playerList, PlayerActionListener listener) {
        this.context = context;
        this.playerList = playerList;
        this.listener = listener;
    }

    @Override
    public int getCount() { return playerList.size(); }
    @Override
    public Object getItem(int position) { return playerList.get(position); }
    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.row_player, parent, false);
        }

        final PlayerModel player = playerList.get(position);

        TextView name = (TextView) convertView.findViewById(R.id.tvPlayerName);
        TextView role = (TextView) convertView.findViewById(R.id.tvPlayerRole);
        TextView initial = (TextView) convertView.findViewById(R.id.tvPlayerInitial);
        ImageView ivPhoto = (ImageView) convertView.findViewById(R.id.ivPlayerPhoto);
        ImageView edit = (ImageView) convertView.findViewById(R.id.btnEdit);
        ImageView delete = (ImageView) convertView.findViewById(R.id.btnDelete);

        name.setText(player.name);
        role.setText(player.role);

        if (player.name != null && !player.name.trim().isEmpty()) {
            initial.setText(player.name.trim().substring(0, 1).toUpperCase());
        }

        // Load photo if exists, otherwise display initial
        ImageStorageHelper.loadPlayerPhotoInto(context, player.name, ivPhoto, initial);

        // ফিক্স: new View.OnClickListener ব্যবহার
        edit.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					listener.onEdit(player);
				}
			});

        delete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					listener.onDelete(player.id);
				}
			});

        convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					listener.onProfileClick(player);
				}
			});

        return convertView;
    }

    public static class PlayerModel {
        int id;
        String name;
        String role;
        public PlayerModel(int id, String name, String role) {
            this.id = id;
            this.name = name;
            this.role = role;
        }
    }
}

