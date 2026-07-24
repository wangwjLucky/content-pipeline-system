package com.pipeline.admin.service;

import com.pipeline.admin.entity.Task;
import com.pipeline.admin.entity.TaskEvent;
import com.pipeline.admin.event.*;
import com.pipeline.admin.mapper.TaskEventMapper;
import com.pipeline.admin.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskMapper taskMapper;
    private final TaskEventMapper taskEventMapper;
    private final AiService aiService;
    private final TaskStateMachine stateMachine;
    private final PipelineEventPublisher eventPublisher;

    @Override
    @Transactional
    public Task createTask(Long topicId, String title) {
        return createTask(topicId, title, "video");
    }

    @Override
    @Transactional
    public Task createTask(Long topicId, String title, String contentType) {
        Task task = new Task();
        task.setTopicId(topicId);
        task.setTitle(title);
        task.setContentType(contentType != null ? contentType : "video");
        task.setStatus("WAIT");
        task.setProgress(0);
        task.setRetryCount(0);
        task.setPriority(0);
        taskMapper.insert(task);

        recordEvent(task.getId(), null, "WAIT", "SYSTEM", "任务创建", "TASK_CREATED", null);

        updateStatus(task.getId(), "SCRIPTING", 10, null);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiService.sendScriptGenerate(task.getId(), title, Map.of("topic", title));
                log.info("任务创建完成，已发送脚本生成消息: taskId={}, topic={}", task.getId(), title);
            }
        });
        return taskMapper.selectById(task.getId());
    }

    @Override
    @Transactional
    public void updateStatus(Long taskId, String newStatus, Integer progress, String errorMessage) {
        updateStatus(taskId, newStatus, progress, errorMessage, null);
    }

    @Override
    @Transactional
    public void updateStatus(Long taskId, String newStatus, Integer progress, String errorMessage, String failReason) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            log.error("任务不存在: taskId={}", taskId);
            return;
        }

        String oldStatus = task.getStatus();
        String error = stateMachine.validate(oldStatus, newStatus);
        if (error != null) {
            throw new IllegalStateException(error);
        }

        Task update = new Task();
        update.setId(taskId);
        update.setStatus(newStatus);
        if (progress != null) update.setProgress(progress);
        update.setErrorMessage(errorMessage);
        update.setFailReason(failReason);
        update.setVersion(task.getVersion());
        int rows = taskMapper.updateById(update);
        if (rows == 0) {
            throw new IllegalStateException("任务状态更新失败，数据已被其他操作修改: taskId=" + taskId);
        }

        String eventType = resolveEventType(oldStatus, newStatus);
        recordEvent(taskId, oldStatus, newStatus, "SYSTEM", null, eventType, null);
        publishDomainEvent(taskId, oldStatus, newStatus);
        log.info("任务状态更新: taskId={}, {} → {} ({})", taskId, oldStatus, newStatus, eventType);
    }

    private void publishDomainEvent(Long taskId, String from, String to) {
        PipelineEvent event = null;
        if ("SCRIPT_REVIEW".equals(to)) {
            event = new ScriptGeneratedEvent(taskId);
        } else if ("STORYBOARD".equals(to)) {
            event = new ScriptReviewedEvent(taskId);
        } else if ("GENERATING".equals(to)) {
            event = new StoryboardReadyEvent(taskId);
        } else if ("VOICEOVER".equals(to)) {
            event = new MaterialReadyEvent(taskId);
        } else if ("EDITING".equals(to)) {
            event = new VoiceCompletedEvent(taskId);
        } else if ("REVIEW".equals(to) && !"GENERATING".equals(from)) {
            event = new EditCompletedEvent(taskId);
        }
        if (event != null) {
            eventPublisher.publish(event);
        }
    }

    @Override
    @Transactional
    public void cancelTask(Long taskId, String operator, String comment) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        String oldStatus = task.getStatus();
        String error = stateMachine.validate(oldStatus, "CANCELLED");
        if (error != null) {
            throw new IllegalStateException(error);
        }

        Task update = new Task();
        update.setId(taskId);
        update.setStatus("CANCELLED");
        update.setVersion(task.getVersion());
        int rows = taskMapper.updateById(update);
        if (rows == 0) {
            throw new IllegalStateException("取消任务失败，数据已被其他操作修改: taskId=" + taskId);
        }

        recordEvent(taskId, oldStatus, "CANCELLED", operator, comment, "TASK_CANCELLED", null);
        log.info("任务已取消: taskId={}, operator={}", taskId, operator);
    }

    @Override
    @Transactional
    public void retryTask(Long taskId, String operator) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        // 检查重试次数上限（最多 3 次）
        int currentRetryCount = task.getRetryCount() != null ? task.getRetryCount() : 0;
        if (currentRetryCount >= 3) {
            cancelTask(taskId, operator, "重试次数超过上限 (3)，任务已取消");
            log.warn("任务重试次数超过上限，已取消: taskId={}, retryCount={}", taskId, currentRetryCount);
            return;
        }

        String oldStatus = task.getStatus();
        String error = stateMachine.validate(oldStatus, "WAIT");
        if (error != null) {
            throw new IllegalStateException(error);
        }

        Task update = new Task();
        update.setId(taskId);
        update.setStatus("WAIT");
        update.setProgress(0);
        update.setErrorMessage(null);
        update.setFailReason(null);
        update.setRetryCount(currentRetryCount + 1);
        update.setVersion(task.getVersion());
        int rows = taskMapper.updateById(update);
        if (rows == 0) {
            throw new IllegalStateException("重试任务失败，数据已被其他操作修改: taskId=" + taskId);
        }

        recordEvent(taskId, oldStatus, "WAIT", operator, "重试", "TASK_RETRIED", null);

        updateStatus(taskId, "SCRIPTING", 10, null);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiService.sendScriptGenerate(task.getId(), task.getTitle(), Map.of("topic", task.getTitle()));
                log.info("任务已重试: taskId={}, operator={}", taskId, operator);
            }
        });
    }

    @Override
    public List<TaskEvent> getTimeline(Long taskId) {
        return taskEventMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskEvent>()
                        .eq(TaskEvent::getTaskId, taskId)
                        .orderByAsc(TaskEvent::getCreatedAt)
        );
    }

    private void recordEvent(Long taskId, String fromStatus, String toStatus, String operator, String comment) {
        recordEvent(taskId, fromStatus, toStatus, operator, comment, null, null);
    }

    private void recordEvent(Long taskId, String fromStatus, String toStatus, String operator, String comment, String eventType, String payload) {
        TaskEvent event = new TaskEvent();
        event.setTaskId(taskId);
        event.setEventType(eventType);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setOperator(operator);
        event.setComment(comment);
        event.setPayload(payload);
        taskEventMapper.insert(event);
    }

    private String resolveEventType(String from, String to) {
        if (to == null) return null;
        return switch (to) {
            case "SCRIPT_REVIEW" -> "SCRIPT_GENERATED";
            case "STORYBOARD" -> "SCRIPT_REVIEWED";
            case "GENERATING" -> "STORYBOARD_READY";
            case "VOICEOVER" -> "MATERIAL_READY";
            case "EDITING" -> "VOICE_COMPLETED";
            case "REVIEW" -> "EDIT_COMPLETED";
            case "ERROR" -> "TASK_ERRORED";
            default -> null;
        };
    }
}