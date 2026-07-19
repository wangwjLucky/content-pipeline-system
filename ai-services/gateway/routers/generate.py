"""通用生成和 embedding 路由。"""

from fastapi import APIRouter, HTTPException
from common.models import GenerateRequest, GenerateResponse
from gateway.providers.registry import get_providers, get_provider

router = APIRouter(tags=["generate"])


def _get_generate_provider(model: str):
    """根据模型名获取合适的生成 Provider"""
    for provider in get_providers():
        if model in provider.supported_models:
            return provider
    # 默认使用 OpenAI（或 DeepSeek 兜底）
    return get_provider("openai") or get_provider("deepseek")


@router.post("/generate", response_model=GenerateResponse)
async def generate(request: GenerateRequest):
    """通用 AI 生成"""
    provider = _get_generate_provider(request.model)
    if provider is None:
        raise HTTPException(status_code=400, detail=f"不支持的模型: {request.model}")
    result = await provider.generate(
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