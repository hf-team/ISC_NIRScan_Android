package com.Innospectra.NanoScan;


import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * 可以动态控制左右滑动的ViewPager
 */
public class NoScrollViewPager extends ViewPager {
    private boolean mCanScroll = false;

    public NoScrollViewPager(Context context) {
        super(context);
    }

    public NoScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param canScroll true 可以左右滑动, false 禁止作用滑动
     */
    public void setCanScroll(boolean canScroll) {
        this.mCanScroll = canScroll;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mCanScroll;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mCanScroll;
    }
}