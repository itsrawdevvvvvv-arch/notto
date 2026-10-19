package com.notto.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Base64;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Notto — a fast, offline notes app.
 *
 * The UI is a single self-contained HTML/CSS/JS document served from the app
 * assets. This activity hosts it and bridges the few things a WebView cannot do
 * by itself: saving exports, speech to text, audio recording, reminders and the
 * home-screen widgets.
 */
public class MainActivity extends Activity {

    private static final int REQ_FILE = 1001;
    private static final int REQ_MIC = 1002;
    private static final int REQ_NOTIF = 1003;
    private static final String BG = "#0b0b0b";
    static final String CHANNEL = "notto.reminders";
    static final String PREFS = "notto.prefs";

    private WebView web;
    private ValueCallback<Uri[]> filePathCallback;
    private SpeechRecognizer recognizer;
    private MediaRecorder recorder;
    private File recFile;
    private boolean pendingSpeech = false;
    private boolean pendingRec = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.setStatusBarColor(Color.parseColor(BG));
        w.setNavigationBarColor(Color.parseColor(BG));
        ensureChannel(this);

        web = new NottoWebView(this);
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
        s.setLoadWithOverviewMode(false);
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
                    String type = "*/*";
                    String[] accept = params.getAcceptTypes();
                    if (accept != null) {
                        for (String a : accept) {
                            if (a != null && a.indexOf('/') > 0) {
                                type = a;
                                break;
                            }
                        }
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

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            request.grant(request.getResources());
                        } catch (Exception ignored) {
                        }
                    }
                });
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            web.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    int top = 0, bottom = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                        top = bars.top;
                        bottom = bars.bottom;
                    } else {
                        top = insets.getSystemWindowInsetTop();
                        bottom = insets.getSystemWindowInsetBottom();
                    }
                    setCssInsets(top, bottom);
                    return insets;
                }
            });
        }

        web.addJavascriptInterface(new Bridge(), "NottoBridge");
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    /**
     * WebView that keeps Android's floating text toolbar hidden while leaving
     * text selection itself completely intact: instead of returning null (which
     * aborts Chromium's selection flow), we hand back a no-op ActionMode whose
     * menu is never shown.
     */
    private class NottoWebView extends WebView {
        NottoWebView(Context context) {
            super(context);
        }

        @Override
        public ActionMode startActionMode(ActionMode.Callback callback, int type) {
            if (type == ActionMode.TYPE_FLOATING) {
                return ghost(callback);
            }
            return super.startActionMode(callback, type);
        }

        @Override
        public ActionMode startActionMode(ActionMode.Callback callback) {
            return ghost(callback);
        }
    }

    private ActionMode ghost(ActionMode.Callback cb) {
        GhostActionMode g = new GhostActionMode(cb);
        try {
            if (cb != null) {
                cb.onCreateActionMode(g, g.getMenu());
            }
        } catch (Exception ignored) {
        }
        return g;
    }

    private class GhostActionMode extends ActionMode {
        private final Menu menu;

        GhostActionMode(ActionMode.Callback cb) {
            Menu m;
            try {
                m = new PopupMenu(MainActivity.this, web).getMenu();
            } catch (Exception e) {
                m = null;
            }
            this.menu = m;
        }

        @Override public void setTitle(CharSequence title) { }
        @Override public void setTitle(int resId) { }
        @Override public void setSubtitle(CharSequence subtitle) { }
        @Override public void setSubtitle(int resId) { }
        @Override public void setCustomView(View view) { }
        @Override public void invalidate() { }
        @Override public void finish() { }
        @Override public Menu getMenu() { return menu; }
        @Override public CharSequence getTitle() { return ""; }
        @Override public CharSequence getSubtitle() { return ""; }
        @Override public View getCustomView() { return null; }
        @Override public MenuInflater getMenuInflater() { return MainActivity.this.getMenuInflater(); }
    }

    private void setCssInsets(int topPx, int bottomPx) {
        final float d = getResources().getDisplayMetrics().density;
        final String js = String.format(Locale.US,
                "document.documentElement.style.setProperty('--sat','%.1fpx');"
                        + "document.documentElement.style.setProperty('--sab','%.1fpx');",
                topPx / d, bottomPx / d);
        if (web != null) {
            web.post(new Runnable() {
                @Override
                public void run() {
                    web.evaluateJavascript(js, null);
                }
            });
        }
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingSpeech) {
                pendingSpeech = false;
                new Bridge().startSpeech();
            }
            if (pendingRec) {
                pendingRec = false;
                new Bridge().startRecording();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        if (recorder != null) {
            try {
                recorder.release();
            } catch (Exception ignored) {
            }
            recorder = null;
        }
        super.onDestroy();
    }

    // ------------------------------------------------------------------ bridge

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

        // ---- permissions ----

        @JavascriptInterface
        public void requestMic() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            }
        }

        @JavascriptInterface
        public void requestNotif() {
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIF);
            }
        }

        // ---- speech to text (Android's built-in recogniser — free, no account) ----

        @JavascriptInterface
        public void startSpeech() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!SpeechRecognizer.isRecognitionAvailable(MainActivity.this)) {
                        speechBack("error", "unavailable");
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {
                        pendingSpeech = true;
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
                        return;
                    }
                    if (recognizer != null) {
                        try { recognizer.cancel(); } catch (Exception ignored) {}
                        try { recognizer.destroy(); } catch (Exception ignored) {}
                        recognizer = null;
                    }
                    recognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
                    recognizer.setRecognitionListener(new Listener());
                    Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                    i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                    try {
                        recognizer.startListening(i);
                        speechBack("start", "");
                    } catch (Exception e) {
                        speechBack("error", "failed");
                    }
                }
            });
        }

        @JavascriptInterface
        public void stopSpeech() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (recognizer != null) {
                        try {
                            recognizer.stopListening();
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
        }

        // ---- audio recording (native MediaRecorder: reliable inside a WebView) ----

        @JavascriptInterface
        public void startRecording() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {
                        pendingRec = true;
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
                        return;
                    }
                    try {
                        recFile = new File(getCacheDir(),
                                "notto-rec-" + System.currentTimeMillis() + ".m4a");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            recorder = new MediaRecorder(MainActivity.this);
                        } else {
                            recorder = new MediaRecorder();
                        }
                        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        recorder.setAudioChannels(1);
                        recorder.setAudioSamplingRate(44100);
                        recorder.setAudioEncodingBitRate(64000);
                        recorder.setOutputFile(recFile.getAbsolutePath());
                        recorder.prepare();
                        recorder.start();
                        recBack("start", "");
                    } catch (Exception e) {
                        cleanupRec();
                        recBack("error", "failed");
                    }
                }
            });
        }

        @JavascriptInterface
        public void stopRecording() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (recorder == null) {
                        return;
                    }
                    try {
                        recorder.stop();
                    } catch (Exception e) {
                        cleanupRec();
                        recBack("error", "failed");
                        return;
                    }
                    try {
                        recorder.release();
                    } catch (Exception ignored) {
                    }
                    recorder = null;
                    try {
                        FileInputStream in = new FileInputStream(recFile);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = in.read(buf)) > 0) {
                            bos.write(buf, 0, r);
                        }
                        in.close();
                        String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
                        recBack("done", "data:audio/mp4;base64," + b64);
                    } catch (Exception e) {
                        recBack("error", "failed");
                    } finally {
                        cleanupRec();
                    }
                }
            });
        }

        // ---- keep the system bars matching the app background ----

        @JavascriptInterface
        public void setBars(final String color, final boolean light) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int c = Color.parseColor(color);
                        getWindow().setStatusBarColor(c);
                        getWindow().setNavigationBarColor(c);
                        if (web != null) {
                            web.setBackgroundColor(c);
                        }
                        View dv = getWindow().getDecorView();
                        int flags = dv.getSystemUiVisibility();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            flags = light ? (flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
                                    : (flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            flags = light ? (flags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                                    : (flags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                        }
                        dv.setSystemUiVisibility(flags);
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        // ---- reminders ----

        @JavascriptInterface
        public void scheduleReminder(int hour, int minute) {
            getSharedPreferences(PREFS, 0).edit()
                    .putInt("remHour", hour).putInt("remMin", minute).putBoolean("remOn", true).apply();
            ReminderReceiver.schedule(MainActivity.this, hour, minute);
        }

        @JavascriptInterface
        public void cancelReminder() {
            getSharedPreferences(PREFS, 0).edit().putBoolean("remOn", false).apply();
            ReminderReceiver.cancel(MainActivity.this);
        }

        @JavascriptInterface
        public void testNotification() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ReminderReceiver.post(MainActivity.this, "Notto",
                            "Reminders are working. This is a test.");
                    toast("Test notification sent");
                }
            });
        }

        // ---- home-screen widgets ----

        @JavascriptInterface
        public void updateWidget(String json) {
            WidgetData.set(MainActivity.this, json);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshWidgets(MainActivity.this);
                }
            });
        }
    }

    private void cleanupRec() {
        try {
            if (recorder != null) {
                recorder.release();
            }
        } catch (Exception ignored) {
        }
        recorder = null;
        if (recFile != null && recFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            recFile.delete();
        }
        recFile = null;
    }

    private class Listener implements RecognitionListener {
        @Override public void onReadyForSpeech(Bundle p) {}
        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float v) {}
        @Override public void onBufferReceived(byte[] b) {}
        @Override public void onEndOfSpeech() {}
        @Override public void onEvent(int t, Bundle p) {}
        @Override public void onPartialResults(Bundle b) {
            ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (r != null && !r.isEmpty()) speechBack("partial", r.get(0));
        }
        @Override public void onResults(Bundle b) {
            ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            speechBack("final", (r != null && !r.isEmpty()) ? r.get(0) : "");
        }
        @Override public void onError(int e) {
            speechBack("error", e == SpeechRecognizer.ERROR_NO_MATCH ? "nomatch" : "failed");
        }
    }

    private void speechBack(String type, String text) {
        js("window.__speech&&window.__speech(" + JSONObject.quote(type) + ","
                + JSONObject.quote(text == null ? "" : text) + ")");
    }

    private void recBack(String type, String data) {
        js("window.__rec&&window.__rec(" + JSONObject.quote(type) + ","
                + JSONObject.quote(data == null ? "" : data) + ")");
    }

    private void js(final String code) {
        if (web != null) {
            web.post(new Runnable() {
                @Override
                public void run() {
                    web.evaluateJavascript(code, null);
                }
            });
        }
    }

    static void refreshWidgets(Context c) {
        Class<?>[] providers = {NottoWidgetProvider.class, ChecklistWidgetProvider.class,
                BoardWidgetProvider.class};
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        if (mgr == null) {
            return;
        }
        for (Class<?> p : providers) {
            try {
                int[] ids = mgr.getAppWidgetIds(new ComponentName(c, p));
                if (ids == null || ids.length == 0) {
                    continue;
                }
                Intent i = new Intent(c, p);
                i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                c.sendBroadcast(i);
            } catch (Exception ignored) {
            }
        }
    }

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "Reminders",
                        NotificationManager.IMPORTANCE_DEFAULT);
                ch.setDescription("Daily reminders to write in Notto");
                nm.createNotificationChannel(ch);
            }
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
