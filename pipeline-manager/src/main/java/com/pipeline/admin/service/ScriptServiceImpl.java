package com.pipeline.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipeline.admin.entity.Script;
import com.pipeline.admin.entity.Task;
import com.pipeline.admin.entity.VersionGraph;
import com.pipeline.admin.mapper.ScriptMapper;
import com.pipeline.admin.mapper.TaskMapper;
import com.pipeline.admin.mapper.VersionGraphMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptServiceImpl implements ScriptService {
    private final ScriptMapper scriptMapper;
    private final TaskMapper taskMapper;
    private final TaskService taskService;
    private final AiService aiService;
    private final VersionGraphMapper versionGraphMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Script generate(Long taskId, String topicTitle) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在: " + taskId);

        Script script = new Script();
        script.setTaskId(taskId);
        script.setTopicId(task.getTopicId());
        script.setTitle(topicTitle);
        script.setContent("");
        script.setSubtitle("");
        script.setVersion(1);
        script.setStatus("PENDING_REVIEW");
        scriptMapper.insert(script);

        task.setScriptId(script.getId());
        taskMapper.updateById(task);

        // 立即更新任务状态为 SCRIPT_REVIEW，无需等待 AI 回调
        taskService.updateStatus(taskId, "SCRIPT_REVIEW", 30, null);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiService.sendScriptGenerate(taskId, topicTitle, Map.of("scriptId", script.getId(), "topic", topicTitle));
                log.info("脚本生成已触发: taskId={}, scriptId={}", taskId, script.getId());
            }
        });
        return script;
    }

    @Override
    @Transactional
    public void approve(Long scriptId, Long reviewerId) {
        Script script = scriptMapper.selectById(scriptId);
        if (script == null) {
            throw new IllegalArgumentException("脚本不存在: " + scriptId);
        }

        script.setStatus("APPROVED");
        scriptMapper.updateById(script);

        Task task = taskMapper.selectById(script.getTaskId());
        if (task != null && "text".equals(task.getContentType())) {
            taskService.updateStatus(script.getTaskId(), "READY", 95, null);
            log.info("纯文案脚本已批准，进入待发布: scriptId={}, taskId={}", scriptId, script.getTaskId());
            return;
        }

        taskService.updateStatus(script.getTaskId(), "STORYBOARD", 40, null);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiService.sendPromptGenerate(script.getTaskId(), Map.of(
                        "scriptId", scriptId,
                        "content", script.getContent()
                ));
                log.info("脚本已批准: scriptId={}, taskId={}", scriptId, script.getTaskId());
            }
        });
    }

    @Override
    @Transactional
    public void reject(Long scriptId, Long reviewerId, String reason) {
        Script script = scriptMapper.selectById(scriptId);
        if (script == null) {
            throw new IllegalArgumentException("脚本不存在: " + scriptId);
        }

        script.setStatus("REJECTED");
        scriptMapper.updateById(script);

        Task currentTask = taskMapper.selectById(script.getTaskId());
        String targetStatus = "WAIT";
        int targetProgress = 0;
        if (currentTask != null && Set.of("STORYBOARD", "GENERATING", "VOICEOVER", "EDITING").contains(currentTask.getStatus())) {
            targetStatus = "SCRIPT_REVIEW";
            targetProgress = 30;
        }
        taskService.updateStatus(script.getTaskId(), targetStatus, targetProgress, reason);
        log.info("脚本已驳回: scriptId={}, taskId={}, reason={}, targetStatus={}", scriptId, script.getTaskId(), reason, targetStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Script edit(Long scriptId, String content, String subtitle, Long editorId) {
        Script script = scriptMapper.selectById(scriptId);
        if (script == null) {
            throw new IllegalArgumentException("脚本不存在: " + scriptId);
        }

        script.setContent(content);
        script.setSubtitle(subtitle);
        script.setStatus("PENDING_REVIEW");
        // 检查乐观锁，失败时抛出异常回滚事务
        if (scriptMapper.updateById(script) == 0) {
            throw new IllegalStateException("脚本更新失败，数据已被其他操作修改: scriptId=" + scriptId);
        }

        Task currentTask = taskMapper.selectById(script.getTaskId());
        if (currentTask != null && !"SCRIPT_REVIEW".equals(currentTask.getStatus())) {
            taskService.updateStatus(script.getTaskId(), "SCRIPT_REVIEW", 30, null);
        }

        // 记录版本历史（使用 Jackson 序列化避免 JSON 转义问题）
        try {
            // 使用 HashMap 避免 Map.of() 在值为 null 时抛 NPE
            Map<String, String> snapshotMap = new HashMap<>();
            snapshotMap.put("content", content);
            snapshotMap.put("subtitle", subtitle);
            String snapshot = objectMapper.writeValueAsString(snapshotMap);
            VersionGraph vg = new VersionGraph();
            vg.setEntityType("SCRIPT");
            vg.setEntityId(scriptId);
            // updateById 后 @Version 已自增，直接取即可
            vg.setVersion(script.getVersion() != null ? script.getVersion() : 1);
            vg.setSnapshot(snapshot);
            vg.setCreatedBy(editorId);
            versionGraphMapper.insert(vg);
        } catch (JsonProcessingException e) {
            log.warn("版本快照序列化失败: scriptId={}", scriptId, e);
            throw new RuntimeException("版本快照序列化失败，事务回滚", e);
        }

        log.info("脚本已编辑: scriptId={}, version={}", scriptId, script.getVersion());
        return script;
    }
}