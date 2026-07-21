package com.pipeline.admin.event;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public abstract class PipelineEvent {
    private final Long taskId;
    private final String fromStatus;
    private final String toStatus;
    private final LocalDateTime timestamp;

    public PipelineEvent(Long taskId, String fromStatus, String toStatus) {
        this.taskId = taskId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.timestamp = LocalDateTime.now();
    }
}