package org.mintsoft.mintly;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetURL;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.ArrayList;
import java.util.HashMap;

public class Support extends BaseAppCompat {
    private EditText editText;
    private ImageView imageView;
    private TextView dateView, msgView;
    private boolean isSending, stopReload;
    private Dialog conDiag;
    private Handler handler;
    private Runnable runnable, reload;
    private LinearLayout listView;
    private LayoutInflater inflater;
    private ScrollView scrollView;
    private ArrayList<HashMap<String, String>> list;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.support);
        TextView titleView = findViewById(R.id.support_titleView);
        titleView.setText(DataParse.getStr(this, "support", Home.spf));
        editText = findViewById(R.id.support_inputView);
        editText.setHint(DataParse.getStr(this, "write_here", Home.spf));
        listView = findViewById(R.id.support_listView);
        scrollView = findViewById(R.id.support_scroll);
        inflater = LayoutInflater.from(this);
        callNet();
        findViewById(R.id.support_send_btn).setOnClickListener(view -> {
            String msg = editText.getText().toString();
            if (msg.isEmpty()) return;
            if (msg.length() < 20) {
                Toast.makeText(Support.this, DataParse.getStr(Support.this, "msg_short", Home.spf), Toast.LENGTH_LONG).show();
                return;
            }
            sendMsg(msg, true);
        });
        findViewById(R.id.support_close).setOnClickListener(view -> finish());
        handler = new Handler();
        runnable = () -> scrollView.fullScroll(View.FOCUS_DOWN);
        reload = () -> {
            if (!stopReload) {
                stopReload = true;
                callNet();
            }
            handler.postDelayed(reload, 20000);
        };
        handler.postDelayed(reload, 20000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(reload);
        handler.removeCallbacks(runnable);
        super.onDestroy();
    }

    private void callNet() {
        GetURL.getSupport(this, new onResponse() {
            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> l) {
                list = l;
                initList();
                stopReload = false;
            }

            @Override
            public void onError(int errorCode, String error) {
                stopReload = true;
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, Support.this, () -> {
                        conDiag.dismiss();
                        callNet();
                    });
                } else {
                    Toast.makeText(Support.this, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void sendMsg(String msg, boolean newMsg) {
        if (isSending) return;
        isSending = true;
        editText.setText("");
        if (newMsg) {
            View v = inflater.inflate(R.layout.support_y, null);
            msgView = v.findViewById(R.id.support_msgView);
            msgView.setText(Misc.html(msg));
            dateView = v.findViewById(R.id.support_dateView);
            imageView = v.findViewById(R.id.support_statusView);
            imageView.setImageResource(R.drawable.loading);
            dateView.setText(DataParse.getStr(Support.this, "please_wait", Home.spf).toLowerCase());
            listView.addView(v);
            scrollDown();
        }
        handler.removeCallbacks(reload);
        GetURL.sendSupport(this, msg, new onResponse() {
            @Override
            public void onSuccess(String response) {
                imageView.setImageResource(R.drawable.ic_sent);
                dateView.setText(response);
                isSending = false;
                handler.postDelayed(reload, 1000);
            }

            @Override
            public void onError(int errorCode, String error) {
                handler.postDelayed(reload, 20000);
                isSending = false;
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, Support.this, () -> {
                        conDiag.dismiss();
                        sendMsg(msg, false);
                    });
                } else if (errorCode == 0) {
                    imageView.setImageResource(R.drawable.ic_error);
                    dateView.setText(DataParse.getStr(Support.this, "failed", Home.spf));
                    msgView.setText(error);
                    msgView.setTextColor(Color.GRAY);
                    msgView.setTypeface(msgView.getTypeface(), Typeface.ITALIC);
                } else {
                    Toast.makeText(Support.this, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void initList() {
        listView.removeAllViews();
        if (list.size() == 0) {
            View v = inflater.inflate(R.layout.support_a, null);
            TextView dateView = v.findViewById(R.id.support_dateView);
            TextView msgView = v.findViewById(R.id.support_msgView);
            dateView.setText(DataParse.getStr(this, "hello", Home.spf));
            msgView.setText(DataParse.getStr(this, "support_welcome", Home.spf));
            listView.addView(v);
        } else {
            for (int i = 0; i < list.size(); i++) {
                View v;
                if (list.get(i).get("staff").equals("1")) {
                    v = inflater.inflate(R.layout.support_a, null);
                } else {
                    v = inflater.inflate(R.layout.support_y, null);
                }
                TextView dateView = v.findViewById(R.id.support_dateView);
                TextView msgView = v.findViewById(R.id.support_msgView);
                dateView.setText(list.get(i).get("time"));
                msgView.setText(Misc.html(list.get(i).get("msg")));
                listView.addView(v);
            }
        }
        scrollDown();
    }

    private void scrollDown() {
        handler.postDelayed(runnable, 200);
    }
}
