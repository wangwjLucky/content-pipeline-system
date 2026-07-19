package com.pipeline.admin.service;

import com.pipeline.admin.entity.PlatformAccount;
import com.pipeline.admin.entity.PublishLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PublishService {

    /**
     * 创建发布记录
     */
    PublishLog create(Long taskId, String platform, Long accountId, String title, String tags, LocalDateTime scheduledAt);

    /**
     * 执行发布（READY → PUBLISHED）
     */
    void publish(Long publishId);

    /**
     * 设置定时发布
     */
    void schedule(Long publishId, LocalDateTime scheduledAt);

    /**
     * 取消发布
     */
    void cancelPublish(Long publishId);

    /**
     * 发布日历
     */
    List<Map<String, Object>> getCalendar(String startDate, String endDate);

    /**
     * 发布记录列表，taskId 为 null 时返回全部
     */
    List<PublishLog> getByTask(Long taskId);

    /**
     * 可用平台账号列表
     */
    List<PlatformAccount> getPlatformAccounts(String platform);
}