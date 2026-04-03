package za.ac.tut.databaseManagement;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import za.ac.tut.databaseConnection.DatabaseConnection;

public class EventManagerDashboardDAO {

    public Map<String, Object> getManagerProfile(int eventManagerId) throws SQLException {
        String sql = "SELECT m.eventManagerID, m.firstname, m.lastname, m.email, m.venueGuardID, "
                + "vg.venueID, v.name AS campusName "
                + "FROM event_manager m "
                + "LEFT JOIN venue_guard vg ON vg.venueGuardID = m.venueGuardID "
                + "LEFT JOIN venue v ON v.venueID = vg.venueID "
                + "WHERE m.eventManagerID = ?";
        List<Map<String, Object>> rows = runListQuery(sql, eventManagerId);
        return rows.isEmpty() ? new HashMap<String, Object>() : rows.get(0);
    }

    public List<Map<String, Object>> getAssignedEvents(int eventManagerId) throws SQLException {
        String sql = "SELECT e.eventID, e.name AS eventName, e.type AS eventType, e.date AS eventDate, "
            + "e.description, e.infoUrl, COALESCE(NULLIF(TRIM(e.status), ''), 'ACTIVE') AS status, "
                + "v.name AS campusName, "
                + "COUNT(DISTINCT eht.ticketID) AS ticketTemplates, "
                + "COUNT(aht.attendeeID) AS soldTickets, "
                + "COALESCE(SUM(t.price), 0) AS revenue "
                + "FROM event e "
                + "JOIN event_has_manager ehm ON ehm.eventID = e.eventID "
                + "LEFT JOIN venue v ON v.venueID = e.venueID "
                + "LEFT JOIN event_has_ticket eht ON eht.eventID = e.eventID "
                + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = eht.ticketID "
                + "LEFT JOIN ticket t ON t.ticketID = aht.ticketID "
                + "WHERE ehm.eventManagerID = ? "
                + "GROUP BY e.eventID, e.name, e.type, e.date, e.description, e.infoUrl, e.status, v.name "
                + "ORDER BY e.date ASC";
        return runListQuery(sql, eventManagerId);
    }

    public List<Map<String, Object>> getVenueGuardCoverage(int eventManagerId) throws SQLException {
        String sql = "SELECT g.venueGuardID, g.firstname, g.lastname, g.email, g.eventID, e.name AS eventName, "
                + "SUM(CASE WHEN sl.result = 'VALID' THEN 1 ELSE 0 END) AS validScans, "
                + "SUM(CASE WHEN sl.result = 'INVALID' THEN 1 ELSE 0 END) AS invalidScans "
                + "FROM event_manager m "
                + "JOIN venue_guard ownerGuard ON ownerGuard.venueGuardID = m.venueGuardID "
                + "JOIN venue_guard g ON g.venueID = ownerGuard.venueID "
                + "LEFT JOIN event e ON e.eventID = g.eventID "
                + "LEFT JOIN scan_log sl ON sl.venueGuardID = g.venueGuardID "
                + "WHERE m.eventManagerID = ? "
                + "GROUP BY g.venueGuardID, g.firstname, g.lastname, g.email, g.eventID, e.name "
                + "ORDER BY g.venueGuardID DESC";
        return runListQuery(sql, eventManagerId);
    }

    public List<Map<String, Object>> getPresenterSessions(int eventManagerId) throws SQLException {
        String sql = "SELECT p.tertiaryPresenterID, p.firstname, p.lastname, p.email, p.tertiaryInstitution, "
                + "p.eventID, e.name AS eventName, e.date AS eventDate "
                + "FROM event_manager m "
                + "JOIN venue_guard vg ON vg.venueGuardID = m.venueGuardID "
                + "JOIN tertiary_presenter p ON p.venueID = vg.venueID "
                + "LEFT JOIN event e ON e.eventID = p.eventID "
                + "WHERE m.eventManagerID = ? "
                + "ORDER BY e.date ASC, p.tertiaryPresenterID DESC";
        return runListQuery(sql, eventManagerId);
    }

