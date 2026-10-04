package com.fongmi.android.tv.player;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.ui.PlayerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.impl.ParseCallback;
import com.fongmi.android.tv.player.exo.ExoUtil;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Path;
import com.google.common.net.HttpHeaders;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class Players implements Player.Listener, IMediaPlayer.Listener, ParseCallback {

    private static final String TAG = Players.class.getSimpleName();

    public static final int SYS = 0;
    public static final int IJK = 1;
    public static final int EXO = 2;

    public static final int SOFT = 0;
    public static final int HARD = 1;

    private final StringBuilder builder;
    private final Formatter formatter;
    private final Runnable runnable;

    private Map<String, String> headers;
    private IjkVideoView ijkPlayer;
    private ExoPlayer exoPlayer;
    private ParseJob parseJob;
    private List<Sub> subs;
    private String format;
    private String url;
    private Drm drm;
    private Sub sub;

    private long position;
    private int decode;
    private int count;
    private int player;
    private int retry;

    public static Players create(Activity activity) {
        Players player = new Players(activity);
        Server.get().setPlayer(player);
        return player;
    }

    public static boolean isExo(int type) {
        return false;
    }

    public static boolean isHard(int decode) {
        return decode == HARD;
    }

    public boolean isHard() {
        return decode == HARD;
    }

    public boolean isSoft() {
        return decode == SOFT;
    }

    public boolean isExo() {
        return false;
    }

    public boolean isIjk() {
        return true;
    }

    private Players(Activity activity) {
        player = IJK;
        decode = Setting.getDecode(player);
        builder = new StringBuilder();
        runnable = ErrorEvent::timeout;
        formatter = new Formatter(builder, Locale.getDefault());
        position = C.TIME_UNSET;
    }

    public void init(PlayerView exo, IjkVideoView ijk) {
        releaseIjk();
        initIjk(ijk);
    }

    private void initExo(PlayerView view) {
    }

    private void initIjk(IjkVideoView view) {
        ijkPlayer = view.render(Setting.getRender()).decode(decode);
        ijkPlayer.addListener(this);
        ijkPlayer.setPlayer(player);
    }

    public ExoPlayer exo() {
        return exoPlayer;
    }

    public IjkVideoView ijk() {
        return ijkPlayer;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers == null ? new HashMap<>() : headers;
    }

    public void setSub(Sub sub) {
        this.sub = sub;
        if (isIjk()) return;
        setPosition(getPosition());
        setMediaSource();
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getPlayer() {
        return player;
    }

    public void setPlayer(int player) {
        this.player = IJK;
        this.decode = getDecode(IJK);
    }

    public int getDecode(int player) {
        return Setting.getDecode(player);
    }

    public void setDecode(int player, int decode) {
        Setting.putDecode(player, decode);
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public boolean canToggleDecode() {
        return isExo() && ++count <= 1;
    }

    public void reset() {
        position = C.TIME_UNSET;
        removeTimeoutCheck();
        stopParse();
        count = 0;
        retry = 0;
    }

    public void clear() {
        headers = null;
        format = null;
        subs = null;
        drm = null;
        url = null;
    }

    public int addRetry() {
        return ++retry;
    }

    public String stringToTime(long time) {
        return Util.format(builder, formatter, time);
    }

    public int getVideoWidth() {
        return isExo() ? exoPlayer.getVideoSize().width : ijkPlayer.getVideoWidth();
    }

    public int getVideoHeight() {
        return isExo() ? exoPlayer.getVideoSize().height : ijkPlayer.getVideoHeight();
    }

    public float getSpeed() {
        if (isExo() && exoPlayer != null) return exoPlayer.getPlaybackParameters().speed;
        if (isIjk() && ijkPlayer != null) return ijkPlayer.getSpeed();
        return 1.0f;
    }

    public long getPosition() {
        if (isExo() && exoPlayer != null) return exoPlayer.getCurrentPosition();
        if (isIjk() && ijkPlayer != null) return ijkPlayer.getCurrentPosition();
        return 0;
    }

    public long getDuration() {
        if (isExo() && exoPlayer != null) return exoPlayer.getDuration();
        if (isIjk() && ijkPlayer != null) return ijkPlayer.getDuration();
        return -1;
    }

    public long getBuffered() {
        if (isExo() && exoPlayer != null) return exoPlayer.getBufferedPosition();
        if (isIjk() && ijkPlayer != null) return ijkPlayer.getBufferedPosition();
        return 0;
    }

    public void prepared() {
    }

    public boolean canAdjustSpeed() {
        return isIjk() || !Setting.isTunnel();
    }

    public boolean haveTrack(int type) {
        if (isExo() && exoPlayer != null) return ExoUtil.haveTrack(exoPlayer.getCurrentTracks(), type);
        if (isIjk() && ijkPlayer != null) return ijkPlayer.haveTrack(type);
        return false;
    }

    public boolean isPlaying() {
        return isExo() ? exoPlayer != null && exoPlayer.isPlaying() : ijkPlayer != null && ijkPlayer.isPlaying();
    }

    public boolean isEnd() {
        if (isExo() && exoPlayer != null) return exoPlayer.getPlaybackState() == Player.STATE_ENDED;
        if (isIjk() && ijkPlayer != null) return ijkPlayer.getPlaybackState() == IjkVideoView.STATE_ENDED;
        return false;
    }

    public boolean isRelease() {
        return exoPlayer == null || ijkPlayer == null;
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(getUrl());
    }

    public boolean isLive() {
        return getDuration() < 5 * 60 * 1000;
    }

    public boolean isVod() {
        return getDuration() > 5 * 60 * 1000;
    }

    public boolean isPortrait() {
        return getVideoHeight() > getVideoWidth();
    }

    public String getSizeText() {
        return getVideoWidth() + " x " + getVideoHeight();
    }

    public String getSpeedText() {
        return String.format(Locale.getDefault(), "%.2f", getSpeed());
    }

    public String getPlayerText() {
        return ResUtil.getStringArray(R.array.select_player)[player];
    }

    public String getDecodeText() {
        return ResUtil.getStringArray(R.array.select_decode)[decode];
    }

    public String setSpeed(float speed) {
        if (exoPlayer != null && !Setting.isTunnel()) exoPlayer.setPlaybackSpeed(speed);
        if (ijkPlayer != null) ijkPlayer.setSpeed(speed);
        return getSpeedText();
    }

    public String addSpeed() {
        float speed = getSpeed();
        float addon = speed >= 2 ? 1f : 0.25f;
        speed = speed >= 5 ? 0.25f : Math.min(speed + addon, 5.0f);
        return setSpeed(speed);
    }

    public String addSpeed(float value) {
        float speed = getSpeed();
        speed = Math.min(speed + value, 5);
        return setSpeed(speed);
    }

    public String subSpeed(float value) {
        float speed = getSpeed();
        speed = Math.max(speed - value, 0.2f);
        return setSpeed(speed);
    }

    public String toggleSpeed() {
        float speed = getSpeed();
        speed = speed == 1 ? 3f : 1f;
        return setSpeed(speed);
    }

    public void togglePlayer() {
        setPlayer(IJK);
    }

    public void nextPlayer() {
        setPlayer(IJK);
    }

    public void toggleDecode(boolean save) {
        decode = isHard() ? SOFT : HARD;
        if (save) setDecode(player, decode);
    }

    public String getPositionTime(long time) {
        time = getPosition() + time;
        if (time > getDuration()) time = getDuration();
        else if (time < 0) time = 0;
        return stringToTime(time);
    }

    public String getDurationTime() {
        long time = getDuration();
        if (time < 0) time = 0;
        return stringToTime(time);
    }

    public void seekTo(int time) {
        seekTo(getPosition() + time);
    }

    public void seekTo(long time) {
        if (isExo() && exoPlayer != null) exoPlayer.seekTo(time);
        if (isIjk() && ijkPlayer != null) ijkPlayer.seekTo(time);
    }

    public void play() {
        if (isPlaying() || isEnd()) return;
        if (isExo()) playExo();
        if (isIjk()) playIjk();
    }

    public void pause() {
        if (isExo()) pauseExo();
        if (isIjk()) pauseIjk();
    }

    public void stop() {
        if (isExo()) stopExo();
        if (isIjk()) stopIjk();
    }

    public void release() {
        stopParse();
        if (isExo()) releaseExo();
        if (isIjk()) releaseIjk();
        removeTimeoutCheck();
        Server.get().setPlayer(null);
        App.execute(() -> Source.get().stop());
    }

    public void start(Channel channel, int timeout) {
        if (channel.hasMsg()) {
            ErrorEvent.extract(channel.getMsg());
        } else if (channel.getParse() == 1) {
            startParse(channel.result(), false);
        } else if (isIllegal(channel.getUrl())) {
            ErrorEvent.url(0);
        } else {
            setMediaSource(channel, timeout);
        }
    }

    public void start(Result result, boolean useParse, int timeout) {
        if (result.hasMsg()) {
            ErrorEvent.extract(result.getMsg());
        } else if (result.getParse(1) == 1 || result.getJx() == 1) {
            startParse(result, useParse);
        } else if (isIllegal(result.getRealUrl())) {
            ErrorEvent.url(0);
        } else {
            setMediaSource(result, timeout);
        }
    }

    private void playExo() {
        if (exoPlayer == null) return;
        exoPlayer.play();
    }

    private void playIjk() {
        if (ijkPlayer == null) return;
        ijkPlayer.start();
    }

    private void pauseExo() {
        if (exoPlayer == null) return;
        exoPlayer.pause();
    }

    private void pauseIjk() {
        if (ijkPlayer == null) return;
        ijkPlayer.pause();
    }

    private void stopExo() {
        if (exoPlayer == null) return;
        exoPlayer.stop();
        exoPlayer.clearMediaItems();
    }

    private void stopIjk() {
        if (ijkPlayer == null) return;
        ijkPlayer.stop();
    }

    private void releaseExo() {
        if (exoPlayer == null) return;
        exoPlayer.removeListener(this);
        exoPlayer.release();
        exoPlayer = null;
    }

    private void releaseIjk() {
        if (ijkPlayer == null) return;
        ijkPlayer.release();
        ijkPlayer = null;
    }

    private void startParse(Result result, boolean useParse) {
        stopParse();
        parseJob = ParseJob.create(this).start(result, useParse);
    }

    private void stopParse() {
        if (parseJob != null) parseJob.stop();
        parseJob = null;
    }

    public void setMediaSource() {
        setMediaSource(headers, url, format, drm, subs, Constant.TIMEOUT_PLAY);
    }

    public void setMediaSource(String url) {
        setMediaSource(new HashMap<>(), url);
    }

    private void setMediaSource(Map<String, String> headers, String url) {
        setMediaSource(headers, url, null, null, new ArrayList<>(), Constant.TIMEOUT_PLAY);
    }

    private void setMediaSource(Channel channel, int timeout) {
        setMediaSource(channel.getHeaders(), channel.getUrl(), channel.getFormat(), channel.getDrm(), new ArrayList<>(), timeout);
    }

    private void setMediaSource(Result result, int timeout) {
        setMediaSource(result.getHeaders(), result.getRealUrl(), result.getFormat(), result.getDrm(), result.getSubs(), timeout);
    }

    private void setMediaSource(Map<String, String> headers, String url, String format, Drm drm, List<Sub> subs, int timeout) {
        if (isIjk() && ijkPlayer != null) {
            ijkPlayer.setMediaSource(IjkUtil.getSource(this.headers = checkUa(headers), this.url = url), position);
            ijkPlayer.start();
        }
        if (isExo() && exoPlayer != null) exoPlayer.setMediaItem(ExoUtil.getMediaItem(this.headers = checkUa(headers), UrlUtil.uri(this.url = url), this.format = format, this.drm = drm, checkSub(this.subs = subs), decode), position);
        if (isExo() && exoPlayer != null) exoPlayer.prepare();
        App.post(runnable, timeout);
        PlayerEvent.prepare();
        Logger.t(TAG).d(url);
    }

    private void removeTimeoutCheck() {
        App.removeCallbacks(runnable);
    }

    public void setTrack(List<Track> tracks) {
        for (Track track : tracks) setTrack(track);
    }

    private void setTrack(Track item) {
        if (item.isExo(player)) setTrackExo(item);
        if (item.isIjk(player)) setTrackIjk(item);
    }

    private void setTrackExo(Track item) {
        if (item.isSelected()) {
            ExoUtil.selectTrack(exoPlayer, item.getGroup(), item.getTrack());
        } else {
            ExoUtil.deselectTrack(exoPlayer, item.getGroup(), item.getTrack());
        }
    }

    private void setTrackIjk(Track item) {
        if (item.isSelected()) {
            ijkPlayer.selectTrack(item.getType(), item.getTrack());
        } else {
            ijkPlayer.deselectTrack(item.getType(), item.getTrack());
        }
    }

    private boolean isIllegal(String url) {
        Uri uri = UrlUtil.uri(url);
        String host = UrlUtil.host(uri);
        String scheme = UrlUtil.scheme(uri);
        if ("data".equals(scheme)) return false;
        return scheme.isEmpty() || "file".equals(scheme) ? !Path.exists(url) : host.isEmpty();
    }

    public static Map<String, String> checkUa(Map<String, String> headers) {
        if (Setting.getUa().isEmpty()) return headers;
        for (Map.Entry<String, String> header : headers.entrySet()) if (HttpHeaders.USER_AGENT.equalsIgnoreCase(header.getKey())) return headers;
        headers.put(HttpHeaders.USER_AGENT, Setting.getUa());
        return headers;
    }

    private List<Sub> checkSub(List<Sub> subs) {
        if (sub == null) return subs;
        subs.add(0, sub);
        return subs;
    }

    public Uri getUri() {
        return getUrl().startsWith("file://") || getUrl().startsWith("/") ? FileUtil.getShareUri(getUrl()) : Uri.parse(getUrl());
    }

    public String[] getHeaderArray() {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : getHeaders().entrySet()) list.addAll(Arrays.asList(entry.getKey(), entry.getValue()));
        return list.toArray(new String[0]);
    }

    public Bundle getHeaderBundle() {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : getHeaders().entrySet()) bundle.putString(entry.getKey(), entry.getValue());
        return bundle;
    }

    public void choose(Activity activity, CharSequence title) {
        try {
            if (isEmpty()) return;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(getUri(), "video/*");
            intent.putExtra("title", title);
            intent.putExtra("return_result", isVod());
            intent.putExtra("headers", getHeaderArray());
            if (isVod()) intent.putExtra("position", (int) getPosition());
            activity.startActivityForResult(Util.getChooser(intent), 1001);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkData(Intent data) {
        try {
            if (data == null || data.getExtras() == null) return;
            int position = data.getExtras().getInt("position", 0);
            String endBy = data.getExtras().getString("end_by", "");
            if ("playback_completion".equals(endBy)) ActionEvent.next();
            if ("user".equals(endBy)) seekTo(position);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onParseSuccess(Map<String, String> headers, String url, String from) {
        if (!TextUtils.isEmpty(from)) Notify.show(ResUtil.getString(R.string.parse_from, from));
        setMediaSource(headers, url);
    }

    @Override
    public void onParseError() {
        ErrorEvent.parse();
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
        Logger.t(TAG).e(error.errorCode + "," + url);
        ErrorEvent.url(ExoUtil.getRetry(error.errorCode), error.errorCode);
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        PlayerEvent.state(state);
    }

    @Override
    public void onInfo(IMediaPlayer mp, int what, int extra) {
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                PlayerEvent.state(Player.STATE_BUFFERING);
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
            case IMediaPlayer.MEDIA_INFO_VIDEO_SEEK_RENDERING_START:
            case IMediaPlayer.MEDIA_INFO_AUDIO_SEEK_RENDERING_START:
                PlayerEvent.state(Player.STATE_READY);
                break;
        }
    }

    @Override
    public boolean onError(IMediaPlayer mp, int what, int extra) {
        ErrorEvent.url(1);
        return true;
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        PlayerEvent.state(Player.STATE_READY);
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        PlayerEvent.state(Player.STATE_ENDED);
    }

}
