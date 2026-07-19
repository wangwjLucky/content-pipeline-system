package com.pipeline.admin.dto;

import lombok.Data;
import java.util.Map;

@Data
public class CallbackRequest {
    private Long taskId;
    private String service;
    private String status;
    private Map<String, Object> data;
    private String error;
}