# AI 服务层 — Python 微服务

## 结构

```
ai-services/
├── common/                # 共享库
│   ├── config.py          #   Pydantic Settings（PIPELINE_ 前缀环境变量）
│   ├── logging.py         #   日志配置（RotatingFileHandler，分级别）
│   ├── models.py          #   数据模型（TaskMessage、CallbackBody）
│   ├── callback.py        #   回调 Java 公共函数（自动注入令牌）
│   ├── rabbit.py          #   RabbitMQ 连接管理（线程安全）
│   └── minio.py           #   MinIO S3 客户端封装
├── gateway/               # AI Gateway — 统一模型路由（:8001）
│   ├── routers/           #   路由（chat、image、voice、video 等）
│   └── providers/         #   模型 Provider（OpenAI、Claude、Doubao、Keling）
├── script-service/        # 脚本生成服务（:8002）
│   └── generators/        #   LLM 脚本生成器
├── prompt-service/        # 分镜生成服务（:8003）
│   └── generators/        #   LLM 分镜拆分生成器
├── video-service/         # 视频生成服务（:8004）
│   └── generators/        #   视频生成器（调用 Keling API）
├── voice-service/         # 配音生成服务（:8005）
│   └── tts/               #   Doubao TTS 引擎
├── ffmpeg-service/        # 剪辑合成服务（:8006）
│   ├── composite.py       #   视频合成
│   ├── audio.py           #   音频处理
│   ├── subtitle.py        #   字幕生成
│   └── cover.py           #   封面生成
└── image-service/         # 图片生成服务（:8007）
    └── generators/        #   图片生成器（调用 Keling/DALL-E）
```

## 通信方式

- **Java → Python（异步）**：RabbitMQ 消息队列（6 个队列）
- **Java → Python（同步）**：HTTP 调用 AI Gateway
- **Python → Java（回调）**：HTTP POST 携带 `X-Callback-Token`
- **Python → AI API**：通过 AI Gateway 统一路由

## 本地启动

```bash
# 安装依赖
pip install -r gateway/requirements.txt

# 启动（示例：AI Gateway）
uvicorn gateway.main:app --reload --port 8001

# 启动（示例：Script Service）
uvicorn script_service.main:app --reload --port 8002
```

## 配置

所有配置通过 `PIPELINE_` 前缀环境变量注入，由 `common/config.py` 的 Pydantic Settings 读取。
详见 `docs/startup-guide.md` 的配置章节。