package com.pipeline.admin.controller;

import com.pipeline.admin.common.OperationLog;
import com.pipeline.admin.entity.PlatformAccount;
import com.pipeline.admin.entity.PublishLog;
import com.pipeline.admin.service.PublishService;
import com.pipeline.admin.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/publish")
@RequiredArgsConstructor
public class PublishController {
    private final PublishService publishService;

    @OperationLog(module = "发布管理", action = "创建发布")
    @PostMapping
    public Result<PublishLog> create(@RequestBody Map<String, Object> body) {
        Long taskId = Long.valueOf(body.get("taskId").toString());
        String platform = (String) body.get("platform");
        Long accountId = body.get("accountId") != null ? Long.valueOf(body.get("accountId").toString()) : null;
        String title = (String) body.get("title");
        String tags = (String) body.get("tags");
        String scheduledAtStr = (String) body.get("scheduledAt");
        LocalDateTime scheduledAt = scheduledAtStr != null ? LocalDateTime.parse(scheduledAtStr) : null;
        return Result.success(publishService.create(taskId, platform, accountId, title, tags, scheduledAt));
    }

    @OperationLog(module = "发布管理", action = "发布")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        publishService.publish(id);
        return Result.success(null);
    }

    @GetMapping
    public Result<List<PublishLog>> list(@RequestParam(required = false) Long taskId) {
        return Result.success(publishService.getByTask(taskId));
    }

    @PostMapping("/{id}/schedule")
    public Result<Void> schedule(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String scheduledAt = body.get("scheduledAt");
        if (scheduledAt == null) {
            return Result.error(400, "定时时间不能为空");
        }
        publishService.schedule(id, LocalDateTime.parse(scheduledAt));
        return Result.success(null);
    }

    @OperationLog(module = "发布管理", action = "取消发布")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelPublish(@PathVariable Long id) {
        publishService.cancelPublish(id);
        return Result.success(null);
    }

    @GetMapping("/calendar")
    public Result<List<Map<String, Object>>> calendar(@RequestParam(required = false) String startDate,
                                                       @RequestParam(required = false) String endDate) {
        return Result.success(publishService.getCalendar(startDate, endDate));
    }

    @GetMapping("/accounts")
    public Result<List<PlatformAccount>> accounts(@RequestParam(required = false) String platform) {
        return Result.success(publishService.getPlatformAccounts(platform));
    }
}