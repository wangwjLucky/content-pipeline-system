"""豆包 / 火山引擎 TTS Provider。"""

from typing import Any
from gateway.providers.base import BaseProvider


class DoubaoProvider(BaseProvider):
    """豆包 TTS 语音合成"""

    def __init__(self, api_key: str = "", endpoint: str = ""):
        super().__init__()
        self.api_key = api_key
        self.endpoint = endpoint or "https://api.volcengine.com"
        self._supported_models = ["doubao-tts-1", "doubao-tts-2"]

    @property
    def name(self) -> str:
        return "doubao"

    async def chat(self, messages: list[dict[str, str]], **kwargs) -> str:
        return "TTS 模型不支持对话"

    async def generate(self, prompt: str, **kwargs) -> Any:
        """生成语音"""
        return {"task_id": "mock_tts_task", "status": "pending", "text": prompt}