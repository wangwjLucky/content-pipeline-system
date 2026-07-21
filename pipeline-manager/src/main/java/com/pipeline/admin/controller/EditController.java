package com.pipeline.admin.controller;

import com.pipeline.admin.common.OperationLog;
import com.pipeline.admin.entity.Task;
import com.pipeline.admin.mapper.TaskMapper;
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
    private final TaskMapper taskMapper;

    @OperationLog(module = "剪辑管理", action = "合成")
    @PostMapping("/compile")
    public Result<Void> compile(@RequestBody Map<String, Object> body) {
        Long taskId = Long.valueOf(body.get("taskId").toString());
        editService.compile(taskId);
        return Result.success(null);
    }

    @OperationLog(module = "剪辑管理", action = "合成")
    @PostMapping("/{taskId}/compile")
    public Result<Void> compile(@PathVariable Long taskId) {
        editService.compile(taskId);
        return Result.success(null);
    }

    @GetMapping("/{taskId}/preview")
    public Result<Map<String, Object>> preview(@PathVariable Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("title", task.getTitle());
        data.put("status", task.getStatus());
        if ("REVIEW".equals(task.getStatus()) || "READY".equals(task.getStatus())) {
            data.put("previewUrl", "https://minio.internal/pipeline-videos-final/" + taskId + "/preview.mp4");
        }
        return Result.success(data);
    }

    @OperationLog(module = "剪辑管理", action = "重新剪辑")
    @PostMapping("/{taskId}/regenerate")
    public Result<Void> regenerate(@PathVariable Long taskId) {
        editService.compile(taskId);
        return Result.success(null);
    }
}