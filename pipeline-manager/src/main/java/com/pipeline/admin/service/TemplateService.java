package com.pipeline.admin.service;

import com.pipeline.admin.entity.PromptTemplate;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface TemplateService {

    Page<PromptTemplate> list(int page, int size, String type);

    PromptTemplate get(Long id);

    PromptTemplate create(PromptTemplate template);

    PromptTemplate update(Long id, PromptTemplate template);

    void delete(Long id);
}