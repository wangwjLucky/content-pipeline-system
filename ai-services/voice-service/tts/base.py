"""TTS 引擎抽象基类。"""

from abc import ABC, abstractmethod


class TTSEngine(ABC):
    """TTS 引擎基类，定义语音合成接口。"""

    @abstractmethod
    def synthesize(self, text: str, **kwargs) -> dict:
        """将文本合成为语音，返回包含 url / duration 等信息的字典。"""
        ...