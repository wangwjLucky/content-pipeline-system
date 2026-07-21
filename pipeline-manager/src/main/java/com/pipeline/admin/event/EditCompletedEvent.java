package com.pipeline.admin.event;

public class EditCompletedEvent extends PipelineEvent {
    public EditCompletedEvent(Long taskId) {
        super(taskId, "EDITING", "REVIEW");
    }
}