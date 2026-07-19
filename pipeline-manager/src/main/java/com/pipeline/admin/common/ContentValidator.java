package com.pipeline.admin.common;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 内容安全校验工具，用于敏感词过滤和 Prompt 注入防护。
 * 符合技术文档 10.3 节安全设计要求。
 */
@Slf4j
public class ContentValidator {

    private static final List<String> SENSITIVE_WORDS = List.of();

    private static final List<Pattern> PROMPT_INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore\\s+(all\\s+)?(previous|above|below)\\s+instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("forget\\s+(all\\s+)?(previous|above|below)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard\\s+(all\\s+)?(previous|above|below)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+(not\\s+)?(an?\\s+)?(AI|assistant|chatbot)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("role\\s*(play|playact|pretend)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system\\s+(prompt|instruction|message)", Pattern.CASE_INSENSITIVE)
    );

    /**
     * 检查文本是否包含敏感词。
     *
     * @return 如果包含敏感词返回第一个匹配的敏感词，否则返回 null
     */
    public static String checkSensitiveWords(String text) {
        if (text == null || text.isEmpty()) return null;
        String lower = text.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lower.contains(word.toLowerCase())) {
                log.warn("检测到敏感词: {}", word);
                return word;
            }
        }
        return null;
    }

    /**
     * 检查是否存在 Prompt 注入攻击。
     *
     * @return 如果检测到注入返回 true
     */
    public static boolean hasPromptInjection(String text) {
        if (text == null || text.isEmpty()) return false;
        for (Pattern pattern : PROMPT_INJECTION_PATTERNS) {
            if (pattern.matcher(text).find()) {
                log.warn("检测到 Prompt 注入: {}", pattern.pattern());
                return true;
            }
        }
        return false;
    }

    /**
     * 校验内容是否安全，同时检查敏感词和 Prompt 注入。
     *
     * @return 校验通过返回 null，否则返回错误描述
     */
    public static String validate(String text) {
        if (text == null || text.isEmpty()) return null;
        String sensitive = checkSensitiveWords(text);
        if (sensitive != null) {
            return "内容包含敏感词: " + sensitive;
        }
        if (hasPromptInjection(text)) {
            return "检测到 Prompt 注入攻击";
        }
        return null;
    }
}