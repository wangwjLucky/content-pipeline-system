package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("topic")
@Schema(description = "选题")
public class Topic extends BaseEntity {
    @Schema(description = "选题标题")
    private String title;

    @Schema(description = "来源: MANUAL/AUTO/HOT")
    private String source;

    @Schema(description = "来源链接")
    private String sourceUrl;

    @Schema(description = "热度分值")
    private Integer hotScore;

    @Schema(description = "是否自动抓取")
    private Boolean isAuto;

    @Schema(description = "状态: PENDING/PROCESSING/COMPLETED")
    private String status;

    @Schema(description = "创建人")
    private Long createdBy;
}