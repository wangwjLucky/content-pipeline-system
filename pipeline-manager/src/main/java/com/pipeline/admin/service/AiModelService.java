package com.pipeline.admin.service;

import com.pipeline.admin.entity.AiModelConfig;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface AiModelService {

    Page<AiModelConfig> list(int page, int size, String provider);

    AiModelConfig get(Long id);

    AiModelConfig create(AiModelConfig config);

    AiModelConfig update(Long id, AiModelConfig config);

    void delete(Long id);
}