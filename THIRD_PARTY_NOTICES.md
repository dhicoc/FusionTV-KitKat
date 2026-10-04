# 第三方来源与许可

- **OK 影视 / FongMi / CatVod**：直接基线为 okcaptain/TV kitkat `f4a5204f49bc311b086eda75345663ac5d760061`，GPL-3.0。正文见 LICENSE.md，原作者权利保持不变。
- **NativeWasmTv TLS 桥接**：作者 dhicoc 的桥接源码，在本项目按 GPL-3.0 公开；完整 vendor 和重建配置见 native/tls/。
- **Mbed TLS 3.6.7**：Apache-2.0 许可选项。见 native/tls/vendor/LICENSE 和 app/src/main/assets/licenses/mbedtls.txt；原始发行地址 https://github.com/Mbed-TLS/mbedtls/releases/tag/mbedtls-3.6.7 。
- **IJKPlayer / FFmpeg**：上游自带 Java 包装及 armeabi-v7a、arm64-v8a 预编译库；通常分别涉及 LGPL 及具体 FFmpeg 构建选项。上游源码 https://github.com/bilibili/ijkplayer 。本仓库未声称这些 SO 是本次从完整 native 源码重建，具体构建配置和对应源应向原分发方核实。
- **QuickJS 包装及 quickjs.aar**：沿用上游二进制组件和 wang.harlon.quickjs 依赖，相关声明以各组件分发包为准；不是本项目自研。
- **javax.servlet-3.0.0.jar**：org.eclipse.jetty.orbit:javax.servlet:3.0.0.v201112011016，保留 JAR 内 META-INF 许可信息。
- **其他上游预编译组件**：app/libs 中 DLNA AAR，以及 forcetech、jianpian、thunder、zlive 等模块的 SO/AAR 继续保留上游形态。其中部分功能未被应用启用；不因为仓库 GPL 许可证而改变第三方文件自身权利。
- **Maven / AndroidX / Glide / Jetty 等依赖**：版本见各模块 build.gradle，许可见各自项目和下载包，不将其版权归于本项目。

Release 提供公开源码构建的 ARM32 Debug 测试 APK；相关组件仍按各自许可证分发，作者自有修改按 GPL-3.0 公开。上游预编译库的确切构建配置、对应源码定位及完整许可审计仍有信息缺口，本次 APK 发布不表示这些缺口已解决；本文件保留来源线索，后续补全时同步更新。
