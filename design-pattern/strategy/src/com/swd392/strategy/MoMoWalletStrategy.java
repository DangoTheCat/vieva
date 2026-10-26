package com.swd392.strategy;

/** Concrete Strategy (diagram #3): MoMo e-wallet gives 5% off, capped at 20,000 VND. */
public class MoMoWalletStrategy implements PaymentStrategy {

    private static final long DISCOUNT_PERCENT = 5;
    private static final long MAX_DISCOUNT = 20_000;

    @Override
    public long pay(long amount) {
        long discount = Math.min(amount * DISCOUNT_PERCENT / 100, MAX_DISCOUNT);
        long total = amount - discount;
        Trace.log("ConcreteStrategy", "#3", "MoMoWalletStrategy.pay(): "
                + Trace.vnd(amount) + " - " + DISCOUNT_PERCENT + "% off, max " + Trace.vnd(MAX_DISCOUNT)
                + " (" + Trace.vnd(discount) + ") = " + Trace.vnd(total));
        return total;
    }
}
