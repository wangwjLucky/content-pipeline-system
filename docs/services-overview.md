# 服务说明与调用链路

> 版本：v1.1 | 日期：2026-07-19
>
> 系统由 **Java 后台**（pipeline-manager）、**7 个 Python 微服务**、**Vue 3 前端**、**4 个基础设施服务** 组成。

---

## 1. 基础设施服务

### 1.1 PostgreSQL 16

| 项目     | 说明                                             |
| -------- | ------------------------------------------------ |
| 用途     | 主数据库，存储所有业务数据                       |
| 端口     | `5432`                                         |
| 镜像     | `postgres:16`                                  |
| 数据卷   | `postgres_data:/var/lib/postgresql/data`       |
| 初始化   | `init.sql` 自动建表                            |
| 连接     | JDBC`jdbc:postgresql://postgres:5432/pipeline` |
| 健康检查 | `pg_isready -U pipeline`                       |

**核心表**：`task`、`script`、`storyboard`、`material`、`voice`、`publish_log`、`task_event`、`topic`、`sys_user`、`sys_role`、`ai_model_config`、`platform_account`、`prompt_template`

### 1.2 Redis 7

| 项目   | 说明                     |
| ------ | ------------------------ |
| 用途   | 缓存、分布式锁、临时数据 |
| 端口   | `6379`                 |
| 镜像   | `redis:7-alpine`       |
| 数据卷 | `redis_data:/data`     |

### 1.3 MinIO

| 项目     | 说明                                                 |
| -------- | ---------------------------------------------------- |
| 用途     | 对象存储（视频、图片、音频素材）                     |
| 端口     | `9000`（API）、`9001`（控制台）                  |
| 镜像     | `minio/minio`                                      |
| 数据卷   | `minio_data:/data`                                 |
| 默认凭据 | `pipeline` / `pipeline123`（通过 `.env` 配置） |

### 1.4 RabbitMQ

| 项目     | 说明                                                 |
| -------- | ---------------------------------------------------- |
| 用途     | 异步消息队列，Java → Python 通信                    |
| 端口     | `5672`（AMQP）、`15672`（管理控制台）            |
| 镜像     | `rabbitmq:3-management`                            |
| 数据卷   | `rabbitmq_data:/var/lib/rabbitmq`                  |
| 默认凭据 | `pipeline` / `pipeline123`（通过 `.env` 配置） |

**队列列表**：

| 队列                         | 用途     | 生产者 | 消费者         |
| ---------------------------- | -------- | ------ | -------------- |
| `pipeline.script.generate` | 脚本生成 | Java   | script-service |
| `pipeline.prompt.generate` | 分镜生成 | Java   | prompt-service |
| `pipeline.video.generate`  | 视频生成 | Java   | video-service  |
| `pipeline.voice.generate`  | 配音生成 | Java   | voice-service  |
| `pipeline.image.generate`  | 图片生成 | Java   | image-service  |
| `pipeline.ffmpeg.compile`  | 剪辑合成 | Java   | ffmpeg-service |

**系统队列（框架内部使用）**：

| 队列                       | 用途                            |
| -------------------------- | ------------------------------- |
| `pipeline.task.create`   | 新任务创建事件（预留）          |
| `pipeline.task.callback` | Python→Java 回调结果（预留）   |
| `pipeline.dlq.task`      | 死信队列（重试 3 次失败后进入） |

---

## 2. Java 后台 — pipeline-admin

| 项目 | 说明                                    |
| ---- | --------------------------------------- |
| 端口 | `8080`                                |
| 框架 | Spring Boot 3.2 + Java 17               |
| ORM  | MyBatis-Plus 3.5.7                      |
| 构建 | Maven                                   |
| 部署 | `pipeline-manager/Dockerfile` → 镜像 |

### 配置项

```
server.port=8080
spring.datasource.url=jdbc:postgresql://<host>:5432/pipeline
spring.datasource.username=pipeline
spring.datasource.password=${DB_PASSWORD}
spring.rabbitmq.host=<host>
spring.rabbitmq.port=5672
spring.data.redis.host=<host>
spring.data.redis.port=6379
jwt.secret=${JWT_SECRET}
minio.endpoint=http://<host>:9000
pipeline.callback-base-url=${CALLBACK_BASE_URL}
pipeline.callback-token=${CALLBACK_TOKEN}
```

