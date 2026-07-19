"""脚本生成服务 —— 消费 MQ 消息、调用 AI 生成脚本、回调 Java。"""

import threading
from contextlib import asynccontextmanager
from fastapi import FastAPI

from common.config import settings
from common.logging import setup_logging
from common.models import TaskMessage
from common.rabbit import RabbitMQClient
from common.callback import send_callback
from script_service.generators.llm_generator import LLMScriptGenerator

logger = setup_logging("script_service")

mq_client = RabbitMQClient()
script_generator = LLMScriptGenerator()


def handle_script_message(message: dict):
    """处理脚本生成 MQ 消息。"""
    try:
        task_msg = TaskMessage.model_validate(message)
        logger.info(f"收到脚本生成任务: taskId={task_msg.task_id}, action={task_msg.action}")

        topic = task_msg.params.get("topic", task_msg.params.get("title", ""))
        model = task_msg.params.get("model", "gpt-4o")

        # 调用 AI 生成脚本
        script_data = script_generator.generate(topic=topic, model=model)

        # 回调 Java
        callback_url = task_msg.callback_url
        if not callback_url:
            # 默认回调地址
            callback_url = "http://pipeline-admin:8080/api/v1/tasks/callback"

        logger.info(f"回调 Java: taskId={task_msg.task_id}, url={callback_url}")
        send_callback(callback_url, task_msg.task_id, "script", "SUCCESS", script_data)

    except Exception as e:
        logger.error(f"脚本处理失败: {e}")
        # 发送失败回调
        callback_url = message.get("callbackUrl", "")
        if callback_url:
            send_callback(callback_url, message.get("taskId", 0), "script", "FAILURE", error=str(e))


def start_mq_consumer():
    """在后台线程中启动 MQ 消费者。"""
    try:
        logger.info("启动 MQ 消费者...")
        mq_client.connect()
        mq_client.consume("pipeline.script.generate", handle_script_message)
    except Exception as e:
        logger.error(f"MQ 消费者异常: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理。"""
    logger.info("脚本生成服务启动中...")
    # 后台启动 MQ 消费者
    consumer_thread = threading.Thread(target=start_mq_consumer, daemon=True)
    consumer_thread.start()
    yield
    logger.info("脚本生成服务关闭中...")
    mq_client.close()


app = FastAPI(title="Script Service", version="1.0.0", lifespan=lifespan)


@app.get("/health")
async def health():
    """健康检查。"""
    return {"status": "UP", "service": "script-service"}