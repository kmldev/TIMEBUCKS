package org.mintsoft.mintly.offers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.PlayerMetaData;

import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintlib.GetNet;
import org.mintsoft.mintlib.Offerwalls;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;

import java.util.HashMap;

public class GlobalAds {
    private static RewardedAd rewardedAd;
    private static String uid;
    private static Handler handler1;
    private static FloatingActionButton fab;

    public static void fab(Activity context, String unitName) {
        if (Home.rewardedUnit == null || !Home.fab) return;
        Offerwalls.getStat(context, Home.sAdv ? "unityads" : "admob", true, new onResponse() {
            @Override
            public void onSuccess(String s) {
                if (context.isFinishing() || context.isDestroyed()) return;
                if (s.equals("1")) context.runOnUiThread(() -> forward(context, unitName));
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private static void forward(Activity context, String unitName) {
        String name;
        if (Home.sAdv) {
            HashMap<String, String> unityData = GetNet.sdkInfo("infos_cpv",
                    "unityads", new String[]{unitName, "fab", "fav_iv"});
            name = unityData.get(unitName);
        } else {
            HashMap<String, String> admobData = GetNet.sdkInfo("infos_cpv",
                    "admob", new String[]{unitName, "fab", "fav_iv"});
            name = admobData.get(unitName);
        }
        if (name == null || !name.equals("yes")) return;
        fab = new FloatingActionButton(context);
        View views = context.findViewById(android.R.id.content);
        ViewGroup rootView = (ViewGroup) ((ViewGroup) views).getChildAt(0);
        rootView.addView(fab);
        fab.post(() -> fab.setVisibility(View.GONE));
        try {
            fab.setImageResource(R.drawable.anim_offer);
            fab.setCustomSize(155);
            fab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1171a5")));
            fab.setScaleType(ImageView.ScaleType.CENTER);
            fab.post(() -> {
                AnimationDrawable frameAnimation = (AnimationDrawable) fab.getDrawable();
                frameAnimation.start();
            });
            views.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            String posStr = Home.spf.getString("fab_pos", null);
                            float x = views.getWidth() - fab.getWidth() - views.getWidth() / 15f;
                            float y = views.getHeight() - fab.getHeight() - views.getHeight() / 20f;
                            if (posStr == null) {
                                fab.setX(x);
                                fab.setY(y);
                                Home.spf.edit().putString("fab_pos", x + "," + y).apply();
                                views.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } else {
                                boolean updated = false;
                                String[] pos = posStr.split(",");
                                float sX = Float.parseFloat(pos[0]);
                                float sY = Float.parseFloat(pos[1]);
                                if (sX > x) {
                                    sX = x;
                                    updated = true;
                                } else if (sX < 20) {
                                    sX = 30;
                                    updated = true;
                                }
                                if (sY > y) {
                                    sY = y;
                                    updated = true;
                                } else if (sY < 20) {
                                    sY = 30;
                                    updated = true;
                                }
                                fab.setX(sX);
                                fab.setY(sY);
                                if (updated) {
                                    Home.spf.edit().putString("fab_pos", sX + "," + sY).apply();
                                }
                            }
                            views.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    });
            fab.setOnLongClickListener(v -> {
                v.setOnTouchListener((view, event) -> {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_MOVE:
                            view.setX(event.getRawX() - 60);
                            view.setY(event.getRawY() - 100);
                            break;
                        case MotionEvent.ACTION_UP:
                            view.setOnTouchListener(null);
                            Home.spf.edit().putString("fab_pos", (event.getRawX() - 60)
                                    + "," + (event.getRawY() - 100)).apply();
                            break;
                        default:
                            break;
                    }
                    return true;
                });
                return true;
            });
            uid = GetAuth.user(context);
            if (Home.sAdv) {
                handler1 = new Handler();
                handler1.postDelayed(runnable1, 5000);
            } else if (rewardedAd == null) {
                load(context, fab);
            } else {
                fab.post(() -> fab.setVisibility(View.VISIBLE));
            }
            fab.setOnClickListener(view -> {
                if (Home.sAdv) {
                    context.runOnUiThread(() -> {
                        PlayerMetaData playerMetaData = new PlayerMetaData(context);
                        playerMetaData.setServerId(GetAuth.user(context));
                        playerMetaData.commit();
                        UnityAds.show(context, Home.rewardedUnit, new IUnityAdsShowListener() {
                            @Override
                            public void onUnityAdsShowFailure(String s, UnityAds.UnityAdsShowError unityAdsShowError, String s1) {
                            }

                            @Override
                            public void onUnityAdsShowStart(String s) {
                            }

                            @Override
                            public void onUnityAdsShowClick(String s) {
                            }

                            @Override
                            public void onUnityAdsShowComplete(String s, UnityAds.UnityAdsShowCompletionState state) {
                                handler1.postDelayed(runnable1, Home.fab_iv);
                                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                                    Home.checkBalance = 1;
                                    Offerwalls.getStat(context.getApplicationContext(), "unityads", false, null);
                                }
                            }
                        });
                    });

                } else if (rewardedAd != null) {
                    rewardedAd.show(context, rewardItem ->
                            Offerwalls.getStat(context.getApplicationContext(), "admob", false, null));
                    rewardedAd = null;
                }
                fab.setVisibility(View.GONE);
            });
        } catch (Exception e) {
            fab.setVisibility(View.GONE);
        }
    }

    private static void load(Activity context, FloatingActionButton fab) {
        if (rewardedAd != null) return;
        context.runOnUiThread(() -> {
            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedAd.load(context.getApplicationContext(), Home.rewardedUnit,
                    adRequest, new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            fab.setVisibility(View.GONE);
                            Toast.makeText(context, "" + loadAdError.getMessage(), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd ad) {
                            ad.setServerSideVerificationOptions(new ServerSideVerificationOptions.Builder().setUserId(uid).build());
                            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    rewardedAd = null;
                                    fab.setVisibility(View.GONE);
                                    new Handler().postDelayed(() -> load(context, fab), Home.fab_iv);
                                }
                            });
                            rewardedAd = ad;
                            fab.setVisibility(View.VISIBLE);
                        }
                    });
        });
    }

    private static final Runnable runnable1 = new Runnable() {
        @Override
        public void run() {
            UnityAds.load(Home.rewardedUnit, new IUnityAdsLoadListener() {
                @Override
                public void onUnityAdsAdLoaded(String placementId) {
                    if (handler1 != null) handler1.removeCallbacks(runnable1);
                    if (fab != null) fab.setVisibility(View.VISIBLE);
                }

                @Override
                public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                    if (handler1 != null) handler1.postDelayed(runnable1, 3000);
                }
            });
        }
    };

    public static void hLoad(Activity activity) {
        fab(activity, "fab_hg");
    }
}