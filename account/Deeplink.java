package org.mintsoft.mintly.account;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.Splash;
import org.mintsoft.mintly.helper.Misc;

public class Deeplink extends AppCompatActivity {
    private TextView textView, okBtn;
    private Dialog loadingDiag;
    private LinearLayout holder;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
        } else {
            setContentView(R.layout.deeplink);
            loadingDiag = Misc.loadingDiag(this);
            loadingDiag.show();
            TextView titleView = findViewById(R.id.deeplink_title);
            titleView.setText(DataParse.getStr(this, "app_name", Home.spf));
            holder = findViewById(R.id.deeplink_holder);
            holder.setVisibility(View.GONE);
            textView = findViewById(R.id.deeplink_textView);
            okBtn = findViewById(R.id.deeplink_okBtn);
            okBtn.setText(DataParse.getStr(this, "ok", Home.spf));
            String url = uri.toString();
            if (url.contains("/register/confirm")) {
                GetAuth.confirmReg(this, url.replace("app://", "https://"), new onResponse() {
                    @Override
                    public void onSuccess(String s) {
                        loadingDiag.dismiss();
                        Intent intent = new Intent(getApplicationContext(), Splash.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(int i, String s) {
                        if (i != 9) textView.setText(s);
                        okBtn.setText(DataParse.getStr(Deeplink.this, "cancl", Home.spf));
                        okBtn.setTextColor(Color.YELLOW);
                        okBtn.setOnClickListener(view -> finishAndRemoveTask());
                        holder.setVisibility(View.VISIBLE);
                        loadingDiag.dismiss();
                    }
                });
            } else if (url.contains("/login/reset")) {
                GetAuth.resetPass(this, url.replace("app://", "https://"), new onResponse() {
                    @Override
                    public void onSuccess(String s) {
                        textView.setText(s);
                        okBtn.setOnClickListener(view -> {
                            GetAuth.removeCred(Deeplink.this);
                            Intent intent = new Intent(getApplicationContext(), Splash.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        });
                        holder.setVisibility(View.VISIBLE);
                        loadingDiag.dismiss();
                    }

                    @Override
                    public void onError(int i, String s) {
                        if (i != 9) textView.setText(s);
                        okBtn.setText(DataParse.getStr(Deeplink.this, "cancl", Home.spf));
                        okBtn.setTextColor(Color.YELLOW);
                        okBtn.setOnClickListener(view -> finishAndRemoveTask());
                        holder.setVisibility(View.VISIBLE);
                        loadingDiag.dismiss();
                    }
                });
            } else {
                finish();
            }
        }
    }
}