### 核心 API

| 路径                                             | 方法           | 说明                                                                                             |
| ------------------------------------------------ | -------------- | ------------------------------------------------------------------------------------------------ |
| `/api/v1/auth/login`                           | POST           | 用户登录，返回 JWT Token                                                                         |
| `/api/v1/auth/register`                        | POST           | 用户注册                                                                                         |
| `/api/v1/auth/me`                              | GET            | 获取当前登录用户信息                                                                             |
| `/api/v1/health`                               | GET            | 健康检查，返回`Result.success("OK")`                                                           |
| `/api/v1/tasks/callback`                       | POST           | Python 服务回调（受`X-Callback-Token` 保护），处理 script/prompt/video/image/voice/ffmpeg 回调 |
| `/api/v1/tasks`                                | GET            | 任务列表（分页 + 状态筛选）                                                                      |
| `/api/v1/tasks`                                | POST           | 创建任务（自动推进到 SCRIPTING 并发送 MQ）                                                       |
| `/api/v1/tasks/{id}`                           | GET            | 任务详情                                                                                         |
| `/api/v1/tasks/{id}/cancel`                    | POST           | 取消任务                                                                                         |
| `/api/v1/tasks/{id}/retry`                     | POST           | 重试失败任务（重置到 WAIT→SCRIPTING）                                                           |
| `/api/v1/tasks/{id}/timeline`                  | GET            | 任务时间线（事件列表）                                                                           |
| `/api/v1/scripts`                              | GET            | 脚本列表（支持 taskId/topicId/status 筛选）                                                      |
| `/api/v1/scripts/{id}`                         | GET            | 脚本详情                                                                                         |
| `/api/v1/scripts/generate`                     | POST           | 触发脚本生成（写入数据库后发送 MQ）                                                              |
| `/api/v1/scripts/{id}`                         | PUT            | 编辑脚本（递增版本号，状态回退到 PENDING_REVIEW）                                                |
| `/api/v1/scripts/{id}/review`                  | POST           | 审核脚本（approve/reject）                                                                       |
| `/api/v1/scripts/{id}/approve`                 | POST           | 批准脚本（推进到 STORYBOARD → 发送分镜生成 MQ）                                                 |
| `/api/v1/scripts/{id}/reject`                  | POST           | 驳回脚本（回退到 WAIT）                                                                          |
| `/api/v1/scripts/{id}/versions`                | GET            | 脚本版本历史                                                                                     |
| `/api/v1/ai-models`                            | GET/POST       | AI 模型配置列表 / 新增                                                                           |
| `/api/v1/ai-models/{id}`                       | GET/PUT/DELETE | AI 模型配置详情 / 更新 / 删除                                                                    |
| `/api/v1/ai-models/{id}/test`                  | POST           | 测试模型连接                                                                                     |
| `/api/v1/ai-models/test-mq`                    | POST           | 测试 MQ 消息发送                                                                                 |
| `/api/v1/topics`                               | GET/POST       | 选题列表 / 创建                                                                                  |
| `/api/v1/topics/{id}`                          | GET/PUT/DELETE | 选题详情 / 更新 / 删除                                                                           |
| `/api/v1/topics/{id}/generate-task`            | POST           | 从选题创建生产任务                                                                               |
| `/api/v1/publish`                              | GET/POST       | 发布列表 / 创建发布记录                                                                          |
| `/api/v1/publish/{id}/publish`                 | POST           | 执行发布                                                                                         |
| `/api/v1/publish/{id}/schedule`                | POST           | 定时发布                                                                                         |
| `/api/v1/publish/{id}/cancel`                  | POST           | 取消发布                                                                                         |
| `/api/v1/publish/calendar`                     | GET            | 发布日历                                                                                         |
| `/api/v1/publish/accounts`                     | GET            | 平台账号列表                                                                                     |
| `/api/v1/platform-accounts`                    | GET/POST       | 平台账号列表 / 新增                                                                              |
| `/api/v1/platform-accounts/{id}`               | GET/PUT/DELETE | 平台账号详情 / 更新 / 删除                                                                       |
| `/api/v1/analytics/overview`                   | GET            | 数据总览                                                                                         |
| `/api/v1/analytics/daily`                      | GET            | 日报数据                                                                                         |
| `/api/v1/analytics/topics`                     | GET            | 选题效果分析                                                                                     |
| `/api/v1/analytics/accounts`                   | GET            | 账号维度统计                                                                                     |
| `/api/v1/templates`                            | GET/POST       | Prompt 模板列表 / 创建                                                                           |
| `/api/v1/templates/{id}`                       | GET/PUT/DELETE | 模板详情 / 更新 / 删除                                                                           |
| `/api/v1/users`                                | GET/POST       | 用户列表 / 创建                                                                                  |
| `/api/v1/users/{id}`                           | GET/PUT/DELETE | 用户详情 / 更新 / 删除                                                                           |
| `/api/v1/roles`                                | GET/POST       | 角色列表 / 创建                                                                                  |
| `/api/v1/roles/{id}`                           | GET/PUT/DELETE | 角色详情 / 更新 / 删除                                                                           |
| `/api/v1/tasks/{taskId}/storyboard`            | GET            | 获取分镜列表                                                                                     |
| `/api/v1/tasks/{taskId}/storyboard`            | PUT            | 批量更新分镜                                                                                     |
| `/api/v1/tasks/{taskId}/storyboard/auto-split` | POST           | AI 自动拆解分镜                                                                                  |
| `/api/v1/materials`                            | GET            | 素材列表                                                                                         |
| `/api/v1/materials/{id}`                       | GET/DELETE     | 素材详情 / 删除                                                                                  |
| `/api/v1/materials/batch-generate`             | POST           | 批量生成素材                                                                                     |
| `/api/v1/voices/generate`                      | POST           | 生成配音                                                                                         |
| `/api/v1/voices/{taskId}`                      | GET/PUT        | 获取配音状态 / 更新配音参数                                                                      |
| `/api/v1/edits/compile`                        | POST           | 启动剪辑合成                                                                                     |
| `/api/v1/edits/{taskId}/compile`               | POST           | 启动指定任务剪辑合成                                                                             |
| `/api/v1/edits/{taskId}/preview`               | GET            | 获取预览地址                                                                                     |
| `/api/v1/edits/{taskId}/regenerate`            | POST           | 重新剪辑                                                                                         |
| `/api/v1/files/upload`                         | POST           | 文件上传                                                                                         |
| `/api/v1/files/download`                       | GET            | 文件下载                                                                                         |
| `/api/v1/files`                                | GET/DELETE     | 文件列表 / 删除                                                                                  |

