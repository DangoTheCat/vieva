package com.example.vieva.infrastructure.service;

import com.example.vieva.application.ports.output.TransactionRunnerPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Declarative REQUIRES_NEW boundary. Declarative (proxy) instead of TransactionTemplate so the
 * bean can be created even when no transaction manager exists (slice tests).
 * Both overloads are annotated: the interface's default {@code Runnable} variant would otherwise
 * call the {@code Supplier} variant on {@code this}, bypassing the transactional proxy.
 */
@Component
public class SpringTransactionRunnerGateway implements TransactionRunnerPort {

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T inNewTransaction(Supplier<T> work) {
        return work.get();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inNewTransaction(Runnable work) {
        work.run();
    }
}
