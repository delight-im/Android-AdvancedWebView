/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.advancewebview.rigid;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.webkit.WebView;


/**
 * A custom WebView that is robust to rapid resize events in sequence.
 * <p>
 * This is useful for a WebView which needs to have a layout of {@code WRAP_CONTENT}, since any
 * contents with percent-based height will force the WebView to infinitely expand (or shrink).
 */
public class RigidWebView extends WebView {
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
	private boolean hasListener = false;

	public RigidWebView(Context context) {
		super(getFixedContext(context));
		init();
	}

	public RigidWebView(Context context, AttributeSet attrs) {
		super(getFixedContext(context), attrs);
		init();
	}

	public RigidWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(getFixedContext(context), attrs, defStyleAttr);
		init();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public RigidWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(getFixedContext(context), attrs, defStyleAttr, defStyleRes);
		init();
	}

	public RigidWebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
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
		if (hasListener) return;
		hasListener = true;
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
