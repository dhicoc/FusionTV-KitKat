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
