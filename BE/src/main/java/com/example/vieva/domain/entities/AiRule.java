package com.example.vieva.domain.entities;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Quy tắc / prompt mà tầng AI đọc lúc chạy, tra theo {@code ruleCode}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRule {
    private UUID ruleId;
    private String ruleCode;
    private String ruleName;
    private AiRuleType ruleType;
    private String promptContent;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Boolean active;
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;
}