### 通用组件

| 组件         | 类                                        | 说明                                                                                                                                       |
| ------------ | ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| 统一响应     | `Result<T>`                             | 所有 API 统一返回`{code, message, data}` 格式，`success()` 和 `error()` 静态工厂方法                                                 |
| 加密工具     | `EncryptionUtil`                        | AES-256-GCM 加密，用于 API Key 和平台 Cookie 的加密存储。密钥通过`AES_ENCRYPTION_KEY` 环境变量配置，12 字节随机 IV + 128 位 GCM 认证标签 |
| 内容校验     | `ContentValidator`                      | 敏感词过滤 + Prompt 注入防护（正则匹配`ignore previous instructions`、`role play`、`system prompt` 等注入模式）                      |
| JWT 工具     | `JwtUtil`                               | HS256 签名，24 小时有效期，支持`generateToken()`、`parseToken()`、`validateToken()`                                                  |
| 全局异常处理 | `GlobalExceptionHandler`                | 统一异常拦截，返回标准`Result` 错误格式                                                                                                  |
| 操作日志     | `OperationLog` + `OperationLogAspect` | 基于 AOP 的方法级操作日志，通过`@OperationLog(module, action)` 注解自动记录操作人、耗时、参数、成功/失败状态                             |
| 乐观锁       | `@Version` 注解                         | `Task` 实体使用 MyBatis-Plus 乐观锁，防止并发状态更新覆盖                                                                                |
| 数据初始化   | `DataInitializer`                       | 首次启动时自动创建默认管理员账号`admin / admin123`                                                                                       |
| 加密密钥注入 | `EncryptionConfig`                      | 启动时将`AES_ENCRYPTION_KEY` 环境变量注入 `EncryptionUtil`                                                                             |
| 全局异常处理 | `GlobalExceptionHandler`                | `@RestControllerAdvice` 统一拦截异常，返回标准 `Result` 格式（400 参数错误 / 400 状态机错误 / 500 服务器错误）                         |

