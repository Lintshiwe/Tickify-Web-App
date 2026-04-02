package za.ac.tut.application.auth;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import za.ac.tut.databaseManagement.UserDAO;

public class AuthServiceImpl implements AuthService {

    private static final String PRIVILEGED_ADMIN_EMAIL = "admin@tickify.ac.za";
    private final UserDAO userDAO;
    private final Map<String, RoleConfig> roleMap;

    public AuthServiceImpl() {
        this.userDAO = new UserDAO();
        this.roleMap = buildRoleMap();
    }

    @Override
    public AuthenticationResult authenticate(String loginIdentifier, String password, String chosenRole) throws SQLException {
        if (isBlank(loginIdentifier) || isBlank(password)) {
            return AuthenticationResult.failure("All fields and a role selection are required.");
        }

        String normalizedRole = normalizeRole(chosenRole);
        if (normalizedRole == null || !roleMap.containsKey(normalizedRole)) {
            return AuthenticationResult.failure("All fields and a role selection are required.");
        }

        String resolvedRole = userDAO.authenticateSpecific(loginIdentifier, password, normalizedRole);
        if (resolvedRole == null) {
            return AuthenticationResult.failure("Invalid username/email, password, or role selection.");
        }

        RoleConfig config = roleMap.get(resolvedRole);
        int userId = userDAO.getUserIDByIdentifier(loginIdentifier, config.table, config.idCol, supportsUsername(resolvedRole));
        if (userId <= 0) {
            return AuthenticationResult.failure("Unable to resolve user account.");
        }

        String displayName = userDAO.getDisplayNameByIdentifier(loginIdentifier, config.table, supportsUsername(resolvedRole));
        if (userDAO.isAccountLocked(resolvedRole, userId)) {
            if (displayName != null && !displayName.trim().isEmpty()) {
                return AuthenticationResult.failure("Profile for " + displayName.trim() + " is locked. Please contact an admin to unblock your account.");
            }
            return AuthenticationResult.failure("This profile is locked. Please contact an admin to unblock your account.");
        }

        if (userDAO.isClientRole(resolvedRole) && !userDAO.isClientEmailVerified(resolvedRole, userId)) {
            return AuthenticationResult.failure("Please verify your email before signing in. Check your inbox for the verification link.");
        }

        String resolvedEmail = userDAO.getEmailByIdentifier(loginIdentifier, config.table, supportsUsername(resolvedRole));
        String fullName = userDAO.getFullNameByIdentifier(loginIdentifier, config.table, supportsUsername(resolvedRole));
        String campusName = userDAO.getCampusNameForRole(resolvedRole, userId);
        if ("ADMIN".equals(resolvedRole) && resolvedEmail != null
                && PRIVILEGED_ADMIN_EMAIL.equalsIgnoreCase(resolvedEmail.trim())) {
            campusName = "Tickify Admin";
        }

        String roleNumberLabel = buildRoleNumberLabel(resolvedRole, userId);
        String normalizedCampus = (campusName != null && !campusName.trim().isEmpty())
                ? campusName.trim() : "Campus Unassigned";

        return AuthenticationResult.success(
                resolvedRole,
                userId,
                displayName,
                fullName,
                resolvedEmail,
                normalizedCampus,
                roleNumberLabel);
    }

    @Override
    public String handleFailedAuthenticationThreshold(String chosenRole, String loginIdentifier, String sourceIp) throws SQLException {
        String normalizedRole = normalizeRole(chosenRole);
        if (normalizedRole == null || "ADMIN".equals(normalizedRole) || isBlank(loginIdentifier)) {
            return null;
        }

        int knownUserId = userDAO.resolveUserIdForRoleIdentifier(normalizedRole, loginIdentifier);
        if (knownUserId <= 0) {
            return null;
        }

        userDAO.lockAccountForFailedAuth(normalizedRole, knownUserId, loginIdentifier, sourceIp);
        RoleConfig failedRoleConfig = roleMap.get(normalizedRole);
        String subjectName = "";
        if (failedRoleConfig != null) {
            subjectName = userDAO.getDisplayNameByIdentifier(loginIdentifier,
                    failedRoleConfig.table,
                    supportsUsername(normalizedRole));
        }

        if (subjectName != null && !subjectName.trim().isEmpty()) {
            return "Profile for " + subjectName.trim() + " has been locked after repeated failed authorization attempts. Admin has been alerted and must unblock the account.";
        }

        return "This profile has been locked after repeated failed authorization attempts. Admin has been alerted and must unblock the account.";
    }

    private Map<String, RoleConfig> buildRoleMap() {
        Map<String, RoleConfig> map = new HashMap<String, RoleConfig>();
        map.put("ADMIN", new RoleConfig("admin", "adminID", "/AdminDashboard.do"));
        map.put("EVENT_MANAGER", new RoleConfig("event_manager", "eventManagerID", "/EventManagerDashboard.do"));
        map.put("VENUE_GUARD", new RoleConfig("venue_guard", "venueGuardID", "/VenueGuard/VenueGuardDashboard.jsp"));
        map.put("TERTIARY_PRESENTER", new RoleConfig("tertiary_presenter", "tertiaryPresenterID", "/TertiaryPresenterDashboard.do"));
        map.put("ATTENDEE", new RoleConfig("attendee", "attendeeID", "/AttendeeDashboardServlet.do"));
        return map;
    }

    public String getRedirectPathForRole(String role) {
        RoleConfig config = roleMap.get(role);
        return config == null ? "/Login.jsp" : config.redirectPath;
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

    private boolean supportsUsername(String role) {
        return "ATTENDEE".equals(role) || "TERTIARY_PRESENTER".equals(role);
    }

    private String buildRoleNumberLabel(String role, int uid) {
        if (uid <= 0) {
            return "Profile #0";
        }
        if ("ADMIN".equals(role)) {
            return "Admin #" + uid;
        }
        if ("EVENT_MANAGER".equals(role)) {
            return "Manager #" + uid;
        }
        if ("TERTIARY_PRESENTER".equals(role)) {
            return "Presenter #" + uid;
        }
        if ("VENUE_GUARD".equals(role)) {
            return "Guard #" + uid;
        }
        return "Attendee #" + uid;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class RoleConfig {
        private final String table;
        private final String idCol;
        private final String redirectPath;

        private RoleConfig(String table, String idCol, String redirectPath) {
            this.table = table;
            this.idCol = idCol;
            this.redirectPath = redirectPath;
        }
    }
}
