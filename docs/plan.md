# Walkie-Talkie Implementation Plan (SC15001-based)

> **2026-06-19 更新**：已按照用户确认的“最小可验证的核心 demo”范围完成了主要代码实现（昵称 + 6位码群组 + LiveKit PTT）。因暂无测试设备，暂时停止编码，重点记录当前进展。详见下方 “Implementation Progress” 章节及根目录 README.md。

## Context

The current workspace (`/Users/kent/Projects/walkie-talkie`) contained no code when the task started. The only "project documentation and framework" available is the legacy `SC15001` reference that was synced locally:

- Old Eclipse/ADT Android skeleton (API 19, single empty MainActivity + "Hello World" layout).
- Primary source of truth: handwritten planning notes photo at `SC15001/Doc/IMG_20151224_032618.jpg` (created/saved ~2014-2015).
- The notes describe a **group-based walkie-talkie (对讲机) voice chat application** with clients and server responsibilities.

User moved the reference into the workspace and explicitly requested:
- All future operations stay inside the current working directory.
- Create a **complete project implementation plan**.
- Implementation details and requirements can be discussed interactively (multiple clarifications have already occurred).

### Key Decisions from Interactive Clarification
- **Client**: Android only, modern Kotlin + **Jetpack Compose**.
- **Voice transport**: Self-hosted **LiveKit** (WebRTC SFU) — chosen for quality, Android SDK maturity, and PTT suitability.
- **App backend**: **Go** (pairs naturally with LiveKit server-sdk-go for token issuance).
- **MVP scope**: Groups only (create + join by code, small active groups 2-20 speakers). Classic **PTT** (press-to-talk) focus. Friends/contacts, QR code scanning, and "面对面建群" deferred to later phases.
- **Auth**: Flexible for MVP (simple identity or dev auth to start; phone binding/SMS can be added without blocking core voice).

The goal is a focused, working modern re-implementation of the spirit of the 2015 planning doc inside this workspace.

## Recommended Approach

**Overall Architecture (Client + App Backend + Media Plane)**

```
Android App (Compose)
   ↕ REST/JSON (auth, groups, membership, get token)
Go App Backend (users, groups, codes, token minting)
   ↕ LiveKit SDK (server-sdk-go)
LiveKit Server (SFU - handles actual audio routing)
   ↕ WebRTC (low-latency audio)
Other Android clients in the same LiveKit room
```

**Why this stack**
- LiveKit makes real-time group audio (with selective publishing for PTT) dramatically simpler than raw WebRTC or custom UDP.
- Small groups + PTT map perfectly: clients only publish their mic track while the PTT button is held.
- Go backend keeps things lightweight and consistent with LiveKit's native language.
- Jetpack Compose is current standard for new Android UIs.
- Self-hosted (Docker) keeps full control and no per-minute billing during development.

**MVP Feature Scope (derived directly from planning notes + decisions)**

Client features (Android):
- Basic user identity (register/login stub)
- Create group (name)
- List my groups
- Join group using a short code / slug
- Group room screen:
  - Big prominent PTT button (press/hold to speak)
  - List of participants (with speaking indicator if possible)
  - Leave group
- Proper Android permissions + Bluetooth headset considerations

Server responsibilities (Go):
- User accounts (minimal at first)
- Group creation + persistence + invite code generation
- Membership
- Issue LiveKit access tokens (VideoGrant for specific room mapped to group)
- (Optional early) Basic group metadata

Deferred (per decisions):
- Friends / contacts list
- QR codes for join/add
- Avatars, signatures, "面对面建群"
- Full-duplex / always-on mode
- Large groups (> ~20 simultaneous)

**Concrete MVP API Surface (suggested, Go backend)**

Example REST endpoints (protected by simple auth):
- `POST /auth/login` or `/auth/anonymous` → returns user + token
- `POST /groups` { "name": "Team Alpha" } → { id, code: "AB12CD", ... }
- `GET /groups` → list user's groups
- `POST /groups/join` { "code": "AB12CD" }
- `POST /groups/:id/leave`
- `POST /groups/:id/token` → { livekitUrl: "wss://...", token: "eyJ..." }

