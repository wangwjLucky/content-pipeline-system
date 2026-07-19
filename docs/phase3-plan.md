# Phase 3: 全流水线 Java 模块 + Python AI 服务 + 前端 (MVP)

**目标:** 在 Phase 2 E2E 核心路径跑通基础上，补齐剩余 6 个管线阶段（分镜 → 素材生成 → 配音 → 剪辑 → 审核 → 发布），实现完整 7 阶段 MVP。

**架构模式:** `Java Controller → MQ → Python Worker → HTTP Callback → Java 状态推进`

**前置条件:** Phase 2 完成，Docker 基础设施运行中，Spring Boot 可访问 `localhost:8080`

---

## 设计决策

1. **所有 Java 模块保留在 pipeline-admin 内**（不拆分为独立 Maven 模块），因为实体已存在于此，拆分在当前阶段只是增加复杂度
2. **CallbackController 不使用 switch 扩展**，改为注入 `StageHandler` 策略接口映射，保持开闭原则
3. **新增 TaskEvent 表跟踪状态机**，所有状态转换记录时间线
4. **Python 服务遵循 script-service 模式**：MQ 消费 → 调用 AI Gateway → HTTP 回调 Java
5. **前端使用 Vue 3 + Ant Design Vue**（Naive UI 备选），提供完整管理界面

---

## 第一阶段：任务状态机 + 时间线

**目标:** 扩展 TaskController 支持完整任务生命周期（取消/重试/时间线），添加状态机验证。

### Files
- **Create:** `pipeline-admin/.../entity/TaskEvent.java` — `@TableName("task_event")`: taskId, fromStatus, toStatus, operator, comment
- **Create:** `pipeline-admin/.../mapper/TaskEventMapper.java` — `@Mapper extends BaseMapper<TaskEvent>`
- **Modify:** `pipeline-admin/.../controller/TaskController.java` — 添加:
  - `POST /{id}/cancel` (WAIT/SCRIPTING → CANCELLED)
  - `POST /{id}/retry` (ERROR → WAIT)
  - `GET /{id}/timeline` (事件历史)
- **Modify:** `pipeline-admin/.../service/TaskService.java` + `TaskServiceImpl.java` — 状态机验证逻辑
- **Create:** `pipeline-admin/.../service/TaskStateMachine.java` — 状态转换校验器组件

### 状态机规则
```
WAIT → SCRIPTING | CANCELLED
SCRIPTING → SCRIPT_REVIEW | ERROR | CANCELLED
SCRIPT_REVIEW → STORYBOARD | WAIT(驳回) | CANCELLED
STORYBOARD → GENERATING | ERROR | CANCELLED
GENERATING → VOICEOVER | ERROR | CANCELLED
VOICEOVER → EDITING | ERROR | CANCELLED
EDITING → REVIEW | ERROR | CANCELLED
REVIEW → READY | WAIT(驳回) | CANCELLED
READY → PUBLISHED | CANCELLED
PUBLISHED → (终态)
CANCELLED → (终态)
ERROR → WAIT | CANCELLED
```

---

## 第二阶段：脚本管理（审核工作流）

**目标:** 脚本审核（批准/驳回）、版本历史、手动编辑。

### Files
- **Create:** `pipeline-admin/.../controller/ScriptController.java`
  - `GET /api/v1/scripts` — 分页，按 taskId/topicId/status 筛选
  - `GET /api/v1/scripts/{id}` — 详情
  - `PUT /api/v1/scripts/{id}` — 编辑
  - `POST /api/v1/scripts/{id}/approve` — 批准 → 任务推进 STORYBOARD，触发 AiService.sendPromptGenerate
  - `POST /api/v1/scripts/{id}/reject` — 驳回 → 任务回退 WAIT
  - `GET /api/v1/scripts/{id}/versions` — 版本历史
- **Create:** `pipeline-admin/.../service/ScriptService.java` + `ScriptServiceImpl.java`
- **Modify:** `CallbackController.java` — script 回调设置 status=PENDING_REVIEW 而非直接 SCRIPT_REVIEW

---

## 第三阶段：分镜管理（CRUD + AI 自动拆分）

**目标:** 分镜 CRUD + 批量编辑 + 触发 AI 自动拆分。

### Files
- **Create:** `pipeline-admin/.../controller/StoryboardController.java`
  - `GET /api/v1/storyboards` — 按 taskId 筛选
  - `POST /api/v1/storyboards/batch` — 批量保存
  - `POST /api/v1/storyboards/auto-split` — MQ 触发 AI 拆分
- **Create:** `pipeline-admin/.../mapper/StoryboardMapper.java` — `@Mapper extends BaseMapper<Storyboard>`
- **Create:** `pipeline-admin/.../service/StoryboardService.java` + `StoryboardServiceImpl.java`
- **Modify:** `CallbackController.java` — 添加 `case "prompt"` → STORYBOARD→GENERATING

---

## 第四阶段：素材管理

**目标:** 素材 CRUD + 批量触发视频/图片生成。

### Files
- **Create:** `pipeline-admin/.../controller/MaterialController.java`
  - `GET /api/v1/materials` — 按 taskId/storyboardId/type 筛选
  - `POST /api/v1/materials/batch-generate` — 为每个分镜发送 MQ 消息
