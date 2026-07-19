"""Prompt 生成路由（文生视频 Prompt 生成）。"""

from fastapi import APIRouter
from pydantic import BaseModel
from gateway.providers.registry import get_providers

router = APIRouter(tags=["prompt"])


def _get_prompt_provider(model: str):
    for provider in get_providers():
        if model in provider.supported_models:
            return provider
    return None


class PromptGenerateRequest(BaseModel):
    """Prompt 生成请求"""
    task_id: int
    storyboard_content: str
    model: str = "gpt-4o"


@router.post("/prompt/generate")
async def prompt_generate(request: PromptGenerateRequest):
    """生成文生视频 Prompt（含可灵、Veo 等模型适配）"""
    provider = _get_prompt_provider(request.model)
    if provider is None:
        return {"task_id": request.task_id, "prompt": {"content": "不支持的模型"}, "model": request.model}
    result = await provider.generate(
        prompt=f"你是一个 AI 视频生成 Prompt 工程师。请为以下分镜描述生成一段适用于可灵/Veo 等文生视频模型的 Prompt。\n\n分镜描述：\n{request.storyboard_content}",
        model=request.model,
    )
    return {"task_id": request.task_id, "prompt": result, "model": request.model}