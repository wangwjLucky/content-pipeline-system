package com.pipeline.admin.controller;

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

    @PutMapping
    public Result<Void> batchSave(@PathVariable Long taskId, @RequestBody List<Storyboard> storyboards) {
        storyboardService.batchSave(taskId, storyboards);
        return Result.success(null);
    }

    @PostMapping("/auto-split")
    public Result<Void> autoSplit(@PathVariable Long taskId) {
        storyboardService.autoSplit(taskId);
        return Result.success(null);
    }
}