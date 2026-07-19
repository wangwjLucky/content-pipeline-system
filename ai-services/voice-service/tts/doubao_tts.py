"""Doubao TTS 引擎，调用 AI Gateway 的 Doubao provider 生成配音。"""

import httpx
from common.logging import setup_logging
from common.config import settings
from voice_service.tts.base import TTSEngine

logger = setup_logging("doubao_tts")


class DoubaoTTS(TTSEngine):
    """通过 AI Gateway 调用豆包 TTS 生成配音。"""

    def __init__(self, gateway_url: str = ""):
        self.gateway_url = gateway_url or settings.gateway_url

    def synthesize(self, text: str, **kwargs) -> dict:
        voice_type = kwargs.get("voice_type", "zh_female_01")
        logger.info(f"调用 AI Gateway TTS: voice_type={voice_type}, text_len={len(text)}")

        payload = {
            "task_id": kwargs.get("task_id", 0),
            "text": text,
            "voice_type": voice_type,
        }

        try:
            with httpx.Client(timeout=120) as client:
                resp = client.post(f"{self.gateway_url}/ai/v1/voice/generate", json=payload)
                resp.raise_for_status()
                result = resp.json()
                logger.info(f"TTS 生成成功: url={result.get('voice_url', '')}")
                return result
        except Exception as e:
            logger.error(f"TTS 生成失败: {e}")
            raise