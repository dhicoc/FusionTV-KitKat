# 来源与修改说明

## 应用基线

直接上游：https://github.com/okcaptain/TV ，kitkat 分支。
本地基线提交：`f4a5204f49bc311b086eda75345663ac5d760061`。
原项目继承 FongMi/TV 和 CatVod。公开快照保留原 LICENSE.md、文件内版权和原使用说明，不冒充上游。

2026-10-04，dhicoc 将本地 Android 4.4 优化工作整理为独立源码快照。完整优化内容见 CHANGELOG.md。旧会话、私人配置、SDK、AVD、构建缓存和签名密钥不进入公开仓库。本版不是工作区根目录下另一个自行重写的 FusionTV 实验应用。

## TLS 来源

`catvod/src/main/java/xiao/bu/tv/LegacyTlsSocket.java` 来自作者自己的 NativeWasmTv / FusionTV 本地集成；公开版 `app/src/main/jniLibs/armeabi-v7a/libntvtls.so` 从下述随仓 native 源码以 NDK r23c、android-19 / armeabi-v7a 重新构建。
公开补充的 TLS native 源码来自 https://github.com/dhicoc/NativeWasmTv ，提交 `2645848df83563b405ca3c4386dde2b6ba89560f` 的 `native/tls/`。
桥接的 JNI 类名保留 `xiao.bu.tv.LegacyTlsSocket`。Mbed TLS vendor 为 3.6.7，使用 Apache-2.0 许可选项，许可正文保留于 vendor/LICENSE 和应用 assets/licenses/mbedtls.txt。
作者在本项目公开的自有修改和桥接代码按 GPL-3.0 分发；第三方已有许可不变。

## 发布版配置差异

原本地包含私人订阅地址并将非预置配置重置为预置地址。公开源码将 Config.vod() 恢复为保留数据库配置、未配置时返回空配置的行为，私人订阅及备用影视源不进入公开快照。Gradle Wrapper 从旧版 7.4.2 对齐实际构建使用的 8.9。

