package za.ac.tut.payment;

public class PaymentResult {

    private final boolean successful;
    private final String message;
    private final String transactionRef;

    public PaymentResult(boolean successful, String message, String transactionRef) {
        this.successful = successful;
        this.message = message;
        this.transactionRef = transactionRef;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public static PaymentResult success(String message, String transactionRef) {
        return new PaymentResult(true, message, transactionRef);
    }

    public static PaymentResult failure(String message) {
        return new PaymentResult(false, message, null);
    }
}
