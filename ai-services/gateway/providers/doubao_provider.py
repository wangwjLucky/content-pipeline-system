"""豆包 / 火山引擎 TTS Provider。"""

from typing import Any
from gateway.providers.base import BaseProvider


class DoubaoProvider(BaseProvider):
    """豆包 TTS 语音合成"""

    def __init__(self, api_key: str = "", endpoint: str = ""):
        self.api_key = api_key
        self.endpoint = endpoint or "https://api.volcengine.com"

    @property
    def name(self) -> str:
        return "doubao"

    @property
    def supported_models(self) -> list[str]:
        return ["doubao-tts-1", "doubao-tts-2"]

    def chat(self, messages: list[dict[str, str]], **kwargs) -> str:
        return "TTS 模型不支持对话"

    def generate(self, prompt: str, **kwargs) -> Any:
        """生成语音"""
        return {"task_id": "mock_tts_task", "status": "pending", "text": prompt}