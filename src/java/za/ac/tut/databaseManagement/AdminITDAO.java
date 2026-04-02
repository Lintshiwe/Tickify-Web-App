package za.ac.tut.databaseManagement;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import za.ac.tut.databaseConnection.DatabaseConnection;
import za.ac.tut.security.PasswordUtil;

public class AdminITDAO {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final String PRIVILEGED_ADMIN_EMAIL = "admin@tickify.ac.za";
    private static final String SYSTEM_SETTING_TABLE = "system_setting";
    private static final String MINOR_RESTRICTED_KEYWORDS_SETTING_KEY = "minor_restricted_event_keywords";
    private static final String MINOR_RESTRICTED_KEYWORDS_DEFAULT = "18+,adult,alcohol,club,night,nightlife,cocktail,wine,liquor,beer";

    private static final Set<String> PREVIEW_TABLES = new TreeSet<>(Arrays.asList(
            "admin", "event_manager", "venue_guard", "tertiary_presenter", "attendee",
            "event", "venue", "ticket", "attendee_has_ticket", "scan_log", "advert", "attendee_wishlist",
            "account_control", "admin_audit_log", "delete_request"
    ));

    private static final Map<String, String> SAFE_DELETE_PK = new HashMap<>();

    static {
        SAFE_DELETE_PK.put("admin", "adminID");
        SAFE_DELETE_PK.put("event_manager", "eventManagerID");
        SAFE_DELETE_PK.put("venue_guard", "venueGuardID");
        SAFE_DELETE_PK.put("tertiary_presenter", "tertiaryPresenterID");
        SAFE_DELETE_PK.put("attendee", "attendeeID");
        SAFE_DELETE_PK.put("event", "eventID");
        SAFE_DELETE_PK.put("venue", "venueID");
        SAFE_DELETE_PK.put("ticket", "ticketID");
        SAFE_DELETE_PK.put("scan_log", "scanLogID");
        SAFE_DELETE_PK.put("advert", "advertID");
        SAFE_DELETE_PK.put("account_control", "controlID");
        SAFE_DELETE_PK.put("admin_audit_log", "adminAuditLogID");
    }

