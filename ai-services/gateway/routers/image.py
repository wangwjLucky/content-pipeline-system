"""图片生成路由（可灵 Kling / DALL-E）。"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from common.config import settings
from gateway.providers.keling_provider import KelingProvider

router = APIRouter(tags=["image"])


class ImageGenerateRequest(BaseModel):
    """图片生成请求"""
    prompt: str
    model: str = "kling-v1"
    negative_prompt: Optional[str] = None
    width: int = 1080
    height: int = 1920


class ImageGenerateResponse(BaseModel):
    """图片生成响应"""
    task_id: str
    status: str
    prompt: str
    image_url: Optional[str] = None


@router.post("/image/generate", response_model=ImageGenerateResponse)
async def image_generate(request: ImageGenerateRequest):
    """生成图片（调用可灵或 DALL-E）

    需要配置环境变量 PIPELINE_OPENAI_API_KEY（DALL-E）或对应模型 API Key。
    """
    provider = KelingProvider(
        api_key=settings.openai_api_key,  # 实际部署时替换为对应模型 API Key
    )
    result = provider.generate(
        prompt=request.prompt,
        model=request.model,
        negative_prompt=request.negative_prompt,
        width=request.width,
        height=request.height,
    )
    return ImageGenerateResponse(
        task_id=result.get("task_id", ""),
        status=result.get("status", "pending"),
        prompt=request.prompt,
        image_url=result.get("image_url"),
    )