The returned LiveKit token uses `VideoGrant{RoomJoin: true, Room: "group-<id>"}` (generated via `livekit/server-sdk-go`).

In Android, the client uses the token + url to:
```kotlin
val room = Room(context)
room.connect(livekitUrl, token)
...
// PTT
room.localParticipant.setMicrophoneEnabled(true)   // talk
room.localParticipant.setMicrophoneEnabled(false)  // stop
```

Base the room UI heavily on official samples:
- https://github.com/livekit/client-sdk-android/tree/main/sample-app-compose
- Components: https://github.com/livekit/components-android

## Proposed Project Structure (inside working directory)

```
/Users/kent/Projects/walkie-talkie/
├── reference/
│   └── sc15001-legacy/          # archived original (SC15001 content moved here)
│       ├── Doc/IMG_20151224_032618.jpg   # ← primary requirements photo
│       └── ...
├── docs/
│   └── plan.md                  # this document (or future ADRs)
├── docker-compose.yml           # postgres + livekit-server + (later) your backend
├── server/                      # Go backend
│   ├── cmd/server/main.go
│   ├── internal/
│   │   ├── api/                 # HTTP handlers (groups, auth, token)
│   │   ├── auth/
│   │   ├── group/
│   │   ├── livekit/             # token generation wrapper
│   │   └── db/                  # postgres models, migrations
│   ├── go.mod
│   └── go.sum
├── android/                     # Android client (Android Studio project)
│   ├── app/
│   │   ├── src/main/java/com/walkietalkie/app/...
│   │   ├── src/main/res/
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│   # Use modern Gradle + Kotlin + Compose
├── scripts/                     # dev helper scripts (start stack, seed data, etc.)
└── .gitignore
```

(Alternative naming: `client-android/` + `backend/` is also acceptable. The above keeps things clear.)

## Critical Files & Areas to Create/Modify

**Infrastructure**
- `docker-compose.yml` (livekit-server image + postgres + optional pgadmin)
- `server/go.mod` + basic main with HTTP server + LiveKit client

**Backend (Go)**
- `server/internal/livekit/token.go` — `getJoinToken(apiKey, secret, roomName, identity)`
- Group and membership models + CRUD
- Endpoint `POST /groups/{id}/token` (auth user → return LiveKit JWT + ws URL)
- Simple code generation for groups (short, unique, human-enterable)

**Android Client**
- New Android project using Kotlin + Jetpack Compose (target modern SDK, minSdk ~26+)
- Dependencies: `livekit-client-sdk-android`, Compose, Retrofit/OkHttp (or Ktor client), Kotlin Coroutines, ViewModel
- Key packages/screens:
  - `ui/screens/GroupsListScreen.kt`
  - `ui/screens/GroupRoomScreen.kt` (big PTT button + participants)
  - `data/remote/LiveKitRepository.kt` or direct Room usage
  - `MainActivity.kt` + navigation (Compose Navigation)
- Permission handling: `RECORD_AUDIO`, `BLUETOOTH_CONNECT`, etc.
- Audio focus / Bluetooth SCO handling helpers

**Integration points**
- Android obtains token from Go backend, then connects via LiveKit Android SDK `Room`:
  ```kotlin
  // Rough shape (see official samples)
  val room = Room(appContext, rtcConfig)
  room.connect(url, token)
  // PTT control:
  // room.localParticipant.setMicrophoneEnabled(true)  // start talking
  // room.localParticipant.setMicrophoneEnabled(false) // stop
  ```
- Observe `room.remoteParticipants` or track publications for speaking UI indicators.

## Existing Code / Assets to Reuse

- **Primary**: `reference/sc15001-legacy/Doc/IMG_20151224_032618.jpg` — the handwritten requirements (client group flows, PTT button behavior, server responsibilities, "按住说话", two work modes, etc.).
- Old skeleton (`AndroidManifest.xml`, package name ideas) only for historical reference (do **not** reuse the Java code or Ant build).
- The original high-level feature names and flow (群组创建/加入/退出, 群组列表滑动进入, 个人, 对讲机语音输入, 服务器端群组+语音转发).

No substantial reusable production code exists.

## Phased Implementation Roadmap (suggested order)

