# 服务启动指南

> 涵盖 Docker 部署和本地开发两种方式的详细启动步骤。

---

## 1. Docker 部署（推荐）

### 1.1 前置条件

- Docker >= 24.0
- Docker Compose >= 2.20
- Git

### 1.2 快速启动

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env，至少修改 DB_PASSWORD、JWT_SECRET、CALLBACK_TOKEN

# 2. 启动所有服务
docker compose up -d

# 3. 检查服务状态
docker compose ps -a

# 4. 查看启动日志
docker compose logs --tail=50

# 5. 访问服务
# 前端: http://localhost
# Java API: http://localhost:8080
# API 文档: http://localhost:8080/doc.html
```

### 1.3 分步启动（推荐首次使用）

```bash
# 步骤 1：启动基础设施
docker compose up -d postgres redis minio rabbitmq

# 验证基础设施
docker compose ps
# 输出应该显示 4 个服务都是 "Up" 状态

# 步骤 2：构建并启动 Java 后台
docker compose build pipeline-admin
docker compose up -d pipeline-admin

# 验证 Java 后端
sleep 15  # 等待 Spring Boot 完成初始化
curl http://localhost:8080/api/v1/health

# 步骤 3：构建并启动 Python AI 服务
docker compose build ai-gateway
docker compose up -d ai-gateway

# 验证 AI Gateway
curl http://localhost:8001/health

# 启动其他 Python 服务
docker compose build script-service prompt-service
docker compose up -d script-service prompt-service

docker compose build video-service voice-service
docker compose build ffmpeg-service image-service
docker compose up -d video-service voice-service ffmpeg-service image-service

# 步骤 4：构建并启动前端
docker compose build frontend
docker compose up -d frontend

# 验证前端
curl -I http://localhost
```

### 1.4 验证所有服务

```bash
# 检查所有服务健康状态
echo "=== 服务状态 ==="
docker compose ps -a

echo ""
echo "=== Java 后台 ==="
curl -s http://localhost:8080/api/v1/health

echo ""
echo "=== AI Gateway ==="
curl -s http://localhost:8001/health

echo ""
echo "=== 各 Python 服务 ==="
for port in 8002 8003 8004 8005 8006 8007; do
    echo "Service :$port → $(curl -s http://localhost:$port/health 2>/dev/null || echo 'unreachable')"
done

echo ""
echo "=== 前端 ==="
curl -s -o /dev/null -w "HTTP %{http_code}" http://localhost/
```

### 1.5 首次登录

1. 打开浏览器访问 `http://localhost`
2. 使用默认管理员账号登录（由 `init.sql` 初始化）
3. 进入 **AI 模型配置** 页面配置 API Key
4. 进入 **平台账号** 页面添加社交媒体账号

---

## 2. 本地开发模式

### 2.1 前置条件

| 工具 | 版本 | 验证 |
|------|------|------|
| JDK | 17+ | `java --version` |
| Maven | 3.8+ | `mvn --version` |
| Python | 3.12+ | `python --version` |
| Node.js | 18+ | `node --version` |
| npm | 9+ | `npm --version` |

### 2.2 基础设施（Docker）

```bash
# 仅启动基础设施服务
docker compose up -d postgres redis minio rabbitmq

# 验证
docker compose ps -a
```

### 2.3 启动 Java 后台

```bash
# 方式一：命令行（热重载）
cd pipeline-manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 方式二：打包后运行
mvn clean package -DskipTests
java -jar target/pipeline-admin-1.0.0-SNAPSHOT.jar

# 方式三：IDE 中运行
# 打开 pipeline-manager/pom.xml 作为 Maven 项目
# 运行 PipelineAdminApplication.java 的 main 方法
```

Java 服务启动后访问：
- API: `http://localhost:8080`
- Swagger 文档: `http://localhost:8080/doc.html`
- 健康检查: `http://localhost:8080/api/v1/health`

### 2.4 启动 Python AI 服务

