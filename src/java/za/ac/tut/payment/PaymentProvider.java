package za.ac.tut.payment;

public interface PaymentProvider {

    PaymentResult processPayment(PaymentRequest request);

    String modeName();
}
