package org.mintsoft.mintly.games;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetGame;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.util.ArrayList;
import java.util.HashMap;

public class JigsawpuzzleCat extends AppCompatActivity {
    private ArrayList<HashMap<String, String>> list;
    private int score, rank;
    private String hCost;
    private RecyclerView recyclerView;
    private catAdapter adapter;
    private TextView scoreView, rankView, titleView;
    private Dialog conDiag, loadingDiag;
    private ActivityResultLauncher<Intent> activityForResult;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (Home.gams.contains("jp")) {
            finish();
            return;
        }
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        setContentView(R.layout.game_jigsawpuzzle_cat);
        loadingDiag = Misc.loadingDiag(this);
        TextView titleView2 = findViewById(R.id.game_jigsawpuzzle_cat_title2);
        titleView2.setText(DataParse.getStr(this, "app_name", Home.spf));
        Misc.setLogo(this, titleView2);
        TextView header = findViewById(R.id.game_jigsawpuzzle_cat_header);
        header.setText(DataParse.getStr(this, "jpz_cat", Home.spf));
        recyclerView = findViewById(R.id.game_jigsawpuzzle_cat_recyclerView);
        titleView = findViewById(R.id.game_jigsawpuzzle_cat_title);
        scoreView = findViewById(R.id.game_jigsawpuzzle_cat_score);
        rankView = findViewById(R.id.game_jigsawpuzzle_cat_rank);
        activityForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    score += result.getResultCode();
                    scoreView.setText(String.valueOf(score));
                });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        findViewById(R.id.game_jigsawpuzzle_cat_close).setOnClickListener(view -> onBackPressed());
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        list = Variables.getArrayHash("jigsawpuzzle_cat");
        if (list == null) {
            callNet();
        } else {
            score = Integer.parseInt(Variables.getHash("score"));
            rank = Integer.parseInt(Variables.getHash("rank"));
            hCost = Variables.getHash("jigsawpuzzle_hcost");
            initList();
        }
    }

    @Override
    public void onBackPressed() {
        titleView.setVisibility(View.GONE);
        rankView.setVisibility(View.GONE);
        scoreView.setVisibility(View.GONE);
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Variables.getHash("show_offers") != null) finish();
    }

    @Override
    protected void onDestroy() {
        recyclerView.setAdapter(null);
        adapter = null;
        Variables.setArrayHash("jigsawpuzzle_cat", list);
        Variables.setHash("jigsawpuzzle_hcost", hCost);
        Variables.setHash("score", String.valueOf(score));
        Variables.setHash("rank", String.valueOf(rank));
        if (loadingDiag.isShowing()) loadingDiag.dismiss();
        super.onDestroy();
    }

    private void callNet() {
        loadingDiag.show();
        GetGame.getJPZCat(this, new onResponse() {
            @Override
            public void onSuccess(String response) {
                String[] res = response.split(",");
                score = Integer.parseInt(res[0]);
                rank = Integer.parseInt(res[1]);
                hCost = res[2];
            }

            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> lists) {
                super.onSuccessListHashMap(list);
                list = lists;
                initList();
                loadingDiag.dismiss();
            }

            @Override
            public void onError(int errorCode, String error) {
                loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, JigsawpuzzleCat.this, () -> {
                        callNet();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(JigsawpuzzleCat.this, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void initList() {
        new Handler().postDelayed(() -> {
            titleView.setText(DataParse.getStr(this,"your_current_score",Home.spf));
            scoreView.setText(String.valueOf(score));
            rankView.setText((DataParse.getStr(this,"rank_prefix",Home.spf) + " "
                    + rank + " " + DataParse.getStr(this,"rank_suffix",Home.spf)));
        }, 600);
        adapter = new catAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    private class catAdapter extends RecyclerView.Adapter<catAdapter.ViewHolder> {
        private final LayoutInflater mInflater;
        private final Context context;

        catAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.game_puzzles_cat_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.titleView.setText(list.get(position).get("title"));
            holder.sizeView.setText((list.get(position).get("col") + " X " + list.get(position).get("row")));
            holder.scoreView.setText(("Score +" + list.get(position).get("reward") + " for "));
            holder.amtView.setText(list.get(position).get("cost"));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            final TextView sizeView, titleView, scoreView, amtView;

            ViewHolder(View itemView) {
                super(itemView);
                titleView = itemView.findViewById(R.id.game_imagepuzzle_cat_list_titleView);
                sizeView = itemView.findViewById(R.id.game_imagepuzzle_cat_list_sizeView);
                scoreView = itemView.findViewById(R.id.game_imagepuzzle_cat_list_scoreView);
                amtView = itemView.findViewById(R.id.game_imagepuzzle_cat_list_amtView);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                int pos = getAbsoluteAdapterPosition();
                Intent intent = new Intent(context, Jigsawpuzzle.class);
                intent.putExtra("cat", list.get(pos).get("id"));
                intent.putExtra("row", list.get(pos).get("row"));
                intent.putExtra("col", list.get(pos).get("col"));
                intent.putExtra("cost_r", list.get(pos).get("cost"));
                intent.putExtra("cost_p", hCost);
                activityForResult.launch(intent);
            }
        }
    }
}
