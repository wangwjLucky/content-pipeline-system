package com.pipeline.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pipeline.admin.common.OperationLog;
import com.pipeline.admin.entity.Task;
import com.pipeline.admin.entity.TaskEvent;
import com.pipeline.admin.mapper.TaskMapper;
import com.pipeline.admin.service.TaskService;
import com.pipeline.admin.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskMapper taskMapper;
    private final TaskService taskService;

    @GetMapping
    public Result<Page<Task>> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Task> q = new LambdaQueryWrapper<Task>()
                .eq(status != null && !status.isEmpty(), Task::getStatus, status)
                .orderByDesc(Task::getCreatedAt);
        return Result.success(taskMapper.selectPage(new Page<>(page, size), q));
    }

    @GetMapping("/{id}")
    public Result<Task> get(@PathVariable Long id) {
        Task task = taskMapper.selectById(id);
        return task != null ? Result.success(task) : Result.error(404, "任务不存在");
    }

    @OperationLog(module = "任务管理", action = "创建")
    @PostMapping
    public Result<Task> create(@RequestBody Task task) {
        Task created = taskService.createTask(task.getTopicId(), task.getTitle(), task.getContentType());
        return Result.success(created);
    }

    @OperationLog(module = "任务管理", action = "取消")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        taskService.cancelTask(id, body != null ? body.get("operator") : null, body != null ? body.get("comment") : null);
        return Result.success(null);
    }

    @OperationLog(module = "任务管理", action = "重试")
    @PostMapping("/{id}/retry")
    public Result<Void> retry(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        taskService.retryTask(id, body != null ? body.get("operator") : null);
        return Result.success(null);
    }

    @GetMapping("/{id}/timeline")
    public Result<List<TaskEvent>> timeline(@PathVariable Long id) {
        return Result.success(taskService.getTimeline(id));
    }
}