package com.pipeline.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipeline.admin.entity.Storyboard;
import com.pipeline.admin.mapper.StoryboardMapper;
import com.pipeline.admin.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryboardServiceImpl implements StoryboardService {
    private final StoryboardMapper storyboardMapper;
    private final AiService aiService;

    @Override
    @Transactional
    public void batchSave(Long taskId, List<Storyboard> storyboards) {
        // 删除旧分镜
        storyboardMapper.delete(new LambdaQueryWrapper<Storyboard>()
                .eq(Storyboard::getTaskId, taskId));
        // 批量插入
        for (int i = 0; i < storyboards.size(); i++) {
            Storyboard sb = storyboards.get(i);
            sb.setTaskId(taskId);
            sb.setSequence(i + 1);
            storyboardMapper.insert(sb);
        }
        log.info("分镜批量保存完成: taskId={}, count={}", taskId, storyboards.size());
    }

    @Override
    public List<Storyboard> getByTaskId(Long taskId) {
        return storyboardMapper.selectList(
                new LambdaQueryWrapper<Storyboard>()
                        .eq(Storyboard::getTaskId, taskId)
                        .orderByAsc(Storyboard::getSequence)
        );
    }

    @Override
    public void autoSplit(Long taskId) {
        // 触发 AI 分镜生成（异步 MQ）
        aiService.sendPromptGenerate(taskId, Map.of("taskId", taskId));
        log.info("已触发 AI 自动分镜: taskId={}", taskId);
    }
}