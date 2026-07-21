package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pipeline_node_relation")
@Schema(description = "流水线节点关系")
public class PipelineNodeRelation extends BaseEntity {
    @Schema(description = "所属流水线 ID")
    private Long pipelineId;

    @Schema(description = "上游节点 ID")
    private Long fromNodeId;

    @Schema(description = "下游节点 ID")
    private Long toNodeId;

    @Schema(description = "条件表达式")
    private String conditionExpr;

    @Schema(description = "排序")
    private Integer sortOrder;
}