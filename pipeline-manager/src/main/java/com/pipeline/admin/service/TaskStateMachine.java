package com.pipeline.admin.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

/**
 * 任务状态机 —— 校验状态转换合法性。
 *
 * <pre>
 * 状态说明：
 *   WAIT        等待中      — 初始状态，等待 AI 脚本生成
 *   SCRIPTING   脚本生成中  — AI 正在生成脚本
 *   SCRIPT_REVIEW 脚本审核中 — 人工审核脚本内容
 *   STORYBOARD  分镜中      — 脚本已批准，拆分为镜头
 *   GENERATING  素材生成中  — AI 生成视频/图片素材
 *   VOICEOVER   配音中      — TTS 语音合成
 *   EDITING     剪辑中      — FFmpeg 合成视频
 *   REVIEW      终审中      — 最终审核
 *   READY       待发布      — 审核通过，等待发布
 *   PUBLISHED   已发布      — 已发布到平台（终态）
 *   CANCELLED   已取消      — 用户取消（终态）
 *   ERROR       异常        — 流程异常，可重试
 *
 * 转换规则：
 *   WAIT → SCRIPTING | CANCELLED
 *   SCRIPTING → SCRIPT_REVIEW | ERROR | CANCELLED
 *   SCRIPT_REVIEW → STORYBOARD(批准) | WAIT(驳回) | CANCELLED
 *   STORYBOARD → GENERATING(素材生成) | SCRIPT_REVIEW(脚本驳回) | ERROR | CANCELLED
 *   GENERATING → VOICEOVER(素材完成) | SCRIPT_REVIEW(脚本驳回) | ERROR | CANCELLED
 *   VOICEOVER → EDITING(配音完成) | VOICEOVER(素材仍就绪中) | ERROR | CANCELLED
 *   EDITING → REVIEW | ERROR | CANCELLED
 *   REVIEW → READY(通过) | WAIT(驳回) | CANCELLED
 *   READY → PUBLISHED | CANCELLED
 *   PUBLISHED → (终态)
 *   CANCELLED → (终态)
 *   ERROR → WAIT(重试) | CANCELLED
 * </pre>
 */
@Component
public class TaskStateMachine {

    private static final Map<String, Set<String>> TRANSITIONS = Map.ofEntries(
            entry("WAIT", Set.of("WAIT", "SCRIPTING", "CANCELLED")),
            entry("SCRIPTING", Set.of("SCRIPT_REVIEW", "ERROR", "CANCELLED")),
            entry("SCRIPT_REVIEW", Set.of("STORYBOARD", "SCRIPT_REVIEW", "WAIT", "READY", "CANCELLED")),
            entry("STORYBOARD", Set.of("GENERATING", "SCRIPT_REVIEW", "ERROR", "CANCELLED")),
            entry("GENERATING", Set.of("VOICEOVER", "SCRIPT_REVIEW", "ERROR", "CANCELLED")),
            entry("VOICEOVER", Set.of("VOICEOVER", "EDITING", "SCRIPT_REVIEW", "ERROR", "CANCELLED")),
            entry("EDITING", Set.of("REVIEW", "SCRIPT_REVIEW", "ERROR", "CANCELLED")),
            entry("REVIEW", Set.of("READY", "SCRIPT_REVIEW", "WAIT", "CANCELLED")),
            entry("READY", Set.of("PUBLISHED", "CANCELLED")),
            entry("PUBLISHED", Set.of()),
            entry("CANCELLED", Set.of()),
            entry("ERROR", Set.of("WAIT", "CANCELLED"))
    );

    /**
     * 校验从 from 状态是否能转换到 to 状态。
     *
     * @return 如果允许转换返回 null，否则返回错误描述
     */
    public String validate(String from, String to) {
        Set<String> allowed = TRANSITIONS.get(from);
        if (allowed == null) {
            return "未知状态: " + from;
        }
        if (!allowed.contains(to)) {
            return String.format("不允许的状态转换: %s → %s", from, to);
        }
        return null;
    }

    /**
     * 判断是否终态（不允许任何出站转换）
     */
    public boolean isFinal(String status) {
        Set<String> allowed = TRANSITIONS.get(status);
        return allowed != null && allowed.isEmpty();
    }
}