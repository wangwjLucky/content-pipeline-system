"""回调 Java 后端的公共函数。"""

import httpx
from common.config import settings
from common.logging import setup_logging
from common.models import CallbackBody
from typing import Optional

logger = setup_logging("callback")


def send_callback(
    callback_url: str,
    task_id: int,
    service: str,
    status: str = "SUCCESS",
    data: Optional[dict] = None,
    error: Optional[str] = None,
    timeout: float = 30,
) -> bool:
    """向 Java 后端发送回调请求，自动注入认证令牌。"""
    body = CallbackBody(
        taskId=task_id,
        service=service,
        status=status,
        data=data,
        error=error,
    )
    headers = {"X-Callback-Token": settings.callback_token}
    try:
        with httpx.Client(timeout=timeout) as client:
            resp = client.post(callback_url, json=body.model_dump(), headers=headers)
            resp.raise_for_status()
            return True
    except Exception as e:
        logger.error(f"发送回调失败: taskId={task_id}, service={service}, error={e}")
        return False