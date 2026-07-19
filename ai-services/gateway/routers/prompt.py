"""Prompt 生成路由（文生视频 Prompt 生成）。"""

from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional
from common.config import settings
from gateway.providers.openai_provider import OpenAIProvider
from gateway.providers.deepseek_provider import DeepSeekProvider
from gateway.providers.sensenova_provider import SenseNovaProvider

router = APIRouter(tags=["prompt"])

_prompt_providers = {
    "openai": OpenAIProvider(api_key=settings.openai_api_key),
    "deepseek": DeepSeekProvider(api_key=settings.deepseek_api_key),
    "sensenova": SenseNovaProvider(api_key=settings.sensenova_api_key),
}


def _get_prompt_provider(model: str):
    for name, provider in _prompt_providers.items():
        if model in provider.supported_models:
            return provider
    return _prompt_providers.get("openai", OpenAIProvider())


class PromptGenerateRequest(BaseModel):
    """Prompt 生成请求"""
    task_id: int
    storyboard_content: str
    model: str = "gpt-4o"


@router.post("/prompt/generate")
async def prompt_generate(request: PromptGenerateRequest):
    """生成文生视频 Prompt（含可灵、Veo 等模型适配）"""
    provider = _get_prompt_provider(request.model)
    result = provider.generate(
        prompt=f"你是一个 AI 视频生成 Prompt 工程师。请为以下分镜描述生成一段适用于可灵/Veo 等文生视频模型的 Prompt。\n\n分镜描述：\n{request.storyboard_content}",
        model=request.model,
    )
    return {"task_id": request.task_id, "prompt": result, "model": request.model}
