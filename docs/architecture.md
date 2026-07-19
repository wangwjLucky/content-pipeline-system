# 内容生产流水线系统 — 架构总览

> 版本：v1.1 | 日期：2026-07-19

---

## 1. 系统架构

### 1.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                          客户端层                                    │
│              Vue 3 + Ant Design Vue + Pinia                         │
│              Web 后台管理界面（:80 / :3000）                        │
├─────────────────────────────────────────────────────────────────────┤
│                          业务层（Java）                              │
│              Spring Boot 3.2 + Java 17 + MyBatis-Plus              │
│              pipeline-admin（:8080）                                 │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐           │
│  │选题  │ │任务  │ │脚本  │ │分镜  │ │发布  │ │用户  │           │
│  │管理  │ │管理  │ │管理  │ │管理  │ │管理  │ │权限  │           │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘           │
├─────────────────────────────────────────────────────────────────────┤
│                        AI 服务层（Python）                           │
│           FastAPI + Python 3.12 + httpx + pika                      │
│                                                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│  │ AI       │  │ Script   │  │ Prompt   │  │ Video    │           │
│  │ Gateway  │←│ Service  │←│ Service  │←│ Service  │           │
│  │ :8001    │  │ :8002    │  │ :8003    │  │ :8004    │           │
│  └────┬─────┘  └──────────┘  └──────────┘  └──────────┘           │
│       │        ┌──────────┐  ┌──────────┐  ┌──────────┐           │
│       │        │ Voice    │  │ Image    │  │ FFmpeg   │           │
│       └────────│ Service  │←│ Service  │←│ Service  │           │
│                │ :8005    │  │ :8007    │  │ :8006    │           │
│                └──────────┘  └──────────┘  └──────────┘           │
├─────────────────────────────────────────────────────────────────────┤
│                       基础设施层                                     │
│  PostgreSQL 16  Redis 7  MinIO  RabbitMQ 3-management              │
│  :5432         :6379    :9000  :5672                               │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 架构原则

| 原则 | 说明 |
|------|------|
| **Java 做平台，Python 做 AI** | Java 负责业务逻辑和后台管理，Python 只负责 AI 能力 |
| **服务解耦** | Java 与 Python 通过 REST API + 消息队列通信 |
| **状态驱动** | 任务通过状态机流转，每一步状态变更持久化到数据库 |
| **异步编排** | 视频生产是长流程，全部异步处理，通过 MQ 驱动下游 |
| **AI Gateway** | 统一 AI 模型调用入口，新增模型无需改业务代码 |

---

## 2. 服务间通信

### 2.1 通信方式

| 通信场景 | 方式 | 协议 |
|---------|------|------|
| 前端 → Java 后台 | HTTP REST | JSON |
| Java → Python（异步） | RabbitMQ | AMQP（6 个队列） |
| Python → Java（回调） | HTTP POST | JSON（X-Callback-Token 认证） |
| Python → AI API | HTTP（通过 AI Gateway） | JSON |
| 所有服务 → MinIO | HTTP | S3 兼容 API |
| Java → PostgreSQL | JDBC | TCP :5432 |
| Java → Redis | Redis Protocol | TCP :6379 |

### 2.2 消息队列（RabbitMQ）

| 队列 | 生产者 | 消费者 | 说明 |
|------|--------|--------|------|
| `pipeline.script.generate` | Java | script-service | 触发脚本生成 |
| `pipeline.prompt.generate` | Java | prompt-service | 触发分镜拆分 |
| `pipeline.video.generate` | Java | video-service | 触发视频生成 |
| `pipeline.voice.generate` | Java | voice-service | 触发配音生成 |
| `pipeline.image.generate` | Java | image-service | 触发图片生成 |
| `pipeline.ffmpeg.compile` | Java | ffmpeg-service | 触发剪辑合成 |

### 2.3 回调认证

Python 服务回调 Java 时携带 `X-Callback-Token` 头，Java 端验证令牌有效性。

---

## 3. 任务状态机