### 功能模块

| 模块        | 说明                                      |
| ----------- | ----------------------------------------- |
| 选题管理    | 选题的创建、热度排序、自动抓取            |
| 任务管理    | 任务创建、状态流转、进度追踪、取消/重试   |
| 脚本管理    | 脚本生成、审批、驳回、编辑、版本管理      |
| 分镜管理    | 分镜数据保存、AI 自动拆分                 |
| 素材管理    | 视频/图片/音频素材管理                    |
| 发布管理    | 多渠道发布、定时发布、发布日历            |
| 平台账号    | 社交媒体账号管理（Cookies 加密存储）      |
| AI 模型配置 | 模型供应商、API Key（加密存储）、权重配置 |

### 回调处理逻辑

`CallbackController.java` 处理所有 Python 服务回调（`POST /api/v1/tasks/callback`），核心逻辑：

1. **令牌验证**：校验 `X-Callback-Token` 请求头
2. **参数校验**：校验 `taskId`、`service` 非空
3. **终态检查**：如果任务已是 `CANCELLED` 或 `PUBLISHED`，忽略回调
4. **状态分发**：根据 `status` 字段区分 SUCCESS / FAILURE

各服务成功回调后的状态推进：

| 回调 service | 推进到            | 进度 | 附加操作                                                |
| ------------ | ----------------- | ---- | ------------------------------------------------------- |
| `script`   | `SCRIPT_REVIEW` | 30%  | 创建/更新 Script 记录，幂等性检查（已有脚本则更新现有） |
| `prompt`   | `GENERATING`    | 50%  | 保存 AI 拆分的分镜数据（批量保存，先删后插）            |
| `video`    | `VOICEOVER`     | 60%  | —                                                      |
| `image`    | `VOICEOVER`     | 60%  | —                                                      |
| `voice`    | `EDITING`       | 80%  | —                                                      |
| `ffmpeg`   | `REVIEW`        | 95%  | —                                                      |

回调失败时统一推进到 `ERROR` 状态，保留错误信息。

### 状态机

```
WAIT → SCRIPTING → SCRIPT_REVIEW → STORYBOARD → GENERATING → VOICEOVER → EDITING → REVIEW → READY → PUBLISHED
  ↑                                                                                          ↑                  │
  │                                                                                          │                  │
  └───────────────────────────────── ERROR ──────────────────────────────────────────────────┘                  │
                                                                                                          CANCELLED (终态)

允许的转换（含自循环和回退）：
  WAIT         → WAIT(自循环) | SCRIPTING | CANCELLED
  SCRIPTING    → SCRIPT_REVIEW | ERROR | CANCELLED
  SCRIPT_REVIEW → STORYBOARD(批准) | WAIT(驳回) | CANCELLED
  STORYBOARD   → GENERATING | SCRIPT_REVIEW(脚本驳回) | ERROR | CANCELLED
  GENERATING   → VOICEOVER | SCRIPT_REVIEW(脚本驳回) | ERROR | CANCELLED
  VOICEOVER    → VOICEOVER(素材就绪中) | EDITING | SCRIPT_REVIEW(脚本驳回) | ERROR | CANCELLED
  EDITING      → REVIEW | ERROR | CANCELLED
  REVIEW       → READY(通过) | WAIT(驳回) | CANCELLED
  READY        → PUBLISHED | CANCELLED
  PUBLISHED    → (终态)
  CANCELLED    → (终态)
  ERROR        → WAIT(重试) | CANCELLED
```

---

## 3. Python AI 服务

所有 Python 服务共用以下模块：

