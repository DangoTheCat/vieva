package com.example.vieva.infrastructure.database;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Builds the Hibernate metamodel/session factory offline (no database required) to make
 * sure every JPA mapping — including the custom {@link PgVectorType} on
 * {@code document_chunks.embedding} — resolves correctly.
 */
class HibernateMappingSmokeTest {

    private static final Class<?>[] ENTITIES = {
            UserJpaEntity.class,
            RoleJpaEntity.class,
            UserRoleJpaEntity.class,
            AuditEventJpaEntity.class,
            CourseDocumentJpaEntity.class,
            DocumentChunkJpaEntity.class,
            LecturerSubjectJpaEntity.class,
            QuestionJpaEntity.class,
            QuestionGenerationRequestJpaEntity.class,
            QuestionSourceJpaEntity.class,
            QuestionVersionJpaEntity.class,
            RubricCriterionJpaEntity.class,
            RubricJpaEntity.class,
            SpeechConfigVersionJpaEntity.class,
            SubjectJpaEntity.class,
            TopicJpaEntity.class
    };

    @Test
    void hibernateMetamodelBuilds() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .build();
        try {
            MetadataSources sources = new MetadataSources(registry);
            for (Class<?> entity : ENTITIES) {
                sources.addAnnotatedClass(entity);
            }
            Metadata metadata = sources.buildMetadata();
            assertNotNull(metadata);

            try (SessionFactory sessionFactory = metadata.buildSessionFactory()) {
                assertNotNull(sessionFactory);
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
