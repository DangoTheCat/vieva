package com.example.vieva.infrastructure.database;

import com.example.vieva.application.ports.output.DuplicateKeyException;
import com.example.vieva.domain.entities.LecturerSubject;
import com.example.vieva.domain.entities.QuestionVersion;
import com.example.vieva.domain.entities.Subject;
import com.example.vieva.domain.entities.Topic;
import com.example.vieva.domain.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DuplicateKeyExceptionTranslationTest {

    @Test
    @DisplayName("TopicRepositoryImpl.saveAndFlush translates DataIntegrityViolationException to DuplicateKeyException")
    void topicRepositorySaveAndFlushTranslates() {
        TopicJpaRepository jpa = mock(TopicJpaRepository.class);
        TopicPersistenceMapper mapper = mock(TopicPersistenceMapper.class);
        when(mapper.toEntity(any())).thenReturn(new TopicJpaEntity());
        when(jpa.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate topic"));

        TopicRepositoryImpl repo = new TopicRepositoryImpl(jpa, mapper);
        Topic topic = Topic.builder().topicId(UUID.randomUUID()).topicName("Test").build();

        assertThatThrownBy(() -> repo.saveAndFlush(topic))
                .isInstanceOf(DuplicateKeyException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("TopicRepositoryImpl.saveAllAndFlush translates DataIntegrityViolationException to DuplicateKeyException")
    void topicRepositorySaveAllAndFlushTranslates() {
        TopicJpaRepository jpa = mock(TopicJpaRepository.class);
        TopicPersistenceMapper mapper = mock(TopicPersistenceMapper.class);
        when(mapper.toEntity(any())).thenReturn(new TopicJpaEntity());
        when(jpa.saveAllAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate topic batch"));

        TopicRepositoryImpl repo = new TopicRepositoryImpl(jpa, mapper);
        Topic topic = Topic.builder().topicId(UUID.randomUUID()).topicName("Test").build();

        assertThatThrownBy(() -> repo.saveAllAndFlush(List.of(topic)))
                .isInstanceOf(DuplicateKeyException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("UserRepositoryImpl.saveAndFlush translates DataIntegrityViolationException to DuplicateKeyException")
    void userRepositorySaveAndFlushTranslates() {
        UserJpaRepository jpa = mock(UserJpaRepository.class);
        UserPersistenceMapper mapper = mock(UserPersistenceMapper.class);
        when(mapper.toEntity(any())).thenReturn(new UserJpaEntity());
        when(jpa.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate email"));

        UserRepositoryImpl repo = new UserRepositoryImpl(jpa, mapper);
        User user = User.builder().userId(UUID.randomUUID()).email("a@fpt.edu.vn").build();

        assertThatThrownBy(() -> repo.saveAndFlush(user))
                .isInstanceOf(DuplicateKeyException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("SubjectRepositoryImpl.saveAndFlush translates DataIntegrityViolationException to DuplicateKeyException")
    void subjectRepositorySaveAndFlushTranslates() {
        SubjectJpaRepository jpa = mock(SubjectJpaRepository.class);
        SubjectPersistenceMapper mapper = mock(SubjectPersistenceMapper.class);
        when(mapper.toEntity(any())).thenReturn(new SubjectJpaEntity());
        when(jpa.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate subject code"));

        SubjectRepositoryImpl repo = new SubjectRepositoryImpl(jpa, mapper);
        Subject subject = Subject.builder().subjectId(UUID.randomUUID()).subjectCode("SWD392").build();

        assertThatThrownBy(() -> repo.saveAndFlush(subject))
                .isInstanceOf(DuplicateKeyException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("QuestionVersionRepositoryImpl.save translates DataIntegrityViolationException to DuplicateKeyException")
    void questionVersionRepositorySaveTranslates() {
        QuestionVersionJpaRepository jpa = mock(QuestionVersionJpaRepository.class);
        QuestionJpaRepository questionJpa = mock(QuestionJpaRepository.class);
        QuestionVersionPersistenceMapper mapper = mock(QuestionVersionPersistenceMapper.class);
        when(jpa.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate draft"));

        QuestionVersionRepositoryImpl repo = new QuestionVersionRepositoryImpl(jpa, questionJpa, mapper);
        QuestionVersion version = QuestionVersion.builder().questionVersionId(UUID.randomUUID()).build();

        assertThatThrownBy(() -> repo.save(version))
                .isInstanceOf(DuplicateKeyException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("LecturerSubjectRepositoryImpl.saveAll translates DataIntegrityViolationException to DuplicateKeyException")
    void lecturerSubjectRepositorySaveAllTranslates() {
        LecturerSubjectJpaRepository jpa = mock(LecturerSubjectJpaRepository.class);
        LecturerSubjectPersistenceMapper mapper = mock(LecturerSubjectPersistenceMapper.class);
        when(mapper.toEntity(any())).thenReturn(new LecturerSubjectJpaEntity());
        when(jpa.saveAll(any())).thenThrow(new DataIntegrityViolationException("duplicate assignment"));

        LecturerSubjectRepositoryImpl repo = new LecturerSubjectRepositoryImpl(jpa, mapper);
        LecturerSubject assignment = LecturerSubject.builder().lecturerSubjectId(UUID.randomUUID()).build();

        assertThatThrownBy(() -> repo.saveAll(List.of(assignment)))
                .isInstanceOf(DuplicateKeyException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }
}
