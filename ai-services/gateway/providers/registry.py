"""Provider 注册表 —— 全局共享的 Provider 实例，由 init_providers() 初始化。

所有 Router 通过此模块获取 Provider 实例，确保：
  1. 所有 Provider 实例是单例的，不重复创建 HTTP 客户端
  2. init_providers() 刷新的模型列表对所有 Router 生效
  3. 新增 Provider 只需在注册表中添加，无需修改各 Router
"""

import asyncio
import http.client
import json
import random
import subprocess
import threading
import time
from typing import Any
import httpx
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
        VeoProvider(api_key=settings.veo_api_key),
    ]
    _providers_by_name = {p.name: p for p in _all_providers}
    _initialized = True

    logger.info(f"Provider 注册表初始化完成，共 {len(_all_providers)} 个 Provider")


def _sync_model_configs() -> None:
    """从 Java 后台同步模型配置，覆盖 Provider 的类型和权重"""
    from urllib.parse import urlparse
    admin_url = settings.pipeline_admin_url.rstrip("/")
    parsed = urlparse(admin_url)
    host = parsed.hostname or "host.docker.internal"
    port = parsed.port or 8080
    path = "/api/v1/ai-models?page=1&size=100"

    configs = []
    for attempt in range(5):
        try:
            # 用 getent 解析 hostname（兼容 Docker Desktop 的 DNS 延迟）
            result = subprocess.run(
                ["getent", "hosts", host],
                capture_output=True, text=True, timeout=5)
            if result.returncode != 0:
                raise Exception(f"DNS 解析失败: {host}")
            ip = result.stdout.split()[0]

            conn = http.client.HTTPConnection(ip, port, timeout=10)
            conn.request("GET", path, headers={
                "Host": host,
                "X-Callback-Token": settings.callback_token,
            })
            resp = conn.getresponse()
            body = json.loads(resp.read().decode())
            conn.close()
            configs = body.get("data", {}).get("records", [])
            break
        except Exception as e:
            if attempt < 4:
                time.sleep(5)
                continue
            logger.warning(f"从 Java 后台同步模型配置失败: {e}，使用 Provider 默认值")
            return
    if not configs:
        logger.info("Java 后台无模型配置，使用 Provider 默认值")
        return

    for cfg in configs:
        model_name = cfg.get("modelName")
        provider_name = cfg.get("provider")
        model_type = cfg.get("modelType")
        weight = cfg.get("weight")
        if not model_name or not provider_name:
            continue

        provider = _providers_by_name.get(provider_name)
        if not provider:
            logger.warning(f"同步配置时未找到 Provider: {provider_name}")
            continue

        # 更新模型类型映射
        if model_type:
            if not hasattr(provider, "_model_type_map") or provider._model_type_map is None:
                provider._model_type_map = {}
            provider._model_type_map[model_name] = model_type

        # 更新 Provider 权重（取所有配置中的最大值）
        if weight is not None:
            provider._weight = max(provider._weight, weight)

        logger.info(
        f"模型配置已同步: {model_name} → provider={provider_name}, "
        f"type={model_type}, weight={weight}"
    )

    logger.info("模型配置同步完成")


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

    # 后台线程延迟同步（等待网络就绪）
    threading.Thread(target=lambda: (time.sleep(5), _sync_model_configs()), daemon=True).start()


def get_model_type(model_id: str) -> str | None:
    """查询指定模型 ID 的类型（遍历所有 Provider）"""
    for provider in get_providers():
        if model_id in provider.supported_models:
            return provider.get_model_type(model_id)
    return None


def get_providers_by_type(model_type: str) -> list[Any]:
    """按模型类型获取 Provider 列表，按权重降序排列"""
    providers = [p for p in get_providers() if p.model_type == model_type and p.supported_models]
    providers.sort(key=lambda p: p.weight, reverse=True)
    return providers


def get_provider_weighted(model_type: str) -> Any | None:
    """按模型类型和权重随机选择一个 Provider（加权随机）"""
    providers = get_providers_by_type(model_type)
    if not providers:
        return None
    weights = [p.weight for p in providers]
    total = sum(weights)
    if total <= 0:
        return providers[0]
    r = random.uniform(0, total)
    cumulative = 0
    for i, p in enumerate(providers):
        cumulative += weights[i]
        if r <= cumulative:
            return p
    return providers[-1]


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