"""模型列表路由。"""

from fastapi import APIRouter
from common.logging import setup_logging
from common.config import settings
from gateway.providers.openai_provider import OpenAIProvider
from gateway.providers.claude_provider import ClaudeProvider
from gateway.providers.deepseek_provider import DeepSeekProvider
from gateway.providers.sensenova_provider import SenseNovaProvider
from gateway.providers.keling_provider import KelingProvider
from gateway.providers.doubao_provider import DoubaoProvider

logger = setup_logging("models_router")

router = APIRouter(tags=["models"])

# Provider 中文名映射
_PROVIDER_NAMES = {
    "openai": "OpenAI",
    "anthropic": "Anthropic Claude",
    "deepseek": "DeepSeek",
    "sensenova": "SenseNova（商汤大装置）",
    "keling": "可灵 AI",
    "doubao": "豆包",
}

# 全局 Provider 实例（lazy init，由 init_providers() 填充）
_all_providers: list = []


async def init_providers() -> None:
    """初始化所有 Provider 并刷新模型列表（服务启动时调用）"""
    global _all_providers

    _all_providers = [
        OpenAIProvider(api_key=settings.openai_api_key),
        ClaudeProvider(api_key=settings.anthropic_api_key),
        DeepSeekProvider(api_key=settings.deepseek_api_key),
        SenseNovaProvider(api_key=settings.sensenova_api_key),
        KelingProvider(),
        DoubaoProvider(),
    ]

    logger.info("正在初始化 AI Provider 模型列表...")
    for provider in _all_providers:
        if hasattr(provider, "refresh_models") and callable(getattr(provider, "refresh_models")):
            try:
                await provider.refresh_models()
            except Exception as e:
                logger.warning(f"{provider.name} 模型列表刷新失败: {e}")

    model_count = sum(len(p.supported_models) for p in _all_providers)
    logger.info(f"所有 Provider 初始化完成，共 {model_count} 个模型")


@router.get("/models")
async def list_models():
    """列出所有可用 AI 模型（按 Provider 分组）"""
    providers = []
    seen_models = set()
    for provider in _all_providers:
        models = []
        for model in provider.supported_models:
            if model not in seen_models:
                seen_models.add(model)
                models.append({
                    "id": model,
                    "provider": provider.name,
                })
        if models:
            providers.append({
                "provider": provider.name,
                "provider_name": _PROVIDER_NAMES.get(provider.name, provider.name),
                "models": models,
            })
    # 扁平列表（兼容前端下拉选择）
    all_models = []
    for p in providers:
        for m in p["models"]:
            all_models.append(m)

    return {
        "models": all_models,
        "providers": providers,
        "total": len(all_models),
    }
