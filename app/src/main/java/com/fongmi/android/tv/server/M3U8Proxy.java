package com.fongmi.android.tv.server;

import android.net.Uri;

import com.fongmi.android.tv.bean.Rule;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.github.catvod.utils.Util;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Playlist-level HLS ad filtering, executed before IJK receives the media source. */
public final class M3U8Proxy {

    private static final String EXTINF = "#EXTINF";
    private static final String DISCONTINUITY = "#EXT-X-DISCONTINUITY";
    private static final Pattern DURATION = Pattern.compile("#EXTINF:([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern URI_ATTRIBUTE = Pattern.compile("URI=\"([^\"]+)\"");
    private static final String TOKEN = java.util.UUID.randomUUID().toString();
    private static final int MAX_PLAYLIST_BYTES = 4 * 1024 * 1024;

    private M3U8Proxy() {}

    public static String getToken() { return TOKEN; }

    public static boolean isPlaylistUrl(String value) {
        if (value == null) return false;
        String path = Uri.parse(value).getPath();
        if (path == null) return value.toLowerCase(Locale.ROOT).contains("m3u8");
        return path.toLowerCase(Locale.ROOT).endsWith(".m3u8") || value.toLowerCase(Locale.ROOT).contains("m3u8?");
    }

    public static boolean validToken(String value) { return TOKEN.equals(value); }

    public static String rewrite(String sourceUrl, String content, Map<String, String> headers) {
        if (content == null || !content.trim().startsWith("#EXTM3U")) return content;
        String base = sourceUrl;
        List<String> lines = new ArrayList<>();
        for (String line : content.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            lines.add(resolveLine(base, line, headers));
        }
        List<Rule> rules = matchingRules(sourceUrl);
        List<String> filtered = filterSegments(lines, rules);
        return join(filtered);
    }

    private static String resolveLine(String base, String line, Map<String, String> headers) {
        if (line == null || line.isEmpty()) return line;
        if (!line.startsWith("#")) {
            String absolute = resolve(base, line.trim());
            return isPlaylistUrl(absolute) ? localUrl(absolute, headers) : absolute;
        }
        Matcher matcher = URI_ATTRIBUTE.matcher(line);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String absolute = resolve(base, matcher.group(1));
            String replacement = isPlaylistUrl(absolute) ? localUrl(absolute, headers) : absolute;
            matcher.appendReplacement(result, Matcher.quoteReplacement("URI=\"" + replacement + "\""));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String localUrl(String sourceUrl, Map<String, String> headers) {
        try {
            return Server.get().getAddress("/m3u8?token=" + TOKEN
                    + "&url=" + URLEncoder.encode(sourceUrl, "UTF-8")
                    + "&headers=" + URLEncoder.encode(com.fongmi.android.tv.App.gson().toJson(headers), "UTF-8"));
        } catch (Exception e) {
            return sourceUrl;
        }
    }

    private static String resolve(String base, String reference) {
        try {
            return new URI(base).resolve(new URI(reference)).toString();
        } catch (Exception e) {
            return reference;
        }
    }

    private static List<Rule> matchingRules(String sourceUrl) {
        String host = Uri.parse(sourceUrl).getHost();
        List<Rule> result = new ArrayList<>();
        if (host == null) return result;
        addRules(result, VodConfig.get().getRules(), host);
        addRules(result, LiveConfig.get().getRules(), host);
        return result;
    }

    private static void addRules(List<Rule> result, List<Rule> candidates, String host) {
        for (Rule rule : candidates) {
            for (String candidate : rule.getHosts()) {
                try {
                    if (Util.containOrMatch(host, candidate)) {
                        result.add(rule);
                        break;
                    }
                } catch (Throwable ignored) {
                    if (host.contains(candidate)) { result.add(rule); break; }
                }
            }
        }
    }

    private static List<String> filterSegments(List<String> lines, List<Rule> rules) {
        Map<String, Integer> prefixes = new HashMap<>();
        Map<String, Integer> domains = new HashMap<>();
        int segmentCount = 0;
        for (String line : lines) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            segmentCount++;
            String path = Uri.parse(line).getPath();
            if (path == null) path = line;
            int dot = path.lastIndexOf('.');
            if (dot > 4) {
                String prefix = path.substring(0, Math.max(0, dot - 4));
                prefixes.put(prefix, prefixes.containsKey(prefix) ? prefixes.get(prefix) + 1 : 1);
            }
            String host = Uri.parse(line).getHost();
            if (host != null) domains.put(host, domains.containsKey(host) ? domains.get(host) + 1 : 1);
        }
        String dominantPrefix = dominant(prefixes, segmentCount, 0.80);
        String dominantDomain = dominant(domains, segmentCount, 0.80);
        boolean domainMode = dominantPrefix == null && dominantDomain != null && domains.size() > 1;
        Map<String, Integer> discontinuityDurations = new HashMap<>();
        List<String> output = new ArrayList<>();
        List<String> pending = new ArrayList<>();
        List<String> group = new ArrayList<>();
        boolean inDiscontinuity = false;
        for (String line : lines) {
            if (!line.startsWith("#") && !line.isEmpty()) {
                pending.add(line);
                boolean drop = shouldDrop(pending, rules, dominantPrefix, dominantDomain, domainMode, domains);
                if (inDiscontinuity) {
                    group.addAll(pending);
                    pending.clear();
                    if (isDiscontinuityEnd(lines, line)) { // marker is normally detected on the next loop iteration
                        output.addAll(group);
                        group.clear();
                        inDiscontinuity = false;
                    }
                } else if (drop) {
                    // Exclude the segment URI and the metadata immediately associated with it.
                    pending.clear();
                } else {
                    output.addAll(pending);
                    pending.clear();
                }
                continue;
            }
            if (line.startsWith(DISCONTINUITY)) {
                if (!group.isEmpty()) { output.addAll(group); group.clear(); }
                inDiscontinuity = true;
                group.addAll(pending);
                pending.clear();
                group.add(line);
            } else if (line.startsWith(EXTINF) || line.startsWith("#EXT-X-BYTERANGE") || line.startsWith("#EXT-X-GAP")) {
                pending.add(line);
            } else if (inDiscontinuity && (line.startsWith("#EXT-X-ENDLIST") || line.startsWith("#EXT-X-DISCONTINUITY"))) {
                // Keep structural tags outside the media segment group.
                output.addAll(group);
                group.clear();
                inDiscontinuity = false;
                output.add(line);
            } else if (pending.isEmpty()) {
                output.add(line);
            } else {
                pending.add(line);
            }
        }
        output.addAll(pending);
        output.addAll(group);
        return output;
    }

