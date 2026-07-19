package com.pipeline.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipeline.admin.entity.Script;
import com.pipeline.admin.entity.Storyboard;
import com.pipeline.admin.entity.Task;
import com.pipeline.admin.mapper.ScriptMapper;
import com.pipeline.admin.mapper.TaskMapper;
import com.pipeline.admin.service.StoryboardService;
import com.pipeline.admin.service.TaskService;
import com.pipeline.admin.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class CallbackController {
    private final TaskMapper taskMapper;
    private final ScriptMapper scriptMapper;
    private final TaskService taskService;
    private final StoryboardService storyboardService;

    @Value("${pipeline.callback-token}")
    private String callbackToken;

    @PostMapping("/callback")
    @Transactional
    public Result<String> callback(
            @RequestHeader("X-Callback-Token") String token,
            @RequestBody Map<String, Object> request) {
        // 验证回调令牌
        if (!Objects.equals(token, callbackToken)) {
            log.warn("回调令牌无效: token={}", token);
            return Result.error(403, "禁止访问：回调令牌无效");
        }
        // 参数校验
        Object taskIdObj = request.get("taskId");
        if (taskIdObj == null) {
            log.warn("回调参数缺失: taskId 为空");
            return Result.error(400, "taskId 不能为空");
        }
        Long taskId;
        try {
            taskId = Long.valueOf(taskIdObj.toString());
        } catch (NumberFormatException e) {
            log.warn("回调参数无效: taskId={}", taskIdObj);
            return Result.error(400, "taskId 格式无效");
        }
        String service = (String) request.get("service");
        if (service == null || service.isEmpty()) {
            log.warn("回调参数缺失: service 为空, taskId={}", taskId);
            return Result.error(400, "service 不能为空");
        }
        String status = (String) request.get("status");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) request.get("data");
        String error = (String) request.get("error");

        log.info("收到回调: taskId={}, service={}, status={}", taskId, service, status);

        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return Result.error(404, "任务不存在");
        }

        // 终态任务跳过回调处理
        if ("CANCELLED".equals(task.getStatus()) || "PUBLISHED".equals(task.getStatus())) {
            log.warn("任务已处于终态，忽略回调: taskId={}, status={}", taskId, task.getStatus());
            return Result.success("ignored");
        }

        if ("SUCCESS".equals(status)) {
            handleSuccess(task, service, data);
        } else {
            taskService.updateStatus(task.getId(), "ERROR", task.getProgress(), error);
            log.error("任务处理失败: taskId={}, service={}, error={}", taskId, service, error);
        }

        return Result.success("ok");
    }

    private void handleSuccess(Task task, String service, Map<String, Object> data) {
        switch (service) {
            case "script" -> {
                // 幂等性检查：如果该任务已有脚本，则更新现有脚本而非创建新记录
                LambdaQueryWrapper<Script> existingQuery = new LambdaQueryWrapper<Script>()
                        .eq(Script::getTaskId, task.getId());
                Script existingScript = scriptMapper.selectOne(existingQuery);

                if (existingScript != null) {
                    // 更新现有脚本
                    existingScript.setTitle(data != null ? (String) data.get("title") : null);
                    existingScript.setContent(data != null ? (String) data.get("content") : null);
                    existingScript.setSubtitle(data != null ? (String) data.get("subtitle") : null);
                    existingScript.setStatus("PENDING_REVIEW");
                    scriptMapper.updateById(existingScript);
                    taskService.updateStatus(task.getId(), "SCRIPT_REVIEW", 30, null);
                    log.info("脚本已更新（幂等回调）: taskId={}, scriptId={}", task.getId(), existingScript.getId());
                } else {
                    Script script = new Script();
                    script.setTaskId(task.getId());
                    script.setTopicId(task.getTopicId());
                    script.setTitle(data != null ? (String) data.get("title") : null);
                    script.setContent(data != null ? (String) data.get("content") : null);
                    script.setSubtitle(data != null ? (String) data.get("subtitle") : null);
                    script.setStatus("PENDING_REVIEW");
                    scriptMapper.insert(script);
                    taskService.updateStatus(task.getId(), "SCRIPT_REVIEW", 30, null);
                    // 重新查询 task 获取最新版本号，避免 updateStatus 乐观锁递增后版本号陈旧
                    Task freshTask = taskMapper.selectById(task.getId());
                    Task update = new Task();
                    update.setId(freshTask.getId());
                    update.setScriptId(script.getId());
                    update.setVersion(freshTask.getVersion());
                    taskMapper.updateById(update);
                    log.info("脚本生成完成: taskId={}, scriptId={}", task.getId(), script.getId());
                }
            }
            case "prompt" -> {
                // 保存 AI 自动拆分的分镜数据
                if (data != null && data.containsKey("storyboards")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> sbList = (List<Map<String, Object>>) data.get("storyboards");
                    List<Storyboard> storyboards = sbList.stream().map(sb -> {
                        Storyboard s = new Storyboard();
                        s.setTaskId(task.getId());
                        s.setSequence(sb.containsKey("sequence") ? ((Number) sb.get("sequence")).intValue() : 0);
                        s.setDuration(sb.containsKey("duration") ? ((Number) sb.get("duration")).intValue() : 5);
                        s.setSceneType((String) sb.get("sceneType"));
                        s.setCharacter((String) sb.get("character"));
                        s.setAction((String) sb.get("action"));
                        s.setEnvironment((String) sb.get("environment"));
                        s.setCamera((String) sb.get("camera"));
                        s.setLighting((String) sb.get("lighting"));
                        s.setStyle((String) sb.get("style"));
                        s.setAiPrompt((String) sb.get("aiPrompt"));
                        return s;
                    }).collect(Collectors.toList());
                    storyboardService.batchSave(task.getId(), storyboards);
                    log.info("分镜数据已保存: taskId={}, count={}", task.getId(), storyboards.size());
                }
                taskService.updateStatus(task.getId(), "GENERATING", 50, null);
                log.info("分镜生成完成: taskId={}", task.getId());
            }
            case "video" -> {
                taskService.updateStatus(task.getId(), "VOICEOVER", 60, null);
                log.info("视频生成完成: taskId={}（若任务已处于 VOICEOVER 状态，表示另一素材仍在处理中）", task.getId());
            }
            case "image" -> {
                taskService.updateStatus(task.getId(), "VOICEOVER", 60, null);
                log.info("图片生成完成: taskId={}（若任务已处于 VOICEOVER 状态，表示另一素材仍在处理中）", task.getId());
            }
            case "voice" -> {
                taskService.updateStatus(task.getId(), "EDITING", 80, null);
                log.info("配音生成完成: taskId={}", task.getId());
            }
            case "ffmpeg" -> {
                taskService.updateStatus(task.getId(), "REVIEW", 95, null);
                log.info("剪辑合成完成: taskId={}", task.getId());
            }
            default -> log.warn("未知服务类型: {}", service);
        }
    }
}