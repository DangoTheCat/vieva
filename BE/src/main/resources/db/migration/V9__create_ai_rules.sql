-- ============================================================================
-- V9: AI rules — prompt / rule store the AI layer reads at runtime
--   * One row per rule, looked up by rule_code (active rows only)
--   * rule_type groups rules: system prompt, safety, adaptive probing,
--     rubric grading, context filter
--   * temperature / max_tokens: per-rule LLM call parameters
-- ============================================================================

CREATE TABLE IF NOT EXISTS ai_rules (
    rule_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_code       VARCHAR(50)  NOT NULL UNIQUE,
    rule_name       VARCHAR(150) NOT NULL,
    rule_type       VARCHAR(50)  NOT NULL CHECK (rule_type IN ('SYSTEM_PROMPT', 'SAFETY_RULE', 'ADAPTIVE_PROBING_RULE', 'RUBRIC_GRADING_RULE', 'CONTEXT_FILTER')),
    prompt_content  TEXT         NOT NULL,
    temperature     NUMERIC(3,2) NOT NULL DEFAULT 0.70,
    max_tokens      INTEGER      NOT NULL DEFAULT 2048,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    version         INTEGER      NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_rules_code_active ON ai_rules(rule_code, is_active);
CREATE INDEX IF NOT EXISTS idx_ai_rules_type ON ai_rules(rule_type);
