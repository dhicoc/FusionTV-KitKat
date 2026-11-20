package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.ActivitySettingCustomBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.CacheDirCallback;
import com.fongmi.android.tv.impl.LanguageCallback;
import com.fongmi.android.tv.impl.MenuKeyCallback;
import com.fongmi.android.tv.impl.X5WebViewCallback;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.ButtonsDialog;
import com.fongmi.android.tv.ui.dialog.CacheDirDialog;
import com.fongmi.android.tv.ui.dialog.DisplayDialog;
import com.fongmi.android.tv.ui.dialog.LanguageDialog;
import com.fongmi.android.tv.ui.dialog.MenuKeyDialog;
import com.fongmi.android.tv.utils.LanguageUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Shell;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.permissionx.guolindev.PermissionX;

import java.util.Locale;

public class SettingCustomActivity extends BaseActivity implements MenuKeyCallback, X5WebViewCallback, LanguageCallback, CacheDirCallback {

    private ActivitySettingCustomBinding mBinding;
    private String[] quality, size, episode, fullscreenMenuKey, smallWindowBackKey, homeUI, parseWebview, configCache;

    @Override protected ViewBinding getBinding() { return mBinding = ActivitySettingCustomBinding.inflate(getLayoutInflater()); }
    public static void start(Activity activity) { activity.startActivity(new Intent(activity, SettingCustomActivity.class)); }
    private String getSwitch(boolean value) { return getString(value ? R.string.setting_on : R.string.setting_off); }

    @Override protected void initView() {
        mBinding.quality.requestFocus();
        mBinding.qualityText.setText((quality = ResUtil.getStringArray(R.array.select_quality))[Setting.getQuality()]);
        mBinding.sizeText.setText((size = ResUtil.getStringArray(R.array.select_size))[Setting.getSize()]);
        mBinding.episodeText.setText((episode = ResUtil.getStringArray(R.array.select_episode))[Setting.getEpisode()]);
        mBinding.speedText.setText(getSpeedText());
        mBinding.fullscreenMenuKeyText.setText((fullscreenMenuKey = ResUtil.getStringArray(R.array.select_fullscreen_menu_key))[Setting.getFullscreenMenuKey()]);
        mBinding.homeSiteLockText.setText(getSwitch(Setting.isHomeSiteLock()));
        mBinding.incognitoText.setText(getSwitch(Setting.isIncognito()));
        mBinding.smallWindowBackKeyText.setText((smallWindowBackKey = ResUtil.getStringArray(R.array.select_small_window_back_key))[Setting.getSmallWindowBackKey()]);
        mBinding.homeMenuKeyText.setText(ResUtil.getStringArray(R.array.select_home_menu_key)[Setting.getHomeMenuKey()]);
        mBinding.aggregatedSearchText.setText(getSwitch(Setting.isAggregatedSearch()));
        mBinding.homeUIText.setText((homeUI = ResUtil.getStringArray(R.array.select_home_ui))[Setting.getHomeUI()]);
        mBinding.homeHistoryText.setText(getSwitch(Setting.isHomeHistory()));
        mBinding.cacheDirText.setText(Setting.getThunderCacheDir());
        mBinding.removeAdText.setText(getSwitch(Setting.isRemoveAd()));
        mBinding.languageText.setText(ResUtil.getStringArray(R.array.select_language)[Setting.getLanguage()]);
        mBinding.parseWebviewText.setText((parseWebview = ResUtil.getStringArray(R.array.select_parse_webview))[Setting.getParseWebView()]);
        mBinding.configCacheText.setText((configCache = ResUtil.getStringArray(R.array.select_config_cache))[Setting.getConfigCache()]);
    }

    @Override protected void initEvent() {
        mBinding.quality.setOnClickListener(this::setQuality);
        mBinding.size.setOnClickListener(this::setSize);
        mBinding.episode.setOnClickListener(this::setEpisode);
        mBinding.display.setOnClickListener(v -> DisplayDialog.create(this).show());
        mBinding.speed.setOnClickListener(this::setSpeed);
        mBinding.speed.setOnLongClickListener(this::resetSpeed);
        mBinding.fullscreenMenuKey.setOnClickListener(this::setFullscreenMenuKey);
        mBinding.homeSiteLock.setOnClickListener(v -> { Setting.putHomeSiteLock(!Setting.isHomeSiteLock()); mBinding.homeSiteLockText.setText(getSwitch(Setting.isHomeSiteLock())); });
        mBinding.incognito.setOnClickListener(v -> { Setting.putIncognito(!Setting.isIncognito()); mBinding.incognitoText.setText(getSwitch(Setting.isIncognito())); });
        mBinding.smallWindowBackKey.setOnClickListener(this::setSmallWindowBackKey);
        mBinding.homeMenuKey.setOnClickListener(v -> MenuKeyDialog.create(this).show());
        mBinding.aggregatedSearch.setOnClickListener(v -> { Setting.putAggregatedSearch(!Setting.isAggregatedSearch()); mBinding.aggregatedSearchText.setText(getSwitch(Setting.isAggregatedSearch())); });
        mBinding.homeUI.setOnClickListener(this::setHomeUI);
        mBinding.homeButtons.setOnClickListener(v -> ButtonsDialog.create(this).show());
        mBinding.homeHistory.setOnClickListener(v -> { Setting.putHomeHistory(!Setting.isHomeHistory()); mBinding.homeHistoryText.setText(getSwitch(Setting.isHomeHistory())); });
        mBinding.removeAd.setOnClickListener(this::setRemoveAd);
        mBinding.setLanguage.setOnClickListener(v -> LanguageDialog.create(this).show());
        mBinding.parseWebview.setOnClickListener(this::setParseWebview);
        mBinding.configCache.setOnClickListener(this::setConfigCache);
        mBinding.cacheDir.setOnClickListener(this::setCacheDir);
        mBinding.reset.setOnClickListener(this::onReset);
    }

