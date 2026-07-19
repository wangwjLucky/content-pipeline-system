package com.pipeline.admin.controller;

import com.pipeline.admin.common.OperationLog;
import com.pipeline.admin.entity.Task;
import com.pipeline.admin.entity.Topic;
import com.pipeline.admin.service.TaskService;
import com.pipeline.admin.service.TopicService;
import com.pipeline.admin.common.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
public class TopicController {
    private final TopicService topicService;
    private final TaskService taskService;

    @GetMapping
    public Result<Page<Topic>> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String status) {
        return Result.success(topicService.list(page, size, status));
    }

    @GetMapping("/{id}")
    public Result<Topic> get(@PathVariable Long id) {
        Topic topic = topicService.get(id);
        return topic != null ? Result.success(topic) : Result.error(404, "选题不存在");
    }

    @OperationLog(module = "选题管理", action = "创建")
    @PostMapping
    public Result<Topic> create(@RequestBody Topic topic) {
        return Result.success(topicService.create(topic));
    }

    @OperationLog(module = "选题管理", action = "更新")
    @PutMapping("/{id}")
    public Result<Topic> update(@PathVariable Long id, @RequestBody Topic topic) {
        return Result.success(topicService.update(id, topic));
    }

    @OperationLog(module = "选题管理", action = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        topicService.delete(id);
        return Result.success(null);
    }

    @OperationLog(module = "选题管理", action = "生成任务")
    @PostMapping("/{id}/generate-task")
    public Result<Task> generateTask(@PathVariable Long id) {
        Topic topic = topicService.get(id);
        if (topic == null) {
            return Result.error(404, "选题不存在");
        }
        Task task = taskService.createTask(id, topic.getTitle());
        return Result.success(task);
    }
}