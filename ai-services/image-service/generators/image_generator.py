"""图片生成引擎，通过 AI Gateway 调用文生图模型。"""

import httpx
from common.logging import setup_logging
from common.config import settings
from image_service.generators import BaseImageGenerator

logger = setup_logging("image_generator")


class ImageGenerator(BaseImageGenerator):
    """调用 AI Gateway 的 Keling/DALL-E 等模型生成图片。"""

    def __init__(self, gateway_url: str = ""):
        self.gateway_url = gateway_url or settings.gateway_url

    def generate(self, prompt: str, **kwargs) -> dict:
        model = kwargs.get("model", "keling")
        logger.info(f"调用 AI Gateway 图片生成: model={model}, prompt_len={len(prompt)}")

        payload = {
            "model": model,
            "prompt": prompt,
            "negative_prompt": kwargs.get("negative_prompt", ""),
            "image_size": kwargs.get("image_size", "1024x1024"),
            "num_images": kwargs.get("num_images", 1),
        }

        try:
            with httpx.Client(timeout=120) as client:
                resp = client.post(f"{self.gateway_url}/ai/v1/image/generate", json=payload)
                resp.raise_for_status()
                result = resp.json()
                logger.info(f"图片生成成功: url={result.get('image_url', '')}")
                return result
        except Exception as e:
            logger.error(f"图片生成失败: {e}")
            raise