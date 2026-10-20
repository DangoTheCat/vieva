package com.example.vieva.infrastructure.configuration;

import com.example.vieva.application.usecases.access.CourseAccessQueryImpl;
import com.example.vieva.application.usecases.access.QuestionBankAuditor;
import com.example.vieva.application.usecases.access.SubjectAccessGuard;
import com.example.vieva.application.usecases.access.SubjectHeadServiceImpl;
import com.example.vieva.application.usecases.auth.AuthServiceImpl;
import com.example.vieva.application.usecases.catalog.LecturerCatalogServiceImpl;
import com.example.vieva.application.usecases.common.ErrorTranslatorImpl;
import com.example.vieva.application.usecases.document.DocumentServiceImpl;
import com.example.vieva.application.usecases.generation.QuestionGenCoordinatorImpl;
import com.example.vieva.application.usecases.importing.QuestionImportServiceImpl;
import com.example.vieva.application.usecases.lecturer.LecturerSubjectServiceImpl;
import com.example.vieva.application.usecases.question.QuestionAccessLoader;
import com.example.vieva.application.usecases.question.QuestionAuthoringServiceImpl;
import com.example.vieva.application.usecases.question.QuestionBankServiceImpl;
import com.example.vieva.application.usecases.question.QuestionReviewServiceImpl;
import com.example.vieva.application.usecases.question.ReviewNotifications;
import com.example.vieva.application.usecases.question.RubricServiceImpl;
import com.example.vieva.application.usecases.subject.SubjectServiceImpl;
import com.example.vieva.application.usecases.notification.NotificationServiceImpl;
import com.example.vieva.application.usecases.user.AdminUserServiceImpl;
import com.example.vieva.application.usecases.user.EnsureDefaultRolesUseCaseImpl;
import com.example.vieva.application.usecases.user.ResolveActorUseCaseImpl;
import com.example.vieva.application.usecases.user.UserServiceImpl;
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
        SubjectHeadServiceImpl.class,
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
        SubjectServiceImpl.class,
        AdminUserServiceImpl.class,
        UserServiceImpl.class,
        NotificationServiceImpl.class,
        ResolveActorUseCaseImpl.class,
        EnsureDefaultRolesUseCaseImpl.class
})
public class ApplicationUseCaseConfiguration { }