```
WAIT → SCRIPTING → SCRIPT_REVIEW → STORYBOARD → GENERATING → VOICEOVER → EDITING → REVIEW → READY → PUBLISHED
  ↑                                                                                          ↑                  │
  │                                                                                          │                  │
  └───────────────────────────────── ERROR ──────────────────────────────────────────────────┘                  │
                                                                                                          CANCELLED (终态)
```

支持自循环和回退：`VOICEOVER` 可自循环（素材就绪中），`STORYBOARD`、`GENERATING`、`VOICEOVER` 可回退到 `SCRIPT_REVIEW`（脚本驳回后重新审核），`SCRIPT_REVIEW` 驳回后回到 `WAIT` 重新开始。

| 状态 | 说明 | 进度 |
|------|------|------|
| `WAIT` | 等待中 | 0% |
| `SCRIPTING` | 脚本生成中 | 10% |
| `SCRIPT_REVIEW` | 脚本审核中 | 30% |
| `STORYBOARD` | 分镜生成中 | 40% |
| `GENERATING` | 视频/图片素材生成中 | 50% |
| `VOICEOVER` | 配音生成中 | 60% |
| `EDITING` | 剪辑合成中 | 80% |
| `REVIEW` | 成片审核中 | 95% |
| `READY` | 待发布 | 95% |
| `PUBLISHED` | 已发布 | 100% |
| `CANCELLED` | 已取消 | — |
| `ERROR` | 处理失败 | — |

### 3.1 允许的状态转换（来自 `TaskStateMachine.java`）

| 当前状态 | 允许转换到 |
|---------|-----------|
| `WAIT` | `WAIT`(自循环), `SCRIPTING`, `CANCELLED` |
| `SCRIPTING` | `SCRIPT_REVIEW`, `ERROR`, `CANCELLED` |
| `SCRIPT_REVIEW` | `STORYBOARD`(脚本批准), `WAIT`(脚本驳回), `CANCELLED` |
| `STORYBOARD` | `GENERATING`, `SCRIPT_REVIEW`(脚本驳回), `ERROR`, `CANCELLED` |
| `GENERATING` | `VOICEOVER`, `SCRIPT_REVIEW`(脚本驳回), `ERROR`, `CANCELLED` |
| `VOICEOVER` | `VOICEOVER`(自循环,素材就绪中), `EDITING`, `SCRIPT_REVIEW`(脚本驳回), `ERROR`, `CANCELLED` |
| `EDITING` | `REVIEW`, `ERROR`, `CANCELLED` |
| `REVIEW` | `READY`(审核通过), `WAIT`(驳回), `CANCELLED` |
| `READY` | `PUBLISHED`, `CANCELLED` |
| `PUBLISHED` | (终态，无出站转换) |
| `CANCELLED` | (终态，无出站转换) |
| `ERROR` | `WAIT`(重试), `CANCELLED` |

> **注意**：`STORYBOARD` 及后续状态（`GENERATING`, `VOICEOVER`）支持回退到 `SCRIPT_REVIEW`，这是脚本被驳回后重新审核的场景。`SCRIPT_REVIEW` 驳回后进入 `WAIT`，`REVIEW` 驳回后也进入 `WAIT`，均需重新从脚本生成开始。`EDITING` 不支持直接回退到 `SCRIPT_REVIEW`。

---

## 4. 技术选型

### 4.1 Java 后台

| 组件 | 选型 |
|------|------|
| 框架 | Spring Boot 3.2.x |
| JDK | Java 17 |
| ORM | MyBatis-Plus 3.5.7 |
| 认证 | Spring Security + JWT |
| 消息队列 | Spring AMQP (RabbitMQ) |
| 缓存 | Spring Data Redis |
| 接口文档 | Knife4j (Swagger / OpenAPI 3) |
| 数据库 | PostgreSQL 16 |
| 构建 | Maven |

### 4.2 Python AI 服务

