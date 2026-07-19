"""分镜生成器抽象基类。"""

from abc import ABC, abstractmethod


class PromptGenerator(ABC):
    """分镜生成器基类，将脚本内容拆分为多个镜头。"""

    @abstractmethod
    def split(self, content: str, **kwargs) -> list[dict]:
        """根据脚本内容拆分为分镜列表，每个分镜包含 scene_type / character / action / environment / camera / ai_prompt 等字段。"""
        ...