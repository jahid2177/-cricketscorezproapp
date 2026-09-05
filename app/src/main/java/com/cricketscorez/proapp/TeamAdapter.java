package com.cricketscorez.proapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

public class TeamAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> teamList;
    private TeamActionListener actionListener;

    public interface TeamActionListener {
        void onEdit(String teamName);
        void onDelete(String teamName);
    }

    public TeamAdapter(Context context, ArrayList<String> teamList, TeamActionListener actionListener) {
        this.context = context;
        this.teamList = teamList;
        this.actionListener = actionListener;
    }

    @Override
    public int getCount() { return teamList.size(); }
    @Override
    public Object getItem(int position) { return teamList.get(position); }
    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.row_team, parent, false);
        }

        TextView tvTeamName = (TextView) convertView.findViewById(R.id.tvTeamName);
        ImageView btnEdit = (ImageView) convertView.findViewById(R.id.btnEdit);
        ImageView btnDelete = (ImageView) convertView.findViewById(R.id.btnDelete);

        final String teamName = teamList.get(position);
        tvTeamName.setText(teamName);

        btnEdit.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					actionListener.onEdit(teamName);
				}
			});

        btnDelete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					actionListener.onDelete(teamName);
				}
			});

        return convertView;
    }
}

