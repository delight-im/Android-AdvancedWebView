package im.delight.android.webview;

/*
 * Android-AdvancedWebView (https://github.com/delight-im/Android-AdvancedWebView)
 * Copyright (c) delight.im (https://www.delight.im/)
 * Licensed under the MIT License (https://opensource.org/licenses/MIT)
 */

import android.content.ActivityNotFoundException;
import android.view.ViewGroup;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.os.Environment;
import android.webkit.CookieManager;
import java.util.Arrays;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.HashMap;
import android.net.http.SslError;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.os.Message;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.WebStorage.QuotaUpdater;
import android.app.Fragment;
import android.util.Base64;
import android.os.Build;
import android.webkit.DownloadListener;
import android.graphics.Bitmap;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;
import java.util.MissingResourceException;
import java.util.Locale;
import java.util.LinkedList;
import java.util.Collection;
import java.util.List;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Map;

/** Advanced WebView component for Android that works as intended out of the box */
@SuppressWarnings("deprecation")
public class AdvancedWebView extends WebView {

	public interface Listener {
		void onPageStarted(String url, Bitmap favicon);
		void onPageFinished(String url);
		void onPageError(int errorCode, String description, String failingUrl);
		void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent);
		void onExternalPageRequest(String url);
	}

	public static final String PACKAGE_NAME_DOWNLOAD_MANAGER = "com.android.providers.downloads";
	protected static final int REQUEST_CODE_FILE_PICKER = 51426;
	protected static final String DATABASES_SUB_FOLDER = "/databases";
	protected static final String LANGUAGE_DEFAULT_ISO3 = "eng";
	protected static final String CHARSET_DEFAULT = "UTF-8";
	/** Alternative browsers that have their own rendering engine and *may* be installed on this device */
	protected static final String[] ALTERNATIVE_BROWSERS = new String[] { "org.mozilla.firefox", "com.android.chrome", "com.opera.browser", "org.mozilla.firefox_beta", "com.chrome.beta", "com.opera.browser.beta" };
	protected WeakReference<Activity> mActivity;
	protected WeakReference<Fragment> mFragment;
	protected Listener mListener;
	protected final List<String> mPermittedHostnames = new LinkedList<String>();
	/** File upload callback for platform versions prior to Android 5.0 */
	protected ValueCallback<Uri> mFileUploadCallbackFirst;
	/** File upload callback for Android 5.0+ */
	protected ValueCallback<Uri[]> mFileUploadCallbackSecond;
	protected long mLastError;
	protected String mLanguageIso3;
	protected int mRequestCodeFilePicker = REQUEST_CODE_FILE_PICKER;
	protected WebViewClient mCustomWebViewClient;
	protected WebChromeClient mCustomWebChromeClient;
	protected boolean mGeolocationEnabled;
	protected String mUploadableFileTypes = "*/*";
	protected final Map<String, String> mHttpHeaders = new HashMap<String, String>();

	public AdvancedWebView(Context context) {
		super(context);
		init(context);
	}

	public AdvancedWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public AdvancedWebView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	public void setListener(final Activity activity, final Listener listener) {
		setListener(activity, listener, REQUEST_CODE_FILE_PICKER);
	}

	public void setListener(final Activity activity, final Listener listener, final int requestCodeFilePicker) {
		if (activity != null) {
			mActivity = new WeakReference<Activity>(activity);
		}
		else {
			mActivity = null;
		}

		setListener(listener, requestCodeFilePicker);
	}

	public void setListener(final Fragment fragment, final Listener listener) {
		setListener(fragment, listener, REQUEST_CODE_FILE_PICKER);
	}

	public void setListener(final Fragment fragment, final Listener listener, final int requestCodeFilePicker) {
		if (fragment != null) {
			mFragment = new WeakReference<Fragment>(fragment);
		}
		else {
			mFragment = null;
		}

		setListener(listener, requestCodeFilePicker);
	}

	protected void setListener(final Listener listener, final int requestCodeFilePicker) {
		mListener = listener;
		mRequestCodeFilePicker = requestCodeFilePicker;
	}

	@Override
	public void setWebViewClient(final WebViewClient client) {
		mCustomWebViewClient = client;
	}

	@Override
	public void setWebChromeClient(final WebChromeClient client) {
		mCustomWebChromeClient = client;
	}

	@SuppressLint("SetJavaScriptEnabled")
	public void setGeolocationEnabled(final boolean enabled) {
		if (enabled) {
			getSettings().setJavaScriptEnabled(true);
			getSettings().setGeolocationEnabled(true);
			setGeolocationDatabasePath();
		}

		mGeolocationEnabled = enabled;
	}

	@SuppressLint("NewApi")
	protected void setGeolocationDatabasePath() {
		final Activity activity;

		if (mFragment != null && mFragment.get() != null && Build.VERSION.SDK_INT >= 11 && mFragment.get().getActivity() != null) {
			activity = mFragment.get().getActivity();
		}
		else if (mActivity != null && mActivity.get() != null) {
			activity = mActivity.get();
		}
		else {
			return;
		}

		getSettings().setGeolocationDatabasePath(activity.getFilesDir().getPath());
	}

	public void setUploadableFileTypes(final String mimeType) {
		mUploadableFileTypes = mimeType;
	}

	/**
	 * Loads and displays the provided HTML source text
	 *
	 * @param html the HTML source text to load
	 */
	public void loadHtml(final String html) {
		loadHtml(html, null);
	}

	/**
	 * Loads and displays the provided HTML source text
	 *
	 * @param html the HTML source text to load
	 * @param baseUrl the URL to use as the page's base URL
	 */
	public void loadHtml(final String html, final String baseUrl) {
		loadHtml(html, baseUrl, null);
	}

	/**
	 * Loads and displays the provided HTML source text
	 *
	 * @param html the HTML source text to load
	 * @param baseUrl the URL to use as the page's base URL
	 * @param historyUrl the URL to use for the page's history entry
	 */
	public void loadHtml(final String html, final String baseUrl, final String historyUrl) {
		loadHtml(html, baseUrl, historyUrl, "utf-8");
	}

	/**
	 * Loads and displays the provided HTML source text
	 *
	 * @param html the HTML source text to load
	 * @param baseUrl the URL to use as the page's base URL
	 * @param historyUrl the URL to use for the page's history entry
	 * @param encoding the encoding or charset of the HTML source text
	 */
	public void loadHtml(final String html, final String baseUrl, final String historyUrl, final String encoding) {
		loadDataWithBaseURL(baseUrl, html, "text/html", encoding, historyUrl);
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("all")
	public void onResume() {
		if (Build.VERSION.SDK_INT >= 11) {
			super.onResume();
		}
		resumeTimers();
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("all")
	public void onPause() {
		pauseTimers();
		if (Build.VERSION.SDK_INT >= 11) {
			super.onPause();
		}
	}

	public void onDestroy() {
		// try to remove this view from its parent first
		try {
			((ViewGroup) getParent()).removeView(this);
		}
		catch (Exception ignored) { }

		// then try to remove all child views from this view
		try {
			removeAllViews();
		}
		catch (Exception ignored) { }

		// and finally destroy this view
		destroy();
	}

	public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		if (requestCode == mRequestCodeFilePicker) {
			if (resultCode == Activity.RESULT_OK) {
				if (intent != null) {
					if (mFileUploadCallbackFirst != null) {
						mFileUploadCallbackFirst.onReceiveValue(intent.getData());
						mFileUploadCallbackFirst = null;
					}
					else if (mFileUploadCallbackSecond != null) {
						Uri[] dataUris = null;

						try {
							if (intent.getDataString() != null) {
								dataUris = new Uri[] { Uri.parse(intent.getDataString()) };
							}
							else {
								if (Build.VERSION.SDK_INT >= 16) {
									if (intent.getClipData() != null) {
										final int numSelectedFiles = intent.getClipData().getItemCount();

										dataUris = new Uri[numSelectedFiles];

										for (int i = 0; i < numSelectedFiles; i++) {
											dataUris[i] = intent.getClipData().getItemAt(i).getUri();
										}
									}
								}
							}
						}
						catch (Exception ignored) { }

						mFileUploadCallbackSecond.onReceiveValue(dataUris);
						mFileUploadCallbackSecond = null;
					}
				}
			}
			else {
				if (mFileUploadCallbackFirst != null) {
					mFileUploadCallbackFirst.onReceiveValue(null);
					mFileUploadCallbackFirst = null;
				}
				else if (mFileUploadCallbackSecond != null) {
					mFileUploadCallbackSecond.onReceiveValue(null);
					mFileUploadCallbackSecond = null;
				}
			}
		}
	}

	/**
	 * Adds an additional HTTP header that will be sent along with every HTTP `GET` request
	 *
	 * This does only affect the main requests, not the requests to included resources (e.g. images)
	 *
	 * If you later want to delete an HTTP header that was previously added this way, call `removeHttpHeader()`
	 *
	 * The `WebView` implementation may in some cases overwrite headers that you set or unset
	 *
	 * @param name the name of the HTTP header to add
	 * @param value the value of the HTTP header to send
	 */
	public void addHttpHeader(final String name, final String value) {
		mHttpHeaders.put(name, value);
	}

	/**
	 * Removes one of the HTTP headers that have previously been added via `addHttpHeader()`
	 *
	 * If you want to unset a pre-defined header, set it to an empty string with `addHttpHeader()` instead
	 *
	 * The `WebView` implementation may in some cases overwrite headers that you set or unset
	 *
	 * @param name the name of the HTTP header to remove
	 */
	public void removeHttpHeader(final String name) {
		mHttpHeaders.remove(name);
	}

	public void addPermittedHostname(String hostname) {
		mPermittedHostnames.add(hostname);
	}

	public void addPermittedHostnames(Collection<? extends String> collection) {
		mPermittedHostnames.addAll(collection);
	}

	public List<String> getPermittedHostnames() {
		return mPermittedHostnames;
	}

	public void removePermittedHostname(String hostname) {
		mPermittedHostnames.remove(hostname);
	}

	public void clearPermittedHostnames() {
		mPermittedHostnames.clear();
	}

	public boolean onBackPressed() {
		if (canGoBack()) {
			goBack();
			return false;
		}
		else {
			return true;
		}
	}

	@SuppressLint("NewApi")
	protected static void setAllowAccessFromFileUrls(final WebSettings webSettings, final boolean allowed) {
		if (Build.VERSION.SDK_INT >= 16) {
			webSettings.setAllowFileAccessFromFileURLs(allowed);
			webSettings.setAllowUniversalAccessFromFileURLs(allowed);
		}
	}

	@SuppressWarnings("static-method")
	public void setCookiesEnabled(final boolean enabled) {
		CookieManager.getInstance().setAcceptCookie(enabled);
	}

	@SuppressLint("NewApi")
	public void setThirdPartyCookiesEnabled(final boolean enabled) {
		if (Build.VERSION.SDK_INT >= 21) {
			CookieManager.getInstance().setAcceptThirdPartyCookies(this, enabled);
		}
	}

	public void setMixedContentAllowed(final boolean allowed) {
		setMixedContentAllowed(getSettings(), allowed);
	}

	@SuppressWarnings("static-method")
	@SuppressLint("NewApi")
	protected void setMixedContentAllowed(final WebSettings webSettings, final boolean allowed) {
		if (Build.VERSION.SDK_INT >= 21) {
			webSettings.setMixedContentMode(allowed ? WebSettings.MIXED_CONTENT_ALWAYS_ALLOW : WebSettings.MIXED_CONTENT_NEVER_ALLOW);
		}
	}

	public void setDesktopMode(final boolean enabled) {
		final WebSettings webSettings = getSettings();

		final String newUserAgent;
		if (enabled) {
			newUserAgent = webSettings.getUserAgentString().replace("Mobile", "eliboM").replace("Android", "diordnA");
		}
		else {
			newUserAgent = webSettings.getUserAgentString().replace("eliboM", "Mobile").replace("diordnA", "Android");
		}

		webSettings.setUserAgentString(newUserAgent);
		webSettings.setUseWideViewPort(enabled);
		webSettings.setLoadWithOverviewMode(enabled);
		webSettings.setSupportZoom(enabled);
		webSettings.setBuiltInZoomControls(enabled);
	}

	@SuppressLint({ "SetJavaScriptEnabled" })
	protected void init(Context context) {
		// in IDE's preview mode
		if (isInEditMode()) {
			// do not run the code from this method
			return;
		}

		if (context instanceof Activity) {
			mActivity = new WeakReference<Activity>((Activity) context);
		}

		mLanguageIso3 = getLanguageIso3();

		setFocusable(true);
		setFocusableInTouchMode(true);

		setSaveEnabled(true);

		final String filesDir = context.getFilesDir().getPath();
		final String databaseDir = filesDir.substring(0, filesDir.lastIndexOf("/")) + DATABASES_SUB_FOLDER;

		final WebSettings webSettings = getSettings();
		webSettings.setAllowFileAccess(false);
		setAllowAccessFromFileUrls(webSettings, false);
		webSettings.setBuiltInZoomControls(false);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setDomStorageEnabled(true);
		if (Build.VERSION.SDK_INT < 18) {
			webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
		}
		webSettings.setDatabaseEnabled(true);
		if (Build.VERSION.SDK_INT < 19) {
			webSettings.setDatabasePath(databaseDir);
		}

		if (Build.VERSION.SDK_INT >= 21) {
			webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
		}

		setThirdPartyCookiesEnabled(true);

		super.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				if (!hasError()) {
					if (mListener != null) {
						mListener.onPageStarted(url, favicon);
					}
				}

				if (mCustomWebViewClient != null) {
					mCustomWebViewClient.onPageStarted(view, url, favicon);
				}
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				if (!hasError()) {
					if (mListener != null) {
						mListener.onPageFinished(url);
					}
				}

				if (mCustomWebViewClient != null) {
					mCustomWebViewClient.onPageFinished(view, url);
				}
			}

			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				setLastError();

				if (mListener != null) {
					mListener.onPageError(errorCode, description, failingUrl);
				}

				if (mCustomWebViewClient != null) {
					mCustomWebViewClient.onReceivedError(view, errorCode, description, failingUrl);
				}
			}

			@Override
			public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
				if (!isPermittedUrl(url)) {
					// if a listener is available
					if (mListener != null) {
						// inform the listener about the request
						mListener.onExternalPageRequest(url);
					}

					// cancel the original request
					return true;
				}

				// if there is a user-specified handler available
				if (mCustomWebViewClient != null) {
					// if the user-specified handler asks to override the request
					if (mCustomWebViewClient.shouldOverrideUrlLoading(view, url)) {
						// cancel the original request
						return true;
					}
				}

				final Uri uri = Uri.parse(url);
				final String scheme = uri.getScheme();

				if (scheme != null) {
					final Intent externalSchemeIntent;

					if (scheme.equals("tel")) {
						externalSchemeIntent = new Intent(Intent.ACTION_DIAL, uri);
					}
					else if (scheme.equals("sms")) {
						externalSchemeIntent = new Intent(Intent.ACTION_SENDTO, uri);
					}
					else if (scheme.equals("mailto")) {
						externalSchemeIntent = new Intent(Intent.ACTION_SENDTO, uri);
					}
					else if (scheme.equals("whatsapp")) {
						externalSchemeIntent = new Intent(Intent.ACTION_SENDTO, uri);
						externalSchemeIntent.setPackage("com.whatsapp");
					}
					else {
						externalSchemeIntent = null;
					}

					if (externalSchemeIntent != null) {
						externalSchemeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

						try {
							if (mActivity != null && mActivity.get() != null) {
								mActivity.get().startActivity(externalSchemeIntent);
							}
							else {
								getContext().startActivity(externalSchemeIntent);
							}
						}
						catch (ActivityNotFoundException ignored) {}

						// cancel the original request
						return true;
					}
				}

				// route the request through the custom URL loading method
				view.loadUrl(url);

				// cancel the original request
				return true;
			}

			@Override
			public void onLoadResource(WebView view, String url) {
				if (mCustomWebViewClient != null) {
					mCustomWebViewClient.onLoadResource(view, url);
				}
				else {
					super.onLoadResource(view, url);
				}
			}

			@SuppressLint("NewApi")
			@SuppressWarnings("all")
			public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
				if (Build.VERSION.SDK_INT >= 11) {
					if (mCustomWebViewClient != null) {
						return mCustomWebViewClient.shouldInterceptRequest(view, url);
					}
					else {
						return super.shouldInterceptRequest(view, url);
					}
				}
				else {
					return null;
				}
			}

			@SuppressLint("NewApi")
			@SuppressWarnings("all")
			public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
				if (Build.VERSION.SDK_INT >= 21) {
					if (mCustomWebViewClient != null) {
						return mCustomWebViewClient.shouldInterceptRequest(view, request);
					}
					else {
						return super.shouldInterceptRequest(view, request);
					}
				}
				else {
					return null;
				}
			}

			@Override
			public void onFormResubmission(WebView view, Message dontResend, Message resend) {
				if (mCustomWebViewClient != null) {
					mCustomWebViewClient.onFormResubmission(view, dontResend, resend);
				}
				else {
					super.onFormResubmission(view, dontResend, resend);
				}
			}

			@Override
			public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
				if (mCustomWebViewClient != null) {
					mCustomWebViewClient.doUpdateVisitedHistory(view, url, isReload);
				}
				else {
					super.doUpdateVisitedHistory(view, url, isReload);
				}
			}

			@Override
			public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
				if (mCustomWebViewClient != null) {
					mCustomWebViewClient.onReceivedSslError(view, handler, error);
				}
				else {
					super.onReceivedSslError(view, handler, error);
				}
			}

			@SuppressLint("NewApi")
			@SuppressWarnings("all")
			public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
				if (Build.VERSION.SDK_INT >= 21) {
					if (mCustomWebViewClient != null) {
						mCustomWebViewClient.onReceivedClientCertRequest(view, request);
					}
					else {
						super.onReceivedClientCertRequest(view, request);
					}
				}
			}

			@Override
			public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
				if (mCustomWebViewClient != null) {
					mCustomWebViewClient.onReceivedHttpAuthRequest(view, handler, host, realm);
				}
				else {
					super.onReceivedHttpAuthRequest(view, handler, host, realm);
				}
			}

			@Override
			public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
				if (mCustomWebViewClient != null) {
					return mCustomWebViewClient.shouldOverrideKeyEvent(view, event);
				}
				else {
					return super.shouldOverrideKeyEvent(view, event);
				}
			}

			@Override
			public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
				if (mCustomWebViewClient != null) {
					mCustomWebViewClient.onUnhandledKeyEvent(view, event);
				}
				else {
					super.onUnhandledKeyEvent(view, event);
				}
			}

			@SuppressLint("NewApi")
			@SuppressWarnings("all")
			public void onUnhandledInputEvent(WebView view, InputEvent event) {
				if (Build.VERSION.SDK_INT >= 21) {
					if (mCustomWebViewClient != null) {
						mCustomWebViewClient.onUnhandledInputEvent(view, event);
					}
					else {
						super.onUnhandledInputEvent(view, event);
					}
				}
			}

			@Override
			public void onScaleChanged(WebView view, float oldScale, float newScale) {
				if (mCustomWebViewClient != null) {
					mCustomWebViewClient.onScaleChanged(view, oldScale, newScale);
				}
				else {
					super.onScaleChanged(view, oldScale, newScale);
				}
			}

			@SuppressLint("NewApi")
			@SuppressWarnings("all")
			public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
				if (Build.VERSION.SDK_INT >= 12) {
					if (mCustomWebViewClient != null) {
						mCustomWebViewClient.onReceivedLoginRequest(view, realm, account, args);
					}
					else {
						super.onReceivedLoginRequest(view, realm, account, args);
					}
				}
			}

		});

		super.setWebChromeClient(new WebChromeClient() {

			// file upload callback (Android 2.2 (API level 8) -- Android 2.3 (API level 10)) (hidden method)
			@SuppressWarnings("unused")
			public void openFileChooser(ValueCallback<Uri> uploadMsg) {
				openFileChooser(uploadMsg, null);
			}

			// file upload callback (Android 3.0 (API level 11) -- Android 4.0 (API level 15)) (hidden method)
			public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
				openFileChooser(uploadMsg, acceptType, null);
			}

			// file upload callback (Android 4.1 (API level 16) -- Android 4.3 (API level 18)) (hidden method)
			@SuppressWarnings("unused")
			public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
				openFileInput(uploadMsg, null, false);
			}

			// file upload callback (Android 5.0 (API level 21) -- current) (public method)
			@SuppressWarnings("all")
			public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
				if (Build.VERSION.SDK_INT >= 21) {
					final boolean allowMultiple = fileChooserParams.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE;

					openFileInput(null, filePathCallback, allowMultiple);

					return true;
				}
				else {
					return false;
				}
			}

			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onProgressChanged(view, newProgress);
				}
				else {
					super.onProgressChanged(view, newProgress);
				}
			}

			@Override
			public void onReceivedTitle(WebView view, String title) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onReceivedTitle(view, title);
				}
				else {
					super.onReceivedTitle(view, title);
				}
			}

			@Override
			public void onReceivedIcon(WebView view, Bitmap icon) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onReceivedIcon(view, icon);
				}
				else {
					super.onReceivedIcon(view, icon);
				}
			}

			@Override
			public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onReceivedTouchIconUrl(view, url, precomposed);
				}
				else {
					super.onReceivedTouchIconUrl(view, url, precomposed);
				}
			}

			@Override
			public void onShowCustomView(View view, CustomViewCallback callback) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onShowCustomView(view, callback);
				}
				else {
					super.onShowCustomView(view, callback);
				}
			}

			@SuppressLint("NewApi")
			@SuppressWarnings("all")
			public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
				if (Build.VERSION.SDK_INT >= 14) {
					if (mCustomWebChromeClient != null) {
						mCustomWebChromeClient.onShowCustomView(view, requestedOrientation, callback);
					}
					else {
						super.onShowCustomView(view, requestedOrientation, callback);
					}
				}
			}

			@Override
			public void onHideCustomView() {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onHideCustomView();
				}
				else {
					super.onHideCustomView();
				}
			}

			@Override
			public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
				}
				else {
					return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
				}
			}

			@Override
			public void onRequestFocus(WebView view) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onRequestFocus(view);
				}
				else {
					super.onRequestFocus(view);
				}
			}

			@Override
			public void onCloseWindow(WebView window) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onCloseWindow(window);
				}
				else {
					super.onCloseWindow(window);
				}
			}

			@Override
			public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onJsAlert(view, url, message, result);
				}
				else {
					return super.onJsAlert(view, url, message, result);
				}
			}

			@Override
			public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onJsConfirm(view, url, message, result);
				}
				else {
					return super.onJsConfirm(view, url, message, result);
				}
			}

			@Override
			public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onJsPrompt(view, url, message, defaultValue, result);
				}
				else {
					return super.onJsPrompt(view, url, message, defaultValue, result);
				}
			}

			@Override
			public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onJsBeforeUnload(view, url, message, result);
				}
				else {
					return super.onJsBeforeUnload(view, url, message, result);
				}
			}

			@Override
			public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
				if (mGeolocationEnabled) {
					callback.invoke(origin, true, false);
				}
				else {
					if (mCustomWebChromeClient != null) {
						mCustomWebChromeClient.onGeolocationPermissionsShowPrompt(origin, callback);
					}
					else {
						super.onGeolocationPermissionsShowPrompt(origin, callback);
					}
				}
			}

			@Override
			public void onGeolocationPermissionsHidePrompt() {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onGeolocationPermissionsHidePrompt();
				}
				else {
					super.onGeolocationPermissionsHidePrompt();
				}
			}

			@SuppressLint("NewApi")
			@SuppressWarnings("all")
			public void onPermissionRequest(PermissionRequest request) {
				if (Build.VERSION.SDK_INT >= 21) {
					if (mCustomWebChromeClient != null) {
						mCustomWebChromeClient.onPermissionRequest(request);
					}
					else {
						super.onPermissionRequest(request);
					}
				}
			}

			@SuppressLint("NewApi")
			@SuppressWarnings("all")
			public void onPermissionRequestCanceled(PermissionRequest request) {
				if (Build.VERSION.SDK_INT >= 21) {
					if (mCustomWebChromeClient != null) {
						mCustomWebChromeClient.onPermissionRequestCanceled(request);
					}
					else {
						super.onPermissionRequestCanceled(request);
					}
				}
			}

			@Override
			public boolean onJsTimeout() {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onJsTimeout();
				}
				else {
					return super.onJsTimeout();
				}
			}

			@Override
			public void onConsoleMessage(String message, int lineNumber, String sourceID) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onConsoleMessage(message, lineNumber, sourceID);
				}
				else {
					super.onConsoleMessage(message, lineNumber, sourceID);
				}
			}

			@Override
			public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.onConsoleMessage(consoleMessage);
				}
				else {
					return super.onConsoleMessage(consoleMessage);
				}
			}

			@Override
			public Bitmap getDefaultVideoPoster() {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.getDefaultVideoPoster();
				}
				else {
					return super.getDefaultVideoPoster();
				}
			}

			@Override
			public View getVideoLoadingProgressView() {
				if (mCustomWebChromeClient != null) {
					return mCustomWebChromeClient.getVideoLoadingProgressView();
				}
				else {
					return super.getVideoLoadingProgressView();
				}
			}

			@Override
			public void getVisitedHistory(ValueCallback<String[]> callback) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.getVisitedHistory(callback);
				}
				else {
					super.getVisitedHistory(callback);
				}
			}

			@Override
			public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota, long estimatedDatabaseSize, long totalQuota, QuotaUpdater quotaUpdater) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater);
				}
				else {
					super.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater);
				}
			}

			@Override
			public void onReachedMaxAppCacheSize(long requiredStorage, long quota, QuotaUpdater quotaUpdater) {
				if (mCustomWebChromeClient != null) {
					mCustomWebChromeClient.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
				}
				else {
					super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
				}
			}

		});

		setDownloadListener(new DownloadListener() {

			@Override
			public void onDownloadStart(final String url, final String userAgent, final String contentDisposition, final String mimeType, final long contentLength) {
				final String suggestedFilename = URLUtil.guessFileName(url, contentDisposition, mimeType);

				if (mListener != null) {
					mListener.onDownloadRequested(url, suggestedFilename, mimeType, contentLength, contentDisposition, userAgent);
				}
			}

		});
	}

	@Override
	public void loadUrl(final String url, Map<String, String> additionalHttpHeaders) {
		if (additionalHttpHeaders == null) {
			additionalHttpHeaders = mHttpHeaders;
		}
		else if (mHttpHeaders.size() > 0) {
			additionalHttpHeaders.putAll(mHttpHeaders);
		}

		super.loadUrl(url, additionalHttpHeaders);
	}

	@Override
	public void loadUrl(final String url) {
		if (mHttpHeaders.size() > 0) {
			super.loadUrl(url, mHttpHeaders);
		}
		else {
			super.loadUrl(url);
		}
	}

	public void loadUrl(String url, final boolean preventCaching) {
		if (preventCaching) {
			url = makeUrlUnique(url);
		}

		loadUrl(url);
	}

	public void loadUrl(String url, final boolean preventCaching, final Map<String,String> additionalHttpHeaders) {
		if (preventCaching) {
			url = makeUrlUnique(url);
		}

		loadUrl(url, additionalHttpHeaders);
	}

	protected static String makeUrlUnique(final String url) {
		StringBuilder unique = new StringBuilder();
		unique.append(url);

		if (url.contains("?")) {
			unique.append('&');
		}
		else {
			if (url.lastIndexOf('/') <= 7) {
				unique.append('/');
			}
			unique.append('?');
		}

		unique.append(System.currentTimeMillis());
		unique.append('=');
		unique.append(1);

		return unique.toString();
	}

	public boolean isPermittedUrl(final String url) {
		// if the permitted hostnames have not been restricted to a specific set
		if (mPermittedHostnames.size() == 0) {
			// all hostnames are allowed
			return true;
		}

		final Uri parsedUrl = Uri.parse(url);

		// get the hostname of the URL that is to be checked
		final String actualHost = parsedUrl.getHost();

		// if the hostname could not be determined, usually because the URL has been invalid
		if (actualHost == null) {
			return false;
		}

		// if the host contains invalid characters (e.g. a backslash)
		if (!actualHost.matches("^[a-zA-Z0-9._!~*')(;:&=+$,%\\[\\]-]*$")) {
			// prevent mismatches between interpretations by `Uri` and `WebView`, e.g. for `http://evil.example.com\.good.example.com/`
			return false;
		}

		// get the user information from the authority part of the URL that is to be checked
		final String actualUserInformation = parsedUrl.getUserInfo();

		// if the user information contains invalid characters (e.g. a backslash)
		if (actualUserInformation != null && !actualUserInformation.matches("^[a-zA-Z0-9._!~*')(;:&=+$,%-]*$")) {
			// prevent mismatches between interpretations by `Uri` and `WebView`, e.g. for `http://evil.example.com\@good.example.com/`
			return false;
		}

		// for every hostname in the set of permitted hosts
		for (String expectedHost : mPermittedHostnames) {
			// if the two hostnames match or if the actual host is a subdomain of the expected host
			if (actualHost.equals(expectedHost) || actualHost.endsWith("." + expectedHost)) {
				// the actual hostname of the URL to be checked is allowed
				return true;
			}
		}

		// the actual hostname of the URL to be checked is not allowed since there were no matches
		return false;
	}

	/**
	 * @deprecated use `isPermittedUrl` instead
	 */
	protected boolean isHostnameAllowed(final String url) {
		return isPermittedUrl(url);
	}

	protected void setLastError() {
		mLastError = System.currentTimeMillis();
	}

	protected boolean hasError() {
		return (mLastError + 500) >= System.currentTimeMillis();
	}

	protected static String getLanguageIso3() {
		try {
			return Locale.getDefault().getISO3Language().toLowerCase(Locale.US);
		}
		catch (MissingResourceException e) {
			return LANGUAGE_DEFAULT_ISO3;
		}
	}

	/**
	 * Provides localizations for the 25 most widely spoken languages that have a ISO 639-2/T code
	 *
	 * @return the label for the file upload prompts as a string
	 */
	protected String getFileUploadPromptLabel() {
		try {
			if (mLanguageIso3.equals("zho")) return decodeBase64("6YCJ5oup5LiA5Liq5paH5Lu2");
			else if (mLanguageIso3.equals("spa")) return decodeBase64("RWxpamEgdW4gYXJjaGl2bw==");
			else if (mLanguageIso3.equals("hin")) return decodeBase64("4KSP4KSVIOCkq+CkvOCkvuCkh+CksiDgpJrgpYHgpKjgpYfgpII=");
			else if (mLanguageIso3.equals("ben")) return decodeBase64("4KaP4KaV4Kaf4Ka/IOCmq+CmvuCmh+CmsiDgpqjgpr/gprDgp43gpqzgpr7gpprgpqg=");
			else if (mLanguageIso3.equals("ara")) return decodeBase64("2KfYrtiq2YrYp9ixINmF2YTZgSDZiNin2K3Yrw==");
			else if (mLanguageIso3.equals("por")) return decodeBase64("RXNjb2xoYSB1bSBhcnF1aXZv");
			else if (mLanguageIso3.equals("rus")) return decodeBase64("0JLRi9Cx0LXRgNC40YLQtSDQvtC00LjQvSDRhNCw0LnQuw==");
			else if (mLanguageIso3.equals("jpn")) return decodeBase64("MeODleOCoeOCpOODq+OCkumBuOaKnuOBl+OBpuOBj+OBoOOBleOBhA==");
			else if (mLanguageIso3.equals("pan")) return decodeBase64("4KiH4Kmx4KiVIOCoq+CovuCoh+CosiDgqJrgqYHgqKPgqYs=");
			else if (mLanguageIso3.equals("deu")) return decodeBase64("V8OkaGxlIGVpbmUgRGF0ZWk=");
			else if (mLanguageIso3.equals("jav")) return decodeBase64("UGlsaWggc2lqaSBiZXJrYXM=");
			else if (mLanguageIso3.equals("msa")) return decodeBase64("UGlsaWggc2F0dSBmYWls");
			else if (mLanguageIso3.equals("tel")) return decodeBase64("4LCS4LCVIOCwq+CxhuCxluCwsuCxjeCwqOCxgSDgsI7gsILgsJrgsYHgsJXgsYvgsILgsKHgsL8=");
			else if (mLanguageIso3.equals("vie")) return decodeBase64("Q2jhu41uIG3hu5l0IHThuq1wIHRpbg==");
			else if (mLanguageIso3.equals("kor")) return decodeBase64("7ZWY64KY7J2YIO2MjOydvOydhCDshKDtg50=");
			else if (mLanguageIso3.equals("fra")) return decodeBase64("Q2hvaXNpc3NleiB1biBmaWNoaWVy");
			else if (mLanguageIso3.equals("mar")) return decodeBase64("4KSr4KS+4KSH4KSyIOCkqOCkv+CkteCkoeCkvg==");
			else if (mLanguageIso3.equals("tam")) return decodeBase64("4K6S4K6w4K+BIOCuleCvh+CuvuCuquCvjeCuquCviCDgrqTgr4fgrrDgr43grrXgr4E=");
			else if (mLanguageIso3.equals("urd")) return decodeBase64("2KfbjNqpINmB2KfYptmEINmF24zauiDYs9uSINin2YbYqtiu2KfYqCDaqdix24zaug==");
			else if (mLanguageIso3.equals("fas")) return decodeBase64("2LHYpyDYp9mG2KrYrtin2Kgg2qnZhtuM2K8g24zaqSDZgdin24zZhA==");
			else if (mLanguageIso3.equals("tur")) return decodeBase64("QmlyIGRvc3lhIHNlw6dpbg==");
			else if (mLanguageIso3.equals("ita")) return decodeBase64("U2NlZ2xpIHVuIGZpbGU=");
			else if (mLanguageIso3.equals("tha")) return decodeBase64("4LmA4Lil4Li34Lit4LiB4LmE4Lif4Lil4LmM4Lir4LiZ4Li24LmI4LiH");
			else if (mLanguageIso3.equals("guj")) return decodeBase64("4KqP4KqVIOCqq+CqvuCqh+CqsuCqqOCrhyDgqqrgqrjgqoLgqqY=");
		}
		catch (Exception ignored) { }

		// return English translation by default
		return "Choose a file";
	}

	protected static String decodeBase64(final String base64) throws IllegalArgumentException, UnsupportedEncodingException {
		final byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
		return new String(bytes, CHARSET_DEFAULT);
	}

	@SuppressLint("NewApi")
	protected void openFileInput(final ValueCallback<Uri> fileUploadCallbackFirst, final ValueCallback<Uri[]> fileUploadCallbackSecond, final boolean allowMultiple) {
		if (mFileUploadCallbackFirst != null) {
			mFileUploadCallbackFirst.onReceiveValue(null);
		}
		mFileUploadCallbackFirst = fileUploadCallbackFirst;

		if (mFileUploadCallbackSecond != null) {
			mFileUploadCallbackSecond.onReceiveValue(null);
		}
		mFileUploadCallbackSecond = fileUploadCallbackSecond;

		Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.addCategory(Intent.CATEGORY_OPENABLE);

		if (allowMultiple) {
			if (Build.VERSION.SDK_INT >= 18) {
				i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
			}
		}

		i.setType(mUploadableFileTypes);

		if (mFragment != null && mFragment.get() != null && Build.VERSION.SDK_INT >= 11) {
			mFragment.get().startActivityForResult(Intent.createChooser(i, getFileUploadPromptLabel()), mRequestCodeFilePicker);
		}
		else if (mActivity != null && mActivity.get() != null) {
			mActivity.get().startActivityForResult(Intent.createChooser(i, getFileUploadPromptLabel()), mRequestCodeFilePicker);
		}
	}

	/**
	 * Returns whether file uploads can be used on the current device (generally all platform versions except for 4.4)
	 *
	 * @return whether file uploads can be used
	 */
	public static boolean isFileUploadAvailable() {
		return isFileUploadAvailable(false);
	}

	/**
	 * Returns whether file uploads can be used on the current device (generally all platform versions except for 4.4)
	 *
	 * On Android 4.4.3/4.4.4, file uploads may be possible but will come with a wrong MIME type
	 *
	 * @param needsCorrectMimeType whether a correct MIME type is required for file uploads or `application/octet-stream` is acceptable
	 * @return whether file uploads can be used
	 */
	public static boolean isFileUploadAvailable(final boolean needsCorrectMimeType) {
		if (Build.VERSION.SDK_INT == 19) {
			final String platformVersion = (Build.VERSION.RELEASE == null) ? "" : Build.VERSION.RELEASE;

			return !needsCorrectMimeType && (platformVersion.startsWith("4.4.3") || platformVersion.startsWith("4.4.4"));
		}
		else {
			return true;
		}
	}

	/**
	 * Handles a download by loading the file from `fromUrl` and saving it to `toFilename` on the external storage
	 *
	 * This requires the two permissions `android.permission.INTERNET` and `android.permission.WRITE_EXTERNAL_STORAGE`
	 *
	 * Only supported on API level 9 (Android 2.3) and above
	 *
	 * @param context a valid `Context` reference
	 * @param fromUrl the URL of the file to download, e.g. the one from `AdvancedWebView.onDownloadRequested(...)`
	 * @param toFilename the name of the destination file where the download should be saved, e.g. `myImage.jpg`
	 * @return whether the download has been successfully handled or not
	 * @throws IllegalStateException if the storage or the target directory could not be found or accessed
	 */
	@SuppressLint("NewApi")
	public static boolean handleDownload(final Context context, final String fromUrl, final String toFilename) {
		if (Build.VERSION.SDK_INT < 9) {
			throw new RuntimeException("Method requires API level 9 or above");
		}

		final Request request = new Request(Uri.parse(fromUrl));
		if (Build.VERSION.SDK_INT >= 11) {
			request.allowScanningByMediaScanner();
			request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		}
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, toFilename);

		final DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		try {
			try {
				dm.enqueue(request);
			}
			catch (SecurityException e) {
				if (Build.VERSION.SDK_INT >= 11) {
					request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
				}
				dm.enqueue(request);
			}

			return true;
		}
		// if the download manager app has been disabled on the device
		catch (IllegalArgumentException e) {
			// show the settings screen where the user can enable the download manager app again
			openAppSettings(context, AdvancedWebView.PACKAGE_NAME_DOWNLOAD_MANAGER);

			return false;
		}
	}

	@SuppressLint("NewApi")
	private static boolean openAppSettings(final Context context, final String packageName) {
		if (Build.VERSION.SDK_INT < 9) {
			throw new RuntimeException("Method requires API level 9 or above");
		}

		try {
			final Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			intent.setData(Uri.parse("package:" + packageName));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			context.startActivity(intent);

			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	/** Wrapper for methods related to alternative browsers that have their own rendering engines */
	public static class Browsers {

		/** Package name of an alternative browser that is installed on this device */
		private static String mAlternativePackage;

		/**
		 * Returns whether there is an alternative browser with its own rendering engine currently installed
		 *
		 * @param context a valid `Context` reference
		 * @return whether there is an alternative browser or not
		 */
		public static boolean hasAlternative(final Context context) {
			return getAlternative(context) != null;
		}

		/**
		 * Returns the package name of an alternative browser with its own rendering engine or `null`
		 *
		 * @param context a valid `Context` reference
		 * @return the package name or `null`
		 */
		public static String getAlternative(final Context context) {
			if (mAlternativePackage != null) {
				return mAlternativePackage;
			}

			final List<String> alternativeBrowsers = Arrays.asList(ALTERNATIVE_BROWSERS);
			final List<ApplicationInfo> apps = context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

			for (ApplicationInfo app : apps) {
				if (!app.enabled) {
					continue;
				}

				if (alternativeBrowsers.contains(app.packageName)) {
					mAlternativePackage = app.packageName;

					return app.packageName;
				}
			}

			return null;
		}

		/**
		 * Opens the given URL in an alternative browser
		 *
		 * @param context a valid `Activity` reference
		 * @param url the URL to open
		 */
		public static void openUrl(final Activity context, final String url) {
			openUrl(context, url, false);
		}

		/**
		 * Opens the given URL in an alternative browser
		 *
		 * @param context a valid `Activity` reference
		 * @param url the URL to open
		 * @param withoutTransition whether to switch to the browser `Activity` without a transition
		 */
		public static void openUrl(final Activity context, final String url, final boolean withoutTransition) {
			final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			intent.setPackage(getAlternative(context));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			context.startActivity(intent);

			if (withoutTransition) {
				context.overridePendingTransition(0, 0);
			}
		}

	}

}
