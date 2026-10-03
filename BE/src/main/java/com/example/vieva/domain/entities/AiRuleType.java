package com.example.vieva.domain.entities;

/**
 * Nhóm quy tắc AI; khớp CHECK constraint của bảng {@code ai_rules}.
 */
public enum AiRuleType {
    SYSTEM_PROMPT,
    SAFETY_RULE,
    ADAPTIVE_PROBING_RULE,
    RUBRIC_GRADING_RULE,
    CONTEXT_FILTER
}
