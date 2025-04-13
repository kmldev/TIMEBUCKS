package org.mintsoft.mintly.helper;

import android.os.Bundle;
import android.os.Handler;
import android.transition.ChangeBounds;
import android.transition.Explode;
import android.transition.Transition;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import org.mintsoft.mintlib.GetGame;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;

public class PopupAr extends BaseAppCompat {
    private ChangeBounds bounds;
    private ImageView imageView;
    private ConstraintLayout coinView;
    private TextView textView;
    private Animation zoom_in, zoom_out;
    private Transition.TransitionListener listener;
    private int id, canStop, canReturn;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        bounds = new ChangeBounds();
        bounds.setDuration(500);
        getWindow().setSharedElementEnterTransition(bounds);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.popup_ar);
        imageView = findViewById(R.id.popup_ar_image);
        textView = findViewById(R.id.popup_ar_text);
        coinView = findViewById(R.id.popup_ar_coin);
        zoom_in = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
        zoom_out = AnimationUtils.loadAnimation(this, R.anim.zoom_out);
        imageView.setAnimation(zoom_in);
        imageView.setAnimation(zoom_out);
        id = getIntent().getIntExtra("id", 0);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (bounds != null) {
            listener = new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    imageView.startAnimation(zoom_out);
                    new Handler().postDelayed(() -> GetGame.getActivityReward(PopupAr.this, id, new onResponse() {
                        @Override
                        public void onSuccess(String amt) {
                            canStop = 1;
                            textView.setText(amt);
                            try {
                                Home.balance = String.valueOf(Integer.parseInt(Home.balance) + Integer.parseInt(amt));
                                Home.checkBalance = 2;
                            } catch (Exception ignored) {
                            }
                        }

                        @Override
                        public void onError(int errorCode, String error) {
                            canReturn = 1;
                            Toast.makeText(PopupAr.this, error, Toast.LENGTH_LONG).show();
                            bounds.removeListener(listener);
                            onBackPressed();
                        }
                    }), 3000);
                }

                @Override
                public void onTransitionCancel(Transition transition) {

                }

                @Override
                public void onTransitionPause(Transition transition) {

                }

                @Override
                public void onTransitionResume(Transition transition) {

                }
            };
            bounds.addListener(listener);
        }
        zoom_in.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation arg0) {

            }

            @Override
            public void onAnimationRepeat(Animation arg0) {
            }

            @Override
            public void onAnimationEnd(Animation arg0) {
                if (canStop == 1) {
                    bounds.removeListener(listener);
                    imageView.setImageResource(R.drawable.reward_done);
                    canReturn = 1;
                    setResult(18);
                    makeRewardAnim();
                } else {
                    imageView.startAnimation(zoom_out);
                }
            }
        });
        zoom_out.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation arg0) {

            }

            @Override
            public void onAnimationRepeat(Animation arg0) {

            }

            @Override
            public void onAnimationEnd(Animation arg0) {
                imageView.startAnimation(zoom_in);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (canReturn == 1) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, "Wait until it finishes", Toast.LENGTH_LONG).show();
        }
    }

    private void makeRewardAnim() {
        Animation translation = new TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_PARENT, 0, TranslateAnimation.RELATIVE_TO_PARENT, 0,
                TranslateAnimation.RELATIVE_TO_PARENT, -0.1f, TranslateAnimation.RELATIVE_TO_PARENT, -0.15f
        );
        translation.setDuration(500);
        translation.setFillAfter(true);
        Animation scale = new ScaleAnimation(
                0.8f, 1f, 0.8f, 1f,
                TranslateAnimation.RELATIVE_TO_PARENT, 0.2f,
                TranslateAnimation.RELATIVE_TO_PARENT, 0
        );
        scale.setDuration(500);
        scale.setFillAfter(true);
        Animation alpha = new AlphaAnimation(1, 0);
        alpha.setStartOffset(4000);
        alpha.setDuration(500);
        alpha.setFillAfter(true);

        AnimationSet as2 = new AnimationSet(true);
        as2.addAnimation(translation);
        as2.addAnimation(scale);
        as2.addAnimation(alpha);
        as2.setInterpolator(new LinearInterpolator());

        as2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                coinView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                coinView.setVisibility(View.GONE);
                onBackPressed();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        coinView.startAnimation(as2);
    }
}