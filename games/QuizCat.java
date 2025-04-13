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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
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

public class QuizCat extends BaseAppCompat {
    private ArrayList<HashMap<String, String>> list;
    private int score, rank;
    private RecyclerView recyclerView;
    private catAdapter adapter;
    private TextView scoreView, rankView, titleView;
    private Dialog conDiag;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (Home.gams.contains("qz")) {
            finish();
            return;
        }
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        setContentView(R.layout.game_quiz_cat);
        TextView titleView2 = findViewById(R.id.game_quiz_cat_title2);
        titleView2.setText(DataParse.getStr(this, "app_name", Home.spf));
        Misc.setLogo(this, titleView2);
        TextView header = findViewById(R.id.game_quiz_cat_header);
        header.setText(DataParse.getStr(this, "quiz_cat", Home.spf));
        recyclerView = findViewById(R.id.game_quiz_cat_recyclerView);
        titleView = findViewById(R.id.game_quiz_cat_title);
        scoreView = findViewById(R.id.game_quiz_cat_score);
        rankView = findViewById(R.id.game_quiz_cat_rank);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        findViewById(R.id.game_quiz_cat_close).setOnClickListener(view -> onBackPressed());
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        list = Variables.getArrayHash("quiz_cat");
        if (list == null) {
            callNet();
        } else {
            score = Integer.parseInt(Variables.getHash("score"));
            rank = Integer.parseInt(Variables.getHash("rank"));
            initList();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 141) {
            score += resultCode;
            scoreView.setText(String.valueOf(score));
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
    protected void onDestroy() {
        recyclerView.setAdapter(null);
        adapter = null;
        Variables.setArrayHash("quiz_cat", list);
        Variables.setHash("score", String.valueOf(score));
        Variables.setHash("rank", String.valueOf(rank));
        super.onDestroy();
    }

    private void callNet() {
        GetGame.getQuizCat(this, new onResponse() {
            @Override
            public void onSuccess(String response) {
                String[] res = response.split(",");
                score = Integer.parseInt(res[0]);
                rank = Integer.parseInt(res[1]);
            }

            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> lists) {
                list = lists;
                initList();
            }

            @Override
            public void onError(int errorCode, String error) {
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, QuizCat.this, () -> {
                        callNet();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(QuizCat.this, error, Toast.LENGTH_LONG).show();
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
            View view = mInflater.inflate(R.layout.game_quiz_cat_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.titleView.setText(list.get(position).get("title"));
            holder.descView.setText(list.get(position).get("desc"));
            Picasso.get().load(list.get(position).get("image"))
                    .placeholder(R.drawable.anim_loading)
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            final TextView titleView, descView;
            final ImageView imageView;

            ViewHolder(View itemView) {
                super(itemView);
                titleView = itemView.findViewById(R.id.game_quiz_cat_list_title);
                descView = itemView.findViewById(R.id.game_quiz_cat_list_desc);
                imageView = itemView.findViewById(R.id.game_quiz_cat_list_image);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(context, Quiz.class)
                        .putExtra("cat", list.get(getAbsoluteAdapterPosition()).get("id")), 141);
            }
        }
    }
}
