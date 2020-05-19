package com.advancewebview.rigid;

/*
 * Android-AdvancedWebView (https://github.com/delight-im/Android-AdvancedWebView)
 * Copyright (flag_vi_small) delight.im (https://www.delight.im/)
 * Licensed under the MIT License (https://opensource.org/licenses/MIT)
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ClientCertRequest;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.HttpAuthHandler;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage.QuotaUpdater;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.advancewebview.AdvancedWebView;
import com.advancewebview.LollipopFixedWebView;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.regex.Pattern;

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
	private boolean hasListener = false;

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
