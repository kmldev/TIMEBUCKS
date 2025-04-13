package org.mintsoft.mintly.games;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetGame;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;
import org.mintsoft.mintly.offers.GlobalAds;

import java.util.HashMap;

public class GuessWord extends AppCompatActivity {
    private LinearLayout fromGrid, toGrid;
    private View holder, hintBtn, solveBtn;
    private HashMap<String, String> fromData;
    private int filled, retryCost, hintCost, solveCost;
    private String cc;
    private ImageView imageView;
    private CountDownTimer countDown;
    private ProgressBar timeProgress;
    private Dialog loadingDiag, successDiag, failedDiag, lowBalDiag, quitDiag, conDiag;
    private boolean isAnimating, isLocked;
    private TextView scoreView, hintView, retryView, infoView, wrongView, solveView;
    private SparseArray<TextView> fromTv, toTv;
    private Button nextQs;
    private final Animation alphaAnim = Misc.alphaAnim();
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (Home.gams.contains("gw")) {
            finish();
            return;
        }
        Window window = getWindow();
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#a37e1f"));
        setContentView(R.layout.game_guess_word);
        TextView titleView = findViewById(R.id.game_guess_word_title);
        titleView.setText(DataParse.getStr(this, "word_guessing", Home.spf));
        TextView scoreTitle = findViewById(R.id.game_guess_word_scoreTitle);
        scoreTitle.setText(DataParse.getStr(this, "score", Home.spf));
        TextView hinTitle = findViewById(R.id.game_guess_word_hintTitle);
        hinTitle.setText(DataParse.getStr(this, "gw_hints", Home.spf));
        TextView retryTitle = findViewById(R.id.game_guess_word_retryTitle);
        retryTitle.setText(DataParse.getStr(this, "gw_retry", Home.spf));
        TextView spendView = findViewById(R.id.game_guess_word_spend);
        spendView.setText(DataParse.getStr(this, "spend", Home.spf));
        TextView solveIt = findViewById(R.id.game_guess_word_solveIt);
        solveIt.setText(DataParse.getStr(this, "to_solve_it", Home.spf));
        holder = findViewById(R.id.game_guess_word_holder);
        holder.setVisibility(View.INVISIBLE);
        fromGrid = findViewById(R.id.game_guess_word_fromGrid);
        toGrid = findViewById(R.id.game_guess_word_toGrid);
        timeProgress = findViewById(R.id.game_guess_word_progress);
        scoreView = findViewById(R.id.game_guess_word_scoreView);
        scoreView.setText("0");
        hintView = findViewById(R.id.game_guess_word_hintView);
        hintView.setText("0");
        retryView = findViewById(R.id.game_guess_word_retryView);
        retryView.setText("0");
        solveView = findViewById(R.id.game_guess_word_solveView);
        solveView.setText("0");
        imageView = findViewById(R.id.game_guess_word_imageView);
        infoView = findViewById(R.id.game_guess_word_infoView);
        wrongView = findViewById(R.id.game_guess_word_wrongView);
        wrongView.setText(DataParse.getStr(this, "wrong_gw", Home.spf));
        nextQs = findViewById(R.id.game_guess_word_next);
        nextQs.setText(DataParse.getStr(this, "next_question", Home.spf));
        hintBtn = findViewById(R.id.game_guess_word_letter_help);
        solveBtn = findViewById(R.id.game_guess_word_solve_help);
        solveBtn.setVisibility(View.GONE);
        cc = Home.spf.getString("cc", null);
        loadingDiag = Misc.loadingDiag(this);
        GlobalAds.fab(this, "fab_wg");
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        findViewById(R.id.game_guess_word_close).setOnClickListener(view -> onBackPressed());
        handler = new Handler();
        runnable = () -> hintBtn.startAnimation(alphaAnim);
        new Handler().postDelayed(this::callOnce, 600);

    }

    private void callOnce() {
        if (loadingDiag != null && !loadingDiag.isShowing()) loadingDiag.show();
        GetGame.getGWInfo(GuessWord.this, new onResponse() {
            @Override
            public void onSuccessHashMap(HashMap<String, String> data) {
                updateScore(data.get("score"));
                updateRetry(data.get("retry_chance"));
                updateHint(data.get("hint_chance"));
                String sCost = data.get("solve_cost");
                holder.setVisibility(View.VISIBLE);
                solveView.setText(sCost);
                retryCost = Integer.parseInt(data.get("retry_cost"));
                hintCost = Integer.parseInt(data.get("hint_cost"));
                solveCost = Integer.parseInt(sCost);
                callNet();

                if (successDiag == null) {
                    successDiag = Misc.decoratedDiag(GuessWord.this, R.layout.dialog_quiz_post, 0.8f);
                    TextView wellDone = successDiag.findViewById(R.id.dialog_quiz_post_title);
                    wellDone.setText(DataParse.getStr(GuessWord.this, "well_done", Home.spf));
                    TextView successDesc = successDiag.findViewById(R.id.dialog_quiz_post_desc);
                    successDesc.setText(DataParse.getStr(GuessWord.this, "quiz_correct_popup", Home.spf));
                    Button quitTxt = successDiag.findViewById(R.id.dialog_quiz_post_quit);
                    quitTxt.setText(DataParse.getStr(GuessWord.this, "quit", Home.spf));
                    Button nextTxt = successDiag.findViewById(R.id.dialog_quiz_post_next);
                    nextTxt.setText(DataParse.getStr(GuessWord.this, "next_question", Home.spf));
                }
                successDiag.findViewById(R.id.dialog_quiz_post_quit).setOnClickListener(view -> {
                    successDiag.dismiss();
                    onBackPressed();
                });
                successDiag.findViewById(R.id.dialog_quiz_post_next).setOnClickListener(view -> {
                    successDiag.dismiss();
                    callNet();
                });


                if (failedDiag == null) {
                    failedDiag = Misc.decoratedDiag(GuessWord.this, R.layout.dialog_quiz_post, 0.8f);
                }
                ImageView failedIcon = failedDiag.findViewById(R.id.dialog_quiz_post_icon);
                failedIcon.setImageResource(R.drawable.ic_wrong);
                TextView failedTitle = failedDiag.findViewById(R.id.dialog_quiz_post_title);
                failedTitle.setText(DataParse.getStr(GuessWord.this, "timeup", Home.spf));
                failedTitle.setTextColor(ContextCompat.getColor(GuessWord.this, R.color.red_2));
                TextView failedDesc = failedDiag.findViewById(R.id.dialog_quiz_post_desc);
                failedDesc.setText(DataParse.getStr(GuessWord.this, "timeup_desc", Home.spf));
                Button backBtn = failedDiag.findViewById(R.id.dialog_quiz_post_quit);
                backBtn.setText(DataParse.getStr(GuessWord.this, "back", Home.spf));
                backBtn.setOnClickListener(view -> failedDiag.dismiss());
                Button keepTrying = failedDiag.findViewById(R.id.dialog_quiz_post_next);
                keepTrying.getBackground().setColorFilter(
                        ContextCompat.getColor(GuessWord.this, R.color.blue_2),
                        PorterDuff.Mode.MULTIPLY);
                keepTrying.setText(DataParse.getStr(GuessWord.this, "yes", Home.spf));
                keepTrying.setOnClickListener(view -> {
                    callNet();
                    failedDiag.dismiss();
                });
            }

            @Override
            public void onError(int errorCode, String error) {
                if (loadingDiag != null && loadingDiag.isShowing()) loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, GuessWord.this, () -> {
                        callOnce();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(GuessWord.this, error, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isLocked) {
            showQuitDiag();
        } else {
            holder.setVisibility(View.GONE);
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (countDown != null) countDown.cancel();
        if (handler != null) {
            handler.removeCallbacks(runnable);
            handler = null;
        }
        super.onDestroy();
    }

    private void callNet() {
        if (loadingDiag != null && !loadingDiag.isShowing()) loadingDiag.show();
        wrongView.setVisibility(View.GONE);
        fromTv = new SparseArray<>();
        toTv = new SparseArray<>();
        GetGame.getGW(this, cc, new onResponse() {
            @Override
            public void onSuccessHashMap(HashMap<String, String> data) {
                nextQs.setVisibility(View.GONE);
                if (!solveView.getText().toString().equals("0"))
                    solveBtn.setVisibility(View.VISIBLE);
                Picasso.get().load(data.get("image")).placeholder(R.drawable.anim_loading).into(imageView);
                infoView.setText(data.get("info"));
                int time = Integer.parseInt(data.get("timeup"));
                data.remove("image");
                data.remove("info");
                data.remove("timeup");
                fromData = data;
                initAdapter(time);
                if (loadingDiag != null && loadingDiag.isShowing()) loadingDiag.dismiss();
            }

            @Override
            public void onError(int errorCode, String error) {
                if (loadingDiag != null && loadingDiag.isShowing()) loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, GuessWord.this, () -> {
                        callNet();
                        conDiag.dismiss();
                    });
                } else {
                    isLocked = false;
                    Toast.makeText(GuessWord.this, error, Toast.LENGTH_LONG).show();
                    if (errorCode == 2) onBackPressed();
                }
            }

            @Override
            public void onLowCredit() {
                if (loadingDiag != null && loadingDiag.isShowing()) loadingDiag.dismiss();
                showLowBalDiag();
            }
        });
    }

    private void getResult(String ans) {
        if (loadingDiag != null && !loadingDiag.isShowing()) loadingDiag.show();
        clearHintAnim();
        GetGame.getGWReward(this, ans, new onResponse() {
            @Override
            public void onSuccess(String response) {
                updateScore(response);
                if (loadingDiag != null && loadingDiag.isShowing()) loadingDiag.dismiss();
                //Toast.makeText(GuessWord.this, "+" + response + " added to your total score", Toast.LENGTH_LONG).show();
                successDiag.show();
                if (countDown != null) countDown.cancel();
                isLocked = false;
                nextQs.setVisibility(View.VISIBLE);
                solveBtn.setVisibility(View.GONE);
            }

            @Override
            public void onError(int errorCode, String error) {
                if (loadingDiag != null && loadingDiag.isShowing()) loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, GuessWord.this, () -> {
                        getResult(ans);
                        conDiag.dismiss();
                    });
                } else {
                    if (errorCode == 2) {
                        updateRetry("-1");
                        wrongView.setVisibility(View.VISIBLE);
                        isAnimating = false;
                        nextQs.setVisibility(View.VISIBLE);
                        if (countDown != null) countDown.cancel();
                    } else {
                        Toast.makeText(GuessWord.this, error, Toast.LENGTH_LONG).show();
                        if (countDown != null) countDown.cancel();
                        isLocked = false;
                    }
                }
            }
        });
    }

    private void initAdapter(int time) {
        timeProgress.setMax(time);
        timeProgress.setProgress(time);
        countDown = new CountDownTimer(time, 200) {
            @Override
            public void onTick(long l) {
                timeProgress.setProgress((int) l);
            }

            @Override
            public void onFinish() {
                if (!isFinishing() && !isDestroyed() && isLocked) {
                    isLocked = false;
                    timeProgress.setProgress(0);
                    nextQs.setVisibility(View.VISIBLE);
                    solveBtn.setVisibility(View.GONE);
                    clearHintAnim();
                    failedDiag.show();
                }
            }
        };
        countDown.start();
        filled = 0;
        isAnimating = false;
        isLocked = true;
        fromGrid.removeAllViews();
        toGrid.removeAllViews();
        LinearLayout fRow = null, tRow = null;
        LayoutInflater inflater = LayoutInflater.from(this);
        int rSize = auto_fit() - 1;
        for (
                int i = 0; i < fromData.size(); i++) {
            if (i % rSize == 0) {
                if (fRow != null) {
                    fromGrid.addView(fRow);
                    toGrid.addView(tRow);
                }
                fRow = new LinearLayout(this);
                fRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                fRow.setOrientation(LinearLayout.HORIZONTAL);
                fRow.setGravity(Gravity.CENTER);

                tRow = new LinearLayout(this);
                tRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                tRow.setOrientation(LinearLayout.HORIZONTAL);
                tRow.setGravity(Gravity.CENTER);
            }

            @SuppressLint("InflateParams") View fV = inflater.inflate(R.layout.game_guess_word_item, null);
            TextView ftextView = fV.findViewById(R.id.game_guess_word_item_text);
            ftextView.setText(fromData.get(String.valueOf(i)));
            ftextView.setBackgroundResource(R.drawable.button_3d);
            fromTv.put(i, ftextView);
            fRow.addView(fV);

            @SuppressLint("InflateParams") View tV = inflater.inflate(R.layout.game_guess_word_item, null);
            TextView ttextView = tV.findViewById(R.id.game_guess_word_item_text);
            toTv.put(i, ttextView);
            tRow.addView(tV);

            ftextView.setOnClickListener(view -> {
                if (isAnimating || !isLocked) return;
                String txt = ftextView.getText().toString();
                if (!txt.isEmpty()) {
                    for (int n = 0; n < toTv.size(); n++) {
                        TextView t = toTv.get(n);
                        if (t.getText().toString().isEmpty()) {
                            filled++;
                            showAnim_1(ftextView, t, txt);
                            break;
                        }
                    }
                }
            });
            ttextView.setOnClickListener(view -> {
                if (isAnimating || !isLocked) return;
                wrongView.setVisibility(View.GONE);
                String txt = ttextView.getText().toString();
                if (txt.isEmpty()) return;
                for (int n = 0; n < fromTv.size(); n++) {
                    TextView t = fromTv.get(n);
                    if (t.getText().toString().isEmpty()) {
                        showAnim_2(ttextView, t, txt);
                        filled--;
                        break;
                    }
                }
            });
        }
        if (fRow != null) {
            fromGrid.addView(fRow);
            toGrid.addView(tRow);
        }

        hintBtn.setOnClickListener(view -> {
            if (isLocked) {
                if (!isAnimating) {
                    clearHintAnim();
                    if (hintView.getText().toString().equals("0")) {
                        Toast.makeText(GuessWord.this, DataParse.getStr(GuessWord.this, "hint_no_chance", Home.spf), Toast.LENGTH_LONG).show();
                        return;
                    }
                    for (int n = 0; n < toTv.size(); n++) {
                        TextView t = toTv.get(n);
                        if (t.getText().toString().isEmpty()) {
                            getHint(t, n);
                            break;
                        }
                    }
                }
            } else {
                Toast.makeText(GuessWord.this, DataParse.getStr(GuessWord.this, "not_in_game", Home.spf), Toast.LENGTH_LONG).show();
            }
        });

        solveBtn.setOnClickListener(view -> {
            if (isLocked) {
                if (!isAnimating) {
                    if (loadingDiag != null && !loadingDiag.isShowing()) loadingDiag.show();
                    GetGame.solveGW(this, new onResponse() {
                        @Override
                        public void onSuccessHashMap(HashMap<String, String> data) {
                            if (loadingDiag != null && loadingDiag.isShowing())
                                loadingDiag.dismiss();
                            if (countDown != null) countDown.cancel();
                            for (int i = 0; i < fromTv.size(); i++) {
                                TextView ft = fromTv.get(i);
                                TextView tt = toTv.get(i);
                                ft.setText(fromData.get(String.valueOf(i)));
                                ft.setVisibility(View.VISIBLE);
                                ft.setScaleX(1);
                                ft.setScaleY(1);
                                tt.setBackgroundResource(R.drawable.button_3d_yellow);
                                tt.setText(data.get(String.valueOf(i)));
                            }
                            isLocked = false;
                            wrongView.setVisibility(View.GONE);
                            nextQs.setVisibility(View.VISIBLE);
                            clearHintAnim();
                        }

                        @Override
                        public void onError(int errorCode, String error) {
                            if (loadingDiag != null && loadingDiag.isShowing())
                                loadingDiag.dismiss();
                            Toast.makeText(GuessWord.this, error, Toast.LENGTH_LONG).show();
                            isLocked = false;
                        }

                        @Override
                        public void onLowCredit() {
                            if (loadingDiag != null && loadingDiag.isShowing())
                                loadingDiag.dismiss();
                            showLowBalDiag();
                        }
                    });
                }
            } else {
                Toast.makeText(GuessWord.this, DataParse.getStr(GuessWord.this, "not_in_game", Home.spf), Toast.LENGTH_LONG).show();
            }
        });

        nextQs.setOnClickListener(view ->

                callNet());

        handler.removeCallbacks(runnable);
        if (!hintView.getText().

                toString().

                equals("0"))
            handler.postDelayed(runnable, 5000);
    }

    private int auto_fit() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (screenWidthDp / 45 + 0.5);
    }

    private void showAnim_1(TextView from, TextView to, String txt) {
        isAnimating = true;
        from.setText("");
        from.animate().alpha(0.2f).scaleX(0.4f).scaleY(0.4f).setDuration(200).withEndAction(() -> {
            from.setVisibility(View.GONE);
            to.setText(txt);
            to.setBackgroundResource(R.drawable.button_3d_green);
            to.setScaleX(0.3f);
            to.setScaleY(0.3f);
            to.animate().scaleX(1.0f).scaleY(1.0f).setDuration(500).setInterpolator(new BounceInterpolator()).withEndAction(() -> {
                if (filled == fromData.size()) {
                    StringBuilder word = new StringBuilder();
                    for (int k = 0; k < toTv.size(); k++) {
                        word.append(toTv.get(k).getText().toString());
                    }
                    getResult(word.toString());
                } else {
                    isAnimating = false;
                }
            }).start();
        }).start();
    }

    private void showAnim_2(TextView from, TextView to, String txt) {
        isAnimating = true;
        from.setText("");
        from.setBackgroundResource(R.drawable.button_3d_input);
        to.setText(txt);
        to.setBackgroundResource(R.drawable.button_3d);
        to.setVisibility(View.VISIBLE);
        to.animate().alpha(1).scaleX(1).scaleY(1).setDuration(200).withEndAction(() -> isAnimating = false).start();
    }

    private void updateScore(String scToAdd) {
        String score = String.valueOf(Integer.parseInt(scoreView.getText().toString()) + Integer.parseInt(scToAdd));
        scoreView.setText(score);
        Variables.setHash("score", score);
    }

    private void updateHint(String scToAdd) {
        hintView.setText(String.valueOf(Integer.parseInt(hintView.getText().toString()) + Integer.parseInt(scToAdd)));
    }

    private void updateRetry(String scToAdd) {
        retryView.setText(String.valueOf(Integer.parseInt(retryView.getText().toString()) + Integer.parseInt(scToAdd)));
    }

    private void clearHintAnim() {
        if (handler != null) {
            handler.removeCallbacks(runnable);
            hintBtn.clearAnimation();
        }
    }

    public void showLowBalDiag() {
        if (lowBalDiag == null) lowBalDiag = Misc.lowbalanceDiag(this, new Misc.yesNo() {
            @Override
            public void yes() {
                lowBalDiag.dismiss();
                Variables.setHash("show_offers", "1");
                finish();
            }

            @Override
            public void no() {
                lowBalDiag.dismiss();
            }
        });
        lowBalDiag.show();
    }

    private void showQuitDiag() {
        if (quitDiag == null) {
            quitDiag = Misc.decoratedDiag(this, R.layout.dialog_quit, 0.8f);
            TextView titleV = quitDiag.findViewById(R.id.dialog_quit_title);
            titleV.setText(DataParse.getStr(this, "are_you_sure", Home.spf));
            TextView descV = quitDiag.findViewById(R.id.dialog_quit_desc);
            descV.setText(DataParse.getStr(this, "close_diag_desc", Home.spf));
            quitDiag.findViewById(R.id.dialog_quit_no).setOnClickListener(view -> quitDiag.dismiss());
            quitDiag.findViewById(R.id.dialog_quit_yes).setOnClickListener(view -> {
                quitDiag.dismiss();
                finish();
            });
        }
        quitDiag.show();
    }

    private void getHint(TextView t, int n) {
        GetGame.getGWHint(GuessWord.this, n, new onResponse() {
            @Override
            public void onSuccess(String helpLetter) {
                updateHint("-1");
                boolean intact = true;
                for (int i = 0; i < fromTv.size(); i++) {
                    TextView ft = fromTv.get(i);
                    if (ft.getText().toString().equals(helpLetter)) {
                        filled++;
                        showAnim_1(ft, t, helpLetter);
                        intact = false;
                        break;
                    }
                }
                if (intact) {
                    for (int i = 0; i < toTv.size(); i++) {
                        TextView tt = toTv.get(i);
                        if (tt.getText().toString().equals(helpLetter)) {
                            t.setText(helpLetter);
                            t.setBackgroundResource(R.drawable.button_3d_green);
                            tt.setText("");
                            tt.setBackgroundResource(R.drawable.button_3d_input);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onError(int errorCode, String error) {
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, GuessWord.this, () -> {
                        getHint(t, n);
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(GuessWord.this, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}