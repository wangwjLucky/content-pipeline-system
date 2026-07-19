"""Google Veo 视频生成 Provider。

模型列表获取策略：
  1. 启动时通过 API 动态拉取（需配置 API Key）
  2. 拉取失败时回退到硬编码兜底列表
  3. API Key 未配置时模型列表为空
"""

from typing import Any
import httpx
from gateway.providers.base import BaseProvider
from common.logging import setup_logging

logger = setup_logging("veo_provider")

# 兜底模型列表（API 拉取失败时使用）
_FALLBACK_MODELS = ["veo-2.0", "veo-3.0"]


class VeoProvider(BaseProvider):
    """Google Veo 视频生成 API"""

    def __init__(self, api_key: str = "", endpoint: str = ""):
        super().__init__()
        self.api_key = api_key
        self.endpoint = endpoint or "https://generativelanguage.googleapis.com"
        self._http_client = httpx.AsyncClient(timeout=30)
        self._supported_models = list(_FALLBACK_MODELS) if api_key else []

    @property
    def name(self) -> str:
        return "veo"

    async def close(self) -> None:
        await self._http_client.aclose()

    async def refresh_models(self) -> None:
        """从 Google API 实时拉取模型列表"""
        if not self.api_key:
            logger.info("Veo API Key 未配置，跳过模型列表加载")
            return

        logger.info("正在从 Google API 拉取模型列表...")
        try:
            response = await self._http_client.get(
                f"{self.endpoint}/v1/models",
                params={"key": self.api_key},
                headers={"Content-Type": "application/json"},
                timeout=30,
            )
            response.raise_for_status()
            data = response.json()

            # Google 格式: {"models":[{"name":"models/veo-2.0",...}]}
            models_raw = data.get("models", [])
            fetched = []
            for m in models_raw:
                if isinstance(m, dict) and m.get("name"):
                    name = m["name"]
                    # 去掉 "models/" 前缀，只保留模型 ID
                    model_id = name.split("/")[-1] if name.startswith("models/") else name
                    fetched.append(model_id)

            if fetched:
                self._supported_models = fetched
                logger.info(f"Veo 模型列表刷新成功: {len(fetched)} 个模型")
            else:
                logger.warning("Google API 返回的模型列表为空，保留兜底列表")
        except Exception as e:
            logger.warning(f"Veo 模型列表拉取失败: {e}，使用兜底列表")

    async def chat(self, messages: list[dict[str, str]], **kwargs) -> str:
        return "视频模型不支持对话"

    async def generate(self, prompt: str, **kwargs) -> Any:
        """提交视频生成任务到 Veo API"""
        return {"task_id": "mock_veo_task", "status": "pending", "prompt": prompt}