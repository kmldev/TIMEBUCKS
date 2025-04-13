package org.mintsoft.mintly.offers;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.squareup.picasso.Picasso;
import com.ytplayer.library.player.PlayerConstants;
import com.ytplayer.library.player.YouTubePlayer;
import com.ytplayer.library.player.listeners.AbstractYouTubePlayerListener;
import com.ytplayer.library.player.listeners.YouTubePlayerFullScreenListener;
import com.ytplayer.library.player.views.YouTubePlayerView;
import com.ytplayer.library.ui.PlayerUiController;

import org.mintsoft.mintlib.Customoffers;
import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Confetti;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.util.ArrayList;
import java.util.HashMap;

public class Yt extends BaseAppCompat {
    private int position = -1, margin;
    private View topBar;
    public String _id, currency;
    private boolean isPlaying, isCancelled, isPaused, isLive;
    private ListView listView;
    private Dialog loadingDiag, conDiag, quitDiag;
    private yAdapter adapter;
    private LayoutInflater inflater;
    private YouTubePlayer player;
    private YouTubePlayerView youTubePlayerView;
    private PlayerUiController playerUiController;
    private LinearLayout.LayoutParams params;
    private ArrayList<HashMap<String, String>> list;


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        isLive = true;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        loadingDiag = Misc.loadingDiagExit(this);
        loadingDiag.show();
        setContentView(R.layout.offers_ytube);
        TextView titleView = findViewById(R.id.offers_ytube_title);
        titleView.setText(DataParse.getStr(this, "videos", Home.spf));
        TextView headerView = findViewById(R.id.offers_ytube_header);
        headerView.setText(DataParse.getStr(this, "yt_avl_vid", Home.spf));
        youTubePlayerView = findViewById(R.id.offers_ytube_playerView);
        if (Home.spf.getBoolean("is_hw", true)) {
            youTubePlayerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            youTubePlayerView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        listView = findViewById(R.id.offers_ytube_listView);
        topBar = findViewById(R.id.offers_ytube_topBar);
        params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        margin = Misc.dpToPx(this, 14);
        params.setMargins(margin, margin, margin, margin);
        youTubePlayerView.setLayoutParams(params);
        list = Variables.getArrayHash("ytvideos_list");
        findViewById(R.id.offers_ytube_back).setOnClickListener(view -> onBackPressed());
        currency = " " + Home.currency.toLowerCase() + "s";
        inflater = LayoutInflater.from(this);
        if (list == null) {
            netCall();
        } else {
            initList();
        }
    }

