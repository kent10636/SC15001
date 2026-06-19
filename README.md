# Walkie-Talkie - 最小可验证核心 Demo

[![Go](https://img.shields.io/badge/Go-1.22-00ADD8?logo=go)](https://go.dev)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-Compose-3DDC84?logo=android)](https://developer.android.com/jetpack/compose)
[![Stars](https://img.shields.io/github/stars/kent10636/walkie-talkie?style=social)](https://github.com/kent10636/walkie-talkie/stargazers)
[![Last Commit](https://img.shields.io/github/last-commit/kent10636/walkie-talkie)](https://github.com/kent10636/walkie-talkie/commits/main)

基于 2015 年规划文档的现代实现。当前目标：**最小可验证的核心对讲机 Demo**。

**核心流程**：
1. 输入昵称（匿名）
2. 创建群组 或 输入6位码加入
3. 进入房间 → **按住大按钮说话**（真实通过 LiveKit 传输）
4. 多设备使用相同码即可实时对讲

## 运行步骤（推荐）

### 1. 启动基础设施
```bash
docker compose up -d
```
- LiveKit: ws://localhost:7880
- Postgres（当前后端暂未使用）

### 2. 启动 Go 后端
```bash
cd server
go mod tidy
LIVEKIT_API_KEY=devkey LIVEKIT_API_SECRET=secret go run cmd/server/main.go
```
后端默认监听 :8080

**重要**：Android 模拟器访问本机用 `10.0.2.2:8080`（已在代码中配置）。

真实手机测试时把 ApiClient 中的 BASE_URL 改成你电脑的局域网 IP（如 http://192.168.1.100:8080/）。

### 3. 运行 Android App
- 用 Android Studio 打开 `android/` 文件夹
- Sync Gradle
- 运行到模拟器或真机（需要麦克风权限）

### 4. 测试
- 两台设备/模拟器输入相同昵称后，用同一个6位码加入同一个群
- 按住大按钮说话，另一端应该能听到声音

## 当前状态（2026-06-19 记录）

**已完成的最小可验证核心 Demo 代码结构：**

- **匿名昵称登录**：启动后输入昵称即可使用（MVP 灵活认证）
- **群组管理（内存）**：
  - 创建群组 → 自动生成 6 位字母数字加入码（如 AB12CD）
  - 通过 6 位码加入群组
- **Material You 风格界面**（动态配色）：
  - 昵称输入页
  - 群组列表/创建/加入页
  - 房间页（大圆形 PTT 按钮）
- **实时对讲核心**：
  - 进入房间后自动从后端获取 LiveKit token
  - 使用 LiveKit Android SDK 连接房间
  - 按住大按钮 → `localParticipant.setMicrophoneEnabled(true)`
  - 松开 → `setMicrophoneEnabled(false)`
  - 基础成员列表显示
- **后端**：
  - Go + chi 路由
  - 内存存储群组
  - token 生成接口（使用 server-sdk-go）
  - 支持模拟器（10.0.2.2）和真机

**基础设施**：
- `docker-compose.yml` 已包含 livekit/livekit-server + postgres
- `livekit.yaml` 配置就绪

**未测试**：
- 因为手边暂时没有可用的测试设备（模拟器/真机），核心语音通话功能尚未端到端验证。
- Android 权限请求、错误处理、参与者实时更新等细节可后续完善。

**下次继续方向**（可参考）：
- 实际运行测试 + 修复问题
- 添加麦克风权限请求
- 改进房间内参与者列表（监听 remoteParticipants）
- 后端持久化 + 简单用户关联
- 蓝牙耳机支持、UI 微调

更多原始计划见 `docs/plan.md`。

### 补充文档（推荐阅读）
- [docs/README.md](docs/README.md) - 文档总索引
- [PROGRESS.md](docs/PROGRESS.md) - 详细当前进展快照
- [RESUME_CHECKLIST.md](docs/RESUME_CHECKLIST.md) - 下次恢复时的完整 checklist
- [QUICK_COMMANDS.md](docs/QUICK_COMMANDS.md) - 常用命令速查
- [KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) - 已知问题与限制列表
- [TODOS.md](docs/TODOS.md) - 待办事项列表（含优先级）

---

**如何快速恢复**：
1. `docker compose up -d`
2. `cd server && go mod tidy && LIVEKIT_API_KEY=devkey LIVEKIT_API_SECRET=secret go run cmd/server/main.go`
3. Android Studio 打开 `android/` 文件夹运行

准备好测试设备后随时继续。
