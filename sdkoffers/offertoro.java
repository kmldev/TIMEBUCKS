package org.mintsoft.mintly.sdkoffers;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.offertoro.sdk.OfferWall;
import com.offertoro.sdk.OfferWallListener;

import org.json.JSONArray;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.HashMap;

public class offertoro extends BaseAppCompat {
    private Dialog dialog;
    private boolean isLive;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        isLive = true;
        Intent intent = getIntent();
        HashMap<String, String> data = Misc.convertToHashMap(intent, "info");
        String user = intent.getStringExtra("user");
        if (data != null && user != null) {
            dialog = Misc.loadingDiagExit(this);
            dialog.show();
            try {
                OfferWall.getInstance().setConfig(data.get("app_id"), data.get("app_secret"), user);
                //OfferWall.getInstance().getUserWallCredits();
                OfferWall.getInstance().setOfferWallListener(new OfferWallListener() {
                    @Override
                    public void onOfferWallInitSuccess() {
                        if (!isLive) return;
                        if (dialog.isShowing()) dialog.dismiss();
                        Home.checkBalance = 1;
                        finish();
                    }

                    @Override
                    public void onOfferWallInitFail(String s) {
                        if (!isLive) return;
                        if (dialog.isShowing()) dialog.dismiss();
                        uiToast("" + s);
                        finish();
                    }

                    @Override
                    public void onOfferWallOpened() {

                    }

                    @Override
                    public void onOfferWallOfferClicked(String s) {

                    }

                    @Override
                    public void onOfferWallCredited(double v, double v1) {

                    }

                    @Override
                    public void onOfferLoadFail(String s) {

                    }

                    @Override
                    public void onOfferWallClosed() {
                    }

                    @Override
                    public void onOfferWallGetUserCredits(JSONArray jsonArray) {

                    }

                    @Override
                    public void onOfferWallGetUserCreditsError(String s) {

                    }

                    @Override
                    public void onOfferWallMissingCreditsError() {

                    }
                });
                OfferWall.getInstance().showOfferWall(this);
            } catch (Exception e) {
                if (!isLive) return;
                Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onStop() {
        isLive = false;
        super.onStop();
    }

    private void uiToast(final String toast) {
        runOnUiThread(() -> Toast.makeText(offertoro.this, toast, Toast.LENGTH_LONG).show());
    }
}