package za.ac.tut.databaseManagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import za.ac.tut.databaseConnection.DatabaseConnection;

public class ScannerDAO {

    public ScanResult validateAndLog(String code, int venueGuardId) throws SQLException {
        return validateAndLog(code, venueGuardId, true);
    }

    public ScanResult validateAndLog(String code, int venueGuardId, boolean consumeOnSuccess) throws SQLException {
        String trimmedCode = code != null ? code.trim() : "";
        if (trimmedCode.isEmpty()) {
            logScan(venueGuardId, null, trimmedCode, "INVALID", "Code is empty");
            return ScanResult.invalid("Code is empty", "NOT_PROVIDED");
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            int guardEventId = getGuardEventId(conn, venueGuardId);
            ParsedTicketCode parsed = parseTicketCode(trimmedCode);
            String authenticityStatus = "NOT_PROVIDED";

            TicketSnapshot ticket;
            if (parsed.authOnlyToken != null) {
                ticket = findTicketByAuthToken(conn, parsed.authOnlyToken);
                authenticityStatus = ticket != null ? "VERIFIED" : "MISMATCH";
            } else {
                ticket = findTicketByCode(conn, parsed.baseCode);
            }

            if (ticket == null) {
                logScan(conn, venueGuardId, null, trimmedCode, "INVALID", "Ticket code not found");
                return ScanResult.invalid("Ticket code not found", authenticityStatus);
            }

            if (parsed.authToken != null && !parsed.authToken.isEmpty()) {
                String expected = deriveAuthToken(ticket.ticketNumber, ticket.qrPayload, ticket.attendeeEmail);
                if (!expected.equalsIgnoreCase(parsed.authToken)) {
                    logScan(conn, venueGuardId, ticket.ticketId, trimmedCode, "INVALID", "Authenticity token mismatch");
                    return ScanResult.invalid("Authenticity token mismatch", "MISMATCH");
                }
                authenticityStatus = "VERIFIED";
            }

            if (ticket.eventId == null) {
                logScan(conn, venueGuardId, ticket.ticketId, trimmedCode, "INVALID", "Ticket is not mapped to an event");
                return ScanResult.invalid("Ticket is not mapped to an event", authenticityStatus);
            }

            if (ticket.eventId != guardEventId) {
                logScan(conn, venueGuardId, ticket.ticketId, trimmedCode, "INVALID", "Ticket is for a different event");
                return ScanResult.invalid("Ticket is for a different event", authenticityStatus);
            }

            if (isTicketAlreadyUsed(conn, ticket.ticketId)) {
                logScan(conn, venueGuardId, ticket.ticketId, trimmedCode, "INVALID", "Ticket already used");
                return ScanResult.invalid("Ticket already used", authenticityStatus);
            }

            if (consumeOnSuccess) {
                logScan(conn, venueGuardId, ticket.ticketId, trimmedCode, "VALID", "Entry granted");
                return ScanResult.valid("Entry granted", ticket.ticketId, ticket.eventName, authenticityStatus);
            }

            logScan(conn, venueGuardId, ticket.ticketId, trimmedCode, "VERIFY", "Authenticity check only");
            return ScanResult.valid("Ticket found and authenticity verified", ticket.ticketId, ticket.eventName, authenticityStatus);
        }
    }

    private String deriveAuthToken(String ticketNumber, String qrPayload, String attendeeEmail) {
        String base = (ticketNumber == null ? "" : ticketNumber)
                + "|" + (qrPayload == null ? "" : qrPayload)
                + "|" + (attendeeEmail == null ? "" : attendeeEmail.trim().toLowerCase());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02X", b));
            }
            return "AUTH-" + hex.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private ParsedTicketCode parseTicketCode(String input) {
        String trimmed = input == null ? "" : input.trim();
        String upper = trimmed.toUpperCase();

        if (upper.startsWith("AUTH-") && upper.indexOf('|') < 0) {
            return new ParsedTicketCode(null, null, trimmed);
        }

        int authPos = upper.indexOf("|AUTH=");
        if (authPos < 0) {
            return new ParsedTicketCode(trimmed, null, null);
        }

        String base = trimmed.substring(0, authPos).trim();
        String token = trimmed.substring(authPos + 6).trim();
        if (base.isEmpty()) {
            base = trimmed;
        }
        return new ParsedTicketCode(base, token, null);
    }

