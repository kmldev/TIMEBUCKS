package org.mintsoft.mintly.sdkoffers;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.fyber.Fyber;
import com.fyber.ads.AdFormat;
import com.fyber.requesters.OfferWallRequester;
import com.fyber.requesters.RequestCallback;
import com.fyber.requesters.RequestError;

import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.HashMap;

public class fyber extends BaseAppCompat {
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
            showDialog();
            try {
                Fyber.with(data.get("app_id"), this)
                        .withSecurityToken(data.get("security_token"))
                        .withUserId(user)
                        .start();
                OfferWallRequester.create(new RequestCallback() {
                    @Override
                    public void onAdAvailable(Intent intent) {
                        if (!isLive) return;
                        startActivity(intent);
                        new Handler().postDelayed(() -> finish(), 1000);
                        Home.checkBalance = 1;
                    }

                    @Override
                    public void onAdNotAvailable(AdFormat adFormat) {
                        if (!isLive) return;
                        if (dialog.isShowing()) dialog.dismiss();
                        uiToast(fyber.this, "Offer not available");
                        finish();
                    }

                    @Override
                    public void onRequestError(RequestError requestError) {
                        if (!isLive) return;
                        if (dialog.isShowing()) dialog.dismiss();
                        uiToast(fyber.this, "" + requestError.getDescription());
                        finish();
                    }
                }).request(this);
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

    private void showDialog() {
        dialog = Misc.loadingDiagExit(this);
        dialog.show();
    }

    private void uiToast(final Activity context, final String toast) {
        context.runOnUiThread(() -> Toast.makeText(context, toast, Toast.LENGTH_LONG).show());
    }
}