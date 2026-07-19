"""基于 LLM 的分镜生成器，调用 AI Gateway 拆分脚本为分镜。"""

import json
import httpx
from common.logging import setup_logging
from common.config import settings
from prompt_service.generators.base import PromptGenerator

logger = setup_logging("llm_prompt_generator")

PROMPT_SYSTEM_PROMPT = """你是一个专业的短视频分镜师。请将以下脚本拆分为分镜，每个分镜包含以下字段：
- scene_type: 场景类型（如：实拍、动画、文字、空镜）
- character: 角色描述
- action: 动作描述
- environment: 环境描述
- camera: 运镜方式（如：推、拉、摇、移）
- duration: 镜头时长（秒），默认5秒
- ai_prompt: 用于 AI 视频/图片生成的提示词（英文，详细描述画面）

以 JSON 数组格式返回，每个元素包含上述字段。
例如：[{"scene_type": "动画", "character": "", "action": "文字出现", "environment": "深色背景", "camera": "固定", "duration": 5, "ai_prompt": "Dark background with text appearing..."}]
"""


class LLMPromptGenerator(PromptGenerator):
    """通过 AI Gateway 调用 LLM 拆分脚本为分镜。"""

    def __init__(self, gateway_url: str = "", model: str = "gpt-4o"):
        self.gateway_url = gateway_url or settings.gateway_url
        self.model = model

    def split(self, content: str, **kwargs) -> list[dict]:
        model = kwargs.get("model", self.model)
        temperature = kwargs.get("temperature", 0.3)
        max_tokens = kwargs.get("max_tokens", 4096)

        messages = [
            {"role": "system", "content": PROMPT_SYSTEM_PROMPT},
            {"role": "user", "content": f"请将以下脚本拆分为分镜：\n\n{content}"},
        ]

        logger.info(f"调用 AI Gateway 拆分分镜: model={model}, content_len={len(content)}")
        payload = {
            "model": model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
        }

        try:
            with httpx.Client(timeout=120) as client:
                resp = client.post(f"{self.gateway_url}/ai/v1/chat", json=payload)
                resp.raise_for_status()
                data = resp.json()
                result = self._parse_response(data.get("content", ""))
                logger.info(f"分镜拆分成功: count={len(result)}")
                return result
        except Exception as e:
            logger.error(f"分镜拆分失败: {e}")
            raise

    def _parse_response(self, content: str) -> list[dict]:
        cleaned = content.strip()
        if cleaned.startswith("```"):
            cleaned = cleaned.split("\n", 1)[-1]
            cleaned = cleaned.rsplit("\n", 1)[0]
            if cleaned.endswith("```"):
                cleaned = cleaned[:-3]
        cleaned = cleaned.strip()
        try:
            storyboards = json.loads(cleaned)
            if isinstance(storyboards, list):
                return storyboards
            return []
        except json.JSONDecodeError:
            logger.warning("JSON 解析失败，返回默认分镜")
            return [{"scene_type": "文字", "action": "展示内容", "environment": "纯色背景",
                      "camera": "固定", "duration": 10, "ai_prompt": content}]