package com.advancewebview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Browser;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static com.advancewebview.AdvancedWebView.isPdf;

public class WebViewActivity extends AppCompatActivity implements View.OnClickListener, AdvancedWebView.Listener {
	protected AdvancedWebView webView;
	private ProgressBar webViewProgressBar, horizontalProgress;
	private ImageView back, forward, close, menu;
	private View webViewActionBar;
	private static String webViewUrl = "http://google.com";
	public static final String KEY_URL = "url";
	public static final String KEY_HEADER_DATA = "header";
	protected TextView tvTitle;
	protected ConstraintLayout llBottoms;
	private boolean canGoBack = true;
	boolean isPdfShowing = false;
	private List<HeaderObj> headerObjects;
	PDFView pdfView;

	public void setCanGoBack(boolean canGoBack) {
		this.canGoBack = canGoBack;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (getIntent().getExtras() != null) {
			webViewUrl = getIntent().getExtras().getString(KEY_URL);
			headerObjects = getIntent().getExtras().getParcelableArrayList(KEY_HEADER_DATA);
		}
		if (webViewUrl == null) webViewUrl = "";
		Log.e("Webview url", webViewUrl);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_advance_webview);
		initViews();
		back.setAlpha(0.3f);
		forward.setAlpha(0.3f);
		setUpWebView();
		setListeners();
		registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}

	protected void setActionBarColor(@ColorRes int color) {
		webViewActionBar.setBackgroundColor(ContextCompat.getColor(this, color));
	}

	private void initViews() {
		tvTitle = findViewById(R.id.tv_title);
		back = findViewById(R.id.webviewBack);
		forward = findViewById(R.id.webviewForward);
		close = findViewById(R.id.webviewClose);
		menu = findViewById(R.id.webviewMenu);
		webViewProgressBar = findViewById(R.id.webViewProgressBar);
		horizontalProgress = findViewById(R.id.horizontalProgress);
		llBottoms = findViewById(R.id.ll_bottoms);
		webView = findViewById(R.id.sitesWebView);
		webView.setListener(this, this);
		webViewActionBar = findViewById(R.id.webview_actionbar);
		pdfView = findViewById(R.id.pdfView);
		pdfView.setVisibility(View.GONE);
	}


	private void setUpWebView() {
		webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 7.0; SM-G930V Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 Mobile Safari/537.36");
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
		webView.getSettings().setSupportZoom(true);
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setDisplayZoomControls(false);
		webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
		webView.getSettings().setUseWideViewPort(true);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onLoadResource(WebView view, String url) {

				super.onLoadResource(view, url);
			}
		});
