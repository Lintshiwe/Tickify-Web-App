package za.ac.tut.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import za.ac.tut.application.user.UserAccountService.ClientAccount;
import za.ac.tut.notification.EmailService;
import za.ac.tut.security.EmailVerificationTokenUtil;
import za.ac.tut.application.registration.RegistrationRequest;
import za.ac.tut.application.registration.RegistrationResult;
import za.ac.tut.application.registration.RegistrationService;
import za.ac.tut.application.registration.RegistrationServiceImpl;
import za.ac.tut.application.user.UserAccountService;

public class RegistrationServlet extends HttpServlet {

    private static final int MAX_SIGNUP_ATTEMPTS = -1;
    private static final long WINDOW_MS = 15 * 60 * 1000L;
    private static final long LOCK_MS = 15 * 60 * 1000L;
    private static final long VERIFY_TOKEN_TTL_MS = 24 * 60 * 60 * 1000L;
    private static final ConcurrentMap<String, AttemptRecord> SIGNUP_ATTEMPTS = new ConcurrentHashMap<>();
    private final RegistrationService registrationService = new RegistrationServiceImpl();
    private final UserAccountService userAccountService = new UserAccountService();
    private final EmailService emailService = new EmailService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String ipKey = extractClientIp(request);
        if (isLocked(ipKey)) {
            response.sendRedirect(request.getContextPath() + "/UserSignUp.jsp?err=RateLimit");
            return;
        }

        String website = request.getParameter("website");
        if (website != null && !website.trim().isEmpty()) {
            registerAttempt(ipKey);
            response.sendRedirect(request.getContextPath() + "/UserSignUp.jsp?err=DBFail");
            return;
        }
        
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setRole(request.getParameter("userRole"));
        registrationRequest.setFirstName(request.getParameter("firstname"));
        registrationRequest.setLastName(request.getParameter("lastname"));
        registrationRequest.setUsername(request.getParameter("username"));
        registrationRequest.setEmail(request.getParameter("email"));
        registrationRequest.setRawPassword(request.getParameter("password"));
        registrationRequest.setPhoneNumber(request.getParameter("phoneNumber"));
        registrationRequest.setBiography(request.getParameter("biography"));
        registrationRequest.setClientType(request.getParameter("clientType"));
        registrationRequest.setAttendeeInstitution(request.getParameter("attendeeInstitution"));
        registrationRequest.setPresenterInstitution(request.getParameter("presenterInstitution"));
        registrationRequest.setStudentNumber(request.getParameter("studentNumber"));
        registrationRequest.setIdPassportNumber(request.getParameter("idPassportNumber"));
        registrationRequest.setDateOfBirth(request.getParameter("dateOfBirth"));

        try {
            RegistrationResult result = registrationService.register(registrationRequest);

            // REDIRECTION LOGIC - Fixes 404 by using correct filenames
            if (result.isSuccess()) {
                clearAttempts(ipKey);
                boolean verificationSent = trySendVerificationEmail(request, registrationRequest);
                if (verificationSent) {
                    response.sendRedirect(request.getContextPath() + "/Login.jsp?msg=VerifyEmailSent");
                } else {
                    response.sendRedirect(request.getContextPath() + "/Login.jsp?msg=VerifyEmailPending");
                }
            } else {
                registerAttempt(ipKey);
                // Business Failure: Stay on UserSignUp.jsp
                response.sendRedirect(request.getContextPath() + "/UserSignUp.jsp?err=" + result.getCode());
            }

        } catch (SQLException e) {
            registerAttempt(ipKey);
            if (userAccountService.isUniqueConstraintViolation(e)) {
                response.sendRedirect(request.getContextPath() + "/UserSignUp.jsp?err=Duplicate");
                return;
            }
            // Database Crash: Stay on UserSignUp.jsp
            response.sendRedirect(request.getContextPath() + "/UserSignUp.jsp?err=SQLCrash");
        }
    }

    private boolean trySendVerificationEmail(HttpServletRequest request, RegistrationRequest registrationRequest) {
        String normalizedRole = normalizeClientRole(registrationRequest.getRole());
        String email = trimToNull(registrationRequest.getEmail());
        if (normalizedRole == null || email == null) {
            return false;
        }

        try {
            ClientAccount account = userAccountService.findClientByIdentifier(normalizedRole, email);
            if (account == null) {
                return false;
            }

            long expiresAt = System.currentTimeMillis() + VERIFY_TOKEN_TTL_MS;
            String token = EmailVerificationTokenUtil.generate(account.getRole(), account.getUserId(), account.getEmail(), expiresAt);
            String encoded = URLEncoder.encode(token, StandardCharsets.UTF_8.name());
            String verifyLink = resolveAppBaseUrl(request) + request.getContextPath() + "/VerifyEmail.do?token=" + encoded;
            emailService.sendEmailVerificationEmail(account.getEmail(), verifyLink);
            return true;
        } catch (Exception ex) {
            log("Failed to send verification email", ex);
            return false;
        }
    }

    private String normalizeClientRole(String role) {
        if (role == null) {
            return null;
        }
        String normalized = role.trim().toUpperCase();
        if ("PRESENTER".equals(normalized)) {
            return "TERTIARY_PRESENTER";
        }
        if ("ATTENDEE".equals(normalized) || "TERTIARY_PRESENTER".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveAppBaseUrl(HttpServletRequest request) {
        String configured = System.getenv("TICKIFY_APP_BASE_URL");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getProperty("tickify.app.baseUrl");
        }
        if (configured != null && !configured.trim().isEmpty()) {
            return configured.trim().replaceAll("/$", "");
        }

        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return scheme + "://" + host + (defaultPort ? "" : ":" + port);
    }

    private String extractClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private boolean isLocked(String ipKey) {
        if (MAX_SIGNUP_ATTEMPTS <= 0) {
            return false;
        }

        AttemptRecord rec = SIGNUP_ATTEMPTS.get(ipKey);
        if (rec == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (rec.lockUntil > now) {
            return true;
        }

        if (rec.lockUntil > 0 && rec.lockUntil <= now) {
            SIGNUP_ATTEMPTS.remove(ipKey, rec);
        }
        return false;
    }

    private void registerAttempt(String ipKey) {
        if (MAX_SIGNUP_ATTEMPTS <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        SIGNUP_ATTEMPTS.compute(ipKey, (k, rec) -> {
            AttemptRecord current = rec;
            if (current == null || now - current.windowStart > WINDOW_MS) {
                current = new AttemptRecord();
                current.windowStart = now;
                current.count = 1;
            } else {
                current.count++;
            }

            if (current.count >= MAX_SIGNUP_ATTEMPTS) {
                current.lockUntil = now + LOCK_MS;
                current.count = 0;
                current.windowStart = now;
            }

            return current;
        });
    }

    private void clearAttempts(String ipKey) {
        SIGNUP_ATTEMPTS.remove(ipKey);
    }

    private static class AttemptRecord {
        long windowStart;
        int count;
        long lockUntil;
    }
}