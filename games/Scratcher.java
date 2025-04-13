package org.mintsoft.mintly.games;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.Guideline;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetGame;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintlib.scratchIt;
import org.mintsoft.mintlib.scratchListener;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Confetti;
import org.mintsoft.mintly.helper.Misc;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class Scratcher extends BaseAppCompat {
    private Dialog loadingDialog, askDiag;
    private ImageView cardHolder;
    private String id, coord, amtWon, imgUrl;
    private int freeChance, canExit;
    private final Bitmap[] bitmaps = new Bitmap[2];
    private final ImageView[] coinIcons = new ImageView[9];
    private final TextView[] amtViews = new TextView[9];
    private Guideline scLeft, scRight, scTop, scBottom;
    private boolean isBusy, rwdGiven;
    private final float visible_percent = 0.7f;
    private LinearLayout scratchHolder;
    private ArrayList<String> data;
    private ActivityResultLauncher<Intent> activityForResult;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setResult(9);
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
        } else {
            String title = extras.getString("name", DataParse.getStr(this, "scratch_n_win", Home.spf));
            imgUrl = extras.getString("image", null);
            coord = extras.getString("coord", null);
            id = extras.getString("id", null);
            canExit = extras.getInt("exit", 0);
            if (coord == null || imgUrl == null || id == null) {
                Toast.makeText(this, DataParse.getStr(this, "invalid_category_selected", Home.spf), Toast.LENGTH_LONG).show();
                finish();
            } else {
                loadingDialog = Misc.loadingDiag(this);
                loadingDialog.show();
                setContentView(R.layout.game_scratcher);
                TextView titleView = findViewById(R.id.game_scratcher_titleView);
                titleView.setText(title);
                cardHolder = findViewById(R.id.game_scratcher_cardHolder);
                scratchHolder = findViewById(R.id.game_scratcher_scratchHolder);
                scLeft = findViewById(R.id.game_scratcher_scratchHolder_guide_left);
                scRight = findViewById(R.id.game_scratcher_scratchHolder_guide_right);
                scTop = findViewById(R.id.game_scratcher_scratchHolder_guide_top);
                scBottom = findViewById(R.id.game_scratcher_scratchHolder_guide_bottom);
                coinIcons[0] = findViewById(R.id.game_scratcher_imageView_1);
                coinIcons[1] = findViewById(R.id.game_scratcher_imageView_2);
                coinIcons[2] = findViewById(R.id.game_scratcher_imageView_3);
                coinIcons[3] = findViewById(R.id.game_scratcher_imageView_4);
                coinIcons[4] = findViewById(R.id.game_scratcher_imageView_5);
                coinIcons[5] = findViewById(R.id.game_scratcher_imageView_6);
                coinIcons[6] = findViewById(R.id.game_scratcher_imageView_7);
                coinIcons[7] = findViewById(R.id.game_scratcher_imageView_8);
                coinIcons[8] = findViewById(R.id.game_scratcher_imageView_9);
                amtViews[0] = findViewById(R.id.game_scratcher_textView_1);
                amtViews[1] = findViewById(R.id.game_scratcher_textView_2);
                amtViews[2] = findViewById(R.id.game_scratcher_textView_3);
                amtViews[3] = findViewById(R.id.game_scratcher_textView_4);
                amtViews[4] = findViewById(R.id.game_scratcher_textView_5);
                amtViews[5] = findViewById(R.id.game_scratcher_textView_6);
                amtViews[6] = findViewById(R.id.game_scratcher_textView_7);
                amtViews[7] = findViewById(R.id.game_scratcher_textView_8);
                amtViews[8] = findViewById(R.id.game_scratcher_textView_9);
                findViewById(R.id.game_scratcher_back).setOnClickListener(view -> onBackPressed());
                new Thread(() -> {
                    try {
                        URL url = new URL(imgUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        bitmaps[0] = BitmapFactory.decodeStream(input);
                        runOnUiThread(() -> {
                            String[] coords = coord.split(",");
                            makeBitmaps(bitmaps[0],
                                    Float.parseFloat(coords[0]),
                                    Float.parseFloat(coords[1]),
                                    Float.parseFloat(coords[2]),
                                    Float.parseFloat(coords[3])
                            );
                            loadingDialog.dismiss();
                            showAskDiag();
                        });
                    } catch (IOException e) {
                        runOnUiThread(() -> {
                            if (loadingDialog != null && loadingDialog.isShowing())
                                loadingDialog.dismiss();
                            Toast.makeText(Scratcher.this, "Failed to load the card!", Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                }).start();
                activityForResult = registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            int resultCode = result.getResultCode();
                            if (resultCode == 55) {
                                if (freeChance != 0) {
                                    Intent intent = new Intent(Scratcher.this, Confetti.class);
                                    intent.putExtra("text", DataParse.getStr(this, "you_won", Home.spf)
                                            + " " + freeChance + " free chances");
                                    intent.putExtra("icon", R.drawable.icon_free);
                                    intent.putExtra("code", 56);
                                    activityForResult.launch(intent);
                                    freeChance = 0;
                                } else if (canExit == 1) finish();
                            } else if (resultCode == 56) {
                                setResult(10);
                                if (canExit == 1) finish();
                            }
                        });
            }
        }
    }

    private void makeBitmaps(Bitmap bitmap, float leftPct, float rightPct, float topPct, float bottomPct) {
        if (leftPct < rightPct && topPct < bottomPct) {
            int bH = bitmap.getHeight();
            int bW = bitmap.getWidth();
            float l = bW * leftPct / 100;
            float r = bW * rightPct / 100;
            float t = bH * topPct / 100;
            float b = bH * bottomPct / 100;
            float offset = 0.08f;
            scLeft.setGuidelinePercent((leftPct - offset) / 100);
            scRight.setGuidelinePercent((rightPct + offset) / 100);
            scTop.setGuidelinePercent((topPct - offset) / 100);
            scBottom.setGuidelinePercent((bottomPct + offset) / 100);
            Bitmap mask1 = Bitmap.createBitmap(bW, bH, Bitmap.Config.ARGB_8888);
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            Canvas canvas1 = new Canvas(mask1);
            canvas1.drawBitmap(bitmap, 0, 0, null);
            canvas1.drawRect(l, t, r, b, paint);
            bitmaps[0] = mask1;
            bitmaps[1] = Bitmap.createBitmap(bitmap, (int) l, (int) t, (int) (r - l), (int) (b - t) + 2);
            cardHolder.setImageBitmap(bitmaps[0]);
            initScratcher();
        } else {
            Toast.makeText(this, DataParse.getStr(this, "invalid_coordination", Home.spf), Toast.LENGTH_LONG).show();
        }
        bitmap.recycle();
        loadingDialog.dismiss();
    }

    private void callNet() {
        if (isBusy) return;
        isBusy = true;
        if (!loadingDialog.isShowing()) loadingDialog.show();
        GetGame.scratcherResult(this, id, new onResponse() {
            @Override
            public void onSuccessHashMap(HashMap<String, String> dta) {
                setResult(8);
                amtWon = dta.get("won");
                data = new ArrayList<>();
                for (int i = 1; i < dta.size(); i++) {
                    data.add(dta.get(String.valueOf(i - 1)));
                }
                loadingDialog.dismiss();
            }

            @Override
            public void onError(int errorCode, String error) {
                Toast.makeText(Scratcher.this, error, Toast.LENGTH_LONG).show();
                loadingDialog.dismiss();
            }
        });

    }

    private void initScratcher() {
        scratchIt scratcher = new scratchIt(this);
        scratcher.setScratchDrawable(new BitmapDrawable(getResources(), bitmaps[1]));
        scratcher.setScratchWidth(100);
        scratcher.setScratchable(true);
        scratchHolder.removeAllViews();
        scratchHolder.addView(scratcher);
        scratchHolder.setBackground(null);
        scratcher.setOnScratchListener(new scratchListener() {
            @Override
            public void onStart() {
                for (int i = 0; i < data.size(); i++) {
                    if (data.get(i).equals("0")) {
                        freeChance++;
                        coinIcons[i].setVisibility(View.INVISIBLE);
                        amtViews[i].setText(DataParse.getStr(Scratcher.this, "free", Home.spf));
                        amtViews[i].setTextColor(Color.WHITE);
                        amtViews[i].setAllCaps(true);
                    } else {
                        amtViews[i].setText(String.valueOf(data.get(i)));
                    }
                }
            }

            @Override
            public void onScratch(scratchIt scratchCard, float visiblePercent) {
                if (rwdGiven) return;
                if (visiblePercent > visible_percent && isBusy) {
                    isBusy = false;
                    rwdGiven = true;
                    if (!amtWon.equals("0")) {
                        Home.balance = String.valueOf(Integer.parseInt(Home.balance) + Integer.parseInt(amtWon));
                        Intent intent = new Intent(Scratcher.this, Confetti.class);
                        intent.putExtra("text", DataParse.getStr(Scratcher.this, "you_won", Home.spf)
                                + " " + amtWon + " " + Home.currency.toLowerCase() + "s");
                        intent.putExtra("icon", R.drawable.icon_coin);
                        intent.putExtra("code", 55);
                        activityForResult.launch(intent);
                    }
                } else if (isBusy) {
                    Toast.makeText(Scratcher.this, DataParse.getStr(Scratcher.this, "keep_scratching", Home.spf), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showAskDiag() {
        if (askDiag == null) {
            askDiag = Misc.decoratedDiag(this, R.layout.dialog_sc, 0.7f);
            TextView textView = askDiag.findViewById(R.id.dialog_sc_textView);
            textView.setText(DataParse.getStr(this, "scratcher_popup_text", Home.spf));
            Button laterBtn = askDiag.findViewById(R.id.dialog_sc_laterBtn);
            laterBtn.setText(DataParse.getStr(this, "later", Home.spf));
            askDiag.findViewById(R.id.dialog_sc_laterBtn).setOnClickListener(view -> {
                askDiag.dismiss();
                finish();
            });
            askDiag.findViewById(R.id.dialog_sc_yesBtn).setOnClickListener(view -> {
                askDiag.dismiss();
                callNet();
            });
        }
        askDiag.show();
    }

    @Override
    public void onBackPressed() {
        if (isBusy) {
            Toast.makeText(this, DataParse.getStr(this, "scratcher_exit_toast", Home.spf), Toast.LENGTH_LONG).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        for (Bitmap b : bitmaps) if (b != null && !b.isRecycled()) b.recycle();
        super.onDestroy();
    }
}