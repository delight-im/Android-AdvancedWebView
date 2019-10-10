package com.advancewebview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;

import java.lang.reflect.Field;

import es.voghdev.pdfviewpager.library.RemotePDFViewPager;
import es.voghdev.pdfviewpager.library.remote.DownloadFile;

public class VerticalViewPager extends RemotePDFViewPager {
	public VerticalViewPager(Context context, String pdfUrl, DownloadFile.Listener listener) {
		super(context, pdfUrl, listener);
		init();
	}

	public VerticalViewPager(Context context, DownloadFile downloadFile, String pdfUrl, DownloadFile.Listener listener) {
		super(context, downloadFile, pdfUrl, listener);
		init();
	}

	public VerticalViewPager(Context context) {
		this(context, null);
		init();
	}

	public VerticalViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		// The majority of the magic happens here
		setPageTransformer(false, new VerticalPageTransformer());
		// The easiest way to get rid of the overscroll drawing that happens on the left and right
		setOverScrollMode(OVER_SCROLL_NEVER);

		try {
			Class cls = this.getClass().getSuperclass();
			Field distanceField = cls.getDeclaredField("mFlingDistance");
			distanceField.setAccessible(true);
			distanceField.setInt(this, distanceField.getInt(this) / 40);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		try {
			Class cls = this.getClass().getSuperclass();
			Field minVelocityField = cls.getDeclaredField("mMinimumVelocity");
			minVelocityField.setAccessible(true);
			minVelocityField.setInt(this, minVelocityField.getInt(this) / 25);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		try {
			Class cls = this.getClass().getSuperclass();
			Field maxVelocityField = cls.getDeclaredField("mMaximumVelocity");
			maxVelocityField.setAccessible(true);
			maxVelocityField.setInt(this, maxVelocityField.getInt(this) * 10);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		try {
			Class cls = this.getClass().getSuperclass();
			Field slopField = cls.getDeclaredField("mTouchSlop");
			slopField.setAccessible(true);
			slopField.setInt(this, slopField.getInt(this) / 10);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		try {
			Class cls = this.getClass().getSuperclass();
			Field minHeightWidthRatioField = cls.getDeclaredField("minYXRatioForIntercept");
			minHeightWidthRatioField.setAccessible(true);
			minHeightWidthRatioField.setFloat(this, minHeightWidthRatioField.getFloat(this) * 8);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		try {
			Class cls = this.getClass().getSuperclass();
			Field minHeightWidthRatioField = cls.getDeclaredField("minYXRatioForTouch");
			minHeightWidthRatioField.setAccessible(true);
			minHeightWidthRatioField.setInt(this, minHeightWidthRatioField.getInt(this) * 4);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Swaps the X and Y coordinates of your touch event.
	 */
	private MotionEvent swapXY(MotionEvent ev) {
		float width = getWidth();
		float height = getHeight();

		float y = ev.getY();
		float x = ev.getX();

		float newX = (y / height) * width;
		float newY = (x / width) * height;

		ev.setLocation(newX, newY);

		return ev;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		boolean intercepted = super.onInterceptTouchEvent(swapXY(ev));
		swapXY(ev); // return touch coordinates to original reference frame for any child views
		return intercepted;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return super.onTouchEvent(swapXY(ev));
	}

	private class VerticalPageTransformer implements ViewPager.PageTransformer {
		private static final float MIN_SCALE = 0.75f;

		@Override
		public void transformPage(View view, float position) {

			if (position <= -1) { // [-Infinity,-1)
				// This page is way off-screen to the left.
				view.setAlpha(0);

			} else if (position <= 0) { // [-1,0]
				// Use the default slide transition when moving to the left/top page
				view.setAlpha(1);
				ViewCompat.setElevation(view, 1);
				// Counteract the default slide transition
				view.setTranslationX(view.getWidth() * -position);
				view.setTranslationY(0);

				//set Y position to swipe in from top
				float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
				view.setScaleX(scaleFactor);
				view.setScaleY(scaleFactor);

			} else if (position <= 1) { // [0,1]
				view.setAlpha(1);
				ViewCompat.setElevation(view, 2);

				// Counteract the default slide transition
				view.setTranslationX(view.getWidth() * -position);
				view.setTranslationY(position * view.getHeight());

				// Scale the page down (between MIN_SCALE and 1)
				view.setScaleX(1);
				view.setScaleY(1);

			} else { // (1,+Infinity]
				// This page is way off-screen to the right.
				view.setAlpha(0);
			}

		}
	}

}
