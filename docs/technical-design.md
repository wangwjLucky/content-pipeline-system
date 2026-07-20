# 内容生产流水线系统 — 技术设计方案

> 版本：v1.2
> 日期：2026-07-20
> 状态：已更新（同步代码 v1.2）

---

## 1. 系统架构概览

### 1.1 分层架构

```
┌──────────────────────────────────────────────────────────────────┐
│                        客户端层 (Client)                          │
│          Vue 3 + Ant Design Vue                                  │
│                    Web 后台管理                                   │
├──────────────────────────────────────────────────────────────────┤
│                        接入层 (Gateway)                           │
│              Spring Cloud Gateway / Nginx                        │
│                    认证 + 路由 + 限流                              │
├──────────────────────────────────────────────────────────────────┤
│                       业务层 (Java)                               │
│       Spring Boot 3 + Java 17                                    │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐         │
│  │选题  │ │脚本  │ │分镜  │ │素材  │ │发布  │ │用户  │         │
│  │管理  │ │管理  │ │管理  │ │管理  │ │管理  │ │权限  │         │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘         │
├──────────────────────────────────────────────────────────────────┤
│                       AI 服务层 (Python)                          │
│           FastAPI + Python 3.12                                  │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐             │
│  │ 脚本生成服务   │ │ 视频生成服务  │ │ 图片生成服务  │             │
│  │ (script)      │ │ (video)      │ │ (image)      │             │
│  ├──────────────┤ ├──────────────┤ ├──────────────┤             │
│  │ 配音服务      │ │ 剪辑合成服务  │ │ AI Gateway   │             │
│  │ (voice)      │ │ (ffmpeg)     │ │ (router)     │             │
│  └──────────────┘ └──────────────┘ └──────────────┘             │
├──────────────────────────────────────────────────────────────────┤
│                       基础设施层                                   │
│    PostgreSQL  Redis  MinIO  RabbitMQ  Prometheus  Grafana       │
└──────────────────────────────────────────────────────────────────┘
```

### 1.2 架构原则

| 原则                                | 说明                                                          |
| ----------------------------------- | ------------------------------------------------------------- |
| **Java 做平台，Python 做 AI** | Java 负责所有业务逻辑和后台管理，Python 只负责 AI 相关能力    |
| **服务解耦**                  | Java 与 Python 通过 REST API + 消息队列通信，互不依赖内部实现 |
| **状态驱动**                  | 所有任务通过状态机流转，每一步状态变更都持久化到数据库        |
| **异步编排**                  | 视频生产是长流程，全部异步处理，通过 MQ 驱动下游              |
| **AI Gateway**                | 统一 AI 模型调用入口，新增模型无需改业务代码                  |

---

## 2. 技术选型明细

### 2.1 Java 平台（核心后台）

| 组件     | 技术选型                   | 说明                                                                                          |
| -------- | -------------------------- | --------------------------------------------------------------------------------------------- |
| 框架     | Spring Boot 3.x            | 最新稳定版，GraalVM 支持                                                                      |
| JDK      | Java 17                    | LTS 版本，虚拟线程支持                                                                        |
| ORM      | MyBatis Plus               | 熟悉度高，开发效率好                                                                          |
| 数据库   | PostgreSQL 16+             | JSONB、全文检索、性能优异                                                                     |
| 缓存     | Redis 7+                   | 热点数据、任务队列、分布式锁                                                                  |
| 对象存储 | MinIO                      | S3 兼容，私有化部署                                                                           |
| 消息队列 | RabbitMQ                   | 稳定可靠，适合中小规模                                                                        |
| 认证     | Spring Security + JWT      | RBAC 权限模型                                                                                 |
| 接口文档 | Knife4j (Swagger)          | OpenAPI 3 规范，`SwaggerConfig.java` 配置，访问 `/doc.html` 或 `/swagger-ui/index.html` |
| 定时任务 | XXL-JOB / Spring Scheduler | 分布式定时任务                                                                                |
| 监控     | Prometheus + Grafana       | JVM 指标、业务指标                                                                            |

### 2.2 Python AI 服务层

| 组件        | 技术选型                           | 说明                            |
| ----------- | ---------------------------------- | ------------------------------- |
| Web 框架    | FastAPI                            | 异步原生，自动生成 OpenAPI 文档 |
| Python      | 3.12+                              | 最新稳定版                      |
| AI SDK      | OpenAI / Anthropic / Google AI SDK | 官方 Python SDK                 |
| 视频生成    | 可灵 API / Veo API / 即梦 API      | REST API 调用                   |
| 音视频处理  | FFmpeg (subprocess)                | 剪辑合成核心                    |
| 图片处理    | Pillow / OpenCV                    | 封面生成、图片处理              |
| 字幕生成    | Whisper (本地或 API)               | 语音转文字                      |
| 异步任务    | Celery / Arq                       | Python 异步任务队列（可选）     |
| HTTP 客户端 | httpx                              | 异步 HTTP 请求                  |

### 2.3 前端

