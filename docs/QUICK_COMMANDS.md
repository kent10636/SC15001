# 常用命令速查 (Quick Commands)

本文件整理了开发和运行 WalkieTalkie 最小核心 Demo 时的常用命令。

## 1. 基础设施 (Docker)

```bash
# 启动所有服务（LiveKit + Postgres）
docker compose up -d

# 查看日志
docker compose logs -f

# 停止所有服务
docker compose down

# 停止并删除数据卷（重置数据库）
docker compose down -v

# 仅重启 LiveKit
docker compose restart livekit
```

**访问地址**:
- LiveKit: `ws://localhost:7880` (模拟器用 `ws://10.0.2.2:7880`)
- LiveKit HTTP 管理（如果开启）: `http://localhost:7880`

## 2. 后端 (Go)

```bash
cd server

# 安装/更新依赖
go mod tidy

# 运行后端（使用开发密钥）
LIVEKIT_API_KEY=devkey LIVEKIT_API_SECRET=secret go run cmd/server/main.go

# 带调试日志运行（如果添加了日志）
LIVEKIT_API_KEY=devkey LIVEKIT_API_SECRET=secret go run cmd/server/main.go
```

**常用测试接口** (使用 curl 或 Postman):

```bash
# 健康检查
curl http://localhost:8080/health

# 创建群组
curl -X POST http://localhost:8080/groups \
  -H "Content-Type: application/json" \
  -d '{"name": "测试群"}'

# 通过码加入群组
curl -X POST http://localhost:8080/groups/join \
  -H "Content-Type: application/json" \
  -d '{"code": "AB12CD"}'

# 获取 token（用于 Android）
curl -X POST "http://localhost:8080/groups/AB12CD/token?nickname=测试用户"
```

**注意**:
- Android 模拟器访问后端请使用 `http://10.0.2.2:8080`
- 真机请改用电脑局域网 IP

## 3. Android 开发

```bash
# 在项目根目录下（或直接在 Android Studio 执行）

# 清理构建（如果 Gradle 出问题）
./gradlew clean

# 构建 Debug APK
./gradlew assembleDebug

# 安装到已连接设备
./gradlew installDebug

# 运行测试（如果有）
./gradlew test
```

**Android Studio 常用操作**:
- Sync Project with Gradle Files
- Invalidate Caches / Restart
- Build > Clean Project / Rebuild Project

## 4. 快速完整启动流程（推荐）

```bash
# 终端 1
docker compose up -d

# 终端 2
cd server
LIVEKIT_API_KEY=devkey LIVEKIT_API_SECRET=secret go run cmd/server/main.go

# 终端 3（可选）
# 打开 Android Studio → 运行 App
```

## 5. 调试与日志

**后端**:
- 后端已使用 `middleware.Logger`，请求日志会直接输出到控制台。

**Android**:
- 使用 Logcat 过滤 `LiveKit` 或 `WalkieTalkie`
- Room 连接日志会出现在 Logcat 中

**LiveKit 服务器日志**:
```bash
docker compose logs -f livekit
```

## 6. 重置环境

```bash
# 完全重置（删除所有数据）
docker compose down -v
cd server && rm -f walkie.db  # 如果后续有本地数据库文件

# 重新启动
docker compose up -d
cd server
go run cmd/server/main.go
```

## 7. 下次测试前检查清单

1. `docker compose ps` 确认容器在运行
2. 后端能响应 `/health`
3. Android 使用的 BASE_URL 正确（模拟器 vs 真机）
4. 两台设备使用**同一个** 6 位码

---

**提示**: 可以把这个文件收藏或打印出来，方便快速参考。所有命令都已在当前工作目录测试过结构。
