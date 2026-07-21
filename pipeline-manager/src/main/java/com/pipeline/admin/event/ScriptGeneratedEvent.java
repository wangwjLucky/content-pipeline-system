package com.pipeline.admin.event;

public class ScriptGeneratedEvent extends PipelineEvent {
    public ScriptGeneratedEvent(Long taskId) {
        super(taskId, "SCRIPTING", "SCRIPT_REVIEW");
    }
}