| 组件     | 技术选型                | 说明                           |
| -------- | ----------------------- | ------------------------------ |
| 框架     | Vue 3 + Composition API | 现代前端框架                   |
| UI 库    | Ant Design Vue 4.x      | 成熟的企业级组件库（实际选型） |
| 状态管理 | Pinia                   | Vue 3 官方推荐                 |
| 路由     | Vue Router 4            | SPA 路由                       |
| 构建     | Vite                    | 极速开发体验                   |
| HTTP     | Axios                   | HTTP 客户端封装                |
| 图表     | ECharts                 | 数据分析可视化                 |

### 2.4 DevOps

| 组件     | 技术选型                   |
| -------- | -------------------------- |
| 容器化   | Docker + Docker Compose    |
| 镜像仓库 | Harbor / Docker Registry   |
| CI/CD    | GitLab CI / GitHub Actions |
| 日志     | ELK / Loki + Grafana       |
| 监控     | Prometheus + Grafana       |

---

## 3. 模块划分

### 3.1 Java 业务模块

```
content-pipeline
├── pipeline-admin          # 后台管理入口
│   └── PipelineAdminApplication.java
├── pipeline-api            # 通用 API 定义（DTO、Feign 接口）
├── pipeline-common         # 通用工具类、异常、常量
├── pipeline-framework      # 核心框架配置（Spring Security、Redis、MQ）
├── pipeline-module-topic   # 选题管理模块
│   ├── controller
│   ├── service
│   ├── mapper
│   └── entity
├── pipeline-module-script  # 脚本管理模块
├── pipeline-module-storyboard # 分镜管理模块
├── pipeline-module-material   # 素材管理模块
├── pipeline-module-voice      # 配音管理模块
├── pipeline-module-edit       # 剪辑合成模块
├── pipeline-module-publish    # 发布管理模块
├── pipeline-module-task       # 任务调度与状态管理
├── pipeline-module-user       # 用户与权限模块
├── pipeline-module-analytics  # 数据分析模块
├── pipeline-module-ai         # AI Gateway（与 Python 服务通信）
└── pipeline-module-template   # 模板管理（Prompt 模板、分镜模板）
```

### 3.2 Python AI 服务

```
ai-services/
├── gateway/                # AI Gateway — 统一模型路由
│   ├── main.py
│   ├── routers/
│   │   ├── chat.py
│   │   ├── generate.py
│   │   ├── models.py
│   │   ├── script.py
│   │   ├── prompt.py
│   │   ├── image.py
│   │   ├── video.py
│   │   └── voice.py
│   ├── providers/
│   │   ├── base.py                 # 抽象基类（async def 定义）
│   │   ├── registry.py             # 全局 Provider 注册表（共享实例）
│   │   ├── openai_provider.py
│   │   ├── claude_provider.py
│   │   ├── deepseek_provider.py
│   │   ├── sensenova_provider.py
│   │   ├── keling_provider.py
│   │   ├── veo_provider.py
│   │   └── doubao_provider.py
│   └── __init__.py
├── script-service/         # 脚本生成服务
│   ├── main.py
│   └── generators/
├── prompt-service/         # Prompt 生成服务
├── video-service/          # 视频生成服务
│   ├── main.py
│   └── generators/
├── image-service/          # 图片生成服务
├── voice-service/          # 配音服务
│   ├── main.py
│   └── tts/
├── ffmpeg-service/         # 剪辑合成服务
│   ├── main.py
│   ├── subtitle.py         # 字幕生成
│   ├── composite.py        # 视频合成
│   ├── audio.py            # 音频处理
│   └── cover.py            # 封面生成
└── common/                 # 公共库
    ├── models/
    ├── utils/
    └── config/
```

---

## 4. 数据模型设计

### 4.1 核心表结构（ER 描述）

