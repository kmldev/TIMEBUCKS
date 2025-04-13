package org.mintsoft.mintly;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.mintsoft.mintlib.DataParse;
import org.mintsoft.mintly.frags.FragHgame;
import org.mintsoft.mintly.frags.FragHhist;
import org.mintsoft.mintly.frags.FragHref;
import org.mintsoft.mintly.frags.FragHwdr;
import org.mintsoft.mintly.helper.BaseAppCompat;
import org.mintsoft.mintly.helper.Misc;

import java.util.ArrayList;

public class History extends BaseAppCompat {
    private ArrayList<Fragment> fragments;
    private ArrayList<String> tabs;
    public static boolean loadAll;
    public static int goTo = -1;
    public static ViewPager2 viewPager;
    public static Dialog histLoading, hConDiag;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.history);
        histLoading = Misc.loadingDiag(this);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            goTo = extras.getInt("pos", -1);
        }
        TextView titleView = findViewById(R.id.history_title);
        titleView.setText(DataParse.getStr(this, "history", Home.spf));
        fragments = new ArrayList<>();
        fragments.add(new FragHhist());
        fragments.add(new FragHgame());
        fragments.add(new FragHref());
        tabs = new ArrayList<>();
        tabs.add(DataParse.getStr(this, "hist_activity", Home.spf));
        tabs.add(DataParse.getStr(this, "hist_game", Home.spf));
        tabs.add(DataParse.getStr(this, "hist_invite", Home.spf));
        if (Home.canRedeem) {
            fragments.add(new FragHwdr());
            tabs.add(DataParse.getStr(this, "hist_gift", Home.spf));
        }
        viewPager = findViewById(R.id.history_viewPager);
        viewPager.setSaveEnabled(false);
        pagerAdapter adapter = new pagerAdapter(this);
        viewPager.setAdapter(adapter);
        TabLayout tabLayout = findViewById(R.id.history_tabLayout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(tabs.get(position))).attach();
        findViewById(R.id.history_back).setOnClickListener(v -> finish());
    }

    private class pagerAdapter extends FragmentStateAdapter {
        public pagerAdapter(AppCompatActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int pos) {
            return fragments.get(pos);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }
}
