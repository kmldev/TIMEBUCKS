package org.mintsoft.mintly.games;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetGame;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.util.ArrayList;
import java.util.HashMap;

public class TournamentRes extends BaseAppCompat {
    private String yData, rewards, marks;
    private Dialog loadingDiag, conDiag;
    private TextView rym, ryc, ryrw, ryr;
    private String[] rankReward;
    private final TextView[] pRank = new TextView[9];
    private final ImageView[] pRankI = new ImageView[3];
    private ArrayList<HashMap<String, String>> list;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.game_tour_result);
        TextView titleView = findViewById(R.id.game_tour_result_title);
        titleView.setText(DataParse.getStr(this,"tour_result", Home.spf));
        TextView winnerTitle = findViewById(R.id.game_tour_result_winner);
        winnerTitle.setText(DataParse.getStr(this,"winner", Home.spf));
        TextView urPosTitle = findViewById(R.id.game_tour_result_urpos_title);
        urPosTitle.setText(DataParse.getStr(this,"you_pos_prefix", Home.spf));
        TextView bcrTitle = findViewById(R.id.game_tour_result_bcr);
        bcrTitle.setText(DataParse.getStr(this,"you_pos_suffix", Home.spf));
        TextView corrTitle = findViewById(R.id.game_tour_result_correct);
        corrTitle.setText(DataParse.getStr(this,"correct", Home.spf));
        TextView marksTitle = findViewById(R.id.game_tour_result_markstitle);
        marksTitle.setText(DataParse.getStr(this,"marks", Home.spf));
        TextView nameT = findViewById(R.id.game_tour_result_nameT);
        nameT.setText(DataParse.getStr(this,"name", Home.spf));
        TextView marksT = findViewById(R.id.game_tour_result_marksT);
        marksT.setText(DataParse.getStr(this,"marks", Home.spf));
        TextView rewardT = findViewById(R.id.game_tour_result_rewardT);
        rewardT.setText(DataParse.getStr(this,"reward", Home.spf));
        findViewById(R.id.game_tour_result_close).setOnClickListener(view -> finish());
        pRank[0] = findViewById(R.id.game_tour_result_r1nView);
        pRank[1] = findViewById(R.id.game_tour_result_r2nView);
        pRank[2] = findViewById(R.id.game_tour_result_r3nView);
        pRank[3] = findViewById(R.id.game_tour_result_r1rView);
        pRank[4] = findViewById(R.id.game_tour_result_r2rView);
        pRank[5] = findViewById(R.id.game_tour_result_r3rView);
        pRank[6] = findViewById(R.id.game_tour_result_r1mView);
        pRank[7] = findViewById(R.id.game_tour_result_r2mView);
        pRank[8] = findViewById(R.id.game_tour_result_r3mView);
        pRankI[0] = findViewById(R.id.game_tour_result_r1aView);
        pRankI[1] = findViewById(R.id.game_tour_result_r2aView);
        pRankI[2] = findViewById(R.id.game_tour_result_r3aView);
        rym = findViewById(R.id.game_tour_result_rymView);
        ryc = findViewById(R.id.game_tour_result_rycView);
        ryrw = findViewById(R.id.game_tour_result_ryrwView);
        ryr = findViewById(R.id.game_tour_result_ryrView);
        marks = " " + DataParse.getStr(this,"marks",Home.spf).toLowerCase();
        list = Variables.getArrayHash("tournamantres_list");
        if (list == null) {
            callNet();
        } else {
            rewards = Variables.getHash("tournamantres_rewards");
            yData = Variables.getHash("tournamantres_ydata");
            initList();
        }
    }

    @Override
    protected void onDestroy() {
        Variables.setHash("tournamantres_ydata", yData);
        Variables.setHash("tournamantres_rewards", rewards);
        Variables.setArrayHash("tournamantres_list", list);
        super.onDestroy();
    }

    private void callNet() {
        if (loadingDiag == null) loadingDiag = Misc.loadingDiag(this);
        loadingDiag.show();
        GetGame.tourRank(this, new onResponse() {
            @Override
            public void onSuccessHashMap(HashMap<String, String> data) {
                loadingDiag.cancel();
                yData = data.get("y");
                String reward = data.get("r");
                if (reward.endsWith(",")) {
                    rewards = reward.substring(0, reward.length() - 1);
                } else {
                    rewards = reward;
                }
            }

            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> l) {
                list = l;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initList();
                    }
                }, 1000);
            }

            @Override
            public void onError(int errorCode, String error) {
                loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, TournamentRes.this, () -> {
                        callNet();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(TournamentRes.this, error, Toast.LENGTH_LONG).show();
                    if (errorCode == 2) finish();
                }
            }
        });
    }

    private void initList() {
        rankReward = rewards.split(",");
        String[] yList = yData.split(";");
        ryr.setText(yList[0]);
        ryc.setText(yList[1]);
        rym.setText((yList[2]));
        int yrank = Integer.parseInt(yList[0]);
        if (yrank != 0 && yrank <= list.size()) {
            ryrw.setText(rankReward[yrank - 1]);
        }
        int sz = Math.min(list.size(), 3);
        for (int i = 0; i < sz; i++) {
            pRank[i].setText(list.get(i).get("n"));
            pRank[i + 3].setText(rankReward[i]);
            pRank[i + 6].setText((list.get(i).get("m") + marks));
            Picasso.get().load(list.get(i).get("a")).into(pRankI[i]);
        }
        RecyclerView recyclerView = findViewById(R.id.game_tour_result_recyclerView);
        if (list.size() > 3) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new wAdapter());
        } else {
            TextView emptyView = findViewById(R.id.game_tour_result_emptyView);
            emptyView.setText(DataParse.getStr(this,"no_more_rank",Home.spf));
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    private class wAdapter extends RecyclerView.Adapter<wAdapter.ViewHolder> {
        private final LayoutInflater inflater;

        wAdapter() {
            inflater = LayoutInflater.from(TournamentRes.this);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(inflater.inflate(R.layout.game_tour_result_list, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int pos = position + 3;
            holder.rankView.setText(String.valueOf(pos + 1));
            holder.nameView.setText(list.get(pos).get("n"));
            holder.marksView.setText(list.get(pos).get("m"));
            if (rankReward[position] != null) {
                holder.rewardView.setText(rankReward[position]);
            }
        }

        @Override
        public int getItemCount() {
            return list.size() - 4;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView rankView, nameView, rewardView, marksView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                rankView = itemView.findViewById(R.id.game_tour_result_list_rankView);
                nameView = itemView.findViewById(R.id.game_tour_result_list_nameView);
                marksView = itemView.findViewById(R.id.game_tour_result_list_marksView);
                rewardView = itemView.findViewById(R.id.game_tour_result_list_rwdView);
            }
        }
    }
}
