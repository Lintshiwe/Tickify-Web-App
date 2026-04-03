package za.ac.tut.databaseManagement;

import java.sql.*;
import za.ac.tut.databaseConnection.DatabaseConnection;
import za.ac.tut.security.PasswordUtil;

public class UserDAO {

    private static final String[] PRIVILEGED_ADMIN_EMAILS = {
        "ntoampilp@gmail.com",
        "admin@tickify.ac.za"
    };

    public static class ClientAccount {
        private final String role;
        private final int userId;
        private final String email;
        private final String passwordHash;

        public ClientAccount(String role, int userId, String email, String passwordHash) {
            this.role = role;
            this.userId = userId;
            this.email = email;
            this.passwordHash = passwordHash;
        }

        public String getRole() {
            return role;
        }

        public int getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getPasswordHash() {
            return passwordHash;
        }
    }

    private boolean supportsUsernameByRole(String role) {
        String normalized = normalizeClientRole(role);
        if (normalized == null) {
            return false;
        }
        return "ATTENDEE".equals(normalized) || "TERTIARY_PRESENTER".equals(normalized);
    }

    private String tableForRole(String chosenRole) {
        String normalized = normalizeClientRole(chosenRole);
        if (normalized == null) {
            return null;
        }
        switch (normalized) {
            case "ADMIN":
                return "admin";
            case "EVENT_MANAGER":
                return "event_manager";
            case "VENUE_GUARD":
                return "venue_guard";
            case "TERTIARY_PRESENTER":
                return "tertiary_presenter";
            case "ATTENDEE":
                return "attendee";
            default:
                return null;
        }
    }

