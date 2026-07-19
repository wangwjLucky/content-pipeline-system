package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("script")
@Schema(description = "脚本")
public class Script extends BaseEntity {
    @Schema(description = "关联选题 ID")
    private Long topicId;

    @Schema(description = "关联任务 ID")
    private Long taskId;

    @Schema(description = "脚本标题")
    private String title;

    @Schema(description = "脚本正文")
    private String content;

    @Schema(description = "字幕文本")
    private String subtitle;

    @Schema(description = "使用的 Prompt 模板 ID")
    private Long promptTemplateId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "状态: PENDING_REVIEW/APPROVED/REJECTED")
    private String status;

    @Schema(description = "创建人")
    private Long createdBy;
}