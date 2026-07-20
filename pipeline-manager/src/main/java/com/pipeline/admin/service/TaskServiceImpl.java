package com.pipeline.admin.service;

import com.pipeline.admin.entity.Task;
import com.pipeline.admin.entity.TaskEvent;
import com.pipeline.admin.mapper.TaskEventMapper;
import com.pipeline.admin.mapper.TaskMapper;
import com.pipeline.admin.service.AiService;
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
        taskMapper.insert(task);

        // 记录初始事件
        recordEvent(task.getId(), null, "WAIT", "SYSTEM", "任务创建");

        // 推进到 SCRIPTING 状态，确保回调时能正确过渡到 SCRIPT_REVIEW
        updateStatus(task.getId(), "SCRIPTING", 10, null);

        // 事务提交后再发送 MQ 消息，避免回滚后消息已发出
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
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            log.error("任务不存在: taskId={}", taskId);
            return;
        }

        String oldStatus = task.getStatus();
        // 校验状态转换
        String error = stateMachine.validate(oldStatus, newStatus);
        if (error != null) {
            throw new IllegalStateException(error);
        }

        Task update = new Task();
        update.setId(taskId);
        update.setStatus(newStatus);
        if (progress != null) update.setProgress(progress);
        update.setErrorMessage(errorMessage);
        // 乐观锁：使用当前版本号防止并发覆盖
        update.setVersion(task.getVersion());
        int rows = taskMapper.updateById(update);
        if (rows == 0) {
            throw new IllegalStateException("任务状态更新失败，数据已被其他操作修改: taskId=" + taskId);
        }

        // 记录事件
        recordEvent(taskId, oldStatus, newStatus, "SYSTEM", null);
        log.info("任务状态更新: taskId={}, {} → {}", taskId, oldStatus, newStatus);
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

        recordEvent(taskId, oldStatus, "CANCELLED", operator, comment);
        log.info("任务已取消: taskId={}, operator={}", taskId, operator);
    }

    @Override
    @Transactional
    public void retryTask(Long taskId, String operator) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        String oldStatus = task.getStatus();
        String error = stateMachine.validate(oldStatus, "WAIT");
        if (error != null) {
            throw new IllegalStateException(error);
        }

        // 重置为 WAIT（清除错误状态）
        Task update = new Task();
        update.setId(taskId);
        update.setStatus("WAIT");
        update.setProgress(0);
        update.setErrorMessage(null);
        update.setVersion(task.getVersion());
        int rows = taskMapper.updateById(update);
        if (rows == 0) {
            throw new IllegalStateException("重试任务失败，数据已被其他操作修改: taskId=" + taskId);
        }

        recordEvent(taskId, oldStatus, "WAIT", operator, "重试");

        // 立即推进到 SCRIPTING，确保回调时状态机正确过渡
        updateStatus(taskId, "SCRIPTING", 10, null);

        // 事务提交后再重新发送脚本生成消息
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
        TaskEvent event = new TaskEvent();
        event.setTaskId(taskId);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setOperator(operator);
        event.setComment(comment);
        taskEventMapper.insert(event);
    }
}