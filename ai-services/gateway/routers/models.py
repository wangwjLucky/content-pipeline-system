"""模型列表路由。"""

from fastapi import APIRouter
from gateway.providers.openai_provider import OpenAIProvider
from gateway.providers.claude_provider import ClaudeProvider
from gateway.providers.keling_provider import KelingProvider
from gateway.providers.doubao_provider import DoubaoProvider

router = APIRouter(tags=["models"])

_all_providers = [
    OpenAIProvider(),
    ClaudeProvider(),
    KelingProvider(),
    DoubaoProvider(),
]


@router.get("/models")
async def list_models():
    """列出所有可用 AI 模型"""
    models = []
    for provider in _all_providers:
        for model in provider.supported_models:
            models.append({
                "name": model,
                "provider": provider.name,
            })
    return {"models": models}