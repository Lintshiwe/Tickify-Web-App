package za.ac.tut.payment;

public class PayFastPaymentProvider implements PaymentProvider {

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        return PaymentResult.failure("PayFast integration not configured yet.");
    }

    @Override
    public String modeName() {
        return "payfast";
    }
}
