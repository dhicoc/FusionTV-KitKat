# TLS 桥接源码

本目录来自 dhicoc/NativeWasmTv 提交 2645848df83563b405ca3c4386dde2b6ba89560f。Mbed TLS 3.6.7 vendor 未修改，选用 Apache-2.0；许可见 vendor/LICENSE。

本项目在 API 19 ARMv7 使用此桥接，为 OkHttp 提供 TLS 1.2。JNI 类名为 xiao.bu.tv.LegacyTlsSocket，Java 代码位于 catvod 模块。应用日常构建使用 app/src/main/jniLibs/armeabi-v7a/libntvtls.so。

重建示例（NDK r23c）：

```powershell
./tools/build-tls.ps1 -NdkRoot '<NDK 路径>'
./gradlew.bat :app:assembleLeanbackDebug
```

脚本明确指定 android-19 / armeabi-v7a，不依赖宿主本机绝对路径。源码实现 HTTPS client / TLS 1.2，不实现 HTTP/2、ALPN 或 TLS 1.3。证书校验交由 Java 提供的 TrustManager，主机名策略由 OkHttp 决定；应用继承的宽松策略见根 README。
