# FusionTV KitKat · 极速影视

基于 [OK 影视 KitKat](https://github.com/okcaptain/TV/tree/kitkat)（继承 [FongMi/TV](https://github.com/FongMi/TV) / CatVod）的 Android 4.4 电视优化版本，面向 ARM32、1GB 内存的老电视和盒子。

这是 dhicoc 的独立修改版，非上游官方发行。公开整理日期：2026-10-04。项目源码遵循 [GPL-3.0](LICENSE.md)，原作者版权与第三方组件许可保持有效。

## 适用设备

- Android 4.4 / API 19 起；仅打包 `armeabi-v7a`。
- 电视遥控器界面（leanback），应用显示名称为“极速影视”。
- 应用包名保留 `com.fongmi.android.tv`。安装前确认已有同包应用的签名；签名不同不能直接覆盖，卸载会删除原应用数据。

## 优化内容

- KitKat 兼容依赖与 API 替换，MultiDex，默认使用 IJK 播放链路。
- 老系统 HTTPS 通过 LegacyTlsSocket / Mbed TLS 1.2 适配。
- 海报 RGB_565 解码、160dp × 240dp 下采样；图片缓存和 Bitmap Pool 各 16MiB。
- 聚合搜索最多 5 个工作线程，并对界面刷新节流。
- IJK 普通路径探测参数为 128KiB / 500,000μs，最大缓冲 5MiB；部分媒体仍走原有特殊参数分支。这些参数不保证所有源都在 0.5 秒起播。
- 播放失败时尝试后续线路和站点；首页返回键二次确认退出，推荐首项自动聚焦。
- 中文资源裁剪，删除 X5 安装包和 Go 视频代理，裁剪部分不兼容模块。

## 使用

首次启动后，在设置中填入你有权使用的点播/直播配置。公开版不预置私人订阅令牌或第三方影视源，也不覆盖用户已有配置。外部站点、Spider 和媒体的可用性取决于服务端及插件本身；不要把第三方内容视为本仓库提供的服务。

## 首次配置：推荐用手机扫码

1. 手机和电视连接同一家庭网络，保持电视上的应用开启。
2. 电视进入 **设置 → 点播**，打开接口输入弹窗；保持弹窗不要关闭。
3. 用手机扫码弹窗二维码，或在手机/电脑浏览器输入电视显示的地址，例如 `http://192.168.1.20:9978/?tab=3`。实际 IP 以电视显示为准。
4. 在网页 **設定** 页填“名称”（可选）和完整接口地址，点 **確定**；电视输入框会收到地址。
5. 在电视弹窗再点 **确定** 保存并加载，等待站点/推荐列表出现，然后选择站点浏览或搜索。

短地址也可直接用遥控器填写。二维码打开的是电视的局域网配置页面，不是影视源订阅二维码；网页的“推送”页用于播放地址推送，配置接口应填在“設定”页。以后可再次进入设置更换接口，或使用点播配置历史切换；本版会保留你选定的配置。

网页无法打开时，核对两端是否同一网络、是否启用访客网络/客户端隔离，并确认电视应用仍在前台。电脑或手机能下载接口，不代表电视也能访问同一域名。

## 接口兼容实测：怎样选源

**Android 4.4 优先选纯 Type 0/1 采集接口。** 导入、首页、搜索详情、起播和长时间稳定播放是不同层面的验证，不能只凭 HTTP 200 或站点数量声称“全部完美运行”。

原内置 **MoonTVPlus** 是本项目验证最充分的选择：开发会话记录了 Android 4.4 模拟器上导入、首页推荐和部分剧集实际播放；未逐一验证所有站点、所有影片和所有真机。2026-10-04 本机复核仍可取得配置，但站点数已从历史 250 变为 **89**，说明接口内容会变化。

| 接口 | 开发会话中的站点数 / Type 0、1 / Type 3 | 2026-10-04 复核 | 安卓 4.4 使用建议 |
|---|---|---|---|
| MoonTVPlus（原内置） | 250 / 250 / 0 | HTTP 200；89 / 89 / 0；直播 5 组 | 优先选择。有历史 API19 部分实际播放证据，需自行取得有效订阅 |
| 4K小盒子 | 53 / 9 / 44 | HTTP 200；53 / 9 / 44；直播 2 组 | 备用候选，优先尝试 9 个直连站；其余依赖外部爬虫 |
| Qist jsm | 151 / 15 / 136 | HTTP 200；149 / 15 / 134；直播 3 组 | 备用候选，优先直连站；JS/JAR 是否可用须逐站测试 |
| 高天流云 0821 | 86 / 7 / 79 | HTTP 200；86 / 7 / 79；直播 16 组 | 备用候选，7 个直连站；直播组数不等于可播频道数 |
| Lightconer 单仓聚合 | 167 / 9 / 158 | HTTP 200；167 / 9 / 158；直播 7 组 | 备用候选，直连比例较低，不保证全部爬虫可用 |
| 高天流云 js | 298 / 0 / 298 | 本轮未复核 | 历史配置全为 Type 3，老系统应逐站验证，不列为开箱推荐 |
| 肥猫 / 王二小备用 / 俊哥蜂蜜 | 39 / 0 / 39；96 / 0 / 96；24 / 0 / 24 | 本轮未复核 | 历史依赖 Spider，导入成功也可能加载/播放失败 |
| 饭太硬 | 历史请求返回加密或非标准内容 | 本轮未复核 | 当时未完成正常解析；不据此判断今天所有客户端都不可用 |
| OK影视 / 摸鱼 / 巧记 / 小米接口 | 历史部分域名请求失败 | 本轮未复核 | 网络失败是当时本机观察，不把它写成永久失效 |

今天的复核仅测试桌面 HTTP 请求与 JSON 结构，没有启动电视或逐站播放。统计原始回执见 [2026-10-04 接口复核](docs/interface-check-2026-10-04.json)。Type 3 表示插件站点，可能是 JS/JAR 等，并不能只凭类型判定必然不兼容。

### 原内置 MoonTVPlus：如何配置

访问原服务 [tv.094264.xyz](https://tv.094264.xyz)，按服务提供方方式获取你自己的有效 TVBox 订阅地址；是否开放注册、收费或提供服务以对方现状为准。本仓库不分发作者原有的令牌。

地址形式如下，**需要换成有效令牌，不能原样粘贴模板**：

```text
https://tv.094264.xyz/api/tvbox/subscribe?token=<你的有效令牌>&adFilter=true&yellowFilter=true
```

服务端支持时，`adFilter=true` 和 `yellowFilter=true` 请求广告分片与成人分类过滤；效果取决于服务实现，不能保证所有内容都已过滤。取得完整地址后按上面的手机扫码步骤配置。

### 可直接复制的备用地址

以下地址本轮可解析，属于第三方服务。优先使用其中的 Type 0/1 直连站，选择你有权访问的内容；列表能导入并不保证每个站点都能播放。

**4K小盒子**（地址短，适合遥控器输入）：

```text
http://xhztv.top/4k.json
```

**Qist jsm**：

```text
https://raw.githubusercontent.com/qist/tvbox/master/jsm.json
```

**高天流云 0821**：

```text
https://raw.githubusercontent.com/gaotianliuyun/gao/master/0821.json
```

**Lightconer 单仓聚合**：

```text
https://raw.githubusercontent.com/Lightconer/tvbox-ysc-config/main/output/单仓聚合.json
```

### 常见失败怎么判断

- 配置加载失败：先核对完整地址、令牌有效性和电视网络；401/403 通常应向服务方确认授权，超时可能是网络或服务器问题。
- 列表出现但某站失败：换一个 Type 0/1 站点。依赖 `java.time`、Stream 或新版 Android API 的外部插件可能在 API19 报类缺失或 Dex 错误。
- 详情正常但无法起播：该媒体链接、解析、编码格式或分片域名可能不可用；尝试其他线路/站点，不要把导入成功当作播放器链路已通过。
- 首页空白：确认配置已保存并选择了可用的首页站点；纯采集站也可能只提供分类/搜索而没有推荐数据。

## 构建

需要 JDK 17 或 21、Android SDK Platform 35、Build Tools 36.0.0。Android Studio 或 Android command-line tools 均可；配置 `ANDROID_HOME`，或创建不提交的 `local.properties`。

```bash
sdkmanager "platforms;android-35" "build-tools;36.0.0" "platform-tools"
./gradlew :app:assembleLeanbackDebug --no-daemon
```

Windows 使用 `gradlew.bat :app:assembleLeanbackDebug --no-daemon`。

Gradle Wrapper 8.9、Android Gradle Plugin 8.7.3。首次构建会下载 Google Maven / Maven Central / JitPack 依赖，需要网络。输出：`app/build/outputs/apk/leanback/debug/leanback.apk`。

Debug APK 由本机 Android 默认调试密钥签名，只适合自行测试。Release 构建命令为 `:app:assembleLeanbackRelease`，当前不配置公开签名密钥，需要自行签名。仓库不分发原工作区中带私人配置的旧 APK。

常规构建使用已有 native 库。TLS C 源码与 Mbed TLS 3.6.7 位于 `native/tls/`，重建方法见 [TLS 说明](native/tls/README.md)。其他上游预编译组件及来源见 [第三方说明](THIRD_PARTY_NOTICES.md)。

## 验证范围与限制

开发会话记录了 API 19 ARM 模拟器上的首页、播放、遥控器和压力测试。它们是历史观察，不代表所有老电视、解码芯片和外部影视源均通过测试。本次开源整理的构建与静态检查见 [发布验证](docs/VALIDATION.md)。

- 本版对 DLNA、X5、Go 代理、部分第三方协议/提取器及弹幕等有裁剪，详见 [变更记录](CHANGELOG.md)，不宣称与最新版 FongMi 功能完全一致。
- `largeHeap=true` 保留；它扩大可用 Java 堆额度，不等于系统内存占用降低。
- 继承上游网络客户端的宽松证书/主机名校验策略。TLS 兼容不代表已完成安全加固，避免用本应用传输敏感凭据。
- 本项目维护 Java 应用源码和 TLS 桥接源码；部分 native/AAR 组件沿用上游预编译分发形式，其许可证和维护责任各自独立。

## 来源和贡献

精确基线、上游原说明和改动列表见 [来源记录](docs/PROVENANCE.md)、[上游 README](docs/UPSTREAM_README.md) 和 [CHANGELOG](CHANGELOG.md)。欢迎提交兼容性问题，请注明系统版本、CPU ABI、设备型号和可脱敏的日志。

