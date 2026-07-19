"""可灵 AI 视频生成 Provider。"""

from typing import Any
from gateway.providers.base import BaseProvider


class KelingProvider(BaseProvider):
    """可灵 AI 视频生成 API"""

    def __init__(self, api_key: str = "", endpoint: str = ""):
        super().__init__()
        self.api_key = api_key
        self.endpoint = endpoint or "https://api.klingai.com"
        self._supported_models = ["kling-v1", "kling-v1.5"]

    @property
    def name(self) -> str:
        return "keling"

    async def chat(self, messages: list[dict[str, str]], **kwargs) -> str:
        return "视频模型不支持对话"

    async def generate(self, prompt: str, **kwargs) -> Any:
        """提交视频生成任务"""
        return {"task_id": "mock_video_task", "status": "pending", "prompt": prompt}