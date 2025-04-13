package org.mintsoft.mintly.sdkoffers;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkSettings;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.Offerwalls;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class applovin extends BaseAppCompat {
    private Dialog dialog;
    private AppLovinIncentivizedInterstitial myIncent;
    private AppLovinAdDisplayListener listener;
    private AppLovinSdk instance;
    private MaxRewardedAd rewardedAd;
    private boolean loaded = false, isLive;
    private int retryAttempt;
    private Handler handler;
    private String unit, err = "Ad loading timeout!";
    private HashMap<String, String> data;
    private String user;

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
            Offerwalls.getStat(this, "applovin", true, new onResponse() {
                @Override
                public void onSuccess(String s) {
                    if (!isLive) return;
                    if (s.equals("1")) {
                        runOnUiThread(() -> forward());
                    } else {
                        Toast.makeText(applovin.this, DataParse.getStr(applovin.this,
                                "exceed_daily_limit", Home.spf), Toast.LENGTH_LONG).show();
                        finish();
                    }
                }

                @Override
                public void onError(int i, String s) {
                    if (!isLive) return;
                    dialog.dismiss();
                    Toast.makeText(applovin.this, s, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    private void forward() {
        String key = data.get("sdk_key");
        if (key == null) {
            Toast.makeText(this, "Setup first", Toast.LENGTH_LONG).show();
            if (!isFinishing()) finish();
            return;
        } else {
            String[] k = key.split(",");
            if (k.length > 1) {
                key = k[0];
                unit = k[1];
            } else {
                unit = data.get("unit_id");
            }
        }
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            ai.metaData.putString("applovin.sdk.key", key);
        } catch (Exception ignored) {
        }
        AppLovinSdkSettings settings = new AppLovinSdkSettings(this);
        settings.setVerboseLogging(false);
        AppLovinPrivacySettings.setHasUserConsent(true, this);
        AppLovinPrivacySettings.setDoNotSell(true, this);
        //AppLovinPrivacySettings.setIsAgeRestrictedUser(false, this);
        instance = AppLovinSdk.getInstance(key, settings, this);
        instance.setUserIdentifier(user);
        if (unit != null && !unit.isEmpty()) instance.setMediationProvider("max");
        AppLovinSdk.SdkInitializationListener sdkListener = config -> {
            if (!isLive) return;
            if (unit == null || unit.isEmpty()) {
                myIncent = AppLovinIncentivizedInterstitial.create(instance);
                listener = new AppLovinAdDisplayListener() {
                    @Override
                    public void adDisplayed(AppLovinAd ad) {

                    }

                    @Override
                    public void adHidden(AppLovinAd ad) {
                        if (!isLive) return;
                        myIncent.preload(null);
                        if (dialog.isShowing()) dialog.dismiss();
                        if (!isFinishing()) finish();
                    }
                };
                myIncent.preload(new AppLovinAdLoadListener() {
                    @Override
                    public void adReceived(AppLovinAd appLovinAd) {
                        if (!isLive) return;
                        if (dialog.isShowing()) dialog.dismiss();
                        myIncent.show(appLovinAd, applovin.this, null,
                                null, listener, null);
                        Home.checkBalance = 1;
                        Offerwalls.getStat(getApplicationContext(), "applovin", false, null);
                    }

                    @Override
                    public void failedToReceiveAd(int errorCode) {
                        if (!isLive) return;
                        if (dialog.isShowing()) dialog.dismiss();
                        uiToast("Error code: " + errorCode);
                    }
                });
            } else {
                rewardedAd = MaxRewardedAd.getInstance(unit, instance, applovin.this);
                rewardedAd.setListener(new MaxRewardedAdListener() {
                    @Override
                    public void onUserRewarded(@NonNull MaxAd maxAd, @NonNull MaxReward maxReward) {
                        Home.checkBalance = 1;
                    }

                    @Override
                    public void onAdLoaded(@NonNull MaxAd maxAd) {
                        if (!isLive) return;
                        if (dialog.isShowing()) dialog.dismiss();
                        if (!loaded) {
                            loaded = true;
                            rewardedAd.showAd();
                        }
                    }

                    @Override
                    public void onAdDisplayed(@NonNull MaxAd maxAd) {
                        if (!isLive) return;
                        finish();
                    }

                    @Override
                    public void onAdHidden(@NonNull MaxAd maxAd) {
                        if (!isLive) return;
                        if (!loaded) rewardedAd.loadAd();
                    }

                    @Override
                    public void onAdClicked(@NonNull MaxAd maxAd) {

                    }

                    @Override
                    public void onAdLoadFailed(@NonNull String s, @NonNull MaxError error) {
                        if (!isLive) return;
                        if (retryAttempt < 5) {
                            retryAttempt++;
                            long delayMillis = TimeUnit.SECONDS.toMillis((long) Math.pow(2, Math.min(6, retryAttempt)));
                            new Handler().postDelayed(() -> {
                                if (!loaded) rewardedAd.loadAd();
                            }, delayMillis);
                            err = "ErrorCode: " + error.getCode() + " message" + error.getMessage();
                        } else {
                            uiToast("" + error.getMessage());
                        }
                    }

                    @Override
                    public void onAdDisplayFailed(@NonNull MaxAd maxAd, @NonNull MaxError error) {
                        if (!isLive) return;
                        uiToast("" + error.getMessage());
                    }
                });
                if (!loaded) rewardedAd.loadAd();
                handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isLive) return;
                        if (!loaded) {
                            if (rewardedAd.isReady()) {
                                loaded = true;
                                rewardedAd.showAd();
                            } else {
                                handler.postDelayed(this, 3000);
                            }
                        }
                    }
                }, 3000);
            }
        };
        instance.initializeSdk(sdkListener);
        new Handler().postDelayed(() -> runOnUiThread(() -> {
            if (!isLive) return;
            if (dialog.isShowing()) dialog.dismiss();
            if (instance != null && !instance.isInitialized()) {
                Toast.makeText(applovin.this, "Could not initialize SDK", Toast.LENGTH_LONG).show();
            } else if (!loaded) {
                Toast.makeText(applovin.this, err, Toast.LENGTH_LONG).show();
            }
            finish();
        }), 64000);
    }

    private void uiToast(final String toast) {
        runOnUiThread(() -> {
            if (!isLive) return;
            if (dialog.isShowing()) dialog.dismiss();
            Toast.makeText(applovin.this, toast, Toast.LENGTH_LONG).show();
            if (!isFinishing()) finish();
        });
    }

    @Override
    protected void onDestroy() {
        isLive = false;
        if (dialog.isShowing()) dialog.dismiss();
        super.onDestroy();
    }
}