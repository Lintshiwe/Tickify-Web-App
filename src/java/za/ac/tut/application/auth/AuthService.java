package za.ac.tut.application.auth;

import java.sql.SQLException;

public interface AuthService {

    AuthenticationResult authenticate(String loginIdentifier, String password, String chosenRole) throws SQLException;

    String handleFailedAuthenticationThreshold(String chosenRole, String loginIdentifier, String sourceIp) throws SQLException;

    String getRedirectPathForRole(String role);
}
