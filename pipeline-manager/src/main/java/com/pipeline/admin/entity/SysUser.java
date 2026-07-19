package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@Schema(description = "系统用户")
public class SysUser extends BaseEntity {
    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码（BCrypt 加密）")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "角色 ID")
    private Long roleId;

    @Schema(description = "状态: ENABLED/DISABLED")
    private String status;
}