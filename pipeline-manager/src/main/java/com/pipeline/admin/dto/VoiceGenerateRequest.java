package com.pipeline.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class VoiceGenerateRequest {
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    private String voiceType;

    private Double speed;

    private Map<String, Object> params;
}