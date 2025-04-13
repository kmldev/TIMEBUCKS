package org.mintsoft.mintly.sdkoffers;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdOptions;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.Offerwalls;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.HashMap;

public class adcolony extends BaseAppCompat {
    private Dialog dialog;
    private AdColonyInterstitialListener listener;
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
            Offerwalls.getStat(this, "adcolony", true, new onResponse() {
                @Override
                public void onSuccess(String s) {
                    if (!isLive) return;
                    if (s.equals("1")) {
                        runOnUiThread(() -> forward());
                    } else {
                        Toast.makeText(adcolony.this, DataParse.getStr(adcolony.this,
                                "exceed_daily_limit", Home.spf), Toast.LENGTH_LONG).show();
                        finish();
                    }
                }

                @Override
                public void onError(int i, String s) {
                    if (!isLive) return;
                    dialog.dismiss();
                    Toast.makeText(adcolony.this, s, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } else {
            finish();
        }
    }

    private void forward() {
        AdColonyAppOptions appOptions = new AdColonyAppOptions().setUserID(user).setGDPRConsentString("1")
                .setKeepScreenOn(true).setGDPRRequired(true);
        AdColony.configure(this, appOptions, data.get("app_id"), data.get("zone_id"));
        AdColonyAdOptions options = new AdColonyAdOptions().enableConfirmationDialog(false).enableResultsDialog(false);
        listener = new AdColonyInterstitialListener() {
            @Override
            public void onRequestFilled(AdColonyInterstitial adColonyInterstitial) {
                if (!isLive) return;
                if (dialog.isShowing()) dialog.dismiss();
                adColonyInterstitial.show();
                Home.checkBalance = 1;
                Offerwalls.getStat(getApplicationContext(), "adcolony", false, null);
            }

            @Override
            public void onRequestNotFilled(AdColonyZone zone) {
                if (!isLive) return;
                if (dialog.isShowing()) dialog.dismiss();
                Toast.makeText(adcolony.this, "No fill", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onClosed(AdColonyInterstitial ad) {
                finish();
            }
        };
        AdColony.requestInterstitial(data.get("zone_id"), listener, options);
    }

	@Override
    protected void onDestroy() {
		listener = null;
        isLive = false;
		super.onDestroy();
    }
}
