package com.pipeline.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublishCreateRequest {
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    @NotBlank(message = "平台不能为空")
    private String platform;

    private Long accountId;

    private String title;

    private String tags;

    private LocalDateTime scheduledAt;
}