"""Google Veo 视频生成 Provider。"""

from typing import Any
from gateway.providers.base import BaseProvider


class VeoProvider(BaseProvider):
    """Google Veo 视频生成 API"""

    def __init__(self, api_key: str = "", endpoint: str = ""):
        super().__init__()
        self.api_key = api_key
        self.endpoint = endpoint or "https://generativelanguage.googleapis.com"
        self._supported_models = ["veo-2.0", "veo-3.0"]

    @property
    def name(self) -> str:
        return "veo"

    def chat(self, messages: list[dict[str, str]], **kwargs) -> str:
        return "视频模型不支持对话"

    def generate(self, prompt: str, **kwargs) -> Any:
        """提交视频生成任务到 Veo API"""
        return {"task_id": "mock_veo_task", "status": "pending", "prompt": prompt}