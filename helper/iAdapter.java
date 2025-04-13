package org.mintsoft.mintly.helper;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.squareup.picasso.Picasso;

import org.mintsoft.mintly.R;

import java.util.ArrayList;
import java.util.Objects;

public class iAdapter extends PagerAdapter {
    private final Context context;
    private final ArrayList<Object[]> list;

    public iAdapter(Context context, ArrayList<Object[]> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = inflater.inflate(R.layout.frag_offers_top_item, container, false);
        ImageView ivHeader = itemView.findViewById(R.id.frag_offers_top_item_imageView);
        if (Objects.equals(list.get(position)[0], "url")) {
            Picasso.get().load((String) list.get(position)[1]).into(ivHeader);
        } else {
            ivHeader.setImageResource((int) list.get(position)[1]);
        }
        ivHeader.setOnClickListener(v -> context.startActivity(new Intent(context, (Class<?>) list.get(position)[2])));
        container.addView(itemView);
        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
        container.removeView((RelativeLayout) object);
    }
}