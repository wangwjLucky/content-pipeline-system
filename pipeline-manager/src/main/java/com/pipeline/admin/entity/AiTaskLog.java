package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_task_log")
@Schema(description = "AI 任务日志")
public class AiTaskLog extends BaseEntity {
    @Schema(description = "关联任务 ID")
    private Long taskId;

    @Schema(description = "关联执行 ID")
    private Long runId;

    @Schema(description = "关联节点执行 ID")
    private Long nodeRunId;

    @Schema(description = "AI 供应商: claude/gpt/deepseek/keling/veo/doubao")
    private String provider;

    @Schema(description = "实际使用的模型")
    private String model;

    @Schema(description = "请求 Prompt")
    private String prompt;

    @Schema(description = "响应内容")
    private String response;

    @Schema(description = "输入 Token 数")
    private Integer promptTokens;

    @Schema(description = "输出 Token 数")
    private Integer completionTokens;

    @Schema(description = "响应耗时（毫秒）")
    private Integer latencyMs;

    @Schema(description = "估算费用")
    private BigDecimal cost;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "状态: PENDING/SUCCESS/FAILED")
    private String status;

    @Schema(description = "错误信息")
    private String errorMessage;
}