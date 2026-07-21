package com.pipeline.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pipeline.admin.common.OperationLog;
import com.pipeline.admin.entity.Script;
import com.pipeline.admin.entity.VersionGraph;
import com.pipeline.admin.mapper.ScriptMapper;
import com.pipeline.admin.mapper.VersionGraphMapper;
import com.pipeline.admin.service.ScriptService;
import com.pipeline.admin.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/scripts")
@RequiredArgsConstructor
public class ScriptController {
    private final ScriptMapper scriptMapper;
    private final ScriptService scriptService;
    private final VersionGraphMapper versionGraphMapper;

    @GetMapping
    public Result<Page<Script>> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) Long taskId,
                                     @RequestParam(required = false) Long topicId,
                                     @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Script> q = new LambdaQueryWrapper<Script>()
                .eq(taskId != null, Script::getTaskId, taskId)
                .eq(topicId != null, Script::getTopicId, topicId)
                .eq(status != null && !status.isEmpty(), Script::getStatus, status)
                .orderByDesc(Script::getCreatedAt);
        return Result.success(scriptMapper.selectPage(new Page<>(page, size), q));
    }

    @GetMapping("/{id}")
    public Result<Script> get(@PathVariable Long id) {
        Script script = scriptMapper.selectById(id);
        return script != null ? Result.success(script) : Result.error(404, "脚本不存在");
    }

    @OperationLog(module = "脚本管理", action = "生成")
    @PostMapping("/generate")
    public Result<Script> generate(@RequestBody Map<String, Object> body) {
        Long taskId = Long.valueOf(body.get("taskId").toString());
        String topicTitle = (String) body.get("topicTitle");
        Script generated = scriptService.generate(taskId, topicTitle);
        return Result.success(generated);
    }

    @OperationLog(module = "脚本管理", action = "编辑")
    @PutMapping("/{id}")
    public Result<Script> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Script updated = scriptService.edit(id, body.get("content"), body.get("subtitle"), null);
        return Result.success(updated);
    }

    @OperationLog(module = "脚本管理", action = "审核")
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String action = (String) body.get("action");
        Long reviewerId = body.get("reviewerId") != null
                ? Long.valueOf(body.get("reviewerId").toString()) : null;
        if ("approve".equals(action)) {
            scriptService.approve(id, reviewerId);
        } else if ("reject".equals(action)) {
            String reason = (String) body.get("reason");
            scriptService.reject(id, reviewerId, reason);
        } else {
            return Result.error(400, "无效操作: " + action + "，仅支持 approve/reject");
        }
        return Result.success(null);
    }

    @OperationLog(module = "脚本管理", action = "批准")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long reviewerId = body != null && body.get("reviewerId") != null
                ? Long.valueOf(body.get("reviewerId").toString()) : null;
        scriptService.approve(id, reviewerId);
        return Result.success(null);
    }

    @OperationLog(module = "脚本管理", action = "驳回")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long reviewerId = body != null && body.get("reviewerId") != null
                ? Long.valueOf(body.get("reviewerId").toString()) : null;
        String reason = body != null ? (String) body.get("reason") : null;
        scriptService.reject(id, reviewerId, reason);
        return Result.success(null);
    }

    @GetMapping("/{id}/versions")
    public Result<List<VersionGraph>> versions(@PathVariable Long id) {
        Script script = scriptMapper.selectById(id);
        if (script == null) {
            return Result.error(404, "脚本不存在");
        }
        List<VersionGraph> versions = versionGraphMapper.selectList(
                new LambdaQueryWrapper<VersionGraph>()
                        .eq(VersionGraph::getEntityType, "SCRIPT")
                        .eq(VersionGraph::getEntityId, id)
                        .orderByDesc(VersionGraph::getVersion));
        return Result.success(versions);
    }
}