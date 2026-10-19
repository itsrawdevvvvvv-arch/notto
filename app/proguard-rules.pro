# Keep the WebView JS bridge methods; they are called from JavaScript.
-keepclassmembers class com.notto.app.MainActivity$Bridge {
    public *;
}
