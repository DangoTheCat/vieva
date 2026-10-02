package com.example.vieva.infrastructure.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuestionJpaRepository extends JpaRepository<QuestionJpaEntity, UUID>, JpaSpecificationExecutor<QuestionJpaEntity> {
}
