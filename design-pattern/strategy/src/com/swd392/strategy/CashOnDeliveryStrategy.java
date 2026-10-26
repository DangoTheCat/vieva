package com.swd392.strategy;

/** Concrete Strategy (diagram #3): cash on delivery adds a flat handling fee. */
public class CashOnDeliveryStrategy implements PaymentStrategy {

    private static final long COD_FEE = 15_000;

    @Override
    public long pay(long amount) {
        long total = amount + COD_FEE;
        Trace.log("ConcreteStrategy", "#3", "CashOnDeliveryStrategy.pay(): "
                + Trace.vnd(amount) + " + " + Trace.vnd(COD_FEE) + " COD fee = " + Trace.vnd(total));
        return total;
    }
}
