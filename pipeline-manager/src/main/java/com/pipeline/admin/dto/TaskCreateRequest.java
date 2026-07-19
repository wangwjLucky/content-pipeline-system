package com.pipeline.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskCreateRequest {
    @NotNull(message = "topicId 不能为空")
    private Long topicId;

    @NotBlank(message = "标题不能为空")
    private String title;
}