package com.pipeline.admin.event;

public class StoryboardReadyEvent extends PipelineEvent {
    public StoryboardReadyEvent(Long taskId) {
        super(taskId, "STORYBOARD", "GENERATING");
    }
}