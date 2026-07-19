package com.pipeline.admin.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        return template;
    }

    // ========== 队列名称常量 ==========
    // 业务队列（已投入使用）
    public static final String QUEUE_SCRIPT_GENERATE = "pipeline.script.generate";
    public static final String QUEUE_PROMPT_GENERATE = "pipeline.prompt.generate";
    public static final String QUEUE_VIDEO_GENERATE = "pipeline.video.generate";
    public static final String QUEUE_IMAGE_GENERATE = "pipeline.image.generate";
    public static final String QUEUE_VOICE_GENERATE = "pipeline.voice.generate";
    public static final String QUEUE_FFMPEG_COMPILE = "pipeline.ffmpeg.compile";
    // 预留队列（用于未来版本，框架声明但尚未投入使用）
    public static final String QUEUE_TASK_CREATE = "pipeline.task.create";
    public static final String QUEUE_TASK_CALLBACK = "pipeline.task.callback";
    // 死信队列
    public static final String QUEUE_DLQ = "pipeline.dlq.task";

    // ========== 死信交换机 ==========
    public static final String DLX_EXCHANGE = "pipeline.dlx";
    public static final String DLX_ROUTING_KEY = "dlq.task";

    // ========== 普通队列（含死信配置） ==========
    private Queue buildQueue(String name) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);
        return QueueBuilder.durable(name).withArguments(args).build();
    }

    @Bean
    public Queue taskCreateQueue() { return buildQueue(QUEUE_TASK_CREATE); }

    @Bean
    public Queue scriptGenerateQueue() { return buildQueue(QUEUE_SCRIPT_GENERATE); }

    @Bean
    public Queue promptGenerateQueue() { return buildQueue(QUEUE_PROMPT_GENERATE); }

    @Bean
    public Queue videoGenerateQueue() { return buildQueue(QUEUE_VIDEO_GENERATE); }

    @Bean
    public Queue imageGenerateQueue() { return buildQueue(QUEUE_IMAGE_GENERATE); }

    @Bean
    public Queue voiceGenerateQueue() { return buildQueue(QUEUE_VOICE_GENERATE); }

    @Bean
    public Queue ffmpegCompileQueue() { return buildQueue(QUEUE_FFMPEG_COMPILE); }

    @Bean
    public Queue taskCallbackQueue() { return buildQueue(QUEUE_TASK_CALLBACK); }

    // ========== 死信队列 ==========
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqQueue()).to(dlxExchange()).with(DLX_ROUTING_KEY);
    }
}