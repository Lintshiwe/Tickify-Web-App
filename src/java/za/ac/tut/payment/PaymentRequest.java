package za.ac.tut.payment;

public class PaymentRequest {

    private final String holder;
    private final String cardNumber;
    private final String expiry;
    private final String cvv;
    private final String country;
    private final double amount;

    public PaymentRequest(String holder, String cardNumber, String expiry, String cvv, String country, double amount) {
        this.holder = holder;
        this.cardNumber = cardNumber;
        this.expiry = expiry;
        this.cvv = cvv;
        this.country = country;
        this.amount = amount;
    }

    public String getHolder() {
        return holder;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getExpiry() {
        return expiry;
    }

    public String getCvv() {
        return cvv;
    }

    public String getCountry() {
        return country;
    }

    public double getAmount() {
        return amount;
    }
}
