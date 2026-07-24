package com.pipeline.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipeline.admin.entity.Material;
import com.pipeline.admin.entity.Storyboard;
import com.pipeline.admin.entity.Task;
import com.pipeline.admin.entity.VersionGraph;
import com.pipeline.admin.mapper.MaterialMapper;
import com.pipeline.admin.mapper.StoryboardMapper;
import com.pipeline.admin.mapper.TaskMapper;
import com.pipeline.admin.mapper.VersionGraphMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryboardServiceImpl implements StoryboardService {
    private final StoryboardMapper storyboardMapper;
    private final MaterialMapper materialMapper;
    private final TaskMapper taskMapper;
    private final TaskService taskService;
    private final AiService aiService;
    private final VersionGraphMapper versionGraphMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSave(Long taskId, List<Storyboard> storyboards) {
        // 锁定任务行，序列化对同一 taskId 的并发分镜保存操作
        taskMapper.selectOne(new LambdaQueryWrapper<Task>().eq(Task::getId, taskId).last("FOR UPDATE"));

        // 获取当前版本号（取最大版本号 + 1，比 COUNT 更准确）
        VersionGraph latest = versionGraphMapper.selectOne(
                new LambdaQueryWrapper<VersionGraph>()
                        .eq(VersionGraph::getEntityType, "STORYBOARD")
                        .eq(VersionGraph::getEntityId, taskId)
                        .orderByDesc(VersionGraph::getVersion)
                        .last("LIMIT 1"));
        int newVersion = latest != null ? latest.getVersion() + 1 : 1;

        // 记录旧版本快照（先删后插前）
        List<Storyboard> oldList = storyboardMapper.selectList(
                new LambdaQueryWrapper<Storyboard>().eq(Storyboard::getTaskId, taskId));
        if (!oldList.isEmpty()) {
            try {
                List<Map<String, Object>> snapshots = oldList.stream().map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("seq", s.getSequence());
                    m.put("sceneType", s.getSceneType());
                    m.put("character", s.getCharacter());
                    return m;
                }).toList();
                String snapshot = objectMapper.writeValueAsString(snapshots);
                VersionGraph vg = new VersionGraph();
                vg.setEntityType("STORYBOARD");
                vg.setEntityId(taskId);
                vg.setVersion(newVersion);
                vg.setSnapshot(snapshot);
                versionGraphMapper.insert(vg);
            } catch (JsonProcessingException e) {
                log.warn("分镜快照序列化失败: taskId={}", taskId, e);
                throw new RuntimeException("分镜快照序列化失败，事务回滚", e);
            }
        }

        // 先删除关联的素材，避免外键约束冲突
        materialMapper.delete(new LambdaQueryWrapper<Material>()
                .eq(Material::getTaskId, taskId));
        // 删除旧分镜
        storyboardMapper.delete(new LambdaQueryWrapper<Storyboard>()
                .eq(Storyboard::getTaskId, taskId));
        // 批量插入并为每个分镜创建素材记录
        String mediaType = "video";
        Task task = taskMapper.selectById(taskId);
        if (task != null && ("image".equals(task.getContentType()) || "image_text".equals(task.getContentType()))) {
            mediaType = "image";
        }

        for (int i = 0; i < storyboards.size(); i++) {
            Storyboard sb = storyboards.get(i);
            sb.setTaskId(taskId);
            sb.setSequence(i + 1);
            storyboardMapper.insert(sb);

            Material material = new Material();
            material.setTaskId(taskId);
            material.setStoryboardId(sb.getId());
            material.setType(mediaType);
            material.setPrompt(sb.getAiPrompt());
            material.setStatus("PENDING");
            materialMapper.insert(material);
        }

        log.info("分镜批量保存完成: taskId={}, count={}, 素材记录已创建", taskId, storyboards.size());

        // 推进任务到 GENERATING，根据 content_type 触发对应素材生成
        if (task != null && "STORYBOARD".equals(task.getStatus())) {
            taskService.updateStatus(taskId, "GENERATING", 50, null);
            String ct = task.getContentType();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if ("image".equals(ct) || "image_text".equals(ct)) {
                        aiService.sendImageGenerate(taskId, Map.of("taskId", taskId));
                        log.info("图片素材生成已触发: taskId={}", taskId);
                    } else {
                        aiService.sendVideoGenerate(taskId, Map.of("taskId", taskId));
                        aiService.sendImageGenerate(taskId, Map.of("taskId", taskId));
                        log.info("素材生成已触发: taskId={}", taskId);
                    }
                }
            });
        }
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
        aiService.sendPromptGenerate(taskId, Map.of("taskId", taskId));
        log.info("已触发 AI 自动分镜: taskId={}", taskId);
    }
}