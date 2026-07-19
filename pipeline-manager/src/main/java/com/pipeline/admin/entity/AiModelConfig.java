package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model_config")
@Schema(description = "AI 模型配置")
public class AiModelConfig extends BaseEntity {
    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "供应商: openai/kling/doubao")
    private String provider;

    @Schema(description = "API Key（加密存储）")
    private String apiKeyEncrypted;

    @Schema(description = "API 端点")
    private String endpoint;

    @Schema(description = "模型类型: text/image/video/audio")
    private String modelType;

    @Schema(description = "默认参数（JSON）")
    private String defaultParams;

    @Schema(description = "限流配置（JSON）")
    private String rateLimit;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "权重（负载均衡）")
    private Integer weight;
}