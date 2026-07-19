"""剪辑合成服务 —— 消费 MQ 消息、FFmpeg 合成视频、回调 Java。"""

import threading
from contextlib import asynccontextmanager
from fastapi import FastAPI

from common.config import settings
from common.logging import setup_logging
from common.models import TaskMessage
from common.rabbit import RabbitMQClient
from common.callback import send_callback
from ffmpeg_service.composite import VideoCompositor

logger = setup_logging("ffmpeg_service")

mq_client = RabbitMQClient()
compositor = VideoCompositor()


def handle_ffmpeg_message(message: dict):
    try:
        task_msg = TaskMessage.model_validate(message)
        logger.info(f"收到剪辑合成任务: taskId={task_msg.task_id}, action={task_msg.action}")

        # FFmpeg 合成视频
        result = compositor.compile(task_id=task_msg.task_id, params=task_msg.params)

        callback_url = task_msg.callback_url or "http://pipeline-admin:8080/api/v1/tasks/callback"

        logger.info(f"回调 Java: taskId={task_msg.task_id}")
        send_callback(callback_url, task_msg.task_id, "ffmpeg", "SUCCESS", result, timeout=60)

    except Exception as e:
        logger.error(f"剪辑合成失败: {e}")
        callback_url = message.get("callbackUrl", "")
        if callback_url:
            send_callback(callback_url, message.get("taskId", 0), "ffmpeg", "FAILURE", error=str(e))


def start_mq_consumer():
    try:
        logger.info("启动 MQ 消费者...")
        mq_client.connect()
        mq_client.consume("pipeline.ffmpeg.compile", handle_ffmpeg_message)
    except Exception as e:
        logger.error(f"MQ 消费者异常: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("剪辑合成服务启动中...")
    consumer_thread = threading.Thread(target=start_mq_consumer, daemon=True)
    consumer_thread.start()
    yield
    logger.info("剪辑合成服务关闭中...")
    mq_client.close()


app = FastAPI(title="FFmpeg Service", version="1.0.0", lifespan=lifespan)


@app.get("/health")
async def health():
    return {"status": "UP", "service": "ffmpeg-service"}