# 第三方来源与许可

- **OK 影视 / FongMi / CatVod**：直接基线为 okcaptain/TV kitkat `f4a5204f49bc311b086eda75345663ac5d760061`，GPL-3.0。正文见 LICENSE.md，原作者权利保持不变。
- **NativeWasmTv TLS 桥接**：作者 dhicoc 的桥接源码，在本项目按 GPL-3.0 公开；完整 vendor 和重建配置见 native/tls/。
- **Mbed TLS 3.6.7**：Apache-2.0 许可选项。见 native/tls/vendor/LICENSE 和 app/src/main/assets/licenses/mbedtls.txt；原始发行地址 https://github.com/Mbed-TLS/mbedtls/releases/tag/mbedtls-3.6.7 。
- **IJKPlayer / FFmpeg**：上游自带 Java 包装及 armeabi-v7a、arm64-v8a 预编译库；通常分别涉及 LGPL 及具体 FFmpeg 构建选项。上游源码 https://github.com/bilibili/ijkplayer 。本仓库未声称这些 SO 是本次从完整 native 源码重建，具体构建配置和对应源应向原分发方核实。
- **QuickJS 包装及 quickjs.aar**：沿用上游二进制组件和 wang.harlon.quickjs 依赖，相关声明以各组件分发包为准；不是本项目自研。
- **javax.servlet-3.0.0.jar**：org.eclipse.jetty.orbit:javax.servlet:3.0.0.v201112011016，保留 JAR 内 META-INF 许可信息。
- **其他上游预编译组件**：app/libs 中 DLNA AAR，以及 forcetech、jianpian、thunder、zlive 等模块的 SO/AAR 继续保留上游形态。其中部分功能未被应用启用；不因为仓库 GPL 许可证而改变第三方文件自身权利。
- **Maven / AndroidX / Glide / Jetty 等依赖**：版本见各模块 build.gradle，许可见各自项目和下载包，不将其版权归于本项目。

公开源码整理未完成所有上游预编译库的许可证与对应源码全面审计，因此本次不发布 APK 二进制资产。开发者可自行构建和评估；未来二进制发布应先补齐所分发组件的对应源码、配置与许可要求。