```
┌─────────────────────┐       ┌─────────────────────┐
│        topic        │       │        task         │
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │       │ id (PK)             │
│ title (选题标题)     │──┐    │ topic_id (FK)       │
│ source (来源)       │  │    │ title (视频标题)     │
│ source_url (来源URL)│  │    │ script_id (FK)      │
│ hot_score (热度评分) │  │    │ status (任务状态)    │
│ is_auto (是否自动采集)│  │    │ progress (进度 0-100)│
│ status (状态)       │  │    │ error_message       │
│ created_by          │  │    │ created_by          │
│ created_at          │  │    │ created_at          │
│ updated_at          │  │    │ updated_at          │
└─────────────────────┘  │    └────────┬────────────┘
                         │            │
                         │    ┌───────▼────────────┐
                         │    │      script        │
                         │    ├────────────────────┤
                         │    │ id (PK)            │
                         └────│ topic_id (FK)      │
                              │ task_id (FK)       │
                              │ title (视频标题)    │
                              │ content (文案全文)  │
                              │ subtitle (字幕文本) │
                              │ prompt_template_id │
                              │ version (版本号)    │
                              │ status (待审核/通过/驳回)│
                              │ created_by         │
                              │ created_at         │
                              │ updated_at         │
                              └────────────────────┘

┌─────────────────────┐      ┌─────────────────────┐
│    storyboard       │     │      material        │
├─────────────────────┤      ├─────────────────────┤
│ id (PK)             │      │ id (PK)             │
│ task_id (FK)        │      │ task_id (FK)        │
│ sequence (镜头序号)  │      │ storyboard_id (FK)  │
│ duration (时长 秒)   │      │ type (image/video)  │
│ scene_type (景别)    │      │ model (生成模型)     │
│ character (人物)     │      │ url (MinIO 地址)    │
│ action (动作)       │      │ prompt (生成 Prompt) │
│ environment (环境)   │      │ status (生成状态)    │
│ camera (运镜方式)    │      │ created_at         │
│ lighting (光影)      │      └─────────────────────┘
│ style (风格)        │
│ ai_prompt (AI Prompt)│
│ created_at          │
│ updated_at          │
└─────────────────────┘

┌─────────────────────┐      ┌─────────────────────┐
│       voice         │      │   publish_log       │
├─────────────────────┤      ├─────────────────────┤
│ id (PK)             │      │ id (PK)             │
│ task_id (FK)        │      │ task_id (FK)        │
│ voice_type (配音类型)│      │ platform (平台)     │
│ voice_url (音频地址) │      │ account_id (账号ID) │
│ speed (语速)        │      │ title (发布标题)    │
│ duration (时长)     │      │ cover_url (封面地址) │
│ status (生成状态)    │      │ tags (标签列表)     │
│ created_at          │      │ scheduled_at (定时)  │
│ updated_at          │      │ published_at (发布时间)│
└─────────────────────┘      │ status (发布状态)    │
                             │ platform_video_id   │
┌─────────────────────┐      │ created_at          │
│   ai_model_config   │      └─────────────────────┘
├─────────────────────┤
│ id (PK)             │      ┌─────────────────────┐
│ model_name (模型名称)│      │   platform_account  │
│ provider (提供商)    │      ├─────────────────────┤
│ api_key_encrypted   │      │ id (PK)             │
│ endpoint (接口地址)  │      │ platform (抖音/...) │
│ model_type (类型)    │      │ account_name        │
│ default_params (默认参数)    │      │ cookies_encrypted   │
│ rate_limit (速率限制)        │      │ status (状态)       │
│ weight (权重)                │      │ created_at          │
│ enabled (启用状态)           │      └─────────────────────┘
└─────────────────────┘

┌─────────────────────┐      ┌─────────────────────┐
│   prompt_template   │      │       user          │
├─────────────────────┤      ├─────────────────────┤
│ id (PK)             │      │ id (PK)             │
│ name (模板名称)      │      │ username            │
│ type (类型:脚本/Prompt)│    │ password            │
│ content (模板内容)   │      │ nickname            │
│ variables (变量列表)  │      │ role_id (FK)        │
│ enabled             │      │ status (启用/禁用)   │
│ created_at          │      │ created_at          │
│ updated_at          │      └─────────────────────┘
└─────────────────────┘
```

### 4.2 任务状态机 (task_status)

```
WAIT         → SCRIPTING              | CANCELLED
SCRIPTING    → SCRIPT_REVIEW          | ERROR | CANCELLED
SCRIPT_REVIEW → STORYBOARD (批准)     | WAIT (驳回) | CANCELLED
STORYBOARD   → GENERATING             | SCRIPT_REVIEW (脚本驳回) | ERROR | CANCELLED
GENERATING   → VOICEOVER              | SCRIPT_REVIEW (脚本驳回) | ERROR | CANCELLED
VOICEOVER    → VOICEOVER (素材就绪中)  | EDITING | SCRIPT_REVIEW (脚本驳回) | ERROR | CANCELLED
EDITING      → REVIEW                 | ERROR | CANCELLED
REVIEW       → READY (通过)            | WAIT (驳回) | CANCELLED
READY        → PUBLISHED              | CANCELLED
PUBLISHED    → (终态)
CANCELLED    → (终态)
ERROR        → WAIT (重试)            | CANCELLED
```

注意：`VOICEOVER` 支持自循环（素材就绪中场景），`STORYBOARD`、`GENERATING`、`VOICEOVER` 支持回退到 `SCRIPT_REVIEW`（脚本驳回后重新审核）。`SCRIPT_REVIEW` 驳回后进入 `WAIT`，`REVIEW` 驳回后也进入 `WAIT`，均需重新从脚本生成开始。

### 4.3 核心 SQL 示例

```sql
-- 任务表
CREATE TABLE task (
    id              BIGSERIAL PRIMARY KEY,
    topic_id        BIGINT REFERENCES topic(id),
    title           VARCHAR(200) NOT NULL,
    script_id       BIGINT REFERENCES script(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'WAIT',
    progress        INT DEFAULT 0,
    error_message   TEXT,
    created_by      BIGINT REFERENCES sys_user(id),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_status ON task(status);
CREATE INDEX idx_task_created_at ON task(created_at);

-- 分镜表
CREATE TABLE storyboard (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES task(id),
    sequence        INT NOT NULL,
    duration        INT NOT NULL DEFAULT 5,
    scene_type      VARCHAR(50),
    character       VARCHAR(200),
    action          TEXT,
    environment     VARCHAR(200),
    camera          VARCHAR(100),
    lighting        VARCHAR(100),
    style           VARCHAR(100),
    ai_prompt       TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_storyboard_task ON storyboard(task_id);

-- 发布日志表
CREATE TABLE publish_log (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES task(id),
    platform        VARCHAR(20) NOT NULL,
    account_id      BIGINT REFERENCES platform_account(id),
    title           VARCHAR(200),
    cover_url       VARCHAR(500),
    tags            VARCHAR(500),
    scheduled_at    TIMESTAMP,
    published_at    TIMESTAMP,
    status          VARCHAR(20) DEFAULT 'PENDING',
    platform_video_id VARCHAR(100),
    error_message   TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_publish_log_platform ON publish_log(platform, status);
```

