package com.fongmi.android.tv.player.extractor;

import com.fongmi.android.tv.player.Source;

public class Force implements Source.Extractor {

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
