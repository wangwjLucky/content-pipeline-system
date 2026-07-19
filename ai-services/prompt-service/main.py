"""分镜生成服务 —— 消费 MQ 消息、调用 AI 拆分脚本为分镜、回调 Java。"""

import threading
from contextlib import asynccontextmanager
from fastapi import FastAPI

from common.config import settings
from common.logging import setup_logging
from common.models import TaskMessage
from common.rabbit import RabbitMQClient
from common.callback import send_callback
from prompt_service.generators.llm_prompt_generator import LLMPromptGenerator

logger = setup_logging("prompt_service")

mq_client = RabbitMQClient()
prompt_generator = LLMPromptGenerator()


def handle_prompt_message(message: dict):
    """处理分镜生成 MQ 消息。"""
    try:
        task_msg = TaskMessage.model_validate(message)
        logger.info(f"收到分镜生成任务: taskId={task_msg.task_id}, action={task_msg.action}")

        # 提取脚本内容
        content = task_msg.params.get("content", "")
        model = task_msg.params.get("model", "gpt-4o")

        # 调用 AI 拆分为分镜
        storyboards = prompt_generator.split(content=content, model=model)

        # 回调 Java
        callback_url = task_msg.callback_url or "http://pipeline-admin:8080/api/v1/tasks/callback"

        logger.info(f"回调 Java: taskId={task_msg.task_id}, storyboards_count={len(storyboards)}")
        send_callback(callback_url, task_msg.task_id, "prompt", "SUCCESS", {"storyboards": storyboards})

    except Exception as e:
        logger.error(f"分镜生成失败: {e}")
        callback_url = message.get("callbackUrl", "")
        if callback_url:
            send_callback(callback_url, message.get("taskId", 0), "prompt", "FAILURE", error=str(e))


def start_mq_consumer():
    """在后台线程中启动 MQ 消费者。"""
    try:
        logger.info("启动 MQ 消费者...")
        mq_client.connect()
        mq_client.consume("pipeline.prompt.generate", handle_prompt_message)
    except Exception as e:
        logger.error(f"MQ 消费者异常: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("分镜生成服务启动中...")
    consumer_thread = threading.Thread(target=start_mq_consumer, daemon=True)
    consumer_thread.start()
    yield
    logger.info("分镜生成服务关闭中...")
    mq_client.close()


app = FastAPI(title="Prompt Service", version="1.0.0", lifespan=lifespan)


@app.get("/health")
async def health():
    return {"status": "UP", "service": "prompt-service"}