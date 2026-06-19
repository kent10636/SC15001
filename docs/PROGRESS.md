# WalkieTalkie 项目进展记录

**记录日期**: 2026-06-19  
**当前阶段**: 最小可验证核心 Demo（已暂停测试）  
**状态**: 代码实现基本完成，等待设备进行端到端验证

## 项目目标（本次迭代）
按照用户确认的最小范围，实现一个**可验证的对讲机核心流程**：

1. 匿名输入昵称
2. 创建群组（自动生成 6 位加入码）或通过码加入
3. 进入房间
4. **按住大按钮说话**（真实通过 LiveKit 传输语音）
5. 松开停止
6. 多设备可使用相同码进入同一个房间进行对讲

**UI 风格**: Material You（动态配色）

## 已完成的工作

### 1. 项目结构整理
- 将原始 SC15001 旧项目移至 `reference/sc15001-legacy/`
- 建立干净的现代项目结构：
  - `server/` - Go 后端
  - `android/` - Kotlin + Jetpack Compose 客户端
  - `docker-compose.yml` + `livekit.yaml`

### 2. 后端 (Go)
- 使用 `chi` 路由（符合之前确认的轻量框架）
- 内存存储群组（`internal/group/group.go`）
- 6 位字母数字加入码自动生成
- 核心接口：
  - `POST /groups` - 创建群组
  - `POST /groups/join` - 通过 6 位码加入
  - `POST /groups/{code}/token?nickname=xxx` - 获取 LiveKit token
- LiveKit token 生成封装（使用 `server-sdk-go`）
- 支持模拟器（10.0.2.2）和真机访问

### 3. Android 客户端
- **主题**: Material You（支持 Android 12+ 动态颜色）
- **导航**: Navigation Compose
  - `NicknameScreen` - 昵称输入
  - `GroupsScreen` - 创建/加入群组（Material You 卡片风格）
  - `RoomScreen` - 房间界面 + PTT 按钮
- **真实集成**:
  - Retrofit 调用后端 API
  - LiveKit Android SDK 连接房间
  - PTT 实现：`localParticipant.setMicrophoneEnabled(true/false)`
  - 大圆形按钮使用 `pointerInput` 实现按住/松开手势
- 基础成员列表展示

### 4. 基础设施
- `docker-compose.yml` 包含：
  - `livekit/livekit-server:latest`
  - Postgres（为后续持久化预留）
- `livekit.yaml` 基础配置
- 开发密钥：`devkey` / `secret`

### 5. 文档
- `README.md` - 详细运行说明 + 当前状态
- `docs/plan.md` - 原始完整计划 + 进展补充
- `docs/PROGRESS.md` - 本文件，详细进展快照
- `docs/RESUME_CHECKLIST.md` - 下次恢复 checklist
- `docs/KNOWN_ISSUES.md` - 已知问题列表
- `docs/QUICK_COMMANDS.md` - 常用命令速查
- `docs/TODOS.md` - 待办事项（带优先级）

## 当前限制 / 未验证部分

**重要**: 因暂时没有可用的测试设备（模拟器或真机），以下内容**尚未进行端到端验证**：

- 实际语音传输是否正常工作（两台设备互听）
- LiveKit 连接稳定性
- 麦克风权限请求流程
- 房间内其他参与者实时显示
- 网络切换、重连、断线处理
- 蓝牙耳机音频路由
- 多设备同时说话时的表现

**已知待办**:
- Android 运行时权限请求（RECORD_AUDIO）
- 更好的连接状态提示和错误处理
- 房间内参与者列表的实时更新（监听 remoteParticipants）
- 后端持久化存储（目前纯内存，重启后群组消失）
- UI 微调（按钮反馈、说话波形指示等）
- 离开房间时正确清理 LiveKit 连接

## 下次恢复工作建议流程

1. **启动基础设施**
   ```bash
   docker compose up -d
   ```

2. **启动后端**
   ```bash
   cd server
   go mod tidy
   LIVEKIT_API_KEY=devkey LIVEKIT_API_SECRET=secret go run cmd/server/main.go
   ```

3. **启动 Android**
   - Android Studio 打开 `android/` 文件夹
   - Sync 项目
   - 运行到模拟器或真机

4. **验证 checklist**（推荐按顺序）:
   - [ ] 两台设备能成功创建/加入同一个 6 位码群组
   - [ ] 进入房间后状态显示 "已连接"
   - [ ] 按住 PTT 按钮时一方说话，另一方能听到声音
   - [ ] 松开后声音停止
   - [ ] 成员列表基本显示
   - [ ] 离开房间流程正常
   - [ ] 后端重启后群组消失（当前内存特性）

## 关键配置说明

**Android 访问后端地址**:
- 模拟器: `http://10.0.2.2:8080/` （已在 `ApiClient.kt` 中默认）
- 真机: 需要改成电脑的局域网 IP（如 `http://192.168.31.100:8080/`）

**LiveKit 访问**:
- 模拟器: `ws://10.0.2.2:7880`
- 真机: 需确保设备能访问到 LiveKit 容器暴露的地址

## 未来规划方向（参考原始计划）

- 完善权限 + 音频焦点处理
- 真实持久化 + 简单用户系统
- 参与者说话状态指示
- "面对面建群" 等扩展功能（后期）
- 蓝牙耳机专项支持

---

**记录人**: Grok  
**最后更新**: 2026-06-19

如需恢复，直接参考本文件 + README.md 即可快速上手。
