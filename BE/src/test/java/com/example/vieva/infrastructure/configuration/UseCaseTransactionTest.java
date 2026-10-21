package com.example.vieva.infrastructure.configuration;

import com.example.vieva.application.transaction.UseCaseTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UseCaseTransactionTest {

    static class RecordingTransactionManager implements PlatformTransactionManager {
        final List<String> events = new ArrayList<>();
        boolean lastReadOnly = false;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            lastReadOnly = definition.isReadOnly();
            events.add("BEGIN" + (definition.isReadOnly() ? "(readOnly)" : ""));
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
            events.add("COMMIT");
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
            events.add("ROLLBACK");
        }
    }

    static class TestTransactionalService {
        @UseCaseTransaction
        public void success() {
            // normal execution
        }

        @UseCaseTransaction
        public void failWithRuntimeException() {
            throw new IllegalStateException("Simulated business failure");
        }

        @UseCaseTransaction(readOnly = true)
        public String readOnlyOperation() {
            return "read-only-result";
        }
    }

    @UseCaseTransaction
    static class TestClassLevelTransactionalService {
        public void classLevelSuccess() {
        }

        public void classLevelFailure() {
            throw new RuntimeException("Class level failure");
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig extends UseCaseTransactionConfig {
        @Bean
        public RecordingTransactionManager transactionManager() {
            return new RecordingTransactionManager();
        }

        @Bean
        public TestTransactionalService testTransactionalService() {
            return new TestTransactionalService();
        }

        @Bean
        public TestClassLevelTransactionalService testClassLevelTransactionalService() {
            return new TestClassLevelTransactionalService();
        }
    }

    @Test
    @DisplayName("Method annotated with @UseCaseTransaction commits on success")
    void commitsOnSuccess() {
        try (var ctx = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var service = ctx.getBean(TestTransactionalService.class);
            var txManager = ctx.getBean(RecordingTransactionManager.class);

            service.success();

            assertThat(txManager.events).containsExactly("BEGIN", "COMMIT");
            assertThat(txManager.lastReadOnly).isFalse();
        }
    }

    @Test
    @DisplayName("Method annotated with @UseCaseTransaction rolls back on RuntimeException")
    void rollsBackOnRuntimeException() {
        try (var ctx = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var service = ctx.getBean(TestTransactionalService.class);
            var txManager = ctx.getBean(RecordingTransactionManager.class);

            assertThatThrownBy(service::failWithRuntimeException)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Simulated business failure");

            assertThat(txManager.events).containsExactly("BEGIN", "ROLLBACK");
        }
    }

    @Test
    @DisplayName("Method annotated with @UseCaseTransaction(readOnly = true) sets readOnly flag")
    void setsReadOnlyFlag() {
        try (var ctx = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var service = ctx.getBean(TestTransactionalService.class);
            var txManager = ctx.getBean(RecordingTransactionManager.class);

            String result = service.readOnlyOperation();

            assertThat(result).isEqualTo("read-only-result");
            assertThat(txManager.events).containsExactly("BEGIN(readOnly)", "COMMIT");
            assertThat(txManager.lastReadOnly).isTrue();
        }
    }

    @Test
    @DisplayName("Class-level @UseCaseTransaction applies transaction and rolls back on failure")
    void classLevelTransactionWorks() {
        try (var ctx = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var service = ctx.getBean(TestClassLevelTransactionalService.class);
            var txManager = ctx.getBean(RecordingTransactionManager.class);

            service.classLevelSuccess();
            assertThat(txManager.events).containsExactly("BEGIN", "COMMIT");

            txManager.events.clear();
            assertThatThrownBy(service::classLevelFailure)
                    .isInstanceOf(RuntimeException.class);
            assertThat(txManager.events).containsExactly("BEGIN", "ROLLBACK");
        }
    }
}
