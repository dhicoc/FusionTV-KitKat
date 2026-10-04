package com.fongmi.android.tv.player.extractor;

import com.fongmi.android.tv.player.Source;

public class JianPian implements Source.Extractor {

    @Override
    public boolean match(String scheme, String host) {
        return false;
    }

    @Override
    public String fetch(String url) {
        return url;
    }

    @Override
    public void stop() {
    }

    @Override
    public void exit() {
    }
}
