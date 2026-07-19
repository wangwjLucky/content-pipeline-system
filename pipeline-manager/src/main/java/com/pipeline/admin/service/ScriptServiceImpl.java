package com.pipeline.admin.service;

import com.pipeline.admin.entity.Script;
import com.pipeline.admin.entity.Task;
import com.pipeline.admin.mapper.ScriptMapper;
import com.pipeline.admin.mapper.TaskMapper;
import com.pipeline.admin.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptServiceImpl implements ScriptService {
    private final ScriptMapper scriptMapper;
    private final TaskMapper taskMapper;
    private final TaskService taskService;
    private final AiService aiService;

    @Override
    @Transactional
    public Script generate(Long taskId, String topicTitle) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在: " + taskId);

        // 创建脚本记录
        Script script = new Script();
        script.setTaskId(taskId);
        script.setTopicId(task.getTopicId());
        script.setTitle(topicTitle);
        script.setContent("");
        script.setSubtitle("");
        script.setVersion(1);
        script.setStatus("PENDING_REVIEW");
        scriptMapper.insert(script);

        // 关联脚本 ID 到任务
        task.setScriptId(script.getId());
        taskMapper.updateById(task);

        // 事务提交后再发送 MQ 消息
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

        // 更新脚本状态为已批准
        script.setStatus("APPROVED");
        scriptMapper.updateById(script);

        // 推进任务到 STORYBOARD
        taskService.updateStatus(script.getTaskId(), "STORYBOARD", 40, null);

        // 事务提交后再触发 AI 分镜生成
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

        // 更新脚本状态为已驳回
        script.setStatus("REJECTED");
        scriptMapper.updateById(script);

        // 任务回退到 WAIT
        taskService.updateStatus(script.getTaskId(), "WAIT", 0, reason);
        log.info("脚本已驳回: scriptId={}, taskId={}, reason={}", scriptId, script.getTaskId(), reason);
    }

    @Override
    @Transactional
    public Script edit(Long scriptId, String content, String subtitle, Long editorId) {
        Script script = scriptMapper.selectById(scriptId);
        if (script == null) {
            throw new IllegalArgumentException("脚本不存在: " + scriptId);
        }

        // 更新内容并递增版本号
        script.setContent(content);
        script.setSubtitle(subtitle);
        script.setVersion(script.getVersion() == null ? 1 : script.getVersion() + 1);
        // 编辑后需要重新审核
        script.setStatus("PENDING_REVIEW");
        scriptMapper.updateById(script);

        // 回退任务状态到 SCRIPT_REVIEW（如果当前在 STORYBOARD 或后续状态）
        taskService.updateStatus(script.getTaskId(), "SCRIPT_REVIEW", 30, null);

        log.info("脚本已编辑: scriptId={}, version={}", scriptId, script.getVersion());
        return script;
    }
}