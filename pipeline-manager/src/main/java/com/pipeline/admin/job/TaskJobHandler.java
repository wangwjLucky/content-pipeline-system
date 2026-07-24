package com.pipeline.admin.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipeline.admin.entity.PublishLog;
import com.pipeline.admin.entity.Task;
import com.pipeline.admin.mapper.PublishLogMapper;
import com.pipeline.admin.mapper.TaskMapper;
import com.pipeline.admin.service.TaskService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskJobHandler {

    private final TaskMapper taskMapper;
    private final TaskService taskService;
    private final PublishLogMapper publishLogMapper;

    @XxlJob("pendingTaskScanner")
    public void pendingTaskScanner() {
        log.info("开始扫描待处理任务...");
        // 按优先级从高到低（数值越大优先级越高），每次最多处理 50 个
        List<Task> pending = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, "WAIT")
                        .orderByDesc(Task::getPriority)
                        .last("LIMIT 50"));
        int count = 0;
        for (Task task : pending) {
            try {
                taskService.updateStatus(task.getId(), "SCRIPTING", 10, null);
                count++;
            } catch (Exception e) {
                log.warn("任务推进失败: taskId={}, error={}", task.getId(), e.getMessage());
            }
        }
        XxlJobHelper.handleSuccess("扫描完成，处理 " + count + " 个任务");
    }

    @XxlJob("retryQueueProcessor")
    public void retryQueueProcessor() {
        log.info("开始处理重试队列...");
        // 重试所有失败原因的任务（不限 SCRIPT_FAILED），每次最多 20 个
        List<Task> errored = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getStatus, "ERROR")
                        .isNotNull(Task::getFailReason)
                        .last("LIMIT 20"));
        int count = 0;
        for (Task task : errored) {
            try {
                taskService.retryTask(task.getId(), "SYSTEM");
                count++;
            } catch (Exception e) {
                log.warn("重试失败: taskId={}, error={}", task.getId(), e.getMessage());
            }
        }
        XxlJobHelper.handleSuccess("重试队列处理完成，重试 " + count + " 个任务");
    }

    @XxlJob("timeoutMonitor")
    public void timeoutMonitor() {
        log.info("开始检测超时任务...");
        int timeoutCount = 0;
        List<String> runningStatuses = List.of("SCRIPTING", "GENERATING", "VOICEOVER", "EDITING");
        for (String status : runningStatuses) {
            List<Task> tasks = taskMapper.selectList(
                    new LambdaQueryWrapper<Task>()
                            .eq(Task::getStatus, status));
            for (Task task : tasks) {
                if (task.getCreatedAt() != null &&
                        task.getCreatedAt().plusHours(2).isBefore(LocalDateTime.now())) {
                    log.warn("超时任务: taskId={}, status={}, 创建于 {}", task.getId(), status, task.getCreatedAt());
                    timeoutCount++;
                }
            }
        }
        XxlJobHelper.handleSuccess("超时检测完成，发现 " + timeoutCount + " 个超时任务");
    }

    @XxlJob("tempMaterialCleaner")
    public void tempMaterialCleaner() {
        log.info("开始清理过期临时素材...");
        XxlJobHelper.handleSuccess("清理完成");
    }

    @XxlJob("publishScheduler")
    public void publishScheduler() {
        log.info("开始检查定时发布...");
        LocalDateTime now = LocalDateTime.now();
        List<PublishLog> pending = publishLogMapper.selectList(
                new LambdaQueryWrapper<PublishLog>()
                        .eq(PublishLog::getStatus, "PENDING")
                        .isNotNull(PublishLog::getScheduledAt)
                        .apply("scheduled_at <= {0}", now));
        int success = 0;
        for (PublishLog pub : pending) {
            try {
                pub.setStatus("PUBLISHED");
                pub.setPublishedAt(LocalDateTime.now());
                publishLogMapper.updateById(pub);
                taskService.updateStatus(pub.getTaskId(), "PUBLISHED", 100, null);
                success++;
            } catch (Exception e) {
                log.error("定时发布失败: publishId={}", pub.getId(), e);
                pub.setStatus("FAILURE");
                pub.setErrorMessage(e.getMessage());
                publishLogMapper.updateById(pub);
            }
        }
        XxlJobHelper.handleSuccess("定时发布检查完成，发布 " + success + " 个");
    }
}