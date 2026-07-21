package com.pipeline.admin.service;

import com.pipeline.admin.entity.Task;
import com.pipeline.admin.entity.TaskEvent;

import java.util.List;

public interface TaskService {
    Task createTask(Long topicId, String title);
    Task createTask(Long topicId, String title, String contentType);

    /**
     * 更新任务状态，校验状态机合法性并记录事件。
     */
    void updateStatus(Long taskId, String status, Integer progress, String errorMessage);

    /**
     * 更新任务状态，记录失败原因。
     */
    void updateStatus(Long taskId, String status, Integer progress, String errorMessage, String failReason);

    /**
     * 取消任务（WAIT/SCRIPTING/SCRIPT_REVIEW/... → CANCELLED）
     */
    void cancelTask(Long taskId, String operator, String comment);

    /**
     * 重试任务（ERROR → WAIT），重新发送脚本生成消息
     */
    void retryTask(Long taskId, String operator);

    /**
     * 获取任务时间线（事件历史）
     */
    List<TaskEvent> getTimeline(Long taskId);
}