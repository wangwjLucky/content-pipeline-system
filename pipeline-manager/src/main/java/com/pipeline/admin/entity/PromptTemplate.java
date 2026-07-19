package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prompt_template")
@Schema(description = "Prompt 模板")
public class PromptTemplate extends BaseEntity {
    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "模板类型: script/prompt/image/voice")
    private String type;

    @Schema(description = "模板内容")
    private String content;

    @Schema(description = "变量列表（逗号分隔）")
    private String variables;

    @Schema(description = "是否启用")
    private Boolean enabled;
}