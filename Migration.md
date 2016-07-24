# Migration

## From `v2.x.x` to `v3.x.x`

 * The license has been changed from the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0) to the [MIT License](https://opensource.org/licenses/MIT).
 * The signature of `AdvancedWebView.Listener#onDownloadRequested` has changed from

   ```java
   void onDownloadRequested(String url, String userAgent, String contentDisposition, String mimetype, long contentLength)
   ```

   to

   ```java
   void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent)
   ```
