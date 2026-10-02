package com.example.vieva.application.ports.output;

import java.util.function.Supplier;

/**
 * Runs a unit of work in its own transaction. Lets long-running use cases (indexing, LLM calls)
 * keep slow external calls outside of database transactions.
 */
public interface TransactionRunnerPort {
    <T> T inNewTransaction(Supplier<T> work);

    default void inNewTransaction(Runnable work) {
        inNewTransaction(() -> {
            work.run();
            return null;
        });
    }
}
