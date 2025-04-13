package org.mintsoft.mintly.offers;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.Offerwalls;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Surf;

import java.util.ArrayList;
import java.util.HashMap;

public class APIOffers extends BaseAppCompat {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.offers_api);
        Bundle extras = getIntent().getExtras();
        String id = extras.getString("id");
        if (id == null) {
            finish();
            return;
        }
        TextView titleView = findViewById(R.id.offers_api_title);
        String title = extras.getString("title");
        if (title == null) {
            titleView.setText(DataParse.getStr(this, "offers", Home.spf));
        } else {
            titleView.setText(title);
        }
        recyclerView = findViewById(R.id.offers_api_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        progressBar = findViewById(R.id.offers_api_progressBar);
        callnet(id);
        findViewById(R.id.offers_api_close).setOnClickListener(view -> finish());
    }

    private void callnet(String id) {
        Offerwalls.api(this, id, new onResponse() {
            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> list) {
                rAdapter adapter = new rAdapter(APIOffers.this, list, R.layout.offers_api_list);
                recyclerView.setAdapter(adapter);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(int errorCode, String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(APIOffers.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static class rAdapter extends RecyclerView.Adapter<rAdapter.ViewHolder> {
        private final ArrayList<HashMap<String, String>> mList;
        private final LayoutInflater mInflater;
        private final int mLayout;
        private final Context mContext;

        public rAdapter(Context context, ArrayList<HashMap<String, String>> list, int layout) {
            this.mInflater = LayoutInflater.from(context);
            this.mList = list;
            this.mLayout = layout;
            this.mContext = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(mLayout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.titleView.setText(mList.get(position).get("title"));
            holder.descView.setText(mList.get(position).get("desc"));
            holder.amountView.setText(mList.get(position).get("amount"));
            Picasso.get().load(mList.get(position).get("image"))
                    .placeholder(R.drawable.anim_loading)
                    .error(R.color.gray)
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            final TextView titleView, descView, amountView;
            final ImageView imageView;

            ViewHolder(View itemView) {
                super(itemView);
                titleView = itemView.findViewById(R.id.offers_api_list_titleView);
                descView = itemView.findViewById(R.id.offers_api_list_descView);
                amountView = itemView.findViewById(R.id.offers_api_list_amountView);
                imageView = itemView.findViewById(R.id.offers_api_list_imageView);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                String url = mList.get(getAbsoluteAdapterPosition()).get("url");
                if (url == null) return;
                Intent intent = new Intent(mContext, Surf.class);
                if (url.contains("?alt=1")) {
                    intent.putExtra("cred", "alt");
                    url = url.replace("?alt=1", "");
                } else if (url.contains("&alt=1")) {
                    intent.putExtra("cred", "alt");
                    url = url.replace("&alt=1", "");
                }
                intent.putExtra("url", url);
                mContext.startActivity(intent);
            }
        }
    }
}