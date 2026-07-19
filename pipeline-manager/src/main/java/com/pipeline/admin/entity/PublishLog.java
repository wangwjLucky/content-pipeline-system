package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("publish_log")
@Schema(description = "发布日志")
public class PublishLog extends BaseEntity {
    @Schema(description = "关联任务 ID")
    private Long taskId;

    @Schema(description = "发布平台: douyin/kuaishou/xiaohongshu")
    private String platform;

    @Schema(description = "发布账号 ID")
    private Long accountId;

    @Schema(description = "发布标题")
    private String title;

    @Schema(description = "封面图地址")
    private String coverUrl;

    @Schema(description = "标签（逗号分隔）")
    private String tags;

    @Schema(description = "定时发布时间")
    private LocalDateTime scheduledAt;

    @Schema(description = "实际发布时间")
    private LocalDateTime publishedAt;

    @Schema(description = "状态: PENDING/PUBLISHED/FAILURE")
    private String status;

    @Schema(description = "平台视频 ID")
    private String platformVideoId;

    @Schema(description = "错误信息")
    private String errorMessage;
}