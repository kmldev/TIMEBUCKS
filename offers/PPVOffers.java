package org.mintsoft.mintly.offers;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetURL;
import org.mintsoft.mintlib.PPV;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class PPVOffers extends BaseAppCompat {
    private Dialog loadingDiag;
    private RecyclerView recyclerView;
    private int ps;
    private pvAdapter adapter;
    private ArrayList<HashMap<String, String>> list;
    public static String p_id = null;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.offers_ppv);
        loadingDiag = Misc.loadingDiag(this);
        loadingDiag.show();
        TextView titleView = findViewById(R.id.offers_ppv_title);
        titleView.setText(DataParse.getStr(this,"ppv", Home.spf));
        recyclerView = findViewById(R.id.offers_ppv_recyclerView);
        findViewById(R.id.offers_ppv_back).setOnClickListener(v -> finish());
        GetURL.getPPV(this, new onResponse() {
            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> ls) {
                loadingDiag.dismiss();
                list = ls;
                recyclerView.setLayoutManager(new LinearLayoutManager(PPVOffers.this));
                adapter = new pvAdapter();
                recyclerView.setAdapter(adapter);
                if (!Home.spf.getBoolean("ppvw", false)) {
                    Misc.showMessage(PPVOffers.this, DataParse.getStr(PPVOffers.this,"ppv_warn", Home.spf), false);
                    Home.spf.edit().putBoolean("ppvw", true).apply();
                }
            }

            @Override
            public void onError(int errorCode, String error) {
                loadingDiag.dismiss();
                Toast.makeText(PPVOffers.this, error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        if (p_id != null) {
            if (Objects.requireNonNull(list.get(ps).get("id")).equals(p_id)) {
                p_id = null;
                if (Integer.parseInt(list.get(ps).get("can_visit")) < 2) {
                    list.remove(ps);
                    adapter.notifyItemRemoved(ps);
                    adapter.notifyItemRangeChanged(ps, list.size());
                } else {
                    int ct = Integer.parseInt(list.get(ps).get("can_visit")) - 1;
                    list.get(ps).put("can_visit", String.valueOf(ct));
                    adapter.notifyItemChanged(ps);
                }
                Home.checkBalance = 1;
            } else {
                for (int i = 0; i < list.size(); i++) {
                    if (Objects.requireNonNull(list.get(i).get("id")).equals(p_id)) {
                        p_id = null;
                        if (Integer.parseInt(list.get(i).get("can_visit")) < 2) {
                            list.remove(i);
                            adapter.notifyItemRemoved(i);
                            adapter.notifyItemRangeChanged(i, list.size());
                        } else {
                            int ct = Integer.parseInt(list.get(i).get("can_visit")) - 1;
                            list.get(i).put("can_visit", String.valueOf(ct));
                            adapter.notifyItemChanged(i);
                        }
                        Home.checkBalance = 1;
                        break;
                    }
                }
            }
        }
        super.onResume();
    }

    private class pvAdapter extends RecyclerView.Adapter<pvAdapter.viewHolder> {
        private final String curr;
        private final LayoutInflater inflater;

        pvAdapter() {
            this.inflater = LayoutInflater.from(PPVOffers.this);
            this.curr = Home.currency + "s";
        }

        @NonNull
        @Override
        public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new viewHolder(inflater.inflate(R.layout.offers_ppv_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull viewHolder holder, int position) {
            holder.currView.setText(curr);
            holder.amtView.setText(list.get(position).get("amount"));
            holder.timeView.setText(Misc.html("Surf for <b>" + list.get(position).get("time") + "</b> seconds"));
            holder.titleView.setText(list.get(position).get("title"));
            String vv = list.get(position).get("can_visit");
            if (vv.equals("0")) {
                holder.visitView.setVisibility(View.GONE);
            } else {
                holder.visitView.setVisibility(View.VISIBLE);
                holder.visitView.setText(list.get(position).get("can_visit"));
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private class viewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView currView, timeView, titleView, amtView, visitView;

            public viewHolder(@NonNull View itemView) {
                super(itemView);
                currView = itemView.findViewById(R.id.offers_ppv_item_currView);
                amtView = itemView.findViewById(R.id.offers_ppv_item_amtView);
                timeView = itemView.findViewById(R.id.offers_ppv_item_timeView);
                titleView = itemView.findViewById(R.id.offers_ppv_item_titleView);
                visitView = itemView.findViewById(R.id.offers_ppv_item_visitView);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int position = getAbsoluteAdapterPosition();
                ps = position;
                Intent intent = new Intent(PPVOffers.this, PPV.class);
                intent.putExtra("id", list.get(position).get("id"));
                intent.putExtra("url", list.get(position).get("url"));
                intent.putExtra("time", list.get(position).get("time"));
                intent.putExtra("title", list.get(position).get("title"));
                startActivity(intent);
            }
        }
    }
}