---

## 5. API 接口设计

### 5.1 Java 后端 API（RESTful）

```
# 认证与健康
POST   /api/v1/auth/login                # 用户登录，返回 JWT Token
POST   /api/v1/auth/register             # 用户注册
GET    /api/v1/auth/me                   # 获取当前登录用户信息
GET    /api/v1/health                    # 健康检查

# 选题管理
POST   /api/v1/topics                    # 创建选题
GET    /api/v1/topics                    # 选题列表（分页 + 筛选）
GET    /api/v1/topics/{id}               # 选题详情
PUT    /api/v1/topics/{id}               # 更新选题
DELETE /api/v1/topics/{id}               # 删除选题
POST   /api/v1/topics/{id}/generate-task # 从选题创建生产任务

# 脚本管理
POST   /api/v1/scripts/generate         # AI 生成脚本（写入数据库后发送 MQ）
GET    /api/v1/scripts                   # 脚本列表（支持 taskId/topicId/status 筛选）
GET    /api/v1/scripts/{id}              # 脚本详情
PUT    /api/v1/scripts/{id}              # 编辑脚本（递增版本号）
POST   /api/v1/scripts/{id}/review       # 审核脚本（approve/reject）
POST   /api/v1/scripts/{id}/approve      # 批准脚本（推进到 STORYBOARD → 发送分镜生成 MQ）
POST   /api/v1/scripts/{id}/reject       # 驳回脚本（回退到 WAIT）
GET    /api/v1/scripts/{id}/versions     # 脚本版本历史

# 任务管理
POST   /api/v1/tasks                     # 创建生产任务（自动推进到 SCRIPTING → 发送 MQ）
GET    /api/v1/tasks                     # 任务列表（分页 + 状态筛选）
GET    /api/v1/tasks/{id}                # 任务详情
POST   /api/v1/tasks/{id}/retry          # 重试失败任务（重置到 WAIT → SCRIPTING）
POST   /api/v1/tasks/{id}/cancel         # 取消任务
GET    /api/v1/tasks/{id}/timeline       # 任务时间线
POST   /api/v1/tasks/callback            # Python 服务回调（受 X-Callback-Token 保护）

# 分镜管理
GET    /api/v1/tasks/{taskId}/storyboard                # 获取分镜列表
PUT    /api/v1/tasks/{taskId}/storyboard                # 批量更新分镜
POST   /api/v1/tasks/{taskId}/storyboard/auto-split     # AI 自动拆解分镜

# 素材管理
GET    /api/v1/materials                  # 素材列表
GET    /api/v1/materials/{id}             # 素材详情
DELETE /api/v1/materials/{id}             # 删除素材
POST   /api/v1/materials/batch-generate  # 批量生成素材

# 配音管理
POST   /api/v1/voices/generate           # 生成配音
GET    /api/v1/voices/{taskId}            # 获取配音状态
PUT    /api/v1/voices/{taskId}            # 更新配音参数

# 剪辑合成
POST   /api/v1/edits/compile             # 启动剪辑合成
POST   /api/v1/edits/{taskId}/compile    # 启动指定任务剪辑合成
GET    /api/v1/edits/{taskId}/preview     # 获取预览地址
POST   /api/v1/edits/{taskId}/regenerate  # 重新剪辑

# 发布管理
POST   /api/v1/publish                    # 创建发布记录
GET    /api/v1/publish                    # 发布列表（支持 taskId 筛选）
POST   /api/v1/publish/{id}/publish       # 执行发布
POST   /api/v1/publish/{id}/schedule      # 定时发布
POST   /api/v1/publish/{id}/cancel        # 取消发布
GET    /api/v1/publish/calendar           # 发布日历（支持 startDate/endDate 筛选）
GET    /api/v1/publish/accounts           # 平台账号列表（支持 platform 筛选）

# 数据分析
GET    /api/v1/analytics/overview         # 总览数据
GET    /api/v1/analytics/daily            # 日报数据（支持 startDate/endDate）
GET    /api/v1/analytics/topics           # 选题效果（支持 limit 参数）
GET    /api/v1/analytics/accounts         # 账号维度统计

# AI 模型配置
GET    /api/v1/ai-models                  # 模型配置列表
POST   /api/v1/ai-models                  # 新增模型配置
GET    /api/v1/ai-models/{id}             # 模型配置详情
PUT    /api/v1/ai-models/{id}             # 更新模型配置
DELETE /api/v1/ai-models/{id}             # 删除模型配置
POST   /api/v1/ai-models/{id}/test        # 测试模型连接
POST   /api/v1/ai-models/test-mq          # 测试 MQ 消息发送

# 系统管理
GET    /api/v1/templates                  # Prompt 模板列表
POST   /api/v1/templates                  # 创建模板
GET    /api/v1/templates/{id}             # 模板详情
PUT    /api/v1/templates/{id}             # 更新模板
DELETE /api/v1/templates/{id}             # 删除模板

GET    /api/v1/users                      # 用户列表
POST   /api/v1/users                      # 创建用户
GET    /api/v1/users/{id}                 # 用户详情
PUT    /api/v1/users/{id}                 # 更新用户
DELETE /api/v1/users/{id}                 # 删除用户

GET    /api/v1/roles                      # 角色列表
POST   /api/v1/roles                      # 创建角色
GET    /api/v1/roles/{id}                 # 角色详情
PUT    /api/v1/roles/{id}                 # 更新角色
DELETE /api/v1/roles/{id}                 # 删除角色

GET    /api/v1/platform-accounts          # 平台账号列表
POST   /api/v1/platform-accounts          # 新增平台账号
GET    /api/v1/platform-accounts/{id}     # 平台账号详情
PUT    /api/v1/platform-accounts/{id}     # 更新平台账号
DELETE /api/v1/platform-accounts/{id}     # 删除平台账号

# 文件管理
POST   /api/v1/files/upload               # 文件上传
GET    /api/v1/files/download             # 文件下载
GET    /api/v1/files                      # 文件列表
DELETE /api/v1/files                      # 删除文件
```