//        webView.setInitialScale(1);
		LoadWebViewUrl(webViewUrl);
	}

	private void setListeners() {
		back.setOnClickListener(this);
		forward.setOnClickListener(this);
		close.setOnClickListener(this);
		menu.setOnClickListener(this);
	}

	public static Map<String, String> getParams(String query) {
		query = query.split("/")[query.split("/").length - 1];
		String[] params = query.split("&");
		Map<String, String> map = new HashMap<>();
		for (String param : params) {
			String[] pair = param.split("=");
			if (pair.length == 2) {
				String name = pair[0];
				String value = pair[1];
				map.put(name, value);
			}
		}
		// Log.d(getClass().getSimpleName(), "getParams() called with: query = [" + map.toString() + "]");
		return map;
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.webviewBack) {
			onBackPressed();
		} else if (view.getId() == R.id.webviewForward) {
			if (webView.canGoForward())
				webView.goForward();
		} else if (view.getId() == R.id.webviewReload) {
			if (webView != null) {
				webView.reload();
			}
		} else if (view.getId() == R.id.webviewClose) {
			finish();
		} else if (view.getId() == R.id.webviewMenu) {
			showMenu();
		}
	}

	private void showMenu() {
		PopupMenu popup = new PopupMenu(this, menu);
		popup.getMenuInflater().inflate(R.menu.menu, popup.getMenu());
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getItemId() == R.id.open_in_browser) {
					if (webView.getUrl() != null) {
						try {
							Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webView.getUrl()));
							if (headerObjects != null) {
								Bundle bundle = new Bundle();
								for (HeaderObj headerObj : headerObjects) {
									bundle.putString(headerObj.getHeaderName(), headerObj.getHeaderData());
								}
								browserIntent.putExtra(Browser.EXTRA_HEADERS, bundle);
							}
							startActivity(browserIntent);
						} catch (ActivityNotFoundException e) {
							Toast.makeText(WebViewActivity.this, "Thiết bị không có trình duyệt", Toast.LENGTH_SHORT).show();
						}
					}
				} else if (item.getItemId() == R.id.reload) {
					webView.reload();
				}
				return true;
			}
		});
		popup.show();
	}

	@Override
	public void onPageStarted(String url, Bitmap favicon) {
//        if (!webViewProgressBar.isShown())
//            webViewProgressBar.setVisibility(View.VISIBLE);
		if (!horizontalProgress.isShown())
			horizontalProgress.setVisibility(View.VISIBLE);
	}

	@Override
	public void onPageFinished(String url) {
//        if (webViewProgressBar.isShown())
//            webViewProgressBar.setVisibility(View.GONE);
		if (horizontalProgress.isShown())
			horizontalProgress.setVisibility(View.GONE);
		if (webView.canGoBack()) back.setAlpha(1f);
		else back.setAlpha(0.3f);
		if (webView.canGoForward()) forward.setAlpha(1f);
		else forward.setAlpha(0.3f);
	}

	@Override
	public void onPageError(int errorCode, String description, String failingUrl) {
//        if (webViewProgressBar.isShown())
//            webViewProgressBar.setVisibility(View.GONE);
	}


	long downloadID;
	String fileName;
	String mimeType;
	private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//Fetching the download id received with the broadcast
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
			//Checking if the received broadcast is for our enqueued download by matching download id
			if (downloadID == id) {
				openDownloadedAttachment(context, downloadID);
			}
		}
	};

	private void openDownloadedAttachment(final Context context, final long downloadId) {
		DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		DownloadManager.Query query = new DownloadManager.Query();
		query.setFilterById(downloadId);
		Cursor cursor = downloadManager.query(query);
		if (cursor.moveToFirst()) {
			int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
			String downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
			String downloadMimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
			if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL) && downloadLocalUri != null) {
				openDownloadedAttachment(context, Uri.parse(downloadLocalUri), downloadMimeType);
			}
		}
		cursor.close();
	}


	private void openDownloadedAttachment(final Context context, Uri attachmentUri, final String attachmentMimeType) {
		if (isPdf(mimeType)) showPdf(attachmentUri);
		else {
			if (attachmentUri != null) {
				if (ContentResolver.SCHEME_FILE.equals(attachmentUri.getScheme()) && attachmentUri.getPath() != null) {
					File file = new File(attachmentUri.getPath());
					FileOpen.openFile(context, file, mimeType);
				}
			}
		}
	}

	private void showPdf(Uri attachmentUri) {
		pdfView.setVisibility(View.VISIBLE);
		webView.setVisibility(View.GONE);

		pdfView.fromUri(attachmentUri)
			.defaultPage(0)
			.enableSwipe(true)
			.swipeHorizontal(false)
			.onPageChange(new OnPageChangeListener() {
				@Override
				public void onPageChanged(int page, int pageCount) {

				}
			})
			.enableAnnotationRendering(true)
			.onLoad(new OnLoadCompleteListener() {
				@Override
				public void loadComplete(int nbPages) {
					back.setAlpha(1f);
					horizontalProgress.setVisibility(View.GONE);
					isPdfShowing = true;
				}
			})
			.scrollHandle(new DefaultScrollHandle(this))
			.load();
	}

	public static final String[] PERMISSIONS_STORAGE = {
		Manifest.permission.READ_EXTERNAL_STORAGE,
		Manifest.permission.WRITE_EXTERNAL_STORAGE
	};
	public static final int REQUEST_EXTERNAL_STORAGE = 1;

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public static boolean verifyStoragePermissions(Activity activity) {
		if (activity == null) return false;
		int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		int permission1 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
		if (permission != PackageManager.PERMISSION_GRANTED || permission1 != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(
				activity,
				PERMISSIONS_STORAGE,
				REQUEST_EXTERNAL_STORAGE
			);
			return false;
		} else return true;
	}

	// url = file path or whatever suitable URL you want.
	public static String getMimeType(String url) {
		String type = null;
		String extension = MimeTypeMap.getFileExtensionFromUrl(url);
		if (extension != null) {
			type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		}
		return type;
	}

	@Override
	public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {
		if (AdvancedWebView.isSpreadSheets(mimeType) || AdvancedWebView.isDocs(mimeType) || isPdf(mimeType)) {
			if (!verifyStoragePermissions(this)) return;
//            FileOpen.openFile(this, url,mimeType);
			File file = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS) + "/" + suggestedFilename);
			if (file.exists() && getMimeType(file.getPath()).equals(mimeType)) {
				Log.e("File", suggestedFilename);
				showPdf(Uri.fromFile(file));
			} else {
				DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
				request.setDescription("");
				request.setTitle(suggestedFilename);
				request.allowScanningByMediaScanner();
				request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
					DownloadManager.Request.NETWORK_MOBILE)
					.setAllowedOverRoaming(false);
				request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
				request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, suggestedFilename);
				DownloadManager manager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
				if (manager != null) {

					fileName = suggestedFilename;
					this.mimeType = mimeType;
					downloadID = manager.enqueue(request);
				}
			}
		} else {
			pdfView.setVisibility(View.GONE);
			webView.setVisibility(View.VISIBLE);
			// Log.d(getClass().getSimpleName(), "LoadWebViewUrl() called with: url = [" + url + "]");
			if (TextUtils.isEmpty(url)) return;
			if (AdvancedWebView.isFileUrl(url) && !url.contains("https://docs.google.com"))
				url = "https://docs.google.com/viewerng/viewer?url=" + url;
			if (isInternetConnected())
				webView.loadUrl(checkUrl(url), false);
			else {
				Toast.makeText(WebViewActivity.this, "Oops!! There is no internet connection. Please enable your internet connection.", Toast.LENGTH_LONG).show();
			}
		}


		//Mở file
