"""视频/图片生成服务 —— 消费 MQ 消息、调用 Keling 生成视频/图片、回调 Java。"""

import threading
from contextlib import asynccontextmanager
from fastapi import FastAPI

from common.config import settings
from common.logging import setup_logging
from common.models import TaskMessage
from common.rabbit import RabbitMQClient
from common.callback import send_callback
from video_service.generators.video_generator import VideoGenerator

logger = setup_logging("video_service")

mq_client = RabbitMQClient()
video_generator = VideoGenerator()


def handle_video_message(message: dict):
    """处理视频/图片生成 MQ 消息。"""
    try:
        task_msg = TaskMessage.model_validate(message)
        logger.info(f"收到视频生成任务: taskId={task_msg.task_id}, action={task_msg.action}")

        prompt = task_msg.params.get("prompt", "")
        model = task_msg.params.get("model", "kling")

        result = video_generator.generate(prompt=prompt, model=model)

        callback_url = task_msg.callback_url or "http://pipeline-admin:8080/api/v1/tasks/callback"

        logger.info(f"回调 Java: taskId={task_msg.task_id}")
        send_callback(callback_url, task_msg.task_id, "video", "SUCCESS", result)

    except Exception as e:
        logger.error(f"视频生成失败: {e}")
        callback_url = message.get("callbackUrl", "")
        if callback_url:
            send_callback(callback_url, message.get("taskId", 0), "video", "FAILURE", error=str(e))


def start_mq_consumer():
    try:
        logger.info("启动 MQ 消费者...")
        mq_client.connect()
        mq_client.consume("pipeline.video.generate", handle_video_message)
    except Exception as e:
        logger.error(f"MQ 消费者异常: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("视频生成服务启动中...")
    consumer_thread = threading.Thread(target=start_mq_consumer, daemon=True)
    consumer_thread.start()
    yield
    logger.info("视频生成服务关闭中...")
    mq_client.close()


app = FastAPI(title="Video Service", version="1.0.0", lifespan=lifespan)


@app.get("/health")
async def health():
    return {"status": "UP", "service": "video-service"}