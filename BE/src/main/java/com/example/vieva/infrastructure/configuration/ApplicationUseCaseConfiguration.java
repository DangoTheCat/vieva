package com.example.vieva.infrastructure.configuration;

import com.example.vieva.application.usecases.access.CourseAccessQueryImpl;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.application.usecases.admin.AdminAnalyticsServiceImpl;
import com.example.vieva.application.usecases.auth.AuthServiceImpl;
import com.example.vieva.application.usecases.catalog.LecturerCatalogServiceImpl;
import com.example.vieva.application.usecases.common.ErrorTranslatorImpl;
import com.example.vieva.application.usecases.document.DocumentServiceImpl;
import com.example.vieva.application.usecases.exam.AttemptGradingServiceImpl;
import com.example.vieva.application.usecases.exam.ExamServiceImpl;
import com.example.vieva.application.usecases.exam.GetExamAttemptUseCaseImpl;
import com.example.vieva.application.usecases.exam.ManageExamConnectionUseCaseImpl;
import com.example.vieva.application.usecases.exam.StartExamSessionUseCaseImpl;
import com.example.vieva.application.usecases.exam.MarkTurnReadyUseCaseImpl;
import com.example.vieva.application.usecases.exam.StartExamUseCaseImpl;
import com.example.vieva.application.usecases.exam.StudentExamServiceImpl;
import com.example.vieva.application.usecases.exam.SubmitTurnAnswerUseCaseImpl;
import com.example.vieva.application.usecases.generation.QuestionGenCoordinatorImpl;
import com.example.vieva.application.usecases.grading.LecturerGradingServiceImpl;
import com.example.vieva.application.usecases.importing.QuestionImportServiceImpl;
import com.example.vieva.application.usecases.importing.TextQuestionImportServiceImpl;
import com.example.vieva.application.usecases.knowledge.KnowledgeSearchServiceImpl;
import com.example.vieva.application.usecases.lecturer.LecturerSubjectServiceImpl;
import com.example.vieva.application.usecases.question.QuestionAccessLoader;
import com.example.vieva.application.usecases.question.QuestionAuthoringServiceImpl;
import com.example.vieva.application.usecases.question.QuestionBankServiceImpl;
import com.example.vieva.application.usecases.question.QuestionReviewServiceImpl;
import com.example.vieva.application.usecases.question.ReviewNotifications;
import com.example.vieva.application.usecases.question.RubricServiceImpl;
import com.example.vieva.application.usecases.question.RubricTemplateServiceImpl;
import com.example.vieva.application.usecases.speech.SpeechSettingsServiceImpl;
import com.example.vieva.application.usecases.subject.SubjectServiceImpl;
import com.example.vieva.application.usecases.notification.NotificationServiceImpl;
import com.example.vieva.application.usecases.user.AdminUserServiceImpl;
import com.example.vieva.application.usecases.user.EnsureDefaultRolesUseCaseImpl;
import com.example.vieva.application.usecases.user.ResolveActorUseCaseImpl;
import com.example.vieva.application.usecases.user.UserServiceImpl;
import com.example.vieva.application.ports.output.*;
import com.example.vieva.application.usecases.exam.AttemptGradingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Composition root for application use cases. The application layer stays framework-free;
 * Spring knows the concrete use cases only at the outer configuration boundary.
 */
@Configuration
@Import({
        ErrorTranslatorImpl.class,
        CourseAccessQueryImpl.class,
        QuestionBankAuditor.class,
        SubjectAccessGuard.class,
        AuthServiceImpl.class,
        LecturerCatalogServiceImpl.class,
        DocumentServiceImpl.class,
        QuestionGenCoordinatorImpl.class,
        QuestionImportServiceImpl.class,
        LecturerSubjectServiceImpl.class,
        QuestionAccessLoader.class,
        QuestionAuthoringServiceImpl.class,
        QuestionBankServiceImpl.class,
        QuestionReviewServiceImpl.class,
        ReviewNotifications.class,
        RubricServiceImpl.class,
        SpeechSettingsServiceImpl.class,
        ExamServiceImpl.class,
        StudentExamServiceImpl.class,
        AdminAnalyticsServiceImpl.class,
        StartExamSessionUseCaseImpl.class,
        MarkTurnReadyUseCaseImpl.class,
        GetExamAttemptUseCaseImpl.class,
        SubjectServiceImpl.class,
        AdminUserServiceImpl.class,
        UserServiceImpl.class,
        NotificationServiceImpl.class,
        ResolveActorUseCaseImpl.class,
        EnsureDefaultRolesUseCaseImpl.class
})
public class ApplicationUseCaseConfiguration {

    @Bean
    public StartExamUseCaseImpl startExamUseCase(
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            QuestionRepository questionRepository,
            QuestionVersionRepository questionVersionRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamTurnRepository examTurnRepository,
            ExamAttemptQuestionSnapshotRepository snapshotRepository,
            RubricRepository rubricRepository,
            RubricCriterionRepository rubricCriterionRepository,
            JsonSerializerPort jsonSerializerPort
    ) {
        return new StartExamUseCaseImpl(
                userRepository,
                subjectRepository,
                questionRepository,
                questionVersionRepository,
                examAttemptRepository,
                examTurnRepository,
                snapshotRepository,
                rubricRepository,
                rubricCriterionRepository,
                jsonSerializerPort
        );
    }

