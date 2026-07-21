package com.pipeline.admin.event;

public class MaterialReadyEvent extends PipelineEvent {
    public MaterialReadyEvent(Long taskId) {
        super(taskId, "GENERATING", "VOICEOVER");
    }
}