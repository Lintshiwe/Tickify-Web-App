package za.ac.tut.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import za.ac.tut.application.auth.AuthService;
import za.ac.tut.application.auth.AuthServiceImpl;
import za.ac.tut.application.auth.AuthenticationResult;
import za.ac.tut.notification.EmailService;

public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthServiceImpl();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L;
    private static final long LOCK_MS = 15 * 60 * 1000L;
    private static final long TWO_FACTOR_TTL_MS = 10 * 60 * 1000L;
    private static final ConcurrentMap<String, AttemptRecord> ACCOUNT_ATTEMPTS = new ConcurrentHashMap<>();
    private final EmailService emailService = new EmailService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Capture parameters from the form
        String loginId = req.getParameter("loginId");
        if (loginId == null || loginId.trim().isEmpty()) {
            loginId = req.getParameter("email");
        }
        String password = req.getParameter("password");
        String chosenRole = req.getParameter("userRole"); // Captured from the Radio Buttons
        chosenRole = normalizeRole(chosenRole);
        String normalizedLoginId = loginId != null ? loginId.trim().toLowerCase() : null;

        // 2. Input Validation
        if (loginId == null || loginId.trim().isEmpty() || 
            password == null || password.trim().isEmpty() ||
            chosenRole == null) {
            handleError(req, resp, "All fields and a role selection are required.");
            return;
        }

        String accountKey = chosenRole + "|" + normalizedLoginId;
        if (isLocked(ACCOUNT_ATTEMPTS, accountKey)) {
            handleError(req, resp, "Too many failed login attempts. Please wait 15 minutes and try again.");
            return;
        }

        try {
            AuthenticationResult authResult = authService.authenticate(loginId, password, chosenRole);
            if (!authResult.isSuccess()) {
                boolean accountThresholdTriggered = registerFailure(ACCOUNT_ATTEMPTS, accountKey);
                if (accountThresholdTriggered) {
                    String lockMessage = authService.handleFailedAuthenticationThreshold(
                            chosenRole,
                            loginId,
                            extractClientIp(req));
                    if (lockMessage != null) {
                        handleError(req, resp, lockMessage);
                        return;
                    }
                }
                handleError(req, resp, authResult.getErrorMessage());
                return;
            }

            clearFailures(ACCOUNT_ATTEMPTS, accountKey);

            if (requiresTwoFactor(authResult.getRole())) {
                if (authResult.getEmail() == null || authResult.getEmail().trim().isEmpty()) {
                    handleError(req, resp, "This role requires an email address for 2FA.");
                    return;
                }

                String code = generateTwoFactorCode();
                try {
                    emailService.sendLoginTwoFactorCodeEmail(authResult.getEmail(), code);
                } catch (Exception ex) {
                    log("2FA email delivery failed", ex);
                    handleError(req, resp, "Unable to send 2FA code. Please try again.");
                    return;
                }

                HttpSession pendingSession = req.getSession(true);
                pendingSession.setAttribute("pending2faRole", authResult.getRole());
                pendingSession.setAttribute("pending2faUserId", authResult.getUserId());
                pendingSession.setAttribute("pending2faDisplayName", authResult.getDisplayName());
                pendingSession.setAttribute("pending2faFullName", authResult.getFullName());
                pendingSession.setAttribute("pending2faEmail", authResult.getEmail());
                pendingSession.setAttribute("pending2faCampusName", authResult.getCampusName());
                pendingSession.setAttribute("pending2faRoleNumberLabel", authResult.getRoleNumberLabel());
                pendingSession.setAttribute("pending2faLoginId", loginId.trim());
                pendingSession.setAttribute("pending2faCode", code);
                pendingSession.setAttribute("pending2faExpiresAt", System.currentTimeMillis() + TWO_FACTOR_TTL_MS);
                pendingSession.setAttribute("pending2faAttempts", 0);
                resp.sendRedirect(req.getContextPath() + "/TwoFactor.jsp?msg=CodeSent");
                return;
            }

            // 3. Secure Session Management using application-layer authentication output
            HttpSession session = req.getSession(true);
            applyAuthenticatedSession(session, authResult, loginId.trim());
            
            // Security best practice: rotate session ID on login
            req.changeSessionId(); 

            // 4. Redirect to the appropriate dashboard
            resp.sendRedirect(req.getContextPath() + authService.getRedirectPathForRole(authResult.getRole()));

        } catch (SQLException e) {
            log("Database error: " + e.getMessage());
            handleError(req, resp, "System connection issue. Please try again later.");
        } catch (Exception e) {
            log("General error: " + e.getMessage());
            handleError(req, resp, "An unexpected error occurred.");
        }
    }

    private void handleError(HttpServletRequest req, HttpServletResponse resp, String message) 
            throws ServletException, IOException {
        req.setAttribute("error", message);
        req.getRequestDispatcher("Login.jsp").forward(req, resp);
    }

    private String extractClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private boolean isLocked(ConcurrentMap<String, AttemptRecord> tracker, String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }

        AttemptRecord rec = tracker.get(key);
        if (rec == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (rec.lockUntil > now) {
            return true;
        }

        if (rec.lockUntil > 0 && rec.lockUntil <= now) {
            tracker.remove(key, rec);
        }
        return false;
    }

    private boolean registerFailure(ConcurrentMap<String, AttemptRecord> tracker, String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();
        final boolean[] thresholdTriggered = new boolean[]{false};
        tracker.compute(key, (k, rec) -> {
            AttemptRecord current = rec;
            if (current == null || now - current.windowStart > WINDOW_MS) {
                current = new AttemptRecord();
                current.windowStart = now;
                current.failedCount = 1;
            } else {
                current.failedCount++;
            }

            if (current.failedCount >= MAX_FAILED_ATTEMPTS) {
                current.lockUntil = now + LOCK_MS;
                current.failedCount = 0;
                current.windowStart = now;
                thresholdTriggered[0] = true;
            }

            return current;
        });
        return thresholdTriggered[0];
    }

    private void clearFailures(ConcurrentMap<String, AttemptRecord> tracker, String key) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        tracker.remove(key);
    }

    private void applyAuthenticatedSession(HttpSession session, AuthenticationResult authResult, String loginId) {
        session.setAttribute("userEmail", authResult.getEmail());
        session.setAttribute("userRole", authResult.getRole());
        session.setAttribute("userID", authResult.getUserId());
        session.setAttribute("userFullName", authResult.getDisplayName());
        session.setAttribute("userLegalName", authResult.getFullName());
        session.setAttribute("userLoginId", loginId);
        session.setAttribute("userCampusName", authResult.getCampusName());
        session.setAttribute("userRoleNumberLabel", authResult.getRoleNumberLabel());
    }

    private boolean requiresTwoFactor(String role) {
        return "ADMIN".equals(role) || "EVENT_MANAGER".equals(role) || "TERTIARY_PRESENTER".equals(role);
    }

    private String generateTwoFactorCode() {
        int code = 100000 + new Random().nextInt(900000);
        return String.valueOf(code);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        String normalized = role.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        if ("PRESENTER".equals(normalized)) {
            return "TERTIARY_PRESENTER";
        }
        if ("MANAGER".equals(normalized)) {
            return "EVENT_MANAGER";
        }
        if ("GUARD".equals(normalized)) {
            return "VENUE_GUARD";
        }
        return normalized;
    }

    private static class AttemptRecord {
        long windowStart;
        int failedCount;
        long lockUntil;
    }
}