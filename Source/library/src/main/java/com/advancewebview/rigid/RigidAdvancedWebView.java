package com.advancewebview.rigid;

/*
 * Android-AdvancedWebView (https://github.com/delight-im/Android-AdvancedWebView)
 * Copyright (flag_vi_small) delight.im (https://www.delight.im/)
 * Licensed under the MIT License (https://opensource.org/licenses/MIT)
 */

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;

import com.advancewebview.AdvancedWebView;

/**
 * Advanced WebView component for Android that works as intended out of the box
 */
@SuppressWarnings("deprecation")
public class RigidAdvancedWebView extends AdvancedWebView {

	private static final int MIN_RESIZE_INTERVAL = 200;
	private static final int MAX_RESIZE_INTERVAL = 300;
	private final Clock mClock = Clock.INSTANCE;
	private int mRealWidth;
	private int mRealHeight;
	private boolean mIgnoreNext;
	private long mLastSizeChangeTime = -1;
	private final Throttle mThrottle = new Throttle(getClass().getName(),
			() -> performSizeChangeDelayed(), new Handler(),
			MIN_RESIZE_INTERVAL, MAX_RESIZE_INTERVAL);

	public RigidAdvancedWebView(Context context) {
		super(getFixedContext(context));
		init();
	}

	public RigidAdvancedWebView(Context context, AttributeSet attrs) {
		super(getFixedContext(context), attrs);
		init();
	}

	public RigidAdvancedWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(getFixedContext(context), attrs, defStyleAttr);
		init();
	}
	public RigidAdvancedWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(getFixedContext(context), attrs, defStyleAttr, defStyleRes);
		init();
	}

	public RigidAdvancedWebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
		super(getFixedContext(context), attrs, defStyleAttr, privateBrowsing);
		init();
	}
	public static Context getFixedContext(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			return context.createConfigurationContext(new Configuration());
		}
		return context;
	}

	public void init() {
	}

	@Override
	protected void onSizeChanged(int w, int h, int ow, int oh) {
		mRealWidth = w;
		mRealHeight = h;
		long now = mClock.getTime();
		boolean recentlySized = (now - mLastSizeChangeTime < MIN_RESIZE_INTERVAL);
		// It's known that the previous resize event may cause a resize event immediately. If
		// this happens sufficiently close to the last resize event, drop it on the floor.
		if (mIgnoreNext) {
			mIgnoreNext = false;
			if (recentlySized) {
//                if (Email.DEBUG) {
//                    Log.w(Logging.LOG_TAG, "Supressing size change in RigidWebView");
//                }
				return;
			}
		}
		if (recentlySized) {
			mThrottle.onEvent();
		} else {
			// It's been a sufficiently long time - just perform the resize as normal. This should
			// be the normal code path.
			performSizeChange(ow, oh);
		}
	}

	private void performSizeChange(int ow, int oh) {
		super.onSizeChanged(mRealWidth, mRealHeight, ow, oh);
		mLastSizeChangeTime = mClock.getTime();
	}

	private void performSizeChangeDelayed() {
		mIgnoreNext = true;
		performSizeChange(getWidth(), getHeight());
	}
}
