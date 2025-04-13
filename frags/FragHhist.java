package org.mintsoft.mintly.frags;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetURL;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.History;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.util.ArrayList;
import java.util.HashMap;

public class FragHhist extends Fragment {
    private int count;
    private Context context;
    private boolean isLive;
    private ImageView emptyView;
    private RecyclerView recyclerView;
    private haAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        View v = inflater.inflate(R.layout.frag_h_hist, container, false);
        if (context == null || getActivity() == null) return v;
        isLive = true;
        recyclerView = v.findViewById(R.id.frag_h_hist_recyclerView);
        emptyView = v.findViewById(R.id.frag_h_hist_emptyView);
        String aTime = Variables.getHash("hist_a_time");
        History.loadAll = aTime == null || System.currentTimeMillis() - Long.parseLong(aTime) > 30000;
        if (History.loadAll) {
            callNet();
        } else {
            initList(Variables.getArrayHash("hist_activity"));
        }
        return v;
    }

    @Override
    public void onDestroy() {
        isLive = false;
        super.onDestroy();
    }

    private void callNet() {
        if (!History.histLoading.isShowing()) History.histLoading.show();
        GetURL.getHistory(context, count, new onResponse() {
            @Override
            public void onSuccessHashListHashMap(HashMap<String, ArrayList<HashMap<String, String>>> hashListHashmap) {
                ArrayList<HashMap<String, String>> list = hashListHashmap.get("activity_history");
                if (count == 0) {
                    Variables.setArrayHash("hist_activity", list);
                    Variables.setArrayHash("hist_game", hashListHashmap.get("game_history"));
                    Variables.setHash("hist_a_time", String.valueOf(System.currentTimeMillis()));
                }
                if (!isLive) return;
                History.histLoading.dismiss();
                if (count == 0) {
                    initList(list);
                } else {
                    adapter.addItems(list);
                }
            }

            @Override
            public void onError(int errorCode, String error) {
                if (!isLive) return;
                History.histLoading.dismiss();
                if (errorCode == -9) {
                    History.hConDiag = Misc.noConnection(History.hConDiag, context, () -> {
                        callNet();
                        History.hConDiag.dismiss();
                    });
                } else {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void initList(ArrayList<HashMap<String, String>> list) {
        if (list.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
            if (adapter == null) {
                adapter = new haAdapter(list);
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }
        }
        if (History.goTo > -1) {
            History.viewPager.setCurrentItem(History.goTo);
            History.goTo = -1;
        }
    }

    private class haAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final int perPage = 10;
        private final LayoutInflater inflaters;
        private final ArrayList<HashMap<String, String>> list;

        haAdapter(ArrayList<HashMap<String, String>> ls) {
            this.list = ls;
            inflaters = LayoutInflater.from(context);
            if (list.size() == perPage) {
                list.add(null);
            }
        }

        public void addItems(ArrayList<HashMap<String, String>> extra) {
            int prv = list.size() - 1;
            list.remove(prv);
            notifyItemRemoved(prv + 1);
            list.addAll(extra);
            if (list.size() % perPage == 0) {
                list.add(null);
            }
            notifyItemRangeInserted(prv, list.size());
        }

        @Override
        public int getItemViewType(int position) {
            return list.get(position) == null ? 1 : 0;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new viewHolder(inflaters.inflate(R.layout.frag_h_hist_item, parent, false));
            } else {
                TextView textView = new TextView(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Misc.dpToPx(context, 46));
                int margin = Misc.dpToPx(context, 5);
                params.setMargins(0, margin, 0, margin + margin);
                textView.setLayoutParams(params);
                textView.setBackgroundResource(R.drawable.rc_blue_6dp);
                textView.setText(DataParse.getStr(context, "load_more", Home.spf));
                textView.setGravity(Gravity.CENTER);
                textView.setAllCaps(true);
                textView.setTextColor(Color.WHITE);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                return new footHolder(textView);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof viewHolder) {
                viewHolder vH = (viewHolder) holder;
                String title = list.get(position).get("title").replace("(", "<font color='#999999'>(").replace(")", ")</font>");
                String firstChar = title.substring(0, 1).toUpperCase();
                vH.iconView.setText(firstChar);
                vH.fromView.setText(Misc.html(firstChar + title.substring(1)));
                vH.amtView.setText(list.get(position).get("amt"));
                vH.dateView.setText(list.get(position).get("date"));
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private class viewHolder extends RecyclerView.ViewHolder {
            TextView iconView, fromView, dateView, amtView;

            public viewHolder(@NonNull View itemView) {
                super(itemView);
                iconView = itemView.findViewById(R.id.frag_h_hist_item_iconView);
                fromView = itemView.findViewById(R.id.frag_h_hist_item_fromView);
                dateView = itemView.findViewById(R.id.frag_h_hist_item_dateView);
                amtView = itemView.findViewById(R.id.frag_h_hist_item_amtView);
            }
        }

        class footHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            public footHolder(@NonNull View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                count++;
                callNet();
            }
        }
    }
}
