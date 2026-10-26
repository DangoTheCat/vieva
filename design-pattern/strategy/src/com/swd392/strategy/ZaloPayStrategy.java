package com.swd392.strategy;

/**
 * Concrete Strategy (diagram #3) added after the others.
 * Adding it did not change PaymentContext or any other strategy (Open/Closed Principle).
 */
public class ZaloPayStrategy implements PaymentStrategy {

    private static final long VOUCHER = 10_000;
    private static final long MIN_ORDER = 200_000;

    @Override
    public long pay(long amount) {
        long discount = amount >= MIN_ORDER ? VOUCHER : 0;
        long total = amount - discount;
        Trace.log("ConcreteStrategy", "#3", "ZaloPayStrategy.pay(): "
                + Trace.vnd(amount) + " - " + Trace.vnd(discount) + " voucher (order >= "
                + Trace.vnd(MIN_ORDER) + ") = " + Trace.vnd(total));
        return total;
    }
}
