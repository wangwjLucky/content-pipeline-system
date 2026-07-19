"""FFmpeg 剪辑合成路由。"""

from fastapi import APIRouter
from typing import Optional
from pydantic import BaseModel

router = APIRouter(tags=["ffmpeg"])


class CompositeRequest(BaseModel):
    """视频合成请求"""
    task_id: int
    video_urls: list[str] = []
    voice_url: Optional[str] = None
    bgm_url: Optional[str] = None


class SubtitleRequest(BaseModel):
    """字幕生成请求"""
    task_id: int
    text: str
    style: str = "ass"


class CoverRequest(BaseModel):
    """封面生成请求"""
    task_id: int
    title: str
    style: str = "tech"


class AudioRequest(BaseModel):
    """音频处理请求"""
    task_id: int
    voice_url: str
    bgm_url: Optional[str] = None
    speed: float = 1.05


@router.post("/ffmpeg/composite")
async def ffmpeg_composite(request: CompositeRequest):
    """视频合成（FFmpeg）"""
    return {"task_id": request.task_id, "status": "processing", "message": "合成任务已提交"}


@router.post("/ffmpeg/subtitle")
async def ffmpeg_subtitle(request: SubtitleRequest):
    """字幕生成"""
    return {"task_id": request.task_id, "subtitle_url": f"https://minio.internal/pipeline-temp/{request.task_id}/subtitle.ass", "status": "completed"}


@router.post("/ffmpeg/cover")
async def ffmpeg_cover(request: CoverRequest):
    """封面生成"""
    return {"task_id": request.task_id, "cover_url": f"https://minio.internal/pipeline-covers/{request.task_id}/cover.png", "status": "completed"}


@router.post("/ffmpeg/audio")
async def ffmpeg_audio(request: AudioRequest):
    """音频处理（混音、变速等）"""
    return {"task_id": request.task_id, "audio_url": f"https://minio.internal/pipeline-temp/{request.task_id}/audio_mixed.mp3", "status": "completed"}


@router.get("/ffmpeg/{task_id}")
async def ffmpeg_status(task_id: str):
    """查询 FFmpeg 合成任务状态"""
    return {
        "task_id": task_id,
        "status": "completed",
        "progress": 100,
        "output_url": f"https://minio.internal/pipeline-videos-final/{task_id}/output.mp4",
        "duration_seconds": 120,
    }