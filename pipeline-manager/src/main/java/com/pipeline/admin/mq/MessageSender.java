package com.pipeline.admin.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSender {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void send(String queue, TaskMessage message) {
        try {
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID().toString());
            }
            String json = objectMapper.writeValueAsString(message);
            Message amqpMsg = MessageBuilder.withBody(json.getBytes(StandardCharsets.UTF_8))
                    .setContentType("application/json")
                    .build();
            rabbitTemplate.convertAndSend(queue, amqpMsg);
            log.info("MQ 消息已发送: queue={}, taskId={}, action={}", queue, message.getTaskId(), message.getAction());
        } catch (JsonProcessingException e) {
            log.error("MQ 消息序列化失败: {}", e.getMessage(), e);
        }
    }
}