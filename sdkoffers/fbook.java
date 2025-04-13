package org.mintsoft.mintly.sdkoffers;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.RewardData;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.Offerwalls;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.util.HashMap;

public class fbook extends BaseAppCompat implements AudienceNetworkAds.InitListener, RewardedVideoAdListener {
    private Dialog dialog;
    private String user;
    private RewardedVideoAd rewardedVideoAd;
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
            Offerwalls.getStat(this, "fbook", true, new onResponse() {
                @Override
                public void onSuccess(String s) {
                    if (!isLive) return;
                    if (s.equals("1")) {
                        runOnUiThread(() -> forward());
                    } else {
                        Toast.makeText(fbook.this, DataParse.getStr(fbook.this,
                                "exceed_daily_limit", Home.spf), Toast.LENGTH_LONG).show();
                        finish();
                    }
                }

                @Override
                public void onError(int i, String s) {
                    if (!isLive) return;
                    dialog.dismiss();
                    Toast.makeText(fbook.this, s, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    private void forward() {
        if (AudienceNetworkAds.isInitialized(getApplicationContext())) {
            loadAd();
        } else {
            if (Variables.getPHash("debug").equals("1")) AdSettings.turnOnSDKDebugger(this);
            AudienceNetworkAds.buildInitSettings(getApplicationContext()).withInitListener(this).initialize();
        }
    }

	@Override
    protected void onDestroy() {
        isLive = false;
		if (rewardedVideoAd != null) {
            rewardedVideoAd.destroy();
            rewardedVideoAd = null;
        }
		super.onDestroy();
    }

    @Override
    public void onInitialized(AudienceNetworkAds.InitResult initResult) {
        loadAd();
    }

    private void loadAd() {
        if (!isLive) return;
        rewardedVideoAd = new RewardedVideoAd(this, data.get("placement_id"));
        RewardData rewardData = new RewardData(user, "1");
        rewardedVideoAd.loadAd(rewardedVideoAd.buildLoadAdConfig()
                .withAdListener(this).withRewardData(rewardData).build());
    }

    @Override
    public void onRewardedVideoCompleted() {
        Home.checkBalance = 1;
        if (!isLive) return;
        Offerwalls.getStat(getApplicationContext(), "fbook", false, null);
    }

    @Override
    public void onRewardedVideoClosed() {
        finish();
    }

    @Override
    public void onError(Ad ad, AdError adError) {
        if (!isLive) return;
        dialog.dismiss();
        Toast.makeText(this, "Error: " + adError.getErrorMessage(), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onAdLoaded(Ad ad) {
        if (!isLive) return;
        dialog.dismiss();
        rewardedVideoAd.show();
    }

    @Override
    public void onAdClicked(Ad ad) {

    }

    @Override
    public void onLoggingImpression(Ad ad) {

    }
}