    private static boolean isDiscontinuityEnd(List<String> lines, String current) { return false; }

    private static boolean shouldDrop(List<String> entry, List<Rule> rules, String dominantPrefix, String dominantDomain,
                                      boolean domainMode, Map<String, Integer> domains) {
        String segment = entry.get(entry.size() - 1);
        String path = Uri.parse(segment).getPath();
        if (path == null) path = segment;
        int dot = path.lastIndexOf('.');
        if (dominantPrefix != null && dot > 4 && !path.startsWith(dominantPrefix)) return true;
        String host = Uri.parse(segment).getHost();
        if (domainMode && host != null && !host.equals(dominantDomain) && domains.get(host) != null && domains.get(host) <= 15) return true;
        for (Rule rule : rules) {
            for (String expression : rule.getRegex()) {
                if (!isPlaylistRule(expression)) continue;
                try {
                    if (Pattern.compile(expression, Pattern.MULTILINE).matcher(join(entry)).find()) return true;
                } catch (RuntimeException ignored) { }
            }
        }
        return false;
    }

    private static boolean isPlaylistRule(String expression) {
        if (expression.contains(DISCONTINUITY) || expression.contains(EXTINF) || expression.contains("#EXT-X-ENDLIST") || expression.contains("#EXT-X-KEY")) return true;
        try { return Double.parseDouble(expression) != 0; } catch (Exception ignored) { return false; }
    }

    private static String dominant(Map<String, Integer> counts, int total, double threshold) {
        String best = null;
        int max = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) if (entry.getValue() > max) { best = entry.getKey(); max = entry.getValue(); }
        return total > 0 && max * 1.0 / total >= threshold && counts.size() > 1 ? best : null;
    }

    private static String join(List<String> lines) {
        StringBuilder result = new StringBuilder();
        for (String line : lines) result.append(line).append('\n');
        return result.toString();
    }
}
