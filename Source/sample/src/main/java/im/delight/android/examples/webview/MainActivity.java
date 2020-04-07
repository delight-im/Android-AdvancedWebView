package im.delight.android.examples.webview;

import android.webkit.WebChromeClient;
import android.widget.Toast;
import android.webkit.WebView;
import android.view.View;
import android.webkit.WebViewClient;
import android.graphics.Bitmap;
import android.content.Intent;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.app.Activity;

import com.advancewebview.AdvancedWebView;
import com.advancewebview.HeaderObj;
import com.advancewebview.WebViewActivity;

import java.util.ArrayList;

public class MainActivity extends WebViewActivity implements AdvancedWebView.Listener {

	private static final String TEST_PAGE_URL = "https://whoops.ko.edu.vn/student-message/get-attach-file/1134?updated";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_AppCompat);
		webViewUrl = TEST_PAGE_URL;
		super.onCreate(savedInstanceState);

	}



}
