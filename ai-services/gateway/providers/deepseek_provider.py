"""DeepSeek Provider —— 通过 OpenAI 兼容接口调用 DeepSeek API。

模型列表获取策略：
  1. 启动时通过 GET /v1/models 从 API 动态拉取（需配置 API Key）
  2. 拉取失败时回退到硬编码兜底列表
  3. 兜底列表包含即将弃用的旧名称，确保兼容期内不影响已有调用方
"""

import json
from typing import Any
import httpx
from gateway.providers.base import BaseProvider
from common.logging import setup_logging

logger = setup_logging("deepseek_provider")

# 兜底模型列表（API 拉取失败时使用）
_FALLBACK_MODELS = [
    "deepseek-v4-flash",
    "deepseek-v4-pro",
    "deepseek-chat",       # 2026/07/24 弃用，对应 v4-flash 非思考模式
    "deepseek-reasoner",   # 2026/07/24 弃用，对应 v4-flash 思考模式
]


class DeepSeekProvider(BaseProvider):
    """DeepSeek API Provider（兼容 OpenAI 接口格式）"""

    def __init__(self, api_key: str = "", endpoint: str = "https://api.deepseek.com/v1"):
        super().__init__()
        self.api_key = api_key
        self.endpoint = endpoint.rstrip("/")
        self._http_client = httpx.AsyncClient(timeout=120)
        self._supported_models = list(_FALLBACK_MODELS)

    @property
    def name(self) -> str:
        return "deepseek"

    async def refresh_models(self) -> None:
        """从 DeepSeek API 实时拉取模型列表"""
        if not self.api_key:
            logger.info("DeepSeek API Key 未配置，使用兜底模型列表")
            return

        logger.info("正在从 DeepSeek API 拉取模型列表...")
        try:
            response = await self._http_client.get(
                f"{self.endpoint}/models",
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                },
                timeout=30,
            )
            response.raise_for_status()
            data = response.json()

            # OpenAI 兼容格式: {"object":"list","data":[{"id":"deepseek-v4-flash",...}]}
            models_raw = data.get("data", [])
            fetched = [m["id"] for m in models_raw if isinstance(m, dict) and m.get("id")]

            if fetched:
                # 同时保留旧模型名（兼容期）
                old_names = [m for m in _FALLBACK_MODELS if m not in fetched]
                self._supported_models = fetched + old_names
                logger.info(
                    f"DeepSeek 模型列表刷新成功: {len(fetched)} 个在线模型"
                    f"{' + ' + str(len(old_names)) + ' 个兼容旧名' if old_names else ''}"
                )
            else:
                logger.warning("DeepSeek API 返回的模型列表为空，保留兜底列表")
        except Exception as e:
            logger.warning(f"DeepSeek 模型列表拉取失败: {e}，使用兜底列表")

    async def chat(self, messages: list[dict], **kwargs) -> str:
        model = kwargs.get("model", "deepseek-chat")
        temperature = kwargs.get("temperature", 0.7)
        max_tokens = kwargs.get("max_tokens", 4096)

        logger.info(f"DeepSeek chat: model={model}, messages={len(messages)}")

        if not self.api_key:
            logger.warning("DeepSeek API Key 未配置，返回模拟数据")
            return self._mock_chat(messages)

        try:
            response = await self._http_client.post(
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
            choices = data.get("choices", [])
            if not choices:
                logger.error(f"DeepSeek API 返回的 choices 为空: {data}")
                return ""
            content = choices[0].get("message", {}).get("content", "")
            usage = data.get("usage", {})
            logger.info(
                f"DeepSeek chat 完成: input_tokens={usage.get('prompt_tokens', 'unknown')}, "
                f"output_tokens={usage.get('completion_tokens', 'unknown')}"
            )
            return content
        except Exception as e:
            logger.error(f"DeepSeek API 调用失败: {e}，回退到模拟数据")
            return self._mock_chat(messages)

    async def generate(self, prompt: str, **kwargs) -> Any:
        messages = [
            {"role": "system", "content": kwargs.get("system_prompt", "你是一个有帮助的助手。")},
            {"role": "user", "content": prompt},
        ]
        result = await self.chat(messages, **kwargs)

        # 尝试解析 JSON 响应
        try:
            return json.loads(result)
        except (json.JSONDecodeError, TypeError):
            return {"content": result}

    def _mock_chat(self, messages: list[dict[str, str]]) -> str:
        """模拟响应（无 API Key 时降级）"""
        last_msg = messages[-1]["content"] if messages else ""
        return f"[DeepSeek] 这是对「{last_msg[:30]}」的模拟回复。 (API Key 未配置)"