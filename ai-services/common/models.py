"""共享 Pydantic 模型定义。"""

from pydantic import BaseModel, Field, ConfigDict
from typing import Any, Optional


class TaskMessage(BaseModel):
    """MQ 任务消息（支持 Java camelCase 字段名）"""

    model_config = ConfigDict(populate_by_name=True, alias_generator=lambda s: "".join(
        "_" + c.lower() if c.isupper() else c for c in s
    ).lstrip("_"))

    message_id: str = Field(alias="messageId")
    task_id: int = Field(alias="taskId")
    action: str
    params: dict[str, Any] = {}
    callback_url: str = Field(default="", alias="callbackUrl")
    timestamp: str = ""


class CallbackBody(BaseModel):
    """回调 Java 接口的请求体"""
    taskId: int
    service: str
    status: str = "SUCCESS"
    data: Optional[dict[str, Any]] = None
    error: Optional[str] = None


class ChatRequest(BaseModel):
    """AI 聊天请求"""
    model: str = "gpt-4o"
    messages: list[dict[str, str]]
    temperature: float = 0.7
    max_tokens: int = 4096


class ChatResponse(BaseModel):
    """AI 聊天响应"""
    content: str
    model: str
    usage: Optional[dict[str, int]] = None


class GenerateRequest(BaseModel):
    """通用生成请求"""
    model: str
    prompt: str
    temperature: float = 0.7
    max_tokens: int = 4096


class GenerateResponse(BaseModel):
    """通用生成响应"""
    result: Any
    model: str