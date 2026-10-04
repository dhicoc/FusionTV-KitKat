package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.impl.ParseCallback;
import com.fongmi.android.tv.ui.dialog.WebDialog;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.crawler.Spider;
import com.google.common.net.HttpHeaders;
import com.orhanobut.logger.Logger;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CustomWebView extends WebView implements DialogInterface.OnDismissListener {

    private static final String TAG = CustomWebView.class.getSimpleName();
    private static final String BLANK = "about:blank";

    private Map<String, String> headers;
    private WebResourceResponse empty;
    private ParseCallback callback;
    private WebDialog dialog;
    private Runnable timer;
    private boolean detect;
    private String click;
    private String from;
    private String key;

    public static CustomWebView create(@NonNull Context context) {
        return new CustomWebView(context);
    }

    public CustomWebView(@NonNull Context context) {
        super(context);
        initSettings();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initSettings() {
        this.timer = () -> stop(true);
        this.empty = new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
        getSettings().setSupportZoom(true);
        getSettings().setUseWideViewPort(true);
        getSettings().setDatabaseEnabled(true);
        getSettings().setBlockNetworkImage(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setBuiltInZoomControls(true);
        getSettings().setDisplayZoomControls(false);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setUserAgentString(Setting.getUa());
        getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        setWebViewClient(webViewClient());
        setWebChromeClient(webChromeClient());
    }

    public CustomWebView start(String key, String from, Map<String, String> headers, String url, String click, ParseCallback callback, boolean detect) {
        App.post(timer, Constant.TIMEOUT_PARSE_WEB);
        this.callback = callback;
        this.headers = headers;
        this.detect = detect;
        this.click = click;
        this.from = from;
        this.key = key;
        start(url, headers);
        return this;
    }

    private void start(String url, Map<String, String> headers) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
        checkHeader(url, headers);
        loadUrl(url, headers);
    }

    private void checkHeader(String url, Map<String, String> headers) {
        for (String key : headers.keySet()) {
            if (HttpHeaders.USER_AGENT.equalsIgnoreCase(key)) getSettings().setUserAgentString(headers.get(key));
        }
    }

    private WebViewClient webViewClient() {
        return new WebViewClient() {
            @Override
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                String host = request.getUrl().getHost();
                if (TextUtils.isEmpty(host) || isAd(host)) return empty;
                Map<String, String> headers = request.getRequestHeaders();
                if (url.contains("challenges.cloudflare.com/turnstile")) App.post(() -> showDialog());
                if (detect && url.contains("player/?url=")) onParseAdd(headers, url);
                else if (isVideoFormat(url)) onParseSuccess(headers, url);
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                String host = UrlUtil.host(url);
                if (TextUtils.isEmpty(host) || isAd(host)) return empty;
                if (url.contains("challenges.cloudflare.com/turnstile")) App.post(() -> showDialog());
                if (detect && url.contains("player/?url=")) onParseAdd(headers, url);
                else if (isVideoFormat(url)) onParseSuccess(headers, url);
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.equals(BLANK)) return;
                evaluate(getScript(url));
            }

            @Override
            @SuppressLint("WebViewClientOnReceivedSslError")
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        };
    }

    private WebChromeClient webChromeClient() {
        return new WebChromeClient() {
            @Override
            public Bitmap getDefaultVideoPoster() {
                try {
                    return BitmapFactory.decodeResource(App.get().getResources(), R.drawable.ic_logo);
                } catch (Throwable e) {
                    return super.getDefaultVideoPoster();
                }
            }
        };
    }

    private void showDialog() {
        if (dialog != null || App.activity() == null) return;
        if (getParent() != null) ((ViewGroup) getParent()).removeView(this);
        dialog = new WebDialog(this).show();
        App.removeCallbacks(timer);
    }

    private void hideDialog() {
        if (dialog != null) dialog.dismiss();
        dialog = null;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        stop(true);
    }

    private List<String> getScript(String url) {
        List<String> script = new ArrayList<>(Sniffer.getScript(Uri.parse(url)));
        if (TextUtils.isEmpty(click) || script.contains(click)) return script;
        script.add(0, click);
        return script;
    }

    private void evaluate(List<String> script) {
        if (script.isEmpty()) return;
        if (TextUtils.isEmpty(script.get(0))) {
            evaluate(script.subList(1, script.size()));
        } else {
            loadUrl("javascript:" + script.get(0));
        }
    }

    private boolean isAd(String host) {
        for (String ad : VodConfig.get().getAds()) if (host.contains(ad)) return true;
        for (String ad : LiveConfig.get().getAds()) if (host.contains(ad)) return true;
        for (String ad : VodConfig.get().getAds()) if (Pattern.compile(ad).matcher(host).find()) return true;
        for (String ad : LiveConfig.get().getAds()) if (Pattern.compile(ad).matcher(host).find()) return true;
        return false;
    }

    private boolean isVideoFormat(String url) {
        try {
            Logger.t(TAG).d(url);
            Spider spider = VodConfig.get().getSite(key).spider();
            if (spider.manualVideoCheck()) return spider.isVideoFormat(url);
            return Sniffer.isVideoFormat(url);
        } catch (Exception ignored) {
            return Sniffer.isVideoFormat(url);
        }
    }

    private void onParseAdd(Map<String, String> headers, String url) {
        App.post(() -> CustomWebView.create(App.get()).start(key, from, headers, url, click, callback, false));
    }

    private void onParseSuccess(Map<String, String> headers, String url) {
        if (callback != null) callback.onParseSuccess(headers, url, from);
        App.post(() -> stop(false));
        callback = null;
    }

    private void onParseError() {
        if (callback != null) callback.onParseError();
        callback = null;
    }

    public void stop(boolean error) {
        hideDialog();
        stopLoading();
        loadUrl(BLANK);
        App.removeCallbacks(timer);
        if (error) onParseError();
        else callback = null;
    }
}
