package com.pipeline.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScriptGenerateRequest {
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    @NotBlank(message = "topicTitle 不能为空")
    private String topicTitle;
}