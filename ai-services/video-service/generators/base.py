"""视频/图片生成器抽象基类。"""

from abc import ABC, abstractmethod


class BaseGenerator(ABC):
    """AI 生成器基类。"""

    @abstractmethod
    def generate(self, prompt: str, **kwargs) -> dict:
        """根据提示词生成视频/图片，返回包含 url 等信息的字典。"""
        ...