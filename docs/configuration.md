# 内容生产流水线系统 — 配置体系文档

---

## 目录

1. [架构概览](#1-架构概览)
2. [配置注入链路](#2-配置注入链路)
3. [Python 服务配置](#3-python-服务配置)
4. [Java 后端配置](#4-java-后端配置)
5. [基础设施组件](#5-基础设施组件)
6. [部署环境配置](#6-部署环境配置)
7. [配置清单（完整字段一览）](#7-配置清单完整字段一览)
8. [常见问题](#8-常见问题)

---

## 1. 架构概览

系统采用 **每服务独立配置** 的策略，每个模块有自己的配置文件，部署时只读取本服务需要的配置项。

```
┌─────────────────────────────────────────────────────────────────┐
│                      docker-compose.yml                         │
│  根目录 .env  →  基础设施密码（DB / MinIO / RabbitMQ）          │
│  各服务 .env  →  服务自身配置（API Key、连接地址等）            │
└─────────────────────────────────────────────────────────────────┘
         │ env_file                          │ environment
         ▼                                   ▼
┌──────────────────────┐          ┌──────────────────────┐
│ Python 微服务（×7）   │          │ Java 后端             │
│ gateway / script /    │          │ pipeline-manager     │
│ prompt / image / ...  │          │ Spring Boot          │
│ Pydantic Settings     │          │ ${ENV_VAR:default}   │
└──────────────────────┘          └──────────────────────┘
```

### 配置来源优先级（高 → 低）

| 优先级 | 来源                            | 说明                                                               |
| ------ | ------------------------------- | ------------------------------------------------------------------ |
| 1      | **容器环境变量**          | `docker-compose.yml` 的 `environment:` 块，直接注入容器进程    |
| 2      | **Service `.env` 文件** | `docker-compose.yml` 的 `env_file:` 指令，将文件内容加载到容器 |
| 3      | **本地 `.env` 文件**    | 本地开发时`python main.py` 运行目录下的 `.env`                 |
| 4      | **代码内默认值**          | Pydantic`field(default=...)` 或 Java `:default` 语法           |

> **为什么 `environment` 优先级高于 `env_file`？**
> 这是 Docker Compose 的行为：`environment` 和 `env_file` 都会把变量注入容器，但如果同名，`environment` 的值会覆盖 `env_file` 的值。我们利用这个机制做环境适配：
>
> - `.env` 文件里写 `PIPELINE_RABBITMQ_HOST=localhost`（本地开发）
> - `environment` 里覆盖 `PIPELINE_RABBITMQ_HOST: rabbitmq`（Docker 容器内用服务名访问）

---

## 2. 配置注入链路

### 2.1 Python 服务（以 Gateway 为例）

```
docker-compose.yml                     Dockerfile                      Python
─────────────────                     ──────────                      ──────
env_file: ./ai-services/gateway/.env   CMD: uvicorn gateway.main:app    Pydantic Settings
  └─ PIPELINE_DEEPSEEK_API_KEY=xxx          ↓                          settings.deepseek_api_key
environment:                                                          model_config = {
  └─ PIPELINE_RABBITMQ_HOST=rabbitmq       容器进程环境变量                env_prefix = "PIPELINE_",
                                                                        env_file = ".env"
                                                                      }
```

**关键机制：**

- Pydantic Settings 的 `env_file = ".env"` 是 **相对路径**，从进程工作目录（CWD）解析
- 本地开发时：`cd ai-services/gateway && python main.py` → 读取 `gateway/.env`
- Docker 部署时：`env_file` 将 `.env` 注入容器，`environment` 覆盖特定变量
- `env_prefix = "PIPELINE_"` 表示环境变量需以 `PIPELINE_` 开头

#### `environment` vs `env_file` 实际解析

以 `docker-compose.yml` 中的 `ai-gateway` 服务为例：

```yaml
ai-gateway:
  env_file: ./ai-services/gateway/.env    # ① 加载 .env 文件到容器
  environment:                             # ② 覆盖特定变量，适配容器网络
    PIPELINE_RABBITMQ_HOST: rabbitmq       #    localhost → 容器服务名
    PIPELINE_MINIO_ENDPOINT: http://minio:9000
    PIPELINE_GATEWAY_URL: http://ai-gateway:8001
    PIPELINE_CALLBACK_TOKEN: ${CALLBACK_TOKEN:-pipeline-callback-token-change-in-prod}
```

**执行流程：**

```
步骤 ①  env_file 加载
  └─ gateway/.env 中的变量全部注入容器
     PIPELINE_RABBITMQ_HOST=localhost
     PIPELINE_DEEPSEEK_API_KEY=sk-xxx
     ...

步骤 ②  environment 覆盖（同名变量）
  └─ PIPELINE_RABBITMQ_HOST=rabbitmq    ← 覆盖 localhost
  └─ PIPELINE_CALLBACK_TOKEN=${CALLBACK_TOKEN}  ← 从根 .env 读取

步骤 ③  容器内最终环境变量
  └─ PIPELINE_RABBITMQ_HOST=rabbitmq    ← 覆盖后的值
  └─ PIPELINE_DEEPSEEK_API_KEY=sk-xxx   ← 来自 .env 文件，未被覆盖
  └─ PIPELINE_CALLBACK_TOKEN=xxx        ← 来自根 .env 的 ${CALLBACK_TOKEN}
```

**为什么这样做？**

| 场景                  | RabbitMQ 地址                     | 原因                                             |
| --------------------- | --------------------------------- | ------------------------------------------------ |
| 本地开发`.env`      | `localhost`                     | 服务直接在宿主机运行，连本机 RabbitMQ            |
| Docker`environment` | `rabbitmq`                      | 容器内通过 Docker DNS 访问 RabbitMQ 容器         |
| 统一配置              | 同一份`.env` 文件两种环境都能用 | `environment` 的优先级更高，覆盖 `.env` 的值 |

**其他 Python 服务同样的模式：**

```yaml
script-service:
  env_file: ./ai-services/script-service/.env
  environment:
    PIPELINE_RABBITMQ_HOST: rabbitmq
    PIPELINE_GATEWAY_URL: http://ai-gateway:8001
    PIPELINE_CALLBACK_TOKEN: ${CALLBACK_TOKEN:-...}

prompt-service:
  env_file: ./ai-services/prompt-service/.env
  environment:
    PIPELINE_RABBITMQ_HOST: rabbitmq
    PIPELINE_GATEWAY_URL: http://ai-gateway:8001
    PIPELINE_CALLBACK_TOKEN: ${CALLBACK_TOKEN:-...}
```

**关键原则：** `.env` 文件只存"本服务特有的配置"（如 API Key、本地地址），`environment` 只覆盖"容器网络相关的地址"（`localhost` → 服务名）。

#### Docker 容器网络解析

```yaml
services:
  rabbitmq:                    # ① 服务名 = rabbitmq
    image: rabbitmq:3-management
    # ...

  ai-gateway:                  # ② 其他服务通过服务名访问
    environment:
      PIPELINE_RABBITMQ_HOST: rabbitmq   # ③ 直接写服务名
```

当 `docker compose up` 启动时，Docker Compose 会自动创建一个网络（默认名为 `项目名_default`），所有服务都加入这个网络。Docker 内置 DNS 将服务名 `rabbitmq` 解析为 RabbitMQ 容器的 IP 地址。

```
容器网络（bridge）
┌─────────────────────────────────────────────────┐
│                                                   │
│  rabbitmq (172.18.0.2:5672)                      │
│    ↑  Docker DNS 解析 "rabbitmq" → 172.18.0.2    │
│                                                   │
│  ai-gateway                                       │
│    └─ 代码读取 settings.rabbitmq_host             │
│       → 得到 "rabbitmq"（来自环境变量）            │
│       → 连接 rabbitmq:5672                        │
│       → Docker DNS 解析 → 172.18.0.2:5672          │
│                                                   │
│  script-service                                   │
│    └─ 同样连接 rabbitmq:5672                      │
│                                                   │
└─────────────────────────────────────────────────┘
```

**为什么本地开发用 `localhost`？**
因为本地开发时服务直接在宿主机运行，RabbitMQ 也在宿主机上（通过 `docker compose up -d rabbitmq` 启动），所以通过 `localhost:5672` 访问。

**为什么 Docker 内用服务名 `rabbitmq`？**
因为每个服务都在自己的容器里，通过 `localhost` 只能访问自己容器内部，不能访问其他容器。必须通过 Docker DNS 用服务名来访问其他容器。

### 2.2 Java 后端

```
docker-compose.yml                     Spring Boot
─────────────────                     ────────────
environment:                           application.yml
  spring.datasource.url: jdbc:...       password: ${DB_PASSWORD:pipeline123}
  DB_PASSWORD: ${DB_PASSWORD}           → 优先取环境变量 DB_PASSWORD
  CALLBACK_TOKEN: ${CALLBACK_TOKEN}     → 取不到则用默认值 pipeline123
```

**关键机制：**

- `application.yml` 中通过 `${VAR_NAME:default}` 语法读取环境变量
- `application-dev.yml` 在 `dev` profile 下覆盖默认值
- `docker-compose.yml` 的 `environment:` 块直接注入环境变量到容器

---

## 3. Python 服务配置

### 3.1 共享配置类

**文件：** `ai-services/common/config.py`

```python
class Settings(BaseSettings):
    # 应用
    app_name: str = "content-pipeline"
    debug: bool = False

    # RabbitMQ
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_username: str = "pipeline"
    rabbitmq_password: str = "pipeline123"

    # MinIO
    minio_endpoint: str = "http://localhost:9000"
    minio_access_key: str = "pipeline"
    minio_secret_key: str = "pipeline123"

    # AI Gateway
    gateway_url: str = "http://ai-gateway:8001"
    openai_api_key: str = ""
    anthropic_api_key: str = ""
    deepseek_api_key: str = ""
    sensenova_api_key: str = ""

    # 回调认证
    callback_token: str = "pipeline-callback-token-change-in-prod"

    model_config = {"env_prefix": "PIPELINE_", "env_file": ".env"}

settings = Settings()  # 全局单例，各服务共享
```

**字段映射规则：** `PIPELINE_RABBITMQ_HOST` → `settings.rabbitmq_host`（去掉前缀、转小写）。

### 3.2 各服务 `.env` 文件

#### AI Gateway（`ai-services/gateway/.env`）

| 变量                           | 用途                       | 本地值                                     | Docker 值                                        |
| ------------------------------ | -------------------------- | ------------------------------------------ | ------------------------------------------------ |
| `PIPELINE_RABBITMQ_HOST`     | RabbitMQ 地址              | `localhost`                              | `rabbitmq`（由 `environment` 覆盖）          |
| `PIPELINE_RABBITMQ_PORT`     | RabbitMQ 端口              | `5672`                                   | `5672`                                         |
| `PIPELINE_RABBITMQ_USERNAME` | RabbitMQ 用户              | `pipeline`                               | `pipeline`                                     |
| `PIPELINE_RABBITMQ_PASSWORD` | RabbitMQ 密码              | `pipeline123`                            | `pipeline123`                                  |
| `PIPELINE_MINIO_ENDPOINT`    | MinIO 地址                 | `http://localhost:9000`                  | `http://minio:9000`（由 `environment` 覆盖） |
| `PIPELINE_MINIO_ACCESS_KEY`  | MinIO 访问密钥             | `pipeline`                               | `pipeline`                                     |
| `PIPELINE_MINIO_SECRET_KEY`  | MinIO 秘密密钥             | `pipeline123`                            | `pipeline123`                                  |
| `PIPELINE_DEEPSEEK_API_KEY`  | DeepSeek API Key           | `sk-xxx`                                 | `sk-xxx`                                       |
| `PIPELINE_OPENAI_API_KEY`    | OpenAI API Key             | （空）                                     | （空）                                           |
| `PIPELINE_ANTHROPIC_API_KEY` | Anthropic API Key          | （空）                                     | （空）                                           |
| `PIPELINE_SENSENOVA_API_KEY` | SenseNova API Key          | `sk-xxx`                                 | `sk-xxx`                                       |
| `PIPELINE_GATEWAY_URL`       | 自身地址（供其他服务调用） | `http://ai-gateway:8001`                 | `http://ai-gateway:8001`                       |
| `PIPELINE_CALLBACK_TOKEN`    | 回调认证令牌               | `pipeline-callback-token-change-in-prod` | 由`${CALLBACK_TOKEN}` 注入                     |
| `PIPELINE_DEBUG`             | 调试模式                   | `false`                                  | `false`                                        |

**Gateway 是唯一需要 AI API Key 的服务**，其他 Python 服务通过调用 Gateway 的 HTTP API 来使用 AI 能力。

**Provider 注册表**：`gateway/providers/registry.py` 统一管理所有 Provider 实例，各 Router 通过 `get_providers()` / `get_provider()` 获取共享实例。`init_providers()` 初始化时调用每个 Provider 的 `refresh_models()` 动态拉取模型列表。所有 Provider 的 `chat()` 和 `generate()` 方法均为 `async def`，使用 `httpx.AsyncClient`。

#### Script Service（`ai-services/script-service/.env`）

| 变量                        | 用途            | 本地值                                     | Docker 值（覆盖）            |
| --------------------------- | --------------- | ------------------------------------------ | ---------------------------- |
| `PIPELINE_RABBITMQ_HOST`  | RabbitMQ 地址   | `localhost`                              | `rabbitmq`                 |
| `PIPELINE_RABBITMQ_PORT`  | RabbitMQ 端口   | `5672`                                   | `5672`                     |
| `PIPELINE_GATEWAY_URL`    | AI Gateway 地址 | `http://ai-gateway:8001`                 | `http://ai-gateway:8001`   |
| `PIPELINE_CALLBACK_TOKEN` | 回调认证令牌    | `pipeline-callback-token-change-in-prod` | 由`${CALLBACK_TOKEN}` 注入 |

#### Prompt Service（`ai-services/prompt-service/.env`）

| 变量                        | 用途            | 本地值                                     | Docker 值（覆盖）            |
| --------------------------- | --------------- | ------------------------------------------ | ---------------------------- |
| `PIPELINE_RABBITMQ_HOST`  | RabbitMQ 地址   | `localhost`                              | `rabbitmq`                 |
| `PIPELINE_RABBITMQ_PORT`  | RabbitMQ 端口   | `5672`                                   | `5672`                     |
| `PIPELINE_GATEWAY_URL`    | AI Gateway 地址 | `http://ai-gateway:8001`                 | `http://ai-gateway:8001`   |
| `PIPELINE_CALLBACK_TOKEN` | 回调认证令牌    | `pipeline-callback-token-change-in-prod` | 由`${CALLBACK_TOKEN}` 注入 |

#### Image Service（`ai-services/image-service/.env`）

| 变量                        | 用途            | 本地值                                     | Docker 值（覆盖）            |
| --------------------------- | --------------- | ------------------------------------------ | ---------------------------- |
| `PIPELINE_RABBITMQ_HOST`  | RabbitMQ 地址   | `localhost`                              | `rabbitmq`                 |
| `PIPELINE_RABBITMQ_PORT`  | RabbitMQ 端口   | `5672`                                   | `5672`                     |
| `PIPELINE_MINIO_ENDPOINT` | MinIO 地址      | `http://minio:9000`                      | `http://minio:9000`        |
| `PIPELINE_GATEWAY_URL`    | AI Gateway 地址 | `http://ai-gateway:8001`                 | `http://ai-gateway:8001`   |
| `PIPELINE_CALLBACK_TOKEN` | 回调认证令牌    | `pipeline-callback-token-change-in-prod` | 由`${CALLBACK_TOKEN}` 注入 |

#### Video Service（`ai-services/video-service/.env`）

| 变量                        | 用途            | 本地值                                     | Docker 值（覆盖）            |
| --------------------------- | --------------- | ------------------------------------------ | ---------------------------- |
| `PIPELINE_RABBITMQ_HOST`  | RabbitMQ 地址   | `localhost`                              | `rabbitmq`                 |
| `PIPELINE_RABBITMQ_PORT`  | RabbitMQ 端口   | `5672`                                   | `5672`                     |
| `PIPELINE_GATEWAY_URL`    | AI Gateway 地址 | `http://ai-gateway:8001`                 | `http://ai-gateway:8001`   |
| `PIPELINE_CALLBACK_TOKEN` | 回调认证令牌    | `pipeline-callback-token-change-in-prod` | 由`${CALLBACK_TOKEN}` 注入 |

#### Voice Service（`ai-services/voice-service/.env`）

| 变量                        | 用途            | 本地值                                     | Docker 值（覆盖）            |
| --------------------------- | --------------- | ------------------------------------------ | ---------------------------- |
| `PIPELINE_RABBITMQ_HOST`  | RabbitMQ 地址   | `localhost`                              | `rabbitmq`                 |
| `PIPELINE_RABBITMQ_PORT`  | RabbitMQ 端口   | `5672`                                   | `5672`                     |
| `PIPELINE_GATEWAY_URL`    | AI Gateway 地址 | `http://ai-gateway:8001`                 | `http://ai-gateway:8001`   |
| `PIPELINE_CALLBACK_TOKEN` | 回调认证令牌    | `pipeline-callback-token-change-in-prod` | 由`${CALLBACK_TOKEN}` 注入 |

#### FFmpeg Service（`ai-services/ffmpeg-service/.env`）

| 变量                        | 用途            | 本地值                                     | Docker 值（覆盖）            |
| --------------------------- | --------------- | ------------------------------------------ | ---------------------------- |
| `PIPELINE_RABBITMQ_HOST`  | RabbitMQ 地址   | `localhost`                              | `rabbitmq`                 |
| `PIPELINE_RABBITMQ_PORT`  | RabbitMQ 端口   | `5672`                                   | `5672`                     |
| `PIPELINE_GATEWAY_URL`    | AI Gateway 地址 | `http://ai-gateway:8001`                 | `http://ai-gateway:8001`   |
| `PIPELINE_CALLBACK_TOKEN` | 回调认证令牌    | `pipeline-callback-token-change-in-prod` | 由`${CALLBACK_TOKEN}` 注入 |

### 3.3 Docker 部署环境变量覆盖

```yaml
# docker-compose.yml 中每个 Python 服务的典型配置
script-service:
  env_file: ./ai-services/script-service/.env        # ① 基础配置
  environment:
    PIPELINE_RABBITMQ_HOST: rabbitmq                  # ② 覆盖 → 容器内服务名
    PIPELINE_GATEWAY_URL: http://ai-gateway:8001      # ② 覆盖 → 容器内服务名
    PIPELINE_CALLBACK_TOKEN: ${CALLBACK_TOKEN:-...}   # ② 覆盖 → 从根目录 .env 读取
```

> **注意：** `env_file` 加载的变量会被 `environment` 块中同名的变量覆盖。Docker 利用此机制实现：本地开发用 `localhost`，容器内用服务名（`rabbitmq`、`minio`）。

---

## 4. Java 后端配置

### 4.1 配置文件结构

```
pipeline-manager/src/main/resources/
├── application.yml          # 基础配置（含环境变量占位符）
└── application-dev.yml      # dev profile 覆盖
```

### 4.2 `application.yml` 主要配置

| 配置项                         | 环境变量              | 默认值                                        | 说明            |
| ------------------------------ | --------------------- | --------------------------------------------- | --------------- |
| `server.port`                | —                    | `8080`                                      | 服务端口        |
| `spring.datasource.url`      | —                    | `jdbc:postgresql://localhost:5432/pipeline` | 数据库连接      |
| `spring.datasource.password` | `DB_PASSWORD`       | `pipeline123`                               | 数据库密码      |
| `spring.rabbitmq.host`       | —                    | `localhost`                                 | RabbitMQ 地址   |
| `spring.rabbitmq.password`   | —                    | `pipeline123`                               | RabbitMQ 密码   |
| `jwt.secret`                 | `JWT_SECRET`        | `pipeline-secret-key-change-in-prod`        | JWT 签名密钥    |
| `pipeline.callback-base-url` | `CALLBACK_BASE_URL` | `http://host.docker.internal:8080`          | AI 服务回调地址 |
| `pipeline.callback-token`    | `CALLBACK_TOKEN`    | `pipeline-callback-token-change-in-prod`    | 回调认证令牌    |
| `minio.endpoint`             | —                    | `http://localhost:9000`                     | MinIO 地址      |
| `minio.access-key`           | —                    | `pipeline`                                  | MinIO 访问密钥  |
| `minio.secret-key`           | —                    | `pipeline123`                               | MinIO 秘密密钥  |

### 4.3 `application-dev.yml` 覆盖

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/pipeline  # 支持 DB_HOST 环境变量
    password: ${DB_PASSWORD:pipeline123}
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    password: ${RABBITMQ_PASSWORD:pipeline123}
```

### 4.4 Docker 部署注入

```yaml
# docker-compose.yml
pipeline-admin:
  environment:
    SPRING_PROFILES_ACTIVE: dev                       # 激活 dev profile
    DB_PASSWORD: ${DB_PASSWORD:-pipeline123}          # 数据库密码
    spring.datasource.url: jdbc:postgresql://postgres:5432/pipeline  # 容器内地址
    spring.rabbitmq.host: rabbitmq                    # 容器内服务名
    spring.data.redis.host: redis                     # 容器内服务名
    minio.endpoint: http://minio:9000                 # 容器内服务名
    CALLBACK_TOKEN: ${CALLBACK_TOKEN:-...}            # 回调令牌
```

---

## 5. 基础设施组件

以下组件的密码由**根目录 `.env`** 文件统一管理，`docker-compose.yml` 通过 `${VAR}` 语法引用：

| 组件       | 镜像                      | 默认端口                             | 认证变量                                  | 默认值                         |
| ---------- | ------------------------- | ------------------------------------ | ----------------------------------------- | ------------------------------ |
| PostgreSQL | `postgres:16`           | `5432`                             | `DB_PASSWORD`                           | `pipeline123`                |
| MinIO      | `minio/minio`           | `9000`（API）/ `9001`（Console） | `MINIO_USER` / `MINIO_PASSWORD`       | `pipeline` / `pipeline123` |
| RabbitMQ   | `rabbitmq:3-management` | `5672`（AMQP）/ `15672`（UI）    | `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | `pipeline` / `pipeline123` |
| Redis      | `redis:7-alpine`        | `6379`                             | 无认证                                    | —                             |

**根目录 `.env` 文件示例：**

```bash
# 基础设施密码（仅 docker-compose.yml 读取）
DB_PASSWORD=pipeline123
MINIO_USER=pipeline
MINIO_PASSWORD=pipeline123
RABBITMQ_USER=pipeline
RABBITMQ_PASSWORD=pipeline123
JWT_SECRET=pipeline-secret-key-change-in-prod
CALLBACK_TOKEN=pipeline-callback-token-change-in-prod
CALLBACK_BASE_URL=http://pipeline-admin:8080
```

---

## 6. 部署环境配置

### 6.1 本地开发

```bash
# 1. 启动基础设施（PostgreSQL、MinIO、RabbitMQ、Redis）
docker compose up -d postgres minio rabbitmq redis

# 2. 启动 AI Gateway（读取 gateway/.env）
cd ai-services/gateway
python main.py

# 3. 启动其他 Python 服务（每个服务读取自己的 .env）
cd ai-services/script-service && python main.py &
cd ai-services/prompt-service && python main.py &
# ...

# 4. 启动 Java 后端
cd pipeline-manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 5. 启动前端
cd frontend
npm run dev
```

**关键点：** 每个 Python 服务必须在自己的目录下运行，否则 `env_file = ".env"` 找不到正确路径。

### 6.2 Docker Compose 部署

```bash
# 一键启动所有服务
docker compose up -d

# 配置来源链：
#   根目录 .env  →  docker-compose.yml 的 ${VAR} 引用
#   各服务 .env  →  docker-compose.yml 的 env_file 指令
#   environment  →  覆盖容器内连接地址为服务名
```

### 6.3 生产部署

生产环境建议：

1. **禁止使用 `.env` 文件存储敏感信息**，改用容器编排平台的 Secrets 管理（如 Kubernetes Secrets、Docker Swarm Secrets）
2. 通过环境变量直接注入，覆盖 `.env` 中的值
3. 密码和密钥使用强随机值：`openssl rand -hex 32`
4. 各服务依然独立配置，互不依赖

---

## 7. 配置清单（完整字段一览）

### 7.1 Python 服务（PIPELINE_ 前缀）

| 环境变量                       | 类型     | 默认值                                     | 使用方          | 说明              |
| ------------------------------ | -------- | ------------------------------------------ | --------------- | ----------------- |
| `PIPELINE_APP_NAME`          | `str`  | `content-pipeline`                       | 所有服务        | 应用名称          |
| `PIPELINE_DEBUG`             | `bool` | `False`                                  | 所有服务        | 调试模式          |
| `PIPELINE_RABBITMQ_HOST`     | `str`  | `localhost`                              | 所有服务        | RabbitMQ 地址     |
| `PIPELINE_RABBITMQ_PORT`     | `int`  | `5672`                                   | 所有服务        | RabbitMQ 端口     |
| `PIPELINE_RABBITMQ_USERNAME` | `str`  | `pipeline`                               | 所有服务        | RabbitMQ 用户     |
| `PIPELINE_RABBITMQ_PASSWORD` | `str`  | `pipeline123`                            | 所有服务        | RabbitMQ 密码     |
| `PIPELINE_MINIO_ENDPOINT`    | `str`  | `http://localhost:9000`                  | gateway, image  | MinIO 地址        |
| `PIPELINE_MINIO_ACCESS_KEY`  | `str`  | `pipeline`                               | gateway, image  | MinIO 访问密钥    |
| `PIPELINE_MINIO_SECRET_KEY`  | `str`  | `pipeline123`                            | gateway, image  | MinIO 秘密密钥    |
| `PIPELINE_GATEWAY_URL`       | `str`  | `http://ai-gateway:8001`                 | 非 gateway 服务 | AI Gateway 地址   |
| `PIPELINE_OPENAI_API_KEY`    | `str`  | `""`                                     | gateway         | OpenAI API Key    |
| `PIPELINE_ANTHROPIC_API_KEY` | `str`  | `""`                                     | gateway         | Anthropic API Key |
| `PIPELINE_DEEPSEEK_API_KEY`  | `str`  | `""`                                     | gateway         | DeepSeek API Key  |
| `PIPELINE_SENSENOVA_API_KEY` | `str`  | `""`                                     | gateway         | SenseNova API Key |
| `PIPELINE_KELING_API_KEY`    | `str`  | `""`                                     | gateway         | 可灵 AI 视频生成 API Key |
| `PIPELINE_DOUBAO_API_KEY`    | `str`  | `""`                                     | gateway         | 豆包 TTS API Key |
| `PIPELINE_CALLBACK_TOKEN`    | `str`  | `pipeline-callback-token-change-in-prod` | 所有服务        | 回调认证令牌      |

### 7.2 Java 后端

| 配置项（application.yml）      | 环境变量              | 默认值                                     | 说明          |
| ------------------------------ | --------------------- | ------------------------------------------ | ------------- |
| `spring.datasource.password` | `DB_PASSWORD`       | `pipeline123`                            | 数据库密码    |
| `spring.rabbitmq.password`   | —                    | `pipeline123`                            | RabbitMQ 密码 |
| `jwt.secret`                 | `JWT_SECRET`        | `pipeline-secret-key-change-in-prod`     | JWT 密钥      |
| `pipeline.callback-base-url` | `CALLBACK_BASE_URL` | `http://host.docker.internal:8080`       | 回调地址      |
| `pipeline.callback-token`    | `CALLBACK_TOKEN`    | `pipeline-callback-token-change-in-prod` | 回调令牌      |
| `minio.secret-key`           | —                    | `pipeline123`                            | MinIO 密钥    |

### 7.3 基础设施（根目录 .env）

| 环境变量              | 默认值                                     | 使用方                   | 说明          |
| --------------------- | ------------------------------------------ | ------------------------ | ------------- |
| `DB_PASSWORD`       | `pipeline123`                            | postgres, pipeline-admin | 数据库密码    |
| `MINIO_USER`        | `pipeline`                               | minio, pipeline-admin    | MinIO 用户    |
| `MINIO_PASSWORD`    | `pipeline123`                            | minio, pipeline-admin    | MinIO 密码    |
| `RABBITMQ_USER`     | `pipeline`                               | rabbitmq                 | RabbitMQ 用户 |
| `RABBITMQ_PASSWORD` | `pipeline123`                            | rabbitmq                 | RabbitMQ 密码 |
| `JWT_SECRET`        | `pipeline-secret-key-change-in-prod`     | pipeline-admin           | JWT 签名密钥  |
| `CALLBACK_TOKEN`    | `pipeline-callback-token-change-in-prod` | 所有服务                 | 回调认证令牌  |
| `CALLBACK_BASE_URL` | `http://pipeline-admin:8080`             | pipeline-admin           | Java 回调地址 |

---

## 8. 常见问题

### Q: 为什么每个服务有自己的 `.env` 而不是一个全局的？

**答：** 服务独立部署时可以只携带自己的配置，不依赖其他服务的配置项。例如 Script Service 不需要关心 AI API Key，它的 `.env` 里只有 RabbitMQ 和 Gateway 地址。

### Q: 本地开发时，配置没生效怎么办？

**答：** 确保你在服务自己的目录下运行：

```bash
# ✅ 正确
cd ai-services/gateway && python main.py

# ❌ 错误（读不到 /gateway/.env）
cd ai-services && python gateway/main.py
```

### Q: Docker 部署时，为什么还要 `environment` 覆盖 `env_file`？

**答：** 本地开发时各服务通过 `localhost:5672` 连接 RabbitMQ；Docker 容器内需要通过服务名 `rabbitmq:5672` 访问。`environment` 块覆盖 `env_file` 中的值，实现同一份 `.env` 兼容两种环境。

### Q: 生产环境怎么管理密钥？

**答：** 生产环境应：

1. 将 `.env` 文件中的密钥替换为容器编排平台的 Secrets
2. 通过环境变量直接注入（如 `Kubernetes Secret → env`）
3. `.env` 文件仅保留非敏感配置项
4. `AES_ENCRYPTION_KEY` 等加密密钥使用 `openssl rand -hex 16` 重新生成

### Q: 添加新服务时需要做什么？

**答：**

1. 在 `ai-services/<new-service>/` 下创建 `.env`，包含本服务需要的配置项
2. 在 `common/config.py` 中添加新字段（如有必要）
3. 在 `docker-compose.yml` 中添加新 service，配置 `env_file` 和 `environment`
4. 参考本文档更新配置清单
