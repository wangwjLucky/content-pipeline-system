package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task")
@Schema(description = "任务")
public class Task extends BaseEntity {
    @Schema(description = "关联选题 ID")
    private Long topicId;

    @Schema(description = "任务标题")
    private String title;

    @Schema(description = "关联脚本 ID")
    private Long scriptId;

    @Schema(description = "内容产出方式: video/text/image/image_text")
    @TableField("content_type")
    private String contentType;

    @Schema(description = "任务状态: WAIT/SCRIPTING/SCRIPT_REVIEW/STORYBOARD/GENERATING/VOICEOVER/EDITING/REVIEW/READY/PUBLISHED/CANCELLED/ERROR")
    private String status;

    @Schema(description = "进度百分比 0-100")
    private Integer progress;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "失败原因归类: SCRIPT_FAILED/MATERIAL_FAILED/VOICE_FAILED/EDIT_FAILED/PUBLISH_FAILED")
    private String failReason;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "优先级: 0-100，越高越优先处理")
    private Integer priority;

    @Schema(description = "创建人")
    private Long createdBy;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;
}