# 模板管理

CRUD   /api/v1/templates                  # Prompt 模板 CRUD

# 模型配置

CRUD   /api/v1/ai-models                  # AI 模型配置 CRUD
POST   /api/v1/ai-models/{id}/test        # 测试模型连接

# 系统管理

CRUD   /api/v1/users                      # 用户管理
CRUD   /api/v1/roles                      # 角色管理
POST   /api/v1/platform-accounts          # 平台账号管理

```

### 5.2 Python AI 服务 API（FastAPI）
```

# AI Gateway — 统一模型调用（gateway/routers/）

POST   /ai/v1/chat               # LLM 对话（自动匹配 Provider）
POST   /ai/v1/chat/{provider}    # 按 Provider 调用
POST   /ai/v1/generate           # 通用生成
POST   /ai/v1/embedding          # 向量化（预留）
GET    /ai/v1/models             # 模型列表（按 Provider 分组，含中文名映射）

# 脚本生成（gateway/routers/script.py）

POST   /ai/v1/script/generate    # 生成脚本
POST   /ai/v1/script/rewrite     # 重写脚本

# Prompt 生成（gateway/routers/prompt.py）

POST   /ai/v1/prompt/generate    # 分镜拆分

# 视频生成（gateway/routers/video.py）

POST   /ai/v1/video/generate     # 生成视频
GET    /ai/v1/video/{task_id}    # 查询视频生成状态

# 图片生成（gateway/routers/image.py）

POST   /ai/v1/image/generate     # 生成图片

# 配音服务（gateway/routers/voice.py）

POST   /ai/v1/voice/generate     # 生成配音
POST   /ai/v1/voice/clone        # 克隆声音
GET    /ai/v1/voice/{task_id}    # 查询配音状态

# FFmpeg 剪辑服务（gateway/routers/ffmpeg.py）

POST   /ai/v1/ffmpeg/composite   # 视频合成
POST   /ai/v1/ffmpeg/subtitle    # 字幕生成
POST   /ai/v1/ffmpeg/cover       # 封面生成
POST   /ai/v1/ffmpeg/audio       # 音频处理
GET    /ai/v1/ffmpeg/{task_id}   # 查询合成进度

# 各服务健康检查

GET    /health                    # 所有服务统一返回 {"status": "UP"|"ok", "service": "..."}

```

### 5.3 Java 调用 Python 的接口约定
```

Java → RabbitMQ → Python
Java ← HTTP Callback (POST /api/v1/tasks/callback) ← Python

Callback Body:
{
  "task_id": 123,
  "service": "script | video | voice | ffmpeg",
  "status": "SUCCESS | FAILED",
  "data": { ... },        // 成功时的结果数据
  "error": "..."          // 失败时的错误信息
}

```

---

## 6. AI Gateway 设计

### 6.1 架构
```

Python AI Gateway（:8001）
     │
     ├── OpenAI (GPT-4o, GPT-4o-mini, GPT-4.1)
     ├── Anthropic (Claude Sonnet 4.6, Opus 4.8, Haiku 4.5)
     ├── DeepSeek (v4-flash, v4-pro, 动态拉取模型列表)
     ├── SenseNova 商汤大装置 (SenseNova-5.0, 动态拉取模型列表)
     ├── 可灵 AI (视频生成, 动态拉取模型列表)
     ├── Google Veo (视频生成, 动态拉取模型列表)
     └── 豆包 (TTS 配音, 动态拉取模型列表)

