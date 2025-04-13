package org.mintsoft.mintly.frags;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.util.ArrayList;
import java.util.HashMap;

public class FragHgame extends Fragment {
    private ArrayList<HashMap<String, String>> list;
    private LayoutInflater inflaters;
    private hgAdapter adapter;
    private ImageView emptyView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = getContext();
        View v = inflater.inflate(R.layout.frag_h_hist, container, false);
        if (context == null || getActivity() == null) return v;
        RecyclerView recyclerView = v.findViewById(R.id.frag_h_hist_recyclerView);
        emptyView = v.findViewById(R.id.frag_h_hist_emptyView);
        list = Variables.getArrayHash("hist_game");
        if (list == null || list.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            inflaters = LayoutInflater.from(context);
            adapter = new hgAdapter();
            recyclerView.setAdapter(adapter);
        }
        return v;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null && adapter.getItemCount() == 0) {
            list = Variables.getArrayHash("hist_game");
            if (list == null || list.size() == 0) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private class hgAdapter extends RecyclerView.Adapter<hgAdapter.viewHolder> {

        @NonNull
        @Override
        public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new viewHolder(inflaters.inflate(R.layout.frag_h_hist_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull viewHolder holder, int position) {
            String title = list.get(position).get("title");
            String firstChar = title.substring(0, 1).toUpperCase();
            holder.iconView.setText(firstChar);
            holder.fromView.setText(Misc.html(firstChar + title.substring(1)));
            String amt = list.get(position).get("amt");
            if (amt.equals("0")) {
                holder.amtHolder.setVisibility(View.GONE);
                holder.ptsView.setText(list.get(position).get("pts") + " RP");
            } else {
                holder.amtHolder.setVisibility(View.VISIBLE);
                holder.amtView.setText(amt);
            }
            holder.dateView.setText(list.get(position).get("date"));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class viewHolder extends RecyclerView.ViewHolder {
            LinearLayout amtHolder;
            TextView iconView, fromView, dateView, amtView, ptsView;

            public viewHolder(@NonNull View itemView) {
                super(itemView);
                iconView = itemView.findViewById(R.id.frag_h_hist_item_iconView);
                fromView = itemView.findViewById(R.id.frag_h_hist_item_fromView);
                dateView = itemView.findViewById(R.id.frag_h_hist_item_dateView);
                amtView = itemView.findViewById(R.id.frag_h_hist_item_amtView);
                amtHolder = itemView.findViewById(R.id.frag_h_hist_item_amtHolder);
                ptsView = itemView.findViewById(R.id.frag_h_hist_item_ptsView);
            }
        }
    }
}
