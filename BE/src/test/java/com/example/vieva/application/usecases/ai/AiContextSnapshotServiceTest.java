package com.example.vieva.application.usecases.ai;

import com.example.vieva.application.ports.output.AiRuleRepository;
import com.example.vieva.application.ports.output.SubjectRepository;
import com.example.vieva.application.ports.output.TopicRepository;
import com.example.vieva.domain.entities.AiRule;
import com.example.vieva.domain.entities.AiRuleType;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.SubjectStatus;
import com.example.vieva.domain.entities.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiContextSnapshotServiceTest {

    private final SubjectRepository subjectRepository = mock(SubjectRepository.class);
    private final TopicRepository topicRepository = mock(TopicRepository.class);
    private final AiRuleRepository aiRuleRepository = mock(AiRuleRepository.class);
    private final AiContextSnapshotService service =
            new AiContextSnapshotService(subjectRepository, topicRepository, aiRuleRepository);

    private final UUID subjectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(subjectRepository.findAllByStatus(SubjectStatus.ACTIVE)).thenReturn(List.of(Subject.builder()
                .subjectId(subjectId).subjectCode("SWD392").subjectName("Kiến trúc phần mềm")
                .description("Clean Architecture").credits(3).build()));
        when(topicRepository.findBySubjectIds(List.of(subjectId))).thenReturn(List.of(
                Topic.builder().subjectId(subjectId).topicName("Onion Architecture").description("Phân tầng đồng tâm").build()));
        when(aiRuleRepository.findActiveByType(AiRuleType.CONTEXT_FILTER)).thenReturn(List.of(
                AiRule.builder().ruleCode("EXAM_REGULATIONS").promptContent("Cửa sổ cứu vớt mất mạng: 60 giây").build()));
    }

    @Test
    @DisplayName("snapshot lists active subjects with topics, context rules and the grounding rule")
    void buildsSnapshot() {
        AiContextSnapshotService.CachedSnapshot snapshot = service.refresh();

        assertThat(snapshot.generatedAt()).isNotNull();
        assertThat(snapshot.content())
                .startsWith(AiContextSnapshotService.HEADER)
                .contains("- SWD392 — Kiến trúc phần mềm (3 tín chỉ)")
                .contains("Mô tả: Clean Architecture")
                .contains("1. Onion Architecture: Phân tầng đồng tâm")
                .contains("Cửa sổ cứu vớt mất mạng: 60 giây")
                .contains(AiContextSnapshotService.GROUNDING_RULE)
                .endsWith(AiContextSnapshotService.FOOTER);
    }

    @Test
    @DisplayName("snapshot says so when no subject is active")
    void noActiveSubject() {
        when(subjectRepository.findAllByStatus(SubjectStatus.ACTIVE)).thenReturn(List.of());

        assertThat(service.refresh().content()).contains("Chưa có môn học nào đang mở.");
    }

    @Test
    @DisplayName("current() builds lazily once, then serves the cached snapshot")
    void currentIsLazyAndCached() {
        AiContextSnapshotService.CachedSnapshot first = service.current();
        AiContextSnapshotService.CachedSnapshot second = service.current();

        assertThat(second).isSameAs(first);
        verify(subjectRepository, times(1)).findAllByStatus(SubjectStatus.ACTIVE);
    }

    @Test
    @DisplayName("a failed refresh keeps the previous snapshot")
    void failedRefreshKeepsPrevious() {
        AiContextSnapshotService.CachedSnapshot previous = service.refresh();
        when(subjectRepository.findAllByStatus(SubjectStatus.ACTIVE)).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(service::refresh).isInstanceOf(IllegalStateException.class);
        assertThat(service.current()).isSameAs(previous);
    }
}
