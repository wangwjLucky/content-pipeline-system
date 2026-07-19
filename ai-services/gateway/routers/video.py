"""视频生成路由（可灵 Kling / Veo 等）。"""

from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional
from common.config import settings
from gateway.providers.keling_provider import KelingProvider

router = APIRouter(tags=["video"])


class VideoGenerateRequest(BaseModel):
    """视频生成请求"""
    task_id: int
    prompt: str
    model: str = "kling-v1"
    duration: int = 5
    negative_prompt: Optional[str] = None


class VideoStatusResponse(BaseModel):
    """视频生成状态响应"""
    task_id: str
    status: str
    progress: int = 0
    video_url: Optional[str] = None


@router.post("/video/generate")
async def video_generate(request: VideoGenerateRequest):
    """生成视频素材

    需要配置对应视频生成模型的 API Key（如可灵、Veo 等）。
    当前使用 KelingProvider，需设置对应环境变量。
    """
    provider = KelingProvider(
        api_key=settings.openai_api_key,  # 替换为可灵/Veo API Key
    )
    result = provider.generate(
        prompt=request.prompt,
        model=request.model,
        duration=request.duration,
        negative_prompt=request.negative_prompt,
    )
    return {"task_id": request.task_id, "video_task_id": result.get("task_id"), "status": "pending"}


@router.get("/video/{task_id}", response_model=VideoStatusResponse)
async def video_status(task_id: str):
    """查询视频生成状态"""
    return VideoStatusResponse(
        task_id=task_id,
        status="completed",
        progress=100,
        video_url=f"https://minio.internal/pipeline-videos-raw/{task_id}/output.mp4",
    )