    public int countInvalidScansLast24h(int eventManagerId) throws SQLException {
        String sql = "SELECT COUNT(*) "
                + "FROM event_manager m "
                + "JOIN venue_guard ownerGuard ON ownerGuard.venueGuardID = m.venueGuardID "
                + "JOIN venue_guard g ON g.venueID = ownerGuard.venueID "
                + "JOIN scan_log sl ON sl.venueGuardID = g.venueGuardID "
                + "WHERE m.eventManagerID = ? "
                + "AND sl.result = 'INVALID' "
                + "AND sl.scannedAt >= ?";
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - (24L * 60L * 60L * 1000L));
        return runCountWithTimestamp(sql, eventManagerId, cutoff);
    }

    public int countValidScansLast24h(int eventManagerId) throws SQLException {
        String sql = "SELECT COUNT(*) "
                + "FROM event_manager m "
                + "JOIN venue_guard ownerGuard ON ownerGuard.venueGuardID = m.venueGuardID "
                + "JOIN venue_guard g ON g.venueID = ownerGuard.venueID "
                + "JOIN scan_log sl ON sl.venueGuardID = g.venueGuardID "
                + "WHERE m.eventManagerID = ? "
                + "AND sl.result = 'VALID' "
                + "AND sl.scannedAt >= ?";
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - (24L * 60L * 60L * 1000L));
        return runCountWithTimestamp(sql, eventManagerId, cutoff);
    }

    public int countEventsWithoutTickets(int eventManagerId) throws SQLException {
        String sql = "SELECT COUNT(*) "
                + "FROM event e "
                + "JOIN event_has_manager ehm ON ehm.eventID = e.eventID "
                + "LEFT JOIN event_has_ticket eht ON eht.eventID = e.eventID "
                + "WHERE ehm.eventManagerID = ? "
                + "GROUP BY e.eventID "
                + "HAVING COUNT(eht.ticketID) = 0";
        List<Map<String, Object>> grouped = runListQuery(sql, eventManagerId);
        return grouped.size();
    }

    public int countGuardsWithNoScans(int eventManagerId) throws SQLException {
        String sql = "SELECT COUNT(*) "
                + "FROM event_manager m "
                + "JOIN venue_guard ownerGuard ON ownerGuard.venueGuardID = m.venueGuardID "
                + "JOIN venue_guard g ON g.venueID = ownerGuard.venueID "
                + "LEFT JOIN scan_log sl ON sl.venueGuardID = g.venueGuardID "
                + "WHERE m.eventManagerID = ? "
                + "GROUP BY g.venueGuardID "
                + "HAVING COUNT(sl.scanLogID) = 0";
        List<Map<String, Object>> grouped = runListQuery(sql, eventManagerId);
        return grouped.size();
    }

    public int countPresentersWithoutMappedEvent(int eventManagerId) throws SQLException {
        String sql = "SELECT COUNT(*) "
                + "FROM event_manager m "
                + "JOIN venue_guard vg ON vg.venueGuardID = m.venueGuardID "
                + "JOIN tertiary_presenter p ON p.venueID = vg.venueID "
                + "LEFT JOIN event e ON e.eventID = p.eventID "
                + "WHERE m.eventManagerID = ? "
                + "AND e.eventID IS NULL";
        return runCount(sql, eventManagerId);
    }

    public boolean updateAssignedEventDetails(int eventManagerId, int eventId, String eventName,
            String eventType, Timestamp eventDate, String description, String infoUrl, String status) throws SQLException {
        if (eventManagerId <= 0 || eventId <= 0 || eventDate == null
                || eventName == null || eventName.trim().isEmpty()
                || eventType == null || eventType.trim().isEmpty()) {
            return false;
        }

        String sql = "UPDATE event SET name = ?, type = ?, date = ?, description = ?, infoUrl = ?, status = ? "
                + "WHERE eventID = ? AND EXISTS ("
                + "SELECT 1 FROM event_has_manager ehm WHERE ehm.eventID = event.eventID AND ehm.eventManagerID = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventName.trim());
            ps.setString(2, eventType.trim());
            ps.setTimestamp(3, eventDate);
            ps.setString(4, description);
            ps.setString(5, infoUrl);
            ps.setString(6, status);
            ps.setInt(7, eventId);
            ps.setInt(8, eventManagerId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean createEventForManager(int eventManagerId, String eventName,
            String eventType, Timestamp eventDate, String description, String infoUrl, String status) throws SQLException {
        return createEventForManagerAndGetId(eventManagerId, eventName, eventType, eventDate, description, infoUrl, status) > 0;
    }

    public int createEventForManagerAndGetId(int eventManagerId, String eventName,
            String eventType, Timestamp eventDate, String description, String infoUrl, String status) throws SQLException {
        if (eventManagerId <= 0 || eventDate == null
                || eventName == null || eventName.trim().isEmpty()
                || eventType == null || eventType.trim().isEmpty()) {
            return 0;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int venueId = resolveManagerVenueId(conn, eventManagerId);
                if (venueId <= 0) {
                    conn.rollback();
                    return 0;
                }

                int eventId;
                try (PreparedStatement eventInsert = conn.prepareStatement(
                        "INSERT INTO event(name, type, date, venueID, description, infoUrl, status) VALUES(?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    eventInsert.setString(1, eventName.trim());
                    eventInsert.setString(2, eventType.trim());
                    eventInsert.setTimestamp(3, eventDate);
                    eventInsert.setInt(4, venueId);
                    eventInsert.setString(5, description);
                    eventInsert.setString(6, infoUrl);
                    eventInsert.setString(7, status);
                    eventInsert.executeUpdate();

                    try (ResultSet rs = eventInsert.getGeneratedKeys()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return 0;
                        }
                        eventId = rs.getInt(1);
                    }
                }

                try (PreparedStatement mapInsert = conn.prepareStatement(
                        "INSERT INTO event_has_manager(eventID, eventManagerID) VALUES(?, ?)")) {
                    mapInsert.setInt(1, eventId);
                    mapInsert.setInt(2, eventManagerId);
                    mapInsert.executeUpdate();
                }

                conn.commit();
                return eventId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public int createConcertSeriesForManager(int eventManagerId) throws SQLException {
        Map<String, Object> result = createConcertSeriesForManagerDetailed(eventManagerId);
        Object total = result.get("totalTickets");
        if (total instanceof Number) {
            return ((Number) total).intValue();
        }
        return 0;
    }

    public Map<String, Object> createConcertSeriesForManagerDetailed(int eventManagerId) throws SQLException {
        long now = System.currentTimeMillis();
        int totalTickets = 0;
        List<Integer> createdEventIds = new ArrayList<>();

        String[][] events = new String[][] {
            {"Neon Pulse Live", "Electronic Concert", "A high-energy electronic night with immersive visuals.", "https://tickify.live/events/neon-pulse"},
            {"Campus Amp Festival", "Festival Concert", "A multi-artist campus festival featuring diverse live acts.", "https://tickify.live/events/campus-amp"}
        };

        for (int i = 0; i < events.length; i++) {
            Timestamp date = new Timestamp(now + ((long) (i + 2) * 24L * 60L * 60L * 1000L));
            int eventId = createEventForManagerAndGetId(
                    eventManagerId,
                    events[i][0],
                    events[i][1],
                    date,
                    events[i][2],
                    events[i][3],
                    "ACTIVE"
            );
            if (eventId <= 0) {
                continue;
            }
            createdEventIds.add(eventId);

            byte[] poster = buildConcertPosterSvg(events[i][0], i).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            updateAssignedEventAlbumImage(eventManagerId, eventId,
                    "concert-poster-" + (i + 1) + ".svg", "image/svg+xml", poster);

            if (addTicketTierForAssignedEvent(eventManagerId, eventId, "VIP Front Row", new BigDecimal("950.00"), 30)) {
                totalTickets += 30;
            }
            if (addTicketTierForAssignedEvent(eventManagerId, eventId, "Golden Circle", new BigDecimal("650.00"), 80)) {
                totalTickets += 80;
            }
            if (addTicketTierForAssignedEvent(eventManagerId, eventId, "General Access", new BigDecimal("350.00"), 250)) {
                totalTickets += 250;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalTickets", totalTickets);
        result.put("eventIds", createdEventIds);
        return result;
    }

    public boolean updateAssignedEventAlbumImage(int eventManagerId, int eventId, String filename,
            String mimeType, byte[] imageData) throws SQLException {
        if (eventManagerId <= 0 || eventId <= 0 || imageData == null || imageData.length == 0) {
            return false;
        }
        String sql = "UPDATE event SET imageFilename = ?, imageMimeType = ?, imageData = ? "
                + "WHERE eventID = ? AND EXISTS ("
                + "SELECT 1 FROM event_has_manager ehm WHERE ehm.eventID = event.eventID AND ehm.eventManagerID = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filename);
            ps.setString(2, mimeType);
            ps.setBytes(3, imageData);
            ps.setInt(4, eventId);
            ps.setInt(5, eventManagerId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean addTicketTierForAssignedEvent(int eventManagerId, int eventId, String tierName,
            BigDecimal price, int quantity) throws SQLException {
        if (eventManagerId <= 0 || eventId <= 0 || quantity <= 0 || price == null
                || tierName == null || tierName.trim().isEmpty()) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!isEventAssignedToManager(conn, eventManagerId, eventId)) {
                    conn.rollback();
                    return false;
                }

                try (PreparedStatement qrInsert = conn.prepareStatement(
                        "INSERT INTO qrcode(barstring, number) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement ticketInsert = conn.prepareStatement(
                        "INSERT INTO ticket(name, price, QRcodeID) VALUES(?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement mapInsert = conn.prepareStatement(
                        "INSERT INTO event_has_ticket(eventID, ticketID) VALUES(?, ?)")) {

                    long marker = System.currentTimeMillis();
                    for (int i = 1; i <= quantity; i++) {
                        String qrPayload = "EM-" + eventId + "-" + marker + "-" + i;
                        qrInsert.setString(1, qrPayload);
                        qrInsert.setInt(2, (int) ((marker % 1000000) + i));
                        qrInsert.executeUpdate();

                        int qrId;
                        try (ResultSet qrKeys = qrInsert.getGeneratedKeys()) {
                            if (!qrKeys.next()) {
                                conn.rollback();
                                return false;
                            }
                            qrId = qrKeys.getInt(1);
                        }

                        ticketInsert.setString(1, tierName.trim() + " #" + i);
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
                    }
                }

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

    public int clearUnsoldTicketTemplatesForAssignedEvent(int eventManagerId, int eventId) throws SQLException {
        if (eventManagerId <= 0 || eventId <= 0) {
            return 0;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!isEventAssignedToManager(conn, eventManagerId, eventId)) {
                    conn.rollback();
                    return 0;
                }

                List<Integer> ticketIds = new ArrayList<>();
                List<Integer> qrIds = new ArrayList<>();

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT t.ticketID, t.QRcodeID "
                        + "FROM event_has_ticket eht "
                        + "JOIN ticket t ON t.ticketID = eht.ticketID "
                        + "WHERE eht.eventID = ? "
                        + "AND NOT EXISTS (SELECT 1 FROM attendee_has_ticket aht WHERE aht.ticketID = eht.ticketID)")) {
                    ps.setInt(1, eventId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ticketIds.add(rs.getInt("ticketID"));
                            qrIds.add(rs.getInt("QRcodeID"));
                        }
                    }
                }

                if (ticketIds.isEmpty()) {
                    conn.commit();
                    return 0;
                }

                try (PreparedStatement delMap = conn.prepareStatement(
                        "DELETE FROM event_has_ticket WHERE eventID = ? AND ticketID = ?");
                     PreparedStatement delTicket = conn.prepareStatement(
                        "DELETE FROM ticket WHERE ticketID = ?");
                     PreparedStatement delQr = conn.prepareStatement(
                        "DELETE FROM qrcode WHERE QRcodeID = ?")) {
                    for (int i = 0; i < ticketIds.size(); i++) {
                        int ticketId = ticketIds.get(i);
                        int qrId = qrIds.get(i);

                        delMap.setInt(1, eventId);
                        delMap.setInt(2, ticketId);
                        delMap.executeUpdate();

                        delTicket.setInt(1, ticketId);
                        delTicket.executeUpdate();

                        delQr.setInt(1, qrId);
                        delQr.executeUpdate();
                    }
                }

                conn.commit();
                return ticketIds.size();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private boolean isEventAssignedToManager(Connection conn, int eventManagerId, int eventId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM event_has_manager WHERE eventManagerID = ? AND eventID = ?")) {
            ps.setInt(1, eventManagerId);
            ps.setInt(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int resolveManagerVenueId(Connection conn, int eventManagerId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT vg.venueID "
                + "FROM event_manager m "
                + "LEFT JOIN venue_guard vg ON vg.venueGuardID = m.venueGuardID "
                + "WHERE m.eventManagerID = ?")) {
            ps.setInt(1, eventManagerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                int venueId = rs.getInt(1);
                return rs.wasNull() ? 0 : venueId;
            }
        }
    }

    private String buildConcertPosterSvg(String title, int variant) {
        String[] accents = new String[] {"#79c84a", "#ff7a59", "#3db3ff"};
        String accent = accents[Math.abs(variant) % accents.length];
        return "<svg xmlns='http://www.w3.org/2000/svg' width='1200' height='630' viewBox='0 0 1200 630'>"
                + "<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>"
                + "<stop offset='0%' stop-color='#111827'/><stop offset='100%' stop-color='#1f2937'/></linearGradient></defs>"
                + "<rect width='1200' height='630' fill='url(#g)'/>"
                + "<circle cx='170' cy='130' r='90' fill='" + accent + "' opacity='0.18'/>"
                + "<circle cx='1020' cy='510' r='120' fill='" + accent + "' opacity='0.16'/>"
                + "<text x='80' y='280' font-family='Segoe UI, Arial, sans-serif' font-size='68' font-weight='700' fill='#f9fafb'>"
                + escapeXml(title)
                + "</text>"
                + "<text x='82' y='340' font-family='Segoe UI, Arial, sans-serif' font-size='28' fill='" + accent + "'>LIVE CONCERT SERIES</text>"
                + "<rect x='80' y='386' width='380' height='8' fill='" + accent + "' rx='4'/></svg>";
    }

    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private int runCount(String sql, int eventManagerId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventManagerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                return rs.getInt(1);
            }
        }
    }

    private int runCountWithTimestamp(String sql, int eventManagerId, Timestamp cutoff) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventManagerId);
            ps.setTimestamp(2, cutoff);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                return rs.getInt(1);
            }
        }
    }

    private List<Map<String, Object>> runListQuery(String sql, int eventManagerId) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventManagerId);
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
}
