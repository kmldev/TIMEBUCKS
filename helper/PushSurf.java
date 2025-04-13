package org.mintsoft.mintly.helper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.squareup.picasso.Picasso;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;

import java.util.HashMap;
import java.util.Set;

public class PushSurf extends AppCompatActivity {
    private WebView webView;
    private TextView titleView;
    private Dialog askDiag;
    private String url1, mimeType, userAgent, contentDisposition, filename, url;
    private ProgressBar progressBar;
    private LinearLayout nHolder;
    private RelativeLayout notifHolder;
    private SharedPreferences spf;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
        } else {
            url = extras.getString("url", null);
            if (url == null) {
                finish();
            } else {
                setContentView(R.layout.push_surf);
                notifHolder = findViewById(R.id.push_surf_notif_holder);
                notifHolder.setVisibility(View.GONE);
                nHolder = findViewById(R.id.push_surf_notifHolder);
                nHolder.setVisibility(View.GONE);
                titleView = findViewById(R.id.surf_title);
                titleView.setText(DataParse.getStr(this, "webview_name", Home.spf));
                webView = findViewById(R.id.surf_webView);
                progressBar = findViewById(R.id.surf_progressBar);
                progressBar.setIndeterminate(false);
                progressBar.setMax(100);
                spf = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                findViewById(R.id.surf_close).setOnClickListener(view -> finish());
                initWebView(url);
                TextView extTitle = findViewById(R.id.push_surf_exTitle);
                extTitle.setText(DataParse.getStr(this, "external_browser", Home.spf));
                findViewById(R.id.push_surf_external).setOnClickListener(view -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    nHolder.setVisibility(View.GONE);
                });
                if (extras.getBoolean("only", false)) {
                    new Handler().postDelayed(() -> {
                        if (!isFinishing() && !isDestroyed()) nHolder.setVisibility(View.VISIBLE);
                    }, 2000);
                } else {
                    initNotif();
                }
            }
        }
    }

    private void initNotif() {
        try {
            Set<String> data = spf.getStringSet("push_msg", null);
            if (data == null) {
                new Handler().postDelayed(() -> {
                    if (!isFinishing() && !isFinishing()) nHolder.setVisibility(View.VISIBLE);
                }, 2000);
            } else {
                ImageView imageView = findViewById(R.id.push_surf_notif_imageView);
                TextView titleView = findViewById(R.id.push_surf_notif_titleView);
                TextView descView = findViewById(R.id.push_surf_notif_descView);
                Object[] objects = data.toArray();
                HashMap<String, String> hashMap = new HashMap<>();
                for (Object o : objects) {
                    if (o.toString().contains("title###")) {
                        hashMap.put("title", o.toString().replace("title###", ""));
                    } else if (o.toString().contains("desc###")) {
                        hashMap.put("desc", o.toString().replace("desc###", ""));
                    } else if (o.toString().contains("image###")) {
                        hashMap.put("image", o.toString().replace("image###", ""));
                    }
                }
                if (hashMap.containsKey("title")) {
                    titleView.setText(hashMap.get("title"));
                }
                if (hashMap.containsKey("desc")) {
                    descView.setText(hashMap.get("desc"));
                }
                if (hashMap.containsKey("image")) {
                    Picasso.get().load(hashMap.get("image")).placeholder(R.drawable.anim_loading).error(R.color.gray).into(imageView);
                } else {
                    imageView.setVisibility(View.GONE);
                }
                findViewById(R.id.push_surf_notif_close).setOnClickListener(view -> {
                    notifHolder.setVisibility(View.GONE);
                    new Handler().postDelayed(() -> {
                        if (!isFinishing() && !isFinishing()) nHolder.setVisibility(View.VISIBLE);
                    }, 2000);
                });
                spf.edit().remove("push_msg").apply();
                notifHolder.setVisibility(View.VISIBLE);
            }
        } catch (Exception ignored) {
            new Handler().postDelayed(() -> {
                if (!isFinishing() && !isFinishing()) nHolder.setVisibility(View.VISIBLE);
            }, 2000);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 131) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showAskDiag();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
        } else {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (askDiag != null && askDiag.isShowing()) askDiag.dismiss();
        WebStorage.getInstance().deleteAllData();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        webView.clearCache(true);
        webView.clearFormData();
        webView.clearHistory();
        webView.clearSslPreferences();
        super.onDestroy();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView(String url) {
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (progress < 100 && progressBar.getVisibility() == ProgressBar.GONE) {
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                }
                progressBar.setProgress(progress);
                if (progress == 100) {
                    progressBar.setVisibility(ProgressBar.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (!TextUtils.isEmpty(title)) {
                    titleView.setText(title);
                }
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                boolean playStore = url.contains("//play.google.com/store/apps/");
                if (playStore || url.startsWith("market://")) {
                    if (playStore) {
                        url = "https://play.google.com/store/apps/" + url.split("//play.google.com/store/apps/")[1];
                    }
                    try {
                        Intent i = new Intent("android.intent.action.VIEW");
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                        return true;
                    } catch (Exception e) {
                        view.loadUrl(url);
                        return false;
                    }
                }
                view.loadUrl(url);
                return false;
            }
        });
        webView.setDownloadListener((url1_, userAgent_, contentDisposition_, mimeType_, contentLength_) -> {
            url1 = url1_;
            userAgent = userAgent_;
            mimeType = mimeType_;
            contentDisposition = contentDisposition_;
            filename = URLUtil.guessFileName(url1, contentDisposition, mimeType);
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if (ActivityCompat.checkSelfPermission(PushSurf.this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, 131);
            } else {
                showAskDiag();
            }
        });
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webView.loadUrl(url);
    }

    private void showAskDiag() {
        if (askDiag == null) {
            askDiag = Misc.decoratedDiag(this, R.layout.dialog_quit, 0.8f);
            TextView titleV = askDiag.findViewById(R.id.dialog_quit_title);
            titleV.setText(DataParse.getStr(this, "are_you_sure", Home.spf));
            TextView desc = askDiag.findViewById(R.id.dialog_quit_desc);
            desc.setText("You are about to download a file with name \"" + filename + "\". Are you sure you want to download it?");
            askDiag.findViewById(R.id.dialog_quit_no).setOnClickListener(view -> askDiag.dismiss());
            askDiag.findViewById(R.id.dialog_quit_yes).setOnClickListener(view -> {
                askDiag.dismiss();
                dlFile();
            });
        }
        askDiag.show();
    }

    private void dlFile() {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url1));
        request.setMimeType(mimeType);
        String cookies = CookieManager.getInstance().getCookie(url1);
        request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", userAgent);
        request.setDescription("Downloading file...");
        request.setTitle(filename);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        dm.enqueue(request);
        Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
    }
}