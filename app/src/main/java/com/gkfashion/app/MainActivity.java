package com.gkfashion.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MainActivity extends Activity {

    private static final String WEBSITE_URL = "https://gkfashion.in";
    private static final String WEBSITE_HOST = "gkfashion.in";
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final long SPLASH_MIN_MS = 1500L;
    private static final long SPLASH_MAX_MS = 6000L;

    /* Put only your Razorpay Key ID here. Never put the Key Secret in the APK. */
    private static final String RAZORPAY_KEY_ID = "YOUR_RAZORPAY_KEY_ID";

    private WebView webView;
    private ImageView splash;
    private ValueCallback<Uri[]> fileCallback;
    private Object razorpay;
    private Class<?> razorpayClass;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long splashStartTime;
    private boolean pageLoaded = false;
    private boolean splashHidden = false;

    private final Runnable splashTimeout = () -> hideSplash();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Android 15/16: handle system-bar insets ourselves so the WebView
           never goes underneath the status/navigation bars. */
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.parseColor("#7A001E"));
        getWindow().setNavigationBarColor(Color.WHITE);

        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        webView = findViewById(R.id.webview);
        splash = findViewById(R.id.splash);
        splashStartTime = System.currentTimeMillis();

        setupWebView();

        /* Safety timeout: even if the website is slow, the splash cannot
           remain forever. Normally it hides as soon as the page is ready. */
        handler.postDelayed(splashTimeout, SPLASH_MAX_MS);
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);

        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pageLoaded = true;
                hideSplashWhenReady();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                fileCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            openExternal(url);
        });

        initializeRazorpayWebView();
        webView.loadUrl(WEBSITE_URL);
    }

    private void hideSplashWhenReady() {
        long elapsed = System.currentTimeMillis() - splashStartTime;
        long remaining = Math.max(0L, SPLASH_MIN_MS - elapsed);
        handler.postDelayed(this::hideSplash, remaining);
    }

    private void hideSplash() {
        if (splashHidden || splash == null) return;
        splashHidden = true;
        handler.removeCallbacks(splashTimeout);
        splash.setVisibility(View.GONE);
        getWindow().setNavigationBarColor(Color.WHITE);
    }

    /* Official Razorpay WebView UPI Intent SDK is supplied as a JAR by
       Razorpay and is placed in app/libs. Reflection keeps this base project
       buildable before that optional JAR is copied. */
    private void initializeRazorpayWebView() {
        if (RAZORPAY_KEY_ID.equals("YOUR_RAZORPAY_KEY_ID")) return;

        try {
            razorpayClass = Class.forName("com.razorpay.Razorpay");
            Constructor<?> constructor = razorpayClass.getConstructor(
                    String.class, WebView.class, Activity.class
            );
            razorpay = constructor.newInstance(RAZORPAY_KEY_ID, webView, this);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Razorpay UPI SDK अभी setup नहीं है", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean handleUrl(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

        if (("http".equals(scheme) || "https".equals(scheme))
                && (host.equals(WEBSITE_HOST) || host.endsWith("." + WEBSITE_HOST))) {
            return false;
        }

        if ("intent".equals(scheme)) {
            return openIntentUri(uri.toString());
        }

        if ("upi".equals(scheme) || "phonepe".equals(scheme)
                || "paytmmp".equals(scheme) || "gpay".equals(scheme)
                || "tez".equals(scheme) || "bhim".equals(scheme)
                || "whatsapp".equals(scheme) || "mailto".equals(scheme)
                || "tel".equals(scheme)) {
            openExternal(uri.toString());
            return true;
        }

        openExternal(uri.toString());
        return true;
    }

    private boolean openIntentUri(String url) {
        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setComponent(null);
            intent.setSelector(null);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                return true;
            }

            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
            if (fallbackUrl != null && !fallbackUrl.isEmpty()) {
                webView.loadUrl(fallbackUrl);
            }
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "पेमेंट ऐप उपलब्ध नहीं है", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "इस link को खोलने के लिए app नहीं मिला", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (fileCallback != null) {
                Uri[] results = null;
                if (resultCode == RESULT_OK && data != null) {
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = data.getClipData().getItemAt(i).getUri();
                        }
                    } else if (data.getData() != null) {
                        results = new Uri[]{data.getData()};
                    }
                }
                fileCallback.onReceiveValue(results);
                fileCallback = null;
            }
            return;
        }

        if (razorpay != null && razorpayClass != null) {
            try {
                Method method = razorpayClass.getMethod(
                        "onActivityResult", int.class, int.class, Intent.class
                );
                method.invoke(razorpay, requestCode, resultCode, data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
