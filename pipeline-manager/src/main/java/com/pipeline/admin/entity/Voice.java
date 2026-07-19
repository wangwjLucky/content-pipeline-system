package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("voice")
@Schema(description = "配音")
public class Voice extends BaseEntity {
    @Schema(description = "关联任务 ID")
    private Long taskId;

    @Schema(description = "配音类型: doubao/azure/aliyun")
    private String voiceType;

    @Schema(description = "配音文件地址")
    private String voiceUrl;

    @Schema(description = "语速倍数")
    private java.math.BigDecimal speed;

    @Schema(description = "时长（秒）")
    private Integer duration;

    @Schema(description = "状态: PENDING/SUCCESS/FAILURE")
    private String status;
}