package org.mintsoft.mintly.sdkoffers;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.ironsource.adapters.supersonicads.SupersonicConfig;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.Offerwalls;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.HashMap;

public class ironsrc extends BaseAppCompat {
    private Dialog dialog;
    private HashMap<String, String> data;
    private String user;
    private boolean isAvailable, isLive;

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
            Offerwalls.getStat(this, "ironsrc", true, new onResponse() {
                @Override
                public void onSuccess(String s) {
                    if (!isLive) return;
                    if (s.equals("1")) {
                        runOnUiThread(() -> forward());
                    } else {
                        Toast.makeText(ironsrc.this, DataParse.getStr(ironsrc.this,
                                "exceed_daily_limit", Home.spf), Toast.LENGTH_LONG).show();
                        finish();
                    }
                }

                @Override
                public void onError(int i, String s) {
                    if (!isLive) return;
                    dialog.dismiss();
                    Toast.makeText(ironsrc.this, s, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    private void forward() {
        IronSource.init(this, data.get("app_key"), () -> {
            if (!isLive) return;
            IronSource.setUserId(user);
            SupersonicConfig.getConfigObj().setClientSideCallbacks(false);
            IronSource.setLevelPlayRewardedVideoListener(new LevelPlayRewardedVideoListener() {
                @Override
                public void onAdAvailable(AdInfo adInfo) {
                    if (!isLive) return;
                    isAvailable = true;
                    IronSource.showRewardedVideo(data.get("offerwall_placement"));
                }

                @Override
                public void onAdUnavailable() {
                    if (!isLive) return;
                    if (dialog.isShowing()) dialog.dismiss();
                    uiToast("Ads not available");
                }

                @Override
                public void onAdOpened(AdInfo adInfo) {
                    if (isLive && dialog.isShowing()) dialog.dismiss();
                }

                @Override
                public void onAdShowFailed(IronSourceError ironSourceError, AdInfo adInfo) {
                    if (!isLive) return;
                    if (dialog.isShowing()) dialog.dismiss();
                    uiToast(ironSourceError.getErrorMessage());
                }

                @Override
                public void onAdClicked(Placement placement, AdInfo adInfo) {

                }

                @Override
                public void onAdRewarded(Placement placement, AdInfo adInfo) {
                    Home.checkBalance = 1;
                    if (!isLive) return;
                    Offerwalls.getStat(getApplicationContext(), "vungle", false, null);
                }

                @Override
                public void onAdClosed(AdInfo adInfo) {
                    if (!isLive) return;
                    finish();
                }
            });
        });
        new Handler().postDelayed(() -> {
            if (!isLive) return;
            if (!isAvailable) {
                if (dialog.isShowing()) dialog.dismiss();
                uiToast("No fill");
            }
        }, 10000);

    }

    private void uiToast(final String toast) {
        runOnUiThread(() -> {
            Toast.makeText(ironsrc.this, toast, Toast.LENGTH_LONG).show();
            finish();
        });
    }

	@Override
    protected void onDestroy() {
        isLive = false;
		if (dialog != null && dialog.isShowing()) dialog.dismiss();
		super.onDestroy();
    }
}
