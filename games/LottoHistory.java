package org.mintsoft.mintly.games;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetGame;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LottoHistory extends BaseAppCompat {
    private Dialog dialog, progressDialog;
    private ListView listView;
    private TextView todayView;
    private final int[] stars = {R.drawable.star_0, R.drawable.star_1, R.drawable.star_2,
            R.drawable.star_3, R.drawable.star_4, R.drawable.star_5};

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.game_lotto_history);
        progressDialog = Misc.loadingDiag(this);
        TextView yesterday = findViewById(R.id.game_lotto_history_yesterday);
        yesterday.setText(DataParse.getStr(this,"yesterday",Home.spf));
        TextView todayTitle = findViewById(R.id.game_lotto_history_todaytitle);
        todayTitle.setText(DataParse.getStr(this,"today",Home.spf));
        listView = findViewById(R.id.game_lotto_history_listView);
        todayView = findViewById(R.id.game_lotto_history_today);
        callNet();
        findViewById(R.id.game_lotto_history_close).setOnClickListener(view -> finish());
    }

    private void callNet() {
        GetGame.getLottoHistory(this, new onResponse() {
            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> list) {
                progressDialog.dismiss();
                setResult(8, new Intent().putExtra("balance", list.get(0).get("balance")));
                String today = list.get(0).get("today").replace(",", "   ||   ");
                todayView.setText(today);

                ArrayList<Map<String, Object>> list2 = new ArrayList<>();
                Map<String, Object> data2;
                HashMap<String, String> data1;
                for (int i = 1; i < list.size(); i++) {
                    data1 = list.get(i);
                    data2 = new HashMap<>();
                    data2.put("n", stringSeperator(String.valueOf(data1.get("n"))));
                    data2.put("s", stars[Integer.parseInt(data1.get("c"))]);
                    data2.put("t", R.drawable.icon_coin);
                    data2.put("a", String.valueOf(data1.get("r")));
                    list2.add(data2);
                }
                SimpleAdapter adapter = new SimpleAdapter(LottoHistory.this, list2, R.layout.game_lotto_history_item,
                        new String[]{"n", "s", "t", "a"}, new int[]{R.id.game_lotto_hist_item_number, R.id.game_lotto_hist_item_star,
                        R.id.game_lotto_hist_item_coin_points, R.id.game_lotto_hist_item_amount});
                listView.setAdapter(adapter);
                String date = new SimpleDateFormat("dd", Locale.getDefault())
                        .format(new Date(Home.spf.getLong("stime", 0L)));
                Home.spf.edit().putString("l_time", date).apply();
            }

            @Override
            public void onError(int errorCode, String error) {
                progressDialog.dismiss();
                if (errorCode == -9) {
                    dialog = Misc.noConnection(dialog, LottoHistory.this, () -> {
                        callNet();
                        dialog.dismiss();
                    });
                } else {
                    Toast.makeText(LottoHistory.this, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private String stringSeperator(String text) {
        try {
            long numbers = Long.parseLong(text);
            DecimalFormat df = new DecimalFormat("##,##,##,##,##");
            return df.format(numbers).replaceAll(",", " ");
        } catch (Exception e) {
            return text;
        }
    }

    @Override
    protected void onDestroy() {
        if (progressDialog.isShowing()) progressDialog.dismiss();
        super.onDestroy();
    }
}