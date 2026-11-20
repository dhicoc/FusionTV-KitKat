package com.fongmi.android.tv.player;

import android.net.Uri;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.server.M3U8Proxy;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.utils.Json;

import java.net.URLEncoder;
import java.util.Map;

import tv.danmaku.ijk.media.player.MediaSource;

public class IjkUtil {

 public static MediaSource getSource(Result result) {
 return getSource(result.getHeaders(), result.getRealUrl());
    }

 public static MediaSource getSource(Channel channel) {
 return getSource(channel.getHeaders(), channel.getUrl());
    }

 public static MediaSource getSource(Map<String, String> headers, String url) {
 Uri uri = UrlUtil.uri(url);
 Map<String, String> safeHeaders = Players.checkUa(headers);
 if (Setting.isRemoveAd() && M3U8Proxy.isPlaylistUrl(url)) {
 try {
 String target = Server.get().getAddress("/m3u8?token=" + M3U8Proxy.getToken()
                        + "&url=" + URLEncoder.encode(url, "UTF-8")
                        + "&headers=" + URLEncoder.encode(Json.toObject(safeHeaders).toString(), "UTF-8")); // 修复：使用现有 toObject() 生成 headers JSON。
 uri = Uri.parse(target);
            } catch (Exception ignored) {
 // Fall back to the original stream if the local proxy URL cannot be built.
            }
        }
 return new MediaSource(safeHeaders, uri);
    }
}
