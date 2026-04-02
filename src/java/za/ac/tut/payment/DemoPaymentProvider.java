package za.ac.tut.payment;

import java.util.UUID;

public class DemoPaymentProvider implements PaymentProvider {

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        if (isBlank(request.getHolder()) || isBlank(request.getCardNumber())
                || isBlank(request.getExpiry()) || isBlank(request.getCvv())) {
            return PaymentResult.failure("Missing required payment fields.");
        }

        String digitsOnly = request.getCardNumber().replaceAll("\\D", "");
        if (digitsOnly.length() < 12) {
            return PaymentResult.failure("Card number is invalid.");
        }

        if (request.getAmount() <= 0) {
            return PaymentResult.failure("Amount must be greater than zero.");
        }

        String txRef = "DEMO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return PaymentResult.success("Demo payment approved.", txRef);
    }

    @Override
    public String modeName() {
        return "demo";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
