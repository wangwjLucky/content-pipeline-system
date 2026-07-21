package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_run")
@Schema(description = "任务执行")
public class TaskRun extends BaseEntity {
    @Schema(description = "关联任务 ID")
    private Long taskId;

    @Schema(description = "执行序号")
    private Integer runNumber;

    @Schema(description = "状态: RUNNING/SUCCESS/FAILED")
    private String status;

    @Schema(description = "触发方式: AUTO/MANUAL/RETRY")
    private String triggeredBy;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime finishedAt;
}