package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pipeline_definition")
@Schema(description = "流水线定义")
public class PipelineDefinition extends BaseEntity {
    @Schema(description = "流水线名称")
    private String name;

    @Schema(description = "编码: video/text/image/image_text")
    private String code;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "是否启用")
    private Boolean enabled;
}