//        LoadWebViewUrl(url);
	}


	@Override
	public void onExternalPageRequest(String url) {
	}

	@Override
	public void onReceivedTitle(String title) {
		this.tvTitle.setText(title);
	}

	@Override
	public void onProgressChanged(int newProgress) {
		horizontalProgress.setProgress(newProgress);
	}

	@Override
	public void onReceivedIcon(Bitmap icon) {

	}


	@SuppressLint("NewApi")
	@Override
	protected void onResume() {
		super.onResume();
		webView.onResume();
		// ...
	}

	@SuppressLint("NewApi")
	@Override
	protected void onPause() {
		webView.onPause();
		// ...
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		webView.onDestroy();
		unregisterReceiver(onDownloadComplete);
		// ...
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		webView.onActivityResult(requestCode, resultCode, intent);
		// ...
	}

	@Override
	public void onBackPressed() {
		if (isPdfShowing) {
			webView.setVisibility(View.VISIBLE);
			pdfView.setVisibility(View.GONE);
			isPdfShowing = false;
			if (webView.canGoBack()) back.setAlpha(1f);
			else back.setAlpha(0.3f);
			return;
		}
		if (canGoBack && !webView.onBackPressed()) {
			return;
		}
		// ...
		super.onBackPressed();
	}


	public void LoadWebViewUrl(String url) {
		isPdfShowing = false;
		pdfView.setVisibility(View.GONE);
		webView.setVisibility(View.VISIBLE);
		HashMap<String, String> headers = new HashMap<>();
		if (headerObjects != null) {
			for (HeaderObj headerObject : headerObjects) {
				headers.put(headerObject.getHeaderName(), headerObject.getHeaderData());
			}
		}
		// Log.d(getClass().getSimpleName(), "LoadWebViewUrl() called with: url = [" + url + "]");
		if (TextUtils.isEmpty(url)) return;
		if (isInternetConnected())
			webView.loadUrl(checkUrl(url), false, headers);
		else {
			Toast.makeText(WebViewActivity.this, "Oops!! There is no internet connection. Please enable your internet connection.", Toast.LENGTH_LONG).show();
		}
	}

	private String checkUrl(String url) {
//        if (AdvancedWebView.isFileUrl(url) && !url.contains("https://docs.google.com"))
//            url = "https://docs.google.com/viewerng/viewer?url=" + url;
		return url;
	}

	public boolean isInternetConnected() {
		// At activity_album startup we manually check the internet status and change
		// the text status
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		return networkInfo != null && networkInfo.isConnected();
	}
}
