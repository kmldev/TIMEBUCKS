package org.mintsoft.mintly.frags;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetGame;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.games.GameList;
import org.mintsoft.mintly.games.GuessWord;
import org.mintsoft.mintly.games.ImagepuzzleCat;
import org.mintsoft.mintly.games.JigsawpuzzleCat;
import org.mintsoft.mintly.games.Lotto;
import org.mintsoft.mintly.games.QuizCat;
import org.mintsoft.mintly.games.ScratcherCat;
import org.mintsoft.mintly.games.Tournament;
import org.mintsoft.mintly.games.TournamentRes;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;
import org.mintsoft.mintly.helper.arAdapter;
import org.mintsoft.mintly.helper.htmlAdapter;
import org.mintsoft.mintly.offers.TaskOffers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class FragMain extends Fragment {
    private Context context;
    private Activity activity;
    private View v;
    private boolean isLive;
    private long pubTime;
    private int joinTour, requestCode;
    private CountDownTimer pubTimer, countDown;
    private CardView tourHolder;
    private TextView tourBtnText;
    private LinearLayout goQuiz, goGW, goIP, goJPZ;
    private ImageView tourEnrol, goLotto, goScratcher;
    private Dialog lowBalDiag;
    private RecyclerView gridView;
    private arAdapter aradapter;
    private ActivityResultLauncher<Intent> activityForResult;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        activity = getActivity();
        v = inflater.inflate(R.layout.frag_main, container, false);
        if (context == null || activity == null) return v;
        isLive = true;
        String tour = Home.spf.getString("tour", null);
        tourHolder = v.findViewById(R.id.frag_main_tour_holder);
        goQuiz = v.findViewById(R.id.frag_main_go_quiz);
        goGW = v.findViewById(R.id.frag_main_go_guess_word);
        goIP = v.findViewById(R.id.frag_main_go_imagepuzzle);
        goJPZ = v.findViewById(R.id.frag_main_go_jigsawpuzzle);
        goLotto = v.findViewById(R.id.frag_main_go_lotto);
        goScratcher = v.findViewById(R.id.frag_main_go_scratcher);
        gridView = v.findViewById(R.id.frag_main_gridView);
        activityForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    if (requestCode == 97) {
                        if (resultCode == 10) tourHolder.setVisibility(View.GONE);
                    } else if (resultCode == 18) {
                        aradapter.done();
                        aradapter.getImageView().setImageResource(R.drawable.reward_done);
                    }
                    requestCode = 0;
                });
        if (tour == null) {
            tourHolder.setVisibility(View.GONE);
        } else {
            String[] strs = tour.split(";");
            if (strs[0].equals("rs")) {
                if (strs[1].equals("-1")) {
                    showResultBtn();
                } else {
                    tourHolder.removeAllViews();
                    tourHolder.setBackground(null);
                    pubTime = Long.parseLong(strs[1]);
                    String dt = new SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(new Date(pubTime + System.currentTimeMillis()));
                    View penView = LayoutInflater.from(context).inflate(R.layout.frag_main_result_pending, null, false);
                    penView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    TextView pTitleView = penView.findViewById(R.id.frag_main_result_pending_title);
                    pTitleView.setText(DataParse.getStr(context, "result_pub_on", Home.spf));
                    TextView timeView = penView.findViewById(R.id.frag_main_result_pending_time);
                    timeView.setText(dt);
                    tourHolder.addView(penView);
                    pubTimer = new CountDownTimer(pubTime, pubTime) {
                        @Override
                        public void onTick(long l) {

                        }

                        @Override
                        public void onFinish() {
                            showResultBtn();
                        }
                    };
                    pubTimer.start();
                }
            } else {
                TextView tourTitleView = v.findViewById(R.id.frag_main_tour_titleView);
                TextView tourFeeView = v.findViewById(R.id.frag_main_tour_feeView);
                TextView tourRewardView = v.findViewById(R.id.frag_main_tour_rewardView);
                TextView tourTimeView = v.findViewById(R.id.frag_main_tour_timeView);
                TextView tourDayView = v.findViewById(R.id.frag_main_tour_dateView);
                tourBtnText = v.findViewById(R.id.frag_main_tour_enrollText);
                tourBtnText.setText(DataParse.getStr(context, "enroll_now", Home.spf));
                tourTitleView.setText(strs[0]);
                tourFeeView.setText(Misc.html("<b>" + DataParse.getStr(context, "enroll_fee", Home.spf) + "</b>"
                        + " " + strs[1] + " " + Home.currency.toLowerCase() + "s"
                ));
                tourRewardView.setText(Misc.html("<b>" + DataParse.getStr(context, "total_prize", Home.spf) + "</b>"
                        + " " + strs[2] + " " + Home.currency.toLowerCase() + "s"
                ));
                tourEnrol = v.findViewById(R.id.frag_main_tour_enroll);
                if (strs.length > 4 && strs[4].equals("1")) {
                    enrolled();
                }
                tourEnrol.setOnClickListener(view -> {
                    if (joinTour == 2) {
                        requestCode = 97;
                        activityForResult.launch(new Intent(context, Tournament.class).putExtra("t", strs[0]));
                    } else if (joinTour == 0) {
                        tourEnroll();
                    }
                });
                tourTime(tourTimeView, tourDayView, strs[3]);
            }
        }

        TextView g1Title = v.findViewById(R.id.frag_main_games_1_titleView);
        g1Title.setText(DataParse.getStr(context, "games_1", Home.spf));
        TextView g2Title = v.findViewById(R.id.frag_main_games_2_titleView);
        g2Title.setText(DataParse.getStr(context, "games_2", Home.spf));
        TextView g3Title = v.findViewById(R.id.frag_main_html5_all_title);
        g3Title.setText(DataParse.getStr(context, "games_3", Home.spf));
        TextView qzTitle = v.findViewById(R.id.frag_main_title_quiz);
        qzTitle.setText(DataParse.getStr(context, "quiz_game", Home.spf));
        TextView ipTitle = v.findViewById(R.id.frag_main_title_ip);
        ipTitle.setText(DataParse.getStr(context, "imagepuzzle", Home.spf));
        TextView gwTitle = v.findViewById(R.id.frag_main_title_gw);
        gwTitle.setText(DataParse.getStr(context, "word_guessing", Home.spf));
        TextView jpTitle = v.findViewById(R.id.frag_main_title_jp);
        jpTitle.setText(DataParse.getStr(context, "jpz", Home.spf));
        TextView scTitle = v.findViewById(R.id.frag_main_title_sc);
        scTitle.setText(DataParse.getStr(context, "scratch_n_win", Home.spf));
        TextView loTitle = v.findViewById(R.id.frag_main_title_lo);
        loTitle.setText(DataParse.getStr(context, "lotto", Home.spf));
        TextView loadTitle = v.findViewById(R.id.frag_main_load_more);
        loadTitle.setText(DataParse.getStr(context, "load_more_desc", Home.spf));
        TextView loadBTitle = v.findViewById(R.id.frag_main_html5_all_btn);
        loadBTitle.setText(DataParse.getStr(context, "load_more", Home.spf));
        ArrayList<HashMap<String, String>> listH = Variables.getArrayHash("home_html");
        if (listH == null) {
            GetGame.getHtml(context, "6", new onResponse() {
                @Override
                public void onSuccessListHashMap(ArrayList<HashMap<String, String>> listH2) {
                    Variables.setArrayHash("home_html", listH2);
                    if (!isLive) return;
                    initHtml(listH2);
                }
            });
        } else {
            initHtml(listH);
        }
        checkActivityReward();
        initListeners();
        return v;
    }

    @Override
    public void onDestroy() {
        isLive = false;
        if (countDown != null) countDown.cancel();
        if (pubTimer != null) pubTimer.cancel();
        super.onDestroy();
    }

    private void initListeners() {
        if (Home.gams.contains("to")) {
            tourHolder.setVisibility(View.GONE);
        }
        if (Home.gams.contains("qz")) {
            v.findViewById(R.id.frag_main_go_quiz_holder).setVisibility(View.GONE);
        } else {
            goQuiz.setOnClickListener(view -> {
                Intent intent = new Intent(context, QuizCat.class);
                startTransition(intent, goQuiz);
            });
        }
        if (Home.gams.contains("gw")) {
            v.findViewById(R.id.frag_main_go_guess_word_holder).setVisibility(View.GONE);
        } else {
            goGW.setOnClickListener(view -> {
                Intent intent = new Intent(context, GuessWord.class);
                startTransition(intent, goGW);
            });
        }
        if (Home.gams.contains("ip")) {
            v.findViewById(R.id.frag_main_go_imagepuzzle_holder).setVisibility(View.GONE);
        } else {
            goIP.setOnClickListener(view -> {
                Intent intent = new Intent(context, ImagepuzzleCat.class);
                startTransition(intent, goIP);
            });
        }
        if (Home.gams.contains("jp")) {
            v.findViewById(R.id.frag_main_go_jigsawpuzzle_holder).setVisibility(View.GONE);
        } else {
            goJPZ.setOnClickListener(view -> {
                Intent intent = new Intent(context, JigsawpuzzleCat.class);
                startTransition(intent, goJPZ);
            });
        }
        if (Home.gams.contains("lo")) {
            v.findViewById(R.id.frag_main_go_lotto_holder).setVisibility(View.GONE);
        } else {
            goLotto.setOnClickListener(view -> {
                Intent intent = new Intent(context, Lotto.class);
                startActivity(intent);
            });
        }
        if (Home.gams.contains("sc")) {
            v.findViewById(R.id.frag_main_go_scratcher_holder).setVisibility(View.GONE);
        } else {
            goScratcher.setOnClickListener(view -> {
                Intent intent = new Intent(context, ScratcherCat.class);
                startActivity(intent);
            });
        }
        View goTasks = v.findViewById(R.id.frag_main_go_tasks);
        if (Home.tasks) {
            goTasks.setVisibility(View.VISIBLE);
            goTasks.setOnClickListener(view -> startActivity(new Intent(context, TaskOffers.class)));
        } else {
            goTasks.setVisibility(View.GONE);
        }
    }

    private void startTransition(Intent intent, View v) {
        ActivityOptionsCompat activityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, v, v.getTransitionName());
        startActivity(intent, activityOptionsCompat.toBundle());
    }

    private void showResultBtn() {
        tourHolder.removeAllViews();
        tourHolder.setBackground(null);
        View pubView = LayoutInflater.from(context).inflate(R.layout.frag_main_result_published, null, false);
        pubView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pubView.findViewById(R.id.frag_main_result_published_btn).setOnClickListener(view ->
                startActivity(new Intent(context, TournamentRes.class))
        );
        TextView pubTitle = pubView.findViewById(R.id.frag_main_result_published_title);
        pubTitle.setText(DataParse.getStr(context, "result_published", Home.spf));
        TextView pubCheck = pubView.findViewById(R.id.frag_main_result_published_check);
        pubCheck.setText(DataParse.getStr(context, "click_check", Home.spf));
        tourHolder.addView(pubView);
    }

    private void enrolled() {
        joinTour = 1;
        tourBtnText.setText(DataParse.getStr(context, "enrolled", Home.spf));
        tourBtnText.setAlpha(0.5f);
        tourEnrol.setAlpha(0.3f);
    }

    private void tourEnroll() {
        joinTour = 1;
        tourBtnText.setText(DataParse.getStr(context, "please_wait", Home.spf));
        GetGame.tourEnroll(context, new onResponse() {
            @Override
            public void onSuccess(String response) {
                if (!isLive) return;
                if (response.equals("2")) {
                    enrolled();
                    Toast.makeText(context, DataParse.getStr(context, "enrolled_already", Home.spf), Toast.LENGTH_LONG).show();
                } else if (response.equals("1")) {
                    enrolled();
                }
            }

            @Override
            public void onLowCredit() {
                if (!isLive) return;
                joinTour = 0;
                tourBtnText.setText(DataParse.getStr(context, "enroll_now", Home.spf));
                tourBtnText.setAlpha(1.0f);
                if (lowBalDiag == null) {
                    lowBalDiag = Misc.lowbalanceDiag(activity, new Misc.yesNo() {
                        @Override
                        public void yes() {
                            lowBalDiag.dismiss();
                            ((Home) activity).changeFrag(1);
                        }

                        @Override
                        public void no() {
                            lowBalDiag.dismiss();
                        }
                    });
                }
                lowBalDiag.show();
            }

            @Override
            public void onError(int errorCode, String error) {
                if (!isLive) return;
                joinTour = 0;
                tourBtnText.setText(DataParse.getStr(context, "enroll_now", Home.spf));
                tourBtnText.setAlpha(1.0f);
                Toast.makeText(context, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void tourTime(TextView tv, TextView dv, String time) {
        countDown = new CountDownTimer(Long.parseLong(time) - System.currentTimeMillis(), 1000) {
            long day;

            @Override
            public void onTick(long l) {
                long t = l / 1000;
                long s = t % 60;
                long m = (t / 60) % 60;
                long h = (t / (60 * 60)) % 24;
                long d = Math.abs(t / (60 * 60 * 24));
                if (d != day) {
                    day = d;
                    dv.setText(("In " + d + " days"));
                }
                tv.setText(String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s));
            }

            @Override
            public void onFinish() {
                if (joinTour == 1) {
                    joinTour = 2;
                    tourBtnText.setText(DataParse.getStr(context, "lets_start", Home.spf));
                    tourBtnText.setAlpha(1.0f);
                    tourEnrol.setAlpha(0.8f);
                } else {
                    tourBtnText.setText(DataParse.getStr(context, "ongoing", Home.spf));
                    tourBtnText.setAlpha(0.5f);
                    tourEnrol.setOnClickListener(null);
                    tourEnrol.setAlpha(0.1f);
                }
                dv.setVisibility(View.INVISIBLE);
                tv.setText(DataParse.getStr(context, "on_live", Home.spf));
                //tv.setPadding(0, 0, 0, 30);
                tv.setTextColor(ContextCompat.getColor(context, R.color.red_1));
                //tv.startAnimation(blink);
                countDown = null;
            }
        };
        countDown.start();
    }

    private void checkActivityReward() {
        final RecyclerView arView = v.findViewById(R.id.frag_main_ar_recyclerView);
        arView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        ArrayList<HashMap<String, String>> listA = Variables.getArrayHash("home_ar");
        if (listA == null) {
            GetGame.activityReward(context, new onResponse() {
                @Override
                public void onSuccessListHashMap(ArrayList<HashMap<String, String>> listA2) {
                    ArrayList<HashMap<String, String>> listA4 = new ArrayList<>(listA2);
                    Variables.setArrayHash("home_ar", listA4);
                    if (!isLive) return;
                    initAr(listA2, arView);
                }

                @Override
                public void onError(int errorCode, String error) {
                    if (!isLive) return;
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            ArrayList<HashMap<String, String>> listA5 = new ArrayList<>(listA);
            initAr(listA5, arView);
        }
    }

    private void initHtml(ArrayList<HashMap<String, String>> list) {
        View vAll = v.findViewById(R.id.frag_main_html5_all);
        if (list.size() == 0) {
            v.findViewById(R.id.frag_main_html5_all_title).setVisibility(View.GONE);
            v.findViewById(R.id.frag_main_html5_all_titleLine).setVisibility(View.GONE);
            vAll.setVisibility(View.GONE);
        } else {
            if (list.size() < 6) {
                vAll.setVisibility(View.GONE);
            } else {
                vAll.setVisibility(View.VISIBLE);
            }
            v.findViewById(R.id.frag_main_html5_all_btn).setOnClickListener(view ->
                    startActivity(new Intent(context, GameList.class))
            );
            htmlAdapter adapter = new htmlAdapter(context, list, R.layout.frag_main_item, 6);
            GridLayoutManager manager = new GridLayoutManager(context, 2);
            manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return adapter.getItemViewType(position) == htmlAdapter.VIEW_TYPE_ITEM ? 1 : 2;
                }
            });
            gridView.setLayoutManager(manager);
            gridView.setAdapter(adapter);
        }
    }

    private void initAr(ArrayList<HashMap<String, String>> listA3, RecyclerView arView) {
        final TextView arViewTitle = v.findViewById(R.id.frag_main_ar_titleView);
        arViewTitle.setText(DataParse.getStr(context, "activity_reward", Home.spf));
        final View arViewTitleLine = v.findViewById(R.id.frag_main_ar_titleViewLine);
        int activeReward = Integer.parseInt(Objects.requireNonNull(listA3.get(0).get("current")));
        int isDone = Integer.parseInt(Objects.requireNonNull(listA3.get(0).get("is_done")));
        listA3.remove(0);
        listA3.remove(1);
        if (activeReward + isDone >= listA3.size()) {
            arViewTitle.setVisibility(View.GONE);
            arViewTitleLine.setVisibility(View.GONE);
            arView.setVisibility(View.GONE);
        } else {
            arViewTitle.setVisibility(View.VISIBLE);
            arViewTitleLine.setVisibility(View.VISIBLE);
            arView.setVisibility(View.VISIBLE);
            aradapter = new arAdapter(context, listA3, activeReward, isDone, activityForResult);
            arView.setAdapter(aradapter);
        }
    }
}
