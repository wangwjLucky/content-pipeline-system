"""脚本生成路由（AI Gateway 统一入口）。"""

from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional
from gateway.providers.registry import get_providers

router = APIRouter(tags=["script"])


def _get_script_provider(model: str):
    """根据模型名获取 Provider，默认使用 OpenAI"""
    for provider in get_providers():
        if model in provider.supported_models:
            return provider
    return None


class ScriptGenerateRequest(BaseModel):
    """脚本生成请求"""
    task_id: int
    topic_title: str
    model: str = "gpt-4o"
    temperature: float = 0.7
    max_tokens: int = 4096


class ScriptRewriteRequest(BaseModel):
    """脚本重写请求"""
    content: str
    instructions: Optional[str] = None
    model: str = "gpt-4o"


@router.post("/script/generate")
async def script_generate(request: ScriptGenerateRequest):
    """AI 生成脚本内容"""
    provider = _get_script_provider(request.model)
    if provider is None:
        return {"task_id": request.task_id, "content": {"content": "不支持的模型"}, "model": request.model}
    result = await provider.generate(
        prompt=f"你是一个 AI 技术视频脚本作者。请为主题「{request.topic_title}」生成一个完整的视频脚本，包含开场、正文和结尾。",
        model=request.model,
        temperature=request.temperature,
        max_tokens=request.max_tokens,
    )
    return {"task_id": request.task_id, "content": result, "model": request.model}


@router.post("/script/rewrite")
async def script_rewrite(request: ScriptRewriteRequest):
    """重写/优化脚本"""
    provider = _get_script_provider(request.model)
    if provider is None:
        return {"content": {"content": "不支持的模型"}, "model": request.model}
    instructions = request.instructions or "让语言更通俗易懂，适合抖音短视频风格"
    result = await provider.generate(
        prompt=f"请根据以下要求重写脚本：{instructions}\n\n原始脚本：\n{request.content}",
        model=request.model,
    )
    return {"content": result, "model": request.model}