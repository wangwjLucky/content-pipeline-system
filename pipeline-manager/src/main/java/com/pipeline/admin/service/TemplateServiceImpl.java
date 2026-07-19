package com.pipeline.admin.service;

import com.pipeline.admin.entity.PromptTemplate;
import com.pipeline.admin.mapper.PromptTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {
    private final PromptTemplateMapper promptTemplateMapper;

    @Override
    public Page<PromptTemplate> list(int page, int size, String type) {
        LambdaQueryWrapper<PromptTemplate> q = new LambdaQueryWrapper<PromptTemplate>()
                .eq(type != null && !type.isEmpty(), PromptTemplate::getType, type)
                .orderByDesc(PromptTemplate::getCreatedAt);
        return promptTemplateMapper.selectPage(new Page<>(page, size), q);
    }

    @Override
    public PromptTemplate get(Long id) {
        return promptTemplateMapper.selectById(id);
    }

    @Override
    public PromptTemplate create(PromptTemplate template) {
        template.setEnabled(true);
        promptTemplateMapper.insert(template);
        return template;
    }

    @Override
    public PromptTemplate update(Long id, PromptTemplate template) {
        template.setId(id);
        promptTemplateMapper.updateById(template);
        return promptTemplateMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        promptTemplateMapper.deleteById(id);
    }
}