package com.pipeline.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipeline.admin.entity.PlatformAccount;
import com.pipeline.admin.entity.PublishLog;
import com.pipeline.admin.entity.Task;
import com.pipeline.admin.mapper.PlatformAccountMapper;
import com.pipeline.admin.mapper.PublishLogMapper;
import com.pipeline.admin.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishServiceImpl implements PublishService {
    private final PublishLogMapper publishLogMapper;
    private final PlatformAccountMapper platformAccountMapper;
    private final TaskMapper taskMapper;
    private final TaskService taskService;

    @Override
    public PublishLog create(Long taskId, String platform, Long accountId, String title, String tags, LocalDateTime scheduledAt) {
        PublishLog logEntry = new PublishLog();
        logEntry.setTaskId(taskId);
        logEntry.setPlatform(platform);
        logEntry.setAccountId(accountId);
        logEntry.setTitle(title);
        logEntry.setTags(tags);
        logEntry.setScheduledAt(scheduledAt);
        logEntry.setStatus("PENDING");
        publishLogMapper.insert(logEntry);
        return logEntry;
    }

    @Override
    @Transactional
    public void publish(Long publishId) {
        PublishLog logEntry = publishLogMapper.selectById(publishId);
        if (logEntry == null) {
            throw new IllegalArgumentException("发布记录不存在: " + publishId);
        }

        // 推进任务状态 READY → PUBLISHED
        taskService.updateStatus(logEntry.getTaskId(), "PUBLISHED", 100, null);

        logEntry.setStatus("PUBLISHED");
        logEntry.setPublishedAt(LocalDateTime.now());
        publishLogMapper.updateById(logEntry);
        log.info("发布完成: publishId={}, taskId={}", publishId, logEntry.getTaskId());
    }

    @Override
    public void schedule(Long publishId, LocalDateTime scheduledAt) {
        PublishLog logEntry = publishLogMapper.selectById(publishId);
        if (logEntry == null) throw new IllegalArgumentException("发布记录不存在: " + publishId);
        logEntry.setScheduledAt(scheduledAt);
        publishLogMapper.updateById(logEntry);
        log.info("定时发布已设置: publishId={}, scheduledAt={}", publishId, scheduledAt);
    }

    @Override
    public void cancelPublish(Long publishId) {
        PublishLog logEntry = publishLogMapper.selectById(publishId);
        if (logEntry == null) throw new IllegalArgumentException("发布记录不存在: " + publishId);
        logEntry.setStatus("CANCELLED");
        publishLogMapper.updateById(logEntry);
        log.info("发布已取消: publishId={}", publishId);
    }

    @Override
    public List<Map<String, Object>> getCalendar(String startDate, String endDate) {
        // MVP：返回模拟发布日历数据
        return new ArrayList<>();
    }

    @Override
    public List<PublishLog> getByTask(Long taskId) {
        LambdaQueryWrapper<PublishLog> q = new LambdaQueryWrapper<PublishLog>()
                .orderByDesc(PublishLog::getCreatedAt);
        if (taskId != null) {
            q.eq(PublishLog::getTaskId, taskId);
        }
        return publishLogMapper.selectList(q);
    }

    @Override
    public List<PlatformAccount> getPlatformAccounts(String platform) {
        return platformAccountMapper.selectList(
                new LambdaQueryWrapper<PlatformAccount>()
                        .eq(platform != null && !platform.isEmpty(), PlatformAccount::getPlatform, platform)
                        .eq(PlatformAccount::getStatus, "ENABLED")
        );
    }
}