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
import androidx.core.content.ContextCompat;
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
import java.util.Objects;

public class FragHwdr extends Fragment {
    private Context context;
    private boolean isLive;
    private ImageView emptyView;
    private LayoutInflater inflaters;
    private RecyclerView recyclerView;
    private HashMap<String, String> histData;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        View v = inflater.inflate(R.layout.frag_h_hist, container, false);
        if (context == null || getActivity() == null) return v;
        isLive = true;
        inflaters = LayoutInflater.from(context);
        recyclerView = v.findViewById(R.id.frag_h_hist_recyclerView);
        emptyView = v.findViewById(R.id.frag_h_hist_emptyView);
        ArrayList<HashMap<String, String>> list = Variables.getArrayHash("gift_list");
        if (list == null || History.loadAll) {
            callNet();
        } else {
            initList(list);
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
        GetURL.getGifts(context, new onResponse() {
            @Override
            public void onSuccess(String response) {
                Home.spf.edit().putLong("r_time", System.currentTimeMillis() + Home.delay).commit();
                Home.balance = response;
            }

            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> l) {
                Variables.setArrayHash("gift_list", l);
                if (!isLive) return;
                History.histLoading.dismiss();
                initList(l);
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

    private void initList(ArrayList<HashMap<String, String>> list) {
        histData = list.get(list.size() - 1);
        if (histData.size() == 0) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(new hwAdapter());
        }
    }

    private class hwAdapter extends RecyclerView.Adapter<hwAdapter.viewHolder> {
        private final int red, green, white;
        private final String pending, completed, rejected, sTitle;

        hwAdapter() {
            red = ContextCompat.getColor(context, R.color.red_1);
            green = ContextCompat.getColor(context, R.color.green_1);
            white = ContextCompat.getColor(context, R.color.white_aa);
            pending = DataParse.getStr(context, "pending", Home.spf);
            completed = DataParse.getStr(context, "completed", Home.spf);
            rejected = DataParse.getStr(context, "rejected", Home.spf);
            sTitle = DataParse.getStr(context, "status", Home.spf);
        }

        @NonNull
        @Override
        public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new viewHolder(inflaters.inflate(R.layout.frag_h_hist_item_alt, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull viewHolder holder, int position) {
            String[] data = histData.get(String.valueOf(position)).split(";@");
            String[] title = data[0].split("\\[");
            Picasso.get().load(data[3]).into(holder.imageView);
            holder.fromView.setText(title[0]);
            if (title.length > 1) {
                holder.amtView.setText(title[1].replace("]", ""));
            }
            holder.statusPrefix.setText(sTitle + " ");
            String status = data[1];
            switch (Objects.requireNonNull(status)) {
                case "0":
                    holder.statusView.setText(pending);
                    holder.statusView.setTextColor(white);
                    break;
                case "1":
                    holder.statusView.setText(completed);
                    holder.statusView.setTextColor(green);
                    break;
                case "2":
                    holder.statusView.setText(rejected);
                    holder.statusView.setTextColor(red);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return histData.size();
        }

        class viewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView fromView, statusView, amtView, statusPrefix;
            ImageView imageView;

            public viewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.frag_h_hist_item_imageView);
                fromView = itemView.findViewById(R.id.frag_h_hist_item_fromView);
                statusView = itemView.findViewById(R.id.frag_h_hist_item_statusView);
                amtView = itemView.findViewById(R.id.frag_h_hist_item_amtView);
                statusPrefix = itemView.findViewById(R.id.frag_h_hist_item_statusPrefix);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                String[] data = histData.get(String.valueOf(getAbsoluteAdapterPosition())).split(";@");
                if (!data[2].isEmpty()) {
                    Misc.showMessage(context, data[2], false);
                }
            }
        }
    }

}
