package za.ac.tut.databaseManagement;

import java.sql.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import za.ac.tut.databaseConnection.DatabaseConnection;
import za.ac.tut.entities.Attendee;

public class AttendeeDAO {

    private static final String SYSTEM_SETTING_TABLE = "system_setting";
    private static final String MINOR_RESTRICTED_KEYWORDS_SETTING_KEY = "minor_restricted_event_keywords";
    private static final String MINOR_RESTRICTED_KEYWORDS_DEFAULT = "18+,adult,alcohol,club,night,nightlife,cocktail,wine,liquor,beer";

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
            // Compact fingerprint that still has high collision resistance for ticket checks.
            return "AUTH-" + hex.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String deriveSecretCode(String ticketNumber, String qrPayload) {
        String base = (ticketNumber == null ? "" : ticketNumber) + "|" + (qrPayload == null ? "" : qrPayload);
        int hash = Math.abs(base.hashCode());
        String token = Integer.toHexString(hash).toUpperCase();
        while (token.length() < 8) {
            token = "0" + token;
        }
        return "SEC-" + token.substring(0, 4) + "-" + token.substring(4, 8);
    }

    /**
     * MAPPING: Resultset to Attendee Object
     */
    private Attendee mapRow(ResultSet rs) throws SQLException {
        Attendee a = new Attendee();
        a.setAttendeeID(rs.getInt("attendeeID"));
        a.setUsername(rs.getString("username"));
        a.setClientType(rs.getString("clientType"));
        a.setTertiaryInstitution(rs.getString("tertiaryInstitution"));
        a.setPhoneNumber(rs.getString("phoneNumber"));
        a.setStudentNumber(rs.getString("studentNumber"));
        a.setIdPassportNumber(rs.getString("idPassportNumber"));
        a.setDateOfBirth(rs.getDate("dateOfBirth"));
        a.setBiography(rs.getString("biography"));
        a.setFirstname(rs.getString("firstname"));
        a.setLastname(rs.getString("lastname"));
        a.setEmail(rs.getString("email"));
        a.setPassword(rs.getString("password"));
        return a;
    }

    // --- NEW METHODS FOR DASHBOARD FUNCTIONALITY ---
    /**
     * FETCH ALL EVENTS Used to populate the "Available Events" grid in the
     * dashboard. Returns a list of Maps to handle generic event data without
     * needing an Event entity yet.
     */
  public List<Map<String, Object>> getAllEvents() throws SQLException {
    return getAllEventsForAttendee(null);
}

