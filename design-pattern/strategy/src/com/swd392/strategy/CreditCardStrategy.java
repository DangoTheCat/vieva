package com.swd392.strategy;

/** Concrete Strategy (diagram #3): credit card adds a 2% processing fee. */
public class CreditCardStrategy implements PaymentStrategy {

    private static final long FEE_PERCENT = 2;

    @Override
    public long pay(long amount) {
        long fee = amount * FEE_PERCENT / 100;
        long total = amount + fee;
        Trace.log("ConcreteStrategy", "#3", "CreditCardStrategy.pay(): "
                + Trace.vnd(amount) + " + " + FEE_PERCENT + "% card fee (" + Trace.vnd(fee) + ") = " + Trace.vnd(total));
        return total;
    }
}
