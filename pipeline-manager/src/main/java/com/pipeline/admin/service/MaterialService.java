package com.pipeline.admin.service;

import com.pipeline.admin.entity.Material;

import java.util.List;

public interface MaterialService {

    /**
     * 按 ID 查询素材
     */
    Material getById(Long id);

    /**
     * 删除素材
     */
    void delete(Long id);

    /**
     * 批量生成素材：对每个未完成的分镜发送 AI 生成消息
     */
    void batchGenerate(Long taskId);

    /**
     * 按 taskId / storyboardId / type 查询
     */
    List<Material> getByTask(Long taskId, Long storyboardId, String type);
}