    public List<Map<String, Object>> getAllEventsForAttendee(Integer attendeeID) throws SQLException {
        List<Map<String, Object>> events = new ArrayList<>();

        String sql = "SELECT e.eventID, e.name AS eventName, e.type, e.date, "
            + "e.description, e.infoUrl, COALESCE(NULLIF(TRIM(e.status), ''), 'ACTIVE') AS status, "
                + "v.name AS venueName, v.address, "
                + "MIN(t.price) AS minPrice, "
            + "COUNT(DISTINCT eht.ticketID) AS totalTickets, "
            + "COUNT(DISTINCT aht.ticketID) AS soldTickets, "
                + "MAX(CASE WHEN ahe.attendeeID IS NOT NULL THEN 1 ELSE 0 END) AS purchasedByAttendee "
                + "FROM event e "
                + "JOIN venue v ON e.venueID = v.venueID "
                + "LEFT JOIN event_has_ticket eht ON e.eventID = eht.eventID "
                + "LEFT JOIN ticket t ON eht.ticketID = t.ticketID "
                + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = eht.ticketID "
                + "LEFT JOIN attendee_has_event ahe ON ahe.eventID = e.eventID AND ahe.attendeeID = ? "
                + "GROUP BY e.eventID, e.name, e.type, e.date, e.description, e.infoUrl, e.status, v.name, v.address";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (attendeeID == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, attendeeID);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> event = new HashMap<>();
                    event.put("id", rs.getInt("eventID"));
                    event.put("name", rs.getString("eventName"));
                    event.put("type", rs.getString("type"));
                    event.put("date", rs.getDate("date"));
                    event.put("description", rs.getString("description"));
                    event.put("infoUrl", rs.getString("infoUrl"));
                    event.put("status", rs.getString("status"));
                    event.put("venueName", rs.getString("venueName"));
                    event.put("address", rs.getString("address"));
                    event.put("price", rs.getDouble("minPrice"));

                    int totalTickets = rs.getInt("totalTickets");
                    int soldTickets = Math.min(totalTickets, rs.getInt("soldTickets"));
                    int availableTickets = Math.max(0, totalTickets - soldTickets);
                    int soldPercentage = 0;
                    if (totalTickets > 0) {
                        soldPercentage = (int) Math.round((soldTickets * 100.0) / totalTickets);
                    }

                    boolean nearlySoldOut = totalTickets > 0 && soldPercentage >= 80 && soldPercentage < 100;
                    boolean soldOut = totalTickets > 0 && soldPercentage >= 100;

                    event.put("totalTickets", totalTickets);
                    event.put("soldTickets", soldTickets);
                    event.put("availableTickets", availableTickets);
                    event.put("soldPercentage", soldPercentage);
                    event.put("nearlySoldOut", nearlySoldOut);
                    event.put("soldOut", soldOut);
                    event.put("purchased", rs.getInt("purchasedByAttendee") == 1);

                    events.add(event);
                }
            }
        }

        return events;
    }

    /**
     * FETCH PURCHASED TICKETS Joins the ticket and event tables to show what
     * the specific attendee owns.
     */
    public List<Map<String, Object>> getAttendeeTickets(int attendeeID) throws SQLException {
        List<Map<String, Object>> tickets = new ArrayList<>();

        String sql = "SELECT t.ticketID, t.name AS ticketNumber, t.price, q.barstring, "
            + "e.eventID, "
            + "e.name AS eventName, e.type, e.date, v.name AS venueName, v.address, "
            + "a.firstname, a.lastname, a.email, a.tertiaryInstitution "
            + "FROM attendee_has_ticket aht "
            + "JOIN ticket t ON aht.ticketID = t.ticketID "
            + "JOIN qrcode q ON t.QRcodeID = q.QRcodeID "
            + "LEFT JOIN event_has_ticket eht ON t.ticketID = eht.ticketID "
            + "LEFT JOIN event e ON eht.eventID = e.eventID "
            + "LEFT JOIN venue v ON e.venueID = v.venueID "
            + "JOIN attendee a ON a.attendeeID = aht.attendeeID "
            + "WHERE aht.attendeeID = ? "
            + "ORDER BY t.ticketID DESC";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attendeeID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> ticket = new HashMap<>();
                    ticket.put("eventName", rs.getString("eventName"));
                    ticket.put("eventID", rs.getInt("eventID"));
                    ticket.put("eventType", rs.getString("type"));
                    ticket.put("eventDate", rs.getDate("date"));
                    ticket.put("venueName", rs.getString("venueName"));
                    ticket.put("venueAddress", rs.getString("address"));
                    ticket.put("ticketID", rs.getInt("ticketID"));
                    ticket.put("ticketNumber", rs.getString("ticketNumber"));
                    ticket.put("price", rs.getDouble("price"));
                    ticket.put("qrCode", rs.getString("barstring"));
                    ticket.put("secretCode", deriveSecretCode(rs.getString("ticketNumber"), rs.getString("barstring")));
                    ticket.put("attendeeName", rs.getString("firstname") + " " + rs.getString("lastname"));
                    ticket.put("attendeeEmail", rs.getString("email"));
                    ticket.put("attendeeInstitution", rs.getString("tertiaryInstitution"));
                    String authToken = deriveAuthToken(rs.getString("ticketNumber"), rs.getString("barstring"), rs.getString("email"));
                    ticket.put("authToken", authToken);
                    ticket.put("scannableCode", rs.getString("barstring") + "|AUTH=" + authToken);
                    ticket.put("status", "CONFIRMED");
                    tickets.add(ticket);
                }
            }
        }
        return tickets;
    }

    public List<Map<String, Object>> getRecentAttendeeTicketsForEvent(int attendeeID, int eventID, int maxRows) throws SQLException {
        List<Map<String, Object>> tickets = new ArrayList<>();
        if (attendeeID <= 0 || eventID <= 0 || maxRows <= 0) {
            return tickets;
        }

        String sql = "SELECT t.ticketID, t.name AS ticketNumber, t.price, q.barstring, "
                + "e.eventID, e.name AS eventName, e.type, e.date, v.name AS venueName, v.address "
                + "FROM attendee_has_ticket aht "
                + "JOIN ticket t ON aht.ticketID = t.ticketID "
                + "JOIN qrcode q ON t.QRcodeID = q.QRcodeID "
                + "JOIN event_has_ticket eht ON t.ticketID = eht.ticketID "
                + "JOIN event e ON eht.eventID = e.eventID "
                + "LEFT JOIN venue v ON e.venueID = v.venueID "
                + "WHERE aht.attendeeID = ? AND e.eventID = ? "
                + "ORDER BY t.ticketID DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeID);
            ps.setInt(2, eventID);
            ps.setMaxRows(maxRows);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> ticket = new HashMap<>();
                    ticket.put("eventName", rs.getString("eventName"));
                    ticket.put("eventID", rs.getInt("eventID"));
                    ticket.put("eventType", rs.getString("type"));
                    ticket.put("eventDate", rs.getDate("date"));
                    ticket.put("venueName", rs.getString("venueName"));
                    ticket.put("venueAddress", rs.getString("address"));
                    ticket.put("ticketID", rs.getInt("ticketID"));
                    ticket.put("ticketNumber", rs.getString("ticketNumber"));
                    ticket.put("price", rs.getDouble("price"));
                    ticket.put("qrCode", rs.getString("barstring"));
                    tickets.add(ticket);
                }
            }
        }

        return tickets;
    }

    public boolean recordOrderHistory(int attendeeID, String transactionRef, java.util.Collection<Map<String, Object>> cartItems, double totalAmount) throws SQLException {
        if (attendeeID <= 0 || cartItems == null || cartItems.isEmpty()) {
            return false;
        }

        String insertOrderSql = "INSERT INTO attendee_order(attendeeID, transactionRef, totalAmount, status, createdAt) VALUES(?,?,?,?,CURRENT_TIMESTAMP)";
        String insertItemSql = "INSERT INTO attendee_order_item(orderID, eventID, quantity, unitPrice, lineTotal) VALUES(?,?,?,?,?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, attendeeID);
                ps.setString(2, transactionRef);
                ps.setBigDecimal(3, BigDecimal.valueOf(totalAmount));
                ps.setString(4, "PAID");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Unable to create attendee order");
                    }
                    orderId = rs.getInt(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertItemSql)) {
                for (Map<String, Object> item : cartItems) {
                    int eventId = toInt(item.get("eventID"));
                    int quantity = toInt(item.get("quantity"));
                    double unitPrice = toDouble(item.get("price"));
                    if (eventId <= 0 || quantity <= 0) {
                        continue;
                    }
                    double lineTotal = quantity * unitPrice;
                    ps.setInt(1, orderId);
                    ps.setInt(2, eventId);
                    ps.setInt(3, quantity);
                    ps.setBigDecimal(4, BigDecimal.valueOf(unitPrice));
                    ps.setBigDecimal(5, BigDecimal.valueOf(lineTotal));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            if (conn != null) {
                conn.rollback();
            }
            throw ex;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public List<Map<String, Object>> getAttendeeOrderHistory(int attendeeID) throws SQLException {
        List<Map<String, Object>> orders = new ArrayList<>();
        if (attendeeID <= 0) {
            return orders;
        }

        String sql = "SELECT o.orderID, o.transactionRef, o.totalAmount, o.status, o.createdAt, "
                + "i.orderItemID, i.eventID, i.quantity, i.unitPrice, i.lineTotal, "
                + "e.name AS eventName, e.type AS eventType, e.date AS eventDate, v.name AS venueName "
                + "FROM attendee_order o "
                + "JOIN attendee_order_item i ON i.orderID = o.orderID "
                + "LEFT JOIN event e ON e.eventID = i.eventID "
                + "LEFT JOIN venue v ON v.venueID = e.venueID "
                + "WHERE o.attendeeID = ? "
                + "ORDER BY o.createdAt DESC, o.orderID DESC, i.orderItemID ASC";

        Map<Integer, Map<String, Object>> grouped = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int orderId = rs.getInt("orderID");
                    Map<String, Object> order = grouped.get(orderId);
                    if (order == null) {
                        order = new HashMap<>();
                        order.put("orderID", orderId);
                        order.put("transactionRef", rs.getString("transactionRef"));
                        order.put("totalAmount", rs.getBigDecimal("totalAmount"));
                        order.put("status", rs.getString("status"));
                        order.put("createdAt", rs.getTimestamp("createdAt"));
                        order.put("items", new ArrayList<Map<String, Object>>());
                        grouped.put(orderId, order);
                    }

                    Map<String, Object> item = new HashMap<>();
                    item.put("orderItemID", rs.getInt("orderItemID"));
                    item.put("eventID", rs.getInt("eventID"));
                    item.put("eventName", rs.getString("eventName"));
                    item.put("eventType", rs.getString("eventType"));
                    item.put("eventDate", rs.getTimestamp("eventDate"));
                    item.put("venueName", rs.getString("venueName"));
                    item.put("quantity", rs.getInt("quantity"));
                    item.put("unitPrice", rs.getBigDecimal("unitPrice"));
                    item.put("lineTotal", rs.getBigDecimal("lineTotal"));

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
                    items.add(item);
                }
            }
        }

        orders.addAll(grouped.values());
        return orders;
    }

    public int generateAndAssignUniqueTickets(int attendeeID, int eventID, int quantity, double unitPrice) throws SQLException {
        if (quantity <= 0) {
            return 0;
        }

        String insertQrSql = "INSERT INTO qrcode(barstring, number) VALUES(?, ?)";
        String insertTicketSql = "INSERT INTO ticket(name, price, QRcodeID) VALUES(?, ?, ?)";
        String insertEventTicketSql = "INSERT INTO event_has_ticket(eventID, ticketID) VALUES(?, ?)";
        String insertAttendeeTicketSql = "INSERT INTO attendee_has_ticket(attendeeID, ticketID) VALUES(?, ?)";
        String insertAttendeeEventSql = "INSERT INTO attendee_has_event(attendeeID, eventID) VALUES(?, ?)";
        String existsEventSql = "SELECT 1 FROM attendee_has_event WHERE attendeeID = ? AND eventID = ?";

        Connection conn = null;
        int created = 0;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            for (int i = 0; i < quantity; i++) {
                String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
                String ticketNumber = "TKY-" + eventID + "-" + token;
                String qrPayload = "TICKIFY|" + ticketNumber + "|EVT-" + eventID + "|A-" + attendeeID;
                int qrNumber = Math.abs(qrPayload.hashCode());

                int qrId;
                try (PreparedStatement ps = conn.prepareStatement(insertQrSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, qrPayload);
                    ps.setInt(2, qrNumber);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("Unable to generate QR code id");
                        }
                        qrId = rs.getInt(1);
                    }
                }

                int ticketId;
                try (PreparedStatement ps = conn.prepareStatement(insertTicketSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, ticketNumber);
                    ps.setBigDecimal(2, BigDecimal.valueOf(unitPrice));
                    ps.setInt(3, qrId);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("Unable to generate ticket id");
                        }
                        ticketId = rs.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(insertEventTicketSql)) {
                    ps.setInt(1, eventID);
                    ps.setInt(2, ticketId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(insertAttendeeTicketSql)) {
                    ps.setInt(1, attendeeID);
                    ps.setInt(2, ticketId);
                    ps.executeUpdate();
                }

                created++;
            }

            if (created > 0 && !exists(conn, existsEventSql, attendeeID, eventID)) {
                try (PreparedStatement ps = conn.prepareStatement(insertAttendeeEventSql)) {
                    ps.setInt(1, attendeeID);
                    ps.setInt(2, eventID);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return created;
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public double getAttendeeTicketTotal(int attendeeID) throws SQLException {
        String sql = "SELECT COALESCE(SUM(t.price), 0) AS total "
                + "FROM attendee_has_ticket aht "
                + "JOIN ticket t ON aht.ticketID = t.ticketID "
                + "WHERE aht.attendeeID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        return 0.0;
    }

    public int getAttendeeTicketCount(int attendeeID) throws SQLException {
        String sql = "SELECT COUNT(*) AS ticketCount FROM attendee_has_ticket WHERE attendeeID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ticketCount");
                }
            }
        }
        return 0;
    }

    public int countAvailableTicketStockForEvent(int eventID) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT eht.ticketID) AS availableCount "
                + "FROM event_has_ticket eht "
                + "JOIN ticket t ON t.ticketID = eht.ticketID "
                + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = eht.ticketID "
                + "WHERE eht.eventID = ? "
                + "AND aht.ticketID IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("availableCount");
                }
            }
        }
        return 0;
    }

    public Map<String, Object> getEventCartDetails(int eventID) throws SQLException {
        String sql = "SELECT e.eventID, e.name AS eventName, e.type AS eventType, COALESCE(MIN(t.price), 0) AS minPrice "
                + "FROM event e "
                + "LEFT JOIN event_has_ticket eht ON e.eventID = eht.eventID "
                + "LEFT JOIN ticket t ON eht.ticketID = t.ticketID "
                + "WHERE e.eventID = ? "
            + "GROUP BY e.eventID, e.name, e.type";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> event = new HashMap<>();
                    event.put("id", rs.getInt("eventID"));
                    event.put("name", rs.getString("eventName"));
                    event.put("type", rs.getString("eventType"));
                    event.put("price", rs.getDouble("minPrice"));
                    return event;
                }
            }
        }
        return null;
    }

    public boolean isAttendeeUnder18(int attendeeID) throws SQLException {
        String sql = "SELECT dateOfBirth FROM attendee WHERE attendeeID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                Date dob = rs.getDate("dateOfBirth");
                if (dob == null) {
                    return false;
                }
                LocalDate birthDate = dob.toLocalDate();
                int age = Period.between(birthDate, LocalDate.now()).getYears();
                return age < 18;
            }
        }
    }

    public boolean isRestrictedForMinorByEventType(String eventType) {
        if (eventType == null || eventType.trim().isEmpty()) {
            return false;
        }
        String normalized = eventType.trim().toLowerCase();
        for (String keyword : getMinorRestrictedKeywords()) {
            if (keyword == null || keyword.trim().isEmpty()) {
                continue;
            }
            String k = keyword.trim().toLowerCase();
            if (normalized.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getMinorRestrictedKeywords() {
        List<String> defaults = splitKeywords(MINOR_RESTRICTED_KEYWORDS_DEFAULT);
        try (Connection conn = DatabaseConnection.getConnection()) {
            ensureSystemSettingTable(conn);
            String sql = "SELECT setting_value FROM " + SYSTEM_SETTING_TABLE + " WHERE setting_key = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, MINOR_RESTRICTED_KEYWORDS_SETTING_KEY);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String value = rs.getString("setting_value");
                        if (value != null && !value.trim().isEmpty()) {
                            return splitKeywords(value);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            // Fallback to safe defaults when setting storage is unavailable.
        }
        return defaults;
    }

    private List<String> splitKeywords(String csv) {
        List<String> items = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) {
            return items;
        }
        String[] parts = csv.split(",");
        for (String p : parts) {
            if (p != null && !p.trim().isEmpty()) {
                items.add(p.trim());
            }
        }
        return items;
    }

    private void ensureSystemSettingTable(Connection conn) throws SQLException {
        if (tableExists(conn, SYSTEM_SETTING_TABLE)) {
            return;
        }
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE " + SYSTEM_SETTING_TABLE
                    + " (setting_key VARCHAR(120) NOT NULL PRIMARY KEY, setting_value VARCHAR(2048), updated_at TIMESTAMP)");
            try (PreparedStatement seed = conn.prepareStatement(
                    "INSERT INTO " + SYSTEM_SETTING_TABLE + "(setting_key, setting_value, updated_at) VALUES(?, ?, CURRENT_TIMESTAMP)")) {
                seed.setString(1, MINOR_RESTRICTED_KEYWORDS_SETTING_KEY);
                seed.setString(2, MINOR_RESTRICTED_KEYWORDS_DEFAULT);
                seed.executeUpdate();
            }
        }
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        String upper = tableName == null ? "" : tableName.toUpperCase();
        try (ResultSet rs = meta.getTables(null, null, upper, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    public int purchaseTicketsForEvent(int attendeeID, int eventID, int quantity) throws SQLException {
        if (quantity <= 0) {
            return 0;
        }

        String ticketQuery = "SELECT t.ticketID FROM event_has_ticket eht "
                + "JOIN ticket t ON t.ticketID = eht.ticketID "
                + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = t.ticketID "
            + "WHERE eht.eventID = ? AND aht.ticketID IS NULL "
                + "ORDER BY t.price ASC, t.ticketID ASC";
        String insertTicketSql = "INSERT INTO attendee_has_ticket(attendeeID, ticketID) VALUES(?, ?)";
        String insertEventSql = "INSERT INTO attendee_has_event(attendeeID, eventID) VALUES(?, ?)";
        String existsEventSql = "SELECT 1 FROM attendee_has_event WHERE attendeeID = ? AND eventID = ?";

        Connection conn = null;
        int purchasedCount = 0;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            List<Integer> candidateTicketIds = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(ticketQuery)) {
                ps.setInt(1, eventID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next() && purchasedCount < quantity) {
                        candidateTicketIds.add(rs.getInt("ticketID"));
                        purchasedCount++;
                    }
                }
            }

            purchasedCount = 0;
            try (PreparedStatement insertTicket = conn.prepareStatement(insertTicketSql)) {
                for (Integer ticketId : candidateTicketIds) {
                    insertTicket.setInt(1, attendeeID);
                    insertTicket.setInt(2, ticketId);
                    insertTicket.executeUpdate();
                    purchasedCount++;
                }
            }

            if (purchasedCount > 0 && !exists(conn, existsEventSql, attendeeID, eventID)) {
                try (PreparedStatement ps = conn.prepareStatement(insertEventSql)) {
                    ps.setInt(1, attendeeID);
                    ps.setInt(2, eventID);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return purchasedCount;
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public List<Map<String, Object>> getWishlistEvents(int attendeeID) throws SQLException {
        List<Map<String, Object>> events = new ArrayList<>();
        String sql = "SELECT e.eventID, e.name AS eventName, e.type, e.date, "
                + "v.name AS venueName, v.address, MIN(t.price) AS minPrice, "
            + "COUNT(DISTINCT eht.ticketID) AS totalTickets, "
            + "COUNT(DISTINCT aht.ticketID) AS soldTickets, "
                + "MAX(CASE WHEN ahe.attendeeID IS NOT NULL THEN 1 ELSE 0 END) AS purchasedByAttendee "
                + "FROM attendee_wishlist aw "
                + "JOIN event e ON aw.eventID = e.eventID "
                + "JOIN venue v ON e.venueID = v.venueID "
                + "LEFT JOIN event_has_ticket eht ON e.eventID = eht.eventID "
                + "LEFT JOIN ticket t ON eht.ticketID = t.ticketID "
                + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = eht.ticketID "
                + "LEFT JOIN attendee_has_event ahe ON ahe.eventID = e.eventID AND ahe.attendeeID = ? "
                + "WHERE aw.attendeeID = ? "
                + "GROUP BY e.eventID, e.name, e.type, e.date, v.name, v.address, aw.createdAt "
                + "ORDER BY aw.createdAt DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeID);
            ps.setInt(2, attendeeID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> event = new HashMap<>();
                    event.put("id", rs.getInt("eventID"));
                    event.put("name", rs.getString("eventName"));
                    event.put("type", rs.getString("type"));
                    event.put("date", rs.getDate("date"));
                    event.put("venueName", rs.getString("venueName"));
                    event.put("address", rs.getString("address"));
                    event.put("price", rs.getDouble("minPrice"));

                    int totalTickets = rs.getInt("totalTickets");
                    int soldTickets = Math.min(totalTickets, rs.getInt("soldTickets"));
                    int availableTickets = Math.max(0, totalTickets - soldTickets);
                    int soldPercentage = 0;
                    if (totalTickets > 0) {
                        soldPercentage = (int) Math.round((soldTickets * 100.0) / totalTickets);
                    }

                    event.put("totalTickets", totalTickets);
                    event.put("soldTickets", soldTickets);
                    event.put("availableTickets", availableTickets);
                    event.put("soldPercentage", soldPercentage);
                    event.put("nearlySoldOut", totalTickets > 0 && soldPercentage >= 80 && soldPercentage < 100);
                    event.put("soldOut", totalTickets > 0 && soldPercentage >= 100);
                    event.put("purchased", rs.getInt("purchasedByAttendee") == 1);
                    event.put("wishlisted", true);

                    events.add(event);
                }
            }
        }

        return events;
    }

    public Set<Integer> getWishlistEventIds(int attendeeID) throws SQLException {
        Set<Integer> eventIds = new HashSet<>();
        String sql = "SELECT eventID FROM attendee_wishlist WHERE attendeeID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    eventIds.add(rs.getInt("eventID"));
                }
            }
        }
        return eventIds;
    }

    public boolean addEventToWishlist(int attendeeID, int eventID) throws SQLException {
        String existsSql = "SELECT 1 FROM attendee_wishlist WHERE attendeeID = ? AND eventID = ?";
        String insertSql = "INSERT INTO attendee_wishlist(attendeeID, eventID, createdAt) VALUES(?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (exists(conn, existsSql, attendeeID, eventID)) {
                return true;
            }
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setInt(1, attendeeID);
                ps.setInt(2, eventID);
                return ps.executeUpdate() > 0;
            }
        }
    }

    public boolean removeEventFromWishlist(int attendeeID, int eventID) throws SQLException {
        String sql = "DELETE FROM attendee_wishlist WHERE attendeeID = ? AND eventID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attendeeID);
            ps.setInt(2, eventID);
            return ps.executeUpdate() > 0;
        }
    }

    // --- EXISTING CRUD METHODS ---
    public Attendee getAttendeeByID(int id) throws SQLException {
        List<Attendee> results = executeQuery("SELECT * FROM attendee WHERE attendeeID = ?", id);
        return results.isEmpty() ? null : results.get(0);
    }

    public boolean insertAttendee(Attendee a) throws SQLException {
        String sql = "INSERT INTO attendee(username, clientType, tertiaryInstitution, phoneNumber, studentNumber, idPassportNumber, dateOfBirth, biography, firstname, lastname, email, password, emailVerified, qrcode_QRcodeID) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        int qrId = (a.getQrCode() != null) ? a.getQrCode().getQrCodeID() : 0;
        return executeUpdate(sql,
                a.getUsername(),
                a.getClientType(),
                a.getTertiaryInstitution(),
                a.getPhoneNumber(),
                a.getStudentNumber(),
                a.getIdPassportNumber(),
                a.getDateOfBirth(),
                a.getBiography(),
                a.getFirstname(),
                a.getLastname(),
                a.getEmail(),
                a.getPassword(),
                false,
                qrId);
    }

    public boolean updateAttendee(Attendee a) throws SQLException {
        String sql = "UPDATE attendee SET username=?, clientType=?, tertiaryInstitution=?, phoneNumber=?, studentNumber=?, idPassportNumber=?, dateOfBirth=?, biography=?, firstname=?, lastname=?, email=?, password=? WHERE attendeeID=?";
        return executeUpdate(sql,
                a.getUsername(),
                a.getClientType(),
                a.getTertiaryInstitution(),
                a.getPhoneNumber(),
                a.getStudentNumber(),
                a.getIdPassportNumber(),
                a.getDateOfBirth(),
                a.getBiography(),
                a.getFirstname(),
                a.getLastname(),
                a.getEmail(),
                a.getPassword(),
                a.getAttendeeID());
    }

    public boolean purchaseCheapestTicketForEvent(int attendeeID, int eventID) throws SQLException {
        String pickTicketSql = "SELECT t.ticketID FROM event_has_ticket eht "
                + "JOIN ticket t ON t.ticketID = eht.ticketID "
                + "WHERE eht.eventID = ? "
                + "ORDER BY t.price ASC FETCH FIRST ROW ONLY";
        String insertTicketSql = "INSERT INTO attendee_has_ticket(attendeeID, ticketID) VALUES(?, ?)";
        String insertEventSql = "INSERT INTO attendee_has_event(attendeeID, eventID) VALUES(?, ?)";
        String existsTicketSql = "SELECT 1 FROM attendee_has_ticket WHERE attendeeID = ? AND ticketID = ?";
        String existsEventSql = "SELECT 1 FROM attendee_has_event WHERE attendeeID = ? AND eventID = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            Integer ticketId = null;
            try (PreparedStatement ps = conn.prepareStatement(pickTicketSql)) {
                ps.setInt(1, eventID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ticketId = rs.getInt("ticketID");
                    }
                }
            }

            if (ticketId == null) {
                conn.rollback();
                return false;
            }

            if (!exists(conn, existsTicketSql, attendeeID, ticketId)) {
                try (PreparedStatement ps = conn.prepareStatement(insertTicketSql)) {
                    ps.setInt(1, attendeeID);
                    ps.setInt(2, ticketId);
                    ps.executeUpdate();
                }
            }

            if (!exists(conn, existsEventSql, attendeeID, eventID)) {
                try (PreparedStatement ps = conn.prepareStatement(insertEventSql)) {
                    ps.setInt(1, attendeeID);
                    ps.setInt(2, eventID);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public List<Map<String, Object>> getEventStockSnapshots() throws SQLException {
        List<Map<String, Object>> events = new ArrayList<>();
        String sql = "SELECT e.eventID, COUNT(DISTINCT eht.ticketID) AS totalTickets, "
                + "COUNT(DISTINCT aht.ticketID) AS soldTickets "
                + "FROM event e "
                + "LEFT JOIN event_has_ticket eht ON e.eventID = eht.eventID "
                + "LEFT JOIN attendee_has_ticket aht ON aht.ticketID = eht.ticketID "
                + "GROUP BY e.eventID";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int eventId = rs.getInt("eventID");
                int totalTickets = rs.getInt("totalTickets");
                int soldTickets = Math.min(totalTickets, rs.getInt("soldTickets"));
                int availableTickets = Math.max(0, totalTickets - soldTickets);
                int soldPercentage = totalTickets > 0
                        ? (int) Math.round((soldTickets * 100.0) / totalTickets)
                        : 0;

                Map<String, Object> event = new HashMap<>();
                event.put("id", eventId);
                event.put("totalTickets", totalTickets);
                event.put("soldTickets", soldTickets);
                event.put("availableTickets", availableTickets);
                event.put("soldPercentage", soldPercentage);
                event.put("soldOut", totalTickets > 0 && soldPercentage >= 100);
                event.put("nearlySoldOut", totalTickets > 0 && soldPercentage >= 80 && soldPercentage < 100);
                events.add(event);
            }
        }

        return events;
    }

