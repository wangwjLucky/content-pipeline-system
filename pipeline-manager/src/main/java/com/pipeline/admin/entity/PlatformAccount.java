package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("platform_account")
@Schema(description = "平台账号")
public class PlatformAccount extends BaseEntity {
    @Schema(description = "平台: douyin/kuaishou/xiaohongshu")
    private String platform;

    @Schema(description = "账号名称")
    private String accountName;

    @Schema(description = "Cookies（加密存储）")
    private String cookiesEncrypted;

    @Schema(description = "状态: ENABLED/DISABLED")
    private String status;
}