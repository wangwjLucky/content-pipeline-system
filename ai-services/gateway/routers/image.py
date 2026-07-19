"""图片生成路由（可灵 Kling / DALL-E）。"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from gateway.providers.registry import get_provider

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
    """生成图片（调用可灵或 DALL-E）"""
    provider = get_provider("keling") or get_provider("openai")
    if provider is None:
        raise HTTPException(status_code=500, detail="图片生成服务不可用：未配置 Provider")
    result = await provider.generate(
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