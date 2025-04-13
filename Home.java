package org.mintsoft.mintly;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.squareup.picasso.Picasso;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetAuth;
import org.mintsoft.mintlib.GetNet;
import org.mintsoft.mintlib.GetURL;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.account.Login;
import org.mintsoft.mintly.account.Refs;
import org.mintsoft.mintly.account.Settings;
import org.mintsoft.mintly.chatsupp.Chat;
import org.mintsoft.mintly.frags.FragGifts;
import org.mintsoft.mintly.frags.FragInvite;
import org.mintsoft.mintly.frags.FragMain;
import org.mintsoft.mintly.frags.FragOffers;
import org.mintsoft.mintly.frags.FragProfile;
import org.mintsoft.mintly.frags.FragRanks;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.GlobalMsg;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.PopupNotif;
import org.mintsoft.mintly.helper.PushMsg;
import org.mintsoft.mintly.helper.Surf;
import org.mintsoft.mintly.helper.Variables;

import java.util.HashMap;

public class Home extends BaseAppCompat {
    private Toast exitToast;
    private Animation blink;
    public static long cTime;
    public Fragment[] fragments;
    public static Dialog loadingDiag;
    public static SharedPreferences spf;
    private int cussPos = -1, adLoading, intentType;
    private BottomNavigationView bottomNav;
    private TextView notifCountView, nameView, goChat;
    public static InterstitialAd interstitialAd;
    private boolean backClick, unityIntReady;
    public static int fab_iv = 10000, delay, requestCode, checkBalance;
    public static ActivityResultLauncher<Intent> activityForResult;
    public static String currency, balance, interstitialUnit, rewardedUnit, uName, gams;
    public static boolean isExternal, adMobInitialized, fab, checkNotif, sAdv, showInterstitial,
            confettiAds = true, canRedeem = true, tasks, chatDisabled, cbl;
    private DrawerLayout drawerLayout;
    private LinearLayout homeFrame;
    public ShapeableImageView avatarView;
    private Intent drawerIntent;
    private Toolbar toolbar;
    public TextView balView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.home);
        currency = "Coin";
        balance = "0";
        spf = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        balView = findViewById(R.id.drawer_balanceView);
        drawerLayout = findViewById(R.id.drawerLayout);
        homeFrame = findViewById(R.id.homeFrame);
        nameView = findViewById(R.id.drawer_nameView);
        avatarView = findViewById(R.id.drawer_avatarView);
        goChat = findViewById(R.id.drawer_go_chat);
        toolbar = findViewById(R.id.home_toolBar);
        setSupportActionBar(toolbar);
        TextView titleView = findViewById(R.id.home_titleView);
        titleView.setText(DataParse.getStr(this, "app_name", spf));
        Misc.setLogo(this, titleView);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                homeFrame.setTranslationX(slideOffset * drawerView.getWidth());
                drawerLayout.bringChildToFront(drawerView);
                drawerLayout.requestLayout();
                drawerLayout.setScrimColor(Color.TRANSPARENT);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                cTime = System.currentTimeMillis();
                long sT = spf.getLong("r_time", cTime);
                if (sT <= cTime) {
                    balView.setText("...");
                    checkBal();
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (drawerIntent != null) {
                    if (intentType == 1) {
                        requestCode = 96;
                        activityForResult.launch(drawerIntent);
                    } else {
                        startActivity(drawerIntent);
                    }
                    drawerIntent = null;
                }
            }
        };
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        notifCountView = findViewById(R.id.home_notif_count);
        String tsk = GetURL.getMiscData("tasks");
        tasks = tsk != null && tsk.equals("1");
        gams = GetURL.getMiscData("games");
        if (gams == null) gams = "";
        loadingDiag = Misc.loadingDiag(this);
        blink = AnimationUtils.loadAnimation(Home.this, R.anim.blink);
        isExternal = spf.getBoolean("ex_surf", false);
        currency = spf.getString("currency", "Coin");
        String gft = GetURL.getMiscData("redeem");
        canRedeem = gft == null || !gft.equals("0");
        fragments = new Fragment[]{new FragMain(), new FragOffers(), new FragGifts(),
                new FragRanks(), new FragProfile(), new FragInvite()};
        bottomNav = findViewById(R.id.home_bottomNav);
        bottomNav.getMenu().add(Menu.NONE, 0, Menu.NONE, DataParse.getStr(this, "nav_home", spf)).setIcon(R.drawable.ic_home);
        bottomNav.getMenu().add(Menu.NONE, 1, Menu.NONE, DataParse.getStr(this, "nav_offers", spf)).setIcon(R.drawable.ic_offer);
        if (canRedeem) {
            bottomNav.getMenu().add(Menu.NONE, 2, Menu.NONE, DataParse.getStr(this, "nav_gifts", spf)).setIcon(R.drawable.ic_gift);
        } else {
            bottomNav.getMenu().add(Menu.NONE, 5, Menu.NONE, DataParse.getStr(this, "nav_invite", spf)).setIcon(R.drawable.ic_person_add);
        }
        bottomNav.getMenu().add(Menu.NONE, 3, Menu.NONE, DataParse.getStr(this, "nav_ranks", spf)).setIcon(R.drawable.ic_ranking);
        bottomNav.getMenu().add(Menu.NONE, 4, Menu.NONE, DataParse.getStr(this, "nav_profile", spf)).setIcon(R.drawable.ic_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int iid = item.getItemId();
            if (iid < fragments.length) {
                changeFrag(iid);
                return true;
            } else {
                return false;
            }
        });
        bottomNav.setSelectedItemId(0);
        GetAuth.userinfo(this, new onResponse() {
            @Override
            public void onSuccessHashMap(HashMap<String, String> data) {
                delay = spf.getInt("interval", 10) * 1000;
                uName = data.get("name");
                nameView.setText(uName);
                Picasso.get().load(data.get("avatar")).placeholder(R.drawable.anim_loading).error(R.drawable.avatar).into(avatarView);
                checkNotifCount();
            }
        });
        findViewById(R.id.home_notifView).setOnClickListener(view -> {
            requestCode = 99;
            activityForResult.launch(new Intent(Home.this, PopupNotif.class));
        });
        activityForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    if (requestCode == 99) {
                        if (resultCode == 0) {
                            notifCountView.setVisibility(View.GONE);
                            notifCountView.clearAnimation();
                        } else if (resultCode > 9) {
                            notifCountView.setText("9+");
                            notifCountView.clearAnimation();
                            notifCountView.startAnimation(blink);
                        } else {
                            notifCountView.setText(String.valueOf(resultCode));
                            notifCountView.clearAnimation();
                            notifCountView.startAnimation(blink);
                        }
                    } else if (requestCode == 96) {
                        if (resultCode == 9) {
                            Variables.reset();
                            GetAuth.removeCred(this);
                            startActivity(new Intent(this, Login.class));
                            finish();
                        } else if (resultCode == 7) {
                            startActivity(new Intent(this, Splash.class));
                            finish();
                        } else if (resultCode == 6) {
                            goChat.setVisibility(View.GONE);
                        }
                    }
                });
        TextView dwHist = findViewById(R.id.drawer_go_hist);
        dwHist.setText(DataParse.getStr(this, "history", Home.spf));
        dwHist.setOnClickListener(view -> {
            intentType = 0;
            drawerIntent = new Intent(Home.this, History.class);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        TextView dwRef = findViewById(R.id.drawer_go_refs);
        if (Home.canRedeem) {
            dwRef.setText(DataParse.getStr(this, "invite", Home.spf));
            dwRef.setOnClickListener(view -> {
                intentType = 0;
                drawerIntent = new Intent(Home.this, Refs.class);
                drawerLayout.closeDrawer(GravityCompat.START);
            });
        } else {
            dwRef.setVisibility(View.GONE);
        }
        TextView dwSett = findViewById(R.id.drawer_go_settings);
        dwSett.setText(DataParse.getStr(this, "sett", Home.spf));
        dwSett.setOnClickListener(view -> {
            intentType = 1;
            drawerIntent = new Intent(Home.this, Settings.class);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        TextView dwFaq = findViewById(R.id.drawer_go_faq);
        dwFaq.setText(DataParse.getStr(this, "faq", Home.spf));
        dwFaq.setOnClickListener(view -> {
            intentType = 0;
            drawerIntent = new Intent(Home.this, Faq.class);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        goChat.setText(DataParse.getStr(this, "chat_room", spf));
        goChat.setOnClickListener(view -> {
            intentType = 1;
            drawerIntent = new Intent(Home.this, Chat.class);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        TextView dwCon = findViewById(R.id.drawer_go_contact);
        dwCon.setText(DataParse.getStr(this, "support", Home.spf));
        dwCon.setOnClickListener(view -> {
            intentType = 0;
            drawerIntent = new Intent(Home.this, Support.class);
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        TextView dwFol = findViewById(R.id.drawer_go_follow);
        String followUrl = GetURL.getMiscData("follow");
        if (followUrl == null || followUrl.isEmpty()) {
            dwFol.setVisibility(View.GONE);
        } else {
            dwFol.setText(DataParse.getStr(this, "follow_us", Home.spf));
            dwFol.setOnClickListener(v1 -> {
                Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(followUrl));
                Intent chooser = Intent.createChooser(sendIntent, DataParse.getStr(Home.this, "open_url_with", Home.spf));
                if (sendIntent.resolveActivity(getPackageManager()) == null) {
                    startActivity(new Intent(Home.this, Surf.class).putExtra("url", followUrl));
                } else {
                    startActivity(chooser);
                }
            });
        }
        TextView dwExit = findViewById(R.id.drawer_go_exit);
        dwExit.setText(DataParse.getStr(this, "close_app", Home.spf));
        dwExit.setOnClickListener(view -> finishAndRemoveTask());
        checkGlobalMsg();
        preload();
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onResume() {
        super.onResume();
        if (showInterstitial || spf.getBoolean("show_ad", false)) {
            showInterstitial = false;
            if (sAdv && unityIntReady) {
                spf.edit().putBoolean("show_ad", false).apply();
                showUnityAds();
            } else {
                if (interstitialAd != null) {
                    spf.edit().putBoolean("show_ad", false).apply();
                    interstitialAd.show(this);
                } else {
                    loadAd();
                }
            }
        }
        if (checkNotif) {
            checkNotif = false;
            checkNotifCount();
        }
        if (Variables.getHash("show_offers") != null) {
            Variables.setHash("show_offers", null);
            bottomNav.setSelectedItemId(1);
        }
        if (checkBalance == 1) {
            checkBalance = 0;
            checkBal();
        } else if (checkBalance == 2) {
            checkBalance = 0;
            balView.setText(balance);
        }
    }

    @Override
    public void onBackPressed() {
        if (backClick) {
            super.onBackPressed();
        } else {
            backClick = true;
            if (exitToast == null) {
                exitToast = Toast.makeText(this, DataParse.getStr(this, "double_back", spf), Toast.LENGTH_SHORT);
            }
            exitToast.show();
            new Handler().postDelayed(() -> backClick = false, 2000);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (exitToast != null) exitToast.cancel();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onDestroy() {
        spf.edit().remove("r_time").commit();
        if (loadingDiag != null && loadingDiag.isShowing()) loadingDiag.dismiss();
        super.onDestroy();
    }

    private void checkBal() {
        if (cbl) return;
        cbl = true;
        GetAuth.balance(Home.this, spf, new onResponse() {
            @Override
            public void onSuccessHashMap(HashMap<String, String> data1) {
                spf.edit().putLong("r_time", cTime + delay).commit();
                cbl = false;
                balance = data1.get("balance");
                balView.setText(balance);
                uName = data1.get("name");
                nameView.setText(uName);
            }

            @Override
            public void onSuccess(String success) {
                checkNotifCount();
            }

            @Override
            public void onError(int i, String s) {
                cbl = false;
            }
        });
    }

    private void checkNotifCount() {
        int msgCount = GetNet.messageCount(Home.this);
        if (msgCount == 0) {
            notifCountView.setVisibility(View.GONE);
            notifCountView.clearAnimation();
        } else {
            if (msgCount > 9) {
                notifCountView.setText("9+");
            } else {
                notifCountView.setText(String.valueOf(msgCount));
            }
            notifCountView.setVisibility(View.VISIBLE);
            notifCountView.clearAnimation();
            notifCountView.startAnimation(blink);

        }
    }

    private void checkGlobalMsg() {
        new Handler().postDelayed(() -> {
            if (spf.getStringSet("push_msg", null) != null) {
                startActivity(new Intent(Home.this, PushMsg.class));
            } else if (spf.getBoolean("g_msg", true)) {
                String title = spf.getString("g_title", "");
                if (!title.isEmpty()) {
                    Intent intent = new Intent(Home.this, GlobalMsg.class);
                    intent.putExtra("id", spf.getString("gmid", "none"));
                    intent.putExtra("title", title);
                    intent.putExtra("info", spf.getString("g_desc", "Empty message body."));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    startActivity(intent);
                }
            }
        }, 2000);
    }

    public void preload() {
        try {
            HashMap<String, String> unityData = GetNet.sdkInfo("infos_cpv",
                    "unityads", new String[]{"active", "game_id", "unit_id_i",
                            "unit_id_r", "fab", "fab_iv", "confetti"});
            if (unityData.containsKey("active")) {
                String active = unityData.get("active");
                sAdv = active != null && active.equals("yes");
            }
            if (sAdv) {
                String i_slot = unityData.get("unit_id_i");
                if (i_slot != null && !i_slot.isEmpty()) {
                    interstitialUnit = i_slot;
                }
                String r_slot = unityData.get("unit_id_r");
                if (r_slot != null && !r_slot.isEmpty()) {
                    rewardedUnit = r_slot;
                }
                String fa = unityData.get("fab");
                if (fa != null && fa.equals("yes")) fab = true;
                String fiv = unityData.get("fab_iv");
                if (fiv != null) fab_iv = Integer.parseInt(fiv) * 1000;
                UnityAds.setDebugMode(false);
                UnityAds.initialize(getApplicationContext(), unityData.get("game_id"),
                        false, new IUnityAdsInitializationListener() {
                            @Override
                            public void onInitializationComplete() {
                                loadUnityAds();
                            }

                            @Override
                            public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                                unityIntReady = false;
                            }
                        });
                String cf = unityData.get("confetti");
                if (cf != null && !cf.equals("yes")) {
                    confettiAds = false;
                }
            } else {
                HashMap<String, String> admobData = GetNet.sdkInfo("infos_cpv",
                        "admob", new String[]{"app_id", "interstitial_slot", "rewarded_slot",
                                "fab", "fab_iv", "confetti"});
                String app_id = admobData.get("app_id");
                if (app_id != null) {
                    ApplicationInfo ai = getPackageManager()
                            .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
                    ai.metaData.putString("com.google.android.gms.ads.APPLICATION_ID", app_id);
                }
                String i_slot = admobData.get("interstitial_slot");
                if (i_slot != null && !i_slot.isEmpty()) {
                    interstitialUnit = i_slot;
                }
                String r_slot = admobData.get("rewarded_slot");
                if (r_slot != null && !r_slot.isEmpty()) {
                    rewardedUnit = r_slot;
                }
                String fa = admobData.get("fab");
                if (fa != null && fa.equals("yes")) fab = true;
                MobileAds.initialize(getApplicationContext(), initializationStatus -> {
                    adMobInitialized = true;
                    loadAd();
                });
                String fiv = admobData.get("fab_iv");
                if (fiv != null) fab_iv = Integer.parseInt(fiv) * 1000;
                String cf = unityData.get("confetti");
                if (cf != null && !cf.equals("yes")) {
                    confettiAds = false;
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void loadAd() {
        if (adLoading == 1 || interstitialAd != null || interstitialUnit == null) return;
        adLoading = 1;
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(getApplicationContext(), interstitialUnit, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitial) {
                        adLoading = 0;
                        interstitialAd = interstitial;
                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        interstitialAd = null;
                                        loadAd();
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                        interstitialAd = null;
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        loadAd();
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        adLoading = 0;
                        interstitialAd = null;
                    }
                });
    }

    private void loadUnityAds() {
        UnityAds.load(interstitialUnit, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                unityIntReady = true;
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                unityIntReady = false;
            }
        });
    }

    private void showUnityAds() {
        UnityAds.show(this, interstitialUnit, new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                unityIntReady = false;
            }

            @Override
            public void onUnityAdsShowStart(String placementId) {

            }

            @Override
            public void onUnityAdsShowClick(String placementId) {

            }

            @Override
            public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
                loadUnityAds();
            }
        });
    }

    public void changeFrag(int pos) {
        if (cussPos == pos) return;
        cussPos = pos;
        if (pos == 1) {
            toolbar.setVisibility(View.GONE);
        } else {
            toolbar.setVisibility(View.VISIBLE);
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.home_frameLayout, fragments[pos], String.valueOf(pos));
        fragmentTransaction.commit();
    }

    public void changeTab(int pos) {
        bottomNav.setSelectedItemId(pos);
    }

    public void openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START);
    }
}