package com.pipeline.admin.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipeline.admin.entity.PublishLog;
import com.pipeline.admin.mapper.PublishLogMapper;
import com.pipeline.admin.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Spring 内置定时任务，用于定时发布等功能。
 * 当 XXL-JOB 启用时（xxl.job.enabled=true），可通过配置关闭此调度器避免冲突。
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "xxl.job.enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class SchedulingConfig {
    private final PublishLogMapper publishLogMapper;
    private final TaskService taskService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processScheduledPublishes() {
        LocalDateTime now = LocalDateTime.now();
        var tasks = publishLogMapper.selectList(
                new LambdaQueryWrapper<PublishLog>()
                        .eq(PublishLog::getStatus, "PENDING")
                        .isNotNull(PublishLog::getScheduledAt)
                        .apply("scheduled_at <= {0}", now));

        for (PublishLog task : tasks) {
            try {
                task.setStatus("PUBLISHED");
                task.setPublishedAt(LocalDateTime.now());
                publishLogMapper.updateById(task);
                taskService.updateStatus(task.getTaskId(), "PUBLISHED", 100, null);
                log.info("定时发布完成: publishId={}, taskId={}", task.getId(), task.getTaskId());
            } catch (Exception e) {
                log.error("定时发布失败: publishId={}", task.getId(), e);
                task.setStatus("FAILURE");
                task.setErrorMessage(e.getMessage());
                publishLogMapper.updateById(task);
            }
        }
    }
}