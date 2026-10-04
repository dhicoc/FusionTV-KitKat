# 首次公开源码验证

日期：2026-10-04。

- 独立公开源码目录使用 JDK 21、Gradle 8.9、AGP 8.7.3、SDK 35 / Build Tools 36.0.0 完成 `:app:assembleLeanbackDebug`，104 个任务执行，BUILD SUCCESSFUL（51 秒）。
- 私人预置订阅从 Config.java 移除；Config.vod() 保留数据库已有选项，未配置时返回空配置，行为与原上游配置入口一致。原开发树和旧 APK 保持原状。
- 五个接口 HTTP/JSON 复核结果见 interface-check-2026-10-04.json，不包含私人完整 URL 或令牌。今天未启动 API19 模拟器，未重复历史逐剧播放及压力测试。
- 首次配置说明对照真实 SettingActivity / ConfigDialog、网页 index.html 和 script.js；扫码网页填地址后仍需电视确认。

本次验证不覆盖全部外部插件、第三方媒体可播率、Release 签名与所有真机性能。

TLS native 源码以 NDK r23c 明确指定 android-19 / armeabi-v7a 重建成功，最终 APK 增量构建成功（13 秒，4 项执行）。本次未在 API19 设备重新验证该重建库的握手；JNI 接口与本地 Java 桥接保持一致。

## Release 测试 APK 补充验证

v2.5.0-kitkat.1 现附 ARM32 Debug APK。应用模块对源码标签无改动；API19 条件下 v1/v2 签名验证通过，签名证书 SHA-256 为 02233ccd18855fa364b7820aebaee91775e224f7e623b259fb617c582fb6c567。所有 ZIP 条目解压扫描未检出原私人订阅令牌，包中 libntvtls.so 与仓库重建库 SHA-256 一致。本轮没有新增 API19 运行验收。

## v2.5.0-kitkat.2 审查修复验证

日期：2026-10-04。独立 API19 / ARM 模拟器，使用公开源码和本机受控站点，不导入私人订阅。

- JDK21、Gradle8.9、AGP8.7.3、SDK35 / Build Tools36 下 `:app:assembleLeanbackDebug :app:assembleLeanbackRelease --no-daemon` 成功（2分28秒）。Release 保留 R8，其 mapping/seeds 保留 `transportRead` / `transportWrite` 原名，usage 不再删除它们。
- Debug APK 的实际 Players 初始化/释放状态检查通过。受控 IJK 媒体时钟以位置45000ms、时长120000ms、缓冲60000ms驱动实际 CustomSeekView，显示00:45 / 02:00及缓冲符合断言。这项测试不包含真实影片解码和长时间播放。
- 自动换源模式实际向全部11个本机 CMS 发出 HTTP 搜索请求，第11站请求通过断言；保留两线程并发。
- Debug APK 原生 TLS 到 example.com 完成真实 TLS1.2 握手并取得 HTTP 响应。
- 混淆 Release APK 在本机临时使用相同调试证书签名，通过应用实际混淆 HTTP 客户端取得 socket factory，原生 TLS 握手和 HTTP 响应通过。该专项只验 TLS；发行资产使用已验证的 Debug APK。
- 两组 Instrumentation 最终结果均为 `stream=PASS`、`INSTRUMENTATION_CODE: -1`。复用入口见 [tools/regression](../tools/regression/README.md)。
- `gradlew` 的 Git 执行位100755、索引及工作树LF，Git Bash `bash -n` 通过；未在 Linux 主机重新全量构建。
- 新 Debug APK 大小20,019,673字节，包名com.fongmi.android.tv，versionCode251 / versionName2.5.0-kitkat.2，minSdk19 / targetSdk28 / armeabi-v7a。API19 条件下 v1/v2 签名验证通过，证书指纹与前版相同，可覆盖前版安装；所有 ZIP 条目解压扫描未检出原私人令牌。
- 最终发行文件通过 `adb install -r` 安装后再次运行完整 Debug 回归，结果 PASS。桌面入口启动进入 HomeActivity，遥控方向键/确认键进入 SettingActivity，界面显示版本2.5.0-kitkat.2；点播配置对话框正常显示二维码、网页配置地址和输入入口。该启动及导航日志未检出 FATAL EXCEPTION / ANR / Fatal signal / OutOfMemoryError。

上述结果不代表所有外部接口、第三方插件、真实影片解码、真机性能和长期播放全部通过；接口结构复核与历史播放证据仍按 README 原范围说明。
