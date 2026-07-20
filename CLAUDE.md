# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

内容生产流水线系统，三层层架构：

- **前端** — Vue 3 + Ant Design Vue 4.x + Pinia + Vite 5
- **Java 后台** — Spring Boot 3.2 + Java 17 + MyBatis-Plus 3.5.7（业务逻辑层）
- **Python AI 服务** — FastAPI + Python 3.12（AI 能力层，7 个微服务）

## 构建与运行命令

### 完整部署（Docker Compose）

```bash
docker compose up -d
```

### Java 后台（pipeline-manager/）

```bash
cd pipeline-manager
mvn clean package -DskipTests
java -jar target/pipeline-admin-1.0.0-SNAPSHOT.jar
```

### Python AI 服务（ai-services/）

```bash
cd ai-services/<service-name>
pip install -r requirements.txt
python main.py
```

环境变量：`PIPELINE_ENV=dev`（默认）加载 `.env.dev`，`PIPELINE_ENV=pro` 加载 `.env.pro`

### 前端（frontend/）

```bash
cd frontend
npm install
npm run dev      # 开发
npm run build    # 构建
npm run preview  # 预览构建结果
```

## 架构要点

### 服务间通信

- **Java → Python**：RabbitMQ 异步驱动（6 个队列，见 `docs/architecture.md`）
- **Python → Java**：HTTP POST 回调（`X-Callback-Token` 认证，见 `ai-services/common/callback.py`）
- **Python → AI 模型**：统一通过 AI Gateway（:8001），不直接调用第三方 API
- 所有服务通过 MinIO（S3 兼容 API）读写文件

### 任务状态机

12 个状态：`WAIT → SCRIPTING → SCRIPT_REVIEW → STORYBOARD → GENERATING → VOICEOVER → EDITING → REVIEW → READY → PUBLISHED`，外加 `CANCELLED`、`ERROR`。支持自循环和回退。详见 `docs/architecture.md` 状态转换表。

### Python 服务共同模式

每个 AI 服务统一的启动模式：

```python
# main.py
from common.config import settings
from common.rabbit import RabbitMQClient
from common.callback import send_callback

mq = RabbitMQClient()

def handle_message(message):
    # 处理 MQ 消息
    send_callback(settings.callback_url, task_id, "service-name", "SUCCESS", data=result)

mq.start_consumer_thread("pipeline.xxx.generate", handle_message)
```

### 配置体系

- Python 配置：`PIPELINE_` 前缀，Pydantic Settings 管理，按环境加载 `.env.dev` / `.env.pro`
- Java 配置：`application.yml` + `application-{profile}.yml`，`SPRING_PROFILES_ACTIVE=dev` 控制
- 根目录 `.env`：Docker Compose 基础设施的密码和密钥

### 目录结构

| 目录 | 内容 |
|------|------|
| `ai-services/common/` | Python 共享模块（config, callback, logging, minio, rabbit, models） |
| `ai-services/{gateway,script,prompt,video,voice,image,ffmpeg}-service/` | 7 个 AI 微服务，各自独立运行 |
| `pipeline-manager/` | Java Spring Boot 后台（controller/entity/mapper/service） |
| `frontend/` | Vue 3 前端（api/router/stores/views） |