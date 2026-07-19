package com.pipeline.admin.service;

import com.pipeline.admin.entity.Script;

public interface ScriptService {

    /**
     * AI 生成脚本
     */
    Script generate(Long taskId, String topicTitle);

    /**
     * 批准脚本，任务推进到 STORYBOARD，触发 AI 分镜生成。
     */
    void approve(Long scriptId, Long reviewerId);

    /**
     * 驳回脚本，任务回退到 WAIT。
     */
    void reject(Long scriptId, Long reviewerId, String reason);

    /**
     * 编辑脚本内容，版本号递增。
     */
    Script edit(Long scriptId, String content, String subtitle, Long editorId);
}