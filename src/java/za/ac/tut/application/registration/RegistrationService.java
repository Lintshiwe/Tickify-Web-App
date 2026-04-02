package za.ac.tut.application.registration;

import java.sql.SQLException;

public interface RegistrationService {

    RegistrationResult register(RegistrationRequest request) throws SQLException;
}
