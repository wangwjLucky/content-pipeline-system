package com.pipeline.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CompileRequest {
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    private Map<String, Object> params;
}