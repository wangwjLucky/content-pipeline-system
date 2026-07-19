package com.pipeline.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScriptReviewRequest {
    @NotBlank(message = "审核操作不能为空")
    private String action; // approve | reject

    private Long reviewerId;

    private String reason;
}