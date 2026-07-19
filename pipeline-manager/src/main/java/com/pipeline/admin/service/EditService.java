package com.pipeline.admin.service;

import com.pipeline.admin.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditService {
    private final AiService aiService;

    /**
     * 触发 FFmpeg 剪辑合成
     */
    public void compile(Long taskId) {
        aiService.sendFfmpegCompile(taskId, Map.of("taskId", taskId));
        log.info("剪辑合成已触发: taskId={}", taskId);
    }
}