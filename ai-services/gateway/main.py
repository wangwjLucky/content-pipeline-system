"""AI Gateway — 统一 AI 模型路由入口。"""

from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from common.logging import setup_logging
from common.config import settings
from gateway.routers import (
    chat,
    generate,
    models,
    image,
    ffmpeg,
    script,
    prompt,
    video,
    voice,
)


logger = setup_logging("gateway")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("AI Gateway 启动中...")
    logger.info(f"RabbitMQ: {settings.rabbitmq_host}:{settings.rabbitmq_port}")

    # 初始化所有 Provider 并刷新模型列表
    await models.init_providers()

    yield
    logger.info("AI Gateway 已关闭")


app = FastAPI(
    title="AI Gateway",
    description="内容生产流水线 — 统一 AI 模型路由",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


app.include_router(chat.router, prefix="/ai/v1")
app.include_router(generate.router, prefix="/ai/v1")
app.include_router(models.router, prefix="/ai/v1")
app.include_router(image.router, prefix="/ai/v1")
app.include_router(ffmpeg.router, prefix="/ai/v1")
app.include_router(script.router, prefix="/ai/v1")
app.include_router(prompt.router, prefix="/ai/v1")
app.include_router(video.router, prefix="/ai/v1")
app.include_router(voice.router, prefix="/ai/v1")


@app.get("/health")
async def health():
    return {"status": "ok"}


if __name__ == "__main__":
    uvicorn.run("gateway.main:app", host="0.0.0.0", port=8001, reload=True)
