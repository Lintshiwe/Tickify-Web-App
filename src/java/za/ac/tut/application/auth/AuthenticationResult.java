package za.ac.tut.application.auth;

public class AuthenticationResult {

    private final boolean success;
    private final String role;
    private final int userId;
    private final String displayName;
    private final String fullName;
    private final String email;
    private final String campusName;
    private final String roleNumberLabel;
    private final String errorMessage;

    private AuthenticationResult(boolean success, String role, int userId,
            String displayName, String fullName, String email,
            String campusName, String roleNumberLabel, String errorMessage) {
        this.success = success;
        this.role = role;
        this.userId = userId;
        this.displayName = displayName;
        this.fullName = fullName;
        this.email = email;
        this.campusName = campusName;
        this.roleNumberLabel = roleNumberLabel;
        this.errorMessage = errorMessage;
    }

    public static AuthenticationResult success(String role, int userId,
            String displayName, String fullName, String email,
            String campusName, String roleNumberLabel) {
        return new AuthenticationResult(true, role, userId, displayName, fullName,
                email, campusName, roleNumberLabel, null);
    }

    public static AuthenticationResult failure(String errorMessage) {
        return new AuthenticationResult(false, null, -1, null, null,
                null, null, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getRole() {
        return role;
    }

    public int getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getCampusName() {
        return campusName;
    }

    public String getRoleNumberLabel() {
        return roleNumberLabel;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
