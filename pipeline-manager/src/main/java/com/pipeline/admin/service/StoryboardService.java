package com.pipeline.admin.service;

import com.pipeline.admin.entity.Storyboard;

import java.util.List;

public interface StoryboardService {

    /**
     * 批量保存分镜（全量替换：先删后插）
     */
    void batchSave(Long taskId, List<Storyboard> storyboards);

    /**
     * 查询任务的分镜列表，按 sequence 排序
     */
    List<Storyboard> getByTaskId(Long taskId);

    /**
     * 触发 AI 自动拆分脚本为分镜
     */
    void autoSplit(Long taskId);
}