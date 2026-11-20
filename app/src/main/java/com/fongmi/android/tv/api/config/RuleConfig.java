package com.fongmi.android.tv.api.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Shared rule helpers for ad filtering. */
public final class RuleConfig {

    private RuleConfig() {
    }

    public static List<String> getAds() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.addAll(VodConfig.get().getAds());
        result.addAll(LiveConfig.get().getAds());
        return new ArrayList<>(result);
    }
}
