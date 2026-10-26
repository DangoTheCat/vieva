package com.swd392.strategy.before;

/**
 * The problem Strategy solves: every payment rule lives in one if/else chain.
 * Adding a new method (e.g. ZaloPay) means editing and re-testing this class.
 */
public class CheckoutWithoutPattern {

    public long checkout(long amount, String method) {
        if ("COD".equals(method)) {
            return amount + 15_000;
        } else if ("CARD".equals(method)) {
            return amount + amount * 2 / 100;
        } else if ("MOMO".equals(method)) {
            return amount - Math.min(amount * 5 / 100, 20_000);
        } else {
            throw new IllegalArgumentException("Unsupported payment method: " + method);
        }
    }
}
