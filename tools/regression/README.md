# API19 回归检查

先构建 `:app:assembleLeanbackDebug`，启动独立、已完成开机的 API19 测试模拟器，配置 `JAVA_HOME`。

```powershell
./tools/regression/run-review-checks.ps1 -AndroidSdkRoot '<SDK>' -Serial '<adb 序列号>'
```

脚本需要 PowerShell 7、SDK35、Build Tools36；可用 `-AdbPath` / `-AdbPort` 指定旧 SDK 的 ADB。使用和目标 Debug APK 相同的默认调试密钥，密钥只在本机读取，不进入测试包或仓库。脚本安装目标包及独立测试包，测试输出在 `.work/review-regression/result.log`。

覆盖实际 Players 初始化/释放状态、用受控媒体时钟驱动实际进度控件、自动换源搜索对全部11个本机 CMS 站点的真实 HTTP 请求，以及 native TLS 到 example.com 的真实握手和 HTTP 响应。fixture 不写应用配置数据库，搜索结束恢复原内存配置。测试需要外网 TLS 连通；网络失败须按具体异常归因。

这是 Debug APK 集成检查，不替代混淆 Release 的设备测试、真实媒体解码/定位或所有外部源可播率验收。Release 的 JNI 裁剪另用 R8 seeds/mapping/usage 检查。

还可使用 `-TargetApk '<已用同一调试证书签名的 Release APK>' -MinifiedNetworkClass '<mapping.txt 中 com.github.catvod.net.OkHttp 对应的类名>'` 运行混淆版 TLS 专项：通过应用实际 HTTP 客户端取得 socket factory，验证保留 JNI 回调后的握手与响应。该模式只检查 TLS，不运行依赖原类名的进度/搜索检查。
