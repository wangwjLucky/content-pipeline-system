"""
应用配置，从环境变量读取。

配置注入链路（Docker 部署）：
  docker-compose.yml                  Docker 容器                     Python
  ─────────────────                  ────────────                   ────────
  environment:                       环境变量                         Pydantic Settings
    PIPELINE_RABBITMQ_HOST: rabbitmq  →  $PIPELINE_RABBITMQ_HOST     →  settings.rabbitmq_host = "rabbitmq"
    PIPELINE_RABBITMQ_PORT: "5672"    →  $PIPELINE_RABBITMQ_PORT     →  settings.rabbitmq_port = 5672

env_prefix = "PIPELINE_" 表示查找环境变量中 PIPELINE_XXX 格式的变量，
去掉前缀后映射到同名的字段（小写）。代码中通过 from common.config import settings
导入后使用 settings.rabbitmq_host 读取。

env_file: ".env" 仅用于本地开发（直接 python main.py），
Docker 内环境变量已由 docker-compose 注入，不会读 .env 文件。
"""

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
    openai_api_key: str = ""
    anthropic_api_key: str = ""
    deepseek_api_key: str = ""
    sensenova_api_key: str = ""
    keling_api_key: str = ""
    doubao_api_key: str = ""

    # 回调认证
    callback_token: str = "pipeline-callback-token-change-in-prod"

    model_config = {"env_prefix": "PIPELINE_", "env_file": ".env"}


settings = Settings()
