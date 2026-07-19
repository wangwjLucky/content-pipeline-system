package com.pipeline.admin.controller;

import com.pipeline.admin.common.OperationLog;
import com.pipeline.admin.common.Result;
import com.pipeline.admin.entity.AiModelConfig;
import com.pipeline.admin.service.AiModelService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai-models")
@RequiredArgsConstructor
public class AiModelController {
    private final AiModelService aiModelService;
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @GetMapping
    public Result<Page<AiModelConfig>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) String provider) {
        return Result.success(aiModelService.list(page, size, provider));
    }

    @GetMapping("/{id}")
    public Result<AiModelConfig> get(@PathVariable Long id) {
        AiModelConfig config = aiModelService.get(id);
        return config != null ? Result.success(config) : Result.error(404, "模型配置不存在");
    }

    @OperationLog(module = "模型配置", action = "创建")
    @PostMapping
    public Result<AiModelConfig> create(@RequestBody AiModelConfig config) {
        return Result.success(aiModelService.create(config));
    }

    @OperationLog(module = "模型配置", action = "更新")
    @PutMapping("/{id}")
    public Result<AiModelConfig> update(@PathVariable Long id, @RequestBody AiModelConfig config) {
        return Result.success(aiModelService.update(id, config));
    }

    @OperationLog(module = "模型配置", action = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        aiModelService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/test")
    public Result<String> test(@PathVariable Long id) {
        AiModelConfig config = aiModelService.get(id);
        if (config == null) return Result.error(404, "模型配置不存在");
        return Result.success("连接测试成功: " + config.getModelName());
    }

    @PostMapping("/test-mq")
    public Result<String> testMq() {
        if (rabbitTemplate == null) {
            return Result.error(500, "RabbitMQ 未配置");
        }
        try {
            String msg = "{\"messageId\":\"test\",\"taskId\":1,\"action\":\"test\"}";
            rabbitTemplate.convertAndSend("pipeline.test.queue", msg);
            return Result.success("MQ 消息发送成功");
        } catch (Exception e) {
            return Result.error(500, "MQ 发送失败: " + e.getMessage());
        }
    }
}