- **Create:** `pipeline-admin/.../mapper/MaterialMapper.java` — `@Mapper extends BaseMapper<Material>`
- **Create:** `pipeline-admin/.../service/MaterialService.java` + `MaterialServiceImpl.java`
- **Modify:** `CallbackController.java` — 添加 `case "video"` → GENERATING→VOICEOVER
- **Modify:** `AiService.java` — 添加 `sendImageGenerate`

---

## 第五阶段：配音管理

**目标:** 配音 CRUD + 触发 TTS 生成。

### Files
- **Create:** `pipeline-admin/.../controller/VoiceController.java`
  - `POST /api/v1/voices/generate` — 触发 TTS
  - `GET /api/v1/voices/{taskId}` — 查询状态
- **Create:** `pipeline-admin/.../mapper/VoiceMapper.java` — `@Mapper extends BaseMapper<Voice>`
- **Create:** `pipeline-admin/.../service/VoiceService.java` + `VoiceServiceImpl.java`
- **Modify:** `CallbackController.java` — 已有 `case "voice"` → VOICEOVER→EDITING

---

## 第六阶段：剪辑合成管理

**目标:** 触发 FFmpeg 合成。

### Files
- **Create:** `pipeline-admin/.../controller/EditController.java`
  - `POST /api/v1/edits/{taskId}/compile` — 触发 FFmpeg
- **Create:** `pipeline-admin/.../service/EditService.java` + `EditServiceImpl.java`
- **Modify:** `CallbackController.java` — 已有 `case "ffmpeg"` → EDITING→REVIEW

---

## 第七阶段：发布管理

**目标:** 手动发布到平台。

### Files
- **Create:** `pipeline-admin/.../controller/PublishController.java`
  - `POST /api/v1/publishes` — 创建发布记录
  - `POST /api/v1/publishes/{id}/publish` — 执行发布（READY→PUBLISHED）
  - `GET /api/v1/publishes` — 列表
- **Create:** `pipeline-admin/.../mapper/PublishLogMapper.java` — `@Mapper extends BaseMapper<PublishLog>`
- **Create:** `pipeline-admin/.../entity/PlatformAccount.java` — `@TableName("platform_account")`
- **Create:** `pipeline-admin/.../mapper/PlatformAccountMapper.java`
- **Create:** `pipeline-admin/.../service/PublishService.java` + `PublishServiceImpl.java`
- **Modify:** `CallbackController.java` — `handleSuccess` 中变 `REVIEW→READY` 仅在终审通过时

---

## 第八阶段：Python AI 服务

**目标:** 补齐 Prompt / Video / Voice / FFmpeg 四个 Python 服务。

### Files
- **Create:** `ai-services/prompt-service/` — 拆分脚本为分镜
  - `main.py`, `generators/base.py`, `generators/llm_prompt_generator.py`, `Dockerfile`
- **Create:** `ai-services/video-service/` — 视频/图片生成（调用 AI Gateway Keling provider）
  - `main.py`, `generators/base.py`, `generators/video_generator.py`, `Dockerfile`
- **Create:** `ai-services/voice-service/` — TTS 配音（调用 AI Gateway Doubao provider）
  - `main.py`, `tts/base.py`, `tts/doubao_tts.py`, `Dockerfile`
- **Create:** `ai-services/ffmpeg-service/` — 剪辑合成 + 字幕 + 封面
  - `main.py`, `composite.py`, `subtitle.py`, `audio.py`, `cover.py`, `Dockerfile`
- **Modify:** `docker-compose.yml` — 添加以上 4 个服务
- **Modify:** `ai-services/gateway/providers/` — 补齐真实 API 调用（当前为 mock）

---

## 第九阶段：前端（Vue 3 + Ant Design Vue）

**目标:** 提供完整的后台管理界面。

### Files (~30 个)
- **Create:** `frontend/` 完整 Vue 3 项目
  - 登录页、工作台、选题管理、任务列表/详情、脚本审核、分镜编辑、素材面板、配音面板、发布管理、AI 模型配置
  - JWT 认证拦截、状态标签组件、进度条组件

---

## 全局修改

- **init.sql** — 添加 `task_event` 表
- **SecurityConfig.java** — 新端点根据是否需要认证配置 `.permitAll()` 或保留认证
- **AiService.java** — 添加 `sendPromptGenerate`、`sendImageGenerate`
- **RabbitConfig.java** — 确认 `pipeline.prompt.generate`、`pipeline.image.generate` 队列已声明

---

## 验证方案

1. `mvn clean install -DskipTests` 编译通过
2. 启动 Spring Boot + Docker 服务
3. 创建选题 → 创建任务 → 自动生成脚本 → ScriptController 审核通过
4. 分镜编辑 → 触发 AI 自动拆分 → 状态推进 GENERATING
5. 素材批量生成 → 视频生成回调 → 状态推进 VOICEOVER
6. 触发配音 → 配音回调 → 状态推进 EDITING
7. 触发 FFmpeg 合成 → 合成回调 → 状态推进 REVIEW
8. 终审通过 → READY → 发布 → PUBLISHED
9. 前端登录、各页面 CRUD 操作正常