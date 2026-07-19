"""聊天路由。"""

from fastapi import APIRouter, HTTPException
from common.models import ChatRequest, ChatResponse
from gateway.providers.registry import get_provider, get_providers

router = APIRouter(tags=["chat"])


def _get_provider(model: str):
    """根据模型名获取 Provider"""
    for provider in get_providers():
        if model in provider.supported_models:
            return provider
    raise HTTPException(status_code=400, detail=f"不支持的模型: {model}")


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """AI 对话"""
    provider = _get_provider(request.model)
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