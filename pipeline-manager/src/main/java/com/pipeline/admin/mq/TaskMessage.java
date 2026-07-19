package com.pipeline.admin.mq;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class TaskMessage {
    private String messageId;
    private Long taskId;
    private String action;
    private Map<String, Object> params;
    private String callbackUrl;
    private LocalDateTime timestamp;
}