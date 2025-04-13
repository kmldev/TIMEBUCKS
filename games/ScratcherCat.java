package org.mintsoft.mintly.games;

import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetGame;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.util.ArrayList;
import java.util.HashMap;

public class ScratcherCat extends BaseAppCompat {
    private Dialog loadingDialog, askDiag, conDiag;
    private TextView diagTitleView, diagQtyView, diagCostView;
    private ViewPager2 viewPager;
    private int defaultPos, pos, decorated, qty, bal, cost;
    private scAdapter adapter;
    private String id, cid;
    private ArrayList<HashMap<String, String>> list;
    private ActivityResultLauncher<Intent> activityForResult;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (Home.gams.contains("sc")) {
            finish();
            return;
        }
        loadingDialog = Misc.loadingDiag(this);
        loadingDialog.show();
        defaultPos = getIntent().getIntExtra("id", 0);
        setContentView(R.layout.game_scratcher_cat);
        TextView titleView = findViewById(R.id.scratcher_title);
        titleView.setText(DataParse.getStr(this,"scratch_cards",Home.spf));
        viewPager = findViewById(R.id.scratcher_viewPager);
        findViewById(R.id.scratcher_back).setOnClickListener(view -> finish());
        list = Variables.getArrayHash("scratcher_cat");
        if (list == null) {
            callNet();
        } else {
            initList();
        }
        activityForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    if (resultCode == 8) {
                        for (int j = 0; j < list.get(pos).size() - 7; j++) {
                            String d = list.get(pos).get(String.valueOf(j));
                            if (d != null) {
                                String[] strs = d.split("@");
                                if (strs[0].equals(id)) {
                                    list.get(pos).put(String.valueOf(j), null);
                                    adapter.notifyDataSetChanged();
                                    break;
                                }
                            }
                        }
                    } else if (resultCode == 10) {
                        callNet();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Variables.getHash("show_offers") != null) finish();
    }

    @Override
    protected void onDestroy() {
        Variables.setArrayHash("scratcher_cat", list);
        super.onDestroy();
    }

    private void callNet() {
        if (!loadingDialog.isShowing()) loadingDialog.show();
        GetGame.getScratcher(this, new onResponse() {
            @Override
            public void onSuccess(String response) {
                Home.balance = response;
            }

            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> l) {
                list = l;
                initList();
            }

            @Override
            public void onError(int errorCode, String error) {
                loadingDialog.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, ScratcherCat.this, () -> {
                        callNet();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(ScratcherCat.this, error, Toast.LENGTH_LONG).show();
                    finish();
                }

            }
        });
    }

    private void initList() {
        adapter = new scAdapter(this);
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        int nextItemVisiblePx = 80;
        int currentItemHorizontalMarginPx = 80;
        int pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx;
        ViewPager2.PageTransformer pageTransformer = (page, position) -> {
            page.setTranslationX(-pageTranslationX * position);
            page.setScaleY(1 - (0.1f * abs(position)));
            page.setAlpha(0.25f + (1 - abs(position)));

        };
        viewPager.setPageTransformer(pageTransformer);
        if (decorated == 0) {
            decorated = 1;
            itemDecoration decoration = new itemDecoration(currentItemHorizontalMarginPx);
            viewPager.addItemDecoration(decoration);
        }
        viewPager.post(() -> {
            if (defaultPos == 0 && list.size() > 2) {
                defaultPos = 1;
            } else if (defaultPos != 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).get("id").equals(String.valueOf(defaultPos))) {
                        defaultPos = i;
                        break;
                    }
                }
            }
            viewPager.setCurrentItem(defaultPos);
            new Handler().postDelayed(() -> {
                if (loadingDialog.isShowing()) loadingDialog.dismiss();
            }, 1000);
        });
    }

    public static class itemDecoration extends RecyclerView.ItemDecoration {
        private final int hMargin;

        public itemDecoration(int hMargin) {
            this.hMargin = hMargin + 10;
        }

        @Override
        public void getItemOffsets(Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            outRect.right = hMargin;
            outRect.left = hMargin;
        }
    }

    private class scAdapter extends RecyclerView.Adapter<scAdapter.viewHolder> {
        private final Context context;
        private final LayoutInflater inflater;

        public scAdapter(Context context) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.game_scratcher_cat_list, parent, false);
            TextView pchsView = view.findViewById(R.id.scratcher_list_pchs);
            pchsView.setText(DataParse.getStr(context, "purchase_card", Home.spf));
            return new viewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull viewHolder holder, int p) {
            int position = holder.getAbsoluteAdapterPosition();
            Picasso.get().load(list.get(position).get("icon")).into(holder.cardImage);
            holder.titleView.setText(list.get(position).get("name"));
            int size = list.get(position).size() - 7;
            holder.listView.removeAllViews();
            if (size > 0) {
                holder.emptyView.setVisibility(View.GONE);
                for (int j = 0; j < size; j++) {
                    String str = list.get(position).get(String.valueOf(j));
                    if (str != null) {
                        String[] strs = str.split("@");
                        View iView = inflater.inflate(R.layout.game_scratcher_cat_item, null);
                        TextView cV = iView.findViewById(R.id.scratcher_item_recView);
                        TextView eV = iView.findViewById(R.id.scratcher_item_expView);
                        cV.setText(("Rec on: " + strs[1]));
                        eV.setText(("Exp date: " + strs[2]));
                        iView.setOnClickListener(view -> {
                            id = strs[0];
                            pos = position;
                            Intent intent = new Intent(context, Scratcher.class);
                            intent.putExtra("name", list.get(position).get("name"));
                            intent.putExtra("image", list.get(position).get("bg"));
                            intent.putExtra("coord", list.get(position).get("coord"));
                            intent.putExtra("id", id);
                            activityForResult.launch(intent);
                        });
                        holder.listView.addView(iView);
                    }
                }
            } else {
                holder.emptyView.setVisibility(View.VISIBLE);
            }
            holder.countView.setText(String.valueOf(size));
            if (list.get(position).get("purchase").equals("1")) {
                holder.pBtn.setVisibility(View.VISIBLE);
                holder.pBtn.setOnClickListener(view ->
                        showAskDiag(
                                list.get(position).get("name"),
                                list.get(position).get("id"),
                                list.get(position).get("cost")
                        )
                );
            } else {
                holder.pBtn.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class viewHolder extends RecyclerView.ViewHolder {
            final LinearLayout listView;
            final ImageView cardImage, emptyView;
            final TextView titleView, countView;
            final LinearLayout pBtn;

            public viewHolder(@NonNull View itemView) {
                super(itemView);
                cardImage = itemView.findViewById(R.id.scratcher_list_imageView);
                emptyView = itemView.findViewById(R.id.scratcher_list_emptyView);
                titleView = itemView.findViewById(R.id.scratcher_list_titleView);
                countView = itemView.findViewById(R.id.scratcher_list_countView);
                pBtn = itemView.findViewById(R.id.scratcher_list_purchaseBtn);
                listView = itemView.findViewById(R.id.scratcher_list_listView);
            }
        }
    }

    private void showAskDiag(String title, String id, String cst) {
        bal = Integer.parseInt(Home.balance);
        cost = Integer.parseInt(cst);
        cid = id;
        defaultPos = Integer.parseInt(id);
        if (askDiag == null) {
            askDiag = Misc.decoratedDiag(this, R.layout.dialog_sc_purchase, 0.8f);
            TextView askView = askDiag.findViewById(R.id.dialog_sc_askView);
            askView.setText( DataParse.getStr(this,"scratcher_diag_ask",Home.spf));
            diagTitleView = askDiag.findViewById(R.id.dialog_sc_titleView);
            diagQtyView = askDiag.findViewById(R.id.dialog_sc_qtyView);
            diagCostView = askDiag.findViewById(R.id.dialog_sc_cost);
            TextView costTitle = askDiag.findViewById(R.id.dialog_sc_costTitle);
            costTitle.setText( DataParse.getStr(this,"cost",Home.spf));
            askDiag.findViewById(R.id.dialog_sc_plus).setOnClickListener(view -> {
                if (bal < cost * qty) {
                    Toast.makeText(ScratcherCat.this, DataParse.getStr(this,"insufficient_balance",Home.spf), Toast.LENGTH_LONG).show();
                } else {
                    qty += 1;
                    diagCostView.setText(String.valueOf(cost * qty));
                    diagQtyView.setText(String.valueOf(qty));
                }
            });
            askDiag.findViewById(R.id.dialog_sc_minus).setOnClickListener(view -> {
                if (qty > 1) {
                    qty -= 1;
                    diagCostView.setText(String.valueOf(cost * qty));
                    diagQtyView.setText(String.valueOf(qty));
                }
            });
            Button cancelView = askDiag.findViewById(R.id.dialog_sc_cancel);
            cancelView.setText(DataParse.getStr(this,"cancl",Home.spf));
            cancelView.setOnClickListener(view -> {
                qty = 0;
                diagCostView.setText("0");
                askDiag.dismiss();
            });
            Button submitView = askDiag.findViewById(R.id.dialog_sc_submit);
            submitView.setText(DataParse.getStr(this,"submit",Home.spf));
            submitView.setOnClickListener(view -> {
                if (qty != 0) {
                    askDiag.dismiss();
                    makePurchase();
                }
            });
        }
        diagTitleView.setText(title);
        diagQtyView.setText(String.valueOf(qty));
        askDiag.show();
    }

    private void makePurchase() {
        loadingDialog.show();
        qty = Integer.parseInt(diagQtyView.getText().toString());
        GetGame.scratcherPurchase(this, cid, qty, new onResponse() {
            @Override
            public void onSuccess(String response) {
                qty = 0;
                diagCostView.setText("0");
                Toast.makeText(ScratcherCat.this, response, Toast.LENGTH_LONG).show();
                callNet();
            }

            @Override
            public void onError(int errorCode, String error) {
                qty = 0;
                diagCostView.setText("0");
                loadingDialog.dismiss();
                Toast.makeText(ScratcherCat.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}