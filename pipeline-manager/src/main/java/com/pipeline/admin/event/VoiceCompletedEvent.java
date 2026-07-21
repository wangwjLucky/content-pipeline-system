package com.pipeline.admin.event;

public class VoiceCompletedEvent extends PipelineEvent {
    public VoiceCompletedEvent(Long taskId) {
        super(taskId, "VOICEOVER", "EDITING");
    }
}