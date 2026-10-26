package com.swd392.strategy;

/**
 * Strategy (diagram #2): the common interface for every payment algorithm.
 * PaymentContext only talks to this type, never to a concrete class.
 */
public interface PaymentStrategy {

    /** Runs this payment algorithm and returns the final amount the customer pays. */
    long pay(long amount);
}