| 模块                   | 功能                                                                                                                                                                    |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `common/config.py`   | Pydantic Settings，通过`PIPELINE_` 前缀环境变量注入，全局单例 `settings`                                                                                            |
| `common/logging.py`  | RotatingFileHandler 日志（debug/info/warning/error 分级别写到独立文件），每文件 30MB 滚动，保留 3 个备份。日志目录通过`PIPELINE_LOG_DIR` 环境变量配置，默认 `logs/` |
| `common/rabbit.py`   | RabbitMQ 连接与消费者封装（线程安全），支持`publish()` 和 `consume()`，`consume()` 为阻塞调用需在独立线程中运行，提供 `start_consumer_thread()` 便捷方法        |
| `common/models.py`   | Pydantic 数据模型（TaskMessage 支持 camelCase 字段映射、CallbackBody、ChatRequest/Response、GenerateRequest/Response）                                                  |
| `common/callback.py` | 回调 Java 后端的公共函数（`send_callback()`），自动注入 `X-Callback-Token` 头，使用 `httpx.Client` 同步发送                                                       |
| `common/minio.py`    | MinIO S3 客户端封装（boto3），提供`upload_bytes()`、`upload_file()`、`download()`、`list_files()`、`delete_file()` 方法                                       |

### 3.1 AI Gateway

| 项目     | 说明                                      |
| -------- | ----------------------------------------- |
| 端口     | `8001`                                  |
| 框架     | FastAPI + Uvicorn                         |
| 镜像构建 | `ai-services/gateway/Dockerfile`        |
| 配置     | `gateway/.env` / `PIPELINE_` 环境变量 |

**功能**：统一 AI 模型路由入口，所有 Python 服务的 AI 调用都通过此网关转发。

**路由列表**：

| 路径                        | 方法 | 说明                       |
| --------------------------- | ---- | -------------------------- |
| `/ai/v1/chat`             | POST | AI 对话（OpenAI / Claude） |
| `/ai/v1/chat/{provider}`  | POST | 按 Provider 调用           |
| `/ai/v1/generate`         | POST | 通用生成                   |
| `/ai/v1/models`           | GET  | 模型列表                   |
| `/ai/v1/image/generate`   | POST | 图片生成                   |
| `/ai/v1/voice/generate`   | POST | 配音生成                   |
| `/ai/v1/voice/clone`      | POST | 声音克隆                   |
| `/ai/v1/voice/{task_id}`  | GET  | 查询配音状态               |
| `/ai/v1/video/generate`   | POST | 视频生成                   |
| `/ai/v1/video/{task_id}`  | GET  | 查询视频生成状态           |
| `/ai/v1/script/generate`  | POST | 脚本生成                   |
| `/ai/v1/prompt/generate`  | POST | 分镜拆分                   |
| `/ai/v1/ffmpeg/composite` | POST | FFmpeg 合成                |
| `/ai/v1/ffmpeg/subtitle`  | POST | 字幕生成                   |
| `/ai/v1/ffmpeg/cover`     | POST | 封面生成                   |
| `/ai/v1/ffmpeg/audio`     | POST | 音频处理                   |
| `/ai/v1/ffmpeg/{task_id}` | GET  | 查询合成进度               |

**Provider 列表**：`OpenAIProvider`、`ClaudeProvider`、`DeepSeekProvider`、`SenseNovaProvider`、`KelingProvider`、`VeoProvider`、`DoubaoProvider`

**Provider 注册表**：所有 Provider 实例由 `gateway/providers/registry.py` 统一管理（单例），所有 Router 通过 `get_providers()` / `get_provider()` 获取共享实例。`init_providers()` 初始化时调用每个 Provider 的 `refresh_models()` 动态拉取模型列表。`refresh_all_models()` 使用 `asyncio.gather` 并行刷新所有 Provider 的模型列表，减少启动延迟。

**异步化**：所有 Provider 的 `chat()` 和 `generate()` 方法均为 `async def`，使用 `httpx.AsyncClient` 发送 HTTP 请求。Router 层通过 `await` 调用，避免阻塞事件循环。服务关闭时调用 `shutdown()` 释放所有 Provider 的 HTTP 连接。

**Provider 模型列表策略**：

