package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_event")
@Schema(description = "任务事件（状态机时间线）")
public class TaskEvent extends BaseEntity {
    @Schema(description = "关联任务 ID")
    private Long taskId;

    @Schema(description = "事件类型: TASK_CREATED/SCRIPT_GENERATED/SCRIPT_REVIEWED/STORYBOARD_READY/MATERIAL_READY/VOICE_COMPLETED/EDIT_COMPLETED/TASK_CANCELLED/TASK_RETRIED/TASK_ERRORED")
    private String eventType;

    @Schema(description = "来源状态")
    private String fromStatus;

    @Schema(description = "目标状态")
    private String toStatus;

    @Schema(description = "操作人")
    private String operator;

    @Schema(description = "备注说明")
    private String comment;

    @Schema(description = "事件数据（JSON）")
    private String payload;
}