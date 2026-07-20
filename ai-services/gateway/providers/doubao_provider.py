"""豆包 / 火山引擎 TTS Provider。

模型列表获取策略：
  1. API Key 未配置时模型列表为空
  2. 有 API Key 时从 API 动态拉取 TTS 模型列表
  3. 拉取失败时回退到预置兜底列表
"""

from typing import Any
import httpx
from gateway.providers.base import BaseProvider
from common.logging import setup_logging

logger = setup_logging("doubao_provider")

# 兜底模型列表（API 拉取失败时使用）
_FALLBACK_MODELS = ["doubao-tts-1", "doubao-tts-2"]


class DoubaoProvider(BaseProvider):
    """豆包 TTS 语音合成"""

    def __init__(self, api_key: str = "", endpoint: str = ""):
        super().__init__()
        self._model_type = "audio"
        self._weight = 10
        self.api_key = api_key
        self.endpoint = endpoint or "https://open.volcengine.com"
        self._http_client = httpx.AsyncClient(timeout=30)
        self._supported_models = list(_FALLBACK_MODELS) if api_key else []

    @property
    def name(self) -> str:
        return "doubao"

    async def close(self) -> None:
        await self._http_client.aclose()

    async def refresh_models(self) -> None:
        """从火山引擎 API 实时拉取 TTS 模型列表"""
        if not self.api_key:
            logger.info("豆包 API Key 未配置，跳过模型列表加载")
            return

        logger.info("正在从火山引擎 API 拉取 TTS 模型列表...")
        try:
            response = await self._http_client.get(
                f"{self.endpoint}/api/v1/tts/models",
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                },
                timeout=30,
            )
            response.raise_for_status()
            data = response.json()

            models_raw = data.get("data", []) or data.get("models", []) or data.get("voices", [])
            fetched = []
            for m in models_raw:
                if isinstance(m, dict):
                    if m.get("id"):
                        fetched.append(m["id"])
                    elif m.get("name"):
                        fetched.append(m["name"])
                elif isinstance(m, str):
                    fetched.append(m)

            if fetched:
                self._supported_models = fetched
                logger.info(f"豆包 TTS 模型列表刷新成功: {len(fetched)} 个模型")
            else:
                logger.warning("火山引擎 API 返回的模型列表为空，保留兜底列表")
        except Exception as e:
            logger.warning(f"豆包 TTS 模型列表拉取失败: {e}，使用兜底列表")

    async def chat(self, messages: list[dict[str, str]], **kwargs) -> str:
        return "TTS 模型不支持对话"

    async def generate(self, prompt: str, **kwargs) -> Any:
        """生成语音"""
        return {"task_id": "mock_tts_task", "status": "pending", "text": prompt}