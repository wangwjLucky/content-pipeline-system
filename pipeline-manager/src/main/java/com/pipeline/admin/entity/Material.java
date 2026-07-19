package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material")
@Schema(description = "素材")
public class Material extends BaseEntity {
    @Schema(description = "关联任务 ID")
    private Long taskId;

    @Schema(description = "关联分镜 ID")
    private Long storyboardId;

    @Schema(description = "素材类型: video/image/audio")
    private String type;

    @Schema(description = "使用的 AI 模型")
    private String model;

    @Schema(description = "素材文件地址")
    private String url;

    @Schema(description = "生成提示词")
    private String prompt;

    @Schema(description = "状态: PENDING/SUCCESS/FAILURE")
    private String status;
}