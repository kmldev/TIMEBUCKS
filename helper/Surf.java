package org.mintsoft.mintly.helper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;

import java.util.HashMap;

public class Surf extends BaseActivity {
    private WebView webView;
    private TextView titleView;
    private Dialog askDiag;
    private String cred, url1, mimeType, userAgent, contentDisposition, filename;
    private ProgressBar progressBar;
    private ImageView backwardView, forwardView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
        } else {
            String url = extras.getString("url", null);
            if (url == null) {
                finish();
            } else {
                cred = extras.getString("cred", "");
                if (cred.equals("alt")) {
                    Uri uri = Uri.parse(url);
                    CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
                    CustomTabColorSchemeParams params = new CustomTabColorSchemeParams.Builder()
                            .setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                            .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                            .setSecondaryToolbarColor(ContextCompat.getColor(this, R.color.colorPrimaryLight))
                            .build();
                    intentBuilder.setDefaultColorSchemeParams(params);
                    //intentBuilder.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, params);
                    intentBuilder.setCloseButtonIcon(bitmapFromDrawable(this, R.drawable.ic_arrow));
                    intentBuilder.setStartAnimations(this, android.R.anim.fade_in, android.R.anim.fade_out);
                    intentBuilder.setExitAnimations(this, android.R.anim.fade_in, android.R.anim.fade_out);
                    try {
                        CustomTabsIntent customTabsIntent = intentBuilder.build();
                        customTabsIntent.intent.setPackage("com.android.chrome");
                        customTabsIntent.launchUrl(this, uri);
                    } catch (Exception e) {
                        CustomTabsIntent customTabsIntent = intentBuilder.build();
                        customTabsIntent.launchUrl(this, uri);
                    }
                    finish();
                } else {
                    setContentView(R.layout.surf);
                    if (extras.getBoolean("fullscreen", false)) {
                        findViewById(R.id.surf_full_scr_1).setVisibility(View.GONE);
                        findViewById(R.id.surf_full_scr_2).setVisibility(View.GONE);
                    }
                    TextView titleView2 = findViewById(R.id.surf_title2);
                    titleView2.setText(DataParse.getStr(this,"webview_name", Home.spf));
                    titleView = findViewById(R.id.surf_title);
                    webView = findViewById(R.id.surf_webView);
                    backwardView = findViewById(R.id.surf_backward);
                    forwardView = findViewById(R.id.surf_forward);
                    progressBar = findViewById(R.id.surf_progressBar);
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(100);
                    initWebView(url);
                    findViewById(R.id.surf_close).setOnClickListener(view -> finish());
                    backwardView.setOnClickListener(view -> {
                        if (webView.canGoBack()) {
                            webView.goBack();
                        }
                    });
                    forwardView.setOnClickListener(view -> {
                        if (webView.canGoForward()) {
                            webView.goForward();
                        }
                    });
                }
            }
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
    protected void onDestroy() {
        if (!cred.equals("alt")) {
            if (askDiag != null && askDiag.isShowing()) askDiag.dismiss();
            WebStorage.getInstance().deleteAllData();
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
            webView.clearCache(true);
            webView.clearFormData();
            webView.clearHistory();
            webView.clearSslPreferences();
        }
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

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                if (webView.canGoBack()) {
                    backwardView.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
                } else {
                    backwardView.setColorFilter(Color.argb(150, 0, 0, 0), android.graphics.PorterDuff.Mode.SRC_IN);
                }
                if (webView.canGoForward()) {
                    forwardView.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);
                } else {
                    forwardView.setColorFilter(Color.argb(150, 0, 0, 0), android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }
        });
        webView.setDownloadListener((url1_, userAgent_, contentDisposition_, mimeType_, contentLength_) -> {
            url1 = url1_;
            userAgent = userAgent_;
            mimeType = mimeType_;
            contentDisposition = contentDisposition_;
            filename = URLUtil.guessFileName(url1, contentDisposition, mimeType);
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if (ActivityCompat.checkSelfPermission(Surf.this, permission)
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
        if (cred.isEmpty()) {
            try {
                String[] creds = cred.split(":");
                HashMap<String, String> headerMap = new HashMap<>();
                headerMap.put(creds[0], creds[1]);
                webView.loadUrl(url, headerMap);
            } catch (Exception e) {
                webView.loadUrl(url);
            }
        } else {
            webView.loadUrl(url);
        }
    }

    private void showAskDiag() {
        if (askDiag == null) {
            askDiag = Misc.decoratedDiag(this, R.layout.dialog_quit, 0.8f);
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

    private static Bitmap bitmapFromDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof VectorDrawable) {
            return bitmapFromVectorDrawable((VectorDrawable) drawable);
        }
        return ((BitmapDrawable) drawable).getBitmap();
    }

    private static Bitmap bitmapFromVectorDrawable(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }
}