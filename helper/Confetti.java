package org.mintsoft.mintly.helper;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.UnityAds;

import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.offers.PPVOffers;

public class Confetti extends BaseAppCompat {
    private boolean adAvailable;
    private Animation anim;
    private int resultCode, icon;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            Bundle extras = getIntent().getExtras();
            assert extras != null;
            resultCode = extras.getInt("code", -1);
            if (resultCode != -1) setResult(resultCode);
            String text = extras.getString("text");
            icon = extras.getInt("icon", 0);
            String pId = extras.getString("ppv", null);
            if (pId != null) PPVOffers.p_id = pId;
            if (text != null) {
                loadAd();
                Window window = getWindow();
                window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setNavigationBarColor(Color.argb(150, 0, 0, 0));
                window.setStatusBarColor(Color.argb(150, 0, 0, 0));
                setContentView(R.layout.confetti);
                TextView textView = findViewById(R.id.confetti_textView);
                ImageView flareView = findViewById(R.id.confetti_flare);
                textView.setText(text);
                ImageView iconView = findViewById(R.id.confetti_iconView);
                if (icon == 0) {
                    iconView.setImageResource(R.drawable.icon_coin);
                } else {
                    iconView.setImageResource(icon);
                }
                flareView.animate().setInterpolator(new LinearInterpolator())
                        .setDuration(7000)
                        .rotation(360)
                        .withEndAction(() -> {
                            if (anim != null) anim.cancel();
                            finish();
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        })
                        .start();
                anim = new ScaleAnimation(
                        1f, 0.9f, 1f, 0.9f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                anim.setRepeatCount(Animation.INFINITE);
                anim.setRepeatMode(Animation.REVERSE);
                anim.setDuration(3000);
                anim.setFillAfter(true);
                iconView.startAnimation(anim);
                findViewById(R.id.confetti_holder).setOnClickListener(view -> finish());
                String btnText = extras.getString("btn_text", null);
                if (btnText != null) {
                    TextView btnTextView = findViewById(R.id.confetti_btnTextView);
                    btnTextView.setText(btnText);
                    View btnHolder = findViewById(R.id.confetti_btnHolder);
                    btnHolder.setVisibility(View.VISIBLE);
                    btnTextView.setVisibility(View.VISIBLE);
                    btnHolder.setOnClickListener(view -> {
                        Intent intent = new Intent();
                        intent.putExtra("btn", true);
                        setResult(resultCode, intent);
                        finish();
                    });
                }
            } else {
                finish();
            }
        } catch (Exception e) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adAvailable) {
            if (Home.sAdv) {
                UnityAds.show(this, Home.interstitialUnit);
            } else {
                Home.interstitialAd.show(this);
            }
        }
    }

    private void loadAd() {
        if ((icon == 0 || icon == R.drawable.icon_coin) && Home.confettiAds) {
            if (Home.sAdv) {
                UnityAds.load(Home.rewardedUnit, new IUnityAdsLoadListener() {
                    @Override
                    public void onUnityAdsAdLoaded(String placementId) {
                        adAvailable = true;
                    }

                    @Override
                    public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                        adAvailable = false;
                    }
                });
            } else if (Home.interstitialAd != null) {
                adAvailable = true;
            }
        }

    }
}