package org.mintsoft.mintly.helper;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;

import java.util.HashMap;
import java.util.Set;

public class PushMsg extends BaseAppCompat {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Set<String> data = Home.spf.getStringSet("push_msg", null);
        if (data == null) {
            finish();
        } else {
            setContentView(R.layout.push_msg);
            Object[] objects = data.toArray();
            HashMap<String, String> hashMap = new HashMap<>();
            for (Object o : objects) {
                if (o.toString().contains("title###")) {
                    hashMap.put("title", o.toString().replace("title###", ""));
                } else if (o.toString().contains("desc###")) {
                    hashMap.put("desc", o.toString().replace("desc###", ""));
                } else if (o.toString().contains("image###")) {
                    hashMap.put("image", o.toString().replace("image###", ""));
                } else if (o.toString().contains("small###")) {
                    hashMap.put("small", "1");
                }
            }
            if (hashMap.containsKey("title")) {
                TextView titleView = findViewById(R.id.push_msg_titleView);
                titleView.setText(hashMap.get("title"));
            }
            if (hashMap.containsKey("desc")) {
                TextView descView = findViewById(R.id.push_msg_descView);
                descView.setText(hashMap.get("desc"));
            }
            if (hashMap.containsKey("image")) {
                ImageView imageView = findViewById(R.id.push_msg_imageView);
                if (hashMap.containsKey("small")) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            Misc.dpToPx(this, 100),
                            Misc.dpToPx(this, 100)
                    );
                    params.topMargin = Misc.dpToPx(this, 10);
                    imageView.setLayoutParams(params);
                }
                Picasso.get().load(hashMap.get("image")).placeholder(R.drawable.anim_loading)
                        .error(R.color.gray).into(imageView);
            }
            findViewById(R.id.push_msg_close).setOnClickListener(view -> finish());
        }
        Home.spf.edit().remove("push_msg").apply();
    }
}
