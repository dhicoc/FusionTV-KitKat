package com.fongmi.android.tv.ui.custom;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.fongmi.android.tv.api.config.RuleConfig;

import java.io.ByteArrayInputStream;
import java.util.regex.Pattern;

final class WebViewRules {

    private WebViewRules() {
    }

    static boolean isAd(Uri uri) {
        if (uri == null || TextUtils.isEmpty(uri.getHost())) return false;
        String host = uri.getHost().toLowerCase();
        for (String rule : RuleConfig.getAds()) {
            if (TextUtils.isEmpty(rule)) continue;
            String value = rule.trim();
            if (host.contains(value)) return true;
            try {
                if (Pattern.compile(value, Pattern.CASE_INSENSITIVE).matcher(host).find()) return true;
            } catch (Exception ignored) {
                // Ignore invalid regular expressions and keep literal matching useful.
            }
        }
        return false;
    }

    static WebResourceResponse empty() {
        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
    }
}