public boolean deleteAttendee(int id) throws SQLException {
    // 1. Define the SQL for the relationships first
    String deleteEventLinks = "DELETE FROM attendee_has_event WHERE attendeeID = ?";
    String deleteTicketLinks = "DELETE FROM attendee_has_ticket WHERE attendeeID = ?";
    String deleteWishlistLinks = "DELETE FROM attendee_wishlist WHERE attendeeID = ?";
    
    // 2. Define the SQL for the Attendee
    String deleteAttendee = "DELETE FROM attendee WHERE attendeeID = ?";
    
    // 3. Optional: If you want to delete the QR code from the qrcode table too:
    // String deleteQR = "DELETE FROM qrcode WHERE QRcodeID = (SELECT qrcode_QRcodeID FROM attendee WHERE attendeeID = ?)";

    Connection conn = null;
    try {
        conn = DatabaseConnection.getConnection();
        conn.setAutoCommit(false); // Start transaction

        // Step A: Clear Many-to-Many links (Child records)
        try (PreparedStatement ps1 = conn.prepareStatement(deleteEventLinks)) {
            ps1.setInt(1, id);
            ps1.executeUpdate();
        }
        
        try (PreparedStatement ps2 = conn.prepareStatement(deleteTicketLinks)) {
            ps2.setInt(1, id);
            ps2.executeUpdate();
        }

        try (PreparedStatement psWishlist = conn.prepareStatement(deleteWishlistLinks)) {
            psWishlist.setInt(1, id);
            psWishlist.executeUpdate();
        }

        // Step B: Clear the Attendee (Parent record)
        try (PreparedStatement ps3 = conn.prepareStatement(deleteAttendee)) {
            ps3.setInt(1, id);
            int rowsAffected = ps3.executeUpdate();

            conn.commit(); // Save all changes
            return rowsAffected > 0;
        }

    } catch (SQLException e) {
        if (conn != null) conn.rollback(); // Undo if any step fails
        throw e;
    } finally {
        if (conn != null) conn.close();
    }
}

    // --- UTILITY EXECUTORS ---
    private List<Attendee> executeQuery(String sql, Object... params) throws SQLException {
        List<Attendee> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private boolean executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate() > 0;
        }
    }

    private boolean exists(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }
}
