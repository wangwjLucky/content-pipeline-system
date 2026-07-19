"""OpenAI 模型 Provider —— 真实 API 调用（兼容 OpenAI 接口格式）。"""

import json
from typing import Any
import httpx
from gateway.providers.base import BaseProvider
from common.logging import setup_logging

logger = setup_logging("openai_provider")


class OpenAIProvider(BaseProvider):
    """OpenAI / 兼容 API 的 LLM 调用（支持 OpenAI、DeepSeek 等兼容接口）"""

    def __init__(self, api_key: str = "", endpoint: str = ""):
        super().__init__()
        self.api_key = api_key
        self.endpoint = endpoint or "https://api.openai.com/v1"
        self._http_client = httpx.AsyncClient(timeout=120)
        self._supported_models = ["gpt-4o", "gpt-4.1", "gpt-4o-mini", "gpt-3.5-turbo"]

    @property
    def name(self) -> str:
        return "openai"

    async def close(self) -> None:
        await self._http_client.aclose()

    async def chat(self, messages: list[dict[str, str]], **kwargs) -> str:
        model = kwargs.get("model", "gpt-4o")
        temperature = kwargs.get("temperature", 0.7)
        max_tokens = kwargs.get("max_tokens", 4096)

        logger.info(f"OpenAI chat: model={model}, messages={len(messages)}")

        if not self.api_key:
            logger.warning("OpenAI API Key 未配置，返回模拟数据")
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
            content = choices[0].get("message", {}).get("content", "") if choices else ""
            usage = data.get("usage", {})
            logger.info(
                f"OpenAI chat 完成: input_tokens={usage.get('prompt_tokens', 'unknown')}, "
                f"output_tokens={usage.get('completion_tokens', 'unknown')}"
            )
            return content
        except Exception as e:
            logger.error(f"OpenAI API 调用失败: {e}，回退到模拟数据")
            return self._mock_chat(messages)

    async def generate(self, prompt: str, **kwargs) -> Any:
        messages = [
            {"role": "system", "content": kwargs.get("system_prompt", "你是一个有帮助的助手。")},
            {"role": "user", "content": prompt},
        ]
        result = await self.chat(messages, **kwargs)

        try:
            return json.loads(result)
        except (json.JSONDecodeError, TypeError):
            return {"content": result}

    def _mock_chat(self, messages: list[dict[str, str]]) -> str:
        """模拟 LLM 响应（无 API Key 时降级使用）"""
        last_msg = messages[-1]["content"] if messages else ""
        if "脚本" in last_msg or "文案" in last_msg:
            return """{
  "title": "AI 改变世界的十种方式",
  "content": "大家好，今天我们来聊聊 AI 正在如何改变我们的世界。\\n\\n第一，AI 正在重塑教育方式。个性化学习让每个学生都能按照自己的节奏学习。\\n\\n第二，AI 在医疗领域大放异彩。从疾病诊断到药物研发，AI 正在帮助医生做出更准确的判断。\\n\\n第三，自动驾驶技术正在改变出行方式。\\n\\n...（此处省略更多内容）",
  "subtitle": "AI 正在重塑教育、医疗、出行等各个领域。"
}"""
        return f"这是对「{last_msg[:30]}」的模拟回复。"