```

**Provider 注册表（`gateway/providers/registry.py`）**：所有 Provider 实例统一管理，通过 `init_providers()` 初始化（幂等），`get_providers()` / `get_provider()` 获取共享实例。`refresh_all_models()` 使用 `asyncio.gather` 并行刷新所有已配置 API Key 的 Provider 模型列表。API Key 未配置的 Provider 模型列表为空，不会出现在模型列表中。服务关闭时调用 `shutdown()` 释放所有 HTTP 连接。

**Provider 模型列表策略**：
| Provider | 模型列表来源 | API Key 未配置时 |
|----------|-------------|-----------------|
| OpenAI | 启动时 `GET /v1/models` 动态拉取，失败时使用兜底列表 | 模型列表为空，不显示 |
| Claude | 启动时 `GET /v1/models` 动态拉取，失败时使用兜底列表 | 模型列表为空，不显示 |
| DeepSeek | 启动时 `GET /v1/models` 动态拉取，失败时使用兜底列表 | 模型列表为空，不显示 |
| SenseNova | 启动时 `GET /v1/models` 动态拉取，失败时使用兜底列表 | 模型列表为空，不显示 |
| 可灵 AI | 启动时 `GET /v1/models` 动态拉取，失败时使用兜底列表 | 模型列表为空，不显示 |
| Veo | 启动时 `GET /v1/models` 动态拉取，失败时使用兜底列表 | 模型列表为空，不显示 |
| 豆包 | 启动时从 API 动态拉取 TTS 模型列表，失败时使用兜底列表 | 模型列表为空，不显示 |

### 6.2 数据模型

```json
{
  "model_config": {
    "id": 1,
    "model_name": "claude-opus-4.7",
    "provider": "anthropic",
    "model_type": "chat",
    "api_key_encrypted": "...",
    "endpoint": "https://api.anthropic.com",
    "default_params": {
      "max_tokens": 4096,
      "temperature": 0.7
    },
    "rate_limit": {
      "requests_per_minute": 60,
      "tokens_per_minute": 100000
    },
    "enabled": true,
    "weight": 10
  }
}
```

### 6.3 调用流程

```
1. Java 业务代码发起 AI 调用请求（含模型类型 + Prompt）
2. Java AI Gateway 根据模型类型从数据库路由到模型配置
3. HTTP 请求转发到 Python AI Gateway
4. Python AI Gateway 根据 provider 调用对应 SDK
5. 返回结果（同步）或 TaskID（异步生成任务）
6. 异步任务完成后回调 Java 接口
```

---

## 7. 消息队列设计

### 7.1 队列定义

所有队列声明在 `RabbitConfig.java`，使用 `QueueBuilder.durable()` 持久化队列，并配置死信交换机（DLX）。

| 队列名称                     | 类型   | 消费者 | 说明                                      |
| ---------------------------- | ------ | ------ | ----------------------------------------- |
| `pipeline.script.generate` | Direct | Python | 脚本生成任务（已投入使用）                |
| `pipeline.prompt.generate` | Direct | Python | Prompt 生成任务（已投入使用）             |
| `pipeline.video.generate`  | Direct | Python | 视频生成任务（已投入使用）                |
| `pipeline.image.generate`  | Direct | Python | 图片生成任务（已投入使用）                |
| `pipeline.voice.generate`  | Direct | Python | 配音生成任务（已投入使用）                |
| `pipeline.ffmpeg.compile`  | Direct | Python | 剪辑合成任务（已投入使用）                |
| `pipeline.task.create`     | Direct | 预留   | 新任务创建事件（框架声明，尚未投入使用）  |
| `pipeline.task.callback`   | Direct | 预留   | Python 回调结果（框架声明，尚未投入使用） |
| `pipeline.dlq.task`        | Direct | —     | 死信队列，绑定到`pipeline.dlx` 交换机   |

### 7.2 死信机制

所有业务队列均配置了 `x-dead-letter-exchange: pipeline.dlx` 和 `x-dead-letter-routing-key: dlq.task`。当消息被拒绝（`basic.nack` with `requeue=false`）或超时后，自动进入死信队列 `pipeline.dlq.task`。
| `pipeline.voice.generate` | Direct | Python | 配音生成任务 |
| `pipeline.ffmpeg.compile` | Direct | Python | 剪辑合成任务 |
| `pipeline.task.callback` | Direct | Java | Python 回调结果 |

### 7.2 消息格式

```json
// Java → Python 任务消息
{
  "message_id": "uuid",
  "task_id": 123,
  "action": "generate_script | generate_video | ...",
  "params": {
    "model": "claude-opus-4.7",
    "prompt": "...",
    "temperature": 0.7,
    ...
  },
  "callback_url": "http://pipeline-admin/api/v1/tasks/callback",
  "timestamp": "2026-07-16T10:00:00Z"
}

