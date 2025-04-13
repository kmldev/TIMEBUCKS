package org.mintsoft.mintly.frags;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetURL;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;
import org.mintsoft.mintly.helper.fullGridView;

import java.util.ArrayList;
import java.util.HashMap;

public class FragRanks extends Fragment {
    private Context context;
    private View v;
    private Dialog conDiag;
    private boolean isLive;
    private String scoreStr;
    private fullGridView listView;
    private ArrayList<HashMap<String, String>> list, list2;
    private ArrayList<ImageView> avatarViews;
    private ArrayList<ConstraintLayout> avHolders;
    private ArrayList<TextView> nameViews, amtViews, scoreViews;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        v = inflater.inflate(R.layout.frag_ranks, container, false);
        if (context == null || getActivity() == null) return v;
        isLive = true;
        TextView dummy_2 = v.findViewById(R.id.frag_ranks_dummy_1);
        dummy_2.setText(DataParse.getStr(context, "leaderboard_desc", Home.spf));
        TextView empt = v.findViewById(R.id.frag_ranks_emptyView);
        empt.setText(DataParse.getStr(context, "list_is_empty", Home.spf));
        avatarViews = new ArrayList<>();
        avatarViews.add(v.findViewById(R.id.frag_ranks_avatar_r_1));
        avatarViews.add(v.findViewById(R.id.frag_ranks_avatar_r_2));
        avatarViews.add(v.findViewById(R.id.frag_ranks_avatar_r_3));
        avHolders = new ArrayList<>();
        avHolders.add(v.findViewById(R.id.frag_ranks_holder_r_1));
        avHolders.add(v.findViewById(R.id.frag_ranks_holder_r_2));
        avHolders.add(v.findViewById(R.id.frag_ranks_holder_r_3));
        nameViews = new ArrayList<>();
        nameViews.add(v.findViewById(R.id.frag_ranks_name_r_1));
        nameViews.add(v.findViewById(R.id.frag_ranks_name_r_2));
        nameViews.add(v.findViewById(R.id.frag_ranks_name_r_3));
        amtViews = new ArrayList<>();
        amtViews.add(v.findViewById(R.id.frag_ranks_reward_r_1));
        amtViews.add(v.findViewById(R.id.frag_ranks_reward_r_2));
        amtViews.add(v.findViewById(R.id.frag_ranks_reward_r_3));
        scoreViews = new ArrayList<>();
        scoreViews.add(v.findViewById(R.id.frag_ranks_score_r_1));
        scoreViews.add(v.findViewById(R.id.frag_ranks_score_r_2));
        scoreViews.add(v.findViewById(R.id.frag_ranks_score_r_3));
        listView = v.findViewById(R.id.frag_ranks_listView);
        String earnStr = DataParse.getStr(context, "earn", Home.spf);
        TextView amtTitle1 = v.findViewById(R.id.frag_ranks_rtitle_r_1);
        amtTitle1.setText(earnStr);
        TextView amtTitle2 = v.findViewById(R.id.frag_ranks_rtitle_r_2);
        amtTitle2.setText(earnStr);
        TextView amtTitle3 = v.findViewById(R.id.frag_ranks_rtitle_r_3);
        amtTitle3.setText(earnStr);
        list = Variables.getArrayHash("leaderboard_list");
        if (list == null) {
            netCall();
        } else {
            initList();
        }
        return v;
    }

    @Override
    public void onDestroy() {
        isLive = false;
        super.onDestroy();
    }

    private void netCall() {
        if (!Home.loadingDiag.isShowing()) Home.loadingDiag.show();
        GetURL.leaderboard(context, new onResponse() {
            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> l) {
                Variables.setArrayHash("leaderboard_list", l);
                if (!isLive) return;
                list = l;
                Home.loadingDiag.dismiss();
                initList();
            }

            @Override
            public void onError(int errorCode, String error) {
                if (!isLive) return;
                Home.loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, context, () -> {
                        netCall();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void initList() {
        scoreStr = DataParse.getStr(context, "score", Home.spf);
        int size = list.size();
        if (size == 0) {
            v.findViewById(R.id.frag_ranks_dummy_2).setVisibility(View.GONE);
            v.findViewById(R.id.frag_ranks_emptyView).setVisibility(View.VISIBLE);
            return;
        }
        for (int i = 0; i < 3; i++) {
            if (size > i) {
                nameViews.get(i).setText(list.get(i).get("n"));
                scoreViews.get(i).setText(scoreStr + ": " + list.get(i).get("s"));
                amtViews.get(i).setText(list.get(i).get("r"));
                Picasso.get().load(list.get(i).get("a")).placeholder(R.drawable.avatar).into(avatarViews.get(i));
                if (list.get(i).get("y").equals("y")) {
                    nameViews.get(i).setTextColor(ContextCompat.getColor(context, R.color.red_1));
                }
            } else {
                avHolders.get(i).setVisibility(View.INVISIBLE);
            }
        }
        if (size > 3) {
            list2 = new ArrayList<>();
            for (int i = 3; i < list.size(); i++) {
                list2.add(list.get(i));
            }
            listView.setAdapter(new rAdapter(context));
        } else {
            listView.setVisibility(View.GONE);
        }
    }

    private class rAdapter extends BaseAdapter {
        private final LayoutInflater inflater;

        rAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View v = view;
            if (v == null) {
                v = inflater.inflate(R.layout.frag_ranks_item, viewGroup, false);
            }
            TextView rankView = v.findViewById(R.id.frag_ranks_item_rank);
            TextView nameView = v.findViewById(R.id.frag_ranks_item_name);
            TextView scoreView = v.findViewById(R.id.frag_ranks_item_score);
            TextView rewardView = v.findViewById(R.id.frag_ranks_item_reward);
            rankView.setText(String.valueOf(i + 4));
            nameView.setText(list2.get(i).get("n"));
            scoreView.setText(scoreStr + ": " + list2.get(i).get("s"));
            rewardView.setText(list2.get(i).get("r"));
            ImageView imageView = v.findViewById(R.id.frag_ranks_item_avatar);
            Picasso.get().load(list2.get(i).get("a")).placeholder(R.drawable.avatar).into(imageView);
            if (list2.get(i).get("y").equals("y")) {
                v.setBackgroundResource(R.drawable.rc_white_border_trans);
            } else {
                v.setBackgroundResource(R.drawable.rc_colorprimary);
            }
            return v;
        }

        @Override
        public int getCount() {
            return list2.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }
    }
}
