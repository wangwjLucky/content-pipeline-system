package com.pipeline.admin.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineEventListener {

    @EventListener
    public void onScriptGenerated(ScriptGeneratedEvent event) {
        log.info("脚本生成完成事件: taskId={}", event.getTaskId());
        // 后续可在此扩展：发送通知、触发 Webhook 等
    }

    @EventListener
    public void onScriptReviewed(ScriptReviewedEvent event) {
        log.info("脚本审核通过事件: taskId={}", event.getTaskId());
    }

    @EventListener
    public void onStoryboardReady(StoryboardReadyEvent event) {
        log.info("分镜就绪事件: taskId={}", event.getTaskId());
    }

    @EventListener
    public void onMaterialReady(MaterialReadyEvent event) {
        log.info("素材生成完成事件: taskId={}", event.getTaskId());
    }

    @EventListener
    public void onVoiceCompleted(VoiceCompletedEvent event) {
        log.info("配音完成事件: taskId={}", event.getTaskId());
    }

    @EventListener
    public void onEditCompleted(EditCompletedEvent event) {
        log.info("剪辑完成事件: taskId={}", event.getTaskId());
    }
}