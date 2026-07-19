"""Provider 抽象基类。"""

from abc import ABC, abstractmethod
from typing import Any


class BaseProvider(ABC):
    """AI 模型 Provider 抽象基类"""

    @abstractmethod
    def chat(self, messages: list[dict[str, str]], **kwargs) -> str:
        """对话生成"""
        ...

    @abstractmethod
    def generate(self, prompt: str, **kwargs) -> Any:
        """通用生成"""
        ...

    @property
    @abstractmethod
    def name(self) -> str:
        """Provider 名称"""
        ...

    @property
    @abstractmethod
    def supported_models(self) -> list[str]:
        """支持的模型列表"""
        ...