package org.mintsoft.mintly.helper;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.mintsoft.mintly.Splash;

public abstract class BaseAppCompat extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (!Variables.isLive) {
            startActivity(new Intent(this, Splash.class));
            finish();
        }
    }
}