**Phase 0: Housekeeping & Setup (in working dir)**
- Create `reference/sc15001-legacy/` and move current `SC15001/` contents there (clean root).
- Create `docker-compose.yml` with livekit-server + postgres.
- Initialize Go module + minimal HTTP server that can mint tokens.
- Initialize fresh Android project (Compose, modern AGP) in `android/`.

**Phase 1: Backend Foundation**
- User model + basic auth stub (hardcoded or simple JWT/dev token for speed).
- Group model + create / list / join-by-code.
- Token issuance endpoint that returns LiveKit URL + JWT for a group's room.
- DB migrations.

**Phase 2: Android Scaffold + Navigation**
- Project setup, theme, basic Compose navigation.
- Groups list screen + create group flow (calls backend).
- Join-by-code flow.
- Permissions screen / rationale.

**Phase 3: Core Voice (the heart of the project)**
- Integrate LiveKit Android SDK.
- Connect/disconnect from LiveKit room on group enter/leave.
- Implement PTT button:
  - On press: enable mic publish + visual "transmitting".
  - On release: disable.
- Show remote participants (at minimum names/identities).
- Basic audio routing confirmation (you should hear others when they PTT).

**Phase 4: Polish & Hardening**
- Participant list with speaking state (subscribe to track publications or data messages).
- Error handling, reconnect, token refresh.
- Leave group (remove membership + disconnect).
- Group settings stub (name, leave).
- Proper Android lifecycle, wake locks if needed, Bluetooth headset support.
- Basic UI/UX matching "walkie-talkie" feel (large tappable button, minimal chrome).

**Phase 5+ (post-MVP, per original notes)**
- Avatars, user profile (nickname, signature).
- Friends/contacts + QR.
- "面对面建群".
- Optional full-duplex mode.
- Push notifications for invites.
- Production auth (phone + SMS via chosen provider).
- Testing on real devices + Bluetooth accessories.
- Recording? (if needed later).

## Key Technical Considerations

1. **PTT with LiveKit**: Do **not** keep mic always published. Use `setMicrophoneEnabled` / publish control only on button state. This keeps bandwidth and "walkie" semantics correct.
2. **Android Audio**:
   - Runtime permissions + explainers.
   - `AudioManager` mode changes for communication.
   - Bluetooth: `BLUETOOTH_CONNECT` (API 31+), handling SCO.
   - Foreground service or proper connection config if background voice is desired (LiveKit SDK has guidance).
3. **Room Naming**: Use stable mapping e.g. `group-{groupId}` or a short code room. Create rooms lazily (LiveKit creates on first valid join).
4. **Token Security**: Never embed API key/secret in client. Always generate on your Go backend.
5. **Scale for MVP**: Assume 2–10 participants per group. LiveKit handles this easily.
6. **Data Sync**: Your Go backend owns canonical group membership. LiveKit room membership is ephemeral (media only).

## Development Workflow (recommended)

1. `docker compose up -d` — starts postgres + livekit-server.
   Use the official image: `livekit/livekit-server:latest`
   (export LIVEKIT_API_KEY / LIVEKIT_API_SECRET in compose or .env).
2. Run Go server locally: `go run cmd/server/main.go` (loads same keys, connects to postgres and LiveKit).
3. In Android Studio: run the app on emulator or device (point REST base URL to host machine IP or use ADB reverse).
4. Use `scripts/seed.sh` (to be created) or manual curl to create test users/groups.
5. Test with two instances (two emulators, or emulator + physical device).
6. View LiveKit server logs + your Go logs during PTT sessions.
7. LiveKit has a built-in web playground (when running locally) useful for quick sanity checks.

Add a Makefile or justdoc for common commands (`make dev`, `make android-install`, etc.).

## Verification / Testing (End-to-End)

1. **Environment**:
   - `docker compose up` brings up postgres + livekit-server (use official livekit image + env with keys).
   - Run Go server locally (or in compose).
   - Run Android app in emulator or on device (API 28+ recommended).

2. **Happy Path Test**:
   - App A creates a group → receives join code.
   - App B (different device/emulator) joins using the code.
   - Both enter the group room.
   - App A holds PTT and speaks → App B hears the audio with low latency.
   - Release → audio stops.
   - Verify multiple speakers can take turns.
   - Leave works cleanly; room cleans up.

