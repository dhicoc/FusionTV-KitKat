# Agent Note: 修复 IJK 裁剪与公开构建回归

Status: implemented

## Problem

代码审查确认 IJK 单引擎仍被双引擎释放条件判为释放、播放页换源仅遍历十站、JNI 回调被 R8 删除，以及 Release 商店 targetSdk 检查和 Linux wrapper 格式阻断构建。

## Decision

播放器释放状态按实际使用的引擎判断；播放页仍保留两线程并发，但搜索所有合格站点。TLS 模块在 JNI 反调 Java 方法上声明 @Keep，让混淆保留契约随库源码传播。侧载构建仅禁用 ExpiredTargetSdkVersion 检查，保留 targetSdk28 和其余 lint 检查。gradlew 固定 LF 和 Git 执行位。修复包增加 versionCode，沿用已有 Debug 签名发布新标签，不覆盖旧资产。

## Alternatives considered

为恢复旧释放条件重新初始化 Exo 可让进度恢复，但违背 IJK 单引擎的低内存路线；只检查当前引擎更准确。提高换源截断数量可增加命中，却仍然永久遗漏后续站点，故保留完整队列。把 targetSdk 升到新商店门槛可消除 lint 报错，却会改变旧应用权限和组件行为，本项目在 GitHub 侧载，精确处理商店检查。关闭 R8 可绕过 JNI 裁剪，但没有修复 native 回调契约，保留混淆并声明方法边界。

## Testing

JDK21 / Gradle8.9 下 Debug/Release 构建通过；R8 mapping/seeds 保留两个 native 回调，usage 不再列为删除。独立 API19 模拟器的 Debug 集成检查通过：IJK 初始化/释放状态正确，受控媒体时钟驱动实际 CustomSeekView 显示 00:45 / 02:00 和缓冲 60000ms，自动换源实际请求全部11个本机CMS站点，原生 TLS 完成真实握手和 HTTP 响应。混淆 Release 另以本机调试证书签名，通过应用实际混淆 HTTP 客户端验证同一 TLS 链路；该签名产物仅用于验证。

源码索引为 gradlew LF/100755，Git Bash 语法检查通过。发行 Debug APK 的 API19 v1/v2 签名有效，与前版证书一致；versionCode251/versionName2.5.0-kitkat.2，解压全部 ZIP 条目扫描未检出原私人令牌。测试入口和边界见 [回归检查](../../../../tools/regression/README.md)，完整公开结果见 [验证记录](../../../../docs/VALIDATION.md)。

最终发行文件再次通过 ADB 安装与同组回归。首页启动、遥控导航到设置和点播配置对话框通过，启动导航日志未检出崩溃/ANR/OOM标记。

## Consequences

完整站点队列扩大覆盖范围，慢站点仍可能延迟后续任务，但并发保持两线程。Debug 与混淆构建验证应分别记录，不将编译或静态检查声称为所有真机播放验收。

## Audit

已有 [公开源码边界](../../implemented/process/2026-10-04-public-source.md) 和 [测试 APK 发行](../../implemented/process/2026-10-04-test-apk-release.md) 负责发布范围，本篇只补充运行与构建缺陷，部分重叠互链，不取代既有签名和隐私决定。
