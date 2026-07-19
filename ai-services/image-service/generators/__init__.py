"""图片生成器基类。"""

from abc import ABC, abstractmethod


class BaseImageGenerator(ABC):
    """图片生成器抽象基类。"""

    @abstractmethod
    def generate(self, prompt: str, **kwargs) -> dict:
        """生成图片，返回结果字典（含 url 等字段）。"""
        pass