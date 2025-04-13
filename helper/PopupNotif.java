package org.mintsoft.mintly.helper;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetNet;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PopupNotif extends BaseAppCompat {
    private notif_adapter adapter;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Bundle extras = getIntent().getExtras();
        setContentView(R.layout.popup_list);
        RelativeLayout holderView = findViewById(R.id.popup_list_holder);
        List<HashMap<String, String>> list = new ArrayList<>();
        if (extras == null) {
            list = GetNet.getMessages(this);
        } else {
            String title = extras.getString("title");
            String msg = extras.getString("msg");
            if (title == null || msg == null) {
                finish();
            } else {
                String t = extras.getString("title");
                String m = extras.getString("msg");
                String d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        .format(System.currentTimeMillis());
                HashMap<String, String> data = new HashMap<>();
                data.put("title", t);
                data.put("msg", m);
                data.put("date", d);
                list.add(data);
                GetNet.addMessage(this, d, t, m);
            }
        }
        if (list.size() == 0) {
            Toast.makeText(this, DataParse.getStr(this,"no_message", Home.spf), Toast.LENGTH_LONG).show();
            finish();
        } else {
            RecyclerView recyclerView = findViewById(R.id.popup_list_recyclerView);
            LinearLayoutManager lm = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(lm);
            adapter = new notif_adapter(this, list);
            recyclerView.setAdapter(adapter);
            Misc.listAnimate(lm, recyclerView);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    adapter.remove(viewHolder.getAbsoluteAdapterPosition());
                }
            });
            itemTouchHelper.attachToRecyclerView(recyclerView);
            holderView.setOnClickListener(view -> onBackPressed());
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private static class notif_adapter extends RecyclerView.Adapter<notif_adapter.ViewHolder> {
        private final List<HashMap<String, String>> mList;
        private final LayoutInflater mInflater;
        private final Activity mActivity;

        notif_adapter(Activity activity, List<HashMap<String, String>> list) {
            this.mInflater = LayoutInflater.from(activity);
            this.mList = list;
            this.mActivity = activity;
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.popup_notif, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String title = mList.get(position).get("title");
            String msg = mList.get(position).get("msg");
            String date = mList.get(position).get("date");
            holder.titleView.setText(title);
            holder.msgView.setText(msg);
            holder.dateView.setText(date);
        }

        @Override
        public int getItemCount() {
            return Math.min(mList.size(), 3);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView titleView, msgView, dateView;

            ViewHolder(View itemView) {
                super(itemView);
                titleView = itemView.findViewById(R.id.popup_notif_title);
                msgView = itemView.findViewById(R.id.popup_notif_desc);
                dateView = itemView.findViewById(R.id.popup_notif_date);
            }
        }

        public void remove(int position) {
            mList.remove(position);
            GetNet.delMessages(mActivity, position);
            int size = mList.size();
            if (size == 0) {
                mActivity.setResult(0);
                mActivity.onBackPressed();
            } else {
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, size);
                mActivity.setResult(size);
            }
        }
    }
}