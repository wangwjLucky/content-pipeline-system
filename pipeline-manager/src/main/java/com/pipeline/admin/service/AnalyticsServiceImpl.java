package com.pipeline.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipeline.admin.entity.PublishLog;
import com.pipeline.admin.entity.Script;
import com.pipeline.admin.entity.Task;
import com.pipeline.admin.entity.Topic;
import com.pipeline.admin.mapper.PublishLogMapper;
import com.pipeline.admin.mapper.ScriptMapper;
import com.pipeline.admin.mapper.TaskMapper;
import com.pipeline.admin.mapper.TopicMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {
    private final TaskMapper taskMapper;
    private final TopicMapper topicMapper;
    private final ScriptMapper scriptMapper;
    private final PublishLogMapper publishLogMapper;

    @Override
    public Map<String, Object> getOverview() {
        long totalTasks = taskMapper.selectCount(null);
        long completedTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getStatus, "PUBLISHED"));
        long inProgressTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>()
                        .notIn(Task::getStatus, List.of("PUBLISHED", "CANCELLED", "ERROR")));
        long pendingReviewScripts = scriptMapper.selectCount(
                new LambdaQueryWrapper<Script>().eq(Script::getStatus, "PENDING_REVIEW"));
        long pendingPublish = publishLogMapper.selectCount(
                new LambdaQueryWrapper<PublishLog>()
                        .eq(PublishLog::getStatus, "PENDING")
                        .isNull(PublishLog::getScheduledAt));
        long todayPublished = publishLogMapper.selectCount(
                new LambdaQueryWrapper<PublishLog>()
                        .eq(PublishLog::getStatus, "PUBLISHED")
                        .apply("DATE(published_at) = CURRENT_DATE"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalTasks", totalTasks);
        data.put("completedTasks", completedTasks);
        data.put("inProgressTasks", inProgressTasks);
        data.put("pendingReviewScripts", pendingReviewScripts);
        data.put("pendingPublish", pendingPublish);
        data.put("todayPublished", todayPublished);
        return data;
    }

    @Override
    public Map<String, Object> getDaily(String startDate, String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        List<Map<String, Object>> trends = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            LocalDate day = date;
            long published = publishLogMapper.selectCount(
                    new LambdaQueryWrapper<PublishLog>()
                            .eq(PublishLog::getStatus, "PUBLISHED")
                            .apply("DATE(published_at) = DATE({0})", day));
            long created = taskMapper.selectCount(
                    new LambdaQueryWrapper<Task>()
                            .apply("DATE(created_at) = DATE({0})", day));

            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", day.toString());
            dayData.put("published", published);
            dayData.put("created", created);
            trends.add(dayData);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("startDate", start.toString());
        data.put("endDate", end.toString());
        data.put("trends", trends);
        return data;
    }

    @Override
    public Map<String, Object> getTopics(int limit) {
        long totalTopics = topicMapper.selectCount(null);
        long hotTopics = topicMapper.selectCount(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getIsAuto, true));
        long completedTopics = topicMapper.selectCount(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getStatus, "COMPLETED"));

        // 按来源分组统计
        List<Map<String, Object>> topSources = new ArrayList<>();
        // MyBatis Plus 没有直接 group by 的支持，这里简化处理
        // 实际生产环境可以用 XML 自定义查询

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalTopics", totalTopics);
        data.put("hotTopics", hotTopics);
        data.put("completedTopics", completedTopics);
        data.put("topTopicSources", topSources);
        return data;
    }

    @Override
    public Map<String, Object> getAccounts() {
        long totalAccounts = publishLogMapper.selectCount(
                new LambdaQueryWrapper<PublishLog>()
                        .isNotNull(PublishLog::getAccountId)
                        .apply("account_id IS NOT NULL"));
        long activeAccounts = publishLogMapper.selectCount(
                new LambdaQueryWrapper<PublishLog>()
                        .eq(PublishLog::getStatus, "PUBLISHED")
                        .apply("published_at >= NOW() - INTERVAL '30 days'"));
        long totalPublished = publishLogMapper.selectCount(
                new LambdaQueryWrapper<PublishLog>()
                        .eq(PublishLog::getStatus, "PUBLISHED"));

        // 按平台分组统计
        List<Map<String, Object>> accountStats = new ArrayList<>();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalAccounts", totalAccounts);
        data.put("activeAccounts", activeAccounts);
        data.put("totalPublished", totalPublished);
        data.put("accountStats", accountStats);
        return data;
    }
}