package org.mintsoft.mintly.helper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.mintsoft.mintly.Splash;

public abstract class BaseActivity extends Activity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (!Variables.isLive) {
            startActivity(new Intent(this, Splash.class));
            finish();
        }
    }
}