| Provider  | 模型列表来源                                           | API Key 未配置时     |
| --------- | ------------------------------------------------------ | -------------------- |
| OpenAI    | 启动时`GET /v1/models` 动态拉取，失败时使用兜底列表  | 模型列表为空，不显示 |
| Claude    | 启动时`GET /v1/models` 动态拉取，失败时使用兜底列表  | 模型列表为空，不显示 |
| DeepSeek  | 启动时`GET /v1/models` 动态拉取，失败时使用兜底列表  | 模型列表为空，不显示 |
| SenseNova | 启动时`GET /v1/models` 动态拉取，失败时使用兜底列表  | 模型列表为空，不显示 |
| 可灵 AI   | 启动时`GET /v1/models` 动态拉取，失败时使用兜底列表  | 模型列表为空，不显示 |
| Veo       | 启动时`GET /v1/models` 动态拉取，失败时使用兜底列表  | 模型列表为空，不显示 |
| 豆包      | 启动时从 API 动态拉取 TTS 模型列表，失败时使用兜底列表 | 模型列表为空，不显示 |

### 3.2 Script Service（脚本生成）

| 项目   | 说明                                              |
| ------ | ------------------------------------------------- |
| 端口   | `8002`                                          |
| 队列   | `pipeline.script.generate`                      |
| 生成器 | `LLMScriptGenerator` → Gateway `/ai/v1/chat` |

**调用链路**：

```
Java (createTask) → MQ pipeline.script.generate → Script Service → Gateway → OpenAI
                                                                    ↓
Java (callback) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← HTTP POST
```

### 3.3 Prompt Service（分镜生成）

| 项目   | 说明                                              |
| ------ | ------------------------------------------------- |
| 端口   | `8003`                                          |
| 队列   | `pipeline.prompt.generate`                      |
| 生成器 | `LLMPromptGenerator` → Gateway `/ai/v1/chat` |

**调用链路**：

```
Java (approve) → MQ pipeline.prompt.generate → Prompt Service → Gateway → OpenAI
                                                                        ↓
Java (callback) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← HTTP POST
```

### 3.4 Video Service（视频生成）

| 项目   | 说明                                              |
| ------ | ------------------------------------------------- |
| 端口   | `8004`                                          |
| 队列   | `pipeline.video.generate`                       |
| 生成器 | `VideoGenerator` → Gateway `/ai/v1/generate` |

**调用链路**：

```
Java (状态推进 → GENERATING) → MQ pipeline.video.generate → Video Service → Gateway → Keling API
                                                                              ↓
Java (callback → VOICEOVER) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← HTTP POST
```

### 3.5 Voice Service（配音生成）

| 项目     | 说明                                               |
| -------- | -------------------------------------------------- |
| 端口     | `8005`                                           |
| 队列     | `pipeline.voice.generate`                        |
| TTS 引擎 | `DoubaoTTS` → Gateway `/ai/v1/voice/generate` |

**调用链路**：

```
Java (状态推进 → VOICEOVER) → MQ pipeline.voice.generate → Voice Service → Gateway → Doubao TTS
                                                                              ↓
Java (callback → EDITING) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← HTTP POST
```

### 3.6 Image Service（图片生成）

| 项目   | 说明                                                    |
| ------ | ------------------------------------------------------- |
| 端口   | `8007`                                                |
| 队列   | `pipeline.image.generate`                             |
| 生成器 | `ImageGenerator` → Gateway `/ai/v1/image/generate` |

**调用链路**：

```
Java → MQ pipeline.image.generate → Image Service → Gateway → Keling/DALL-E
                                                      ↓
Java (callback) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← HTTP POST
```

### 3.7 FFmpeg Service（剪辑合成）

| 项目   | 说明                                     |
| ------ | ---------------------------------------- |
| 端口   | `8006`                                 |
| 队列   | `pipeline.ffmpeg.compile`              |
| 合成器 | `VideoCompositor`（子进程调用 FFmpeg） |

**功能模块**：`composite.py`（合成）、`audio.py`（音频处理）、`subtitle.py`（字幕）、`cover.py`（封面）

**调用链路**：

