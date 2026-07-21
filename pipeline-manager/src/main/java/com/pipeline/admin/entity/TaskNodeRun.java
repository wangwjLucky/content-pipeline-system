package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_node_run")
@Schema(description = "节点执行")
public class TaskNodeRun extends BaseEntity {
    @Schema(description = "关联执行 ID")
    private Long runId;

    @Schema(description = "节点编码: SCRIPT/STORYBOARD/GENERATE/VOICE/EDIT/REVIEW/PUBLISH")
    private String nodeCode;

    @Schema(description = "状态: PENDING/RUNNING/SUCCESS/FAILED")
    private String status;

    @Schema(description = "处理器")
    private String handler;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "最大重试次数")
    private Integer maxRetries;

    @Schema(description = "输入参数快照")
    private String inputSnapshot;

    @Schema(description = "输出结果快照")
    private String outputSnapshot;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime finishedAt;

    @Schema(description = "错误信息")
    private String errorMessage;
}