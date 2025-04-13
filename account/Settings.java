package org.mintsoft.mintly.account;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.messaging.FirebaseMessaging;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintly.Home;
import org.mintsoft.mintly.R;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;
import org.mintsoft.mintly.helper.Variables;

public class Settings extends BaseAppCompat {
    private TextView verView;
    private SharedPreferences spf;
    private Button hardware, software;
    private SwitchCompat pushSw, localSw, globalSw;
    private boolean locked;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.settings);
        TextView titleView = findViewById(R.id.settings_title);
        titleView.setText(DataParse.getStr(this, "sett", Home.spf));
        TextView notifT = findViewById(R.id.settings_notifT);
        notifT.setText(DataParse.getStr(this, "notifications", Home.spf));
        TextView pushT = findViewById(R.id.settings_pushT);
        pushT.setText(DataParse.getStr(this, "receive_push_message", Home.spf));
        TextView localT = findViewById(R.id.settings_localT);
        localT.setText(DataParse.getStr(this, "personalized_message", Home.spf));
        TextView globalT = findViewById(R.id.settings_globalT);
        globalT.setText(DataParse.getStr(this, "global_notification", Home.spf));
        TextView layerT = findViewById(R.id.settings_layerT);
        layerT.setText(DataParse.getStr(this, "layer_type", Home.spf));
        TextView cacheT = findViewById(R.id.settings_cacheT);
        cacheT.setText(DataParse.getStr(this, "caching", Home.spf));
        TextView avT = findViewById(R.id.settings_appverT);
        avT.setText(DataParse.getStr(this, "app_ver", Home.spf));
        pushSw = findViewById(R.id.settings_push);
        localSw = findViewById(R.id.settings_local);
        globalSw = findViewById(R.id.settings_global);
        verView = findViewById(R.id.settings_app_ver);
        hardware = findViewById(R.id.settings_hardware);
        hardware.setText(DataParse.getStr(this, "hardware", Home.spf));
        software = findViewById(R.id.settings_software);
        software.setText(DataParse.getStr(this, "software", Home.spf));
        spf = Home.spf;
        pushSw.setChecked(spf.getInt("p_msgs", 0) == 1);
        localSw.setChecked(spf.getBoolean("l_msg", true));
        globalSw.setChecked(spf.getBoolean("g_msg", true));
        if (spf.getBoolean("is_hw", true)) {
            hardware.setBackgroundResource(R.drawable.rc_blue);
            software.setBackgroundResource(R.drawable.rc_colorprimary);
        } else {
            software.setBackgroundResource(R.drawable.rc_blue);
            hardware.setBackgroundResource(R.drawable.rc_colorprimary);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        verView.setText(Variables.getPHash("ver_n"));
        findViewById(R.id.settings_close).setOnClickListener(view -> finish());
        pushSw.setOnCheckedChangeListener((compoundButton, b) -> {
            if (locked) {
                Toast.makeText(this, DataParse.getStr(this, "please_wait", Home.spf), Toast.LENGTH_LONG).show();
                pushSw.setChecked(!b);
            } else {
                locked = true;
                if (b) {
                    FirebaseMessaging.getInstance().subscribeToTopic("misc")
                            .addOnCompleteListener(task -> {
                                spf.edit().putInt("p_msgs", 1).apply();
                                locked = false;
                            });
                } else {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("misc")
                            .addOnCompleteListener(task -> {
                                spf.edit().putInt("p_msgs", 0).apply();
                                locked = false;
                            });
                }
            }
        });
        localSw.setOnCheckedChangeListener((compoundButton, b) -> {
            spf.edit().putBoolean("l_msg", b).apply();
        });
        globalSw.setOnCheckedChangeListener((compoundButton, b) -> {
            spf.edit().putBoolean("g_msg", b).apply();
        });
        hardware.setOnClickListener(view -> {
            hardware.setBackgroundResource(R.drawable.rc_blue);
            software.setBackgroundResource(R.drawable.rc_colorprimary);
            spf.edit().putBoolean("is_hw", true).apply();
        });
        software.setOnClickListener(view -> {
            software.setBackgroundResource(R.drawable.rc_blue);
            hardware.setBackgroundResource(R.drawable.rc_colorprimary);
            spf.edit().putBoolean("is_hw", false).apply();
        });
        Button sCache = findViewById(R.id.settings_session_cache);
        sCache.setText(DataParse.getStr(this, "clear_session_cache", Home.spf));
        sCache.setOnClickListener(view -> {
            Variables.reset();
            Toast.makeText(Settings.this, DataParse.getStr(this, "session_cleared", Home.spf), Toast.LENGTH_LONG).show();
        });
        Button lCache = findViewById(R.id.settings_login_cache);
        lCache.setText(DataParse.getStr(this, "clear_login_data", Home.spf));
        lCache.setOnClickListener(view -> {
            setResult(9);
            finish();
        });
        TextView localeBtn = findViewById(R.id.settings_locale);
        localeBtn.setText(DataParse.getStr(this, "select_language", Home.spf));
        if (getResources().getBoolean(R.bool.show_translation_dialog)) {
            localeBtn.setOnClickListener(view -> {
                Misc.chooseLocale(this, spf, new Misc.yesNo() {
                    @Override
                    public void yes() {
                        setResult(7);
                        finish();
                    }

                    @Override
                    public void no() {
                    }
                });
            });
        } else {
            localeBtn.setVisibility(View.GONE);
        }
    }
}
