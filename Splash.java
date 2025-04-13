package org.mintsoft.mintly;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.GetURL;
import org.mintsoft.mintlib.onResponse;
import org.mintsoft.mintly.account.Login;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

import java.util.HashMap;

public class Splash extends AppCompatActivity {
    private SharedPreferences spf;
    private String cc, irc, dta;
    private Dialog dialog;
    private boolean isSuccess;
    private TextView textView;
    private ActivityResultLauncher<String> activityForResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        textView = findViewById(R.id.splash_text);
        Misc.setLogo(this, textView);
        spf = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        cc = spf.getString("cc", null);
        Variables.reset();
        irc = spf.getString("ir_c", null);
        if (irc == null) {
            InstallReferrerClient client = InstallReferrerClient.newBuilder(this).build();
            client.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        try {
                            ReferrerDetails response = client.getInstallReferrer();
                            irc = response.getInstallReferrer();
                            spf.edit().putString("ir_c", irc).apply();
                            client.endConnection();
                            String irf = Misc.getR(irc);
                            if (irf != null) spf.edit().putString("rfb", irf).apply();
                        } catch (RemoteException ignored) {
                        }
                    }
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                }
            });
        }
        try {
            FirebaseApp.initializeApp(getApplicationContext());
            FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
            firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());
        } catch (Exception ignored) {
        }
        activityForResult = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if (Build.VERSION.SDK_INT >= 33) {
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                    showPrDiag();
                } else {
                    if (result) spf.edit().putBoolean("nfprm", true).apply();
                    beginProcess();
                }
            } else {
                beginProcess();
            }
        });
        beginProcess();
    }

    @Override
    protected void onDestroy() {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        super.onDestroy();
    }

    private void beginProcess() {
        AsyncTask.execute(() -> {
            try {
                AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                String adId = adInfo.getId();
                spf.edit().putString("gid", adId).apply();
            } catch (Exception ignored) {
            }
        });
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            cc = tm.getSimCountryIso();
            if (cc == null || cc.length() != 2) {
                getCC();
            } else {
                call();
            }
        } catch (Exception e) {
            if (cc == null || cc.length() != 2) getCC();
        }
    }

    private void getCC() {
        GetURL.getCc(this, getString(R.string.domain_name), new onResponse() {
            @Override
            public void onSuccess(String response) {
                if (response != null && response.length() == 2) cc = response;
                call();
            }

            @Override
            public void onError(int errorCode, String error) {
                call();
            }
        });
    }

    private void call() {
        if (cc == null || cc.length() != 2) {
            cc = "us";
        } else {
            cc = cc.toLowerCase();
        }
        spf.edit().putString("cc", cc).apply();
        new Handler().postDelayed(() -> GetURL.app_java(Splash.this,
                "https://" + getString(R.string.domain_name), getInfo(), spf, cc, new onResponse() {
                    @Override
                    public void onSuccess(String response) {
                        isSuccess = true;
                        dta = response;
                        chooseLocale();
                    }

                    @SuppressLint("ApplySharedPref")
                    @Override
                    public void onError(int errorCode, String error) {
                        if (errorCode == -9) {
                            dialog = Misc.noConnection(dialog, Splash.this, () -> {
                                call();
                                dialog.dismiss();
                            });
                            if (error.startsWith("Er:")) {
                                TextView dVw = dialog.findViewById(R.id.dialog_connection_desc);
                                dVw.setText(error.replace("Er:", ""));
                                dVw.setTextColor(Color.YELLOW);
                            }
                        } else if (errorCode == -11) {
                            if (spf.getBoolean("dtld", false)) {
                                spf.edit().remove("app_locale").commit();
                                dialog = Misc.noConnection(dialog, Splash.this, () -> {
                                    chooseLocale();
                                    dialog.dismiss();
                                });
                            } else {
                                Misc.lockedDiag(Splash.this, error, "");
                            }
                        } else if (errorCode == -10) {
                            Misc.lockedDiag(Splash.this, error, DataParse.getStr(Splash.this, "unsupported_device_desc", spf));
                        } else if (errorCode == -2) {
                            Misc.lockedDiag(Splash.this, DataParse.getStr(Splash.this, "user_banned", spf), error);
                        } else if (errorCode == -3) {
                            String[] err = error.split(",,");
                            Misc.lockedDiag(Splash.this, err[0], err[1]);
                        } else if (errorCode == -1) {
                            isSuccess = false;
                            dta = error;
                            chooseLocale();
                        } else {
                            Toast.makeText(Splash.this, error, Toast.LENGTH_LONG).show();
                            finish();
                        }

                    }
                }), 1000);
    }

    public HashMap<String, String> getInfo() {
        HashMap<String, String> data = new HashMap<>();
        data.put("cc", spf.getString("cc", cc));
        return data;
    }

    private void chooseLocale() {
        if (getResources().getBoolean(R.bool.show_translation_dialog)) {
            String lc = spf.getString("app_locale", null);
            if (lc == null) {
                Misc.chooseLocale(this, spf, new Misc.yesNo() {
                    @Override
                    public void yes() {
                        Intent mIntent = new Intent(Splash.this, Splash.class);
                        finish();
                        startActivity(mIntent);
                    }

                    @Override
                    public void no() {
                        startProcess();
                    }
                });
            } else {
                String app_name = DataParse.getStr(this, "app_name", spf);
                textView.setText(app_name.isEmpty() ? DataParse.getStr(this, "app_name", spf) : app_name);
                Misc.setLogo(this, textView);
                startProcess();
            }
        } else {
            startProcess();
        }
    }

    private void startProcess() {
        if (!spf.getBoolean("nfprm", false) && Build.VERSION.SDK_INT >= 33 && ActivityCompat
                .checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            showPrDiag();
        } else {
            postRedirect();
        }
    }

    private void showPrDiag() {
        final Dialog permDiag = Misc.decoratedDiag(this, R.layout.dialog_perm, 1f);
        int width = (int) (getResources().getDisplayMetrics().widthPixels);
        int height = (int) (getResources().getDisplayMetrics().heightPixels);
        Window w = permDiag.getWindow();
        if (w != null) w.setLayout(width, height);
        TextView permT = permDiag.findViewById(R.id.dialog_permT);
        permT.setText(DataParse.getStr(this, "permission_required", spf));
        TextView permD = permDiag.findViewById(R.id.dialog_permD);
        permD.setText(DataParse.getStr(this, "permission_notif_desc", spf));
        TextView allow = permDiag.findViewById(R.id.dialog_perm_allow);
        allow.setText(DataParse.getStr(this, "allow", spf));
        allow.setOnClickListener(view -> {
            permDiag.dismiss();
            if (Build.VERSION.SDK_INT >= 33) {
                activityForResult.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            } else {
                postRedirect();
            }
        });
        TextView nope = permDiag.findViewById(R.id.dialog_perm_no);
        nope.setText(DataParse.getStr(this, "nop", spf));
        nope.findViewById(R.id.dialog_perm_no).setOnClickListener(view -> {
            spf.edit().putBoolean("nfprm", true).apply();
            permDiag.dismiss();
            beginProcess();
        });
        permDiag.show();
    }


    private void postRedirect() {
        String[] data = dta.split(",");
        Variables.isLive = true;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            Variables.setPHash("ver_n", pInfo.versionName);
            Variables.setPHash("ver_c", String.valueOf(pInfo.versionCode));
            boolean isDebuggable = 0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE);
            Variables.setPHash("debug", isDebuggable ? "1" : "0");
            if (pInfo.versionCode < Integer.parseInt(data[0])) {
                if (data[1].equals("1")) {
                    Misc.lockedDiag(Splash.this, DataParse.getStr(Splash.this, "outdated_version", spf),
                            DataParse.getStr(Splash.this, "outdated_version_desc", spf));
                } else {
                    showQuitDiag(isSuccess);
                }
            } else {
                if (isSuccess) {
                    if (spf.getBoolean("tos", false)) {
                        startActivity(new Intent(this, Home.class));
                    } else {
                        startActivity(new Intent(this, Tos.class));
                    }
                } else {
                    startActivity(new Intent(Splash.this, Login.class));
                }
                finish();
            }
        } catch (Exception e) {
            Variables.setPHash("ver_n", "1.0");
            Variables.setPHash("ver_c", String.valueOf(1));
            boolean isDebuggable = 0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE);
            Variables.setPHash("debug", isDebuggable ? "1" : "0");
            if (isSuccess) {
                if (spf.getBoolean("tos", false)) {
                    startActivity(new Intent(this, Home.class));
                } else {
                    startActivity(new Intent(this, Tos.class));
                }
            } else {
                startActivity(new Intent(Splash.this, Login.class));
            }
            finish();
        }
    }

    private void showQuitDiag(boolean isSuccess) {
        Dialog lokDiag = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        View lowBalView = LayoutInflater.from(this).inflate(R.layout.dialog_connection, null);
        lokDiag.setContentView(lowBalView);
        lokDiag.setCancelable(false);
        Window w = lokDiag.getWindow();
        if (w != null) {
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
            w.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }
        ImageView lokImgView = lowBalView.findViewById(R.id.dialog_connection_img);
        lokImgView.setImageResource(R.drawable.ic_warning);
        TextView lokTitleView = lowBalView.findViewById(R.id.dialog_connection_title);
        lokTitleView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        lokTitleView.setText(DataParse.getStr(this, "outdated_version", spf));
        TextView lokDescView = lowBalView.findViewById(R.id.dialog_connection_desc);
        lokDescView.setText(DataParse.getStr(this, "outdated_version_desc", spf));
        Button pS = lowBalView.findViewById(R.id.dialog_connection_retry);
        Button close = lowBalView.findViewById(R.id.dialog_connection_exit);
        close.setText(DataParse.getStr(Splash.this, "continu", spf));
        pS.setText(DataParse.getStr(this, "go_to_ps", spf));
        close.setOnClickListener(v -> {
            lokDiag.dismiss();
            if (isSuccess) {
                if (spf.getBoolean("tos", false)) {
                    startActivity(new Intent(this, Home.class));
                } else {
                    startActivity(new Intent(this, Tos.class));
                }
            } else {
                startActivity(new Intent(Splash.this, Login.class));
            }
            finish();
        });
        pS.setOnClickListener(v -> {
            String appPackageName = getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        });
        lokDiag.show();
    }
}