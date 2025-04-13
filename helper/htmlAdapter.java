package org.mintsoft.mintly.helper;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;

import java.util.ArrayList;
import java.util.HashMap;

public class htmlAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_ITEM = 0;
    private final ArrayList<HashMap<String, String>> list;
    private final ArrayList<HashMap<String, String>> mainList;
    private final LayoutInflater inflater;
    private final int layout;
    private final Context context;
    private boolean isLoading;
    private final Handler handler;

    public htmlAdapter(Context context, ArrayList<HashMap<String, String>> lst, int layout, int count) {
        inflater = LayoutInflater.from(context);
        this.context = context;
        this.layout = layout;
        this.mainList = lst;
        this.list = new ArrayList<>();
        for (int i = 0; i < Math.min(lst.size(), count); i++) {
            list.add(lst.get(i));
        }
        this.handler = new Handler();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = inflater.inflate(layout, parent, false);
            return new ItemViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.loading_item, parent, false);
            return new LoadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof ItemViewHolder) {
            populateItemRows((ItemViewHolder) viewHolder, position);
        } else if (viewHolder instanceof LoadingViewHolder) {
            showLoadingView((LoadingViewHolder) viewHolder, position);
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position) == null ? 1 : VIEW_TYPE_ITEM;
    }

    private class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView titleView, activationView;
        ImageView imageView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.frag_main_item_titleView);
            imageView = itemView.findViewById(R.id.frag_main_item_imageView);
            activationView = itemView.findViewById(R.id.frag_main_item_activationView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int pos = getAbsoluteAdapterPosition();
            new Misc().gameInfo(context, list.get(pos), new Misc.htmlYN() {
                @Override
                public void yes(Dialog d) {
                    d.dismiss();
                    list.get(pos).put("file", "none");
                    TextView tv = v.findViewById(R.id.frag_main_item_activationView);
                    tv.setVisibility(View.GONE);
                }

                @Override
                public void no(Dialog d) {
                    d.dismiss();
                }
            });
        }
    }

    private static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LinearProgressIndicator progressBar;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }

    private void showLoadingView(LoadingViewHolder viewHolder, int position) {
    }

    private void populateItemRows(ItemViewHolder holder, int pos) {
        holder.titleView.setText(list.get(pos).get("name"));
        Picasso.get().load(list.get(pos).get("image")).placeholder(R.drawable.anim_loading).into(holder.imageView);
        holder.activationView.setText(Misc.html(DataParse.getStr(context, "activation_required", Home.spf)));
        String file = list.get(pos).get("file");
        if (file != null) {
            if (file.isEmpty()) {
                holder.activationView.setVisibility(View.VISIBLE);
            } else {
                holder.activationView.setVisibility(View.GONE);
            }
        }
    }

    public void recyclerState(RecyclerView recyclerView) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!isLoading && list.size() < mainList.size()) {
                    if (lm != null && lm.findLastCompletelyVisibleItemPosition() == list.size() - 1) {
                        list.add(null);
                        notifyItemInserted(list.size() - 1);
                        handler.postDelayed(() -> {
                            list.remove(list.size() - 1);
                            int scrollPosition = list.size();
                            notifyItemRemoved(scrollPosition);
                            for (int i = scrollPosition; i < Math.min(mainList.size(), scrollPosition + 4); i++) {
                                list.add(mainList.get(i));
                            }
                            notifyItemInserted(list.size() - 1);
                            isLoading = false;
                        }, 1500);
                        isLoading = true;
                    }
                }
            }
        });
    }

    public void nestedState(NestedScrollView scrollView, View moreView) {
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (scrollY == (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())) {
                        if (!isLoading) {
                            if (list.size() < mainList.size()) {
                                list.add(null);
                                notifyItemInserted(list.size() - 1);
                                handler.postDelayed(() -> {
                                    list.remove(list.size() - 1);
                                    int scrollPosition = list.size();
                                    notifyItemRemoved(scrollPosition);
                                    for (int i = scrollPosition; i < Math.min(mainList.size(), scrollPosition + 4); i++) {
                                        list.add(mainList.get(i));
                                    }
                                    notifyItemInserted(list.size() - 1);
                                    isLoading = false;
                                }, 1500);
                            } else {
                                moreView.setVisibility(View.VISIBLE);
                            }
                            isLoading = true;
                        }
                    }
                });
    }
}