package com.pipeline.admin.controller;

import com.pipeline.admin.common.OperationLog;
import com.pipeline.admin.entity.Storyboard;
import com.pipeline.admin.service.StoryboardService;
import com.pipeline.admin.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/storyboard")
@RequiredArgsConstructor
public class StoryboardController {
    private final StoryboardService storyboardService;

    @GetMapping
    public Result<List<Storyboard>> list(@PathVariable Long taskId) {
        return Result.success(storyboardService.getByTaskId(taskId));
    }

    @OperationLog(module = "分镜管理", action = "批量保存")
    @PutMapping
    public Result<Void> batchSave(@PathVariable Long taskId, @RequestBody List<Storyboard> storyboards) {
        storyboardService.batchSave(taskId, storyboards);
        return Result.success(null);
    }

    @OperationLog(module = "分镜管理", action = "AI 自动拆分")
    @PostMapping("/auto-split")
    public Result<Void> autoSplit(@PathVariable Long taskId) {
        storyboardService.autoSplit(taskId);
        return Result.success(null);
    }
}