package com.example.vieva.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubjectTest {

    @Test
    @DisplayName("Subject creation and property verification without head lecturer")
    void subjectCreationAndProperties() {
        UUID subjectId = UUID.randomUUID();
        Instant now = Instant.now();
        Subject subject = Subject.builder()
                .subjectId(subjectId)
                .subjectCode("SWD392")
                .subjectName("Software Architecture")
                .description("Architecture patterns and design principles")
                .credits(3)
                .status(SubjectStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertThat(subject.getSubjectId()).isEqualTo(subjectId);
        assertThat(subject.getSubjectCode()).isEqualTo("SWD392");
        assertThat(subject.getSubjectName()).isEqualTo("Software Architecture");
        assertThat(subject.getDescription()).isEqualTo("Architecture patterns and design principles");
        assertThat(subject.getCredits()).isEqualTo(3);
        assertThat(subject.getStatus()).isEqualTo(SubjectStatus.ACTIVE);
        assertThat(subject.getCreatedAt()).isEqualTo(now);
        assertThat(subject.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Subject status transition to INACTIVE")
    void subjectStatusTransition() {
        Subject subject = Subject.builder()
                .subjectId(UUID.randomUUID())
                .subjectCode("PRN211")
                .subjectName("Basic Cross-Platform")
                .status(SubjectStatus.ACTIVE)
                .build();

        Subject deactivated = subject.toBuilder()
                .status(SubjectStatus.INACTIVE)
                .updatedAt(Instant.now())
                .build();

        assertThat(deactivated.getStatus()).isEqualTo(SubjectStatus.INACTIVE);
        assertThat(deactivated.getUpdatedAt()).isNotNull();
    }
}
