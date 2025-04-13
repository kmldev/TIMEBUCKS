package org.mintsoft.mintly.frags;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.imageview.ShapeableImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.Customoffers;
import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintlib.GetNet;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.CircleIndicator;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.PopupNotif;
import org.mintsoft.mintly.helper.Surf;
import org.mintsoft.mintly.helper.Variables;
import org.mintsoft.mintly.helper.iAdapter;
import org.mintsoft.mintly.offers.APIOffers;
import org.mintsoft.mintly.offers.PPVOffers;
import org.mintsoft.mintly.offers.TaskOffers;
import org.mintsoft.mintly.offers.Yt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class FragOffers extends Fragment {
    private View v;
    private boolean isLive;
    private Context context;
    private Activity activity;
    private ViewPager vPager;
    private iAdapter adapter;
    private Timer timer;
    private Dialog conDiag;
    private String packageName, userId;
    private LayoutInflater inflaters;
    private LinearLayout layout;
    private boolean shouldReload;
    private ArrayList<HashMap<String, String>> listP;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        activity = getActivity();
        v = inflater.inflate(R.layout.frag_offers, container, false);
        if (context == null || activity == null) return v;
        isLive = true;
        CollapsingToolbarLayout collView = v.findViewById(R.id.frag_offers_coll);
        collView.setTitle(DataParse.getStr(context, "offerwalls", Home.spf));
        layout = v.findViewById(R.id.frag_offers_listHolder);
        v.findViewById(R.id.frag_offers_back).setOnClickListener(view -> ((Home) activity).openDrawer());
        inflaters = LayoutInflater.from(context);
        packageName = context.getPackageName();
        userId = GetAuth.user(context);
        initSlidingItems();
        listP = Variables.getArrayHash("offer_premium");
        if (listP == null) {
            callNet();
        } else {
            initList();
        }
        v.findViewById(R.id.frag_offers_notifView).setOnClickListener(v1 -> {
            Home.requestCode = 99;
            Home.activityForResult.launch(new Intent(context, PopupNotif.class));
        });
        return v;
    }

    @Override
    public void onResume() {
        if (shouldReload) {
            shouldReload = false;
            callNet();
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        isLive = false;
        super.onDestroy();
        timer.cancel();
    }

    private class sTimer extends TimerTask {
        @Override
        public void run() {
            activity.runOnUiThread(() -> {
                if (vPager.getCurrentItem() < adapter.getCount() - 1) {
                    vPager.setCurrentItem(vPager.getCurrentItem() + 1);
                } else {
                    vPager.setCurrentItem(0);
                }
            });
        }
    }

    private void initSlidingItems() {
        vPager = v.findViewById(R.id.frag_offers_viewPager);
        CircleIndicator indicator = v.findViewById(R.id.frag_offers_indicator);
        ArrayList<Object[]> list = new ArrayList<>();
        list.add(new Object[]{"image", R.drawable.ppv_icon, PPVOffers.class});
        list.add(new Object[]{"image", R.drawable.yt_icon, Yt.class});
        if (Home.tasks) {
            list.add(new Object[]{"image", R.drawable.banner_tasks_alt, TaskOffers.class});
        }
        adapter = new iAdapter(context, list);
        vPager.setAdapter(adapter);
        indicator.setViewPager(vPager);
        timer = new Timer();
        timer.scheduleAtFixedRate(new sTimer(), 3000, 5000);
    }

    private void callNet() {
        if (!Home.loadingDiag.isShowing()) Home.loadingDiag.show();
        Customoffers.getCustomOffers(context, new onResponse() {
            @Override
            public void onSuccessHashListHashMap(HashMap<String, ArrayList<HashMap<String, String>>> hashListHashmap) {
                ArrayList<HashMap<String, String>> list = new ArrayList<>();
                list.addAll(Objects.requireNonNull(hashListHashmap.get("i")));
                list.addAll(Objects.requireNonNull(hashListHashmap.get("s")));
                Variables.setArrayHash("offer_premium", list);
                if (!isLive) return;
                listP = list;
                Home.loadingDiag.dismiss();
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
                    Toast.makeText(context, DataParse.getStr(context, "could_not_connect", Home.spf), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void initList() {
        //int listPSize = listP.size();
        if (listP.size() > 0) {
            //int countP = listPSize < 2 ? 1 : listPSize < 5 ? 2 : 3;
            View layoutP = inflaters.inflate(R.layout.frag_offers_list, null);
            TextView titleP = layoutP.findViewById(R.id.frag_offers_list_titleView);
            titleP.setText(DataParse.getStr(context, "premium_offers", Home.spf));
            RecyclerView recyclerP = layoutP.findViewById(R.id.frag_offers_list_recyclerView);
            recyclerP.setLayoutManager(new GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false));
            recyclerP.setAdapter(new oAdapter("cus", listP, R.layout.frag_offers_item_alt));
            layout.addView(layoutP);
        }
        ArrayList<HashMap<String, String>> listW = GetNet.webInfos(context);
        int listWSize = listW.size();
        if (listWSize > 0) {
            //int countW = listWSize < 6 ? 1 : 2;
            View layoutW = inflaters.inflate(R.layout.frag_offers_list, null);
            TextView titleW = layoutW.findViewById(R.id.frag_offers_list_titleView);
            titleW.setText(DataParse.getStr(context, "web_offerwall", Home.spf));
            RecyclerView recyclerW = layoutW.findViewById(R.id.frag_offers_list_recyclerView);
            recyclerW.setLayoutManager(new GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false));
            recyclerW.setAdapter(new oAdapter("web", listW, R.layout.frag_offers_item));
            layout.addView(layoutW);
        }
        ArrayList<HashMap<String, String>> listS = GetNet.cpiInfos();
        int listSSize = listS.size();
        if (listSSize > 0) {
            //int countS = listSSize < 6 ? 1 : 2;
            View layoutS = inflaters.inflate(R.layout.frag_offers_list, null);
            TextView titleS = layoutS.findViewById(R.id.frag_offers_list_titleView);
            titleS.setText(DataParse.getStr(context, "sdk_offerwalls", Home.spf));
            RecyclerView recyclerS = layoutS.findViewById(R.id.frag_offers_list_recyclerView);
            recyclerS.setLayoutManager(new GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false));
            recyclerS.setAdapter(new oAdapter("sdk", listS, R.layout.frag_offers_item));
            layout.addView(layoutS);
        }
        ArrayList<HashMap<String, String>> listV = GetNet.cpvInfos();
        int listVSize = listV.size();
        if (listVSize > 0) {
            //int countV = listVSize < 6 ? 1 : 2;
            View layoutV = inflaters.inflate(R.layout.frag_offers_list, null);
            TextView titleV = layoutV.findViewById(R.id.frag_offers_list_titleView);
            titleV.setText(DataParse.getStr(context, "video_offers", Home.spf));
            RecyclerView recyclerV = layoutV.findViewById(R.id.frag_offers_list_recyclerView);
            recyclerV.setLayoutManager(new GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false));
            recyclerV.setAdapter(new oAdapter("sdk", listV, R.layout.frag_offers_item));
            layout.addView(layoutV);
        }
        ArrayList<HashMap<String, String>> listA = GetNet.apiInfos();
        int listASize = listA.size();
        if (listASize > 0) {
            //int countA = listASize < 6 ? 1 : 2;
            View layoutA = inflaters.inflate(R.layout.frag_offers_list, null);
            TextView titleA = layoutA.findViewById(R.id.frag_offers_list_titleView);
            titleA.setText(DataParse.getStr(context, "api_offerwalls", Home.spf));
            RecyclerView recyclerA = layoutA.findViewById(R.id.frag_offers_list_recyclerView);
            recyclerA.setLayoutManager(new GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false));
            recyclerA.setAdapter(new oAdapter("api", listA, R.layout.frag_offers_item));
            layout.addView(layoutA);
        }
    }

    private class oAdapter extends RecyclerView.Adapter<oAdapter.viewHolder> {
        private final String type;
        private final int layout;
        private final ArrayList<HashMap<String, String>> list;

        oAdapter(String type, ArrayList<HashMap<String, String>> list, int layout) {
            this.type = type;
            this.list = list;
            this.layout = layout;
        }

        @NonNull
        @Override
        public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new viewHolder(inflaters.inflate(layout, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull viewHolder holder, int position) {
            holder.titleView.setText(list.get(position).get("title"));
            holder.descView.setText(list.get(position).get("desc"));
            Picasso.get().load(list.get(position).get("image")).into(holder.imageView, new Callback() {
                @Override
                public void onSuccess() {
                    holder.imageView.setBackgroundResource(0);
                }

                @Override
                public void onError(Exception e) {

                }
            });
            if (type.equals("cus")) {
                holder.amtView.setText("+" + list.get(position).get("amount"));
                holder.amtView.setVisibility(View.VISIBLE);
            } else {
                holder.amtView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class viewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            final ShapeableImageView imageView;
            final TextView titleView, descView, amtView;

            viewHolder(View itemView) {
                super(itemView);
                titleView = itemView.findViewById(R.id.frag_offers_item_titleView);
                descView = itemView.findViewById(R.id.frag_offers_item_descView);
                imageView = itemView.findViewById(R.id.frag_offers_item_imageView);
                amtView = itemView.findViewById(R.id.frag_offers_item_amtView);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                HashMap<String, String> data = list.get(getAbsoluteAdapterPosition());
                switch (type) {
                    case "sdk":
                        try {
                            Class<?> c = Class.forName(packageName + ".sdkoffers." + data.get("name"));
                            startActivity(new Intent(context, c).putExtra("user", userId).putExtra("info", data));
                        } catch (ClassNotFoundException e) {
                            Toast.makeText(context, DataParse.getStr(context, "class_not_found", Home.spf), Toast.LENGTH_LONG).show();
                        }
                        break;
                    case "api": {
                        Intent intent = new Intent(context, APIOffers.class);
                        intent.putExtra("id", data.get("id"));
                        intent.putExtra("title", data.get("title"));
                        context.startActivity(intent);
                        break;
                    }
                    case "web": {
                        String url = data.get("url");
                        if (url == null) return;
                        Intent intent = new Intent(context, Surf.class);
                        intent.putExtra("fullscreen", true);
                        String[] urls = url.split("@@@");
                        if (urls.length > 1) {
                            intent.putExtra("url", urls[0]);
                            intent.putExtra("cred", urls[1]);
                        } else {
                            intent.putExtra("url", url.replace("@@@", ""));
                        }
                        context.startActivity(intent);
                        break;
                    }
                    case "cus": {
                        String url = data.get("url");
                        if (url == null) return;
                        if (url.startsWith("market://")) {
                            Toast.makeText(context, "This offer is not available anymore!", Toast.LENGTH_LONG).show();
                            remove(getAbsoluteAdapterPosition());
                        } else {
                            Misc.onenUrl(context, url);
                            shouldReload = true;
                        }
                    }
                }
            }
        }

        public void remove(int position) {
            list.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, list.size());
            Variables.setArrayHash("offer_premium", list);
        }
    }
}