```
Java (状态推进 → REVIEW) → MQ pipeline.ffmpeg.compile → FFmpeg Service → FFmpeg 子进程
                                                          ↓
Java (callback → REVIEW) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← HTTP POST
```

---

## 4. 前端

| 项目     | 说明                               |
| -------- | ---------------------------------- |
| 框架     | Vue 3 + Composition API            |
| UI 库    | Ant Design Vue 4.x                 |
| 状态管理 | Pinia                              |
| 路由     | Vue Router 4                       |
| 构建     | Vite 5                             |
| HTTP     | Axios（拦截器自动附加 JWT Token）  |
| 图表     | ECharts 6                          |
| Docker   | 构建 → Nginx 静态服务，端口`80` |

**核心页面**：

| 路由                            | 页面     | 功能                            |
| ------------------------------- | -------- | ------------------------------- |
| `/login`                      | 登录     | 用户认证                        |
| `/dashboard`                  | 仪表盘   | 统计概览、待审核数量、发布趋势  |
| `/tasks`                      | 任务列表 | 任务管理、状态追踪              |
| `/tasks/:id`                  | 任务详情 | 状态流转、脚本审批、流水线视图  |
| `/topics`                     | 选题管理 | 选题列表、热度排序              |
| `/topics/new`                 | 新增选题 | 手动录入选题                    |
| `/topics/:id/edit`            | 编辑选题 | 更新选题信息                    |
| `/scripts`                    | 脚本管理 | 脚本列表、状态筛选              |
| `/scripts/:id`                | 脚本审核 | 脚本内容审核、批准/驳回         |
| `/storyboards/:taskId`        | 分镜编辑 | 分镜列表、镜头编辑、AI 自动拆分 |
| `/publish`                    | 发布管理 | 发布列表、定时发布、发布配置    |
| `/publish/calendar`           | 发布日历 | 日历视图                        |
| `/voice`                      | 配音管理 | 配音生成、参数设置              |
| `/edits`                      | 剪辑管理 | 剪辑合成任务管理                |
| `/materials`                  | 素材库   | 已生成的图片/视频素材           |
| `/templates`                  | 模板管理 | Prompt 模板、分镜模板           |
| `/analytics`                  | 数据分析 | 播放数据、趋势图表              |
| `/settings/models`            | 模型配置 | AI 模型参数、API Key 管理       |
| `/settings/platform-accounts` | 平台账号 | 多平台账号管理                  |
| `/settings/users`             | 用户管理 | 系统用户管理                    |
| `/settings/roles`             | 角色管理 | 角色权限管理                    |

---

## 5. 服务间通信总览

```
┌──────────┐     HTTP       ┌──────────────┐
│  前端     │ ────────────→ │  Java 后台    │
│  Vue 3   │ ←──────────── │  :8080       │
│  :80     │     JSON       │  业务 API     │
└──────────┘               └──────┬───────┘
                                  │
                    ┌─────────────┼─────────────┐
                    │ MQ          │ HTTP         │ HTTP Callback
                    ▼             ▼              ▲
            ┌────────────┐ ┌──────────┐         │
            │ RabbitMQ   │ │  AI      │ ────────┘
            │ 6 个队列    │ │  Gateway │   X-Callback-Token
            └─────┬──────┘ │  :8001   │
                  │         └──────────┘
        ┌────────┼────────┐      │
        ▼        ▼        ▼      │ AI API
  ┌────────┐┌────────┐┌────────┐  │
  │ Script ││ Prompt ││ Video  │  │
  │ Service││ Service││ Service│  │
  │ :8002  ││ :8003  ││ :8004  │  │
  └────────┘└────────┘└────────┘  │
  ┌────────┐┌────────┐┌────────┐  │
  │ Voice  ││ Image  ││ FFmpeg │  │
  │ Service││ Service││ Service│  │
  │ :8005  ││ :8007  ││ :8006  │  │
  └────────┘└────────┘└────────┘  │
                                   │
  ┌──────────┐ ┌──────────┐ ┌─────┴────┐
  │PostgreSQL│ │  Redis   │ │  MinIO   │
  │ :5432    │ │ :6379    │ │ :9000    │
  └──────────┘ └──────────┘ └──────────┘
```
