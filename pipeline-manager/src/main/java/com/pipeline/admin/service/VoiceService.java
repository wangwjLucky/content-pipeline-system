package com.pipeline.admin.service;

import com.pipeline.admin.entity.Voice;

public interface VoiceService {

    /**
     * 触发 TTS 配音生成
     */
    void generate(Long taskId, String voiceType);

    /**
     * 查询任务配音
     */
    Voice getByTaskId(Long taskId);

    /**
     * 更新配音参数（配音类型、语速等）
     */
    Voice update(Long taskId, String voiceType, java.math.BigDecimal speed);
}