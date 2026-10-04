# 变更记录

## v2.5.0-kitkat.2 · 2026-10-04

- 修复 IJK 已初始化却被判为释放，恢复进度控件的位置、时长和缓冲刷新。
- 播放页快速搜索/自动换源遍历全部合格站点，保留两线程并发，移除前十站截断。
- 在 TLS Java 回调上声明 @Keep，防止 R8 删除 native GetMethodID 需要的方法。
- GitHub 侧载构建精确禁用 ExpiredTargetSdkVersion 商店策略检查，其余 lint 和 targetSdk28 保留。
- gradlew 规范为 LF / 100755，用 .gitattributes 保持换行约定。
- versionCode251，Debug 签名沿用前版；新增可复用的 API19 集成检查入口 tools/regression/。

## 2026-10-04 · 首次公开源码

相对 OK 影视 KitKat f4a5204：

- 构建迁移至 AGP 8.7.3 / SDK 35 / Gradle 8.9；minSdk 19、targetSdk 28、ARMv7、leanback 单变体和 MultiDex。
- 固定旧系统可用的 OkHttp / Okio 等依赖，补 javax.servlet 本地 JAR；替换现代 API 调用。
- 增加旧系统 TLS 适配和配置字符串容错转换 CoerceStringAdapter。
- 默认 IJK 播放，低内存探测/缓冲调优；视频失败尝试换线路/站点。
- 海报下采样、RGB_565、缓存与池各 16MiB；聚合搜索 5 线程及刷新节流。
- 双击返回确认退出、首页推荐自动聚焦，名称“极速影视”，中文资源打包。
- X5 转系统 WebView，移除 X5 安装包、Go 视频代理和 NewPipe 实现；DLNA 投屏、部分协议提取器、弹幕与轨道界面等兼容裁剪。
- 公开版去除私人订阅地址和预埋影视源，保留自填配置；补充来源、GPL 与 native TLS 源码。

历史开发记录中的内存和起播数据来自特定模拟器及测试源，不作为所有设备的性能保证。
