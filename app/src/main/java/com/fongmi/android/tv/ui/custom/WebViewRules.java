package com.fongmi.android.tv.ui.custom;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;

import com.fongmi.android.tv.api.config.RuleConfig;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.regex.Pattern;

final class WebViewRules {

    private WebViewRules() {
    }

    static boolean isAd(Uri uri) {
        if (uri == null) return false;
        String host = uri.getHost();
        if (TextUtils.isEmpty(host)) return false;
        host = host.toLowerCase(java.util.Locale.ROOT);
        List<String> rules = RuleConfig.getAds();
        for (String rule : rules) {
            if (TextUtils.isEmpty(rule)) continue;
            String value = rule.trim();
            if (value.isEmpty()) continue;
            if (host.contains(value.toLowerCase(java.util.Locale.ROOT))) return true;
            try {
                if (Pattern.compile(value, Pattern.CASE_INSENSITIVE).matcher(host).find()) return true;
            } catch (RuntimeException ignored) {
                // A malformed regex must not break WebView resource loading.
            }
        }
        return false;
    }

    static WebResourceResponse empty() {
        // Return a fresh response for every intercepted request (the body stream is consumed).
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
    }
}
