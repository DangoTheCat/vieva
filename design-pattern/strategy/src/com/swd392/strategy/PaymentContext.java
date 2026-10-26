package com.swd392.strategy;

/**
 * Context (diagram #1): keeps a reference to one strategy and delegates the
 * payment calculation to it through the PaymentStrategy interface only.
 */
public class PaymentContext {

    private PaymentStrategy strategy;

    public PaymentContext(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    /** Lets the client replace the algorithm at runtime (diagram #5). */
    public void setStrategy(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Diagram #4: runs whichever strategy is linked. The context does not know
     * the concrete type, so this method never changes when a new strategy is added.
     */
    public long checkout(long amount) {
        Trace.log("Context", "#1 #4", "checkout(" + Trace.vnd(amount) + ") -> strategy.pay(amount)");
        return strategy.pay(amount);
    }
}
