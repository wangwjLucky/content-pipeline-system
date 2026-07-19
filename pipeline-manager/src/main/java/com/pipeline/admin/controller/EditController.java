package com.pipeline.admin.controller;

import com.pipeline.admin.service.EditService;
import com.pipeline.admin.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/edits")
@RequiredArgsConstructor
public class EditController {
    private final EditService editService;

    @PostMapping("/compile")
    public Result<Void> compile(@RequestBody Map<String, Object> body) {
        Long taskId = Long.valueOf(body.get("taskId").toString());
        editService.compile(taskId);
        return Result.success(null);
    }

    @PostMapping("/{taskId}/compile")
    public Result<Void> compile(@PathVariable Long taskId) {
        editService.compile(taskId);
        return Result.success(null);
    }

    @GetMapping("/{taskId}/preview")
    public Result<Map<String, Object>> preview(@PathVariable Long taskId) {
        // MVP：返回模拟预览地址
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("previewUrl", "https://minio.internal/pipeline-videos-final/" + taskId + "/preview.mp4");
        data.put("status", "PROCESSING");
        return Result.success(data);
    }

    @PostMapping("/{taskId}/regenerate")
    public Result<Void> regenerate(@PathVariable Long taskId) {
        editService.compile(taskId);
        return Result.success(null);
    }
}