    private void setQuality(View v) { int i = next(Setting.getQuality(), quality.length); Setting.putQuality(i); mBinding.qualityText.setText(quality[i]); RefreshEvent.image(); }
    private void setSize(View v) { int i = next(Setting.getSize(), size.length); Setting.putSize(i); mBinding.sizeText.setText(size[i]); RefreshEvent.size(); }
    private void setEpisode(View v) { int i = next(Setting.getEpisode(), episode.length); Setting.putEpisode(i); mBinding.episodeText.setText(episode[i]); }
    private int next(int current, int length) { return current >= length - 1 ? 0 : current + 1; }
    private String getSpeedText() { return String.format(Locale.getDefault(), "%.2f", Setting.getPlaySpeed()); }
    private void setSpeed(View v) { float speed = Setting.getPlaySpeed(); speed = speed >= 5 ? .2f : Math.min(speed + (speed >= 2 ? 1f : .1f), 5f); Setting.putPlaySpeed(speed); mBinding.speedText.setText(getSpeedText()); }
    private boolean resetSpeed(View v) { Setting.putPlaySpeed(1f); mBinding.speedText.setText(getSpeedText()); return true; }
    private void setFullscreenMenuKey(View v) { int i = next(Setting.getFullscreenMenuKey(), fullscreenMenuKey.length); Setting.putFullscreenMenuKey(i); mBinding.fullscreenMenuKeyText.setText(fullscreenMenuKey[i]); }
    private void setSmallWindowBackKey(View v) { int i = next(Setting.getSmallWindowBackKey(), smallWindowBackKey.length); Setting.putSmallWindowBackKey(i); mBinding.smallWindowBackKeyText.setText(smallWindowBackKey[i]); }
    private void setHomeUI(View v) { int i = next(Setting.getHomeUI(), homeUI.length); Setting.putHomeUI(i); mBinding.homeUIText.setText(homeUI[i]); }
    private void setRemoveAd(View v) { boolean enabled = !Setting.isRemoveAd(); Setting.putRemoveAd(enabled); mBinding.removeAdText.setText(getSwitch(enabled)); }
    private void setParseWebview(View v) { int i = next(Setting.getParseWebView(), parseWebview.length); Setting.putParseWebView(i); mBinding.parseWebviewText.setText(parseWebview[i]); }
    private void setConfigCache(View v) { int i = next(Setting.getConfigCache(), configCache.length); Setting.putConfigCache(i); mBinding.configCacheText.setText(configCache[i]); }
    private void setCacheDir(View v) { PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, granted, denied) -> { if (allGranted) CacheDirDialog.create(this).show(); }); }
    private void onReset(View v) { new MaterialAlertDialogBuilder(this).setTitle(R.string.dialog_reset_app).setMessage(R.string.dialog_reset_app_data).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (d, w) -> reset()).show(); }
    private void reset() { new Thread(() -> Shell.exec("pm clear " + App.get().getPackageName())).start(); }
    @Override public void setCacheDir(String dir) { Setting.putThunderCacheDir(dir); mBinding.cacheDirText.setText(dir); App.post(() -> Util.restartApp(this), 1000); }
    @Override public void setLanguage(int lang) { Setting.putLanguage(lang); LanguageUtil.setLocale(LanguageUtil.getLocale(Setting.getLanguage())); mBinding.languageText.setText(ResUtil.getStringArray(R.array.select_language)[Setting.getLanguage()]); App.post(() -> Util.restartApp(this), 1000); }
    @Override public void onX5Success() { int i = 1; Setting.putParseWebView(i); mBinding.parseWebviewText.setText(parseWebview[i]); App.post(() -> Util.restartApp(this), 500); }
    @Override public void onX5Error() { int i = 0; Setting.putParseWebView(i); mBinding.parseWebviewText.setText(parseWebview[i]); }
    @Override public void onX5Cancel() { int i = 0; Setting.putParseWebView(i); mBinding.parseWebviewText.setText(parseWebview[i]); }
    @Override public void onMenuKeyItemClick(int position) { Setting.putHomeMenuKey(position); mBinding.homeMenuKeyText.setText(ResUtil.getStringArray(R.array.select_home_menu_key)[Setting.getHomeMenuKey()]); }
}
