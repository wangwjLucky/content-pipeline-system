package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pipeline_node")
@Schema(description = "流水线节点")
public class PipelineNode extends BaseEntity {
    @Schema(description = "所属流水线 ID")
    private Long pipelineId;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "节点编码: TOPIC/SCRIPT/STORYBOARD/GENERATE/VOICE/EDIT/REVIEW/PUBLISH")
    private String code;

    @Schema(description = "处理器 bean 名称")
    private String handler;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "是否需要人工审核")
    private Boolean requiredReview;

    @Schema(description = "是否支持迭代循环")
    private Boolean supportLoop;

    @Schema(description = "是否可并行")
    private Boolean parallel;

    @Schema(description = "是否支持重试")
    private Boolean retryable;

    @Schema(description = "超时时间（秒）")
    private Integer timeoutSeconds;
}