3. **Non-functional checks**:
   - Microphone permission flow works without crash.
   - Bluetooth headset: audio routes correctly when connected (test SCO if classic BT).
   - App survives screen off / brief background (within LiveKit limits).
   - Re-join after network change (reconnect logic).

4. **Artifacts**:
   - Two (or more) Android instances can communicate voice.
   - No console errors in LiveKit server logs during PTT.
   - Group state persists across server restarts (DB).

5. **Later**: Instrumented UI tests or screenshot tests for room screen; load test with 5–10 concurrent small groups if desired.

## Open Items / Future Discussion Points

- Exact auth mechanism for MVP (email, username + pin, anonymous with display name, or immediate phone flow)?
- Group code format (short 6-char alphanumeric? UUID slug?).
- Do we want any persistent voice history / messages, or pure realtime only?
- Backend framework inside Go (chi + sqlc? fiber? gin?).
- Deployment target for server (local Docker only for now, or VPS plan).
- Any preference for package name / app name / icon?

These can be resolved interactively before or during implementation.

## Next Steps After Plan Approval

1. Reorganize `SC15001` → `reference/sc15001-legacy`.
2. Set up docker-compose + LiveKit + Postgres.
3. Scaffold Go backend + first token endpoint.
4. Create Android project and wire basic navigation + first REST call.
5. Integrate LiveKit SDK and build the PTT button.

This plan is intentionally scoped to deliver a **working classic group walkie-talkie experience** quickly while staying faithful to the original documented requirements.

---

## Implementation Progress (as of 2026-06-19)

**已暂停**：用户表示目前手边没有测试设备，决定先记录进展，下次有设备再继续端到端验证。

### 已实现内容（最小核心 Demo）

**后端 (server/)**
- 使用 chi 作为轻量路由（符合之前确认的选型）
- 内存存储群组（`internal/group/group.go`）
- 6 位字母数字加入码自动生成
- 接口：
  - `POST /groups` 创建群组
  - `POST /groups/join` 按码加入
  - `POST /groups/{code}/token?nickname=xxx` 获取 LiveKit token
- LiveKit token 生成封装（`internal/livekit/token.go`）
- 简单 CORS 支持，便于 Android 开发

**Android 客户端 (android/)**
- Material You 主题（支持 Android 12+ 动态配色）
- Navigation Compose 完整流程：
  1. NicknameScreen（输入昵称）
  2. GroupsScreen（创建/加入群）
  3. RoomScreen（房间 + PTT）
- 真实 API 调用（Retrofit + Gson）
- LiveKit 集成：
  - 进入房间时调用后端获取 token 并 `LiveKit.create().connect()`
  - 大圆形按钮使用 `pointerInput` 实现按住/松开
  - 按住时 `localParticipant.setMicrophoneEnabled(true)`
  - 松开时设为 false
- 基础参与者列表

**基础设施**
- `docker-compose.yml` + `livekit.yaml`（官方 livekit/livekit-server）
- `README.md` 详细运行说明
- 引用文档保留在 `reference/sc15001-legacy/`

### 尚未完成 / 下次继续建议
- 实际设备/模拟器端到端语音测试
- 运行时麦克风权限请求
- 房间内实时成员列表更新（监听 LiveKit remoteParticipants）
- 错误处理、连接状态、重连逻辑
- 后端持久化（sqlc + Postgres）
- 蓝牙耳机音频路由
- UI 细节打磨（动画、波形指示等）

### 关键文件清单（当前）
- `server/cmd/server/main.go`
- `server/internal/group/group.go`
- `android/app/src/main/java/com/walkietalkie/app/MainActivity.kt`
- `android/app/src/main/java/com/walkietalkie/app/ui/RoomScreen.kt`
- `android/app/src/main/java/com/walkietalkie/app/ui/GroupsScreen.kt`
- `android/app/src/main/java/com/walkietalkie/app/data/ApiClient.kt`

所有操作均限定在当前工作目录内。

---

**恢复工作时建议**：
1. 先按 README 启动后端和 LiveKit
2. 用至少两个 Android 实例验证 PTT 是否能听到声音
3. 根据测试结果再决定优先修复哪部分

文档已更新，代码结构完整，随时可以继续。
