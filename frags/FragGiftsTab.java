package org.mintsoft.mintly.frags;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;

import java.util.HashMap;

public class FragGiftsTab extends Fragment {
    private View v;
    private Context context;
    private LayoutInflater inflaters;
    private FragGifts fragment;
    private HashMap<String, String> hashData;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        if (context == null || getActivity() == null) return null;
        inflaters = LayoutInflater.from(context);
        fragment = (FragGifts) getActivity().getSupportFragmentManager().findFragmentByTag("2");
        RecyclerView recyclerView = new RecyclerView(context);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
        recyclerView.setLayoutParams(params);
        recyclerView.setVerticalScrollBarEnabled(false);
        recyclerView.setHorizontalFadingEdgeEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new giftAdapter());
        return recyclerView;
    }

    public static Fragment newInstance(HashMap<String, String> data) {
        FragGiftsTab fragGiftsTab = new FragGiftsTab();
        fragGiftsTab.hashData = data;
        return fragGiftsTab;
    }

    private class giftAdapter extends RecyclerView.Adapter<giftAdapter.viewHolder> {
        private final int bals;
        private final String lockedStr, unlockedStr;

        giftAdapter() {
            bals = Integer.parseInt(Home.balance);
            lockedStr = DataParse.getStr(context, "gift_locked", Home.spf);
            unlockedStr = DataParse.getStr(context, "gift_unlocked", Home.spf);
        }

        @NonNull
        @Override
        public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new viewHolder(inflaters.inflate(R.layout.frag_gifts_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull viewHolder holder, int position) {
            String[] strings = hashData.get(String.valueOf(position)).split(",@");
            holder.nameView.setText(strings[2]);
            holder.availView.setText(strings[1] + " available");
            holder.coinView.setText(strings[3]);
            Picasso.get().load(hashData.get("image")).into(holder.imageView);
            int reqC = Integer.parseInt(strings[3]);
            if (bals < reqC) {
                holder.progressBar.setMax(reqC);
                holder.progressBar.setProgress(bals);
                holder.lockView.setImageResource(R.drawable.ic_locked);
                holder.statusView.setText(lockedStr);
            } else {
                holder.progressBar.setMax(100);
                holder.progressBar.setProgress(100);
                holder.lockView.setImageResource(R.drawable.ic_unlocked);
                holder.statusView.setText(unlockedStr);
            }
        }

        @Override
        public int getItemCount() {
            int size = hashData.size();
            return size > 4 ? size - 4 : 0;
        }

        class viewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            ImageView lockView;
            ProgressBar progressBar;
            ShapeableImageView imageView;
            TextView nameView, coinView, availView, statusView;

            public viewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.frag_gifts_item_imageView);
                nameView = itemView.findViewById(R.id.frag_gifts_item_nameView);
                coinView = itemView.findViewById(R.id.frag_gifts_item_coinView);
                availView = itemView.findViewById(R.id.frag_gifts_item_availView);
                statusView = itemView.findViewById(R.id.frag_gifts_item_statText);
                progressBar = itemView.findViewById(R.id.frag_gifts_item_statProgress);
                lockView = itemView.findViewById(R.id.frag_gifts_item_statLock);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                String[] strings = hashData.get(String.valueOf(getAbsoluteAdapterPosition())).split(",@");
                int rb = Integer.parseInt(strings[3]);
                if (rb == 0 || rb > Integer.parseInt(Home.balance)) {
                    Toast.makeText(context, DataParse.getStr(context, "insufficient_balance", Home.spf), Toast.LENGTH_LONG).show();
                } else {
                    fragment.confirmDiag(hashData.get("image"), hashData.get("name"), "(" + strings[2] + ")",
                            hashData.get("desc"), hashData.get("type"), strings[0]);
                }
            }
        }
    }

}
