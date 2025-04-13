package org.mintsoft.mintly.helper;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintlib.HtmlGame;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class Misc {

    public interface yesNo {
        void yes();

        void no();
    }

    public interface htmlYN {
        void yes(Dialog d);

        void no(Dialog d);
    }

    public interface resp {
        void clicked();
    }

    public static void setLogo(Context context, TextView textView) {
        if (textView.getText().toString().equals("Mintly")) {
            Spannable word1 = new SpannableString("Mint");
            word1.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.green_1)), 0, word1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            word1.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, word1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(word1);
            Spannable word2 = new SpannableString("ly");
            word2.setSpan(new ForegroundColorSpan(Color.WHITE), 0, word2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.append(word2);
        }
    }

    public static Spanned html(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text.replace("\\n", "<br />"), Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(text.replace("\\n", "<br />"));
        }
    }

    public static void listAnimate(final LinearLayoutManager llm, final RecyclerView recList) {
        new Handler().postDelayed(() -> {
            int start = llm.findFirstVisibleItemPosition();
            int end = llm.findLastVisibleItemPosition();
            int DELAY = 50;
            RecyclerView.ViewHolder vw;
            for (int i = start; i <= end; i++) {
                vw = recList.findViewHolderForAdapterPosition(i);
                if (vw == null) return;
                View v = vw.itemView;
                v.setAlpha(0);
                PropertyValuesHolder slide = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 150, 0);
                PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0, 1);
                ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v, slide, alpha);
                a.setDuration(600);
                a.setStartDelay((long) i * DELAY);
                a.setInterpolator(new DecelerateInterpolator());
                a.start();
            }
            recList.setAlpha(1);

        }, 50);
    }

    public static void onenUrl(Context context, String url) {
        if (Home.isExternal) {
            Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            Intent chooser = Intent.createChooser(sendIntent, DataParse.getStr(context, "open_url_with", Home.spf));
            if (sendIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(chooser);
            } else {
                context.startActivity(new Intent(context, Surf.class).putExtra("url", url));
            }
        } else {
            context.startActivity(new Intent(context, Surf.class).putExtra("url", url));
        }
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, String> convertToHashMap(Intent intent, String key) {
        try {
            return (HashMap<String, String>) intent.getSerializableExtra(key);
        } catch (Exception e) {
            return null;
        }
    }

    public static Dialog decoratedDiag(Context context, int layout, float opacity) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        View view = LayoutInflater.from(context).inflate(layout, null);
        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        Window w = dialog.getWindow();
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.dimAmount = opacity;
        lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        w.setAttributes(lp);
        w.setGravity(Gravity.CENTER);
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
        w.setNavigationBarColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
        return dialog;
    }

    public static Animation alphaAnim() {
        Animation anim = new AlphaAnimation(0.5f, 1.0f);
        anim.setDuration(800);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        return anim;
    }

    public static Dialog loadingDiag(Context context) {
        Dialog loadingDialog = Misc.decoratedDiag(context, R.layout.dialog_loading, 0.8f);
        loadingDialog.setCancelable(false);
        TextView textView = loadingDialog.findViewById(R.id.dialog_loading_text);
        textView.setText(DataParse.getStr(context, "please_wait", Home.spf));
        ImageView imageView = loadingDialog.findViewById(R.id.dialog_loading_imageView);
        AnimationDrawable anim = (AnimationDrawable) imageView.getDrawable();
        anim.start();
        return loadingDialog;
    }

    public static Dialog loadingDiagExit(Activity activity) {
        Dialog loadingDialog = Misc.decoratedDiag(activity, R.layout.dialog_loading_exit, 0.8f);
        loadingDialog.setCancelable(false);
        ImageView imageView = loadingDialog.findViewById(R.id.dialog_loading_exit_imageView);
        AnimationDrawable anim = (AnimationDrawable) imageView.getDrawable();
        anim.start();
        TextView textView = loadingDialog.findViewById(R.id.dialog_loading_exit_text);
        textView.setText(DataParse.getStr(activity, "please_wait", Home.spf));
        Button button = loadingDialog.findViewById(R.id.dialog_loading_exit_close);
        button.setText(DataParse.getStr(activity, "cancl", Home.spf));
        button.setOnClickListener(v -> {
            loadingDialog.dismiss();
            activity.finish();
        });
        return loadingDialog;
    }

    public static Dialog lowbalanceDiag(Activity activity, yesNo yn) {
        String credit = " " + Home.currency.toLowerCase() + "s";
        Dialog lowbalDiag = Misc.decoratedDiag(activity, R.layout.dialog_quit, 0.8f);
        lowbalDiag.setCancelable(false);
        TextView lowbalTitle = lowbalDiag.findViewById(R.id.dialog_quit_title);
        TextView lowbalDesc = lowbalDiag.findViewById(R.id.dialog_quit_desc);
        TextView quitNo = lowbalDiag.findViewById(R.id.dialog_quit_no);
        TextView quitYes = lowbalDiag.findViewById(R.id.dialog_quit_yes);
        quitYes.setText(DataParse.getStr(activity, "yes", Home.spf));
        lowbalTitle.setText((DataParse.getStr(activity, "low_bal_title", Home.spf) + credit + "!"));
        lowbalDesc.setText((DataParse.getStr(activity, "low_bal_desc_prefix", Home.spf) + credit + ". " + DataParse.getStr(activity, "low_bal_desc_suffix", Home.spf) + credit + "?"));
        quitNo.setText((DataParse.getStr(activity, "earn", Home.spf) + credit));
        quitYes.setText(DataParse.getStr(activity, "quit", Home.spf));
        quitNo.setOnClickListener(view -> {
            yn.yes();
        });
        quitYes.setOnClickListener(view -> {
            yn.no();
        });
        return lowbalDiag;
    }

    private static Dialog diag;

    public static Dialog noConnection(Dialog conDiag, Context activity, resp r) {
        diag = conDiag;
        if (diag == null) {
            diag = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
            View views = LayoutInflater.from(activity).inflate(R.layout.dialog_connection, null);
            diag.setContentView(views);
            diag.setCancelable(false);
            Window w = diag.getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setNavigationBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
            w.setStatusBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
            TextView noConTitle = views.findViewById(R.id.dialog_connection_title);
            TextView noConDesc = views.findViewById(R.id.dialog_connection_desc);
            Button exitBtn = views.findViewById(R.id.dialog_connection_exit);
            Button retryBtn = views.findViewById(R.id.dialog_connection_retry);
            SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
            if (spf.getBoolean("dtld", false) && spf.getString("app_locale", null) != null) {
                noConTitle.setText(DataParse.getStr(activity, "connection_error", null));
                noConDesc.setText(DataParse.getStr(activity, "connection_error_desc", null));
                exitBtn.setText(DataParse.getStr(activity, "exit_window", null));
                retryBtn.setText(DataParse.getStr(activity, "retry", null));
            } else {
                noConTitle.setText("Connection error!");
                noConDesc.setText("Could not connect to the server. Click on retry button to try again.");
                exitBtn.setText("Exit the process");
                retryBtn.setText("Retry");
            }
            exitBtn.setOnClickListener(view -> {
                diag.dismiss();
                ((Activity) activity).finish();
            });
            retryBtn.setOnClickListener(view -> r.clicked());
        }
        try {
            diag.show();
        } catch (Exception ignored) {

        }
        return diag;
    }

    public static void lockedDiag(Activity activity, String title, String desc) {
        Dialog lokDiag = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        View lowBalView = LayoutInflater.from(activity).inflate(R.layout.dialog_connection, null);
        lokDiag.setContentView(lowBalView);
        lokDiag.setCancelable(false);
        Window w = lokDiag.getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setNavigationBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
        w.setStatusBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
        ImageView lokImgView = lowBalView.findViewById(R.id.dialog_connection_img);
        lokImgView.setImageResource(R.drawable.ic_warning);
        TextView lokTitleView = lowBalView.findViewById(R.id.dialog_connection_title);
        lokTitleView.setTextColor(ContextCompat.getColor(activity, android.R.color.holo_red_light));
        lokTitleView.setText(title);
        TextView lokDescView = lowBalView.findViewById(R.id.dialog_connection_desc);
        lokDescView.setText(desc);
        Button exitBtn = lowBalView.findViewById(R.id.dialog_connection_exit);
        Button retryBtn = lowBalView.findViewById(R.id.dialog_connection_retry);
        retryBtn.setVisibility(View.GONE);
        SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        if (spf.getBoolean("dtld", false) && spf.getString("app_locale", null) != null) {
            exitBtn.setText(DataParse.getStr(activity, "exit_window", null));
            retryBtn.setText(DataParse.getStr(activity, "retry", null));
        } else {
            exitBtn.setText("Exit the process");
            retryBtn.setText("Retry");
        }
        exitBtn.setOnClickListener(view -> {
            lokDiag.dismiss();
            activity.finish();
        });
        lokDiag.show();
    }

    public static Animation btnEffect() {
        Animation anim = new ScaleAnimation(1f, 0.9f, 1f, 0.9f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setRepeatCount(1);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setDuration(80);
        return anim;
    }

    public static void showMessage(Context context, String message, boolean closeActivity) {
        AlertDialog ad = new AlertDialog.Builder(context).setMessage(message).setCancelable(false).setPositiveButton(DataParse.getStr(context, "got_it", Home.spf), (dialog, id) -> {
            dialog.dismiss();
            if (closeActivity) ((Activity) context).finish();
        }).create();
        ad.setOnShowListener(arg0 -> ad.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.fb_color)));
        ad.show();
    }

    public static Bitmap addGradient(Bitmap bitmap) {
        final int reflectionGap = 10;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);
        Bitmap rBitmap = Bitmap.createBitmap(bitmap, 0, height / 2, width, height / 2, matrix, false);
        Bitmap fBitmap = Bitmap.createBitmap(width, (height + height / 3), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(fBitmap);
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.drawBitmap(rBitmap, 0, height + reflectionGap, null);
        Paint paint = new Paint();
        LinearGradient shader = new LinearGradient(0, bitmap.getHeight(), 0, fBitmap.getHeight() + reflectionGap, 0x40ffffff, 0x00ffffff, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawRect(0, height, width, fBitmap.getHeight() + reflectionGap, paint);
        if (bitmap.isRecycled()) {
            bitmap.recycle();
        }
        if (rBitmap.isRecycled()) {
            rBitmap.recycle();
        }
        return fBitmap;
    }

    public static int dpToPx(Context context, int dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static Intent startHtml(Context context, String f, boolean ori, boolean na) {
        Intent intent = new Intent(context, HtmlGame.class);
        intent.putExtra("id", f);
        intent.putExtra("landscape", ori);
        intent.putExtra("layout", R.layout.dialog_quit);
        intent.putExtra("tv", R.id.dialog_quit_desc);
        intent.putExtra("y", R.id.dialog_quit_yes);
        intent.putExtra("n", R.id.dialog_quit_no);
        intent.putExtra("desc", DataParse.getStr(context, "close_diag_desc2", Home.spf));
        intent.putExtra("color", ContextCompat.getColor(context, R.color.colorPrimaryDark));
        intent.putExtra("na", na);
        intent.putExtra("t", R.id.dialog_quit_title);
        intent.putExtra("title", DataParse.getStr(context, "are_you_sure", Home.spf));
        return intent;
    }

    @SuppressLint("ApplySharedPref")
    public static void chooseLocale(Activity activity, SharedPreferences spf, yesNo res) {
        Dialog localeDiag = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        View dView = LayoutInflater.from(activity).inflate(R.layout.dialog_locale, null);
        localeDiag.setContentView(dView);
        localeDiag.setCancelable(false);
        Window w = localeDiag.getWindow();
        if (w != null) {
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setNavigationBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
            w.setStatusBarColor(ContextCompat.getColor(activity, R.color.colorPrimaryDark));
        }
        TextView titleView = dView.findViewById(R.id.dialog_locale_title);
        titleView.setText(DataParse.getStr(activity, "select_language", spf));
        TextView closeView = dView.findViewById(R.id.dialog_locale_close);
        closeView.setText(DataParse.getStr(activity, "close", spf));
        closeView.setOnClickListener(view -> {
            localeDiag.dismiss();
            res.no();
        });
        LinearLayout listView = dView.findViewById(R.id.dialog_locale_listView);
        LayoutInflater inflater = LayoutInflater.from(activity);
        try {
            JSONObject locale = new JSONObject(spf.getString("locales", ""));
            JSONArray arrCode = locale.getJSONArray("code");
            JSONArray arrLang = locale.getJSONArray("lang");
            JSONArray arrImage = locale.getJSONArray("image");
            int length = Math.min(arrCode.length(), arrLang.length());
            if (length == 1) {
                spf.edit().putString("app_locale", "en").apply();
                res.yes();
                return;
            }
            LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 1));
            int colorPrimary = Color.argb(50, 0, 0, 0);
            for (int i = 0; i < length; i++) {
                View localeList = inflater.inflate(R.layout.dialog_locale_list, listView, false);
                ImageView imageView = localeList.findViewById(R.id.dialog_locale_list_imageView);
                TextView textView = localeList.findViewById(R.id.dialog_locale_list_nameView);
                textView.setText(arrLang.getString(i));
                Picasso.get().load(arrImage.getString(i)).into(imageView);
                final String code = arrCode.getString(i);
                localeList.setOnClickListener(v -> {
                    spf.edit().putString("app_locale", code).commit();
                    localeDiag.dismiss();
                    res.yes();
                });
                listView.addView(localeList);
                if (i < length - 1) {
                    View view = new View(activity);
                    view.setLayoutParams(params2);
                    view.setBackgroundColor(colorPrimary);
                    listView.addView(view);
                }
            }
        } catch (Exception e) {
            spf.edit().putString("app_locale", "en").apply();
            res.yes();
            return;
        }
        localeDiag.show();
    }


    public static void hGame(Activity activity, Integer errCode, String err) {
        activity.runOnUiThread(() -> {
            if (errCode == -1) {
                String credit = " " + Home.currency.toLowerCase() + "s";
                Dialog lowbalDiag = Misc.decoratedDiag(activity, R.layout.dialog_quit, 0.8f);
                lowbalDiag.setCancelable(false);
                TextView lowbalTitle = lowbalDiag.findViewById(R.id.dialog_quit_title);
                TextView lowbalDesc = lowbalDiag.findViewById(R.id.dialog_quit_desc);
                TextView quitNo = lowbalDiag.findViewById(R.id.dialog_quit_no);
                TextView quitYes = lowbalDiag.findViewById(R.id.dialog_quit_yes);
                lowbalTitle.setText((DataParse.getStr(activity, "low_bal_title", Home.spf) + credit + "!"));
                lowbalDesc.setText((DataParse.getStr(activity, "low_bal_desc_prefix", Home.spf) + credit + ". " + DataParse.getStr(activity, "low_bal_desc_suffix", Home.spf) + credit + "?"));
                quitNo.setText((DataParse.getStr(activity, "earn", Home.spf) + credit));
                quitNo.getBackground().setTint(ContextCompat.getColor(activity, R.color.green_2));
                quitYes.setText(DataParse.getStr(activity, "quit", Home.spf));
                quitYes.getBackground().setTint(ContextCompat.getColor(activity, R.color.gray_2));
                quitNo.setOnClickListener(view -> {
                    lowbalDiag.dismiss();
                    Variables.setHash("show_offers", "1");
                    activity.finish();
                });
                quitYes.setOnClickListener(view -> {
                    lowbalDiag.dismiss();
                    activity.finish();
                });
                lowbalDiag.show();
            } else {
                showMessage(activity, err, true);
            }
        });
    }

    public void gameInfo(Context context, HashMap<String, String> data, htmlYN yn) {
        Intent intent = Misc.startHtml(context, data.get("id"), data.get("ori").equals("1"), data.get("na").equals("1"));
        int reward = Integer.parseInt(data.get("reward"));
        String filename = data.get("file");
        if (reward == 0 && !filename.isEmpty()) {
            context.startActivity(intent);
        } else {
            Dialog gameDiag = Misc.decoratedDiag(context, R.layout.dialog_html, 0.8f);
            gameDiag.setCancelable(false);
            TextView titleView = gameDiag.findViewById(R.id.dialog_html_titleView);
            titleView.setText(DataParse.getStr(context, "game_info", Home.spf));
            ImageView imageView = gameDiag.findViewById(R.id.dialog_html_imageView);
            Picasso.get().load(data.get("image")).into(imageView);
            TextView nameView = gameDiag.findViewById(R.id.dialog_html_nameView);
            nameView.setText(data.get("name"));
            TextView descView = gameDiag.findViewById(R.id.dialog_html_descView);
            StringBuilder desc = new StringBuilder();
            if (!data.get("require").equals("0") && filename.isEmpty()) {
                desc.append(DataParse.getStr(context, "html_activation_amt", Home.spf).replace("AAA", "<b><font color='#ff0000'>" + data.get("require") + "&nbsp;" + Home.currency.toLowerCase() + "s</font></b>")).append(" ");
            }
            if (reward < 0) {
                desc.append(DataParse.getStr(context, "html_play_deduction", Home.spf).replace("AAA", "<b><font color='#0000ff'>" + (-reward) + "&nbsp;" + Home.currency.toLowerCase() + "s</font></b> in every " + data.get("rtime") + " seconds"));
            } else if (reward > 0) {
                desc.append(DataParse.getStr(context, "html_play_reward", Home.spf).replace("AAA", "<b><font color='#00ff00'>" + reward + "&nbsp;" + Home.currency.toLowerCase() + "s</font></b> in every " + data.get("rtime") + " seconds"));
            }
            descView.setText(html(desc.toString()));
            TextView desc2View = gameDiag.findViewById(R.id.dialog_quit_desc2);
            desc2View.setText(DataParse.getStr(context, "game_next", Home.spf));
            Button yBtn = gameDiag.findViewById(R.id.dialog_html_yes);
            yBtn.setText(DataParse.getStr(context, "yes", Home.spf));
            Button nBtn = gameDiag.findViewById(R.id.dialog_html_no);
            nBtn.setText(DataParse.getStr(context, "no", Home.spf));
            yBtn.setOnClickListener(view -> {
                context.startActivity(intent);
                yn.yes(gameDiag);
            });
            nBtn.setOnClickListener(view -> {
                yn.no(gameDiag);
            });
            gameDiag.show();
        }
    }

    public static void showReminder(Activity activity, String spotName, resp res) {
        if (!Home.spf.getBoolean(spotName, false)) {
            Dialog remDiag = Misc.decoratedDiag(activity, R.layout.dialog_quit, 0.8f);
            remDiag.setCancelable(false);
            TextView lowbalTitle = remDiag.findViewById(R.id.dialog_quit_title);
            TextView lowbalDesc = remDiag.findViewById(R.id.dialog_quit_desc);
            lowbalTitle.setText(html((DataParse.getStr(activity, "reminder", Home.spf))));
            lowbalDesc.setText(html(DataParse.getStr(activity, "reminder_desc", Home.spf)));
            TextView quitNo = remDiag.findViewById(R.id.dialog_quit_yes);
            quitNo.setText((DataParse.getStr(activity, "no", Home.spf)));
            quitNo.setOnClickListener(diagview -> remDiag.dismiss());
            TextView quitYes = remDiag.findViewById(R.id.dialog_quit_no);
            quitYes.setText(DataParse.getStr(activity, "yes", Home.spf));
            quitYes.setOnClickListener(diagview -> {
                Home.spf.edit().putBoolean(spotName, true).apply();
                remDiag.dismiss();
                res.clicked();
            });
            remDiag.show();
        } else {
            res.clicked();
        }
    }

    public static String getR(String url) {
        try {
            URL parsedUrl = new URL(url);
            String query = parsedUrl.getQuery();
            if (query != null) {
                for (String parameter : query.split("&")) {
                    String[] parts = parameter.split("=");
                    if (parts.length > 1 && parts[0].equals("referrer")) {
                        return parts[1];
                    }
                }
            }
        } catch (MalformedURLException e) {
            return null;
        }
        return null;
    }
}