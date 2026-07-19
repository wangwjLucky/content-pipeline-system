package com.pipeline.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipeline.admin.entity.Material;
import com.pipeline.admin.entity.Storyboard;
import com.pipeline.admin.mapper.MaterialMapper;
import com.pipeline.admin.mapper.StoryboardMapper;
import com.pipeline.admin.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {
    private final MaterialMapper materialMapper;
    private final StoryboardMapper storyboardMapper;
    private final AiService aiService;

    @Override
    public Material getById(Long id) {
        return materialMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        materialMapper.deleteById(id);
    }

    @Override
    public void batchGenerate(Long taskId) {
        // 查询所有分镜，为每个分镜创建素材记录
        List<Storyboard> storyboards = storyboardMapper.selectList(
                new LambdaQueryWrapper<Storyboard>()
                        .eq(Storyboard::getTaskId, taskId)
                        .orderByAsc(Storyboard::getSequence)
        );

        for (Storyboard sb : storyboards) {
            Material material = new Material();
            material.setTaskId(taskId);
            material.setStoryboardId(sb.getId());
            material.setType("video");
            material.setPrompt(sb.getAiPrompt());
            material.setStatus("PENDING");
            materialMapper.insert(material);
            log.debug("创建素材记录: storyboardId={}, prompt={}", sb.getId(), sb.getAiPrompt());

            // 发送 MQ 消息触发 AI 视频生成
            aiService.sendVideoGenerate(taskId, Map.of(
                    "storyboardId", sb.getId(),
                    "prompt", sb.getAiPrompt(),
                    "materialId", material.getId()
            ));
        }
        log.info("素材批量生成完成: taskId={}, count={}", taskId, storyboards.size());
    }

    @Override
    public List<Material> getByTask(Long taskId, Long storyboardId, String type) {
        return materialMapper.selectList(
                new LambdaQueryWrapper<Material>()
                        .eq(taskId != null, Material::getTaskId, taskId)
                        .eq(storyboardId != null, Material::getStoryboardId, storyboardId)
                        .eq(type != null && !type.isEmpty(), Material::getType, type)
        );
    }
}