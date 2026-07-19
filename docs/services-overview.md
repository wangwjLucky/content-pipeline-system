# 服务说明与调用链路

> 系统由 **Java 后台**（pipeline-manager）、**7 个 Python 微服务**、**Vue 3 前端**、**4 个基础设施服务** 组成。

---

## 1. 基础设施服务

### 1.1 PostgreSQL 16

| 项目 | 说明 |
|------|------|
| 用途 | 主数据库，存储所有业务数据 |
| 端口 | `5432` |
| 镜像 | `postgres:16` |
| 数据卷 | `postgres_data:/var/lib/postgresql/data` |
| 初始化 | `init.sql` 自动建表 |
| 连接 | JDBC `jdbc:postgresql://postgres:5432/pipeline` |
| 健康检查 | `pg_isready -U pipeline` |

**核心表**：`task`、`script`、`storyboard`、`material`、`voice`、`publish_log`、`task_event`、`topic`、`sys_user`、`sys_role`、`ai_model_config`、`platform_account`、`prompt_template`

### 1.2 Redis 7

| 项目 | 说明 |
|------|------|
| 用途 | 缓存、分布式锁、临时数据 |
| 端口 | `6379` |
| 镜像 | `redis:7-alpine` |
| 数据卷 | `redis_data:/data` |

### 1.3 MinIO

| 项目 | 说明 |
|------|------|
| 用途 | 对象存储（视频、图片、音频素材） |
| 端口 | `9000`（API）、`9001`（控制台） |
| 镜像 | `minio/minio` |
| 数据卷 | `minio_data:/data` |
| 默认凭据 | `pipeline` / `pipeline123`（通过 `.env` 配置） |

### 1.4 RabbitMQ

| 项目 | 说明 |
|------|------|
| 用途 | 异步消息队列，Java → Python 通信 |
| 端口 | `5672`（AMQP）、`15672`（管理控制台） |
| 镜像 | `rabbitmq:3-management` |
| 数据卷 | `rabbitmq_data:/var/lib/rabbitmq` |
| 默认凭据 | `pipeline` / `pipeline123`（通过 `.env` 配置） |

**队列列表**：

| 队列 | 用途 | 生产者 | 消费者 |
|------|------|--------|--------|
| `pipeline.script.generate` | 脚本生成 | Java | script-service |
| `pipeline.prompt.generate` | 分镜生成 | Java | prompt-service |
| `pipeline.video.generate` | 视频生成 | Java | video-service |
| `pipeline.voice.generate` | 配音生成 | Java | voice-service |
| `pipeline.image.generate` | 图片生成 | Java | image-service |
| `pipeline.ffmpeg.compile` | 剪辑合成 | Java | ffmpeg-service |

**系统队列（框架内部使用）**：

| 队列 | 用途 |
|------|------|
| `pipeline.task.create` | 新任务创建事件（预留） |
| `pipeline.task.callback` | Python→Java 回调结果（预留） |
| `pipeline.dlq.task` | 死信队列（重试 3 次失败后进入） |

---

## 2. Java 后台 — pipeline-admin

