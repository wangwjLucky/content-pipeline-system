package com.pipeline.admin.mq;

import lombok.Data;
import java.util.Map;

@Data
public class CallbackMessage {
    private Long taskId;
    private String service;
    private String status;
    private Map<String, Object> data;
    private String error;
}