"""图片生成路由（可灵 Kling / DALL-E）。"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from gateway.providers.registry import get_provider_weighted

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
    provider = get_provider_weighted("image")
    if provider is None:
        provider = get_provider_weighted("text")
    if provider is None:
        raise HTTPException(status_code=500, detail="图片生成服务不可用：未配置图片或文本模型")
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