package com.fongmi.android.tv.api.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Combines bundled defaults with ad rules from the loaded VOD and live configurations. */
public final class RuleConfig {

    /** Common ad-serving domains; keep entries specific to avoid blocking normal site content. */
    private static final String[] DEFAULT_ADS = {
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "adservice.google.com",
            "adnxs.com",
            "adsrvr.org",
            "adform.net",
            "amazon-adsystem.com",
            "criteo.com",
            "taboola.com",
            "outbrain.com",
            "serving-sys.com",
            "smartadserver.com",
            "scorecardresearch.com",
            "ad.youku.com",
            "atm.youku.com",
            "ad.api.3g.youku.com",
            "v2html.atm.youku.com",
            "ad.video.3g.youku.com",
            "acs.youku.com",
            "stat.youku.com"
    };

    private RuleConfig() {
    }

    /**
     * Returns built-in rules first, followed by rules from the current VOD and live configs.
     * Duplicate entries are removed case-insensitively.
     */
    public static List<String> getAds() {
        Map<String, String> rules = new LinkedHashMap<>();
        addRules(rules, java.util.Arrays.asList(DEFAULT_ADS));
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
