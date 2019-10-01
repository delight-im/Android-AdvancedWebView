package com.advancewebview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

import es.voghdev.pdfviewpager.library.RemotePDFViewPager;
import es.voghdev.pdfviewpager.library.remote.DownloadFile;


public class VerticalViewPager extends RemotePDFViewPager {


	public VerticalViewPager(Context context, String pdfUrl, DownloadFile.Listener listener) {
		super(context, pdfUrl, listener);
		init();
	}

	public VerticalViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		// The majority of the magic happens here
		setPageTransformer(true, new VerticalPageTransformer());
		// The easiest way to get rid of the overscroll drawing that happens on the left and right
		setOverScrollMode(View.OVER_SCROLL_NEVER);
	}

	private class VerticalPageTransformer implements ViewPager.PageTransformer {

		@Override
		public void transformPage(View view, float position) {

			if (position < -1) { // [-Infinity,-1)
				// This page is way off-screen to the left.
				view.setAlpha(0);

			} else if (position <= 1) { // [-1,1]
				view.setAlpha(1);

				// Counteract the default slide transition
				view.setTranslationX(view.getWidth() * -position);

				//set Y position to swipe in from top
				float yPosition = position * view.getHeight();
				view.setTranslationY(yPosition);

			} else { // (1,+Infinity]
				// This page is way off-screen to the right.
				view.setAlpha(0);
			}
		}
	}

	/**
	 * Swaps the X and Y coordinates of your touch event.
	 */
	private MotionEvent swapXY(MotionEvent ev) {
		float width = getWidth();
		float height = getHeight();

		float newX = (ev.getY() / height) * width;
		float newY = (ev.getX() / width) * height;

		ev.setLocation(newX, newY);

		return ev;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		try {
			boolean intercepted = super.onInterceptTouchEvent(swapXY(ev));
			swapXY(ev); // return touch coordinates to original reference frame for any child views
			return intercepted;
		} catch (Throwable e) {
			try {
				return super.onInterceptTouchEvent(ev);
			} catch (Throwable e1) {
				return false;
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return super.onTouchEvent(swapXY(ev));
	}

}
