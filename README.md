# AdvancedWebView

Advanced WebView component for Android that works as intended out of the box

Works on Android 2.2+ (API level 8 and above)

## Installation

 * Include one of the [JARs](JARs) in your `libs` folder
 * or
 * Copy the Java package to your project's source folder
 * or
 * Create a new library project from this repository and reference it in your project

## Usage

### AndroidManifest.xml

```
<uses-permission android:name="android.permission.INTERNET" />
```

### Layout (XML)

```
<im.delight.android.webview.AdvancedWebView
	android:id="@+id/webview"
	android:layout_width="match_parent"
	android:layout_height="match_parent" />
```

### Activity (Java)

#### Without Fragments

```
public class MyActivity extends Activity implements AdvancedWebView.Listener {

	private AdvancedWebView mWebView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my);

		mWebView = (AdvancedWebView) findViewById(R.id.webview);
		mWebView.setListener(this, this);
		mWebView.loadUrl("http://www.example.org/");
		
		// ...
	}

	@SuppressLint("NewApi")
	@Override
	protected void onResume() {
		super.onResume();
		mWebView.onResume();
		// ...
	}

	@SuppressLint("NewApi")
	@Override
	protected void onPause() {
		mWebView.onPause();
		// ...
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mWebView.onDestroy();
		// ...
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		mWebView.onActivityResult(requestCode, resultCode, intent);
		// ...
	}

	@Override
	public void onBackPressed() {
		if (!mWebView.onBackPressed()) { return; }
		// ...
		super.onBackPressed();
	}

	@Override
	public void onPageStarted(String url, Bitmap favicon) { }

	@Override
	public void onPageFinished(String url) { }

	@Override
	public void onPageError(int errorCode, String description, String failingUrl) { }

	@Override
	public void onDownloadRequested(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) { }

	@Override
	public void onExternalPageRequest(String url) { }

}
```

#### With Fragments

```
public class MyFragment extends Fragment implements AdvancedWebView.Listener {

	private AdvancedWebView mWebView;

	public MyFragment() { }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_my, container, false);

        mWebView = (AdvancedWebView) rootView.findViewById(R.id.webview);
        mWebView.setListener(this, this);
        mWebView.loadUrl("http://www.example.org/");
		
		// ...

		return rootView;
	}

	@SuppressLint("NewApi")
	@Override
	public void onResume() {
		super.onResume();
		mWebView.onResume();
		// ...
	}

	@SuppressLint("NewApi")
	@Override
	public void onPause() {
		mWebView.onPause();
		// ...
		super.onPause();
	}

	@Override
	public void onDestroy() {
		mWebView.onDestroy();
		// ...
		super.onDestroy();
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mWebView.onActivityResult(requestCode, resultCode, intent);
        // ...
    }

	@Override
	public void onPageStarted(String url, Bitmap favicon) { }

	@Override
	public void onPageFinished(String url) { }

	@Override
	public void onPageError(int errorCode, String description, String failingUrl) { }

	@Override
	public void onDownloadRequested(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) { }

	@Override
	public void onExternalPageRequest(String url) { }

}
```

### ProGuard (if enabled)

```
-keep class * extends android.webkit.WebChromeClient { *; }
-dontwarn im.delight.android.webview.**
```

## Features

 * Optimized for best performance and security
 * Features are patched across Android versions
 * File uploads are handled automatically (check availability with `AdvancedWebView.isFileUploadAvailable()`)
 * JavaScript and WebStorage are enabled by default
 * Includes localizations for the 25 most widely spoken languages
 * Receive callbacks when pages start/finish loading or have errors

   ```
   @Override
   public void onPageStarted(String url, Bitmap favicon) {
       // a new page started loading
   }

   @Override
   public void onPageFinished(String url) {
       // the new page finished loading
   }

   @Override
   public void onPageError(int errorCode, String description, String failingUrl) {
       // the new page failed to load
   }
   ```

 * Downloads are handled automatically and can be listened to

   ```
   @Override
   public void onDownloadRequested(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
       // some file is available for download
   }
   ```

 * Enable geolocation support (needs `<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />`)

   ```
   mWebView.setGeolocationEnabled(true);
   ```

 * Add custom HTTP headers in addition to the ones sent by the web browser implementation

   ```
   mWebView.addHttpHeader("X-Requested-With", "My wonderful app");
   ```

 * Define a custom set of permitted hostnames and receive callbacks for all other hostnames

   ```
   mWebView.addPermittedHostname("www.example.org");
   ```

   and

   ```
   @Override
   public void onExternalPageRequest(String url) {
       // the user tried to open a page from a non-permitted hostname
   }
   ```

 * Prevent caching of HTML pages

   ```
   boolean preventCaching = true;
   mWebView.loadUrl("http://www.example.org/", preventCaching);
   ```

 * Check for alternative browsers installed on the device

   ```
   if (AdvancedWebView.Browsers.hasAlternative(this)) {
       AdvancedWebView.Browsers.openUrl(this, "http://www.example.org/");
   }
   ```

 * Disable cookies

   ```
   // disable third-party cookies only
   mWebView.setThirdPartyCookiesEnabled(false);
   // or disable cookies in general
   mWebView.setCookiesEnabled(false);
   ```

 * Disallow mixed content (HTTP content being loaded inside HTTPS sites)

   ```
   mWebView.setMixedContentAllowed(false);
   ```

## Dependencies

 * Android 2.2+

## Contributing

All contributions are welcome! If you wish to contribute, please create an issue first so that your feature, problem or question can be discussed.

## License

```
Copyright 2015 delight.im <info@delight.im>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
