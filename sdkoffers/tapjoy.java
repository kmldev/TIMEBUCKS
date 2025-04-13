package org.mintsoft.mintly.sdkoffers;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.tapjoy.TJActionRequest;
import com.tapjoy.TJConnectListener;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJSetUserIDListener;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyConnectFlag;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.HashMap;
import java.util.Hashtable;

public class tapjoy extends BaseAppCompat {
    private TJSetUserIDListener tjSetUserIDListener;
    private TJConnectListener tjConnectListener;
    private TJPlacement tjPlacement;
    private TJPlacementListener tjPlacementListener;
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
            tjPlacementListener = new TJPlacementListener() {
                @Override
                public void onRequestSuccess(TJPlacement tjPlacement) {
                    if (!isLive) return;
                    if (!tjPlacement.isContentAvailable()) {
                        if (dialog.isShowing()) dialog.dismiss();
                        uiToast(tapjoy.this, DataParse.getStr(tapjoy.this, "ad_not_available", Home.spf));
                        finish();
                    }
                }

                @Override
                public void onRequestFailure(TJPlacement tjPlacement, TJError tjError) {
                    if (!isLive) return;
                    if (dialog.isShowing()) dialog.dismiss();
                    uiToast(tapjoy.this, tjError.message);
                    finish();
                }

                @Override
                public void onContentReady(TJPlacement tjPlacement) {
                    if (!isLive) return;
                    tjPlacement.showContent();
                    if (dialog.isShowing()) dialog.dismiss();
                    Home.checkBalance = 1;
                }

                @Override
                public void onContentShow(TJPlacement tjPlacement) {

                }

                @Override
                public void onContentDismiss(TJPlacement tjPlacement) {
                    if (!isLive) return;
                    finish();
                }

                @Override
                public void onPurchaseRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s) {

                }

                @Override
                public void onRewardRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s, int i) {

                }

                @Override
                public void onClick(TJPlacement tjPlacement) {

                }
            };
            tjPlacement = new TJPlacement(this, data.get("placement_name"), tjPlacementListener);
            tjSetUserIDListener = new TJSetUserIDListener() {
                @Override
                public void onSetUserIDSuccess() {
                    if (!isLive) return;
                    if (!isFinishing() && !isDestroyed())
                        tjPlacement.requestContent();
                }

                @Override
                public void onSetUserIDFailure(String s) {
                    if (!isLive) return;
                    if (dialog.isShowing()) dialog.dismiss();
                    uiToast(tapjoy.this, "" + s);
                    finish();
                }
            };
            tjConnectListener = new TJConnectListener() {
                @Override
                public void onConnectSuccess() {
                    super.onConnectSuccess();
                    if (!isLive) return;
                    Tapjoy.setUserID(user, tjSetUserIDListener);
                }

                @Override
                public void onConnectFailure(int code, String message) {
                    super.onConnectFailure(code, message);
                    if (!isLive) return;
                    if (dialog.isShowing()) dialog.dismiss();
                    uiToast(tapjoy.this, "" + message);
                    finish();
                }
            };
            Hashtable<String, Object> connectFlags = new Hashtable<>();
            connectFlags.put(TapjoyConnectFlag.ENABLE_LOGGING, "true");
            Tapjoy.connect(this, data.get("sdk_key"), connectFlags, tjConnectListener);
        } else {
            finish();
        }
    }

    @Override
    protected void onStop() {
        isLive = false;
        tjPlacementListener = null;
        tjPlacement = null;
        tjSetUserIDListener = null;
        tjConnectListener = null;
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