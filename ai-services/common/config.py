"""
应用配置，所有配置项从环境变量读取。

## 环境变量命名规则

PIPELINE_XXX → 去掉 PIPELINE_ 前缀，剩下的转小写就是代码里的字段名。
例如 PIPELINE_RABBITMQ_HOST=rabbitmq 会映射到 settings.rabbitmq_host = "rabbitmq"。

## 不同环境的配置加载

加载顺序：环境变量 > .env 文件 > 代码里的默认值。

- 本地开发：根据 PIPELINE_ENV 加载对应的 .env 文件
  - PIPELINE_ENV=dev（默认）→ 加载 .env.dev
  - PIPELINE_ENV=pro            → 加载 .env.pro
- Docker 部署：环境变量由 docker-compose.yml 或 k8s 注入，不需要 .env 文件
"""

import os

from pydantic_settings import BaseSettings


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
    # Java 后台地址（用于同步模型配置，Docker 内用 service 名，本地用 host.docker.internal）
    pipeline_admin_url: str = "http://pipeline-admin:8080"
    openai_api_key: str = ""
    anthropic_api_key: str = ""
    deepseek_api_key: str = ""
    sensenova_api_key: str = ""
    keling_api_key: str = ""
    doubao_api_key: str = ""
    veo_api_key: str = ""

    # 回调认证
    callback_token: str = "pipeline-callback-token-change-in-prod"

    model_config = {"env_prefix": "PIPELINE_"}


# 根据 PIPELINE_ENV 区分环境，默认 dev
_env = os.getenv("PIPELINE_ENV", "dev")
settings = Settings(_env_file=f".env.{_env}")
