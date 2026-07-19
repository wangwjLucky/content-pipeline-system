"""Provider 注册表 —— 全局共享的 Provider 实例，由 init_providers() 初始化。

所有 Router 通过此模块获取 Provider 实例，确保：
  1. 所有 Provider 实例是单例的，不重复创建 HTTP 客户端
  2. init_providers() 刷新的模型列表对所有 Router 生效
  3. 新增 Provider 只需在注册表中添加，无需修改各 Router
"""

import asyncio
from typing import Any
from common.config import settings
from common.logging import setup_logging
from gateway.providers.openai_provider import OpenAIProvider
from gateway.providers.claude_provider import ClaudeProvider
from gateway.providers.deepseek_provider import DeepSeekProvider
from gateway.providers.sensenova_provider import SenseNovaProvider
from gateway.providers.keling_provider import KelingProvider
from gateway.providers.doubao_provider import DoubaoProvider
from gateway.providers.veo_provider import VeoProvider

logger = setup_logging("provider_registry")

# 懒加载：_providers 在首次调用 get_providers() 或 init_providers() 时填充
_all_providers: list[Any] = []
_providers_by_name: dict[str, Any] = {}
_initialized = False


def get_providers() -> list[Any]:
    """获取所有已初始化的 Provider 列表（返回副本，防止调用方修改内部状态）"""
    if not _initialized:
        init_providers()
    return list(_all_providers)


def get_provider(name: str) -> Any:
    """按名称获取 Provider 实例"""
    if not _initialized:
        init_providers()
    return _providers_by_name.get(name)


def init_providers() -> None:
    """初始化所有 Provider 实例（幂等，可重复调用）"""
    global _all_providers, _providers_by_name, _initialized

    if _initialized:
        return

    _all_providers = [
        OpenAIProvider(api_key=settings.openai_api_key),
        ClaudeProvider(api_key=settings.anthropic_api_key),
        DeepSeekProvider(api_key=settings.deepseek_api_key),
        SenseNovaProvider(api_key=settings.sensenova_api_key),
        KelingProvider(api_key=settings.keling_api_key),
        DoubaoProvider(api_key=settings.doubao_api_key),
        VeoProvider(),
    ]
    _providers_by_name = {p.name: p for p in _all_providers}
    _initialized = True

    logger.info(f"Provider 注册表初始化完成，共 {len(_all_providers)} 个 Provider")


async def refresh_all_models() -> None:
    """刷新所有 Provider 的模型列表（启动时调用）"""
    if not _initialized:
        init_providers()

    logger.info("正在刷新所有 Provider 模型列表...")

    # 并行刷新所有 Provider 的模型列表，减少启动延迟
    async def _refresh_one(provider: Any) -> None:
        try:
            await provider.refresh_models()
        except Exception as e:
            logger.warning(f"{provider.name} 模型列表刷新失败: {e}")

    await asyncio.gather(*[_refresh_one(p) for p in _all_providers])

    model_count = sum(len(p.supported_models) for p in _all_providers)
    logger.info(f"模型列表刷新完成，共 {model_count} 个模型")


async def shutdown() -> None:
    """关闭所有 Provider，释放资源（服务关闭时调用）"""
    if not _initialized:
        return
    logger.info("正在关闭 Provider 连接...")
    for provider in _all_providers:
        try:
            await provider.close()
        except Exception as e:
            logger.warning(f"{provider.name} 关闭失败: {e}")
    logger.info("所有 Provider 已关闭")