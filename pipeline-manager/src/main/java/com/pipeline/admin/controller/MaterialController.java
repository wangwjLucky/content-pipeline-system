package com.pipeline.admin.controller;

import com.pipeline.admin.entity.Material;
import com.pipeline.admin.service.MaterialService;
import com.pipeline.admin.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {
    private final MaterialService materialService;

    @GetMapping
    public Result<List<Material>> list(@RequestParam(required = false) Long taskId,
                                       @RequestParam(required = false) Long storyboardId,
                                       @RequestParam(required = false) String type) {
        return Result.success(materialService.getByTask(taskId, storyboardId, type));
    }

    @GetMapping("/{id}")
    public Result<Material> get(@PathVariable Long id) {
        Material material = materialService.getById(id);
        return material != null ? Result.success(material) : Result.error(404, "素材不存在");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/batch-generate")
    public Result<Void> batchGenerate(@RequestParam Long taskId) {
        materialService.batchGenerate(taskId);
        return Result.success(null);
    }
}