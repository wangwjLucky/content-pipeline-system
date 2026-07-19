package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("storyboard")
@Schema(description = "分镜")
public class Storyboard extends BaseEntity {
    @Schema(description = "关联任务 ID")
    private Long taskId;

    @Schema(description = "镜头序号")
    private Integer sequence;

    @Schema(description = "镜头时长（秒）")
    private Integer duration;

    @Schema(description = "场景类型")
    private String sceneType;

    @Schema(description = "角色描述")
    private String character;

    @Schema(description = "动作描述")
    private String action;

    @Schema(description = "环境描述")
    private String environment;

    @Schema(description = "运镜方式")
    private String camera;

    @Schema(description = "灯光描述")
    private String lighting;

    @Schema(description = "风格描述")
    private String style;

    @Schema(description = "AI 生成提示词")
    private String aiPrompt;
}