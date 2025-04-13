package org.mintsoft.mintly.games;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.imagePuzzle;
import org.mintsoft.mintlib.ipListener;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;
import org.mintsoft.mintly.offers.GlobalAds;

import java.util.ArrayList;
import java.util.List;

public class Imagepuzzle extends BaseAppCompat {
    private boolean isLocked, isImgShowing;
    private TextView diagAmtView;
    private Dialog congratsDiag, quitDiag, lowBalDiag, conDiag;
    private imagePuzzle iPuzzle;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.game_imagepuzzle);
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
        } else {
            String cat = extras.getString("cat", null);
            int row = Integer.parseInt(extras.getString("row", "3"));
            int col = Integer.parseInt(extras.getString("col", "4"));
            if (cat == null) {
                Toast.makeText(this, DataParse.getStr(this,"invalid_category_selected",Home.spf), Toast.LENGTH_LONG).show();
                finish();
            } else {
                TextView titleView = findViewById(R.id.game_imagepuzzle_title);
                titleView.setText(DataParse.getStr(this,"imagepuzzle",Home.spf));
                RelativeLayout gridView = findViewById(R.id.game_imagepuzzle_grid);
                TextView progressBar = findViewById(R.id.game_imagepuzzle_progress);
                progressBar.setText(DataParse.getStr(this,"please_wait",Home.spf));
                ImageView fullImageView = findViewById(R.id.game_imagepuzzle_image);
                TextView verifyView = findViewById(R.id.game_imagepuzzle_verifyView);
                verifyView.setText(DataParse.getStr(this,"ip_finished",Home.spf));
                ProgressBar timeProgress = findViewById(R.id.game_imagepuzzle_timeProgress);
                View verifyBtn = findViewById(R.id.game_imagepuzzle_verify);
                TextView scoreView = findViewById(R.id.game_imagepuzzle_scoreView);
                iPuzzle = new imagePuzzle(this);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                iPuzzle.setLayoutParams(params);
                iPuzzle.localize(DataParse.getStr(this,"please_wait",Home.spf), DataParse.getStr(this,"ip_finished",Home.spf),
                        DataParse.getStr(this,"try_another_round",Home.spf), DataParse.getStr(this,"ip_score",Home.spf),
                        DataParse.getStr(this,"not_in_game",Home.spf), DataParse.getStr(this,"ip_not_solved",Home.spf),
                        DataParse.getStr(this,"timeup",Home.spf));
                iPuzzle.setBgcolor(Color.DKGRAY);
                gridView.addView(iPuzzle);
                iPuzzle.init(cat, row, col, progressBar, fullImageView, verifyView,
                        timeProgress, verifyBtn, scoreView, R.layout.game_imagepuzzle_item,
                        R.id.game_imagepuzzle_item_holder, new ipListener() {
                            @Override
                            public void setupViews(List<Integer> list, List<View> viewList, ArrayList<Bitmap> imgList) {
                                for (int i = 0; i < list.size(); i++) {
                                    View itemView = viewList.get(list.get(i));
                                    TextView tv = itemView.findViewById(R.id.game_imagepuzzle_item_text);
                                    tv.setText(String.valueOf(i));
                                    ImageView iv = itemView.findViewById(R.id.game_imagepuzzle_item_image);
                                    iv.setImageBitmap(imgList.get(i + 1));
                                }
                            }

                            @Override
                            public void connectionError(int type) {
                                conDiag = Misc.noConnection(conDiag, Imagepuzzle.this, () -> {
                                    iPuzzle.callNet(type);
                                    conDiag.dismiss();
                                });
                            }

                            @Override
                            public void onFoundImage(String imgUrl) {
                                Picasso.get().load(imgUrl).into(new Target() {
                                    @Override
                                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                        iPuzzle.scaleCenterCrop(bitmap);
                                    }

                                    @Override
                                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                                        Toast.makeText(Imagepuzzle.this, "Image failed to load!", Toast.LENGTH_LONG).show();
                                        finish();
                                    }

                                    @Override
                                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                                    }
                                });
                            }

                            @Override
                            public void onResult(int score) {
                                setResult(score);
                            }

                            @Override
                            public void onDiag(int type, String response) {
                                if (type == 1) {
                                    showDiag(response);
                                } else if (type == 2) {
                                    showLowBalDiag();
                                }
                            }

                            @Override
                            public void onLocked(boolean locked) {
                                isLocked = locked;
                            }

                            @Override
                            public void layoutHeight(int height) {
                                gridView.getLayoutParams().height = height;
                            }
                        });

                findViewById(R.id.game_imagepuzzle_showimg).setOnClickListener(view -> {
                    if (isImgShowing) {
                        isImgShowing = false;
                        fullImageView.setVisibility(View.GONE);
                    } else {
                        isImgShowing = true;
                        fullImageView.setVisibility(View.VISIBLE);
                    }
                });
                fullImageView.setOnClickListener(view -> {
                    if (isImgShowing) {
                        fullImageView.setVisibility(View.GONE);
                        isImgShowing = false;
                    }
                });
                findViewById(R.id.game_imagepuzzle_close).setOnClickListener(view -> onBackPressed());
                GlobalAds.fab(this, "fab_ip");
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (isLocked) {
            showQuitDiag();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        iPuzzle.onDestroy();
        super.onDestroy();
    }

    private void showDiag(String wAmt) {
        if (quitDiag != null && quitDiag.isShowing()) quitDiag.dismiss();
        if (congratsDiag == null) {
            congratsDiag = Misc.decoratedDiag(this, R.layout.dialog_quiz_post, 0.8f);
            diagAmtView = congratsDiag.findViewById(R.id.dialog_quiz_post_title);
            TextView desc = congratsDiag.findViewById(R.id.dialog_quiz_post_desc);
            desc.setText(DataParse.getStr(this,"ip_congrats",Home.spf));
            Button qB = congratsDiag.findViewById(R.id.dialog_quiz_post_quit);
            qB.setText(DataParse.getStr(this,"back",Home.spf));
            qB.setOnClickListener(view -> congratsDiag.dismiss());
            Button nB = congratsDiag.findViewById(R.id.dialog_quiz_post_next);
            nB.setText(DataParse.getStr(this,"ip_next",Home.spf));
            nB.setOnClickListener(view -> {
                //callNet();
                congratsDiag.dismiss();
            });
        }
        diagAmtView.setText(("Received " + wAmt + " " + Home.currency.toLowerCase() + "s"));
        congratsDiag.show();
    }

    private void showQuitDiag() {
        if (congratsDiag != null && congratsDiag.isShowing()) congratsDiag.dismiss();
        if (quitDiag == null) {
            quitDiag = Misc.decoratedDiag(this, R.layout.dialog_quit, 0.8f);
            TextView titleV = quitDiag.findViewById(R.id.dialog_quit_title);
            titleV.setText(DataParse.getStr(this, "are_you_sure", Home.spf));
            TextView descV = quitDiag.findViewById(R.id.dialog_quit_desc);
            descV.setText(DataParse.getStr(this, "close_diag_desc", Home.spf));
            quitDiag.findViewById(R.id.dialog_quit_no).setOnClickListener(view -> quitDiag.dismiss());
            quitDiag.findViewById(R.id.dialog_quit_yes).setOnClickListener(view -> {
                quitDiag.dismiss();
                finish();
            });
        }
        quitDiag.show();
    }

    public void showLowBalDiag() {
        if (lowBalDiag == null) lowBalDiag = Misc.lowbalanceDiag(this, new Misc.yesNo() {
            @Override
            public void yes() {
                lowBalDiag.dismiss();
                Variables.setHash("show_offers", "1");
                finish();
            }

            @Override
            public void no() {
                lowBalDiag.dismiss();
                finish();
            }
        });
        lowBalDiag.show();
    }
}