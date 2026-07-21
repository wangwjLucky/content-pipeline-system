package com.pipeline.admin.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(PipelineEvent event) {
        log.debug("发布事件: {} (taskId={})", event.getClass().getSimpleName(), event.getTaskId());
        publisher.publishEvent(event);
    }
}