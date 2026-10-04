# Agent Note: 公开版 ARM32 测试 APK 发行

Status: implemented

## Problem

用户明确要求 Release 提供可安装 APK。仅附源码不能满足直接安装使用的需求，原开发包又包含私人订阅配置。

## Decision

现有 v2.5.0-kitkat.1 Release 附公开源码已构建的 leanbackDebug APK，名称明确包含 arm32-debug，并附独立 APK-SHA256SUMS.txt。发行说明和 README 标明 Debug 构建、调试证书、API19/ARM32 及尚未重新完成设备播放验收。源码标签与当前应用模块无代码差异，原固定源码资产和标签保持不变。

## Alternatives considered

改用新生产签名和 R8 Release 构建可提供长期生产升级通道，但会同时引入新签名及未经本轮运行验证的混淆产物。当前发布已经构建检查通过的测试包，并明确签名和验证边界。

继续仅发布源码能保留初始发行形态，但用户明确要求直接安装包；依此补齐 APK 资产，保留第三方来源信息缺口，不将其写成已完成全面审计。

## Consequences

用户可以直接下载安装并自行配置接口。包保留 com.fongmi.android.tv 和 versionCode250/versionName2.5.0，签名不同的同包应用不能直接覆盖。APK 的 v1/v2 签名在 minSdk19 条件验证通过，解压条目扫描未检出原私人令牌，APK 的 TLS SO 与仓库随源码重建库哈希一致。验证没有扩展为真机播放和逐站可播率结论。

## Audit

本篇部分取代 [首次公开源码边界](2026-10-04-public-source.md) 的“暂不上传 APK”决定，其余源码整理与隐私边界继续有效。两篇互链，既有第三方来源与验证记录保留。

[审查修复版](../bug-fix/2026-10-04-review-regressions.md) 延续 Debug 签名和公开配置边界，以新标签 v2.5.0-kitkat.2 和 versionCode251 提供可覆盖升级的安装包，保留旧发行资产。
