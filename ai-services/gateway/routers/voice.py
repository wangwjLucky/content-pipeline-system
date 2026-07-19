"""配音生成路由（豆包 Doubao / ElevenLabs 等）。"""

from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional
from common.config import settings
from gateway.providers.doubao_provider import DoubaoProvider

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
    """生成配音

    需要配置对应 TTS 服务的 API Key（如豆包、ElevenLabs 等）。
    """
    provider = DoubaoProvider(api_key=settings.openai_api_key)  # 替换为对应 TTS API Key
    result = provider.generate(
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
