package org.mintsoft.mintly.frags;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintlib.GetURL;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.History;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.util.HashMap;

public class FragInvite extends Fragment {
    private Context context;
    private boolean isLive;
    private String dLink, code;
    private Dialog conDiag;
    private ImageView copyLinkBtn;
    private TextView refAmtView, descView, urlView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        View v = inflater.inflate(R.layout.frag_invite, container, false);
        if (context == null || getActivity() == null) return v;
        isLive = true;
        TextView refearnT = v.findViewById(R.id.frag_invite_refearnT);
        refearnT.setText(DataParse.getStr(context, "ref_n_earn", Home.spf));
        TextView codeT = v.findViewById(R.id.frag_invite_codeT);
        codeT.setText(DataParse.getStr(context, "your_ref_code", Home.spf));
        TextView copyT = v.findViewById(R.id.frag_invite_copyT);
        copyT.setText(DataParse.getStr(context, "copy", Home.spf));
        TextView codeT2 = v.findViewById(R.id.frag_invite_codeT2);
        codeT2.setText(DataParse.getStr(context, "code", Home.spf));
        TextView orT = v.findViewById(R.id.frag_invite_orT);
        orT.setText(DataParse.getStr(context, "or", Home.spf));
        TextView shareT = v.findViewById(R.id.frag_invite_shareT);
        shareT.setText(DataParse.getStr(context, "share_ref_via", Home.spf));
        TextView tgT = v.findViewById(R.id.frag_invite_tgT);
        tgT.setText(DataParse.getStr(context, "telegram", Home.spf));
        TextView fbT = v.findViewById(R.id.frag_invite_fbT);
        fbT.setText(DataParse.getStr(context, "facebook", Home.spf));
        TextView waT = v.findViewById(R.id.frag_invite_waT);
        waT.setText(DataParse.getStr(context, "whatsapp", Home.spf));
        TextView refCodeView = v.findViewById(R.id.frag_invite_codeView);
        urlView = v.findViewById(R.id.frag_invite_refUrl_inputView);
        urlView.setText(DataParse.getStr(context, "please_wait", Home.spf));
        refAmtView = v.findViewById(R.id.frag_invite_referrer_amtView);
        descView = v.findViewById(R.id.frag_invite_descView);
        code = GetAuth.user(context);
        refCodeView.setText(code);
        copyLinkBtn = v.findViewById(R.id.frag_invite_copyLink_btn);
        dLink = "https://" + getString(R.string.domain_name) + "/j/" + GetAuth.user(context);
        setLink();
        v.findViewById(R.id.frag_invite_copyBtn).setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("ReferralCode", code);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, DataParse.getStr(context, "ref_code_copied", Home.spf), Toast.LENGTH_LONG).show();
        });
        Button goHist = v.findViewById(R.id.frag_invite_go_history);
        goHist.setText(DataParse.getStr(context, "ref_history", Home.spf));
        goHist.setOnClickListener(view -> {
            Intent intent = new Intent(context, History.class);
            intent.putExtra("pos", 2);
            startActivity(intent);
        });
        v.findViewById(R.id.frag_invite_go_telegram).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage("org.telegram.messenger");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Join " + DataParse.getStr(context, "app_name", Home.spf) + ":");
            intent.putExtra(Intent.EXTRA_TEXT, dLink);
            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(context, "Telegram is not been installed.", Toast.LENGTH_LONG).show();
            }
        });
        v.findViewById(R.id.frag_invite_go_facebook).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage("com.facebook.katana");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Join " + DataParse.getStr(context, "app_name", Home.spf) + ":");
            intent.putExtra(Intent.EXTRA_TEXT, dLink);
            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                try {
                    intent.setPackage("com.facebook.lite");
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException ex) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/sharer/sharer.php?u=" + dLink)));
                }
            }
        });
        v.findViewById(R.id.frag_invite_go_whatsapp).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage("com.whatsapp");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Join " + DataParse.getStr(context, "app_name", Home.spf) + ":");
            intent.putExtra(Intent.EXTRA_TEXT, dLink);
            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(context, "Whatsapp is not been installed.", Toast.LENGTH_LONG).show();
            }
        });
        callNet();
        return v;
    }

    @Override
    public void onDestroy() {
        isLive = false;
        super.onDestroy();
    }

    private void callNet() {
        String iDesc = Variables.getHash("inv_desc");
        if (iDesc == null) {
            if (!Home.loadingDiag.isShowing()) Home.loadingDiag.show();
            GetURL.getRef(context, new onResponse() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onSuccessHashMap(HashMap<String, String> data) {
                    Variables.setHash("inv_ref", data.get("ref"));
                    String invDesc = DataParse.getStr(context, "ref_desc_1", Home.spf) + " " + data.get("user")
                            + " " + Home.currency.toLowerCase() + "s " + DataParse.getStr(context, "ref_desc_2", Home.spf)
                            + " " + data.get("ref") + " " + Home.currency.toLowerCase()
                            + "s " + DataParse.getStr(context, "ref_desc_3", Home.spf);
                    Variables.setHash("inv_desc", invDesc);
                    if (!isLive) return;
                    Home.loadingDiag.dismiss();
                    refAmtView.setText(data.get("ref"));
                    descView.setText(invDesc);
                }

                @Override
                public void onError(int errorCode, String error) {
                    if (!isLive) return;
                    Home.loadingDiag.dismiss();
                    if (errorCode == -9) {
                        conDiag = Misc.noConnection(conDiag, context, () -> {
                            callNet();
                            conDiag.dismiss();
                        });
                    } else {
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            refAmtView.setText(Variables.getHash("inv_ref"));
            descView.setText(iDesc);
        }
    }

    private void setLink() {
        urlView.setText(dLink);
        urlView.setTypeface(Typeface.SANS_SERIF);
        urlView.setPadding(Misc.dpToPx(context, 10), 0, Misc.dpToPx(context, 42), 0);
        copyLinkBtn.setVisibility(View.VISIBLE);
        copyLinkBtn.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("ReferralLink", urlView.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, DataParse.getStr(context, "ref_link_copied", Home.spf), Toast.LENGTH_LONG).show();
        });
    }
}
