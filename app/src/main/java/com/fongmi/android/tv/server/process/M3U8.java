package com.fongmi.android.tv.server.process;

import android.net.Uri;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.server.M3U8Proxy;
import com.fongmi.android.tv.server.Nano;
import com.github.catvod.net.OkHttp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import okhttp3.Request;
import okhttp3.Response;

/** Fetches an HLS playlist and returns a rewritten copy with matched ad segments removed. */
public class M3U8 implements Process {

 @Override
 public boolean isRequest(NanoHTTPD.IHTTPSession session, String path) {
 return "/m3u8".equals(path);
    }

 @Override
 public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String path, Map<String, String> files) {
 Map<String, String> params = session.getParms();
 String source = params.get("url");
 if (!M3U8Proxy.validToken(params.get("token")) || source == null || !M3U8Proxy.isPlaylistUrl(source)) {
 return Nano.error(NanoHTTPD.Response.Status.FORBIDDEN, "Invalid playlist proxy request");
        }
 Uri uri = Uri.parse(source);
 if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
 return Nano.error(NanoHTTPD.Response.Status.BAD_REQUEST, "Unsupported playlist URL");
        }
 try {
 Map<String, String> headers = readHeaders(params.get("headers"));
 Request.Builder builder = new Request.Builder().url(source).get();
 for (Map.Entry<String, String> header : headers.entrySet()) {
 if (header.getKey() != null && header.getValue() != null) builder.header(header.getKey(), header.getValue());
            }
 // 修复：当前 OkHttp 工具类没有 newCall(OkHttpClient, Request) 重载，直接使用 OkHttpClient 保留完整请求头。
        try (Response response = OkHttp.client(Constant.TIMEOUT_PLAY).newCall(builder.build()).execute()) {
 if (!response.isSuccessful() || response.body() == null) return Nano.error("Playlist fetch failed: " + response.code());
 String body = response.body().string();
 if (body.length() > 4 * 1024 * 1024) return Nano.error(NanoHTTPD.Response.Status.INTERNAL_ERROR, "Playlist too large");
 String effectiveUrl = response.request().url().toString();
 String output = Setting.isRemoveAd() ? M3U8Proxy.rewrite(effectiveUrl, body, headers) : body;
 return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/vnd.apple.mpegurl; charset=utf-8", output);
            }
        } catch (IOException | RuntimeException e) {
 return Nano.error(e.getMessage() == null ? "Playlist proxy failed" : e.getMessage());
        }
    }

 private Map<String, String> readHeaders(String json) {
 Map<String, String> result = new HashMap<>();
 if (json == null || json.isEmpty()) return result;
 try {
 org.json.JSONObject object = new org.json.JSONObject(json);
 Iterator<String> keys = object.keys();
 while (keys.hasNext()) {
 String key = keys.next();
 result.put(key, object.optString(key, ""));
            }
        } catch (Exception ignored) { }
 return result;
    }
}
