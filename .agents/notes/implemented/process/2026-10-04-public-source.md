# Agent Note: Android 4.4 优化版公开源码边界

Status: implemented

## Problem

开发工作区包含两个应用、私人订阅令牌、模拟器记录和本机 SDK。直接公开整个目录会泄漏私人配置，也无法给贡献者提供清晰构建入口。

## Decision

公开最后交付的 OK-TV-kitkat 源码快照，仓库名 FusionTV-KitKat。保留 GPL-3.0 和上游来源、现有第三方文件许可。私人订阅改为用户配置，已有配置保持有效。Gradle wrapper 对齐 8.9。TLS Java 桥接与 native/tls 对应源码、Mbed TLS 许可证一并提供。公开目录独立于原开发树。

## Alternatives considered

直接公开整个 FusionTV44 项目能保存完整过程，但混合两个应用、私人令牌和本机测试数据，发布边界不清晰。使用独立源码快照，并记录上游精确提交和差异，避免历史数据泄露。

## Consequences

公开目录独立于原开发树，用户的原 APK 和订阅保持原状；新安装公开版需要一次手动配置。README 提供真实二维码配置流程、原服务订阅模板及可复制备用地址。接口历史结果与 2026-10-04 桌面 HTTP/JSON 复核分开记录；89 个站点结构可解析不等于逐站播放全部通过。

TLS 库由随仓 native 源码以 NDK r23c / android-19 / armeabi-v7a 重建，源码和库同步。保留本地 Java 桥接对 OkHttp cipher/protocol 列表的兼容处理。公开版完成 Gradle Debug 构建；本次不启动模拟器，不扩展历史 API19 播放结论。上游其他预编译组件尚缺完整来源审计，故仅发布源码仓库，不上传旧 APK 或新二进制发行包。

## Audit

目标项目没有已有 .agents/notes；公开目录首次建立本篇过程决定，无冲突旧笔记。现有 THIRD_PARTY_NOTICES 属于旧自研 FusionTV 路线，不迁入新仓库；本仓库记录实际 OK KitKat 基线及作者自有 TLS 公开授权。
