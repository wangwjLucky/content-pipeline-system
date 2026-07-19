"""RabbitMQ 连接管理与消费者基类。"""

import json
import threading
import pika
from typing import Callable
from common.config import settings


class RabbitMQClient:
    """RabbitMQ 连接与通信封装（线程安全）"""

    def __init__(self):
        self.connection: pika.BlockingConnection | None = None
        self.channel: pika.adapters.blocking_connection.BlockingChannel | None = None
        self._lock = threading.Lock()

    def connect(self):
        """建立连接"""
        with self._lock:
            if self.channel and self.channel.is_open:
                return self.channel
            credentials = pika.PlainCredentials(
                settings.rabbitmq_username, settings.rabbitmq_password
            )
            self.connection = pika.BlockingConnection(
                pika.ConnectionParameters(
                    host=settings.rabbitmq_host,
                    port=settings.rabbitmq_port,
                    credentials=credentials,
                    heartbeat=600,
                    blocked_connection_timeout=300,
                )
            )
            self.channel = self.connection.channel()
        return self.channel

    def close(self):
        """关闭连接"""
        with self._lock:
            if self.connection and self.connection.is_open:
                self.connection.close()
                self.connection = None
                self.channel = None

    def publish(self, queue: str, message: dict):
        """发送消息到指定队列"""
        with self._lock:
            if not self.channel or not self.channel.is_open:
                self.connect()
            self.channel.queue_declare(queue=queue, durable=True)
            self.channel.basic_publish(
                exchange="",
                routing_key=queue,
                body=json.dumps(message, ensure_ascii=False),
                properties=pika.BasicProperties(
                    delivery_mode=2,  # 持久化
                    content_type="application/json",
                ),
            )

    def consume(self, queue: str, callback: Callable):
        """消费队列消息（队列需已存在，由 Java 声明）"""
        if not self.channel:
            self.connect()
        # 使用 passive 声明：队列必须已存在，避免与 Java 声明的参数冲突
        try:
            self.channel.queue_declare(queue=queue, durable=True, passive=True)
        except Exception:
            # 如果 passive 失败，尝试非 passive 声明
            self.channel.queue_declare(queue=queue, durable=True)
        self.channel.basic_qos(prefetch_count=1)
        self.channel.basic_consume(
            queue=queue,
            on_message_callback=lambda ch, method, props, body: self._wrap_callback(
                ch, method, props, body, callback
            ),
        )
        print(f"[MQ] 开始消费队列: {queue}")
        self.channel.start_consuming()

    def _wrap_callback(self, ch, method, props, body, callback):
        """包装回调，自动 ACK"""
        try:
            message = json.loads(body)
            callback(message)
            ch.basic_ack(delivery_tag=method.delivery_tag)
        except Exception as e:
            print(f"[MQ] 处理消息失败: {e}")
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)