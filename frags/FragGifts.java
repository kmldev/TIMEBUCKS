package org.mintsoft.mintly.frags;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintlib.GetURL;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.util.ArrayList;
import java.util.HashMap;

public class FragGifts extends Fragment {
    private View v;
    private Context context;
    private Activity activity;
    public String currency;
    private Dialog conDiag, confirmDiag;
    private boolean isLive;
    private String diagType, diagWid;
    private TextView diagDesc, diagTitle, diagSubTitle;
    private EditText diagInput;
    private ShapeableImageView diagImageView;
    private TextView balView;
    private ArrayList<String> tabs;
    public ArrayList<HashMap<String, String>> list;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        activity = getActivity();
        v = inflater.inflate(R.layout.frag_gifts, container, false);
        if (context == null || activity == null) return v;
        isLive = true;
        if (!Home.canRedeem) {
            ((Home) getActivity()).changeTab(0);
            return v;
        }
        TextView bannerTitle = v.findViewById(R.id.frag_gifts_bannertitle);
        bannerTitle.setText(DataParse.getStr(context, "gift_ex_text", Home.spf));
        TextView yourBal = v.findViewById(R.id.frag_gifts_yourbal);
        yourBal.setText(DataParse.getStr(context, "your_balance", Home.spf));
        balView = v.findViewById(R.id.frag_gifts_balView);
        balView.setText(Home.balance);
        currency = Home.currency.toLowerCase() + "s";
        list = Variables.getArrayHash("gift_list");
        if (list == null) {
            callNet();
        } else {
            long cTime = System.currentTimeMillis();
            long sT = Home.spf.getLong("r_time", cTime);
            if (sT <= cTime) {
                balView.setText(" " + DataParse.getStr(context, "checking", Home.spf) + " ");
                checkBal();
            } else {
                initList();
            }
        }
        return v;
    }

    @Override
    public void onDestroy() {
        isLive = false;
        super.onDestroy();
    }

    private void callNet() {
        if (!Home.loadingDiag.isShowing()) Home.loadingDiag.show();
        GetURL.getGifts(context, new onResponse() {
            @Override
            public void onSuccess(String response) {
                Home.spf.edit().putLong("r_time", System.currentTimeMillis() + Home.delay).commit();
                Home.balance = response;
                balView.setText(Home.balance);
            }

            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> l) {
                Variables.setArrayHash("gift_list", l);
                if (!isLive) return;
                Home.loadingDiag.dismiss();
                list = l;
                initList();
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
    }

    private void initList() {
        tabs = new ArrayList<>();
        for (int i = 0; i < list.size() - 1; i++) {
            tabs.add(list.get(i).get("name"));
        }
        pagerAdapter adapter = new pagerAdapter(this);
        ViewPager2 viewPager = v.findViewById(R.id.frag_gifts_viewPager);
        viewPager.setAdapter(adapter);
        viewPager.setSaveEnabled(false);
        TabLayout tabLayout = v.findViewById(R.id.frag_gifts_tabLayout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(tabs.get(position))).attach();
    }

    private class pagerAdapter extends FragmentStateAdapter {

        public pagerAdapter(Fragment fm) {
            super(fm);
        }

        @Override
        public int getItemCount() {
            return list.size() - 1;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return FragGiftsTab.newInstance(list.get(position));
        }
    }

    public void confirmDiag(String imageUrl, String title, String subTitle, String desc, String type, String wid) {
        diagType = type;
        diagWid = wid;
        if (confirmDiag == null) {
            confirmDiag = Misc.decoratedDiag(context, R.layout.dialog_gift, 0.8f);
            confirmDiag.setCancelable(false);
            diagImageView = confirmDiag.findViewById(R.id.dialog_redeem_imageView);
            Picasso.get().load(imageUrl).error(R.drawable.ic_warning).into(diagImageView);
            diagTitle = confirmDiag.findViewById(R.id.dialog_redeem_title);
            diagTitle.setText(title);
            diagSubTitle = confirmDiag.findViewById(R.id.dialog_redeem_subTitle);
            diagSubTitle.setText(subTitle);
            diagDesc = confirmDiag.findViewById(R.id.dialog_redeem_desc);
            diagDesc.setText(Misc.html(desc));
            diagInput = confirmDiag.findViewById(R.id.dialog_redeem_edittext);
            diagInput.setHint(DataParse.getStr(context, "write_here", Home.spf));
            setInputType();
            TextView cancelBtn = confirmDiag.findViewById(R.id.dialog_redeem_close);
            cancelBtn.setText(DataParse.getStr(context, "cancl", Home.spf));
            cancelBtn.setOnClickListener(view -> confirmDiag.dismiss());
            TextView submitBtn = confirmDiag.findViewById(R.id.dialog_redeem_btn);
            submitBtn.setText(DataParse.getStr(context, "request_this_item", Home.spf));
            submitBtn.setOnClickListener(view -> {
                String inputData = diagInput.getText().toString();
                if (TextUtils.isEmpty(inputData)) {
                    Misc.showMessage(context, DataParse.getStr(context, "fill_input", Home.spf), false);
                    return;
                }
                if (diagType.equals("2")) {
                    if (!Patterns.EMAIL_ADDRESS.matcher(inputData).matches()) {
                        Misc.showMessage(context, DataParse.getStr(context, "invalid_email", Home.spf), false);
                        return;
                    }
                } else if (diagType.equals("3")) {
                    if (!TextUtils.isDigitsOnly(inputData)) {
                        Misc.showMessage(context, DataParse.getStr(context, "invalid_number", Home.spf), false);
                        return;
                    }
                }
                requestRedeem(diagWid, inputData);
            });
        } else {
            Picasso.get().load(imageUrl).error(R.drawable.ic_warning).into(diagImageView);
            diagTitle.setText(title);
            diagSubTitle.setText(subTitle);
            diagDesc.setText(Misc.html(desc));
            setInputType();
        }
        confirmDiag.show();
    }

    private void setInputType() {
        int padding = Misc.dpToPx(context, 10);
        switch (diagType) {
            case "1":
                diagInput.setSingleLine(false);
                diagInput.setPadding(padding, padding, padding, padding);
                diagInput.setGravity(Gravity.TOP);
                diagInput.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
                diagInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                diagInput.setLines(4);
                diagInput.setMaxLines(7);
                diagInput.setVerticalScrollBarEnabled(true);
                diagInput.setMovementMethod(ScrollingMovementMethod.getInstance());
                diagInput.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
                break;
            case "2":
                diagInput.setPadding(padding, 0, padding, 0);
                diagInput.setGravity(Gravity.CENTER_VERTICAL);
                diagInput.setSingleLine(true);
                diagInput.setImeOptions(EditorInfo.IME_FLAG_NAVIGATE_NEXT);
                diagInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                diagInput.setLines(1);
                break;
            case "3":
                diagInput.setPadding(padding, 0, padding, 0);
                diagInput.setGravity(Gravity.CENTER_VERTICAL);
                diagInput.setSingleLine(true);
                diagInput.setImeOptions(EditorInfo.IME_FLAG_NAVIGATE_NEXT);
                diagInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                diagInput.setLines(1);
                break;
        }
    }

    private void requestRedeem(String wid, String toAc) {
        if (confirmDiag.isShowing()) confirmDiag.dismiss();
        if (!Home.loadingDiag.isShowing()) Home.loadingDiag.show();
        GetURL.requestGift(context, wid, toAc, new onResponse() {
            @Override
            public void onSuccess(String response) {
                Home.balance = String.valueOf(Integer.parseInt(Home.balance) - Integer.parseInt(response));
                if (!isLive) return;
                Home.loadingDiag.dismiss();
                Variables.setArrayHash("gift_list", null);
                diagInput.setText("");
                Misc.showMessage(context, DataParse.getStr(context, "gift_request", Home.spf), false);
            }

            @Override
            public void onError(int errorCode, String error) {
                if (!isLive) return;
                Home.loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, context, () -> {
                        requestRedeem(wid, toAc);
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void checkBal() {
        if (Home.cbl) return;
        Home.cbl = true;
        if (!Home.loadingDiag.isShowing()) Home.loadingDiag.show();
        GetAuth.balance(context, Home.spf, new onResponse() {
            @SuppressLint("ApplySharedPref")
            @Override
            public void onSuccessHashMap(HashMap<String, String> data1) {
                Home.cbl = false;
                Home.spf.edit().putLong("r_time", System.currentTimeMillis() + Home.delay).commit();
                Home.balance = data1.get("balance");
                if (!isLive) return;
                ((Home) activity).balView.setText(data1.get("balance"));
                Home.loadingDiag.dismiss();
                balView.setText(Home.balance);
                initList();
            }

            @Override
            public void onError(int i, String s) {
                Home.cbl = false;
                if (!isLive) return;
                Home.loadingDiag.dismiss();
            }
        });
    }
}