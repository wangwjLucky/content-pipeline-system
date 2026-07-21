package com.pipeline.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.pipeline.admin.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("version_graph")
@Schema(description = "版本 DAG")
public class VersionGraph extends BaseEntity {
    @Schema(description = "实体类型: SCRIPT/STORYBOARD/MATERIAL/VIDEO")
    private String entityType;

    @Schema(description = "实体 ID")
    private Long entityId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "父版本 ID")
    private Long parentVersionId;

    @Schema(description = "版本快照（JSON）")
    private String snapshot;

    @Schema(description = "创建人")
    private Long createdBy;
}