package org.mintsoft.mintly.sdkoffers;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.Offerwalls;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.HashMap;
import java.util.Objects;

public class admob extends BaseAppCompat {
    private Dialog dialog;
    private String slot, user;
    private HashMap<String, String> data;
    private boolean isLive;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        isLive = true;
        Intent intent = getIntent();
        data = Misc.convertToHashMap(intent, "info");
        user = intent.getStringExtra("user");
        if (data != null && user != null) {
            dialog = Misc.loadingDiagExit(this);
            dialog.show();
            Offerwalls.getStat(this, "admob", true, new onResponse() {
                @Override
                public void onSuccess(String s) {
                    if (!isLive) return;
                    if (s.equals("1")) {
                        runOnUiThread(() -> forward());
                    } else {
                        Toast.makeText(admob.this, DataParse.getStr(admob.this,
                                "exceed_daily_limit", Home.spf), Toast.LENGTH_LONG).show();
                        finish();
                    }
                }

                @Override
                public void onError(int i, String s) {
                    if (!isLive) return;
                    dialog.dismiss();
                    Toast.makeText(admob.this, s, Toast.LENGTH_LONG).show();
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

    private void forward() {
        slot = data.get("rewarded_slot");
        try {
            String app_id = data.get("app_id");
            if (app_id != null) {
                ApplicationInfo ai = getPackageManager()
                        .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
                ai.metaData.putString("com.google.android.gms.ads.APPLICATION_ID", app_id);
            }
            MobileAds.initialize(this);
            new Handler().postDelayed(() -> runOnUiThread(this::loadAds), 2000);
        } catch (Exception e) {
            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadAds() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, Objects.requireNonNull(slot),
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        if (!isLive) return;
                        dialog.dismiss();
                        if (loadAdError.getCode() == 3) {
                            uiToast("Ads not available");
                        } else {
                            uiToast("Code: " + loadAdError.getCode() + ". Message:" + loadAdError.getMessage());
                        }
                        finish();
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        if (!isLive) return;
                        dialog.dismiss();
                        ServerSideVerificationOptions options = new ServerSideVerificationOptions.Builder().setUserId(user).build();
                        ad.setServerSideVerificationOptions(options);
                        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdShowedFullScreenContent() {
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                uiToast("" + adError.getMessage());
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                finish();
                            }
                        });
                        ad.show(admob.this, rewardItem -> {
                            if (!isLive) return;
                            Home.checkBalance = 1;
                            Offerwalls.getStat(getApplicationContext(), "admob", false, null);
                        });
                    }
                });
    }

    private void uiToast(final String toast) {
        if (!isLive) return;
        runOnUiThread(() -> Toast.makeText(admob.this, toast, Toast.LENGTH_LONG).show());
    }
}