    @Override
    protected void onStop() {
        isLive = false;
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (youTubePlayerView != null && youTubePlayerView.isFullScreen()) {
            youTubePlayerView.toggleFullScreen();
        } else if (isPlaying) {
            if (quitDiag == null) {
                quitDiag = Misc.decoratedDiag(this, R.layout.dialog_quit, 0.6f);
                TextView quitTv = quitDiag.findViewById(R.id.dialog_quit_desc);
                quitTv.setText(DataParse.getStr(this, "close_diag_desc_v", Home.spf));
                quitDiag.findViewById(R.id.dialog_quit_no).setOnClickListener(view -> quitDiag.dismiss());
                quitDiag.findViewById(R.id.dialog_quit_yes).setOnClickListener(view -> {
                    quitDiag.dismiss();
                    super.onBackPressed();
                });
            }
            quitDiag.show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isPlaying) player.a();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isPaused) player.b();
    }

    @Override
    protected void onDestroy() {
        isLive = false;
        Variables.setArrayHash("ytvideos_list", list);
        youTubePlayerView.release();
        adapter = null;
        super.onDestroy();
    }

    private void netCall() {
        Customoffers.getYt(this, new onResponse() {
            @Override
            public void onSuccessListHashMap(ArrayList<HashMap<String, String>> lists) {
                if (!isLive) return;
                list = lists;
                initList();
            }

            @Override
            public void onError(int errorCode, String error) {
                if (!isLive) return;
                loadingDiag.dismiss();
                if (errorCode == -9) {
                    conDiag = Misc.noConnection(conDiag, Yt.this, () -> {
                        netCall();
                        conDiag.dismiss();
                    });
                } else {
                    Toast.makeText(Yt.this, errorCode + ": " + error, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    private void initList() {
        adapter = new yAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            if (isPlaying) {
                Toast.makeText(this, DataParse.getStr(this, "yt_playing_wait", Home.spf), Toast.LENGTH_LONG).show();
            } else {
                if (player == null) {
                    Toast.makeText(this, DataParse.getStr(this, "yt_player_not_ready", Home.spf), Toast.LENGTH_LONG).show();
                } else {
                    isPlaying = true;
                    position = i;
                    _id = list.get(position).get("id");
                    isCancelled = false;
                    player.loadVideo(list.get(position).get("vid"), 0);
                    adapter.notifyDataSetChanged();
                }
            }
        });
        playerUiController = youTubePlayerView.getPlayerUiController();
        playerUiController.showSeekBar(false);
        playerUiController.showFullscreenButton(true);
        playerUiController.showMenuButton(false);
        playerUiController.showYouTubeButton(false);
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                if (!isLive) return;
                player = youTubePlayer;
                playerUiController.removeLoadingBar(true);
                loadingDiag.dismiss();
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.ENDED && !isCancelled) {
                    if (!isLive) return;
                    Customoffers.rewardYt(Yt.this, _id, new onResponse() {
                        @Override
                        public void onSuccess(String response) {
                            if (!isLive) return;
                            super.onSuccess(response);
                            adapter.remove(position);
                            isPlaying = false;
                            Intent intent = new Intent(Yt.this, Confetti.class);
                            intent.putExtra("text", "You received " + response + " " + Home.currency.toLowerCase() + "s");
                            intent.putExtra("icon", R.drawable.icon_coin);
                            startActivity(intent);
                        }

                        @Override
                        public void onError(int errorCode, String error) {
                            if (!isLive) return;
                            isPlaying = false;
                            Toast.makeText(Yt.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
                } else if (state == PlayerConstants.PlayerState.PAUSED) {
                    isPaused = true;
                }
            }
        });
        youTubePlayerView.addFullScreenListener(new YouTubePlayerFullScreenListener() {
            @Override
            public void onYouTubePlayerEnterFullScreen() {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                topBar.setVisibility(View.GONE);
                params.setMargins(0, 0, 0, 0);
                youTubePlayerView.setLayoutParams(params);
            }

            @SuppressLint("SourceLockedOrientationActivity")
            @Override
            public void onYouTubePlayerExitFullScreen() {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                topBar.setVisibility(View.VISIBLE);
                params.setMargins(margin, margin, margin, margin);
                youTubePlayerView.setLayoutParams(params);
            }
        });
    }

    private class yAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) view = inflater.inflate(R.layout.offers_ytube_item, viewGroup, false);
            ImageView imageView = view.findViewById(R.id.offers_ytube_item_imageView);
            TextView titleView = view.findViewById(R.id.offers_ytube_item_titleView);
            TextView amtView = view.findViewById(R.id.offers_ytube_item_amtView);
            titleView.setText(list.get(i).get("title"));
            amtView.setText(("Receive " + list.get(i).get("amount") + " " + currency));
            Picasso.get().load("https://img.youtube.com/vi/"
                            + list.get(i).get("vid") + "/mqdefault.jpg")
                    .placeholder(R.drawable.anim_loading)
                    .into(imageView);
            RelativeLayout lout = view.findViewById(R.id.offers_ytube_item_holder);
            if (i == position) {
                lout.setBackgroundResource(R.drawable.rc_blue);
                lout.setAlpha(0.7f);
            } else {
                lout.setBackgroundResource(R.drawable.ripple_rc_colorprimary_light);
                lout.setAlpha(1f);
            }
            return view;
        }

        void remove(int pos) {
            if (list.size() > pos) {
                String v = list.get(pos).get("id");
                if (v != null && v.equals(_id)) {
                    list.remove(pos);
                    notifyDataSetChanged();
                }
            }
        }
    }
}
