"""DeepSeek Provider —— 通过 OpenAI 兼容接口调用 DeepSeek API。"""

import json
from typing import Any
import httpx
from gateway.providers.base import BaseProvider
from common.logging import setup_logging

logger = setup_logging("deepseek_provider")


class DeepSeekProvider(BaseProvider):
    """DeepSeek API Provider（兼容 OpenAI 接口格式）"""

    def __init__(self, api_key: str, endpoint: str = "https://api.deepseek.com/v1"):
        self.api_key = api_key
        self.endpoint = endpoint.rstrip("/")
        self._http_client = httpx.Client(timeout=120)

    @property
    def name(self) -> str:
        return "deepseek"

    @property
    def supported_models(self) -> list[str]:
        return ["deepseek-chat", "deepseek-reasoner"]

    def chat(self, messages: list[dict], **kwargs) -> str:
        model = kwargs.get("model", "deepseek-chat")
        temperature = kwargs.get("temperature", 0.7)
        max_tokens = kwargs.get("max_tokens", 4096)

        logger.info(f"DeepSeek chat: model={model}, messages={len(messages)}")

        response = self._http_client.post(
            f"{self.endpoint}/chat/completions",
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            },
            json={
                "model": model,
                "messages": messages,
                "temperature": temperature,
                "max_tokens": max_tokens,
            },
            timeout=120,
        )
        response.raise_for_status()
        data = response.json()
        content = data["choices"][0]["message"]["content"]
        logger.info(
            f"DeepSeek chat 完成: input_tokens={data['usage']['prompt_tokens']}, "
            f"output_tokens={data['usage']['completion_tokens']}"
        )
        return content

    def generate(self, prompt: str, **kwargs) -> Any:
        messages = [
            {"role": "system", "content": kwargs.get("system_prompt", "你是一个有帮助的助手。")},
            {"role": "user", "content": prompt},
        ]
        result = self.chat(messages, **kwargs)

        # 尝试解析 JSON 响应
        try:
            return json.loads(result)
        except (json.JSONDecodeError, TypeError):
            return {"content": result}
