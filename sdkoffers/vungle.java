package org.mintsoft.mintly.sdkoffers;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.vungle.warren.AdConfig;
import com.vungle.warren.InitCallback;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.Offerwalls;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.HashMap;

public class vungle extends BaseAppCompat {
    private Dialog dialog;
    private LoadAdCallback loadAdCallback;
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
            Offerwalls.getStat(this, "vungle", true, new onResponse() {
                @Override
                public void onSuccess(String s) {
                    if (!isLive) return;
                    if (s.equals("1")) {
                        runOnUiThread(() -> forward());
                    } else {
                        Toast.makeText(vungle.this, DataParse.getStr(vungle.this,
                                "exceed_daily_limit", Home.spf), Toast.LENGTH_LONG).show();
                        finish();
                    }
                }

                @Override
                public void onError(int i, String s) {
                    if (!isLive) return;
                    dialog.dismiss();
                    Toast.makeText(vungle.this, s, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    private void forward() {
        loadAdCallback = new LoadAdCallback() {
            @Override
            public void onAdLoad(String id) {
                if (!isLive) return;
                if (dialog.isShowing()) dialog.dismiss();
                AdConfig adConfig = new AdConfig();
                adConfig.setAdOrientation(AdConfig.MATCH_VIDEO);
                Vungle.playAd(id, adConfig, new PlayAdCallback() {
                    @Override
                    public void creativeId(String creativeId) {

                    }

                    @Override
                    public void onAdStart(String placementId) {

                    }

                    @Override
                    public void onAdEnd(String placementId, boolean completed, boolean isCTAClicked) {
                        if (!isLive) return;
                        if (completed) {
                            Home.checkBalance = 1;
                            Offerwalls.getStat(getApplicationContext(), "vungle", false, null);
                            finish();
                        }
                    }

                    @Override
                    public void onAdEnd(String placementId) {

                    }

                    @Override
                    public void onAdClick(String placementId) {

                    }

                    @Override
                    public void onAdRewarded(String placementId) {

                    }

                    @Override
                    public void onAdLeftApplication(String placementId) {

                    }

                    @Override
                    public void onError(String placementId, VungleException exception) {
                        if (!isLive) return;
                        uiToast("Ad loading: " + exception.getMessage());
                    }

                    @Override
                    public void onAdViewed(String placementId) {

                    }
                });
            }

            @Override
            public void onError(String id, VungleException exception) {
                if (!isLive) return;
                if (dialog.isShowing()) dialog.dismiss();
                uiToast("Loading failed: " + exception.getMessage());
                finish();
            }
        };
        InitCallback initCallback = new InitCallback() {
            @Override
            public void onSuccess() {
                if (!isLive) return;
                Vungle.setIncentivizedFields(user, null, null, null, "Close");
                Vungle.loadAd(data.get("placement"), loadAdCallback);
            }

            @Override
            public void onError(VungleException exception) {
                if (!isLive) return;
                if (dialog.isShowing()) dialog.dismiss();
                uiToast("Initialization failed: " + exception.getMessage());
                finish();
            }

            @Override
            public void onAutoCacheAdAvailable(String placementId) {

            }
        };
        Vungle.init(data.get("app_id"), getApplicationContext(), initCallback);
    }

    private void uiToast(final String toast) {
        runOnUiThread(() -> Toast.makeText(vungle.this, toast, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onStop() {
        isLive = false;
        if (dialog.isShowing()) dialog.dismiss();
        super.onStop();
    }
}
