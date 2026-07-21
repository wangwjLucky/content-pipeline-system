package com.pipeline.admin.controller;

import com.pipeline.admin.common.OperationLog;
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

    @OperationLog(module = "模板管理", action = "创建")
    @PostMapping
    public Result<PromptTemplate> create(@RequestBody PromptTemplate template) {
        return Result.success(templateService.create(template));
    }

    @OperationLog(module = "模板管理", action = "更新")
    @PutMapping("/{id}")
    public Result<PromptTemplate> update(@PathVariable Long id, @RequestBody PromptTemplate template) {
        return Result.success(templateService.update(id, template));
    }

    @OperationLog(module = "模板管理", action = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.success(null);
    }
}