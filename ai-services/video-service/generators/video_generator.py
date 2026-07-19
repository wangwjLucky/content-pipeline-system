"""视频/图片生成器，调用 AI Gateway Keling provider 生成素材。"""

import httpx
from common.logging import setup_logging
from common.config import settings
from video_service.generators.base import BaseGenerator

logger = setup_logging("video_generator")


class VideoGenerator(BaseGenerator):
    """通过 AI Gateway 调用 Keling 生成视频/图片。"""

    def __init__(self, gateway_url: str = ""):
        self.gateway_url = gateway_url or settings.gateway_url

    def generate(self, prompt: str, **kwargs) -> dict:
        model = kwargs.get("model", "kling")
        logger.info(f"调用 AI Gateway 生成视频: model={model}, prompt={prompt[:50]}...")

        payload = {
            "model": model,
            "prompt": prompt,
            "task_type": kwargs.get("task_type", "video"),
        }

        try:
            with httpx.Client(timeout=300) as client:
                resp = client.post(f"{self.gateway_url}/ai/v1/generate", json=payload)
                resp.raise_for_status()
                result = resp.json()
                logger.info(f"视频生成成功: url={result.get('url', '')}")
                return result
        except Exception as e:
            logger.error(f"视频生成失败: {e}")
            raise