// Python → Java 回调消息
{
  "message_id": "uuid",
  "task_id": 123,
  "service": "script",
  "status": "SUCCESS",
  "data": {
    "script_content": "...",
    "title": "...",
    "subtitle": "..."
  },
  "error": null,
  "timestamp": "2026-07-16T10:01:00Z"
}
```

### 重试与错误处理

**MQ 消息失败处理**：Python 服务 `RabbitMQClient._wrap_callback()` 中，消息处理异常时调用 `basic_nack(requeue=false)`，消息进入死信队列 `pipeline.dlq.task`。

**任务重试**：Java 端 `TaskServiceImpl.retryTask()` 将任务状态从 `ERROR` 重置为 `WAIT`，然后立即推进到 `SCRIPTING`，最后在事务提交后重新发送脚本生成 MQ 消息。

**终态回调忽略**：`CallbackController` 中，如果任务已是 `CANCELLED` 或 `PUBLISHED`，忽略回调并返回 `"ignored"`。

---

## 8. 关键流程时序

### 8.1 创建视频任务

```
用户                  Java                    MQ                    Python
 │                     │                     │                      │
 ├─ POST /api/v1/tasks─►                     │                      │
 │                     │                     │                      │
 │                     ├─ INSERT task(WAIT)  │                      │
 │                     │                     │                      │
 │                     ├─ send → pipeline.task.create              │
 │                     │                     │                      │
 │                     ├─ listen ← pipeline.task.create            │
 │                     │                     │                      │
 │                     ├─ UPDATE task(SCRIPTING)                   │
 │                     │                     │                      │
 │                     ├─ send → pipeline.script.generate ────────►│
 │                     │                     │                      │
 │ ◄── 返回 task_id ───┤                     │                      │
 │                     │                     │                      │
 │                     │                     │     Python 开始生成脚本
 │                     │                     │           ...
 │                     │                     │     Python 生成完成
 │                     │                     │                      │
 │                     │◄── callback ────────┼──────────────────────┤
 │                     │                     │                      │
 │                     ├─ UPDATE script + task(STORYBOARD)         │
 │                     │                     │                      │
```

### 8.2 完整流水线（异步编排）

```
task.create
    │
    ▼
script.generate ── callback ──► script_review (人工)
                                        │
                                        ▼
                               storyboard.auto-split
                                        │
                                        ▼
                               prompt.generate
                                        │
                                        ▼
                               video.generate (拆分为多段)
                                        │
                                        ▼
                               voice.generate
                                        │
                                        ▼
                               ffmpeg.compile
                                        │
                                        ▼
                               review (人工终审)
                                        │
                                        ▼
                               publish
```

---

## 9. 素材存储策略

### 9.1 MinIO Bucket 设计

Bucket 配置在 `MinIOConfig.java` 中，通过 `@ConfigurationProperties(prefix = "minio")` 绑定：

| Bucket 名称               | 配置属性                      | 说明                  | 生命周期        |
| ------------------------- | ----------------------------- | --------------------- | --------------- |
| `pipeline-scripts`      | `minio.bucket-scripts`      | 脚本文件              | 永久            |
| `pipeline-images`       | `minio.bucket-images`       | 生成的图片素材        | 30 天后可清理   |
| `pipeline-videos-raw`   | `minio.bucket-videos-raw`   | AI 生成的原始视频片段 | 7 天后可清理    |
| `pipeline-videos-final` | `minio.bucket-videos-final` | 最终合成的成片        | 永久            |
| `pipeline-voices`       | `minio.bucket-voices`       | 配音音频              | 永久            |
| `pipeline-covers`       | `minio.bucket-covers`       | 封面图片              | 永久            |
| `pipeline-temp`         | `minio.bucket-temp`         | 临时文件              | 24 小时自动清理 |

**Java 端实现**：使用 AWS SDK for Java v2 (`S3Client`)，配置 `forcePathStyle=true` 以兼容 MinIO 的非 AWS 路径格式。Python 端使用 `boto3`（S3 兼容接口）。两者均可通过修改 `endpoint` 切换存储服务（MinIO / AWS S3 / 阿里云 OSS / 腾讯 COS），无需改动代码。

### 9.2 文件命名规范

```
{module}/{task_id}/{timestamp}_{sequence}.{ext}

