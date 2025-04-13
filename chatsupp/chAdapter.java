package org.mintsoft.mintly.chatsupp;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aghajari.emojiview.view.AXEmojiTextView;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.Misc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;

class chAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final LayoutInflater inflater;
    private static final int TXT_SENT = 1, TXT_RECEIVED = 2, IMG_SENT = 3, IMG_RECEIVED = 4,
            AUDIO_SENT = 5, AUDIO_RECEIVED = 6;
    private final int me, others, admin, pad_high, pad_normal, pad_narrow, em_sm, em_lg;
    private final String userid, selfDom;
    private int lastPos = -1;
    private final RecyclerView rV;
    private final ArrayList<HashMap<String, String>> list;
    private final Context context;
    private final Picasso picasso;
    private final Map<String, String> audioHeader;
    private final Aplayer aplayer;
    private final EditText editText;
    private final InputMethodManager mgr;
    private Dialog aDiag;
    private String blocked, txt_, user_id = null;

    chAdapter(Context context, RecyclerView recyclerView, ArrayList<HashMap<String, String>> list, EditText editText) {
        this.list = list;
        this.context = context;
        this.editText = editText;
        this.selfDom = "https://" + context.getString(R.string.domain_name);
        mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        me = ContextCompat.getColor(context, R.color.colorPrimary);
        others = ContextCompat.getColor(context, R.color.blue_4);
        admin = Color.RED;
        inflater = LayoutInflater.from(context);
        userid = GetAuth.user(context);
        rV = recyclerView;
        pad_high = Misc.dpToPx(context, 15);
        pad_normal = Misc.dpToPx(context, 5);
        pad_narrow = Misc.dpToPx(context, 2);
        em_sm = Misc.dpToPx(context, 19);
        em_lg = Misc.dpToPx(context, 36);
        picasso = new Picasso.Builder(context)
                .downloader(new OkHttp3Downloader(new OkHttpClient.Builder()
                        .addInterceptor(chain -> {
                            Request newRequest = chain.request().newBuilder()
                                    .addHeader("Authorization", GetAuth.cred(context))
                                    .build();
                            return chain.proceed(newRequest);
                        })
                        .build()))
                .build();
        aplayer = Aplayer.getInstance();
        audioHeader = new HashMap<>();
        audioHeader.put("Authorization", GetAuth.cred(context));
        blocked = Home.spf.getString("cblk", "");
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(this);
    }

    void addItems(ArrayList<HashMap<String, String>> newList) {
        int s = list.size();
        list.addAll(newList);
        notifyItemRangeInserted(s, newList.size());
        rV.scrollToPosition(list.size() - 1);
    }

    String getLastUid() {
        if (list.size() > 0) {
            return list.get(list.size() - 1).get("uid");
        } else {
            return "";
        }
    }

    @Override
    public int getItemViewType(int position) {
        String msg = list.get(position).get("msg");
        String uid = list.get(position).get("uid");
        boolean isSent = uid != null && uid.equals(userid);
        if (blocked.contains(uid + "||")) {
            if (isSent) {
                return TXT_SENT;
            } else {
                return TXT_RECEIVED;
            }
        }
        if (msg.startsWith("http")) {
            String ext = msg.substring(msg.length() - 4);
            if (ext.equals("jpeg") || ext.equals(".jpg") || ext.equals(".png") || ext.equals(".gif")) {
                if (isSent) {
                    return IMG_SENT;
                } else {
                    return IMG_RECEIVED;
                }
            } else if (ext.equals(".mp3") || ext.equals(".mp4")) {
                if (isSent) {
                    return AUDIO_SENT;
                } else {
                    return AUDIO_RECEIVED;
                }
            }
        }
        if (isSent) {
            return TXT_SENT;
        } else {
            return TXT_RECEIVED;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TXT_SENT) {
            return new vhText(inflater.inflate(R.layout.chat_text_me, parent, false));
        } else if (viewType == TXT_RECEIVED) {
            return new vhText(inflater.inflate(R.layout.chat_text_others, parent, false));
        } else if (viewType == IMG_SENT) {
            return new vhImg(inflater.inflate(R.layout.chat_img_me, parent, false));
        } else if (viewType == IMG_RECEIVED) {
            return new vhImg(inflater.inflate(R.layout.chat_img_others, parent, false));
        } else if (viewType == AUDIO_SENT) {
            return new vhAudio(inflater.inflate(R.layout.chat_audio_me, parent, false));
        } else if (viewType == AUDIO_RECEIVED) {
            return new vhAudio(inflater.inflate(R.layout.chat_audio_others, parent, false));
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HashMap<String, String> data = list.get(position);
        String msg = data.get("msg");
        int vT = getItemViewType(position);
        if (vT == IMG_SENT || vT == IMG_RECEIVED) {
            vhImg vh = (vhImg) holder;
            vh.nameView.setText("By: " + data.get("name"));
            vh.timeView.setText(getTime(data.get("time")));
            String avatar = data.get("avatar");
            if (avatar == null || avatar.isEmpty() || avatar.equals("none")) {
                vh.avatarHolder.setVisibility(View.VISIBLE);
                vh.avatarView.setImageResource(R.drawable.avatar);
                vh.itemHolder.setPadding(0, pad_high, 0, pad_normal);
            } else if (avatar.equals("hide")) {
                vh.avatarHolder.setVisibility(View.INVISIBLE);
                vh.itemHolder.setPadding(0, pad_narrow, 0, pad_normal);
            } else {
                vh.avatarHolder.setVisibility(View.VISIBLE);
                Picasso.get().load(data.get("avatar")).placeholder(R.drawable.avatar)
                        .error(R.drawable.avatar).into(vh.avatarView);
                vh.itemHolder.setPadding(0, pad_high, 0, pad_normal);
            }
            if (data.get("uid").equals(userid)) {
                vh.nameView.setTextColor(me);
            } else if (data.get("staff").equals("1")) {
                vh.nameView.setTextColor(admin);
            } else {
                vh.nameView.setTextColor(others);
            }
            if (msg.startsWith(selfDom)) {
                picasso.load(msg).placeholder(R.drawable.anim_loading)
                        .error(R.drawable.chat_img_placeholder)
                        .into(vh.imageView);
            } else {
                Picasso.get().load(msg).placeholder(R.drawable.anim_loading)
                        .error(R.drawable.chat_img_placeholder)
                        .into(vh.imageView);
            }
        } else if (vT == AUDIO_SENT || vT == AUDIO_RECEIVED) {
            vhAudio vh = (vhAudio) holder;
            vh.nameView.setText(data.get("name"));
            vh.timeView.setText(getTime(data.get("time")));
            String avatar = data.get("avatar");
            if (avatar == null || avatar.isEmpty() || avatar.equals("none")) {
                vh.avatarHolder.setVisibility(View.VISIBLE);
                vh.avatarView.setImageResource(R.drawable.avatar);
                vh.itemHolder.setPadding(0, pad_high, 0, pad_normal);
            } else if (avatar.equals("hide")) {
                vh.avatarHolder.setVisibility(View.INVISIBLE);
                vh.itemHolder.setPadding(0, pad_narrow, 0, pad_normal);
            } else {
                vh.avatarHolder.setVisibility(View.VISIBLE);
                Picasso.get().load(data.get("avatar")).placeholder(R.drawable.avatar)
                        .error(R.drawable.avatar).into(vh.avatarView);
                vh.itemHolder.setPadding(0, pad_high, 0, pad_normal);
            }
            if (data.get("uid").equals(userid)) {
                vh.nameView.setTextColor(me);
            } else if (data.get("staff").equals("1")) {
                vh.nameView.setTextColor(admin);
            } else {
                vh.nameView.setTextColor(others);
            }
            vh.playBtn.setOnClickListener(view -> playAudio(position, msg));
        } else {
            vhText vh = (vhText) holder;
            String name = data.get("name");
            vh.nameView.setText(name);
            vh.timeView.setText(getTime(data.get("time")));
            String avatar = data.get("avatar");
            if (avatar == null || avatar.isEmpty() || avatar.equals("none")) {
                vh.avatarHolder.setVisibility(View.VISIBLE);
                vh.avatarView.setImageResource(R.drawable.avatar);
                vh.itemHolder.setPadding(0, pad_high, 0, pad_normal);
            } else if (avatar.equals("hide")) {
                vh.avatarHolder.setVisibility(View.INVISIBLE);
                vh.itemHolder.setPadding(0, pad_narrow, 0, pad_normal);
            } else {
                vh.avatarHolder.setVisibility(View.VISIBLE);
                Picasso.get().load(data.get("avatar")).placeholder(R.drawable.avatar)
                        .error(R.drawable.avatar).into(vh.avatarView);
                vh.itemHolder.setPadding(0, pad_high, 0, pad_normal);
            }
            boolean isMe = data.get("uid").equals(userid);
            if (isMe) {
                vh.nameView.setTextColor(me);
            } else if (data.get("staff").equals("1")) {
                vh.nameView.setTextColor(admin);
            } else {
                vh.nameView.setTextColor(others);
            }
            boolean isBlocked = blocked.contains(data.get("uid") + "||");
            if (isBlocked) {
                vh.msgView.setText(DataParse.getStr(context, "blocked_user_msg", Home.spf));
            } else {
                vh.msgView.setText(Misc.html(msg));
            }
            boolean emojiOnly = emojiOnly(msg);
            if (emojiOnly) {
                vh.msgView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                vh.msgView.setEmojiSize(em_lg);
            } else {
                vh.msgView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                vh.msgView.setEmojiSize(em_sm);
            }
            if (isBlocked) {
                vh.msgView.setTypeface(vh.msgView.getTypeface(), Typeface.ITALIC);
                vh.msgView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                vh.msgView.setEmojiSize(em_sm);
            } else {
                vh.msgView.setTypeface(vh.msgView.getTypeface(), Typeface.NORMAL);
            }
            if (isMe) {
                vh.msgHolder.setBackgroundResource(R.drawable.chat_bg_me);
            } else if (emojiOnly) {
                vh.msgHolder.setBackgroundResource(R.drawable.chat_bg_others);
            } else {
                if (msg.contains("@" + Home.uName)) {
                    vh.msgHolder.setBackgroundResource(R.drawable.chat_bg_highlight);
                } else {
                    vh.msgHolder.setBackgroundResource(R.drawable.chat_bg_others);
                }
            }
        }

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private class vhText extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView avatarView;
        CardView avatarHolder;
        RelativeLayout itemHolder;
        TextView nameView, timeView;
        AXEmojiTextView msgView;
        RelativeLayout msgHolder;

        public vhText(@NonNull View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.chat_item_avatarView);
            avatarHolder = itemView.findViewById(R.id.chat_item_avatarHolder);
            itemHolder = itemView.findViewById(R.id.chat_item_holder);
            nameView = itemView.findViewById(R.id.chat_item_nameView);
            timeView = itemView.findViewById(R.id.chat_item_timeView);
            msgView = itemView.findViewById(R.id.chat_item_messageView);
            msgHolder = itemView.findViewById(R.id.chat_item_msgHolder);
            itemView.setOnClickListener(this);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onClick(View view) {
            HashMap<String, String> data = list.get(getAbsoluteAdapterPosition());
            if (!data.get("uid").equals(userid)) {
                String txt = editText.getText().toString();
                if (txt.isEmpty()) {
                    txt = "@" + data.get("name") + " ";
                } else {
                    txt += " @" + data.get("name") + " ";
                }
                showActions(data.get("uid"), data.get("name"), txt);
            }
        }
    }

    private class vhImg extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView avatarView, imageView;
        CardView avatarHolder;
        RelativeLayout itemHolder;
        TextView nameView, timeView;

        public vhImg(@NonNull View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.chat_item_avatarView);
            avatarHolder = itemView.findViewById(R.id.chat_item_avatarHolder);
            itemHolder = itemView.findViewById(R.id.chat_item_holder);
            nameView = itemView.findViewById(R.id.chat_item_nameView);
            timeView = itemView.findViewById(R.id.chat_item_timeView);
            imageView = itemView.findViewById(R.id.chat_item_imageView);
            itemView.setOnClickListener(this);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onClick(View view) {
            HashMap<String, String> data = list.get(getAbsoluteAdapterPosition());
            if (!data.get("uid").equals(userid)) {
                String txt = editText.getText().toString();
                if (txt.isEmpty()) {
                    txt = "@" + data.get("name") + " ";
                } else {
                    txt += " @" + data.get("name") + " ";
                }
                showActions(data.get("uid"), data.get("name"), txt);
            }
        }
    }

    private class vhAudio extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView avatarView, playBtn;
        CardView avatarHolder;
        RelativeLayout itemHolder;
        TextView nameView, timeView, durationView;
        SeekBar seekBar;

        public vhAudio(@NonNull View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.chat_item_avatarView);
            avatarHolder = itemView.findViewById(R.id.chat_item_avatarHolder);
            itemHolder = itemView.findViewById(R.id.chat_item_holder);
            nameView = itemView.findViewById(R.id.chat_item_nameView);
            timeView = itemView.findViewById(R.id.chat_item_timeView);
            playBtn = itemView.findViewById(R.id.chat_item_audio_playBtn);
            seekBar = itemView.findViewById(R.id.chat_item_audio_seekBar);
            durationView = itemView.findViewById(R.id.chat_item_audio_durationView);
            itemView.setOnClickListener(this);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onClick(View view) {
            HashMap<String, String> data = list.get(getAbsoluteAdapterPosition());
            if (!data.get("uid").equals(userid)) {
                String txt = editText.getText().toString();
                if (txt.isEmpty()) {
                    txt = "@" + data.get("name") + " ";
                } else {
                    txt += " @" + data.get("name") + " ";
                }
                showActions(data.get("uid"), data.get("name"), txt);
            }
        }
    }

    private String getTime(String time) {
        return new SimpleDateFormat("h:mm a - MMM d", Locale.getDefault()).format(new Date(Long.parseLong(time)));
    }

    private static final Pattern pattern = Pattern.compile("^[\\s\n\r]*(?:(?:[\u00a9\u00ae\u203c\u2049\u2122\u2139\u2194-\u2199\u21a9-\u21aa\u231a-\u231b\u2328\u23cf\u23e9-\u23f3\u23f8-\u23fa\u24c2\u25aa-\u25ab\u25b6\u25c0\u25fb-\u25fe\u2600-\u2604\u260e\u2611\u2614-\u2615\u2618\u261d\u2620\u2622-\u2623\u2626\u262a\u262e-\u262f\u2638-\u263a\u2648-\u2653\u2660\u2663\u2665-\u2666\u2668\u267b\u267f\u2692-\u2694\u2696-\u2697\u2699\u269b-\u269c\u26a0-\u26a1\u26aa-\u26ab\u26b0-\u26b1\u26bd-\u26be\u26c4-\u26c5\u26c8\u26ce-\u26cf\u26d1\u26d3-\u26d4\u26e9-\u26ea\u26f0-\u26f5\u26f7-\u26fa\u26fd\u2702\u2705\u2708-\u270d\u270f\u2712\u2714\u2716\u271d\u2721\u2728\u2733-\u2734\u2744\u2747\u274c\u274e\u2753-\u2755\u2757\u2763-\u2764\u2795-\u2797\u27a1\u27b0\u27bf\u2934-\u2935\u2b05-\u2b07\u2b1b-\u2b1c\u2b50\u2b55\u3030\u303d\u3297\u3299\ud83c\udc04\ud83c\udccf\ud83c\udd70-\ud83c\udd71\ud83c\udd7e-\ud83c\udd7f\ud83c\udd8e\ud83c\udd91-\ud83c\udd9a\ud83c\ude01-\ud83c\ude02\ud83c\ude1a\ud83c\ude2f\ud83c\ude32-\ud83c\ude3a\ud83c\ude50-\ud83c\ude51\u200d\ud83c\udf00-\ud83d\uddff\ud83d\ude00-\ud83d\ude4f\ud83d\ude80-\ud83d\udeff\ud83e\udd00-\ud83e\uddff\udb40\udc20-\udb40\udc7f]|\u200d[\u2640\u2642]|[\ud83c\udde6-\ud83c\uddff]{2}|.[\u20e0\u20e3\ufe0f]+)+[\\s\n\r]*)+$");

    private boolean emojiOnly(String msg) {
        return pattern.matcher(msg).find();
    }

    private void playAudio(int newPos, String url) {
        Uri uri = Uri.parse(url);
        RecyclerView.LayoutManager layoutManager = rV.getLayoutManager();
        if (layoutManager != null) {
            Aplayer aplayer = Aplayer.getInstance();
            if (lastPos == newPos) {
                if (aplayer.isPlaying()) {
                    aplayer.pause();
                } else {
                    aplayer.play();
                }
            } else {
                aplayer.release();
                View itemView = layoutManager.findViewByPosition(newPos);
                if (itemView != null) {
                    ImageView playBtn = itemView.findViewById(R.id.chat_item_audio_playBtn);
                    SeekBar seekBar = itemView.findViewById(R.id.chat_item_audio_seekBar);
                    TextView durationView = itemView.findViewById(R.id.chat_item_audio_durationView);
                    if (uri != null) {
                        if (url.startsWith(selfDom)) {
                            aplayer.init(context, uri, audioHeader).setPlayView(playBtn)
                                    .setSeekBar(seekBar).setPlaytime(durationView);
                        } else {
                            aplayer.init(context, uri, null).setPlayView(playBtn)
                                    .setSeekBar(seekBar).setPlaytime(durationView);
                        }
                        aplayer.play();
                    }
                }
                lastPos = newPos;
            }
        }
    }

    public void playerRelease() {
        if (aplayer != null) aplayer.release();
    }

    @SuppressLint({"NotifyDataSetChanged", "ApplySharedPref"})
    private void showActions(String uid, String name, String txt) {
        user_id = uid;
        txt_ = txt;
        if (aDiag == null) {
            aDiag = Misc.decoratedDiag(context, R.layout.chat_options, 0.6f);
            Window w = aDiag.getWindow();
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.dimAmount = 0.6f;
            lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            w.setAttributes(lp);
            aDiag.setCancelable(true);
            aDiag.setCanceledOnTouchOutside(true);
            TextView opReply = aDiag.findViewById(R.id.chat_option_reply);
            opReply.setText(DataParse.getStr(context, "reply", Home.spf));
            opReply.setOnClickListener(view -> {
                aDiag.dismiss();
                editText.setText(txt_);
                editText.setSelection(txt_.length());
                editText.requestFocus();
                mgr.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            });
            aDiag.findViewById(R.id.chat_option_block).setOnClickListener(view -> {
                if (user_id != null) {
                    if (blocked.contains(user_id + "||")) {
                        blocked = blocked.replace(user_id + "||", "");
                    } else {
                        blocked += user_id + "||";
                    }
                    Home.spf.edit().putString("cblk", blocked).commit();
                    notifyDataSetChanged();
                }
                aDiag.dismiss();
            });
            TextView opReport = aDiag.findViewById(R.id.chat_option_report);
            opReport.setText(DataParse.getStr(context, "report", Home.spf));
            opReport.setOnClickListener(view -> {
                aDiag.dismiss();
                new Handler().postDelayed(() -> Toast.makeText(context, DataParse.getStr(context, "user_reported", Home.spf),
                        Toast.LENGTH_LONG).show(), new Random().nextInt(5000) + 1000);
            });
            aDiag.findViewById(R.id.chat_option_close).setOnClickListener(view -> aDiag.dismiss());
        }
        aDiag.setOnDismissListener(dialogInterface -> {
            user_id = null;
            txt_ = "";
        });
        TextView bTxtView = aDiag.findViewById(R.id.chat_option_block);
        if (blocked.contains(user_id + "||")) {
            bTxtView.setText(DataParse.getStr(context, "unblock", Home.spf));
            int col = ContextCompat.getColor(context, R.color.green_2);
            bTxtView.setTextColor(col);
            TextViewCompat.setCompoundDrawableTintList(bTxtView, ColorStateList.valueOf(col));
        } else {
            bTxtView.setText(DataParse.getStr(context, "block", Home.spf));
            int col = ContextCompat.getColor(context, R.color.red_1);
            bTxtView.setTextColor(col);
            TextViewCompat.setCompoundDrawableTintList(bTxtView, ColorStateList.valueOf(col));
        }
        TextView nView = aDiag.findViewById(R.id.chat_option_nameView);
        nView.setText(name);
        aDiag.show();
    }
}