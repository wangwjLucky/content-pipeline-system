"""聊天路由。"""

from fastapi import APIRouter, HTTPException
from common.models import ChatRequest, ChatResponse
from gateway.providers.registry import get_provider_weighted, get_providers

router = APIRouter(tags=["chat"])


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """AI 对话（按权重选择文本模型）"""
    provider = get_provider_weighted("text")
    if request.model:
        for p in get_providers():
            if request.model in p.supported_models:
                provider = p
                break
    if provider is None:
        raise HTTPException(status_code=400, detail=f"不支持的模型: {request.model}")
    content = await provider.chat(
        messages=request.messages,
        model=request.model,
        temperature=request.temperature,
        max_tokens=request.max_tokens,
    )
    return ChatResponse(content=content, model=request.model)


@router.post("/chat/{provider_name}")
async def chat_by_provider(provider_name: str, request: ChatRequest):
    """按 Provider 调用 AI 对话"""
    provider = get_provider(provider_name)
    if provider is None:
        raise HTTPException(status_code=404, detail=f"不支持的 Provider: {provider_name}")
    content = await provider.chat(
        messages=request.messages,
        model=request.model,
        temperature=request.temperature,
        max_tokens=request.max_tokens,
    )
    return ChatResponse(content=content, model=request.model)