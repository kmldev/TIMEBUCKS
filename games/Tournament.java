package org.mintsoft.mintly.games;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetGame;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class Tournament extends BaseAppCompat {
    private Dialog conDiag, loadingDiag, quitDiag;
    private CountDownTimer countDown;
    private RecyclerView recyclerView;
    private tAdapter adapter;
    private String q_id;
    private int a_id;
    private boolean timeup, setRes;
    private Handler handler;
    private Runnable runnable;
    private Snackbar snackbar;
    private TextView timeView, qsView, countView, totalView, marksView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Bundle extras = getIntent().getExtras();
        if (extras == null || Home.gams.contains("to")) {
            finish();
        } else {
            String title = extras.getString("t", null);
            if (title == null) {
                finish();
            } else {
                setContentView(R.layout.game_tour);
                TextView titleView = findViewById(R.id.game_tour_titleView);
                titleView.setText(title);
                TextView ruleView = findViewById(R.id.game_tour_rule);
                ruleView.setText(DataParse.getStr(this,"game_tour_info",Home.spf));
                TextView qsTitle = findViewById(R.id.game_tour_qsTitle);
                qsTitle.setText(DataParse.getStr(this,"question",Home.spf));
                timeView = findViewById(R.id.game_tour_timeView);
                marksView = findViewById(R.id.game_tour_marksView);
                marksView.setText(DataParse.getStr(this,"marks_zero",Home.spf));
                qsView = findViewById(R.id.game_tour_qsView);
                countView = findViewById(R.id.game_tour_countView);
                totalView = findViewById(R.id.game_tour_totalView);
                recyclerView = findViewById(R.id.game_tour_recyclerView);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                adapter = new tAdapter(this, new ArrayList<>());
                recyclerView.setAdapter(adapter);
                loadingDiag = Misc.loadingDiag(this);
                callNet();
                findViewById(R.id.game_tournament_close).setOnClickListener(view -> finish());
                handler = new Handler();
                runnable = this::callNet;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (setRes) {
            showQuitDiag();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (snackbar != null && snackbar.isShown()) snackbar.dismiss();
        if (countDown != null) countDown.cancel();
        if (handler != null) handler.removeCallbacks(runnable);
        super.onDestroy();
    }

    private void callNet() {
        if (!loadingDiag.isShowing()) loadingDiag.show();
        GetGame.tourGet(this, new onResponse() {
            @Override
            public void onSuccessHashMap(HashMap<String, String> data) {
                if (data.get("id").equals(q_id)) {
                    handler.postDelayed(runnable, 2000);
                } else {
                    if (loadingDiag.isShowing()) loadingDiag.dismiss();
                    adapter.changeOptions(data);
                }
            }

            @Override
            public void onError(int errorCode, String error) {
                if (loadingDiag.isShowing()) loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, Tournament.this, () -> {
                        callNet();
                        conDiag.dismiss();
                    });
                } else if (errorCode == 2) {
                    gameFinished(error);
                } else {
                    showSnack(error, 10000);
                }
            }
        });
    }

    private class tAdapter extends RecyclerView.Adapter<tAdapter.ViewHolder> {
        private ArrayList<String> data;
        private final LayoutInflater inflater;
        private final SparseArray<ImageView> imageViews;

        tAdapter(Context context, ArrayList<String> data) {
            this.data = data;
            this.inflater = LayoutInflater.from(context);
            this.imageViews = new SparseArray<>();
        }

        @NonNull
        @Override
        public tAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new tAdapter.ViewHolder(inflater.inflate(R.layout.game_tour_option, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull tAdapter.ViewHolder holder, int position) {
            holder.opView.setText(data.get(position));
            holder.selectionView.setImageResource(R.drawable.ic_mark_inactive);
            imageViews.put(position, holder.selectionView);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            final TextView opView;
            final ImageView selectionView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                opView = itemView.findViewById(R.id.game_tour_option_optionView);
                selectionView = itemView.findViewById(R.id.game_tour_option_selectionView);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                if (setRes) return;
                setRes = true;
                int pos = getAbsoluteAdapterPosition();
                imageViews.get(pos).setImageResource(R.drawable.ic_mark);
                a_id = pos + 1;
                if (timeup) {
                    makeAnswer();
                } else {
                    showSnack(DataParse.getStr(Tournament.this,"wait_for_time_to_finish",Home.spf), 0);
                }
            }
        }

        public void changeOptions(HashMap<String, String> d) {
            data = new ArrayList<>(Arrays.asList(Objects.requireNonNull(d.get("o")).split(";;")));
            q_id = d.get("id");
            a_id = 0;

            //todo: question animation set text etc
            qsView.setText(Misc.html(d.get("q")));
            marksView.setText(("Marks: " + d.get("s")));
            countView.setText(d.get("l"));
            totalView.setText(d.get("c"));

            final LayoutAnimationController controller =
                    AnimationUtils.loadLayoutAnimation(Tournament.this, R.anim.slide_from_right);
            recyclerView.setLayoutAnimation(controller);
            notifyDataSetChanged();
            recyclerView.scheduleLayoutAnimation();
            timeup = false;
            setRes = false;
            setTimer(d.get("t"));
        }
    }

    private void setTimer(String time) {
        if (countDown != null) countDown.cancel();
        countDown = new CountDownTimer(Integer.parseInt(time), 1000) {
            @Override
            public void onTick(long l) {
                timeView.setText(("Time: " + (l / 1000)));
            }

            @Override
            public void onFinish() {
                timeup = true;
                timeView.setText(("Time: 0"));
                if (snackbar != null && snackbar.isShown()) snackbar.dismiss();
                if (setRes) makeAnswer();
            }
        };
        countDown.start();
    }

    private void makeAnswer() {
        if (a_id == 0) {
            callNet();
        } else {
            if (!loadingDiag.isShowing()) loadingDiag.show();
            GetGame.tourAns(this, q_id, a_id, new onResponse() {
                @Override
                public void onSuccess(String response) {
                    callNet();
                }

                @Override
                public void onError(int errorCode, String error) {
                    if (loadingDiag.isShowing()) loadingDiag.dismiss();
                    if (errorCode == -9) {
                        conDiag = Misc.noConnection(conDiag, Tournament.this, () -> {
                            makeAnswer();
                            conDiag.dismiss();
                        });
                    } else if (errorCode == 2) {
                        if (loadingDiag.isShowing()) loadingDiag.dismiss();
                        gameFinished(error);
                    } else {
                        showSnack(error, 10000);
                    }
                }
            });
        }

    }

    private void gameFinished(String msg) {
        setResult(10);
        Misc.showMessage(this, msg, true);
    }

    private void showSnack(String msg, int time) {
        snackbar = Snackbar.make(findViewById(android.R.id.content), msg,
                time == 0 ? BaseTransientBottomBar.LENGTH_INDEFINITE : time)
                .setTextColor(Color.WHITE)
                .setActionTextColor(Color.GRAY);
        snackbar.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        snackbar.show();
    }

    private void showQuitDiag() {
        if (quitDiag == null) {
            quitDiag = Misc.decoratedDiag(this, R.layout.dialog_quit, 0.8f);
            TextView titleV = quitDiag.findViewById(R.id.dialog_quit_title);
            titleV.setText(DataParse.getStr(this, "are_you_sure", Home.spf));
            TextView desc = quitDiag.findViewById(R.id.dialog_quit_desc);
            desc.setText(DataParse.getStr(this,"game_tour_quit_desc",Home.spf));
            quitDiag.findViewById(R.id.dialog_quit_no).setOnClickListener(view -> quitDiag.dismiss());
            quitDiag.findViewById(R.id.dialog_quit_yes).setOnClickListener(view -> {
                quitDiag.dismiss();
                finish();
            });
        }
        quitDiag.show();
    }
}