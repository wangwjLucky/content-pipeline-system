"""通用生成和 embedding 路由。"""

from fastapi import APIRouter, HTTPException
from common.config import settings
from common.models import GenerateRequest, GenerateResponse
from gateway.providers.openai_provider import OpenAIProvider
from gateway.providers.deepseek_provider import DeepSeekProvider
from gateway.providers.sensenova_provider import SenseNovaProvider

router = APIRouter(tags=["generate"])

# 通用生成 Provider 注册表
_generate_providers = {
    "openai": OpenAIProvider(api_key=settings.openai_api_key),
    "deepseek": DeepSeekProvider(api_key=settings.deepseek_api_key),
    "sensenova": SenseNovaProvider(api_key=settings.sensenova_api_key),
}


def _get_generate_provider(model: str):
    """根据模型名获取合适的生成 Provider"""
    for name, provider in _generate_providers.items():
        if model in provider.supported_models:
            return provider
    # 默认使用 OpenAI
    return _generate_providers.get("openai", OpenAIProvider())


@router.post("/generate", response_model=GenerateResponse)
async def generate(request: GenerateRequest):
    """通用 AI 生成"""
    provider = _get_generate_provider(request.model)
    result = provider.generate(
        prompt=request.prompt,
        model=request.model,
        temperature=request.temperature,
        max_tokens=request.max_tokens,
    )
    return GenerateResponse(result=result, model=request.model)


@router.post("/embedding")
async def embedding(text: str, model: str = "text-embedding-3-small"):
    """文本向量化（预留）"""
    return {"text": text, "model": model, "embedding": [], "dimensions": 0}
