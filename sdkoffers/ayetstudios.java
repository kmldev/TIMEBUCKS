package org.mintsoft.mintly.sdkoffers;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.ayetstudios.publishersdk.AyetSdk;
import com.ayetstudios.publishersdk.interfaces.UserBalanceCallback;
import com.ayetstudios.publishersdk.messages.SdkUserBalance;

import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.HashMap;

public class ayetstudios extends BaseAppCompat {
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
                AyetSdk.init(getApplication(), user, new UserBalanceCallback() {
                    @Override
                    public void userBalanceChanged(SdkUserBalance sdkUserBalance) {

                    }

                    @Override
                    public void userBalanceInitialized(SdkUserBalance sdkUserBalance) {
                        if (!isLive) return;
                        AyetSdk.showOfferwall(getApplication(), data.get("slot_name"));
                        new Handler().postDelayed(() -> {
                            if (dialog.isShowing()) dialog.dismiss();
                            finish();
                        }, 1000);
                        Home.checkBalance = 1;
                    }

                    @Override
                    public void initializationFailed() {
                        if (!isLive) return;
                        if (dialog.isShowing()) dialog.dismiss();
                        Toast.makeText(ayetstudios.this, "Could not connect! Did you set your APP API KEY?", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }, data.get("app_key"));
            } catch (Exception e) {
                if (!isLive) return;
                Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        isLive = false;
		super.onDestroy();
    }

    private void showDialog() {
        dialog = Misc.loadingDiagExit(this);
        dialog.show();
    }
}