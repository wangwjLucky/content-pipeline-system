"""配音生成路由（豆包 Doubao / ElevenLabs 等）。"""

from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional
from gateway.providers.registry import get_provider_weighted

router = APIRouter(tags=["voice"])


class VoiceGenerateRequest(BaseModel):
    """配音生成请求"""
    task_id: int
    text: str
    voice_type: str = "doubao"
    speed: float = 1.05


class VoiceCloneRequest(BaseModel):
    """声音克隆请求"""
    task_id: int
    audio_url: str
    voice_name: str


@router.post("/voice/generate")
async def voice_generate(request: VoiceGenerateRequest):
    """生成配音"""
    provider = get_provider_weighted("audio")
    if provider is None:
        return {"task_id": request.task_id, "error": "配音服务不可用：未配置音频模型", "status": "failed"}
    result = await provider.generate(
        prompt=request.text,
        voice_type=request.voice_type,
        speed=request.speed,
    )
    return {"task_id": request.task_id, "voice_url": result.get("audio_url"), "status": "completed"}


@router.post("/voice/clone")
async def voice_clone(request: VoiceCloneRequest):
    """克隆声音（预留）"""
    return {"task_id": request.task_id, "voice_name": request.voice_name, "status": "pending", "message": "声音克隆功能预留"}


@router.get("/voice/{task_id}")
async def voice_status(task_id: str):
    """查询配音状态"""
    return {"task_id": task_id, "status": "completed", "voice_url": f"https://minio.internal/pipeline-voices/{task_id}/voice.mp3", "duration": 120}