| 组件 | 选型 |
|------|------|
| Web 框架 | FastAPI |
| Python | 3.12+ |
| MQ 客户端 | pika |
| HTTP 客户端 | httpx |
| 配置管理 | Pydantic Settings |
| 日志 | logging + RotatingFileHandler |
| 音视频处理 | FFmpeg (subprocess) |
| 图片处理 | Pillow |
| 对象存储 | boto3 (MinIO S3) |

### 4.3 前端

| 组件 | 选型 |
|------|------|
| 框架 | Vue 3 + Composition API |
| UI 库 | Ant Design Vue 4.x |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |
| 构建 | Vite 5 |
| HTTP | Axios |
| 图表 | ECharts 6 |

---

## 5. 数据流

### 5.1 完整视频生产流程

```
用户创建任务
    │
    ▼
Java 创建任务记录 → 状态: WAIT
    │
    ▼ 事务提交后发送 MQ
RabbitMQ → Script Service
    │
    ▼ 回调 Java
脚本生成完成 → 状态: SCRIPT_REVIEW
    │
    ▼ 用户审批
脚本通过 → 状态: STORYBOARD
    │
    ▼ 发送 MQ
RabbitMQ → Prompt Service
    │
    ▼ 回调 Java
分镜生成完成 → 状态: GENERATING
    │
    ▼ 发送 MQ
RabbitMQ → Video Service / Image Service
    │
    ▼ 回调 Java
素材生成完成 → 状态: VOICEOVER
    │
    ▼ 发送 MQ
RabbitMQ → Voice Service
    │
    ▼ 回调 Java
配音生成完成 → 状态: EDITING
    │
    ▼ 发送 MQ
RabbitMQ → FFmpeg Service
    │
    ▼ 回调 Java
剪辑合成完成 → 状态: REVIEW
    │
    ▼ 用户审核
审核通过 → 状态: READY
    │
    ▼ 发布
已发布 → 状态: PUBLISHED
```

### 5.2 错误处理流程

```
任何 Python 服务处理失败
    │
    ├── 发送失败回调 → Java 更新状态为 ERROR
    │
    └── 用户可在前端点击重试
        │
        ▼
        Java 重置状态为 WAIT → 重新发送 MQ
```

---

## 6. 安全设计

| 安全措施 | 说明 |
|---------|------|
| JWT 认证 | 前端登录后获取 Token，后续请求携带 Bearer Token |
| 回调认证 | Python → Java 回调需携带 X-Callback-Token |
| 密码加密 | 用户密码使用 BCrypt 加密存储 |
| API Key 加密 | AI 模型 API Key 使用 AES-256-GCM 加密存储 |
| Cookies 加密 | 平台账号 Cookies 使用 AES-256-GCM 加密存储 |
| 乐观锁 | Task 实体使用 @Version 防止并发更新覆盖 |
| 参数校验 | 回调接口增加参数空值和格式校验 |

---

## 7. 部署架构

### 7.1 Docker 单机部署

```yaml
# 共 13 个容器
services:
  postgres          # 数据库
  redis             # 缓存
  minio             # 对象存储
  rabbitmq          # 消息队列
  pipeline-admin    # Java 后台（:8080）
  ai-gateway        # AI 网关（:8001）
  script-service    # 脚本服务（:8002）
  prompt-service    # 分镜服务（:8003）
  video-service     # 视频服务（:8004）
  voice-service     # 配音服务（:8005）
  ffmpeg-service    # 剪辑服务（:8006）
  image-service     # 图片服务（:8007）
  frontend          # 前端（:80）
```

### 7.2 水平扩展

| 服务 | 扩展方式 | 说明 |
|------|---------|------|
| pipeline-admin | 多实例 + Nginx 负载均衡 | 无状态，Session 存入 Redis |
| Python 服务 | 多实例 + MQ 竞争消费 | 自动拉取 MQ 消息 |
| RabbitMQ | Cluster 模式 | 3 节点集群 |
| PostgreSQL | 主从 + 读写分离 | 写主库，读从库 |
| MinIO | 分布式模式 | Erasure Code 数据保护 |

详见 [Docker 操作指南](docker-guide.md) 和 [启动指南](startup-guide.md)。