    private int getGuardEventId(Connection conn, int venueGuardId) throws SQLException {
        String sql = "SELECT eventID FROM venue_guard WHERE venueGuardID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, venueGuardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("eventID");
                }
            }
        }
        throw new SQLException("Venue guard not found for ID: " + venueGuardId);
    }

    private TicketSnapshot findTicketByCode(Connection conn, String code) throws SQLException {
        String sql = "SELECT t.ticketID, t.name AS ticketNumber, q.barstring, a.email, e.eventID, e.name AS eventName "
                + "FROM ticket t "
                + "JOIN qrcode q ON q.QRcodeID = t.QRcodeID "
            + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = t.ticketID "
            + "LEFT JOIN attendee a ON a.attendeeID = aht.attendeeID "
                + "LEFT JOIN event_has_ticket eht ON eht.ticketID = t.ticketID "
                + "LEFT JOIN event e ON e.eventID = eht.eventID "
                + "WHERE UPPER(q.barstring) = UPPER(?) "
                + "FETCH FIRST ROW ONLY";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Integer eventId = rs.getObject("eventID") != null ? rs.getInt("eventID") : null;
                return new TicketSnapshot(
                        rs.getInt("ticketID"),
                        eventId,
                        rs.getString("eventName"),
                        rs.getString("ticketNumber"),
                        rs.getString("barstring"),
                        rs.getString("email")
                );
            }
        }
    }

    private boolean isTicketAlreadyUsed(Connection conn, int ticketId) throws SQLException {
        String sql = "SELECT 1 FROM scan_log WHERE ticketID = ? AND result = 'VALID' FETCH FIRST ROW ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private TicketSnapshot findTicketByAuthToken(Connection conn, String authToken) throws SQLException {
        if (authToken == null || authToken.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT t.ticketID, t.name AS ticketNumber, q.barstring, a.email, e.eventID, e.name AS eventName "
                + "FROM ticket t "
                + "JOIN qrcode q ON q.QRcodeID = t.QRcodeID "
                + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = t.ticketID "
                + "LEFT JOIN attendee a ON a.attendeeID = aht.attendeeID "
                + "LEFT JOIN event_has_ticket eht ON eht.ticketID = t.ticketID "
                + "LEFT JOIN event e ON e.eventID = eht.eventID";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String expected = deriveAuthToken(rs.getString("ticketNumber"), rs.getString("barstring"), rs.getString("email"));
                if (expected.equalsIgnoreCase(authToken.trim())) {
                    Integer eventId = rs.getObject("eventID") != null ? rs.getInt("eventID") : null;
                    return new TicketSnapshot(
                            rs.getInt("ticketID"),
                            eventId,
                            rs.getString("eventName"),
                            rs.getString("ticketNumber"),
                            rs.getString("barstring"),
                            rs.getString("email")
                    );
                }
            }
        }
        return null;
    }

    private void logScan(int venueGuardId, Integer ticketId, String rawCode, String result, String reason) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            logScan(conn, venueGuardId, ticketId, rawCode, result, reason);
        }
    }

    private void logScan(Connection conn, int venueGuardId, Integer ticketId, String rawCode, String result, String reason)
            throws SQLException {
        String sql = "INSERT INTO scan_log(venueGuardID, ticketID, rawCode, result, reason, scannedAt) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, venueGuardId);
            if (ticketId == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, ticketId);
            }
            ps.setString(3, rawCode);
            ps.setString(4, result);
            ps.setString(5, reason);
            ps.setTimestamp(6, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> getAttendeeListForGuardEvent(int venueGuardId, int limit) throws SQLException {
        int safeLimit = limit <= 0 ? 200 : Math.min(limit, 1000);
        String sql = "SELECT DISTINCT a.attendeeID, a.username, a.firstname, a.lastname, a.email "
                + "FROM venue_guard vg "
                + "JOIN event_has_ticket eht ON eht.eventID = vg.eventID "
                + "JOIN attendee_has_ticket aht ON aht.ticketID = eht.ticketID "
                + "JOIN attendee a ON a.attendeeID = aht.attendeeID "
                + "WHERE vg.venueGuardID = ? "
                + "ORDER BY a.firstname, a.lastname "
                + "FETCH FIRST " + safeLimit + " ROWS ONLY";

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, venueGuardId);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        String label = meta.getColumnLabel(i);
                        if (label == null || label.trim().isEmpty()) {
                            label = meta.getColumnName(i);
                        }
                        row.put(label, rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private static class TicketSnapshot {
        private final int ticketId;
        private final Integer eventId;
        private final String eventName;
        private final String ticketNumber;
        private final String qrPayload;
        private final String attendeeEmail;

        private TicketSnapshot(int ticketId, Integer eventId, String eventName,
                               String ticketNumber, String qrPayload, String attendeeEmail) {
            this.ticketId = ticketId;
            this.eventId = eventId;
            this.eventName = eventName;
            this.ticketNumber = ticketNumber;
            this.qrPayload = qrPayload;
            this.attendeeEmail = attendeeEmail;
        }
    }

    private static class ParsedTicketCode {
        private final String baseCode;
        private final String authToken;
        private final String authOnlyToken;

        private ParsedTicketCode(String baseCode, String authToken, String authOnlyToken) {
            this.baseCode = baseCode;
            this.authToken = authToken;
            this.authOnlyToken = authOnlyToken;
        }
    }

    public static class ScanResult {
        private final boolean valid;
        private final String message;
        private final Integer ticketId;
        private final String eventName;
        private final String scannedAt;
        private final String authenticityStatus;

        private ScanResult(boolean valid, String message, Integer ticketId, String eventName, String authenticityStatus) {
            this.valid = valid;
            this.message = message;
            this.ticketId = ticketId;
            this.eventName = eventName;
            this.authenticityStatus = authenticityStatus;
            this.scannedAt = Instant.now().toString();
        }

        public static ScanResult valid(String message, Integer ticketId, String eventName, String authenticityStatus) {
            return new ScanResult(true, message, ticketId, eventName, authenticityStatus);
        }

        public static ScanResult invalid(String message, String authenticityStatus) {
            return new ScanResult(false, message, null, null, authenticityStatus);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public Integer getTicketId() {
            return ticketId;
        }

        public String getEventName() {
            return eventName;
        }

        public String getScannedAt() {
            return scannedAt;
        }

        public String getAuthenticityStatus() {
            return authenticityStatus;
        }
    }
}