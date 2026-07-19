"""SenseNova（商汤大装置）Provider —— 接入 token.sensenova.cn API。

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

logger = setup_logging("sensenova_provider")

# 兜底模型列表（API 拉取失败时使用）
_FALLBACK_MODELS = [
    "sensenova-6.7-flash-lite",
    "sensenova-u1-fast",
    "deepseek-v4-flash",
    "glm-5.2",
]


class SenseNovaProvider(BaseProvider):
    """SenseNova / 商汤大装置 LLM API Provider

    平台地址: https://platform.sensenova.cn/
    API 文档: https://console.sensecore.cn/micro/help/docs/model-as-a-service/nova/
    API Key:  https://platform.sensenova.cn/ → 密钥管理 → API Key

    端点: https://token.sensenova.cn/v1/chat/completions（OpenAI 兼容格式）
    模型列表: GET https://token.sensenova.cn/v1/models

    支持的模型（通过 SenseNova 平台调用）:
      - sensenova-6.7-flash-lite    SenseNova 6.7 Flash-Lite（轻量多模态）
      - sensenova-u1-fast           SenseNova U1 Fast（信息图生成，不同端点）
      - deepseek-v4-flash           DeepSeek V4 Flash（通过 SenseNova 调用）
      - glm-5.2                     智谱 GLM-5.2（通过 SenseNova 调用）
    """

    def __init__(self, api_key: str = "", endpoint: str = ""):
        super().__init__()
        self.api_key = api_key
        self.endpoint = endpoint or "https://token.sensenova.cn/v1"
        self._http_client = httpx.AsyncClient(timeout=120)
        self._supported_models = list(_FALLBACK_MODELS) if api_key else []

    @property
    def name(self) -> str:
        return "sensenova"

    async def close(self) -> None:
        await self._http_client.aclose()

    async def refresh_models(self) -> None:
        """从 SenseNova API 实时拉取模型列表"""
        if not self.api_key:
            logger.info("SenseNova API Key 未配置，跳过模型列表加载")
            return

        logger.info("正在从 SenseNova API 拉取模型列表...")
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

            # OpenAI 兼容格式: {"object":"list","data":[{"id":"sensenova-6.7-flash-lite",...}]}
            models_raw = data.get("data", [])
            fetched = [m["id"] for m in models_raw if isinstance(m, dict) and m.get("id")]

            if fetched:
                self._supported_models = fetched
                logger.info(f"SenseNova 模型列表刷新成功: {len(fetched)} 个模型")
            else:
                logger.warning("SenseNova API 返回的模型列表为空，保留兜底列表")
        except Exception as e:
            logger.warning(f"SenseNova 模型列表拉取失败: {e}，使用兜底列表")

    async def chat(self, messages: list[dict[str, str]], **kwargs) -> str:
        """AI 对话（OpenAI 兼容格式）

        参数说明:
          - model: 模型名，默认 sensenova-6.7-flash-lite
          - temperature: [0, 2]，默认 0.8
          - max_tokens: [1, 65536]，默认 1024
          - top_p: (0, 1)，默认 0.7
          - stream: 是否流式，默认 False
          - stop: 停止词
          - frequency_penalty / presence_penalty
          - reasoning_effort: 推理强度（low/medium/high）
        """
        model = kwargs.get("model", "sensenova-6.7-flash-lite")
        temperature = kwargs.get("temperature", 0.8)
        max_tokens = kwargs.get("max_tokens", kwargs.get("max_new_tokens", 1024))
        top_p = kwargs.get("top_p", 0.7)
        stream = kwargs.get("stream", False)
        stop = kwargs.get("stop", None)
        reasoning_effort = kwargs.get("reasoning_effort", None)

        logger.info(f"SenseNova chat: model={model}, messages={len(messages)}")

        if not self.api_key:
            logger.warning("SenseNova API Key 未配置，返回模拟数据")
            return self._mock_chat(messages)

        try:
            return await self._chat_completions(
                model, messages, temperature, max_tokens,
                top_p, stream, stop, reasoning_effort,
            )
        except Exception as e:
            logger.error(f"SenseNova API 调用失败: {e}，回退到模拟数据")
            return self._mock_chat(messages)

    async def _chat_completions(
        self, model: str, messages: list[dict], temperature: float,
        max_tokens: int, top_p: float, stream: bool, stop: list[str] | None,
        reasoning_effort: str | None = None,
    ) -> str:
        """调用 SenseNova OpenAI 兼容接口 /v1/chat/completions"""
        request_body = {
            "model": model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
            "top_p": top_p,
            "stream": stream,
        }
        if stop:
            request_body["stop"] = stop
        if reasoning_effort:
            request_body["reasoning_effort"] = reasoning_effort
        request_body = {k: v for k, v in request_body.items() if v is not None}

        response = await self._http_client.post(
            f"{self.endpoint}/chat/completions",
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            },
            json=request_body,
            timeout=120,
        )
        response.raise_for_status()
        data = response.json()

        choices = data.get("choices", [])
        msg = choices[0].get("message", {}) if choices else {}
        # 不同模型回复字段不同：
        #   sensenova-6.7-flash-lite → reasoning
        #   glm-5.2                 → reasoning_content
        #   deepseek-v4-flash       → content + reasoning_content
        content = msg.get("content")
        if not content:
            content = msg.get("reasoning", "") or msg.get("reasoning_content", "")
        usage = data.get("usage", {})
        logger.info(
            f"SenseNova 完成: model={model}, "
            f"prompt_tokens={usage.get('prompt_tokens')}, "
            f"completion_tokens={usage.get('completion_tokens')}"
        )
        return content

    async def generate(self, prompt: str, **kwargs) -> Any:
        """通用生成"""
        system_prompt = kwargs.get("system_prompt", "你是一个有帮助的助手。")
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": prompt},
        ]
        result = await self.chat(messages, **kwargs)

        try:
            return json.loads(result)
        except (json.JSONDecodeError, TypeError):
            return {"content": result}

    def _mock_chat(self, messages: list[dict[str, str]]) -> str:
        """模拟响应（无 API Key 时降级）"""
        last_msg = messages[-1]["content"] if messages else ""
        if "脚本" in last_msg or "文案" in last_msg:
            return json.dumps({
                "title": "AI 技术前沿分享",
                "content": "大家好，今天我们来聊聊最新的 AI 技术发展。\n\n"
                           "SenseNova 平台提供了多种大模型能力，包括轻量级的 Flash-Lite 模型和强大的 DeepSeek V4 模型。\n\n"
                           "第一，Flash-Lite 模型速度快、成本低，适合日常对话场景。\n\n"
                           "第二，DeepSeek V4 Flash 支持 1M 上下文和工具调用，适合复杂任务。",
                "subtitle": "AI 技术正在快速发展。",
            }, ensure_ascii=False)
        return f"[SenseNova] 这是对「{last_msg[:30]}」的模拟回复。"