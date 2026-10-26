package com.swd392.strategy;

/** Client (diagram #5): picks a concrete strategy and hands it to the context. */
public class Main {

    public static void main(String[] args) {
        long orderAmount = 500_000;
        System.out.println("=== Strategy Pattern demo: checkout payment ===");
        System.out.println("Order amount: " + Trace.vnd(orderAmount));

        scenario("1) Customer picks Cash on Delivery");
        Trace.log("Client", "#5", "context = new PaymentContext(new CashOnDeliveryStrategy())");
        PaymentContext context = new PaymentContext(new CashOnDeliveryStrategy());
        pay(context, orderAmount);

        scenario("2) Customer switches to Credit Card at runtime");
        Trace.log("Client", "#5", "context.setStrategy(new CreditCardStrategy())");
        context.setStrategy(new CreditCardStrategy());
        pay(context, orderAmount);

        scenario("3) Customer switches to MoMo wallet");
        Trace.log("Client", "#5", "context.setStrategy(new MoMoWalletStrategy())");
        context.setStrategy(new MoMoWalletStrategy());
        pay(context, orderAmount);

        scenario("4) ZaloPayStrategy added later - PaymentContext unchanged (Open/Closed)");
        Trace.log("Client", "#5", "context.setStrategy(new ZaloPayStrategy())");
        context.setStrategy(new ZaloPayStrategy());
        pay(context, orderAmount);
    }

    private static void scenario(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }

    private static void pay(PaymentContext context, long amount) {
        long total = context.checkout(amount);
        Trace.log("Client", "", "customer pays " + Trace.vnd(total));
    }
}
