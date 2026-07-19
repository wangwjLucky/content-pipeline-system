"""聊天路由。"""

from fastapi import APIRouter, HTTPException
from common.config import settings
from common.models import ChatRequest, ChatResponse
from gateway.providers.openai_provider import OpenAIProvider
from gateway.providers.claude_provider import ClaudeProvider
from gateway.providers.deepseek_provider import DeepSeekProvider
from gateway.providers.sensenova_provider import SenseNovaProvider

router = APIRouter(tags=["chat"])

# Provider 注册表（注入 API Key）
_providers: dict[str, OpenAIProvider | ClaudeProvider | DeepSeekProvider | SenseNovaProvider] = {
    "openai": OpenAIProvider(api_key=settings.openai_api_key),
    "anthropic": ClaudeProvider(api_key=settings.anthropic_api_key),
    "deepseek": DeepSeekProvider(api_key=settings.deepseek_api_key),
    "sensenova": SenseNovaProvider(api_key=settings.sensenova_api_key),
}


def _get_provider(model: str):
    """根据模型名获取 Provider"""
    for name, provider in _providers.items():
        if model in provider.supported_models:
            return provider
    raise HTTPException(status_code=400, detail=f"不支持的模型: {model}")


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """AI 对话"""
    provider = _get_provider(request.model)
    content = provider.chat(
        messages=request.messages,
        model=request.model,
        temperature=request.temperature,
        max_tokens=request.max_tokens,
    )
    return ChatResponse(content=content, model=request.model)


@router.post("/chat/{provider_name}")
async def chat_by_provider(provider_name: str, request: ChatRequest):
    """按 Provider 调用 AI 对话"""
    if provider_name not in _providers:
        raise HTTPException(status_code=404, detail=f"不支持的 Provider: {provider_name}")
    provider = _providers[provider_name]
    content = provider.chat(
        messages=request.messages,
        model=request.model,
        temperature=request.temperature,
        max_tokens=request.max_tokens,
    )
    return ChatResponse(content=content, model=request.model)