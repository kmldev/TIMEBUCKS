package org.mintsoft.mintly.sdkoffers;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.chartboost.sdk.CBLocation;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.ChartboostDelegate;
import com.chartboost.sdk.Model.CBError;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.Offerwalls;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.HashMap;
import java.util.Objects;

public class chartboost extends BaseAppCompat {
    private boolean shownAd = false;
    private Dialog dialog;
    private HashMap<String, String> data;
    private String user;
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
            Offerwalls.getStat(this, "chartboost", true, new onResponse() {
                @Override
                public void onSuccess(String s) {
                    if (!isLive) return;
                    if (s.equals("1")) {
                        runOnUiThread(() -> forward());
                    } else {
                        Toast.makeText(chartboost.this, DataParse.getStr(chartboost.this,
                                "exceed_daily_limit", Home.spf), Toast.LENGTH_LONG).show();
                        finish();
                    }
                }

                @Override
                public void onError(int i, String s) {
                    if (!isLive) return;
                    dialog.dismiss();
                    Toast.makeText(chartboost.this, s, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    private void forward() {
        Chartboost.setCustomId(user);
        ChartboostDelegate delegate = new ChartboostDelegate() {
            @Override
            public boolean shouldDisplayRewardedVideo(String location) {
                super.shouldDisplayRewardedVideo(location);
                if (isLive && dialog.isShowing()) dialog.dismiss();
                return true;
            }

            @Override
            public void didInitialize() {
                super.didInitialize();
                if (!isLive) return;
                if (!shownAd) {
                    shownAd = true;
                    Chartboost.showRewardedVideo(CBLocation.LOCATION_DEFAULT);
                }
            }

            @Override
            public void didFailToLoadRewardedVideo(String location, CBError.CBImpressionError error) {
                super.didFailToLoadRewardedVideo(location, error);
                if (!isLive) return;
                uiToast("" + error);
                finish();
            }

            @Override
            public void didCloseRewardedVideo(String location) {
                super.didCloseRewardedVideo(location);
                if (!isLive) return;
                if (Objects.equals(location, CBLocation.LOCATION_DEFAULT)) {
                    Home.checkBalance = 1;
                    Offerwalls.getStat(getApplicationContext(), "chartboost", false, null);
                    finish();
                }
            }
        };
        Chartboost.setDelegate(delegate);
        Chartboost.startWithAppId((Context) this, data.get("app_id"), data.get("app_signature"));
    }

    private void uiToast(final String toast) {
        runOnUiThread(() -> Toast.makeText(chartboost.this, toast, Toast.LENGTH_LONG).show());
    }

	@Override
    protected void onDestroy() {
        isLive = false;
        if (dialog.isShowing()) dialog.dismiss();
		super.onDestroy();
    }
}