package com.pipeline.admin.controller;

import com.pipeline.admin.entity.PromptTemplate;
import com.pipeline.admin.service.TemplateService;
import com.pipeline.admin.common.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateService templateService;

    @GetMapping
    public Result<Page<PromptTemplate>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) String type) {
        return Result.success(templateService.list(page, size, type));
    }

    @GetMapping("/{id}")
    public Result<PromptTemplate> get(@PathVariable Long id) {
        PromptTemplate template = templateService.get(id);
        return template != null ? Result.success(template) : Result.error(404, "模板不存在");
    }

    @PostMapping
    public Result<PromptTemplate> create(@RequestBody PromptTemplate template) {
        return Result.success(templateService.create(template));
    }

    @PutMapping("/{id}")
    public Result<PromptTemplate> update(@PathVariable Long id, @RequestBody PromptTemplate template) {
        return Result.success(templateService.update(id, template));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.success(null);
    }
}