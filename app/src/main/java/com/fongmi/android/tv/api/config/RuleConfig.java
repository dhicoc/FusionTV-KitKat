package com.fongmi.android.tv.api.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Combines ad rules from the currently loaded VOD and live configurations. */
public final class RuleConfig {

    private RuleConfig() {
    }

    public static List<String> getAds() {
        Map<String, String> rules = new LinkedHashMap<>();
        addRules(rules, VodConfig.get().getAds());
        addRules(rules, LiveConfig.get().getAds());
        return new ArrayList<>(rules.values());
    }

    private static void addRules(Map<String, String> target, List<String> source) {
        if (source == null) return;
        for (String rule : source) {
            if (rule == null || rule.trim().isEmpty()) continue;
            String value = rule.trim();
            target.put(value.toLowerCase(Locale.ROOT), value);
        }
    }
}
