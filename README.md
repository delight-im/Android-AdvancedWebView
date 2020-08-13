# AdvancedWebView

Enhanced WebView component for Android that works as intended out of the box

## Requirements

 * Android 2.2+

## Installation

 * Add this library to your project
   * Declare the Gradle repository in your root `build.gradle`

     ```gradle
     allprojects {
         repositories {
             maven { url "https://jitpack.io" }
         }
     }
     ```

   * Declare the Gradle dependency in your app module's `build.gradle`

     ```gradle
     dependencies {
         implementation 'com.github.delight-im:Android-AdvancedWebView:v3.2.1'
     }
     ```

## Usage

### AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Layout (XML)

```xml
<im.delight.android.webview.AdvancedWebView
    android:id="@+id/webview"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### Activity (Java)

#### Without Fragments

```java
public class MyActivity extends Activity implements AdvancedWebView.Listener {

    private AdvancedWebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        mWebView = (AdvancedWebView) findViewById(R.id.webview);
        mWebView.setListener(this, this);
        mWebView.setMixedContentAllowed(false);
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
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) { }

    @Override
    public void onExternalPageRequest(String url) { }

}
```

#### With Fragments (`android.app.Fragment`)

**Note:** If you're using the `Fragment` class from the support library (`android.support.v4.app.Fragment`), please refer to the next section (see below) instead of this one.

```java
public class MyFragment extends Fragment implements AdvancedWebView.Listener {

    private AdvancedWebView mWebView;

    public MyFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my, container, false);

        mWebView = (AdvancedWebView) rootView.findViewById(R.id.webview);
        mWebView.setListener(this, this);
        mWebView.setMixedContentAllowed(false);
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
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) { }

    @Override
    public void onExternalPageRequest(String url) { }

}
```

#### With Fragments from the support library (`android.support.v4.app.Fragment`)

 * Use the code for normal `Fragment` usage as shown above
 * Change

   ```java
   mWebView.setListener(this, this);
   ```

   to

   ```java
   mWebView.setListener(getActivity(), this);
   ```

 * Add the following code to the parent `FragmentActivity` in order to forward the results from the `FragmentActivity` to the appropriate `Fragment` instance

   ```java
   public class MyActivity extends FragmentActivity implements AdvancedWebView.Listener {

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (mFragment != null) {
            mFragment.onActivityResult(requestCode, resultCode, intent);
        }
    }

   }
   ```

### ProGuard (if enabled)

```
-keep class * extends android.webkit.WebChromeClient { *; }
-dontwarn im.delight.android.webview.**
```

### Cleartext (non-HTTPS) traffic

If you want to serve sites or just single resources over plain `http` instead of `https`, there’s usually nothing to do if you’re targeting Android 8.1 (API level 27) or earlier. On Android 9 (API level 28) and later, however, [cleartext support is disabled by default](https://developer.android.com/training/articles/security-config). You may have to set `android:usesCleartextTraffic="true"` on the `<application>` element in `AndroidManifest.xml` or provide a custom [network security configuration](https://developer.android.com/training/articles/security-config).

## Features

 * Optimized for best performance and security
 * Features are patched across Android versions
 * File uploads are handled automatically (check availability with `AdvancedWebView.isFileUploadAvailable()`)
   * Multiple file uploads via single input fields (`multiple` attribute in HTML) are supported on Android 5.0+. The application that is used to pick the files (i.e. usually a gallery or file manager app) must provide controls for selecting multiple files, which some apps don't.
 * JavaScript and WebStorage are enabled by default
 * Includes localizations for the 25 most widely spoken languages
 * Receive callbacks when pages start/finish loading or have errors

   ```java
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

   ```java
   @Override
   public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {
       // some file is available for download
       // either handle the download yourself or use the code below

       if (AdvancedWebView.handleDownload(this, url, suggestedFilename)) {
           // download successfully handled
       }
       else {
           // download couldn't be handled because user has disabled download manager app on the device
           // TODO show some notice to the user
       }
   }
   ```

 * Enable geolocation support (needs `<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />`)

   ```java
   mWebView.setGeolocationEnabled(true);
   ```

 * Add custom HTTP headers in addition to the ones sent by the web browser implementation

   ```java
   mWebView.addHttpHeader("X-Requested-With", "My wonderful app");
   ```

 * Define a custom set of permitted hostnames and receive callbacks for all other hostnames

   ```java
   mWebView.addPermittedHostname("example.org");
   ```

   and

   ```java
   @Override
   public void onExternalPageRequest(String url) {
       // the user tried to open a page from a non-permitted hostname
   }
   ```

 * Prevent caching of HTML pages

   ```java
   boolean preventCaching = true;
   mWebView.loadUrl("http://www.example.org/", preventCaching);
   ```

 * Check for alternative browsers installed on the device

   ```java
   if (AdvancedWebView.Browsers.hasAlternative(this)) {
       AdvancedWebView.Browsers.openUrl(this, "http://www.example.org/");
   }
   ```

 * Disable cookies

   ```java
   // disable third-party cookies only
   mWebView.setThirdPartyCookiesEnabled(false);
   // or disable cookies in general
   mWebView.setCookiesEnabled(false);
   ```

 * Allow or disallow (both passive and active) mixed content (HTTP content being loaded inside HTTPS sites)

   ```java
   mWebView.setMixedContentAllowed(true);
   // or
   mWebView.setMixedContentAllowed(false);
   ```

 * Switch between mobile and desktop mode

   ```java
   mWebView.setDesktopMode(true);
   // or
   // mWebView.setDesktopMode(false);
   ```

 * Load HTML file from “assets” (e.g. at `app/src/main/assets/html/index.html`)

   ```java
   mWebView.loadUrl("file:///android_asset/html/index.html");
   ```

 * Load HTML file from SD card

   ```java
   // <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

   if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
       mWebView.getSettings().setAllowFileAccess(true);
       mWebView.loadUrl("file:///sdcard/Android/data/com.my.app/my_folder/index.html");
   }
   ```

 * Load HTML source text and display as page

   ```java
   myWebView.loadHtml("<html>...</html>");

   // or

   final String myBaseUrl = "http://www.example.com/";
   myWebView.loadHtml("<html>...</html>", myBaseUrl);
   ```

 * Enable multi-window support

   ```java
   myWebView.getSettings().setSupportMultipleWindows(true);
   // myWebView.getSettings().setJavaScriptEnabled(true);
   // myWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

   myWebView.setWebChromeClient(new WebChromeClient() {

       @Override
       public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
           AdvancedWebView newWebView = new AdvancedWebView(MyNewActivity.this);
           // myParentLayout.addView(newWebView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
           WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
           transport.setWebView(newWebView);
           resultMsg.sendToTarget();

           return true;
       }

   }
   ```

## Contributing

All contributions are welcome! If you wish to contribute, please create an issue first so that your feature, problem or question can be discussed.

## License

This project is licensed under the terms of the [MIT License](https://opensource.org/licenses/MIT).
