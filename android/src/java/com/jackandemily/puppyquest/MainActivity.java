package com.jackandemily.puppyquest;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Plays the Puppy Quest web game full screen from files packed inside the app.
 * Files are served from a fixed https address so saves (localStorage) and fetch() work
 * exactly like they do in a browser, with no internet needed.
 */
public class MainActivity extends Activity {
    static final String HOST = "appassets.androidplatform.net";
    static final String START = "https://" + HOST + "/index.html";

    private WebView web;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            // Draw under the camera cut-out on phones that have one (field added in Android 9).
            try {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.getClass().getField("layoutInDisplayCutoutMode").setInt(lp, 1);
                getWindow().setAttributes(lp);
            } catch (Exception ignored) {
            }
        }

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setTextZoom(100);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setBackgroundColor(0xff8fd3ff);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                if (!HOST.equals(req.getUrl().getHost())) return null;
                return asset(req.getUrl().getPath());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Stay inside the game; never open outside links.
                return !HOST.equals(android.net.Uri.parse(url).getHost());
            }
        });
        web.addJavascriptInterface(new PhotoBridge(), "PQAndroid");
        setContentView(web);
        hideSystemBars();
        if (state != null) web.restoreState(state);
        else web.loadUrl(START);
    }

    /** Lets the game's photo mode save a picture into the phone's Photos, in a "Puppy Quest" album. */
    class PhotoBridge {
        @JavascriptInterface
        public String savePhoto(String base64, String name) {
            try {
                if (Build.VERSION.SDK_INT < 29
                        && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        }
                    });
                    return "ask";
                }
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                String file = name.replaceAll("[^A-Za-z0-9._-]", "_");
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, file);
                v.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                if (Build.VERSION.SDK_INT >= 29) {
                    // MediaColumns.RELATIVE_PATH and IS_PENDING (Android 10), written by name because the build compiles against an older SDK.
                    v.put("relative_path", Environment.DIRECTORY_PICTURES + "/Puppy Quest");
                    v.put("is_pending", 1);
                } else {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Puppy Quest");
                    dir.mkdirs();
                    v.put(MediaStore.MediaColumns.DATA, new File(dir, file).getAbsolutePath());
                }
                ContentResolver cr = getContentResolver();
                Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                if (uri == null) return "error";
                OutputStream out = cr.openOutputStream(uri);
                if (out == null) return "error";
                try {
                    out.write(data);
                } finally {
                    out.close();
                }
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues done = new ContentValues();
                    done.put("is_pending", 0);
                    cr.update(uri, done, null, null);
                }
                return "ok";
            } catch (Exception e) {
                return "error";
            }
        }
    }

    private WebResourceResponse asset(String path) {
        if (path == null || path.equals("/")) path = "/index.html";
        if (path.contains("..")) return notFound();
        try {
            InputStream in = getAssets().open("www" + path);
            Map<String, String> headers = new HashMap<>();
            headers.put("Cache-Control", "no-cache");
            headers.put("Access-Control-Allow-Origin", "*");
            return new WebResourceResponse(mime(path), mime(path).startsWith("text") || path.endsWith(".js") || path.endsWith(".json") ? "utf-8" : null,
                    200, "OK", headers, in);
        } catch (IOException e) {
            return notFound();
        }
    }

    private static WebResourceResponse notFound() {
        return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", new HashMap<String, String>(), null);
    }

    private static String mime(String p) {
        p = p.toLowerCase();
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js")) return "application/javascript";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".woff2")) return "font/woff2";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".mp3")) return "audio/mpeg";
        if (p.endsWith(".ogg")) return "audio/ogg";
        return "application/octet-stream";
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Back opens or closes the pause menu; on the title screen it leaves the app running.
        web.evaluateJavascript("window.__pqBack?__pqBack():'exit'", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String r) {
                if (r == null || r.contains("exit")) moveTaskToBack(true);
            }
        });
    }

    @Override
    protected void onPause() {
        // The game saves itself when the page is hidden; also hush the music.
        web.evaluateJavascript("window.__pqPause&&__pqPause()", null);
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        web.evaluateJavascript("window.__pqResume&&__pqResume()", null);
        hideSystemBars();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