    @Bean
    public ManageExamConnectionUseCaseImpl manageExamConnectionUseCase(
            ExamAttemptRepository examAttemptRepository,
            ExamTurnRepository examTurnRepository,
            AttemptGradingService attemptGradingService,
            ExamFinalizationPort examFinalizationPort
    ) {
        return new ManageExamConnectionUseCaseImpl(
                examAttemptRepository,
                examTurnRepository,
                attemptGradingService,
                examFinalizationPort
        );
    }

    @Bean
    public SubmitTurnAnswerUseCaseImpl submitTurnAnswerUseCase(
            ExamAttemptRepository examAttemptRepository,
            ExamTurnRepository examTurnRepository,
            QuestionRepository questionRepository,
            QuestionVersionRepository questionVersionRepository,
            RubricRepository rubricRepository,
            FileStoragePort fileStoragePort,
            VivaEvaluationGateway vivaEvaluationGateway,
            AttemptGradingService attemptGradingService,
            ExamAttemptQuestionSnapshotRepository snapshotRepository,
            ExamFinalizationPort examFinalizationPort,
            CourseDocumentRepository courseDocumentRepository,
            DocumentChunkRepository documentChunkRepository,
            EmbeddingModelPort embeddingModelPort
    ) {
        return new SubmitTurnAnswerUseCaseImpl(
                examAttemptRepository,
                examTurnRepository,
                questionRepository,
                questionVersionRepository,
                rubricRepository,
                fileStoragePort,
                vivaEvaluationGateway,
                attemptGradingService,
                snapshotRepository,
                examFinalizationPort,
                courseDocumentRepository,
                documentChunkRepository,
                embeddingModelPort
        );
    }

    @Bean
    public KnowledgeSearchServiceImpl knowledgeSearchService(
            CourseDocumentRepository courseDocumentRepository,
            DocumentChunkRepository documentChunkRepository,
            EmbeddingModelPort embeddingModel,
            SubjectAccessGuard subjectAccessGuard,
            TopicRepository topicRepository,
            TextSplitterPort textSplitter,
            FileStoragePort fileStoragePort
    ) {
        return new KnowledgeSearchServiceImpl(
                courseDocumentRepository,
                documentChunkRepository,
                embeddingModel,
                subjectAccessGuard,
                topicRepository,
                textSplitter,
                fileStoragePort
        );
    }

    @Bean
    public LecturerGradingServiceImpl lecturerGradingService(
            SubjectRepository subjectRepository,
            LecturerSubjectRepository lecturerSubjectRepository,
            ExamRepository examRepository,
            ExamQuestionRepository examQuestionRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamAttemptQuestionSnapshotRepository snapshotRepository,
            ExamTurnRepository examTurnRepository,
            ExamScoreRepository examScoreRepository,
            UserRepository userRepository,
            SubjectAccessGuard accessGuard,
            QuestionBankAuditor auditor
    ) {
        return new LecturerGradingServiceImpl(
                subjectRepository,
                lecturerSubjectRepository,
                examRepository,
                examQuestionRepository,
                examAttemptRepository,
                snapshotRepository,
                examTurnRepository,
                examScoreRepository,
                userRepository,
                accessGuard,
                auditor
        );
    }

    @Bean
    public RubricTemplateServiceImpl rubricTemplateService(
            RubricTemplateRepository rubricTemplateRepository,
            RubricRepository rubricRepository,
            RubricCriterionRepository rubricCriterionRepository,
            QuestionVersionRepository questionVersionRepository,
            QuestionRepository questionRepository,
            JsonSerializerPort jsonSerializerPort,
            SubjectAccessGuard accessGuard,
            QuestionBankAuditor auditor
    ) {
        return new RubricTemplateServiceImpl(
                rubricTemplateRepository,
                rubricRepository,
                rubricCriterionRepository,
                questionVersionRepository,
                questionRepository,
                jsonSerializerPort,
                accessGuard,
                auditor
        );
    }

    @Bean
    public TextQuestionImportServiceImpl textQuestionImportService(
            SubjectRepository subjectRepository,
            TopicRepository topicRepository,
            RubricTemplateRepository rubricTemplateRepository,
            QuestionDraftWriter draftWriter,
            JsonSerializerPort jsonSerializerPort,
            SubjectAccessGuard accessGuard,
            QuestionBankAuditor auditor
    ) {
        return new TextQuestionImportServiceImpl(
                subjectRepository,
                topicRepository,
                rubricTemplateRepository,
                draftWriter,
                jsonSerializerPort,
                accessGuard,
                auditor
        );
    }

    @Bean
    public AttemptGradingServiceImpl attemptGradingService(
            AttemptGradingGateway attemptGradingGateway,
            RuleBasedGraderPort ruleBasedGraderPort,
            RubricRepository rubricRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamScoreRepository examScoreRepository,
            ExamAttemptQuestionSnapshotRepository snapshotRepository
    ) {
        return new AttemptGradingServiceImpl(
                attemptGradingGateway,
                ruleBasedGraderPort,
                rubricRepository,
                examAttemptRepository,
                examScoreRepository,
                snapshotRepository
        );
    }
}
