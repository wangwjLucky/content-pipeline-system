"""Provider 抽象基类。"""

from abc import ABC, abstractmethod
from typing import Any


class BaseProvider(ABC):
    """AI 模型 Provider 抽象基类"""

    def __init__(self) -> None:
        self._supported_models: list[str] = []

    @abstractmethod
    async def chat(self, messages: list[dict[str, str]], **kwargs) -> str:
        """对话生成（异步）"""
        ...

    @abstractmethod
    async def generate(self, prompt: str, **kwargs) -> Any:
        """通用生成（异步）"""
        ...

    @property
    @abstractmethod
    def name(self) -> str:
        """Provider 名称"""
        ...

    @property
    def supported_models(self) -> list[str]:
        """支持的模型列表（可动态刷新）"""
        return self._supported_models

    async def refresh_models(self) -> None:
        """从 API 刷新模型列表（默认空操作，子类可覆盖）"""
        ...