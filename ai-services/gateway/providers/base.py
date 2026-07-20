"""Provider 抽象基类。"""

from abc import ABC, abstractmethod
from typing import Any


class BaseProvider(ABC):
    """AI 模型 Provider 抽象基类"""

    def __init__(self) -> None:
        self._supported_models: list[str] = []
        self._model_type: str = "text"   # text / image / video / audio
        self._weight: int = 10            # 负载均衡权重

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
    def model_type(self) -> str:
        """模型类型: text / image / video / audio"""
        return self._model_type

    @property
    def weight(self) -> int:
        """负载均衡权重，值越大被选中的概率越高"""
        return self._weight

    @property
    def supported_models(self) -> list[str]:
        """支持的模型列表（可动态刷新）"""
        return list(self._supported_models)

    def get_model_type(self, model_id: str) -> str | None:
        """查询指定模型的具体类型，默认返回 Provider 的主类型"""
        return getattr(self, "_model_type_map", {}).get(model_id, self._model_type)

    async def close(self) -> None:
        """释放 Provider 资源（默认空操作，子类可覆盖）"""
        ...

    async def refresh_models(self) -> None:
        """从 API 刷新模型列表（默认空操作，子类可覆盖）"""
        ...