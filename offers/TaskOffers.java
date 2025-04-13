package org.mintsoft.mintly.offers;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.Tasks;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.util.ArrayList;
import java.util.HashMap;

public class TaskOffers extends BaseAppCompat {
    private Dialog loadingDiag, conDiag;
    private ArrayList<HashMap<String, String>> list;
    private RecyclerView recyclerView;
    private tAdapter adapter;
    public static String reload = null;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (!Home.tasks) {
            finish();
            return;
        }
        setContentView(R.layout.offers_task);
        TextView titleView = findViewById(R.id.offers_task_title);
        titleView.setText(DataParse.getStr(this,"task_for_you",Home.spf));
        loadingDiag = Misc.loadingDiag(this);
        recyclerView = findViewById(R.id.offers_task_recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        list = Variables.getArrayHash("task_list");
        if (list == null) {
            netCall();
        } else {
            initList();
        }
        findViewById(R.id.offers_task_back).setOnClickListener(view -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reload != null) {
            int j = -1;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).get("id").equals(reload)) {
                    list.remove(i);
                    j = i;
                    break;
                }
            }
            reload = null;
            if (j != -1) {
                adapter.notifyItemRemoved(j);
                adapter.notifyItemRangeChanged(j, adapter.getItemCount());
            }
        }
    }

    @Override
    protected void onDestroy() {
        Variables.setArrayHash("task_list", list);
        super.onDestroy();
    }

    private void netCall() {
        if (!loadingDiag.isShowing()) loadingDiag.show();
        Tasks.getList(this, new onResponse() {
            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> ls) {
                list = ls;
                loadingDiag.dismiss();
                initList();
            }

            @Override
            public void onError(int errorCode, String error) {
                loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, TaskOffers.this, () -> {
                        netCall();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(TaskOffers.this, error, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    private void initList() {
        adapter = new tAdapter();
        recyclerView.setAdapter(adapter);
    }

    private class tAdapter extends RecyclerView.Adapter<tAdapter.viewHolder> {
        private final LayoutInflater inflater;

        tAdapter() {
            inflater = LayoutInflater.from(TaskOffers.this);
        }

        @NonNull
        @Override
        public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new viewHolder(inflater.inflate(R.layout.offers_task_grid, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull viewHolder holder, int i) {
            Picasso.get().load(list.get(i).get("image")).into(holder.imageView);
            holder.titleView.setText(list.get(i).get("title"));
            //holder.rewardView.setText("Get " + list.get(i).get("reward") + curr);
            holder.rewardView.setText(list.get(i).get("reward"));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class viewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            ImageView imageView;
            TextView titleView, rewardView;

            public viewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.offers_task_grid_imageView);
                titleView = itemView.findViewById(R.id.offers_task_grid_titleView);
                rewardView = itemView.findViewById(R.id.offers_task_grid_rewardView);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                int pos = getAbsoluteAdapterPosition();
                Intent intent = new Intent(TaskOffers.this, TaskDetails.class);
                intent.putExtra("id", list.get(pos).get("id"));
                startActivity(intent);
            }
        }
    }
}
