"""通用生成和 embedding 路由。"""

from fastapi import APIRouter, HTTPException
from common.models import GenerateRequest, GenerateResponse
from gateway.providers.registry import get_provider_weighted

router = APIRouter(tags=["generate"])


@router.post("/generate", response_model=GenerateResponse)
async def generate(request: GenerateRequest):
    """通用 AI 生成（按权重选择文本模型）"""
    provider = get_provider_weighted("text")
    if provider is None:
        raise HTTPException(status_code=400, detail=f"不支持的模型: {request.model}")
    if request.model and request.model not in provider.supported_models:
        # 指定了模型但当前 provider 不支持，尝试找支持的 provider
        from gateway.providers.registry import get_providers
        for p in get_providers():
            if request.model in p.supported_models:
                provider = p
                break
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