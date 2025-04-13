package org.mintsoft.mintly.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintlib.GetURL;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.History;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.HashMap;

public class Refs extends BaseAppCompat {
    private Window w;
    private String dLink, code;
    private Dialog conDiag, loadingDiag;
    private ImageView copyLinkBtn;
    private TextView refAmtView, descView, urlView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        loadingDiag = Misc.loadingDiag(this);
        loadingDiag.show();
        w = getWindow();
        w.setNavigationBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        setContentView(R.layout.refs);
        TextView titleView = findViewById(R.id.refs_title);
        titleView.setText(DataParse.getStr(this, "referral_system", Home.spf));
        TextView refearnT = findViewById(R.id.refs_refearnT);
        refearnT.setText(DataParse.getStr(this, "ref_n_earn", Home.spf));
        TextView codeT = findViewById(R.id.refs_codeT);
        codeT.setText(DataParse.getStr(this, "your_ref_code", Home.spf));
        TextView copyT = findViewById(R.id.refs_copyT);
        copyT.setText(DataParse.getStr(this, "copy", Home.spf));
        TextView codeT2 = findViewById(R.id.refs_codeT2);
        codeT2.setText(DataParse.getStr(this, "code", Home.spf));
        TextView orT = findViewById(R.id.refs_orT);
        orT.setText(DataParse.getStr(this, "or", Home.spf));
        TextView shareT = findViewById(R.id.refs_shareT);
        shareT.setText(DataParse.getStr(this, "share_ref_via", Home.spf));
        TextView tgT = findViewById(R.id.refs_tgT);
        tgT.setText(DataParse.getStr(this, "telegram", Home.spf));
        TextView fbT = findViewById(R.id.refs_fbT);
        fbT.setText(DataParse.getStr(this, "facebook", Home.spf));
        TextView waT = findViewById(R.id.refs_waT);
        waT.setText(DataParse.getStr(this, "whatsapp", Home.spf));
        TextView refCodeView = findViewById(R.id.refs_codeView);
        urlView = findViewById(R.id.refs_refUrl_inputView);
        urlView.setText(DataParse.getStr(this, "please_wait", Home.spf));
        refAmtView = findViewById(R.id.refs_referrer_amtView);
        descView = findViewById(R.id.refs_descView);
        code = GetAuth.user(Refs.this);
        refCodeView.setText(code);
        copyLinkBtn = findViewById(R.id.refs_copyLink_btn);
        dLink = "https://" + getString(R.string.domain_name) + "/j/" + GetAuth.user(this);
        setLink();
        findViewById(R.id.refs_copyBtn).setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("ReferralCode", code);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(Refs.this, DataParse.getStr(this, "ref_code_copied", Home.spf), Toast.LENGTH_LONG).show();
        });
        Button goHist = findViewById(R.id.refs_go_history);
        goHist.setText(DataParse.getStr(this, "ref_history", Home.spf));
        goHist.setOnClickListener(view -> {
            Intent intent = new Intent(Refs.this, History.class);
            intent.putExtra("pos", 2);
            startActivity(intent);
        });
        findViewById(R.id.refs_back).setOnClickListener(view -> finish());
        findViewById(R.id.refs_go_telegram).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage("org.telegram.messenger");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Join " + DataParse.getStr(this, "app_name", Home.spf) + ":");
            intent.putExtra(Intent.EXTRA_TEXT, dLink);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(Refs.this, "Telegram is not been installed.", Toast.LENGTH_LONG).show();
            }
        });
        findViewById(R.id.refs_go_facebook).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage("com.facebook.katana");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Join " + DataParse.getStr(this, "app_name", Home.spf) + ":");
            intent.putExtra(Intent.EXTRA_TEXT, dLink);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                try {
                    intent.setPackage("com.facebook.lite");
                    startActivity(intent);
                } catch (ActivityNotFoundException ex) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/sharer/sharer.php?u=" + dLink)));
                }
            }
        });
        findViewById(R.id.refs_go_whatsapp).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage("com.whatsapp");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Join " + DataParse.getStr(this, "app_name", Home.spf) + ":");
            intent.putExtra(Intent.EXTRA_TEXT, dLink);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(Refs.this, "Whatsapp is not been installed.", Toast.LENGTH_LONG).show();
            }
        });
        callNet();
    }

    @Override
    protected void onDestroy() {
        w.setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        super.onDestroy();
    }

    private void callNet() {
        if (!loadingDiag.isShowing()) loadingDiag.show();
        GetURL.getRef(this, new onResponse() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccessHashMap(HashMap<String, String> data) {
                refAmtView.setText(data.get("ref"));
                descView.setText((DataParse.getStr(Refs.this, "ref_desc_1", Home.spf) + " " + data.get("user") + " " + Home.currency.toLowerCase() + "s " + DataParse.getStr(Refs.this, "ref_desc_2", Home.spf) + " " + data.get("ref") + " " + Home.currency.toLowerCase() + "s " + DataParse.getStr(Refs.this, "ref_desc_3", Home.spf)));
                loadingDiag.dismiss();
            }

            @Override
            public void onError(int errorCode, String error) {
                loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, Refs.this, () -> {
                        callNet();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(Refs.this, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setLink() {
        urlView.setText(dLink);
        urlView.setTypeface(Typeface.SANS_SERIF);
        urlView.setPadding(Misc.dpToPx(this, 10), 0, Misc.dpToPx(this, 42), 0);
        copyLinkBtn.setVisibility(View.VISIBLE);
        copyLinkBtn.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("ReferralLink", urlView.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(Refs.this, DataParse.getStr(this, "ref_link_copied", Home.spf), Toast.LENGTH_LONG).show();
        });
    }
}