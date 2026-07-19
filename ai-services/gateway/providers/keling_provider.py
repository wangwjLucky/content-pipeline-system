"""可灵 AI 视频生成 Provider。

模型列表获取策略：
  1. 启动时通过 API 动态拉取（需配置 API Key）
  2. 拉取失败时回退到硬编码兜底列表
  3. API Key 未配置时模型列表为空
"""

from typing import Any
import httpx
from gateway.providers.base import BaseProvider
from common.logging import setup_logging

logger = setup_logging("keling_provider")

# 兜底模型列表（API 拉取失败时使用）
_FALLBACK_MODELS = ["kling-v1", "kling-v1.5"]


class KelingProvider(BaseProvider):
    """可灵 AI 视频生成 API"""

    def __init__(self, api_key: str = "", endpoint: str = ""):
        super().__init__()
        self.api_key = api_key
        self.endpoint = endpoint or "https://api.klingai.com"
        self._http_client = httpx.AsyncClient(timeout=30)
        self._supported_models = list(_FALLBACK_MODELS) if api_key else []

    @property
    def name(self) -> str:
        return "keling"

    async def close(self) -> None:
        await self._http_client.aclose()

    async def refresh_models(self) -> None:
        """从可灵 AI API 实时拉取模型列表"""
        if not self.api_key:
            logger.info("可灵 AI API Key 未配置，跳过模型列表加载")
            return

        logger.info("正在从可灵 AI API 拉取模型列表...")
        try:
            response = await self._http_client.get(
                f"{self.endpoint}/v1/models",
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                },
                timeout=30,
            )
            response.raise_for_status()
            data = response.json()

            models_raw = data.get("data", [])
            fetched = [m["id"] for m in models_raw if isinstance(m, dict) and m.get("id")]

            if fetched:
                self._supported_models = fetched
                logger.info(f"可灵 AI 模型列表刷新成功: {len(fetched)} 个模型")
            else:
                logger.warning("可灵 AI API 返回的模型列表为空，保留兜底列表")
        except Exception as e:
            logger.warning(f"可灵 AI 模型列表拉取失败: {e}，使用兜底列表")

    async def chat(self, messages: list[dict[str, str]], **kwargs) -> str:
        return "视频模型不支持对话"

    async def generate(self, prompt: str, **kwargs) -> Any:
        """提交视频生成任务"""
        return {"task_id": "mock_video_task", "status": "pending", "prompt": prompt}