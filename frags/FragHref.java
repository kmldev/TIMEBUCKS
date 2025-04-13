package org.mintsoft.mintly.frags;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetURL;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.History;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.util.ArrayList;
import java.util.HashMap;

public class FragHref extends Fragment {
    private Context context;
    private boolean isLive;
    private ImageView emptyView;
    private LayoutInflater inflaters;
    private RecyclerView recyclerView;
    private ArrayList<HashMap<String, String>> list;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        View v = inflater.inflate(R.layout.frag_h_hist, container, false);
        if (context == null || getActivity() == null) return v;
        isLive = true;
        inflaters = LayoutInflater.from(context);
        recyclerView = v.findViewById(R.id.frag_h_hist_recyclerView);
        emptyView = v.findViewById(R.id.frag_h_hist_emptyView);
        list = Variables.getArrayHash("hist_ref");
        if (list == null || History.loadAll) {
            callNet();
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

    private void callNet() {
        if (!History.histLoading.isShowing()) History.histLoading.show();
        GetURL.getRefHistory(context, new onResponse() {

            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> l) {
                Variables.setArrayHash("hist_ref", l);
                if (!isLive) return;
                list = l;
                History.histLoading.dismiss();
                initList();
            }

            @Override
            public void onError(int errorCode, String error) {
                if (!isLive) return;
                History.histLoading.dismiss();
                if (errorCode == -9) {
                    History.hConDiag = Misc.noConnection(History.hConDiag, context, () -> {
                        callNet();
                        History.hConDiag.dismiss();
                    });
                } else {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void initList() {
        if (list.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(new hrAdapter());
        }
    }

    private class hrAdapter extends RecyclerView.Adapter<hrAdapter.viewHolder> {
        private final String sTitle;

        hrAdapter() {
            sTitle = DataParse.getStr(context, "referrer_since", Home.spf);
        }

        @NonNull
        @Override
        public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new viewHolder(inflaters.inflate(R.layout.frag_h_hist_item_alt, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull viewHolder holder, int position) {
            holder.amtView.setVisibility(View.GONE);
            Picasso.get().load(list.get(position).get("image")).placeholder(R.drawable.avatar).into(holder.imageView);
            holder.fromView.setText(list.get(position).get("name"));
            holder.statusView.setText(list.get(position).get("date"));
            holder.statusPrefix.setText(sTitle + " ");
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class viewHolder extends RecyclerView.ViewHolder {
            TextView fromView, statusView, amtView, statusPrefix;
            ImageView imageView;

            public viewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.frag_h_hist_item_imageView);
                fromView = itemView.findViewById(R.id.frag_h_hist_item_fromView);
                statusView = itemView.findViewById(R.id.frag_h_hist_item_statusView);
                amtView = itemView.findViewById(R.id.frag_h_hist_item_amtView);
                statusPrefix = itemView.findViewById(R.id.frag_h_hist_item_statusPrefix);
            }
        }
    }
}
