"""Anthropic Claude 模型 Provider —— 真实 API 调用。

模型列表获取策略：
  1. 启动时通过 GET /v1/models 从 API 动态拉取（需配置 API Key）
  2. 拉取失败时回退到硬编码兜底列表
  3. API Key 未配置时模型列表为空
"""

import json
from typing import Any
import httpx
from gateway.providers.base import BaseProvider
from common.logging import setup_logging

logger = setup_logging("claude_provider")

# 兜底模型列表（API 拉取失败时使用）
_FALLBACK_MODELS = ["claude-sonnet-4-6", "claude-opus-4-8", "claude-haiku-4-5"]


class ClaudeProvider(BaseProvider):
    """Anthropic Claude API 调用（Messages API）"""

    def __init__(self, api_key: str = "", endpoint: str = ""):
        super().__init__()
        self.api_key = api_key
        self.endpoint = endpoint or "https://api.anthropic.com"
        self._http_client = httpx.AsyncClient(timeout=120)
        self._supported_models = list(_FALLBACK_MODELS) if api_key else []

    @property
    def name(self) -> str:
        return "anthropic"

    async def close(self) -> None:
        await self._http_client.aclose()

    async def refresh_models(self) -> None:
        """从 Anthropic API 实时拉取模型列表"""
        if not self.api_key:
            logger.info("Claude API Key 未配置，跳过模型列表加载")
            return

        logger.info("正在从 Anthropic API 拉取模型列表...")
        try:
            response = await self._http_client.get(
                f"{self.endpoint}/v1/models",
                headers={
                    "x-api-key": self.api_key,
                    "anthropic-version": "2023-06-01",
                    "Content-Type": "application/json",
                },
                timeout=30,
            )
            response.raise_for_status()
            data = response.json()

            # Anthropic 格式: {"data":[{"id":"claude-sonnet-4-6",...}]}
            models_raw = data.get("data", [])
            fetched = [m["id"] for m in models_raw if isinstance(m, dict) and m.get("id")]

            if fetched:
                self._supported_models = fetched
                logger.info(f"Claude 模型列表刷新成功: {len(fetched)} 个模型")
            else:
                logger.warning("Anthropic API 返回的模型列表为空，保留兜底列表")
        except Exception as e:
            logger.warning(f"Claude 模型列表拉取失败: {e}，使用兜底列表")

    async def chat(self, messages: list[dict[str, str]], **kwargs) -> str:
        model = kwargs.get("model", "claude-sonnet-4-6")
        temperature = kwargs.get("temperature", 0.7)
        max_tokens = kwargs.get("max_tokens", 4096)

        logger.info(f"Claude chat: model={model}, messages={len(messages)}")

        if not self.api_key:
            logger.warning("Claude API Key 未配置，返回模拟数据")
            return self._mock_chat(messages)

        # 将 OpenAI 格式消息转换为 Anthropic Messages 格式
        system_prompt = ""
        converted_messages = []
        for msg in messages:
            if msg["role"] == "system":
                system_prompt = msg["content"]
            else:
                converted_messages.append({
                    "role": "user" if msg["role"] == "user" else "assistant",
                    "content": msg["content"],
                })

        request_body = {
            "model": model,
            "max_tokens": max_tokens,
            "messages": converted_messages,
        }
        if system_prompt:
            request_body["system"] = system_prompt
        if temperature is not None:
            request_body["temperature"] = temperature

        try:
            response = await self._http_client.post(
                f"{self.endpoint}/v1/messages",
                headers={
                    "x-api-key": self.api_key,
                    "anthropic-version": "2023-06-01",
                    "Content-Type": "application/json",
                },
                json=request_body,
                timeout=120,
            )
            response.raise_for_status()
            data = response.json()
            content_blocks = data.get("content", [])
            content = content_blocks[0].get("text", "") if content_blocks else ""
            usage = data.get("usage", {})
            logger.info(
                f"Claude API 完成: model={model}, "
                f"input_tokens={usage.get('input_tokens', 'unknown')}, "
                f"output_tokens={usage.get('output_tokens', 'unknown')}"
            )
            return content
        except Exception as e:
            logger.error(f"Claude API 调用失败: {e}，回退到模拟数据")
            return self._mock_chat(messages)

    async def generate(self, prompt: str, **kwargs) -> Any:
        messages = [{"role": "user", "content": prompt}]
        result = await self.chat(messages, **kwargs)

        try:
            return json.loads(result)
        except (json.JSONDecodeError, TypeError):
            return {"content": result}

    def _mock_chat(self, messages: list[dict[str, str]]) -> str:
        last_msg = messages[-1]["content"] if messages else ""
        return f"[Claude] 这是对「{last_msg[:30]}」的模拟回复。"