    public Map<String, Object> getDashboardMetrics() throws SQLException {
        Map<String, Object> metrics = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            metrics.put("activeEvents", queryInt(conn,
                    "SELECT COUNT(*) FROM event WHERE date >= CURRENT_TIMESTAMP"));
            metrics.put("ticketsSold", queryInt(conn,
                    "SELECT COUNT(*) FROM attendee_has_ticket"));
            metrics.put("revenue", queryDouble(conn,
                    "SELECT COALESCE(SUM(t.price), 0) FROM attendee_has_ticket aht JOIN ticket t ON t.ticketID = aht.ticketID"));
            metrics.put("scannerUptime", calculateScannerUptime(conn));
        }
        return metrics;
    }

    public List<Map<String, Object>> getUserTableSummary() throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String[][] defs = {
            {"admin", "Admins"},
            {"event_manager", "Managers"},
            {"venue_guard", "Guards"},
            {"tertiary_presenter", "Presenters"},
            {"attendee", "Attendees"},
            {"event", "Events"},
            {"ticket", "Tickets"},
            {"scan_log", "Scan Logs"}
        };

        try (Connection conn = DatabaseConnection.getConnection()) {
            for (String[] def : defs) {
                Map<String, Object> row = new HashMap<>();
                row.put("table", def[0]);
                row.put("label", def[1]);
                row.put("count", queryInt(conn, "SELECT COUNT(*) FROM " + def[0]));
                rows.add(row);
            }
        }

        return rows;
    }

    public List<Map<String, Object>> getAdmins() throws SQLException {
        return runListQuery("SELECT a.adminID, a.firstname, a.lastname, a.email, a.eventID, v.name AS campusName "
                + "FROM admin a "
                + "LEFT JOIN event e ON e.eventID = a.eventID "
                + "LEFT JOIN venue v ON v.venueID = e.venueID "
                + "ORDER BY a.adminID DESC", Collections.emptyList());
    }

    public List<Map<String, Object>> getAdminsForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return getAdmins();
        }
        return runListQuery("SELECT a.adminID, a.firstname, a.lastname, a.email, a.eventID, v.name AS campusName "
                + "FROM admin a JOIN event e ON e.eventID = a.eventID "
            + "LEFT JOIN venue v ON v.venueID = e.venueID "
                + "WHERE e.venueID = ? ORDER BY a.adminID DESC",
                Arrays.<Object>asList(getAdminCampusVenueId(adminId)));
    }

    public List<Map<String, Object>> getGuards() throws SQLException {
        return runListQuery("SELECT g.venueGuardID, g.firstname, g.lastname, g.email, g.eventID, g.venueID, v.name AS campusName "
                + "FROM venue_guard g "
                + "LEFT JOIN venue v ON v.venueID = g.venueID "
                + "ORDER BY g.venueGuardID DESC", Collections.emptyList());
    }

    public List<Map<String, Object>> getGuardsForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return getGuards();
        }
        return runListQuery("SELECT g.venueGuardID, g.firstname, g.lastname, g.email, g.eventID, g.venueID, v.name AS campusName "
            + "FROM venue_guard g "
            + "LEFT JOIN venue v ON v.venueID = g.venueID "
            + "WHERE g.venueID = ? ORDER BY g.venueGuardID DESC",
                Arrays.<Object>asList(getAdminCampusVenueId(adminId)));
    }

    public List<Map<String, Object>> getManagers() throws SQLException {
        return runListQuery("SELECT m.eventManagerID, m.firstname, m.lastname, m.email, m.venueGuardID, v.name AS campusName "
                + "FROM event_manager m "
                + "LEFT JOIN venue_guard g ON g.venueGuardID = m.venueGuardID "
                + "LEFT JOIN venue v ON v.venueID = g.venueID "
                + "ORDER BY m.eventManagerID DESC", Collections.emptyList());
    }

    public List<Map<String, Object>> getManagersForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return getManagers();
        }
        return runListQuery("SELECT m.eventManagerID, m.firstname, m.lastname, m.email, m.venueGuardID, v.name AS campusName "
                + "FROM event_manager m "
                + "JOIN venue_guard g ON g.venueGuardID = m.venueGuardID "
            + "LEFT JOIN venue v ON v.venueID = g.venueID "
                + "WHERE g.venueID = ? ORDER BY m.eventManagerID DESC",
                Arrays.<Object>asList(getAdminCampusVenueId(adminId)));
    }

    public List<Map<String, Object>> getPresenters() throws SQLException {
        return runListQuery("SELECT p.tertiaryPresenterID, p.firstname, p.lastname, p.email, p.tertiaryInstitution, p.eventID, p.venueID, v.name AS campusName "
                + "FROM tertiary_presenter p "
                + "LEFT JOIN venue v ON v.venueID = p.venueID "
                + "ORDER BY p.tertiaryPresenterID DESC", Collections.emptyList());
    }

    public List<Map<String, Object>> getPresentersForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return getPresenters();
        }
        return runListQuery("SELECT p.tertiaryPresenterID, p.firstname, p.lastname, p.email, p.tertiaryInstitution, p.eventID, p.venueID, v.name AS campusName "
            + "FROM tertiary_presenter p "
            + "LEFT JOIN venue v ON v.venueID = p.venueID "
            + "WHERE p.venueID = ? ORDER BY p.tertiaryPresenterID DESC",
                Arrays.<Object>asList(getAdminCampusVenueId(adminId)));
    }

    public List<Map<String, Object>> getEventOptions() throws SQLException {
        return runListQuery("SELECT eventID, name FROM event ORDER BY date ASC", Collections.emptyList());
    }

    public List<Map<String, Object>> getEventOptionsForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return getEventOptions();
        }
        return runListQuery("SELECT eventID, name FROM event WHERE venueID = ? ORDER BY date ASC",
                Arrays.<Object>asList(getAdminCampusVenueId(adminId)));
    }

    public List<Map<String, Object>> getEventControlRowsForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return runListQuery("SELECT e.eventID, e.name, e.type, e.date, COALESCE(NULLIF(TRIM(e.description), ''), '') AS description, "
                    + "COALESCE(NULLIF(TRIM(e.infoUrl), ''), '') AS infoUrl, COALESCE(NULLIF(TRIM(e.status), ''), 'ACTIVE') AS status, "
                    + "e.venueID, v.name AS campusName, v.address AS campusAddress "
                    + "FROM event e LEFT JOIN venue v ON v.venueID = e.venueID "
                    + "ORDER BY e.date DESC", Collections.emptyList());
        }
        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (campusVenueId == null || campusVenueId <= 0) {
            return new ArrayList<>();
        }
        return runListQuery("SELECT e.eventID, e.name, e.type, e.date, COALESCE(NULLIF(TRIM(e.description), ''), '') AS description, "
                + "COALESCE(NULLIF(TRIM(e.infoUrl), ''), '') AS infoUrl, COALESCE(NULLIF(TRIM(e.status), ''), 'ACTIVE') AS status, "
                + "e.venueID, v.name AS campusName, v.address AS campusAddress "
                + "FROM event e LEFT JOIN venue v ON v.venueID = e.venueID "
                + "WHERE e.venueID = ? ORDER BY e.date DESC", Arrays.<Object>asList(campusVenueId));
    }

    public int countEventControlRowsForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                return queryInt(conn, "SELECT COUNT(*) FROM event");
            }
        }
        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (campusVenueId == null || campusVenueId <= 0) {
            return 0;
        }
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM event WHERE venueID = ?")) {
            ps.setInt(1, campusVenueId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Map<String, Object>> getEventControlRowsForScope(int adminId, int page, int pageSize) throws SQLException {
        int safePage = page <= 0 ? 1 : page;
        int safePageSize = pageSize <= 0 ? 10 : pageSize;
        int offset = (safePage - 1) * safePageSize;
        if (isPrivilegedAdmin(adminId)) {
            return runListQuery("SELECT e.eventID, e.name, e.type, e.date, COALESCE(NULLIF(TRIM(e.description), ''), '') AS description, "
                    + "COALESCE(NULLIF(TRIM(e.infoUrl), ''), '') AS infoUrl, COALESCE(NULLIF(TRIM(e.status), ''), 'ACTIVE') AS status, "
                    + "e.venueID, v.name AS campusName, v.address AS campusAddress "
                    + "FROM event e LEFT JOIN venue v ON v.venueID = e.venueID "
                    + "ORDER BY e.date DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                    Arrays.<Object>asList(offset, safePageSize));
        }
        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (campusVenueId == null || campusVenueId <= 0) {
            return new ArrayList<>();
        }
        return runListQuery("SELECT e.eventID, e.name, e.type, e.date, COALESCE(NULLIF(TRIM(e.description), ''), '') AS description, "
                + "COALESCE(NULLIF(TRIM(e.infoUrl), ''), '') AS infoUrl, COALESCE(NULLIF(TRIM(e.status), ''), 'ACTIVE') AS status, "
                + "e.venueID, v.name AS campusName, v.address AS campusAddress "
                + "FROM event e LEFT JOIN venue v ON v.venueID = e.venueID "
                + "WHERE e.venueID = ? ORDER BY e.date DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
                Arrays.<Object>asList(campusVenueId, offset, safePageSize));
    }

    public int createEvent(int adminId, String name, String type, Timestamp eventDate,
            int venueId, String description, String infoUrl, String status) throws SQLException {
        if (name == null || name.trim().isEmpty() || type == null || type.trim().isEmpty() || eventDate == null || venueId <= 0) {
            throw new SQLException("MissingFields");
        }

        if (!hasCampusAccessForRoleMutation(adminId, "VENUE_GUARD", null, null, venueId, null)) {
            throw new SQLException("CampusScopeDenied");
        }

        if (!existsById(venueId, "venue", "venueID")) {
            throw new SQLException("InvalidAssignment");
        }

        String normalizedStatus = normalizeEventStatus(status);
        String normalizedDescription = normalizeOptionalText(description, 1200);
        String normalizedInfoUrl = normalizeOptionalText(infoUrl, 255);

        String sql = "INSERT INTO event(name, type, date, venueID, description, infoUrl, status) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.setString(2, type.trim());
            ps.setTimestamp(3, eventDate);
            ps.setInt(4, venueId);
            ps.setString(5, normalizedDescription);
            ps.setString(6, normalizedInfoUrl);
            ps.setString(7, normalizedStatus);
            int affected = ps.executeUpdate();
            if (affected <= 0) {
                return 0;
            }
            int newEventId = 0;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    newEventId = keys.getInt(1);
                }
            }
            logAudit(conn, adminId, "CREATE_EVENT", "event", String.valueOf(newEventId), "Created event '" + name.trim() + "'");
            return newEventId;
        }
    }

    public boolean updateEvent(int adminId, int eventId, String name, String type, Timestamp eventDate,
            int venueId, String description, String infoUrl, String status) throws SQLException {
        if (eventId <= 0 || name == null || name.trim().isEmpty() || type == null || type.trim().isEmpty() || eventDate == null || venueId <= 0) {
            throw new SQLException("MissingFields");
        }

        if (!hasCampusAccessForRoleMutation(adminId, "ADMIN", null, eventId, null, null)
                || !hasCampusAccessForRoleMutation(adminId, "VENUE_GUARD", null, null, venueId, null)) {
            throw new SQLException("CampusScopeDenied");
        }

        if (!existsById(venueId, "venue", "venueID")) {
            throw new SQLException("InvalidAssignment");
        }

        String normalizedStatus = normalizeEventStatus(status);
        String normalizedDescription = normalizeOptionalText(description, 1200);
        String normalizedInfoUrl = normalizeOptionalText(infoUrl, 255);

        String sql = "UPDATE event SET name=?, type=?, date=?, venueID=?, description=?, infoUrl=?, status=? WHERE eventID=?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, type.trim());
            ps.setTimestamp(3, eventDate);
            ps.setInt(4, venueId);
            ps.setString(5, normalizedDescription);
            ps.setString(6, normalizedInfoUrl);
            ps.setString(7, normalizedStatus);
            ps.setInt(8, eventId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logAudit(conn, adminId, "UPDATE_EVENT", "event", String.valueOf(eventId), "Updated event fields");
                return true;
            }
            return false;
        }
    }

    public boolean updateEventAlbumImageForScope(int adminId, int eventId,
            String filename, String mimeType, byte[] imageData) throws SQLException {
        if (adminId <= 0 || eventId <= 0 || imageData == null || imageData.length == 0) {
            throw new SQLException("MissingFields");
        }
        if (!hasCampusAccessForRoleMutation(adminId, "ADMIN", null, eventId, null, null)) {
            throw new SQLException("CampusScopeDenied");
        }
        String normalizedFilename = normalizeOptionalText(filename, 255);
        String normalizedMimeType = normalizeOptionalText(mimeType, 100);
        if (normalizedMimeType == null) {
            normalizedMimeType = "image/jpeg";
        }

        String sql = "UPDATE event SET imageFilename = ?, imageMimeType = ?, imageData = ? WHERE eventID = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedFilename);
            ps.setString(2, normalizedMimeType);
            ps.setBytes(3, imageData);
            ps.setInt(4, eventId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logAudit(conn, adminId, "UPLOAD_EVENT_COVER", "event", String.valueOf(eventId), "Updated event album cover");
                return true;
            }
            return false;
        }
    }

    public boolean clearEventAlbumImageForScope(int adminId, int eventId) throws SQLException {
        if (adminId <= 0 || eventId <= 0) {
            throw new SQLException("MissingFields");
        }
        if (!hasCampusAccessForRoleMutation(adminId, "ADMIN", null, eventId, null, null)) {
            throw new SQLException("CampusScopeDenied");
        }

        String sql = "UPDATE event SET imageFilename = NULL, imageMimeType = NULL, imageData = NULL WHERE eventID = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logAudit(conn, adminId, "CLEAR_EVENT_COVER", "event", String.valueOf(eventId), "Removed event album cover");
                return true;
            }
            return false;
        }
    }

    public List<Map<String, Object>> getTicketControlRowsForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return runListQuery("SELECT t.ticketID, t.name AS ticketName, t.price, e.eventID, e.name AS eventName, "
                    + "v.venueID, v.name AS campusName, COUNT(DISTINCT aht.attendeeID) AS soldCount, "
                    + "COUNT(DISTINCT sl.scanLogID) AS scanCount "
                    + "FROM ticket t "
                    + "LEFT JOIN event_has_ticket eht ON eht.ticketID = t.ticketID "
                    + "LEFT JOIN event e ON e.eventID = eht.eventID "
                    + "LEFT JOIN venue v ON v.venueID = e.venueID "
                    + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = t.ticketID "
                    + "LEFT JOIN scan_log sl ON sl.ticketID = t.ticketID "
                    + "GROUP BY t.ticketID, t.name, t.price, e.eventID, e.name, v.venueID, v.name "
                    + "ORDER BY t.ticketID DESC FETCH FIRST 400 ROWS ONLY", Collections.emptyList());
        }
        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (campusVenueId == null || campusVenueId <= 0) {
            return new ArrayList<>();
        }
        return runListQuery("SELECT t.ticketID, t.name AS ticketName, t.price, e.eventID, e.name AS eventName, "
                + "v.venueID, v.name AS campusName, COUNT(DISTINCT aht.attendeeID) AS soldCount, "
                + "COUNT(DISTINCT sl.scanLogID) AS scanCount "
                + "FROM ticket t "
                + "LEFT JOIN event_has_ticket eht ON eht.ticketID = t.ticketID "
                + "LEFT JOIN event e ON e.eventID = eht.eventID "
                + "LEFT JOIN venue v ON v.venueID = e.venueID "
                + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = t.ticketID "
                + "LEFT JOIN scan_log sl ON sl.ticketID = t.ticketID "
                + "WHERE v.venueID = ? "
                + "GROUP BY t.ticketID, t.name, t.price, e.eventID, e.name, v.venueID, v.name "
                + "ORDER BY t.ticketID DESC FETCH FIRST 400 ROWS ONLY", Arrays.<Object>asList(campusVenueId));
    }

    public boolean createTicketsForEvent(int adminId, int eventId, String ticketName, BigDecimal price, int quantity) throws SQLException {
        if (eventId <= 0 || ticketName == null || ticketName.trim().isEmpty() || price == null || quantity <= 0) {
            throw new SQLException("MissingFields");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new SQLException("InvalidAssignment");
        }
        if (!existsById(eventId, "event", "eventID")) {
            throw new SQLException("InvalidAssignment");
        }
        if (!hasCampusAccessForRoleMutation(adminId, "ADMIN", null, eventId, null, null)) {
            throw new SQLException("CampusScopeDenied");
        }

        String normalizedName = normalizeOptionalText(ticketName, 45);
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new SQLException("MissingFields");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ticketInsert = conn.prepareStatement(
                    "INSERT INTO ticket(name, price, QRcodeID) VALUES(?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement mapInsert = conn.prepareStatement(
                            "INSERT INTO event_has_ticket(eventID, ticketID) VALUES(?, ?)")) {
                long marker = System.currentTimeMillis();
                int created = 0;
                for (int i = 1; i <= quantity; i++) {
                    int qrId = createQrRecord(conn, "ADM-" + eventId + "-" + marker + "-" + i);

                    String generatedName = quantity == 1
                            ? normalizedName
                            : normalizedName + " #" + i;
                    ticketInsert.setString(1, generatedName);
                    ticketInsert.setBigDecimal(2, price);
                    ticketInsert.setInt(3, qrId);
                    ticketInsert.executeUpdate();

                    int ticketId;
                    try (ResultSet ticketKeys = ticketInsert.getGeneratedKeys()) {
                        if (!ticketKeys.next()) {
                            conn.rollback();
                            return false;
                        }
                        ticketId = ticketKeys.getInt(1);
                    }

                    mapInsert.setInt(1, eventId);
                    mapInsert.setInt(2, ticketId);
                    mapInsert.executeUpdate();
                    created++;
                }
                logAudit(conn, adminId, "CREATE_TICKET", "ticket", String.valueOf(eventId),
                        "Created " + created + " ticket(s) for event " + eventId);
                conn.commit();
                return created > 0;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean updateTicket(int adminId, int ticketId, int eventId, String ticketName, BigDecimal price) throws SQLException {
        if (!isPrivilegedAdmin(adminId)) {
            throw new SQLException("PrivilegedRequired");
        }
        if (ticketId <= 0 || eventId <= 0 || ticketName == null || ticketName.trim().isEmpty() || price == null) {
            throw new SQLException("MissingFields");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new SQLException("InvalidAssignment");
        }
        if (!existsById(eventId, "event", "eventID") || !existsById(ticketId, "ticket", "ticketID")) {
            throw new SQLException("InvalidAssignment");
        }

        String normalizedName = normalizeOptionalText(ticketName, 45);
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new SQLException("MissingFields");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            int soldCount = 0;
            try (PreparedStatement soldPs = conn.prepareStatement("SELECT COUNT(*) FROM attendee_has_ticket WHERE ticketID = ?")) {
                soldPs.setInt(1, ticketId);
                try (ResultSet rs = soldPs.executeQuery()) {
                    if (rs.next()) {
                        soldCount = rs.getInt(1);
                    }
                }
            }
            if (soldCount > 0) {
                throw new SQLException("TicketHasSales");
            }

            conn.setAutoCommit(false);
            try {
                try (PreparedStatement updateTicketPs = conn.prepareStatement("UPDATE ticket SET name = ?, price = ? WHERE ticketID = ?")) {
                    updateTicketPs.setString(1, normalizedName);
                    updateTicketPs.setBigDecimal(2, price);
                    updateTicketPs.setInt(3, ticketId);
                    if (updateTicketPs.executeUpdate() <= 0) {
                        conn.rollback();
                        return false;
                    }
                }

                try (PreparedStatement deleteMaps = conn.prepareStatement("DELETE FROM event_has_ticket WHERE ticketID = ?")) {
                    deleteMaps.setInt(1, ticketId);
                    deleteMaps.executeUpdate();
                }

                try (PreparedStatement insertMap = conn.prepareStatement("INSERT INTO event_has_ticket(eventID, ticketID) VALUES(?, ?)")) {
                    insertMap.setInt(1, eventId);
                    insertMap.setInt(2, ticketId);
                    insertMap.executeUpdate();
                }

                logAudit(conn, adminId, "UPDATE_TICKET", "ticket", String.valueOf(ticketId),
                        "Updated ticket details and event mapping");
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean deleteTicket(int adminId, int ticketId) throws SQLException {
        if (!isPrivilegedAdmin(adminId)) {
            throw new SQLException("PrivilegedRequired");
        }
        if (ticketId <= 0) {
            throw new SQLException("MissingFields");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            int soldCount = 0;
            try (PreparedStatement soldPs = conn.prepareStatement("SELECT COUNT(*) FROM attendee_has_ticket WHERE ticketID = ?")) {
                soldPs.setInt(1, ticketId);
                try (ResultSet rs = soldPs.executeQuery()) {
                    if (rs.next()) {
                        soldCount = rs.getInt(1);
                    }
                }
            }
            if (soldCount > 0) {
                throw new SQLException("TicketHasSales");
            }

            int scanCount = 0;
            try (PreparedStatement scanPs = conn.prepareStatement("SELECT COUNT(*) FROM scan_log WHERE ticketID = ?")) {
                scanPs.setInt(1, ticketId);
                try (ResultSet rs = scanPs.executeQuery()) {
                    if (rs.next()) {
                        scanCount = rs.getInt(1);
                    }
                }
            }
            if (scanCount > 0) {
                throw new SQLException("TicketHasScans");
            }

            Integer qrCodeId = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT QRcodeID FROM ticket WHERE ticketID = ?")) {
                ps.setInt(1, ticketId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        qrCodeId = rs.getInt(1);
                    }
                }
            }

            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM eventmanager_has_ticket WHERE ticketID = ?")) {
                    ps.setInt(1, ticketId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM event_has_ticket WHERE ticketID = ?")) {
                    ps.setInt(1, ticketId);
                    ps.executeUpdate();
                }
                int affected;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ticket WHERE ticketID = ?")) {
                    ps.setInt(1, ticketId);
                    affected = ps.executeUpdate();
                }
                if (affected <= 0) {
                    conn.rollback();
                    return false;
                }

                if (qrCodeId != null && qrCodeId > 0) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM qrcode WHERE QRcodeID = ?")) {
                        ps.setInt(1, qrCodeId);
                        ps.executeUpdate();
                    }
                }

                logAudit(conn, adminId, "DELETE_TICKET", "ticket", String.valueOf(ticketId), "Deleted ticket");
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean deleteEvent(int adminId, int eventId) throws SQLException {
        if (eventId <= 0) {
            throw new SQLException("MissingFields");
        }
        if (!hasCampusAccessForRoleMutation(adminId, "ADMIN", null, eventId, null, null)) {
            throw new SQLException("CampusScopeDenied");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            int sold = 0;
            try (PreparedStatement soldPs = conn.prepareStatement(
                    "SELECT COUNT(*) FROM attendee_has_ticket aht "
                    + "JOIN event_has_ticket eht ON eht.ticketID = aht.ticketID "
                    + "WHERE eht.eventID = ?")) {
                soldPs.setInt(1, eventId);
                try (ResultSet rs = soldPs.executeQuery()) {
                    if (rs.next()) {
                        sold = rs.getInt(1);
                    }
                }
            }

            if (sold > 0) {
                throw new SQLException("EventHasSales");
            }

            try (PreparedStatement detachManagers = conn.prepareStatement("DELETE FROM event_has_manager WHERE eventID = ?")) {
                detachManagers.setInt(1, eventId);
                detachManagers.executeUpdate();
            }
            try (PreparedStatement detachTickets = conn.prepareStatement("DELETE FROM event_has_ticket WHERE eventID = ?")) {
                detachTickets.setInt(1, eventId);
                detachTickets.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM event WHERE eventID = ?")) {
                ps.setInt(1, eventId);
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    logAudit(conn, adminId, "DELETE_EVENT", "event", String.valueOf(eventId), "Deleted event");
                    return true;
                }
            }
            return false;
        }
    }

    public int cleanupRetiredEvents(int adminId) throws SQLException {
        if (!isPrivilegedAdmin(adminId)) {
            throw new SQLException("PrivilegedRequired");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<Integer> candidateIds = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT eventID FROM event "
                        + "WHERE date < CURRENT_TIMESTAMP OR UPPER(COALESCE(status,'')) IN ('CANCELLED','PASSED')")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            candidateIds.add(rs.getInt(1));
                        }
                    }
                }

                int deleted = 0;
                for (Integer eventId : candidateIds) {
                    if (eventId == null || eventId <= 0) {
                        continue;
                    }
                    if (!isRetiredEventDeletable(conn, eventId)) {
                        continue;
                    }
                    if (deleteEventGraph(conn, adminId, eventId)) {
                        deleted++;
                    }
                }

                conn.commit();
                return deleted;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private boolean isRetiredEventDeletable(Connection conn, int eventId) throws SQLException {
        int sold;
        try (PreparedStatement soldPs = conn.prepareStatement(
                "SELECT COUNT(*) FROM attendee_has_ticket aht "
                + "JOIN event_has_ticket eht ON eht.ticketID = aht.ticketID "
                + "WHERE eht.eventID = ?")) {
            soldPs.setInt(1, eventId);
            try (ResultSet rs = soldPs.executeQuery()) {
                sold = rs.next() ? rs.getInt(1) : 0;
            }
        }
        if (sold > 0) {
            return false;
        }

        if (hasReferences(conn, "admin", "eventID", eventId)) {
            return false;
        }
        if (hasReferences(conn, "venue_guard", "eventID", eventId)) {
            return false;
        }
        if (hasReferences(conn, "tertiary_presenter", "eventID", eventId)) {
            return false;
        }
        return true;
    }

    private boolean hasReferences(Connection conn, String table, String column, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean deleteEventGraph(Connection conn, int adminId, int eventId) throws SQLException {
        List<Integer> ticketIds = new ArrayList<>();
        List<Integer> qrIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT t.ticketID, t.QRcodeID "
                + "FROM event_has_ticket eht "
                + "JOIN ticket t ON t.ticketID = eht.ticketID "
                + "WHERE eht.eventID = ?")) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ticketIds.add(rs.getInt("ticketID"));
                    qrIds.add(rs.getInt("QRcodeID"));
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM attendee_wishlist WHERE eventID = ?")) {
            ps.setInt(1, eventId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM attendee_has_event WHERE eventID = ?")) {
            ps.setInt(1, eventId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM event_has_manager WHERE eventID = ?")) {
            ps.setInt(1, eventId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM event_has_ticket WHERE eventID = ?")) {
            ps.setInt(1, eventId);
            ps.executeUpdate();
        }

        for (Integer ticketId : ticketIds) {
            if (ticketId == null || ticketId <= 0) {
                continue;
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM eventmanager_has_ticket WHERE ticketID = ?")) {
                ps.setInt(1, ticketId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ticket WHERE ticketID = ?")) {
                ps.setInt(1, ticketId);
                ps.executeUpdate();
            }
        }

        for (Integer qrId : qrIds) {
            if (qrId == null || qrId <= 0) {
                continue;
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM qrcode WHERE QRcodeID = ?")) {
                ps.setInt(1, qrId);
                ps.executeUpdate();
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM event WHERE eventID = ?")) {
            ps.setInt(1, eventId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logAudit(conn, adminId, "CLEANUP_EVENT", "event", String.valueOf(eventId), "Auto/manual cleanup deleted retired event");
                return true;
            }
        }
        return false;
    }

    public List<Map<String, Object>> getVenueOptions() throws SQLException {
        return runListQuery("SELECT venueID, name FROM venue ORDER BY name ASC", Collections.emptyList());
    }

    public List<Map<String, Object>> getVenueOptionsForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return getVenueOptions();
        }
        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (campusVenueId == null || campusVenueId <= 0) {
            return new ArrayList<>();
        }
        return runListQuery("SELECT venueID, name FROM venue WHERE venueID = ? ORDER BY name ASC",
                Arrays.<Object>asList(campusVenueId));
    }

    public List<Map<String, Object>> getGuardOptions() throws SQLException {
        return runListQuery("SELECT venueGuardID, firstname, lastname, email FROM venue_guard ORDER BY venueGuardID DESC", Collections.emptyList());
    }

    public List<Map<String, Object>> getGuardOptionsForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return getGuardOptions();
        }
        return runListQuery("SELECT venueGuardID, firstname, lastname, email FROM venue_guard WHERE venueID = ? ORDER BY venueGuardID DESC",
                Arrays.<Object>asList(getAdminCampusVenueId(adminId)));
    }

    public boolean isPrivilegedAdmin(int adminId) throws SQLException {
        if (adminId <= 0) {
            return false;
        }
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT email FROM admin WHERE adminID = ?")) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String email = rs.getString("email");
                return email != null && PRIVILEGED_ADMIN_EMAIL.equalsIgnoreCase(email.trim());
            }
        }
    }

    public boolean verifyPrivilegedRootPassword(int adminId, String providedPassword) throws SQLException {
        if (providedPassword == null || providedPassword.trim().isEmpty() || adminId <= 0) {
            return false;
        }
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT email, password FROM admin WHERE adminID = ?")) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String email = rs.getString("email");
                String stored = rs.getString("password");
                if (email == null || !PRIVILEGED_ADMIN_EMAIL.equalsIgnoreCase(email.trim())) {
                    return false;
                }
                return PasswordUtil.matches(providedPassword, stored);
            }
        }
    }

    public Map<String, Object> getAdminProfile(int adminId) throws SQLException {
        if (adminId <= 0) {
            return new HashMap<>();
        }
        List<Map<String, Object>> rows = runListQuery(
                "SELECT adminID, firstname, lastname, email FROM admin WHERE adminID = ?",
                Arrays.<Object>asList(adminId));
        if (rows.isEmpty()) {
            return new HashMap<>();
        }
        return rows.get(0);
    }

    public boolean updateOwnAdminProfile(int adminId, String firstName, String lastName, String email, String newPassword) throws SQLException {
        if (adminId <= 0) {
            return false;
        }
        String safeFirstName = firstName == null ? "" : firstName.trim();
        String safeLastName = lastName == null ? "" : lastName.trim();
        String safeEmail = email == null ? "" : email.trim().toLowerCase();
        String safeNewPassword = newPassword == null ? "" : newPassword.trim();

        if (safeFirstName.isEmpty() || safeLastName.isEmpty() || safeEmail.isEmpty()) {
            throw new SQLException("MissingFields");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement exists = conn.prepareStatement(
                    "SELECT 1 FROM admin WHERE LOWER(email) = LOWER(?) AND adminID <> ?")) {
                exists.setString(1, safeEmail);
                exists.setInt(2, adminId);
                try (ResultSet rs = exists.executeQuery()) {
                    if (rs.next()) {
                        throw new SQLException("EmailInUse");
                    }
                }
            }

            String sql;
            if (safeNewPassword.isEmpty()) {
                sql = "UPDATE admin SET firstname = ?, lastname = ?, email = ? WHERE adminID = ?";
            } else {
                sql = "UPDATE admin SET firstname = ?, lastname = ?, email = ?, password = ? WHERE adminID = ?";
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                ps.setString(idx++, safeFirstName);
                ps.setString(idx++, safeLastName);
                ps.setString(idx++, safeEmail);
                if (!safeNewPassword.isEmpty()) {
                    ps.setString(idx++, PasswordUtil.hashPassword(safeNewPassword));
                }
                ps.setInt(idx, adminId);
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    logAudit(conn, adminId, "UPDATE_SELF_PROFILE", "admin", String.valueOf(adminId),
                            safeNewPassword.isEmpty() ? "Updated own admin profile" : "Updated own admin profile and password");
                    return true;
                }
                return false;
            }
        }
    }

    public List<Map<String, Object>> getIdentityDirectory() throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(withRole(getAdmins(), "ADMIN", "adminID"));
        rows.addAll(withRole(getGuards(), "VENUE_GUARD", "venueGuardID"));
        rows.addAll(withRole(getManagers(), "EVENT_MANAGER", "eventManagerID"));
        rows.addAll(withRole(getPresenters(), "TERTIARY_PRESENTER", "tertiaryPresenterID"));
        return rows;
    }

    public List<String> getUserExportCampusNamesForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            List<Map<String, Object>> rows = runListQuery(
                    "SELECT DISTINCT name AS campusName FROM venue WHERE name IS NOT NULL ORDER BY name ASC",
                    Collections.emptyList());
            List<String> names = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                String name = stringValue(row.get("campusName")).trim();
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
            return names;
        }

        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (campusVenueId == null || campusVenueId <= 0) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> rows = runListQuery(
                "SELECT DISTINCT name AS campusName FROM venue WHERE venueID = ? AND name IS NOT NULL ORDER BY name ASC",
                Arrays.<Object>asList(campusVenueId));
        List<String> names = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String name = stringValue(row.get("campusName")).trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    public List<Map<String, Object>> getUserDirectoryForExport(int adminId, String roleFilter,
            String campusFilter, String keyword, boolean includeAllForPrivileged) throws SQLException {
        boolean privileged = isPrivilegedAdmin(adminId);
        Integer scopedVenueId = getAdminCampusVenueId(adminId);

        String normalizedRole = roleFilter == null ? "ALL" : roleFilter.trim().toUpperCase();
        if (normalizedRole.isEmpty()) {
            normalizedRole = "ALL";
        }
        String normalizedCampus = campusFilter == null ? "" : campusFilter.trim();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();

        String baseSql =
                "SELECT * FROM ("
                + "SELECT 'ADMIN' AS role, a.adminID AS userID, CAST(NULL AS VARCHAR(45)) AS username, "
                + "a.firstname, a.lastname, a.email, v.name AS campusName, a.eventID AS eventID, v.venueID AS venueID "
                + "FROM admin a "
                + "LEFT JOIN event e ON e.eventID = a.eventID "
                + "LEFT JOIN venue v ON v.venueID = e.venueID "
                + "UNION ALL "
                + "SELECT 'VENUE_GUARD' AS role, g.venueGuardID AS userID, CAST(NULL AS VARCHAR(45)) AS username, "
                + "g.firstname, g.lastname, g.email, v.name AS campusName, g.eventID AS eventID, g.venueID AS venueID "
                + "FROM venue_guard g "
                + "LEFT JOIN venue v ON v.venueID = g.venueID "
                + "UNION ALL "
                + "SELECT 'EVENT_MANAGER' AS role, m.eventManagerID AS userID, CAST(NULL AS VARCHAR(45)) AS username, "
                + "m.firstname, m.lastname, m.email, v.name AS campusName, CAST(NULL AS INT) AS eventID, v.venueID AS venueID "
                + "FROM event_manager m "
                + "LEFT JOIN venue_guard g ON g.venueGuardID = m.venueGuardID "
                + "LEFT JOIN venue v ON v.venueID = g.venueID "
                + "UNION ALL "
                + "SELECT 'TERTIARY_PRESENTER' AS role, p.tertiaryPresenterID AS userID, p.username AS username, "
                + "p.firstname, p.lastname, p.email, v.name AS campusName, p.eventID AS eventID, p.venueID AS venueID "
                + "FROM tertiary_presenter p "
                + "LEFT JOIN venue v ON v.venueID = p.venueID "
                + "UNION ALL "
                + "SELECT 'ATTENDEE' AS role, a.attendeeID AS userID, a.username AS username, "
                + "a.firstname, a.lastname, a.email, MIN(v.name) AS campusName, MIN(e.eventID) AS eventID, MIN(v.venueID) AS venueID "
                + "FROM attendee a "
                + "LEFT JOIN attendee_has_event ahe ON ahe.attendeeID = a.attendeeID "
                + "LEFT JOIN event e ON e.eventID = ahe.eventID "
                + "LEFT JOIN venue v ON v.venueID = e.venueID "
                + "GROUP BY a.attendeeID, a.username, a.firstname, a.lastname, a.email"
                + ") u WHERE 1=1";

        StringBuilder sql = new StringBuilder(baseSql);
        List<Object> params = new ArrayList<>();

        if (!"ALL".equals(normalizedRole)) {
            sql.append(" AND UPPER(u.role) = ?");
            params.add(normalizedRole);
        }

        if (!normalizedCampus.isEmpty()) {
            sql.append(" AND LOWER(COALESCE(u.campusName, '')) = LOWER(?)");
            params.add(normalizedCampus);
        }

        if (!normalizedKeyword.isEmpty()) {
            sql.append(" AND (LOWER(COALESCE(u.username, '')) LIKE ? "
                    + "OR LOWER(COALESCE(u.firstname, '')) LIKE ? "
                    + "OR LOWER(COALESCE(u.lastname, '')) LIKE ? "
                    + "OR LOWER(COALESCE(u.email, '')) LIKE ?)");
            String like = "%" + normalizedKeyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        if (!(privileged && includeAllForPrivileged)) {
            if (scopedVenueId == null || scopedVenueId <= 0) {
                return new ArrayList<>();
            }
            sql.append(" AND u.venueID = ?");
            params.add(scopedVenueId);
        }

        sql.append(" ORDER BY u.role ASC, u.userID DESC");
        return runListQuery(sql.toString(), params);
    }

    public List<Map<String, Object>> getTicketDirectoryForExport(int adminId,
            String campusFilter, Integer eventIdFilter, String keyword,
            boolean includeAllForPrivileged) throws SQLException {
        boolean privileged = isPrivilegedAdmin(adminId);
        Integer scopedVenueId = getAdminCampusVenueId(adminId);

        String normalizedCampus = campusFilter == null ? "" : campusFilter.trim();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();

        StringBuilder sql = new StringBuilder(
                "SELECT t.ticketID, t.name AS ticketName, t.price, t.QRcodeID, qr.barstring AS qrCode, "
                + "e.eventID, e.name AS eventName, v.venueID, v.name AS campusName, "
                + "a.attendeeID, a.username, a.firstname, a.lastname, a.email "
                + "FROM ticket t "
                + "LEFT JOIN qrcode qr ON qr.QRcodeID = t.QRcodeID "
                + "LEFT JOIN event_has_ticket eht ON eht.ticketID = t.ticketID "
                + "LEFT JOIN event e ON e.eventID = eht.eventID "
                + "LEFT JOIN venue v ON v.venueID = e.venueID "
                + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = t.ticketID "
                + "LEFT JOIN attendee a ON a.attendeeID = aht.attendeeID "
                + "WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (!normalizedCampus.isEmpty()) {
            sql.append(" AND LOWER(COALESCE(v.name, '')) = LOWER(?)");
            params.add(normalizedCampus);
        }

        if (eventIdFilter != null && eventIdFilter > 0) {
            sql.append(" AND e.eventID = ?");
            params.add(eventIdFilter);
        }

        if (!normalizedKeyword.isEmpty()) {
            String like = "%" + normalizedKeyword + "%";
            sql.append(" AND (LOWER(COALESCE(t.name, '')) LIKE ? "
                    + "OR LOWER(COALESCE(e.name, '')) LIKE ? "
                    + "OR LOWER(COALESCE(v.name, '')) LIKE ? "
                    + "OR LOWER(COALESCE(a.username, '')) LIKE ? "
                    + "OR LOWER(COALESCE(a.firstname, '')) LIKE ? "
                    + "OR LOWER(COALESCE(a.lastname, '')) LIKE ? "
                    + "OR LOWER(COALESCE(a.email, '')) LIKE ? "
                    + "OR LOWER(COALESCE(qr.barstring, '')) LIKE ?)");
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }

        if (!(privileged && includeAllForPrivileged)) {
            if (scopedVenueId == null || scopedVenueId <= 0) {
                return new ArrayList<>();
            }
            sql.append(" AND v.venueID = ?");
            params.add(scopedVenueId);
        }

        sql.append(" ORDER BY t.ticketID DESC, e.eventID DESC");
        return runListQuery(sql.toString(), params);
    }

    public void createAdmin(int adminId, String firstName, String lastName, String email, String password, int eventId) throws SQLException {
        if (!isRoleAssignmentValid("ADMIN", eventId, null, null, null, true)) {
            throw new SQLException("Invalid admin assignment for event");
        }
        if (!hasCampusAccessForRoleMutation(adminId, "ADMIN", null, eventId, null, null)) {
            throw new SQLException("CampusScopeDenied");
        }
        String sql = "INSERT INTO admin(firstname, lastname, email, password, eventID) VALUES(?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, PasswordUtil.hashPassword(password));
            ps.setInt(5, eventId);
            ps.executeUpdate();
            logAudit(conn, adminId, "CREATE_ADMIN", "admin", email, "Created admin account");
        }
    }

    public void createGuard(int adminId, String firstName, String lastName, String email, String password, int eventId, int venueId) throws SQLException {
        if (!isRoleAssignmentValid("VENUE_GUARD", eventId, venueId, null, null, true)) {
            throw new SQLException("Invalid guard assignment for event or venue");
        }
        if (!hasCampusAccessForRoleMutation(adminId, "VENUE_GUARD", null, eventId, venueId, null)) {
            throw new SQLException("CampusScopeDenied");
        }
        String insertGuard = "INSERT INTO venue_guard(firstname, lastname, email, password, eventID, venueID, QRcodeID) VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int qrId = createQrRecord(conn, "VG|" + email.toLowerCase() + "|" + System.currentTimeMillis());
                try (PreparedStatement ps = conn.prepareStatement(insertGuard)) {
                    ps.setString(1, firstName);
                    ps.setString(2, lastName);
                    ps.setString(3, email);
                    ps.setString(4, PasswordUtil.hashPassword(password));
                    ps.setInt(5, eventId);
                    ps.setInt(6, venueId);
                    ps.setInt(7, qrId);
                    ps.executeUpdate();
                }
                logAudit(conn, adminId, "CREATE_GUARD", "venue_guard", email, "Created guard account");
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void createManager(int adminId, String firstName, String lastName, String email, String password, int venueGuardId) throws SQLException {
        if (!isRoleAssignmentValid("EVENT_MANAGER", null, null, venueGuardId, null, true)) {
            throw new SQLException("Invalid manager assignment for venue guard");
        }
        if (!hasCampusAccessForRoleMutation(adminId, "EVENT_MANAGER", null, null, null, venueGuardId)) {
            throw new SQLException("CampusScopeDenied");
        }
        String sql = "INSERT INTO event_manager(firstname, lastname, email, password, venueGuardID) VALUES(?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, PasswordUtil.hashPassword(password));
            ps.setInt(5, venueGuardId);
            ps.executeUpdate();
            logAudit(conn, adminId, "CREATE_MANAGER", "event_manager", email, "Created event manager account");
        }
    }

    public void createPresenter(int adminId, String firstName, String lastName, String email, String password,
            String tertiaryInstitution, int eventId, int venueId) throws SQLException {
        if (!isRoleAssignmentValid("TERTIARY_PRESENTER", eventId, venueId, null, tertiaryInstitution, true)) {
            throw new SQLException("Invalid presenter assignment for institution, event, or venue");
        }
        if (!hasCampusAccessForRoleMutation(adminId, "TERTIARY_PRESENTER", null, eventId, venueId, null)) {
            throw new SQLException("CampusScopeDenied");
        }
        String sql = "INSERT INTO tertiary_presenter(firstname, lastname, email, password, tertiaryInstitution, eventID, venueID) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, PasswordUtil.hashPassword(password));
            ps.setString(5, tertiaryInstitution);
            ps.setInt(6, eventId);
            ps.setInt(7, venueId);
            ps.executeUpdate();
            logAudit(conn, adminId, "CREATE_PRESENTER", "tertiary_presenter", email, "Created presenter account");
        }
    }

    public boolean updateUserByRole(int adminId, String role, int id, String firstName, String lastName, String email,
            Integer eventId, Integer venueId, Integer guardId, String tertiaryInstitution) throws SQLException {
        String table = tableForRole(role);
        String idCol = idColumnForRole(role);
        if (table == null || idCol == null) {
            return false;
        }
        if (!isRoleAssignmentValid(role, eventId, venueId, guardId, tertiaryInstitution, false)) {
            throw new SQLException("Invalid assignment values for selected role");
        }
        if (!hasCampusAccessForRoleMutation(adminId, role, id, eventId, venueId, guardId)) {
            throw new SQLException("CampusScopeDenied");
        }

        StringBuilder sql = new StringBuilder("UPDATE " + table + " SET firstname=?, lastname=?, email=?");
        List<Object> params = new ArrayList<>();
        params.add(firstName);
        params.add(lastName);
        params.add(email);

        if ("ADMIN".equals(role) && eventId != null && eventId > 0) {
            sql.append(", eventID=?");
            params.add(eventId);
        }
        if ("VENUE_GUARD".equals(role)) {
            if (eventId != null && eventId > 0) {
                sql.append(", eventID=?");
                params.add(eventId);
            }
            if (venueId != null && venueId > 0) {
                sql.append(", venueID=?");
                params.add(venueId);
            }
        }
        if ("EVENT_MANAGER".equals(role) && guardId != null && guardId > 0) {
            sql.append(", venueGuardID=?");
            params.add(guardId);
        }
        if ("TERTIARY_PRESENTER".equals(role)) {
            if (tertiaryInstitution != null && !tertiaryInstitution.trim().isEmpty()) {
                sql.append(", tertiaryInstitution=?");
                params.add(tertiaryInstitution.trim());
            }
            if (eventId != null && eventId > 0) {
                sql.append(", eventID=?");
                params.add(eventId);
            }
            if (venueId != null && venueId > 0) {
                sql.append(", venueID=?");
                params.add(venueId);
            }
        }

        sql.append(" WHERE ").append(idCol).append("=?");
        params.add(id);

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logAudit(conn, adminId, "UPDATE_USER", table, String.valueOf(id), "Updated profile fields for role " + role);
                return true;
            }
            return false;
        }
    }

    public boolean deleteByRole(int adminId, String role, int id) throws SQLException {
        if (!isPrivilegedAdmin(adminId)) {
            throw new SQLException("PrivilegedRequired");
        }
        String table = tableForRole(role);
        String idCol = idColumnForRole(role);
        if (table == null || idCol == null) {
            return false;
        }

        String sql = "DELETE FROM " + table + " WHERE " + idCol + " = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logAudit(conn, adminId, "DELETE_USER", table, String.valueOf(id), "Deleted role " + role + " account");
                return true;
            }
            return false;
        }
    }

    public boolean setAccountLock(int adminId, String role, int userId, boolean locked) throws SQLException {
        if (!locked && !isPrivilegedAdmin(adminId)) {
            throw new SQLException("PrivilegedRequired");
        }
        if (!hasCampusAccessForRoleMutation(adminId, role, userId, null, null, null)) {
            throw new SQLException("CampusScopeDenied");
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean hasUpdatedByAdminId = columnExists(conn, "account_control", "updatedByAdminID");
            boolean hasUpdatedAt = columnExists(conn, "account_control", "updatedAt");
            boolean hasForceReset = columnExists(conn, "account_control", "forceReset");
            boolean exists = controlRowExists(conn, role, userId);
            int affected;
            if (exists) {
                StringBuilder updateSql = new StringBuilder("UPDATE account_control SET isLocked=?");
                if (hasUpdatedByAdminId) {
                    updateSql.append(", updatedByAdminID=?");
                }
                if (hasUpdatedAt) {
                    updateSql.append(", updatedAt=CURRENT_TIMESTAMP");
                }
                updateSql.append(" WHERE roleName=? AND userID=?");

                try (PreparedStatement ps = conn.prepareStatement(updateSql.toString())) {
                    int idx = 1;
                    ps.setBoolean(idx++, locked);
                    if (hasUpdatedByAdminId) {
                        ps.setInt(idx++, adminId);
                    }
                    ps.setString(idx++, role);
                    ps.setInt(idx, userId);
                    affected = ps.executeUpdate();
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
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    int idx = 1;
                    ps.setString(idx++, role);
                    ps.setInt(idx++, userId);
                    ps.setBoolean(idx++, locked);
                    if (hasForceReset) {
                        ps.setBoolean(idx++, false);
                    }
                    if (hasUpdatedByAdminId) {
                        ps.setInt(idx++, adminId);
                    }
                    affected = ps.executeUpdate();
                }
            }
            logAudit(conn, adminId, locked ? "LOCK_ACCOUNT" : "UNLOCK_ACCOUNT", "account_control", role + ":" + userId,
                    (locked ? "Locked " : "Unlocked ") + "account");
            return affected > 0;
        }
    }

    public boolean resetPassword(int adminId, String role, int userId, String temporaryPassword) throws SQLException {
        if (!hasCampusAccessForRoleMutation(adminId, role, userId, null, null, null)) {
            throw new SQLException("CampusScopeDenied");
        }
        String table = tableForRole(role);
        String idCol = idColumnForRole(role);
        if (table == null || idCol == null) {
            return false;
        }

        String sql = "UPDATE " + table + " SET password=? WHERE " + idCol + "=?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hashPassword(temporaryPassword));
            ps.setInt(2, userId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                upsertForceReset(conn, role, userId, adminId, true);
                logAudit(conn, adminId, "RESET_PASSWORD", table, String.valueOf(userId), "Temporary password assigned");
                return true;
            }
            return false;
        }
    }

    public boolean reassignRole(int adminId, String sourceRole, int sourceId, String targetRole,
            Integer eventId, Integer venueId, Integer guardId, String tertiaryInstitution) throws SQLException {
        String sourceTable = tableForRole(sourceRole);
        String sourceIdCol = idColumnForRole(sourceRole);
        String targetTable = tableForRole(targetRole);
        if (sourceTable == null || sourceIdCol == null || targetTable == null || sourceRole.equals(targetRole)) {
            return false;
        }
        if (!hasCampusAccessForRoleMutation(adminId, sourceRole, sourceId, null, null, null)) {
            throw new SQLException("CampusScopeDenied");
        }
        if (!hasCampusAccessForRoleMutation(adminId, targetRole, null, eventId, venueId, guardId)) {
            throw new SQLException("CampusScopeDenied");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Map<String, Object> profile = fetchIdentity(conn, sourceTable, sourceIdCol, sourceId);
                if (profile == null) {
                    conn.rollback();
                    return false;
                }

                String firstName = stringValue(profile.get("firstname"));
                String lastName = stringValue(profile.get("lastname"));
                String email = stringValue(profile.get("email"));
                String passwordHash = stringValue(profile.get("password"));

                insertRoleAccount(conn, targetRole, firstName, lastName, email, passwordHash,
                        eventId, venueId, guardId, tertiaryInstitution);

                try (PreparedStatement deletePs = conn.prepareStatement("DELETE FROM " + sourceTable + " WHERE " + sourceIdCol + " = ?")) {
                    deletePs.setInt(1, sourceId);
                    deletePs.executeUpdate();
                }

                upsertForceReset(conn, targetRole, resolveUserIdByEmail(conn, targetTable, idColumnForRole(targetRole), email), adminId, true);
                logAudit(conn, adminId, "REASSIGN_ROLE", sourceTable + "->" + targetTable, String.valueOf(sourceId),
                        "Moved account to role " + targetRole);
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public int purgeOldScanLogs(int adminId, int days) throws SQLException {
        if (days < 1) {
            days = 1;
        }
        String sql = "DELETE FROM scan_log WHERE scannedAt < (CURRENT_TIMESTAMP - ? DAYS)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            int removed = ps.executeUpdate();
            logAudit(conn, adminId, "PURGE_SCAN_LOGS", "scan_log", "-", "Removed rows: " + removed + ", days=" + days);
            return removed;
        }
    }

    public boolean safeDeleteRow(int adminId, String tableName, int rowId) throws SQLException {
        String table = normalizeTableName(tableName);
        String pkCol = SAFE_DELETE_PK.get(table);
        if (pkCol == null) {
            return false;
        }

        String sql = "DELETE FROM " + table + " WHERE " + pkCol + " = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rowId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logAudit(conn, adminId, "SAFE_DELETE_ROW", table, String.valueOf(rowId), "Deleted by safe row tool");
                return true;
            }
            return false;
        }
    }

    public Map<String, Object> getTablePreview(String requestedTable) throws SQLException {
        String table = normalizeTableName(requestedTable);
        if (!PREVIEW_TABLES.contains(table)) {
            table = "scan_log";
        }

        Map<String, Object> model = new HashMap<>();
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM " + table + " FETCH FIRST 25 ROWS ONLY")) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                columns.add(md.getColumnLabel(i));
            }
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String col : columns) {
                    row.put(col, rs.getObject(col));
                }
                rows.add(row);
            }
        }

        model.put("table", table);
        model.put("columns", columns);
        model.put("rows", rows);
        model.put("allowedTables", new ArrayList<>(PREVIEW_TABLES));
        model.put("safeDeleteTables", new TreeSet<>(SAFE_DELETE_PK.keySet()));
        return model;
    }

    public List<Map<String, Object>> getFaultSignals() throws SQLException {
        String sql = "SELECT scanLogID, result, reason, scannedAt FROM scan_log WHERE result = 'INVALID' ORDER BY scannedAt DESC FETCH FIRST 100 ROWS ONLY";
        return runListQuery(sql, Collections.emptyList());
    }

    public List<Map<String, Object>> getTicketIntelligence() throws SQLException {
        String sql = "SELECT t.ticketID, t.name AS ticketNumber, t.price, "
                + "a.attendeeID, a.firstname AS attendeeFirst, a.lastname AS attendeeLast, a.email AS attendeeEmail, "
                + "e.eventID, e.name AS eventName, v.name AS venueName, v.address AS venueAddress "
                + "FROM ticket t "
                + "JOIN attendee_has_ticket aht ON aht.ticketID = t.ticketID "
                + "JOIN attendee a ON a.attendeeID = aht.attendeeID "
                + "LEFT JOIN event_has_ticket eht ON eht.ticketID = t.ticketID "
                + "LEFT JOIN event e ON e.eventID = eht.eventID "
                + "LEFT JOIN venue v ON v.venueID = e.venueID "
                + "ORDER BY v.name ASC, e.name ASC, t.ticketID DESC FETCH FIRST 300 ROWS ONLY";

        List<Map<String, Object>> rows = runListQuery(sql, Collections.emptyList());
        Map<Integer, Map<String, Object>> latestScanByTicket = new HashMap<>();
        List<Map<String, Object>> scans = runListQuery(
                "SELECT s.ticketID, s.result, s.reason, s.scannedAt, s.venueGuardID, g.firstname AS guardFirst, g.lastname AS guardLast, g.email AS guardEmail "
                + "FROM scan_log s "
                + "LEFT JOIN venue_guard g ON g.venueGuardID = s.venueGuardID "
                + "WHERE s.ticketID IS NOT NULL "
                + "ORDER BY s.scannedAt DESC", Collections.emptyList());
        for (Map<String, Object> scan : scans) {
            Object ticketIdObj = scan.get("ticketID");
            if (!(ticketIdObj instanceof Number)) {
                continue;
            }
            Integer ticketId = ((Number) ticketIdObj).intValue();
            if (!latestScanByTicket.containsKey(ticketId)) {
                latestScanByTicket.put(ticketId, scan);
            }
        }

        for (Map<String, Object> row : rows) {
            Object ticketIdObj = row.get("ticketID");
            Integer ticketId = ticketIdObj instanceof Number ? ((Number) ticketIdObj).intValue() : null;
            Map<String, Object> scan = ticketId == null ? null : latestScanByTicket.get(ticketId);
            if (scan == null) {
                row.put("scanResult", "NOT_SCANNED");
                row.put("scanReason", "-");
                row.put("scannedAt", null);
                row.put("guardName", "-");
            } else {
                row.put("scanResult", scan.get("result"));
                row.put("scanReason", scan.get("reason"));
                row.put("scannedAt", scan.get("scannedAt"));
                String guardName = stringValue(scan.get("guardFirst")) + " " + stringValue(scan.get("guardLast"));
                row.put("guardName", guardName.trim().isEmpty() ? "-" : guardName.trim());
                row.put("guardEmail", scan.get("guardEmail"));
            }
        }
        return rows;
    }

    public List<Map<String, Object>> getTicketIntelligenceForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return getTicketIntelligence();
        }
        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (campusVenueId == null || campusVenueId <= 0) {
            return new ArrayList<>();
        }

        String sql = "SELECT t.ticketID, t.name AS ticketNumber, t.price, "
                + "a.attendeeID, a.firstname AS attendeeFirst, a.lastname AS attendeeLast, a.email AS attendeeEmail, "
                + "e.eventID, e.name AS eventName, v.name AS venueName, v.address AS venueAddress "
                + "FROM ticket t "
                + "JOIN attendee_has_ticket aht ON aht.ticketID = t.ticketID "
                + "JOIN attendee a ON a.attendeeID = aht.attendeeID "
                + "JOIN event_has_ticket eht ON eht.ticketID = t.ticketID "
                + "JOIN event e ON e.eventID = eht.eventID "
                + "JOIN venue v ON v.venueID = e.venueID "
                + "WHERE v.venueID = ? "
                + "ORDER BY v.name ASC, e.name ASC, t.ticketID DESC FETCH FIRST 300 ROWS ONLY";

        List<Map<String, Object>> rows = runListQuery(sql, Arrays.<Object>asList(campusVenueId));
        Map<Integer, Map<String, Object>> latestScanByTicket = new HashMap<>();
        List<Map<String, Object>> scans = runListQuery(
                "SELECT s.ticketID, s.result, s.reason, s.scannedAt, s.venueGuardID, g.firstname AS guardFirst, g.lastname AS guardLast, g.email AS guardEmail "
                + "FROM scan_log s "
                + "LEFT JOIN venue_guard g ON g.venueGuardID = s.venueGuardID "
                + "WHERE s.ticketID IS NOT NULL "
                + "ORDER BY s.scannedAt DESC", Collections.emptyList());
        for (Map<String, Object> scan : scans) {
            Object ticketIdObj = scan.get("ticketID");
            if (!(ticketIdObj instanceof Number)) {
                continue;
            }
            Integer ticketId = ((Number) ticketIdObj).intValue();
            if (!latestScanByTicket.containsKey(ticketId)) {
                latestScanByTicket.put(ticketId, scan);
            }
        }

        for (Map<String, Object> row : rows) {
            Object ticketIdObj = row.get("ticketID");
            Integer ticketId = ticketIdObj instanceof Number ? ((Number) ticketIdObj).intValue() : null;
            Map<String, Object> scan = ticketId == null ? null : latestScanByTicket.get(ticketId);
            if (scan == null) {
                row.put("scanResult", "NOT_SCANNED");
                row.put("scanReason", "-");
                row.put("scannedAt", null);
                row.put("guardName", "-");
            } else {
                row.put("scanResult", scan.get("result"));
                row.put("scanReason", scan.get("reason"));
                row.put("scannedAt", scan.get("scannedAt"));
                String guardName = stringValue(scan.get("guardFirst")) + " " + stringValue(scan.get("guardLast"));
                row.put("guardName", guardName.trim().isEmpty() ? "-" : guardName.trim());
                row.put("guardEmail", scan.get("guardEmail"));
            }
        }
        return rows;
    }

    public List<Map<String, Object>> getCampusRevenueReport() throws SQLException {
        String sql = "SELECT v.venueID, v.name AS campusName, v.address AS campusAddress, "
                + "COUNT(DISTINCT t.ticketID) AS ticketsSold, "
                + "COALESCE(SUM(t.price), 0) AS revenue "
                + "FROM venue v "
                + "LEFT JOIN event e ON e.venueID = v.venueID "
                + "LEFT JOIN event_has_ticket eht ON eht.eventID = e.eventID "
                + "LEFT JOIN ticket t ON t.ticketID = eht.ticketID "
                + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = t.ticketID "
                + "GROUP BY v.venueID, v.name, v.address "
                + "ORDER BY revenue DESC, ticketsSold DESC";
        return runListQuery(sql, Collections.emptyList());
    }

    public List<Map<String, Object>> getCampusRevenueReportForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return getCampusRevenueReport();
        }
        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (campusVenueId == null || campusVenueId <= 0) {
            return new ArrayList<>();
        }
        String sql = "SELECT v.venueID, v.name AS campusName, v.address AS campusAddress, "
                + "COUNT(DISTINCT t.ticketID) AS ticketsSold, "
                + "COALESCE(SUM(t.price), 0) AS revenue "
                + "FROM venue v "
                + "LEFT JOIN event e ON e.venueID = v.venueID "
                + "LEFT JOIN event_has_ticket eht ON eht.eventID = e.eventID "
                + "LEFT JOIN ticket t ON t.ticketID = eht.ticketID "
                + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = t.ticketID "
                + "WHERE v.venueID = ? "
                + "GROUP BY v.venueID, v.name, v.address "
                + "ORDER BY revenue DESC, ticketsSold DESC";
        return runListQuery(sql, Arrays.<Object>asList(campusVenueId));
    }

    public List<Map<String, Object>> getFinancialReconciliationForScope(int adminId) throws SQLException {
        boolean privileged = isPrivilegedAdmin(adminId);
        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (!privileged && (campusVenueId == null || campusVenueId <= 0)) {
            return new ArrayList<>();
        }

        StringBuilder sql = new StringBuilder(
            "SELECT v.venueID, v.name AS campusName, "
            + "COALESCE(soldAgg.soldTickets, 0) AS soldTickets, "
            + "COALESCE(soldAgg.recordedRevenue, 0) AS recordedRevenue, "
            + "COALESCE(validAgg.validatedTickets, 0) AS validatedTickets, "
            + "COALESCE(validAgg.validatedRevenue, 0) AS validatedRevenue "
            + "FROM venue v "
            + "LEFT JOIN ("
            + "  SELECT e.venueID, COUNT(DISTINCT aht.ticketID) AS soldTickets, COALESCE(SUM(t.price), 0) AS recordedRevenue "
            + "  FROM event e "
            + "  LEFT JOIN event_has_ticket eht ON eht.eventID = e.eventID "
            + "  LEFT JOIN attendee_has_ticket aht ON aht.ticketID = eht.ticketID "
            + "  LEFT JOIN ticket t ON t.ticketID = aht.ticketID "
            + "  GROUP BY e.venueID"
            + ") soldAgg ON soldAgg.venueID = v.venueID "
            + "LEFT JOIN ("
            + "  SELECT e3.venueID, COUNT(DISTINCT s.ticketID) AS validatedTickets, COALESCE(SUM(t2.price), 0) AS validatedRevenue "
            + "  FROM scan_log s "
            + "  JOIN event_has_ticket eht3 ON eht3.ticketID = s.ticketID "
            + "  JOIN event e3 ON e3.eventID = eht3.eventID "
            + "  LEFT JOIN ticket t2 ON t2.ticketID = s.ticketID "
            + "  WHERE s.result = 'VALID' "
            + "  GROUP BY e3.venueID"
            + ") validAgg ON validAgg.venueID = v.venueID "
            + "WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (!privileged) {
            sql.append(" AND v.venueID = ?");
            params.add(campusVenueId);
        }
        sql.append(" ORDER BY v.name ASC");

        List<Map<String, Object>> rows = runListQuery(sql.toString(), params);
        for (Map<String, Object> row : rows) {
            int sold = toInt(row.get("soldTickets"));
            int validated = toInt(row.get("validatedTickets"));
            double recorded = toDouble(row.get("recordedRevenue"));
            double validatedRevenue = toDouble(row.get("validatedRevenue"));
            row.put("ticketDelta", sold - validated);
            row.put("revenueDelta", round2(recorded - validatedRevenue));
            row.put("status", (sold - validated == 0 && Math.abs(recorded - validatedRevenue) < 0.01) ? "BALANCED" : "REVIEW_REQUIRED");
        }
        return rows;
    }

    public int countFinancialReconciliationRowsForScope(int adminId) throws SQLException {
        boolean privileged = isPrivilegedAdmin(adminId);
        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (!privileged && (campusVenueId == null || campusVenueId <= 0)) {
            return 0;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (privileged) {
                return queryInt(conn, "SELECT COUNT(*) FROM venue");
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM venue WHERE venueID = ?")) {
                ps.setInt(1, campusVenueId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        }
    }

    public List<Map<String, Object>> getFinancialReconciliationForScope(int adminId, int page, int pageSize) throws SQLException {
        int safePage = page <= 0 ? 1 : page;
        int safePageSize = pageSize <= 0 ? 10 : pageSize;
        int offset = (safePage - 1) * safePageSize;
        boolean privileged = isPrivilegedAdmin(adminId);
        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (!privileged && (campusVenueId == null || campusVenueId <= 0)) {
            return new ArrayList<>();
        }

        StringBuilder sql = new StringBuilder(
            "SELECT v.venueID, v.name AS campusName, "
            + "COALESCE(soldAgg.soldTickets, 0) AS soldTickets, "
            + "COALESCE(soldAgg.recordedRevenue, 0) AS recordedRevenue, "
            + "COALESCE(validAgg.validatedTickets, 0) AS validatedTickets, "
            + "COALESCE(validAgg.validatedRevenue, 0) AS validatedRevenue "
            + "FROM venue v "
            + "LEFT JOIN ("
            + "  SELECT e.venueID, COUNT(DISTINCT aht.ticketID) AS soldTickets, COALESCE(SUM(t.price), 0) AS recordedRevenue "
            + "  FROM event e "
            + "  LEFT JOIN event_has_ticket eht ON eht.eventID = e.eventID "
            + "  LEFT JOIN attendee_has_ticket aht ON aht.ticketID = eht.ticketID "
            + "  LEFT JOIN ticket t ON t.ticketID = aht.ticketID "
            + "  GROUP BY e.venueID"
            + ") soldAgg ON soldAgg.venueID = v.venueID "
            + "LEFT JOIN ("
            + "  SELECT e3.venueID, COUNT(DISTINCT s.ticketID) AS validatedTickets, COALESCE(SUM(t2.price), 0) AS validatedRevenue "
            + "  FROM scan_log s "
            + "  JOIN event_has_ticket eht3 ON eht3.ticketID = s.ticketID "
            + "  JOIN event e3 ON e3.eventID = eht3.eventID "
            + "  LEFT JOIN ticket t2 ON t2.ticketID = s.ticketID "
            + "  WHERE s.result = 'VALID' "
            + "  GROUP BY e3.venueID"
            + ") validAgg ON validAgg.venueID = v.venueID "
            + "WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (!privileged) {
            sql.append(" AND v.venueID = ?");
            params.add(campusVenueId);
        }
        sql.append(" ORDER BY v.name ASC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(safePageSize);

        List<Map<String, Object>> rows = runListQuery(sql.toString(), params);
        for (Map<String, Object> row : rows) {
            int sold = toInt(row.get("soldTickets"));
            int validated = toInt(row.get("validatedTickets"));
            double recorded = toDouble(row.get("recordedRevenue"));
            double validatedRevenue = toDouble(row.get("validatedRevenue"));
            row.put("ticketDelta", sold - validated);
            row.put("revenueDelta", round2(recorded - validatedRevenue));
            row.put("status", (sold - validated == 0 && Math.abs(recorded - validatedRevenue) < 0.01) ? "BALANCED" : "REVIEW_REQUIRED");
        }
        return rows;
    }

    public List<Map<String, Object>> getCampusOwnershipReport() throws SQLException {
        List<Map<String, Object>> campuses = runListQuery(
                "SELECT v.venueID, v.name AS campusName, v.address AS campusAddress FROM venue v ORDER BY v.name ASC",
                Collections.emptyList());

        for (Map<String, Object> campus : campuses) {
            Object venueIdObj = campus.get("venueID");
            if (venueIdObj == null) {
                venueIdObj = campus.get("VENUEID");
            }
            if (!(venueIdObj instanceof Number)) {
                campus.put("admins", "-");
                campus.put("managers", "-");
                campus.put("databaseOwner", "Tickify Operational Database");
                continue;
            }
            int venueId = ((Number) venueIdObj).intValue();
            List<Map<String, Object>> adminRows = runListQuery(
                    "SELECT DISTINCT a.firstname, a.lastname, a.email "
                    + "FROM admin a "
                    + "JOIN event e ON e.eventID = a.eventID "
                    + "WHERE e.venueID = ? "
                    + "ORDER BY a.firstname, a.lastname", Arrays.<Object>asList(venueId));
            List<Map<String, Object>> managerRows = runListQuery(
                    "SELECT DISTINCT m.firstname, m.lastname, m.email "
                    + "FROM event_manager m "
                    + "JOIN venue_guard g ON g.venueGuardID = m.venueGuardID "
                    + "WHERE g.venueID = ? "
                    + "ORDER BY m.firstname, m.lastname", Arrays.<Object>asList(venueId));
            List<Map<String, Object>> studentRows = runListQuery(
                    "SELECT COUNT(*) AS studentCount "
                    + "FROM attendee a "
                    + "JOIN attendee_has_ticket aht ON aht.attendeeID = a.attendeeID "
                    + "JOIN event_has_ticket eht ON eht.ticketID = aht.ticketID "
                    + "JOIN event e ON e.eventID = eht.eventID "
                    + "WHERE e.venueID = ?", Arrays.<Object>asList(venueId));

            int studentCount = 0;
            if (!studentRows.isEmpty() && studentRows.get(0).get("studentCount") instanceof Number) {
                studentCount = ((Number) studentRows.get(0).get("studentCount")).intValue();
            }

            campus.put("admins", joinNames(adminRows));
            campus.put("managers", joinNames(managerRows));
            campus.put("studentCount", studentCount);
            campus.put("databaseOwner", "Tickify Operational Database");
        }
        return campuses;
    }

    public List<Map<String, Object>> getCampusOwnershipReportForScope(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return getCampusOwnershipReport();
        }
        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (campusVenueId == null || campusVenueId <= 0) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> campuses = runListQuery(
                "SELECT v.venueID, v.name AS campusName, v.address AS campusAddress FROM venue v WHERE v.venueID = ? ORDER BY v.name ASC",
                Arrays.<Object>asList(campusVenueId));

        for (Map<String, Object> campus : campuses) {
            int venueId = ((Number) campus.get("venueID")).intValue();
            List<Map<String, Object>> adminRows = runListQuery(
                    "SELECT DISTINCT a.firstname, a.lastname, a.email "
                    + "FROM admin a "
                    + "JOIN event e ON e.eventID = a.eventID "
                    + "WHERE e.venueID = ? "
                    + "ORDER BY a.firstname, a.lastname", Arrays.<Object>asList(venueId));
            List<Map<String, Object>> managerRows = runListQuery(
                    "SELECT DISTINCT m.firstname, m.lastname, m.email "
                    + "FROM event_manager m "
                    + "JOIN venue_guard g ON g.venueGuardID = m.venueGuardID "
                    + "WHERE g.venueID = ? "
                    + "ORDER BY m.firstname, m.lastname", Arrays.<Object>asList(venueId));
            List<Map<String, Object>> studentRows = runListQuery(
                    "SELECT COUNT(*) AS studentCount "
                    + "FROM attendee a "
                    + "JOIN attendee_has_ticket aht ON aht.attendeeID = a.attendeeID "
                    + "JOIN event_has_ticket eht ON eht.ticketID = aht.ticketID "
                    + "JOIN event e ON e.eventID = eht.eventID "
                    + "WHERE e.venueID = ?", Arrays.<Object>asList(venueId));

            int studentCount = 0;
            if (!studentRows.isEmpty() && studentRows.get(0).get("studentCount") instanceof Number) {
                studentCount = ((Number) studentRows.get(0).get("studentCount")).intValue();
            }

            campus.put("admins", joinNames(adminRows));
            campus.put("managers", joinNames(managerRows));
            campus.put("studentCount", studentCount);
            campus.put("databaseOwner", "Tickify Operational Database");
        }
        return campuses;
    }

    public List<Map<String, Object>> getLockedAccountsForUnblockQueue(int adminId) throws SQLException {
        String sql = "SELECT ac.roleName, ac.userID, ac.updatedAt, ac.updatedByAdminID, "
                + "a.firstname, a.lastname, a.email, "
                + "(SELECT MIN(v2.name) "
                + " FROM attendee_has_ticket aht2 "
                + " JOIN event_has_ticket eht2 ON eht2.ticketID = aht2.ticketID "
                + " JOIN event e2 ON e2.eventID = eht2.eventID "
                + " JOIN venue v2 ON v2.venueID = e2.venueID "
                + " WHERE aht2.attendeeID = a.attendeeID) AS campusName "
                + "FROM account_control ac "
                + "JOIN attendee a ON ac.roleName = 'ATTENDEE' AND ac.userID = a.attendeeID "
                + "WHERE ac.isLocked = TRUE "
                + "UNION ALL "
                + "SELECT ac.roleName, ac.userID, ac.updatedAt, ac.updatedByAdminID, "
                + "p.firstname, p.lastname, p.email, v.name AS campusName "
                + "FROM account_control ac "
                + "JOIN tertiary_presenter p ON ac.roleName = 'TERTIARY_PRESENTER' AND ac.userID = p.tertiaryPresenterID "
                + "LEFT JOIN venue v ON v.venueID = p.venueID "
                + "WHERE ac.isLocked = TRUE "
                + "UNION ALL "
                + "SELECT ac.roleName, ac.userID, ac.updatedAt, ac.updatedByAdminID, "
                + "m.firstname, m.lastname, m.email, v.name AS campusName "
                + "FROM account_control ac "
                + "JOIN event_manager m ON ac.roleName = 'EVENT_MANAGER' AND ac.userID = m.eventManagerID "
                + "LEFT JOIN venue_guard g ON g.venueGuardID = m.venueGuardID "
                + "LEFT JOIN venue v ON v.venueID = g.venueID "
                + "WHERE ac.isLocked = TRUE "
                + "UNION ALL "
                + "SELECT ac.roleName, ac.userID, ac.updatedAt, ac.updatedByAdminID, "
                + "g.firstname, g.lastname, g.email, v.name AS campusName "
                + "FROM account_control ac "
                + "JOIN venue_guard g ON ac.roleName = 'VENUE_GUARD' AND ac.userID = g.venueGuardID "
                + "LEFT JOIN venue v ON v.venueID = g.venueID "
                + "WHERE ac.isLocked = TRUE "
                + "ORDER BY campusName ASC, updatedAt DESC";

        List<Map<String, Object>> rows = runListQuery(sql, Collections.emptyList());
        if (isPrivilegedAdmin(adminId)) {
            return rows;
        }

        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (campusVenueId == null || campusVenueId <= 0) {
            return new ArrayList<>();
        }
        String campusName = getVenueNameById(campusVenueId);
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String rowCampus = stringValue(row.get("campusName"));
            if (!rowCampus.isEmpty() && rowCampus.equalsIgnoreCase(campusName)) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    public List<Map<String, Object>> getAuthLockoutAlerts(int adminId) throws SQLException {
        List<Map<String, Object>> rows = runListQuery(
                "SELECT l.adminAuditLogID, l.adminID, a.firstname, a.lastname, l.actionType, l.targetTable, l.targetID, l.details, l.createdAt "
                + "FROM admin_audit_log l "
                + "JOIN admin a ON a.adminID = l.adminID "
                + "WHERE l.actionType = 'AUTH_LOCKOUT_ALERT' "
                + "ORDER BY l.createdAt DESC FETCH FIRST 200 ROWS ONLY", Collections.emptyList());
        if (isPrivilegedAdmin(adminId)) {
            return rows;
        }
        return new ArrayList<>();
    }

    private String getVenueNameById(int venueId) throws SQLException {
        List<Map<String, Object>> rows = runListQuery("SELECT name FROM venue WHERE venueID = ?", Arrays.<Object>asList(venueId));
        if (rows.isEmpty()) {
            return "";
        }
        return stringValue(rows.get(0).get("name"));
    }

    public List<Map<String, Object>> getAuditLogs(Integer adminId, String actionType, String fromDate, String toDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT l.adminAuditLogID, l.adminID, a.firstname, a.lastname, l.actionType, l.targetTable, l.targetID, l.details, l.createdAt "
                + "FROM admin_audit_log l JOIN admin a ON a.adminID = l.adminID WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (adminId != null && adminId > 0) {
            sql.append(" AND l.adminID = ?");
            params.add(adminId);
        }
        if (actionType != null && !actionType.trim().isEmpty()) {
            sql.append(" AND UPPER(l.actionType) = ?");
            params.add(actionType.trim().toUpperCase());
        }
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            sql.append(" AND l.createdAt >= TIMESTAMP(?)");
            params.add(fromDate.trim() + " 00:00:00");
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            sql.append(" AND l.createdAt <= TIMESTAMP(?)");
            params.add(toDate.trim() + " 23:59:59");
        }

        sql.append(" ORDER BY l.createdAt DESC FETCH FIRST 200 ROWS ONLY");
        return runListQuery(sql.toString(), params);
    }

    public List<Map<String, Object>> getAdminActors() throws SQLException {
        String sql = "SELECT adminID, firstname, lastname FROM admin ORDER BY firstname, lastname";
        return runListQuery(sql, Collections.emptyList());
    }

    public boolean createDeletionRequest(int requestedByAdminId, String targetRole, int targetUserId, String reason) throws SQLException {
        if (requestedByAdminId <= 0 || targetUserId <= 0 || targetRole == null || targetRole.trim().isEmpty()) {
            return false;
        }
        if (!hasCampusAccessForRoleMutation(requestedByAdminId, targetRole, targetUserId, null, null, null)) {
            throw new SQLException("CampusScopeDenied");
        }
        String sql = "INSERT INTO delete_request(requestedByAdminID, targetRole, targetUserID, reason, status, requestedAt) "
                + "VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestedByAdminId);
            ps.setString(2, targetRole.trim().toUpperCase());
            ps.setInt(3, targetUserId);
            ps.setString(4, reason == null ? "Requested from role console" : reason.trim());
            ps.setString(5, "PENDING");
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logAudit(conn, requestedByAdminId, "DELETE_REQUESTED", "delete_request",
                        targetRole + ":" + targetUserId, "Requested delete approval from privileged admin");
                return true;
            }
            return false;
        }
    }

    public List<Map<String, Object>> getDeletionRequests(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return runListQuery("SELECT d.deleteRequestID, d.requestedByAdminID, d.targetRole, d.targetUserID, d.reason, d.status, d.requestedAt, "
                    + "a.firstname AS requestedByFirst, a.lastname AS requestedByLast "
                    + "FROM delete_request d JOIN admin a ON a.adminID = d.requestedByAdminID "
                    + "ORDER BY d.status ASC, d.requestedAt DESC", Collections.emptyList());
        }
        return runListQuery("SELECT d.deleteRequestID, d.requestedByAdminID, d.targetRole, d.targetUserID, d.reason, d.status, d.requestedAt "
                + "FROM delete_request d WHERE d.requestedByAdminID = ? ORDER BY d.requestedAt DESC",
                Arrays.<Object>asList(adminId));
    }

    public List<Map<String, Object>> getDeletionRequestsForAlerts(int adminId) throws SQLException {
        if (isPrivilegedAdmin(adminId)) {
            return runListQuery("SELECT d.deleteRequestID, d.requestedByAdminID, d.targetRole, d.targetUserID, d.reason, d.status, d.requestedAt, "
                    + "a.firstname AS requestedByFirst, a.lastname AS requestedByLast, v.name AS campusName "
                    + "FROM delete_request d "
                    + "JOIN admin a ON a.adminID = d.requestedByAdminID "
                    + "LEFT JOIN event e ON e.eventID = a.eventID "
                    + "LEFT JOIN venue v ON v.venueID = e.venueID "
                    + "ORDER BY d.status ASC, d.requestedAt DESC", Collections.emptyList());
        }
        return runListQuery("SELECT d.deleteRequestID, d.requestedByAdminID, d.targetRole, d.targetUserID, d.reason, d.status, d.requestedAt, "
                + "a.firstname AS requestedByFirst, a.lastname AS requestedByLast, v.name AS campusName "
                + "FROM delete_request d "
                + "JOIN admin a ON a.adminID = d.requestedByAdminID "
                + "LEFT JOIN event e ON e.eventID = a.eventID "
                + "LEFT JOIN venue v ON v.venueID = e.venueID "
                + "WHERE d.requestedByAdminID = ? ORDER BY d.requestedAt DESC",
                Arrays.<Object>asList(adminId));
    }

    public List<Map<String, Object>> getCampusNames() throws SQLException {
        return runListQuery("SELECT DISTINCT name AS campusName FROM venue ORDER BY name ASC", Collections.emptyList());
    }

    public boolean resolveDeletionRequest(int adminId, int deleteRequestId, boolean approve, String resolutionNote) throws SQLException {
        if (!isPrivilegedAdmin(adminId)) {
            throw new SQLException("PrivilegedRequired");
        }
        String fetchSql = "SELECT targetRole, targetUserID, status FROM delete_request WHERE deleteRequestID = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String targetRole = null;
                Integer targetUserId = null;
                try (PreparedStatement fetch = conn.prepareStatement(fetchSql)) {
                    fetch.setInt(1, deleteRequestId);
                    try (ResultSet rs = fetch.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        if (!"PENDING".equalsIgnoreCase(rs.getString("status"))) {
                            conn.rollback();
                            return false;
                        }
                        targetRole = rs.getString("targetRole");
                        targetUserId = rs.getInt("targetUserID");
                    }
                }

                if (approve) {
                    String table = tableForRole(targetRole);
                    String idCol = idColumnForRole(targetRole);
                    if (table == null || idCol == null) {
                        conn.rollback();
                        return false;
                    }
                    try (PreparedStatement deletePs = conn.prepareStatement("DELETE FROM " + table + " WHERE " + idCol + " = ?")) {
                        deletePs.setInt(1, targetUserId);
                        deletePs.executeUpdate();
                    }
                }

                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE delete_request SET status=?, resolvedByAdminID=?, resolvedAt=CURRENT_TIMESTAMP, resolutionNote=? WHERE deleteRequestID=?")) {
                    upd.setString(1, approve ? "APPROVED" : "REJECTED");
                    upd.setInt(2, adminId);
                    upd.setString(3, resolutionNote == null ? (approve ? "Approved and deleted." : "Rejected.") : resolutionNote.trim());
                    upd.setInt(4, deleteRequestId);
                    upd.executeUpdate();
                }

                logAudit(conn, adminId, approve ? "DELETE_REQUEST_APPROVED" : "DELETE_REQUEST_REJECTED",
                        "delete_request", String.valueOf(deleteRequestId),
                        (approve ? "Approved delete request for " : "Rejected delete request for ") + targetRole + ":" + targetUserId);

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean createEventProposal(int adminId, String eventName, String eventType, Timestamp eventDate,
            int venueId, String notes) throws SQLException {
        if (adminId <= 0 || venueId <= 0 || eventDate == null
                || eventName == null || eventName.trim().isEmpty()
                || eventType == null || eventType.trim().isEmpty()) {
            throw new SQLException("MissingFields");
        }
        if (!hasCampusAccessForRoleMutation(adminId, "VENUE_GUARD", null, null, venueId, null)) {
            throw new SQLException("CampusScopeDenied");
        }

        String sql = "INSERT INTO event_proposal(submittedByAdminID, venueID, eventName, eventType, eventDate, notes, status, createdAt) "
                + "VALUES(?,?,?,?,?,?,?,CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setInt(2, venueId);
            ps.setString(3, eventName.trim());
            ps.setString(4, eventType.trim());
            ps.setTimestamp(5, eventDate);
            ps.setString(6, notes == null ? null : notes.trim());
            ps.setString(7, "PENDING");
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logAudit(conn, adminId, "CREATE_EVENT_PROPOSAL", "event_proposal", "-", "Submitted event proposal");
                return true;
            }
            return false;
        }
    }

    public List<Map<String, Object>> getEventProposalsForScope(int adminId) throws SQLException {
        String sql = "SELECT p.proposalID, p.submittedByAdminID, p.venueID, p.eventName, p.eventType, p.eventDate, p.notes, p.status, "
                + "p.reviewedByAdminID, p.reviewedAt, p.reviewNote, p.createdAt, v.name AS campusName, "
                + "sa.firstname AS submittedFirst, sa.lastname AS submittedLast, "
                + "ra.firstname AS reviewedFirst, ra.lastname AS reviewedLast "
                + "FROM event_proposal p "
                + "LEFT JOIN venue v ON v.venueID = p.venueID "
                + "LEFT JOIN admin sa ON sa.adminID = p.submittedByAdminID "
                + "LEFT JOIN admin ra ON ra.adminID = p.reviewedByAdminID ";
        if (isPrivilegedAdmin(adminId)) {
            return runListQuery(sql + "ORDER BY p.status ASC, p.createdAt DESC", Collections.emptyList());
        }
        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (campusVenueId == null || campusVenueId <= 0) {
            return new ArrayList<>();
        }
        return runListQuery(sql + "WHERE p.venueID = ? ORDER BY p.status ASC, p.createdAt DESC",
                Arrays.<Object>asList(campusVenueId));
    }

    public boolean reviewEventProposal(int adminId, int proposalId, boolean approve, String reviewNote) throws SQLException {
        if (proposalId <= 0) {
            return false;
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int venueId = 0;
                String status;
                String eventName = null;
                String eventType = null;
                Timestamp eventDate = null;
                try (PreparedStatement fetch = conn.prepareStatement(
                        "SELECT venueID, status, eventName, eventType, eventDate FROM event_proposal WHERE proposalID = ?")) {
                    fetch.setInt(1, proposalId);
                    try (ResultSet rs = fetch.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        venueId = rs.getInt("venueID");
                        status = rs.getString("status");
                        eventName = rs.getString("eventName");
                        eventType = rs.getString("eventType");
                        eventDate = rs.getTimestamp("eventDate");
                    }
                }

                if (!"PENDING".equalsIgnoreCase(status)) {
                    conn.rollback();
                    return false;
                }

                if (!isPrivilegedAdmin(adminId) && !hasCampusAccessForRoleMutation(adminId, "VENUE_GUARD", null, null, venueId, null)) {
                    throw new SQLException("CampusScopeDenied");
                }

                String newStatus = approve ? "APPROVED" : "REJECTED";
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE event_proposal SET status = ?, reviewedByAdminID = ?, reviewedAt = CURRENT_TIMESTAMP, reviewNote = ? WHERE proposalID = ?")) {
                    update.setString(1, newStatus);
                    update.setInt(2, adminId);
                    update.setString(3, reviewNote == null ? null : reviewNote.trim());
                    update.setInt(4, proposalId);
                    update.executeUpdate();
                }

                if (approve) {
                    try (PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO event(name, type, date, venueID) VALUES(?,?,?,?)")) {
                        insert.setString(1, eventName);
                        insert.setString(2, eventType);
                        insert.setTimestamp(3, eventDate);
                        insert.setInt(4, venueId);
                        insert.executeUpdate();
                    }
                }

                logAudit(conn, adminId, approve ? "APPROVE_EVENT_PROPOSAL" : "REJECT_EVENT_PROPOSAL",
                        "event_proposal", String.valueOf(proposalId), "Reviewed event proposal");
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean createRefundCase(int adminId, int attendeeId, Integer orderId, Integer eventId, String reason) throws SQLException {
        if (adminId <= 0 || attendeeId <= 0) {
            throw new SQLException("MissingFields");
        }
        if (eventId != null && eventId > 0 && !hasCampusAccessForRoleMutation(adminId, "ADMIN", null, eventId, null, null)) {
            throw new SQLException("CampusScopeDenied");
        }

        String sql = "INSERT INTO attendee_refund_request(attendeeID, orderID, eventID, requestedByAdminID, reason, status, requestedAt) "
                + "VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeId);
            if (orderId == null || orderId <= 0) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, orderId);
            }
            if (eventId == null || eventId <= 0) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, eventId);
            }
            ps.setInt(4, adminId);
            ps.setString(5, reason == null ? "Campus support refund case" : reason.trim());
            ps.setString(6, "PENDING");
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logAudit(conn, adminId, "CREATE_REFUND_CASE", "attendee_refund_request", "-", "Created refund case");
                return true;
            }
            return false;
        }
    }

    public List<Map<String, Object>> getRefundRequestsForScope(int adminId) throws SQLException {
        String sql = "SELECT r.refundRequestID, r.attendeeID, r.orderID, r.eventID, r.requestedByAdminID, r.reason, r.status, "
                + "r.resolutionNote, r.resolvedByAdminID, r.requestedAt, r.resolvedAt, "
                + "a.firstname AS attendeeFirst, a.lastname AS attendeeLast, a.email AS attendeeEmail, "
                + "e.name AS eventName, v.name AS campusName "
                + "FROM attendee_refund_request r "
                + "JOIN attendee a ON a.attendeeID = r.attendeeID "
                + "LEFT JOIN event e ON e.eventID = r.eventID "
                + "LEFT JOIN venue v ON v.venueID = e.venueID ";

        if (isPrivilegedAdmin(adminId)) {
            return runListQuery(sql + "ORDER BY r.status ASC, r.requestedAt DESC", Collections.emptyList());
        }

        Integer campusVenueId = getAdminCampusVenueId(adminId);
        if (campusVenueId == null || campusVenueId <= 0) {
            return new ArrayList<>();
        }

        return runListQuery(sql + "WHERE e.venueID = ? ORDER BY r.status ASC, r.requestedAt DESC",
                Arrays.<Object>asList(campusVenueId));
    }

    public boolean resolveRefundCase(int adminId, int refundRequestId, boolean approve, String resolutionNote) throws SQLException {
        if (refundRequestId <= 0) {
            return false;
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer eventId = null;
                String status;
                try (PreparedStatement fetch = conn.prepareStatement(
                        "SELECT eventID, status FROM attendee_refund_request WHERE refundRequestID = ?")) {
                    fetch.setInt(1, refundRequestId);
                    try (ResultSet rs = fetch.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        status = rs.getString("status");
                        eventId = rs.getObject("eventID") == null ? null : rs.getInt("eventID");
                    }
                }

                if (!"PENDING".equalsIgnoreCase(status)) {
                    conn.rollback();
                    return false;
                }

                if (!isPrivilegedAdmin(adminId) && eventId != null && eventId > 0
                        && !hasCampusAccessForRoleMutation(adminId, "ADMIN", null, eventId, null, null)) {
                    throw new SQLException("CampusScopeDenied");
                }

                String newStatus = approve ? "APPROVED" : "REJECTED";
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE attendee_refund_request SET status = ?, resolutionNote = ?, resolvedByAdminID = ?, resolvedAt = CURRENT_TIMESTAMP WHERE refundRequestID = ?")) {
                    update.setString(1, newStatus);
                    update.setString(2, resolutionNote == null ? null : resolutionNote.trim());
                    update.setInt(3, adminId);
                    update.setInt(4, refundRequestId);
                    update.executeUpdate();
                }

                logAudit(conn, adminId, approve ? "APPROVE_REFUND_CASE" : "REJECT_REFUND_CASE",
                        "attendee_refund_request", String.valueOf(refundRequestId), "Reviewed refund case");
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Map<String, Object> getRootPasswordStatus() throws SQLException {
        Map<String, Object> status = new HashMap<>();
        status.put("source", "LEGACY");
        status.put("updatedAt", null);
        return status;
    }

    public Map<String, Object> getRootPasswordStatusForAdmin(int adminId) throws SQLException {
        Map<String, Object> status = new HashMap<>();
        status.put("source", isPrivilegedAdmin(adminId) ? "LOGIN_PASSWORD_SYNCED" : "LIMITED_ADMIN");
        status.put("updatedAt", null);
        return status;
    }

    public String getMinorRestrictedKeywordsSetting() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureSystemSettingTable(conn);
            String sql = "SELECT setting_value FROM " + SYSTEM_SETTING_TABLE + " WHERE setting_key = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, MINOR_RESTRICTED_KEYWORDS_SETTING_KEY);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String value = rs.getString("setting_value");
                        if (value != null && !value.trim().isEmpty()) {
                            return value.trim();
                        }
                    }
                }
            }

            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO " + SYSTEM_SETTING_TABLE + "(setting_key, setting_value, updated_at) VALUES(?, ?, CURRENT_TIMESTAMP)")) {
                insert.setString(1, MINOR_RESTRICTED_KEYWORDS_SETTING_KEY);
                insert.setString(2, MINOR_RESTRICTED_KEYWORDS_DEFAULT);
                insert.executeUpdate();
            }
            return MINOR_RESTRICTED_KEYWORDS_DEFAULT;
        }
    }

    public boolean updateMinorRestrictedKeywordsSetting(int adminId, String value) throws SQLException {
        if (!isPrivilegedAdmin(adminId)) {
            throw new SQLException("PrivilegedRequired");
        }
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            normalized = MINOR_RESTRICTED_KEYWORDS_DEFAULT;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureSystemSettingTable(conn);
            int updated;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE " + SYSTEM_SETTING_TABLE + " SET setting_value = ?, updated_at = CURRENT_TIMESTAMP WHERE setting_key = ?")) {
                ps.setString(1, normalized);
                ps.setString(2, MINOR_RESTRICTED_KEYWORDS_SETTING_KEY);
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO " + SYSTEM_SETTING_TABLE + "(setting_key, setting_value, updated_at) VALUES(?, ?, CURRENT_TIMESTAMP)")) {
                    insert.setString(1, MINOR_RESTRICTED_KEYWORDS_SETTING_KEY);
                    insert.setString(2, normalized);
                    updated = insert.executeUpdate();
                }
            }
            if (updated > 0) {
                logAudit(conn, adminId, "UPDATE_MINOR_KEYWORDS", SYSTEM_SETTING_TABLE,
                        MINOR_RESTRICTED_KEYWORDS_SETTING_KEY, "Updated under-18 restricted event keywords setting");
            }
            return updated > 0;
        }
    }

    public boolean rotateRootPassword(int adminId, String currentPassword, String newPassword) throws SQLException {
        if (!verifyPrivilegedRootPassword(adminId, currentPassword)) {
            return false;
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new SQLException("New root password is too short");
        }

        String hashed = PasswordUtil.hashPassword(newPassword.trim());
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement update = conn.prepareStatement("UPDATE admin SET password = ? WHERE adminID = ?")) {
            update.setString(1, hashed);
            update.setInt(2, adminId);
            update.executeUpdate();
            logAudit(conn, adminId, "ROTATE_ROOT_PASSWORD", "admin", String.valueOf(adminId), "Privileged admin password rotated from admin UI");
        }
        return true;
    }

    public Map<String, Object> executeCrudSql(int adminId, String sql, String rootPassword) throws SQLException {
        if (!verifyPrivilegedRootPassword(adminId, rootPassword)) {
            throw new SQLException("Invalid root password");
        }

        if (sql == null) {
            throw new SQLException("SQL is required");
        }

        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        if (trimmed.contains(";")) {
            throw new SQLException("Multiple SQL statements are not allowed");
        }

        String op = trimmed.split("\\s+")[0].toUpperCase();
        if (!("SELECT".equals(op) || "INSERT".equals(op) || "UPDATE".equals(op) || "DELETE".equals(op))) {
            throw new SQLException("Only SELECT/INSERT/UPDATE/DELETE are allowed");
        }

        Map<String, Object> result = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection(); Statement st = conn.createStatement()) {
            if ("SELECT".equals(op)) {
                try (ResultSet rs = st.executeQuery(trimmed)) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    List<String> columns = new ArrayList<>();
                    for (int i = 1; i <= cols; i++) {
                        columns.add(md.getColumnLabel(i));
                    }
                    List<Map<String, Object>> rows = new ArrayList<>();
                    int count = 0;
                    while (rs.next() && count < 500) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (String c : columns) {
                            row.put(c, rs.getObject(c));
                        }
                        rows.add(row);
                        count++;
                    }
                    result.put("columns", columns);
                    result.put("rows", rows);
                    result.put("updateCount", 0);
                }
                logAudit(conn, adminId, "SQL_SELECT", "sql_console", "-", trimmed);
            } else {
                int updates = st.executeUpdate(trimmed);
                result.put("columns", new ArrayList<String>());
                result.put("rows", new ArrayList<Map<String, Object>>());
                result.put("updateCount", updates);
                logAudit(conn, adminId, "SQL_MUTATION", "sql_console", "-", op + " affected rows: " + updates);
            }
        }
        return result;
    }

    public boolean updateSingleCell(int adminId, String tableName, String rowIdColumn, int rowId,
            String targetColumn, String newValue, String rootPassword) throws SQLException {
        if (!verifyPrivilegedRootPassword(adminId, rootPassword)) {
            throw new SQLException("Invalid root password");
        }

        String table = normalizeTableName(tableName);
        String rowCol = normalizeIdentifier(rowIdColumn);
        String targetCol = normalizeIdentifier(targetColumn);
        if (!PREVIEW_TABLES.contains(table) || rowCol.isEmpty() || targetCol.isEmpty()) {
            return false;
        }

        String sql = "UPDATE " + table + " SET " + targetCol + " = ? WHERE " + rowCol + " = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newValue);
            ps.setInt(2, rowId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logAudit(conn, adminId, "UPDATE_CELL", table, String.valueOf(rowId), targetCol + " updated");
                return true;
            }
            return false;
        }
    }

    public List<String> getAllUserTables() throws SQLException {
        List<String> tableNames = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (tableName != null && !tableName.startsWith("SYS")) {
                        tableNames.add(tableName.toLowerCase());
                    }
                }
            }
        }
        Collections.sort(tableNames);
        return tableNames;
    }

    private List<Map<String, Object>> withRole(List<Map<String, Object>> rows, String role, String idColumn) {
        for (Map<String, Object> row : rows) {
            row.put("role", role);
            row.put("uid", row.get(idColumn));
        }
        return rows;
    }

    private int createQrRecord(Connection conn, String barString) throws SQLException {
        String insertQr = "INSERT INTO qrcode(barstring, number) VALUES(?, ?)";
        try (PreparedStatement qrPs = conn.prepareStatement(insertQr, Statement.RETURN_GENERATED_KEYS)) {
            qrPs.setString(1, barString);
            qrPs.setInt(2, Math.abs(barString.hashCode()));
            qrPs.executeUpdate();
            try (ResultSet keys = qrPs.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Failed to create QR code");
                }
                return keys.getInt(1);
            }
        }
    }

    private void insertRoleAccount(Connection conn, String role, String firstName, String lastName, String email,
            String passwordHash, Integer eventId, Integer venueId, Integer guardId, String tertiaryInstitution) throws SQLException {
        if ("ADMIN".equals(role)) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO admin(firstname, lastname, email, password, eventID) VALUES(?,?,?,?,?)")) {
                ps.setString(1, firstName);
                ps.setString(2, lastName);
                ps.setString(3, email);
                ps.setString(4, passwordHash);
                ps.setInt(5, requiredInt(eventId, "eventID"));
                ps.executeUpdate();
            }
            return;
        }
        if ("VENUE_GUARD".equals(role)) {
            int qrId = createQrRecord(conn, "VG|" + email.toLowerCase() + "|R|" + System.currentTimeMillis());
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO venue_guard(firstname, lastname, email, password, eventID, venueID, QRcodeID) VALUES(?,?,?,?,?,?,?)")) {
                ps.setString(1, firstName);
                ps.setString(2, lastName);
                ps.setString(3, email);
                ps.setString(4, passwordHash);
                ps.setInt(5, requiredInt(eventId, "eventID"));
                ps.setInt(6, requiredInt(venueId, "venueID"));
                ps.setInt(7, qrId);
                ps.executeUpdate();
            }
            return;
        }
        if ("EVENT_MANAGER".equals(role)) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO event_manager(firstname, lastname, email, password, venueGuardID) VALUES(?,?,?,?,?)")) {
                ps.setString(1, firstName);
                ps.setString(2, lastName);
                ps.setString(3, email);
                ps.setString(4, passwordHash);
                ps.setInt(5, requiredInt(guardId, "venueGuardID"));
                ps.executeUpdate();
            }
            return;
        }
        if ("TERTIARY_PRESENTER".equals(role)) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO tertiary_presenter(firstname, lastname, email, password, tertiaryInstitution, eventID, venueID) VALUES(?,?,?,?,?,?,?)")) {
                ps.setString(1, firstName);
                ps.setString(2, lastName);
                ps.setString(3, email);
                ps.setString(4, passwordHash);
                ps.setString(5, tertiaryInstitution == null ? "Institution Pending" : tertiaryInstitution);
                ps.setInt(6, requiredInt(eventId, "eventID"));
                ps.setInt(7, requiredInt(venueId, "venueID"));
                ps.executeUpdate();
            }
            return;
        }
        throw new SQLException("Unsupported target role for reassignment");
    }

    private int requiredInt(Integer value, String label) throws SQLException {
        if (value == null || value <= 0) {
            throw new SQLException("Missing required field: " + label);
        }
        return value;
    }

    private Map<String, Object> fetchIdentity(Connection conn, String table, String idCol, int id) throws SQLException {
        String sql = "SELECT firstname, lastname, email, password FROM " + table + " WHERE " + idCol + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Map<String, Object> map = new HashMap<>();
                map.put("firstname", rs.getString("firstname"));
                map.put("lastname", rs.getString("lastname"));
                map.put("email", rs.getString("email"));
                map.put("password", rs.getString("password"));
                return map;
            }
        }
    }

    private int resolveUserIdByEmail(Connection conn, String table, String idCol, String email) throws SQLException {
        String sql = "SELECT " + idCol + " FROM " + table + " WHERE LOWER(email)=LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                return rs.getInt(1);
            }
        }
    }

    private void upsertForceReset(Connection conn, String role, int userId, int adminId, boolean forceReset) throws SQLException {
        if (userId <= 0) {
            return;
        }
        boolean hasUpdatedByAdminId = columnExists(conn, "account_control", "updatedByAdminID");
        boolean hasUpdatedAt = columnExists(conn, "account_control", "updatedAt");
        boolean hasForceReset = columnExists(conn, "account_control", "forceReset");
        boolean exists = controlRowExists(conn, role, userId);
        if (exists) {
            StringBuilder updateSql = new StringBuilder("UPDATE account_control SET ");
            if (hasForceReset) {
                updateSql.append("forceReset=?");
            } else {
                // No forceReset column on legacy schemas; keep lock-state semantics unchanged.
                updateSql.append("isLocked=isLocked");
            }
            if (hasUpdatedByAdminId) {
                updateSql.append(", updatedByAdminID=?");
            }
            if (hasUpdatedAt) {
                updateSql.append(", updatedAt=CURRENT_TIMESTAMP");
            }
            updateSql.append(" WHERE roleName=? AND userID=?");

            try (PreparedStatement ps = conn.prepareStatement(updateSql.toString())) {
                int idx = 1;
                if (hasForceReset) {
                    ps.setBoolean(idx++, forceReset);
                }
                if (hasUpdatedByAdminId) {
                    ps.setInt(idx++, adminId);
                }
                ps.setString(idx++, role);
                ps.setInt(idx, userId);
                ps.executeUpdate();
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
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                int idx = 1;
                ps.setString(idx++, role);
                ps.setInt(idx++, userId);
                ps.setBoolean(idx++, false);
                if (hasForceReset) {
                    ps.setBoolean(idx++, forceReset);
                }
                if (hasUpdatedByAdminId) {
                    ps.setInt(idx++, adminId);
                }
                ps.executeUpdate();
            }
        }
    }

    private boolean controlRowExists(Connection conn, String role, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM account_control WHERE roleName=? AND userID=?")) {
            ps.setString(1, role);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void logAudit(Connection conn, int adminId, String actionType, String targetTable, String targetId, String details) throws SQLException {
        if (adminId <= 0) {
            return;
        }
        String sql = "INSERT INTO admin_audit_log(adminID, actionType, targetTable, targetID, details, createdAt) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setString(2, actionType);
            ps.setString(3, targetTable);
            ps.setString(4, targetId);
            ps.setString(5, details);
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        }
    }

    public boolean hasCampusAccessForRoleMutation(int adminId, String role, Integer targetUserId,
            Integer eventId, Integer venueId, Integer guardId) throws SQLException {
        if (adminId <= 0) {
            return false;
        }
        if (isPrivilegedAdmin(adminId)) {
            return true;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            Integer campusVenueId = getAdminCampusVenueId(conn, adminId);
            if (campusVenueId == null || campusVenueId <= 0) {
                return false;
            }

            String normalizedRole = role == null ? "" : role.trim().toUpperCase();

            if (targetUserId != null && targetUserId > 0) {
                if (!roleTargetBelongsToVenue(conn, normalizedRole, targetUserId, campusVenueId)) {
                    return false;
                }
            }

            if (eventId != null && eventId > 0 && !eventBelongsToVenue(conn, eventId, campusVenueId)) {
                return false;
            }
            if (venueId != null && venueId > 0 && venueId.intValue() != campusVenueId.intValue()) {
                return false;
            }
            if (guardId != null && guardId > 0 && !guardBelongsToVenue(conn, guardId, campusVenueId)) {
                return false;
            }

            return true;
        }
    }

    private Integer getAdminCampusVenueId(int adminId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getAdminCampusVenueId(conn, adminId);
        }
    }

    private String normalizeOptionalText(String value, int maxLength) throws SQLException {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (maxLength > 0 && normalized.length() > maxLength) {
            throw new SQLException("InvalidAssignment");
        }
        return normalized;
    }

    private String normalizeEventStatus(String status) {
        String normalized = status == null ? "ACTIVE" : status.trim().toUpperCase();
        if (normalized.isEmpty()) {
            normalized = "ACTIVE";
        }
        if (!"ACTIVE".equals(normalized) && !"CANCELLED".equals(normalized) && !"PASSED".equals(normalized)) {
            return "ACTIVE";
        }
        return normalized;
    }

    private Integer getAdminCampusVenueId(Connection conn, int adminId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT e.venueID FROM admin a JOIN event e ON e.eventID = a.eventID WHERE a.adminID = ?")) {
            ps.setInt(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getInt(1);
            }
        }
    }

    private boolean eventBelongsToVenue(Connection conn, int eventId, int venueId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM event WHERE eventID = ? AND venueID = ?")) {
            ps.setInt(1, eventId);
            ps.setInt(2, venueId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean guardBelongsToVenue(Connection conn, int guardId, int venueId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM venue_guard WHERE venueGuardID = ? AND venueID = ?")) {
            ps.setInt(1, guardId);
            ps.setInt(2, venueId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean roleTargetBelongsToVenue(Connection conn, String role, int userId, int venueId) throws SQLException {
        if ("ADMIN".equals(role)) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM admin a JOIN event e ON e.eventID = a.eventID WHERE a.adminID = ? AND e.venueID = ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, venueId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }
        if ("VENUE_GUARD".equals(role)) {
            return guardBelongsToVenue(conn, userId, venueId);
        }
        if ("EVENT_MANAGER".equals(role)) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM event_manager m JOIN venue_guard g ON g.venueGuardID = m.venueGuardID WHERE m.eventManagerID = ? AND g.venueID = ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, venueId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }
        if ("TERTIARY_PRESENTER".equals(role)) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM tertiary_presenter WHERE tertiaryPresenterID = ? AND venueID = ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, venueId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }
        return false;
    }

    private String tableForRole(String role) {
        if (role == null) {
            return null;
        }
        switch (role.trim().toUpperCase()) {
            case "ADMIN":
                return "admin";
            case "VENUE_GUARD":
                return "venue_guard";
            case "EVENT_MANAGER":
                return "event_manager";
            case "TERTIARY_PRESENTER":
                return "tertiary_presenter";
            default:
                return null;
        }
    }

    private String idColumnForRole(String role) {
        if (role == null) {
            return null;
        }
        switch (role.trim().toUpperCase()) {
            case "ADMIN":
                return "adminID";
            case "VENUE_GUARD":
                return "venueGuardID";
            case "EVENT_MANAGER":
                return "eventManagerID";
            case "TERTIARY_PRESENTER":
                return "tertiaryPresenterID";
            default:
                return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String joinNames(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "-";
        }
        List<String> names = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String full = (stringValue(row.get("firstname")) + " " + stringValue(row.get("lastname"))).trim();
            String email = stringValue(row.get("email"));
            if (!full.isEmpty() && !email.isEmpty()) {
                names.add(full + " (" + email + ")");
            } else if (!full.isEmpty()) {
                names.add(full);
            } else if (!email.isEmpty()) {
                names.add(email);
            }
        }
        return names.isEmpty() ? "-" : String.join(", ", names);
    }

    private boolean isRoleAssignmentValid(String role, Integer eventId, Integer venueId, Integer guardId,
            String tertiaryInstitution, boolean strict) throws SQLException {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if ("ADMIN".equals(normalizedRole)) {
                return strict ? existsById(conn, "event", "eventID", eventId) : optionalExists(conn, "event", "eventID", eventId);
            }
            if ("VENUE_GUARD".equals(normalizedRole)) {
                boolean eventOk = strict ? existsById(conn, "event", "eventID", eventId) : optionalExists(conn, "event", "eventID", eventId);
                boolean venueOk = strict ? existsById(conn, "venue", "venueID", venueId) : optionalExists(conn, "venue", "venueID", venueId);
                return eventOk && venueOk;
            }
            if ("EVENT_MANAGER".equals(normalizedRole)) {
                return strict ? existsById(conn, "venue_guard", "venueGuardID", guardId) : optionalExists(conn, "venue_guard", "venueGuardID", guardId);
            }
            if ("TERTIARY_PRESENTER".equals(normalizedRole)) {
                if (strict && (tertiaryInstitution == null || tertiaryInstitution.trim().isEmpty())) {
                    return false;
                }
                boolean eventOk = strict ? existsById(conn, "event", "eventID", eventId) : optionalExists(conn, "event", "eventID", eventId);
                boolean venueOk = strict ? existsById(conn, "venue", "venueID", venueId) : optionalExists(conn, "venue", "venueID", venueId);
                return eventOk && venueOk;
            }
            return false;
        }
    }

    private boolean optionalExists(Connection conn, String table, String column, Integer id) throws SQLException {
        if (id == null || id <= 0) {
            return true;
        }
        return existsById(conn, table, column, id);
    }

    private boolean existsById(Connection conn, String table, String column, Integer id) throws SQLException {
        if (id == null || id <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM " + table + " WHERE " + column + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean existsById(int id, String table, String column) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return existsById(conn, table, column, id);
        }
    }

    private int toInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private List<Map<String, Object>> runListQuery(String sql, List<Object> params) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    for (int i = 1; i <= cols; i++) {
                        String label = meta.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        if (label == null || label.trim().isEmpty()) {
                            label = meta.getColumnName(i);
                        }
                        if (label == null || label.trim().isEmpty()) {
                            continue;
                        }
                        row.put(label, value);
                        row.putIfAbsent(label.toLowerCase(), value);
                        row.putIfAbsent(label.toUpperCase(), value);
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private int queryInt(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private double queryDouble(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private double calculateScannerUptime(Connection conn) throws SQLException {
        int valid = queryInt(conn, "SELECT COUNT(*) FROM scan_log WHERE result = 'VALID'");
        int invalid = queryInt(conn, "SELECT COUNT(*) FROM scan_log WHERE result = 'INVALID'");
        int total = valid + invalid;
        if (total == 0) {
            return 100.0;
        }
        return Math.round((valid * 10000.0) / total) / 100.0;
    }

    private String normalizeTableName(String table) {
        if (table == null) {
            return "";
        }
        return table.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
    }

    private String normalizeIdentifier(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (!SAFE_IDENTIFIER.matcher(trimmed).matches()) {
            return "";
        }
        return trimmed;
    }

    private void ensureSystemSettingTable(Connection conn) throws SQLException {
        if (tableExists(conn, SYSTEM_SETTING_TABLE)) {
            return;
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE " + SYSTEM_SETTING_TABLE
                    + " (setting_key VARCHAR(120) NOT NULL PRIMARY KEY, setting_value VARCHAR(2048), updated_at TIMESTAMP)");
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        String upper = tableName == null ? "" : tableName.toUpperCase();
        try (ResultSet rs = meta.getTables(null, null, upper, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        String upperTable = tableName == null ? "" : tableName.toUpperCase();
        String upperColumn = columnName == null ? "" : columnName.toUpperCase();
        try (ResultSet rs = meta.getColumns(null, null, upperTable, upperColumn)) {
            return rs.next();
        }
    }
}
