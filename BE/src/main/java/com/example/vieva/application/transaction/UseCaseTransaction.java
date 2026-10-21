package com.example.vieva.application.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Framework-independent transaction boundary annotation for application use cases.
 * Wired to Spring's transaction interceptor in infrastructure/configuration/UseCaseTransactionConfig.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface UseCaseTransaction {

    /**
     * Whether the transaction is read-only.
     */
    boolean readOnly() default false;
}
