package com.pipeline.admin.event;

public class ScriptReviewedEvent extends PipelineEvent {
    public ScriptReviewedEvent(Long taskId) {
        super(taskId, "SCRIPT_REVIEW", "STORYBOARD");
    }
}