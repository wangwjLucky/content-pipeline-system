"""基于 LLM 的脚本生成器，调用 AI Gateway 生成脚本。"""

import json
import httpx
from common.logging import setup_logging
from common.config import settings
from script_service.generators.base import ScriptGenerator
from script_service.prompts import SCRIPT_SYSTEM_PROMPT

logger = setup_logging("llm_generator")


class LLMScriptGenerator(ScriptGenerator):
    """通过 AI Gateway 调用 LLM 生成脚本。"""

    def __init__(self, gateway_url: str = "", model: str = "gpt-4o"):
        self.gateway_url = gateway_url or settings.gateway_url
        self.model = model

    def generate(self, topic: str, **kwargs) -> dict:
        """调用 AI Gateway 生成脚本。"""
        model = kwargs.get("model", self.model)
        temperature = kwargs.get("temperature", 0.7)
        max_tokens = kwargs.get("max_tokens", 4096)

        messages = [
            {"role": "system", "content": SCRIPT_SYSTEM_PROMPT},
            {"role": "user", "content": f"请根据以下选题生成短视频脚本：\n\n{topic}"},
        ]

        logger.info(f"调用 AI Gateway 生成脚本: model={model}, topic={topic[:30]}...")

        try:
            content = self._call_gateway(model, messages, temperature, max_tokens)
            result = self._parse_response(content)
            logger.info(f"脚本生成成功: title={result.get('title', '')}")
            return result
        except Exception as e:
            logger.error(f"脚本生成失败: {e}")
            raise

    def _call_gateway(
        self, model: str, messages: list[dict], temperature: float, max_tokens: int
    ) -> str:
        """调用 AI Gateway 的 /ai/v1/chat 接口。"""
        payload = {
            "model": model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
        }
        with httpx.Client(timeout=120) as client:
            resp = client.post(f"{self.gateway_url}/ai/v1/chat", json=payload)
            resp.raise_for_status()
            data = resp.json()
            return data.get("content", "")

    def _parse_response(self, content: str) -> dict:
        """解析 LLM 返回的 JSON 字符串。"""
        cleaned = content.strip()
        # 移除可能的 markdown 代码块标记
        if cleaned.startswith("```"):
            cleaned = cleaned.split("\n", 1)[-1]
            cleaned = cleaned.rsplit("\n", 1)[0]
            if cleaned.endswith("```"):
                cleaned = cleaned[:-3]
        cleaned = cleaned.strip()

        try:
            result = json.loads(cleaned)
        except json.JSONDecodeError:
            logger.warning("JSON 解析失败，将原始内容作为正文")
            result = {"title": "AI 生成视频", "content": cleaned, "subtitle": ""}

        return {
            "title": result.get("title", "AI 生成视频"),
            "content": result.get("content", cleaned),
            "subtitle": result.get("subtitle", ""),
        }