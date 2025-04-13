package org.mintsoft.mintly.helper;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;

public class PopupPreQuiz extends BaseAppCompat {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Bundle extras = getIntent().getExtras();
        setResult(9);
        if (extras == null) {
            finish();
        } else {
            setContentView(R.layout.popup_pre_quiz);
            try {
                String imageUrl = extras.getString("image");
                if (imageUrl != null) {
                    ImageView imageView = findViewById(R.id.popup_pre_quiz_img);
                    Picasso.get().load(imageUrl).into(imageView);
                }
                TextView titleView2 = findViewById(R.id.popup_pre_quiz_title2);
                titleView2.setText(DataParse.getStr(this, "app_name", Home.spf));
                Misc.setLogo(this, titleView2);
                TextView ppq_ps = findViewById(R.id.ppq_per_set);
                ppq_ps.setText(DataParse.getStr(this, "per_set", Home.spf));
                TextView ppq_rc = findViewById(R.id.ppq_round_cost);
                ppq_rc.setText(DataParse.getStr(this, "round_cost", Home.spf));
                TextView ppq_tl = findViewById(R.id.ppq_today_left);
                ppq_tl.setText(DataParse.getStr(this, "today_left", Home.spf));
                TextView ppq_wc = findViewById(R.id.ppq_want_cont);
                ppq_wc.setText(DataParse.getStr(this, "want_continue", Home.spf));
                TextView titleView = findViewById(R.id.popup_pre_quiz_title);
                TextView descView = findViewById(R.id.popup_pre_quiz_desc);
                TextView qsView = findViewById(R.id.popup_pre_quiz_qs);
                TextView costView = findViewById(R.id.popup_pre_quiz_cost);
                TextView roundView = findViewById(R.id.popup_pre_quiz_round);
                titleView.setText(extras.getString("title"));
                descView.setText(extras.getString("desc"));
                qsView.setText((extras.getString("qs") + " " + DataParse.getStr(this, "questions", Home.spf)));
                String cost = extras.getString("cost");
                costView.setText((cost + " " + Home.currency.toLowerCase() + "s"));
                int remain = extras.getInt("remain");
                roundView.setText((remain + " " + DataParse.getStr(this, "rounds", Home.spf)));
                Button yesBtn = findViewById(R.id.popup_pre_quiz_yes);
                yesBtn.setText(DataParse.getStr(this, "yes", Home.spf));
                if (remain < 1) {
                    yesBtn.setAlpha(0.4f);
                    yesBtn.setText(DataParse.getStr(this, "exceed_daily_limit", Home.spf));
                } else {
                    if (cost.equals("0")) {
                        yesBtn.setText(DataParse.getStr(this, "free_play", Home.spf));
                    }
                    yesBtn.setOnClickListener(view1 -> {
                        setResult(8);
                        finish();
                    });
                }
                Button noBtn = findViewById(R.id.popup_pre_quiz_no);
                yesBtn.setText(DataParse.getStr(this, "no", Home.spf));
                noBtn.setOnClickListener(view -> finish());
                findViewById(R.id.popup_pre_quiz_close).setOnClickListener(view -> finish());
            } catch (Exception e) {
                finish();
            }
        }
    }
}