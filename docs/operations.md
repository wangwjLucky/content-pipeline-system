# 运维部署手册

> 版本：v2.0 | 日期：2026-07-19
>
> 本手册涵盖系统从部署到日常运维的全流程，非技术人员可按步骤操作。

---

## 目录

1. [系统概述](#1-系统概述)
2. [环境准备](#2-环境准备)
3. [Docker 镜像全生命周期管理](#3-docker-镜像全生命周期管理)
4. [服务部署与启动](#4-服务部署与启动)
5. [服务停止与重启](#5-服务停止与重启)
6. [服务验证（健康检查）](#6-服务验证健康检查)
7. [日志查看与管理](#7-日志查看与管理)
8. [日常运维](#8-日常运维)
9. [RabbitMQ 管理](#9-rabbitmq-管理)
10. [数据库管理](#10-数据库管理)
11. [配置管理](#11-配置管理)
12. [问题排查](#12-问题排查)
13. [灾难恢复](#13-灾难恢复)
14. [多节点扩展方案](#14-多节点扩展方案)
15. [附录：本地开发模式](#15-附录本地开发模式)

---

## 1. 系统概述

### 1.1 系统组成

内容生产流水线系统由以下 13 个服务组成：

| 类别 | 服务名 | 端口 | 说明 |
|------|--------|------|------|
| 基础设施 | `postgres` | 5432 | PostgreSQL 16 数据库 |
| 基础设施 | `redis` | 6379 | Redis 7 缓存 |
| 基础设施 | `minio` | 9000 / 9001 | MinIO 对象存储（API / 控制台） |
| 基础设施 | `rabbitmq` | 5672 / 15672 | RabbitMQ 消息队列（AMQP / 管理控制台） |
| Java 后台 | `pipeline-admin` | 8080 | Spring Boot 业务后台 |
| Python AI | `ai-gateway` | 8001 | AI 模型统一路由网关 |
| Python AI | `script-service` | 8002 | 脚本生成服务 |
| Python AI | `prompt-service` | 8003 | 分镜生成服务 |
| Python AI | `video-service` | 8004 | 视频生成服务 |
| Python AI | `voice-service` | 8005 | 配音生成服务 |
| Python AI | `ffmpeg-service` | 8006 | 剪辑合成服务 |
| Python AI | `image-service` | 8007 | 图片生成服务 |
| 前端 | `frontend` | 80 | Vue 3 Web 管理界面 |

### 1.2 服务依赖关系

```
postgres ──→ pipeline-admin ──→ frontend
redis    ──→ pipeline-admin
minio    ──→ pipeline-admin, image-service
rabbitmq ──→ 所有 Python 服务, pipeline-admin
              └── ai-gateway ──→ script-service, prompt-service, video-service, ...
```

---

## 2. 环境准备

### 2.1 硬件要求

| 环境 | CPU | 内存 | 磁盘 | 说明 |
|------|-----|------|------|------|
| 开发/测试 | 2 核 | 8 GB | 50 GB | 单机部署 |
| 生产（最小） | 4 核 | 16 GB | 200 GB | 单机部署 |
| 生产（推荐） | 8 核 | 32 GB | 500 GB | 建议 PostgreSQL 单独部署 |

### 2.2 软件要求

| 软件 | 版本要求 | 验证命令 |
|------|---------|---------|
| Docker | >= 24.0 | `docker --version` |
| Docker Compose | >= 2.20 | `docker compose version` |
| Git | >= 2.30 | `git --version` |
| 操作系统 | Linux / Windows / macOS | — |

### 2.3 获取项目代码

```bash
# 方式一：从 Git 仓库克隆
git clone <仓库地址> content-pipeline-system
cd content-pipeline-system

# 方式二：直接使用已有代码目录
cd D:\workspace\content-pipeline-system
```

### 2.4 项目目录结构

```
content-pipeline-system/
├── ai-services/              # Python AI 服务源码
│   ├── common/               #   共享模块
│   ├── gateway/              #   AI Gateway
│   ├── script-service/       #   脚本生成服务
│   ├── prompt-service/       #   分镜生成服务
│   ├── video-service/        #   视频生成服务
│   ├── voice-service/        #   配音生成服务
│   ├── image-service/        #   图片生成服务
│   └── ffmpeg-service/       #   剪辑合成服务
├── pipeline-manager/         # Java Spring Boot 后台源码
├── frontend/                 # Vue 3 前端源码
├── docker-compose.yml        # Docker Compose 编排文件（核心！）
├── .env.example              # 环境变量模板
└── init.sql                  # 数据库初始化脚本
```

### 2.5 配置环境变量

首次部署时需要创建并编辑 `.env` 文件：

```bash
# 1. 从模板创建 .env 文件
cp .env.example .env

# 2. 编辑 .env 文件（至少修改以下值）
#    - DB_PASSWORD：数据库密码
#    - JWT_SECRET：JWT 签名密钥
#    - CALLBACK_TOKEN：回调认证令牌
#    - 至少配置一个 AI API Key
```

**生产环境密钥生成命令**：

```bash
# 生成强随机密钥（分别执行，每个命令输出一个密钥串）
openssl rand -hex 32    # 用于 JWT_SECRET
openssl rand -hex 32    # 用于 CALLBACK_TOKEN
openssl rand -hex 16    # 用于 AES_ENCRYPTION_KEY
```

**.env 文件关键配置项**：

| 变量 | 必须修改？ | 说明 |
|------|-----------|------|
| `DB_PASSWORD` | ✅ 生产环境必须 | PostgreSQL 数据库密码 |
| `JWT_SECRET` | ✅ 生产环境必须 | JWT 签名密钥，用于用户登录认证 |
| `CALLBACK_TOKEN` | ✅ 生产环境必须 | Python → Java 回调认证令牌 |
| `AES_ENCRYPTION_KEY` | ✅ 生产环境必须 | API Key 加密存储密钥 |
| `PIPELINE_DEEPSEEK_API_KEY` | ⚠️ 按需 | DeepSeek 模型 API Key（至少配一个 AI Key） |
| `PIPELINE_OPENAI_API_KEY` | ⚠️ 按需 | OpenAI API Key |
| `PIPELINE_ANTHROPIC_API_KEY` | ⚠️ 按需 | Anthropic Claude API Key |
| `PIPELINE_KELING_API_KEY` | ⚠️ 按需 | 可灵 AI 视频生成 API Key |
| `PIPELINE_DOUBAO_API_KEY` | ⚠️ 按需 | 豆包 TTS 配音 API Key |
| `PIPELINE_VEO_API_KEY` | ⚠️ 按需 | Google Veo 视频生成 API Key |
| `CALLBACK_BASE_URL` | ⚠️ 按需 | Python 回调 Java 的地址，默认 `http://pipeline-admin:8080` |

---

## 3. Docker 镜像全生命周期管理

### 3.1 查看镜像列表

```bash
# 查看所有 Docker 镜像
docker images

# 查看本项目的所有镜像
docker images | grep -E "pipeline|content-pipeline|ai-"
```

### 3.2 构建镜像

```bash
# 方式一：构建所有镜像（最常用）
docker compose build

# 方式二：构建所有镜像（无缓存，确保最新）
docker compose build --no-cache

# 方式三：构建单个服务镜像
docker compose build ai-gateway
docker compose build pipeline-admin
docker compose build frontend
```

**各服务的 Dockerfile 位置**：

| 服务 | Dockerfile 路径 | 基础镜像 |
|------|----------------|---------|
| `pipeline-admin` | `pipeline-manager/Dockerfile` | `eclipse-temurin:17-jre-alpine`（多阶段构建） |
| `ai-gateway` | `ai-services/gateway/Dockerfile` | `python:3.12-slim` |
| `script-service` | `ai-services/script-service/Dockerfile` | `python:3.12-slim` |
| `prompt-service` | `ai-services/prompt-service/Dockerfile` | `python:3.12-slim` |
| `video-service` | `ai-services/video-service/Dockerfile` | `python:3.12-slim` |
| `voice-service` | `ai-services/voice-service/Dockerfile` | `python:3.12-slim` |
| `ffmpeg-service` | `ai-services/ffmpeg-service/Dockerfile` | `python:3.12-slim`（含 FFmpeg 系统包） |
| `image-service` | `ai-services/image-service/Dockerfile` | `python:3.12-slim` |
| `frontend` | `frontend/Dockerfile` | `nginx:alpine`（多阶段构建） |

### 3.3 标记镜像（Tag）

构建完成后，可以给镜像打标签以便推送到仓库：

```bash
# 语法：docker tag <原镜像名> <仓库地址>/<项目名>/<服务名>:<版本>

# 示例：推送到私有仓库
docker tag pipeline-admin:latest registry.example.com/content-pipeline/pipeline-admin:v1.0
docker tag ai-gateway:latest registry.example.com/content-pipeline/ai-gateway:v1.0
docker tag frontend:latest registry.example.com/content-pipeline/frontend:v1.0

# 查看所有镜像的完整标签
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}"
```

### 3.4 推送镜像到仓库

```bash
# 1. 登录镜像仓库
docker login registry.example.com
# 输入用户名和密码

# 2. 推送单个镜像
docker push registry.example.com/content-pipeline/pipeline-admin:v1.0

# 3. 推送所有镜像
docker compose push
# ⚠️ 注意：docker compose push 推送的是 docker-compose.yml 中定义的镜像名
```

### 3.5 拉取镜像

```bash
# 方式一：拉取所有镜像（从已有仓库）
docker compose pull

# 方式二：拉取单个镜像
docker compose pull ai-gateway

# 方式三：直接从仓库拉取（不依赖 compose）
docker pull registry.example.com/content-pipeline/pipeline-admin:v1.0
```

### 3.6 删除镜像

```bash
# 删除单个镜像
docker rmi ai-gateway:latest

# 删除所有未被使用的镜像（谨慎操作）
docker image prune -a

# 强制删除（即使有容器在使用）
docker rmi -f ai-gateway:latest
```

### 3.7 镜像构建流程总结

```
开发机                      镜像仓库                    生产服务器
───────                    ────────                   ──────────
git pull (获取最新代码)
docker compose build       ── push ──→ registry       docker compose pull
(构建镜像)                            (推送镜像)       (拉取镜像)
                                                      docker compose up -d
                                                      (启动服务)
```

---

## 4. 服务部署与启动

### 4.1 快速启动（所有服务一键启动）

> **一键部署脚本**：项目根目录提供了自动化部署脚本：
> - Linux/Mac：`bash deploy.sh`
> - Windows PowerShell：`.\deploy.ps1`
>
> 脚本会自动完成构建镜像、启动服务、等待就绪、健康检查的全流程。

```bash
# 手动一键启动方式：
# 2. 构建并启动所有服务
docker compose up -d

# 3. 检查所有服务是否正常运行
docker compose ps -a
```

**预期输出**（所有服务状态应为 `Up`）：

```
NAME                IMAGE                        STATUS         PORTS
postgres            postgres:16                  Up             0.0.0.0:5432->5432/tcp
redis               redis:7-alpine               Up             0.0.0.0:6379->6379/tcp
minio               minio/minio                  Up             0.0.0.0:9000-9001->9000-9001/tcp
rabbitmq            rabbitmq:3-management        Up             0.0.0.0:5672->5672, 15672->15672/tcp
pipeline-admin      pipeline-admin:latest        Up             0.0.0.0:8080->8080/tcp
ai-gateway          ai-gateway:latest             Up             0.0.0.0:8001->8001/tcp
script-service      script-service:latest        Up             0.0.0.0:8002->8002/tcp
prompt-service      prompt-service:latest        Up             0.0.0.0:8003->8003/tcp
video-service       video-service:latest         Up             0.0.0.0:8004->8004/tcp
voice-service       voice-service:latest         Up             0.0.0.0:8005->8005/tcp
ffmpeg-service      ffmpeg-service:latest        Up             0.0.0.0:8006->8006/tcp
image-service       image-service:latest         Up             0.0.0.0:8007->8007/tcp
frontend            frontend:latest              Up             0.0.0.0:80->80/tcp
```

### 4.2 分步启动（推荐首次使用）

按顺序启动，每个步骤验证成功后进入下一步：

#### 步骤 1：启动基础设施

```bash
# 启动数据库、缓存、存储、消息队列
docker compose up -d postgres redis minio rabbitmq

# 等待 10 秒确保全部就绪
sleep 10

# 验证基础设施
docker compose ps
# 输出应显示 4 个服务都是 "Up" 状态
```

#### 步骤 2：启动 Java 后台

```bash
# 构建 Java 镜像
docker compose build pipeline-admin

# 启动 Java 后台
docker compose up -d pipeline-admin

# 等待 15 秒等待 Spring Boot 完成初始化
sleep 15

# 验证 Java 后端
curl http://localhost:8080/api/v1/health
# 预期响应：{"code":200,"message":"success","data":"OK"}
```

#### 步骤 3：启动 AI Gateway

```bash
# 构建 AI Gateway 镜像
docker compose build ai-gateway

# 启动 AI Gateway
docker compose up -d ai-gateway

# 验证 AI Gateway
curl http://localhost:8001/health
# 预期响应：{"status": "ok"}
```

#### 步骤 4：启动其他 Python 服务

```bash
# 构建所有 Python 服务镜像
docker compose build script-service prompt-service
docker compose build video-service voice-service
docker compose build ffmpeg-service image-service

# 启动所有 Python 服务
docker compose up -d script-service prompt-service
docker compose up -d video-service voice-service
docker compose up -d ffmpeg-service image-service

# 等待 5 秒
sleep 5
```

#### 步骤 5：启动前端

```bash
# 构建前端镜像
docker compose build frontend

# 启动前端
docker compose up -d frontend

# 验证前端
curl -I http://localhost
# 预期响应：HTTP/1.1 200 OK
```

#### 步骤 6：首次登录

1. 打开浏览器访问 `http://localhost`
2. 使用默认管理员账号登录（系统首次启动时由 `DataInitializer.java` 自动创建）：
   - **用户名**：`admin`
   - **密码**：`admin123`
3. 登录后请立即修改默认密码
4. 进入 **AI 模型配置** 页面配置 API Key
5. 进入 **平台账号** 页面添加社交媒体账号

### 4.3 启动顺序说明

所有服务配置了 `restart: unless-stopped`，容器异常退出后会自动重启。`docker-compose.yml` 中的 `depends_on` 确保启动顺序正确：

```
步骤 1: postgres → redis → minio → rabbitmq
步骤 2:                      pipeline-admin
步骤 3:                           ai-gateway
步骤 4: script-service → prompt-service → video-service → voice-service → ffmpeg-service → image-service
步骤 5:                           frontend
```

---

## 5. 服务停止与重启

### 5.1 停止所有服务

```bash
# 停止所有服务（不删除容器）
docker compose stop

# 停止所有服务并删除容器（保留数据卷）
docker compose down

# 停止所有服务并删除容器和数据卷（会丢失所有数据！）
docker compose down -v
```

### 5.2 停止单个服务

```bash
# 停止单个服务
docker compose stop script-service

# 停止并删除单个服务容器
docker compose rm -s script-service
```

### 5.3 重启单个服务

```bash
# 重启服务（不重建镜像）
docker compose restart script-service

# 重建镜像并重启（代码有变更时使用）
docker compose build script-service
docker compose up -d script-service

# 强制重新创建容器
docker compose up -d --force-recreate script-service
```

### 5.4 重启所有服务

```bash
# 优雅重启
docker compose down
docker compose up -d

# 完全重建（清除构建缓存）
docker compose build --no-cache
docker compose up -d

# 不停机重启单个服务（不影响其他服务）
docker compose restart ai-gateway
```

### 5.5 查看服务状态

```bash
# 查看所有服务运行状态
docker compose ps

# 查看所有服务（包括已停止的）
docker compose ps -a

# 查看资源使用情况
docker stats
```

---

## 6. 服务验证（健康检查）

### 6.1 各服务健康检查端点

| 服务 | 端口 | 健康检查地址 | 预期响应 |
|------|------|-------------|---------|
| Java 后台 | 8080 | `http://localhost:8080/api/v1/health` | `{"code":200,"message":"success","data":"OK"}` |
| API 文档 | 8080 | `http://localhost:8080/doc.html` | Knife4j Swagger UI（OpenAPI 3） |
| AI Gateway | 8001 | `http://localhost:8001/health` | `{"status": "ok"}` |
| Script Service | 8002 | `http://localhost:8002/health` | `{"status":"UP","service":"script-service"}` |
| Prompt Service | 8003 | `http://localhost:8003/health` | `{"status":"UP","service":"prompt-service"}` |
| Video Service | 8004 | `http://localhost:8004/health` | `{"status":"UP","service":"video-service"}` |
| Voice Service | 8005 | `http://localhost:8005/health` | `{"status":"UP","service":"voice-service"}` |
| FFmpeg Service | 8006 | `http://localhost:8006/health` | `{"status":"UP","service":"ffmpeg-service"}` |
| Image Service | 8007 | `http://localhost:8007/health` | `{"status":"UP","service":"image-service"}` |
| 前端 | 80 | `http://localhost/` | HTTP 200 |
| PostgreSQL | 5432 | `pg_isready -U pipeline` | `accepting connections` |
| MinIO | 9000 | `http://localhost:9000/minio/health/live` | HTTP 200 |
| RabbitMQ | 15672 | `http://localhost:15672/api/health/checks/alarms` | HTTP 200 |

### 6.2 一键健康检查脚本

将以下内容保存为 `health-check.sh`，部署后运行：

```bash
#!/bin/bash
# 内容生产流水线系统 — 一键健康检查脚本

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║        内容生产流水线系统 — 服务健康检查                      ║"
echo "╚══════════════════════════════════════════════════════════════╝"

echo ""
echo "▶ 步骤 1/4：检查容器运行状态"
echo "────────────────────────────────────────────────────────────────"
docker compose ps -a
echo ""

echo "▶ 步骤 2/4：检查 Java 后台"
echo "────────────────────────────────────────────────────────────────"
JAVA_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/health 2>/dev/null || echo "000")
if [ "$JAVA_CHECK" = "200" ]; then
    echo "  ✅ Java 后台 (:8080) — HTTP $JAVA_CHECK"
else
    echo "  ❌ Java 后台 (:8080) — HTTP $JAVA_CHECK"
fi
echo ""

echo "▶ 步骤 3/4：检查 AI 服务"
echo "────────────────────────────────────────────────────────────────"
for port in 8001 8002 8003 8004 8005 8006 8007; do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/health 2>/dev/null || echo "000")
    NAME=$(case $port in
        8001) echo "AI Gateway      " ;;
        8002) echo "Script Service  " ;;
        8003) echo "Prompt Service  " ;;
        8004) echo "Video Service   " ;;
        8005) echo "Voice Service   " ;;
        8006) echo "FFmpeg Service  " ;;
        8007) echo "Image Service   " ;;
    esac)
    if [ "$STATUS" = "200" ]; then
        echo "  ✅ $NAME(:$port) — HTTP $STATUS"
    else
        echo "  ❌ $NAME(:$port) — HTTP $STATUS"
    fi
done
echo ""

echo "▶ 步骤 4/4：检查基础设施"
echo "────────────────────────────────────────────────────────────────"
# PostgreSQL
PG_CHECK=$(docker compose exec postgres pg_isready -U pipeline 2>/dev/null || echo "down")
if echo "$PG_CHECK" | grep -q "accepting"; then
    echo "  ✅ PostgreSQL (:5432) — $PG_CHECK"
else
    echo "  ❌ PostgreSQL (:5432) — $PG_CHECK"
fi

# MinIO
MINIO_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9000/minio/health/live 2>/dev/null || echo "000")
if [ "$MINIO_CHECK" = "200" ]; then
    echo "  ✅ MinIO (:9000) — HTTP $MINIO_CHECK"
else
    echo "  ❌ MinIO (:9000) — HTTP $MINIO_CHECK"
fi

# RabbitMQ
MQ_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:15672 2>/dev/null || echo "000")
if [ "$MQ_CHECK" = "200" ]; then
    echo "  ✅ RabbitMQ (:15672) — HTTP $MQ_CHECK"
else
    echo "  ❌ RabbitMQ (:15672) — HTTP $MQ_CHECK"
fi

# 前端
FE_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/ 2>/dev/null || echo "000")
if [ "$FE_CHECK" = "200" ]; then
    echo "  ✅ 前端 (:80) — HTTP $FE_CHECK"
else
    echo "  ❌ 前端 (:80) — HTTP $FE_CHECK"
fi

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  检查完成！所有服务正常 ✅  发现问题 → 参考第 12 节问题排查  ║"
echo "╚══════════════════════════════════════════════════════════════╝"
```

---

## 7. 日志查看与管理

### 7.1 日志目录结构

Python 服务日志输出到各服务目录下的 `logs/` 文件夹：

```
ai-services/
├── gateway/
│   └── logs/                        # AI Gateway 日志
│       ├── debug.log                # DEBUG 级别日志
│       ├── info.log                 # INFO 级别日志
│       ├── warn.log                 # WARNING 级别日志
│       └── error.log                # ERROR 级别日志
├── script-service/
│   └── logs/                        # 脚本生成服务日志（同上结构）
├── prompt-service/
│   └── logs/                        # 分镜生成服务日志
├── video-service/
│   └── logs/                        # 视频生成服务日志
├── voice-service/
│   └── logs/                        # 配音服务日志
├── ffmpeg-service/
│   └── logs/                        # 剪辑合成服务日志
└── image-service/
    └── logs/                        # 图片生成服务日志
```

Docker 部署时，日志通过 volume 映射到宿主机，可直接在宿主机查看。

### 7.2 日志文件格式

```
[2026-07-19 10:30:00][INFO][MainThread][gateway.health:45] - [健康检查通过]
```

| 字段 | 说明 |
|------|------|
| `2026-07-19 10:30:00` | 时间戳 |
| `INFO` | 日志级别（DEBUG / INFO / WARNING / ERROR） |
| `MainThread` | 线程名称 |
| `gateway.health:45` | 模块名.方法名:行号 |
| `健康检查通过` | 日志消息内容 |

### 7.3 日志滚动策略

| 项目 | 配置 |
|------|------|
| 滚动方式 | `RotatingFileHandler` |
| 单文件最大 | 30 MB |
| 备份数量 | 3 个（保留 `.log.1`、`.log.2`、`.log.3`） |
| 分级别 | 是（debug / info / warn / error 各写独立文件） |

### 7.4 查看日志

```bash
# ─── Docker 日志 ───

# 查看所有服务的日志（最近 100 行）
docker compose logs --tail=100

# 实时跟踪某个服务的日志（按 Ctrl+C 退出）
docker compose logs -f script-service

# 查看某个服务的日志（最近 50 行）
docker compose logs --tail=50 pipeline-admin

# 搜索日志中的关键字
docker compose logs script-service 2>&1 | grep "ERROR"

# ─── 宿主机日志文件 ───

# 查看某个服务的 INFO 日志
tail -f ai-services/script-service/logs/info.log

# 查看错误日志
tail -50 ai-services/script-service/logs/error.log

# 搜索日志关键字
grep "taskId=123" ai-services/script-service/logs/info.log

# 统计各服务错误数量
for svc in gateway script-service prompt-service video-service voice-service ffmpeg-service image-service; do
    echo "$svc: $(grep -c 'ERROR' ai-services/$svc/logs/error.log 2>/dev/null || echo 0)"
done
```

### 7.5 Java 日志

Java 后端使用 Spring Boot 默认日志（Logback），输出到控制台：

```bash
# 查看 Java 后台日志
docker compose logs pipeline-admin --tail=100

# 实时跟踪 Java 日志
docker compose logs -f pipeline-admin

# 搜索回调相关日志
docker compose logs pipeline-admin 2>&1 | grep -i callback
```

---

## 8. 日常运维

### 8.1 数据库备份

```bash
# 备份到文件
docker compose exec -T postgres pg_dump -U pipeline pipeline > backup_$(date +%Y%m%d).sql

# 压缩备份（推荐）
docker compose exec -T postgres pg_dump -U pipeline pipeline | gzip > backup_$(date +%Y%m%d).sql.gz

# 恢复数据库
gunzip -c backup_20260719.sql.gz | docker compose exec -T postgres psql -U pipeline pipeline
```

### 8.2 MinIO 数据备份

```bash
# 备份视频素材
docker compose exec minio mc mirror local/pipeline-videos-final /backup/videos-final

# 备份配音文件
docker compose exec minio mc mirror local/pipeline-voices /backup/voices

# 备份封面
docker compose exec minio mc mirror local/pipeline-covers /backup/covers
```

### 8.3 数据重置

> ⚠️ **警告：以下操作会丢失所有数据！请先备份。**

```bash
# 全量重置（删除所有数据卷并重建）
docker compose down -v
docker compose up -d

# 仅重置数据库
docker compose stop postgres
docker compose rm postgres
docker compose up -d postgres
# init.sql 会自动执行，重新创建所有表
```

### 8.4 磁盘清理

```bash
# 查看磁盘使用情况
docker system df

# 清理未使用的容器、网络、镜像（无依赖的 dangling 镜像）
docker system prune -f

# 清理所有未使用的镜像（包括无标签的）
docker image prune -a -f

# 清理临时文件（MinIO 中 24 小时前的临时文件）
docker compose exec minio mc rm --recursive --older-than 1d local/pipeline-temp/
```

### 8.5 资源监控

```bash
# 查看所有容器资源使用
docker stats

# 查看特定服务资源
docker stats pipeline-admin ai-gateway

# 查看容器日志大小
docker ps --size

# 查看磁盘空间
df -h
```

### 8.6 定时发布任务

系统内置定时任务（`SchedulingConfig.java`），每分钟检查一次是否有到期的定时发布：

```java
@Scheduled(fixedRate = 60000)  // 每 60 秒执行一次
public void processScheduledPublishes() {
    // 查询 PENDING 状态且 scheduled_at 已到期的发布记录
    // 逐条发布，推进任务状态到 PUBLISHED
}
```

**查看待发布任务**：

```sql
SELECT id, task_id, platform, title, scheduled_at
FROM publish_log
WHERE status = 'PENDING' AND scheduled_at IS NOT NULL
ORDER BY scheduled_at;
```

### 8.7 查看 API 文档

系统内置 Knife4j (Swagger / OpenAPI 3) 接口文档：

```bash
# 启动服务后访问
# http://localhost:8080/doc.html
# 或
# http://localhost:8080/swagger-ui/index.html
```

API 文档自动生成，包含所有 Controller 的接口定义、请求参数、响应格式。

---

## 9. RabbitMQ 管理

### 9.1 管理控制台

访问地址：`http://localhost:15672`
默认凭据：`pipeline` / `pipeline123`（由 `.env` 中的 `RABBITMQ_USER` / `RABBITMQ_PASSWORD` 配置）

**关键操作**：
1. **Queues** 标签页 — 查看队列深度和消费者数量
2. **Exchanges** 标签页 — 查看消息路由
3. **Admin** 标签页 — 管理用户和权限

### 9.2 查看队列状态

```bash
# 查看所有队列状态（消息数量、消费者数量）
curl -s -u pipeline:pipeline123 http://localhost:15672/api/queues | \
  python -c "import sys,json; [print(f'{q[\"name\"]}: ready={q[\"messages_ready\"]}, unacked={q[\"messages_unacknowledged\"]}, consumers={q[\"consumers\"]}') for q in json.load(sys.stdin)]"

# 查看死信队列
curl -s -u pipeline:pipeline123 http://localhost:15672/api/queues/%2F/pipeline.dlq.task | \
  python -c "import sys,json; q=json.load(sys.stdin); print(f'死信队列: {q[\"messages_ready\"]} 条消息')"
```

### 9.3 手动发送测试消息

```bash
# 向脚本生成队列发送测试消息
curl -u pipeline:pipeline123 \
  -H "Content-Type: application/json" \
  -X POST http://localhost:15672/api/exchanges/%2F/pipeline.script.generate/publish \
  -d '{
    "properties": {"content_type": "application/json", "delivery_mode": 2},
    "routing_key": "pipeline.script.generate",
    "payload": "{\"messageId\":\"manual-test\",\"taskId\":1,\"action\":\"generate_script\",\"params\":{\"topic\":\"测试选题\"}}",
    "payload_encoding": "string"
  }'
```

### 9.4 队列管理

| 操作 | 命令 |
|------|------|
| 清除队列消息 | `curl -u pipeline:pipeline123 -X DELETE http://localhost:15672/api/queues/%2F/pipeline.script.generate/contents` |
| 查看队列消费者 | `curl -s -u pipeline:pipeline123 http://localhost:15672/api/queues \| python -c "import sys,json; [print(f'{q[\"name\"]}: {q[\"consumers\"]} consumers') for q in json.load(sys.stdin)]"` |
| 查看死信队列消息 | `curl -s -u pipeline:pipeline123 http://localhost:15672/api/queues/%2F/pipeline.dlq.task/get` |

### 9.5 队列配置

所有业务队列在 `RabbitConfig.java` 中声明，配置了死信交换机：

| 队列 | 持久化 | 死信交换机 | 死信路由键 |
|------|--------|-----------|-----------|
| `pipeline.script.generate` | 是 | `pipeline.dlx` | `dlq.task` |
| `pipeline.prompt.generate` | 是 | `pipeline.dlx` | `dlq.task` |
| `pipeline.video.generate` | 是 | `pipeline.dlx` | `dlq.task` |
| `pipeline.image.generate` | 是 | `pipeline.dlx` | `dlq.task` |
| `pipeline.voice.generate` | 是 | `pipeline.dlx` | `dlq.task` |
| `pipeline.ffmpeg.compile` | 是 | `pipeline.dlx` | `dlq.task` |
| `pipeline.dlq.task` | 是 | — | — |

---

## 10. 数据库管理

### 10.1 连接数据库

```bash
# 通过容器连接
docker compose exec postgres psql -U pipeline -d pipeline

# 通过宿主机连接（需安装 psql 客户端）
psql -h localhost -U pipeline -d pipeline
```

### 10.2 常用查询

```sql
-- 查看任务状态分布
SELECT status, COUNT(*) FROM task GROUP BY status ORDER BY status;

-- 查看卡住的任务（超过 1 小时没有更新的）
SELECT id, title, status, progress, created_at, updated_at
FROM task
WHERE status NOT IN ('PUBLISHED', 'CANCELLED')
  AND updated_at < NOW() - INTERVAL '1 hour'
ORDER BY updated_at;

-- 查看最近 20 条任务事件
SELECT * FROM task_event ORDER BY created_at DESC LIMIT 20;

-- 查看回调查看
SELECT * FROM task_event WHERE comment LIKE '%callback%' ORDER BY created_at DESC LIMIT 20;

-- 查看错误任务
SELECT id, title, status, error_message, updated_at
FROM task
WHERE status = 'ERROR'
ORDER BY updated_at DESC;

-- 查看今日发布数量
SELECT COUNT(*) FROM publish_log WHERE DATE(published_at) = CURRENT_DATE;

-- 查看最热门的选题
SELECT t.title, COUNT(*) as task_count
FROM topic t
JOIN task ta ON t.id = ta.topic_id
GROUP BY t.id, t.title
ORDER BY task_count DESC
LIMIT 10;
```

### 10.3 数据库备份与恢复

```bash
# 备份单个表
docker compose exec -T postgres pg_dump -U pipeline -t task pipeline > backup_task.sql

# 只备份数据（不备份表结构）
docker compose exec -T postgres pg_dump -U pipeline --data-only pipeline > backup_data.sql

# 恢复（从 SQL 文件）
cat backup.sql | docker compose exec -T postgres psql -U pipeline pipeline
```

---

## 11. 配置管理

### 11.1 配置来源总览

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

配置优先级（高 → 低）：
1. **容器环境变量**（`docker-compose.yml` 的 `environment:` 块）
2. **Service `.env` 文件**（`docker-compose.yml` 的 `env_file:` 指令）
3. **代码内默认值**（Pydantic `field(default=...)` 或 Java `:default` 语法）

### 11.2 验证配置

```bash
# 检查所有 Python 服务的回调令牌是否一致
for svc in gateway script-service prompt-service video-service voice-service ffmpeg-service image-service; do
    echo "$svc: $(docker compose exec $svc env | grep PIPELINE_CALLBACK_TOKEN)"
done

# 验证各服务 MQ 连接地址
for svc in gateway script-service prompt-service video-service; do
    echo "$svc: $(docker compose exec $svc env | grep PIPELINE_RABBITMQ_HOST)"
done

# 查看 Java 后台的配置
docker compose exec pipeline-admin env | grep -E "DB_PASSWORD|CALLBACK_TOKEN|JWT_SECRET"
```

### 11.3 环境变量速查表

| 变量 | 默认值 | 说明 | 生产环境必须修改？ |
|------|--------|------|------------------|
| `DB_PASSWORD` | `pipeline123` | PostgreSQL 密码 | ✅ |
| `JWT_SECRET` | `pipeline-secret-key-change-in-prod` | JWT 签名密钥 | ✅ |
| `CALLBACK_TOKEN` | `pipeline-callback-token-change-in-prod` | 回调认证令牌 | ✅ |
| `AES_ENCRYPTION_KEY` | `pipeline-default-key-change-in-prod!` | 加密密钥（API Key 加密存储） | ✅ |
| `MINIO_PASSWORD` | `pipeline123` | MinIO 密码 | ✅ |
| `RABBITMQ_PASSWORD` | `pipeline123` | RabbitMQ 密码 | ✅ |
| `PIPELINE_DEEPSEEK_API_KEY` | `""` | DeepSeek API Key | ⚠️ |
| `PIPELINE_OPENAI_API_KEY` | `""` | OpenAI API Key | ⚠️ |
| `PIPELINE_ANTHROPIC_API_KEY` | `""` | Anthropic Claude API Key | ⚠️ |
| `PIPELINE_KELING_API_KEY` | `""` | 可灵 AI 视频生成 API Key | ⚠️ |
| `PIPELINE_DOUBAO_API_KEY` | `""` | 豆包 TTS API Key | ⚠️ |
| `PIPELINE_VEO_API_KEY` | `""` | Google Veo 视频生成 API Key | ⚠️ |
| `PIPELINE_DEBUG` | `false` | 调试模式 | — |

### 11.4 生产环境安全配置清单

部署到生产环境前，请逐项检查：

- [ ] 使用 `openssl rand -hex 32` 重新生成 `JWT_SECRET`
- [ ] 使用 `openssl rand -hex 32` 重新生成 `CALLBACK_TOKEN`
- [ ] 使用 `openssl rand -hex 16` 重新生成 `AES_ENCRYPTION_KEY`
- [ ] 为 `DB_PASSWORD`、`MINIO_PASSWORD`、`RABBITMQ_PASSWORD` 设置强密码
- [ ] 配置至少一个 AI API Key（DeepSeek / OpenAI / Anthropic）
- [ ] 确保 `CALLBACK_BASE_URL` 正确（Docker 内为 `http://pipeline-admin:8080`）
- [ ] 检查防火墙是否开放必要的端口（80、8080、443 等）
- [ ] 生产环境建议配置 HTTPS 证书

---

## 12. 问题排查

### 12.1 服务启动失败

**症状**：`docker compose up -d` 后服务状态为 `Exit` 或 `Restarting`。

**排查步骤**：

```bash
# 1. 查看错误原因
docker compose logs <服务名> --tail=50

# 2. 检查端口是否被占用
netstat -ano | findstr :8080    # Windows
lsof -i :8080                    # Linux / macOS

# 3. 检查数据库是否就绪
docker compose exec postgres pg_isready -U pipeline

# 4. 检查 RabbitMQ 是否就绪
docker compose exec rabbitmq rabbitmqctl status

# 5. 检查配置是否正确
docker compose config
```

**常见错误及解决**：

| 错误信息 | 可能原因 | 解决方法 |
|---------|---------|---------|
| `port is already allocated` | 端口被占用 | 停止占用端口的进程，或修改 `docker-compose.yml` 中的端口映射 |
| `Connection refused` | 依赖服务未就绪 | 等待依赖服务启动，或检查 `depends_on` 配置 |
| `Cannot connect to RabbitMQ` | RabbitMQ 地址或凭据错误 | 检查 `PIPELINE_RABBITMQ_HOST` 和 `RABBITMQ_PASSWORD` |
| `FATAL: password authentication failed` | 数据库密码错误 | 检查 `.env` 中的 `DB_PASSWORD` |
| `No such file or directory` | 路径错误 | 确保在项目根目录下运行命令 |

### 12.2 任务卡在某个状态不推进

**症状**：任务长时间停留在 `SCRIPTING`、`GENERATING` 等中间状态。

**排查步骤**：

```bash
# 1. 检查 RabbitMQ 队列深度（是否有消息堆积）
curl -s -u pipeline:pipeline123 http://localhost:15672/api/queues | \
  python -c "import sys,json; [print(f'{q[\"name\"]}: {q[\"messages_ready\"]} ready, {q[\"consumers\"]} consumers') for q in json.load(sys.stdin)]"

# 2. 检查对应 Python 服务日志
tail -50 ai-services/script-service/logs/error.log

# 3. 检查服务是否在运行
docker compose ps script-service

# 4. 检查回调是否可达（从容器内部测试）
docker compose exec script-service curl -s http://pipeline-admin:8080/api/v1/health
```

**常见原因**：
- Python 服务异常退出（检查日志中的 Exception）
- 回调 URL 配置错误（`CALLBACK_BASE_URL` 不正确）
- 回调令牌不匹配（`CALLBACK_TOKEN` 不一致）
- RabbitMQ 消费者未启动（检查 `pika` 连接日志）

### 12.3 回调失败

**症状**：Java 日志中出现 `回调令牌无效` 或 `任务不存在` 警告。

```bash
# 查看 Java 回调日志
docker compose logs pipeline-admin --tail=50 | grep -i callback

# 检查回调令牌是否一致
# Java 端：pipeline.callback-token = ${CALLBACK_TOKEN}
# Python 端：PIPELINE_CALLBACK_TOKEN = ${CALLBACK_TOKEN}
# 确保 .env 中 CALLBACK_TOKEN 是同一个值
```

**回调验证流程**：
1. Python 发送请求 → 请求头携带 `X-Callback-Token`
2. Java 收到请求 → 校验 `X-Callback-Token` 是否匹配
3. 不匹配 → 返回 403 `禁止访问：回调令牌无效`
4. 匹配 → 校验 `taskId` 和 `service` 参数非空
5. 校验通过 → 根据 `service` 类型推进任务状态

### 12.4 MinIO 存储问题

```bash
# 检查 MinIO 状态
curl -s http://localhost:9000/minio/health/live

# 列出所有 bucket
docker compose exec minio mc ls local

# 查看磁盘使用
docker compose exec minio du -sh /data

# 清理临时文件（24 小时过期）
docker compose exec minio mc rm --recursive --older-than 1d local/pipeline-temp/
```

### 12.5 模型列表为空

**症状**：`GET /ai/v1/models` 返回 `{"models": [], "total": 0}`，但已配置 API Key。

**排查步骤**：

```bash
# 1. 检查 settings 是否读到了 API Key
cd ai-services
python -c "from common.config import settings; print(f'deepseek={settings.deepseek_api_key!r}')"

# 2. 如果输出 `deepseek=''`，说明 .env 文件路径不对
#    Settings() 读取的是当前目录的 .env 文件
#    需要确保 ai-services/.env 存在，或直接设置环境变量
$env:PIPELINE_DEEPSEEK_API_KEY = "你的key"
python -m gateway.main
```

**常见原因**：
- 本地开发时 API Key 配置在 `gateway/.env` 但 `ai-services/.env` 不存在
- 解决方案：`cp gateway/.env .env` 或手动设置环境变量

### 12.6 日志告警关键字

建议对以下关键字设置日志监控告警：

| 关键字 | 级别 | 说明 |
|--------|------|------|
| `回调令牌无效` | 严重 | 安全配置错误，需立即检查 |
| `乐观锁冲突` | 警告 | 并发更新冲突，需检查任务是否被重复操作 |
| `调用失败` | 警告 | AI API 调用异常，检查 API Key 和网络 |
| `Connection refused` | 严重 | 服务连接失败，检查依赖服务 |
| `模型列表刷新失败` | 警告 | Provider 模型列表拉取异常 |
| `Exception` | 警告 | 代码执行异常，需进一步排查 |

---

## 13. 灾难恢复

### 13.1 服务完全不可用

```bash
# 1. 检查所有服务状态
docker compose ps

# 2. 检查关键基础设施
docker compose logs postgres --tail=20
docker compose logs rabbitmq --tail=20
docker compose logs minio --tail=20

# 3. 重启所有服务
docker compose down
docker compose up -d

# 4. 等待所有服务就绪
sleep 30

# 5. 验证健康状态
curl http://localhost:8080/api/v1/health
curl http://localhost:8001/health
```

### 13.2 数据恢复

```bash
# 1. 从备份恢复数据库
gunzip -c backup_20260719.sql.gz | docker compose exec -T postgres psql -U pipeline pipeline

# 2. 从 MinIO 备份恢复
docker compose exec minio mc mirror /backup/videos-final local/pipeline-videos-final
docker compose exec minio mc mirror /backup/voices local/pipeline-voices
docker compose exec minio mc mirror /backup/covers local/pipeline-covers
```

### 13.3 数据库损坏

```bash
# 1. 停止所有依赖数据库的服务
docker compose stop pipeline-admin

# 2. 从备份恢复
gunzip -c backup_latest.sql.gz | docker compose exec -T postgres psql -U pipeline pipeline

# 3. 重启服务
docker compose start pipeline-admin

# 4. 验证
curl http://localhost:8080/api/v1/health
```

---

## 14. 多节点扩展方案

> 当单机部署无法满足性能需求时，可按以下方案逐步扩展为多节点集群。
>
> **核心原则**：各服务无状态化设计，通过增加实例数水平扩展。

### 14.1 扩展时机

| 指标 | 警戒线 | 建议行动 |
|------|--------|---------|
| CPU 使用率持续 > 80% | 任一服务 | 增加该服务实例数 |
| 内存使用率持续 > 85% | 任一服务 | 增加该服务实例数或升级硬件 |
| RabbitMQ 队列积压 > 1000 | 任一队列 | 增加对应 Python 服务消费者实例 |
| API 响应时间 > 2s | Java 后台 | 增加 pipeline-admin 实例 + Nginx 负载均衡 |
| 磁盘使用率 > 80% | MinIO / PostgreSQL | 清理或扩容存储 |

### 14.2 Python 服务水平扩展（最简单）

Python 服务通过 RabbitMQ 消息驱动，天生支持多实例竞争消费：

```yaml
# docker-compose.yml 扩展示例
# 只需增加同名服务实例，使用不同端口
script-service-2:
  build:
    context: ./ai-services
    dockerfile: script-service/Dockerfile
  env_file: ./ai-services/script-service/.env
  environment:
    PIPELINE_RABBITMQ_HOST: rabbitmq
    PIPELINE_GATEWAY_URL: http://ai-gateway:8001
    PIPELINE_CALLBACK_TOKEN: ${CALLBACK_TOKEN}
  depends_on:
    rabbitmq: { condition: service_started }
    ai-gateway: { condition: service_started }
  restart: unless-stopped
  # 无需暴露端口，只消费 MQ 消息
  # 不设 ports 映射，仅内部通信

script-service-3:
  # 同上，可再增加第三个实例
  ...
```

**工作原理**：
- 多个实例同时消费同一个队列（如 `pipeline.script.generate`）
- RabbitMQ 通过 `basic.qos(prefetch_count=1)` 确保每个消息只被一个消费者处理
- 新增实例无需修改代码，只需在 `docker-compose.yml` 中增加服务定义
- 扩展后处理能力 = 单实例处理能力 × 实例数

**限制**：FFmpeg Service 受 CPU 密集型计算限制，建议单节点不超过 2 个实例。

### 14.3 Java 后台水平扩展

Java 后端是无状态服务（Session 存储在 Redis），可通过多实例 + Nginx 负载均衡扩展：

```nginx
# nginx 负载均衡配置
upstream pipeline-admin {
    least_conn;                          # 最少连接数策略
    server pipeline-admin-1:8080 max_fails=3 fail_timeout=30s;
    server pipeline-admin-2:8080 max_fails=3 fail_timeout=30s;
    server pipeline-admin-3:8080 backup; # 备用实例
}

server {
    listen 80;
    location /api/ {
        proxy_pass http://pipeline-admin;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

```yaml
# docker-compose.yml 扩展
pipeline-admin-1:
  build: ./pipeline-manager
  environment:
    SPRING_PROFILES_ACTIVE: dev
    DB_PASSWORD: ${DB_PASSWORD}
    CALLBACK_TOKEN: ${CALLBACK_TOKEN}
    spring.datasource.url: jdbc:postgresql://postgres:5432/pipeline
    spring.rabbitmq.host: rabbitmq
    spring.data.redis.host: redis
    minio.endpoint: http://minio:9000
  # 不对外暴露端口，通过 Nginx 转发

pipeline-admin-2:
  # 同上，第二个实例
  ...

pipeline-admin-nlb:
  image: nginx:alpine
  volumes:
    - ./nginx-admin.conf:/etc/nginx/conf.d/default.conf
  ports:
    - "8080:80"
  depends_on:
    - pipeline-admin-1
    - pipeline-admin-2
```

**关键前提**：
- 所有实例共享同一个 Redis（存储 Session 和分布式锁）
- 所有实例共享同一个 PostgreSQL（通过连接池）
- 所有实例共享同一个 RabbitMQ（发送 MQ 消息）
- 回调 URL（`CALLBACK_BASE_URL`）指向 Nginx 负载均衡器地址

### 14.4 PostgreSQL 扩展

#### 方案一：主从复制（读写分离）

```yaml
# docker-compose.yml
postgres-primary:
  image: postgres:16
  environment:
    POSTGRES_DB: pipeline
    POSTGRES_USER: pipeline
    POSTGRES_PASSWORD: ${DB_PASSWORD}
  volumes:
    - postgres_primary_data:/var/lib/postgresql/data
  ports:
    - "5432:5432"

postgres-standby:
  image: postgres:16
  environment:
    POSTGRES_DB: pipeline
    POSTGRES_USER: pipeline
    POSTGRES_PASSWORD: ${DB_PASSWORD}
  volumes:
    - postgres_standby_data:/var/lib/postgresql/data
  # 配置流复制
  command: |
    postgres -c wal_level=replica
              -c max_wal_senders=5
              -c hot_standby=on
```

#### 方案二：连接池（PgBouncer，轻量级）

```yaml
pgbouncer:
  image: edoburu/pgbouncer:latest
  environment:
    DB_USER: pipeline
    DB_PASSWORD: ${DB_PASSWORD}
    DB_HOST: postgres
    DB_PORT: "5432"
    DB_NAME: pipeline
    POOL_MODE: transaction
    DEFAULT_POOL_SIZE: 50
  ports:
    - "5432:5432"
```

### 14.5 RabbitMQ 集群

```yaml
# docker-compose.yml（3 节点集群）
rabbitmq-1:
  image: rabbitmq:3-management
  hostname: rabbitmq-1
  environment:
    RABBITMQ_ERLANG_COOKIE: "SECRET_COOKIE_VALUE"
    RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
    RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
  volumes:
    - rabbitmq_data_1:/var/lib/rabbitmq

rabbitmq-2:
  image: rabbitmq:3-management
  hostname: rabbitmq-2
  environment:
    RABBITMQ_ERLANG_COOKIE: "SECRET_COOKIE_VALUE"
  command: >
    sh -c "sleep 10 && rabbitmq-server
    && rabbitmqctl stop_app
    && rabbitmqctl join_cluster rabbit@rabbitmq-1
    && rabbitmqctl start_app"
  depends_on: [rabbitmq-1]

rabbitmq-3:
  # 同上，加入集群
  ...
```

### 14.6 MinIO 分布式模式

MinIO 分布式模式需要 4 个节点起步（Erasure Code 保护）：

```yaml
# docker-compose.yml 分布式 MinIO（4 节点）
minio-1:
  image: minio/minio
  command: server --console-address ":9001" http://minio-{1...4}/data
  environment:
    MINIO_ROOT_USER: ${MINIO_USER}
    MINIO_ROOT_PASSWORD: ${MINIO_PASSWORD}
  volumes:
    - minio_data_1:/data
  ports:
    - "9000:9000"

minio-2:
  # 同上（不同数据卷）
  ...
minio-3:
  ...
minio-4:
  ...
```

### 14.7 全量扩展架构图

```
                          ┌──────────────┐
                          │   Nginx LB   │
                          │   :80/:443   │
                          └──────┬───────┘
                                 │
              ┌──────────────────┼──────────────────┐
              ▼                  ▼                  ▼
      ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
      │ pipeline-admin│  │ pipeline-admin│  │ pipeline-admin│
      │  实例 1       │  │  实例 2       │  │  实例 3       │
      └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
             │                 │                 │
             └─────────────────┼─────────────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
      ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
      │  PostgreSQL  │ │    Redis     │ │   RabbitMQ   │
      │  主从/连接池  │ │   单机/集群  │ │   3 节点集群  │
      └──────────────┘ └──────────────┘ └──────┬───────┘
                                               │
              ┌────────────────────────────────┼────────────────────────┐
              ▼                ▼                ▼                ▼
      ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
      │ Script Svc   │ │ Prompt Svc   │ │ Video Svc    │ │ Voice Svc    │
      │ 实例 1..N    │ │ 实例 1..N    │ │ 实例 1..N    │ │ 实例 1..N    │
      └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
      ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
      │ FFmpeg Svc   │ │ Image Svc    │ │  AI Gateway  │
      │ 实例 1..N    │ │ 实例 1..N    │ │  单实例      │
      └──────────────┘ └──────────────┘ └──────────────┘
```

### 14.8 扩展注意事项

| 组件 | 扩展方式 | 注意事项 |
|------|---------|---------|
| Python 服务 | 增加实例数 | 无需修改代码，注意回调 URL 指向同一个 pipeline-admin 负载均衡器 |
| Java 后台 | 多实例 + Nginx | 回调 URL（`CALLBACK_BASE_URL`）必须指向 Nginx 地址 |
| PostgreSQL | 主从复制 / PgBouncer | 写操作走主库，读操作可分发到从库；需修改 `application.yml` 配置 |
| RabbitMQ | 3 节点集群 | 所有服务需连接同一个集群地址；使用 `haproxy` 或 `nginx` 做 TCP 负载均衡 |
| MinIO | 4 节点分布式 | 最低 4 节点，支持 Erasure Code 数据保护；需挂载独立磁盘 |
| Redis | 主从 / Sentinel | 缓存数据可丢失，主从已足够；Java 配置 `spring.redis.sentinel` |

### 14.9 扩展到 Kubernetes

当节点数超过 10 台时，建议迁移到 Kubernetes：

```yaml
# 关键配置示例
apiVersion: apps/v1
kind: Deployment
metadata:
  name: script-service
spec:
  replicas: 3                    # 副本数，根据负载自动扩缩
  selector:
    matchLabels:
      app: script-service
  template:
    metadata:
      labels:
        app: script-service
    spec:
      containers:
        - name: script-service
          image: registry.example.com/script-service:latest
          env:
            - name: PIPELINE_RABBITMQ_HOST
              value: rabbitmq
            - name: PIPELINE_GATEWAY_URL
              value: http://ai-gateway:8001
            - name: PIPELINE_CALLBACK_TOKEN
              valueFrom:
                secretKeyRef:
                  name: pipeline-secrets
                  key: callback-token
          resources:
            requests:
              cpu: "500m"
              memory: "512Mi"
            limits:
              cpu: "2"
              memory: "2Gi"
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: script-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: script-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

---

## 15. 附录：本地开发模式

> 本节内容面向开发者，非运维人员可跳过。

### 15.1 前置条件

| 工具 | 版本 | 验证 |
|------|------|------|
| JDK | 17+ | `java --version` |
| Maven | 3.8+ | `mvn --version` |
| Python | 3.12+ | `python --version` |
| Node.js | 18+ | `node --version` |
| npm | 9+ | `npm --version` |

### 15.2 启动基础设施（Docker）

```bash
# 仅启动基础设施服务
docker compose up -d postgres redis minio rabbitmq

# 验证
docker compose ps -a
```

### 15.3 启动 Java 后台

```bash
# 方式一：Maven 命令行（热重载）
cd pipeline-manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 方式二：打包后运行
mvn clean package -DskipTests
java -jar target/pipeline-admin-1.0.0-SNAPSHOT.jar

# 方式三：IDE 中运行
# 打开 pipeline-manager/pom.xml 作为 Maven 项目
# 运行 PipelineAdminApplication.java 的 main 方法
```

### 15.4 启动 Python AI 服务

> Python 服务目录名使用连字符（如 `script-service`），但 Python 包名使用下划线（如 `script_service`）。Docker 构建时自动处理此转换，本地开发使用 `-m` 模块方式运行，无需创建符号链接。

**推荐方式：使用 `python -m` 运行（无需符号链接）**

```bash
cd ai-services

# 启动 AI Gateway（必须先启动）
python -m gateway.main &

# 启动其他 Python 服务（分别在新终端中运行）
python -m script_service.main &
python -m prompt_service.main &
python -m video_service.main &
python -m voice_service.main &
python -m ffmpeg_service.main &
python -m image_service.main &
```

> `python -m 包名.模块名` 会将当前目录加入 Python 路径，`common` 包和各个服务包都能被正确找到。

**备选方式：使用 uvicorn 运行（需创建符号链接）**

```bash
cd ai-services
# 创建符号链接（只需做一次）
for dir in script-service prompt-service video-service voice-service ffmpeg-service image-service; do
    pkg="${dir//-/_}"
    [ ! -L "$pkg" ] && ln -s "$dir" "$pkg"
done

# 启动 AI Gateway（必须先启动）
uvicorn gateway.main:app --reload --port 8001 &

# 启动其他 Python 服务
for service in script_service prompt_service video_service voice_service ffmpeg_service image_service; do
    port=${service#*_}
    case $service in script_service) port=8002 ;; prompt_service) port=8003 ;; video_service) port=8004 ;; voice_service) port=8005 ;; ffmpeg_service) port=8006 ;; image_service) port=8007 ;; esac
    uvicorn ${service}.main:app --reload --port $port &
    sleep 1
done
```

**注意**：
- Python 服务需要 RabbitMQ 连接才能消费队列消息
- **API Key 配置**：`Settings()` 从当前目录的 `.env` 文件读取 API Key。运行前需确保 `ai-services/.env` 存在且包含 `PIPELINE_DEEPSEEK_API_KEY` 等配置，或直接设置环境变量
- 各服务启动时会自动初始化 MQ 消费者线程，在后台消费对应队列的消息
- 服务启动流程：`lifespan` 事件中启动后台 MQ 消费者线程，关闭时清理连接

### 15.5 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器（热重载）：`http://localhost:3000`
Vite 配置了代理，`/api` 请求自动转发到 `http://localhost:8080`。

### 15.6 本地开发配置

Python 服务通过 `PIPELINE_` 前缀环境变量配置：

```bash
# 本地开发时，每个服务读自己的 .env 文件
# 例如 ai-services/gateway/.env 内容：
PIPELINE_RABBITMQ_HOST=localhost
PIPELINE_RABBITMQ_PORT=5672
PIPELINE_RABBITMQ_USERNAME=pipeline
PIPELINE_RABBITMQ_PASSWORD=pipeline123
PIPELINE_DEEPSEEK_API_KEY=sk-xxx
PIPELINE_CALLBACK_TOKEN=pipeline-callback-token-change-in-prod
```

> **注意**：本地开发时 `PIPELINE_RABBITMQ_HOST=localhost`（连接宿主机上的 RabbitMQ），Docker 部署时被 `environment` 覆盖为 `rabbitmq`（连接容器内的 RabbitMQ）。