    private String normalizeClientRole(String role) {
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

    public boolean isClientRole(String role) {
        String normalized = normalizeClientRole(role);
        return "ATTENDEE".equals(normalized) || "TERTIARY_PRESENTER".equals(normalized);
    }

    public ClientAccount findClientByIdentifier(String role, String identifier) throws SQLException {
        String normalizedRole = normalizeClientRole(role);
        if (!isClientRole(normalizedRole) || identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        String table = tableForRole(normalizedRole);
        String idCol = idColumnForRole(normalizedRole);
        String sql = "SELECT " + idCol + " AS userId, email, password FROM " + table
                + " WHERE LOWER(email)=LOWER(?) OR LOWER(username)=LOWER(?) FETCH FIRST ROW ONLY";

        return executeSingleQuery(sql,
                rs -> new ClientAccount(normalizedRole, rs.getInt("userId"), rs.getString("email"), rs.getString("password")),
                identifier, identifier);
    }

    public ClientAccount findClientByRoleAndId(String role, int userId) throws SQLException {
        String normalizedRole = normalizeClientRole(role);
        if (!isClientRole(normalizedRole) || userId <= 0) {
            return null;
        }
        String table = tableForRole(normalizedRole);
        String idCol = idColumnForRole(normalizedRole);
        String sql = "SELECT " + idCol + " AS userId, email, password FROM " + table + " WHERE " + idCol + " = ?";

        return executeSingleQuery(sql,
                rs -> new ClientAccount(normalizedRole, rs.getInt("userId"), rs.getString("email"), rs.getString("password")),
                userId);
    }

    public boolean updateClientPassword(String role, int userId, String newPasswordHash) throws SQLException {
        String normalizedRole = normalizeClientRole(role);
        if (!isClientRole(normalizedRole) || userId <= 0 || newPasswordHash == null || newPasswordHash.trim().isEmpty()) {
            return false;
        }
        String table = tableForRole(normalizedRole);
        String idCol = idColumnForRole(normalizedRole);
        String sql = "UPDATE " + table + " SET password = ? WHERE " + idCol + " = ?";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean isClientEmailVerified(String role, int userId) throws SQLException {
        String normalizedRole = normalizeClientRole(role);
        if (!isClientRole(normalizedRole) || userId <= 0) {
            return true;
        }
        String table = tableForRole(normalizedRole);
        String idCol = idColumnForRole(normalizedRole);
        String sql = "SELECT emailVerified FROM " + table + " WHERE " + idCol + " = ?";
        Boolean verified = executeSingleQuery(sql, rs -> rs.getBoolean("emailVerified"), userId);
        return verified != null && verified;
    }

    public boolean verifyClientEmailAddress(String role, int userId, String expectedEmail) throws SQLException {
        String normalizedRole = normalizeClientRole(role);
        String email = expectedEmail == null ? null : expectedEmail.trim().toLowerCase();
        if (!isClientRole(normalizedRole) || userId <= 0 || email == null || email.isEmpty()) {
            return false;
        }
        String table = tableForRole(normalizedRole);
        String idCol = idColumnForRole(normalizedRole);
        String sql = "UPDATE " + table + " SET emailVerified = ?, emailVerifiedAt = CURRENT_TIMESTAMP "
                + "WHERE " + idCol + " = ? AND LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, true);
            ps.setInt(2, userId);
            ps.setString(3, email);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean isUniqueConstraintViolation(SQLException ex) {
        SQLException current = ex;
        while (current != null) {
            String state = current.getSQLState();
            if ("23505".equals(state)) {
                return true;
            }
            current = current.getNextException();
        }
        return false;
    }

    private String idColumnForRole(String chosenRole) {
        String normalized = normalizeClientRole(chosenRole);
        if (normalized == null) {
            return null;
        }
        switch (normalized) {
            case "ADMIN":
                return "adminID";
            case "EVENT_MANAGER":
                return "eventManagerID";
            case "VENUE_GUARD":
                return "venueGuardID";
            case "TERTIARY_PRESENTER":
                return "tertiaryPresenterID";
            case "ATTENDEE":
                return "attendeeID";
            default:
                return null;
        }
    }

    /**
     * CENTRALIZED QUERY EXECUTOR
     * Handles connection boilerplate and mapping.
     */
    private <T> T executeSingleQuery(String sql, ResultSetMapper<T> mapper, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapper.map(rs);
                }
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /**
     * NEW: Targeted Authentication
     * This checks ONLY the table corresponding to the role selected on the Login UI.
     * Prevents "Attendee Overlap" where one email exists in multiple tables.
     */
    public String authenticateSpecific(String identifier, String password, String chosenRole) throws SQLException {
        String table = tableForRole(chosenRole);
        if (table == null) {
            return null;
        }

        // Only check that specific table
        if (check(identifier, password, table, supportsUsernameByRole(chosenRole))) {
            return chosenRole; 
        }
        
        return null; 
    }

    public boolean isAccountLocked(String role, int userId) throws SQLException {
        String sql = "SELECT isLocked FROM account_control WHERE roleName = ? AND userID = ?";
        Boolean locked = executeSingleQuery(sql, rs -> rs.getBoolean("isLocked"), role, userId);
        return locked != null && locked;
    }

    public int resolveUserIdForRoleIdentifier(String chosenRole, String identifier) throws SQLException {
        String table = tableForRole(chosenRole);
        String idCol = idColumnForRole(chosenRole);
        if (table == null || idCol == null || identifier == null || identifier.trim().isEmpty()) {
            return -1;
        }
        return getUserIDByIdentifier(identifier, table, idCol, supportsUsernameByRole(chosenRole));
    }

    public boolean lockAccountForFailedAuth(String chosenRole, int userId, String identifier, String sourceIp) throws SQLException {
        if (chosenRole == null || userId <= 0 || "ADMIN".equals(chosenRole)) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean hasUpdatedByAdminId = columnExists(conn, "account_control", "updatedByAdminID");
            boolean hasUpdatedAt = columnExists(conn, "account_control", "updatedAt");
            boolean hasForceReset = columnExists(conn, "account_control", "forceReset");
            boolean exists;
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT 1 FROM account_control WHERE roleName = ? AND userID = ?")) {
                check.setString(1, chosenRole);
                check.setInt(2, userId);
                try (ResultSet rs = check.executeQuery()) {
                    exists = rs.next();
                }
            }

            int affected;
            if (exists) {
                StringBuilder updateSql = new StringBuilder("UPDATE account_control SET isLocked = ?");
                if (hasUpdatedAt) {
                    updateSql.append(", updatedAt = CURRENT_TIMESTAMP");
                }
                updateSql.append(" WHERE roleName = ? AND userID = ?");

                try (PreparedStatement update = conn.prepareStatement(updateSql.toString())) {
                    int idx = 1;
                    update.setBoolean(idx++, true);
                    update.setString(idx++, chosenRole);
                    update.setInt(idx, userId);
                    affected = update.executeUpdate();
                }
            } else {
                StringBuilder columns = new StringBuilder("roleName, userID, isLocked");
                StringBuilder values = new StringBuilder("?, ?, ?");
                if (hasForceReset) {
                    columns.append(", forceReset");
                    values.append(", ?");
                }
                if (hasUpdatedByAdminId) {
                    columns.append(", updatedByAdminID");
                    values.append(", ?");
                }
                if (hasUpdatedAt) {
                    columns.append(", updatedAt");
                    values.append(", CURRENT_TIMESTAMP");
                }

                String insertSql = "INSERT INTO account_control(" + columns + ") VALUES(" + values + ")";
                try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    int idx = 1;
                    insert.setString(idx++, chosenRole);
                    insert.setInt(idx++, userId);
                    insert.setBoolean(idx++, true);
                    if (hasForceReset) {
                        insert.setBoolean(idx++, false);
                    }
                    if (hasUpdatedByAdminId) {
                        insert.setNull(idx++, Types.INTEGER);
                    }
                    affected = insert.executeUpdate();
                }
            }

            int privilegedAdminId = findPrivilegedAdminId(conn);
            if (privilegedAdminId > 0) {
                try (PreparedStatement audit = conn.prepareStatement(
                        "INSERT INTO admin_audit_log(adminID, actionType, targetTable, targetID, details, createdAt) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)")) {
                    audit.setInt(1, privilegedAdminId);
                    audit.setString(2, "AUTH_LOCKOUT_ALERT");
                    audit.setString(3, "account_control");
                    audit.setString(4, chosenRole + ":" + userId);
                    audit.setString(5, "Auto-lock after repeated failed auth. Identifier=" + safe(identifier) + ", sourceIP=" + safe(sourceIp));
                    audit.executeUpdate();
                }
            }

            return affected > 0;
        }
    }

    private int findPrivilegedAdminId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT adminID FROM admin WHERE LOWER(email)=LOWER(?) FETCH FIRST ROW ONLY")) {
            for (String privilegedEmail : PRIVILEGED_ADMIN_EMAILS) {
                ps.setString(1, privilegedEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT adminID FROM admin ORDER BY adminID ASC FETCH FIRST ROW ONLY")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    private String safe(String value) {
        if (value == null) {
            return "-";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "-" : trimmed;
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        String upperTable = tableName == null ? "" : tableName.toUpperCase();
        String upperColumn = columnName == null ? "" : columnName.toUpperCase();
        try (ResultSet rs = meta.getColumns(null, null, upperTable, upperColumn)) {
            return rs.next();
        }
    }

    /**
     * Legacy authenticate (Keep for compatibility if needed, but use Specific for Login)
     */
    public String authenticate(String email, String password) throws SQLException {
        String[] tables = {"admin", "event_manager", "venue_guard", "tertiary_presenter", "attendee"};
        String[] roles = {"ADMIN", "EVENT_MANAGER", "VENUE_GUARD", "TERTIARY_PRESENTER", "ATTENDEE"};

        for (int i = 0; i < tables.length; i++) {
            if (check(email, password, tables[i], supportsUsernameByRole(roles[i]))) {
                return roles[i];
            }
        }
        return null;
    }

    public int getUserIDByIdentifier(String identifier, String table, String idCol, boolean allowUsername) throws SQLException {
        String sql = allowUsername
                ? "SELECT " + idCol + " FROM " + table + " WHERE LOWER(email)=LOWER(?) OR LOWER(username)=LOWER(?)"
                : "SELECT " + idCol + " FROM " + table + " WHERE LOWER(email)=LOWER(?)";
        Integer id = allowUsername
                ? executeSingleQuery(sql, rs -> rs.getInt(1), identifier, identifier)
                : executeSingleQuery(sql, rs -> rs.getInt(1), identifier);
        return (id != null) ? id : -1;
    }

    public String getFullNameByIdentifier(String identifier, String table, boolean allowUsername) throws SQLException {
        String sql = allowUsername
                ? "SELECT firstname, lastname FROM " + table + " WHERE LOWER(email)=LOWER(?) OR LOWER(username)=LOWER(?)"
                : "SELECT firstname, lastname FROM " + table + " WHERE LOWER(email)=LOWER(?)";
        String name = allowUsername
                ? executeSingleQuery(sql, rs -> rs.getString("firstname") + " " + rs.getString("lastname"), identifier, identifier)
                : executeSingleQuery(sql, rs -> rs.getString("firstname") + " " + rs.getString("lastname"), identifier);
        return (name != null) ? name : "Unknown User";
    }

    public String getDisplayNameByIdentifier(String identifier, String table, boolean allowUsername) throws SQLException {
        String sql = allowUsername
                ? "SELECT username, firstname, lastname FROM " + table + " WHERE LOWER(email)=LOWER(?) OR LOWER(username)=LOWER(?)"
                : "SELECT firstname, lastname FROM " + table + " WHERE LOWER(email)=LOWER(?)";
        String displayName;
        if (allowUsername) {
            displayName = executeSingleQuery(sql, rs -> {
                String username = rs.getString("username");
                if (username != null && !username.trim().isEmpty()) {
                    return username.trim();
                }
                return rs.getString("firstname") + " " + rs.getString("lastname");
            }, identifier, identifier);
        } else {
            displayName = executeSingleQuery(sql, rs -> rs.getString("firstname") + " " + rs.getString("lastname"), identifier);
        }
        return (displayName != null && !displayName.trim().isEmpty()) ? displayName : "User";
    }

    public String getEmailByIdentifier(String identifier, String table, boolean allowUsername) throws SQLException {
        String sql = allowUsername
                ? "SELECT email FROM " + table + " WHERE LOWER(email)=LOWER(?) OR LOWER(username)=LOWER(?)"
                : "SELECT email FROM " + table + " WHERE LOWER(email)=LOWER(?)";
        String email = allowUsername
                ? executeSingleQuery(sql, rs -> rs.getString("email"), identifier, identifier)
                : executeSingleQuery(sql, rs -> rs.getString("email"), identifier);
        return email;
    }

    public boolean identifierExistsInRole(String identifier, String chosenRole) throws SQLException {
        String table = tableForRole(chosenRole);
        if (table == null || identifier == null || identifier.trim().isEmpty()) {
            return false;
        }
        boolean allowUsername = supportsUsernameByRole(chosenRole);
        String sql = allowUsername
                ? "SELECT 1 FROM " + table + " WHERE LOWER(email)=LOWER(?) OR LOWER(username)=LOWER(?)"
                : "SELECT 1 FROM " + table + " WHERE LOWER(email)=LOWER(?)";
        Integer exists = allowUsername
                ? executeSingleQuery(sql, rs -> 1, identifier, identifier)
                : executeSingleQuery(sql, rs -> 1, identifier);
        return exists != null;
    }

    public String getCampusNameForRole(String role, int userId) throws SQLException {
        if (role == null || userId <= 0) {
            return null;
        }

        String sql;
        switch (role) {
            case "ADMIN":
                sql = "SELECT v.name AS campusName "
                        + "FROM admin a "
                        + "JOIN event e ON a.eventID = e.eventID "
                        + "JOIN venue v ON e.venueID = v.venueID "
                        + "WHERE a.adminID = ?";
                break;
            case "EVENT_MANAGER":
                sql = "SELECT v.name AS campusName "
                        + "FROM event_manager em "
                        + "JOIN venue_guard vg ON em.venueGuardID = vg.venueGuardID "
                        + "JOIN venue v ON vg.venueID = v.venueID "
                        + "WHERE em.eventManagerID = ?";
                break;
            case "TERTIARY_PRESENTER":
                sql = "SELECT v.name AS campusName "
                        + "FROM tertiary_presenter tp "
                        + "JOIN venue v ON tp.venueID = v.venueID "
                        + "WHERE tp.tertiaryPresenterID = ?";
                break;
            case "VENUE_GUARD":
                sql = "SELECT v.name AS campusName "
                        + "FROM venue_guard vg "
                        + "JOIN venue v ON vg.venueID = v.venueID "
                        + "WHERE vg.venueGuardID = ?";
                break;
            default:
                return null;
        }

        return executeSingleQuery(sql, rs -> rs.getString("campusName"), userId);
    }

    private boolean check(String identifier, String password, String table, boolean allowUsername) throws SQLException {
        String sql = allowUsername
                ? "SELECT password FROM " + table + " WHERE LOWER(email)=LOWER(?) OR LOWER(username)=LOWER(?)"
                : "SELECT password FROM " + table + " WHERE LOWER(email)=LOWER(?)";
        String storedPassword = allowUsername
                ? executeSingleQuery(sql, rs -> rs.getString("password"), identifier, identifier)
                : executeSingleQuery(sql, rs -> rs.getString("password"), identifier);
        return storedPassword != null && PasswordUtil.matches(password, storedPassword);
    }
}