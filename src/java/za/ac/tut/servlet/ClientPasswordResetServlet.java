package za.ac.tut.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import za.ac.tut.application.user.UserAccountService;
import za.ac.tut.notification.EmailService;
import za.ac.tut.security.PasswordResetTokenUtil;
import za.ac.tut.security.PasswordUtil;

public class ClientPasswordResetServlet extends HttpServlet {

    private static final long RESET_TOKEN_TTL_MS = 20 * 60 * 1000L;
    private final UserAccountService userAccountService = new UserAccountService();
    private final EmailService emailService = new EmailService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = trimToNull(request.getParameter("token"));
        if (token != null) {
            applyTokenViewState(request, token);
        }
        request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = trimToNull(request.getParameter("action"));
        if ("requestToken".equals(action)) {
            handleRequestToken(request, response);
            return;
        }
        if ("resetPassword".equals(action)) {
            handlePasswordReset(request, response);
            return;
        }
        request.setAttribute("error", "Unsupported reset action.");
        request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
    }

    private void handleRequestToken(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String rawRole = trimToNull(request.getParameter("userRole"));
        String role = normalizeClientRole(rawRole);
        String identifier = trimToNull(request.getParameter("identifier"));
        String resetLink = null;

        if (!userAccountService.isClientRole(role) || identifier == null) {
            request.setAttribute("error", "Choose a client role and enter your username or email.");
            request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
            return;
        }

        try {
            UserAccountService.ClientAccount account = userAccountService.findClientByIdentifier(role, identifier);
            if (account == null) {
                request.setAttribute("status", "If this account exists, a reset link can be generated.");
                request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
                return;
            }

            long expiresAt = System.currentTimeMillis() + RESET_TOKEN_TTL_MS;
            String token = PasswordResetTokenUtil.generate(account.getRole(), account.getUserId(), expiresAt, account.getPasswordHash());
            String encoded = URLEncoder.encode(token, StandardCharsets.UTF_8.name());
            String appBase = resolveAppBaseUrl(request);
            resetLink = appBase + request.getContextPath() + "/ClientPasswordReset.do?token=" + encoded;

            emailService.sendPasswordResetEmail(account.getEmail(), resetLink);
            request.setAttribute("status", "A password reset email has been sent to your account if it exists.");
            request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
        } catch (SQLException ex) {
            log("Failed to generate password reset token", ex);
            request.setAttribute("error", "Unable to generate reset token right now.");
            request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
        } catch (Exception ex) {
            log("Failed to send password reset email", ex);
            request.setAttribute("error", resolveEmailSendError(ex));
            applyResetLinkFallback(request, resetLink);
            request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
        } catch (Throwable ex) {
            // Guard against runtime linkage/classloading issues in the mail stack.
            log("Failed to send password reset email", ex);
            request.setAttribute("error", resolveEmailSendError(ex));
            applyResetLinkFallback(request, resetLink);
            request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
        }
    }

    private String resolveEmailSendError(Throwable ex) {
        String message = ex == null ? null : ex.getMessage();
        if (message != null) {
            String normalized = message.toLowerCase();
            if (normalized.contains("smtpauthenticationerror")
                    || normalized.contains("5.7.8")
                    || normalized.contains("username and password not accepted")) {
                return "SMTP authentication failed. Configure a Gmail App Password and try again.";
            }
        }
        return "Unable to send reset email right now. Please try again later.";
    }

    private void applyResetLinkFallback(HttpServletRequest request, String resetLink) {
        if (resetLink == null) {
            return;
        }
        if (isLocalRequest(request) || isFallbackExplicitlyEnabled()) {
            request.setAttribute("fallbackResetLink", resetLink);
            request.setAttribute("fallbackHint", "Temporary local fallback: use this reset link directly.");
        }
    }

    private boolean isLocalRequest(HttpServletRequest request) {
        String host = request.getServerName();
        if (host == null) {
            return false;
        }
        String normalized = host.trim().toLowerCase();
        return "localhost".equals(normalized) || "127.0.0.1".equals(normalized);
    }

    private boolean isFallbackExplicitlyEnabled() {
        String env = System.getenv("TICKIFY_RESET_FALLBACK_LINK");
        if (env != null && !env.trim().isEmpty()) {
            return Boolean.parseBoolean(env.trim());
        }
        String prop = System.getProperty("tickify.reset.fallbackLink");
        if (prop != null && !prop.trim().isEmpty()) {
            return Boolean.parseBoolean(prop.trim());
        }
        return false;
    }

    private void handlePasswordReset(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = trimToNull(request.getParameter("token"));
        String password = trimToNull(request.getParameter("newPassword"));
        String confirm = trimToNull(request.getParameter("confirmPassword"));

        if (token == null || password == null || confirm == null) {
            request.setAttribute("error", "Token and both password fields are required.");
            if (token != null) {
                applyTokenViewState(request, token);
            }
            request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
            return;
        }

        if (!password.equals(confirm)) {
            request.setAttribute("error", "Password confirmation does not match.");
            applyTokenViewState(request, token);
            request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
            return;
        }

        if (password.length() < 8) {
            request.setAttribute("error", "Password must be at least 8 characters.");
            applyTokenViewState(request, token);
            request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
            return;
        }

        try {
            PasswordResetTokenUtil.TokenPayload payload = resolvePayload(token);
            if (payload == null || !userAccountService.isClientRole(payload.getRole())) {
                request.setAttribute("error", "Reset token is invalid or expired.");
                request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
                return;
            }

            String newHash = PasswordUtil.hashPassword(password);
            boolean updated = userAccountService.updateClientPassword(payload.getRole(), payload.getUserId(), newHash);
            if (!updated) {
                request.setAttribute("error", "Reset token is invalid or expired.");
                request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
                return;
            }

            response.sendRedirect(request.getContextPath() + "/Login.jsp?msg=ResetSuccess");
        } catch (SQLException ex) {
            log("Failed to reset client password", ex);
            request.setAttribute("error", "Unable to reset password right now.");
            applyTokenViewState(request, token);
            request.getRequestDispatcher("/ClientPasswordReset.jsp").forward(request, response);
        }
    }

    private void applyTokenViewState(HttpServletRequest request, String token) {
        request.setAttribute("token", token);
        PasswordResetTokenUtil.TokenPayload payload = resolvePayload(token);
        if (payload == null || !userAccountService.isClientRole(payload.getRole())) {
            request.setAttribute("tokenValid", false);
            return;
        }
        request.setAttribute("tokenValid", true);
        request.setAttribute("tokenRole", payload.getRole());
        request.setAttribute("tokenUserId", payload.getUserId());
        request.setAttribute("tokenExpiresAt", payload.getExpiresAtEpochMs());
    }

    private PasswordResetTokenUtil.TokenPayload resolvePayload(String token) {
        try {
            String normalizedToken = trimToNull(token);
            if (normalizedToken == null) {
                return null;
            }
            PasswordResetTokenUtil.TokenPayload tentative = decodeUnsignedPayload(normalizedToken);
            if (tentative == null) {
                return null;
            }
            UserAccountService.ClientAccount account = userAccountService.findClientByRoleAndId(tentative.getRole(), tentative.getUserId());
            if (account == null) {
                return null;
            }
            return PasswordResetTokenUtil.verify(normalizedToken, account.getPasswordHash());
        } catch (SQLException ex) {
            return null;
        }
    }

    private PasswordResetTokenUtil.TokenPayload decodeUnsignedPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                return null;
            }
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] values = payload.split("\\|");
            if (values.length != 3) {
                return null;
            }
            String role = values[0];
            int userId = Integer.parseInt(values[1]);
            long exp = Long.parseLong(values[2]);
            return new PasswordResetTokenUtil.TokenPayload(role, userId, exp);
        } catch (RuntimeException ex) {
            return null;
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
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveAppBaseUrl(HttpServletRequest request) {
        String env = System.getenv("TICKIFY_APP_BASE_URL");
        if (env != null && !env.trim().isEmpty()) {
            return stripTrailingSlash(env.trim());
        }
        String prop = System.getProperty("tickify.app.baseUrl");
        if (prop != null && !prop.trim().isEmpty()) {
            return stripTrailingSlash(prop.trim());
        }
        String inferred = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort());
        return stripTrailingSlash(inferred);
    }

    private String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
