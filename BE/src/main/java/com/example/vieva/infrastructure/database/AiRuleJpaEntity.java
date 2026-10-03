package com.example.vieva.infrastructure.database;

import com.example.vieva.domain.entities.AiRuleType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Quy tắc / prompt AI (bảng {@code ai_rules}, tạo bởi V9). Chỉ đọc từ ứng dụng; dữ liệu do migration seed.
 */
@Entity
@Table(name = "ai_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRuleJpaEntity {

    @Id
    @Column(name = "rule_id", updatable = false, nullable = false)
    private UUID ruleId;

    @Column(name = "rule_code", nullable = false, unique = true, length = 50)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private AiRuleType ruleType;

    @Column(name = "prompt_content", nullable = false, columnDefinition = "TEXT")
    private String promptContent;

    @Column(name = "temperature", nullable = false, precision = 3, scale = 2)
    private BigDecimal temperature;

    @Column(name = "max_tokens", nullable = false)
    private Integer maxTokens;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
