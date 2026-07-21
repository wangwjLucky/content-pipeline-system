package com.pipeline.admin.controller;

import com.pipeline.admin.common.OperationLog;
import com.pipeline.admin.entity.Voice;
import com.pipeline.admin.service.VoiceService;
import com.pipeline.admin.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/voices")
@RequiredArgsConstructor
public class VoiceController {
    private final VoiceService voiceService;

    @OperationLog(module = "配音管理", action = "生成")
    @PostMapping("/generate")
    public Result<Void> generate(@RequestBody Map<String, Object> body) {
        Long taskId = Long.valueOf(body.get("taskId").toString());
        String voiceType = (String) body.getOrDefault("voiceType", "doubao");
        voiceService.generate(taskId, voiceType);
        return Result.success(null);
    }

    @GetMapping("/{taskId}")
    public Result<Voice> getByTask(@PathVariable Long taskId) {
        Voice voice = voiceService.getByTaskId(taskId);
        return voice != null ? Result.success(voice) : Result.error(404, "配音记录不存在");
    }

    @OperationLog(module = "配音管理", action = "更新")
    @PutMapping("/{taskId}")
    public Result<Voice> update(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        String voiceType = (String) body.getOrDefault("voiceType", null);
        java.math.BigDecimal speed = body.get("speed") != null
                ? java.math.BigDecimal.valueOf(Double.parseDouble(body.get("speed").toString())) : null;
        Voice voice = voiceService.update(taskId, voiceType, speed);
        return voice != null ? Result.success(voice) : Result.error(404, "配音记录不存在");
    }
}