package com.example.vieva.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubjectTest {

    @Test
    @DisplayName("assignHead: sets head lecturer and updates updatedAt")
    void assignHeadSetsLecturerAndTimestamp() {
        Subject subject = Subject.builder()
                .subjectId(UUID.randomUUID())
                .subjectCode("SWD392")
                .subjectName("Software Architecture")
                .status(SubjectStatus.ACTIVE)
                .build();

        UUID headId = UUID.randomUUID();
        subject.assignHead(headId);

        assertThat(subject.getHeadLecturerId()).isEqualTo(headId);
        assertThat(subject.getUpdatedAt()).isNotNull();
    }
}