示例：
videos-raw/123/20260716_01.mp4
videos-raw/123/20260716_02.mp4
videos-final/123/20260716_final.mp4
voices/123/20260716_voice.mp3
covers/123/20260716_cover.png
```

---

## 10. 安全设计

### 10.1 认证与授权

- **Spring Security + JWT 无状态认证**：`SecurityConfig.java` 配置，`JwtAuthFilter` 从请求头 `Authorization: Bearer <token>` 提取并验证 JWT
- **回调令牌认证**：`JwtAuthFilter` 同时支持 `X-Callback-Token` 请求头，匹配则跳过 JWT 验证。用于 AI Gateway 内部调用（如同步模型配置），令牌通过 `CALLBACK_TOKEN` 环境变量配置
- **密码加密**：使用 `BCryptPasswordEncoder` 存储用户密码哈希
- **Session 管理**：`SessionCreationPolicy.STATELESS`（无状态，每次请求独立认证）
- **CORS**：配置为允许所有来源（`*`），所有 HTTP 方法，所有请求头
- **CSRF**：禁用（API 使用 Token 认证，无需 CSRF 保护）
- **公开端点**（无需认证）：
  - `POST /api/v1/auth/login` — 用户登录
  - `POST /api/v1/auth/register` — 用户注册
  - `GET /api/v1/health` — 健康检查
  - `POST /api/v1/tasks/callback` — Python 服务回调（需 `X-Callback-Token` 头）
  - `GET /api/v1/ai-models` — AI 模型配置列表（需 `X-Callback-Token` 头，供 Gateway 内部同步）
- RBAC 权限模型（管理员 / 运营 / 编辑 三级角色）
- **操作日志**：`OperationLogAspect` 通过 AOP 拦截 `@OperationLog(module, action)` 注解，自动记录操作人、模块、动作、耗时、参数、成功/失败状态
- **数据初始化**：`DataInitializer` 首次启动时自动创建默认管理员账号（`admin / admin123`）
- **加密密钥注入**：`EncryptionConfig` 在启动时将 `AES_ENCRYPTION_KEY` 环境变量注入 `EncryptionUtil`

### 10.2 数据安全

- **API Key 加密存储**：使用 `EncryptionUtil`（AES-256-GCM 模式），密钥通过 `AES_ENCRYPTION_KEY` 环境变量配置，默认密钥仅用于开发环境
- **平台账号 Cookie 加密存储**：同样使用 AES-256-GCM，每次加密生成 12 字节随机 IV
- **乐观锁**：`Task` 实体使用 `@Version` 注解，MyBatis-Plus 在更新时自动校验版本号，防止并发覆盖
- 参数校验：回调接口增加参数空值和格式校验
- HTTPS 传输加密（生产环境）
- MinIO 访问凭证定期轮换

### 10.3 AI 安全

- **AI 生成内容审核**：`ContentValidator` 提供敏感词过滤（`checkSensitiveWords()`）和 Prompt 注入防护（`hasPromptInjection()`）
- **Prompt 注入防护**：正则匹配 `ignore previous instructions`、`forget all previous`、`role play`、`system prompt` 等注入模式
- 模型调用频率限制（Rate Limiting）

### 10.4 异常处理

`GlobalExceptionHandler`（`@RestControllerAdvice`）统一拦截异常，返回标准 `Result` 格式：

| 异常类型                            | HTTP 状态码 | 说明                            |
| ----------------------------------- | ----------- | ------------------------------- |
| `MethodArgumentNotValidException` | 400         | 参数校验失败（`@Valid` 注解） |
| `BindException`                   | 400         | 参数绑定失败                    |
| `HttpMessageNotReadableException` | 400         | 请求体 JSON 格式错误            |
| `IllegalArgumentException`        | 400         | 参数非法（如任务不存在）        |
| `IllegalStateException`           | 400         | 状态机非法转换                  |
| `Exception`（兜底）               | 500         | 未捕获的服务器内部错误          |

---

## 11. 内容产出方式

### 11.1 概述

当前系统只支持视频生产流水线。新增内容产出方式，让同一套选题/脚本流程可以产出不同形态的内容：

| 产出方式 | 产出内容 | 生产流程 |
|----------|----------|----------|
| `video` | 完整视频 | 选题→脚本→分镜→素材→配音→剪辑→发布（现有流程） |
| `text` | 文案/脚本文字 | 选题→脚本→审核→发布 |
| `image` | 信息图/海报 | 选题→脚本→分镜→图片生成→发布 |
| `image_text` | 图片+文案 | 选题→脚本→分镜→图片生成→排版→发布 |

### 11.2 数据模型

Task 实体新增 `content_type` 字段：

```sql
ALTER TABLE task ADD COLUMN content_type VARCHAR(20) NOT NULL DEFAULT 'video';
```

### 11.3 状态机

不同 content_type 走不同状态路径：

```
text:       WAIT → SCRIPTING → SCRIPT_REVIEW → READY → PUBLISHED
image:      WAIT → SCRIPTING → SCRIPT_REVIEW → STORYBOARD → GENERATING → REVIEW → READY → PUBLISHED
image_text: WAIT → SCRIPTING → SCRIPT_REVIEW → STORYBOARD → GENERATING → EDITING → REVIEW → READY → PUBLISHED
video:      WAIT → SCRIPTING → SCRIPT_REVIEW → STORYBOARD → GENERATING → VOICEOVER → EDITING → REVIEW → READY → PUBLISHED
```

关键变更：
- `SCRIPT_REVIEW → READY` 新增转换（纯文案审核通过后直达待发布）
- `GENERATING → REVIEW` 图片/图文模式（素材生成后直接进入终审，跳过配音和剪辑）

### 11.4 回调处理

`CallbackController.handleSuccess()` 根据 `task.contentType` 分支：

- **prompt 回调**：`image`/`image_text` 只触发图片生成 MQ，不触发视频
- **image 回调**：`image`/`image_text` 直接进入 REVIEW，`video` 进入 VOICEOVER
- **video 回调**：仅 `video` 类型处理
- **voice 回调**：仅 `video` 类型处理

### 11.5 前端适配

- 任务列表新增"类型"列，显示视频/文案/图片/图文标签
- 选题列表创建任务时弹出产出方式选择器
- 任务详情根据 content_type 动态显示操作按钮（纯文案不显示分镜按钮等）