| 项目 | 说明 |
|------|------|
| 端口 | `8080` |
| 框架 | Spring Boot 3.2 + Java 17 |
| ORM | MyBatis-Plus 3.5.7 |
| 构建 | Maven |
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

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/v1/auth/**` | POST | 登录认证 |
| `/api/v1/health` | GET | 健康检查 |
| `/api/v1/tasks/callback` | POST | Python 服务回调（受 `X-Callback-Token` 保护） |
| `/api/v1/tasks/**` | GET/POST/PUT | 任务 CRUD |
| `/api/v1/scripts/**` | GET/POST/PUT | 脚本管理 |
| `/api/v1/ai-models/**` | GET/POST/PUT | AI 模型配置 |
| `/api/v1/topics/**` | GET/POST/PUT | 选题管理 |
| `/api/v1/publishes/**` | GET/POST/PUT | 发布管理 |
| `/api/v1/platform-accounts/**` | GET/POST/PUT | 平台账号管理 |

### 功能模块

| 模块 | 说明 |
|------|------|
| 选题管理 | 选题的创建、热度排序、自动抓取 |
| 任务管理 | 任务创建、状态流转、进度追踪、取消/重试 |
| 脚本管理 | 脚本生成、审批、驳回、编辑、版本管理 |
| 分镜管理 | 分镜数据保存、AI 自动拆分 |
| 素材管理 | 视频/图片/音频素材管理 |
| 发布管理 | 多渠道发布、定时发布 |
| 平台账号 | 社交媒体账号管理（Cookies 加密存储） |
| AI 模型配置 | 模型供应商、API Key（加密存储）、权重配置 |

### 状态机

```
WAIT → SCRIPTING → SCRIPT_REVIEW → STORYBOARD → GENERATING → VOICEOVER → EDITING → REVIEW → READY → PUBLISHED
  ↑                                                                                                       │
  └────────────────────────────────── ERROR ──────────────────────────────────────────────────────────────┘
                                                                                                    CANCELLED (终态)
```

---

## 3. Python AI 服务

所有 Python 服务共用以下模块：

| 模块 | 功能 |
|------|------|
| `common/config.py` | Pydantic Settings，通过 `PIPELINE_` 前缀环境变量注入 |
| `common/logging.py` | RotatingFileHandler 日志（debug/info/warning/error 分级别） |
| `common/rabbit.py` | RabbitMQ 连接与消费者封装（线程安全） |
| `common/models.py` | Pydantic 数据模型（TaskMessage、CallbackBody 等） |
| `common/callback.py` | 回调 Java 后端的公共函数，自动注入 `X-Callback-Token` |
| `common/minio.py` | MinIO S3 客户端封装 |

### 3.1 AI Gateway

| 项目 | 说明 |
|------|------|
| 端口 | `8001` |
| 框架 | FastAPI + Uvicorn |
| 镜像构建 | `ai-services/gateway/Dockerfile` |
| 配置 | `gateway/.env` / `PIPELINE_` 环境变量 |

**功能**：统一 AI 模型路由入口，所有 Python 服务的 AI 调用都通过此网关转发。

**路由列表**：

| 路径 | 方法 | 说明 |
|------|------|------|
| `/ai/v1/chat` | POST | AI 对话（OpenAI / Claude） |
| `/ai/v1/chat/{provider}` | POST | 按 Provider 调用 |
| `/ai/v1/generate` | POST | 通用生成 |
| `/ai/v1/models` | GET | 模型列表 |
| `/ai/v1/image/generate` | POST | 图片生成 |
| `/ai/v1/voice/generate` | POST | 配音生成 |
| `/ai/v1/voice/clone` | POST | 声音克隆 |
| `/ai/v1/video/generate` | POST | 视频生成 |
| `/ai/v1/script/generate` | POST | 脚本生成 |
| `/ai/v1/prompt/split` | POST | 分镜拆分 |
| `/ai/v1/ffmpeg/compile` | POST | FFmpeg 合成 |

**Provider 列表**：`OpenAIProvider`、`ClaudeProvider`、`DoubaoProvider`、`KelingProvider`、`VeoProvider`

### 3.2 Script Service（脚本生成）

| 项目 | 说明 |
|------|------|
| 端口 | `8002` |
| 队列 | `pipeline.script.generate` |
| 生成器 | `LLMScriptGenerator` → Gateway `/ai/v1/chat` |

**调用链路**：
```
Java (createTask) → MQ pipeline.script.generate → Script Service → Gateway → OpenAI
                                                                    ↓
Java (callback) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← HTTP POST
```

### 3.3 Prompt Service（分镜生成）

| 项目 | 说明 |
|------|------|
| 端口 | `8003` |
| 队列 | `pipeline.prompt.generate` |
| 生成器 | `LLMPromptGenerator` → Gateway `/ai/v1/chat` |

**调用链路**：
```
Java (approve) → MQ pipeline.prompt.generate → Prompt Service → Gateway → OpenAI
                                                                        ↓
Java (callback) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← HTTP POST
```

### 3.4 Video Service（视频生成）

| 项目 | 说明 |
|------|------|
| 端口 | `8004` |
| 队列 | `pipeline.video.generate` |
| 生成器 | `VideoGenerator` → Gateway `/ai/v1/generate` |

**调用链路**：
```
Java (状态推进 → GENERATING) → MQ pipeline.video.generate → Video Service → Gateway → Keling API
                                                                              ↓
Java (callback → VOICEOVER) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← HTTP POST
```

### 3.5 Voice Service（配音生成）

| 项目 | 说明 |
|------|------|
| 端口 | `8005` |
| 队列 | `pipeline.voice.generate` |
| TTS 引擎 | `DoubaoTTS` → Gateway `/ai/v1/voice/generate` |

**调用链路**：
```
Java (状态推进 → VOICEOVER) → MQ pipeline.voice.generate → Voice Service → Gateway → Doubao TTS
                                                                              ↓
Java (callback → EDITING) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← HTTP POST
```

### 3.6 Image Service（图片生成）

| 项目 | 说明 |
|------|------|
| 端口 | `8007` |
| 队列 | `pipeline.image.generate` |
| 生成器 | `ImageGenerator` → Gateway `/ai/v1/image/generate` |

**调用链路**：
```
Java → MQ pipeline.image.generate → Image Service → Gateway → Keling/DALL-E
                                                      ↓
Java (callback) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← HTTP POST
```

### 3.7 FFmpeg Service（剪辑合成）

| 项目 | 说明 |
|------|------|
| 端口 | `8006` |
| 队列 | `pipeline.ffmpeg.compile` |
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

| 项目 | 说明 |
|------|------|
| 框架 | Vue 3 + Composition API |
| UI 库 | Ant Design Vue 4.x |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |
| 构建 | Vite 5 |
| HTTP | Axios（拦截器自动附加 JWT Token） |
| 图表 | ECharts 6 |
| Docker | 构建 → Nginx 静态服务，端口 `80` |

**核心页面**：

| 路由 | 页面 | 功能 |
|------|------|------|
| `/login` | 登录 | 用户认证 |
| `/` | 仪表盘 | 统计概览 |
| `/tasks` | 任务列表 | 任务管理、状态追踪 |
| `/tasks/:id` | 任务详情 | 状态流转、脚本审批 |
| `/topics` | 选题管理 | 选题列表、热度排序 |
| `/scripts` | 脚本管理 | 脚本审批、编辑 |
| `/publish` | 发布管理 | 发布配置、定时发布 |
| `/publish/calendar` | 发布日历 | 日历视图 |
| `/settings/accounts` | 平台账号 | 多平台账号管理 |
| `/settings/ai-models` | AI 模型配置 | 模型参数、API Key |
| `/analytics` | 数据分析 | 趋势图表 |

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