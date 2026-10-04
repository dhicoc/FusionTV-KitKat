package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.impl.X5WebViewCallback;
import com.github.catvod.utils.Path;

import java.io.File;

public class Tbs {

    public static void init() {
    }

    public static String url() {
        return "";
    }

    public static File file() {
        return Path.cache("x5.tbs.apk");
    }

    public static void remove() {
        File file = file();
        if (file.exists()) file.delete();
    }

    public static void install(X5WebViewCallback callback) {
        if (callback != null) callback.onX5Error();
    }
}