每个 Python 服务可以独立启动，无需 Docker。

```bash
# 安装共享依赖
cd ai-services
pip install -r gateway/requirements.txt
pip install -r script-service/requirements.txt
# ... (安装其他服务的依赖)

# 启动 AI Gateway（必须先启动）
cd ai-services
uvicorn gateway.main:app --reload --port 8001

# 在另一个终端启动 Script Service
cd ai-services
uvicorn script_service.main:app --reload --port 8002
# 或使用 Python 直接运行
python -m script_service.main

# 启动其他服务（新终端）
uvicorn prompt_service.main:app --reload --port 8003
uvicorn video_service.main:app --reload --port 8004
uvicorn voice_service.main:app --reload --port 8005
uvicorn ffmpeg_service.main:app --reload --port 8006
uvicorn image_service.main:app --reload --port 8007
```

**注意**：Python 服务需要 RabbitMQ 连接才能消费队列消息。如果 RabbitMQ 未就绪，服务会启动但无法处理任务。

### 2.5 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器（热重载）：`http://localhost:3000`

Vite 配置了代理，`/api` 请求自动转发到 `http://localhost:8080`（见 `vite.config.ts`）。

### 2.6 本地开发快速启动脚本

```bash
# 保存为 start-dev.sh
#!/bin/bash

# 启动基础设施
docker compose up -d postgres redis minio rabbitmq

# 启动 Java 后台
cd pipeline-manager
mvn spring-boot:run -Dspring-boot.run.profiles=dev &
JAVA_PID=$!

# 启动 Python 服务
cd ../ai-services
uvicorn gateway.main:app --reload --port 8001 &
uvicorn prompt_service.main:app --reload --port 8003 &

# 启动前端
cd ../frontend
npm run dev &
FRONTEND_PID=$!

echo "Java PID: $JAVA_PID"
echo "Frontend PID: $FRONTEND_PID"
wait
```

---

## 3. 环境配置详情

### 3.1 Java 配置

配置文件：`pipeline-manager/src/main/resources/application.yml`

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|---------|--------|------|
| `spring.datasource.password` | `DB_PASSWORD` | `pipeline123` | 数据库密码 |
| `spring.rabbitmq.password` | `RABBITMQ_PASSWORD` | `pipeline123` | MQ 密码 |
| `jwt.secret` | `JWT_SECRET` | `pipeline-secret-key-change-in-prod` | JWT 密钥 |
| `pipeline.callback-base-url` | `CALLBACK_BASE_URL` | `http://host.docker.internal:8080` | 回调地址 |
| `pipeline.callback-token` | `CALLBACK_TOKEN` | `pipeline-callback-token-change-in-prod` | 回调令牌 |

### 3.2 Python 配置

配置文件：`ai-services/common/config.py`（Pydantic Settings）

通过 `PIPELINE_` 前缀环境变量注入，例如：

| 环境变量 | Python 属性 | 默认值 | 说明 |
|---------|------------|--------|------|
| `PIPELINE_RABBITMQ_HOST` | `rabbitmq_host` | `localhost` | RabbitMQ 地址 |
| `PIPELINE_RABBITMQ_PORT` | `rabbitmq_port` | `5672` | RabbitMQ 端口 |
| `PIPELINE_RABBITMQ_USERNAME` | `rabbitmq_username` | `pipeline` | MQ 用户名 |
| `PIPELINE_RABBITMQ_PASSWORD` | `rabbitmq_password` | `pipeline123` | MQ 密码 |
| `PIPELINE_MINIO_ENDPOINT` | `minio_endpoint` | `http://localhost:9000` | MinIO 地址 |
| `PIPELINE_GATEWAY_URL` | `gateway_url` | `http://ai-gateway:8001` | AI Gateway 地址 |
| `PIPELINE_OPENAI_API_KEY` | `openai_api_key` | `` | OpenAI API Key |
| `PIPELINE_ANTHROPIC_API_KEY` | `anthropic_api_key` | `` | Anthropic API Key |
| `PIPELINE_DEEPSEEK_API_KEY` | `deepseek_api_key` | `` | DeepSeek API Key |
| `PIPELINE_CALLBACK_TOKEN` | `callback_token` | `pipeline-callback-token-change-in-prod` | 回调令牌 |
| `PIPELINE_DEBUG` | `debug` | `False` | 调试模式 |

