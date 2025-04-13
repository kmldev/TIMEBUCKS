package org.mintsoft.mintly.helper;

import androidx.viewpager.widget.ViewPager;

public interface pIndi extends ViewPager.OnPageChangeListener {
    void setViewPager(ViewPager view);

    void setViewPager(ViewPager view, int initialPosition);

    void setCurrentItem(int item);

    void setOnPageChangeListener(ViewPager.OnPageChangeListener listener);

    void notifyDataSetChanged();

}