package com.notto.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Notto — a fast, offline notes app.
 *
 * The whole UI is a single self-contained HTML/CSS/JS document served from the
 * app assets. This activity hosts it in a hardened WebView and bridges the few
 * web APIs Android WebView does not implement natively (file exports).
 */
public class MainActivity extends Activity {

    private static final int REQ_FILE = 1001;
    private static final String BG = "#0b0b0b";

    private WebView web;
    private ValueCallback<Uri[]> filePathCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.setStatusBarColor(Color.parseColor(BG));
        w.setNavigationBarColor(Color.parseColor(BG));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor(BG));

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.setSafeBrowsingEnabled(false);
        }

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String scheme = u.getScheme();
                if (scheme != null && (scheme.equals("http") || scheme.equals("https"))) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, u));
                    } catch (Exception ignored) {
                    }
                    return true;
                }
                return false;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                try {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    String[] accept = params.getAcceptTypes();
                    String type = "*/*";
                    if (accept != null && accept.length > 0 && accept[0] != null
                            && accept[0].trim().length() > 0) {
                        type = accept[0];
                    }
                    i.setType(type);
                    if (params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }
                    startActivityForResult(Intent.createChooser(i, "Select file"), REQ_FILE);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "NottoBridge");
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            Uri[] result = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int n = data.getClipData().getItemCount();
                    result = new Uri[n];
                    for (int i = 0; i < n; i++) {
                        result[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    result = new Uri[]{data.getData()};
                }
            }
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(result);
                filePathCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /** JS bridge: lets the web layer save exported files (PNG / PDF / TXT). */
    public class Bridge {
        @JavascriptInterface
        public void saveFile(String name, String dataUrl) {
            try {
                if (name == null || name.trim().isEmpty()) {
                    name = "notto-export";
                }
                if (dataUrl == null || !dataUrl.startsWith("data:")) {
                    toast("Export failed: bad data");
                    return;
                }
                int comma = dataUrl.indexOf(',');
                if (comma < 0) {
                    toast("Export failed: bad data");
                    return;
                }
                String mime = "application/octet-stream";
                String meta = dataUrl.substring(5, comma);
                int semi = meta.indexOf(';');
                if (semi > 0) {
                    mime = meta.substring(0, semi);
                } else if (meta.length() > 0) {
                    mime = meta;
                }
                byte[] bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    cv.put(MediaStore.Downloads.MIME_TYPE, mime);
                    cv.put(MediaStore.Downloads.IS_PENDING, 1);
                    Uri uri = getContentResolver()
                            .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri != null) {
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        if (os != null) {
                            os.write(bytes);
                            os.flush();
                            os.close();
                        }
                        cv.clear();
                        cv.put(MediaStore.Downloads.IS_PENDING, 0);
                        getContentResolver().update(uri, cv, null, null);
                        toast("Saved to Downloads: " + name);
                        return;
                    }
                }

                File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (dir == null) {
                    dir = getFilesDir();
                }
                File out = new File(dir, name);
                FileOutputStream fos = new FileOutputStream(out);
                fos.write(bytes);
                fos.flush();
                fos.close();
                toast("Saved: " + out.getAbsolutePath());
            } catch (Exception e) {
                toast("Export failed: " + e.getMessage());
            }
        }

        @JavascriptInterface
        public void toastMsg(String message) {
            toast(message);
        }
    }

    private void toast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
