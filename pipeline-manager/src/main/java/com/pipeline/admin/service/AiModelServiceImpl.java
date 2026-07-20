package com.pipeline.admin.service;

import com.pipeline.admin.common.EncryptionUtil;
import com.pipeline.admin.entity.AiModelConfig;
import com.pipeline.admin.mapper.AiModelConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {
    private final AiModelConfigMapper mapper;

    @Override
    public Page<AiModelConfig> list(int page, int size, String provider) {
        LambdaQueryWrapper<AiModelConfig> q = new LambdaQueryWrapper<AiModelConfig>()
                .eq(provider != null && !provider.isEmpty(), AiModelConfig::getProvider, provider)
                .orderByDesc(AiModelConfig::getWeight);
        return mapper.selectPage(new Page<>(page, size), q);
    }

    @Override
    public AiModelConfig get(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public AiModelConfig create(AiModelConfig config) {
        config.setEnabled(true);
        // 兼容前端传 apiKey 字段
        String rawKey = config.getApiKey();
        if (rawKey != null && !rawKey.isEmpty()) {
            config.setApiKeyEncrypted(EncryptionUtil.encrypt(rawKey));
        } else if (config.getApiKeyEncrypted() != null && !config.getApiKeyEncrypted().isEmpty()) {
            config.setApiKeyEncrypted(EncryptionUtil.encrypt(config.getApiKeyEncrypted()));
        }
        mapper.insert(config);
        return config;
    }

    @Override
    public AiModelConfig update(Long id, AiModelConfig config) {
        config.setId(id);
        mapper.updateById(config);
        return mapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }
}