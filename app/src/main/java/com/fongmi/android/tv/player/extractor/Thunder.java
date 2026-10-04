package com.fongmi.android.tv.player.extractor;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.player.Source;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class Thunder implements Source.Extractor {

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

    public static class Parser implements Callable<List<Episode>> {

        public static boolean match(String url) {
            return false;
        }

        public static Parser get(String url) {
            return new Parser();
        }

        @Override
        public List<Episode> call() {
            return Collections.emptyList();
        }
    }
}
