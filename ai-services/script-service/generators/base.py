"""脚本生成器抽象基类。"""

from abc import ABC, abstractmethod


class ScriptGenerator(ABC):
    """脚本生成器基类，定义脚本生成接口。"""

    @abstractmethod
    def generate(self, topic: str, **kwargs) -> dict:
        """根据选题信息生成脚本，返回包含 title / content / subtitle 的字典。"""
        ...