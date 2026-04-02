package za.ac.tut.application.registration;

import java.sql.Date;
import java.time.LocalDate;
import za.ac.tut.databaseManagement.AttendeeDAO;
import za.ac.tut.databaseManagement.TertiaryPresenterDAO;
import za.ac.tut.databaseManagement.UserDAO;
import za.ac.tut.entities.Attendee;
import za.ac.tut.entities.QRCode;
import za.ac.tut.entities.TertiaryPresenter;
import za.ac.tut.security.PasswordUtil;

public class RegistrationServiceImpl implements RegistrationService {

    private final UserDAO userDAO;
    private final AttendeeDAO attendeeDAO;
    private final TertiaryPresenterDAO presenterDAO;

    public RegistrationServiceImpl() {
        this.userDAO = new UserDAO();
        this.attendeeDAO = new AttendeeDAO();
        this.presenterDAO = new TertiaryPresenterDAO();
    }

    @Override
    public RegistrationResult register(RegistrationRequest request) throws java.sql.SQLException {
        String normalizedRole = normalizeRole(request.getRole());
        String normalizedUsername = trimToNull(request.getUsername());
        String normalizedEmail = trimToNull(request.getEmail());

        if (isBlank(normalizedRole)
                || isBlank(request.getFirstName())
                || isBlank(request.getLastName())
                || isBlank(normalizedUsername)
                || isBlank(normalizedEmail)
                || isBlank(request.getRawPassword())
                || request.getRawPassword().length() < 8
                || !isValidUsername(normalizedUsername)
                || !isEmailRequiredValid(normalizedEmail)) {
            return RegistrationResult.failure(RegistrationResult.VALIDATION);
        }

        if ("ATTENDEE".equals(normalizedRole)) {
            Date dob = parseDate(request.getDateOfBirth());
            if (trimToNull(request.getClientType()) == null
                    || trimToNull(request.getIdPassportNumber()) == null
                    || dob == null) {
                return RegistrationResult.failure(RegistrationResult.VALIDATION);
            }
        }

        if (userDAO.identifierExistsInRole(normalizedUsername, normalizedRole)
                || (normalizedEmail != null && userDAO.identifierExistsInRole(normalizedEmail, normalizedRole))) {
            return RegistrationResult.failure(RegistrationResult.DUPLICATE);
        }

        String hashedPassword = PasswordUtil.hashPassword(request.getRawPassword());

        boolean success = false;
        if ("ATTENDEE".equals(normalizedRole)) {
            Attendee attendee = new Attendee();
            attendee.setFirstname(request.getFirstName());
            attendee.setLastname(request.getLastName());
            attendee.setUsername(normalizedUsername);
            attendee.setEmail(normalizedEmail);
            attendee.setPassword(hashedPassword);
            attendee.setClientType(trimToNull(request.getClientType()));
            attendee.setTertiaryInstitution(trimToNull(request.getAttendeeInstitution()));
            attendee.setPhoneNumber(request.getPhoneNumber());
            attendee.setStudentNumber(trimToNull(request.getStudentNumber()));
            attendee.setIdPassportNumber(trimToNull(request.getIdPassportNumber()));
            attendee.setDateOfBirth(parseDate(request.getDateOfBirth()));
            attendee.setBiography(request.getBiography());

            QRCode qr = new QRCode();
            qr.setQrCodeID(1);
            attendee.setQrCode(qr);

            success = attendeeDAO.insertAttendee(attendee);
        } else if ("TERTIARY_PRESENTER".equals(normalizedRole)) {
            TertiaryPresenter presenter = new TertiaryPresenter();
            presenter.setFirstname(request.getFirstName());
            presenter.setLastname(request.getLastName());
            presenter.setUsername(normalizedUsername);
            presenter.setEmail(normalizedEmail);
            presenter.setPassword(hashedPassword);
            presenter.setTertiaryInstitution(trimToNull(request.getPresenterInstitution()));
            presenter.setPhoneNumber(request.getPhoneNumber());
            presenter.setBiography(request.getBiography());

            success = presenterDAO.insertPresenter(presenter);
        }

        if (success) {
            return RegistrationResult.success();
        }

        return RegistrationResult.failure(RegistrationResult.DB_FAIL);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        if ("PRESENTER".equals(normalized)) {
            return "TERTIARY_PRESENTER";
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isValidUsername(String username) {
        return username != null && username.matches("^[A-Za-z0-9_.-]{4,30}$");
    }

    private boolean isEmailRequiredValid(String email) {
        if (email == null) {
            return false;
        }
        return email.contains("@") && email.length() <= 60;
    }

    private Date parseDate(String rawDate) {
        String value = trimToNull(rawDate);
        if (value == null) {
            return null;
        }
        try {
            return Date.valueOf(LocalDate.parse(value));
        } catch (Exception e) {
            return null;
        }
    }
}
