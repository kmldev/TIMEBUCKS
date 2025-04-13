package org.mintsoft.mintly.sdkoffers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.PlayerMetaData;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.Offerwalls;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.HashMap;

public class unityads extends BaseAppCompat {
    private String user;
    private ProgressBar progressBar;
    private HashMap<String, String> data;
    private boolean blocked, isLive;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        isLive = true;
        RelativeLayout layout = new RelativeLayout(this);
        RelativeLayout.LayoutParams match_parent = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        layout.setLayoutParams(match_parent);
        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        RelativeLayout.LayoutParams wrap_content = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        wrap_content.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        progressBar.setLayoutParams(wrap_content);
        layout.addView(progressBar);
        setContentView(layout);
        Intent intent = getIntent();
        data = Misc.convertToHashMap(intent, "info");
        user = intent.getStringExtra("user");
        if (data != null && user != null) {
            progressBar.setVisibility(View.VISIBLE);
            blocked = true;
            Offerwalls.getStat(this, "unityads", true, new onResponse() {
                @Override
                public void onSuccess(String s) {
                    if (!isLive) return;
                    blocked = false;
                    if (isFinishing() || isDestroyed()) return;
                    if (s.equals("1")) {
                        runOnUiThread(() -> forward());
                    } else {
                        Toast.makeText(unityads.this, DataParse.getStr(unityads.this,
                                "exceed_daily_limit", Home.spf), Toast.LENGTH_LONG).show();
                        finish();
                    }
                }

                @Override
                public void onError(int i, String s) {
                    if (!isLive) return;
                    blocked = false;
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(unityads.this, s, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        isLive = false;
		super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!blocked) super.onBackPressed();
    }

    private void forward() {
        if (UnityAds.isInitialized()) {
            new Handler().postDelayed(this::showAds, 2000);
        } else {
            UnityAds.initialize((Context) this, data.get("game_id"), false, new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    if (!isLive) return;
                    showAds();
                }

                @Override
                public void onInitializationFailed(UnityAds.UnityAdsInitializationError unityAdsInitializationError, String s) {
                    if (!isLive) return;
                    Toast.makeText(unityads.this, "" + s, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }
    }

    private void showAds() {
        if (!isLive) return;
        PlayerMetaData playerMetaData = new PlayerMetaData(this);
        playerMetaData.setServerId(user);
        playerMetaData.commit();
        UnityAds.load(data.get("unit_id_r"), new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String s) {
                if (!isLive) return;
                if (s.equals(data.get("unit_id_r"))) {
                    progressBar.setVisibility(View.GONE);
                    UnityAds.show(unityads.this, data.get("unit_id_r"), new IUnityAdsShowListener() {
                        @Override
                        public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                            if (!isLive) return;
                            Toast.makeText(unityads.this, "" + message, Toast.LENGTH_LONG).show();
                            finish();
                        }

                        @Override
                        public void onUnityAdsShowStart(String placementId) {

                        }

                        @Override
                        public void onUnityAdsShowClick(String placementId) {

                        }

                        @Override
                        public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
                            if (!isLive) return;
                            if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                                Home.checkBalance = 1;
                                Offerwalls.getStat(getApplicationContext(), "unityads", false, null);
                                finish();
                            }
                        }
                    });
                }
            }

            @Override
            public void onUnityAdsFailedToLoad(String s, UnityAds.UnityAdsLoadError unityAdsLoadError, String s1) {
                if (!isLive) return;
                Toast.makeText(unityads.this, "" + s1, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}