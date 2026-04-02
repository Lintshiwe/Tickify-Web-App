package za.ac.tut.application.registration;

public class RegistrationResult {

    public static final String OK = "OK";
    public static final String VALIDATION = "Validation";
    public static final String DUPLICATE = "Duplicate";
    public static final String DB_FAIL = "DBFail";

    private final boolean success;
    private final String code;

    private RegistrationResult(boolean success, String code) {
        this.success = success;
        this.code = code;
    }

    public static RegistrationResult success() {
        return new RegistrationResult(true, OK);
    }

    public static RegistrationResult failure(String code) {
        return new RegistrationResult(false, code);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }
}