### 3.3 Docker 环境变量注入

在 `docker-compose.yml` 中通过 `environment` 或 `env_file` 注入：

```yaml
services:
  ai-gateway:
    env_file: ./ai-services/gateway/.env          # 本地开发配置
    environment:
      PIPELINE_RABBITMQ_HOST: rabbitmq             # Docker 环境覆盖
      PIPELINE_MINIO_ENDPOINT: http://minio:9000
      PIPELINE_CALLBACK_TOKEN: ${CALLBACK_TOKEN}   # 引用 .env 文件变量
```

---

## 4. 生产环境部署

### 4.1 Docker 部署（生产）

```bash
# 1. 准备生产配置
cp .env.example .env
# 修改以下变量为生产值：
# DB_PASSWORD=强密码
# JWT_SECRET=64位随机字符串
# CALLBACK_TOKEN=64位随机字符串
# MINIO_PASSWORD=强密码
# RABBITMQ_PASSWORD=强密码

# 2. 构建镜像（无缓存）
docker compose build --no-cache

# 3. 启动
docker compose up -d

# 4. 验证
./scripts/health-check.sh  # 如果有健康检查脚本
```

### 4.2 生产安全配置

- **JWT 密钥**：使用 `openssl rand -hex 32` 生成，通过 `JWT_SECRET` 环境变量注入
- **回调令牌**：使用 `openssl rand -hex 32` 生成，通过 `CALLBACK_TOKEN` 环境变量注入
- **数据库密码**：使用强密码，通过 `DB_PASSWORD` 注入
- **API Key**：在管理后台的 AI 模型配置页面设置，加密存储
- **RabbitMQ**：生产环境应配置 SSL 和 VHost

---

## 5. 消息队列调试

### 5.1 RabbitMQ 管理控制台

访问 `http://localhost:15672`（默认凭据 `pipeline` / `pipeline123`）

**关键操作**：
1. **Queues** 标签页查看队列深度和消费者
2. **Exchanges** 标签页查看消息路由
3. **Admin** 标签页管理用户和权限

### 5.2 手动发送 MQ 消息

```bash
# 通过 RabbitMQ 管理 API 发送
curl -u pipeline:pipeline123 \
  -H "Content-Type: application/json" \
  -X POST http://localhost:15672/api/exchanges/%2F/pipeline.script.generate/publish \
  -d '{
    "properties": {"content_type": "application/json", "delivery_mode": 2},
    "routing_key": "pipeline.script.generate",
    "payload": "{\"messageId\":\"manual\",\"taskId\":1,\"action\":\"generate\"}",
    "payload_encoding": "string"
  }'

# 通过 Java API 测试
curl -X POST http://localhost:8080/api/v1/ai-models/test-mq
```

### 5.3 查看队列消费者

```bash
# 通过管理 API
curl -s -u pipeline:pipeline123 http://localhost:15672/api/queues | jq '.[] | {name: .name, consumers: .consumers, messages: .messages_ready}'
```

---

## 6. 数据初始化

系统启动时自动通过 `init.sql` 初始化数据库：

- 创建所有业务表（task、script、storyboard 等）
- 插入默认角色（管理员、运营、编辑）
- 添加字段注释

### 手动初始化

```bash
# 如果数据库需要重新初始化
docker compose down -v     # 删除所有数据卷
docker compose up -d       # 重新创建，init.sql 自动执行

# 或在 PostgreSQL 容器中手动执行
docker exec -i postgres psql -U pipeline -d pipeline < init.sql
```