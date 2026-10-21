package com.example.vieva.infrastructure.configuration;

import com.example.vieva.application.transaction.UseCaseTransaction;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.TransactionAnnotationParser;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;

/**
 * Connects application @UseCaseTransaction annotations to Spring's transaction infrastructure.
 * Mapped 1-to-1 to RuleBasedTransactionAttribute with identical semantics to @Transactional:
 * - readOnly attribute supported
 * - automatic rollback on RuntimeException and Error
 * - method-level and class-level declaration
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class UseCaseTransactionConfig {

    public static class UseCaseTransactionAnnotationParser implements TransactionAnnotationParser, Serializable {

        @Override
        public boolean isCandidateClass(Class<?> targetClass) {
            return AnnotationUtils.isCandidateClass(targetClass, UseCaseTransaction.class);
        }

        @Override
        public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
            UseCaseTransaction ann = AnnotatedElementUtils.findMergedAnnotation(element, UseCaseTransaction.class);
            if (ann == null) {
                return null;
            }
            RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
            rbta.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            rbta.setIsolationLevel(TransactionDefinition.ISOLATION_DEFAULT);
            rbta.setTimeout(TransactionDefinition.TIMEOUT_DEFAULT);
            rbta.setReadOnly(ann.readOnly());
            rbta.getRollbackRules().add(new RollbackRuleAttribute(RuntimeException.class));
            return rbta;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof UseCaseTransactionAnnotationParser;
        }

        @Override
        public int hashCode() {
            return UseCaseTransactionAnnotationParser.class.hashCode();
        }
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public TransactionAttributeSource useCaseTransactionAttributeSource() {
        return new AnnotationTransactionAttributeSource(new UseCaseTransactionAnnotationParser());
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public TransactionInterceptor useCaseTransactionInterceptor(TransactionAttributeSource useCaseTransactionAttributeSource) {
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionAttributeSource(useCaseTransactionAttributeSource);
        return interceptor;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public BeanFactoryTransactionAttributeSourceAdvisor useCaseTransactionAdvisor(
            TransactionAttributeSource useCaseTransactionAttributeSource,
            TransactionInterceptor useCaseTransactionInterceptor) {
        BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();
        advisor.setTransactionAttributeSource(useCaseTransactionAttributeSource);
        advisor.setAdvice(useCaseTransactionInterceptor);
        return advisor;
    }
}
