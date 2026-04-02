package za.ac.tut.payment;

public final class PaymentProviderFactory {

    private static final String MODE_KEY = "tickify.payment.mode";

    private PaymentProviderFactory() {
    }

    public static PaymentProvider resolveProvider() {
        String mode = System.getProperty(MODE_KEY, "demo").trim().toLowerCase();
        if ("payfast".equals(mode)) {
            return new PayFastPaymentProvider();
        }
        return new DemoPaymentProvider();
    }
}
