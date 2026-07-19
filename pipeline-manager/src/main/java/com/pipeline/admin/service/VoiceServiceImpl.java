package com.pipeline.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipeline.admin.entity.Voice;
import com.pipeline.admin.mapper.VoiceMapper;
import com.pipeline.admin.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceServiceImpl implements VoiceService {
    private final VoiceMapper voiceMapper;
    private final AiService aiService;

    @Override
    public void generate(Long taskId, String voiceType) {
        Voice voice = new Voice();
        voice.setTaskId(taskId);
        voice.setVoiceType(voiceType);
        voice.setStatus("PENDING");
        voiceMapper.insert(voice);

        // 发送 MQ 消息触发 AI 配音
        aiService.sendVoiceGenerate(taskId, Map.of(
                "voiceId", voice.getId(),
                "voiceType", voiceType
        ));
        log.info("配音生成已触发: taskId={}, voiceType={}", taskId, voiceType);
    }

    @Override
    public Voice getByTaskId(Long taskId) {
        return voiceMapper.selectOne(
                new LambdaQueryWrapper<Voice>().eq(Voice::getTaskId, taskId)
        );
    }

    @Override
    public Voice update(Long taskId, String voiceType, java.math.BigDecimal speed) {
        Voice voice = getByTaskId(taskId);
        if (voice == null) return null;
        if (voiceType != null) voice.setVoiceType(voiceType);
        if (speed != null) voice.setSpeed(speed);
        voiceMapper.updateById(voice);
        log.info("配音参数已更新: taskId={}, voiceType={}, speed={}", taskId, voiceType, speed);
        return voice;
    }
}