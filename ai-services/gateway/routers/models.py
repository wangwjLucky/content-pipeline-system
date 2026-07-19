"""模型列表路由。"""

from fastapi import APIRouter
from common.logging import setup_logging
from gateway.providers.registry import get_providers, init_providers as init_registry, refresh_all_models

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
    "veo": "Google Veo",
}


async def init_providers() -> None:
    """初始化所有 Provider 并刷新模型列表（服务启动时调用）"""
    init_registry()  # 初始化注册表（同步，幂等）
    await refresh_all_models()  # 刷新模型列表


@router.get("/models")
async def list_models():
    """列出所有可用 AI 模型（按 Provider 分组）"""
    providers = []
    for provider in get_providers():
        models = []
        for model in provider.supported_models:
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